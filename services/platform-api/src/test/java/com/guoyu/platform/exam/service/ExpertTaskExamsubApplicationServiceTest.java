package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.ExpertTaskExamsubSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamsubMapper;
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

class ExpertTaskExamsubApplicationServiceTest {

  private final ExamTaskExpertMapper expertMapper = Mockito.mock(ExamTaskExpertMapper.class);
  private final ExamTaskExamsubMapper taskMapper = Mockito.mock(ExamTaskExamsubMapper.class);
  private final ExamTaskExamsubAuditRecordMapper auditMapper = Mockito.mock(ExamTaskExamsubAuditRecordMapper.class);

  private final ExpertTaskExamsubApplicationService service = new ExpertTaskExamsubApplicationService(
      expertMapper, taskMapper, auditMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        400L, 1L, "expert_c", "专家C", "expert",
        List.of("EXPERT"),
        List.of("expert:task-examsub:view", "expert:task-examsub:submit")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void myTasks_filtersByCurrentUser() {
    ExamTaskExpertEntity e = new ExamTaskExpertEntity();
    e.setId(1); e.setTaskId(30); e.setExpertId(400); e.setTaskRole(0); e.setTaskType("examsub");
    Page<ExamTaskExpertEntity> pg = new Page<>(1, 20);
    pg.setRecords(List.of(e)); pg.setTotal(1);
    when(expertMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pg);
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(30); t.setNums(3);
    when(taskMapper.selectById(30)).thenReturn(t);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    var resp = service.myTasks(0, 1, 20);
    assertThat(resp.items().get(0).getTag()).isEqualTo("examsub");
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
    rec.setId(1); rec.setTaskId(30); rec.setExpertId(400); rec.setTaskRole(0); rec.setTaskType("examsub");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(30); t.setNums(3); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

    ExpertTaskExamsubSubmitReq req = new ExpertTaskExamsubSubmitReq();
    req.setTaskId(30); req.setRole(0); req.setId(301);
    assertThatThrownBy(() -> service.submit(req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经提交过");
  }

  @Test
  void submit_advancesStatus_partial() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(30); rec.setExpertId(400); rec.setTaskRole(0); rec.setTaskType("examsub");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(30); t.setNums(3); t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

    ExpertTaskExamsubSubmitReq req = new ExpertTaskExamsubSubmitReq();
    req.setTaskId(30); req.setRole(0); req.setId(301);
    req.setContent("主观题题干"); req.setAnswer("参考答案");
    ExamTaskSubmitResultVO out = service.submit(req);
    assertThat(out.getIsdone()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(1);
    verify(auditMapper).insert(any(ExamTaskExamsubAuditRecordEntity.class));
  }

  @Test
  void submit_advancesStatus_fullFinal() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(30); rec.setExpertId(400); rec.setTaskRole(0); rec.setTaskType("examsub");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamsubEntity t = new ExamTaskExamsubEntity();
    t.setId(30); t.setNums(2); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

    ExpertTaskExamsubSubmitReq req = new ExpertTaskExamsubSubmitReq();
    req.setTaskId(30); req.setRole(0); req.setId(301);
    ExamTaskSubmitResultVO out = service.submit(req);
    assertThat(out.getIsdone()).isEqualTo(1);
    assertThat(t.getStatus()).isEqualTo(2);
  }
}
