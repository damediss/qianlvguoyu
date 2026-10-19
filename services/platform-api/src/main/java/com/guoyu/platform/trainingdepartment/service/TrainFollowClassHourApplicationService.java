package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.AttendanceInput;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.ClassHours;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.SessionInput;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentHours;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentInput;
import com.guoyu.platform.trainingdepartment.vo.FollowClassHoursVO;
import com.guoyu.platform.trainingdepartment.vo.FollowStudentClassHoursVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainFollowClassHourApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainAttendanceMapper attendanceMapper;
  private final TrainPeopleListMapper peopleListMapper;

  public TrainFollowClassHourApplicationService(
      TrainInfoClassMapper classMapper,
      TrainScheduleMapper scheduleMapper,
      TrainAttendanceMapper attendanceMapper,
      TrainPeopleListMapper peopleListMapper) {
    this.classMapper = classMapper;
    this.scheduleMapper = scheduleMapper;
    this.attendanceMapper = attendanceMapper;
    this.peopleListMapper = peopleListMapper;
  }

  @Transactional(readOnly = true)
  public FollowClassHoursVO classHours(Long classId) {
    requireClass(classId);
    Long tenantId = tenantId();
    ClassHours computed = compute(classId, tenantId);
    return toVo(computed);
  }

  ClassHours compute(Long classId, Long tenantId) {
    List<TrainScheduleEntity> schedules = scheduleMapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getDeleted, 0)
            .orderByAsc(TrainScheduleEntity::getScheduleDate)
            .orderByAsc(TrainScheduleEntity::getStartTime)
            .orderByAsc(TrainScheduleEntity::getId));
    if (tenantId != null) {
      schedules = schedules.stream()
          .filter(s -> s.getTenantId() == null || tenantId.equals(s.getTenantId()))
          .toList();
    }

    List<TrainAttendanceEntity> attendances = attendanceMapper.selectList(
        new LambdaQueryWrapper<TrainAttendanceEntity>()
            .eq(TrainAttendanceEntity::getClassId, classId)
            .eq(TrainAttendanceEntity::getDeleted, 0));
    if (tenantId != null) {
      attendances = attendances.stream()
          .filter(a -> a.getTenantId() == null || tenantId.equals(a.getTenantId()))
          .toList();
    }

    List<TrainPeopleListEntity> people = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0)
            .orderByAsc(TrainPeopleListEntity::getId));
    if (tenantId != null) {
      people = people.stream()
          .filter(s -> s.getTenantId() == null || tenantId.equals(s.getTenantId()))
          .toList();
    }

    List<SessionInput> sessions = new ArrayList<>();
    for (TrainScheduleEntity s : schedules) {
      sessions.add(new SessionInput(
          s.getId(), s.getScheduleDate(), s.getStartTime(), s.getEndTime(), s.getClassHours()));
    }
    List<AttendanceInput> att = new ArrayList<>();
    for (TrainAttendanceEntity a : attendances) {
      att.add(new AttendanceInput(
          a.getUserId(), a.getAttendanceDate(), a.getScheduleId(), a.getStatus(), a.getRemark()));
    }
    List<StudentInput> students = new ArrayList<>();
    for (TrainPeopleListEntity p : people) {
      students.add(new StudentInput(p.getId(), p.getRealname()));
    }
    return FollowClassHourCalculator.compute(sessions, att, students);
  }

  public void appendMetrics(Map<String, Object> result, Long classId, Long tenantId) {
    ClassHours hours = compute(classId, tenantId);
    result.put("plannedHours", hours.plannedHours());
    result.put("plannedHoursFromExcel", hours.plannedHoursFromExcel());
    result.put("attendedHoursSum", hours.attendedHoursSum());
    result.put("attendedHoursAvg", hours.attendedHoursAvg());
    result.put("hoursCompletionAvg", hours.hoursCompletionAvg());
    result.put("unconvertibleSessions", hours.unconvertibleSessions());
    result.put("hoursMinuteUnit", FollowClassHourCalculator.MINUTE_UNIT);
    result.put("recordedStudentCount", hours.recordedStudentCount());
    result.put("unrecordedStudentCount", hours.unrecordedStudentCount());
  }

  private FollowClassHoursVO toVo(ClassHours hours) {
    List<FollowStudentClassHoursVO> students = new ArrayList<>();
    for (StudentHours row : hours.students()) {
      students.add(new FollowStudentClassHoursVO(
          row.peopleId(),
          row.name(),
          row.attendedHours(),
          row.plannedHours(),
          row.completionRate(),
          row.presentSessions(),
          row.absentSessions(),
          row.hoursStatus()));
    }
    return new FollowClassHoursVO(
        hours.plannedHours(),
        hours.plannedHoursFromExcel(),
        FollowClassHourCalculator.MINUTE_UNIT,
        hours.unconvertibleSessions(),
        hours.recordedStudentCount(),
        hours.unrecordedStudentCount(),
        hours.attendedHoursSum(),
        hours.attendedHoursAvg(),
        hours.hoursCompletionAvg(),
        students);
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }
}
