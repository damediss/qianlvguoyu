package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.controller.StandardParseController;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * C5：权限矩阵——每个接口方法的 @PreAuthorize 权限码与方案 §11 一致（无 token → 401 由安全框架兜底）。
 */
class StandardPermissionMatrixTest {

  private static final Map<String, String> EXPECTED = Map.ofEntries(
      Map.entry("upload", "ximubiaoguanli:generate"),
      Map.entry("page", "ximubiaoguanli:view"),
      Map.entry("getTask", "ximubiaoguanli:view"),
      Map.entry("summarize", "ximubiaoguanli:generate"),
      Map.entry("tree", "ximubiaoguanli:view"),
      Map.entry("updateNode", "ximubiaoguanli:generate"),
      Map.entry("createNode", "ximubiaoguanli:generate"),
      Map.entry("deleteNode", "ximubiaoguanli:generate"),
      Map.entry("moveNode", "ximubiaoguanli:generate"),
      Map.entry("resummarize", "ximubiaoguanli:generate"),
      Map.entry("reviewDone", "ximubiaoguanli:generate"),
      Map.entry("deleteTask", "ximubiaoguanli:generate"),
      Map.entry("exportExcel", "ximubiaoguanli:export"),
      Map.entry("exportSkillExcel", "ximubiaoguanli:export"),
      Map.entry("exportSql", "ximubiaoguanli:export"));

  @Test
  void c5_allEndpointsGuarded() {
    for (Map.Entry<String, String> e : EXPECTED.entrySet()) {
      Method method = findMethod(e.getKey());
      assertNotNull(method, "缺少接口方法：" + e.getKey());
      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
      assertNotNull(preAuthorize, e.getKey() + " 缺少 @PreAuthorize");
      assertEquals("hasAuthority('" + e.getValue() + "')", preAuthorize.value(),
          e.getKey() + " 权限码");
    }
  }

  private Method findMethod(String name) {
    for (Method m : StandardParseController.class.getDeclaredMethods()) {
      if (m.getName().equals(name)) {
        return m;
      }
    }
    return null;
  }
}
