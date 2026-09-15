package com.guoyu.platform.competition.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CompetitionFormalExamSessionMapper
    extends BaseMapper<CompetitionFormalExamSessionEntity> {

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE tenant_id = #{tenantId} AND stage_id = #{stageId}
        AND exam_type = #{examType} AND occupancy_token = 0 AND deleted = 0
      ORDER BY id DESC
      FOR UPDATE
      """)
  List<CompetitionFormalExamSessionEntity> selectActiveForUpdate(
      @Param("tenantId") Long tenantId,
      @Param("stageId") Long stageId,
      @Param("examType") String examType);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE tenant_id = #{tenantId} AND stage_id = #{stageId}
        AND exam_type = #{examType} AND occupancy_token = 0 AND deleted = 0
      ORDER BY id DESC
      """)
  List<CompetitionFormalExamSessionEntity> selectActive(
      @Param("tenantId") Long tenantId,
      @Param("stageId") Long stageId,
      @Param("examType") String examType);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE tenant_id = #{tenantId} AND stage_id = #{stageId} AND deleted = 0
      ORDER BY occupancy_token ASC, id DESC
      LIMIT 1
      """)
  CompetitionFormalExamSessionEntity selectLatestByStage(
      @Param("tenantId") Long tenantId,
      @Param("stageId") Long stageId);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE tenant_id = #{tenantId} AND stage_id = #{stageId}
        AND exam_type = #{examType} AND deleted = 0
      ORDER BY id DESC
      LIMIT 1
      """)
  CompetitionFormalExamSessionEntity selectLatestByStageAndExamType(
      @Param("tenantId") Long tenantId,
      @Param("stageId") Long stageId,
      @Param("examType") String examType);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE id = #{id} AND deleted = 0
      FOR UPDATE
      """)
  CompetitionFormalExamSessionEntity selectByIdForUpdate(@Param("id") Long id);

  @Update("""
      UPDATE competition_formal_exam_session
      SET status = #{toStatus},
          actual_start_time = #{actualStartTime},
          deadline_at = TIMESTAMPADD(SECOND, duration_seconds, #{actualStartTime}),
          updated_at = #{updatedAt},
          updated_by = #{updatedBy}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND occupancy_token = 0
        AND deleted = 0
      """)
  int markRunningIfReady(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("actualStartTime") LocalDateTime actualStartTime,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("updatedBy") Long updatedBy);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT tenant_id
      FROM competition_formal_exam_session
      WHERE status = 'running'
        AND occupancy_token = 0
        AND deleted = 0
        AND deadline_at IS NOT NULL
        AND deadline_at <= #{now}
        AND (expire_claim_until IS NULL OR expire_claim_until <= #{now})
      """)
  List<Long> selectExpiredRunningTenantIds(@Param("now") LocalDateTime now);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE status = 'running'
        AND occupancy_token = 0
        AND deleted = 0
        AND deadline_at IS NOT NULL
        AND deadline_at <= #{now}
        AND (expire_claim_until IS NULL OR expire_claim_until <= #{now})
      ORDER BY id ASC
      """)
  List<CompetitionFormalExamSessionEntity> selectExpiredRunningSessions(@Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_formal_exam_session
      SET expire_claim_owner = #{owner},
          expire_claim_until = #{until},
          updated_at = #{now}
      WHERE id = #{id}
        AND status = 'running'
        AND occupancy_token = 0
        AND deleted = 0
        AND deadline_at IS NOT NULL
        AND deadline_at <= #{now}
        AND (expire_claim_until IS NULL OR expire_claim_until <= #{now})
      """)
  int claimExpiredSession(
      @Param("id") Long id,
      @Param("owner") String owner,
      @Param("until") LocalDateTime until,
      @Param("now") LocalDateTime now);

  @Update("""
      UPDATE competition_formal_exam_session
      SET status = #{toStatus},
          actual_end_time = #{actualEndTime},
          occupancy_token = #{occupancyToken},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = #{fromStatus}
        AND occupancy_token = 0
        AND deleted = 0
      """)
  int markFinishedIfRunning(
      @Param("id") Long id,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("actualEndTime") LocalDateTime actualEndTime,
      @Param("occupancyToken") Long occupancyToken,
      @Param("updatedAt") LocalDateTime updatedAt);

  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT DISTINCT tenant_id
      FROM competition_formal_exam_session
      WHERE status = 'finished'
        AND deleted = 0
        AND (
          scoring_status IN ('not_started', 'queued', 'failed')
          OR (
            scoring_status = 'scoring'
            AND scoring_started_at IS NOT NULL
            AND scoring_started_at <= #{stuckBefore}
          )
        )
      """)
  List<Long> selectScoringDueTenantIds(@Param("stuckBefore") LocalDateTime stuckBefore);

  @Select("""
      SELECT * FROM competition_formal_exam_session
      WHERE status = 'finished'
        AND deleted = 0
        AND (
          scoring_status IN ('not_started', 'queued', 'failed')
          OR (
            scoring_status = 'scoring'
            AND scoring_started_at IS NOT NULL
            AND scoring_started_at <= #{stuckBefore}
          )
        )
      ORDER BY id ASC
      """)
  List<CompetitionFormalExamSessionEntity> selectScoringDueSessions(
      @Param("stuckBefore") LocalDateTime stuckBefore);

  @Update("""
      UPDATE competition_formal_exam_session
      SET scoring_status = 'failed',
          scoring_error = #{scoringError},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'finished'
        AND scoring_status = 'scoring'
        AND scoring_started_at IS NOT NULL
        AND scoring_started_at <= #{stuckBefore}
        AND deleted = 0
      """)
  int reclaimStuckScoring(
      @Param("id") Long id,
      @Param("stuckBefore") LocalDateTime stuckBefore,
      @Param("scoringError") String scoringError,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE competition_formal_exam_session
      SET scoring_status = 'scoring',
          scoring_started_at = #{scoringStartedAt},
          scoring_error = NULL,
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'finished'
        AND scoring_status IN ('not_started', 'failed')
        AND deleted = 0
      """)
  int markScoringIfIdle(
      @Param("id") Long id,
      @Param("scoringStartedAt") LocalDateTime scoringStartedAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE competition_formal_exam_session
      SET scoring_status = 'completed',
          scored_time = #{scoredTime},
          scoring_error = NULL,
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'finished'
        AND scoring_status = 'scoring'
        AND deleted = 0
      """)
  int markScoringCompleted(
      @Param("id") Long id,
      @Param("scoredTime") LocalDateTime scoredTime,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE competition_formal_exam_session
      SET scoring_status = 'failed',
          scoring_error = #{scoringError},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'finished'
        AND scoring_status = 'scoring'
        AND deleted = 0
      """)
  int markScoringFailed(
      @Param("id") Long id,
      @Param("scoringError") String scoringError,
      @Param("updatedAt") LocalDateTime updatedAt);
}
