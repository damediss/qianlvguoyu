package com.guoyu.platform.competition.prepractice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPlayerPrePracticeAnswerSaveReq;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeAnswerEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeDispatchBatchEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeDispatchRecipientEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeTaskEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeTaskQuestionEntity;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeAnswerMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeDispatchBatchMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeDispatchRecipientMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeTaskMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeTaskQuestionMapper;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeAnswerVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeFeedbackVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeOptionVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeQuestionVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeTaskDetailVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPlayerPrePracticeTaskVO;
import com.guoyu.platform.exam.reviewsync.service.AnswerJudgeDomainService;
import com.guoyu.platform.exam.reviewsync.service.AnswerJudgeDomainService.AnswerOption;
import com.guoyu.platform.exam.reviewsync.support.AnswerOptionParser;
import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.exam.reviewsync.support.SubjectiveAnswerExtractor;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionPlayerPrePracticeTaskApplicationService {

  private static final int MAX_TEXT_ANSWER_LENGTH = 500;
  private static final int MAX_FILL_BLANKS = 20;
  private static final String JUDGMENT_UNANSWERED = "unanswered";
  private static final String JUDGMENT_PENDING = "pending";
  private static final String JUDGMENT_UNAVAILABLE = "unavailable";
  private static final String JUDGMENT_CORRECT = "correct";
  private static final String JUDGMENT_INCORRECT = "incorrect";

  private record Judgment(String status, Boolean correct, String correctAnswer) {
  }

  private final CompetitionPrePracticeDispatchRecipientMapper recipientMapper;
  private final CompetitionPrePracticeDispatchBatchMapper batchMapper;
  private final CompetitionPrePracticeTaskMapper taskMapper;
  private final CompetitionPrePracticeTaskQuestionMapper taskQuestionMapper;
  private final CompetitionPrePracticeAnswerMapper answerMapper;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerPrePracticeTaskApplicationService(
      CompetitionPrePracticeDispatchRecipientMapper recipientMapper,
      CompetitionPrePracticeDispatchBatchMapper batchMapper,
      CompetitionPrePracticeTaskMapper taskMapper,
      CompetitionPrePracticeTaskQuestionMapper taskQuestionMapper,
      CompetitionPrePracticeAnswerMapper answerMapper,
      ObjectMapper objectMapper) {
    this.recipientMapper = recipientMapper;
    this.batchMapper = batchMapper;
    this.taskMapper = taskMapper;
    this.taskQuestionMapper = taskQuestionMapper;
    this.answerMapper = answerMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerPrePracticeTaskDetailVO detail(Long deliveryId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionPrePracticeDispatchRecipientEntity recipient = requireRecipient(deliveryId, player);
    CompetitionPrePracticeDispatchBatchEntity batch = requireBatch(recipient, player);
    CompetitionPrePracticeTaskEntity task = requireTask(recipient, batch, player);
    ensureTaskOpen(task);
    List<CompetitionPrePracticeTaskQuestionEntity> questions = loadQuestions(task.getId(), player);
    List<CompetitionPrePracticeAnswerEntity> answers = loadAnswers(deliveryId, player);
    Map<Long, CompetitionPrePracticeAnswerEntity> answerByQuestionId = new HashMap<>();
    for (CompetitionPrePracticeAnswerEntity answer : answers) {
      if (answer != null && answer.getTaskQuestionId() != null) {
        answerByQuestionId.put(answer.getTaskQuestionId(), answer);
      }
    }

    List<CompetitionPlayerPrePracticeQuestionVO> questionVOs = new ArrayList<>(questions.size());
    int answeredCount = 0;
    for (CompetitionPrePracticeTaskQuestionEntity question : questions) {
      CompetitionPrePracticeAnswerEntity saved = answerByQuestionId.get(question.getId());
      JsonNode userAnswer = saved == null ? null : parseSavedAnswer(saved);
      boolean answered = hasAnswer(userAnswer);
      if (answered) {
        answeredCount++;
      }
      questionVOs.add(toQuestionVO(question, userAnswer, answered));
    }

    CompetitionPlayerPrePracticeTaskVO taskVO = new CompetitionPlayerPrePracticeTaskVO(
        value(recipient.getId()), value(batch.getId()), value(task.getId()),
        batch.getTaskTitleSnapshot(), value(batch.getEventId()), batch.getEventNameSnapshot(),
        value(batch.getEventWorkTypeId()), batch.getEventWorkTypeNameSnapshot(),
        batch.getEventWorkTypeLevelCode(), batch.getEventWorkTypeLevelNameSnapshot(),
        value(batch.getStageId()), batch.getStageNameSnapshot(), task.getQuestionCount(),
        recipient.getStatus(), recipient.getDeliveredAt(), batch.getPushedAt(),
        task.getEffectiveTimeEnabled(), task.getEffectiveStartAt(), task.getEffectiveEndAt());
    return new CompetitionPlayerPrePracticeTaskDetailVO(taskVO, answeredCount, List.copyOf(questionVOs));
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerPrePracticeFeedbackVO feedback(Long deliveryId, Long taskQuestionId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionPrePracticeDispatchRecipientEntity recipient = requireRecipient(deliveryId, player);
    requireOpenTask(recipient, player);
    CompetitionPrePracticeTaskQuestionEntity question = requireQuestion(
        recipient.getTaskId(), taskQuestionId, player.getTenantId());
    return feedbackVO(question);
  }

  @Transactional
  public CompetitionPlayerPrePracticeAnswerVO saveAnswer(
      Long deliveryId,
      Long taskQuestionId,
      CompetitionPlayerPrePracticeAnswerSaveReq req) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionPrePracticeDispatchRecipientEntity recipient = requireRecipient(deliveryId, player);
    requireOpenTask(recipient, player);
    CompetitionPrePracticeTaskQuestionEntity question = requireQuestion(
        recipient.getTaskId(), taskQuestionId, player.getTenantId());
    if (req == null || req.answer() == null || req.answer().isNull()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写作答内容");
    }

    JsonNode answer = validateAnswer(question, req.answer());
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    CompetitionPrePracticeAnswerEntity answerEntity = new CompetitionPrePracticeAnswerEntity();
    answerEntity.setId(IdWorker.getId());
    answerEntity.setTenantId(player.getTenantId());
    answerEntity.setDeliveryId(deliveryId);
    answerEntity.setTaskId(recipient.getTaskId());
    answerEntity.setTaskQuestionId(taskQuestionId);
    answerEntity.setPlayerId(player.getPlayerId());
    answerEntity.setAnswerJson(writeJson(answer));
    answerEntity.setAnsweredAt(now);
    answerEntity.setDeleted(0);
    answerEntity.setCreatedBy(player.getPlayerId());
    answerEntity.setUpdatedBy(player.getPlayerId());
    answerEntity.setCreatedAt(now);
    answerEntity.setUpdatedAt(now);
    int affected = answerMapper.upsert(answerEntity);
    if (affected <= 0) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "作答保存失败，请重试");
    }

    List<CompetitionPrePracticeTaskQuestionEntity> questions = loadQuestions(recipient.getTaskId(), player);
    List<CompetitionPrePracticeAnswerEntity> savedAnswers = loadAnswers(deliveryId, player);
    int answeredCount = countAnswered(questions, savedAnswers, taskQuestionId, answer);
    CompetitionPrePracticeAnswerEntity responseEntity = new CompetitionPrePracticeAnswerEntity();
    responseEntity.setTaskQuestionId(taskQuestionId);
    responseEntity.setAnswerJson(writeJson(answer));
    JsonNode responseAnswer = parseSavedAnswer(responseEntity);
    Judgment judgment = judge(question, responseAnswer);
    return new CompetitionPlayerPrePracticeAnswerVO(
        value(taskQuestionId), responseAnswer, hasAnswer(responseAnswer), judgment.status(),
        judgment.correct(), judgment.correctAnswer(), answeredCount, questions.size(), now);
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Pre-practice task request has no authenticated competition player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }

  private CompetitionPrePracticeDispatchRecipientEntity requireRecipient(
      Long deliveryId, CompetitionPlayerPrincipal player) {
    if (deliveryId == null || deliveryId <= 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛前练习任务不存在");
    }
    CompetitionPrePracticeDispatchRecipientEntity recipient = recipientMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPrePracticeDispatchRecipientEntity>()
            .eq(CompetitionPrePracticeDispatchRecipientEntity::getId, deliveryId)
            .eq(CompetitionPrePracticeDispatchRecipientEntity::getTenantId, player.getTenantId())
            .eq(CompetitionPrePracticeDispatchRecipientEntity::getPlayerId, player.getPlayerId()));
    if (recipient == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛前练习任务不存在或已不属于当前选手");
    }
    return recipient;
  }

  private CompetitionPrePracticeDispatchBatchEntity requireBatch(
      CompetitionPrePracticeDispatchRecipientEntity recipient,
      CompetitionPlayerPrincipal player) {
    CompetitionPrePracticeDispatchBatchEntity batch = batchMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPrePracticeDispatchBatchEntity>()
            .eq(CompetitionPrePracticeDispatchBatchEntity::getId, recipient.getBatchId())
            .eq(CompetitionPrePracticeDispatchBatchEntity::getTenantId, player.getTenantId()));
    if (batch == null || !Objects.equals(batch.getTaskId(), recipient.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛前练习任务已失效");
    }
    return batch;
  }

  private CompetitionPrePracticeTaskEntity requireTask(
      CompetitionPrePracticeDispatchRecipientEntity recipient,
      CompetitionPrePracticeDispatchBatchEntity batch,
      CompetitionPlayerPrincipal player) {
    CompetitionPrePracticeTaskEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPrePracticeTaskEntity>()
            .eq(CompetitionPrePracticeTaskEntity::getId, recipient.getTaskId())
            .eq(CompetitionPrePracticeTaskEntity::getTenantId, player.getTenantId()));
    if (task == null || !Objects.equals(task.getId(), batch.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛前练习题目已失效");
    }
    return task;
  }

  private CompetitionPrePracticeTaskEntity requireOpenTask(
      CompetitionPrePracticeDispatchRecipientEntity recipient,
      CompetitionPlayerPrincipal player) {
    CompetitionPrePracticeDispatchBatchEntity batch = requireBatch(recipient, player);
    CompetitionPrePracticeTaskEntity task = requireTask(recipient, batch, player);
    ensureTaskOpen(task);
    return task;
  }

  private void ensureTaskOpen(CompetitionPrePracticeTaskEntity task) {
    if (!task.getEffectiveTimeEnabled()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    if (task.getEffectiveStartAt() == null || task.getEffectiveEndAt() == null
        || now.isBefore(task.getEffectiveStartAt()) || now.isAfter(task.getEffectiveEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前不在赛前练习任务的开放时间内");
    }
  }

  private List<CompetitionPrePracticeTaskQuestionEntity> loadQuestions(Long taskId, CompetitionPlayerPrincipal player) {
    List<CompetitionPrePracticeTaskQuestionEntity> questions = taskQuestionMapper.selectList(
        new LambdaQueryWrapper<CompetitionPrePracticeTaskQuestionEntity>()
            .eq(CompetitionPrePracticeTaskQuestionEntity::getTenantId, player.getTenantId())
            .eq(CompetitionPrePracticeTaskQuestionEntity::getTaskId, taskId)
            .orderByAsc(CompetitionPrePracticeTaskQuestionEntity::getSortOrder)
            .orderByAsc(CompetitionPrePracticeTaskQuestionEntity::getId));
    return questions == null ? List.of() : questions.stream().filter(Objects::nonNull).toList();
  }

  private List<CompetitionPrePracticeAnswerEntity> loadAnswers(
      Long deliveryId, CompetitionPlayerPrincipal player) {
    List<CompetitionPrePracticeAnswerEntity> answers = answerMapper.selectList(
        new LambdaQueryWrapper<CompetitionPrePracticeAnswerEntity>()
            .eq(CompetitionPrePracticeAnswerEntity::getTenantId, player.getTenantId())
            .eq(CompetitionPrePracticeAnswerEntity::getDeliveryId, deliveryId)
            .eq(CompetitionPrePracticeAnswerEntity::getPlayerId, player.getPlayerId()));
    return answers == null ? List.of() : answers.stream().filter(Objects::nonNull).toList();
  }

  private CompetitionPrePracticeTaskQuestionEntity requireQuestion(
      Long taskId, Long taskQuestionId, Long tenantId) {
    if (taskQuestionId == null || taskQuestionId <= 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
    }
    CompetitionPrePracticeTaskQuestionEntity question = taskQuestionMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPrePracticeTaskQuestionEntity>()
            .eq(CompetitionPrePracticeTaskQuestionEntity::getId, taskQuestionId)
            .eq(CompetitionPrePracticeTaskQuestionEntity::getTenantId, tenantId)
            .eq(CompetitionPrePracticeTaskQuestionEntity::getTaskId, taskId));
    if (question == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在或不属于本次练习任务");
    }
    return question;
  }

  private CompetitionPlayerPrePracticeQuestionVO toQuestionVO(
      CompetitionPrePracticeTaskQuestionEntity question, JsonNode userAnswer, boolean answered) {
    JsonNode snapshot = parseSnapshot(question);
    String type = QuestionTypes.normalize(question.getQuestionType());
    String rawAnswer = text(snapshot, "answer");
    List<AnswerOption> parsedOptions = QuestionTypes.OBJECTIVE_TYPES.contains(type)
        ? AnswerOptionParser.parse(rawAnswer, objectMapper) : List.of();
    List<CompetitionPlayerPrePracticeOptionVO> options = new ArrayList<>(parsedOptions.size());
    for (int i = 0; i < parsedOptions.size(); i++) {
      options.add(new CompetitionPlayerPrePracticeOptionVO(optionLabel(i), parsedOptions.get(i).content()));
    }
    Judgment judgment = judge(question, userAnswer);
    return new CompetitionPlayerPrePracticeQuestionVO(
        value(question.getId()), question.getSortOrder(), type, QuestionTypes.toLabel(type),
        QuestionTypes.OBJECTIVE_TYPES.contains(type), text(snapshot, "content"),
        text(snapshot, "audio"), text(snapshot, "video"), List.copyOf(options), userAnswer, answered,
        judgment.status(), judgment.correct());
  }

  private Judgment judge(CompetitionPrePracticeTaskQuestionEntity question, JsonNode userAnswer) {
    if (!hasAnswer(userAnswer)) {
      return new Judgment(JUDGMENT_UNANSWERED, null, null);
    }
    JsonNode snapshot = parseSnapshot(question);
    String type = QuestionTypes.normalize(question.getQuestionType());
    String answerJson = text(snapshot, "answer");
    if (QuestionTypes.OBJECTIVE_TYPES.contains(type)) {
      List<AnswerOption> options = AnswerOptionParser.parse(answerJson, objectMapper);
      long correctCount = options.stream().filter(AnswerOption::correct).count();
      if (correctCount == 0 || (!"multi".equals(type) && correctCount != 1)) {
        return new Judgment(JUDGMENT_UNAVAILABLE, null, null);
      }
      AnswerJudgeDomainService.JudgeResult result = "multi".equals(type)
          ? AnswerJudgeDomainService.judgeStrictMulti(options, textValues(userAnswer), null)
          : AnswerJudgeDomainService.judge(type, options, userAnswer.asText(), List.of());
      return new Judgment(
          result.correct() ? JUDGMENT_CORRECT : JUDGMENT_INCORRECT,
          result.correct(), correctOptions(answerJson));
    }
    if ("fill".equals(type)) {
      List<String> expected = fillReferenceAnswers(answerJson);
      List<String> actual = textValues(userAnswer);
      if (expected.isEmpty()) {
        return new Judgment(JUDGMENT_UNAVAILABLE, null, null);
      }
      boolean complete = actual.size() == expected.size()
          && actual.stream().allMatch(StringUtils::hasText);
      if (!complete) {
        return new Judgment(JUDGMENT_PENDING, null, null);
      }
      boolean correct = true;
      for (int i = 0; i < expected.size(); i++) {
        if (!expected.get(i).trim().equalsIgnoreCase(actual.get(i).trim())) {
          correct = false;
          break;
        }
      }
      return new Judgment(
          correct ? JUDGMENT_CORRECT : JUDGMENT_INCORRECT,
          correct, String.join("；", expected));
    }
    String reference = SubjectiveAnswerExtractor.extract(answerJson);
    if (!StringUtils.hasText(reference) || !userAnswer.isTextual()) {
      return new Judgment(JUDGMENT_UNAVAILABLE, null, null);
    }
    AnswerJudgeDomainService.JudgeResult result =
        AnswerJudgeDomainService.judgeText(reference, userAnswer.asText());
    return new Judgment(
        result.correct() ? JUDGMENT_CORRECT : JUDGMENT_INCORRECT,
        result.correct(), result.correctAnswerText());
  }

  private List<String> fillReferenceAnswers(String answerJson) {
    String reference = SubjectiveAnswerExtractor.extract(answerJson);
    if (!StringUtils.hasText(reference)) {
      return List.of();
    }
    return java.util.Arrays.stream(reference.split("\\R", -1))
        .map(String::trim)
        .toList();
  }

  private List<String> textValues(JsonNode values) {
    if (values == null || values.isNull()) {
      return List.of();
    }
    if (values.isTextual()) {
      return List.of(values.asText());
    }
    if (!values.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>(values.size());
    for (JsonNode value : values) {
      result.add(value != null && value.isTextual() ? value.asText() : "");
    }
    return result;
  }

  private CompetitionPlayerPrePracticeFeedbackVO feedbackVO(
      CompetitionPrePracticeTaskQuestionEntity question) {
    JsonNode snapshot = parseSnapshot(question);
    String answer = text(snapshot, "answer");
    String type = QuestionTypes.normalize(question.getQuestionType());
    String reference = QuestionTypes.OBJECTIVE_TYPES.contains(type)
        ? correctOptions(answer) : SubjectiveAnswerExtractor.extract(answer);
    return new CompetitionPlayerPrePracticeFeedbackVO(value(question.getId()), reference, text(snapshot, "resolve"));
  }

  private String correctOptions(String answerJson) {
    List<String> correct = new ArrayList<>();
    List<AnswerOption> options = AnswerOptionParser.parse(answerJson, objectMapper);
    for (int i = 0; i < options.size(); i++) {
      AnswerOption option = options.get(i);
      if (option.correct()) {
        correct.add(optionLabel(i) + " · " + option.content());
      }
    }
    return correct.isEmpty() ? null : String.join("；", correct);
  }

  private JsonNode validateAnswer(CompetitionPrePracticeTaskQuestionEntity question, JsonNode answer) {
    String type = QuestionTypes.normalize(question.getQuestionType());
    JsonNode snapshot = parseSnapshot(question);
    List<AnswerOption> options = AnswerOptionParser.parse(text(snapshot, "answer"), objectMapper);
    List<String> optionContents = options.stream().map(AnswerOption::content).toList();

    if ("single".equals(type) || "judge".equals(type)) {
      if (!answer.isTextual()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择一个选项");
      }
      String selected = answer.asText();
      if (StringUtils.hasText(selected) && !optionContents.contains(selected)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "所选答案不属于当前题目");
      }
      return answer.deepCopy();
    }
    if ("multi".equals(type)) {
      if (!answer.isArray() || answer.size() > options.size()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "多选题答案格式无效");
      }
      List<String> selected = new ArrayList<>(answer.size());
      for (JsonNode value : answer) {
        if (!value.isTextual() || !optionContents.contains(value.asText()) || selected.contains(value.asText())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "多选题包含无效或重复选项");
        }
        selected.add(value.asText());
      }
      return answer.deepCopy();
    }
    if ("fill".equals(type) && answer.isArray()) {
      if (answer.size() > MAX_FILL_BLANKS) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "填空答案数量超出限制");
      }
      for (JsonNode value : answer) {
        if (!value.isTextual() || value.asText().length() > MAX_TEXT_ANSWER_LENGTH) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "填空答案格式无效或过长");
        }
      }
      return answer.deepCopy();
    }
    if (!answer.isTextual() || answer.asText().length() > MAX_TEXT_ANSWER_LENGTH) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文字作答格式无效或超过500字");
    }
    return answer.deepCopy();
  }

  private int countAnswered(
      List<CompetitionPrePracticeTaskQuestionEntity> questions,
      List<CompetitionPrePracticeAnswerEntity> savedAnswers,
      Long changedQuestionId,
      JsonNode changedAnswer) {
    Map<Long, JsonNode> answerByQuestionId = new HashMap<>();
    for (CompetitionPrePracticeAnswerEntity saved : savedAnswers) {
      if (saved != null && saved.getTaskQuestionId() != null) {
        answerByQuestionId.put(saved.getTaskQuestionId(), parseSavedAnswer(saved));
      }
    }
    answerByQuestionId.put(changedQuestionId, changedAnswer);
    int count = 0;
    for (CompetitionPrePracticeTaskQuestionEntity question : questions) {
      if (question != null && hasAnswer(answerByQuestionId.get(question.getId()))) {
        count++;
      }
    }
    return count;
  }

  private JsonNode parseSnapshot(CompetitionPrePracticeTaskQuestionEntity question) {
    try {
      JsonNode snapshot = objectMapper.readTree(question.getQuestionSnapshotJson());
      if (snapshot == null || !snapshot.isObject()) {
        throw new IllegalArgumentException("snapshot root is not an object");
      }
      return snapshot;
    } catch (Exception ex) {
      log.error("Pre-practice question snapshot is invalid, taskQuestionId={}", question.getId(), ex);
      throw new BusinessException(ErrorCode.CONFLICT, "题目数据暂不可用，请联系赛事方");
    }
  }

  private JsonNode parseSavedAnswer(CompetitionPrePracticeAnswerEntity answer) {
    if (answer == null || !StringUtils.hasText(answer.getAnswerJson())) {
      return null;
    }
    try {
      return objectMapper.readTree(answer.getAnswerJson());
    } catch (Exception ex) {
      log.warn("Saved pre-practice answer JSON is invalid, answerId={}", answer.getId(), ex);
      return null;
    }
  }

  private boolean hasAnswer(JsonNode answer) {
    if (answer == null || answer.isNull()) {
      return false;
    }
    if (answer.isTextual()) {
      return StringUtils.hasText(answer.asText());
    }
    if (answer.isArray()) {
      for (JsonNode item : answer) {
        if (item != null && item.isTextual() && StringUtils.hasText(item.asText())) {
          return true;
        }
      }
    }
    return false;
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private String writeJson(JsonNode answer) {
    try {
      return objectMapper.writeValueAsString(answer);
    } catch (Exception ex) {
      log.error("Pre-practice answer JSON serialization failed", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "作答保存失败，请重试");
    }
  }

  private String optionLabel(int index) {
    return index >= 0 && index < 26 ? String.valueOf((char) ('A' + index)) : String.valueOf(index + 1);
  }

  private String value(Long value) {
    return value == null ? null : value.toString();
  }
}
