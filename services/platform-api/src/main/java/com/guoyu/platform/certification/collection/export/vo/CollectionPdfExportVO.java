package com.guoyu.platform.certification.collection.export.vo;

/** 生成后已上传到平台文件服务的 PDF 文件信息。 */
public record CollectionPdfExportVO(
    Long fileId,
    String fileName,
    String downloadUrl
) {
}
