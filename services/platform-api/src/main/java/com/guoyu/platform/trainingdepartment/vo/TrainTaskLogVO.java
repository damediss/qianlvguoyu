package com.guoyu.platform.trainingdepartment.vo;

/** 培训任务记录视图对象 */
public record TrainTaskLogVO(
    Long id,
    /** 培训班级ID */
    Long trainClassId,
    /** 打卡ID */
    Long ckId,
    /** 日期 */
    String date,
    /** 添加时间 */
    Integer addtime,
    /** 状态 */
    Integer status,
    /** 时间段 */
    String times
) {}
