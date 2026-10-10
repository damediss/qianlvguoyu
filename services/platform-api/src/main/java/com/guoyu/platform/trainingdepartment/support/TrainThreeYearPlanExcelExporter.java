package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanMatrixEntity;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanDistributionVO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TrainThreeYearPlanExcelExporter {

  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public byte[] exportReviewedPlan(
      TrainThreeYearPlanEntity plan,
      OrgEntity targetOrg,
      List<TrainThreeYearPlanMatrixEntity> matrixRows,
      List<TrainThreeYearPlanItemEntity> items,
      List<TrainThreeYearPlanDistributionVO> distribution) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeSummarySheet(workbook, plan, targetOrg, items.size());
      writeMatrixSheet(workbook, matrixRows);
      writeDistributionSheet(workbook, distribution);
      writeAllItemsSheet(workbook, items);
      writeYearDetailSheets(workbook, plan, items);
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("生成已审核规划表失败: " + e.getMessage(), e);
    }
  }

  private void writeSummarySheet(
      Workbook workbook,
      TrainThreeYearPlanEntity plan,
      OrgEntity targetOrg,
      int itemCount) {
    Sheet sheet = workbook.createSheet("规划信息");
    String[][] rows = {
        {"规划编号", safe(plan.getPlanNo())},
        {"规划标题", safe(plan.getTitle())},
        {"目标企业", targetOrg != null ? safe(targetOrg.getName()) : ""},
        {"规划年份", safe(plan.getPlanYears())},
        {"人员明细数", String.valueOf(itemCount)},
        {"导出时间", TIME_FMT.format(LocalDateTime.now())},
        {"说明", "本表依据机构审核修改后的本地数据生成，用于企业审核确认"}
    };
    for (int i = 0; i < rows.length; i++) {
      Row row = sheet.createRow(i);
      row.createCell(0).setCellValue(rows[i][0]);
      row.createCell(1).setCellValue(rows[i][1]);
    }
    sheet.setColumnWidth(0, 14 * 256);
    sheet.setColumnWidth(1, 48 * 256);
  }

  private void writeMatrixSheet(Workbook workbook, List<TrainThreeYearPlanMatrixEntity> matrixRows) {
    Sheet sheet = workbook.createSheet("矩阵汇总");
    String[] headers = {"工种", "规划年", "培训方式", "等级", "人数"};
    writeHeader(sheet, headers);
    List<TrainThreeYearPlanMatrixEntity> sorted = matrixRows.stream()
        .sorted(Comparator
            .comparing(TrainThreeYearPlanMatrixEntity::getPlanYear, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TrainThreeYearPlanMatrixEntity::getJobName, Comparator.nullsLast(String::compareTo))
            .thenComparing(TrainThreeYearPlanMatrixEntity::getLevelName, Comparator.nullsLast(String::compareTo)))
        .toList();
    int rowIdx = 1;
    for (TrainThreeYearPlanMatrixEntity item : sorted) {
      Row row = sheet.createRow(rowIdx++);
      row.createCell(0).setCellValue(safe(item.getJobName()));
      setInt(row, 1, item.getPlanYear());
      row.createCell(2).setCellValue(displayMode(item.getTrainMode()));
      row.createCell(3).setCellValue(safe(item.getLevelName()));
      setInt(row, 4, item.getHeadcount());
    }
    autosize(sheet, headers.length);
  }

  private void writeDistributionSheet(Workbook workbook, List<TrainThreeYearPlanDistributionVO> distribution) {
    Sheet sheet = workbook.createSheet("认定分布");
    String[] headers = {"工种", "等级", "人数", "开班阈值", "达阈值", "提示"};
    writeHeader(sheet, headers);
    int rowIdx = 1;
    for (TrainThreeYearPlanDistributionVO item : distribution) {
      Row row = sheet.createRow(rowIdx++);
      row.createCell(0).setCellValue(safe(item.jobName()));
      row.createCell(1).setCellValue(safe(item.levelName()));
      setInt(row, 2, item.employeeCount());
      setInt(row, 3, item.thresholdMin());
      row.createCell(4).setCellValue(Boolean.TRUE.equals(item.meetsThreshold()) ? "是" : "否");
      row.createCell(5).setCellValue(safe(item.warning()));
    }
    autosize(sheet, headers.length);
  }

  private void writeAllItemsSheet(Workbook workbook, List<TrainThreeYearPlanItemEntity> items) {
    Sheet sheet = workbook.createSheet("人员明细");
    String[] headers = {
        "序号", "姓名", "证件号", "单位", "工种", "等级", "培训方式",
        "规划年", "培训年", "认定年", "认定时间", "数据来源"
    };
    writeHeader(sheet, headers);
    List<TrainThreeYearPlanItemEntity> sorted = items.stream()
        .sorted(Comparator
            .comparing(TrainThreeYearPlanItemEntity::getPlanYear, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TrainThreeYearPlanItemEntity::getRowIndex, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TrainThreeYearPlanItemEntity::getPersonName, Comparator.nullsLast(String::compareTo)))
        .toList();
    int rowIdx = 1;
    int seq = 1;
    for (TrainThreeYearPlanItemEntity item : sorted) {
      Row row = sheet.createRow(rowIdx++);
      setInt(row, 0, seq++);
      row.createCell(1).setCellValue(safe(item.getPersonName()));
      row.createCell(2).setCellValue(safe(item.getIdCard()));
      row.createCell(3).setCellValue(safe(item.getCompanyName()));
      row.createCell(4).setCellValue(safe(item.getJobName()));
      row.createCell(5).setCellValue(safe(item.getLevelName()));
      row.createCell(6).setCellValue(displayMode(item.getTrainMode()));
      setInt(row, 7, item.getPlanYear());
      setInt(row, 8, item.getTrainYear());
      setInt(row, 9, item.getCertYear());
      row.createCell(10).setCellValue(safe(item.getEligibleDate()));
      row.createCell(11).setCellValue(sourceLabel(item.getSource()));
    }
    autosize(sheet, headers.length);
  }

  private void writeYearDetailSheets(
      Workbook workbook,
      TrainThreeYearPlanEntity plan,
      List<TrainThreeYearPlanItemEntity> items) {
    List<Integer> years = parsePlanYears(plan.getPlanYears());
    Map<Integer, List<TrainThreeYearPlanItemEntity>> byYear = items.stream()
        .filter(item -> item.getPlanYear() != null)
        .collect(Collectors.groupingBy(TrainThreeYearPlanItemEntity::getPlanYear, LinkedHashMap::new, Collectors.toList()));
    for (Integer year : years) {
      List<TrainThreeYearPlanItemEntity> yearItems = byYear.getOrDefault(year, List.of());
      Sheet sheet = workbook.createSheet("明细_" + year);
      String[] headers = {"序号", "姓名", "证件号", "单位", "工种", "等级", "培训方式", "认定时间", "备注"};
      writeHeader(sheet, headers);
      List<TrainThreeYearPlanItemEntity> sorted = yearItems.stream()
          .sorted(Comparator
              .comparing((TrainThreeYearPlanItemEntity item) -> displayMode(item.getTrainMode()))
              .thenComparing(TrainThreeYearPlanItemEntity::getJobName, Comparator.nullsLast(String::compareTo))
              .thenComparing(TrainThreeYearPlanItemEntity::getPersonName, Comparator.nullsLast(String::compareTo)))
          .toList();
      int rowIdx = 1;
      int seq = 1;
      for (TrainThreeYearPlanItemEntity item : sorted) {
        Row row = sheet.createRow(rowIdx++);
        setInt(row, 0, seq++);
        row.createCell(1).setCellValue(safe(item.getPersonName()));
        row.createCell(2).setCellValue(safe(item.getIdCard()));
        row.createCell(3).setCellValue(safe(item.getCompanyName()));
        row.createCell(4).setCellValue(safe(item.getJobName()));
        row.createCell(5).setCellValue(safe(item.getLevelName()));
        row.createCell(6).setCellValue(displayMode(item.getTrainMode()));
        row.createCell(7).setCellValue(safe(item.getEligibleDate()));
        row.createCell(8).setCellValue("manual".equals(item.getSource()) ? "人工修改" : "");
      }
      autosize(sheet, headers.length);
    }
  }

  private List<Integer> parsePlanYears(String planYears) {
    if (!StringUtils.hasText(planYears)) {
      return List.of(2026, 2027, 2028);
    }
    List<Integer> years = new ArrayList<>();
    for (String part : planYears.split(",")) {
      try {
        years.add(Integer.parseInt(part.trim()));
      } catch (NumberFormatException ignored) {
        // skip invalid token
      }
    }
    return years.isEmpty() ? List.of(2026, 2027, 2028) : years;
  }

  private void writeHeader(Sheet sheet, String[] headers) {
    Row header = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      header.createCell(i).setCellValue(headers[i]);
    }
  }

  private void autosize(Sheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      sheet.autoSizeColumn(i);
      int width = sheet.getColumnWidth(i);
      sheet.setColumnWidth(i, Math.min(width + 512, 48 * 256));
    }
  }

  private void setInt(Row row, int col, Integer value) {
    if (value != null) {
      row.createCell(col).setCellValue(value);
    } else {
      row.createCell(col).setCellValue("");
    }
  }

  private String displayMode(String mode) {
    return StringUtils.hasText(mode) ? mode.trim() : "自费认定";
  }

  private String sourceLabel(String source) {
    if ("manual".equals(source)) {
      return "人工修改";
    }
    if ("engine".equals(source)) {
      return "引擎识别";
    }
    return safe(source);
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
