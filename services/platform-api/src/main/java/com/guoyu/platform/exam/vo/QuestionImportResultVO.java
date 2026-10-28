package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.support.QuestionImportIssue;
import java.util.List;

/**
 * 正式导入结果（方案 §14.4.4）：不再只返回数量，必须带回格式提醒明细。
 *
 * @param warnings 最多 50 条；warningCount 是总数，不因截断而减少
 */
public record QuestionImportResultVO(
    String fileSha256,
    int importedCount,
    int warningCount,
    List<QuestionImportIssue> warnings,
    boolean truncated) {
}
