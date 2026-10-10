package com.guoyu.platform.certification.defense;

import com.guoyu.platform.certification.defense.DefenseScoringModels.ScoreSheetView;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DefenseSummaryExportService {

  static final String TEMPLATE_PATH = "templates/技师、高级技师评价综合评定科目评审成绩汇总表.docx";
  private static final int HEADER_ROWS = 2;
  private static final int COL_SEQ = 0;
  private static final int COL_TICKET = 1;
  private static final int COL_NAME = 2;
  private static final int COL_ID_CARD = 3;
  private static final int COL_THESIS_EXPERT_1 = 4;
  private static final int COL_THESIS_AVG = 7;
  private static final int COL_PERF_EXPERT_1 = 8;
  private static final int COL_PERF_AVG = 11;
  private static final int COL_FINAL = 12;
  private static final int COL_VOTE = 13;

  public record ExpertSignImage(Long assessorId, String assessorName, byte[] imageBytes) {
  }

  public record AssessorSheetExport(
      String candidateName,
      String admissionTicketNo,
      String workTypeName,
      String levelName,
      String assessorName,
      ScoreSheetView thesis,
      ScoreSheetView performance,
      byte[] thesisSignature,
      byte[] performanceSignature
  ) {
  }

  public ExportResultDTO export(DefenseScoringModels.ProgressView progress) {
    return export(progress, List.of(), List.of());
  }

  public ExportResultDTO export(
      DefenseScoringModels.ProgressView progress,
      List<ExpertSignImage> expertSigns,
      List<AssessorSheetExport> sheets
  ) {
    if (progress == null || progress.candidates() == null || progress.candidates().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的答辩成绩");
    }
    try {
      byte[] summary = buildSummaryDocument(progress, expertSigns == null ? List.of() : expertSigns);
      if (sheets == null || sheets.isEmpty()) {
        return new ExportResultDTO(summary, summaryDocxName(progress));
      }
      return packZip(progress, summary, sheets);
    } catch (IOException ex) {
      // P2-2：异常类名与消息可能暴露内部路径/实现，只入日志不回显
      log.error("导出答辩成绩汇总表失败", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出答辩成绩汇总表失败，请稍后重试");
    }
  }

  private byte[] buildSummaryDocument(
      DefenseScoringModels.ProgressView progress,
      List<ExpertSignImage> expertSigns
  ) throws IOException {
    try (InputStream template = new ClassPathResource(TEMPLATE_PATH).getInputStream();
        XWPFDocument document = new XWPFDocument(template);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      fillHeader(document, progress);
      fillTable(document, progress);
      fillFooter(document, progress, expertSigns);
      document.write(output);
      return output.toByteArray();
    }
  }

  private ExportResultDTO packZip(
      DefenseScoringModels.ProgressView progress,
      byte[] summary,
      List<AssessorSheetExport> sheets
  ) throws IOException {
    LinkedHashMap<String, List<AssessorSheetExport>> byAssessor = new LinkedHashMap<>();
    for (AssessorSheetExport sheet : sheets) {
      if (sheet == null) {
        continue;
      }
      String key = StringUtils.hasText(sheet.assessorName()) ? sheet.assessorName() : "考评员";
      byAssessor.computeIfAbsent(key, ignored -> new ArrayList<>()).add(sheet);
    }
    ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(zipBuffer, StandardCharsets.UTF_8)) {
      int sequence = 1;
      putZipEntry(zip, padded(sequence++) + "-技师、高级技师评价综合评定科目评审成绩汇总表.docx", summary);
      for (Map.Entry<String, List<AssessorSheetExport>> entry : byAssessor.entrySet()) {
        String assessor = sanitizeFilePart(entry.getKey());
        putZipEntry(
            zip,
            padded(sequence++) + "-技术总结（论文）答辩评审评分表-" + assessor + ".docx",
            writeSheetDocument(entry.getValue(), true)
        );
        putZipEntry(
            zip,
            padded(sequence++) + "-技师、高级技师业绩评估报告评分表-" + assessor + ".docx",
            writeSheetDocument(entry.getValue(), false)
        );
      }
    }
    return new ExportResultDTO(zipBuffer.toByteArray(), summaryZipName(progress));
  }

  private byte[] writeSheetDocument(List<AssessorSheetExport> sheets, boolean thesis) throws IOException {
    String template = thesis
        ? DefenseScoreSheetDocxWriter.THESIS_TEMPLATE
        : DefenseScoreSheetDocxWriter.PERFORMANCE_TEMPLATE;
    List<AssessorSheetExport> valid = sheets.stream().filter(sheet -> sheet != null).toList();
    if (valid.isEmpty()) {
      return new byte[0];
    }
    try (XWPFDocument document = openTemplate(template);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ensurePortrait(document);
      DefenseScoreSheetDocxWriter writer = new DefenseScoreSheetDocxWriter();
      writer.fill(document, valid.get(0), thesis, true);
      for (int i = 1; i < valid.size(); i++) {
        try (XWPFDocument extra = openTemplate(template)) {
          writer.fill(extra, valid.get(i), thesis, false);
          appendBody(document, extra);
        }
        writer.fillSignatureOnLastTable(document, valid.get(i), thesis);
      }
      document.write(output);
      return output.toByteArray();
    }
  }

  static XWPFDocument openTemplate(String classpath) throws IOException {
    byte[] bytes = new ClassPathResource(classpath).getContentAsByteArray();
    if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
      throw new IOException("评分表模板不是有效的 Word 文件: " + classpath);
    }
    return new XWPFDocument(new ByteArrayInputStream(bytes));
  }

  private void fillHeader(XWPFDocument document, DefenseScoringModels.ProgressView progress) {
    String project = firstNonBlank(progress.taskName(), progress.taskNo());
    String header = "评审项目：" + emptyToDash(project)
        + "  评审时间：" + emptyToDash(progress.examDate())
        + "      评审地点：" + emptyToDash(progress.examSite());
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      if (paragraph.getText() != null && paragraph.getText().startsWith("评审项目")) {
        replaceParagraph(paragraph, header);
        break;
      }
    }
  }

  private void fillFooter(
      XWPFDocument document,
      DefenseScoringModels.ProgressView progress,
      List<ExpertSignImage> expertSigns
  ) {
    String occupation = "评审职业：" + emptyToDash(progress.workTypeName())
        + "                     等级：" + emptyToDash(summarizeCandidateLevels(progress))
        + "             登分员：";
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      String text = paragraph.getText();
      if (text == null) {
        continue;
      }
      if (text.startsWith("评审职业")) {
        replaceParagraph(paragraph, occupation);
      } else if (text.startsWith("评审专家")) {
        fillExpertSignatures(paragraph, progress, expertSigns);
      }
    }
  }

  private static String summarizeCandidateLevels(DefenseScoringModels.ProgressView progress) {
    if (progress.candidates() == null) {
      return "";
    }
    return progress.candidates().stream()
        .filter(candidate -> candidate != null)
        .map(DefenseScoringModels.CandidateProgressView::levelName)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .sorted(Comparator.comparingInt(DefenseSummaryExportService::candidateLevelOrder))
        .collect(Collectors.joining("、"));
  }

  private static int candidateLevelOrder(String levelName) {
    String normalized = levelName.replace(" ", "").replace("\u3000", "");
    return normalized.contains("高级技师") || normalized.contains("一级") ? 1 : 0;
  }

  private void fillExpertSignatures(
      XWPFParagraph paragraph,
      DefenseScoringModels.ProgressView progress,
      List<ExpertSignImage> expertSigns
  ) {
    replaceParagraph(paragraph, "评审专家：");
    if (expertSigns == null || expertSigns.isEmpty()) {
      String names = progress.experts().stream()
          .map(DefenseScoringModels.ExpertView::assessorName)
          .filter(StringUtils::hasText)
          .reduce((left, right) -> left + "、" + right)
          .orElse("");
      paragraph.getRuns().get(0).setText("评审专家：" + names, 0);
      return;
    }
    for (int i = 0; i < expertSigns.size(); i++) {
      ExpertSignImage sign = expertSigns.get(i);
      XWPFRun spacer = paragraph.createRun();
      spacer.setText(i == 0 ? " " : "　　");
      if (sign != null && sign.imageBytes() != null && sign.imageBytes().length > 0) {
        try {
          DefenseScoreSheetDocxWriter.insertPicture(paragraph.createRun(), sign.imageBytes());
        } catch (Exception ignored) {
          // keep the expert name even if the picture cannot be embedded
        }
      }
      XWPFRun name = paragraph.createRun();
      name.setText(sign == null ? "" : emptyToDash(sign.assessorName()));
    }
  }

  private static void putZipEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(data == null ? new byte[0] : data);
    zip.closeEntry();
  }

  private static void ensurePortrait(XWPFDocument document) {
    CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
        ? document.getDocument().getBody().getSectPr()
        : document.getDocument().getBody().addNewSectPr();
    CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
    pageSize.setOrient(STPageOrientation.PORTRAIT);
    if (pageSize.getW() == null || pageSize.getH() == null) {
      pageSize.setW(BigInteger.valueOf(11906));
      pageSize.setH(BigInteger.valueOf(16838));
    }
  }

  private static void appendBody(XWPFDocument target, XWPFDocument source) {
    boolean firstParagraph = true;
    for (IBodyElement element : List.copyOf(source.getBodyElements())) {
      if (element instanceof XWPFParagraph paragraph) {
        XWPFParagraph copy = target.createParagraph();
        copy.getCTP().set(paragraph.getCTP().copy());
        if (firstParagraph) {
          copy.setPageBreak(true);
          firstParagraph = false;
        }
      } else if (element instanceof XWPFTable table) {
        XWPFTable copy = target.createTable();
        copy.getCTTbl().set(table.getCTTbl().copy());
      }
    }
  }

  private static String summaryDocxName(DefenseScoringModels.ProgressView progress) {
    return "技师、高级技师评价综合评定科目评审成绩汇总表"
        + (StringUtils.hasText(progress.taskNo()) ? "_" + progress.taskNo() : "")
        + ".docx";
  }

  private static String summaryZipName(DefenseScoringModels.ProgressView progress) {
    return "技师、高级技师评价综合评定科目评审成绩汇总表"
        + (StringUtils.hasText(progress.taskNo()) ? "_" + progress.taskNo() : "")
        + ".zip";
  }

  private static String padded(int sequence) {
    return sequence < 10 ? "0" + sequence : String.valueOf(sequence);
  }

  private static String sanitizeFilePart(String value) {
    String cleaned = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    return StringUtils.hasText(cleaned) ? cleaned : "考评员";
  }

  private void fillTable(XWPFDocument document, DefenseScoringModels.ProgressView progress) {
    if (document.getTables().isEmpty()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答辩成绩汇总表模板缺少表格");
    }
    XWPFTable table = document.getTables().get(0);
    List<DefenseScoringModels.CandidateProgressView> candidates = progress.candidates();
    ensureDataRowCount(table, candidates.size());
    for (int i = 0; i < candidates.size(); i++) {
      fillDataRow(table.getRow(HEADER_ROWS + i), i + 1, candidates.get(i));
    }
  }

  private void ensureDataRowCount(XWPFTable table, int candidateCount) {
    if (table.getNumberOfRows() < HEADER_ROWS + 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答辩成绩汇总表模板行结构不正确");
    }
    XWPFTableRow templateRow = table.getRow(HEADER_ROWS);
    while (table.getNumberOfRows() < HEADER_ROWS + candidateCount) {
      insertClonedRow(table, templateRow, table.getNumberOfRows());
    }
    while (table.getNumberOfRows() > HEADER_ROWS + candidateCount) {
      table.removeRow(table.getNumberOfRows() - 1);
    }
  }

  private void insertClonedRow(XWPFTable table, XWPFTableRow source, int pos) {
    XWPFTableRow inserted = table.insertNewTableRow(pos);
    inserted.getCtRow().set(source.getCtRow());
    table.getRows().set(pos, new XWPFTableRow(inserted.getCtRow(), table));
  }

  private void fillDataRow(XWPFTableRow row, int sequence, DefenseScoringModels.CandidateProgressView candidate) {
    setCell(row, COL_SEQ, String.valueOf(sequence));
    setCell(row, COL_TICKET, firstNonBlank(candidate.admissionTicketNo(), candidate.candidateNo()));
    setCell(row, COL_NAME, emptyToDash(candidate.candidateName()));
    setCell(row, COL_ID_CARD, firstNonBlank(candidate.idCardNo(), candidate.maskedIdCardNo()));
    if ("absent".equals(candidate.status())) {
      for (int index = 0; index < DefenseScoreCatalog.EXPORT_EXPERT_COUNT; index++) {
        setCell(row, COL_THESIS_EXPERT_1 + index, "");
        setCell(row, COL_PERF_EXPERT_1 + index, "");
      }
      setCell(row, COL_THESIS_AVG, "");
      setCell(row, COL_PERF_AVG, "");
      setCell(row, COL_FINAL, "0");
      setCell(row, COL_VOTE, "");
      return;
    }
    for (int index = 0; index < DefenseScoreCatalog.EXPORT_EXPERT_COUNT; index++) {
      setCell(row, COL_THESIS_EXPERT_1 + index, formatScore(nth(candidate.thesisScores(), index)));
      setCell(row, COL_PERF_EXPERT_1 + index, formatScore(nth(candidate.performanceScores(), index)));
    }
    setCell(row, COL_THESIS_AVG, formatScore(candidate.thesisAverage()));
    setCell(row, COL_PERF_AVG, formatScore(candidate.performanceAverage()));
    if (Boolean.TRUE.equals(candidate.rejected())) {
      setCell(row, COL_FINAL, "不合格");
    } else {
      setCell(row, COL_FINAL, formatScore(candidate.finalScore()));
    }
    setCell(row, COL_VOTE, "");
  }

  private static BigDecimal nth(List<BigDecimal> scores, int index) {
    if (scores == null || index < 0 || index >= scores.size()) {
      return null;
    }
    return scores.get(index);
  }

  private static String formatScore(BigDecimal score) {
    if (score == null) {
      return "";
    }
    return score.stripTrailingZeros().toPlainString();
  }

  private static void setCell(XWPFTableRow row, int index, String value) {
    List<XWPFTableCell> cells = row.getTableCells();
    if (index < 0 || index >= cells.size()) {
      return;
    }
    XWPFTableCell cell = cells.get(index);
    if (cell.getParagraphs().isEmpty()) {
      cell.addParagraph();
    }
    XWPFParagraph paragraph = cell.getParagraphs().get(0);
    replaceParagraph(paragraph, value == null ? "" : value);
    for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
      cell.removeParagraph(i);
    }
  }

  private static void replaceParagraph(XWPFParagraph paragraph, String text) {
    List<XWPFRun> runs = paragraph.getRuns();
    if (runs == null || runs.isEmpty()) {
      paragraph.createRun().setText(text == null ? "" : text, 0);
      return;
    }
    runs.get(0).setText(text == null ? "" : text, 0);
    for (int i = runs.size() - 1; i > 0; i--) {
      paragraph.removeRun(i);
    }
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private static String emptyToDash(String value) {
    return StringUtils.hasText(value) ? value : "—";
  }
}
