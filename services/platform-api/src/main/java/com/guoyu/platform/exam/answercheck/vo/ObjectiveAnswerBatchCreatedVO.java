package com.guoyu.platform.exam.answercheck.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** 创建批次返回：秒回批次 ID（字符串，避免前端精度丢失）。 */
public record ObjectiveAnswerBatchCreatedVO(
    @JsonSerialize(using = ToStringSerializer.class) Long batchId) {
}
