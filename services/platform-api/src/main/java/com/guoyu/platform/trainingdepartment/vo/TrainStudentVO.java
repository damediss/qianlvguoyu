package com.guoyu.platform.trainingdepartment.vo;

/** 报名学员视图对象 */
public record TrainStudentVO(
    Long id,
    String name,
    String phone,
    String idCardNumber,
    String gender
) {}
