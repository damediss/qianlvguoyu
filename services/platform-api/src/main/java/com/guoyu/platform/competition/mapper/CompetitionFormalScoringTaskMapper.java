package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalScoringTaskMapper
    extends BaseMapper<CompetitionFormalScoringTaskEntity> {

  @Select("""
      SELECT * FROM competition_formal_scoring_task
      WHERE candidate_exam_id = #{candidateExamId} AND result_revision_no = #{revisionNo}
        AND deleted = 0 LIMIT 1
      """)
  CompetitionFormalScoringTaskEntity selectByCandidateAndRevision(
      @Param("candidateExamId") Long candidateExamId, @Param("revisionNo") int revisionNo);

  @Update("""
      UPDATE competition_formal_scoring_task
      SET task_status = 'processing', attempt_count = attempt_count + 1,
          lease_owner = #{owner}, lease_until = #{leaseUntil},
          started_at = COALESCE(started_at, #{now}), last_error = NULL, updated_at = #{now}
      WHERE id = #{id} AND deleted = 0
        AND task_status IN ('pending', 'retry') AND next_attempt_at <= #{now}
        AND (lease_until IS NULL OR lease_until <= #{now})
      """)
  int claim(@Param("id") Long id, @Param("owner") String owner,
      @Param("leaseUntil") LocalDateTime leaseUntil, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_formal_scoring_task
      SET task_status = 'completed', completed_at = #{now}, lease_owner = NULL,
          lease_until = NULL, last_error = NULL, updated_at = #{now}
      WHERE id = #{id} AND task_status = 'processing' AND lease_owner = #{owner} AND deleted = 0
      """)
  int complete(@Param("id") Long id, @Param("owner") String owner,
      @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_formal_scoring_task
      SET task_status = #{status}, next_attempt_at = #{nextAttemptAt},
          lease_owner = NULL, lease_until = NULL, last_error = #{error}, updated_at = #{now}
      WHERE id = #{id} AND task_status = 'processing' AND lease_owner = #{owner} AND deleted = 0
      """)
  int fail(@Param("id") Long id, @Param("owner") String owner,
      @Param("status") String status, @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
      @Param("error") String error, @Param("now") LocalDateTime now);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT tenant_id FROM competition_formal_scoring_task
      WHERE deleted = 0 AND (
        (task_status IN ('pending','retry') AND next_attempt_at <= #{now})
        OR (task_status = 'processing' AND lease_until <= #{now})
      )
      """)
  List<Long> selectRecoverableTenantIds(@Param("now") LocalDateTime now);

  @Select("""
      SELECT * FROM competition_formal_scoring_task
      WHERE deleted = 0 AND (
        (task_status IN ('pending','retry') AND next_attempt_at <= #{now})
        OR (task_status = 'processing' AND lease_until <= #{now})
      ) ORDER BY id LIMIT #{limit}
      """)
  List<CompetitionFormalScoringTaskEntity> selectRecoverable(
      @Param("now") LocalDateTime now, @Param("limit") int limit);

  @Update("""
      UPDATE competition_formal_scoring_task
      SET task_status = 'retry', lease_owner = NULL, lease_until = NULL,
          next_attempt_at = #{now}, last_error = '处理租约超时，已恢复', updated_at = #{now}
      WHERE id = #{id} AND task_status = 'processing' AND lease_until <= #{now} AND deleted = 0
      """)
  int recoverExpiredLease(@Param("id") Long id, @Param("now") LocalDateTime now);

  @Select("""
      SELECT COUNT(*) FROM competition_formal_scoring_task t
      WHERE t.exam_session_id = #{sessionId} AND t.deleted = 0
        AND t.result_revision_no = (
          SELECT MAX(latest.result_revision_no)
          FROM competition_formal_scoring_task latest
          WHERE latest.tenant_id = t.tenant_id
            AND latest.candidate_exam_id = t.candidate_exam_id
            AND latest.deleted = 0
        )
        AND t.task_status <> 'completed'
      """)
  int countIncompleteBySession(@Param("sessionId") Long sessionId);

  @Select("""
      SELECT COUNT(DISTINCT candidate_exam_id) FROM competition_formal_scoring_task
      WHERE exam_session_id = #{sessionId} AND deleted = 0
      """)
  int countBySession(@Param("sessionId") Long sessionId);

  @Select("""
      SELECT COUNT(*) FROM competition_formal_scoring_task t
      WHERE t.exam_session_id = #{sessionId} AND t.deleted = 0
        AND t.result_revision_no = (
          SELECT MAX(latest.result_revision_no)
          FROM competition_formal_scoring_task latest
          WHERE latest.tenant_id = t.tenant_id
            AND latest.candidate_exam_id = t.candidate_exam_id
            AND latest.deleted = 0
        )
        AND t.task_status = 'failed'
      """)
  int countFailedBySession(@Param("sessionId") Long sessionId);
}
