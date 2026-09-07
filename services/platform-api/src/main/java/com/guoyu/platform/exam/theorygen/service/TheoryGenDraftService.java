package com.guoyu.platform.exam.theorygen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.theorygen.dto.TheoryDraftVO;
import com.guoyu.platform.exam.theorygen.dto.TheoryPublishReq;
import com.guoyu.platform.exam.theorygen.dto.TheoryPublishResultVO;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenBatchMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenDraftMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 草稿审核/查询/编辑 + 人工入库（publish）+ 知识点树打通。
 * 入库映射（方案 v1.1 §T4）：客观题 → task_exam(+_detail)；主观题 → task_examsub(+_detail)；
 * 难度写既有 nanyicd（"1"~"5"）；source=2；知识点按 course 挂接（course.level 0~4 = task level-1）。
 */
@Service
@Slf4j
public class TheoryGenDraftService {

  private static final int SOURCE_AI = 2;

  private final TheoryGenDraftMapper draftMapper;
  private final TheoryGenBatchMapper batchMapper;
  private final TheoryBlueprintMapper blueprintMapper;
  private final TheoryBlueprintNodeMapper nodeMapper;
  private final ExamTaskExamMapper taskExamMapper;
  private final ExamTaskExamDetailMapper taskExamDetailMapper;
  private final ExamTaskExamsubMapper taskExamsubMapper;
  private final ExamTaskExamsubDetailMapper taskExamsubDetailMapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final ExamCourseMapper courseMapper;
  private final ExamWorkTypeMapper workTypeMapper;
  private final ObjectMapper objectMapper;

  public TheoryGenDraftService(TheoryGenDraftMapper draftMapper, TheoryGenBatchMapper batchMapper,
      TheoryBlueprintMapper blueprintMapper, TheoryBlueprintNodeMapper nodeMapper,
      ExamTaskExamMapper taskExamMapper, ExamTaskExamDetailMapper taskExamDetailMapper,
      ExamTaskExamsubMapper taskExamsubMapper, ExamTaskExamsubDetailMapper taskExamsubDetailMapper,
      ExamKnowledgeStructureMapper knowledgeMapper, ExamCourseMapper courseMapper,
      ExamWorkTypeMapper workTypeMapper, ObjectMapper objectMapper) {
    this.draftMapper = draftMapper;
    this.batchMapper = batchMapper;
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.taskExamMapper = taskExamMapper;
    this.taskExamDetailMapper = taskExamDetailMapper;
    this.taskExamsubMapper = taskExamsubMapper;
    this.taskExamsubDetailMapper = taskExamsubDetailMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.courseMapper = courseMapper;
    this.workTypeMapper = workTypeMapper;
    this.objectMapper = objectMapper;
  }

  /** 草稿分页（按批次/题型/审核状态筛选） */
  @Transactional(readOnly = true)
  public PageResponse<TheoryDraftVO> page(Long tenantId, Long batchId, String questionType,
      String reviewStatus, Integer publishStatus, Long nodeId, long page, long pageSize) {
    LambdaQueryWrapper<TheoryGenDraftEntity> w = new LambdaQueryWrapper<TheoryGenDraftEntity>()
        .eq(TheoryGenDraftEntity::getTenantId, tenantId)
        .eq(batchId != null, TheoryGenDraftEntity::getBatchId, batchId)
        .eq(questionType != null && !questionType.isBlank(), TheoryGenDraftEntity::getQuestionType, questionType)
        .eq(reviewStatus != null && !reviewStatus.isBlank(), TheoryGenDraftEntity::getReviewStatus, reviewStatus)
        .eq(publishStatus != null, TheoryGenDraftEntity::getPublishStatus, publishStatus)
        .eq(nodeId != null, TheoryGenDraftEntity::getNodeId, nodeId)
        .orderByAsc(TheoryGenDraftEntity::getNodeId)
        .orderByAsc(TheoryGenDraftEntity::getId);
    Page<TheoryGenDraftEntity> p = draftMapper.selectPage(new Page<>(page, pageSize), w);
    return new PageResponse<>(
        p.getRecords().stream().map(TheoryDraftVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 编辑草稿（未入库的才可编辑） */
  @Transactional
  public TheoryDraftVO edit(Long tenantId, Long userId, Long draftId, TheoryGenDraftEntity patch) {
    TheoryGenDraftEntity d = getOwned(tenantId, draftId);
    if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "已入库草稿不可编辑");
    }
    if (patch.getContent() != null) {
      d.setContent(patch.getContent());
    }
    if (patch.getOptions() != null) {
      d.setOptions(patch.getOptions());
    }
    if (patch.getAnswer() != null) {
      d.setAnswer(patch.getAnswer());
    }
    if (patch.getAnalysis() != null) {
      d.setAnalysis(patch.getAnalysis());
    }
    if (patch.getDifficulty() != null) {
      if (patch.getDifficulty() < 1 || patch.getDifficulty() > 5) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "难度必须 1~5");
      }
      d.setDifficulty(patch.getDifficulty());
    }
    if (patch.getReferenceAnswer() != null) {
      d.setReferenceAnswer(patch.getReferenceAnswer());
    }
    if (patch.getScorePoints() != null) {
      d.setScorePoints(patch.getScorePoints());
    }
    if (patch.getScoringStandard() != null) {
      d.setScoringStandard(patch.getScoringStandard());
    }
    if (patch.getScore() != null) {
      d.setScore(patch.getScore());
    }
    d.setUpdatedBy(userId);
    d.setUpdatedAt(LocalDateTime.now());
    draftMapper.updateById(d);
    return TheoryDraftVO.from(d);
  }

  /** 批量审核：通过/驳回（已入库的不可再审） */
  @Transactional
  public int review(Long tenantId, Long userId, List<Long> draftIds, boolean approve) {
    int count = 0;
    for (Long id : draftIds) {
      TheoryGenDraftEntity d = getOwned(tenantId, id);
      if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
        continue;
      }
      d.setReviewStatus(approve ? TheoryGenDraftEntity.REVIEW_APPROVED : TheoryGenDraftEntity.REVIEW_REJECTED);
      d.setReviewBy(userId);
      d.setReviewAt(LocalDateTime.now());
      d.setUpdatedBy(userId);
      d.setUpdatedAt(LocalDateTime.now());
      draftMapper.updateById(d);
      count++;
    }
    return count;
  }

  /**
   * 人工入库：approved 且未入库的草稿 → task_exam(+_detail) / task_examsub(+_detail)。
   * 幂等：publish_status=1 跳过；事务内全量成功或回滚。
   */
  @Transactional
  public TheoryPublishResultVO publish(Long tenantId, Long userId, TheoryPublishReq req) {
    TheoryGenBatchEntity batch = batchMapper.selectById(req.batchId());
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    TheoryBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    List<TheoryGenDraftEntity> drafts = draftMapper.selectList(
        new LambdaQueryWrapper<TheoryGenDraftEntity>()
            .eq(TheoryGenDraftEntity::getTenantId, tenantId)
            .eq(TheoryGenDraftEntity::getBatchId, req.batchId())
            .eq(TheoryGenDraftEntity::getReviewStatus, TheoryGenDraftEntity.REVIEW_APPROVED));
    if (drafts == null || drafts.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "无审核通过的草稿可入库");
    }

    // 题量闸门（确认项 C1：低于下限二次确认 → 前端传 confirmed=true；审计留痕）
    int quotaTotal = quotaTotal(batch.getQuotaConfig()) * countPoints(tenantId, batch.getBlueprintId());
    int approvedCount = drafts.size();
    if (approvedCount < quotaTotal && !Boolean.TRUE.equals(req.confirmedBelowQuota())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          String.format("本批次计划生成 %d 题，目前审核通过的只有 %d 题【需二次确认】", quotaTotal, approvedCount));
    }

    // 知识点打通：先保证 course 存在
    Integer courseId = bp.getCourseId();
    if (courseId == null) {
      courseId = ensureCourse(tenantId, userId, bp);
      bp.setCourseId(courseId);
      blueprintMapper.updateById(bp);
    }

    // 查找/创建任务（客观/主观各一个）
    ExamTaskExamEntity examTask = resolveTask(tenantId, userId, bp, req,
        batch.getId(), true);
    ExamTaskExamsubEntity subTask = null;

    int published = 0;
    int skipped = 0;
    int objectiveCount = 0;
    int subjectiveCount = 0;
    // 考评点 → knowledge_id 缓存
    Map<Long, Long> knowledgeCache = new HashMap<>();

    for (TheoryGenDraftEntity d : drafts) {
      if (d.getPublishStatus() != null && d.getPublishStatus() == 1) {
        skipped++;
        continue;
      }
      Long knowledgeId = resolveKnowledge(tenantId, userId, bp, d, knowledgeCache);
      boolean subjective = isSubjective(d.getQuestionType());
      if (!subjective) {
        ExamTaskExamDetailEntity detail = new ExamTaskExamDetailEntity();
        detail.setTaskId(examTask.getId());
        detail.setExamType(chineseType(d.getQuestionType()));
        detail.setExamContent(truncate(d.getContent(), 300));
        fillOptions(detail, d);
        detail.setExamAnswer(truncate(d.getAnswer(), 20));
        detail.setNanyicd(String.valueOf(d.getDifficulty() == null ? 3 : d.getDifficulty()));
        detail.setKnowledgeId(knowledgeId);
        detail.setAiBatchId(batch.getId());
        detail.setSource(SOURCE_AI);
        detail.setIsblod(0);
        detail.setTenantId(tenantId.intValue());
        detail.setDeleted(0);
        detail.setCreateTime(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail.setCreatedBy(userId == null ? null : userId.intValue());
        detail.setUpdatedBy(userId == null ? null : userId.intValue());
        taskExamDetailMapper.insert(detail);
        d.setPublishedTaskType("exam");
        d.setPublishedTaskId(Long.valueOf(examTask.getId()));
        d.setPublishedDetailId(Long.valueOf(detail.getId()));
        objectiveCount++;
      } else {
        if (subTask == null) {
          subTask = resolveSubTask(tenantId, userId, bp, req, batch.getId());
        }
        ExamTaskExamsubDetailEntity detail = new ExamTaskExamsubDetailEntity();
        detail.setTaskId(subTask.getId());
        detail.setZhishidian(truncate(pointName(tenantId, d), 100));
        detail.setContent(truncate(d.getContent(), 500));
        detail.setAnswer(d.getReferenceAnswer());
        detail.setNanyicd(String.valueOf(d.getDifficulty() == null ? 3 : d.getDifficulty()));
        detail.setKnowledgeId(knowledgeId);
        detail.setAiBatchId(batch.getId());
        detail.setSource(SOURCE_AI);
        detail.setIsJisuan("cal".equals(d.getQuestionType()) ? 1 : 0);
        detail.setIsblod(0);
        detail.setTenantId(tenantId.intValue());
        detail.setDeleted(0);
        detail.setCreateTime(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail.setCreatedBy(userId == null ? null : userId.intValue());
        detail.setUpdatedBy(userId == null ? null : userId.intValue());
        // 新增列（迁移 V20260904_002）：exam_type / score_points / scoring_standard
        detail.setExamType(chineseType(d.getQuestionType()));
        detail.setScorePoints(d.getScorePoints());
        detail.setScoringStandard(d.getScoringStandard());
        taskExamsubDetailMapper.insert(detail);
        d.setPublishedTaskType("examsub");
        d.setPublishedTaskId(Long.valueOf(subTask.getId()));
        d.setPublishedDetailId(Long.valueOf(detail.getId()));
        subjectiveCount++;
      }
      d.setKnowledgeId(knowledgeId);
      d.setPublishStatus(1);
      d.setUpdatedBy(userId);
      d.setUpdatedAt(LocalDateTime.now());
      draftMapper.updateById(d);
      published++;
    }

    // 任务 nums 校正
    examTask.setNums(examTask.getNums() + objectiveCount);
    examTask.setUpdatedBy(userId == null ? null : userId.intValue());
    examTask.setUpdatedAt(LocalDateTime.now());
    taskExamMapper.updateById(examTask);
    if (subTask != null) {
      subTask.setNums(subTask.getNums() + subjectiveCount);
      subTask.setUpdatedBy(userId == null ? null : userId.intValue());
      subTask.setUpdatedAt(LocalDateTime.now());
      taskExamsubMapper.updateById(subTask);
    }

    if (approvedCount < quotaTotal) {
      log.warn("入库低于批次配额放行（已人工确认）：batchId={}, approved={}, quota={}", batch.getId(), approvedCount, quotaTotal);
    }
    log.info("理论草稿入库完成：batchId={}, published={}（客观{} 主观{}）, skipped={}",
        batch.getId(), published, objectiveCount, subjectiveCount, skipped);
    return new TheoryPublishResultVO(published, objectiveCount, subjectiveCount, skipped,
        Long.valueOf(examTask.getId()), subTask == null ? null : Long.valueOf(subTask.getId()));
  }

  /** 批次全量草稿（导出用） */
  @Transactional(readOnly = true)
  public List<TheoryGenDraftEntity> listAll(Long tenantId, Long batchId) {
    return draftMapper.selectList(new LambdaQueryWrapper<TheoryGenDraftEntity>()
        .eq(TheoryGenDraftEntity::getTenantId, tenantId)
        .eq(TheoryGenDraftEntity::getBatchId, batchId)
        .orderByAsc(TheoryGenDraftEntity::getNodeId)
        .orderByAsc(TheoryGenDraftEntity::getId));
  }

  // ─────────── 私有辅助 ───────────

  private boolean isSubjective(String questionType) {
    return "essay".equals(questionType) || "cal".equals(questionType) || "draw".equals(questionType)
        || "discuss".equals(questionType) || "case_analysis".equals(questionType);
  }

  private String chineseType(String questionType) {
    return switch (questionType) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "essay" -> "简答题";
      case "cal" -> "计算题";
      case "draw" -> "绘图题";
      case "discuss" -> "论述题";
      case "case_analysis" -> "案例分析题";
      default -> questionType;
    };
  }

  private void fillOptions(ExamTaskExamDetailEntity detail, TheoryGenDraftEntity d) {
    List<Map<String, String>> options = parseOptions(d.getOptions());
    int size = options.size();
    for (int i = 0; i < size && i < 8; i++) {
      String content = truncate(options.get(i).get("content"), 2000);
      switch (i) {
        case 0 -> detail.setExamA(content);
        case 1 -> detail.setExamB(content);
        case 2 -> detail.setExamC(content);
        case 3 -> detail.setExamD(content);
        case 4 -> detail.setExamE(content);
        case 5 -> detail.setExamF(content);
        case 6 -> detail.setExamG(content);
        case 7 -> detail.setExamH(content);
        default -> { }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, String>> parseOptions(String json) {
    if (json == null || json.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() { });
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  /** 查找既有待审核任务或新建（追加/新建两种模式，确认项已在方案 §T4 固化） */
  private ExamTaskExamEntity resolveTask(Long tenantId, Long userId, TheoryBlueprintEntity bp,
      TheoryPublishReq req, Long batchId, boolean objective) {
    if ("append".equals(req.taskMode()) && req.taskId() != null) {
      ExamTaskExamEntity t = taskExamMapper.selectById(req.taskId());
      if (t == null || !t.getTenantId().equals(tenantId)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "要追加的理论命题任务不存在");
      }
      return t;
    }
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setWorkTypeId(bp.getWorkTypeId());
    t.setLevel(bp.getLevel());
    t.setNums(0);
    t.setStatus(0);
    t.setAddtime(LocalDateTime.now());
    t.setTenantId(tenantId.intValue());
    t.setDeleted(0);
    t.setCreateTime(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t.setCreatedBy(userId == null ? null : userId.intValue());
    t.setUpdatedBy(userId == null ? null : userId.intValue());
    taskExamMapper.insert(t);
    return t;
  }

  private ExamTaskExamsubEntity resolveSubTask(Long tenantId, Long userId, TheoryBlueprintEntity bp,
      TheoryPublishReq req, Long batchId) {
    if ("append".equals(req.taskMode()) && req.subTaskId() != null) {
      ExamTaskExamsubEntity t = taskExamsubMapper.selectById(req.subTaskId());
      if (t == null || !t.getTenantId().equals(tenantId)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "要追加的主观命题任务不存在");
      }
      return t;
    }
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setWorkTypeId(bp.getWorkTypeId());
    t.setLevel(bp.getLevel());
    t.setNums(0);
    t.setStatus(0);
    t.setAddtime(LocalDateTime.now());
    t.setTenantId(tenantId.intValue());
    t.setDeleted(0);
    t.setCreateTime(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t.setCreatedBy(userId == null ? null : userId.intValue());
    t.setUpdatedBy(userId == null ? null : userId.intValue());
    taskExamsubMapper.insert(t);
    return t;
  }

  /** 考评点 → 知识点树节点（幂等：course_id + series_no(full_code) + level 匹配） */
  private Long resolveKnowledge(Long tenantId, Long userId, TheoryBlueprintEntity bp,
      TheoryGenDraftEntity d, Map<Long, Long> cache) {
    TheoryBlueprintNodeEntity point = nodeMapper.selectById(d.getNodeId());
    if (point == null) {
      return null;
    }
    return ensureKnowledgePath(tenantId, userId, bp, point, cache);
  }

  /** 沿考评点父链确保知识点树存在，返回考评点（L3）的 knowledge_id */
  private Long ensureKnowledgePath(Long tenantId, Long userId, TheoryBlueprintEntity bp,
      TheoryBlueprintNodeEntity point, Map<Long, Long> cache) {
    if (cache.containsKey(point.getId())) {
      return cache.get(point.getId());
    }
    // 自底向上收集链：point → ... → L1
    List<TheoryBlueprintNodeEntity> chain = new ArrayList<>();
    TheoryBlueprintNodeEntity cur = point;
    while (cur != null) {
      chain.add(0, cur);
      cur = cur.getParentId() != null && cur.getParentId() > 0
          ? nodeMapper.selectById(cur.getParentId()) : null;
    }
    Long parentId = 0L;
    Long pointKnowledgeId = null;
    for (TheoryBlueprintNodeEntity n : chain) {
      // 知识点树 level：node_level 1..4 → 0..3
      int ksLevel = n.getNodeLevel() - 1;
      String seriesNo = n.getFullCode();
      ExamKnowledgeStructureEntity exist = knowledgeMapper.selectOne(
          new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
              .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId)
              .eq(ExamKnowledgeStructureEntity::getCourseId, bp.getCourseId())
              .eq(ExamKnowledgeStructureEntity::getSeriesNo, seriesNo)
              .eq(ExamKnowledgeStructureEntity::getLevel, ksLevel)
              .eq(ExamKnowledgeStructureEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (exist != null) {
        parentId = Long.valueOf(exist.getId());
      } else {
        ExamKnowledgeStructureEntity entity = new ExamKnowledgeStructureEntity();
        entity.setTenantId(tenantId.intValue());
        entity.setType(0);
        entity.setCourseId(bp.getCourseId());
        entity.setParentId(parentId.intValue());
        entity.setTitle(truncate(n.getName(), 255));
        entity.setSeriesNo(seriesNo);
        entity.setLevel(ksLevel);
        entity.setImportant(n.getNodeLevel() == 4 ? n.getImportance() : null);
        entity.setStatus(1);
        entity.setSortNumber(n.getSortOrder());
        entity.setAmount(0);
        entity.setUserId(userId == null ? null : userId.intValue());
        entity.setCreateTime(LocalDateTime.now());
        entity.setDeleted(0);
        knowledgeMapper.insert(entity);
        parentId = Long.valueOf(entity.getId());
      }
      if (n.getNodeLevel() == 4) {
        pointKnowledgeId = parentId;
      }
    }
    // 回填 blueprint_node.knowledge_id
    if (pointKnowledgeId != null && point.getKnowledgeId() == null) {
      point.setKnowledgeId(pointKnowledgeId);
      point.setUpdatedAt(LocalDateTime.now());
      nodeMapper.updateById(point);
    }
    cache.put(point.getId(), pointKnowledgeId);
    return pointKnowledgeId;
  }

  /** 无科目时创建 course 行（title=工种名+等级，course.level = task level-1） */
  private Integer ensureCourse(Long tenantId, Long userId, TheoryBlueprintEntity bp) {
    ExamCourseEntity c = new ExamCourseEntity();
    ExamWorkTypeEntity wt = workTypeMapper.selectById(Long.valueOf(bp.getWorkTypeId()));
    String wtName = wt != null && wt.getTitle() != null ? wt.getTitle() : ("工种" + bp.getWorkTypeId());
    c.setTitle(truncate(wtName + TheoryBlueprintApplicationService.LEVEL_NAMES[bp.getLevel()], 255));
    c.setWorkTypeId(bp.getWorkTypeId());
    c.setLevel(5 - bp.getLevel());
    c.setStatus(1);
    c.setSortNumber(1);
    c.setTenantId(tenantId);
    c.setCreateTime(LocalDateTime.now());
    c.setDeleted(0);
    c.setUserId(userId == null ? null : userId.intValue());
    courseMapper.insert(c);
    log.info("知识点打通：新建科目 courseId={}, title={}", c.getId(), c.getTitle());
    return c.getId();
  }

  private String pointName(Long tenantId, TheoryGenDraftEntity d) {
    TheoryBlueprintNodeEntity n = nodeMapper.selectById(d.getNodeId());
    return n != null && n.getName() != null ? n.getName() : "";
  }

  private int quotaTotal(String quotaConfig) {
    if (quotaConfig == null || quotaConfig.isBlank()) {
      return 0;
    }
    try {
      Map<String, Integer> q = objectMapper.readValue(quotaConfig,
          new TypeReference<Map<String, Integer>>() { });
      return q.values().stream().mapToInt(Integer::intValue).sum();
    } catch (Exception e) {
      return 0;
    }
  }

  private int countPoints(Long tenantId, Long blueprintId) {
    Long c = nodeMapper.selectCount(new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
        .eq(TheoryBlueprintNodeEntity::getTenantId, tenantId)
        .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId)
        .eq(TheoryBlueprintNodeEntity::getNodeLevel, 4));
    return c == null ? 0 : c.intValue();
  }

  private TheoryGenDraftEntity getOwned(Long tenantId, Long draftId) {
    TheoryGenDraftEntity d = draftMapper.selectById(draftId);
    if (d == null || !d.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "草稿不存在");
    }
    return d;
  }

  private String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
