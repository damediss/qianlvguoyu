package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TrainScheduleApplicationService {

  private final TrainScheduleMapper mapper;
  private final TrainInfoClassMapper classMapper;
  private final FileMapper fileMapper;
  private final FileStorageProperties fileStorageProperties;

  public TrainScheduleApplicationService(TrainScheduleMapper mapper,
      TrainInfoClassMapper classMapper,
      FileMapper fileMapper,
      FileStorageProperties fileStorageProperties) {
    this.mapper = mapper;
    this.classMapper = classMapper;
    this.fileMapper = fileMapper;
    this.fileStorageProperties = fileStorageProperties;
  }

  @Transactional(readOnly = true)
  public List<TrainScheduleConflictVO> checkConflicts(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // Query all schedules for the same tenant (cross-class conflict detection)
    LambdaQueryWrapper<TrainScheduleEntity> wrapper = new LambdaQueryWrapper<TrainScheduleEntity>()
        .eq(TrainScheduleEntity::getTenantId, tenantId)
        .eq(TrainScheduleEntity::getDeleted, 0);
    List<TrainScheduleEntity> allSchedules = mapper.selectList(wrapper);

    List<TrainScheduleConflictVO> conflicts = new ArrayList<>();

    for (int i = 0; i < allSchedules.size(); i++) {
      for (int j = i + 1; j < allSchedules.size(); j++) {
        TrainScheduleEntity s1 = allSchedules.get(i);
        TrainScheduleEntity s2 = allSchedules.get(j);

        // At least one schedule must belong to the queried class
        if (!Objects.equals(s1.getClassId(), classId) && !Objects.equals(s2.getClassId(), classId)) {
          continue;
        }

        // Must be on the same day
        if (!isSameDay(s1, s2)) {
          continue;
        }

        // Check teacher time overlap
        if (s1.getTeacherId() != null && s2.getTeacherId() != null
            && Objects.equals(s1.getTeacherId(), s2.getTeacherId())
            && hasTimeOverlap(s1, s2)) {
          conflicts.add(new TrainScheduleConflictVO(
              s1.getId(), s2.getId(),
              "TEACHER_CONFLICT",
              "教师 " + s1.getTeacherName() + " 在 " + s1.getScheduleDate()
                  + " " + s1.getStartTime() + "-" + s1.getEndTime()
                  + " 与 " + s2.getStartTime() + "-" + s2.getEndTime() + " 存在时间冲突"
          ));
        }

        // Check room time overlap
        if (s1.getRoomName() != null && s2.getRoomName() != null
            && Objects.equals(s1.getRoomName(), s2.getRoomName())
            && hasTimeOverlap(s1, s2)) {
          conflicts.add(new TrainScheduleConflictVO(
              s1.getId(), s2.getId(),
              "ROOM_CONFLICT",
              "教室 " + s1.getRoomName() + " 在 " + s1.getScheduleDate()
                  + " " + s1.getStartTime() + "-" + s1.getEndTime()
                  + " 与 " + s2.getStartTime() + "-" + s2.getEndTime() + " 存在时间冲突"
          ));
        }
      }
    }
    return conflicts;
  }

  private boolean isSameDay(TrainScheduleEntity s1, TrainScheduleEntity s2) {
    if (s1.getScheduleDate() != null && s2.getScheduleDate() != null) {
      return Objects.equals(s1.getScheduleDate(), s2.getScheduleDate());
    }
    // Fall back to dayOfWeek comparison if scheduleDate is not set
    return s1.getDayOfWeek() != null && Objects.equals(s1.getDayOfWeek(), s2.getDayOfWeek());
  }

  private boolean hasTimeOverlap(TrainScheduleEntity s1, TrainScheduleEntity s2) {
    if (s1.getStartTime() == null || s1.getEndTime() == null
        || s2.getStartTime() == null || s2.getEndTime() == null) {
      return false;
    }
    // Overlap exists when one starts before the other ends and vice versa
    return s1.getStartTime().compareTo(s2.getEndTime()) < 0
        && s2.getStartTime().compareTo(s1.getEndTime()) < 0;
  }

  /** 获取班级课表 */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> getByClassId(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainScheduleEntity> schedules = mapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getTenantId, tenantId)
            .eq(TrainScheduleEntity::getDeleted, 0)
            .orderByAsc(TrainScheduleEntity::getScheduleDate)
            .orderByAsc(TrainScheduleEntity::getStartTime));

    List<Map<String, Object>> list = new ArrayList<>();
    for (TrainScheduleEntity s : schedules) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", s.getId());
      item.put("classId", s.getClassId());
      item.put("courseId", s.getCourseId());
      item.put("courseName", s.getCourseName());
      item.put("teacherId", s.getTeacherId());
      item.put("teacherName", s.getTeacherName());
      item.put("roomName", s.getRoomName());
      item.put("scheduleDate", s.getScheduleDate());
      item.put("dayOfWeek", s.getDayOfWeek());
      item.put("startTime", s.getStartTime());
      item.put("endTime", s.getEndTime());
      item.put("status", s.getStatus());
      list.add(item);
    }
    return list;
  }

  /** 批量保存课表（全量替换班级课表） */
  @Transactional
  public Map<String, Object> saveSchedule(Long classId, List<Map<String, Object>> items) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // Verify class exists
    TrainInfoClassEntity classEntity = classMapper.selectById(classId);
    if (classEntity == null || classEntity.getDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    // Delete existing schedules for this class
    List<TrainScheduleEntity> existing = mapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getDeleted, 0));
    for (TrainScheduleEntity e : existing) {
      e.setDeleted(1);
      mapper.updateById(e);
    }

    // Insert new schedules
    int count = 0;
    for (Map<String, Object> item : items) {
      TrainScheduleEntity entity = new TrainScheduleEntity();
      entity.setClassId(classId);
      entity.setCourseId(item.get("courseId") != null ? Long.valueOf(item.get("courseId").toString()) : null);
      entity.setCourseName(item.get("courseName") != null ? item.get("courseName").toString() : null);
      entity.setTeacherId(item.get("teacherId") != null ? Long.valueOf(item.get("teacherId").toString()) : null);
      entity.setTeacherName(item.get("teacherName") != null ? item.get("teacherName").toString() : null);
      entity.setRoomName(item.get("roomName") != null ? item.get("roomName").toString() : null);
      entity.setScheduleDate(item.get("scheduleDate") != null ? item.get("scheduleDate").toString() : null);
      entity.setDayOfWeek(item.get("dayOfWeek") != null ? Integer.valueOf(item.get("dayOfWeek").toString()) : null);
      entity.setStartTime(item.get("startTime") != null ? item.get("startTime").toString() : null);
      entity.setEndTime(item.get("endTime") != null ? item.get("endTime").toString() : null);
      entity.setStatus(item.get("status") != null ? Integer.valueOf(item.get("status").toString()) : 0);
      entity.setTenantId(tenantId);
      entity.setCreatedBy(userId);
      entity.setUpdatedBy(userId);
      mapper.insert(entity);
      count++;
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("count", count);
    return result;
  }

  /** 导出课表为CSV文件 */
  @Transactional
  public Map<String, Object> exportSchedule(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    TrainInfoClassEntity classEntity = classMapper.selectById(classId);
    if (classEntity == null || classEntity.getDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    List<Map<String, Object>> schedules = getByClassId(classId);

    StringBuilder csv = new StringBuilder();
    csv.append("日期,星期,课程名称,讲师,教室,开始时间,结束时间\n");
    String[] weekDays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    for (Map<String, Object> s : schedules) {
      csv.append(s.get("scheduleDate") != null ? s.get("scheduleDate") : "").append(",");
      Integer dow = s.get("dayOfWeek") != null ? (Integer) s.get("dayOfWeek") : null;
      csv.append(dow != null && dow > 0 && dow < 8 ? weekDays[dow] : "").append(",");
      csv.append(escapeCsv(s.get("courseName"))).append(",");
      csv.append(escapeCsv(s.get("teacherName"))).append(",");
      csv.append(escapeCsv(s.get("roomName"))).append(",");
      csv.append(s.get("startTime") != null ? s.get("startTime") : "").append(",");
      csv.append(s.get("endTime") != null ? s.get("endTime") : "").append("\n");
    }

    String safeName = classEntity.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_") + "_课表.csv";
    String objectKey = String.format("%d/%d/%02d/%s-%s",
        tenantId, LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue(),
        UUID.randomUUID().toString().substring(0, 8), safeName);

    try {
      Path storagePath = Paths.get(fileStorageProperties.localRoot(), objectKey);
      Files.createDirectories(storagePath.getParent());
      Files.write(storagePath, csv.toString().getBytes(StandardCharsets.UTF_8));

      FileEntity fileEntity = new FileEntity();
      fileEntity.setTenantId(tenantId);
      fileEntity.setStorageProvider(fileStorageProperties.storageProvider());
      fileEntity.setObjectKey(objectKey);
      fileEntity.setOriginalName(safeName);
      fileEntity.setContentType("text/csv; charset=UTF-8");
      fileEntity.setSizeBytes((long) csv.toString().getBytes(StandardCharsets.UTF_8).length);
      fileEntity.setAccessPolicy("private");
      fileEntity.setStatus("uploaded");
      fileEntity.setCreatedBy(userId);
      fileEntity.setUpdatedBy(userId);
      fileMapper.insert(fileEntity);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("fileId", fileEntity.getId());
      result.put("downUrl", "/api/files/" + fileEntity.getId() + "/download");
      result.put("fileName", safeName);
      return result;
    } catch (Exception e) {
      log.error("导出课表失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出课表失败: " + e.getMessage());
    }
  }

  private String escapeCsv(Object val) {
    if (val == null) return "";
    String s = val.toString();
    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }
}