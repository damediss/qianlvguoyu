package com.guoyu.platform.certification.defense;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
      DefenseSummaryExportService exportService
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
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前认定任务暂无考生，不能完成答辩");
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
    return exportService.export(current);
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
