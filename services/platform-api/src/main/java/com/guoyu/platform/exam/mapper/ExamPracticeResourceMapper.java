package com.guoyu.platform.exam.mapper;

import com.guoyu.platform.exam.dto.ExamPracticeCourseScope;
import com.guoyu.platform.exam.dto.ExamPracticeQuestionResourceRow;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Read-only adapter for approved, active course questions available to pre-practice tasks. */
@Mapper
public interface ExamPracticeResourceMapper {

  @Select("""
      <script>
      SELECT e.type AS questionType, COUNT(*) AS availableCount
      FROM exam_course c
      JOIN exam_knowledge_structure k
        ON k.course_id = c.id AND k.tenant_id = c.tenant_id
       AND k.deleted = 0 AND k.type = 0 AND k.level = 3
      JOIN exam_exam_detail e
        ON e.knowledge_id = k.id AND e.tenant_id = c.tenant_id
       AND e.deleted = 0 AND e.status = 1 AND e.final_check_status = 1
      WHERE c.tenant_id = #{tenantId} AND c.deleted = 0 AND c.status = 1
        AND c.work_type_id IS NOT NULL
        AND e.type IN
        <foreach collection="questionTypes" item="type" open="(" separator="," close=")">
          #{type}
        </foreach>
        AND (
          <foreach collection="scopes" item="scope" separator=" OR ">
            (c.work_type_id = #{scope.workTypeId} AND c.level = #{scope.courseLevel})
          </foreach>
        )
      GROUP BY e.type
      </script>
      """)
  List<Map<String, Object>> countAvailableQuestions(
      @Param("tenantId") Long tenantId,
      @Param("scopes") List<ExamPracticeCourseScope> scopes,
      @Param("questionTypes") List<String> questionTypes);

  @Select("""
      <script>
      SELECT e.id AS questionId, c.id AS sourceCourseId,
             c.work_type_id AS sourceWorkTypeId, c.level AS sourceLevel,
             e.knowledge_id AS knowledgeId, e.type AS questionType,
             e.content, e.answer, e.resolve, e.audio, e.video, e.difficulty, e.score
      FROM exam_course c
      JOIN exam_knowledge_structure k
        ON k.course_id = c.id AND k.tenant_id = c.tenant_id
       AND k.deleted = 0 AND k.type = 0 AND k.level = 3
      JOIN exam_exam_detail e
        ON e.knowledge_id = k.id AND e.tenant_id = c.tenant_id
       AND e.deleted = 0 AND e.status = 1 AND e.final_check_status = 1
      WHERE c.tenant_id = #{tenantId} AND c.deleted = 0 AND c.status = 1
        AND c.work_type_id IS NOT NULL AND e.type = #{questionType}
        AND (
          <foreach collection="scopes" item="scope" separator=" OR ">
            (c.work_type_id = #{scope.workTypeId} AND c.level = #{scope.courseLevel})
          </foreach>
        )
      ORDER BY e.id
      LIMIT #{offset}, #{limit}
      </script>
      """)
  List<ExamPracticeQuestionResourceRow> selectQuestionWindow(
      @Param("tenantId") Long tenantId,
      @Param("scopes") List<ExamPracticeCourseScope> scopes,
      @Param("questionType") String questionType,
      @Param("offset") long offset,
      @Param("limit") int limit);
}
