package com.guoyu.platform.exam.support;

import java.util.List;

/**
 * 单元格读取结果（方案 §14.4.2）。
 *
 * @param text           最终用于入库的文本（普通数值为 plain decimal、日期/百分比按格式显示）
 * @param rawDisplayText 单元格原始显示文本，仅用于报告
 * @param numberFormat   单元格数字格式，仅用于报告
 * @param issues         该单元格的硬错误/软警告
 */
public record ExcelCellValue(
    String text,
    String rawDisplayText,
    String numberFormat,
    List<QuestionImportIssue> issues) {

  public boolean hasError() {
    return issues != null && issues.stream().anyMatch(QuestionImportIssue::isError);
  }
}
