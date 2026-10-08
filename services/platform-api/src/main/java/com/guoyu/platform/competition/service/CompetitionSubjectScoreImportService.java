package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.domain.CompetitionStageContentScoringWeights;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionSubjectScoreImportBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionSubjectScoreImportRowEntity;
import com.guoyu.platform.competition.enums.CompetitionScoreSourceType;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreContestantRow;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionSubjectScoreImportRowMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports practical or simulation scores from Excel and matches them to contestants by ID card.
 * Confirmed rows become current score sources with no score rule, so the stage content weights
 * drive the combined score and ranking.
 */
@Service
public class CompetitionSubjectScoreImportService {
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final CompetitionSubjectScoreImportBatchMapper batchMapper;
  private final CompetitionSubjectScoreImportRowMapper rowMapper;
  private final CompetitionSubjectScoreContestantMapper contestantMapper;
  private final CompetitionStageContentMapper stageContentMapper;
  private final CompetitionScoreSourceMapper sourceMapper;
  private final CompetitionRankingSnapshotMapper rankingMapper;
  private final CompetitionScorePipelineOutboxService pipelineOutboxService;
  private final CompetitionAutomaticScorePipelineService scorePipelineService;
  private final CompetitionPlayerIdentityDomainService identityDomainService;
  private final CompetitionPlayerDataHash dataHash;
  private final FileApplicationService fileApplicationService;
  private final Executor executor;
  private final TransactionTemplate transactionTemplate;

  public CompetitionSubjectScoreImportService(
      CompetitionSubjectScoreImportBatchMapper batchMapper,
      CompetitionSubjectScoreImportRowMapper rowMapper,
      CompetitionSubjectScoreContestantMapper contestantMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionScoreSourceMapper sourceMapper,
      CompetitionRankingSnapshotMapper rankingMapper,
      CompetitionScorePipelineOutboxService pipelineOutboxService,
      CompetitionAutomaticScorePipelineService scorePipelineService,
      CompetitionPlayerIdentityDomainService identityDomainService,
      CompetitionPlayerDataHash dataHash,
      FileApplicationService fileApplicationService,
      PlatformTransactionManager transactionManager,
      @Qualifier("competitionSubjectScoreImportExecutor") Executor executor) {
    this.batchMapper = batchMapper;
    this.rowMapper = rowMapper;
    this.contestantMapper = contestantMapper;
    this.stageContentMapper = stageContentMapper;
    this.sourceMapper = sourceMapper;
    this.rankingMapper = rankingMapper;
    this.pipelineOutboxService = pipelineOutboxService;
    this.scorePipelineService = scorePipelineService;
    this.identityDomainService = identityDomainService;
    this.dataHash = dataHash;
    this.fileApplicationService = fileApplicationService;
    this.executor = executor;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public byte[] template(String sourceType) {
    String type = requireImportType(sourceType);
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(label(type) + "成绩");
      Row header = sheet.createRow(0);
      String[] names = {"身份证号", label(type) + "成绩"};
      for (int i = 0; i < names.length; i++) {
        header.createCell(i).setCellValue(names[i]);
        sheet.setColumnWidth(i, i == 0 ? 24 * 256 : 18 * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, label(type) + "成绩模板生成失败");
    }
  }

  @Transactional
  public CompetitionSubjectScoreImportBatchEntity upload(Long tenantId, Long userId,
      Long eventId, Long stageId, Long workTypeId, Long levelId, String sourceType,
      String fileName, byte[] bytes) {
    String type = requireImportType(sourceType);
    requireEnabledContent(stageId, type);
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择非空 Excel 文件");
    }
    String hash = sha256(bytes);
    CompetitionSubjectScoreImportBatchEntity existing = batchMapper.selectOne(batchScope(
        tenantId, eventId, stageId, workTypeId, levelId, type)
        .eq(CompetitionSubjectScoreImportBatchEntity::getFileHash, hash).last("LIMIT 1"));
    if (existing != null) {
      if (!"invalid".equals(existing.getBatchStatus()) && !"failed".equals(existing.getBatchStatus())) {
        return existing;
      }
      rowMapper.delete(new LambdaQueryWrapper<CompetitionSubjectScoreImportRowEntity>()
          .eq(CompetitionSubjectScoreImportRowEntity::getBatchId, existing.getId()));
      batchMapper.deleteById(existing.getId());
    }
    LocalDateTime now = LocalDateTime.now();
    FileVO storedFile = fileApplicationService.storeFromBytesForTenant(tenantId, bytes,
        fileName == null ? type + "-score.xlsx" : fileName,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "private");
    CompetitionSubjectScoreImportBatchEntity batch = new CompetitionSubjectScoreImportBatchEntity();
    batch.setId(IdWorker.getId());
    batch.setTenantId(tenantId);
    batch.setEventId(eventId);
    batch.setStageId(stageId);
    batch.setEventWorkTypeId(workTypeId);
    batch.setEventWorkTypeLevelId(levelId);
    batch.setSourceType(type);
    batch.setSourceFileId(storedFile.id());
    batch.setFileName(fileName == null ? type + "-score.xlsx" : fileName);
    batch.setFileHash(hash);
    batch.setBatchStatus("parsing");
    batch.setTotalRows(0);
    batch.setValidRows(0);
    batch.setErrorRows(0);
    batch.setConfirmedRows(0);
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(now);
    batch.setUpdatedAt(now);
    batch.setDeleted(0);
    if (batchMapper.insert(batch) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, label(type) + "成绩导入批次创建失败");
    }
    scheduleAfterCommit(tenantId, userId, batch.getId());
    return batch;
  }

  public CompetitionSubjectScoreImportBatchEntity detail(Long tenantId, Long batchId) {
    CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !tenantId.equals(batch.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "成绩导入批次不存在");
    }
    return batch;
  }

  public byte[] errorWorkbook(Long tenantId, Long batchId) {
    CompetitionSubjectScoreImportBatchEntity batch = detail(tenantId, batchId);
    List<CompetitionSubjectScoreImportRowEntity> invalidRows = rows(tenantId, batchId, "invalid");
    String typeLabel = label(batch.getSourceType());
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("错误明细");
      String[] names = {"原行号", "身份证号", typeLabel + "成绩", "外部流水号", "取得时间", "备注", "错误原因"};
      Row header = sheet.createRow(0);
      for (int i = 0; i < names.length; i++) {
        header.createCell(i).setCellValue(names[i]);
        sheet.setColumnWidth(i, i == 6 ? 40 * 256 : 22 * 256);
      }
      int index = 1;
      for (CompetitionSubjectScoreImportRowEntity source : invalidRows) {
        Row row = sheet.createRow(index++);
        row.createCell(0).setCellValue(source.getRowNo());
        row.createCell(1).setCellValue(nullToEmpty(source.getIdCardNo()));
        row.createCell(2).setCellValue(source.getRawScore() == null
            ? "" : source.getRawScore().toPlainString());
        row.createCell(3).setCellValue(nullToEmpty(source.getExternalReference()));
        row.createCell(4).setCellValue(source.getAchievedAt() == null
            ? "" : source.getAchievedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        row.createCell(5).setCellValue(nullToEmpty(source.getRemark()));
        row.createCell(6).setCellValue(nullToEmpty(source.getErrorMessage()));
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR,
          typeLabel + "成绩导入批次 " + batch.getId() + " 的错误明细生成失败");
    }
  }

  @Transactional
  public CompetitionSubjectScoreImportBatchEntity confirm(Long tenantId, Long userId,
      Long batchId, String correctionReason) {
    CompetitionSubjectScoreImportBatchEntity batch = detail(tenantId, batchId);
    if ("completed".equals(batch.getBatchStatus()) || "confirming".equals(batch.getBatchStatus())) {
      return batch;
    }
    boolean retryableFailure = "failed".equals(batch.getBatchStatus())
        && batch.getValidRows() != null
        && batch.getValidRows() > (batch.getConfirmedRows() == null ? 0 : batch.getConfirmedRows());
    if (!("ready".equals(batch.getBatchStatus()) || retryableFailure)
        || !Integer.valueOf(0).equals(batch.getErrorRows())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅预检全部通过的批次可以确认");
    }
    requireEnabledContent(batch.getStageId(), batch.getSourceType());
    CompetitionRankingSnapshotEntity published = rankingMapper.selectOne(rankingScope(batch)
        .eq(CompetitionRankingSnapshotEntity::getStatus, "published").last("LIMIT 1"));
    if (published != null && (correctionReason == null || correctionReason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布范围更正成绩必须填写原因");
    }
    batch.setBatchStatus("confirming");
    batch.setCorrectionReason(correctionReason);
    batch.setErrorMessage(null);
    batch.setUpdatedBy(userId);
    if (batchMapper.update(batch, new LambdaQueryWrapper<CompetitionSubjectScoreImportBatchEntity>()
        .eq(CompetitionSubjectScoreImportBatchEntity::getId, batchId)
        .in(CompetitionSubjectScoreImportBatchEntity::getBatchStatus, "ready", "failed")) != 1) {
      CompetitionSubjectScoreImportBatchEntity latest = detail(tenantId, batchId);
      if ("confirming".equals(latest.getBatchStatus()) || "completed".equals(latest.getBatchStatus())) {
        return latest;
      }
      throw new BusinessException(ErrorCode.CONFLICT, "导入批次状态已变化，请刷新后重试");
    }
    scheduleConfirmationAfterCommit(tenantId, userId, batchId);
    return batch;
  }

  private List<CompetitionSubjectScoreImportRowEntity> rows(Long tenantId, Long batchId,
      String validationStatus) {
    detail(tenantId, batchId);
    List<CompetitionSubjectScoreImportRowEntity> rows = rowMapper.selectList(
        new LambdaQueryWrapper<CompetitionSubjectScoreImportRowEntity>()
            .eq(CompetitionSubjectScoreImportRowEntity::getBatchId, batchId)
            .eq(validationStatus != null && !validationStatus.isBlank(),
                CompetitionSubjectScoreImportRowEntity::getValidationStatus, validationStatus)
            .eq(CompetitionSubjectScoreImportRowEntity::getDeleted, 0)
            .orderByAsc(CompetitionSubjectScoreImportRowEntity::getRowNo));
    return rows == null ? List.of() : rows;
  }

  private void scheduleConfirmationAfterCommit(Long tenantId, Long userId, Long batchId) {
    Runnable submit = () -> submitConfirmation(tenantId, userId, batchId);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          submit.run();
        }
      });
    } else {
      submit.run();
    }
  }

  private void submitConfirmation(Long tenantId, Long userId, Long batchId) {
    try {
      executor.execute(() -> confirmAsync(tenantId, userId, batchId));
    } catch (RuntimeException ex) {
      markConfirmationFailed(tenantId, userId, batchId, "确认任务队列繁忙，请稍后重试");
    }
  }

  private void confirmAsync(Long tenantId, Long userId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      while (Boolean.TRUE.equals(transactionTemplate.execute(status ->
          confirmChunk(tenantId, userId, batchId)))) {
        // Each chunk commits independently so a long import does not hold one transaction.
      }
    } catch (RuntimeException ex) {
      markConfirmationFailed(tenantId, userId, batchId, abbreviate(ex.getMessage()));
    } finally {
      TenantContextHolder.clear();
    }
  }

  private boolean confirmChunk(Long tenantId, Long userId, Long batchId) {
    CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !"confirming".equals(batch.getBatchStatus())) return false;
    List<CompetitionSubjectScoreImportRowEntity> pending = rowMapper.selectList(
        new LambdaQueryWrapper<CompetitionSubjectScoreImportRowEntity>()
            .eq(CompetitionSubjectScoreImportRowEntity::getBatchId, batchId)
            .eq(CompetitionSubjectScoreImportRowEntity::getValidationStatus, "valid")
            .orderByAsc(CompetitionSubjectScoreImportRowEntity::getRowNo)
            .last("LIMIT 100"));
    if (pending == null || pending.isEmpty()) {
      batch.setBatchStatus("completed");
      batch.setConfirmedAt(LocalDateTime.now());
      batch.setConfirmedBy(userId);
      batch.setUpdatedBy(userId);
      batch.setErrorMessage(null);
      batchMapper.updateById(batch);
      return false;
    }
    int confirmed = 0;
    for (CompetitionSubjectScoreImportRowEntity row : pending) {
      CompetitionScoreSourceEntity latest = sourceMapper.selectOne(sourceScope(batch,
          row.getContestantMemberId())
          .orderByDesc(CompetitionScoreSourceEntity::getRevisionNo).last("LIMIT 1"));
      CompetitionScoreSourceEntity current = sourceMapper.selectOne(sourceScope(batch,
          row.getContestantMemberId())
          .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1).last("LIMIT 1"));
      if (current != null) {
        current.setCurrentFlag(0);
        current.setUpdatedBy(userId);
        sourceMapper.updateById(current);
      }
      LocalDateTime now = LocalDateTime.now();
      CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
      source.setId(IdWorker.getId());
      source.setTenantId(tenantId);
      source.setEventId(batch.getEventId());
      source.setStageId(batch.getStageId());
      source.setEventWorkTypeId(batch.getEventWorkTypeId());
      source.setEventWorkTypeLevelId(batch.getEventWorkTypeLevelId());
      source.setContestantMemberId(row.getContestantMemberId());
      source.setSourceType(batch.getSourceType());
      source.setRawScore(row.getRawScore());
      source.setConfirmationStatus("confirmed");
      source.setSourceSystem("competition-subject-excel");
      source.setSourceReference("subject-import:" + batch.getSourceType() + ":"
          + batchId + ":row:" + row.getRowNo());
      source.setScoreRuleId(null);
      source.setRevisionNo(latest == null || latest.getRevisionNo() == null
          ? 1 : latest.getRevisionNo() + 1);
      source.setCurrentFlag(1);
      source.setAchievedAt(row.getAchievedAt() == null ? now : row.getAchievedAt());
      source.setConfirmedAt(now);
      source.setConfirmedBy(userId);
      source.setCreatedBy(userId);
      source.setUpdatedBy(userId);
      source.setCreatedAt(now);
      source.setUpdatedAt(now);
      source.setDeleted(0);
      if (sourceMapper.insert(source) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, label(batch.getSourceType()) + "成绩来源保存失败");
      }
      row.setValidationStatus("confirmed");
      row.setScoreSourceId(source.getId());
      row.setUpdatedBy(userId);
      rowMapper.updateById(row);
      confirmed++;
      pipelineOutboxService.enqueue(source, userId);
      scorePipelineService.onConfirmedSource(source, userId);
    }
    batch.setConfirmedRows((batch.getConfirmedRows() == null ? 0 : batch.getConfirmedRows()) + confirmed);
    batch.setUpdatedBy(userId);
    batchMapper.updateById(batch);
    return true;
  }

  private void markConfirmationFailed(Long tenantId, Long userId, Long batchId, String message) {
    Long previousTenantId = TenantContextHolder.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    try {
      transactionTemplate.executeWithoutResult(status -> {
        CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null || "completed".equals(batch.getBatchStatus())) return;
        batch.setBatchStatus("failed");
        batch.setErrorMessage(message);
        batch.setUpdatedBy(userId);
        batchMapper.updateById(batch);
      });
    } finally {
      if (previousTenantId == null) TenantContextHolder.clear();
      else TenantContextHolder.setTenantId(previousTenantId);
    }
  }

  private void parseAsync(Long tenantId, Long userId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) return;
      byte[] bytes = fileApplicationService.readFileBytesById(batch.getSourceFileId());
      Map<String, Set<Long>> membersByHash = contestantsByIdCardHash(batch);
      Set<String> duplicates = new HashSet<>();
      int total = 0;
      int valid = 0;
      int invalid = 0;
      DataFormatter formatter = new DataFormatter();
      try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
          Row excelRow = sheet.getRow(i);
          if (excelRow == null || formatter.formatCellValue(excelRow.getCell(0)).isBlank()) continue;
          total++;
          CompetitionSubjectScoreImportRowEntity row = new CompetitionSubjectScoreImportRowEntity();
          row.setId(IdWorker.getId());
          row.setTenantId(tenantId);
          row.setBatchId(batchId);
          row.setRowNo(i + 1);
          row.setExternalReference(text(formatter, excelRow.getCell(2)));
          row.setRemark(text(formatter, excelRow.getCell(4)));
          String error = null;
          String normalizedIdCard = null;
          try {
            normalizedIdCard = identityDomainService.normalizeIdCard(text(formatter, excelRow.getCell(0)));
            row.setIdCardNo(normalizedIdCard);
          } catch (RuntimeException ex) {
            error = "身份证号格式不合法";
            row.setIdCardNo(text(formatter, excelRow.getCell(0)));
          }
          try {
            row.setRawScore(decimal(formatter, excelRow.getCell(1)));
          } catch (RuntimeException ex) {
            if (error == null) error = label(batch.getSourceType()) + "成绩格式错误";
          }
          try {
            row.setAchievedAt(dateTime(formatter, excelRow.getCell(3)));
          } catch (RuntimeException ex) {
            row.setAchievedAt(null);
          }
          String hash = normalizedIdCard == null ? null : dataHash.lookupHash(normalizedIdCard);
          Set<Long> members = hash == null ? Set.of() : membersByHash.getOrDefault(hash, Set.of());
          Long matchedMemberId = null;
          if (normalizedIdCard != null && !duplicates.add(normalizedIdCard)) {
            error = "批次内身份证号重复";
          } else if (error == null && members.isEmpty()) {
            error = "身份证号不在当前赛事赛段工种等级名单";
          } else if (error == null && members.size() > 1) {
            error = "身份证号匹配到多名选手";
          } else if (error == null && row.getRawScore() == null) {
            error = label(batch.getSourceType()) + "成绩不能为空";
          } else if (error == null && (row.getRawScore().compareTo(BigDecimal.ZERO) < 0
              || row.getRawScore().compareTo(HUNDRED) > 0)) {
            error = label(batch.getSourceType()) + "成绩必须在0到100之间";
          } else if (error == null) {
            matchedMemberId = members.iterator().next();
          }
          row.setContestantMemberId(matchedMemberId);
          row.setValidationStatus(error == null ? "valid" : "invalid");
          row.setErrorMessage(error);
          row.setCreatedBy(userId);
          row.setUpdatedBy(userId);
          row.setCreatedAt(LocalDateTime.now());
          row.setUpdatedAt(LocalDateTime.now());
          row.setDeleted(0);
          rowMapper.insert(row);
          if (error == null) valid++;
          else invalid++;
        }
      }
      batch.setTotalRows(total);
      batch.setValidRows(valid);
      batch.setErrorRows(invalid);
      batch.setBatchStatus(total > 0 && invalid == 0 ? "ready" : "invalid");
      batch.setUpdatedBy(userId);
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
    } catch (Exception ex) {
      CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        batch.setBatchStatus("failed");
        batch.setErrorMessage(abbreviate(ex.getMessage()));
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void scheduleAfterCommit(Long tenantId, Long userId, Long batchId) {
    Runnable submit = () -> {
      try {
        executor.execute(() -> parseAsync(tenantId, userId, batchId));
      } catch (RuntimeException ex) {
        markDispatchFailed(tenantId, userId, batchId);
      }
    };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          submit.run();
        }
      });
    } else {
      submit.run();
    }
  }

  private void markDispatchFailed(Long tenantId, Long userId, Long batchId) {
    Long previousTenantId = TenantContextHolder.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    try {
      CompetitionSubjectScoreImportBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null && "parsing".equals(batch.getBatchStatus())) {
        batch.setBatchStatus("failed");
        batch.setErrorMessage("导入处理队列繁忙，请重新上传或稍后重试");
        batch.setUpdatedBy(userId);
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
      }
    } finally {
      if (previousTenantId == null) TenantContextHolder.clear();
      else TenantContextHolder.setTenantId(previousTenantId);
    }
  }

  private Map<String, Set<Long>> contestantsByIdCardHash(
      CompetitionSubjectScoreImportBatchEntity batch) {
    List<CompetitionSubjectScoreContestantRow> rows = contestantMapper.selectByScope(
        batch.getTenantId(), batch.getEventId(), batch.getStageId(),
        batch.getEventWorkTypeId(), batch.getEventWorkTypeLevelId());
    Map<String, Set<Long>> result = new HashMap<>();
    if (rows == null) return result;
    for (CompetitionSubjectScoreContestantRow row : rows) {
      if (row == null || row.getIdCardLookupHash() == null || row.getContestantMemberId() == null) {
        continue;
      }
      result.computeIfAbsent(row.getIdCardLookupHash(), key -> new LinkedHashSet<>())
          .add(row.getContestantMemberId());
    }
    return result;
  }

  private void requireEnabledContent(Long stageId, String sourceType) {
    List<CompetitionStageContentEntity> contents = stageContentMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getDeleted, 0));
    CompetitionStageContentScoringWeights weights =
        CompetitionStageContentScoringWeights.from(contents);
    if (!weights.valid()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "赛段内容权重尚未配置完成，不能导入实操或仿真成绩");
    }
    if (!weights.enabled(sourceType)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段未启用" + label(sourceType) + "，不能导入成绩");
    }
  }

  private String requireImportType(String sourceType) {
    if (CompetitionScoreSourceType.PRACTICAL.getCode().equalsIgnoreCase(sourceType)) {
      return CompetitionScoreSourceType.PRACTICAL.getCode();
    }
    if (CompetitionScoreSourceType.SIMULATION.getCode().equalsIgnoreCase(sourceType)) {
      return CompetitionScoreSourceType.SIMULATION.getCode();
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "只能导入实操或仿真成绩");
  }

  private String label(String sourceType) {
    return "practical".equals(sourceType) ? "实操" : "仿真";
  }

  private LambdaQueryWrapper<CompetitionSubjectScoreImportBatchEntity> batchScope(Long tenantId,
      Long eventId, Long stageId, Long workTypeId, Long levelId, String sourceType) {
    return new LambdaQueryWrapper<CompetitionSubjectScoreImportBatchEntity>()
        .eq(CompetitionSubjectScoreImportBatchEntity::getTenantId, tenantId)
        .eq(CompetitionSubjectScoreImportBatchEntity::getEventId, eventId)
        .eq(CompetitionSubjectScoreImportBatchEntity::getStageId, stageId)
        .eq(CompetitionSubjectScoreImportBatchEntity::getEventWorkTypeId, workTypeId)
        .eq(levelId != null, CompetitionSubjectScoreImportBatchEntity::getEventWorkTypeLevelId, levelId)
        .isNull(levelId == null, CompetitionSubjectScoreImportBatchEntity::getEventWorkTypeLevelId)
        .eq(CompetitionSubjectScoreImportBatchEntity::getSourceType, sourceType)
        .eq(CompetitionSubjectScoreImportBatchEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionScoreSourceEntity> sourceScope(
      CompetitionSubjectScoreImportBatchEntity batch, Long memberId) {
    return new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getEventId, batch.getEventId())
        .eq(CompetitionScoreSourceEntity::getStageId, batch.getStageId())
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(batch.getEventWorkTypeLevelId() != null,
            CompetitionScoreSourceEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .isNull(batch.getEventWorkTypeLevelId() == null,
            CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, memberId)
        .eq(CompetitionScoreSourceEntity::getSourceType, batch.getSourceType())
        .eq(CompetitionScoreSourceEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingScope(
      CompetitionSubjectScoreImportBatchEntity batch) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, batch.getEventId())
        .eq(CompetitionRankingSnapshotEntity::getStageId, batch.getStageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(batch.getEventWorkTypeLevelId() != null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .isNull(batch.getEventWorkTypeLevelId() == null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .isNull(CompetitionRankingSnapshotEntity::getScoreRuleId)
        .eq(CompetitionRankingSnapshotEntity::getDeleted, 0);
  }

  private String normalizeAchievedAt(String value) {
    if (value == null) return null;
    java.util.regex.Matcher matcher = java.util.regex.Pattern
        .compile("^(\\d{4}-\\d{2}-\\d{2}) (\\d{1,3}):(\\d{2})(?::(\\d{2}))?$")
        .matcher(value.trim());
    if (!matcher.matches()) return value;
    int hour = Integer.parseInt(matcher.group(2));
    if (hour > 23) return value;
    String second = matcher.group(4) == null ? "00" : matcher.group(4);
    return matcher.group(1) + " " + String.format("%02d", hour) + ":"
        + matcher.group(3) + ":" + second;
  }

  private String text(DataFormatter formatter, Cell cell) {
    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
    return value.isEmpty() ? null : value;
  }

  private BigDecimal decimal(DataFormatter formatter, Cell cell) {
    String value = text(formatter, cell);
    return value == null ? null : new BigDecimal(value);
  }

  private LocalDateTime dateTime(DataFormatter formatter, Cell cell) {
    String value = normalizeAchievedAt(text(formatter, cell));
    if (value == null) return null;
    if (cell != null && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue();
    }
    DateTimeFormatter[] formats = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    };
    for (DateTimeFormatter format : formats) {
      try {
        return LocalDateTime.parse(value, format);
      } catch (DateTimeParseException ignored) {
        // Try the next spreadsheet datetime format.
      }
    }
    throw new IllegalArgumentException("invalid achieved time");
  }

  private String nullToEmpty(String value) { return value == null ? "" : value; }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String abbreviate(String value) {
    if (value == null) return "Excel 解析失败";
    return value.length() <= 1000 ? value : value.substring(0, 1000);
  }
}
