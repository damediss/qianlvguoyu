package com.guoyu.platform.exam.knowledgefix.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.knowledgefix.dto.ApplyResultVO;
import com.guoyu.platform.exam.knowledgefix.dto.DedupStatsVO;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailDedupListMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 去重清单生成与软删除执行（方案 §6.3 / Phase 1）。
 * 清单生成 = 单条 INSERT...SELECT（服务端集合运算）；软删除 = 备份表 + 分批 deleted=1，可整体回滚。
 */
@Service
@Slf4j
public class KnowledgeFixDedupService {

  private static final int BATCH_SIZE = 1000;
  /** 备份表名守卫：表名会拼进 SQL（${backupTable}），禁止任意标识符直通 */
  private static final Pattern DEDUP_BACKUP_TABLE_PATTERN =
      Pattern.compile("exam_exam_detail_bak_dedup_\\d{8}");

  private final ExamExamDetailFixMapper queryMapper;
  private final ExamDetailDedupListMapper dedupListMapper;

  public KnowledgeFixDedupService(ExamExamDetailFixMapper queryMapper,
      ExamDetailDedupListMapper dedupListMapper) {
    this.queryMapper = queryMapper;
    this.dedupListMapper = dedupListMapper;
  }

  /** 生成/刷新去重清单（幂等：仅重置 pending；已执行清单不动） */
  public DedupStatsVO generate(Long userId) {
    dedupListMapper.clearPending();
    int inserted = dedupListMapper.generateDedupList();
    int manual = dedupListMapper.markManualReviewRows();
    DedupStatsVO stats = stats();
    log.info("去重清单生成完成：inserted={}, manual_review={}, 统计={}", inserted, manual, stats);
    return stats;
  }

  public DedupStatsVO stats() {
    long mayTotal = queryMapper.selectMayRowCount();
    var raw = dedupListMapper.selectDedupStatsRaw();
    long dupGroups = ((Number) raw.getOrDefault("dup_groups", 0)).longValue();
    long deleteRows = ((Number) raw.getOrDefault("delete_rows", 0)).longValue();
    long deletedRows = ((Number) raw.getOrDefault("deleted_rows", 0)).longValue();
    long manualRows = dedupListMapper.selectManualReviewCount();
    return new DedupStatsVO(mayTotal, dupGroups, deleteRows, deletedRows, manualRows);
  }

  /** 软删除执行：清单 pending → approved → 备份 → 分批 deleted=1 → done（方案 §6.3） */
  public ApplyResultVO applySoftDelete(Long tenantId, Long userId) {
    int approved = dedupListMapper.approvePending();
    if (approved == 0) {
      log.warn("去重软删除：无待执行清单行（可能已执行或清单未生成）");
      return new ApplyResultVO(null, 0, 0);
    }
    String backupTable = "exam_exam_detail_bak_dedup_"
        + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    queryMapper.createBackupTable(backupTable);
    // FIX-8：去重清单行状态为 approved
    queryMapper.backupRows(backupTable, "exam_detail_dedup_list", "approved");

    int totalUpdated = 0;
    int lastId = 0;
    while (true) {
      List<Integer> ids = dedupListMapper.selectApprovedDeleteIds(lastId, BATCH_SIZE);
      if (ids.isEmpty()) {
        break;
      }
      totalUpdated += dedupListMapper.softDeleteByIds(ids);
      lastId = ids.get(ids.size() - 1);
    }
    dedupListMapper.markDedupDone();
    log.info("去重软删除完成：approved={}, deleted={}", approved, totalUpdated);
    return new ApplyResultVO(backupTable, approved, totalUpdated);
  }

  /**
   * 去重回滚（方案 §6.3，回滚后清单标记 rolled_back）。
   * 备份表名做前缀+日期格式守卫（与写回侧同规）：表名会拼进 SQL，禁止任意标识符直通。
   */
  public int rollbackSoftDelete(String backupTable) {
    if (backupTable == null || !DEDUP_BACKUP_TABLE_PATTERN.matcher(backupTable).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "非法备份表名：" + backupTable + "（应形如 exam_exam_detail_bak_dedup_YYYYMMDD）");
    }
    int restored = queryMapper.rollbackSoftDelete(backupTable);
    dedupListMapper.markDedupRolledBack();
    log.info("去重回滚完成：backupTable={}, restored={}", backupTable, restored);
    return restored;
  }
}
