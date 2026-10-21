package com.guoyu.platform.trainingdepartment.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassTeacherPayMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowEvidenceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowIssueMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowOpLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSupervisionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSurveyResponseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationArchiveMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationNoticeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleChangeRequestMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainShiftAssignRecordMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskLogMapper;
import com.guoyu.platform.trainingdepartment.service.ClassPracticeScopeApplicationService;
import com.guoyu.platform.trainingdepartment.service.ClassReviewSyncApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainClassDeleteCascadeSupportTest {

  @Mock TrainPeopleListMapper peopleListMapper;
  @Mock TrainPlanRosterMapper planRosterMapper;
  @Mock TrainSignUpSelfMapper signUpSelfMapper;
  @Mock TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  @Mock TrainTaskLogMapper taskLogMapper;
  @Mock TrainShiftAssignRecordMapper shiftAssignRecordMapper;
  @Mock TrainCertificateMapper certificateMapper;
  @Mock TrainSubsidyMapper subsidyMapper;
  @Mock TrainGraduationArchiveMapper graduationArchiveMapper;
  @Mock TrainGraduationNoticeMapper graduationNoticeMapper;
  @Mock TrainScheduleMapper scheduleMapper;
  @Mock TrainScheduleChangeRequestMapper scheduleChangeRequestMapper;
  @Mock TrainAttendanceMapper attendanceMapper;
  @Mock TrainClassRoleMapper classRoleMapper;
  @Mock TrainClassTeacherPayMapper teacherPayMapper;
  @Mock TrainFollowTaskMapper followTaskMapper;
  @Mock TrainFollowEvidenceMapper followEvidenceMapper;
  @Mock TrainFollowPhotoPackMapper followPhotoPackMapper;
  @Mock TrainFollowLogMapper followLogMapper;
  @Mock TrainFollowOpLogMapper followOpLogMapper;
  @Mock TrainFollowIssueMapper followIssueMapper;
  @Mock TrainFollowSupervisionMapper followSupervisionMapper;
  @Mock TrainFollowSurveyResponseMapper followSurveyResponseMapper;
  @Mock ClassPracticeScopeApplicationService classPracticeScopeApplicationService;
  @Mock ClassReviewSyncApplicationService classReviewSyncApplicationService;

  private TrainClassDeleteCascadeSupport support;

  @BeforeEach
  void setUp() {
    support = new TrainClassDeleteCascadeSupport(
        peopleListMapper,
        planRosterMapper,
        signUpSelfMapper,
        signUpPeopleSelfMapper,
        taskLogMapper,
        shiftAssignRecordMapper,
        certificateMapper,
        subsidyMapper,
        graduationArchiveMapper,
        graduationNoticeMapper,
        scheduleMapper,
        scheduleChangeRequestMapper,
        attendanceMapper,
        classRoleMapper,
        teacherPayMapper,
        followTaskMapper,
        followEvidenceMapper,
        followPhotoPackMapper,
        followLogMapper,
        followOpLogMapper,
        followIssueMapper,
        followSupervisionMapper,
        followSurveyResponseMapper,
        classPracticeScopeApplicationService,
        classReviewSyncApplicationService);
  }

  @Test
  void assertDeletable_blocksWhenCertificateExists() {
    when(certificateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
    when(subsidyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(graduationArchiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(graduationNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    BusinessException ex = assertThrows(BusinessException.class, () -> support.assertDeletable(11L));
    assertTrue(ex.getMessage().contains("证书"));
  }

  @Test
  void assertDeletable_allowsWhenNoFormalData() {
    when(certificateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(subsidyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(graduationArchiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(graduationNoticeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    assertDoesNotThrow(() -> support.assertDeletable(11L));
  }
}
