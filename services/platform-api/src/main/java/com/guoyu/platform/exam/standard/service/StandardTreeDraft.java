package com.guoyu.platform.exam.standard.service;

import java.math.BigDecimal;

/**
 * staging 树节点草稿（持久化前的纯结构，供规则单测对拍）。
 */
public record StandardTreeDraft(
    String nodeKey,
    String parentNodeKey,
    int nodeLevel,
    int sortNumber,
    String dimension,
    String title,
    String titleRaw,
    String important,
    BigDecimal weight,
    String relatedKnowledge,
    String skillRequirement,
    String source,
    String direction,
    int levelCode,
    boolean needLlm) {
}
