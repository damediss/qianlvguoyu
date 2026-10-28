package com.guoyu.platform.exam.answercheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckItemMapper;
import com.guoyu.platform.exam.answercheck.service.ObjectiveAnswerCheckBatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * verifier 补充（真实 MySQL）：done 批次"重试失败项"必须能取得新 run。
 *
 * <p>回归背景：acquire 的 SQL 状态白名单曾漏掉 done，单元测试 mock 了 acquire 所以测不出；
 * 本用例直接走真实 SQL，覆盖方案 §11.1 的 done → running（仅 retry-failed 确实复位了行）。
 */
@SpringBootTest
@ActiveProfiles("courseware-e2e")
class ObjectiveAnswerCheckRetryFromDoneIT {

  private static final long TENANT_ID = 10159L;
  private static final long BATCH_ID = 990_029_001L;
  private static final long ITEM_ID = 990_029_002L;
  private static final int DETAIL_ID = 990_029_003;

  @Autowired
  private ObjectiveAnswerCheckBatchService batchService;
  @Autowired
  private ObjectiveAnswerCheckBatchMapper batchMapper;
  @Autowired
  private ObjectiveAnswerCheckItemMapper itemMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void retryFailedFromDoneBatchStartsNewRun() {
    TenantContextHolder.setTenantId(TENANT_ID);
    try {
      cleanup();
      jdbcTemplate.update(
          "INSERT INTO exam_objective_answer_check_batch (id, tenant_id, selection_mode, filter_json,"
              + " status, total, done, failed, model_id, prompt_version, confidence_threshold,"
              + " margin_threshold, created_by, updated_by) VALUES (?, ?, 'ids', '{}', 'done', 1, 1, 1,"
              + " 'ateve-jev-v1', 'objective-answer-v1', 0.800, 0.200, 7, 7)",
          BATCH_ID, TENANT_ID);
      jdbcTemplate.update(
          "INSERT INTO exam_objective_answer_check_item (id, tenant_id, batch_id, detail_id,"
              + " question_type, stem_text, option_snapshot_json, source_hash, process_status,"
              + " result_code, retryable, attempts, version, review_status, created_by, updated_by)"
              + " VALUES (?, ?, ?, ?, 'single', '题干', '[]', 'hash', 'failed', 'failed', 1, 1, 0,"
              + " 'unreviewed', 7, 7)",
          ITEM_ID, TENANT_ID, BATCH_ID, DETAIL_ID);

      int reset = batchService.retryFailed(BATCH_ID, TENANT_ID, 7L);

      assertThat(reset).isEqualTo(1);
      ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(BATCH_ID);
      assertThat(batch.getStatus()).isEqualTo("running");
      assertThat(batch.getRunId()).isNotNull();
      assertThat(batch.getActiveTenantId()).isEqualTo(TENANT_ID);
      assertThat(batch.getDone()).isZero();
      assertThat(batch.getFailed()).isZero();
      ObjectiveAnswerCheckItemEntity item = itemMapper.findById(ITEM_ID);
      assertThat(item.getProcessStatus()).isEqualTo("pending");
      assertThat(item.getResultCode()).isNull();
      assertThat(item.getRetryable()).isZero();
      assertThat(item.getVersion()).isEqualTo(1);
    } finally {
      cleanup();
      TenantContextHolder.clear();
    }
  }

  private void cleanup() {
    jdbcTemplate.update("DELETE FROM exam_objective_answer_check_item WHERE id = ?", ITEM_ID);
    jdbcTemplate.update("DELETE FROM exam_objective_answer_check_batch WHERE id = ?", BATCH_ID);
  }
}
