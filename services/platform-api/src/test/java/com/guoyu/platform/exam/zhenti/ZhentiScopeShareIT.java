package com.guoyu.platform.exam.zhenti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 真题共享组端到端（方案 v2.1 §14.2）：同 scope 跨计划共享、不同 scope 隔离、
 * 无绑定/失效工种降级、scope_key 去重、多绑定上传必须选 scope、scopeInfo/scopePlans/scopeBatchSave。
 *
 * <p>fixture：A/B 同 scope（工种 901/等级 1），C 同工种等级 0，D 无绑定，E 绑定失效工种，F 双绑定。
 */
class ZhentiScopeShareIT extends ZhentiApiTestSupport {

  private static final String BASE = "/appzhenti/admin";

  @BeforeEach
  void insertScopeFixture() {
    insertScopeWorkType(SCOPE_WORK_TYPE, "IT合成工种甲");
    insertScopeWorkType(SCOPE_WORK_TYPE_2, "IT合成工种乙");
    insertScopeCourse(SCOPE_COURSE, SCOPE_WORK_TYPE, 1, "IT合成课程-中级");
    insertScopeExam(SCOPE_EXAM_A, "甲四级");
    insertScopeExam(SCOPE_EXAM_B, "甲四级-另一计划");
    insertScopeExam(SCOPE_EXAM_C, "甲五级");
    insertScopeExam(SCOPE_EXAM_D, "无绑定");
    insertScopeExam(SCOPE_EXAM_E, "失效工种");
    insertScopeExam(SCOPE_EXAM_F, "双绑定");
    insertScopeBinding(SCOPE_EXAM_A, SCOPE_WORK_TYPE, 1, SCOPE_COURSE);
    insertScopeBinding(SCOPE_EXAM_B, SCOPE_WORK_TYPE, 1, SCOPE_COURSE);
    insertScopeBinding(SCOPE_EXAM_C, SCOPE_WORK_TYPE, 0, SCOPE_COURSE);
    insertScopeBinding(SCOPE_EXAM_E, SCOPE_MISSING_WORK_TYPE, 1, null);
    insertScopeBinding(SCOPE_EXAM_F, SCOPE_WORK_TYPE, 1, SCOPE_COURSE);
    // 测试库 level 列为 tinyint(1)（Boolean 化，踩坑 #23），多绑定用 0/1 两个可表达等级验证机制
    insertScopeBinding(SCOPE_EXAM_F, SCOPE_WORK_TYPE_2, 0, SCOPE_COURSE);
  }

  private MockMultipartFile pdf(String name) {
    return new MockMultipartFile(
        "file", name, "application/pdf", pdfBytes("scope-" + System.nanoTime()));
  }

  private void upload(long examId, String filename) throws Exception {
    upload(examId, filename, null, null);
  }

  private void upload(long examId, String filename, Long scopeWorkTypeId, Integer scopeLevel)
      throws Exception {
    var request = multipart(BASE + "/upload")
        .file(pdf(filename))
        .param("examId", String.valueOf(examId))
        .header("X-Tenant-Id", TENANT_ID)
        .with(user(principalWith("exam:zhenti:manage")));
    if (scopeWorkTypeId != null) {
      request.param("scopeWorkTypeId", String.valueOf(scopeWorkTypeId));
    }
    if (scopeLevel != null) {
      request.param("scopeLevel", String.valueOf(scopeLevel));
    }
    mockMvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"));
  }

  private int appListSize(long examId) throws Exception {
    String body = mockMvc.perform(post("/appzhenti/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + examId + "}")
            .with(user(principalWith())))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(body, "$.data.records.length()");
  }

  private int adminListSize(long examId) throws Exception {
    String body = mockMvc.perform(post(BASE + "/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + examId + ",\"page\":1,\"size\":50}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(body, "$.data.records.length()");
  }

  @Test
  @DisplayName("A 上传 → B（同 scope）管理端与学员端均可见，C（不同等级）不可见")
  void crossExamShare() throws Exception {
    upload(SCOPE_EXAM_A, "共享真题-2024.pdf");

    String adminB = mockMvc.perform(post(BASE + "/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + SCOPE_EXAM_B + ",\"page\":1,\"size\":50}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].scopeLabel").value("IT合成工种甲 · 四级/中级工"))
        .andReturn().getResponse().getContentAsString();
    assertThat(adminB).contains("共享真题");

    assertThat(appListSize(SCOPE_EXAM_B)).isEqualTo(1);
    assertThat(appListSize(SCOPE_EXAM_C)).isZero();
    assertThat(appListSize(SCOPE_EXAM_A)).isEqualTo(1);
  }

  @Test
  @DisplayName("无绑定计划 D：上传走私有口径，仅自己可见")
  void unboundExamFallsBackToPrivate() throws Exception {
    upload(SCOPE_EXAM_D, "私有真题-2024.pdf");
    assertThat(appListSize(SCOPE_EXAM_D)).isEqualTo(1);
    assertThat(appListSize(SCOPE_EXAM_A)).isZero();

    mockMvc.perform(post(BASE + "/scopeInfo")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + SCOPE_EXAM_D + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.unscoped").value(true))
        .andExpect(jsonPath("$.data.scopes.length()").value(0));
  }

  @Test
  @DisplayName("失效工种计划 E：降级私有，真题不消失")
  void invalidWorkTypeFallsBack() throws Exception {
    upload(SCOPE_EXAM_E, "失效计划真题-2024.pdf");
    assertThat(appListSize(SCOPE_EXAM_E)).isEqualTo(1);
    assertThat(appListSize(SCOPE_EXAM_A)).isZero();
  }

  @Test
  @DisplayName("去重按共享组：同 scope 同文件 409；不同 scope 同文件允许")
  void dedupByScopeKey() throws Exception {
    byte[] content = pdfBytes("dedup-" + System.nanoTime());
    MockMultipartFile fileA = new MockMultipartFile(
        "file", "重复文件-2024.pdf", "application/pdf", content);

    mockMvc.perform(multipart(BASE + "/upload")
            .file(fileA)
            .param("examId", String.valueOf(SCOPE_EXAM_A))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    MockMultipartFile fileA2 = new MockMultipartFile(
        "file", "重复文件-2024.pdf", "application/pdf", content);
    mockMvc.perform(multipart(BASE + "/upload")
            .file(fileA2)
            .param("examId", String.valueOf(SCOPE_EXAM_B))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.containsString("同工种同等级下已存在相同文件")));

    MockMultipartFile fileC = new MockMultipartFile(
        "file", "重复文件-2024.pdf", "application/pdf", content);
    mockMvc.perform(multipart(BASE + "/upload")
            .file(fileC)
            .param("examId", String.valueOf(SCOPE_EXAM_C))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("未归类旧真题（scope_key 空）只在所属计划可见，不跨 scope 泄漏")
  void legacyUnscopedStaysOnOwnExam() throws Exception {
    String md5 = uniqueMd5();
    jdbcTemplate.update(
        "INSERT INTO exam_zhenti (id, tenant_id, exam_id, title, year, file_key, file_size,"
            + " file_md5, sort, status, meta_pending, created_at, updated_at, deleted, scope_key)"
            + " VALUES (?, ?, ?, '旧真题-2023', '2023', ?, 11, ?, 0, 1, 0, NOW(), NOW(), 0, '')",
        System.currentTimeMillis(), TENANT_ID, SCOPE_EXAM_A, "zhenti/legacy/" + md5 + ".pdf", md5);

    assertThat(appListSize(SCOPE_EXAM_A)).isEqualTo(1);
    assertThat(appListSize(SCOPE_EXAM_B)).isZero();
  }

  @Test
  @DisplayName("多绑定计划上传必须选 scope；foreign scope 400；合法 scope 成功")
  void multiScopeUploadRequiresSelection() throws Exception {
    MockMultipartFile file = pdf("多绑定-2024.pdf");
    mockMvc.perform(multipart(BASE + "/upload")
            .file(pdf("多绑定-2024.pdf"))
            .param("examId", String.valueOf(SCOPE_EXAM_F))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.containsString("绑定了多个工种/等级")));

    mockMvc.perform(multipart(BASE + "/upload")
            .file(pdf("多绑定-2024.pdf"))
            .param("examId", String.valueOf(SCOPE_EXAM_F))
            .param("scopeWorkTypeId", "35")
            .param("scopeLevel", "1")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.containsString("不一致")));

    upload(SCOPE_EXAM_F, "多绑定-选定-2024.pdf", SCOPE_WORK_TYPE_2, 0);
    assertThat(appListSize(SCOPE_EXAM_F)).isEqualTo(1);
  }

  @Test
  @DisplayName("scopeInfo：A 的共享组含组内计划数 2；F 返回两个 scope")
  void scopeInfoReturnsLabelsAndCounts() throws Exception {
    mockMvc.perform(post(BASE + "/scopeInfo")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + SCOPE_EXAM_A + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.unscoped").value(false))
        .andExpect(jsonPath("$.data.scopes.length()").value(1))
        .andExpect(jsonPath("$.data.scopes[0].label").value("IT合成工种甲 · 四级/中级工"))
        .andExpect(jsonPath("$.data.scopes[0].planCount").value(3)); // A、B、F 同组

    mockMvc.perform(post(BASE + "/scopeInfo")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + SCOPE_EXAM_F + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.scopes.length()").value(2));
  }

  @Test
  @DisplayName("scopePlans 标出未绑定/多绑定/失效；scopeBatchSave 改绑课程后 scopeInfo 生效")
  void scopePlansAndBatchSave() throws Exception {
    String body = mockMvc.perform(post(BASE + "/scopePlans")
            .param("page", "1")
            .param("pageSize", "200")
            .param("filter", "all")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.length()").value(
            org.hamcrest.Matchers.greaterThan(0)))
        .andReturn().getResponse().getContentAsString();
    assertThat(body).contains("真题IT-无绑定");
    assertThat(body).contains("真题IT-双绑定");
    assertThat(body).contains("真题IT-失效工种");

    // 失效工种筛选：测试库存在历史脏数据（10 条），断言包含本 fixture 的 E 即可
    String invalidBody = mockMvc.perform(post(BASE + "/scopePlans")
            .param("page", "1")
            .param("pageSize", "200")
            .param("filter", "invalid")
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(
            org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
        .andReturn().getResponse().getContentAsString();
    assertThat(invalidBody).contains("真题IT-失效工种");

    // D 改绑课程 → 获得有效共享组
    mockMvc.perform(post(BASE + "/scopeBatchSave")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"items\":[{\"examId\":" + SCOPE_EXAM_D
                + ",\"courseIdList\":[" + SCOPE_COURSE + "]}]}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    mockMvc.perform(post(BASE + "/scopeInfo")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + SCOPE_EXAM_D + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.unscoped").value(false))
        .andExpect(jsonPath("$.data.scopes[0].label").value("IT合成工种甲 · 四级/中级工"));
  }
}
