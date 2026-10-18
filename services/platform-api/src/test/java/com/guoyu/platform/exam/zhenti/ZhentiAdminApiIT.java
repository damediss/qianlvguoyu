package com.guoyu.platform.exam.zhenti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiImportLogEntity;
import com.guoyu.platform.exam.zhenti.service.ZhentiUploadValidator;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class ZhentiAdminApiIT extends ZhentiApiTestSupport {

  private static final String BASE = "/appzhenti/admin";

  @Test
  @DisplayName("D1 列表：分页出参 records、id 为字符串、响应不含 file_key")
  void listReturnsRecordsAndHidesFileKey() throws Exception {
    insertRow(1, "2024", uniqueMd5());
    MvcResult result = mockMvc.perform(post(BASE + "/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20}")
            .with(user(principalWith("exam:zhenti:view", "exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0"))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").isString())
        .andExpect(jsonPath("$.data.records[0].examId").isString())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.years[0]").value("2024"))
        .andReturn();
    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("file_key").doesNotContain("zhenti/" + EXAM_ID);
  }

  @Test
  @DisplayName("D1 筛选：status 与 metaPending 分别生效")
  void listFiltersByStatusAndPending() throws Exception {
    insertRow(1, "2024", uniqueMd5());
    insertRow(0, "2023", uniqueMd5());
    mockMvc.perform(post(BASE + "/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20,\"status\":0}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].year").value("2023"));
  }

  @Test
  @DisplayName("D2 上传成功：默认上架 + 解析标题/年份")
  void uploadSucceeds() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf",
        "application/pdf", pdfBytes("upload"));
    mockMvc.perform(multipart(BASE + "/upload")
            .file(file)
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").isString())
        .andExpect(jsonPath("$.data.year").value("2024"))
        .andExpect(jsonPath("$.data.metaPending").value(0));
  }

  @Test
  @DisplayName("D2 .exe 改 .pdf 名（magic 不符）→ 415")
  void uploadRejectsFakePdf() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "evil.pdf", "application/pdf",
        "MZ\u0000fake".getBytes(StandardCharsets.US_ASCII));
    mockMvc.perform(multipart(BASE + "/upload")
            .file(file)
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("D2 超过 50MB → 413")
  void uploadRejectsOversize() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "big.pdf", "application/pdf", pdfBytes("big")) {
      @Override
      public long getSize() {
        return ZhentiUploadValidator.MAX_PDF_BYTES + 1;
      }
    };
    mockMvc.perform(multipart(BASE + "/upload")
            .file(file)
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isPayloadTooLarge());
  }

  @Test
  @DisplayName("D2 同 exam 同 MD5 → 409 且提示已有标题")
  void uploadDuplicateRejected() throws Exception {
    byte[] content = pdfBytes("duplicate");
    MockMultipartFile file = new MockMultipartFile(
        "file", "重复-2024.pdf", "application/pdf", content);
    mockMvc.perform(multipart(BASE + "/upload")
            .file(file)
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    mockMvc.perform(multipart(BASE + "/upload")
            .file(new MockMultipartFile("file", "重复-2024.pdf", "application/pdf", content))
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("已存在相同文件")));
  }

  @Test
  @DisplayName("D2 软删记录不参与去重 → 删除后可重新上传同一文件")
  void uploadAfterSoftDeleteAllowed() throws Exception {
    byte[] content = pdfBytes("reupload");
    MockMultipartFile file = new MockMultipartFile(
        "file", "重复-2023.pdf", "application/pdf", content);
    MvcResult first = mockMvc.perform(multipart(BASE + "/upload")
            .file(file)
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andReturn();
    long id = Long.parseLong(com.jayway.jsonpath.JsonPath.read(
        first.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id").toString());

    mockMvc.perform(post(BASE + "/delete")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + id + "}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    mockMvc.perform(multipart(BASE + "/upload")
            .file(new MockMultipartFile("file", "重复-2023.pdf", "application/pdf", content))
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("D2 不同考试项目同 MD5 → 允许并存")
  void sameMd5DifferentExamAllowed() throws Exception {
    byte[] content = pdfBytes("cross-exam");
    for (long examId : new long[]{EXAM_ID, EXAM_ID + 1}) {
      mockMvc.perform(multipart(BASE + "/upload")
              .file(new MockMultipartFile("file", "同文件-2024.pdf", "application/pdf", content))
              .param("examId", String.valueOf(examId))
              .header("X-Tenant-Id", TENANT_ID)
              .with(user(principalWith("exam:zhenti:manage"))))
          .andExpect(status().isOk());
    }
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM exam_zhenti WHERE exam_id IN (?, ?) AND deleted = 0",
        Integer.class, EXAM_ID, EXAM_ID + 1);
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("D4 局部更新：只改 status，标题保持不变；清空年份 → 待完善")
  void updatePartialFields() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    mockMvc.perform(post(BASE + "/update")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + ",\"status\":0}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("测试真题-" + entity.getFileMd5()));

    ExamZhentiEntity afterStatus = zhentiMapper.selectById(entity.getId());
    assertThat(afterStatus.getStatus()).isZero();
    assertThat(afterStatus.getTitle()).isEqualTo("测试真题-" + entity.getFileMd5());

    mockMvc.perform(post(BASE + "/update")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + ",\"year\":\"\"}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.metaPending").value(1));

    ExamZhentiEntity afterClearYear = zhentiMapper.selectById(entity.getId());
    assertThat(afterClearYear.getYear()).isNull();
    assertThat(afterClearYear.getMetaPending()).isEqualTo(1);
  }

  @Test
  @DisplayName("D5 删除为软删：D1 不再返回、库内 deleted=1")
  void deleteIsSoftDelete() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    mockMvc.perform(post(BASE + "/delete")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + "}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    assertThat(zhentiMapper.selectById(entity.getId())).isNull();
    Integer deleted = jdbcTemplate.queryForObject(
        "SELECT deleted FROM exam_zhenti WHERE id = ?", Integer.class, entity.getId());
    assertThat(deleted).isEqualTo(1);

    // 审计：操作人、目标 id、动作可查（@AuditLog 同步落 sys_audit_log）
    java.util.Map<String, Object> audit = jdbcTemplate.queryForMap(
        "SELECT user_id, target_id, action, detail_json FROM sys_audit_log "
            + "WHERE target_type = 'exam_zhenti' AND target_id = ? ORDER BY id DESC LIMIT 1",
        entity.getId());
    assertThat(((Number) audit.get("user_id")).longValue()).isEqualTo(1L);
    assertThat(((Number) audit.get("target_id")).longValue()).isEqualTo(entity.getId());
    assertThat(String.valueOf(audit.get("action"))).isEqualTo("zhenti:delete");
    assertThat(String.valueOf(audit.get("detail_json"))).contains(String.valueOf(entity.getId()));
  }

  @Test
  @DisplayName("D6 按 importId 返回进度与逐文件结果")
  void importLogByImportId() throws Exception {
    ExamZhentiImportLogEntity log = new ExamZhentiImportLogEntity();
    log.setTenantId(TENANT_ID);
    log.setExamId(EXAM_ID);
    log.setOperatorId(1L);
    log.setTotal(2);
    log.setFinished(2);
    log.setStatus("done");
    log.setResultsJson("[{\"fileName\":\"a.pdf\",\"ok\":true,\"id\":\"1\",\"title\":\"A\",\"year\":\"2024\"}]");
    importLogMapper.insert(log);

    mockMvc.perform(post(BASE + "/importLog")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"importId\":" + log.getId() + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.importId").value(String.valueOf(log.getId())))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.results[0].fileName").value("a.pdf"));
  }

  @Test
  @DisplayName("D3 >50 个文件转异步：D6 可轮询到完成")
  void batchImportOver50RunsAsync() throws Exception {
    MvcResult submitted = mockMvc.perform(multipart(BASE + "/batchImport")
            .file(new MockMultipartFile(
                "file", "batch.zip", "application/zip", zipOfPdfs(51, "async-")))
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mode").value("async"))
        .andExpect(jsonPath("$.data.importId").isString())
        .andReturn();
    String importId = com.jayway.jsonpath.JsonPath.read(
        submitted.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.importId");

    String status = "running";
    String pollBody = null;
    for (int attempt = 0; attempt < 120 && "running".equals(status); attempt++) {
      MvcResult poll = mockMvc.perform(post(BASE + "/importLog")
              .header("X-Tenant-Id", TENANT_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"importId\":\"" + importId + "\"}")
              .with(user(principalWith("exam:zhenti:view"))))
          .andExpect(status().isOk())
          .andReturn();
      pollBody = poll.getResponse().getContentAsString(StandardCharsets.UTF_8);
      status = com.jayway.jsonpath.JsonPath.read(pollBody, "$.data.status");
      if ("running".equals(status)) {
        Thread.sleep(500);
      }
    }
    assertThat(status).as("异步导入应在超时前完成，最后一次响应=%s", pollBody).isEqualTo("done");
    assertThat((Integer) com.jayway.jsonpath.JsonPath.read(pollBody, "$.data.finished"))
        .isEqualTo(51);
  }

  @Test
  @DisplayName("D6 按 examId 返回历史分页")
  void importLogByExamId() throws Exception {
    ExamZhentiImportLogEntity log = new ExamZhentiImportLogEntity();
    log.setTenantId(TENANT_ID);
    log.setExamId(EXAM_ID);
    log.setTotal(1);
    log.setFinished(1);
    log.setStatus("done");
    log.setResultsJson("[]");
    importLogMapper.insert(log);

    mockMvc.perform(post(BASE + "/importLog")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(1));
  }

  @Test
  @DisplayName("未登录 → 401")
  void anonymousRejected() throws Exception {
    mockMvc.perform(post(BASE + "/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("只有查看权 → 上传 403")
  void viewOnlyCannotUpload() throws Exception {
    mockMvc.perform(multipart(BASE + "/upload")
            .file(new MockMultipartFile("file", "a.pdf", "application/pdf", pdfBytes("x")))
            .param("examId", String.valueOf(EXAM_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("D7 管理端预览返回带 sign/t 的 viewerUrl")
  void previewReturnsSignedViewerUrl() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    MvcResult result = mockMvc.perform(post(BASE + "/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + "}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.viewerUrl").isString())
        .andReturn();
    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    String viewerUrl = com.jayway.jsonpath.JsonPath.read(body, "$.data.viewerUrl");
    String decoded = java.net.URLDecoder.decode(viewerUrl, StandardCharsets.UTF_8);
    assertThat(decoded)
        .startsWith("https://viewer.test.local/pdf-viewer/viewer.html?file=")
        .contains("/appzhenti/file/" + entity.getId())
        .contains("sign=")
        .contains("&t=");
  }
}
