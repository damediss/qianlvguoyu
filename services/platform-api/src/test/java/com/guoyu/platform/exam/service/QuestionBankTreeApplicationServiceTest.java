package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.vo.QuestionBankTreeNodeVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 覆盖场景:
 * 1) 空租户 — 没有工种,直接返回 []
 * 2) 单工种单课程单知识点
 * 3) 多层级递归 (L0→L1→L2→L3) 题数正确累加
 * 4) 停用知识点过滤 (status=0 不进树)
 * 5) 逻辑删除过滤 (mapper 用 @TableLogic,这里通过 mock 不返回已删除行模拟)
 */
class QuestionBankTreeApplicationServiceTest {

  private final ExamWorkTypeMapper workTypeMapper = Mockito.mock(ExamWorkTypeMapper.class);
  private final ExamCourseMapper courseMapper = Mockito.mock(ExamCourseMapper.class);
  private final ExamKnowledgeStructureMapper knowledgeMapper =
      Mockito.mock(ExamKnowledgeStructureMapper.class);
  private final ExamExamDetailMapper examDetailMapper = Mockito.mock(ExamExamDetailMapper.class);

  private final QuestionBankTreeApplicationService service = new QuestionBankTreeApplicationService(
      workTypeMapper, courseMapper, knowledgeMapper, examDetailMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"),
        List.of("exam:question:view"),"asd@qq.com"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ── helper: 解析 wrapper 中 SQL 条件,模拟 MP 多租户/逻辑删除拦截器的行为
  // 这里单元测试场景下没有拦截器,我们 mock 的 mapper 直接拿到 wrapper,
  // 我们的 service 已经显式 .eq(tenantId),所以这里只用宽松 any() 匹配。
  // 关键: service 不应再拼 tenantId/deleted,而是依赖 MP 拦截器 (生产环境自动)。
  // 因此 mock 直接返回数据,不验证 wrapper 里的 SQL。

  // ────────────────────── Case 1: 空租户 ──────────────────────
  @Test
  @DisplayName("空租户: 没有工种时返回 []")
  void emptyTenant_returnsEmptyList() {
    when(workTypeMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

    List<QuestionBankTreeNodeVO> tree = service.buildTree();

    assertThat(tree).isNotNull().isEmpty();
  }

  // ────────────────────── Case 2: 单工种单课程单知识 ──────────────────────
  @Test
  @DisplayName("单工种→单课程→单L3 知识点: 1 道题,各层题数都=1")
  void singleWorkTypeCourseKnowledge_singleQuestion() {
    // 1 工种
    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(workType(10, "焊工", 0, 1)));

    // 1 课程
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(course(100, 10, "熔化焊", 2, 5, 1)));

    // 1 L3 知识点
    when(knowledgeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(knowledge(1000, 100, 0, "氩弧焊", 3, 1, 0)));

    // 1 道题（服务走 MySQL 端 GROUP BY：countByKnowledgeIdGrouped）
    when(examDetailMapper.countByKnowledgeIdGrouped(any()))
        .thenReturn(List.of(countRow(1000, 1)));

    List<QuestionBankTreeNodeVO> tree = service.buildTree();

    assertThat(tree).hasSize(1);
    QuestionBankTreeNodeVO wt = tree.get(0);
    assertThat(wt.getNodeType()).isEqualTo("workType");
    assertThat(wt.getKey()).isEqualTo("workType-10");
    assertThat(wt.getQuestionCount()).isEqualTo(1L);

    assertThat(wt.getChildren()).hasSize(1);
    QuestionBankTreeNodeVO c = wt.getChildren().get(0);
    assertThat(c.getNodeType()).isEqualTo("course");
    assertThat(c.getKey()).isEqualTo("course-100");
    assertThat(c.getQuestionCount()).isEqualTo(1L);

    assertThat(c.getChildren()).hasSize(1);
    QuestionBankTreeNodeVO k = c.getChildren().get(0);
    assertThat(k.getNodeType()).isEqualTo("knowledge");
    assertThat(k.getKey()).isEqualTo("ks-1000");
    assertThat(k.getLevel()).isEqualTo(3);
    assertThat(k.getQuestionCount()).isEqualTo(1L);
  }

  // ────────────────────── Case 3: 多层级递归题数 ──────────────────────
  @Test
  @DisplayName("L0→L1→L2→L3 4 级,题数从叶子递归累加到根")
  void multiLevelRecursiveCounts() {
    // 2 工种
    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(
            workType(1, "焊工", 0, 1),
            workType(2, "电工", 0, 1)));

    // 焊工→1 课程; 电工→1 课程
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(
            course(10, 1, "熔化焊", 2, 5, 1),
            course(20, 2, "低压电", 3, 5, 1)));

    // 焊工课程 10 下面:
    //   L0(101) → L1(102) → L2(103) → L3(104, 105)
    // 电工课程 20 下面:
    //   L0(201) → L3(202)
    when(knowledgeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(
            knowledge(101, 10, 0, "L0-焊", 0, 1, 0),
            knowledge(102, 10, 101, "L1-焊", 1, 1, 0),
            knowledge(103, 10, 102, "L2-焊", 2, 1, 0),
            knowledge(104, 10, 103, "L3-焊-A", 3, 1, 0),
            knowledge(105, 10, 103, "L3-焊-B", 3, 1, 0),
            knowledge(201, 20, 0, "L0-电", 0, 1, 0),
            knowledge(202, 20, 201, "L3-电-A", 3, 1, 0)));

    // 题: 焊课 L3-A 上 2 道; 焊课 L3-B 上 1 道; 电课 L3-电-A 上 3 道（GROUP BY 聚合行）
    when(examDetailMapper.countByKnowledgeIdGrouped(any()))
        .thenReturn(List.of(
            countRow(104, 2),
            countRow(105, 1),
            countRow(202, 3)));

    List<QuestionBankTreeNodeVO> tree = service.buildTree();
    assertThat(tree).hasSize(2);

    // ── 焊工
    QuestionBankTreeNodeVO wtHand = findById(tree, "workType-1");
    assertThat(wtHand).isNotNull();
    assertThat(wtHand.getQuestionCount()).isEqualTo(3L); // 2 + 1

    QuestionBankTreeNodeVO cHand = findById(wtHand.getChildren(), "course-10");
    assertThat(cHand.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO kL0 = findById(cHand.getChildren(), "ks-101");
    assertThat(kL0.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO kL1 = findById(kL0.getChildren(), "ks-102");
    assertThat(kL1.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO kL2 = findById(kL1.getChildren(), "ks-103");
    assertThat(kL2.getQuestionCount()).isEqualTo(3L);

    // L2 直接子节点是 2 个 L3
    assertThat(kL2.getChildren()).hasSize(2);
    QuestionBankTreeNodeVO kL3a = findById(kL2.getChildren(), "ks-104");
    QuestionBankTreeNodeVO kL3b = findById(kL2.getChildren(), "ks-105");
    assertThat(kL3a.getQuestionCount()).isEqualTo(2L);
    assertThat(kL3b.getQuestionCount()).isEqualTo(1L);
    assertThat(kL3a.getChildren()).isEmpty();
    assertThat(kL3b.getChildren()).isEmpty();

    // ── 电工
    QuestionBankTreeNodeVO wtDian = findById(tree, "workType-2");
    assertThat(wtDian.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO cDian = findById(wtDian.getChildren(), "course-20");
    assertThat(cDian.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO dL0 = findById(cDian.getChildren(), "ks-201");
    assertThat(dL0.getQuestionCount()).isEqualTo(3L);
    assertThat(dL0.getChildren()).hasSize(1);
    QuestionBankTreeNodeVO dL3 = findById(dL0.getChildren(), "ks-202");
    assertThat(dL3.getQuestionCount()).isEqualTo(3L);
  }

  // ────────────────────── Case 4: 停用知识点过滤 ──────────────────────
  @Test
  @DisplayName("status=0 知识点不出现在树中,其挂载的题不计入任何祖先")
  void disabledKnowledgeFilteredOut() {
    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(workType(1, "焊工", 0, 1)));
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(course(10, 1, "熔化焊", 2, 5, 1)));

    // status=1: 启用; status=0: 停用 — 停用的不应被 knowledgeMapper 返回
    // (我们 mock selectList,实际生产里 service 用 .eq(status, 1),这里直接
    //  只返回启用的即可,等价于过滤)
    when(knowledgeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(
            knowledge(101, 10, 0, "启用-A", 3, 1, 0),
            // 102 是停用的,在真实 SQL 会被 .eq(status, 1) 过滤掉,所以不返回
            knowledge(103, 10, 0, "启用-B", 3, 1, 0)));

    // 题目: 启用-A 上 2 道, 启用-B 上 1 道, 停用节点 102 上 5 道 (这些应被忽略)
    when(examDetailMapper.countByKnowledgeIdGrouped(any()))
        .thenReturn(List.of(
            countRow(101, 2),
            countRow(103, 1),
            countRow(102, 5)));

    List<QuestionBankTreeNodeVO> tree = service.buildTree();
    QuestionBankTreeNodeVO wt = tree.get(0);
    // 停用节点 102 不在树中,其题数不应被累加
    assertThat(wt.getQuestionCount()).isEqualTo(3L);

    QuestionBankTreeNodeVO c = wt.getChildren().get(0);
    assertThat(c.getQuestionCount()).isEqualTo(3L);
    assertThat(c.getChildren()).hasSize(2);
  }

  // ────────────────────── Case 5: 逻辑删除过滤 ──────────────────────
  @Test
  @DisplayName("逻辑删除: mapper 在生产环境由 @TableLogic 自动过滤 deleted=1 行;这里模拟同样效果")
  void logicallyDeletedQuestionsAreFiltered() {
    // 注解说明: exam_exam_detail.deleted 用 @TableLogic(value="0", delval="1")
    // 在生产环境 MyBatis-Plus TenantLineInnerInterceptor + TableLogic 拦截器
    // 会自动给 SQL 拼 deleted=0 条件。本测试通过让 mock mapper 不返回
    // logically-deleted 行来模拟同一行为,验证 service 不依赖业务层过滤。
    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(workType(1, "焊工", 0, 1)));
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(course(10, 1, "熔化焊", 2, 5, 1)));
    when(knowledgeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(knowledge(101, 10, 0, "L3", 3, 1, 0)));

    // 模拟拦截器已经把 deleted=1 的过滤掉,只剩 deleted=0 的 2 条（GROUP BY 聚合行）
    when(examDetailMapper.countByKnowledgeIdGrouped(any()))
        .thenReturn(List.of(countRow(101, 2)));

    List<QuestionBankTreeNodeVO> tree = service.buildTree();
    assertThat(tree.get(0).getQuestionCount()).isEqualTo(2L);
    assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getQuestionCount())
        .isEqualTo(2L);
  }

  // ────────────────────── 额外 Case: 课程下无知识点 ──────────────────────
  @Test
  @DisplayName("课程下没有知识点: course 出现但 children=[],questionCount=0")
  void courseWithNoKnowledge() {
    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(workType(1, "焊工", 0, 1)));
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(course(10, 1, "熔化焊", 2, 5, 1)));
    // 知识节点: 无
    when(knowledgeMapper.selectList(any(Wrapper.class)))
        .thenReturn(Collections.emptyList());
    when(examDetailMapper.countByKnowledgeIdGrouped(any()))
        .thenReturn(Collections.emptyList());

    List<QuestionBankTreeNodeVO> tree = service.buildTree();
    assertThat(tree).hasSize(1);
    assertThat(tree.get(0).getQuestionCount()).isEqualTo(0L);
    assertThat(tree.get(0).getChildren()).hasSize(1);
    assertThat(tree.get(0).getChildren().get(0).getChildren()).isEmpty();
    assertThat(tree.get(0).getChildren().get(0).getQuestionCount()).isEqualTo(0L);
  }

  // ────────────────────── 额外 Case: 多租户隔离 (service 不直接拼 tenantId) ──────────────────────
  @Test
  @DisplayName("多租户隔离: service 用 SecurityContext 取 tenantId,只查当前租户 (mock 不做断言,仅验证不抛)")
  void multiTenantIsolationUsesSecurityContext() {
    // 切换到另一个租户
    PlatformUserPrincipal tenant2 = new PlatformUserPrincipal(
        2000L, 2L, "user2", "用户2", "platform",
        List.of("PLATFORM_ADMIN"),
        List.of("exam:question:view"),"asd@qq.com"
    );
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(tenant2, "token", tenant2.getAuthorities()));

    when(workTypeMapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(workType(99, "租户2工种", 0, 1)));
    when(courseMapper.selectList(any(Wrapper.class)))
        .thenReturn(Collections.emptyList());

    List<QuestionBankTreeNodeVO> tree = service.buildTree();
    assertThat(tree).hasSize(1);
    assertThat(tree.get(0).getTitle()).isEqualTo("租户2工种");
  }

  // ────────────────────── helpers ──────────────────────
  private static ExamWorkTypeEntity workType(int id, String title, int parentId, int sort) {
    ExamWorkTypeEntity e = new ExamWorkTypeEntity();
    e.setId(id);
    e.setTitle(title);
    e.setParentId(parentId);
    e.setSortNumber(sort);
    e.setStatus(1);
    e.setTenantId(1);
    return e;
  }

  private static ExamCourseEntity course(int id, int workTypeId, String title, int level, int sort, int status) {
    ExamCourseEntity e = new ExamCourseEntity();
    e.setId(id);
    e.setWorkTypeId(workTypeId);
    e.setTitle(title);
    e.setLevel(level);
    e.setSortNumber(sort);
    e.setStatus(status);
    e.setTenantId(1L);
    return e;
  }

  private static ExamKnowledgeStructureEntity knowledge(
      int id, int courseId, int parentId, String title, int level, int status, int sort) {
    ExamKnowledgeStructureEntity e = new ExamKnowledgeStructureEntity();
    e.setId(id);
    e.setCourseId(courseId);
    e.setParentId(parentId);
    e.setTitle(title);
    e.setLevel(level);
    e.setStatus(status);
    e.setSortNumber(sort);
    e.setTenantId(1);
    return e;
  }

  /** GROUP BY 聚合行（对应 ExamExamDetailMapper.countByKnowledgeIdGrouped 返回结构） */
  private static java.util.Map<String, Object> countRow(int knowledgeId, long cnt) {
    java.util.Map<String, Object> row = new java.util.HashMap<>();
    row.put("knowledgeId", knowledgeId);
    row.put("cnt", cnt);
    return row;
  }

  private static QuestionBankTreeNodeVO findById(List<QuestionBankTreeNodeVO> list, String key) {
    return list.stream()
        .filter(n -> key.equals(n.getKey()))
        .findFirst()
        .orElse(null);
  }
}
