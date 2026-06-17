package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 考试计划视图对象 */
public record TrainExamListVO(
    Long id,
    String title,
    String level,
    Long trainId,
    Long orgId,
    String orgName,
    /** 0线上 1线下 */
    Integer type,
    /** 0内部 1外部 */
    Integer source,
    /** 监考人ID */
    Long invigilateUserId,
    /** 监考人 */
    String invigilateUsername,
    /** 考评人ID */
    Long evaluateUserId,
    /** 考评人 */
    String evaluateUsername,
    /** 督导员ID */
    Long watcherUserId,
    /** 督导员 */
    String watcherUsername,
    /** 考务人员ID */
    Long staffUserId,
    /** 考务人员 */
    String staffUsername,
    /** 命题人员ID */
    Long propositionUserId,
    /** 命题人员 */
    String propositionUsername,
    /** 阅卷人员ID */
    Long markingUserId,
    /** 阅卷人员 */
    String markingUsername,
    /** 试卷ID */
    Long examId,
    /** 考试日期 */
    String examDate,
    /** 0截止日期 1时间段 */
    Integer examDateMode,
    /** 考试日期列表 */
    String examDateList,
    /** 考试时间(分钟) */
    Integer examTime,
    /** 是否需要先完成游戏学 */
    Integer needFinishAdventure,
    /** 是否需要先完成常规学 */
    Integer needFinishNormal,
    Long userId,
    Integer sortNumber,
    Long tenantId,
    /** 机构ID */
    Long jigouId,
    LocalDateTime createTime,
    /** 关联工种列表（含 workTypeId/level） */
    List<TrainExamWorkTypeListVO> workTypeLists,
    /** 关联课程ID列表（从 workTypeLists 提取的 courseId） */
    List<Long> courseIdList
) {}
