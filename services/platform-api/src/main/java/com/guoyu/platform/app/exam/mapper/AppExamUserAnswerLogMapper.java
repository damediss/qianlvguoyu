package com.guoyu.platform.app.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.app.exam.entity.AppExamUserAnswerLogEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppExamUserAnswerLogMapper extends BaseMapper<AppExamUserAnswerLogEntity> {

  /** 按 knowledge_id 聚合错题数/答题数，用于考点预测 */
  @Select("""
      SELECT knowledge_id AS knowledgeId,
             SUM(CASE WHEN is_right = 0 THEN 1 ELSE 0 END) AS wrongCount,
             COUNT(*) AS totalCount
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND user_id = #{userId}
        AND knowledge_id IS NOT NULL
      GROUP BY knowledge_id
      """)
  List<Map<String, Object>> wrongStatsByKnowledge(@Param("tenantId") Long tenantId,
                                                   @Param("userId") Long userId);

  /** 错题 id 列表（按错题时间倒序，可选限定考点） */
  @Select("""
      <script>
      SELECT DISTINCT question_id
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND is_right = 0
      <if test="knowledgeId != null"> AND knowledge_id = #{knowledgeId} </if>
      ORDER BY id DESC
      LIMIT #{limit}
      </script>
      """)
  List<Integer> findWrongQuestionIds(@Param("tenantId") Long tenantId,
                                     @Param("userId") Long userId,
                                     @Param("knowledgeId") Integer knowledgeId,
                                     @Param("limit") Integer limit);

  @Select("""
      <script>
      SELECT user_id AS userId,
             COUNT(*) AS answerCount,
             SUM(CASE WHEN is_right = 1 THEN 1 ELSE 0 END) AS rightCount,
             COUNT(DISTINCT paper_id) AS paperCount,
             COUNT(DISTINCT COALESCE(biz_date, DATE(create_time))) AS practiceDays,
             MAX(create_time) AS lastPracticeAt
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND class_id = #{classId}
        AND create_time &gt;= #{from} AND create_time &lt; #{to}
      <if test="knowledgeIds != null and knowledgeIds.size() &gt; 0">
        AND knowledge_id IN
        <foreach collection="knowledgeIds" item="kid" open="(" separator="," close=")">#{kid}</foreach>
      </if>
      GROUP BY user_id
      </script>
      """)
  List<Map<String, Object>> aggregateByUser(@Param("tenantId") Long tenantId,
                                            @Param("classId") Long classId,
                                            @Param("from") java.time.LocalDateTime from,
                                            @Param("to") java.time.LocalDateTime to,
                                            @Param("knowledgeIds") List<Integer> knowledgeIds);

  @Select("""
      <script>
      SELECT knowledge_id AS knowledgeId,
             COUNT(*) AS answerCount,
             SUM(CASE WHEN is_right = 0 THEN 1 ELSE 0 END) AS wrongCount,
             SUM(CASE WHEN is_right = 1 THEN 1 ELSE 0 END) AS rightCount,
             COUNT(DISTINCT user_id) AS userCount
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND class_id = #{classId}
        AND create_time &gt;= #{from} AND create_time &lt; #{to}
        AND knowledge_id IS NOT NULL
      <if test="knowledgeIds != null and knowledgeIds.size() &gt; 0">
        AND knowledge_id IN
        <foreach collection="knowledgeIds" item="kid" open="(" separator="," close=")">#{kid}</foreach>
      </if>
      GROUP BY knowledge_id
      ORDER BY wrongCount DESC, answerCount DESC
      LIMIT #{limit}
      </script>
      """)
  List<Map<String, Object>> aggregateByKnowledge(@Param("tenantId") Long tenantId,
                                                 @Param("classId") Long classId,
                                                 @Param("from") java.time.LocalDateTime from,
                                                 @Param("to") java.time.LocalDateTime to,
                                                 @Param("knowledgeIds") List<Integer> knowledgeIds,
                                                 @Param("limit") Integer limit);

  @Select("""
      <script>
      SELECT COALESCE(biz_date, DATE(create_time)) AS bizDate,
             COUNT(*) AS answerCount,
             SUM(CASE WHEN is_right = 1 THEN 1 ELSE 0 END) AS rightCount,
             COUNT(DISTINCT user_id) AS userCount,
             COUNT(DISTINCT paper_id) AS paperCount
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND class_id = #{classId}
        AND create_time &gt;= #{from} AND create_time &lt; #{to}
      <if test="knowledgeIds != null and knowledgeIds.size() &gt; 0">
        AND knowledge_id IN
        <foreach collection="knowledgeIds" item="kid" open="(" separator="," close=")">#{kid}</foreach>
      </if>
      GROUP BY COALESCE(biz_date, DATE(create_time))
      ORDER BY bizDate ASC
      </script>
      """)
  List<Map<String, Object>> aggregateDaily(@Param("tenantId") Long tenantId,
                                           @Param("classId") Long classId,
                                           @Param("from") java.time.LocalDateTime from,
                                           @Param("to") java.time.LocalDateTime to,
                                           @Param("knowledgeIds") List<Integer> knowledgeIds);
}
