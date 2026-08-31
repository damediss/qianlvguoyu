package com.guoyu.platform.certification.defense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseConfigEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
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
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.file.service.FileApplicationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DefenseManagementApplicationService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");
  private static final Comparator<CertificationDefenseScoreEntity> SIGNATURE_RECENCY =
      Comparator.comparing(
              CertificationDefenseScoreEntity::getSubmittedAt, Comparator.nullsFirst(LocalDateTime::compareTo))
          .thenComparing(CertificationDefenseScoreEntity::getId, Comparator.nullsFirst(Long::compareTo));

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationDefenseConfigMapper configMapper;
  private final CertificationDefenseReportMapper reportMapper;
  private final CertificationDefenseScoreMapper scoreMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationTaskExamSiteMapper examSiteMapper;
  private final DefenseSummaryExportService exportService;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;

  public DefenseManagementApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationDefenseConfigMapper configMapper,
      CertificationDefenseReportMapper reportMapper,
      CertificationDefenseScoreMapper scoreMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationTaskExamSiteMapper examSiteMapper,
      DefenseSummaryExportService exportService,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.groupMapper = groupMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.configMapper = configMapper;
    this.reportMapper = reportMapper;
    this.scoreMapper = scoreMapper;
    this.scopeMapper = scopeMapper;
    this.examSiteMapper = examSiteMapper;
    this.exportService = exportService;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public DefenseScoringModels.ProgressView progress(Long taskId) {
    return buildProgress(requireTask(taskId));
  }

  @Transactional
  public DefenseScoringModels.CompleteView complete(Long taskId) {
    CertificationTaskEntity task = requireTask(taskId);
    DefenseScoringModels.ProgressView current = buildProgress(task);
    if ("not_configured".equals(current.overallStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未配置综合答辩考评组");
    }
    if (current.totalCandidateCount() == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, DefenseCandidateEligibility.EMPTY_ROSTER_MESSAGE + "，不能完成答辩");
    }
    List<DefenseScoringModels.CandidateProgressView> unfinished = current.candidates().stream()
        .filter(row -> !"completed".equals(row.status()))
        .toList();
    if (!unfinished.isEmpty()) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "仍有 " + unfinished.size() + " 名考生的论文答辩或业绩评估未全部提交，不能完成答辩");
    }
    CertificationDefenseConfigEntity config = loadConfig(taskId);
    if (config == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未配置综合答辩考评组");
    }
    if (!DefenseScoreCatalog.STATUS_COMPLETED.equals(config.getStatus())) {
      config.setStatus(DefenseScoreCatalog.STATUS_COMPLETED);
      configMapper.updateById(config);
    }
    log.info("综合答辩已完成: taskId={}, candidates={}", taskId, current.totalCandidateCount());
    return new DefenseScoringModels.CompleteView(taskId, DefenseScoreCatalog.STATUS_COMPLETED, current.totalCandidateCount());
  }

  @Transactional(readOnly = true)
  public ExportResultDTO exportSummary(Long taskId) {
    CertificationTaskEntity task = requireTask(taskId);
    DefenseScoringModels.ProgressView current = buildProgress(task);
    if (!"completed".equals(current.overallStatus()) && !"ready_to_complete".equals(current.overallStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "全部考生完成论文答辩和业绩评估后才能导出成绩汇总表");
    }
    Map<String, CertificationDefenseScoreEntity> scores = loadScores(taskId);
    return exportService.export(current, expertSigns(current, scores), assessorSheets(current, scores));
  }

  private List<DefenseSummaryExportService.ExpertSignImage> expertSigns(
      DefenseScoringModels.ProgressView progress,
      Map<String, CertificationDefenseScoreEntity> scores
  ) {
    Map<Long, byte[]> cache = new LinkedHashMap<>();
    List<DefenseSummaryExportService.ExpertSignImage> images = new ArrayList<>();
    for (DefenseScoringModels.ExpertView expert : progress.experts()) {
      Long fileId = latestSignatureFileId(expert.assessorId(), DefenseScoreCatalog.SHEET_THESIS, scores);
      images.add(new DefenseSummaryExportService.ExpertSignImage(
          expert.assessorId(),
          expert.assessorName(),
          readSignatureBytes(fileId, cache)
      ));
    }
    return images;
  }

  private List<DefenseSummaryExportService.AssessorSheetExport> assessorSheets(
      DefenseScoringModels.ProgressView progress,
      Map<String, CertificationDefenseScoreEntity> scores
  ) {
    Map<Long, byte[]> cache = new LinkedHashMap<>();
    Map<Long, Long> thesisSigns = new LinkedHashMap<>();
    Map<Long, Long> performanceSigns = new LinkedHashMap<>();
    for (DefenseScoringModels.ExpertView expert : progress.experts()) {
      thesisSigns.put(
          expert.assessorId(),
          latestSignatureFileId(expert.assessorId(), DefenseScoreCatalog.SHEET_THESIS, scores));
      performanceSigns.put(
          expert.assessorId(),
          latestSignatureFileId(expert.assessorId(), DefenseScoreCatalog.SHEET_PERFORMANCE, scores));
    }
    List<DefenseSummaryExportService.AssessorSheetExport> sheets = new ArrayList<>();
    for (DefenseScoringModels.CandidateProgressView candidate : progress.candidates()) {
      for (DefenseScoringModels.ExpertView expert : progress.experts()) {
        CertificationDefenseScoreEntity thesis = scores.get(scoreKey(
            candidate.candidateId(), expert.assessorId(), DefenseScoreCatalog.SHEET_THESIS));
        CertificationDefenseScoreEntity performance = scores.get(scoreKey(
            candidate.candidateId(), expert.assessorId(), DefenseScoreCatalog.SHEET_PERFORMANCE));
        sheets.add(new DefenseSummaryExportService.AssessorSheetExport(
            candidate.candidateName(),
            firstNonBlank(candidate.admissionTicketNo(), candidate.candidateNo()),
            firstNonBlank(candidate.workTypeName(), progress.workTypeName()),
            firstNonBlank(candidate.levelName(), progress.levelName()),
            expert.assessorName(),
            toSheetView(DefenseScoreCatalog.SHEET_THESIS, thesis, expert.assessorName()),
            toSheetView(DefenseScoreCatalog.SHEET_PERFORMANCE, performance, expert.assessorName()),
            readSignatureBytes(thesisSigns.get(expert.assessorId()), cache),
            readSignatureBytes(performanceSigns.get(expert.assessorId()), cache)
        ));
      }
    }
    return sheets;
  }

  private Long latestSignatureFileId(
      Long assessorId,
      String sheetType,
      Map<String, CertificationDefenseScoreEntity> scores
  ) {
    return scores.values().stream()
        .filter(score -> score != null
            && Objects.equals(assessorId, score.getAssessorId())
            && sheetType.equals(score.getSheetType())
            && isSubmitted(score)
            && score.getSignatureFileId() != null)
        .max(SIGNATURE_RECENCY)
        .map(CertificationDefenseScoreEntity::getSignatureFileId)
        .orElse(null);
  }

  private byte[] readSignatureBytes(Long fileId, Map<Long, byte[]> cache) {
    if (fileId == null) {
      return null;
    }
    if (cache.containsKey(fileId)) {
      return cache.get(fileId);
    }
    try {
      byte[] bytes = fileApplicationService.readFileBytesById(fileId);
      cache.put(fileId, bytes);
      return bytes;
    } catch (RuntimeException ex) {
      log.warn("读取综合答辩签名图片失败: fileId={}", fileId, ex);
      cache.put(fileId, null);
      return null;
    }
  }

  private DefenseScoringModels.ScoreSheetView toSheetView(
      String sheetType,
      CertificationDefenseScoreEntity entity,
      String assessorName
  ) {
    if (entity == null) {
      boolean thesis = DefenseScoreCatalog.SHEET_THESIS.equals(sheetType);
      return new DefenseScoringModels.ScoreSheetView(
          sheetType,
          DefenseScoreCatalog.STATUS_EMPTY,
          thesis ? DefenseScoreCatalog.emptyThesis() : null,
          thesis ? null : DefenseScoreCatalog.emptyPerformance(),
          null,
          null,
          null,
          null,
          false,
          null,
          null,
          assessorName,
          null
      );
    }
    DefenseScoringModels.ThesisPayload thesis = null;
    DefenseScoringModels.PerformancePayload performance = null;
    if (DefenseScoreCatalog.SHEET_THESIS.equals(sheetType)) {
      thesis = readThesis(entity.getPayloadJson());
    } else {
      performance = readPerformance(entity.getPayloadJson());
    }
    return new DefenseScoringModels.ScoreSheetView(
        sheetType,
        statusOf(entity),
        thesis,
        performance,
        entity.getContentSubtotal(),
        entity.getDefenseSubtotal(),
        entity.getRawTotal(),
        entity.getFinalScore(),
        Integer.valueOf(1).equals(entity.getRejected()),
        entity.getSignatureFileId(),
        entity.getSignatureHash(),
        assessorName,
        entity.getSubmittedAt()
    );
  }

  private DefenseScoringModels.ThesisPayload readThesis(String json) {
    if (!StringUtils.hasText(json)) {
      return DefenseScoreCatalog.emptyThesis();
    }
    try {
      return DefenseScoreCatalog.normalizeThesis(
          objectMapper.readValue(json, DefenseScoringModels.ThesisPayload.class));
    } catch (JsonProcessingException ex) {
      log.warn("论文答辩评分表内容损坏，将导出空表");
      return DefenseScoreCatalog.emptyThesis();
    }
  }

  private DefenseScoringModels.PerformancePayload readPerformance(String json) {
    if (!StringUtils.hasText(json)) {
      return DefenseScoreCatalog.emptyPerformance();
    }
    try {
      return DefenseScoreCatalog.normalizePerformance(
          objectMapper.readValue(json, DefenseScoringModels.PerformancePayload.class));
    } catch (JsonProcessingException ex) {
      log.warn("业绩评估报告评分表内容损坏，将导出空表");
      return DefenseScoreCatalog.emptyPerformance();
    }
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private DefenseScoringModels.ProgressView buildProgress(CertificationTaskEntity task) {
    Long taskId = task.getId();
    CertificationDefenseConfigEntity config = loadConfig(taskId);
    List<CertificationTaskAssessorEntity> members = config == null
        ? List.of()
        : groupMembers(taskId, config.getGroupId());
    CertificationTaskAssessorGroupEntity group = config == null || config.getGroupId() == null
        ? null
        : groupMapper.selectById(config.getGroupId());
    List<DefenseScoringModels.ExpertView> experts = toExperts(members, group);
    List<CertificationCandidateEntity> candidates = activeCandidates(taskId);
    Map<Long, CertificationDefenseReportEntity> reports = loadReports(taskId);
    Map<String, CertificationDefenseScoreEntity> scores = loadScores(taskId);
    List<DefenseScoringModels.CandidateProgressView> rows = new ArrayList<>();
    int submitted = 0;
    for (CertificationCandidateEntity candidate : candidates) {
      DefenseScoringModels.CandidateProgressView row = toCandidateProgress(
          candidate, reports.get(candidate.getId()), members, group, scores);
      rows.add(row);
      if ("completed".equals(row.status())) {
        submitted += 1;
      }
    }
    String overallStatus = resolveOverallStatus(config, candidates.size(), submitted);
    return new DefenseScoringModels.ProgressView(
        taskId,
        task.getTaskNo(),
        task.getTaskName(),
        summarizeWorkTypes(taskId),
        summarizeLevelNames(taskId),
        formatDate(resolveExamDate(task)),
        resolveExamSite(task),
        overallStatus,
        config == null ? null : config.getStatus(),
        group == null ? null : group.getGroupName(),
        submitted,
        candidates.size(),
        experts,
        rows
    );
  }

  private DefenseScoringModels.CandidateProgressView toCandidateProgress(
      CertificationCandidateEntity candidate,
      CertificationDefenseReportEntity report,
      List<CertificationTaskAssessorEntity> members,
      CertificationTaskAssessorGroupEntity group,
      Map<String, CertificationDefenseScoreEntity> scores
  ) {
    List<DefenseScoringModels.AssessorScoreView> assessorViews = new ArrayList<>();
    List<BigDecimal> thesisScores = new ArrayList<>();
    List<BigDecimal> performanceScores = new ArrayList<>();
    List<BigDecimal> submittedThesis = new ArrayList<>();
    List<BigDecimal> submittedPerformance = new ArrayList<>();
    boolean rejected = false;
    int submittedSheets = 0;
    int requiredSheets = members.size() * 2;
    for (CertificationTaskAssessorEntity member : members) {
      boolean leader = group != null && Objects.equals(member.getAssessorId(), group.getLeaderAssessorId());
      CertificationDefenseScoreEntity thesis = scores.get(scoreKey(
          candidate.getId(), member.getAssessorId(), DefenseScoreCatalog.SHEET_THESIS));
      CertificationDefenseScoreEntity performance = scores.get(scoreKey(
          candidate.getId(), member.getAssessorId(), DefenseScoreCatalog.SHEET_PERFORMANCE));
      thesisScores.add(thesis == null ? null : thesis.getFinalScore());
      performanceScores.add(performance == null ? null : performance.getFinalScore());
      if (isSubmitted(thesis)) {
        submittedSheets += 1;
        submittedThesis.add(thesis.getFinalScore());
        rejected = rejected || Integer.valueOf(1).equals(thesis.getRejected());
      }
      if (isSubmitted(performance)) {
        submittedSheets += 1;
        submittedPerformance.add(performance.getFinalScore());
        rejected = rejected || Integer.valueOf(1).equals(performance.getRejected());
      }
      assessorViews.add(new DefenseScoringModels.AssessorScoreView(
          member.getAssessorId(),
          member.getAssessorName(),
          leader,
          statusOf(thesis),
          thesis == null ? null : thesis.getFinalScore(),
          thesis != null && Integer.valueOf(1).equals(thesis.getRejected()),
          statusOf(performance),
          performance == null ? null : performance.getFinalScore(),
          performance != null && Integer.valueOf(1).equals(performance.getRejected())
      ));
    }
    BigDecimal thesisAverage = DefenseScoreCatalog.average(submittedThesis);
    BigDecimal performanceAverage = DefenseScoreCatalog.average(submittedPerformance);
    BigDecimal finalScore = rejected ? null : DefenseScoreCatalog.comprehensive(thesisAverage, performanceAverage);
    String status;
    if (members.isEmpty() || report == null) {
      status = report == null ? "pending_report" : "pending";
    } else if (requiredSheets > 0 && submittedSheets >= requiredSheets) {
      status = "completed";
    } else if (submittedSheets > 0) {
      status = "scoring";
    } else {
      status = "pending";
    }
    return new DefenseScoringModels.CandidateProgressView(
        candidate.getId(),
        candidate.getCandidateNo(),
        candidate.getAdmissionTicketNo(),
        candidate.getCandidateName(),
        candidate.getMaskedIdCardNo(),
        candidate.getIdCardNo(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        report != null,
        status,
        thesisScores,
        thesisAverage,
        performanceScores,
        performanceAverage,
        finalScore,
        rejected,
        assessorViews
    );
  }

  private List<DefenseScoringModels.ExpertView> toExperts(
      List<CertificationTaskAssessorEntity> members,
      CertificationTaskAssessorGroupEntity group
  ) {
    List<DefenseScoringModels.ExpertView> experts = new ArrayList<>();
    for (CertificationTaskAssessorEntity member : members) {
      experts.add(new DefenseScoringModels.ExpertView(
          member.getAssessorId(),
          member.getAssessorName(),
          group != null && Objects.equals(member.getAssessorId(), group.getLeaderAssessorId())
      ));
    }
    return experts;
  }

  private static String resolveOverallStatus(
      CertificationDefenseConfigEntity config,
      int totalCandidates,
      int submittedCandidates
  ) {
    if (config == null) {
      return "not_configured";
    }
    if (DefenseScoreCatalog.STATUS_COMPLETED.equals(config.getStatus())) {
      return "completed";
    }
    if (totalCandidates > 0 && submittedCandidates == totalCandidates) {
      return "ready_to_complete";
    }
    return "scoring";
  }

  private CertificationTaskEntity requireTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationDefenseConfigEntity loadConfig(Long taskId) {
    return configMapper.selectOne(new LambdaQueryWrapper<CertificationDefenseConfigEntity>()
        .eq(CertificationDefenseConfigEntity::getTaskId, taskId)
        .last("LIMIT 1"));
  }

  private List<CertificationTaskAssessorEntity> groupMembers(Long taskId, Long groupId) {
    if (groupId == null) {
      return List.of();
    }
    return safeList(taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId)
            .orderByAsc(CertificationTaskAssessorEntity::getSortOrder)
            .orderByAsc(CertificationTaskAssessorEntity::getId)));
  }

  private List<CertificationCandidateEntity> activeCandidates(Long taskId) {
    return safeList(candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .orderByAsc(CertificationCandidateEntity::getCandidateNo)
            .orderByAsc(CertificationCandidateEntity::getId)))
        .stream()
        .filter(candidate -> candidate != null && !"inactive".equalsIgnoreCase(candidate.getStatus()))
        .filter(DefenseCandidateEligibility::isEligible)
        .toList();
  }

  private Map<Long, CertificationDefenseReportEntity> loadReports(Long taskId) {
    return safeList(reportMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseReportEntity>()
            .eq(CertificationDefenseReportEntity::getTaskId, taskId)))
        .stream()
        .filter(report -> report.getCandidateId() != null)
        .collect(Collectors.toMap(CertificationDefenseReportEntity::getCandidateId, report -> report, (a, b) -> a));
  }

  private Map<String, CertificationDefenseScoreEntity> loadScores(Long taskId) {
    Map<String, CertificationDefenseScoreEntity> map = new LinkedHashMap<>();
    for (CertificationDefenseScoreEntity score : safeList(scoreMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseScoreEntity>()
            .eq(CertificationDefenseScoreEntity::getTaskId, taskId)))) {
      if (score.getCandidateId() == null || score.getAssessorId() == null || score.getSheetType() == null) {
        continue;
      }
      map.put(scoreKey(score.getCandidateId(), score.getAssessorId(), score.getSheetType()), score);
    }
    return map;
  }

  private String summarizeWorkTypes(Long taskId) {
    return safeList(scopeMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
            .orderByAsc(CertificationTaskScopeEntity::getId)))
        .stream()
        .map(CertificationTaskScopeEntity::getWorkTypeName)
        .filter(StringUtils::hasText)
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private String summarizeLevelNames(Long taskId) {
    return safeList(scopeMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
            .orderByAsc(CertificationTaskScopeEntity::getId)))
        .stream()
        .map(CertificationTaskScopeEntity::getLevelName)
        .filter(StringUtils::hasText)
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private LocalDate resolveExamDate(CertificationTaskEntity task) {
    if (task.getExamDate() != null) {
      return task.getExamDate();
    }
    CertificationTaskExamSiteEntity site = firstExamSite(task.getId());
    return site == null ? null : site.getExamDate();
  }

  private String resolveExamSite(CertificationTaskEntity task) {
    CertificationTaskExamSiteEntity site = firstExamSite(task.getId());
    if (site != null && StringUtils.hasText(site.getSiteName())) {
      return site.getSiteName();
    }
    if (site != null && StringUtils.hasText(site.getAddress())) {
      return site.getAddress();
    }
    return StringUtils.hasText(task.getCompanyName()) ? task.getCompanyName() : "";
  }

  private CertificationTaskExamSiteEntity firstExamSite(Long taskId) {
    List<CertificationTaskExamSiteEntity> sites = safeList(examSiteMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
            .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskExamSiteEntity::getSortOrder)
            .orderByAsc(CertificationTaskExamSiteEntity::getId)
            .last("LIMIT 1")));
    return sites.isEmpty() ? null : sites.get(0);
  }

  private static boolean isSubmitted(CertificationDefenseScoreEntity score) {
    return score != null && DefenseScoreCatalog.STATUS_SUBMITTED.equals(score.getStatus());
  }

  private static String statusOf(CertificationDefenseScoreEntity score) {
    if (score == null || !StringUtils.hasText(score.getStatus())) {
      return DefenseScoreCatalog.STATUS_EMPTY;
    }
    return score.getStatus();
  }

  private static String scoreKey(Long candidateId, Long assessorId, String sheetType) {
    return candidateId + ":" + assessorId + ":" + sheetType;
  }

  private static String formatDate(LocalDate date) {
    return date == null ? "" : date.format(DATE_FORMAT);
  }

  private static <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
