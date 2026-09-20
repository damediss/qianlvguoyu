package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionAssessmentContentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionAssessmentContentMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 竞赛题目批量落库边界。任何一条失败都会回滚整个批次。 */
@Service
public class CompetitionQuestionImportPersistenceService {

  private static final Safelist PRACTICAL_HTML = Safelist.relaxed()
      .addTags("u", "s")
      .addProtocols("a", "href", "http", "https", "mailto")
      .removeTags("img");

  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper detailMapper;
  private final CompetitionPracticalQuestionAssessmentContentMapper contentMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;
  private final CompetitionQuestionBlueprintBindingMapper bindingMapper;

  @Autowired
  public CompetitionQuestionImportPersistenceService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper detailMapper,
      CompetitionPracticalQuestionAssessmentContentMapper contentMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionQuestionBlueprintBindingMapper bindingMapper) {
    this.questionMapper = questionMapper;
    this.detailMapper = detailMapper;
    this.contentMapper = contentMapper;
    this.scoreItemMapper = scoreItemMapper;
    this.bindingMapper = bindingMapper;
  }

  public CompetitionQuestionImportPersistenceService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper detailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper) {
    this(questionMapper, detailMapper, null, scoreItemMapper, null);
  }

  /** Constructor retained for tests/extensions that already provide structured assessment storage. */
  public CompetitionQuestionImportPersistenceService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper detailMapper,
      CompetitionPracticalQuestionAssessmentContentMapper contentMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper) {
    this(questionMapper, detailMapper, contentMapper, scoreItemMapper, null);
  }

  @Transactional
  public void persist(
      Long tenantId,
      Long userId,
      Long eventId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      Long batchId,
      List<CompetitionQuestionWorkbookParser.ParsedQuestion> parsedQuestions) {
    persist(tenantId, userId, eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType,
        batchId, parsedQuestions, List.of());
  }

  @Transactional
  public void persist(
      Long tenantId,
      Long userId,
      Long eventId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      Long batchId,
      List<CompetitionQuestionWorkbookParser.ParsedQuestion> parsedQuestions,
      List<Binding> bindings) {
    Map<Integer, Binding> bindingByIndex = new HashMap<>();
    if (bindings != null) {
      for (Binding binding : bindings) {
        if (binding != null) bindingByIndex.put(binding.questionIndex(), binding);
      }
    }
    for (int questionIndex = 0; questionIndex < parsedQuestions.size(); questionIndex++) {
      CompetitionQuestionWorkbookParser.ParsedQuestion parsed = parsedQuestions.get(questionIndex);
      CompetitionQuestionEntity question = new CompetitionQuestionEntity();
      question.setId(IdWorker.getId());
      question.setTenantId(tenantId);
      question.setEventId(eventId);
      question.setEventWorkTypeId(eventWorkTypeId);
      question.setEventWorkTypeLevelId(eventWorkTypeLevelId);
      question.setContentType(contentType);
      question.setQuestionType(parsed.questionType());
      question.setSortOrder(parsed.sortOrder());
      question.setScore("practical".equals(contentType) && parsed.totalScore() != null
          ? parsed.totalScore() : BigDecimal.ONE);
      question.setStem(parsed.stem());
      question.setReferenceAnswer(parsed.referenceAnswer());
      question.setFillAnswer(parsed.fillAnswer());
      question.setOptionsJson(parsed.optionsJson());
      question.setDifficulty(parsed.difficulty());
      question.setStatus("enabled");
      question.setSourceBatchId(batchId);
      question.setCreatedBy(userId);
      question.setUpdatedBy(userId);
      question.setDeleted(0);
      if (questionMapper.insert(question) != 1) {
        throw new com.guoyu.platform.common.exception.BusinessException(
            com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "竞赛试题保存失败");
      }

      Binding binding = bindingByIndex.get(questionIndex);
      if (binding != null) {
        if (bindingMapper == null) {
          throw new com.guoyu.platform.common.exception.BusinessException(
              com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "题目绑定服务未就绪");
        }
        CompetitionQuestionBlueprintBindingEntity row = new CompetitionQuestionBlueprintBindingEntity();
        row.setId(IdWorker.getId());
        row.setTenantId(tenantId);
        row.setQuestionId(question.getId());
        row.setBlueprintId(binding.blueprintId());
        row.setBlueprintNodeId(binding.blueprintNodeId());
        row.setMatchMode(binding.matchMode());
        row.setSourceBatchId(batchId);
        row.setCreatedBy(userId);
        row.setUpdatedBy(userId);
        row.setDeleted(0);
        if (bindingMapper.insert(row) != 1) {
          throw new com.guoyu.platform.common.exception.BusinessException(
              com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "题目考评点绑定保存失败");
        }
      }

      if ("practical".equals(contentType)) {
        CompetitionPracticalQuestionDetailEntity detail = new CompetitionPracticalQuestionDetailEntity();
        detail.setId(IdWorker.getId());
        detail.setTenantId(tenantId);
        detail.setQuestionId(question.getId());
        detail.setDurationMinutes(parsed.durationMinutes() == null ? 60 : parsed.durationMinutes());
        detail.setScoreDifference(parsed.scoreDifference() == null ? new BigDecimal("10") : parsed.scoreDifference());
        detail.setScoringMethod(parsed.scoringMethod() == null || parsed.scoringMethod().isBlank()
            ? "deductive" : parsed.scoringMethod());
        BigDecimal totalScore = sumScoreItems(parsed.scoreItems());
        detail.setTotalScore(parsed.totalScore() == null ? totalScore : parsed.totalScore());
        detail.setAssessmentRequirementsHtml(sanitize(parsed.assessmentRequirementsHtml() == null
            ? parsed.operationContent() : parsed.assessmentRequirementsHtml()));
        detail.setExamRoomPrepareHtml(sanitize(parsed.examRoomPrepareHtml() == null
            ? parsed.equipmentPrepare() : parsed.examRoomPrepareHtml()));
        detail.setCandidatePrepareHtml(sanitize(parsed.candidatePrepareHtml() == null
            ? parsed.studentPrepare() : parsed.candidatePrepareHtml()));
        detail.setNegativeItemsHtml(sanitize(parsed.negativeItemsHtml()));
        detail.setQuestionMaterialsHtml(sanitize(parsed.questionMaterialsHtml()));
        detail.setOperationContent(detail.getAssessmentRequirementsHtml());
        detail.setStudentPrepare(detail.getCandidatePrepareHtml());
        detail.setEquipmentPrepare(detail.getExamRoomPrepareHtml());
        detail.setCreatedBy(userId);
        detail.setUpdatedBy(userId);
        detail.setDeleted(0);
        if (detailMapper.insert(detail) != 1) {
          throw new com.guoyu.platform.common.exception.BusinessException(
              com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "实操题详情保存失败");
        }

        List<CompetitionQuestionWorkbookParser.ParsedAssessmentContent> groups = parsed.assessmentContents();
        if (groups == null || groups.isEmpty()) {
          groups = List.of(new CompetitionQuestionWorkbookParser.ParsedAssessmentContent(1,
              parsed.operationContent() == null || parsed.operationContent().isBlank()
                  ? "<p>默认考核内容</p>" : parsed.operationContent(), parsed.scoreItems()));
        }
        int globalItemNo = 1;
        for (CompetitionQuestionWorkbookParser.ParsedAssessmentContent group : groups) {
          CompetitionPracticalQuestionAssessmentContentEntity content = new CompetitionPracticalQuestionAssessmentContentEntity();
          content.setId(IdWorker.getId());
          content.setTenantId(tenantId);
          content.setQuestionId(question.getId());
          content.setSortOrder(group.sortOrder());
          content.setContentHtml(sanitize(group.contentHtml()));
          content.setCreatedBy(userId);
          content.setUpdatedBy(userId);
          content.setDeleted(0);
          if (contentMapper != null && contentMapper.insert(content) != 1) {
            throw new com.guoyu.platform.common.exception.BusinessException(
                com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "实操考核内容保存失败");
          }
          for (CompetitionQuestionWorkbookParser.ParsedScoreItem item : group.elements()) {
            CompetitionPracticalQuestionScoreItemEntity scoreItem = new CompetitionPracticalQuestionScoreItemEntity();
            scoreItem.setId(IdWorker.getId());
            scoreItem.setTenantId(tenantId);
            scoreItem.setQuestionId(question.getId());
            scoreItem.setContentId(contentMapper == null ? null : content.getId());
            scoreItem.setElementOrder(item.itemNo());
            scoreItem.setItemNo(globalItemNo++);
            scoreItem.setItemName(legacyItemName(item.elementHtml() == null ? item.itemName() : item.elementHtml()));
            scoreItem.setElementHtml(sanitize(item.elementHtml() == null ? item.itemName() : item.elementHtml()));
            scoreItem.setAllocationEnabled(item.allocationEnabled() ? 1 : 0);
            scoreItem.setCriterion(Jsoup.parse(item.criterion() == null ? item.itemName() : item.criterion()).text());
            scoreItem.setStandardHtml(sanitize(item.criterion() == null || item.criterion().isBlank()
                ? item.itemName() : item.criterion()));
            scoreItem.setMaxScore(item.allocationEnabled() ? item.maxScore() : null);
            scoreItem.setMaxDeduction(item.allocationEnabled() ? item.maxDeduction() : null);
            scoreItem.setDeductionStep(item.allocationEnabled() ? item.deductionStep() : null);
            scoreItem.setStatus("enabled");
            scoreItem.setCreatedBy(userId);
            scoreItem.setUpdatedBy(userId);
            scoreItem.setDeleted(0);
            if (scoreItemMapper.insert(scoreItem) != 1) {
              throw new com.guoyu.platform.common.exception.BusinessException(
                  com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "实操评分项保存失败");
            }
          }
        }
      }
    }
  }

  /** Position-based binding plan; the question entity id does not exist until this transaction. */
  public record Binding(int questionIndex, Long blueprintId, Long blueprintNodeId, String matchMode) {}

  public static BigDecimal sumScoreItems(List<CompetitionQuestionWorkbookParser.ParsedScoreItem> items) {
    return (items == null ? List.<CompetitionQuestionWorkbookParser.ParsedScoreItem>of() : items).stream()
        .map(CompetitionQuestionWorkbookParser.ParsedScoreItem::maxScore)
        .filter(value -> value != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String sanitize(String html) {
    if (html == null || html.isBlank()) return null;
    return Jsoup.clean(html, PRACTICAL_HTML);
  }

  private static String legacyItemName(String html) {
    String text = Jsoup.parse(html == null ? "" : html).text();
    return text.length() <= 255 ? text : text.substring(0, 255);
  }
}
