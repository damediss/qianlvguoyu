package com.guoyu.platform.textbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import com.guoyu.platform.textbook.dto.OutlineNodeUpdateReq;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.service.TextbookOutlineApplicationService;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 大纲编排服务（设计 §4.1-TextbookOutlineApplicationServiceTest，Mockito + 内存假库）。
 */
class TextbookOutlineApplicationServiceTest {

  private static final long TENANT = 10159L;

  private TextbookOutlineNodeMapper nodeMapper;
  private TextbookProjectMapper projectMapper;
  private TextbookOutlineApplicationService service;
  private com.guoyu.platform.textbook.service.TextbookProjectLockService lockService;
  private com.guoyu.platform.textbook.service.TextbookStructureGuard structureGuard;
  private org.mockito.MockedStatic<Db> dbMock;

  private Map<Long, TextbookOutlineNodeEntity> nodesDb;
  private Map<Long, TextbookProjectEntity> projectsDb;

  /** LambdaQueryWrapper 列解析依赖 MP 实体元数据缓存，纯 Mockito 环境需手工初始化 */
  @org.junit.jupiter.api.BeforeAll
  static void initMybatisPlusCache() {
    org.apache.ibatis.builder.MapperBuilderAssistant assistant =
        new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TextbookOutlineNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookProjectEntity.class);
  }

  @BeforeEach
  void setUp() {
    nodeMapper = mock(TextbookOutlineNodeMapper.class);
    projectMapper = mock(TextbookProjectMapper.class);
    lockService = mock(com.guoyu.platform.textbook.service.TextbookProjectLockService.class);
    structureGuard = mock(com.guoyu.platform.textbook.service.TextbookStructureGuard.class);
    when(lockService.lockProject(any())).thenAnswer(
        inv -> projectsDb.get(((Number) inv.getArgument(0)).longValue()));
    service = new TextbookOutlineApplicationService(nodeMapper, projectMapper,
        new ObjectMapper(), lockService, structureGuard);
    nodesDb = new HashMap<>();
    projectsDb = new HashMap<>();

    when(nodeMapper.selectOne(any())).thenAnswer(inv -> nodesDb.values().stream()
        .filter(n -> matches(n, eqParams(inv.getArgument(0)))).findFirst().orElse(null));
    when(projectMapper.selectOne(any())).thenAnswer(inv -> projectsDb.values().stream()
        .filter(p -> matchesProject(p, eqParams(inv.getArgument(0)))).findFirst().orElse(null));
    when(nodeMapper.selectList(any())).thenAnswer(inv -> nodesDb.values().stream()
        .filter(n -> matches(n, eqParams(inv.getArgument(0)))).toList());
    when(nodeMapper.insert(any(TextbookOutlineNodeEntity.class))).thenAnswer(inv -> {
      TextbookOutlineNodeEntity n = inv.getArgument(0);
      if (n.getId() == null) {
        n.setId(IdWorker.getId());
      }
      nodesDb.put(n.getId(), n);
      return 1;
    });
    when(nodeMapper.updateById(any(TextbookOutlineNodeEntity.class))).thenAnswer(inv -> {
      TextbookOutlineNodeEntity n = inv.getArgument(0);
      nodesDb.put(n.getId(), n);
      return 1;
    });
    when(nodeMapper.deleteBatchIds(any())).thenAnswer(inv -> {
      Collection<?> ids = inv.getArgument(0);
      ids.forEach(id -> nodesDb.computeIfPresent((Long) id,
          (k, v) -> {
            v.setDeleted(1);
            return v;
          }));
      return ids.size();
    });
    // regenerate 走 Db.saveBatch 批量插入：静态打桩落到假库（纯 Mockito 无 SqlSessionFactory）
    dbMock = org.mockito.Mockito.mockStatic(Db.class);
    dbMock.when(() -> Db.saveBatch(any(java.util.Collection.class), anyInt())).thenAnswer(inv -> {
      Collection<TextbookOutlineNodeEntity> entities = inv.getArgument(0);
      entities.forEach(n -> nodesDb.put(n.getId(), n));
      return true;
    });
  }

  @org.junit.jupiter.api.AfterEach
  void tearDownMock() {
    if (dbMock != null) {
      dbMock.close();
    }
  }

  /** ① 改字数置 words_manual=1，并返回合计校验 */
  @Test
  void updateWordsMarksManualAndReturnsSummary() {
    TextbookProjectEntity project = project(1L, 10000);
    TextbookOutlineNodeEntity chapter = node(11L, 1L, 0L, 2, "职业道德");
    chapter.setTargetWords(5000);
    nodesDb.put(chapter.getId(), chapter);
    projectsDb.put(project.getId(), project);

    OutlineSummaryVO summary = service.updateNode(11L,
        new OutlineNodeUpdateReq("职业道德（重命名）", 1234, null));;

    TextbookOutlineNodeEntity updated = nodesDb.get(11L);
    assertEquals("职业道德（重命名）", updated.getTitle());
    assertEquals(1234, updated.getTargetWords());
    assertEquals(1, updated.getWordsManual());
    assertEquals(1234, summary.chapterSum());
    assertEquals(10000, summary.totalWords());
    assertTrue(!summary.balanced());
  }

  /** ② 重建保留人工章字数，合计仍守恒（6000 = 人工 4000 + 自动分盘 2000） */
  @Test
  void rebuildPreservesManualChapterAndConservesTotal() {
    TextbookProjectEntity project = project(1L, 6000);
    project.setDirection("铝电解槽操作工");
    project.setLevelCode(5);
    project.setFunctionNosJson("[1,2]");
    projectsDb.put(project.getId(), project);

    TextbookOutlineNodeEntity manualChapter = node(101L, 1L, 900L, 2, "铝电解槽操作");
    manualChapter.setSourceNo("2");
    manualChapter.setWordsManual(1);
    manualChapter.setTargetWords(4000);
    TextbookOutlineNodeEntity oldSection = node(102L, 1L, 101L, 3, "旧节");
    oldSection.setSourceNo("2.1");
    TextbookOutlineNodeEntity oldPoint = node(103L, 1L, 102L, 4, "旧点");
    nodesDb.put(101L, manualChapter);
    nodesDb.put(102L, oldSection);
    nodesDb.put(103L, oldPoint);

    List<String> warnings =
        service.regenerate(project, syntheticStandard(), true).warnings();

    long chapterSum = nodesDb.values().stream()
        .filter(n -> n.getDeleted() == null || n.getDeleted() == 0)
        .filter(n -> n.getNodeLevel() == 2)
        .mapToLong(n -> n.getTargetWords() == null ? 0 : n.getTargetWords())
        .sum();
    assertEquals(6000, chapterSum, "重建后章合计必须仍 = 总字数；" + warnings);

    // 人工章原样保留（id/字数不动），旧自动节/点已删
    assertEquals(4000, nodesDb.get(101L).getTargetWords());
    assertEquals(1, nodesDb.get(101L).getWordsManual());
    assertEquals(1, nodesDb.get(102L).getDeleted());
    assertEquals(1, nodesDb.get(103L).getDeleted());

    // 自动章 工作准备 分到 2000（6000-4000），其节按比例拿到全部
    TextbookOutlineNodeEntity autoChapter = nodesDb.values().stream()
        .filter(n -> n.getNodeLevel() == 2 && "工作准备".equals(n.getTitle()))
        .findFirst().orElseThrow();
    assertEquals(2000, autoChapter.getTargetWords());
    // 人工章下节重建（预算 4000；排除已软删旧节）
    TextbookOutlineNodeEntity newSection = nodesDb.values().stream()
        .filter(n -> (n.getDeleted() == null || n.getDeleted() == 0))
        .filter(n -> n.getNodeLevel() == 3 && Objects.equals(n.getParentId(), 101L))
        .findFirst().orElseThrow();
    assertEquals(4000, newSection.getTargetWords());
    assertEquals("2.1", newSection.getSourceNo());
  }

  /** ③（二期 §5.7）regenerate 返回保留/移除章清单：分配行据此保留 active=0 行/软删，防唯一键冲突 */
  @Test
  void regenerateReportsRetainedAndRemovedChapters() {
    TextbookProjectEntity project = project(1L, 6000);
    project.setDirection("铝电解槽操作工");
    project.setLevelCode(5);
    project.setFunctionNosJson("[1,2]");
    projectsDb.put(project.getId(), project);
    TextbookOutlineNodeEntity manualChapter = node(101L, 1L, 900L, 2, "铝电解槽操作");
    manualChapter.setSourceNo("2");
    manualChapter.setWordsManual(1);
    manualChapter.setTargetWords(4000);
    TextbookOutlineNodeEntity autoChapter = node(900L, 1L, 0L, 2, "工作准备");
    autoChapter.setSourceNo("1");
    autoChapter.setWordsManual(0);
    autoChapter.setTargetWords(2000);
    nodesDb.put(101L, manualChapter);
    nodesDb.put(900L, autoChapter);

    var result = service.regenerate(project, syntheticStandard(), true);

    assertTrue(result.retainedChapterIds().contains(101L), "人工调整章应保留原 ID");
    assertTrue(result.removedChapterIds().contains(900L), "自动章被移除应上报（其分配行须软删）");
    assertEquals(1, result.retainedChapterIds().size());
    assertEquals(1, result.removedChapterIds().size());
  }

  /** ③ 删除节点级联软删子孙 */
  @Test
  void deleteNodeCascadesToDescendants() {
    nodesDb.put(201L, node(201L, 2L, 0L, 1, "工作要求"));
    nodesDb.put(202L, node(202L, 2L, 201L, 2, "章"));
    nodesDb.put(203L, node(203L, 2L, 202L, 3, "节"));
    nodesDb.put(204L, node(204L, 2L, 0L, 1, "基本要求"));

    service.deleteNode(201L);

    assertEquals(1, nodesDb.get(201L).getDeleted());
    assertEquals(1, nodesDb.get(202L).getDeleted());
    assertEquals(1, nodesDb.get(203L).getDeleted());
    assertEquals(0, nodesDb.get(204L).getDeleted(), "兄弟子树不应被删");
  }

  /** ④ sort_no 上移/下移交换正确；顶端再上移不动 */
  @Test
  void moveNodeSwapsSortNoWithAdjacentSibling() {
    nodesDb.put(301L, section(301L, 3L, 300L, "一", 1));
    nodesDb.put(302L, section(302L, 3L, 300L, "二", 2));
    nodesDb.put(303L, section(303L, 3L, 300L, "三", 3));

    service.moveNode(303L, true);
    assertEquals(2, nodesDb.get(303L).getSortNo());
    assertEquals(3, nodesDb.get(302L).getSortNo());
    assertEquals(1, nodesDb.get(301L).getSortNo());

    service.moveNode(301L, true); // 已在顶端
    assertEquals(1, nodesDb.get(301L).getSortNo());
    assertEquals(2, nodesDb.get(303L).getSortNo());
  }

  // ============================================================
  // 假库支撑
  // ============================================================

  /** 从 wrapper 提取等值条件（仅测试用，依赖 MP 公开 API getSqlSegment/getParamNameValuePairs） */
  private static Map<String, Object> eqParams(Wrapper<?> wrapper) {
    AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
    String segment = aw.getSqlSegment();
    Map<String, Object> pairs = aw.getParamNameValuePairs();
    Map<String, Object> out = new LinkedHashMap<>();
    Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}")
        .matcher(segment);
    while (m.find()) {
      out.put(m.group(1), pairs.get(m.group(2)));
    }
    return out;
  }

  private static boolean matches(TextbookOutlineNodeEntity n, Map<String, Object> eq) {
    for (Map.Entry<String, Object> e : eq.entrySet()) {
      boolean ok = switch (e.getKey()) {
        case "id" -> Objects.equals(n.getId(), e.getValue());
        case "tenant_id" -> Objects.equals(n.getTenantId(), e.getValue());
        case "parent_id" -> Objects.equals(n.getParentId(), e.getValue());
        case "project_id" -> Objects.equals(n.getProjectId(), e.getValue());
        case "node_level" -> Objects.equals(n.getNodeLevel(), e.getValue());
        default -> true;
      };
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  private static boolean matchesProject(TextbookProjectEntity p, Map<String, Object> eq) {
    for (Map.Entry<String, Object> e : eq.entrySet()) {
      boolean ok = switch (e.getKey()) {
        case "id" -> Objects.equals(p.getId(), e.getValue());
        case "tenant_id" -> Objects.equals(p.getTenantId(), e.getValue());
        default -> true;
      };
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  private static TextbookProjectEntity project(long id, int totalWords) {
    TextbookProjectEntity p = new TextbookProjectEntity();
    p.setId(id);
    p.setTenantId(TENANT);
    p.setTotalWords(totalWords);
    p.setStatus("outlined");
    return p;
  }

  private static TextbookOutlineNodeEntity node(long id, long projectId, long parentId,
      int level, String title) {
    TextbookOutlineNodeEntity n = new TextbookOutlineNodeEntity();
    n.setId(id);
    n.setTenantId(TENANT);
    n.setProjectId(projectId);
    n.setParentId(parentId);
    n.setNodeLevel(level);
    n.setTitle(title);
    n.setTitleRaw(title);
    n.setWordsManual(0);
    n.setSortNo(1);
    n.setDeleted(0);
    return n;
  }

  private static TextbookOutlineNodeEntity section(long id, long projectId, long parentId,
      String title, int sortNo) {
    TextbookOutlineNodeEntity n = node(id, projectId, parentId, 3, title);
    n.setSortNo(sortNo);
    return n;
  }

  /** 合成最小标准：五级两个职业功能（工作准备/铝电解槽操作），理论权重 10/50 */
  private static ParsedStandard syntheticStandard() {
    ParsedStandard parsed = new ParsedStandard();
    StandardLevelDoc level = new StandardLevelDoc();
    level.setLevelCode(0);
    level.setLevelName("五级/初级工");
    level.getWorkFunctions().add(function("1", "工作准备",
        l2("1.1", "交接班", List.of("安全操作规程", "岗位工作任务"), List.of("能确认现场安全"))));
    level.getWorkFunctions().add(function("2", "铝电解槽操作",
        l2("2.1", "更换阳极", List.of("换极表知识", "残极检查方法"), List.of("能确认换极槽号"))));
    parsed.getLevels().add(level);

    StandardWeightTable theory = new StandardWeightTable();
    theory.setKind(StandardWeightTable.Kind.THEORY);
    theory.getRows().add(weightRow("工作准备", "10"));
    theory.getRows().add(weightRow("铝电解槽操作", "50"));
    parsed.setTheoryWeights(theory);
    return parsed;
  }

  private static StandardNodeDoc function(String no, String title, StandardNodeDoc... children) {
    StandardNodeDoc f = new StandardNodeDoc();
    f.setLevel(1);
    f.setNumber(no);
    f.setTitle(title);
    for (StandardNodeDoc c : children) {
      f.getChildren().add(c);
    }
    return f;
  }

  private static StandardNodeDoc l2(String no, String title, List<String> theory,
      List<String> skill) {
    StandardNodeDoc n = new StandardNodeDoc();
    n.setLevel(2);
    n.setNumber(no);
    n.setTitle(title);
    n.getTheoryMaterials().addAll(theory);
    n.getSkillMaterials().addAll(skill);
    return n;
  }

  private static StandardWeightRow weightRow(String item, String valueAtDefaultLevel5) {
    StandardWeightRow row = new StandardWeightRow();
    row.setItem(item);
    row.getValues().put(StandardWeightRow.keyOf(0, "默认"),
        new BigDecimal(valueAtDefaultLevel5));
    return row;
  }
}
