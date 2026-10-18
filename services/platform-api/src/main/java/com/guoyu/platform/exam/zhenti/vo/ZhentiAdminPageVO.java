package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/** D1 列表出参：records/total/page/size（方案 R1）+ 当前考试项目已有年份去重。 */
public record ZhentiAdminPageVO(
    List<ZhentiVO> records,
    long total,
    long page,
    long size,
    List<String> years
) {
}
