package com.guoyu.platform.integration.php.dto;

/** PHP 知识结构树节点（exam_knowledge_structure） */
public record PhpKnowledgeNode(
    long id,
    Long parentId,
    String title,
    Integer level,
    String important) {
}
