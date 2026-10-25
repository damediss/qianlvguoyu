package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.NotNull;

/** C4 取消分配请求：assignmentVersion 为当前分配版本（必传，防并发取消/改派错乱）。 */
public record TextbookAssignmentCancelReq(@NotNull Integer assignmentVersion) {
}
