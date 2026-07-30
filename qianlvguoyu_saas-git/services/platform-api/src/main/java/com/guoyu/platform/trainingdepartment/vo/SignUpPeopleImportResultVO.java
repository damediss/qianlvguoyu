package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/** 报名人员 Excel 批量导入结果 */
public record SignUpPeopleImportResultVO(
    int inserted,
    int skipped,
    int invalid,
    String filePath,
    List<String> errors
) {}
