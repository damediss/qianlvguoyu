package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final DateTimeFormatter ADD_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

  public TrainInfoClassApplicationService(TrainInfoClassMapper mapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainFileTempMapper trainFileTempMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.trainFileTempMapper = trainFileTempMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.operatorScopeService = operatorScopeService;
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

    List<Map<String, Object>> list = mpPage.getRecords().stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("title", e.getTitle());
      item.put("nums", e.getNums());
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
        mapper.updateById(entity);
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
    mapper.updateById(entity);
  }

  /**
   * 挂载文件模板到班级
   */
  @Transactional
  public Map<String, Object> attachFileTemps(Long classId, List<Long> templateIds) {
    TrainInfoClassEntity entity = getExisting(classId);
    List<Long> currentIds = parseFileTemps(entity.getFileTemps());
    for (Long tid : templateIds) {
      if (tid != null && !currentIds.contains(tid)) {
        currentIds.add(tid);
      }
    }
    try {
      entity.setFileTemps(objectMapper.writeValueAsString(currentIds));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化模板ID列表失败");
    }
    mapper.updateById(entity);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("count", templateIds != null ? templateIds.size() : 0);
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
