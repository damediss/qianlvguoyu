package com.guoyu.platform.exam.reviewsync.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** 主观题参考答案提取：string / array / object / JSON 字符串四形态 + 空与失败兜底（方案 §8.1） */
class SubjectiveAnswerExtractorTest {

  @Test
  void plainTextReturnedAsIs() {
    assertEquals("这是参考答案", SubjectiveAnswerExtractor.extract("这是参考答案"));
    assertEquals("答案 中间 有空格", SubjectiveAnswerExtractor.extract("答案 中间 有空格"));
  }

  @Test
  void plainTextTrimmedWhenBlankAround() {
    assertEquals("参考答案", SubjectiveAnswerExtractor.extract("  参考答案  "));
  }

  @Test
  void jsonArrayJoinsItemContents() {
    assertEquals("要点一\n要点二",
        SubjectiveAnswerExtractor.extract("[{\"content\":\"要点一\"},{\"content\":\"要点二\"}]"));
  }

  @Test
  void jsonArraySkipsEmptyAndNonContentItems() {
    assertEquals("要点一\n纯文本项",
        SubjectiveAnswerExtractor.extract("[{\"content\":\"要点一\"},\"纯文本项\",{\"tag\":\"x\"},{\"content\":\"\"}]"));
  }

  @Test
  void jsonObjectTakesContent() {
    assertEquals("唯一答案", SubjectiveAnswerExtractor.extract("{\"content\":\"唯一答案\"}"));
  }

  @Test
  void jsonStringFormParsedThenHandled() {
    // 以 [ 或 { 开头的 JSON 字符串：先 parse 再按数组/对象处理
    assertEquals("答案A\n答案B", SubjectiveAnswerExtractor.extract("[\"答案A\",\"答案B\"]"));
    assertEquals("对象答案", SubjectiveAnswerExtractor.extract("{\"content\":\"对象答案\",\"other\":1}"));
  }

  @Test
  void brokenJsonReturnsNull() {
    assertNull(SubjectiveAnswerExtractor.extract("[{broken"));
    assertNull(SubjectiveAnswerExtractor.extract("{\"content\":"));
  }

  @Test
  void emptyOrStructurelessReturnsNull() {
    assertNull(SubjectiveAnswerExtractor.extract(null));
    assertNull(SubjectiveAnswerExtractor.extract(""));
    assertNull(SubjectiveAnswerExtractor.extract("   "));
    assertNull(SubjectiveAnswerExtractor.extract("[]"));
    assertNull(SubjectiveAnswerExtractor.extract("[{\"content\":\"\"}]"));
    assertNull(SubjectiveAnswerExtractor.extract("{\"tag\":\"A\"}"));
    assertNull(SubjectiveAnswerExtractor.extract("{}"));
  }
}
