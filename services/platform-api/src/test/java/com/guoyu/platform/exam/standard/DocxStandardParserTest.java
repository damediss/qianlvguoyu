package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A 组：DocxStandardParser 黄金样例驱动测试（方案 §13.1 A1~A9）。
 * 样例文件：src/test/resources/standard/矿山救护工.docx、焊工标准.docx
 */
class DocxStandardParserTest {

  private static ParsedStandard kuangshan;
  private static ParsedStandard hangong;
  private static ParsedStandard beiliao;

  @BeforeAll
  static void parseAll() throws Exception {
    DocxStandardParser parser = new DocxStandardParser();
    try (InputStream in = DocxStandardParserTest.class
        .getResourceAsStream("/standard/矿山救护工.docx")) {
      assertNotNull(in, "测试资源缺失：矿山救护工.docx");
      kuangshan = parser.parse(in);
    }
    try (InputStream in = DocxStandardParserTest.class
        .getResourceAsStream("/standard/焊工标准.docx")) {
      assertNotNull(in, "测试资源缺失：焊工标准.docx");
      hangong = parser.parse(in);
    }
    try (InputStream in = DocxStandardParserTest.class
        .getResourceAsStream("/standard/重冶备料工.docx")) {
      assertNotNull(in, "测试资源缺失：重冶备料工.docx");
      beiliao = parser.parse(in);
    }
  }

  /** A1：矿山救护工全量解析——表格识别、基本要求结构、等级节数量、无 ERROR 级警告 */
  @Test
  void a1_kuangshanFullParse() {
    assertEquals(20, kuangshan.getTotalTableCount(), "矿山救护工应识别 20 张表格");
    assertEquals(18, kuangshan.getWorkTableCount(), "18 张工作要求表");
    assertEquals(2, kuangshan.getWeightTableCount(), "2 张权重表");
    assertEquals(5, kuangshan.getLevels().size(), "应识别 5 个等级节");
    assertEquals("矿山救护工", kuangshan.getWorkTypeTitle());
    assertEquals("6-16-01-12", kuangshan.getSeriesNo());
    assertEquals("2019", kuangshan.getVersion());

    List<StandardNodeDoc> basic = kuangshan.getBasicSections();
    assertEquals(2, basic.size(), "基本要求 L1=2（职业道德/基础知识）");
    assertEquals("职业道德", basic.get(0).getTitle());
    assertEquals("基础知识", basic.get(1).getTitle());
    int l2Count = basic.stream().mapToInt(l1 -> l1.getChildren().size()).sum();
    assertEquals(7, l2Count, "基本要求 L2=7（2.1.1~2.2.5）");

    for (String warning : kuangshan.getParseWarnings()) {
      assertFalse(warning.startsWith("ERROR"), "不应有 ERROR 级警告：" + warning);
    }
  }

  /** A1b：职业名称剥离尾部标点——重冶备料工 docx 1.1 原文为"重冶备料工 。"（PDF 转 docx 残留），且无职业编码节/年版 */
  @Test
  void a1b_beiliaoTitleStripsTrailingPunctuation() {
    assertEquals("重冶备料工", beiliao.getWorkTypeTitle(), "尾部句号与空格应被剥离");
    assertNull(beiliao.getSeriesNo(), "该 docx 无职业编码节，编码应为空");
    assertNull(beiliao.getVersion(), "该 docx 全文无年版字样，版本应为空");
  }

  /** A2：骨架层级对拍 exam_knowledge_structure_矿山救护工初级.md */
  @Test
  void a2_kuangshanSkeleton() {
    StandardLevelDoc chuJi = kuangshan.levelOf(0);
    assertNotNull(chuJi);
    assertEquals("五级/初级工", chuJi.getLevelName());

    List<StandardNodeDoc> funcs = chuJi.getWorkFunctions();
    assertEquals(3, funcs.size(), "初级工职业功能=3（事故预防/事故救援/装备操作）");
    assertEquals("事故预防", funcs.get(0).getTitle());
    assertEquals("事故救援", funcs.get(1).getTitle());
    assertEquals("装备操作", funcs.get(2).getTitle());

    List<StandardNodeDoc> l2OfPrevention = funcs.get(0).getChildren();
    assertEquals(2, l2OfPrevention.size());
    assertEquals("安全检查", l2OfPrevention.get(0).getTitle());
    assertEquals("安全技术工作实施", l2OfPrevention.get(1).getTitle());

    assertEquals(2, funcs.get(1).getChildren().size(), "事故救援 L2=2");
    assertEquals(3, funcs.get(2).getChildren().size(), "装备操作 L2=3");

    // 技师含培训指导
    StandardLevelDoc jiShi = kuangshan.levelOf(3);
    assertNotNull(jiShi);
    assertTrue(jiShi.getWorkFunctions().stream().anyMatch(f -> f.getTitle().contains("培训指导")),
        "技师应含培训指导职业功能");
  }

  /** A3：焊工 vMerge 继承——跨行合并的职业功能不断裂 */
  @Test
  void a3_hangongVmergeInheritance() {
    StandardLevelDoc chuJi = hangong.levelOf(0);
    assertNotNull(chuJi);
    assertFalse(chuJi.getWorkFunctions().isEmpty());
    StandardNodeDoc func1 = chuJi.getWorkFunctions().get(0);
    assertTrue(func1.getTitle().contains("焊条电弧焊"), "首个职业功能应为焊条电弧焊类：" + func1.getTitle());
    assertEquals(3, func1.getChildren().size(), "职业功能1应有3个工作内容（焊前准备/焊接操作/焊后检查）");
    assertEquals("焊前准备", func1.getChildren().get(0).getTitle());
    assertEquals("焊接操作", func1.getChildren().get(1).getTitle());
    assertEquals("焊后检查", func1.getChildren().get(2).getTitle());
    // 续行（vMerge 继承行）的技能要求与相关知识不得为空
    for (StandardNodeDoc l2 : func1.getChildren()) {
      assertFalse(l2.getSkillMaterials().isEmpty(), l2.getTitle() + " 技能素材不应为空");
      assertFalse(l2.getTheoryMaterials().isEmpty(), l2.getTitle() + " 理论素材不应为空");
    }
    // 所有职业功能标题均不含空标题（vMerge 悬空）
    for (StandardNodeDoc f : chuJi.getWorkFunctions()) {
      assertFalse(f.getTitle().isBlank(), "职业功能标题不应为空");
    }
  }

  /** A4：编号陷阱——职业编码/续表/页脚注不产生节点 */
  @Test
  void a4_headingTraps() {
    assertNodeTreeClean(kuangshan);
    assertNodeTreeClean(hangong);
  }

  private void assertNodeTreeClean(ParsedStandard standard) {
    for (StandardNodeDoc l1 : standard.getBasicSections()) {
      assertFalse(l1.getTitle().contains("6-16-01-12") || l1.getTitle().contains("续表")
          || l1.getTitle().contains("职业编码"), l1.getTitle());
      for (StandardNodeDoc l2 : l1.getChildren()) {
        assertFalse(l2.getTitle().contains("续表") || l2.getTitle().contains("①"), l2.getTitle());
      }
    }
    for (StandardLevelDoc level : standard.getLevels()) {
      for (StandardNodeDoc f : level.getWorkFunctions()) {
        assertFalse(f.getTitle().contains("续表") || f.getTitle().contains("职业编码"), f.getTitle());
        for (StandardNodeDoc l2 : f.getChildren()) {
          assertFalse(l2.getTitle().contains("续表"), l2.getTitle());
        }
      }
    }
  }

  /** A5：矿山救护工理论权重表 */
  @Test
  void a5_kuangshanTheoryWeights() {
    StandardWeightTable theory = kuangshan.getTheoryWeights();
    assertNotNull(theory, "应解析到理论知识权重表");
    assertWeight(theory, "职业道德", 0, "默认", 5);
    assertWeight(theory, "基础知识", 0, "默认", 20);
    assertWeight(theory, "事故预防", 0, "默认", 20);
    assertWeight(theory, "事故救援", 0, "默认", 30);
    assertWeight(theory, "装备操作", 0, "默认", 25);
    assertWeight(theory, "培训指导", 3, "默认", 10);
    // 初级无培训指导 → —
    StandardWeightRow peixun = theory.findRow(DocxTextUtil.normalizeItemName("培训指导"));
    assertNotNull(peixun);
    assertNull(peixun.valueAt(0, "默认"), "初级培训指导应为 —（null）");
  }

  /** A6：矿山救护工技能权重表 */
  @Test
  void a6_kuangshanSkillWeights() {
    StandardWeightTable skill = kuangshan.getSkillWeights();
    assertNotNull(skill, "应解析到技能要求权重表");
    assertWeight(skill, "事故预防", 0, "默认", 15);
    assertWeight(skill, "事故救援", 0, "默认", 15);
    assertWeight(skill, "装备操作", 0, "默认", 70);
    StandardWeightRow peixun = skill.findRow(DocxTextUtil.normalizeItemName("培训指导"));
    assertNotNull(peixun);
    assertNull(peixun.valueAt(0, "默认"), "初级培训指导技能权重应为 null");
  }

  /** A7：焊工方向矩阵——116 表全解析、4 方向×5 等级双权重矩阵完整 */
  @Test
  void a7_hangongDirectionMatrix() {
    assertEquals(116, hangong.getTotalTableCount(), "焊工应识别 116 张表格");
    assertEquals(97, hangong.getWorkTableCount());
    assertEquals(19, hangong.getWeightTableCount());
    assertEquals(5, hangong.getLevels().size());
    assertEquals(List.of("电焊工", "气焊工", "钎焊工", "焊接设备操作工"), hangong.getDirections(),
        "应识别 4 个方向");
    assertEquals("6-18-02-04", hangong.getSeriesNo());

    StandardWeightTable theory = hangong.getTheoryWeights();
    assertNotNull(theory);
    // 五级/电焊工：焊条电弧焊（必选）理论 25
    assertWeight(theory, "低碳钢或低合金钢板角接或T形接头平焊焊条电弧焊（必选）", 0, "电焊工", 25);
    // 同一项目对气焊工为空
    StandardWeightRow row = theory.findRow(
        DocxTextUtil.normalizeItemName("低碳钢或低合金钢板角接或T形接头平焊焊条电弧焊（必选）"));
    assertNotNull(row);
    assertNull(row.valueAt(0, "气焊工"), "气焊工不包含焊条电弧焊项目");
    // 钎焊工五级：火焰钎焊（必选）25
    assertWeight(theory, "低碳钢板对接/搭接火焰钎焊（必选）", 0, "钎焊工", 25);
    // 焊接技术管理：二级/电焊工 25
    assertWeight(theory, "焊接技术管理（必选）", 3, "电焊工", 25);
    // 基本要求各方向同值
    assertWeight(theory, "职业道德", 0, "电焊工", 5);
    assertWeight(theory, "职业道德", 4, "焊接设备操作工", 5);

    StandardWeightTable skill = hangong.getSkillWeights();
    assertNotNull(skill);
    assertWeight(skill, "低碳钢或低合金钢板角接或T形接头平焊焊条电弧焊（必选）", 0, "电焊工", 40);
    assertWeight(skill, "焊接技术管理（必选）", 3, "电焊工", 30);
  }

  /** A8：技能素材抽取——编号项逐条抽出 */
  @Test
  void a8_skillMaterials() {
    StandardLevelDoc chuJi = kuangshan.levelOf(0);
    StandardNodeDoc anquanCheck = chuJi.getWorkFunctions().get(0).getChildren().get(0);
    List<String> skills = anquanCheck.getSkillMaterials();
    assertEquals(2, skills.size(), "安全检查应有 2 条技能素材");
    assertTrue(skills.get(0).startsWith("能查出风门"), skills.get(0));
    assertTrue(skills.get(1).contains("能查出"), skills.get(1));

    // （1）/1./1) 编号写法兼容（单元测试工具方法）
    assertEquals(3, DocxTextUtil.splitItems("（1）甲。（2）乙。（3）丙。").size());
    assertEquals(2, DocxTextUtil.splitItems("1.1.1 能甲 1.1.2 能乙").size());
    assertEquals(2, DocxTextUtil.splitItems("1) 甲 2) 乙").size());

    // relineItems：单行串联按编号逐行重排（保留编号），已多行/无编号文本原样返回
    assertEquals("2.2.1精矿指标判断\n2.2.2沉降槽泥层判断",
        DocxTextUtil.relineItems("2.2.1精矿指标判断2.2.2沉降槽泥层判断"));
    assertEquals("2.1精矿调配\n2.2磨矿操作",
        DocxTextUtil.relineItems("2.1精矿调配2.2磨矿操作"), "两级点号编号也可重排");
    assertEquals("（1）甲\n（2）乙", DocxTextUtil.relineItems("（1）甲（2）乙"));
    assertEquals("1.1.1能查出风门损坏\n1.1.2能查出风险点",
        DocxTextUtil.relineItems("1.1.1能查出风门损坏\n1.1.2能查出风险点"), "已多行文本原样返回");
    assertEquals("矿浆浓度控制方法", DocxTextUtil.relineItems("矿浆浓度控制方法"), "无编号文本原样返回");

    // 带 ★ 的技能条目应去星
    StandardNodeDoc fanghu = chuJi.getWorkFunctions().get(2).getChildren().get(0);
    assertTrue(fanghu.getSkillMaterials().stream().noneMatch(s -> s.contains("★")),
        "技能素材应剥离 ★ 标记");
    assertTrue(fanghu.getSkillMaterials().size() >= 4, "防护装备操作应有 ≥4 条技能素材");
  }

  /** A9：空小节标记；"职业道德基本知识"空小节按统一业务口径固定补齐相关知识 */
  @Test
  void a9_emptySectionNeedLlm() {
    StandardNodeDoc zhiyeDaode = kuangshan.getBasicSections().get(0);
    StandardNodeDoc jibenZhishi = zhiyeDaode.getChildren().get(0);
    assertEquals("职业道德基本知识", jibenZhishi.getTitle());
    // 源标准无列举项，解析期固定补齐：素材 4 条、相关知识带编号逐行、不再标记 needLlm
    assertEquals(List.of("工匠精神培养", "安全意识培养", "职业道德素养", "团队协作精神"),
        jibenZhishi.getTheoryMaterials(), "2.1.1 应补齐固定素材");
    assertEquals("1.工匠精神培养\n2.安全意识培养\n3.职业道德素养\n4.团队协作精神",
        jibenZhishi.getRelatedKnowledgeRaw());
    assertFalse(jibenZhishi.isNeedLlm(), "补齐后不再标记 needLlm");
    // 2.1.2 职业守则有 9 条
    StandardNodeDoc shouze = zhiyeDaode.getChildren().get(1);
    assertEquals(9, shouze.getTheoryMaterials().size());
    assertFalse(shouze.isNeedLlm());
  }

  /** B1：重冶备料工"比重表"写法——无"技能等级项目"表头、等级列为初级/中级/高级/技师/高级技师 */
  @Test
  void b1_beiliaoWeightTablesRecognized() {
    assertEquals(12, beiliao.getTotalTableCount(), "重冶备料工应识别 12 张表格");
    assertEquals(10, beiliao.getWorkTableCount(), "10 张工作要求表");
    assertEquals(2, beiliao.getWeightTableCount(), "2 张比重表（无技能等级项目表头也应识别）");
    assertNotNull(beiliao.getTheoryWeights(), "应解析到理论知识比重表");
    assertNotNull(beiliao.getSkillWeights(), "应解析到技能操作比重表");
    assertTrue(beiliao.getParseWarnings().stream()
            .noneMatch(w -> w.contains("未解析到理论知识权重表")),
        "不应再有理论权重表缺失警告");
  }

  /** B2：重冶备料工比重表取值——等级关键词列映射、"—"为 null、"总计"行不入表 */
  @Test
  void b2_beiliaoWeightValues() {
    StandardWeightTable theory = beiliao.getTheoryWeights();
    assertWeight(theory, "职业道德", 0, "默认", 5);
    assertWeight(theory, "职业道德", 4, "默认", 5);
    assertWeight(theory, "基础知识", 0, "默认", 40);
    assertWeight(theory, "基础知识", 3, "默认", 15);
    assertWeight(theory, "物料制备", 2, "默认", 30);
    assertWeight(theory, "设备维护", 3, "默认", 20);
    assertWeight(theory, "管理", 3, "默认", 15);
    assertWeight(theory, "培训与指导", 4, "默认", 10);
    // "—" 占位：技师/高级技师无安全操作
    StandardWeightRow anquan = theory.findRow(DocxTextUtil.normalizeItemName("安全操作"));
    assertNotNull(anquan);
    assertWeight(theory, "安全操作", 0, "默认", 5);
    assertNull(anquan.valueAt(3, "默认"), "技师安全操作应为 —（null）");
    assertNull(anquan.valueAt(4, "默认"), "高级技师安全操作应为 —（null）");
    StandardWeightRow guanli = theory.findRow(DocxTextUtil.normalizeItemName("管理"));
    assertNotNull(guanli);
    assertNull(guanli.valueAt(0, "默认"), "初级管理应为 —（null）");
    // "总计" 行应跳过，不入权重表
    assertNull(theory.findRow("总计"), "总计行不应进入权重表");

    StandardWeightTable skill = beiliao.getSkillWeights();
    assertWeight(skill, "工作准备", 0, "默认", 10);
    assertWeight(skill, "物料制备", 0, "默认", 35);
    assertWeight(skill, "物料制备", 4, "默认", 20);
    assertWeight(skill, "安全操作", 1, "默认", 15);
    assertWeight(skill, "管理", 4, "默认", 20);
    assertNull(skill.findRow("总计"), "技能比重表总计行不应进入权重表");
  }

  private void assertWeight(StandardWeightTable table, String item, int levelCode,
      String direction, int expected) {
    StandardWeightRow row = table.findRow(DocxTextUtil.normalizeItemName(item));
    assertNotNull(row, "权重表缺少项目行：" + item);
    BigDecimal value = row.valueAt(levelCode, direction);
    assertNotNull(value, item + " 在 " + levelCode + "/" + direction + " 应有权重值");
    assertEquals(0, value.compareTo(new BigDecimal(expected)),
        item + " 在 " + levelCode + "/" + direction + " 权重应为 " + expected);
  }
}
