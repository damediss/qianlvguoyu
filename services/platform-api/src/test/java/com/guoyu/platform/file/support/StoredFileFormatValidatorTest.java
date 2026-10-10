package com.guoyu.platform.file.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class StoredFileFormatValidatorTest {

  @Test
  void acceptsXlsxMagic() {
    byte[] xlsx = new byte[] {'P', 'K', 3, 4};
    assertThatCode(() -> StoredFileFormatValidator.assertSupportedOfficeFile("导入表.xlsx", xlsx))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsCorruptedXlsx() {
    byte[] bad = new byte[] {(byte) 0x88, 0x7d, 0x1c, 0x0d};
    assertThatThrownBy(() -> StoredFileFormatValidator.assertSupportedOfficeFile("导入表.xlsx", bad))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不是有效的 Excel/Word 文档");
  }

  @Test
  void acceptsJpegSoiOnly() {
    byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, 0x00, 0x10};
    assertThatCode(() ->
            StoredFileFormatValidator.assertSupportedImageFile("photo.jpg", "image/jpeg", jpeg))
        .doesNotThrowAnyException();
  }

  @Test
  void allowsNonStandardImageBytesWithJpgName() {
    // 部分手机/旧 CDN 文件头非标准，不再直接拒绝
    byte[] weird = new byte[] {(byte) 0x88, 0x7d, 0x1c, 0x0d};
    assertThatCode(() ->
            StoredFileFormatValidator.assertSupportedImageFile("photo.jpg", "image/jpeg", weird))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsHtmlPayloadDisguisedAsJpg() {
    byte[] html = "<!DOCTYPE html>".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    assertThatThrownBy(() ->
            StoredFileFormatValidator.assertSupportedImageFile("photo.jpg", "image/jpeg", html))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不是有效图片");
  }
}
