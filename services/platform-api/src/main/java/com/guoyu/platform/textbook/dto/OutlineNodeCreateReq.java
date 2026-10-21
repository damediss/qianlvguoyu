package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 人工新增大纲节点：父为章（2）→ 新增节（3）；父为节（3）→ 新增点（4，dimension 必填）。
 * source_no 恒空（人工补充，不溯源）。
 */
public record OutlineNodeCreateReq(
    @NotNull(message = "projectId 不能为空") Long projectId,
    @NotNull(message = "parentId 不能为空") Long parentId,
    @NotEmpty(message = "title 不能为空") @Size(max = 255, message = "title 过长") String title,
    /** 点级必填：theory/skill */
    String dimension,
    /** 仅新增节时可给初始字数（置 words_manual=1）；新增点忽略 */
    @Min(value = 0, message = "targetWords 不能为负") Integer targetWords
) {
}
