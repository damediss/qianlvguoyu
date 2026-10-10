package com.guoyu.platform.exam.knowledgefix.dto;

import java.util.List;

/** 人工定夺请求（批量，≤200 行/次） */
public record FixAdjudicateBatchRequest(List<Long> rowIds, String action) {
}
