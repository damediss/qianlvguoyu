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
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassReviewSyncMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.util.Collections;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
  @Mock JdbcTemplate jdbcTemplate;

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
        reviewPlanApplicationService,
        jdbcTemplate);

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
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any())).thenReturn(Collections.emptyList());
    when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any())).thenReturn(1);
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
  void pickBestMatchingExamId_returnsNullWhenOnlyWorkTypeMatchesDifferentLevel() {
    // 同工种但等级不同（中级计划 vs 高级班）不得自动复用
    when(examListMapper.selectList(any())).thenReturn(List.of(exam(200L, "中级1班-同步学")));
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(200L, 5L, "4")));
    assertThat(service.pickBestMatchingExamId(clazz)).isNull();
  }

  @Test
  void pickBestMatchingExamId_returnsNullWhenNoWorkTypeMatch() {
    when(examListMapper.selectList(any())).thenReturn(List.of(exam(100L, "其它")));
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(100L, 9L, "3")));
    assertThat(service.pickBestMatchingExamId(clazz)).isNull();
  }

  @Test
  void autoProvision_rejectsMismatchedLevelBindingAndCreatesNew() {
    TrainClassReviewSyncEntity existing = new TrainClassReviewSyncEntity();
    existing.setId(3L);
    existing.setTenantId(1L);
    existing.setClassId(88L);
    existing.setExamId(200L);
    existing.setEnabled(1);
    existing.setDeleted(0);
    stored[0] = existing;

    TrainExamListEntity midExam = exam(200L, "中级1班-同步学");
    TrainExamListEntity created = exam(999L, "浸出班-同步学");
    when(examListMapper.selectOne(any())).thenReturn(midExam);
    when(examListMapper.selectList(any())).thenReturn(List.of(midExam));
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(200L, 5L, "4")));
    when(examListApplicationService.createForClassReviewSync(any(), any())).thenReturn(999L);
    when(examListMapper.selectById(999L)).thenReturn(created);

    ClassPracticeScopeSaveReq req = new ClassPracticeScopeSaveReq();
    req.setEnabled(1);
    req.setCourseIds(List.of(41));

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.autoProvisionFromPracticeScope(88L, req);
      assertThat(out.get("ok")).isEqualTo(true);
      assertThat(out.get("action")).isEqualTo("created");
      assertThat(out.get("examSource")).isEqualTo("CREATED");
      assertThat(out.get("examId")).isEqualTo("999");
    }
    verify(examListApplicationService).createForClassReviewSync(clazz, List.of(41));
    assertThat(stored[0].getExamId()).isEqualTo(999L);
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
    // 工种+等级双匹配才允许沿用已绑计划
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(454L, 5L, "3")));

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
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(454L, 5L, "3")));

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

  @Test
  void unenrollClassRosterFromExam_softDeletesOnlyClassStudents() {
    TrainPeopleListEntity student = new TrainPeopleListEntity();
    student.setPlatformUserId(501L);
    student.setIdCard("110101199001011234");
    student.setType("student");
    student.setDeleted(0);
    when(peopleMapper.selectList(any())).thenReturn(List.of(student));

    TrainExamPeopleListEntity hit = new TrainExamPeopleListEntity();
    hit.setId(77L);
    hit.setExamId(200L);
    hit.setPlatformUserId(501L);
    hit.setDeleted(0);
    when(examPeopleMapper.selectList(any())).thenReturn(List.of(hit));
    when(examPeopleMapper.deleteById(77L)).thenReturn(1);

    int removed = service.unenrollClassRosterFromExam(88L, 200L);
    assertThat(removed).isEqualTo(1);
    verify(examPeopleMapper).deleteById(77L);
  }

  @Test
  void enrollRoster_removesStudentsFromOtherSyncExams() {
    TrainClassReviewSyncEntity existing = new TrainClassReviewSyncEntity();
    existing.setId(3L);
    existing.setTenantId(1L);
    existing.setClassId(88L);
    existing.setExamId(999L);
    existing.setEnabled(1);
    existing.setDeleted(0);
    stored[0] = existing;

    TrainExamListEntity keep = exam(999L, "高级工2班-同步学");
    when(examListMapper.selectOne(any())).thenReturn(keep);
    when(examListMapper.selectById(999L)).thenReturn(keep);

    TrainPeopleListEntity student = new TrainPeopleListEntity();
    student.setPlatformUserId(501L);
    student.setRealname("张三");
    student.setType("student");
    student.setDeleted(0);
    when(peopleMapper.selectList(any())).thenReturn(List.of(student));
    // 当前计划无人；其它同步学计划有残留
    when(examPeopleMapper.selectList(any())).thenReturn(List.of());

    TrainExamListEntity mid = exam(200L, "中级1班-同步学");
    when(examListMapper.selectList(any())).thenReturn(List.of(mid));

    TrainExamPeopleListEntity leftover = new TrainExamPeopleListEntity();
    leftover.setId(88L);
    leftover.setExamId(200L);
    leftover.setPlatformUserId(501L);
    leftover.setDeleted(0);
    // enrollRoster 先查当前 exam people（空），再查 foreign hits
    when(examPeopleMapper.selectList(any()))
        .thenReturn(List.of())
        .thenReturn(List.of(leftover));
    when(examPeopleMapper.deleteById(88L)).thenReturn(1);
    when(examPeopleMapper.insert(any(TrainExamPeopleListEntity.class))).thenReturn(1);

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.enrollRoster(88L);
      assertThat(out.get("inserted")).isEqualTo(1);
      assertThat(out.get("removedForeign")).isEqualTo(1);
    }
    verify(examPeopleMapper).deleteById(88L);
  }

  @Test
  void ensureExamReviewConfig_insertsWhenMissing() {
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any())).thenReturn(Collections.emptyList());
    when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any())).thenReturn(1);
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      assertThat(service.ensureExamReviewConfig(999L)).isTrue();
    }
    verify(jdbcTemplate).update(any(String.class), org.mockito.ArgumentMatchers.eq(999L),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(0),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(1L));
  }

  @Test
  void ensureExamReviewConfig_skipsWhenExists() {
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any())).thenReturn(List.of(12L));
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      assertThat(service.ensureExamReviewConfig(999L)).isFalse();
    }
    verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any());
  }

  @Test
  void enrollRoster_ensuresReviewConfig() {
    TrainClassReviewSyncEntity existing = new TrainClassReviewSyncEntity();
    existing.setId(3L);
    existing.setTenantId(1L);
    existing.setClassId(88L);
    existing.setExamId(999L);
    existing.setEnabled(1);
    existing.setDeleted(0);
    stored[0] = existing;

    TrainExamListEntity keep = exam(999L, "高级工2班-同步学");
    when(examListMapper.selectOne(any())).thenReturn(keep);
    when(examListMapper.selectById(999L)).thenReturn(keep);
    when(examListMapper.selectList(any())).thenReturn(List.of());
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any())).thenReturn(Collections.emptyList());
    when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any())).thenReturn(1);

    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      Map<String, Object> out = service.enrollRoster(88L);
      assertThat(out.get("reviewEnsured")).isEqualTo(true);
    }
    verify(jdbcTemplate).update(any(String.class), org.mockito.ArgumentMatchers.eq(999L),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(0),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(1L));
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
