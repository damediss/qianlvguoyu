package com.guoyu.platform.certification.collection.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Single source of truth for whether a candidate may enter practical collection.
 * Metadata checks deliberately do not download file content; strict export performs that check.
 */
@Slf4j
@Service
public class PracticalCollectionEligibilityService {

  public static final List<String> REQUIRED_DOCUMENT_CODES = List.of("DOC001", "DOC002", "DOC003", "DOC004");

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationCandidateDocumentMapper documentMapper;
  private final FileMapper fileMapper;

  public PracticalCollectionEligibilityService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationCandidateDocumentMapper documentMapper,
      FileMapper fileMapper
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.documentMapper = documentMapper;
    this.fileMapper = fileMapper;
  }

  public CollectionEligibilityResult check(Long tenantId, Long taskId, Long candidateId) {
    if (tenantId == null || taskId == null || candidateId == null) {
      return invalid(candidateId, "CANDIDATE_NOT_FOUND", "考生不属于当前认定任务");
    }
    CertificationTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getId, taskId)
        .eq(CertificationTaskEntity::getDeleted, 0));
    CertificationCandidateEntity candidate = candidateMapper.selectOne(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getId, candidateId)
        .eq(CertificationCandidateEntity::getDeleted, 0));
    String candidateStatus = nullToEmpty(candidate == null ? null : candidate.getStatus()).toLowerCase();
    if (task == null || candidate == null || Set.of("disabled", "deleted", "inactive").contains(candidateStatus)) {
      log.warn("Practical collection admission rejected: tenantId={}, taskId={}, candidateId={}, reason=candidate_not_found_or_disabled",
          tenantId, taskId, candidateId);
      return invalid(candidateId, "CANDIDATE_NOT_FOUND", "考生不属于当前认定任务或已被禁用");
    }
    return checkCandidates(tenantId, taskId, List.of(candidate)).get(candidateId);
  }

  public void requireEligible(Long tenantId, Long taskId, Long candidateId) {
    CollectionEligibilityResult result = check(tenantId, taskId, candidateId);
    if (!result.eligible()) {
      log.warn("Practical collection admission rejected: tenantId={}, taskId={}, candidateId={}, missing={}, invalid={}",
          tenantId, taskId, candidateId, result.missingDocumentCodes(), result.invalidDocumentCodes());
      throw new BusinessException(ErrorCode.BAD_REQUEST, result.reasonMessage());
    }
  }

  public Map<Long, CollectionEligibilityResult> checkCandidates(
      Long tenantId, Long taskId, List<CertificationCandidateEntity> candidates
  ) {
    if (candidates == null || candidates.isEmpty()) {
      return Map.of();
    }
    List<Long> candidateIds = candidates.stream()
        .map(CertificationCandidateEntity::getId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    if (candidateIds.isEmpty()) {
      return Map.of();
    }
    List<CertificationCandidateDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTenantId, tenantId)
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, candidateIds)
            .in(CertificationCandidateDocumentEntity::getDocCode, REQUIRED_DOCUMENT_CODES)
            .eq(CertificationCandidateDocumentEntity::getDeleted, 0)
            .orderByDesc(CertificationCandidateDocumentEntity::getUpdatedAt));
    Map<Long, Map<String, CertificationCandidateDocumentEntity>> documentMap = new LinkedHashMap<>();
    for (CertificationCandidateDocumentEntity document : safeList(documents)) {
      documentMap.computeIfAbsent(document.getCandidateId(), ignored -> new LinkedHashMap<>())
          .putIfAbsent(document.getDocCode(), document);
    }
    Set<Long> fileIds = safeList(documents).stream()
        .map(CertificationCandidateDocumentEntity::getFileId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, FileEntity> validFiles = new LinkedHashMap<>();
    if (!fileIds.isEmpty()) {
      List<FileEntity> files = fileMapper.selectList(new LambdaQueryWrapper<FileEntity>()
          .eq(FileEntity::getTenantId, tenantId)
          .in(FileEntity::getId, fileIds)
          .eq(FileEntity::getDeleted, 0)
          .eq(FileEntity::getStatus, "uploaded"));
      validFiles.putAll(safeList(files).stream().collect(Collectors.toMap(
          FileEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new)));
    }
    Map<Long, CollectionEligibilityResult> result = new LinkedHashMap<>();
    for (CertificationCandidateEntity candidate : candidates) {
      result.put(candidate.getId(), evaluate(candidate, documentMap.getOrDefault(candidate.getId(), Map.of()), validFiles));
    }
    return result;
  }

  private CollectionEligibilityResult evaluate(
      CertificationCandidateEntity candidate,
      Map<String, CertificationCandidateDocumentEntity> documents,
      Map<Long, FileEntity> validFiles
  ) {
    List<String> missing = new ArrayList<>();
    List<String> invalid = new ArrayList<>();
    Map<String, Long> validDocumentFileIds = new LinkedHashMap<>();
    for (String code : REQUIRED_DOCUMENT_CODES) {
      CertificationCandidateDocumentEntity document = documents.get(code);
      if (document == null || document.getFileId() == null) {
        missing.add(code);
        continue;
      }
      if (!"approved".equalsIgnoreCase(nullToEmpty(document.getStatus()))
          || !validFiles.containsKey(document.getFileId())) {
        invalid.add(code);
        continue;
      }
      validDocumentFileIds.put(code, document.getFileId());
    }
    boolean eligible = missing.isEmpty() && invalid.isEmpty();
    String documentStatus = eligible ? "COMPLETED" : invalid.isEmpty() ? "INCOMPLETE" : "INVALID";
    String reasonCode = eligible ? "ELIGIBLE" : invalid.isEmpty() ? "DOCUMENT_INCOMPLETE" : "DOCUMENT_INVALID";
    StringBuilder message = new StringBuilder();
    if (!eligible) {
      message.append("该考生的信息收集尚未完成，暂不能进入实操信息采集。");
      if (!missing.isEmpty()) message.append("缺少资料：").append(String.join("、", missing)).append("。");
      if (!invalid.isEmpty()) message.append("资料异常：").append(String.join("、", invalid)).append("。");
      message.append("请先前往“认定筹备—信息收集”补充资料。");
    }
    return new CollectionEligibilityResult(
        eligible,
        candidate.getId(),
        candidate.getCandidateName(),
        candidate.getAdmissionTicketNo(),
        documentStatus,
        List.copyOf(missing),
        List.copyOf(invalid),
        reasonCode,
        eligible ? "资料已完整有效" : message.toString(),
        Collections.unmodifiableMap(validDocumentFileIds)
    );
  }

  private CollectionEligibilityResult invalid(Long candidateId, String reasonCode, String reasonMessage) {
    return new CollectionEligibilityResult(false, candidateId, null, null, "INVALID", List.of(), List.of(),
        reasonCode, reasonMessage, Map.of());
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private String nullToEmpty(String value) {
    return StringUtils.hasText(value) ? value.trim() : "";
  }
}
