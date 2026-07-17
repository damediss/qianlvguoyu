package com.guoyu.platform.trainingdepartment.service.shiftassign.domain;

import lombok.Data;

/**
 * 班次时间定义：班次名称 → 开始/结束时间（某厂区）
 */
@Data
public class ShiftTimeDef {
    private String shiftName;
    private String startTime;  // HH:mm
    private String endTime;   // HH:mm, 跨日则 end < start
    private boolean crossDay; // 是否跨日（兼容旧数据；新逻辑优先用 nextDayBlock）

    /**
     * 是否允许排课（上班可抽 45 分钟上课）：true=允许，false=不允许，null=未配置（走默认规则）
     */
    private Boolean allowMorning;
    private Boolean allowAfternoon;

    /**
     * 次日限制（可拓展配置）：
     * 0=不限制；1=次日上午不可排；2=次日下午不可排；3=次日全天不可排
     */
    private Integer nextDayBlock;

    public static ShiftTimeDef rest() {
        ShiftTimeDef d = new ShiftTimeDef();
        d.setShiftName("休");
        d.setCrossDay(false);
        d.setAllowMorning(false);
        d.setAllowAfternoon(false);
        d.setNextDayBlock(0);
        return d;
    }
}
