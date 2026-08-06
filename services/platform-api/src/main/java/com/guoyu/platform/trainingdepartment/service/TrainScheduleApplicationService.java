package com.guoyu.platform.trainingdepartment.service;

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
import com.guoyu.platform.trainingdepartment.support.TrainKcbiaoScheduleParser;
import com.guoyu.platform.trainingdepartment.support.TrainingStoredFileResolver;
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
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainScheduleApplicationService {

  private final TrainScheduleMapper mapper;
  private final TrainInfoClassMapper classMapper;
  private final FileMapper fileMapper;
  private final FileStorageProperties fileStorageProperties;
  private final TrainingStoredFileResolver fileResolver;
  private final TrainKcbiaoScheduleParser kcbiaoParser;

  public TrainScheduleApplicationService(
      TrainScheduleMapper mapper,
      TrainInfoClassMapper classMapper,
      FileMapper fileMapper,
      FileStorageProperties fileStorageProperties,
      TrainingStoredFileResolver fileResolver,
      TrainKcbiaoScheduleParser kcbiaoParser) {
    this.mapper = mapper;
    this.classMapper = classMapper;
    this.fileMapper = fileMapper;
    this.fileStorageProperties = fileStorageProperties;
    this.fileResolver = fileResolver;
    this.kcbiaoParser = kcbiaoParser;
  }

  @Transactional(readOnly = true)
  public List<TrainScheduleConflictVO> checkConflicts(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainScheduleEntity> wrapper = new LambdaQueryWrapper<TrainScheduleEntity>()
        .eq(TrainScheduleEntity::getTenantId, tenantId)
        .eq(TrainScheduleEntity::getDeleted, 0);
    List<TrainScheduleEntity> allSchedules = mapper.selectList(wrapper);

    List<TrainScheduleConflictVO> conflicts = new ArrayList<>();

    for (int i = 0; i < allSchedules.size(); i++) {
      for (int j = i + 1; j < allSchedules.size(); j++) {
        TrainScheduleEntity s1 = allSchedules.get(i);
        TrainScheduleEntity s2 = allSchedules.get(j);

        if (!Objects.equals(s1.getClassId(), classId) && !Objects.equals(s2.getClassId(), classId)) {
          continue;
        }
        if (!isSameDay(s1, s2)) {
          continue;
        }

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
    return s1.getDayOfWeek() != null && Objects.equals(s1.getDayOfWeek(), s2.getDayOfWeek());
  }

  private boolean hasTimeOverlap(TrainScheduleEntity s1, TrainScheduleEntity s2) {
    if (s1.getStartTime() == null || s1.getEndTime() == null
        || s2.getStartTime() == null || s2.getEndTime() == null) {
      return false;
    }
    return s1.getStartTime().compareTo(s2.getEndTime()) < 0
        && s2.getStartTime().compareTo(s1.getEndTime()) < 0;
  }

  /**
   * 获取班级课表。若结构化课次为空且开班已上传 kcbiao_file，则自动解析导入一次。
   */
  @Transactional
  public List<Map<String, Object>> getByClassId(Long classId) {
    List<Map<String, Object>> list = listMaps(classId);
    if (!list.isEmpty()) {
      return list;
    }
    TrainInfoClassEntity clazz = requireClass(classId);
    if (!StringUtils.hasText(clazz.getKcbiaoFile())) {
      return list;
    }
    try {
      importFromKcbiao(classId, false, false);
      return listMaps(classId);
    } catch (BusinessException ex) {
      log.warn("自动导入开班课表失败 classId={} msg={}", classId, ex.getMessage());
      return list;
    }
  }

  /**
   * 从开班准备上传的课程表 Excel 导入 train_schedule。
   *
   * @param force true=覆盖已有结构化课表；false=已有课次则跳过
   * @param confirmOverwriteManual 课表已跟班调过课时，force 覆盖须再确认
   */
  @Transactional
  public Map<String, Object> importFromKcbiao(Long classId, boolean force, boolean confirmOverwriteManual) {
    TrainInfoClassEntity clazz = requireClass(classId);
    if (!StringUtils.hasText(clazz.getKcbiaoFile())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该班尚未在开班准备上传课程表文件");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    long existing = mapper.selectCount(new LambdaQueryWrapper<TrainScheduleEntity>()
        .eq(TrainScheduleEntity::getClassId, classId)
        .eq(TrainScheduleEntity::getTenantId, tenantId)
        .eq(TrainScheduleEntity::getDeleted, 0));
    if (existing > 0 && !force) {
      Map<String, Object> skipped = new LinkedHashMap<>();
      skipped.put("classId", classId);
      skipped.put("imported", 0);
      skipped.put("skipped", true);
      skipped.put("reason", "已有结构化课表，未覆盖；如需重导请传 force=true");
      skipped.put("manualEdited", Objects.equals(clazz.getScheduleManualEdited(), 1));
      return skipped;
    }
    if (existing > 0 && force
        && Objects.equals(clazz.getScheduleManualEdited(), 1)
        && !confirmOverwriteManual) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "本班课表已在跟班手工调整过，重导开班课表将覆盖调课结果。请确认后重试（confirmOverwriteManual=true）");
    }

    Path local;
    try {
      local = fileResolver.resolveLocalPath(clazz.getKcbiaoFile());
    } catch (BusinessException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取开班课程表文件：" + ex.getMessage());
    }

    List<Map<String, Object>> items;
    try {
      items = kcbiaoParser.parse(local);
    } catch (Exception ex) {
      log.error("解析开班课程表失败 classId={}", classId, ex);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析开班课程表失败：" + ex.getMessage());
    }
    if (items.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "开班课程表未解析到有效课次，请确认 Excel 含「日期」表头及日期/时间/课程列");
    }

    Map<String, Object> saved = replaceScheduleRows(classId, items, false);
    clazz.setScheduleImportedAt(LocalDateTime.now());
    clazz.setScheduleManualEdited(0);
    classMapper.updateById(clazz);

    Map<String, Object> result = new LinkedHashMap<>(saved);
    result.put("imported", saved.get("count"));
    result.put("skipped", false);
    result.put("source", "kcbiao_file");
    result.put("manualEdited", false);
    return result;
  }

  /** 课表状态：是否有开班文件、是否手工调过、课次数 */
  @Transactional(readOnly = true)
  public Map<String, Object> getScheduleStatus(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    long count = mapper.selectCount(new LambdaQueryWrapper<TrainScheduleEntity>()
        .eq(TrainScheduleEntity::getClassId, classId)
        .eq(TrainScheduleEntity::getTenantId, tenantId)
        .eq(TrainScheduleEntity::getDeleted, 0));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("hasKcbiaoFile", StringUtils.hasText(clazz.getKcbiaoFile()));
    out.put("kcbiaoFile", clazz.getKcbiaoFile());
    out.put("manualEdited", Objects.equals(clazz.getScheduleManualEdited(), 1));
    out.put("importedAt", clazz.getScheduleImportedAt());
    out.put("sessionCount", count);
    return out;
  }

  /** 批量保存课表（全量替换班级课表；不回写开班 kcbiao_file；标记手工调课） */
  @Transactional
  public Map<String, Object> saveSchedule(Long classId, List<Map<String, Object>> items) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Map<String, Object> result = replaceScheduleRows(classId, items, true);
    clazz.setScheduleManualEdited(1);
    classMapper.updateById(clazz);
    result.put("manualEdited", true);
    return result;
  }

  private Map<String, Object> replaceScheduleRows(
      Long classId, List<Map<String, Object>> items, boolean fromManualSave) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    if (items == null) {
      items = List.of();
    }

    mapper.physicalDeleteByClass(classId, tenantId);

    int count = 0;
    for (Map<String, Object> item : items) {
      TrainScheduleEntity entity = new TrainScheduleEntity();
      entity.setClassId(classId);
      entity.setCourseId(item.get("courseId") != null ? Long.valueOf(item.get("courseId").toString()) : null);
      entity.setCourseName(item.get("courseName") != null ? item.get("courseName").toString() : null);
      entity.setClassHours(item.get("classHours") != null ? item.get("classHours").toString() : null);
      entity.setTeachForm(item.get("teachForm") != null ? item.get("teachForm").toString() : null);
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
      entity.setDeleted(0);
      mapper.insert(entity);
      count++;
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("classId", classId);
    result.put("count", count);
    result.put("fromManualSave", fromManualSave);
    return result;
  }

  @Transactional
  public Map<String, Object> exportSchedule(Long classId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    TrainInfoClassEntity classEntity = requireClass(classId);
    List<Map<String, Object>> schedules = listMaps(classId);

    StringBuilder csv = new StringBuilder();
    csv.append("日期,星期,时间,培训内容,学时,授课形式,老师\n");
    String[] weekDays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    for (Map<String, Object> s : schedules) {
      csv.append(s.get("scheduleDate") != null ? s.get("scheduleDate") : "").append(",");
      Integer dow = s.get("dayOfWeek") != null ? (Integer) s.get("dayOfWeek") : null;
      csv.append(dow != null && dow > 0 && dow < 8 ? weekDays[dow] : "").append(",");
      String start = s.get("startTime") != null ? String.valueOf(s.get("startTime")) : "";
      String end = s.get("endTime") != null ? String.valueOf(s.get("endTime")) : "";
      String timeRange = (start.isEmpty() && end.isEmpty()) ? "" : (start + "-" + end);
      csv.append(escapeCsv(timeRange)).append(",");
      csv.append(escapeCsv(s.get("courseName"))).append(",");
      csv.append(escapeCsv(s.get("classHours"))).append(",");
      csv.append(escapeCsv(s.get("teachForm"))).append(",");
      csv.append(escapeCsv(s.get("teacherName"))).append("\n");
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

  private List<Map<String, Object>> listMaps(Long classId) {
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
      item.put("classHours", s.getClassHours());
      item.put("teachForm", s.getTeachForm());
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

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity classEntity = classMapper.selectById(classId);
    if (classEntity == null || Objects.equals(classEntity.getDeleted(), 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return classEntity;
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
