package com.guoyu.platform.app.coach.service;

import com.guoyu.platform.app.coach.client.CoachVisionClient;
import com.guoyu.platform.app.coach.client.WeKnoraClient;
import com.guoyu.platform.app.coach.client.WeKnoraClient.WeKnoraChatResult;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** App 培训详情 · AI 陪练（WeKnora 知识库对话代理）。 */
@Service
@Slf4j
public class AppTrainAiCoachService {

  private static final String PHOTO_SEARCH_PROMPT =
      "请阅读附带图片中的题目文字，给出详细解答，并结合本地知识库进行智能讲解和知识拓展。";

  private final WeKnoraClient weKnoraClient;
  private final CoachVisionClient coachVisionClient;

  public AppTrainAiCoachService(
      WeKnoraClient weKnoraClient,
      CoachVisionClient coachVisionClient) {
    this.weKnoraClient = weKnoraClient;
    this.coachVisionClient = coachVisionClient;
  }

  /** 创建 WeKnora 会话（客户端缓存 sessionId 后复用）。 */
  public Map<String, Object> createSession(Map<String, Object> body) {
    requireLogin();
    String trade = strVal(body, "trade", "培训");
    String chapter = strVal(body, "chapter", "");
    String title = strVal(body, "title", trade + " · AI陪练");
    String description = buildContext(trade, chapter, strVal(body, "context", ""));
    String sessionId = weKnoraClient.createSession(title, description);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("sessionId", sessionId);
    data.put("title", title);
    return data;
  }

  /** 发送问题并返回聚合后的回答。 */
  public Map<String, Object> chat(Map<String, Object> body) {
    requireLogin();
    String question = strVal(body, "question", null);
    List<String> images = parseImageDataUrls(body);
    if (!images.isEmpty()) {
      log.info("[appcoach] chat with {} image(s), questionLen={}", images.size(), question.length());
    }
    if (!StringUtils.hasText(question) && images.isEmpty()) {
      throw new AppSmsException(-1, "请输入问题或上传题目图片");
    }
    if (!StringUtils.hasText(question)) {
      question = PHOTO_SEARCH_PROMPT;
    }

    String sessionId = strVal(body, "sessionId", null);
    if (!StringUtils.hasText(sessionId)) {
      String trade = strVal(body, "trade", "培训");
      String chapter = strVal(body, "chapter", "");
      sessionId = weKnoraClient.createSession(
          trade + " · AI陪练",
          buildContext(trade, chapter, ""));
    }

    List<String> kbIds = parseKnowledgeBaseIds(body);
    String agentId = strVal(body, "agentId", null);

    WeKnoraChatResult result;
    if (!images.isEmpty() && coachVisionClient.isConfigured()) {
      result = chatWithVisionPreprocess(sessionId, question, kbIds, agentId, images.get(0));
    } else {
      result = weKnoraClient.knowledgeChat(
          sessionId, question, kbIds, agentId, images);
      if (!images.isEmpty() && looksLikeImageNotReceived(result.answer())) {
        log.warn("[appcoach] WeKnora may not have parsed image, retry builtin-quick-answer");
        result = weKnoraClient.knowledgeChat(
            sessionId, question, kbIds, "builtin-quick-answer", images);
      }
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("sessionId", sessionId);
    data.put("answer", result.answer());
    data.put("references", result.references());
    if (StringUtils.hasText(result.sessionTitle())) {
      data.put("sessionTitle", result.sessionTitle());
    }
    return data;
  }

  /** 拍照搜题：multipart 上传图片，服务端转 base64 再转发 WeKnora。 */
  public Map<String, Object> chatWithImageBytes(
      byte[] bytes,
      String contentType,
      String filename,
      String sessionId,
      String question,
      String trade,
      String chapter) {
    if (bytes == null || bytes.length == 0) {
      throw new AppSmsException(-1, "请上传题目图片");
    }
    if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
      contentType = "image/jpeg";
    }
    try {
      if (bytes.length > 10 * 1024 * 1024) {
        throw new AppSmsException(-1, "图片过大，请压缩后重试");
      }
      String dataUrl = "data:" + contentType + ";base64,"
          + Base64.getEncoder().encodeToString(bytes);
      log.info("[appcoach] chat-image size={}KB type={} name={}",
          bytes.length / 1024, contentType, filename);

      Map<String, Object> body = new LinkedHashMap<>();
      if (StringUtils.hasText(sessionId)) {
        body.put("sessionId", sessionId);
      }
      body.put("question", StringUtils.hasText(question) ? question : PHOTO_SEARCH_PROMPT);
      if (StringUtils.hasText(trade)) {
        body.put("trade", trade);
      }
      if (StringUtils.hasText(chapter)) {
        body.put("chapter", chapter);
      }
      body.put("images", List.of(dataUrl));
      return chat(body);
    } catch (AppSmsException e) {
      throw e;
    } catch (Exception e) {
      log.error("[appcoach] chat-image failed", e);
      throw new AppSmsException(-1, "图片处理失败：" + e.getMessage());
    }
  }

  private WeKnoraChatResult chatWithVisionPreprocess(
      String sessionId,
      String question,
      List<String> kbIds,
      String agentId,
      String dataUrl) {
    String ocr = coachVisionClient.describeImage(dataUrl, PHOTO_SEARCH_PROMPT);
    String enriched = """
        用户拍摄了培训题目图片，识别内容如下：
        ---
        %s
        ---
        请基于以上内容，结合本地知识库进行详细解答、智能讲解和知识拓展。
        """.formatted(ocr);
    if (StringUtils.hasText(question) && !PHOTO_SEARCH_PROMPT.equals(question.trim())) {
      enriched = enriched + "\n用户补充说明：" + question.trim();
    }
    log.info("[appcoach] vision preprocess -> text-only WeKnora chat");
    return weKnoraClient.knowledgeChat(sessionId, enriched, kbIds, agentId, List.of());
  }

  private static boolean looksLikeImageNotReceived(String answer) {
    if (!StringUtils.hasText(answer)) {
      return true;
    }
    String a = answer.toLowerCase();
    return answer.contains("没有附带图片")
        || answer.contains("看不到任何图片")
        || answer.contains("看不到图片")
        || answer.contains("未看到任何图片")
        || answer.contains("没有图片")
        || answer.contains("no picture attached")
        || answer.contains("no image attached")
        || (a.contains("re-upload") && a.contains("image"));
  }

  private static String buildContext(String trade, String chapter, String extra) {
    StringBuilder sb = new StringBuilder("App 培训 AI 陪练");
    if (StringUtils.hasText(trade)) {
      sb.append("，工种/课程：").append(trade);
    }
    if (StringUtils.hasText(chapter)) {
      sb.append("，当前章节：").append(chapter);
    }
    if (StringUtils.hasText(extra)) {
      sb.append("。").append(extra);
    }
    return sb.toString();
  }

  private static List<String> parseImageDataUrls(Map<String, Object> body) {
    if (body == null || body.get("images") == null) {
      return List.of();
    }
    Object raw = body.get("images");
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<String> images = new ArrayList<>();
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      if (item instanceof Map<?, ?> map) {
        Object data = map.get("data");
        if (data != null && StringUtils.hasText(String.valueOf(data))) {
          images.add(String.valueOf(data).trim());
        }
        continue;
      }
      String dataUrl = String.valueOf(item).trim();
      if (StringUtils.hasText(dataUrl)) {
        images.add(dataUrl);
      }
    }
    return images;
  }

  private static List<String> parseKnowledgeBaseIds(Map<String, Object> body) {
    if (body == null || body.get("knowledgeBaseIds") == null) {
      return List.of();
    }
    Object raw = body.get("knowledgeBaseIds");
    if (raw instanceof List<?> list) {
      List<String> ids = new ArrayList<>();
      for (Object item : list) {
        if (item != null && StringUtils.hasText(String.valueOf(item))) {
          ids.add(String.valueOf(item).trim());
        }
      }
      return ids;
    }
    String csv = String.valueOf(raw).trim();
    if (!StringUtils.hasText(csv)) {
      return List.of();
    }
    return List.of(csv.split(","));
  }

  private static PlatformUserPrincipal requireLogin() {
    try {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      if (user == null || user.getUserId() == null) {
        throw new AppSmsException(-1, "请先登录");
      }
      return user;
    } catch (IllegalStateException e) {
      throw new AppSmsException(-1, "请先登录");
    }
  }

  private static String strVal(Map<String, Object> body, String key, String def) {
    if (body == null || body.get(key) == null) {
      return def;
    }
    String s = String.valueOf(body.get(key)).trim();
    return s.isEmpty() ? def : s;
  }
}
