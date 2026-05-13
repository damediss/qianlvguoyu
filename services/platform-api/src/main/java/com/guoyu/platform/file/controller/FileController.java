package com.guoyu.platform.file.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.DownloadUrlVO;
import com.guoyu.platform.file.vo.FileVO;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/files")
public class FileController {

  private final FileApplicationService fileApplicationService;

  public FileController(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  @PostMapping
  @AuditLog(module = "file", action = "file:upload", targetType = "file")
  public ApiResponse<FileVO> upload(
      @RequestParam("file") @NotNull MultipartFile file,
      @RequestParam(value = "accessPolicy", required = false) String accessPolicy
  ) {
    return ApiResponse.ok(fileApplicationService.upload(file, accessPolicy));
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
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    Resource resource = fileApplicationService.downloadResource(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }

  @DeleteMapping("/{id}")
  @AuditLog(module = "file", action = "file:delete", targetType = "file")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    fileApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
