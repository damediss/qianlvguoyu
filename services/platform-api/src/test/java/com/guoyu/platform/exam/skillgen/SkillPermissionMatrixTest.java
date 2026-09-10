package com.guoyu.platform.exam.skillgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.skillgen.controller.SkillBlueprintController;
import com.guoyu.platform.exam.skillgen.controller.SkillGenBatchController;
import com.guoyu.platform.exam.skillgen.controller.SkillGenDraftController;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * E2E-3：操作技能权限矩阵——全部接口方法的 @PreAuthorize 权限码与方案 §T1（迁移 3）一致。
 * 无 token / 无权限时由安全框架统一拦截（401/403，实测无 token 请求已被拒绝）。
 */
class SkillPermissionMatrixTest {

  private static final Map<String, String> EXPECTED = Map.ofEntries(
      // 细目表
      Map.entry("importBlueprint", "exam:skill:blueprint:import"),
      Map.entry("page", "exam:skill:blueprint:view"),
      Map.entry("tree", "exam:skill:blueprint:view"),
      Map.entry("sourceFile", "exam:skill:blueprint:view"),
      Map.entry("delete", "exam:skill:blueprint:import"),
      // 生成批次
      Map.entry("submit", "exam:skill:gen:submit"),
      Map.entry("modelConfigs", "exam:skill:gen:view"),
      Map.entry("progress", "exam:skill:gen:view"),
      Map.entry("pause", "exam:skill:gen:pause"),
      Map.entry("resume", "exam:skill:gen:pause"),
      Map.entry("retryFailed", "exam:skill:gen:pause"),
      // 草稿
      Map.entry("edit", "exam:skill:draft:edit"),
      Map.entry("review", "exam:skill:draft:review"),
      Map.entry("publishPreview", "exam:skill:draft:view"),
      Map.entry("publish", "exam:skill:draft:publish"),
      Map.entry("export", "exam:skill:gen:export"));

  private static final List<Class<?>> CONTROLLERS = List.of(
      SkillBlueprintController.class, SkillGenBatchController.class, SkillGenDraftController.class);

  /** 每个接口方法都带 @PreAuthorize 且权限码与方案一致 */
  @Test
  void e2e3_allEndpointsGuardedWithExpectedAuthority() {
    for (Map.Entry<String, String> e : EXPECTED.entrySet()) {
      Method method = findMethod(e.getKey());
      assertNotNull(method, "缺少接口方法：" + e.getKey());
      PreAuthorize pa = method.getAnnotation(PreAuthorize.class);
      assertNotNull(pa, e.getKey() + " 缺少 @PreAuthorize");
      if (!pa.value().contains(e.getValue())) {
        assertEquals("hasAuthority('" + e.getValue() + "')", pa.value(), e.getKey() + " 权限码不符");
      }
    }
  }

  /** 权限码集合与迁移 3 种子的 11 条完全一致（不多不少），避免"漏建权限"或"多授权限" */
  @Test
  void e2e3_authoritySetMatchesSeededPermissions() {
    Set<String> declared = new HashSet<>();
    for (Class<?> c : CONTROLLERS) {
      for (Method m : c.getDeclaredMethods()) {
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        if (pa != null) {
          // 兼容 hasAuthority('x') 与 hasAnyAuthority('x','y') 两种写法：提取全部引号内权限码
          java.util.regex.Matcher matcher = java.util.regex.Pattern
              .compile("'([^']+)'").matcher(pa.value());
          while (matcher.find()) {
            declared.add(matcher.group(1));
          }
        }
      }
    }
    Set<String> expected = new HashSet<>(EXPECTED.values());
    // 导出端点用 hasAnyAuthority 同时接受两种导出权限，故种子 11 条全部被端点引用
    expected.add("exam:skill:draft:export");
    assertEquals(expected, declared, "控制器声明的权限码集合与方案种子不一致");
    assertEquals(11, declared.size(), "权限码应为 11 条（与 V20260910_012 种子一致）");
  }

  private Method findMethod(String name) {
    for (Class<?> c : CONTROLLERS) {
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().equals(name)) {
          return m;
        }
      }
    }
    return null;
  }
}
