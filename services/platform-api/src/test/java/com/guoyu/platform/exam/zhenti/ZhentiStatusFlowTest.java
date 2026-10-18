package com.guoyu.platform.exam.zhenti;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** 状态机：上架 → 下架 → 再上架 → 软删；A1/D1/F1 三处语义一致（方案 §2.10）。 */
class ZhentiStatusFlowTest extends ZhentiApiTestSupport {

  private void updateStatus(long id, int status) throws Exception {
    mockMvc.perform(post("/appzhenti/admin/update")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + id + ",\"status\":" + status + "}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());
  }

  private int appCount() throws Exception {
    String body = mockMvc.perform(post("/appzhenti/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + "}")
            .with(user(principalWith())))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    return com.jayway.jsonpath.JsonPath.read(body, "$.data.records.length()");
  }

  @Test
  @DisplayName("下架 → A1 不返回、D1 仍可见；再上架恢复")
  void offlineThenOnline() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    org.assertj.core.api.Assertions.assertThat(appCount()).isEqualTo(1);

    updateStatus(entity.getId(), 0);
    org.assertj.core.api.Assertions.assertThat(appCount()).isZero();

    mockMvc.perform(post("/appzhenti/admin/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(1));

    updateStatus(entity.getId(), 1);
    org.assertj.core.api.Assertions.assertThat(appCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("删除 → A1/D1 不含、F1 立即 404")
  void deleteHidesEverywhere() throws Exception {
    ExamZhentiEntity entity = insertRow(1, "2024", uniqueMd5());
    var signed = signatureService.sign(entity.getId());

    mockMvc.perform(post("/appzhenti/admin/delete")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + entity.getId() + "}")
            .with(user(principalWith("exam:zhenti:manage"))))
        .andExpect(status().isOk());

    org.assertj.core.api.Assertions.assertThat(appCount()).isZero();

    mockMvc.perform(post("/appzhenti/admin/list")
            .header("X-Tenant-Id", TENANT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\":" + EXAM_ID + ",\"page\":1,\"size\":20}")
            .with(user(principalWith("exam:zhenti:view"))))
        .andExpect(jsonPath("$.data.records.length()").value(0));

    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isNotFound());
  }
}
