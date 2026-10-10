package com.guoyu.platform.exam.standard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 人工新增叶节点请求 */
public record StandardNodeCreateReq(
    @NotNull Long taskId,
    @NotNull Long parentId,
    @NotBlank @Size(max = 500) String title,
    String important,
    String dimension) {
}
