package com.guoyu.platform.certification.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.entity.CertificationPracticalStartSessionEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorWorkbenchContextMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalStartSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AssessorWorkbenchContextServiceTest {

  private final CertificationInfoAssessorMapper assessorMapper = mock(CertificationInfoAssessorMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = mock(CertificationTaskAssessorMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskMapper taskMapper = mock(CertificationTaskMapper.class);
  private final CertificationAssessorWorkbenchContextMapper contextMapper = mock(CertificationAssessorWorkbenchContextMapper.class);
  private final CertificationPracticalStartSessionMapper sessionMapper = mock(CertificationPracticalStartSessionMapper.class);
  private final PracticalCompleteSignatureApplicationService completeSignatureService =
      mock(PracticalCompleteSignatureApplicationService.class);
  private AssessorWorkbenchContextService service;

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "组长", "platform", List.of("assessor"), List.of("certification.practical.score"),"asd@qq.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setUserId(100L);
    assessor.setAssessorName("组长");
    assessor.setAuditStatus("approved");
    when(assessorMapper.selectOne(any())).thenReturn(assessor);

    CertificationTaskAssessorEntity membership = new CertificationTaskAssessorEntity();
    membership.setId(10L);
    membership.setTaskId(1L);
    membership.setGroupId(2L);
    membership.setAssessorId(9L);
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(membership));
    CertificationTaskAssessorGroupEntity group = new CertificationTaskAssessorGroupEntity();
    group.setId(2L);
    group.setLeaderAssessorId(9L);
    when(groupMapper.selectBatchIds(any())).thenReturn(List.of(group));
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setTaskNo("RD-001");
    task.setTaskName("铝业实操认定");
    task.setStatus("in_progress");
    when(taskMapper.selectBatchIds(any())).thenReturn(List.of(task));
    when(contextMapper.selectOne(any())).thenReturn(null);
    when(sessionMapper.selectCount(any())).thenReturn(0L);
    when(completeSignatureService.viewForAssessor(any(), any())).thenReturn(
        PracticalCompleteSignatureApplicationService.emptyView(1L));
    service = new AssessorWorkbenchContextService(
        assessorMapper, taskAssessorMapper, groupMapper, taskMapper, contextMapper, sessionMapper,
        completeSignatureService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void leaderCanSelectOwnBoundTaskAndItBecomesTheActiveContext() {
    PracticalScoringModels.AssessorWorkbenchContext context = service.current();

    assertThat(context.leader()).isTrue();
    assertThat(context.activeTaskId()).isEqualTo(1L);
    assertThat(context.switchableTasks()).extracting(PracticalScoringModels.AssessorWorkbenchTask::taskId)
        .containsExactly(1L);

    service.switchTask(1L);

    verify(contextMapper).insert(any(com.guoyu.platform.certification.entity.CertificationAssessorWorkbenchContextEntity.class));
  }

  @Test
  void idleMembershipIsIgnoredAsReleasedHistory() {
    CertificationTaskAssessorEntity membership = new CertificationTaskAssessorEntity();
    membership.setId(10L);
    membership.setTaskId(1L);
    membership.setGroupId(2L);
    membership.setAssessorId(9L);
    membership.setWorkStatus("idle");
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(membership));

    PracticalScoringModels.AssessorWorkbenchContext context = service.current();

    assertThat(context.activeTaskId()).isNull();
    assertThat(context.switchableTasks()).isEmpty();
  }

  @Test
  void leaderCannotSelectAnUnboundTask() {
    assertThatThrownBy(() -> service.switchTask(99L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("无权切换");
  }
}
