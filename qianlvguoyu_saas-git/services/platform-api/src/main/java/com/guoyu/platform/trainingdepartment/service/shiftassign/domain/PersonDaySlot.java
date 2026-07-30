package com.guoyu.platform.trainingdepartment.service.shiftassign.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 某人某日某时段（上午/下午）的班次
 * 人员唯一标识：有工号用工号，无工号用姓名（避免同厂重名混淆）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDaySlot {
    private String factoryId;   // 厂区
    private String name;        // 姓名（展示用）
    private String employeeNo;  // 工号（可选，用于唯一标识、与学员主数据关联）
    private String dept;        // 工段/部门（与排班表「工段」或「部门」列对应）
    private String shiftClass;  // 班别（与排班表「班别」列对应）
    private int day;            // 日期 1-31
    private boolean morning;    // true=上午, false=下午
    private String shiftName;   // 班次名称（休/白班/四班两倒早班等）
    /**
     * Excel 原表中的行号（数据区起始行为 headerRow+1）。用于课表视图按原表顺序展示人员。
     */
    private Integer rowIndex;
}
