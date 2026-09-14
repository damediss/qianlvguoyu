package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.entity.DictItemEntity;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.DictItemMapper;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainBaseDataMapper;
import com.guoyu.platform.trainingdepartment.dto.DemandConvertReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainCreateReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainQueryReq;
import com.guoyu.platform.trainingdepartment.dto.DemandTrainUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.CourseKejianEntity;
import com.guoyu.platform.trainingdepartment.entity.DemandTrainEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.CourseKejianMapper;
import com.guoyu.platform.trainingdepartment.mapper.DemandTrainMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.DemandPendingVO;
import com.guoyu.platform.trainingdepartment.vo.DemandTrainVO;
import com.guoyu.platform.trainingdepartment.vo.TrainStudentVO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DemandTrainApplicationService {

  private final DemandTrainMapper mapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final CourseKejianMapper courseKejianMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final DictMapper dictMapper;
  private final DictItemMapper dictItemMapper;
  private final TrainBaseDataMapper trainBaseDataMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final UserMapper userMapper;

  public DemandTrainApplicationService(DemandTrainMapper mapper, OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper, TrainInfoListMapper trainInfoListMapper,
      CourseKejianMapper courseKejianMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      DictMapper dictMapper,
      DictItemMapper dictItemMapper,
      TrainBaseDataMapper trainBaseDataMapper,
      TrainingOperatorScopeService operatorScopeService,
      UserMapper userMapper) {
    this.mapper = mapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.courseKejianMapper = courseKejianMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.dictMapper = dictMapper;
    this.dictItemMapper = dictItemMapper;
    this.trainBaseDataMapper = trainBaseDataMapper;
    this.operatorScopeService = operatorScopeService;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<DemandTrainVO> page(DemandTrainQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<DemandTrainEntity> wrapper = new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getDeleted, 0)
        .orderByDesc(DemandTrainEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(DemandTrainEntity::getContent, req.getKeyword());
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(DemandTrainEntity::getOrgId, companyFilter);
    } else if (req.getOrgId() != null) {
      wrapper.eq(DemandTrainEntity::getOrgId, req.getOrgId());
    }
    Page<DemandTrainEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);

    // LEFT JOIN train_info_list 获取 solution 状态
    Map<Long, Integer> solutionMap = Map.of();
    List<Long> demandIds = page.getRecords().stream().map(DemandTrainEntity::getId).toList();
    if (!demandIds.isEmpty()) {
      List<TrainInfoListEntity> infoList = trainInfoListMapper.selectList(
          new LambdaQueryWrapper<TrainInfoListEntity>()
              .in(TrainInfoListEntity::getTraindemandId, demandIds)
              .eq(TrainInfoListEntity::getTenantId, tenantId)
              .eq(TrainInfoListEntity::getDeleted, 0));
      solutionMap = infoList.stream()
          .filter(t -> t.getTraindemandId() != null)
          .collect(Collectors.toMap(TrainInfoListEntity::getTraindemandId,
              t -> t.getCompanyHasConfirm() != null ? t.getCompanyHasConfirm() : 0, (a, b) -> a));
    }
    Map<Long, Integer> finalSolutionMap = solutionMap;
    PublisherContext publisherContext = buildPublisherContext(page.getRecords());
    return new PageResponse<>(page.getRecords().stream()
        .map(e -> toVO(e, finalSolutionMap, publisherContext)).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public DemandTrainVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public DemandTrainVO create(DemandTrainCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long orgId = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (orgId == null) {
      orgId = req.getOrgId();
    }
    if (orgId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少企业信息");
    }
    DemandTrainEntity entity = new DemandTrainEntity();
    entity.setTenantId(tenantId);
    entity.setOrgId(orgId);
    entity.setContent(req.getContent());
    entity.setNums(req.getNums());
    entity.setStatus(req.getStatus() != null ? req.getStatus() : 0);
    entity.setAddtime(req.getAddtime() != null ? req.getAddtime() : (int) (System.currentTimeMillis() / 1000));
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    entity.setDeleted(0);
    entity.setCreatedBy(SecurityContext.currentUser().getUserId());
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public DemandTrainVO update(Long id, DemandTrainUpdateReq req) {
    DemandTrainEntity entity = getExisting(id);
    if (req.getOrgId() != null) {
      Long orgId = operatorScopeService.resolveCompanyFilter(req.getOrgId());
      entity.setOrgId(orgId != null ? orgId : req.getOrgId());
    }
    if (StringUtils.hasText(req.getContent())) { entity.setContent(req.getContent()); }
    if (req.getNums() != null) { entity.setNums(req.getNums()); }
    if (req.getStatus() != null) { entity.setStatus(req.getStatus()); }
    if (req.getAddtime() != null) { entity.setAddtime(req.getAddtime()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    DemandTrainEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  @Transactional
  public void batchDelete(List<Long> ids) {
    for (Long id : ids) {
      DemandTrainEntity entity = getExisting(id);
      entity.setDeleted(1);
      mapper.deleteById(entity.getId());
    }
  }

  // ======== 需求管理业务方法 ========

  @Transactional(readOnly = true)
  public PageResponse<DemandPendingVO> getPendings(Integer page, Integer pageSize, String keyword, Integer del) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    int deleted = (del != null) ? del : 0;
    LambdaQueryWrapper<DemandTrainEntity> wrapper = new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getStatus, 0)
        .eq(DemandTrainEntity::getDeleted, deleted)
        .orderByDesc(DemandTrainEntity::getAddtime);
    Long companyFilter = operatorScopeService.resolveCompanyFilter(null);
    if (companyFilter != null) {
      wrapper.eq(DemandTrainEntity::getOrgId, companyFilter);
    }
    if (StringUtils.hasText(keyword)) {
      wrapper.like(DemandTrainEntity::getContent, keyword);
    }
    Page<DemandTrainEntity> result = mapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 30), wrapper);

    // 获取所有企业/工作室信息用于填充（排除 id=1 的系统组织）
    List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .ne(OrgEntity::getId, 1L)
        .in(OrgEntity::getOrgType, "company", "master")
        .eq(OrgEntity::getDeleted, 0));
    Map<Long, OrgEntity> orgMap = orgs.stream()
        .collect(Collectors.toMap(OrgEntity::getId, o -> o, (a, b) -> a));
    Map<String, String> typeLabels = Map.of("master", "技能大师工作室", "company", "企业");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    List<Long> creatorIds = result.getRecords().stream()
        .map(DemandTrainEntity::getCreatedBy)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, String> creatorNames = Map.of();
    if (!creatorIds.isEmpty()) {
      creatorNames = userMapper.selectBatchIds(creatorIds).stream()
          .collect(Collectors.toMap(
              UserEntity::getId,
              u -> StringUtils.hasText(u.getDisplayName()) ? u.getDisplayName() : u.getUsername(),
              (a, b) -> a));
    }

    Map<Long, String> finalCreatorNames = creatorNames;
    List<DemandPendingVO> vos = result.getRecords().stream().map(e -> {
      OrgEntity org = orgMap.get(e.getOrgId());
      String company = org != null ? org.getName() : "-";
      String companyType = "-";
      if (org != null && org.getOrgType() != null) {
        companyType = typeLabels.getOrDefault(org.getOrgType(), org.getOrgType());
      }
      String addtimeCn = "-";
      if (e.getAddtime() != null && e.getAddtime() > 0) {
        addtimeCn = LocalDateTime.ofInstant(Instant.ofEpochSecond(e.getAddtime()), ZoneId.systemDefault())
            .format(fmt);
      }
      String userName = e.getCreatedBy() != null ? finalCreatorNames.get(e.getCreatedBy()) : null;
      String publisher = resolvePublisherName(company, userName);
      return new DemandPendingVO(e.getId(), e.getOrgId(), e.getContent(), e.getNums(),
          e.getStatus(), e.getAddtime(), addtimeCn, company, companyType, publisher);
    }).toList();

    return new PageResponse<>(vos, result.getCurrent(), result.getSize(), result.getTotal());
  }

  /** 发布人展示：公司名 / 用户名；无用户时仅公司名 */
  private static String resolvePublisherName(String company, String userName) {
    boolean hasCompany = StringUtils.hasText(company) && !"-".equals(company);
    boolean hasUser = StringUtils.hasText(userName);
    if (hasCompany && hasUser) {
      return company + " / " + userName;
    }
    if (hasUser) {
      return userName;
    }
    if (hasCompany) {
      return company;
    }
    return "-";
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getPublicData(List<String> tags) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    boolean all = (tags == null || tags.isEmpty());
    Map<String, Object> data = new LinkedHashMap<>();
    Long companyFilter = operatorScopeService.resolveCompanyFilter(null);

    // 企业/工作室列表（排除 id=1 的系统组织）；企业账号仅本企业
    if (all || tags.contains("company")) {
      LambdaQueryWrapper<OrgEntity> orgWrapper = new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, tenantId)
          .ne(OrgEntity::getId, 1L)
          .in(OrgEntity::getOrgType, "company", "master")
          .eq(OrgEntity::getDeleted, 0);
      if (companyFilter != null) {
        orgWrapper.eq(OrgEntity::getId, companyFilter);
      }
      List<OrgEntity> orgs = orgMapper.selectList(orgWrapper);
      List<Map<String, Object>> companyList = orgs.stream().map(o -> {
        Map<String, Object> m = new LinkedHashMap<>();
        // 雪花 ID 必须以字符串下发，否则前端 Number 丢精度导致按企业筛班级为空
        m.put("id", o.getId() == null ? null : String.valueOf(o.getId()));
        m.put("title", o.getName());
        m.put("tag", o.getOrgType());
        return m;
      }).toList();
      data.put("company_list", companyList);
    }

    // 工种列表（查全表，不做 deleted 过滤）
    if (all || tags.contains("worktype")) {
      List<ExamWorkTypeEntity> workTypes = examWorkTypeMapper.selectList(
          new LambdaQueryWrapper<ExamWorkTypeEntity>()
              .orderByAsc(ExamWorkTypeEntity::getParentId));
      Map<Integer, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
      List<Map<String, Object>> workTypeList = new ArrayList<>();
      for (ExamWorkTypeEntity wt : workTypes) {
        if (wt.getParentId() != null && wt.getParentId() > 0) {
          grouped.computeIfAbsent(wt.getParentId(), k -> new ArrayList<>())
              .add(Map.of("id", wt.getId(), "title", wt.getTitle()));
        } else {
          Map<String, Object> group = new LinkedHashMap<>();
          group.put("id", wt.getId());
          group.put("title", wt.getTitle());
          group.put("options", new ArrayList<>());
          workTypeList.add(group);
        }
      }
      for (Map<String, Object> group : workTypeList) {
        Integer groupId = (Integer) group.get("id");
        List<Map<String, Object>> options = grouped.get(groupId);
        if (options != null) {
          group.put("options", options);
        }
      }
      data.put("worktype_list", workTypeList);
    }

    // 培训信息列表（仅 tags 明确指定时返回）；企业账号仅本企业
    if (tags != null && tags.contains("traininfo")) {
      LambdaQueryWrapper<TrainInfoListEntity> trainWrapper = new LambdaQueryWrapper<TrainInfoListEntity>()
          .eq(TrainInfoListEntity::getTenantId, tenantId)
          .eq(TrainInfoListEntity::getDeleted, 0);
      if (companyFilter != null) {
        trainWrapper.eq(TrainInfoListEntity::getCompanyId, companyFilter);
      }
      List<TrainInfoListEntity> trainInfoList = trainInfoListMapper.selectList(trainWrapper);
      List<Map<String, Object>> traininfoList = trainInfoList.stream().map(t -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId() == null ? null : String.valueOf(t.getId()));
        m.put("title", t.getTitle());
        m.put("company_id", t.getCompanyId() == null ? null : String.valueOf(t.getCompanyId()));
        m.put("company_name", t.getCompanyName());
        m.put("work_type_id", t.getWorkTypeId() == null ? null : String.valueOf(t.getWorkTypeId()));
        return m;
      }).toList();
      data.put("traininfo_list", traininfoList);
    }

    // 课件列表（仅 tags 明确指定时返回）
    if (tags != null && tags.contains("kejian")) {
      List<CourseKejianEntity> kejianList = courseKejianMapper.selectList(
          new LambdaQueryWrapper<CourseKejianEntity>()
              .eq(CourseKejianEntity::getDeleted, 0)
              .eq(CourseKejianEntity::getTag, 0)
              .eq(CourseKejianEntity::getOwnerId, 1L)
              .gt(CourseKejianEntity::getWorkTypeId, 0L));
      List<Map<String, Object>> kejianVOList = kejianList.stream().map(k -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", k.getId());
        m.put("title", k.getTitle());
        return m;
      }).toList();
      data.put("kejian_list", kejianVOList);
    }

    // 申报条件（对齐 PHP TrainsController conditionList / dict_id=867）
    if (all || tags.contains("condition")) {
      try {
        data.put("condition_list", loadDeclarationConditions(tenantId));
      } catch (Exception ex) {
        log.warn("load condition_list failed, fallback to empty list", ex);
        data.put("condition_list", List.of());
      }
    }

    return data;
  }

  /** 优先 sys_dict(signup_declaration_condition)，其次 train_base_data 码表 */
  private List<Map<String, Object>> loadDeclarationConditions(Long tenantId) {
    List<Map<String, Object>> fromDict = loadConditionsFromDict(tenantId);
    if (!fromDict.isEmpty()) {
      return fromDict;
    }
    return loadConditionsFromBaseData(tenantId);
  }

  private List<Map<String, Object>> loadConditionsFromDict(Long tenantId) {
    DictEntity dict = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getCode, "signup_declaration_condition")
        .eq(DictEntity::getDeleted, 0)
        .eq(DictEntity::getStatus, "enabled")
        .last("LIMIT 1"));
    if (dict == null) {
      return List.of();
    }
    return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemEntity>()
            .eq(DictItemEntity::getTenantId, tenantId)
            .eq(DictItemEntity::getDictId, dict.getId())
            .eq(DictItemEntity::getDeleted, 0)
            .eq(DictItemEntity::getStatus, "enabled")
            .orderByAsc(DictItemEntity::getSortOrder)
            .orderByAsc(DictItemEntity::getId))
        .stream()
        .map(item -> toConditionMap(item.getId(), item.getItemLabel(), item.getItemValue()))
        .toList();
  }

  /** 回落 train_base_data；共享 PHP 库无 tenant_id/deleted，仅用 legacy 列查询 */
  private List<Map<String, Object>> loadConditionsFromBaseData(Long tenantId) {
    try {
      return trainBaseDataMapper.selectDeclarationConditionsLegacy().stream()
          .filter(row -> StringUtils.hasText(row.title()))
          .map(row -> toConditionMap(row.id(), row.title(), null))
          .toList();
    } catch (Exception ex) {
      log.debug("train_base_data legacy condition query skipped: {}", ex.getMessage());
      return List.of();
    }
  }

  /** condition_list 项：title=全文；value/reqLevel 供前端按预约等级筛选 */
  private static Map<String, Object> toConditionMap(Object id, String title, String itemValue) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", id);
    m.put("title", title);
    if (StringUtils.hasText(itemValue)) {
      m.put("value", itemValue);
      Integer reqLevel = parseConditionReqLevel(itemValue);
      if (reqLevel != null) {
        m.put("reqLevel", reqLevel);
      }
    }
    return m;
  }

  /** item_value 约定：{reqLevel}-{序号}，如 0-01 */
  private static Integer parseConditionReqLevel(String itemValue) {
    int dash = itemValue.indexOf('-');
    if (dash <= 0) {
      return null;
    }
    try {
      return Integer.parseInt(itemValue.substring(0, dash).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  @Transactional
  public Map<String, Object> convertToTraining(DemandConvertReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    long duplicateCount = trainInfoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .eq(TrainInfoListEntity::getTitle, req.getTitle())
        .eq(TrainInfoListEntity::getCompanyId, req.getCompanyId())
        .eq(TrainInfoListEntity::getWorkTypeId, req.getWorkTypeId())
        .eq(TrainInfoListEntity::getWorkTypeLevel, req.getWorkTypeLevel())
        .eq(TrainInfoListEntity::getNum, req.getNum()));
    if (duplicateCount > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已存在相同培训立项记录");
    }

    // Look up company name
    String companyName = "";
    if (req.getCompanyId() != null) {
      OrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, tenantId)
          .eq(OrgEntity::getId, req.getCompanyId())
          .eq(OrgEntity::getDeleted, 0));
      if (org != null) {
        companyName = org.getName();
      }
    }

    TrainInfoListEntity entity = new TrainInfoListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setCompanyId(req.getCompanyId());
    entity.setCompanyName(companyName);
    entity.setTrainCompanyName(req.getTrainCompanyName() != null ? req.getTrainCompanyName() : "千铝工投");
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setNum(req.getNum());
    entity.setPassStatus(1);
    entity.setStatus(0);
    entity.setGroupType(0);
    entity.setSortNumber(1);
    entity.setCreateTime(LocalDateTime.now());
    entity.setDeleted(0);
    if (req.getTraindemandId() != null) {
      entity.setTraindemandId(req.getTraindemandId());
    }
    trainInfoListMapper.insert(entity);

    // 标记原需求为已处理
    if (req.getTraindemandId() != null && req.getTraindemandId() > 0) {
      DemandTrainEntity demand = mapper.selectOne(new LambdaQueryWrapper<DemandTrainEntity>()
          .eq(DemandTrainEntity::getTenantId, tenantId)
          .eq(DemandTrainEntity::getId, req.getTraindemandId())
          .eq(DemandTrainEntity::getDeleted, 0));
      if (demand != null) {
        demand.setStatus(1);
        mapper.updateById(demand);
      }
    }

    return Map.of("id", entity.getId(), "title", entity.getTitle());
  }

  @Transactional(readOnly = true)
  public List<TrainStudentVO> getStudentsByDemandId(Long demandId) {
    getExisting(demandId);
    Long tenantId = SecurityContext.currentUser().getTenantId();

    // Step 1: demand_train → train_info_list
    List<TrainInfoListEntity> infoList = trainInfoListMapper.selectList(
        new LambdaQueryWrapper<TrainInfoListEntity>()
            .eq(TrainInfoListEntity::getTenantId, tenantId)
            .eq(TrainInfoListEntity::getTraindemandId, demandId)
            .eq(TrainInfoListEntity::getDeleted, 0));
    if (infoList.isEmpty()) {
      return List.of();
    }
    List<Long> infoIds = infoList.stream().map(TrainInfoListEntity::getId).toList();

    // Step 2: train_info_list → train_sign_up_self
    List<TrainSignUpSelfEntity> signUpList = signUpSelfMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .in(TrainSignUpSelfEntity::getTrainInfoId, infoIds)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUpList.isEmpty()) {
      return List.of();
    }
    List<Long> signUpIds = signUpList.stream().map(TrainSignUpSelfEntity::getId).toList();

    // Step 3: train_sign_up_self → train_sign_up_people_self
    List<TrainSignUpPeopleSelfEntity> people = signUpPeopleSelfMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .in(TrainSignUpPeopleSelfEntity::getSignUpId, signUpIds)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));

    // 身份证号脱敏：中间8位替换为 *
    return people.stream().map(p -> {
      String idCard = p.getIdCardNumber();
      if (idCard != null && idCard.length() >= 15) {
        idCard = idCard.substring(0, 6) + "********" + idCard.substring(14);
      }
      String gender = p.getGender();
      if (gender == null && idCard != null && idCard.length() >= 17) {
        try {
          int digit = Integer.parseInt(idCard.substring(16, 17));
          gender = digit % 2 == 1 ? "男" : "女";
        } catch (Exception ignored) {
          gender = "-";
        }
      }
      return new TrainStudentVO(p.getId(), p.getName(), p.getPhone(), idCard, gender);
    }).toList();
  }

  private DemandTrainEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DemandTrainEntity entity = mapper.selectOne(new LambdaQueryWrapper<DemandTrainEntity>()
        .eq(DemandTrainEntity::getTenantId, tenantId)
        .eq(DemandTrainEntity::getId, id)
        .eq(DemandTrainEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该培训需求");
    }
    return entity;
  }

  private DemandTrainVO toVO(DemandTrainEntity e) {
    return toVO(e, Map.of(), buildPublisherContext(List.of(e)));
  }

  private DemandTrainVO toVO(DemandTrainEntity e, Map<Long, Integer> solutionMap) {
    return toVO(e, solutionMap, buildPublisherContext(List.of(e)));
  }

  private DemandTrainVO toVO(DemandTrainEntity e, Map<Long, Integer> solutionMap, PublisherContext ctx) {
    Integer solution = solutionMap.get(e.getId());
    String solutionCn;
    if (e.getStatus() == null || e.getStatus() == 0) {
      solutionCn = "-";
    } else if (solution == null) {
      solutionCn = "-";
    } else if (solution == 0) {
      solutionCn = "待确认";
    } else {
      solutionCn = "已确认";
    }
    String publisher = resolvePublisherForEntity(e, ctx);
    return new DemandTrainVO(
        e.getId(),
        e.getOrgId(),
        e.getContent(),
        e.getNums(),
        e.getStatus(),
        e.getAddtime(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime(),
        solution,
        solutionCn,
        publisher
    );
  }

  private PublisherContext buildPublisherContext(List<DemandTrainEntity> records) {
    List<Long> orgIds = records.stream()
        .map(DemandTrainEntity::getOrgId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, OrgEntity> orgMap = orgIds.isEmpty()
        ? Map.of()
        : orgMapper.selectBatchIds(orgIds).stream()
            .collect(Collectors.toMap(OrgEntity::getId, o -> o, (a, b) -> a));

    List<Long> creatorIds = records.stream()
        .map(DemandTrainEntity::getCreatedBy)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    Map<Long, String> creatorNames = creatorIds.isEmpty()
        ? Map.of()
        : userMapper.selectBatchIds(creatorIds).stream()
            .collect(Collectors.toMap(
                UserEntity::getId,
                u -> StringUtils.hasText(u.getDisplayName()) ? u.getDisplayName() : u.getUsername(),
                (a, b) -> a));
    return new PublisherContext(orgMap, creatorNames);
  }

  private String resolvePublisherForEntity(DemandTrainEntity e, PublisherContext ctx) {
    OrgEntity org = e.getOrgId() != null ? ctx.orgMap().get(e.getOrgId()) : null;
    String company = org != null ? org.getName() : "-";
    String userName = e.getCreatedBy() != null ? ctx.creatorNames().get(e.getCreatedBy()) : null;
    return resolvePublisherName(company, userName);
  }

  private record PublisherContext(Map<Long, OrgEntity> orgMap, Map<Long, String> creatorNames) {}
}
