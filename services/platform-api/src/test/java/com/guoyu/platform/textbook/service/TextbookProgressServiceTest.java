package com.guoyu.platform.textbook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 章进度重算（方案 §12.1-TextbookProgressServiceTest）：
 * 各状态组合的 task_status 优先级、submittedCount 驱动审定队列、进度刷新不递增 assignment_version。
 */
class TextbookProgressServiceTest {

  private static final long TENANT = 10159L;
  private static final long CHAPTER = 9001L;

  private TextbookOutlineNodeMapper nodeMapper;
  private TextbookSectionContentMapper contentMapper;
  private TextbookChapterAssignmentMapper assignmentMapper;
  private TextbookProgressService service;

  private Map<Long, TextbookOutlineNodeEntity> nodesDb;
  private Map<Long, TextbookSectionContentEntity> contentDb;

  @BeforeAll
  static void initMybatisPlusCache() {
    org.apache.ibatis.builder.MapperBuilderAssistant assistant =
        new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, TextbookOutlineNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookSectionContentEntity.class);
    TableInfoHelper.initTableInfo(assistant, TextbookChapterAssignmentEntity.class);
  }

  @BeforeEach
  void setUp() {
    nodeMapper = mock(TextbookOutlineNodeMapper.class);
    contentMapper = mock(TextbookSectionContentMapper.class);
    assignmentMapper = mock(TextbookChapterAssignmentMapper.class);
    service = new TextbookProgressService(nodeMapper, contentMapper, assignmentMapper);
    nodesDb = new HashMap<>();
    contentDb = new HashMap<>();

    when(nodeMapper.selectList(any())).thenAnswer(inv -> nodesDb.values().stream()
        .filter(n -> matches(n, eqParams(inv.getArgument(0)))).toList());
    when(contentMapper.selectList(any())).thenAnswer(inv -> contentDb.values().stream()
        .filter(c -> inList(c.getSectionId(), eqInParams(inv.getArgument(0))))
        .toList());
    when(assignmentMapper.selectOne(any())).thenAnswer(inv -> assignmentRow);
    when(assignmentMapper.update(any(), any())).thenReturn(1);
  }

  private TextbookChapterAssignmentEntity assignmentRow;

  @BeforeEach
  void initAssignmentRow() {
    assignmentRow = new TextbookChapterAssignmentEntity();
    assignmentRow.setId(7001L);
    assignmentRow.setChapterId(CHAPTER);
    assignmentRow.setActive(1);
  }

  private void section(long id, String title, int sort) {
    TextbookOutlineNodeEntity node = new TextbookOutlineNodeEntity();
    node.setId(id);
    node.setTenantId(TENANT);
    node.setProjectId(1L);
    node.setParentId(CHAPTER);
    node.setNodeLevel(3);
    node.setTitle(title);
    node.setSortNo(sort);
    nodesDb.put(id, node);
  }

  private void content(long sectionId, String status, String md) {
    TextbookSectionContentEntity content = new TextbookSectionContentEntity();
    content.setId(IdWorker.getId());
    content.setSectionId(sectionId);
    content.setProjectId(1L);
    content.setChapterId(CHAPTER);
    content.setContentStatus(status);
    content.setContentMd(md == null ? "" : md);
    content.setWordCount(md == null ? 0 : md.length());
    contentDb.put(sectionId, content);
  }

  private Map<String, Object> capturedUpdateSets;

  private void refreshAndCapture() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<LambdaUpdateWrapper<TextbookChapterAssignmentEntity>> captor =
        ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    service.refreshChapterSummary(CHAPTER);
    verify(assignmentMapper).update(any(), captor.capture());
    String sqlSet = captor.getValue().getSqlSet();
    Map<String, Object> pairs = captor.getValue().getParamNameValuePairs();
    Map<String, Object> sets = new LinkedHashMap<>();
    Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}")
        .matcher(sqlSet);
    while (m.find()) {
      sets.put(m.group(1), pairs.get(m.group(2)));
    }
    capturedUpdateSets = sets;
  }

  @Test
  @DisplayName("未开始（全部节无正文）→ assigned；缓存各计数与 untouched 口径")
  void untouchedChapterIsAssigned() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_ASSIGNED, capturedUpdateSets.get("task_status"));
    assertEquals(2, capturedUpdateSets.get("section_count"));
    assertEquals(0, capturedUpdateSets.get("approved_count"));
    assertEquals(0, capturedUpdateSets.get("submitted_count"));
    assertEquals(2, service.untouchedCount(CHAPTER), "untouched = 全部节 - 有正文记录节");
  }

  @Test
  @DisplayName("部分草稿 → writing")
  void partialDraftIsWriting() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    content(9101L, TextbookSectionContentApplicationService.STATUS_DRAFT, "开始写了");
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_WRITING, capturedUpdateSets.get("task_status"));
    assertEquals(1, capturedUpdateSets.get("draft_count"));
  }

  @Test
  @DisplayName("部分提交+部分草稿 = writing（不是 reviewing）；submittedCount>0 驱动审定队列")
  void partialSubmittedPartialDraftIsWritingWithSubmittedCount() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    content(9101L, TextbookSectionContentApplicationService.STATUS_SUBMITTED, "已提交");
    content(9102L, TextbookSectionContentApplicationService.STATUS_DRAFT, "还在写");
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_WRITING, capturedUpdateSets.get("task_status"),
        "部分节已提交、部分节仍在写整体显示 writing（§3.3）");
    assertEquals(1, capturedUpdateSets.get("submitted_count"),
        "submitted_count>0 是审定队列条件，不只看 task_status");
  }

  @Test
  @DisplayName("全部提交（无草稿）→ reviewing")
  void allSubmittedIsReviewing() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    content(9101L, TextbookSectionContentApplicationService.STATUS_SUBMITTED, "a");
    content(9102L, TextbookSectionContentApplicationService.STATUS_SUBMITTED, "b");
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_REVIEWING, capturedUpdateSets.get("task_status"));
  }

  @Test
  @DisplayName("有打回 → returned（优先于 reviewing/writing）")
  void anyReturnedIsReturned() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    content(9101L, TextbookSectionContentApplicationService.STATUS_RETURNED, "被打回");
    content(9102L, TextbookSectionContentApplicationService.STATUS_SUBMITTED, "已提交");
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_RETURNED, capturedUpdateSets.get("task_status"));
    assertEquals(1, capturedUpdateSets.get("returned_count"));
  }

  @Test
  @DisplayName("全通过 → approved")
  void allApprovedIsApproved() {
    section(9101L, "节一", 1);
    section(9102L, "节二", 2);
    content(9101L, TextbookSectionContentApplicationService.STATUS_APPROVED, "a");
    content(9102L, TextbookSectionContentApplicationService.STATUS_APPROVED, "b");
    refreshAndCapture();
    assertEquals(TextbookProgressService.TASK_APPROVED, capturedUpdateSets.get("task_status"));
    assertEquals(2, capturedUpdateSets.get("approved_count"));
  }

  @Test
  @DisplayName("进度刷新不递增 assignment_version（set 子句不含该列）")
  void refreshDoesNotTouchAssignmentVersion() {
    section(9101L, "节一", 1);
    content(9101L, TextbookSectionContentApplicationService.STATUS_DRAFT, "x");
    refreshAndCapture();
    assertFalse(capturedUpdateSets.containsKey("assignment_version"),
        "assignment_version 仅改派/取消递增，进度刷新不递增（§5.1）");
    assertFalse(capturedUpdateSets.containsKey("active"));
  }

  @Test
  @DisplayName("computeTaskStatus 纯函数优先级：零节兜底 assigned")
  void computeTaskStatusPriority() {
    assertEquals(TextbookProgressService.TASK_ASSIGNED,
        TextbookProgressService.computeTaskStatus(0, 0, 0, 0, 0));
    assertEquals(TextbookProgressService.TASK_APPROVED,
        TextbookProgressService.computeTaskStatus(2, 2, 0, 0, 2));
    assertEquals(TextbookProgressService.TASK_RETURNED,
        TextbookProgressService.computeTaskStatus(2, 1, 1, 1, 2));
    assertEquals(TextbookProgressService.TASK_REVIEWING,
        TextbookProgressService.computeTaskStatus(2, 1, 1, 0, 2));
    assertEquals(TextbookProgressService.TASK_WRITING,
        TextbookProgressService.computeTaskStatus(2, 0, 1, 0, 1));
    assertEquals(TextbookProgressService.TASK_ASSIGNED,
        TextbookProgressService.computeTaskStatus(2, 0, 0, 0, 0));
  }

  @Test
  @DisplayName("统计卡口径：相交不要求相加等于总数")
  void summaryCountsOverlap() {
    TextbookAssignmentApplicationService assignmentService =
        new TextbookAssignmentApplicationService(null, null, assignmentMapper, null, null, null,
            null, null, null, null, null, null);
    TextbookChapterAssignmentEntity row = new TextbookChapterAssignmentEntity();
    row.setTaskStatus(TextbookProgressService.TASK_RETURNED);
    row.setSubmittedCount(1);
    row.setReturnedCount(1);
    row.setApprovedCount(0);
    var writing = assignmentService.writingSummary(List.of(row));
    assertEquals(1, writing.returnedCount());
    assertEquals(1, writing.pendingReviewCount(), "同章可同时进两个口径，不求和等于总数");
    var review = assignmentService.reviewSummary(List.of(row));
    assertEquals(1, review.pendingCount());
    assertEquals(1, review.returnedCount());
    assertEquals(0, review.partialApprovedCount());
  }

  // ============================================================
  // 假库工具（与一期 TextbookOutlineApplicationServiceTest 同范式）
  // ============================================================

  private static Map<String, Object> eqParams(Wrapper<?> wrapper) {
    AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
    String segment = aw.getSqlSegment();
    Map<String, Object> pairs = aw.getParamNameValuePairs();
    Map<String, Object> out = new LinkedHashMap<>();
    Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}")
        .matcher(segment);
    while (m.find()) {
      out.put(m.group(1), pairs.get(m.group(2)));
    }
    return out;
  }

  private static List<Object> eqInParams(Wrapper<?> wrapper) {
    AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
    Map<String, Object> pairs = aw.getParamNameValuePairs();
    return List.copyOf(pairs.values());
  }

  private static boolean inList(Long value, List<Object> candidates) {
    if (!candidates.isEmpty()) {
      for (Object candidate : candidates) {
        if (Objects.equals(candidate, value)) {
          return true;
        }
      }
      return false;
    }
    return true;
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
}
