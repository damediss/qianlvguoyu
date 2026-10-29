package com.guoyu.platform.competition.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 生成统一赛事编码：固定前缀 + 创建日期 + 每日流水号。 */
public final class CompetitionEventCodeGenerator {

  private static final String PREFIX = "JS";
  private static final DateTimeFormatter CREATED_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
  static final int MAX_SEQUENCE = 9999;

  private CompetitionEventCodeGenerator() {
  }

  public static String generate(LocalDateTime createdAt, int sequence) {
    if (createdAt == null) {
      throw new IllegalArgumentException("创建时间不能为空");
    }
    if (sequence < 1 || sequence > MAX_SEQUENCE) {
      throw new IllegalArgumentException("赛事编码流水号超出范围");
    }
    return String.format(Locale.ROOT, "%s-%s-%04d",
        PREFIX, createdAt.format(CREATED_DATE), sequence);
  }

  static String datePrefix(LocalDateTime createdAt) {
    if (createdAt == null) {
      throw new IllegalArgumentException("创建时间不能为空");
    }
    return PREFIX + "-" + createdAt.format(CREATED_DATE) + "-";
  }
}
