package com.guoyu.platform.competition.sorting.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompetitionPlayerSortingWorkbookExportService {

  private static final String TEMPLATE_PATH =
      "templates/competition-practical-sorting-template.xlsx";

  public byte[] export(
      String stageName,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      int groupCount) {
    return export(stageName, assignments, groupCount, null, List.of());
  }

  public byte[] export(
      String stageName,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      int groupCount,
      String workstationLabel,
      List<String> workstationCodes) {
    return export(stageName, assignments, groupCount, workstationLabel, workstationCodes, null);
  }

  public byte[] export(
      String stageName,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      int groupCount,
      String workstationLabel,
      List<String> workstationCodes,
      String subWorkstationLabel) {
    ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
    try (InputStream input = resource.getInputStream();
        Workbook workbook = WorkbookFactory.create(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet rosterSheet = workbook.getSheet("实操出场名单");
      Sheet workstationSheet = workbook.getSheet("分组工位配置");
      if (rosterSheet == null || workstationSheet == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实操出场名单模板缺少工作表");
      }
      setTitle(rosterSheet, stageName);
      insertRoleColumn(rosterSheet);
      configureRosterColumns(rosterSheet, workstationLabel, subWorkstationLabel);
      fillRoster(rosterSheet, assignments);
      fillWorkstations(workstationSheet, assignments, groupCount, workstationLabel, workstationCodes);
      workbook.write(output);
      return output.toByteArray();
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实操出场名单 Excel 生成失败");
    }
  }

  private void setTitle(Sheet sheet, String stageName) {
    if (!StringUtils.hasText(stageName)) {
      return;
    }
    Row titleRow = sheet.getRow(1);
    if (titleRow == null) {
      return;
    }
    Cell title = titleRow.getCell(0);
    if (title != null) {
      title.setCellValue(stageName.trim() + " 实操出场名单");
    }
  }

  private void fillRoster(
      Sheet sheet, List<CompetitionPlayerSortingAssignmentVO> assignments) {
    Row styleRow = sheet.getRow(5);
    int rowIndex = 5;
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        row = sheet.createRow(rowIndex);
      }
      copyRowStyle(styleRow, row, 9);
      setString(row, 0, groupTitle(assignment));
      setNumber(row, 1, assignment.groupOrder());
      setString(row, 2, assignment.workstation());
      setString(row, 3, assignment.name());
      setString(row, 4, assignment.role());
      setString(row, 5, leaderLabel(assignment.groupLeader()));
      setString(row, 6, assignment.mobile());
      setString(row, 7, assignment.orgName() == null ? "" : assignment.orgName());
      setString(row, 8, assignment.subWorkstation());
      rowIndex++;
    }
  }

  private void configureRosterColumns(Sheet sheet, String workstationLabel, String subWorkstationLabel) {
    Row headerRow = sheet.getRow(4);
    if (headerRow == null) {
      return;
    }
    setString(headerRow, 0, "组名");
    setString(headerRow, 2, StringUtils.hasText(workstationLabel) ? workstationLabel.trim() : "对应工位");
    setString(headerRow, 3, "考生姓名");
    setString(headerRow, 4, "组内角色");
    setString(headerRow, 5, "组长");
    setString(headerRow, 6, "手机号");
    setString(headerRow, 7, "参赛公司");
    setString(headerRow, 8, StringUtils.hasText(subWorkstationLabel) ? subWorkstationLabel.trim() : "子工位");
    sheet.setColumnWidth(8, 16 * 256);
    sheet.setColumnHidden(8, false);
    sheet.setColumnWidth(5, 12 * 256);
    sheet.setColumnWidth(6, Math.max(sheet.getColumnWidth(6), sheet.getColumnWidth(3)));
    sheet.setColumnWidth(7, 24 * 256);
    sheet.setColumnHidden(7, false);
  }

  /** 在考生姓名右侧插入角色列，原手机号、签到、备注顺延。 */
  private void insertRoleColumn(Sheet sheet) {
    sheet.shiftColumns(4, 6, 1);
    copyCellStyle(sheet.getRow(4), 3, 4);
    copyCellStyle(sheet.getRow(5), 3, 4);
    sheet.setColumnWidth(4, 12 * 256);
  }

  private void copyCellStyle(Row row, int fromColumn, int toColumn) {
    if (row == null) {
      return;
    }
    Cell source = row.getCell(fromColumn);
    if (source == null) {
      return;
    }
    Cell target = row.getCell(toColumn);
    if (target == null) {
      target = row.createCell(toColumn);
    }
    target.setCellStyle(source.getCellStyle());
  }

  private String leaderLabel(Boolean groupLeader) {
    if (groupLeader == null) {
      return "";
    }
    return groupLeader ? "组长" : "";
  }

  private String joinStations(CompetitionPlayerSortingAssignmentVO assignment) {
    if (!StringUtils.hasText(assignment.subWorkstation())) {
      return assignment.workstation() == null ? "" : assignment.workstation();
    }
    String main = assignment.workstation() == null ? "" : assignment.workstation();
    return main + " / " + assignment.subWorkstation();
  }

  private String groupTitle(CompetitionPlayerSortingAssignmentVO assignment) {
    if (StringUtils.hasText(assignment.groupName())) {
      return assignment.groupName().trim();
    }
    return groupLabel(assignment.groupNo());
  }

  private void fillWorkstations(
      Sheet sheet,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      int groupCount,
      String workstationLabel,
      List<String> workstationCodes) {
    if (hasGroupWorkstation(assignments)) {
      fillGroupDrawResult(sheet, assignments, workstationLabel);
      return;
    }
    if (workstationCodes != null && !workstationCodes.isEmpty()) {
      fillCreatedWorkstations(sheet, workstationLabel, workstationCodes);
      return;
    }
    int firstDataRow = 5;
    int noteRow = findNoteRow(sheet);
    int lastDataRow = firstDataRow + groupCount - 1;
    if (noteRow >= firstDataRow && noteRow <= lastDataRow) {
      sheet.shiftRows(noteRow, sheet.getLastRowNum(), lastDataRow - noteRow + 1);
    }

    Map<Integer, Set<String>> workstationByGroup = new LinkedHashMap<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      workstationByGroup
          .computeIfAbsent(assignment.groupNo(), key -> new LinkedHashSet<>())
          .add(assignment.workstation() == null ? "" : assignment.workstation());
    }
    Row styleRow = sheet.getRow(firstDataRow);
    for (int groupNo = 1; groupNo <= groupCount; groupNo++) {
      int rowIndex = firstDataRow + groupNo - 1;
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        row = sheet.createRow(rowIndex);
      }
      copyRowStyle(styleRow, row, 3);
      setString(row, 0, groupLabel(groupNo));
      setString(row, 1, String.join("、",
          workstationByGroup.getOrDefault(groupNo, Set.of())));
      setString(row, 2, "");
    }
  }

  private boolean hasGroupWorkstation(List<CompetitionPlayerSortingAssignmentVO> assignments) {
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      if (StringUtils.hasText(assignment.groupName()) && StringUtils.hasText(assignment.workstation())) {
        return true;
      }
    }
    return false;
  }

  /** 每个组一行，工位属于整组而不是单个选手。 */
  private void fillGroupDrawResult(
      Sheet sheet,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      String workstationLabel) {
    Row headerRow = sheet.getRow(4);
    if (headerRow != null) {
      setString(headerRow, 0, "组名");
      setString(headerRow, 1, "参赛公司");
      setString(headerRow, 2, StringUtils.hasText(workstationLabel) ? workstationLabel.trim() : "工位");
    }
    Map<Integer, CompetitionPlayerSortingAssignmentVO> firstByGroup = new LinkedHashMap<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      firstByGroup.putIfAbsent(assignment.groupNo(), assignment);
    }
    Row styleRow = sheet.getRow(5);
    int rowIndex = 5;
    for (CompetitionPlayerSortingAssignmentVO assignment : firstByGroup.values()) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        row = sheet.createRow(rowIndex);
      }
      copyRowStyle(styleRow, row, 3);
      setString(row, 0, groupTitle(assignment));
      setString(row, 1, assignment.orgName() == null ? "" : assignment.orgName());
      setString(row, 2, joinStations(assignment));
      rowIndex++;
    }
  }

  private void fillCreatedWorkstations(
      Sheet sheet, String workstationLabel, List<String> workstationCodes) {
    Row headerRow = sheet.getRow(4);
    if (headerRow != null) {
      setString(headerRow, 0, "工位名称");
      setString(headerRow, 1, "工位编号");
    }
    Row styleRow = sheet.getRow(5);
    int rowIndex = 5;
    for (String code : workstationCodes) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        row = sheet.createRow(rowIndex);
      }
      copyRowStyle(styleRow, row, 3);
      setString(row, 0, StringUtils.hasText(workstationLabel) ? workstationLabel.trim() : "");
      setString(row, 1, code);
      setString(row, 2, "");
      rowIndex++;
    }
  }

  private int findNoteRow(Sheet sheet) {
    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      Cell cell = row == null ? null : row.getCell(0);
      if (cell != null && cell.getCellType() == CellType.STRING
          && cell.getStringCellValue().contains("提示：")) {
        return rowIndex;
      }
    }
    return -1;
  }

  private void copyRowStyle(Row source, Row target, int columnCount) {
    if (source == null || source == target) {
      return;
    }
    target.setHeight(source.getHeight());
    for (int column = 0; column < columnCount; column++) {
      Cell sourceCell = source.getCell(column);
      if (sourceCell == null) {
        continue;
      }
      Cell targetCell = target.getCell(column);
      if (targetCell == null) {
        targetCell = target.createCell(column);
      }
      targetCell.setCellStyle(sourceCell.getCellStyle());
    }
  }

  private void setString(Row row, int column, String value) {
    Cell cell = row.getCell(column);
    if (cell == null) {
      cell = row.createCell(column);
    }
    cell.setCellValue(value == null ? "" : value);
  }

  private void setNumber(Row row, int column, int value) {
    Cell cell = row.getCell(column);
    if (cell == null) {
      cell = row.createCell(column);
    }
    cell.setCellValue(value);
  }

  private String groupLabel(int groupNo) {
    return "第" + groupNo + "组";
  }
}
