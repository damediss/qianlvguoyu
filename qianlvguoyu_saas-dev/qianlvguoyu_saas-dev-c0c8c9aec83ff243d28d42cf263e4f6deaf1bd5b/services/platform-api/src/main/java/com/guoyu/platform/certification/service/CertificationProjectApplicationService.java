package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.CertificationDocumentReq;
import com.guoyu.platform.certification.dto.CertificationDocumentReviewReq;
import com.guoyu.platform.certification.dto.CertificationMemberReq;
import com.guoyu.platform.certification.dto.CertificationRiskReq;
import com.guoyu.platform.certification.dto.CertificationRiskStatusReq;
import com.guoyu.platform.certification.dto.CertificationScheduleItemReq;
import com.guoyu.platform.certification.dto.CertificationTaskCreateReq;
import com.guoyu.platform.certification.dto.CertificationTaskQueryReq;
import com.guoyu.platform.certification.dto.CertificationTaskUpdateReq;
import com.guoyu.platform.certification.dto.CertificationWbsItemReq;
import com.guoyu.platform.certification.entity.CertificationRiskEntity;
import com.guoyu.platform.certification.entity.CertificationScheduleItemEntity;
import com.guoyu.platform.certification.entity.CertificationTaskDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskMemberEntity;
import com.guoyu.platform.certification.entity.CertificationTaskPhaseEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.entity.CertificationWbsItemEntity;
import com.guoyu.platform.certification.mapper.CertificationCharterGoalMapper;
import com.guoyu.platform.certification.mapper.CertificationRiskMapper;
import com.guoyu.platform.certification.mapper.CertificationScheduleItemMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMemberMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.mapper.CertificationWbsItemMapper;
import com.guoyu.platform.certification.vo.CertificationDocumentWorkflowVO;
import com.guoyu.platform.certification.vo.CertificationTaskScopeVO;
import com.guoyu.platform.certification.vo.CertificationDocumentVO;
import com.guoyu.platform.certification.vo.CertificationMemberVO;
import com.guoyu.platform.certification.vo.CertificationPhaseVO;
import com.guoyu.platform.certification.vo.CertificationRiskVO;
import com.guoyu.platform.certification.vo.CertificationScheduleItemVO;
import com.guoyu.platform.certification.vo.CertificationTaskVO;
import com.guoyu.platform.certification.vo.CertificationWbsItemVO;
import com.guoyu.platform.certification.vo.CertificationWorkbenchVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.certification.workflow.CertificationDocumentWorkflowService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationProjectApplicationService {

  private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
  // 任务创建时默认阶段为“任务创建阶段”，状态为“进行中”
  private static final String DEFAULT_PHASE = CertificationTaskPhases.TASK_CREATION;
  private static final String DEFAULT_TASK_STATUS = CertificationTaskStatuses.IN_PROGRESS;
  private static final String DEFAULT_DOCUMENT_STATUS = "draft";
  private static final String STATUS_SUBMITTED = "submitted";
  private static final String STATUS_INITIAL_REVIEWING = "initial_reviewing";
  private static final String STATUS_INITIAL_REJECTED = "initial_rejected";
  private static final String STATUS_FINAL_REVIEWING = "final_reviewing";
  private static final String STATUS_FINAL_REJECTED = "final_rejected";
  private static final String STATUS_ARCHIVED = "archived";
  private static final Set<String> EDITABLE_DOCUMENT_STATUSES = Set.of(
      DEFAULT_DOCUMENT_STATUS,
      STATUS_INITIAL_REJECTED,
      STATUS_FINAL_REJECTED
  );
  private static final Set<String> REVIEWING_DOCUMENT_STATUSES = Set.of(
      STATUS_SUBMITTED,
      STATUS_INITIAL_REVIEWING,
      STATUS_FINAL_REVIEWING
  );

  private static final List<CertificationTaskPhaseSupport.PhaseSeed> PHASES = CertificationTaskPhaseSupport.PHASES;

  private static final List<DocumentSeed> DOCUMENTS = List.of(
      new DocumentSeed("charter", "任务章程"),
      new DocumentSeed("members", "项目成员"),
      new DocumentSeed("wbs", "项目WBS"),
      new DocumentSeed("schedule", "项目进度"),
      new DocumentSeed("risk", "风险管理")
  );

  private static final Map<String, Integer> RISK_SCORE = Map.of(
      "low", 1,
      "medium", 2,
      "high", 3
  );

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskPhaseMapper phaseMapper;
  private final CertificationTaskDocumentMapper documentMapper;
  private final CertificationCharterGoalMapper charterGoalMapper;
  private final CertificationTaskMemberMapper memberMapper;
  private final CertificationWbsItemMapper wbsMapper;
  private final CertificationScheduleItemMapper scheduleMapper;
  private final CertificationRiskMapper riskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationDocumentWorkflowService workflowService;

  public CertificationProjectApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      CertificationTaskDocumentMapper documentMapper,
      CertificationCharterGoalMapper charterGoalMapper,
      CertificationTaskMemberMapper memberMapper,
      CertificationWbsItemMapper wbsMapper,
      CertificationScheduleItemMapper scheduleMapper,
      CertificationRiskMapper riskMapper,
      CertificationTaskScopeMapper scopeMapper
  ) {
    this(taskMapper, phaseMapper, documentMapper, charterGoalMapper, memberMapper, wbsMapper, scheduleMapper,
        riskMapper, scopeMapper, null);
  }

  @Autowired
  public CertificationProjectApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      CertificationTaskDocumentMapper documentMapper,
      CertificationCharterGoalMapper charterGoalMapper,
      CertificationTaskMemberMapper memberMapper,
      CertificationWbsItemMapper wbsMapper,
      CertificationScheduleItemMapper scheduleMapper,
      CertificationRiskMapper riskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationDocumentWorkflowService workflowService
  ) {
    this.taskMapper = taskMapper;
    this.phaseMapper = phaseMapper;
    this.documentMapper = documentMapper;
    this.charterGoalMapper = charterGoalMapper;
    this.memberMapper = memberMapper;
    this.wbsMapper = wbsMapper;
    this.scheduleMapper = scheduleMapper;
    this.riskMapper = riskMapper;
    this.scopeMapper = scopeMapper;
    this.workflowService = workflowService;
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationTaskVO> pageTasks(CertificationTaskQueryReq req) {
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .orderByDesc(CertificationTaskEntity::getLastBusinessUpdatedAt)
        .orderByDesc(CertificationTaskEntity::getCreatedAt);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(CertificationTaskEntity::getTaskName, req.getKeyword())
          .or()
          .like(CertificationTaskEntity::getTaskNo, req.getKeyword())
          .or()
          .like(CertificationTaskEntity::getTaskManagerName, req.getKeyword())
          .or()
          .like(CertificationTaskEntity::getCompanyName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getTaskNo())) {
      wrapper.like(CertificationTaskEntity::getTaskNo, req.getTaskNo());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationTaskEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getCurrentPhase())) {
      wrapper.eq(CertificationTaskEntity::getCurrentPhase, req.getCurrentPhase());
    }
    if (StringUtils.hasText(req.getLevelCode())) {
      List<Long> taskIds = scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
              .eq(CertificationTaskScopeEntity::getLevelCode, req.getLevelCode()))
          .stream()
          .map(CertificationTaskScopeEntity::getTaskId)
          .filter(Objects::nonNull)
          .distinct()
          .toList();
      if (taskIds.isEmpty()) {
        return new PageResponse<>(List.of(), req.getPage(), req.getPageSize(), 0);
      }
      wrapper.in(CertificationTaskEntity::getId, taskIds);
    }

    Page<CertificationTaskEntity> page = taskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, List<CertificationTaskScopeVO>> scopeMap = loadScopes(page.getRecords().stream().map(CertificationTaskEntity::getId).filter(Objects::nonNull).toList());
    return new PageResponse<>(
        page.getRecords().stream()
            .map(task -> CertificationTaskVO.from(task, scopeMap.getOrDefault(task.getId(), List.of())))
            .toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public CertificationTaskVO getTask(Long taskId) {
    CertificationTaskEntity task = findTask(taskId);
    return CertificationTaskVO.from(task, listScopeVO(taskId));
  }

  @Transactional
  public CertificationTaskVO createTask(CertificationTaskCreateReq req) {
    validateTaskDates(req.planStartDate(), req.planEndDate(), req.examDate());
    CertificationTaskEntity entity = new CertificationTaskEntity();
    entity.setTaskNo(req.taskNo());
    applyTaskFields(entity, req.taskName(), req.taskManagerName(), req.companyName(), req.certificationType(),
        req.plannedPeopleCount(), req.planStartDate(),
        req.planEndDate(), req.examDate(), req.remark());
    entity.setSourceType(StringUtils.hasText(req.sourceType()) ? req.sourceType() : "manual");
    entity.setSourceName("manual".equals(entity.getSourceType()) ? "手动新增" : "任务订单");
    entity.setCurrentPhase(DEFAULT_PHASE);
    entity.setStatus(DEFAULT_TASK_STATUS);
    entity.setDocumentCompletedCount(0);
    entity.setDocumentTotalCount(DOCUMENTS.size());
    entity.setPhaseCompletedCount(0);
    entity.setPhaseTotalCount(PHASES.size());
    entity.setLastBusinessUpdatedAt(LocalDateTime.now());
    entity.setDeleted(0);
    taskMapper.insert(entity);

    saveScopes(entity, req.scopes());

    PHASES.forEach(seed -> {
      CertificationTaskPhaseEntity phase = new CertificationTaskPhaseEntity();
      phase.setTaskId(entity.getId());
      phase.setPhaseCode(seed.code());
      phase.setPhaseName(seed.name());
      phase.setPhaseOrder(seed.order());
      phase.setStatus(seed.status());
      phase.setDeleted(0);
      phaseMapper.insert(phase);
    });

    DOCUMENTS.forEach(seed -> {
      CertificationTaskDocumentEntity document = new CertificationTaskDocumentEntity();
      document.setTaskId(entity.getId());
      document.setDocumentType(seed.type());
      document.setDocumentName(seed.name());
      document.setVersionNo(1);
      document.setIsCurrent(1);
      document.setStatus(DEFAULT_DOCUMENT_STATUS);
      document.setLocked(0);
      document.setCompletionPercent(0);
      document.setRequiredValid(0);
      document.setDeleted(0);
      documentMapper.insert(document);
    });

    CertificationTaskEntity savedTask = taskMapper.selectById(entity.getId());
    return CertificationTaskVO.from(savedTask == null ? entity : savedTask, listScopeVO(entity.getId()));
  }

  @Transactional
  public CertificationTaskVO updateTask(Long taskId, CertificationTaskUpdateReq req) {
    validateTaskDates(req.planStartDate(), req.planEndDate(), req.examDate());
    CertificationTaskEntity entity = findTask(taskId);
    applyTaskFields(entity, req.taskName(), req.taskManagerName(), req.companyName(), req.certificationType(),
        req.plannedPeopleCount(), req.planStartDate(),
        req.planEndDate(), req.examDate(), req.remark());
    // status 与 current_phase 已彻底解耦，分别落库
    if (StringUtils.hasText(req.status()) && CertificationTaskStatuses.isValid(req.status())) {
      entity.setStatus(req.status());
    }
    if (StringUtils.hasText(req.currentPhase()) && CertificationTaskPhases.isValid(req.currentPhase())) {
      entity.setCurrentPhase(req.currentPhase());
    }
    entity.setLastBusinessUpdatedAt(LocalDateTime.now());
    taskMapper.updateById(entity);
    replaceScopes(taskId, req.scopes());
    CertificationTaskEntity savedTask = taskMapper.selectById(taskId);
    return CertificationTaskVO.from(savedTask == null ? entity : savedTask, listScopeVO(taskId));
  }

  @Transactional
  public void deleteTask(Long taskId) {
    findTask(taskId);
    taskMapper.deleteById(taskId);
  }

  @Transactional
  public List<CertificationPhaseVO> flow(Long taskId) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationTaskPhaseSupport.ensureCanonicalPhases(taskMapper, phaseMapper, task);
    return listPhaseVOs(taskId);
  }

  @Transactional
  public CertificationWorkbenchVO workbench(Long taskId) {
    CertificationTaskEntity entity = findTask(taskId);
    CertificationTaskPhaseSupport.ensureCanonicalPhases(taskMapper, phaseMapper, entity);
    CertificationTaskEntity refreshed = taskMapper.selectById(taskId);
    CertificationTaskVO task = CertificationTaskVO.from(refreshed == null ? entity : refreshed, listScopeVO(taskId));
    return new CertificationWorkbenchVO(
        task,
        listPhaseVOs(taskId),
        listDocuments(taskId),
        listMembers(taskId),
        listWbsItems(taskId),
        listScheduleItems(taskId),
        listRisks(taskId)
    );
  }

  private List<CertificationPhaseVO> listPhaseVOs(Long taskId) {
    return phaseMapper.selectList(new LambdaQueryWrapper<CertificationTaskPhaseEntity>()
            .eq(CertificationTaskPhaseEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskPhaseEntity::getPhaseOrder))
        .stream()
        .map(CertificationPhaseVO::from)
        .toList();
  }

  private Map<Long, List<CertificationTaskScopeVO>> loadScopes(List<Long> taskIds) {
    if (taskIds == null || taskIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<Long> uniqueIds = taskIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    List<CertificationTaskScopeEntity> scopes = scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .in(CertificationTaskScopeEntity::getTaskId, uniqueIds)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
        .orderByAsc(CertificationTaskScopeEntity::getId));
    return scopes.stream()
        .collect(Collectors.groupingBy(
            CertificationTaskScopeEntity::getTaskId,
            Collectors.mapping(CertificationTaskScopeVO::from, Collectors.toList())
        ));
  }

  private List<CertificationTaskScopeVO> listScopeVO(Long taskId) {
    if (taskId == null) {
      return List.of();
    }
    List<CertificationTaskScopeEntity> scopes = scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTaskId, taskId)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
        .orderByAsc(CertificationTaskScopeEntity::getId));
    return scopes.stream().map(CertificationTaskScopeVO::from).toList();
  }

  private void replaceScopes(Long taskId, List<com.guoyu.platform.certification.dto.CertificationTaskScopeReq> reqScopes) {
    if (taskId == null) {
      return;
    }
    scopeMapper.delete(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTaskId, taskId));
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task != null) {
      saveScopes(task, reqScopes);
    }
  }

  private void saveScopes(CertificationTaskEntity task, List<com.guoyu.platform.certification.dto.CertificationTaskScopeReq> reqScopes) {
    if (task == null || task.getId() == null) {
      return;
    }
    List<com.guoyu.platform.certification.dto.CertificationTaskScopeReq> scopes = reqScopes == null ? List.of() : reqScopes;
    List<CertificationTaskScopeEntity> toSave = new ArrayList<>();
    for (int i = 0; i < scopes.size(); i++) {
      com.guoyu.platform.certification.dto.CertificationTaskScopeReq scopeReq = scopes.get(i);
      if (scopeReq == null) {
        continue;
      }
      boolean hasValue = StringUtils.hasText(scopeReq.certificationType())
          || StringUtils.hasText(scopeReq.workTypeName())
          || StringUtils.hasText(scopeReq.levelCode())
          || StringUtils.hasText(scopeReq.levelName())
          || scopeReq.plannedPeopleCount() != null;
      if (!hasValue) {
        continue;
      }
      CertificationTaskScopeEntity entity = new CertificationTaskScopeEntity();
      entity.setTaskId(task.getId());
      entity.setCertificationType(scopeReq.certificationType());
      entity.setWorkTypeName(scopeReq.workTypeName());
      entity.setLevelCode(scopeReq.levelCode());
      entity.setLevelName(scopeReq.levelName());
      entity.setPlannedPeopleCount(scopeReq.plannedPeopleCount());
      entity.setSortOrder(i + 1);
      entity.setDeleted(0);
      scopeMapper.insert(entity);
      toSave.add(entity);
    }

  }

  @Transactional(readOnly = true)
  public List<CertificationDocumentVO> listDocuments(Long taskId) {
    findTask(taskId);
    return documentMapper.selectList(new LambdaQueryWrapper<CertificationTaskDocumentEntity>()
            .eq(CertificationTaskDocumentEntity::getTaskId, taskId)
            .eq(CertificationTaskDocumentEntity::getIsCurrent, 1)
            .orderByAsc(CertificationTaskDocumentEntity::getDocumentType))
        .stream()
        .map(CertificationDocumentVO::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public CertificationDocumentVO getDocument(Long taskId, String documentType) {
    findTask(taskId);
    return CertificationDocumentVO.from(requireCurrentDocument(taskId, documentType));
  }

  @Transactional
  public CertificationDocumentVO saveDocument(Long taskId, String documentType, CertificationDocumentReq req) {
    findTask(taskId);
    CertificationTaskDocumentEntity document = requireCurrentDocument(taskId, documentType);
    requireEditableDocument(document);
    document.setContentText(req.contentText());
    document.setContentJson(req.contentJson());
    document.setRemark(req.remark());
    document.setStatus(DEFAULT_DOCUMENT_STATUS);
    document.setRejectedAt(null);
    document.setRejectedBy(null);
    document.setRejectReason(null);
    document.setLastSavedAt(LocalDateTime.now());
    documentMapper.updateById(document);
    refreshProjectStatus(taskId);
    return CertificationDocumentVO.from(document);
  }

  @Transactional
  public CertificationDocumentVO submitDocument(Long taskId, String documentType) {
    findTask(taskId);
    CertificationTaskDocumentEntity document = requireCurrentDocument(taskId, documentType);
    requireEditableDocument(document);
    validateDocumentDetailBeforeSubmit(taskId, documentType);
    String processInstanceId = requireWorkflowService().startReview(document);
    PlatformUserPrincipal currentUser = currentUserOrNull();
    document.setStatus(STATUS_INITIAL_REVIEWING);
    document.setCompletionPercent(100);
    document.setRequiredValid(1);
    document.setSubmittedAt(LocalDateTime.now());
    document.setSubmittedBy(currentUser == null ? null : currentUser.getUserId());
    document.setApprovedAt(null);
    document.setApprovedBy(null);
    document.setRejectedAt(null);
    document.setRejectedBy(null);
    document.setRejectReason(null);
    document.setProcessInstanceId(processInstanceId);
    documentMapper.updateById(document);
    refreshProjectStatus(taskId);
    return CertificationDocumentVO.from(document);
  }

  @Transactional
  public CertificationDocumentVO initialReviewDocument(Long taskId, String documentType, CertificationDocumentReviewReq req) {
    findTask(taskId);
    CertificationTaskDocumentEntity document = requireCurrentDocument(taskId, documentType);
    if (!STATUS_INITIAL_REVIEWING.equals(document.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Document is not waiting for initial review");
    }
    boolean approved = requireReviewDecision(req);
    requireWorkflowService().completeInitialReview(document, approved, req.comment());
    if (approved) {
      document.setStatus(STATUS_FINAL_REVIEWING);
    } else {
      rejectDocument(document, STATUS_INITIAL_REJECTED, req.comment());
    }
    documentMapper.updateById(document);
    refreshProjectStatus(taskId);
    return CertificationDocumentVO.from(document);
  }

  @Transactional
  public CertificationDocumentVO finalReviewDocument(Long taskId, String documentType, CertificationDocumentReviewReq req) {
    findTask(taskId);
    CertificationTaskDocumentEntity document = requireCurrentDocument(taskId, documentType);
    if (!STATUS_FINAL_REVIEWING.equals(document.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Document is not waiting for final review");
    }
    boolean approved = requireReviewDecision(req);
    requireWorkflowService().completeFinalReview(document, approved, req.comment());
    if (approved) {
      approveAndArchiveDocument(document);
      requireWorkflowService().recordArchive(document, req.comment());
    } else {
      rejectDocument(document, STATUS_FINAL_REJECTED, req.comment());
    }
    documentMapper.updateById(document);
    refreshProjectStatus(taskId);
    return CertificationDocumentVO.from(document);
  }

  @Transactional(readOnly = true)
  public CertificationDocumentWorkflowVO getDocumentWorkflow(Long taskId, String documentType) {
    findTask(taskId);
    CertificationTaskDocumentEntity document = requireCurrentDocument(taskId, documentType);
    return requireWorkflowService().getWorkflow(document);
  }

  @Transactional(readOnly = true)
  public List<CertificationMemberVO> listMembers(Long taskId) {
    findTask(taskId);
    return memberMapper.selectList(new LambdaQueryWrapper<CertificationTaskMemberEntity>()
            .eq(CertificationTaskMemberEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskMemberEntity::getSortOrder)
            .orderByAsc(CertificationTaskMemberEntity::getId))
        .stream()
        .map(CertificationMemberVO::from)
        .toList();
  }

  @Transactional
  public List<CertificationMemberVO> saveMembers(Long taskId, List<CertificationMemberReq> reqs) {
    findTask(taskId);
    Long documentId = currentDocumentId(taskId, "members");
    for (int i = 0; i < reqs.size(); i++) {
      validateMember(reqs.get(i));
    }
    memberMapper.delete(new LambdaQueryWrapper<CertificationTaskMemberEntity>()
        .eq(CertificationTaskMemberEntity::getTaskId, taskId));
    List<CertificationTaskMemberEntity> saved = new java.util.ArrayList<>();
    for (int i = 0; i < reqs.size(); i++) {
      CertificationMemberReq req = reqs.get(i);
      CertificationTaskMemberEntity entity = new CertificationTaskMemberEntity();
      entity.setTaskId(taskId);
      entity.setDocumentId(documentId);
      entity.setMemberName(req.memberName());
      entity.setRoleCode(req.roleCode());
      entity.setRoleName(req.roleName());
      entity.setDepartmentName(req.departmentName());
      entity.setResponsibility(req.responsibility());
      entity.setStartDate(req.startDate());
      entity.setEndDate(req.endDate());
      entity.setInputFrequency(req.inputFrequency());
      entity.setWorkloadPercent(req.workloadPercent());
      entity.setPhone(req.phone());
      entity.setMaskedPhone(maskPhone(req.phone()));
      entity.setCoreRole(isCoreRole(req.roleCode()));
      entity.setSortOrder(i + 1);
      entity.setRemark(req.remark());
      entity.setDeleted(0);
      memberMapper.insert(entity);
      saved.add(entity);
    }
    touchTask(taskId);
    return saved.stream().map(CertificationMemberVO::from).toList();
  }

  @Transactional(readOnly = true)
  public List<CertificationWbsItemVO> listWbsItems(Long taskId) {
    findTask(taskId);
    return wbsMapper.selectList(new LambdaQueryWrapper<CertificationWbsItemEntity>()
            .eq(CertificationWbsItemEntity::getTaskId, taskId)
            .orderByAsc(CertificationWbsItemEntity::getSortOrder)
            .orderByAsc(CertificationWbsItemEntity::getWbsCode))
        .stream()
        .map(CertificationWbsItemVO::from)
        .toList();
  }

  @Transactional
  public List<CertificationWbsItemVO> saveWbsItems(Long taskId, List<CertificationWbsItemReq> reqs) {
    findTask(taskId);
    Long documentId = currentDocumentId(taskId, "wbs");
    for (CertificationWbsItemReq req : reqs) {
      validateWbs(req);
    }
    wbsMapper.delete(new LambdaQueryWrapper<CertificationWbsItemEntity>()
        .eq(CertificationWbsItemEntity::getTaskId, taskId));
    List<CertificationWbsItemEntity> saved = new java.util.ArrayList<>();
    for (int i = 0; i < reqs.size(); i++) {
      CertificationWbsItemReq req = reqs.get(i);
      CertificationWbsItemEntity entity = new CertificationWbsItemEntity();
      entity.setTaskId(taskId);
      entity.setDocumentId(documentId);
      entity.setWbsCode(req.wbsCode());
      entity.setCategoryCode(req.categoryCode());
      entity.setCategoryName(req.categoryName());
      entity.setItemName(req.itemName());
      entity.setActivityDescription(req.activityDescription());
      entity.setEstimateHours(req.estimateHours());
      entity.setManpowerCount(req.manpowerCount());
      entity.setManpowerRoleCode(req.manpowerRoleCode());
      entity.setResponsibleMemberId(req.responsibleMemberId());
      entity.setResourceInput(req.resourceInput());
      entity.setCostAmount(req.costAmount());
      entity.setDurationDays(req.durationDays());
      entity.setPredecessorId(req.predecessorId());
      entity.setLeaf(true);
      entity.setProgressPercent("completed".equals(req.status()) ? 100 : 0);
      entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "pending");
      entity.setSortOrder(i + 1);
      entity.setDeleted(0);
      wbsMapper.insert(entity);
      saved.add(entity);
    }
    touchTask(taskId);
    return saved.stream().map(CertificationWbsItemVO::from).toList();
  }

  @Transactional(readOnly = true)
  public List<CertificationScheduleItemVO> listScheduleItems(Long taskId) {
    findTask(taskId);
    return scheduleMapper.selectList(new LambdaQueryWrapper<CertificationScheduleItemEntity>()
            .eq(CertificationScheduleItemEntity::getTaskId, taskId)
            .orderByAsc(CertificationScheduleItemEntity::getDayNo)
            .orderByAsc(CertificationScheduleItemEntity::getStartDate))
        .stream()
        .map(CertificationScheduleItemVO::from)
        .toList();
  }

  @Transactional
  public List<CertificationScheduleItemVO> saveScheduleItems(Long taskId, List<CertificationScheduleItemReq> reqs) {
    findTask(taskId);
    Long documentId = currentDocumentId(taskId, "schedule");
    for (CertificationScheduleItemReq req : reqs) {
      validateSchedule(req);
    }
    scheduleMapper.delete(new LambdaQueryWrapper<CertificationScheduleItemEntity>()
        .eq(CertificationScheduleItemEntity::getTaskId, taskId));
    List<CertificationScheduleItemEntity> saved = new java.util.ArrayList<>();
    for (int i = 0; i < reqs.size(); i++) {
      CertificationScheduleItemReq req = reqs.get(i);
      CertificationScheduleItemEntity entity = new CertificationScheduleItemEntity();
      entity.setTaskId(taskId);
      entity.setDocumentId(documentId);
      entity.setDayNo(req.dayNo());
      entity.setPlanDate(req.planDate());
      entity.setStartDate(req.startDate());
      entity.setEndDate(req.endDate());
      entity.setItemName(req.itemName());
      entity.setItemDescription(req.itemDescription());
      entity.setRequired(Boolean.TRUE.equals(req.required()));
      entity.setCompleted(Boolean.TRUE.equals(req.completed()));
      entity.setExternalContactMemberId(req.externalContactMemberId());
      entity.setCertificationOwnerMemberId(req.certificationOwnerMemberId());
      entity.setSortOrder(i + 1);
      entity.setDeleted(0);
      scheduleMapper.insert(entity);
      saved.add(entity);
    }
    touchTask(taskId);
    return saved.stream().map(CertificationScheduleItemVO::from).toList();
  }

  @Transactional(readOnly = true)
  public List<CertificationRiskVO> listRisks(Long taskId) {
    findTask(taskId);
    return riskMapper.selectList(new LambdaQueryWrapper<CertificationRiskEntity>()
            .eq(CertificationRiskEntity::getTaskId, taskId)
            .orderByDesc(CertificationRiskEntity::getRiskLevelScore)
            .orderByAsc(CertificationRiskEntity::getSortOrder))
        .stream()
        .map(CertificationRiskVO::from)
        .toList();
  }

  @Transactional
  public List<CertificationRiskVO> saveRisks(Long taskId, List<CertificationRiskReq> reqs) {
    findTask(taskId);
    Long documentId = currentDocumentId(taskId, "risk");
    riskMapper.delete(new LambdaQueryWrapper<CertificationRiskEntity>()
        .eq(CertificationRiskEntity::getTaskId, taskId));
    List<CertificationRiskEntity> saved = new java.util.ArrayList<>();
    for (int i = 0; i < reqs.size(); i++) {
      CertificationRiskReq req = reqs.get(i);
      CertificationRiskEntity entity = new CertificationRiskEntity();
      RiskLevel level = riskLevel(req.probabilityCode(), req.impactCode());
      entity.setTaskId(taskId);
      entity.setDocumentId(documentId);
      entity.setRiskNo("R-" + String.format("%03d", i + 1));
      entity.setRiskDescription(req.riskDescription());
      entity.setProbabilityCode(req.probabilityCode());
      entity.setImpactCode(req.impactCode());
      entity.setRiskLevelCode(level.code());
      entity.setRiskLevelScore(level.score());
      entity.setResponsePlan(req.responsePlan());
      entity.setOwnerMemberId(req.ownerMemberId());
      entity.setOwnerUserId(req.ownerUserId());
      entity.setOwnerName(req.ownerName());
      entity.setStatus("open");
      entity.setExpectedCloseDate(req.expectedCloseDate());
      entity.setRemark(req.remark());
      entity.setSortOrder(i + 1);
      entity.setDeleted(0);
      riskMapper.insert(entity);
      saved.add(entity);
    }
    touchTask(taskId);
    return saved.stream().map(CertificationRiskVO::from).toList();
  }

  @Transactional
  public CertificationRiskVO toggleRiskStatus(Long taskId, Long riskId, CertificationRiskStatusReq req) {
    findTask(taskId);
    CertificationRiskEntity risk = riskMapper.selectById(riskId);
    if (risk == null || !taskId.equals(risk.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "风险事项不存在");
    }
    risk.setStatus(req.status());
    if ("closed".equals(req.status())) {
      risk.setClosedAt(LocalDateTime.now());
      risk.setCloseReason(req.closeReason());
    }
    riskMapper.updateById(risk);
    touchTask(taskId);
    return CertificationRiskVO.from(risk);
  }

  private void applyTaskFields(
      CertificationTaskEntity entity,
      String taskName,
      String taskManagerName,
      String companyName,
      String certificationType,
      Integer plannedPeopleCount,
      LocalDate planStartDate,
      LocalDate planEndDate,
      LocalDate examDate,
      String remark
  ) {
    entity.setTaskName(taskName);
    entity.setTaskManagerName(taskManagerName);
    entity.setCompanyName(companyName);
    entity.setCertificationType(certificationType);
    entity.setPlannedPeopleCount(plannedPeopleCount == null ? 0 : plannedPeopleCount);
    entity.setPlanStartDate(planStartDate);
    entity.setPlanEndDate(planEndDate);
    entity.setExamDate(examDate);
    entity.setRemark(remark);
  }

  private CertificationTaskEntity findTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定项目任务不存在");
    }
    return task;
  }

  private CertificationTaskDocumentEntity currentDocument(Long taskId, String documentType) {
    return documentMapper.selectOne(new LambdaQueryWrapper<CertificationTaskDocumentEntity>()
        .eq(CertificationTaskDocumentEntity::getTaskId, taskId)
        .eq(CertificationTaskDocumentEntity::getDocumentType, documentType)
        .eq(CertificationTaskDocumentEntity::getIsCurrent, 1));
  }

  private CertificationTaskDocumentEntity requireCurrentDocument(Long taskId, String documentType) {
    CertificationTaskDocumentEntity document = currentDocument(taskId, documentType);
    if (document == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务文档不存在");
    }
    return document;
  }

  private Long currentDocumentId(Long taskId, String documentType) {
    CertificationTaskDocumentEntity document = currentDocument(taskId, documentType);
    return document == null ? null : document.getId();
  }

  private void validateTaskDates(LocalDate start, LocalDate end, LocalDate examDate) {
    if (start != null && end != null && start.isAfter(end)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务开始日期不能晚于结束日期");
    }
    if (start != null && examDate != null && start.isAfter(examDate)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务开始日期不能晚于考试日期");
    }
  }

  private void validateMember(CertificationMemberReq req) {
    if (StringUtils.hasText(req.phone()) && !MOBILE_PATTERN.matcher(req.phone()).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "成员手机号格式不正确");
    }
    if (req.startDate() != null && req.endDate() != null && req.startDate().isAfter(req.endDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "成员投入开始日期不能晚于结束日期");
    }
    if (req.workloadPercent() != null
        && (req.workloadPercent().compareTo(BigDecimal.ZERO) < 0
        || req.workloadPercent().compareTo(BigDecimal.valueOf(100)) > 0)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "成员投入比例必须在 0 到 100 之间");
    }
  }

  private void validateWbs(CertificationWbsItemReq req) {
    if (isNegative(req.estimateHours()) || isNegative(req.costAmount())
        || (req.durationDays() != null && req.durationDays() < 0)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "WBS 工时、费用和工期不能为负数");
    }
    if (isNegative(req.manpowerCount())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "WBS 人力数量不能为负数");
    }
  }

  private void validateSchedule(CertificationScheduleItemReq req) {
    if (req.startDate() != null && req.endDate() != null && req.startDate().isAfter(req.endDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "进度事项开始日期不能晚于结束日期");
    }
  }

  private void requireEditableDocument(CertificationTaskDocumentEntity document) {
    if (!EDITABLE_DOCUMENT_STATUSES.contains(document.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Document is not editable in status " + document.getStatus());
    }
  }

  private boolean requireReviewDecision(CertificationDocumentReviewReq req) {
    if (req == null || req.approved() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Review decision is required");
    }
    if (!Boolean.TRUE.equals(req.approved()) && !StringUtils.hasText(req.comment())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Rejection comment is required");
    }
    return Boolean.TRUE.equals(req.approved());
  }

  private void rejectDocument(CertificationTaskDocumentEntity document, String rejectedStatus, String comment) {
    PlatformUserPrincipal currentUser = currentUserOrNull();
    document.setStatus(rejectedStatus);
    document.setRejectedAt(LocalDateTime.now());
    document.setRejectedBy(currentUser == null ? null : currentUser.getUserId());
    document.setRejectReason(comment);
    document.setApprovedAt(null);
    document.setApprovedBy(null);
  }

  private void approveAndArchiveDocument(CertificationTaskDocumentEntity document) {
    PlatformUserPrincipal currentUser = currentUserOrNull();
    document.setStatus(STATUS_ARCHIVED);
    document.setApprovedAt(LocalDateTime.now());
    document.setApprovedBy(currentUser == null ? null : currentUser.getUserId());
    document.setRejectedAt(null);
    document.setRejectedBy(null);
    document.setRejectReason(null);
    document.setCompletionPercent(100);
    document.setRequiredValid(1);
  }

  private CertificationDocumentWorkflowService requireWorkflowService() {
    if (workflowService == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Document workflow service is not configured");
    }
    return workflowService;
  }

  private PlatformUserPrincipal currentUserOrNull() {
    try {
      return SecurityContext.currentUser();
    } catch (Exception e) {
      return null;
    }
  }

  private void validateDocumentDetailBeforeSubmit(Long taskId, String documentType) {
    if ("members".equals(documentType) && countMembers(taskId) == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "项目成员信息未填写");
    }
    if ("wbs".equals(documentType) && countWbs(taskId) == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "项目WBS信息未填写");
    }
    if ("schedule".equals(documentType) && countSchedule(taskId) == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "项目进度信息未填写");
    }
    if ("schedule".equals(documentType) && countIncompleteSchedule(taskId) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "准备期关键安排尚未全部确认完成，不能提交审核");
    }
    if ("risk".equals(documentType) && countRisks(taskId) == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "风险信息未填写");
    }
  }

  private long countMembers(Long taskId) {
    Long count = memberMapper.selectCount(new LambdaQueryWrapper<CertificationTaskMemberEntity>()
        .eq(CertificationTaskMemberEntity::getTaskId, taskId));
    return count == null ? 0 : count;
  }

  private long countWbs(Long taskId) {
    Long count = wbsMapper.selectCount(new LambdaQueryWrapper<CertificationWbsItemEntity>()
        .eq(CertificationWbsItemEntity::getTaskId, taskId));
    return count == null ? 0 : count;
  }

  private long countSchedule(Long taskId) {
    Long count = scheduleMapper.selectCount(new LambdaQueryWrapper<CertificationScheduleItemEntity>()
        .eq(CertificationScheduleItemEntity::getTaskId, taskId));
    return count == null ? 0 : count;
  }

  private long countIncompleteSchedule(Long taskId) {
    Long count = scheduleMapper.selectCount(new LambdaQueryWrapper<CertificationScheduleItemEntity>()
        .eq(CertificationScheduleItemEntity::getTaskId, taskId)
        .and(wrapper -> wrapper.eq(CertificationScheduleItemEntity::getCompleted, false)
            .or()
            .isNull(CertificationScheduleItemEntity::getCompleted)));
    return count == null ? 0 : count;
  }

  private long countRisks(Long taskId) {
    Long count = riskMapper.selectCount(new LambdaQueryWrapper<CertificationRiskEntity>()
        .eq(CertificationRiskEntity::getTaskId, taskId));
    return count == null ? 0 : count;
  }

  private boolean isNegative(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) < 0;
  }

  private String maskPhone(String phone) {
    if (!StringUtils.hasText(phone) || phone.length() != 11) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
  }

  private boolean isCoreRole(String roleCode) {
    return List.of("task_manager", "material_specialist", "certification_owner").contains(roleCode);
  }

  private RiskLevel riskLevel(String probabilityCode, String impactCode) {
    int probability = RISK_SCORE.getOrDefault(probabilityCode, 1);
    int impact = RISK_SCORE.getOrDefault(impactCode, 1);
    int score = probability + impact - 1;
    String code = switch (score) {
      case 2 -> "medium_low";
      case 3 -> "medium";
      case 4 -> "medium_high";
      case 5 -> "high";
      default -> "low";
    };
    return new RiskLevel(code, score);
  }

  private void refreshProjectStatus(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      return;
    }
    // 已经离开“任务创建阶段”的任务不再回退，由各自阶段的应用服务管理 status
    String currentPhase = task.getCurrentPhase();
    if (!CertificationTaskPhases.TASK_CREATION.equals(currentPhase)
        && CertificationTaskPhases.isValid(currentPhase)) {
      task.setLastBusinessUpdatedAt(LocalDateTime.now());
      refreshDocumentCounts(task);
      return;
    }

    List<CertificationTaskDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskDocumentEntity>()
            .eq(CertificationTaskDocumentEntity::getTaskId, taskId)
            .eq(CertificationTaskDocumentEntity::getIsCurrent, 1));
    if (documents == null) {
      documents = List.of();
    }
    long archivedCount = documents.stream()
        .filter(document -> STATUS_ARCHIVED.equals(document.getStatus()))
        .count();
    long reviewingCount = documents.stream()
        .filter(document -> REVIEWING_DOCUMENT_STATUSES.contains(document.getStatus()))
        .count();
    long rejectedCount = documents.stream()
        .filter(document -> STATUS_INITIAL_REJECTED.equals(document.getStatus())
            || STATUS_FINAL_REJECTED.equals(document.getStatus()))
        .count();

    boolean allDocumentsArchived = documents.size() >= DOCUMENTS.size() && archivedCount == DOCUMENTS.size();
    task.setDocumentCompletedCount((int) archivedCount);
    task.setDocumentTotalCount(DOCUMENTS.size());
    task.setLastBusinessUpdatedAt(LocalDateTime.now());

    if (allDocumentsArchived) {
      // 所有资料复审通过 -> 推进到“认定筹备阶段”，状态置为“进行中”
      taskMapper.updateById(task);
      CertificationTaskPhaseSupport.advancePhase(
          taskMapper,
          phaseMapper,
          taskId,
          CertificationTaskPhases.TASK_CREATION,
          CertificationTaskPhases.PREPARATION,
          CertificationTaskStatuses.IN_PROGRESS
      );
      return;
    }

    // 仍在任务创建阶段时，根据材料状态推断阶段内的 status
    task.setCurrentPhase(CertificationTaskPhases.TASK_CREATION);
    String nextStatus;
    if (rejectedCount > 0) {
      nextStatus = CertificationTaskStatuses.REJECTED;
    } else if (reviewingCount > 0) {
      nextStatus = CertificationTaskStatuses.PENDING_REVIEW;
    } else {
      nextStatus = CertificationTaskStatuses.IN_PROGRESS;
    }
    task.setStatus(nextStatus);
    taskMapper.updateById(task);
  }

  private void refreshDocumentCounts(CertificationTaskEntity task) {
    if (task == null || task.getId() == null) {
      return;
    }
    List<CertificationTaskDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskDocumentEntity>()
            .eq(CertificationTaskDocumentEntity::getTaskId, task.getId())
            .eq(CertificationTaskDocumentEntity::getIsCurrent, 1));
    if (documents == null) {
      documents = List.of();
    }
    long archivedCount = documents.stream()
        .filter(document -> STATUS_ARCHIVED.equals(document.getStatus()))
        .count();
    task.setDocumentCompletedCount((int) archivedCount);
    task.setDocumentTotalCount(DOCUMENTS.size());
    taskMapper.updateById(task);
  }

  private void touchTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task != null) {
      task.setLastBusinessUpdatedAt(LocalDateTime.now());
      taskMapper.updateById(task);
    }
  }

  private record DocumentSeed(String type, String name) {
  }

  private record RiskLevel(String code, Integer score) {
  }
}
