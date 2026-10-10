package com.guoyu.platform.certification.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCandidateResultEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignSessionEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignatureEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCandidateResultMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCompleteSignSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCompleteSignatureMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PracticalCompleteSignatureApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationAssessorTaskMapper assessorTaskMapper = Mockito.mock(CertificationAssessorTaskMapper.class);
  private final CertificationPracticalCandidateResultMapper candidateResultMapper =
      Mockito.mock(CertificationPracticalCandidateResultMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationInfoAssessorMapper assessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final CertificationPracticalCompleteSignSessionMapper sessionMapper =
      Mockito.mock(CertificationPracticalCompleteSignSessionMapper.class);
  private final CertificationPracticalCompleteSignatureMapper signatureMapper =
      Mockito.mock(CertificationPracticalCompleteSignatureMapper.class);
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
  private final PracticalStartEventPublisher publisher = Mockito.mock(PracticalStartEventPublisher.class);
  private PracticalCompleteSignatureApplicationService service;
  private final AtomicLong ids = new AtomicLong(9000);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "考评员", "platform",
        List.of("assessor"), List.of("certification.practical.score"), "asd@qq.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new PracticalCompleteSignatureApplicationService(
        taskMapper, assessorTaskMapper, candidateResultMapper, taskAssessorMapper, assessorMapper,
        sessionMapper, signatureMapper, fileApplicationService, publisher);
    when(taskMapper.selectById(1L)).thenReturn(taskEntity());
    when(assessorMapper.selectOne(any())).thenReturn(assessor(9L));
    when(assessorMapper.selectById(9L)).thenReturn(assessor(9L));
    when(assessorMapper.selectById(10L)).thenReturn(assessor(10L));
    when(assessorMapper.selectById(11L)).thenReturn(assessor(11L));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void requestSignaturesCreatesPendingRowsForThreeAssessors() {
    stubSettledScoring();
    when(sessionMapper.lockByTask(1L, 1L)).thenReturn(null);
    Mockito.doAnswer(invocation -> {
      CertificationPracticalCompleteSignSessionEntity session = invocation.getArgument(0);
      session.setId(ids.incrementAndGet());
      return 1;
    }).when(sessionMapper).insert(any(CertificationPracticalCompleteSignSessionEntity.class));
    Mockito.doAnswer(invocation -> {
      CertificationPracticalCompleteSignatureEntity row = invocation.getArgument(0);
      row.setId(ids.incrementAndGet());
      return 1;
    }).when(signatureMapper).insert(any(CertificationPracticalCompleteSignatureEntity.class));
    when(signatureMapper.selectList(any())).thenReturn(List.of(
        pendingRow(9L, "考评员甲"), pendingRow(10L, "考评员乙"), pendingRow(11L, "考评员丙")));

    PracticalScoringModels.CompleteSignView view = service.requestSignatures(1L);

    assertThat(view.status()).isEqualTo("pending");
    assertThat(view.requiredCount()).isEqualTo(3);
    assertThat(view.signedCount()).isEqualTo(0);
    verify(signatureMapper, Mockito.times(3)).insert(any(CertificationPracticalCompleteSignatureEntity.class));
  }

  @Test
  void requestSignaturesRejectsWhenNotExactlyThreeAssessors() {
    stubSettledScoring();
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(binding(9L), binding(10L)));

    assertThatThrownBy(() -> service.requestSignatures(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("三名考评员签字确认");
  }

  @Test
  void submitMineMarksSessionSignedWhenThirdAssessorFinishes() {
    CertificationPracticalCompleteSignSessionEntity session = pendingSession();
    when(sessionMapper.lockByTask(1L, 1L)).thenReturn(session);
    when(signatureMapper.lockByAssessor(1L, 80L, 9L)).thenReturn(pendingRow(9L, "考评员甲"));
    FileEntity file = new FileEntity();
    file.setSha256("a".repeat(64));
    when(fileApplicationService.getExistingFile(1L, 55L)).thenReturn(file);
    when(signatureMapper.selectList(any())).thenReturn(List.of(
        signedRow(10L, "考评员乙"), signedRow(11L, "考评员丙"), signedRow(9L, "考评员甲")));

    PracticalScoringModels.CompleteSignView view = service.submitMine(1L, new PracticalScoringModels.SubmitCompleteSignReq(
        55L, "a".repeat(64), "{\"platform\":\"h5\"}"));

    assertThat(view.status()).isEqualTo("signed");
    assertThat(view.signedCount()).isEqualTo(3);
    assertThat(view.currentAssessorSigned()).isTrue();
    assertThat(session.getStatus()).isEqualTo("signed");
  }

  @Test
  void loadSignedImagesReadsSignedFilesAndSkipsPendingRows() {
    CertificationPracticalCompleteSignSessionEntity session = pendingSession();
    session.setStatus("consumed");
    when(sessionMapper.selectOne(any())).thenReturn(session);
    CertificationPracticalCompleteSignatureEntity signed = signedRow(9L, "考评员9");
    signed.setSignatureFileId(501L);
    CertificationPracticalCompleteSignatureEntity pending = pendingRow(10L, "考评员10");
    when(signatureMapper.selectList(any())).thenReturn(List.of(signed, pending));
    when(fileApplicationService.readFileBytesById(501L)).thenReturn(new byte[] {1, 2, 3});

    List<PracticalCompleteSignatureApplicationService.AssessorSignImage> images = service.loadSignedImages(1L);

    assertThat(images).hasSize(1);
    assertThat(images.get(0).assessorId()).isEqualTo(9L);
    assertThat(images.get(0).imageBytes()).containsExactly(1, 2, 3);
  }

  @Test
  void requireAllSignedRejectsPendingSession() {
    CertificationPracticalCompleteSignSessionEntity session = pendingSession();
    session.setSignedCount(1);
    when(sessionMapper.selectOne(any())).thenReturn(session);

    assertThatThrownBy(() -> service.requireAllSigned(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("未签字确认");
  }

  private void stubSettledScoring() {
    CertificationAssessorTaskEntity scoringTask = new CertificationAssessorTaskEntity();
    scoringTask.setId(50L);
    scoringTask.setTaskId(1L);
    scoringTask.setCandidateId(11L);
    scoringTask.setPaperQuestionId(501L);
    scoringTask.setGroupId(30L);
    when(assessorTaskMapper.selectList(any())).thenReturn(List.of(scoringTask));
    CertificationPracticalCandidateResultEntity result = new CertificationPracticalCandidateResultEntity();
    result.setCandidateId(11L);
    result.setStatus("completed");
    when(candidateResultMapper.selectList(any())).thenReturn(List.of(result));
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(binding(9L), binding(10L), binding(11L)));
  }

  private CertificationTaskAssessorEntity binding(Long assessorId) {
    CertificationTaskAssessorEntity member = new CertificationTaskAssessorEntity();
    member.setTaskId(1L);
    member.setGroupId(30L);
    member.setAssessorId(assessorId);
    return member;
  }

  private CertificationPracticalCompleteSignSessionEntity pendingSession() {
    CertificationPracticalCompleteSignSessionEntity session = new CertificationPracticalCompleteSignSessionEntity();
    session.setId(80L);
    session.setTaskId(1L);
    session.setStatus("pending");
    session.setRequiredCount(3);
    session.setSignedCount(2);
    return session;
  }

  private CertificationPracticalCompleteSignatureEntity pendingRow(Long assessorId, String name) {
    CertificationPracticalCompleteSignatureEntity row = new CertificationPracticalCompleteSignatureEntity();
    row.setId(assessorId);
    row.setSessionId(80L);
    row.setTaskId(1L);
    row.setGroupId(30L);
    row.setAssessorId(assessorId);
    row.setAssessorName(name);
    row.setStatus("pending");
    return row;
  }

  private CertificationPracticalCompleteSignatureEntity signedRow(Long assessorId, String name) {
    CertificationPracticalCompleteSignatureEntity row = pendingRow(assessorId, name);
    row.setStatus("signed");
    return row;
  }

  private CertificationInfoAssessorEntity assessor(Long id) {
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(id);
    assessor.setUserId(id == 9L ? 100L : 100L + id);
    assessor.setAssessorName("考评员" + id);
    assessor.setAuditStatus("approved");
    return assessor;
  }

  private CertificationTaskEntity taskEntity() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(1L);
    task.setTaskName("铝业实操认定");
    task.setStatus("in_progress");
    return task;
  }
}
