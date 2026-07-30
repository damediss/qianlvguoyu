package com.guoyu.platform.trainingdepartment.service.shiftassign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignBindClassReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignGenerateMaterializeReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignGenerateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignMaterializeReq;
import com.guoyu.platform.trainingdepartment.dto.TrainShiftAssignPatchReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainShiftAssignRecordEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainShiftAssignTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainShiftAssignRecordMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainShiftAssignTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMapper;
import com.guoyu.platform.trainingdepartment.service.TrainingOperatorScopeService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.config.EffectiveSchedulingConfig;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ClassAssignResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ScheduleParseResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ShiftTimeDef;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ExcelScheduleParseService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ScheduleAlgorithmService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ShiftTimeParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ShiftAssignApplicationService {

  @Value("${platform.training.division.require-roster:true}")
  private boolean requireRoster;

  private final TrainThreeYearPlanMapper planMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainInfoClassMapper trainInfoClassMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainPlanRosterMapper rosterMapper;
  private final TrainShiftAssignTaskMapper taskMapper;
  private final TrainShiftAssignRecordMapper recordMapper;
  private final FileApplicationService fileApplicationService;
  private final ExcelScheduleParseService excelParseService;
  private final ScheduleAlgorithmService algorithmService;
  private final TrainingOperatorScopeService scopeService;
  private final ObjectMapper objectMapper;

  /** T0 Adapter：直通预览（解析人日槽 + 警告，不落库） */
  @Transactional(readOnly = true)
  public Map<String, Object> previewAdapter(TrainShiftAssignGenerateReq req) {
    RunBundle bundle = prepareAndParse(req, false);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("adapterType", bundle.adapterType);
    result.put("year", req.getYear());
    result.put("month", req.getMonth());
    result.put("personCount", countPersons(bundle.parse));
    result.put("slotCount", bundle.parse.getPersonDaySlots() == null ? 0 : bundle.parse.getPersonDaySlots().size());
    result.put("warnings", bundle.warnings);
    result.put("unrecognizedShifts", bundle.parse.getUnrecognizedShiftList());
    result.put("failedExtractRows", bundle.parse.getFailedExtractRows());
    return result;
  }

  /** 预检：跑引擎但不落正式结果 */
  @Transactional(readOnly = true)
  public Map<String, Object> precheck(TrainShiftAssignGenerateReq req) {
    RunBundle bundle = prepareAndParse(req, false);
    ClassAssignResult assign = algorithmService.runClassAssign(bundle.parse, bundle.config);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("adapterType", bundle.adapterType);
    result.put("year", req.getYear());
    result.put("month", req.getMonth());
    result.put("warnings", mergeWarnings(bundle.warnings, assign));
    result.put("algoClassNames", distinctAlgoClasses(assign));
    result.put("dailyStats", toDailyStatsView(assign));
    result.put("unassignedPersons", toUnassignedView(assign));
    result.put("recordCount", countRecords(assign));
    result.put("theoryDays", assign.getTheoryDays());
    result.put("practiceDays", assign.getPracticeDays());
    return result;
  }

  @Transactional
  public Map<String, Object> generate(TrainShiftAssignGenerateReq req) {
    RunBundle bundle = prepareAndParse(req, true);
    ClassAssignResult assign = algorithmService.runClassAssign(bundle.parse, bundle.config);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LocalDateTime now = LocalDateTime.now();

    TrainShiftAssignTaskEntity task = new TrainShiftAssignTaskEntity();
    task.setTenantId(tenantId);
    task.setPlanId(bundle.plan.getId());
    task.setTrainInfoId(req.getTrainInfoId());
    task.setYear(req.getYear());
    task.setMonth(req.getMonth());
    task.setSourceShiftFileId(bundle.plan.getEnterpriseShiftFileId());
    task.setAdapterType(bundle.adapterType);
    task.setStatus("generated");
    task.setVersion(1);
    task.setWarningJson(writeJson(mergeWarnings(bundle.warnings, assign)));
    task.setConfigJson(writeJson(Map.of(
        "classMin", bundle.config.getClassMin(),
        "classMax", bundle.config.getClassMax(),
        "morning", bundle.config.getMorningStart() + "-" + bundle.config.getMorningEnd(),
        "afternoon", bundle.config.getAfternoonStart() + "-" + bundle.config.getAfternoonEnd()
    )));
    task.setUnassignedJson(writeJson(toUnassignedView(assign)));
    task.setTheoryDaysJson(writeJson(assign.getTheoryDays()));
    task.setPracticeDaysJson(writeJson(assign.getPracticeDays()));
    task.setDeleted(0);
    task.setCreateTime(now);
    task.setUpdateTime(now);
    taskMapper.insert(task);

    List<TrainShiftAssignRecordEntity> records = flattenRecords(tenantId, task.getId(), assign);
    for (TrainShiftAssignRecordEntity r : records) {
      recordMapper.insert(r);
    }

    Map<String, Object> result = taskResultMap(task, records, assign);
    result.put("created", true);
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getTask(Long taskId) {
    TrainShiftAssignTaskEntity task = requireTask(taskId);
    List<TrainShiftAssignRecordEntity> records = recordMapper.selectList(
        new LambdaQueryWrapper<TrainShiftAssignRecordEntity>()
            .eq(TrainShiftAssignRecordEntity::getTenantId, task.getTenantId())
            .eq(TrainShiftAssignRecordEntity::getTaskId, taskId)
            .orderByAsc(TrainShiftAssignRecordEntity::getAssignDate)
            .orderByAsc(TrainShiftAssignRecordEntity::getSlot)
            .orderByAsc(TrainShiftAssignRecordEntity::getAlgoClassName)
            .orderByAsc(TrainShiftAssignRecordEntity::getPersonName));
    return taskResultMap(task, records, null);
  }

  @Transactional
  public Map<String, Object> patch(Long taskId, TrainShiftAssignPatchReq req) {
    TrainShiftAssignTaskEntity task = requireTask(taskId);
    if ("materialized".equalsIgnoreCase(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已物化为培训班，禁止调课；请重新生成任务");
    }
    if (!Objects.equals(task.getVersion(), req.getVersion())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已被他人修改，请刷新后重试");
    }
    Long tenantId = task.getTenantId();
    for (TrainShiftAssignPatchReq.Change change : req.getChanges()) {
      LocalDate date = LocalDate.parse(change.getAssignDate());
      String slot = resolveSlot(change.getSlot(), change.getAlgoClassName());
      LambdaQueryWrapper<TrainShiftAssignRecordEntity> q = new LambdaQueryWrapper<TrainShiftAssignRecordEntity>()
          .eq(TrainShiftAssignRecordEntity::getTenantId, tenantId)
          .eq(TrainShiftAssignRecordEntity::getTaskId, taskId)
          .eq(TrainShiftAssignRecordEntity::getPersonName, change.getPersonName().trim())
          .eq(TrainShiftAssignRecordEntity::getAssignDate, date)
          .eq(TrainShiftAssignRecordEntity::getSlot, slot);
      if (StringUtils.hasText(change.getEmployeeNo())) {
        q.eq(TrainShiftAssignRecordEntity::getEmployeeNo, change.getEmployeeNo().trim());
      }
      List<TrainShiftAssignRecordEntity> matched = recordMapper.selectList(q);
      if (!StringUtils.hasText(change.getAlgoClassName())) {
        for (TrainShiftAssignRecordEntity row : matched) {
          recordMapper.deleteById(row.getId());
        }
        continue;
      }
      if (matched.isEmpty()) {
        TrainShiftAssignRecordEntity row = new TrainShiftAssignRecordEntity();
        row.setTenantId(tenantId);
        row.setTaskId(taskId);
        row.setPersonName(change.getPersonName().trim());
        row.setEmployeeNo(StringUtils.hasText(change.getEmployeeNo()) ? change.getEmployeeNo().trim() : null);
        row.setAssignDate(date);
        row.setSlot(slot);
        row.setAlgoClassName(change.getAlgoClassName().trim());
        row.setDeleted(0);
        recordMapper.insert(row);
      } else {
        for (TrainShiftAssignRecordEntity row : matched) {
          row.setAlgoClassName(change.getAlgoClassName().trim());
          row.setSlot(slot);
          recordMapper.updateById(row);
        }
      }
    }
    task.setVersion(task.getVersion() == null ? 2 : task.getVersion() + 1);
    task.setStatus("patched");
    task.setUpdateTime(LocalDateTime.now());
    taskMapper.updateById(task);
    return getTask(taskId);
  }

  @Transactional
  public Map<String, Object> generateAndMaterialize(TrainShiftAssignGenerateMaterializeReq req) {
    TrainShiftAssignGenerateReq gen = new TrainShiftAssignGenerateReq();
    gen.setPlanId(req.getPlanId());
    gen.setTrainInfoId(req.getTrainInfoId());
    gen.setYear(req.getYear());
    gen.setMonth(req.getMonth());
    gen.setAdapterType(req.getAdapterType());
    gen.setClassMin(req.getClassMin());
    gen.setClassMax(req.getClassMax());
    Map<String, Object> generated = generate(gen);
    Long taskId = ((Number) generated.get("taskId")).longValue();

    TrainShiftAssignMaterializeReq mat = new TrainShiftAssignMaterializeReq();
    mat.setTrainInfoId(req.getTrainInfoId());
    mat.setMode(req.getMode());
    mat.setSubmitForCompanyConfirm(req.getSubmitForCompanyConfirm());
    mat.setClassNamePrefix(req.getClassNamePrefix());
    mat.setOnlyUnbound(req.getOnlyUnbound());
    mat.setMatchRoster(req.getMatchRoster());
    Map<String, Object> materialized = materialize(taskId, mat);

    Map<String, Object> result = new LinkedHashMap<>(generated);
    result.putAll(materialized);
    result.put("taskId", taskId);
    result.put("generatedAndMaterialized", true);
    return result;
  }

  /**
   * 将算法班物化为 train_info_class + train_people_list，并回写 record.train_class_id。
   */
  @Transactional
  public Map<String, Object> materialize(Long taskId, TrainShiftAssignMaterializeReq req) {
    TrainShiftAssignTaskEntity task = requireTask(taskId);
    Long tenantId = task.getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoEntity(req.getTrainInfoId(), task.getPlanId());
    if (task.getTrainInfoId() == null) {
      task.setTrainInfoId(trainInfo.getId());
    } else if (!Objects.equals(task.getTrainInfoId(), trainInfo.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "物化培训信息与任务不一致");
    }

    String mode = StringUtils.hasText(req.getMode()) ? req.getMode().trim() : "algo_class_as_train_class";
    if (!"algo_class_as_train_class".equalsIgnoreCase(mode) && !"bind_existing".equalsIgnoreCase(mode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的物化 mode：" + mode);
    }
    boolean onlyUnbound = req.getOnlyUnbound() == null || Boolean.TRUE.equals(req.getOnlyUnbound());
    boolean matchRoster = req.getMatchRoster() == null || Boolean.TRUE.equals(req.getMatchRoster());
    boolean submitConfirm = req.getSubmitForCompanyConfirm() == null
        || Boolean.TRUE.equals(req.getSubmitForCompanyConfirm());
    String prefix = StringUtils.hasText(req.getClassNamePrefix())
        ? req.getClassNamePrefix().trim()
        : buildMaterializePrefix(trainInfo, task);

    List<TrainShiftAssignRecordEntity> records = recordMapper.selectList(
        new LambdaQueryWrapper<TrainShiftAssignRecordEntity>()
            .eq(TrainShiftAssignRecordEntity::getTenantId, tenantId)
            .eq(TrainShiftAssignRecordEntity::getTaskId, taskId));
    if (records.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务无排课明细，无法物化");
    }

    Map<String, TrainPlanRosterEntity> rosterByIdCard = new HashMap<>();
    Map<String, TrainPlanRosterEntity> rosterByName = new HashMap<>();
    if (matchRoster) {
      List<TrainPlanRosterEntity> rosterRows = rosterMapper.selectList(
          new LambdaQueryWrapper<TrainPlanRosterEntity>()
              .eq(TrainPlanRosterEntity::getTenantId, tenantId)
              .eq(TrainPlanRosterEntity::getPlanId, task.getPlanId()));
      for (TrainPlanRosterEntity r : rosterRows) {
        if (StringUtils.hasText(r.getIdCard())) {
          rosterByIdCard.putIfAbsent(r.getIdCard().trim().toUpperCase(), r);
        }
        if (StringUtils.hasText(r.getPersonName())) {
          rosterByName.putIfAbsent(r.getPersonName().trim(), r);
        }
      }
    }

    Map<String, List<TrainShiftAssignRecordEntity>> byAlgo = records.stream()
        .filter(r -> StringUtils.hasText(r.getAlgoClassName()))
        .collect(Collectors.groupingBy(
            r -> r.getAlgoClassName().trim(),
            LinkedHashMap::new,
            Collectors.toList()));

    List<Map<String, Object>> classes = new ArrayList<>();
    List<Map<String, Object>> warnings = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    Set<String> claimedIdCards = new HashSet<>();

    for (Map.Entry<String, List<TrainShiftAssignRecordEntity>> entry : byAlgo.entrySet()) {
      String algoName = entry.getKey();
      List<TrainShiftAssignRecordEntity> algoRecords = entry.getValue();
      Long existingBind = algoRecords.stream()
          .map(TrainShiftAssignRecordEntity::getTrainClassId)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);

      if (onlyUnbound && existingBind != null && "algo_class_as_train_class".equalsIgnoreCase(mode)) {
        warnings.add(Map.of(
            "level", "INFO",
            "code", "SKIP_BOUND",
            "message", algoName + " 已绑定班级 #" + existingBind + "，已跳过"));
        continue;
      }

      TrainInfoClassEntity clazz;
      if ("bind_existing".equalsIgnoreCase(mode)) {
        if (existingBind == null) {
          warnings.add(Map.of(
              "level", "WARNING",
              "code", "NO_BIND",
              "message", algoName + " 尚未绑定培训班，bind_existing 已跳过"));
          continue;
        }
        clazz = trainInfoClassMapper.selectById(existingBind);
        if (clazz == null || !Objects.equals(clazz.getTrainInfoId(), trainInfo.getId())) {
          warnings.add(Map.of(
              "level", "WARNING",
              "code", "BAD_BIND",
              "message", algoName + " 绑定班级无效，已跳过"));
          continue;
        }
      } else if (existingBind != null && !onlyUnbound) {
        clazz = trainInfoClassMapper.selectById(existingBind);
        if (clazz == null) {
          clazz = createTrainClass(trainInfo, prefix + algoName, submitConfirm);
        } else {
          clazz.setTitle(prefix + algoName);
          if (submitConfirm) clazz.setCompanyConfirmStatus(0);
          trainInfoClassMapper.updateById(clazz);
        }
      } else {
        clazz = createTrainClass(trainInfo, prefix + algoName, submitConfirm);
      }

      Map<String, TrainShiftAssignRecordEntity> uniquePersons = new LinkedHashMap<>();
      for (TrainShiftAssignRecordEntity r : algoRecords) {
        String key = personUniqueKey(r);
        uniquePersons.putIfAbsent(key, r);
      }

      int added = 0;
      for (TrainShiftAssignRecordEntity r : uniquePersons.values()) {
        String idCard = resolveIdCard(r, rosterByIdCard, rosterByName);
        String phone = null;
        TrainPlanRosterEntity matched = null;
        if (StringUtils.hasText(idCard)) {
          matched = rosterByIdCard.get(idCard.trim().toUpperCase());
        }
        if (matched == null && StringUtils.hasText(r.getPersonName())) {
          matched = rosterByName.get(r.getPersonName().trim());
        }
        if (matched != null) {
          if (!StringUtils.hasText(idCard) && StringUtils.hasText(matched.getIdCard())) {
            idCard = matched.getIdCard().trim();
          }
          phone = matched.getPhone();
        }
        if (!StringUtils.hasText(idCard)) {
          warnings.add(Map.of(
              "level", "WARNING",
              "code", "NO_ID_CARD",
              "message", r.getPersonName() + " 无身份证，已写入学员但请人工核对"));
        } else {
          String idKey = idCard.trim().toUpperCase();
          if (claimedIdCards.contains(idKey)) {
            warnings.add(Map.of(
                "level", "WARNING",
                "code", "ID_CARD_MULTI_CLASS",
                "message", r.getPersonName() + "/" + idCard + " 已分入其他算法班，本班跳过"));
            continue;
          }
          Long exists = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
              .eq(TrainPeopleListEntity::getTenantId, tenantId)
              .eq(TrainPeopleListEntity::getTrainId, trainInfo.getId())
              .eq(TrainPeopleListEntity::getIdCard, idCard.trim())
              .ne(TrainPeopleListEntity::getType, "teacher"));
          if (exists != null && exists > 0) {
            warnings.add(Map.of(
                "level", "WARNING",
                "code", "ALREADY_IN_CLASS",
                "message", r.getPersonName() + "/" + idCard + " 已在该培训信息班级中，跳过"));
            claimedIdCards.add(idKey);
            continue;
          }
          claimedIdCards.add(idKey);
        }

        TrainPeopleListEntity people = new TrainPeopleListEntity();
        people.setTenantId(tenantId);
        people.setTrainId(trainInfo.getId());
        people.setTrainClassId(clazz.getId());
        people.setType("student");
        people.setRealname(r.getPersonName());
        people.setPhone(phone);
        people.setIdCard(StringUtils.hasText(idCard) ? idCard.trim() : null);
        people.setDeleted(0);
        people.setCreateTime(now);
        peopleListMapper.insert(people);
        added++;
      }

      clazz.setNums((clazz.getNums() == null ? 0 : clazz.getNums()) + added);
      // 首化物化用去重人数覆盖
      clazz.setNums(added);
      trainInfoClassMapper.updateById(clazz);

      recordMapper.update(null, new LambdaUpdateWrapper<TrainShiftAssignRecordEntity>()
          .eq(TrainShiftAssignRecordEntity::getTenantId, tenantId)
          .eq(TrainShiftAssignRecordEntity::getTaskId, taskId)
          .eq(TrainShiftAssignRecordEntity::getAlgoClassName, algoName)
          .set(TrainShiftAssignRecordEntity::getTrainClassId, clazz.getId()));

      Map<String, Object> classRow = new LinkedHashMap<>();
      classRow.put("algoClassName", algoName);
      classRow.put("classId", clazz.getId());
      classRow.put("title", clazz.getTitle());
      classRow.put("nums", added);
      classes.add(classRow);
    }

    if (classes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可物化的算法班（可能均已绑定或无明细）");
    }

    task.setTrainInfoId(trainInfo.getId());
    task.setStatus("materialized");
    task.setUpdateTime(now);
    // 追加物化警告到 warning_json
    List<Map<String, Object>> allWarnings = readJson(task.getWarningJson(),
        new TypeReference<List<Map<String, Object>>>() {});
    if (allWarnings == null) allWarnings = new ArrayList<>();
    allWarnings.addAll(warnings);
    task.setWarningJson(writeJson(allWarnings));
    taskMapper.updateById(task);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("taskId", taskId);
    result.put("trainInfoId", trainInfo.getId());
    result.put("status", task.getStatus());
    result.put("classCount", classes.size());
    result.put("classes", classes);
    result.put("warnings", warnings);
    result.put("submitForCompanyConfirm", submitConfirm);
    return result;
  }

  @Transactional
  public Map<String, Object> bindClass(Long taskId, TrainShiftAssignBindClassReq req) {
    TrainShiftAssignTaskEntity task = requireTask(taskId);
    if ("materialized".equalsIgnoreCase(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已物化，请通过培训班级管理调整绑定");
    }
    TrainInfoClassEntity clazz = trainInfoClassMapper.selectById(req.getTrainClassId());
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训班级不存在");
    }
    if (task.getTrainInfoId() != null && !Objects.equals(clazz.getTrainInfoId(), task.getTrainInfoId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级不属于当前任务关联的培训信息");
    }
    String algo = req.getAlgoClassName().trim();
    LambdaUpdateWrapper<TrainShiftAssignRecordEntity> u = new LambdaUpdateWrapper<TrainShiftAssignRecordEntity>()
        .eq(TrainShiftAssignRecordEntity::getTenantId, task.getTenantId())
        .eq(TrainShiftAssignRecordEntity::getTaskId, taskId)
        .eq(TrainShiftAssignRecordEntity::getAlgoClassName, algo)
        .set(TrainShiftAssignRecordEntity::getTrainClassId, req.getTrainClassId());
    int updated = recordMapper.update(null, u);
    if (updated == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到算法班：" + algo);
    }
    task.setUpdateTime(LocalDateTime.now());
    taskMapper.updateById(task);
    Map<String, Object> result = getTask(taskId);
    result.put("boundAlgoClassName", algo);
    result.put("boundTrainClassId", req.getTrainClassId());
    result.put("boundRecordCount", updated);
    return result;
  }

  private RunBundle prepareAndParse(TrainShiftAssignGenerateReq req, boolean requireTrainInfoOptional) {
    TrainThreeYearPlanEntity plan = requirePlan(req.getPlanId());
    assertReturnReady(plan);
    if (req.getTrainInfoId() != null) {
      requireTrainInfo(req.getTrainInfoId(), plan.getId());
    } else if (requireTrainInfoOptional) {
      // generate 允许无 trainInfoId，但建议前端带上
    }
    String adapterType = StringUtils.hasText(req.getAdapterType()) ? req.getAdapterType().trim() : "T0";
    if (!"T0".equalsIgnoreCase(adapterType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "二期仅支持 adapterType=T0（标准人日直通），异构 Adapter 属三期");
    }
    byte[] bytes = fileApplicationService.readFileBytesById(plan.getEnterpriseShiftFileId());
    Map<String, ShiftTimeDef> shiftMap = ShiftAssignDefaults.defaultShiftTimeMap();
    // 文档兼容：早/早班 → 白班规则
    ensureAlias(shiftMap, "早", true, true, false);
    ensureAlias(shiftMap, "早班", true, true, false);

    ScheduleParseResult parse;
    try {
      parse = excelParseService.parseBytes(
          bytes,
          plan.getEnterpriseShiftFileName(),
          String.valueOf(plan.getTargetOrgId()),
          req.getYear(),
          req.getMonth(),
          shiftMap);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析排班表失败：" + ex.getMessage());
    }
    if (parse.getPersonDaySlots() == null || parse.getPersonDaySlots().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "排班表未解析到有效人日数据，请确认是否为标准「姓名+日期列」格式");
    }

    EffectiveSchedulingConfig cfg = ShiftAssignDefaults.config();
    if (req.getClassMin() != null) cfg.setClassMin(req.getClassMin());
    if (req.getClassMax() != null) cfg.setClassMax(req.getClassMax());

    List<Map<String, Object>> warnings = new ArrayList<>();
    if (parse.getUnrecognizedShiftList() != null) {
      for (String s : parse.getUnrecognizedShiftList()) {
        warnings.add(Map.of("level", "WARNING", "code", "UNKNOWN_SHIFT", "message", s));
      }
    }
    if (parse.getFailedExtractRows() != null) {
      for (String s : parse.getFailedExtractRows()) {
        warnings.add(Map.of("level", "WARNING", "code", "FAILED_ROW", "message", s));
      }
    }
    return new RunBundle(plan, parse, cfg, warnings, "T0");
  }

  private void ensureAlias(Map<String, ShiftTimeDef> map, String name, boolean am, boolean pm, boolean night) {
    String key = ShiftTimeParser.normalizeShiftNameForLookup(name);
    if (map.containsKey(key) || map.containsKey(name)) return;
    ShiftTimeDef def = new ShiftTimeDef();
    def.setShiftName(name);
    def.setAllowMorning(am);
    def.setAllowAfternoon(pm);
    def.setNextDayBlock(night ? 3 : 0);
    def.setCrossDay(night);
    map.put(key, def);
    map.put(name, def);
  }

  private List<TrainShiftAssignRecordEntity> flattenRecords(
      Long tenantId, Long taskId, ClassAssignResult assign) {
    List<TrainShiftAssignRecordEntity> list = new ArrayList<>();
    if (assign.getPersonDayClass() == null) return list;
    for (Map.Entry<String, Map<LocalDate, Map<String, String>>> pe : assign.getPersonDayClass().entrySet()) {
      String personKey = pe.getKey();
      String name = assign.getPersonKeyToName() != null
          ? assign.getPersonKeyToName().getOrDefault(personKey, personKey)
          : personKey;
      String empNo = assign.getPersonKeyToEmployeeNo() != null
          ? assign.getPersonKeyToEmployeeNo().get(personKey) : null;
      String dept = assign.getPersonKeyToDept() != null
          ? assign.getPersonKeyToDept().get(personKey) : null;
      String shiftClass = assign.getPersonKeyToShiftClass() != null
          ? assign.getPersonKeyToShiftClass().get(personKey) : null;
      Integer rowIndex = assign.getPersonKeyToRowIndex() != null
          ? assign.getPersonKeyToRowIndex().get(personKey) : null;
      String idCard = looksLikeIdCard(empNo) ? empNo : null;
      for (Map.Entry<LocalDate, Map<String, String>> de : pe.getValue().entrySet()) {
        for (Map.Entry<String, String> se : de.getValue().entrySet()) {
          TrainShiftAssignRecordEntity row = new TrainShiftAssignRecordEntity();
          row.setTenantId(tenantId);
          row.setTaskId(taskId);
          row.setPersonName(name);
          row.setIdCard(idCard);
          row.setEmployeeNo(empNo);
          row.setDept(dept);
          row.setShiftClass(shiftClass);
          row.setAssignDate(de.getKey());
          row.setSlot(se.getKey());
          row.setAlgoClassName(se.getValue());
          row.setRowIndex(rowIndex);
          row.setDeleted(0);
          list.add(row);
        }
      }
    }
    return list;
  }

  private Map<String, Object> taskResultMap(
      TrainShiftAssignTaskEntity task,
      List<TrainShiftAssignRecordEntity> records,
      ClassAssignResult assignOrNull) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("taskId", task.getId());
    map.put("planId", task.getPlanId());
    map.put("trainInfoId", task.getTrainInfoId());
    map.put("year", task.getYear());
    map.put("month", task.getMonth());
    map.put("adapterType", task.getAdapterType());
    map.put("status", task.getStatus());
    map.put("version", task.getVersion());
    map.put("sourceShiftFileId", task.getSourceShiftFileId());
    map.put("warnings", readJson(task.getWarningJson(), new TypeReference<List<Map<String, Object>>>() {}));
    map.put("config", readJson(task.getConfigJson(), new TypeReference<Map<String, Object>>() {}));
    map.put("unassignedPersons", readJson(task.getUnassignedJson(), new TypeReference<List<Map<String, Object>>>() {}));
    map.put("theoryDays", readJson(task.getTheoryDaysJson(), new TypeReference<List<String>>() {}));
    map.put("practiceDays", readJson(task.getPracticeDaysJson(), new TypeReference<List<String>>() {}));
    map.put("recordCount", records.size());
    map.put("algoClassNames", records.stream()
        .map(TrainShiftAssignRecordEntity::getAlgoClassName)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList()));
    map.put("bindings", buildBindings(records));
    map.put("records", records.stream().limit(500).map(this::toRecordView).collect(Collectors.toList()));
    if (assignOrNull != null) {
      map.put("dailyStats", toDailyStatsView(assignOrNull));
    } else {
      map.put("dailyStats", buildDailyStatsFromRecords(records));
    }
    return map;
  }

  private List<Map<String, Object>> buildBindings(List<TrainShiftAssignRecordEntity> records) {
    Map<String, Long> bind = new LinkedHashMap<>();
    Map<String, Long> counts = new HashMap<>();
    for (TrainShiftAssignRecordEntity r : records) {
      counts.merge(r.getAlgoClassName(), 1L, Long::sum);
      if (r.getTrainClassId() != null) {
        bind.putIfAbsent(r.getAlgoClassName(), r.getTrainClassId());
      }
    }
    List<Map<String, Object>> list = new ArrayList<>();
    for (String name : counts.keySet().stream().sorted().toList()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("algoClassName", name);
      row.put("recordCount", counts.get(name));
      row.put("trainClassId", bind.get(name));
      list.add(row);
    }
    return list;
  }

  private Map<String, Object> toRecordView(TrainShiftAssignRecordEntity r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", r.getId());
    m.put("personName", r.getPersonName());
    m.put("idCard", r.getIdCard());
    m.put("employeeNo", r.getEmployeeNo());
    m.put("dept", r.getDept());
    m.put("shiftClass", r.getShiftClass());
    m.put("assignDate", r.getAssignDate() == null ? null : r.getAssignDate().toString());
    m.put("slot", r.getSlot());
    m.put("algoClassName", r.getAlgoClassName());
    m.put("trainClassId", r.getTrainClassId());
    return m;
  }

  private List<Map<String, Object>> mergeWarnings(List<Map<String, Object>> base, ClassAssignResult assign) {
    List<Map<String, Object>> warnings = new ArrayList<>(base);
    if (assign.getUnassignedPersons() != null) {
      for (String key : assign.getUnassignedPersons()) {
        String reason = assign.getUnassignedReasons() != null
            ? assign.getUnassignedReasons().getOrDefault(key, "无可排课时段")
            : "无可排课时段";
        String name = assign.getPersonKeyToName() != null
            ? assign.getPersonKeyToName().getOrDefault(key, key) : key;
        warnings.add(Map.of(
            "level", "INFO",
            "code", "UNASSIGNED_PERSON",
            "message", name + "：" + reason
        ));
      }
    }
    return warnings;
  }

  private List<String> distinctAlgoClasses(ClassAssignResult assign) {
    Set<String> names = new LinkedHashSet<>();
    if (assign.getPersonDayClass() == null) return List.of();
    for (Map<LocalDate, Map<String, String>> byDate : assign.getPersonDayClass().values()) {
      for (Map<String, String> bySlot : byDate.values()) {
        names.addAll(bySlot.values());
      }
    }
    return names.stream().sorted().toList();
  }

  private List<Map<String, Object>> toDailyStatsView(ClassAssignResult assign) {
    List<Map<String, Object>> list = new ArrayList<>();
    if (assign.getDailyStats() == null) return list;
    assign.getDailyStats().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> {
          for (ClassAssignResult.ClassCount c : e.getValue()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey().toString());
            row.put("slot", c.getSlot());
            row.put("classIndex", c.getClassIndex());
            row.put("algoClassName", c.getSlot() + c.getClassIndex() + "班");
            row.put("count", c.getCount());
            list.add(row);
          }
        });
    return list;
  }

  private List<Map<String, Object>> buildDailyStatsFromRecords(List<TrainShiftAssignRecordEntity> records) {
    Map<String, Long> counter = new LinkedHashMap<>();
    for (TrainShiftAssignRecordEntity r : records) {
      String key = r.getAssignDate() + "|" + r.getAlgoClassName();
      counter.merge(key, 1L, Long::sum);
    }
    List<Map<String, Object>> list = new ArrayList<>();
    for (Map.Entry<String, Long> e : counter.entrySet()) {
      String[] parts = e.getKey().split("\\|", 2);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("date", parts[0]);
      row.put("algoClassName", parts[1]);
      row.put("count", e.getValue());
      list.add(row);
    }
    return list;
  }

  private List<Map<String, Object>> toUnassignedView(ClassAssignResult assign) {
    List<Map<String, Object>> list = new ArrayList<>();
    if (assign.getUnassignedPersons() == null) return list;
    for (String key : assign.getUnassignedPersons()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("personKey", key);
      row.put("personName", assign.getPersonKeyToName() != null
          ? assign.getPersonKeyToName().getOrDefault(key, key) : key);
      row.put("reason", assign.getUnassignedReasons() != null
          ? assign.getUnassignedReasons().getOrDefault(key, "") : "");
      list.add(row);
    }
    return list;
  }

  private int countPersons(ScheduleParseResult parse) {
    if (parse.getPersonDaySlots() == null) return 0;
    Set<String> keys = new HashSet<>();
    parse.getPersonDaySlots().forEach(s -> {
      String key = StringUtils.hasText(s.getEmployeeNo()) ? s.getEmployeeNo() : s.getName();
      keys.add(key);
    });
    return keys.size();
  }

  private int countRecords(ClassAssignResult assign) {
    int n = 0;
    if (assign.getPersonDayClass() == null) return 0;
    for (Map<LocalDate, Map<String, String>> byDate : assign.getPersonDayClass().values()) {
      for (Map<String, String> bySlot : byDate.values()) {
        n += bySlot.size();
      }
    }
    return n;
  }

  private String resolveSlot(String slot, String algoClassName) {
    if (StringUtils.hasText(slot)) return slot.trim();
    if (StringUtils.hasText(algoClassName) && algoClassName.contains("下午")) return "下午";
    return "上午";
  }

  private boolean looksLikeIdCard(String v) {
    if (!StringUtils.hasText(v)) return false;
    String t = v.trim();
    return t.length() == 15 || t.length() == 18;
  }

  private TrainThreeYearPlanEntity requirePlan(Long planId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainThreeYearPlanEntity plan = planMapper.selectById(planId);
    if (plan == null || !Objects.equals(plan.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "三年规划不存在");
    }
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    if (companyFilter != null && !Objects.equals(companyFilter, plan.getTargetOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该企业规划");
    }
    if (plan.getStatus() == null || plan.getStatus() != 4) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅「企业已确认」的规划可排课分班");
    }
    return plan;
  }

  private void assertReturnReady(TrainThreeYearPlanEntity plan) {
    if (plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未补交排班表，请先补交后再排课分班");
    }
    if (requireRoster && plan.getEnterpriseRosterFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未补交筛选名单，请先补交后再排课分班");
    }
  }

  private void requireTrainInfo(Long trainInfoId, Long planId) {
    requireTrainInfoEntity(trainInfoId, planId);
  }

  private TrainInfoListEntity requireTrainInfoEntity(Long trainInfoId, Long planId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity info = trainInfoListMapper.selectById(trainInfoId);
    if (info == null || !Objects.equals(info.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训信息不存在");
    }
    if (info.getThreeYearPlanId() != null && !Objects.equals(info.getThreeYearPlanId(), planId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "培训信息未关联当前三年规划");
    }
    if (info.getThreeYearPlanId() == null) {
      info.setThreeYearPlanId(planId);
      trainInfoListMapper.updateById(info);
    }
    return info;
  }

  private TrainInfoClassEntity createTrainClass(
      TrainInfoListEntity trainInfo, String title, boolean submitConfirm) {
    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setTrainInfoId(trainInfo.getId());
    clazz.setTitle(title);
    clazz.setCompanyId(trainInfo.getCompanyId());
    clazz.setWorkTypeId(trainInfo.getWorkTypeId());
    clazz.setWorkTypeLevel(trainInfo.getWorkTypeLevel());
    clazz.setNums(0);
    clazz.setAddtime((int) (System.currentTimeMillis() / 1000));
    clazz.setDeleted(0);
    clazz.setCompanyConfirmStatus(submitConfirm ? 0 : 0);
    trainInfoClassMapper.insert(clazz);
    return clazz;
  }

  private String buildMaterializePrefix(TrainInfoListEntity trainInfo, TrainShiftAssignTaskEntity task) {
    String base = StringUtils.hasText(trainInfo.getCompanyName())
        ? trainInfo.getCompanyName()
        : (StringUtils.hasText(trainInfo.getTitle()) ? trainInfo.getTitle() : "培训");
    return base + "-" + task.getYear() + String.format("%02d", task.getMonth()) + "-";
  }

  private String personUniqueKey(TrainShiftAssignRecordEntity r) {
    if (StringUtils.hasText(r.getIdCard())) return "ID:" + r.getIdCard().trim().toUpperCase();
    if (StringUtils.hasText(r.getEmployeeNo()) && looksLikeIdCard(r.getEmployeeNo())) {
      return "ID:" + r.getEmployeeNo().trim().toUpperCase();
    }
    if (StringUtils.hasText(r.getEmployeeNo())) return "NO:" + r.getEmployeeNo().trim();
    return "N:" + (r.getPersonName() == null ? "" : r.getPersonName().trim());
  }

  private String resolveIdCard(
      TrainShiftAssignRecordEntity r,
      Map<String, TrainPlanRosterEntity> byIdCard,
      Map<String, TrainPlanRosterEntity> byName) {
    if (StringUtils.hasText(r.getIdCard())) return r.getIdCard().trim();
    if (StringUtils.hasText(r.getEmployeeNo()) && looksLikeIdCard(r.getEmployeeNo())) {
      return r.getEmployeeNo().trim();
    }
    if (StringUtils.hasText(r.getPersonName())) {
      TrainPlanRosterEntity m = byName.get(r.getPersonName().trim());
      if (m != null && StringUtils.hasText(m.getIdCard())) return m.getIdCard().trim();
    }
    return null;
  }

  private TrainShiftAssignTaskEntity requireTask(Long taskId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainShiftAssignTaskEntity task = taskMapper.selectById(taskId);
    if (task == null || !Objects.equals(task.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排课分班任务不存在");
    }
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    if (companyFilter != null) {
      TrainThreeYearPlanEntity plan = planMapper.selectById(task.getPlanId());
      if (plan == null || !Objects.equals(plan.getTargetOrgId(), companyFilter)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该任务");
      }
    }
    return task;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化失败：" + ex.getMessage());
    }
  }

  private <T> T readJson(String json, TypeReference<T> type) {
    if (!StringUtils.hasText(json)) return null;
    try {
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      return null;
    }
  }

  private record RunBundle(
      TrainThreeYearPlanEntity plan,
      ScheduleParseResult parse,
      EffectiveSchedulingConfig config,
      List<Map<String, Object>> warnings,
      String adapterType
  ) {}
}
