package com.guoyu.platform.trainingdepartment.support;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.util.StringUtils;

/**
 * 默认模板导出 zip 路径：文件始终在 {@code {cdn}/down/} 下。
 * Windows 上不要用 {@code Paths.get(cdn).resolve("down/xxx.zip")} 把带斜杠的相对路径一次性 resolve。
 */
public final class SignUpDefaultTemplateExportPaths {

  private SignUpDefaultTemplateExportPaths() {}

  public static String sanitizeTitle(String title) {
    if (!StringUtils.hasText(title)) {
      return "sign-up-export";
    }
    return title.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  public static String taskZipRelative(String title, long taskId) {
    return "down/" + sanitizeTitle(title) + "_t" + taskId + ".zip";
  }

  public static boolean endsWithTaskZip(String path, long taskId) {
    if (!StringUtils.hasText(path) || taskId <= 0) {
      return false;
    }
    return leafName(path).endsWith("_t" + taskId + ".zip");
  }

  public static String leafName(String relativeOrName) {
    if (!StringUtils.hasText(relativeOrName)) {
      return "";
    }
    String normalized = relativeOrName.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    return slash >= 0 ? normalized.substring(slash + 1) : normalized;
  }

  /** {@code {cdn}/down}，与打包写入目录一致。 */
  public static Path downDir(String cdnPath) {
    return Paths.get(cdnPath).toAbsolutePath().normalize().resolve("down");
  }

  /** 在 down 目录下拼文件名（文件名不得再含路径分隔符）。 */
  public static Path zipAbsolute(String cdnPath, String relativeOrFileName) {
    return downDir(cdnPath).resolve(leafName(relativeOrFileName));
  }

  /** 已上传到文件模块 / COS，check 不再依赖本机 down 目录。 */
  public static boolean isStoredDownloadUrl(String resultFile) {
    if (!StringUtils.hasText(resultFile)) {
      return false;
    }
    String value = resultFile.trim();
    return value.startsWith("/api/files/")
        || value.startsWith("http://")
        || value.startsWith("https://");
  }

  public static String fileDownloadPath(long fileId) {
    return "/api/files/" + fileId + "/download";
  }

  /**
   * 按任务号在 down 目录找 {@code *_t{taskId}.zip}，以磁盘实名为准。
   */
  public static Path findZipByTaskId(String cdnPath, long taskId) {
    if (taskId <= 0) {
      return null;
    }
    File dir = downDir(cdnPath).toFile();
    if (!dir.isDirectory()) {
      return null;
    }
    String suffix = "_t" + taskId + ".zip";
    File[] hits = dir.listFiles((d, name) -> name != null && name.endsWith(suffix));
    if (hits == null || hits.length == 0) {
      return null;
    }
    File file = hits[0];
    return file.isFile() ? file.getAbsoluteFile().toPath() : null;
  }
}
