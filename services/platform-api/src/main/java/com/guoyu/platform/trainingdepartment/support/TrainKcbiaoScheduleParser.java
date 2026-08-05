package com.guoyu.platform.trainingdepartment.support;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析开班准备上传的课程表 Excel（与材料打包 {@code buildCourseHourSheet} 同一版式）：
 * 表头行第 2 列为「日期」；数据列：日期 / 时间段 / 课程名称 / 学时 / 授课类型 / 讲师。
 */
@Component
public class TrainKcbiaoScheduleParser {

  private static final DateTimeFormatter[] DATE_FORMATS = {
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("yyyy-M-d"),
      DateTimeFormatter.ofPattern("yyyy/M/d"),
      DateTimeFormatter.ofPattern("yyyy.M.d"),
      DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA),
  };

  private final DataFormatter formatter = new DataFormatter();

  public List<Map<String, Object>> parse(Path excelPath) throws Exception {
    try (InputStream in = Files.newInputStream(excelPath);
        Workbook wb = WorkbookFactory.create(in)) {
      return parseSheet(wb.getSheetAt(0));
    }
  }

  public List<Map<String, Object>> parseSheet(Sheet sheet) {
    List<Map<String, Object>> items = new ArrayList<>();
    if (sheet == null) {
      return items;
    }
    boolean afterHeader = false;
    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      String c0 = cellText(row, 0);
      String c1 = cellText(row, 1);
      if (c0.contains("合计") || c1.contains("合计")) {
        break;
      }
      if ("日期".equals(c1) || "日期".equals(c0)) {
        afterHeader = true;
        continue;
      }
      if (!afterHeader) {
        continue;
      }
      // 标准版式（与材料打包一致）：col1=日期 col2=时间 col3=课程 col6=讲师
      String dateRaw = cellText(row, 1);
      String timeRange = cellText(row, 2);
      String courseName = cellText(row, 3);
      String teacherName = cellText(row, 6);
      if (!StringUtils.hasText(dateRaw) && StringUtils.hasText(cellText(row, 0))) {
        // 退化：有的表把日期放在第一列
        dateRaw = cellText(row, 0);
        timeRange = cellText(row, 1);
        courseName = cellText(row, 2);
        teacherName = cellText(row, 5);
        if (!StringUtils.hasText(teacherName)) {
          teacherName = cellText(row, 4);
        }
      }
      if (!StringUtils.hasText(dateRaw) || !StringUtils.hasText(courseName)) {
        continue;
      }
      String scheduleDate = normalizeDate(dateRaw);
      if (scheduleDate == null) {
        continue;
      }
      String[] times = splitTimeRange(timeRange);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("scheduleDate", scheduleDate);
      item.put("dayOfWeek", dayOfWeek(scheduleDate));
      item.put("courseName", courseName.trim());
      item.put("startTime", times[0]);
      item.put("endTime", times[1]);
      if (StringUtils.hasText(teacherName)) {
        item.put("teacherName", teacherName.trim());
      }
      items.add(item);
    }
    return items;
  }

  private String[] splitTimeRange(String timeRange) {
    if (!StringUtils.hasText(timeRange)) {
      return new String[]{null, null};
    }
    String t = timeRange.trim().replace('～', '-').replace('—', '-').replace('–', '-');
    String[] parts = t.split("-");
    String start = parts.length > 0 ? normalizeTime(parts[0].trim()) : null;
    String end = parts.length > 1 ? normalizeTime(parts[1].trim()) : null;
    return new String[]{start, end};
  }

  private String normalizeTime(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim();
    // 830 → 08:30；8:30 → 08:30
    if (t.matches("^\\d{3,4}$")) {
      if (t.length() == 3) {
        t = "0" + t;
      }
      return t.substring(0, 2) + ":" + t.substring(2);
    }
    if (t.matches("^\\d{1,2}:\\d{2}$")) {
      String[] p = t.split(":");
      return String.format("%02d:%s", Integer.parseInt(p[0]), p[1]);
    }
    return t;
  }

  private String normalizeDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String s = raw.trim()
        .replace('年', '-')
        .replace('月', '-')
        .replace('日', ' ')
        .replace('/', '-')
        .replace('.', '-')
        .trim();
    if (s.endsWith("-")) {
      s = s.substring(0, s.length() - 1);
    }
    // 取日期部分（去掉时间）
    if (s.contains(" ")) {
      s = s.split("\\s+")[0];
    }
    for (DateTimeFormatter fmt : DATE_FORMATS) {
      try {
        return LocalDate.parse(s, fmt).toString();
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    // Excel 序列号偶发以纯数字出现
    if (s.matches("^\\d+(\\.\\d+)?$")) {
      try {
        double serial = Double.parseDouble(s);
        LocalDate epoch = LocalDate.of(1899, 12, 30);
        return epoch.plusDays((long) serial).toString();
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  private Integer dayOfWeek(String isoDate) {
    try {
      return LocalDate.parse(isoDate).getDayOfWeek().getValue();
    } catch (Exception e) {
      return null;
    }
  }

  private String cellText(Row row, int col) {
    if (row == null || row.getCell(col) == null) {
      return "";
    }
    return formatter.formatCellValue(row.getCell(col)).trim();
  }
}
