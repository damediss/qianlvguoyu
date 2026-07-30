package com.guoyu.platform.certification.archive.storage;

public record ArchiveFileObject(
    String fileName,
    String fileKey,
    long fileSize,
    String fileHash,
    String contentType
) {
}
