package com.guoyu.platform.exam.standard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 节点编辑请求（标题/重要程度/权重/排序） */
public record StandardNodeUpdateReq(
    @Size(max = 500) String title,
    String important,
    @DecimalMin("0") BigDecimal weight,
    Integer sortNumber) {
}
