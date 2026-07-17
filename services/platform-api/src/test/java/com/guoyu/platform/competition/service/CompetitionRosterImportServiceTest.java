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
import com.guoyu.platform.competition.mapper.CompetitionRosterImportMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
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
  private final CompetitionRosterImportService service = new CompetitionRosterImportService(importMapper, itemMapper);

  @Test
  void precheckStoresValidRowsAndBatchCounts() throws Exception {
    AtomicReference<CompetitionRosterImportEntity> batchRef = new AtomicReference<>();
    when(importMapper.insert(any(CompetitionRosterImportEntity.class))).thenAnswer(invocation -> {
      CompetitionRosterImportEntity batch = invocation.getArgument(0);
      batch.setId(7001L);
      batchRef.set(batch);
      return 1;
    });

    CompetitionRosterImportEntity batch = service.precheck(1L, 1001L, 3001L, 8001L,
        workbook(new String[][]{{"E001", "Alice", "electrician", "senior"}}));

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
            {"E001", "Alice", "electrician", "senior"},
            {"E001", "Bob", "welder", "intermediate"}
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

  private byte[] workbook(String[][] rows) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("roster");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("employeeNo");
      header.createCell(1).setCellValue("name");
      header.createCell(2).setCellValue("workType");
      header.createCell(3).setCellValue("level");
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
}
