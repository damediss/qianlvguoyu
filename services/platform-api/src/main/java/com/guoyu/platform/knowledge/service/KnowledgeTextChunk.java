package com.guoyu.platform.knowledge.service;

import lombok.extern.slf4j.Slf4j;

public record KnowledgeTextChunk(
    int chunkNo,
    String heading,
    Integer pageNo,
    String content,
    int tokenCount
) {
}
