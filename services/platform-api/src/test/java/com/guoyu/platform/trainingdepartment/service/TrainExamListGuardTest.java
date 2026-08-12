package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * PHP 来源考试计划只读守卫（AI同步学统计方案 v1.1 §3.3）：
 * id &lt; 100000 的考试计划编辑/删除/考生写操作一律拒绝；SaaS 本地新建（id≥100000）放行。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainExamListGuardTest {

  @Mock
  private TrainExamListMapper mapper;
  @Mock
  private TrainExamWorkTypeListMapper workTypeListMapper;
  @Mock
  private TrainExamPeopleListMapper peopleListMapper;
  @Mock
  private ExamCourseMapper examCourseMapper;
  @Mock
  private TrainInfoListMapper trainInfoListMapper;
  @Mock
  private TrainPeopleListMapper trainPeopleListMapper;

  private TrainExamListApplicationService service;

  @BeforeEach
  void setUp() {
    service = new TrainExamListApplicationService(mapper, workTypeListMapper, peopleListMapper,
        examCourseMapper, trainInfoListMapper, trainPeopleListMapper);
  }

  private MockedStatic<SecurityContext> mockCurrentAdmin() {
    PlatformUserPrincipal principal = mock(PlatformUserPrincipal.class);
    lenient().when(principal.getUserId()).thenReturn(1L);
    lenient().when(principal.getTenantId()).thenReturn(1L);
    MockedStatic<SecurityContext> mocked = mockStatic(SecurityContext.class);
    mocked.when(SecurityContext::currentUser).thenReturn(principal);
    return mocked;
  }

  @Test
  void updatePhpSourceExamRejected() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      TrainExamListUpdateReq req = new TrainExamListUpdateReq();
      req.setTitle("改名");
      assertThatThrownBy(() -> service.update(440L, req))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("来源于工匠基地平台");
      verify(mapper, never()).updateById(any(TrainExamListEntity.class));
    }
  }

  @Test
  void deletePhpSourceExamRejected() {
    assertThatThrownBy(() -> service.delete(433L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("来源于工匠基地平台");
    verify(mapper, never()).deleteById(any(Long.class));
  }

  @Test
  void updatePeopleGradeOfPhpSourceExamRejected() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      TrainExamPeopleListEntity people = new TrainExamPeopleListEntity();
      people.setId(9L);
      people.setExamId(433L); // PHP 来源计划
      when(peopleListMapper.selectOne(any())).thenReturn(people);
      assertThatThrownBy(() -> service.updatePeopleGrade(9L, 1, null, null, null))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("来源于工匠基地平台");
      verify(peopleListMapper, never()).updateById(any(TrainExamPeopleListEntity.class));
    }
  }

  @Test
  void importPeopleToPhpSourceExamRejected() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      assertThatThrownBy(() -> service.importPeopleByIdCard(440L, "450303199001011234"))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("来源于工匠基地平台");
      verify(peopleListMapper, never()).insert(any(TrainExamPeopleListEntity.class));
    }
  }

  @Test
  void importFromTrainToPhpSourceExamRejected() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      assertThatThrownBy(() -> service.importPeopleFromTrain(440L, 7L))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("来源于工匠基地平台");
    }
  }

  @Test
  void saasLocalExamNotBlockedByGuard() {
    // id ≥ 100000 为 SaaS 本地新建：守卫不拦截（后续流程报 NOT_FOUND 而非只读拦截）
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(null);
      assertThatThrownBy(() -> service.delete(100001L))
          .isInstanceOf(BusinessException.class)
          .hasMessageNotContaining("来源于工匠基地平台");
    }
  }
}
