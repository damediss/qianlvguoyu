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

  /** 按题型聚合错题数/答题数（日志无题型时回退题库 type） */
  @Select("""
      SELECT COALESCE(NULLIF(TRIM(l.question_type), ''), NULLIF(TRIM(d.type), ''), 'unknown') AS questionType,
             SUM(CASE WHEN l.is_right = 0 THEN 1 ELSE 0 END) AS wrongCount,
             COUNT(*) AS totalCount
      FROM exam_user_answer_log l
      LEFT JOIN exam_exam_detail d ON d.id = l.question_id
      WHERE l.tenant_id = #{tenantId} AND l.user_id = #{userId}
      GROUP BY COALESCE(NULLIF(TRIM(l.question_type), ''), NULLIF(TRIM(d.type), ''), 'unknown')
      """)
  List<Map<String, Object>> wrongStatsByQuestionType(@Param("tenantId") Long tenantId,
                                                     @Param("userId") Long userId);

  /**
   * 错题 id 列表：仅保留「最近一次作答仍为错」的题（答对后自动出本）。
   * 按首次入本时间升序；limit 为空或 ≤0 表示全部。
   */
  @Select("""
      <script>
      SELECT t.question_id
      FROM exam_user_answer_log t
      INNER JOIN (
        SELECT question_id, MAX(id) AS max_id
        FROM exam_user_answer_log
        WHERE tenant_id = #{tenantId} AND user_id = #{userId}
        GROUP BY question_id
      ) latest ON t.id = latest.max_id
      WHERE t.is_right = 0
      <if test="knowledgeId != null"> AND t.knowledge_id = #{knowledgeId} </if>
      ORDER BY t.id ASC
      <if test="limit != null and limit &gt; 0"> LIMIT #{limit} </if>
      </script>
      """)
  List<Integer> findWrongQuestionIds(@Param("tenantId") Long tenantId,
                                     @Param("userId") Long userId,
                                     @Param("knowledgeId") Integer knowledgeId,
                                     @Param("limit") Integer limit);

  /** 错题本总数（最近一次仍错，按题去重） */
  @Select("""
      <script>
      SELECT COUNT(*)
      FROM exam_user_answer_log t
      INNER JOIN (
        SELECT question_id, MAX(id) AS max_id
        FROM exam_user_answer_log
        WHERE tenant_id = #{tenantId} AND user_id = #{userId}
        GROUP BY question_id
      ) latest ON t.id = latest.max_id
      WHERE t.is_right = 0
      <if test="knowledgeId != null"> AND t.knowledge_id = #{knowledgeId} </if>
      </script>
      """)
  long countWrongQuestionIds(@Param("tenantId") Long tenantId,
                             @Param("userId") Long userId,
                             @Param("knowledgeId") Integer knowledgeId);

  /** 删除指定用户某题的历史错题记录（答对出本时可选用） */
  @org.apache.ibatis.annotations.Delete("""
      DELETE FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId} AND user_id = #{userId}
        AND question_id = #{questionId} AND is_right = 0
      """)
  int deleteWrongLogsByQuestion(@Param("tenantId") Long tenantId,
                                @Param("userId") Long userId,
                                @Param("questionId") Integer questionId);

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

  @Select("SELECT IFNULL(MAX(id), 0) FROM exam_user_answer_log")
  Long selectMaxId();

  /**
   * 本班学员每题最近一次作答（用于覆盖进度：已做 / 仍错）。
   */
  @Select("""
      SELECT t.question_id AS questionId,
             t.is_right AS isRight,
             t.knowledge_id AS knowledgeId,
             t.question_type AS questionType,
             t.user_answer AS userAnswer
      FROM exam_user_answer_log t
      INNER JOIN (
        SELECT question_id, MAX(id) AS max_id
        FROM exam_user_answer_log
        WHERE tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND class_id = #{classId}
          AND question_id IS NOT NULL
        GROUP BY question_id
      ) latest ON t.id = latest.max_id
      """)
  List<Map<String, Object>> findLatestAnswersByClassUser(@Param("tenantId") Long tenantId,
                                                         @Param("userId") Long userId,
                                                         @Param("classId") Long classId);

  /** 本班已做过的题目 id（去重，不论对错） */
  @Select("""
      SELECT DISTINCT question_id
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId}
        AND user_id = #{userId}
        AND class_id = #{classId}
        AND question_id IS NOT NULL
      """)
  List<Integer> findDoneQuestionIdsByClass(@Param("tenantId") Long tenantId,
                                           @Param("userId") Long userId,
                                           @Param("classId") Long classId);

  /** 学员答过的题目 id（不论对错），用于同型推题时优先未做过的新题 */
  @Select("""
      SELECT DISTINCT question_id
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId}
        AND user_id = #{userId}
        AND question_id IS NOT NULL
      """)
  List<Integer> findAnsweredQuestionIds(@Param("tenantId") Long tenantId,
                                        @Param("userId") Long userId);

  /** 近 N 天答题对错汇总（精准学效果） */
  @Select("""
      SELECT SUM(CASE WHEN is_right = 0 THEN 1 ELSE 0 END) AS wrongCount,
             COUNT(*) AS totalCount
      FROM exam_user_answer_log
      WHERE tenant_id = #{tenantId}
        AND user_id = #{userId}
        AND create_time >= #{fromTime}
      """)
  Map<String, Object> answerStatsSince(@Param("tenantId") Long tenantId,
                                       @Param("userId") Long userId,
                                       @Param("fromTime") java.time.LocalDateTime fromTime);

  /**
   * 近 N 日「出本」题数：最近一次作答为对，且历史上有过错。
   * 用于精准学 Hub 展示「消灭错题」感知。
   */
  @Select("""
      SELECT COUNT(DISTINCT t.question_id)
      FROM exam_user_answer_log t
      INNER JOIN (
        SELECT question_id, MAX(id) AS max_id
        FROM exam_user_answer_log
        WHERE tenant_id = #{tenantId}
          AND user_id = #{userId}
          AND create_time >= #{fromTime}
          AND question_id IS NOT NULL
        GROUP BY question_id
      ) latest ON t.id = latest.max_id
      WHERE t.is_right = 1
        AND EXISTS (
          SELECT 1 FROM exam_user_answer_log w
          WHERE w.tenant_id = #{tenantId}
            AND w.user_id = #{userId}
            AND w.question_id = t.question_id
            AND w.is_right = 0
            AND w.id < t.id
        )
      """)
  long countClearedWrongSince(@Param("tenantId") Long tenantId,
                              @Param("userId") Long userId,
                              @Param("fromTime") java.time.LocalDateTime fromTime);
}
