package com.guoyu.platform.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RFC 5987 Content-Disposition 构造（2026-10-16 知识点导出乱码事故收口）：
 * 禁止回退到 Spring ContentDisposition 的 RFC 2047 =?UTF-8?Q?...?= 形态。
 */
class DownloadHeadersTest {

  @Test
  @DisplayName("中文文件名 → filename* 百分号编码，无 RFC 2047 形态")
  void chineseFilenameEncodedAsRfc5987() {
    String header = DownloadHeaders.contentDispositionAttachment("知识点导出_291.xlsx");
    assertEquals("attachment; filename*=UTF-8''%E7%9F%A5%E8%AF%86%E7%82%B9%E5%AF%BC%E5%87%BA_291.xlsx", header);
    assertFalse(header.contains("?Q?"), "不得出现 RFC 2047 Q 编码");
    assertFalse(header.contains("=?UTF-8"), "不得出现 RFC 2047 编码词");
  }

  @Test
  @DisplayName("空格编码为 %20 而非 form 风格的 +")
  void spaceEncodedAsPercent20() {
    String header = DownloadHeaders.contentDispositionAttachment("成绩 导入模板.xlsx");
    assertTrue(header.contains("%20"), "空格必须是 %20");
    assertFalse(header.contains("+"), "URLEncoder 的 + 必须替换掉");
  }

  @Test
  @DisplayName("ASCII 文件名保持原样可用")
  void asciiFilenamePassesThrough() {
    assertEquals("attachment; filename*=UTF-8''report.xlsx",
        DownloadHeaders.contentDispositionAttachment("report.xlsx"));
  }
}
