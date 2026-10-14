package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainClassRoleEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassRoleMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceExportService.RowModel;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceExportService.SheetModel;
import com.guoyu.platform.trainingdepartment.support.FollowClassHourCalculator.StudentHours;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class TrainAttendanceExportServiceTest {

  // ---- 装配：基础与合并口径（T1~T7） ----

  @Test
  void assemble_singleRecord_fillsCellAndCount() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    TrainAttendanceEntity att = attendance(1L, "2026-10-08", 1);

    SheetModel model = assembleSheet(List.of(s), List.of(att));

    assertEquals(List.of("10-08"), model.dateColumns());
    RowModel row = model.rows().get(0);
    assertEquals("出勤", row.cells().get(0));
    assertEquals(1, row.present());
    assertEquals(0, row.late());
    assertEquals(0, row.early());
    assertEquals(0, row.absent());
  }

  @Test
  void assemble_sameDayAbsentPlusPresent_winsPresentAndCountsMergedOnly() {
    // 同日：上午缺勤 + 下午出勤 → 出勤；缺勤不重复计数（按合并后口径）
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    List<TrainAttendanceEntity> atts = List.of(
        attendance(1L, "2026-10-08", 0),
        attendance(1L, "2026-10-08", 1));

    RowModel row = assembleSheet(List.of(s), atts).rows().get(0);

    assertEquals(List.of("出勤"), row.cells());
    assertEquals(1, row.present());
    assertEquals(0, row.absent());
  }

  @Test
  void assemble_priorityOrder_winnerAlwaysBeatsLoser() {
    // 优先级：出勤(1) > 迟到(2) > 早退(3) > 缺勤(0)，8 组两两覆盖
    int[][] pairs = {{1, 2}, {1, 3}, {1, 0}, {2, 3}, {2, 0}, {3, 0}, {2, 1}, {0, 3}};
    int[] expected = {1, 1, 1, 2, 2, 3, 1, 3};
    for (int i = 0; i < pairs.length; i++) {
      TrainPeopleListEntity s = student(1L, "学员" + i);
      List<TrainAttendanceEntity> atts = List.of(
          attendance(1L, "2026-10-08", pairs[i][0]),
          attendance(1L, "2026-10-08", pairs[i][1]));
      RowModel row = assembleSheet(List.of(s), atts).rows().get(0);
      String[] labels = {"缺勤", "出勤", "迟到", "早退"};
      assertEquals(labels[expected[i]], row.cells().get(0),
          "pair " + pairs[i][0] + " vs " + pairs[i][1]);
    }
  }

  @Test
  void assemble_latePlusEarly_winsLate() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    List<TrainAttendanceEntity> atts = List.of(
        attendance(1L, "2026-10-08", 2),
        attendance(1L, "2026-10-08", 3));

    RowModel row = assembleSheet(List.of(s), atts).rows().get(0);

    assertEquals("迟到", row.cells().get(0));
    assertEquals(1, row.late());
    assertEquals(0, row.early());
    assertEquals(0, row.absent());
  }

  @Test
  void assemble_studentWithoutRecords_blankCellsAndZeroCounts() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    TrainAttendanceEntity other = attendance(2L, "2026-10-08", 1);

    SheetModel model = assembleSheet(List.of(s), List.of(other));

    RowModel row = model.rows().get(0);
    assertEquals("", row.cells().get(0));
    assertEquals(0, row.present());
    assertEquals(0, row.absent());
  }

  @Test
  void assemble_datesSortedAndDistinct() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    List<TrainAttendanceEntity> atts = List.of(
        attendance(1L, "2026-10-10", 1),
        attendance(1L, "2026-10-08 08:30:00", 1), // 带时间后缀的归一
        attendance(1L, "2026-10-08", 2),
        attendance(1L, "2026-10-09", 3));

    SheetModel model = assembleSheet(List.of(s), atts);

    assertEquals(List.of("10-08", "10-09", "10-10"), model.dateColumns());
    // 10-08 合并：出勤 + 迟到 → 出勤
    assertEquals("出勤", model.rows().get(0).cells().get(0));
  }

  @Test
  void assemble_zeroAttendance_emptyDateColumns() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");

    SheetModel model = assembleSheet(List.of(s), List.of());

    assertTrue(model.dateColumns().isEmpty());
    assertEquals(1, model.rows().size());
    assertTrue(model.rows().get(0).cells().isEmpty());
  }

  @Test
  void assemble_crossYearDates_useFullDateHeader() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    List<TrainAttendanceEntity> atts = List.of(
        attendance(1L, "2025-12-30", 1),
        attendance(1L, "2026-01-02", 1));

    SheetModel model = assembleSheet(List.of(s), atts);

    assertEquals(List.of("2025-12-30", "2026-01-02"), model.dateColumns());
  }

  // ---- 课时列（T9/T10） ----

  @Test
  void assemble_hoursMappedByPeopleId() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    Map<Long, StudentHours> hours = new HashMap<>();
    hours.put(1L, new StudentHours(1L, "黄毅坚",
        new BigDecimal("32.05"), new BigDecimal("126"), new BigDecimal("25.44"),
        10, 2, "ok"));

    RowModel row = assembleSheet(List.of(s), List.of(), hours).rows().get(0);

    assertEquals("32.1", row.attendedHours());
    assertEquals("25.4%", row.completionRate());
  }

  @Test
  void assemble_studentWithoutAttendance_hoursStillFilled() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    TrainAttendanceEntity other = attendance(2L, "2026-10-08", 1);
    Map<Long, StudentHours> hours = new HashMap<>();
    hours.put(1L, new StudentHours(1L, "黄毅坚",
        new BigDecimal("8.0"), new BigDecimal("126"), new BigDecimal("6.3"),
        2, 0, "unrecorded"));

    RowModel row = assembleSheet(List.of(s), List.of(other), hours).rows().get(0);

    assertEquals("", row.cells().get(0));
    assertEquals("8.0", row.attendedHours());
    assertEquals("6.3%", row.completionRate());
  }

  @Test
  void assemble_missingHours_usesDash() {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    RowModel row = assembleSheet(List.of(s), List.of()).rows().get(0);
    assertEquals("—", row.attendedHours());
    assertEquals("—", row.completionRate());
  }

  // ---- 空学员班级（T8） ----

  @Test
  void export_emptyStudents_throwsBadRequest() {
    TrainInfoClassMapper classMapper = mock(TrainInfoClassMapper.class);
    TrainInfoListMapper infoListMapper = mock(TrainInfoListMapper.class);
    TrainPeopleListMapper peopleListMapper = mock(TrainPeopleListMapper.class);
    TrainAttendanceMapper attendanceMapper = mock(TrainAttendanceMapper.class);
    TrainClassRoleMapper classRoleMapper = mock(TrainClassRoleMapper.class);
    OrgMapper orgMapper = mock(OrgMapper.class);
    ExamWorkTypeMapper examWorkTypeMapper = mock(ExamWorkTypeMapper.class);
    TrainFollowClassHourApplicationService hourService =
        mock(TrainFollowClassHourApplicationService.class);
    TrainingClassAccessGuard guard = mock(TrainingClassAccessGuard.class);

    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setId(9L);
    clazz.setTitle("空班");
    when(guard.requireAccessibleClass(9L)).thenReturn(clazz);
    when(peopleListMapper.selectList(any())).thenReturn(List.of());

    TrainAttendanceExportService service = new TrainAttendanceExportService(
        classMapper, infoListMapper, peopleListMapper, attendanceMapper, classRoleMapper,
        orgMapper, examWorkTypeMapper, hourService, guard);

    BusinessException ex = assertThrows(BusinessException.class, () -> service.export(9L));
    assertTrue(ex.getMessage().contains("暂无学员"));
  }

  // ---- 信息行（T11~T13） ----

  @Test
  void infoLines_missingFields_renderDash() {
    TrainAttendanceExportService service = serviceWith(List.of());
    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setId(9L);
    clazz.setTitle("电工班");
    // workTypeId/companyId/level/日期全空 → 各字段显示 —

    String line1 = service.infoLine1(clazz);
    String line2 = service.infoLine2(clazz, 9L);

    assertTrue(line1.contains("班级名称：电工班"));
    assertTrue(line1.contains("培训工种及等级：—"));
    assertTrue(line1.contains("培训时间：—"));
    assertTrue(line2.contains("企业：—"));
    assertTrue(line2.contains("班主任：—"));
    assertTrue(line2.contains("导出日期："));
  }

  @Test
  void infoLines_multipleHeadTeachers_joinedWithDunhao() {
    TrainClassRoleEntity r1 = new TrainClassRoleEntity();
    r1.setId(1L);
    r1.setDisplayName("张三");
    r1.setPhone("13800000000");
    TrainClassRoleEntity r2 = new TrainClassRoleEntity();
    r2.setId(2L);
    r2.setDisplayName("李四");
    r2.setPhone(null);
    TrainAttendanceExportService service = serviceWith(List.of(r1, r2));
    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setId(9L);
    clazz.setTitle("电工班");

    String line2 = service.infoLine2(clazz, 9L);

    assertTrue(line2.contains("班主任：张三 13800000000、李四"), line2);
  }

  // ---- 模板写出（T11/T15/T16） ----

  @Test
  void buildWorkbook_writesTitleInfoHeadersDatesRowsAndBlankSignature() throws Exception {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    s.setPhone("15878607437");
    s.setIdCard("452600199001011234");
    Map<Long, StudentHours> hours = new HashMap<>();
    hours.put(1L, new StudentHours(1L, "黄毅坚",
        new BigDecimal("32.0"), new BigDecimal("126"), new BigDecimal("25.4"),
        10, 2, "ok"));
    SheetModel model = TrainAttendanceExportService.assemble(
        "班级名称：电工班",
        "企业：某某公司",
        List.of("10-08", "10-09"),
        List.of(s),
        List.of(
            attendance(1L, "2026-10-08", 1),
            attendance(1L, "2026-10-09", 0)),
        hours);

    TrainAttendanceExportService service = serviceWith(List.of());
    byte[] bytes = service.buildWorkbookBytes(model);

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheetAt(0);
      assertEquals("职业技能培训考勤表", sheet.getRow(0).getCell(0).getStringCellValue());
      assertEquals("班级名称：电工班", sheet.getRow(1).getCell(0).getStringCellValue());
      assertEquals("企业：某某公司", sheet.getRow(2).getCell(0).getStringCellValue());

      Row header = sheet.getRow(3);
      assertEquals("序号", header.getCell(0).getStringCellValue());
      assertEquals("身份证号", header.getCell(3).getStringCellValue());
      assertEquals("10-08", header.getCell(4).getStringCellValue());
      assertEquals("10-09", header.getCell(5).getStringCellValue());
      assertEquals("出勤", header.getCell(6).getStringCellValue());
      assertEquals("签名", header.getCell(12).getStringCellValue());

      Row data = sheet.getRow(4);
      assertEquals("1", data.getCell(0).getStringCellValue());
      assertEquals("黄毅坚", data.getCell(1).getStringCellValue());
      assertEquals("15878607437", data.getCell(2).getStringCellValue());
      assertEquals("452600199001011234", data.getCell(3).getStringCellValue());
      assertEquals("出勤", data.getCell(4).getStringCellValue());
      assertEquals("缺勤", data.getCell(5).getStringCellValue());
      assertEquals("1", data.getCell(6).getStringCellValue());
      assertEquals("1", data.getCell(9).getStringCellValue());
      assertEquals("32.0", data.getCell(10).getStringCellValue());
      assertEquals("25.4%", data.getCell(11).getStringCellValue());
      assertEquals("", data.getCell(12).getStringCellValue());
      // 标题合并区应覆盖到日期+汇总+签名全宽（K+N 列）
      assertEquals(13, sheet.getMergedRegion(0).getLastColumn() + 1);
    }
  }

  @Test
  void buildWorkbook_zeroDates_keepsFixedColumns() throws Exception {
    TrainPeopleListEntity s = student(1L, "黄毅坚");
    SheetModel model = TrainAttendanceExportService.assemble(
        "i1", "i2", List.of(), List.of(s), List.of(), Map.of());

    byte[] bytes = serviceWith(List.of()).buildWorkbookBytes(model);

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheetAt(0);
      Row header = sheet.getRow(3);
      assertEquals("出勤", header.getCell(4).getStringCellValue());
      assertEquals("签名", header.getCell(10).getStringCellValue());
      Row data = sheet.getRow(4);
      assertEquals("黄毅坚", data.getCell(1).getStringCellValue());
      assertEquals("0", data.getCell(4).getStringCellValue());
      assertEquals("", data.getCell(10).getStringCellValue());
    }
  }

  @Test
  void buildWorkbook_largeMatrix_generatesAllRows() throws Exception {
    List<TrainPeopleListEntity> students = new ArrayList<>();
    List<TrainAttendanceEntity> atts = new ArrayList<>();
    for (long i = 1; i <= 50; i++) {
      students.add(student(i, "学员" + i));
      for (int d = 1; d <= 60; d++) {
        atts.add(attendance(i, String.format("2026-10-%02d", d), 1));
      }
    }
    SheetModel model = TrainAttendanceExportService.assemble(
        "i1", "i2",
        TrainAttendanceExportService.dateColumns(TrainAttendanceExportService.sortedDistinctDates(atts)),
        students, atts, Map.of());

    byte[] bytes = serviceWith(List.of()).buildWorkbookBytes(model);

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheetAt(0);
      // 表头：4 固定列 + 60 日期列 + 7 汇总/签名列
      assertEquals(71, sheet.getRow(3).getLastCellNum());
      assertEquals(60, model.dateColumns().size());
      Row lastRow = sheet.getRow(4 + 49);
      assertEquals("学员50", lastRow.getCell(1).getStringCellValue());
      assertEquals("出勤", lastRow.getCell(4 + 59).getStringCellValue()); // 第60天
      assertEquals("60", lastRow.getCell(4 + 60).getStringCellValue()); // 出勤计数
    }
  }

  // ---- 工程性（T14/T15） ----

  @Test
  void sanitizeFileName_stripsIllegalChars() {
    assertEquals("电工_高级_班", TrainAttendanceExportService.sanitizeFileName("电工/高级:班"));
    assertEquals("a_b_c_d_e_f_g_h_i_j",
        TrainAttendanceExportService.sanitizeFileName("a\\b/c:d*e?f\"g<h>i|j"));
    assertEquals("plain", TrainAttendanceExportService.sanitizeFileName("  plain  "));
  }

  @Test
  void xlsxResponse_utf8ContentDisposition() {
    ResponseEntity<byte[]> resp =
        TrainAttendanceExportService.xlsxResponse(new byte[] {1}, "考勤表_电工班.xlsx");
    String cd = resp.getHeaders().getFirst("Content-Disposition");
    assertTrue(cd != null && cd.contains("filename*=UTF-8''"));
    assertTrue(cd.contains("%E8%80%83%E5%8B%A4%E8%A1%A8")); // 考勤表 URL 编码
    assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        resp.getHeaders().getContentType().toString());
  }

  // ---- 工具方法 ----

  private static SheetModel assembleSheet(
      List<TrainPeopleListEntity> students, List<TrainAttendanceEntity> atts) {
    return assembleSheet(students, atts, Map.of());
  }

  private static SheetModel assembleSheet(
      List<TrainPeopleListEntity> students,
      List<TrainAttendanceEntity> atts,
      Map<Long, StudentHours> hours) {
    List<String> dates = TrainAttendanceExportService.sortedDistinctDates(atts);
    return TrainAttendanceExportService.assemble(
        "i1", "i2", TrainAttendanceExportService.dateColumns(dates), students, atts, hours);
  }

  private static TrainPeopleListEntity student(long id, String name) {
    TrainPeopleListEntity p = new TrainPeopleListEntity();
    p.setId(id);
    p.setRealname(name);
    return p;
  }

  private static TrainAttendanceEntity attendance(long userId, String date, int status) {
    TrainAttendanceEntity a = new TrainAttendanceEntity();
    a.setUserId(userId);
    a.setAttendanceDate(date);
    a.setStatus(status);
    return a;
  }

  private static TrainAttendanceExportService serviceWith(List<TrainClassRoleEntity> roles) {
    TrainClassRoleMapper classRoleMapper = mock(TrainClassRoleMapper.class);
    when(classRoleMapper.selectList(any())).thenReturn(roles);
    return new TrainAttendanceExportService(
        mock(TrainInfoClassMapper.class),
        mock(TrainInfoListMapper.class),
        mock(TrainPeopleListMapper.class),
        mock(TrainAttendanceMapper.class),
        classRoleMapper,
        mock(OrgMapper.class),
        mock(ExamWorkTypeMapper.class),
        mock(TrainFollowClassHourApplicationService.class),
        mock(TrainingClassAccessGuard.class));
  }
}
