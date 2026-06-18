package com.guoyu.platform.certification.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.info.application.CertificationInfoApplicationService;
import com.guoyu.platform.certification.info.dto.AssessorAccountCreateReq;
import com.guoyu.platform.certification.info.dto.AssessorInfoSaveReq;
import com.guoyu.platform.certification.info.dto.CertificationInfoQueryReq;
import com.guoyu.platform.certification.info.dto.ExamEquipmentInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamSiteInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamStaffInfoSaveReq;
import com.guoyu.platform.certification.info.dto.InfoReviewReq;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAuditRecordEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamSiteEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAuditRecordMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamSiteMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.info.vo.AssessorInfoVO;
import com.guoyu.platform.certification.info.vo.ExamStaffInfoVO;
import com.guoyu.platform.certification.mapper.ExamStaffAssignedTaskRow;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.service.UserApplicationService;
import com.guoyu.platform.system.vo.UserVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CertificationInfoApplicationServiceTest {

  private final CertificationInfoAssessorMapper assessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final CertificationInfoExamStaffMapper examStaffMapper = Mockito.mock(CertificationInfoExamStaffMapper.class);
  private final CertificationTaskExamStaffMapper taskExamStaffMapper = Mockito.mock(CertificationTaskExamStaffMapper.class);
  private final CertificationInfoExamSiteMapper siteMapper = Mockito.mock(CertificationInfoExamSiteMapper.class);
  private final CertificationInfoExamEquipmentMapper equipmentMapper = Mockito.mock(CertificationInfoExamEquipmentMapper.class);
  private final CertificationInfoAuditRecordMapper auditRecordMapper = Mockito.mock(CertificationInfoAuditRecordMapper.class);
  private final RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final UserApplicationService userApplicationService = Mockito.mock(UserApplicationService.class);

  private CertificationInfoApplicationService service;

  @BeforeEach
  void setUp() {
    RoleEntity role = new RoleEntity();
    role.setId(88L);
    role.setCode("CERTIFICATION_ASSESSOR");
    role.setStatus("enabled");
    when(roleMapper.selectOne(any())).thenReturn(role);
    when(userApplicationService.create(any())).thenReturn(new UserVO(
        99L, 1L, null, null, null, "assessor.zhangsan", "张三", "assessor",
        "13800001234", null, "enabled", null, List.of(88L),
        List.of("CERTIFICATION_ASSESSOR"), LocalDateTime.now(), LocalDateTime.now()
    ));
    service = new CertificationInfoApplicationService(
        assessorMapper, examStaffMapper, taskExamStaffMapper, siteMapper, equipmentMapper, auditRecordMapper,
        roleMapper, userMapper, userApplicationService);
  }

  @Test
  void createAssessorSavesDraftAndMasksPhone() {
    when(assessorMapper.selectCount(any())).thenReturn(0L);
    AssessorInfoVO vo = service.createAssessor(new AssessorInfoSaveReq(
        "张三", "assessor.zhangsan", "Assessor@123", "CERT-001", "电工", List.of("电工"), "高级", LocalDate.of(2027, 6, 30),
        "13800001234", "材料齐全说明", "备注"
    ));

    assertThat(vo.auditStatus()).isEqualTo("draft");
    assertThat(vo.workStatus()).isEqualTo("pending_review");
    assertThat(vo.userId()).isEqualTo(99L);
    assertThat(vo.accountUsername()).isEqualTo("assessor.zhangsan");
    assertThat(vo.accountPassword()).isEqualTo("Assessor@123");
    assertThat(vo.maskedPhone()).isEqualTo("138****1234");
    verify(userApplicationService).create(any());
    verify(assessorMapper).insert(any(CertificationInfoAssessorEntity.class));
  }

  @Test
  void createAssessorRejectsDuplicateCertificateNo() {
    when(assessorMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(() -> service.createAssessor(new AssessorInfoSaveReq(
        "张三", "assessor.zhangsan", "Assessor@123", "CERT-001", "电工", List.of("电工"), null, LocalDate.of(2027, 6, 30),
        null, null, null
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("证号已存在");
  }

  @Test
  void createAccountBindsExistingApprovedAssessor() {
    CertificationInfoAssessorEntity existing = assessor("approved", "idle");
    when(assessorMapper.selectById(1L)).thenReturn(existing);

    AssessorInfoVO result = service.createAssessorAccount(
        1L,
        new AssessorAccountCreateReq("assessor.existing", "Assessor@123")
    );

    assertThat(result.userId()).isEqualTo(99L);
    assertThat(result.accountUsername()).isEqualTo("assessor.existing");
    assertThat(result.accountPassword()).isEqualTo("Assessor@123");
    assertThat(existing.getUserId()).isEqualTo(99L);
    assertThat(existing.getAccountUsername()).isEqualTo("assessor.existing");
    assertThat(existing.getAccountPassword()).isEqualTo("Assessor@123");
    verify(userApplicationService).create(any());
    verify(assessorMapper).updateById(existing);
  }

  @Test
  void submitAssessorRequiresMaterialsDescriptionAndWritesAuditRecord() {
    CertificationInfoAssessorEntity assessor = assessor("draft", "idle");
    assessor.setMaterialsDescription("短");
    when(assessorMapper.selectById(1L)).thenReturn(assessor);

    assertThatThrownBy(() -> service.submitAssessor(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("材料说明");

    assessor.setMaterialsDescription("材料说明完整有效");
    service.submitAssessor(1L);

    assertThat(assessor.getAuditStatus()).isEqualTo("pending");
    verify(assessorMapper).updateById(assessor);
    verify(auditRecordMapper).insert(any(CertificationInfoAuditRecordEntity.class));
  }

  @Test
  void reviewAssessorRejectRequiresChineseReasonAndPassApproves() {
    CertificationInfoAssessorEntity assessor = assessor("pending", "idle");
    when(assessorMapper.selectById(1L)).thenReturn(assessor);

    assertThatThrownBy(() -> service.reviewAssessor(1L, new InfoReviewReq("reject", "短")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("驳回原因");

    service.reviewAssessor(1L, new InfoReviewReq("pass", null));

    assertThat(assessor.getAuditStatus()).isEqualTo("approved");
    verify(auditRecordMapper).insert(any(CertificationInfoAuditRecordEntity.class));
  }

  @Test
  void deleteDisabledAssessorSucceeds() {
    CertificationInfoAssessorEntity disabled = assessor("disabled", "disabled");
    when(assessorMapper.selectById(3L)).thenReturn(disabled);

    service.deleteAssessor(3L);

    verify(assessorMapper).deleteById(3L);
  }

  @Test
  void deleteExamStaffWithOperationalDisabledStatusSucceeds() {
    CertificationInfoExamStaffEntity staff = examStaff("disabled");
    staff.setAuditStatus("approved");
    staff.setReferencedCount(0);
    when(examStaffMapper.selectById(9L)).thenReturn(staff);

    service.deleteExamStaff(9L);

    verify(examStaffMapper).deleteById(9L);
  }

  @Test
  void deleteAssessorProtectsApprovedOrReferencedRecords() {
    CertificationInfoAssessorEntity approved = assessor("approved", "idle");
    approved.setReferencedCount(0);
    when(assessorMapper.selectById(1L)).thenReturn(approved);

    assertThatThrownBy(() -> service.deleteAssessor(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已入库");

    CertificationInfoAssessorEntity rejected = assessor("rejected", "idle");
    rejected.setReferencedCount(1);
    when(assessorMapper.selectById(2L)).thenReturn(rejected);

    assertThatThrownBy(() -> service.deleteAssessor(2L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已被任务引用");
  }

  @Test
  void availableAssessorsOnlyIncludeApprovedIdleAndUnexpiredRecords() {
    CertificationInfoAssessorEntity available = assessor("approved", "idle");
    available.setValidUntil(LocalDate.now().plusDays(1));
    CertificationInfoAssessorEntity expired = assessor("approved", "idle");
    expired.setId(2L);
    expired.setValidUntil(LocalDate.now().minusDays(1));
    when(assessorMapper.selectList(any())).thenReturn(List.of(available, expired));

    assertThat(service.listAvailableAssessors(null, List.of())).hasSize(1);
    assertThat(service.listAvailableAssessors(null, List.of()).getFirst().id()).isEqualTo(1L);
  }

  @Test
  void getExamStaffResolvesAssignedTaskNosFromPreparationBindings() {
    CertificationInfoExamStaffEntity staff = examStaff("assigned");
    when(examStaffMapper.selectById(9L)).thenReturn(staff);
    ExamStaffAssignedTaskRow rowA = new ExamStaffAssignedTaskRow();
    rowA.setStaffId(9L);
    rowA.setTaskNo("RD-2026-001");
    ExamStaffAssignedTaskRow rowB = new ExamStaffAssignedTaskRow();
    rowB.setStaffId(9L);
    rowB.setTaskNo("RD-2026-002");
    when(taskExamStaffMapper.selectAssignedTaskNosByStaffIds(List.of(9L))).thenReturn(List.of(rowA, rowB));

    ExamStaffInfoVO vo = service.getExamStaff(9L);

    assertThat(vo.assignedTaskNos()).isEqualTo("RD-2026-001、RD-2026-002");
  }

  @Test
  void createExamStaffStartsPendingAndPassReviewMakesItApprovedIdle() {
    ExamStaffInfoVO created = service.createExamStaff(new ExamStaffInfoSaveReq("测试考务", "13800000009"));

    assertThat(created.auditStatus()).isEqualTo("pending");
    assertThat(created.workStatus()).isEqualTo("idle");
    verify(examStaffMapper).insert(any(CertificationInfoExamStaffEntity.class));

    CertificationInfoExamStaffEntity staff = examStaff("assigned");
    staff.setAuditStatus("pending");
    when(examStaffMapper.selectById(9L)).thenReturn(staff);

    ExamStaffInfoVO approved = service.reviewExamStaff(9L, new InfoReviewReq("pass", null));

    assertThat(approved.auditStatus()).isEqualTo("approved");
    assertThat(approved.workStatus()).isEqualTo("idle");
    assertThat(staff.getWorkStatus()).isEqualTo("idle");
  }

  @Test
  void pageExamStaffReleasesAssignedStaffWhenNoActiveTaskBindingExists() {
    CertificationInfoExamStaffEntity stale = examStaff("assigned");
    stale.setReferencedCount(1);
    Page<CertificationInfoExamStaffEntity> page = new Page<>(1, 10);
    page.setRecords(List.of(stale));
    page.setTotal(1);
    when(examStaffMapper.selectPage(any(), any())).thenReturn(page);
    when(taskExamStaffMapper.selectAssignedTaskNosByStaffIds(List.of(9L))).thenReturn(List.of());

    var result = service.pageExamStaff(new CertificationInfoQueryReq());

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().workStatus()).isEqualTo("idle");
    assertThat(result.items().getFirst().assignedTaskNos()).isNull();
    assertThat(stale.getWorkStatus()).isEqualTo("idle");
    assertThat(stale.getReferencedCount()).isZero();
    verify(examStaffMapper).updateById(stale);
  }

  @Test
  void availableExamStaffIncludesStaleAssignedStaffAfterRelease() {
    CertificationInfoExamStaffEntity stale = examStaff("assigned");
    stale.setReferencedCount(1);
    when(examStaffMapper.selectList(any())).thenReturn(List.of(stale));
    when(taskExamStaffMapper.selectAssignedTaskNosByStaffIds(List.of(9L))).thenReturn(List.of());

    List<ExamStaffInfoVO> available = service.listAvailableExamStaff(null);

    assertThat(available).hasSize(1);
    assertThat(available.getFirst().workStatus()).isEqualTo("idle");
    assertThat(stale.getWorkStatus()).isEqualTo("idle");
    assertThat(stale.getReferencedCount()).isZero();
    verify(examStaffMapper).updateById(stale);
  }

  @Test
  void siteAndEquipmentDuplicateBusinessKeysAreBlocked() {
    when(siteMapper.selectCount(any())).thenReturn(1L);
    assertThatThrownBy(() -> service.createSite(new ExamSiteInfoSaveReq(
        "一号考点", "theory", "园区路1号", 30, "FIL-001", "李四", "13900001234",
        "设备良好", "备案材料完整", "active", null
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("备案号已存在");

    when(siteMapper.selectCount(any())).thenReturn(0L);
    when(equipmentMapper.selectCount(any())).thenReturn(1L);
    assertThatThrownBy(() -> service.createEquipment(new ExamEquipmentInfoSaveReq(
        "理论考试终端", "ASSET-001", 1, null, null, "T1000", LocalDate.now().plusMonths(6),
        "normal", null, null, "设备资料完整", null
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("资产编号已存在");
  }

  @Test
  void createEquipmentDefaultsMissingStatusToIdle() {
    when(equipmentMapper.selectCount(any())).thenReturn(0L);

    var result = service.createEquipment(new ExamEquipmentInfoSaveReq(
        "理论考试终端", "ASSET-002", 1, null, null, "T1000", LocalDate.now().plusMonths(6),
        null, null, null, "设备资料完整", null
    ));

    ArgumentCaptor<CertificationInfoExamEquipmentEntity> captor = forClass(CertificationInfoExamEquipmentEntity.class);
    verify(equipmentMapper).insert(captor.capture());
    assertThat(result.equipmentStatus()).isEqualTo("idle");
    assertThat(captor.getValue().getEquipmentStatus()).isEqualTo("idle");
  }

  @Test
  void createEquipmentPersistsPhotoFileInfo() {
    when(equipmentMapper.selectCount(any())).thenReturn(0L);

    var result = service.createEquipment(new ExamEquipmentInfoSaveReq(
        "实操考试终端", "ASSET-003", 1, null, null, "P2000", LocalDate.now().plusMonths(6),
        null, 9001L, "terminal-photo.jpg", "设备资料完整", null
    ));

    ArgumentCaptor<CertificationInfoExamEquipmentEntity> captor = forClass(CertificationInfoExamEquipmentEntity.class);
    verify(equipmentMapper).insert(captor.capture());
    assertThat(result.equipmentPhotoFileId()).isEqualTo(9001L);
    assertThat(result.equipmentPhotoFileName()).isEqualTo("terminal-photo.jpg");
    assertThat(captor.getValue().getEquipmentPhotoFileId()).isEqualTo(9001L);
    assertThat(captor.getValue().getEquipmentPhotoFileName()).isEqualTo("terminal-photo.jpg");
  }

  @Test
  void reviewEquipmentPassSetsStatusToIdle() {
    CertificationInfoExamEquipmentEntity equipment = equipment("pending", "normal");
    when(equipmentMapper.selectById(30L)).thenReturn(equipment);

    var result = service.reviewEquipment(30L, new InfoReviewReq("pass", null));

    assertThat(result.auditStatus()).isEqualTo("approved");
    assertThat(result.equipmentStatus()).isEqualTo("idle");
    assertThat(equipment.getEquipmentStatus()).isEqualTo("idle");
    verify(equipmentMapper).updateById(equipment);
  }

  @Test
  void availableEquipmentOnlyReturnsApprovedIdleRecords() {
    CertificationInfoExamEquipmentEntity idle = equipment("approved", "idle");
    CertificationInfoExamEquipmentEntity inUse = equipment("approved", "in_use");
    inUse.setId(31L);
    when(equipmentMapper.selectList(any())).thenReturn(List.of(idle, inUse));

    var available = service.listAvailableEquipment(null);

    assertThat(available).hasSize(1);
    assertThat(available.getFirst().id()).isEqualTo(30L);
    assertThat(available.getFirst().equipmentStatus()).isEqualTo("idle");
  }

  private CertificationInfoExamStaffEntity examStaff(String workStatus) {
    CertificationInfoExamStaffEntity entity = new CertificationInfoExamStaffEntity();
    entity.setId(9L);
    entity.setStaffName("测试考务");
    entity.setPhone("13800000009");
    entity.setMaskedPhone("138****0009");
    entity.setAuditStatus("approved");
    entity.setWorkStatus(workStatus);
    entity.setReferencedCount(1);
    return entity;
  }

  private CertificationInfoAssessorEntity assessor(String auditStatus, String workStatus) {
    CertificationInfoAssessorEntity entity = new CertificationInfoAssessorEntity();
    entity.setId(1L);
    entity.setAssessorName("张三");
    entity.setCertificateNo("CERT-001");
    entity.setWorkTypeScope("电工");
    entity.setLevelScope("高级");
    entity.setValidUntil(LocalDate.of(2027, 6, 30));
    entity.setPhone("13800001234");
    entity.setMaskedPhone("138****1234");
    entity.setMaterialsDescription("材料说明完整有效");
    entity.setAuditStatus(auditStatus);
    entity.setWorkStatus(workStatus);
    entity.setReferencedCount(0);
    return entity;
  }

  private CertificationInfoExamEquipmentEntity equipment(String auditStatus, String equipmentStatus) {
    CertificationInfoExamEquipmentEntity entity = new CertificationInfoExamEquipmentEntity();
    entity.setId(30L);
    entity.setEquipmentType("Computer");
    entity.setAssetNo("EQ-001");
    entity.setEquipmentModel("T1000");
    entity.setCalibrationValidUntil(LocalDate.now().plusMonths(6));
    entity.setMaterialsDescription("complete equipment materials");
    entity.setAuditStatus(auditStatus);
    entity.setEquipmentStatus(equipmentStatus);
    entity.setReferencedCount(0);
    return entity;
  }
}
