package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareJson;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestTopic;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.entity.CwDigestEntity;
import com.guoyu.platform.courseware.mapper.CwDigestMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 素材消化层(方案 §6.5):标准条文 → 教学化素材(核心知识点/易混淆点/关键数据)。
 * 失败返回 null 走降级(消化层失败不阻断主流程)。
 */
@Slf4j
@Service
public class CoursewareDigestService {

  /** 输入裁剪预算(方案 §6.5.1):单节 2000、总 12000 */
  static final int MAX_SECTION_CHARS = 2_000;
  static final int MAX_TOTAL_CHARS = 12_000;

  /** 数字回查:至少两位数或小数或百分数(避免序号误报) */
  private static final Pattern NUMBER_RE = Pattern.compile(
      "\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?%?|\\d+\\.\\d+%?|\\d{2,}%?");

  private final CoursewareAiService aiService;
  private final CwDigestMapper digestMapper;
  private final ObjectMapper objectMapper;

  public CoursewareDigestService(CoursewareAiService aiService, CwDigestMapper digestMapper,
                                 ObjectMapper objectMapper) {
    this.aiService = aiService;
    this.digestMapper = digestMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * 尝试生成消化素材。LLM 异常或两轮校验不过 → 返回 null(降级,主流程继续)。
   */
  public DigestDraft tryDigest(Long tenantId, Long projectId, Long operatorId,
                               String title, String audience, List<SourceSection> sections) {
    List<SourceSection> trimmed = trimSections(sections, MAX_SECTION_CHARS, MAX_TOTAL_CHARS);
    Set<String> validRefs = validRefs(sections);
    String allSourceText = concatSourceText(sections);
    String userPrompt = CoursewarePrompts.digestUser(title, audience, trimmed, objectMapper);
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        String content = aiService.callDigest(tenantId, projectId, operatorId,
            CoursewarePrompts.digestSystem(), userPrompt);
        DigestDraft draft = parseAndValidate(content, validRefs, allSourceText, objectMapper);
        persist(tenantId, projectId, operatorId, draft);
        return draft;
      } catch (IllegalArgumentException e) {
        log.warn("课件消化输出校验失败(第 {} 轮): projectId={}, error={}", attempt + 1, projectId,
            e.getMessage());
        userPrompt += "\n上次输出不符合约定,必须只输出合法 JSON 对象,不要任何额外文字。";
      } catch (RuntimeException e) {
        log.warn("课件消化 LLM 调用失败: projectId={}, error={}", projectId, e.getMessage());
        return null;
      }
    }
    log.warn("课件消化两轮校验不过,降级为无消化素材: projectId={}", projectId);
    return null;
  }

  /** 输出零信任校验:结构/refs 合法性/topic 钳制 [3,8]/key_data 数字回查剔除 */
  public static DigestDraft parseAndValidate(String content, Set<String> validRefs, String allSourceText,
                                      ObjectMapper om) {
    JsonNode root = CoursewareJson.extractObject(content, om);
    JsonNode topicsNode = root.path("topics");
    if (!topicsNode.isArray() || topicsNode.isEmpty()) {
      throw new IllegalArgumentException("topics 缺失或为空");
    }
    List<DigestTopic> topics = new ArrayList<>();
    for (JsonNode node : topicsNode) {
      String name = textOr(node, "name");
      if (name == null || name.isBlank()) {
        continue;
      }
      List<String> points = stringList(node.path("points"));
      List<String> confusions = stringList(node.path("confusions"));
      List<String> keyData = filterKeyData(stringList(node.path("key_data")), allSourceText);
      List<String> refs = stringList(node.path("source_refs")).stream()
          .filter(validRefs::contains)
          .distinct()
          .toList();
      topics.add(new DigestTopic(name.trim(), points, confusions, keyData, refs));
    }
    if (topics.size() > 8) {
      topics = new ArrayList<>(topics.subList(0, 8));
    }
    if (topics.size() < 3) {
      throw new IllegalArgumentException("topics 不足 3 个: " + topics.size());
    }
    return new DigestDraft(topics);
  }

  /** key_data 数字回查:条目中的数字在来源文本里找不到 → 整条剔除(防编造,方案 §6.5.3) */
  static List<String> filterKeyData(List<String> keyData, String allSourceText) {
    if (keyData.isEmpty()) {
      return keyData;
    }
    String haystack = allSourceText == null ? "" : allSourceText.replace(",", "").replace(" ", "");
    List<String> kept = new ArrayList<>();
    for (String item : keyData) {
      boolean allFound = true;
      var matcher = NUMBER_RE.matcher(item);
      while (matcher.find()) {
        String token = matcher.group();
        if (!numberVariantsFound(token, haystack)) {
          allFound = false;
          break;
        }
      }
      if (allFound) {
        kept.add(item);
      }
    }
    return kept;
  }

  private static boolean numberVariantsFound(String token, String haystack) {
    String raw = token.replace(",", "");
    Set<String> variants = new LinkedHashSet<>();
    variants.add(raw);
    variants.add(raw.replace("%", ""));
    String bare = raw.replace("%", "");
    try {
      double v = Double.parseDouble(bare);
      if (v == Math.floor(v)) {
        variants.add(String.valueOf((long) v));
      }
    } catch (NumberFormatException ignored) {
      // 非纯数字变体跳过
    }
    return variants.stream().anyMatch(haystack::contains);
  }

  private void persist(Long tenantId, Long projectId, Long operatorId, DigestDraft draft) {
    try {
      CwDigestEntity entity = new CwDigestEntity();
      entity.setId(IdWorker.getId());
      entity.setTenantId(tenantId);
      entity.setProjectId(projectId);
      entity.setTopics(objectMapper.writeValueAsString(draft.topics()));
      entity.setStatus(CwDigestEntity.STATUS_READY);
      entity.setCreatedBy(operatorId);
      entity.setUpdatedBy(operatorId);
      digestMapper.insert(entity);
    } catch (Exception e) {
      // 落库失败视同降级(产物仍在内存可用)
      log.warn("消化素材落库失败(降级继续): projectId={}, error={}", projectId, e.getMessage());
    }
  }

  /** 裁剪:单节上限 + 总预算,按整节追加超长截断该节正文(引用不错位) */
  public static List<SourceSection> trimSections(List<SourceSection> sections, int maxSectionChars,
                                          int maxTotalChars) {
    List<SourceSection> out = new ArrayList<>();
    int used = 0;
    for (SourceSection section : sections) {
      String text = section.text() == null ? "" : section.text().replaceAll("\\s+", " ").trim();
      if (text.isEmpty() && (section.heading() == null || section.heading().isBlank())) {
        continue;
      }
      if (text.length() > maxSectionChars) {
        text = text.substring(0, maxSectionChars);
      }
      int remaining = maxTotalChars - used;
      if (remaining <= 0) {
        break;
      }
      if (text.length() > remaining) {
        text = text.substring(0, remaining);
      }
      out.add(new SourceSection(section.ref(), section.heading(), section.level(), text,
          section.locator()));
      used += text.length();
    }
    return out;
  }

  public static Set<String> validRefs(List<SourceSection> sections) {
    Set<String> refs = new LinkedHashSet<>();
    sections.forEach(s -> refs.add(s.ref()));
    return refs;
  }

  private static String concatSourceText(List<SourceSection> sections) {
    StringBuilder sb = new StringBuilder();
    sections.forEach(s -> sb.append(s.heading() == null ? "" : s.heading()).append('\n')
        .append(s.text() == null ? "" : s.text()).append('\n'));
    return sb.toString();
  }

  private static String textOr(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isTextual() ? v.asText() : null;
  }

  private static List<String> stringList(JsonNode node) {
    if (!node.isArray()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (JsonNode item : node) {
      if (item.isTextual()) {
        out.add(item.asText());
      }
    }
    return out;
  }
}
