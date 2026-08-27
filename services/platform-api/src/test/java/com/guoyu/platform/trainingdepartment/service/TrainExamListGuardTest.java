package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 考试计划 10 万区间只读守卫移除回归（单库化 Q10）：合并库 train_exam_list 单表单库，
 * requireSaaSLocalExam 已删除——小 id（原 PHP 来源）计划的编辑/删除/考生写操作一律放行，
 * 存在性校验（NOT_FOUND）语义不变。
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
    lenient().when(principal.getTenantId()).thenReturn(10159L);
    MockedStatic<SecurityContext> mocked = mockStatic(SecurityContext.class);
    mocked.when(SecurityContext::currentUser).thenReturn(principal);
    return mocked;
  }

  private static TrainExamListEntity exam(Long id) {
    TrainExamListEntity e = new TrainExamListEntity();
    e.setId(id);
    e.setTitle("原标题");
    e.setTenantId(10159L);
    e.setDeleted(0);
    return e;
  }

  @Test
  void updateSmallIdExamAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(exam(440L));
      TrainExamListUpdateReq req = new TrainExamListUpdateReq();
      req.setTitle("改名");
      service.update(440L, req);
      verify(mapper).updateById(any(TrainExamListEntity.class));
    }
  }

  @Test
  void deleteSmallIdExamAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(exam(433L));
      service.delete(433L);
      verify(mapper).deleteById(433L);
    }
  }

  @Test
  void updatePeopleGradeOfSmallIdExamAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      TrainExamPeopleListEntity people = new TrainExamPeopleListEntity();
      people.setId(9L);
      people.setExamId(433L);
      when(peopleListMapper.selectOne(any())).thenReturn(people);
      service.updatePeopleGrade(9L, 1, null, new BigDecimal("88.5"), new BigDecimal("100"));
      verify(peopleListMapper).updateById(any(TrainExamPeopleListEntity.class));
    }
  }

  @Test
  void importPeopleToSmallIdExamAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(peopleListMapper.selectCount(any())).thenReturn(0L);
      int count = service.importPeopleByIdCard(440L, "450303199001011234");
      org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
      verify(peopleListMapper).insert(any(TrainExamPeopleListEntity.class));
    }
  }

  @Test
  void importFromTrainToSmallIdExamAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      TrainInfoListEntity train = new TrainInfoListEntity();
      train.setId(7L);
      train.setWorkTypeId(10L);
      train.setWorkTypeLevel(3);
      when(trainInfoListMapper.selectById(7L)).thenReturn(train);

      TrainPeopleListEntity person = new TrainPeopleListEntity();
      person.setId(21L);
      person.setTrainId(7L);
      person.setIdCard("450303199001011234");
      person.setRealname("张三");
      person.setPhone("13800138000");
      when(trainPeopleListMapper.selectList(any())).thenReturn(List.of(person));

      int count = service.importPeopleFromTrain(440L, 7L);
      org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
      verify(peopleListMapper).insert(any(TrainExamPeopleListEntity.class));
    }
  }

  @Test
  void nonExistingExamStillRejected() {
    // 守卫移除≠放开存在性校验：记录不存在仍报 NOT_FOUND（防误删语义不变）
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(null);
      assertThatThrownBy(() -> service.delete(433L))
          .isInstanceOf(BusinessException.class);
    }
  }
}
