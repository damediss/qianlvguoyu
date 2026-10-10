package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.service.shiftassign.ShiftAssignDefaults;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.PersonDaySlot;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ScheduleParseResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ExcelScheduleParseService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ShiftTimeParser;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 排班轻量画像：班组 / 主倒班类型 / 可上课槽位数（供 auto-pack S1 使用）。
 */
@Service
@RequiredArgsConstructor
public class TrainShiftProfileService {

  private final FileApplicationService fileApplicationService;
  private final ExcelScheduleParseService excelParseService;

  public Map<String, ShiftPersonProfile> loadProfiles(TrainThreeYearPlanEntity plan, int year, int month) {
    if (plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少企业排班表");
    }
    byte[] bytes = fileApplicationService.readFileBytesById(plan.getEnterpriseShiftFileId());
    ScheduleParseResult parse;
    try {
      parse = excelParseService.parseBytes(
          bytes,
          plan.getEnterpriseShiftFileName(),
          String.valueOf(plan.getTargetOrgId()),
          year,
          month,
          ShiftAssignDefaults.defaultShiftTimeMap());
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析排班表失败：" + ex.getMessage());
    }
    Map<String, ShiftPersonProfile> map = new LinkedHashMap<>();
    if (parse.getPersonDaySlots() == null) return map;
    for (PersonDaySlot slot : parse.getPersonDaySlots()) {
      if (slot == null || !StringUtils.hasText(slot.getName())) continue;
      String key = personKey(slot.getEmployeeNo(), slot.getName());
      ShiftPersonProfile p = map.computeIfAbsent(key, k -> {
        ShiftPersonProfile np = new ShiftPersonProfile();
        np.name = slot.getName().trim();
        np.employeeNo = StringUtils.hasText(slot.getEmployeeNo()) ? slot.getEmployeeNo().trim() : null;
        return np;
      });
      if (!StringUtils.hasText(p.shiftClass) && StringUtils.hasText(slot.getShiftClass())) {
        p.shiftClass = slot.getShiftClass().trim();
      }
      if (!StringUtils.hasText(p.dept) && StringUtils.hasText(slot.getDept())) {
        p.dept = slot.getDept().trim();
      }
      String shiftName = slot.getShiftName();
      String alias = ShiftTimeParser.resolveShiftAlias(shiftName);
      if (alias != null) shiftName = alias;
      String type = classifyShift(shiftName);
      p.typeCounts.merge(type, 1, Integer::sum);
      if (canAttend(shiftName, type)) {
        p.attendableSlots++;
      }
    }
    for (ShiftPersonProfile p : map.values()) {
      p.dominantType = dominantType(p.typeCounts);
    }
    return map;
  }

  public static String personKey(String employeeNo, String name) {
    if (StringUtils.hasText(employeeNo)) return "NO:" + employeeNo.trim().toUpperCase();
    return "N:" + (name == null ? "" : name.trim());
  }

  private static String classifyShift(String shiftName) {
    if (!StringUtils.hasText(shiftName) || ShiftTimeParser.isRest(shiftName)) return "休";
    if (ShiftTimeParser.isNightShift(shiftName)) return "夜";
    String n = ShiftTimeParser.normalizeShiftNameForLookup(shiftName);
    if (n.contains("中")) return "中";
    if (n.contains("早") || n.contains("白") || n.contains("行政") || n.contains("长白")) return "早";
    boolean[] rule = ShiftTimeParser.DEFAULT_SHIFT_RULES.get(n);
    if (rule != null) {
      if (!rule[0] && !rule[1]) return "夜";
      if (!rule[0] && rule[1]) return "中";
      return "早";
    }
    return "早";
  }

  private static boolean canAttend(String shiftName, String type) {
    if ("休".equals(type) || "夜".equals(type)) return false;
    if ("中".equals(type)) return true; // 下午可上
    if ("早".equals(type)) return true;
    String n = ShiftTimeParser.normalizeShiftNameForLookup(shiftName);
    boolean[] rule = ShiftTimeParser.DEFAULT_SHIFT_RULES.get(n);
    if (rule == null) return true;
    return rule[0] || rule[1];
  }

  private static String dominantType(Map<String, Integer> counts) {
    String best = "未识别";
    int max = -1;
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      if ("休".equals(e.getKey())) continue;
      if (e.getValue() > max) {
        max = e.getValue();
        best = e.getKey();
      }
    }
    if (max < 0 && counts.containsKey("休")) return "休";
    return best;
  }

  public static class ShiftPersonProfile {
    public String name;
    public String employeeNo;
    public String shiftClass;
    public String dept;
    public String dominantType;
    public int attendableSlots;
    public Map<String, Integer> typeCounts = new HashMap<>();
  }
}
