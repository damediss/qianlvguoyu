package com.guoyu.platform.trainingdepartment.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 运营数据面板「同步练习」分区聚合查询（《运营数据面板_同步练习统计面板方案》v0.1 §5）。
 *
 * <p>只读聚合；tenant_id 由 MyBatis-Plus 多租户插件自动追加（ai_review_* / exam_knowledge_structure
 * 不在忽略名单），禁止手写 tenant_id 条件。范围/明细口径复用 ReviewSyncStatsMapper，
 * 本 Mapper 只承载明细页没有的增量口径。
 */
public interface ReviewSyncPanelMapper {

  /** 计划状态分布 + 中断人次（interrupt_count>0 的计划数） */
  @Select("""
      SELECT status AS status,
             COUNT(*) AS cnt,
             COALESCE(SUM(interrupt_count > 0), 0) AS interruptCnt
      FROM ai_review_plan
      WHERE deleted = 0
      GROUP BY status
      """)
  List<Map<String, Object>> planStatusAgg();

  /** 模考汇总：完成套数 / 平均分 / 及格数（status='done'） */
  @Select("""
      SELECT COUNT(*) AS doneCnt,
             AVG(score) AS avgScore,
             COALESCE(SUM(score >= pass_score), 0) AS passCnt
      FROM ai_review_mock_paper
      WHERE deleted = 0 AND status = 'done'
      """)
  Map<String, Object> mockSummary();

  /** 近 7 日活跃学员数（含今天，distinct） */
  @Select("""
      SELECT COUNT(DISTINCT platform_user_id) AS cnt
      FROM ai_review_answer
      WHERE deleted = 0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
      """)
  Long activeUsers7d();

  /** 近 7 日新增计划数（含今天，按 created_at） */
  @Select("""
      SELECT COUNT(*) AS cnt
      FROM ai_review_plan
      WHERE deleted = 0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
      """)
  Long newPlans7d();

  /** 累计作答总行数（不限时间，面板"累计作答"口径） */
  @Select("SELECT COUNT(*) AS cnt FROM ai_review_answer WHERE deleted = 0")
  Long totalAnswers();

  /**
   * 范围内知识点作答聚合（薄弱 Top5 用）。明细页 knowledgeAgg 无时间过滤，
   * 面板需要近 N 天口径，另起一条不污染原查询。
   */
  @Select("""
      SELECT knowledge_id AS knowledgeId,
             COUNT(*) AS answers,
             COALESCE(SUM(is_correct), 0) AS corrects
      FROM ai_review_answer
      WHERE deleted = 0 AND knowledge_id IS NOT NULL
        AND created_at >= #{from} AND created_at < DATE_ADD(#{to}, INTERVAL 1 DAY)
      GROUP BY knowledge_id
      """)
  List<Map<String, Object>> knowledgeAggInRange(@Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** 按 id 批量加载知识节点（L3→L1 祖先链组装用） */
  @Select("""
      <script>
      SELECT id, parent_id AS parentId, level, title
      FROM exam_knowledge_structure
      WHERE deleted = 0 AND id IN
      <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
      </script>
      """)
  List<Map<String, Object>> selectKnowledgeNodesByIds(@Param("ids") List<Long> ids);
}
