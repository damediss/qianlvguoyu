package com.guoyu.platform.exam.zhenti.dto;

import jakarta.validation.constraints.NotNull;

/** 共享组信息查询（方案 v2.1 §7 提示条）。 */
public record ZhentiScopeInfoReq(@NotNull Long examId) {
}
