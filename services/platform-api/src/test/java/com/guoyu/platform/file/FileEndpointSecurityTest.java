package com.guoyu.platform.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 修订方案 §2.2（P0-5）：/files/** 接口收紧后的安全语义（真实 SecurityFilterChain）。
 *
 * <p>跑在 courseware-e2e profile（真实启动 + 测试库），全部用例不产生写入：
 * 匿名请求在授权层被拒（401）；有权限码的 DELETE 打到不存在的 id，在业务层 NOT_FOUND（非 403），
 * 以此证明 @PreAuthorize 通过且不落任何数据。
 *
 * <p>注意：TenantContextFilter 要求非白名单路径必须带 X-Tenant-Id（否则 400），
 * 因此所有请求统一带 10159。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
class FileEndpointSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  private static UserDetails principalWith(String... permissionCodes) {
    return new PlatformUserPrincipal(
        1L, 10159L, "sec-test", "安全测试", "admin", List.of(), List.of(permissionCodes), null);
  }

  @Test
  @DisplayName("★ 匿名上传 → 401（此前 permitAll 任何人可上传）")
  void anonymousUploadRejected() throws Exception {
    mockMvc.perform(post("/files")
            .header("X-Tenant-Id", "10159")
            .contentType("application/octet-stream")
            .content(new byte[] {1, 2, 3}))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("匿名下载 → 401")
  void anonymousDownloadRejected() throws Exception {
    mockMvc.perform(get("/files/999999/download").header("X-Tenant-Id", "10159"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("匿名删除 → 401")
  void anonymousDeleteRejected() throws Exception {
    mockMvc.perform(delete("/files/999999").header("X-Tenant-Id", "10159"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("★ 已登录但无 file:delete 权限码 → 403（@PreAuthorize 生效）")
  void deleteWithoutPermissionForbidden() throws Exception {
    mockMvc.perform(delete("/files/999999")
            .header("X-Tenant-Id", "10159")
            .with(user(principalWith("course:view"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("★ 有 file:delete 权限码 → 通过 @PreAuthorize，业务层 NOT_FOUND（不落数据）")
  void deleteWithPermissionReachesService() throws Exception {
    MvcResult result = mockMvc.perform(delete("/files/999999")
            .header("X-Tenant-Id", "10159")
            .with(user(principalWith("file:delete"))))
        .andReturn();
    int status = result.getResponse().getStatus();
    assertThat(status).as("应到达业务层而非被授权层拦截，实际 status=%s", status)
        .isNotIn(401, 403);
  }

  @Test
  @DisplayName("★ /exam/files/** 保持匿名放行（证件照 <img> 直链依赖，未被 P0-5 误伤）")
  void examFilesRemainAnonymous() throws Exception {
    MvcResult result = mockMvc.perform(get("/exam/files/sec-test.pdf"))
        .andReturn();
    int status = result.getResponse().getStatus();
    assertThat(status).as("匿名直链不应被授权层拦截，实际 status=%s", status)
        .isNotIn(401, 403);
  }
}
