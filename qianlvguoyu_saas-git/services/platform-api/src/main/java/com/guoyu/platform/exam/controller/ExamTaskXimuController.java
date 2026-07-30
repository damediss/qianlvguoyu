package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamTaskXimuAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskXimuQueryReq;
import com.guoyu.platform.exam.service.ExamTaskXimuApplicationService;
import com.guoyu.platform.exam.vo.ExamTaskXimuDailyVO;
import com.guoyu.platform.exam.vo.ExamTaskXimuDetailVO;
import com.guoyu.platform.exam.vo.ExamTaskXimuVO;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.vo.PublicDataVO;
import com.guoyu.platform.file.support.CdnFileServeHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamTaskXimuController {

  private final ExamTaskXimuApplicationService service;
  private final CdnFileServeHelper cdnFileServeHelper;

  public ExamTaskXimuController(
      ExamTaskXimuApplicationService service,
      CdnFileServeHelper cdnFileServeHelper) {
    this.service = service;
    this.cdnFileServeHelper = cdnFileServeHelper;
  }

  @GetMapping("/task-ximus")
  @PreAuthorize("hasAuthority('task:ximu:view')")
  public ApiResponse<PageResponse<ExamTaskXimuVO>> page(@Valid @ModelAttribute ExamTaskXimuQueryReq req) {
    log.info("GET /exam/task-ximus page={}, pageSize={}", req.getPage(), req.getPageSize());
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/task-ximus/{id}/details")
  @PreAuthorize("hasAuthority('task:ximu:view')")
  public ApiResponse<List<ExamTaskXimuDetailVO>> details(@PathVariable Integer id) {
    log.info("GET /exam/task-ximus/{}/details", id);
    return ApiResponse.ok(service.getDetails(id));
  }

  @GetMapping("/task-ximus/{id}/daily")
  @PreAuthorize("hasAuthority('task:ximu:view')")
  public ApiResponse<List<ExamTaskXimuDailyVO>> daily(
      @PathVariable Integer id, @RequestParam Integer role) {
    log.info("GET /exam/task-ximus/{}/daily role={}", id, role);
    return ApiResponse.ok(service.getDaily(id, role));
  }

  @PostMapping("/task-ximus/import")
  @PreAuthorize("hasAuthority('task:ximu:import')")
  @AuditLog(module = "exam", action = "task-ximu:import", targetType = "task_ximu")
  public ApiResponse<Integer> importExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam Integer workTypeId,
      @RequestParam Integer level,
      @RequestParam Integer types) {
    log.info("POST /exam/task-ximus/import workTypeId={}, level={}, types={}", workTypeId, level, types);
    int count = service.importExcel(file, workTypeId, level, types);
    return ApiResponse.ok(count);
  }

  @PostMapping("/task-ximus/{id}/assign-expert")
  @PreAuthorize("hasAuthority('task:ximu:assign-expert')")
  @AuditLog(module = "exam", action = "task-ximu:assign-expert", targetType = "task_ximu")
  public ApiResponse<Void> assignExpert(
      @PathVariable Integer id,
      @Valid @RequestBody ExamTaskXimuAssignExpertReq req) {
    log.info("POST /exam/task-ximus/{}/assign-expert expertId={}, role={}", id, req.expertId(), req.role());
    service.assignExpert(req);
    return ApiResponse.ok();
  }

  @GetMapping("/task-ximus/{id}/export")
  @PreAuthorize("hasAuthority('task:ximu:export')")
  @AuditLog(module = "exam", action = "task-ximu:export", targetType = "task_ximu")
  public ResponseEntity<InputStreamResource> export(@PathVariable Integer id) throws IOException {
    log.info("GET /exam/task-ximus/{}/export", id);
    ExportResultDTO result = service.exportExcel(id);
    InputStreamResource resource = new InputStreamResource(new java.io.ByteArrayInputStream(result.data()));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.filename(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }

  @DeleteMapping("/task-ximus/{id}")
  @PreAuthorize("hasAuthority('task:ximu:delete')")
  @AuditLog(module = "exam", action = "task-ximu:delete", targetType = "task_ximu")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    log.info("DELETE /exam/task-ximus/{}", id);
    service.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/public-data")
  @PreAuthorize("hasAuthority('task:ximu:view')")
  public ApiResponse<PublicDataVO> publicData() {
    log.info("GET /exam/public-data");
    return ApiResponse.ok(service.getPublicData());
  }

  /**
   * CDN file access — maps /api/files/** to the configured cdn path.
   * Used by Office Online Viewer and direct download.
   */
  @GetMapping("/files/**")
  public ResponseEntity<?> serveFile(jakarta.servlet.http.HttpServletRequest request)
      throws IOException {
    String fp = extractCdnRelativePath(request);
    log.info("GET /exam/files/{}", fp);
    var local = cdnFileServeHelper.resolveLocalPath(fp);
    if (local.isPresent()) {
      Path path = local.get();
      InputStreamResource resource = new InputStreamResource(new FileInputStream(path.toFile()));
      String contentType = Files.probeContentType(path);
      if (contentType == null) {
        contentType = "application/octet-stream";
      }
      ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType));
      // 仅导出 zip 强制下载；证件照等仍需 inline 供 <img> 预览
      if (fp.startsWith("down/")) {
        String fileName = path.getFileName() != null ? path.getFileName().toString() : "download.zip";
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(fileName, StandardCharsets.UTF_8)
            .build();
        builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
      }
      return builder.body(resource);
    }
    // 本地导出包（down/**）只存在本机，切勿 302 到生产 CDN（会变成 nginx 404）
    if (fp.startsWith("down/")) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
    }
    var remote = cdnFileServeHelper.remoteFallbackUrl(fp);
    if (remote.isPresent()) {
      log.info("GET /exam/files/{} redirect to {}", fp, remote.get());
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(remote.get())).build();
    }
    throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
  }

  private static String extractCdnRelativePath(jakarta.servlet.http.HttpServletRequest request) {
    String marker = "/exam/files/";
    String uri = request.getRequestURI();
    int idx = uri.indexOf(marker);
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid CDN file path: " + uri);
    }
    // getRequestURI() 保留百分号编码；中文文件名若不解码会找不到本地文件并误跳 CDN
    String relative = uri.substring(idx + marker.length());
    return java.net.URLDecoder.decode(relative, StandardCharsets.UTF_8);
  }
}
