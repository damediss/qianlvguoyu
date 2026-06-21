package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateSourceStagingMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class CertificationCandidateCsvImportServiceTest {

  @BeforeEach
  void setUpSecurityContext() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        10L, 1L, "admin", "管理员", "admin", List.of("PLATFORM_ADMIN"), List.of());
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void templateIsAnXlsxWithTheAgreedTwentyFourColumnContractAndFiveSampleCandidates() throws Exception {
    CertificationCandidateCsvImportService service = new CertificationCandidateCsvImportService(
        Mockito.mock(CertificationTaskScopeMapper.class),
        Mockito.mock(CertificationCandidateImportBatchMapper.class),
        Mockito.mock(CertificationCandidateImportErrorMapper.class),
        Mockito.mock(CertificationCandidateMapper.class),
        Mockito.mock(CertificationCandidateSourceStagingMapper.class),
        Mockito.mock(CertificationPreparationApplicationService.class),
        Mockito.mock(FileApplicationService.class), transactionManager());

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(service.templateBytes()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(workbook.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) 24);
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("序号");
      assertThat(workbook.getSheetAt(0).getRow(0).getCell(23).getStringCellValue()).isEqualTo("原证书编号");
      assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(5);
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("测试考生一");
    }
  }

  @Test
  void importsAllFiveSampleCandidatesIntoCandidateAndSourceSnapshotRecords() throws Exception {
    CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
    CertificationCandidateImportBatchMapper batchMapper = Mockito.mock(CertificationCandidateImportBatchMapper.class);
    CertificationCandidateImportErrorMapper errorMapper = Mockito.mock(CertificationCandidateImportErrorMapper.class);
    CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
    CertificationCandidateSourceStagingMapper stagingMapper = Mockito.mock(CertificationCandidateSourceStagingMapper.class);
    CertificationPreparationApplicationService preparationService = Mockito.mock(CertificationPreparationApplicationService.class);
    FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
    CertificationCandidateCsvImportService service = new CertificationCandidateCsvImportService(
        scopeMapper, batchMapper, errorMapper, candidateMapper, stagingMapper, preparationService, fileApplicationService,
        transactionManager());
    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setId(20L);
    scope.setTaskId(10L);
    scope.setLevelCode("junior");
    scope.setLevelName("初级");
    scope.setDeleted(0);
    when(scopeMapper.selectById(20L)).thenReturn(scope);
    when(fileApplicationService.upload(any(), eq("private"))).thenReturn(
        new FileVO(30L, "考生信息批量导入模板.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1L, "private", "active", LocalDateTime.now(), ""));
    when(preparationService.createCandidateForImport(eq(10L), any(PreparationCandidateSaveReq.class), any())).thenAnswer(invocation -> {
      CertificationCandidateEntity candidate = new CertificationCandidateEntity();
      candidate.setId(100L);
      return candidate;
    });

    MockMultipartFile file = new MockMultipartFile(
        "file", "考生信息批量导入模板.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", service.templateBytes());

    var result = service.importExcel(10L, 20L, file, "模板测试导入");

    assertThat(result.totalCount()).isEqualTo(5);
    assertThat(result.successCount()).isEqualTo(5);
    assertThat(result.failedCount()).isZero();
    assertThat(result.sourceType()).isEqualTo("other_xlsx");
    verify(preparationService, times(5)).createCandidateForImport(eq(10L), any(PreparationCandidateSaveReq.class), any());
    verify(stagingMapper, times(5)).insert(any(CertificationCandidateSourceStagingEntity.class));
    ArgumentCaptor<CertificationCandidateSourceStagingEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CertificationCandidateSourceStagingEntity.class);
    verify(stagingMapper, times(5)).insert(snapshotCaptor.capture());
    assertThat(new ObjectMapper().readTree(snapshotCaptor.getAllValues().get(0).getRawPayload())
        .get("姓名").asText()).isEqualTo("测试考生一");
    verify(batchMapper).insert(any(CertificationCandidateImportBatchEntity.class));
    verify(batchMapper).updateById(any(CertificationCandidateImportBatchEntity.class));
  }

  @Test
  void resumesOtherXlsxCandidateWithoutSnapshotBeforeCallingTransactionalCreateService() throws Exception {
    CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
    CertificationCandidateImportBatchMapper batchMapper = Mockito.mock(CertificationCandidateImportBatchMapper.class);
    CertificationCandidateImportErrorMapper errorMapper = Mockito.mock(CertificationCandidateImportErrorMapper.class);
    CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
    CertificationCandidateSourceStagingMapper stagingMapper = Mockito.mock(CertificationCandidateSourceStagingMapper.class);
    CertificationPreparationApplicationService preparationService = Mockito.mock(CertificationPreparationApplicationService.class);
    FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
    CertificationCandidateCsvImportService service = new CertificationCandidateCsvImportService(
        scopeMapper, batchMapper, errorMapper, candidateMapper, stagingMapper, preparationService, fileApplicationService,
        transactionManager());
    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setId(20L);
    scope.setTaskId(10L);
    scope.setLevelCode("junior");
    scope.setLevelName("初级");
    scope.setDeleted(0);
    CertificationCandidateEntity existing = new CertificationCandidateEntity();
    existing.setId(100L);
    existing.setDeleted(0);
    existing.setImportSource("other_xlsx");
    when(scopeMapper.selectById(20L)).thenReturn(scope);
    when(fileApplicationService.upload(any(), eq("private"))).thenReturn(
        new FileVO(30L, "考生信息批量导入模板.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1L, "private", "active", LocalDateTime.now(), ""));
    when(candidateMapper.selectByTaskAndIdCardIncludingDeleted(any(), eq(10L), any())).thenReturn(existing);
    when(stagingMapper.selectCount(any())).thenReturn(0L);

    MockMultipartFile file = new MockMultipartFile(
        "file", "考生信息批量导入模板.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", service.templateBytes());

    var result = service.importExcel(10L, 20L, file, "补齐来源快照");

    assertThat(result.successCount()).isEqualTo(5);
    assertThat(result.failedCount()).isZero();
    verify(preparationService, never()).createCandidateForImport(any(), any(), any());
    verify(candidateMapper, times(5)).updateById(existing);
    verify(stagingMapper, times(5)).insert(any(CertificationCandidateSourceStagingEntity.class));
  }

  private PlatformTransactionManager transactionManager() {
    PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
    return transactionManager;
  }
}
