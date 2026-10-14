package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainAttendanceExportService;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceImportResultVO;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceVO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 培训考勤管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainAttendanceController {

  private final TrainAttendanceApplicationService trainAttendanceApplicationService;
  private final TrainAttendanceExportService trainAttendanceExportService;

  public TrainAttendanceController(
      TrainAttendanceApplicationService trainAttendanceApplicationService,
      TrainAttendanceExportService trainAttendanceExportService) {
    this.trainAttendanceApplicationService = trainAttendanceApplicationService;
    this.trainAttendanceExportService = trainAttendanceExportService;
  }

  /** 分页查询 */
  @GetMapping("/attendances")
  @PreAuthorize("hasAuthority('train:attendance:view')")
  public ApiResponse<PageResponse<TrainAttendanceVO>> page(@Valid @ModelAttribute TrainAttendanceQueryReq req) {
    return ApiResponse.ok(trainAttendanceApplicationService.page(req));
  }

  /** 根据ID查询 */
  @GetMapping("/attendances/{id}")
  @PreAuthorize("hasAuthority('train:attendance:view')")
  public ApiResponse<TrainAttendanceVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainAttendanceApplicationService.getById(id));
  }

  /** 创建 */
  @PostMapping("/attendances")
  @PreAuthorize("hasAuthority('train:attendance:create')")
  @AuditLog(module = "training", action = "attendance:create", targetType = "train_attendance")
  public ApiResponse<TrainAttendanceVO> create(@Valid @RequestBody TrainAttendanceCreateReq req) {
    return ApiResponse.ok(trainAttendanceApplicationService.create(req));
  }

  /** 更新 */
  @PutMapping("/attendances/{id}")
  @PreAuthorize("hasAuthority('train:attendance:update')")
  @AuditLog(module = "training", action = "attendance:update", targetType = "train_attendance")
  public ApiResponse<TrainAttendanceVO> update(@PathVariable Long id, @Valid @RequestBody TrainAttendanceUpdateReq req) {
    return ApiResponse.ok(trainAttendanceApplicationService.update(id, req));
  }

  /** 删除 */
  @DeleteMapping("/attendances/{id}")
  @PreAuthorize("hasAuthority('train:attendance:delete')")
  @AuditLog(module = "training", action = "attendance:delete", targetType = "train_attendance")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainAttendanceApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 签到 */
  @PostMapping("/attendances/sign-in")
  @PreAuthorize("hasAuthority('train:attendance:create')")
  @AuditLog(module = "training", action = "attendance:sign-in", targetType = "train_attendance")
  public ApiResponse<TrainAttendanceVO> signIn(@Valid @RequestBody TrainAttendanceCreateReq req) {
    return ApiResponse.ok(trainAttendanceApplicationService.signIn(req));
  }

  /**
   * 导入考勤信息列表 Excel（.xls/.xlsx），按身份证匹配本班学员；有时段则按课次行落库。
   */
  @PostMapping("/classes/{classId}/attendances/import")
  @PreAuthorize("hasAuthority('train:attendance:create')")
  @AuditLog(module = "training", action = "attendance:import", targetType = "train_attendance")
  public ApiResponse<TrainAttendanceImportResultVO> importExcel(
      @PathVariable Long classId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainAttendanceApplicationService.importFromExcel(classId, file));
  }

  /** 导出考勤表（矩阵式 xlsx：班级信息行 + 学员×日期 + 汇总统计 + 签名列） */
  @GetMapping("/classes/{classId}/attendances/export")
  @PreAuthorize("hasAuthority('train:attendance:export')")
  @AuditLog(module = "training", action = "attendance:export", targetType = "train_attendance")
  public ResponseEntity<byte[]> export(@PathVariable Long classId) {
    return trainAttendanceExportService.export(classId);
  }
}
