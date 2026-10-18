package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/** A1 App 端真题清单出参（records/total/page/size 与 R1 同口径）。 */
public record ZhentiAppListVO(
    List<ZhentiItemVO> records,
    long total,
    long page,
    long size
) {
}
