package com.guoyu.platform.courseware.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** LLM 输出 JSON 的宽容提取(输出零信任,方案 §12):剥 Markdown 围栏、截取首个完整对象。 */
public final class CoursewareJson {

  private CoursewareJson() {
  }

  /**
   * 从模型输出中提取首个 JSON 对象。失败抛 IllegalArgumentException(调用方按契约失败处理)。
   */
  public static JsonNode extractObject(String content, ObjectMapper om) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("输出为空");
    }
    String cleaned = stripFence(content.trim());
    int start = cleaned.indexOf('{');
    if (start < 0) {
      throw new IllegalArgumentException("输出中未找到 JSON 对象");
    }
    int depth = 0;
    int end = -1;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < cleaned.length(); i++) {
      char c = cleaned.charAt(i);
      if (inString) {
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == '"') {
          inString = false;
        }
        continue;
      }
      if (c == '"') {
        inString = true;
      } else if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          end = i;
          break;
        }
      }
    }
    if (end < 0) {
      throw new IllegalArgumentException("JSON 对象不完整(可能被截断)");
    }
    try {
      return om.readTree(cleaned.substring(start, end + 1));
    } catch (Exception e) {
      throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
    }
  }

  private static String stripFence(String s) {
    String out = s;
    if (out.startsWith("```")) {
      int firstNewline = out.indexOf('\n');
      if (firstNewline > 0) {
        out = out.substring(firstNewline + 1);
      }
      int fence = out.lastIndexOf("```");
      if (fence >= 0) {
        out = out.substring(0, fence);
      }
    }
    return out.trim();
  }

  /** prompt 摘要(审计用):sha256 前 16 位 + 长度 */
  public static String digest(String prompt) {
    if (prompt == null) {
      return "sha256:null";
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder("sha256:");
      for (int i = 0; i < 8; i++) {
        sb.append(String.format("%02x", hash[i]));
      }
      return sb.append(" len=").append(prompt.length()).toString();
    } catch (NoSuchAlgorithmException e) {
      return "sha256:unavailable len=" + prompt.length();
    }
  }
}
