package com.guoyu.platform.textbook.vo;

import java.math.BigDecimal;

/**
 * 大纲节点（扁平下发，前端自组树）。
 */
public record OutlineNodeVO(
    String id,
    String projectId,
    String parentId,
    Integer nodeLevel,
    String dimension,
    String sourceNo,
    String titleRaw,
    String title,
    BigDecimal weight,
    Integer targetWords,
    Integer wordsManual,
    Integer sortNo
) {
}
