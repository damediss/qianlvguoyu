package com.guoyu.platform.app.coach.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "platform.weknora")
public record WeKnoraProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    /** 逗号分隔，如 kb-001,kb-002 */
    String defaultKnowledgeBaseIds,
    String defaultAgentId,
    boolean useAgentChat,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    /** 拍照搜题：先用视觉 API 识别成文字，再交给 WeKnora（绕过 WeKnora VLM 兼容问题） */
    boolean visionPreprocessEnabled,
    String visionApiUrl,
    String visionApiKey,
    String visionModel
) {

  public WeKnoraProperties {
    if (baseUrl != null && baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    if (!StringUtils.hasText(defaultAgentId)) {
      defaultAgentId = "builtin-quick-answer";
    }
    if (connectTimeoutSeconds <= 0) {
      connectTimeoutSeconds = 15;
    }
    if (readTimeoutSeconds <= 0) {
      readTimeoutSeconds = 120;
    }
    if (!StringUtils.hasText(visionModel)) {
      visionModel = "MiniMax-M3";
    }
  }

  public List<String> defaultKnowledgeBaseIdList() {
    if (!StringUtils.hasText(defaultKnowledgeBaseIds)) {
      return List.of();
    }
    return Arrays.stream(defaultKnowledgeBaseIds.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList();
  }
}
