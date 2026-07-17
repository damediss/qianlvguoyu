package com.guoyu.platform.trainingdepartment.service.shiftassign.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 分班结果（某厂区）
 */
@Data
public class ClassAssignResult {
    private String factoryId;
    private int year;
    private int month;
    /** 理论日、实操日 */
    private List<LocalDate> theoryDays;
    private List<LocalDate> practiceDays;
    /** 每人每日班级：key=人员唯一键（工号或姓名）-> date -> slot(上午/下午) -> "上午1班" / "下午2班" */
    private Map<String, Map<LocalDate, Map<String, String>>> personDayClass;
    /** 人员唯一键 -> 姓名（展示用） */
    private Map<String, String> personKeyToName;
    /** 人员唯一键 -> 工号（可选） */
    private Map<String, String> personKeyToEmployeeNo;
    /** 人员唯一键 -> 工段/部门（展示、导出用） */
    private Map<String, String> personKeyToDept;
    /** 人员唯一键 -> 班别（展示、导出用） */
    private Map<String, String> personKeyToShiftClass;
    /** 人员唯一键 -> Excel 原表行序（用于课表视图按原表顺序展示） */
    private Map<String, Integer> personKeyToRowIndex;
    /** 每日统计：date -> 该日各班人数 [{slot, classIndex, count}] */
    private Map<LocalDate, List<ClassCount>> dailyStats;
    /** 未排入任何班的人员（全月休息、班次未配置等），存人员唯一键 */
    private List<String> unassignedPersons;
    /** 未排入原因：人员唯一键 -> 原因说明（便于排查） */
    private Map<String, String> unassignedReasons;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassCount {
        private String slot;      // 上午/下午
        private int classIndex;
        private int count;
    }
}
