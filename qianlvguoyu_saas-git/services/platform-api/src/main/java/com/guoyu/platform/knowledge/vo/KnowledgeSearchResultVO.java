package com.guoyu.platform.knowledge.vo;

public record KnowledgeSearchResultVO(
    Long chunkId,
    Long spaceId,
    String spaceName,
    Long documentId,
    String documentTitle,
    String heading,
    Integer pageNo,
    String content,
    double score,
    String citationLabel
) {
}
