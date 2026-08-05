package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRosterRow;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompetitionTheoryRosterExportService {

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final List<String> HEADERS = List.of(
      "赛事", "工种", "场次名称", "开始时间", "结束时间", "考场名称", "地址",
      "座位号", "参赛编号", "选手姓名", "所属企业", "签到");
  private final CompetitionTheoryScheduleApplicationService scheduleService;

  public CompetitionTheoryRosterExportService(
      CompetitionTheoryScheduleApplicationService scheduleService) {
    this.scheduleService = scheduleService;
  }

  public byte[] exportExcel(Long versionId) {
    List<CompetitionTheoryRosterRow> rows = requireRoster(versionId);
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      CellStyle headerStyle = headerStyle(workbook);
      int sheetIndex = 1;
      for (Map.Entry<RosterGroup, List<CompetitionTheoryRosterRow>> entry
          : group(rows).entrySet()) {
        RosterGroup group = entry.getKey();
        Sheet sheet = workbook.createSheet(uniqueSheetName(
            workbook, sheetIndex++ + "-" + group.sessionName() + "-" + group.roomName()));
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
          Cell cell = header.createCell(i);
          cell.setCellValue(HEADERS.get(i));
          cell.setCellStyle(headerStyle);
        }
        int rowIndex = 1;
        for (CompetitionTheoryRosterRow source : entry.getValue()) {
          Row row = sheet.createRow(rowIndex++);
          List<String> values = values(source);
          for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
          }
        }
        for (int i = 0; i < HEADERS.size(); i++) {
          sheet.autoSizeColumn(i);
          sheet.setColumnWidth(i, Math.min(12000, sheet.getColumnWidth(i) + 700));
        }
        sheet.createFreezePane(0, 1);
        sheet.setRepeatingRows(org.apache.poi.ss.util.CellRangeAddress.valueOf("1:1"));
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel考场名单生成失败");
    }
  }

  public byte[] exportPdf(Long versionId) {
    List<CompetitionTheoryRosterRow> rows = requireRoster(versionId);
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDFont font = loadChineseFont(document);
      for (Map.Entry<RosterGroup, List<CompetitionTheoryRosterRow>> entry
          : group(rows).entrySet()) {
        List<CompetitionTheoryRosterRow> groupRows = entry.getValue();
        for (int offset = 0; offset < groupRows.size(); offset += 22) {
          PDPage page = new PDPage(new PDRectangle(
              PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
          document.addPage(page);
          try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            drawPdfPage(
                content, font, entry.getKey(),
                groupRows.subList(offset, Math.min(offset + 22, groupRows.size())),
                offset / 22 + 1);
          }
        }
      }
      document.save(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF考场名单生成失败");
    }
  }

  private List<CompetitionTheoryRosterRow> requireRoster(Long versionId) {
    List<CompetitionTheoryRosterRow> rows = scheduleService.rosterRows(versionId, true);
    if (rows == null || rows.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "已发布安排没有可导出的选手名单");
    }
    return rows;
  }

  private Map<RosterGroup, List<CompetitionTheoryRosterRow>> group(
      List<CompetitionTheoryRosterRow> rows) {
    Map<RosterGroup, List<CompetitionTheoryRosterRow>> result = new LinkedHashMap<>();
    for (CompetitionTheoryRosterRow row : rows) {
      RosterGroup key = new RosterGroup(
          row.getSessionId(), safe(row.getSessionName()), safe(row.getRoomName()),
          safe(row.getAddress()), safe(row.getEventName()), safe(row.getWorkTypeName()),
          row.getStartAt() == null ? "-" : TIME_FORMAT.format(row.getStartAt()),
          row.getEndAt() == null ? "-" : TIME_FORMAT.format(row.getEndAt()));
      result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
    }
    return result;
  }

  private List<String> values(CompetitionTheoryRosterRow row) {
    return List.of(
        safe(row.getEventName()),
        safe(row.getWorkTypeName()),
        safe(row.getSessionName()),
        row.getStartAt() == null ? "-" : TIME_FORMAT.format(row.getStartAt()),
        row.getEndAt() == null ? "-" : TIME_FORMAT.format(row.getEndAt()),
        safe(row.getRoomName()),
        safe(row.getAddress()),
        safe(row.getSeatNo()),
        safe(row.getContestantNo()),
        safe(row.getContestantName()),
        safe(row.getOrgName()),
        "");
  }

  private CellStyle headerStyle(Workbook workbook) {
    Font font = workbook.createFont();
    font.setBold(true);
    CellStyle style = workbook.createCellStyle();
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setFillForegroundColor((short) 22);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private String uniqueSheetName(Workbook workbook, String raw) {
    String base = raw.replaceAll("[\\\\/?*\\[\\]:]", "-");
    base = base.length() > 31 ? base.substring(0, 31) : base;
    String candidate = base;
    int suffix = 2;
    while (workbook.getSheet(candidate) != null) {
      String marker = "-" + suffix++;
      candidate = base.substring(0, Math.min(base.length(), 31 - marker.length())) + marker;
    }
    return candidate;
  }

  private void drawPdfPage(
      PDPageContentStream content,
      PDFont font,
      RosterGroup group,
      List<CompetitionTheoryRosterRow> rows,
      int pageNumber) throws IOException {
    float margin = 28;
    float pageWidth = PDRectangle.A4.getHeight();
    drawText(content, font, 16, margin, 560,
        group.eventName() + " 理论考试考场名单");
    drawText(content, font, 10, margin, 540,
        "工种：" + group.workTypeName()
            + "    场次：" + group.sessionName()
            + "    时间：" + group.startAt() + " 至 " + group.endAt());
    drawText(content, font, 10, margin, 524,
        "考场：" + group.roomName() + "    地址：" + group.address()
            + "    第" + pageNumber + "页");

    List<String> headers = List.of("座位", "参赛编号", "姓名", "所属企业", "签到");
    float[] widths = {54, 110, 90, 390, 100};
    float y = 500;
    float rowHeight = 19;
    drawTableRow(content, font, headers, widths, margin, y, rowHeight, true);
    for (CompetitionTheoryRosterRow row : rows) {
      y -= rowHeight;
      drawTableRow(content, font, List.of(
          safe(row.getSeatNo()), safe(row.getContestantNo()),
          safe(row.getContestantName()), safe(row.getOrgName()), ""),
          widths, margin, y, rowHeight, false);
    }
  }

  private void drawTableRow(
      PDPageContentStream content,
      PDFont font,
      List<String> values,
      float[] widths,
      float x,
      float y,
      float height,
      boolean header) throws IOException {
    float cursor = x;
    for (int i = 0; i < widths.length; i++) {
      if (header) {
        content.setNonStrokingColor(new Color(230, 238, 250));
        content.addRect(cursor, y - height, widths[i], height);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);
      }
      content.addRect(cursor, y - height, widths[i], height);
      content.stroke();
      drawText(content, font, 9, cursor + 4, y - 13,
          truncate(values.get(i), font, 9, widths[i] - 8));
      cursor += widths[i];
    }
  }

  private void drawText(
      PDPageContentStream content,
      PDFont font,
      float fontSize,
      float x,
      float y,
      String value) throws IOException {
    content.beginText();
    content.setFont(font, fontSize);
    content.newLineAtOffset(x, y);
    content.showText(value);
    content.endText();
  }

  private String truncate(String value, PDFont font, float size, float maxWidth)
      throws IOException {
    String text = safe(value);
    if (font.getStringWidth(text) / 1000 * size <= maxWidth) {
      return text;
    }
    String suffix = "…";
    int end = text.length();
    while (end > 0
        && font.getStringWidth(text.substring(0, end) + suffix) / 1000 * size > maxWidth) {
      end--;
    }
    return end == 0 ? suffix : text.substring(0, end) + suffix;
  }

  private PDFont loadChineseFont(PDDocument document) throws IOException {
    String[] fonts = {
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/msyh.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc"
    };
    IOException last = null;
    for (String value : fonts) {
      Path path = Path.of(value);
      if (!Files.isRegularFile(path)) {
        continue;
      }
      try {
        return loadFont(document, path);
      } catch (IOException ex) {
        last = ex;
      }
    }
    throw new IOException("未找到可嵌入的中文字体", last);
  }

  private PDFont loadFont(PDDocument document, Path path) throws IOException {
    if (isCollection(path)) {
      try (InputStream input = Files.newInputStream(path);
          TrueTypeCollection collection = new TrueTypeCollection(input)) {
        PDFont[] loaded = new PDFont[1];
        IOException[] failure = new IOException[1];
        collection.processAllFonts(font -> {
          if (loaded[0] != null) {
            return;
          }
          try {
            loaded[0] = PDType0Font.load(document, font, true);
          } catch (IOException ex) {
            failure[0] = ex;
          }
        });
        if (loaded[0] != null) {
          return loaded[0];
        }
        throw new IOException("字体集合中没有可嵌入字体", failure[0]);
      }
    }
    try (InputStream input = Files.newInputStream(path)) {
      return PDType0Font.load(document, input, true);
    }
  }

  private boolean isCollection(Path path) throws IOException {
    try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
      return Arrays.equals(input.readNBytes(4), new byte[] {'t', 't', 'c', 'f'});
    }
  }

  private String safe(String value) {
    return StringUtils.hasText(value) ? value.trim() : "-";
  }

  private record RosterGroup(
      Long sessionId,
      String sessionName,
      String roomName,
      String address,
      String eventName,
      String workTypeName,
      String startAt,
      String endAt
  ) {
  }
}
