package com.guoyu.platform.certification.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationDocumentWorkflowRecordEntity;
import com.guoyu.platform.certification.entity.CertificationTaskDocumentEntity;
import com.guoyu.platform.certification.mapper.CertificationDocumentWorkflowRecordMapper;
import com.guoyu.platform.certification.vo.CertificationDocumentWorkflowRecordVO;
import com.guoyu.platform.certification.vo.CertificationDocumentWorkflowVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CertificationDocumentWorkflowService {

  public static final String PROCESS_KEY = "certificationDocumentReview";
  private static final String INITIAL_REVIEW_TASK = "initialReviewTask";
  private static final String FINAL_REVIEW_TASK = "finalReviewTask";

  private final RuntimeService runtimeService;
  private final TaskService taskService;
  private final CertificationDocumentWorkflowRecordMapper recordMapper;

  public CertificationDocumentWorkflowService(
      RuntimeService runtimeService,
      TaskService taskService,
      CertificationDocumentWorkflowRecordMapper recordMapper
  ) {
    this.runtimeService = runtimeService;
    this.taskService = taskService;
    this.recordMapper = recordMapper;
  }

  public String startReview(CertificationTaskDocumentEntity document) {
    PlatformUserPrincipal operator = currentUser();
    Map<String, Object> variables = new HashMap<>();
    variables.put("tenantId", document.getTenantId());
    variables.put("taskId", document.getTaskId());
    variables.put("documentId", document.getId());
    variables.put("documentType", document.getDocumentType());
    variables.put("submitterUserId", operator == null ? null : operator.getUserId());
    variables.put("submitterName", operator == null ? null : operator.getDisplayName());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        PROCESS_KEY,
        businessKey(document),
        variables
    );
    insertRecord(document, processInstance.getProcessInstanceId(), null, "submit", "Submit", "submit",
        "submitted", null, operator);
    return processInstance.getProcessInstanceId();
  }

  public void completeInitialReview(CertificationTaskDocumentEntity document, boolean approved, String comment) {
    validateRejectionComment(approved, comment);
    Task task = requireActiveTask(document, INITIAL_REVIEW_TASK, "initial review");
    PlatformUserPrincipal operator = currentUser();
    if (operator != null) {
      taskService.setAssignee(task.getId(), String.valueOf(operator.getUserId()));
    }
    taskService.complete(task.getId(), Map.of(
        "initialReviewApproved", approved,
        "reviewComment", comment == null ? "" : comment
    ));
    insertRecord(document, document.getProcessInstanceId(), task, "initial_review",
        approved ? "approved" : "rejected", comment, operator);
  }

  public void completeFinalReview(CertificationTaskDocumentEntity document, boolean approved, String comment) {
    validateRejectionComment(approved, comment);
    Task task = requireActiveTask(document, FINAL_REVIEW_TASK, "final review");
    PlatformUserPrincipal operator = currentUser();
    if (operator != null) {
      taskService.setAssignee(task.getId(), String.valueOf(operator.getUserId()));
    }
    taskService.complete(task.getId(), Map.of(
        "finalReviewApproved", approved,
        "reviewComment", comment == null ? "" : comment
    ));
    insertRecord(document, document.getProcessInstanceId(), task, "final_review",
        approved ? "approved" : "rejected", comment, operator);
  }

  public void recordArchive(CertificationTaskDocumentEntity document, String comment) {
    insertRecord(document, document.getProcessInstanceId(), null, "archive", "Archive Document", "archive",
        "archived", comment, currentUser());
  }

  public CertificationDocumentWorkflowVO getWorkflow(CertificationTaskDocumentEntity document) {
    Task currentTask = null;
    if (StringUtils.hasText(document.getProcessInstanceId())) {
      currentTask = taskService.createTaskQuery()
          .processInstanceId(document.getProcessInstanceId())
          .active()
          .singleResult();
    }
    List<CertificationDocumentWorkflowRecordVO> records = recordMapper.selectList(
            new LambdaQueryWrapper<CertificationDocumentWorkflowRecordEntity>()
                .eq(CertificationDocumentWorkflowRecordEntity::getDocumentId, document.getId())
                .orderByAsc(CertificationDocumentWorkflowRecordEntity::getOperatedAt)
                .orderByAsc(CertificationDocumentWorkflowRecordEntity::getId))
        .stream()
        .map(CertificationDocumentWorkflowRecordVO::from)
        .toList();
    return new CertificationDocumentWorkflowVO(
        document.getId() == null ? null : String.valueOf(document.getId()),
        document.getDocumentType(),
        document.getStatus(),
        document.getProcessInstanceId(),
        currentTask == null ? null : currentTask.getTaskDefinitionKey(),
        currentTask == null ? null : currentTask.getName(),
        records
    );
  }

  private Task requireActiveTask(CertificationTaskDocumentEntity document, String taskDefinitionKey, String label) {
    if (!StringUtils.hasText(document.getProcessInstanceId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Document has no active workflow process");
    }
    Task task = taskService.createTaskQuery()
        .processInstanceId(document.getProcessInstanceId())
        .taskDefinitionKey(taskDefinitionKey)
        .active()
        .singleResult();
    if (task == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "No active " + label + " task found");
    }
    return task;
  }

  private void validateRejectionComment(boolean approved, String comment) {
    if (!approved && !StringUtils.hasText(comment)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Rejection comment is required");
    }
  }

  private void insertRecord(
      CertificationTaskDocumentEntity document,
      String processInstanceId,
      Task task,
      String action,
      String actionResult,
      String comment,
      PlatformUserPrincipal operator
  ) {
    insertRecord(
        document,
        processInstanceId,
        task == null ? null : task.getId(),
        task == null ? null : task.getTaskDefinitionKey(),
        task == null ? null : task.getName(),
        action,
        actionResult,
        comment,
        operator
    );
  }

  private void insertRecord(
      CertificationTaskDocumentEntity document,
      String processInstanceId,
      String engineTaskId,
      String nodeKey,
      String nodeName,
      String action,
      String actionResult,
      String comment,
      PlatformUserPrincipal operator
  ) {
    CertificationDocumentWorkflowRecordEntity entity = new CertificationDocumentWorkflowRecordEntity();
    entity.setTenantId(document.getTenantId());
    entity.setTaskId(document.getTaskId());
    entity.setDocumentId(document.getId());
    entity.setDocumentType(document.getDocumentType());
    entity.setProcessInstanceId(processInstanceId);
    entity.setTaskIdInEngine(engineTaskId);
    entity.setNodeKey(nodeKey);
    entity.setNodeName(nodeName);
    entity.setAction(action);
    entity.setActionResult(actionResult);
    entity.setComment(comment);
    entity.setOperatorUserId(operator == null ? null : operator.getUserId());
    entity.setOperatorName(operator == null ? null : operator.getDisplayName());
    entity.setOperatedAt(LocalDateTime.now());
    entity.setDeleted(0);
    recordMapper.insert(entity);
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (Exception e) {
      return null;
    }
  }

  private String businessKey(CertificationTaskDocumentEntity document) {
    return document.getTaskId() + ":" + document.getDocumentType() + ":" + document.getId();
  }
}
