package com.guoyu.platform.certification.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskHistoryEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskScoreEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCandidateResultEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalQuestionResultEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalScoringConfigEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskHistoryMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskScoreMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCandidateResultMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalQuestionResultMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalScoringConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamOperatePaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamOperatePaperEntity;
import com.guoyu.platform.exam.mapper.ExamOperatePaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamOperatePaperMapper;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class PracticalScoringApplicationService {

  private static final Set<String> EDITABLE_STATUSES = Set.of("assigned", "draft", "returned");
  private static final int MIN_FULL_RECORD_QUESTIONS = 4;
  private static final int MAX_FULL_RECORD_QUESTIONS = 8;
  private static final BigDecimal FULL_RECORD_TOTAL_SCORE = new BigDecimal("100");
  private static final BigDecimal MAX_ASSESSOR_SCORE_DEVIATION = new BigDecimal("10");
  private static final Pattern QUESTION_SCORE_PATTERN =
      Pattern.compile("[（(]\\s*(\\d+(?:\\.\\d+)?)\\s*分\\s*[）)]");
  private static final String SUMMARY_TEMPLATE_RESOURCE = "templates/评分记录汇总表模板.xlsx";
  private static final String SCORE_RECORD_TEMPLATE_RESOURCE = "templates/实操评分记录表模板.xlsx";
  private static final int SUMMARY_DATA_ROW_INDEX = 6;
  private static final int SUMMARY_HEADER_ROW_INDEX = 5;
  private static final int SUMMARY_QUESTION_COUNT = 8;
  private static final int SUMMARY_SCORE_COLUMNS_PER_QUESTION = 6;
  private static final int SUMMARY_FIRST_QUESTION_COLUMN = 3;
  private static final int SUMMARY_TOTAL_SCORE_COLUMN = 51;
  private static final int EXCEL_COLUMN_WIDTH_UNIT = 256;
  private static final int SUMMARY_ASSESSOR_COLUMN_WIDTH = excelColumnWidth(3.2D);
  private static final int SUMMARY_AVERAGE_FINAL_COLUMN_WIDTH = excelColumnWidth(4.8D);
  private static final int SUMMARY_WEIGHT_COLUMN_WIDTH = excelColumnWidth(4D);
  private static final int SUMMARY_TOTAL_DISPLAY_COLUMN_WIDTH = excelColumnWidth(6D);
  private static final int SUMMARY_WEIGHT_VALUE = 100;
  private static final int SCORE_RECORD_SCORE_START_COLUMN = 4;
  private static final int SCORE_RECORD_CANDIDATE_START_ROW = 11;
  private static final int SCORE_RECORD_TEMPLATE_CANDIDATE_ROW = 18;
  private static final int SCORE_RECORD_RECOMMENDED_MAX_STANDARD_COUNT = 9;
  private static final String DECIMAL_SCORE_FORMAT = "0.0";
  private static final String INTEGER_SCORE_FORMAT = "0";
  private static final BigDecimal ZERO_SCORE = new BigDecimal("0.0");

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final CertificationAssessorTaskScoreMapper scoreMapper;
  private final CertificationPracticalScoringConfigMapper configMapper;
  private final CertificationPracticalQuestionResultMapper questionResultMapper;
  private final CertificationPracticalCandidateResultMapper candidateResultMapper;
  private final CertificationAssessorTaskHistoryMapper historyMapper;
  private final ExamOperatePaperMapper paperMapper;
  private final ExamOperatePaperDetailMapper paperDetailMapper;
  private final ObjectMapper objectMapper;
  private final PracticalStartApplicationService startService;
  private final AssessorWorkbenchContextService workbenchContextService;

  private static int excelColumnWidth(double characters) {
    return (int) Math.round(characters * EXCEL_COLUMN_WIDTH_UNIT);
  }

  public PracticalScoringApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      CertificationAssessorTaskScoreMapper scoreMapper,
      CertificationPracticalScoringConfigMapper configMapper,
      CertificationPracticalQuestionResultMapper questionResultMapper,
      CertificationPracticalCandidateResultMapper candidateResultMapper,
      CertificationAssessorTaskHistoryMapper historyMapper,
      ExamOperatePaperMapper paperMapper,
      ExamOperatePaperDetailMapper paperDetailMapper,
      ObjectMapper objectMapper,
      PracticalStartApplicationService startService,
      AssessorWorkbenchContextService workbenchContextService
  ) {
    this.taskMapper = taskMapper;
    this.scopeMapper = scopeMapper;
    this.candidateMapper = candidateMapper;
    this.groupMapper = groupMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.assessorMapper = assessorMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.scoreMapper = scoreMapper;
    this.configMapper = configMapper;
    this.questionResultMapper = questionResultMapper;
    this.candidateResultMapper = candidateResultMapper;
    this.historyMapper = historyMapper;
    this.paperMapper = paperMapper;
    this.paperDetailMapper = paperDetailMapper;
    this.objectMapper = objectMapper;
    this.startService = startService;
    this.workbenchContextService = workbenchContextService;
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.SetupView setup(Long taskId) {
    CertificationTaskEntity task = requireTask(taskId);
    List<CertificationTaskScopeEntity> scopes = scopeMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder));
    List<CertificationTaskAssessorGroupEntity> groups = groupMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
            .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskAssessorGroupEntity::getCreatedAt));
    Map<Long, Integer> memberCounts = loadMemberCountsByTaskId(taskId, groups);
    List<ExamOperatePaperEntity> papers = paperMapper.selectList(
        new LambdaQueryWrapper<ExamOperatePaperEntity>()
            .eq(ExamOperatePaperEntity::getStatus, 1)
            .orderByDesc(ExamOperatePaperEntity::getCreateTime));
    List<PracticalScoringModels.PaperOption> paperOptions = papers.stream()
        .map(this::paperOption)
        .toList();
    List<CertificationPracticalScoringConfigEntity> configs = configMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId));
    return new PracticalScoringModels.SetupView(
        taskId,
        task.getExamDate(),
        scopes.stream().map(s -> new PracticalScoringModels.ScopeOption(
            s.getId(), s.getWorkTypeId(), s.getWorkTypeName(), s.getLevelCode(), s.getLevelName())).toList(),
        groups.stream().map(g -> new PracticalScoringModels.GroupOption(
            g.getId(), g.getGroupName(), g.getLeaderAssessorId(), memberCounts.getOrDefault(g.getId(), 0))).toList(),
        paperOptions,
        configs.stream().map(this::configView).toList()
    );
  }

  @Transactional
  public PracticalScoringModels.ConfigView saveConfig(Long taskId, PracticalScoringModels.SaveConfigReq req) {
    CertificationTaskEntity task = requireTask(taskId);
    if (task.getExamDate() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先设置认定任务考试日期");
    }
    CertificationTaskScopeEntity scope = scopeMapper.selectById(req.scopeId());
    if (scope == null || !taskId.equals(scope.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工种等级范围不存在");
    }
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, req.groupId());
    validateThreeAssessors(taskId, group.getId());
    ExamOperatePaperEntity paper = requirePaper(req.paperId());
    if (paper.getWorkTypeId() != null && !req.workTypeId().equals(paper.getWorkTypeId().longValue())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操试卷工种与任务范围不一致");
    }
    List<ExamOperatePaperDetailEntity> questions = paperQuestions(req.paperId());
    validateQuestionTemplates(questions);

    CertificationPracticalScoringConfigEntity entity = configMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
            .eq(CertificationPracticalScoringConfigEntity::getScopeId, req.scopeId())
            .isNull(CertificationPracticalScoringConfigEntity::getCandidateId)
            .last("LIMIT 1"));
    if (entity == null) {
      entity = new CertificationPracticalScoringConfigEntity();
      entity.setTaskId(taskId);
      entity.setScopeId(req.scopeId());
      entity.setCandidateId(null);
      entity.setDeleted(0);
    }
    entity.setWorkTypeId(req.workTypeId());
    entity.setWorkTypeName(scope.getWorkTypeName());
    entity.setLevelCode(scope.getLevelCode());
    entity.setLevelName(scope.getLevelName());
    entity.setExamDate(task.getExamDate());
    entity.setPaperId(req.paperId());
    entity.setGroupId(req.groupId());
    entity.setStatus("configured");
    if (entity.getId() == null) {
      configMapper.insert(entity);
    } else {
      configMapper.updateById(entity);
    }

    scope.setWorkTypeId(req.workTypeId());
    scopeMapper.updateById(scope);
    CertificationCandidateEntity candidatePatch = new CertificationCandidateEntity();
    candidatePatch.setWorkTypeId(req.workTypeId());
    candidateMapper.update(candidatePatch, new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getLevelCode, scope.getLevelCode())
        .eq(CertificationCandidateEntity::getWorkTypeName, scope.getWorkTypeName()));
    return configView(entity);
  }

  @Transactional
  public List<PracticalScoringModels.TemplateItem> saveQuestionTemplate(
      Long paperId,
      Long questionId,
      List<PracticalScoringModels.TemplateItem> items
  ) {
    requirePaper(paperId);
    ExamOperatePaperDetailEntity question = paperDetailMapper.selectById(questionId.intValue());
    if (question == null || !paperId.equals(question.getPaperId().longValue())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "实操试题不存在");
    }
    List<PracticalScoringModels.TemplateItem> normalized = normalizeTemplate(items, question.getScore());
    question.setScoringTemplateJson(writeJson(normalized));
    question.setWeight(100);
    paperDetailMapper.updateById(question);
    return normalized;
  }

  @Transactional
  public PracticalScoringModels.GenerateView generate(Long taskId) {
    CertificationTaskEntity task = requireTask(taskId);
    List<CertificationPracticalScoringConfigEntity> configs = configMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
            .eq(CertificationPracticalScoringConfigEntity::getStatus, "configured"));
    if (CollectionUtils.isEmpty(configs)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成实操试卷与考评组配置");
    }
    int created = 0;
    int existing = 0;
    int candidateCount = 0;
    int questionCount = 0;
    for (CertificationPracticalScoringConfigEntity config : configs) {
      List<CertificationTaskAssessorEntity> members = validateThreeAssessors(taskId, config.getGroupId());
      List<ExamOperatePaperDetailEntity> questions = paperQuestions(config.getPaperId());
      validateQuestionTemplates(questions);
      List<CertificationCandidateEntity> candidates = candidatesForConfig(config);
      if (candidates.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            config.getWorkTypeName() + " " + config.getLevelName() + "没有可生成任务的考生");
      }
      candidateCount += candidates.size();
      questionCount += questions.size();
      for (CertificationCandidateEntity candidate : candidates) {
        GenerateCounter counter = generateForCandidate(task, config, candidate, members, questions);
        created += counter.created();
        existing += counter.existing();
      }
    }
    return new PracticalScoringModels.GenerateView(created, existing, candidateCount, questionCount);
  }

  @Transactional
  public PracticalScoringModels.GenerateView linkCandidate(
      Long taskId,
      Long candidateId,
      PracticalScoringModels.LinkCandidateReq req
  ) {
    CertificationTaskEntity task = requireTask(taskId);
    if (task.getExamDate() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先设置认定任务考试日期");
    }
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    if (!"active".equals(candidate.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅可为有效考生关联考评表模板");
    }
    if (!Boolean.TRUE.equals(candidate.getDocumentComplete())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅可为已完成申报材料的考生关联考评表模板");
    }
    CertificationTaskScopeEntity scope = resolveScopeForCandidate(taskId, candidate, req.scopeId());
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, req.groupId());
    List<CertificationTaskAssessorEntity> members = validateThreeAssessors(taskId, group.getId());
    ExamOperatePaperEntity paper = requirePaper(req.paperId());
    Long effectiveWorkTypeId = effectiveWorkTypeId(candidate, scope, req.workTypeId(), paper);
    if (paper.getWorkTypeId() != null
        && effectiveWorkTypeId != null
        && !Objects.equals(paper.getWorkTypeId().longValue(), effectiveWorkTypeId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操试卷工种与学员工种不一致");
    }
    if (effectiveWorkTypeId != null && candidate.getWorkTypeId() == null) {
      candidate.setWorkTypeId(effectiveWorkTypeId);
      candidateMapper.updateById(candidate);
    }
    if (effectiveWorkTypeId != null && scope.getWorkTypeId() == null) {
      scope.setWorkTypeId(effectiveWorkTypeId);
      scopeMapper.updateById(scope);
    }

    List<ExamOperatePaperDetailEntity> questions = paperQuestions(req.paperId());
    validateQuestionTemplates(questions);
    CertificationPracticalScoringConfigEntity config = upsertCandidateConfig(task, scope, candidate, req, effectiveWorkTypeId);
    GenerateCounter counter = generateForCandidate(task, config, candidate, members, questions);
    return new PracticalScoringModels.GenerateView(counter.created(), counter.existing(), 1, questions.size());
  }

  /**
   * Applies one practical paper and assessor group to several candidates. Candidates that already
   * have a practical configuration or generated scoring tasks are deliberately left unchanged.
   */
  @Transactional
  public PracticalScoringModels.BatchGenerateView linkCandidates(
      Long taskId,
      PracticalScoringModels.BatchLinkCandidatesReq req
  ) {
    requireTask(taskId);
    List<Long> candidateIds = req.candidateIds().stream().distinct().toList();
    List<CertificationAssessorTaskEntity> generatedTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .in(CertificationAssessorTaskEntity::getCandidateId, candidateIds)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    Set<Long> generatedCandidateIds = (generatedTasks == null ? List.<CertificationAssessorTaskEntity>of() : generatedTasks)
        .stream()
        .map(CertificationAssessorTaskEntity::getCandidateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<CertificationPracticalScoringConfigEntity> existingConfigs = configMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
            .in(CertificationPracticalScoringConfigEntity::getCandidateId, candidateIds));
    Set<Long> unavailableCandidateIds = new java.util.HashSet<>(generatedCandidateIds);
    (existingConfigs == null ? List.<CertificationPracticalScoringConfigEntity>of() : existingConfigs).stream()
        .map(CertificationPracticalScoringConfigEntity::getCandidateId)
        .filter(Objects::nonNull)
        .forEach(unavailableCandidateIds::add);
    Set<Long> configuredScopeIds = (existingConfigs == null
        ? List.<CertificationPracticalScoringConfigEntity>of()
        : existingConfigs).stream()
        .filter(config -> config.getCandidateId() == null)
        .map(CertificationPracticalScoringConfigEntity::getScopeId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (!configuredScopeIds.isEmpty()) {
      for (Long candidateId : candidateIds) {
        if (unavailableCandidateIds.contains(candidateId)) {
          continue;
        }
        CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
        if (candidate != null && taskId.equals(candidate.getTaskId())) {
          CertificationTaskScopeEntity scope = resolveScopeForCandidate(taskId, candidate, null);
          if (configuredScopeIds.contains(scope.getId())) {
            unavailableCandidateIds.add(candidateId);
          }
        }
      }
    }

    int created = 0;
    int existing = 0;
    int configured = 0;
    int questions = 0;
    for (Long candidateId : candidateIds) {
      if (unavailableCandidateIds.contains(candidateId)) {
        continue;
      }
      PracticalScoringModels.GenerateView result = linkCandidate(taskId, candidateId,
          new PracticalScoringModels.LinkCandidateReq(null, null, req.paperId(), req.groupId()));
      created += result.createdTasks();
      existing += result.existingTasks();
      configured += result.candidates();
      questions += result.questions();
    }
    return new PracticalScoringModels.BatchGenerateView(
        created, existing, configured, unavailableCandidateIds.size(), questions);
  }

  @Transactional
  public PracticalScoringModels.StartSessionView markCandidateAbsent(Long taskId, Long candidateId) {
    CertificationTaskEntity task = requireTask(taskId);
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "candidate not found");
    }
    if ("completed".equals(candidate.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "completed candidate cannot be marked absent");
    }

    List<CertificationAssessorTaskEntity> existingTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (existingTasks != null && existingTasks.stream().anyMatch(t -> "submitted".equals(t.getStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "submitted practical scores cannot be marked absent");
    }
    CertificationPracticalCandidateResultEntity existingResult = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidateId)
            .last("LIMIT 1"));
    if (existingResult != null && "completed".equals(existingResult.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "completed practical result cannot be marked absent");
    }

    CertificationPracticalScoringConfigEntity config = resolveAbsentConfig(taskId, candidate);
    List<CertificationTaskAssessorEntity> members = validateThreeAssessors(taskId, config.getGroupId());
    List<ExamOperatePaperDetailEntity> questions = paperQuestions(config.getPaperId());
    validateQuestionTemplates(questions);
    generateForCandidate(task, config, candidate, members, questions);
    applyAbsentScores(task, config, candidate, questions.size());
    candidate.setStatus("absent");
    candidateMapper.updateById(candidate);
    return startService.markAbsent(taskId, candidateId);
  }

  /**
   * Cancels all generated practical scoring data and configuration for one candidate before the
   * practical exam starts.
   */
  @Transactional
  public void cancelCandidateGeneratedTasks(Long taskId, Long candidateId) {
    CertificationTaskEntity task = requireTask(taskId);
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }

    List<CertificationAssessorTaskEntity> assessorTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (CollectionUtils.isEmpty(assessorTasks)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "该考生尚未生成实操评分任务");
    }

    // Locks and validates the unified-start session first, preventing a concurrent start from
    // leaving a started session without its scoring tasks.
    startService.cancelUnstartedSession(task.getId(), candidateId);

    List<Long> assessorTaskIds = assessorTasks.stream()
        .map(CertificationAssessorTaskEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (!assessorTaskIds.isEmpty()) {
      scoreMapper.delete(new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
          .in(CertificationAssessorTaskScoreEntity::getAssessorTaskId, assessorTaskIds));
      assessorTaskMapper.delete(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
          .in(CertificationAssessorTaskEntity::getId, assessorTaskIds));
    }
    questionResultMapper.delete(new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
        .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId)
        .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidateId));
    candidateResultMapper.delete(new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
        .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
        .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidateId));
    configMapper.delete(new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
        .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
        .eq(CertificationPracticalScoringConfigEntity::getCandidateId, candidateId));
  }

  @Transactional
  public PracticalScoringModels.ImportPaperView importCustomPaper(Long taskId, MultipartFile file) {
    CertificationTaskEntity task = requireTask(taskId);
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择评分记录表文件");
    }
    PlatformUserPrincipal user = SecurityContext.currentUser();
    ParsedPracticalPaper parsed = parseScoreRecordWorkbook(file);
    LocalDateTime importedAt = LocalDateTime.now();
    String importedAtText = importedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    ExamOperatePaperEntity paper = new ExamOperatePaperEntity();
    paper.setMakeId(null);
    paper.setSource(2);
    paper.setWorkTypeId(null);
    paper.setCourseId(null);
    paper.setNum(parsed.questions().size());
    paper.setCode("CERT-" + taskId + "-"
        + importedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
    paper.setLevel(null);
    paper.setTitle(parsed.title() + "-" + importedAtText);
    paper.setTime(null);
    paper.setTotalPoint(parsed.totalScore().intValue());
    paper.setPassPoint(null);
    paper.setComments("认定筹备导入：" + file.getOriginalFilename());
    paper.setUserId(user.getUserId().intValue());
    paper.setStatus(1);
    paper.setSortNumber(100);
    paper.setTenantId(user.getTenantId().intValue());
    paper.setCreateTime(importedAt);
    paperMapper.insert(paper);

    int sort = 1;
    int itemCount = 0;
    for (ParsedPracticalQuestion question : parsed.questions()) {
      ExamOperatePaperDetailEntity detail = new ExamOperatePaperDetailEntity();
      detail.setPaperId(paper.getId());
      detail.setTitle(question.title());
      detail.setStudentPrepare(question.studentPrepare());
      detail.setContent(question.content());
      detail.setScore(question.score());
      detail.setWeight(100);
      detail.setEquipPrepare(question.equipmentPrepare());
      detail.setExamContent(question.content());
      detail.setScoringTemplateJson(writeJson(question.items()));
      detail.setUserId(user.getUserId().intValue());
      detail.setStatus(1);
      detail.setSortNumber(sort++);
      detail.setTenantId(user.getTenantId().intValue());
      detail.setCreateTime(importedAt);
      paperDetailMapper.insert(detail);
      itemCount += question.items().size();
    }

    return new PracticalScoringModels.ImportPaperView(
        paper.getId().longValue(), paper.getTitle(), parsed.questions().size(), itemCount);
  }

  @Transactional(readOnly = true)
  public List<PracticalScoringModels.AssessorTaskListItem> myTasks(String status) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    Long activeTaskId = workbenchContextService.current().activeTaskId();
    LambdaQueryWrapper<CertificationAssessorTaskEntity> wrapper =
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId())
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId)
            .orderByAsc(CertificationAssessorTaskEntity::getStatus)
            .orderByDesc(CertificationAssessorTaskEntity::getUpdatedAt);
    if (StringUtils.hasText(status)) {
      wrapper.eq(CertificationAssessorTaskEntity::getStatus, status);
    }
    if (activeTaskId != null) {
      wrapper.eq(CertificationAssessorTaskEntity::getTaskId, activeTaskId);
    }
    return assessorTaskMapper.selectList(wrapper).stream().map(this::listItem).toList();
  }

  @Transactional(readOnly = true)
  public List<PracticalScoringModels.AssessorExamListItem> myExams(String status) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    Long activeTaskId = workbenchContextService.current().activeTaskId();
    List<CertificationAssessorTaskEntity> tasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId())
            .eq(activeTaskId != null, CertificationAssessorTaskEntity::getTaskId, activeTaskId)
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByDesc(CertificationAssessorTaskEntity::getUpdatedAt));
    Map<Long, Integer> paperTotals = paperTotals(tasks);
    return groupExamTasks(tasks).values().stream()
        .map(group -> examListItem(group, paperTotals))
        .filter(item -> !StringUtils.hasText(status) || status.equals(item.status()))
        .sorted(Comparator.comparing(PracticalScoringModels.AssessorExamListItem::updatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.AssessorExamDetail myExam(Long taskId, Long candidateId) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    workbenchContextService.requireAccessibleTask(assessor, taskId);
    List<CertificationAssessorTaskEntity> tasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidateId)
            .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId())
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByAsc(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (CollectionUtils.isEmpty(tasks)) {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      log.warn("用户{}尝试访问非本人实操考试任务 taskId={}, candidateId={}",
          user.getUserId(), taskId, candidateId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该实操考试任务");
    }
    Map<Long, Integer> paperTotals = paperTotals(tasks);
    List<PracticalScoringModels.AssessorTaskDetail> questions = tasks.stream()
        .sorted(Comparator.comparing(CertificationAssessorTaskEntity::getPaperQuestionId))
        .map(this::detail)
        .toList();
    return new PracticalScoringModels.AssessorExamDetail(examListItem(tasks, paperTotals), questions);
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.AssessorTaskDetail myTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = requireOwnedTask(assessorTaskId, false);
    return detail(task);
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.AssessorTaskDetail adminTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = assessorTaskMapper.selectById(assessorTaskId);
    if (task == null || task.getCandidateId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分任务不存在");
    }
    return detail(task, startService.session(task.getTaskId(), task.getCandidateId()));
  }

  @Transactional
  public PracticalScoringModels.AssessorTaskDetail saveDraft(
      Long assessorTaskId,
      PracticalScoringModels.SaveScoreReq req
  ) {
    return saveScore(assessorTaskId, req, false);
  }

  @Transactional
  public PracticalScoringModels.AssessorTaskDetail submit(
      Long assessorTaskId,
      PracticalScoringModels.SaveScoreReq req
  ) {
    return saveScore(assessorTaskId, req, true);
  }

  @Transactional
  public PracticalScoringModels.AssessorTaskDetail adjustScore(
      Long assessorTaskId,
      PracticalScoringModels.AdjustScoreReq req
  ) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationAssessorTaskEntity task = assessorTaskMapper.lockById(user.getTenantId(), assessorTaskId);
    if (task == null || task.getCandidateId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分任务不存在");
    }
    if (!"submitted".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已提交的异常评分任务可在实操管理中修改");
    }
    int currentVersion = nvl(task.getVersionNo(), 0);
    if (!Objects.equals(req.version(), currentVersion)) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分任务已被更新，请刷新后再修改");
    }
    List<CertificationAssessorTaskScoreEntity> scores = scores(task.getId());
    ScoreInputIndex requestItems = scoreInputIndex(req.items());
    if (scores.isEmpty()
        || req.items().size() != scores.size()
        || scores.stream().anyMatch(score -> scoreInput(score, requestItems) == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素必须完整提交");
    }
    BigDecimal total = applyScoreInputs(scores, requestItems);
    task.setProgressNote(req.note());
    task.setTotalScore(total);
    task.setStatus("submitted");
    task.setSubmittedAt(LocalDateTime.now());
    task.setReturnedAt(null);
    task.setReturnedBy(null);
    task.setReturnReason(null);
    task.setVersionNo(currentVersion + 1);
    assessorTaskMapper.updateById(task);
    writeHistory(task, "adjust", req.reason(), user.getUserId());
    calculateResults(task);
    return detail(task, startService.session(task.getTaskId(), task.getCandidateId()));
  }

  @Transactional
  public void returnTask(Long assessorTaskId, String reason) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationAssessorTaskEntity task = assessorTaskMapper.lockById(user.getTenantId(), assessorTaskId);
    if (task == null || task.getCandidateId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分任务不存在");
    }
    if (startService.isCompleted(task.getTaskId(), task.getCandidateId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试已完成，评分数据已锁定");
    }
    writeHistory(task, "return", reason, user.getUserId());
    task.setStatus("returned");
    task.setReturnedAt(LocalDateTime.now());
    task.setReturnedBy(user.getUserId());
    task.setReturnReason(reason);
    task.setSubmittedAt(null);
    task.setTotalScore(null);
    task.setRevisionNo(nvl(task.getRevisionNo(), 1) + 1);
    task.setVersionNo(nvl(task.getVersionNo(), 0) + 1);
    assessorTaskMapper.updateById(task);
    resetScoresToDefault(scores(task.getId()));
    invalidateResults(task);
  }

  @Transactional
  public PracticalScoringModels.StartSessionView completeByAssessorTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity ownedTask = requireOwnedTask(assessorTaskId, false);
    return completeCandidate(ownedTask.getTaskId(), ownedTask.getCandidateId());
  }

  @Transactional
  public PracticalScoringModels.StartSessionView completeCandidate(Long taskId, Long candidateId) {
    List<CertificationAssessorTaskEntity> tasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (CollectionUtils.isEmpty(tasks)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分任务不存在");
    }
    long submitted = tasks.stream().filter(task -> "submitted".equals(task.getStatus())).count();
    if (submitted != tasks.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名考评员全部提交后才能完成考试");
    }
    calculateCandidateResult(taskId, candidateId);
    CertificationPracticalCandidateResultEntity result = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidateId)
            .last("LIMIT 1"));
    if (result == null || !"completed".equals(result.getStatus())) {
      if (result != null && "review_required".equals(result.getStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "三名考评员评分偏差超过10分，请审核并退回修正后再完成考试");
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考生成绩尚未汇总完成");
    }
    String summaryFileName = "practical-summary-" + taskId + "-" + candidateId + ".xlsx";
    return startService.completeCandidate(taskId, candidateId, summaryFileName);
  }

  /**
   * Closes the practical scoring work after every generated candidate result has been settled.
   * Scoring rows remain as audit history; only assessor-group memberships are released.
   */
  @Transactional
  public PracticalScoringModels.CompleteTaskView completeTask(Long taskId) {
    requireTask(taskId);
    List<CertificationAssessorTaskEntity> scoringTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (CollectionUtils.isEmpty(scoringTasks)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前认定任务尚未生成实操评分任务，不能完成评分任务");
    }

    Set<Long> candidateIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getCandidateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<CertificationPracticalCandidateResultEntity> candidateResults = candidateResultMapper.selectList(
            new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
                .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId));
    Map<Long, String> resultStatuses = (candidateResults == null ? List.<CertificationPracticalCandidateResultEntity>of() : candidateResults)
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CertificationPracticalCandidateResultEntity::getCandidateId,
            CertificationPracticalCandidateResultEntity::getStatus,
            (left, right) -> left));
    long unsettled = candidateIds.stream()
        .filter(candidateId -> !"completed".equals(resultStatuses.get(candidateId))
            && !"absent".equals(resultStatuses.get(candidateId)))
        .count();
    if (unsettled > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "仍有 " + unsettled + " 名考生的实操评分未完成，不能结束评分任务");
    }

    Set<Long> groupIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getGroupId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (groupIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分任务缺少考评组绑定，不能结束评分任务");
    }
    List<CertificationTaskAssessorEntity> bindings = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .in(CertificationTaskAssessorEntity::getGroupId, groupIds));
    if (bindings == null || bindings.isEmpty()) {
      return new PracticalScoringModels.CompleteTaskView(groupIds.size(), 0);
    }
    Set<Long> assessorIds = bindings.stream()
        .map(CertificationTaskAssessorEntity::getAssessorId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    taskAssessorMapper.delete(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
        .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
        .in(CertificationTaskAssessorEntity::getGroupId, groupIds));
    assessorIds.forEach(this::refreshAssessorWorkStatus);
    return new PracticalScoringModels.CompleteTaskView(groupIds.size(), assessorIds.size());
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.ProgressView progress(Long taskId) {
    requireTask(taskId);
    List<CertificationAssessorTaskEntity> tasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId));
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId));
    Map<Long, CertificationPracticalCandidateResultEntity> resultMap = candidateResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId))
        .stream().collect(Collectors.toMap(CertificationPracticalCandidateResultEntity::getCandidateId, Function.identity()));
    List<CertificationPracticalQuestionResultEntity> questionResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId));
    Map<String, CertificationPracticalQuestionResultEntity> questionResultMap = questionResults.stream()
        .collect(Collectors.toMap(
            result -> result.getCandidateId() + ":" + result.getPaperQuestionId(),
            Function.identity(),
            (left, right) -> left));
    Map<Long, CertificationCandidateEntity> candidateMap = candidates.stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity()));
    List<Long> assessorIds = tasks.stream()
        .map(CertificationAssessorTaskEntity::getAssessorId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Long, String> assessorNames = assessorIds.isEmpty()
        ? Map.of()
        : assessorMapper.selectBatchIds(assessorIds).stream()
            .collect(Collectors.toMap(
                CertificationInfoAssessorEntity::getId,
                CertificationInfoAssessorEntity::getAssessorName));
    List<PracticalScoringModels.CandidateProgress> rows = candidates.stream().map(candidate -> {
      int totalTasks = (int) tasks.stream().filter(t -> candidate.getId().equals(t.getCandidateId())).count();
      int submittedTasks = (int) tasks.stream().filter(t -> candidate.getId().equals(t.getCandidateId()))
          .filter(t -> "submitted".equals(t.getStatus())).count();
      int totalQuestions = (int) questionResults.stream()
          .filter(r -> candidate.getId().equals(r.getCandidateId())).count();
      int completedQuestions = (int) questionResults.stream()
          .filter(r -> candidate.getId().equals(r.getCandidateId()))
          .filter(r -> "completed".equals(r.getStatus())).count();
      CertificationPracticalCandidateResultEntity result = resultMap.get(candidate.getId());
      return new PracticalScoringModels.CandidateProgress(
          candidate.getId(), candidate.getCandidateNo(), candidate.getCandidateName(),
          submittedTasks, totalTasks, completedQuestions, totalQuestions,
          resolveCandidateTotalScore(candidate.getId(), result, questionResults),
          result == null ? "pending" : result.getStatus(),
          startService.session(taskId, candidate.getId()));
    }).toList();
    return new PracticalScoringModels.ProgressView(
        taskId,
        (int) tasks.stream().filter(t -> "submitted".equals(t.getStatus())).count(),
        tasks.size(),
        (int) questionResults.stream().filter(r -> "completed".equals(r.getStatus())).count(),
        questionResults.size(),
        rows,
        tasks.stream().map(scoringTask -> {
          CertificationCandidateEntity candidate = candidateMap.get(scoringTask.getCandidateId());
          CertificationPracticalQuestionResultEntity questionResult = questionResultMap.get(
              scoringTask.getCandidateId() + ":" + scoringTask.getPaperQuestionId());
          BigDecimal scoreDeviation = scoreDeviation(tasks, scoringTask.getCandidateId(), scoringTask.getPaperQuestionId());
          String questionResultStatus = questionResult == null ? "pending" : questionResult.getStatus();
          return new PracticalScoringModels.TaskProgressItem(
              scoringTask.getId(),
              scoringTask.getCandidateId(),
              candidate == null ? "" : candidate.getCandidateName(),
              scoringTask.getPaperQuestionId(),
              scoringTask.getTitle(),
              scoringTask.getAssessorId(),
              assessorNames.getOrDefault(scoringTask.getAssessorId(), ""),
              scoringTask.getStatus(),
              scoringTask.getTotalScore(),
              scoringTask.getSubmittedAt(),
              questionResultStatus,
              scoreDeviation,
              "review_required".equals(questionResultStatus));
        }).toList()
    );
  }

  @Transactional(readOnly = true)
  public ExportResultDTO exportSummary(Long taskId, List<Long> candidateIds) {
    CertificationTaskEntity task = requireTask(taskId);
    if (CollectionUtils.isEmpty(candidateIds)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择需要导出的考生");
    }
    List<Long> ids = candidateIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择需要导出的考生");
    }
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .in(CertificationCandidateEntity::getId, ids)
            .orderByAsc(CertificationCandidateEntity::getCandidateNo)
            .orderByAsc(CertificationCandidateEntity::getId));
    if (candidates.size() != ids.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "存在不属于当前认定任务的考生");
    }
    Map<Long, CertificationCandidateEntity> candidateById = candidates.stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity()));
    List<CertificationPracticalCandidateResultEntity> candidateResults = candidateResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
            .in(CertificationPracticalCandidateResultEntity::getCandidateId, ids));
    Map<Long, CertificationPracticalCandidateResultEntity> resultByCandidate = candidateResults.stream()
        .collect(Collectors.toMap(CertificationPracticalCandidateResultEntity::getCandidateId, Function.identity()));
    List<CertificationPracticalQuestionResultEntity> questionResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId)
            .in(CertificationPracticalQuestionResultEntity::getCandidateId, ids)
            .orderByAsc(CertificationPracticalQuestionResultEntity::getCandidateId)
            .orderByAsc(CertificationPracticalQuestionResultEntity::getPaperQuestionId));
    List<CertificationAssessorTaskEntity> scoringTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .in(CertificationAssessorTaskEntity::getCandidateId, ids)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByAsc(CertificationAssessorTaskEntity::getCandidateId)
            .orderByAsc(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByAsc(CertificationAssessorTaskEntity::getAssessorId));
    if ((questionResults == null || questionResults.isEmpty()) && (scoringTasks == null || scoringTasks.isEmpty())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的实操评分任务");
    }
    if (questionResults == null) {
      questionResults = List.of();
    }
    if (scoringTasks == null) {
      scoringTasks = List.of();
    }
    questionResults = completeExportQuestionResults(questionResults, scoringTasks);

    Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate =
        questionResults.stream().collect(Collectors.groupingBy(
            CertificationPracticalQuestionResultEntity::getCandidateId,
            LinkedHashMap::new,
            Collectors.toList()));
    Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion =
        scoringTasks.stream().collect(Collectors.groupingBy(
            taskItem -> taskItem.getCandidateId() + ":" + taskItem.getPaperQuestionId(),
            LinkedHashMap::new,
            Collectors.toList()));
    int questionCount = questionResultsByCandidate.values().stream()
        .mapToInt(List::size)
        .max()
        .orElse(0);
    if (questionCount > SUMMARY_QUESTION_COUNT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "评分记录汇总表模板最多支持" + SUMMARY_QUESTION_COUNT + "道试题，当前评分任务包含" + questionCount + "道试题");
    }
    List<CertificationTaskAssessorEntity> assessors = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskAssessorEntity::getSortOrder)
            .orderByAsc(CertificationTaskAssessorEntity::getId));
    if (assessors == null) {
      assessors = List.of();
    }
    Map<Long, Integer> assessorOrder = new HashMap<>();
    for (int index = 0; index < assessors.size(); index++) {
      CertificationTaskAssessorEntity assessor = assessors.get(index);
      if (assessor.getAssessorId() != null) {
        assessorOrder.putIfAbsent(assessor.getAssessorId(), index);
      }
    }
    tasksByCandidateQuestion.values().forEach(items -> items.sort(Comparator.comparingInt(
        item -> assessorOrder.getOrDefault(item.getAssessorId(), Integer.MAX_VALUE))));
    List<Long> assessorTaskIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    List<CertificationAssessorTaskScoreEntity> scoreRows = assessorTaskIds.isEmpty()
        ? List.of()
        : scoreMapper.selectList(new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
            .in(CertificationAssessorTaskScoreEntity::getAssessorTaskId, assessorTaskIds)
            .orderByAsc(CertificationAssessorTaskScoreEntity::getAssessorTaskId)
            .orderByAsc(CertificationAssessorTaskScoreEntity::getStepNo));
    if (scoreRows == null) {
      scoreRows = List.of();
    }
    Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask =
        scoreRows.stream().collect(Collectors.groupingBy(
            CertificationAssessorTaskScoreEntity::getAssessorTaskId,
            LinkedHashMap::new,
            Collectors.toList()));
    byte[] data = buildSummaryWorkbookFromTemplate(task, candidates, resultByCandidate,
        questionResultsByCandidate, tasksByCandidateQuestion, scoresByAssessorTask, assessors, questionCount);
    String filename = "评分记录汇总表_" + task.getTaskNo() + "_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .format(LocalDateTime.now()) + ".xlsx";
    return new ExportResultDTO(data, filename);
  }

  private String formatDate(LocalDate date) {
    return date == null ? "" : date.toString();
  }

  private byte[] buildSummaryWorkbookFromTemplate(
      CertificationTaskEntity task,
      List<CertificationCandidateEntity> candidates,
      Map<Long, CertificationPracticalCandidateResultEntity> resultByCandidate,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask,
      List<CertificationTaskAssessorEntity> assessors,
      int questionCount
  ) {
    ClassPathResource template = new ClassPathResource(SUMMARY_TEMPLATE_RESOURCE);
    try (InputStream input = template.getInputStream();
        Workbook workbook = WorkbookFactory.create(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = requireSummaryTemplateSheet(workbook);
      Row templateDataRow = sheet.getRow(SUMMARY_DATA_ROW_INDEX);
      CellStyle decimalStyle = decimalStyle(workbook, templateDataRow.getCell(SUMMARY_FIRST_QUESTION_COLUMN));
      CellStyle summaryDecimalStyle = decimalStyle(workbook, templateDataRow.getCell(SUMMARY_FIRST_QUESTION_COLUMN));
      CellStyle integerStyle = integerStyle(workbook, templateDataRow.getCell(SUMMARY_FIRST_QUESTION_COLUMN + 4));
      summaryDecimalStyle.setShrinkToFit(true);
      integerStyle.setShrinkToFit(true);
      writeTemplateCell(sheet, 1, 0, "计划编码：" + nvlText(task.getTaskNo()));
      writeTemplateCell(sheet, 1, SUMMARY_FIRST_QUESTION_COLUMN, "考 点 ：" + nvlText(task.getCompanyName()));
      writeTemplateCell(sheet, 2, 0, "计划名称：" + nvlText(task.getTaskName()));
      writeTemplateCell(sheet, 2, SUMMARY_FIRST_QUESTION_COLUMN, "考试时间：" + formatDate(task.getExamDate()));

      int sequence = 1;
      for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
        CertificationCandidateEntity candidate = candidates.get(candidateIndex);
        Row row = candidateIndex == 0
            ? templateDataRow
            : sheet.createRow(SUMMARY_DATA_ROW_INDEX + candidateIndex);
        if (candidateIndex > 0) {
          copyDataRowStyle(templateDataRow, row);
        }
        writeTemplateCell(row, 0, sequence++);
        writeTemplateCell(row, 1, nvlText(candidate.getCandidateName()));
        writeTemplateCell(row, 2, nvlText(candidate.getAdmissionTicketNo()));
        List<CertificationPracticalQuestionResultEntity> questionResults =
            questionResultsByCandidate.getOrDefault(candidate.getId(), List.of());
        int actualTotalColumn = SUMMARY_FIRST_QUESTION_COLUMN
            + questionCount * SUMMARY_SCORE_COLUMNS_PER_QUESTION;
        BigDecimal questionFinalTotal = BigDecimal.ZERO;
        for (int questionIndex = 0; questionIndex < questionCount; questionIndex++) {
          int startColumn = SUMMARY_FIRST_QUESTION_COLUMN
              + questionIndex * SUMMARY_SCORE_COLUMNS_PER_QUESTION;
          CertificationPracticalQuestionResultEntity questionResult = questionResultAt(questionResults, questionIndex);
          List<CertificationAssessorTaskEntity> scoreTasks = tasksByCandidateQuestion.getOrDefault(
              candidate.getId() + ":" + (questionResult == null ? null : questionResult.getPaperQuestionId()), List.of());
          for (int assessorIndex = 0; assessorIndex < 3; assessorIndex++) {
            writeDecimalCell(row, startColumn + assessorIndex,
                assessorIndex < scoreTasks.size() ? exportAssessorTaskScore(scoreTasks.get(assessorIndex)) : BigDecimal.ZERO);
          }
          BigDecimal finalScore = exportQuestionFinalScore(questionResult);
          writeDecimalCell(row, startColumn + 3, exportQuestionAverageScore(questionResult));
          writeTemplateCell(row, startColumn + 4, SUMMARY_WEIGHT_VALUE);
          writeDecimalCell(row, startColumn + 5, finalScore);
          questionFinalTotal = questionFinalTotal.add(finalScore);
        }
        writeDecimalCell(row, actualTotalColumn, questionFinalTotal.setScale(1, RoundingMode.HALF_UP));
      }
      trimSummarySheet(sheet, questionCount);
      applySummaryColumnWidths(sheet, questionCount);
      applySummaryInfoRowsLayout(sheet);
      applySummaryNumberStyles(sheet, questionCount, candidates.size(), summaryDecimalStyle, integerStyle);
      appendScoreRecordSheets(workbook, task, candidates, questionResultsByCandidate,
          tasksByCandidateQuestion, scoresByAssessorTask, assessors, decimalStyle);
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      log.warn("评分记录汇总表模板导出失败: {}", ex.getMessage(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出评分记录汇总表失败");
    }
  }

  private Sheet requireSummaryTemplateSheet(Workbook workbook) {
    if (workbook.getNumberOfSheets() == 0) {
      throw new IllegalStateException("评分记录汇总表模板不包含工作表");
    }
    Sheet sheet = workbook.getSheetAt(0);
    Row dataTemplateRow = sheet.getRow(SUMMARY_DATA_ROW_INDEX);
    if (dataTemplateRow == null || dataTemplateRow.getCell(SUMMARY_TOTAL_SCORE_COLUMN) == null) {
      throw new IllegalStateException("评分记录汇总表模板缺少第7行数据样式模板");
    }
    return sheet;
  }

  private void copyDataRowStyle(Row source, Row target) {
    target.setHeight(source.getHeight());
    for (int column = 0; column <= SUMMARY_TOTAL_SCORE_COLUMN; column++) {
      Cell sourceCell = source.getCell(column);
      if (sourceCell != null) {
        target.createCell(column).setCellStyle(sourceCell.getCellStyle());
      }
    }
  }

  /**
   * 按实际题数裁剪汇总表：将模板中第51列（合计得分）复制到实际位置，清除多余列，重建合并区域。
   */
  private void trimSummarySheet(Sheet sheet, int questionCount) {
    int targetTotalColumn = SUMMARY_FIRST_QUESTION_COLUMN
        + questionCount * SUMMARY_SCORE_COLUMNS_PER_QUESTION;
    int firstExtraColumn = targetTotalColumn + 1;

    // 将模板合计得分列（col 51）的值和样式复制到实际位置（数据行的值已在主循环中写入）
    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
      Row sourceRow = sheet.getRow(r);
      if (sourceRow == null) continue;
      Cell src = sourceRow.getCell(SUMMARY_TOTAL_SCORE_COLUMN);
      if (src == null) continue;
      Cell dst = sourceRow.getCell(targetTotalColumn);
      if (dst == null) dst = sourceRow.createCell(targetTotalColumn);
      // 只复制值到表头/标题行（第 1-5 行），避免覆盖主循环写入的数据
      if (r >= 1 && r <= 5 && src.getCellType() == CellType.STRING) {
        dst.setCellValue(src.getStringCellValue());
      }
      dst.setCellStyle(src.getCellStyle());
    }

    // 清除多余列（多余试题列 + 模板原合计得分列位置）
    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;
      for (int c = firstExtraColumn; c <= Math.max(row.getLastCellNum() - 1, SUMMARY_TOTAL_SCORE_COLUMN); c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          row.removeCell(cell);
        }
      }
    }

    // 清除模板中所有合并区域，再按实际列数重建
    int regionCount = sheet.getNumMergedRegions();
    for (int i = regionCount - 1; i >= 0; i--) {
      sheet.removeMergedRegion(i);
    }

    int lastColumn = targetTotalColumn; // 0-based
    // 试题区分界线：左侧 col 0-2 放计划编码/名称，右侧 col 3+ 放考点/考试时间
    int infoDivider = SUMMARY_FIRST_QUESTION_COLUMN; // 3

    // 标题行（row 0）跨全部实际列
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));
    // 计划编码（row 1 左） + 考点（row 1 右）
    sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, infoDivider - 1));
    sheet.addMergedRegion(new CellRangeAddress(1, 1, infoDivider, lastColumn));
    // 计划名称（row 2 左） + 考试时间（row 2 右）
    sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, infoDivider - 1));
    sheet.addMergedRegion(new CellRangeAddress(2, 2, infoDivider, lastColumn));
    // 签字行（row 3）跨全部实际列
    sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, lastColumn));
    // 序号（col 0）、姓名（col 1）、准考证号（col 2）竖向合并 row 4-5
    for (int col = 0; col < SUMMARY_FIRST_QUESTION_COLUMN; col++) {
      sheet.addMergedRegion(new CellRangeAddress(4, 5, col, col));
    }
    // 每道试题标题横向合并 6 列（row 4）
    for (int q = 0; q < questionCount; q++) {
      int start = SUMMARY_FIRST_QUESTION_COLUMN + q * SUMMARY_SCORE_COLUMNS_PER_QUESTION;
      sheet.addMergedRegion(new CellRangeAddress(4, 4, start, start + SUMMARY_SCORE_COLUMNS_PER_QUESTION - 1));
    }
    // 合计得分列竖向合并 row 4-5
    sheet.addMergedRegion(new CellRangeAddress(4, 5, targetTotalColumn, targetTotalColumn));
  }

  private void writeTemplateCell(Sheet sheet, int rowIndex, int column, Object value) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }
    writeTemplateCell(row, column, value);
  }

  private void writeTemplateCell(Row row, int column, Object value) {
    Cell cell = row.getCell(column);
    if (cell == null) {
      cell = row.createCell(column);
    }
    if (value == null) {
      cell.setBlank();
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(String.valueOf(value));
    }
  }

  private void writeDecimalCell(Row row, int column, BigDecimal value) {
    writeTemplateCell(row, column, value);
  }

  private CellStyle decimalStyle(Workbook workbook, Cell baseCell) {
    return scoreStyle(workbook, baseCell, DECIMAL_SCORE_FORMAT);
  }

  private CellStyle scoreStyle(Workbook workbook, Cell baseCell, String dataFormat) {
    CellStyle style = workbook.createCellStyle();
    if (baseCell != null && baseCell.getCellStyle() != null) {
      style.cloneStyleFrom(baseCell.getCellStyle());
    }
    style.setDataFormat(workbook.createDataFormat().getFormat(dataFormat));
    return style;
  }

  private CellStyle integerStyle(Workbook workbook, Cell baseCell) {
    CellStyle style = workbook.createCellStyle();
    if (baseCell != null && baseCell.getCellStyle() != null) {
      style.cloneStyleFrom(baseCell.getCellStyle());
    }
    style.setDataFormat(workbook.createDataFormat().getFormat(INTEGER_SCORE_FORMAT));
    return style;
  }

  private void applySummaryColumnWidths(Sheet sheet, int questionCount) {
    Row headerRow = sheet.getRow(SUMMARY_HEADER_ROW_INDEX);
    DataFormatter formatter = new DataFormatter();
    int lastQuestionColumn = SUMMARY_FIRST_QUESTION_COLUMN
        + questionCount * SUMMARY_SCORE_COLUMNS_PER_QUESTION - 1;
    int totalColumn = lastQuestionColumn + 1;
    for (int column = SUMMARY_FIRST_QUESTION_COLUMN; column <= lastQuestionColumn; column++) {
      String header = headerRow == null || headerRow.getCell(column) == null
          ? ""
          : formatter.formatCellValue(headerRow.getCell(column));
      int offset = (column - SUMMARY_FIRST_QUESTION_COLUMN) % SUMMARY_SCORE_COLUMNS_PER_QUESTION;
      if (isSummaryAssessorColumn(header, offset)) {
        sheet.setColumnWidth(column, SUMMARY_ASSESSOR_COLUMN_WIDTH);
        sheet.setColumnHidden(column, false);
      } else if (isSummaryAverageOrFinalColumn(header, offset)) {
        sheet.setColumnWidth(column, SUMMARY_AVERAGE_FINAL_COLUMN_WIDTH);
        sheet.setColumnHidden(column, false);
      } else if (isSummaryWeightColumnHeader(header, offset)) {
        sheet.setColumnWidth(column, SUMMARY_WEIGHT_COLUMN_WIDTH);
        sheet.setColumnHidden(column, false);
      }
    }
    sheet.setColumnWidth(totalColumn, SUMMARY_TOTAL_DISPLAY_COLUMN_WIDTH);
    sheet.setColumnHidden(totalColumn, false);
  }

  private boolean isSummaryAssessorColumn(String header, int offset) {
    return offset <= 2 || header.contains("考评员");
  }

  private boolean isSummaryAverageOrFinalColumn(String header, int offset) {
    return offset == 3 || offset == 5 || header.contains("平均分") || header.contains("最后");
  }

  private boolean isSummaryWeightColumnHeader(String header, int offset) {
    return offset == 4 || header.contains("权重");
  }

  private void applySummaryInfoRowsLayout(Sheet sheet) {
    Map<Short, CellStyle> wrapStyles = new HashMap<>();
    for (int rowIndex = 1; rowIndex <= 2; rowIndex++) {
      applyWrapText(sheet, rowIndex, 0, wrapStyles);
      applyWrapText(sheet, rowIndex, SUMMARY_FIRST_QUESTION_COLUMN, wrapStyles);
      adjustSummaryInfoRowHeight(sheet, rowIndex);
    }
  }

  private void adjustSummaryInfoRowHeight(Sheet sheet, int rowIndex) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return;
    }
    float baseHeight = row.getHeightInPoints() > 0
        ? row.getHeightInPoints()
        : sheet.getDefaultRowHeightInPoints();
    int maxLines = 1;
    DataFormatter formatter = new DataFormatter();
    for (int column : List.of(0, SUMMARY_FIRST_QUESTION_COLUMN)) {
      Cell cell = row.getCell(column);
      if (cell == null) {
        continue;
      }
      String text = formatter.formatCellValue(cell).trim();
      if (!StringUtils.hasText(text)) {
        continue;
      }
      CellRangeAddress mergedRange = mergedRangeAt(sheet, rowIndex, column);
      int firstColumn = mergedRange == null ? column : mergedRange.getFirstColumn();
      int lastColumn = mergedRange == null ? column : mergedRange.getLastColumn();
      double charsPerLine = Math.max(8D, mergedColumnWidthChars(sheet, firstColumn, lastColumn) * 0.9D);
      maxLines = Math.max(maxLines, estimatedWrappedLineCount(text, charsPerLine));
    }
    row.setHeightInPoints(Math.max(baseHeight, baseHeight * maxLines));
  }

  private int estimatedWrappedLineCount(String text, double charsPerLine) {
    if (!StringUtils.hasText(text)) {
      return 1;
    }
    int lineCount = 0;
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    for (String line : normalized.split("\n", -1)) {
      lineCount += Math.max(1, (int) Math.ceil(weightedTextLength(line) / charsPerLine));
    }
    return Math.max(1, lineCount);
  }

  private void applySummaryNumberStyles(
      Sheet sheet,
      int questionCount,
      int candidateCount,
      CellStyle decimalStyle,
      CellStyle integerStyle
  ) {
    int totalColumn = SUMMARY_FIRST_QUESTION_COLUMN + questionCount * SUMMARY_SCORE_COLUMNS_PER_QUESTION;
    for (int rowIndex = SUMMARY_DATA_ROW_INDEX; rowIndex < SUMMARY_DATA_ROW_INDEX + candidateCount; rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      for (int column = SUMMARY_FIRST_QUESTION_COLUMN; column <= totalColumn; column++) {
        Cell cell = row.getCell(column);
        if (cell != null) {
          cell.setCellStyle(summaryCellUsesIntegerStyle(cell, column, questionCount) ? integerStyle : decimalStyle);
        }
      }
    }
  }

  private boolean summaryCellUsesIntegerStyle(Cell cell, int column, int questionCount) {
    return isSummaryWeightColumn(column, questionCount) || isIntegerNumericCell(cell);
  }

  private boolean isIntegerNumericCell(Cell cell) {
    if (cell.getCellType() != CellType.NUMERIC) {
      return false;
    }
    double value = cell.getNumericCellValue();
    return Math.abs(value - Math.rint(value)) < 0.0000001D;
  }

  private boolean isSummaryWeightColumn(int column, int questionCount) {
    if (column < SUMMARY_FIRST_QUESTION_COLUMN) {
      return false;
    }
    int questionColumnOffset = column - SUMMARY_FIRST_QUESTION_COLUMN;
    return questionColumnOffset < questionCount * SUMMARY_SCORE_COLUMNS_PER_QUESTION
        && questionColumnOffset % SUMMARY_SCORE_COLUMNS_PER_QUESTION == 4;
  }

  private void appendScoreRecordSheets(
      Workbook workbook,
      CertificationTaskEntity task,
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask,
      List<CertificationTaskAssessorEntity> assessors,
      CellStyle decimalStyle
  ) {
    int questionCount = questionResultsByCandidate.values().stream()
        .mapToInt(List::size)
        .max()
        .orElse(0);
    ClassPathResource template = new ClassPathResource(SCORE_RECORD_TEMPLATE_RESOURCE);
    try (InputStream input = template.getInputStream();
        Workbook templateWorkbook = WorkbookFactory.create(input)) {
      List<ScoreRecordAssessor> recordAssessors = scoreRecordAssessors(assessors, tasksByCandidateQuestion);
      for (int questionIndex = 0; questionIndex < questionCount; questionIndex++) {
        List<CertificationAssessorTaskScoreEntity> templateItems = scoreRecordTemplateItems(
            candidates, questionResultsByCandidate, tasksByCandidateQuestion, scoresByAssessorTask, questionIndex);
        Sheet sourceSheet = chooseScoreRecordTemplateSheet(templateWorkbook, questionIndex, templateItems.size());
        for (int assessorIndex = 0; assessorIndex < recordAssessors.size(); assessorIndex++) {
          ScoreRecordAssessor assessor = recordAssessors.get(assessorIndex);
          List<CertificationCandidateEntity> assessorCandidates = candidatesForScoreRecordAssessor(
              candidates, questionResultsByCandidate, tasksByCandidateQuestion, questionIndex, assessor);
          if (assessorCandidates.isEmpty()) {
            continue;
          }
          Sheet sheet = copyTemplateSheet(workbook, sourceSheet,
              scoreRecordSheetName(questionIndex + 1, assessorIndex + 1, assessor.name()));
          appendScoreRecordSheet(sheet, task, assessorCandidates, questionResultsByCandidate,
              tasksByCandidateQuestion, scoresByAssessorTask, questionIndex, assessor);
        }
      }
    } catch (Exception ex) {
      log.warn("实操评分记录表模板导出失败: {}", ex.getMessage(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出实操评分记录表失败");
    }
  }

  private void appendScoreRecordSheet(
      Sheet sheet,
      CertificationTaskEntity task,
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask,
      int questionIndex,
      ScoreRecordAssessor assessor
  ) {
    String questionTitle = scoreRecordQuestionTitle(candidates, questionResultsByCandidate,
        tasksByCandidateQuestion, questionIndex);
    List<CertificationAssessorTaskScoreEntity> templateItems = scoreRecordTemplateItems(
        candidates, questionResultsByCandidate, tasksByCandidateQuestion, scoresByAssessorTask, questionIndex);
    int standardCount = templateItems.size();
    if (standardCount > SCORE_RECORD_RECOMMENDED_MAX_STANDARD_COUNT) {
      log.warn("评分记录表导出提示：当前试题评分标准数量超过推荐打印范围{}个，standardCount = {}, taskId = {}, questionNo = {}",
          SCORE_RECORD_RECOMMENDED_MAX_STANDARD_COUNT, standardCount, task.getId(), questionIndex + 1);
    }
    int totalColumn = calculateScoreRecordTotalColumn(standardCount);
    prepareScoreRecordSheetLayout(sheet, standardCount, candidates.size(), totalColumn);
    writeTemplateCell(sheet, 0, 0, "表" + (questionIndex + 1) + ":评分记录表");
    writeTemplateCell(sheet, 1, 0, "计划编码：" + nvlText(task.getTaskNo())
        + "                                 考 点 ：" + nvlText(task.getCompanyName()));
    writeTemplateCell(sheet, 2, 0, "计划名称：" + nvlText(task.getTaskName())
        + "        考试时间：" + formatChineseDate(task.getExamDate()) + "        考评员  签字：" + nvlText(assessor.name()));
    writeTemplateCell(sheet, 3, 3, "试题" + (questionIndex + 1) + "、" + questionTitle
        + "(" + scoreRecordQuestionScore(candidates, questionResultsByCandidate, tasksByCandidateQuestion,
        scoresByAssessorTask, questionIndex) + "分)");
    writeTemplateCell(sheet, 4, 4, questionTitle);
    clearScoreRecordTemplateRows(sheet, candidates.size(), totalColumn);
    writeScoreRecordTemplateItems(sheet, templateItems, totalColumn);

    for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
      CertificationCandidateEntity candidate = candidates.get(candidateIndex);
      CertificationPracticalQuestionResultEntity questionResult =
          questionResultAt(questionResultsByCandidate.get(candidate.getId()), questionIndex);
      int rowIndex = SCORE_RECORD_CANDIDATE_START_ROW + candidateIndex;
      ensureScoreRecordCandidateRow(sheet, rowIndex, totalColumn);
      Row row = sheet.getRow(rowIndex);
      writeTemplateCell(row, 0, candidateIndex + 1);
      writeTemplateCell(row, 1, nvlText(candidate.getCandidateName()));
      writeTemplateCell(row, 2, nvlText(candidate.getAdmissionTicketNo()));
      List<CertificationAssessorTaskEntity> scoreTasks = scoreTasksForCandidateQuestion(
          tasksByCandidateQuestion, candidate.getId(), questionResult);
      CertificationAssessorTaskEntity scoreTask = scoreTaskForAssessor(scoreTasks, assessor);
      List<CertificationAssessorTaskScoreEntity> items = scoreTask == null
          ? firstScoreItems(scoreTasks, scoresByAssessorTask)
          : scoresByAssessorTask.getOrDefault(scoreTask.getId(), List.of());
      for (int itemIndex = 0; itemIndex < items.size()
          && SCORE_RECORD_SCORE_START_COLUMN + itemIndex * 2 + 1 < totalColumn; itemIndex++) {
        CertificationAssessorTaskScoreEntity item = items.get(itemIndex);
        int deductColumn = SCORE_RECORD_SCORE_START_COLUMN + itemIndex * 2;
        int scoreColumn = deductColumn + 1;
        writeTemplateCell(row, deductColumn, exportScoreItemValue(scoreTask, item.getDeductedScore()));
        writeTemplateCell(row, scoreColumn, exportScoreItemValue(scoreTask, item.getAwardedScore()));
        applyDecimalCellStyle(row.getCell(deductColumn));
        applyDecimalCellStyle(row.getCell(scoreColumn));
      }
      writeTemplateCell(row, totalColumn, exportAssessorTaskScore(scoreTask));
      applyDecimalCellStyle(row.getCell(totalColumn));
      applyScoreRecordCandidateRowHeight(sheet, rowIndex, totalColumn);
    }
    applyScoreRecordPrintSettings(sheet, totalColumn,
        calculateScoreRecordLastRow(candidates.size()));
  }

  private String scoreRecordQuestionTitle(
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      int questionIndex
  ) {
    for (CertificationCandidateEntity candidate : candidates) {
      CertificationPracticalQuestionResultEntity questionResult =
          questionResultAt(questionResultsByCandidate.get(candidate.getId()), questionIndex);
      if (questionResult == null) {
        continue;
      }
      List<CertificationAssessorTaskEntity> scoreTasks = scoreTasksForCandidateQuestion(
          tasksByCandidateQuestion, candidate.getId(), questionResult);
      if (!scoreTasks.isEmpty() && StringUtils.hasText(scoreTasks.get(0).getTitle())) {
        return scoreTasks.get(0).getTitle();
      }
    }
    return "试题" + (questionIndex + 1);
  }

  private List<CertificationAssessorTaskScoreEntity> scoreRecordTemplateItems(
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask,
      int questionIndex
  ) {
    return candidates.stream()
        .map(candidate -> questionResultAt(questionResultsByCandidate.get(candidate.getId()), questionIndex))
        .filter(Objects::nonNull)
        .flatMap(questionResult -> tasksByCandidateQuestion.values().stream()
            .flatMap(List::stream)
            .filter(task -> Objects.equals(task.getPaperQuestionId(), questionResult.getPaperQuestionId())))
        .map(task -> scoresByAssessorTask.getOrDefault(task.getId(), List.of()))
        .max(Comparator.comparingInt(List::size))
        .orElse(List.of());
  }

  private List<CertificationPracticalQuestionResultEntity> completeExportQuestionResults(
      List<CertificationPracticalQuestionResultEntity> questionResults,
      List<CertificationAssessorTaskEntity> scoringTasks
  ) {
    Map<String, CertificationPracticalQuestionResultEntity> byCandidateQuestion = new LinkedHashMap<>();
    for (CertificationPracticalQuestionResultEntity result : questionResults) {
      byCandidateQuestion.put(result.getCandidateId() + ":" + result.getPaperQuestionId(), result);
    }
    for (CertificationAssessorTaskEntity task : scoringTasks) {
      if (task.getCandidateId() == null || task.getPaperQuestionId() == null) {
        continue;
      }
      String key = task.getCandidateId() + ":" + task.getPaperQuestionId();
      byCandidateQuestion.computeIfAbsent(key, ignored -> {
        CertificationPracticalQuestionResultEntity result = new CertificationPracticalQuestionResultEntity();
        result.setTaskId(task.getTaskId());
        result.setCandidateId(task.getCandidateId());
        result.setPaperId(task.getPaperId());
        result.setPaperQuestionId(task.getPaperQuestionId());
        result.setSubmittedCount(0);
        result.setStatus("pending");
        return result;
      });
    }
    return byCandidateQuestion.values().stream()
        .sorted(Comparator
            .comparing(CertificationPracticalQuestionResultEntity::getCandidateId)
            .thenComparing(CertificationPracticalQuestionResultEntity::getPaperQuestionId))
        .toList();
  }

  private BigDecimal exportAssessorTaskScore(CertificationAssessorTaskEntity task) {
    return task != null && "submitted".equals(task.getStatus()) && task.getTotalScore() != null
        ? task.getTotalScore()
        : BigDecimal.ZERO;
  }

  private BigDecimal exportScoreItemValue(
      CertificationAssessorTaskEntity task,
      BigDecimal value
  ) {
    return task != null && "submitted".equals(task.getStatus()) && value != null ? value : BigDecimal.ZERO;
  }

  private BigDecimal exportQuestionAverageScore(CertificationPracticalQuestionResultEntity result) {
    return result != null && "completed".equals(result.getStatus()) && result.getAverageScore() != null
        ? result.getAverageScore()
        : BigDecimal.ZERO;
  }

  private BigDecimal exportQuestionFinalScore(CertificationPracticalQuestionResultEntity result) {
    return exportQuestionAverageScore(result);
  }

  private List<CertificationCandidateEntity> candidatesForScoreRecordAssessor(
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      int questionIndex,
      ScoreRecordAssessor assessor
  ) {
    return candidates.stream()
        .filter(candidate -> {
          CertificationPracticalQuestionResultEntity questionResult =
              questionResultAt(questionResultsByCandidate.get(candidate.getId()), questionIndex);
          List<CertificationAssessorTaskEntity> scoreTasks = scoreTasksForCandidateQuestion(
              tasksByCandidateQuestion, candidate.getId(), questionResult);
          return scoreTaskForAssessor(scoreTasks, assessor) != null;
        })
        .toList();
  }

  private List<ScoreRecordAssessor> scoreRecordAssessors(
      List<CertificationTaskAssessorEntity> assessors,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion
  ) {
    List<ScoreRecordAssessor> configured = new ArrayList<>();
    for (int index = 0; index < assessors.size(); index++) {
      CertificationTaskAssessorEntity assessor = assessors.get(index);
      if (assessor.getAssessorId() != null) {
        configured.add(new ScoreRecordAssessor(
            assessor.getAssessorId(),
            StringUtils.hasText(assessor.getAssessorName())
                ? assessor.getAssessorName()
                : "考评员" + (index + 1),
            index + 1));
      }
    }
    if (!configured.isEmpty()) {
      return configured;
    }
    return tasksByCandidateQuestion.values().stream()
        .flatMap(List::stream)
        .filter(task -> task.getAssessorId() != null)
        .collect(Collectors.toMap(
            CertificationAssessorTaskEntity::getAssessorId,
            Function.identity(),
            (left, right) -> left,
            LinkedHashMap::new))
        .values()
        .stream()
        .map(task -> new ScoreRecordAssessor(task.getAssessorId(), "考评员" + task.getAssessorId(), 0))
        .toList();
  }

  private String scoreRecordSheetName(int questionNo, int assessorNo, String assessorName) {
    String safeName = nvlText(assessorName).replaceAll("[\\\\/?*\\[\\]:]", "");
    if (!StringUtils.hasText(safeName)) {
      safeName = "考评员" + assessorNo;
    }
    String base = "评分记录表" + questionNo + "-" + safeName;
    return base.length() <= 31 ? base : base.substring(0, 31);
  }

  private CertificationAssessorTaskEntity scoreTaskForAssessor(
      List<CertificationAssessorTaskEntity> scoreTasks,
      ScoreRecordAssessor assessor
  ) {
    if (scoreTasks == null || scoreTasks.isEmpty()) {
      return null;
    }
    return scoreTasks.stream()
        .filter(task -> Objects.equals(task.getAssessorId(), assessor.assessorId()))
        .findFirst()
        .orElse(null);
  }

  private Sheet chooseScoreRecordTemplateSheet(Workbook workbook, int questionIndex, int itemCount) {
    if (workbook.getNumberOfSheets() == 0) {
      throw new IllegalStateException("实操评分记录表模板不包含工作表");
    }
    Sheet best = null;
    int bestCapacity = Integer.MAX_VALUE;
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      int capacity = scoreRecordItemCapacity(sheet);
      if (capacity >= itemCount && capacity < bestCapacity) {
        best = sheet;
        bestCapacity = capacity;
      }
    }
    if (best != null) {
      return best;
    }
    bestCapacity = Integer.MAX_VALUE;
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      int capacity = expandableScoreRecordItemCapacity(sheet);
      if (capacity >= itemCount && capacity < bestCapacity) {
        best = sheet;
        bestCapacity = capacity;
      }
    }
    if (best != null) {
      return best;
    }
    return workbook.getSheetAt(Math.min(questionIndex, workbook.getNumberOfSheets() - 1));
  }

  private int scoreRecordItemCapacity(Sheet sheet) {
    int totalColumn = findScoreRecordTotalColumn(sheet);
    return Math.max(0, (totalColumn - SCORE_RECORD_SCORE_START_COLUMN) / 2);
  }

  private int expandableScoreRecordItemCapacity(Sheet sheet) {
    return Math.max(scoreRecordItemCapacity(sheet),
        Math.max(0, (maxTemplateColumn(sheet) - SCORE_RECORD_SCORE_START_COLUMN) / 2));
  }

  private int calculateScoreRecordTotalColumn(int standardCount) {
    return SCORE_RECORD_SCORE_START_COLUMN + Math.max(0, standardCount) * 2;
  }

  private int calculateScoreRecordScoreEndColumn(int standardCount) {
    return calculateScoreRecordTotalColumn(standardCount) - 1;
  }

  private int calculateScoreRecordLastRow(int candidateCount) {
    return SCORE_RECORD_CANDIDATE_START_ROW + Math.max(8, candidateCount) - 1;
  }

  private Sheet copyTemplateSheet(Workbook targetWorkbook, Sheet sourceSheet, String targetName) {
    Sheet target = targetWorkbook.createSheet(uniqueSheetName(targetWorkbook, targetName));
    int maxColumn = maxTemplateColumn(sourceSheet);
    for (int column = 0; column <= maxColumn; column++) {
      target.setColumnWidth(column, sourceSheet.getColumnWidth(column));
      target.setColumnHidden(column, sourceSheet.isColumnHidden(column));
    }
    Map<Short, CellStyle> styleMap = new HashMap<>();
    for (int rowIndex = 0; rowIndex <= sourceSheet.getLastRowNum(); rowIndex++) {
      Row sourceRow = sourceSheet.getRow(rowIndex);
      if (sourceRow == null) {
        continue;
      }
      Row targetRow = target.createRow(rowIndex);
      targetRow.setHeight(sourceRow.getHeight());
      for (int column = 0; column < sourceRow.getLastCellNum(); column++) {
        Cell sourceCell = sourceRow.getCell(column);
        if (sourceCell == null) {
          continue;
        }
        Cell targetCell = targetRow.createCell(column);
        copyCell(sourceCell, targetCell, targetWorkbook, styleMap);
      }
    }
    for (int i = 0; i < sourceSheet.getNumMergedRegions(); i++) {
      CellRangeAddress range = sourceSheet.getMergedRegion(i);
      target.addMergedRegion(new CellRangeAddress(
          range.getFirstRow(), range.getLastRow(), range.getFirstColumn(), range.getLastColumn()));
    }
    target.setDisplayGridlines(sourceSheet.isDisplayGridlines());
    target.setPrintGridlines(sourceSheet.isPrintGridlines());
    target.setMargin(Sheet.LeftMargin, sourceSheet.getMargin(Sheet.LeftMargin));
    target.setMargin(Sheet.RightMargin, sourceSheet.getMargin(Sheet.RightMargin));
    target.setMargin(Sheet.TopMargin, sourceSheet.getMargin(Sheet.TopMargin));
    target.setMargin(Sheet.BottomMargin, sourceSheet.getMargin(Sheet.BottomMargin));
    target.getPrintSetup().setLandscape(sourceSheet.getPrintSetup().getLandscape());
    target.getPrintSetup().setScale(sourceSheet.getPrintSetup().getScale());
    return target;
  }

  private int maxTemplateColumn(Sheet sheet) {
    int max = 0;
    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row != null && row.getLastCellNum() > 0) {
        max = Math.max(max, row.getLastCellNum() - 1);
      }
    }
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      max = Math.max(max, sheet.getMergedRegion(i).getLastColumn());
    }
    return max;
  }

  private void copyCell(
      Cell source,
      Cell target,
      Workbook targetWorkbook,
      Map<Short, CellStyle> styleMap
  ) {
    if (source.getCellStyle() != null) {
      short sourceStyleIndex = source.getCellStyle().getIndex();
      CellStyle targetStyle = styleMap.computeIfAbsent(sourceStyleIndex, ignored -> {
        CellStyle style = targetWorkbook.createCellStyle();
        style.cloneStyleFrom(source.getCellStyle());
        return style;
      });
      target.setCellStyle(targetStyle);
    }
    switch (source.getCellType()) {
      case STRING -> target.setCellValue(source.getStringCellValue());
      case NUMERIC -> target.setCellValue(source.getNumericCellValue());
      case BOOLEAN -> target.setCellValue(source.getBooleanCellValue());
      case FORMULA -> target.setCellFormula(source.getCellFormula());
      case ERROR -> target.setCellErrorValue(source.getErrorCellValue());
      case BLANK, _NONE -> target.setBlank();
    }
  }

  private String uniqueSheetName(Workbook workbook, String baseName) {
    String name = baseName;
    int suffix = 2;
    while (workbook.getSheet(name) != null) {
      name = baseName + "_" + suffix++;
    }
    return name;
  }

  private int findScoreRecordTotalColumn(Sheet sheet) {
    Row row = sheet.getRow(3);
    if (row == null) {
      return Math.max(4, maxTemplateColumn(sheet));
    }
    DataFormatter formatter = new DataFormatter();
    for (int column = 0; column < row.getLastCellNum(); column++) {
      Cell cell = row.getCell(column);
      if (cell != null && formatter.formatCellValue(cell).contains("本题得分合计")) {
        return column;
      }
    }
    return Math.max(4, maxTemplateColumn(sheet));
  }

  private void prepareScoreRecordSheetLayout(
      Sheet sheet,
      int standardCount,
      int candidateCount,
      int totalColumn
  ) {
    int templateTotalColumn = findScoreRecordTotalColumn(sheet);
    ensureScoreRecordCapacity(sheet, templateTotalColumn, standardCount);
    copyScoreRecordTotalColumnStyle(sheet, templateTotalColumn, totalColumn);
    clearScoreRecordUnusedColumns(sheet, totalColumn, Math.max(maxTemplateColumn(sheet), templateTotalColumn));
    rebuildScoreRecordHeaderMerges(sheet, standardCount, totalColumn);
    applyScoreRecordColumnWidths(sheet, standardCount, totalColumn);
    applyScoreRecordBaseRowHeights(sheet, candidateCount);
    applyScoreRecordCellStyleDefaults(sheet, totalColumn, calculateScoreRecordLastRow(candidateCount));
    writeTemplateCell(sheet, 3, totalColumn, "本题得分合计");
  }

  private void copyScoreRecordTotalColumnStyle(Sheet sheet, int sourceColumn, int targetColumn) {
    if (sourceColumn == targetColumn) {
      return;
    }
    Workbook workbook = sheet.getWorkbook();
    Map<Short, CellStyle> styleMap = new HashMap<>();
    sheet.setColumnWidth(targetColumn, sheet.getColumnWidth(sourceColumn));
    sheet.setColumnHidden(targetColumn, false);
    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      Cell source = row.getCell(sourceColumn);
      Cell target = row.getCell(targetColumn);
      if (target == null) {
        target = row.createCell(targetColumn);
      }
      if (source != null) {
        copyCellStyle(source, target, workbook, styleMap);
      }
      if (rowIndex >= 3) {
        target.setBlank();
      }
    }
  }

  private void clearScoreRecordUnusedColumns(Sheet sheet, int totalColumn, int maxColumn) {
    for (int column = totalColumn + 1; column <= maxColumn; column++) {
      sheet.setColumnHidden(column, true);
      for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        Cell cell = row.getCell(column);
        if (cell != null) {
          cell.setBlank();
        }
      }
    }
  }

  private void rebuildScoreRecordHeaderMerges(Sheet sheet, int standardCount, int totalColumn) {
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress range = sheet.getMergedRegion(i);
      if (range.getFirstRow() <= 10 && range.getLastRow() >= 0) {
        sheet.removeMergedRegion(i);
      }
    }
    int scoreEndColumn = calculateScoreRecordScoreEndColumn(standardCount);
    mergeSafely(sheet, 0, 0, 0, totalColumn);
    mergeSafely(sheet, 1, 1, 0, totalColumn);
    mergeSafely(sheet, 2, 2, 0, totalColumn);
    mergeSafely(sheet, 3, 3, 3, Math.max(3, scoreEndColumn));
    if (scoreEndColumn >= SCORE_RECORD_SCORE_START_COLUMN) {
      mergeSafely(sheet, 4, 4, SCORE_RECORD_SCORE_START_COLUMN, scoreEndColumn);
    }
    mergeSafely(sheet, 3, 10, 0, 0);
    mergeSafely(sheet, 3, 10, 1, 1);
    mergeSafely(sheet, 3, 10, 2, 2);
    mergeSafely(sheet, 3, 9, totalColumn, totalColumn);
  }

  private void applyScoreRecordColumnWidths(Sheet sheet, int standardCount, int totalColumn) {
    sheet.setColumnWidth(0, 6 * 256);
    sheet.setColumnWidth(1, 10 * 256);
    sheet.setColumnWidth(2, 16 * 256);
    sheet.setColumnWidth(3, 12 * 256);
    int scoreColumnWidth = scoreRecordScoreColumnWidth(standardCount) * 256;
    for (int column = SCORE_RECORD_SCORE_START_COLUMN; column < totalColumn; column++) {
      sheet.setColumnWidth(column, scoreColumnWidth);
      sheet.setColumnHidden(column, false);
    }
    sheet.setColumnWidth(totalColumn, 8 * 256);
    sheet.setColumnHidden(totalColumn, false);
  }

  private int scoreRecordScoreColumnWidth(int standardCount) {
    if (standardCount <= 5) {
      return 7;
    }
    if (standardCount <= 7) {
      return 6;
    }
    return 5;
  }

  private void applyScoreRecordBaseRowHeights(Sheet sheet, int candidateCount) {
    setScoreRecordRowHeight(sheet, 4, 26F);
    setScoreRecordRowHeight(sheet, 5, 90F);
    setScoreRecordRowHeight(sheet, 6, 20F);
    setScoreRecordRowHeight(sheet, 7, 110F);
    setScoreRecordRowHeight(sheet, 8, 18F);
    setScoreRecordRowHeight(sheet, 9, 18F);
    setScoreRecordRowHeight(sheet, 10, 20F);
    int lastRow = calculateScoreRecordLastRow(candidateCount);
    for (int rowIndex = SCORE_RECORD_CANDIDATE_START_ROW; rowIndex <= lastRow; rowIndex++) {
      setScoreRecordRowHeight(sheet, rowIndex, 24F);
    }
  }

  private void setScoreRecordRowHeight(Sheet sheet, int rowIndex, float heightPoints) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }
    row.setHeightInPoints(heightPoints);
  }

  private void applyScoreRecordCellStyleDefaults(Sheet sheet, int totalColumn, int lastRow) {
    Workbook workbook = sheet.getWorkbook();
    Map<Short, CellStyle> styleMap = new HashMap<>();
    for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      for (int column = 0; column <= totalColumn; column++) {
        Cell cell = row.getCell(column);
        if (cell == null) {
          cell = row.createCell(column);
        }
        applyScoreRecordCellStyleDefaults(cell, workbook, styleMap);
      }
    }
  }

  private void applyScoreRecordCellStyleDefaults(
      Cell cell,
      Workbook workbook,
      Map<Short, CellStyle> styleMap
  ) {
    if (cell.getCellStyle() == null) {
      return;
    }
    short sourceStyleIndex = cell.getCellStyle().getIndex();
    CellStyle style = styleMap.computeIfAbsent(sourceStyleIndex, ignored -> {
      CellStyle cloned = workbook.createCellStyle();
      cloned.cloneStyleFrom(cell.getCellStyle());
      cloned.setAlignment(HorizontalAlignment.CENTER);
      cloned.setVerticalAlignment(VerticalAlignment.CENTER);
      cloned.setWrapText(true);
      return cloned;
    });
    cell.setCellStyle(style);
  }

  private void applyScoreRecordCandidateRowHeight(Sheet sheet, int rowIndex, int totalColumn) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return;
    }
    DataFormatter formatter = new DataFormatter();
    double maxLength = 0D;
    for (int column = 1; column <= Math.min(2, totalColumn); column++) {
      Cell cell = row.getCell(column);
      if (cell != null) {
        maxLength = Math.max(maxLength, weightedTextLength(formatter.formatCellValue(cell)));
      }
    }
    if (maxLength > 30D) {
      row.setHeightInPoints(36F);
    } else if (maxLength > 18D) {
      row.setHeightInPoints(30F);
    } else if (row.getHeightInPoints() < 24F) {
      row.setHeightInPoints(24F);
    }
  }

  private void applyScoreRecordPrintSettings(Sheet sheet, int totalColumn, int lastRow) {
    Workbook workbook = sheet.getWorkbook();
    PrintSetup printSetup = sheet.getPrintSetup();
    printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
    printSetup.setLandscape(true);
    printSetup.setFitWidth((short) 1);
    printSetup.setFitHeight((short) 0);
    sheet.setFitToPage(true);
    sheet.setAutobreaks(true);
    sheet.setHorizontallyCenter(true);
    sheet.setMargin(Sheet.LeftMargin, 0.25D);
    sheet.setMargin(Sheet.RightMargin, 0.25D);
    sheet.setMargin(Sheet.TopMargin, 0.5D);
    sheet.setMargin(Sheet.BottomMargin, 0.5D);
    int sheetIndex = workbook.getSheetIndex(sheet);
    if (sheetIndex >= 0) {
      workbook.setPrintArea(sheetIndex, 0, totalColumn, 0, lastRow);
    }
  }

  private int ensureScoreRecordCapacity(Sheet sheet, int currentTotalColumn, int itemCount) {
    int requiredTotalColumn = calculateScoreRecordTotalColumn(itemCount);
    if (requiredTotalColumn <= currentTotalColumn) {
      return currentTotalColumn;
    }
    Workbook workbook = sheet.getWorkbook();
    Map<Short, CellStyle> styleMap = new HashMap<>();
    sheet.setColumnWidth(requiredTotalColumn, sheet.getColumnWidth(currentTotalColumn));
    sheet.setColumnHidden(requiredTotalColumn, sheet.isColumnHidden(currentTotalColumn));
    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      Cell source = row.getCell(currentTotalColumn);
      Cell target = row.getCell(requiredTotalColumn);
      if (target == null) {
        target = row.createCell(requiredTotalColumn);
      }
      if (source != null) {
        copyCell(source, target, workbook, styleMap);
        if (rowIndex >= 3) {
          source.setBlank();
        }
      }
    }
    copyExpandedScoreRecordItemColumnStyles(sheet, currentTotalColumn, requiredTotalColumn, styleMap);
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress range = sheet.getMergedRegion(i);
      boolean headerRows = range.getFirstRow() <= 9 && range.getLastRow() >= 3;
      boolean oldOrNewTotalColumn = range.isInRange(3, currentTotalColumn)
          || range.isInRange(3, requiredTotalColumn);
      if (headerRows && oldOrNewTotalColumn) {
        sheet.removeMergedRegion(i);
      }
    }
    sheet.addMergedRegion(new CellRangeAddress(3, 9, requiredTotalColumn, requiredTotalColumn));
    return requiredTotalColumn;
  }

  private void copyExpandedScoreRecordItemColumnStyles(
      Sheet sheet,
      int firstExpandedColumn,
      int totalColumn,
      Map<Short, CellStyle> styleMap
  ) {
    Workbook workbook = sheet.getWorkbook();
    for (int column = firstExpandedColumn; column < totalColumn; column++) {
      int sourceColumn = scoreRecordItemStyleSourceColumn(column, firstExpandedColumn);
      sheet.setColumnWidth(column, sheet.getColumnWidth(sourceColumn));
      sheet.setColumnHidden(column, sheet.isColumnHidden(sourceColumn));
      for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        Cell source = row.getCell(sourceColumn);
        Cell target = row.getCell(column);
        if (target == null) {
          target = row.createCell(column);
        }
        if (source != null) {
          copyCellStyle(source, target, workbook, styleMap);
        }
        target.setBlank();
      }
    }
  }

  private int scoreRecordItemStyleSourceColumn(int targetColumn, int firstExpandedColumn) {
    int pairOffset = Math.floorMod(targetColumn - SCORE_RECORD_SCORE_START_COLUMN, 2);
    int lastTemplatePairColumn = firstExpandedColumn - 2 + pairOffset;
    if (lastTemplatePairColumn >= SCORE_RECORD_SCORE_START_COLUMN) {
      return lastTemplatePairColumn;
    }
    return SCORE_RECORD_SCORE_START_COLUMN + pairOffset;
  }

  private void copyCellStyle(
      Cell source,
      Cell target,
      Workbook targetWorkbook,
      Map<Short, CellStyle> styleMap
  ) {
    if (source.getCellStyle() == null) {
      return;
    }
    short sourceStyleIndex = source.getCellStyle().getIndex();
    CellStyle targetStyle = styleMap.computeIfAbsent(sourceStyleIndex, ignored -> {
      CellStyle style = targetWorkbook.createCellStyle();
      style.cloneStyleFrom(source.getCellStyle());
      return style;
    });
    target.setCellStyle(targetStyle);
  }

  private void clearScoreRecordTemplateRows(Sheet sheet, int candidateCount, int totalColumn) {
    int rows = Math.max(8, candidateCount);
    for (int i = 0; i < rows; i++) {
      int rowIndex = SCORE_RECORD_CANDIDATE_START_ROW + i;
      ensureScoreRecordCandidateRow(sheet, rowIndex, totalColumn);
      Row row = sheet.getRow(rowIndex);
      for (int column = 1; column <= totalColumn; column++) {
        Cell cell = row.getCell(column);
        if (cell == null) {
          cell = row.createCell(column);
        }
        cell.setBlank();
      }
    }
  }

  private void ensureScoreRecordCandidateRow(Sheet sheet, int rowIndex, int totalColumn) {
    Row row = sheet.getRow(rowIndex);
    if (row != null) {
      return;
    }
    Row templateRow = sheet.getRow(SCORE_RECORD_TEMPLATE_CANDIDATE_ROW);
    row = sheet.createRow(rowIndex);
    if (templateRow != null) {
      row.setHeight(templateRow.getHeight());
      for (int column = 0; column <= totalColumn; column++) {
        Cell source = templateRow.getCell(column);
        if (source == null) {
          continue;
        }
        Cell target = row.createCell(column);
        target.setCellStyle(source.getCellStyle());
      }
    }
  }

  private List<CertificationAssessorTaskScoreEntity> firstScoreItems(
      List<CertificationAssessorTaskEntity> scoreTasks,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask
  ) {
    return scoreTasks.stream()
        .map(task -> scoresByAssessorTask.getOrDefault(task.getId(), List.of()))
        .filter(items -> !items.isEmpty())
        .findFirst()
        .orElse(List.of());
  }

  private void writeScoreRecordTemplateItems(
      Sheet sheet,
      List<CertificationAssessorTaskScoreEntity> items,
      int totalColumn
  ) {
    int capacity = Math.max(0, (totalColumn - SCORE_RECORD_SCORE_START_COLUMN) / 2);
    clearScoreRecordItemMerges(sheet, totalColumn);
    Map<Short, CellStyle> wrapStyles = new HashMap<>();
    for (int itemIndex = 0; itemIndex < capacity; itemIndex++) {
      int column = SCORE_RECORD_SCORE_START_COLUMN + itemIndex * 2;
      CertificationAssessorTaskScoreEntity item = itemIndex < items.size() ? items.get(itemIndex) : null;
      writeTemplateCell(sheet, 5, column, "");
      writeTemplateCell(sheet, 6, column, "");
      writeTemplateCell(sheet, 7, column, item == null ? "" : nvlText(item.getCriterion()));
      writeTemplateCell(sheet, 8, column, item == null ? "" : item.getMaxDeduction());
      writeTemplateCell(sheet, 9, column, item == null ? "" : item.getDeductionStep());
      writeTemplateCell(sheet, 10, column, item == null ? "" : "扣分");
      writeTemplateCell(sheet, 10, column + 1, item == null ? "" : "得分");
      applyWrapText(sheet, 7, column, wrapStyles);
      clearTemplateCell(sheet, 5, column + 1);
      clearTemplateCell(sheet, 6, column + 1);
      clearTemplateCell(sheet, 7, column + 1);
      clearTemplateCell(sheet, 8, column + 1);
      clearTemplateCell(sheet, 9, column + 1);
      if (item != null) {
        mergeScoreRecordItemPair(sheet, 7, column);
        mergeScoreRecordItemPair(sheet, 8, column);
        mergeScoreRecordItemPair(sheet, 9, column);
      }
    }
    int groupStart = 0;
    int itemLimit = Math.min(items.size(), capacity);
    while (groupStart < itemLimit) {
      CertificationAssessorTaskScoreEntity first = items.get(groupStart);
      String stepName = nvlText(first.getStepName());
      int groupEnd = groupStart;
      BigDecimal groupScore = BigDecimal.ZERO;
      while (groupEnd < itemLimit
          && Objects.equals(stepName, nvlText(items.get(groupEnd).getStepName()))) {
        BigDecimal score = items.get(groupEnd).getMaxScore();
        groupScore = groupScore.add(score == null ? BigDecimal.ZERO : score);
        groupEnd++;
      }
      int startColumn = SCORE_RECORD_SCORE_START_COLUMN + groupStart * 2;
      int endColumn = SCORE_RECORD_SCORE_START_COLUMN + (groupEnd - 1) * 2 + 1;
      writeTemplateCell(sheet, 5, startColumn, stepName);
      writeTemplateCell(sheet, 6, startColumn, groupScore);
      applyWrapText(sheet, 5, startColumn, wrapStyles);
      mergeScoreRecordItemRange(sheet, 5, startColumn, endColumn);
      mergeScoreRecordItemRange(sheet, 6, startColumn, endColumn);
      groupStart = groupEnd;
    }
    adjustScoreRecordHeaderRowHeight(sheet, 5, totalColumn, 120F);
    adjustScoreRecordHeaderRowHeight(sheet, 7, totalColumn, 160F);
    keepScoreRecordHeaderRowMinimumHeight(sheet, 6);
    keepScoreRecordHeaderRowMinimumHeight(sheet, 8);
    keepScoreRecordHeaderRowMinimumHeight(sheet, 9);
  }

  private void applyWrapText(
      Sheet sheet,
      int rowIndex,
      int column,
      Map<Short, CellStyle> wrapStyles
  ) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return;
    }
    Cell cell = row.getCell(column);
    if (cell == null || cell.getCellStyle() == null) {
      return;
    }
    short styleIndex = cell.getCellStyle().getIndex();
    CellStyle wrapStyle = wrapStyles.computeIfAbsent(styleIndex, ignored -> {
      CellStyle style = sheet.getWorkbook().createCellStyle();
      style.cloneStyleFrom(cell.getCellStyle());
      style.setWrapText(true);
      return style;
    });
    cell.setCellStyle(wrapStyle);
  }

  private void adjustScoreRecordHeaderRowHeight(
      Sheet sheet,
      int rowIndex,
      int totalColumn,
      float maxHeightPoints
  ) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return;
    }
    float baseHeight = row.getHeightInPoints() > 0
        ? row.getHeightInPoints()
        : sheet.getDefaultRowHeightInPoints();
    int maxLines = 1;
    DataFormatter formatter = new DataFormatter();
    for (int column = SCORE_RECORD_SCORE_START_COLUMN; column < totalColumn; column++) {
      CellRangeAddress mergedRange = mergedRangeAt(sheet, rowIndex, column);
      if (mergedRange != null && mergedRange.getFirstColumn() != column) {
        continue;
      }
      Cell cell = row.getCell(column);
      if (cell == null) {
        continue;
      }
      String text = formatter.formatCellValue(cell).trim();
      if (!StringUtils.hasText(text)) {
        continue;
      }
      int firstColumn = mergedRange == null ? column : mergedRange.getFirstColumn();
      int lastColumn = mergedRange == null ? column : Math.min(mergedRange.getLastColumn(), totalColumn - 1);
      double charsPerLine = Math.max(6D, mergedColumnWidthChars(sheet, firstColumn, lastColumn) * 0.85D);
      int lines = Math.max(1, (int) Math.ceil(weightedTextLength(text) / charsPerLine));
      maxLines = Math.max(maxLines, lines);
    }
    float targetHeight = Math.min(maxHeightPoints, Math.max(baseHeight, baseHeight * maxLines));
    row.setHeightInPoints(targetHeight);
  }

  private void keepScoreRecordHeaderRowMinimumHeight(Sheet sheet, int rowIndex) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return;
    }
    float baseHeight = sheet.getDefaultRowHeightInPoints();
    if (row.getHeightInPoints() < baseHeight) {
      row.setHeightInPoints(baseHeight);
    }
  }

  private CellRangeAddress mergedRangeAt(Sheet sheet, int rowIndex, int column) {
    for (CellRangeAddress range : sheet.getMergedRegions()) {
      if (range.isInRange(rowIndex, column)) {
        return range;
      }
    }
    return null;
  }

  private double mergedColumnWidthChars(Sheet sheet, int firstColumn, int lastColumn) {
    double chars = 0D;
    for (int column = firstColumn; column <= lastColumn; column++) {
      chars += sheet.getColumnWidth(column) / 256D;
    }
    return chars;
  }

  private double weightedTextLength(String text) {
    double length = 0D;
    for (int offset = 0; offset < text.length(); ) {
      int codePoint = text.codePointAt(offset);
      if (codePoint == '\n' || codePoint == '\r') {
        length += 8D;
      } else if (codePoint < 128) {
        length += 1D;
      } else {
        length += 1.8D;
      }
      offset += Character.charCount(codePoint);
    }
    return length;
  }

  private void clearScoreRecordItemMerges(Sheet sheet, int totalColumn) {
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress range = sheet.getMergedRegion(i);
      boolean rowIntersects = range.getFirstRow() <= 9 && range.getLastRow() >= 5;
      boolean columnIntersects = range.getFirstColumn() < totalColumn
          && range.getLastColumn() >= SCORE_RECORD_SCORE_START_COLUMN;
      if (rowIntersects && columnIntersects) {
        sheet.removeMergedRegion(i);
      }
    }
  }

  private void mergeScoreRecordItemPair(Sheet sheet, int rowIndex, int column) {
    mergeScoreRecordItemRange(sheet, rowIndex, column, column + 1);
  }

  private void mergeScoreRecordItemRange(Sheet sheet, int rowIndex, int firstColumn, int lastColumn) {
    mergeSafely(sheet, rowIndex, rowIndex, firstColumn, lastColumn);
  }

  private void mergeSafely(
      Sheet sheet,
      int firstRow,
      int lastRow,
      int firstColumn,
      int lastColumn
  ) {
    if (lastRow < firstRow || lastColumn < firstColumn) {
      return;
    }
    if (lastRow == firstRow && lastColumn == firstColumn) {
      return;
    }
    CellRangeAddress target = new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress existing = sheet.getMergedRegion(i);
      if (existing.formatAsString().equals(target.formatAsString())) {
        return;
      }
      if (existing.intersects(target)) {
        sheet.removeMergedRegion(i);
      }
    }
    sheet.addMergedRegion(target);
  }

  private void clearTemplateCell(Sheet sheet, int rowIndex, int column) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }
    Cell cell = row.getCell(column);
    if (cell == null) {
      cell = row.createCell(column);
    }
    cell.setBlank();
  }

  private void applyDecimalCellStyle(Cell cell) {
    if (cell == null) {
      return;
    }
    Workbook workbook = cell.getSheet().getWorkbook();
    CellStyle style = workbook.createCellStyle();
    if (cell.getCellStyle() != null) {
      style.cloneStyleFrom(cell.getCellStyle());
    }
    style.setDataFormat(workbook.createDataFormat().getFormat(DECIMAL_SCORE_FORMAT));
    cell.setCellStyle(style);
  }

  private String scoreRecordQuestionScore(
      List<CertificationCandidateEntity> candidates,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Map<Long, List<CertificationAssessorTaskScoreEntity>> scoresByAssessorTask,
      int questionIndex
  ) {
    for (CertificationCandidateEntity candidate : candidates) {
      CertificationPracticalQuestionResultEntity questionResult =
          questionResultAt(questionResultsByCandidate.get(candidate.getId()), questionIndex);
      List<CertificationAssessorTaskEntity> scoreTasks = scoreTasksForCandidateQuestion(
          tasksByCandidateQuestion, candidate.getId(), questionResult);
      List<CertificationAssessorTaskScoreEntity> items = firstScoreItems(scoreTasks, scoresByAssessorTask);
      if (!items.isEmpty()) {
        BigDecimal score = items.stream()
            .map(CertificationAssessorTaskScoreEntity::getMaxScore)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return score.stripTrailingZeros().toPlainString();
      }
    }
    return "0";
  }

  private CertificationPracticalQuestionResultEntity questionResultAt(
      List<CertificationPracticalQuestionResultEntity> results,
      int index
  ) {
    return results == null || index < 0 || index >= results.size() ? null : results.get(index);
  }

  private List<CertificationAssessorTaskEntity> scoreTasksForCandidateQuestion(
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      Long candidateId,
      CertificationPracticalQuestionResultEntity questionResult
  ) {
    if (candidateId == null || questionResult == null || questionResult.getPaperQuestionId() == null) {
      return List.of();
    }
    return tasksByCandidateQuestion.getOrDefault(
        candidateId + ":" + questionResult.getPaperQuestionId(), List.of());
  }

  private String assessorName(
      CertificationAssessorTaskEntity task,
      Map<Long, String> assessorNames
  ) {
    String name = assessorNames.get(task.getAssessorId());
    return StringUtils.hasText(name) ? name : "考评员" + nvlText(task.getAssessorId());
  }

  private String formatChineseDate(LocalDate date) {
    return date == null ? "" : date.getYear() + "年" + date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
  }

  private PracticalScoringModels.AssessorTaskDetail saveScore(
      Long assessorTaskId,
      PracticalScoringModels.SaveScoreReq req,
      boolean submit
  ) {
    CertificationAssessorTaskEntity task = requireOwnedTask(assessorTaskId, true);
    startService.requireStarted(task.getTaskId(), task.getCandidateId());
    if (!EDITABLE_STATUSES.contains(task.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分任务已提交，不能直接修改");
    }
    int currentVersion = nvl(task.getVersionNo(), 0);
    if (req.version() != currentVersion) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分任务已在其他设备更新，请重新加载");
    }
    List<CertificationAssessorTaskScoreEntity> scores = scores(task.getId());
    ScoreInputIndex requestItems = scoreInputIndex(req.items());
    if (scores.size() != requestItems.size()
        || scores.stream().anyMatch(score -> scoreInput(score, requestItems) == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素必须完整提交");
    }
    BigDecimal total = applyScoreInputs(scores, requestItems);
    task.setProgressNote(req.note());
    task.setTotalScore(total);
    task.setVersionNo(currentVersion + 1);
    task.setStatus(submit ? "submitted" : "draft");
    task.setSubmittedAt(submit ? LocalDateTime.now() : null);
    task.setReturnedAt(null);
    task.setReturnedBy(null);
    task.setReturnReason(null);
    assessorTaskMapper.updateById(task);
    writeHistory(task, submit ? "submit" : "draft", null, SecurityContext.currentUser().getUserId());
    if (submit) {
      calculateResults(task);
    }
    return detail(task);
  }

  private void calculateResults(CertificationAssessorTaskEntity task) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationPracticalQuestionResultEntity result = questionResultMapper.lockResult(
        tenantId, task.getTaskId(), task.getCandidateId(), task.getPaperQuestionId());
    if (result == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试题成绩汇总记录不存在");
    }
    List<CertificationAssessorTaskEntity> submitted = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, task.getTaskId())
            .eq(CertificationAssessorTaskEntity::getCandidateId, task.getCandidateId())
            .eq(CertificationAssessorTaskEntity::getPaperQuestionId, task.getPaperQuestionId())
            .eq(CertificationAssessorTaskEntity::getStatus, "submitted"));
    result.setSubmittedCount(submitted.size());
    if (submitted.size() == 3) {
      BigDecimal minScore = submitted.stream()
          .map(CertificationAssessorTaskEntity::getTotalScore)
          .filter(Objects::nonNull)
          .min(BigDecimal::compareTo)
          .orElse(BigDecimal.ZERO);
      BigDecimal maxScore = submitted.stream()
          .map(CertificationAssessorTaskEntity::getTotalScore)
          .filter(Objects::nonNull)
          .max(BigDecimal::compareTo)
          .orElse(BigDecimal.ZERO);
      if (maxScore.subtract(minScore).compareTo(MAX_ASSESSOR_SCORE_DEVIATION) > 0) {
        result.setAverageScore(null);
        result.setFinalScore(null);
        result.setStatus("review_required");
        result.setCalculatedAt(LocalDateTime.now());
        questionResultMapper.updateById(result);
        calculateCandidateResult(task.getTaskId(), task.getCandidateId());
        return;
      }
      BigDecimal sum = submitted.stream()
          .map(CertificationAssessorTaskEntity::getTotalScore)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal average = sum.divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
      result.setAverageScore(average);
      result.setFinalScore(average);
      result.setStatus("completed");
      result.setCalculatedAt(LocalDateTime.now());
    } else {
      result.setAverageScore(null);
      result.setFinalScore(null);
      result.setStatus("pending");
      result.setCalculatedAt(null);
    }
    questionResultMapper.updateById(result);
    calculateCandidateResult(task.getTaskId(), task.getCandidateId());
  }

  private void calculateCandidateResult(Long taskId, Long candidateId) {
    List<CertificationPracticalQuestionResultEntity> results = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId)
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidateId));
    CertificationPracticalCandidateResultEntity candidateResult = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidateId)
            .last("LIMIT 1"));
    if (candidateResult == null) {
      return;
    }
    int completed = (int) results.stream().filter(r -> "completed".equals(r.getStatus())).count();
    boolean reviewRequired = results.stream().anyMatch(r -> "review_required".equals(r.getStatus()));
    candidateResult.setQuestionCount(results.size());
    candidateResult.setCompletedQuestionCount(completed);
    if (!results.isEmpty() && completed == results.size()) {
      candidateResult.setTotalScore(results.stream()
          .map(CertificationPracticalQuestionResultEntity::getAverageScore)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .setScale(1, RoundingMode.HALF_UP));
      candidateResult.setStatus("completed");
      candidateResult.setCalculatedAt(LocalDateTime.now());
    } else if (reviewRequired) {
      candidateResult.setTotalScore(null);
      candidateResult.setStatus("review_required");
      candidateResult.setCalculatedAt(LocalDateTime.now());
    } else {
      candidateResult.setTotalScore(null);
      candidateResult.setStatus("pending");
      candidateResult.setCalculatedAt(null);
    }
    candidateResultMapper.updateById(candidateResult);
  }

  private BigDecimal resolveCandidateTotalScore(
      Long candidateId,
      CertificationPracticalCandidateResultEntity result,
      List<CertificationPracticalQuestionResultEntity> questionResults
  ) {
    if (result == null) {
      return null;
    }
    if ("absent".equals(result.getStatus())) {
      return result.getTotalScore();
    }
    if (!"completed".equals(result.getStatus())) {
      return null;
    }
    List<CertificationPracticalQuestionResultEntity> candidateQuestions = questionResults.stream()
        .filter(questionResult -> candidateId.equals(questionResult.getCandidateId()))
        .toList();
    if (candidateQuestions.isEmpty()) {
      return null;
    }
    return candidateQuestions.stream()
        .map(CertificationPracticalQuestionResultEntity::getAverageScore)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(1, RoundingMode.HALF_UP);
  }

  private void invalidateResults(CertificationAssessorTaskEntity task) {
    CertificationPracticalQuestionResultEntity result = questionResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getTaskId())
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, task.getCandidateId())
            .eq(CertificationPracticalQuestionResultEntity::getPaperQuestionId, task.getPaperQuestionId())
            .last("LIMIT 1"));
    if (result != null) {
      int submittedCount = assessorTaskMapper.selectList(
          new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
              .eq(CertificationAssessorTaskEntity::getTaskId, task.getTaskId())
              .eq(CertificationAssessorTaskEntity::getCandidateId, task.getCandidateId())
              .eq(CertificationAssessorTaskEntity::getPaperQuestionId, task.getPaperQuestionId())
              .eq(CertificationAssessorTaskEntity::getStatus, "submitted")).size();
      result.setSubmittedCount(submittedCount);
      result.setAverageScore(null);
      result.setFinalScore(null);
      result.setStatus("pending");
      result.setCalculatedAt(null);
      questionResultMapper.updateById(result);
    }
    calculateCandidateResult(task.getTaskId(), task.getCandidateId());
  }

  private void createAssessorTask(
      CertificationTaskEntity task,
      CertificationPracticalScoringConfigEntity config,
      CertificationCandidateEntity candidate,
      ExamOperatePaperDetailEntity question,
      CertificationTaskAssessorEntity member
  ) {
    CertificationTaskAssessorGroupEntity group = requireGroup(task.getId(), config.getGroupId());
    List<PracticalScoringModels.TemplateItem> template = parseTemplate(question.getScoringTemplateJson());
    CertificationAssessorTaskEntity entity = new CertificationAssessorTaskEntity();
    entity.setTaskId(task.getId());
    entity.setGroupId(config.getGroupId());
    entity.setTaskAssessorId(member.getId());
    entity.setAssessorId(member.getAssessorId());
    entity.setLeaderAssessorId(group.getLeaderAssessorId());
    entity.setCandidateId(candidate.getId());
    entity.setPaperId(config.getPaperId());
    entity.setPaperQuestionId(question.getId().longValue());
    entity.setRevisionNo(1);
    entity.setVersionNo(0);
    entity.setCandidateSnapshotJson(writeJson(Map.of(
        "candidateNo", nullToEmpty(candidate.getCandidateNo()),
        "candidateName", candidate.getCandidateName(),
        "workTypeName", nullToEmpty(candidate.getWorkTypeName()),
        "levelName", nullToEmpty(candidate.getLevelName()))));
    entity.setQuestionSnapshotJson(questionSnapshot(question, template));
    entity.setQuestionId(String.valueOf(question.getId()));
    entity.setSubject(config.getWorkTypeName() + " " + nullToEmpty(config.getLevelName()));
    entity.setTitle(question.getTitle());
    entity.setDurationMin(null);
    entity.setDueDate(task.getExamDate());
    entity.setStatus("assigned");
    entity.setDeleted(0);
    assessorTaskMapper.insert(entity);
    insertScoreRows(task.getId(), entity.getId(), template);
  }

  private void insertScoreRows(Long taskId, Long assessorTaskId, List<PracticalScoringModels.TemplateItem> template) {
    if (template == null || template.isEmpty()) {
      return;
    }
    List<CertificationAssessorTaskScoreEntity> rows = new ArrayList<>(template.size());
    for (PracticalScoringModels.TemplateItem item : template) {
      CertificationAssessorTaskScoreEntity score = new CertificationAssessorTaskScoreEntity();
      score.setAssessorTaskId(assessorTaskId);
      score.setTaskId(taskId);
      score.setStepNo(item.itemNo());
      score.setStepName(item.itemName());
      score.setCriterion(item.criterion());
      score.setMaxScore(item.maxScore());
      score.setMaxDeduction(item.maxDeduction());
      score.setDeductionStep(item.deductionStep());
      score.setDeductedScore(BigDecimal.ZERO);
      score.setAwardedScore(item.maxScore());
      score.setScore(item.maxScore());
      score.setDeleted(0);
      rows.add(score);
    }
    for (CertificationAssessorTaskScoreEntity score : rows) {
      scoreMapper.insert(score);
    }
  }

  private record ScoreInputIndex(
      Map<Long, PracticalScoringModels.SaveScoreItemReq> byScoreItemId,
      Map<Integer, PracticalScoringModels.SaveScoreItemReq> byItemNo,
      int size
  ) {
  }

  private ScoreInputIndex scoreInputIndex(List<PracticalScoringModels.SaveScoreItemReq> items) {
    Map<Long, PracticalScoringModels.SaveScoreItemReq> byScoreItemId = items.stream()
        .filter(item -> item.scoreItemId() != null)
        .collect(Collectors.toMap(
            PracticalScoringModels.SaveScoreItemReq::scoreItemId,
            Function.identity(),
            (left, right) -> right));
    Map<Integer, PracticalScoringModels.SaveScoreItemReq> byItemNo = items.stream()
        .collect(Collectors.toMap(
            PracticalScoringModels.SaveScoreItemReq::itemNo,
            Function.identity(),
            (left, right) -> right));
    return new ScoreInputIndex(byScoreItemId, byItemNo, items.size());
  }

  private PracticalScoringModels.SaveScoreItemReq scoreInput(
      CertificationAssessorTaskScoreEntity score,
      ScoreInputIndex requestItems
  ) {
    if (score.getId() != null) {
      PracticalScoringModels.SaveScoreItemReq byId = requestItems.byScoreItemId().get(score.getId());
      if (byId != null) {
        return byId;
      }
    }
    PracticalScoringModels.SaveScoreItemReq byItemNo = requestItems.byItemNo().get(score.getStepNo());
    if (byItemNo == null) {
      return null;
    }
    if (requestItems.byScoreItemId().isEmpty() || byItemNo.scoreItemId() == null) {
      return byItemNo;
    }
    return null;
  }

  private BigDecimal applyScoreInputs(
      List<CertificationAssessorTaskScoreEntity> scores,
      ScoreInputIndex requestItems
  ) {
    BigDecimal total = BigDecimal.ZERO;
    for (CertificationAssessorTaskScoreEntity score : scores) {
      PracticalScoringModels.SaveScoreItemReq input = scoreInput(score, requestItems);
      BigDecimal deduction = input.deductedScore().stripTrailingZeros();
      validateDeduction(score, deduction);
      BigDecimal awarded = score.getMaxScore().subtract(deduction).max(BigDecimal.ZERO);
      score.setDeductedScore(deduction);
      score.setAwardedScore(awarded);
      score.setScore(awarded);
      score.setRemark(input.remark());
      total = total.add(awarded);
    }
    if (!scores.isEmpty()) {
      persistScoreUpdates(scores);
    }
    return total;
  }

  private void persistScoreUpdates(List<CertificationAssessorTaskScoreEntity> scores) {
    for (CertificationAssessorTaskScoreEntity score : scores) {
      scoreMapper.updateById(score);
    }
  }

  private void resetScoresToDefault(List<CertificationAssessorTaskScoreEntity> scores) {
    if (scores == null || scores.isEmpty()) {
      return;
    }
    for (CertificationAssessorTaskScoreEntity score : scores) {
      score.setDeductedScore(BigDecimal.ZERO);
      score.setAwardedScore(score.getMaxScore());
      score.setScore(score.getMaxScore());
      score.setRemark(null);
    }
    persistScoreUpdates(scores);
  }

  private GenerateCounter generateForCandidate(
      CertificationTaskEntity task,
      CertificationPracticalScoringConfigEntity config,
      CertificationCandidateEntity candidate,
      List<CertificationTaskAssessorEntity> members,
      List<ExamOperatePaperDetailEntity> questions
  ) {
    int created = 0;
    int existing = 0;
    replaceCandidateTasksIfPaperChanged(task, config, candidate, questions.size());
    startService.ensureSession(config, candidate.getId());
    ensureCandidateResult(task, candidate, questions.size());
    Set<String> existingTaskKeys = loadExistingAssessorTaskKeys(task.getId(), candidate.getId());
    for (ExamOperatePaperDetailEntity question : questions) {
      ensureQuestionResult(task, candidate, config.getPaperId(), question.getId().longValue());
      for (CertificationTaskAssessorEntity member : members) {
        String key = assessorTaskKey(question.getId().longValue(), member.getAssessorId());
        if (existingTaskKeys.contains(key)) {
          existing++;
          continue;
        }
        createAssessorTask(task, config, candidate, question, member);
        created++;
      }
    }
    return new GenerateCounter(created, existing);
  }

  private Set<String> loadExistingAssessorTaskKeys(Long taskId, Long candidateId) {
    List<CertificationAssessorTaskEntity> existing = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidateId));
    if (existing == null || existing.isEmpty()) {
      return Set.of();
    }
    return existing.stream()
        .filter(t -> t.getPaperQuestionId() != null && t.getAssessorId() != null)
        .map(t -> assessorTaskKey(t.getPaperQuestionId(), t.getAssessorId()))
        .collect(Collectors.toSet());
  }

  private String assessorTaskKey(Long paperQuestionId, Long assessorId) {
    return paperQuestionId + "@" + assessorId;
  }

  private void replaceCandidateTasksIfPaperChanged(
      CertificationTaskEntity task,
      CertificationPracticalScoringConfigEntity config,
      CertificationCandidateEntity candidate,
      int questionCount
  ) {
    List<CertificationAssessorTaskEntity> existingTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, task.getId())
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidate.getId())
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (existingTasks == null) {
      existingTasks = List.of();
    }
    boolean hasOldPaper = existingTasks.stream()
        .anyMatch(existing -> !config.getPaperId().equals(existing.getPaperId()));
    if (!hasOldPaper) {
      return;
    }
    boolean hasLockedTask = existingTasks.stream()
        .anyMatch(existing -> !EDITABLE_STATUSES.contains(existing.getStatus()));
    if (hasLockedTask) {
      throw new BusinessException(ErrorCode.CONFLICT, "考生已有已提交的实操评分任务，不能替换评分表");
    }
    List<Long> existingTaskIds = existingTasks.stream()
        .map(CertificationAssessorTaskEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (!existingTaskIds.isEmpty()) {
      scoreMapper.delete(new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
          .in(CertificationAssessorTaskScoreEntity::getAssessorTaskId, existingTaskIds));
      assessorTaskMapper.delete(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
          .in(CertificationAssessorTaskEntity::getId, existingTaskIds));
    }
    List<CertificationPracticalQuestionResultEntity> oldResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidate.getId()));
    if (oldResults == null) {
      oldResults = List.of();
    }
    if (!oldResults.isEmpty()) {
      questionResultMapper.delete(new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
          .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getId())
          .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidate.getId()));
    }
    resetCandidateResult(task, candidate, questionCount);
  }

  private record GenerateCounter(int created, int existing) {
  }

  private List<CertificationTaskAssessorEntity> validateThreeAssessors(Long taskId, Long groupId) {
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, groupId);
    List<CertificationTaskAssessorEntity> members = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId)
            .orderByAsc(CertificationTaskAssessorEntity::getSortOrder));
    if (members.size() != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操考评组必须且只能配置3名考评员");
    }
    if (group.getLeaderAssessorId() == null
        || members.stream().noneMatch(m -> group.getLeaderAssessorId().equals(m.getAssessorId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组组长必须是本组三名考评员之一");
    }
    List<Long> assessorIds = members.stream()
        .map(CertificationTaskAssessorEntity::getAssessorId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (!assessorIds.isEmpty()) {
      Map<Long, CertificationInfoAssessorEntity> assessorById = assessorMapper.selectList(
              new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
                  .in(CertificationInfoAssessorEntity::getId, assessorIds))
          .stream()
          .collect(Collectors.toMap(CertificationInfoAssessorEntity::getId, Function.identity()));
      for (CertificationTaskAssessorEntity member : members) {
        CertificationInfoAssessorEntity assessor = assessorById.get(member.getAssessorId());
        if (assessor == null || assessor.getUserId() == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              member.getAssessorName() + "未绑定有效平台账号，不能生成平板评分任务");
        }
      }
    }
    return members;
  }

  private Map<Long, Integer> loadMemberCountsByTaskId(Long taskId, List<CertificationTaskAssessorGroupEntity> groups) {
    Map<Long, Integer> memberCounts = new HashMap<>();
    if (groups == null || groups.isEmpty()) {
      return memberCounts;
    }
    List<Long> groupIds = groups.stream().map(CertificationTaskAssessorGroupEntity::getId).toList();
    List<CertificationTaskAssessorEntity> allMembers = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .in(CertificationTaskAssessorEntity::getGroupId, groupIds));
    Map<Long, List<CertificationTaskAssessorEntity>> grouped = allMembers == null ? Map.of()
        : allMembers.stream().collect(Collectors.groupingBy(CertificationTaskAssessorEntity::getGroupId));
    for (CertificationTaskAssessorGroupEntity group : groups) {
      memberCounts.put(group.getId(), grouped.getOrDefault(group.getId(), List.of()).size());
    }
    return memberCounts;
  }

  private CertificationTaskScopeEntity resolveScopeForCandidate(
      Long taskId,
      CertificationCandidateEntity candidate,
      Long requestedScopeId
  ) {
    if (requestedScopeId != null) {
      CertificationTaskScopeEntity scope = scopeMapper.selectById(requestedScopeId);
      if (scope == null || !taskId.equals(scope.getTaskId())) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "工种等级范围不存在");
      }
      if (!matchesCandidateScope(scope, candidate)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "学员工种等级与所选筛选条件不一致");
      }
      return scope;
    }
    List<CertificationTaskScopeEntity> scopes = scopeMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .eq(CertificationTaskScopeEntity::getLevelCode, candidate.getLevelCode())
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder));
    return scopes.stream()
        .filter(scope -> matchesCandidateScope(scope, candidate))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "未找到匹配学员工种等级的任务范围"));
  }

  private boolean matchesCandidateScope(
      CertificationTaskScopeEntity scope,
      CertificationCandidateEntity candidate
  ) {
    boolean sameLevel = Objects.equals(scope.getLevelCode(), candidate.getLevelCode());
    boolean sameWorkType = candidate.getWorkTypeId() != null
        ? Objects.equals(scope.getWorkTypeId(), candidate.getWorkTypeId())
            || Objects.equals(scope.getWorkTypeName(), candidate.getWorkTypeName())
        : Objects.equals(scope.getWorkTypeName(), candidate.getWorkTypeName());
    return sameLevel && sameWorkType;
  }

  private Long effectiveWorkTypeId(
      CertificationCandidateEntity candidate,
      CertificationTaskScopeEntity scope,
      Long requestedWorkTypeId,
      ExamOperatePaperEntity paper
  ) {
    if (candidate.getWorkTypeId() != null) {
      return candidate.getWorkTypeId();
    }
    if (scope.getWorkTypeId() != null) {
      return scope.getWorkTypeId();
    }
    if (requestedWorkTypeId != null) {
      return requestedWorkTypeId;
    }
    return paper.getWorkTypeId() == null ? null : paper.getWorkTypeId().longValue();
  }

  private CertificationPracticalScoringConfigEntity upsertCandidateConfig(
      CertificationTaskEntity task,
      CertificationTaskScopeEntity scope,
      CertificationCandidateEntity candidate,
      PracticalScoringModels.LinkCandidateReq req,
      Long workTypeId
  ) {
    CertificationPracticalScoringConfigEntity entity = configMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, task.getId())
            .eq(CertificationPracticalScoringConfigEntity::getCandidateId, candidate.getId())
            .last("LIMIT 1"));
    if (entity == null) {
      entity = new CertificationPracticalScoringConfigEntity();
      entity.setTaskId(task.getId());
      entity.setCandidateId(candidate.getId());
      entity.setDeleted(0);
    }
    entity.setScopeId(scope.getId());
    entity.setWorkTypeId(workTypeId);
    entity.setWorkTypeName(StringUtils.hasText(scope.getWorkTypeName())
        ? scope.getWorkTypeName()
        : candidate.getWorkTypeName());
    entity.setLevelCode(StringUtils.hasText(scope.getLevelCode())
        ? scope.getLevelCode()
        : candidate.getLevelCode());
    entity.setLevelName(StringUtils.hasText(scope.getLevelName())
        ? scope.getLevelName()
        : candidate.getLevelName());
    entity.setExamDate(task.getExamDate());
    entity.setPaperId(req.paperId());
    entity.setGroupId(req.groupId());
    entity.setStatus("configured");
    if (entity.getId() == null) {
      configMapper.insert(entity);
    } else {
      configMapper.updateById(entity);
    }
    return entity;
  }

  private ParsedPracticalPaper parseScoreRecordWorkbook(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "评分记录表没有工作表");
      }
      DataFormatter formatter = new DataFormatter();
      List<Sheet> recordSheets = scoreRecordSheets(workbook);
      if (recordSheets.size() > 1) {
        return parseFullScoreRecordWorkbook(recordSheets, formatter);
      }
      Sheet sheet = recordSheets.isEmpty() ? workbook.getSheetAt(0) : recordSheets.get(0);
      List<Integer> questionStarts = new ArrayList<>();
      int totalColumn = Math.max(0, sheet.getRow(3) == null ? sheet.getLastRowNum() : sheet.getRow(3).getLastCellNum() - 1);
      for (int col = 3; col <= totalColumn; col++) {
        String title = directCellText(sheet, 3, col, formatter);
        if (StringUtils.hasText(title) && title.contains("得分合计")) {
          totalColumn = col - 1;
          break;
        }
        if (StringUtils.hasText(title) && title.startsWith("试题")) {
          questionStarts.add(col);
          continue;
        }
        if (StringUtils.hasText(title) && title.startsWith("试题")) {
          questionStarts.add(col);
        }
        if (StringUtils.hasText(title) && title.contains("得分合计")) {
          totalColumn = col - 1;
          break;
        }
      }
      if (questionStarts.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到试题标题，请确认第4行包含“试题”");
      }
      List<ParsedPracticalQuestion> questions = new ArrayList<>();
      BigDecimal paperScore = BigDecimal.ZERO;
      for (int i = 0; i < questionStarts.size(); i++) {
        int startCol = questionStarts.get(i);
        int endCol = i + 1 < questionStarts.size() ? questionStarts.get(i + 1) - 1 : totalColumn;
        ParsedPracticalQuestion question = parseQuestionBlock(sheet, formatter, startCol, endCol);
        questions.add(question);
        paperScore = paperScore.add(question.score());
      }
      String headerTitle = cellText(sheet, 0, 0, formatter);
      String planName = cellText(sheet, 2, 0, formatter);
      String title = StringUtils.hasText(planName) ? planName : headerTitle;
      if (!StringUtils.hasText(title)) {
        title = "认定实操评分记录表";
      }
      return new ParsedPracticalPaper(trimTitle(title), paperScore, questions);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("评分记录表解析失败: {}", ex.getMessage());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分记录表解析失败：" + ex.getMessage());
    }
  }

  private List<Sheet> scoreRecordSheets(Workbook workbook) {
    List<Sheet> sheets = new ArrayList<>();
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      String name = sheet.getSheetName();
      if (StringUtils.hasText(name) && name.contains("评分汇总表")) {
        continue;
      }
      if (StringUtils.hasText(name) && name.contains("评分记录表")) {
        sheets.add(sheet);
        continue;
      }
      DataFormatter formatter = new DataFormatter();
      String title = cellText(sheet, 0, 0, formatter);
      if (StringUtils.hasText(title) && title.contains("评分记录表")) {
        sheets.add(sheet);
      }
    }
    return sheets;
  }

  private ParsedPracticalPaper parseFullScoreRecordWorkbook(
      List<Sheet> recordSheets,
      DataFormatter formatter
  ) {
    if (recordSheets.size() < MIN_FULL_RECORD_QUESTIONS || recordSheets.size() > MAX_FULL_RECORD_QUESTIONS) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "完整实操评分记录表必须包含4-8张题目表，当前识别到" + recordSheets.size() + "张");
    }
    List<ParsedPracticalQuestion> questions = new ArrayList<>();
    BigDecimal paperScore = BigDecimal.ZERO;
    for (Sheet sheet : recordSheets) {
      ParsedPracticalQuestion question = parseQuestionBlock(sheet, formatter, 3, scoreRecordQuestionEndColumn(sheet, formatter));
      questions.add(question);
      paperScore = paperScore.add(question.score());
    }
    if (paperScore.compareTo(FULL_RECORD_TOTAL_SCORE) != 0) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "完整实操评分记录表总分必须为100分，当前合计为"
              + paperScore.stripTrailingZeros().toPlainString() + "分");
    }
    String title = trimTitle(cellText(recordSheets.get(0), 2, 0, formatter));
    if (!StringUtils.hasText(title)) {
      title = "认定实操评分记录表";
    }
    return new ParsedPracticalPaper(title, FULL_RECORD_TOTAL_SCORE, questions);
  }

  private int scoreRecordQuestionEndColumn(Sheet sheet, DataFormatter formatter) {
    Row titleRow = sheet.getRow(3);
    int lastColumn = titleRow == null ? 3 : Math.max(3, titleRow.getLastCellNum() - 1);
    for (int col = 3; col <= lastColumn; col++) {
      String title = directCellText(sheet, 3, col, formatter);
      if (StringUtils.hasText(title) && title.contains("得分合计")) {
        return col - 1;
      }
    }
    return lastColumn;
  }

  private ParsedPracticalQuestion parseQuestionBlock(
      Sheet sheet,
      DataFormatter formatter,
      int startCol,
      int endCol
  ) {
    String title = cellText(sheet, 3, startCol, formatter);
    if (!StringUtils.hasText(title)) {
      title = "实操题目";
    }
    String content = cellText(sheet, 4, startCol + 1, formatter);
    BigDecimal titleScore = questionScore(title);
    List<PracticalScoringModels.TemplateItem> items = new ArrayList<>();
    int itemNo = 1;
    BigDecimal score = BigDecimal.ZERO;
    for (int col = startCol + 1; col <= endCol; col++) {
      String marker = cellText(sheet, 10, col, formatter);
      if (!"扣分".equals(marker)) {
        continue;
      }
      String criterion = cellText(sheet, 7, col, formatter);
      BigDecimal maxDeduction = decimal(cellText(sheet, 8, col, formatter), BigDecimal.ZERO);
      BigDecimal step = decimal(cellText(sheet, 9, col, formatter), BigDecimal.ONE);
      if (!StringUtils.hasText(criterion) || maxDeduction.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      String factor = cellText(sheet, 5, col, formatter);
      String itemName = StringUtils.hasText(factor) ? factor : "评分要素" + itemNo;
      items.add(new PracticalScoringModels.TemplateItem(
          itemNo++,
          itemName,
          maxDeduction,
          criterion,
          maxDeduction,
          step.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : step));
      score = score.add(maxDeduction);
    }
    if (items.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题“" + title + "”未识别到评分项");
    }
    if (titleScore != null && score.compareTo(titleScore) != 0) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "试题“" + title + "”评分项合计为" + score.stripTrailingZeros().toPlainString()
              + "分，与题目分值" + titleScore.stripTrailingZeros().toPlainString() + "分不一致");
    }
    return new ParsedPracticalQuestion(title, content, "", "", titleScore == null ? score : titleScore, items);
  }

  private BigDecimal questionScore(String title) {
    if (!StringUtils.hasText(title)) {
      return null;
    }
    Matcher matcher = QUESTION_SCORE_PATTERN.matcher(title);
    if (!matcher.find()) {
      return null;
    }
    return new BigDecimal(matcher.group(1));
  }

  private String cellText(Sheet sheet, int rowIndex, int colIndex, DataFormatter formatter) {
    Row row = sheet.getRow(rowIndex);
    if (row != null) {
      Cell cell = row.getCell(colIndex);
      if (cell != null) {
        String value = formatter.formatCellValue(cell).trim();
        if (StringUtils.hasText(value)) {
          if ("扣分".equals(value)) {
            return "扣分";
          }
          return value;
        }
      }
    }
    for (CellRangeAddress range : sheet.getMergedRegions()) {
      if (range.isInRange(rowIndex, colIndex)) {
        Row firstRow = sheet.getRow(range.getFirstRow());
        if (firstRow == null) {
          return "";
        }
        Cell firstCell = firstRow.getCell(range.getFirstColumn());
        return firstCell == null ? "" : formatter.formatCellValue(firstCell).trim();
      }
    }
    return "";
  }

  private String directCellText(Sheet sheet, int rowIndex, int colIndex, DataFormatter formatter) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      return "";
    }
    Cell cell = row.getCell(colIndex);
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }

  private BigDecimal decimal(String value, BigDecimal fallback) {
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    try {
      return new BigDecimal(value.replace("分", "").trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private String trimTitle(String title) {
    String normalized = title.replaceAll("\\s+", " ").trim();
    return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
  }

  private record ParsedPracticalPaper(
      String title,
      BigDecimal totalScore,
      List<ParsedPracticalQuestion> questions
  ) {
  }

  private record ParsedPracticalQuestion(
      String title,
      String content,
      String studentPrepare,
      String equipmentPrepare,
      BigDecimal score,
      List<PracticalScoringModels.TemplateItem> items
  ) {
  }

  private record ScoreRecordAssessor(
      Long assessorId,
      String name,
      int sortOrder
  ) {
  }

  private List<CertificationCandidateEntity> candidatesForConfig(
      CertificationPracticalScoringConfigEntity config
  ) {
    if (config.getCandidateId() != null) {
      CertificationCandidateEntity candidate = candidateMapper.selectById(config.getCandidateId());
      if (candidate == null || !config.getTaskId().equals(candidate.getTaskId())) {
        return List.of();
      }
      return List.of(candidate);
    }
    return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, config.getTaskId())
        .eq(CertificationCandidateEntity::getStatus, "active")
        .eq(CertificationCandidateEntity::getLevelCode, config.getLevelCode())
        .and(w -> w.eq(CertificationCandidateEntity::getWorkTypeId, config.getWorkTypeId())
            .or()
            .and(n -> n.isNull(CertificationCandidateEntity::getWorkTypeId)
                .eq(CertificationCandidateEntity::getWorkTypeName, config.getWorkTypeName()))));
  }

  private CertificationPracticalScoringConfigEntity resolveAbsentConfig(
      Long taskId,
      CertificationCandidateEntity candidate
  ) {
    CertificationPracticalScoringConfigEntity candidateConfig = configMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
            .eq(CertificationPracticalScoringConfigEntity::getCandidateId, candidate.getId())
            .eq(CertificationPracticalScoringConfigEntity::getStatus, "configured")
            .last("LIMIT 1"));
    if (candidateConfig != null) {
      return candidateConfig;
    }
    List<CertificationPracticalScoringConfigEntity> configs = configMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalScoringConfigEntity>()
            .eq(CertificationPracticalScoringConfigEntity::getTaskId, taskId)
            .eq(CertificationPracticalScoringConfigEntity::getStatus, "configured")
            .isNull(CertificationPracticalScoringConfigEntity::getCandidateId));
    if (configs == null) {
      configs = List.of();
    }
    return configs.stream()
        .filter(config -> matchesAbsentConfig(config, candidate))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
            "configure practical paper and assessor group before marking absent"));
  }

  private boolean matchesAbsentConfig(
      CertificationPracticalScoringConfigEntity config,
      CertificationCandidateEntity candidate
  ) {
    boolean sameLevel = Objects.equals(config.getLevelCode(), candidate.getLevelCode())
        || Objects.equals(config.getLevelName(), candidate.getLevelName());
    boolean sameWorkType = candidate.getWorkTypeId() != null
        ? Objects.equals(config.getWorkTypeId(), candidate.getWorkTypeId())
            || Objects.equals(config.getWorkTypeName(), candidate.getWorkTypeName())
        : Objects.equals(config.getWorkTypeName(), candidate.getWorkTypeName());
    return sameLevel && sameWorkType;
  }

  private void applyAbsentScores(
      CertificationTaskEntity task,
      CertificationPracticalScoringConfigEntity config,
      CertificationCandidateEntity candidate,
      int questionCount
  ) {
    LocalDateTime now = LocalDateTime.now();
    List<CertificationAssessorTaskEntity> scoringTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, task.getId())
            .eq(CertificationAssessorTaskEntity::getCandidateId, candidate.getId())
            .eq(CertificationAssessorTaskEntity::getPaperId, config.getPaperId())
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (scoringTasks == null || scoringTasks.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "practical scoring tasks are not generated");
    }
    List<Long> assessorTaskIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    for (CertificationAssessorTaskEntity scoringTask : scoringTasks) {
      scoringTask.setStatus("absent");
      scoringTask.setTotalScore(ZERO_SCORE);
      scoringTask.setSubmittedAt(null);
      scoringTask.setProgressNote("absent");
      scoringTask.setVersionNo(nvl(scoringTask.getVersionNo(), 0) + 1);
      assessorTaskMapper.updateById(scoringTask);
    }
    if (!assessorTaskIds.isEmpty()) {
      List<CertificationAssessorTaskScoreEntity> scoreRows = scoreMapper.selectList(
          new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
              .in(CertificationAssessorTaskScoreEntity::getAssessorTaskId, assessorTaskIds));
      if (scoreRows != null) {
        for (CertificationAssessorTaskScoreEntity scoreRow : scoreRows) {
          scoreRow.setDeductedScore(ZERO_SCORE);
          scoreRow.setAwardedScore(ZERO_SCORE);
          scoreRow.setScore(ZERO_SCORE);
          scoreMapper.updateById(scoreRow);
        }
      }
    }

    List<CertificationPracticalQuestionResultEntity> questionResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidate.getId())
            .eq(CertificationPracticalQuestionResultEntity::getPaperId, config.getPaperId()));
    if (questionResults == null) {
      questionResults = List.of();
    }
    for (CertificationPracticalQuestionResultEntity questionResult : questionResults) {
      questionResult.setSubmittedCount(0);
      questionResult.setAverageScore(ZERO_SCORE);
      questionResult.setFinalScore(ZERO_SCORE);
      questionResult.setStatus("absent");
      questionResult.setCalculatedAt(now);
      questionResultMapper.updateById(questionResult);
    }

    CertificationPracticalCandidateResultEntity result = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidate.getId())
            .last("LIMIT 1"));
    if (result == null) {
      result = new CertificationPracticalCandidateResultEntity();
      result.setTaskId(task.getId());
      result.setCandidateId(candidate.getId());
      result.setDeleted(0);
      result.setQuestionCount(questionCount);
      result.setCompletedQuestionCount(0);
      result.setTotalScore(ZERO_SCORE);
      result.setStatus("absent");
      result.setCalculatedAt(now);
      candidateResultMapper.insert(result);
    } else {
      result.setQuestionCount(questionCount);
      result.setCompletedQuestionCount(0);
      result.setTotalScore(ZERO_SCORE);
      result.setStatus("absent");
      result.setCalculatedAt(now);
      candidateResultMapper.updateById(result);
    }
  }

  private void ensureQuestionResult(
      CertificationTaskEntity task,
      CertificationCandidateEntity candidate,
      Long paperId,
      Long questionId
  ) {
    long count = safeCount(questionResultMapper.selectCount(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidate.getId())
            .eq(CertificationPracticalQuestionResultEntity::getPaperQuestionId, questionId)));
    if (count > 0) {
      return;
    }
    CertificationPracticalQuestionResultEntity result = new CertificationPracticalQuestionResultEntity();
    result.setTaskId(task.getId());
    result.setCandidateId(candidate.getId());
    result.setPaperId(paperId);
    result.setPaperQuestionId(questionId);
    result.setSubmittedCount(0);
    result.setStatus("pending");
    result.setDeleted(0);
    questionResultMapper.insert(result);
  }

  private void ensureCandidateResult(
      CertificationTaskEntity task,
      CertificationCandidateEntity candidate,
      int questionCount
  ) {
    CertificationPracticalCandidateResultEntity existing = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidate.getId())
            .last("LIMIT 1"));
    if (existing != null) {
      existing.setQuestionCount(questionCount);
      candidateResultMapper.updateById(existing);
      return;
    }
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setTaskId(task.getId());
    result.setCandidateId(candidate.getId());
    result.setQuestionCount(questionCount);
    result.setCompletedQuestionCount(0);
    result.setStatus("pending");
    result.setDeleted(0);
    candidateResultMapper.insert(result);
  }

  private void resetCandidateResult(
      CertificationTaskEntity task,
      CertificationCandidateEntity candidate,
      int questionCount
  ) {
    CertificationPracticalCandidateResultEntity existing = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, candidate.getId())
            .last("LIMIT 1"));
    if (existing == null) {
      return;
    }
    existing.setQuestionCount(questionCount);
    existing.setCompletedQuestionCount(0);
    existing.setTotalScore(null);
    existing.setStatus("pending");
    existing.setCalculatedAt(null);
    candidateResultMapper.updateById(existing);
  }

  private PracticalScoringModels.AssessorTaskDetail detail(CertificationAssessorTaskEntity task) {
    return detail(task, startService.forAssessorTask(task.getId()));
  }

  private PracticalScoringModels.AssessorTaskDetail detail(
      CertificationAssessorTaskEntity task,
      PracticalScoringModels.StartSessionView startSession
  ) {
    JsonNode candidate = readTree(task.getCandidateSnapshotJson());
    JsonNode question = readTree(task.getQuestionSnapshotJson());
    PracticalScoringModels.AssessorTaskListItem listItem = new PracticalScoringModels.AssessorTaskListItem(
        task.getId(), task.getTaskId(), task.getCandidateId(),
        text(candidate, "candidateNo"), text(candidate, "candidateName"),
        text(candidate, "workTypeName"), text(candidate, "levelName"),
        task.getPaperQuestionId(), task.getTitle(), task.getStatus(), task.getTotalScore(),
        nvl(task.getVersionNo(), 0), nvl(task.getRevisionNo(), 1), task.getUpdatedAt(), task.getSubmittedAt(),
        Objects.equals(task.getAssessorId(), task.getLeaderAssessorId()));
    return new PracticalScoringModels.AssessorTaskDetail(
        listItem,
        text(question, "content"),
        text(question, "studentPrepare"),
        text(question, "equipmentPrepare"),
        scores(task.getId()).stream().map(this::scoreItemView).toList(),
        task.getProgressNote(),
        task.getReturnReason(),
        startSession
    );
  }

  private Map<ExamGroupKey, List<CertificationAssessorTaskEntity>> groupExamTasks(
      List<CertificationAssessorTaskEntity> tasks
  ) {
    Map<ExamGroupKey, List<CertificationAssessorTaskEntity>> grouped = new LinkedHashMap<>();
    for (CertificationAssessorTaskEntity task : tasks) {
      ExamGroupKey key = new ExamGroupKey(task.getTaskId(), task.getCandidateId(), task.getPaperId());
      grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(task);
    }
    return grouped;
  }

  private BigDecimal scoreDeviation(
      List<CertificationAssessorTaskEntity> tasks,
      Long candidateId,
      Long paperQuestionId
  ) {
    List<BigDecimal> submittedScores = tasks.stream()
        .filter(task -> Objects.equals(candidateId, task.getCandidateId()))
        .filter(task -> Objects.equals(paperQuestionId, task.getPaperQuestionId()))
        .filter(task -> "submitted".equals(task.getStatus()))
        .map(CertificationAssessorTaskEntity::getTotalScore)
        .filter(Objects::nonNull)
        .toList();
    if (submittedScores.size() < 2) {
      return null;
    }
    BigDecimal min = submittedScores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    BigDecimal max = submittedScores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    return max.subtract(min);
  }

  private Map<Long, Integer> paperTotals(List<CertificationAssessorTaskEntity> tasks) {
    List<Integer> paperIds = tasks.stream()
        .map(CertificationAssessorTaskEntity::getPaperId)
        .filter(Objects::nonNull)
        .filter(id -> id <= Integer.MAX_VALUE)
        .map(Long::intValue)
        .distinct()
        .toList();
    if (paperIds.isEmpty()) {
      return Map.of();
    }
    return paperMapper.selectBatchIds(paperIds).stream()
        .collect(Collectors.toMap(
            paper -> paper.getId().longValue(),
            paper -> paper.getTotalPoint() == null ? FULL_RECORD_TOTAL_SCORE.intValueExact() : paper.getTotalPoint(),
            (left, right) -> left));
  }

  private PracticalScoringModels.AssessorExamListItem examListItem(
      List<CertificationAssessorTaskEntity> tasks,
      Map<Long, Integer> paperTotals
  ) {
    List<CertificationAssessorTaskEntity> sorted = tasks.stream()
        .sorted(Comparator.comparing(CertificationAssessorTaskEntity::getPaperQuestionId))
        .toList();
    CertificationAssessorTaskEntity first = sorted.get(0);
    JsonNode candidate = readTree(first.getCandidateSnapshotJson());
    int submittedQuestions = (int) sorted.stream().filter(task -> "submitted".equals(task.getStatus())).count();
    BigDecimal currentScore = sorted.stream()
        .map(CertificationAssessorTaskEntity::getTotalScore)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    LocalDateTime updatedAt = sorted.stream()
        .map(CertificationAssessorTaskEntity::getUpdatedAt)
        .filter(Objects::nonNull)
        .max(LocalDateTime::compareTo)
        .orElse(null);
    CertificationPracticalCandidateResultEntity result = candidateResultMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, first.getTaskId())
            .eq(CertificationPracticalCandidateResultEntity::getCandidateId, first.getCandidateId())
            .last("LIMIT 1"));
    String resultStatus = result == null ? "pending" : result.getStatus();
    return new PracticalScoringModels.AssessorExamListItem(
        first.getTaskId(),
        first.getCandidateId(),
        first.getPaperId(),
        text(candidate, "candidateNo"),
        text(candidate, "candidateName"),
        text(candidate, "workTypeName"),
        text(candidate, "levelName"),
        submittedQuestions,
        sorted.size(),
        FULL_RECORD_TOTAL_SCORE.intValueExact(),
        currentScore,
        aggregateExamStatus(sorted),
        updatedAt,
        startService.forAssessorTask(first.getId()),
        resultStatus,
        "review_required".equals(resultStatus));
  }

  private String aggregateExamStatus(List<CertificationAssessorTaskEntity> tasks) {
    if (tasks.stream().anyMatch(task -> "returned".equals(task.getStatus()))) {
      return "returned";
    }
    if (!tasks.isEmpty() && tasks.stream().allMatch(task -> "submitted".equals(task.getStatus()))) {
      return "submitted";
    }
    if (tasks.stream().anyMatch(task -> "draft".equals(task.getStatus()))) {
      return "draft";
    }
    return "assigned";
  }

  private PracticalScoringModels.AssessorTaskListItem listItem(CertificationAssessorTaskEntity task) {
    JsonNode candidate = readTree(task.getCandidateSnapshotJson());
    return new PracticalScoringModels.AssessorTaskListItem(
        task.getId(), task.getTaskId(), task.getCandidateId(),
        text(candidate, "candidateNo"), text(candidate, "candidateName"),
        text(candidate, "workTypeName"), text(candidate, "levelName"),
        task.getPaperQuestionId(), task.getTitle(), task.getStatus(), task.getTotalScore(),
        nvl(task.getVersionNo(), 0), nvl(task.getRevisionNo(), 1), task.getUpdatedAt(), task.getSubmittedAt(),
        Objects.equals(task.getAssessorId(), task.getLeaderAssessorId()));
  }

  private record ExamGroupKey(Long taskId, Long candidateId, Long paperId) {
  }

  private PracticalScoringModels.ScoreItemView scoreItemView(CertificationAssessorTaskScoreEntity score) {
    return new PracticalScoringModels.ScoreItemView(
        score.getId(), score.getStepNo(), score.getStepName(), score.getCriterion(),
        score.getId(), score.getStepName(), score.getCriterion(),
        score.getMaxScore(), score.getMaxDeduction(), score.getDeductionStep(),
        score.getDeductedScore(), score.getAwardedScore(), score.getRemark());
  }

  private PracticalScoringModels.PaperOption paperOption(ExamOperatePaperEntity paper) {
    List<ExamOperatePaperDetailEntity> questions = paperQuestions(paper.getId().longValue());
    return new PracticalScoringModels.PaperOption(
        paper.getId().longValue(),
        paper.getWorkTypeId() == null ? null : paper.getWorkTypeId().longValue(),
        paper.getLevel(),
        paper.getTitle(),
        paper.getTotalPoint(),
        questions.stream().map(question -> {
          List<PracticalScoringModels.TemplateItem> items = parseTemplate(question.getScoringTemplateJson());
          return new PracticalScoringModels.PaperQuestionOption(
              question.getId().longValue(), question.getTitle(), question.getScore(),
              !items.isEmpty(), items);
        }).toList()
    );
  }

  private PracticalScoringModels.ConfigView configView(CertificationPracticalScoringConfigEntity entity) {
    return new PracticalScoringModels.ConfigView(
        entity.getId(), entity.getTaskId(), entity.getScopeId(), entity.getCandidateId(), entity.getWorkTypeId(),
        entity.getWorkTypeName(), entity.getLevelCode(), entity.getLevelName(), entity.getExamDate(),
        entity.getPaperId(), entity.getGroupId(), entity.getStatus());
  }

  private List<PracticalScoringModels.TemplateItem> normalizeTemplate(
      List<PracticalScoringModels.TemplateItem> items,
      BigDecimal questionScore
  ) {
    if (CollectionUtils.isEmpty(items)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分模板至少包含1个评分要素");
    }
    List<PracticalScoringModels.TemplateItem> sorted = items.stream()
        .sorted(Comparator.comparing(PracticalScoringModels.TemplateItem::itemNo))
        .toList();
    if (sorted.stream().map(PracticalScoringModels.TemplateItem::itemNo).distinct().count() != sorted.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素序号不能重复");
    }
    for (PracticalScoringModels.TemplateItem item : sorted) {
      if (item.maxDeduction().compareTo(item.maxScore()) > 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "最大扣分不能超过评分要素配分");
      }
      if (item.maxDeduction().remainder(item.deductionStep()).compareTo(BigDecimal.ZERO) != 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "最大扣分必须是扣分步长的整数倍");
      }
    }
    BigDecimal total = sorted.stream().map(PracticalScoringModels.TemplateItem::maxScore)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (questionScore != null && total.compareTo(questionScore) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素配分合计必须等于试题分值");
    }
    return sorted;
  }

  private void validateQuestionTemplates(List<ExamOperatePaperDetailEntity> questions) {
    if (questions.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操试卷没有试题");
    }
    for (ExamOperatePaperDetailEntity question : questions) {
      List<PracticalScoringModels.TemplateItem> items = parseTemplate(question.getScoringTemplateJson());
      if (items.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "试题“" + question.getTitle() + "”尚未配置评分模板");
      }
      normalizeTemplate(items, question.getScore());
    }
  }

  private List<PracticalScoringModels.TemplateItem> parseTemplate(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {
      });
      List<PracticalScoringModels.TemplateItem> items = new ArrayList<>();
      int index = 1;
      for (Map<String, Object> row : raw) {
        items.add(new PracticalScoringModels.TemplateItem(
            integer(row, "itemNo", index++),
            string(row, "itemName", string(row, "item", string(row, "yaosu", ""))),
            decimal(row, "maxScore", decimal(row, "score", decimal(row, "peifen", BigDecimal.ZERO))),
            string(row, "criterion", string(row, "scoreDesc", string(row, "biaozhun", ""))),
            decimal(row, "maxDeduction", decimal(row, "maxDeduct", decimal(row, "koufen", BigDecimal.ZERO))),
            decimal(row, "deductionStep", decimal(row, "step", decimal(row, "buchang", BigDecimal.ONE)))
        ));
      }
      return items;
    } catch (Exception ex) {
      log.warn("实操评分模板解析失败: {}", ex.getMessage());
      return List.of();
    }
  }

  private void validateDeduction(CertificationAssessorTaskScoreEntity score, BigDecimal deduction) {
    if (deduction.compareTo(BigDecimal.ZERO) < 0 || deduction.compareTo(score.getMaxDeduction()) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, score.getStepName() + "扣分超出允许范围");
    }
    if (deduction.remainder(score.getDeductionStep()).compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, score.getStepName() + "扣分必须符合步长规则");
    }
  }

  private CertificationAssessorTaskEntity requireOwnedTask(Long id, boolean lock) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = currentAssessor();
    CertificationAssessorTaskEntity task = lock
        ? assessorTaskMapper.lockById(user.getTenantId(), id)
        : assessorTaskMapper.selectById(id);
    if (task == null || task.getCandidateId() == null || !assessor.getId().equals(task.getAssessorId())) {
      log.warn("用户{}尝试访问非本人评分任务{}", user.getUserId(), id);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该评分任务");
    }
    workbenchContextService.requireAccessibleTask(assessor, task.getTaskId());
    return task;
  }

  private CertificationInfoAssessorEntity currentAssessor() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    if (assessor == null) {
      log.warn("用户{}没有已入库考评员身份", user.getUserId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定有效考评员档案");
    }
    return assessor;
  }

  private void refreshAssessorWorkStatus(Long assessorId) {
    List<CertificationTaskAssessorEntity> remainingBindings = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getAssessorId, assessorId));
    String workStatus = (remainingBindings == null || remainingBindings.isEmpty())
        ? "idle"
        : remainingBindings.stream().anyMatch(binding -> "assigned".equals(binding.getWorkStatus()))
            ? "assigned"
            : "preparing";
    CertificationInfoAssessorEntity assessor = assessorMapper.selectById(assessorId);
    if (assessor != null && !Objects.equals(assessor.getWorkStatus(), workStatus)) {
      assessor.setWorkStatus(workStatus);
      assessorMapper.updateById(assessor);
    }
  }

  private CertificationTaskEntity requireTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationTaskAssessorGroupEntity requireGroup(Long taskId, Long groupId) {
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(groupId);
    if (group == null || !taskId.equals(group.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评组不存在");
    }
    return group;
  }

  private ExamOperatePaperEntity requirePaper(Long paperId) {
    if (paperId == null || paperId > Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操试卷ID无效");
    }
    ExamOperatePaperEntity paper = paperMapper.selectById(paperId.intValue());
    if (paper == null || !Integer.valueOf(1).equals(paper.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "有效实操试卷不存在");
    }
    return paper;
  }

  private List<ExamOperatePaperDetailEntity> paperQuestions(Long paperId) {
    return paperDetailMapper.selectList(new LambdaQueryWrapper<ExamOperatePaperDetailEntity>()
        .eq(ExamOperatePaperDetailEntity::getPaperId, paperId.intValue())
        .eq(ExamOperatePaperDetailEntity::getStatus, 1)
        .orderByAsc(ExamOperatePaperDetailEntity::getSortNumber)
        .orderByAsc(ExamOperatePaperDetailEntity::getId));
  }

  private List<CertificationAssessorTaskScoreEntity> scores(Long taskId) {
    return scoreMapper.selectList(new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
        .eq(CertificationAssessorTaskScoreEntity::getAssessorTaskId, taskId)
        .orderByAsc(CertificationAssessorTaskScoreEntity::getStepNo));
  }

  private void writeHistory(
      CertificationAssessorTaskEntity task,
      String action,
      String reason,
      Long operatorId
  ) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("task", Map.of(
        "status", nullToEmpty(task.getStatus()),
        "version", nvl(task.getVersionNo(), 0),
        "revision", nvl(task.getRevisionNo(), 1),
        "totalScore", task.getTotalScore() == null ? "" : task.getTotalScore()));
    snapshot.put("scores", scores(task.getId()).stream().map(score -> Map.of(
        "itemNo", score.getStepNo(),
        "itemName", score.getStepName(),
        "deductedScore", score.getDeductedScore(),
        "awardedScore", score.getAwardedScore(),
        "remark", nullToEmpty(score.getRemark()))).toList());
    CertificationAssessorTaskHistoryEntity history = new CertificationAssessorTaskHistoryEntity();
    history.setTaskId(task.getTaskId());
    history.setAssessorTaskId(task.getId());
    history.setCandidateId(task.getCandidateId());
    history.setPaperQuestionId(task.getPaperQuestionId());
    history.setAssessorId(task.getAssessorId());
    history.setRevisionNo(nvl(task.getRevisionNo(), 1));
    history.setAction(action);
    history.setVersionNo(nvl(task.getVersionNo(), 0));
    history.setTotalScore(task.getTotalScore());
    history.setSnapshotJson(writeJson(snapshot));
    history.setReason(reason);
    history.setOperatedAt(LocalDateTime.now());
    history.setOperatedBy(operatorId);
    history.setDeleted(0);
    historyMapper.insert(history);
  }

  private String questionSnapshot(
      ExamOperatePaperDetailEntity question,
      List<PracticalScoringModels.TemplateItem> template
  ) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", nullToEmpty(question.getTitle()));
    snapshot.put("content", nullToEmpty(question.getContent()));
    snapshot.put("studentPrepare", nullToEmpty(question.getStudentPrepare()));
    snapshot.put("equipmentPrepare", nullToEmpty(question.getEquipPrepare()));
    snapshot.put("score", question.getScore());
    snapshot.put("weight", 100);
    snapshot.put("template", template);
    return writeJson(snapshot);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分数据序列化失败");
    }
  }

  private JsonNode readTree(String json) {
    try {
      return StringUtils.hasText(json) ? objectMapper.readTree(json) : objectMapper.createObjectNode();
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分任务快照损坏");
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? "" : value.asText();
  }

  private int integer(Map<String, Object> row, String key, int fallback) {
    Object value = row.get(key);
    return value == null ? fallback : new BigDecimal(String.valueOf(value)).intValue();
  }

  private String string(Map<String, Object> row, String key, String fallback) {
    Object value = row.get(key);
    return value == null ? fallback : String.valueOf(value).trim();
  }

  private BigDecimal decimal(Map<String, Object> row, String key, BigDecimal fallback) {
    Object value = row.get(key);
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return fallback;
    }
    return new BigDecimal(String.valueOf(value));
  }

  private long safeCount(Long count) {
    return count == null ? 0 : count;
  }

  private int nvl(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String nvlText(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
