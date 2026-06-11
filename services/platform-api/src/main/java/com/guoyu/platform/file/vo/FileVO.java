package com.guoyu.platform.file.vo;

import java.time.LocalDateTime;

public record FileVO(
    Long id,
    String originalName,
    String contentType,
    Long sizeBytes,
    String accessPolicy,
    String status,
    LocalDateTime createdAt,
    /** 公网可访问 URL(本地走 /api/exam/files/{objectKey},COS 走 bucket/CDN 公网 URL)。 */
    String url
) {
}
