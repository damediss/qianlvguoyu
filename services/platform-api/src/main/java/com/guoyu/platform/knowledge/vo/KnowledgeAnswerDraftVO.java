package com.guoyu.platform.knowledge.vo;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeAnswerDraftVO(
    Long draftId,
    String answer,
    boolean evidenceSufficient,
    String modelName,
    List<KnowledgeSearchResultVO> citations,
    LocalDateTime createdAt
) {
}
