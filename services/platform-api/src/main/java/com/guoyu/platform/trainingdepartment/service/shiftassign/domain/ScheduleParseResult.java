package com.guoyu.platform.trainingdepartment.service.shiftassign.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Excel 解析结果：某厂区排班数据 + 班次时间表
 */
@Data
public class ScheduleParseResult {
    private String factoryId;
    private int year;
    private int month;
    /** 班次名称 -> 时间定义（该厂区） */
    private Map<String, ShiftTimeDef> shiftTimeMap;
    /** 每人每日上午/下午班次 */
    private List<PersonDaySlot> personDaySlots;
    /** 老师名单（可选，从配置或表内取） */
    private List<String> teacherNames;
    /** 提取失败行（无姓名或无法识别为数据行），每项为 "第N行：原因" 或 行号+原始内容摘要 */
    private List<String> failedExtractRows;
    /** 未识别班次名单，每项为 "姓名 工号? N日 班次「xxx」未在班次时间中配置" */
    private List<String> unrecognizedShiftList;
}
