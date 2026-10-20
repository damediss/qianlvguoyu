package com.guoyu.platform.exam.zhenti.vo;

import java.time.LocalDateTime;

/**
 * 管理端真题行 VO。
 *
 * <p>id/examId 以字符串下发（方案 R2，防 JS 精度丢失）；file_key 永不出参。
 * scopeLabel：共享组展示（「工种 · 四级/中级工」），私有真题显示「本计划私有」。
 */
public record ZhentiVO(
    String id,
    String examId,
    String title,
    String year,
    Long fileSize,
    Integer sort,
    Integer status,
    Integer metaPending,
    String createdBy,
    String createdById,
    LocalDateTime createdAt,
    String scopeLabel
) {
}
