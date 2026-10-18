package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiUploadValidatorTest {

  private final ZhentiUploadValidator validator = new ZhentiUploadValidator();

  private static byte[] pdf(String tail) {
    return ("%PDF-1.4\n" + tail + "\n%%EOF").getBytes(StandardCharsets.US_ASCII);
  }

  @Test
  @DisplayName("真 PDF 通过")
  void realPdfPasses() {
    assertThatCode(() -> validator.validatePdf("真题.pdf", pdf("hello")))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName(".exe 改 .pdf 名（magic 不符）→ 415")
  void renamedExeRejected() {
    byte[] exe = "MZ\u0000\u0000binary".getBytes(StandardCharsets.US_ASCII);
    assertThatThrownBy(() -> validator.validatePdf("evil.pdf", exe))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("415"));
  }

  @Test
  @DisplayName("扩展名非 pdf → 415")
  void wrongExtensionRejected() {
    assertThatThrownBy(() -> validator.validatePdf("evil.exe", pdf("x")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("415"));
  }

  @Test
  @DisplayName("0 字节 → 400")
  void emptyRejected() {
    assertThatThrownBy(() -> validator.validatePdf("empty.pdf", new byte[0]))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("400"));
  }

  @Test
  @DisplayName("50MB+1 → 413（不实际分配大内存）")
  void oversizeRejected() {
    assertThatThrownBy(() -> validator.assertPdfSize(ZhentiUploadValidator.MAX_PDF_BYTES + 1))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("413"));
    assertThatCode(() -> validator.assertPdfSize(ZhentiUploadValidator.MAX_PDF_BYTES))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("恶意 PDF 关键词仅识别告警，不拦截")
  void suspiciousTokensDetected() {
    byte[] suspicious = pdf("/JavaScript /Launch /EmbeddedFile");
    assertThat(validator.suspiciousTokens(suspicious))
        .containsExactly("/JavaScript", "/Launch", "/EmbeddedFile");
    assertThatCode(() -> validator.validatePdf("suspicious.pdf", suspicious))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("MD5 稳定且为 32 位小写十六进制")
  void md5Stable() {
    String md5 = validator.md5(pdf("same"));
    assertThat(md5).hasSize(32).matches("[0-9a-f]{32}");
    assertThat(validator.md5(pdf("same"))).isEqualTo(md5);
    assertThat(validator.md5(pdf("other"))).isNotEqualTo(md5);
  }

  @Test
  @DisplayName("zip 扩展名识别大小写不敏感")
  void zipNameCaseInsensitive() {
    assertThat(validator.isZipName("真题.ZIP")).isTrue();
    assertThat(validator.isZipName("真题.rar")).isFalse();
  }
}
