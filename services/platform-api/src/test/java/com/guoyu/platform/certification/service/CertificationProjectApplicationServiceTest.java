package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.dto.CertificationDocumentReq;
import com.guoyu.platform.certification.dto.CertificationDocumentReviewReq;
import com.guoyu.platform.certification.dto.CertificationMemberReq;
import com.guoyu.platform.certification.dto.CertificationRiskReq;
import com.guoyu.platform.certification.dto.CertificationScheduleItemReq;
import com.guoyu.platform.certification.dto.CertificationTaskCreateReq;
import com.guoyu.platform.certification.dto.CertificationTaskScopeReq;
import com.guoyu.platform.certification.dto.CertificationWbsItemReq;
import com.guoyu.platform.certification.entity.CertificationRiskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskMemberEntity;
import com.guoyu.platform.certification.entity.CertificationTaskPhaseEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCharterGoalMapper;
import com.guoyu.platform.certification.mapper.CertificationRiskMapper;
import com.guoyu.platform.certification.mapper.CertificationScheduleItemMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMemberMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.mapper.CertificationWbsItemMapper;
import com.guoyu.platform.certification.workflow.CertificationDocumentWorkflowService;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CertificationProjectApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationTaskPhaseMapper phaseMapper = Mockito.mock(CertificationTaskPhaseMapper.class);
  private final CertificationTaskDocumentMapper documentMapper = Mockito.mock(CertificationTaskDocumentMapper.class);
  private final CertificationCharterGoalMapper charterGoalMapper = Mockito.mock(CertificationCharterGoalMapper.class);
  private final CertificationTaskMemberMapper memberMapper = Mockito.mock(CertificationTaskMemberMapper.class);
  private final CertificationWbsItemMapper wbsMapper = Mockito.mock(CertificationWbsItemMapper.class);
  private final CertificationScheduleItemMapper scheduleMapper = Mockito.mock(CertificationScheduleItemMapper.class);
  private final CertificationRiskMapper riskMapper = Mockito.mock(CertificationRiskMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationDocumentWorkflowService workflowService = Mockito.mock(CertificationDocumentWorkflowService.class);

  private CertificationProjectApplicationService service;

  @BeforeEach
  void setUp() {
    authenticate(1001L, "business-user");
    service = new CertificationProjectApplicationService(
        taskMapper,
        phaseMapper,
        documentMapper,
        charterGoalMapper,
        memberMapper,
        wbsMapper,
        scheduleMapper,
        riskMapper,
        scopeMapper,
        workflowService
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createTaskPersistsMultipleScopes() {
    CertificationTaskCreateReq req = new CertificationTaskCreateReq(
        "RD-2026-SCOPE",
        "多范围认定任务",
        "周楠",
        "测试企业",
        "with_question_bank",
        java.util.List.of(
            new CertificationTaskScopeReq("with_question_bank", "电工", "senior", "高级", 40),
            new CertificationTaskScopeReq("with_question_bank", "焊工", "intermediate", "中级", 40)
        ),
        80,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 20),
        LocalDate.of(2026, 5, 20),
        "manual",
        "备注"
    );

    CertificationTaskEntity insertedTask = new CertificationTaskEntity();
    insertedTask.setId(88001L);
    doAnswer(invocation -> {
      CertificationTaskEntity entity = invocation.getArgument(0);
      entity.setId(88001L);
      return 1;
    }).when(taskMapper).insert(any(CertificationTaskEntity.class));
    when(taskMapper.selectById(88001L)).thenReturn(insertedTask);

    CertificationTaskScopeEntity scopeA = new CertificationTaskScopeEntity();
    scopeA.setId(880011L);
    scopeA.setTaskId(88001L);
    scopeA.setWorkTypeName("电工");
    scopeA.setLevelCode("senior");
    scopeA.setLevelName("高级");
    CertificationTaskScopeEntity scopeB = new CertificationTaskScopeEntity();
    scopeB.setId(880012L);
    scopeB.setTaskId(88001L);
    scopeB.setWorkTypeName("焊工");
    scopeB.setLevelCode("intermediate");
    scopeB.setLevelName("中级");
    when(scopeMapper.selectList(any())).thenReturn(java.util.List.of(scopeA, scopeB));

    var result = service.createTask(req);

    ArgumentCaptor<CertificationTaskScopeEntity> scopeCaptor = ArgumentCaptor.forClass(CertificationTaskScopeEntity.class);
    verify(scopeMapper, Mockito.times(2)).insert(scopeCaptor.capture());
    assertThat(scopeCaptor.getAllValues())
        .extracting(CertificationTaskScopeEntity::getWorkTypeName)
        .containsExactly("电工", "焊工");
    assertThat(result.scopes()).hasSize(2);
  }

  @Test
  void createTaskSeedsFourPhasesAndFiveDocuments() {
    CertificationTaskCreateReq req = new CertificationTaskCreateReq(
        "RD-2026-009",
        "企业专项认定协同任务",
        "周楠",
        "企业专项认定协同任务",
        "with_question_bank",
        java.util.List.of(new CertificationTaskScopeReq("with_question_bank", "电工", "senior", "高级", 80)),
        80,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 20),
        LocalDate.of(2026, 5, 20),
        "manual",
        "备注"
    );

    service.createTask(req);

    verify(taskMapper).insert(any(CertificationTaskEntity.class));
    // 四阶段流程：项目创建 → 认定筹备 → 认定归档 → 认定完成
    verify(phaseMapper, Mockito.times(4)).insert(any(CertificationTaskPhaseEntity.class));
    verify(documentMapper, Mockito.times(5)).insert(any(CertificationTaskDocumentEntity.class));
  }

  @Test
  void saveMembersRejectsInvalidPhone() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationMemberReq req = new CertificationMemberReq(
        null,
        "李敏",
        "material_specialist",
        "资料专员",
        "运营部",
        "整理资料",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 10),
        "full",
        BigDecimal.valueOf(80),
        "12345",
        "备注"
    );

    assertThatThrownBy(() -> service.saveMembers(1L, java.util.List.of(req)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("成员手机号格式不正确");
  }

  @Test
  void saveMembersAcceptsNullRoleCode() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationMemberReq req = new CertificationMemberReq(
        null,
        "李敏",
        null,
        "资料专员",
        "运营部",
        "整理资料",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 10),
        "full",
        BigDecimal.valueOf(80),
        null,
        "备注"
    );

    service.saveMembers(1L, java.util.List.of(req));

    ArgumentCaptor<CertificationTaskMemberEntity> captor = ArgumentCaptor.forClass(CertificationTaskMemberEntity.class);
    verify(memberMapper).insert(captor.capture());
    assertThat(captor.getValue().getCoreRole()).isFalse();
  }

  @Test
  void saveWbsRejectsNegativeCost() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationWbsItemReq req = new CertificationWbsItemReq(
        null,
        "1",
        "planning",
        "规划",
        "任务立项与策划",
        "确认范围",
        BigDecimal.valueOf(8),
        BigDecimal.ONE,
        "task_manager",
        null,
        "会议室",
        BigDecimal.valueOf(-1),
        1,
        null,
        "pending"
    );

    assertThatThrownBy(() -> service.saveWbsItems(1L, java.util.List.of(req)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("WBS 工时、费用和工期不能为负数");
  }

  @Test
  void saveScheduleRejectsStartDateAfterEndDate() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationScheduleItemReq req = new CertificationScheduleItemReq(
        null,
        1,
        null,
        LocalDate.of(2026, 5, 10),
        LocalDate.of(2026, 5, 1),
        "与企业确认考试时间",
        "确认时间",
        true,
        false,
        null,
        null
    );

    assertThatThrownBy(() -> service.saveScheduleItems(1L, java.util.List.of(req)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("进度事项开始日期不能晚于结束日期");
  }

  @Test
  void saveRiskDerivesRiskLevelFromProbabilityAndImpact() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationRiskReq req = new CertificationRiskReq(
        null,
        "资料未按期补齐",
        "high",
        "medium",
        "提前催办",
        null,
        null,
        "张晓峰",
        LocalDate.of(2026, 5, 18),
        "备注"
    );

    service.saveRisks(1L, java.util.List.of(req));

    ArgumentCaptor<CertificationRiskEntity> captor = ArgumentCaptor.forClass(CertificationRiskEntity.class);
    verify(riskMapper).insert(captor.capture());
    assertThat(captor.getValue().getRiskLevelCode()).isEqualTo("medium_high");
    assertThat(captor.getValue().getRiskLevelScore()).isEqualTo(4);
  }

  @Test
  void submitDocumentRequiresExistingCurrentDocument() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(documentMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.submitDocument(1L, "charter"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("任务文档不存在");
  }

  @Test
  void completeDocumentWorkflowFromNewTaskThroughRejectionRevisionReviewAndArchive() {
    CertificationTaskCreateReq createReq = new CertificationTaskCreateReq(
        "RD-2026-WF",
        "Certification workflow task",
        "Alice",
        "Acme",
        "with_question_bank",
        List.of(new CertificationTaskScopeReq("with_question_bank", "Electrician", "senior", "Senior", 12)),
        12,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 10),
        LocalDate.of(2026, 6, 10),
        "manual",
        "workflow"
    );
    CertificationTaskEntity task = existingTask();
    task.setId(1L);
    task.setTaskNo("RD-2026-WF");
    doAnswer(invocation -> {
      CertificationTaskEntity entity = invocation.getArgument(0);
      entity.setId(1L);
      return 1;
    }).when(taskMapper).insert(any(CertificationTaskEntity.class));
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(scopeMapper.selectList(any())).thenReturn(List.of());

    service.createTask(createReq);

    CertificationTaskDocumentEntity document = currentDocument();
    when(documentMapper.selectOne(any())).thenReturn(document);
    when(documentMapper.selectList(any())).thenAnswer(invocation -> List.of(document));
    when(workflowService.startReview(document)).thenReturn("pi-first-round", "pi-second-round");

    service.saveDocument(1L, "charter", new CertificationDocumentReq("first draft", "{\"version\":1}", "draft"));

    var submitted = service.submitDocument(1L, "charter");
    assertThat(submitted.status()).isEqualTo("initial_reviewing");
    assertThat(document.getProcessInstanceId()).isEqualTo("pi-first-round");
    assertThat(document.getSubmittedBy()).isEqualTo(1001L);

    authenticate(2001L, "supervisor");
    var initialApproved = service.initialReviewDocument(
        1L,
        "charter",
        new CertificationDocumentReviewReq(true, "initial ok")
    );
    assertThat(initialApproved.status()).isEqualTo("final_reviewing");

    var finalRejected = service.finalReviewDocument(
        1L,
        "charter",
        new CertificationDocumentReviewReq(false, "needs more evidence")
    );
    assertThat(finalRejected.status()).isEqualTo("final_rejected");
    assertThat(document.getRejectedBy()).isEqualTo(2001L);
    assertThat(document.getRejectReason()).isEqualTo("needs more evidence");

    authenticate(1001L, "business-user");
    var revised = service.saveDocument(1L, "charter", new CertificationDocumentReq("revised draft", "{\"version\":2}", "fixed"));
    assertThat(revised.status()).isEqualTo("draft");

    var resubmitted = service.submitDocument(1L, "charter");
    assertThat(resubmitted.status()).isEqualTo("initial_reviewing");
    assertThat(document.getProcessInstanceId()).isEqualTo("pi-second-round");

    authenticate(2001L, "supervisor");
    service.initialReviewDocument(1L, "charter", new CertificationDocumentReviewReq(true, "initial ok again"));
    var archived = service.finalReviewDocument(
        1L,
        "charter",
        new CertificationDocumentReviewReq(true, "final ok")
    );

    assertThat(archived.status()).isEqualTo("archived");
    assertThat(document.getApprovedBy()).isEqualTo(2001L);
    assertThat(document.getRejectedBy()).isNull();
    verify(workflowService, Mockito.times(2)).startReview(document);
    verify(workflowService, Mockito.times(2)).completeInitialReview(any(), Mockito.anyBoolean(), any());
    verify(workflowService, Mockito.times(2)).completeFinalReview(any(), Mockito.anyBoolean(), any());
  }

  @Test
  void finalReviewAllProjectDocumentsAdvancesTaskToPreparation() {
    CertificationTaskEntity task = existingTask();
    task.setStatus("pending_review");
    task.setCurrentPhase(CertificationTaskPhaseSupport.TASK_CREATION);
    when(taskMapper.selectById(1L)).thenReturn(task);

    CertificationTaskDocumentEntity current = currentDocument();
    current.setStatus("final_reviewing");
    when(documentMapper.selectOne(any())).thenReturn(current);
    java.util.List<CertificationTaskDocumentEntity> documents = new java.util.ArrayList<>();
    documents.add(current);
    for (int i = 0; i < 4; i++) {
      CertificationTaskDocumentEntity document = currentDocument();
      document.setId(100L + i);
      document.setDocumentType("doc-" + i);
      document.setStatus("archived");
      documents.add(document);
    }
    when(documentMapper.selectList(any())).thenReturn(documents);
    when(phaseMapper.selectOne(any())).thenReturn(new CertificationTaskPhaseEntity());

    service.finalReviewDocument(1L, "charter", new CertificationDocumentReviewReq(true, "final ok"));

    ArgumentCaptor<CertificationTaskEntity> taskCaptor = ArgumentCaptor.forClass(CertificationTaskEntity.class);
    verify(taskMapper, Mockito.atLeastOnce()).updateById(taskCaptor.capture());
    assertThat(current.getStatus()).isEqualTo("archived");
    // 资料全部归档后，任务推进到“认定筹备阶段”：current_phase=preparation，status=in_progress
    assertThat(taskCaptor.getAllValues().stream()
        .map(CertificationTaskEntity::getCurrentPhase)
        .filter(CertificationTaskPhases.PREPARATION::equals)
        .findFirst())
        .contains(CertificationTaskPhases.PREPARATION);
    assertThat(taskCaptor.getAllValues().stream()
        .map(CertificationTaskEntity::getStatus)
        .filter(CertificationTaskStatuses.IN_PROGRESS::equals)
        .findFirst())
        .contains(CertificationTaskStatuses.IN_PROGRESS);
  }

  private CertificationTaskEntity existingTask() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setTaskNo("RD-2026-001");
    task.setTaskName("2026 年职业技能认定任务");
    task.setStatus(CertificationTaskStatuses.IN_PROGRESS);
    task.setCurrentPhase(CertificationTaskPhaseSupport.TASK_CREATION);
    return task;
  }

  @SuppressWarnings("unused")
  private CertificationTaskDocumentEntity currentDocument() {
    CertificationTaskDocumentEntity document = new CertificationTaskDocumentEntity();
    document.setId(10L);
    document.setTaskId(1L);
    document.setDocumentType("charter");
    document.setStatus("draft");
    document.setIsCurrent(1);
    return document;
  }

  private void authenticate(Long userId, String username) {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        userId,
        1L,
        username,
        username,
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of(
            "certification.project.document.submit",
            "certification.project.document.initialReview",
            "certification.project.document.finalReview",
            "certification.project.document.workflowView"
        )
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }
}
