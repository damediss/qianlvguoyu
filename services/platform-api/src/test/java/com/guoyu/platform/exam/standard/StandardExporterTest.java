package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.export.KnowledgeSqlExporter;
import com.guoyu.platform.exam.standard.export.MubiaoExcelExporter;
import com.guoyu.platform.exam.standard.export.SkillMubiaoExcelExporter;
import com.guoyu.platform.exam.standard.export.StandardExportSupport;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * B4 X/Y/Z 统计 / B7 SQL 导出 / B8 Excel 导出（方案 §13.1）。
 */
class StandardExporterTest {

  private static ExportTree tree;
  private static List<ExamStandardNodeEntity> nodes;

  @BeforeAll
  static void buildTree() {
    nodes = new ArrayList<>();
    // A 支：基本要求A → 职业道德A-A → 职业道德基本知识A-A-A → 2 理论叶（X/Y）
    nodes.add(node(1L, 0L, "A", 0, "基本要求A", null, new BigDecimal("0.2500"), "theory"));
    nodes.add(node(2L, 1L, "A-A", 1, "职业道德A-A", null, new BigDecimal("0.0500"), "theory"));
    nodes.add(node(3L, 2L, "A-A-A", 2, "职业道德基本知识A-A-A", null, null, "theory"));
    nodes.get(2).setRelatedKnowledge("1.职业道德基本知识");
    nodes.add(node(4L, 3L, "A-A-A-001", 3, "工匠精神的基本内涵", "X", null, "theory"));
    nodes.add(node(5L, 3L, "A-A-A-002", 3, "职业道德的特点", "Y", null, "theory"));
    // B 支：工作要求B → 事故预防B-A → 安全检查B-A-A → 2 理论叶 + 2 技能叶
    nodes.add(node(6L, 0L, "B", 0, "工作要求B", null, new BigDecimal("0.7500"), "theory"));
    nodes.add(node(7L, 6L, "B-A", 1, "事故预防B-A", null, new BigDecimal("0.2000"), "theory"));
    nodes.add(node(8L, 7L, "B-A-A", 2, "安全检查B-A-A", null, null, "theory"));
    nodes.get(7).setRelatedKnowledge("1.通风设施的相关知识");
    nodes.get(7).setSkillRequirement("1.1.1能查出风门损坏\n1.1.2能查出风险点");
    nodes.add(node(9L, 8L, "B-A-A-001", 3, "通风设施相关知识", "X", null, "theory"));
    nodes.add(node(10L, 8L, "B-A-A-002", 3, "风险点相关知识", "Z", null, "theory"));
    nodes.add(node(11L, 8L, "B-A-A-001", 3, "检查风门损坏状况", "X", null, "skill"));
    nodes.add(node(12L, 8L, "B-A-A-002", 3, "检查风险点", "X", null, "skill"));
    tree = StandardExportSupport.assemble(nodes);
  }

  private static ExamStandardNodeEntity node(Long id, Long parentId, String key, int level,
      String title, String important, BigDecimal weight, String dimension) {
    ExamStandardNodeEntity n = new ExamStandardNodeEntity();
    n.setId(id);
    n.setTaskId(100L);
    n.setParentId(parentId);
    n.setNodeKey(key);
    n.setNodeLevel(level);
    n.setSortNumber((int) (long) id);
    n.setTitle(title);
    n.setImportant(important);
    n.setWeight(weight);
    n.setDimension(dimension);
    n.setDirection("默认");
    n.setLevelCode(0);
    n.setReviewStatus("approved");
    n.setSource("extract");
    return n;
  }

  /** B4：子树 X:Y:Z 计数与叶分布一致（dimension 过滤） */
  @Test
  void b4_xyzCounts() {
    int[] a = StandardExportSupport.countXyz(tree, 1L, "theory");
    assertArrayEquals(new int[]{1, 1, 0}, a, "A 支 X:Y:Z=1:1:0");
    int[] b = StandardExportSupport.countXyz(tree, 6L, "theory");
    assertArrayEquals(new int[]{1, 0, 1}, b, "B 支理论叶 X:Y:Z=1:0:1");
    int[] bSkill = StandardExportSupport.countXyz(tree, 6L, "skill");
    assertArrayEquals(new int[]{2, 0, 0}, bSkill, "B 支技能叶 X:Y:Z=2:0:0");
    assertEquals("（1:1:0）", StandardExportSupport.xyzText(a));
  }

  private static void assertArrayEquals(int[] expected, int[] actual, String msg) {
    assertEquals(expected[0], actual[0], msg + " [X]");
    assertEquals(expected[1], actual[1], msg + " [Y]");
    assertEquals(expected[2], actual[2], msg + " [Z]");
  }

  /** B8c：L2 权重最大余数法——合计严格等于 L1 权重（复现 0.03+0.03>0.05 的显示矛盾场景） */
  @Test
  void b8c_l2WeightLargestRemainder() {
    List<ExamStandardNodeEntity> ns = new ArrayList<>();
    ns.add(node(101L, 0L, "A", 0, "基本要求A", null, new BigDecimal("0.30"), "theory"));
    ns.add(node(102L, 101L, "A-A", 1, "职业道德A-A", null, new BigDecimal("0.05"), "theory"));
    ns.add(node(103L, 102L, "A-A-A", 2, "职业道德基本知识A-A-A", null, null, "theory"));
    ns.add(node(104L, 102L, "A-A-B", 2, "职业守则A-A-B", null, null, "theory"));
    // 两个 L2 各 8 理论叶（复现 2:3:3 / 5:1:2）
    long id = 200L;
    for (String imp : new String[]{"X", "X", "Y", "Y", "Y", "Z", "Z", "Z"}) {
      ns.add(node(id++, 103L, "A-A-A-" + id, 3, "叶" + id, imp, null, "theory"));
    }
    for (String imp : new String[]{"X", "X", "X", "X", "X", "Y", "Z", "Z"}) {
      ns.add(node(id++, 104L, "A-A-B-" + id, 3, "叶" + id, imp, null, "theory"));
    }
    ExportTree t = StandardExportSupport.assemble(ns);

    ExamStandardNodeEntity l1 = t.children(101L).get(0);
    Map<Long, BigDecimal> weights = StandardExportSupport.allocateL2Weights(t, l1);

    BigDecimal w1 = weights.get(103L);
    BigDecimal w2 = weights.get(104L);
    assertNotNull(w1);
    assertNotNull(w2);
    // 精确值各 0.025：最大余数法后一个 0.03、一个 0.02，合计严格等于 L1 权重
    assertEquals(0, w1.add(w2).compareTo(new BigDecimal("0.05")), "L2 权重合计必须等于 L1 权重");
    assertEquals(0, w1.compareTo(new BigDecimal("0.03")), "余数并列按顺序，首个 L2 补到 0.03");
    assertEquals(0, w2.compareTo(new BigDecimal("0.02")), "第二个 L2 为 0.02");
  }

  /** B8d：L2 手工权重固定，剩余权重按叶数比例分配（手工值参与合计核算） */
  @Test
  void b8d_l2WeightManualFixed() {
    List<ExamStandardNodeEntity> ns = new ArrayList<>();
    ns.add(node(101L, 0L, "A", 0, "基本要求A", null, new BigDecimal("0.30"), "theory"));
    ns.add(node(102L, 101L, "A-A", 1, "职业道德A-A", null, new BigDecimal("0.05"), "theory"));
    ns.add(node(103L, 102L, "A-A-A", 2, "职业道德基本知识A-A-A", null, null, "theory"));
    ns.add(node(104L, 102L, "A-A-B", 2, "职业守则A-A-B", null, new BigDecimal("0.03"), "theory"));
    long id = 200L;
    for (int i = 0; i < 8; i++) {
      ns.add(node(id++, 103L, "A-A-A-" + id, 3, "叶" + id, "X", null, "theory"));
    }
    for (int i = 0; i < 8; i++) {
      ns.add(node(id++, 104L, "A-A-B-" + id, 3, "叶" + id, "X", null, "theory"));
    }
    ExportTree t = StandardExportSupport.assemble(ns);

    ExamStandardNodeEntity l1 = t.children(101L).get(0);
    Map<Long, BigDecimal> weights = StandardExportSupport.allocateL2Weights(t, l1);

    assertEquals(0, weights.get(104L).compareTo(new BigDecimal("0.03")), "手工权重原样保留");
    assertEquals(0, weights.get(103L).compareTo(new BigDecimal("0.02")), "剩余 0.02 分给自动 L2");
    assertEquals(0, weights.get(103L).add(weights.get(104L)).compareTo(new BigDecimal("0.05")),
        "合计仍等于 L1 权重");
  }

  /** B7：SQL 导出——变量链连续、仅理论叶、文件头注释完整 */
  @Test
  void b7_sqlExport() {
    String sql = new KnowledgeSqlExporter().export("矿山救护工", "6-16-01-12", "2019",
        Map.of("0|默认", tree), "矿山救护工.docx",
        KnowledgeSqlExporter.SqlExportOptions.defaults("测试员"));

    assertTrue(sql.contains("INSERT INTO exam_work_type"), "含 work_type 插入");
    assertTrue(sql.contains("SET @wt_0 = LAST_INSERT_ID();"), "work_type id 变量");
    assertTrue(sql.contains("INSERT INTO exam_course"), "含 course 插入");
    assertTrue(sql.contains("矿山救护工初级"), "课程名");
    assertTrue(sql.contains("'6-16-01-12-1'"), "课程 series_no 带等级后缀");
    assertTrue(sql.contains("SET @c_0_"), "course id 变量");

    // 变量链：每个节点 INSERT 后 SET @k_*，子节点 parent 引用父变量
    assertTrue(sql.contains("SET @k_A = LAST_INSERT_ID();"));
    assertTrue(sql.contains("SET @k_A_A = LAST_INSERT_ID();"));
    assertTrue(sql.contains("SET @k_A_A_A_001 = LAST_INSERT_ID();"));
    assertTrue(sql.contains("parent_id, title") , "knowledge_structure 列");
    assertTrue(sql.contains(", @k_A_A, '职业道德基本知识A-A-A'"), "L2 parent 引用 L1 变量");
    assertTrue(sql.contains(", @k_A, '职业道德A-A'"), "L1 parent 引用 L0 变量");
    assertTrue(sql.contains("'Y'"), "important varchar 下发");
    assertTrue(sql.contains("10159"), "PHP 租户口径");
    assertTrue(sql.contains("10.00"), "amount 默认值");

    // 技能叶不入库
    assertFalse(sql.contains("检查风门损坏状况"), "不含 dimension=skill 节点");
    assertFalse(sql.contains("检查风险点'"), "技能叶不出现");

    // 文件头注释
    assertTrue(sql.contains("来源文件：矿山救护工.docx"));
    assertTrue(sql.contains("操作人：测试员"));
    assertTrue(sql.contains("生成时间："));
    assertTrue(sql.contains("行数统计"));
  }

  /** B7b：SQL 导出复用模式——不新建 work_type/course，按工种/等级 SELECT 定位 + DELETE 旧树 */
  @Test
  void b7_sqlExportReuseMode() {
    String sql = new KnowledgeSqlExporter().export("矿山救护工", "6-16-01-12", "2019",
        Map.of("0|默认", tree), "矿山救护工.docx",
        KnowledgeSqlExporter.SqlExportOptions.defaults("测试员",
            KnowledgeSqlExporter.SqlExportMode.REUSE));

    // 不新建 work_type / course
    assertFalse(sql.contains("INSERT INTO exam_work_type"), "复用模式不应生成 work_type 插入");
    assertFalse(sql.contains("INSERT INTO exam_course"), "复用模式不应生成 course 插入");

    // 按工种(title) 定位 work_type，按 level 定位 course
    assertTrue(sql.contains("SELECT id INTO @wt_") && sql.contains("FROM exam_work_type WHERE title='默认'"),
        "按工种名定位已存在 exam_work_type");
    assertTrue(sql.contains("FROM exam_course WHERE work_type_id=@wt_") && sql.contains("AND level=0"),
        "按 work_type + level 定位已存在 exam_course");

    // 每个 course 先清空旧树，再写新树（幂等可重复执行）
    assertTrue(sql.contains("DELETE FROM exam_knowledge_structure WHERE course_id = @c_0_"),
        "先删除该 course 下旧知识树");
    assertTrue(sql.contains("INSERT INTO exam_knowledge_structure"), "仍含新树插入");
    assertTrue(sql.contains("SET @k_A_A_A_001 = LAST_INSERT_ID();"), "变量链保留");

    // 文件头危险提示
    assertTrue(sql.contains("复用已有课程模式"), "文件头标注复用模式");
    assertTrue(sql.contains("执行前请备份目标库"), "危险提示");
    assertTrue(sql.contains("知识树入库 SQL"), "通用文件头");
  }

  /** B8a：理论细目表 Excel——10 列、表头结构、合并区域、权重数字格式 */
  @Test
  void b8_theoryExcel() throws Exception {
    byte[] bytes = new MubiaoExcelExporter().export(tree, "Sheet1",
        "矿山救护工(五级)理论知识考评要素细目表");
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      var sheet = wb.getSheet("Sheet1");
      assertNotNull(sheet);
      assertEquals("矿山救护工(五级)理论知识考评要素细目表", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals("考评范围", sheet.getRow(1).getCell(0).getStringCellValue());
      assertEquals("考评点", sheet.getRow(1).getCell(7).getStringCellValue());
      assertEquals("一级", sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals("三级", sheet.getRow(2).getCell(4).getStringCellValue());
      assertEquals("重要程度", sheet.getRow(2).getCell(9).getStringCellValue());
      assertEquals("相关知识", sheet.getRow(3).getCell(6).getStringCellValue());

      // 数据行：L0 A 合并、权重 0.25（0.00 格式）、L2 相关知识、叶 001/002
      String l0Text = sheet.getRow(4).getCell(0).getStringCellValue();
      assertTrue(l0Text.contains("基本要求") && l0Text.contains("（1:1:0）"), l0Text);
      assertEquals(0.25, sheet.getRow(4).getCell(1).getNumericCellValue(), 0.0001);
      assertEquals("0.00", sheet.getRow(4).getCell(1).getCellStyle().getDataFormatString());
      assertEquals("001", sheet.getRow(4).getCell(7).getStringCellValue());
      assertEquals("工匠精神的基本内涵", sheet.getRow(4).getCell(8).getStringCellValue());
      assertEquals("X", sheet.getRow(4).getCell(9).getStringCellValue());

      // 合并区域存在（A 列 L0 纵向合并 + 表头合并）
      boolean hasHeaderMerge = sheet.getMergedRegions().stream()
          .anyMatch(r -> r.getFirstRow() == 0 && r.getLastRow() == 0
              && r.getFirstColumn() == 0 && r.getLastColumn() == 9);
      assertTrue(hasHeaderMerge, "标题整行合并");
    }
  }

  /** B8b：技能细目表 Excel——13 列、必考/任选一项、仅首个二级标比重、试题量留空 */
  @Test
  void b8_skillExcel() throws Exception {
    byte[] bytes = new SkillMubiaoExcelExporter().export(tree,
        "矿山救护工（五级）操作技能考评要素细目表", "职业： 矿山救护工                级别： 五",
        l1 -> new BigDecimal("0.1500"));
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      var sheet = wb.getSheet("Sheet1");
      assertNotNull(sheet);
      assertEquals("矿山救护工（五级）操作技能考评要素细目表",
          sheet.getRow(0).getCell(0).getStringCellValue());
      assertTrue(sheet.getRow(1).getCell(0).getStringCellValue().contains("职业： 矿山救护工"));
      assertEquals("考评范围一级", sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals("考评范围二级", sheet.getRow(2).getCell(4).getStringCellValue());
      assertEquals("考评点", sheet.getRow(2).getCell(9).getStringCellValue());
      assertEquals("试题量", sheet.getRow(3).getCell(12).getStringCellValue());
      assertEquals(13, sheet.getRow(3).getLastCellNum(), "13 列");

      // 数据行（第 5 行起）：一级 必考 + 比重 0.15；二级 任选一项；技能要求原文；试题量空
      var dataRow = sheet.getRow(4);
      assertEquals("A(2:0:0)", dataRow.getCell(0).getStringCellValue());
      assertEquals("事故预防", dataRow.getCell(1).getStringCellValue());
      assertEquals(0.15, dataRow.getCell(2).getNumericCellValue(), 0.0001);
      assertEquals("必考", dataRow.getCell(3).getStringCellValue());
      assertEquals("A(2:0:0)", dataRow.getCell(4).getStringCellValue());
      assertEquals("安全检查", dataRow.getCell(5).getStringCellValue());
      assertTrue(dataRow.getCell(6).getStringCellValue().contains("1.1.1能查出风门损坏"));
      assertEquals(0.15, dataRow.getCell(7).getNumericCellValue(), 0.0001, "首个二级标出比重");
      assertEquals("任选一项", dataRow.getCell(8).getStringCellValue());
      assertEquals("001", dataRow.getCell(9).getStringCellValue());
      assertEquals("检查风门损坏状况", dataRow.getCell(10).getStringCellValue());
      assertEquals("X", dataRow.getCell(11).getStringCellValue());
      assertEquals("", dataRow.getCell(12).getStringCellValue(), "试题量列必须留空（拍板⑤）");

      // 尾行
      int last = sheet.getLastRowNum();
      assertEquals("填表人：", sheet.getRow(last).getCell(0).getStringCellValue());
    }
  }
}
