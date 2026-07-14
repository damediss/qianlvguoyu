package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** LLM 返回 JSON 解析器 — 提取 similar_questions / negative_questions */
@Slf4j
@Component
public class FaqLlmResponseParser {

  private final ObjectMapper objectMapper;

  public FaqLlmResponseParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 从 LLM 返回的 content 字符串中提取 JSON 数组字段。
   *
   * @param llmContent LLM 返回的原始 content（可能包含 ```json 包裹或前导文本）
   * @param fieldName  要提取的字段名（"similar_questions" 或 "negative_questions"）
   * @return 解析后的字符串列表，解析失败返回空列表
   */
  public List<String> extractJsonArray(String llmContent, String fieldName) {
    if (llmContent == null || llmContent.isBlank()) {
      return List.of();
    }

    String jsonStr = stripMarkdownCodeBlock(llmContent.trim());

    // 尝试直接解析
    try {
      JsonNode root = objectMapper.readTree(jsonStr);
      return extractArrayField(root, fieldName);
    } catch (JsonProcessingException e) {
      // 尝试从文本中提取 JSON 对象部分
      int start = jsonStr.indexOf('{');
      int end = jsonStr.lastIndexOf('}');
      if (start >= 0 && end > start) {
        try {
          JsonNode root = objectMapper.readTree(jsonStr.substring(start, end + 1));
          return extractArrayField(root, fieldName);
        } catch (JsonProcessingException ex) {
          log.warn("LLM 响应 JSON 解析失败（二次尝试）：fieldName={}, content={}", fieldName, jsonStr);
        }
      }
    }
    log.warn("LLM 响应 JSON 解析失败：fieldName={}, content={}", fieldName, jsonStr);
    return List.of();
  }

  private List<String> extractArrayField(JsonNode root, String fieldName) {
    JsonNode arrayNode = root.path(fieldName);
    if (!arrayNode.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (JsonNode item : arrayNode) {
      if (item.isTextual() && !item.asText().isBlank()) {
        result.add(item.asText());
      }
    }
    return result;
  }

  private String stripMarkdownCodeBlock(String content) {
    if (content.startsWith("```")) {
      int firstNewline = content.indexOf('\n');
      if (firstNewline > 0) {
        content = content.substring(firstNewline + 1);
      }
      int lastFence = content.lastIndexOf("```");
      if (lastFence >= 0) {
        content = content.substring(0, lastFence);
      }
    }
    return content.trim();
  }
}
