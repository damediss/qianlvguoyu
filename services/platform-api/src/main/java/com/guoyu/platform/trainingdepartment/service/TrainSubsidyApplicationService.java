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
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyPackProfileEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
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
  private final TrainPeopleListMapper peopleListMapper;
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
      TrainPeopleListMapper peopleListMapper,
      TrainSubsidyPackProfileApplicationService profileService,
      TrainSubsidyMaterialPackService materialPackService,
      com.guoyu.platform.file.service.FileApplicationService fileApplicationService) {
    this.mapper = mapper;
    this.eventMapper = eventMapper;
    this.slotFileMapper = slotFileMapper;
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
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
      String kw = req.getKeyword().trim();
      List<Long> matchedClassIds = classMapper.selectList(new LambdaQueryWrapper<TrainInfoClassEntity>()
              .select(TrainInfoClassEntity::getId)
              .eq(TrainInfoClassEntity::getDeleted, 0)
              .like(TrainInfoClassEntity::getTitle, kw)
              .last("LIMIT 80"))
          .stream()
          .map(TrainInfoClassEntity::getId)
          .toList();
      wrapper.and(w -> {
        w.like(TrainSubsidyEntity::getPolicyTemplate, kw)
            .or().like(TrainSubsidyEntity::getRegionName, kw)
            .or().like(TrainSubsidyEntity::getRemark, kw);
        if (!matchedClassIds.isEmpty()) {
          w.or().in(TrainSubsidyEntity::getClassId, matchedClassIds);
        }
      });
    }
    Page<TrainSubsidyEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSubsidyVO getById(Long id) {
    return toVO(getExisting(id));
  }

  /** 本班当前进行中补贴单（排除已关闭）；无则返回 null。 */
  @Transactional(readOnly = true)
  public TrainSubsidyVO getActiveByClassId(Long classId) {
    requireClass(classId);
    TrainSubsidyEntity entity = findOpenByClassId(classId);
    return entity == null ? null : toVO(entity);
  }

  /**
   * 按班幂等取单：已有进行中补贴单则直接返回，否则按默认/入参创建草稿。
   * 地市/政策年尽量从培训单与班级推导；推导失败时用试点默认（来宾×当年）保证一键可开台。
   */
  @Transactional
  public TrainSubsidyVO ensureForClass(Long classId, TrainSubsidyCreateReq req) {
    requireClass(classId);
    TrainSubsidyEntity existing = findOpenByClassId(classId);
    if (existing != null) {
      return toVO(existing);
    }
    TrainSubsidyCreateReq createReq = req != null ? req : new TrainSubsidyCreateReq();
    createReq.setClassId(classId);
    if (!StringUtils.hasText(createReq.getTrainMode())) {
      createReq.setTrainMode(MODE_SKILL);
    }
    return create(createReq, true);
  }

  @Transactional
  public TrainSubsidyVO create(TrainSubsidyCreateReq req) {
    return create(req, false);
  }

  private TrainSubsidyVO create(TrainSubsidyCreateReq req, boolean softDefaults) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainInfoClassEntity clazz = requireClass(req.getClassId());
    assertNoOpenSubsidy(clazz.getId());
    TrainInfoListEntity info = clazz.getTrainInfoId() != null
        ? infoListMapper.selectById(clazz.getTrainInfoId()) : null;

    String trainMode = req.getTrainMode().trim();
    if (!MODE_SKILL.equals(trainMode) && !"apprentice".equals(trainMode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "trainMode 仅支持 skill_upgrade / apprentice");
    }
    if ("apprentice".equals(trainMode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "本期不做新型学徒（D16）");
    }

    String[] region = resolveRegionPair(req, info, softDefaults);
    String regionCode = region[0];
    String regionName = region[1];

    Integer policyYear = req.getPolicyYear();
    if (policyYear == null) {
      policyYear = resolvePolicyYear(clazz, info, softDefaults);
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
      profile = profileService.resolveActive(regionCode, regionName, trainMode, policyYear, softDefaults);
      // 软回退时以实际命中的配方对齐地市/政策年，避免单上字段与配方不一致
      if (softDefaults && profile.getRegionCode() != null) {
        regionCode = profile.getRegionCode();
        regionName = profile.getRegionName() != null ? profile.getRegionName() : regionCode;
      }
      if (softDefaults && profile.getPolicyYear() != null) {
        policyYear = profile.getPolicyYear();
      }
    }

    Integer studentCount = req.getStudentCount();
    if (studentCount == null) {
      studentCount = countClassStudents(clazz.getId(), tenantId);
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
    entity.setStudentCount(studentCount);
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
    addEvent(entity, "create", null, "draft",
        softDefaults ? "创建补贴单（按班自动推导）" : "创建补贴单", null);
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
  public void deleteSlotFile(Long subsidyId, Long slotFileId) {
    TrainSubsidyEntity entity = getExisting(subsidyId);
    String phase = entity.getPhase();
    if (!"draft".equals(phase) && !"checking".equals(phase) && !"ready".equals(phase) && !"rejected".equals(phase)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可删除槽位附件");
    }
    TrainSubsidySlotFileEntity row = slotFileMapper.selectOne(new LambdaQueryWrapper<TrainSubsidySlotFileEntity>()
        .eq(TrainSubsidySlotFileEntity::getId, slotFileId)
        .eq(TrainSubsidySlotFileEntity::getSubsidyId, subsidyId)
        .eq(TrainSubsidySlotFileEntity::getDeleted, 0));
    if (row == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "槽位附件不存在");
    }
    slotFileMapper.deleteById(row.getId());
    addEvent(entity, "delete_slot", entity.getPhase(), entity.getPhase(),
        "删除槽位附件 " + row.getSlotCode(), Map.of("slotCode", row.getSlotCode(), "fileId", row.getFileId()));
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
    return resolvePolicyYear(clazz, info, false);
  }

  private Integer resolvePolicyYear(TrainInfoClassEntity clazz, TrainInfoListEntity info, boolean softDefaults) {
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
    if (softDefaults) {
      return java.time.Year.now().getValue();
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "无法从结业日推导政策年，请手工填写 policyYear");
  }

  /**
   * @return [regionCode, regionName]
   */
  private String[] resolveRegionPair(TrainSubsidyCreateReq req, TrainInfoListEntity info, boolean softDefaults) {
    String regionCode = StringUtils.hasText(req.getRegionCode()) ? req.getRegionCode().trim() : null;
    String regionName = StringUtils.hasText(req.getRegionName()) ? req.getRegionName().trim() : null;
    if (!StringUtils.hasText(regionName) && info != null && StringUtils.hasText(info.getCityName())) {
      regionName = info.getCityName().trim();
    }
    if (!StringUtils.hasText(regionCode) && info != null && info.getCityId() != null) {
      // 纯数字 cityId 不能当配方 regionCode，优先用地名归一
      regionCode = String.valueOf(info.getCityId());
    }
    String[] normalized = normalizePilotRegion(regionCode, regionName);
    if (normalized != null) {
      return normalized;
    }
    if (StringUtils.hasText(regionName) || StringUtils.hasText(regionCode)) {
      if (!StringUtils.hasText(regionCode)) regionCode = regionName;
      if (!StringUtils.hasText(regionName)) regionName = regionCode;
      // 若 code 仍是纯数字，用 name 作 code 以免配方匹配失败
      if (regionCode.chars().allMatch(Character::isDigit) && StringUtils.hasText(regionName)) {
        regionCode = regionName;
      }
      return new String[] { regionCode, regionName };
    }
    if (softDefaults) {
      return new String[] { "来宾", "来宾市" };
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST,
        "无法解析申报地市：请在培训单维护城市，或在新建补贴单时手工选择来宾/玉林/南丹");
  }

  /** 试点三市归一：来宾 / 玉林 / 河池南丹 */
  private String[] normalizePilotRegion(String code, String name) {
    String blob = ((name != null ? name : "") + " " + (code != null ? code : "")).trim();
    if (!StringUtils.hasText(blob)) return null;
    if (blob.contains("来宾")) return new String[] { "来宾", "来宾市" };
    if (blob.contains("玉林")) return new String[] { "玉林", "玉林市" };
    if (blob.contains("南丹") || (blob.contains("河池") && blob.contains("南丹"))) {
      return new String[] { "河池南丹", "河池市南丹县" };
    }
    if (blob.contains("河池")) return new String[] { "河池南丹", "河池市南丹县" };
    return null;
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

  /** 进行中：未删除且 phase ≠ closed（一班一单口径）。 */
  private TrainSubsidyEntity findOpenByClassId(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return mapper.selectOne(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getClassId, classId)
        .eq(TrainSubsidyEntity::getDeleted, 0)
        .ne(TrainSubsidyEntity::getPhase, "closed")
        .orderByDesc(TrainSubsidyEntity::getUpdateTime)
        .orderByDesc(TrainSubsidyEntity::getId)
        .last("LIMIT 1"));
  }

  private void assertNoOpenSubsidy(Long classId) {
    TrainSubsidyEntity open = findOpenByClassId(classId);
    if (open != null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "该班级已有进行中的补贴单（#" + open.getId() + "），请勿重复创建；可关闭后再建新单");
    }
  }

  private Integer countClassStudents(Long classId, Long tenantId) {
    long count = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0));
    return (int) count;
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
