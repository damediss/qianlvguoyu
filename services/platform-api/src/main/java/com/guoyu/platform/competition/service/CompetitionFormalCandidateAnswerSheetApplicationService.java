package com.guoyu.platform.competition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateResultStatus;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.NormalizedAnswer;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.ProjectedOption;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetDocxWriter.AnswerSheetDocument;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetFile;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateQuestionScoreVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateResultVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerOptionVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionFormalCandidateAnswerSheetApplicationService {

  private final CompetitionFormalExamSessionApplicationService sessionService;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalCandidateAnswerMapper answerMapper;
  private final FormalExamCandidateQuestionProjection questionProjection;
  private final CompetitionFormalCandidateAnswerSheetDocxWriter docxWriter;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final ObjectMapper objectMapper;

  public CompetitionFormalCandidateAnswerSheetApplicationService(
      CompetitionFormalExamSessionApplicationService sessionService,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper,
      FormalExamCandidateQuestionProjection questionProjection,
      CompetitionFormalCandidateAnswerSheetDocxWriter docxWriter,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionRegistrationMapper registrationMapper) {
    this(
        sessionService,
        questionSnapshotMapper,
        answerMapper,
        questionProjection,
        docxWriter,
        candidateExamMapper,
        registrationMapper,
        new ObjectMapper());
  }

  @Autowired
  public CompetitionFormalCandidateAnswerSheetApplicationService(
      CompetitionFormalExamSessionApplicationService sessionService,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper,
      FormalExamCandidateQuestionProjection questionProjection,
      CompetitionFormalCandidateAnswerSheetDocxWriter docxWriter,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionRegistrationMapper registrationMapper,
      ObjectMapper objectMapper) {
    this.sessionService = sessionService;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.answerMapper = answerMapper;
    this.questionProjection = questionProjection;
    this.docxWriter = docxWriter;
    this.candidateExamMapper = candidateExamMapper;
    this.registrationMapper = registrationMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateAnswerSheetVO getAnswerSheet(Long candidateExamId) {
    LoadedAnswerSheet loaded = load(candidateExamId);
    return new CompetitionFormalCandidateAnswerSheetVO(
        loaded.candidate().id(),
        loaded.candidate().candidateName(),
        loaded.candidate().candidateNo(),
        loaded.questions());
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateAnswerSheetFile exportAnswerSheet(Long candidateExamId) {
    return writeOne(load(candidateExamId));
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateAnswerSheetFile exportAnswerSheets(List<Long> candidateExamIds) {
    List<Long> ids = distinctIds(candidateExamIds);
    if (ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导出的选手");
    }
    if (ids.size() > CompetitionFormalCandidateAnswerSheetDocxWriter.MAX_EXPORT_COUNT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "一次最多导出 200 份答题卡");
    }
    if (ids.size() == 1) {
      return writeOne(load(ids.getFirst()));
    }
    Set<String> usedNames = new LinkedHashSet<>();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
      for (Long candidateExamId : ids) {
        LoadedAnswerSheet loaded = load(candidateExamId);
        byte[] document = docxWriter.write(toDocument(loaded));
        String entryName = CompetitionFormalCandidateAnswerSheetDocxWriter.uniqueDocxFileName(
            loaded.candidate().candidateName(), loaded.candidate().candidateNo(), usedNames);
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(document);
        zip.closeEntry();
      }
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成答题卡压缩包失败");
    }
    return new CompetitionFormalCandidateAnswerSheetFile(
        output.toByteArray(),
        CompetitionFormalCandidateAnswerSheetDocxWriter.ZIP_FILE_NAME,
        CompetitionFormalCandidateAnswerSheetDocxWriter.ZIP_MEDIA_TYPE);
  }

  private CompetitionFormalCandidateAnswerSheetFile writeOne(LoadedAnswerSheet loaded) {
    return new CompetitionFormalCandidateAnswerSheetFile(
        docxWriter.write(toDocument(loaded)),
        CompetitionFormalCandidateAnswerSheetDocxWriter.docxFileName(
            loaded.candidate().candidateName(), loaded.candidate().candidateNo()),
        CompetitionFormalCandidateAnswerSheetDocxWriter.DOCX_MEDIA_TYPE);
  }

  private LoadedAnswerSheet load(Long candidateExamId) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }

    // Reuse the session service's tenant, event, stage, and candidate scope checks.
    CompetitionFormalCandidateExamVO candidate = sessionService.getCandidate(candidateExamId);
    CompetitionFormalCandidateResultVO result = sessionService.getCandidateResult(candidateExamId);
    if (!CompetitionFormalCandidateResultStatus.SCORED.getCode().equals(result.resultStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手成绩尚未完成，不能读取答题卡");
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

    List<CompetitionFormalCandidateAnswerSheetQuestionVO> questionVos = new ArrayList<>();
    Map<String, JsonNode> correctAnswersByQuestion = new LinkedHashMap<>();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      List<ProjectedOption> projectedOptions = questionProjection.projectOptions(
          question.getId(), question.getOptionsJson());
      questionVos.add(toQuestionVO(question, answersByQuestion, scoresByQuestion, projectedOptions));
      correctAnswersByQuestion.put(
          String.valueOf(question.getId()), correctAnswer(question, projectedOptions));
    }
    return new LoadedAnswerSheet(candidate, result, questionVos, scoresByQuestion, correctAnswersByQuestion);
  }

  private AnswerSheetDocument toDocument(LoadedAnswerSheet loaded) {
    List<AnswerSheetDocument.Question> questions = loaded.questions().stream()
        .map(question -> {
          CompetitionFormalCandidateQuestionScoreVO score =
              loaded.scoresByQuestion().get(question.questionSnapshotId());
          BigDecimal awarded = score == null ? null : score.awardedScore();
          BigDecimal max = score == null ? null : score.maxScore();
          List<AnswerSheetDocument.Option> options = safe(question.options()).stream()
              .filter(Objects::nonNull)
              .map(option -> new AnswerSheetDocument.Option(
                  option.optionId(), option.label(), option.content()))
              .toList();
          return new AnswerSheetDocument.Question(
              question.questionOrder(),
              question.questionType(),
              question.stem(),
              question.answered(),
              question.correct(),
              awarded,
              max,
              question.candidateAnswer(),
              loaded.correctAnswersByQuestion().get(question.questionSnapshotId()),
              options);
        })
        .toList();
    SheetIdentity identity = identityOf(loaded.candidate().id());
    return new AnswerSheetDocument(
        loaded.candidate().candidateName(),
        loaded.candidate().candidateNo(),
        loaded.result().totalScore(),
        loaded.result().fullScore(),
        questions,
        identity.eventName(),
        identity.workTypeName(),
        identity.orgName(),
        identity.idCardNo());
  }

  private SheetIdentity identityOf(String candidateExamId) {
    Long id = parseId(candidateExamId);
    if (id == null) {
      return SheetIdentity.empty();
    }
    CompetitionFormalCandidateExamEntity exam = candidateExamMapper.selectById(id);
    if (exam == null || exam.getRegistrationId() == null || exam.getTenantId() == null) {
      return SheetIdentity.empty();
    }
    CompetitionPlayerQueryRow player = registrationMapper.selectPlayerByRegistrationId(
        exam.getTenantId(), exam.getRegistrationId());
    if (player == null) {
      return SheetIdentity.empty();
    }
    return new SheetIdentity(
        player.getEventName(), player.getEventWorkTypeName(), player.getOrgName(), player.getIdCardNo());
  }

  private static Long parseId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private CompetitionFormalCandidateAnswerSheetQuestionVO toQuestionVO(
      CompetitionFormalExamQuestionSnapshotEntity question,
      Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion,
      Map<String, CompetitionFormalCandidateQuestionScoreVO> scoresByQuestion,
      List<ProjectedOption> projectedOptions) {
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

  private JsonNode correctAnswer(
      CompetitionFormalExamQuestionSnapshotEntity question, List<ProjectedOption> options) {
    ObjectNode answer = objectMapper.createObjectNode();
    String type = question.getQuestionType() == null
        ? ""
        : question.getQuestionType().trim().toLowerCase(java.util.Locale.ROOT);
    if ("fill".equals(type)) {
      ArrayNode blanks = answer.putArray("blanks");
      if (question.getFillAnswer() != null) {
        blanks.add(question.getFillAnswer());
      }
      return answer;
    }

    ArrayNode selectedIds = answer.putArray("selectedOptionIds");
    JsonNode correctKeys;
    try {
      correctKeys = objectMapper.readTree(question.getCorrectAnswersJson());
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      return answer;
    }
    if (correctKeys == null || !correctKeys.isArray()) {
      return answer;
    }
    Set<String> keys = new LinkedHashSet<>();
    for (JsonNode correctKey : correctKeys) {
      if (correctKey != null && !correctKey.isNull()) {
        String key = correctKey.asText("").trim();
        if (!key.isEmpty()) {
          keys.add(key);
        }
      }
    }
    for (ProjectedOption option : options) {
      int separator = option.optionId().lastIndexOf(':');
      String key = separator < 0 ? "" : option.optionId().substring(separator + 1);
      if (keys.contains(key)) {
        selectedIds.add(option.optionId());
      }
    }
    return answer;
  }

  private static List<Long> distinctIds(List<Long> candidateExamIds) {
    if (candidateExamIds == null || candidateExamIds.isEmpty()) {
      return List.of();
    }
    List<Long> ids = new ArrayList<>();
    Set<Long> seen = new LinkedHashSet<>();
    for (Long candidateExamId : candidateExamIds) {
      if (candidateExamId != null && seen.add(candidateExamId)) {
        ids.add(candidateExamId);
      }
    }
    return ids;
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private record SheetIdentity(String eventName, String workTypeName, String orgName, String idCardNo) {
    private static SheetIdentity empty() {
      return new SheetIdentity(null, null, null, null);
    }
  }

  private record LoadedAnswerSheet(
      CompetitionFormalCandidateExamVO candidate,
      CompetitionFormalCandidateResultVO result,
      List<CompetitionFormalCandidateAnswerSheetQuestionVO> questions,
      Map<String, CompetitionFormalCandidateQuestionScoreVO> scoresByQuestion,
      Map<String, JsonNode> correctAnswersByQuestion) {
  }
}
