package com.guoyu.platform.app.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.app.exam.entity.AppExamMockPaperEntity;
import com.guoyu.platform.app.exam.mapper.AppExamMockPaperMapper;
import com.guoyu.platform.app.exam.mapper.AppExamUserAnswerLogMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeCourseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeKnowledgeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassPracticeScopeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassReviewSyncMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 错题重练 wrongReplay 的题型防御性过滤（《简答题作答与AI判分方案》Q-C）：
 * trainsave 写 exam_user_answer_log 后主观题错题会漏进重练池且无作答 UI——
 * 反查只保留客观三型（single/multi/judge，含 "1"/"2"/"3" 数字型别名归一）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppExamSmartWrongReplayTest {

  @Mock
  private ExamExamDetailMapper examDetailMapper;
  @Mock
  private ExamKnowledgeStructureMapper knowledgeMapper;
  @Mock
  private AppExamMockPaperMapper mockPaperMapper;
  @Mock
  private AppExamUserAnswerLogMapper answerLogMapper;
  @Mock
  private TrainPeopleListMapper peopleListMapper;
  @Mock
  private TrainClassPracticeScopeMapper practiceScopeMapper;
  @Mock
  private TrainClassPracticeCourseMapper practiceCourseMapper;
  @Mock
  private TrainClassPracticeKnowledgeMapper practiceKnowledgeMapper;
  @Mock
  private FollowTaskBankQuestionConverter bankQuestionConverter;
  @Mock
  private TrainClassReviewSyncMapper classReviewSyncMapper;
  @Mock
  private AiReviewPlanMapper reviewPlanMapper;
  @Mock
  private AiReviewWrongItemMapper reviewWrongItemMapper;

  private AppExamSmartService service;

  @BeforeEach
  void setUp() {
    service = new AppExamSmartService(examDetailMapper, knowledgeMapper, mockPaperMapper,
        answerLogMapper, peopleListMapper, practiceScopeMapper, practiceCourseMapper,
        practiceKnowledgeMapper, bankQuestionConverter, classReviewSyncMapper, reviewPlanMapper,
        reviewWrongItemMapper);
    PlatformUserPrincipal principal = new PlatformUserPrincipal(7L, 10159L, "u", "u", "app",
        List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", List.of()));
    TenantContextHolder.setTenantId(10159L);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private static ExamExamDetailEntity question(int id, String type) {
    ExamExamDetailEntity e = new ExamExamDetailEntity();
    e.setId(id);
    e.setType(type);
    e.setDeleted(0);
    e.setContent("题干" + id);
    e.setAnswer("[{\"content\":\"对\",\"isCorrect\":true},{\"content\":\"错\",\"isCorrect\":false}]");
    e.setResolve("解析");
    e.setScore(BigDecimal.ONE);
    e.setKnowledgeId(100);
    return e;
  }

  @Test
  void wrongReplayKeepsOnlyObjectiveThreeTypes() {
    // 错题 id：1 单选、2 简答（主观）、3 判断、4 计算（主观）、5 多选
    when(answerLogMapper.findWrongQuestionIds(10159L, 7L, null, 20))
        .thenReturn(List.of(1, 2, 3, 4, 5));
    when(examDetailMapper.selectBatchIds(any())).thenReturn(List.of(
        question(1, "single"), question(2, "answer"), question(3, "judge"),
        question(4, "cal"), question(5, "multi")));

    Map<String, Object> result = service.wrongReplay(new HashMap<>());

    ArgumentCaptor<AppExamMockPaperEntity> captor =
        ArgumentCaptor.forClass(AppExamMockPaperEntity.class);
    verify(mockPaperMapper).insert(captor.capture());
    // 主观题（2 answer / 4 cal）被过滤，且保持错题出现顺序
    assertThat(captor.getValue().getQuestionIds()).isEqualTo("1,3,5");
    assertThat(captor.getValue().getTotalCount()).isEqualTo(3);
    assertThat(result.get("totalCount")).isEqualTo(3);
  }

  @Test
  void objectiveTypePredicateCoversAliasesAndRejectsSubjective() {
    assertThat(AppExamSmartService.isObjectiveQuestionType("single")).isTrue();
    assertThat(AppExamSmartService.isObjectiveQuestionType("multi")).isTrue();
    assertThat(AppExamSmartService.isObjectiveQuestionType("judge")).isTrue();
    // 数字/中文别名走 normalizeQuestionType 归一
    assertThat(AppExamSmartService.isObjectiveQuestionType("1")).isTrue();
    assertThat(AppExamSmartService.isObjectiveQuestionType("2")).isTrue();
    assertThat(AppExamSmartService.isObjectiveQuestionType(" 判断 ")).isTrue();
    // 主观题白名单与空/未知 type 一律排除
    for (String t : new String[]{"answer", "cal", "essay", "analysis", "explain", "fill",
        "short", "drafting", "", "unknown"}) {
      assertThat(AppExamSmartService.isObjectiveQuestionType(t)).as(t).isFalse();
    }
    assertThat(AppExamSmartService.isObjectiveQuestionType(null)).isFalse();
  }
}
