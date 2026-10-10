package com.guoyu.platform.exam.knowledgefix.dto;

/** 人工定夺请求（单行） */
public record FixAdjudicateRequest(Long rowId, String action) {
}
