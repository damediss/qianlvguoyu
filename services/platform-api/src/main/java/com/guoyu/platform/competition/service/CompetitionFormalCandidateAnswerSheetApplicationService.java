package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateResultStatus;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.NormalizedAnswer;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.ProjectedOption;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateQuestionScoreVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateResultVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerOptionVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionFormalCandidateAnswerSheetApplicationService {

  private final CompetitionFormalExamSessionApplicationService sessionService;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalCandidateAnswerMapper answerMapper;
  private final FormalExamCandidateQuestionProjection questionProjection;

  public CompetitionFormalCandidateAnswerSheetApplicationService(
      CompetitionFormalExamSessionApplicationService sessionService,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper,
      FormalExamCandidateQuestionProjection questionProjection) {
    this.sessionService = sessionService;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.answerMapper = answerMapper;
    this.questionProjection = questionProjection;
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateAnswerSheetVO getAnswerSheet(Long candidateExamId) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }

    // Reuse the session service's tenant, event, stage, and candidate scope checks.
    CompetitionFormalCandidateExamVO candidate = sessionService.getCandidate(candidateExamId);
    CompetitionFormalCandidateResultVO result = sessionService.getCandidateResult(candidateExamId);
    if (!CompetitionFormalCandidateResultStatus.SCORED.getCode().equals(result.resultStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手成绩尚未完成，不能查看答题卡");
    }
    if (candidate.paperSnapshotId() == null || candidate.examSessionId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "选手考试实例关联信息不完整，无法读取答题卡");
    }

    Long paperSnapshotId = Long.valueOf(candidate.paperSnapshotId());
    Long examSessionId = Long.valueOf(candidate.examSessionId());
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = safe(
        questionSnapshotMapper.selectByPaperSnapshotId(paperSnapshotId)).stream()
        .filter(Objects::nonNull)
        .filter(question -> Objects.equals(question.getExamSessionId(), examSessionId))
        .filter(question -> Objects.equals(question.getPaperSnapshotId(), paperSnapshotId))
        .toList();
    Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion = safe(
        answerMapper.selectByCandidateExamId(candidateExamId)).stream()
        .filter(Objects::nonNull)
        .filter(answer -> answer.getQuestionSnapshotId() != null)
        .collect(Collectors.toMap(
            CompetitionFormalCandidateAnswerEntity::getQuestionSnapshotId,
            answer -> answer,
            (first, ignored) -> first,
            LinkedHashMap::new));
    Map<String, CompetitionFormalCandidateQuestionScoreVO> scoresByQuestion = safe(
        result.questionScores()).stream()
        .filter(Objects::nonNull)
        .filter(score -> score.questionSnapshotId() != null)
        .collect(Collectors.toMap(
            CompetitionFormalCandidateQuestionScoreVO::questionSnapshotId,
            score -> score,
            (first, ignored) -> first,
            LinkedHashMap::new));

    List<CompetitionFormalCandidateAnswerSheetQuestionVO> questionVos = questions.stream()
        .map(question -> toQuestionVO(question, answersByQuestion, scoresByQuestion))
        .toList();
    return new CompetitionFormalCandidateAnswerSheetVO(
        candidate.id(), candidate.candidateName(), candidate.candidateNo(), questionVos);
  }

  private CompetitionFormalCandidateAnswerSheetQuestionVO toQuestionVO(
      CompetitionFormalExamQuestionSnapshotEntity question,
      Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion,
      Map<String, CompetitionFormalCandidateQuestionScoreVO> scoresByQuestion) {
    List<ProjectedOption> projectedOptions = questionProjection.projectOptions(
        question.getId(), question.getOptionsJson());
    List<CompetitionFormalExamPlayerOptionVO> options = projectedOptions.stream()
        .map(option -> new CompetitionFormalExamPlayerOptionVO(
            option.optionId(), option.label(), option.content()))
        .toList();
    CompetitionFormalCandidateAnswerEntity saved = answersByQuestion.get(question.getId());
    NormalizedAnswer answer = questionProjection.parseStored(
        question.getQuestionType(), saved == null ? null : saved.getAnswerJson(), projectedOptions);
    CompetitionFormalCandidateQuestionScoreVO score =
        scoresByQuestion.get(String.valueOf(question.getId()));
    Boolean answered = score == null ? answer.answered() : score.answered();
    Boolean correct = score == null ? Boolean.FALSE : score.correct();
    return new CompetitionFormalCandidateAnswerSheetQuestionVO(
        String.valueOf(question.getId()),
        question.getQuestionOrder(),
        question.getQuestionType(),
        question.getStem(),
        answered,
        correct,
        answer.node(),
        options);
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }
}
