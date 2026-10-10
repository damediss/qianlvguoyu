package com.guoyu.platform.certification.defense;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseReportEntity;
import com.guoyu.platform.certification.entity.CertificationDefenseScoreEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.mapper.CertificationDefenseScoreMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DefenseScoreSheetApplicationService {

  private final DefenseScoringApplicationService reports;
  private final CertificationDefenseScoreMapper scoreMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;

  public DefenseScoreSheetApplicationService(
      DefenseScoringApplicationService reports,
      CertificationDefenseScoreMapper scoreMapper,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper
  ) {
    this.reports = reports;
    this.scoreMapper = scoreMapper;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public DefenseScoringModels.ScoreSheetsView scoreSheets(Long reportId) {
    CertificationDefenseReportEntity report = reports.requireVisibleReport(reportId);
    CertificationInfoAssessorEntity assessor = reports.currentAssessor();
    CertificationTaskEntity task = reports.requireTask(report.getTaskId());
    CertificationCandidateEntity candidate = reports.requireCandidate(report.getTaskId(), report.getCandidateId());
    CertificationDefenseScoreEntity thesis = loadOwnScore(assessor.getId(), report, DefenseScoreCatalog.SHEET_THESIS);
    CertificationDefenseScoreEntity performance = loadOwnScore(
        assessor.getId(), report, DefenseScoreCatalog.SHEET_PERFORMANCE);
    return new DefenseScoringModels.ScoreSheetsView(
        reports.toDetail(report, task, candidate, thesis, performance),
        toView(DefenseScoreCatalog.SHEET_THESIS, thesis, assessor.getAssessorName()),
        toView(DefenseScoreCatalog.SHEET_PERFORMANCE, performance, assessor.getAssessorName())
    );
  }

  @Transactional
  public DefenseScoringModels.ScoreSheetView save(
      Long reportId,
      String sheetType,
      DefenseScoringModels.SaveScoreSheetReq req
  ) {
    return persist(reportId, sheetType, req, false);
  }

  @Transactional
  public DefenseScoringModels.ScoreSheetView submit(
      Long reportId,
      String sheetType,
      DefenseScoringModels.SubmitScoreSheetReq req
  ) {
    DefenseScoringModels.SaveScoreSheetReq saveReq = new DefenseScoringModels.SaveScoreSheetReq(
        req.thesis(),
        req.performance(),
        req.signatureFileId(),
        req.signatureHash()
    );
    return persist(reportId, sheetType, saveReq, true);
  }

  private DefenseScoringModels.ScoreSheetView persist(
      Long reportId,
      String rawSheetType,
      DefenseScoringModels.SaveScoreSheetReq req,
      boolean submit
  ) {
    String sheetType = normalizeSheetType(rawSheetType);
    CertificationDefenseReportEntity report = reports.requireVisibleReport(reportId);
    CertificationInfoAssessorEntity assessor = reports.currentAssessor();
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationDefenseScoreEntity entity = loadOwnScore(assessor.getId(), report, sheetType);
    if (entity != null && DefenseScoreCatalog.STATUS_SUBMITTED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该评分表已提交，不能再修改");
    }
    DefenseScoringModels.ScoreComputation computation = compute(sheetType, req, submit);
    Long signatureFileId = req == null ? null : req.signatureFileId();
    String signatureHash = req == null ? null : req.signatureHash();
    if (submit) {
      requireSignature(user.getTenantId(), signatureFileId, signatureHash);
    } else if (signatureFileId != null) {
      requireSignature(user.getTenantId(), signatureFileId, signatureHash);
    }
    if (entity == null) {
      entity = new CertificationDefenseScoreEntity();
      entity.setTenantId(user.getTenantId());
      entity.setTaskId(report.getTaskId());
      entity.setCandidateId(report.getCandidateId());
      entity.setReportId(report.getId());
      entity.setAssessorId(assessor.getId());
      entity.setAssessorUserId(user.getUserId());
      entity.setSheetType(sheetType);
      entity.setDeleted(0);
    }
    entity.setPayloadJson(writePayload(sheetType, req));
    entity.setContentSubtotal(computation.contentSubtotal());
    entity.setDefenseSubtotal(computation.defenseSubtotal());
    entity.setRawTotal(computation.rawTotal());
    entity.setFinalScore(computation.finalScore());
    entity.setRejected(computation.rejected() ? 1 : 0);
    if (signatureFileId != null) {
      entity.setSignatureFileId(signatureFileId);
      entity.setSignatureHash(signatureHash);
    }
    if (submit) {
      entity.setStatus(DefenseScoreCatalog.STATUS_SUBMITTED);
      entity.setSubmittedAt(LocalDateTime.now());
      log.info(
          "综合答辩评分已提交: taskId={}, candidateId={}, sheetType={}, assessorId={}, finalScore={}",
          report.getTaskId(), report.getCandidateId(), sheetType, assessor.getId(), computation.finalScore()
      );
    } else {
      entity.setStatus(DefenseScoreCatalog.STATUS_DRAFT);
      log.info(
          "综合答辩评分已保存草稿: taskId={}, candidateId={}, sheetType={}, assessorId={}",
          report.getTaskId(), report.getCandidateId(), sheetType, assessor.getId()
      );
    }
    if (entity.getId() == null) {
      scoreMapper.insert(entity);
    } else {
      scoreMapper.updateById(entity);
    }
    return toView(sheetType, entity, assessor.getAssessorName());
  }

  private DefenseScoringModels.ScoreComputation compute(
      String sheetType,
      DefenseScoringModels.SaveScoreSheetReq req,
      boolean submit
  ) {
    if (DefenseScoreCatalog.SHEET_THESIS.equals(sheetType)) {
      return DefenseScoreCatalog.computeThesis(req == null ? null : req.thesis(), submit);
    }
    return DefenseScoreCatalog.computePerformance(req == null ? null : req.performance(), submit);
  }

  private String writePayload(String sheetType, DefenseScoringModels.SaveScoreSheetReq req) {
    try {
      if (DefenseScoreCatalog.SHEET_THESIS.equals(sheetType)) {
        return objectMapper.writeValueAsString(DefenseScoreCatalog.normalizeThesis(req == null ? null : req.thesis()));
      }
      return objectMapper.writeValueAsString(
          DefenseScoreCatalog.normalizePerformance(req == null ? null : req.performance()));
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分表内容无法保存");
    }
  }

  private DefenseScoringModels.ScoreSheetView toView(
      String sheetType,
      CertificationDefenseScoreEntity entity,
      String assessorName
  ) {
    if (entity == null) {
      return emptyView(sheetType, assessorName);
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
        entity.getStatus(),
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

  private DefenseScoringModels.ScoreSheetView emptyView(String sheetType, String assessorName) {
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

  private DefenseScoringModels.ThesisPayload readThesis(String json) {
    if (!StringUtils.hasText(json)) {
      return DefenseScoreCatalog.emptyThesis();
    }
    try {
      return DefenseScoreCatalog.normalizeThesis(objectMapper.readValue(json, DefenseScoringModels.ThesisPayload.class));
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "论文答辩评分表内容损坏");
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
      throw new BusinessException(ErrorCode.BAD_REQUEST, "业绩评估报告评分表内容损坏");
    }
  }

  private void requireSignature(Long tenantId, Long signatureFileId, String signatureHash) {
    if (signatureFileId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成评审专家电子签名");
    }
    FileEntity file = fileApplicationService.getExistingFile(tenantId, signatureFileId);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    String name = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase(Locale.ROOT);
    boolean image = contentType.startsWith("image/")
        || name.endsWith(".png")
        || name.endsWith(".jpg")
        || name.endsWith(".jpeg");
    if (!image) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "电子签名必须为图片文件");
    }
    if (StringUtils.hasText(file.getSha256())
        && StringUtils.hasText(signatureHash)
        && !file.getSha256().equalsIgnoreCase(signatureHash)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "签名文件 hash 不一致");
    }
  }

  private CertificationDefenseScoreEntity loadOwnScore(
      Long assessorId,
      CertificationDefenseReportEntity report,
      String sheetType
  ) {
    return scoreMapper.selectOne(new LambdaQueryWrapper<CertificationDefenseScoreEntity>()
        .eq(CertificationDefenseScoreEntity::getAssessorId, assessorId)
        .eq(CertificationDefenseScoreEntity::getTaskId, report.getTaskId())
        .eq(CertificationDefenseScoreEntity::getCandidateId, report.getCandidateId())
        .eq(CertificationDefenseScoreEntity::getSheetType, sheetType)
        .last("LIMIT 1"));
  }

  private static String normalizeSheetType(String sheetType) {
    String normalized = sheetType == null ? "" : sheetType.trim().toUpperCase(Locale.ROOT);
    if ("PAPER".equals(normalized) || "THESIS_DEFENSE".equals(normalized)) {
      normalized = DefenseScoreCatalog.SHEET_THESIS;
    }
    if ("REPORT".equals(normalized) || "PERFORMANCE_REPORT".equals(normalized)) {
      normalized = DefenseScoreCatalog.SHEET_PERFORMANCE;
    }
    DefenseScoreCatalog.requireSheetType(normalized);
    return normalized;
  }
}
