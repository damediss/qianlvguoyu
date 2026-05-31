package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.PostCreateReq;
import com.guoyu.platform.system.dto.PostQueryReq;
import com.guoyu.platform.system.dto.PostUpdateReq;
import com.guoyu.platform.system.service.PostApplicationService;
import com.guoyu.platform.system.vo.PostVO;
import jakarta.validation.Valid;
import java.util.List;
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

@Validated
@RestController
@RequestMapping("/system/posts")
@Slf4j
public class PostController {

  private final PostApplicationService postApplicationService;

  public PostController(PostApplicationService postApplicationService) {
    this.postApplicationService = postApplicationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('post:view')")
  public ApiResponse<PageResponse<PostVO>> page(@Valid @ModelAttribute PostQueryReq req) {
    return ApiResponse.ok(postApplicationService.page(req));
  }

  @GetMapping("/all")
  @PreAuthorize("hasAuthority('post:view')")
  public ApiResponse<List<PostVO>> listAll() {
    return ApiResponse.ok(postApplicationService.listAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('post:view')")
  public ApiResponse<PostVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(postApplicationService.detail(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('post:create')")
  @AuditLog(module = "system", action = "post:create", targetType = "post")
  public ApiResponse<PostVO> create(@Valid @RequestBody PostCreateReq req) {
    return ApiResponse.ok(postApplicationService.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('post:update')")
  @AuditLog(module = "system", action = "post:update", targetType = "post")
  public ApiResponse<PostVO> update(@PathVariable Long id, @Valid @RequestBody PostUpdateReq req) {
    return ApiResponse.ok(postApplicationService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('post:delete')")
  @AuditLog(module = "system", action = "post:delete", targetType = "post")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    postApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
