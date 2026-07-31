package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private static final DateTimeFormatter ADD_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

  public TrainInfoClassApplicationService(TrainInfoClassMapper mapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainFileTempMapper trainFileTempMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainingOperatorScopeService operatorScopeService,
      ClassCollectionApplicationService classCollectionApplicationService) {
    this.mapper = mapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.trainFileTempMapper = trainFileTempMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.operatorScopeService = operatorScopeService;
    this.classCollectionApplicationService = classCollectionApplicationService;
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

    List<Map<String, Object>> list = mpPage.getRecords().stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("title", e.getTitle());
      // 列表人数以花名册实时统计为准，避免开班准备导入后 nums 冗余字段滞后
      item.put("nums", studentNumsMap.getOrDefault(e.getId(), 0));
      item.put("addtime", e.getAddtime());
      item.put("addtime_cn", formatAddtime(e.getAddtime()));
      item.put("train_info_id", e.getTrainInfoId());
      item.put("company_id", e.getCompanyId());
      item.put("work_type_id", e.getWorkTypeId());
      item.put("company_name", companyNameMap.getOrDefault(e.getCompanyId(), null));
      item.put("work_type_title", workTypeNameMap.getOrDefault(e.getWorkTypeId(), null));
      item.put("date_start", e.getDateStart());
      item.put("date_end", e.getDateEnd());
      item.put("file_temps", e.getFileTemps());
      item.put("kcbiao_file", e.getKcbiaoFile());
      item.put("kcbiao_src", resolveKcbiaoSrc(e.getKcbiaoFile()));
      TrainInfoListEntity ti = trainInfoMap.get(e.getTrainInfoId());
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
      entity.setDateStart(req.getDateStart());
      entity.setDateEnd(req.getDateEnd());
      entity.setKcbiaoFile(req.getKcbiaoFile());
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
      entity.setDateStart(req.getDateStart());
      entity.setDateEnd(req.getDateEnd());
      entity.setKcbiaoFile(req.getKcbiaoFile());

      // Auto-populate from train_info_list
      if (req.getTrainInfoId() != null) {
        TrainInfoListEntity ti = trainInfoListMapper.selectById(req.getTrainInfoId());
        if (ti != null) {
          entity.setCompanyId(ti.getCompanyId());
          entity.setWorkTypeId(ti.getWorkTypeId());
          entity.setWorkTypeLevel(ti.getWorkTypeLevel());
          if (req.getNums() == null || req.getNums() == 0) {
            entity.setNums(ti.getNum());
          } else {
            entity.setNums(req.getNums());
          }
        } else {
          entity.setNums(req.getNums());
        }
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
    for (Long id : ids) {
      TrainInfoClassEntity entity = mapper.selectById(id);
      if (entity != null && entity.getDeleted() == 0) {
        operatorScopeService.assertClassAccessible(entity);
        entity.setDeleted(1);
        mapper.deleteById(entity.getId());
        count++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("count", count);
    return result;
  }

  @Transactional
  public TrainInfoClassVO create(TrainInfoClassCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoClassEntity entity = new TrainInfoClassEntity();
    entity.setTrainInfoId(req.getTrainInfoId());
    entity.setTitle(req.getTitle());
    entity.setNums(req.getNums());
    entity.setCompanyId(req.getCompanyId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setDateStart(req.getDateStart());
    entity.setDateEnd(req.getDateEnd());
    entity.setAddtime(req.getAddtime());
    entity.setFileTemps(req.getFileTemps());
    entity.setKcbiaoFile(req.getKcbiaoFile());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainInfoClassVO update(Long id, TrainInfoClassUpdateReq req) {
    TrainInfoClassEntity entity = getExisting(id);
    entity.setTitle(req.getTitle());
    entity.setNums(req.getNums());
    entity.setDateStart(req.getDateStart());
    entity.setDateEnd(req.getDateEnd());
    entity.setFileTemps(req.getFileTemps());
    entity.setKcbiaoFile(req.getKcbiaoFile());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainInfoClassEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
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

  private TrainInfoClassVO toVO(TrainInfoClassEntity entity) {
    return new TrainInfoClassVO(
        entity.getId(),
        entity.getTrainInfoId(),
        entity.getTitle(),
        entity.getNums(),
        entity.getCompanyId(),
        entity.getWorkTypeId(),
        entity.getDateStart(),
        entity.getDateEnd(),
        entity.getAddtime(),
        entity.getFileTemps(),
        entity.getKcbiaoFile()
    );
  }
}
