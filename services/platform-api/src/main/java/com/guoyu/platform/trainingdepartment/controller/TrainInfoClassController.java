package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainInfoClassApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainMaterialGenerateApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训班级管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainInfoClassController {

  private final TrainInfoClassApplicationService trainInfoClassApplicationService;
  private final TrainMaterialGenerateApplicationService trainMaterialGenerateApplicationService;

  public TrainInfoClassController(TrainInfoClassApplicationService trainInfoClassApplicationService,
      TrainMaterialGenerateApplicationService trainMaterialGenerateApplicationService) {
    this.trainInfoClassApplicationService = trainInfoClassApplicationService;
    this.trainMaterialGenerateApplicationService = trainMaterialGenerateApplicationService;
  }

  /** 分页查询培训班级列表（含企业名、工种名、培训标题等关联字段） */
  @GetMapping("/info-classes")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<Map<String, Object>> page(@Valid @ModelAttribute TrainInfoClassQueryReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.page(req));
  }

  /** 根据ID查询培训班级详情 */
  @GetMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<TrainInfoClassVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainInfoClassApplicationService.getById(id));
  }

  /** 新增/编辑培训班级（id=0新增, id>0编辑） */
  @PostMapping("/info-classes/save")
  @PreAuthorize("hasAuthority('train:info-class:create')")
  @AuditLog(module = "training", action = "info-class:save", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> save(@Valid @RequestBody TrainInfoClassCreateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.save(req));
  }

  /** 批量删除培训班级 */
  @PostMapping("/info-classes/batch-delete")
  @PreAuthorize("hasAuthority('train:info-class:delete')")
  @AuditLog(module = "training", action = "info-class:batch-delete", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainInfoClassApplicationService.batchDelete(body.get("ids")));
  }

  /** 新增培训班级 */
  @PostMapping("/info-classes")
  @PreAuthorize("hasAuthority('train:info-class:create')")
  @AuditLog(module = "training", action = "info-class:create", targetType = "train_info_class")
  public ApiResponse<TrainInfoClassVO> create(@Valid @RequestBody TrainInfoClassCreateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.create(req));
  }

  /** 修改培训班级 */
  @PutMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:update", targetType = "train_info_class")
  public ApiResponse<TrainInfoClassVO> update(@PathVariable Long id, @Valid @RequestBody TrainInfoClassUpdateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.update(id, req));
  }

  /** 删除培训班级 */
  @DeleteMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:delete')")
  @AuditLog(module = "training", action = "info-class:delete", targetType = "train_info_class")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainInfoClassApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 挂载文件模板到班级 */
  @PostMapping("/info-classes/{classId}/file-temps")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:attach-file-temps", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> attachFileTemps(@PathVariable Long classId, @RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainInfoClassApplicationService.attachFileTemps(classId, body.get("ids")));
  }

  /** 移除班级文件模板 */
  @DeleteMapping("/info-classes/{classId}/file-temps/{templateId}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:detach-file-temp", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> detachFileTemp(@PathVariable Long classId, @PathVariable Long templateId) {
    return ApiResponse.ok(trainInfoClassApplicationService.detachFileTemp(classId, templateId));
  }

  /** 生成班级申报材料 */
  @PostMapping("/info-classes/{classId}/materials/generate")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:generate-materials", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> generateMaterials(@PathVariable Long classId) {
    return ApiResponse.ok(trainMaterialGenerateApplicationService.generate(classId));
  }
}
