package com.guoyu.platform.exam.dto;

/** 模型配置查询请求 */
public record FaqModelConfigQueryReq(
    String keyword,
    String status,
    Long page,
    Long pageSize
) {
}
