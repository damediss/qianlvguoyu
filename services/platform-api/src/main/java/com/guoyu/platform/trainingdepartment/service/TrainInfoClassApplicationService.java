package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainShiftAssignRecordEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskLogEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainShiftAssignRecordMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskLogMapper;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训信息班级管理服务实现
 */
@Service
@Slf4j
public class TrainInfoClassApplicationService {

  private final TrainInfoClassMapper mapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainFileTempMapper trainFileTempMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainPlanRosterMapper planRosterMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainTaskLogMapper taskLogMapper;
  private final TrainShiftAssignRecordMapper shiftAssignRecordMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private final ClassPracticeScopeApplicationService classPracticeScopeApplicationService;
  private final ClassReviewSyncApplicationService classReviewSyncApplicationService;
  private static final DateTimeFormatter ADD_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

  public TrainInfoClassApplicationService(TrainInfoClassMapper mapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainFileTempMapper trainFileTempMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainCertificateMapper certificateMapper,
      TrainPlanRosterMapper planRosterMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainTaskLogMapper taskLogMapper,
      TrainShiftAssignRecordMapper shiftAssignRecordMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainingOperatorScopeService operatorScopeService,
      ClassCollectionApplicationService classCollectionApplicationService,
      ClassPracticeScopeApplicationService classPracticeScopeApplicationService,
      ClassReviewSyncApplicationService classReviewSyncApplicationService) {
    this.mapper = mapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.trainFileTempMapper = trainFileTempMapper;
    this.peopleListMapper = peopleListMapper;
    this.certificateMapper = certificateMapper;
    this.planRosterMapper = planRosterMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.taskLogMapper = taskLogMapper;
    this.shiftAssignRecordMapper = shiftAssignRecordMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.operatorScopeService = operatorScopeService;
    this.classCollectionApplicationService = classCollectionApplicationService;
    this.classPracticeScopeApplicationService = classPracticeScopeApplicationService;
    this.classReviewSyncApplicationService = classReviewSyncApplicationService;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> page(TrainInfoClassQueryReq req) {
    LambdaQueryWrapper<TrainInfoClassEntity> wrapper = new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getDeleted, 0)
        .orderByDesc(TrainInfoClassEntity::getAddtime);
    if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainInfoClassEntity::getTrainInfoId, req.getTrainInfoId());
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getCompanyId());
    if (companyFilter != null) {
      wrapper.eq(TrainInfoClassEntity::getCompanyId, companyFilter);
    }
    if (req.getWorkTypeId() != null && req.getWorkTypeId() != 0) {
      wrapper.eq(TrainInfoClassEntity::getWorkTypeId, req.getWorkTypeId());
    }
    if (req.getCompanyConfirmStatus() != null) {
      wrapper.eq(TrainInfoClassEntity::getCompanyConfirmStatus, req.getCompanyConfirmStatus());
    }
    if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
      wrapper.like(TrainInfoClassEntity::getTitle, req.getKeyword().trim());
    }

    String certFilter = req.getCertFilter() != null ? req.getCertFilter().trim().toLowerCase() : "";
    if ("certified".equals(certFilter) || "uncertified".equals(certFilter)) {
      Set<Long> certifiedClassIds = loadCertifiedClassIds();
      if ("certified".equals(certFilter)) {
        if (certifiedClassIds.isEmpty()) {
          Map<String, Object> empty = new LinkedHashMap<>();
          empty.put("list", List.of());
          empty.put("total", 0);
          empty.put("limit", req.getPageSize());
          return empty;
        }
        wrapper.in(TrainInfoClassEntity::getId, certifiedClassIds);
      } else if (!certifiedClassIds.isEmpty()) {
        wrapper.notIn(TrainInfoClassEntity::getId, certifiedClassIds);
      }
    }

    Page<TrainInfoClassEntity> mpPage = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);

    // Batch resolve names
    List<Long> companyIds = new ArrayList<>();
    List<Long> workTypeIds = new ArrayList<>();
    List<Long> trainInfoIds = new ArrayList<>();
    for (TrainInfoClassEntity e : mpPage.getRecords()) {
      if (e.getCompanyId() != null) companyIds.add(e.getCompanyId());
      if (e.getWorkTypeId() != null) workTypeIds.add(e.getWorkTypeId());
      if (e.getTrainInfoId() != null) trainInfoIds.add(e.getTrainInfoId());
    }

    Map<Long, String> companyNameMap = companyIds.isEmpty() ? Map.of()
        : orgMapper.selectBatchIds(companyIds).stream()
            .collect(Collectors.toMap(o -> o.getId().longValue(), OrgEntity::getName, (a, b) -> a));

    Map<Long, String> workTypeNameMap = workTypeIds.isEmpty() ? Map.of()
        : examWorkTypeMapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .in(ExamWorkTypeEntity::getId, workTypeIds)
            .eq(ExamWorkTypeEntity::getDeleted, 0))
            .stream().collect(Collectors.toMap(e -> e.getId().longValue(), ExamWorkTypeEntity::getTitle, (a, b) -> a));

    Map<Long, TrainInfoListEntity> trainInfoMap = trainInfoIds.isEmpty() ? Map.of()
        : trainInfoListMapper.selectBatchIds(trainInfoIds).stream()
            .collect(Collectors.toMap(TrainInfoListEntity::getId, e -> e, (a, b) -> a));

    List<Long> classIds = mpPage.getRecords().stream().map(TrainInfoClassEntity::getId).toList();
    Map<Long, Map<String, Object>> profileMap = classCollectionApplicationService.batchProfileProgress(classIds);
    Map<Long, Integer> studentNumsMap = countStudentsByClassIds(classIds);
    Map<Long, Integer> certifiedNumsMap = countCertifiedByClassIds(classIds);

    List<Map<String, Object>> list = mpPage.getRecords().stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("title", e.getTitle());
      // 列表人数以花名册实时统计为准，避免开班准备导入后 nums 冗余字段滞后
      int totalNums = studentNumsMap.getOrDefault(e.getId(), 0);
      item.put("nums", totalNums);
      item.put("pass_hours", e.getPassHours());
      item.put("certified_nums", certifiedNumsMap.getOrDefault(e.getId(), 0));
      item.put("addtime", e.getAddtime());
      item.put("addtime_cn", formatAddtime(e.getAddtime()));
      item.put("train_info_id", e.getTrainInfoId());
      item.put("company_id", e.getCompanyId());
      item.put("work_type_id", e.getWorkTypeId());
      item.put("work_type_level", e.getWorkTypeLevel());
      TrainInfoListEntity ti = trainInfoMap.get(e.getTrainInfoId());
      String companyName = e.getCompanyId() != null ? companyNameMap.get(e.getCompanyId()) : null;
      if (!StringUtils.hasText(companyName) && ti != null && StringUtils.hasText(ti.getCompanyName())) {
        companyName = ti.getCompanyName();
      }
      item.put("company_name", companyName);
      item.put("work_type_title", workTypeNameMap.getOrDefault(e.getWorkTypeId(), null));
      item.put("date_start", e.getDateStart());
      item.put("date_end", e.getDateEnd());
      item.put("file_temps", e.getFileTemps());
      item.put("kcbiao_file", e.getKcbiaoFile());
      item.put("kcbiao_src", resolveKcbiaoSrc(e.getKcbiaoFile()));
      item.put("company_contact_name", e.getCompanyContactName());
      item.put("company_contact_phone", e.getCompanyContactPhone());
      item.put("schedule_manual_edited", e.getScheduleManualEdited() != null ? e.getScheduleManualEdited() : 0);
      item.put("schedule_imported_at", e.getScheduleImportedAt());
      item.put("train_title", ti != null ? ti.getTitle() : null);
      item.put("position", ti != null ? ti.getPosition() : null);
      item.put("course_num", ti != null ? ti.getCourseNum() : null);
      item.put("company_confirm_status", e.getCompanyConfirmStatus() != null ? e.getCompanyConfirmStatus() : 0);
      item.put("company_confirm_time", e.getCompanyConfirmTime());
      item.put("company_confirm_username", e.getCompanyConfirmUsername());
      item.put("company_reject_reason", e.getCompanyRejectReason());
      Map<String, Object> profile = profileMap.getOrDefault(e.getId(), Map.of());
      item.put("collection_sign_up_id", profile.get("signUpId"));
      item.put("profile_total", profile.getOrDefault("profile_total", 0));
      item.put("profile_approved", profile.getOrDefault("profile_approved", 0));
      item.put("profile_pending", profile.getOrDefault("profile_pending", 0));
      item.put("profile_ready", profile.getOrDefault("profile_ready", false));
      return item;
    }).toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", mpPage.getTotal());
    result.put("limit", req.getPageSize());
    return result;
  }

  @Transactional(readOnly = true)
  public TrainInfoClassVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public Map<String, Object> save(TrainInfoClassCreateReq req) {
    operatorScopeService.requireOrgOperator();
    if (req.getId() != null && req.getId() > 0) {
      // Update
      TrainInfoClassEntity entity = getExisting(req.getId());
      operatorScopeService.assertClassAccessible(entity);
      entity.setTitle(req.getTitle());
      entity.setNums(req.getNums());
      if (req.getPassHours() != null) {
        entity.setPassHours(req.getPassHours());
      }
      entity.setDateStart(req.getDateStart());
      entity.setDateEnd(req.getDateEnd());
      entity.setKcbiaoFile(req.getKcbiaoFile());
      entity.setCompanyContactName(req.getCompanyContactName());
      entity.setCompanyContactPhone(req.getCompanyContactPhone());
      if (req.getTrainInfoId() != null) {
        entity.setTrainInfoId(req.getTrainInfoId());
        applyCompanyAndWorkTypeFromTrainInfo(entity, req.getTrainInfoId(), req.getWorkTypeId());
      } else if (req.getWorkTypeId() != null && req.getWorkTypeId() > 0) {
        entity.setWorkTypeId(req.getWorkTypeId());
      }
      if (req.getWorkTypeLevel() != null && req.getWorkTypeLevel() > 0) {
        entity.setWorkTypeLevel(req.getWorkTypeLevel());
      }
      mapper.updateById(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      result.put("title", entity.getTitle());
      result.put("nums", entity.getNums());
      return result;
    } else {
      // Create
      TrainInfoClassEntity entity = new TrainInfoClassEntity();
      entity.setTitle(req.getTitle());
      entity.setTrainInfoId(req.getTrainInfoId());
      entity.setPassHours(req.getPassHours());
      entity.setDateStart(req.getDateStart());
      entity.setDateEnd(req.getDateEnd());
      entity.setKcbiaoFile(req.getKcbiaoFile());
      entity.setCompanyContactName(req.getCompanyContactName());
      entity.setCompanyContactPhone(req.getCompanyContactPhone());

      TrainInfoListEntity ti = applyCompanyAndWorkTypeFromTrainInfo(
          entity, req.getTrainInfoId(), req.getWorkTypeId());
      if (req.getWorkTypeLevel() != null && req.getWorkTypeLevel() > 0) {
        entity.setWorkTypeLevel(req.getWorkTypeLevel());
      } else if (ti != null) {
        entity.setWorkTypeLevel(ti.getWorkTypeLevel());
      }
      if (req.getNums() == null || req.getNums() == 0) {
        entity.setNums(ti != null ? ti.getNum() : null);
      } else {
        entity.setNums(req.getNums());
      }
      entity.setAddtime((int) (System.currentTimeMillis() / 1000));
      entity.setDeleted(0);
      entity.setCompanyConfirmStatus(0);
      mapper.insert(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      result.put("title", entity.getTitle());
      result.put("nums", entity.getNums());
      return result;
    }
  }

  @Transactional
  public Map<String, Object> batchDelete(List<Long> ids) {
    operatorScopeService.requireOrgOperator();
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要删除的班级");
    }
    int count = 0;
    Map<String, Object> cascadeTotal = emptyCascadeStats();
    for (Long id : ids) {
      TrainInfoClassEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        operatorScopeService.assertClassAccessible(entity);
        mergeCascadeStats(cascadeTotal, cleanupRelatedOnClassDelete(entity.getId()));
        entity.setDeleted(1);
        mapper.deleteById(entity.getId());
        count++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    result.put("cascade", cascadeTotal);
    return result;
  }

  @Transactional
  public TrainInfoClassVO create(TrainInfoClassCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoClassEntity entity = new TrainInfoClassEntity();
    entity.setTrainInfoId(req.getTrainInfoId());
    entity.setTitle(req.getTitle());
    entity.setNums(req.getNums());
    entity.setPassHours(req.getPassHours());
    entity.setCompanyId(req.getCompanyId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setDateStart(req.getDateStart());
    entity.setDateEnd(req.getDateEnd());
    entity.setAddtime(req.getAddtime());
    entity.setFileTemps(req.getFileTemps());
    entity.setKcbiaoFile(req.getKcbiaoFile());
    entity.setCompanyContactName(req.getCompanyContactName());
    entity.setCompanyContactPhone(req.getCompanyContactPhone());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainInfoClassVO update(Long id, TrainInfoClassUpdateReq req) {
    TrainInfoClassEntity entity = getExisting(id);
    entity.setTitle(req.getTitle());
    entity.setNums(req.getNums());
    if (req.getPassHours() != null) {
      entity.setPassHours(req.getPassHours());
    }
    entity.setDateStart(req.getDateStart());
    entity.setDateEnd(req.getDateEnd());
    entity.setFileTemps(req.getFileTemps());
    entity.setKcbiaoFile(req.getKcbiaoFile());
    entity.setCompanyContactName(req.getCompanyContactName());
    entity.setCompanyContactPhone(req.getCompanyContactPhone());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainInfoClassEntity entity = getExisting(id);
    operatorScopeService.assertClassAccessible(entity);
    cleanupRelatedOnClassDelete(entity.getId());
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  /**
   * 删除班级时的关联清理（保留 platform_user 学员账号）：
   * 1) 花名册软删 2) 名单池回滚待分 3) 本班资料收集计划+人员软删
   * 4) 培训任务软删 5) 排课记录解绑班级
   */
  private Map<String, Object> cleanupRelatedOnClassDelete(Long classId) {
    Map<String, Object> stats = emptyCascadeStats();
    if (classId == null) {
      return stats;
    }
    LocalDateTime now = LocalDateTime.now();

    List<TrainPeopleListEntity> people = peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId));
    for (TrainPeopleListEntity row : people) {
      peopleListMapper.deleteById(row.getId());
    }
    stats.put("peopleRemoved", people.size());

    int rosterReset = planRosterMapper.update(null, new LambdaUpdateWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTrainClassId, classId)
        .eq(TrainPlanRosterEntity::getAssignStatus, 1)
        .set(TrainPlanRosterEntity::getAssignStatus, 0)
        .set(TrainPlanRosterEntity::getTrainClassId, null)
        .set(TrainPlanRosterEntity::getUpdateTime, now));
    stats.put("rosterReset", rosterReset);

    List<TrainSignUpSelfEntity> plans = signUpSelfMapper.selectList(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getTrainClassId, classId)
        .eq(TrainSignUpSelfEntity::getCollectionSource, ClassCollectionApplicationService.SOURCE_CLASS_PREP));
    int collectionPeople = 0;
    for (TrainSignUpSelfEntity plan : plans) {
      List<TrainSignUpPeopleSelfEntity> collectionRows = signUpPeopleSelfMapper.selectList(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, plan.getId()));
      for (TrainSignUpPeopleSelfEntity person : collectionRows) {
        signUpPeopleSelfMapper.deleteById(person.getId());
      }
      collectionPeople += collectionRows.size();
      signUpSelfMapper.deleteById(plan.getId());
    }
    stats.put("collectionPlansRemoved", plans.size());
    stats.put("collectionPeopleRemoved", collectionPeople);

    List<TrainTaskLogEntity> tasks = taskLogMapper.selectList(new LambdaQueryWrapper<TrainTaskLogEntity>()
        .eq(TrainTaskLogEntity::getTrainClassId, classId));
    for (TrainTaskLogEntity task : tasks) {
      taskLogMapper.deleteById(task.getId());
    }
    stats.put("tasksRemoved", tasks.size());

    int shiftUnbound = shiftAssignRecordMapper.update(null, new LambdaUpdateWrapper<TrainShiftAssignRecordEntity>()
        .eq(TrainShiftAssignRecordEntity::getTrainClassId, classId)
        .set(TrainShiftAssignRecordEntity::getTrainClassId, null));
    stats.put("shiftRecordsUnbound", shiftUnbound);

    int practiceScopeRemoved = classPracticeScopeApplicationService.cleanupByClassId(classId);
    stats.put("practiceScopeRemoved", practiceScopeRemoved);
    int reviewSyncRemoved = classReviewSyncApplicationService.cleanupByClassId(classId);
    stats.put("reviewSyncRemoved", reviewSyncRemoved);

    log.info("class delete cascade classId={} stats={}", classId, stats);
    return stats;
  }

  private static Map<String, Object> emptyCascadeStats() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("peopleRemoved", 0);
    stats.put("rosterReset", 0);
    stats.put("collectionPlansRemoved", 0);
    stats.put("collectionPeopleRemoved", 0);
    stats.put("tasksRemoved", 0);
    stats.put("shiftRecordsUnbound", 0);
    stats.put("practiceScopeRemoved", 0);
    stats.put("reviewSyncRemoved", 0);
    return stats;
  }

  private static void mergeCascadeStats(Map<String, Object> total, Map<String, Object> one) {
    for (Map.Entry<String, Object> e : one.entrySet()) {
      int cur = total.get(e.getKey()) instanceof Number n ? n.intValue() : 0;
      int add = e.getValue() instanceof Number n ? n.intValue() : 0;
      total.put(e.getKey(), cur + add);
    }
  }

  /**
   * 挂载文件模板到班级。同名模板只保留一份（新导入覆盖旧挂载）。
   */
  @Transactional
  public Map<String, Object> attachFileTemps(Long classId, List<Long> templateIds) {
    if (templateIds == null || templateIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要挂载的模板");
    }
    TrainInfoClassEntity entity = getExisting(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<Long> distinctIds = templateIds.stream()
        .filter(id -> id != null && id > 0)
        .distinct()
        .collect(Collectors.toList());
    if (distinctIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要挂载的模板");
    }
    List<TrainFileTempEntity> incoming = trainFileTempMapper.selectList(new LambdaQueryWrapper<TrainFileTempEntity>()
        .eq(TrainFileTempEntity::getTenantId, tenantId)
        .eq(TrainFileTempEntity::getDeleted, 0)
        .in(TrainFileTempEntity::getId, distinctIds));
    if (incoming.size() != distinctIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "部分模板不存在或已删除，请刷新后重试");
    }
    List<Long> currentIds = parseFileTemps(entity.getFileTemps());
    Map<Long, String> existingTitleById = new LinkedHashMap<>();
    if (!currentIds.isEmpty()) {
      trainFileTempMapper.selectList(new LambdaQueryWrapper<TrainFileTempEntity>()
              .eq(TrainFileTempEntity::getTenantId, tenantId)
              .eq(TrainFileTempEntity::getDeleted, 0)
              .in(TrainFileTempEntity::getId, currentIds))
          .forEach(t -> existingTitleById.put(t.getId(), t.getTitle()));
    }
    for (TrainFileTempEntity neu : incoming) {
      String title = neu.getTitle();
      currentIds.removeIf(id -> title != null && title.equals(existingTitleById.get(id)));
      if (!currentIds.contains(neu.getId())) {
        currentIds.add(neu.getId());
      }
      existingTitleById.put(neu.getId(), title);
    }
    try {
      entity.setFileTemps(objectMapper.writeValueAsString(currentIds));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化模板ID列表失败");
    }
    mapper.updateById(entity);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("count", distinctIds.size());
    result.put("ids", currentIds);
    result.put("fileTemps", currentIds);
    return result;
  }

  /**
   * 从班级移除文件模板
   */
  @Transactional
  public Map<String, Object> detachFileTemp(Long classId, Long templateId) {
    TrainInfoClassEntity entity = getExisting(classId);
    List<Long> currentIds = parseFileTemps(entity.getFileTemps());
    currentIds.remove(templateId);
    try {
      entity.setFileTemps(objectMapper.writeValueAsString(currentIds));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化模板ID列表失败");
    }
    mapper.updateById(entity);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("count", 1);
    result.put("ids", currentIds);
    result.put("fileTemps", currentIds);
    return result;
  }

  @Transactional
  public Map<String, Object> companyConfirm(Long id) {
    TrainInfoClassEntity entity = getExisting(id);
    assertEnterpriseCanConfirm(entity);
    entity.setCompanyConfirmStatus(1);
    entity.setCompanyConfirmTime(LocalDateTime.now());
    entity.setCompanyConfirmUserId(SecurityContext.currentUser().getUserId());
    entity.setCompanyConfirmUsername(SecurityContext.currentUser().getUsername());
    entity.setCompanyRejectReason(null);
    mapper.updateById(entity);
    return Map.of("id", entity.getId(), "company_confirm_status", 1);
  }

  @Transactional
  public Map<String, Object> companyReject(Long id, String reason) {
    TrainInfoClassEntity entity = getExisting(id);
    assertEnterpriseCanConfirm(entity);
    if (reason == null || reason.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写驳回理由");
    }
    entity.setCompanyConfirmStatus(2);
    entity.setCompanyRejectReason(reason.trim());
    mapper.updateById(entity);
    return Map.of("id", entity.getId(), "company_confirm_status", 2);
  }

  private void assertEnterpriseCanConfirm(TrainInfoClassEntity entity) {
    operatorScopeService.assertClassAccessible(entity);
    Integer status = entity.getCompanyConfirmStatus() != null ? entity.getCompanyConfirmStatus() : 0;
    if (status != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待确认状态的班级可审核");
    }
    TrainingOperatorScopeService.Scope scope = operatorScopeService.currentScope();
    if (!scope.isEnterpriseViewer()) {
      operatorScopeService.requireOrgOperator();
      return;
    }
    if (scope.orgId() == null || entity.getCompanyId() == null || !scope.orgId().equals(entity.getCompanyId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅目标企业可确认该班级");
    }
  }

  private List<Long> parseFileTemps(String fileTemps) {
    if (fileTemps == null || fileTemps.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(fileTemps, new TypeReference<List<Long>>() {});
    } catch (Exception e) {
      // 兼容逗号分隔的旧格式
      List<Long> ids = new ArrayList<>();
      for (String part : fileTemps.replace("[", "").replace("]", "").split(",")) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          try { ids.add(Long.parseLong(trimmed)); } catch (NumberFormatException ignored) {}
        }
      }
      return ids;
    }
  }

  /** 按班级批量统计花名册学员人数（type=student 且未删除） */
  private Map<Long, Integer> countStudentsByClassIds(List<Long> classIds) {
    if (classIds == null || classIds.isEmpty()) {
      return Map.of();
    }
    List<TrainPeopleListEntity> rows = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .select(TrainPeopleListEntity::getId, TrainPeopleListEntity::getTrainClassId)
            .in(TrainPeopleListEntity::getTrainClassId, classIds)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    Map<Long, Integer> map = new HashMap<>();
    for (TrainPeopleListEntity row : rows) {
      if (row.getTrainClassId() == null) {
        continue;
      }
      map.merge(row.getTrainClassId(), 1, Integer::sum);
    }
    return map;
  }

  /** 有回填真实证书的班级 ID 集合（与 certified_nums 口径一致） */
  private Set<Long> loadCertifiedClassIds() {
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .select(
                TrainCertificateEntity::getClassId,
                TrainCertificateEntity::getPeopleId,
                TrainCertificateEntity::getFilePath,
                TrainCertificateEntity::getCertificateType,
                TrainCertificateEntity::getCertificateNo)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .isNotNull(TrainCertificateEntity::getClassId)
            .isNotNull(TrainCertificateEntity::getPeopleId));
    Set<Long> ids = new HashSet<>();
    for (TrainCertificateEntity c : rows) {
      if (c.getClassId() != null && isBackfilledCertificate(c)) {
        ids.add(c.getClassId());
      }
    }
    return ids;
  }

  /** 按班级统计已回填真实证书人数（与毕业典礼证书回填口径一致） */
  private Map<Long, Integer> countCertifiedByClassIds(List<Long> classIds) {
    if (classIds == null || classIds.isEmpty()) {
      return Map.of();
    }
    List<TrainCertificateEntity> rows = certificateMapper.selectList(
        new LambdaQueryWrapper<TrainCertificateEntity>()
            .in(TrainCertificateEntity::getClassId, classIds)
            .eq(TrainCertificateEntity::getDeleted, 0)
            .isNotNull(TrainCertificateEntity::getPeopleId));
    Map<Long, Set<Long>> peopleByClass = new HashMap<>();
    for (TrainCertificateEntity c : rows) {
      if (!isBackfilledCertificate(c)) {
        continue;
      }
      Long classId = c.getClassId();
      Long peopleId = c.getPeopleId();
      if (classId == null || peopleId == null) {
        continue;
      }
      peopleByClass.computeIfAbsent(classId, k -> new HashSet<>()).add(peopleId);
    }
    Map<Long, Integer> map = new HashMap<>();
    for (Map.Entry<Long, Set<Long>> e : peopleByClass.entrySet()) {
      map.put(e.getKey(), e.getValue().size());
    }
    return map;
  }

  private static boolean isBackfilledCertificate(TrainCertificateEntity c) {
    if (c == null) {
      return false;
    }
    if (StringUtils.hasText(c.getFilePath())) {
      return true;
    }
    String type = c.getCertificateType();
    if ("skill_level_cert".equalsIgnoreCase(type) || "renshe".equalsIgnoreCase(type)) {
      return true;
    }
    String no = c.getCertificateNo();
    return StringUtils.hasText(no) && !no.startsWith("GC-");
  }

  private TrainInfoClassEntity getExisting(Long id) {
    TrainInfoClassEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getId, id)
        .eq(TrainInfoClassEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private String formatAddtime(Integer addtime) {
    if (addtime == null || addtime <= 0) {
      return "-";
    }
    return ADD_TIME_FMT.format(Instant.ofEpochSecond(addtime.longValue()));
  }

  private String resolveKcbiaoSrc(String kcbiaoFile) {
    if (kcbiaoFile == null || kcbiaoFile.isBlank()) {
      return "";
    }
    if (kcbiaoFile.startsWith("http://") || kcbiaoFile.startsWith("https://")
        || kcbiaoFile.startsWith("/api/")) {
      return kcbiaoFile;
    }
    return kcbiaoFile.startsWith("/") ? kcbiaoFile : "/" + kcbiaoFile;
  }

  /** 企业、工种跟培训信息；工种等级由班级自己保存 */
  private TrainInfoListEntity applyCompanyAndWorkTypeFromTrainInfo(
      TrainInfoClassEntity entity, Long trainInfoId, Long requestWorkTypeId) {
    if (trainInfoId == null) {
      if (requestWorkTypeId != null && requestWorkTypeId > 0) {
        entity.setWorkTypeId(requestWorkTypeId);
      }
      return null;
    }
    TrainInfoListEntity ti = trainInfoListMapper.selectById(trainInfoId);
    if (ti == null) {
      if (requestWorkTypeId != null && requestWorkTypeId > 0) {
        entity.setWorkTypeId(requestWorkTypeId);
      }
      return null;
    }
    entity.setCompanyId(ti.getCompanyId());
    if (requestWorkTypeId != null && requestWorkTypeId > 0) {
      entity.setWorkTypeId(requestWorkTypeId);
    } else {
      entity.setWorkTypeId(ti.getWorkTypeId());
    }
    return ti;
  }

  private TrainInfoClassVO toVO(TrainInfoClassEntity entity) {
    return new TrainInfoClassVO(
        entity.getId(),
        entity.getTrainInfoId(),
        entity.getTitle(),
        entity.getNums(),
        entity.getPassHours(),
        entity.getCompanyId(),
        entity.getWorkTypeId(),
        entity.getDateStart(),
        entity.getDateEnd(),
        entity.getAddtime(),
        entity.getFileTemps(),
        entity.getKcbiaoFile(),
        entity.getCompanyContactName(),
        entity.getCompanyContactPhone()
    );
  }
}
