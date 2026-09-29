package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseConfigEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseAbsenceMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseReportMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseScoreMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.file.service.FileApplicationService;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefenseManagementApplicationServiceTest {

  private final CertificationTaskMapper taskMapper = Mockito.mock(CertificationTaskMapper.class);
  private final CertificationCandidateMapper candidateMapper = Mockito.mock(CertificationCandidateMapper.class);
  private final CertificationTaskAssessorGroupMapper groupMapper = Mockito.mock(CertificationTaskAssessorGroupMapper.class);
  private final CertificationTaskAssessorMapper taskAssessorMapper = Mockito.mock(CertificationTaskAssessorMapper.class);
  private final CertificationDefenseAbsenceMapper absenceMapper = Mockito.mock(CertificationDefenseAbsenceMapper.class);
  private final CertificationDefenseConfigMapper configMapper = Mockito.mock(CertificationDefenseConfigMapper.class);
  private final CertificationDefenseReportMapper reportMapper = Mockito.mock(CertificationDefenseReportMapper.class);
  private final CertificationDefenseScoreMapper scoreMapper = Mockito.mock(CertificationDefenseScoreMapper.class);
  private final CertificationTaskScopeMapper scopeMapper = Mockito.mock(CertificationTaskScopeMapper.class);
  private final CertificationTaskExamSiteMapper examSiteMapper = Mockito.mock(CertificationTaskExamSiteMapper.class);
  private final DefenseSummaryExportService exportService = new DefenseSummaryExportService();
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private DefenseManagementApplicationService service;

  @BeforeEach
  void setUp() {
    service = new DefenseManagementApplicationService(
        taskMapper, candidateMapper, groupMapper, taskAssessorMapper, absenceMapper, configMapper,
        reportMapper, scoreMapper, scopeMapper, examSiteMapper, exportService,
        fileApplicationService, objectMapper);
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
  void progressOnlyListsTechnicianAndSeniorTechnician() {
    stubTask();
    CertificationCandidateEntity technician = new CertificationCandidateEntity();
    technician.setId(11L);
    technician.setTaskId(5L);
    technician.setCandidateName("技师考生");
    technician.setStatus("active");
    technician.setLevelCode("technician");
    technician.setLevelName("技师");
    CertificationCandidateEntity junior = new CertificationCandidateEntity();
    junior.setId(12L);
    junior.setTaskId(5L);
    junior.setCandidateName("初级考生");
    junior.setStatus("active");
    junior.setLevelCode("junior");
    junior.setLevelName("初级");
    CertificationCandidateEntity senior = new CertificationCandidateEntity();
    senior.setId(13L);
    senior.setTaskId(5L);
    senior.setCandidateName("高级技师考生");
    senior.setStatus("active");
    senior.setLevelCode("senior_technician");
    senior.setLevelName("高级技师");
    when(candidateMapper.selectList(any())).thenReturn(List.of(technician, junior, senior));
    when(scoreMapper.selectList(any())).thenReturn(List.of());

    DefenseScoringModels.ProgressView progress = service.progress(5L);

    assertThat(progress.candidates()).extracting(DefenseScoringModels.CandidateProgressView::candidateName)
        .containsExactly("技师考生", "高级技师考生");
    assertThat(progress.totalCandidateCount()).isEqualTo(2);
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

  @Test
  void exportSummaryEmbedsThesisSignaturesAndAssessorSheets() throws Exception {
    stubTask();
    byte[] thesisPng = png("thesis");
    byte[] performancePng = png("performance");
    when(fileApplicationService.readFileBytesById(101L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(102L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(103L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(201L)).thenReturn(performancePng);
    when(fileApplicationService.readFileBytesById(202L)).thenReturn(performancePng);
    when(fileApplicationService.readFileBytesById(203L)).thenReturn(performancePng);
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submittedSheet(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "90", 101L, thesisPayload()),
        submittedSheet(11L, 2L, DefenseScoreCatalog.SHEET_THESIS, "80", 102L, thesisPayload()),
        submittedSheet(11L, 3L, DefenseScoreCatalog.SHEET_THESIS, "70", 103L, thesisPayload()),
        submittedSheet(11L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "100", 201L, performancePayload()),
        submittedSheet(11L, 2L, DefenseScoreCatalog.SHEET_PERFORMANCE, "80", 202L, performancePayload()),
        submittedSheet(11L, 3L, DefenseScoreCatalog.SHEET_PERFORMANCE, "60", 203L, performancePayload())
    ));

    ExportResultDTO result = service.exportSummary(5L);

    assertThat(result.filename()).endsWith(".zip");
    Map<String, byte[]> files = unzip(result.data());
    assertThat(files).hasSize(7);
    assertThat(files.keySet()).filteredOn(name -> name.contains("汇总表")).hasSize(1);
    assertThat(files.keySet()).filteredOn(name -> name.contains("论文")).hasSize(3);
    assertThat(files.keySet()).filteredOn(name -> name.contains("业绩评估")).hasSize(3);

    byte[] summaryBytes = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("汇总表"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(summaryBytes))) {
      XWPFParagraph expertParagraph = document.getParagraphs().stream()
          .filter(paragraph -> paragraph.getText() != null && paragraph.getText().startsWith("评审专家"))
          .findFirst()
          .orElseThrow();
      List<byte[]> footerPictures = expertParagraph.getRuns().stream()
          .map(XWPFRun::getEmbeddedPictures)
          .flatMap(List::stream)
          .map(XWPFPicture::getPictureData)
          .map(data -> data.getData())
          .toList();
      assertThat(footerPictures).hasSize(3);
      assertThat(footerPictures).allMatch(bytes -> java.util.Arrays.equals(bytes, thesisPng));
    }

    String thesisText = documentText(new ByteArrayInputStream(files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("论文"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow()));
    assertThat(thesisText).contains("技术总结（论文）答辩评审评分表");
    assertThat(thesisText).contains("论文内容的意义和难度");
    assertThat(thesisText).contains("实践总结");

    String performanceText = documentText(new ByteArrayInputStream(files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("业绩评估"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow()));
    assertThat(performanceText).contains("技师、高级技师业绩评估报告评分表");
    assertThat(performanceText).contains("履行岗位职责能力");
  }

  @Test
  void exportSummaryUsesLatestElectronicSignatureNotFirstCandidateIdCard() throws Exception {
    stubTask();
    CertificationCandidateEntity first = new CertificationCandidateEntity();
    first.setId(11L);
    first.setTaskId(5L);
    first.setCandidateNo("C-1");
    first.setCandidateName("蝙蝠侠");
    first.setAdmissionTicketNo("912");
    first.setStatus("active");
    first.setLevelCode("technician");
    first.setLevelName("技师");
    CertificationCandidateEntity second = new CertificationCandidateEntity();
    second.setId(12L);
    second.setTaskId(5L);
    second.setCandidateNo("C-2");
    second.setCandidateName("唐家三少");
    second.setAdmissionTicketNo("917708356654121");
    second.setStatus("active");
    second.setLevelCode("senior_technician");
    second.setLevelName("高级技师");
    when(candidateMapper.selectList(any())).thenReturn(List.of(first, second));

    CertificationDefenseReportEntity firstReport = new CertificationDefenseReportEntity();
    firstReport.setId(70L);
    firstReport.setTaskId(5L);
    firstReport.setCandidateId(11L);
    firstReport.setFileId(9001L);
    CertificationDefenseReportEntity secondReport = new CertificationDefenseReportEntity();
    secondReport.setId(71L);
    secondReport.setTaskId(5L);
    secondReport.setCandidateId(12L);
    secondReport.setFileId(9002L);
    when(reportMapper.selectList(any())).thenReturn(List.of(firstReport, secondReport));

    byte[] idCardPng = png("idcard");
    byte[] thesisPng = png("thesis");
    byte[] performancePng = png("performance");
    when(fileApplicationService.readFileBytesById(801L)).thenReturn(idCardPng);
    when(fileApplicationService.readFileBytesById(901L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(902L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(903L)).thenReturn(thesisPng);
    when(fileApplicationService.readFileBytesById(911L)).thenReturn(performancePng);
    when(fileApplicationService.readFileBytesById(912L)).thenReturn(performancePng);
    when(fileApplicationService.readFileBytesById(913L)).thenReturn(performancePng);

    LocalDateTime early = LocalDateTime.of(2026, 8, 19, 17, 31, 3);
    LocalDateTime lateThesis = LocalDateTime.of(2026, 8, 20, 9, 5, 5);
    LocalDateTime latePerformance = LocalDateTime.of(2026, 8, 20, 9, 5, 58);
    when(scoreMapper.selectList(any())).thenReturn(List.of(
        submittedSheet(11L, 1L, DefenseScoreCatalog.SHEET_THESIS, "84.2", 801L, thesisPayload(), early, 1L),
        submittedSheet(11L, 2L, DefenseScoreCatalog.SHEET_THESIS, "87.2", 801L, thesisPayload(), early, 2L),
        submittedSheet(11L, 3L, DefenseScoreCatalog.SHEET_THESIS, "93.2", 801L, thesisPayload(), early, 3L),
        submittedSheet(11L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "75", 801L, performancePayload(), early, 4L),
        submittedSheet(11L, 2L, DefenseScoreCatalog.SHEET_PERFORMANCE, "73", 801L, performancePayload(), early, 5L),
        submittedSheet(11L, 3L, DefenseScoreCatalog.SHEET_PERFORMANCE, "86", 801L, performancePayload(), early, 6L),
        submittedSheet(12L, 1L, DefenseScoreCatalog.SHEET_THESIS, "87.2", 901L, thesisPayload(), lateThesis, 7L),
        submittedSheet(12L, 2L, DefenseScoreCatalog.SHEET_THESIS, "70", 902L, thesisPayload(), lateThesis, 8L),
        submittedSheet(12L, 3L, DefenseScoreCatalog.SHEET_THESIS, "89.8", 903L, thesisPayload(), lateThesis, 9L),
        submittedSheet(12L, 1L, DefenseScoreCatalog.SHEET_PERFORMANCE, "95", 911L, performancePayload(), latePerformance, 10L),
        submittedSheet(12L, 2L, DefenseScoreCatalog.SHEET_PERFORMANCE, "73", 912L, performancePayload(), latePerformance, 11L),
        submittedSheet(12L, 3L, DefenseScoreCatalog.SHEET_PERFORMANCE, "59", 913L, performancePayload(), latePerformance, 12L)
    ));

    ExportResultDTO result = service.exportSummary(5L);

    Map<String, byte[]> files = unzip(result.data());
    byte[] summaryBytes = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("汇总表"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(summaryBytes))) {
      XWPFParagraph expertParagraph = document.getParagraphs().stream()
          .filter(paragraph -> paragraph.getText() != null && paragraph.getText().startsWith("评审专家"))
          .findFirst()
          .orElseThrow();
      List<byte[]> footerPictures = expertParagraph.getRuns().stream()
          .map(XWPFRun::getEmbeddedPictures)
          .flatMap(List::stream)
          .map(XWPFPicture::getPictureData)
          .map(data -> data.getData())
          .toList();
      assertThat(footerPictures).hasSize(3);
      assertThat(footerPictures).allMatch(bytes -> java.util.Arrays.equals(bytes, thesisPng));
      assertThat(footerPictures).noneMatch(bytes -> java.util.Arrays.equals(bytes, idCardPng));
    }

    byte[] thesisSheet = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("论文"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(thesisSheet))) {
      List<byte[]> pictures = document.getAllPictures().stream()
          .map(data -> data.getData())
          .toList();
      assertThat(pictures).isNotEmpty();
      assertThat(pictures).noneMatch(bytes -> java.util.Arrays.equals(bytes, idCardPng));
    }
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
    candidate.setLevelCode("senior_technician");
    candidate.setLevelName("一级/高级技师");
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

  private CertificationDefenseScoreEntity submittedSheet(
      Long candidateId,
      Long assessorId,
      String sheetType,
      String score,
      Long signatureFileId,
      String payloadJson
  ) {
    return submittedSheet(candidateId, assessorId, sheetType, score, signatureFileId, payloadJson, null, null);
  }

  private CertificationDefenseScoreEntity submittedSheet(
      Long candidateId,
      Long assessorId,
      String sheetType,
      String score,
      Long signatureFileId,
      String payloadJson,
      LocalDateTime submittedAt,
      Long id
  ) {
    CertificationDefenseScoreEntity entity = submitted(candidateId, assessorId, sheetType, score);
    entity.setId(id);
    entity.setSignatureFileId(signatureFileId);
    entity.setPayloadJson(payloadJson);
    entity.setRejected(0);
    entity.setSubmittedAt(submittedAt);
    return entity;
  }

  private String thesisPayload() throws Exception {
    return objectMapper.writeValueAsString(new DefenseScoringModels.ThesisPayload(
        "实践总结",
        List.of(false, false, false, false),
        "",
        List.of(15, 15, 15, 15, 15, 15, 10),
        List.of("问题1", "问题2", "问题3"),
        List.of(25, 25, 25, 25)
    ));
  }

  private String performancePayload() throws Exception {
    return objectMapper.writeValueAsString(DefenseScoreCatalog.emptyPerformance());
  }

  private static byte[] png(String seed) {
    String encoded = switch (seed) {
      case "thesis" ->
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
      case "performance" ->
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
      default ->
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
    };
    return Base64.getDecoder().decode(encoded);
  }

  private static Map<String, byte[]> unzip(byte[] data) throws Exception {
    Map<String, byte[]> files = new LinkedHashMap<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        files.put(entry.getName(), zip.readAllBytes());
      }
    }
    return files;
  }

  private static String documentText(ByteArrayInputStream input) throws Exception {
    try (XWPFDocument document = new XWPFDocument(input)) {
      return documentText(document);
    }
  }

  private static String documentText(XWPFDocument document) {
    StringBuilder text = new StringBuilder();
    document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()));
    document.getTables().forEach(table -> table.getRows().forEach(row ->
        row.getTableCells().forEach(cell -> text.append(cell.getText()))));
    return text.toString();
  }
}
