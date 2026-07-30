package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchCollectionCreatePlanReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.CertVerifyReq;
import com.guoyu.platform.trainingdepartment.service.TrainResearchCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchCollectionVO;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchPersonVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanVO;
import com.guoyu.platform.trainingdepartment.vo.CertVerifyBatchResultVO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training/research-orgs/{researchOrgId:\\d+}/collection")
public class TrainResearchCollectionController {

  private final TrainResearchCollectionApplicationService collectionService;

  public TrainResearchCollectionController(TrainResearchCollectionApplicationService collectionService) {
    this.collectionService = collectionService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('train:research_org:view')")
  public ApiResponse<TrainResearchCollectionVO> getCollection(@PathVariable Long researchOrgId) {
    return ApiResponse.ok(collectionService.getCollection(researchOrgId));
  }

  @PostMapping("/start")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  @AuditLog(module = "training", action = "research-collection:start", targetType = "train_research_org")
  public ApiResponse<TrainResearchCollectionVO> startCollection(@PathVariable Long researchOrgId) {
    return ApiResponse.ok(collectionService.startCollection(researchOrgId));
  }

  @GetMapping("/people")
  @PreAuthorize("hasAuthority('train:research_org:view')")
  public ApiResponse<PageResponse<TrainResearchPersonVO>> pagePeople(
      @PathVariable Long researchOrgId,
      @Valid TrainResearchPersonQueryReq req) {
    return ApiResponse.ok(collectionService.pagePeople(researchOrgId, req));
  }

  @PostMapping("/people")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  @AuditLog(module = "training", action = "research-collection:create-person", targetType = "train_sign_up_people_self")
  public ApiResponse<TrainResearchPersonVO> createPerson(
      @PathVariable Long researchOrgId,
      @Valid @RequestBody TrainResearchPersonCreateReq req) {
    return ApiResponse.ok(collectionService.createPerson(researchOrgId, req));
  }

  @PutMapping("/people/{peopleId:\\d+}")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  @AuditLog(module = "training", action = "research-collection:update-person", targetType = "train_sign_up_people_self")
  public ApiResponse<TrainResearchPersonVO> updatePerson(
      @PathVariable Long researchOrgId,
      @PathVariable Long peopleId,
      @Valid @RequestBody TrainResearchPersonUpdateReq req) {
    return ApiResponse.ok(collectionService.updatePerson(researchOrgId, peopleId, req));
  }

  @DeleteMapping("/people/{peopleId:\\d+}")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  @AuditLog(module = "training", action = "research-collection:delete-person", targetType = "train_sign_up_people_self")
  public ApiResponse<Void> deletePerson(
      @PathVariable Long researchOrgId,
      @PathVariable Long peopleId) {
    collectionService.deletePerson(researchOrgId, peopleId);
    return ApiResponse.ok();
  }

  @PostMapping("/people/verify-cert")
  @PreAuthorize("hasAuthority('train:research_org:update')")
  @AuditLog(module = "training", action = "research-collection:verify-cert", targetType = "train_sign_up_people_self")
  public ApiResponse<CertVerifyBatchResultVO> verifyPeopleCertificate(
      @PathVariable Long researchOrgId,
      @Valid @RequestBody CertVerifyReq req) {
    ///users/import/{ymd}/cert
    //ChazhengController.certCheck
    return ApiResponse.ok(collectionService.verifyPeopleCertificate(researchOrgId, req.peopleIds()));
  }

  @GetMapping("/export-template")
  @PreAuthorize("hasAuthority('train:research_org:view')")
  public ResponseEntity<byte[]> exportTemplate(@PathVariable Long researchOrgId) {
    return collectionService.exportCollectionTemplate(researchOrgId);
  }

  @PostMapping("/create-three-year-plan")
  @PreAuthorize("hasAuthority('pxanfangguihua:manage')")
  @AuditLog(module = "training", action = "research-collection:create-plan", targetType = "train_three_year_plan")
  public ApiResponse<TrainThreeYearPlanVO> createThreeYearPlan(
      @PathVariable Long researchOrgId,
      @RequestBody(required = false) TrainResearchCollectionCreatePlanReq req) {
    return ApiResponse.ok(collectionService.createThreeYearPlan(researchOrgId, req));
  }
}
