package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.ClassPracticeScopeSaveReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.ClassCollectionApplicationService;
import com.guoyu.platform.trainingdepartment.service.ClassPracticeAnalyticsApplicationService;
import com.guoyu.platform.trainingdepartment.service.ClassPracticeScopeApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainInfoClassApplicationService;
import com.guoyu.platform.trainingdepartment.service.TrainMaterialGenerateApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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

/**
 * 培训班级管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainInfoClassController {

  private final TrainInfoClassApplicationService trainInfoClassApplicationService;
  private final TrainMaterialGenerateApplicationService trainMaterialGenerateApplicationService;
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private final ClassPracticeScopeApplicationService classPracticeScopeApplicationService;
  private final ClassPracticeAnalyticsApplicationService classPracticeAnalyticsApplicationService;

  public TrainInfoClassController(TrainInfoClassApplicationService trainInfoClassApplicationService,
      TrainMaterialGenerateApplicationService trainMaterialGenerateApplicationService,
      ClassCollectionApplicationService classCollectionApplicationService,
      ClassPracticeScopeApplicationService classPracticeScopeApplicationService,
      ClassPracticeAnalyticsApplicationService classPracticeAnalyticsApplicationService) {
    this.trainInfoClassApplicationService = trainInfoClassApplicationService;
    this.trainMaterialGenerateApplicationService = trainMaterialGenerateApplicationService;
    this.classCollectionApplicationService = classCollectionApplicationService;
    this.classPracticeScopeApplicationService = classPracticeScopeApplicationService;
    this.classPracticeAnalyticsApplicationService = classPracticeAnalyticsApplicationService;
  }

  /** 分页查询培训班级列表（含企业名、工种名、培训标题等关联字段） */
  @GetMapping("/info-classes")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_PAGE_VIEW)
  public ApiResponse<Map<String, Object>> page(@Valid @ModelAttribute TrainInfoClassQueryReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.page(req));
  }

  /** 根据ID查询培训班级详情 */
  @GetMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:view')")
  public ApiResponse<TrainInfoClassVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainInfoClassApplicationService.getById(id));
  }

  /** 新增/编辑培训班级（id=0新增, id>0编辑） */
  @PostMapping("/info-classes/save")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_SAVE)
  @AuditLog(module = "training", action = "info-class:save", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> save(@Valid @RequestBody TrainInfoClassCreateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.save(req));
  }

  /** 批量删除培训班级 */
  @PostMapping("/info-classes/batch-delete")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_BATCH_DELETE)
  @AuditLog(module = "training", action = "info-class:batch-delete", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainInfoClassApplicationService.batchDelete(body.get("ids")));
  }

  /** 新增培训班级 */
  @PostMapping("/info-classes")
  @PreAuthorize("hasAuthority('train:info-class:create')")
  @AuditLog(module = "training", action = "info-class:create", targetType = "train_info_class")
  public ApiResponse<TrainInfoClassVO> create(@Valid @RequestBody TrainInfoClassCreateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.create(req));
  }

  /** 修改培训班级 */
  @PutMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:update')")
  @AuditLog(module = "training", action = "info-class:update", targetType = "train_info_class")
  public ApiResponse<TrainInfoClassVO> update(@PathVariable Long id, @Valid @RequestBody TrainInfoClassUpdateReq req) {
    return ApiResponse.ok(trainInfoClassApplicationService.update(id, req));
  }

  /** 删除培训班级 */
  @DeleteMapping("/info-classes/{id}")
  @PreAuthorize("hasAuthority('train:info-class:delete')")
  @AuditLog(module = "training", action = "info-class:delete", targetType = "train_info_class")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainInfoClassApplicationService.delete(id);
    return ApiResponse.ok();
  }

  /** 挂载文件模板到班级 */
  @PostMapping("/info-classes/{classId}/file-temps")
  @PreAuthorize(TrainingAuthorityExpressions.DECLARE_CLASS_MUTATE)
  @AuditLog(module = "training", action = "info-class:attach-file-temps", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> attachFileTemps(@PathVariable Long classId, @RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainInfoClassApplicationService.attachFileTemps(classId, body.get("ids")));
  }

  /** 移除班级文件模板 */
  @DeleteMapping("/info-classes/{classId}/file-temps/{templateId}")
  @PreAuthorize(TrainingAuthorityExpressions.DECLARE_CLASS_MUTATE)
  @AuditLog(module = "training", action = "info-class:detach-file-temp", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> detachFileTemp(@PathVariable Long classId, @PathVariable Long templateId) {
    return ApiResponse.ok(trainInfoClassApplicationService.detachFileTemp(classId, templateId));
  }

  /** 生成班级申报材料 */
  @PostMapping("/info-classes/{classId}/materials/generate")
  @PreAuthorize(TrainingAuthorityExpressions.DECLARE_CLASS_MUTATE)
  @AuditLog(module = "training", action = "info-class:generate-materials", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> generateMaterials(@PathVariable Long classId) {
    return ApiResponse.ok(trainMaterialGenerateApplicationService.generate(classId));
  }

  /** 企业确认分班 */
  @PostMapping("/info-classes/{id}/company-confirm")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "info-class:company-confirm", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> companyConfirm(@PathVariable Long id) {
    return ApiResponse.ok(trainInfoClassApplicationService.companyConfirm(id));
  }

  /** 企业驳回分班 */
  @PostMapping("/info-classes/{id}/company-reject")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_ENTERPRISE_CONFIRM)
  @AuditLog(module = "training", action = "info-class:company-reject", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> companyReject(@PathVariable Long id, @RequestParam String reason) {
    return ApiResponse.ok(trainInfoClassApplicationService.companyReject(id, reason));
  }

  /** 开班准备：确保本班资料收集计划并从花名册种子人员 */
  @PostMapping("/info-classes/{classId}/ensure-collection")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  @AuditLog(module = "training", action = "info-class:ensure-collection", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> ensureCollection(@PathVariable Long classId) {
    return ApiResponse.ok(classCollectionApplicationService.ensureCollection(classId));
  }

  /** 班级练习范围可选科目（培训权限，不依赖 exam:course:view） */
  @GetMapping("/practice-scope/options/courses")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> practiceScopeCourseOptions(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long workTypeId) {
    return ApiResponse.ok(classPracticeScopeApplicationService.listCourseOptions(keyword, workTypeId));
  }

  /** 班级练习范围知识点树 */
  @GetMapping("/practice-scope/options/knowledge-tree")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<List<Map<String, Object>>> practiceScopeKnowledgeTree(
      @RequestParam Integer courseId) {
    return ApiResponse.ok(classPracticeScopeApplicationService.knowledgeTree(courseId));
  }

  /** 班级练习范围（科目 + 知识点白名单） */
  @GetMapping("/info-classes/{classId}/practice-scope")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> getPracticeScope(@PathVariable Long classId) {
    return ApiResponse.ok(classPracticeScopeApplicationService.getScope(classId));
  }

  /** 按班级工种推荐科目（不落库） */
  @GetMapping("/info-classes/{classId}/practice-scope/suggest-courses")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> suggestPracticeCourses(@PathVariable Long classId) {
    return ApiResponse.ok(classPracticeScopeApplicationService.suggestCourses(classId));
  }

  /** 保存班级练习范围 */
  @PutMapping("/info-classes/{classId}/practice-scope")
  @PreAuthorize(TrainingAuthorityExpressions.INFO_CLASS_SAVE)
  @AuditLog(module = "training", action = "info-class:practice-scope-save", targetType = "train_info_class")
  public ApiResponse<Map<String, Object>> savePracticeScope(
      @PathVariable Long classId,
      @Valid @RequestBody ClassPracticeScopeSaveReq req) {
    return ApiResponse.ok(classPracticeScopeApplicationService.saveScope(classId, req));
  }

  /** 班级刷题学情：总览 */
  @GetMapping("/info-classes/{classId}/practice-analytics/summary")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> practiceAnalyticsSummary(
      @PathVariable Long classId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "true") boolean inScopeOnly) {
    return ApiResponse.ok(classPracticeAnalyticsApplicationService.summary(classId, from, to, inScopeOnly));
  }

  /** 班级刷题学情：学员明细 */
  @GetMapping("/info-classes/{classId}/practice-analytics/students")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> practiceAnalyticsStudents(
      @PathVariable Long classId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "true") boolean inScopeOnly,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestParam(required = false, defaultValue = "20") int pageSize) {
    return ApiResponse.ok(classPracticeAnalyticsApplicationService.students(
        classId, from, to, inScopeOnly, keyword, page, pageSize));
  }

  /** 班级刷题学情：薄弱知识点 */
  @GetMapping("/info-classes/{classId}/practice-analytics/knowledges")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> practiceAnalyticsKnowledges(
      @PathVariable Long classId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "true") boolean inScopeOnly,
      @RequestParam(required = false, defaultValue = "30") int limit) {
    return ApiResponse.ok(classPracticeAnalyticsApplicationService.knowledges(
        classId, from, to, inScopeOnly, limit));
  }

  /** 班级刷题学情：每日趋势 */
  @GetMapping("/info-classes/{classId}/practice-analytics/daily")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> practiceAnalyticsDaily(
      @PathVariable Long classId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "true") boolean inScopeOnly) {
    return ApiResponse.ok(classPracticeAnalyticsApplicationService.daily(classId, from, to, inScopeOnly));
  }

  /** 班级刷题学情：导出 Excel（指定日期或全部历史） */
  @GetMapping("/info-classes/{classId}/practice-analytics/export")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ResponseEntity<byte[]> practiceAnalyticsExport(
      @PathVariable Long classId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "true") boolean inScopeOnly,
      @RequestParam(required = false, defaultValue = "false") boolean allTime) {
    return classPracticeAnalyticsApplicationService.exportExcel(classId, from, to, inScopeOnly, allTime);
  }
}
