package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析人社/培训平台导出的「考勤信息列表」Excel（.xls/.xlsx）。
 */
@Component
public class TrainAttendanceImportSupport {

  private static final Set<String> REQUIRED_HEADERS = Set.of("身份证号码", "上课日期");

  private static final DateTimeFormatter[] DATE_FORMATS = {
      DateTimeFormatter.ofPattern("yyyy-MM-dd"),
      DateTimeFormatter.ofPattern("yyyy/M/d"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd"),
      DateTimeFormatter.ofPattern("yyyy年M月d日"),
      DateTimeFormatter.ofPattern("yyyy.M.d"),
  };

  private static final DateTimeFormatter[] DATE_TIME_FORMATS = {
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
      DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
  };

  private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

  public List<ParsedRow> parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考勤文件为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      if (workbook.getNumberOfSheets() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "考勤文件无有效工作表");
      }
      Sheet sheet = workbook.getSheetAt(0);
      HeaderHit header = findHeader(sheet);
      if (header == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "未识别到考勤表头，请确认包含「身份证号码」「上课日期」等列（人社/培训平台考勤信息列表）");
      }

      List<ParsedRow> rows = new ArrayList<>();
      for (int r = header.headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;
        String name = cell(row, header.col("姓名"));
        String idCard = normalizeIdCard(cell(row, header.col("身份证号码")));
        String dateRaw = cellDate(row, header.col("上课日期"));
        if (!StringUtils.hasText(name) && !StringUtils.hasText(idCard) && !StringUtils.hasText(dateRaw)) {
          continue;
        }
        ParsedRow pr = new ParsedRow();
        pr.excelRowNum = r + 1;
        pr.name = trimToEmpty(name);
        pr.idCard = idCard;
        pr.attendanceDate = normalizeDate(dateRaw);
        pr.startTime = normalizeTime(cellTime(row, header.col("上课时间")));
        pr.endTime = normalizeTime(cellTime(row, header.col("下课时间")));
        pr.late = yesFlag(cell(row, header.col("是否迟到")));
        pr.earlyLeave = yesFlag(cell(row, header.col("是否早退")));
        pr.absent = yesFlag(cell(row, header.col("是否旷课")));
        pr.checkInTime = normalizeDateTime(cellDateTime(row, header.col("签到时间")));
        pr.checkOutTime = normalizeDateTime(cellDateTime(row, header.col("签退时间")));
        pr.status = resolveStatus(pr);
        rows.add(pr);
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "考勤文件未解析到有效数据行");
      }
      return rows;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析考勤文件失败：" + ex.getMessage());
    }
  }

  /**
   * 同日多课次合并：状态取最差；签到取最早；签退取最晚；备注汇总课次。
   * @deprecated 导入已改为课次行落库；仅保留给无上课/下课时间的旧表兜底。
   */
  public List<MergedDay> mergeByPersonDay(List<ParsedRow> rows) {
    Map<String, MergedDay> map = new LinkedHashMap<>();
    for (ParsedRow row : rows) {
      if (!StringUtils.hasText(row.idCard) || !StringUtils.hasText(row.attendanceDate)) {
        continue;
      }
      String key = row.idCard + "|" + row.attendanceDate;
      MergedDay m = map.get(key);
      if (m == null) {
        m = new MergedDay();
        m.idCard = row.idCard;
        m.name = row.name;
        m.attendanceDate = row.attendanceDate;
        m.status = row.status;
        m.checkInTime = row.checkInTime;
        m.checkOutTime = row.checkOutTime;
        m.sourceRows = new ArrayList<>();
        m.sessions = new ArrayList<>();
        map.put(key, m);
      }
      m.sourceRows.add(row.excelRowNum);
      if (StringUtils.hasText(row.name)) m.name = row.name;
      m.status = worseStatus(m.status, row.status);
      m.checkInTime = earlierTime(m.checkInTime, row.checkInTime);
      m.checkOutTime = laterTime(m.checkOutTime, row.checkOutTime);
      if (StringUtils.hasText(row.startTime) || StringUtils.hasText(row.endTime)) {
        m.sessions.add(trimToEmpty(row.startTime) + "-" + trimToEmpty(row.endTime));
      }
    }
    for (MergedDay m : map.values()) {
      if (!m.sessions.isEmpty()) {
        String joined = String.join("；", m.sessions);
        if (joined.length() > 200) joined = joined.substring(0, 197) + "...";
        m.remark = "导入课次：" + joined;
      } else {
        m.remark = "Excel导入";
      }
    }
    return new ArrayList<>(map.values());
  }

  public static String normalizeIdCard(String raw) {
    if (!StringUtils.hasText(raw)) return "";
    return raw.trim().replace(" ", "").replace("\u3000", "").toUpperCase(Locale.ROOT);
  }

  /** 统一为 HH:mm，便于与课表匹配。 */
  public static String normalizeTimeHm(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String v = raw.trim();
    if (v.contains(" ")) {
      v = v.substring(v.lastIndexOf(' ') + 1);
    }
    if (v.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
      String[] p = v.split(":");
      String hh = p[0].length() == 1 ? "0" + p[0] : p[0];
      return hh + ":" + p[1];
    }
    if (v.matches("^\\d{3,4}$")) {
      if (v.length() == 3) {
        v = "0" + v;
      }
      return v.substring(0, 2) + ":" + v.substring(2);
    }
    return v;
  }

  public static String sessionMatchKey(String date, String startTime, String endTime) {
    return trimToEmpty(date) + "|" + normalizeTimeHm(startTime) + "|" + normalizeTimeHm(endTime);
  }

  private Integer resolveStatus(ParsedRow pr) {
    if (Boolean.TRUE.equals(pr.absent)) return 0;
    if (Boolean.TRUE.equals(pr.late)) return 2;
    if (Boolean.TRUE.equals(pr.earlyLeave)) return 3;
    if (StringUtils.hasText(pr.checkInTime) || StringUtils.hasText(pr.checkOutTime)) return 1;
    // 明确「旷课=否」且无迟到早退 → 出勤
    if (pr.absent != null && !pr.absent) return 1;
    return 0;
  }

  /** 缺勤(0) > 迟到(2) > 早退(3) > 出勤(1) */
  private Integer worseStatus(Integer a, Integer b) {
    if (a == null) return b;
    if (b == null) return a;
    return rank(a) >= rank(b) ? a : b;
  }

  private int rank(int status) {
    return switch (status) {
      case 0 -> 40;
      case 2 -> 30;
      case 3 -> 20;
      case 1 -> 10;
      default -> 0;
    };
  }

  private String earlierTime(String a, String b) {
    if (!StringUtils.hasText(a)) return b;
    if (!StringUtils.hasText(b)) return a;
    return a.compareTo(b) <= 0 ? a : b;
  }

  private String laterTime(String a, String b) {
    if (!StringUtils.hasText(a)) return b;
    if (!StringUtils.hasText(b)) return a;
    return a.compareTo(b) >= 0 ? a : b;
  }

  private HeaderHit findHeader(Sheet sheet) {
    int maxScan = Math.min(Math.max(sheet.getLastRowNum(), 0), 30);
    for (int i = 0; i <= maxScan; i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;
      Map<String, Integer> cols = new HashMap<>();
      short last = row.getLastCellNum();
      if (last < 0) continue;
      for (int c = 0; c < last; c++) {
        String h = normalizeHeader(cell(row, c));
        if (!StringUtils.hasText(h)) continue;
        cols.putIfAbsent(h, c);
      }
      boolean ok = true;
      for (String req : REQUIRED_HEADERS) {
        if (!cols.containsKey(req)) {
          ok = false;
          break;
        }
      }
      if (ok) {
        HeaderHit hit = new HeaderHit();
        hit.headerRowIdx = i;
        hit.cols = cols;
        return hit;
      }
    }
    return null;
  }

  private String normalizeHeader(String raw) {
    if (!StringUtils.hasText(raw)) return "";
    return raw.trim().replace(" ", "").replace("\u3000", "");
  }

  private Boolean yesFlag(String raw) {
    if (!StringUtils.hasText(raw)) return null;
    String v = raw.trim();
    if ("是".equals(v) || "Y".equalsIgnoreCase(v) || "YES".equalsIgnoreCase(v) || "1".equals(v)) {
      return true;
    }
    if ("否".equals(v) || "N".equalsIgnoreCase(v) || "NO".equalsIgnoreCase(v) || "0".equals(v)) {
      return false;
    }
    return null;
  }

  private String normalizeDate(String raw) {
    if (!StringUtils.hasText(raw)) return "";
    String v = raw.trim();
    if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
      return v.substring(0, 10);
    }
    for (DateTimeFormatter f : DATE_FORMATS) {
      try {
        return LocalDate.parse(v, f).toString();
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    // datetime string → date
    for (DateTimeFormatter f : DATE_TIME_FORMATS) {
      try {
        return LocalDateTime.parse(v, f).toLocalDate().toString();
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    return v;
  }

  private String normalizeDateTime(String raw) {
    if (!StringUtils.hasText(raw)) return "";
    String v = raw.trim();
    for (DateTimeFormatter f : DATE_TIME_FORMATS) {
      try {
        LocalDateTime dt = LocalDateTime.parse(v, f);
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    // only time HH:mm(:ss)
    if (v.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
      String[] p = v.split(":");
      String hh = p[0].length() == 1 ? "0" + p[0] : p[0];
      String mm = p[1];
      String ss = p.length > 2 ? p[2] : "00";
      return hh + ":" + mm + ":" + ss;
    }
    return v;
  }

  private String normalizeTime(String raw) {
    return normalizeTimeHm(raw);
  }

  private String cell(Row row, Integer col) {
    if (row == null || col == null) return "";
    Cell cell = row.getCell(col);
    if (cell == null) return "";
    return formatter.formatCellValue(cell).trim();
  }

  /** 上课/下课时间：数值日期单元格取时分，避免被格式成日期串。 */
  private String cellTime(Row row, Integer col) {
    if (row == null || col == null) return "";
    Cell cell = row.getCell(col);
    if (cell == null) return "";
    if (isNumericDateCell(cell)) {
      try {
        var ldt = cell.getLocalDateTimeCellValue();
        return String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
      } catch (Exception ignored) {
        // fall through
      }
    }
    return formatter.formatCellValue(cell).trim();
  }

  private String cellDate(Row row, Integer col) {
    if (row == null || col == null) return "";
    Cell cell = row.getCell(col);
    if (cell == null) return "";
    // 文本型日期不能先调 isCellDateFormatted（内部会按 NUMERIC 取值并抛错）
    if (isNumericDateCell(cell)) {
      try {
        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
      } catch (Exception ignored) {
        // fall through to formatter
      }
    }
    return formatter.formatCellValue(cell).trim();
  }

  private String cellDateTime(Row row, Integer col) {
    if (row == null || col == null) return "";
    Cell cell = row.getCell(col);
    if (cell == null) return "";
    if (isNumericDateCell(cell)) {
      try {
        return cell.getLocalDateTimeCellValue()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      } catch (Exception ignored) {
        // fall through to formatter
      }
    }
    return formatter.formatCellValue(cell).trim();
  }

  /** 仅对真正的数值/日期单元格调用 DateUtil，避免 STRING 单元格抛 IllegalStateException */
  private boolean isNumericDateCell(Cell cell) {
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    if (type != CellType.NUMERIC) {
      return false;
    }
    try {
      return DateUtil.isCellDateFormatted(cell);
    } catch (Exception ex) {
      return false;
    }
  }

  private static String trimToEmpty(String s) {
    return s == null ? "" : s.trim();
  }

  private static final class HeaderHit {
    int headerRowIdx;
    Map<String, Integer> cols;

    Integer col(String name) {
      return cols.get(name);
    }
  }

  public static final class ParsedRow {
    public int excelRowNum;
    public String name;
    public String idCard;
    public String attendanceDate;
    public String startTime;
    public String endTime;
    public Boolean late;
    public Boolean earlyLeave;
    public Boolean absent;
    public String checkInTime;
    public String checkOutTime;
    public Integer status;
  }

  public static final class MergedDay {
    public String idCard;
    public String name;
    public String attendanceDate;
    public Integer status;
    public String checkInTime;
    public String checkOutTime;
    public String remark;
    public List<Integer> sourceRows;
    public List<String> sessions;
  }
}
