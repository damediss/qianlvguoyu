package com.guoyu.platform.competition.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.sourceforge.pinyin4j.PinyinHelper;

/** 生成赛事编码：赛事名称首拼音 + 创建时间（年月日时）。 */
public final class CompetitionEventCodeGenerator {

  private static final DateTimeFormatter CREATED_HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH");
  private static final int MAX_PREFIX_LENGTH = 51;

  private CompetitionEventCodeGenerator() {
  }

  public static String generate(String eventName, LocalDateTime createdAt) {
    if (eventName == null || eventName.isBlank() || createdAt == null) {
      throw new IllegalArgumentException("赛事名称和创建时间不能为空");
    }
    String prefix = initials(eventName);
    return prefix + createdAt.format(CREATED_HOUR);
  }

  static String initials(String eventName) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < eventName.length(); i++) {
      char character = eventName.charAt(i);
      String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(character);
      if (pinyin != null && pinyin.length > 0 && !pinyin[0].isBlank()) {
        result.append(pinyin[0].charAt(0));
      } else if (character < 128 && Character.isLetterOrDigit(character)) {
        result.append(character);
      }
    }
    String normalized = result.toString().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      normalized = "competition";
    }
    return normalized.substring(0, Math.min(normalized.length(), MAX_PREFIX_LENGTH));
  }
}
