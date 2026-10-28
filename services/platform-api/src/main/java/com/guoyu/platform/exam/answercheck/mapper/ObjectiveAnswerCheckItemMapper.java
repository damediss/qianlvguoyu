package com.guoyu.platform.exam.answercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 明细认领、结果写入与重试复位（方案 §11.3 / §11.6）。
 *
 * <p>认领与结果写入都必须带批次 run_id 条件；旧 Worker 晚到写入影响 0 行，
 * 不得覆盖新结果，也不得重复增加批次结果计数。
 */
@Mapper
public interface ObjectiveAnswerCheckItemMapper extends BaseMapper<ObjectiveAnswerCheckItemEntity> {

  /**
   * 快照批量插入（每批 ≤200 行，自定义 SQL，禁止 Db.saveBatch）；审计字段显式传入。
   */
  @org.apache.ibatis.annotations.Insert("""
      <script>
      INSERT INTO exam_objective_answer_check_item (
        id, tenant_id, batch_id, detail_id, question_type, source, work_type_id, work_type_title,
        level_code, course_id, course_title, knowledge_id, knowledge_path, content_snapshot,
        stem_text, answer_snapshot, resolve_snapshot, option_snapshot_json, source_hash,
        content_quality, content_warnings_json, process_status, result_code, invalid_reason,
        retryable, attempts, version, review_status, created_at, updated_at, created_by,
        updated_by, deleted
      ) VALUES
      <foreach collection="list" item="it" separator=",">
        (#{it.id}, #{tenantId}, #{it.batchId}, #{it.detailId}, #{it.questionType}, #{it.source},
         #{it.workTypeId}, #{it.workTypeTitle}, #{it.levelCode}, #{it.courseId},
         #{it.courseTitle}, #{it.knowledgeId}, #{it.knowledgePath}, #{it.contentSnapshot},
         #{it.stemText}, #{it.answerSnapshot}, #{it.resolveSnapshot}, #{it.optionSnapshotJson},
         #{it.sourceHash}, #{it.contentQuality}, #{it.contentWarningsJson}, #{it.processStatus},
         #{it.resultCode}, #{it.invalidReason}, #{it.retryable}, #{it.attempts}, #{it.version},
         #{it.reviewStatus}, NOW(), NOW(), #{userId}, #{userId}, 0)
      </foreach>
      </script>
      """)
  int insertBatch(@Param("list") List<ObjectiveAnswerCheckItemEntity> items,
      @Param("tenantId") Long tenantId, @Param("userId") Long userId);

  /** 认领：只有 pending 且批次持有 run_id、running 且租约有效才能成功；影响 1 行后 attempts 即 claimAttempt。 */
  @Update("""
      UPDATE exam_objective_answer_check_item i
      SET i.process_status = 'processing', i.attempts = i.attempts + 1,
          i.version = i.version + 1, i.updated_at = NOW()
      WHERE i.id = #{id} AND i.deleted = 0 AND i.process_status = 'pending'
        AND EXISTS (
          SELECT 1 FROM exam_objective_answer_check_batch b
          WHERE b.id = i.batch_id AND b.run_id = #{runId} AND b.status = 'running'
            AND b.lease_until >= NOW())
      """)
  int claim(@Param("id") Long id, @Param("runId") Long runId);

  @Select("""
      SELECT * FROM exam_objective_answer_check_item WHERE id = #{id} AND deleted = 0
      """)
  ObjectiveAnswerCheckItemEntity findById(@Param("id") Long id);

  @Update("""
      UPDATE exam_objective_answer_check_item i
      SET i.process_status = 'success', i.result_code = #{it.resultCode},
          i.consistent = #{it.consistent}, i.original_answer_keys = #{it.originalAnswerKeys},
          i.ai_answer_keys = #{it.aiAnswerKeys}, i.reference_confidence = #{it.referenceConfidence},
          i.ai_confidence = #{it.aiConfidence}, i.min_margin = #{it.minMargin},
          i.option_results_json = #{it.optionResultsJson},
          i.decision_warning = #{it.decisionWarning}, i.invalid_reason = NULL,
          i.error_code = NULL, i.error_message = NULL, i.retryable = 0,
          i.checked_at = NOW(), i.updated_at = NOW(), i.updated_by = #{userId},
          i.version = i.version + 1
      WHERE i.id = #{it.id} AND i.deleted = 0 AND i.process_status = 'processing'
        AND i.attempts = #{claimAttempt}
        AND EXISTS (
          SELECT 1 FROM exam_objective_answer_check_batch b
          WHERE b.id = i.batch_id AND b.run_id = #{runId}
            AND b.status IN ('running', 'pausing', 'failed'))
      """)
  int writeSuccess(@Param("it") ObjectiveAnswerCheckItemEntity item,
      @Param("runId") Long runId, @Param("claimAttempt") Integer claimAttempt,
      @Param("userId") Long userId);

  @Update("""
      UPDATE exam_objective_answer_check_item i
      SET i.process_status = 'invalid', i.result_code = 'invalid', i.retryable = 0,
          i.invalid_reason = #{it.invalidReason}, i.option_results_json = NULL,
          i.checked_at = NOW(), i.updated_at = NOW(), i.updated_by = #{userId},
          i.version = i.version + 1
      WHERE i.id = #{it.id} AND i.deleted = 0 AND i.process_status = 'processing'
        AND i.attempts = #{claimAttempt}
        AND EXISTS (
          SELECT 1 FROM exam_objective_answer_check_batch b
          WHERE b.id = i.batch_id AND b.run_id = #{runId}
            AND b.status IN ('running', 'pausing', 'failed'))
      """)
  int writeInvalid(@Param("it") ObjectiveAnswerCheckItemEntity item,
      @Param("runId") Long runId, @Param("claimAttempt") Integer claimAttempt,
      @Param("userId") Long userId);

  @Update("""
      UPDATE exam_objective_answer_check_item i
      SET i.process_status = 'failed', i.result_code = 'failed', i.retryable = #{it.retryable},
          i.error_code = #{it.errorCode}, i.error_message = #{it.errorMessage},
          i.option_results_json = NULL, i.decision_warning = NULL,
          i.checked_at = NOW(), i.updated_at = NOW(), i.updated_by = #{userId},
          i.version = i.version + 1
      WHERE i.id = #{it.id} AND i.deleted = 0 AND i.process_status = 'processing'
        AND i.attempts = #{claimAttempt}
        AND EXISTS (
          SELECT 1 FROM exam_objective_answer_check_batch b
          WHERE b.id = i.batch_id AND b.run_id = #{runId}
            AND b.status IN ('running', 'pausing', 'failed'))
      """)
  int writeFailed(@Param("it") ObjectiveAnswerCheckItemEntity item,
      @Param("runId") Long runId, @Param("claimAttempt") Integer claimAttempt,
      @Param("userId") Long userId);

  /** 投递拒绝补偿：把刚认领的行恢复 pending（version 递增，attempts 保留）。 */
  @Update("""
      UPDATE exam_objective_answer_check_item
      SET process_status = 'pending', version = version + 1, updated_at = NOW(),
          updated_by = #{userId}
      WHERE id = #{id} AND deleted = 0 AND process_status = 'processing' AND attempts = #{claimAttempt}
      """)
  int restorePending(@Param("id") Long id, @Param("claimAttempt") Integer claimAttempt,
      @Param("userId") Long userId);

  /**
   * 失败重试：只复位 retryable=1 的 failed 行；清空旧 AI/复核字段，保留内容质量确定性快照。
   */
  @Update("""
      UPDATE exam_objective_answer_check_item
      SET process_status = 'pending', result_code = NULL, consistent = NULL,
          original_answer_keys = NULL, ai_answer_keys = NULL, reference_confidence = NULL,
          ai_confidence = NULL, min_margin = NULL, option_results_json = NULL,
          decision_warning = NULL, invalid_reason = NULL, error_code = NULL, error_message = NULL,
          checked_at = NULL, review_status = 'unreviewed', review_note = NULL,
          reviewed_by = NULL, reviewed_at = NULL, retryable = 0,
          version = version + 1, updated_at = NOW(), updated_by = #{userId}
      WHERE batch_id = #{batchId} AND deleted = 0 AND process_status = 'failed' AND retryable = 1
      """)
  int resetFailedToPending(@Param("batchId") Long batchId, @Param("userId") Long userId);

  @Select("""
      SELECT COUNT(*) FROM exam_objective_answer_check_item
      WHERE batch_id = #{batchId} AND deleted = 0 AND process_status = #{status}
      """)
  int countByProcessStatus(@Param("batchId") Long batchId, @Param("status") String status);

  @Select("""
      SELECT COUNT(*) FROM exam_objective_answer_check_item
      WHERE batch_id = #{batchId} AND deleted = 0 AND result_code = #{resultCode}
      """)
  int countByResultCode(@Param("batchId") Long batchId, @Param("resultCode") String resultCode);

  @Select("""
      SELECT id FROM exam_objective_answer_check_item
      WHERE batch_id = #{batchId} AND deleted = 0 AND process_status = 'pending'
      ORDER BY id LIMIT #{limit}
      """)
  List<Long> listPendingIds(@Param("batchId") Long batchId, @Param("limit") int limit);

  /** 恢复扫描：租约已过期批次的遗留 processing 回到 pending，attempts 不清零。 */
  @Update("""
      UPDATE exam_objective_answer_check_item
      SET process_status = 'pending', version = version + 1, updated_at = NOW(), updated_by = #{userId}
      WHERE batch_id = #{batchId} AND deleted = 0 AND process_status = 'processing'
      """)
  int recoverProcessing(@Param("batchId") Long batchId, @Param("userId") Long userId);

  /** 人工复核：id + version + 终态条件更新，version+1；pending/processing 禁止复核。 */
  @Update("""
      UPDATE exam_objective_answer_check_item
      SET review_status = #{reviewStatus}, review_note = #{note},
          reviewed_by = #{userId},
          reviewed_at = IF(#{reviewStatus} = 'unreviewed', NULL, NOW()),
          version = version + 1, updated_at = NOW(), updated_by = #{userId}
      WHERE id = #{id} AND batch_id = #{batchId} AND deleted = 0 AND version = #{expectedVersion}
        AND process_status IN ('success', 'invalid', 'failed')
      """)
  int updateReview(@Param("id") Long id, @Param("batchId") Long batchId,
      @Param("expectedVersion") Integer expectedVersion, @Param("reviewStatus") String reviewStatus,
      @Param("note") String note, @Param("userId") Long userId);
}
