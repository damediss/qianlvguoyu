package com.guoyu.platform.exam.answercheck;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOption;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 单批上限与服务端硬限制（方案 §7.3 / §16.1-G）：试点 2 道时绝不允许创建 3 道。
 * 预览超限返回 200+overLimit，创建超限才 413。
 */
@SpringBootTest(properties = "platform.objectiveanswercheck.batch-max-size=2")
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
class ObjectiveAnswerCheckLimitIT {

  private static final long TENANT_ID = 10159L;
  private static final int WORK_TYPE_ID = 990_028_911;
  private static final int COURSE_ID = 990_028_952;
  private static final int KNOWLEDGE_ID = 990_028_964;
  private static final List<Integer> DETAILS = List.of(990_028_981, 990_028_982, 990_028_983);
  private static final String SINGLE_ANSWER = """
      [{"content":"A选项","isCorrect":true},{"content":"B选项","isCorrect":false}]
      """;

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private JdbcTemplate jdbc;
  @MockBean
  private ObjectiveAnswerCheckAiService aiService;

  @BeforeEach
  void setUp() {
    clean();
    org.mockito.Mockito.when(aiService.descriptor())
        .thenReturn(new ObjectiveAnswerCheckAiDescriptor("OBJECTIVE_ANSWER_CHECK_JEV_MODEL",
            "ateve-jev-v1", "https://tokendance.space/gateway", true, 5, 30));
    org.mockito.Mockito.when(aiService.check(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion question = invocation.getArgument(0);
          List<ObjectiveAnswerOption> options = question.options();
          Map<String, Double> probabilities = new LinkedHashMap<>();
          probabilities.put(options.get(0).optionId(), 0.9);
          probabilities.put(options.get(1).optionId(), 0.1);
          return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_QUESTION,
              options.get(0).optionId(), 0.9, probabilities, Map.of(), 1);
        });

    jdbc.update("INSERT INTO exam_work_type (id, tenant_id, title, parent_id, deleted, status) "
        + "VALUES (?, ?, 'IT上限工种', 0, 0, 1)", WORK_TYPE_ID, TENANT_ID);
    jdbc.update("INSERT INTO exam_course (id, tenant_id, work_type_id, title, level, deleted) "
        + "VALUES (?, ?, ?, 'IT上限课程', 1, 0)", COURSE_ID, TENANT_ID, WORK_TYPE_ID);
    jdbc.update("INSERT INTO exam_knowledge_structure (id, tenant_id, course_id, parent_id, title, "
        + "type, level, status, deleted) VALUES (?, ?, ?, 0, '上限知识点', 1, 1, 1, 0)",
        KNOWLEDGE_ID, TENANT_ID, COURSE_ID);
    for (int id : DETAILS) {
      jdbc.update("INSERT INTO exam_exam_detail (id, tenant_id, type, content, answer, source, "
              + "knowledge_id, status, check_status, final_check_status, is_private, sort_number, "
              + "user_id, deleted, create_time) VALUES (?, ?, 'single', ?, ?, 0, ?, 1, 1, 1, 0, 1, "
              + "1, 0, NOW())",
          id, TENANT_ID, "<p>IT上限题" + id + "</p>", SINGLE_ANSWER, KNOWLEDGE_ID);
    }
  }

  @AfterEach
  void tearDown() {
    clean();
  }

  private void clean() {
    List<Long> batchIds = jdbc.queryForList(
        "SELECT DISTINCT batch_id FROM exam_objective_answer_check_item "
            + "WHERE detail_id IN (?, ?, ?)", Long.class,
        DETAILS.get(0), DETAILS.get(1), DETAILS.get(2));
    for (Long batchId : batchIds) {
      jdbc.update("DELETE FROM exam_objective_answer_check_call_log WHERE batch_id = ?", batchId);
    }
    jdbc.update("DELETE FROM exam_objective_answer_check_item WHERE detail_id IN (?, ?, ?)",
        DETAILS.get(0), DETAILS.get(1), DETAILS.get(2));
    for (Long batchId : batchIds) {
      jdbc.update("DELETE FROM exam_objective_answer_check_batch WHERE id = ?", batchId);
      jdbc.update("DELETE FROM sys_audit_log WHERE target_id = ?", String.valueOf(batchId));
    }
    jdbc.update("DELETE FROM exam_exam_detail WHERE id IN (?, ?, ?)",
        DETAILS.get(0), DETAILS.get(1), DETAILS.get(2));
    jdbc.update("DELETE FROM exam_knowledge_structure WHERE id = ?", KNOWLEDGE_ID);
    jdbc.update("DELETE FROM exam_course WHERE id = ?", COURSE_ID);
    jdbc.update("DELETE FROM exam_work_type WHERE id = ?", WORK_TYPE_ID);
  }

  private UserDetails admin() {
    return new PlatformUserPrincipal(1L, TENANT_ID, "limit-it", "上限测试", "admin",
        List.of("PLATFORM_ADMIN"), List.of("exam:objective-answer-check:view",
        "exam:objective-answer-check:submit"), null);
  }

  @Test
  @DisplayName("预览超限返回 overLimit，不抛异常")
  void previewOverLimitReturnsFlag() throws Exception {
    mockMvc.perform(post("/exam/objective-answer-check/selection/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"selectionMode":"filter","scope":{"workTypeId":990028911,
                 "types":["single"]}}
                """)
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.overLimit").value(true))
        .andExpect(jsonPath("$.data.selectionCount").value(3))
        .andExpect(jsonPath("$.data.maxBatchSize").value(2))
        .andExpect(jsonPath("$.data.validCount").doesNotExist());
  }

  @Test
  @DisplayName("创建超限返回 413；等于上限可创建")
  void createOverLimitReturns413() throws Exception {
    mockMvc.perform(post("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"selectionMode":"ids","detailIds":[990028981,990028982,990028983]}
                """)
            .with(user(admin())))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("最多检查 2 道")));

    String created = mockMvc.perform(post("/exam/objective-answer-check/batches")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"selectionMode":"ids","detailIds":[990028981,990028982]}
                """)
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.batchId").isString())
        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    long batchId = Long.parseLong(com.jayway.jsonpath.JsonPath.read(created, "$.data.batchId"));
    for (int i = 0; i < 100; i++) {
      String status = jdbc.queryForObject(
          "SELECT status FROM exam_objective_answer_check_batch WHERE id = ?", String.class,
          batchId);
      if ("done".equals(status) || "failed".equals(status) || "paused".equals(status)) {
        break;
      }
      Thread.sleep(50);
    }
  }
}
