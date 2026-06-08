package com.guoyu.platform.trainingdepartment.vo;

/** 默认模板导出进度查询结果 */
public record SignUpDefaultTemplateExportCheckVO(
    String url,
    String jindu,
    Integer error,
    String msg
) {}
