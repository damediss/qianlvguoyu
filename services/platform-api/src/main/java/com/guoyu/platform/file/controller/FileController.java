package com.guoyu.platform.file.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.DownloadUrlVO;
import com.guoyu.platform.file.vo.FileVO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

  private final FileApplicationService fileApplicationService;

  public FileController(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @AuditLog(module = "file", action = "file:upload", targetType = "file")
  public ApiResponse<FileVO> uploadBinary(
      @RequestBody byte[] content,
      @RequestHeader(value = "X-Original-Filename", required = false) String originalNameHeader,
      @RequestHeader(value = "X-Access-Policy", required = false) String accessPolicy,
      @RequestHeader(value = "X-Content-Type", required = false) String fileContentType,
      @RequestHeader(value = "Content-Type", required = false) String contentType
  ) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }
    String resolvedContentType = hasText(fileContentType)
        ? fileContentType.trim()
        : (hasText(contentType) ? contentType.trim() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
    return ApiResponse.ok(fileApplicationService.storeFromBytes(
        content,
        decodeFilename(originalNameHeader),
        resolvedContentType,
        accessPolicy));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @AuditLog(module = "file", action = "file:upload", targetType = "file")
  public ApiResponse<FileVO> upload(HttpServletRequest request) throws IOException, ServletException {
    Part filePart = null;
    String accessPolicy = null;
    String originalName = null;
    for (Part part : request.getParts()) {
      if ("file".equals(part.getName())) {
        filePart = part;
      } else if ("accessPolicy".equals(part.getName())) {
        accessPolicy = readPartAsString(part);
      } else if ("originalName".equals(part.getName())) {
        originalName = readPartAsString(part);
      }
    }
    if (filePart == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is required");
    }
    byte[] content;
    try (InputStream input = filePart.getInputStream()) {
      content = input.readAllBytes();
    }
    if (content.length == 0) {
      log.warn(
          "Upload rejected: partSize={}, contentType={}, submittedFileName={}",
          filePart.getSize(),
          filePart.getContentType(),
          filePart.getSubmittedFileName());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File is empty");
    }
    String storedName = hasText(originalName) ? originalName.trim() : filePart.getSubmittedFileName();
    return ApiResponse.ok(fileApplicationService.storeFromBytes(
        content,
        storedName,
        filePart.getContentType(),
        accessPolicy));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String readPartAsString(Part part) throws IOException {
    try (InputStream input = part.getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String decodeFilename(String header) {
    if (!hasText(header)) {
      return "upload.bin";
    }
    try {
      return URLDecoder.decode(header.trim(), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return header.trim();
    }
  }

  @GetMapping("/{id}")
  public ApiResponse<FileVO> get(@PathVariable Long id) {
    return ApiResponse.ok(fileApplicationService.get(id));
  }

  @GetMapping("/{id}/download-url")
  public ApiResponse<DownloadUrlVO> downloadUrl(@PathVariable Long id) {
    return ApiResponse.ok(fileApplicationService.downloadUrl(id));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(
      @PathVariable Long id,
      @RequestParam(value = "inline", defaultValue = "false") boolean inline
  ) {
    return fileApplicationService.buildDownloadResponse(id, inline);
  }

  @DeleteMapping("/{id}")
  @AuditLog(module = "file", action = "file:delete", targetType = "file")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    fileApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
