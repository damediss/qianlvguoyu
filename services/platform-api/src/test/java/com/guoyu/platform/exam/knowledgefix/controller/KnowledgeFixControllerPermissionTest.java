package com.guoyu.platform.exam.knowledgefix.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/** 端点权限拦截反射断言（KF-24）：全部端点逐一核对 @PreAuthorize 权限码 */
class KnowledgeFixControllerPermissionTest {

  @Test
  void allEndpointsCarryExpectedAuthorities() throws Exception {
    Map<String, String> expected = new HashMap<>();
    expected.put("generateDedupList", "exam:knowledgefix:dedup:generate");
    expected.put("dedupStats", "exam:knowledgefix:view");
    expected.put("applyDedup", "exam:knowledgefix:dedup:apply");
    expected.put("rollbackDedup", "exam:knowledgefix:rollback");
    expected.put("submitClassify", "exam:knowledgefix:classify:submit");
    expected.put("resetClassify", "exam:knowledgefix:classify:submit");
    expected.put("pauseClassify", "exam:knowledgefix:classify:submit");
    expected.put("classifyProgress", "exam:knowledgefix:view");
    expected.put("fixList", "exam:knowledgefix:view");
    expected.put("exportFixList", "exam:knowledgefix:classify:export");
    expected.put("applyFix", "exam:knowledgefix:fix:apply");
    expected.put("rollbackFix", "exam:knowledgefix:rollback");
    expected.put("report", "exam:knowledgefix:report:view");

    int checked = 0;
    for (Method m : KnowledgeFixController.class.getDeclaredMethods()) {
      PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
      if (pa == null) {
        continue;
      }
      String expectedCode = expected.get(m.getName());
      assertNotNull(expectedCode, "出现未纳入权限核对的端点: " + m.getName());
      assertTrue(pa.value().contains(expectedCode),
          "端点 " + m.getName() + " 权限码不符: " + pa.value());
      checked++;
    }
    assertEquals(expected.size(), checked, "核对端点数应与控制器写/读端点数一致");
  }
}
