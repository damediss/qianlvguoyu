package com.guoyu.platform.exam.zhenti.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** D3 出参：同步模式带 results；异步模式仅 importId。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZhentiImportResultVO(
    String mode,
    String importId,
    List<ZhentiImportFileResult> results
) {

  public static ZhentiImportResultVO sync(List<ZhentiImportFileResult> results) {
    return new ZhentiImportResultVO("sync", null, results);
  }

  public static ZhentiImportResultVO async(Long importId) {
    return new ZhentiImportResultVO("async", String.valueOf(importId), null);
  }
}
