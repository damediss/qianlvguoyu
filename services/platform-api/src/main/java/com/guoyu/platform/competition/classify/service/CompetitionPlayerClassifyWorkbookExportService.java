package com.guoyu.platform.competition.classify.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.classify.vo.CompetitionPlayerClassifyAssignmentVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class CompetitionPlayerClassifyWorkbookExportService {

  public byte[] export(String stageName, List<CompetitionPlayerClassifyAssignmentVO> assignments) {
    if (assignments == null || assignments.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "分类结果不完整，无法导出");
    }
    List<CompetitionPlayerClassifyAssignmentVO> ordered = assignments.stream()
        .sorted(displayOrder(assignments))
        .toList();
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("排序分类");
      Row title = sheet.createRow(0);
      title.createCell(0).setCellValue(
          (stageName == null || stageName.isBlank() ? "赛段" : stageName.trim()) + " 排序分类");
      Row header = sheet.createRow(1);
      String[] columns = {"顺序", "姓名", "分类", "参赛公司"};
      for (int index = 0; index < columns.length; index++) {
        header.createCell(index).setCellValue(columns[index]);
      }
      int rowIndex = 2;
      for (CompetitionPlayerClassifyAssignmentVO assignment : ordered) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(assignment.orderInType());
        row.createCell(1).setCellValue(text(assignment.name()));
        row.createCell(2).setCellValue(text(assignment.typeName()));
        row.createCell(3).setCellValue(text(assignment.orgName()));
      }
      int[] widths = {12, 16, 24, 36};
      for (int index = 0; index < widths.length; index++) {
        sheet.setColumnWidth(index, widths[index] * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类 Excel 生成失败");
    }
  }

  private Comparator<CompetitionPlayerClassifyAssignmentVO> displayOrder(
      List<CompetitionPlayerClassifyAssignmentVO> assignments) {
    if (isGlobalSequence(assignments)) {
      return Comparator.comparingInt(CompetitionPlayerClassifyAssignmentVO::orderInType);
    }
    return Comparator
        .comparingInt(CompetitionPlayerClassifyAssignmentVO::typeIndex)
        .thenComparingInt(CompetitionPlayerClassifyAssignmentVO::orderInType);
  }

  /** 新结果的顺序是不重复的 1..N。旧结果的顺序在每个类型内各自从 1 开始。 */
  private boolean isGlobalSequence(List<CompetitionPlayerClassifyAssignmentVO> assignments) {
    boolean[] seen = new boolean[assignments.size()];
    for (CompetitionPlayerClassifyAssignmentVO assignment : assignments) {
      int order = assignment.orderInType();
      if (order < 1 || order > assignments.size() || seen[order - 1]) {
        return false;
      }
      seen[order - 1] = true;
    }
    return true;
  }

  private String text(String value) {
    return value == null ? "" : value;
  }
}
