package com.guoyu.platform.exam.standard.service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 概括产出的单个考评点。
 */
public record StandardLlmPoint(String title, String important) {

  /** 解析结果容器：points + 护栏警告 */
  public static final class ParseResult {
    private final List<StandardLlmPoint> points = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public List<StandardLlmPoint> getPoints() {
      return points;
    }

    public List<String> getWarnings() {
      return warnings;
    }
  }
}
