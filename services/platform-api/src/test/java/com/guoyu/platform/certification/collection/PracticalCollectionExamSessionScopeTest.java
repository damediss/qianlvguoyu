package com.guoyu.platform.certification.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.collection.application.PracticalCollectionApplicationService;
import com.guoyu.platform.certification.collection.converter.PracticalCollectionConverter;
import com.guoyu.platform.certification.collection.domain.CollectionEligibilityResult;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionDomainService;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionEligibilityService;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionStatusMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PracticalCollectionExamSessionScopeTest {

  private static final Long TENANT_ID = 10159L;
  private static final Long TASK_ID = 200L;
  private static final Long SESSION_ID = 300L;
  private static final Long CANDIDATE_ID = 400L;

  @Mock CertificationTaskMapper taskMapper;
  @Mock CertificationCandidateMapper candidateMapper;
  @Mock CertificationExamSessionMapper examSessionMapper;
  @Mock CertificationCandidateExamSessionMapper candidateExamSessionMapper;
  @Mock PracticalCollectionStatusMapper statusMapper;
  @Mock PracticalCollectionEligibilityService eligibilityService;

  private PracticalCollectionApplicationService service;

  @BeforeEach
  void setUp() {
    service = new PracticalCollectionApplicationService(
        taskMapper,
        candidateMapper,
        examSessionMapper,
        candidateExamSessionMapper,
        null,
        null,
        null,
        statusMapper,
        null,
        null,
        null,
        null,
        null,
        new PracticalCollectionDomainService(),
        eligibilityService,
        new PracticalCollectionConverter(),
        "");
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1L, TENANT_ID, "tester", "tester", "admin", List.of("PLATFORM_ADMIN"), List.of(), "");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void initializesIndependentStatusForAssignedCandidateInSelectedSession() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(TASK_ID);
    task.setTenantId(TENANT_ID);
    task.setDeleted(0);
    when(taskMapper.selectOne(any())).thenReturn(task);

    CertificationExamSessionEntity session = new CertificationExamSessionEntity();
    session.setId(SESSION_ID);
    session.setTenantId(TENANT_ID);
    session.setTaskId(TASK_ID);
    session.setDeleted(0);
    when(examSessionMapper.selectOne(any())).thenReturn(session);

    CertificationCandidateExamSessionEntity relation = new CertificationCandidateExamSessionEntity();
    relation.setCandidateId(CANDIDATE_ID);
    relation.setParticipationStatus("ASSIGNED");
    when(candidateExamSessionMapper.selectBySession(TENANT_ID, TASK_ID, SESSION_ID))
        .thenReturn(List.of(relation));

    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(CANDIDATE_ID);
    candidate.setTaskId(TASK_ID);
    candidate.setTenantId(TENANT_ID);
    candidate.setIdCardNo("452201199001011234");
    candidate.setDeleted(0);
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));
    when(statusMapper.selectByTaskAndCandidate(TENANT_ID, TASK_ID, SESSION_ID, CANDIDATE_ID))
        .thenReturn(null);
    when(eligibilityService.checkCandidates(eq(TENANT_ID), eq(TASK_ID), any()))
        .thenReturn(Map.of(CANDIDATE_ID, new CollectionEligibilityResult(
            true, CANDIDATE_ID, "考生", "A001", "COMPLETED", List.of(), List.of(), null, null,
            Map.of("DOC002", 900L))));

    assertThat(service.initializeCollectionStatus(TASK_ID, SESSION_ID)).isEqualTo(1);

    ArgumentCaptor<PracticalCollectionStatusEntity> captor =
        ArgumentCaptor.forClass(PracticalCollectionStatusEntity.class);
    verify(statusMapper).insert(captor.capture());
    assertThat(captor.getValue().getExamSessionId()).isEqualTo(SESSION_ID);
    assertThat(captor.getValue().getCandidateId()).isEqualTo(CANDIDATE_ID);
    assertThat(captor.getValue().getIdCardLast4()).isEqualTo("1234");
  }

  @Test
  void rejectsSessionThatDoesNotBelongToTask() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(TASK_ID);
    when(taskMapper.selectOne(any())).thenReturn(task);
    when(examSessionMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.initializeCollectionStatus(TASK_ID, SESSION_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("考试场次不存在");
  }

  @Test
  void rejectsTaskLevelFallbackWhenTaskAlreadyHasSessions() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(TASK_ID);
    when(taskMapper.selectOne(any())).thenReturn(task);
    when(examSessionMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(() -> service.initializeCollectionStatus(TASK_ID, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(error -> assertThat(((BusinessException) error).getCode())
            .isEqualTo(com.guoyu.platform.common.exception.ErrorCode.BAD_REQUEST));
  }
}
