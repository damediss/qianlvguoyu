package com.guoyu.platform.certification.service;

import com.guoyu.platform.file.service.FileApplicationService;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CertificationImageSourceService {

  private static final Pattern FILE_DOWNLOAD_PATH =
      Pattern.compile("^/?api/files/(\\d+)/download(?:\\?.*)?$");
  private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;

  private final FileApplicationService fileApplicationService;

  public CertificationImageSourceService(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  public byte[] readFile(Long fileId) {
    if (fileId == null) {
      return null;
    }
    return readResource(fileApplicationService.downloadResource(fileId), "fileId=" + fileId);
  }

  public byte[] readReference(String reference) {
    if (!StringUtils.hasText(reference)) {
      return null;
    }
    String normalized = reference.trim();
    if (normalized.startsWith("classpath:")) {
      String path = normalized.substring("classpath:".length()).replaceFirst("^/+", "");
      return readResource(new ClassPathResource(path), normalized);
    }
    if (normalized.startsWith("file-id:")) {
      try {
        return readFile(Long.valueOf(normalized.substring("file-id:".length())));
      } catch (NumberFormatException ex) {
        log.warn("Invalid certification image file reference: {}", normalized);
        return null;
      }
    }
    Matcher matcher = FILE_DOWNLOAD_PATH.matcher(normalized);
    if (matcher.matches()) {
      return readFile(Long.valueOf(matcher.group(1)));
    }
    log.warn("Unsupported certification image reference: {}", normalized);
    return null;
  }

  private byte[] readResource(Resource resource, String source) {
    try {
      if (resource == null || !resource.exists() || !resource.isReadable()) {
        log.warn("Certification image is not readable: {}", source);
        return null;
      }
      try (InputStream input = resource.getInputStream()) {
        byte[] content = input.readNBytes(MAX_IMAGE_BYTES + 1);
        if (content.length == 0 || content.length > MAX_IMAGE_BYTES) {
          log.warn("Certification image is empty or too large: {}", source);
          return null;
        }
        return content;
      }
    } catch (IOException ex) {
      log.warn("Failed to read certification image: {}", source, ex);
      return null;
    }
  }
}
