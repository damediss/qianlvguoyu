package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainTeacherBankApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankFileVO;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankImportResultVO;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training")
public class TrainTeacherBankController {

  private final TrainTeacherBankApplicationService trainTeacherBankApplicationService;

  public TrainTeacherBankController(TrainTeacherBankApplicationService trainTeacherBankApplicationService) {
    this.trainTeacherBankApplicationService = trainTeacherBankApplicationService;
  }

  @GetMapping("/teacher-banks")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ApiResponse<PageResponse<TrainTeacherBankVO>> page(
      @Valid @ModelAttribute TrainTeacherBankQueryReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.page(req));
  }

  @GetMapping("/teacher-banks/import/template")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ResponseEntity<byte[]> importTemplate() {
    return trainTeacherBankApplicationService.downloadImportTemplate();
  }

  @PostMapping("/teacher-banks/import")
  @PreAuthorize("hasAuthority('train:teacher-bank:create')")
  @AuditLog(module = "training", action = "teacher-bank:import", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(trainTeacherBankApplicationService.importExcel(file));
  }

  @PostMapping("/teacher-banks/migrate-from-legacy")
  @PreAuthorize("hasAuthority('train:teacher-bank:create')")
  @AuditLog(module = "training", action = "teacher-bank:migrate", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankImportResultVO> migrateFromLegacy(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "migrateImages", defaultValue = "true") boolean migrateImages) {
    return ApiResponse.ok(trainTeacherBankApplicationService.migrateFromLegacyJson(file, migrateImages));
  }

  @GetMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ApiResponse<TrainTeacherBankVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainTeacherBankApplicationService.getById(id));
  }

  @GetMapping("/teacher-banks/{id}/files")
  @PreAuthorize("hasAuthority('train:teacher-bank:view')")
  public ApiResponse<List<TrainTeacherBankFileVO>> listFiles(@PathVariable Long id) {
    return ApiResponse.ok(trainTeacherBankApplicationService.listFiles(id));
  }

  @PutMapping("/teacher-banks/{id}/files/{fileType}")
  @PreAuthorize("hasAuthority('train:teacher-bank:update')")
  @AuditLog(module = "training", action = "teacher-bank:bind-file", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> bindFile(
      @PathVariable Long id,
      @PathVariable String fileType,
      @RequestBody Map<String, Long> body) {
    Long fileId = body == null ? null : body.get("fileId");
    return ApiResponse.ok(trainTeacherBankApplicationService.bindFile(id, fileType, fileId));
  }

  @DeleteMapping("/teacher-banks/{id}/files/{fileType}")
  @PreAuthorize("hasAuthority('train:teacher-bank:update')")
  @AuditLog(module = "training", action = "teacher-bank:clear-file", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> clearFile(
      @PathVariable Long id, @PathVariable String fileType) {
    return ApiResponse.ok(trainTeacherBankApplicationService.clearFile(id, fileType));
  }

  @PostMapping("/teacher-banks")
  @PreAuthorize("hasAuthority('train:teacher-bank:create')")
  @AuditLog(module = "training", action = "teacher-bank:create", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> create(@Valid @RequestBody TrainTeacherBankCreateReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.create(req));
  }

  @PutMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:update')")
  @AuditLog(module = "training", action = "teacher-bank:update", targetType = "train_teacher_bank")
  public ApiResponse<TrainTeacherBankVO> update(
      @PathVariable Long id, @Valid @RequestBody TrainTeacherBankUpdateReq req) {
    return ApiResponse.ok(trainTeacherBankApplicationService.update(id, req));
  }

  @DeleteMapping("/teacher-banks/{id}")
  @PreAuthorize("hasAuthority('train:teacher-bank:delete')")
  @AuditLog(module = "training", action = "teacher-bank:delete", targetType = "train_teacher_bank")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainTeacherBankApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
