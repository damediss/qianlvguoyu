package com.guoyu.platform.exam.answercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 批次与租约 SQL（方案 §11）。
 *
 * <p>所有状态推进都是条件 UPDATE，不在 Java 里先读后无条件覆盖；时间比较统一用数据库 NOW()，
 * 避免多节点时钟差。
 */
@Mapper
public interface ObjectiveAnswerCheckBatchMapper extends BaseMapper<ObjectiveAnswerCheckBatchEntity> {

  @Select("SELECT * FROM exam_objective_answer_check_batch WHERE id = #{id} AND deleted = 0")
  ObjectiveAnswerCheckBatchEntity findById(@Param("id") Long id);

  @Select("""
      SELECT * FROM exam_objective_answer_check_batch
      WHERE id = #{id} AND deleted = 0
      FOR UPDATE
      """)
  ObjectiveAnswerCheckBatchEntity lockById(@Param("id") Long id);

  @Select("""
      SELECT * FROM exam_objective_answer_check_batch
      WHERE active_tenant_id = #{tenantId} AND deleted = 0
      ORDER BY id DESC LIMIT 1
      """)
  ObjectiveAnswerCheckBatchEntity findActive(@Param("tenantId") Long tenantId);

  /**
   * 取得活动槽并发起/恢复编排：paused 可继续，processing=0 的 failed 也可继续，
   * done 仅允许"重试失败项"路径进入（方案 §11.1 done→running）；
   * 同租户已有其他活动批次时不更新（返回 0）。
   */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET active_tenant_id = #{tenantId}, run_id = #{runId}, owner_id = #{ownerId},
          lease_until = DATE_ADD(NOW(), INTERVAL 60 SECOND),
          status = 'running', started_at = COALESCE(started_at, NOW()), finished_at = NULL,
          fail_reason = NULL, updated_at = NOW(), updated_by = #{userId}
      WHERE id = #{batchId} AND deleted = 0
        AND (status IN ('pending', 'paused', 'done')
             OR (status = 'failed' AND NOT EXISTS (
                   SELECT 1 FROM exam_objective_answer_check_item i
                   WHERE i.batch_id = #{batchId} AND i.process_status = 'processing')))
      """)
  int acquire(@Param("batchId") Long batchId, @Param("tenantId") Long tenantId,
      @Param("runId") Long runId, @Param("ownerId") String ownerId, @Param("userId") Long userId);

  /** 心跳续租；影响 0 行表示所有权已失效，编排器必须停止派发与收尾。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch b
      SET b.lease_until = DATE_ADD(NOW(), INTERVAL 60 SECOND)
      WHERE b.id = #{batchId} AND b.run_id = #{runId} AND b.owner_id = #{ownerId}
        AND b.lease_until >= NOW()
        AND (b.status IN ('pending', 'running', 'pausing')
             OR (b.status = 'failed' AND EXISTS (
                   SELECT 1 FROM exam_objective_answer_check_item i
                   WHERE i.batch_id = b.id AND i.process_status = 'processing')))
      """)
  int renewLease(@Param("batchId") Long batchId, @Param("runId") Long runId,
      @Param("ownerId") String ownerId);

  /** 失败锁存：401 或连续 5 次失败时以 run_id 条件原子更新，后续成功不得撤销。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET status = 'failed', fail_reason = #{reason}, updated_at = NOW()
      WHERE id = #{batchId} AND run_id = #{runId} AND deleted = 0
        AND status IN ('pending', 'running', 'pausing')
      """)
  int markFailed(@Param("batchId") Long batchId, @Param("runId") Long runId,
      @Param("reason") String reason);

  /** 无 processing 时释放活动槽与所有权（failed 归零后同样释放）。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch b
      SET b.active_tenant_id = NULL, b.run_id = NULL, b.owner_id = NULL, b.lease_until = NULL,
          b.updated_at = NOW()
      WHERE b.id = #{batchId} AND b.deleted = 0
        AND NOT EXISTS (SELECT 1 FROM exam_objective_answer_check_item i
                        WHERE i.batch_id = b.id AND i.process_status = 'processing')
      """)
  int releaseActivity(@Param("batchId") Long batchId);

  /** 收尾：paused/done 都不覆盖 failed；必须带原状态 + run_id 条件。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch b
      SET b.status = #{status}, b.finished_at = NOW(), b.active_tenant_id = NULL,
          b.run_id = NULL, b.owner_id = NULL, b.lease_until = NULL, b.updated_at = NOW()
      WHERE b.id = #{batchId} AND b.run_id = #{runId} AND b.deleted = 0
        AND b.status IN ('running', 'pausing')
        AND NOT EXISTS (SELECT 1 FROM exam_objective_answer_check_item i
                        WHERE i.batch_id = b.id AND i.process_status = 'processing')
      """)
  int finish(@Param("batchId") Long batchId, @Param("runId") Long runId,
      @Param("status") String status);

  /** 结果计数只由当前 run + 有效认领推进；调用方必须确认明细写入影响 1 行。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET done = done + 1,
          consistent_high = consistent_high + IF(#{resultCode} = 'consistent_high', 1, 0),
          mismatch_high = mismatch_high + IF(#{resultCode} = 'mismatch_high', 1, 0),
          low_confidence = low_confidence + IF(#{resultCode} = 'low_confidence', 1, 0),
          invalid = invalid + IF(#{resultCode} = 'invalid', 1, 0),
          failed = failed + IF(#{resultCode} = 'failed', 1, 0),
          updated_at = NOW()
      WHERE id = #{batchId} AND run_id = #{runId} AND deleted = 0
      """)
  int incrementResult(@Param("batchId") Long batchId, @Param("runId") Long runId,
      @Param("resultCode") String resultCode);

  /** 实际用量按“每个实际发出的逻辑块”累计；晚到被 fencing 的调用同样计入，但不推进结果计数。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET call_count = call_count + 1,
          input_tokens_exact = input_tokens_exact + #{inputExact},
          input_tokens_estimated = input_tokens_estimated + #{inputEstimated},
          output_tokens = output_tokens + #{outputTokens},
          usage_estimated_calls = usage_estimated_calls + #{usageEstimatedCalls},
          model_duration_ms = model_duration_ms + #{durationMs},
          updated_at = NOW()
      WHERE id = #{batchId} AND deleted = 0
      """)
  int addUsage(@Param("batchId") Long batchId, @Param("inputExact") long inputExact,
      @Param("inputEstimated") long inputEstimated, @Param("outputTokens") long outputTokens,
      @Param("usageEstimatedCalls") int usageEstimatedCalls, @Param("durationMs") long durationMs);

  /** 请求暂停：只允许 pending/running → pausing。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET status = 'pausing', updated_at = NOW()
      WHERE id = #{batchId} AND deleted = 0 AND status IN ('pending', 'running')
      """)
  int requestPause(@Param("batchId") Long batchId);

  /** 无在飞行行时把 pausing 收尾为 paused（不覆盖 failed）。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch b
      SET b.status = 'paused', b.finished_at = NOW(), b.active_tenant_id = NULL,
          b.run_id = NULL, b.owner_id = NULL, b.lease_until = NULL, b.updated_at = NOW()
      WHERE b.id = #{batchId} AND b.deleted = 0 AND b.status = 'pausing'
        AND NOT EXISTS (SELECT 1 FROM exam_objective_answer_check_item i
                        WHERE i.batch_id = b.id AND i.process_status = 'processing')
      """)
  int finishPausedIfIdle(@Param("batchId") Long batchId);

  /** 失败重试时同事务回减 done/failed，保持 total=done+pending+processing。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET done = GREATEST(done - #{count}, 0), failed = GREATEST(failed - #{count}, 0),
          updated_at = NOW()
      WHERE id = #{batchId} AND deleted = 0
      """)
  int decrementForRetry(@Param("batchId") Long batchId, @Param("count") int count);

  /**
   * 恢复扫描：只取租约过期的活动批次，以及仍有 processing 的过期 failed。
   * 跨租户查询必须走受限的 @InterceptorIgnore 方法，不得暴露给 Controller。
   */
  @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT b.id AS id, b.tenant_id AS tenantId, b.status AS status, b.run_id AS runId,
             b.lease_until AS leaseUntil
      FROM exam_objective_answer_check_batch b
      WHERE b.deleted = 0 AND b.lease_until IS NOT NULL AND b.lease_until < NOW()
        AND (b.status IN ('pending', 'running', 'pausing')
             OR (b.status = 'failed' AND EXISTS (
                   SELECT 1 FROM exam_objective_answer_check_item i
                   WHERE i.batch_id = b.id AND i.process_status = 'processing')))
      ORDER BY b.lease_until
      LIMIT 50
      """)
  List<RecoveryCandidate> listExpired();

  /** 过期批次交还：failed 保持 failed，其余回到 paused；清空活动槽与所有权。 */
  @Update("""
      UPDATE exam_objective_answer_check_batch
      SET finished_at = IF(status = 'failed', finished_at, NOW()),
          status = IF(status = 'failed', 'failed', #{targetStatus}),
          active_tenant_id = NULL, run_id = NULL, owner_id = NULL, lease_until = NULL,
          updated_at = NOW()
      WHERE id = #{batchId} AND deleted = 0 AND lease_until IS NOT NULL AND lease_until < NOW()
      """)
  int resetExpired(@Param("batchId") Long batchId, @Param("targetStatus") String targetStatus);

  /** 跨租户恢复候选：只读最小字段。 */
  class RecoveryCandidate {
    private Long id;
    private Long tenantId;
    private String status;
    private Long runId;
    private java.time.LocalDateTime leaseUntil;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getTenantId() {
      return tenantId;
    }

    public void setTenantId(Long tenantId) {
      this.tenantId = tenantId;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public Long getRunId() {
      return runId;
    }

    public void setRunId(Long runId) {
      this.runId = runId;
    }

    public java.time.LocalDateTime getLeaseUntil() {
      return leaseUntil;
    }

    public void setLeaseUntil(java.time.LocalDateTime leaseUntil) {
      this.leaseUntil = leaseUntil;
    }
  }
}
