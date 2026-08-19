package com.guoyu.platform.certification.defense;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
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

  public ExportResultDTO export(DefenseScoringModels.ProgressView progress) {
    if (progress == null || progress.candidates() == null || progress.candidates().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可导出的答辩成绩");
    }
    try (InputStream template = new ClassPathResource(TEMPLATE_PATH).getInputStream();
        XWPFDocument document = new XWPFDocument(template);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      fillHeader(document, progress);
      fillTable(document, progress);
      fillFooter(document, progress);
      document.write(output);
      String filename = "技师、高级技师评价综合评定科目评审成绩汇总表"
          + (StringUtils.hasText(progress.taskNo()) ? "_" + progress.taskNo() : "")
          + ".docx";
      return new ExportResultDTO(output.toByteArray(), filename);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出答辩成绩汇总表失败");
    }
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

  private void fillFooter(XWPFDocument document, DefenseScoringModels.ProgressView progress) {
    String occupation = "评审职业：" + emptyToDash(progress.workTypeName())
        + "                     等级：" + emptyToDash(progress.levelName())
        + "             登分员：";
    String experts = "评审专家：" + progress.experts().stream()
        .map(DefenseScoringModels.ExpertView::assessorName)
        .filter(StringUtils::hasText)
        .reduce((left, right) -> left + "、" + right)
        .orElse("");
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      String text = paragraph.getText();
      if (text == null) {
        continue;
      }
      if (text.startsWith("评审职业")) {
        replaceParagraph(paragraph, occupation);
      } else if (text.startsWith("评审专家")) {
        replaceParagraph(paragraph, experts);
      }
    }
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
