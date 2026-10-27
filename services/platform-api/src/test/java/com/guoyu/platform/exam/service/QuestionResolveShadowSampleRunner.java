package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.app.exam.service.QuestionResolveAssistService;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 试题解析 shadow 采样 runner（M6 任务 3；《试题解析Markdown表格化_一期实施方案》§10.1）。
 *
 * <p>默认不跑（mvn test 跳过）；显式开启：{@code mvn test -Dtest=QuestionResolveShadowSampleRunner
 * -DquestionResolveShadow=true}。真实启动全上下文：默认 profile 连测试库 gjjddb2026（114.132.67.185:3309）
 * + 本机 Redis；为避免对共享测试库做意外 DDL，运行时关闭 Flyway。
 *
 * <p>走内部 ensureResolve（与 App 入口共用同一生成链：官方读取器→draft 缓存→开关→冷却→Redis 锁→
 * LLM→质量门→shadow draft），仅外壳（登录/限流）不同，不影响 shadow 质量口径。
 *
 * <p>输入：{@code target/shadow-samples.json}（[{id, layer, type, knowledgeTitle}]）；
 * 输出：{@code target/shadow-results.jsonl}（逐题记录）+ 控制台摘要。
 * 硬检查：shadow 不回写 resolve；同 sourceHash 二次调用零 LLM；安全/法规层 official_only 零 LLM。
 */
@ActiveProfiles("courseware-e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.flyway.enabled=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "questionResolveShadow", matches = "true")
class QuestionResolveShadowSampleRunner {

  @Autowired
  private QuestionResolveAssistService assistService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("shadow 分层采样：逐题真实生成 + 硬检查")
  void runShadowSamples() throws Exception {
    File sampleFile = new File("target/shadow-samples.json");
    assertTrue(sampleFile.exists(), "缺少 target/shadow-samples.json（先用 SQL 导出样本清单）");
    ArrayNode samples = (ArrayNode) objectMapper.readTree(Files.readString(sampleFile.toPath(), StandardCharsets.UTF_8));
    assertFalse(samples.isEmpty(), "样本清单为空");

    File outFile = new File("target/shadow-results.jsonl");
    StringBuilder jsonl = new StringBuilder();
    int llmCalls = 0;
    int shadow = 0;
    int rejected = 0;
    int officialOnly = 0;

    for (int i = 0; i < samples.size(); i++) {
      ObjectNode sample = (ObjectNode) samples.get(i);
      long questionId = sample.get("id").asLong();
      String layer = sample.get("layer").asText();
      String type = sample.path("type").asText("");
      String knowledgeTitle = sample.path("knowledgeTitle").asText("");

      TenantContextHolder.setTenantId(10159L);
      long before = draftCount(questionId);
      long start = System.currentTimeMillis();
      Map<String, Object> result;
      try {
        result = assistService.ensureResolve((int) questionId);
      } finally {
        TenantContextHolder.clear();
      }
      long elapsed = System.currentTimeMillis() - start;
      long after = draftCount(questionId);
      String source = String.valueOf(result.get("source"));
      Object reason = result.get("reason");

      ObjectNode row = objectMapper.createObjectNode();
      row.put("idx", i + 1);
      row.put("layer", layer);
      row.put("questionId", questionId);
      row.put("type", type);
      row.put("knowledgeTitle", knowledgeTitle);
      row.put("source", source);
      row.put("reason", reason == null ? null : String.valueOf(reason));
      row.put("generated", Boolean.TRUE.equals(result.get("generated")));
      row.put("draftsBefore", before);
      row.put("draftsAfter", after);
      row.put("elapsedMs", elapsed);
      Map<String, String> latest = latestDraft(questionId);
      row.put("modelName", latest.getOrDefault("modelName", ""));
      row.put("promptVersion", promptVersionOf(latest.get("prompt")));
      row.put("resolveText", result.get("resolve") == null ? null : String.valueOf(result.get("resolve")));
      // 采样后回查：该题 resolve 列必须仍为空（shadow 不回写）
      row.put("resolveColumnStillEmpty", resolveColumnEmpty(questionId));
      if ("shadow".equals(source) && after > before && Boolean.TRUE.equals(result.get("generated"))) {
        llmCalls++;
        shadow++;
      } else if ("empty".equals(source)
          && ("quality_rejected".equals(reason) || "llm_failed".equals(reason))) {
        rejected++;
      } else if ("empty".equals(source) && "official_only".equals(reason)) {
        officialOnly++;
      }
      jsonl.append(objectMapper.writeValueAsString(row)).append('\n');
      Files.writeString(outFile.toPath(), jsonl.toString(), StandardCharsets.UTF_8);
      System.out.printf("[shadow %d/%d] layer=%s id=%d type=%s source=%s reason=%s drafts %d->%d %dms%n",
          i + 1, samples.size(), layer, questionId, type, source, reason, before, after, elapsed);
    }

    // 硬检查 1：全样本第二轮——缓存复用，drafts 不增、generated=false、零 LLM
    int secondRoundLlmCalls = 0;
    for (int i = 0; i < samples.size(); i++) {
      ObjectNode sample = (ObjectNode) samples.get(i);
      long questionId = sample.get("id").asLong();
      long before = draftCount(questionId);
      TenantContextHolder.setTenantId(10159L);
      Map<String, Object> second;
      try {
        second = assistService.ensureResolve((int) questionId);
      } finally {
        TenantContextHolder.clear();
      }
      long after = draftCount(questionId);
      boolean generatedAgain = Boolean.TRUE.equals(second.get("generated"));
      assertTrue(before == after, "第二轮 drafts 不应新增：questionId=" + questionId);
      assertFalse(generatedAgain, "第二轮不应再调用 LLM：questionId=" + questionId);
      if (generatedAgain || after > before) {
        secondRoundLlmCalls++;
      }
      System.out.printf("[round2 %d/%d] id=%d source=%s generated=%s drafts %d->%d%n",
          i + 1, samples.size(), questionId, second.get("source"), second.get("generated"), before, after);
    }
    assertTrue(secondRoundLlmCalls == 0, "第二轮 LLM 调用必须为 0");

    System.out.printf("[summary] total=%d llmCalls=%d shadow=%d officialOnly=%d secondRoundLlmCalls=%d%n",
        samples.size(), llmCalls, shadow, officialOnly, secondRoundLlmCalls);
  }

  private ObjectNode findFirstShadowSample(ArrayNode samples, File outFile) throws Exception {
    List<String> lines = Files.readAllLines(outFile.toPath(), StandardCharsets.UTF_8);
    for (String line : lines) {
      ObjectNode row = (ObjectNode) objectMapper.readTree(line);
      if ("shadow".equals(row.path("source").asText())) {
        long qid = row.get("questionId").asLong();
        for (int i = 0; i < samples.size(); i++) {
          ObjectNode s = (ObjectNode) samples.get(i);
          if (s.get("id").asLong() == qid) {
            return s;
          }
        }
      }
    }
    return null;
  }

  /** 最新一条 draft 的 model_name / prompt（留痕审计用）。 */
  private Map<String, String> latestDraft(long questionId) {
    try {
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          "SELECT model_name, prompt FROM ai_generation_draft WHERE draft_type='question_resolve' "
              + "AND source_type='exam_exam_detail' AND source_id=? AND deleted=0 "
              + "ORDER BY id DESC LIMIT 1",
          questionId);
      if (rows.isEmpty()) {
        return Map.of();
      }
      Object modelName = rows.get(0).get("model_name");
      Object prompt = rows.get(0).get("prompt");
      return Map.of(
          "modelName", modelName == null ? "" : String.valueOf(modelName),
          "prompt", prompt == null ? "" : String.valueOf(prompt));
    } catch (Exception e) {
      return Map.of();
    }
  }

  /** draft.prompt 前缀 → prompt 版本标记（v2 / v2.1 / v2-rejected / unknown）。 */
  private static String promptVersionOf(String prompt) {
    if (prompt == null) {
      return "";
    }
    if (prompt.startsWith("question_resolve_v2.1#")) {
      return "v2.1";
    }
    if (prompt.startsWith("question_resolve_v2.1_rejected#")) {
      return "v2.1-rejected";
    }
    if (prompt.startsWith("question_resolve_v2#")) {
      return "v2";
    }
    if (prompt.startsWith("question_resolve_v2_rejected#")) {
      return "v2-rejected";
    }
    return "legacy";
  }

  private long draftCount(long questionId) {
    Long count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM ai_generation_draft WHERE draft_type='question_resolve' "
            + "AND source_type='exam_exam_detail' AND source_id=? AND deleted=0",
        Long.class, questionId);
    return count == null ? 0 : count;
  }

  private boolean resolveColumnEmpty(long questionId) {
    Integer len = jdbcTemplate.queryForObject(
        "SELECT CHAR_LENGTH(IFNULL(TRIM(resolve),'')) FROM exam_exam_detail WHERE id=?",
        Integer.class, questionId);
    return len == null || len == 0;
  }
}
