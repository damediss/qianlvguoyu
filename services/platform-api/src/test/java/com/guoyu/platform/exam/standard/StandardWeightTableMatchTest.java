package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import org.junit.jupiter.api.Test;

/**
 * StandardWeightTable.findRow 宽松匹配兜底：树标题与权重表条目之间的枚举编号/尾注/空格写法差异
 * 不影响权重匹配（矿井通风工"1业前准备"、重冶备料工"五、安全 操作"场景）。
 */
class StandardWeightTableMatchTest {

  private StandardWeightTable tableOf(String... items) {
    StandardWeightTable table = new StandardWeightTable();
    for (String item : items) {
      StandardWeightRow row = new StandardWeightRow();
      row.setItem(item);
      table.getRows().add(row);
    }
    return table;
  }

  private StandardWeightRow find(StandardWeightTable table, String item) {
    return table.findRow(DocxTextUtil.normalizeItemName(item));
  }

  @Test
  void exactMatchStillWorks() {
    StandardWeightTable table = tableOf("业前准备", "业中操作");
    assertSame(table.getRows().get(0), find(table, "业前准备"));
    assertSame(table.getRows().get(1), find(table, "业中操作"));
  }

  @Test
  void arabicPrefixWithoutSeparatorMatches() {
    // 矿井通风工：职业功能 "1业前准备"（编号与标题间无分隔符）↔ 权重条目 "业前准备"
    StandardWeightTable table = tableOf("业前准备", "业中操作");
    assertSame(table.getRows().get(0), find(table, "1业前准备"));
    assertSame(table.getRows().get(1), find(table, "2业中操作"));
  }

  @Test
  void dottedAndDunPrefixMatches() {
    StandardWeightTable table = tableOf("业前准备");
    assertSame(table.getRows().get(0), find(table, "1. 业前准备"));
    assertSame(table.getRows().get(0), find(table, "1、业前准备"));
  }

  @Test
  void chineseNumeralPrefixMatches() {
    // 重冶备料工：职业功能 "五、安全 操作"（中文数字编号）↔ 权重条目 "安全操作"
    StandardWeightTable table = tableOf("安全操作");
    assertSame(table.getRows().get(0), find(table, "五、安全 操作"));
  }

  @Test
  void trailingParenNoteMatches() {
    // 重冶备料工：树标题 "二、物料制备︵工作内容三至五为可选︶" ↔ 权重条目 "物料制备"（含 ︵︶ 全角变体）
    StandardWeightTable table = tableOf("物料制备");
    assertSame(table.getRows().get(0), find(table, "二、物料制备︵工作内容三至五为可选︶"));
    assertSame(table.getRows().get(0), find(table, "物料制备（工作内容三至五为可选）"));
  }

  @Test
  void dunVariantMatches() {
    // 重冶火法冶炼工："洗丶停炉" ↔ "洗、停炉"
    StandardWeightTable table = tableOf("洗、停炉");
    assertSame(table.getRows().get(0), find(table, "洗丶停炉"));
  }

  @Test
  void distinctItemsDoNotCrossMatch() {
    StandardWeightTable table = tableOf("业前准备", "业中操作");
    assertNull(find(table, "业后整理"));
  }

  @Test
  void versionLikeLeadingDigitsNotStripped() {
    // "5S管理""2022版" 等数字开头但非枚举编号的标题不被剥坏
    assertEquals("5S管理", DocxTextUtil.normalizeMatchKey("5S管理"));
    assertEquals("2022版", DocxTextUtil.normalizeMatchKey("2022版"));
  }

  @Test
  void emptyAfterStripReturnsNull() {
    StandardWeightTable table = tableOf("业前准备");
    assertNull(find(table, "业后整理"));
    assertNotNull(find(table, "1业前准备"));
  }
}
