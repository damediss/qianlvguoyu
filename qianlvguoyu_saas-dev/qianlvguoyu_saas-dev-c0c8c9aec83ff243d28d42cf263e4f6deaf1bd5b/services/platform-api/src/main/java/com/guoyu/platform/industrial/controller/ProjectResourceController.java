package com.guoyu.platform.industrial.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.industrial.dto.ProjectResourceQueryReq;
import com.guoyu.platform.industrial.service.ProjectResourceApplicationService;
import com.guoyu.platform.industrial.vo.ProjectResourceVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/industrial")
public class ProjectResourceController {

  private final ProjectResourceApplicationService service;

  public ProjectResourceController(ProjectResourceApplicationService service) {
    this.service = service;
  }

  @GetMapping("/projectcourse")
  @PreAuthorize("hasAuthority('industrial:project-course:view')")
  public ApiResponse<PageResponse<ProjectResourceVO>> projectCourse(@Valid @ModelAttribute ProjectResourceQueryReq req) {
    return ApiResponse.ok(service.page("course", req));
  }

  @GetMapping("/projectteach")
  @PreAuthorize("hasAuthority('industrial:project-teach:view')")
  public ApiResponse<PageResponse<ProjectResourceVO>> projectTeach(@Valid @ModelAttribute ProjectResourceQueryReq req) {
    return ApiResponse.ok(service.page("teach", req));
  }

  @GetMapping("/projectexam")
  @PreAuthorize("hasAuthority('industrial:project-exam:view')")
  public ApiResponse<PageResponse<ProjectResourceVO>> projectExam(@Valid @ModelAttribute ProjectResourceQueryReq req) {
    return ApiResponse.ok(service.page("exam", req));
  }

  @GetMapping("/projecttrain")
  @PreAuthorize("hasAuthority('industrial:project-train:view')")
  public ApiResponse<PageResponse<ProjectResourceVO>> projectTrain(@Valid @ModelAttribute ProjectResourceQueryReq req) {
    return ApiResponse.ok(service.page("train", req));
  }

  @GetMapping("/projectcriterion")
  @PreAuthorize("hasAuthority('industrial:project-criterion:view')")
  public ApiResponse<PageResponse<ProjectResourceVO>> projectCriterion(@Valid @ModelAttribute ProjectResourceQueryReq req) {
    return ApiResponse.ok(service.page("criterion", req));
  }
}
