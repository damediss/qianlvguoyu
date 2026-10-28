package com.guoyu.platform.exam.answercheck.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.exam.answercheck.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeQuery;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeRow;
import com.guoyu.platform.exam.answercheck.dto.WorkTypeOptionRow;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 统一 scope 查询（方案 §7.2）：试题分页、创建快照、筛选项共用同一套关联与过滤。
 *
 * <p>租户过滤交给 MyBatis-Plus 插件，SQL 不写当前租户字面量；JOIN ON 只写 deleted=0 与主外键等值。
 * d.knowledge_id 为单值，关联链不会把一道题放大成多行。
 */
@Mapper
public interface ObjectiveAnswerCheckScopeMapper {

  String BASE_FROM = """
      FROM exam_exam_detail d
      LEFT JOIN exam_knowledge_structure k ON d.knowledge_id = k.id AND k.deleted = 0
      LEFT JOIN exam_course c ON k.course_id = c.id AND c.deleted = 0
      LEFT JOIN exam_work_type w ON c.work_type_id = w.id AND w.deleted = 0
      """;

  String WHERE = """
      <where>
        d.deleted = 0
        <if test="q.types != null and q.types.size() > 0">
          AND d.type IN
          <foreach collection="q.types" item="type" open="(" separator="," close=")">#{type}</foreach>
        </if>
        <if test="q.source != null">AND d.source = #{q.source}</if>
        <if test="q.workTypeId != null">AND c.work_type_id = #{q.workTypeId}</if>
        <if test="q.levelCode != null">AND c.level = 5 - #{q.levelCode}</if>
        <if test="q.detailIds != null and q.detailIds.size() > 0">
          AND d.id IN
          <foreach collection="q.detailIds" item="detailId" open="(" separator="," close=")">#{detailId}</foreach>
        </if>
      </where>
      """;

  @Select("""
      <script>
      SELECT d.id AS detailId, d.type AS questionType, d.content AS content, d.answer AS answer,
             d.resolve AS resolveText, d.source AS source, d.audio AS audio, d.video AS video,
             d.knowledge_id AS knowledgeId,
             k.title AS knowledgeTitle, k.course_id AS courseId, c.title AS courseTitle,
             c.level AS courseLevel, c.work_type_id AS workTypeId, w.title AS workTypeTitle
      """ + BASE_FROM + WHERE + """
      ORDER BY d.id
      </script>
      """)
  IPage<ObjectiveAnswerScopeRow> pageQuestions(Page<ObjectiveAnswerScopeRow> page,
      @Param("q") ObjectiveAnswerScopeQuery query);

  @Select("""
      <script>
      SELECT COUNT(*)
      """ + BASE_FROM + WHERE + """
      </script>
      """)
  long countQuestions(@Param("q") ObjectiveAnswerScopeQuery query);

  @Select("""
      <script>
      SELECT d.id AS detailId, d.type AS questionType, d.content AS content, d.answer AS answer,
             d.resolve AS resolveText, d.source AS source, d.audio AS audio, d.video AS video,
             d.knowledge_id AS knowledgeId,
             k.title AS knowledgeTitle, k.course_id AS courseId, c.title AS courseTitle,
             c.level AS courseLevel, c.work_type_id AS workTypeId, w.title AS workTypeTitle
      """ + BASE_FROM + WHERE + """
      ORDER BY d.id
      LIMIT #{limit} OFFSET #{offset}
      </script>
      """)
  List<ObjectiveAnswerScopeRow> listQuestions(@Param("q") ObjectiveAnswerScopeQuery query,
      @Param("limit") int limit, @Param("offset") int offset);

  /** 只返回经“知识点→课程”关联后确实有存活客观题的工种。 */
  @Select("""
      SELECT DISTINCT w.id AS id, w.title AS title, w.parent_id AS parentId, w.series_no AS seriesNo
      FROM exam_exam_detail d
      JOIN exam_knowledge_structure k ON d.knowledge_id = k.id AND k.deleted = 0
      JOIN exam_course c ON k.course_id = c.id AND c.deleted = 0
      JOIN exam_work_type w ON c.work_type_id = w.id AND w.deleted = 0
      WHERE d.deleted = 0 AND d.type IN ('single', 'multi', 'judge')
      ORDER BY w.sort_number, w.id
      """)
  List<WorkTypeOptionRow> listWorkTypesWithQuestions();

  /** 全量工种（构建完整父路径用；不参与“有题”过滤）。 */
  @Select("""
      SELECT id, title, parent_id AS parentId, series_no AS seriesNo
      FROM exam_work_type
      WHERE deleted = 0
      ORDER BY sort_number, id
      """)
  List<WorkTypeOptionRow> listAllWorkTypes();

  /** 返回该工种实际存在的课程等级（0～4 存储值），服务层转 levelCode=5-level。 */
  @Select("""
      <script>
      SELECT DISTINCT c.level
      FROM exam_exam_detail d
      JOIN exam_knowledge_structure k ON d.knowledge_id = k.id AND k.deleted = 0
      JOIN exam_course c ON k.course_id = c.id AND c.deleted = 0
      WHERE d.deleted = 0 AND d.type IN ('single', 'multi', 'judge')
      <if test="workTypeId != null">AND c.work_type_id = #{workTypeId}</if>
      ORDER BY c.level
      </script>
      """)
  List<Integer> listLevels(@Param("workTypeId") Integer workTypeId);

  @Select("""
      <script>
      SELECT id, title, parent_id AS parentId, course_id AS courseId
      FROM exam_knowledge_structure
      WHERE deleted = 0
      <if test="courseIds != null and courseIds.size() > 0">
        AND course_id IN
        <foreach collection="courseIds" item="courseId" open="(" separator="," close=")">#{courseId}</foreach>
      </if>
      </script>
      """)
  List<KnowledgeNodeRow> listKnowledgeNodes(@Param("courseIds") Collection<Integer> courseIds);
}
