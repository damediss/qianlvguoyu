package com.guoyu.platform.common.util;

/**
 * 敏感字段脱敏工具。
 *
 * <p>此前项目里 {@code maskIdCard} / {@code maskPhone} / {@code maskMobile} 有 10 份私有复制
 * （certification×5 / chazheng / competition×3 / training 各自的 service 里），口径互相不一致
 * —— 同一个 18 位身份证能渲染出 5 种以上结果。2026-09-22 安全审查先发现 4 处身份证、2 处手机号
 * +短信验证码明文进日志；2026-09-29 复查又发现其中 5 处在"长度不符合预期"时直接 {@code return 原值}，
 * 导致固话 / 带区号 / 国际号码<b>明文落进名为 masked_phone / masked_id_card 的列</b>。
 * 现已全部收敛到本类，项目内不再有任何私有脱敏实现。
 *
 * <p><b>契约（不可破坏）</b>：本类任何方法在任何输入下都<b>不返回原值</b>。长度不足、null、空白
 * 一律整体打码。新增脱敏需求请在这里加方法，不要在业务 Service 里再写私有复制。
 *
 * <p>规则：日志、审计、异常信息里出现个人敏感信息时一律走这里；业务展示需要完整值时才用原值。
 */
public final class SensitiveMaskUtils {

  private SensitiveMaskUtils() {
  }

  /**
   * 身份证脱敏：保留前 6 位与后 4 位，中间以 * 填充。
   *
   * <p>兼容 18 位与 15 位旧证；null / 空白 / 长度不足（&lt; 8）时不做部分暴露，全部打码。
   */
  public static String maskIdCard(String idCard) {
    if (idCard == null || idCard.isBlank()) {
      return "****";
    }
    String value = idCard.trim();
    if (value.length() < 8) {
      return "*".repeat(value.length());
    }
    int keepTail = 4;
    int keepHead = value.length() >= 18 ? 6 : 4;
    int masked = value.length() - keepHead - keepTail;
    if (masked <= 0) {
      // 长度恰好等于 keepHead + keepTail（如 8 位）时会退化成原样返回，必须整体打码
      return "*".repeat(value.length());
    }
    return value.substring(0, keepHead) + "*".repeat(masked) + value.substring(value.length() - keepTail);
  }

  /** 手机号脱敏：保留前 3 位与后 4 位。非 11 位时只保留后 4 位。 */
  public static String maskPhone(String phone) {
    if (phone == null || phone.isBlank()) {
      return "****";
    }
    String value = phone.trim();
    if (value.length() < 7) {
      return "*".repeat(value.length());
    }
    if (value.length() == 11) {
      return value.substring(0, 3) + "****" + value.substring(7);
    }
    return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
  }

  /** 姓名脱敏：保留姓，其余打码（用于日志、日志文件名等排障场景）。 */
  public static String maskName(String name) {
    if (name == null || name.isBlank()) {
      return "****";
    }
    String value = name.trim();
    if (value.length() <= 1) {
      return "*";
    }
    return value.charAt(0) + "*".repeat(value.length() - 1);
  }

  /**
   * 短信验证码：一律不进日志（验证码等同一次性口令）。
   * 需要占位时用 {@link #OTP_PLACEHOLDER}。
   */
  public static final String OTP_PLACEHOLDER = "******";
}
