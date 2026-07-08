package com.guoyu.platform.certification.collection.vo;

/**
 * 二维码导出结果，包含生成的 Excel 字节内容和文件名。
 */
public record QrCodeExportVO(byte[] data, String filename) {
}
