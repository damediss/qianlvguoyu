package com.guoyu.platform.file.artifact.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.common.security.PermissionCodes;
import com.guoyu.platform.file.artifact.service.FileArtifactApplicationService;
import com.guoyu.platform.file.artifact.vo.FileArtifactMetadataVO;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-file provider endpoint; never expose it without platform auth and signed actor meta. */
@RestController
@RequestMapping("/internal/artifacts/files")
@PreAuthorize("hasAuthority('" + PermissionCodes.FILE_ARTIFACT_READ + "')")
public class FileArtifactController {

  private final FileArtifactApplicationService service;

  public FileArtifactController(FileArtifactApplicationService service) {
    this.service = service;
  }

  @GetMapping("/{fileId}/metadata")
  @AuditLog(module = "file-artifact", action = "file:artifact:metadata", targetType = "file")
  public ApiResponse<FileArtifactMetadataVO> metadata(@PathVariable Long fileId) {
    return ApiResponse.ok(service.getMetadata(fileId));
  }

  @GetMapping(value = "/{fileId}/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @AuditLog(module = "file-artifact", action = "file:artifact:read", targetType = "file")
  public ResponseEntity<Resource> content(@PathVariable Long fileId) {
    return service.download(fileId);
  }
}
