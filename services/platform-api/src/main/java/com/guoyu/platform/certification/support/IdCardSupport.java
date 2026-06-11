package com.guoyu.platform.certification.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

public final class IdCardSupport {

  private static final DateTimeFormatter BIRTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private IdCardSupport() {
  }

  public static String parseGender(String idCardNo) {
    if (!StringUtils.hasText(idCardNo) || idCardNo.length() < 17) {
      return null;
    }
    char genderCode = idCardNo.charAt(16);
    if (!Character.isDigit(genderCode)) {
      return null;
    }
    return (genderCode - '0') % 2 == 1 ? "男" : "女";
  }

  public static LocalDate parseBirthDate(String idCardNo) {
    if (!StringUtils.hasText(idCardNo) || idCardNo.length() < 14) {
      return null;
    }
    try {
      String birth = idCardNo.substring(6, 14);
      return LocalDate.parse(birth, DateTimeFormatter.BASIC_ISO_DATE);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  public static String formatBirthDate(LocalDate birthDate) {
    return birthDate == null ? "" : birthDate.format(BIRTH_FORMAT);
  }
}
