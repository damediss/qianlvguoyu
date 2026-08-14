package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.SubsidyTransitionReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackProfileEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.vo.SubsidyEventVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainSubsidyApplicationService {

  public static final String MODE_SKILL = "skill_upgrade";
  private static final Set<String> PHASES = Set.of(
      "draft", "checking", "ready", "submitted", "rejected", "approved", "granting", "granted", "closed");

  private final TrainSubsidyMapper mapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainSubsidyPackProfileApplicationService profileService;
  private final TrainSubsidyMaterialPackService materialPackService;
  private final com.guoyu.platform.file.service.FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TrainSubsidyApplicationService(
      TrainSubsidyMapper mapper,
      TrainSubsidyEventMapper eventMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainSubsidyPackProfileApplicationService profileService,
      TrainSubsidyMaterialPackService materialPackService,
      com.guoyu.platform.file.service.FileApplicationService fileApplicationService) {
    this.mapper = mapper;
    this.eventMapper = eventMapper;
    this.slotFileMapper = slotFileMapper;
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.profileService = profileService;
    this.materialPackService = materialPackService;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSubsidyVO> page(TrainSubsidyQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSubsidyEntity> wrapper = new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getDeleted, 0)
        .orderByDesc(TrainSubsidyEntity::getCreateTime);
    if (req.getProjectId() != null) wrapper.eq(TrainSubsidyEntity::getProjectId, req.getProjectId());
    if (req.getClassId() != null) wrapper.eq(TrainSubsidyEntity::getClassId, req.getClassId());
    if (StringUtils.hasText(req.getPhase())) wrapper.eq(TrainSubsidyEntity::getPhase, req.getPhase().trim());
    if (StringUtils.hasText(req.getRegionCode())) wrapper.eq(TrainSubsidyEntity::getRegionCode, req.getRegionCode().trim());
    if (StringUtils.hasText(req.getTrainMode())) wrapper.eq(TrainSubsidyEntity::getTrainMode, req.getTrainMode().trim());
    if (req.getPolicyYear() != null) wrapper.eq(TrainSubsidyEntity::getPolicyYear, req.getPolicyYear());
    if (req.getStatus() != null) wrapper.eq(TrainSubsidyEntity::getStatus, req.getStatus());
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(TrainSubsidyEntity::getPolicyTemplate, req.getKeyword())
          .or().like(TrainSubsidyEntity::getRegionName, req.getKeyword())
          .or().like(TrainSubsidyEntity::getRemark, req.getKeyword()));
    }
    Page<TrainSubsidyEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSubsidyVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainSubsidyVO create(TrainSubsidyCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainInfoClassEntity clazz = requireClass(req.getClassId());
    TrainInfoListEntity info = clazz.getTrainInfoId() != null
        ? infoListMapper.selectById(clazz.getTrainInfoId()) : null;

    String trainMode = req.getTrainMode().trim();
    if (!MODE_SKILL.equals(trainMode) && !"apprentice".equals(trainMode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "trainMode 仅支持 skill_upgrade / apprentice");
    }
    if ("apprentice".equals(trainMode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "本期不做新型学徒（D16）");
    }

    String regionCode = StringUtils.hasText(req.getRegionCode())
        ? req.getRegionCode().trim()
        : (info != null && info.getCityId() != null ? String.valueOf(info.getCityId()) : null);
    String regionName = StringUtils.hasText(req.getRegionName())
        ? req.getRegionName().trim()
        : (info != null ? info.getCityName() : null);
    if (!StringUtils.hasText(regionName) && !StringUtils.hasText(regionCode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析申报地市，请手工选择");
    }
    if (!StringUtils.hasText(regionCode)) {
      regionCode = regionName;
    }
    if (!StringUtils.hasText(regionName)) {
      regionName = regionCode;
    }

    Integer policyYear = req.getPolicyYear();
    if (policyYear == null) {
      policyYear = resolvePolicyYear(clazz, info);
    }

    TrainSubsidyPackProfileEntity profile;
    if (req.getProfileId() != null) {
      var vo = profileService.get(req.getProfileId());
      profile = new TrainSubsidyPackProfileEntity();
      profile.setId(vo.id());
      profile.setProfileVersion(vo.profileVersion());
      profile.setRegionCode(vo.regionCode());
      profile.setRegionName(vo.regionName());
      profile.setTrainMode(vo.trainMode());
      profile.setPolicyYear(vo.policyYear());
      profile.setTitle(vo.title());
    } else {
      profile = profileService.resolveActive(regionCode, regionName, trainMode, policyYear);
    }

    TrainSubsidyEntity entity = new TrainSubsidyEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setClassId(clazz.getId());
    entity.setTrainInfoId(clazz.getTrainInfoId());
    entity.setProjectId(req.getProjectId() != null ? req.getProjectId() : clazz.getTrainInfoId());
    entity.setPolicyTemplate(StringUtils.hasText(req.getPolicyTemplate())
        ? req.getPolicyTemplate()
        : profile.getTitle());
    entity.setSubsidyAmount(req.getSubsidyAmount());
    entity.setStudentCount(req.getStudentCount());
    entity.setRemark(req.getRemark());
    entity.setStatus(0);
    entity.setPhase("draft");
    entity.setRegionCode(regionCode);
    entity.setRegionName(regionName);
    entity.setTrainMode(trainMode);
    entity.setPolicyYear(policyYear);
    entity.setProfileId(profile.getId());
    entity.setProfileVersion(profile.getProfileVersion());
    entity.setProfileLocked(0);
    entity.setMaterialStatus(0);
    entity.setRssbStatus(0);
    entity.setDeleted(0);
    mapper.insert(entity);
    addEvent(entity, "create", null, "draft", "创建补贴单", null);
    return toVO(entity);
  }

  @Transactional
  public TrainSubsidyVO update(Long id, TrainSubsidyUpdateReq req) {
    TrainSubsidyEntity entity = getExisting(id);
    assertDraftEditable(entity);
    if (req.getRegionCode() != null) entity.setRegionCode(req.getRegionCode().trim());
    if (req.getRegionName() != null) entity.setRegionName(req.getRegionName().trim());
    if (req.getTrainMode() != null) {
      if ("apprentice".equals(req.getTrainMode())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "本期不做新型学徒");
      }
      entity.setTrainMode(req.getTrainMode().trim());
    }
    if (req.getPolicyYear() != null) entity.setPolicyYear(req.getPolicyYear());
    if (req.getPolicyTemplate() != null) entity.setPolicyTemplate(req.getPolicyTemplate());
    if (req.getSubsidyAmount() != null) entity.setSubsidyAmount(req.getSubsidyAmount());
    if (req.getStudentCount() != null) entity.setStudentCount(req.getStudentCount());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());

    boolean keyChanged = req.getRegionCode() != null || req.getRegionName() != null
        || req.getTrainMode() != null || req.getPolicyYear() != null || req.getProfileId() != null;
    if (keyChanged && (entity.getProfileLocked() == null || entity.getProfileLocked() == 0)) {
      if (req.getProfileId() != null) {
        var vo = profileService.get(req.getProfileId());
        entity.setProfileId(vo.id());
        entity.setProfileVersion(vo.profileVersion());
      } else {
        TrainSubsidyPackProfileEntity profile = profileService.resolveActive(
            entity.getRegionCode(), entity.getRegionName(), entity.getTrainMode(), entity.getPolicyYear());
        entity.setProfileId(profile.getId());
        entity.setProfileVersion(profile.getProfileVersion());
      }
    }
    mapper.updateById(entity);
    addEvent(entity, "update", entity.getPhase(), entity.getPhase(), "更新补贴单", null);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainSubsidyEntity entity = getExisting(id);
    if (!"draft".equals(entity.getPhase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可删除");
    }
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  @Transactional
  public TrainSubsidyVO transition(Long id, SubsidyTransitionReq req) {
    TrainSubsidyEntity entity = getExisting(id);
    String to = req.getToPhase().trim();
    if (!PHASES.contains(to)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "非法 phase: " + to);
    }
    String from = entity.getPhase();
    assertTransition(from, to);
    entity.setPhase(to);
    // 双写旧 status
    if ("draft".equals(to) || "checking".equals(to) || "ready".equals(to) || "rejected".equals(to)) {
      entity.setStatus(0);
    } else if ("submitted".equals(to)) {
      entity.setStatus(1);
    } else if ("approved".equals(to) || "granting".equals(to) || "granted".equals(to) || "closed".equals(to)) {
      entity.setStatus(2);
    }
    mapper.updateById(entity);
    addEvent(entity, "transition", from, to, req.getRemark(), null);
    return toVO(entity);
  }

  @Transactional(readOnly = true)
  public List<SubsidyEventVO> events(Long id) {
    getExisting(id);
    return eventMapper.selectList(new LambdaQueryWrapper<TrainSubsidyEventEntity>()
            .eq(TrainSubsidyEventEntity::getSubsidyId, id)
            .eq(TrainSubsidyEventEntity::getDeleted, 0)
            .orderByDesc(TrainSubsidyEventEntity::getId))
        .stream()
        .map(e -> new SubsidyEventVO(
            e.getId(), e.getSubsidyId(), e.getAction(), e.getFromPhase(), e.getToPhase(),
            e.getRemark(), e.getOperatorId(), e.getCreatedAt()))
        .toList();
  }

  @Transactional
  public Map<String, Object> uploadSlotFile(Long id, String slotCode, MultipartFile file) {
    TrainSubsidyEntity entity = getExisting(id);
    if (entity.getProfileLocked() != null && entity.getProfileLocked() == 1
        && !"draft".equals(entity.getPhase()) && !"checking".equals(entity.getPhase())
        && !"ready".equals(entity.getPhase()) && !"rejected".equals(entity.getPhase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可再上传槽位附件");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择文件");
    }
    if (!StringUtils.hasText(slotCode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少 slotCode");
    }
    var stored = fileApplicationService.upload(file, "private");
    TrainSubsidySlotFileEntity row = new TrainSubsidySlotFileEntity();
    row.setSubsidyId(id);
    row.setSlotCode(slotCode.trim());
    row.setFileId(stored.id());
    row.setFilePath("/api/files/" + stored.id() + "/download");
    row.setFileName(stored.originalName());
    row.setUploadedBy(SecurityContext.currentUser().getUserId());
    row.setTenantId(entity.getTenantId());
    row.setDeleted(0);
    slotFileMapper.insert(row);
    addEvent(entity, "upload_slot", entity.getPhase(), entity.getPhase(),
        "上传槽位 " + slotCode, Map.of("slotCode", slotCode, "fileId", stored.id()));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", row.getId());
    result.put("slotCode", row.getSlotCode());
    result.put("fileId", row.getFileId());
    result.put("filePath", row.getFilePath());
    result.put("fileName", row.getFileName());
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listSlotFiles(Long id) {
    getExisting(id);
    return slotFileMapper.selectList(new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
            .eq(TrainSubsidySlotFileEntity::getSubsidyId, id)
            .eq(TrainSubsidySlotFileEntity::getDeleted, 0)
            .orderByAsc(TrainSubsidySlotFileEntity::getId))
        .stream()
        .map(f -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", f.getId());
          m.put("slotCode", f.getSlotCode());
          m.put("fileId", f.getFileId());
          m.put("filePath", f.getFilePath());
          m.put("fileName", f.getFileName());
          return m;
        })
        .toList();
  }

  @Transactional
  public Map<String, Object> generatePack(Long id) {
    TrainSubsidyEntity entity = getExisting(id);
    Map<String, Object> result = materialPackService.generate(entity);
    mapper.updateById(entity);
    addEvent(entity, "generate", entity.getPhase(), entity.getPhase(),
        Boolean.TRUE.equals(result.get("ok")) ? "材料包生成成功" : "材料包缺项未打包",
        result);
    return result;
  }

  /** 兼容旧接口 */
  @Transactional
  public TrainSubsidyVO submit(Long id) {
    SubsidyTransitionReq req = new SubsidyTransitionReq();
    req.setToPhase("submitted");
    req.setRemark("提交");
    return transition(id, req);
  }

  @Transactional
  public TrainSubsidyVO approve(Long id) {
    SubsidyTransitionReq req = new SubsidyTransitionReq();
    req.setToPhase("approved");
    req.setRemark("审批通过");
    return transition(id, req);
  }

  @Transactional
  public TrainSubsidyVO reject(Long id, String reason) {
    SubsidyTransitionReq req = new SubsidyTransitionReq();
    req.setToPhase("rejected");
    req.setRemark(reason);
    TrainSubsidyEntity entity = getExisting(id);
    entity.setRssbRejectReason(reason);
    mapper.updateById(entity);
    return transition(id, req);
  }

  private void assertTransition(String from, String to) {
    if (from == null) from = "draft";
    boolean ok = switch (from) {
      case "draft" -> Set.of("checking", "ready", "submitted").contains(to);
      case "checking" -> Set.of("draft", "ready", "submitted").contains(to);
      case "ready" -> Set.of("checking", "submitted").contains(to);
      case "submitted" -> Set.of("approved", "rejected").contains(to);
      case "rejected" -> Set.of("draft", "checking", "ready", "submitted").contains(to);
      case "approved" -> Set.of("granting", "granted", "closed").contains(to);
      case "granting" -> Set.of("granted", "closed").contains(to);
      case "granted" -> Set.of("closed").contains(to);
      default -> false;
    };
    if (!ok) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许从 " + from + " 转到 " + to);
    }
  }

  private void assertDraftEditable(TrainSubsidyEntity entity) {
    if (entity.getProfileLocked() != null && entity.getProfileLocked() == 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配方已锁定，如需换年/换版请走「升级重生成」");
    }
    if (!"draft".equals(entity.getPhase()) && !"checking".equals(entity.getPhase())
        && !"rejected".equals(entity.getPhase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可改配方关键字段");
    }
  }

  private Integer resolvePolicyYear(TrainInfoClassEntity clazz, TrainInfoListEntity info) {
    String date = clazz.getDateEnd();
    if (!StringUtils.hasText(date) && info != null) {
      date = info.getEndDate();
    }
    if (StringUtils.hasText(date) && date.length() >= 4) {
      try {
        return Integer.parseInt(date.substring(0, 4));
      } catch (NumberFormatException ignored) {
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "无法从结业日推导政策年，请手工填写 policyYear");
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, classId)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private TrainSubsidyEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSubsidyEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getId, id)
        .eq(TrainSubsidyEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "补贴单不存在");
    return entity;
  }

  private void addEvent(TrainSubsidyEntity entity, String action, String from, String to,
      String remark, Object payload) {
    TrainSubsidyEventEntity e = new TrainSubsidyEventEntity();
    e.setSubsidyId(entity.getId());
    e.setAction(action);
    e.setFromPhase(from);
    e.setToPhase(to);
    e.setRemark(remark);
    e.setOperatorId(SecurityContext.currentUser().getUserId());
    e.setTenantId(entity.getTenantId());
    e.setDeleted(0);
    e.setCreatedAt(LocalDateTime.now());
    if (payload != null) {
      try {
        e.setPayloadJson(objectMapper.writeValueAsString(payload));
      } catch (Exception ignored) {
      }
    }
    eventMapper.insert(e);
  }

  private TrainSubsidyVO toVO(TrainSubsidyEntity entity) {
    String className = null;
    if (entity.getClassId() != null) {
      TrainInfoClassEntity c = classMapper.selectById(entity.getClassId());
      if (c != null) className = c.getTitle();
    }
    return new TrainSubsidyVO(
        entity.getId(), entity.getProjectId(), entity.getClassId(), entity.getTrainInfoId(), className,
        entity.getPolicyTemplate(), entity.getSubsidyAmount(), entity.getStudentCount(),
        entity.getMaterialPackagePath(), entity.getMaterialStatus(), entity.getRssbStatus(),
        entity.getRssbSubmitTime(), entity.getRssbReceiptNo(), entity.getRssbRejectReason(),
        entity.getGrantedAmount(), entity.getGrantTime(), entity.getRemark(), entity.getStatus(),
        entity.getPhase(), entity.getRegionCode(), entity.getRegionName(), entity.getTrainMode(),
        entity.getPolicyYear(), entity.getProfileId(), entity.getProfileVersion(), entity.getProfileLocked(),
        entity.getChecklistJson(), entity.getEvidenceIndexPath(), entity.getSortNumber(),
        entity.getTenantId(), entity.getUserId(), entity.getCreateTime(), entity.getUpdateTime());
  }
}
