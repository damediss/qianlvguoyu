package com.guoyu.platform.competition.controller;

import com.guoyu.platform.competition.service.CompetitionQuestionBlueprintTemplateService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionQuestionBlueprintTemplateController {

  private final CompetitionQuestionBlueprintTemplateService service;

  public CompetitionQuestionBlueprintTemplateController(CompetitionQuestionBlueprintTemplateService service) {
    this.service = service;
  }

  @GetMapping("/question-blueprint-template")
  @PreAuthorize("hasAuthority('competition:question-blueprint:view')")
  public ResponseEntity<byte[]> download(@RequestParam String contentType) {
    var template = service.get(contentType);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(template.filename(), StandardCharsets.UTF_8).build().toString())
        .header(HttpHeaders.CONTENT_TYPE, template.contentType())
        .body(template.content());
  }
}
