package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.FollowTeacherPaySaveReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassTeacherPayEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassTeacherPayMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.ClassTeacherDays;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.SessionInput;
import com.guoyu.platform.trainingdepartment.support.FollowTeacherDayCalculator.TeacherFormDays;
import com.guoyu.platform.trainingdepartment.vo.FollowTeacherDayRowVO;
import com.guoyu.platform.trainingdepartment.vo.FollowTeacherDaysVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainFollowTeacherDayApplicationService {

  private final TrainInfoClassMapper classMapper;
  private final TrainScheduleMapper scheduleMapper;
  private final TrainClassTeacherPayMapper payMapper;

  public TrainFollowTeacherDayApplicationService(
      TrainInfoClassMapper classMapper,
      TrainScheduleMapper scheduleMapper,
      TrainClassTeacherPayMapper payMapper) {
    this.classMapper = classMapper;
    this.scheduleMapper = scheduleMapper;
    this.payMapper = payMapper;
  }

  @Transactional(readOnly = true)
  public FollowTeacherDaysVO teacherDays(Long classId) {
    TrainInfoClassEntity clazz = requireClass(classId);
    ClassTeacherDays computed = compute(classId);
    Map<String, TrainClassTeacherPayEntity> payMap = loadPayMap(classId);
    return toVo(clazz, computed, payMap);
  }

  @Transactional
  public FollowTeacherDaysVO savePay(Long classId, FollowTeacherPaySaveReq req) {
    TrainInfoClassEntity clazz = requireClass(classId);
    Long tenantId = tenantId();
    Long operatorId = SecurityContext.currentUser().getUserId();
    List<FollowTeacherPaySaveReq.Item> items = req == null ? List.of() : req.getItems();
    for (FollowTeacherPaySaveReq.Item item : items) {
      String name = FollowTeacherDayCalculator.normalizeName(item.getTeacherName());
      String form = FollowTeacherDayCalculator.normalizeTeachForm(item.getTeachForm());
      if (!StringUtils.hasText(name)) {
        continue;
      }
      TrainClassTeacherPayEntity existing = payMapper.selectOne(
          new LambdaQueryWrapper<TrainClassTeacherPayEntity>()
              .eq(TrainClassTeacherPayEntity::getTenantId, tenantId)
              .eq(TrainClassTeacherPayEntity::getClassId, classId)
              .eq(TrainClassTeacherPayEntity::getTeacherName, name)
              .eq(TrainClassTeacherPayEntity::getTeachForm, form)
              .eq(TrainClassTeacherPayEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (existing == null) {
        TrainClassTeacherPayEntity entity = new TrainClassTeacherPayEntity();
        entity.setTenantId(tenantId);
        entity.setCreatedBy(operatorId);
        entity.setClassId(classId);
        entity.setTeacherName(name);
        entity.setTeachForm(form);
        entity.setRatePerDay(item.getRatePerDay());
        entity.setDeleted(0);
        payMapper.insert(entity);
      } else {
        existing.setRatePerDay(item.getRatePerDay());
        existing.setUpdatedBy(operatorId);
        payMapper.updateById(existing);
      }
    }
    ClassTeacherDays computed = compute(classId);
    return toVo(clazz, computed, loadPayMap(classId));
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportExcel(Long classId) {
    FollowTeacherDaysVO vo = teacherDays(classId);
    String classTitle = StringUtils.hasText(vo.classTitle()) ? vo.classTitle() : "班级";
    List<FollowTeacherDayRowVO> rows = vo.teachers() == null ? List.of() : vo.teachers();
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("教师课酬");
      Row header = sheet.createRow(0);
      String[] heads = {"序号", "班期", "授课老师", "授课形式", "授课天数", "课酬（天数/元）", "合计课酬", "备注"};
      for (int i = 0; i < heads.length; i++) {
        header.createCell(i).setCellValue(heads[i]);
      }
      int r = 1;
      for (FollowTeacherDayRowVO row : rows) {
        Row excelRow = sheet.createRow(r);
        excelRow.createCell(0).setCellValue(r);
        excelRow.createCell(1).setCellValue(classTitle);
        excelRow.createCell(2).setCellValue(nullToEmpty(row.teacherName()));
        excelRow.createCell(3).setCellValue(nullToEmpty(row.teachForm()));
        if (row.taughtDays() != null) {
          excelRow.createCell(4).setCellValue(row.taughtDays().doubleValue());
        } else {
          excelRow.createCell(4).setCellValue("");
        }
        if (row.ratePerDay() != null) {
          excelRow.createCell(5).setCellValue(row.ratePerDay().doubleValue());
        } else {
          excelRow.createCell(5).setCellValue("");
        }
        if (row.payTotal() != null) {
          excelRow.createCell(6).setCellValue(row.payTotal().doubleValue());
        } else {
          excelRow.createCell(6).setCellValue("");
        }
        String remark = row.remark();
        if (!StringUtils.hasText(remark) && row.ratePerDay() == null) {
          remark = "未设课酬";
        }
        excelRow.createCell(7).setCellValue(nullToEmpty(remark));
        r++;
      }
      if (rows.size() > 1) {
        sheet.addMergedRegion(new CellRangeAddress(1, rows.size(), 1, 1));
      }
      for (int i = 0; i < heads.length; i++) {
        sheet.autoSizeColumn(i);
      }
      wb.write(bos);
      String fileName = classTitle + "-教师课酬-" + LocalDate.now(FollowTeacherDayCalculator.ZONE)
          .format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
      String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(bos.toByteArray());
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出教师课酬失败：" + e.getMessage());
    }
  }

  private ClassTeacherDays compute(Long classId) {
    Long tenantId = tenantId();
    List<TrainScheduleEntity> schedules = scheduleMapper.selectList(
        new LambdaQueryWrapper<TrainScheduleEntity>()
            .eq(TrainScheduleEntity::getClassId, classId)
            .eq(TrainScheduleEntity::getDeleted, 0)
            .orderByAsc(TrainScheduleEntity::getScheduleDate)
            .orderByAsc(TrainScheduleEntity::getStartTime)
            .orderByAsc(TrainScheduleEntity::getId));
    if (tenantId != null) {
      schedules = schedules.stream()
          .filter(s -> s.getTenantId() == null || tenantId.equals(s.getTenantId()))
          .toList();
    }
    List<SessionInput> sessions = new ArrayList<>();
    for (TrainScheduleEntity s : schedules) {
      sessions.add(new SessionInput(
          s.getScheduleDate(), s.getStartTime(), s.getTeacherName(), s.getTeachForm()));
    }
    return FollowTeacherDayCalculator.compute(sessions);
  }

  private Map<String, TrainClassTeacherPayEntity> loadPayMap(Long classId) {
    Long tenantId = tenantId();
    List<TrainClassTeacherPayEntity> list = payMapper.selectList(
        new LambdaQueryWrapper<TrainClassTeacherPayEntity>()
            .eq(TrainClassTeacherPayEntity::getTenantId, tenantId)
            .eq(TrainClassTeacherPayEntity::getClassId, classId)
            .eq(TrainClassTeacherPayEntity::getDeleted, 0));
    Map<String, TrainClassTeacherPayEntity> map = new HashMap<>();
    for (TrainClassTeacherPayEntity e : list) {
      String key = payKey(e.getTeacherName(), e.getTeachForm());
      map.putIfAbsent(key, e);
    }
    return map;
  }

  private FollowTeacherDaysVO toVo(
      TrainInfoClassEntity clazz,
      ClassTeacherDays computed,
      Map<String, TrainClassTeacherPayEntity> payMap) {
    List<FollowTeacherDayRowVO> rows = new ArrayList<>();
    BigDecimal payTotalSum = BigDecimal.ZERO;
    for (TeacherFormDays t : computed.teachers()) {
      TrainClassTeacherPayEntity pay = payMap.get(payKey(t.teacherName(), t.teachForm()));
      BigDecimal rate = pay == null ? null : pay.getRatePerDay();
      String remark = pay == null ? null : pay.getRemark();
      BigDecimal payTotal = null;
      if (rate != null && t.taughtDays() != null) {
        payTotal = rate.multiply(t.taughtDays()).setScale(0, RoundingMode.HALF_UP);
        payTotalSum = payTotalSum.add(payTotal);
      }
      rows.add(new FollowTeacherDayRowVO(
          t.teacherName(),
          t.teachForm(),
          t.taughtSessionCount(),
          t.taughtDays(),
          t.plannedSessionCount(),
          t.plannedDays(),
          t.overflowSessionCount(),
          rate,
          payTotal,
          remark));
    }
    return new FollowTeacherDaysVO(
        clazz.getId(),
        clazz.getTitle(),
        computed.sessionUnitDays(),
        computed.maxSessionsPerDay(),
        computed.asOfDate() == null ? null : computed.asOfDate().toString(),
        computed.assignedSessionCount(),
        computed.unassignedSessionCount(),
        computed.overflowSessionCount(),
        computed.taughtDaysSum(),
        computed.plannedDaysSum(),
        payTotalSum.setScale(0, RoundingMode.HALF_UP),
        rows);
  }

  private static String payKey(String teacherName, String teachForm) {
    return FollowTeacherDayCalculator.normalizeName(teacherName)
        + "|"
        + FollowTeacherDayCalculator.normalizeTeachForm(teachForm);
  }

  private TrainInfoClassEntity requireClass(Long classId) {
    TrainInfoClassEntity clazz = classMapper.selectOne(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .eq(TrainInfoClassEntity::getId, classId)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (clazz == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在");
    }
    return clazz;
  }

  private Long tenantId() {
    Long t = TenantContextHolder.getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
