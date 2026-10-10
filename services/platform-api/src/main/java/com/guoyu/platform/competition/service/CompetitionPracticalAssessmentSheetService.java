package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssessmentSheetEntity;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssessmentSheetVO;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompetitionPracticalAssessmentSheetService {
  private static final String PRACTICAL = "practical";
  private final CompetitionStageApplicationService stageService;
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper;
  private final CompetitionMakeExamPaperQuestionMapper questionMapper;
  private final CompetitionPracticalAssessmentSheetMapper sheetMapper;
  private final ObjectMapper objectMapper;

  public CompetitionPracticalAssessmentSheetService(
      CompetitionStageApplicationService stageService,
      CompetitionMakeExamPaperSnapshotMapper snapshotMapper,
      CompetitionMakeExamPaperQuestionMapper questionMapper,
      CompetitionPracticalAssessmentSheetMapper sheetMapper,
      ObjectMapper objectMapper) {
    this.stageService = stageService;
    this.snapshotMapper = snapshotMapper;
    this.questionMapper = questionMapper;
    this.sheetMapper = sheetMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalAssessmentSheetVO> list(
      Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId) {
    var principal = SecurityContext.currentUser();
    var stage = stageService.get(stageId);
    if (!stage.getEventWorkTypeId().equals(eventWorkTypeId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "考评表工种与赛段不一致");
    }
    List<CompetitionPracticalAssessmentSheetEntity> rows = sheetMapper.selectList(
        new LambdaQueryWrapper<CompetitionPracticalAssessmentSheetEntity>()
            .eq(CompetitionPracticalAssessmentSheetEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionPracticalAssessmentSheetEntity::getStageId, stageId)
            .eq(CompetitionPracticalAssessmentSheetEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionPracticalAssessmentSheetEntity::getEventWorkTypeLevelId,
                eventWorkTypeLevelId)
            .eq(CompetitionPracticalAssessmentSheetEntity::getDeleted, 0)
            .orderByAsc(CompetitionPracticalAssessmentSheetEntity::getSheetCode));
    return (rows == null ? List.<CompetitionPracticalAssessmentSheetEntity>of() : rows)
        .stream().map(this::toVO).toList();
  }

  @Transactional
  public CompetitionPracticalAssessmentSheetEntity ensureForPaper(
      Long tenantId, Long userId, Long eventId, Long stageId, Long eventWorkTypeId,
      Long eventWorkTypeLevelId, Long paperSnapshotId) {
    CompetitionPracticalAssessmentSheetEntity existing = findForPaper(
        tenantId, stageId, eventWorkTypeId, eventWorkTypeLevelId, paperSnapshotId);
    if (existing != null) return existing;

    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotMapper.selectById(paperSnapshotId);
    if (snapshot == null || !tenantId.equals(snapshot.getTenantId())
        || !eventId.equals(snapshot.getEventId())
        || !eventWorkTypeId.equals(snapshot.getEventWorkTypeId())
        || !eventWorkTypeLevelId.equals(snapshot.getEventWorkTypeLevelId())
        || !PRACTICAL.equals(snapshot.getContentType())
        || !"published".equals(snapshot.getStatus())
        || Integer.valueOf(1).equals(snapshot.getDeleted())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只能为当前范围的已发布实操试卷生成考评表");
    }
    List<CompetitionMakeExamPaperQuestionEntity> questions = questionMapper.selectByPaper(
        tenantId, paperSnapshotId);
    if (questions == null || questions.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操试卷没有题目，无法生成考评表");
    }

    BigDecimal total = questions.stream()
        .map(CompetitionMakeExamPaperQuestionEntity::getScore)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() <= 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操试卷总分无效，无法生成考评表");
    }

    ObjectNode root = objectMapper.createObjectNode();
    root.put("template", "competition-practical-assessment-v1");
    root.put("paperSnapshotId", String.valueOf(paperSnapshotId));
    root.put("paperCode", snapshot.getPaperCode());
    root.put("paperTitle", snapshot.getTitle());
    root.put("totalScore", total);
    ArrayNode summary = root.putArray("summary");
    ArrayNode details = root.putArray("questions");
    for (CompetitionMakeExamPaperQuestionEntity question : questions) {
      ObjectNode item = summary.addObject();
      item.put("paperQuestionId", String.valueOf(question.getId()));
      item.put("sourceQuestionId", String.valueOf(question.getSourceQuestionId()));
      item.put("sortOrder", question.getSortOrder());
      item.put("title", question.getStem());
      item.put("score", question.getScore());
      item.put("weight", 100);

      ObjectNode detail = details.addObject();
      detail.setAll(item.deepCopy());
      detail.put("scoringMethod", question.getScoringMethod());
      if (question.getScoreDifference() != null) {
        detail.put("scoreDifference", question.getScoreDifference());
      }
      detail.set("detail", parseJson(question.getPracticalDetailJson(), "实操题明细快照"));
      detail.set("scoreItems", parseJson(question.getScoringItemsJson(), "实操题评分项快照"));
    }

    CompetitionPracticalAssessmentSheetEntity row = new CompetitionPracticalAssessmentSheetEntity();
    row.setId(IdWorker.getId());
    row.setTenantId(tenantId);
    row.setEventId(eventId);
    row.setStageId(stageId);
    row.setEventWorkTypeId(eventWorkTypeId);
    row.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    row.setPaperSnapshotId(paperSnapshotId);
    row.setVersionNo(1);
    row.setSheetCode(buildSheetCode(snapshot.getPaperCode(), paperSnapshotId));
    row.setTitle((StringUtils.hasText(snapshot.getTitle()) ? snapshot.getTitle() : "实操试卷")
        + "考评表");
    row.setQuestionCount(questions.size());
    row.setTotalScore(total);
    row.setSheetJson(root.toString());
    row.setSourceHash(snapshot.getSourceHash());
    row.setStatus("published");
    row.setCreatedBy(userId);
    row.setUpdatedBy(userId);
    row.setCreatedAt(LocalDateTime.now());
    row.setUpdatedAt(LocalDateTime.now());
    row.setDeleted(0);
    if (sheetMapper.insert(row) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考评表生成失败");
    }
    return row;
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalAssessmentSheetEntity findForPaper(
      Long tenantId, Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId,
      Long paperSnapshotId) {
    return sheetMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalAssessmentSheetEntity>()
        .eq(CompetitionPracticalAssessmentSheetEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalAssessmentSheetEntity::getStageId, stageId)
        .eq(CompetitionPracticalAssessmentSheetEntity::getEventWorkTypeId, eventWorkTypeId)
        .eq(CompetitionPracticalAssessmentSheetEntity::getEventWorkTypeLevelId,
            eventWorkTypeLevelId)
        .eq(CompetitionPracticalAssessmentSheetEntity::getPaperSnapshotId, paperSnapshotId)
        .eq(CompetitionPracticalAssessmentSheetEntity::getStatus, "published")
        .eq(CompetitionPracticalAssessmentSheetEntity::getDeleted, 0)
        .orderByDesc(CompetitionPracticalAssessmentSheetEntity::getVersionNo)
        .last("LIMIT 1"));
  }

  public CompetitionPracticalAssessmentSheetVO toVO(
      CompetitionPracticalAssessmentSheetEntity row) {
    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotMapper.selectById(
        row.getPaperSnapshotId());
    return new CompetitionPracticalAssessmentSheetVO(
        id(row.getId()), id(row.getEventId()), id(row.getStageId()),
        id(row.getEventWorkTypeId()), id(row.getEventWorkTypeLevelId()),
        id(row.getPaperSnapshotId()), snapshot == null ? null : snapshot.getPaperCode(),
        row.getVersionNo(), row.getSheetCode(), row.getTitle(), row.getQuestionCount(),
        row.getTotalScore(), row.getStatus(), parseJson(row.getSheetJson(), "考评表"));
  }

  /**
   * The paper code is a display label (normally A/B) and is not guaranteed to
   * be unique across paper snapshots. The snapshot id is the immutable
   * identity that keeps two assessment sheets distinguishable even when the
   * source generator reused the same paper code.
   */
  static String buildSheetCode(String paperCode, Long paperSnapshotId) {
    String prefix = StringUtils.hasText(paperCode) ? paperCode.trim() : String.valueOf(paperSnapshotId);
    return prefix + "-KP-" + paperSnapshotId;
  }

  private JsonNode parseJson(String value, String label) {
    if (!StringUtils.hasText(value)) return objectMapper.createObjectNode();
    try {
      return objectMapper.readTree(value);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.CONFLICT, label + "数据损坏");
    }
  }

  private static String id(Long value) {
    return value == null ? null : String.valueOf(value);
  }
}
