package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;
import java.util.Map;

/** 报名人员 Excel / 学员库批量导入结果 */
public record SignUpPeopleImportResultVO(
    int inserted,
    int skipped,
    int invalid,
    String filePath,
    List<String> errors,
    Map<String, Object> platformUserSync
) {
  public SignUpPeopleImportResultVO(
      int inserted, int skipped, int invalid, String filePath, List<String> errors) {
    this(inserted, skipped, invalid, filePath, errors, null);
  }
}
