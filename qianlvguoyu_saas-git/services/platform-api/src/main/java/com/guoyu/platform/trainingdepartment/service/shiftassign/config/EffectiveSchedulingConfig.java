package com.guoyu.platform.trainingdepartment.service.shiftassign.config;

import lombok.Data;

/**
 * 排课使用的有效配置：优先 DB（系统设置），无则用 yml 默认
 */
@Data
public class EffectiveSchedulingConfig {
    private int classMin = 30;
    private int classMax = 50;
    private int theoryHoursTotal = 48;
    private int practiceHoursTotal = 122;
    private int hoursPerSession = 4;
    private String morningStart = "08:00";
    private String morningEnd = "12:00";
    private String afternoonStart = "14:00";
    private String afternoonEnd = "18:00";
    /** 理论课达标比例（0.8=80%，1.0=100%），用于控制「修够多少课时就停止排课」 */
    private double theoryCompletionRate = 1.0;
    /** 实操课达标比例（0.8=80%，1.0=100%） */
    private double practiceCompletionRate = 1.0;
    /** 理论+实操总课时达标比例（0.8=80%）：总已排课时/总目标课时>=此值也视为已达标 */
    private double totalCompletionRate = 0.8;
}
