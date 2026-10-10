package com.guoyu.platform.exam.knowledgefix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 知识修正清单 Mapper */
@Mapper
public interface ExamDetailKnowledgeFixMapper extends BaseMapper<ExamDetailKnowledgeFixEntity> {

  /** 单行分类结果落库（含批次号审计：§13.2 回收 SQL 按 batch_no 查分层构成） */
  @Update("UPDATE exam_detail_knowledge_fix SET new_knowledge_id = #{newKnowledgeId}, "
      + "confidence = #{confidence}, basis = #{basis}, llm_reason = #{reason}, "
      + "status = #{status}, batch_no = #{batchNo}, updated_at = NOW() "
      + "WHERE id = #{rowId} AND deleted = 0")
  int updateClassifyResult(@Param("rowId") Long rowId, @Param("newKnowledgeId") Integer newKnowledgeId,
      @Param("confidence") java.math.BigDecimal confidence, @Param("basis") String basis,
      @Param("reason") String reason, @Param("status") String status,
      @Param("batchNo") Long batchNo);

  /** 写回后清单行置 applied */
  @Update("UPDATE exam_detail_knowledge_fix SET status = 'applied', updated_at = NOW() "
      + "WHERE status = 'apply' AND deleted = 0")
  int markAllApplied();

  /**
   * 人工定夺（仅存疑行可翻）：状态翻转 + 结论落 new_knowledge_id（保留=原节点）+ 记操作人。
   * 条件更新带 status='flag' 守卫——并发下被改走的行 affected=0，调用方按冲突处理。
   */
  @Update("UPDATE exam_detail_knowledge_fix SET status = #{status}, "
      + "new_knowledge_id = #{newKnowledgeId}, updated_by = #{userId}, updated_at = NOW() "
      + "WHERE id = #{rowId} AND status = 'flag' AND deleted = 0")
  int adjudicateRow(@Param("rowId") Long rowId, @Param("status") String status,
      @Param("newKnowledgeId") Integer newKnowledgeId, @Param("userId") Long userId);

  /**
   * 待分类行（联考试题内容；供 LLM 分类输入）。
   * FIX-9 范围加固（v1.7）：分类阶段再次限定 5 月以来——首轮越界装入的历史行若残留清单，绝不送模型（DEF-4/5）。
   */
  @Select("""
      SELECT f.id AS rowId, f.detail_id AS detailId, f.old_knowledge_id AS oldKnowledgeId,
             f.course_id AS courseId, e.content, e.answer, e.resolve,
             f.status AS currentStatus, f.confidence AS currentConfidence
      FROM exam_detail_knowledge_fix f
      JOIN exam_exam_detail e ON e.id = f.detail_id
      WHERE f.status = 'pending' AND f.deleted = 0 AND e.deleted = 0
        AND e.create_time >= '2026-05-01'
      ORDER BY f.id
      """)
  java.util.List<com.guoyu.platform.exam.knowledgefix.dto.PendingFixRow> selectPendingWithContent();

  /**
   * 重跑通道（FIX-3/DEF-6）：flag/rejected 行复位为 pending，清空上一轮结论
   * （new_knowledge_id/confidence/basis/llm_reason/batch_no），使批次可完整重跑；
   * 已写回（applied）与 apply 待写回行不动——它们已有结论，不能被重跑覆盖。
   * FIX-9 范围加固（v1.7）：与分类范围同口径（5 月以来 + detail 存活），复位不会把 5 月前历史行拉回待分类。
   */
  @Update("""
      UPDATE exam_detail_knowledge_fix f
      JOIN exam_exam_detail e ON e.id = f.detail_id
      SET f.status = 'pending', f.new_knowledge_id = NULL, f.confidence = NULL, f.basis = NULL,
          f.llm_reason = NULL, f.batch_no = NULL, f.updated_at = NOW()
      WHERE f.status = 'flag' AND f.deleted = 0 AND e.deleted = 0
        AND e.create_time >= '2026-05-01'
      """)
  int resetFlagRows();

  /** 复位行数（按状态统计，供前端提示；FIX-9：与分类范围同口径） */
  @Select("""
      SELECT COUNT(*) FROM exam_detail_knowledge_fix f
      JOIN exam_exam_detail e ON e.id = f.detail_id
      WHERE f.status = #{status} AND f.deleted = 0 AND e.deleted = 0
        AND e.create_time >= '2026-05-01'
      """)
  long countByStatus(@Param("status") String status);
}
