package com.guoyu.platform.exam.answercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckCallLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 调用审计。异步链写库必须显式给出 tenantId/userId 与时间，禁止依赖 SecurityContext 自动填充。
 */
@Mapper
public interface ObjectiveAnswerCheckCallLogMapper
    extends BaseMapper<ObjectiveAnswerCheckCallLogEntity> {

  @Insert("""
      INSERT INTO exam_objective_answer_check_call_log (
        id, tenant_id, batch_id, item_id, detail_id, run_id, claim_attempt, call_id,
        upstream_request_id, tool_name, prompt_version, chunk_index, chunk_count, question_type,
        requested_model, response_model, confidence_definition, prompt_sha256, prompt_preview,
        input_tokens, output_tokens, usage_estimated, duration_ms, status, error_code, error_message,
        created_at, updated_at, created_by, updated_by, deleted
      ) VALUES (
        #{e.id}, #{tenantId}, #{e.batchId}, #{e.itemId}, #{e.detailId}, #{e.runId},
        #{e.claimAttempt}, #{e.callId}, #{e.upstreamRequestId}, #{e.toolName}, #{e.promptVersion},
        #{e.chunkIndex}, #{e.chunkCount}, #{e.questionType}, #{e.requestedModel},
        #{e.responseModel}, #{e.confidenceDefinition}, #{e.promptSha256}, #{e.promptPreview},
        #{e.inputTokens}, #{e.outputTokens}, #{e.usageEstimated}, #{e.durationMs}, #{e.status},
        #{e.errorCode}, #{e.errorMessage}, NOW(), NOW(), #{userId}, #{userId}, 0
      )
      """)
  int insertExplicit(@Param("e") ObjectiveAnswerCheckCallLogEntity entity,
      @Param("tenantId") Long tenantId, @Param("userId") Long userId);

  @Select("""
      SELECT * FROM exam_objective_answer_check_call_log
      WHERE item_id = #{itemId} AND deleted = 0
      ORDER BY id
      """)
  List<ObjectiveAnswerCheckCallLogEntity> listByItem(@Param("itemId") Long itemId);
}
