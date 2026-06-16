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
}
