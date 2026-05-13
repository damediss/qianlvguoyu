package com.guoyu.platform.knowledge.service;

public record KnowledgeTextChunk(
    int chunkNo,
    String heading,
    Integer pageNo,
    String content,
    int tokenCount
) {
}
