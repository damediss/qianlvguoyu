package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.SignUpPeopleBatchReq;
import com.guoyu.platform.trainingdepartment.dto.SignUpStudentImportReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterImportService;
import com.guoyu.platform.trainingdepartment.service.PlatformUserApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpPeopleImportService;
import com.guoyu.platform.trainingdepartment.service.TrainSignUpPeopleSelfApplicationService;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpPeopleImportResultVO;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpPeopleSelfVO;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/training")
public class TrainSignUpPeopleSelfController {

  private final TrainSignUpPeopleSelfApplicationService trainSignUpPeopleSelfApplicationService;
  private final TrainSignUpCollectionApplicationService collectionService;
  private final TrainSignUpPeopleImportService importService;
  private final ClassPrepRosterImportService classPrepRosterImportService;
  private final PlatformUserApplicationService platformUserApplicationService;

  public TrainSignUpPeopleSelfController(
      TrainSignUpPeopleSelfApplicationService trainSignUpPeopleSelfApplicationService,
      TrainSignUpCollectionApplicationService collectionService,
      TrainSignUpPeopleImportService importService,
      ClassPrepRosterImportService classPrepRosterImportService,
      PlatformUserApplicationService platformUserApplicationService) {
    this.trainSignUpPeopleSelfApplicationService = trainSignUpPeopleSelfApplicationService;
    this.collectionService = collectionService;
    this.importService = importService;
    this.classPrepRosterImportService = classPrepRosterImportService;
    this.platformUserApplicationService = platformUserApplicationService;
  }

  @GetMapping("/sign-up-people")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<PageResponse<TrainSignUpPeopleSelfVO>> page(@Valid @ModelAttribute TrainSignUpPeopleSelfQueryReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.page(req));
  }

  @GetMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<TrainSignUpPeopleSelfVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.getById(id));
  }

  @PostMapping("/sign-up-people")
  @PreAuthorize("hasAuthority('train:sign-up-people:create')")
  @AuditLog(module = "training", action = "sign-up-people:create", targetType = "sign-up-people")
  public ApiResponse<TrainSignUpPeopleSelfVO> create(@Valid @RequestBody TrainSignUpPeopleSelfCreateReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.create(req));
  }

  @PutMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:update')")
  @AuditLog(module = "training", action = "sign-up-people:update", targetType = "sign-up-people")
  public ApiResponse<TrainSignUpPeopleSelfVO> update(@PathVariable Long id, @Valid @RequestBody TrainSignUpPeopleSelfUpdateReq req) {
    return ApiResponse.ok(trainSignUpPeopleSelfApplicationService.update(id, req));
  }

  @DeleteMapping("/sign-up-people/{id}")
  @PreAuthorize("hasAuthority('train:sign-up-people:delete')")
  @AuditLog(module = "training", action = "sign-up-people:delete", targetType = "sign-up-people")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainSignUpPeopleSelfApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 批量审核/删除（对齐 peopleselfoptmoreAction） */
  @PutMapping("/sign-up-people/batch")
  @PreAuthorize("hasAuthority('train:sign-up-people:update')")
  @AuditLog(module = "training", action = "sign-up-people:batch", targetType = "sign-up-people")
  public ApiResponse<Integer> batchUpdate(@Valid @RequestBody SignUpPeopleBatchReq req) {
    return ApiResponse.ok(collectionService.batchUpdatePeople(req.getIds(), req.getField(), req.getValue()));
  }

  /** Excel 批量导入（对齐 peopleselfimportAction） */
  @PostMapping("/sign-up-people/import")
  @PreAuthorize("hasAuthority('train:sign-up-people:create')")
  @AuditLog(module = "training", action = "sign-up-people:import", targetType = "sign-up-people")
  public ApiResponse<SignUpPeopleImportResultVO> importExcel(
      @RequestParam Long signUpId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(importService.importExcel(signUpId, file));
  }

  /** 开班准备资料收集 · 学徒制花名册导入 */
  @PostMapping("/sign-up-people/import-class-roster")
  @PreAuthorize("hasAuthority('train:sign-up-people:create')")
  @AuditLog(module = "training", action = "sign-up-people:import-class-roster", targetType = "sign-up-people")
  public ApiResponse<SignUpPeopleImportResultVO> importClassRoster(
      @RequestParam Long signUpId,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(classPrepRosterImportService.importExcel(signUpId, file));
  }

  /** 下载 Excel 批量导入模板 */
  @GetMapping("/sign-up-people/import/template")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ResponseEntity<InputStreamResource> downloadImportTemplate() throws IOException {
    return importService.downloadImportTemplate();
  }

  /** 从学员库导入（对齐 studentimportAction） */
  @PostMapping("/sign-up-people/import/students")
  @PreAuthorize("hasAuthority('train:sign-up-people:create')")
  @AuditLog(module = "training", action = "sign-up-people:import-students", targetType = "sign-up-people")
  public ApiResponse<SignUpPeopleImportResultVO> importStudents(@Valid @RequestBody SignUpStudentImportReq req) {
    return ApiResponse.ok(importService.importFromStudents(req.getSignUpId(), req.getIds()));
  }

  /** 学员库候选列表（对齐 studentlistAction，供从学员导入弹窗使用） */
  @GetMapping("/sign-up-people/import/students")
  @PreAuthorize("hasAuthority('train:sign-up-people:view')")
  public ApiResponse<PageResponse<PlatformUserVO>> listImportCandidates(
      @Valid @ModelAttribute PlatformUserQueryReq req) {
    return ApiResponse.ok(platformUserApplicationService.page(req));
  }
}
