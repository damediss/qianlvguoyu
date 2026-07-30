package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.archive.storage.FileStorageService;
import com.guoyu.platform.certification.entity.CertificationArchiveMaterialEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveRecordEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamSiteEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamSiteMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveBasicSnapshotMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveCandidateMaterialMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveMaterialMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveRecordMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskEquipmentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CertificationArchiveApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationTaskPhaseMapper phaseMapper = Mockito.mock(CertificationTaskPhaseMapper.class);
  private final CertificationCandidateMapper sourceCandidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationTaskExamStaffMapper examStaffMapper = Mockito.mock(CertificationTaskExamStaffMapper.class);
  private final CertificationTaskEquipmentMapper equipmentMapper = Mockito.mock(CertificationTaskEquipmentMapper.class);
  private final CertificationTaskExamSiteMapper examSiteMapper = Mockito.mock(CertificationTaskExamSiteMapper.class);
  private final CertificationTaskAssessorMapper assessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationInfoExamStaffMapper infoExamStaffMapper = Mockito.mock(CertificationInfoExamStaffMapper.class);
  private final CertificationInfoExamEquipmentMapper infoEquipmentMapper =
      Mockito.mock(CertificationInfoExamEquipmentMapper.class);
  private final CertificationInfoExamSiteMapper infoExamSiteMapper = Mockito.mock(CertificationInfoExamSiteMapper.class);
  private final CertificationInfoAssessorMapper infoAssessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final CertificationArchiveRecordMapper recordMapper = Mockito.mock(CertificationArchiveRecordMapper.class);
  private final CertificationArchiveBasicSnapshotMapper snapshotMapper =
      Mockito.mock(CertificationArchiveBasicSnapshotMapper.class);
  private final CertificationArchiveMaterialMapper materialMapper = Mockito.mock(CertificationArchiveMaterialMapper.class);
  private final CertificationArchiveCandidateMapper candidateMapper = Mockito.mock(CertificationArchiveCandidateMapper.class);
  private final CertificationArchiveCandidateMaterialMapper candidateMaterialMapper =
      Mockito.mock(CertificationArchiveCandidateMaterialMapper.class);
  private final CertificationArchiveImportBatchMapper importBatchMapper =
      Mockito.mock(CertificationArchiveImportBatchMapper.class);
  private final CertificationArchiveImportErrorMapper importErrorMapper =
      Mockito.mock(CertificationArchiveImportErrorMapper.class);
  private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);

  private CertificationArchiveApplicationService service;

  @BeforeEach
  void setUp() {
    service = new CertificationArchiveApplicationService(
        taskMapper,
        phaseMapper,
        sourceCandidateMapper,
        scopeMapper,
        examStaffMapper,
        equipmentMapper,
        examSiteMapper,
        assessorMapper,
        infoExamStaffMapper,
        infoEquipmentMapper,
        infoExamSiteMapper,
        infoAssessorMapper,
        recordMapper,
        snapshotMapper,
        materialMapper,
        candidateMapper,
        candidateMaterialMapper,
        importBatchMapper,
        importErrorMapper,
        fileStorageService,
        new ObjectMapper()
    );
  }

  @Test
  void uploadMaterialRejectsDuplicateTypeBeforeStoringFile() {
    when(recordMapper.selectById(10L)).thenReturn(archiveRecord());
    when(materialMapper.selectOne(any())).thenReturn(new CertificationArchiveMaterialEntity());

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "result.pdf",
        "application/pdf",
        "content".getBytes()
    );

    assertThatThrownBy(() -> service.uploadMaterial(10L, "RESULT_CONFIRM_FORM", null, file))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该类认定材料已上传，请勿重复上传");
    verify(fileStorageService, never()).store(any(), any());
  }

  @Test
  void completeCheckUsesThreeArchiveClosureItems() {
    CertificationArchiveRecordEntity record = archiveRecord();
    record.setBasicArchived(true);
    when(recordMapper.selectById(10L)).thenReturn(record);
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setCurrentPhase(CertificationTaskPhases.ARCHIVE);
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(materialMapper.selectCount(any())).thenReturn(9L);
    when(importBatchMapper.selectCount(any())).thenReturn(0L);

    var result = service.completeCheck(10L, false);

    assertThat(result.passed()).isFalse();
    assertThat(result.items()).extracting("name")
        .containsExactly("基础信息归档", "认定材料", "考试材料导入");
    assertThat(result.items().get(2).message()).isEqualTo("请完成考试材料导入并确认批次");
  }

  @Test
  void releaseTaskResourcesReturnsAllConfiguredInfoToIdle() {
    CertificationTaskExamStaffEntity staffBinding = new CertificationTaskExamStaffEntity();
    staffBinding.setStaffId(20L);
    when(examStaffMapper.selectList(any())).thenReturn(List.of(staffBinding));
    CertificationTaskEquipmentEntity equipmentBinding = new CertificationTaskEquipmentEntity();
    equipmentBinding.setEquipmentId(30L);
    when(equipmentMapper.selectList(any())).thenReturn(List.of(equipmentBinding));
    CertificationTaskExamSiteEntity siteBinding = new CertificationTaskExamSiteEntity();
    siteBinding.setSiteId(40L);
    when(examSiteMapper.selectList(any())).thenReturn(List.of(siteBinding));
    CertificationTaskAssessorEntity assessorBinding = new CertificationTaskAssessorEntity();
    assessorBinding.setAssessorId(50L);
    when(assessorMapper.selectList(any())).thenReturn(List.of(assessorBinding));

    CertificationInfoExamStaffEntity staff = new CertificationInfoExamStaffEntity();
    staff.setAuditStatus("approved");
    staff.setWorkStatus("assigned");
    staff.setReferencedCount(1);
    when(infoExamStaffMapper.selectById(20L)).thenReturn(staff);
    CertificationInfoExamEquipmentEntity equipment = new CertificationInfoExamEquipmentEntity();
    equipment.setAuditStatus("approved");
    equipment.setEquipmentStatus("in_use");
    equipment.setReferencedCount(1);
    when(infoEquipmentMapper.selectById(30L)).thenReturn(equipment);
    CertificationInfoExamSiteEntity site = new CertificationInfoExamSiteEntity();
    site.setAuditStatus("approved");
    site.setStatus("in_use");
    site.setReferencedCount(1);
    when(infoExamSiteMapper.selectById(40L)).thenReturn(site);
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setAuditStatus("approved");
    assessor.setWorkStatus("assigned");
    assessor.setReferencedCount(1);
    when(infoAssessorMapper.selectById(50L)).thenReturn(assessor);

    ReflectionTestUtils.invokeMethod(service, "releaseTaskResources", 1L);

    assertThat(staff.getWorkStatus()).isEqualTo("idle");
    assertThat(equipment.getEquipmentStatus()).isEqualTo("idle");
    assertThat(site.getStatus()).isEqualTo("idle");
    assertThat(assessor.getWorkStatus()).isEqualTo("idle");
    assertThat(List.of(
        staff.getReferencedCount(),
        equipment.getReferencedCount(),
        site.getReferencedCount(),
        assessor.getReferencedCount()
    )).containsOnly(0);
    verify(infoExamStaffMapper).updateById(staff);
    verify(infoEquipmentMapper).updateById(equipment);
    verify(infoExamSiteMapper).updateById(site);
    verify(infoAssessorMapper).updateById(assessor);
  }

  private CertificationArchiveRecordEntity archiveRecord() {
    CertificationArchiveRecordEntity record = new CertificationArchiveRecordEntity();
    record.setId(10L);
    record.setTaskId(1L);
    record.setTaskNo("RD-2026-001");
    record.setArchiveStatus("NOT_ARCHIVED");
    record.setBasicArchived(false);
    return record;
  }
}
