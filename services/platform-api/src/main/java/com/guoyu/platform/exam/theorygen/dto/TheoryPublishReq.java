package com.guoyu.platform.exam.theorygen.dto;

/** 入库请求：batchId 必填；taskMode create|append；低于配额需 confirmedBelowQuota=true */
public record TheoryPublishReq(
    Long batchId,
    String taskMode,
    Long taskId,
    Long subTaskId,
    Boolean confirmedBelowQuota) {
}
