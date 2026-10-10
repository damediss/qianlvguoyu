package com.guoyu.platform.certification.collection.domain;

import com.guoyu.platform.certification.collection.enums.CollectionOverallStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionQueueStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionReleaseScopeEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureStatusEnum;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PracticalCollectionDomainService {

  public boolean canJoinQueue(
      CollectionOverallStatusEnum overallStatus,
      CollectionQueueStatusEnum queueStatus
  ) {
    if (overallStatus == null) {
      return false;
    }
    if (overallStatus == CollectionOverallStatusEnum.COMPLETED
        || overallStatus == CollectionOverallStatusEnum.CANCELLED
        || overallStatus == CollectionOverallStatusEnum.INFO_ERROR) {
      return false;
    }
    if (queueStatus == CollectionQueueStatusEnum.QUEUED
        || queueStatus == CollectionQueueStatusEnum.COLLECTING) {
      return true;
    }
    return overallStatus == CollectionOverallStatusEnum.NOT_ARRIVED
        || overallStatus == CollectionOverallStatusEnum.ISSUED
        || overallStatus == CollectionOverallStatusEnum.ARRIVED
        || overallStatus == CollectionOverallStatusEnum.PENDING_RETRY;
  }

  public boolean canStartCollecting(
      CollectionOverallStatusEnum overallStatus,
      CollectionQueueStatusEnum queueStatus
  ) {
    return (overallStatus == CollectionOverallStatusEnum.QUEUED
        || overallStatus == CollectionOverallStatusEnum.ARRIVED
        || overallStatus == CollectionOverallStatusEnum.PENDING_RETRY)
        && queueStatus == CollectionQueueStatusEnum.QUEUED;
  }

  public boolean canSubmitFingerprint(CollectionOverallStatusEnum overallStatus) {
    return overallStatus == CollectionOverallStatusEnum.COLLECTING;
  }

  public boolean canSubmitSignature(CollectionOverallStatusEnum overallStatus) {
    return overallStatus == CollectionOverallStatusEnum.COLLECTING;
  }

  public boolean canComplete(
      FingerprintStatusEnum fingerprintStatus,
      SignatureStatusEnum signatureStatus
  ) {
    return (fingerprintStatus == FingerprintStatusEnum.DONE && signatureStatus == SignatureStatusEnum.DONE)
        || (fingerprintStatus == FingerprintStatusEnum.EXCEPTION_RELEASED
            && signatureStatus == SignatureStatusEnum.DONE)
        || (fingerprintStatus == FingerprintStatusEnum.DONE
            && signatureStatus == SignatureStatusEnum.EXCEPTION_RELEASED);
  }

  public void assertCanJoinQueue(String overallStatusCode, String queueStatusCode) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    CollectionQueueStatusEnum queueStatus = queueStatus(queueStatusCode);
    if (!canJoinQueue(overallStatus, queueStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许加入采集队列");
    }
  }

  public void assertCanStartCollecting(String overallStatusCode, String queueStatusCode) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    CollectionQueueStatusEnum queueStatus = queueStatus(queueStatusCode);
    if (!canStartCollecting(overallStatus, queueStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许开始采集");
    }
  }

  public void assertCanSubmitFingerprint(String overallStatusCode) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    if (!canSubmitFingerprint(overallStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有采集中考生可以提交指纹记录");
    }
  }

  public void assertCanSubmitSignature(String overallStatusCode) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    if (!canSubmitSignature(overallStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有采集中考生可以提交签名记录");
    }
  }

  public void assertCanComplete(String fingerprintStatusCode, String signatureStatusCode) {
    FingerprintStatusEnum fingerprintStatus = fingerprintStatus(fingerprintStatusCode);
    SignatureStatusEnum signatureStatus = signatureStatus(signatureStatusCode);
    if (!canComplete(fingerprintStatus, signatureStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "指纹和签名未满足完成采集条件");
    }
  }

  public void assertCanExceptionRelease(String overallStatusCode, String releaseScopeCode, String reason) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    if (overallStatus == CollectionOverallStatusEnum.CANCELLED) {
      throw new BusinessException(ErrorCode.CONFLICT, "已取消的考生不能免考放行");
    }
    if (!StringUtils.hasText(reason)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "免考放行原因不能为空");
    }
    if (CollectionReleaseScopeEnum.fromCode(releaseScopeCode) == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "免考放行范围不正确");
    }
  }

  public void assertNotCompleted(String overallStatusCode, String actionName) {
    CollectionOverallStatusEnum overallStatus = overallStatus(overallStatusCode);
    if (overallStatus == CollectionOverallStatusEnum.COMPLETED
        || overallStatus == CollectionOverallStatusEnum.CANCELLED) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成或已取消的考生不能" + actionName);
    }
  }

  private CollectionOverallStatusEnum overallStatus(String code) {
    CollectionOverallStatusEnum status = CollectionOverallStatusEnum.fromCode(code);
    if (status == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知采集状态: " + code);
    }
    return status;
  }

  private CollectionQueueStatusEnum queueStatus(String code) {
    CollectionQueueStatusEnum status = CollectionQueueStatusEnum.fromCode(code);
    if (status == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知队列状态: " + code);
    }
    return status;
  }

  private FingerprintStatusEnum fingerprintStatus(String code) {
    FingerprintStatusEnum status = FingerprintStatusEnum.fromCode(code);
    if (status == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知指纹状态: " + code);
    }
    return status;
  }

  private SignatureStatusEnum signatureStatus(String code) {
    SignatureStatusEnum status = SignatureStatusEnum.fromCode(code);
    if (status == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知签名状态: " + code);
    }
    return status;
  }
}
