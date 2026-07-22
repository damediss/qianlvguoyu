package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionRosterPrecheckReq;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionRosterImportMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionRosterImportServiceTest {

  private final CompetitionRosterImportMapper importMapper = Mockito.mock(CompetitionRosterImportMapper.class);
  private final CompetitionRosterItemMapper itemMapper = Mockito.mock(CompetitionRosterItemMapper.class);
  private final CompetitionPlayerIdentityDomainService playerIdentityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);
  private final CompetitionRosterImportService service = new CompetitionRosterImportService(
      importMapper, itemMapper, playerIdentityService);

  @Test
  void precheckStoresValidRowsAndBatchCounts() throws Exception {
    stubPlayerIdentity(9001L);
    AtomicReference<CompetitionRosterImportEntity> batchRef = new AtomicReference<>();
    when(importMapper.insert(any(CompetitionRosterImportEntity.class))).thenAnswer(invocation -> {
      CompetitionRosterImportEntity batch = invocation.getArgument(0);
      batch.setId(7001L);
      batchRef.set(batch);
      return 1;
    });

    CompetitionRosterImportEntity batch = service.precheck(1L, 1001L, 3001L, 8001L,
        workbook(new String[][]{{"E001", "Alice", "11010519491231002X", "13800138000", "electrician", "senior"}}));

    assertThat(batchRef.get()).isSameAs(batch);
    assertThat(batch.getStatus()).isEqualTo("pending_confirm");
    assertThat(batch.getTotalCount()).isEqualTo(1);
    assertThat(batch.getValidCount()).isEqualTo(1);
    assertThat(batch.getInvalidCount()).isEqualTo(0);
    verify(itemMapper).insert(any(CompetitionRosterItemEntity.class));
  }

  @Test
  void duplicateEmployeeNoMakesAllDuplicateRowsInvalid() throws Exception {
    when(importMapper.insert(any(CompetitionRosterImportEntity.class))).thenAnswer(invocation -> {
      CompetitionRosterImportEntity batch = invocation.getArgument(0);
      batch.setId(7002L);
      return 1;
    });

    CompetitionRosterImportEntity batch = service.precheck(1L, 1001L, 3001L, 8001L,
        workbook(new String[][]{
            {"E001", "Alice", "11010519491231002X", "13800138000", "electrician", "senior"},
            {"E001", "Bob", "11010519491231002X", "13800138000", "welder", "intermediate"}
        }));

    assertThat(batch.getStatus()).isEqualTo("validation_failed");
    assertThat(batch.getValidCount()).isEqualTo(0);
    assertThat(batch.getInvalidCount()).isEqualTo(2);
    verify(itemMapper, Mockito.times(2)).insert(any(CompetitionRosterItemEntity.class));
  }

  @Test
  void emptyRosterFailsBeforeCreatingBatch() throws Exception {
    assertThatThrownBy(() -> service.precheck(1L, 1001L, 3001L, 8001L, workbook(new String[0][0])))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("名单不能为空");
    verify(importMapper, never()).insert(any(CompetitionRosterImportEntity.class));
  }

  @Test
  void singleImportCreatesOnePendingConfirmBatch() {
    stubPlayerIdentity(9002L);
    when(importMapper.insert(any(CompetitionRosterImportEntity.class))).thenAnswer(invocation -> {
      CompetitionRosterImportEntity batch = invocation.getArgument(0);
      batch.setId(7003L);
      return 1;
    });

    CompetitionRosterImportEntity batch = service.precheckSingle(1L, 1001L, 3001L,
        "E001", "Alice", "11010519491231002X", "13800138000", "electrician", "senior");

    assertThat(batch.getBatchNo()).startsWith("SINGLE-");
    assertThat(batch.getStatus()).isEqualTo("pending_confirm");
    verify(itemMapper).insert(any(CompetitionRosterItemEntity.class));
  }

  private byte[] workbook(String[][] rows) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("roster");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("employeeNo");
      header.createCell(1).setCellValue("name");
      header.createCell(2).setCellValue("idCardNo");
      header.createCell(3).setCellValue("mobile");
      header.createCell(4).setCellValue("workType");
      header.createCell(5).setCellValue("level");
      for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
        Row row = sheet.createRow(rowIndex + 1);
        for (int column = 0; column < rows[rowIndex].length; column++) {
          row.createCell(column).setCellValue(rows[rowIndex][column]);
        }
      }
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private void stubPlayerIdentity(Long playerId) {
    when(playerIdentityService.normalizeIdCard(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(playerIdentityService.normalizeMobile(any())).thenAnswer(invocation -> invocation.getArgument(0));
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(playerId);
    when(playerIdentityService.resolveOrCreate(any(), any(), any())).thenReturn(player);
  }
}
