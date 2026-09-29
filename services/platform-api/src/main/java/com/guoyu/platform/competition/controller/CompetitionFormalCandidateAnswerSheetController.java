package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetApplicationService;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateAnswerSheetVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/formal-exam-candidates")
public class CompetitionFormalCandidateAnswerSheetController {

  private final CompetitionFormalCandidateAnswerSheetApplicationService service;

  public CompetitionFormalCandidateAnswerSheetController(
      CompetitionFormalCandidateAnswerSheetApplicationService service) {
    this.service = service;
  }

  @GetMapping("/{candidateExamId}/answer-sheet")
  @PreAuthorize("hasAuthority('competition:exam-session:view')")
  public ApiResponse<CompetitionFormalCandidateAnswerSheetVO> answerSheet(
      @PathVariable Long candidateExamId) {
    return ApiResponse.ok(service.getAnswerSheet(candidateExamId));
  }
}
