package com.guoyu.platform.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeTextChunker {

  private static final int DEFAULT_MAX_CHARS = 1200;
  private static final int DEFAULT_OVERLAP_CHARS = 120;

  public List<KnowledgeTextChunk> chunk(String text) {
    return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP_CHARS);
  }

  List<KnowledgeTextChunk> chunk(String text, int maxChars, int overlapChars) {
    String normalized = normalize(text);
    if (!StringUtils.hasText(normalized)) {
      return List.of();
    }

    List<KnowledgeTextChunk> chunks = new ArrayList<>();
    String[] paragraphs = normalized.split("\\n{2,}");
    StringBuilder current = new StringBuilder();
    String heading = null;
    int chunkNo = 1;
    for (String paragraph : paragraphs) {
      String trimmed = paragraph.trim();
      if (!StringUtils.hasText(trimmed)) {
        continue;
      }
      if (isHeading(trimmed)) {
        heading = cleanHeading(trimmed);
      }
      if (current.length() > 0 && current.length() + trimmed.length() + 2 > maxChars) {
        chunks.add(toChunk(chunkNo++, heading, current.toString()));
        current = new StringBuilder(overlapTail(current.toString(), overlapChars));
      }
      if (current.length() > 0) {
        current.append("\n\n");
      }
      current.append(trimmed);
    }

    if (current.length() > 0) {
      chunks.add(toChunk(chunkNo, heading, current.toString()));
    }
    return chunks;
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\r\n", "\n")
        .replace('\r', '\n')
        .replaceAll("[ \\t]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  private boolean isHeading(String text) {
    return text.startsWith("#") || (text.length() <= 40 && !text.endsWith("。") && !text.endsWith("."));
  }

  private String cleanHeading(String text) {
    return text.replaceFirst("^#+", "").trim();
  }

  private String overlapTail(String content, int overlapChars) {
    if (overlapChars <= 0 || content.length() <= overlapChars) {
      return "";
    }
    return content.substring(content.length() - overlapChars).trim();
  }

  private KnowledgeTextChunk toChunk(int chunkNo, String heading, String content) {
    String trimmed = content.trim();
    return new KnowledgeTextChunk(chunkNo, heading, null, trimmed, estimateTokens(trimmed));
  }

  private int estimateTokens(String content) {
    if (!StringUtils.hasText(content)) {
      return 0;
    }
    return Math.max(1, (int) Math.ceil(content.length() / 1.8));
  }
}
