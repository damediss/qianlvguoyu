package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempSaveReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempUpdateReq;
import java.util.List;
import java.util.Map;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainFileTempApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainFileTempVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training")
public class TrainFileTempController {

  private final TrainFileTempApplicationService trainFileTempApplicationService;

  public TrainFileTempController(TrainFileTempApplicationService trainFileTempApplicationService) {
    this.trainFileTempApplicationService = trainFileTempApplicationService;
  }

  @GetMapping("/file-temps")
  @PreAuthorize("hasAuthority('train:file-temp:view')")
  public ApiResponse<PageResponse<TrainFileTempVO>> page(@Valid @ModelAttribute TrainFileTempQueryReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.page(req));
  }

  /** 材料模板列表（对齐 PHP file-temp-list，返回 list） */
  @GetMapping("/file-temps/list")
  @PreAuthorize(TrainingAuthorityExpressions.FILE_TEMP_LEGACY_VIEW)
  public ApiResponse<Map<String, Object>> listForLegacy(@Valid @ModelAttribute TrainFileTempQueryReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.listForLegacy(req));
  }

  /** 保存材料模板（对齐 PHP file-temp-save） */
  @PostMapping("/file-temps/save")
  @PreAuthorize(TrainingAuthorityExpressions.FILE_TEMP_LEGACY_SAVE)
  @AuditLog(module = "training", action = "file-temp:save", targetType = "train_file_temp")
  public ApiResponse<Map<String, Object>> saveLegacy(@Valid @RequestBody TrainFileTempSaveReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.saveLegacy(req));
  }

  /** 批量删除（对齐 PHP file-temp-del） */
  @PostMapping("/file-temps/batch-delete")
  @PreAuthorize(TrainingAuthorityExpressions.FILE_TEMP_LEGACY_DELETE)
  @AuditLog(module = "training", action = "file-temp:batch-delete", targetType = "train_file_temp")
  public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainFileTempApplicationService.batchDelete(body.get("ids")));
  }

  @GetMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:view')")
  public ApiResponse<TrainFileTempVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainFileTempApplicationService.getById(id));
  }

  @PostMapping("/file-temps")
  @PreAuthorize("hasAuthority('train:file-temp:create')")
  @AuditLog(module = "training", action = "file-temp:create", targetType = "file-temps")
  public ApiResponse<TrainFileTempVO> create(@Valid @RequestBody TrainFileTempCreateReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.create(req));
  }

  @PutMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:update')")
  @AuditLog(module = "training", action = "file-temp:update", targetType = "file-temps")
  public ApiResponse<TrainFileTempVO> update(@PathVariable Long id, @Valid @RequestBody TrainFileTempUpdateReq req) {
    return ApiResponse.ok(trainFileTempApplicationService.update(id, req));
  }

  @DeleteMapping("/file-temps/{id}")
  @PreAuthorize("hasAuthority('train:file-temp:delete')")
  @AuditLog(module = "training", action = "file-temp:delete", targetType = "file-temps")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainFileTempApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
