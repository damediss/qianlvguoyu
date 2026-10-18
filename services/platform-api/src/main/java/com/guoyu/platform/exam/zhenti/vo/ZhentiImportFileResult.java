package com.guoyu.platform.exam.zhenti.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 批量导入单文件结果。
 *
 * <p>pending=true 表示文件已入库但元数据待补录（缺年份/工种等级），管理端可就地编辑后走 D4 补录；
 * 其余失败（类型非法/超限/重复）未入库，需重新上传。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZhentiImportFileResult(
    String fileName,
    boolean ok,
    String id,
    String title,
    String year,
    String reason,
    Boolean pending
) {

  public static ZhentiImportFileResult success(
      String fileName, Long id, String title, String year) {
    return new ZhentiImportFileResult(
        fileName, true, id == null ? null : String.valueOf(id), title, year, null, null);
  }

  public static ZhentiImportFileResult pending(
      String fileName, Long id, String title, String year, String reason) {
    return new ZhentiImportFileResult(
        fileName, false, id == null ? null : String.valueOf(id), title, year, reason, true);
  }

  public static ZhentiImportFileResult failure(String fileName, String reason) {
    return new ZhentiImportFileResult(fileName, false, null, null, null, reason, null);
  }
}
