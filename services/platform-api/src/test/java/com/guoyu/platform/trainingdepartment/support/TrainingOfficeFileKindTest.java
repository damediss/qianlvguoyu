package com.guoyu.platform.trainingdepartment.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class TrainingOfficeFileKindTest {

  @Test
  void sniffFormat_distinguishesDocxAndXlsx() throws IOException {
    byte[] docx;
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      doc.createParagraph().createRun().setText("${companyName}");
      doc.write(bos);
      docx = bos.toByteArray();
    }
    byte[] xlsx;
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      wb.createSheet("s").createRow(0).createCell(0).setCellValue("${companyName}");
      wb.write(bos);
      xlsx = bos.toByteArray();
    }

    assertEquals("docx", SubsidyExcelStyleSupport.sniffFormat(docx));
    assertEquals("xlsx", SubsidyExcelStyleSupport.sniffFormat(xlsx));
    assertEquals("docx", TrainingOfficeFileKind.sniffFormat(docx));
    assertEquals("xlsx", TrainingOfficeFileKind.sniffFormat(xlsx));
  }

  @Test
  void sniffFormat_emptyIsBlank() {
    assertEquals("", SubsidyExcelStyleSupport.sniffFormat(null));
    assertEquals("", SubsidyExcelStyleSupport.sniffFormat(new byte[] {1, 2, 3}));
  }
}
