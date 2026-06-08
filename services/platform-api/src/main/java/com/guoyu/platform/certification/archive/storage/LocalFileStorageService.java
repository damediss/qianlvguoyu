package com.guoyu.platform.certification.archive.storage;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {
  private final CertificationArchiveStorageProperties properties;

  public LocalFileStorageService(CertificationArchiveStorageProperties properties) {
    this.properties = properties;
  }

  @Override
  public ArchiveFileObject store(MultipartFile file, String fileKey) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
    }
    String safeName = sanitizeFileName(file.getOriginalFilename());
    Path target = resolve(fileKey);
    try {
      Files.createDirectories(target.getParent());
      String hash = copyAndHash(file.getInputStream(), target);
      return new ArchiveFileObject(safeName, fileKey, file.getSize(), hash, file.getContentType());
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档文件写入失败");
    }
  }

  @Override
  public ArchiveFileObject store(byte[] bytes, String originalFileName, String contentType, String fileKey) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档文件不能为空");
    }
    String safeName = sanitizeFileName(originalFileName);
    Path target = resolve(fileKey);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, bytes);
      return new ArchiveFileObject(safeName, fileKey, bytes.length, sha256(bytes), contentType);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档文件写入失败");
    }
  }

  @Override
  public Resource load(String fileKey) {
    Path path = resolve(fileKey);
    try {
      Resource resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "归档文件不存在");
      }
      return resource;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档文件读取失败");
    }
  }

  @Override
  public String sanitizeFileName(String originalFileName) {
    String name = StringUtils.hasText(originalFileName) ? originalFileName : "archive-file";
    return name.replace("\\", "_")
        .replace("/", "_")
        .replace(":", "_")
        .replace("..", "_")
        .trim();
  }

  private Path resolve(String fileKey) {
    if (!"local".equalsIgnoreCase(properties.getStorageType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持本地归档存储");
    }
    Path root = Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
    Path resolved = root.resolve(fileKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "非法归档文件路径");
    }
    return resolved;
  }

  private String copyAndHash(InputStream input, Path target) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (DigestInputStream digestInput = new DigestInputStream(input, digest)) {
        Files.copy(digestInput, target);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }
}
