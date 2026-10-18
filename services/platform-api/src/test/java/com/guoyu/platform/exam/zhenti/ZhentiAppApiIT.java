package com.guoyu.platform.exam.zhenti;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ZhentiAppApiIT extends ZhentiApiTestSupport {

  @Test
  @DisplayName("A1 只返回上架，按年份倒序")
  void listOnlyPublishedSortedByYearDesc() throws Exception {
    insertRow(1, "2023", uniqueMd5());
    insertRow(1, "2024", uniqueMd5());
    insertRow(0, "2025", uniqueMd5());

    mockMvc.perform(post("/appzhenti/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + "}")
            .with(user(principalWith())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].year").value("2024"))
        .andExpect(jsonPath("$.data.records[1].year").value("2023"))
        .andExpect(jsonPath("$.data.records[0].id").isString());
  }

  @Test
  @DisplayName("A1 他租户数据不可见（examId 命中但租户不同 → 空）")
  void listIsTenantScoped() throws Exception {
    ExamZhentiEntity otherTenant = new ExamZhentiEntity();
    otherTenant.setTenantId(10160L);
    otherTenant.setExamId(EXAM_ID);
    otherTenant.setTitle("他租户真题");
    otherTenant.setYear("2024");
    otherTenant.setFileKey("zhenti/other/x.pdf");
    otherTenant.setFileSize(10L);
    otherTenant.setFileMd5(uniqueMd5());
    otherTenant.setSort(0);
    otherTenant.setStatus(1);
    otherTenant.setMetaPending(0);
    zhentiMapper.insert(otherTenant);

    mockMvc.perform(post("/appzhenti/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + "}")
            .with(user(principalWith())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(0));
  }

  @Test
  @DisplayName("A2 返回 viewerUrl（含 sign/t 与配置的 viewerOrigin）")
  void previewReturnsSignedViewerUrl() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    MvcResult result = mockMvc.perform(post("/appzhenti/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + "}")
            .with(user(principalWith())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value(entity.getTitle()))
        .andReturn();
    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    String viewerUrl = com.jayway.jsonpath.JsonPath.read(body, "$.data.viewerUrl");
    String decoded = java.net.URLDecoder.decode(viewerUrl, StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThat(decoded)
        .startsWith("https://viewer.test.local/pdf-viewer/viewer.html?file=")
        .contains("/appzhenti/file/" + entity.getId())
        .contains("sign=")
        .contains("&t=");
  }

  @Test
  @DisplayName("A2 下架真题 → 404")
  void previewOfflineRejected() throws Exception {
    ExamZhentiEntity entity = insertRow(0, "2024", uniqueMd5());
    mockMvc.perform(post("/appzhenti/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + "}")
            .with(user(principalWith())))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("A1/A2 未带 itoken → 401")
  void anonymousRejected() throws Exception {
    mockMvc.perform(post("/appzhenti/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + "}"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/appzhenti/preview")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1}"))
        .andExpect(status().isUnauthorized());
  }
}
