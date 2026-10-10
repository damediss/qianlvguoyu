package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

class SubsidyDocxTextPatcherTest {

  @Test
  void parsesAndPatchesMetaAndCaption() throws Exception {
    byte[] template;
    try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      para(doc, "高级2班培训照片");
      para(doc, "培训工种：炭极生产工");
      para(doc, "培训级别：高级工");
      para(doc, "培训时间：");
      para(doc, "培训单位：千铝工投");
      para(doc, "培训地址：测试地址");
      para(doc, "图1：开班仪式                                 2026年9月7日上午");
      doc.write(bos);
      template = bos.toByteArray();
    }

    SubsidyDocxTextPatcher.ParseResult parsed = SubsidyDocxTextPatcher.parse(template);
    assertThat(parsed.lines()).extracting(SubsidyDocxTextPatcher.Line::kind)
        .contains("title", "meta", "caption");

    int timeIdx =
        parsed.lines().stream()
            .filter(l -> "培训时间".equals(l.label()))
            .findFirst()
            .orElseThrow()
            .index();
    byte[] out =
        SubsidyDocxTextPatcher.patch(
            template, Map.of(timeIdx, "培训时间：2026-08-18至2026-08-19"));
    SubsidyDocxTextPatcher.ParseResult after = SubsidyDocxTextPatcher.parse(out);
    assertThat(
            after.lines().stream()
                .filter(l -> "培训时间".equals(l.label()))
                .findFirst()
                .orElseThrow()
                .value())
        .isEqualTo("2026-08-18至2026-08-19");
  }

  @Test
  void parsesAndPatchesTableLabelValueCells() throws Exception {
    byte[] template;
    try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      para(doc, "职业技能培训补贴申请表");
      XWPFTable table = doc.createTable(2, 4);
      setCell(table.getRow(0).getCell(0), "培训机构");
      setCell(table.getRow(0).getCell(1), "广西现代职业技术学院");
      setCell(table.getRow(0).getCell(2), "培训时间");
      setCell(table.getRow(0).getCell(3), "2024/03/10至2024/04/03");
      setCell(table.getRow(1).getCell(0), "经办人姓名");
      setCell(table.getRow(1).getCell(1), "罗艳静");
      setCell(table.getRow(1).getCell(2), "联系电话");
      setCell(table.getRow(1).getCell(3), "18677892608");
      doc.write(bos);
      template = bos.toByteArray();
    }

    SubsidyDocxTextPatcher.ParseResult parsed = SubsidyDocxTextPatcher.parse(template);
    assertThat(parsed.lines()).extracting(SubsidyDocxTextPatcher.Line::label)
        .contains("培训机构", "培训时间", "经办人姓名", "联系电话");

    int orgIdx =
        parsed.lines().stream()
            .filter(l -> "培训机构".equals(l.label()))
            .findFirst()
            .orElseThrow()
            .index();
    assertThat(SubsidyDocxTextPatcher.isTableIndex(orgIdx)).isTrue();

    byte[] out = SubsidyDocxTextPatcher.patch(template, Map.of(orgIdx, "测试培训机构"));
    SubsidyDocxTextPatcher.ParseResult after = SubsidyDocxTextPatcher.parse(out);
    assertThat(
            after.lines().stream()
                .filter(l -> "培训机构".equals(l.label()))
                .findFirst()
                .orElseThrow()
                .value())
        .isEqualTo("测试培训机构");
  }

  private static void para(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    XWPFRun run = p.createRun();
    run.setText(text);
  }

  private static void setCell(XWPFTableCell cell, String text) {
    cell.removeParagraph(0);
    XWPFParagraph p = cell.addParagraph();
    p.createRun().setText(text);
  }
}
