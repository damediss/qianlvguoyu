package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.guoyu.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

/** 解析纯文本化与 2000 字校验（§5.12/§5.8）。 */
class ResolvePlainTextTest {

  @Test
  void noTableReturnsAsIs() {
    String text = "【考点】电气\n【正确】答案：A。规程规定。\n【干扰】选 B 错。";
    assertEquals(text, ResolvePlainText.flatten(text));
    assertEquals(null, ResolvePlainText.flatten(null));
    assertEquals("  ", ResolvePlainText.flatten("  "));
  }

  @Test
  void tableFlattensToDunhaoSemicolon() {
    String resolve = "【正确】答案：A。见下表。\n| 电压 | 场景 |\n|---|---|\n| 36 | 干燥 |\n| 12 | 潮湿 |\n【记法】口诀。";
    String out = ResolvePlainText.flatten(resolve);
    assertEquals("【正确】答案：A。见下表。\n电压36、场景干燥；电压12、场景潮湿\n【记法】口诀。", out);
    assertFalse(out.contains("|"));
    assertFalse(out.contains("---"));
  }

  @Test
  void raggedTableDegradesToPipeStrippedLines() {
    String resolve = "前文\n| 电压 | 场景 |\n|---|---|\n| 36 |\n后文";
    String out = ResolvePlainText.flatten(resolve);
    assertFalse(out.contains("|"));
    assertFalse(out.contains("---"));
    assertEquals("前文\n电压 场景\n36\n后文", out);
  }

  @Test
  void orphanPipesStrippedWithoutTableShape() {
    String resolve = "这不是表格 | 只是说明\n| 单管道行 | 无分隔行 |";
    String out = ResolvePlainText.flatten(resolve);
    assertFalse(out.contains("|"));
  }

  @Test
  void crlfNormalizes() {
    String resolve = "【正确】答案：A。\r\n| 电压 | 场景 |\r\n|---|---|\r\n| 36 | 干燥 |";
    String out = ResolvePlainText.flatten(resolve);
    assertEquals("【正确】答案：A。\n电压36、场景干燥", out);
  }

  @Test
  void assertEditableLengthBoundaries() {
    String ok2000 = "字".repeat(2000);
    String bad2001 = "字".repeat(2001);
    assertDoesNotThrow(() -> ResolvePlainText.assertEditableLength(ok2000));
    assertDoesNotThrow(() -> ResolvePlainText.assertEditableLength(null));
    assertDoesNotThrow(() -> ResolvePlainText.assertEditableLength(""));
    BusinessException ex = assertThrows(BusinessException.class,
        () -> ResolvePlainText.assertEditableLength(bad2001));
    assertEquals("解析不能超过 2000 字", ex.getMessage());
  }
}
