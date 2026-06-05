package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.CourseKejianEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.CourseKejianMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskLogMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskLogVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TrainTaskLogApplicationService {

  private final TrainTaskLogMapper mapper;
  private final CourseKejianMapper courseKejianMapper;
  private final TrainInfoClassMapper trainInfoClassMapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public TrainTaskLogApplicationService(TrainTaskLogMapper mapper,
      CourseKejianMapper courseKejianMapper,
      TrainInfoClassMapper trainInfoClassMapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.courseKejianMapper = courseKejianMapper;
    this.trainInfoClassMapper = trainInfoClassMapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTaskLogVO> page(TrainTaskLogQueryReq req) {
    LambdaQueryWrapper<TrainTaskLogEntity> wrapper = new LambdaQueryWrapper<TrainTaskLogEntity>()
        .orderByDesc(TrainTaskLogEntity::getId);
    if (req.getTrainClassId() != null) {
      wrapper.eq(TrainTaskLogEntity::getTrainClassId, req.getTrainClassId());
    }
    Page<TrainTaskLogEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTaskLogVO getById(Long id) {
    TrainTaskLogEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return toVO(entity);
  }

  @Transactional
  public TrainTaskLogVO create(TrainTaskLogCreateReq req) {
    TrainTaskLogEntity entity = new TrainTaskLogEntity();
    entity.setTrainClassId(req.getTrainClassId());
    entity.setCkId(req.getCkId());
    entity.setDate(req.getDate());
    entity.setAddtime(req.getAddtime());
    entity.setStatus(req.getStatus());
    entity.setTimes(req.getTimes());
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTaskLogVO update(Long id, TrainTaskLogUpdateReq req) {
    TrainTaskLogEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getTimes() != null) {
      entity.setTimes(req.getTimes());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    mapper.deleteById(id);
  }

  // ==================== 班级培训任务 ====================

  /** 培训任务列表（含课件名称映射） */
  @Transactional(readOnly = true)
  public Map<String, Object> classTaskList(Long classId) {
    assertClassViewable(classId);
    List<TrainTaskLogEntity> records = mapper.selectList(
        new LambdaQueryWrapper<TrainTaskLogEntity>()
            .eq(TrainTaskLogEntity::getTrainClassId, classId)
            .orderByDesc(TrainTaskLogEntity::getId));

    // 课件名称映射
    Map<Long, String> kejianNameMap = Map.of();
    if (!records.isEmpty()) {
      List<Long> ckIds = records.stream().map(TrainTaskLogEntity::getCkId).filter(c -> c != null).distinct().toList();
      if (!ckIds.isEmpty()) {
        kejianNameMap = courseKejianMapper.selectBatchIds(ckIds).stream()
            .collect(Collectors.toMap(CourseKejianEntity::getId, CourseKejianEntity::getTitle, (a, b) -> a));
      }
    }

    Map<Long, String> finalMap = kejianNameMap;
    List<Map<String, Object>> list = records.stream().map(e -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", e.getId());
      item.put("ck_id", e.getCkId());
      item.put("title", finalMap.getOrDefault(e.getCkId(), null));
      item.put("date", e.getDate());
      item.put("status", e.getStatus());
      item.put("status_cn", e.getStatus() != null && e.getStatus() == 1 ? "已完成" : "未开始");
      item.put("times", e.getTimes());
      String timesStr = e.getTimes();
      int timesCnt = 0;
      if (timesStr != null && !timesStr.isEmpty()) {
        try {
          timesCnt = timesStr.split(",").length;
        } catch (Exception ignored) {}
      }
      item.put("times_cn", timesCnt);
      item.put("addtime", e.getAddtime());
      item.put("train_class_id", e.getTrainClassId());
      return item;
    }).toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    return result;
  }

  /** 新增/编辑培训任务 */
  @Transactional
  public Map<String, Object> saveTask(Map<String, Object> params) {
    Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : 0L;
    if (id > 0) {
      TrainTaskLogEntity entity = mapper.selectById(id);
      if (entity == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
      }
      if (params.get("date") != null) entity.setDate(params.get("date").toString());
      if (params.get("ck_id") != null) entity.setCkId(Long.valueOf(params.get("ck_id").toString()));
      mapper.updateById(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      result.put("ck_id", entity.getCkId());
      result.put("date", entity.getDate());
      result.put("train_class_id", entity.getTrainClassId());
      return result;
    } else {
      TrainTaskLogEntity entity = new TrainTaskLogEntity();
      entity.setTrainClassId(params.get("classId") != null ? Long.valueOf(params.get("classId").toString()) : null);
      entity.setCkId(params.get("ck_id") != null ? Long.valueOf(params.get("ck_id").toString()) : null);
      entity.setDate(params.get("date") != null ? params.get("date").toString() : null);
      entity.setAddtime((int) (System.currentTimeMillis() / 1000));
      mapper.insert(entity);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", entity.getId());
      result.put("ck_id", entity.getCkId());
      result.put("date", entity.getDate());
      result.put("train_class_id", entity.getTrainClassId());
      return result;
    }
  }

  private void assertClassViewable(Long classId) {
    if (classId == null) {
      return;
    }
    TrainInfoClassEntity clazz = trainInfoClassMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    operatorScopeService.assertClassAccessible(clazz);
  }

  private TrainTaskLogVO toVO(TrainTaskLogEntity entity) {
    return new TrainTaskLogVO(
        entity.getId(),
        entity.getTrainClassId(),
        entity.getCkId(),
        entity.getDate(),
        entity.getAddtime(),
        entity.getStatus(),
        entity.getTimes(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}
