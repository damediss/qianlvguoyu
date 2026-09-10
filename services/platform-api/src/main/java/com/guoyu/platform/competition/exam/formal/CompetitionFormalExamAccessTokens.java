package com.guoyu.platform.competition.exam.formal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CompetitionFormalExamAccessTokens {

  private CompetitionFormalExamAccessTokens() {
  }

  public static String device(Long accessSessionId) {
    return "formal-as-" + accessSessionId;
  }

  public static String fingerprint(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }
}
