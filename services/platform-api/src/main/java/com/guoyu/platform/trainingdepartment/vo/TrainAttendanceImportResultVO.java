package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/**
 * 跟班考勤 Excel 导入结果
 */
public record TrainAttendanceImportResultVO(
    int totalRows,
    int parsedRows,
    int created,
    int updated,
    int skipped,
    List<FailureItem> failures
) {
  public record FailureItem(
      int rowNum,
      String name,
      String idCard,
      String attendanceDate,
      String reason
  ) {}
}
