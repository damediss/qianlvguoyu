package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.support.QuestionImportIssue;
import java.util.List;

/**
 * 导入前只读预检结果（方案 §14.4.3）。
 *
 * <p>validRows 指无 hard error、可进入正式导入的行；一行可有多个 issue，
 * 因此 totalRows - validRows 不等于 errors.size()，hardErrorCount 按 issue 计数。
 */
public record QuestionImportValidateVO(
    String fileSha256,
    int totalRows,
    int validRows,
    int hardErrorCount,
    int warningCount,
    List<QuestionImportIssue> errors,
    List<QuestionImportIssue> warnings,
    boolean truncated) {
}
