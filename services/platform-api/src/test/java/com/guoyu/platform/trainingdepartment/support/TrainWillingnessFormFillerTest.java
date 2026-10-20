package com.guoyu.platform.trainingdepartment.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.trainingdepartment.support.TrainWillingnessFormFiller.FillInput;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TrainWillingnessFormFillerTest {

  @Test
  void fillsBasicFieldsAndChecksWithoutBreakingTemplate() throws Exception {
    byte[] template = new ClassPathResource(TrainWillingnessFormPackService.TEMPLATE_PATH)
        .getInputStream()
        .readAllBytes();
    FillInput input = new FillInput(
        "张三",
        "男",
        "壮族",
        "28",
        "450103199801011234",
        "13800138000",
        "南宁市青秀区某某路1号",
        "企业在岗职工",
        true,
        "电工",
        false,
        "",
        "电工",
        5);

    byte[] filled = TrainWillingnessFormFiller.fill(template, input);
    assertTrue(filled.length > 1000);

    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(filled))) {
      XWPFTable table = doc.getTables().get(0);
      String r0 = table.getRow(0).getCell(1).getText();
      assertTrue(r0.contains("张三"), r0);
      assertTrue(table.getRow(0).getCell(3).getText().contains("男"));
      assertTrue(table.getRow(1).getCell(1).getText().contains("450103199801011234"));
      String category = table.getRow(3).getCell(1).getText();
      assertTrue(category.contains("\u2611企业在岗职工") || category.contains("\u2611其他人员"), category);
      String employment = table.getRow(5).getCell(1).getText();
      assertTrue(employment.contains("\u2611已就业"), employment);
      assertTrue(employment.contains("\u25A1已创业"), employment);
      assertTrue(employment.contains("\u25A1未创业"), employment);
      String willingness = table.getRow(6).getCell(1).getText();
      assertTrue(willingness.contains("张三"), willingness);
      assertTrue(willingness.contains("电工"), willingness);
      assertTrue(willingness.contains("五"), willingness);
    }

    // 版式：仍为有效 OOXML，且标题段落仍在
    String xml = readDocumentXml(filled);
    assertTrue(xml.contains("培训意愿需求登记表"));
    assertTrue(xml.contains("南宁市补贴性职业技能培训学员"));
  }

  private static String readDocumentXml(byte[] docx) throws Exception {
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if ("word/document.xml".equals(entry.getName())) {
          return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    }
    return "";
  }
}
