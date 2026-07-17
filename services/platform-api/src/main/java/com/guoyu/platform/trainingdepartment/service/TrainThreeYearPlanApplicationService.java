package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.client.RencaishibieClient;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanItemQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanItemUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanQueryReq;
import com.guoyu.platform.trainingdepartment.entity.TrainResearchOrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanMatrixEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainResearchOrgMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMatrixMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanAnalysisStatusVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanDetailVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanDistributionVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanItemVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanMatrixVO;
import com.guoyu.platform.trainingdepartment.support.TrainThreeYearPlanExcelExporter;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanVO;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainThreeYearPlanApplicationService {

  private static final Map<Integer, String> STATUS_LABELS = Map.of(
      0, "草稿",
      1, "分析中",
      2, "分析完成请审核",
      3, "待机构审核",
      4, "企业已确认",
      5, "已驳回",
      6, "已作废"
  );

  private static final Set<String> TERMINAL_RENCAI_STATUS = Set.of(
      "COMPLETED", "PENDING_MANUAL", "FAILED"
  );

  private final TrainThreeYearPlanMapper planMapper;
  private final TrainThreeYearPlanItemMapper itemMapper;
  private final TrainThreeYearPlanMatrixMapper matrixMapper;
  private final OrgMapper orgMapper;
  private final TrainResearchOrgMapper researchOrgMapper;
  private final RencaishibieClient rencaishibieClient;
  private final TrainingOperatorScopeService scopeService;
  private final ObjectMapper objectMapper;
  private final TrainThreeYearPlanExcelExporter excelExporter;
  private final FileApplicationService fileApplicationService;

  public TrainThreeYearPlanApplicationService(
      TrainThreeYearPlanMapper planMapper,
      TrainThreeYearPlanItemMapper itemMapper,
      TrainThreeYearPlanMatrixMapper matrixMapper,
      OrgMapper orgMapper,
      TrainResearchOrgMapper researchOrgMapper,
      RencaishibieClient rencaishibieClient,
      TrainingOperatorScopeService scopeService,
      ObjectMapper objectMapper,
      TrainThreeYearPlanExcelExporter excelExporter,
      FileApplicationService fileApplicationService) {
    this.planMapper = planMapper;
    this.itemMapper = itemMapper;
    this.matrixMapper = matrixMapper;
    this.orgMapper = orgMapper;
    this.researchOrgMapper = researchOrgMapper;
    this.rencaishibieClient = rencaishibieClient;
    this.scopeService = scopeService;
    this.objectMapper = objectMapper;
    this.excelExporter = excelExporter;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional
  public PageResponse<TrainThreeYearPlanVO> page(TrainThreeYearPlanQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long companyFilter = scopeService.resolveCompanyFilter(req.targetOrgId());

    LambdaQueryWrapper<TrainThreeYearPlanEntity> wrapper = new LambdaQueryWrapper<TrainThreeYearPlanEntity>()
        .eq(TrainThreeYearPlanEntity::getTenantId, tenantId)
        .orderByDesc(TrainThreeYearPlanEntity::getCreateTime);
    if (StringUtils.hasText(req.title())) {
      wrapper.like(TrainThreeYearPlanEntity::getTitle, req.title());
    }
    if (companyFilter != null) {
      wrapper.eq(TrainThreeYearPlanEntity::getTargetOrgId, companyFilter);
    }
    if (req.status() != null) {
      wrapper.eq(TrainThreeYearPlanEntity::getStatus, req.status());
    }

    Page<TrainThreeYearPlanEntity> page = new Page<>(req.page(), req.pageSize());
    Page<TrainThreeYearPlanEntity> result = planMapper.selectPage(page, wrapper);
    List<TrainThreeYearPlanVO> list = result.getRecords().stream()
        .map(entity -> {
          JsonNode statusNode = syncAnalysisStatusFromRencai(entity);
          if (statusNode == null && entity.getStatus() != null && entity.getStatus() == 1) {
            statusNode = safeAnalysisStatus(entity);
          }
          return toVO(entity, statusNode);
        })
        .toList();
    return new PageResponse<>(list, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public TrainThreeYearPlanVO getById(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    JsonNode statusNode = syncAnalysisStatusFromRencai(entity);
    if (statusNode == null && entity.getStatus() != null && entity.getStatus() == 1) {
      statusNode = safeAnalysisStatus(entity);
    }
    return toVO(entity, statusNode);
  }

  /** 详情页聚合接口：单次请求返回规划头、人员分页、矩阵与认定分布（均读本地库） */
  @Transactional
  public TrainThreeYearPlanDetailVO getDetailBundle(Long id, TrainThreeYearPlanItemQueryReq req) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    JsonNode statusNode = syncAnalysisStatusFromRencai(entity);
    if (statusNode == null && entity.getStatus() != null && entity.getStatus() == 1) {
      statusNode = safeAnalysisStatus(entity);
    }
    ensureLocalDataReady(entity);

    PageResponse<TrainThreeYearPlanItemVO> items = pageItemsFromDb(entity, req);
    if (items.total() == 0 && canPreviewFromRencai(entity)) {
      items = previewEmployeesFromRencai(entity, req);
    }

    List<TrainThreeYearPlanMatrixVO> matrix = listMatrixFromDb(entity);
    if (matrix.isEmpty() && canPreviewFromRencai(entity)) {
      matrix = previewMatrixFromRencai(entity);
    }

    return new TrainThreeYearPlanDetailVO(
        toVO(entity, statusNode),
        items,
        matrix,
        loadDistribution(entity)
    );
  }

  @Transactional
  public TrainThreeYearPlanVO createWithFile(TrainThreeYearPlanCreateReq req, MultipartFile file) {
    scopeService.requireOrgOperator();
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传员工 Excel 文件");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 .xlsx / .xls 格式");
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    OrgEntity org = orgMapper.selectById(req.targetOrgId());
    if (org == null || !tenantId.equals(org.getTenantId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "目标企业不存在");
    }

    TrainThreeYearPlanEntity entity = new TrainThreeYearPlanEntity();
    entity.setTenantId(tenantId);
    entity.setPlanNo(generatePlanNo());
    entity.setTitle(req.title().trim());
    entity.setTargetOrgId(req.targetOrgId());
    entity.setResearchOrgId(req.researchOrgId());
    entity.setPlanYears("2026,2027,2028");
    entity.setStatus(0);
    entity.setAnalysisStatus("pending");
    entity.setCreatedBy(SecurityContext.currentUser().getUserId());
    planMapper.insert(entity);

    JsonNode upload = rencaishibieClient.uploadFile(file, tenantId, entity.getId());
    long inputFileId = upload.path("file_id").asLong(0);
    if (inputFileId <= 0) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "人才识别文件上传失败");
    }
    entity.setRencaiInputFileId(inputFileId);

    JsonNode run = rencaishibieClient.startAnalysis(inputFileId, tenantId, entity.getId());
    long taskId = run.path("task_id").asLong(0);
    if (taskId <= 0) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "启动人才识别分析失败");
    }
    entity.setRencaiTaskId(taskId);
    entity.setStatus(1);
    entity.setAnalysisStatus("running");
    planMapper.updateById(entity);
    return toVO(entity, null);
  }

  @Transactional
  public TrainThreeYearPlanAnalysisStatusVO refreshAnalysisStatus(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getRencaiTaskId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "规划尚未关联人才识别任务");
    }

    JsonNode status = syncAnalysisStatusFromRencai(entity);
    if (status == null) {
      status = rencaishibieClient.getAnalysisStatus(
          entity.getRencaiTaskId(), entity.getTenantId(), entity.getId());
    }
    String rencaiStatus = status.path("status").asText("");

    Map<String, Object> raw = new LinkedHashMap<>();
    status.fields().forEachRemaining(f -> raw.put(f.getKey(), jsonValue(f.getValue())));

    return new TrainThreeYearPlanAnalysisStatusVO(
        entity.getId(),
        entity.getRencaiTaskId(),
        entity.getAnalysisStatus(),
        entity.getStatus(),
        rencaiStatus,
        status.path("total_rows").asInt(0),
        status.path("processed_rows").asInt(0),
        status.path("progress_pct").asDouble(0),
        status.path("recommended_rows").asInt(0),
        status.path("not_recommended_rows").asInt(0),
        status.path("pending_manual_rows").asInt(0),
        textOrNull(status, "error_message"),
        raw
    );
  }

  @Transactional
  public TrainThreeYearPlanVO syncFromRencai(Long id) {
    scopeService.requireOrgOperator();
    TrainThreeYearPlanEntity entity = getExisting(id);
    if (entity.getRencaiTaskId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未关联人才识别任务");
    }
    if (entity.getStatus() == null || entity.getStatus() < 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分析尚未完成，无法同步");
    }
    syncRencaiDataToLocal(entity);
    return toVO(entity, null);
  }

  /** 打开审核详情时自动拉取分析结果到本地，便于查看和修改 */
  @Transactional
  public TrainThreeYearPlanVO prepareReview(Long id) {
    scopeService.requireOrgOperator();
    TrainThreeYearPlanEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 2) {
      return toVO(entity, null);
    }
    long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, id)
        .eq(TrainThreeYearPlanItemEntity::getTenantId, entity.getTenantId()));
    if (itemCount == 0 && entity.getRencaiTaskId() != null) {
      syncRencaiDataToLocal(entity);
    }
    return toVO(entity, null);
  }

  @Transactional
  public TrainThreeYearPlanItemVO updateItem(Long planId, Long itemId, TrainThreeYearPlanItemUpdateReq req) {
    scopeService.requireOrgOperator();
    TrainThreeYearPlanEntity plan = getExisting(planId);
    if (plan.getStatus() == null || plan.getStatus() != 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅「分析完成请审核」状态下可修改明细");
    }
    TrainThreeYearPlanItemEntity entity = itemMapper.selectOne(
        new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
            .eq(TrainThreeYearPlanItemEntity::getId, itemId)
            .eq(TrainThreeYearPlanItemEntity::getPlanId, planId)
            .eq(TrainThreeYearPlanItemEntity::getTenantId, plan.getTenantId()));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "明细不存在");
    }
    if (req.personName() != null) {
      entity.setPersonName(req.personName().trim());
    }
    if (req.jobName() != null) {
      entity.setJobName(req.jobName().trim());
    }
    if (req.levelName() != null) {
      entity.setLevelName(req.levelName().trim());
    }
    if (req.eligibleDate() != null) {
      entity.setEligibleDate(req.eligibleDate().trim());
      entity.setPlanYear(parsePlanYear(entity.getEligibleDate()));
    }
    if (req.planYear() != null) {
      entity.setPlanYear(req.planYear());
    }
    entity.setSource("manual");
    itemMapper.updateById(entity);
    rebuildMatrixForPlan(plan);
    refreshDistributionSnapshotFromLocal(plan);
    return toItemVO(entity);
  }

  private void syncRencaiDataToLocal(TrainThreeYearPlanEntity entity) {
    Long tenantId = entity.getTenantId();
    Long taskId = entity.getRencaiTaskId();
    Long id = entity.getId();

    itemMapper.delete(new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, id)
        .eq(TrainThreeYearPlanItemEntity::getTenantId, tenantId));
    clearMatrixForPlan(id, tenantId);

    syncItemsFromRencai(entity, taskId);
    syncMatrixFromItems(entity);
    syncDistributionSnapshotFromRencai(entity);
    entity.setAnalysisStatus("done");
    planMapper.updateById(entity);
  }

  private void rebuildMatrixForPlan(TrainThreeYearPlanEntity plan) {
    clearMatrixForPlan(plan.getId(), plan.getTenantId());
    syncMatrixFromItems(plan);
  }

  /** 矩阵为派生汇总数据，重建时需物理删除，避免软删除与唯一键冲突 */
  private void clearMatrixForPlan(Long planId, Long tenantId) {
    matrixMapper.physicalDeleteByPlan(planId, tenantId);
  }

  @Transactional
  public TrainThreeYearPlanVO submit(Long id, MultipartFile personnelFile, MultipartFile schemeFile) {
    scopeService.requireOrgOperator();
    TrainThreeYearPlanEntity entity = getExisting(id);
    Integer currentStatus = entity.getStatus();
    if (currentStatus == null || (currentStatus != 2 && currentStatus != 5)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "仅「分析完成请审核」或「已驳回」状态的规划可提交企业审核");
    }
    if (personnelFile == null || personnelFile.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传企业审核人员名单");
    }
    if (schemeFile == null || schemeFile.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传方案文档");
    }
    assertSubmitFileExtension(personnelFile, true);
    assertSubmitFileExtension(schemeFile, false);
    ensureLocalDataReady(entity);
    refreshDistributionSnapshotFromLocal(entity);
    long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, id)
        .eq(TrainThreeYearPlanItemEntity::getTenantId, entity.getTenantId()));
    if (itemCount <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无规划明细，请打开详情等待数据加载或手动同步");
    }
    FileVO personnelStored = fileApplicationService.upload(personnelFile, "private");
    FileVO schemeStored = fileApplicationService.upload(schemeFile, "private");
    byte[] reviewedExcel = buildReviewedPlanExcelBytes(entity);
    FileVO reviewedFile = fileApplicationService.storeFromBytes(
        reviewedExcel,
        entity.getPlanNo() + "-已审核三年培训规划表.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "private");
    entity.setSubmitPersonnelFileId(personnelStored.id());
    entity.setSubmitSchemeFileId(schemeStored.id());
    entity.setReviewedPlanFileId(reviewedFile.id());
    // 机构重新提交时清空企业当前回传指针（历史保留）
    entity.setEnterpriseRosterFileId(null);
    entity.setEnterpriseShiftFileId(null);
    entity.setEnterpriseRosterFileName(null);
    entity.setEnterpriseShiftFileName(null);
    entity.setEnterpriseReturnTime(null);
    entity.setEnterpriseReturnUserId(null);
    entity.setEnterpriseReturnUsername(null);
    entity.setStatus(3);
    entity.setRejectReason(null);
    appendSubmitHistory(entity, personnelStored, schemeStored);
    planMapper.updateById(entity);
    return toVO(entity, null);
  }

  private void assertSubmitFileExtension(MultipartFile file, boolean personnelOnly) {
    String name = file.getOriginalFilename();
    if (!StringUtils.hasText(name)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名无效");
    }
    String lower = name.toLowerCase();
    if (personnelOnly) {
      if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "人员名单请上传 Excel 文件（.xlsx / .xls）");
      }
      return;
    }
    if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")
        && !lower.endsWith(".docx") && !lower.endsWith(".doc")
        && !lower.endsWith(".pdf")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "方案文档支持 Excel、Word 或 PDF 格式");
    }
  }

  /** 追加一条驳回历史到 reject_history_json */
  private void appendRejectHistory(TrainThreeYearPlanEntity entity, String reason) {
    List<Map<String, Object>> history = parseHistoryList(entity.getRejectHistoryJson());
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("reason", reason);
    item.put("rejectedBy", SecurityContext.currentUser().getUserId());
    item.put("rejectedByUsername", SecurityContext.currentUser().getDisplayName());
    item.put("rejectedAt", LocalDateTime.now().toString());
    history.add(item);
    entity.setRejectHistoryJson(writeHistoryJson(history));
  }

  /** 追加一条提交历史到 submit_history_json */
  private void appendSubmitHistory(TrainThreeYearPlanEntity entity, FileVO personnelFile, FileVO schemeFile) {
    List<Map<String, Object>> history = parseHistoryList(entity.getSubmitHistoryJson());
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("submittedBy", SecurityContext.currentUser().getUserId());
    item.put("submittedByUsername", SecurityContext.currentUser().getDisplayName());
    item.put("submittedAt", LocalDateTime.now().toString());
    item.put("personnelFileName", personnelFile != null ? personnelFile.originalName() : null);
    item.put("schemeFileName", schemeFile != null ? schemeFile.originalName() : null);
    history.add(item);
    entity.setSubmitHistoryJson(writeHistoryJson(history));
  }

  private List<Map<String, Object>> parseHistoryList(String json) {
    if (!StringUtils.hasText(json)) {
      return new ArrayList<>();
    }
    try {
      List<Map<String, Object>> parsed = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
      return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
    } catch (Exception e) {
      log.warn("解析历史 JSON 失败，将重置：{}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private String writeHistoryJson(List<Map<String, Object>> history) {
    try {
      return objectMapper.writeValueAsString(history);
    } catch (Exception e) {
      log.warn("序列化历史 JSON 失败：{}", e.getMessage());
      return null;
    }
  }

  @Transactional
  public TrainThreeYearPlanVO uploadEnterpriseReturn(
      Long id, MultipartFile rosterFile, MultipartFile shiftFile) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertEnterpriseCanUploadReturn(entity);
    boolean hasRoster = rosterFile != null && !rosterFile.isEmpty();
    boolean hasShift = shiftFile != null && !shiftFile.isEmpty();
    if (!hasRoster && !hasShift) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少上传筛选名单或排班表其中一份");
    }
    if (hasRoster) {
      assertExcelExtension(rosterFile, "筛选名单");
      FileVO stored = fileApplicationService.upload(rosterFile, "private");
      entity.setEnterpriseRosterFileId(stored.id());
      entity.setEnterpriseRosterFileName(stored.originalName());
    }
    if (hasShift) {
      assertExcelExtension(shiftFile, "排班表");
      FileVO stored = fileApplicationService.upload(shiftFile, "private");
      entity.setEnterpriseShiftFileId(stored.id());
      entity.setEnterpriseShiftFileName(stored.originalName());
    }
    entity.setEnterpriseReturnTime(LocalDateTime.now());
    entity.setEnterpriseReturnUserId(SecurityContext.currentUser().getUserId());
    entity.setEnterpriseReturnUsername(
        StringUtils.hasText(SecurityContext.currentUser().getDisplayName())
            ? SecurityContext.currentUser().getDisplayName()
            : SecurityContext.currentUser().getUsername());
    appendEnterpriseReturnHistory(entity);
    planMapper.updateById(entity);
    return toVO(entity, null);
  }

  private void assertExcelExtension(MultipartFile file, String label) {
    String name = file.getOriginalFilename();
    if (!StringUtils.hasText(name)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "文件名无效");
    }
    String lower = name.toLowerCase();
    if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "请上传 Excel 文件（.xlsx / .xls）");
    }
  }

  private void appendEnterpriseReturnHistory(TrainThreeYearPlanEntity entity) {
    List<Map<String, Object>> history = parseHistoryList(entity.getEnterpriseReturnHistoryJson());
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("rosterFileId", entity.getEnterpriseRosterFileId());
    item.put("rosterFileName", entity.getEnterpriseRosterFileName());
    item.put("shiftFileId", entity.getEnterpriseShiftFileId());
    item.put("shiftFileName", entity.getEnterpriseShiftFileName());
    item.put("uploadedBy", SecurityContext.currentUser().getUserId());
    item.put("uploadedByUsername", entity.getEnterpriseReturnUsername());
    item.put("uploadedAt", LocalDateTime.now().toString());
    item.put("remark", null);
    history.add(item);
    entity.setEnterpriseReturnHistoryJson(writeHistoryJson(history));
  }

  @Transactional
  public TrainThreeYearPlanVO companyConfirm(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertEnterpriseCanConfirm(entity);
    // 确认与回传完备度解耦：未齐套也可确认，后续在 status=4 补交
    entity.setStatus(4);
    entity.setCompanyConfirmTime(LocalDateTime.now());
    entity.setCompanyConfirmUserId(SecurityContext.currentUser().getUserId());
    entity.setCompanyConfirmUsername(SecurityContext.currentUser().getUsername());
    entity.setRejectReason(null);
    planMapper.updateById(entity);
    return toVO(entity, null);
  }

  @Transactional
  public TrainThreeYearPlanVO companyReject(Long id, String reason) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertEnterpriseCanConfirm(entity);
    if (!StringUtils.hasText(reason)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写驳回理由");
    }
    entity.setStatus(5);
    entity.setRejectReason(reason.trim());
    appendRejectHistory(entity, reason.trim());
    planMapper.updateById(entity);
    return toVO(entity, null);
  }

  @Transactional
  public void delete(Long id) {
    scopeService.requireOrgOperator();
    TrainThreeYearPlanEntity entity = getExisting(id);
    if (entity.getStatus() != null && entity.getStatus() == 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "待机构审核的规划不可删除");
    }
    planMapper.deleteById(entity.getId());
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainThreeYearPlanItemVO> pageItems(Long planId, TrainThreeYearPlanItemQueryReq req) {
    TrainThreeYearPlanEntity plan = getExisting(planId);
    assertAccessible(plan);

    LambdaQueryWrapper<TrainThreeYearPlanItemEntity> wrapper = new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, planId)
        .eq(TrainThreeYearPlanItemEntity::getTenantId, plan.getTenantId())
        .orderByAsc(TrainThreeYearPlanItemEntity::getPlanYear)
        .orderByAsc(TrainThreeYearPlanItemEntity::getRowIndex);
    if (req.planYear() != null) {
      wrapper.eq(TrainThreeYearPlanItemEntity::getPlanYear, req.planYear());
    }
    if (StringUtils.hasText(req.jobName())) {
      wrapper.like(TrainThreeYearPlanItemEntity::getJobName, req.jobName());
    }

    Page<TrainThreeYearPlanItemEntity> page = new Page<>(req.page(), req.pageSize());
    Page<TrainThreeYearPlanItemEntity> result = itemMapper.selectPage(page, wrapper);
    if (result.getTotal() == 0 && canPreviewFromRencai(plan)) {
      return previewEmployeesFromRencai(plan, req);
    }
    List<TrainThreeYearPlanItemVO> list = result.getRecords().stream().map(this::toItemVO).toList();
    return new PageResponse<>(list, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public List<TrainThreeYearPlanMatrixVO> listMatrix(Long planId) {
    TrainThreeYearPlanEntity plan = getExisting(planId);
    assertAccessible(plan);
    List<TrainThreeYearPlanMatrixEntity> rows = matrixMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanMatrixEntity>()
            .eq(TrainThreeYearPlanMatrixEntity::getPlanId, planId)
            .eq(TrainThreeYearPlanMatrixEntity::getTenantId, plan.getTenantId())
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getPlanYear)
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getJobName)
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getLevelName));
    if (rows.isEmpty() && canPreviewFromRencai(plan)) {
      return previewMatrixFromRencai(plan);
    }
    return rows.stream().map(this::toMatrixVO).toList();
  }

  @Transactional(readOnly = true)
  public List<TrainThreeYearPlanDistributionVO> getReviewDistribution(Long planId) {
    TrainThreeYearPlanEntity plan = getExisting(planId);
    assertAccessible(plan);
    if (plan.getStatus() == null || plan.getStatus() < 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分析尚未完成");
    }
    List<TrainThreeYearPlanDistributionVO> cached = parseDistributionSnapshot(plan.getDistributionSnapshot());
    if (!cached.isEmpty()) {
      return cached;
    }
    if (plan.getRencaiTaskId() == null) {
      return List.of();
    }
    return fetchDistributionFromRencai(plan);
  }

  private void ensureLocalDataReady(TrainThreeYearPlanEntity entity) {
    if (entity.getStatus() == null || entity.getStatus() < 2 || entity.getRencaiTaskId() == null) {
      return;
    }
    long itemCount = countLocalItems(entity);
    if (itemCount == 0) {
      try {
        syncRencaiDataToLocal(entity);
        itemCount = countLocalItems(entity);
      } catch (Exception e) {
        log.warn("Sync rencai data to local failed for plan {}: {}", entity.getId(), e.getMessage());
      }
      if (itemCount == 0 && countRencaiEmployeeTotal(entity) > 0) {
        try {
          syncRencaiDataToLocal(entity);
        } catch (Exception e) {
          log.warn("Retry sync rencai data failed for plan {}: {}", entity.getId(), e.getMessage());
        }
      }
    } else if (!StringUtils.hasText(entity.getDistributionSnapshot())) {
      try {
        syncDistributionSnapshotFromRencai(entity);
        planMapper.updateById(entity);
      } catch (Exception e) {
        log.warn("Sync distribution snapshot failed for plan {}: {}", entity.getId(), e.getMessage());
      }
    }
  }

  private long countLocalItems(TrainThreeYearPlanEntity entity) {
    return itemMapper.selectCount(new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, entity.getId())
        .eq(TrainThreeYearPlanItemEntity::getTenantId, entity.getTenantId()));
  }

  private int countRencaiEmployeeTotal(TrainThreeYearPlanEntity entity) {
    if (entity.getRencaiTaskId() == null) {
      return 0;
    }
    try {
      JsonNode resp = rencaishibieClient.listEmployees(
          entity.getRencaiTaskId(), 1, 1, entity.getTenantId(), entity.getId());
      return resp.path("total").asInt(0);
    } catch (Exception e) {
      log.warn("Count rencai employees failed for plan {}: {}", entity.getId(), e.getMessage());
      return 0;
    }
  }

  private PageResponse<TrainThreeYearPlanItemVO> pageItemsFromDb(
      TrainThreeYearPlanEntity plan,
      TrainThreeYearPlanItemQueryReq req) {
    LambdaQueryWrapper<TrainThreeYearPlanItemEntity> wrapper = new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, plan.getId())
        .eq(TrainThreeYearPlanItemEntity::getTenantId, plan.getTenantId())
        .orderByAsc(TrainThreeYearPlanItemEntity::getPlanYear)
        .orderByAsc(TrainThreeYearPlanItemEntity::getRowIndex);
    if (req.planYear() != null) {
      wrapper.eq(TrainThreeYearPlanItemEntity::getPlanYear, req.planYear());
    }
    if (StringUtils.hasText(req.jobName())) {
      wrapper.like(TrainThreeYearPlanItemEntity::getJobName, req.jobName());
    }
    Page<TrainThreeYearPlanItemEntity> page = new Page<>(req.page(), req.pageSize());
    Page<TrainThreeYearPlanItemEntity> result = itemMapper.selectPage(page, wrapper);
    List<TrainThreeYearPlanItemVO> list = result.getRecords().stream().map(this::toItemVO).toList();
    return new PageResponse<>(list, result.getCurrent(), result.getSize(), result.getTotal());
  }

  private List<TrainThreeYearPlanMatrixVO> listMatrixFromDb(TrainThreeYearPlanEntity plan) {
    List<TrainThreeYearPlanMatrixEntity> rows = matrixMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanMatrixEntity>()
            .eq(TrainThreeYearPlanMatrixEntity::getPlanId, plan.getId())
            .eq(TrainThreeYearPlanMatrixEntity::getTenantId, plan.getTenantId())
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getPlanYear)
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getJobName)
            .orderByAsc(TrainThreeYearPlanMatrixEntity::getLevelName));
    return rows.stream().map(this::toMatrixVO).toList();
  }

  private List<TrainThreeYearPlanDistributionVO> loadDistribution(TrainThreeYearPlanEntity plan) {
    List<TrainThreeYearPlanDistributionVO> cached = parseDistributionSnapshot(plan.getDistributionSnapshot());
    if (!cached.isEmpty()) {
      return cached;
    }
    if (plan.getRencaiTaskId() == null) {
      return List.of();
    }
    List<TrainThreeYearPlanDistributionVO> distribution = fetchDistributionFromRencai(plan);
    saveDistributionSnapshot(plan, distribution);
    planMapper.updateById(plan);
    return distribution;
  }

  private List<TrainThreeYearPlanDistributionVO> fetchDistributionFromRencai(TrainThreeYearPlanEntity plan) {
    JsonNode distribution = rencaishibieClient.getDistribution(
        plan.getRencaiTaskId(), plan.getTenantId(), plan.getId());
    return parseDistributionFromRencai(distribution);
  }

  private void syncDistributionSnapshotFromRencai(TrainThreeYearPlanEntity entity) {
    if (entity.getRencaiTaskId() == null) {
      return;
    }
    try {
      List<TrainThreeYearPlanDistributionVO> distribution = fetchDistributionFromRencai(entity);
      saveDistributionSnapshot(entity, distribution);
    } catch (Exception ignored) {
      refreshDistributionSnapshotFromLocal(entity);
    }
  }

  private void refreshDistributionSnapshotFromLocal(TrainThreeYearPlanEntity plan) {
    Map<String, Integer> thresholds = new HashMap<>();
    for (TrainThreeYearPlanDistributionVO row : parseDistributionSnapshot(plan.getDistributionSnapshot())) {
      thresholds.put(distributionKey(row.jobName(), row.levelName()), row.thresholdMin());
    }
    List<TrainThreeYearPlanDistributionVO> distribution = computeDistributionFromLocalItems(plan, thresholds);
    saveDistributionSnapshot(plan, distribution);
    planMapper.updateById(plan);
  }

  private List<TrainThreeYearPlanDistributionVO> computeDistributionFromLocalItems(
      TrainThreeYearPlanEntity plan,
      Map<String, Integer> thresholds) {
    List<TrainThreeYearPlanItemEntity> items = itemMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
            .eq(TrainThreeYearPlanItemEntity::getPlanId, plan.getId())
            .eq(TrainThreeYearPlanItemEntity::getTenantId, plan.getTenantId()));
    Map<String, Integer> counter = new LinkedHashMap<>();
    for (TrainThreeYearPlanItemEntity item : items) {
      if (!StringUtils.hasText(item.getJobName()) || !StringUtils.hasText(item.getLevelName())) {
        continue;
      }
      String key = distributionKey(item.getJobName(), item.getLevelName());
      counter.merge(key, 1, Integer::sum);
    }
    List<TrainThreeYearPlanDistributionVO> distribution = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : counter.entrySet()) {
      String[] parts = entry.getKey().split("\u0001", -1);
      int threshold = thresholds.getOrDefault(entry.getKey(), 30);
      int count = entry.getValue();
      distribution.add(new TrainThreeYearPlanDistributionVO(
          parts[0],
          parts[1],
          count,
          threshold,
          count >= threshold,
          count >= threshold ? null : "未达开班阈值"
      ));
    }
    return distribution;
  }

  private void saveDistributionSnapshot(
      TrainThreeYearPlanEntity plan,
      List<TrainThreeYearPlanDistributionVO> distribution) {
    try {
      plan.setDistributionSnapshot(objectMapper.writeValueAsString(distribution));
    } catch (Exception e) {
      plan.setDistributionSnapshot(null);
    }
  }

  private List<TrainThreeYearPlanDistributionVO> parseDistributionSnapshot(String snapshot) {
    if (!StringUtils.hasText(snapshot)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(snapshot, new TypeReference<List<TrainThreeYearPlanDistributionVO>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<TrainThreeYearPlanDistributionVO> parseDistributionFromRencai(JsonNode distribution) {
    List<TrainThreeYearPlanDistributionVO> list = new ArrayList<>();
    JsonNode jobs = distribution.path("jobs");
    if (!jobs.isArray()) {
      return list;
    }
    for (JsonNode job : jobs) {
      String jobName = job.path("job_name").asText("");
      JsonNode levels = job.path("levels");
      if (!levels.isArray()) {
        continue;
      }
      for (JsonNode level : levels) {
        list.add(new TrainThreeYearPlanDistributionVO(
            jobName,
            level.path("level_name").asText(""),
            level.path("employee_count").asInt(0),
            level.path("threshold_min").asInt(0),
            level.path("meets_threshold").asBoolean(false),
            textOrNull(level, "warning")
        ));
      }
    }
    return list;
  }

  private String distributionKey(String jobName, String levelName) {
    return jobName + "\u0001" + levelName;
  }

  private boolean canPreviewFromRencai(TrainThreeYearPlanEntity plan) {
    return plan.getRencaiTaskId() != null
        && plan.getStatus() != null
        && plan.getStatus() >= 2;
  }

  private PageResponse<TrainThreeYearPlanItemVO> previewEmployeesFromRencai(
      TrainThreeYearPlanEntity plan,
      TrainThreeYearPlanItemQueryReq req) {
    List<TrainThreeYearPlanItemVO> all = new ArrayList<>();
    int page = 1;
    int pageSize = 200;
    while (true) {
      JsonNode resp = rencaishibieClient.listEmployees(
          plan.getRencaiTaskId(), page, pageSize, plan.getTenantId(), plan.getId());
      JsonNode items = resp.path("items");
      if (!items.isArray() || items.isEmpty()) {
        break;
      }
      for (JsonNode item : items) {
        TrainThreeYearPlanItemVO row = toPreviewItemVO(plan.getId(), item);
        if (req.planYear() != null && row.planYear() != null && !req.planYear().equals(row.planYear())) {
          continue;
        }
        if (StringUtils.hasText(req.jobName())
            && (row.jobName() == null || !row.jobName().contains(req.jobName()))) {
          continue;
        }
        all.add(row);
      }
      int total = resp.path("total").asInt(0);
      if (page * pageSize >= total) {
        break;
      }
      page++;
    }
    int from = (req.page() - 1) * req.pageSize();
    if (from >= all.size()) {
      return new PageResponse<>(List.of(), req.page(), req.pageSize(), all.size());
    }
    int to = Math.min(from + req.pageSize(), all.size());
    return new PageResponse<>(all.subList(from, to), req.page(), req.pageSize(), all.size());
  }

  private List<TrainThreeYearPlanMatrixVO> previewMatrixFromRencai(TrainThreeYearPlanEntity plan) {
    List<TrainThreeYearPlanItemVO> all = new ArrayList<>();
    int page = 1;
    int pageSize = 200;
    while (true) {
      JsonNode resp = rencaishibieClient.listEmployees(
          plan.getRencaiTaskId(), page, pageSize, plan.getTenantId(), plan.getId());
      JsonNode items = resp.path("items");
      if (!items.isArray() || items.isEmpty()) {
        break;
      }
      for (JsonNode item : items) {
        if (!StringUtils.hasText(item.path("determined_job").asText(""))) {
          continue;
        }
        all.add(toPreviewItemVO(plan.getId(), item));
      }
      int total = resp.path("total").asInt(0);
      if (page * pageSize >= total) {
        break;
      }
      page++;
    }
    Map<String, Integer> counter = new HashMap<>();
    long syntheticId = 1;
    List<TrainThreeYearPlanMatrixVO> matrix = new ArrayList<>();
    for (TrainThreeYearPlanItemVO item : all) {
      if (item.planYear() == null || !StringUtils.hasText(item.jobName())) {
        continue;
      }
      String mode = StringUtils.hasText(item.trainMode()) ? item.trainMode() : "";
      String level = StringUtils.hasText(item.levelName()) ? item.levelName() : "";
      String key = item.jobName() + "|" + item.planYear() + "|" + mode + "|" + level;
      counter.merge(key, 1, Integer::sum);
    }
    for (Map.Entry<String, Integer> entry : counter.entrySet()) {
      String[] parts = entry.getKey().split("\\|", -1);
      matrix.add(new TrainThreeYearPlanMatrixVO(
          syntheticId++,
          parts[0],
          Integer.parseInt(parts[1]),
          parts[2],
          parts[3],
          entry.getValue()
      ));
    }
    matrix.sort((a, b) -> {
      int year = Integer.compare(a.planYear(), b.planYear());
      if (year != 0) {
        return year;
      }
      return a.jobName().compareTo(b.jobName());
    });
    return matrix;
  }

  private TrainThreeYearPlanItemVO toPreviewItemVO(Long planId, JsonNode item) {
    String eligibleDate = textOrNull(item, "eligible_date");
    return new TrainThreeYearPlanItemVO(
        (long) item.path("row_index").asInt(0),
        planId,
        item.path("name").asText(""),
        null,
        item.path("company").asText(""),
        item.path("determined_job").asText(""),
        item.path("determined_level").asText(""),
        null,
        parsePlanYear(eligibleDate),
        null,
        null,
        eligibleDate,
        item.path("row_index").asInt(0),
        "preview"
    );
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> downloadPlanFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getRencaiTaskId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未生成三年规划文件");
    }
    byte[] bytes = rencaishibieClient.downloadPlan(
        entity.getRencaiTaskId(), entity.getTenantId(), entity.getId());
    String filename = entity.getPlanNo() + "-原始三年培训规划表.xlsx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + urlEncode(filename))
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public ResponseEntity<byte[]> downloadReviewedPlanFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getStatus() == null || entity.getStatus() < 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分析尚未完成，无法下载已审核规划表");
    }
    byte[] bytes;
    if (entity.getStatus() >= 3 && entity.getReviewedPlanFileId() != null) {
      bytes = readStoredFileBytes(entity.getReviewedPlanFileId(), entity, true);
    } else {
      ensureLocalDataReady(entity);
      bytes = buildReviewedPlanExcelBytes(entity);
    }
    String filename = entity.getPlanNo() + "-已审核三年培训规划表.xlsx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + urlEncode(filename))
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private byte[] buildReviewedPlanExcelBytes(TrainThreeYearPlanEntity entity) {
    OrgEntity org = orgMapper.selectById(entity.getTargetOrgId());
    List<TrainThreeYearPlanMatrixEntity> matrixRows = matrixMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanMatrixEntity>()
            .eq(TrainThreeYearPlanMatrixEntity::getPlanId, entity.getId())
            .eq(TrainThreeYearPlanMatrixEntity::getTenantId, entity.getTenantId()));
    List<TrainThreeYearPlanItemEntity> items = itemMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
            .eq(TrainThreeYearPlanItemEntity::getPlanId, entity.getId())
            .eq(TrainThreeYearPlanItemEntity::getTenantId, entity.getTenantId())
            .orderByAsc(TrainThreeYearPlanItemEntity::getPlanYear)
            .orderByAsc(TrainThreeYearPlanItemEntity::getRowIndex));
    List<TrainThreeYearPlanDistributionVO> distribution = listDistributionForExport(entity);
    return excelExporter.exportReviewedPlan(entity, org, matrixRows, items, distribution);
  }

  private List<TrainThreeYearPlanDistributionVO> listDistributionForExport(TrainThreeYearPlanEntity plan) {
    List<TrainThreeYearPlanDistributionVO> cached = parseDistributionSnapshot(plan.getDistributionSnapshot());
    if (!cached.isEmpty()) {
      return cached;
    }
    return computeDistributionFromLocalItems(plan, Map.of());
  }

  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public ResponseEntity<byte[]> downloadSubmitPersonnelFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getStatus() == null || entity.getStatus() < 3 || entity.getSubmitPersonnelFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未提交企业审核人员名单");
    }
    return downloadStoredAttachment(entity.getSubmitPersonnelFileId(), entity.getPlanNo() + "-企业审核人员名单");
  }

  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public ResponseEntity<byte[]> downloadSubmitSchemeFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getStatus() == null || entity.getStatus() < 3 || entity.getSubmitSchemeFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未提交方案文档");
    }
    return downloadStoredAttachment(entity.getSubmitSchemeFileId(), entity.getPlanNo() + "-方案文档");
  }

  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public ResponseEntity<byte[]> downloadEnterpriseRosterFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getEnterpriseRosterFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未回传筛选培训名单");
    }
    return downloadStoredAttachment(
        entity.getEnterpriseRosterFileId(),
        entity.getPlanNo() + "-企业筛选培训名单");
  }

  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public ResponseEntity<byte[]> downloadEnterpriseShiftFile(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未回传排班表");
    }
    return downloadStoredAttachment(
        entity.getEnterpriseShiftFileId(),
        entity.getPlanNo() + "-企业排班表");
  }

  private ResponseEntity<byte[]> downloadStoredAttachment(Long fileId, String fallbackName) {
    FileVO file = fileApplicationService.getById(fileId);
    byte[] bytes = readStoredFileBytes(fileId, null, false);
    String filename = StringUtils.hasText(file.originalName()) ? file.originalName() : fallbackName;
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (StringUtils.hasText(file.contentType())) {
      try {
        mediaType = MediaType.parseMediaType(file.contentType());
      } catch (Exception ignored) {
        // keep default
      }
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + urlEncode(filename))
        .contentType(mediaType)
        .body(bytes);
  }

  /**
   * @param planForFallback 已审核规划表读取失败时用于实时生成的方案实体（仅 reviewed 场景传入）
   * @param fallbackToGeneratedReviewedPlan 读取固化文件失败时是否回退为实时生成已审核规划表
   */
  private byte[] readStoredFileBytes(
      Long fileId,
      TrainThreeYearPlanEntity planForFallback,
      boolean fallbackToGeneratedReviewedPlan) {
    try {
      return fileApplicationService.readFileBytesById(fileId);
    } catch (BusinessException ex) {
      if (fallbackToGeneratedReviewedPlan && planForFallback != null) {
        log.warn("读取固化已审核规划表失败(fileId={})，回退实时生成: {}", fileId, ex.getMessage());
        ensureLocalDataReady(planForFallback);
        return buildReviewedPlanExcelBytes(planForFallback);
      }
      throw ex;
    } catch (Exception e) {
      if (fallbackToGeneratedReviewedPlan && planForFallback != null) {
        log.warn("读取固化已审核规划表异常(fileId={})，回退实时生成", fileId, e);
        ensureLocalDataReady(planForFallback);
        return buildReviewedPlanExcelBytes(planForFallback);
      }
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败");
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> downloadResultZip(Long id) {
    TrainThreeYearPlanEntity entity = getExisting(id);
    assertAccessible(entity);
    if (entity.getRencaiTaskId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "尚未生成认定结果");
    }
    byte[] bytes = rencaishibieClient.downloadZip(
        entity.getRencaiTaskId(), entity.getTenantId(), entity.getId());
    String filename = entity.getPlanNo() + "-认定结果和三年规划.zip";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + urlEncode(filename))
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(bytes);
  }

  private void syncItemsFromRencai(TrainThreeYearPlanEntity entity, Long taskId) {
    int page = 1;
    int pageSize = 200;
    while (true) {
      JsonNode resp = rencaishibieClient.listEmployees(taskId, page, pageSize, entity.getTenantId(), entity.getId());
      JsonNode items = resp.path("items");
      if (!items.isArray() || items.isEmpty()) {
        break;
      }
      for (JsonNode item : items) {
        if (!StringUtils.hasText(item.path("determined_job").asText(""))) {
          continue;
        }
        TrainThreeYearPlanItemEntity row = new TrainThreeYearPlanItemEntity();
        row.setPlanId(entity.getId());
        row.setTenantId(entity.getTenantId());
        row.setPersonName(item.path("name").asText(""));
        row.setCompanyName(item.path("company").asText(""));
        row.setJobName(item.path("determined_job").asText(""));
        row.setLevelName(item.path("determined_level").asText(""));
        row.setEligibleDate(textOrNull(item, "eligible_date"));
        row.setPlanYear(parsePlanYear(row.getEligibleDate()));
        row.setRowIndex(item.path("row_index").asInt(0));
        row.setSource("engine");
        itemMapper.insert(row);
      }
      int total = resp.path("total").asInt(0);
      if (page * pageSize >= total) {
        break;
      }
      page++;
    }
  }

  private void syncMatrixFromItems(TrainThreeYearPlanEntity entity) {
    List<TrainThreeYearPlanItemEntity> items = itemMapper.selectList(
        new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
            .eq(TrainThreeYearPlanItemEntity::getPlanId, entity.getId())
            .eq(TrainThreeYearPlanItemEntity::getTenantId, entity.getTenantId()));
    Map<String, Integer> counter = new HashMap<>();
    for (TrainThreeYearPlanItemEntity item : items) {
      if (item.getPlanYear() == null || !StringUtils.hasText(item.getJobName())) {
        continue;
      }
      String mode = StringUtils.hasText(item.getTrainMode()) ? item.getTrainMode() : "";
      String level = StringUtils.hasText(item.getLevelName()) ? item.getLevelName() : "";
      String key = item.getJobName() + "|" + item.getPlanYear() + "|" + mode + "|" + level;
      counter.merge(key, 1, Integer::sum);
    }
    for (Map.Entry<String, Integer> entry : counter.entrySet()) {
      String[] parts = entry.getKey().split("\\|", -1);
      TrainThreeYearPlanMatrixEntity row = new TrainThreeYearPlanMatrixEntity();
      row.setPlanId(entity.getId());
      row.setTenantId(entity.getTenantId());
      row.setJobName(parts[0]);
      row.setPlanYear(Integer.parseInt(parts[1]));
      row.setTrainMode(parts[2]);
      row.setLevelName(parts[3]);
      row.setHeadcount(entry.getValue());
      matrixMapper.insert(row);
    }
  }

  private JsonNode syncAnalysisStatusFromRencai(TrainThreeYearPlanEntity entity) {
    if (entity.getRencaiTaskId() == null || entity.getStatus() == null || entity.getStatus() != 1) {
      return null;
    }
    try {
      JsonNode status = rencaishibieClient.getAnalysisStatus(
          entity.getRencaiTaskId(), entity.getTenantId(), entity.getId());
      applyRencaiStatusToEntity(entity, status);
      return status;
    } catch (Exception e) {
      return null;
    }
  }

  private void applyRencaiStatusToEntity(TrainThreeYearPlanEntity entity, JsonNode status) {
    String rencaiStatus = status.path("status").asText("");
    entity.setAnalysisStatus(mapAnalysisStatus(rencaiStatus));
    if (TERMINAL_RENCAI_STATUS.contains(rencaiStatus) && entity.getStatus() != null && entity.getStatus() == 1) {
      if ("FAILED".equals(rencaiStatus)) {
        entity.setStatus(0);
        planMapper.updateById(entity);
        return;
      }
      // 共库模式下 Python 可能已同库落库并置 status=2，此处仅作补偿
      long existingItems = countLocalItems(entity);
      if (existingItems > 0) {
        entity.setStatus(2);
        entity.setAnalysisStatus("done");
        planMapper.updateById(entity);
        return;
      }
      int recommended = status.path("recommended_rows").asInt(0);
      int rencaiTotal = countRencaiEmployeeTotal(entity);
      if (rencaiTotal == 0 && recommended > 0) {
        // rencaishibie 已标记完成但明细尚未入库，保持分析中并继续轮询
        entity.setAnalysisStatus("syncing");
        planMapper.updateById(entity);
        return;
      }
      syncRencaiDataToLocal(entity);
      long itemCount = countLocalItems(entity);
      if (itemCount == 0 && recommended > 0) {
        entity.setAnalysisStatus("syncing");
        planMapper.updateById(entity);
        return;
      }
      entity.setStatus(2);
      planMapper.updateById(entity);
      return;
    }
    planMapper.updateById(entity);
  }

  private TrainThreeYearPlanEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainThreeYearPlanEntity entity = planMapper.selectOne(
        new LambdaQueryWrapper<TrainThreeYearPlanEntity>()
            .eq(TrainThreeYearPlanEntity::getId, id)
            .eq(TrainThreeYearPlanEntity::getTenantId, tenantId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "三年规划不存在");
    }
    return entity;
  }

  private void assertAccessible(TrainThreeYearPlanEntity entity) {
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    if (companyFilter != null && !companyFilter.equals(entity.getTargetOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该三年规划");
    }
  }

  private void assertEnterpriseCanConfirm(TrainThreeYearPlanEntity entity) {
    assertAccessible(entity);
    if (entity.getStatus() == null || entity.getStatus() != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待企业审核状态的规划可确认或驳回");
    }
    assertEnterpriseActor(entity);
  }

  /** 待审(3) 上传或已确认(4) 补交/覆盖回传材料 */
  private void assertEnterpriseCanUploadReturn(TrainThreeYearPlanEntity entity) {
    assertAccessible(entity);
    Integer status = entity.getStatus();
    if (status == null || (status != 3 && status != 4)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待企业审核或已确认状态可上传/补交回传材料");
    }
    assertEnterpriseActor(entity);
  }

  private void assertEnterpriseActor(TrainThreeYearPlanEntity entity) {
    TrainingOperatorScopeService.Scope scope = scopeService.currentScope();
    if (!scope.isEnterpriseViewer()) {
      scopeService.requireOrgOperator();
      return;
    }
    if (scope.orgId() == null || !scope.orgId().equals(entity.getTargetOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅目标企业可操作该规划回传");
    }
  }

  private JsonNode safeAnalysisStatus(TrainThreeYearPlanEntity entity) {
    if (entity.getRencaiTaskId() == null) {
      return null;
    }
    try {
      return rencaishibieClient.getAnalysisStatus(
          entity.getRencaiTaskId(), entity.getTenantId(), entity.getId());
    } catch (Exception e) {
      return null;
    }
  }

  private TrainThreeYearPlanVO toVO(TrainThreeYearPlanEntity entity, JsonNode statusNode) {
    OrgEntity org = orgMapper.selectById(entity.getTargetOrgId());
    String orgName = org != null ? org.getName() : "";
    String researchTitle = null;
    if (entity.getResearchOrgId() != null) {
      TrainResearchOrgEntity research = researchOrgMapper.selectById(entity.getResearchOrgId());
      researchTitle = research != null ? research.getTitle() : null;
    }
    Integer totalRows = null;
    Integer recommendedRows = null;
    Double progressPct = null;
    String rencaiTaskStatus = null;
    if (statusNode != null) {
      totalRows = statusNode.path("total_rows").asInt(0);
      recommendedRows = statusNode.path("recommended_rows").asInt(0);
      progressPct = statusNode.path("progress_pct").asDouble(0);
      rencaiTaskStatus = statusNode.path("status").asText(null);
    }
    String statusLabel = STATUS_LABELS.getOrDefault(entity.getStatus(), "未知");
    if (entity.getStatus() != null && entity.getStatus() == 1 && rencaiTaskStatus != null
        && TERMINAL_RENCAI_STATUS.contains(rencaiTaskStatus) && !"FAILED".equals(rencaiTaskStatus)) {
      statusLabel = "同步分析结果";
      progressPct = null;
    } else if (entity.getStatus() != null && entity.getStatus() == 1
        && "syncing".equals(entity.getAnalysisStatus())) {
      statusLabel = "同步分析结果";
      progressPct = null;
    } else if (entity.getStatus() != null && entity.getStatus() == 1 && "PROCESSING".equals(rencaiTaskStatus)) {
      progressPct = progressPct == null ? 0.0 : Math.min(progressPct, 99.0);
    }
    List<TrainThreeYearPlanVO.RejectHistoryItem> rejectHistory = parseRejectHistory(entity.getRejectHistoryJson());
    List<TrainThreeYearPlanVO.SubmitHistoryItem> submitHistory = parseSubmitHistory(entity.getSubmitHistoryJson());
    List<TrainThreeYearPlanVO.EnterpriseReturnHistoryItem> returnHistory =
        parseEnterpriseReturnHistory(entity.getEnterpriseReturnHistoryJson());
    boolean returnReady = entity.getEnterpriseRosterFileId() != null && entity.getEnterpriseShiftFileId() != null;
    String rosterName = StringUtils.hasText(entity.getEnterpriseRosterFileName())
        ? entity.getEnterpriseRosterFileName()
        : resolveOriginalFileName(entity.getEnterpriseRosterFileId());
    String shiftName = StringUtils.hasText(entity.getEnterpriseShiftFileName())
        ? entity.getEnterpriseShiftFileName()
        : resolveOriginalFileName(entity.getEnterpriseShiftFileId());
    return new TrainThreeYearPlanVO(
        entity.getId(),
        entity.getPlanNo(),
        entity.getTitle(),
        entity.getTargetOrgId(),
        orgName,
        entity.getResearchOrgId(),
        researchTitle,
        entity.getRencaiTaskId(),
        entity.getRencaiInputFileId(),
        entity.getPlanYears(),
        entity.getStatus(),
        statusLabel,
        entity.getAnalysisStatus(),
        entity.getRejectReason(),
        rejectHistory,
        submitHistory,
        entity.getCompanyConfirmTime(),
        entity.getCompanyConfirmUsername(),
        entity.getCreatedBy(),
        entity.getCreateTime(),
        entity.getUpdateTime(),
        totalRows,
        recommendedRows,
        progressPct,
        rencaiTaskStatus,
        entity.getSubmitPersonnelFileId(),
        entity.getSubmitSchemeFileId(),
        resolveOriginalFileName(entity.getSubmitPersonnelFileId()),
        resolveOriginalFileName(entity.getSubmitSchemeFileId()),
        entity.getEnterpriseRosterFileId(),
        entity.getEnterpriseShiftFileId(),
        rosterName,
        shiftName,
        entity.getEnterpriseReturnTime(),
        entity.getEnterpriseReturnUsername(),
        returnReady,
        returnHistory
    );
  }

  private List<TrainThreeYearPlanVO.EnterpriseReturnHistoryItem> parseEnterpriseReturnHistory(String json) {
    List<Map<String, Object>> raw = parseHistoryList(json);
    List<TrainThreeYearPlanVO.EnterpriseReturnHistoryItem> result = new ArrayList<>();
    for (Map<String, Object> m : raw) {
      result.add(new TrainThreeYearPlanVO.EnterpriseReturnHistoryItem(
          asLong(m.get("rosterFileId")),
          asString(m.get("rosterFileName")),
          asLong(m.get("shiftFileId")),
          asString(m.get("shiftFileName")),
          asString(m.get("uploadedBy")),
          asString(m.get("uploadedByUsername")),
          asLocalDateTime(m.get("uploadedAt")),
          asString(m.get("remark"))
      ));
    }
    return result;
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private List<TrainThreeYearPlanVO.RejectHistoryItem> parseRejectHistory(String json) {
    List<Map<String, Object>> raw = parseHistoryList(json);
    List<TrainThreeYearPlanVO.RejectHistoryItem> result = new ArrayList<>();
    for (Map<String, Object> m : raw) {
      result.add(new TrainThreeYearPlanVO.RejectHistoryItem(
          asString(m.get("reason")),
          asString(m.get("rejectedBy")),
          asString(m.get("rejectedByUsername")),
          asLocalDateTime(m.get("rejectedAt"))
      ));
    }
    return result;
  }

  private List<TrainThreeYearPlanVO.SubmitHistoryItem> parseSubmitHistory(String json) {
    List<Map<String, Object>> raw = parseHistoryList(json);
    List<TrainThreeYearPlanVO.SubmitHistoryItem> result = new ArrayList<>();
    for (Map<String, Object> m : raw) {
      result.add(new TrainThreeYearPlanVO.SubmitHistoryItem(
          asString(m.get("submittedBy")),
          asString(m.get("submittedByUsername")),
          asLocalDateTime(m.get("submittedAt")),
          asString(m.get("personnelFileName")),
          asString(m.get("schemeFileName"))
      ));
    }
    return result;
  }

  private static String asString(Object v) {
    return v == null ? null : String.valueOf(v);
  }

  private static LocalDateTime asLocalDateTime(Object v) {
    if (v == null) return null;
    String s = String.valueOf(v);
    try {
      return LocalDateTime.parse(s);
    } catch (Exception e) {
      return null;
    }
  }

  private String resolveOriginalFileName(Long fileId) {
    if (fileId == null) {
      return null;
    }
    try {
      return fileApplicationService.get(fileId).originalName();
    } catch (Exception ex) {
      return null;
    }
  }

  private TrainThreeYearPlanItemVO toItemVO(TrainThreeYearPlanItemEntity entity) {
    return new TrainThreeYearPlanItemVO(
        entity.getId(),
        entity.getPlanId(),
        entity.getPersonName(),
        entity.getIdCard(),
        entity.getCompanyName(),
        entity.getJobName(),
        entity.getLevelName(),
        entity.getTrainMode(),
        entity.getPlanYear(),
        entity.getTrainYear(),
        entity.getCertYear(),
        entity.getEligibleDate(),
        entity.getRowIndex(),
        entity.getSource()
    );
  }

  private TrainThreeYearPlanMatrixVO toMatrixVO(TrainThreeYearPlanMatrixEntity entity) {
    return new TrainThreeYearPlanMatrixVO(
        entity.getId(),
        entity.getJobName(),
        entity.getPlanYear(),
        entity.getTrainMode(),
        entity.getLevelName(),
        entity.getHeadcount()
    );
  }

  private String generatePlanNo() {
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    int suffix = ThreadLocalRandom.current().nextInt(100, 999);
    return "TYP" + ts + suffix;
  }

  private String mapAnalysisStatus(String rencaiStatus) {
    if ("FAILED".equals(rencaiStatus)) {
      return "failed";
    }
    if ("COMPLETED".equals(rencaiStatus) || "PENDING_MANUAL".equals(rencaiStatus)) {
      return "done";
    }
    if ("PROCESSING".equals(rencaiStatus)) {
      return "running";
    }
    return "pending";
  }

  private Integer parsePlanYear(String eligibleDate) {
    if (!StringUtils.hasText(eligibleDate)) {
      return null;
    }
    String digits = eligibleDate.replaceAll("[^0-9]", "");
    if (digits.length() >= 4) {
      try {
        return Integer.parseInt(digits.substring(0, 4));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private String textOrNull(JsonNode node, String field) {
    String value = node.path(field).asText(null);
    return StringUtils.hasText(value) ? value : null;
  }

  private Object jsonValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.numberValue();
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isTextual()) {
      return node.asText();
    }
    return node.toString();
  }

  private String urlEncode(String value) {
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }
}
