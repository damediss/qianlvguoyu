package com.guoyu.platform.exam.knowledgefix.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.knowledgefix.dto.AdjudicateResultVO;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 存疑行人工定夺（方案 docs/04_requirements/存疑行人工定夺功能方案.md）。
 * 状态机：仅 存疑(flag) → 改挂(apply)/保留(keep)；只翻清单行状态，绝不动 exam_exam_detail
 * （题库落地仍走既有「执行写回」单一入口，备份/回滚链不变）。
 */
@Service
public class KnowledgeFixAdjudicateService {

  public static final String ACTION_APPLY = "apply";
  public static final String ACTION_KEEP = "keep";
  private static final int BATCH_LIMIT = 200;

  private final ExamDetailKnowledgeFixMapper fixMapper;
  private final ExamKnowledgeStructureMapper structureMapper;

  public KnowledgeFixAdjudicateService(ExamDetailKnowledgeFixMapper fixMapper,
      ExamKnowledgeStructureMapper structureMapper) {
    this.fixMapper = fixMapper;
    this.structureMapper = structureMapper;
  }

  /** 单行定夺：非法输入直接抛业务异常（前端按单条失败提示） */
  public AdjudicateResultVO adjudicate(Long rowId, String action, Long userId) {
    validateAction(action);
    if (rowId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "rowId 不能为空");
    }
    List<AdjudicateResultVO.SkipVO> skipped = new ArrayList<>();
    long succeeded = applyOne(rowId, action, userId, skipped, true);
    return AdjudicateResultVO.of(succeeded, skipped);
  }

  /** 批量定夺（≤200）：逐行独立校验，失败的行跳过并带原因，不整批失败 */
  public AdjudicateResultVO batch(List<Long> rowIds, String action, Long userId) {
    validateAction(action);
    if (rowIds == null || rowIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "rowIds 不能为空");
    }
    if (rowIds.size() > BATCH_LIMIT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "批量定夺一次最多 " + BATCH_LIMIT + " 行（当前 " + rowIds.size() + " 行）");
    }
    Set<Long> distinct = new LinkedHashSet<>(rowIds);
    List<AdjudicateResultVO.SkipVO> skipped = new ArrayList<>();
    long succeeded = 0;
    for (Long rowId : distinct) {
      succeeded += applyOne(rowId, action, userId, skipped, false);
    }
    return AdjudicateResultVO.of(succeeded, skipped);
  }

  private void validateAction(String action) {
    if (!ACTION_APPLY.equals(action) && !ACTION_KEEP.equals(action)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "action 仅支持 " + ACTION_APPLY + "（改挂）/" + ACTION_KEEP + "（保留）");
    }
  }

  /**
   * 单行状态机执行。single=true 时任何问题抛异常（单行定夺语义）；
   * single=false 时跳过并记原因（批量语义）。返回 1=成功 / 0=跳过。
   */
  private long applyOne(Long rowId, String action, Long userId,
      List<AdjudicateResultVO.SkipVO> skipped, boolean single) {
    if (rowId == null) {
      return skipOrThrow(single, skipped, null, "rowId 为空");
    }
    ExamDetailKnowledgeFixEntity row = fixMapper.selectById(rowId);
    if (row == null) {
      return skipOrThrow(single, skipped, rowId, "行不存在或已删除");
    }
    if (!ExamDetailKnowledgeFixEntity.STATUS_FLAG.equals(row.getStatus())) {
      return skipOrThrow(single, skipped, rowId,
          "仅存疑行可定夺（当前状态：" + row.getStatus() + "）");
    }
    String targetStatus;
    Integer targetNode;
    if (ACTION_APPLY.equals(action)) {
      Integer newId = row.getNewKnowledgeId();
      if (newId == null) {
        return skipOrThrow(single, skipped, rowId, "该行无新知识点建议，无法定夺为改挂");
      }
      ExamKnowledgeStructureEntity node = structureMapper.selectById(newId);
      if (node == null) {
        return skipOrThrow(single, skipped, rowId, "新知识点不存在或已删，无法改挂");
      }
      if (!Objects.equals(node.getCourseId(), row.getCourseId())) {
        return skipOrThrow(single, skipped, rowId,
            "新知识点与该行课程不一致（越界），已阻止改挂");
      }
      targetStatus = ExamDetailKnowledgeFixEntity.STATUS_APPLY;
      targetNode = newId;
    } else {
      // 原节点已删的行不允许"保留"——保留=挂死节点（方案 §9 取舍 4）
      Integer oldId = row.getOldKnowledgeId();
      if (structureMapper.selectById(oldId) == null) {
        return skipOrThrow(single, skipped, rowId, "原知识点已删，不能保留，请定夺为改挂");
      }
      targetStatus = ExamDetailKnowledgeFixEntity.STATUS_KEEP;
      targetNode = oldId;
    }
    int affected = fixMapper.adjudicateRow(rowId, targetStatus, targetNode, userId);
    if (affected == 0) {
      return skipOrThrow(single, skipped, rowId, "行状态已被并发修改，请刷新后重试");
    }
    return 1;
  }

  private long skipOrThrow(boolean single, List<AdjudicateResultVO.SkipVO> skipped,
      Long rowId, String reason) {
    if (single) {
      throw new BusinessException(ErrorCode.CONFLICT, reason);
    }
    skipped.add(new AdjudicateResultVO.SkipVO(rowId == null ? 0L : rowId, reason));
    return 0;
  }
}
