package com.guoyu.platform.certification.info.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.info.dto.AssessorInfoSaveReq;
import com.guoyu.platform.certification.info.dto.AssessorAccountCreateReq;
import com.guoyu.platform.certification.info.dto.CertificationInfoQueryReq;
import com.guoyu.platform.certification.info.dto.ExamEquipmentInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamSiteInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamStaffInfoSaveReq;
import com.guoyu.platform.certification.info.dto.InfoDisableReq;
import com.guoyu.platform.certification.info.dto.InfoReviewReq;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAuditRecordEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamSiteEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAuditRecordMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamSiteMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.info.vo.AssessorInfoVO;
import com.guoyu.platform.certification.info.vo.ExamEquipmentInfoVO;
import com.guoyu.platform.certification.info.vo.ExamSiteInfoVO;
import com.guoyu.platform.certification.info.vo.ExamStaffInfoVO;
import com.guoyu.platform.certification.info.vo.InfoAuditRecordVO;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.mapper.ExamStaffAssignedTaskRow;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.ResetPasswordReq;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.service.UserApplicationService;
import com.guoyu.platform.system.vo.UserVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationInfoApplicationService {

  private static final String DRAFT = "draft";
  private static final String PENDING = "pending";
  private static final String APPROVED = "approved";
  private static final String REJECTED = "rejected";
  private static final String DISABLED = "disabled";

  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationInfoExamStaffMapper examStaffMapper;
  private final CertificationTaskExamStaffMapper taskExamStaffMapper;
  private final CertificationInfoExamSiteMapper siteMapper;
  private final CertificationInfoExamEquipmentMapper equipmentMapper;
  private final CertificationInfoAuditRecordMapper auditRecordMapper;
  private final RoleMapper roleMapper;
  private final UserMapper userMapper;
  private final UserApplicationService userApplicationService;

  public CertificationInfoApplicationService(
      CertificationInfoAssessorMapper assessorMapper,
      CertificationInfoExamStaffMapper examStaffMapper,
      CertificationTaskExamStaffMapper taskExamStaffMapper,
      CertificationInfoExamSiteMapper siteMapper,
      CertificationInfoExamEquipmentMapper equipmentMapper,
      CertificationInfoAuditRecordMapper auditRecordMapper,
      RoleMapper roleMapper,
      UserMapper userMapper,
      UserApplicationService userApplicationService
  ) {
    this.assessorMapper = assessorMapper;
    this.examStaffMapper = examStaffMapper;
    this.taskExamStaffMapper = taskExamStaffMapper;
    this.siteMapper = siteMapper;
    this.equipmentMapper = equipmentMapper;
    this.auditRecordMapper = auditRecordMapper;
    this.roleMapper = roleMapper;
    this.userMapper = userMapper;
    this.userApplicationService = userApplicationService;
  }

  @Transactional(readOnly = true)
  public PageResponse<AssessorInfoVO> pageAssessors(CertificationInfoQueryReq req) {
    LambdaQueryWrapper<CertificationInfoAssessorEntity> wrapper =
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>().orderByDesc(CertificationInfoAssessorEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationInfoAssessorEntity::getAssessorName, req.getKeyword())
          .or().like(CertificationInfoAssessorEntity::getCertificateNo, req.getKeyword())
          .or().like(CertificationInfoAssessorEntity::getWorkTypeScope, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getAuditStatus())) {
      wrapper.eq(CertificationInfoAssessorEntity::getAuditStatus, req.getAuditStatus());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationInfoAssessorEntity::getWorkStatus, req.getStatus());
    }
    Page<CertificationInfoAssessorEntity> page = assessorMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toAssessorVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public AssessorInfoVO createAssessor(AssessorInfoSaveReq req) {
    ensureUniqueAssessorCertificate(req.certificateNo(), null);
    UserVO account = createAssessorAccount(req);
    CertificationInfoAssessorEntity entity = new CertificationInfoAssessorEntity();
    applyAssessor(entity, req);
    entity.setUserId(account.id());
    entity.setAccountUsername(req.accountUsername().trim());
    entity.setAccountPassword(req.accountPassword());
    entity.setAuditStatus(DRAFT);
    entity.setWorkStatus("pending_review");
    entity.setReferencedCount(0);
    assessorMapper.insert(entity);
    return toAssessorVO(entity);
  }

  @Transactional(readOnly = true)
  public AssessorInfoVO getAssessor(Long id) {
    return toAssessorVO(requireAssessor(id));
  }

  @Transactional
  public AssessorInfoVO updateAssessor(Long id, AssessorInfoSaveReq req) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    ensureAssessorEditable(entity.getAuditStatus());
    ensureUniqueAssessorCertificate(req.certificateNo(), id);
    synchronizeAssessorAccount(entity, req);
    applyAssessor(entity, req);
    assessorMapper.updateById(entity);
    return toAssessorVO(entity);
  }

  @Transactional
  public AssessorInfoVO createAssessorAccount(Long id, AssessorAccountCreateReq req) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    if (entity.getUserId() != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "该考评员已绑定平板登录账号");
    }
    UserVO account = createAssessorAccount(
        req.username(),
        req.password(),
        entity.getAssessorName(),
        entity.getPhone()
    );
    entity.setUserId(account.id());
    entity.setAccountUsername(req.username().trim());
    entity.setAccountPassword(req.password());
    assessorMapper.updateById(entity);
    return toAssessorVO(entity);
  }

  @Transactional
  public AssessorInfoVO submitAssessor(Long id) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    ensureSubmittable(entity.getAuditStatus());
    requireMaterials(entity.getMaterialsDescription());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(PENDING);
    entity.setWorkStatus("pending_review");
    entity.setSubmittedAt(LocalDateTime.now());
    entity.setSubmittedBy(operatorId());
    entity.setRejectReason(null);
    assessorMapper.updateById(entity);
    record("assessor", id, "submit", before, PENDING, null);
    return toAssessorVO(entity);
  }

  @Transactional
  public AssessorInfoVO reviewAssessor(Long id, InfoReviewReq req) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    ensurePending(entity.getAuditStatus());
    String action = normalizeAction(req.reviewAction());
    String before = entity.getAuditStatus();
    if ("pass".equals(action)) {
      entity.setAuditStatus(APPROVED);
      entity.setWorkStatus("idle");
      entity.setReviewedAt(LocalDateTime.now());
      entity.setReviewedBy(operatorId());
      entity.setReviewerName(operatorName());
      entity.setRejectReason(null);
      assessorMapper.updateById(entity);
      record("assessor", id, "pass", before, APPROVED, req.comment());
    } else if ("reject".equals(action)) {
      requireRejectComment(req.comment());
      entity.setAuditStatus(REJECTED);
      entity.setWorkStatus("pending_review");
      entity.setReviewedAt(LocalDateTime.now());
      entity.setReviewedBy(operatorId());
      entity.setReviewerName(operatorName());
      entity.setRejectReason(req.comment());
      assessorMapper.updateById(entity);
      record("assessor", id, "reject", before, REJECTED, req.comment());
    } else {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "审核动作必须为 pass 或 reject");
    }
    return toAssessorVO(entity);
  }

  @Transactional
  public void disableAssessor(Long id, InfoDisableReq req) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    String before = entity.getAuditStatus();
    entity.setAuditStatus(DISABLED);
    entity.setWorkStatus(DISABLED);
    entity.setDisabledAt(LocalDateTime.now());
    entity.setDisabledBy(operatorId());
    entity.setDisableReason(req == null ? null : req.reason());
    assessorMapper.updateById(entity);
    record("assessor", id, "disable", before, DISABLED, entity.getDisableReason());
  }

  @Transactional
  public void enableAssessor(Long id) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    ensureEnableable(entity.getAuditStatus());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(APPROVED);
    entity.setWorkStatus("idle");
    entity.setDisabledAt(null);
    entity.setDisabledBy(null);
    entity.setDisableReason(null);
    assessorMapper.updateById(entity);
    record("assessor", id, "enable", before, APPROVED, null);
  }

  @Transactional
  public void deleteAssessor(Long id) {
    CertificationInfoAssessorEntity entity = requireAssessor(id);
    protectDeletion(entity.getAuditStatus(), entity.getWorkStatus(), entity.getReferencedCount());
    assessorMapper.deleteById(id);
    record("assessor", id, "delete", entity.getAuditStatus(), "deleted", null);
  }

  @Transactional(readOnly = true)
  public List<AssessorInfoVO> listAvailableAssessors(String keyword, List<String> requiredWorkTypes) {
    return listAvailableAssessors(keyword, requiredWorkTypes, false);
  }

  @Transactional(readOnly = true)
  public List<AssessorInfoVO> listAvailableAssessors(
      String keyword,
      List<String> requiredWorkTypes,
      boolean includeAssigned
  ) {
    LambdaQueryWrapper<CertificationInfoAssessorEntity> wrapper = new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
        .eq(CertificationInfoAssessorEntity::getAuditStatus, APPROVED)
        .orderByDesc(CertificationInfoAssessorEntity::getCreatedAt);
    if (!includeAssigned) {
      wrapper.eq(CertificationInfoAssessorEntity::getWorkStatus, "idle");
    }
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(CertificationInfoAssessorEntity::getAssessorName, keyword)
          .or().like(CertificationInfoAssessorEntity::getCertificateNo, keyword));
    }
    LocalDate today = LocalDate.now();
    List<String> required = requiredWorkTypes == null ? List.of()
        : requiredWorkTypes.stream().filter(StringUtils::hasText).distinct().toList();
    return assessorMapper.selectList(wrapper).stream()
        .filter(item -> item.getValidUntil() == null || !item.getValidUntil().isBefore(today))
        .filter(item -> coversRequiredWorkTypes(item, required))
        .map(this::toAssessorVO)
        .toList();
  }

  private boolean coversRequiredWorkTypes(CertificationInfoAssessorEntity item, List<String> required) {
    if (required == null || required.isEmpty()) {
      return true;
    }
    String scope = item.getWorkTypeScope();
    if (!StringUtils.hasText(scope)) {
      return false;
    }
    String lowerScope = scope.toLowerCase(Locale.ROOT);
    for (String need : required) {
      if (!lowerScope.contains(need.toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamStaffInfoVO> pageExamStaff(CertificationInfoQueryReq req) {
    LambdaQueryWrapper<CertificationInfoExamStaffEntity> wrapper =
        new LambdaQueryWrapper<CertificationInfoExamStaffEntity>().orderByDesc(CertificationInfoExamStaffEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationInfoExamStaffEntity::getStaffName, req.getKeyword())
          .or().like(CertificationInfoExamStaffEntity::getPhone, req.getKeyword())
          .or().like(CertificationInfoExamStaffEntity::getMaskedPhone, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getAuditStatus())) {
      wrapper.eq(CertificationInfoExamStaffEntity::getAuditStatus, req.getAuditStatus());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationInfoExamStaffEntity::getWorkStatus, req.getStatus());
    }
    Page<CertificationInfoExamStaffEntity> page = examStaffMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, String> taskNosByStaffId = loadAssignedTaskNosByStaffIds(
        page.getRecords().stream().map(CertificationInfoExamStaffEntity::getId).toList());
    return new PageResponse<>(page.getRecords().stream()
        .map(entity -> toExamStaffVO(entity,
            syncExamStaffAssignmentState(entity, taskNosByStaffId.get(entity.getId()))))
        .toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public ExamStaffInfoVO createExamStaff(ExamStaffInfoSaveReq req) {
    UserVO account = createExamStaffAccount(req);
    CertificationInfoExamStaffEntity entity = new CertificationInfoExamStaffEntity();
    applyExamStaff(entity, req);
    entity.setUserId(account.id());
    entity.setAccountUsername(account.username());
    entity.setAuditStatus(PENDING);
    entity.setWorkStatus("idle");
    entity.setReferencedCount(0);
    examStaffMapper.insert(entity);
    return toExamStaffVO(entity);
  }

  @Transactional
  public ExamStaffInfoVO getExamStaff(Long id) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    return toExamStaffVO(entity);
  }

  @Transactional
  public ExamStaffInfoVO updateExamStaff(Long id, ExamStaffInfoSaveReq req) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    ensureEditable(entity.getAuditStatus());
    applyExamStaff(entity, req);
    synchronizeExamStaffAccount(entity, req);
    examStaffMapper.updateById(entity);
    return toExamStaffVO(entity);
  }

  @Transactional
  public ExamStaffInfoVO submitExamStaff(Long id) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    ensureSubmittable(entity.getAuditStatus());
    ensureExamStaffContact(entity);
    String before = entity.getAuditStatus();
    entity.setAuditStatus(PENDING);
    entity.setSubmittedAt(LocalDateTime.now());
    entity.setSubmittedBy(operatorId());
    entity.setRejectReason(null);
    examStaffMapper.updateById(entity);
    record("exam_staff", id, "submit", before, PENDING, null);
    return toExamStaffVO(entity);
  }

  @Transactional
  public ExamStaffInfoVO reviewExamStaff(Long id, InfoReviewReq req) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    ensurePending(entity.getAuditStatus());
    String action = normalizeAction(req.reviewAction());
    String before = entity.getAuditStatus();
    if ("pass".equals(action)) {
      entity.setAuditStatus(APPROVED);
      entity.setWorkStatus("idle");
      entity.setReviewedAt(LocalDateTime.now());
      entity.setReviewedBy(operatorId());
      entity.setReviewerName(operatorName());
      entity.setRejectReason(null);
      examStaffMapper.updateById(entity);
      record("exam_staff", id, "pass", before, APPROVED, req.comment());
    } else if ("reject".equals(action)) {
      requireRejectComment(req.comment());
      entity.setAuditStatus(REJECTED);
      entity.setReviewedAt(LocalDateTime.now());
      entity.setReviewedBy(operatorId());
      entity.setReviewerName(operatorName());
      entity.setRejectReason(req.comment());
      examStaffMapper.updateById(entity);
      record("exam_staff", id, "reject", before, REJECTED, req.comment());
    } else {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "审核动作必须为 pass 或 reject");
    }
    return toExamStaffVO(entity);
  }

  @Transactional
  public void disableExamStaff(Long id, InfoDisableReq req) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    String before = entity.getAuditStatus();
    entity.setAuditStatus(DISABLED);
    entity.setWorkStatus(DISABLED);
    entity.setDisabledAt(LocalDateTime.now());
    entity.setDisabledBy(operatorId());
    entity.setDisableReason(req == null ? null : req.reason());
    examStaffMapper.updateById(entity);
    record("exam_staff", id, "disable", before, DISABLED, entity.getDisableReason());
  }

  @Transactional
  public void enableExamStaff(Long id) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    ensureEnableable(entity.getAuditStatus());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(APPROVED);
    entity.setWorkStatus("idle");
    entity.setDisabledAt(null);
    entity.setDisabledBy(null);
    entity.setDisableReason(null);
    examStaffMapper.updateById(entity);
    record("exam_staff", id, "enable", before, APPROVED, null);
  }

  @Transactional
  public void deleteExamStaff(Long id) {
    CertificationInfoExamStaffEntity entity = requireExamStaff(id);
    protectDeletion(entity.getAuditStatus(), entity.getWorkStatus(), entity.getReferencedCount());
    examStaffMapper.deleteById(id);
    record("exam_staff", id, "delete", entity.getAuditStatus(), "deleted", null);
  }

  @Transactional(readOnly = true)
  public List<ExamStaffInfoVO> listAvailableExamStaff(String keyword) {
    LambdaQueryWrapper<CertificationInfoExamStaffEntity> wrapper = new LambdaQueryWrapper<CertificationInfoExamStaffEntity>()
        .eq(CertificationInfoExamStaffEntity::getAuditStatus, APPROVED)
        .in(CertificationInfoExamStaffEntity::getWorkStatus, List.of("idle", "assigned"))
        .orderByDesc(CertificationInfoExamStaffEntity::getCreatedAt);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(CertificationInfoExamStaffEntity::getStaffName, keyword)
          .or().like(CertificationInfoExamStaffEntity::getPhone, keyword)
          .or().like(CertificationInfoExamStaffEntity::getMaskedPhone, keyword));
    }
    List<CertificationInfoExamStaffEntity> staffList = examStaffMapper.selectList(wrapper);
    Map<Long, String> taskNosByStaffId = loadAssignedTaskNosByStaffIds(
        staffList.stream().map(CertificationInfoExamStaffEntity::getId).toList());
    return staffList.stream()
        .map(entity -> toExamStaffVO(entity,
            syncExamStaffAssignmentState(entity, taskNosByStaffId.get(entity.getId()))))
        .filter(item -> "idle".equalsIgnoreCase(item.workStatus()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamSiteInfoVO> pageSites(CertificationInfoQueryReq req) {
    LambdaQueryWrapper<CertificationInfoExamSiteEntity> wrapper =
        new LambdaQueryWrapper<CertificationInfoExamSiteEntity>().orderByDesc(CertificationInfoExamSiteEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationInfoExamSiteEntity::getSiteName, req.getKeyword())
          .or().like(CertificationInfoExamSiteEntity::getFilingNo, req.getKeyword())
          .or().like(CertificationInfoExamSiteEntity::getAddress, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getAuditStatus())) {
      wrapper.eq(CertificationInfoExamSiteEntity::getAuditStatus, req.getAuditStatus());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationInfoExamSiteEntity::getStatus, req.getStatus());
    }
    Page<CertificationInfoExamSiteEntity> page = siteMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toSiteVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public ExamSiteInfoVO createSite(ExamSiteInfoSaveReq req) {
    ensureUniqueSiteFiling(req.filingNo(), null);
    CertificationInfoExamSiteEntity entity = new CertificationInfoExamSiteEntity();
    applySite(entity, req);
    entity.setAuditStatus(DRAFT);
    entity.setReferencedCount(0);
    siteMapper.insert(entity);
    return toSiteVO(entity);
  }

  @Transactional(readOnly = true)
  public ExamSiteInfoVO getSite(Long id) {
    return toSiteVO(requireSite(id));
  }

  @Transactional
  public ExamSiteInfoVO updateSite(Long id, ExamSiteInfoSaveReq req) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    ensureEditable(entity.getAuditStatus());
    ensureUniqueSiteFiling(req.filingNo(), id);
    applySite(entity, req);
    siteMapper.updateById(entity);
    return toSiteVO(entity);
  }

  @Transactional
  public ExamSiteInfoVO submitSite(Long id) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    ensureSubmittable(entity.getAuditStatus());
    if (!StringUtils.hasText(entity.getFilingNo())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "备案号提交审核时必填");
    }
    requireMaterials(entity.getMaterialsDescription());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(PENDING);
    entity.setSubmittedAt(LocalDateTime.now());
    entity.setSubmittedBy(operatorId());
    siteMapper.updateById(entity);
    record("exam_site", id, "submit", before, PENDING, null);
    return toSiteVO(entity);
  }

  @Transactional
  public ExamSiteInfoVO reviewSite(Long id, InfoReviewReq req) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    String after = reviewStatus(entity.getAuditStatus(), req, entity::setRejectReason);
    entity.setAuditStatus(after);
    entity.setReviewedAt(LocalDateTime.now());
    entity.setReviewedBy(operatorId());
    entity.setReviewerName(operatorName());
    siteMapper.updateById(entity);
    record("exam_site", id, normalizeAction(req.reviewAction()), PENDING, after, req.comment());
    return toSiteVO(entity);
  }

  @Transactional
  public void disableSite(Long id, InfoDisableReq req) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    String before = entity.getAuditStatus();
    entity.setAuditStatus(DISABLED);
    entity.setStatus(DISABLED);
    entity.setDisabledAt(LocalDateTime.now());
    entity.setDisabledBy(operatorId());
    entity.setDisableReason(req == null ? null : req.reason());
    siteMapper.updateById(entity);
    record("exam_site", id, "disable", before, DISABLED, entity.getDisableReason());
  }

  @Transactional
  public void enableSite(Long id) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    ensureEnableable(entity.getAuditStatus());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(APPROVED);
    entity.setStatus("idle");
    entity.setDisabledAt(null);
    entity.setDisabledBy(null);
    entity.setDisableReason(null);
    siteMapper.updateById(entity);
    record("exam_site", id, "enable", before, APPROVED, null);
  }

  @Transactional
  public void deleteSite(Long id) {
    CertificationInfoExamSiteEntity entity = requireSite(id);
    protectDeletion(entity.getAuditStatus(), entity.getStatus(), entity.getReferencedCount());
    siteMapper.deleteById(id);
    record("exam_site", id, "delete", entity.getAuditStatus(), "deleted", null);
  }

  @Transactional(readOnly = true)
  public List<ExamSiteInfoVO> listAvailableSites(String keyword) {
    LambdaQueryWrapper<CertificationInfoExamSiteEntity> wrapper = new LambdaQueryWrapper<CertificationInfoExamSiteEntity>()
        .eq(CertificationInfoExamSiteEntity::getAuditStatus, APPROVED)
        .in(CertificationInfoExamSiteEntity::getStatus, List.of("idle", "active"))
        .orderByDesc(CertificationInfoExamSiteEntity::getCreatedAt);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(CertificationInfoExamSiteEntity::getSiteName, keyword)
          .or().like(CertificationInfoExamSiteEntity::getFilingNo, keyword));
    }
    return siteMapper.selectList(wrapper).stream().map(this::toSiteVO).toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamEquipmentInfoVO> pageEquipment(CertificationInfoQueryReq req) {
    LambdaQueryWrapper<CertificationInfoExamEquipmentEntity> wrapper =
        new LambdaQueryWrapper<CertificationInfoExamEquipmentEntity>().orderByDesc(CertificationInfoExamEquipmentEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationInfoExamEquipmentEntity::getEquipmentType, req.getKeyword())
          .or().like(CertificationInfoExamEquipmentEntity::getAssetNo, req.getKeyword())
          .or().like(CertificationInfoExamEquipmentEntity::getSiteName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getAuditStatus())) {
      wrapper.eq(CertificationInfoExamEquipmentEntity::getAuditStatus, req.getAuditStatus());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationInfoExamEquipmentEntity::getEquipmentStatus, req.getStatus());
    }
    Page<CertificationInfoExamEquipmentEntity> page = equipmentMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toEquipmentVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public ExamEquipmentInfoVO createEquipment(ExamEquipmentInfoSaveReq req) {
    ensureUniqueEquipmentAsset(req.assetNo(), null);
    CertificationInfoExamEquipmentEntity entity = new CertificationInfoExamEquipmentEntity();
    applyEquipment(entity, req);
    entity.setAuditStatus(DRAFT);
    entity.setReferencedCount(0);
    equipmentMapper.insert(entity);
    return toEquipmentVO(entity);
  }

  @Transactional(readOnly = true)
  public ExamEquipmentInfoVO getEquipment(Long id) {
    return toEquipmentVO(requireEquipment(id));
  }

  @Transactional
  public ExamEquipmentInfoVO updateEquipment(Long id, ExamEquipmentInfoSaveReq req) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    ensureEditable(entity.getAuditStatus());
    ensureUniqueEquipmentAsset(req.assetNo(), id);
    applyEquipment(entity, req);
    equipmentMapper.updateById(entity);
    return toEquipmentVO(entity);
  }

  @Transactional
  public ExamEquipmentInfoVO submitEquipment(Long id) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    ensureSubmittable(entity.getAuditStatus());
    requireMaterials(entity.getMaterialsDescription());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(PENDING);
    entity.setSubmittedAt(LocalDateTime.now());
    entity.setSubmittedBy(operatorId());
    equipmentMapper.updateById(entity);
    record("exam_equipment", id, "submit", before, PENDING, null);
    return toEquipmentVO(entity);
  }

  @Transactional
  public ExamEquipmentInfoVO reviewEquipment(Long id, InfoReviewReq req) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    String after = reviewStatus(entity.getAuditStatus(), req, entity::setRejectReason);
    entity.setAuditStatus(after);
    if (APPROVED.equals(after)) {
      entity.setEquipmentStatus("idle");
    }
    entity.setReviewedAt(LocalDateTime.now());
    entity.setReviewedBy(operatorId());
    entity.setReviewerName(operatorName());
    equipmentMapper.updateById(entity);
    record("exam_equipment", id, normalizeAction(req.reviewAction()), PENDING, after, req.comment());
    return toEquipmentVO(entity);
  }

  @Transactional
  public void disableEquipment(Long id, InfoDisableReq req) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    String before = entity.getAuditStatus();
    entity.setAuditStatus(DISABLED);
    entity.setEquipmentStatus(DISABLED);
    entity.setDisabledAt(LocalDateTime.now());
    entity.setDisabledBy(operatorId());
    entity.setDisableReason(req == null ? null : req.reason());
    equipmentMapper.updateById(entity);
    record("exam_equipment", id, "disable", before, DISABLED, entity.getDisableReason());
  }

  @Transactional
  public void enableEquipment(Long id) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    ensureEnableable(entity.getAuditStatus());
    String before = entity.getAuditStatus();
    entity.setAuditStatus(APPROVED);
    entity.setEquipmentStatus("idle");
    entity.setDisabledAt(null);
    entity.setDisabledBy(null);
    entity.setDisableReason(null);
    equipmentMapper.updateById(entity);
    record("exam_equipment", id, "enable", before, APPROVED, null);
  }

  @Transactional
  public void deleteEquipment(Long id) {
    CertificationInfoExamEquipmentEntity entity = requireEquipment(id);
    protectDeletion(entity.getAuditStatus(), entity.getEquipmentStatus(), entity.getReferencedCount());
    equipmentMapper.deleteById(id);
    record("exam_equipment", id, "delete", entity.getAuditStatus(), "deleted", null);
  }

  @Transactional(readOnly = true)
  public List<ExamEquipmentInfoVO> listAvailableEquipment(String keyword) {
    LambdaQueryWrapper<CertificationInfoExamEquipmentEntity> wrapper = new LambdaQueryWrapper<CertificationInfoExamEquipmentEntity>()
        .eq(CertificationInfoExamEquipmentEntity::getAuditStatus, APPROVED)
        .eq(CertificationInfoExamEquipmentEntity::getEquipmentStatus, "idle")
        .orderByDesc(CertificationInfoExamEquipmentEntity::getCreatedAt);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(CertificationInfoExamEquipmentEntity::getEquipmentType, keyword)
          .or().like(CertificationInfoExamEquipmentEntity::getAssetNo, keyword)
          .or().like(CertificationInfoExamEquipmentEntity::getSiteName, keyword));
    }
    LocalDate today = LocalDate.now();
    return equipmentMapper.selectList(wrapper).stream()
        .filter(item -> "idle".equalsIgnoreCase(item.getEquipmentStatus()))
        .filter(item -> item.getCalibrationValidUntil() == null || !item.getCalibrationValidUntil().isBefore(today))
        .map(this::toEquipmentVO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InfoAuditRecordVO> listAuditRecords(String ownerType, Long ownerId) {
    return auditRecordMapper.selectList(new LambdaQueryWrapper<CertificationInfoAuditRecordEntity>()
            .eq(CertificationInfoAuditRecordEntity::getOwnerType, ownerType)
            .eq(CertificationInfoAuditRecordEntity::getOwnerId, ownerId)
            .orderByDesc(CertificationInfoAuditRecordEntity::getCreatedAt))
        .stream().map(this::toAuditVO).toList();
  }

  private void applyAssessor(CertificationInfoAssessorEntity entity, AssessorInfoSaveReq req) {
    entity.setAssessorName(req.assessorName());
    entity.setIdentityNo(req.identityNo());
    entity.setCertificateNo(req.certificateNo());
    entity.setCertificatePhotoFileId(req.certificatePhotoFileId());
    entity.setCertificatePhotoFileName(req.certificatePhotoFileName());
    String joinedScopes = req.workTypeScopes() == null
        ? ""
        : req.workTypeScopes().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.joining("、"));
    entity.setWorkTypeScope(joinedScopes);
    entity.setWorkTypeJson(toWorkTypeJson(req.workTypeScopes()));
    entity.setLevelScope(req.levelScope());
    entity.setValidUntil(req.validUntil());
    entity.setPhone(req.phone());
    entity.setMaskedPhone(maskPhone(req.phone()));
    entity.setMaterialsDescription(req.materialsDescription());
    entity.setRemark(req.remark());
  }

  private String toWorkTypeJson(List<String> workTypeScopes) {
    if (workTypeScopes == null || workTypeScopes.isEmpty()) {
      return "[]";
    }
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(workTypeScopes);
    } catch (Exception e) {
      return "[]";
    }
  }

  private UserVO createAssessorAccount(AssessorInfoSaveReq req) {
    if (!StringUtils.hasText(req.accountUsername()) || !StringUtils.hasText(req.accountPassword())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "创建考评员时必须填写平板登录账号和密码");
    }
    return createAssessorAccount(
        req.accountUsername(),
        req.accountPassword(),
        req.assessorName(),
        req.phone()
    );
  }

  private UserVO createAssessorAccount(String username, String password, String displayName, String phone) {
    RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getCode, "CERTIFICATION_ASSESSOR")
        .eq(RoleEntity::getStatus, "enabled")
        .last("LIMIT 1"));
    if (role == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操考评员角色未配置，请先执行最新数据库迁移");
    }
    return userApplicationService.create(new UserCreateReq(
        null,
        null,
        username.trim(),
        password,
        displayName,
        "assessor",
        phone,
        null,
        "enabled",
        List.of(role.getId())
    ));
  }

  private UserVO createExamStaffAccount(ExamStaffInfoSaveReq req) {
    if (!StringUtils.hasText(req.accountUsername()) || !StringUtils.hasText(req.accountPassword())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "新增考务信息时必须填写认定管理员账号和密码");
    }
    RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getCode, "CERTIFICATION_ADMIN_NO_REVIEW")
        .eq(RoleEntity::getStatus, "enabled")
        .last("LIMIT 1"));
    if (role == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无审核权限的认定管理员角色未配置，请先执行最新数据库迁移");
    }
    return userApplicationService.create(new UserCreateReq(
        null,
        null,
        req.accountUsername().trim(),
        req.accountPassword(),
        req.staffName().trim(),
        "certification_admin",
        req.phone().trim(),
        null,
        "enabled",
        List.of(role.getId())
    ));
  }

  private void synchronizeExamStaffAccount(CertificationInfoExamStaffEntity entity, ExamStaffInfoSaveReq req) {
    if (entity.getUserId() == null) {
      if (StringUtils.hasText(req.accountUsername()) || StringUtils.hasText(req.accountPassword())) {
        UserVO account = createExamStaffAccount(req);
        entity.setUserId(account.id());
        entity.setAccountUsername(account.username());
      }
      return;
    }
    UserEntity account = userMapper.selectById(entity.getUserId());
    if (account == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考务人员绑定的认定管理员账号不存在");
    }
    account.setDisplayName(req.staffName().trim());
    account.setPhone(req.phone().trim());
    userMapper.updateById(account);
  }

  private void synchronizeAssessorAccount(CertificationInfoAssessorEntity entity, AssessorInfoSaveReq req) {
    boolean hasUsername = StringUtils.hasText(req.accountUsername());
    boolean hasPassword = StringUtils.hasText(req.accountPassword());
    if (!hasUsername && !hasPassword) {
      return;
    }
    if (!hasUsername || !hasPassword) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "编辑平板账号时必须同时填写账号和密码");
    }
    String username = req.accountUsername().trim();
    if (entity.getUserId() == null) {
      entity.setUserId(createAssessorAccount(req).id());
      entity.setAccountUsername(username);
      entity.setAccountPassword(req.accountPassword());
      return;
    }

    UserEntity account = userMapper.selectById(entity.getUserId());
    if (account == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员绑定的平板登录账号不存在");
    }
    boolean accountChanged = false;
    if (!username.equals(account.getUsername())) {
      boolean usernameExists = userMapper.exists(new LambdaQueryWrapper<UserEntity>()
          .eq(UserEntity::getTenantId, account.getTenantId())
          .eq(UserEntity::getUsername, username)
          .ne(UserEntity::getId, account.getId()));
      if (usernameExists) {
        throw new BusinessException(ErrorCode.CONFLICT, "平板登录账号已存在");
      }
      account.setUsername(username);
      accountChanged = true;
    }
    if (!Objects.equals(req.assessorName(), account.getDisplayName())) {
      account.setDisplayName(req.assessorName());
      accountChanged = true;
    }
    if (!Objects.equals(req.phone(), account.getPhone())) {
      account.setPhone(req.phone());
      accountChanged = true;
    }
    if (accountChanged) {
      userMapper.updateById(account);
    }
    if (!Objects.equals(req.accountPassword(), entity.getAccountPassword())) {
      userApplicationService.resetPassword(entity.getUserId(), new ResetPasswordReq(req.accountPassword()));
    }
    entity.setAccountUsername(username);
    entity.setAccountPassword(req.accountPassword());
  }

  private void applyExamStaff(CertificationInfoExamStaffEntity entity, ExamStaffInfoSaveReq req) {
    entity.setStaffName(req.staffName().trim());
    entity.setPhone(req.phone().trim());
    entity.setMaskedPhone(maskPhone(req.phone()));
  }

  private void ensureExamStaffContact(CertificationInfoExamStaffEntity entity) {
    if (!StringUtils.hasText(entity.getStaffName())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "姓名不能为空");
    }
    if (!StringUtils.hasText(entity.getPhone())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "联系电话不能为空");
    }
  }

  private void applySite(CertificationInfoExamSiteEntity entity, ExamSiteInfoSaveReq req) {
    entity.setSiteName(req.siteName());
    entity.setSiteType(req.siteType());
    entity.setAddress(req.address());
    entity.setSeatCount(req.seatCount());
    entity.setFilingNo(req.filingNo());
    entity.setContactName(req.contactName());
    entity.setContactPhone(req.contactPhone());
    entity.setMaskedContactPhone(maskPhone(req.contactPhone()));
    entity.setEquipmentCondition(req.equipmentCondition());
    entity.setMaterialsDescription(req.materialsDescription());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "idle");
    entity.setRemark(req.remark());
  }

  private void applyEquipment(CertificationInfoExamEquipmentEntity entity, ExamEquipmentInfoSaveReq req) {
    entity.setEquipmentType(req.equipmentType());
    entity.setAssetNo(req.assetNo());
    entity.setEquipmentQuantity(req.equipmentQuantity());
    entity.setSiteId(req.siteId());
    entity.setSiteName(req.siteName());
    entity.setEquipmentModel(req.equipmentModel());
    entity.setCalibrationValidUntil(req.calibrationValidUntil());
    if (StringUtils.hasText(req.equipmentStatus())) {
      entity.setEquipmentStatus(req.equipmentStatus());
    } else if (!StringUtils.hasText(entity.getEquipmentStatus())) {
      entity.setEquipmentStatus("idle");
    }
    entity.setEquipmentPhotoFileId(req.equipmentPhotoFileId());
    entity.setEquipmentPhotoFileName(req.equipmentPhotoFileName());
    entity.setMaterialsDescription(req.materialsDescription());
    entity.setRemark(req.remark());
  }

  private CertificationInfoAssessorEntity requireAssessor(Long id) {
    CertificationInfoAssessorEntity entity = assessorMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员信息不存在");
    }
    return entity;
  }

  private CertificationInfoExamStaffEntity requireExamStaff(Long id) {
    CertificationInfoExamStaffEntity entity = examStaffMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考务信息不存在");
    }
    return entity;
  }

  private CertificationInfoExamSiteEntity requireSite(Long id) {
    CertificationInfoExamSiteEntity entity = siteMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考场信息不存在");
    }
    return entity;
  }

  private CertificationInfoExamEquipmentEntity requireEquipment(Long id) {
    CertificationInfoExamEquipmentEntity entity = equipmentMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试设备不存在");
    }
    return entity;
  }

  private void ensureUniqueAssessorCertificate(String certificateNo, Long excludeId) {
    if (!StringUtils.hasText(certificateNo)) {
      return;
    }
    LambdaQueryWrapper<CertificationInfoAssessorEntity> wrapper = new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
        .eq(CertificationInfoAssessorEntity::getCertificateNo, certificateNo)
        .ne(excludeId != null, CertificationInfoAssessorEntity::getId, excludeId);
    if (assessorMapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "考评员证号已存在");
    }
  }

  private void ensureUniqueSiteFiling(String filingNo, Long excludeId) {
    if (!StringUtils.hasText(filingNo)) {
      return;
    }
    LambdaQueryWrapper<CertificationInfoExamSiteEntity> wrapper = new LambdaQueryWrapper<CertificationInfoExamSiteEntity>()
        .eq(CertificationInfoExamSiteEntity::getFilingNo, filingNo)
        .ne(excludeId != null, CertificationInfoExamSiteEntity::getId, excludeId);
    if (siteMapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "考场备案号已存在");
    }
  }

  private void ensureUniqueEquipmentAsset(String assetNo, Long excludeId) {
    LambdaQueryWrapper<CertificationInfoExamEquipmentEntity> wrapper = new LambdaQueryWrapper<CertificationInfoExamEquipmentEntity>()
        .eq(CertificationInfoExamEquipmentEntity::getAssetNo, assetNo)
        .ne(excludeId != null, CertificationInfoExamEquipmentEntity::getId, excludeId);
    if (equipmentMapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "设备资产编号已存在");
    }
  }

  private void ensureEditable(String auditStatus) {
    if (!List.of(DRAFT, REJECTED).contains(auditStatus)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许编辑");
    }
  }

  private void ensureAssessorEditable(String auditStatus) {
    if (!List.of(DRAFT, REJECTED, DISABLED).contains(auditStatus)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许编辑");
    }
  }

  private void ensureSubmittable(String auditStatus) {
    if (!List.of(DRAFT, REJECTED).contains(auditStatus)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许提交审核");
    }
  }

  private void ensurePending(String auditStatus) {
    if (!PENDING.equals(auditStatus)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待主管审核记录可以审核");
    }
  }

  private void ensureEnableable(String auditStatus) {
    if (!DISABLED.equals(auditStatus)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已停用记录可以启用");
    }
  }

  private void requireMaterials(String materialsDescription) {
    if (!StringUtils.hasText(materialsDescription) || materialsDescription.trim().length() < 8) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "材料说明提交审核时必填且不少于8个字符");
    }
  }

  private void requireRejectComment(String comment) {
    if (!StringUtils.hasText(comment) || comment.trim().length() < 6) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回原因必填且不少于6个字符");
    }
  }

  private void protectDeletion(String auditStatus, String operationalStatus, Integer referencedCount) {
    boolean auditDisabled = DISABLED.equals(auditStatus);
    boolean operationallyDisabled = DISABLED.equals(operationalStatus);
    if (APPROVED.equals(auditStatus) && !auditDisabled && !operationallyDisabled) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已入库数据不能删除，只能停用");
    }
    if (referencedCount != null && referencedCount > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "数据已被任务引用，不能删除");
    }
    if (List.of(DRAFT, REJECTED, DISABLED).contains(auditStatus) || operationallyDisabled) {
      return;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不允许删除");
  }

  private String reviewStatus(String currentStatus, InfoReviewReq req, java.util.function.Consumer<String> rejectReasonSetter) {
    ensurePending(currentStatus);
    String action = normalizeAction(req.reviewAction());
    if ("pass".equals(action)) {
      rejectReasonSetter.accept(null);
      return APPROVED;
    }
    if ("reject".equals(action)) {
      requireRejectComment(req.comment());
      rejectReasonSetter.accept(req.comment());
      return REJECTED;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "审核动作必须为 pass 或 reject");
  }

  private String normalizeAction(String action) {
    return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
  }

  private void record(String ownerType, Long ownerId, String action, String beforeStatus, String afterStatus, String comment) {
    CertificationInfoAuditRecordEntity record = new CertificationInfoAuditRecordEntity();
    record.setOwnerType(ownerType);
    record.setOwnerId(ownerId);
    record.setAction(action);
    record.setBeforeStatus(beforeStatus);
    record.setAfterStatus(afterStatus);
    record.setOperatorId(operatorId());
    record.setOperatorName(operatorName());
    record.setComment(comment);
    auditRecordMapper.insert(record);
  }

  private Long operatorId() {
    try {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      return user.getUserId();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private String operatorName() {
    try {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      return user.getDisplayName();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private String maskPhone(String phone) {
    if (!StringUtils.hasText(phone) || phone.length() < 7) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  private AssessorInfoVO toAssessorVO(CertificationInfoAssessorEntity entity) {
    String accountUsername = entity.getAccountUsername();
    if (!StringUtils.hasText(accountUsername) && entity.getUserId() != null) {
      UserEntity user = userMapper.selectById(entity.getUserId());
      if (user != null) {
        accountUsername = user.getUsername();
      }
    }
    return new AssessorInfoVO(entity.getId(), entity.getUserId(), accountUsername, entity.getAccountPassword(),
        entity.getAssessorName(), entity.getIdentityNo(), entity.getCertificateNo(),
        entity.getCertificatePhotoFileId(), entity.getCertificatePhotoFileName(),
        entity.getWorkTypeScope(),
        parseWorkTypeScopes(entity.getWorkTypeJson(), entity.getWorkTypeScope()),
        entity.getLevelScope(), entity.getValidUntil(),
        StringUtils.hasText(entity.getMaskedPhone()) ? entity.getMaskedPhone() : maskPhone(entity.getPhone()),
        entity.getAuditStatus(), entity.getWorkStatus(), entity.getMaterialsDescription(), entity.getRemark());
  }

  private List<String> parseWorkTypeScopes(String workTypeJson, String workTypeScope) {
    if (StringUtils.hasText(workTypeJson) && !"[]".equals(workTypeJson)) {
      try {
        String[] arr = new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(workTypeJson, String[].class);
        if (arr != null && arr.length > 0) {
          return java.util.Arrays.stream(arr)
              .filter(s -> s != null && !s.isBlank())
              .map(String::trim)
              .toList();
        }
      } catch (Exception ignored) {
        // fall through to derive from string column
      }
    }
    if (StringUtils.hasText(workTypeScope)) {
      return java.util.Arrays.stream(workTypeScope.split("[、,，]"))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .distinct()
          .toList();
    }
    return List.of();
  }

  private ExamStaffInfoVO toExamStaffVO(CertificationInfoExamStaffEntity entity) {
    return toExamStaffVO(entity, resolveAssignedTaskNos(entity.getId()));
  }

  private ExamStaffInfoVO toExamStaffVO(CertificationInfoExamStaffEntity entity, String assignedTaskNos) {
    String editablePhone = List.of(DRAFT, REJECTED).contains(entity.getAuditStatus()) ? entity.getPhone() : null;
    return new ExamStaffInfoVO(entity.getId(), entity.getUserId(), entity.getAccountUsername(), entity.getStaffName(), editablePhone,
        StringUtils.hasText(entity.getMaskedPhone()) ? entity.getMaskedPhone() : maskPhone(entity.getPhone()),
        entity.getAuditStatus(), resolveEffectiveWorkStatus(entity, assignedTaskNos), assignedTaskNos);
  }

  private String resolveAssignedTaskNos(Long staffId) {
    if (staffId == null) {
      return null;
    }
    return loadAssignedTaskNosByStaffIds(List.of(staffId)).get(staffId);
  }

  private String resolveEffectiveWorkStatus(CertificationInfoExamStaffEntity entity, String assignedTaskNos) {
    if (entity == null) {
      return null;
    }
    if (!APPROVED.equals(entity.getAuditStatus())) {
      return entity.getWorkStatus();
    }
    return StringUtils.hasText(assignedTaskNos) ? "assigned" : "idle";
  }

  /** 过期待派释放：已审批考务若任务绑定已清空，则把 workStatus/referencedCount 重置为 idle/0（b693f25 既有逻辑，9ae1eff 丢失后恢复） */
  private String syncExamStaffAssignmentState(CertificationInfoExamStaffEntity entity, String assignedTaskNos) {
    if (entity == null || !APPROVED.equals(entity.getAuditStatus())) {
      return assignedTaskNos;
    }
    boolean hasActiveBinding = StringUtils.hasText(assignedTaskNos);
    String expectedWorkStatus = hasActiveBinding ? "assigned" : "idle";
    int expectedReferencedCount = hasActiveBinding ? assignedTaskNos.split("、").length : 0;
    if (!expectedWorkStatus.equals(entity.getWorkStatus())
        || !Objects.equals(entity.getReferencedCount(), expectedReferencedCount)) {
      entity.setWorkStatus(expectedWorkStatus);
      entity.setReferencedCount(expectedReferencedCount);
      examStaffMapper.updateById(entity);
    }
    return assignedTaskNos;
  }

  private Map<Long, String> loadAssignedTaskNosByStaffIds(List<Long> staffIds) {
    if (staffIds == null || staffIds.isEmpty()) {
      return Map.of();
    }
    List<Long> ids = staffIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    List<ExamStaffAssignedTaskRow> rows = taskExamStaffMapper.selectAssignedTaskNosByStaffIds(ids);
    if (rows == null || rows.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> grouped = new LinkedHashMap<>();
    for (ExamStaffAssignedTaskRow row : rows) {
      if (row.getStaffId() == null || !StringUtils.hasText(row.getTaskNo())) {
        continue;
      }
      grouped.merge(row.getStaffId(), row.getTaskNo(), (left, right) -> left + "、" + right);
    }
    return grouped;
  }

  private ExamSiteInfoVO toSiteVO(CertificationInfoExamSiteEntity entity) {
    return new ExamSiteInfoVO(entity.getId(), entity.getSiteName(), entity.getSiteType(), entity.getAddress(),
        entity.getSeatCount(), entity.getFilingNo(), entity.getContactName(),
        StringUtils.hasText(entity.getMaskedContactPhone()) ? entity.getMaskedContactPhone() : maskPhone(entity.getContactPhone()),
        entity.getEquipmentCondition(), entity.getMaterialsDescription(), entity.getAuditStatus(), entity.getStatus(), entity.getRemark());
  }

  private ExamEquipmentInfoVO toEquipmentVO(CertificationInfoExamEquipmentEntity entity) {
    return new ExamEquipmentInfoVO(entity.getId(), entity.getEquipmentType(), entity.getAssetNo(),
        entity.getEquipmentQuantity(), entity.getSiteId(),
        entity.getSiteName(), entity.getEquipmentModel(), entity.getCalibrationValidUntil(), entity.getAuditStatus(),
        entity.getEquipmentStatus(), entity.getEquipmentPhotoFileId(), entity.getEquipmentPhotoFileName(),
        entity.getMaterialsDescription(), entity.getRemark());
  }

  private InfoAuditRecordVO toAuditVO(CertificationInfoAuditRecordEntity entity) {
    return new InfoAuditRecordVO(entity.getId(), entity.getOwnerType(), entity.getOwnerId(), entity.getAction(),
        entity.getBeforeStatus(), entity.getAfterStatus(), entity.getOperatorId(), entity.getOperatorName(),
        entity.getComment(), entity.getCreatedAt());
  }
}
