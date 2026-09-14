package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 规范化与卫兵单测（KF-01/02 + KF-14 卫兵口径） */
class KnowledgeNormUtilTest {

  /** KF-01：4 份已知副本（括号空格/HTML 标签/&nbsp; 差异）归一同一 group_key */
  @Test
  void kf01_knownCopiesCollapseToSameKey() {
    String k1 = KnowledgeNormUtil.groupKey("single",
        "<p>生产环节的质量检验不包括（   ）。</p>", "[{\"content\":\"首检\",\"isCorrect\":false,\"score\":0}]");
    String k2 = KnowledgeNormUtil.groupKey("single",
        "<p>生产环节的质量检验不包括（ ）。</p>", "[{\"content\":\"首检\",\"isCorrect\":false,\"score\":0}]");
    String k3 = KnowledgeNormUtil.groupKey("single",
        "<p>生产环节的质量检验不包括（  ）。</p>&nbsp;", "[{\"content\":\"首检\",\"isCorrect\":false,\"score\":0}]");
    assertEquals(k1, k2);
    assertEquals(k1, k3);
  }

  /** KF-02：通用题干 + 不同选项答案 → 不同 group_key（防误判重） */
  @Test
  void kf02_genericStemDifferentOptionsYieldDifferentKeys() {
    String sameStem = "下列说法不正确的是()。";
    String k1 = KnowledgeNormUtil.groupKey("single", sameStem,
        "[{\"content\":\"选项甲内容\",\"isCorrect\":true,\"score\":1}]");
    String k2 = KnowledgeNormUtil.groupKey("single", sameStem,
        "[{\"content\":\"选项乙内容完全不同\",\"isCorrect\":true,\"score\":1}]");
    assertNotEquals(k1, k2);
  }

  /** 空选项片段剥离：带/不带空选项的副本归一 */
  @Test
  void emptyOptionStripped() {
    String withEmpty = KnowledgeNormUtil.groupKey("single", "题干（ ）。",
        "[{\"content\":\"A\",\"isCorrect\":true,\"score\":1},{\"content\":\"\",\"isCorrect\":false,\"score\":0}]");
    String withoutEmpty = KnowledgeNormUtil.groupKey("single", "题干（ ）。",
        "[{\"content\":\"A\",\"isCorrect\":true,\"score\":1}]");
    assertEquals(withEmpty, withoutEmpty);
  }

  /** KF-14 口径：素养节点识别 + 关键词重合判定 */
  @Test
  void kf14_guardHelpers() {
    assertTrue(KnowledgeNormUtil.isQualityNode("工匠精神培养"));
    assertTrue(KnowledgeNormUtil.isQualityNode("安全意识教育"));
    assertFalse(KnowledgeNormUtil.isQualityNode("蒸发器操作"));
    assertTrue(KnowledgeNormUtil.hasBigramOverlap("蒸发器操作", "造成蒸发器跑碱的原因有"));
    assertFalse(KnowledgeNormUtil.hasBigramOverlap("焙烧温度控制", "造成蒸发器跑碱的原因有"));
  }
}
