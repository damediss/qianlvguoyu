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
}
