package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 竞赛题目批量落库边界。任何一条失败都会回滚整个批次。 */
@Service
public class CompetitionQuestionImportPersistenceService {

  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper detailMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;

  public CompetitionQuestionImportPersistenceService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper detailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper) {
    this.questionMapper = questionMapper;
    this.detailMapper = detailMapper;
    this.scoreItemMapper = scoreItemMapper;
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
    for (CompetitionQuestionWorkbookParser.ParsedQuestion parsed : parsedQuestions) {
      CompetitionQuestionEntity question = new CompetitionQuestionEntity();
      question.setId(IdWorker.getId());
      question.setTenantId(tenantId);
      question.setEventId(eventId);
      question.setEventWorkTypeId(eventWorkTypeId);
      question.setEventWorkTypeLevelId(eventWorkTypeLevelId);
      question.setContentType(contentType);
      question.setQuestionType(parsed.questionType());
      question.setSortOrder(parsed.sortOrder());
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

      if ("practical".equals(contentType)) {
        CompetitionPracticalQuestionDetailEntity detail = new CompetitionPracticalQuestionDetailEntity();
        detail.setId(IdWorker.getId());
        detail.setTenantId(tenantId);
        detail.setQuestionId(question.getId());
        detail.setOperationContent(parsed.operationContent());
        detail.setStudentPrepare(parsed.studentPrepare());
        detail.setEquipmentPrepare(parsed.equipmentPrepare());
        detail.setCreatedBy(userId);
        detail.setUpdatedBy(userId);
        detail.setDeleted(0);
        if (detailMapper.insert(detail) != 1) {
          throw new com.guoyu.platform.common.exception.BusinessException(
              com.guoyu.platform.common.exception.ErrorCode.CONFLICT, "实操题详情保存失败");
        }

        for (CompetitionQuestionWorkbookParser.ParsedScoreItem item : parsed.scoreItems()) {
          CompetitionPracticalQuestionScoreItemEntity scoreItem = new CompetitionPracticalQuestionScoreItemEntity();
          scoreItem.setId(IdWorker.getId());
          scoreItem.setTenantId(tenantId);
          scoreItem.setQuestionId(question.getId());
          scoreItem.setItemNo(item.itemNo());
          scoreItem.setItemName(item.itemName());
          scoreItem.setCriterion(item.criterion());
          scoreItem.setMaxScore(item.maxScore());
          scoreItem.setMaxDeduction(item.maxDeduction());
          scoreItem.setDeductionStep(item.deductionStep());
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

  public static BigDecimal sumScoreItems(List<CompetitionQuestionWorkbookParser.ParsedScoreItem> items) {
    return items.stream()
        .map(CompetitionQuestionWorkbookParser.ParsedScoreItem::maxScore)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
