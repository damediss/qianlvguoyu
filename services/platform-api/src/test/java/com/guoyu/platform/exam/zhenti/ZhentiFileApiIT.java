package com.guoyu.platform.exam.zhenti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class ZhentiFileApiIT extends ZhentiApiTestSupport {

  private byte[] content;
  private ExamZhentiEntity entity;

  @BeforeEach
  void createFile() {
    // 内容 >100 字节，保证 Range: bytes=0-99 是完整可满足区间
    content = pdfBytes("file-api-" + System.nanoTime() + "-" + "x".repeat(200));
    entity = ingestService.ingest(
        TENANT_ID, EXAM_ID, 1L, "铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf",
        content, null, null, null);
  }

  private MvcResult fetch(String sign, Long expiresAt, String range) throws Exception {
    var request = get("/api/appzhenti/file/" + entity.getId()).contextPath("/api");
    if (sign != null) {
      request.param("sign", sign);
    }
    if (expiresAt != null) {
      request.param("t", String.valueOf(expiresAt));
    }
    if (range != null) {
      request.header("Range", range);
    }
    return mockMvc.perform(request).andReturn();
  }

  @Test
  @DisplayName("正常签名 → 200 + inline + private,no-store")
  void servesInlineNoStore() throws Exception {
    var signed = signatureService.sign(entity.getId());
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition",
            org.hamcrest.Matchers.startsWith("inline;")))
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andExpect(header().string("Content-Length", String.valueOf(content.length)));
  }

  @Test
  @DisplayName("Range: bytes=0-99 → 206 + Content-Range/Content-Length=100")
  void rangeReturnsPartialContent() throws Exception {
    var signed = signatureService.sign(entity.getId());
    MvcResult result = mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt()))
            .header("Range", "bytes=0-99"))
        .andExpect(status().isPartialContent())
        .andExpect(header().string("Content-Range",
            "bytes 0-99/" + content.length))
        .andExpect(header().string("Content-Length", "100"))
        .andReturn();
    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(body).hasSize(100);
    assertThat(body).isEqualTo(java.util.Arrays.copyOfRange(content, 0, 100));
  }

  @Test
  @DisplayName("越界 Range → 416 + Content-Range: bytes */size")
  void unsatisfiableRange() throws Exception {
    var signed = signatureService.sign(entity.getId());
    long tooLarge = content.length + 1000L;
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt()))
            .header("Range", "bytes=" + tooLarge + "-" + (tooLarge + 10)))
        .andExpect(status().isRequestedRangeNotSatisfiable())
        .andExpect(header().string("Content-Range", "bytes */" + content.length));
  }

  @Test
  @DisplayName("无签名 → 403")
  void missingSignRejected() throws Exception {
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("错签名 → 403")
  void wrongSignRejected() throws Exception {
    var signed = signatureService.sign(entity.getId());
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", "deadbeef")
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("过期签名 → 403")
  void expiredSignRejected() throws Exception {
    var expired = signatureService.signAt(entity.getId(), Instant.now().getEpochSecond() - 1);
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", expired.sign())
            .param("t", String.valueOf(expired.expiresAt())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("A 文件签名访问 B 文件 → 403")
  void signatureBoundToId() throws Exception {
    ExamZhentiEntity other = ingestService.ingest(
        TENANT_ID, EXAM_ID, 1L, "另一份-2023.pdf",
        pdfBytes("other-" + System.nanoTime()), null, null, null);
    var signed = signatureService.sign(entity.getId());
    mockMvc.perform(get("/api/appzhenti/file/" + other.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("deleted=1 → 404")
  void deletedReturns404() throws Exception {
    var signed = signatureService.sign(entity.getId());
    zhentiMapper.deleteById(entity.getId());
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("下架（status=0）→ 签名有效期内仍可访问")
  void offlineStillAccessibleUntilExpiry() throws Exception {
    var signed = signatureService.sign(entity.getId());
    entity.setStatus(0);
    zhentiMapper.updateById(entity);
    mockMvc.perform(get("/api/appzhenti/file/" + entity.getId()).contextPath("/api")
            .param("sign", signed.sign())
            .param("t", String.valueOf(signed.expiresAt())))
        .andExpect(status().isOk());
  }
}
