package com.guoyu.platform.exam.reviewsync.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 主观题参考答案提取（《简答题作答与AI判分方案》§5-1）。
 *
 * <p>exam_exam_detail.answer 兼容四种形态（与前端 getSubjectAnswers 口径对齐）：
 * <ol>
 *   <li>纯文本 string → 原文返回；</li>
 *   <li>JSON 数组字符串 → 取各项 content（对象项）或项本身（文本项）拼接；</li>
 *   <li>JSON 对象字符串 → 取 content 字段；</li>
 *   <li>以 [ 或 { 开头的 JSON 字符串 → 先 parse 再按 2/3 处理。</li>
 * </ol>
 * 提取失败（JSON 解析异常、结构不含文本）或结果为空 → 返回 null（上层走自评兜底，Q4-A）。
 */
public final class SubjectiveAnswerExtractor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private SubjectiveAnswerExtractor() {
  }

  /** 提取参考答案文本；不可提取返回 null */
  public static String extract(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim();
    if (s.isEmpty()) {
      return null;
    }
    if (s.startsWith("[") || s.startsWith("{")) {
      JsonNode node;
      try {
        node = MAPPER.readTree(s);
      } catch (Exception e) {
        return null;
      }
      return fromNode(node);
    }
    return s;
  }

  private static String fromNode(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode item : node) {
        String part = textOf(item.isObject() ? item.get("content") : item);
        if (part != null && !part.isBlank()) {
          if (sb.length() > 0) {
            sb.append('\n');
          }
          sb.append(part.trim());
        }
      }
      return sb.length() == 0 ? null : sb.toString();
    }
    if (node.isObject()) {
      String content = textOf(node.get("content"));
      return content == null || content.isBlank() ? null : content.trim();
    }
    String text = textOf(node);
    return text == null || text.isBlank() ? null : text.trim();
  }

  private static String textOf(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isValueNode()) {
      return node.asText();
    }
    return null;
  }
}
