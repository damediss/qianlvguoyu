package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassRoleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentHours;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 跟班考勤表导出（矩阵式 学员×日期，模板填充）。
 *
 * <p>口径（docs/04_requirements/跟班考勤导出考勤表方案.md）：全部考勤日期、
 * 日期格同日多课次按日合并取最优（出勤>迟到>早退>缺勤）；
 * 行末有效出勤/迟到/早退/缺勤按<strong>课次次数</strong>计（与详情抽屉逐条 status 累加同口径，不按日合并）；
 * 已上课时/完成率复用 FollowClassHourCalculator；末列签名留空。</p>
 */
@Service
@Slf4j
public class TrainAttendanceExportService {

  private static final String TEMPLATE_PATH = "templates/培训考勤表.xlsx";

  /** 0-based 模板行：标题 / 信息行×2 / 表头 / 数据原型行 */
  private static final int ROW_TITLE = 0;
  private static final int ROW_INFO_1 = 1;
  private static final int ROW_INFO_2 = 2;
  private static final int ROW_HEADER = 3;
  private static final int ROW_PROTO = 4;

  /** 0-based 固定列：序号/姓名/手机/身份证号 | 有效出勤/迟到/早退/缺勤/已上课时/完成率/签名 */
  private static final int FIXED_COL_COUNT = 11;
  /** 日期列插在 身份证号(3) 之后、有效出勤(4) 之前 */
  private static final int DATE_COL_START = 4;
  /** 日期列宽：需容纳跨年列头 yyyy-MM-dd（10 字符）一行显示，过窄会换行截断 */
  private static final int DATE_COL_WIDTH_UNIT = 11 * 256;

  /** 状态文案，与跟班考勤页 ATTENDANCE_STATUS 一致 */
  private static final String[] STATUS_LABELS = {"缺勤", "出勤", "迟到", "早退"};

  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainAttendanceMapper attendanceMapper;
  private final TrainClassRoleMapper classRoleMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainFollowClassHourApplicationService classHourService;
  private final TrainingClassAccessGuard classAccessGuard;

  public TrainAttendanceExportService(
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainAttendanceMapper attendanceMapper,
      TrainClassRoleMapper classRoleMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainFollowClassHourApplicationService classHourService,
      TrainingClassAccessGuard classAccessGuard) {
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.attendanceMapper = attendanceMapper;
    this.classRoleMapper = classRoleMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.classHourService = classHourService;
    this.classAccessGuard = classAccessGuard;
  }

  /** 矩阵装配结果（纯数据，独立于模板写出，便于单测断言） */
  record SheetModel(
      String infoLine1,
      String infoLine2,
      List<String> dateColumns,
      List<RowModel> rows) {}

  /**
   * 一行学员：固定列 + 日期格（空串=无记录）+ 汇总统计。
   *
   * @param effectivePresent 有效出勤次数（逐条 status∈{出勤,迟到,早退}），与详情抽屉同口径
   */
  record RowModel(
      int seq,
      String name,
      String phone,
      String idCard,
      List<String> cells,
      int effectivePresent,
      int late,
      int early,
      int absent,
      String attendedHours,
      String completionRate) {}

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> export(Long classId) {
    TrainInfoClassEntity clazz = classAccessGuard.requireAccessibleClass(classId);
    List<TrainPeopleListEntity> students = listStudents(classId);
    if (students.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该班级暂无学员，无法导出考勤表");
    }
    List<TrainAttendanceEntity> attendances = attendanceMapper.selectList(
        new LambdaQueryWrapper<TrainAttendanceEntity>()
            .eq(TrainAttendanceEntity::getClassId, classId)
            .eq(TrainAttendanceEntity::getDeleted, 0));
    Map<Long, StudentHours> hoursByPeople = new HashMap<>();
    classHourService.compute(classId, currentTenantId()).students()
        .forEach(h -> hoursByPeople.put(h.peopleId(), h));

    List<String> dates = sortedDistinctDates(attendances);
    SheetModel model = assemble(
        infoLine1(clazz),
        infoLine2(clazz, classId),
        dateColumns(dates),
        students,
        attendances,
        hoursByPeople);
    byte[] bytes = buildWorkbookBytes(model);
    String fileName = "考勤表_" + sanitizeFileName(resolveClassTitle(clazz))
        + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
    return xlsxResponse(bytes, fileName);
  }

  // ---- 装配（纯函数，单测直接断言） ----

  static SheetModel assemble(
      String infoLine1,
      String infoLine2,
      List<String> dateColumns,
      List<TrainPeopleListEntity> students,
      List<TrainAttendanceEntity> attendances,
      Map<Long, StudentHours> hoursByPeople) {
    // 日期格：userId + 归一日期 → 最优状态（同日多课次合并，仅影响格内文案）
    Map<Long, Map<String, Integer>> bestByUserDate = new HashMap<>();
    // 汇总列：按课次次数累加（与详情抽屉逐条 status 同口径，不按日合并）
    Map<Long, int[]> countByUser = new HashMap<>(); // [onTime, late, early, absent]
    for (TrainAttendanceEntity att : attendances) {
      if (att.getUserId() == null) {
        continue;
      }
      String date = normalizeDate(att.getAttendanceDate());
      if (!date.isEmpty()) {
        bestByUserDate
            .computeIfAbsent(att.getUserId(), k -> new HashMap<>())
            .merge(date, att.getStatus(), (oldVal, newVal) ->
                statusRank(newVal) > statusRank(oldVal) ? newVal : oldVal);
      }
      Integer status = att.getStatus();
      if (status == null || status < 0 || status >= STATUS_LABELS.length) {
        continue;
      }
      int[] c = countByUser.computeIfAbsent(att.getUserId(), k -> new int[4]);
      switch (status) {
        case 1 -> c[0]++;
        case 2 -> c[1]++;
        case 3 -> c[2]++;
        case 0 -> c[3]++;
        default -> { }
      }
    }
    List<String> dates = sortedDistinctDates(attendances);

    List<RowModel> rows = new ArrayList<>();
    int seq = 1;
    for (TrainPeopleListEntity student : students) {
      Map<String, Integer> byDate = bestByUserDate.get(student.getId());
      List<String> cells = new ArrayList<>(dates.size());
      for (String date : dates) {
        Integer status = byDate == null ? null : byDate.get(date);
        if (status == null || status < 0 || status >= STATUS_LABELS.length) {
          cells.add("");
          continue;
        }
        cells.add(STATUS_LABELS[status]);
      }
      int[] c = countByUser.getOrDefault(student.getId(), new int[4]);
      int onTime = c[0];
      int late = c[1];
      int early = c[2];
      int absent = c[3];
      // 有效出勤次数 = 出勤 + 迟到 + 早退（与 StudentAttendanceDetailDrawer 同口径）
      int effectivePresent = onTime + late + early;
      StudentHours hours = hoursByPeople.get(student.getId());
      rows.add(new RowModel(
          seq++,
          nullToDash(student.getRealname()),
          nullToDash(student.getPhone()),
          nullToDash(student.getIdCard()),
          cells,
          effectivePresent,
          late,
          early,
          absent,
          formatHours(hours == null ? null : hours.attendedHours()),
          formatRate(hours == null ? null : hours.completionRate())));
    }
    return new SheetModel(infoLine1, infoLine2, dateColumns, rows);
  }

  /** 状态优先级，与跟班考勤页 attendanceStatusRank 一致：出勤>迟到>早退>缺勤；未知状态不参与合并 */
  static int statusRank(Integer status) {
    if (status == null) {
      return -1;
    }
    return switch (status) {
      case 1 -> 4;
      case 2 -> 3;
      case 3 -> 2;
      case 0 -> 1;
      default -> -1;
    };
  }

  /** 归一日期：yyyy-M-d（可带时间后缀）→ yyyy-MM-dd；无法解析取前 10 字符；空返回空串 */
  static String normalizeDate(String raw) {
    String s = raw == null ? "" : raw.trim();
    if (s.isEmpty()) {
      return "";
    }
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(s);
    if (m.find()) {
      return m.group(1) + "-" + String.format("%02d", Integer.parseInt(m.group(2)))
          + "-" + String.format("%02d", Integer.parseInt(m.group(3)));
    }
    return s.length() >= 10 ? s.substring(0, 10) : s;
  }

  static List<String> sortedDistinctDates(List<TrainAttendanceEntity> attendances) {
    TreeSet<String> set = new TreeSet<>();
    for (TrainAttendanceEntity att : attendances) {
      String date = normalizeDate(att.getAttendanceDate());
      if (!date.isEmpty()) {
        set.add(date);
      }
    }
    return new ArrayList<>(set);
  }

  /** 列头：同年 MM-dd，跨年 yyyy-MM-dd */
  static List<String> dateColumns(List<String> dates) {
    boolean sameYear = true;
    String year = dates.isEmpty() ? null : dates.get(0).substring(0, 4);
    for (String d : dates) {
      if (!d.startsWith(year)) {
        sameYear = false;
        break;
      }
    }
    List<String> cols = new ArrayList<>(dates.size());
    for (String d : dates) {
      cols.add(sameYear ? d.substring(5) : d);
    }
    return cols;
  }

  // ---- 模板写出 ----

  byte[] buildWorkbookBytes(SheetModel model) {
    try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream();
        XSSFWorkbook wb = new XSSFWorkbook(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.getSheetAt(0);
      int dateCount = model.dateColumns().size();

      // shift 前捕获样式原型：表头「有效出勤」格（日期列头样式）、数据原型行各列样式
      CellStyle dateHeaderStyle = sheet.getRow(ROW_HEADER).getCell(DATE_COL_START).getCellStyle();
      Row proto = sheet.getRow(ROW_PROTO);
      Map<Integer, CellStyle> protoStyles = new LinkedHashMap<>();
      for (int c = 0; c < FIXED_COL_COUNT; c++) {
        Cell cell = proto.getCell(c);
        if (cell != null) {
          protoStyles.put(c, cell.getCellStyle());
        }
      }

      if (dateCount > 0) {
        // 固定汇总列（有效出勤~签名，4~10）右移 dateCount 列，腾出日期列
        sheet.shiftColumns(DATE_COL_START, FIXED_COL_COUNT - 1, dateCount);
        // shiftColumns 不搬合并区域：标题/信息行合并重新铺到全宽
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
          if (sheet.getMergedRegion(i).getLastRow() <= ROW_INFO_2) {
            sheet.removeMergedRegion(i);
          }
        }
        for (int r = ROW_TITLE; r <= ROW_INFO_2; r++) {
          sheet.addMergedRegion(new CellRangeAddress(r, r, 0, FIXED_COL_COUNT - 1 + dateCount));
        }
        for (int i = 0; i < dateCount; i++) {
          sheet.setColumnWidth(DATE_COL_START + i, DATE_COL_WIDTH_UNIT);
        }
      }

      writeInfo(sheet, model);
      writeDateHeaders(sheet, model, dateHeaderStyle);
      rewriteEffectivePresentHeader(sheet, dateCount);
      ensureDataRows(sheet, model.rows().size(), protoStyles, dateCount);
      fillRows(sheet, model, protoStyles, dateCount);
      // 模板打印区只到第5行（原型行），数据多于1行时必须扩到末行，否则打印被截断
      // 参数序：sheetIndex, startColumn, endColumn, startRow, endRow
      wb.setPrintArea(0, 0, FIXED_COL_COUNT + dateCount - 1,
          0, ROW_PROTO + model.rows().size() - 1);

      wb.write(bos);
      return bos.toByteArray();
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("考勤表导出失败", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出考勤表失败: " + ex.getMessage());
    }
  }

  private void writeInfo(Sheet sheet, SheetModel model) {
    replaceInfoCell(sheet.getRow(ROW_INFO_1), model.infoLine1());
    replaceInfoCell(sheet.getRow(ROW_INFO_2), model.infoLine2());
  }

  /**
   * 模板信息行单元格是 openpyxl 生成的 inlineStr，POI 对 inlineStr 直接 setCellValue
   * 写不进（值仍显示模板占位文字），需先删再建（同获证名册 writeClassName 的既有处理）。
   */
  private static void replaceInfoCell(Row row, String value) {
    CellStyle style = null;
    Cell existing = row.getCell(0);
    if (existing != null) {
      style = existing.getCellStyle();
      row.removeCell(existing);
    }
    Cell cell = row.createCell(0);
    cell.setCellValue(value);
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  private void writeDateHeaders(Sheet sheet, SheetModel model, CellStyle style) {
    Row header = sheet.getRow(ROW_HEADER);
    for (int i = 0; i < model.dateColumns().size(); i++) {
      Cell cell = header.createCell(DATE_COL_START + i);
      cell.setCellValue(model.dateColumns().get(i));
      cell.setCellStyle(style);
    }
  }

  /** 模板原「出勤」表头改为「有效出勤」（与数据列口径一致；兼容 openpyxl inlineStr）。 */
  private static void rewriteEffectivePresentHeader(Sheet sheet, int dateCount) {
    Row header = sheet.getRow(ROW_HEADER);
    if (header == null) {
      return;
    }
    int col = DATE_COL_START + dateCount;
    Cell existing = header.getCell(col);
    CellStyle style = existing == null ? null : existing.getCellStyle();
    if (existing != null) {
      header.removeCell(existing);
    }
    Cell cell = header.createCell(col);
    cell.setCellValue("有效出勤");
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  /** 数据行不足时以原型行为样板扩行（样式逐列复制，含右移后的汇总/签名列） */
  private void ensureDataRows(
      Sheet sheet, int studentCount, Map<Integer, CellStyle> protoStyles, int dateCount) {
    int need = Math.max(studentCount, 1);
    int totalCols = FIXED_COL_COUNT + dateCount;
    Row proto = sheet.getRow(ROW_PROTO);
    for (int i = 1; i < need; i++) {
      int rowIdx = ROW_PROTO + i;
      Row row = sheet.getRow(rowIdx);
      if (row == null) {
        row = sheet.createRow(rowIdx);
      }
      row.setHeight(proto.getHeight());
      for (int c = 0; c < totalCols; c++) {
        Cell cell = row.getCell(c);
        if (cell == null) {
          cell = row.createCell(c);
        }
        CellStyle style = styleFor(c, protoStyles, dateCount);
        if (style != null) {
          cell.setCellStyle(style);
        }
      }
    }
  }

  /**
   * 样式映射：日期列统一取原型「有效出勤」列（原 4 列）样式；
   * 汇总/签名列（右移后）回映射到原模板列号取样式（完成率/签名 otherwise 会落空）。
   */
  private static CellStyle styleFor(int col, Map<Integer, CellStyle> protoStyles, int dateCount) {
    if (col >= DATE_COL_START && col < DATE_COL_START + dateCount) {
      return protoStyles.get(DATE_COL_START);
    }
    if (col >= DATE_COL_START + dateCount) {
      return protoStyles.get(col - dateCount);
    }
    return protoStyles.get(col);
  }

  private void fillRows(Sheet sheet, SheetModel model, Map<Integer, CellStyle> protoStyles,
      int dateCount) {
    int summaryStart = DATE_COL_START + dateCount;
    int seq = 0;
    for (RowModel rowModel : model.rows()) {
      Row row = sheet.getRow(ROW_PROTO + seq);
      if (row == null) {
        row = sheet.createRow(ROW_PROTO + seq);
      }
      applyStyle(row, 0, protoStyles, dateCount);
      applyStyle(row, 1, protoStyles, dateCount);
      applyStyle(row, 2, protoStyles, dateCount);
      applyStyle(row, 3, protoStyles, dateCount);
      setCellString(row.getCell(0), String.valueOf(rowModel.seq()));
      setCellString(row.getCell(1), rowModel.name());
      setCellString(row.getCell(2), rowModel.phone());
      setCellString(row.getCell(3), rowModel.idCard());
      for (int d = 0; d < rowModel.cells().size(); d++) {
        applyStyle(row, DATE_COL_START + d, protoStyles, dateCount);
        setCellString(row.getCell(DATE_COL_START + d), rowModel.cells().get(d));
      }
      // 汇总：有效出勤/迟到/早退/缺勤/已上课时/完成率 + 签名留空
      int c = summaryStart;
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), String.valueOf(rowModel.effectivePresent()));
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), String.valueOf(rowModel.late()));
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), String.valueOf(rowModel.early()));
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), String.valueOf(rowModel.absent()));
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), rowModel.attendedHours());
      applyStyle(row, c, protoStyles, dateCount);
      setCellString(row.getCell(c++), rowModel.completionRate());
      applyStyle(row, c, protoStyles, dateCount);
      seq++;
    }
  }

  private static void applyStyle(Row row, int col, Map<Integer, CellStyle> protoStyles,
      int dateCount) {
    Cell cell = row.getCell(col);
    if (cell == null) {
      cell = row.createCell(col);
    }
    CellStyle style = styleFor(col, protoStyles, dateCount);
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  // ---- 信息行装配 ----

  String infoLine1(TrainInfoClassEntity clazz) {
    return "班级名称：" + nullToDash(resolveClassTitle(clazz))
        + "    培训工种及等级：" + nullToDash(workTypeAndLevel(clazz))
        + "    培训时间：" + nullToDash(trainPeriod(clazz));
  }

  String infoLine2(TrainInfoClassEntity clazz, Long classId) {
    return "企业：" + nullToDash(resolveCompanyName(clazz))
        + "    班主任：" + nullToDash(resolveHeadTeachers(classId))
        + "    导出日期：" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private String resolveClassTitle(TrainInfoClassEntity clazz) {
    if (StringUtils.hasText(clazz.getTitle())) {
      return clazz.getTitle().trim();
    }
    if (clazz.getTrainInfoId() != null) {
      TrainInfoListEntity info = infoListMapper.selectById(clazz.getTrainInfoId());
      if (info != null && StringUtils.hasText(info.getTitle())) {
        return info.getTitle().trim();
      }
    }
    return "班级" + clazz.getId();
  }

  private String workTypeAndLevel(TrainInfoClassEntity clazz) {
    String workTypeName = "";
    if (clazz.getWorkTypeId() != null && clazz.getWorkTypeId() > 0) {
      ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(clazz.getWorkTypeId());
      if (wt != null && StringUtils.hasText(wt.getTitle())) {
        workTypeName = wt.getTitle().trim();
      }
    }
    String level = GraduationCertifiedRosterExportService.formatLevelLabel(clazz.getWorkTypeLevel());
    if (!StringUtils.hasText(workTypeName)) {
      return StringUtils.hasText(level) ? level : "";
    }
    return StringUtils.hasText(level) ? workTypeName + " / " + level : workTypeName;
  }

  private String trainPeriod(TrainInfoClassEntity clazz) {
    String start = clazz.getDateStart() == null ? "" : clazz.getDateStart().trim();
    String end = clazz.getDateEnd() == null ? "" : clazz.getDateEnd().trim();
    if (start.isEmpty() && end.isEmpty()) {
      return "";
    }
    return start + " 至 " + end;
  }

  private String resolveCompanyName(TrainInfoClassEntity clazz) {
    if (clazz.getCompanyId() != null && clazz.getCompanyId() > 0) {
      OrgEntity org = orgMapper.selectById(clazz.getCompanyId());
      if (org != null && StringUtils.hasText(org.getName())) {
        return org.getName().trim();
      }
    }
    if (clazz.getTrainInfoId() != null) {
      TrainInfoListEntity info = infoListMapper.selectById(clazz.getTrainInfoId());
      if (info != null && StringUtils.hasText(info.getCompanyName())) {
        return info.getCompanyName().trim();
      }
    }
    return "";
  }

  private String resolveHeadTeachers(Long classId) {
    List<TrainClassRoleEntity> roles = classRoleMapper.selectList(
        new LambdaQueryWrapper<TrainClassRoleEntity>()
            .eq(TrainClassRoleEntity::getClassId, classId)
            .eq(TrainClassRoleEntity::getRoleCode, "head_teacher")
            .eq(TrainClassRoleEntity::getStatus, "active")
            .eq(TrainClassRoleEntity::getDeleted, 0)
            .orderByAsc(TrainClassRoleEntity::getId));
    List<String> teachers = new ArrayList<>();
    for (TrainClassRoleEntity role : roles) {
      String name = role.getDisplayName() == null ? "" : role.getDisplayName().trim();
      if (name.isEmpty()) {
        continue;
      }
      String phone = role.getPhone() == null ? "" : role.getPhone().trim();
      teachers.add(phone.isEmpty() ? name : name + " " + phone);
    }
    return String.join("、", teachers);
  }

  private List<TrainPeopleListEntity> listStudents(Long classId) {
    return peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTrainClassId, classId)
        .eq(TrainPeopleListEntity::getType, "student")
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByAsc(TrainPeopleListEntity::getId));
  }

  private Long currentTenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  // ---- 格式化与写出工具 ----

  static String formatHours(BigDecimal hours) {
    if (hours == null) {
      return "—";
    }
    return hours.setScale(1, RoundingMode.HALF_UP).toPlainString();
  }

  static String formatRate(BigDecimal rate) {
    if (rate == null) {
      return "—";
    }
    return rate.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
  }

  private static void setCellString(Cell cell, String value) {
    cell.setCellValue(value == null ? "" : value);
  }

  private static String nullToDash(String v) {
    return StringUtils.hasText(v) ? v.trim() : "—";
  }

  static String sanitizeFileName(String name) {
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  static ResponseEntity<byte[]> xlsxResponse(byte[] bytes, String fileName) {
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }
}
