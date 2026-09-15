package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.knowledgefix.dto.ApplyResultVO;
import com.guoyu.platform.exam.knowledgefix.dto.DedupStatsVO;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailDedupListMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** 去重清单生成与软删除执行单测（KF-05/06/07/08）；keeper 选择逻辑为 SQL 侧，另由 live-SQL 验证 */
class KnowledgeFixDedupServiceTest {

  private ExamExamDetailFixMapper queryMapper;
  private ExamDetailDedupListMapper dedupListMapper;
  private KnowledgeFixDedupService service;

  @BeforeEach
  void setUp() {
    queryMapper = mock(ExamExamDetailFixMapper.class);
    dedupListMapper = mock(ExamDetailDedupListMapper.class);
    service = new KnowledgeFixDedupService(queryMapper, dedupListMapper);
  }

  /** KF-06：生成流程顺序（清 pending → 生成 → 人工复核标记），可重复调用（幂等口径） */
  @Test
  void kf06_generateFlowIsIdempotentOrdered() {
    when(queryMapper.selectMayRowCount()).thenReturn(118205L);
    Map<String, Object> raw = new HashMap<>();
    raw.put("dup_groups", 51120L);
    raw.put("deleted_rows", 43192L);
    raw.put("delete_rows", 93689L);
    when(dedupListMapper.selectDedupStatsRaw()).thenReturn(raw);

    DedupStatsVO first = service.generate(9L);
    DedupStatsVO second = service.generate(9L);

    verify(dedupListMapper, times(2)).clearPending();
    verify(dedupListMapper, times(2)).generateDedupList();
    verify(dedupListMapper, times(2)).markManualReviewRows();
    assertEquals(118205L, first.mayTotalRows());
    assertEquals(51120L, first.dupGroups());
    assertEquals(93689L, first.deleteRows());
    assertEquals(43192L, first.alreadyDeletedRows());
    assertEquals(first, second);
  }

  /** KF-05：防御过滤——有人工复核标记的行不进入软删除执行（approve 只批 pending） */
  @Test
  void kf05_manualReviewRowsExcludedFromApply() {
    // approvePending 只更新 status='pending'（mock 返回 0 表示全部卡在 manual_review）
    when(dedupListMapper.approvePending()).thenReturn(0);

    ApplyResultVO result = service.applySoftDelete(10159L, 9L);

    assertEquals(0, result.approvedRows());
    verify(queryMapper, never()).createBackupTable(anyString());
    verify(queryMapper, never()).backupRows(anyString(), anyString());
    verify(dedupListMapper, never()).softDeleteByIds(any());
    verify(dedupListMapper, never()).markDedupDone();
  }

  /** KF-07：备份先行 → 游标分批软删 → 标记 done；行数逐批核对 */
  @Test
  void kf07_softDeleteBatchesWithBackupFirst() {
    when(dedupListMapper.approvePending()).thenReturn(1500);
    when(queryMapper.selectMayRowCount()).thenReturn(1500L);
    String backupTable = "exam_exam_detail_bak_dedup_" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    when(queryMapper.createBackupTable(anyString())).thenReturn(1);
    when(queryMapper.backupRows(anyString(), anyString())).thenReturn(1500);
    // 两页：第一页 1000（id 100..1099），第二页 500（id 1100..1599），第三页空
    List<Integer> page1 = IntStream.rangeClosed(100, 1099).boxed().collect(Collectors.toList());
    List<Integer> page2 = IntStream.rangeClosed(1100, 1599).boxed().collect(Collectors.toList());
    when(dedupListMapper.selectApprovedDeleteIds(0, 1000)).thenReturn(page1);
    when(dedupListMapper.selectApprovedDeleteIds(1099, 1000)).thenReturn(page2);
    when(dedupListMapper.selectApprovedDeleteIds(1599, 1000)).thenReturn(List.of());
    when(dedupListMapper.softDeleteByIds(any())).thenReturn(1000, 500);

    ApplyResultVO result = service.applySoftDelete(10159L, 9L);

    // 备份先于软删除
    InOrder inOrder = org.mockito.Mockito.inOrder(queryMapper, dedupListMapper);
    inOrder.verify(queryMapper).createBackupTable(backupTable);
    inOrder.verify(queryMapper).backupRows(backupTable, "exam_detail_dedup_list");
    inOrder.verify(dedupListMapper).softDeleteByIds(page1);
    inOrder.verify(dedupListMapper).softDeleteByIds(page2);
    inOrder.verify(dedupListMapper).markDedupDone();
    assertEquals(1500, result.affectedRows());
    assertEquals(backupTable, result.backupTable());
  }

  /** KF-07 补充：无待执行清单时不执行任何写操作 */
  @Test
  void kf07b_applyWithNothingPendingIsNoop() {
    when(dedupListMapper.approvePending()).thenReturn(0);
    ApplyResultVO result = service.applySoftDelete(10159L, 9L);
    assertEquals(0, result.affectedRows());
    verify(queryMapper, never()).createBackupTable(anyString());
  }

  /** KF-08：回滚委托备份表还原 */
  @Test
  void kf08_rollbackRestoresFromBackup() {
    when(queryMapper.rollbackSoftDelete("exam_exam_detail_bak_dedup_20260912")).thenReturn(93689); // 历史表名样例
    int restored = service.rollbackSoftDelete("exam_exam_detail_bak_dedup_20260912");
    assertEquals(93689, restored);
    verify(dedupListMapper).markDedupRolledBack();
  }
}
