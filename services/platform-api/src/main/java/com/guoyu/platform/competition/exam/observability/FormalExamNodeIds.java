package com.guoyu.platform.competition.exam.observability;

import java.net.InetAddress;
import org.springframework.util.StringUtils;

public final class FormalExamNodeIds {

  private FormalExamNodeIds() {
  }

  public static String current(String configured) {
    if (StringUtils.hasText(configured)) {
      return configured.trim();
    }
    String host = "unknown";
    try {
      host = InetAddress.getLocalHost().getHostName();
    } catch (Exception ignored) {
      host = "unknown";
    }
    long pid = ProcessHandle.current().pid();
    String value = host + "-" + pid;
    return value.length() <= 64 ? value : value.substring(0, 64);
  }
}
