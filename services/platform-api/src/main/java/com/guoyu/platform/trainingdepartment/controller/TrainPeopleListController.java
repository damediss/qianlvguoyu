package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.security.TrainingAuthorityExpressions;
import com.guoyu.platform.trainingdepartment.service.TrainPeopleListApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/training")
public class TrainPeopleListController {

  private final TrainPeopleListApplicationService trainPeopleListApplicationService;

  public TrainPeopleListController(TrainPeopleListApplicationService trainPeopleListApplicationService) {
    this.trainPeopleListApplicationService = trainPeopleListApplicationService;
  }

  // ==================== 基础CRUD ====================

  @GetMapping("/people-lists")
  @PreAuthorize("hasAuthority('train:people-list:view')")
  public ApiResponse<PageResponse<TrainPeopleListVO>> page(@Valid @ModelAttribute TrainPeopleListQueryReq req) {
    return ApiResponse.ok(trainPeopleListApplicationService.page(req));
  }

  @GetMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:view')")
  public ApiResponse<TrainPeopleListVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(trainPeopleListApplicationService.getById(id));
  }

  @PostMapping("/people-lists")
  @PreAuthorize("hasAuthority('train:people-list:create')")
  @AuditLog(module = "training", action = "people-list:create", targetType = "train_people_list")
  public ApiResponse<TrainPeopleListVO> create(@Valid @RequestBody TrainPeopleListCreateReq req) {
    return ApiResponse.ok(trainPeopleListApplicationService.create(req));
  }

  @PutMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:update')")
  @AuditLog(module = "training", action = "people-list:update", targetType = "train_people_list")
  public ApiResponse<TrainPeopleListVO> update(@PathVariable Long id, @Valid @RequestBody TrainPeopleListUpdateReq req) {
    return ApiResponse.ok(trainPeopleListApplicationService.update(id, req));
  }

  @DeleteMapping("/people-lists/{id}")
  @PreAuthorize("hasAuthority('train:people-list:delete')")
  @AuditLog(module = "training", action = "people-list:delete", targetType = "train_people_list")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    trainPeopleListApplicationService.delete(id);
    return ApiResponse.ok();
  }

  @PutMapping("/people-lists/batch-audit")
  @PreAuthorize("hasAuthority('train:people-list:audit')")
  @AuditLog(module = "training", action = "people-list:batch-audit", targetType = "train_people_list")
  public ApiResponse<Void> batchAudit(@Valid @RequestBody SignupAuditReq req) {
    trainPeopleListApplicationService.batchAudit(req);
    return ApiResponse.ok();
  }

  // ==================== 班级学员 ====================

  /** 班级学员列表 */
  @GetMapping("/class-student-list")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> classStudentList(
      @RequestParam Long classId,
      @RequestParam(defaultValue = "1") int currentPage,
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.ok(trainPeopleListApplicationService.classStudentList(classId, currentPage, limit));
  }

  /** 编辑学员信息 */
  @PostMapping("/class-student-save")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_UPDATE)
  @AuditLog(module = "training", action = "class-student:save", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> saveStudent(@RequestBody Map<String, Object> params) {
    return ApiResponse.ok(trainPeopleListApplicationService.saveStudent(params));
  }

  /** 批量删除学员 */
  @PostMapping("/class-student-del")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_DELETE)
  @AuditLog(module = "training", action = "class-student:delete", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> deleteStudents(@RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainPeopleListApplicationService.batchDeleteStudents(body.get("studentIds")));
  }

  /** 可导入的报名人员 */
  @GetMapping("/sign-up-self-list")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> signUpSelfList(
      @RequestParam Long traininfoId,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(trainPeopleListApplicationService.signUpSelfList(traininfoId, keyword));
  }

  /** 从报名导入学员 */
  @PostMapping("/class-student-import")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_CREATE)
  @AuditLog(module = "training", action = "class-student:import", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> importStudents(@RequestBody Map<String, Object> params) {
    Long traininfoId = params.get("traininfoId") != null ? Long.valueOf(params.get("traininfoId").toString()) : null;
    Long classId = params.get("classId") != null ? Long.valueOf(params.get("classId").toString()) : null;
    @SuppressWarnings("unchecked")
    List<Long> studentIds = ((List<Number>) params.get("studentIds")).stream()
        .map(Number::longValue).toList();
    return ApiResponse.ok(trainPeopleListApplicationService.importStudents(traininfoId, classId, studentIds));
  }

  /** 按身份证号导入学员 */
  @PostMapping("/class-student-import-by-idcard")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_CREATE)
  @AuditLog(module = "training", action = "class-student:import-by-idcard", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> importStudentsByIdCard(@RequestBody Map<String, Object> params) {
    Long traininfoId = params.get("traininfoId") != null ? Long.valueOf(params.get("traininfoId").toString()) : null;
    Long classId = params.get("classId") != null ? Long.valueOf(params.get("classId").toString()) : null;
    String studentIdsText = params.get("studentIds") != null ? params.get("studentIds").toString() : "";
    return ApiResponse.ok(trainPeopleListApplicationService.importStudentsByIdCard(traininfoId, classId, studentIdsText));
  }

  // ==================== 教务/负责人 ====================

  /** 教务/负责人列表 */
  @GetMapping("/class-other")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> classOtherList(
      @RequestParam Long train_info_id,
      @RequestParam Long id,
      @RequestParam String tag) {
    return ApiResponse.ok(trainPeopleListApplicationService.classOtherList(train_info_id, id, tag));
  }

  /** 新增/编辑教务/负责人 */
  @PostMapping("/class-other-save")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_CREATE)
  @AuditLog(module = "training", action = "class-other:save", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> saveOther(@RequestBody Map<String, Object> params) {
    return ApiResponse.ok(trainPeopleListApplicationService.saveOther(params));
  }

  /** 批量删除教务/负责人 */
  @PostMapping("/class-other-del")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_DELETE)
  @AuditLog(module = "training", action = "class-other:delete", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> deleteOthers(@RequestBody Map<String, List<Long>> body) {
    return ApiResponse.ok(trainPeopleListApplicationService.batchDeleteOthers(body.get("ids")));
  }

  /** 可导入的教务数据源（教师库） */
  @GetMapping("/class-other-import-data")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_VIEW)
  public ApiResponse<Map<String, Object>> otherImportData(@RequestParam String tag) {
    return ApiResponse.ok(trainPeopleListApplicationService.otherImportData(tag));
  }

  /** 从教师库导入教务人员 */
  @PostMapping("/class-other-import")
  @PreAuthorize(TrainingAuthorityExpressions.CLASS_PEOPLE_CREATE)
  @AuditLog(module = "training", action = "class-other:import", targetType = "train_people_list")
  public ApiResponse<Map<String, Object>> importOthers(@RequestBody Map<String, Object> params) {
    Long traininfoId = params.get("traininfoId") != null ? Long.valueOf(params.get("traininfoId").toString()) : null;
    Long classId = params.get("classId") != null ? Long.valueOf(params.get("classId").toString()) : null;
    Object rawIds = params.get("ids");
    if (!(rawIds instanceof List<?> idList) || idList.isEmpty()) {
      return ApiResponse.ok(Map.of("count", 0, "failed", List.of()));
    }
    List<Long> ids = idList.stream()
        .filter(Number.class::isInstance)
        .map(id -> ((Number) id).longValue())
        .toList();
    return ApiResponse.ok(trainPeopleListApplicationService.importOthers(traininfoId, classId, ids));
  }
}
