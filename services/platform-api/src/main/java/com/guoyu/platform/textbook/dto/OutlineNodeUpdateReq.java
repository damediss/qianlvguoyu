package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 大纲节点编辑：改标题/目标字数（置 words_manual=1）/同级排序。
 */
public record OutlineNodeUpdateReq(
    @Size(max = 255, message = "title 过长") String title,
    /** 仅章/节可设；设置后 words_manual=1 */
    @Min(value = 0, message = "targetWords 不能为负") Integer targetWords,
    /** 同级排序值（上移/下移由前端按兄弟节点换算后提交） */
    @Min(value = 0, message = "sortNo 不能为负") Integer sortNo
) {
}
