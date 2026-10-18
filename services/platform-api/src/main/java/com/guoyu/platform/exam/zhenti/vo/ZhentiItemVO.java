package com.guoyu.platform.exam.zhenti.vo;

/** App 端真题条目：仅暴露预览所需字段。 */
public record ZhentiItemVO(
    String id,
    String title,
    String year,
    Long fileSize
) {
}
