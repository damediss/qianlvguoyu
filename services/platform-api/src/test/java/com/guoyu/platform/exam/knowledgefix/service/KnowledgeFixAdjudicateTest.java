package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.knowledgefix.dto.AdjudicateResultVO;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 存疑行人工定夺状态机（方案 docs/04_requirements/存疑行人工定夺功能方案.md） */
class KnowledgeFixAdjudicateTest {

  private ExamDetailKnowledgeFixMapper fixMapper;
  private ExamKnowledgeStructureMapper structureMapper;
  private KnowledgeFixAdjudicateService service;

  @BeforeEach
  void setUp() {
    fixMapper = mock(ExamDetailKnowledgeFixMapper.class);
    structureMapper = mock(ExamKnowledgeStructureMapper.class);
    service = new KnowledgeFixAdjudicateService(fixMapper, structureMapper);
  }

  private ExamDetailKnowledgeFixEntity row(long id, String status, Integer oldNode,
      Integer newNode, int courseId) {
    ExamDetailKnowledgeFixEntity e = new ExamDetailKnowledgeFixEntity();
    e.setId(id);
    e.setStatus(status);
    e.setOldKnowledgeId(oldNode);
    e.setNewKnowledgeId(newNode);
    e.setCourseId(courseId);
    return e;
  }

  private ExamKnowledgeStructureEntity node(int id, int courseId) {
    ExamKnowledgeStructureEntity n = new ExamKnowledgeStructureEntity();
    n.setId(id);
    n.setCourseId(courseId);
    n.setTitle("节点" + id);
    return n;
  }

  @Test
  @DisplayName("单行：存疑行定夺为改挂成功（新节点存在且课程一致）")
  void adjudicateApplyOk() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(node(29236, 291));
    when(fixMapper.adjudicateRow(eq(1L), eq("apply"), eq(29236), eq(9L))).thenReturn(1);

    AdjudicateResultVO r = service.adjudicate(1L, "apply", 9L);

    assertEquals(1, r.succeeded());
    assertTrue(r.skipped().isEmpty());
    verify(fixMapper).adjudicateRow(1L, "apply", 29236, 9L);
  }

  @Test
  @DisplayName("单行：定夺为改挂但新节点已删 → 拒绝（防采纳死建议）")
  void adjudicateApplyNodeGone() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.adjudicate(1L, "apply", 9L));
    assertTrue(e.getMessage().contains("无法改挂"));
    verify(fixMapper, never()).adjudicateRow(anyLong(), anyString(), any(), anyLong());
  }

  @Test
  @DisplayName("单行：定夺为改挂但新节点越界（课程不一致）→ 拒绝")
  void adjudicateApplyCourseMismatch() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(node(29236, 999));

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.adjudicate(1L, "apply", 9L));
    assertTrue(e.getMessage().contains("越界"));
  }

  @Test
  @DisplayName("单行：非存疑行（已写回）不可定夺")
  void adjudicateNonFlagRejected() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "applied", 20135, 29236, 291));

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.adjudicate(1L, "apply", 9L));
    assertTrue(e.getMessage().contains("仅存疑行可定夺"));
  }

  @Test
  @DisplayName("单行：定夺为保留成功（new_knowledge_id 落原节点，与分类语义一致）")
  void adjudicateKeepOk() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(20135)).thenReturn(node(20135, 291));
    when(fixMapper.adjudicateRow(eq(1L), eq("keep"), eq(20135), eq(9L))).thenReturn(1);

    AdjudicateResultVO r = service.adjudicate(1L, "keep", 9L);

    assertEquals(1, r.succeeded());
    verify(fixMapper).adjudicateRow(1L, "keep", 20135, 9L);
  }

  @Test
  @DisplayName("单行：原节点已删的行拒绝保留（方案 §9 取舍 4：保留=挂死节点）")
  void adjudicateKeepOldNodeGone() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 25033, 29236, 291));
    when(structureMapper.selectById(25033)).thenReturn(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.adjudicate(1L, "keep", 9L));
    assertTrue(e.getMessage().contains("原知识点已删"));
  }

  @Test
  @DisplayName("单行：条件更新 affected=0（并发改走）→ 按冲突拒绝")
  void adjudicateConcurrentLost() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(node(29236, 291));
    when(fixMapper.adjudicateRow(anyLong(), anyString(), any(), anyLong())).thenReturn(0);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.adjudicate(1L, "apply", 9L));
    assertTrue(e.getMessage().contains("并发"));
  }

  @Test
  @DisplayName("单行：非法 action / 空 rowId → 400")
  void adjudicateBadRequest() {
    assertThrows(BusinessException.class, () -> service.adjudicate(1L, "delete", 9L));
    assertThrows(BusinessException.class, () -> service.adjudicate(null, "apply", 9L));
  }

  @Test
  @DisplayName("批量：混合状态行成功+跳过并带原因，不整批失败")
  void batchMixed() {
    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(fixMapper.selectById(2L)).thenReturn(row(2L, "applied", 1, 2, 291));
    when(fixMapper.selectById(3L)).thenReturn(row(3L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(node(29236, 291));
    when(fixMapper.adjudicateRow(eq(1L), eq("apply"), eq(29236), eq(9L))).thenReturn(1);
    when(fixMapper.adjudicateRow(eq(3L), eq("apply"), eq(29236), eq(9L))).thenReturn(1);

    AdjudicateResultVO r = service.batch(List.of(1L, 2L, 3L), "apply", 9L);

    assertEquals(2, r.succeeded());
    assertEquals(1, r.skipped().size());
    assertEquals(2L, r.skipped().get(0).rowId());
    assertTrue(r.skipped().get(0).reason().contains("仅存疑行可定夺"));
  }

  @Test
  @DisplayName("批量：超 200 行拒绝；重复 rowId 去重后只处理一次")
  void batchLimitAndDedupe() {
    List<Long> tooMany = new ArrayList<>();
    for (long i = 1; i <= 201; i++) {
      tooMany.add(i);
    }
    assertThrows(BusinessException.class, () -> service.batch(tooMany, "apply", 9L));

    when(fixMapper.selectById(1L)).thenReturn(row(1L, "flag", 20135, 29236, 291));
    when(structureMapper.selectById(29236)).thenReturn(node(29236, 291));
    when(fixMapper.adjudicateRow(eq(1L), eq("apply"), eq(29236), eq(9L))).thenReturn(1);

    AdjudicateResultVO r = service.batch(List.of(1L, 1L, 1L), "apply", 9L);
    assertEquals(1, r.succeeded());
    verify(fixMapper).adjudicateRow(anyLong(), anyString(), any(), anyLong());
  }
}
