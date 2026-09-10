package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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

  private static void para(XWPFDocument doc, String text) {
    XWPFParagraph p = doc.createParagraph();
    XWPFRun r = p.createRun();
    r.setText(text);
  }
}
