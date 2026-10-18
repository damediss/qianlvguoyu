package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/** D6 历史回溯（按 examId）：导入记录分页。 */
public record ZhentiImportLogPageVO(
    List<ZhentiImportLogVO> records,
    long total,
    long page,
    long size
) {
}
