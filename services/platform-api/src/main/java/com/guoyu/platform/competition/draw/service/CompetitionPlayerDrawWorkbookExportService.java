package com.guoyu.platform.competition.draw.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawSlot;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class CompetitionPlayerDrawWorkbookExportService {

  public byte[] export(String stageName, List<CompetitionPlayerDrawSlot> slots) {
    List<CompetitionPlayerDrawSlot> ordered = slots.stream()
        .sorted(Comparator.comparingInt(CompetitionPlayerDrawSlot::appearanceOrder))
        .toList();
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("抽签出场名单");
      Row title = sheet.createRow(0);
      title.createCell(0).setCellValue(
          (stageName == null || stageName.isBlank() ? "抽签" : stageName.trim()) + " 抽签出场名单");
      Row header = sheet.createRow(1);
      String[] columns = {"出场顺序", "考生姓名", "所属企业", "手机号", "选手编号"};
      for (int index = 0; index < columns.length; index++) {
        header.createCell(index).setCellValue(columns[index]);
      }
      int rowIndex = 2;
      for (CompetitionPlayerDrawSlot slot : ordered) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(slot.appearanceOrder());
        row.createCell(1).setCellValue(text(slot.name()));
        row.createCell(2).setCellValue(text(slot.orgName()));
        row.createCell(3).setCellValue(text(slot.mobile()));
        row.createCell(4).setCellValue(text(slot.contestantNo()));
      }
      for (int index = 0; index < columns.length; index++) {
        sheet.setColumnWidth(index, 18 * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签出场名单 Excel 生成失败");
    }
  }

  private String text(String value) {
    return value == null ? "" : value;
  }
}
