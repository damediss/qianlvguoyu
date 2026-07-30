package com.guoyu.platform.trainingdepartment.service.shiftassign.domain;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 老师排课结果（某厂区）
 */
@Data
public class TeacherScheduleResult {
    private String factoryId;
    private int year;
    private int month;
    /** 老师 -> 日期 -> 上午/下午 -> 带班描述 如 "理论-上午1班" */
    private Map<String, Map<LocalDate, Map<String, String>>> teacherDaySchedule;
}
