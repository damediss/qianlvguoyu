package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.dto.ClassPracticeScopeSaveReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassReviewSyncEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassReviewSyncMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassReviewSyncAutoProvisionTest {

  @Mock TrainInfoClassMapper classMapper;
  @Mock TrainClassReviewSyncMapper reviewSyncMapper;
  @Mock TrainExamListMapper examListMapper;
  @Mock TrainExamWorkTypeListMapper workTypeListMapper;
  @Mock TrainPeopleListMapper peopleMapper;
  @Mock TrainExamPeopleListMapper examPeopleMapper;
  @Mock AiReviewGrayWhitelistMapper grayWhitelistMapper;
  @Mock DictMapper dictMapper;
  @Mock TrainingOperatorScopeService operatorScopeService;
  @Mock TrainExamListApplicationService examListApplicationService;
  @Mock ReviewPlanApplicationService reviewPlanApplicationService;

  ClassReviewSyncApplicationService service;
  TrainInfoClassEntity clazz;
  final TrainClassReviewSyncEntity[] stored = new TrainClassReviewSyncEntity[1];

  @BeforeEach
  void setUp() {
    service = new ClassReviewSyncApplicationService(
        classMapper,
        reviewSyncMapper,
        examListMapper,
        workTypeListMapper,
        peopleMapper,
        examPeopleMapper,
        grayWhitelistMapper,
        dictMapper,
        operatorScopeService,
        examListApplicationService,
        reviewPlanApplicationService);

    clazz = new TrainInfoClassEntity();
    clazz.setId(88L);
    clazz.setTitle("浸出班");
    clazz.setTrainInfoId(10L);
    clazz.setWorkTypeId(5L);
    clazz.setWorkTypeLevel(3);
    clazz.setDeleted(0);

    when(classMapper.selectOne(any())).thenReturn(clazz);
    when(reviewSyncMapper.selectOne(any())).thenAnswer(inv -> stored[0]);
    when(reviewSyncMapper.insert(any(TrainClassReviewSyncEntity.class))).thenAnswer(inv -> {
      TrainClassReviewSyncEntity row = inv.getArgument(0);
      row.setId(1L);
      stored[0] = row;
      return 1;
    });
    when(reviewSyncMapper.updateById(any(TrainClassReviewSyncEntity.class))).thenAnswer(inv -> {
      stored[0] = inv.getArgument(0);
      return 1;
    });
    when(peopleMapper.selectList(any())).thenReturn(List.of());
    when(examPeopleMapper.selectList(any())).thenReturn(List.of());
    when(examPeopleMapper.selectCount(any())).thenReturn(0L);
    when(grayWhitelistMapper.selectCount(any())).thenReturn(0L);
    when(dictMapper.selectList(any())).thenReturn(List.of());
  }

  @Test
  void pickBestMatchingExamId_prefersWorkTypeAndLevel() {
    TrainExamListEntity unmatched = exam(100L, "其它工种");
    TrainExamListEntity workTypeOnly = exam(200L, "同工种");
    TrainExamListEntity fullMatch = exam(150L, "同工种同等级");
    when(examListMapper.selectList(any())).thenReturn(List.of(unmatched, workTypeOnly, fullMatch));

    TrainExamWorkTypeListEntity wtOnly = binding(200L, 5L, "2");
    TrainExamWorkTypeListEntity wtLevel = binding(150L, 5L, "3");
    TrainExamWorkTypeListEntity other = binding(100L, 9L, "3");
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(wtOnly, wtLevel, other));

    Long picked = service.pickBestMatchingExamId(clazz);
    assertThat(picked).isEqualTo(150L);
  }

  @Test
  void pickBestMatchingExamId_returnsNullWhenNoWorkTypeMatch() {
    when(examListMapper.selectList(any())).thenReturn(List.of(exam(100L, "其它")));
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(100L, 9L, "3")));
    assertThat(service.pickBestMatchingExamId(clazz)).isNull();
  }

  @Test
  void autoProvision_disabledWithoutBinding_skips() {
    ClassPracticeScopeSaveReq req = new ClassPracticeScopeSaveReq();
    req.setEnabled(0);
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.autoProvisionFromPracticeScope(88L, req);
      assertThat(out.get("ok")).isEqualTo(true);
      assertThat(out.get("action")).isEqualTo("skipped");
    }
    verify(examListApplicationService, never()).createForClassReviewSync(any(), any());
  }

  @Test
  void autoProvision_createsExamWhenTrainHasNoMatch() {
    when(examListMapper.selectList(any())).thenReturn(List.of());
    when(examListApplicationService.createForClassReviewSync(any(), any())).thenReturn(999L);
    TrainExamListEntity created = exam(999L, "浸出班-同步学");
    when(examListMapper.selectOne(any())).thenReturn(created);
    when(examListMapper.selectById(999L)).thenReturn(created);
    when(workTypeListMapper.selectList(any())).thenReturn(List.of());

    ClassPracticeScopeSaveReq req = new ClassPracticeScopeSaveReq();
    req.setEnabled(1);
    req.setCourseIds(List.of(41, 42));

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.autoProvisionFromPracticeScope(88L, req);
      assertThat(out.get("ok")).isEqualTo(true);
      assertThat(out.get("action")).isEqualTo("created");
      assertThat(out.get("examSource")).isEqualTo("CREATED");
      assertThat(out.get("examId")).isEqualTo("999");
      assertThat(String.valueOf(out.get("message"))).contains("新建");
    }
    verify(examListApplicationService).createForClassReviewSync(clazz, List.of(41, 42));
    assertThat(stored[0]).isNotNull();
    assertThat(stored[0].getExamId()).isEqualTo(999L);
    assertThat(stored[0].getEnabled()).isEqualTo(1);
    assertThat(stored[0].getAutoEnroll()).isEqualTo(1);
  }

  @Test
  void autoProvision_reusesExistingAliveBinding() {
    TrainClassReviewSyncEntity existing = new TrainClassReviewSyncEntity();
    existing.setId(3L);
    existing.setTenantId(1L);
    existing.setClassId(88L);
    existing.setExamId(454L);
    existing.setEnabled(1);
    existing.setDeleted(0);
    stored[0] = existing;

    TrainExamListEntity bound = exam(454L, "已有计划");
    when(examListMapper.selectOne(any())).thenReturn(bound);
    when(examListMapper.selectById(454L)).thenReturn(bound);

    ClassPracticeScopeSaveReq req = new ClassPracticeScopeSaveReq();
    req.setEnabled(1);

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.autoProvisionFromPracticeScope(88L, req);
      assertThat(out.get("ok")).isEqualTo(true);
      assertThat(out.get("examSource")).isEqualTo("REUSE_BINDING");
      assertThat(out.get("examId")).isEqualTo("454");
    }
    verify(examListApplicationService, never()).createForClassReviewSync(any(), any());
  }

  @Test
  void ensureBoundAndSyncRoster_reusesBindingAndEnrolls() {
    TrainClassReviewSyncEntity existing = new TrainClassReviewSyncEntity();
    existing.setId(3L);
    existing.setTenantId(1L);
    existing.setClassId(88L);
    existing.setExamId(454L);
    existing.setEnabled(1);
    existing.setDeleted(0);
    stored[0] = existing;

    TrainExamListEntity bound = exam(454L, "已有计划");
    when(examListMapper.selectOne(any())).thenReturn(bound);
    when(examListMapper.selectById(454L)).thenReturn(bound);

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.ensureBoundAndSyncRoster(88L);
      assertThat(out.get("ok")).isEqualTo(true);
      assertThat(out.get("action")).isEqualTo("synced");
      assertThat(out.get("examId")).isEqualTo("454");
      @SuppressWarnings("unchecked")
      Map<String, Object> enroll = (Map<String, Object>) out.get("enroll");
      assertThat(enroll.get("inserted")).isEqualTo(0);
    }
    verify(examListApplicationService, never()).createForClassReviewSync(any(), any());
  }

  private MockedStatic<SecurityContext> mockCurrentAdmin() {
    PlatformUserPrincipal principal = mock(PlatformUserPrincipal.class);
    lenient().when(principal.getUserId()).thenReturn(1L);
    lenient().when(principal.getTenantId()).thenReturn(1L);
    MockedStatic<SecurityContext> mocked = mockStatic(SecurityContext.class);
    mocked.when(SecurityContext::currentUser).thenReturn(principal);
    return mocked;
  }

  private static TrainExamListEntity exam(Long id, String title) {
    TrainExamListEntity e = new TrainExamListEntity();
    e.setId(id);
    e.setTitle(title);
    e.setDeleted(0);
    e.setTrainId(10L);
    return e;
  }

  private static TrainExamWorkTypeListEntity binding(Long examId, Long workTypeId, String level) {
    TrainExamWorkTypeListEntity b = new TrainExamWorkTypeListEntity();
    b.setExamId(examId);
    b.setWorkTypeId(workTypeId);
    b.setLevel(level);
    b.setDeleted(0);
    return b;
  }
}
