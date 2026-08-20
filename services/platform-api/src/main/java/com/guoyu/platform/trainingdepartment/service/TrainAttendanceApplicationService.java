package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.follow.FollowOpEventCode;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogCommand;
import com.guoyu.platform.trainingdepartment.follow.FollowOpLogWriter;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceImportSupport.MergedDay;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceImportSupport.ParsedRow;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceImportResultVO;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainAttendanceApplicationService {

  private static final int MAX_FAILURES = 100;

  private final TrainAttendanceMapper mapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainInfoClassMapper infoClassMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainAttendanceImportSupport importSupport;
  private final FollowOpLogWriter opLogWriter;

  public TrainAttendanceApplicationService(
      TrainAttendanceMapper mapper,
      TrainPeopleListMapper peopleListMapper,
      TrainInfoClassMapper infoClassMapper,
      TrainScheduleMapper scheduleMapper,
      TrainAttendanceImportSupport importSupport,
      FollowOpLogWriter opLogWriter) {
    this.mapper = mapper;
    this.peopleListMapper = peopleListMapper;
    this.infoClassMapper = infoClassMapper;
    this.scheduleMapper = scheduleMapper;
    this.importSupport = importSupport;
    this.opLogWriter = opLogWriter;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainAttendanceVO> page(TrainAttendanceQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainAttendanceEntity> wrapper = new LambdaQueryWrapper<TrainAttendanceEntity>()
        .eq(TrainAttendanceEntity::getTenantId, tenantId)
        .eq(TrainAttendanceEntity::getDeleted, 0)
        .orderByDesc(TrainAttendanceEntity::getCreateTime);
    if (req.getProjectId() != null) wrapper.eq(TrainAttendanceEntity::getProjectId, req.getProjectId());
    if (req.getClassId() != null) wrapper.eq(TrainAttendanceEntity::getClassId, req.getClassId());
    if (req.getUserId() != null) wrapper.eq(TrainAttendanceEntity::getUserId, req.getUserId());
    if (req.getStatus() != null) wrapper.eq(TrainAttendanceEntity::getStatus, req.getStatus());
    if (StringUtils.hasText(req.getAttendanceDate())) {
      wrapper.eq(TrainAttendanceEntity::getAttendanceDate, req.getAttendanceDate());
    }
    if (req.getScheduleId() != null) {
      wrapper.eq(TrainAttendanceEntity::getScheduleId, req.getScheduleId());
    } else if (Boolean.TRUE.equals(req.getUnscheduledOnly())) {
      wrapper.isNull(TrainAttendanceEntity::getScheduleId);
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainAttendanceEntity::getUserName, req.getKeyword());
    }
    Page<TrainAttendanceEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainAttendanceVO getById(Long id) { return toVO(getExisting(id)); }

  @Transactional
  public TrainAttendanceVO create(TrainAttendanceCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    Long scheduleId = normalizeScheduleId(req.getClassId(), req.getAttendanceDate(), req.getScheduleId());

    TrainAttendanceEntity existing = findByScope(
        tenantId, req.getClassId(), req.getUserId(), req.getAttendanceDate(), scheduleId);
    if (existing != null) {
      applyMutableFields(existing, req, scheduleId);
      mapper.updateById(existing);
      writeSignLog(existing, "更新考勤");
      return toVO(existing);
    }

    TrainAttendanceEntity entity = new TrainAttendanceEntity();
    entity.setTenantId(tenantId);
    entity.setCreatedBy(userId);
    entity.setProjectId(req.getProjectId());
    entity.setClassId(req.getClassId());
    entity.setUserId(req.getUserId());
    entity.setUserName(req.getUserName());
    entity.setAttendanceDate(req.getAttendanceDate());
    entity.setScheduleId(scheduleId);
    entity.setStatus(req.getStatus());
    entity.setCheckInTime(req.getCheckInTime());
    entity.setCheckOutTime(req.getCheckOutTime());
    entity.setPhotoPath(req.getPhotoPath());
    entity.setVideoPath(req.getVideoPath());
    entity.setRemark(req.getRemark());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    writeSignLog(entity, null);
    return toVO(entity);
  }

  @Transactional
  public TrainAttendanceVO update(Long id, TrainAttendanceUpdateReq req) {
    TrainAttendanceEntity entity = getExisting(id);
    if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
    if (req.getClassId() != null) entity.setClassId(req.getClassId());
    if (req.getUserId() != null) entity.setUserId(req.getUserId());
    if (req.getUserName() != null) entity.setUserName(req.getUserName());
    if (req.getAttendanceDate() != null) entity.setAttendanceDate(req.getAttendanceDate());
    if (req.getScheduleId() != null || (req.getAttendanceDate() != null && entity.getScheduleId() != null)) {
      Long classId = req.getClassId() != null ? req.getClassId() : entity.getClassId();
      String date = req.getAttendanceDate() != null ? req.getAttendanceDate() : entity.getAttendanceDate();
      Long scheduleId = req.getScheduleId() != null
          ? normalizeScheduleId(classId, date, req.getScheduleId())
          : entity.getScheduleId();
      entity.setScheduleId(scheduleId);
    }
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getCheckInTime() != null) entity.setCheckInTime(req.getCheckInTime());
    if (req.getCheckOutTime() != null) entity.setCheckOutTime(req.getCheckOutTime());
    if (req.getPhotoPath() != null) entity.setPhotoPath(req.getPhotoPath());
    if (req.getVideoPath() != null) entity.setVideoPath(req.getVideoPath());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);

    if (entity.getClassId() != null) {
      opLogWriter.write(FollowOpLogCommand.of(entity.getClassId(), FollowOpEventCode.ATTENDANCE_BATCH_UPDATE)
          .bizDate(entity.getAttendanceDate())
          .scheduleId(entity.getScheduleId())
          .title("更新考勤")
          .summary((entity.getUserName() != null ? entity.getUserName() : "学员")
              + " · " + (entity.getStatus() != null ? entity.getStatus() : ""))
          .target("attendance", entity.getId()));
    }

    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainAttendanceEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  @Transactional
  public TrainAttendanceVO signIn(TrainAttendanceCreateReq req) {
    return create(req);
  }

  /**
   * 导入人社/培训平台「考勤信息列表」Excel，按身份证匹配本班学员。
   * 有上课/下课时间时按「课次行」落库（匹配或补建课表 schedule_id）；
   * 无时段时仍合并为按日记录。
   */
  @Transactional
  public TrainAttendanceImportResultVO importFromExcel(Long classId, MultipartFile file) {
    if (classId == null || classId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级ID无效");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传考勤 Excel 文件（.xls/.xlsx）");
    }
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 .xls / .xlsx 考勤文件");
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long operatorId = SecurityContext.currentUser().getUserId();
    TrainInfoClassEntity clazz = infoClassMapper.selectById(classId);
    if (clazz == null || (clazz.getDeleted() != null && clazz.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
    }

    List<ParsedRow> parsed = importSupport.parse(bytes);
    Map<String, TrainPeopleListEntity> studentByIdCard = loadStudentIdCardMap(classId);
    Map<String, TrainScheduleEntity> scheduleByKey = loadScheduleKeyMap(classId, tenantId);

    List<TrainAttendanceImportResultVO.FailureItem> failures = new ArrayList<>();
    List<ParsedRow> dayFallbackRows = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int skipped = 0;
    int sessionRows = 0;
    Set<String> clearedDayLevel = new HashSet<>();

    for (ParsedRow row : parsed) {
      if (!StringUtils.hasText(row.idCard)) {
        addFailure(failures, row.excelRowNum, row.name, row.idCard, row.attendanceDate, "缺少身份证号码");
        skipped++;
        continue;
      }
      if (!StringUtils.hasText(row.attendanceDate) || !row.attendanceDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
        addFailure(failures, row.excelRowNum, row.name, row.idCard, row.attendanceDate, "上课日期无效");
        skipped++;
        continue;
      }
      TrainPeopleListEntity student = studentByIdCard.get(row.idCard);
      if (student == null) {
        addFailure(failures, row.excelRowNum, row.name, row.idCard, row.attendanceDate, "本班花名册未匹配到该身份证");
        skipped++;
        continue;
      }

      boolean hasSessionTime = StringUtils.hasText(row.startTime) && StringUtils.hasText(row.endTime);
      if (!hasSessionTime) {
        dayFallbackRows.add(row);
        continue;
      }

      TrainScheduleEntity schedule = resolveOrCreateSchedule(
          scheduleByKey, classId, tenantId, operatorId, row);
      if (schedule == null || schedule.getId() == null) {
        addFailure(failures, row.excelRowNum, row.name, row.idCard, row.attendanceDate,
            "无法匹配或创建课次（上课/下课时间）");
        skipped++;
        continue;
      }

      String dayKey = student.getId() + "|" + row.attendanceDate;
      if (clearedDayLevel.add(dayKey)) {
        softDeleteDayLevel(tenantId, classId, student.getId(), row.attendanceDate);
      }

      String remark = "导入课次：" + row.startTime + "-" + row.endTime;
      boolean isNew = upsertAttendance(
          tenantId, operatorId, clazz, classId, student, row.attendanceDate, schedule.getId(),
          row.status, row.checkInTime, row.checkOutTime,
          StringUtils.hasText(row.name) ? row.name : student.getRealname(),
          remark);
      if (isNew) {
        created++;
      } else {
        updated++;
      }
      sessionRows++;
    }

    List<MergedDay> mergedDays = importSupport.mergeByPersonDay(dayFallbackRows);
    for (MergedDay day : mergedDays) {
      TrainPeopleListEntity student = studentByIdCard.get(day.idCard);
      if (student == null) {
        continue;
      }
      boolean isNew = upsertAttendance(
          tenantId, operatorId, clazz, classId, student, day.attendanceDate, null,
          day.status, day.checkInTime, day.checkOutTime,
          StringUtils.hasText(day.name) ? day.name : student.getRealname(),
          day.remark);
      if (isNew) {
        created++;
      } else {
        updated++;
      }
    }

    int applied = sessionRows + mergedDays.size();
    log.info("attendance import classId={} totalRows={} sessionRows={} dayRows={} created={} updated={} skipped={}",
        classId, parsed.size(), sessionRows, mergedDays.size(), created, updated, skipped);

    opLogWriter.write(FollowOpLogCommand.of(classId, FollowOpEventCode.ATTENDANCE_IMPORT)
        .title("导入考勤")
        .summary("课次 " + sessionRows + "，按日 " + mergedDays.size()
            + "，新建 " + created + "，更新 " + updated + "，跳过 " + skipped)
        .detail(Map.of(
            "totalRows", parsed.size(),
            "sessionRows", sessionRows,
            "dayRows", mergedDays.size(),
            "created", created,
            "updated", updated,
            "skipped", skipped)));

    return new TrainAttendanceImportResultVO(
        parsed.size(),
        applied,
        created,
        updated,
        skipped,
        failures);
  }

  private Map<String, TrainScheduleEntity> loadScheduleKeyMap(Long classId, Long tenantId) {
    List<TrainScheduleEntity> schedules = scheduleMapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getDeleted, 0));
    Map<String, TrainScheduleEntity> map = new HashMap<>();
    for (TrainScheduleEntity s : schedules) {
      if (tenantId != null && s.getTenantId() != null && !tenantId.equals(s.getTenantId())) {
        continue;
      }
      String key = TrainAttendanceImportSupport.sessionMatchKey(
          s.getScheduleDate(), s.getStartTime(), s.getEndTime());
      map.putIfAbsent(key, s);
      // 仅日期+开始时间也可命中（下课时间偶发不一致）
      String startKey = TrainAttendanceImportSupport.sessionMatchKey(
          s.getScheduleDate(), s.getStartTime(), "");
      map.putIfAbsent(startKey, s);
    }
    return map;
  }

  private TrainScheduleEntity resolveOrCreateSchedule(
      Map<String, TrainScheduleEntity> scheduleByKey,
      Long classId,
      Long tenantId,
      Long operatorId,
      ParsedRow row) {
    String fullKey = TrainAttendanceImportSupport.sessionMatchKey(
        row.attendanceDate, row.startTime, row.endTime);
    TrainScheduleEntity hit = scheduleByKey.get(fullKey);
    if (hit != null) {
      return hit;
    }
    String startKey = TrainAttendanceImportSupport.sessionMatchKey(
        row.attendanceDate, row.startTime, "");
    hit = scheduleByKey.get(startKey);
    if (hit != null) {
      return hit;
    }

    TrainScheduleEntity created = new TrainScheduleEntity();
    created.setTenantId(tenantId);
    created.setCreatedBy(operatorId);
    created.setClassId(classId);
    created.setScheduleDate(row.attendanceDate);
    created.setStartTime(TrainAttendanceImportSupport.normalizeTimeHm(row.startTime));
    created.setEndTime(TrainAttendanceImportSupport.normalizeTimeHm(row.endTime));
    created.setCourseName("考勤导入课次");
    created.setTeacherName(null);
    created.setDeleted(0);
    created.setStatus(1);
    scheduleMapper.insert(created);
    scheduleByKey.put(fullKey, created);
    scheduleByKey.putIfAbsent(startKey, created);
    return created;
  }

  private void softDeleteDayLevel(Long tenantId, Long classId, Long userId, String attendanceDate) {
    List<TrainAttendanceEntity> days = mapper.selectList(
        new LambdaQueryWrapper<TrainAttendanceEntity>()
            .eq(TrainAttendanceEntity::getTenantId, tenantId)
            .eq(TrainAttendanceEntity::getClassId, classId)
            .eq(TrainAttendanceEntity::getUserId, userId)
            .eq(TrainAttendanceEntity::getAttendanceDate, attendanceDate)
            .isNull(TrainAttendanceEntity::getScheduleId)
            .eq(TrainAttendanceEntity::getDeleted, 0));
    for (TrainAttendanceEntity day : days) {
      mapper.deleteById(day.getId());
    }
  }

  /** @return true=新建，false=更新 */
  private boolean upsertAttendance(
      Long tenantId,
      Long operatorId,
      TrainInfoClassEntity clazz,
      Long classId,
      TrainPeopleListEntity student,
      String attendanceDate,
      Long scheduleId,
      Integer status,
      String checkInTime,
      String checkOutTime,
      String userName,
      String remark) {
    TrainAttendanceEntity existing = findByScope(
        tenantId, classId, student.getId(), attendanceDate, scheduleId);
    if (existing == null) {
      TrainAttendanceEntity entity = new TrainAttendanceEntity();
      entity.setTenantId(tenantId);
      entity.setCreatedBy(operatorId);
      entity.setProjectId(clazz.getTrainInfoId());
      entity.setClassId(classId);
      entity.setUserId(student.getId());
      entity.setUserName(userName);
      entity.setAttendanceDate(attendanceDate);
      entity.setScheduleId(scheduleId);
      entity.setStatus(status);
      entity.setCheckInTime(blankToNull(checkInTime));
      entity.setCheckOutTime(blankToNull(checkOutTime));
      entity.setRemark(remark);
      entity.setDeleted(0);
      mapper.insert(entity);
      return true;
    }
    existing.setStatus(status);
    if (StringUtils.hasText(checkInTime)) {
      existing.setCheckInTime(checkInTime);
    }
    if (StringUtils.hasText(checkOutTime)) {
      existing.setCheckOutTime(checkOutTime);
    }
    if (StringUtils.hasText(userName)) {
      existing.setUserName(userName);
    }
    existing.setRemark(remark);
    mapper.updateById(existing);
    return false;
  }

  private void applyMutableFields(TrainAttendanceEntity entity, TrainAttendanceCreateReq req, Long scheduleId) {
    if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
    if (req.getClassId() != null) entity.setClassId(req.getClassId());
    if (req.getUserName() != null) entity.setUserName(req.getUserName());
    entity.setScheduleId(scheduleId);
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getCheckInTime() != null) entity.setCheckInTime(req.getCheckInTime());
    if (req.getCheckOutTime() != null) entity.setCheckOutTime(req.getCheckOutTime());
    if (req.getPhotoPath() != null) entity.setPhotoPath(req.getPhotoPath());
    if (req.getVideoPath() != null) entity.setVideoPath(req.getVideoPath());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
  }

  private void writeSignLog(TrainAttendanceEntity entity, String title) {
    if (entity.getClassId() == null) {
      return;
    }
    FollowOpLogCommand cmd = FollowOpLogCommand.of(entity.getClassId(), FollowOpEventCode.ATTENDANCE_SIGN_IN)
        .bizDate(entity.getAttendanceDate())
        .scheduleId(entity.getScheduleId())
        .summary((entity.getUserName() != null ? entity.getUserName() : "学员")
            + " · " + (entity.getStatus() != null ? entity.getStatus() : ""))
        .target("attendance", entity.getId());
    if (title != null) {
      cmd.title(title);
    }
    opLogWriter.write(cmd);
  }

  /**
   * 校验课次归属班级与业务日；返回规范化 scheduleId（非法则抛错）。
   */
  private Long normalizeScheduleId(Long classId, String attendanceDate, Long scheduleId) {
    if (scheduleId == null) {
      return null;
    }
    TrainScheduleEntity schedule = scheduleMapper.selectById(scheduleId);
    if (schedule == null || (schedule.getDeleted() != null && schedule.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课次不存在");
    }
    if (classId != null && !Objects.equals(schedule.getClassId(), classId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课次不属于当前班级");
    }
    if (StringUtils.hasText(attendanceDate)
        && StringUtils.hasText(schedule.getScheduleDate())
        && !attendanceDate.equals(schedule.getScheduleDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课次日期与考勤业务日不一致");
    }
    return scheduleId;
  }

  private TrainAttendanceEntity findByScope(
      Long tenantId, Long classId, Long userId, String attendanceDate, Long scheduleId) {
    if (classId == null || userId == null || !StringUtils.hasText(attendanceDate)) {
      return null;
    }
    LambdaQueryWrapper<TrainAttendanceEntity> wrapper = new LambdaQueryWrapper<TrainAttendanceEntity>()
        .eq(TrainAttendanceEntity::getTenantId, tenantId)
        .eq(TrainAttendanceEntity::getClassId, classId)
        .eq(TrainAttendanceEntity::getUserId, userId)
        .eq(TrainAttendanceEntity::getAttendanceDate, attendanceDate)
        .eq(TrainAttendanceEntity::getDeleted, 0)
        .last("LIMIT 1");
    if (scheduleId == null) {
      wrapper.isNull(TrainAttendanceEntity::getScheduleId);
    } else {
      wrapper.eq(TrainAttendanceEntity::getScheduleId, scheduleId);
    }
    return mapper.selectOne(wrapper);
  }

  private Map<String, TrainPeopleListEntity> loadStudentIdCardMap(Long classId) {
    List<TrainPeopleListEntity> students = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    Map<String, TrainPeopleListEntity> map = new HashMap<>();
    for (TrainPeopleListEntity s : students) {
      String idCard = TrainAttendanceImportSupport.normalizeIdCard(s.getIdCard());
      if (StringUtils.hasText(idCard)) {
        map.putIfAbsent(idCard, s);
      }
    }
    return map;
  }

  private void addFailure(
      List<TrainAttendanceImportResultVO.FailureItem> failures,
      int rowNum,
      String name,
      String idCard,
      String date,
      String reason) {
    if (failures.size() >= MAX_FAILURES) return;
    failures.add(new TrainAttendanceImportResultVO.FailureItem(
        rowNum,
        name == null ? "" : name,
        idCard == null ? "" : idCard,
        date == null ? "" : date,
        reason));
  }

  private static String blankToNull(String s) {
    return StringUtils.hasText(s) ? s : null;
  }

  private TrainAttendanceEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainAttendanceEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainAttendanceEntity>()
        .eq(TrainAttendanceEntity::getTenantId, tenantId)
        .eq(TrainAttendanceEntity::getId, id)
        .eq(TrainAttendanceEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    return entity;
  }

  private TrainAttendanceVO toVO(TrainAttendanceEntity entity) {
    return new TrainAttendanceVO(entity.getId(), entity.getProjectId(), entity.getClassId(),
        entity.getUserId(), entity.getUserName(), entity.getAttendanceDate(), entity.getScheduleId(),
        entity.getStatus(), entity.getCheckInTime(), entity.getCheckOutTime(), entity.getPhotoPath(),
        entity.getVideoPath(), entity.getRemark(), entity.getSortNumber(), entity.getTenantId(),
        entity.getCreatedBy(), entity.getCreateTime(), entity.getUpdateTime());
  }
}
