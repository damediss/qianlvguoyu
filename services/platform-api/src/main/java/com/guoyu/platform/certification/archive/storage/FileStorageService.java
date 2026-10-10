package com.guoyu.platform.certification.archive.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  ArchiveFileObject store(MultipartFile file, String fileKey);

  ArchiveFileObject store(byte[] bytes, String originalFileName, String contentType, String fileKey);

  Resource load(String fileKey);

  String sanitizeFileName(String originalFileName);
}
