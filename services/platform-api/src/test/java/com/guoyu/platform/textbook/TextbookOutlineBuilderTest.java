package com.guoyu.platform.textbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder.BuildResult;
import com.guoyu.platform.textbook.domain.TextbookOutlineBuilder.OutlineNodeDraft;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 骨架生成（设计 §4.1-TextbookOutlineBuilderTest，铝电解工真实 docx 对拍）。
 */
class TextbookOutlineBuilderTest {

  private static final String DIRECTION = "铝电解槽操作工";
  private static ParsedStandard parsed;

  @BeforeAll
  static void parseFixture() throws Exception {
    try (InputStream in = TextbookOutlineBuilderTest.class
        .getResourceAsStream("/standard/铝电解工.docx")) {
      assertTrue(in != null, "测试资源缺失：standard/铝电解工.docx");
      parsed = new DocxStandardParser().parse(in);
    }
  }

  /** ① 权重为 null（—）的项目不生章：三级无"工作准备"、二级无"设备管理" */
  @Test
  void nullWeightProjectsDoNotBecomeChapters() {
    // 三级/高级工（解析器 levelCode=2）：3.3 节职业功能重新编号（1=铝电解槽操作…），
    // 槽操作工考核 1、11、12 = 铝电解槽操作/设备管理/培训与指导；工作准备权重为 — 且无此功能
    BuildResult l3 = TextbookOutlineBuilder.build(parsed, DIRECTION, 2, List.of(1, 11, 12));
    Set<String> l3Chapters = chapterTitles(l3);
    assertFalse(l3Chapters.contains("工作准备"), "三级工作准备权重为 —，不生章：" + l3Chapters);
    assertFalse(l3Chapters.contains("电解槽维修"), "电解槽维修为三级功能10，不在考核列表：" + l3Chapters);
    assertEquals(Set.of("职业道德", "基础知识", "铝电解槽操作", "设备管理", "培训与指导"),
        l3Chapters, "三级槽操作工章集合不符：" + l3Chapters);

    // 二级/技师（解析器 levelCode=3）：槽操作工考核 1、10、11；权重表"设备管理"行二级为 —
    BuildResult l2 = TextbookOutlineBuilder.build(parsed, DIRECTION, 3, List.of(1, 10, 11));
    Set<String> l2Chapters = chapterTitles(l2);
    assertFalse(l2Chapters.contains("设备管理"), "二级设备管理权重为 —，不生章：" + l2Chapters);
    assertEquals(
        Set.of("职业道德", "基础知识", "铝电解槽操作", "设备和技术管理", "培训与指导"),
        l2Chapters, "二级槽操作工章集合不符：" + l2Chapters);
  }

  /** ② 五级槽操作工章集合 = {职业道德, 基础知识, 工作准备, 铝电解槽操作, 设备管理(职业功能12)} */
  @Test
  void level5CaoOperatorChapterSet() {
    BuildResult result = TextbookOutlineBuilder.build(parsed, DIRECTION, 0, List.of(1, 2, 12));
    Set<String> chapters = chapterTitles(result);
    assertEquals(Set.of("职业道德", "基础知识", "工作准备", "铝电解槽操作", "设备管理"), chapters,
        "五级槽操作工章集合不符：" + chapters);
    // 章权重 = 理论知识权重表（五级/默认列）：5+30+10+50+5=100
    Map<String, OutlineNodeDraft> byTitle = chaptersByTitle(result);
    assertEquals(0, byTitle.get("职业道德").weight().compareTo(new java.math.BigDecimal("5")));
    assertEquals(0, byTitle.get("基础知识").weight().compareTo(new java.math.BigDecimal("30")));
    assertEquals(0, byTitle.get("工作准备").weight().compareTo(new java.math.BigDecimal("10")));
    assertEquals(0, byTitle.get("铝电解槽操作").weight().compareTo(new java.math.BigDecimal("50")));
    assertEquals(0, byTitle.get("设备管理").weight().compareTo(new java.math.BigDecimal("5")));
  }

  /** ③ 节=工作内容、点=theory/skill 条数与标准一致（2.x 五节，3/7/2/2/2 条） */
  @Test
  void sectionsAndPointsMatchStandard() {
    BuildResult result = TextbookOutlineBuilder.build(parsed, DIRECTION, 0, List.of(1, 2, 12));
    Map<String, OutlineNodeDraft> chapters = chaptersByTitle(result);
    OutlineNodeDraft caoOperator = chapters.get("铝电解槽操作");

    List<OutlineNodeDraft> sections = childrenOf(result, caoOperator);
    assertEquals(List.of("通电、焙烧、启动电解槽", "更换阳极", "测量作业", "熄灭阳极效应", "抬母线"),
        sections.stream().map(OutlineNodeDraft::title).toList());

    Map<String, long[]> pointCounts = sections.stream().collect(Collectors.toMap(
        OutlineNodeDraft::title,
        s -> {
          List<OutlineNodeDraft> points = childrenOf(result, s);
          long theory = points.stream().filter(p -> "theory".equals(p.dimension())).count();
          long skill = points.stream().filter(p -> "skill".equals(p.dimension())).count();
          return new long[]{theory, skill};
        }));
    assertEquals(3, pointCounts.get("通电、焙烧、启动电解槽")[0]);
    assertEquals(3, pointCounts.get("通电、焙烧、启动电解槽")[1]);
    assertEquals(7, pointCounts.get("更换阳极")[0]);
    assertEquals(7, pointCounts.get("更换阳极")[1]);
    assertEquals(2, pointCounts.get("测量作业")[0]);
    assertEquals(2, pointCounts.get("熄灭阳极效应")[1]);
    assertEquals(2, pointCounts.get("抬母线")[0]);
  }

  /** ④ source_no 与标准编号一致：章用序号、节用 L2 编号、点用条目编号 */
  @Test
  void sourceNoMatchesStandardNumbering() {
    BuildResult result = TextbookOutlineBuilder.build(parsed, DIRECTION, 0, List.of(1, 2, 12));
    Map<String, OutlineNodeDraft> chapters = chaptersByTitle(result);
    assertEquals("2.1", chapters.get("职业道德").sourceNo());
    assertEquals("1", chapters.get("工作准备").sourceNo());
    assertEquals("12", chapters.get("设备管理").sourceNo());

    OutlineNodeDraft caoOperator = chapters.get("铝电解槽操作");
    assertEquals("2", caoOperator.sourceNo());
    List<OutlineNodeDraft> sections = childrenOf(result, caoOperator);
    assertEquals("2.1", sections.get(0).sourceNo());
    assertEquals("2.5", sections.get(4).sourceNo());

    List<OutlineNodeDraft> points = childrenOf(result, sections.get(0));
    assertEquals("2.1.1", points.get(0).sourceNo());
    assertEquals("2.1.3", points.get(2).sourceNo());
    assertEquals("2.1.1", points.get(3).sourceNo(), "技能点编号与理论点同编号体系（维度区分）");
    assertEquals("skill", points.get(3).dimension());
    // 基础篇点：2.1.1 节 → 2.1.1.1
    OutlineNodeDraft ethicsBasic = childrenOf(result, chapters.get("职业道德")).get(0);
    assertEquals("2.1.1", ethicsBasic.sourceNo());
    assertEquals("2.1.1.1", childrenOf(result, ethicsBasic).get(0).sourceNo());
  }

  /** ⑤ 方向过滤：其他方向的职业功能不出现在骨架中 */
  @Test
  void directionFilterExcludesOtherFunctions() {
    BuildResult result = TextbookOutlineBuilder.build(parsed, DIRECTION, 0, List.of(1, 2, 12));
    Set<String> chapters = chapterTitles(result);
    for (String other : new String[]{"铝吸出作业", "多功能机组作业", "物料输送及烟气净化",
        "电解槽计算机监控", "铝及铝合金熔铸", "炭极生产", "物料分析检测", "铝电解筑炉", "电解槽维修"}) {
      assertFalse(chapters.contains(other), "其他方向职业功能不应生章：" + other);
    }
    // 反向：吸出工方向 = 1、3、12 → 有铝吸出作业、无铝电解槽操作
    BuildResult xichu = TextbookOutlineBuilder.build(parsed, "铝吸出工", 0, List.of(1, 3, 12));
    Set<String> xichuChapters = chapterTitles(xichu);
    assertTrue(xichuChapters.contains("铝吸出作业"), "吸出工方向应含铝吸出作业章");
    assertFalse(xichuChapters.contains("铝电解槽操作"), "吸出工方向不应含铝电解槽操作章");
  }

  private static Set<String> chapterTitles(BuildResult result) {
    return result.nodes().stream()
        .filter(n -> n.nodeLevel() == 2)
        .map(OutlineNodeDraft::title)
        .collect(Collectors.toSet());
  }

  private static Map<String, OutlineNodeDraft> chaptersByTitle(BuildResult result) {
    // 同名章跨篇不冲突（基础篇 2.x 与专业篇功能名不重名），toMap 安全
    return result.nodes().stream()
        .filter(n -> n.nodeLevel() == 2)
        .collect(Collectors.toMap(OutlineNodeDraft::title, Function.identity()));
  }

  private static List<OutlineNodeDraft> childrenOf(BuildResult result, OutlineNodeDraft parent) {
    int parentIdx = result.nodes().indexOf(parent);
    return result.nodes().stream()
        .filter(n -> n.parentIndex() == parentIdx)
        .toList();
  }
}
