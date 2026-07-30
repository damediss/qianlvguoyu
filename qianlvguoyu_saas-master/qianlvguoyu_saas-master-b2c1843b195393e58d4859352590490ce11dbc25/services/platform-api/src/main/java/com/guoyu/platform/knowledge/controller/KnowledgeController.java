package com.guoyu.platform.knowledge.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.knowledge.dto.KnowledgeAnswerDraftCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeDocumentCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeDocumentQueryReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSearchReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceQueryReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceUpdateReq;
import com.guoyu.platform.knowledge.service.KnowledgeApplicationService;
import com.guoyu.platform.knowledge.vo.KnowledgeAnswerDraftVO;
import com.guoyu.platform.knowledge.vo.KnowledgeCitationVO;
import com.guoyu.platform.knowledge.vo.KnowledgeDocumentVO;
import com.guoyu.platform.knowledge.vo.KnowledgeIndexTaskVO;
import com.guoyu.platform.knowledge.vo.KnowledgeSearchResultVO;
import com.guoyu.platform.knowledge.vo.KnowledgeSpaceVO;
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
@RequestMapping("/knowledge")
@Slf4j
public class KnowledgeController {

  private final KnowledgeApplicationService knowledgeApplicationService;

  public KnowledgeController(KnowledgeApplicationService knowledgeApplicationService) {
    this.knowledgeApplicationService = knowledgeApplicationService;
  }

  @GetMapping("/spaces")
  @PreAuthorize("hasAuthority('knowledge:view')")
  public ApiResponse<PageResponse<KnowledgeSpaceVO>> spaces(@Valid @ModelAttribute KnowledgeSpaceQueryReq req) {
    return ApiResponse.ok(knowledgeApplicationService.pageSpaces(req));
  }

  @PostMapping("/spaces")
  @PreAuthorize("hasAuthority('knowledge:create')")
  @AuditLog(module = "knowledge", action = "knowledge:create", targetType = "knowledge_space")
  public ApiResponse<KnowledgeSpaceVO> createSpace(@Valid @RequestBody KnowledgeSpaceCreateReq req) {
    return ApiResponse.ok(knowledgeApplicationService.createSpace(req));
  }

  @GetMapping("/spaces/{id}")
  @PreAuthorize("hasAuthority('knowledge:view')")
  public ApiResponse<KnowledgeSpaceVO> getSpace(@PathVariable Long id) {
    return ApiResponse.ok(knowledgeApplicationService.getSpace(id));
  }

  @PutMapping("/spaces/{id}")
  @PreAuthorize("hasAuthority('knowledge:update')")
  @AuditLog(module = "knowledge", action = "knowledge:update", targetType = "knowledge_space")
  public ApiResponse<KnowledgeSpaceVO> updateSpace(
      @PathVariable Long id,
      @Valid @RequestBody KnowledgeSpaceUpdateReq req
  ) {
    return ApiResponse.ok(knowledgeApplicationService.updateSpace(id, req));
  }

  @DeleteMapping("/spaces/{id}")
  @PreAuthorize("hasAuthority('knowledge:delete')")
  @AuditLog(module = "knowledge", action = "knowledge:delete", targetType = "knowledge_space")
  public ApiResponse<Void> deleteSpace(@PathVariable Long id) {
    knowledgeApplicationService.deleteSpace(id);
    return ApiResponse.ok();
  }

  @GetMapping("/spaces/{spaceId}/documents")
  @PreAuthorize("hasAuthority('knowledge:view')")
  public ApiResponse<PageResponse<KnowledgeDocumentVO>> documents(
      @PathVariable Long spaceId,
      @Valid @ModelAttribute KnowledgeDocumentQueryReq req
  ) {
    return ApiResponse.ok(knowledgeApplicationService.pageDocuments(spaceId, req));
  }

  @PostMapping("/spaces/{spaceId}/documents")
  @PreAuthorize("hasAuthority('knowledge:document')")
  @AuditLog(module = "knowledge", action = "knowledge-document:ingest", targetType = "knowledge_document")
  public ApiResponse<KnowledgeDocumentVO> ingestDocument(
      @PathVariable Long spaceId,
      @Valid @RequestBody KnowledgeDocumentCreateReq req
  ) {
    return ApiResponse.ok(knowledgeApplicationService.ingestDocument(spaceId, req));
  }

  @GetMapping("/index-tasks/{id}")
  @PreAuthorize("hasAuthority('knowledge:view')")
  public ApiResponse<KnowledgeIndexTaskVO> indexTask(@PathVariable Long id) {
    return ApiResponse.ok(knowledgeApplicationService.getIndexTask(id));
  }

  @PostMapping("/search")
  @PreAuthorize("hasAuthority('knowledge:search')")
  @AuditLog(module = "knowledge", action = "knowledge:search", targetType = "knowledge_search")
  public ApiResponse<List<KnowledgeSearchResultVO>> search(@Valid @RequestBody KnowledgeSearchReq req) {
    return ApiResponse.ok(knowledgeApplicationService.search(req));
  }

  @PostMapping("/answer-drafts")
  @PreAuthorize("hasAuthority('knowledge:answer')")
  @AuditLog(module = "knowledge", action = "knowledge-answer-draft:create", targetType = "ai_generation_draft")
  public ApiResponse<KnowledgeAnswerDraftVO> answerDraft(@Valid @RequestBody KnowledgeAnswerDraftCreateReq req) {
    return ApiResponse.ok(knowledgeApplicationService.createAnswerDraft(req));
  }

  @GetMapping("/chunks/{id}/citation")
  @PreAuthorize("hasAuthority('knowledge:view')")
  public ApiResponse<KnowledgeCitationVO> citation(@PathVariable Long id) {
    return ApiResponse.ok(knowledgeApplicationService.getCitation(id));
  }
}
