package com.guoyu.platform.system.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.DictCreateReq;
import com.guoyu.platform.system.dto.DictItemCreateReq;
import com.guoyu.platform.system.dto.DictItemUpdateReq;
import com.guoyu.platform.system.dto.DictQueryReq;
import com.guoyu.platform.system.dto.DictUpdateReq;
import com.guoyu.platform.system.service.DictApplicationService;
import com.guoyu.platform.system.vo.DictItemVO;
import com.guoyu.platform.system.vo.DictVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/system")
public class DictController {

  private final DictApplicationService dictApplicationService;

  public DictController(DictApplicationService dictApplicationService) {
    this.dictApplicationService = dictApplicationService;
  }

  @GetMapping("/dicts")
  @PreAuthorize("hasAuthority('dict:view')")
  public ApiResponse<PageResponse<DictVO>> page(@Valid @ModelAttribute DictQueryReq req) {
    return ApiResponse.ok(dictApplicationService.page(req));
  }

  @PostMapping("/dicts")
  @PreAuthorize("hasAuthority('dict:create')")
  @AuditLog(module = "system", action = "dict:create", targetType = "dict")
  public ApiResponse<DictVO> create(@Valid @RequestBody DictCreateReq req) {
    return ApiResponse.ok(dictApplicationService.create(req));
  }

  @PutMapping("/dicts/{id}")
  @PreAuthorize("hasAuthority('dict:update')")
  @AuditLog(module = "system", action = "dict:update", targetType = "dict")
  public ApiResponse<DictVO> update(@PathVariable Long id, @Valid @RequestBody DictUpdateReq req) {
    return ApiResponse.ok(dictApplicationService.update(id, req));
  }

  @GetMapping("/dicts/{code}/items")
  public ApiResponse<List<DictItemVO>> items(@PathVariable String code) {
    return ApiResponse.ok(dictApplicationService.itemsByCode(code));
  }

  @PostMapping("/dicts/{id}/items")
  @PreAuthorize("hasAuthority('dict:update')")
  @AuditLog(module = "system", action = "dict-item:create", targetType = "dict")
  public ApiResponse<DictItemVO> createItem(@PathVariable Long id, @Valid @RequestBody DictItemCreateReq req) {
    return ApiResponse.ok(dictApplicationService.createItem(id, req));
  }

  @PutMapping("/dict-items/{itemId}")
  @PreAuthorize("hasAuthority('dict:update')")
  @AuditLog(module = "system", action = "dict-item:update", targetType = "dict_item")
  public ApiResponse<DictItemVO> updateItem(@PathVariable Long itemId, @Valid @RequestBody DictItemUpdateReq req) {
    return ApiResponse.ok(dictApplicationService.updateItem(itemId, req));
  }
}
