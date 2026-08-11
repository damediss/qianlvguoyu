package com.guoyu.platform.certification.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
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
  private final AssessorWorkbenchContextService workbenchContextService =
      Mockito.mock(AssessorWorkbenchContextService.class);
  private PracticalScoringApplicationService service;

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "考评员", "platform",
        List.of("assessor"), List.of("certification.practical.score"),"asd@qq.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new PracticalScoringApplicationService(
        taskMapper, scopeMapper, candidateMapper, groupMapper, taskAssessorMapper, assessorMapper,
        assessorTaskMapper, scoreMapper, configMapper, questionResultMapper, candidateResultMapper,
        historyMapper, paperMapper, paperDetailMapper, new ObjectMapper(), startService, workbenchContextService);
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setUserId(100L);
    assessor.setAuditStatus("approved");
    when(assessorMapper.selectOne(any())).thenReturn(assessor);
    when(workbenchContextService.current()).thenReturn(
        new PracticalScoringModels.AssessorWorkbenchContext(false, null, null, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void submitCalculatesOneDecimalQuestionScoreAndCandidateTotal() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 0);
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(scoreMapper.selectList(any())).thenReturn(List.of(score(1, "要素一", "10", "10", "0.1")));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(
        submittedTask(new BigDecimal("9.6")),
        submittedTask(new BigDecimal("9.7")),
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
        0, List.of(new PracticalScoringModels.SaveScoreItemReq(1, new BigDecimal("0.2"), null)), null));

    assertThat(questionResult.getAverageScore()).isEqualByComparingTo("9.7");
    assertThat(questionResult.getFinalScore()).isEqualByComparingTo("9.7");
    assertThat(candidateResult.getTotalScore()).isEqualByComparingTo("9.7");
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
    assertThat(questionResult.getFinalScore()).isEqualByComparingTo("23.0");
    assertThat(candidateResult.getStatus()).isEqualTo("completed");
    assertThat(candidateResult.getTotalScore()).isEqualByComparingTo("23.0");
  }

  @Test
  void adminTaskReturnsScoringStandardContentSeparatelyFromElementName() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 2);
    CertificationAssessorTaskScoreEntity first = score(1, "中缝均匀添加20-25公分冰晶石", "5", "5", "1");
    first.setId(101L);
    first.setCriterion("不放冰晶石扣5分");
    CertificationAssessorTaskScoreEntity second = score(2, "中缝均匀添加20-25公分冰晶石", "5", "5", "1");
    second.setId(102L);
    second.setCriterion("冰晶石不均匀扣5分");
    when(assessorTaskMapper.selectById(50L)).thenReturn(task);
    when(startService.session(1L, 11L)).thenReturn(null);
    when(scoreMapper.selectList(any())).thenReturn(List.of(first, second));

    PracticalScoringModels.AssessorTaskDetail detail = service.adminTask(50L);

    assertThat(detail.scoreItems()).hasSize(2);
    assertThat(detail.scoreItems()).extracting(PracticalScoringModels.ScoreItemView::itemName)
        .containsExactly("中缝均匀添加20-25公分冰晶石", "中缝均匀添加20-25公分冰晶石");
    assertThat(detail.scoreItems()).extracting(PracticalScoringModels.ScoreItemView::scoringStandardContent)
        .containsExactly("不放冰晶石扣5分", "冰晶石不均匀扣5分");
    assertThat(detail.scoreItems()).extracting(PracticalScoringModels.ScoreItemView::scoringStandardId)
        .containsExactly(101L, 102L);
  }

  @Test
  void adjustScoreMatchesScoreItemIdBeforeItemNo() {
    CertificationAssessorTaskEntity task = task(50L, 9L, 2);
    task.setStatus("submitted");
    task.setTotalScore(new BigDecimal("20"));
    CertificationAssessorTaskScoreEntity first = score(1, "重复评分要素", "10", "10", "1");
    first.setId(101L);
    first.setCriterion("评分标准一");
    CertificationAssessorTaskScoreEntity second = score(2, "重复评分要素", "10", "10", "1");
    second.setId(102L);
    second.setCriterion("评分标准二");
    when(assessorTaskMapper.lockById(1L, 50L)).thenReturn(task);
    when(scoreMapper.selectList(any())).thenReturn(List.of(first, second));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(
        submittedTask(new BigDecimal("16")),
        submittedTask(new BigDecimal("17")),
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

    service.adjustScore(
        50L,
        new PracticalScoringModels.AdjustScoreReq(
            2,
            List.of(
                new PracticalScoringModels.SaveScoreItemReq(102L, 1, new BigDecimal("3"), "按第二条标准扣分"),
                new PracticalScoringModels.SaveScoreItemReq(101L, 2, new BigDecimal("1"), "按第一条标准扣分")),
            "管理端复核后修正",
            "三人分差复核调整"));

    assertThat(first.getDeductedScore()).isEqualByComparingTo("1");
    assertThat(first.getRemark()).isEqualTo("按第一条标准扣分");
    assertThat(second.getDeductedScore()).isEqualByComparingTo("3");
    assertThat(second.getRemark()).isEqualTo("按第二条标准扣分");
    assertThat(task.getTotalScore()).isEqualByComparingTo("16");
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
  void batchLinkCandidatesUsesOnePaperAndGroupForAllEligibleCandidates() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, true),
        candidate(12L, "李四", 100L, true),
        paper(20, 100),
        null,
        members(3));
    when(scopeMapper.selectList(any())).thenReturn(List.of(scope()));
    when(configMapper.selectList(any())).thenReturn(List.of());
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of());
    AtomicLong ids = new AtomicLong(1000);
    doAnswer(invocation -> {
      CertificationPracticalScoringConfigEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      return 1;
    }).when(configMapper).insert(any(CertificationPracticalScoringConfigEntity.class));

    PracticalScoringModels.BatchGenerateView result = service.linkCandidates(
        1L, new PracticalScoringModels.BatchLinkCandidatesReq(List.of(11L, 12L), 20L, 30L));

    assertThat(result.createdTasks()).isEqualTo(6);
    assertThat(result.configuredCandidates()).isEqualTo(2);
    assertThat(result.skippedCandidates()).isZero();
    ArgumentCaptor<CertificationPracticalScoringConfigEntity> captor =
        ArgumentCaptor.forClass(CertificationPracticalScoringConfigEntity.class);
    Mockito.verify(configMapper, Mockito.times(2)).insert(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(CertificationPracticalScoringConfigEntity::getPaperId)
        .containsOnly(20L);
    assertThat(captor.getAllValues())
        .extracting(CertificationPracticalScoringConfigEntity::getGroupId)
        .containsOnly(30L);
  }

  @Test
  void batchLinkCandidatesSkipsAlreadyConfiguredCandidates() {
    stubCandidateLinkBase(
        candidate(11L, "张三", 100L, true),
        candidate(12L, "李四", 100L, true),
        paper(20, 100),
        null,
        members(3));
    when(scopeMapper.selectList(any())).thenReturn(List.of(scope()));
    CertificationPracticalScoringConfigEntity existingConfig = new CertificationPracticalScoringConfigEntity();
    existingConfig.setTaskId(1L);
    existingConfig.setCandidateId(11L);
    when(configMapper.selectList(any())).thenReturn(List.of(existingConfig));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of());

    PracticalScoringModels.BatchGenerateView result = service.linkCandidates(
        1L, new PracticalScoringModels.BatchLinkCandidatesReq(List.of(11L, 12L), 20L, 30L));

    assertThat(result.configuredCandidates()).isEqualTo(1);
    assertThat(result.skippedCandidates()).isEqualTo(1);
    Mockito.verify(candidateMapper, Mockito.never()).selectById(11L);
    Mockito.verify(candidateMapper).selectById(12L);
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
    candidateResult.setTotalScore(new BigDecimal("80.0"));
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
  void cancelCandidateGeneratedTasksDeletesScoringDataAndConfiguration() {
    CertificationTaskEntity certificationTask = new CertificationTaskEntity();
    certificationTask.setId(1L);
    CertificationCandidateEntity candidate = candidate(11L, "张三", 100L, true);
    CertificationAssessorTaskEntity assessorTask = task(70L, 201L, 0);
    assessorTask.setPaperQuestionId(21L);
    when(taskMapper.selectById(1L)).thenReturn(certificationTask);
    when(candidateMapper.selectById(11L)).thenReturn(candidate);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(assessorTask));

    service.cancelCandidateGeneratedTasks(1L, 11L);

    Mockito.verify(startService).cancelUnstartedSession(1L, 11L);
    Mockito.verify(scoreMapper).delete(any());
    Mockito.verify(assessorTaskMapper).delete(any());
    Mockito.verify(questionResultMapper).delete(any());
    Mockito.verify(candidateResultMapper).delete(any());
    Mockito.verify(configMapper).delete(any());
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

  @Test
  void markCandidateAbsentGeneratesZeroScoreDataWhenNoScoringTasksExist() {
    CertificationCandidateEntity candidate = candidate(11L, "Absent candidate", 100L, true);
    CertificationPracticalScoringConfigEntity config = absentConfig();
    List<CertificationAssessorTaskEntity> taskStore = new ArrayList<>();
    List<CertificationAssessorTaskScoreEntity> scoreStore = new ArrayList<>();
    List<CertificationPracticalQuestionResultEntity> questionResultStore = new ArrayList<>();
    AtomicReference<CertificationPracticalCandidateResultEntity> candidateResultStore = new AtomicReference<>();
    AtomicLong ids = new AtomicLong(1000L);

    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(candidateMapper.selectById(11L)).thenReturn(candidate);
    when(configMapper.selectOne(any())).thenReturn(null);
    when(configMapper.selectList(any())).thenReturn(List.of(config));
    when(groupMapper.selectById(30L)).thenReturn(group());
    when(taskAssessorMapper.selectList(any())).thenReturn(members(3));
    when(assessorMapper.selectList(any())).thenReturn(List.of(assessor(201L), assessor(202L), assessor(203L)));
    when(paperDetailMapper.selectList(any())).thenReturn(List.of(question(20)));
    when(assessorTaskMapper.selectList(any())).thenAnswer(invocation -> List.copyOf(taskStore));
    when(scoreMapper.selectList(any())).thenAnswer(invocation -> List.copyOf(scoreStore));
    when(questionResultMapper.selectCount(any())).thenReturn(0L);
    when(questionResultMapper.selectList(any())).thenAnswer(invocation -> List.copyOf(questionResultStore));
    when(candidateResultMapper.selectOne(any())).thenAnswer(invocation -> candidateResultStore.get());
    when(startService.markAbsent(1L, 11L)).thenReturn(absentSession(11L));
    doAnswer(invocation -> {
      CertificationAssessorTaskEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      taskStore.add(entity);
      return 1;
    }).when(assessorTaskMapper).insert(any(CertificationAssessorTaskEntity.class));
    doAnswer(invocation -> {
      CertificationAssessorTaskScoreEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      scoreStore.add(entity);
      return 1;
    }).when(scoreMapper).insert(any(CertificationAssessorTaskScoreEntity.class));
    doAnswer(invocation -> {
      CertificationPracticalQuestionResultEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      questionResultStore.add(entity);
      return 1;
    }).when(questionResultMapper).insert(any(CertificationPracticalQuestionResultEntity.class));
    doAnswer(invocation -> {
      CertificationPracticalCandidateResultEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      candidateResultStore.set(entity);
      return 1;
    }).when(candidateResultMapper).insert(any(CertificationPracticalCandidateResultEntity.class));

    PracticalScoringModels.StartSessionView session = service.markCandidateAbsent(1L, 11L);

    assertThat(session.status()).isEqualTo("absent");
    assertThat(candidate.getStatus()).isEqualTo("absent");
    assertThat(taskStore).hasSize(3);
    assertThat(taskStore).allSatisfy(task -> {
      assertThat(task.getStatus()).isEqualTo("absent");
      assertThat(task.getTotalScore()).isEqualByComparingTo("0.0");
    });
    assertThat(scoreStore).hasSize(3);
    assertThat(scoreStore).allSatisfy(score -> {
      assertThat(score.getDeductedScore()).isEqualByComparingTo("0.0");
      assertThat(score.getAwardedScore()).isEqualByComparingTo("0.0");
      assertThat(score.getScore()).isEqualByComparingTo("0.0");
    });
    assertThat(questionResultStore).hasSize(1);
    assertThat(questionResultStore.get(0).getStatus()).isEqualTo("absent");
    assertThat(questionResultStore.get(0).getAverageScore()).isEqualByComparingTo("0.0");
    assertThat(questionResultStore.get(0).getFinalScore()).isEqualByComparingTo("0.0");
    assertThat(candidateResultStore.get().getStatus()).isEqualTo("absent");
    assertThat(candidateResultStore.get().getTotalScore()).isEqualByComparingTo("0.0");
  }

  @Test
  void markCandidateAbsentRejectsCompletedResult() {
    CertificationCandidateEntity candidate = candidate(11L, "Completed candidate", 100L, true);
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setStatus("completed");
    result.setTotalScore(new BigDecimal("90.0"));
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(candidateMapper.selectById(11L)).thenReturn(candidate);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of());
    when(candidateResultMapper.selectOne(any())).thenReturn(result);

    assertThatThrownBy(() -> service.markCandidateAbsent(1L, 11L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("completed practical result");
  }

  @Test
  void markCandidateAbsentRejectsSubmittedScore() {
    CertificationCandidateEntity candidate = candidate(11L, "Submitted candidate", 100L, true);
    CertificationAssessorTaskEntity submitted = submittedTask(new BigDecimal("9.0"));
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(candidateMapper.selectById(11L)).thenReturn(candidate);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(submitted));

    assertThatThrownBy(() -> service.markCandidateAbsent(1L, 11L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("submitted practical scores");
  }

  @Test
  void completeTaskReleasesFinishedCandidateGroupsAndKeepsRemainingAssignmentStatus() {
    CertificationAssessorTaskEntity scoringTask = task(50L, 9L, 0);
    scoringTask.setGroupId(30L);
    CertificationTaskAssessorEntity releasedBinding = members(1).get(0);
    releasedBinding.setAssessorId(9L);
    CertificationTaskAssessorEntity remainingBinding = members(1).get(0);
    remainingBinding.setAssessorId(9L);
    remainingBinding.setWorkStatus("assigned");
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setCandidateId(11L);
    result.setStatus("completed");
    CertificationInfoAssessorEntity assessor = assessor(9L);
    assessor.setWorkStatus("preparing");

    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(scoringTask));
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(result));
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(releasedBinding), List.of(remainingBinding));
    when(assessorMapper.selectById(9L)).thenReturn(assessor);

    PracticalScoringModels.CompleteTaskView view = service.completeTask(1L);

    assertThat(view.releasedGroups()).isEqualTo(1);
    assertThat(view.releasedAssessors()).isEqualTo(1);
    assertThat(assessor.getWorkStatus()).isEqualTo("assigned");
    verify(taskAssessorMapper).delete(any());
    verify(assessorMapper).updateById(assessor);
  }

  @Test
  void completeTaskRejectsUnsettledCandidateScores() {
    CertificationAssessorTaskEntity scoringTask = task(50L, 9L, 0);
    scoringTask.setGroupId(30L);
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(scoringTask));
    when(candidateResultMapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.completeTask(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("实操评分未完成");
  }

  @Test
  void progressUsesDecimalAverageTotalInsteadOfLegacyIntegerTotal() {
    CertificationCandidateEntity candidate = candidate(11L, "Candidate", 100L, true);
    candidate.setCandidateNo("C-001");
    CertificationPracticalQuestionResultEntity firstQuestion = new CertificationPracticalQuestionResultEntity();
    firstQuestion.setCandidateId(11L);
    firstQuestion.setPaperQuestionId(1L);
    firstQuestion.setStatus("completed");
    firstQuestion.setAverageScore(new BigDecimal("9.7"));
    firstQuestion.setFinalScore(new BigDecimal("10"));
    CertificationPracticalQuestionResultEntity secondQuestion = new CertificationPracticalQuestionResultEntity();
    secondQuestion.setCandidateId(11L);
    secondQuestion.setPaperQuestionId(2L);
    secondQuestion.setStatus("completed");
    secondQuestion.setAverageScore(new BigDecimal("8.3"));
    secondQuestion.setFinalScore(new BigDecimal("8"));
    CertificationPracticalCandidateResultEntity candidateResult = new CertificationPracticalCandidateResultEntity();
    candidateResult.setCandidateId(11L);
    candidateResult.setStatus("completed");
    candidateResult.setTotalScore(new BigDecimal("18"));
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of());
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(candidateResult));
    when(questionResultMapper.selectList(any())).thenReturn(List.of(firstQuestion, secondQuestion));
    when(startService.session(1L, 11L)).thenReturn(null);

    PracticalScoringModels.ProgressView view = service.progress(1L);

    assertThat(view.candidates()).hasSize(1);
    assertThat(view.candidates().get(0).totalScore()).isEqualByComparingTo("18.0");
  }

  @Test
  void exportSummaryUsesTemplateAndCopiesTheSeventhRowStyle() throws Exception {
    CertificationTaskEntity task = taskEntity();
    task.setTaskNo("PLAN-001");
    task.setTaskName("Template export");
    task.setCompanyName("Test site");
    task.setExamDate(LocalDate.of(2026, 6, 23));
    CertificationCandidateEntity firstCandidate = candidate(11L, "Candidate 1", 100L, true);
    firstCandidate.setCandidateNo("C-001");
    firstCandidate.setAdmissionTicketNo("2607134501032300013");
    CertificationCandidateEntity secondCandidate = candidate(12L, "Candidate 2", 100L, true);
    secondCandidate.setCandidateNo("C-002");
    secondCandidate.setAdmissionTicketNo("2607134501032300014");
    List<CertificationPracticalQuestionResultEntity> questionResults = exportQuestionResults(
        List.of(firstCandidate, secondCandidate));
    List<CertificationAssessorTaskEntity> scoreTasks = exportScoreTasks(
        List.of(firstCandidate, secondCandidate));
    List<CertificationTaskAssessorEntity> assessors = members(3);
    assessors.get(0).setAssessorName("Signer A");
    assessors.get(1).setAssessorName("Signer B");
    assessors.get(2).setAssessorName("Signer C");

    when(taskMapper.selectById(1L)).thenReturn(task);
    when(candidateMapper.selectList(any())).thenReturn(List.of(firstCandidate, secondCandidate));
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(
        exportCandidateResult(firstCandidate.getId(), "100.0"),
        exportAbsentCandidateResult(secondCandidate.getId())));
    when(questionResultMapper.selectList(any())).thenReturn(questionResults);
    when(assessorTaskMapper.selectList(any())).thenReturn(scoreTasks);
    when(scoreMapper.selectList(any())).thenReturn(exportScoreRows(scoreTasks));
    when(taskAssessorMapper.selectList(any())).thenReturn(assessors);
    when(startService.session(1L, firstCandidate.getId())).thenReturn(completedSession(firstCandidate.getId()));
    when(startService.session(1L, secondCandidate.getId())).thenReturn(absentSession(secondCandidate.getId()));

    ExportResultDTO result = service.exportSummary(1L, List.of(firstCandidate.getId(), secondCandidate.getId()));

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.data()))) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(0))).isEqualTo("评分记录汇总表");
      assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("计划编码：PLAN-001");
      assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(3))).isEqualTo("考 点 ：Test site");
      assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(0))).isEqualTo("计划名称：Template export");
      assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(3))).isEqualTo("考试时间：2026-06-23");
      assertThat(formatter.formatCellValue(sheet.getRow(3).getCell(0)))
          .doesNotContain("Signer A", "Signer B", "Signer C");
      assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("Candidate 1");
      assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("Candidate 2");
      assertThat(sheet.getRow(6).getCell(2).getStringCellValue()).isEqualTo("2607134501032300013");
      assertThat(sheet.getRow(7).getCell(2).getStringCellValue()).isEqualTo("2607134501032300014");
      assertThat(sheet.getRow(6).getCell(3).getNumericCellValue()).isEqualTo(11.5D);
      assertThat(sheet.getRow(6).getCell(4).getNumericCellValue()).isEqualTo(12D);
      assertThat(sheet.getRow(6).getCell(5).getNumericCellValue()).isEqualTo(13D);
      assertThat(sheet.getRow(6).getCell(39).getNumericCellValue()).isEqualTo(222D);
      assertThat(sheet.getRow(7).getCell(4).getNumericCellValue()).isEqualTo(0D);
      assertThat(sheet.getRow(7).getCell(8).getNumericCellValue()).isEqualTo(0D);
      assertThat(sheet.getRow(7).getCell(39).getNumericCellValue()).isEqualTo(210D);
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(3))).isEqualTo("11.5");
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(4))).isEqualTo("12");
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(6))).isEqualTo("12");
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(7))).isEqualTo("100");
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(8))).isEqualTo("12");
      assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(39))).isEqualTo("222");
      assertSummaryScoreColumnWidths(sheet, 6);
      assertThat(sheet.getRow(7).getHeight()).isEqualTo(sheet.getRow(6).getHeight());
      assertThat(sheet.getRow(7).getCell(0).getCellStyle().getIndex())
          .isEqualTo(sheet.getRow(6).getCell(0).getCellStyle().getIndex());
      assertThat(sheet.getRow(7).getCell(39).getCellStyle().getIndex())
          .isEqualTo(sheet.getRow(6).getCell(39).getCellStyle().getIndex());
      assertThat(sheet.getPrintSetup().getLandscape()).isTrue();
      assertThat(sheet.getPrintSetup().getScale()).isEqualTo((short) 75);
      assertThat(sheet.getMargin(Sheet.LeftMargin)).isEqualTo(0.25D);
      assertThat(sheet.getMergedRegions()).anyMatch(range -> "A1:AN1".equals(range.formatAsString()));
      try (Workbook templateWorkbook = WorkbookFactory.create(
          new ClassPathResource("templates/评分记录汇总表模板.xlsx").getInputStream())) {
        Sheet templateSheet = templateWorkbook.getSheetAt(0);
        assertThat(sheet.getColumnWidth(0)).isEqualTo(templateSheet.getColumnWidth(0));
        assertThat(sheet.getRow(0).getHeight()).isEqualTo(templateSheet.getRow(0).getHeight());
        assertThat(formatter.formatCellValue(sheet.getRow(3).getCell(0)))
            .isEqualTo(formatter.formatCellValue(templateSheet.getRow(3).getCell(0)));
      }
      assertThat(workbook.getNumberOfSheets()).isEqualTo(19);
      Sheet firstRecordSheet = workbook.getSheet("评分记录表1-Signer A");
      assertThat(firstRecordSheet).isNotNull();
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(0).getCell(0))).isEqualTo("表1:评分记录表");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(2).getCell(0))).contains("考评员  签字：Signer A");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(3).getCell(3))).isEqualTo("试题1、试题1(10分)");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(5).getCell(4))).isEqualTo("评分项1");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(11).getCell(1))).isEqualTo("Candidate 1");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(11).getCell(2)))
          .isEqualTo("2607134501032300013");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(11).getCell(4))).isEqualTo("0.7");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(11).getCell(5))).isEqualTo("9.3");
      assertThat(formatter.formatCellValue(firstRecordSheet.getRow(11).getCell(6))).isEqualTo("11.5");
      Sheet secondAssessorSheet = workbook.getSheet("评分记录表1-Signer B");
      assertThat(secondAssessorSheet).isNotNull();
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(2).getCell(0))).contains("考评员  签字：Signer B");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(11).getCell(4))).isEqualTo("1.5");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(11).getCell(5))).isEqualTo("8.5");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(11).getCell(6))).isEqualTo("12.0");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(12).getCell(1))).isEqualTo("Candidate 2");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(12).getCell(4))).isEqualTo("0.0");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(12).getCell(5))).isEqualTo("0.0");
      assertThat(formatter.formatCellValue(secondAssessorSheet.getRow(12).getCell(6))).isEqualTo("0.0");
      Sheet thirdAssessorSheet = workbook.getSheet("评分记录表1-Signer C");
      assertThat(thirdAssessorSheet).isNotNull();
      assertThat(formatter.formatCellValue(thirdAssessorSheet.getRow(11).getCell(1))).isEqualTo("Candidate 1");
      assertThat(formatter.formatCellValue(thirdAssessorSheet.getRow(12).getCell(1))).isBlank();
      Sheet expandedRecordSheet = workbook.getSheetAt(7);
      assertThat(formatter.formatCellValue(expandedRecordSheet.getRow(5).getCell(18)))
          .isEqualTo("Long grouped factor name for capacity expansion and row height");
      assertThat(formatter.formatCellValue(expandedRecordSheet.getRow(7).getCell(18)))
          .isEqualTo("Long criterion text that should wrap after the sheet grows beyond the original template capacity");
      assertThat(expandedRecordSheet.getColumnWidth(18)).isEqualTo(expandedRecordSheet.getColumnWidth(16));
      assertThat(expandedRecordSheet.getColumnWidth(19)).isEqualTo(expandedRecordSheet.getColumnWidth(17));
      assertThat(formatter.formatCellValue(expandedRecordSheet.getRow(3).getCell(20)))
          .isNotBlank();
      assertThat(formatter.formatCellValue(expandedRecordSheet.getRow(11).getCell(20))).isEqualTo("31.0");
      assertThat(expandedRecordSheet.getRow(5).getCell(18).getCellStyle().getWrapText()).isTrue();
      assertThat(expandedRecordSheet.getRow(7).getCell(18).getCellStyle().getWrapText()).isTrue();
      assertThat(expandedRecordSheet.getRow(5).getHeightInPoints())
          .isGreaterThan(expandedRecordSheet.getRow(6).getHeightInPoints());
      assertThat(expandedRecordSheet.getRow(7).getHeightInPoints())
          .isGreaterThan(expandedRecordSheet.getRow(8).getHeightInPoints());
    }
  }

  @Test
  void exportSummaryExpandsPlanInfoRowsForWrappedText() throws Exception {
    CertificationTaskEntity task = taskEntity();
    task.setTaskNo("RD-202607060001-广西百色广投银海铝业有限责任公司职业技能等级认定超长计划编码");
    task.setTaskName("2026年度广西百色广投银海铝业有限责任公司铝电解操作工高级工职业技能等级认定实操考试超长计划名称");
    task.setCompanyName("广西百色广投银海铝业有限责任公司超长考点名称");
    task.setExamDate(LocalDate.of(2026, 7, 9));
    CertificationCandidateEntity candidate = candidate(11L, "Candidate 1", 100L, true);
    candidate.setCandidateNo("C-001");
    List<CertificationPracticalQuestionResultEntity> questionResults = exportQuestionResults(List.of(candidate));
    List<CertificationAssessorTaskEntity> scoreTasks = exportScoreTasks(List.of(candidate));
    List<CertificationTaskAssessorEntity> assessors = members(1);
    assessors.get(0).setAssessorName("Signer A");

    when(taskMapper.selectById(1L)).thenReturn(task);
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(exportCandidateResult(candidate.getId(), "100.0")));
    when(questionResultMapper.selectList(any())).thenReturn(questionResults);
    when(assessorTaskMapper.selectList(any())).thenReturn(scoreTasks);
    when(scoreMapper.selectList(any())).thenReturn(exportScoreRows(scoreTasks));
    when(taskAssessorMapper.selectList(any())).thenReturn(assessors);

    ExportResultDTO result = service.exportSummary(1L, List.of(candidate.getId()));

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.data()));
        Workbook templateWorkbook = WorkbookFactory.create(
            new ClassPathResource("templates/评分记录汇总表模板.xlsx").getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      Sheet templateSheet = templateWorkbook.getSheetAt(0);
      assertThat(sheet.getRow(1).getHeightInPoints()).isGreaterThan(templateSheet.getRow(1).getHeightInPoints());
      assertThat(sheet.getRow(2).getHeightInPoints()).isGreaterThan(templateSheet.getRow(2).getHeightInPoints());
      assertThat(sheet.getRow(1).getCell(0).getCellStyle().getWrapText()).isTrue();
      assertThat(sheet.getRow(2).getCell(0).getCellStyle().getWrapText()).isTrue();
      assertThat(sheet.getRow(1).getCell(3).getCellStyle().getWrapText()).isTrue();
      assertThat(sheet.getRow(2).getCell(3).getCellStyle().getWrapText()).isTrue();
    }
  }

  @Test
  void exportScoreRecordSheetsUseDynamicSinglePageColumns() throws Exception {
    CertificationTaskEntity task = taskEntity();
    task.setTaskNo("PLAN-DYNAMIC");
    task.setTaskName("Dynamic score record");
    task.setCompanyName("Dynamic site");
    task.setExamDate(LocalDate.of(2026, 7, 6));
    CertificationCandidateEntity candidate = candidate(11L, "Candidate Dynamic", 100L, true);
    candidate.setCandidateNo("CD-001");
    List<Integer> standardCounts = List.of(5, 7, 8, 9, 10);
    List<CertificationPracticalQuestionResultEntity> questionResults =
        dynamicExportQuestionResults(candidate, standardCounts.size());
    List<CertificationAssessorTaskEntity> scoreTasks = dynamicExportScoreTasks(candidate, standardCounts);
    List<CertificationTaskAssessorEntity> assessors = members(1);
    assessors.get(0).setAssessorName("Signer A");

    when(taskMapper.selectById(1L)).thenReturn(task);
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(exportCandidateResult(candidate.getId(), "29.0")));
    when(questionResultMapper.selectList(any())).thenReturn(questionResults);
    when(assessorTaskMapper.selectList(any())).thenReturn(scoreTasks);
    when(scoreMapper.selectList(any())).thenReturn(dynamicExportScoreRows(scoreTasks, standardCounts));
    when(taskAssessorMapper.selectList(any())).thenReturn(assessors);
    when(startService.session(1L, candidate.getId())).thenReturn(completedSession(candidate.getId()));

    ExportResultDTO result = service.exportSummary(1L, List.of(candidate.getId()));

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.data()))) {
      assertScoreRecordLayout(workbook, 1, "Signer A", 5, "E:N", "O");
      assertScoreRecordLayout(workbook, 2, "Signer A", 7, "E:R", "S");
      assertScoreRecordLayout(workbook, 3, "Signer A", 8, "E:T", "U");
      Sheet nineStandardSheet = assertScoreRecordLayout(workbook, 4, "Signer A", 9, "E:V", "W");
      assertScoreRecordLayout(workbook, 5, "Signer A", 10, "E:X", "Y");
      assertMerged(nineStandardSheet, "E6:H6");
      assertMerged(nineStandardSheet, "E7:H7");
      assertMerged(nineStandardSheet, "I6:J6");
      assertMerged(nineStandardSheet, "I7:J7");
      assertMerged(nineStandardSheet, "K6:P6");
      assertMerged(nineStandardSheet, "K7:P7");
      assertMerged(nineStandardSheet, "Q6:R6");
      assertMerged(nineStandardSheet, "Q7:R7");
      assertMerged(nineStandardSheet, "S6:V6");
      assertMerged(nineStandardSheet, "S7:V7");
    }
  }

  private Sheet assertScoreRecordLayout(
      Workbook workbook,
      int questionNo,
      String assessorName,
      int standardCount,
      String expectedScoreArea,
      String expectedTotalColumn
  ) {
    Sheet sheet = workbook.getSheet("评分记录表" + questionNo + "-" + assessorName);
    assertThat(sheet).isNotNull();
    int scoreStartColumn = 4;
    int scoreEndColumn = scoreStartColumn + standardCount * 2 - 1;
    int totalColumn = scoreStartColumn + standardCount * 2;
    String scoreStartName = CellReference.convertNumToColString(scoreStartColumn);
    String scoreEndName = CellReference.convertNumToColString(scoreEndColumn);
    String totalColumnName = CellReference.convertNumToColString(totalColumn);
    assertThat(scoreStartName + ":" + scoreEndName).isEqualTo(expectedScoreArea);
    assertThat(totalColumnName).isEqualTo(expectedTotalColumn);
    DataFormatter formatter = new DataFormatter();
    assertThat(formatter.formatCellValue(sheet.getRow(3).getCell(totalColumn))).isEqualTo("本题得分合计");
    assertMerged(sheet, "A1:" + totalColumnName + "1");
    assertMerged(sheet, "A2:" + totalColumnName + "2");
    assertMerged(sheet, "A3:" + totalColumnName + "3");
    assertMerged(sheet, "D4:" + scoreEndName + "4");
    assertMerged(sheet, "E5:" + scoreEndName + "5");
    assertMerged(sheet, totalColumnName + "4:" + totalColumnName + "10");
    for (int itemIndex = 0; itemIndex < standardCount; itemIndex++) {
      String start = CellReference.convertNumToColString(scoreStartColumn + itemIndex * 2);
      String end = CellReference.convertNumToColString(scoreStartColumn + itemIndex * 2 + 1);
      assertMerged(sheet, start + "8:" + end + "8");
      assertMerged(sheet, start + "9:" + end + "9");
      assertMerged(sheet, start + "10:" + end + "10");
      assertNotMerged(sheet, start + "11:" + end + "11");
    }
    assertThat(sheet.getPrintSetup().getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
    assertThat(sheet.getPrintSetup().getLandscape()).isTrue();
    assertThat(sheet.getPrintSetup().getFitWidth()).isEqualTo((short) 1);
    assertThat(sheet.getPrintSetup().getFitHeight()).isEqualTo((short) 0);
    assertThat(sheet.getFitToPage()).isTrue();
    assertThat(sheet.getHorizontallyCenter()).isTrue();
    assertThat(workbook.getPrintArea(workbook.getSheetIndex(sheet)))
        .contains("$A$1:$" + totalColumnName + "$19");
    assertThat(sheet.getRow(7).getCell(4).getCellStyle().getWrapText()).isTrue();
    assertThat(sheet.getColumnWidth(4)).isEqualTo(expectedScoreColumnWidth(standardCount) * 256);
    assertThat(sheet.getColumnWidth(totalColumn)).isEqualTo(8 * 256);
    return sheet;
  }

  private int expectedScoreColumnWidth(int standardCount) {
    if (standardCount <= 5) {
      return 7;
    }
    if (standardCount <= 7) {
      return 6;
    }
    return 5;
  }

  private void assertSummaryScoreColumnWidths(Sheet sheet, int questionCount) {
    int firstQuestionColumn = 3;
    int columnsPerQuestion = 6;
    for (int questionIndex = 0; questionIndex < questionCount; questionIndex++) {
      int startColumn = firstQuestionColumn + questionIndex * columnsPerQuestion;
      assertThat(sheet.getColumnWidth(startColumn)).isEqualTo(excelColumnWidth(3.2D));
      assertThat(sheet.getColumnWidth(startColumn + 1)).isEqualTo(excelColumnWidth(3.2D));
      assertThat(sheet.getColumnWidth(startColumn + 2)).isEqualTo(excelColumnWidth(3.2D));
      assertThat(sheet.getColumnWidth(startColumn + 3)).isEqualTo(excelColumnWidth(4.8D));
      assertThat(sheet.getColumnWidth(startColumn + 4)).isEqualTo(excelColumnWidth(4D));
      assertThat(sheet.getColumnWidth(startColumn + 5)).isEqualTo(excelColumnWidth(4.8D));
    }
    int totalColumn = firstQuestionColumn + questionCount * columnsPerQuestion;
    assertThat(sheet.getColumnWidth(totalColumn)).isEqualTo(excelColumnWidth(6D));
  }

  private int excelColumnWidth(double characters) {
    return (int) Math.round(characters * 256D);
  }

  private void assertMerged(Sheet sheet, String ref) {
    assertThat(sheet.getMergedRegions())
        .extracting(CellRangeAddress::formatAsString)
        .contains(ref);
  }

  private void assertNotMerged(Sheet sheet, String ref) {
    assertThat(sheet.getMergedRegions())
        .extracting(CellRangeAddress::formatAsString)
        .doesNotContain(ref);
  }

  private List<CertificationPracticalQuestionResultEntity> dynamicExportQuestionResults(
      CertificationCandidateEntity candidate,
      int questionCount
  ) {
    return java.util.stream.IntStream.rangeClosed(1, questionCount)
        .mapToObj(questionIndex -> {
          CertificationPracticalQuestionResultEntity result = new CertificationPracticalQuestionResultEntity();
          result.setTaskId(1L);
          result.setCandidateId(candidate.getId());
          result.setPaperQuestionId((long) questionIndex);
          result.setAverageScore(new BigDecimal(questionIndex));
          result.setFinalScore(new BigDecimal(questionIndex));
          result.setStatus("completed");
          return result;
        }).toList();
  }

  private List<CertificationAssessorTaskEntity> dynamicExportScoreTasks(
      CertificationCandidateEntity candidate,
      List<Integer> standardCounts
  ) {
    return java.util.stream.IntStream.rangeClosed(1, standardCounts.size())
        .mapToObj(questionIndex -> {
          CertificationAssessorTaskEntity task = new CertificationAssessorTaskEntity();
          task.setId(9000L + questionIndex);
          task.setTaskId(1L);
          task.setCandidateId(candidate.getId());
          task.setPaperQuestionId((long) questionIndex);
          task.setAssessorId(201L);
          task.setTitle("动态试题" + questionIndex);
          task.setTotalScore(new BigDecimal(standardCounts.get(questionIndex - 1)));
          task.setStatus("submitted");
          return task;
        }).toList();
  }

  private List<CertificationAssessorTaskScoreEntity> dynamicExportScoreRows(
      List<CertificationAssessorTaskEntity> tasks,
      List<Integer> standardCounts
  ) {
    return tasks.stream().flatMap(task -> {
      int standardCount = standardCounts.get(task.getPaperQuestionId().intValue() - 1);
      return java.util.stream.IntStream.rangeClosed(1, standardCount).mapToObj(itemNo -> {
        CertificationAssessorTaskScoreEntity score = score(
            itemNo,
            dynamicStepName(standardCount, itemNo),
            "1",
            "1",
            "0.5");
        score.setId(task.getId() * 100 + itemNo);
        score.setAssessorTaskId(task.getId());
        score.setStepNo(itemNo);
        score.setCriterion("较长评分标准内容 " + itemNo + "，用于验证自动换行和两列合并。");
        score.setDeductedScore(BigDecimal.ZERO);
        score.setAwardedScore(BigDecimal.ONE);
        return score;
      });
    }).toList();
  }

  private String dynamicStepName(int standardCount, int itemNo) {
    if (standardCount == 9) {
      if (itemNo <= 2) {
        return "评分要素1";
      }
      if (itemNo == 3) {
        return "评分要素2";
      }
      if (itemNo <= 6) {
        return "评分要素3";
      }
      if (itemNo == 7) {
        return "评分要素4";
      }
      return "评分要素5";
    }
    return "评分要素" + ((itemNo + 1) / 2);
  }

  private CertificationPracticalCandidateResultEntity exportCandidateResult(Long candidateId, String totalScore) {
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setTaskId(1L);
    result.setCandidateId(candidateId);
    result.setStatus("completed");
    result.setTotalScore(new BigDecimal(totalScore));
    return result;
  }

  private CertificationPracticalCandidateResultEntity exportAbsentCandidateResult(Long candidateId) {
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setTaskId(1L);
    result.setCandidateId(candidateId);
    result.setStatus("absent");
    result.setTotalScore(BigDecimal.ZERO);
    return result;
  }

  private List<CertificationPracticalQuestionResultEntity> exportQuestionResults(
      List<CertificationCandidateEntity> candidates
  ) {
    return candidates.stream().flatMap(candidate -> java.util.stream.IntStream.rangeClosed(1, 6)
        .mapToObj(questionIndex -> {
          CertificationPracticalQuestionResultEntity result = new CertificationPracticalQuestionResultEntity();
          result.setTaskId(1L);
          result.setCandidateId(candidate.getId());
          result.setPaperQuestionId((long) questionIndex);
          BigDecimal averageScore = new BigDecimal(questionIndex * 10 + 2).setScale(1);
          result.setAverageScore(averageScore);
          result.setFinalScore(averageScore);
          if (candidate.getId().equals(12L) && questionIndex == 1) {
            result.setAverageScore(null);
            result.setFinalScore(null);
            result.setStatus("pending");
          } else {
            result.setStatus("completed");
          }
          return result;
        })).toList();
  }

  private List<CertificationAssessorTaskEntity> exportScoreTasks(
      List<CertificationCandidateEntity> candidates
  ) {
    return candidates.stream().flatMap(candidate -> java.util.stream.IntStream.rangeClosed(1, 6)
        .boxed()
        .flatMap(questionIndex -> List.of(3, 2, 1).stream().map(assessorIndex -> {
          CertificationAssessorTaskEntity task = new CertificationAssessorTaskEntity();
          task.setId(candidate.getId() * 1000 + questionIndex * 10L + assessorIndex);
          task.setTaskId(1L);
          task.setCandidateId(candidate.getId());
          task.setPaperQuestionId(questionIndex.longValue());
          task.setAssessorId(200L + assessorIndex);
          task.setTitle("试题" + questionIndex);
          BigDecimal totalScore = new BigDecimal(questionIndex * 10 + assessorIndex);
          if (candidate.getId().equals(11L) && questionIndex == 1 && assessorIndex == 1) {
            totalScore = new BigDecimal("11.5");
          }
          task.setTotalScore(totalScore);
          task.setStatus("submitted");
          return task;
        }))
        .filter(task -> !(task.getCandidateId().equals(12L)
            && task.getPaperQuestionId().equals(1L)
            && task.getAssessorId().equals(203L)))
        .peek(task -> {
          if (task.getCandidateId().equals(12L)
              && task.getPaperQuestionId().equals(1L)
              && task.getAssessorId().equals(202L)) {
            task.setStatus("assigned");
          }
        })).toList();
  }

  private List<CertificationAssessorTaskScoreEntity> exportScoreRows(
      List<CertificationAssessorTaskEntity> tasks
  ) {
    return tasks.stream().flatMap(task -> {
      int itemCount = task.getPaperQuestionId().equals(3L) ? 8 : 1;
      return java.util.stream.IntStream.rangeClosed(1, itemCount).mapToObj(itemNo -> {
      CertificationAssessorTaskScoreEntity score = score(1, "评分项1", "10", "10", "1");
      score.setAssessorTaskId(task.getId());
      score.setStepNo(itemNo);
      score.setId(task.getId() * 100 + itemNo);
      if (task.getPaperQuestionId().equals(3L)) {
        score.setStepName(itemNo == 8
            ? "Long grouped factor name for capacity expansion and row height"
            : "Factor " + itemNo);
        score.setCriterion(itemNo == 8
            ? "Long criterion text that should wrap after the sheet grows beyond the original template capacity"
            : "criterion " + itemNo);
      }
      BigDecimal deducted = switch (String.valueOf(task.getAssessorId())) {
        case "201" -> new BigDecimal("0.7");
        case "202" -> new BigDecimal("1.5");
        default -> new BigDecimal("2.0");
      };
      score.setDeductedScore(deducted);
      score.setAwardedScore(new BigDecimal("10").subtract(deducted));
      return score;
      });
    }).toList();
  }

  private PracticalScoringModels.StartSessionView completedSession(Long candidateId) {
    return new PracticalScoringModels.StartSessionView(
        1L, 1L, candidateId, 20L, "completed", true, 3, 3, true, 1, null, null, null);
  }

  private PracticalScoringModels.StartSessionView absentSession(Long candidateId) {
    return new PracticalScoringModels.StartSessionView(
        1L, 1L, candidateId, 20L, "absent", false, 0, 3, false, 1, null, null, null);
  }

  private CertificationPracticalScoringConfigEntity absentConfig() {
    CertificationPracticalScoringConfigEntity config = new CertificationPracticalScoringConfigEntity();
    config.setId(900L);
    config.setTaskId(1L);
    config.setScopeId(101L);
    config.setWorkTypeId(100L);
    config.setWorkTypeName("鐢靛伐");
    config.setLevelCode("L3");
    config.setLevelName("楂樼骇");
    config.setPaperId(20L);
    config.setGroupId(30L);
    config.setStatus("configured");
    return config;
  }

  private CertificationInfoAssessorEntity assessor(Long id) {
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(id);
    assessor.setUserId(1000L + id);
    assessor.setAssessorName("Assessor " + id);
    assessor.setAuditStatus("approved");
    return assessor;
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
