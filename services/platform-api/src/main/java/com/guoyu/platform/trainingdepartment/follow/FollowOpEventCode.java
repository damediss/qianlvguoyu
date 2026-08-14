package com.guoyu.platform.trainingdepartment.follow;

/**
 * 跟班过程操作事件白名单。新增跟班写操作须同步登记。
 */
public enum FollowOpEventCode {
  ATTENDANCE_SIGN_IN("attendance.sign_in", "attendance", "学员签到/登记出勤"),
  ATTENDANCE_BATCH_UPDATE("attendance.batch_update", "attendance", "批量更新考勤"),
  ATTENDANCE_IMPORT("attendance.import", "attendance", "导入考勤"),

  EVIDENCE_UPLOAD("evidence.upload", "evidence", "上传课堂影像"),
  EVIDENCE_REVIEW_APPROVE("evidence.review_approve", "evidence", "审核通过影像"),
  EVIDENCE_REVIEW_REJECT("evidence.review_reject", "evidence", "驳回影像"),
  EVIDENCE_DELETE("evidence.delete", "evidence", "删除影像"),
  EVIDENCE_PHOTO_PACK_GENERATE("evidence.photo_pack.generate", "evidence", "生成培训照片包"),

  SCHEDULE_SAVE("schedule.save", "schedule", "保存跟班课表"),
  SCHEDULE_REIMPORT("schedule.reimport", "schedule", "重导开班课表"),

  WORK_LOG_CREATE("work_log.create", "work_log", "发布工作日志"),
  WORK_LOG_UPDATE("work_log.update", "work_log", "更新工作日志"),
  WORK_LOG_DELETE("work_log.delete", "work_log", "删除工作日志"),

  TASK_PUBLISH("task.publish", "task", "发布任务"),
  TASK_SUBMIT("task.submit", "task", "提交任务"),
  TASK_REVIEW("task.review", "task", "批改任务"),
  TASK_CLOSE("task.close", "task", "关闭任务"),
  TASK_REOPEN("task.reopen", "task", "重新开放任务"),

  CLASS_ROLE_ASSIGN("class_role.assign", "role", "指派班级角色"),
  CLASS_ROLE_UNBIND("class_role.unbind", "role", "解绑班级角色");

  private final String code;
  private final String category;
  private final String defaultTitle;

  FollowOpEventCode(String code, String category, String defaultTitle) {
    this.code = code;
    this.category = category;
    this.defaultTitle = defaultTitle;
  }

  public String code() {
    return code;
  }

  public String category() {
    return category;
  }

  public String defaultTitle() {
    return defaultTitle;
  }
}
