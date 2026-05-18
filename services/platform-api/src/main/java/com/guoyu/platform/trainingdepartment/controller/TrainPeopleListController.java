package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainPeopleListService;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;
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
 * 开班人员列表管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainPeopleListController {

  private final TrainPeopleListService service;

  public TrainPeopleListController(TrainPeopleListService service) {
    this.service = service;
  }

  /** 分页查询开班人员列表 */
  @GetMapping("/people-lists")
  @PreAuthorize("hasAuthority('train:people-list:view')")
  public ApiResponse<PageResponse<TrainPeopleListVO>> page(@Valid @ModelAttribute TrainPeopleListQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询开班人员详情 */
  @GetMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:view')")
  public ApiResponse<TrainPeopleListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增开班人员 */
  @PostMapping("/people-lists")
  @PreAuthorize("hasAuthority('train:people-list:create')")
  @AuditLog(module = "training", action = "people-list:create", targetType = "train_people_list")
  public ApiResponse<TrainPeopleListVO> create(@Valid @RequestBody TrainPeopleListCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改开班人员 */
  @PutMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:update')")
  @AuditLog(module = "training", action = "people-list:update", targetType = "train_people_list")
  public ApiResponse<TrainPeopleListVO> update(@PathVariable Long id, @Valid @RequestBody TrainPeopleListUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除开班人员 */
  @DeleteMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:delete')")
  @AuditLog(module = "training", action = "people-list:delete", targetType = "train_people_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  /** 批量审核报名 */
  @PutMapping("/people-lists/batch-audit")
  @PreAuthorize("hasAuthority('train:people-list:audit')")
  @AuditLog(module = "training", action = "people-list:batch-audit", targetType = "train_people_list")
  public ApiResponse<Void> batchAudit(@Valid @RequestBody SignupAuditReq req) {
    service.batchAudit(req);
    return ApiResponse.ok();
  }
}
