package com.guoyu.platform.competition.sorting.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      configureRosterColumns(rosterSheet);
      fillRoster(rosterSheet, assignments);
      fillWorkstations(workstationSheet, assignments, groupCount);
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
      copyRowStyle(styleRow, row, 7);
      setString(row, 0, groupLabel(assignment.groupNo()));
      setNumber(row, 1, assignment.groupOrder());
      setString(row, 2, assignment.workstation());
      setString(row, 3, assignment.name());
      setString(row, 4, assignment.mobile());
      setString(row, 5, "");
      setString(row, 6, "");
      rowIndex++;
    }
  }

  private void configureRosterColumns(Sheet sheet) {
    Row headerRow = sheet.getRow(4);
    if (headerRow == null) {
      return;
    }
    setString(headerRow, 3, "考生姓名");
    setString(headerRow, 4, "手机号");
    setString(headerRow, 5, "现场签到");
    setString(headerRow, 6, "备注");
    sheet.setColumnWidth(4, sheet.getColumnWidth(3));
    sheet.setColumnHidden(6, false);
  }

  private void fillWorkstations(
      Sheet sheet,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      int groupCount) {
    int firstDataRow = 5;
    int noteRow = findNoteRow(sheet);
    int lastDataRow = firstDataRow + groupCount - 1;
    if (noteRow >= firstDataRow && noteRow <= lastDataRow) {
      sheet.shiftRows(noteRow, sheet.getLastRowNum(), lastDataRow - noteRow + 1);
    }

    Map<Integer, String> workstationByGroup = new LinkedHashMap<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      workstationByGroup.putIfAbsent(assignment.groupNo(), assignment.workstation());
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
      setString(row, 1, workstationByGroup.getOrDefault(groupNo, ""));
      setString(row, 2, "");
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
