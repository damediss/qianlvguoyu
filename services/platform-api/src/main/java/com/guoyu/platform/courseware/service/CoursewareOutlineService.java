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

  /** 钳制(方案 §7):章 3~6;总页数上限走配置(知识点页会显著放大体量,v1.4) */
  static final int MIN_CHAPTERS = 3;
  static final int MAX_CHAPTERS = 6;

  private final CoursewareAiService aiService;
  private final CwOutlinePageMapper outlineMapper;
  private final CwProjectMapper projectMapper;
  private final CwSlideMapper slideMapper;
  private final ObjectMapper objectMapper;
  private final int maxTotalPages;

  public CoursewareOutlineService(CoursewareAiService aiService, CwOutlinePageMapper outlineMapper,
                                  CwProjectMapper projectMapper, CwSlideMapper slideMapper,
                                  ObjectMapper objectMapper,
                                  @org.springframework.beans.factory.annotation.Value(
                                      "${platform.courseware.max-pages:300}") int maxTotalPages) {
    this.aiService = aiService;
    this.outlineMapper = outlineMapper;
    this.projectMapper = projectMapper;
    this.slideMapper = slideMapper;
    this.objectMapper = objectMapper;
    this.maxTotalPages = maxTotalPages;
  }

  // ──────────────── 生成(pipeline 调用) ────────────────

  /**
   * 生成大纲页实体列表(不落库,由 pipeline 统一落库)。
   * LLM 异常或两轮校验不过 → BusinessException("大纲生成失败,请重试")。
   * knowledgePoints 非空时:基本要求/工作要求章替换为逐知识点页(方案 v1.4)。
   */
  public List<CwOutlinePageEntity> generateOutlinePages(Long tenantId, Long projectId,
      Long operatorId, String title, String audience, String density,
      List<SourceSection> sections, DigestDraft digest,
      List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity> knowledgePoints) {
    List<SourceSection> trimmed = CoursewareDigestService.trimSections(sections,
        CoursewareDigestService.MAX_SECTION_CHARS, CoursewareDigestService.MAX_TOTAL_CHARS);
    Set<String> validRefs = CoursewareDigestService.validRefs(sections);
    boolean hasKnowledgePoints = knowledgePoints != null && !knowledgePoints.isEmpty();
    String userPrompt = CoursewarePrompts.outlineUser(title, audience, density, trimmed, digest,
        hasKnowledgePoints, objectMapper);
    Exception last = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        String content = aiService.callOutline(tenantId, projectId, operatorId,
            CoursewarePrompts.outlineSystem(), userPrompt);
        OutlineDraft draft = parseOutline(content, validRefs, objectMapper);
        List<CwOutlinePageEntity> pages = assemblePages(draft, title, projectId, tenantId,
            operatorId, maxTotalPages);
        if (hasKnowledgePoints) {
          pages = replaceRequirementChapters(pages, knowledgePoints, projectId, tenantId,
              operatorId, maxTotalPages);
        }
        return pages;
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
   * 确定性组装(方案 §7):章钳制 [3,6](超出并入末章)、总页数 ≤maxTotalPages(截断)、
   * 每章首页强制 section、cover/toc/review 三页确定性生成、position 连续化。
   */
  public static List<CwOutlinePageEntity> assemblePages(OutlineDraft draft, String deckTitle,
      Long projectId, Long tenantId, Long operatorId, int maxTotalPages) {
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

    // 章页:每章首页 section,其余 content;总页数钳制 maxTotalPages(含 3 固定页)
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
        if (pages.size() >= maxTotalPages - 1) {
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

  // ──────────────── 知识点章替换(方案 v1.4) ────────────────

  /**
   * 把大纲里属于「基本要求/工作要求」来源范围的章,替换为逐知识点章:
   * 基本要求 1 章(逐三级编号点)、工作要求按等级 N 章(逐 x.y.z 相关知识要求点)。
   * 识别按章内页面 source_refs 是否全部落在该范围(不依赖模型起的章名);
   * 无匹配章时在 review 页之前兜底插入。总页数仍受 maxTotalPages 截断。
   */
  public static List<CwOutlinePageEntity> replaceRequirementChapters(
      List<CwOutlinePageEntity> pages,
      List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity> points,
      Long projectId, Long tenantId, Long operatorId, int maxTotalPages) {

    List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity> basicPoints = points.stream()
        .filter(p -> com.guoyu.platform.courseware.entity.CwKnowledgePointEntity.CATEGORY_BASIC
            .equals(p.getCategory()))
        .sorted(java.util.Comparator.comparingInt(
            com.guoyu.platform.courseware.entity.CwKnowledgePointEntity::getSortNo))
        .toList();
    List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity> workPoints = points.stream()
        .filter(p -> com.guoyu.platform.courseware.entity.CwKnowledgePointEntity.CATEGORY_WORK
            .equals(p.getCategory()))
        .sorted(java.util.Comparator.comparingInt(
            com.guoyu.platform.courseware.entity.CwKnowledgePointEntity::getSortNo))
        .toList();
    Set<String> basicRefs = points.stream()
        .filter(p -> com.guoyu.platform.courseware.entity.CwKnowledgePointEntity.CATEGORY_BASIC
            .equals(p.getCategory()))
        .map(com.guoyu.platform.courseware.entity.CwKnowledgePointEntity::getSourceRef)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());
    Set<String> workRefs = points.stream()
        .filter(p -> com.guoyu.platform.courseware.entity.CwKnowledgePointEntity.CATEGORY_WORK
            .equals(p.getCategory()))
        .map(com.guoyu.platform.courseware.entity.CwKnowledgePointEntity::getSourceRef)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());

    // 构造知识点章块:基本要求一章 + 工作要求按等级各一章(等级按首见顺序)
    List<List<CwOutlinePageEntity>> knowledgeBlocks = new ArrayList<>();
    if (!basicPoints.isEmpty()) {
      knowledgeBlocks.add(buildKnowledgeChapter("基本要求", basicPoints, projectId, tenantId,
          operatorId));
    }
    java.util.LinkedHashMap<String, List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity>>
        byLevel = new java.util.LinkedHashMap<>();
    for (var p : workPoints) {
      byLevel.computeIfAbsent(p.getLevelName() != null ? p.getLevelName() : "工作要求",
          k -> new ArrayList<>()).add(p);
    }
    for (var entry : byLevel.entrySet()) {
      knowledgeBlocks.add(buildKnowledgeChapter("工作要求·" + entry.getKey(), entry.getValue(),
          projectId, tenantId, operatorId));
    }
    if (knowledgeBlocks.isEmpty()) {
      return pages;
    }

    // 识别并移除被替换章:按页计的重叠率 ≥60%(整章全含时全替换;
    // 模型常把章首页挂到范围外小节 ref,全含规则会漏,2026-09-21 电工冒烟实测)
    java.util.LinkedHashMap<Integer, List<CwOutlinePageEntity>> chapters = new java.util.LinkedHashMap<>();
    for (CwOutlinePageEntity page : pages) {
      if (page.getChapterNo() != null && page.getChapterNo() > 0) {
        chapters.computeIfAbsent(page.getChapterNo(), k -> new ArrayList<>()).add(page);
      }
    }
    Set<Integer> replacedNos = new java.util.HashSet<>();
    for (var entry : chapters.entrySet()) {
      int inRange = 0;
      int total = 0;
      for (CwOutlinePageEntity page : entry.getValue()) {
        java.util.List<String> refs = readRefs(page.getSourceRefs());
        if (refs.isEmpty()) {
          continue;
        }
        total++;
        if (basicRefs.containsAll(refs) || workRefs.containsAll(refs)) {
          inRange++;
        }
      }
      if (total > 0 && inRange * 5 >= total * 3) {
        replacedNos.add(entry.getKey());
      }
    }

    List<CwOutlinePageEntity> out = new ArrayList<>();
    boolean inserted = false;
    if (!replacedNos.isEmpty()) {
      for (CwOutlinePageEntity page : pages) {
        if (page.getChapterNo() != null && replacedNos.contains(page.getChapterNo())) {
          if (!inserted) {
            knowledgeBlocks.forEach(out::addAll);
            inserted = true;
          }
          continue; // 整章移除
        }
        out.add(page);
      }
    } else {
      // 兜底:review 页之前插入(从输入列表定位,review 缺失时追加到末尾)
      int insertAt = -1;
      for (int i = 0; i < pages.size(); i++) {
        if (CwOutlinePageEntity.ROLE_REVIEW.equals(pages.get(i).getPageRole())) {
          insertAt = i;
          break;
        }
      }
      if (insertAt < 0) {
        insertAt = pages.size();
      }
      out.addAll(pages.subList(0, insertAt));
      knowledgeBlocks.forEach(out::addAll);
      out.addAll(pages.subList(insertAt, pages.size()));
    }

    // 重新编章号 + position 连续化 + 总量截断(固定页 cover/toc 在前,review 必须留最后)
    List<CwOutlinePageEntity> capped = new ArrayList<>();
    for (CwOutlinePageEntity page : out) {
      if (CwOutlinePageEntity.ROLE_REVIEW.equals(page.getPageRole())) {
        continue;
      }
      if (capped.size() >= maxTotalPages - 1) {
        break;
      }
      capped.add(page);
    }
    out.stream().filter(p -> CwOutlinePageEntity.ROLE_REVIEW.equals(p.getPageRole())).findFirst()
        .ifPresent(capped::add);

    // 章号按 chapterTitle 首见顺序重排(cover/toc/review 保持 0);position 连续化
    java.util.LinkedHashMap<String, Integer> chapterOrder = new java.util.LinkedHashMap<>();
    for (int i = 0; i < capped.size(); i++) {
      CwOutlinePageEntity page = capped.get(i);
      if (page.getChapterNo() == null || page.getChapterNo() == 0
          || CwOutlinePageEntity.ROLE_REVIEW.equals(page.getPageRole())) {
        page.setChapterNo(0);
      } else {
        chapterOrder.putIfAbsent(page.getChapterTitle(), chapterOrder.size() + 1);
        page.setChapterNo(chapterOrder.get(page.getChapterTitle()));
      }
      page.setPosition(i + 1);
    }
    return capped;
  }

  /** 构造一个知识点章(section 章首页 + 逐知识点内容页) */
  private static List<CwOutlinePageEntity> buildKnowledgeChapter(
      String chapterTitle,
      List<com.guoyu.platform.courseware.entity.CwKnowledgePointEntity> chapterPoints,
      Long projectId, Long tenantId, Long operatorId) {
    List<CwOutlinePageEntity> block = new ArrayList<>();
    CwOutlinePageEntity section = newPage(projectId, tenantId, operatorId, 0, chapterTitle,
        CwOutlinePageEntity.ROLE_SECTION);
    section.setChapterTitle(chapterTitle);
    section.setChapterNo(1);
    section.setObjective("本章按最小知识点逐条精讲");
    section.setKeyPoints(writeJson(List.of()));
    section.setSourceRefs(writeJson(List.of()));
    section.setLayoutId(CoursewareLayout.SECTION);
    block.add(section);
    for (var point : chapterPoints) {
      String title = (point.getCode() != null && !point.getCode().isBlank()
          ? point.getCode() + " " : "") + point.getTitle();
      if (title.length() > 118) {
        title = title.substring(0, 118);
      }
      CwOutlinePageEntity page = newPage(projectId, tenantId, operatorId, 0, title,
          CwOutlinePageEntity.ROLE_CONTENT);
      page.setChapterTitle(chapterTitle);
      page.setChapterNo(1);
      page.setKnowledgePointId(point.getId());
      page.setKeyPoints(writeJson(List.of()));
      page.setSourceRefs(writeJson(
          point.getSourceRef() != null ? List.of(point.getSourceRef()) : List.of()));
      page.setLayoutId(CoursewareLayout.CONTENT_TABLE);
      block.add(page);
    }
    return block;
  }

  private static List<String> readRefs(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> out = new ArrayList<>();
      for (JsonNode node : new ObjectMapper().readTree(json)) {
        if (node.isTextual()) {
          out.add(node.asText());
        }
      }
      return out;
    } catch (Exception e) {
      return List.of();
    }
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
