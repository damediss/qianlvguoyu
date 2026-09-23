package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 小培意图路由回归：锁定已踩坑口语，避免再误进 WeKnora / 错 Tool。
 */
class XiaopeiIntentRoutingTest {

  @Nested
  @DisplayName("detectIntent 学员")
  class DetectStudent {

    @ParameterizedTest(name = "[{1}] {0}")
    @CsvSource({
        "考证有什么用, ask_kb",
        "证书有啥用, ask_kb",
        "考这个证有什么用, ask_kb",
        "精准学是干嘛的, precise_about",
        "什么是精准学, precise_about",
        "AI精准学有什么用, precise_about",
        "同步学是干什么的, sync_about",
        "你可以获取数据库中的系统数据吗, data_access",
        "能查数据库吗, data_access",
        "我有多少错题, wrong_stats",
        "错题情况, wrong_stats",
        "能不能考过, exam_readiness",
        "我学到哪里了, learn_detail",
        "你都可以做什么, help",
        "现在做什么, what_now",
        "去做精准学, go_precise",
        "该刷哪, go_sync",
        "讲讲这题, explain_question",
        "做完有什么用, reward_now"
    })
    void routesKnownUtterances(String text, String intent) {
      assertEquals(intent, ClassAssistantApplicationService.detectIntent(text, true));
    }

    @Test
    void goPreciseNotTriggeredByProductAbout() {
      assertEquals("precise_about",
          ClassAssistantApplicationService.detectIntent("精准学是干嘛的", true));
      assertEquals("go_precise",
          ClassAssistantApplicationService.detectIntent("打开精准学", true));
    }
  }

  @Nested
  @DisplayName("softRoute / KB 值得性")
  class SoftAndKb {

    @Test
    void softRouteProductAndDataAccess() {
      assertEquals("precise_about",
          ClassAssistantApplicationService.softRouteStudentFreeForm("精准学是什么"));
      assertEquals("data_access",
          ClassAssistantApplicationService.softRouteStudentFreeForm("你可以获取数据库中的系统数据吗"));
      assertEquals("ask_kb",
          ClassAssistantApplicationService.softRouteStudentFreeForm("考证有什么用"));
    }

    @Test
    void knowledgeBaseWorthy() {
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("电解槽焙烧温度多少"));
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("考证有什么用"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("你可以获取数据库中的系统数据吗"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("今天天气怎么样"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("精准学是干嘛的"));
    }
  }
}
