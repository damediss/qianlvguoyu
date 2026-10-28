package com.guoyu.platform.exam.answercheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.ai.dto.ObjectiveAnswerCallOutcome;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOption;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOptionDecision;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 客观题答案 AI 检查主链路 IT（真实 MySQL + Flyway，AI 全部走桩）。
 *
 * <p>覆盖：筛选项/分页/预览/创建/轮询/结果/详情/复核/导出、权限与跨租户、
 * 内容质量零调用、伪造参数不生效、复核不改题库。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
class ObjectiveAnswerCheckApiIT {

  private static final long TENANT_ID = 10159L;
  private static final long OTHER_TENANT = 20200L;
  private static final int WORK_TYPE_ID = 990_028_901;
  private static final int COURSE_ID = 990_028_951;
  private static final int KNOWLEDGE_PARENT = 990_028_961;
  private static final int KNOWLEDGE_ID = 990_028_962;
  private static final int DETAIL_SINGLE = 990_028_971;
  private static final int DETAIL_MULTI = 990_028_972;
  private static final int DETAIL_JUDGE = 990_028_973;
  private static final int DETAIL_INVALID = 990_028_974;
  private static final int DETAIL_CONTENT_INVALID = 990_028_975;
  private static final int DETAIL_SUSPECT = 990_028_976;
  private static final int DETAIL_MEDIA = 990_028_977;
  private static final int DETAIL_UNLINKED = 990_028_978;
  private static final List<Integer> ALL_DETAILS = List.of(DETAIL_SINGLE, DETAIL_MULTI,
      DETAIL_JUDGE, DETAIL_INVALID, DETAIL_CONTENT_INVALID, DETAIL_SUSPECT, DETAIL_MEDIA,
      DETAIL_UNLINKED);

  private static final String SINGLE_ANSWER = """
      [{"content":"红色","isCorrect":false},{"content":"蓝色","isCorrect":true},
       {"content":"黑色","isCorrect":false},{"content":"白色","isCorrect":false}]
      """;
  private static final String MULTI_ANSWER = """
      [{"content":"甲","isCorrect":true},{"content":"乙","isCorrect":true},
       {"content":"丙","isCorrect":false},{"content":"丁","isCorrect":false}]
      """;
  private static final String JUDGE_ANSWER = """
      [{"content":"正确","isCorrect":true},{"content":"错误","isCorrect":false}]
      """;

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private JdbcTemplate jdbc;
  @MockBean
  private ObjectiveAnswerCheckAiService aiService;

  private final List<Long> createdBatches = new ArrayList<>();

  @BeforeEach
  void setUp() {
    cleanUp();
    when(aiService.descriptor())
        .thenReturn(new ObjectiveAnswerCheckAiDescriptor("ateve-jev-v1", true));
    when(aiService.check(any(), any(), any())).thenAnswer(invocation -> {
      com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion question = invocation.getArgument(0);
      var context = invocation.getArgument(1,
          com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext.class);
      @SuppressWarnings("unchecked")
      java.util.function.Consumer<ObjectiveAnswerCallOutcome> sink =
          invocation.getArgument(2);
      sink.accept(new ObjectiveAnswerCallOutcome(UUID.randomUUID().toString(), context.batchId(),
          context.itemId(), question.detailId(), context.runId(), context.claimAttempt(), 1,
          "multi".equals(question.questionType()) ? 1 : 1, question.questionType(), "ateve-jev-v1",
          "ateve-jev-v1", "maximum candidate probability", "sha", "preview", 10L, 0L, false, 5,
          "success", null, null));
      return decisionFor(question);
    });

    jdbc.update("DELETE FROM exam_work_type WHERE id = ?", WORK_TYPE_ID);
    jdbc.update("DELETE FROM exam_course WHERE id = ?", COURSE_ID);
    jdbc.update("DELETE FROM exam_knowledge_structure WHERE course_id = ?", COURSE_ID);
    jdbc.update("INSERT INTO exam_work_type (id, tenant_id, title, parent_id, deleted, status) "
        + "VALUES (?, ?, ?, 0, 0, 1)", WORK_TYPE_ID, TENANT_ID, "IT熔铸工");
    jdbc.update("INSERT INTO exam_course (id, tenant_id, work_type_id, title, level, deleted) "
        + "VALUES (?, ?, ?, ?, 1, 0)", COURSE_ID, TENANT_ID, WORK_TYPE_ID, "IT课程");
    jdbc.update("INSERT INTO exam_knowledge_structure (id, tenant_id, course_id, parent_id, title, "
        + "type, level, status, deleted) VALUES (?, ?, ?, 0, '第一章', 1, 1, 1, 0)",
        KNOWLEDGE_PARENT, TENANT_ID, COURSE_ID);
    jdbc.update("INSERT INTO exam_knowledge_structure (id, tenant_id, course_id, parent_id, title, "
        + "type, level, status, deleted) VALUES (?, ?, ?, ?, '安全知识', 1, 2, 1, 0)",
        KNOWLEDGE_ID, TENANT_ID, COURSE_ID, KNOWLEDGE_PARENT);

    insertDetail(DETAIL_SINGLE, "single", "<p>海洋是什么颜色的？</p>", SINGLE_ANSWER, KNOWLEDGE_ID,
        null);
    insertDetail(DETAIL_MULTI, "multi", "<p>以下哪些属于正确项？</p>", MULTI_ANSWER, KNOWLEDGE_ID,
        null);
    insertDetail(DETAIL_JUDGE, "judge", "<p>地球比太阳大</p>", JUDGE_ANSWER, KNOWLEDGE_ID, null);
    insertDetail(DETAIL_INVALID, "single", "<p>没有正确答案的题</p>",
        "[{\"content\":\"A\",\"isCorrect\":false},{\"content\":\"B\",\"isCorrect\":false}]",
        KNOWLEDGE_ID, null);
    insertDetail(DETAIL_CONTENT_INVALID, "single", "<p>内容异常题</p>",
        "[{\"content\":\"#REF!\",\"isCorrect\":true},{\"content\":\"B\",\"isCorrect\":false}]",
        KNOWLEDGE_ID, null);
    insertDetail(DETAIL_SUSPECT, "single", "<p>格式疑似题</p>",
        "[{\"content\":\"1.2E+3\",\"isCorrect\":true},{\"content\":\"B\",\"isCorrect\":false}]",
        KNOWLEDGE_ID, null);
    insertDetail(DETAIL_MEDIA, "single", "<p>看图题</p>", SINGLE_ANSWER, KNOWLEDGE_ID, "a.mp3");
    insertDetail(DETAIL_UNLINKED, "single", "<p>无课程题</p>", SINGLE_ANSWER, null, null);
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  private void cleanUp() {
    for (Long batchId : createdBatches) {
      jdbc.update("DELETE FROM exam_objective_answer_check_call_log WHERE batch_id = ?", batchId);
      jdbc.update("DELETE FROM exam_objective_answer_check_item WHERE batch_id = ?", batchId);
      jdbc.update("DELETE FROM exam_objective_answer_check_batch WHERE id = ?", batchId);
      jdbc.update("DELETE FROM sys_audit_log WHERE target_id = ?", String.valueOf(batchId));
    }
    createdBatches.clear();
    String placeholders = String.join(",", java.util.Collections.nCopies(ALL_DETAILS.size(), "?"));
    jdbc.update("DELETE FROM exam_exam_detail WHERE id IN (" + placeholders + ")",
        ALL_DETAILS.toArray());
    jdbc.update("DELETE FROM exam_work_type WHERE id = ?", WORK_TYPE_ID);
    jdbc.update("DELETE FROM exam_course WHERE id = ?", COURSE_ID);
    jdbc.update("DELETE FROM exam_knowledge_structure WHERE course_id = ?", COURSE_ID);
  }

  private void insertDetail(int id, String type, String content, String answer, Integer knowledgeId,
      String audio) {
    jdbc.update("INSERT INTO exam_exam_detail (id, tenant_id, type, content, answer, resolve, "
            + "audio, source, knowledge_id, status, check_status, final_check_status, is_private, "
            + "sort_number, user_id, deleted, create_time) "
            + "VALUES (?, ?, ?, ?, ?, '人工解析不发送给 AI', ?, 0, ?, 1, 1, 1, 0, 1, 1, 0, NOW())",
        id, TENANT_ID, type, content, answer, audio, knowledgeId);
  }

  private ObjectiveAnswerDecision decisionFor(
      com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion question) {
    List<ObjectiveAnswerOption> options = question.options();
    if ("multi".equals(question.questionType())) {
      Map<String, ObjectiveAnswerOptionDecision> decisions = new LinkedHashMap<>();
      for (int i = 0; i < options.size(); i++) {
        ObjectiveAnswerOption option = options.get(i);
        boolean yes = i < 2;
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("Y", yes ? 0.92 : 0.08);
        probabilities.put("N", yes ? 0.08 : 0.92);
        decisions.put(option.optionId(), new ObjectiveAnswerOptionDecision(option.optionId(),
            yes ? "Y" : "N", 0.92, probabilities, 0.84, 0.92, 0.08));
      }
      return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_OPTION, null, null, null,
          decisions, 1);
    }
    String choice;
    Map<String, Double> probabilities = new LinkedHashMap<>();
    if ("judge".equals(question.questionType())) {
      choice = "T".equals(options.get(0).label()) ? options.get(0).optionId()
          : options.get(1).optionId();
      probabilities.put(choice, 0.93);
      for (ObjectiveAnswerOption option : options) {
        probabilities.putIfAbsent(option.optionId(), 0.07 / Math.max(1, options.size() - 1));
      }
    } else {
      choice = options.get(Math.min(1, options.size() - 1)).optionId();
      double rest = 0.15 / Math.max(1, options.size() - 1);
      for (ObjectiveAnswerOption option : options) {
        probabilities.put(option.optionId(), option.optionId().equals(choice) ? 0.85 : rest);
      }
    }
    return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_QUESTION, choice, 0.9,
        probabilities, Map.of(), 1);
  }

  private UserDetails admin(String... permissions) {
    return new PlatformUserPrincipal(1L, TENANT_ID, "oac-it", "检查管理员", "admin",
        List.of("PLATFORM_ADMIN"), List.of(permissions), null);
  }

  private UserDetails nonAdmin(String... permissions) {
    return new PlatformUserPrincipal(2L, TENANT_ID, "oac-org", "机构管理员", "org",
        List.of("ORG_ADMIN"), List.of(permissions), null);
  }

  private static final String ALL_PERMISSIONS_JSON = """
      "permissions":["exam:objective-answer-check:view","exam:objective-answer-check:submit",
      "exam:objective-answer-check:review","exam:objective-answer-check:export"]
      """;

  private long createBatch(String body, UserDetails principal) throws Exception {
    var result = mockMvc.perform(post("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(user(principal)))
        .andReturn();
    String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    if (result.getResponse().getStatus() != 200) {
      throw new AssertionError("create failed status=" + result.getResponse().getStatus()
          + " body=" + response);
    }
    long batchId = Long.parseLong(com.jayway.jsonpath.JsonPath.read(response, "$.data.batchId"));
    createdBatches.add(batchId);
    return batchId;
  }

  private String waitDone(long batchId) throws Exception {
    String status = "pending";
    for (int i = 0; i < 100; i++) {
      String body = mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId)
              .header("X-Tenant-Id", TENANT_ID).with(user(admin(
                  "exam:objective-answer-check:view"))))
          .andExpect(status().isOk()).andReturn().getResponse()
          .getContentAsString(StandardCharsets.UTF_8);
      status = com.jayway.jsonpath.JsonPath.read(body, "$.data.status");
      if (!"pending".equals(status) && !"running".equals(status) && !"pausing".equals(status)) {
        return body;
      }
      Thread.sleep(100);
    }
    return mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId)
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("筛选项：只返回有题的工种，等级按实际存在转换")
  void filterOptionsOnlyReturnsWorkTypesWithQuestions() throws Exception {
    mockMvc.perform(get("/exam/objective-answer-check/filter-options?workTypeId=990028901")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.workTypes[?(@.id==990028901)].title").exists())
        .andExpect(jsonPath("$.data.levels[0].code").value(4))
        .andExpect(jsonPath("$.data.sources.length()").value(4))
        .andExpect(jsonPath("$.data.types.length()").value(3));
  }

  @Test
  @DisplayName("试题分页：工种+等级过滤生效，无课程题不进入关联查询且无重复行")
  void questionsPageFiltersByScopeWithoutDuplicates() throws Exception {
    String body = """
        {"scope":{"workTypeId":990028901,"levelCode":4,"types":["single","multi","judge"]},
         "page":1,"pageSize":20}
        """;
    var result = mockMvc.perform(post("/exam/objective-answer-check/questions/page")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(7))
        .andReturn();
    String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    List<Integer> ids = com.jayway.jsonpath.JsonPath.read(response, "$.data.items[*].detailId");
    assertThat(ids).doesNotContain(DETAIL_UNLINKED);
    assertThat(ids).hasSize(7).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("预览：区分可检查/结构异常/内容疑似，预计调用数正确")
  void previewCountsAreIndependent() throws Exception {
    String body = """
        {"selectionMode":"filter",
         "scope":{"workTypeId":990028901,"levelCode":4,"types":["single","multi","judge"]}}
        """;
    mockMvc.perform(post("/exam/objective-answer-check/selection/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.selectionCount").value(7))
        .andExpect(jsonPath("$.data.overLimit").value(false))
        .andExpect(jsonPath("$.data.validCount").value(4))
        .andExpect(jsonPath("$.data.invalidCount").value(3))
        .andExpect(jsonPath("$.data.suspectContentCount").value(1))
        .andExpect(jsonPath("$.data.invalidContentCount").value(1))
        .andExpect(jsonPath("$.data.expectedCalls").value(4));
  }

  @Test
  @DisplayName("主链路：创建→终态→结果→详情→复核→导出，题库答案不变")
  void fullHappyPathKeepsQuestionBankUntouched() throws Exception {
    long batchId = createBatch("""
        {"selectionMode":"ids","detailIds":[990028971,990028972,990028973,990028974,
         990028975,990028976,990028977],
         "scope":{"types":["single","multi","judge"],"source":0},
         "modelId":"attacker-model","confidenceThreshold":0.01,"marginThreshold":0.0,
         "apiKey":"leaked"}
        """, admin("exam:objective-answer-check:view", "exam:objective-answer-check:submit",
        "exam:objective-answer-check:review", "exam:objective-answer-check:export"));

    String batch = waitDone(batchId);
    assertThat((String) com.jayway.jsonpath.JsonPath.read(batch, "$.data.status")).isEqualTo("done");
    assertThat((String) com.jayway.jsonpath.JsonPath.read(batch, "$.data.modelId"))
        .isEqualTo("ateve-jev-v1");
    assertThat(((Number) com.jayway.jsonpath.JsonPath.read(batch,
        "$.data.confidenceThreshold")).doubleValue()).isEqualTo(0.8);
    assertThat((Number) com.jayway.jsonpath.JsonPath.read(batch, "$.data.total")).isEqualTo(7);
    assertThat((Number) com.jayway.jsonpath.JsonPath.read(batch, "$.data.invalid")).isEqualTo(3);

    String items = mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId
            + "/items?page=1&pageSize=50").header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(7))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    List<Map<String, Object>> rows = com.jayway.jsonpath.JsonPath.read(items, "$.data.items");
    Map<Integer, Map<String, Object>> byDetail = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      byDetail.put(((Number) row.get("detailId")).intValue(), row);
      assertThat(row.get("id")).isInstanceOf(String.class);
    }
    assertThat(byDetail.get(DETAIL_SINGLE).get("resultCode")).isEqualTo("consistent_high");
    assertThat(byDetail.get(DETAIL_MULTI).get("resultCode")).isEqualTo("consistent_high");
    assertThat(byDetail.get(DETAIL_JUDGE).get("resultCode")).isEqualTo("consistent_high");
    assertThat(byDetail.get(DETAIL_INVALID).get("resultCode")).isEqualTo("invalid");
    assertThat(byDetail.get(DETAIL_CONTENT_INVALID).get("contentQuality")).isEqualTo("invalid");
    assertThat(byDetail.get(DETAIL_SUSPECT).get("contentQuality")).isEqualTo("suspect");
    assertThat(byDetail.get(DETAIL_SUSPECT).get("needsReview")).isEqualTo(true);

    long suspectItemId = Long.parseLong(String.valueOf(byDetail.get(DETAIL_SUSPECT).get("id")));
    String detail = mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId
            + "/items/" + suspectItemId).header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.resolveSnapshot").value("人工解析不发送给 AI"))
        .andExpect(jsonPath("$.data.calls.length()").value(1))
        .andExpect(jsonPath("$.data.sourceChanged").value(false))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    int version = com.jayway.jsonpath.JsonPath.read(detail, "$.data.item.version");

    mockMvc.perform(post("/exam/objective-answer-check/batches/" + batchId + "/items/"
            + suspectItemId + "/review").header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewStatus\":\"needs_edit\",\"note\":\"选项疑似科学计数法\","
                + "\"expectedVersion\":" + version + "}")
            .with(user(admin("exam:objective-answer-check:review"))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/exam/objective-answer-check/batches/" + batchId + "/items/"
            + suspectItemId + "/review").header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reviewStatus\":\"confirmed_correct\",\"expectedVersion\":" + version + "}")
            .with(user(admin("exam:objective-answer-check:review"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("该记录已被其他人更新，请刷新后重试"));

    var export = mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId + "/export")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:export"))))
        .andExpect(status().isOk()).andReturn();
    byte[] bytes = export.getResponse().getContentAsByteArray();
    assertThat(bytes.length).isGreaterThan(1000);
    assertThat(export.getResponse().getHeader("Content-Disposition")).contains(".xlsx");

    // 复核只写结果表：题库答案、内容、更新时间都不得改变
    Map<String, Object> question = jdbc.queryForMap(
        "SELECT content, answer, update_time FROM exam_exam_detail WHERE id = ?", DETAIL_SUSPECT);
    assertThat(question.get("answer")).isEqualTo(
        "[{\"content\":\"1.2E+3\",\"isCorrect\":true},{\"content\":\"B\",\"isCorrect\":false}]");
  }

  @Test
  @DisplayName("内容确定异常零模型调用，结构异常不计失败")
  void invalidRowsNeverCallModel() throws Exception {
    long batchId = createBatch("""
        {"selectionMode":"ids","detailIds":[990028974,990028975]}
        """, admin("exam:objective-answer-check:submit"));
    waitDone(batchId);

    org.mockito.Mockito.verify(aiService, org.mockito.Mockito.times(0))
        .check(any(), any(), any());
    Map<String, Object> row = jdbc.queryForMap(
        "SELECT retryable, result_code FROM exam_objective_answer_check_item "
            + "WHERE batch_id = ? AND detail_id = ?", batchId, DETAIL_CONTENT_INVALID);
    assertThat(((Number) row.get("retryable")).intValue()).isZero();
    assertThat(row.get("result_code")).isEqualTo("invalid");
  }

  @Test
  @DisplayName("权限：无权限 403；有权限码但非平台管理员 403；跨租户 404")
  void permissionsAndTenantIsolation() throws Exception {
    mockMvc.perform(post("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"selectionMode\":\"filter\",\"scope\":{\"types\":[\"single\"]}}")
            .with(user(nonAdmin("exam:objective-answer-check:view",
                "exam:objective-answer-check:submit"))))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/exam/objective-answer-check/batches/current")
            .header("X-Tenant-Id", TENANT_ID).with(user(nonAdmin(
                "exam:objective-answer-check:view"))))
        .andExpect(status().isForbidden());

    long batchId = createBatch("{\"selectionMode\":\"ids\",\"detailIds\":[990028971]}",
        admin("exam:objective-answer-check:submit"));
    UserDetails otherTenantAdmin = new PlatformUserPrincipal(3L, OTHER_TENANT, "other",
        "其他租户", "admin", List.of("PLATFORM_ADMIN"),
        List.of("exam:objective-answer-check:view"), null);
    mockMvc.perform(get("/exam/objective-answer-check/batches/" + batchId)
            .header("X-Tenant-Id", OTHER_TENANT)
            .with(user(otherTenantAdmin)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("停用配置或凭据缺失时创建返回 503，历史查询不受影响")
  void disabledModelReturns503() throws Exception {
    when(aiService.descriptor())
        .thenReturn(new ObjectiveAnswerCheckAiDescriptor("ateve-jev-v1", false));
    mockMvc.perform(post("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"selectionMode\":\"ids\",\"detailIds\":[990028971]}")
            .with(user(admin("exam:objective-answer-check:submit"))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("答案 AI 检查暂未开通，请联系管理员。"));
    mockMvc.perform(get("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(admin("exam:objective-answer-check:view"))))
        .andExpect(status().isOk());
  }
}
