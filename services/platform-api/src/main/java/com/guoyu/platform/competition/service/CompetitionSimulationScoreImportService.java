package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportRowEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionSimulationScoreImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionSimulationScoreImportRowMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
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

@Service
public class CompetitionSimulationScoreImportService {
  private final CompetitionSimulationScoreImportBatchMapper batchMapper;
  private final CompetitionSimulationScoreImportRowMapper rowMapper;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateMapper;
  private final CompetitionScoreSourceMapper sourceMapper;
  private final CompetitionRankingSnapshotMapper rankingMapper;
  private final CompetitionScorePipelineOutboxService pipelineOutboxService;
  private final FileApplicationService fileApplicationService;
  private final Executor executor;
  private final TransactionTemplate transactionTemplate;

  public CompetitionSimulationScoreImportService(
      CompetitionSimulationScoreImportBatchMapper batchMapper,
      CompetitionSimulationScoreImportRowMapper rowMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateMapper,
      CompetitionScoreSourceMapper sourceMapper,
      CompetitionRankingSnapshotMapper rankingMapper,
      CompetitionScorePipelineOutboxService pipelineOutboxService,
      FileApplicationService fileApplicationService,
      PlatformTransactionManager transactionManager,
      @Qualifier("competitionSimulationImportExecutor") Executor executor) {
    this.batchMapper = batchMapper;
    this.rowMapper = rowMapper;
    this.sessionMapper = sessionMapper;
    this.candidateMapper = candidateMapper;
    this.sourceMapper = sourceMapper;
    this.rankingMapper = rankingMapper;
    this.pipelineOutboxService = pipelineOutboxService;
    this.fileApplicationService = fileApplicationService;
    this.executor = executor;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public byte[] template() {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("仿真成绩");
      Row header = sheet.createRow(0);
      String[] names = {"准考证号", "仿真成绩", "外部流水号", "取得时间", "备注"};
      for (int i = 0; i < names.length; i++) {
        header.createCell(i).setCellValue(names[i]);
        sheet.setColumnWidth(i, i < 2 ? 18 * 256 : 24 * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "仿真成绩模板生成失败");
    }
  }

  @Transactional
  public CompetitionSimulationScoreImportBatchEntity upload(Long tenantId, Long userId,
      Long eventId, Long stageId, Long workTypeId, Long levelId, Long scoreRuleId,
      String fileName, byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择非空 Excel 文件");
    }
    String hash = sha256(bytes);
    CompetitionSimulationScoreImportBatchEntity existing = batchMapper.selectOne(batchScope(
        tenantId, eventId, stageId, workTypeId, levelId, scoreRuleId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getFileHash, hash).last("LIMIT 1"));
    if (existing != null) return existing;
    LocalDateTime now = LocalDateTime.now();
    CompetitionSimulationScoreImportBatchEntity batch = new CompetitionSimulationScoreImportBatchEntity();
    FileVO storedFile = fileApplicationService.storeFromBytesForTenant(tenantId, bytes,
        fileName == null ? "simulation-score.xlsx" : fileName,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "private");
    batch.setId(IdWorker.getId()); batch.setTenantId(tenantId); batch.setEventId(eventId);
    batch.setStageId(stageId); batch.setEventWorkTypeId(workTypeId);
    batch.setEventWorkTypeLevelId(levelId); batch.setScoreRuleId(scoreRuleId);
    batch.setSourceFileId(storedFile.id());
    batch.setFileName(fileName == null ? "simulation-score.xlsx" : fileName);
    batch.setFileHash(hash); batch.setBatchStatus("parsing");
    batch.setTotalRows(0); batch.setValidRows(0); batch.setErrorRows(0); batch.setConfirmedRows(0);
    batch.setCreatedBy(userId); batch.setUpdatedBy(userId); batch.setCreatedAt(now);
    batch.setUpdatedAt(now); batch.setDeleted(0);
    if (batchMapper.insert(batch) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "仿真成绩导入批次创建失败");
    }
    scheduleAfterCommit(tenantId, userId, batch.getId());
    return batch;
  }

  public CompetitionSimulationScoreImportBatchEntity detail(Long tenantId, Long batchId) {
    CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !tenantId.equals(batch.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "仿真成绩导入批次不存在");
    }
    return batch;
  }

  public List<CompetitionSimulationScoreImportRowEntity> rows(Long tenantId, Long batchId,
      String validationStatus) {
    detail(tenantId, batchId);
    List<CompetitionSimulationScoreImportRowEntity> rows = rowMapper.selectList(
        new LambdaQueryWrapper<CompetitionSimulationScoreImportRowEntity>()
            .eq(CompetitionSimulationScoreImportRowEntity::getBatchId, batchId)
            .eq(validationStatus != null && !validationStatus.isBlank(),
                CompetitionSimulationScoreImportRowEntity::getValidationStatus, validationStatus)
            .eq(CompetitionSimulationScoreImportRowEntity::getDeleted, 0)
            .orderByAsc(CompetitionSimulationScoreImportRowEntity::getRowNo));
    return rows == null ? List.of() : new ArrayList<>(rows);
  }

  public byte[] errorWorkbook(Long tenantId, Long batchId) {
    CompetitionSimulationScoreImportBatchEntity batch = detail(tenantId, batchId);
    List<CompetitionSimulationScoreImportRowEntity> invalidRows = rows(tenantId, batchId, "invalid");
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("错误明细");
      String[] names = {"原行号", "准考证号", "仿真成绩", "外部流水号", "取得时间", "备注", "错误原因"};
      Row header = sheet.createRow(0);
      for (int i = 0; i < names.length; i++) {
        header.createCell(i).setCellValue(names[i]);
        sheet.setColumnWidth(i, i == 6 ? 40 * 256 : 22 * 256);
      }
      int index = 1;
      for (CompetitionSimulationScoreImportRowEntity source : invalidRows) {
        Row row = sheet.createRow(index++);
        row.createCell(0).setCellValue(source.getRowNo());
        row.createCell(1).setCellValue(nullToEmpty(source.getCandidateNo()));
        row.createCell(2).setCellValue(source.getSimulationScore() == null
            ? "" : source.getSimulationScore().toPlainString());
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
          "仿真成绩导入批次 " + batch.getId() + " 的错误明细生成失败");
    }
  }

  @Transactional
  public CompetitionSimulationScoreImportBatchEntity confirm(Long tenantId, Long userId,
      Long batchId, String correctionReason) {
    CompetitionSimulationScoreImportBatchEntity batch = detail(tenantId, batchId);
    if ("completed".equals(batch.getBatchStatus())) return batch;
    if ("confirming".equals(batch.getBatchStatus())) return batch;
    boolean retryableFailure = "failed".equals(batch.getBatchStatus())
        && batch.getValidRows() != null
        && batch.getValidRows() > (batch.getConfirmedRows() == null ? 0 : batch.getConfirmedRows());
    if (!("ready".equals(batch.getBatchStatus()) || retryableFailure)
        || !Integer.valueOf(0).equals(batch.getErrorRows())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅预检全部通过的批次可以确认");
    }
    CompetitionRankingSnapshotEntity published = rankingMapper.selectOne(rankingScope(batch)
        .eq(CompetitionRankingSnapshotEntity::getStatus, "published").last("LIMIT 1"));
    if (published != null && (correctionReason == null || correctionReason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布范围更正仿真成绩必须填写原因");
    }
    batch.setBatchStatus("confirming"); batch.setCorrectionReason(correctionReason);
    batch.setErrorMessage(null); batch.setUpdatedBy(userId);
    if (batchMapper.update(batch, new LambdaQueryWrapper<CompetitionSimulationScoreImportBatchEntity>()
        .eq(CompetitionSimulationScoreImportBatchEntity::getId, batchId)
        .in(CompetitionSimulationScoreImportBatchEntity::getBatchStatus, "ready", "failed")) != 1) {
      CompetitionSimulationScoreImportBatchEntity latest = detail(tenantId, batchId);
      if ("confirming".equals(latest.getBatchStatus()) || "completed".equals(latest.getBatchStatus())) {
        return latest;
      }
      throw new BusinessException(ErrorCode.CONFLICT, "导入批次状态已变化，请刷新后重试");
    }
    scheduleConfirmationAfterCommit(tenantId, userId, batchId);
    return batch;
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
        // Each chunk is committed independently to keep locks and transactions short.
      }
    } catch (RuntimeException ex) {
      markConfirmationFailed(tenantId, userId, batchId, abbreviate(ex.getMessage()));
    } finally {
      TenantContextHolder.clear();
    }
  }

  private boolean confirmChunk(Long tenantId, Long userId, Long batchId) {
    CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !"confirming".equals(batch.getBatchStatus())) return false;
    List<CompetitionSimulationScoreImportRowEntity> pending = rowMapper.selectList(
        new LambdaQueryWrapper<CompetitionSimulationScoreImportRowEntity>()
            .eq(CompetitionSimulationScoreImportRowEntity::getBatchId, batchId)
            .eq(CompetitionSimulationScoreImportRowEntity::getValidationStatus, "valid")
            .orderByAsc(CompetitionSimulationScoreImportRowEntity::getRowNo)
            .last("LIMIT 100"));
    if (pending == null || pending.isEmpty()) {
      batch.setBatchStatus("completed"); batch.setConfirmedAt(LocalDateTime.now());
      batch.setConfirmedBy(userId); batch.setUpdatedBy(userId); batch.setErrorMessage(null);
      batchMapper.updateById(batch);
      return false;
    }
    int confirmed = 0;
    for (CompetitionSimulationScoreImportRowEntity row : pending) {
      CompetitionScoreSourceEntity current = sourceMapper.selectOne(sourceScope(batch,
          row.getContestantMemberId()).eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
          .last("LIMIT 1"));
      if (current != null) {
        current.setCurrentFlag(0); current.setUpdatedBy(userId); sourceMapper.updateById(current);
      }
      LocalDateTime now = LocalDateTime.now();
      CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
      source.setId(IdWorker.getId()); source.setTenantId(tenantId); source.setEventId(batch.getEventId());
      source.setStageId(batch.getStageId()); source.setEventWorkTypeId(batch.getEventWorkTypeId());
      source.setEventWorkTypeLevelId(batch.getEventWorkTypeLevelId());
      source.setContestantMemberId(row.getContestantMemberId()); source.setSourceType("simulation");
      source.setRawScore(row.getSimulationScore()); source.setConfirmationStatus("confirmed");
      source.setSourceSystem("competition-simulation-excel");
      source.setSourceReference("simulation-import:" + batchId + ":row:" + row.getRowNo());
      source.setScoreRuleId(batch.getScoreRuleId());
      source.setRevisionNo(current == null ? 1 : current.getRevisionNo() + 1); source.setCurrentFlag(1);
      source.setAchievedAt(row.getAchievedAt() == null ? now : row.getAchievedAt());
      source.setConfirmedAt(now); source.setConfirmedBy(userId); source.setCreatedBy(userId);
      source.setUpdatedBy(userId); source.setCreatedAt(now); source.setUpdatedAt(now); source.setDeleted(0);
      if (sourceMapper.insert(source) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "仿真成绩来源保存失败");
      }
      row.setValidationStatus("confirmed"); row.setScoreSourceId(source.getId());
      row.setUpdatedBy(userId); rowMapper.updateById(row); confirmed++;
      pipelineOutboxService.enqueue(source, userId);
    }
    batch.setConfirmedRows((batch.getConfirmedRows() == null ? 0 : batch.getConfirmedRows()) + confirmed);
    batch.setUpdatedBy(userId); batchMapper.updateById(batch);
    return true;
  }

  private void markConfirmationFailed(Long tenantId, Long userId, Long batchId, String message) {
    Long previousTenantId = TenantContextHolder.getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    try {
      transactionTemplate.executeWithoutResult(status -> {
        CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
        if (batch == null || "completed".equals(batch.getBatchStatus())) return;
        batch.setBatchStatus("failed"); batch.setErrorMessage(message);
        batch.setUpdatedBy(userId); batchMapper.updateById(batch);
      });
    } finally {
      if (previousTenantId == null) TenantContextHolder.clear();
      else TenantContextHolder.setTenantId(previousTenantId);
    }
  }

  private void parseAsync(Long tenantId, Long userId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) return;
      byte[] bytes = fileApplicationService.readFileBytesById(batch.getSourceFileId());
      Map<String, CompetitionFormalCandidateExamEntity> candidates = candidates(batch);
      Set<String> duplicates = new HashSet<>();
      int total = 0, valid = 0, invalid = 0;
      DataFormatter formatter = new DataFormatter();
      try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
        Sheet sheet = workbook.getSheetAt(0);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
          Row excelRow = sheet.getRow(i);
          if (excelRow == null || formatter.formatCellValue(excelRow.getCell(0)).isBlank()) continue;
          total++;
          CompetitionSimulationScoreImportRowEntity row = new CompetitionSimulationScoreImportRowEntity();
          row.setId(IdWorker.getId()); row.setTenantId(tenantId); row.setBatchId(batchId);
          row.setRowNo(i + 1); row.setCandidateNo(text(formatter, excelRow.getCell(0)));
          row.setExternalReference(text(formatter, excelRow.getCell(2)));
          row.setRemark(text(formatter, excelRow.getCell(4)));
          String error = null;
          try { row.setSimulationScore(decimal(formatter, excelRow.getCell(1))); }
          catch (Exception ex) { error = "仿真成绩格式错误"; }
          try { row.setAchievedAt(dateTime(formatter, excelRow.getCell(3))); }
          catch (Exception ex) { if (error == null) error = "取得时间格式错误"; }
          CompetitionFormalCandidateExamEntity candidate = candidates.get(row.getCandidateNo());
          if (!duplicates.add(row.getCandidateNo())) error = "批次内准考证号重复";
          else if (candidate == null) error = "准考证号不在当前赛事赛段工种等级名单";
          else if (CompetitionFormalCandidateExamStatus.ABSENT.getCode().equals(candidate.getStatus())) {
            error = "缺考选手不能导入仿真成绩";
          } else if (row.getSimulationScore() == null) error = "仿真成绩不能为空";
          else if (row.getSimulationScore().compareTo(BigDecimal.ZERO) < 0
              || row.getSimulationScore().compareTo(new BigDecimal("100")) > 0) {
            error = "仿真成绩必须在0到100之间";
          }
          row.setContestantMemberId(candidate == null ? null : candidate.getContestantMemberId());
          row.setValidationStatus(error == null ? "valid" : "invalid"); row.setErrorMessage(error);
          row.setCreatedBy(userId); row.setUpdatedBy(userId); row.setCreatedAt(LocalDateTime.now());
          row.setUpdatedAt(LocalDateTime.now()); row.setDeleted(0); rowMapper.insert(row);
          if (error == null) valid++; else invalid++;
        }
      }
      batch.setTotalRows(total); batch.setValidRows(valid); batch.setErrorRows(invalid);
      batch.setBatchStatus(total > 0 && invalid == 0 ? "ready" : "invalid");
      batch.setUpdatedBy(userId); batch.setUpdatedAt(LocalDateTime.now()); batchMapper.updateById(batch);
    } catch (Exception ex) {
      CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        batch.setBatchStatus("failed"); batch.setErrorMessage(abbreviate(ex.getMessage()));
        batch.setUpdatedAt(LocalDateTime.now()); batchMapper.updateById(batch);
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
      CompetitionSimulationScoreImportBatchEntity batch = batchMapper.selectById(batchId);
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

  private Map<String, CompetitionFormalCandidateExamEntity> candidates(
      CompetitionSimulationScoreImportBatchEntity batch) {
    List<CompetitionFormalExamSessionEntity> sessions = sessionMapper.selectList(
        new LambdaQueryWrapper<CompetitionFormalExamSessionEntity>()
            .eq(CompetitionFormalExamSessionEntity::getEventId, batch.getEventId())
            .eq(CompetitionFormalExamSessionEntity::getStageId, batch.getStageId())
            .eq(CompetitionFormalExamSessionEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
            .eq(batch.getEventWorkTypeLevelId() != null,
                CompetitionFormalExamSessionEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
            .isNull(batch.getEventWorkTypeLevelId() == null,
                CompetitionFormalExamSessionEntity::getEventWorkTypeLevelId)
            .eq(CompetitionFormalExamSessionEntity::getDeleted, 0));
    Map<String, CompetitionFormalCandidateExamEntity> result = new HashMap<>();
    if (sessions == null) return result;
    for (CompetitionFormalExamSessionEntity session : sessions) {
      List<CompetitionFormalCandidateExamEntity> candidates = candidateMapper.selectByExamSessionId(session.getId());
      if (candidates == null) continue;
      for (CompetitionFormalCandidateExamEntity candidate : candidates) {
        if (candidate.getCandidateNo() != null && result.put(candidate.getCandidateNo(), candidate) != null) {
          throw new BusinessException(ErrorCode.CONFLICT, "当前范围内准考证号不唯一");
        }
      }
    }
    return result;
  }

  private LambdaQueryWrapper<CompetitionSimulationScoreImportBatchEntity> batchScope(Long tenantId,
      Long eventId, Long stageId, Long workTypeId, Long levelId, Long ruleId) {
    return new LambdaQueryWrapper<CompetitionSimulationScoreImportBatchEntity>()
        .eq(CompetitionSimulationScoreImportBatchEntity::getTenantId, tenantId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getEventId, eventId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getStageId, stageId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getEventWorkTypeId, workTypeId)
        .eq(levelId != null, CompetitionSimulationScoreImportBatchEntity::getEventWorkTypeLevelId, levelId)
        .isNull(levelId == null, CompetitionSimulationScoreImportBatchEntity::getEventWorkTypeLevelId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getScoreRuleId, ruleId)
        .eq(CompetitionSimulationScoreImportBatchEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionScoreSourceEntity> sourceScope(
      CompetitionSimulationScoreImportBatchEntity batch, Long memberId) {
    return new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
        .eq(CompetitionScoreSourceEntity::getEventId, batch.getEventId())
        .eq(CompetitionScoreSourceEntity::getStageId, batch.getStageId())
        .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(batch.getEventWorkTypeLevelId() != null,
            CompetitionScoreSourceEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .isNull(batch.getEventWorkTypeLevelId() == null, CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
        .eq(CompetitionScoreSourceEntity::getContestantMemberId, memberId)
        .eq(CompetitionScoreSourceEntity::getSourceType, "simulation")
        .eq(CompetitionScoreSourceEntity::getDeleted, 0);
  }

  private LambdaQueryWrapper<CompetitionRankingSnapshotEntity> rankingScope(
      CompetitionSimulationScoreImportBatchEntity batch) {
    return new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
        .eq(CompetitionRankingSnapshotEntity::getEventId, batch.getEventId())
        .eq(CompetitionRankingSnapshotEntity::getStageId, batch.getStageId())
        .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(batch.getEventWorkTypeLevelId() != null,
            CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .isNull(batch.getEventWorkTypeLevelId() == null, CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
        .eq(CompetitionRankingSnapshotEntity::getScoreRuleId, batch.getScoreRuleId())
        .eq(CompetitionRankingSnapshotEntity::getDeleted, 0);
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
    String value = text(formatter, cell);
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
        // Try the next documented spreadsheet format.
      }
    }
    throw new IllegalArgumentException("invalid achieved time");
  }
  private String nullToEmpty(String value) { return value == null ? "" : value; }
  private String sha256(byte[] bytes) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch (Exception ex) { throw new IllegalStateException(ex); }
  }
  private String abbreviate(String value) {
    if (value == null) return "Excel 解析失败";
    return value.length() <= 1000 ? value : value.substring(0, 1000);
  }
}
