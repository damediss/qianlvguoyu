package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentUpdateReq;
import com.guoyu.platform.trainingdepartment.service.TrainUserPatentService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPatentVO;
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
 * 用户专利管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainUserPatentController {

  private final TrainUserPatentService service;

  public TrainUserPatentController(TrainUserPatentService service) {
    this.service = service;
  }

  /** 分页查询用户专利列表 */
  @GetMapping("/user-patents")
  @PreAuthorize("hasAuthority('train:user-patent:view')")
  public ApiResponse<PageResponse<TrainUserPatentVO>> page(@Valid @ModelAttribute TrainUserPatentQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询用户专利详情 */
  @GetMapping("/user-patents/{id}")
  @PreAuthorize("hasAuthority('train:user-patent:view')")
  public ApiResponse<TrainUserPatentVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增用户专利 */
  @PostMapping("/user-patents")
  @PreAuthorize("hasAuthority('train:user-patent:create')")
  @AuditLog(module = "training", action = "user-patent:create", targetType = "train_user_patent")
  public ApiResponse<TrainUserPatentVO> create(@Valid @RequestBody TrainUserPatentCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改用户专利 */
  @PutMapping("/user-patents/{id}")
  @PreAuthorize("hasAuthority('train:user-patent:update')")
  @AuditLog(module = "training", action = "user-patent:update", targetType = "train_user_patent")
  public ApiResponse<TrainUserPatentVO> update(@PathVariable Long id, @Valid @RequestBody TrainUserPatentUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除用户专利 */
  @DeleteMapping("/user-patents/{id}")
  @PreAuthorize("hasAuthority('train:user-patent:delete')")
  @AuditLog(module = "training", action = "user-patent:delete", targetType = "train_user_patent")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);

    return ApiResponse.ok();
  }
}
