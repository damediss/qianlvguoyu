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

  /** 单行分类结果落库 */
  @Update("UPDATE exam_detail_knowledge_fix SET new_knowledge_id = #{newKnowledgeId}, "
      + "confidence = #{confidence}, basis = #{basis}, llm_reason = #{reason}, "
      + "status = #{status}, updated_at = NOW() "
      + "WHERE id = #{rowId} AND deleted = 0")
  int updateClassifyResult(@Param("rowId") Long rowId, @Param("newKnowledgeId") Integer newKnowledgeId,
      @Param("confidence") java.math.BigDecimal confidence, @Param("basis") String basis,
      @Param("reason") String reason, @Param("status") String status);

  /** 写回后清单行置 applied */
  @Update("UPDATE exam_detail_knowledge_fix SET status = 'applied', updated_at = NOW() "
      + "WHERE status = 'apply' AND deleted = 0")
  int markAllApplied();

  /** 待分类行（联考试题内容；供 LLM 分类输入） */
  @Select("""
      SELECT f.id AS rowId, f.detail_id AS detailId, f.old_knowledge_id AS oldKnowledgeId,
             f.course_id AS courseId, e.content, e.answer, e.resolve,
             f.status AS currentStatus, f.confidence AS currentConfidence
      FROM exam_detail_knowledge_fix f
      JOIN exam_exam_detail e ON e.id = f.detail_id
      WHERE f.status = 'pending' AND f.deleted = 0 AND e.deleted = 0
      ORDER BY f.id
      """)
  java.util.List<com.guoyu.platform.exam.knowledgefix.dto.PendingFixRow> selectPendingWithContent();

  /**
   * 重跑通道（FIX-3/DEF-6）：flag/rejected 行复位为 pending，清空上一轮结论
   * （new_knowledge_id/confidence/basis/llm_reason/batch_no），使批次可完整重跑；
   * 已写回（applied）与 apply 待写回行不动——它们已有结论，不能被重跑覆盖。
   */
  @Update("UPDATE exam_detail_knowledge_fix SET status = 'pending', new_knowledge_id = NULL, "
      + "confidence = NULL, basis = NULL, llm_reason = NULL, batch_no = NULL, updated_at = NOW() "
      + "WHERE status = 'flag' AND deleted = 0")
  int resetFlagRows();

  /** 复位行数（按状态统计，供前端提示） */
  @Select("SELECT COUNT(*) FROM exam_detail_knowledge_fix WHERE status = #{status} AND deleted = 0")
  long countByStatus(@Param("status") String status);
}
