package com.guoyu.platform.certification.info.controller;

import com.guoyu.platform.certification.info.application.CertificationInfoApplicationService;
import com.guoyu.platform.certification.info.application.AssessorLedgerImportService;
import com.guoyu.platform.certification.info.dto.AssessorInfoSaveReq;
import com.guoyu.platform.certification.info.dto.AssessorLedgerImportStartReq;
import com.guoyu.platform.certification.info.dto.AssessorAccountCreateReq;
import com.guoyu.platform.certification.info.dto.CertificationInfoQueryReq;
import com.guoyu.platform.certification.info.dto.ExamEquipmentInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamSiteInfoSaveReq;
import com.guoyu.platform.certification.info.dto.ExamStaffInfoSaveReq;
import com.guoyu.platform.certification.info.dto.InfoDisableReq;
import com.guoyu.platform.certification.info.dto.InfoReviewReq;
import com.guoyu.platform.certification.info.vo.AssessorInfoVO;
import com.guoyu.platform.certification.info.vo.AssessorLedgerImportVO;
import com.guoyu.platform.certification.info.vo.ExamEquipmentInfoVO;
import com.guoyu.platform.certification.info.vo.ExamSiteInfoVO;
import com.guoyu.platform.certification.info.vo.ExamStaffInfoVO;
import com.guoyu.platform.certification.info.vo.InfoAuditRecordVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/info")
public class CertificationInfoController {

  private final CertificationInfoApplicationService service;
  private final AssessorLedgerImportService assessorLedgerImportService;

  public CertificationInfoController(
      CertificationInfoApplicationService service,
      AssessorLedgerImportService assessorLedgerImportService
  ) {
    this.service = service;
    this.assessorLedgerImportService = assessorLedgerImportService;
  }

  @GetMapping("/assessors")
  @PreAuthorize("hasAuthority('certification.info.assessor.view')")
  public ApiResponse<PageResponse<AssessorInfoVO>> pageAssessors(@Valid @ModelAttribute CertificationInfoQueryReq req) {
    return ApiResponse.ok(service.pageAssessors(req));
  }

  @PostMapping("/assessors")
  @PreAuthorize("hasAuthority('certification.info.assessor.create')")
  @AuditLog(module = "certification", action = "info-assessor:create", targetType = "certification_assessor")
  public ApiResponse<AssessorInfoVO> createAssessor(@Valid @RequestBody AssessorInfoSaveReq req) {
    return ApiResponse.ok(service.createAssessor(req));
  }

  @PostMapping("/assessors/imports")
  @PreAuthorize("hasAuthority('certification.info.assessor.create')")
  @AuditLog(module = "certification", action = "info-assessor:ledger-import", targetType = "certification_assessor")
  public ApiResponse<AssessorLedgerImportVO> startAssessorLedgerImport(
      @Valid @RequestBody AssessorLedgerImportStartReq req
  ) {
    return ApiResponse.ok(assessorLedgerImportService.start(req.fileId()));
  }

  @GetMapping("/assessors/imports/{id}")
  @PreAuthorize("hasAuthority('certification.info.assessor.view')")
  public ApiResponse<AssessorLedgerImportVO> getAssessorLedgerImport(@PathVariable Long id) {
    return ApiResponse.ok(assessorLedgerImportService.get(id));
  }

  @GetMapping("/assessors/{id}")
  @PreAuthorize("hasAuthority('certification.info.assessor.view')")
  public ApiResponse<AssessorInfoVO> getAssessor(@PathVariable Long id) {
    return ApiResponse.ok(service.getAssessor(id));
  }

  @PutMapping("/assessors/{id}")
  @PreAuthorize("hasAuthority('certification.info.assessor.update')")
  @AuditLog(module = "certification", action = "info-assessor:update", targetType = "certification_assessor")
  public ApiResponse<AssessorInfoVO> updateAssessor(@PathVariable Long id, @Valid @RequestBody AssessorInfoSaveReq req) {
    return ApiResponse.ok(service.updateAssessor(id, req));
  }

  @PostMapping("/assessors/{id}/account")
  @PreAuthorize("hasAuthority('certification.info.assessor.update')")
  @AuditLog(module = "certification", action = "info-assessor:create-account", targetType = "certification_assessor")
  public ApiResponse<AssessorInfoVO> createAssessorAccount(
      @PathVariable Long id,
      @Valid @RequestBody AssessorAccountCreateReq req
  ) {
    return ApiResponse.ok(service.createAssessorAccount(id, req));
  }

  @PostMapping("/assessors/{id}/submit")
  @PreAuthorize("hasAuthority('certification.info.assessor.submit')")
  @AuditLog(module = "certification", action = "info-assessor:submit", targetType = "certification_assessor")
  public ApiResponse<AssessorInfoVO> submitAssessor(@PathVariable Long id) {
    return ApiResponse.ok(service.submitAssessor(id));
  }

  @PostMapping("/assessors/{id}/review")
  @PreAuthorize("hasAuthority('certification.info.assessor.review')")
  @AuditLog(module = "certification", action = "info-assessor:review", targetType = "certification_assessor")
  public ApiResponse<AssessorInfoVO> reviewAssessor(@PathVariable Long id, @Valid @RequestBody InfoReviewReq req) {
    return ApiResponse.ok(service.reviewAssessor(id, req));
  }

  @PostMapping("/assessors/{id}/disable")
  @PreAuthorize("hasAuthority('certification.info.assessor.update')")
  @AuditLog(module = "certification", action = "info-assessor:disable", targetType = "certification_assessor")
  public ApiResponse<Void> disableAssessor(@PathVariable Long id, @RequestBody(required = false) InfoDisableReq req) {
    service.disableAssessor(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/assessors/{id}/enable")
  @PreAuthorize("hasAuthority('certification.info.assessor.update')")
  @AuditLog(module = "certification", action = "info-assessor:enable", targetType = "certification_assessor")
  public ApiResponse<Void> enableAssessor(@PathVariable Long id) {
    service.enableAssessor(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/assessors/{id}")
  @PreAuthorize("hasAuthority('certification.info.assessor.delete')")
  @AuditLog(module = "certification", action = "info-assessor:delete", targetType = "certification_assessor")
  public ApiResponse<Void> deleteAssessor(@PathVariable Long id) {
    service.deleteAssessor(id);
    return ApiResponse.ok();
  }

  @GetMapping("/assessors/available")
  @PreAuthorize("hasAuthority('certification.info.assessor.view')")
  public ApiResponse<List<AssessorInfoVO>> listAvailableAssessors(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) List<String> requiredWorkTypes,
      @RequestParam(defaultValue = "false") boolean includeAssigned
  ) {
    return ApiResponse.ok(service.listAvailableAssessors(keyword, requiredWorkTypes, includeAssigned));
  }

  @GetMapping("/exam-staff")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.view')")
  public ApiResponse<PageResponse<ExamStaffInfoVO>> pageExamStaff(@Valid @ModelAttribute CertificationInfoQueryReq req) {
    return ApiResponse.ok(service.pageExamStaff(req));
  }

  @PostMapping("/exam-staff")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.create')")
  @AuditLog(module = "certification", action = "info-exam-staff:create", targetType = "certification_info_exam_staff")
  public ApiResponse<ExamStaffInfoVO> createExamStaff(@Valid @RequestBody ExamStaffInfoSaveReq req) {
    return ApiResponse.ok(service.createExamStaff(req));
  }

  @GetMapping("/exam-staff/available")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.view')")
  public ApiResponse<List<ExamStaffInfoVO>> listAvailableExamStaff(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.listAvailableExamStaff(keyword));
  }

  @GetMapping("/exam-staff/{id}")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.view')")
  public ApiResponse<ExamStaffInfoVO> getExamStaff(@PathVariable Long id) {
    return ApiResponse.ok(service.getExamStaff(id));
  }

  @PutMapping("/exam-staff/{id}")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.update')")
  @AuditLog(module = "certification", action = "info-exam-staff:update", targetType = "certification_info_exam_staff")
  public ApiResponse<ExamStaffInfoVO> updateExamStaff(@PathVariable Long id, @Valid @RequestBody ExamStaffInfoSaveReq req) {
    return ApiResponse.ok(service.updateExamStaff(id, req));
  }

  @PostMapping("/exam-staff/{id}/submit")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.submit')")
  @AuditLog(module = "certification", action = "info-exam-staff:submit", targetType = "certification_info_exam_staff")
  public ApiResponse<ExamStaffInfoVO> submitExamStaff(@PathVariable Long id) {
    return ApiResponse.ok(service.submitExamStaff(id));
  }

  @PostMapping("/exam-staff/{id}/review")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.review')")
  @AuditLog(module = "certification", action = "info-exam-staff:review", targetType = "certification_info_exam_staff")
  public ApiResponse<ExamStaffInfoVO> reviewExamStaff(@PathVariable Long id, @Valid @RequestBody InfoReviewReq req) {
    return ApiResponse.ok(service.reviewExamStaff(id, req));
  }

  @PostMapping("/exam-staff/{id}/disable")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.update')")
  @AuditLog(module = "certification", action = "info-exam-staff:disable", targetType = "certification_info_exam_staff")
  public ApiResponse<Void> disableExamStaff(@PathVariable Long id, @RequestBody(required = false) InfoDisableReq req) {
    service.disableExamStaff(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-staff/{id}/enable")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.update')")
  @AuditLog(module = "certification", action = "info-exam-staff:enable", targetType = "certification_info_exam_staff")
  public ApiResponse<Void> enableExamStaff(@PathVariable Long id) {
    service.enableExamStaff(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/exam-staff/{id}")
  @PreAuthorize("hasAuthority('certification.info.exam-staff.delete')")
  @AuditLog(module = "certification", action = "info-exam-staff:delete", targetType = "certification_info_exam_staff")
  public ApiResponse<Void> deleteExamStaff(@PathVariable Long id) {
    service.deleteExamStaff(id);
    return ApiResponse.ok();
  }

  @GetMapping("/sites")
  @PreAuthorize("hasAuthority('certification.info.site.view')")
  public ApiResponse<PageResponse<ExamSiteInfoVO>> pageSites(@Valid @ModelAttribute CertificationInfoQueryReq req) {
    return ApiResponse.ok(service.pageSites(req));
  }

  @PostMapping("/sites")
  @PreAuthorize("hasAuthority('certification.info.site.create')")
  @AuditLog(module = "certification", action = "info-site:create", targetType = "certification_exam_site")
  public ApiResponse<ExamSiteInfoVO> createSite(@Valid @RequestBody ExamSiteInfoSaveReq req) {
    return ApiResponse.ok(service.createSite(req));
  }

  @GetMapping("/sites/{id}")
  @PreAuthorize("hasAuthority('certification.info.site.view')")
  public ApiResponse<ExamSiteInfoVO> getSite(@PathVariable Long id) {
    return ApiResponse.ok(service.getSite(id));
  }

  @PutMapping("/sites/{id}")
  @PreAuthorize("hasAuthority('certification.info.site.update')")
  @AuditLog(module = "certification", action = "info-site:update", targetType = "certification_exam_site")
  public ApiResponse<ExamSiteInfoVO> updateSite(@PathVariable Long id, @Valid @RequestBody ExamSiteInfoSaveReq req) {
    return ApiResponse.ok(service.updateSite(id, req));
  }

  @PostMapping("/sites/{id}/submit")
  @PreAuthorize("hasAuthority('certification.info.site.submit')")
  @AuditLog(module = "certification", action = "info-site:submit", targetType = "certification_exam_site")
  public ApiResponse<ExamSiteInfoVO> submitSite(@PathVariable Long id) {
    return ApiResponse.ok(service.submitSite(id));
  }

  @PostMapping("/sites/{id}/review")
  @PreAuthorize("hasAuthority('certification.info.site.review')")
  @AuditLog(module = "certification", action = "info-site:review", targetType = "certification_exam_site")
  public ApiResponse<ExamSiteInfoVO> reviewSite(@PathVariable Long id, @Valid @RequestBody InfoReviewReq req) {
    return ApiResponse.ok(service.reviewSite(id, req));
  }

  @PostMapping("/sites/{id}/disable")
  @PreAuthorize("hasAuthority('certification.info.site.update')")
  @AuditLog(module = "certification", action = "info-site:disable", targetType = "certification_exam_site")
  public ApiResponse<Void> disableSite(@PathVariable Long id, @RequestBody(required = false) InfoDisableReq req) {
    service.disableSite(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/sites/{id}/enable")
  @PreAuthorize("hasAuthority('certification.info.site.update')")
  @AuditLog(module = "certification", action = "info-site:enable", targetType = "certification_exam_site")
  public ApiResponse<Void> enableSite(@PathVariable Long id) {
    service.enableSite(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/sites/{id}")
  @PreAuthorize("hasAuthority('certification.info.site.delete')")
  @AuditLog(module = "certification", action = "info-site:delete", targetType = "certification_exam_site")
  public ApiResponse<Void> deleteSite(@PathVariable Long id) {
    service.deleteSite(id);
    return ApiResponse.ok();
  }

  @GetMapping("/sites/available")
  @PreAuthorize("hasAuthority('certification.info.site.view')")
  public ApiResponse<List<ExamSiteInfoVO>> listAvailableSites(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.listAvailableSites(keyword));
  }

  @GetMapping("/equipment")
  @PreAuthorize("hasAuthority('certification.info.equipment.view')")
  public ApiResponse<PageResponse<ExamEquipmentInfoVO>> pageEquipment(@Valid @ModelAttribute CertificationInfoQueryReq req) {
    return ApiResponse.ok(service.pageEquipment(req));
  }

  @PostMapping("/equipment")
  @PreAuthorize("hasAuthority('certification.info.equipment.create')")
  @AuditLog(module = "certification", action = "info-equipment:create", targetType = "certification_exam_equipment")
  public ApiResponse<ExamEquipmentInfoVO> createEquipment(@Valid @RequestBody ExamEquipmentInfoSaveReq req) {
    return ApiResponse.ok(service.createEquipment(req));
  }

  @GetMapping("/equipment/{id}")
  @PreAuthorize("hasAuthority('certification.info.equipment.view')")
  public ApiResponse<ExamEquipmentInfoVO> getEquipment(@PathVariable Long id) {
    return ApiResponse.ok(service.getEquipment(id));
  }

  @PutMapping("/equipment/{id}")
  @PreAuthorize("hasAuthority('certification.info.equipment.update')")
  @AuditLog(module = "certification", action = "info-equipment:update", targetType = "certification_exam_equipment")
  public ApiResponse<ExamEquipmentInfoVO> updateEquipment(@PathVariable Long id, @Valid @RequestBody ExamEquipmentInfoSaveReq req) {
    return ApiResponse.ok(service.updateEquipment(id, req));
  }

  @PostMapping("/equipment/{id}/submit")
  @PreAuthorize("hasAuthority('certification.info.equipment.submit')")
  @AuditLog(module = "certification", action = "info-equipment:submit", targetType = "certification_exam_equipment")
  public ApiResponse<ExamEquipmentInfoVO> submitEquipment(@PathVariable Long id) {
    return ApiResponse.ok(service.submitEquipment(id));
  }

  @PostMapping("/equipment/{id}/review")
  @PreAuthorize("hasAuthority('certification.info.equipment.review')")
  @AuditLog(module = "certification", action = "info-equipment:review", targetType = "certification_exam_equipment")
  public ApiResponse<ExamEquipmentInfoVO> reviewEquipment(@PathVariable Long id, @Valid @RequestBody InfoReviewReq req) {
    return ApiResponse.ok(service.reviewEquipment(id, req));
  }

  @PostMapping("/equipment/{id}/disable")
  @PreAuthorize("hasAuthority('certification.info.equipment.update')")
  @AuditLog(module = "certification", action = "info-equipment:disable", targetType = "certification_exam_equipment")
  public ApiResponse<Void> disableEquipment(@PathVariable Long id, @RequestBody(required = false) InfoDisableReq req) {
    service.disableEquipment(id, req);
    return ApiResponse.ok();
  }

  @PostMapping("/equipment/{id}/enable")
  @PreAuthorize("hasAuthority('certification.info.equipment.update')")
  @AuditLog(module = "certification", action = "info-equipment:enable", targetType = "certification_exam_equipment")
  public ApiResponse<Void> enableEquipment(@PathVariable Long id) {
    service.enableEquipment(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/equipment/{id}")
  @PreAuthorize("hasAuthority('certification.info.equipment.delete')")
  @AuditLog(module = "certification", action = "info-equipment:delete", targetType = "certification_exam_equipment")
  public ApiResponse<Void> deleteEquipment(@PathVariable Long id) {
    service.deleteEquipment(id);
    return ApiResponse.ok();
  }

  @GetMapping("/equipment/available")
  @PreAuthorize("hasAuthority('certification.info.equipment.view')")
  public ApiResponse<List<ExamEquipmentInfoVO>> listAvailableEquipment(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.listAvailableEquipment(keyword));
  }

  @GetMapping("/audit-records/{ownerType}/{ownerId}")
  @PreAuthorize("hasAuthority('certification.info')")
  public ApiResponse<List<InfoAuditRecordVO>> listAuditRecords(@PathVariable String ownerType, @PathVariable Long ownerId) {
    return ApiResponse.ok(service.listAuditRecords(ownerType, ownerId));
  }
}
