package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SubsidyXlsxCellPatcherTest {
  @Test
  void patchKeepsBorderStyle() throws Exception {
    byte[] src;
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Cell cell = sheet.createRow(0).createCell(0);
      cell.setCellValue("old");
      CellStyle style = wb.createCellStyle();
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
      cell.setCellStyle(style);
      wb.write(bos);
      src = bos.toByteArray();
    }

    byte[] out = SubsidyXlsxCellPatcher.patch(src, Map.of("A1", "new-value"));
    try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(out))) {
      Cell cell = wb.getSheetAt(0).getRow(0).getCell(0);
      assertThat(cell.getStringCellValue()).isEqualTo("new-value");
      assertThat(cell.getCellStyle().getBorderBottom()).isEqualTo(BorderStyle.THIN);
      assertThat(cell.getCellStyle().getBorderTop()).isEqualTo(BorderStyle.THIN);
    }
  }

  @Test
  void parseAddress() {
    assertThat(SubsidyXlsxCellPatcher.parseAddress("A1")).containsExactly(0, 0);
    assertThat(SubsidyXlsxCellPatcher.parseAddress("J2")).containsExactly(1, 9);
    assertThat(SubsidyXlsxCellPatcher.parseAddress("AA10")).containsExactly(9, 26);
  }
}
