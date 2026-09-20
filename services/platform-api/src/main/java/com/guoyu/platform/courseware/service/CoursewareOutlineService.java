package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.courseware.domain.CoursewareJson;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineChapterDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlinePageDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSlideMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 大纲生成与确认(方案 §7):章→页两级;cover/toc/review 三页确定性组装;
 * 确认时快照固化到 cw_slide(此后编辑大纲不影响已生成页,U-15)。
 */
@Slf4j
@Service
public class CoursewareOutlineService {

  /** 钳制(方案 §7):章 3~6、内容页+固定页总数 ≤50 */
  static final int MIN_CHAPTERS = 3;
  static final int MAX_CHAPTERS = 6;
  static final int MAX_TOTAL_PAGES = 50;

  private final CoursewareAiService aiService;
  private final CwOutlinePageMapper outlineMapper;
  private final CwProjectMapper projectMapper;
  private final CwSlideMapper slideMapper;
  private final ObjectMapper objectMapper;

  public CoursewareOutlineService(CoursewareAiService aiService, CwOutlinePageMapper outlineMapper,
                                  CwProjectMapper projectMapper, CwSlideMapper slideMapper,
                                  ObjectMapper objectMapper) {
    this.aiService = aiService;
    this.outlineMapper = outlineMapper;
    this.projectMapper = projectMapper;
    this.slideMapper = slideMapper;
    this.objectMapper = objectMapper;
  }

  // ──────────────── 生成(pipeline 调用) ────────────────

  /**
   * 生成大纲页实体列表(不落库,由 pipeline 统一落库)。
   * LLM 异常或两轮校验不过 → BusinessException("大纲生成失败,请重试")。
   */
  public List<CwOutlinePageEntity> generateOutlinePages(Long tenantId, Long projectId,
      Long operatorId, String title, String audience, String density,
      List<SourceSection> sections, DigestDraft digest) {
    List<SourceSection> trimmed = CoursewareDigestService.trimSections(sections,
        CoursewareDigestService.MAX_SECTION_CHARS, CoursewareDigestService.MAX_TOTAL_CHARS);
    Set<String> validRefs = CoursewareDigestService.validRefs(sections);
    String userPrompt = CoursewarePrompts.outlineUser(title, audience, density, trimmed, digest,
        objectMapper);
    Exception last = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        String content = aiService.callOutline(tenantId, projectId, operatorId,
            CoursewarePrompts.outlineSystem(), userPrompt);
        OutlineDraft draft = parseOutline(content, validRefs, objectMapper);
        return assemblePages(draft, title, projectId, tenantId, operatorId);
      } catch (IllegalArgumentException e) {
        last = e;
        log.warn("大纲输出校验失败(第 {} 轮): projectId={}, error={}", attempt + 1, projectId,
            e.getMessage());
        userPrompt += "\n上次输出不符合约定,必须只输出合法 JSON 对象,不要任何额外文字。";
      } catch (RuntimeException e) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "大纲生成失败,请稍后重试");
      }
    }
    log.warn("大纲两轮校验不过: projectId={}, error={}", projectId,
        last == null ? "" : last.getMessage());
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "大纲生成失败,请重试");
  }

  /** 输出零信任解析:结构/refs 过滤/role 兜底(钳制在 assemblePages 做) */
  public static OutlineDraft parseOutline(String content, Set<String> validRefs, ObjectMapper om) {
    JsonNode root = CoursewareJson.extractObject(content, om);
    JsonNode chaptersNode = root.path("chapters");
    if (!chaptersNode.isArray() || chaptersNode.isEmpty()) {
      throw new IllegalArgumentException("chapters 缺失或为空");
    }
    List<OutlineChapterDraft> chapters = new ArrayList<>();
    for (JsonNode ch : chaptersNode) {
      String chapterTitle = textOr(ch, "chapter_title");
      String coreConclusion = textOr(ch, "core_conclusion");
      List<OutlinePageDraft> pages = new ArrayList<>();
      JsonNode pagesNode = ch.path("pages");
      if (pagesNode.isArray()) {
        for (JsonNode p : pagesNode) {
          String title = textOr(p, "title");
          if (title == null || title.isBlank()) {
            continue;
          }
          String role = textOr(p, "page_role");
          if (!"section".equals(role)) {
            role = "content";
          }
          String hint = textOr(p, "layout_hint");
          List<String> refs = stringList(p.path("source_refs")).stream()
              .filter(validRefs::contains).distinct().toList();
          pages.add(new OutlinePageDraft(title.trim(), textOr(p, "objective"),
              stringList(p.path("key_points")), role, hint, refs));
        }
      }
      chapters.add(new OutlineChapterDraft(chapterTitle == null ? "未命名章" : chapterTitle.trim(),
          coreConclusion, pages));
    }
    List<String> reviewQuestions = stringList(root.path("review_questions"));
    return new OutlineDraft(chapters, reviewQuestions);
  }

  /**
   * 确定性组装(方案 §7):章钳制 [3,6](超出并入末章)、总页数 ≤50(截断)、
   * 每章首页强制 section、cover/toc/review 三页确定性生成、position 连续化。
   */
  public static List<CwOutlinePageEntity> assemblePages(OutlineDraft draft, String deckTitle,
      Long projectId, Long tenantId, Long operatorId) {
    // 章钳制:0 章已在 parse 拦截;>6 章并入第 6 章
    List<OutlineChapterDraft> chapters = new ArrayList<>(draft.chapters());
    if (chapters.size() > MAX_CHAPTERS) {
      OutlineChapterDraft sixth = chapters.get(MAX_CHAPTERS - 1);
      List<OutlinePageDraft> merged = new ArrayList<>(sixth.pages());
      for (int i = MAX_CHAPTERS; i < chapters.size(); i++) {
        merged.addAll(chapters.get(i).pages());
      }
      chapters.set(MAX_CHAPTERS - 1,
          new OutlineChapterDraft(sixth.chapterTitle(), sixth.coreConclusion(), merged));
      chapters = new ArrayList<>(chapters.subList(0, MAX_CHAPTERS));
    }

    List<CwOutlinePageEntity> pages = new ArrayList<>();
    int[] position = {1};

    // 固定页:cover
    pages.add(fixedPage(projectId, tenantId, operatorId, position[0]++, deckTitle,
        CwOutlinePageEntity.ROLE_COVER, List.of(), CoursewareLayout.COVER));
    // 固定页:toc(章标题列表)
    List<String> tocItems = chapters.stream().map(OutlineChapterDraft::chapterTitle).toList();
    pages.add(fixedPage(projectId, tenantId, operatorId, position[0]++, "目录",
        CwOutlinePageEntity.ROLE_TOC, tocItems, CoursewareLayout.TOC));

    // 章页:每章首页 section,其余 content;总页数钳制 50(含 3 固定页)
    int chapterNo = 1;
    for (OutlineChapterDraft chapter : chapters) {
      List<OutlinePageDraft> chapterPages = new ArrayList<>(chapter.pages());
      // 每章首页强制 section(title=章标题),模型没给 section 首页则补
      if (chapterPages.isEmpty()
          || !"section".equals(chapterPages.get(0).pageRole())) {
        chapterPages.add(0, new OutlinePageDraft(chapter.chapterTitle(), chapter.coreConclusion(),
            List.of(), "section", null, List.of()));
      }
      for (int i = 0; i < chapterPages.size(); i++) {
        if (pages.size() >= MAX_TOTAL_PAGES - 1) {
          break; // 留给末页 review
        }
        OutlinePageDraft p = chapterPages.get(i);
        String role = i == 0 ? "section" : CwOutlinePageEntity.ROLE_CONTENT;
        String title = i == 0 ? chapter.chapterTitle() : p.title();
        CwOutlinePageEntity entity = newPage(projectId, tenantId, operatorId, position[0]++,
            title, role);
        entity.setChapterNo(chapterNo);
        entity.setChapterTitle(chapter.chapterTitle());
        entity.setObjective(p.objective());
        entity.setKeyPoints(writeJson(p.keyPoints()));
        entity.setSourceRefs(writeJson(p.sourceRefs()));
        entity.setLayoutId(CoursewareLayout.layoutForRole(role, p.layoutHint()));
        pages.add(entity);
      }
      chapterNo++;
    }

    // 固定页:review(复习思考题,末页;缺失时用章核心结论兜底)
    List<String> questions = new ArrayList<>(draft.reviewQuestions());
    if (questions.size() > 5) {
      questions = new ArrayList<>(questions.subList(0, 5));
    }
    if (questions.size() < 3) {
      for (OutlineChapterDraft chapter : chapters) {
        if (questions.size() >= 3) {
          break;
        }
        if (chapter.coreConclusion() != null && !chapter.coreConclusion().isBlank()) {
          questions.add("请回顾并简述:「" + chapter.coreConclusion() + "」");
        }
      }
    }
    if (questions.isEmpty()) {
      questions.add("请回顾本课件各章的核心结论,并用自己的话复述一遍");
    }
    pages.add(fixedPage(projectId, tenantId, operatorId, position[0]++, "复习思考题",
        CwOutlinePageEntity.ROLE_REVIEW, questions, CoursewareLayout.SUMMARY));

    // position 连续化(截断后仍连续)
    for (int i = 0; i < pages.size(); i++) {
      pages.get(i).setPosition(i + 1);
    }
    return pages;
  }

  private static CwOutlinePageEntity fixedPage(Long projectId, Long tenantId, Long operatorId,
      int position, String title, String role, List<String> keyPoints, String layoutId) {
    CwOutlinePageEntity entity = newPage(projectId, tenantId, operatorId, position, title, role);
    entity.setChapterNo(0);
    entity.setKeyPoints(writeJson(keyPoints));
    entity.setSourceRefs(writeJson(List.of()));
    entity.setLayoutId(layoutId);
    return entity;
  }

  private static CwOutlinePageEntity newPage(Long projectId, Long tenantId, Long operatorId,
      int position, String title, String role) {
    CwOutlinePageEntity entity = new CwOutlinePageEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(tenantId);
    entity.setProjectId(projectId);
    entity.setPosition(position);
    entity.setTitle(title);
    entity.setPageRole(role);
    entity.setCreatedBy(operatorId);
    entity.setUpdatedBy(operatorId);
    return entity;
  }

  private static String writeJson(Object value) {
    try {
      return new ObjectMapper().writeValueAsString(value);
    } catch (Exception e) {
      return "[]";
    }
  }

  // ──────────────── 大纲 CRUD(仅 outline_ready 可编辑) ────────────────

  public List<CwOutlinePageEntity> listPages(Long tenantId, Long projectId) {
    return outlineMapper.selectList(new LambdaQueryWrapper<CwOutlinePageEntity>()
        .eq(CwOutlinePageEntity::getTenantId, tenantId)
        .eq(CwOutlinePageEntity::getProjectId, projectId)
        .orderByAsc(CwOutlinePageEntity::getPosition));
  }

  public CwOutlinePageEntity updatePage(Long tenantId, Long pageId, String title,
      String objective, List<String> keyPoints, String chapterTitle, String layoutId) {
    CwOutlinePageEntity page = getEditablePage(tenantId, pageId);
    if (title != null && !title.isBlank()) {
      page.setTitle(title.trim());
    }
    if (objective != null) {
      page.setObjective(objective.trim());
    }
    if (keyPoints != null) {
      page.setKeyPoints(writeJson(keyPoints));
    }
    if (chapterTitle != null && page.getChapterNo() != null && page.getChapterNo() > 0) {
      page.setChapterTitle(chapterTitle.trim());
    }
    if (layoutId != null && CwOutlinePageEntity.ROLE_CONTENT.equals(page.getPageRole())) {
      if (!CoursewareLayout.contentLayoutChoices().contains(layoutId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "内容页版式不合法: " + layoutId);
      }
      page.setLayoutId(layoutId);
    }
    outlineMapper.updateById(page);
    return page;
  }

  /** 新增页:挂到指定章末尾(或 afterPageId 之后),其后页 position 顺移 */
  @Transactional
  public CwOutlinePageEntity addPage(Long tenantId, Long projectId, Long operatorId,
      Integer chapterNo, Long afterPageId, String title, String objective,
      List<String> keyPoints) {
    requireOutlineReady(tenantId, projectId);
    if (title == null || title.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "页标题不能为空");
    }
    List<CwOutlinePageEntity> pages = listPages(tenantId, projectId);
    int insertPos;
    String chapterTitle = null;
    if (afterPageId != null) {
      CwOutlinePageEntity after = pages.stream().filter(p -> p.getId().equals(afterPageId))
          .findFirst()
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "参照页不存在"));
      insertPos = after.getPosition() + 1;
      chapterNo = after.getChapterNo();
      chapterTitle = after.getChapterTitle();
    } else if (chapterNo != null && chapterNo > 0) {
      int finalChapterNo = chapterNo;
      List<CwOutlinePageEntity> inChapter = pages.stream()
          .filter(p -> p.getChapterNo() != null && p.getChapterNo() == finalChapterNo).toList();
      insertPos = inChapter.isEmpty() ? pages.size() : inChapter.get(inChapter.size() - 1).getPosition() + 1;
      chapterTitle = inChapter.isEmpty() ? null : inChapter.get(0).getChapterTitle();
    } else {
      // 默认追加到 review 页之前
      insertPos = pages.size();
    }
    // 顺移(从大到小更新避免顺序冲突)
    for (int i = pages.size() - 1; i >= 0; i--) {
      CwOutlinePageEntity p = pages.get(i);
      if (p.getPosition() >= insertPos) {
        p.setPosition(p.getPosition() + 1);
        outlineMapper.updateById(p);
      }
    }
    CwOutlinePageEntity entity = newPage(projectId, tenantId, operatorId, insertPos, title.trim(),
        CwOutlinePageEntity.ROLE_CONTENT);
    entity.setChapterNo(chapterNo == null ? 0 : chapterNo);
    entity.setChapterTitle(chapterTitle);
    entity.setObjective(objective);
    entity.setKeyPoints(writeJson(keyPoints == null ? List.of() : keyPoints));
    entity.setSourceRefs(writeJson(List.of()));
    entity.setLayoutId(CoursewareLayout.CONTENT_BULLETS);
    outlineMapper.insert(entity);
    return entity;
  }

  /** 删除页:cover/toc/review 固定页不可删;删后 position 连续化 */
  @Transactional
  public void deletePage(Long tenantId, Long pageId) {
    CwOutlinePageEntity page = getEditablePage(tenantId, pageId);
    if (page.getChapterNo() != null && page.getChapterNo() == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "封面/目录/复习思考题页为固定页,不可删除");
    }
    outlineMapper.deleteById(page.getId());
    List<CwOutlinePageEntity> pages = listPages(tenantId, page.getProjectId());
    for (int i = 0; i < pages.size(); i++) {
      if (pages.get(i).getPosition() != i + 1) {
        pages.get(i).setPosition(i + 1);
        outlineMapper.updateById(pages.get(i));
      }
    }
  }

  /** 整单重排:orderedIds 必须与当前页集合一致 */
  @Transactional
  public void reorder(Long tenantId, Long projectId, List<Long> orderedIds) {
    requireOutlineReady(tenantId, projectId);
    List<CwOutlinePageEntity> pages = listPages(tenantId, projectId);
    Map<Long, CwOutlinePageEntity> byId = pages.stream()
        .collect(Collectors.toMap(CwOutlinePageEntity::getId, Function.identity()));
    if (orderedIds == null || orderedIds.size() != pages.size()
        || !new HashSet<>(orderedIds).containsAll(byId.keySet())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "页集合不一致,请刷新后重试");
    }
    for (int i = 0; i < orderedIds.size(); i++) {
      CwOutlinePageEntity page = byId.get(orderedIds.get(i));
      if (page != null && page.getPosition() != i + 1) {
        page.setPosition(i + 1);
        outlineMapper.updateById(page);
      }
    }
  }

  /**
   * 确认大纲:快照固化到 cw_slide(pending),项目置 generating。
   * 返回是否首次确认成功(重复确认幂等返回 false)。生成派发由调用方在事务提交后进行。
   */
  @Transactional
  public boolean confirm(Long tenantId, Long operatorId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    if (!CwProjectEntity.STATUS_OUTLINE_READY.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不能确认大纲");
    }
    List<CwOutlinePageEntity> pages = listPages(tenantId, projectId);
    if (pages.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "大纲为空,不能开始生成");
    }
    // 乐观占用:outline_ready → generating,防并发重复确认
    project.setStatus(CwProjectEntity.STATUS_GENERATING);
    project.setTotalPages(pages.size());
    project.setReadyPages(0);
    project.setFailedPages(0);
    project.setError(null);
    int updated = projectMapper.updateById(project);
    if (updated == 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "大纲已确认,请勿重复操作");
    }
    // 幂等(EA-10):重复确认前清掉旧快照页(上一轮回滚残留的 pending 等)
    slideMapper.delete(new LambdaQueryWrapper<CwSlideEntity>()
        .eq(CwSlideEntity::getTenantId, tenantId)
        .eq(CwSlideEntity::getProjectId, projectId));
    for (int i = 0; i < pages.size(); i++) {
      CwOutlinePageEntity page = pages.get(i);
      if (page.getPosition() != i + 1) {
        page.setPosition(i + 1);
        outlineMapper.updateById(page);
      }
      CwSlideEntity slide = new CwSlideEntity();
      slide.setId(IdWorker.getId());
      slide.setTenantId(tenantId);
      slide.setProjectId(projectId);
      slide.setOutlinePageId(page.getId());
      slide.setPosition(i + 1);
      slide.setLayoutId(page.getLayoutId() != null ? page.getLayoutId()
          : CoursewareLayout.layoutForRole(page.getPageRole(), null));
      slide.setStatus(CwSlideEntity.STATUS_PENDING);
      slide.setRevision(0);
      slide.setCreatedBy(operatorId);
      slide.setUpdatedBy(operatorId);
      slideMapper.insert(slide);
    }
    return true;
  }

  private CwOutlinePageEntity getEditablePage(Long tenantId, Long pageId) {
    CwOutlinePageEntity page = outlineMapper.selectById(pageId);
    if (page == null || !tenantId.equals(page.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "大纲页不存在");
    }
    requireOutlineReady(tenantId, page.getProjectId());
    return page;
  }

  private void requireOutlineReady(Long tenantId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    if (!CwProjectEntity.STATUS_OUTLINE_READY.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅大纲待确认状态可编辑");
    }
  }

  private static String textOr(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isTextual() ? v.asText() : null;
  }

  private static List<String> stringList(JsonNode node) {
    if (!node.isArray()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (JsonNode item : node) {
      if (item.isTextual()) {
        out.add(item.asText());
      }
    }
    return out;
  }
}
