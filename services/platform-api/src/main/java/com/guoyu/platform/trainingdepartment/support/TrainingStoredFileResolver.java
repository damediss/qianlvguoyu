package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 将 path_source（/api/files/{id}/download 或 CDN 相对路径）解析为本地可读路径 */
@Component
public class TrainingStoredFileResolver {

  private static final Pattern FILE_API_PATTERN =
      Pattern.compile("/api/files/(\\d+)/download");

  private final FileMapper fileMapper;
  private final FileStorageProperties fileStorageProperties;
  private final String cdnPath;

  public TrainingStoredFileResolver(
      FileMapper fileMapper,
      FileStorageProperties fileStorageProperties,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath) {
    this.fileMapper = fileMapper;
    this.fileStorageProperties = fileStorageProperties;
    this.cdnPath = cdnPath;
  }

  public Path resolveLocalPath(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径为空");
    }
    String path = storedPath.trim();
    Matcher m = FILE_API_PATTERN.matcher(path);
    if (m.find()) {
      Long fileId = Long.parseLong(m.group(1));
      FileEntity entity = fileMapper.selectOne(new LambdaQueryWrapper<FileEntity>()
          .eq(FileEntity::getId, fileId)
          .eq(FileEntity::getDeleted, 0)
          .eq(FileEntity::getStatus, "uploaded"));
      if (entity == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
      }
      Path root = Paths.get(fileStorageProperties.localRoot()).toAbsolutePath().normalize();
      Path resolved = root.resolve(entity.getObjectKey()).normalize();
      if (!resolved.startsWith(root)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
      }
      if (!Files.exists(resolved)) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "文件未找到: " + entity.getOriginalName());
      }
      return resolved;
    }
    if (path.startsWith("http://") || path.startsWith("https://")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "暂不支持远程模板 URL");
    }
    String relative = path.startsWith("/") ? path.substring(1) : path;
    Path cdnRoot = Paths.get(cdnPath).toAbsolutePath().normalize();
    Path resolved = cdnRoot.resolve(relative).normalize();
    if (!resolved.startsWith(cdnRoot)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
    }
    if (!Files.exists(resolved)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模板文件不存在");
    }
    return resolved;
  }

  public Optional<Path> tryResolveLocalPath(String storedPath) {
    try {
      return Optional.of(resolveLocalPath(storedPath));
    } catch (BusinessException ex) {
      return Optional.empty();
    }
  }

  public void deleteIfExists(String storedPath) {
    tryResolveLocalPath(storedPath).ifPresent(p -> {
      try {
        Files.deleteIfExists(p);
      } catch (Exception ignored) {
        // ignore cleanup errors
      }
    });
  }
}
