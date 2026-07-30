package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.ExamTaskExamAssignExpertReq;
import com.guoyu.platform.exam.dto.ExamTaskExamQueryReq;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
import com.guoyu.platform.exam.mapper.ExamTaskExamAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskExamVO;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ExamTaskExamApplicationServiceTest {

  private final ExamTaskExamMapper taskMapper = Mockito.mock(ExamTaskExamMapper.class);
  private final ExamTaskExamDetailMapper detailMapper = Mockito.mock(ExamTaskExamDetailMapper.class);
  private final ExamTaskExamAuditRecordMapper auditMapper = Mockito.mock(ExamTaskExamAuditRecordMapper.class);
  private final ExamTaskExpertMapper expertMapper = Mockito.mock(ExamTaskExpertMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final UserRoleRelationMapper userRoleMapper = Mockito.mock(UserRoleRelationMapper.class);
  private final RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
  private final ExamWorkTypeApplicationService workTypeService = Mockito.mock(ExamWorkTypeApplicationService.class);

  private final TaskAuditAssignHelper helper = new TaskAuditAssignHelper(workTypeService, userMapper);

  private final ExamTaskExamApplicationService service = new ExamTaskExamApplicationService(
      taskMapper, detailMapper, auditMapper, expertMapper,
      userMapper, userRoleMapper, roleMapper, helper, workTypeService);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"),
        List.of("task:exam-audit:view", "task:exam-audit:assign-expert", "task:exam-audit:delete")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void page_returnsTasksWithEnrichedNames() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10);
    t.setWorkTypeId(1);
    t.setLevel(1);
    t.setNums(5);
    t.setStatus(0);
    t.setAddtime(LocalDateTime.of(2026, 5, 15, 9, 0));

    Page<ExamTaskExamEntity> pg = new Page<>(1, 20);
    pg.setRecords(List.of(t));
    pg.setTotal(1);
    when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pg);

    ExamWorkTypeVO wtVo = new ExamWorkTypeVO();
    wtVo.setId(1);
    wtVo.setTitle("电工");
    when(workTypeService.getById(1)).thenReturn(wtVo);
    when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

    ExamTaskExamQueryReq req = new ExamTaskExamQueryReq();
    var resp = service.page(req);

    assertThat(resp.total()).isEqualTo(1);
    ExamTaskExamVO vo = resp.items().get(0);
    assertThat(vo.getWorkTypeCn()).isEqualTo("电工");
    assertThat(vo.getLevelCn()).isEqualTo("高级技师");
    assertThat(vo.getStatusCn()).isEqualTo("待审核");
    assertThat(vo.getExpertStartName()).isEqualTo("待指派");
  }

  @Test
  void delete_cascadesDetailsAndAuditRecords() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(5);
    t.setTenantId(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);

    service.delete(5);

    verify(taskMapper).deleteById(5);
    verify(detailMapper).delete(any(LambdaQueryWrapper.class));
    verify(auditMapper).delete(any(LambdaQueryWrapper.class));
    verify(expertMapper).delete(any(LambdaQueryWrapper.class));
  }

  @Test
  void assignExpert_throwsWhenSameAsOtherRole() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(7);
    t.setTenantId(1);
    t.setExpertStart(2);
    t.setExpertEnd(null);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);

    // 终审 = 初审 (2) 报错
    assertThatThrownBy(() -> service.assignExpert(new ExamTaskExamAssignExpertReq(7, 2, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不能和初审是同一人");

    // 初审 = 终审 (3) 报错
    t.setExpertStart(null);
    t.setExpertEnd(3);
    assertThatThrownBy(() -> service.assignExpert(new ExamTaskExamAssignExpertReq(7, 3, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不能和终审是同一人");
  }

  @Test
  void assignExpert_throwsWhenDuplicate() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(8);
    t.setTenantId(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(expertMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

    assertThatThrownBy(() -> service.assignExpert(new ExamTaskExamAssignExpertReq(8, 5, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经指派过了");
  }

  @Test
  void assignExpert_marksStatusAsInProgress() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(9);
    t.setTenantId(1);
    t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(expertMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

    service.assignExpert(new ExamTaskExamAssignExpertReq(9, 6, 0));

    assertThat(t.getExpertStart()).isEqualTo(6);
    assertThat(t.getStatus()).isEqualTo(1);
    verify(taskMapper).updateById(t);
    verify(expertMapper).insert(any(ExamTaskExpertEntity.class));
  }

  @Test
  void getDaily_groupsByYmd() {
    // 直接构造 wrapper 会被 MyBatis-Plus 拒绝,这里改成 mock selectList 任意条件
    when(auditMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(List.of(mkAudit("2026-05-20"), mkAudit("2026-05-20"), mkAudit("2026-05-21")));

    var out = service.getDaily(1, 0);
    assertThat(out).hasSize(2);
    assertThat(out.stream().filter(v -> "2026-05-20".equals(v.getYmd())).findFirst().orElseThrow().getNums()).isEqualTo(2L);
    assertThat(out.stream().filter(v -> "2026-05-21".equals(v.getYmd())).findFirst().orElseThrow().getNums()).isEqualTo(1L);
  }

  @Test
  void autoCompleteStatus_marksCompletedWhenAuditCountMatches() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(100);
    t.setTenantId(1);
    t.setStatus(2);
    t.setNums(3);

    when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));

    List<ExamTaskExamAuditRecordEntity> audits = List.of(mkAuditForTask(100), mkAuditForTask(100), mkAuditForTask(100));
    when(auditMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(audits);

    int updated = TaskAuditAssignHelper.autoCompleteStatus(1, taskMapper, auditMapper);

    assertThat(updated).isEqualTo(1);
    assertThat(t.getStatus()).isEqualTo(3);
    verify(taskMapper).updateById(t);
  }

  @Test
  void assignExpert_acceptsValidAndInsertsExpertRecord() {
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(11);
    t.setTenantId(1);
    t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(expertMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

    service.assignExpert(new ExamTaskExamAssignExpertReq(11, 7, 1));

    assertThat(t.getExpertEnd()).isEqualTo(7);
    assertThat(t.getStatus()).isEqualTo(1);
    verify(expertMapper).insert(any(ExamTaskExpertEntity.class));
  }

  // ─── helpers ───
  private static ExamTaskExamAuditRecordEntity mkAudit(String ymd) {
    ExamTaskExamAuditRecordEntity a = new ExamTaskExamAuditRecordEntity();
    a.setYmd(ymd);
    return a;
  }

  private static ExamTaskExamAuditRecordEntity mkAuditForTask(Integer taskId) {
    ExamTaskExamAuditRecordEntity a = new ExamTaskExamAuditRecordEntity();
    a.setTaskId(taskId);
    a.setAuditRole(1);
    a.setAuditStatus(1);
    return a;
  }
}
