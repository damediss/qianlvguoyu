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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
      PracticalStartApplicationService startService
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
    Map<Long, Integer> memberCounts = new HashMap<>();
    for (CertificationTaskAssessorGroupEntity group : groups) {
      Long count = taskAssessorMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
          .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
          .eq(CertificationTaskAssessorEntity::getGroupId, group.getId()));
      memberCounts.put(group.getId(), count == null ? 0 : count.intValue());
    }
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
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getLevelCode, scope.getLevelCode())
            .eq(CertificationCandidateEntity::getWorkTypeName, scope.getWorkTypeName()));
    for (CertificationCandidateEntity candidate : candidates) {
      candidate.setWorkTypeId(req.workTypeId());
      candidateMapper.updateById(candidate);
    }
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
    LambdaQueryWrapper<CertificationAssessorTaskEntity> wrapper =
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId())
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId)
            .orderByAsc(CertificationAssessorTaskEntity::getStatus)
            .orderByDesc(CertificationAssessorTaskEntity::getUpdatedAt);
    if (StringUtils.hasText(status)) {
      wrapper.eq(CertificationAssessorTaskEntity::getStatus, status);
    }
    return assessorTaskMapper.selectList(wrapper).stream().map(this::listItem).toList();
  }

  @Transactional(readOnly = true)
  public List<PracticalScoringModels.AssessorExamListItem> myExams(String status) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    List<CertificationAssessorTaskEntity> tasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getAssessorId, assessor.getId())
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
    if (startService.isCompleted(task.getTaskId(), task.getCandidateId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试已完成，评分数据已锁定");
    }
    if (!"submitted".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已提交的异常评分任务可在实操管理中修改");
    }
    int currentVersion = nvl(task.getVersionNo(), 0);
    if (!Objects.equals(req.version(), currentVersion)) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分任务已被更新，请刷新后再修改");
    }
    List<CertificationAssessorTaskScoreEntity> scores = scores(task.getId());
    Map<Integer, PracticalScoringModels.SaveScoreItemReq> requestItems = req.items().stream()
        .collect(Collectors.toMap(
            PracticalScoringModels.SaveScoreItemReq::itemNo,
            Function.identity(),
            (left, right) -> right));
    if (scores.isEmpty()
        || scores.stream().anyMatch(score -> !requestItems.containsKey(score.getStepNo()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素必须完整提交");
    }
    BigDecimal total = BigDecimal.ZERO;
    for (CertificationAssessorTaskScoreEntity score : scores) {
      PracticalScoringModels.SaveScoreItemReq input = requestItems.get(score.getStepNo());
      BigDecimal deduction = input.deductedScore().stripTrailingZeros();
      validateDeduction(score, deduction);
      BigDecimal awarded = score.getMaxScore().subtract(deduction).max(BigDecimal.ZERO);
      score.setDeductedScore(deduction);
      score.setAwardedScore(awarded);
      score.setScore(awarded);
      score.setRemark(input.remark());
      scoreMapper.updateById(score);
      total = total.add(awarded);
    }
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
    List<CertificationAssessorTaskScoreEntity> scores = scores(task.getId());
    for (CertificationAssessorTaskScoreEntity score : scores) {
      score.setDeductedScore(BigDecimal.ZERO);
      score.setAwardedScore(score.getMaxScore());
      score.setScore(score.getMaxScore());
      score.setRemark(null);
      scoreMapper.updateById(score);
    }
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
          result == null ? null : result.getTotalScore(),
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
    for (Long candidateId : ids) {
      PracticalScoringModels.StartSessionView session = startService.session(taskId, candidateId);
      CertificationPracticalCandidateResultEntity result = resultByCandidate.get(candidateId);
      if (session == null || !"completed".equals(session.status())
          || result == null || !"completed".equals(result.getStatus())) {
        CertificationCandidateEntity candidate = candidateById.get(candidateId);
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            (candidate == null ? "考生" + candidateId : candidate.getCandidateName()) + "尚未完成考试，不能导出汇总表");
      }
    }

    List<CertificationPracticalQuestionResultEntity> questionResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId)
            .in(CertificationPracticalQuestionResultEntity::getCandidateId, ids)
            .orderByAsc(CertificationPracticalQuestionResultEntity::getCandidateId)
            .orderByAsc(CertificationPracticalQuestionResultEntity::getPaperQuestionId));
    if (questionResults.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的实操评分结果");
    }
    List<CertificationAssessorTaskEntity> scoringTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .in(CertificationAssessorTaskEntity::getCandidateId, ids)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByAsc(CertificationAssessorTaskEntity::getCandidateId)
            .orderByAsc(CertificationAssessorTaskEntity::getPaperQuestionId)
            .orderByAsc(CertificationAssessorTaskEntity::getAssessorId));

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
    byte[] data = buildSummaryWorkbook(task, candidates, resultByCandidate,
        questionResultsByCandidate, tasksByCandidateQuestion, questionCount);
    String filename = "评分记录汇总表_" + task.getTaskNo() + "_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .format(LocalDateTime.now()) + ".xlsx";
    return new ExportResultDTO(data, filename);
  }

  private byte[] buildSummaryWorkbook(
      CertificationTaskEntity task,
      List<CertificationCandidateEntity> candidates,
      Map<Long, CertificationPracticalCandidateResultEntity> resultByCandidate,
      Map<Long, List<CertificationPracticalQuestionResultEntity>> questionResultsByCandidate,
      Map<String, List<CertificationAssessorTaskEntity>> tasksByCandidateQuestion,
      int questionCount
  ) {
    int lastColumn = 3 + questionCount * 6;
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("评分记录汇总表");
      CellStyle titleStyle = workbook.createCellStyle();
      Font titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 16);
      titleStyle.setFont(titleFont);
      titleStyle.setAlignment(HorizontalAlignment.CENTER);
      titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      CellStyle infoStyle = workbook.createCellStyle();
      infoStyle.setVerticalAlignment(VerticalAlignment.CENTER);

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      headerStyle.setWrapText(true);
      setBorder(headerStyle);

      CellStyle bodyStyle = workbook.createCellStyle();
      bodyStyle.setAlignment(HorizontalAlignment.CENTER);
      bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      bodyStyle.setWrapText(true);
      setBorder(bodyStyle);

      Row titleRow = sheet.createRow(0);
      titleRow.setHeightInPoints(28);
      setCell(titleRow, 0, "评分记录汇总表", titleStyle);
      merge(sheet, 0, 0, 0, lastColumn);

      int midColumn = Math.max(6, lastColumn / 2);
      Row planRow = sheet.createRow(1);
      setCell(planRow, 0, "计划编码：" + nvlText(task.getTaskNo()), infoStyle);
      merge(sheet, 1, 1, 0, midColumn - 1);
      setCell(planRow, midColumn, "考 点 ：" + nvlText(task.getCompanyName()), infoStyle);
      merge(sheet, 1, 1, midColumn, lastColumn);

      Row taskRow = sheet.createRow(2);
      setCell(taskRow, 0, "计划名称：" + nvlText(task.getTaskName()), infoStyle);
      merge(sheet, 2, 2, 0, midColumn - 1);
      setCell(taskRow, midColumn, "考试时间：" + formatDate(task.getExamDate()), infoStyle);
      merge(sheet, 2, 2, midColumn, lastColumn);

      Row signRow = sheet.createRow(3);
      setCell(signRow, 0, "考评员1(签字):        考评员2(签字):        考评员3(签字):        核分人:", infoStyle);
      merge(sheet, 3, 3, 0, lastColumn);

      Row groupHeader = sheet.createRow(4);
      Row itemHeader = sheet.createRow(5);
      setMergedHeader(sheet, groupHeader, 0, "序号", headerStyle);
      setMergedHeader(sheet, groupHeader, 1, "考生姓名", headerStyle);
      setMergedHeader(sheet, groupHeader, 2, "准考证号", headerStyle);
      String[] subHeaders = {"考评员1\n评分", "考评员2\n评分", "考评员3\n评分", "平均分", "权重", "最后\n得分"};
      for (int i = 0; i < questionCount; i++) {
        int start = 3 + i * 6;
        setCell(groupHeader, start, "试题" + (i + 1), headerStyle);
        merge(sheet, 4, 4, start, start + 5);
        for (int j = 0; j < subHeaders.length; j++) {
          setCell(itemHeader, start + j, subHeaders[j], headerStyle);
        }
      }
      setMergedHeader(sheet, groupHeader, lastColumn, "合计\n得分", headerStyle);

      int rowIndex = 6;
      int sequence = 1;
      for (CertificationCandidateEntity candidate : candidates) {
        Row row = sheet.createRow(rowIndex++);
        setCell(row, 0, sequence++, bodyStyle);
        setCell(row, 1, nvlText(candidate.getCandidateName()), bodyStyle);
        setCell(row, 2, nvlText(candidate.getCandidateNo()), bodyStyle);
        List<CertificationPracticalQuestionResultEntity> questionResults =
            questionResultsByCandidate.getOrDefault(candidate.getId(), List.of());
        for (int i = 0; i < questionCount; i++) {
          int start = 3 + i * 6;
          if (i >= questionResults.size()) {
            for (int j = 0; j < 6; j++) {
              setCell(row, start + j, "", bodyStyle);
            }
            continue;
          }
          CertificationPracticalQuestionResultEntity questionResult = questionResults.get(i);
          List<CertificationAssessorTaskEntity> scoreTasks = tasksByCandidateQuestion.getOrDefault(
              candidate.getId() + ":" + questionResult.getPaperQuestionId(), List.of());
          for (int j = 0; j < 3; j++) {
            setCell(row, start + j,
                j < scoreTasks.size() ? scoreTasks.get(j).getTotalScore() : null, bodyStyle);
          }
          setCell(row, start + 3, questionResult.getAverageScore(), bodyStyle);
          setCell(row, start + 4, 100, bodyStyle);
          setCell(row, start + 5, questionResult.getFinalScore(), bodyStyle);
        }
        CertificationPracticalCandidateResultEntity result = resultByCandidate.get(candidate.getId());
        setCell(row, lastColumn, result == null ? null : result.getTotalScore(), bodyStyle);
      }

      sheet.setColumnWidth(0, 8 * 256);
      sheet.setColumnWidth(1, 14 * 256);
      sheet.setColumnWidth(2, 24 * 256);
      for (int col = 3; col <= lastColumn; col++) {
        sheet.setColumnWidth(col, 11 * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      log.warn("评分记录汇总表导出失败: {}", ex.getMessage(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出评分记录汇总表失败");
    }
  }

  private void setMergedHeader(Sheet sheet, Row row, int column, String value, CellStyle style) {
    setCell(row, column, value, style);
    merge(sheet, 4, 5, column, column);
  }

  private void setCell(Row row, int column, Object value, CellStyle style) {
    Cell cell = row.createCell(column);
    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(value == null ? "" : String.valueOf(value));
    }
    cell.setCellStyle(style);
  }

  private void merge(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
    if (firstRow == lastRow && firstCol == lastCol) {
      return;
    }
    sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
  }

  private void setBorder(CellStyle style) {
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }

  private String formatDate(LocalDate date) {
    return date == null ? "" : date.toString();
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
    Map<Integer, PracticalScoringModels.SaveScoreItemReq> requestItems = req.items().stream()
        .collect(Collectors.toMap(PracticalScoringModels.SaveScoreItemReq::itemNo, Function.identity()));
    if (scores.size() != requestItems.size()
        || scores.stream().anyMatch(score -> !requestItems.containsKey(score.getStepNo()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素必须完整提交");
    }
    BigDecimal total = BigDecimal.ZERO;
    for (CertificationAssessorTaskScoreEntity score : scores) {
      PracticalScoringModels.SaveScoreItemReq input = requestItems.get(score.getStepNo());
      BigDecimal deduction = input.deductedScore().stripTrailingZeros();
      validateDeduction(score, deduction);
      BigDecimal awarded = score.getMaxScore().subtract(deduction).max(BigDecimal.ZERO);
      score.setDeductedScore(deduction);
      score.setAwardedScore(awarded);
      score.setScore(awarded);
      score.setRemark(input.remark());
      scoreMapper.updateById(score);
      total = total.add(awarded);
    }
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
      BigDecimal average = sum.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
      result.setAverageScore(average);
      result.setFinalScore(average.setScale(0, RoundingMode.HALF_UP).intValueExact());
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
      candidateResult.setTotalScore(results.stream().map(CertificationPracticalQuestionResultEntity::getFinalScore)
          .filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
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
    for (PracticalScoringModels.TemplateItem item : template) {
      CertificationAssessorTaskScoreEntity score = new CertificationAssessorTaskScoreEntity();
      score.setAssessorTaskId(entity.getId());
      score.setTaskId(task.getId());
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
      scoreMapper.insert(score);
    }
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
    for (ExamOperatePaperDetailEntity question : questions) {
      ensureQuestionResult(task, candidate, config.getPaperId(), question.getId().longValue());
      for (CertificationTaskAssessorEntity member : members) {
        long count = safeCount(assessorTaskMapper.selectCount(
            new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
                .eq(CertificationAssessorTaskEntity::getTaskId, task.getId())
                .eq(CertificationAssessorTaskEntity::getCandidateId, candidate.getId())
                .eq(CertificationAssessorTaskEntity::getPaperQuestionId, question.getId().longValue())
                .eq(CertificationAssessorTaskEntity::getAssessorId, member.getAssessorId())));
        if (count > 0) {
          existing++;
          continue;
        }
        createAssessorTask(task, config, candidate, question, member);
        created++;
      }
    }
    return new GenerateCounter(created, existing);
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
    for (CertificationAssessorTaskEntity existing : existingTasks) {
      for (CertificationAssessorTaskScoreEntity score : scores(existing.getId())) {
        scoreMapper.deleteById(score.getId());
      }
      assessorTaskMapper.deleteById(existing.getId());
    }
    List<CertificationPracticalQuestionResultEntity> oldResults = questionResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, task.getId())
            .eq(CertificationPracticalQuestionResultEntity::getCandidateId, candidate.getId()));
    if (oldResults == null) {
      oldResults = List.of();
    }
    for (CertificationPracticalQuestionResultEntity oldResult : oldResults) {
      questionResultMapper.deleteById(oldResult.getId());
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
    for (CertificationTaskAssessorEntity member : members) {
      CertificationInfoAssessorEntity assessor = assessorMapper.selectById(member.getAssessorId());
      if (assessor == null || assessor.getUserId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            member.getAssessorName() + "未绑定有效平台账号，不能生成平板评分任务");
      }
    }
    return members;
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
