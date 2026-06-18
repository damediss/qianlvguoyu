package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.dto.PreparationAssessorDispatchReq;
import com.guoyu.platform.certification.dto.PreparationCandidateImportReq;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialGenerateReq;
import com.guoyu.platform.certification.dto.PreparationTaskAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskEquipmentSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffSaveReq;
import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationMaterialTemplateEntity;
import com.guoyu.platform.certification.entity.CertificationExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationApplicationMaterialBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationApplicationMaterialItemMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorGroupWorktypeMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorNotificationMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationMaterialTemplateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskEquipmentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskChecklistItemMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CertificationPreparationApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationTaskPhaseMapper phaseMapper = Mockito.mock(CertificationTaskPhaseMapper.class);
  private final CertificationCandidateImportBatchMapper importBatchMapper =
      Mockito.mock(CertificationCandidateImportBatchMapper.class);
  private final CertificationCandidateImportErrorMapper importErrorMapper =
      Mockito.mock(CertificationCandidateImportErrorMapper.class);
  private final CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationMaterialTemplateMapper templateMapper = Mockito.mock(CertificationMaterialTemplateMapper.class);
  private final CertificationCandidateDocumentMapper documentMapper =
      Mockito.mock(CertificationCandidateDocumentMapper.class);
  private final CertificationApplicationMaterialBatchMapper materialBatchMapper =
      Mockito.mock(CertificationApplicationMaterialBatchMapper.class);
  private final CertificationApplicationMaterialItemMapper materialItemMapper =
      Mockito.mock(CertificationApplicationMaterialItemMapper.class);
  private final CertificationExamSiteMapper examSiteMapper = Mockito.mock(CertificationExamSiteMapper.class);
  private final CertificationTaskExamSiteMapper taskExamSiteMapper = Mockito.mock(CertificationTaskExamSiteMapper.class);
  private final CertificationTaskExamStaffMapper taskExamStaffMapper = Mockito.mock(CertificationTaskExamStaffMapper.class);
  private final CertificationTaskChecklistItemMapper taskChecklistItemMapper =
      Mockito.mock(CertificationTaskChecklistItemMapper.class);
  private final CertificationInfoExamStaffMapper infoExamStaffMapper = Mockito.mock(CertificationInfoExamStaffMapper.class);
  private final CertificationTaskEquipmentMapper taskEquipmentMapper = Mockito.mock(CertificationTaskEquipmentMapper.class);
  private final CertificationInfoExamEquipmentMapper infoEquipmentMapper = Mockito.mock(CertificationInfoExamEquipmentMapper.class);
  private final CertificationAssessorMapper assessorMapper = Mockito.mock(CertificationAssessorMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = Mockito.mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationAssessorTaskMapper assessorTaskMapper = Mockito.mock(CertificationAssessorTaskMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationAssessorGroupWorktypeMapper groupWorktypeMapper =
      Mockito.mock(CertificationAssessorGroupWorktypeMapper.class);
  private final CertificationInfoAssessorMapper infoAssessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final com.guoyu.platform.file.service.FileApplicationService fileApplicationService =
      Mockito.mock(com.guoyu.platform.file.service.FileApplicationService.class);
  private final CertificationCandidateStagingApplicationService stagingApplicationService =
      Mockito.mock(CertificationCandidateStagingApplicationService.class);
  private final CertificationApplicationFormGenerationService applicationFormGenerationService =
      Mockito.mock(CertificationApplicationFormGenerationService.class);
  private final CertificationImageSourceService imageSourceService =
      Mockito.mock(CertificationImageSourceService.class);

  private CertificationPreparationApplicationService service;

  @BeforeEach
  void setUp() {
    service = new CertificationPreparationApplicationService(
        taskMapper,
        phaseMapper,
        importBatchMapper,
        importErrorMapper,
        candidateMapper,
        templateMapper,
        documentMapper,
        materialBatchMapper,
        materialItemMapper,
        examSiteMapper,
        taskExamSiteMapper,
        taskExamStaffMapper,
        taskChecklistItemMapper,
        infoExamStaffMapper,
        taskEquipmentMapper,
        infoEquipmentMapper,
        assessorMapper,
        groupMapper,
        taskAssessorMapper,
        assessorTaskMapper,
        scopeMapper,
        groupWorktypeMapper,
        infoAssessorMapper,
        fileApplicationService,
        stagingApplicationService,
        applicationFormGenerationService,
        imageSourceService
    );
    when(scopeMapper.selectList(any())).thenReturn(List.of(existingScope()));
  }

  @Test
  void createCandidateRejectsDuplicateIdCardInSameTask() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(candidateMapper.selectCount(any())).thenReturn(1L);

    PreparationCandidateSaveReq req = new PreparationCandidateSaveReq(
        "李敏",
        "450102199001011234",
        "13800001234",
        "senior",
        "高级",
        "manual",
        "鉴定中心",
        "备注"
    );

    assertThatThrownBy(() -> service.createCandidate(1L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("同一任务下身份证号不得重复");
  }

  @Test
  void trainingImportRequiresAuthoritativeSourceIds() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());

    PreparationCandidateImportReq req = new PreparationCandidateImportReq(
        "training",
        null,
        null,
        List.of(),
        null);

    assertThatThrownBy(() -> service.createImportBatch(1L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("请选择要导入的培训学员");
    verify(importBatchMapper, never()).insert(any(CertificationCandidateImportBatchEntity.class));
  }

  @Test
  void createCandidateRejectsLevelMismatchWithTask() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(candidateMapper.selectCount(any())).thenReturn(0L);

    PreparationCandidateSaveReq req = new PreparationCandidateSaveReq(
        "李敏",
        "450102199001011234",
        "13800001234",
        "intermediate",
        "中级",
        "manual",
        "鉴定中心",
        null
    );

    assertThatThrownBy(() -> service.createCandidate(1L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("报考等级必须匹配当前认定任务支持的等级");
  }

  @Test
  void generateApplicationMaterialsRejectsIncompleteRequiredDocuments() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(candidateMapper.selectCount(any())).thenReturn(2L);
    CertificationCandidateEntity candidate1 = new CertificationCandidateEntity();
    candidate1.setId(11L);
    candidate1.setTaskId(1L);
    candidate1.setStatus("active");
    CertificationCandidateEntity candidate2 = new CertificationCandidateEntity();
    candidate2.setId(12L);
    candidate2.setTaskId(1L);
    candidate2.setStatus("active");
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate1, candidate2));
    when(documentMapper.selectCount(any())).thenReturn(1L);

    PreparationMaterialGenerateReq req = new PreparationMaterialGenerateReq("selected", List.of(11L, 12L), "批量生成");

    assertThatThrownBy(() -> service.generateApplicationMaterials(1L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("存在未审核通过的必传资料，不能生成申报材料");
  }

  @Test
  void assignExamStaffMarksInfoStaffAsAssigned() {
    CertificationTaskEntity task = existingTask();
    task.setTenantId(1L);
    when(taskMapper.selectById(1L)).thenReturn(task);
    CertificationInfoExamStaffEntity staff = new CertificationInfoExamStaffEntity();
    staff.setId(20L);
    staff.setStaffName("张考务");
    staff.setPhone("13800000001");
    staff.setAuditStatus("approved");
    staff.setWorkStatus("idle");
    staff.setReferencedCount(0);
    when(infoExamStaffMapper.selectById(20L)).thenReturn(staff);
    when(taskExamStaffMapper.selectByTaskAndStaffIncludingDeleted(1L, 1L, 20L)).thenReturn(null);
    doAnswer(invocation -> {
      CertificationTaskExamStaffEntity entity = invocation.getArgument(0);
      entity.setId(100L);
      return 1;
    }).when(taskExamStaffMapper).insert(any(CertificationTaskExamStaffEntity.class));

    service.assignExamStaff(1L, new PreparationTaskExamStaffSaveReq(20L, "exam_staff", "考务员", "负责人"));

    assertThat(staff.getWorkStatus()).isEqualTo("assigned");
    assertThat(staff.getReferencedCount()).isEqualTo(1);
    verify(infoExamStaffMapper).updateById(staff);
    verify(taskExamStaffMapper).insert(any(CertificationTaskExamStaffEntity.class));
  }

  @Test
  void assignTaskEquipmentMarksInfoEquipmentAsInUse() {
    CertificationTaskEntity task = existingTask();
    task.setTenantId(1L);
    when(taskMapper.selectById(1L)).thenReturn(task);
    CertificationInfoExamEquipmentEntity equipment = infoEquipment("idle");
    when(infoEquipmentMapper.selectById(30L)).thenReturn(equipment);
    when(taskEquipmentMapper.selectCount(any())).thenReturn(0L, 1L);
    doAnswer(invocation -> {
      CertificationTaskEquipmentEntity entity = invocation.getArgument(0);
      entity.setId(200L);
      return 1;
    }).when(taskEquipmentMapper).insert(any(CertificationTaskEquipmentEntity.class));

    var result = service.assignTaskEquipment(1L, new PreparationTaskEquipmentSaveReq(30L, "task equipment"));

    assertThat(result.equipmentId()).isEqualTo(30L);
    assertThat(equipment.getEquipmentStatus()).isEqualTo("in_use");
    assertThat(equipment.getReferencedCount()).isEqualTo(1);
    verify(taskEquipmentMapper).insert(any(CertificationTaskEquipmentEntity.class));
    verify(infoEquipmentMapper).updateById(equipment);
  }

  @Test
  void removeLastTaskEquipmentReleasesInfoEquipmentToIdle() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationTaskEquipmentEntity binding = new CertificationTaskEquipmentEntity();
    binding.setId(200L);
    binding.setTaskId(1L);
    binding.setEquipmentId(30L);
    when(taskEquipmentMapper.selectById(200L)).thenReturn(binding);
    when(taskEquipmentMapper.selectCount(any())).thenReturn(0L);
    CertificationInfoExamEquipmentEntity equipment = infoEquipment("in_use");
    when(infoEquipmentMapper.selectById(30L)).thenReturn(equipment);

    service.removeTaskEquipment(1L, 200L);

    assertThat(equipment.getEquipmentStatus()).isEqualTo("idle");
    assertThat(equipment.getReferencedCount()).isZero();
    verify(taskEquipmentMapper).deleteById(200L);
    verify(infoEquipmentMapper).updateById(equipment);
  }

  @Test
  void assignExamStaffRestoresSoftDeletedBinding() {
    CertificationTaskEntity task = existingTask();
    task.setTenantId(1L);
    when(taskMapper.selectById(1L)).thenReturn(task);
    CertificationInfoExamStaffEntity staff = new CertificationInfoExamStaffEntity();
    staff.setId(20L);
    staff.setStaffName("寮犺€冨姟");
    staff.setPhone("13800000001");
    staff.setAuditStatus("approved");
    staff.setWorkStatus("idle");
    staff.setReferencedCount(0);
    when(infoExamStaffMapper.selectById(20L)).thenReturn(staff);
    CertificationTaskExamStaffEntity existing = new CertificationTaskExamStaffEntity();
    existing.setId(100L);
    existing.setTaskId(1L);
    existing.setStaffId(20L);
    existing.setDeleted(1);
    when(taskExamStaffMapper.selectByTaskAndStaffIncludingDeleted(1L, 1L, 20L)).thenReturn(existing);

    service.assignExamStaff(1L, new PreparationTaskExamStaffSaveReq(20L, "invigilator", "监考员", "remark"));

    assertThat(existing.getDeleted()).isZero();
    assertThat(existing.getStatus()).isEqualTo("configured");
    assertThat(existing.getTaskRole()).isEqualTo("invigilator");
    assertThat(existing.getRoleName()).isEqualTo("监考员");
    assertThat(staff.getWorkStatus()).isEqualTo("assigned");
    assertThat(staff.getReferencedCount()).isEqualTo(1);
    verify(taskExamStaffMapper).restoreSoftDeleted(existing);
    verify(infoExamStaffMapper).updateById(staff);
  }

  @Test
  void appointExamStaffLeaderMarksSelectedStaffAsOnlyLeader() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationTaskExamStaffEntity taskStaff = new CertificationTaskExamStaffEntity();
    taskStaff.setId(100L);
    taskStaff.setTaskId(1L);
    taskStaff.setStaffId(20L);
    taskStaff.setStaffName("寮犺€冨姟");
    taskStaff.setIsLeader(0);
    when(taskExamStaffMapper.selectById(100L)).thenReturn(taskStaff);

    var result = service.appointExamStaffLeader(1L, 100L);

    assertThat(taskStaff.getIsLeader()).isEqualTo(1);
    assertThat(taskStaff.getTaskRole()).isEqualTo("internal_supervisor");
    assertThat(taskStaff.getRoleName()).isEqualTo("内督");
    assertThat(result.isLeader()).isTrue();
    verify(taskExamStaffMapper).clearInternalSupervisorsByTask(1L);
    verify(taskExamStaffMapper).updateById(taskStaff);
  }

  @Test
  void assignExamSiteRejectsArrangedCountGreaterThanSeatCount() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    CertificationExamSiteEntity site = new CertificationExamSiteEntity();
    site.setId(5L);
    site.setSiteName("第一实操考场");
    site.setSiteType("practical");
    site.setAddress("教学楼 A101");
    site.setSeatCount(20);
    site.setStatus("active");
    when(examSiteMapper.selectById(5L)).thenReturn(site);

    PreparationTaskExamSiteSaveReq req = new PreparationTaskExamSiteSaveReq(
        5L,
        "2026-06-18",
        "08:30",
        "11:30",
        "theory",
        30,
        "王强",
        "13900001234",
        "备注"
    );

    assertThatThrownBy(() -> service.assignExamSite(1L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("安排人数不得超过考场容量");
  }

  @Test
  void addGroupMemberRejectsExpiredAssessor() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(groupMapper.selectById(2L)).thenReturn(existingGroup());
    CertificationInfoAssessorEntity assessor = assessor("电工,焊工", LocalDate.of(2026, 6, 1));
    when(infoAssessorMapper.selectById(9L)).thenReturn(assessor);

    PreparationTaskAssessorSaveReq req = new PreparationTaskAssessorSaveReq(9L, "member", "成员", "备注");

    assertThatThrownBy(() -> service.addGroupMember(1L, 2L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("证书已过期的考评员不得分配任务");
  }

  @Test
  void addGroupMemberRejectsAssessorWhenWorkScopeDoesNotCoverTaskWorkType() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(groupMapper.selectById(2L)).thenReturn(existingGroup());
    CertificationInfoAssessorEntity assessor = assessor("焊工", LocalDate.of(2026, 12, 31));
    when(infoAssessorMapper.selectById(9L)).thenReturn(assessor);

    PreparationTaskAssessorSaveReq req = new PreparationTaskAssessorSaveReq(9L, "member", "成员", "备注");

    assertThatThrownBy(() -> service.addGroupMember(1L, 2L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("考评员工种范围必须覆盖当前认定任务工种");
  }

  @Test
  void dispatchGroupCreatesAssignedWorkbenchTask() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(groupMapper.selectById(2L)).thenReturn(existingGroup());
    CertificationTaskAssessorEntity member = new CertificationTaskAssessorEntity();
    member.setId(30L);
    member.setTaskId(1L);
    member.setGroupId(2L);
    member.setAssessorId(9L);
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(member));
    when(infoAssessorMapper.selectById(9L)).thenReturn(assessor("鐢靛伐", LocalDate.of(2026, 12, 31)));
    CertificationInfoAssessorEntity leader = assessor("鐢靛伐", LocalDate.of(2026, 12, 31));
    leader.setId(8L);
    when(infoAssessorMapper.selectById(8L)).thenReturn(leader);

    service.dispatchGroup(1L, 2L, new PreparationAssessorDispatchReq(
        "鐢靛伐",
        "瀹炴搷鑰冭瘎",
        1001L,
        "考评表.xlsx",
        90,
        LocalDate.of(2026, 6, 18),
        null
    ));

    ArgumentCaptor<CertificationAssessorTaskEntity> captor =
        ArgumentCaptor.forClass(CertificationAssessorTaskEntity.class);
    verify(assessorTaskMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("assigned");
  }

  @Test
  void completeCheckReturnsStructuredBlockingItems() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(candidateMapper.selectList(any())).thenReturn(List.of());
    when(documentMapper.selectCount(any())).thenReturn(1L);
    when(materialBatchMapper.selectCount(any())).thenReturn(0L);
    when(taskExamSiteMapper.selectCount(any())).thenReturn(0L);
    when(taskExamStaffMapper.selectCount(any())).thenReturn(0L);
    when(taskEquipmentMapper.selectCount(any())).thenReturn(0L);
    when(groupMapper.selectCount(any())).thenReturn(0L);
    when(assessorTaskMapper.selectCount(any())).thenReturn(0L);

    var result = service.completeCheck(1L);

    assertThat(result.passed()).isFalse();
    assertThat(result.items()).extracting("code")
        .contains(
            "candidate_required",
            "document_approved",
            "material_generated",
            "exam_site_required",
            "exam_staff_required",
            "equipment_required",
            "assessor_group_required",
            "assessor_group_leader",
            "assessor_task_dispatched"
        );
    assertThat(result.items()).filteredOn(item -> "document_approved".equals(item.code()))
        .singleElement()
        .satisfies(item -> {
          assertThat(item.passed()).isFalse();
          assertThat(item.message()).isEqualTo("尚无考生，无法校验必传资料");
        });
    assertThat(result.items()).filteredOn(item -> "assessor_group_leader".equals(item.code()))
        .singleElement()
        .satisfies(item -> {
          assertThat(item.passed()).isFalse();
          assertThat(item.message()).isEqualTo("尚未配置考评组，无法校验组长");
        });
  }

  @Test
  void createCandidateInsertsValidCandidateWithMaskedFields() {
    when(taskMapper.selectById(1L)).thenReturn(existingTask());
    when(candidateMapper.selectCount(any())).thenReturn(0L);
    when(candidateMapper.selectByTaskAndIdCardIncludingDeleted(1L, 1L, "450102199001011234")).thenReturn(null);
    doAnswer(invocation -> {
      CertificationCandidateEntity entity = invocation.getArgument(0);
      entity.setId(10001L);
      return 1;
    }).when(candidateMapper).insert(any(CertificationCandidateEntity.class));
    when(templateMapper.selectList(any())).thenReturn(candidateDocTemplates());
    when(documentMapper.selectCount(any())).thenReturn(0L);

    PreparationCandidateSaveReq req = new PreparationCandidateSaveReq(
        "李敏",
        "450102199001011234",
        "13800001234",
        "senior",
        "高级",
        "manual",
        "鉴定中心",
        null
    );

    service.createCandidate(1L, req);

    verify(candidateMapper).insert(any(CertificationCandidateEntity.class));
    verify(candidateMapper, never()).restoreSoftDeleted(any());
    verify(documentMapper, atLeast(1)).insert(any(CertificationCandidateDocumentEntity.class));
  }

  @Test
  void createCandidateRestoresSoftDeletedCandidateInsteadOfInserting() {
    CertificationTaskEntity task = existingTask();
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(candidateMapper.selectCount(any())).thenReturn(0L);
    CertificationCandidateEntity existing = new CertificationCandidateEntity();
    existing.setId(100L);
    existing.setTaskId(1L);
    existing.setIdCardNo("450102199001011234");
    existing.setDeleted(1);
    when(candidateMapper.selectByTaskAndIdCardIncludingDeleted(1L, 1L, "450102199001011234")).thenReturn(existing);
    when(templateMapper.selectList(any())).thenReturn(candidateDocTemplates());
    when(documentMapper.selectCount(any())).thenReturn(0L);

    PreparationCandidateSaveReq req = new PreparationCandidateSaveReq(
        "李敏",
        "450102199001011234",
        "13800001234",
        "senior",
        "高级",
        "training",
        "鉴定中心",
        null
    );

    service.createCandidate(1L, req);

    assertThat(existing.getDeleted()).isZero();
    assertThat(existing.getStatus()).isEqualTo("active");
    assertThat(existing.getCandidateName()).isEqualTo("李敏");
    assertThat(existing.getImportSource()).isEqualTo("training");
    verify(candidateMapper).restoreSoftDeleted(existing);
    verify(candidateMapper, never()).insert(any(CertificationCandidateEntity.class));
    verify(documentMapper, atLeast(1)).insert(any(CertificationCandidateDocumentEntity.class));
  }

  @Test
  void deleteCandidatePurgesPreviousSoftDeletedRecordBeforeDelete() {
    CertificationTaskEntity task = existingTask();
    when(taskMapper.selectById(1L)).thenReturn(task);
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(200L);
    candidate.setTaskId(1L);
    candidate.setIdCardNo("450102199001011234");
    candidate.setDeleted(0);
    when(candidateMapper.selectById(200L)).thenReturn(candidate);
    when(materialItemMapper.selectCount(any())).thenReturn(0L);

    service.deleteCandidate(1L, 200L);

    verify(candidateMapper).purgeSoftDeletedByTaskAndIdCard(1L, 1L, "450102199001011234");
    verify(documentMapper).physicalDeleteByCandidate(1L, 200L);
    verify(stagingApplicationService).releasePromoteByCandidate(1L, 200L);
    verify(candidateMapper).physicalDeleteById(200L);
    verify(candidateMapper, never()).deleteById(200L);
  }

  private List<CertificationMaterialTemplateEntity> candidateDocTemplates() {
    CertificationMaterialTemplateEntity template = new CertificationMaterialTemplateEntity();
    template.setId(101L);
    template.setTemplateScope("candidate_doc");
    template.setItemCode("DOC001");
    template.setItemName("报名信息表");
    template.setRequired(true);
    template.setEnabled(true);
    template.setSortOrder(1);
    return List.of(template);
  }

  private CertificationTaskEntity existingTask() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setTenantId(1L);
    task.setTaskNo("RD-2026-001");
    task.setTaskName("2026 年职业技能认定任务");
    task.setExamDate(LocalDate.of(2026, 6, 18));
    task.setStatus("running");
    return task;
  }

  private CertificationTaskScopeEntity existingScope() {
    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setTaskId(1L);
    scope.setWorkTypeName("电工");
    scope.setLevelCode("senior");
    scope.setLevelName("高级");
    scope.setSortOrder(0);
    scope.setDeleted(0);
    return scope;
  }

  private CertificationTaskAssessorGroupEntity existingGroup() {
    CertificationTaskAssessorGroupEntity group = new CertificationTaskAssessorGroupEntity();
    group.setId(2L);
    group.setTaskId(1L);
    group.setGroupName("实操考评一组");
    group.setLeaderAssessorId(8L);
    return group;
  }

  private CertificationInfoAssessorEntity assessor(String scope, LocalDate validUntil) {
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setAssessorName("赵评");
    assessor.setCertificateNo("A-2026-001");
    assessor.setWorkTypeScope(scope);
    assessor.setValidUntil(validUntil);
    assessor.setAuditStatus("approved");
    assessor.setWorkStatus("idle");
    return assessor;
  }

  private CertificationInfoExamEquipmentEntity infoEquipment(String status) {
    CertificationInfoExamEquipmentEntity equipment = new CertificationInfoExamEquipmentEntity();
    equipment.setId(30L);
    equipment.setEquipmentType("Computer");
    equipment.setAssetNo("EQ-001");
    equipment.setAuditStatus("approved");
    equipment.setEquipmentStatus(status);
    equipment.setReferencedCount(0);
    return equipment;
  }

  private CertificationAssessorTaskEntity assessorTask() {
    CertificationAssessorTaskEntity task = new CertificationAssessorTaskEntity();
    task.setId(50L);
    task.setTaskId(1L);
    task.setAssessorId(9L);
    task.setLeaderAssessorId(8L);
    task.setStatus("assigned");
    return task;
  }
}
