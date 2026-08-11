package com.guoyu.platform.file.artifact.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.artifact.vo.FileArtifactMetadataVO;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.integration.bot.BotActorContext;
import com.guoyu.platform.integration.bot.BotActorContextHolder;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FileArtifactApplicationService {

  private final FileApplicationService fileApplicationService;

  public FileArtifactApplicationService(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional(readOnly = true)
  public FileArtifactMetadataVO getMetadata(Long fileId) {
    FileEntity file = authorizedFile(fileId);
    return new FileArtifactMetadataVO(
        String.valueOf(file.getId()),
        file.getOriginalName(),
        file.getContentType(),
        file.getSizeBytes(),
        file.getSha256(),
        file.getAccessPolicy());
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> download(Long fileId) {
    FileEntity file = authorizedFile(fileId);
    ResponseEntity<Resource> base = fileApplicationService.buildDownloadResponse(
        file.getTenantId(), fileId, false);
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(base.getHeaders());
    headers.set("X-File-Id", String.valueOf(file.getId()));
    headers.set("X-File-Name", file.getOriginalName());
    headers.set("X-File-Mime-Type", StringUtils.hasText(file.getContentType())
        ? file.getContentType() : "application/octet-stream");
    headers.set("X-File-Size", String.valueOf(file.getSizeBytes()));
    if (file.getSha256() != null && !file.getSha256().isBlank()) {
      headers.set("X-File-SHA256", file.getSha256());
    }
    return new ResponseEntity<>(base.getBody(), headers, base.getStatusCode());
  }

  private FileEntity authorizedFile(Long fileId) {
    if (fileId == null || fileId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "File ID is required");
    }
    BotActorContext actor = BotActorContextHolder.get();
    if (actor == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Verified bot actor context is required");
    }
    if (!actor.isPrivateChat()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Contract files can only be delivered to a private chat");
    }
    FileEntity file = fileApplicationService.getExistingFile(actor.tenantId(), fileId);
    if (!Objects.equals(file.getTenantId(), actor.tenantId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "File tenant mismatch");
    }
    return file;
  }
}
