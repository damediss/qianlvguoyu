package com.guoyu.platform.certification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingPromoteReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingSaveReq;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateSourceStagingMapper;
import com.guoyu.platform.certification.support.IdCardSupport;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingPromoteResultVO;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingVO;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.certification.vo.PreparationStagingPromoteErrorVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.service.TrainingStudentSourceApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainingStudentCertificationSourceVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CertificationCandidateStagingApplicationService {

  private final CertificationCandidateSourceStagingMapper stagingMapper;
  private final CertificationPreparationApplicationService preparationService;
  private final TrainingStudentSourceApplicationService trainingStudentSourceService;
  private final ObjectMapper objectMapper;

  public CertificationCandidateStagingApplicationService(
      CertificationCandidateSourceStagingMapper stagingMapper,
      @Lazy CertificationPreparationApplicationService preparationService,
      TrainingStudentSourceApplicationService trainingStudentSourceService,
      ObjectMapper objectMapper
  ) {
    this.stagingMapper = stagingMapper;
    this.preparationService = preparationService;
    this.trainingStudentSourceService = trainingStudentSourceService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationCandidateStagingVO> pageStaging(Long taskId, PreparationCandidateStagingQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<CertificationCandidateSourceStagingEntity> wrapper =
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0)
            .and(w -> w.isNull(CertificationCandidateSourceStagingEntity::getTaskId)
                .or()
                .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId))
            .orderByDesc(CertificationCandidateSourceStagingEntity::getCreatedAt);
    if (StringUtils.hasText(req.getPromoteStatus())) {
      wrapper.eq(CertificationCandidateSourceStagingEntity::getPromoteStatus, req.getPromoteStatus());
    } else {
      wrapper.eq(CertificationCandidateSourceStagingEntity::getPromoteStatus, "draft");
    }
    if (StringUtils.hasText(req.getSourceType())) {
      wrapper.eq(CertificationCandidateSourceStagingEntity::getSourceType, req.getSourceType());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(CertificationCandidateSourceStagingEntity::getCandidateName, req.getKeyword())
          .or()
          .like(CertificationCandidateSourceStagingEntity::getIdCardNo, req.getKeyword())
          .or()
          .like(CertificationCandidateSourceStagingEntity::getPhone, req.getKeyword()));
    }
    Page<CertificationCandidateSourceStagingEntity> page =
        stagingMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(PreparationCandidateStagingVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal());
  }

  @Transactional
  public PreparationCandidateStagingVO createStaging(Long taskId, PreparationCandidateStagingSaveReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationCandidateSourceStagingEntity entity = new CertificationCandidateSourceStagingEntity();
    entity.setTenantId(tenantId);
    entity.setTaskId(taskId);
    entity.setSourceType("test");
    entity.setPromoteStatus("draft");
    entity.setDeleted(0);
    applyStaging(entity, req);
    stagingMapper.insert(entity);
    return PreparationCandidateStagingVO.from(entity);
  }

  /**
   * Upserts the application-form source snapshot for single/manual candidate create or update.
   * Empty request fields are stored as null/empty so the generated form leaves those cells blank.
   */
  @Transactional
  public CertificationCandidateSourceStagingEntity upsertApplicationFormSource(
      Long taskId,
      CertificationCandidateEntity candidate,
      PreparationCandidateSaveReq req
  ) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationCandidateSourceStagingEntity entity = findStagingByCandidate(taskId, candidate.getId());
    boolean creating = entity == null;
    if (creating) {
      entity = new CertificationCandidateSourceStagingEntity();
      entity.setTenantId(tenantId);
      entity.setTaskId(taskId);
      entity.setCandidateId(candidate.getId());
      entity.setSourceType(StringUtils.hasText(req.importSource()) ? req.importSource() : "other");
      entity.setPromoteStatus("promoted");
      entity.setPromotedAt(LocalDateTime.now());
      entity.setDeleted(0);
    }
    entity.setTaskScopeId(candidate.getTaskScopeId());
    entity.setCandidateName(req.candidateName());
    entity.setIdCardNo(req.idCardNo());
    entity.setPhone(req.phone());
    entity.setGender(firstNonBlank(req.gender(), IdCardSupport.parseGender(req.idCardNo())));
    entity.setBirthDate(IdCardSupport.parseBirthDate(req.idCardNo()));
    entity.setEducation(blankToNull(req.education()));
    entity.setGraduateDate(req.graduateDate());
    entity.setSchool(blankToNull(req.school()));
    entity.setMajor(blankToNull(req.major()));
    entity.setNation(blankToNull(req.nation()));
    entity.setHomeAddress(blankToNull(req.homeAddress()));
    entity.setCompanyName(firstNonBlank(req.orgName(), candidate.getOrgName()));
    entity.setCompanyAddress(blankToNull(req.companyAddress()));
    entity.setZipCode(blankToNull(req.zipCode()));
    entity.setCompanyCreditCode(blankToNull(req.companyCreditCode()));
    entity.setWorkStartDate(req.workStartDate());
    entity.setOriginalWorkTypeName(blankToNull(req.originalWorkTypeName()));
    entity.setOriginalLevelName(blankToNull(req.originalLevelName()));
    entity.setOriginalCertNo(blankToNull(req.originalCertNo()));
    entity.setApplyWorkTypeId(candidate.getWorkTypeId());
    entity.setApplyWorkTypeName(candidate.getWorkTypeName());
    entity.setCertWorkTypeId(candidate.getWorkTypeId());
    entity.setCertWorkTypeName(candidate.getWorkTypeName());
    entity.setWorkDirection(blankToNull(req.workDirection()));
    entity.setWorkTypeCode(blankToNull(req.workTypeCode()));
    entity.setApplyLevelCode(candidate.getLevelCode());
    entity.setApplyLevelName(candidate.getLevelName());
    entity.setCertLevelName(candidate.getLevelName());
    entity.setApplyWorkYears(blankToNull(req.applyWorkYears()));
    entity.setWorkResume(blankToNull(req.workResume()));
    entity.setTrainingSummary(blankToNull(req.trainingSummary()));
    entity.setRemark(blankToNull(req.remark()));
    if (creating) {
      stagingMapper.insert(entity);
    } else {
      stagingMapper.updateById(entity);
    }
    return entity;
  }

  private String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  @Transactional(readOnly = true)
  public PreparationCandidateStagingVO getStaging(Long stagingId) {
    return PreparationCandidateStagingVO.from(requireStaging(stagingId));
  }

  @Transactional
  public void releasePromoteByCandidate(Long taskId, Long candidateId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    stagingMapper.releasePromotedCandidate(tenantId, taskId, candidateId);
  }

  @Transactional
  public void updateCandidateDocumentReference(Long taskId, Long candidateId, String docCode, Long fileId) {
    if (fileId == null || !StringUtils.hasText(docCode)) {
      return;
    }
    CertificationCandidateSourceStagingEntity source = stagingMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, SecurityContext.currentUser().getTenantId())
            .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
            .eq(CertificationCandidateSourceStagingEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0)
            .orderByDesc(CertificationCandidateSourceStagingEntity::getPromotedAt)
            .last("LIMIT 1"));
    if (source == null) {
      return;
    }
    String reference = "file-id:" + fileId;
    switch (docCode) {
      case "DOC002" -> source.setPhotoUrl(reference);
      case "DOC003" -> source.setIdCardFrontUrl(reference);
      case "DOC004" -> source.setIdCardBackUrl(reference);
      case "DOC005" -> source.setEducationCertImageUrl(reference);
      case "DOC006" -> source.setOriginalCertImageUrl(reference);
      default -> { return; }
    }
    stagingMapper.updateById(source);
  }

  @Transactional(readOnly = true)
  public CertificationCandidateSourceStagingEntity findStagingByCandidate(Long taskId, Long candidateId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return stagingMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
            .eq(CertificationCandidateSourceStagingEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0)
            .orderByDesc(CertificationCandidateSourceStagingEntity::getPromotedAt)
            .last("LIMIT 1"));
  }

  @Transactional(readOnly = true)
  public CertificationCandidateSourceStagingEntity requireStagingByCandidate(Long taskId, Long candidateId) {
    CertificationCandidateSourceStagingEntity entity = findStagingByCandidate(taskId, candidateId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到该考生的认定来源数据，请重新通过认定部或培训部导入");
    }
    return entity;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PreparationCandidateVO promoteTrainingPerson(Long taskId, Long trainingPersonId) {
    return promoteTrainingPerson(taskId, trainingPersonId, null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PreparationCandidateVO promoteTrainingPerson(Long taskId, Long trainingPersonId, Long taskScopeId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainingStudentCertificationSourceVO source =
        trainingStudentSourceService.requireCertificationSource(trainingPersonId);

    CertificationCandidateSourceStagingEntity snapshot = stagingMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
            .eq(CertificationCandidateSourceStagingEntity::getSourceType, "training_people")
            .eq(CertificationCandidateSourceStagingEntity::getExternalRefTable, "train_sign_up_people_self")
            .eq(CertificationCandidateSourceStagingEntity::getExternalRefId, source.getSignUpPersonId())
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0)
            .orderByDesc(CertificationCandidateSourceStagingEntity::getCreatedAt)
            .last("LIMIT 1"));
    if (snapshot != null && snapshot.getCandidateId() != null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该培训学员已导入当前任务");
    }

    String levelCode = levelCode(source.getReqLevel());
    String levelName = firstNonBlank(
        levelName(source.getReqLevel()),
        levelName(source.getLevel()));
    PreparationCandidateVO candidate = preparationService.createCandidate(
        taskId,
        new PreparationCandidateSaveReq(
            source.getName(),
            source.getIdCardNumber(),
            source.getPhone(),
            levelCode,
            levelName,
            "training",
            firstNonBlank(source.getCompanyName(),
                source.getTrainClassId() == null ? null : "班级" + source.getTrainClassId()),
            source.getComments(),
            taskScopeId));

    if (snapshot == null) {
      snapshot = new CertificationCandidateSourceStagingEntity();
      snapshot.setTenantId(tenantId);
      snapshot.setTaskId(taskId);
      snapshot.setSourceType("training_people");
      snapshot.setExternalRefTable("train_sign_up_people_self");
      snapshot.setExternalRefId(source.getSignUpPersonId());
      snapshot.setDeleted(0);
    }
    snapshot.setTaskScopeId(taskScopeId);
    applyTrainingSnapshot(snapshot, source, candidate);
    if (snapshot.getId() == null) {
      stagingMapper.insert(snapshot);
    } else {
      stagingMapper.updateById(snapshot);
    }
    return candidate;
  }

  @Transactional
  public PreparationCandidateStagingPromoteResultVO promoteStaging(Long taskId, PreparationCandidateStagingPromoteReq req) {
    List<PreparationCandidateVO> candidates = new ArrayList<>();
    List<PreparationStagingPromoteErrorVO> errors = new ArrayList<>();
    for (Long stagingId : req.stagingIds()) {
      try {
        candidates.add(promoteOne(taskId, stagingId));
      } catch (BusinessException ex) {
        CertificationCandidateSourceStagingEntity staging = stagingMapper.selectById(stagingId);
        errors.add(new PreparationStagingPromoteErrorVO(
            stagingId,
            staging == null ? null : staging.getCandidateName(),
            staging == null ? null : staging.getIdCardNo(),
            ex.getMessage()));
        if (staging != null) {
          staging.setPromoteStatus("failed");
          staging.setPromoteMessage(ex.getMessage());
          stagingMapper.updateById(staging);
        }
      } catch (RuntimeException ex) {
        CertificationCandidateSourceStagingEntity staging = stagingMapper.selectById(stagingId);
        errors.add(new PreparationStagingPromoteErrorVO(
            stagingId,
            staging == null ? null : staging.getCandidateName(),
            staging == null ? null : staging.getIdCardNo(),
            "导入失败，请检查学员信息是否重复或格式是否正确"));
        if (staging != null) {
          staging.setPromoteStatus("failed");
          staging.setPromoteMessage("导入失败");
          stagingMapper.updateById(staging);
        }
      }
    }
    return new PreparationCandidateStagingPromoteResultVO(
        req.stagingIds().size(),
        candidates.size(),
        errors.size(),
        candidates,
        errors);
  }

  private PreparationCandidateVO promoteOne(Long taskId, Long stagingId) {
    CertificationCandidateSourceStagingEntity staging = requireStaging(stagingId);
    if ("promoted".equals(staging.getPromoteStatus()) && staging.getCandidateId() != null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该测试学员已导入当前任务");
    }
    staging.setTaskId(taskId);
    PreparationCandidateSaveReq candidateReq = new PreparationCandidateSaveReq(
        staging.getCandidateName(),
        staging.getIdCardNo(),
        staging.getPhone(),
        staging.getApplyLevelCode(),
        firstNonBlank(staging.getApplyLevelName(), staging.getCertLevelName()),
        "staging",
        firstNonBlank(staging.getCompanyName(), staging.getIndustrialPark()),
        staging.getRemark());
    PreparationCandidateVO candidate = preparationService.createCandidate(taskId, candidateReq);
    staging.setCandidateId(candidate.id());
    staging.setPromoteStatus("promoted");
    staging.setPromoteMessage(null);
    staging.setPromotedAt(LocalDateTime.now());
    stagingMapper.updateById(staging);
    return candidate;
  }

  private CertificationCandidateSourceStagingEntity requireStaging(Long stagingId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationCandidateSourceStagingEntity entity = stagingMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getId, stagingId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "测试学员不存在");
    }
    return entity;
  }

  private void applyStaging(CertificationCandidateSourceStagingEntity entity, PreparationCandidateStagingSaveReq req) {
    entity.setCandidateName(req.candidateName());
    entity.setIdCardNo(req.idCardNo());
    entity.setPhone(req.phone());
    entity.setGender(firstNonBlank(req.gender(), IdCardSupport.parseGender(req.idCardNo())));
    entity.setBirthDate(req.birthDate() != null ? req.birthDate() : IdCardSupport.parseBirthDate(req.idCardNo()));
    entity.setEducation(req.education());
    entity.setGraduateDate(req.graduateDate());
    entity.setSchool(req.school());
    entity.setMajor(req.major());
    entity.setNation(req.nation());
    entity.setHomeAddress(req.homeAddress());
    entity.setCompanyName(req.companyName());
    entity.setCompanyAddress(req.companyAddress());
    entity.setZipCode(req.zipCode());
    entity.setCompanyCreditCode(req.companyCreditCode());
    entity.setIndustrialPark(req.industrialPark());
    entity.setWorkStartDate(req.workStartDate());
    entity.setOriginalWorkTypeName(req.originalWorkTypeName());
    entity.setOriginalLevelName(req.originalLevelName());
    entity.setOriginalCertNo(req.originalCertNo());
    entity.setApplyWorkTypeName(req.applyWorkTypeName());
    entity.setCertWorkTypeName(req.certWorkTypeName());
    entity.setWorkDirection(req.workDirection());
    entity.setWorkTypeCode(req.workTypeCode());
    entity.setApplyLevelCode(req.applyLevelCode());
    entity.setApplyLevelName(req.applyLevelName());
    entity.setCertLevelName(req.certLevelName());
    entity.setApplyWorkYears(req.applyWorkYears());
    entity.setApplyCondition(req.applyCondition());
    entity.setWorkResume(req.workResume());
    entity.setTrainingSummary(req.trainingSummary());
    entity.setRemark(req.remark());
  }

  private void applyTrainingSnapshot(
      CertificationCandidateSourceStagingEntity snapshot,
      TrainingStudentCertificationSourceVO source,
      PreparationCandidateVO candidate
  ) {
    snapshot.setCandidateId(candidate.id());
    snapshot.setPromoteStatus("promoted");
    snapshot.setPromoteMessage(null);
    snapshot.setPromotedAt(LocalDateTime.now());
    snapshot.setSignUpId(source.getSignUpId());
    snapshot.setCandidateName(source.getName());
    snapshot.setIdCardNo(source.getIdCardNumber());
    snapshot.setPhone(source.getPhone());
    snapshot.setGender(firstNonBlank(source.getGender(), IdCardSupport.parseGender(source.getIdCardNumber())));
    snapshot.setBirthDate(IdCardSupport.parseBirthDate(source.getIdCardNumber()));
    snapshot.setEducation(source.getEducation());
    snapshot.setGraduateDate(parseDate(source.getGraduateDate()));
    snapshot.setSchool(source.getSchool());
    snapshot.setMajor(source.getMajor());
    snapshot.setNation(source.getNation());
    snapshot.setHomeAddress(source.getAddress());
    snapshot.setCompanyName(source.getCompanyName());
    snapshot.setCompanyAddress(source.getCompanyAddress());
    snapshot.setZipCode(source.getZipCode());
    snapshot.setCompanyCreditCode(source.getCompanyCode());
    snapshot.setIndustrialPark(source.getIndustrialPark());
    snapshot.setWorkStartDate(parseDate(firstNonBlank(source.getWorkDateStart(), source.getWorkTime())));
    snapshot.setOriginalWorkTypeName(firstNonBlank(
        source.getWorkTypePaperName(), source.getJob(), source.getJog()));
    snapshot.setOriginalLevelName(levelName(source.getWorkTypePaperLevel()));
    snapshot.setOriginalCertNo(firstNonBlank(source.getWorkTypePaperNo(), source.getPaperNo()));
    snapshot.setApplyWorkTypeId(source.getReqWorkTypeId());
    snapshot.setApplyWorkTypeName(firstNonBlank(source.getReqWorkTypeName(), source.getWorkTypeName()));
    snapshot.setCertWorkTypeId(source.getWorkTypeId());
    snapshot.setCertWorkTypeName(firstNonBlank(source.getWorkTypeName(), source.getReqWorkTypeName()));
    snapshot.setWorkDirection(source.getJob());
    snapshot.setApplyLevelCode(candidate.levelCode());
    snapshot.setApplyLevelName(firstNonBlank(levelName(source.getReqLevel()), candidate.levelName()));
    snapshot.setCertLevelName(firstNonBlank(levelName(source.getLevel()), candidate.levelName()));
    snapshot.setApplyWorkYears(source.getWorkTime());
    snapshot.setApplyCondition(source.getDeclarationCondition());
    snapshot.setWorkResume(source.getWorkExp());
    snapshot.setTheoryScore(source.getTheoryScore());
    snapshot.setPracticalScore(source.getPracticalScore());
    snapshot.setTrainingSummary(trainingSummary(source));
    snapshot.setPhotoUrl(source.getIdCardHeader());
    snapshot.setPhotoAltUrl(source.getIdCardHeader2());
    snapshot.setIdCardFrontUrl(source.getIdCardFront());
    snapshot.setIdCardBackUrl(source.getIdCardBack());
    snapshot.setOriginalCertImageUrl(source.getWorkTypePaper());
    snapshot.setEducationCertImageUrl(source.getSchoolPaper());
    snapshot.setPlatformUserId(source.getPlatformUserId());
    snapshot.setUserId(source.getUserId());
    snapshot.setRawPayload(rawPayload(source));
    snapshot.setRemark(source.getComments());
  }

  private String rawPayload(TrainingStudentCertificationSourceVO source) {
    try {
      return objectMapper.writeValueAsString(source);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize training source signUpPersonId={}", source.getSignUpPersonId(), ex);
      return null;
    }
  }

  private LocalDate parseDate(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = value.trim();
    List<DateTimeFormatter> formatters = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy年M月d日"));
    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDate.parse(normalized, formatter);
      } catch (DateTimeParseException ignored) {
        // Try the next supported upstream date format.
      }
    }
    return null;
  }

  private String trainingSummary(TrainingStudentCertificationSourceVO source) {
    List<String> parts = new ArrayList<>();
    if (source.getTrainId() != null) {
      parts.add("培训ID：" + source.getTrainId());
    }
    if (source.getTrainClassId() != null) {
      parts.add("班级ID：" + source.getTrainClassId());
    }
    if (source.getSignUpId() != null) {
      parts.add("信息收集ID：" + source.getSignUpId());
    }
    return parts.isEmpty() ? null : String.join("；", parts);
  }

  private String levelCode(Integer level) {
    if (level == null) {
      return null;
    }
    // Training sign-up data keeps the legacy zero-based level values.
    return switch (level) {
      case 0 -> "junior";
      case 1 -> "intermediate";
      case 2 -> "senior";
      case 3 -> "technician";
      case 4 -> "senior_technician";
      default -> null;
    };
  }

  private String levelName(Integer level) {
    if (level == null) {
      return null;
    }
    return switch (level) {
      case 0 -> "初级";
      case 1 -> "中级";
      case 2 -> "高级";
      case 3 -> "技师";
      case 4 -> "高级技师";
      default -> null;
    };
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
