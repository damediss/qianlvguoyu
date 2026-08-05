package com.guoyu.platform.competition.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.exam.dto.CompetitionExamAnswerSaveReq;
import com.guoyu.platform.competition.exam.dto.CompetitionExamLoginReq;
import com.guoyu.platform.competition.exam.dto.CompetitionExamSubmitReq;
import com.guoyu.platform.competition.exam.service.CompetitionExamApplicationService;
import com.guoyu.platform.competition.exam.vo.CompetitionExamAnswerVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamAttemptVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamLoginVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamQuestionVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamSubmissionVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamWaitingVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/exam")
public class CompetitionExamController {
  private final CompetitionExamApplicationService service;

  public CompetitionExamController(CompetitionExamApplicationService service) {
    this.service = service;
  }

  @PostMapping("/auth/login")
  public ApiResponse<CompetitionExamLoginVO> login(
      @Valid @RequestBody CompetitionExamLoginReq request) {
    return ApiResponse.ok(service.login(request));
  }

  @PostMapping("/auth/logout")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<Void> logout() {
    service.logout();
    return ApiResponse.ok();
  }

  @GetMapping("/waiting")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamWaitingVO> waiting() {
    return ApiResponse.ok(service.waiting());
  }

  @PostMapping("/attempt/start")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamAttemptVO> start() {
    return ApiResponse.ok(service.startOrResume());
  }

  @GetMapping("/attempt")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamAttemptVO> current() {
    return ApiResponse.ok(service.current());
  }

  @GetMapping("/questions")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<List<CompetitionExamQuestionVO>> questions() {
    return ApiResponse.ok(service.questions());
  }

  @PutMapping("/answers")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamAnswerVO> saveAnswer(
      @Valid @RequestBody CompetitionExamAnswerSaveReq request) {
    return ApiResponse.ok(service.saveAnswer(request));
  }

  @PostMapping("/submit")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamSubmissionVO> submit(
      @Valid @RequestBody CompetitionExamSubmitReq request) {
    return ApiResponse.ok(service.submit(request.requestId()));
  }

  @GetMapping("/submission")
  @PreAuthorize("hasRole('COMPETITION_EXAM')")
  public ApiResponse<CompetitionExamSubmissionVO> submission() {
    return ApiResponse.ok(service.submissionStatus());
  }
}
