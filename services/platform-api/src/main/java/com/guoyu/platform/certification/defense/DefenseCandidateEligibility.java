package com.guoyu.platform.certification.defense;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 综合答辩仅限技师、高级技师参加。配置页与答辩管理共用同一判定，避免名单不一致。
 */
public final class DefenseCandidateEligibility {

  public static final String RULE_HINT = "综合答辩仅限技师、高级技师参加";
  public static final String EMPTY_ROSTER_MESSAGE = "当前认定任务暂无技师或高级技师考生";

  private DefenseCandidateEligibility() {
  }

  public static boolean isEligible(CertificationCandidateEntity candidate) {
    return candidate != null && isEligibleLevel(candidate.getLevelCode(), candidate.getLevelName());
  }

  public static boolean isEligibleLevel(String levelCode, String levelName) {
    String code = normalize(levelCode);
    String name = normalize(levelName);
    if (code.isEmpty() && name.isEmpty()) {
      return false;
    }
    return isSeniorTechnician(code, name) || isTechnician(code, name);
  }

  private static boolean isSeniorTechnician(String code, String name) {
    return "senior_technician".equals(code)
        || "4".equals(code)
        || name.contains("高级技师")
        || name.contains("一级");
  }

  private static boolean isTechnician(String code, String name) {
    if (isSeniorTechnician(code, name)) {
      return false;
    }
    return "technician".equals(code)
        || "3".equals(code)
        || name.contains("技师")
        || name.contains("二级");
  }

  private static String normalize(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    return value.replace(" ", "").replace("\u3000", "").trim().toLowerCase(Locale.ROOT);
  }
}
