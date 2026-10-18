package com.guoyu.platform.exam.zhenti.vo;

/** D2 上传成功出参。 */
public record ZhentiUploadVO(
    String id,
    String title,
    String year,
    Integer metaPending
) {
}
