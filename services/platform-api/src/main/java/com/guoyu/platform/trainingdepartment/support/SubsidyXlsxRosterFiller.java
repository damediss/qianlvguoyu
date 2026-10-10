package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.util.StringUtils;

/**
 * 补贴 Excel 花名册逐行填数。
 *
 * <p>两种模板约定（自动识别）：
 * <ul>
 *   <li>占位符原型行：某一行含 {@code ${name}}/{@code ${idCard}} 等学员字段，按人数复制该行并逐行替换</li>
 *   <li>中文表头：扫描含「姓名」的表头行，按下表列名写入（对齐开班花名册）</li>
 * </ul>
 * 班级级 {@code ${company}} 等标量在逐行填完后统一替换。
 */
public final class SubsidyXlsxRosterFiller {

  /** 仅学员个人字段（班级标量仍参与全表二次替换） */
  public static final Set<String> PERSON_ONLY_PLACEHOLDERS = Set.of(
      "name", "realname", "idCard", "idcard", "phone", "gender", "nation", "education",
      "political", "address", "ticketNo", "ticket_no", "seq", "index", "no",
      "certificateNo", "certificate_no", "certNo",
      "theoryScore", "realScore", "examScore", "performanceScore",
      "issueDate", "issue_date", "issuingAuthority",
      "employment", "job", "category", "trainTarget", "train_target", "remark",
      "perPersonAmount");

  /** 识别为「学员行模板」的占位符（含班级级复用字段，便于原型行一并替换） */
  public static final Set<String> STUDENT_PLACEHOLDERS = Set.of(
      "name", "realname", "idCard", "idcard", "phone", "gender", "nation", "education",
      "political", "address", "ticketNo", "ticket_no", "seq", "index", "no",
      "certificateNo", "certificate_no", "certNo",
      "theoryScore", "realScore", "examScore", "performanceScore",
      "worktype", "work_type", "level", "company",
      "trainTarget", "train_target", "category", "banqi", "class_term", "class_title", "banqiname",
      "pxtime", "train_period", "date_start", "date_end",
      "issueDate", "issue_date", "issuingAuthority",
      "subsidy", "subsidyAmount", "subsidy_amount", "amount", "perPersonAmount",
      "trainSchool", "train_school", "pxdw", "school",
      "employment", "job", "remark");

  private SubsidyXlsxRosterFiller() {}

  public record FillResult(byte[] bytes, int studentCount, String mode) {}

  public static FillResult fill(
      byte[] templateBytes,
      List<Map<String, String>> studentRows,
      Map<String, String> classVars) throws IOException {
    if (templateBytes == null || templateBytes.length == 0) {
      throw new IOException("空 Excel 模板");
    }
    List<Map<String, String>> rows =
        studentRows == null ? List.of() : studentRows;
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      String mode = "scalar";
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        Integer proto = findPrototypeRow(sheet);
        if (proto != null) {
          expandPrototypeRow(sheet, proto, rows);
          mode = "placeholder_row";
          continue;
        }
        HeaderLayout header = findChineseHeader(sheet);
        if (header != null) {
          writeByChineseHeader(sheet, header, rows);
          mode = "header_map";
        }
      }
      // 班级标量：去掉仅个人字段，避免把已填姓名冲成空
      Map<String, String> scalars = new LinkedHashMap<>(classVars == null ? Map.of() : classVars);
      for (String sk : PERSON_ONLY_PLACEHOLDERS) {
        scalars.remove(sk);
      }
      List<Map.Entry<String, String>> ordered = new ArrayList<>(scalars.entrySet());
      ordered.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        for (Row row : sheet) {
          if (row == null) {
            continue;
          }
          for (Cell cell : row) {
            TrainingXlsxVariableReplacer.replaceCell(cell, ordered);
          }
        }
      }
      // 表头区「培训单位（盖章）：…培训时间： 至」等内联标签（非学员列）
      int metaHits = SubsidyXlsxLabelFiller.applyInlineMetas(workbook, classVars);
      if (metaHits > 0 && "header_map".equals(mode)) {
        mode = "header_map+meta";
      } else if (metaHits > 0 && "placeholder_row".equals(mode)) {
        mode = "placeholder_row+meta";
      } else if (metaHits > 0 && "scalar".equals(mode)) {
        mode = "scalar+meta";
      }
      workbook.write(out);
      return new FillResult(out.toByteArray(), rows.size(), mode);
    }
  }

  /** 是否应按花名册逐行模式处理 */
  public static boolean shouldUseRosterMode(String materialType, byte[] templateBytes) throws IOException {
    if (SubsidyMaterialTypes.ROSTER.equals(materialType)
        || SubsidyMaterialTypes.SUMMARY.equals(materialType)) {
      return true;
    }
    if (templateBytes == null || templateBytes.length == 0) {
      return false;
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        if (findPrototypeRow(sheet) != null || findChineseHeader(sheet) != null) {
          return true;
        }
      }
    }
    return false;
  }

  private static Integer findPrototypeRow(Sheet sheet) {
    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      for (Cell cell : row) {
        String text = TrainingXlsxVariableReplacer.cellText(cell);
        // 必须含学员个人字段，避免把仅有 ${class_title} 的标题行当原型行
        if (TrainingXlsxVariableReplacer.containsAnyPlaceholder(text, PERSON_ONLY_PLACEHOLDERS)) {
          return r;
        }
      }
    }
    return null;
  }

  private static void expandPrototypeRow(
      Sheet sheet, int protoRowIdx, List<Map<String, String>> students) {
    int need = Math.max(students.size(), 1);
    Row proto = sheet.getRow(protoRowIdx);
    if (proto == null) {
      return;
    }
    if (need > 1) {
      int extra = need - 1;
      int lastRow = sheet.getLastRowNum();
      List<CellRangeAddress> belowMerges = new ArrayList<>();
      for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
        CellRangeAddress region = sheet.getMergedRegion(i);
        if (region.getFirstRow() > protoRowIdx) {
          belowMerges.add(region);
          sheet.removeMergedRegion(i);
        }
      }
      if (protoRowIdx < lastRow) {
        sheet.shiftRows(protoRowIdx + 1, lastRow, extra, true, false);
      }
      short height = proto.getHeight();
      int lastCell = Math.max(proto.getLastCellNum(), 0);
      for (int i = 1; i < need; i++) {
        int rowIdx = protoRowIdx + i;
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
          row = sheet.createRow(rowIdx);
        }
        row.setHeight(height);
        for (int c = 0; c < lastCell; c++) {
          Cell src = proto.getCell(c);
          Cell dst = row.getCell(c);
          if (dst == null) {
            dst = row.createCell(c);
          }
          if (src != null) {
            copyCell(src, dst);
          }
        }
      }
      for (CellRangeAddress region : belowMerges) {
        sheet.addMergedRegion(new CellRangeAddress(
            region.getFirstRow() + extra,
            region.getLastRow() + extra,
            region.getFirstColumn(),
            region.getLastColumn()));
      }
    }

    List<Map.Entry<String, String>> emptyOrdered = List.of();
    for (int i = 0; i < need; i++) {
      Map<String, String> stu = i < students.size() ? students.get(i) : Map.of();
      Row row = sheet.getRow(protoRowIdx + i);
      if (row == null) {
        continue;
      }
      List<Map.Entry<String, String>> ordered = new ArrayList<>(stu.entrySet());
      ordered.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
      for (Cell cell : row) {
        TrainingXlsxVariableReplacer.replaceCell(cell, ordered.isEmpty() ? emptyOrdered : ordered);
      }
      // 清空仍残留的学员个人占位符
      List<Map.Entry<String, String>> clear =
          PERSON_ONLY_PLACEHOLDERS.stream().map(k -> Map.entry(k, "")).toList();
      for (Cell cell : row) {
        TrainingXlsxVariableReplacer.replaceCell(cell, clear);
      }
    }
  }

  private record HeaderLayout(int dataStartRow, Map<String, Integer> columns) {}

  private static HeaderLayout findChineseHeader(Sheet sheet) {
    for (int headerRowIdx = 0; headerRowIdx <= sheet.getLastRowNum(); headerRowIdx++) {
      Map<String, Integer> columns = mapChineseColumns(sheet.getRow(headerRowIdx));
      if (columns.containsKey("姓名") || columns.containsKey("身份证号码") || columns.containsKey("身份证")) {
        return new HeaderLayout(headerRowIdx + 1, columns);
      }
    }
    return null;
  }

  private static Map<String, Integer> mapChineseColumns(Row header) {
    Map<String, Integer> map = new HashMap<>();
    if (header == null) {
      return map;
    }
    short last = header.getLastCellNum();
    if (last < 0) {
      return map;
    }
    for (int c = 0; c < last; c++) {
      String v = TrainingXlsxVariableReplacer.cellText(header.getCell(c));
      if (!StringUtils.hasText(v)) {
        continue;
      }
      String t = v.trim().replaceAll("[\\s　\\r\\n]+", "");
      if (t.contains("序号") && !map.containsKey("序号")) {
        map.put("序号", c);
      } else if (t.equals("姓名") || t.startsWith("姓名")) {
        map.putIfAbsent("姓名", c);
      } else if (t.contains("性别")) {
        map.putIfAbsent("性别", c);
      } else if (t.contains("民族")) {
        map.putIfAbsent("民族", c);
      } else if (t.contains("学历") || t.contains("文化程度")) {
        map.putIfAbsent("学历", c);
      } else if (t.contains("政治面貌")) {
        map.putIfAbsent("政治面貌", c);
      } else if (t.contains("身份证")) {
        map.putIfAbsent("身份证号码", c);
      } else if (t.contains("家庭住址") || t.equals("住址") || t.equals("地址")) {
        map.putIfAbsent("家庭住址", c);
      } else if (t.contains("联系电话") || t.contains("手机") || t.equals("电话")) {
        map.putIfAbsent("联系电话", c);
      } else if (t.contains("准考证")) {
        map.putIfAbsent("准考证号", c);
      } else if (t.contains("培训对象") || t.contains("培训人员类别") || t.equals("人员类别")) {
        map.putIfAbsent("培训对象", c);
      } else if (t.contains("班期") || t.contains("班级名称") || t.equals("班级")) {
        map.putIfAbsent("班期", c);
      } else if (t.contains("培训起止") || t.contains("培训时间") || t.contains("起止时间")
          || (t.contains("开班") && t.contains("结业"))) {
        map.putIfAbsent("培训起止时间", c);
      } else if (t.contains("发证时间") || t.contains("发证日期") || t.contains("获证时间")
          || t.contains("取证时间") || t.contains("证书日期")) {
        map.putIfAbsent("发证时间", c);
      } else if (t.contains("培训补贴") || t.contains("补贴金额") || t.equals("补贴")) {
        map.putIfAbsent("培训补贴", c);
      } else if (t.contains("培训学校") || t.contains("培训机构") || t.contains("办学单位")
          || t.contains("承训单位") || t.contains("培训单位")) {
        map.putIfAbsent("培训学校名称", c);
      } else if (t.contains("就业情况") || t.contains("工作单位") || t.contains("就业单位")
          || t.contains("从事工种") || t.contains("岗位")) {
        map.putIfAbsent("就业情况", c);
      } else if (t.contains("证书编号") || t.equals("证号") || t.contains("证书号")) {
        map.putIfAbsent("证书编号", c);
      } else if ((t.contains("单位") && !t.contains("培训")) || t.contains("所在单位")) {
        map.putIfAbsent("单位", c);
      } else if (t.contains("工种") && !t.contains("等级")) {
        map.putIfAbsent("工种", c);
      } else if (t.contains("等级") && !t.contains("证书")) {
        map.putIfAbsent("等级", c);
      } else if (t.contains("理论成绩")) {
        map.putIfAbsent("理论成绩", c);
      } else if (t.contains("实操成绩") || t.contains("技能成绩")) {
        map.putIfAbsent("实操成绩", c);
      } else if (t.contains("综合成绩") || t.contains("考试成绩")) {
        map.putIfAbsent("综合成绩", c);
      } else if (t.contains("业绩成绩") || t.contains("工作业绩")) {
        map.putIfAbsent("业绩成绩", c);
      } else if (t.contains("备注")) {
        map.putIfAbsent("备注", c);
      }
    }
    return map;
  }

  private static void writeByChineseHeader(
      Sheet sheet, HeaderLayout layout, List<Map<String, String>> students) {
    int start = layout.dataStartRow();
    Map<String, Integer> col = layout.columns();
    // 保证有足够行（复制首行样式）
    Row proto = sheet.getRow(start);
    for (int i = 0; i < Math.max(students.size(), 1); i++) {
      int rowIdx = start + i;
      Row row = sheet.getRow(rowIdx);
      if (row == null) {
        row = sheet.createRow(rowIdx);
        if (proto != null && i > 0) {
          row.setHeight(proto.getHeight());
          short last = proto.getLastCellNum();
          for (int c = 0; c < last; c++) {
            Cell src = proto.getCell(c);
            if (src != null && src.getCellStyle() != null) {
              Cell dst = row.createCell(c);
              dst.setCellStyle(src.getCellStyle());
            }
          }
        }
      }
    }
    if (students.isEmpty()) {
      clearMapped(sheet.getRow(start), col);
      return;
    }
    for (int i = 0; i < students.size(); i++) {
      Map<String, String> stu = students.get(i);
      Row row = sheet.getRow(start + i);
      setCol(row, col.get("序号"), stu.getOrDefault("seq", String.valueOf(i + 1)));
      setCol(row, col.get("姓名"), first(stu, "name", "realname"));
      setCol(row, col.get("性别"), stu.get("gender"));
      setCol(row, col.get("民族"), stu.get("nation"));
      setCol(row, col.get("学历"), stu.get("education"));
      setCol(row, col.get("政治面貌"), stu.get("political"));
      setCol(row, col.get("身份证号码"), first(stu, "idCard", "idcard"));
      setCol(row, col.get("家庭住址"), stu.get("address"));
      setCol(row, col.get("联系电话"), stu.get("phone"));
      setCol(row, col.get("准考证号"), first(stu, "ticketNo", "ticket_no"));
      setCol(row, col.get("培训对象"), first(stu, "trainTarget", "train_target", "category"));
      setCol(row, col.get("班期"), first(stu, "banqi", "class_term", "class_title", "banqiname"));
      setCol(row, col.get("培训起止时间"), first(stu, "pxtime", "train_period"));
      setCol(row, col.get("发证时间"), first(stu, "issueDate", "issue_date"));
      setCol(row, col.get("培训补贴"), first(stu, "perPersonAmount", "subsidy", "subsidyAmount", "amount"));
      setCol(row, col.get("培训学校名称"), first(stu, "trainSchool", "train_school", "pxdw", "school"));
      setCol(row, col.get("就业情况"), first(stu, "employment", "job", "company"));
      setCol(row, col.get("单位"), first(stu, "company"));
      setCol(row, col.get("工种"), first(stu, "worktype", "work_type"));
      setCol(row, col.get("等级"), stu.get("level"));
      setCol(row, col.get("证书编号"), first(stu, "certificateNo", "certificate_no", "certNo"));
      setCol(row, col.get("理论成绩"), stu.get("theoryScore"));
      setCol(row, col.get("实操成绩"), stu.get("realScore"));
      setCol(row, col.get("综合成绩"), stu.get("examScore"));
      setCol(row, col.get("业绩成绩"), stu.get("performanceScore"));
      setCol(row, col.get("培训人员类别"), first(stu, "category", "trainTarget"));
      setCol(row, col.get("备注"), stu.get("remark"));
    }
  }

  private static void clearMapped(Row row, Map<String, Integer> col) {
    if (row == null || col == null) {
      return;
    }
    for (Integer c : col.values()) {
      if (c == null) {
        continue;
      }
      Cell cell = row.getCell(c);
      if (cell != null) {
        cell.setBlank();
      }
    }
  }

  private static void setCol(Row row, Integer col, String value) {
    if (row == null || col == null) {
      return;
    }
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    cell.setCellValue(value == null ? "" : value);
  }

  private static String first(Map<String, String> map, String... keys) {
    if (map == null) {
      return "";
    }
    for (String k : keys) {
      String v = map.get(k);
      if (StringUtils.hasText(v)) {
        return v;
      }
    }
    return "";
  }

  private static void copyCell(Cell src, Cell dst) {
    CellStyle style = src.getCellStyle();
    if (style != null) {
      dst.setCellStyle(style);
    }
    switch (src.getCellType()) {
      case STRING -> dst.setCellValue(src.getStringCellValue());
      case NUMERIC -> dst.setCellValue(src.getNumericCellValue());
      case BOOLEAN -> dst.setCellValue(src.getBooleanCellValue());
      case FORMULA -> dst.setCellFormula(src.getCellFormula());
      case BLANK -> dst.setBlank();
      default -> dst.setCellValue(TrainingXlsxVariableReplacer.cellText(src));
    }
  }
}
