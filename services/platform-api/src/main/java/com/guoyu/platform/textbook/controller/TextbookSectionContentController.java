package com.guoyu.platform.textbook.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.textbook.dto.TextbookSectionRestoreReq;
import com.guoyu.platform.textbook.dto.TextbookSectionReviewReq;
import com.guoyu.platform.textbook.dto.TextbookSectionSaveReq;
import com.guoyu.platform.textbook.dto.TextbookSectionSubmitReq;
import com.guoyu.platform.textbook.service.TextbookReviewApplicationService;
import com.guoyu.platform.textbook.service.TextbookSectionContentApplicationService;
import com.guoyu.platform.textbook.vo.TextbookReviewRecordVO;
import com.guoyu.platform.textbook.vo.TextbookSectionContentVO;
import com.guoyu.platform.textbook.vo.TextbookVersionDetailVO;
import com.guoyu.platform.textbook.vo.TextbookVersionVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 章节工作台与正文（方案 §6.3 W2-W9）。
 *
 * <p>W3 自动/手动保存 includeArgs=false——不得把整篇 contentMd 灌入审计详情（方案 §6
 * 审计约定）；操作者/时间/方法/targetId 元数据仍完整记录。
 */
@RestController
@RequestMapping("/textbook")
public class TextbookSectionContentController {

  private final TextbookSectionContentApplicationService contentService;
  private final TextbookReviewApplicationService reviewService;

  public TextbookSectionContentController(
      TextbookSectionContentApplicationService contentService,
      TextbookReviewApplicationService reviewService) {
    this.contentService = contentService;
    this.reviewService = reviewService;
  }

  /** W2 当前正文/检查/版本/图片/最近审定记录（空正文 200 contentExists=false）。 */
  @GetMapping("/sections/{sectionId}/content")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ApiResponse<TextbookSectionContentVO> content(@PathVariable Long sectionId) {
    return ApiResponse.ok(contentService.getContent(sectionId));
  }

  /** W3 自动/手动保存（baseVersion 乐观锁；返回最新 W2）。 */
  @PutMapping("/sections/{sectionId}/content")
  @PreAuthorize("hasAuthority('textbook:content:write')")
  @AuditLog(module = "textbook", action = "section:save",
      targetType = "textbook_section_content", includeArgs = false)
  public ApiResponse<TextbookSectionContentVO> save(@PathVariable Long sectionId,
      @Valid @RequestBody TextbookSectionSaveReq req) {
    return ApiResponse.ok(contentService.save(sectionId, req));
  }

  /** W4 提交审定（生成 submit 快照与 submit 记录）。 */
  @PostMapping("/sections/{sectionId}/submit")
  @PreAuthorize("hasAuthority('textbook:content:write')")
  @AuditLog(module = "textbook", action = "section:submit",
      targetType = "textbook_section_content")
  public ApiResponse<TextbookSectionContentVO> submit(@PathVariable Long sectionId,
      @Valid @RequestBody TextbookSectionSubmitReq req) {
    return ApiResponse.ok(contentService.submit(sectionId, req.baseVersion()));
  }

  /** W5 通过/打回/撤销通过（当前审定人；返回最新 W2）。 */
  @PostMapping("/sections/{sectionId}/review")
  @PreAuthorize("hasAuthority('textbook:content:review')")
  @AuditLog(module = "textbook", action = "section:review",
      targetType = "textbook_section_content")
  public ApiResponse<TextbookSectionContentVO> review(@PathVariable Long sectionId,
      @Valid @RequestBody TextbookSectionReviewReq req) {
    return ApiResponse.ok(reviewService.review(sectionId, req));
  }

  /** W6 审定时间线分页（createdAt/id 稳定倒序）。 */
  @GetMapping("/sections/{sectionId}/reviews")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ApiResponse<PageResponse<TextbookReviewRecordVO>> reviews(@PathVariable Long sectionId,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    return ApiResponse.ok(contentService.reviews(sectionId, page, pageSize));
  }

  /** W7 快照分页（snapshotNo 倒序，不含大正文）。 */
  @GetMapping("/sections/{sectionId}/versions")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ApiResponse<PageResponse<TextbookVersionVO>> versions(@PathVariable Long sectionId,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long pageSize) {
    return ApiResponse.ok(contentService.versions(sectionId, page, pageSize));
  }

  /** W8 单个快照完整内容。 */
  @GetMapping("/section-versions/{versionId}")
  @PreAuthorize("hasAuthority('textbook:project:view') or hasAuthority('textbook:task:view') "
      + "or hasAuthority('textbook:review:view')")
  public ApiResponse<TextbookVersionDetailVO> versionDetail(@PathVariable Long versionId) {
    return ApiResponse.ok(contentService.versionDetail(versionId));
  }

  /** W9 恢复历史版本（保持 draft/returned 原状态；先存 before_restore）。 */
  @PostMapping("/sections/{sectionId}/restore")
  @PreAuthorize("hasAuthority('textbook:content:write')")
  @AuditLog(module = "textbook", action = "section:restore",
      targetType = "textbook_section_content")
  public ApiResponse<TextbookSectionContentVO> restore(@PathVariable Long sectionId,
      @Valid @RequestBody TextbookSectionRestoreReq req) {
    return ApiResponse.ok(contentService.restore(sectionId, req));
  }
}
