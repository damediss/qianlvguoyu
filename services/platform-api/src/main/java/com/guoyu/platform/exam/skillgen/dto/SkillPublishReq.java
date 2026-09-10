package com.guoyu.platform.exam.skillgen.dto;

/** 入库请求：batchId 必填；taskMode create|append（append 需 taskId） */
public record SkillPublishReq(
    Long batchId,
    String taskMode,
    Long taskId) {
}
