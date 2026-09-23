package com.guoyu.platform.file;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

/**
 * 抽象文件存储 —— local / COS / S3 走同一接口。
 *
 * 实现注意:
 *  - put 返回远端可访问 URL(COS 走 CDN prefix,local 走 /api/exam/files/*)
 *  - delete 是 best-effort,失败仅 warn 不抛
 *  - 写文件用 InputStream(可来自 MultipartFile.getInputStream()),不要求把整个 bytes 加载到内存
 */
public interface FileStorage {

  /**
   * 上传文件。
   *
   * @param objectKey 业务侧定的 object key(带 tenantId/日期前缀),例 "1/2026/06/uuid-x.xlsx"
   * @param content   文件流
   * @param sizeBytes 流大小(用于 Content-Length / COS 校验)
   * @param contentType MIME,可能为 null
   * @return 公网可访问 URL
   */
  String put(String objectKey, InputStream content, long sizeBytes, String contentType);

  /** 删除文件,失败不抛(只是 log warn)。 */
  void delete(String objectKey);

  /**
   * 生成免后端中转的直链下载 URL（COS 预签名等），供大文件直连下载、绕开服务端转发瓶颈。
   * 无法直链的存储（local 等）返回 empty，调用方回退 /api/files/{id}/download 中转。
   */
  default Optional<URL> presignedDownloadUrl(String objectKey, Duration ttl) {
    return Optional.empty();
  }

  /** 当前 provider 名,落库用(便于未来数据迁移识别)。 */
  String providerName();
}
