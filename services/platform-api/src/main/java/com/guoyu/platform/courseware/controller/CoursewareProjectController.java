package com.guoyu.platform.courseware.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.service.CoursewareGenerationService;
import com.guoyu.platform.courseware.service.CoursewareProjectService;
import com.guoyu.platform.courseware.service.CoursewareSlideQueryService;
import com.guoyu.platform.courseware.vo.CoursewareVOs.ProgressVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.ProjectVO;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 课件项目(创建/列表/进度/取消/删除) */
@Slf4j
@RestController
@RequestMapping("/courseware/projects")
public class CoursewareProjectController {

  private final CoursewareProjectService projectService;
  private final CoursewareGenerationService generationService;
  private final CoursewareSlideQueryService slideQueryService;

  public CoursewareProjectController(CoursewareProjectService projectService,
                                     CoursewareGenerationService generationService,
                                     CoursewareSlideQueryService slideQueryService) {
    this.projectService = projectService;
    this.generationService = generationService;
    this.slideQueryService = slideQueryService;
  }

  public record CreateProjectReq(Long sourceFileId, String title, String audience,
                                 String contentDensity) {
  }

  @PostMapping
  @PreAuthorize("hasAuthority('courseware:project:create')")
  @AuditLog(module = "courseware", action = "project:create", targetType = "cw_project")
  public ApiResponse<ProjectVO> create(@RequestBody CreateProjectReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    CwProjectEntity project = projectService.createProject(tenantId, userId, req.sourceFileId(),
        req.title(), req.audience(), req.contentDensity());
    return ApiResponse.ok(toVO(project));
  }

  @GetMapping("/page")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<PageResponse<ProjectVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    PageResponse<CwProjectEntity> result = projectService.pageProjects(tenantId, page, pageSize);
    return ApiResponse.ok(new PageResponse<>(result.items().stream().map(this::toVO).toList(),
        result.page(), result.pageSize(), result.total()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<ProjectVO> get(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(toVO(projectService.getProject(tenantId, id)));
  }

  /** 生成进度轮询(项目状态 + 每页状态;方案 §11 三段式第③步) */
  @GetMapping("/{id}/progress")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<ProgressVO> progress(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(slideQueryService.progress(tenantId, id));
  }

  /** 软取消生成(只挡新派发,正在生成的页跑完) */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('courseware:project:generate')")
  @AuditLog(module = "courseware", action = "project:cancel", targetType = "cw_project")
  public ApiResponse<Void> cancel(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    generationService.cancel(tenantId, id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('courseware:project:create')")
  @AuditLog(module = "courseware", action = "project:delete", targetType = "cw_project")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    projectService.deleteProject(tenantId, id);
    return ApiResponse.ok();
  }

  private ProjectVO toVO(CwProjectEntity entity) {
    return new ProjectVO(entity.getId(), entity.getTitle(), entity.getAudience(),
        entity.getContentDensity(), entity.getStatus(), entity.getTotalPages(),
        entity.getReadyPages(), entity.getFailedPages(), entity.getError(), entity.getCreatedAt());
  }
}
