package com.guoyu.platform.exam.standard.service;

/**
 * 树内编码序号器（方案 §2.2 / §13.1 B1）：
 * 基本要求=A 段 / 工作要求=B 段；L1 按序取字母 A/B/C…（超 26 进位 AA/AB…）；
 * L2 在 L1 编码后再取字母；L3 在 L2 编码后接 3 位序号（001 起）。
 */
public final class NodeKeyGenerator {

  private NodeKeyGenerator() {
  }

  /** 序号 → 字母编码：0→A，25→Z，26→AA，27→AB（进位制） */
  public static String letters(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index 不能为负");
    }
    StringBuilder sb = new StringBuilder();
    int n = index;
    do {
      sb.insert(0, (char) ('A' + (n % 26)));
      n = n / 26 - 1;
    } while (n >= 0);
    return sb.toString();
  }

  /** 子级编码：parentKey + "-" + 字母段 */
  public static String childKey(String parentKey, int childIndex) {
    return parentKey + "-" + letters(childIndex);
  }

  /** L3 叶编码：l2Key + "-" + 三位序号（001 起） */
  public static String leafKey(String l2Key, int leafIndex) {
    return l2Key + "-" + String.format("%03d", leafIndex + 1);
  }

  /** 标题内嵌编码：title + nodeKey（与样例 "职业道德A-A" 一致） */
  public static String titleWithKey(String title, String nodeKey) {
    return title + nodeKey;
  }
}
