package com.guoyu.platform.certification.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalScoringConfigEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalStartSessionEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalScoringConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalStartSessionMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PracticalStartApplicationServiceTest {

  private final CertificationPracticalStartSessionMapper sessionMapper = mock(
      CertificationPracticalStartSessionMapper.class);
  private final CertificationPracticalScoringConfigMapper configMapper = mock(
      CertificationPracticalScoringConfigMapper.class);
  private final CertificationAssessorTaskMapper taskMapper = mock(CertificationAssessorTaskMapper.class);
  private final CertificationCandidateMapper candidateMapper = mock(CertificationCandidateMapper.class);
  private final CertificationInfoAssessorMapper assessorMapper = mock(CertificationInfoAssessorMapper.class);
  private final PracticalStartEventPublisher publisher = mock(PracticalStartEventPublisher.class);
  private final AtomicReference<CertificationInfoAssessorEntity> currentAssessor = new AtomicReference<>();
  private PracticalStartApplicationService service;
  private CertificationPracticalStartSessionEntity session;

  @BeforeEach
  void setUp() {
    service = new PracticalStartApplicationService(
        sessionMapper, configMapper, taskMapper, candidateMapper, assessorMapper, publisher, new ObjectMapper());
    session = new CertificationPracticalStartSessionEntity();
    session.setId(30L);
    session.setTenantId(1L);
    session.setTaskId(1L);
    session.setCandidateId(2L);
    session.setConfigId(3L);
    session.setPaperId(4L);
    session.setGroupId(5L);
    session.setAssessorConfirmationsJson("{}");
    session.setStatus("waiting");
    session.setVersionNo(0);
    when(sessionMapper.lockSession(1L, 1L, 2L)).thenReturn(session);

    CertificationPracticalScoringConfigEntity config = new CertificationPracticalScoringConfigEntity();
    config.setId(3L);
    config.setPaperId(4L);
    config.setGroupId(5L);
    when(configMapper.selectById(3L)).thenReturn(config);
    when(taskMapper.selectList(any())).thenReturn(List.of(
        task(101L, 11L), task(102L, 12L), task(103L, 13L)));
    when(assessorMapper.selectOne(any())).thenAnswer(invocation -> currentAssessor.get());
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(2L);
    candidate.setTaskId(1L);
    candidate.setStatus("active");
    when(candidateMapper.selectById(2L)).thenReturn(candidate);
    when(sessionMapper.lockStartedSessionsByGroup(1L, 1L, 5L)).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void leaderStartsCandidateAndOtherAssessorsOnlyConfirm() {
    authenticate(900L, 11L);
    PracticalScoringModels.StartSessionView candidate = service.confirmCandidate(1L, 2L);
    assertThat(candidate.status()).isEqualTo("waiting");
    assertThat(candidate.candidateConfirmed()).isTrue();
    assertThat(candidate.startedAt()).isNull();

    assertThat(confirm(901L, 11L).status()).isEqualTo("waiting");
    assertThat(confirm(902L, 12L).confirmedAssessors()).isEqualTo(2);
    PracticalScoringModels.StartSessionView ready = confirm(903L, 13L);

    assertThat(ready.status()).isEqualTo("ready");
    assertThat(ready.confirmedAssessors()).isEqualTo(3);
    assertThat(ready.startedAt()).isNull();

    authenticate(901L, 11L);
    PracticalScoringModels.StartSessionView started = service.startByAssessorTask(111L);
    assertThat(started.status()).isEqualTo("started");
    assertThat(started.confirmedAssessors()).isEqualTo(3);
    assertThat(started.startedAt()).isNotNull();
    ArgumentCaptor<PracticalScoringModels.StartSessionView> eventCaptor =
        ArgumentCaptor.forClass(PracticalScoringModels.StartSessionView.class);
    verify(publisher, atLeastOnce()).publish(eq(1L), eventCaptor.capture());
    assertThat(eventCaptor.getAllValues())
        .anySatisfy(event -> assertThat(event.currentAssessorConfirmed()).isFalse());
  }

  @Test
  void cancelUnstartedSessionAllowsWaitingAndReadyButRejectsStartedAndCompleted() {
    authenticate(900L, 11L);

    service.cancelUnstartedSession(1L, 2L);
    verify(sessionMapper).deleteById(30L);

    session.setStatus("ready");
    service.cancelUnstartedSession(1L, 2L);

    session.setStatus("started");
    assertThatThrownBy(() -> service.cancelUnstartedSession(1L, 2L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessageContaining("已开始或已完成");

    session.setStatus("completed");
    assertThatThrownBy(() -> service.cancelUnstartedSession(1L, 2L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessageContaining("已开始或已完成");
  }

  private PracticalScoringModels.StartSessionView confirm(Long userId, Long assessorId) {
    authenticate(userId, assessorId);
    CertificationAssessorTaskEntity ownedTask = task(100L + assessorId, assessorId);
    when(taskMapper.selectById(ownedTask.getId())).thenReturn(ownedTask);
    return service.confirmAssessor(ownedTask.getId());
  }

  private void authenticate(Long userId, Long assessorId) {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        userId, 1L, "assessor" + assessorId, "考评员" + assessorId, "platform",
        List.of("CERTIFICATION_ASSESSOR"), List.of("certification.practical.score"));
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(assessorId);
    assessor.setUserId(userId);
    assessor.setAuditStatus("approved");
    currentAssessor.set(assessor);
  }

  private CertificationAssessorTaskEntity task(Long id, Long assessorId) {
    CertificationAssessorTaskEntity task = new CertificationAssessorTaskEntity();
    task.setId(id);
    task.setTaskId(1L);
    task.setCandidateId(2L);
    task.setPaperId(4L);
    task.setAssessorId(assessorId);
    task.setLeaderAssessorId(11L);
    return task;
  }
}
