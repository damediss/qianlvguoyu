package com.guoyu.platform.exam.reviewsync.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI 同步学统计聚合查询（《AI同步学统计功能设计方案》v1.1 §5.2/§5.3）。
 *
 * <p>全部只读聚合 SQL；tenant_id 由 MyBatis-Plus 多租户插件自动追加（ai_review_* 不在忽略名单）。
 * platform_user / train_exam_list 等忽略名单表不参与本 Mapper 的 SQL，由 Service 单独查询后在内存组装。
 */
public interface ReviewSyncStatsMapper {

  /** 每日作答趋势：按作答日期聚合活跃学员/作答数/答对数 */
  @Select("""
      <script>
      SELECT DATE(created_at) AS day,
             COUNT(DISTINCT platform_user_id) AS activeUsers,
             COUNT(*) AS answers,
             COALESCE(SUM(is_correct), 0) AS corrects
      FROM ai_review_answer
      WHERE deleted = 0
        AND created_at &gt;= #{from} AND created_at &lt; DATE_ADD(#{to}, INTERVAL 1 DAY)
        <if test="examId != null">AND exam_plan_id = #{examId}</if>
        <if test="platformUserId != null">AND platform_user_id = #{platformUserId}</if>
      GROUP BY DATE(created_at)
      ORDER BY day
      </script>
      """)
  List<Map<String, Object>> dailyAnswerTrend(@Param("from") LocalDate from, @Param("to") LocalDate to,
      @Param("examId") Long examId, @Param("platformUserId") Long platformUserId);

  /** 每日任务完成情况：按计划日期聚合任务总数/完成数（任务完成率口径） */
  @Select("""
      <script>
      SELECT plan_date AS day,
             COUNT(*) AS totalDays,
             COALESCE(SUM(status = 'done'), 0) AS doneDays
      FROM ai_review_plan_day
      WHERE deleted = 0 AND status != 'skipped'
        AND plan_date &gt;= #{from} AND plan_date &lt;= #{to}
        <if test="examId != null">
          AND plan_id IN (SELECT id FROM ai_review_plan WHERE deleted = 0 AND exam_id = #{examId})
        </if>
      GROUP BY plan_date
      ORDER BY day
      </script>
      """)
  List<Map<String, Object>> dailyDayCompletion(@Param("from") LocalDate from, @Param("to") LocalDate to,
      @Param("examId") Long examId);

  /** 范围内作答汇总：作答数/答对数/活跃学员数（overview 用） */
  @Select("""
      <script>
      SELECT COUNT(*) AS answers,
             COALESCE(SUM(is_correct), 0) AS corrects,
             COUNT(DISTINCT platform_user_id) AS activeUsers
      FROM ai_review_answer
      WHERE deleted = 0
        AND created_at &gt;= #{from} AND created_at &lt; DATE_ADD(#{to}, INTERVAL 1 DAY)
        <if test="examId != null">AND exam_plan_id = #{examId}</if>
      </script>
      """)
  Map<String, Object> answerTotals(@Param("from") LocalDate from, @Param("to") LocalDate to,
      @Param("examId") Long examId);

  /** 错题牌级分布快照：按 card_level + removed 分组（在册=removed 0，移出=removed 1 即绿牌） */
  @Select("""
      <script>
      SELECT card_level AS cardLevel,
             removed AS removed,
             COUNT(*) AS cnt,
             COALESCE(SUM(star), 0) AS starCnt
      FROM ai_review_wrong_item
      WHERE deleted = 0
        <if test="examId != null">
          AND plan_id IN (SELECT id FROM ai_review_plan WHERE deleted = 0 AND exam_id = #{examId})
        </if>
        <if test="platformUserId != null">AND platform_user_id = #{platformUserId}</if>
      GROUP BY card_level, removed
      </script>
      """)
  List<Map<String, Object>> wrongSnapshot(@Param("examId") Long examId,
      @Param("platformUserId") Long platformUserId);

  /** 每日新增错题（近似口径：按首次答错时间） */
  @Select("""
      <script>
      SELECT DATE(first_wrong_time) AS day, COUNT(*) AS cnt
      FROM ai_review_wrong_item
      WHERE deleted = 0 AND first_wrong_time IS NOT NULL
        AND first_wrong_time &gt;= #{from} AND first_wrong_time &lt; DATE_ADD(#{to}, INTERVAL 1 DAY)
        <if test="examId != null">
          AND plan_id IN (SELECT id FROM ai_review_plan WHERE deleted = 0 AND exam_id = #{examId})
        </if>
      GROUP BY DATE(first_wrong_time)
      ORDER BY day
      </script>
      """)
  List<Map<String, Object>> wrongDailyNew(@Param("from") LocalDate from, @Param("to") LocalDate to,
      @Param("examId") Long examId);

  /** 每日消灭错题（近似口径：按移出时间，removed=1） */
  @Select("""
      <script>
      SELECT DATE(remove_time) AS day, COUNT(*) AS cnt
      FROM ai_review_wrong_item
      WHERE deleted = 0 AND removed = 1 AND remove_time IS NOT NULL
        AND remove_time &gt;= #{from} AND remove_time &lt; DATE_ADD(#{to}, INTERVAL 1 DAY)
        <if test="examId != null">
          AND plan_id IN (SELECT id FROM ai_review_plan WHERE deleted = 0 AND exam_id = #{examId})
        </if>
      GROUP BY DATE(remove_time)
      ORDER BY day
      </script>
      """)
  List<Map<String, Object>> wrongDailyRemoved(@Param("from") LocalDate from, @Param("to") LocalDate to,
      @Param("examId") Long examId);

  /** 知识点作答聚合（L3 粒度，Service 侧按知识树聚合到 L1） */
  @Select("""
      <script>
      SELECT knowledge_id AS knowledgeId,
             COUNT(*) AS answers,
             COALESCE(SUM(is_correct), 0) AS corrects
      FROM ai_review_answer
      WHERE deleted = 0 AND knowledge_id IS NOT NULL
        <if test="examId != null">AND exam_plan_id = #{examId}</if>
        <if test="platformUserId != null">AND platform_user_id = #{platformUserId}</if>
      GROUP BY knowledge_id
      </script>
      """)
  List<Map<String, Object>> knowledgeAgg(@Param("examId") Long examId,
      @Param("platformUserId") Long platformUserId);

  /** 一批计划的作答聚合（学员明细表组装用）；lastAnswerDate=真实最后作答日（展示口径） */
  @Select("""
      <script>
      SELECT plan_id AS planId,
             COUNT(*) AS answers,
             COALESCE(SUM(is_correct), 0) AS corrects,
             COALESCE(SUM(round_type = 'new'), 0) AS newAnswers,
             COALESCE(SUM(round_type = 'review'), 0) AS reviewAnswers,
             COALESCE(SUM(round_type = 'mock'), 0) AS mockAnswers,
             MAX(DATE(created_at)) AS lastAnswerDate
      FROM ai_review_answer
      WHERE deleted = 0 AND plan_id IN
      <foreach collection="planIds" item="pid" open="(" separator="," close=")">#{pid}</foreach>
      GROUP BY plan_id
      </script>
      """)
  List<Map<String, Object>> planAnswerAgg(@Param("planIds") List<Long> planIds);

  /** 一批计划的错题聚合（学员明细表组装用） */
  @Select("""
      <script>
      SELECT plan_id AS planId,
             COALESCE(SUM(card_level = 'red' AND removed = 0), 0) AS redCnt,
             COALESCE(SUM(card_level = 'yellow' AND removed = 0), 0) AS yellowCnt,
             COALESCE(SUM(removed = 1), 0) AS greenCnt,
             COALESCE(SUM(star = 1 AND removed = 0), 0) AS starCnt
      FROM ai_review_wrong_item
      WHERE deleted = 0 AND plan_id IN
      <foreach collection="planIds" item="pid" open="(" separator="," close=")">#{pid}</foreach>
      GROUP BY plan_id
      </script>
      """)
  List<Map<String, Object>> planWrongAgg(@Param("planIds") List<Long> planIds);
}
