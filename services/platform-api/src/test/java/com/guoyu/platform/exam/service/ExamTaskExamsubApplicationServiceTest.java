package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.ExamTaskExamsubAssignExpertReq;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.helper.TaskAuditAssignHelper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ExamTaskExamsubApplicationServiceTest {

  private final ExamTaskExamsubMapper taskMapper = Mockito.mock(ExamTaskExamsubMapper.class);
  private final ExamTaskExamsubDetailMapper detailMapper = Mockito.mock(ExamTaskExamsubDetailMapper.class);
  private final ExamTaskExamsubAuditRecordMapper auditMapper = Mockito.mock(ExamTaskExamsubAuditRecordMapper.class);
  private final ExamTaskExpertMapper expertMapper = Mockito.mock(ExamTaskExpertMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final UserRoleRelationMapper userRoleMapper = Mockito.mock(UserRoleRelationMapper.class);
  private final RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
  private final ExamWorkTypeApplicationService workTypeService = Mockito.mock(ExamWorkTypeApplicationService.class);
  private final TaskAuditAssignHelper helper = new TaskAuditAssignHelper(workTypeService, userMapper);

  private final ExamTaskExamsubApplicationService service = new ExamTaskExamsubApplicationService(
      taskMapper, detailMapper, auditMapper, expertMapper,
      userMapper, userRoleMapper, roleMapper, helper, workTypeService);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"),
        List.of("task:examsub-audit:view", "task:examsub-audit:assign-expert", "task:examsub-audit:delete"),"asd@qq.com"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void page_returnsTasksWithEnrichedNames() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1); t.setWorkTypeId(1); t.setLevel(1); t.setNums(3); t.setStatus(0);
    Page<ExamTaskExamsubEntity> pg = new Page<>(1, 20);
    pg.setRecords(List.of(t)); pg.setTotal(1);
    when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pg);
    ExamWorkTypeVO wt = new ExamWorkTypeVO();
    wt.setId(1); wt.setTitle("电工");
    when(workTypeService.getById(1)).thenReturn(wt);
    when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

    var resp = service.page(new com.guoyu.platform.exam.dto.ExamTaskExamsubQueryReq());
    assertThat(resp.items().get(0).getWorkTypeCn()).isEqualTo("电工");
    assertThat(resp.items().get(0).getLevelCn()).isEqualTo("高级技师");
  }

  @Test
  void delete_cascadesDetailsAndAuditRecords() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1); t.setTenantId(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    service.delete(1);
    verify(taskMapper).deleteById(1);
    verify(detailMapper).delete(any(LambdaQueryWrapper.class));
    verify(auditMapper).delete(any(LambdaQueryWrapper.class));
    verify(expertMapper).delete(any(LambdaQueryWrapper.class));
  }

  @Test
  void assignExpert_throwsWhenSameAsOtherRole() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1); t.setExpertStart(9);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    assertThatThrownBy(() -> service.assignExpert(new ExamTaskExamsubAssignExpertReq(1, 9, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("不能和初审是同一人");
  }

  @Test
  void assignExpert_throwsWhenDuplicate() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(expertMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
    assertThatThrownBy(() -> service.assignExpert(new ExamTaskExamsubAssignExpertReq(1, 11, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经指派过了");
  }

  @Test
  void assignExpert_marksStatusAsInProgress() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1); t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(expertMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    service.assignExpert(new ExamTaskExamsubAssignExpertReq(1, 12, 1));
    assertThat(t.getExpertEnd()).isEqualTo(12);
    assertThat(t.getStatus()).isEqualTo(1);
    verify(expertMapper).insert(any(ExamTaskExpertEntity.class));
  }

  @Test
  void getDaily_groupsByYmd() {
    when(auditMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(mk("2026-05-20"), mk("2026-05-20"), mk("2026-05-21")));
    var out = service.getDaily(1, 0);
    assertThat(out).hasSize(2);
  }

  @Test
  void autoCompleteStatus_marksCompletedWhenAuditCountMatches() {
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(1); t.setStatus(2); t.setNums(2);
    when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));
    when(auditMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mkForTask(1), mkForTask(1)));
    int updated = TaskAuditAssignHelper.autoCompleteStatusExamsub(1, taskMapper, auditMapper);
    assertThat(updated).isEqualTo(1);
    assertThat(t.getStatus()).isEqualTo(3);
  }

  private static ExamTaskExamsubAuditRecordEntity mk(String ymd) {
    ExamTaskExamsubAuditRecordEntity a = new ExamTaskExamsubAuditRecordEntity();
    a.setYmd(ymd);
    return a;
  }
  private static ExamTaskExamsubAuditRecordEntity mkForTask(Integer taskId) {
    ExamTaskExamsubAuditRecordEntity a = new ExamTaskExamsubAuditRecordEntity();
    a.setTaskId(taskId); a.setAuditRole(1); a.setAuditStatus(1);
    return a;
  }
}
