package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainFileListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainFileListVO;
import jakarta.validation.Valid;
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
 * 文件列表管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainFileListController {

  private final TrainFileListApplicationService trainFileListApplicationService;

  public TrainFileListController(TrainFileListApplicationService trainFileListApplicationService) {
    this.trainFileListApplicationService = trainFileListApplicationService;
  }

  /** 分页查询文件列表 */
  @GetMapping("/file-lists")
  @PreAuthorize("hasAuthority('train:file-list:view')")
  public ApiResponse<PageResponse<TrainFileListVO>> page(@Valid @ModelAttribute TrainFileListQueryReq req) {
    return ApiResponse.ok(trainFileListApplicationService.page(req));
  }

  /** 根据ID查询文件详情 */
  @GetMapping("/file-lists/{id}")
  @PreAuthorize("hasAuthority('train:file-list:view')")
  public ApiResponse<TrainFileListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainFileListApplicationService.getById(id));
  }

  /** 新增文件 */
  @PostMapping("/file-lists")
  @PreAuthorize("hasAuthority('train:file-list:create')")
  @AuditLog(module = "training", action = "file-list:create", targetType = "train_file_list")
  public ApiResponse<TrainFileListVO> create(@Valid @RequestBody TrainFileListCreateReq req) {
    return ApiResponse.ok(trainFileListApplicationService.create(req));
  }

  /** 修改文件 */
  @PutMapping("/file-lists/{id}")
  @PreAuthorize("hasAuthority('train:file-list:update')")
  @AuditLog(module = "training", action = "file-list:update", targetType = "train_file_list")
  public ApiResponse<TrainFileListVO> update(@PathVariable Long id, @Valid @RequestBody TrainFileListUpdateReq req) {
    return ApiResponse.ok(trainFileListApplicationService.update(id, req));
  }

  /** 删除文件 */
  @DeleteMapping("/file-lists/{id}")
  @PreAuthorize("hasAuthority('train:file-list:delete')")
  @AuditLog(module = "training", action = "file-list:delete", targetType = "train_file_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainFileListApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
