package com.guoyu.platform.exam.zhenti.dto;

import jakarta.validation.constraints.NotNull;

/** D5 删除 / 管理端预览 的 id 入参。 */
public record ZhentiIdReq(@NotNull(message = "id 不能为空") Long id) {
}
