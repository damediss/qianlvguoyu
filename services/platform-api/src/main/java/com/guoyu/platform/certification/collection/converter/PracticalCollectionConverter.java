package com.guoyu.platform.certification.collection.converter;

import com.guoyu.platform.certification.collection.domain.CollectionEligibilityResult;
import com.guoyu.platform.certification.collection.enums.CollectionExceptionStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionExceptionTypeEnum;
import com.guoyu.platform.certification.collection.enums.CollectionMatchMethodEnum;
import com.guoyu.platform.certification.collection.enums.CollectionOverallStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionQueueStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionReleaseScopeEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintCollectStatusEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintStatusEnum;
import com.guoyu.platform.certification.collection.enums.QrIssueStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureRecordStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignerTypeEnum;
import com.guoyu.platform.certification.collection.vo.CandidateCollectionDetailVO;
import com.guoyu.platform.certification.collection.vo.CandidateCollectionItemVO;
import com.guoyu.platform.certification.collection.vo.CandidateMatchVO;
import com.guoyu.platform.certification.collection.vo.CollectionActionResultVO;
import com.guoyu.platform.certification.collection.vo.CollectionExceptionVO;
import com.guoyu.platform.certification.collection.vo.CollectionStatsVO;
import com.guoyu.platform.certification.collection.vo.CollectionTaskVO;
import com.guoyu.platform.certification.collection.vo.FingerprintTraceVO;
import com.guoyu.platform.certification.collection.vo.QueueItemVO;
import com.guoyu.platform.certification.collection.vo.SignatureRecordVO;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionExceptionEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionQueueEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.entity.PracticalFingerprintTraceEntity;
import com.guoyu.platform.certification.entity.PracticalSignatureRecordEntity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PracticalCollectionConverter {

  public CollectionTaskVO toTaskVO(CertificationTaskEntity task, CollectionStatsVO stats) {
    if (task == null) {
      return null;
    }
    CollectionStatsVO safeStats = stats == null ? emptyStats(task.getPlannedPeopleCount()) : stats;
    return new CollectionTaskVO(
        task.getId(),
        task.getTaskNo(),
        task.getTaskName(),
        task.getCompanyName(),
        null,
        null,
        task.getExamDate(),
        safeStats.candidateTotal(),
        safeStats.qrIssuedCount(),
        safeStats.arrivedCount(),
        safeStats.queuedCount(),
        safeStats.collectingCount(),
        safeStats.completedCount(),
        safeStats.pendingRetryCount(),
        safeStats.exceptionCount()
    );
  }

  public CandidateMatchVO toCandidateMatchVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      Long avatarFileId,
      Boolean alreadyQueued,
      String matchMethod
  ) {
    return toCandidateMatchVO(candidate, status, avatarFileId, alreadyQueued, matchMethod, null);
  }

  public CandidateMatchVO toCandidateMatchVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      Long avatarFileId,
      Boolean alreadyQueued,
      String matchMethod,
      CollectionEligibilityResult eligibility
  ) {
    if (candidate == null) {
      return null;
    }
    String overallStatus = status == null ? null : status.getOverallStatus();
    String qrIssueStatus = status == null ? null : status.getQrIssueStatus();
    return new CandidateMatchVO(
        candidate.getId(),
        candidate.getTaskId(),
        candidate.getCandidateName(),
        idCardLast4(candidate, status),
        candidate.getMaskedIdCardNo(),
        candidate.getOrgName(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        candidate.getAdmissionTicketNo(),
        avatarFileId,
        overallStatus,
        overallLabel(overallStatus),
        Boolean.TRUE.equals(alreadyQueued),
        status == null ? null : status.getQueueNo(),
        qrIssueStatus,
        qrIssueStatusLabel(qrIssueStatus),
        matchMethod,
        matchMethodLabel(matchMethod),
        eligibility == null ? null : eligibility.documentStatus(),
        eligibility == null ? List.of() : eligibility.missingDocumentCodes(),
        eligibility == null ? List.of() : eligibility.invalidDocumentCodes(),
        eligibility == null ? null : eligibility.eligible()
    );
  }

  public CandidateCollectionItemVO toCandidateItemVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    return toCandidateItemVO(candidate, status, null);
  }

  public CandidateCollectionItemVO toCandidateItemVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      CollectionEligibilityResult eligibility
  ) {
    if (candidate == null) {
      return null;
    }
    return new CandidateCollectionItemVO(
        candidate.getId(),
        candidate.getTaskId(),
        candidate.getCandidateName(),
        idCardLast4(candidate, status),
        candidate.getMaskedIdCardNo(),
        candidate.getOrgName(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        candidate.getAdmissionTicketNo(),
        status == null ? null : status.getOverallStatus(),
        overallLabel(status == null ? null : status.getOverallStatus()),
        status == null ? null : status.getQueueStatus(),
        queueStatusLabel(status == null ? null : status.getQueueStatus()),
        status == null ? null : status.getFingerprintStatus(),
        fingerprintStatusLabel(status == null ? null : status.getFingerprintStatus()),
        status == null ? null : status.getSignatureStatus(),
        signatureStatusLabel(status == null ? null : status.getSignatureStatus()),
        status == null ? null : status.getExceptionStatus(),
        exceptionStatusLabel(status == null ? null : status.getExceptionStatus()),
        status == null ? null : status.getQueueNo(),
        eligibility == null ? null : eligibility.documentStatus(),
        eligibility == null ? List.of() : eligibility.missingDocumentCodes(),
        eligibility == null ? List.of() : eligibility.invalidDocumentCodes(),
        eligibility == null ? null : eligibility.eligible()
    );
  }

  public CandidateCollectionDetailVO toCandidateDetailVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      Long avatarFileId
  ) {
    return toCandidateDetailVO(candidate, status, avatarFileId, null, null, null, List.of());
  }

  public CandidateCollectionDetailVO toCandidateDetailVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      Long avatarFileId,
      QueueItemVO currentQueue,
      FingerprintTraceVO latestFingerprint,
      SignatureRecordVO latestSignature,
      List<CollectionExceptionVO> exceptions
  ) {
    return toCandidateDetailVO(candidate, status, avatarFileId, currentQueue, latestFingerprint, latestSignature,
        exceptions, null);
  }

  public CandidateCollectionDetailVO toCandidateDetailVO(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      Long avatarFileId,
      QueueItemVO currentQueue,
      FingerprintTraceVO latestFingerprint,
      SignatureRecordVO latestSignature,
      List<CollectionExceptionVO> exceptions,
      CollectionEligibilityResult eligibility
  ) {
    if (candidate == null || status == null) {
      return null;
    }
    return new CandidateCollectionDetailVO(
        status.getId(),
        candidate.getId(),
        candidate.getTaskId(),
        status.getSessionId(),
        candidate.getCandidateName(),
        idCardLast4(candidate, status),
        candidate.getMaskedIdCardNo(),
        candidate.getOrgName(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        candidate.getAdmissionTicketNo(),
        avatarFileId,
        StringUtils.hasText(status.getCollectToken()),
        status.getQrIssueStatus(),
        qrIssueStatusLabel(status.getQrIssueStatus()),
        status.getOverallStatus(),
        overallLabel(status.getOverallStatus()),
        status.getQueueStatus(),
        queueStatusLabel(status.getQueueStatus()),
        status.getFingerprintStatus(),
        fingerprintStatusLabel(status.getFingerprintStatus()),
        status.getSignatureStatus(),
        signatureStatusLabel(status.getSignatureStatus()),
        status.getExceptionStatus(),
        exceptionStatusLabel(status.getExceptionStatus()),
        status.getQueueNo(),
        status.getArrivedAt(),
        status.getCollectingStartedAt(),
        status.getCompletedAt(),
        currentQueue,
        latestFingerprint,
        latestSignature,
        exceptions == null ? List.of() : exceptions,
        eligibility == null ? null : eligibility.documentStatus(),
        eligibility == null ? List.of() : eligibility.missingDocumentCodes(),
        eligibility == null ? List.of() : eligibility.invalidDocumentCodes(),
        eligibility == null ? null : eligibility.eligible()
    );
  }

  public QueueItemVO toQueueItemVO(
      PracticalCollectionQueueEntity queue,
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    if (queue == null) {
      return null;
    }
    return new QueueItemVO(
        queue.getId(),
        queue.getTaskId(),
        queue.getCandidateId(),
        queue.getQueueNo(),
        candidate == null ? null : candidate.getCandidateName(),
        idCardLast4(candidate, status),
        candidate == null ? null : candidate.getOrgName(),
        status == null ? null : status.getOverallStatus(),
        overallLabel(status == null ? null : status.getOverallStatus()),
        queue.getQueueStatus(),
        queueStatusLabel(queue.getQueueStatus()),
        queue.getMatchMethod(),
        matchMethodLabel(queue.getMatchMethod()),
        queue.getJoinedAt(),
        queue.getStartedAt()
    );
  }

  public FingerprintTraceVO toFingerprintTraceVO(PracticalFingerprintTraceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new FingerprintTraceVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getCandidateId(),
        entity.getSessionId(),
        entity.getDeviceSn(),
        entity.getDeviceModel(),
        entity.getDeviceVendorId(),
        entity.getDeviceProductId(),
        entity.getSdkDeviceType(),
        entity.getAdapterType(),
        entity.getCollectStatus(),
        fingerprintCollectStatusLabel(entity.getCollectStatus()),
        entity.getQualityScore(),
        entity.getDurationMs(),
        entity.getErrorCode(),
        entity.getExceptionReason(),
        entity.getCollectedAt(),
        entity.getFingerprintImageFileId(),
        entity.getFingerprintImageHash(),
        entity.getFingerprintImageFormat(),
        entity.getFingerprintImageCapturedAt()
    );
  }

  public SignatureRecordVO toSignatureRecordVO(PracticalSignatureRecordEntity entity) {
    if (entity == null) {
      return null;
    }
    return new SignatureRecordVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getCandidateId(),
        entity.getSessionId(),
        entity.getSignatureFileId(),
        entity.getSignatureHash(),
        entity.getSignerType(),
        signerTypeLabel(entity.getSignerType()),
        entity.getSignerName(),
        entity.getSignedAt(),
        entity.getRecordStatus(),
        signatureRecordStatusLabel(entity.getRecordStatus())
    );
  }

  public CollectionExceptionVO toExceptionVO(
      PracticalCollectionExceptionEntity entity,
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    if (entity == null) {
      return null;
    }
    return new CollectionExceptionVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getCandidateId(),
        candidate == null ? null : candidate.getCandidateName(),
        idCardLast4(candidate, status),
        entity.getExceptionType(),
        exceptionTypeLabel(entity.getExceptionType()),
        entity.getExceptionStatus(),
        exceptionStatusLabel(entity.getExceptionStatus()),
        entity.getReleaseScope(),
        releaseScopeLabel(entity.getReleaseScope()),
        entity.getReason(),
        entity.getDeviceSn(),
        entity.getDeviceStatus(),
        entity.getHandledBy(),
        entity.getHandledAt(),
        entity.getCreatedAt()
    );
  }

  public CollectionActionResultVO toActionResultVO(
      PracticalCollectionStatusEntity status,
      QueueItemVO queueItem,
      String message
  ) {
    if (status == null) {
      return null;
    }
    return new CollectionActionResultVO(
        status.getTaskId(),
        status.getCandidateId(),
        status.getOverallStatus(),
        overallLabel(status.getOverallStatus()),
        status.getQueueStatus(),
        queueStatusLabel(status.getQueueStatus()),
        status.getFingerprintStatus(),
        fingerprintStatusLabel(status.getFingerprintStatus()),
        status.getSignatureStatus(),
        signatureStatusLabel(status.getSignatureStatus()),
        status.getExceptionStatus(),
        exceptionStatusLabel(status.getExceptionStatus()),
        queueItem,
        message
    );
  }

  private CollectionStatsVO emptyStats(Integer candidateTotal) {
    return new CollectionStatsVO(candidateTotal == null ? 0 : candidateTotal, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  private String idCardLast4(CertificationCandidateEntity candidate, PracticalCollectionStatusEntity status) {
    if (status != null && StringUtils.hasText(status.getIdCardLast4())) {
      return status.getIdCardLast4();
    }
    if (candidate != null && StringUtils.hasText(candidate.getIdCardNo())
        && candidate.getIdCardNo().length() >= 4) {
      String idCardNo = candidate.getIdCardNo();
      return idCardNo.substring(idCardNo.length() - 4);
    }
    return null;
  }

  private String overallLabel(String code) {
    CollectionOverallStatusEnum item = CollectionOverallStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String qrIssueStatusLabel(String code) {
    QrIssueStatusEnum item = QrIssueStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String queueStatusLabel(String code) {
    CollectionQueueStatusEnum item = CollectionQueueStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String fingerprintStatusLabel(String code) {
    FingerprintStatusEnum item = FingerprintStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String signatureStatusLabel(String code) {
    SignatureStatusEnum item = SignatureStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String exceptionStatusLabel(String code) {
    CollectionExceptionStatusEnum item = CollectionExceptionStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String exceptionTypeLabel(String code) {
    CollectionExceptionTypeEnum item = CollectionExceptionTypeEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String releaseScopeLabel(String code) {
    CollectionReleaseScopeEnum item = CollectionReleaseScopeEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String matchMethodLabel(String code) {
    CollectionMatchMethodEnum item = CollectionMatchMethodEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String fingerprintCollectStatusLabel(String code) {
    FingerprintCollectStatusEnum item = FingerprintCollectStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String signatureRecordStatusLabel(String code) {
    SignatureRecordStatusEnum item = SignatureRecordStatusEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }

  private String signerTypeLabel(String code) {
    SignerTypeEnum item = SignerTypeEnum.fromCode(code);
    return item == null ? code : item.getLabel();
  }
}
