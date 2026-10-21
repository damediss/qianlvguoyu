package com.guoyu.platform.exam.reviewsync.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 无意义作答闸口（拦截方案 §4.1）：R1~R4 各模式 + 参考答案关键词豁免 + 边界。
 * 宁漏勿错——拿不准的一律放行给 LLM。
 */
class SubjectiveAnswerGuardTest {

  private static final String REFERENCE =
      "方向是低碳、零排放、循环经济，实施路径含节能、环保技术及循环经济举措";
  private static final int MIN_HANZI = 4;

  private static boolean blocked(String answer) {
    return SubjectiveAnswerGuard.check(answer, REFERENCE, MIN_HANZI).meaningless();
  }

  private static boolean blocked(String answer, String reference) {
    return SubjectiveAnswerGuard.check(answer, reference, MIN_HANZI).meaningless();
  }

  // R1 纯数字/纯字母/纯符号
  @Test
  void r1PureDigitLetterSymbolBlocked() {
    for (String a : new String[]{"2", "3", "4", "5", "6", "8", "10", "666", "Q", "y", "...", "？",
        "!"}) {
      assertTrue(blocked(a), "应拦截: " + a);
    }
  }

  // R2 无汉字且总长 < 15
  @Test
  void r2NoHanziUnder15Blocked() {
    for (String a : new String[]{"asdf", "ok", "idk", "abc123", "a b c"}) {
      assertTrue(blocked(a), "应拦截: " + a);
    }
  }

  // R2 放行：无汉字但足够长（可能是认真写的英文作答）
  @Test
  void r2LongLatinPasses() {
    assertFalse(blocked("low carbon recycling is the main direction of green manufacturing"));
  }

  // R3 汉字过少且总长 < 8
  @Test
  void r3TooFewHanziBlocked() {
    for (String a : new String[]{"不会", "不知道", "略", "过", "忘了"}) {
      assertTrue(blocked(a), "应拦截: " + a);
    }
  }

  // R4 重复字符/片段
  @Test
  void r4RepetitionBlocked() {
    for (String a : new String[]{"哈哈哈哈", "啊啊啊啊", "123123123", "ababab", "不知道不知道不知道"}) {
      assertTrue(blocked(a), "应拦截: " + a);
    }
  }

  // 豁免：命中参考答案关键词的真实短答
  @Test
  void exemptionHitsReferenceKeyword() {
    assertFalse(blocked("低碳、零排放"));
    assertFalse(blocked("节能技术"));
    assertFalse(blocked("循环经济"));
  }

  // 豁免优先级：即便文本极短，命中关键词也放行；单字不作豁免
  @Test
  void exemptionBeatsR3() {
    assertFalse(blocked("低碳", REFERENCE));
    assertTrue(blocked("低", REFERENCE));
  }

  // 正常作答放行
  @Test
  void normalAnswerPasses() {
    assertFalse(blocked("影响因素有材料匹配、温度波动和熔体冲刷"));
    assertFalse(blocked("我想不起来了"));
  }

  // 边界：空白归一化
  @Test
  void whitespaceNormalizedBeforeCheck() {
    assertTrue(blocked("  2  "));
    assertTrue(blocked("不 会"));
  }

  // 防御：空作答（上游已挡，闸口兜底）
  @Test
  void emptyAnswerBlocked() {
    assertTrue(blocked(""));
    assertTrue(blocked(null));
    assertTrue(blocked("   "));
  }

  // minHanzi 阈值参数化生效
  @Test
  void minHanziThresholdParameterized() {
    // "不会" 2 汉字：阈值 4 拦截，阈值 2 放行
    assertTrue(SubjectiveAnswerGuard.check("不会", REFERENCE, 4).meaningless());
    assertFalse(SubjectiveAnswerGuard.check("不会", REFERENCE, 2).meaningless());
  }

  // 无参考答案时不享受豁免，但不改变基础判定
  @Test
  void noReferenceStillAppliesBaseRules() {
    assertTrue(blocked("2", null));
    assertTrue(blocked("不会", null));
    assertFalse(blocked("认真作答的具体内容", null));
  }

  // 判定结果带 reason 供日志观测
  @Test
  void verdictCarriesReason() {
    assertEquals("noHanzi", SubjectiveAnswerGuard.check("2", REFERENCE, MIN_HANZI).reason());
    assertEquals("tooShort", SubjectiveAnswerGuard.check("不会", REFERENCE, MIN_HANZI).reason());
    assertEquals("empty", SubjectiveAnswerGuard.check("", REFERENCE, MIN_HANZI).reason());
    assertEquals(SubjectiveAnswerGuard.MEANINGFUL,
        SubjectiveAnswerGuard.check("认真作答内容", REFERENCE, MIN_HANZI));
  }
}
