package com.guoyu.platform.chazheng.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class ChazhengLicenseImageDecodeTest {

  private static final byte[] PNG_HEAD = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
  private static final byte[] JPG_HEAD = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 1, 2, 3, 4};

  @Test
  void decode_pdfMagic_detectsPdf() {
    byte[] pdf = "%PDF-1.4 header 1 0 obj".getBytes();
    String encoded = Base64.getEncoder().encodeToString(pdf);
    ChazhengCertQueryService.DecodedImage image = ChazhengCertQueryService.decodeLicenseImage(encoded);
    assertArrayEquals(pdf, image.bytes());
    assertEquals("pdf", image.extension());
    assertEquals("application/pdf", image.contentType());
  }

  @Test
  void decode_rawBase64_detectsPng() {
    String encoded = Base64.getEncoder().encodeToString(PNG_HEAD);
    ChazhengCertQueryService.DecodedImage image = ChazhengCertQueryService.decodeLicenseImage(encoded);
    assertArrayEquals(PNG_HEAD, image.bytes());
    assertEquals("png", image.extension());
    assertEquals("image/png", image.contentType());
  }

  @Test
  void decode_dataUrlPrefix_stripsAndDetectsJpg() {
    String encoded = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(JPG_HEAD);
    ChazhengCertQueryService.DecodedImage image = ChazhengCertQueryService.decodeLicenseImage(encoded);
    assertArrayEquals(JPG_HEAD, image.bytes());
    assertEquals("jpg", image.extension());
    assertEquals("image/jpeg", image.contentType());
  }

  @Test
  void decode_whitespaceTolerant_mimeDecoder() {
    String encoded = Base64.getEncoder().encodeToString(PNG_HEAD);
    String spaced = encoded.substring(0, 4) + "\n" + encoded.substring(4);
    ChazhengCertQueryService.DecodedImage image = ChazhengCertQueryService.decodeLicenseImage(spaced);
    assertArrayEquals(PNG_HEAD, image.bytes());
  }

  @Test
  void decode_unknownMagic_usesDeclaredType() {
    byte[] raw = {9, 9, 9, 9, 9, 9, 9, 9, 9, 9};
    String encoded = "data:image/png;base64," + Base64.getEncoder().encodeToString(raw);
    ChazhengCertQueryService.DecodedImage image = ChazhengCertQueryService.decodeLicenseImage(encoded);
    assertEquals("image/png", image.contentType());
  }

  @Test
  void decode_invalidBase64_returnsNull() {
    assertNull(ChazhengCertQueryService.decodeLicenseImage("!!!not-base64!!!"));
  }

  @Test
  void decode_blankOrShort_returnsNull() {
    assertNull(ChazhengCertQueryService.decodeLicenseImage(null));
    assertNull(ChazhengCertQueryService.decodeLicenseImage("   "));
    assertNull(ChazhengCertQueryService.decodeLicenseImage(
        Base64.getEncoder().encodeToString(new byte[] {1, 2})));
  }
}
