package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/** D6 导入记录（按 importId 轮询 / 按 examId 回溯）。 */
public record ZhentiImportLogVO(
    String importId,
    String status,
    Integer total,
    Integer finished,
    List<ZhentiImportFileResult> results
) {
}
