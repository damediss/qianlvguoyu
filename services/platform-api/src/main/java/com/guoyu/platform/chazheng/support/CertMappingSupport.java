package com.guoyu.platform.chazheng.support;

import com.guoyu.platform.chazheng.service.CollectedCert;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 查证证书字段映射（多证择优、等级文本解析），供人员采集回填与方案规划人员认定共用。
 */
public final class CertMappingSupport {

  private static final String[] LEVEL_LABELS = {"初级工", "中级工", "高级工", "技师", "高级技师"};
  private static final Pattern LEVEL_IN_PARENS = Pattern.compile("[（(]([^）)]+)[）)]");

  private CertMappingSupport() {}

  public static CollectedCert pickPrimaryCert(List<CollectedCert> certs) {
    if (certs == null || certs.isEmpty()) {
      throw new IllegalArgumentException("certs must not be empty");
    }
    return certs.stream()
        .max(Comparator.comparingInt(c -> {
          Integer level = mapLevelText(extractLevelText(c.certName()));
          return level != null ? level : 0;
        }))
        .orElse(certs.get(0));
  }

  public static String extractLevelText(String certName) {
    if (!StringUtils.hasText(certName)) {
      return "";
    }
    Matcher matcher = LEVEL_IN_PARENS.matcher(certName);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return certName.trim();
  }

  public static Integer mapLevelText(String text) {
    if (!StringUtils.hasText(text)) {
      return null;
    }
    String t = text.trim();
    return switch (t) {
      case "一级", "初级", "初级工" -> 1;
      case "二级", "中级", "中级工" -> 2;
      case "三级", "高级", "高级工" -> 3;
      case "四级", "技师" -> 4;
      case "五级", "高级技师" -> 5;
      default -> {
        for (int i = 0; i < LEVEL_LABELS.length; i++) {
          if (LEVEL_LABELS[i].equals(t)) {
            yield i + 1;
          }
        }
        yield null;
      }
    };
  }

  /** 展示用等级：优先括号内文本，否则用证书全名。 */
  public static String displayCertLevel(CollectedCert primary) {
    if (primary == null) {
      return null;
    }
    String levelText = extractLevelText(primary.certName());
    return StringUtils.hasText(levelText) ? levelText : primary.certName();
  }

  public static boolean isValidIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return false;
    }
    return idCard.trim().matches("^(\\d{15}|\\d{17}[\\dXx])$");
  }
}
