package com.guoyu.platform.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 修订方案 §2.5：敏感字段脱敏工具单测（覆盖 null / 短串 / 15 位旧证等边界）。
 */
class SensitiveMaskUtilsTest {

  @Test
  @DisplayName("18 位身份证保留前 6 后 4")
  void maskIdCard18() {
    assertEquals("450103********0018", SensitiveMaskUtils.maskIdCard("450103199001010018"));
  }

  @Test
  @DisplayName("15 位旧证保留前 4 后 4")
  void maskIdCard15() {
    assertEquals("4501*******1001", SensitiveMaskUtils.maskIdCard("450103900101001"));
  }

  @Test
  @DisplayName("null / 空白返回固定掩码，不抛异常")
  void maskIdCardNullOrBlank() {
    assertEquals("****", SensitiveMaskUtils.maskIdCard(null));
    assertEquals("****", SensitiveMaskUtils.maskIdCard("   "));
  }

  @Test
  @DisplayName("长度不足 8 时全打码，不做部分暴露")
  void maskIdCardTooShort() {
    String masked = SensitiveMaskUtils.maskIdCard("1234567");
    assertEquals("*******", masked);
  }

  @Test
  @DisplayName("手机号保留前 3 后 4")
  void maskPhone11() {
    assertEquals("138****8000", SensitiveMaskUtils.maskPhone("13812348000"));
  }

  @Test
  @DisplayName("非 11 位手机号只保留后 4 位")
  void maskPhoneOtherLength() {
    assertEquals("****5678", SensitiveMaskUtils.maskPhone("12345678"));
    assertEquals("****", SensitiveMaskUtils.maskPhone(null));
  }

  @Test
  @DisplayName("姓名只保留姓")
  void maskName() {
    assertEquals("覃**", SensitiveMaskUtils.maskName("覃某某"));
    assertEquals("*", SensitiveMaskUtils.maskName("覃"));
    assertEquals("****", SensitiveMaskUtils.maskName(null));
  }

  @Test
  @DisplayName("脱敏结果不得包含原串中间段（防止误以为脱敏却泄露）")
  void maskMustNotLeakMiddle() {
    String idCard = "450103199001010018";
    String masked = SensitiveMaskUtils.maskIdCard(idCard);
    assertEquals(18, masked.length());
    assertFalse(masked.contains("19900101"), "脱敏结果不应包含出生日期段");
  }

  @Test
  @DisplayName("契约：任何输入都不得原样返回"
      + "（历史私有实现在长度不符时 return 原值，导致明文落进 masked_phone / masked_id_card 列）")
  void neverReturnsOriginalValue() {
    String[] phones = {null, "", "   ", "1", "123456", "1234567", "0755-12345678",
        "+8613812348000", "13812348000", "123456789012"};
    for (String raw : phones) {
      String masked = SensitiveMaskUtils.maskPhone(raw);
      assertFalse(raw != null && raw.equals(masked), "maskPhone 不得返回原值: [" + raw + "]");
    }

    String[] idCards = {null, "", "   ", "1", "1234567", "X1234567", "12345678",
        "450103900101001", "450103199001010018", "45010319900101001899"};
    for (String raw : idCards) {
      String masked = SensitiveMaskUtils.maskIdCard(raw);
      assertFalse(raw != null && raw.equals(masked), "maskIdCard 不得返回原值: [" + raw + "]");
      assertFalse(raw != null && raw.trim().equals(masked),
          "maskIdCard 不得返回去空格后的原值: [" + raw + "]");
    }
  }

  @Test
  @DisplayName("8 位输入不得退化成原样返回（keepHead + keepTail 恰等于长度的边界缺陷）")
  void maskIdCardEightCharsMustStillMask() {
    assertEquals("********", SensitiveMaskUtils.maskIdCard("X1234567"));
    assertEquals("********", SensitiveMaskUtils.maskIdCard("12345678"));
  }
}
