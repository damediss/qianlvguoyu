package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanDayMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.ExamResultWriteRepository;
import com.guoyu.platform.system.mapper.DictMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

/**
 * 单题作答 answer() 的 exam_result 事务内同步直写（单库化 Q3a）：
 * 作答 INSERT 成功即置 php_synced=1（防回滚旧代码时补偿 job 重写），随后同事务直写 exam_result；
 * 直写失败向外传播使作答事务回滚（方案 §4-C 明示的行为变化）；幂等回放路径不写库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewAnswerExamResultSyncTest {

  @Mock
  private AiReviewPlanMapper planMapper;
  @Mock
  private AiReviewPlanDayMapper dayMapper;
  @Mock
  private AiReviewAnswerMapper answerMapper;
  @Mock
  private AiReviewWrongItemMapper wrongMapper;
  @Mock
  private AiReviewMockPaperMapper mockPaperMapper;
  @Mock
  private AiReviewMockPaperItemMapper mockItemMapper;
  @Mock
  private AiReviewGrayWhitelistMapper grayWhitelistMapper;
  @Mock
  private ReviewQuestionQueryService reviewQuery;
  @Mock
  private ExamResultWriteRepository examResultWriteRepository;
  @Mock
  private ReviewLlmAssistService llmAssistService;
  @Mock
  private DictMapper dictMapper;

  private ReviewPlanApplicationService service;

  private static final String SINGLE_ANSWER_JSON =
      "[{\"content\":\"对\",\"isCorrect\":true},{\"content\":\"错\",\"isCorrect\":false}]";

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper());

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    when(planMapper.selectOne(any())).thenReturn(plan);

    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(100L);
    day.setStatus("pending");
    when(dayMapper.selectById(500L)).thenReturn(day);

    when(reviewQuery.loadQuestionDetail(900L)).thenReturn(new ReviewQuestionDetail(
        900L, 7L, "single", 1, new BigDecimal("2"), "题干", SINGLE_ANSWER_JSON, "解析"));
    // 灰度总开关缺失=全员开放；连对/连错统计为空不触发鼓励文案
    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectCount(any())).thenReturn(0L);
  }

  @Test
  void correctAnswerWritesExamResultInSameTx() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "对", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(1);

    // 作答记录：判对 + php_synced=1（语义=已落 exam_result）
    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    AiReviewAnswerEntity saved = captor.getValue();
    assertThat(saved.getIsCorrect()).isEqualTo(1);
    assertThat(saved.getPhpSynced()).isEqualTo(1);
    assertThat(saved.getAttemptSeq()).isEqualTo(1);
    assertThat(saved.getQuestionType()).isEqualTo("single");

    // 同步直写：答对 get_score=题分（无实得分时）
    verify(examResultWriteRepository).insertExamResult(
        "single", 440L, 900L, 42L, "对", new BigDecimal("2"));
  }

  @Test
  void wrongAnswerWritesZeroScoreAndCreatesWrongCard() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "错", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(0);
    // 答错 get_score=0
    verify(examResultWriteRepository).insertExamResult(
        "single", 440L, 900L, 42L, "错", BigDecimal.ZERO);
    // 首次答错入错题册
    verify(wrongMapper).insert(any(com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity.class));
  }

  @Test
  void examResultInsertFailurePropagatesToRollbackTx() {
    doThrow(new DuplicateKeyException("dup")).when(examResultWriteRepository)
        .insertExamResult(anyString(), anyLong(), anyLong(), anyLong(), anyString(), any());

    assertThatThrownBy(() -> service.answer(42L, 440L, 500L, 900L, "new", "对", 5, 10159L))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void replaySameAnswerSkipsAllWrites() {
    // 幂等回放：答案相同 → 纯回放，不写作答记录、不直写 exam_result
    AiReviewAnswerEntity existing = new AiReviewAnswerEntity();
    existing.setIsCorrect(1);
    existing.setUserAnswer("对");
    when(answerMapper.selectOne(any())).thenReturn(existing);

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "对", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(1);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
    verifyNoInteractions(examResultWriteRepository);
  }
}
