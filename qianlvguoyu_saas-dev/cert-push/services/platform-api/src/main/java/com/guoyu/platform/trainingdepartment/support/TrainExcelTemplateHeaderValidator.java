package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
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

  private final TrainingStoredFileResolver fileResolver;
  private final DataFormatter formatter = new DataFormatter();

  public TrainExcelTemplateHeaderValidator(TrainingStoredFileResolver fileResolver) {
    this.fileResolver = fileResolver;
  }

  public void validate(String title, String pathSource) {
    if (!List.of("导入表", "开班花名册", "课时导入表").contains(title)) {
      return;
    }
    Path local = fileResolver.resolveLocalPath(pathSource);
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
    } catch (BusinessException ex) {
      fileResolver.deleteIfExists(pathSource);
      throw ex;
    } catch (Exception ex) {
      fileResolver.deleteIfExists(pathSource);
      throw new BusinessException(ErrorCode.BAD_REQUEST, title + "格式错误");
    }
  }

  private void validateImportSheet(Sheet sheet) {
    Row row = sheet.getRow(0);
    if (row == null) {
      throw badFormat("导入表");
    }
    String[] expected = {
        "人员类别", "人员补充身份", "公民身份证号码", "姓名", "户口性质", "民族", "文化程度",
        "联系手机", "家庭住址", "是否以工代训", "是否退伍军人", "是否党员", "开户银行",
        "银行账户名称", "银行账号", "就业意向", "所在单位名称(岗位技能培训必填)"
    };
    if (!matchRow(row, expected)) {
      throw badFormat("导入表");
    }
  }

  private void validateRosterSheet(Sheet sheet) {
    Row row = sheet.getRow(4);
    if (row == null) {
      throw badFormat("开班花名册");
    }
    String[] expected = {
        "序号", "姓名", "性别", "民族", "学历", "政治面貌", "身份证号码", "家庭住址",
        "联系电话", "社保号", "签订劳动合同时间(起、止)", "培训人员类别", "备注"
    };
    if (!matchRow(row, expected)) {
      throw badFormat("开班花名册");
    }
  }

  private void validateCourseHourSheet(Sheet sheet) {
    Row row = sheet.getRow(0);
    if (row == null) {
      throw badFormat("课时导入表");
    }
    String[] expected = {
        "教师(一)公民身份证号码", "教师(一)姓名", "授课形式", "上课日期(格式:YYYY-MM-DD)",
        "课程名称", "上课时间(格式:08:00)", "下课时间(格式:08:00)", "理论课时", "实操课时"
    };
    if (!matchRow(row, expected)) {
      throw badFormat("课时导入表");
    }
  }

  private boolean matchRow(Row row, String[] expected) {
    for (int i = 0; i < expected.length; i++) {
      String cell = cellText(row, i);
      if (!expected[i].equals(cell)) {
        return false;
      }
    }
    return true;
  }

  private String cellText(Row row, int col) {
    if (row.getCell(col) == null) {
      return "";
    }
    return formatter.formatCellValue(row.getCell(col)).trim();
  }

  private BusinessException badFormat(String title) {
    return new BusinessException(ErrorCode.BAD_REQUEST, title + "格式错误");
  }
}
