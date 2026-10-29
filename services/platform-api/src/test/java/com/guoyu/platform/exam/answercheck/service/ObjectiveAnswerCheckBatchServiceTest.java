package com.guoyu.platform.exam.answercheck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.answercheck.config.ObjectiveAnswerCheckProperties;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckItemMapper;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 批次状态机（方案 §16.1-G 的关键分支）：暂停/继续/重试/投递拒绝补偿。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObjectiveAnswerCheckBatchServiceTest {

  @Mock
  private ObjectiveAnswerCheckBatchMapper batchMapper;
  @Mock
  private ObjectiveAnswerCheckItemMapper itemMapper;
  @Mock
  private ObjectiveAnswerCheckAuditService auditService;
  @Mock
  private ObjectiveAnswerCheckAiService aiService;
  @Mock
  private ObjectiveAnswerCheckDomainService domainService;
  @Mock
  private ThreadPoolTaskExecutor orchestrationExecutor;
  @Mock
  private ThreadPoolTaskExecutor workerExecutor;
  @Mock
  private ScheduledExecutorService leaseScheduler;

  private ObjectiveAnswerCheckBatchService service;
  private ObjectiveAnswerCheckProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ObjectiveAnswerCheckProperties();
    properties.setEnabled(true);
    properties.setConcurrency(2);
    service = new ObjectiveAnswerCheckBatchService(batchMapper, itemMapper, auditService, aiService,
        domainService, properties, orchestrationExecutor, workerExecutor, leaseScheduler,
        new ObjectMapper());
  }

  private ObjectiveAnswerCheckBatchEntity batch(String status) {
    ObjectiveAnswerCheckBatchEntity batch = new ObjectiveAnswerCheckBatchEntity();
    batch.setId(1L);
    batch.setTenantId(10159L);
    batch.setRunId(99L);
    batch.setOwnerId("owner");
    batch.setStatus(status);
    batch.setModelId("ateve-jev-v1");
    batch.setJevBaseUrl("https://tokendance.space/gateway");
    batch.setJevConcurrency(2);
    batch.setJevReadTimeoutSeconds(30);
    lenient().when(batchMapper.findById(1L)).thenReturn(batch);
    return batch;
  }

  @Test
  void pauseOnlyFromPendingOrRunning() {
    batch("running");
    when(batchMapper.requestPause(1L)).thenReturn(1);
    service.pause(1L);
    verify(batchMapper).requestPause(1L);
    verify(batchMapper).finishPausedIfIdle(1L);

    batch("done");
    assertThrows(BusinessException.class, () -> service.pause(1L));
    verify(batchMapper, org.mockito.Mockito.times(1)).requestPause(1L);
  }

  @Test
  void resumeRejectsWhenProcessingStillInFlight() {
    batch("failed");
    when(itemMapper.countByProcessStatus(1L, "processing")).thenReturn(1);
    BusinessException e = assertThrows(BusinessException.class,
        () -> service.resume(1L, 10159L, 1L));
    assertEquals("409", e.getCode());
    verify(batchMapper, never()).acquire(anyLong(), anyLong(), anyLong(), anyString(), anyLong());
  }

  @Test
  void resumeRejectsLegacyBatchWithoutJevSnapshot() {
    ObjectiveAnswerCheckBatchEntity legacy = batch("paused");
    legacy.setJevBaseUrl(null);
    legacy.setJevConcurrency(null);
    legacy.setJevReadTimeoutSeconds(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.resume(1L, 10159L, 7L));
    assertEquals("409", e.getCode());
    verify(batchMapper, never()).acquire(anyLong(), anyLong(), anyLong(), anyString(), anyLong());
  }

  @Test
  void retryFailedRejectsLegacyBatchWithoutJevSnapshot() {
    ObjectiveAnswerCheckBatchEntity legacy = batch("done");
    legacy.setJevBaseUrl(null);
    legacy.setJevConcurrency(null);
    legacy.setJevReadTimeoutSeconds(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.retryFailed(1L, 10159L, 7L));
    assertEquals("409", e.getCode());
    verify(itemMapper, never()).resetFailedToPending(anyLong(), anyLong());
  }

  @Test
  void resumeAcquiresNewRunForPausedBatch() {
    batch("paused");
    when(itemMapper.countByProcessStatus(1L, "processing")).thenReturn(0);
    when(batchMapper.acquire(eq(1L), eq(10159L), anyLong(), anyString(), eq(7L))).thenReturn(1);

    service.resume(1L, 10159L, 7L);

    verify(batchMapper).acquire(eq(1L), eq(10159L), anyLong(), anyString(), eq(7L));
  }

  @Test
  void resumeReportsConflictWhenAnotherBatchHoldsActiveSlot() {
    batch("paused");
    when(itemMapper.countByProcessStatus(1L, "processing")).thenReturn(0);
    when(batchMapper.acquire(eq(1L), anyLong(), anyLong(), anyString(), anyLong())).thenReturn(0);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.resume(1L, 10159L, 7L));
    assertEquals("409", e.getCode());
  }

  @Test
  void retryFailedOnlyResetsRetryableRowsAndKeepsCountersConsistent() {
    batch("done");
    when(itemMapper.countByProcessStatus(1L, "processing")).thenReturn(0);
    when(itemMapper.resetFailedToPending(1L, 7L)).thenReturn(2);
    when(batchMapper.acquire(eq(1L), anyLong(), anyLong(), anyString(), eq(7L))).thenReturn(1);

    int reset = service.retryFailed(1L, 10159L, 7L);

    assertEquals(2, reset);
    verify(batchMapper).decrementForRetry(1L, 2);
    verify(batchMapper).acquire(eq(1L), anyLong(), anyLong(), anyString(), eq(7L));
  }

  @Test
  void retryFailedWithNothingToResetDoesNotStartRun() {
    batch("paused");
    when(itemMapper.countByProcessStatus(1L, "processing")).thenReturn(0);
    when(itemMapper.resetFailedToPending(1L, 7L)).thenReturn(0);

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.retryFailed(1L, 10159L, 7L));
    assertEquals("400", e.getCode());
    verify(batchMapper, never()).decrementForRetry(anyLong(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void orchestrationRejectionLatchesFailedAndReturns503() {
    batch("running");
    org.mockito.Mockito.doThrow(new RejectedExecutionException("full"))
        .when(orchestrationExecutor).execute(any());

    BusinessException e = assertThrows(BusinessException.class,
        () -> service.dispatch(1L, 10159L, 7L));

    assertEquals("503", e.getCode());
    verify(batchMapper).markFailed(eq(1L), eq(99L), anyString());
  }

  @Test
  void pauseRequestFailureDoesNotFinalize() {
    batch("running");
    when(batchMapper.requestPause(1L)).thenReturn(0);
    assertThrows(BusinessException.class, () -> service.pause(1L));
    verify(batchMapper, never()).finishPausedIfIdle(1L);
  }

  /**
   * verifier 补充（方案 §9.1/§17.3）：判定必须使用批次创建时固化的阈值快照，
   * 运行期 properties 改变不得影响历史批次。
   */
  @Test
  void processClaimedUsesBatchSnapshotThresholds() {
    ObjectiveAnswerCheckBatchEntity batch = batch("running");
    batch.setConfidenceThreshold(new java.math.BigDecimal("0.950"));
    batch.setMarginThreshold(new java.math.BigDecimal("0.550"));

    org.mockito.Mockito.doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(orchestrationExecutor).execute(any(Runnable.class));
    org.mockito.Mockito.doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(workerExecutor).execute(any(Runnable.class));
    when(leaseScheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
        .thenReturn(org.mockito.Mockito.mock(java.util.concurrent.ScheduledFuture.class));
    when(batchMapper.renewLease(anyLong(), anyLong(), anyString())).thenReturn(1);

    com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity item =
        new com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity();
    item.setId(5L);
    item.setBatchId(1L);
    item.setDetailId(11);
    item.setQuestionType("single");
    item.setStemText("题干");
    item.setSourceHash("hash");
    item.setOptionSnapshotJson("[{\"optionId\":\"opt_0\",\"originalIndex\":0,\"label\":\"A\","
        + "\"text\":\"甲\",\"originalCorrect\":true},{\"optionId\":\"opt_1\",\"originalIndex\":1,"
        + "\"label\":\"B\",\"text\":\"乙\",\"originalCorrect\":false}]");
    item.setAttempts(1);
    when(itemMapper.listPendingIds(eq(1L), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(java.util.List.of(5L), java.util.List.of());
    when(itemMapper.claim(5L, 99L)).thenReturn(1);
    when(itemMapper.findById(5L)).thenReturn(item);
    when(aiService.check(any(), any(), any())).thenReturn(new com.guoyu.platform.ai.dto.ObjectiveAnswerDecision(
        com.guoyu.platform.ai.dto.ObjectiveAnswerDecision.SCOPE_QUESTION, "opt_0", 0.99,
        java.util.Map.of("opt_0", 0.99, "opt_1", 0.01), java.util.Map.of(), 1));
    when(domainService.judge(any(), any(), any(), any())).thenReturn(new ObjectiveAnswerCheckResult(
        "consistent_high", true, "opt_0", "opt_0", new java.math.BigDecimal("0.99000"),
        new java.math.BigDecimal("0.99000"), new java.math.BigDecimal("0.98000"), null, null));
    when(itemMapper.writeSuccess(any(), anyLong(), org.mockito.ArgumentMatchers.anyInt(), anyLong()))
        .thenReturn(1);

    service.dispatch(1L, 10159L, 7L);

    org.mockito.ArgumentCaptor<java.math.BigDecimal> theta =
        org.mockito.ArgumentCaptor.forClass(java.math.BigDecimal.class);
    org.mockito.ArgumentCaptor<java.math.BigDecimal> delta =
        org.mockito.ArgumentCaptor.forClass(java.math.BigDecimal.class);
    verify(domainService).judge(any(), any(), theta.capture(), delta.capture());
    assertEquals(new java.math.BigDecimal("0.950"), theta.getValue());
    assertEquals(new java.math.BigDecimal("0.550"), delta.getValue());
  }
}
