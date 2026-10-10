package com.guoyu.platform.app.coach.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.coach.service.AppTrainAiCoachService;
import com.guoyu.platform.app.sms.AppSmsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 培训 AI 陪练（platform-api 代理 WeKnora）。
 *
 * <p>POST /appcoach/session — 创建对话会话
 * <p>POST /appcoach/chat — 文字问答(JSON) 或 拍照搜题(multipart，字段 file)
 * <p>POST /appcoach/photo — 拍照搜题专用（multipart，与 /chat 等价）
 */
@RestController
@RequestMapping("/appcoach")
@Slf4j
public class AppTrainAiCoachController {

  private final AppTrainAiCoachService coachService;
  private final ObjectMapper objectMapper;

  public AppTrainAiCoachController(
      AppTrainAiCoachService coachService,
      ObjectMapper objectMapper) {
    this.coachService = coachService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/session")
  public LegacyAppResponse createSession(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", coachService.createSession(body == null ? Map.of() : body));
  }

  /**
   * 统一入口：根据 Content-Type 分流 JSON / multipart，避免 consumes 匹配导致 415。
   */
  @PostMapping("/chat")
  public LegacyAppResponse chat(HttpServletRequest request)
      throws IOException, jakarta.servlet.ServletException {
    if (isMultipart(request)) {
      log.info("[appcoach] chat multipart upload");
      return chatImageMultipart(request);
    }
    Map<String, Object> body = readJsonBody(request);
    return LegacyAppResponse.ok("成功", coachService.chat(body));
  }

  @PostMapping("/photo")
  public LegacyAppResponse photo(HttpServletRequest request)
      throws IOException, jakarta.servlet.ServletException {
    log.info("[appcoach] photo upload");
    return chatImageMultipart(request);
  }

  private LegacyAppResponse chatImageMultipart(HttpServletRequest request)
      throws IOException, jakarta.servlet.ServletException {
    Part filePart = null;
    String sessionId = null;
    String question = null;
    String trade = null;
    String chapter = null;
    for (Part part : request.getParts()) {
      String name = part.getName();
      if ("file".equals(name)) {
        filePart = part;
      } else if ("sessionId".equals(name)) {
        sessionId = readPartText(part);
      } else if ("question".equals(name)) {
        question = readPartText(part);
      } else if ("trade".equals(name)) {
        trade = readPartText(part);
      } else if ("chapter".equals(name)) {
        chapter = readPartText(part);
      }
    }
    if (filePart == null) {
      throw new AppSmsException(-1, "请上传题目图片");
    }
    byte[] content;
    try (InputStream in = filePart.getInputStream()) {
      content = in.readAllBytes();
    }
    if (content.length == 0) {
      throw new AppSmsException(-1, "图片内容为空");
    }
    return LegacyAppResponse.ok("成功", coachService.chatWithImageBytes(
        content,
        filePart.getContentType(),
        filePart.getSubmittedFileName(),
        sessionId,
        question,
        trade,
        chapter));
  }

  private Map<String, Object> readJsonBody(HttpServletRequest request) throws IOException {
    byte[] raw;
    try (InputStream in = request.getInputStream()) {
      raw = in.readAllBytes();
    }
    if (raw.length == 0) {
      return Map.of();
    }
    return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
  }

  private static boolean isMultipart(HttpServletRequest request) {
    String contentType = request.getContentType();
    return StringUtils.hasText(contentType)
        && contentType.toLowerCase().startsWith("multipart/");
  }

  private static String readPartText(Part part) throws IOException {
    try (InputStream in = part.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleAny(Exception ex) {
    log.error("[appcoach] 未处理异常", ex);
    return LegacyAppResponse.fail(500, "服务异常：" + ex.getMessage());
  }
}
