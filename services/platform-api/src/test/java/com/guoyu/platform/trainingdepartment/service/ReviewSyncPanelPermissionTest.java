package com.guoyu.platform.trainingdepartment.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

/**
 * 同步练习统计面板端点权限语义（方案 §7 / §9）：
 * 匿名 401；已登录但无 train:dashboard:view → 403；有该码 → 200 且一次拿全六个分区。
 *
 * <p>跑在 courseware-e2e profile（真实启动 + 测试库），只读聚合不产生写入。
 * TenantContextFilter 要求非白名单路径带 X-Tenant-Id。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
class ReviewSyncPanelPermissionTest {

  @Autowired
  private MockMvc mockMvc;

  private static UserDetails principalWith(String... permissionCodes) {
    return new PlatformUserPrincipal(
        1L, 10159L, "panel-test", "面板测试", "admin", List.of(), List.of(permissionCodes), null);
  }

  @Test
  @DisplayName("★ 匿名访问 → 401")
  void anonymousRejected() throws Exception {
    mockMvc.perform(get("/training/dashboard/review-sync").header("X-Tenant-Id", "10159"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("★ 已登录但无 train:dashboard:view → 403（@PreAuthorize 生效）")
  void noPermissionForbidden() throws Exception {
    mockMvc.perform(get("/training/dashboard/review-sync")
            .header("X-Tenant-Id", "10159")
            .with(user(principalWith("some:other:perm"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("★ 有 train:dashboard:view → 200，VO 六个分区齐备")
  void withPermissionOk() throws Exception {
    mockMvc.perform(get("/training/dashboard/review-sync")
            .header("X-Tenant-Id", "10159")
            .with(user(principalWith("train:dashboard:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.scale").exists())
        .andExpect(jsonPath("$.data.effort").exists())
        .andExpect(jsonPath("$.data.effect").exists())
        .andExpect(jsonPath("$.data.trend30d").isArray())
        .andExpect(jsonPath("$.data.wrongTrend30d").isArray())
        .andExpect(jsonPath("$.data.weakTop5").isArray());
  }
}
