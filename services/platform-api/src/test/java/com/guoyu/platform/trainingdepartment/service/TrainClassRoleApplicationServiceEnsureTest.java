package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.dto.TrainClassRoleAssignReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassRoleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainClassRoleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 班级角色指派强制 ensure App 账号（《班级角色老师App开户》P0）。
 */
@ExtendWith(MockitoExtension.class)
class TrainClassRoleApplicationServiceEnsureTest {

  @Mock TrainClassRoleMapper roleMapper;
  @Mock TrainInfoClassMapper classMapper;
  @Mock TrainPeopleListMapper peopleMapper;
  @Mock TrainTeacherBankMapper teacherBankMapper;
  @Mock PlatformUserMapper platformUserMapper;
  @Mock PlatformUserEnsureService platformUserEnsureService;
  @Mock TrainingOperatorScopeService operatorScopeService;
  @Mock FollowOpLogWriter opLogWriter;

  TrainClassRoleApplicationService service;

  @BeforeEach
  void setUp() {
    service = new TrainClassRoleApplicationService(
        roleMapper,
        classMapper,
        peopleMapper,
        teacherBankMapper,
        platformUserMapper,
        platformUserEnsureService,
        operatorScopeService,
        opLogWriter);
  }

  @Test
  void assign_createsAccountAndBinds_whenTeacherHasNoPlatformUser() {
    stubClass(100L);
    TrainTeacherBankEntity teacher = teacher(11L, "王老师", "13800138001", "110101199001011234");
    when(teacherBankMapper.selectById(11L)).thenReturn(teacher);
    when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(platformUserEnsureService.ensureFromDivisionPerson(
            eq("王老师"), eq("110101199001011234"), eq("13800138001"), any(), any()))
        .thenReturn(new PlatformUserEnsureService.EnsureResult(
            PlatformUserEnsureService.Outcome.CREATED, 9001L, null));

    TrainClassRoleAssignReq req = new TrainClassRoleAssignReq();
    req.setRoleCode(TrainClassRoleApplicationService.LEAD_TEACHER);
    req.setSubjectType("teacher_bank");
    req.setSubjectId(11L);

    TrainClassRoleVO vo = service.assign(100L, req);

    assertThat(vo.platformUserId()).isEqualTo(9001L);
    assertThat(vo.bindStatus()).isEqualTo("ensured");
    assertThat(vo.unboundWarning()).isFalse();
    ArgumentCaptor<TrainClassRoleEntity> cap = ArgumentCaptor.forClass(TrainClassRoleEntity.class);
    verify(roleMapper).insert(cap.capture());
    assertThat(cap.getValue().getPlatformUserId()).isEqualTo(9001L);
  }

  @Test
  void assign_reusesExistingAccount_withoutSkippingBind() {
    stubClass(100L);
    TrainTeacherBankEntity teacher = teacher(12L, "李老师", "13800138002", "110101199001011235");
    when(teacherBankMapper.selectById(12L)).thenReturn(teacher);
    when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(platformUserEnsureService.ensureFromDivisionPerson(
            eq("李老师"), eq("110101199001011235"), eq("13800138002"), any(), any()))
        .thenReturn(new PlatformUserEnsureService.EnsureResult(
            PlatformUserEnsureService.Outcome.REUSED, 8002L, null));

    TrainClassRoleAssignReq req = new TrainClassRoleAssignReq();
    req.setRoleCode(TrainClassRoleApplicationService.ASSISTANT);
    req.setSubjectType("teacher_bank");
    req.setSubjectId(12L);

    TrainClassRoleVO vo = service.assign(100L, req);
    assertThat(vo.platformUserId()).isEqualTo(8002L);
    assertThat(vo.bindStatus()).isEqualTo("reused");
  }

  @Test
  void assign_rejectsWhenMissingIdCard() {
    stubClass(100L);
    TrainTeacherBankEntity teacher = teacher(13L, "缺证老师", "13800138003", null);
    when(teacherBankMapper.selectById(13L)).thenReturn(teacher);

    TrainClassRoleAssignReq req = new TrainClassRoleAssignReq();
    req.setRoleCode(TrainClassRoleApplicationService.LEAD_TEACHER);
    req.setSubjectType("teacher_bank");
    req.setSubjectId(13L);

    assertThatThrownBy(() -> service.assign(100L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("身份证");
    verify(roleMapper, never()).insert(any(TrainClassRoleEntity.class));
    verify(platformUserEnsureService, never())
        .ensureFromDivisionPerson(any(), any(), any(), any(), any());
  }

  @Test
  void assign_rejectsWhenMissingPhone() {
    stubClass(100L);
    TrainTeacherBankEntity teacher = teacher(14L, "缺号老师", "", "110101199001011236");
    when(teacherBankMapper.selectById(14L)).thenReturn(teacher);

    TrainClassRoleAssignReq req = new TrainClassRoleAssignReq();
    req.setRoleCode(TrainClassRoleApplicationService.FOLLOW_STAFF);
    req.setSubjectType("teacher_bank");
    req.setSubjectId(14L);

    assertThatThrownBy(() -> service.assign(100L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("手机号");
    verify(roleMapper, never()).insert(any(TrainClassRoleEntity.class));
  }

  @Test
  void assign_rejectsWhenPhoneOccupied() {
    stubClass(100L);
    TrainTeacherBankEntity teacher = teacher(15L, "冲突老师", "13800138005", "110101199001011237");
    when(teacherBankMapper.selectById(15L)).thenReturn(teacher);
    when(platformUserEnsureService.ensureFromDivisionPerson(
            eq("冲突老师"), eq("110101199001011237"), eq("13800138005"), any(), any()))
        .thenReturn(PlatformUserEnsureService.EnsureResult.skipped("手机号已被其他学员占用"));

    TrainClassRoleAssignReq req = new TrainClassRoleAssignReq();
    req.setRoleCode(TrainClassRoleApplicationService.LEAD_TEACHER);
    req.setSubjectType("teacher_bank");
    req.setSubjectId(15L);

    assertThatThrownBy(() -> service.assign(100L, req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("其他身份证");
    verify(roleMapper, never()).insert(any(TrainClassRoleEntity.class));
  }

  @Test
  void ensureAppAccount_bindsUnboundRole() {
    stubClass(100L);
    TrainClassRoleEntity role = new TrainClassRoleEntity();
    role.setId(77L);
    role.setClassId(100L);
    role.setRoleCode(TrainClassRoleApplicationService.LEAD_TEACHER);
    role.setSubjectType("teacher_bank");
    role.setSubjectId(16L);
    role.setDisplayName("补开老师");
    role.setPhone("13800138006");
    role.setStatus("active");
    role.setDeleted(0);
    when(roleMapper.selectById(77L)).thenReturn(role);

    TrainTeacherBankEntity teacher = teacher(16L, "补开老师", "13800138006", "110101199001011238");
    when(teacherBankMapper.selectById(16L)).thenReturn(teacher);
    when(platformUserEnsureService.ensureFromDivisionPerson(
            eq("补开老师"), eq("110101199001011238"), eq("13800138006"), any(), any()))
        .thenReturn(new PlatformUserEnsureService.EnsureResult(
            PlatformUserEnsureService.Outcome.CREATED, 7007L, null));

    TrainClassRoleVO vo = service.ensureAppAccount(100L, 77L);
    assertThat(vo.platformUserId()).isEqualTo(7007L);
    assertThat(vo.bindStatus()).isEqualTo("ensured");
    verify(roleMapper).updateById(role);
    assertThat(role.getPlatformUserId()).isEqualTo(7007L);
  }

  @Test
  void ensureAppAccount_returnsBound_whenAlreadyHasAccount() {
    stubClass(100L);
    TrainClassRoleEntity role = new TrainClassRoleEntity();
    role.setId(78L);
    role.setClassId(100L);
    role.setRoleCode(TrainClassRoleApplicationService.ASSISTANT);
    role.setPlatformUserId(555L);
    role.setStatus("active");
    role.setDeleted(0);
    when(roleMapper.selectById(78L)).thenReturn(role);

    TrainClassRoleVO vo = service.ensureAppAccount(100L, 78L);
    assertThat(vo.bindStatus()).isEqualTo("bound");
    verify(platformUserEnsureService, never())
        .ensureFromDivisionPerson(any(), any(), any(), any(), any());
  }

  private void stubClass(Long classId) {
    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setId(classId);
    clazz.setDeleted(0);
    clazz.setCompanyId(9L);
    when(classMapper.selectById(classId)).thenReturn(clazz);
  }

  private static TrainTeacherBankEntity teacher(
      Long id, String name, String phone, String idCard) {
    TrainTeacherBankEntity t = new TrainTeacherBankEntity();
    t.setId(id);
    t.setName(name);
    t.setPhone(phone);
    t.setIdCardNumber(idCard);
    t.setDeleted(0);
    t.setCompanyId(9L);
    return t;
  }
}
