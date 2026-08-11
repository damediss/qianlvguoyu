package com.guoyu.platform.file.artifact.vo;

/** Trusted metadata used by an artifact resolver; IDs are strings for JavaScript safety. */
public record FileArtifactMetadataVO(
    String fileId,
    String filename,
    String mimeType,
    Long size,
    String sha256,
    String accessPolicy
) {
}
