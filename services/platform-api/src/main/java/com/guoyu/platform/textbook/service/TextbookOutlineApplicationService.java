package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder.BuildResult;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder.OutlineNodeDraft;
import com.guoyu.platform.textbook.domain.TextbookWordAllocator;
import com.guoyu.platform.textbook.dto.OutlineNodeCreateReq;
import com.guoyu.platform.textbook.dto.OutlineNodeUpdateReq;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.vo.OutlineNodeVO;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教材大纲编排：节点增删改、上移/下移、合计校验、骨架重建（保留人工调整，设计 §2.4-⑤/⑥）。
 */
@Service
public class TextbookOutlineApplicationService {

  private static final Logger log = LoggerFactory.getLogger(TextbookOutlineApplicationService.class);

  public static final int LEVEL_PART = 1;
  public static final int LEVEL_CHAPTER = 2;
  public static final int LEVEL_SECTION = 3;
  public static final int LEVEL_POINT = 4;

  private final TextbookOutlineNodeMapper nodeMapper;
  private final TextbookProjectMapper projectMapper;
  private final ObjectMapper objectMapper;

  public TextbookOutlineApplicationService(TextbookOutlineNodeMapper nodeMapper,
      TextbookProjectMapper projectMapper, ObjectMapper objectMapper) {
    this.nodeMapper = nodeMapper;
    this.projectMapper = projectMapper;
    this.objectMapper = objectMapper;
  }

  // ============================================================
  // 查询
  // ============================================================

  @Transactional(readOnly = true)
  public List<OutlineNodeVO> outline(Long projectId) {
    findProject(projectId);
    List<TextbookOutlineNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
            .orderByAsc(TextbookOutlineNodeEntity::getParentId)
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    List<OutlineNodeVO> result = new ArrayList<>();
    for (TextbookOutlineNodeEntity node : nodes) {
      result.add(toVO(node));
    }
    return result;
  }

  /** 合计校验：章合计 = 总字数；有节子级的章再校验 节合计 = 章字数 */
  @Transactional(readOnly = true)
  public OutlineSummaryVO summary(TextbookProjectEntity project) {
    List<TextbookOutlineNodeEntity> nodes = allNodes(project.getId());
    long chapterSum = 0;
    Map<Long, List<TextbookOutlineNodeEntity>> byParent = new HashMap<>();
    List<TextbookOutlineNodeEntity> chapters = new ArrayList<>();
    for (TextbookOutlineNodeEntity node : nodes) {
      byParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
      if (node.getNodeLevel() != null && node.getNodeLevel() == LEVEL_CHAPTER) {
        chapters.add(node);
        chapterSum += node.getTargetWords() == null ? 0 : node.getTargetWords();
      }
    }
    long totalWords = project.getTotalWords() == null ? 0 : project.getTotalWords();
    List<String> unbalanced = new ArrayList<>();
    for (TextbookOutlineNodeEntity chapter : chapters) {
      List<TextbookOutlineNodeEntity> sections = byParent.getOrDefault(chapter.getId(), List.of());
      if (sections.isEmpty()) {
        continue;
      }
      long sectionSum = 0;
      for (TextbookOutlineNodeEntity section : sections) {
        sectionSum += section.getTargetWords() == null ? 0 : section.getTargetWords();
      }
      long chapterWords = chapter.getTargetWords() == null ? 0 : chapter.getTargetWords();
      if (sectionSum != chapterWords) {
        unbalanced.add(chapter.getTitle());
      }
    }
    return new OutlineSummaryVO(chapterSum, totalWords, chapterSum == totalWords, unbalanced);
  }

  // ============================================================
  // 节点编辑
  // ============================================================

  /** 改标题/目标字数（置 words_manual=1）/排序；返回更新后的合计校验 */
  @Transactional
  public OutlineSummaryVO updateNode(Long nodeId, OutlineNodeUpdateReq req) {
    TextbookOutlineNodeEntity node = findNode(nodeId);
    if (req.title() != null && !req.title().isBlank()) {
      node.setTitle(req.title().trim());
    }
    if (req.targetWords() != null) {
      if (node.getNodeLevel() != LEVEL_CHAPTER && node.getNodeLevel() != LEVEL_SECTION) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "仅章/节可设置目标字数");
      }
      node.setTargetWords(req.targetWords());
      node.setWordsManual(1);
    }
    if (req.sortNo() != null) {
      node.setSortNo(req.sortNo());
    }
    nodeMapper.updateById(node);
    TextbookProjectEntity project = findProject(node.getProjectId());
    return summary(project);
  }

  /** 人工新增子节点：父为章→节，父为节→点（dimension 必填）；source_no 恒空 */
  @Transactional
  public OutlineNodeVO createNode(OutlineNodeCreateReq req) {
    findProject(req.projectId());
    TextbookOutlineNodeEntity parent = findNode(req.parentId());
    if (!parent.getProjectId().equals(req.projectId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "父节点不属于该工程");
    }
    TextbookOutlineNodeEntity node = new TextbookOutlineNodeEntity();
    node.setProjectId(req.projectId());
    node.setParentId(req.parentId());
    node.setTitleRaw(req.title().trim());
    node.setTitle(req.title().trim());
    node.setSourceNo(null);
    node.setWeight(null);
    if (parent.getNodeLevel() == LEVEL_CHAPTER) {
      node.setNodeLevel(LEVEL_SECTION);
      node.setDimension(null);
      node.setSortNo(nextSortNo(req.projectId(), req.parentId()));
      if (req.targetWords() != null) {
        node.setTargetWords(req.targetWords());
        node.setWordsManual(1);
      }
    } else if (parent.getNodeLevel() == LEVEL_SECTION) {
      node.setNodeLevel(LEVEL_POINT);
      node.setDimension(normalizeDimension(req.dimension()));
      node.setSortNo(nextSortNo(req.projectId(), req.parentId()));
      node.setWordsManual(0);
    } else {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能在章下新增节、节下新增知识点/技能点");
    }
    nodeMapper.insert(node);
    return toVO(node);
  }

  /** 软删节点及其子孙（同事务） */
  @Transactional
  public void deleteNode(Long nodeId) {
    TextbookOutlineNodeEntity node = findNode(nodeId);
    List<Long> ids = new ArrayList<>();
    collectDescendants(nodeId, ids);
    ids.add(nodeId);
    nodeMapper.deleteBatchIds(ids);
    log.info("[教材] 删除大纲节点（含子孙 {} 个）：nodeId={}", ids.size(), nodeId);
  }

  /** 上移/下移：与相邻兄弟交换 sort_no（已在顶端/底端则不动） */
  @Transactional
  public void moveNode(Long nodeId, boolean up) {
    TextbookOutlineNodeEntity node = findNode(nodeId);
    List<TextbookOutlineNodeEntity> siblings = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, node.getProjectId())
            .eq(TextbookOutlineNodeEntity::getParentId, node.getParentId())
            .orderByAsc(TextbookOutlineNodeEntity::getSortNo)
            .orderByAsc(TextbookOutlineNodeEntity::getId));
    int idx = -1;
    for (int i = 0; i < siblings.size(); i++) {
      if (siblings.get(i).getId().equals(nodeId)) {
        idx = i;
        break;
      }
    }
    int target = up ? idx - 1 : idx + 1;
    if (idx < 0 || target < 0 || target >= siblings.size()) {
      return;
    }
    TextbookOutlineNodeEntity other = siblings.get(target);
    Integer a = node.getSortNo();
    node.setSortNo(other.getSortNo());
    other.setSortNo(a);
    nodeMapper.updateById(node);
    nodeMapper.updateById(other);
  }

  // ============================================================
  // 骨架重建（设计 §2.4-⑤）：保留 words_manual=1 节点，合计仍守恒
  // ============================================================

  /**
   * 按标准重建骨架 + 配比。
   *
   * <p>保留规则：words_manual=1 的章/节按 source_no 与新骨架匹配则整节点保留（字数不动）；
   * 人工新增节点（source_no 空）父级仍在则保留并重挂，否则删除并告警；
   * 章级人工节点只保字数不保结构（其下节按标准重建，人工节仍按上规则保留）。
   * 人工字数先从总盘子扣除，剩余按权重/条数最大余数法分摊，合计严格 = total_words。
   *
   * @return 重建期告警（不生章原因/均分兜底等），由调用方并入工程 parse_warnings
   */
  @Transactional
  public List<String> regenerate(TextbookProjectEntity project, ParsedStandard parsed,
      boolean preserveManual) {
    int parserLevelCode = 5 - project.getLevelCode();
    List<Integer> functionNos = readFunctionNos(project.getFunctionNosJson());
    BuildResult built = TextbookOutlineBuilder.build(parsed, project.getDirection(),
        parserLevelCode, functionNos);
    List<String> warnings = new ArrayList<>(built.warnings());
    List<OutlineNodeDraft> drafts = built.nodes();

    // 现网节点（保留判断用）
    List<TextbookOutlineNodeEntity> existing = preserveManual ? allNodes(project.getId())
        : List.of();
    Map<String, TextbookOutlineNodeEntity> manualChapterBySource = new LinkedHashMap<>();
    Map<String, TextbookOutlineNodeEntity> manualSectionBySource = new LinkedHashMap<>();
    List<TextbookOutlineNodeEntity> manualOrphans = new ArrayList<>();
    for (TextbookOutlineNodeEntity node : existing) {
      if (!Integer.valueOf(1).equals(node.getWordsManual())) {
        continue;
      }
      if (node.getNodeLevel() == LEVEL_CHAPTER && node.getSourceNo() != null) {
        manualChapterBySource.put(node.getSourceNo(), node);
      } else if (node.getNodeLevel() == LEVEL_SECTION && node.getSourceNo() != null) {
        manualSectionBySource.put(node.getSourceNo(), node);
      } else {
        manualOrphans.add(node);
      }
    }

    // 新实体装配：draft → entity，预分配 id；保留节点复用旧实体
    List<TextbookOutlineNodeEntity> toInsert = new ArrayList<>();
    List<TextbookOutlineNodeEntity> toUpdate = new ArrayList<>();
    Map<Integer, Long> draftId = new HashMap<>();
    Map<Long, TextbookOutlineNodeEntity> entityById = new HashMap<>();
    Map<String, Long> sourceToNewId = new HashMap<>();
    Map<Integer, TextbookOutlineNodeEntity> chapterEntity = new HashMap<>();
    Map<Integer, TextbookOutlineNodeEntity> sectionEntity = new HashMap<>();
    long manualChapterWords = 0;
    for (int i = 0; i < drafts.size(); i++) {
      OutlineNodeDraft d = drafts.get(i);
      TextbookOutlineNodeEntity entity;
      boolean keptChapter = d.nodeLevel() == LEVEL_CHAPTER
          && manualChapterBySource.containsKey(d.sourceNo());
      boolean keptSection = d.nodeLevel() == LEVEL_SECTION
          && manualSectionBySource.containsKey(d.sourceNo());
      if (keptChapter) {
        entity = manualChapterBySource.get(d.sourceNo());
        manualChapterWords += entity.getTargetWords() == null ? 0 : entity.getTargetWords();
        chapterEntity.put(i, entity);
      } else if (keptSection) {
        entity = manualSectionBySource.get(d.sourceNo());
        sectionEntity.put(i, entity);
      } else {
        entity = newEntity(project, d);
        toInsert.add(entity);
      }
      draftId.put(i, entity.getId());
      entityById.put(entity.getId(), entity);
      if (d.sourceNo() != null) {
        sourceToNewId.put(d.sourceNo(), entity.getId());
      }
    }

    // 章级配比：总盘子先扣人工章字数
    int totalWords = project.getTotalWords() == null ? 0 : project.getTotalWords();
    List<Integer> autoChapterIdx = new ArrayList<>();
    List<java.math.BigDecimal> autoWeights = new ArrayList<>();
    for (int i = 0; i < drafts.size(); i++) {
      OutlineNodeDraft d = drafts.get(i);
      if (d.nodeLevel() == LEVEL_CHAPTER && !chapterEntity.containsKey(i)) {
        autoChapterIdx.add(i);
        autoWeights.add(d.weight());
      }
    }
    int allocatable = totalWords - (int) Math.min(manualChapterWords, Integer.MAX_VALUE);
    if (allocatable < 0) {
      warnings.add("WARN: 人工调整章字数合计已超过总字数，自动章分盘为 0，请在页面调平");
      allocatable = 0;
    }
    int[] autoChapterWords = TextbookWordAllocator.allocateChapters(allocatable, autoWeights,
        warnings);
    Map<Integer, Integer> chapterWords = new HashMap<>();
    for (int k = 0; k < autoChapterIdx.size(); k++) {
      chapterWords.put(autoChapterIdx.get(k), autoChapterWords[k]);
    }

    // 节级配比：章预算 = 人工章原字数 / 自动章分盘；先扣人工节字数，余按条数分摊
    Map<Integer, List<Integer>> sectionIdxByChapter = new HashMap<>();
    for (int i = 0; i < drafts.size(); i++) {
      if (drafts.get(i).nodeLevel() == LEVEL_SECTION) {
        sectionIdxByChapter.computeIfAbsent(drafts.get(i).parentIndex(), k -> new ArrayList<>())
            .add(i);
      }
    }
    Map<Integer, Integer> sectionWords = new HashMap<>();
    Map<Integer, Integer> pointCountBySection = countPoints(drafts);
    for (Map.Entry<Integer, List<Integer>> e : sectionIdxByChapter.entrySet()) {
      int chapterIdx = e.getKey();
      TextbookOutlineNodeEntity chapter = chapterEntity.get(chapterIdx);
      int budget = chapterWords.containsKey(chapterIdx)
          ? chapterWords.get(chapterIdx)
          : (chapter.getTargetWords() == null ? 0 : chapter.getTargetWords());
      List<Integer> sectionIdxs = e.getValue();
      List<Integer> autoSectionIdx = new ArrayList<>();
      int keptSectionWords = 0;
      for (int si : sectionIdxs) {
        if (sectionEntity.containsKey(si)) {
          TextbookOutlineNodeEntity kept = sectionEntity.get(si);
          keptSectionWords += kept.getTargetWords() == null ? 0 : kept.getTargetWords();
        } else {
          autoSectionIdx.add(si);
        }
      }
      int remaining = budget - keptSectionWords;
      if (remaining < 0) {
        warnings.add("WARN: 章「" + chapter.getTitle() + "」人工节字数合计超过章字数，剩余节分盘为 0");
        remaining = 0;
      }
      int[] counts = new int[autoSectionIdx.size()];
      for (int k = 0; k < autoSectionIdx.size(); k++) {
        counts[k] = pointCountBySection.getOrDefault(autoSectionIdx.get(k), 0);
      }
      int[] allocated = TextbookWordAllocator.allocateSections(remaining, counts, warnings);
      for (int k = 0; k < autoSectionIdx.size(); k++) {
        sectionWords.put(autoSectionIdx.get(k), allocated[k]);
      }
    }

    // 落字数 + 父子关系
    for (int i = 0; i < drafts.size(); i++) {
      OutlineNodeDraft d = drafts.get(i);
      TextbookOutlineNodeEntity entity = entityById.get(draftId.get(i));
      if (d.nodeLevel() == LEVEL_CHAPTER && !chapterEntity.containsKey(i)) {
        entity.setTargetWords(chapterWords.get(i));
        entity.setWeight(d.weight());
      } else if (d.nodeLevel() == LEVEL_SECTION && !sectionEntity.containsKey(i)) {
        entity.setTargetWords(sectionWords.get(i));
      }
      Long parentId = d.parentIndex() < 0 ? 0L : draftId.get(d.parentIndex());
      if (!Objects.equals(entity.getParentId(), parentId)) {
        entity.setParentId(parentId);
        if (!toInsert.contains(entity)) {
          toUpdate.add(entity);
        }
      }
    }

    // 人工新增节点（source_no 空）：父级仍在则重挂保留，否则删除
    List<Long> toDelete = new ArrayList<>();
    for (TextbookOutlineNodeEntity node : existing) {
      boolean kept = chapterEntity.containsValue(node) || sectionEntity.containsValue(node);
      if (kept) {
        continue;
      }
      if (manualOrphans.contains(node)) {
        Long newParent = sourceToNewId.get(parentSourceNo(node, existing));
        if (newParent != null) {
          node.setParentId(newParent);
          toUpdate.add(node);
        } else {
          warnings.add("WARN: 人工新增节点「" + node.getTitle() + "」的父级已不在新骨架中，已删除");
          toDelete.add(node.getId());
        }
      } else {
        toDelete.add(node.getId());
      }
    }

    if (!toDelete.isEmpty()) {
      nodeMapper.deleteBatchIds(toDelete);
    }
    for (TextbookOutlineNodeEntity entity : toUpdate) {
      nodeMapper.updateById(entity);
    }
    // 批量插入：骨架重建一次可达数百节点，逐行 insert 在弱网/高延迟链路上往返过多
    if (!toInsert.isEmpty()) {
      com.baomidou.mybatisplus.extension.toolkit.Db.saveBatch(toInsert, 200);
    }
    log.info("[教材] 骨架重建：projectId={} 新增节点={} 保留人工节点={} 删除={}",
        project.getId(), toInsert.size(), chapterEntity.size() + sectionEntity.size(),
        toDelete.size());
    return warnings;
  }

  // ============================================================
  // 内部工具
  // ============================================================

  private Map<Integer, Integer> countPoints(List<OutlineNodeDraft> drafts) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (OutlineNodeDraft d : drafts) {
      if (d.nodeLevel() == LEVEL_POINT) {
        counts.merge(d.parentIndex(), 1, Integer::sum);
      }
    }
    return counts;
  }

  /** 旧父节点的 source_no（重挂人工新增节点用）；父为篇（sourceNo=2/3）或已删节点均返回 null */
  private String parentSourceNo(TextbookOutlineNodeEntity node,
      List<TextbookOutlineNodeEntity> existing) {
    for (TextbookOutlineNodeEntity candidate : existing) {
      if (candidate.getId().equals(node.getParentId())) {
        return candidate.getSourceNo();
      }
    }
    return null;
  }

  private TextbookOutlineNodeEntity newEntity(TextbookProjectEntity project,
      OutlineNodeDraft d) {
    TextbookOutlineNodeEntity entity = new TextbookOutlineNodeEntity();
    entity.setId(IdWorker.getId());
    entity.setProjectId(project.getId());
    entity.setParentId(0L);
    entity.setNodeLevel(d.nodeLevel());
    entity.setDimension(d.dimension());
    entity.setSourceNo(d.sourceNo());
    entity.setTitleRaw(d.titleRaw());
    entity.setTitle(d.title());
    entity.setWeight(d.weight());
    entity.setTargetWords(null);
    entity.setWordsManual(0);
    entity.setSortNo(d.sortNo());
    return entity;
  }

  private int nextSortNo(Long projectId, Long parentId) {
    List<TextbookOutlineNodeEntity> siblings = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getProjectId, projectId)
            .eq(TextbookOutlineNodeEntity::getParentId, parentId));
    int max = 0;
    for (TextbookOutlineNodeEntity sibling : siblings) {
      max = Math.max(max, sibling.getSortNo() == null ? 0 : sibling.getSortNo());
    }
    return max + 1;
  }

  private void collectDescendants(Long parentId, List<Long> ids) {
    List<TextbookOutlineNodeEntity> children = nodeMapper.selectList(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getParentId, parentId));
    for (TextbookOutlineNodeEntity child : children) {
      ids.add(child.getId());
      collectDescendants(child.getId(), ids);
    }
  }

  private List<TextbookOutlineNodeEntity> allNodes(Long projectId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
        .eq(TextbookOutlineNodeEntity::getProjectId, projectId));
  }

  private TextbookProjectEntity findProject(Long projectId) {
    TextbookProjectEntity project = projectMapper.selectOne(
        new LambdaQueryWrapper<TextbookProjectEntity>()
            .eq(TextbookProjectEntity::getId, projectId));
    if (project == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "教材工程不存在");
    }
    return project;
  }

  private TextbookOutlineNodeEntity findNode(Long nodeId) {
    TextbookOutlineNodeEntity node = nodeMapper.selectOne(
        new LambdaQueryWrapper<TextbookOutlineNodeEntity>()
            .eq(TextbookOutlineNodeEntity::getId, nodeId));
    if (node == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "大纲节点不存在");
    }
    return node;
  }

  private String normalizeDimension(String dimension) {
    if (!"skill".equals(dimension)) {
      if (dimension == null || dimension.isBlank() || !"theory".equals(dimension)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "点的维度仅支持 theory/skill");
      }
      return "theory";
    }
    return "skill";
  }

  public List<Integer> readFunctionNos(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Integer>>() {
      });
    } catch (Exception e) {
      log.warn("[教材] function_nos_json 解析失败: {}, json={}", e.getMessage(), json);
      return List.of();
    }
  }

  public static OutlineNodeVO toVO(TextbookOutlineNodeEntity node) {
    return new OutlineNodeVO(
        node.getId() == null ? null : String.valueOf(node.getId()),
        node.getProjectId() == null ? null : String.valueOf(node.getProjectId()),
        node.getParentId() == null ? null : String.valueOf(node.getParentId()),
        node.getNodeLevel(),
        node.getDimension(),
        node.getSourceNo(),
        node.getTitleRaw(),
        node.getTitle(),
        node.getWeight(),
        node.getTargetWords(),
        node.getWordsManual(),
        node.getSortNo());
  }
}
