package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseConfigEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseReportMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseScoreMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefenseManagementApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = Mockito.mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationDefenseConfigMapper configMapper = Mockito.mock(CertificationDefenseConfigMapper.class);
  private final CertificationDefenseReportMapper reportMapper = Mockito.mock(CertificationDefenseReportMapper.class);
  private final CertificationDefenseScoreMapper scoreMapper = Mockito.mock(CertificationDefenseScoreMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationTaskExamSiteMapper examSiteMapper = Mockito.mock(CertificationTaskExamSiteMapper.class);
  private final DefenseSummaryExportService exportService = new DefenseSummaryExportService();

  private DefenseManagementApplicationService service;

  @BeforeEach
  void setUp() {
    service = new DefenseManagementApplicationService(
        taskMapper, candidateMapper, groupMapper, taskAssessorMapper, configMapper,
        reportMapper, scoreMapper, scopeMapper, examSiteMapper, exportService);
  }

  @Test
  void progressAggregatesThesisAndPerformanceAverages() {
    stubTask();
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "90"),
        submitted(11L, 2L, DefenseScoreCatalog.SHEET_THESIS, "80"),
        submitted(11L, 3L, DefenseScoreCatalog.SHEET_THESIS, "70"),
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "100"),
        submitted(11L, 2L, DefenseScoreCatalog.SHEET_PERFORMANCE, "80"),
        submitted(11L, 3L, DefenseScoreCatalog.SHEET_PERFORMANCE, "60")
    ));

    DefenseScoringModels.ProgressView progress = service.progress(5L);

    assertThat(progress.overallStatus()).isEqualTo("ready_to_complete");
    assertThat(progress.submittedCandidateCount()).isEqualTo(1);
    DefenseScoringModels.CandidateProgressView row = progress.candidates().get(0);
    assertThat(row.status()).isEqualTo("completed");
    assertThat(row.thesisAverage()).isEqualByComparingTo("80.00");
    assertThat(row.performanceAverage()).isEqualByComparingTo("80.00");
    assertThat(row.finalScore()).isEqualByComparingTo("80.00");
    assertThat(row.thesisScores()).containsExactly(
        new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("70"));
    assertThat(row.performanceScores()).containsExactly(
        new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("60"));
  }

  @Test
  void progressShowsDraftScoresButDoesNotCountThemAsSubmitted() {
    stubTask();
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "90"),
        draft(11L, 2L, DefenseScoreCatalog.SHEET_THESIS, "88"),
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "100")
    ));

    DefenseScoringModels.ProgressView progress = service.progress(5L);

    assertThat(progress.overallStatus()).isEqualTo("scoring");
    DefenseScoringModels.CandidateProgressView row = progress.candidates().get(0);
    assertThat(row.status()).isEqualTo("scoring");
    assertThat(row.thesisScores().get(0)).isEqualByComparingTo("90");
    assertThat(row.thesisScores().get(1)).isEqualByComparingTo("88");
    assertThat(row.thesisAverage()).isEqualByComparingTo("90.00");
    assertThat(row.performanceScores().get(0)).isEqualByComparingTo("100");
    assertThat(row.finalScore()).isEqualByComparingTo("94.00");
  }

  @Test
  void completeRequiresAllCandidatesSubmitted() {
    stubTask();
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "90")
    ));

    assertThatThrownBy(() -> service.complete(5L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("未全部提交");
  }

  @Test
  void completeMarksConfigCompleted() {
    stubTask();
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "90"),
        submitted(11L, 2L, DefenseScoreCatalog.SHEET_THESIS, "80"),
        submitted(11L, 3L, DefenseScoreCatalog.SHEET_THESIS, "70"),
        submitted(11L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "100"),
        submitted(11L, 2L, DefenseScoreCatalog.SHEET_PERFORMANCE, "80"),
        submitted(11L, 3L, DefenseScoreCatalog.SHEET_PERFORMANCE, "60")
    ));
    when(configMapper.updateById(any(CertificationDefenseConfigEntity.class))).thenReturn(1);

    DefenseScoringModels.CompleteView result = service.complete(5L);

    assertThat(result.status()).isEqualTo("completed");
    assertThat(result.candidateCount()).isEqualTo(1);
  }

  private void stubTask() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(5L);
    task.setTaskNo("20260804BSYHL");
    task.setTaskName("铝电解槽操作工认定");
    task.setExamDate(LocalDate.of(2026, 8, 13));
    task.setCompanyName("广西百色广投银海铝业有限责任公司");
    when(taskMapper.selectById(5L)).thenReturn(task);

    CertificationDefenseConfigEntity config = new CertificationDefenseConfigEntity();
    config.setId(8L);
    config.setTaskId(5L);
    config.setGroupId(80L);
    config.setStatus("configured");
    when(configMapper.selectOne(any())).thenReturn(config);

    CertificationTaskAssessorGroupEntity group = new CertificationTaskAssessorGroupEntity();
    group.setId(80L);
    group.setTaskId(5L);
    group.setGroupName("答辩组");
    group.setLeaderAssessorId(1L);
    when(groupMapper.selectById(80L)).thenReturn(group);

    when(taskAssessorMapper.selectList(any())).thenReturn(List.of(
        member(1L, "专家甲", 1),
        member(2L, "专家乙", 2),
        member(3L, "专家丙", 3)
    ));

    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setId(11L);
    candidate.setTaskId(5L);
    candidate.setCandidateName("张三");
    candidate.setAdmissionTicketNo("2608064501032300010");
    candidate.setIdCardNo("452630198809204378");
    candidate.setMaskedIdCardNo("452630********4378");
    candidate.setStatus("active");
    when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));

    CertificationDefenseReportEntity report = new CertificationDefenseReportEntity();
    report.setId(70L);
    report.setTaskId(5L);
    report.setCandidateId(11L);
    report.setFileId(9001L);
    when(reportMapper.selectList(any())).thenReturn(List.of(report));

    CertificationTaskScopeEntity scope = new CertificationTaskScopeEntity();
    scope.setWorkTypeName("铝电解工");
    scope.setLevelName("一级/高级技师");
    when(scopeMapper.selectList(any())).thenReturn(List.of(scope));
    when(examSiteMapper.selectList(any())).thenReturn(List.of());
  }

  private static CertificationTaskAssessorEntity member(Long assessorId, String name, int sort) {
    CertificationTaskAssessorEntity member = new CertificationTaskAssessorEntity();
    member.setAssessorId(assessorId);
    member.setAssessorName(name);
    member.setSortOrder(sort);
    return member;
  }

  private static CertificationDefenseScoreEntity submitted(
      Long candidateId, Long assessorId, String sheetType, String score
  ) {
    return score(candidateId, assessorId, sheetType, score, DefenseScoreCatalog.STATUS_SUBMITTED);
  }

  private static CertificationDefenseScoreEntity draft(
      Long candidateId, Long assessorId, String sheetType, String score
  ) {
    return score(candidateId, assessorId, sheetType, score, DefenseScoreCatalog.STATUS_DRAFT);
  }

  private static CertificationDefenseScoreEntity score(
      Long candidateId, Long assessorId, String sheetType, String score, String status
  ) {
    CertificationDefenseScoreEntity entity = new CertificationDefenseScoreEntity();
    entity.setCandidateId(candidateId);
    entity.setAssessorId(assessorId);
    entity.setSheetType(sheetType);
    entity.setStatus(status);
    entity.setFinalScore(new BigDecimal(score));
    entity.setRejected(0);
    return entity;
  }
}
