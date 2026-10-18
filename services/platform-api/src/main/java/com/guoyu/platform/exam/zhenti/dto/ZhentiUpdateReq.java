package com.guoyu.platform.exam.zhenti.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** D4 局部更新：仅传要改的字段。 */
public record ZhentiUpdateReq(
    @NotNull(message = "id 不能为空") Long id,
    @Size(max = 200, message = "标题最多 200 字") String title,
    @Size(max = 10, message = "年份最多 10 字") String year,
    Integer sort,
    Integer status
) {
}
