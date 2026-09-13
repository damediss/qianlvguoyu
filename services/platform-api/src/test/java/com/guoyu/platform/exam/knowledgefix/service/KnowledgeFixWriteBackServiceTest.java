package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.dto.ApplyResultVO;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 知识修正写回单测（KF-21/22/23）：仅 apply 行更新、course 预校验、回滚 */
class KnowledgeFixWriteBackServiceTest {

  private ExamDetailKnowledgeFixMapper fixMapper;
  private ExamExamDetailFixMapper queryMapper;
  private KnowledgeFixWriteBackService service;

  @BeforeEach
  void setUp() {
    fixMapper = mock(ExamDetailKnowledgeFixMapper.class);
    queryMapper = mock(ExamExamDetailFixMapper.class);
    service = new KnowledgeFixWriteBackService(fixMapper, queryMapper);
  }

  private ExamDetailKnowledgeFixEntity applyRow(int detailId, int oldId, int newId, int courseId) {
    ExamDetailKnowledgeFixEntity e = new ExamDetailKnowledgeFixEntity();
    e.setId((long) detailId);
    e.setDetailId(detailId);
    e.setOldKnowledgeId(oldId);
    e.setNewKnowledgeId(newId);
    e.setCourseId(courseId);
    e.setStatus("apply");
    return e;
  }

  private List<KnowledgeNodeRow> nodes() {
    KnowledgeNodeRow n1 = new KnowledgeNodeRow();
    n1.setId(301);
    n1.setCourseId(190);
    n1.setTitle("蒸发器跑碱处理");
    KnowledgeNodeRow n2 = new KnowledgeNodeRow();
    n2.setId(302);
    n2.setCourseId(190);
    n2.setTitle("蒸发站真空调整");
    return List.of(n1, n2);
  }

  /** KF-21：仅 apply 行逐行更新，备份先行，行数核对 */
  @Test
  void kf21_applyUpdatesOnlyApplyRows() {
    when(fixMapper.selectList(any())).thenReturn(List.of(
        applyRow(1, 4192, 301, 190), applyRow(2, 4192, 302, 190)));
    when(queryMapper.selectAllNodes()).thenReturn(nodes());
    when(queryMapper.createBackupTable(any())).thenReturn(1);
    when(queryMapper.backupRows(any(), any())).thenReturn(2);
    when(queryMapper.updateKnowledgeId(anyInt(), anyInt())).thenReturn(1);

    ApplyResultVO result = service.apply(10159L, 9L);

    verify(queryMapper).createBackupTable(result.backupTable());
    verify(queryMapper).backupRows(result.backupTable(), "exam_detail_knowledge_fix");
    verify(queryMapper, times(1)).updateKnowledgeId(1, 301);
    verify(queryMapper, times(1)).updateKnowledgeId(2, 302);
    verify(fixMapper).markAllApplied();
  }

  /** KF-22：course 预校验——新节点课程不一致 → 阻止写回 */
  @Test
  void kf22_courseMismatchBlocksWriteBack() {
    when(fixMapper.selectList(any())).thenReturn(List.of(applyRow(1, 4192, 301, 999)));
    when(queryMapper.selectAllNodes()).thenReturn(nodes());

    assertThrows(BusinessException.class, () -> service.apply(10159L, 9L));
    verify(queryMapper, never()).updateKnowledgeId(anyInt(), anyInt());
    verify(fixMapper, never()).markAllApplied();
  }

  /** KF-23：回滚委托备份表还原 knowledge_id */
  @Test
  void kf23_rollbackRestoresKnowledge() {
    when(queryMapper.rollbackKnowledge("exam_exam_detail_bak_kfix_20260912")).thenReturn(500);
    int restored = service.rollback("exam_exam_detail_bak_kfix_20260912");
    assertEquals(500, restored);
  }

  /** KF-23b：非法备份表名拒绝（防注入/误操作） */
  @Test
  void kf23b_illegalBackupTableNameRejected() {
    assertThrows(BusinessException.class, () -> service.rollback("some_table; drop database x"));
    verify(queryMapper, never()).rollbackKnowledge(any());
  }
}
