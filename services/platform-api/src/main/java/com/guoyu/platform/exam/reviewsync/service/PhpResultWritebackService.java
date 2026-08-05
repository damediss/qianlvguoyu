package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.integration.php.PhpExamQueryService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * PHP exam_result 异步回写（§4.7 / §8）。
 *
 * <p>口径（对齐 PHP trainsave）：type=题型、exam_plan_id=train_exam_list.id、paper_id=0、detail_id=0、
 * exam_id=题 id、platform_user_id、user_answer、get_score（对=题分/错=0）、
 * exam_type=5（AI 同步练习）、tenant_id=10159、create_time=NOW()、deleted=0。仅 INSERT。
 *
 * <p>失败置 php_synced=2 交补偿 job（每 5min 扫描 0/2 重试，连续失败记 error 死信日志）。
 * 线程边界：全部参数传入，不调 SecurityContext（AGENTS.md 异步陷阱）。
 */
@Slf4j
@Service
public class PhpResultWritebackService {

  /** 连续失败死信阈值（次数） */
  static final int DEAD_LETTER_THRESHOLD = 12;

  private final PhpExamQueryService phpExamQueryService;
  private final AiReviewAnswerMapper answerMapper;
  private final ThreadPoolTaskExecutor writebackExecutor;

  public PhpResultWritebackService(
      PhpExamQueryService phpExamQueryService,
      AiReviewAnswerMapper answerMapper,
      @Qualifier("reviewSyncExecutor") ThreadPoolTaskExecutor writebackExecutor) {
    this.phpExamQueryService = phpExamQueryService;
    this.answerMapper = answerMapper;
    this.writebackExecutor = writebackExecutor;
  }

  /** 作答后即时异步回写（失败置 2 交补偿） */
  public void writebackAsync(long answerId) {
    try {
      writebackExecutor.execute(() -> writebackOne(answerId));
    } catch (Exception e) {
      // 线程池拒绝（AbortPolicy）：落 2 交补偿，不影响主链路
      log.warn("[AI复习] 回写线程池拒绝，转补偿 job：answerId={}", answerId);
      markFailed(answerId);
    }
  }

  /** 单条回写（同步语义，补偿 job 与异步入口共用） */
  public void writebackOne(long answerId) {
    AiReviewAnswerEntity answer = answerMapper.selectById(answerId);
    if (answer == null || (answer.getPhpSynced() != null && answer.getPhpSynced() == 1)) {
      return;
    }
    try {
      AiReviewAnswerEntity update = new AiReviewAnswerEntity();
      update.setId(answer.getId());
      phpExamQueryService.insertExamResult(
          answer.getQuestionType(),
          answer.getExamPlanId() == null ? 0 : answer.getExamPlanId(),
          answer.getQuestionId(),
          answer.getPlatformUserId(),
          answer.getUserAnswer(),
          getScoreOf(answer));
      update.setPhpSynced(1);
      update.setPhpSyncTime(LocalDateTime.now());
      answerMapper.updateById(update);
    } catch (Exception e) {
      log.warn("[AI复习] 回写 PHP exam_result 失败：answerId={} error={}", answerId, e.getMessage());
      markFailed(answerId);
    }
  }

  /** 补偿 job：扫 php_synced IN (0,2) 重试，单批 ≤500 */
  public int compensateBatch() {
    List<AiReviewAnswerEntity> pending = answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .in(AiReviewAnswerEntity::getPhpSynced, 0, 2)
        .orderByAsc(AiReviewAnswerEntity::getId)
        .last("LIMIT 500"));
    int done = 0;
    for (AiReviewAnswerEntity answer : pending) {
      int before = answer.getPhpSynced() == null ? 0 : answer.getPhpSynced();
      if (before >= DEAD_LETTER_THRESHOLD + 2) {
        log.error("[AI复习] 回写死信：answerId={} phpSynced={}（连续失败 ≥{} 次，转人工）",
            answer.getId(), before, DEAD_LETTER_THRESHOLD);
        continue;
      }
      writebackOne(answer.getId());
      done++;
    }
    return done;
  }

  private void markFailed(long answerId) {
    AiReviewAnswerEntity answer = answerMapper.selectById(answerId);
    if (answer == null) {
      return;
    }
    AiReviewAnswerEntity update = new AiReviewAnswerEntity();
    update.setId(answerId);
    update.setPhpSynced((answer.getPhpSynced() == null ? 0 : answer.getPhpSynced()) + 2);
    answerMapper.updateById(update);
  }

  /** 回写 get_score 口径：优先实得分（严格判分部分分）；无实得分则 对=题分/错=0 */
  private static BigDecimal getScoreOf(AiReviewAnswerEntity answer) {
    if (answer.getEarnedScore() != null) {
      return answer.getEarnedScore();
    }
    return answer.getIsCorrect() != null && answer.getIsCorrect() == 1
        && answer.getQuestionScore() != null ? answer.getQuestionScore() : BigDecimal.ZERO;
  }
}
