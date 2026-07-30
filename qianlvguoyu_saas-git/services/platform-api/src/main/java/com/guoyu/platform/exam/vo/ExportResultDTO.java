package com.guoyu.platform.exam.vo;

/**
 * 导出结果 DTO，包含文件字节数组和描述性文件名。
 */
public record ExportResultDTO(byte[] data, String filename) {
}
