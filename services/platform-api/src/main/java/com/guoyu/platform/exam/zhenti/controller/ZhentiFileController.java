package com.guoyu.platform.exam.zhenti.controller;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import com.guoyu.platform.exam.zhenti.service.ZhentiSignatureService;
import com.guoyu.platform.exam.zhenti.service.ZhentiStorageService;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * F1 真题文件流（方案 §2.6）。
 *
 * <p>只信签名，不校验登录态；支持 Range（pdf.js 分片加载必需）。
 * deleted=1 → 404；下架仍可访问至签名过期；无签名/错签/过期 → 403。
 */
@RestController
@RequestMapping("/appzhenti/file")
@Slf4j
public class ZhentiFileController {

  private final ExamZhentiMapper mapper;
  private final ZhentiSignatureService signatureService;
  private final ZhentiStorageService storageService;

  public ZhentiFileController(
      ExamZhentiMapper mapper,
      ZhentiSignatureService signatureService,
      ZhentiStorageService storageService) {
    this.mapper = mapper;
    this.signatureService = signatureService;
    this.storageService = storageService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Resource> file(
      @PathVariable Long id,
      @RequestParam(value = "sign", required = false) String sign,
      @RequestParam(value = "t", required = false) Long expiresAt,
      @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
    signatureService.verify(id, sign, expiresAt);

    ExamZhentiEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "真题不存在或已删除");
    }
    long size = entity.getFileSize() == null ? 0L : entity.getFileSize();

    ByteRange range;
    try {
      range = parseRange(rangeHeader, size);
    } catch (UnsatisfiableRangeException ex) {
      return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
          .header(HttpHeaders.CONTENT_RANGE, "bytes */" + size)
          .build();
    }

    InputStream raw = storageService.open(entity.getFileKey());
    try {
      long start = range == null ? 0 : range.start();
      long length = range == null ? size : range.length();
      if (start > 0) {
        raw.skipNBytes(start);
      }
      InputStream body = range == null ? raw : new BoundedInputStream(raw, length);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.set(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(entity.getTitle()));
      headers.set(HttpHeaders.CACHE_CONTROL, "private, no-store");
      headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
      headers.setContentLength(length);
      if (range != null) {
        headers.set(HttpHeaders.CONTENT_RANGE,
            "bytes " + range.start() + "-" + range.end() + "/" + size);
      }
      HttpStatus status = range == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
      return new ResponseEntity<>(new InputStreamResource(body), headers, status);
    } catch (IOException ex) {
      closeQuietly(raw);
      log.warn("[ZhentiFile] 文件流读取失败 id={} err={}", id, ex.toString());
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件读取失败");
    } catch (RuntimeException ex) {
      closeQuietly(raw);
      throw ex;
    }
  }

  private static String inlineDisposition(String title) {
    String name = (title == null || title.isBlank() ? "真题" : title) + ".pdf";
    String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    return "inline; filename*=UTF-8''" + encoded;
  }

  private static void closeQuietly(InputStream in) {
    try {
      in.close();
    } catch (IOException ignored) {
      // ignore
    }
  }

  /**
   * 解析单段 Range；多段/非法语法返回 null（按 200 全量处理），越界抛 416。
   */
  static ByteRange parseRange(String header, long size) {
    if (header == null || size <= 0) {
      return null;
    }
    String value = header.trim();
    if (!value.regionMatches(true, 0, "bytes=", 0, 6) || value.contains(",")) {
      return null;
    }
    String spec = value.substring(6).trim();
    int dash = spec.indexOf('-');
    if (dash < 0) {
      return null;
    }
    String startText = spec.substring(0, dash).trim();
    String endText = spec.substring(dash + 1).trim();
    long start;
    long end;
    try {
      if (startText.isEmpty()) {
        long suffix = Long.parseLong(endText);
        if (suffix <= 0) {
          throw new UnsatisfiableRangeException();
        }
        start = Math.max(0, size - suffix);
        end = size - 1;
      } else {
        start = Long.parseLong(startText);
        end = endText.isEmpty() ? size - 1 : Long.parseLong(endText);
      }
    } catch (NumberFormatException ex) {
      return null;
    }
    if (start < 0 || start >= size || end < start) {
      throw new UnsatisfiableRangeException();
    }
    return new ByteRange(start, Math.min(end, size - 1));
  }

  record ByteRange(long start, long end) {
    long length() {
      return end - start + 1;
    }
  }

  static final class UnsatisfiableRangeException extends RuntimeException {
  }

  /** 限制读取长度的流：保证 206 响应体恰好等于 Content-Length。 */
  static final class BoundedInputStream extends FilterInputStream {

    private long remaining;

    BoundedInputStream(InputStream in, long limit) {
      super(in);
      this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
      if (remaining <= 0) {
        return -1;
      }
      int value = super.read();
      if (value >= 0) {
        remaining--;
      }
      return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      if (remaining <= 0) {
        return -1;
      }
      int read = super.read(buffer, offset, (int) Math.min(length, remaining));
      if (read > 0) {
        remaining -= read;
      }
      return read;
    }

    @Override
    public long skip(long count) throws IOException {
      long skipped = super.skip(Math.min(count, remaining));
      remaining -= skipped;
      return skipped;
    }
  }
}
