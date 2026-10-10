package com.guoyu.platform.file.support;

import com.guoyu.platform.file.config.FileStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在多个候选 local-root 下解析 sys_file.object_key，兼容历史路径与不同启动目录。
 */
@Component
public class FileLocalPathResolver {

  private static final String[] LEGACY_LOCAL_ROOTS = {
      "./storage/files",
      "./services/platform-api/storage/files",
      "../storage/files",
      "../../storage/files",
  };

  private final FileStorageProperties properties;

  public FileLocalPathResolver(FileStorageProperties properties) {
    this.properties = properties;
  }

  /** 返回第一个存在且可读的本地文件路径。 */
  public Optional<Path> resolveExisting(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return Optional.empty();
    }
    for (Path root : localRootCandidates()) {
      Optional<Path> resolved = resolveUnderRoot(root, objectKey);
      if (resolved.isPresent()) {
        return resolved;
      }
    }
    return Optional.empty();
  }

  /** 按当前配置的 local-root 解析路径（不校验文件是否存在）。 */
  public Path resolveConfigured(String objectKey) {
    Path root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
    Path resolved = root.resolve(objectKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Invalid file object key");
    }
    return resolved;
  }

  private List<Path> localRootCandidates() {
    List<Path> roots = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    addRoot(roots, seen, properties.localRoot());
    for (String legacy : LEGACY_LOCAL_ROOTS) {
      addRoot(roots, seen, legacy);
    }
    return roots;
  }

  private static void addRoot(List<Path> roots, Set<String> seen, String rootStr) {
    if (!StringUtils.hasText(rootStr)) {
      return;
    }
    Path root = Paths.get(rootStr).toAbsolutePath().normalize();
    if (seen.add(root.toString())) {
      roots.add(root);
    }
  }

  private static Optional<Path> resolveUnderRoot(Path root, String objectKey) {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path resolved = normalizedRoot.resolve(objectKey).normalize();
    if (!resolved.startsWith(normalizedRoot)) {
      return Optional.empty();
    }
    if (!Files.isRegularFile(resolved)) {
      return Optional.empty();
    }
    return Optional.of(resolved);
  }
}
