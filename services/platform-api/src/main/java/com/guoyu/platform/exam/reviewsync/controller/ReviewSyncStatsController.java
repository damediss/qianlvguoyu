package com.guoyu.platform.exam.reviewsync.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.exam.reviewsync.service.ReviewSyncStatsApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 同步学统计（管理端）——《AI同步学统计功能设计方案》v1.1 §5.2。
 *
 * <p>全部 GET 只读；权限码 {@code exam:review-sync:stats:view}，
 * 导出单独 {@code exam:review-sync:stats:export}。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/exam/review-sync/stats")
public class ReviewSyncStatsController {

  private final ReviewSyncStatsApplicationService statsService;

  public ReviewSyncStatsController(ReviewSyncStatsApplicationService statsService) {
    this.statsService = statsService;
  }

  /** #1 概览卡片 */
  @GetMapping("/overview")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<Map<String, Object>> overview(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long examId) {
    return ApiResponse.ok(statsService.overview(from, to, examId));
  }

  /** #2 每日使用趋势 */
  @GetMapping("/daily-trend")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<Map<String, Object>> dailyTrend(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long examId) {
    return ApiResponse.ok(statsService.dailyTrend(from, to, examId));
  }

  /** #3 错题：牌级分布 + 新增/消灭趋势 */
  @GetMapping("/wrong-trend")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<Map<String, Object>> wrongTrend(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long examId) {
    return ApiResponse.ok(statsService.wrongTrend(from, to, examId));
  }

  /** #4 知识点掌握度（L1 考核项目 + L3 明细） */
  @GetMapping("/knowledge-mastery")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<Map<String, Object>> knowledgeMastery(
      @RequestParam Long examId,
      @RequestParam(required = false) Long platformUserId) {
    return ApiResponse.ok(statsService.knowledgeMastery(examId, platformUserId));
  }

  /** #5 学员明细分页 */
  @GetMapping("/students")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<PageResponse<Map<String, Object>>> students(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize,
      @RequestParam(required = false) Long examId,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(statsService.students(page, pageSize, examId, keyword));
  }

  /** #6 学员详情 */
  @GetMapping("/students/{planId}")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:view')")
  public ApiResponse<Map<String, Object>> studentDetail(@PathVariable Long planId) {
    return ApiResponse.ok(statsService.studentDetail(planId));
  }

  /** #7 学员明细 Excel 导出（上限 5000 行，方案 §7） */
  @GetMapping("/students/export")
  @PreAuthorize("hasAuthority('exam:review-sync:stats:export')")
  public void exportStudents(
      @RequestParam(required = false) Long examId,
      @RequestParam(required = false) String keyword,
      HttpServletResponse response) throws IOException {
    List<Map<String, Object>> rows = statsService.studentExportRows(examId, keyword);

    String filename = "AI同步学学员明细_"
        + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
        + URLEncoder.encode(filename, StandardCharsets.UTF_8));

    String[] headers = {"学员", "手机号", "身份证", "考试计划", "阶段", "进度", "状态",
        "累计练题", "新题", "复盘", "模考", "正确率%", "红牌", "黄牌", "已掌握", "加星", "最近活跃"};
    try (Workbook workbook = new SXSSFWorkbook(200);
         OutputStream out = response.getOutputStream()) {
      Sheet sheet = workbook.createSheet("学员明细");
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }
      int rowIndex = 1;
      for (Map<String, Object> data : rows) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        row.createCell(col++).setCellValue(str(data.get("realname")));
        row.createCell(col++).setCellValue(str(data.get("phone")));
        row.createCell(col++).setCellValue(str(data.get("idcard")));
        row.createCell(col++).setCellValue(str(data.get("examTitle")));
        row.createCell(col++).setCellValue("阶段" + num(data.get("stage")));
        row.createCell(col++).setCellValue("第" + num(data.get("dayIndex")) + "/" + num(data.get("totalDays")) + "天");
        row.createCell(col++).setCellValue(statusText(str(data.get("status"))));
        row.createCell(col++).setCellValue(num(data.get("answers")));
        row.createCell(col++).setCellValue(num(data.get("newAnswers")));
        row.createCell(col++).setCellValue(num(data.get("reviewAnswers")));
        row.createCell(col++).setCellValue(num(data.get("mockAnswers")));
        row.createCell(col++).setCellValue(numDouble(data.get("accuracy")));
        row.createCell(col++).setCellValue(num(data.get("redCnt")));
        row.createCell(col++).setCellValue(num(data.get("yellowCnt")));
        row.createCell(col++).setCellValue(num(data.get("greenCnt")));
        row.createCell(col++).setCellValue(num(data.get("starCnt")));
        row.createCell(col).setCellValue(str(data.get("lastActiveDate")));
      }
      workbook.write(out);
      out.flush();
    }
    log.info("[AI复习统计] 学员明细导出：rows={} examId={}", rows.size(), examId);
  }

  private static String statusText(String status) {
    return switch (status == null ? "" : status) {
      case "active" -> "进行中";
      case "completed" -> "已完成";
      case "abandoned" -> "已放弃";
      default -> status == null ? "" : status;
    };
  }

  private static String str(Object value) {
    return value != null ? String.valueOf(value) : "";
  }

  private static long num(Object value) {
    return value instanceof Number number ? number.longValue() : 0;
  }

  private static double numDouble(Object value) {
    return value instanceof Number number ? number.doubleValue() : 0;
  }
}
