package com.guoyu.platform.certification.collection.export.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.collection.domain.CollectionEligibilityResult;
import com.guoyu.platform.certification.collection.enums.CollectionOverallStatusEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintCollectStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureRecordStatusEnum;
import com.guoyu.platform.certification.collection.export.dto.CollectionExportPdfDTO;
import com.guoyu.platform.certification.collection.export.vo.CollectionPdfExportVO;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.entity.PracticalFingerprintTraceEntity;
import com.guoyu.platform.certification.entity.PracticalSignatureRecordEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionStatusMapper;
import com.guoyu.platform.certification.mapper.PracticalFingerprintTraceMapper;
import com.guoyu.platform.certification.mapper.PracticalSignatureRecordMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 实操信息采集表 PDF 用例：查询采集数据、生成固定模板并上传文件服务。 */
@Service
@Slf4j
public class CollectionPdfExportService {

  private static final DateTimeFormatter FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationTaskExamSiteMapper taskExamSiteMapper;
  private final CertificationTaskScopeMapper taskScopeMapper;
  private final PracticalCollectionStatusMapper statusMapper;
  private final PracticalFingerprintTraceMapper fingerprintTraceMapper;
  private final PracticalSignatureRecordMapper signatureRecordMapper;
  private final com.guoyu.platform.certification.collection.domain.PracticalCollectionEligibilityService eligibilityService;
  private final FileApplicationService fileApplicationService;
  private final String fontPath;

  public CollectionPdfExportService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationTaskExamSiteMapper taskExamSiteMapper,
      CertificationTaskScopeMapper taskScopeMapper,
      PracticalCollectionStatusMapper statusMapper,
      PracticalFingerprintTraceMapper fingerprintTraceMapper,
      PracticalSignatureRecordMapper signatureRecordMapper,
      com.guoyu.platform.certification.collection.domain.PracticalCollectionEligibilityService eligibilityService,
      FileApplicationService fileApplicationService,
      @Value("${platform.certification.collection.pdf-font-path:}") String fontPath
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.taskExamSiteMapper = taskExamSiteMapper;
    this.taskScopeMapper = taskScopeMapper;
    this.statusMapper = statusMapper;
    this.fingerprintTraceMapper = fingerprintTraceMapper;
    this.signatureRecordMapper = signatureRecordMapper;
    this.eligibilityService = eligibilityService;
    this.fileApplicationService = fileApplicationService;
    this.fontPath = fontPath;
  }

  @Transactional
  public CollectionPdfExportVO export(CollectionExportPdfDTO request) {
    if (request == null || request.examTaskId() == null || request.candidateIds() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务和考生不能为空");
    }
    List<Long> candidateIds = request.candidateIds().stream()
        .filter(Objects::nonNull).distinct().toList();
    if (candidateIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择需要生成打印版 PDF 的考生");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationTaskEntity task = requireTask(tenantId, request.examTaskId());
    List<CertificationCandidateEntity> candidates = safeList(candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, request.examTaskId())
            .in(CertificationCandidateEntity::getId, candidateIds)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .orderByAsc(CertificationCandidateEntity::getId)));
    Map<Long, CertificationCandidateEntity> candidateMap = candidates.stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity(), (left, right) -> left));
    Map<Long, CollectionEligibilityResult> eligibilityMap = eligibilityService.checkCandidates(tenantId, request.examTaskId(), candidates);
    Map<Long, PracticalCollectionStatusEntity> statusMap = safeList(statusMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, request.examTaskId())
            .in(PracticalCollectionStatusEntity::getCandidateId, candidateIds)
            .eq(PracticalCollectionStatusEntity::getDeleted, 0)))
        .stream().collect(Collectors.toMap(PracticalCollectionStatusEntity::getCandidateId, Function.identity(), (left, right) -> left));

    List<String> errors = new ArrayList<>();
    List<CollectionPdfTemplate.PdfCandidate> pdfCandidates = new ArrayList<>();
    for (Long candidateId : candidateIds) {
      CertificationCandidateEntity candidate = candidateMap.get(candidateId);
      if (candidate == null) {
        errors.add("考生ID" + candidateId + "：不属于当前任务或已被删除");
        continue;
      }
      CollectionEligibilityResult eligibility = eligibilityMap.get(candidateId);
      if (eligibility == null || !eligibility.eligible()) {
        errors.add(candidateLabel(candidate) + "：" + (eligibility == null ? "资料状态无法确认" : eligibility.reasonMessage()));
        continue;
      }
      PracticalCollectionStatusEntity status = statusMap.get(candidateId);
      if (status == null || !CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
        errors.add(candidateLabel(candidate) + "：实操信息采集尚未完成");
        continue;
      }
      PracticalFingerprintTraceEntity fingerprint = latestFingerprintTrace(tenantId, request.examTaskId(), candidateId);
      PracticalSignatureRecordEntity signature = latestSignatureRecord(tenantId, request.examTaskId(), candidateId);
      byte[] fingerprintImage = fingerprint == null || !FingerprintCollectStatusEnum.SUCCESS.getCode().equals(fingerprint.getCollectStatus())
          ? null : readImageSafely(tenantId, fingerprint.getFingerprintImageFileId(), "指纹图片", candidate);
      byte[] signatureImage = signature == null ? null
          : readImageSafely(tenantId, signature.getSignatureFileId(), "电子签名图片", candidate);
      byte[] avatar = readImageSafely(tenantId, eligibility.avatarFileId(), "证件照", candidate);
      // 当前采集模型分别保存最近一次指纹/签名；无签到与交卷独立字段时，四个固定槽位复用当前记录。
      pdfCandidates.add(new CollectionPdfTemplate.PdfCandidate(
          candidate.getCandidateName(), candidate.getIdCardNo(), candidate.getAdmissionTicketNo(),
          candidate.getWorkTypeName(), candidate.getLevelName(), status.getOverallStatus(),
          avatar, fingerprintImage, signatureImage, fingerprintImage, signatureImage));
    }
    if (!errors.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "本次选择" + candidateIds.size() + "名考生，其中" + errors.size()
              + "名不满足打印版 PDF 生成条件：\n" + String.join("\n", errors) + "\n请补充资料后重新生成。");
    }

    CertificationTaskExamSiteEntity site = taskExamSiteMapper.selectOne(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
        .eq(CertificationTaskExamSiteEntity::getTenantId, tenantId)
        .eq(CertificationTaskExamSiteEntity::getTaskId, request.examTaskId())
        .eq(CertificationTaskExamSiteEntity::getDeleted, 0)
        .orderByAsc(CertificationTaskExamSiteEntity::getSortOrder).last("LIMIT 1"));
    CertificationTaskScopeEntity scope = taskScopeMapper.selectOne(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTenantId, tenantId)
        .eq(CertificationTaskScopeEntity::getTaskId, request.examTaskId())
        .eq(CertificationTaskScopeEntity::getDeleted, 0)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder).last("LIMIT 1"));
    String examDate = site != null && site.getExamDate() != null ? site.getExamDate().toString()
        : task.getExamDate() == null ? "-" : task.getExamDate().toString();
    String examTime = examDate + (site == null ? "" : " " + nullToEmpty(site.getStartTime())
        + (StringUtils.hasText(site.getEndTime()) ? "-" + site.getEndTime() : ""));
    String workType = scope == null ? "" : nullToEmpty(scope.getWorkTypeName());
    String level = scope == null ? "" : nullToEmpty(scope.getLevelName());
    String siteName = site == null ? "" : nullToEmpty(site.getSiteName());
    String address = site == null ? "" : nullToEmpty(site.getAddress());
    String examSite = siteName + (StringUtils.hasText(address) ? "（" + address + "）" : "");
    if (!StringUtils.hasText(workType) || !StringUtils.hasText(level)) {
      log.debug("PDF export scope is incomplete: taskId={}, workType={}, level={}", request.examTaskId(), workType, level);
    }
    byte[] pdf;
    try {
      pdf = CollectionPdfTemplate.render(
          new CollectionPdfTemplate.PdfTask(nullToEmpty(task.getTaskName()), examTime, examSite,
              nullToEmpty(workType) + " / " + nullToEmpty(level)),
          pdfCandidates, fontPath);
    } catch (IOException | RuntimeException ex) {
      log.error("Practical collection PDF export failed: taskId={}", request.examTaskId(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "打印版 PDF 生成失败，请检查中文字体配置");
    }
    String fileName = "实操采集打印版_" + safeFilename(task.getTaskName()) + "_"
        + LocalDateTime.now().format(FILE_TS_FORMAT) + ".pdf";
    FileVO file = fileApplicationService.storeFromBytes(pdf, fileName, "application/pdf", "private");
    return new CollectionPdfExportVO(file.id(), file.originalName(), "/api/files/" + file.id() + "/download");
  }

  private byte[] readImageSafely(Long tenantId, Long fileId, String label, CertificationCandidateEntity candidate) {
    if (fileId == null) {
      return null;
    }
    try {
      fileApplicationService.getExistingFile(tenantId, fileId);
      byte[] bytes = fileApplicationService.readBytes(fileId);
      if (bytes == null || bytes.length == 0
          || ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
        log.warn("PDF export image is empty or not an image: candidateId={}, label={}, fileId={}",
            candidate.getId(), label, fileId);
        return null;
      }
      return bytes;
    } catch (BusinessException | IOException ex) {
      log.warn("PDF export image is not readable: candidateId={}, label={}, fileId={}",
          candidate.getId(), label, fileId, ex);
      return null;
    }
  }

  private CertificationTaskEntity requireTask(Long tenantId, Long taskId) {
    CertificationTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getId, taskId)
        .eq(CertificationTaskEntity::getDeleted, 0));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private PracticalFingerprintTraceEntity latestFingerprintTrace(Long tenantId, Long taskId, Long candidateId) {
    return fingerprintTraceMapper.selectOne(new LambdaQueryWrapper<PracticalFingerprintTraceEntity>()
        .eq(PracticalFingerprintTraceEntity::getTenantId, tenantId)
        .eq(PracticalFingerprintTraceEntity::getTaskId, taskId)
        .eq(PracticalFingerprintTraceEntity::getCandidateId, candidateId)
        .eq(PracticalFingerprintTraceEntity::getDeleted, 0)
        .orderByDesc(PracticalFingerprintTraceEntity::getCollectedAt)
        .orderByDesc(PracticalFingerprintTraceEntity::getCreatedAt).last("LIMIT 1"));
  }

  private PracticalSignatureRecordEntity latestSignatureRecord(Long tenantId, Long taskId, Long candidateId) {
    return signatureRecordMapper.selectOne(new LambdaQueryWrapper<PracticalSignatureRecordEntity>()
        .eq(PracticalSignatureRecordEntity::getTenantId, tenantId)
        .eq(PracticalSignatureRecordEntity::getTaskId, taskId)
        .eq(PracticalSignatureRecordEntity::getCandidateId, candidateId)
        .eq(PracticalSignatureRecordEntity::getRecordStatus, SignatureRecordStatusEnum.ACTIVE.getCode())
        .eq(PracticalSignatureRecordEntity::getDeleted, 0)
        .orderByDesc(PracticalSignatureRecordEntity::getSignedAt)
        .orderByDesc(PracticalSignatureRecordEntity::getCreatedAt).last("LIMIT 1"));
  }

  private <T> List<T> safeList(List<T> value) {
    return value == null ? Collections.emptyList() : value;
  }

  private String candidateLabel(CertificationCandidateEntity candidate) {
    return nullToEmpty(candidate.getCandidateName()) + "，准考证号" + nullToEmpty(candidate.getAdmissionTicketNo());
  }

  private String safeFilename(String value) {
    return StringUtils.hasText(value) ? value.replaceAll("[\\\\/:*?\"<>|]", "_") : "任务";
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
