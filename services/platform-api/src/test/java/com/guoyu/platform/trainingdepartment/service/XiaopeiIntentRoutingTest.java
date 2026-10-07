package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        "我的学习情况怎么样, learn_detail",
        "学习情况, learn_detail",
        "刷题情况怎么样, learn_detail",
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
  @DisplayName("个人数据域门禁")
  class PersonalDomain {

    @ParameterizedTest
    @ValueSource(strings = {
        "我的学习情况怎么样",
        "学习进度如何",
        "我有多少错题",
        "作业做完了吗",
        "我完成了哪些任务",
        "能不能考过",
        "刷题情况怎么样",
        "掌握情况如何"
    })
    void personalDomainBlockedFromKb(String text) {
      assertTrue(ClassAssistantApplicationService.looksLikePersonalDataDomain(text));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy(text));
    }

    @ParameterizedTest(name = "[{1}] {0}")
    @CsvSource({
        "我的学习情况怎么样, learn_detail",
        "我有多少错题, wrong_stats",
        "我完成了哪些任务, my_homework",
        "能不能考过, exam_readiness",
        "薄弱点有哪些, weak_points"
    })
    void resolvePersonalIntent(String text, String intent) {
      assertEquals(intent, ClassAssistantApplicationService.resolvePersonalDataIntent(text));
    }

    @Test
    void conceptQuestionStillKbWorthy() {
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("电解槽焙烧温度多少"));
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("考证有什么用"));
      assertFalse(ClassAssistantApplicationService.looksLikePersonalDataDomain("电解槽焙烧温度多少"));
    }
  }

  @Nested
  @DisplayName("softRoute / KB 值得性 / 元话术")
  class SoftAndKb {

    @Test
    void softRouteProductAndDataAccess() {
      assertEquals("precise_about",
          ClassAssistantApplicationService.softRouteStudentFreeForm("精准学是什么"));
      assertEquals("data_access",
          ClassAssistantApplicationService.softRouteStudentFreeForm("你可以获取数据库中的系统数据吗"));
      assertEquals("ask_kb",
          ClassAssistantApplicationService.softRouteStudentFreeForm("考证有什么用"));
      assertEquals("learn_detail",
          ClassAssistantApplicationService.softRouteStudentFreeForm("我的学习情况怎么样"));
    }

    @Test
    void knowledgeBaseWorthy() {
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("电解槽焙烧温度多少"));
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("考证有什么用"));
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("知识问答"));
      assertTrue(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("高处作业注意事项"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("你可以获取数据库中的系统数据吗"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("今天天气怎么样"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("精准学是干嘛的"));
      assertFalse(ClassAssistantApplicationService.looksLikeKnowledgeBaseWorthy("我的学习情况怎么样"));
    }

    @Test
    void homeworkNotConfusedWithIndustrialOps() {
      assertEquals("ask_kb",
          ClassAssistantApplicationService.detectIntent("高处作业注意事项", true));
      assertEquals("my_homework",
          ClassAssistantApplicationService.detectIntent("我的作业做完了吗", true));
    }

    @Test
    void helpLookDoesNotForceLearnDetail() {
      assertEquals("ask_kb",
          ClassAssistantApplicationService.detectIntent("帮我看看焙烧温度", true));
      assertEquals(null,
          ClassAssistantApplicationService.softRouteStudentFreeForm("帮我看看焙烧温度"));
      assertEquals("ask_kb",
          ClassAssistantApplicationService.detectIntent("焙烧情况怎么样", true));
      assertFalse(ClassAssistantApplicationService.looksLikeExamReadiness("看过吗"));
      assertTrue(ClassAssistantApplicationService.looksLikeExamReadiness("能过吗"));
    }

    @Test
    void explainTokensRequireCurrentQuestion() {
      assertEquals("ask_kb",
          ClassAssistantApplicationService.detectIntent("讲解一下安全规程", true));
      assertEquals("explain_question",
          ClassAssistantApplicationService.detectIntent("讲讲这题", true));
    }

    @Test
    void metaRetrievalEssayDetected() {
      String sample = "关于「誉升电解槽-4班学员学习进度」的查询，说明如下：\n"
          + "**知识库检索情况：** 未检索到与该问题直接匹配的内容。"
          + "当前知识库仅收录「AI助教_智能问答」一个 FAQ 条目容器。"
          + "**数据获取说明：** 学员的学习进度属于学习平台的实时数据，"
          + "无法通过知识库检索获取，我也不会猜测或编造相关数字。";
      assertTrue(ClassAssistantApplicationService.looksLikeKbMetaRetrievalEssay(sample));
      assertFalse(ClassAssistantApplicationService.looksLikeKbMetaRetrievalEssay(
          "焙烧温度一般按工艺规程控制，以教材为准。"));
    }
  }
}
