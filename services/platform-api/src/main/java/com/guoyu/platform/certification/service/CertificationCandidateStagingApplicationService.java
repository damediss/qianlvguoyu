package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingPromoteReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateStagingSaveReq;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationCandidateStagingApplicationService {

  private final CertificationCandidateSourceStagingMapper stagingMapper;
  private final CertificationPreparationApplicationService preparationService;

  public CertificationCandidateStagingApplicationService(
      CertificationCandidateSourceStagingMapper stagingMapper,
      @Lazy CertificationPreparationApplicationService preparationService
  ) {
    this.stagingMapper = stagingMapper;
    this.preparationService = preparationService;
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

  @Transactional(readOnly = true)
  public PreparationCandidateStagingVO getStaging(Long stagingId) {
    return PreparationCandidateStagingVO.from(requireStaging(stagingId));
  }

  @Transactional
  public void releasePromoteByCandidate(Long taskId, Long candidateId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<CertificationCandidateSourceStagingEntity> stagingRows = stagingMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
            .eq(CertificationCandidateSourceStagingEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0));
    for (CertificationCandidateSourceStagingEntity staging : stagingRows) {
      staging.setCandidateId(null);
      staging.setPromoteStatus("draft");
      staging.setPromoteMessage(null);
      staging.setPromotedAt(null);
      stagingMapper.updateById(staging);
    }
  }

  @Transactional(readOnly = true)
  public CertificationCandidateSourceStagingEntity requireStagingByCandidate(Long taskId, Long candidateId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertificationCandidateSourceStagingEntity entity = stagingMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
            .eq(CertificationCandidateSourceStagingEntity::getTenantId, tenantId)
            .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
            .eq(CertificationCandidateSourceStagingEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateSourceStagingEntity::getDeleted, 0)
            .orderByDesc(CertificationCandidateSourceStagingEntity::getPromotedAt)
            .last("LIMIT 1"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到该考生的来源暂存数据，请使用测试学员导入");
    }
    return entity;
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

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
