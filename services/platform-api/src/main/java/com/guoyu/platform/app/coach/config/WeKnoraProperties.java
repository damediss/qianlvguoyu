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
    String visionModel,
    /**
     * 小培专用知识库 ID（逗号分隔）。空则回退 {@link #defaultKnowledgeBaseIds}，
     * 再空则按 {@link #xiaopeiKnowledgeBaseName} 在 WeKnora 列表中解析。
     */
    String xiaopeiKnowledgeBaseIds,
    /** 小培默认知识库名称（控制台展示名），如「AI助教_智能问答」 */
    String xiaopeiKnowledgeBaseName,
    /** 小培专用智能体；空则用 {@link #defaultAgentId} */
    String xiaopeiAgentId
) {

  public WeKnoraProperties {
    if (baseUrl != null && baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    // 与嵌入页共用 WEKNORA_BASE_URL（主机根，无 /api/v1）时，陪练/小培客户端自动补全 API 前缀
    if (StringUtils.hasText(baseUrl) && !baseUrl.endsWith("/api/v1")) {
      baseUrl = baseUrl + "/api/v1";
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
    if (!StringUtils.hasText(xiaopeiKnowledgeBaseName)) {
      xiaopeiKnowledgeBaseName = "AI助教_智能问答";
    }
  }

  public List<String> defaultKnowledgeBaseIdList() {
    return splitIds(defaultKnowledgeBaseIds);
  }

  public List<String> xiaopeiKnowledgeBaseIdList() {
    List<String> ids = splitIds(xiaopeiKnowledgeBaseIds);
    if (!ids.isEmpty()) {
      return ids;
    }
    return defaultKnowledgeBaseIdList();
  }

  public String resolveXiaopeiAgentId() {
    return StringUtils.hasText(xiaopeiAgentId) ? xiaopeiAgentId.trim() : defaultAgentId;
  }

  public boolean isApiReady() {
    return enabled && StringUtils.hasText(apiKey);
  }

  private static List<String> splitIds(String csv) {
    if (!StringUtils.hasText(csv)) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList();
  }
}
