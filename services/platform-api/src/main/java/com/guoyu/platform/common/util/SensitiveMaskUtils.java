package com.guoyu.platform.common.util;

/**
 * 敏感字段脱敏工具。
 *
 * <p>此前项目里 {@code maskIdCard} 有 5 份私有复制（certification / chazheng / competition×2 /
 * training 各自的 service 里），口径不一且容易漏用 —— 2026-09-22 安全审查发现 4 处身份证、
 * 2 处手机号+短信验证码因此明文进了日志。统一收敛到本类。
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
