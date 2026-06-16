package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * 开班材料 Excel 表头校验 — 对齐 PHP YuntrainController::filetempsaveAction
 */
@Component
public class TrainExcelTemplateHeaderValidator {

  private static final int HEADER_SCAN_LIMIT = 10;

  private final TrainingStoredFileResolver fileResolver;
  private final org.apache.poi.ss.usermodel.DataFormatter formatter =
      new org.apache.poi.ss.usermodel.DataFormatter();

  public TrainExcelTemplateHeaderValidator(TrainingStoredFileResolver fileResolver) {
    this.fileResolver = fileResolver;
  }

  public void validate(String title, String pathSource) {
    if (!List.of("导入表", "开班花名册", "课时导入表").contains(title)) {
      return;
    }
    Path local = fileResolver.resolveLocalPath(pathSource);
    try {
      TrainingOfficeFileKind.Kind kind = TrainingOfficeFileKind.detect(local);
      if (kind == TrainingOfficeFileKind.Kind.WORD) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            title + "须上传 Excel 文件（.xlsx/.xls），不能上传 Word 文档");
      }
      if (kind != TrainingOfficeFileKind.Kind.EXCEL && kind != TrainingOfficeFileKind.Kind.LEGACY_EXCEL) {
        throw unreadableFile(title);
      }
      try (InputStream in = Files.newInputStream(local);
          Workbook workbook = WorkbookFactory.create(in)) {
        Sheet sheet = workbook.getSheetAt(0);
        if ("导入表".equals(title)) {
          validateImportSheet(sheet);
        } else if ("开班花名册".equals(title)) {
          validateRosterSheet(sheet);
        } else {
          validateCourseHourSheet(sheet);
        }
      }
    } catch (BusinessException ex) {
      fileResolver.deleteIfExists(pathSource);
      throw ex;
    } catch (Exception ex) {
      fileResolver.deleteIfExists(pathSource);
      throw unreadableFile(title);
    }
  }

  private void validateImportSheet(Sheet sheet) {
    String[] expected = {
        "人员类别", "人员补充身份", "公民身份证号码", "姓名", "户口性质", "民族", "文化程度",
        "联系手机", "家庭住址", "是否以工代训", "是否退伍军人", "是否党员", "开户银行",
        "银行账户名称", "银行账号", "就业意向", "所在单位名称(岗位技能培训必填)"
    };
    Row row = findHeaderRow(sheet, expected[0], 0);
    if (row == null) {
      throw badFormat("导入表", "未找到表头行，请确认第 1 行或前几行包含「人员类别」等标准列名");
    }
    assertRowMatches("导入表", row, expected);
  }

  private void validateRosterSheet(Sheet sheet) {
    String[] expected = {
        "序号", "姓名", "性别", "民族", "学历", "政治面貌", "身份证号码", "家庭住址",
        "联系电话", "社保号", "签订劳动合同时间(起、止)", "培训人员类别", "备注"
    };
    Row row = sheet.getRow(4);
    if (row == null || !matchRow(row, expected)) {
      throw badFormat("开班花名册", "第 5 行表头不符合标准，请使用人社部门提供的开班花名册模板");
    }
  }

  private void validateCourseHourSheet(Sheet sheet) {
    String[] expected = {
        "教师(一)公民身份证号码", "教师(一)姓名", "授课形式", "上课日期(格式:YYYY-MM-DD)",
        "课程名称", "上课时间(格式:08:00)", "下课时间(格式:08:00)", "理论课时", "实操课时"
    };
    Row row = findHeaderRow(sheet, expected[0], 0);
    if (row == null) {
      throw badFormat("课时导入表", "未找到表头行，请确认包含「教师(一)公民身份证号码」等标准列名");
    }
    assertRowMatches("课时导入表", row, expected);
  }

  private Row findHeaderRow(Sheet sheet, String firstHeader, int preferredRowIndex) {
    Row preferred = sheet.getRow(preferredRowIndex);
    if (preferred != null && normalizeHeader(readCell(preferred, 0)).equals(normalizeHeader(firstHeader))) {
      return preferred;
    }
    int lastRow = Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT - 1);
    for (int i = 0; i <= lastRow; i++) {
      Row row = sheet.getRow(i);
      if (row != null && normalizeHeader(readCell(row, 0)).equals(normalizeHeader(firstHeader))) {
        return row;
      }
    }
    return null;
  }

  private void assertRowMatches(String title, Row row, String[] expected) {
    for (int i = 0; i < expected.length; i++) {
      String actual = normalizeHeader(readCell(row, i));
      String expect = normalizeHeader(expected[i]);
      if (!expect.equals(actual)) {
        throw badFormat(title,
            "第 " + (i + 1) + " 列应为「" + expected[i] + "」，实际为「" + readCell(row, i) + "」");
      }
    }
  }

  private boolean matchRow(Row row, String[] expected) {
    for (int i = 0; i < expected.length; i++) {
      if (!normalizeHeader(expected[i]).equals(normalizeHeader(readCell(row, i)))) {
        return false;
      }
    }
    return true;
  }

  private String readCell(Row row, int col) {
    if (row.getCell(col) == null) {
      return "";
    }
    return formatter.formatCellValue(row.getCell(col)).trim();
  }

  private String normalizeHeader(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace('\uFEFF', ' ')
        .replace('（', '(')
        .replace('）', ')')
        .replace('\u00A0', ' ')
        .replaceAll("\\s+", "")
        .trim();
  }

  private BusinessException unreadableFile(String title) {
    return new BusinessException(ErrorCode.BAD_REQUEST,
        title + "文件无法读取，请重新上传标准 Excel 文件（.xlsx 或 .xls），"
            + "勿使用加密文件、快捷方式，或仅改后缀的非 Excel 文件");
  }

  private BusinessException badFormat(String title, String detail) {
    return new BusinessException(ErrorCode.BAD_REQUEST, title + "格式错误：" + detail);
  }
}
