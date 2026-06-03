package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.dto.ExpertTaskExamSubmitReq;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExpertEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamAuditRecordMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExpertMapper;
import com.guoyu.platform.exam.vo.ExamTaskSubmitResultVO;
import com.guoyu.platform.exam.vo.ExpertTaskVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ExpertTaskExamApplicationServiceTest {

  private final ExamTaskExpertMapper expertMapper = Mockito.mock(ExamTaskExpertMapper.class);
  private final ExamTaskExamMapper taskMapper = Mockito.mock(ExamTaskExamMapper.class);
  private final ExamTaskExamAuditRecordMapper auditMapper = Mockito.mock(ExamTaskExamAuditRecordMapper.class);

  private final ExpertTaskExamApplicationService service = new ExpertTaskExamApplicationService(
      expertMapper, taskMapper, auditMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        200L, 1L, "expert_a", "专家A", "expert",
        List.of("EXPERT"),
        List.of("expert:task-exam:view", "expert:task-exam:submit")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() { SecurityContextHolder.clearContext(); }

  @Test
  void myTasks_filtersByCurrentUser() {
    ExamTaskExpertEntity e1 = new ExamTaskExpertEntity();
    e1.setId(1); e1.setTaskId(10); e1.setExpertId(200); e1.setTaskRole(0); e1.setTaskType("exam");
    Page<ExamTaskExpertEntity> pg = new Page<>(1, 20);
    pg.setRecords(List.of(e1)); pg.setTotal(1);
    when(expertMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pg);

    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10); t.setWorkTypeId(1); t.setLevel(1); t.setNums(5);
    when(taskMapper.selectById(10)).thenReturn(t);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    var resp = service.myTasks(0, 1, 20);
    assertThat(resp.total()).isEqualTo(1);
    ExpertTaskVO vo = resp.items().get(0);
    assertThat(vo.getTaskId()).isEqualTo(10);
    assertThat(vo.getTag()).isEqualTo("exam");
    assertThat(vo.getNums()).isEqualTo(5);
  }

  @Test
  void getTaskDetail_returnsWhenAssignedToCurrentUser() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(10); rec.setExpertId(200); rec.setTaskRole(0); rec.setTaskType("exam");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10); t.setNums(5);
    when(taskMapper.selectById(10)).thenReturn(t);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

    var vo = service.getTaskDetail(10);
    assertThat(vo.getTaskId()).isEqualTo(10);
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
    rec.setId(1); rec.setTaskId(10); rec.setExpertId(200); rec.setTaskRole(0); rec.setTaskType("exam");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10); t.setNums(5); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

    ExpertTaskExamSubmitReq req = new ExpertTaskExamSubmitReq();
    req.setTaskId(10); req.setRole(0); req.setId(101);
    req.setExamContent("测试"); req.setExamAnswer("A");
    assertThatThrownBy(() -> service.submit(req))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已经提交过");
  }

  @Test
  void submit_advancesStatusCorrectly_partialSubmission() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(10); rec.setExpertId(200); rec.setTaskRole(0); rec.setTaskType("exam");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10); t.setNums(5); t.setStatus(0);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);  // 1 / 5

    ExpertTaskExamSubmitReq req = new ExpertTaskExamSubmitReq();
    req.setTaskId(10); req.setRole(0); req.setId(101);
    req.setExamContent("题干"); req.setExamAnswer("A");
    ExamTaskSubmitResultVO out = service.submit(req);

    assertThat(out.getCount()).isEqualTo(1L);
    assertThat(out.getTotal()).isEqualTo(5L);
    assertThat(out.getIsdone()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(1);  // 初审核中
    verify(auditMapper).insert(any(ExamTaskExamAuditRecordEntity.class));
  }

  @Test
  void submit_advancesStatusCorrectly_fullSubmissionPushesToFinal() {
    ExamTaskExpertEntity rec = new ExamTaskExpertEntity();
    rec.setId(1); rec.setTaskId(10); rec.setExpertId(200); rec.setTaskRole(0); rec.setTaskType("exam");
    when(expertMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rec);
    ExamTaskExamEntity t = new ExamTaskExamEntity();
    t.setId(10); t.setNums(3); t.setStatus(1);
    when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(t);
    when(auditMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
    when(auditMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);  // 全部

    ExpertTaskExamSubmitReq req = new ExpertTaskExamSubmitReq();
    req.setTaskId(10); req.setRole(0); req.setId(101);
    ExamTaskSubmitResultVO out = service.submit(req);

    assertThat(out.getIsdone()).isEqualTo(1);
    assertThat(t.getStatus()).isEqualTo(2);  // 终审中
    assertThat(rec.getEndtime()).isNotNull();
  }
}
