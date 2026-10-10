package com.guoyu.platform.certification.defense;

import com.guoyu.platform.certification.defense.DefenseScoreCatalog.PerformanceCategory;
import com.guoyu.platform.certification.defense.DefenseScoreCatalog.PerformanceItem;
import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformanceItemScore;
import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformancePayload;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ScoreComputation;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ScoreSheetView;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ThesisPayload;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.util.StringUtils;

final class DefenseScoreSheetDocxWriter {

  static final String THESIS_TEMPLATE = "templates/defense-thesis-score-sheet.docx";
  static final String PERFORMANCE_TEMPLATE = "templates/defense-performance-score-sheet.docx";

  private static final int SIGN_WIDTH_EMU = (int) Math.round(3.2 * Units.EMU_PER_CENTIMETER);
  private static final int SIGN_HEIGHT_EMU = (int) Math.round(1.2 * Units.EMU_PER_CENTIMETER);
  private static final String UNCHECKED = "口";
  private static final String CHECKED = "☑";

  void fill(
      XWPFDocument document,
      DefenseSummaryExportService.AssessorSheetExport sheet,
      boolean thesis,
      boolean withSignature
  ) {
    if (thesis) {
      fillThesis(document, sheet, withSignature);
    } else {
      fillPerformance(document, sheet, withSignature);
    }
  }

  void fillSignatureOnLastTable(
      XWPFDocument document,
      DefenseSummaryExportService.AssessorSheetExport sheet,
      boolean thesis
  ) {
    if (document.getTables().isEmpty()) {
      return;
    }
    XWPFTable last = document.getTables().get(document.getTables().size() - 1);
    fillSignatureRow(new XWPFTable(last.getCTTbl(), document), sheet, thesis);
  }

  private void fillThesis(
      XWPFDocument document,
      DefenseSummaryExportService.AssessorSheetExport sheet,
      boolean withSignature
  ) {
    ScoreSheetView view = sheet.thesis();
    ThesisPayload payload = view == null || view.thesis() == null
        ? DefenseScoreCatalog.emptyThesis()
        : DefenseScoreCatalog.normalizeThesis(view.thesis());
    ScoreComputation computed = DefenseScoreCatalog.computeThesis(payload, false);
    XWPFTable table = requireTable(document, "技术总结（论文）答辩评审评分表");
    setBodyParagraph(document, 1, "申报职业：" + text(sheet.workTypeName()) + "  级别：" + text(sheet.levelName()));
    setCellText(cell(table, 0, 1), text(sheet.candidateName()));
    setCellText(cell(table, 0, 3), text(sheet.admissionTicketNo()));
    setCellText(cell(table, 1, 1), text(payload.paperTitle()));
    fillRejectFlags(cell(table, 2, 1), payload);
    setCellText(cell(table, 3, 1), "否定说明：" + text(payload.rejectNote()));
    for (int i = 0; i < DefenseScoreCatalog.THESIS_CONTENT_ITEMS.size(); i++) {
      setCellText(cell(table, 5 + i, 3), formatScore(nth(payload.contentScores(), i)));
    }
    setCellText(cell(table, 12, 3), formatScore(computed.contentSubtotal()));
    List<String> questions = payload.questions() == null ? List.of() : payload.questions();
    for (int i = 0; i < 3; i++) {
      setCellText(cell(table, 14 + i, 2), (i + 1) + "." + text(nth(questions, i)));
    }
    for (int i = 0; i < DefenseScoreCatalog.THESIS_DEFENSE_ITEMS.size(); i++) {
      setCellText(cell(table, 18 + i, 3), formatScore(nth(payload.defenseScores(), i)));
    }
    setCellText(cell(table, 22, 3), formatScore(computed.defenseSubtotal()));
    setCellText(cell(table, 23, 2), formatScore(computed.finalScore()));
    if (withSignature) {
      fillSignatureRow(table, sheet, true);
    }
  }

  private void fillPerformance(
      XWPFDocument document,
      DefenseSummaryExportService.AssessorSheetExport sheet,
      boolean withSignature
  ) {
    ScoreSheetView view = sheet.performance();
    PerformancePayload payload = view == null || view.performance() == null
        ? DefenseScoreCatalog.emptyPerformance()
        : DefenseScoreCatalog.normalizePerformance(view.performance());
    ScoreComputation computed = DefenseScoreCatalog.computePerformance(payload, false);
    Map<String, Integer> scores = scoreMap(payload);
    XWPFTable table = requireTable(document, "技师、高级技师业绩评估报告评分表");
    setBodyParagraph(
        document,
        1,
        "申报职业（工种）：" + text(sheet.workTypeName()) + "       级别：" + text(sheet.levelName())
    );
    setCellText(cell(table, 0, 1), text(sheet.candidateName()));
    setCellText(cell(table, 0, 3), text(sheet.admissionTicketNo()));
    int row = 2;
    for (PerformanceCategory category : DefenseScoreCatalog.PERFORMANCE_CATEGORIES) {
      for (PerformanceItem item : category.items()) {
        setCellText(cell(table, row, 4), formatScore(scores.get(item.key())));
        row += 1;
      }
    }
    setCellText(cell(table, 11, 3), formatScore(computed.finalScore()));
    if (withSignature) {
      fillSignatureRow(table, sheet, false);
    }
  }

  private void fillSignatureRow(
      XWPFTable table,
      DefenseSummaryExportService.AssessorSheetExport sheet,
      boolean thesis
  ) {
    byte[] signature = thesis ? sheet.thesisSignature() : sheet.performanceSignature();
    fillSignature(cell(table, thesis ? 24 : 12, 1), signature);
  }

  private void fillRejectFlags(XWPFTableCell cell, ThesisPayload payload) {
    List<Boolean> flags = payload.rejectFlags();
    for (int i = 0; i < DefenseScoreCatalog.THESIS_REJECT_ITEMS.size(); i++) {
      boolean checked = flags != null && i < flags.size() && Boolean.TRUE.equals(flags.get(i));
      String text = (i + 1) + "." + (checked ? CHECKED : UNCHECKED)
          + DefenseScoreCatalog.THESIS_REJECT_ITEMS.get(i);
      XWPFParagraph paragraph = i < cell.getParagraphs().size() ? cell.getParagraphs().get(i) : cell.addParagraph();
      setParagraphText(paragraph, text);
    }
  }

  private void fillSignature(XWPFTableCell cell, byte[] signature) {
    XWPFParagraph paragraph = resetCell(cell);
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    paragraph.setSpacingBefore(0);
    paragraph.setSpacingAfter(0);
    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    (pr.isSetVAlign() ? pr.getVAlign() : pr.addNewVAlign()).setVal(STVerticalJc.CENTER);
    if (signature == null || signature.length == 0) {
      return;
    }
    try {
      insertPicture(paragraph.createRun(), signature);
    } catch (Exception ignored) {
      // leave the expert cell empty if the image cannot be embedded
    }
  }

  private static XWPFTable requireTable(XWPFDocument document, String title) {
    if (document.getTables().isEmpty()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, title + "模板缺少表格");
    }
    return document.getTables().get(0);
  }

  private static XWPFTableCell cell(XWPFTable table, int rowIndex, int cellIndex) {
    if (rowIndex >= table.getNumberOfRows()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分表模板行结构不正确");
    }
    XWPFTableRow row = table.getRow(rowIndex);
    if (cellIndex >= row.getTableCells().size()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分表模板列结构不正确");
    }
    return row.getCell(cellIndex);
  }

  private static void setBodyParagraph(XWPFDocument document, int index, String text) {
    int seen = 0;
    for (IBodyElement element : document.getBodyElements()) {
      if (element instanceof XWPFParagraph paragraph) {
        if (seen == index) {
          setParagraphText(paragraph, text);
          return;
        }
        seen += 1;
      }
    }
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评分表模板段落结构不正确");
  }

  private static void setCellText(XWPFTableCell cell, String text) {
    XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
    for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
      cell.removeParagraph(i);
    }
    setParagraphText(paragraph, text);
  }

  private static void setParagraphText(XWPFParagraph paragraph, String text) {
    String value = text == null ? "" : text;
    if (paragraph.getRuns().isEmpty()) {
      paragraph.createRun().setText(value);
      return;
    }
    paragraph.getRuns().get(0).setText(value, 0);
    for (int i = paragraph.getRuns().size() - 1; i > 0; i--) {
      paragraph.removeRun(i);
    }
  }

  private static XWPFParagraph resetCell(XWPFTableCell cell) {
    for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
      cell.removeParagraph(i);
    }
    if (cell.getParagraphs().isEmpty()) {
      cell.addParagraph();
    }
    XWPFParagraph paragraph = cell.getParagraphs().get(0);
    for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
      paragraph.removeRun(i);
    }
    return paragraph;
  }

  static void insertPicture(XWPFRun run, byte[] imageBytes) throws Exception {
    int type = pictureType(imageBytes);
    try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
      run.addPicture(in, type, "defense-sign.png", SIGN_WIDTH_EMU, SIGN_HEIGHT_EMU);
    }
  }

  static int pictureType(byte[] bytes) {
    if (bytes != null && bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
      return Document.PICTURE_TYPE_JPEG;
    }
    return Document.PICTURE_TYPE_PNG;
  }

  private static Map<String, Integer> scoreMap(PerformancePayload payload) {
    Map<String, Integer> scores = new HashMap<>();
    if (payload == null || payload.items() == null) {
      return scores;
    }
    for (PerformanceItemScore item : payload.items()) {
      if (item != null && StringUtils.hasText(item.key())) {
        scores.put(item.key(), item.score());
      }
    }
    return scores;
  }

  private static String formatScore(Number score) {
    if (score == null) {
      return "";
    }
    if (score instanceof Integer || score instanceof Long) {
      return String.valueOf(score.longValue());
    }
    return new java.math.BigDecimal(score.toString()).stripTrailingZeros().toPlainString();
  }

  private static <T> T nth(List<T> values, int index) {
    if (values == null || index < 0 || index >= values.size()) {
      return null;
    }
    return values.get(index);
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
