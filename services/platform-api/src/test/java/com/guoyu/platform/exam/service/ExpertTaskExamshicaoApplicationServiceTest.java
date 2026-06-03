package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.ExpertTaskExamshicaoSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskSubmitResultVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ExpertTaskExamshicaoApplicationServiceTest {

  private final ExamTaskExpertMapper expertMapper = Mockito.mock(ExamTaskExpertMapper.class);
  private final ExamTaskExamshicaoMapper taskMapper = Mockito.mock(ExamTaskExamshicaoMapper.class);
  private final ExamTaskExamshicaoAuditRecordMapper auditMapper = Mockito.mock(ExamTaskExamshicaoAuditRecordMapper.class);

  private final ExpertTaskExamshicaoApplicationService service = new ExpertTaskExamshicaoApplicationService(
      expertMapper, taskMapper, auditMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        300L, 1L, "expert_b", "专家B", "expert",
        List.of("EXPERT"),
        List.of("expert:task-examshicao:view", "expert:task-examshicao:submit")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void myTasks_filtersByCurrentUser() {
    ExamTaskExpertEntity e = new ExamTaskExpertEntity();
    e.setId(1); e.setTaskId(20); e.setExpertId(300); e.setTaskRole(0); e.setTaskType("examshicao");
    Page<ExamTaskExpertEntity> pg = new Page<>(1, 20);
    pg.setRecords(List.of(e)); pg.setTotal(1);
    when(expertMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pg);
    ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
    t.setId(20); t.setNums(3);
    when(taskMapper.selectById(20)).thenReturn(t);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    var resp = service.myTasks(0, 1, 20);
    assertThat(resp.items().get(0).getTag()).isEqualTo("examshicao");
  }

  @Test
  void getTaskDetail_throwsWhenNotAssigned() {
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    assertThatThrownBy(() -> service.getTaskDetail(99))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("未指派给您");
  }

  @Test
  void submit_throwsWhenDuplicate() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(20); rec.setExpertId(300); rec.setTaskRole(0); rec.setTaskType("examshicao");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
    t.setId(20); t.setNums(3); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

    ExpertTaskExamshicaoSubmitReq req = new ExpertTaskExamshicaoSubmitReq();
    req.setTaskId(20); req.setRole(0); req.setId(201);
    assertThatThrownBy(() -> service.submit(req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经提交过");
  }

  @Test
  void submit_advancesStatus_partial() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(20); rec.setExpertId(300); rec.setTaskRole(0); rec.setTaskType("examshicao");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
    t.setId(20); t.setNums(3); t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

    ExpertTaskExamshicaoSubmitReq req = new ExpertTaskExamshicaoSubmitReq();
    req.setTaskId(20); req.setRole(0); req.setId(201);
    req.setContent("操作题题干"); req.setPingfen("[]");
    ExamTaskSubmitResultVO out = service.submit(req);
    assertThat(out.getIsdone()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(1);
    verify(auditMapper).insert(any(ExamTaskExamshicaoAuditRecordEntity.class));
  }

  @Test
  void submit_advancesStatus_fullPushesToFinal() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(20); rec.setExpertId(300); rec.setTaskRole(0); rec.setTaskType("examshicao");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamshicaoEntity t = new ExamTaskExamshicaoEntity();
    t.setId(20); t.setNums(2); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

    ExpertTaskExamshicaoSubmitReq req = new ExpertTaskExamshicaoSubmitReq();
    req.setTaskId(20); req.setRole(0); req.setId(201);
    ExamTaskSubmitResultVO out = service.submit(req);
    assertThat(out.getIsdone()).isEqualTo(1);
    assertThat(t.getStatus()).isEqualTo(2);
    assertThat(rec.getEndtime()).isNotNull();
  }
}
