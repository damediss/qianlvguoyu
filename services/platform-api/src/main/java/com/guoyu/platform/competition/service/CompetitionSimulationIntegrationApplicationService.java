package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.common.util.SensitiveMaskUtils;
import com.guoyu.platform.competition.dto.CompetitionSimulationProviderBindingReq;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationProviderBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationSyncBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationSyncRowEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.integration.plantvr.PlantVrCompetitionClient;
import com.guoyu.platform.competition.integration.plantvr.PlantVrCompetitionClient.ScoreAttempt;
import com.guoyu.platform.competition.integration.plantvr.PlantVrCompetitionClient.SessionInfo;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionSimulationProviderBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionSimulationSyncBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionSimulationSyncRowMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantRow;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionSimulationProviderBindingVO;
import com.guoyu.platform.competition.vo.CompetitionSimulationSyncBatchVO;
import com.guoyu.platform.competition.vo.CompetitionSimulationSyncRowVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CompetitionSimulationIntegrationApplicationService {
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

  private final CompetitionSimulationProviderBindingMapper bindingMapper;
  private final CompetitionSimulationSyncBatchMapper batchMapper;
  private final CompetitionSimulationSyncRowMapper rowMapper;
  private final CompetitionSubjectScoreContestantMapper contestantMapper;
  private final CompetitionStageContentMapper stageContentMapper;
  private final CompetitionRankingSnapshotMapper rankingMapper;
  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService;
  private final CompetitionConfirmedScoreSourceService confirmedScoreSourceService;
  private final CompetitionPlayerIdentityDomainService identityDomainService;
  private final CompetitionPlayerDataHash dataHash;
  private final PlantVrCompetitionClient client;
  private final ObjectMapper objectMapper;
  private final Executor executor;
  private final TransactionTemplate transactionTemplate;

  public CompetitionSimulationIntegrationApplicationService(
      CompetitionSimulationProviderBindingMapper bindingMapper,
      CompetitionSimulationSyncBatchMapper batchMapper,
      CompetitionSimulationSyncRowMapper rowMapper,
      CompetitionSubjectScoreContestantMapper contestantMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionRankingSnapshotMapper rankingMapper,
      CompetitionScoreRuleApplicationService scoreRuleApplicationService,
      CompetitionConfirmedScoreSourceService confirmedScoreSourceService,
      CompetitionPlayerIdentityDomainService identityDomainService,
      CompetitionPlayerDataHash dataHash,
      PlantVrCompetitionClient client,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager,
      @Qualifier("competitionSubjectScoreImportExecutor") Executor executor) {
    this.bindingMapper = bindingMapper;
    this.batchMapper = batchMapper;
    this.rowMapper = rowMapper;
    this.contestantMapper = contestantMapper;
    this.stageContentMapper = stageContentMapper;
    this.rankingMapper = rankingMapper;
    this.scoreRuleApplicationService = scoreRuleApplicationService;
    this.confirmedScoreSourceService = confirmedScoreSourceService;
    this.identityDomainService = identityDomainService;
    this.dataHash = dataHash;
    this.client = client;
    this.objectMapper = objectMapper;
    this.executor = executor;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Transactional(readOnly = true)
  public CompetitionSimulationProviderBindingVO binding(
      Long eventId, Long stageId, Long workTypeId, Long levelId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    scoreRuleApplicationService.accessibleEvent(user, eventId);
    return CompetitionSimulationProviderBindingVO.from(bindingMapper.selectOne(
        bindingScope(eventId, stageId, workTypeId, levelId)
            .eq(CompetitionSimulationProviderBindingEntity::getProviderCode, "plantvr")
            .last("LIMIT 1")));
  }

  @Transactional(readOnly = true)
  public List<SessionInfo> availablePlantVrSessions(Long eventId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    scoreRuleApplicationService.accessibleEvent(user, eventId);
    return client.fetchAllSessions();
  }

  @Transactional
  public CompetitionSimulationProviderBindingVO saveBinding(
      Long eventId, CompetitionSimulationProviderBindingReq request) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    scoreRuleApplicationService.accessibleEvent(user, eventId);
    requireSimulationContent(eventId, request.stageId());
    CompetitionSimulationProviderBindingEntity entity = bindingMapper.selectOne(
        bindingScope(eventId, request.stageId(),
            request.eventWorkTypeId(), request.eventWorkTypeLevelId())
            .eq(CompetitionSimulationProviderBindingEntity::getProviderCode, "plantvr")
            .last("LIMIT 1"));
    LocalDateTime now = LocalDateTime.now();
    boolean creating = entity == null;
    if (entity == null) {
      entity = new CompetitionSimulationProviderBindingEntity();
      entity.setId(IdWorker.getId());
      entity.setTenantId(user.getTenantId());
      entity.setEventId(eventId);
      entity.setStageId(request.stageId());
      entity.setEventWorkTypeId(request.eventWorkTypeId());
      entity.setEventWorkTypeLevelId(request.eventWorkTypeLevelId());
      entity.setProviderCode("plantvr");
      entity.setCreatedBy(user.getUserId());
      entity.setCreatedAt(now);
      entity.setDeleted(0);
    }
    entity.setExternalHostTenantId(request.externalHostTenantId().trim());
    entity.setExternalSessionId(request.externalSessionId().trim());
    entity.setModuleFilter(normalizeNullable(request.moduleFilter()));
    entity.setStatus("active");
    entity.setUpdatedBy(user.getUserId());
    entity.setUpdatedAt(now);
    if (creating) bindingMapper.insert(entity);
    else bindingMapper.updateById(entity);
    return CompetitionSimulationProviderBindingVO.from(entity);
  }

  @Transactional
  public CompetitionSimulationSyncBatchVO startSync(Long bindingId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationProviderBindingEntity binding = requireBinding(
        user.getTenantId(), bindingId);
    scoreRuleApplicationService.accessibleEvent(user, binding.getEventId());
    if (!"active".equals(binding.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "PlantVR场次绑定已停用");
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionSimulationSyncBatchEntity batch = new CompetitionSimulationSyncBatchEntity();
    batch.setId(IdWorker.getId());
    batch.setTenantId(user.getTenantId());
    batch.setBindingId(bindingId);
    batch.setEventId(binding.getEventId());
    batch.setStageId(binding.getStageId());
    batch.setEventWorkTypeId(binding.getEventWorkTypeId());
    batch.setEventWorkTypeLevelId(binding.getEventWorkTypeLevelId());
    batch.setBatchStatus("syncing");
    batch.setTotalRows(0);
    batch.setValidRows(0);
    batch.setErrorRows(0);
    batch.setConfirmedRows(0);
    batch.setCreatedBy(user.getUserId());
    batch.setUpdatedBy(user.getUserId());
    batch.setCreatedAt(now);
    batch.setUpdatedAt(now);
    batch.setDeleted(0);
    if (batchMapper.insert(batch) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "仿真成绩同步任务创建失败");
    }
    scheduleAfterCommit(() -> {
      try {
        executor.execute(() -> syncAsync(
            user.getTenantId(), user.getUserId(), bindingId, batch.getId()));
      } catch (RuntimeException ex) {
        markFailed(user.getTenantId(), user.getUserId(), batch.getId(), ex.getMessage());
      }
    });
    return CompetitionSimulationSyncBatchVO.from(batch);
  }

  @Transactional(readOnly = true)
  public CompetitionSimulationSyncBatchVO batch(Long batchId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationSyncBatchEntity batch = requireBatch(user.getTenantId(), batchId);
    scoreRuleApplicationService.accessibleEvent(user, batch.getEventId());
    return CompetitionSimulationSyncBatchVO.from(batch);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionSimulationSyncRowVO> rows(Long batchId, PageQuery query) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationSyncBatchEntity batch = requireBatch(user.getTenantId(), batchId);
    scoreRuleApplicationService.accessibleEvent(user, batch.getEventId());
    IPage<CompetitionSimulationSyncRowEntity> page = rowMapper.selectPage(
        new Page<>(query.getPage(), query.getPageSize()),
        new LambdaQueryWrapper<CompetitionSimulationSyncRowEntity>()
            .eq(CompetitionSimulationSyncRowEntity::getBatchId, batchId)
            .eq(CompetitionSimulationSyncRowEntity::getDeleted, 0)
            .orderByAsc(CompetitionSimulationSyncRowEntity::getRowNo));
    List<CompetitionSimulationSyncRowVO> records = page.getRecords() == null
        ? List.of() : page.getRecords().stream().map(this::toRowVO).toList();
    return new PageResponse<>(records, query.getPage(), query.getPageSize(), page.getTotal());
  }

  @Transactional
  public CompetitionSimulationSyncBatchVO confirm(Long batchId, String correctionReason) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationSyncBatchEntity batch = requireBatch(user.getTenantId(), batchId);
    scoreRuleApplicationService.accessibleEvent(user, batch.getEventId());
    if ("completed".equals(batch.getBatchStatus()) || "confirming".equals(batch.getBatchStatus())) {
      return CompetitionSimulationSyncBatchVO.from(batch);
    }
    if (!("ready".equals(batch.getBatchStatus())
        || "ready_with_errors".equals(batch.getBatchStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "同步核验尚未完成");
    }
    CompetitionRankingSnapshotEntity published = rankingMapper.selectOne(
        rankingScope(batch).eq(CompetitionRankingSnapshotEntity::getStatus, "published")
            .last("LIMIT 1"));
    if (published != null && (correctionReason == null || correctionReason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布排名更正仿真成绩必须填写原因");
    }
    batch.setBatchStatus("confirming");
    batch.setCorrectionReason(normalizeNullable(correctionReason));
    batch.setUpdatedBy(user.getUserId());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    scheduleAfterCommit(() -> {
      try {
        executor.execute(() -> confirmAsync(user.getTenantId(), user.getUserId(), batchId));
      } catch (RuntimeException ex) {
        markFailed(user.getTenantId(), user.getUserId(), batchId, ex.getMessage());
      }
    });
    return CompetitionSimulationSyncBatchVO.from(batch);
  }

  @Transactional(readOnly = true)
  public CompetitionSimulationSyncRowVO scoreDetail(Long scoreSourceId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionSimulationSyncRowEntity row = rowMapper.selectOne(
        new LambdaQueryWrapper<CompetitionSimulationSyncRowEntity>()
            .eq(CompetitionSimulationSyncRowEntity::getScoreSourceId, scoreSourceId)
            .eq(CompetitionSimulationSyncRowEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "仿真评分明细不存在");
    CompetitionSimulationSyncBatchEntity batch = requireBatch(user.getTenantId(), row.getBatchId());
    scoreRuleApplicationService.accessibleEvent(user, batch.getEventId());
    return toRowVO(row);
  }

  private void syncAsync(Long tenantId, Long userId, Long bindingId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      CompetitionSimulationProviderBindingEntity binding = requireBinding(tenantId, bindingId);
      List<ScoreAttempt> attempts = client.fetchAllAttempts(
          binding.getExternalHostTenantId(), binding.getExternalSessionId());
      Map<String, Map<Long, String>> contestants = contestantsByIdCardHash(binding);
      List<PreparedRow> prepared = new ArrayList<>();
      int rowNo = 0;
      for (ScoreAttempt attempt : attempts) {
        if (!matchesModule(binding.getModuleFilter(), attempt)) continue;
        prepared.add(prepare(++rowNo, attempt, contestants));
      }
      Map<Long, Integer> attemptCount = new HashMap<>();
      for (PreparedRow item : prepared) {
        if (item.memberId() != null && item.error() == null) {
          attemptCount.merge(item.memberId(), 1, Integer::sum);
        }
      }
      int valid = 0;
      int errors = 0;
      for (PreparedRow item : prepared) {
        String error = item.error();
        String status = error == null ? "valid" : "invalid";
        if (error == null && attemptCount.getOrDefault(item.memberId(), 0) > 1) {
          error = "同一选手存在多次仿真成绩，请先在PlantVR确认正式成绩";
          status = "conflict";
        }
        insertRow(tenantId, userId, batchId, item, status, error);
        if ("valid".equals(status)) valid++;
        else errors++;
      }
      CompetitionSimulationSyncBatchEntity batch = requireBatch(tenantId, batchId);
      batch.setTotalRows(prepared.size());
      batch.setValidRows(valid);
      batch.setErrorRows(errors);
      batch.setBatchStatus(valid > 0 ? (errors == 0 ? "ready" : "ready_with_errors") : "failed");
      batch.setErrorMessage(valid == 0 ? "没有可确认的PlantVR仿真成绩" : null);
      batch.setUpdatedBy(userId);
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
      binding.setLastSyncAt(LocalDateTime.now());
      binding.setUpdatedBy(userId);
      binding.setUpdatedAt(LocalDateTime.now());
      bindingMapper.updateById(binding);
    } catch (RuntimeException ex) {
      markFailed(tenantId, userId, batchId, ex.getMessage());
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void confirmAsync(Long tenantId, Long userId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      while (Boolean.TRUE.equals(transactionTemplate.execute(
          ignored -> confirmChunk(tenantId, userId, batchId)))) {
        // Commit every chunk so a single slow downstream calculation does not hold all rows.
      }
    } catch (RuntimeException ex) {
      markFailed(tenantId, userId, batchId, ex.getMessage());
    } finally {
      TenantContextHolder.clear();
    }
  }

  private boolean confirmChunk(Long tenantId, Long userId, Long batchId) {
    CompetitionSimulationSyncBatchEntity batch = requireBatch(tenantId, batchId);
    if (!"confirming".equals(batch.getBatchStatus())) return false;
    List<CompetitionSimulationSyncRowEntity> pending = rowMapper.selectList(
        new LambdaQueryWrapper<CompetitionSimulationSyncRowEntity>()
            .eq(CompetitionSimulationSyncRowEntity::getBatchId, batchId)
            .eq(CompetitionSimulationSyncRowEntity::getValidationStatus, "valid")
            .eq(CompetitionSimulationSyncRowEntity::getDeleted, 0)
            .orderByAsc(CompetitionSimulationSyncRowEntity::getRowNo)
            .last("LIMIT 50"));
    if (pending == null || pending.isEmpty()) {
      batch.setBatchStatus("completed");
      batch.setConfirmedAt(LocalDateTime.now());
      batch.setConfirmedBy(userId);
      batch.setUpdatedBy(userId);
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
      return false;
    }
    CompetitionSimulationProviderBindingEntity binding = requireBinding(tenantId, batch.getBindingId());
    int confirmed = 0;
    for (CompetitionSimulationSyncRowEntity row : pending) {
      CompetitionScoreSourceEntity source = confirmedScoreSourceService.saveConfirmed(
          tenantId, userId, batch.getEventId(), batch.getStageId(), batch.getEventWorkTypeId(),
          batch.getEventWorkTypeLevelId(), row.getContestantMemberId(), "simulation",
          row.getRawScore(), "plantvr",
          "plantvr:" + binding.getExternalHostTenantId() + ":"
              + binding.getExternalSessionId() + ":" + row.getExternalSessionId(),
          null, row.getAchievedAt());
      row.setScoreSourceId(source.getId());
      row.setValidationStatus("confirmed");
      row.setUpdatedBy(userId);
      row.setUpdatedAt(LocalDateTime.now());
      rowMapper.updateById(row);
      confirmed++;
    }
    batch.setConfirmedRows((batch.getConfirmedRows() == null ? 0 : batch.getConfirmedRows())
        + confirmed);
    batch.setUpdatedBy(userId);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    return true;
  }

  private PreparedRow prepare(
      int rowNo, ScoreAttempt attempt, Map<String, Map<Long, String>> contestants) {
    String error = null;
    String normalizedIdCard = null;
    try {
      normalizedIdCard = identityDomainService.normalizeIdCard(attempt.idCard());
    } catch (RuntimeException ex) {
      error = "PlantVR身份证号格式不合法";
    }
    Map<Long, String> matches = normalizedIdCard == null ? Map.of()
        : contestants.getOrDefault(dataHash.lookupHash(normalizedIdCard), Map.of());
    Long memberId = null;
    if (error == null && matches.isEmpty()) error = "身份证号不在当前赛事赛段工种等级名单";
    else if (error == null && matches.size() > 1) error = "身份证号匹配到多名云平台选手";
    else if (error == null) {
      memberId = matches.keySet().iterator().next();
      if (!sameName(attempt.realName(), matches.get(memberId))) {
        error = "PlantVR姓名与云平台选手不一致";
        memberId = null;
      }
    }
    if (attempt.vrSessionId() == null || attempt.vrSessionId().isBlank()) {
      error = "PlantVR会话流水号为空";
    } else if (attempt.score() == null) {
      error = "PlantVR仿真成绩为空";
    } else if (attempt.score().compareTo(BigDecimal.ZERO) < 0
        || attempt.score().compareTo(HUNDRED) > 0) {
      error = "PlantVR仿真成绩必须在0到100之间";
    } else if (!detailScoreConsistent(attempt)) {
      error = "PlantVR总分与评分明细合计不一致";
    }
    String detailJson = json(attempt.scoreDetail());
    return new PreparedRow(
        rowNo, attempt, normalizedIdCard, memberId, inferModuleTitle(attempt), detailJson,
        sha256(json(attempt)), error);
  }

  private void insertRow(
      Long tenantId, Long userId, Long batchId, PreparedRow prepared,
      String status, String error) {
    ScoreAttempt attempt = prepared.attempt();
    LocalDateTime now = LocalDateTime.now();
    CompetitionSimulationSyncRowEntity row = new CompetitionSimulationSyncRowEntity();
    row.setId(IdWorker.getId());
    row.setTenantId(tenantId);
    row.setBatchId(batchId);
    row.setRowNo(prepared.rowNo());
    row.setExternalParticipantId(attempt.competitionParticipantId());
    row.setExternalSessionId(attempt.vrSessionId());
    row.setPlayerName(attempt.realName());
    row.setIdCardMasked(SensitiveMaskUtils.maskIdCard(prepared.normalizedIdCard()));
    row.setContestantMemberId(prepared.memberId());
    row.setRawScore(attempt.score());
    row.setModuleTitle(prepared.moduleTitle());
    row.setAppDisplayName(attempt.appDisplayName());
    row.setDurationSeconds(attempt.totalDuration());
    row.setAchievedAt(attempt.recordedAt() == null ? null
        : LocalDateTime.ofInstant(attempt.recordedAt().toInstant(), SHANGHAI));
    row.setValidationStatus(status);
    row.setErrorMessage(error);
    row.setDetailJson(prepared.detailJson());
    row.setPayloadHash(prepared.payloadHash());
    row.setCreatedBy(userId);
    row.setUpdatedBy(userId);
    row.setCreatedAt(now);
    row.setUpdatedAt(now);
    row.setDeleted(0);
    rowMapper.insert(row);
  }

  private Map<String, Map<Long, String>> contestantsByIdCardHash(
      CompetitionSimulationProviderBindingEntity binding) {
    List<CompetitionSubjectScoreContestantRow> rows = contestantMapper.selectByScope(
        binding.getTenantId(), binding.getEventId(), binding.getStageId(),
        binding.getEventWorkTypeId(), binding.getEventWorkTypeLevelId());
    Map<String, Map<Long, String>> result = new HashMap<>();
    if (rows == null) return result;
    for (CompetitionSubjectScoreContestantRow row : rows) {
      if (row == null || row.getIdCardLookupHash() == null
          || row.getContestantMemberId() == null) continue;
      result.computeIfAbsent(row.getIdCardLookupHash(), ignored -> new LinkedHashMap<>())
          .putIfAbsent(row.getContestantMemberId(), row.getPlayerName());
    }
    return result;
  }

  private boolean detailScoreConsistent(ScoreAttempt attempt) {
    JsonNode detail = attempt.scoreDetail();
    if (detail == null || detail.isNull()) return true;
    JsonNode total = detail.get("totalScore");
    if (total == null || !total.isNumber()) return true;
    return attempt.score() != null
        && attempt.score().compareTo(total.decimalValue()) == 0;
  }

  private String inferModuleTitle(ScoreAttempt attempt) {
    JsonNode sheetTitle = attempt.scoreDetail() == null
        ? null : attempt.scoreDetail().path("sheet").path("moduleTitle");
    String sheetModule = normalizeNullable(
        sheetTitle == null || sheetTitle.isMissingNode() || sheetTitle.isNull()
            ? null : sheetTitle.asText(null));
    if (sheetModule != null) return sheetModule;

    String title = normalizeNullable(attempt.moduleTitle());
    if (title != null && !"未标注".equals(title)) return title;
    JsonNode lines = attempt.scoreDetail() == null ? null : attempt.scoreDetail().get("lines");
    if (lines != null && lines.isArray()) {
      for (JsonNode line : lines) {
        String key = line.path("key").asText("");
        if (key.startsWith("Level11_")) return "燃气焙烧";
        if (key.startsWith("Level1_")) return "焦粒焙烧";
      }
    }
    return title == null ? "未标注" : title;
  }

  private boolean matchesModule(String filter, ScoreAttempt attempt) {
    String expected = normalizeNullable(filter);
    if (expected == null) return true;
    return expected.equalsIgnoreCase(normalizeNullable(attempt.moduleTitle()))
        || expected.equalsIgnoreCase(inferModuleTitle(attempt))
        || expected.equalsIgnoreCase(normalizeNullable(attempt.appDisplayName()));
  }

  private boolean sameName(String left, String right) {
    return normalizeName(left).equals(normalizeName(right));
  }

  private String normalizeName(String value) {
    return value == null ? "" : value.replace(" ", "").replace("\u3000", "").trim();
  }

  private void requireSimulationContent(Long eventId, Long stageId) {
    CompetitionStageContentEntity content = stageContentMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getEventId, eventId)
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getContentType, "simulation")
            .eq(CompetitionStageContentEntity::getStatus, "enabled")
            .eq(CompetitionStageContentEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (content == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段未启用仿真比赛内容");
    }
  }

  private CompetitionSimulationProviderBindingEntity requireBinding(Long tenantId, Long id) {
    CompetitionSimulationProviderBindingEntity entity = bindingMapper.selectById(id);
    if (entity == null || !tenantId.equals(entity.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "PlantVR场次绑定不存在");
    }
    return entity;
  }

  private CompetitionSimulationSyncBatchEntity requireBatch(Long tenantId, Long id) {
    CompetitionSimulationSyncBatchEntity entity = batchMapper.selectById(id);
    if (entity == null || !tenantId.equals(entity.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "仿真成绩同步批次不存在");
    }
    return entity;
  }

  private CompetitionSimulationSyncRowVO toRowVO(CompetitionSimulationSyncRowEntity row) {
    return new CompetitionSimulationSyncRowVO(
        row.getId(), row.getBatchId(), row.getRowNo(), row.getExternalParticipantId(),
        row.getExternalSessionId(), row.getPlayerName(), row.getIdCardMasked(),
        row.getContestantMemberId(), row.getRawScore(), row.getModuleTitle(),
        row.getAppDisplayName(), row.getDurationSeconds(), row.getAchievedAt(),
        row.getValidationStatus(), row.getErrorMessage(), row.getScoreSourceId(),
        parseJson(row.getDetailJson()));
  }

  private JsonNode parseJson(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return objectMapper.readTree(value);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "仿真评分明细解析失败");
    }
  }

  private String json(Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PlantVR成绩数据序列化失败");
    }
  }

  private String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String normalizeNullable(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private void scheduleAfterCommit(Runnable action) {
    Runnable safe = () -> {
      try {
        action.run();
      } catch (RuntimeException ignored) {
        // The worker updates the batch on execution failures; queue rejection is handled below.
      }
    };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { safe.run(); }
      });
    } else safe.run();
  }

  private void markFailed(Long tenantId, Long userId, Long batchId, String message) {
    Long previousTenant = TenantContextHolder.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    try {
      transactionTemplate.executeWithoutResult(ignored -> {
        CompetitionSimulationSyncBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null || "completed".equals(batch.getBatchStatus())) return;
        batch.setBatchStatus("failed");
        batch.setErrorMessage(abbreviate(message));
        batch.setUpdatedBy(userId);
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
      });
    } finally {
      if (previousTenant == null) TenantContextHolder.clear();
      else TenantContextHolder.setTenantId(previousTenant);
    }
  }

  private String abbreviate(String value) {
    String result = value == null || value.isBlank() ? "PlantVR成绩同步失败" : value;
    return result.length() <= 1000 ? result : result.substring(0, 1000);
  }

  private LambdaQueryWrapper<CompetitionSimulationProviderBindingEntity> bindingScope(
      Long eventId, Long stageId, Long workTypeId, Long levelId) {
    return new LambdaQueryWrapper<CompetitionSimulationProviderBindingEntity>()
        .eq(CompetitionSimulationProviderBindingEntity::getEventId, eventId)
        .eq(CompetitionSimulationProviderBindingEntity::getStageId, stageId)
        .eq(CompetitionSimulationProviderBindingEntity::getEventWorkTypeId, workTypeId)
        .eq(levelId != null,
            CompetitionSimulationProviderBindingEntity::getEventWorkTypeLevelId, levelId)
        .isNull(levelId == null,
            CompetitionSimulationProviderBindingEntity::getEventWorkTypeLevelId)
        .eq(CompetitionSimulationProviderBindingEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingScope(
      CompetitionSimulationSyncBatchEntity batch) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, batch.getEventId())
        .eq(CompetitionRankingSnapshotEntity::getStageId, batch.getStageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(batch.getEventWorkTypeLevelId() != null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId,
            batch.getEventWorkTypeLevelId())
        .isNull(batch.getEventWorkTypeLevelId() == null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .eq(CompetitionRankingSnapshotEntity::getDeleted, 0);
  }

  private record PreparedRow(
      int rowNo,
      ScoreAttempt attempt,
      String normalizedIdCard,
      Long memberId,
      String moduleTitle,
      String detailJson,
      String payloadHash,
      String error) {}
}
