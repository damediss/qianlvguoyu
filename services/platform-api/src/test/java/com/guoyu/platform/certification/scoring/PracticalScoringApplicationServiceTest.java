package com.guoyu.platform.certification.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
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
import com.guoyu.platform.exam.entity.ExamOperatePaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamOperatePaperEntity;
import com.guoyu.platform.exam.mapper.ExamOperatePaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamOperatePaperMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PracticalScoringApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = Mockito.mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationInfoAssessorMapper assessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final CertificationAssessorTaskMapper assessorTaskMapper = Mockito.mock(CertificationAssessorTaskMapper.class);
  private final CertificationAssessorTaskScoreMapper scoreMapper = Mockito.mock(CertificationAssessorTaskScoreMapper.class);
  private final CertificationPracticalScoringConfigMapper configMapper =
      Mockito.mock(CertificationPracticalScoringConfigMapper.class);
  private final CertificationPracticalQuestionResultMapper questionResultMapper =
      Mockito.mock(CertificationPracticalQuestionResultMapper.class);
  private final CertificationPracticalCandidateResultMapper candidateResultMapper =
      Mockito.mock(CertificationPracticalCandidateResultMapper.class);
  private final CertificationAssessorTaskHistoryMapper historyMapper =
      Mockito.mock(CertificationAssessorTaskHistoryMapper.class);
  private final ExamOperatePaperMapper paperMapper = Mockito.mock(ExamOperatePaperMapper.class);
  private final ExamOperatePaperDetailMapper paperDetailMapper = Mockito.mock(ExamOperatePaperDetailMapper.class);
  private final PracticalStartApplicationService startService =
      Mockito.mock(PracticalStartApplicationService.class);
  private PracticalScoringApplicationService service;

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "考评员", "platform",
        List.of("assessor"), List.of("certification.practical.score"));
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new PracticalScoringApplicationService(
        taskMapper, scopeMapper, candidateMapper, groupMapper, taskAssessorMapper, assessorMapper,
        assessorTaskMapper, scoreMapper, configMapper, questionResultMapper, candidateResultMapper,
        historyMapper, paperMapper, paperDetailMapper, new ObjectMapper(), startService);
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setUserId(100L);
    assessor.setAuditStatus("approved");
    when(assessorMapper.selectOne(any())).thenReturn(assessor);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void submitCalculatesHalfUpQuestionScoreAndCandidateTotal() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 0);
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素一", "10", "10", "1")));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(
        submittedTask(new BigDecimal("9")),
        submittedTask(new BigDecimal("10")),
        task));
    CertificationPracticalQuestionResultEntity questionResult = new CertificationPracticalQuestionResultEntity();
    questionResult.setId(70L);
    questionResult.setTaskId(1L);
    questionResult.setCandidateId(11L);
    questionResult.setPaperQuestionId(21L);
    when(questionResultMapper.lockResult(1L, 1L, 11L, 21L)).thenReturn(questionResult);
    when(questionResultMapper.selectList(any())).thenReturn(List.of(questionResult));
    CertificationPracticalCandidateResultEntity candidateResult = new CertificationPracticalCandidateResultEntity();
    candidateResult.setId(80L);
    when(candidateResultMapper.selectOne(any())).thenReturn(candidateResult);

    service.submit(50L, new PracticalScoringModels.SaveScoreReq(
        0, List.of(new PracticalScoringModels.SaveScoreItemReq(1, BigDecimal.ZERO, null)), null));

    assertThat(questionResult.getAverageScore()).isEqualByComparingTo("9.6667");
    assertThat(questionResult.getFinalScore()).isEqualTo(10);
    assertThat(candidateResult.getTotalScore()).isEqualTo(10);
  }

  @Test
  void submitMarksReviewRequiredWhenAssessorScoresDeviateOverTen() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 0);
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素一", "30", "30", "1")));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(
        submittedTask(new BigDecimal("10")),
        submittedTask(new BigDecimal("18")),
        task));
    CertificationPracticalQuestionResultEntity questionResult = new CertificationPracticalQuestionResultEntity();
    questionResult.setId(70L);
    questionResult.setTaskId(1L);
    questionResult.setCandidateId(11L);
    questionResult.setPaperQuestionId(21L);
    when(questionResultMapper.lockResult(1L, 1L, 11L, 21L)).thenReturn(questionResult);
    when(questionResultMapper.selectList(any())).thenReturn(List.of(questionResult));
    CertificationPracticalCandidateResultEntity candidateResult = new CertificationPracticalCandidateResultEntity();
    candidateResult.setId(80L);
    when(candidateResultMapper.selectOne(any())).thenReturn(candidateResult);

    service.submit(50L, new PracticalScoringModels.SaveScoreReq(
        0, List.of(new PracticalScoringModels.SaveScoreItemReq(1, BigDecimal.ZERO, null)), null));

    assertThat(questionResult.getStatus()).isEqualTo("review_required");
    assertThat(questionResult.getAverageScore()).isNull();
    assertThat(questionResult.getFinalScore()).isNull();
    assertThat(candidateResult.getStatus()).isEqualTo("review_required");
    assertThat(candidateResult.getTotalScore()).isNull();
  }

  @Test
  void adjustScoreAllowsCompletedSessionAndRecalculatesReviewRequiredQuestion() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 2);
    task.setStatus("submitted");
    task.setTotalScore(new BigDecimal("5"));
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(startService.isCompleted(1L, 11L)).thenReturn(true);
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素一", "30", "30", "1")));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(
        submittedTask(new BigDecimal("20")),
        submittedTask(new BigDecimal("25")),
        task));
    CertificationPracticalQuestionResultEntity questionResult = new CertificationPracticalQuestionResultEntity();
    questionResult.setId(70L);
    questionResult.setTaskId(1L);
    questionResult.setCandidateId(11L);
    questionResult.setPaperQuestionId(21L);
    questionResult.setStatus("review_required");
    when(questionResultMapper.lockResult(1L, 1L, 11L, 21L)).thenReturn(questionResult);
    when(questionResultMapper.selectList(any())).thenReturn(List.of(questionResult));
    CertificationPracticalCandidateResultEntity candidateResult = new CertificationPracticalCandidateResultEntity();
    candidateResult.setId(80L);
    candidateResult.setStatus("review_required");
    when(candidateResultMapper.selectOne(any())).thenReturn(candidateResult);

    PracticalScoringModels.AssessorTaskDetail detail = service.adjustScore(
        50L,
        new PracticalScoringModels.AdjustScoreReq(
            2,
            List.of(new PracticalScoringModels.SaveScoreItemReq(1, new BigDecimal("6"), "管理端修正")),
            "管理端复核后修正",
            "三人分差复核调整"));

    assertThat(detail.task().status()).isEqualTo("submitted");
    assertThat(detail.task().totalScore()).isEqualByComparingTo("24");
    assertThat(questionResult.getStatus()).isEqualTo("completed");
    assertThat(questionResult.getFinalScore()).isEqualTo(23);
    assertThat(candidateResult.getStatus()).isEqualTo("completed");
    assertThat(candidateResult.getTotalScore()).isEqualTo(23);
  }

  @Test
  void draftRejectsDeductionOutsideConfiguredStep() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 0);
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素一", "10", "10", "2")));

    assertThatThrownBy(() -> service.saveDraft(50L, new PracticalScoringModels.SaveScoreReq(
        0, List.of(new PracticalScoringModels.SaveScoreItemReq(1, new BigDecimal("3"), null)), null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("步长");
  }

  @Test
  void draftRejectsStaleTabletVersion() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 3);
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);

    assertThatThrownBy(() -> service.saveDraft(50L, new PracticalScoringModels.SaveScoreReq(
        2, List.of(new PracticalScoringModels.SaveScoreItemReq(1, BigDecimal.ZERO, null)), null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("其他设备");
  }

  @Test
  void assessorCannotOpenAnotherAssessorsTask() {
    CertificationAssessorTaskEntity task = task(50L, 10L, 0);
    when(assessorTaskMapper.selectById(50L)).thenReturn(task);

    assertThatThrownBy(() -> service.myTask(50L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无权访问该评分任务");
  }

  @Test
  void linkCandidateAllowsDifferentPapersForCandidatesInSameScope() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, true),
        candidate(12L, "李四", 100L, true),
        paper(20, 100),
        paper(21, 100),
        members(3));
    AtomicLong ids = new AtomicLong(1000);
    doAnswer(invocation -> {
      CertificationPracticalScoringConfigEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      return 1;
    }).when(configMapper).insert(any(CertificationPracticalScoringConfigEntity.class));

    PracticalScoringModels.GenerateView first = service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 20L, 30L));
    PracticalScoringModels.GenerateView second = service.linkCandidate(
        1L, 12L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 21L, 30L));

    ArgumentCaptor<CertificationPracticalScoringConfigEntity> captor =
        ArgumentCaptor.forClass(CertificationPracticalScoringConfigEntity.class);
    Mockito.verify(configMapper, Mockito.times(2)).insert(captor.capture());
    assertThat(first.createdTasks()).isEqualTo(3);
    assertThat(second.createdTasks()).isEqualTo(3);
    assertThat(captor.getAllValues())
        .extracting(CertificationPracticalScoringConfigEntity::getCandidateId)
        .containsExactly(11L, 12L);
    assertThat(captor.getAllValues())
        .extracting(CertificationPracticalScoringConfigEntity::getPaperId)
        .containsExactly(20L, 21L);
  }

  @Test
  void linkCandidateAllowsCustomPaperWithoutStandardWorkType() {
    stubCandidateLinkBase(
        candidate(11L, "张三", null, true),
        null,
        paper(20, null),
        null,
        members(3));
    CertificationTaskScopeEntity scope = scope();
    scope.setWorkTypeId(null);
    when(scopeMapper.selectById(101L)).thenReturn(scope);
    doAnswer(invocation -> {
      CertificationPracticalScoringConfigEntity entity = invocation.getArgument(0);
      entity.setId(1001L);
      return 1;
    }).when(configMapper).insert(any(CertificationPracticalScoringConfigEntity.class));

    PracticalScoringModels.GenerateView result = service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, null, 20L, 30L));

    ArgumentCaptor<CertificationPracticalScoringConfigEntity> captor =
        ArgumentCaptor.forClass(CertificationPracticalScoringConfigEntity.class);
    Mockito.verify(configMapper).insert(captor.capture());
    Mockito.verify(candidateMapper, Mockito.never()).updateById(any(CertificationCandidateEntity.class));
    Mockito.verify(scopeMapper, Mockito.never()).updateById(any(CertificationTaskScopeEntity.class));
    assertThat(result.createdTasks()).isEqualTo(3);
    assertThat(captor.getValue().getWorkTypeId()).isNull();
    assertThat(captor.getValue().getCandidateId()).isEqualTo(11L);
  }

  @Test
  void linkCandidateReplacesExistingGeneratedTasksWhenPaperChanges() {
    CertificationCandidateEntity candidate = candidate(11L, "寮犱笁", 100L, true);
    stubCandidateLinkBase(candidate, null, paper(21, 100), null, members(3));
    CertificationPracticalScoringConfigEntity existingConfig = new CertificationPracticalScoringConfigEntity();
    existingConfig.setId(1001L);
    existingConfig.setTaskId(1L);
    existingConfig.setCandidateId(11L);
    existingConfig.setPaperId(20L);
    existingConfig.setGroupId(30L);
    when(configMapper.selectOne(any())).thenReturn(existingConfig);
    CertificationAssessorTaskEntity oldTask = task(70L, 201L, 0);
    oldTask.setPaperId(20L);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(oldTask));
    CertificationAssessorTaskScoreEntity oldScore = score(1, "鏃ц瘎鍒嗛」", "10", "10", "1");
    oldScore.setId(701L);
    oldScore.setAssessorTaskId(70L);
    when(scoreMapper.selectList(any())).thenReturn(List.of(oldScore));
    CertificationPracticalQuestionResultEntity oldResult = new CertificationPracticalQuestionResultEntity();
    oldResult.setId(801L);
    oldResult.setTaskId(1L);
    oldResult.setCandidateId(11L);
    oldResult.setPaperId(20L);
    oldResult.setPaperQuestionId(21L);
    when(questionResultMapper.selectList(any())).thenReturn(List.of(oldResult));
    CertificationPracticalCandidateResultEntity candidateResult = new CertificationPracticalCandidateResultEntity();
    candidateResult.setId(901L);
    candidateResult.setStatus("completed");
    candidateResult.setTotalScore(80);
    when(candidateResultMapper.selectOne(any())).thenReturn(candidateResult);

    PracticalScoringModels.GenerateView result = service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 21L, 30L));

    assertThat(result.createdTasks()).isEqualTo(3);
    Mockito.verify(scoreMapper).delete(any());
    Mockito.verify(assessorTaskMapper).delete(any());
    Mockito.verify(questionResultMapper).delete(any());
    ArgumentCaptor<CertificationPracticalCandidateResultEntity> resultCaptor =
        ArgumentCaptor.forClass(CertificationPracticalCandidateResultEntity.class);
    Mockito.verify(candidateResultMapper, Mockito.atLeastOnce()).updateById(resultCaptor.capture());
    assertThat(resultCaptor.getAllValues()).anySatisfy(reset -> {
      assertThat(reset.getStatus()).isEqualTo("pending");
      assertThat(reset.getTotalScore()).isNull();
      assertThat(reset.getCompletedQuestionCount()).isZero();
    });
    Mockito.verify(configMapper).updateById(existingConfig);
  }

  @Test
  void linkCandidateRejectsNonThreeMemberGroup() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, true),
        null,
        paper(20, 100),
        null,
        members(2));

    assertThatThrownBy(() -> service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 20L, 30L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("必须且只能配置3名考评员");
  }

  @Test
  void linkCandidateRejectsMismatchedPaperWorkType() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, true),
        null,
        paper(20, 200),
        null,
        members(3));

    assertThatThrownBy(() -> service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 20L, 30L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("实操试卷工种与学员工种不一致");
  }

  @Test
  void linkCandidateRejectsIncompleteCandidateDocuments() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, false),
        null,
        paper(20, 100),
        null,
        members(3));

    assertThatThrownBy(() -> service.linkCandidate(
        1L, 11L, new PracticalScoringModels.LinkCandidateReq(101L, 100L, 20L, 30L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已完成申报材料");
  }

  @Test
  void importCustomPaperParsesScoreRecordWorkbook() throws Exception {
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    doAnswer(invocation -> {
      ExamOperatePaperEntity paper = invocation.getArgument(0);
      paper.setId(900);
      return 1;
    }).when(paperMapper).insert(any(ExamOperatePaperEntity.class));

    PracticalScoringModels.ImportPaperView result = service.importCustomPaper(
        1L,
        new MockMultipartFile(
            "file",
            "score-record.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            scoreRecordWorkbookBytes()));

    ArgumentCaptor<ExamOperatePaperEntity> paperCaptor =
        ArgumentCaptor.forClass(ExamOperatePaperEntity.class);
    ArgumentCaptor<ExamOperatePaperDetailEntity> detailCaptor =
        ArgumentCaptor.forClass(ExamOperatePaperDetailEntity.class);
    Mockito.verify(paperMapper).insert(paperCaptor.capture());
    Mockito.verify(paperDetailMapper).insert(detailCaptor.capture());
    assertThat(result.paperId()).isEqualTo(900L);
    assertThat(result.questions()).isEqualTo(1);
    assertThat(result.scoreItems()).isEqualTo(2);
    assertThat(paperCaptor.getValue().getSource()).isEqualTo(2);
    assertThat(paperCaptor.getValue().getWorkTypeId()).isNull();
    assertThat(paperCaptor.getValue().getTotalPoint()).isEqualTo(5);
    assertThat(detailCaptor.getValue().getTitle()).contains("试题1");
    assertThat(detailCaptor.getValue().getScore()).isEqualByComparingTo("5");
    assertThat(detailCaptor.getValue().getScoringTemplateJson()).contains("安全确认", "设备处理");
  }

  @ParameterizedTest
  @ValueSource(ints = {4, 6, 8})
  void importFullScoreRecordWorkbookAcceptsFourToEightQuestionSheets(int questionCount) throws Exception {
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    doAnswer(invocation -> {
      ExamOperatePaperEntity paper = invocation.getArgument(0);
      paper.setId(901);
      return 1;
    }).when(paperMapper).insert(any(ExamOperatePaperEntity.class));

    PracticalScoringModels.ImportPaperView result = service.importCustomPaper(
        1L,
        new MockMultipartFile(
            "file",
            "full-score-record.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            fullScoreRecordWorkbookBytes(scoresForQuestionCount(questionCount))));

    ArgumentCaptor<ExamOperatePaperEntity> paperCaptor =
        ArgumentCaptor.forClass(ExamOperatePaperEntity.class);
    ArgumentCaptor<ExamOperatePaperDetailEntity> detailCaptor =
        ArgumentCaptor.forClass(ExamOperatePaperDetailEntity.class);
    Mockito.verify(paperMapper).insert(paperCaptor.capture());
    Mockito.verify(paperDetailMapper, Mockito.times(questionCount)).insert(detailCaptor.capture());
    assertThat(result.questions()).isEqualTo(questionCount);
    assertThat(paperCaptor.getValue().getTotalPoint()).isEqualTo(100);
    assertThat(detailCaptor.getAllValues())
        .extracting(ExamOperatePaperDetailEntity::getScore)
        .containsExactlyElementsOf(scoresForQuestionCount(questionCount).stream().map(BigDecimal::new).toList());
  }

  @Test
  void importFullScoreRecordWorkbookRejectsInvalidQuestionCount() throws Exception {
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());

    assertThatThrownBy(() -> service.importCustomPaper(
        1L,
        new MockMultipartFile(
            "file",
            "full-score-record.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            fullScoreRecordWorkbookBytes(List.of(30, 30, 40)))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("4-8");
  }

  @Test
  void importFullScoreRecordWorkbookRejectsNonHundredTotalScore() throws Exception {
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());

    assertThatThrownBy(() -> service.importCustomPaper(
        1L,
        new MockMultipartFile(
            "file",
            "full-score-record.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            fullScoreRecordWorkbookBytes(List.of(20, 20, 20, 30)))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("100");
  }

  @Test
  void myExamsGroupsQuestionTasksByCandidateAndPaper() {
    CertificationAssessorTaskEntity first = task(50L, 9L, 1);
    first.setStatus("submitted");
    first.setTotalScore(new BigDecimal("18"));
    CertificationAssessorTaskEntity second = task(51L, 9L, 0);
    second.setPaperQuestionId(22L);
    second.setTitle("试题2");
    second.setStatus("draft");
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(first, second));
    ExamOperatePaperEntity paper = paper(20, null);
    paper.setTotalPoint(100);
    when(paperMapper.selectBatchIds(any())).thenReturn(List.of(paper));

    List<PracticalScoringModels.AssessorExamListItem> result = service.myExams(null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).totalQuestions()).isEqualTo(2);
    assertThat(result.get(0).submittedQuestions()).isEqualTo(1);
    assertThat(result.get(0).totalPoint()).isEqualTo(100);
    assertThat(result.get(0).currentScore()).isEqualByComparingTo("18");
    assertThat(result.get(0).status()).isEqualTo("draft");
  }

  @Test
  void myExamReturnsQuestionDetailsForOwnedCandidateExam() {
    CertificationAssessorTaskEntity first = task(50L, 9L, 1);
    CertificationAssessorTaskEntity second = task(51L, 9L, 0);
    second.setPaperQuestionId(22L);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(first, second));
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素", "10", "10", "1")));
    ExamOperatePaperEntity paper = paper(20, null);
    paper.setTotalPoint(100);
    when(paperMapper.selectBatchIds(any())).thenReturn(List.of(paper));

    PracticalScoringModels.AssessorExamDetail result = service.myExam(1L, 11L);

    assertThat(result.exam().totalQuestions()).isEqualTo(2);
    assertThat(result.exam().totalPoint()).isEqualTo(100);
    assertThat(result.questions()).hasSize(2);
    assertThat(result.questions()).extracting(question -> question.task().id()).containsExactly(50L, 51L);
  }

  @Test
  void myExamRejectsUnownedCandidateExam() {
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.myExam(1L, 11L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("无权访问");
  }

  private CertificationAssessorTaskEntity task(Long id, Long assessorId, int version) {
    CertificationAssessorTaskEntity task = new CertificationAssessorTaskEntity();
    task.setId(id);
    task.setTaskId(1L);
    task.setCandidateId(11L);
    task.setPaperId(20L);
    task.setPaperQuestionId(21L);
    task.setAssessorId(assessorId);
    task.setStatus("assigned");
    task.setVersionNo(version);
    task.setRevisionNo(1);
    task.setCandidateSnapshotJson("""
        {"candidateNo":"K001","candidateName":"张三","workTypeName":"电工","levelName":"高级"}
        """);
    task.setQuestionSnapshotJson("""
        {"content":"完成接线","studentPrepare":"工具","equipmentPrepare":"电机"}
        """);
    return task;
  }

  private void stubCandidateLinkBase(
      CertificationCandidateEntity firstCandidate,
      CertificationCandidateEntity secondCandidate,
      ExamOperatePaperEntity firstPaper,
      ExamOperatePaperEntity secondPaper,
      List<CertificationTaskAssessorEntity> members
  ) {
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(scopeMapper.selectById(101L)).thenReturn(scope());
    when(groupMapper.selectById(30L)).thenReturn(group());
    when(taskAssessorMapper.selectList(any())).thenReturn(members);
    when(assessorMapper.selectList(any())).thenAnswer(invocation -> members.stream().map(member -> {
      CertificationInfoAssessorEntity row = new CertificationInfoAssessorEntity();
      row.setId(member.getAssessorId());
      row.setUserId(1000L + member.getAssessorId());
      row.setAssessorName(member.getAssessorName());
      row.setAuditStatus("approved");
      return row;
    }).toList());
    when(candidateMapper.selectById(firstCandidate.getId())).thenReturn(firstCandidate);
    if (secondCandidate != null) {
      when(candidateMapper.selectById(secondCandidate.getId())).thenReturn(secondCandidate);
    }
    when(paperMapper.selectById(firstPaper.getId())).thenReturn(firstPaper);
    if (secondPaper != null) {
      when(paperMapper.selectById(secondPaper.getId())).thenReturn(secondPaper);
    }
    when(paperDetailMapper.selectList(any())).thenReturn(List.of(question(20)));
    when(configMapper.selectOne(any())).thenReturn(null);
    when(candidateResultMapper.selectOne(any())).thenReturn(null);
  }

  private CertificationTaskEntity taskEntity() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setExamDate(LocalDate.of(2026, 6, 16));
    return task;
  }

  private CertificationTaskScopeEntity scope() {
    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setId(101L);
    scope.setTaskId(1L);
    scope.setWorkTypeId(100L);
    scope.setWorkTypeName("电工");
    scope.setLevelCode("L3");
    scope.setLevelName("高级");
    scope.setSortOrder(1);
    return scope;
  }

  private CertificationTaskAssessorGroupEntity group() {
    CertificationTaskAssessorGroupEntity group = new CertificationTaskAssessorGroupEntity();
    group.setId(30L);
    group.setTaskId(1L);
    group.setGroupName("第一考评组");
    group.setLeaderAssessorId(201L);
    return group;
  }

  private List<CertificationTaskAssessorEntity> members(int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(index -> {
          CertificationTaskAssessorEntity member = new CertificationTaskAssessorEntity();
          member.setId(300L + index);
          member.setTaskId(1L);
          member.setGroupId(30L);
          member.setAssessorId(201L + index);
          member.setAssessorName("考评员" + (index + 1));
          member.setSortOrder(index + 1);
          return member;
        })
        .toList();
  }

  private CertificationCandidateEntity candidate(Long id, String name, Long workTypeId, boolean documentComplete) {
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(id);
    candidate.setTaskId(1L);
    candidate.setCandidateName(name);
    candidate.setCandidateNo("K" + id);
    candidate.setWorkTypeId(workTypeId);
    candidate.setWorkTypeName("电工");
    candidate.setLevelCode("L3");
    candidate.setLevelName("高级");
    candidate.setStatus("active");
    candidate.setDocumentComplete(documentComplete);
    return candidate;
  }

  private ExamOperatePaperEntity paper(Integer id, Integer workTypeId) {
    ExamOperatePaperEntity paper = new ExamOperatePaperEntity();
    paper.setId(id);
    paper.setWorkTypeId(workTypeId);
    paper.setStatus(1);
    paper.setTitle("实操试卷" + id);
    paper.setLevel("L3");
    return paper;
  }

  private ExamOperatePaperDetailEntity question(Integer paperId) {
    ExamOperatePaperDetailEntity question = new ExamOperatePaperDetailEntity();
    question.setId(501);
    question.setPaperId(paperId);
    question.setTitle("接线操作");
    question.setScore(new BigDecimal("10"));
    question.setStatus(1);
    question.setScoringTemplateJson("""
        [{"itemNo":1,"itemName":"规范","maxScore":10,"criterion":"按规范完成","maxDeduction":10,"deductionStep":1}]
        """);
    return question;
  }

  private CertificationAssessorTaskEntity submittedTask(BigDecimal score) {
    CertificationAssessorTaskEntity task = task(60L, 7L, 1);
    task.setStatus("submitted");
    task.setTotalScore(score);
    return task;
  }

  private CertificationAssessorTaskScoreEntity score(
      int itemNo,
      String name,
      String maxScore,
      String maxDeduction,
      String step
  ) {
    CertificationAssessorTaskScoreEntity score = new CertificationAssessorTaskScoreEntity();
    score.setId((long) itemNo);
    score.setAssessorTaskId(50L);
    score.setStepNo(itemNo);
    score.setStepName(name);
    score.setCriterion("按规范完成");
    score.setMaxScore(new BigDecimal(maxScore));
    score.setMaxDeduction(new BigDecimal(maxDeduction));
    score.setDeductionStep(new BigDecimal(step));
    score.setDeductedScore(BigDecimal.ZERO);
    score.setAwardedScore(new BigDecimal(maxScore));
    return score;
  }

  private byte[] scoreRecordWorkbookBytes() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Sheet1");
      cell(sheet, 0, 0, "表1:评分记录表");
      cell(sheet, 2, 0, "炼铜实操评分记录");
      cell(sheet, 3, 3, "试题1、浸出交接工作中问题的处理(5分)");
      cell(sheet, 3, 18, "本题得分合计");
      cell(sheet, 4, 4, "浸出交接工作中问题的处理");
      cell(sheet, 5, 4, "安全确认");
      cell(sheet, 5, 6, "设备处理");
      cell(sheet, 7, 4, "未检查现场安全，扣分");
      cell(sheet, 7, 6, "未按规程处理设备，扣分");
      cell(sheet, 8, 4, "2");
      cell(sheet, 8, 6, "3");
      cell(sheet, 9, 4, "1");
      cell(sheet, 9, 6, "1");
      cell(sheet, 10, 4, "扣分");
      cell(sheet, 10, 5, "得分");
      cell(sheet, 10, 6, "扣分");
      cell(sheet, 10, 7, "得分");
      sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 17));
      sheet.addMergedRegion(new CellRangeAddress(4, 4, 4, 17));
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private List<Integer> scoresForQuestionCount(int questionCount) {
    return switch (questionCount) {
      case 4 -> List.of(25, 25, 25, 25);
      case 6 -> List.of(20, 40, 20, 10, 5, 5);
      case 8 -> List.of(10, 10, 10, 10, 15, 15, 15, 15);
      default -> throw new IllegalArgumentException("unsupported question count");
    };
  }

  private byte[] fullScoreRecordWorkbookBytes(List<Integer> questionScores) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet summary = workbook.createSheet("评分汇总表");
      cell(summary, 0, 0, "评分记录汇总表");
      for (int i = 0; i < questionScores.size(); i++) {
        Sheet sheet = workbook.createSheet("评分记录表 (" + (i + 1) + ")");
        int score = questionScores.get(i);
        cell(sheet, 0, 0, "表" + (i + 1) + ":评分记录表");
        cell(sheet, 2, 0, "20260610GSXT实操评分记录");
        cell(sheet, 3, 0, "序号");
        cell(sheet, 3, 1, "考生姓名");
        cell(sheet, 3, 2, "准考证号");
        cell(sheet, 3, 3, "试题" + (i + 1) + "、实操题目" + (i + 1) + "(" + score + "分)");
        cell(sheet, 3, 8, "本题得分合计");
        cell(sheet, 4, 4, "实操题目" + (i + 1));
        cell(sheet, 5, 4, "评分要素" + (i + 1));
        cell(sheet, 7, 4, "未按标准完成，扣分");
        cell(sheet, 8, 4, String.valueOf(score));
        cell(sheet, 9, 4, "1");
        cell(sheet, 10, 4, "扣分");
        cell(sheet, 10, 5, "得分");
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 7));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 4, 7));
      }
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private void cell(Sheet sheet, int rowIndex, int columnIndex, String value) {
    var row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }
    row.createCell(columnIndex).setCellValue(value);
  }
}
