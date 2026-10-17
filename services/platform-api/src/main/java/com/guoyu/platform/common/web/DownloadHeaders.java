package com.guoyu.platform.common.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 下载导出响应头构造（中文文件名唯一正确姿势）。
 *
 * <p>禁止用 Spring {@code ContentDisposition.attachment().filename(name, charset)}：
 * 它生成 RFC 2047 编码词 {@code =?UTF-8?Q?...?=}，浏览器不解码 Content-Disposition 里的
 * RFC 2047（那是邮件头协议），下载名原样变成 {@code _=_UTF-8_Q_...} 乱码
 * （2026-10-16 知识点导出事故；试题导出侧 2026-09 已踩过并单点修复，本类收口全站）。
 *
 * <p>RFC 5987 {@code filename*=UTF-8''<percent-encoded>} 是唯一全浏览器兼容的写法；
 * 前端 parseContentDispositionFilename 与浏览器直连下载均已兼容。
 */
public final class DownloadHeaders {

  private DownloadHeaders() {
  }

  /** attachment 下载的 Content-Disposition 头值，中文文件名按 RFC 5987 百分号编码。 */
  public static String contentDispositionAttachment(String filename) {
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename*=UTF-8''" + encoded;
  }
}
