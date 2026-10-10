package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.mapper.CertificationDefenseScoreMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DefenseScoreSheetApplicationServiceTest {

  private final DefenseScoringApplicationService reports = Mockito.mock(DefenseScoringApplicationService.class);
  private final CertificationDefenseScoreMapper scoreMapper = Mockito.mock(CertificationDefenseScoreMapper.class);
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
  private DefenseScoreSheetApplicationService service;
  private final AtomicLong idSeq = new AtomicLong(2000);
  private final AtomicReference<CertificationDefenseScoreEntity> stored = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "考评员", "platform",
        List.of("CERTIFICATION_ASSESSOR"), List.of("certification.defense.review"), "a@b.c");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new DefenseScoreSheetApplicationService(reports, scoreMapper, fileApplicationService, new ObjectMapper());

    CertificationDefenseReportEntity report = new CertificationDefenseReportEntity();
    report.setId(70L);
    report.setTaskId(5L);
    report.setCandidateId(11L);
    report.setFileId(9001L);
    when(reports.requireVisibleReport(70L)).thenReturn(report);
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setUserId(100L);
    assessor.setAssessorName("考评员甲");
    when(reports.currentAssessor()).thenReturn(assessor);
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(5L);
    task.setTaskName("高级技师认定");
    when(reports.requireTask(5L)).thenReturn(task);
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(11L);
    candidate.setTaskId(5L);
    candidate.setCandidateName("张三");
    candidate.setAdmissionTicketNo("2608064501032300010");
    when(reports.requireCandidate(5L, 11L)).thenReturn(candidate);

    when(scoreMapper.selectOne(any())).thenAnswer(invocation -> stored.get());
    doAnswer(invocation -> {
      CertificationDefenseScoreEntity entity = invocation.getArgument(0);
      if (entity.getId() == null) {
        entity.setId(idSeq.incrementAndGet());
      }
      stored.set(copy(entity));
      return 1;
    }).when(scoreMapper).insert(any(CertificationDefenseScoreEntity.class));
    doAnswer(invocation -> {
      CertificationDefenseScoreEntity entity = invocation.getArgument(0);
      stored.set(copy(entity));
      return 1;
    }).when(scoreMapper).updateById(any(CertificationDefenseScoreEntity.class));

    FileEntity png = new FileEntity();
    png.setOriginalName("sign.png");
    png.setContentType("image/png");
    png.setSha256("a".repeat(64));
    when(fileApplicationService.getExistingFile(1L, 8001L)).thenReturn(png);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void saveDraftAllowsPartialThesisScores() {
    DefenseScoringModels.SaveScoreSheetReq req = new DefenseScoringModels.SaveScoreSheetReq(
        new DefenseScoringModels.ThesisPayload(
            "实践总结",
            List.of(false, false, false, false),
            "",
            java.util.Arrays.asList(12, null, null, null, null, null, null),
            List.of("问题1", "", ""),
            java.util.Arrays.asList(null, null, null, null)
        ),
        null,
        null,
        null
    );

    DefenseScoringModels.ScoreSheetView view = service.save(70L, "thesis", req);

    assertThat(view.status()).isEqualTo("draft");
    assertThat(view.contentSubtotal()).isEqualByComparingTo("12.00");
    assertThat(view.thesis().paperTitle()).isEqualTo("实践总结");
  }

  @Test
  void submitRequiresSignatureAndLocksSheet() {
    DefenseScoringModels.ThesisPayload payload = fullThesis();
    DefenseScoringModels.ScoreSheetView submitted = service.submit(70L, "THESIS", new DefenseScoringModels.SubmitScoreSheetReq(
        payload, null, 8001L, "a".repeat(64)));
    assertThat(submitted.status()).isEqualTo("submitted");
    assertThat(submitted.finalScore()).isEqualByComparingTo("87.20");

    assertThatThrownBy(() -> service.save(70L, "THESIS", new DefenseScoringModels.SaveScoreSheetReq(
        payload, null, 8001L, "a".repeat(64))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已提交");
  }

  private static DefenseScoringModels.ThesisPayload fullThesis() {
    return new DefenseScoringModels.ThesisPayload(
        "铝电解槽操作实践",
        List.of(false, false, false, false),
        "",
        List.of(15, 14, 13, 12, 11, 10, 8),
        List.of("简述工艺", "异常处理", "安全要点"),
        List.of(24, 23, 22, 21)
    );
  }

  private static CertificationDefenseScoreEntity copy(CertificationDefenseScoreEntity source) {
    CertificationDefenseScoreEntity copy = new CertificationDefenseScoreEntity();
    copy.setId(source.getId());
    copy.setTenantId(source.getTenantId());
    copy.setTaskId(source.getTaskId());
    copy.setCandidateId(source.getCandidateId());
    copy.setReportId(source.getReportId());
    copy.setAssessorId(source.getAssessorId());
    copy.setAssessorUserId(source.getAssessorUserId());
    copy.setSheetType(source.getSheetType());
    copy.setStatus(source.getStatus());
    copy.setPayloadJson(source.getPayloadJson());
    copy.setContentSubtotal(source.getContentSubtotal());
    copy.setDefenseSubtotal(source.getDefenseSubtotal());
    copy.setRawTotal(source.getRawTotal());
    copy.setFinalScore(source.getFinalScore());
    copy.setRejected(source.getRejected());
    copy.setSignatureFileId(source.getSignatureFileId());
    copy.setSignatureHash(source.getSignatureHash());
    copy.setSubmittedAt(source.getSubmittedAt());
    return copy;
  }
}
