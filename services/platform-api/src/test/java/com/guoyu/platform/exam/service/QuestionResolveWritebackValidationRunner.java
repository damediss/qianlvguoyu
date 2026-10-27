package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.app.exam.service.QuestionResolveAssistService;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.service.ReviewQuestionQueryService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 写回验证 runner（M6 任务 B：同 JVM 证明同步学缓存一致性与人工解析保护）。
 *
 * <p>默认不跑；显式开启：{@code mvn test -Dtest=QuestionResolveWritebackValidationRunner
 * -DquestionResolveWriteback=true}。要求测试库 generation=1、writeback=1。
 */
@ActiveProfiles("courseware-e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.flyway.enabled=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "questionResolveWriteback", matches = "true")
class QuestionResolveWritebackValidationRunner {

  @Autowired
  private QuestionResolveAssistService assistService;

  @Autowired
  private ReviewQuestionQueryService reviewQuestionQueryService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void writebackCacheConsistencyAndHumanProtection() {
    // 1) 同步学缓存一致性：先读一次（缓存空解析）→ 生成 → 再读必须看到新解析
    long cacheQuestionId = 76L;
    TenantContextHolder.setTenantId(10159L);
    ReviewQuestionDetail before = reviewQuestionQueryService.loadQuestionDetail(cacheQuestionId);
    System.out.printf("[wb-cache] before: resolve=%s%n", before == null ? "null-detail" : before.resolve());
    Map<String, Object> generated = assistService.ensureResolve((int) cacheQuestionId);
    System.out.printf("[wb-gen] source=%s generated=%s resolveLen=%d%n",
        generated.get("source"), generated.get("generated"),
        generated.get("resolve") == null ? 0 : String.valueOf(generated.get("resolve")).length());
    assertEquals("ai", generated.get("source"), "writeback=1 下首次生成应回写并返回 ai");
    assertEquals(Boolean.TRUE, generated.get("generated"));
    ReviewQuestionDetail after = reviewQuestionQueryService.loadQuestionDetail(cacheQuestionId);
    assertNotNull(after, "生成后同步学必须能读到题目详情");
    assertTrue(after.resolve() != null && !after.resolve().isBlank(),
        "生成后同步学详情必须拿到新解析（M4 空解析回源 + evict）");
    System.out.printf("[wb-cache] after: resolveLen=%d head=%s%n",
        after.resolve().length(), after.resolve().substring(0, Math.min(40, after.resolve().length())));
    TenantContextHolder.clear();

    // 2) 人工解析保护：手工写入 resolve 后再触发，AI 不得覆盖（source=db，列内容不变）
    long humanQuestionId = 57L;
    String humanText = "人工解析：该列由管理员手工维护，AI 不得覆盖。";
    jdbcTemplate.update("UPDATE exam_exam_detail SET resolve=? WHERE id=?", humanText, humanQuestionId);
    TenantContextHolder.setTenantId(10159L);
    Map<String, Object> humanResult = assistService.ensureResolve((int) humanQuestionId);
    TenantContextHolder.clear();
    assertEquals("db", humanResult.get("source"), "已有人工解析时应直读返回 db，不触发生成");
    String columnNow = jdbcTemplate.queryForObject(
        "SELECT resolve FROM exam_exam_detail WHERE id=?", String.class, humanQuestionId);
    assertEquals(humanText, columnNow, "人工解析内容不得被 AI 覆盖");
    System.out.printf("[human-guard] source=%s column-kept=%s%n",
        humanResult.get("source"), humanText.equals(columnNow));

    // 3) generation=0：返回可理解的 disabled，不调 LLM、不写列
    jdbcTemplate.update(
        "UPDATE sys_dict SET value='0' WHERE code='question_resolve_generation_enabled' AND tenant_id=10159");
    long disabledQuestionId = 25L;
    TenantContextHolder.setTenantId(10159L);
    Map<String, Object> disabled;
    try {
      disabled = assistService.ensureResolve((int) disabledQuestionId);
    } finally {
      TenantContextHolder.clear();
    }
    assertEquals("empty", disabled.get("source"));
    assertEquals("disabled", disabled.get("reason"));
    jdbcTemplate.update(
        "UPDATE sys_dict SET value='1' WHERE code='question_resolve_generation_enabled' AND tenant_id=10159");
    Integer columnLen = jdbcTemplate.queryForObject(
        "SELECT CHAR_LENGTH(IFNULL(resolve,'')) FROM exam_exam_detail WHERE id=?",
        Integer.class, disabledQuestionId);
    assertTrue(columnLen == null || columnLen == 0, "disabled 下不得写 resolve");
    assertFalse(Boolean.TRUE.equals(disabled.get("generated")));
    System.out.printf("[disabled] source=%s reason=%s%n", disabled.get("source"), disabled.get("reason"));
  }
}
