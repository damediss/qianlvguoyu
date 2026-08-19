package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseConfigEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseReportMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
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

class DefenseScoringApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = Mockito.mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationInfoAssessorMapper assessorMapper = Mockito.mock(CertificationInfoAssessorMapper.class);
  private final CertificationDefenseConfigMapper configMapper = Mockito.mock(CertificationDefenseConfigMapper.class);
  private final CertificationDefenseReportMapper reportMapper = Mockito.mock(CertificationDefenseReportMapper.class);
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);

  private DefenseScoringApplicationService service;
  private final AtomicLong idSeq = new AtomicLong(1000);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        100L, 1L, "assessor", "考评员", "platform",
        List.of("CERTIFICATION_ASSESSOR"), List.of("certification.defense.review"), "a@b.c");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new DefenseScoringApplicationService(
        taskMapper, candidateMapper, groupMapper, taskAssessorMapper, assessorMapper,
        configMapper, reportMapper, fileApplicationService);
    CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
    assessor.setId(9L);
    assessor.setUserId(100L);
    assessor.setAssessorName("考评员甲");
    assessor.setAuditStatus("approved");
    when(assessorMapper.selectOne(any())).thenReturn(assessor);
    doAnswer(invocation -> {
      Object entity = invocation.getArgument(0);
      if (entity instanceof CertificationDefenseConfigEntity config && config.getId() == null) {
        config.setId(idSeq.incrementAndGet());
      }
      if (entity instanceof CertificationDefenseReportEntity report && report.getId() == null) {
        report.setId(idSeq.incrementAndGet());
      }
      return 1;
    }).when(configMapper).insert(any(CertificationDefenseConfigEntity.class));
    doAnswer(invocation -> 1).when(configMapper).updateById(any(CertificationDefenseConfigEntity.class));
    doAnswer(invocation -> {
      CertificationDefenseReportEntity report = invocation.getArgument(0);
      if (report.getId() == null) {
        report.setId(idSeq.incrementAndGet());
      }
      return 1;
    }).when(reportMapper).insert(any(CertificationDefenseReportEntity.class));
    doAnswer(invocation -> 1).when(reportMapper).updateById(any(CertificationDefenseReportEntity.class));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void setupListsExamInProgressCandidatesAndSkipsInactive() {
    stubTaskAndGroup();
    CertificationCandidateEntity batman = candidate(11L, "蝙蝠侠", "452121199001011234");
    batman.setStatus("exam_in_progress");
    CertificationCandidateEntity inactive = candidate(12L, "停用考生", "452121199001011235");
    inactive.setStatus("inactive");
    when(candidateMapper.selectList(any())).thenReturn(List.of(batman, inactive));
    when(configMapper.selectOne(any())).thenReturn(null);
    when(reportMapper.selectList(any())).thenReturn(List.of());
    when(groupMapper.selectList(any())).thenReturn(List.of());

    DefenseScoringModels.SetupView setup = service.setup(5L);

    assertThat(setup.candidates()).extracting(DefenseScoringModels.CandidateRowView::candidateName)
        .containsExactly("蝙蝠侠");
  }

  @Test
  void matchCandidateByIdCard() {
    CertificationCandidateEntity zhang = candidate(11L, "张三", "110101199001011234");
    CertificationCandidateEntity li = candidate(12L, "李四", "110101199001011235");
    DefenseScoringApplicationService.MatchOutcome outcome =
        service.matchCandidate("张三_110101199001011234_业绩评估报告.pdf", List.of(zhang, li));
    assertThat(outcome.candidate().getId()).isEqualTo(11L);
    assertThat(outcome.matchKey()).isEqualTo("idCard");
  }

  @Test
  void matchCandidateByNameWhenUnique() {
    CertificationCandidateEntity zhang = candidate(11L, "张三", "110101199001011234");
    CertificationCandidateEntity li = candidate(12L, "李四", "110101199001011235");
    DefenseScoringApplicationService.MatchOutcome outcome =
        service.matchCandidate("张三-业绩评估报告.pdf", List.of(zhang, li));
    assertThat(outcome.candidate().getId()).isEqualTo(11L);
    assertThat(outcome.matchKey()).isEqualTo("name");
  }

  @Test
  void matchReportsBindsPdfToCandidate() {
    stubTaskAndGroup();
    CertificationCandidateEntity zhang = candidate(11L, "张三", "110101199001011234");
    when(candidateMapper.selectList(any())).thenReturn(List.of(zhang));
    when(configMapper.selectOne(any())).thenReturn(config(5L, 80L));
    when(reportMapper.selectOne(any())).thenReturn(null);
    FileEntity pdf = new FileEntity();
    pdf.setOriginalName("张三_110101199001011234.pdf");
    pdf.setContentType("application/pdf");
    when(fileApplicationService.getExistingFile(1L, 9001L)).thenReturn(pdf);

    DefenseScoringModels.MatchResultView result = service.matchReports(5L, new DefenseScoringModels.MatchReportsReq(
        List.of(new DefenseScoringModels.ReportFileReq(9001L, "张三_110101199001011234.pdf"))));

    assertThat(result.matched()).isEqualTo(1);
    assertThat(result.unmatched()).isEqualTo(0);
  }

  @Test
  void rejectNonPdf() {
    stubTaskAndGroup();
    CertificationCandidateEntity zhang = candidate(11L, "张三", "110101199001011234");
    when(candidateMapper.selectList(any())).thenReturn(List.of(zhang));
    when(configMapper.selectOne(any())).thenReturn(null);
    FileEntity doc = new FileEntity();
    doc.setOriginalName("张三.docx");
    doc.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    when(fileApplicationService.getExistingFile(1L, 9002L)).thenReturn(doc);

    DefenseScoringModels.MatchResultView result = service.matchReports(5L, new DefenseScoringModels.MatchReportsReq(
        List.of(new DefenseScoringModels.ReportFileReq(9002L, "张三.docx"))));
    assertThat(result.matched()).isEqualTo(0);
    assertThat(result.unmatchedFiles().get(0).reason()).contains("PDF");
  }

  @Test
  void myReportsOnlyReturnAssignedGroupFiles() {
    CertificationDefenseConfigEntity config = config(5L, 80L);
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(member(9L, "考评员甲")));
    when(configMapper.selectList(any())).thenReturn(List.of(config));
    CertificationDefenseReportEntity report = new CertificationDefenseReportEntity();
    report.setId(70L);
    report.setTaskId(5L);
    report.setCandidateId(11L);
    report.setGroupId(80L);
    report.setFileId(9001L);
    report.setFileName("report.pdf");
    when(reportMapper.selectList(any())).thenReturn(List.of(report));
    when(taskMapper.selectBatchIds(any())).thenReturn(List.of(certTask()));
    when(candidateMapper.selectBatchIds(any())).thenReturn(List.of(candidate(11L, "张三", "110101199001011234")));

    List<DefenseScoringModels.AssessorReportListItem> items = service.myReports();
    assertThat(items).hasSize(1);
    assertThat(items.get(0).candidateName()).isEqualTo("张三");
    assertThat(items.get(0).fileId()).isEqualTo(9001L);
  }

  private void stubTaskAndGroup() {
    when(taskMapper.selectById(5L)).thenReturn(certTask());
    CertificationTaskAssessorGroupEntity group = new CertificationTaskAssessorGroupEntity();
    group.setId(80L);
    group.setTaskId(5L);
    group.setGroupName("第一考评组");
    group.setLeaderAssessorId(9L);
    when(groupMapper.selectById(80L)).thenReturn(group);
    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(member(9L, "考评员甲"), member(10L, "考评员乙")));
  }

  private CertificationTaskEntity certTask() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(5L);
    task.setTaskName("高级技师认定");
    return task;
  }

  private CertificationDefenseConfigEntity config(Long taskId, Long groupId) {
    CertificationDefenseConfigEntity entity = new CertificationDefenseConfigEntity();
    entity.setId(1L);
    entity.setTaskId(taskId);
    entity.setGroupId(groupId);
    entity.setStatus("configured");
    return entity;
  }

  private CertificationCandidateEntity candidate(Long id, String name, String idCard) {
    CertificationCandidateEntity entity = new CertificationCandidateEntity();
    entity.setId(id);
    entity.setTaskId(5L);
    entity.setCandidateName(name);
    entity.setIdCardNo(idCard);
    entity.setMaskedIdCardNo(idCard.substring(0, 4) + "**********" + idCard.substring(14));
    entity.setStatus("active");
    return entity;
  }

  private CertificationTaskAssessorEntity member(Long assessorId, String name) {
    CertificationTaskAssessorEntity member = new CertificationTaskAssessorEntity();
    member.setId(assessorId + 100);
    member.setTaskId(5L);
    member.setGroupId(80L);
    member.setAssessorId(assessorId);
    member.setAssessorName(name);
    return member;
  }
}
