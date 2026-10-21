package com.guoyu.platform.trainingdepartment.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassRoleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassTeacherPayEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowEvidenceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowIssueEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowLogEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowOpLogEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowPhotoPackEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSupervisionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowSurveyResponseEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainGraduationArchiveEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainGraduationNoticeEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleChangeRequestEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainShiftAssignRecordEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassTeacherPayMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowEvidenceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowIssueMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowOpLogMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowPhotoPackMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSupervisionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowSurveyResponseMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationArchiveMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainGraduationNoticeMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleChangeRequestMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainShiftAssignRecordMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskLogMapper;
import com.guoyu.platform.trainingdepartment.service.ClassCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.service.ClassPracticeScopeApplicationService;
import com.guoyu.platform.trainingdepartment.service.ClassReviewSyncApplicationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 删除班级时的拦截校验与关联清理。
 *
 * <p>保留 platform_user 学员账号；证书/补贴/结业等正式业务数据禁止随班删除。
 */
@Component
@Slf4j
public class TrainClassDeleteCascadeSupport {

  private final TrainPeopleListMapper peopleListMapper;
  private final TrainPlanRosterMapper planRosterMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainTaskLogMapper taskLogMapper;
  private final TrainShiftAssignRecordMapper shiftAssignRecordMapper;
  private final TrainCertificateMapper certificateMapper;
  private final TrainSubsidyMapper subsidyMapper;
  private final TrainGraduationArchiveMapper graduationArchiveMapper;
  private final TrainGraduationNoticeMapper graduationNoticeMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainScheduleChangeRequestMapper scheduleChangeRequestMapper;
  private final TrainAttendanceMapper attendanceMapper;
  private final TrainClassRoleMapper classRoleMapper;
  private final TrainClassTeacherPayMapper teacherPayMapper;
  private final TrainFollowTaskMapper followTaskMapper;
  private final TrainFollowEvidenceMapper followEvidenceMapper;
  private final TrainFollowPhotoPackMapper followPhotoPackMapper;
  private final TrainFollowLogMapper followLogMapper;
  private final TrainFollowOpLogMapper followOpLogMapper;
  private final TrainFollowIssueMapper followIssueMapper;
  private final TrainFollowSupervisionMapper followSupervisionMapper;
  private final TrainFollowSurveyResponseMapper followSurveyResponseMapper;
  private final ClassPracticeScopeApplicationService classPracticeScopeApplicationService;
  private final ClassReviewSyncApplicationService classReviewSyncApplicationService;

  public TrainClassDeleteCascadeSupport(
      TrainPeopleListMapper peopleListMapper,
      TrainPlanRosterMapper planRosterMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainTaskLogMapper taskLogMapper,
      TrainShiftAssignRecordMapper shiftAssignRecordMapper,
      TrainCertificateMapper certificateMapper,
      TrainSubsidyMapper subsidyMapper,
      TrainGraduationArchiveMapper graduationArchiveMapper,
      TrainGraduationNoticeMapper graduationNoticeMapper,
      TrainScheduleMapper scheduleMapper,
      TrainScheduleChangeRequestMapper scheduleChangeRequestMapper,
      TrainAttendanceMapper attendanceMapper,
      TrainClassRoleMapper classRoleMapper,
      TrainClassTeacherPayMapper teacherPayMapper,
      TrainFollowTaskMapper followTaskMapper,
      TrainFollowEvidenceMapper followEvidenceMapper,
      TrainFollowPhotoPackMapper followPhotoPackMapper,
      TrainFollowLogMapper followLogMapper,
      TrainFollowOpLogMapper followOpLogMapper,
      TrainFollowIssueMapper followIssueMapper,
      TrainFollowSupervisionMapper followSupervisionMapper,
      TrainFollowSurveyResponseMapper followSurveyResponseMapper,
      ClassPracticeScopeApplicationService classPracticeScopeApplicationService,
      ClassReviewSyncApplicationService classReviewSyncApplicationService) {
    this.peopleListMapper = peopleListMapper;
    this.planRosterMapper = planRosterMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.taskLogMapper = taskLogMapper;
    this.shiftAssignRecordMapper = shiftAssignRecordMapper;
    this.certificateMapper = certificateMapper;
    this.subsidyMapper = subsidyMapper;
    this.graduationArchiveMapper = graduationArchiveMapper;
    this.graduationNoticeMapper = graduationNoticeMapper;
    this.scheduleMapper = scheduleMapper;
    this.scheduleChangeRequestMapper = scheduleChangeRequestMapper;
    this.attendanceMapper = attendanceMapper;
    this.classRoleMapper = classRoleMapper;
    this.teacherPayMapper = teacherPayMapper;
    this.followTaskMapper = followTaskMapper;
    this.followEvidenceMapper = followEvidenceMapper;
    this.followPhotoPackMapper = followPhotoPackMapper;
    this.followLogMapper = followLogMapper;
    this.followOpLogMapper = followOpLogMapper;
    this.followIssueMapper = followIssueMapper;
    this.followSupervisionMapper = followSupervisionMapper;
    this.followSurveyResponseMapper = followSurveyResponseMapper;
    this.classPracticeScopeApplicationService = classPracticeScopeApplicationService;
    this.classReviewSyncApplicationService = classReviewSyncApplicationService;
  }

  /**
   * 正式业务数据未清时禁止删班，避免证书/补贴/结业孤儿或误删归档。
   */
  public void assertDeletable(Long classId) {
    if (classId == null) {
      return;
    }
    List<String> blockers = new ArrayList<>();
    Long certCount = certificateMapper.selectCount(new LambdaQueryWrapper<TrainCertificateEntity>()
        .eq(TrainCertificateEntity::getClassId, classId)
        .eq(TrainCertificateEntity::getDeleted, 0));
    if (certCount != null && certCount > 0) {
      blockers.add("证书 " + certCount + " 条");
    }
    Long subsidyCount = subsidyMapper.selectCount(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getClassId, classId)
        .eq(TrainSubsidyEntity::getDeleted, 0));
    if (subsidyCount != null && subsidyCount > 0) {
      blockers.add("补贴单 " + subsidyCount + " 条");
    }
    Long archiveCount = graduationArchiveMapper.selectCount(
        new LambdaQueryWrapper<TrainGraduationArchiveEntity>()
            .eq(TrainGraduationArchiveEntity::getClassId, classId));
    if (archiveCount != null && archiveCount > 0) {
      blockers.add("结业归档 " + archiveCount + " 条");
    }
    Long noticeCount = graduationNoticeMapper.selectCount(
        new LambdaQueryWrapper<TrainGraduationNoticeEntity>()
            .eq(TrainGraduationNoticeEntity::getClassId, classId));
    if (noticeCount != null && noticeCount > 0) {
      blockers.add("毕业典礼通知 " + noticeCount + " 条");
    }
    if (!blockers.isEmpty()) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "班级已产生正式业务数据，无法删除（"
              + String.join("、", blockers)
              + "）。请先在对应模块处理后再删班，或保留班级作归档。");
    }
  }

  /**
   * 删除班级关联清理。须在软删班级行之前调用。
   */
  @Transactional
  public Map<String, Object> cleanup(Long classId) {
    Map<String, Object> stats = emptyStats();
    if (classId == null) {
      return stats;
    }
    LocalDateTime now = LocalDateTime.now();

    List<TrainPeopleListEntity> people = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTrainClassId, classId));

    // 先从同步学/复习试卷名单摘人，再删配置与花名册
    int unenrolled = classReviewSyncApplicationService.unenrollRemovedStudents(classId, people);
    stats.put("examPeopleUnenrolled", unenrolled);

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

    List<TrainSignUpSelfEntity> plans = signUpSelfMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
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

    List<TrainTaskLogEntity> tasks = taskLogMapper.selectList(
        new LambdaQueryWrapper<TrainTaskLogEntity>()
            .eq(TrainTaskLogEntity::getTrainClassId, classId));
    for (TrainTaskLogEntity task : tasks) {
      taskLogMapper.deleteById(task.getId());
    }
    stats.put("tasksRemoved", tasks.size());

    int shiftUnbound = shiftAssignRecordMapper.update(null,
        new LambdaUpdateWrapper<TrainShiftAssignRecordEntity>()
            .eq(TrainShiftAssignRecordEntity::getTrainClassId, classId)
            .set(TrainShiftAssignRecordEntity::getTrainClassId, null));
    stats.put("shiftRecordsUnbound", shiftUnbound);

    stats.put("schedulesRemoved", softDeleteByClass(scheduleMapper, TrainScheduleEntity::getClassId, classId));
    stats.put(
        "scheduleChangesRemoved",
        softDeleteByClass(scheduleChangeRequestMapper, TrainScheduleChangeRequestEntity::getClassId, classId));
    stats.put("attendanceRemoved", softDeleteByClass(attendanceMapper, TrainAttendanceEntity::getClassId, classId));
    stats.put("rolesRemoved", softDeleteByClass(classRoleMapper, TrainClassRoleEntity::getClassId, classId));
    stats.put("teacherPayRemoved", softDeleteByClass(teacherPayMapper, TrainClassTeacherPayEntity::getClassId, classId));

    stats.put("followTasksRemoved", softDeleteByClass(followTaskMapper, TrainFollowTaskEntity::getClassId, classId));
    stats.put(
        "followEvidenceRemoved",
        softDeleteByClass(followEvidenceMapper, TrainFollowEvidenceEntity::getClassId, classId));
    stats.put(
        "followPhotoPacksRemoved",
        softDeleteByClass(followPhotoPackMapper, TrainFollowPhotoPackEntity::getClassId, classId));
    stats.put("followLogsRemoved", softDeleteByClass(followLogMapper, TrainFollowLogEntity::getClassId, classId));
    stats.put(
        "followOpLogsRemoved", softDeleteByClass(followOpLogMapper, TrainFollowOpLogEntity::getClassId, classId));
    stats.put("followIssuesRemoved", softDeleteByClass(followIssueMapper, TrainFollowIssueEntity::getClassId, classId));
    stats.put(
        "followSupervisionsRemoved",
        softDeleteByClass(followSupervisionMapper, TrainFollowSupervisionEntity::getClassId, classId));
    stats.put(
        "followSurveysRemoved",
        softDeleteByClass(followSurveyResponseMapper, TrainFollowSurveyResponseEntity::getClassId, classId));

    stats.put("practiceScopeRemoved", classPracticeScopeApplicationService.cleanupByClassId(classId));
    stats.put("reviewSyncRemoved", classReviewSyncApplicationService.cleanupByClassId(classId));

    log.info("class delete cascade classId={} stats={}", classId, stats);
    return stats;
  }

  public static Map<String, Object> emptyStats() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("examPeopleUnenrolled", 0);
    stats.put("peopleRemoved", 0);
    stats.put("rosterReset", 0);
    stats.put("collectionPlansRemoved", 0);
    stats.put("collectionPeopleRemoved", 0);
    stats.put("tasksRemoved", 0);
    stats.put("shiftRecordsUnbound", 0);
    stats.put("schedulesRemoved", 0);
    stats.put("scheduleChangesRemoved", 0);
    stats.put("attendanceRemoved", 0);
    stats.put("rolesRemoved", 0);
    stats.put("teacherPayRemoved", 0);
    stats.put("followTasksRemoved", 0);
    stats.put("followEvidenceRemoved", 0);
    stats.put("followPhotoPacksRemoved", 0);
    stats.put("followLogsRemoved", 0);
    stats.put("followOpLogsRemoved", 0);
    stats.put("followIssuesRemoved", 0);
    stats.put("followSupervisionsRemoved", 0);
    stats.put("followSurveysRemoved", 0);
    stats.put("practiceScopeRemoved", 0);
    stats.put("reviewSyncRemoved", 0);
    return stats;
  }

  public static void mergeStats(Map<String, Object> total, Map<String, Object> one) {
    for (Map.Entry<String, Object> e : one.entrySet()) {
      int cur = total.get(e.getKey()) instanceof Number n ? n.intValue() : 0;
      int add = e.getValue() instanceof Number n ? n.intValue() : 0;
      total.put(e.getKey(), cur + add);
    }
  }

  private <T> int softDeleteByClass(
      com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
      com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> classIdGetter,
      Long classId) {
    List<T> rows = mapper.selectList(new LambdaQueryWrapper<T>().eq(classIdGetter, classId));
    int n = 0;
    for (T row : rows) {
      Long id = resolveEntityId(row);
      if (id == null) {
        continue;
      }
      mapper.deleteById(id);
      n++;
    }
    return n;
  }

  private static Long resolveEntityId(Object row) {
    if (row instanceof com.guoyu.platform.common.domain.BaseEntity base) {
      return base.getId();
    }
    if (row instanceof TrainAttendanceEntity att) {
      return att.getId();
    }
    if (row instanceof TrainScheduleEntity sch) {
      return sch.getId();
    }
    try {
      Object id = row.getClass().getMethod("getId").invoke(row);
      return id instanceof Number num ? num.longValue() : null;
    } catch (ReflectiveOperationException ex) {
      return null;
    }
  }
}
