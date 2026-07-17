package com.guoyu.platform.trainingdepartment.service.shiftassign;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ClassAssignResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.PersonDaySlot;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ScheduleParseResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ScheduleAlgorithmService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 二期班次可排规则：中班仅下午；夜班当天+次日无课。
 */
class ShiftAssignRuleTest {

  private final ScheduleAlgorithmService algorithm = new ScheduleAlgorithmService();

  @Test
  void midShift_onlyAfternoon() {
    ScheduleParseResult parse = baseParse(2026, 7);
    parse.setPersonDaySlots(List.of(
        slot("张三", 10, true, "中班"),
        slot("张三", 10, false, "中班")
    ));
    ClassAssignResult result = algorithm.runClassAssign(parse, ShiftAssignDefaults.config());
    Map<String, String> day = result.getPersonDayClass()
        .getOrDefault("张三", Map.of())
        .getOrDefault(LocalDate.of(2026, 7, 10), Map.of());
    assertFalse(day.containsKey("上午"), "中班不应排上午");
    assertTrue(day.containsKey("下午"), "中班应排下午");
  }

  @Test
  void nightShift_blocksTodayAndNextDay() {
    ScheduleParseResult parse = baseParse(2026, 7);
    List<PersonDaySlot> slots = new ArrayList<>();
    // 7/10 夜班：当天不可；次日 7/11 全禁
    slots.add(slot("李四", 10, true, "夜班"));
    slots.add(slot("李四", 10, false, "夜班"));
    // 7/11 白班本可上，但应被次日禁排挡住
    slots.add(slot("李四", 11, true, "白班"));
    slots.add(slot("李四", 11, false, "白班"));
    parse.setPersonDaySlots(slots);

    ClassAssignResult result = algorithm.runClassAssign(parse, ShiftAssignDefaults.config());
    Map<LocalDate, Map<String, String>> byDate =
        result.getPersonDayClass().getOrDefault("李四", Map.of());
    assertTrue(byDate.getOrDefault(LocalDate.of(2026, 7, 10), Map.of()).isEmpty(),
        "夜班当天不应有课");
    assertTrue(byDate.getOrDefault(LocalDate.of(2026, 7, 11), Map.of()).isEmpty(),
        "夜班次日应全禁");
  }

  private ScheduleParseResult baseParse(int year, int month) {
    ScheduleParseResult parse = new ScheduleParseResult();
    parse.setFactoryId("1");
    parse.setYear(year);
    parse.setMonth(month);
    parse.setShiftTimeMap(ShiftAssignDefaults.defaultShiftTimeMap());
    return parse;
  }

  private PersonDaySlot slot(String name, int day, boolean morning, String shift) {
    return PersonDaySlot.builder()
        .factoryId("1")
        .name(name)
        .day(day)
        .morning(morning)
        .shiftName(shift)
        .rowIndex(1)
        .build();
  }
}
