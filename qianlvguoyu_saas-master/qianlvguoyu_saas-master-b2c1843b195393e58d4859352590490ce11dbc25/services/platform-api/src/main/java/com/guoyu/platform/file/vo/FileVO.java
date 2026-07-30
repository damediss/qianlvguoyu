package com.guoyu.platform.file.vo;

import java.time.LocalDateTime;

public record FileVO(
    Long id,
    String originalName,
    String contentType,
    Long sizeBytes,
    String accessPolicy,
    String status,
    LocalDateTime createdAt
) {
}
