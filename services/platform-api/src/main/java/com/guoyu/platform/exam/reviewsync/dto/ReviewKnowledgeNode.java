package com.guoyu.platform.exam.reviewsync.dto;

/** 知识结构树节点（exam_knowledge_structure） */
public record ReviewKnowledgeNode(
    long id,
    Long parentId,
    String title,
    Integer level,
    String important) {
}
