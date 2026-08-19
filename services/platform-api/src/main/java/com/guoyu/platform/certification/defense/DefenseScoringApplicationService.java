package com.guoyu.platform.certification.defense;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseConfigEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseReportMapper;
import com.guoyu.platform.certification.mapper.CertificationDefenseScoreMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DefenseScoringApplicationService {

  private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{17}[0-9Xx]|\\d{15})");

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationDefenseConfigMapper configMapper;
  private final CertificationDefenseReportMapper reportMapper;
  private final CertificationDefenseScoreMapper scoreMapper;
  private final FileApplicationService fileApplicationService;

  public DefenseScoringApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationDefenseConfigMapper configMapper,
      CertificationDefenseReportMapper reportMapper,
      CertificationDefenseScoreMapper scoreMapper,
      FileApplicationService fileApplicationService
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.groupMapper = groupMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.assessorMapper = assessorMapper;
    this.configMapper = configMapper;
    this.reportMapper = reportMapper;
    this.scoreMapper = scoreMapper;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional(readOnly = true)
  public DefenseScoringModels.SetupView setup(Long taskId) {
    requireTask(taskId);
    return new DefenseScoringModels.SetupView(
        taskId,
        configView(loadConfig(taskId)),
        groupOptions(taskId),
        candidateRows(taskId)
    );
  }

  @Transactional
  public DefenseScoringModels.ConfigView saveConfig(Long taskId, DefenseScoringModels.SaveConfigReq req) {
    requireTask(taskId);
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, req.groupId());
    List<CertificationTaskAssessorEntity> members = groupMembers(taskId, group.getId());
    if (members.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组尚未配置成员，不能用于综合答辩");
    }
    CertificationDefenseConfigEntity entity = loadConfig(taskId);
    if (entity == null) {
      entity = new CertificationDefenseConfigEntity();
      entity.setTaskId(taskId);
      entity.setTenantId(SecurityContext.currentUser().getTenantId());
      entity.setDeleted(0);
    }
    entity.setGroupId(req.groupId());
    entity.setStatus("configured");
    if (entity.getId() == null) {
      configMapper.insert(entity);
    } else {
      configMapper.updateById(entity);
    }
    List<CertificationDefenseReportEntity> reports = safeList(reportMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseReportEntity>()
            .eq(CertificationDefenseReportEntity::getTaskId, taskId)));
    for (CertificationDefenseReportEntity report : reports) {
      report.setGroupId(req.groupId());
      reportMapper.updateById(report);
    }
    log.info("综合答辩已绑定考评组: taskId={}, groupId={}, members={}", taskId, req.groupId(), members.size());
    return configView(entity);
  }

  @Transactional
  public DefenseScoringModels.MatchResultView matchReports(Long taskId, DefenseScoringModels.MatchReportsReq req) {
    requireTask(taskId);
    List<CertificationCandidateEntity> candidates = activeCandidates(taskId);
    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前认定任务暂无考生，无法匹配业绩评估报告");
    }
    List<DefenseScoringModels.UnmatchedFileView> unmatched = new ArrayList<>();
    int matched = 0;
    int replaced = 0;
    for (DefenseScoringModels.ReportFileReq fileReq : req.files()) {
      if (fileReq == null || fileReq.fileId() == null) {
        unmatched.add(new DefenseScoringModels.UnmatchedFileView(null, null, "文件无效"));
        continue;
      }
      String fileName = resolveFileName(fileReq);
      MatchOutcome outcome = matchCandidate(fileName, candidates);
      if (outcome.candidate() == null) {
        unmatched.add(new DefenseScoringModels.UnmatchedFileView(fileReq.fileId(), fileName, outcome.reason()));
        continue;
      }
      try {
        boolean wasReplaced = bindReportInternal(
            taskId, outcome.candidate(), fileReq.fileId(), fileName, outcome.matchKey());
        if (wasReplaced) {
          replaced += 1;
        } else {
          matched += 1;
        }
      } catch (BusinessException ex) {
        unmatched.add(new DefenseScoringModels.UnmatchedFileView(fileReq.fileId(), fileName, ex.getMessage()));
      }
    }
    return new DefenseScoringModels.MatchResultView(matched, unmatched.size(), replaced, unmatched);
  }

  @Transactional
  public DefenseScoringModels.CandidateRowView bindCandidateReport(
      Long taskId,
      Long candidateId,
      DefenseScoringModels.ReportFileReq req
  ) {
    requireTask(taskId);
    CertificationCandidateEntity candidate = requireCandidate(taskId, candidateId);
    bindReportInternal(taskId, candidate, req.fileId(), resolveFileName(req), candidate.getCandidateName());
    return candidateRows(taskId).stream()
        .filter(row -> candidateId.equals(row.candidateId()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "考生不存在"));
  }

  @Transactional
  public void deleteCandidateReport(Long taskId, Long candidateId) {
    requireTask(taskId);
    requireCandidate(taskId, candidateId);
    CertificationDefenseReportEntity report = loadReport(taskId, candidateId);
    if (report != null) {
      reportMapper.deleteById(report.getId());
    }
  }

  @Transactional(readOnly = true)
  public List<DefenseScoringModels.AssessorReportListItem> myReports() {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    List<CertificationTaskAssessorEntity> memberships = safeList(taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getAssessorId, assessor.getId())));
    Set<Long> groupIds = memberships.stream()
        .map(CertificationTaskAssessorEntity::getGroupId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (groupIds.isEmpty()) {
      return List.of();
    }
    List<CertificationDefenseConfigEntity> configs = safeList(configMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseConfigEntity>()
            .in(CertificationDefenseConfigEntity::getGroupId, groupIds)));
    if (configs.isEmpty()) {
      return List.of();
    }
    Set<Long> taskIds = configs.stream().map(CertificationDefenseConfigEntity::getTaskId).collect(Collectors.toSet());
    Map<Long, Long> groupByTask = configs.stream()
        .filter(config -> config.getTaskId() != null)
        .collect(Collectors.toMap(CertificationDefenseConfigEntity::getTaskId, CertificationDefenseConfigEntity::getGroupId, (a, b) -> a));
    List<CertificationDefenseReportEntity> reports = safeList(reportMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseReportEntity>()
            .in(CertificationDefenseReportEntity::getTaskId, taskIds)
            .orderByDesc(CertificationDefenseReportEntity::getUploadedAt)
            .orderByDesc(CertificationDefenseReportEntity::getId)));
    Map<Long, CertificationTaskEntity> taskMap = loadTaskMap(taskIds);
    Map<Long, CertificationCandidateEntity> candidateMap = loadCandidateMap(reports.stream()
        .map(CertificationDefenseReportEntity::getCandidateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet()));
    Map<String, CertificationDefenseScoreEntity> scoreMap = loadScoreMap(assessor.getId(), reports);
    List<DefenseScoringModels.AssessorReportListItem> items = new ArrayList<>();
    for (CertificationDefenseReportEntity report : reports) {
      Long expectedGroupId = groupByTask.get(report.getTaskId());
      if (expectedGroupId != null && report.getGroupId() != null && !expectedGroupId.equals(report.getGroupId())) {
        continue;
      }
      if (expectedGroupId != null && !groupIds.contains(expectedGroupId)) {
        continue;
      }
      CertificationTaskEntity certTask = taskMap.get(report.getTaskId());
      CertificationCandidateEntity candidate = candidateMap.get(report.getCandidateId());
      items.add(toListItem(
          report,
          certTask,
          candidate,
          scoreMap.get(scoreKey(report, DefenseScoreCatalog.SHEET_THESIS)),
          scoreMap.get(scoreKey(report, DefenseScoreCatalog.SHEET_PERFORMANCE))
      ));
    }
    return items;
  }

  @Transactional(readOnly = true)
  public DefenseScoringModels.AssessorReportDetail myReport(Long reportId) {
    CertificationDefenseReportEntity report = requireVisibleReport(reportId);
    CertificationTaskEntity certTask = requireTask(report.getTaskId());
    CertificationCandidateEntity candidate = requireCandidate(report.getTaskId(), report.getCandidateId());
    CertificationInfoAssessorEntity assessor = currentAssessor();
    CertificationDefenseScoreEntity thesis = loadScore(assessor.getId(), report, DefenseScoreCatalog.SHEET_THESIS);
    CertificationDefenseScoreEntity performance = loadScore(assessor.getId(), report, DefenseScoreCatalog.SHEET_PERFORMANCE);
    return toDetail(report, certTask, candidate, thesis, performance);
  }

  DefenseScoringModels.AssessorReportDetail toDetail(
      CertificationDefenseReportEntity report,
      CertificationTaskEntity certTask,
      CertificationCandidateEntity candidate,
      CertificationDefenseScoreEntity thesisScore,
      CertificationDefenseScoreEntity performanceScore
  ) {
    return new DefenseScoringModels.AssessorReportDetail(
        report.getId(),
        report.getTaskId(),
        certTask == null ? null : certTask.getTaskName(),
        candidate == null ? null : candidate.getId(),
        candidate == null ? null : candidate.getCandidateName(),
        candidate == null ? null : candidate.getMaskedIdCardNo(),
        candidate == null ? null : candidate.getAdmissionTicketNo(),
        candidate == null ? null : candidate.getWorkTypeName(),
        candidate == null ? null : candidate.getLevelName(),
        candidate == null ? null : candidate.getOrgName(),
        report.getFileId(),
        report.getFileName(),
        statusOf(thesisScore),
        thesisScore == null ? null : thesisScore.getFinalScore(),
        thesisScore != null && Integer.valueOf(1).equals(thesisScore.getRejected()),
        statusOf(performanceScore),
        performanceScore == null ? null : performanceScore.getFinalScore(),
        performanceScore != null && Integer.valueOf(1).equals(performanceScore.getRejected())
    );
  }

  MatchOutcome matchCandidate(String fileName, List<CertificationCandidateEntity> candidates) {
    if (!StringUtils.hasText(fileName)) {
      return MatchOutcome.unmatched("文件名称为空");
    }
    String stem = stripExtension(fileName);
    Matcher matcher = ID_CARD_PATTERN.matcher(stem.replace('ｘ', 'x').replace('Ｘ', 'X'));
    if (matcher.find()) {
      String idCard = matcher.group(1).toUpperCase(Locale.ROOT);
      List<CertificationCandidateEntity> hits = candidates.stream()
          .filter(candidate -> idCard.equalsIgnoreCase(normalize(candidate.getIdCardNo())))
          .toList();
      if (hits.size() == 1) {
        return MatchOutcome.matched(hits.get(0), "idCard");
      }
      if (hits.size() > 1) {
        return MatchOutcome.unmatched("身份证号匹配到多名考生");
      }
    }
    List<CertificationCandidateEntity> ticketHits = candidates.stream()
        .filter(candidate -> containsToken(stem, candidate.getAdmissionTicketNo())
            || containsToken(stem, candidate.getCandidateNo()))
        .toList();
    if (ticketHits.size() == 1) {
      return MatchOutcome.matched(ticketHits.get(0), "ticket");
    }
    if (ticketHits.size() > 1) {
      return MatchOutcome.unmatched("准考证号匹配到多名考生");
    }
    List<CertificationCandidateEntity> nameHits = candidates.stream()
        .filter(candidate -> StringUtils.hasText(candidate.getCandidateName())
            && stem.contains(candidate.getCandidateName().trim()))
        .toList();
    if (nameHits.size() == 1) {
      return MatchOutcome.matched(nameHits.get(0), "name");
    }
    if (nameHits.size() > 1) {
      return MatchOutcome.unmatched("姓名匹配到多名考生，请改用身份证号命名");
    }
    return MatchOutcome.unmatched("未能按身份证号、准考证号或姓名匹配到考生");
  }

  private boolean bindReportInternal(
      Long taskId,
      CertificationCandidateEntity candidate,
      Long fileId,
      String fileName,
      String matchKey
  ) {
    requirePdf(fileId, fileName);
    CertificationDefenseReportEntity existing = loadReport(taskId, candidate.getId());
    boolean replaced = existing != null;
    CertificationDefenseConfigEntity config = loadConfig(taskId);
    CertificationDefenseReportEntity report = existing == null ? new CertificationDefenseReportEntity() : existing;
    report.setTenantId(SecurityContext.currentUser().getTenantId());
    report.setTaskId(taskId);
    report.setCandidateId(candidate.getId());
    report.setGroupId(config == null ? null : config.getGroupId());
    report.setFileId(fileId);
    report.setFileName(fileName);
    report.setMatchKey(matchKey);
    report.setStatus("uploaded");
    report.setUploadedAt(LocalDateTime.now());
    report.setDeleted(0);
    if (report.getId() == null) {
      reportMapper.insert(report);
    } else {
      reportMapper.updateById(report);
    }
    return replaced;
  }

  private List<DefenseScoringModels.CandidateRowView> candidateRows(Long taskId) {
    List<CertificationCandidateEntity> candidates = activeCandidates(taskId);
    Map<Long, CertificationDefenseReportEntity> reports = safeList(reportMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseReportEntity>()
            .eq(CertificationDefenseReportEntity::getTaskId, taskId)))
        .stream()
        .filter(report -> report.getCandidateId() != null)
        .collect(Collectors.toMap(CertificationDefenseReportEntity::getCandidateId, report -> report, (a, b) -> a));
    List<DefenseScoringModels.CandidateRowView> rows = new ArrayList<>();
    for (CertificationCandidateEntity candidate : candidates) {
      CertificationDefenseReportEntity report = reports.get(candidate.getId());
      rows.add(new DefenseScoringModels.CandidateRowView(
          candidate.getId(),
          candidate.getCandidateNo(),
          candidate.getCandidateName(),
          candidate.getMaskedIdCardNo(),
          candidate.getIdCardNo(),
          candidate.getWorkTypeName(),
          candidate.getLevelName(),
          candidate.getOrgName(),
          report == null ? null : report.getId(),
          report == null ? null : report.getFileId(),
          report == null ? null : report.getFileName(),
          report == null ? null : report.getUploadedAt()
      ));
    }
    return rows;
  }

  private List<DefenseScoringModels.GroupOption> groupOptions(Long taskId) {
    List<CertificationTaskAssessorGroupEntity> groups = safeList(groupMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
            .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskAssessorGroupEntity::getId)));
    List<DefenseScoringModels.GroupOption> options = new ArrayList<>();
    for (CertificationTaskAssessorGroupEntity group : groups) {
      List<CertificationTaskAssessorEntity> members = groupMembers(taskId, group.getId());
      String leaderName = members.stream()
          .filter(member -> Objects.equals(member.getAssessorId(), group.getLeaderAssessorId()))
          .map(CertificationTaskAssessorEntity::getAssessorName)
          .filter(StringUtils::hasText)
          .findFirst()
          .orElse(null);
      options.add(new DefenseScoringModels.GroupOption(
          group.getId(),
          group.getGroupName(),
          group.getLeaderAssessorId(),
          leaderName,
          members.size()
      ));
    }
    return options;
  }

  private DefenseScoringModels.ConfigView configView(CertificationDefenseConfigEntity entity) {
    if (entity == null) {
      return null;
    }
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(entity.getGroupId());
    List<CertificationTaskAssessorEntity> members = group == null
        ? List.of()
        : groupMembers(entity.getTaskId(), entity.getGroupId());
    return new DefenseScoringModels.ConfigView(
        entity.getId(),
        entity.getTaskId(),
        entity.getGroupId(),
        group == null ? null : group.getGroupName(),
        entity.getStatus(),
        members.size()
    );
  }

  private DefenseScoringModels.AssessorReportListItem toListItem(
      CertificationDefenseReportEntity report,
      CertificationTaskEntity certTask,
      CertificationCandidateEntity candidate
  ) {
    return toListItem(report, certTask, candidate, null, null);
  }

  private DefenseScoringModels.AssessorReportListItem toListItem(
      CertificationDefenseReportEntity report,
      CertificationTaskEntity certTask,
      CertificationCandidateEntity candidate,
      CertificationDefenseScoreEntity thesisScore,
      CertificationDefenseScoreEntity performanceScore
  ) {
    return new DefenseScoringModels.AssessorReportListItem(
        report.getId(),
        report.getTaskId(),
        certTask == null ? null : certTask.getTaskName(),
        report.getCandidateId(),
        candidate == null ? null : candidate.getCandidateName(),
        candidate == null ? null : candidate.getMaskedIdCardNo(),
        candidate == null ? null : candidate.getAdmissionTicketNo(),
        candidate == null ? null : candidate.getWorkTypeName(),
        candidate == null ? null : candidate.getLevelName(),
        report.getFileId(),
        report.getFileName(),
        statusOf(thesisScore),
        thesisScore == null ? null : thesisScore.getFinalScore(),
        thesisScore != null && Integer.valueOf(1).equals(thesisScore.getRejected()),
        statusOf(performanceScore),
        performanceScore == null ? null : performanceScore.getFinalScore(),
        performanceScore != null && Integer.valueOf(1).equals(performanceScore.getRejected())
    );
  }

  private static String statusOf(CertificationDefenseScoreEntity score) {
    if (score == null || !StringUtils.hasText(score.getStatus())) {
      return DefenseScoreCatalog.STATUS_EMPTY;
    }
    return score.getStatus();
  }

  private Map<String, CertificationDefenseScoreEntity> loadScoreMap(
      Long assessorId,
      List<CertificationDefenseReportEntity> reports
  ) {
    if (assessorId == null || reports == null || reports.isEmpty()) {
      return Map.of();
    }
    List<Long> candidateIds = reports.stream()
        .map(CertificationDefenseReportEntity::getCandidateId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    List<Long> taskIds = reports.stream()
        .map(CertificationDefenseReportEntity::getTaskId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (candidateIds.isEmpty() || taskIds.isEmpty()) {
      return Map.of();
    }
    List<CertificationDefenseScoreEntity> scores = safeList(scoreMapper.selectList(
        new LambdaQueryWrapper<CertificationDefenseScoreEntity>()
            .eq(CertificationDefenseScoreEntity::getAssessorId, assessorId)
            .in(CertificationDefenseScoreEntity::getTaskId, taskIds)
            .in(CertificationDefenseScoreEntity::getCandidateId, candidateIds)));
    Map<String, CertificationDefenseScoreEntity> map = new HashMap<>();
    for (CertificationDefenseScoreEntity score : scores) {
      if (score.getTaskId() == null || score.getCandidateId() == null || score.getSheetType() == null) {
        continue;
      }
      map.put(score.getTaskId() + ":" + score.getCandidateId() + ":" + score.getSheetType(), score);
    }
    return map;
  }

  private CertificationDefenseScoreEntity loadScore(
      Long assessorId,
      CertificationDefenseReportEntity report,
      String sheetType
  ) {
    if (assessorId == null || report == null) {
      return null;
    }
    return scoreMapper.selectOne(new LambdaQueryWrapper<CertificationDefenseScoreEntity>()
        .eq(CertificationDefenseScoreEntity::getAssessorId, assessorId)
        .eq(CertificationDefenseScoreEntity::getTaskId, report.getTaskId())
        .eq(CertificationDefenseScoreEntity::getCandidateId, report.getCandidateId())
        .eq(CertificationDefenseScoreEntity::getSheetType, sheetType)
        .last("LIMIT 1"));
  }

  private static String scoreKey(CertificationDefenseReportEntity report, String sheetType) {
    return report.getTaskId() + ":" + report.getCandidateId() + ":" + sheetType;
  }

  private void requirePdf(Long fileId, String fileName) {
    FileEntity file = fileApplicationService.getExistingFile(SecurityContext.currentUser().getTenantId(), fileId);
    String name = StringUtils.hasText(fileName) ? fileName : file.getOriginalName();
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    boolean pdfName = StringUtils.hasText(name) && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
    boolean pdfType = contentType.contains("pdf");
    if (!pdfName && !pdfType) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持上传 PDF 格式的业绩评估报告");
    }
  }

  CertificationDefenseReportEntity requireVisibleReport(Long reportId) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    CertificationDefenseReportEntity report = reportMapper.selectById(reportId);
    if (report == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "业绩评估报告不存在");
    }
    CertificationDefenseConfigEntity config = loadConfig(report.getTaskId());
    if (config == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权阅览该业绩评估报告");
    }
    CertificationTaskAssessorEntity membership = taskAssessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, report.getTaskId())
            .eq(CertificationTaskAssessorEntity::getGroupId, config.getGroupId())
            .eq(CertificationTaskAssessorEntity::getAssessorId, assessor.getId())
            .last("LIMIT 1"));
    if (membership == null) {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      log.warn("用户{}尝试阅览非本考评组业绩评估报告{}", user.getUserId(), reportId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权阅览该业绩评估报告");
    }
    return report;
  }

  CertificationTaskEntity requireTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationTaskAssessorGroupEntity requireGroup(Long taskId, Long groupId) {
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(groupId);
    if (group == null || !taskId.equals(group.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评组不存在");
    }
    return group;
  }

  CertificationCandidateEntity requireCandidate(Long taskId, Long candidateId) {
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId()) || "inactive".equalsIgnoreCase(candidate.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    return candidate;
  }

  CertificationInfoAssessorEntity currentAssessor() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    if (assessor == null) {
      log.warn("用户{}没有已入库考评员身份", user.getUserId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定有效考评员档案");
    }
    return assessor;
  }

  private CertificationDefenseConfigEntity loadConfig(Long taskId) {
    return configMapper.selectOne(new LambdaQueryWrapper<CertificationDefenseConfigEntity>()
        .eq(CertificationDefenseConfigEntity::getTaskId, taskId)
        .last("LIMIT 1"));
  }

  private CertificationDefenseReportEntity loadReport(Long taskId, Long candidateId) {
    return reportMapper.selectOne(new LambdaQueryWrapper<CertificationDefenseReportEntity>()
        .eq(CertificationDefenseReportEntity::getTaskId, taskId)
        .eq(CertificationDefenseReportEntity::getCandidateId, candidateId)
        .last("LIMIT 1"));
  }

  private List<CertificationCandidateEntity> activeCandidates(Long taskId) {
    // 考生导入后默认为 active；实操开考后会变成 exam_in_progress / absent。
    // 综合答辩配置需要看到本任务全部未停用考生，不能只查 active。
    return safeList(candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .orderByAsc(CertificationCandidateEntity::getId)))
        .stream()
        .filter(candidate -> !isInactiveCandidate(candidate))
        .toList();
  }

  private boolean isInactiveCandidate(CertificationCandidateEntity candidate) {
    return candidate != null && "inactive".equalsIgnoreCase(candidate.getStatus());
  }

  private List<CertificationTaskAssessorEntity> groupMembers(Long taskId, Long groupId) {
    return safeList(taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId)
            .orderByAsc(CertificationTaskAssessorEntity::getSortOrder)
            .orderByAsc(CertificationTaskAssessorEntity::getId)));
  }

  private Map<Long, CertificationTaskEntity> loadTaskMap(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, CertificationTaskEntity> map = new HashMap<>();
    for (CertificationTaskEntity task : safeList(taskMapper.selectBatchIds(ids))) {
      if (task.getId() != null) {
        map.put(task.getId(), task);
      }
    }
    return map;
  }

  private Map<Long, CertificationCandidateEntity> loadCandidateMap(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, CertificationCandidateEntity> map = new HashMap<>();
    for (CertificationCandidateEntity candidate : safeList(candidateMapper.selectBatchIds(ids))) {
      if (candidate.getId() != null) {
        map.put(candidate.getId(), candidate);
      }
    }
    return map;
  }

  private String resolveFileName(DefenseScoringModels.ReportFileReq req) {
    if (req == null) {
      return null;
    }
    if (StringUtils.hasText(req.fileName())) {
      return req.fileName().trim();
    }
    FileEntity file = fileApplicationService.getExistingFile(SecurityContext.currentUser().getTenantId(), req.fileId());
    return file.getOriginalName();
  }

  private static String stripExtension(String fileName) {
    String name = fileName.trim();
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0 && slash < name.length() - 1) {
      name = name.substring(slash + 1);
    }
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean containsToken(String stem, String token) {
    return StringUtils.hasText(token) && stem.contains(token.trim());
  }

  private static <T> List<T> safeList(List<T> list) {
    return list == null ? List.of() : list;
  }

  record MatchOutcome(CertificationCandidateEntity candidate, String matchKey, String reason) {
    static MatchOutcome matched(CertificationCandidateEntity candidate, String matchKey) {
      return new MatchOutcome(candidate, matchKey, null);
    }

    static MatchOutcome unmatched(String reason) {
      return new MatchOutcome(null, null, reason);
    }
  }
}
