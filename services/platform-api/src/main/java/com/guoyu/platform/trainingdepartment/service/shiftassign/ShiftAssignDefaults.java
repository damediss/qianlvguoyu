package com.guoyu.platform.trainingdepartment.service.shiftassign;

import com.guoyu.platform.trainingdepartment.service.shiftassign.config.EffectiveSchedulingConfig;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ShiftTimeDef;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ShiftTimeParser;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 二期默认班次/排课配置（对齐开发文档：早/白全天、中仅下午、夜当天+次日禁排）。
 */
public final class ShiftAssignDefaults {

  private ShiftAssignDefaults() {}

  public static EffectiveSchedulingConfig config() {
    return new EffectiveSchedulingConfig();
  }

  public static List<String> theoryTeachers() {
    return Arrays.asList("老师A", "老师B");
  }

  public static List<String> practiceTeachers() {
    return Arrays.asList("老师A", "老师B");
  }

  public static Map<String, ShiftTimeDef> defaultShiftTimeMap() {
    Map<String, ShiftTimeDef> map = new LinkedHashMap<>();
    for (Map.Entry<String, boolean[]> e : ShiftTimeParser.DEFAULT_SHIFT_RULES.entrySet()) {
      String name = e.getKey();
      if (name == null || name.isBlank()) {
        continue;
      }
      boolean[] allow = e.getValue();
      ShiftTimeDef def = new ShiftTimeDef();
      def.setShiftName(name);
      def.setAllowMorning(allow[0]);
      def.setAllowAfternoon(allow[1]);
      boolean night = ShiftTimeParser.isNightShift(name) || "0".equals(name);
      def.setNextDayBlock(night ? 3 : 0);
      def.setCrossDay(night);
      if (allow[0] && allow[1]) {
        def.setStartTime("08:00");
        def.setEndTime("16:00");
      } else if (!allow[0] && allow[1]) {
        def.setStartTime("16:00");
        def.setEndTime("24:00");
      } else if (night) {
        def.setStartTime("00:00");
        def.setEndTime("08:00");
      }
      map.put(ShiftTimeParser.normalizeShiftNameForLookup(name), def);
      map.putIfAbsent(name, def);
    }
    map.putIfAbsent("休", ShiftTimeDef.rest());
    return map;
  }
}
