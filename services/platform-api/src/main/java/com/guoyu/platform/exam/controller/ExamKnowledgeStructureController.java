package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureCreateReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureQueryReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeStructureUpdateReq;
import com.guoyu.platform.exam.service.ExamKnowledgeStructureApplicationService;
import com.guoyu.platform.exam.vo.ExamKnowledgeStructureVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamKnowledgeStructureController {

  private final ExamKnowledgeStructureApplicationService service;

  public ExamKnowledgeStructureController(ExamKnowledgeStructureApplicationService service) {
    this.service = service;
  }

  @GetMapping("/knowledge-structures")
  @PreAuthorize("hasAuthority('exam:knowledge:view')")
  public ApiResponse<PageResponse<ExamKnowledgeStructureVO>> page(
      @Valid @ModelAttribute ExamKnowledgeStructureQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/knowledge-structures/tree")
  @PreAuthorize("hasAuthority('exam:knowledge:view')")
  public ApiResponse<List<ExamKnowledgeStructureVO>> tree(
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer type) {
    return ApiResponse.ok(service.tree(courseId, type));
  }

  @GetMapping("/knowledge-structures/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge:view')")
  public ApiResponse<ExamKnowledgeStructureVO> get(@PathVariable Integer id) {
    return ApiResponse.ok(service.getById(id));
  }

  @PostMapping("/knowledge-structures")
  @PreAuthorize("hasAuthority('exam:knowledge:create')")
  @AuditLog(module = "exam", action = "knowledge:create", targetType = "exam_knowledge_structure")
  public ApiResponse<ExamKnowledgeStructureVO> create(
      @Valid @RequestBody ExamKnowledgeStructureCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/knowledge-structures/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge:update')")
  @AuditLog(module = "exam", action = "knowledge:update", targetType = "exam_knowledge_structure")
  public ApiResponse<ExamKnowledgeStructureVO> update(
      @PathVariable Integer id,
      @Valid @RequestBody ExamKnowledgeStructureUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/knowledge-structures/{id}")
  @PreAuthorize("hasAuthority('exam:knowledge:delete')")
  @AuditLog(module = "exam", action = "knowledge:delete", targetType = "exam_knowledge_structure")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/knowledge-structures/batch")
  @PreAuthorize("hasAuthority('exam:knowledge:import')")
  @AuditLog(module = "exam", action = "knowledge:batch-import", targetType = "exam_knowledge_structure")
  public ApiResponse<Integer> batchImport(
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer type,
      @RequestBody List<String> lines) {
    return ApiResponse.ok(service.batchImport(courseId, type, lines));
  }
}
