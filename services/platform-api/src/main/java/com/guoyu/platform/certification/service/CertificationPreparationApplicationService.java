package com.guoyu.platform.certification.service;

import com.guoyu.platform.common.util.SensitiveMaskUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.PreparationAssessorGroupSaveReq;
import com.guoyu.platform.certification.dto.PreparationAssessorQueryReq;
import com.guoyu.platform.certification.dto.PreparationAssessorDispatchReq;
import com.guoyu.platform.certification.dto.PreparationAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationCandidateDocumentSaveReq;
import com.guoyu.platform.certification.dto.PreparationCandidateImportReq;
import com.guoyu.platform.certification.dto.PreparationCandidateQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.dto.PreparationChecklistUpdateReq;
import com.guoyu.platform.certification.dto.PreparationDocumentQueryReq;
import com.guoyu.platform.certification.dto.PreparationDocumentReviewReq;
import com.guoyu.platform.certification.dto.PreparationExamSiteQueryReq;
import com.guoyu.platform.certification.dto.PreparationExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialGenerateReq;
import com.guoyu.platform.certification.dto.PreparationMaterialItemSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialQueryReq;
import com.guoyu.platform.certification.dto.PreparationTaskAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskEquipmentSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffRoleReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskQueryReq;
import com.guoyu.platform.certification.entity.CertificationApplicationMaterialBatchEntity;
import com.guoyu.platform.certification.entity.CertificationApplicationMaterialItemEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorGroupWorktypeEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportErrorEntity;
import com.guoyu.platform.certification.entity.CertificationExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationMaterialTemplateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;
import com.guoyu.platform.certification.entity.CertificationTaskChecklistItemEntity;
import com.guoyu.platform.certification.entity.CertificationTaskPhaseEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationApplicationMaterialBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationApplicationMaterialItemMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorGroupWorktypeMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
import com.guoyu.platform.certification.mapper.CandidateDocRow;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationMaterialTemplateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskEquipmentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskChecklistItemMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.AssessorGroupWorktypeItem;
import com.guoyu.platform.certification.vo.PreparationAssessorGroupVO;
import com.guoyu.platform.certification.vo.PreparationAssessorTaskVO;
import com.guoyu.platform.certification.vo.PreparationAssessorVO;
import com.guoyu.platform.certification.vo.PreparationCandidateDocumentVO;
import com.guoyu.platform.certification.vo.PreparationCandidateStagingVO;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.certification.vo.PreparationChecklistItemVO;
import com.guoyu.platform.certification.vo.PreparationChecklistVO;
import com.guoyu.platform.certification.vo.PreparationCheckItemVO;
import com.guoyu.platform.certification.vo.PreparationCompleteCheckVO;
import com.guoyu.platform.certification.vo.PreparationExamSiteVO;
import com.guoyu.platform.certification.vo.PreparationImportBatchVO;
import com.guoyu.platform.certification.vo.PreparationImportErrorVO;
import com.guoyu.platform.certification.vo.PreparationMaterialBatchVO;
import com.guoyu.platform.certification.vo.PreparationMaterialItemVO;
import com.guoyu.platform.certification.vo.PreparationTaskAssessorVO;
import com.guoyu.platform.certification.vo.PreparationTaskEquipmentVO;
import com.guoyu.platform.certification.vo.PreparationTaskExamSiteVO;
import com.guoyu.platform.certification.vo.PreparationTaskExamStaffVO;
import com.guoyu.platform.certification.vo.PreparationTaskOverviewVO;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.exam.service.ExamWorkTypeCoverageService;
import org.springframework.context.annotation.Lazy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class CertificationPreparationApplicationService {

  private static final String EXAM_STAFF_ROLE_INTERNAL_SUPERVISOR = "internal_supervisor";
  private static final String EXAM_STAFF_ROLE_EXAM_STAFF = "exam_staff";
  private static final String EXAM_STAFF_ROLE_INVIGILATOR = "invigilator";
  private static final String EXAM_STAFF_ROLE_WORKER = "worker";

  private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[0-9Xx]$");
  private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
  private static final String CANDIDATE_DOC_SCOPE = "candidate_doc";
  private static final List<String> CANDIDATE_DOC_CODES = List.of("DOC001", "DOC002", "DOC003", "DOC004", "DOC005", "DOC006");
  private static final List<String> AUTO_APPROVED_IMAGE_DOC_CODES = List.of("DOC002", "DOC003", "DOC004");
  private static final String INFO_APPROVED = "approved";
  private static final List<ChecklistTemplateItem> CHECKLIST_TEMPLATE = List.of(
      new ChecklistTemplateItem(1, "平板电脑", "按需"),
      new ChecklistTemplateItem(2, "平板电脑充电插头、线", "按需"),
      new ChecklistTemplateItem(3, "实操试卷", "按需"),
      new ChecklistTemplateItem(4, "职业技能等级认定评价结果确认表", "按需"),
      new ChecklistTemplateItem(5, "实操评分表（1式3份）", "按需"),
      new ChecklistTemplateItem(6, "考场签到表（理论）2份", "2"),
      new ChecklistTemplateItem(7, "考场签到表（实操）2份", "2"),
      new ChecklistTemplateItem(8, "考场简况表（理论）", "1"),
      new ChecklistTemplateItem(9, "考场简况表（实操）", "1"),
      new ChecklistTemplateItem(10, "成绩填报表", "1"),
      new ChecklistTemplateItem(11, "理论考场座次表2份", "2"),
      new ChecklistTemplateItem(12, "考场分布情况表", "按需"),
      new ChecklistTemplateItem(13, "督导报告（广西）", "按需"),
      new ChecklistTemplateItem(14, "督导报告（行业）", "按需"),
      new ChecklistTemplateItem(15, "认定考场记录表（行业）", "按需"),
      new ChecklistTemplateItem(16, "答辩原始记录表（技师、高级技师）", "按需"),
      new ChecklistTemplateItem(17, "评审成绩汇总表（技师、高级技师）", "按需"),
      new ChecklistTemplateItem(18, "投票评审表（技师、高级技师）", "按需"),
      new ChecklistTemplateItem(19, "业绩评估报告评分表（技师、高级技师）", "按需"),
      new ChecklistTemplateItem(20, "答辩评审评分表（技师、高级技师）", "按需"),
      new ChecklistTemplateItem(21, "理论考场纪律", "按需"),
      new ChecklistTemplateItem(22, "实操考场纪律", "按需"),
      new ChecklistTemplateItem(23, "认定日程安排表", "按需"),
      new ChecklistTemplateItem(24, "考评员签到表", "1"),
      new ChecklistTemplateItem(25, "监考人员签到表", "1"),
      new ChecklistTemplateItem(26, "考生报名材料（鉴定表、工作证明、上一等级证书按需）", "按需"),
      new ChecklistTemplateItem(27, "监考人员证卡", "按需"),
      new ChecklistTemplateItem(28, "考评员证卡", "按需"),
      new ChecklistTemplateItem(29, "考务人员证卡", "按需"),
      new ChecklistTemplateItem(30, "督导员证卡", "按需"),
      new ChecklistTemplateItem(31, "摄像头", "按需"),
      new ChecklistTemplateItem(32, "摄像头支架", "按需"),
      new ChecklistTemplateItem(33, "工作手机及充电器", "按需"),
      new ChecklistTemplateItem(34, "考场路由器", "按需"),
      new ChecklistTemplateItem(35, "装订文印文具（钢擦、胶水、水性笔等）", "按需"),
      new ChecklistTemplateItem(36, "横幅", "按需"),
      new ChecklistTemplateItem(37, "劳保服&安全帽", "按需"),
      new ChecklistTemplateItem(38, "夹板（每场3块）", "按需"),
      new ChecklistTemplateItem(39, "应急充电宝", "按需"),
      new ChecklistTemplateItem(40, "长线排插", "按需"),
      new ChecklistTemplateItem(41, "考评员标识服", "按需"),
      new ChecklistTemplateItem(42, "笔记本电脑", "2"),
      new ChecklistTemplateItem(43, "打印机（打印纸）", "按需"),
      new ChecklistTemplateItem(44, "封条", "按需"),
      new ChecklistTemplateItem(45, "人员安排表（监考员、考评员、考务、督导）", "各1份")
  );

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskPhaseMapper phaseMapper;
  private final CertificationCandidateImportBatchMapper importBatchMapper;
  private final CertificationCandidateImportErrorMapper importErrorMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationMaterialTemplateMapper templateMapper;
  private final CertificationCandidateDocumentMapper documentMapper;
  private final CertificationApplicationMaterialBatchMapper materialBatchMapper;
  private final CertificationApplicationMaterialItemMapper materialItemMapper;
  private final CertificationExamSiteMapper examSiteMapper;
  private final CertificationTaskExamSiteMapper taskExamSiteMapper;
  private final CertificationTaskExamStaffMapper taskExamStaffMapper;
  private final CertificationTaskChecklistItemMapper taskChecklistItemMapper;
  private final CertificationInfoExamStaffMapper infoExamStaffMapper;
  private final CertificationTaskEquipmentMapper taskEquipmentMapper;
  private final CertificationInfoExamEquipmentMapper infoEquipmentMapper;
  private final CertificationAssessorMapper assessorMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationAssessorGroupWorktypeMapper groupWorktypeMapper;
  private final CertificationInfoAssessorMapper infoAssessorMapper;
  private final FileApplicationService fileApplicationService;
  private final CertificationCandidateStagingApplicationService stagingApplicationService;
  private final CertificationApplicationFormGenerationService applicationFormGenerationService;
  private final CertificationImageSourceService imageSourceService;
  private final CertificationExamSessionApplicationService examSessionService;
  private final ExamWorkTypeCoverageService workTypeCoverageService;

  public CertificationPreparationApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      CertificationCandidateImportBatchMapper importBatchMapper,
      CertificationCandidateImportErrorMapper importErrorMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationMaterialTemplateMapper templateMapper,
      CertificationCandidateDocumentMapper documentMapper,
      CertificationApplicationMaterialBatchMapper materialBatchMapper,
      CertificationApplicationMaterialItemMapper materialItemMapper,
      CertificationExamSiteMapper examSiteMapper,
      CertificationTaskExamSiteMapper taskExamSiteMapper,
      CertificationTaskExamStaffMapper taskExamStaffMapper,
      CertificationTaskChecklistItemMapper taskChecklistItemMapper,
      CertificationInfoExamStaffMapper infoExamStaffMapper,
      CertificationTaskEquipmentMapper taskEquipmentMapper,
      CertificationInfoExamEquipmentMapper infoEquipmentMapper,
      CertificationAssessorMapper assessorMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationAssessorGroupWorktypeMapper groupWorktypeMapper,
      CertificationInfoAssessorMapper infoAssessorMapper,
      FileApplicationService fileApplicationService,
      @Lazy CertificationCandidateStagingApplicationService stagingApplicationService,
      CertificationApplicationFormGenerationService applicationFormGenerationService,
      CertificationImageSourceService imageSourceService,
      CertificationExamSessionApplicationService examSessionService,
      ExamWorkTypeCoverageService workTypeCoverageService
  ) {
    this.taskMapper = taskMapper;
    this.phaseMapper = phaseMapper;
    this.importBatchMapper = importBatchMapper;
    this.importErrorMapper = importErrorMapper;
    this.candidateMapper = candidateMapper;
    this.templateMapper = templateMapper;
    this.documentMapper = documentMapper;
    this.materialBatchMapper = materialBatchMapper;
    this.materialItemMapper = materialItemMapper;
    this.examSiteMapper = examSiteMapper;
    this.taskExamSiteMapper = taskExamSiteMapper;
    this.taskExamStaffMapper = taskExamStaffMapper;
    this.taskChecklistItemMapper = taskChecklistItemMapper;
    this.infoExamStaffMapper = infoExamStaffMapper;
    this.taskEquipmentMapper = taskEquipmentMapper;
    this.infoEquipmentMapper = infoEquipmentMapper;
    this.assessorMapper = assessorMapper;
    this.groupMapper = groupMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.scopeMapper = scopeMapper;
    this.groupWorktypeMapper = groupWorktypeMapper;
    this.infoAssessorMapper = infoAssessorMapper;
    this.fileApplicationService = fileApplicationService;
    this.stagingApplicationService = stagingApplicationService;
    this.applicationFormGenerationService = applicationFormGenerationService;
    this.imageSourceService = imageSourceService;
    this.examSessionService = examSessionService;
    this.workTypeCoverageService = workTypeCoverageService;
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationTaskOverviewVO> pageTasks(PreparationTaskQueryReq req) {
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .orderByDesc(CertificationTaskEntity::getLastBusinessUpdatedAt)
        .orderByDesc(CertificationTaskEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationTaskEntity::getTaskName, req.getKeyword())
          .or()
          .like(CertificationTaskEntity::getTaskNo, req.getKeyword()));
    }
    wrapper.eq(CertificationTaskEntity::getCurrentPhase, CertificationTaskPhaseSupport.PREPARATION);
    if (StringUtils.hasText(req.getCurrentPhase())) {
      wrapper.eq(CertificationTaskEntity::getCurrentPhase, req.getCurrentPhase());
    }
    Page<CertificationTaskEntity> page = taskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(buildTaskOverviews(page.getRecords()),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public PreparationTaskOverviewVO overview(Long taskId) {
    return overviewFromTask(findTask(taskId));
  }

  @Transactional
  public PreparationImportBatchVO createImportBatch(Long taskId, PreparationCandidateImportReq req) {
    findTask(taskId);
    if ("training".equals(req.sourceType()) && CollectionUtils.isEmpty(req.trainingPeopleIds())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要导入的培训学员");
    }
    if (req.taskScopeId() == null || listScopes(taskId).stream().noneMatch(scope -> req.taskScopeId().equals(scope.getId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择当前任务下的认定班级");
    }
    CertificationCandidateImportBatchEntity batch = new CertificationCandidateImportBatchEntity();
    batch.setTaskId(taskId);
    batch.setTaskScopeId(req.taskScopeId());
    examSessionService.validateSessionIds(taskId, req.examSessionIds());
    batch.setBatchNo("CIB-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
    batch.setSourceType(StringUtils.hasText(req.sourceType()) ? req.sourceType() : "manual");
    batch.setSourceFileId(req.sourceFileId());
    batch.setStartedAt(LocalDateTime.now());
    batch.setStatus("completed");
    batch.setRemark(req.remark());
    batch.setTotalCount(0);
    batch.setSuccessCount(0);
    batch.setFailedCount(0);
    batch.setDuplicateCount(0);
    batch.setDeleted(0);
    importBatchMapper.insert(batch);
    boolean trainingImport = "training".equals(batch.getSourceType());
    int total = trainingImport
        ? req.trainingPeopleIds() == null ? 0 : req.trainingPeopleIds().size()
        : req.candidates() == null ? 0 : req.candidates().size();
    int success = 0;
    int failed = 0;
    if (trainingImport && !CollectionUtils.isEmpty(req.trainingPeopleIds())) {
      for (int i = 0; i < req.trainingPeopleIds().size(); i++) {
        try {
          stagingApplicationService.promoteTrainingPerson(taskId, req.trainingPeopleIds().get(i), req.taskScopeId());
          success++;
        } catch (BusinessException ex) {
          failed++;
          insertImportError(taskId, batch.getId(), i + 1, null, "candidate_invalid", ex.getMessage());
        } catch (RuntimeException ex) {
          failed++;
          insertImportError(taskId, batch.getId(), i + 1, null, "candidate_import_failed",
              "导入失败，请检查培训学员信息是否完整");
        }
      }
    } else if (!trainingImport && !CollectionUtils.isEmpty(req.candidates())) {
      for (int i = 0; i < req.candidates().size(); i++) {
        try {
          createCandidate(taskId, req.candidates().get(i));
          success++;
        } catch (BusinessException ex) {
          failed++;
          insertImportError(taskId, batch.getId(), i + 1, req.candidates().get(i), "candidate_invalid", ex.getMessage());
        } catch (RuntimeException ex) {
          failed++;
          insertImportError(taskId, batch.getId(), i + 1, req.candidates().get(i), "candidate_import_failed", "导入失败，请检查学员信息是否重复或格式是否正确");
        }
      }
    }
    batch.setTotalCount(total);
    batch.setSuccessCount(success);
    batch.setFailedCount(failed);
    batch.setDuplicateCount(failed);
    examSessionService.assignBatchCandidates(taskId, batch.getId(), req.examSessionIds(), req.defaultParticipation());
    batch.setCompletedAt(LocalDateTime.now());
    importBatchMapper.updateById(batch);
    return PreparationImportBatchVO.from(batch);
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationImportErrorVO> pageImportErrors(Long taskId, Long batchId, PageQuery req) {
    findTask(taskId);
    Page<CertificationCandidateImportErrorEntity> page = importErrorMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CertificationCandidateImportErrorEntity>()
            .eq(CertificationCandidateImportErrorEntity::getTaskId, taskId)
            .eq(CertificationCandidateImportErrorEntity::getBatchId, batchId)
            .orderByAsc(CertificationCandidateImportErrorEntity::getRowNo)
    );
    return page(page, page.getRecords().stream().map(PreparationImportErrorVO::from).toList());
  }

  private void insertImportError(
      Long taskId,
      Long batchId,
      int rowNo,
      PreparationCandidateSaveReq candidate,
      String errorCode,
      String errorMessage
  ) {
    CertificationCandidateImportErrorEntity error = new CertificationCandidateImportErrorEntity();
    error.setTaskId(taskId);
    error.setBatchId(batchId);
    error.setRowNo(rowNo);
    error.setCandidateName(candidate == null ? null : candidate.candidateName());
    error.setIdCardNo(candidate == null ? null : candidate.idCardNo());
    error.setPhone(candidate == null ? null : candidate.phone());
    error.setErrorCode(errorCode);
    error.setErrorMessage(errorMessage);
    error.setDeleted(0);
    importErrorMapper.insert(error);
  }

  @Transactional
  public PageResponse<PreparationCandidateVO> pageCandidates(Long taskId, PreparationCandidateQueryReq req) {
    findTask(taskId);
    syncCandidateDocuments(taskId);
    LambdaQueryWrapper<CertificationCandidateEntity> wrapper = new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .orderByDesc(CertificationCandidateEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationCandidateEntity::getCandidateName, req.getKeyword())
          .or()
          .like(CertificationCandidateEntity::getIdCardNo, req.getKeyword())
          .or()
          .like(CertificationCandidateEntity::getPhone, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationCandidateEntity::getStatus, req.getStatus());
    }
    if (req.getTaskScopeId() != null) {
      wrapper.eq(CertificationCandidateEntity::getTaskScopeId, req.getTaskScopeId());
    }
    if (StringUtils.hasText(req.getLevelCode())) {
      wrapper.eq(CertificationCandidateEntity::getLevelCode, req.getLevelCode());
    }
    if (req.getDocumentComplete() != null) {
      wrapper.eq(CertificationCandidateEntity::getDocumentComplete, req.getDocumentComplete());
    }
    Page<CertificationCandidateEntity> page = candidateMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<PreparationCandidateVO> candidates = new ArrayList<>(
        page.getRecords().stream().map(PreparationCandidateVO::from).toList());
    enrichCandidateDocumentSummary(taskId, candidates);
    return page(page, candidates);
  }

  @Transactional
  public PreparationCandidateVO createCandidate(Long taskId, PreparationCandidateSaveReq req) {
    CertificationCandidateEntity entity = createCandidateForImport(taskId, req, null);
    if (shouldPersistApplicationFormSource(req.importSource())) {
      stagingApplicationService.upsertApplicationFormSource(taskId, entity, req);
    }
    return PreparationCandidateVO.from(entity);
  }

  /** Creates a candidate for a specific import batch so material generation can remain batch-scoped. */
  @Transactional
  public CertificationCandidateEntity createCandidateForImport(Long taskId, PreparationCandidateSaveReq req, Long importBatchId) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationCandidateEntity existing = null;
    if (StringUtils.hasText(req.idCardNo())) {
      existing = candidateMapper.selectByTaskAndIdCardIncludingDeleted(
          task.getTenantId(), taskId, req.idCardNo());
    }
    if (existing == null && StringUtils.hasText(req.admissionTicketNo())) {
      existing = candidateMapper.selectByTaskAndAdmissionTicketIncludingDeleted(
          task.getTenantId(), taskId, req.admissionTicketNo());
    }
    validateCandidate(task, req, importBatchId == null || existing == null ? null : existing.getId());
    if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
      if (importBatchId != null) {
        applyCandidate(existing, task, req);
        existing.setImportBatchId(importBatchId);
        existing.setStatus("active");
        existing.setDeleted(0);
        candidateMapper.updateById(existing);
        initializeCandidateDocuments(taskId, existing.getId());
        touchTask(taskId);
        return existing;
      }
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下身份证号不得重复");
    }
    CertificationCandidateEntity entity;
    if (existing != null) {
      entity = existing;
      entity.setTaskId(taskId);
      applyCandidate(entity, task, req);
      entity.setCandidateNo("C-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
      entity.setDocumentCompletedCount(0);
      entity.setDocumentRequiredCount(CANDIDATE_DOC_CODES.size());
      entity.setDocumentComplete(false);
      entity.setStatus("active");
      entity.setDeleted(0);
      entity.setImportBatchId(importBatchId);
      candidateMapper.restoreSoftDeleted(entity);
    } else {
      entity = new CertificationCandidateEntity();
      applyCandidate(entity, task, req);
      entity.setTaskId(taskId);
      entity.setCandidateNo("C-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
      entity.setDocumentCompletedCount(0);
      entity.setDocumentRequiredCount(CANDIDATE_DOC_CODES.size());
      entity.setDocumentComplete(false);
      entity.setStatus("active");
      entity.setDeleted(0);
      entity.setImportBatchId(importBatchId);
      candidateMapper.insert(entity);
    }
    if (entity.getId() != null) {
      initializeCandidateDocuments(taskId, entity.getId());
    }
    touchTask(taskId);
    return entity;
  }

  @Transactional
  public PreparationCandidateVO updateCandidate(Long taskId, Long candidateId, PreparationCandidateSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationCandidateEntity entity = requireCandidate(taskId, candidateId);
    validateCandidate(task, req, candidateId);
    applyCandidate(entity, task, req);
    candidateMapper.updateById(entity);
    if (shouldPersistApplicationFormSource(req.importSource())
        || shouldPersistApplicationFormSource(entity.getImportSource())
        || stagingApplicationService.findStagingByCandidate(taskId, candidateId) != null) {
      stagingApplicationService.upsertApplicationFormSource(taskId, entity, req);
    }
    touchTask(taskId);
    return PreparationCandidateVO.from(entity);
  }

  @Transactional(readOnly = true)
  public PreparationCandidateStagingVO getCandidateApplicationSource(Long taskId, Long candidateId) {
    findTask(taskId);
    requireCandidate(taskId, candidateId);
    CertificationCandidateSourceStagingEntity staging =
        stagingApplicationService.findStagingByCandidate(taskId, candidateId);
    return staging == null ? null : PreparationCandidateStagingVO.from(staging);
  }

  private boolean shouldPersistApplicationFormSource(String importSource) {
    if (!StringUtils.hasText(importSource)) {
      return true;
    }
    String normalized = importSource.trim().toLowerCase(Locale.ROOT);
    return "other".equals(normalized) || "manual".equals(normalized);
  }

  @Transactional
  public void deleteCandidate(Long taskId, Long candidateId) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationCandidateEntity candidate = requireCandidate(taskId, candidateId);
    long generated = safeCount(materialItemMapper.selectCount(new LambdaQueryWrapper<CertificationApplicationMaterialItemEntity>()
        .eq(CertificationApplicationMaterialItemEntity::getTaskId, taskId)
        .eq(CertificationApplicationMaterialItemEntity::getCandidateId, candidateId)
        .eq(CertificationApplicationMaterialItemEntity::getStatus, "generated")));
    if (generated > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已进入后续流程的考生不得删除");
    }
    Long tenantId = candidate.getTenantId() != null ? candidate.getTenantId() : task.getTenantId();
    if (StringUtils.hasText(candidate.getIdCardNo())) {
      candidateMapper.purgeSoftDeletedByTaskAndIdCard(tenantId, taskId, candidate.getIdCardNo());
    }
    documentMapper.physicalDeleteByCandidate(taskId, candidateId);
    stagingApplicationService.releasePromoteByCandidate(taskId, candidateId);
    candidateMapper.physicalDeleteById(candidateId);
    touchTask(taskId);
  }

  @Transactional
  public PageResponse<PreparationCandidateDocumentVO> pageCandidateDocuments(Long taskId, PreparationDocumentQueryReq req) {
    findTask(taskId);
    syncCandidateDocuments(taskId);
    LambdaQueryWrapper<CertificationCandidateDocumentEntity> wrapper =
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getDocCode, CANDIDATE_DOC_CODES)
            .orderByAsc(CertificationCandidateDocumentEntity::getCandidateId)
            .orderByAsc(CertificationCandidateDocumentEntity::getDocCode);
    if (req.getCandidateId() != null) {
      wrapper.eq(CertificationCandidateDocumentEntity::getCandidateId, req.getCandidateId());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationCandidateDocumentEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      applyDocumentKeywordFilter(taskId, req.getKeyword(), wrapper);
    }
    Page<CertificationCandidateDocumentEntity> page =
        documentMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    Map<Long, CertificationCandidateEntity> candidateMap = loadCandidateMap(taskId, page.getRecords());
    return page(page, page.getRecords().stream()
        .map(record -> PreparationCandidateDocumentVO.from(record, candidateMap.get(record.getCandidateId())))
        .toList());
  }

  @Transactional
  public PreparationCandidateDocumentVO generateCandidateDocument(Long taskId, Long candidateId, String docCode) {
    findTask(taskId);
    CertificationCandidateEntity candidate = requireCandidate(taskId, candidateId);
    if (!"DOC001".equals(docCode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持生成报名信息表");
    }
    CertificationCandidateDocumentEntity photoDocument = documentMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateDocumentEntity::getDocCode, "DOC002"));
    if (photoDocument == null || !"approved".equals(photoDocument.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先上传并审核通过两寸证件照");
    }
    long identityImages = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
        .in(CertificationCandidateDocumentEntity::getDocCode, List.of("DOC003", "DOC004"))
        .eq(CertificationCandidateDocumentEntity::getStatus, "approved")));
    if (identityImages != 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先上传并审核通过身份证正反面");
    }
    CertificationCandidateSourceStagingEntity staging = resolveApplicationFormSource(taskId, candidate, photoDocument);
    byte[] portrait = imageSourceService.readFile(photoDocument.getFileId());
    if (portrait == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "两寸证件照文件不可读取，请重新上传");
    }
    byte[] content = applicationFormGenerationService.generateApplicationForm(staging, portrait);
    String fileName = staging.getCandidateName() + "-职业技能等级认定申请表.docx";
    FileVO file = fileApplicationService.storeFromBytes(
        content,
        fileName,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "private");
    CertificationCandidateDocumentEntity entity = documentMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateDocumentEntity::getDocCode, "DOC001"));
    if (entity == null) {
      entity = new CertificationCandidateDocumentEntity();
      entity.setTaskId(taskId);
      entity.setCandidateId(candidateId);
      entity.setDocCode("DOC001");
      entity.setDocName("报名信息表");
      entity.setRequired(true);
      entity.setVersionNo(1);
      entity.setDeleted(0);
    } else {
      entity.setVersionNo((entity.getVersionNo() == null ? 0 : entity.getVersionNo()) + 1);
    }
    entity.setFileId(file.id());
    entity.setFileName(file.originalName());
    entity.setFileType(file.contentType());
    entity.setStatus("approved");
    entity.setUploadedAt(LocalDateTime.now());
    entity.setReviewedAt(LocalDateTime.now());
    entity.setReviewComment("系统生成");
    if (entity.getId() == null) {
      documentMapper.insert(entity);
    } else {
      documentMapper.updateById(entity);
    }
    refreshCandidateDocumentStats(taskId, candidateId);
    touchTask(taskId);
    return PreparationCandidateDocumentVO.from(entity);
  }

  private CertificationCandidateSourceStagingEntity resolveApplicationFormSource(
      Long taskId,
      CertificationCandidateEntity candidate,
      CertificationCandidateDocumentEntity photoDocument
  ) {
    CertificationCandidateSourceStagingEntity source =
        stagingApplicationService.findStagingByCandidate(taskId, candidate.getId());
    if (source != null) {
      return source;
    }

    // Historical/manual candidates predate the source snapshot. Their approved documents and
    // current candidate fields are still authoritative for generating this one application form.
    CertificationCandidateSourceStagingEntity fallback = new CertificationCandidateSourceStagingEntity();
    fallback.setCandidateName(candidate.getCandidateName());
    fallback.setIdCardNo(candidate.getIdCardNo());
    fallback.setPhone(candidate.getPhone());
    fallback.setCompanyName(candidate.getOrgName());
    fallback.setOriginalWorkTypeName(candidate.getWorkTypeName());
    fallback.setApplyWorkTypeId(candidate.getWorkTypeId());
    fallback.setApplyWorkTypeName(candidate.getWorkTypeName());
    fallback.setCertWorkTypeId(candidate.getWorkTypeId());
    fallback.setCertWorkTypeName(candidate.getWorkTypeName());
    fallback.setApplyLevelCode(candidate.getLevelCode());
    fallback.setApplyLevelName(candidate.getLevelName());
    fallback.setCertLevelName(candidate.getLevelName());
    fallback.setRemark(candidate.getRemark());
    fallback.setPhotoUrl(fileReference(photoDocument));
    fallback.setIdCardFrontUrl(candidateDocumentFileReference(taskId, candidate.getId(), "DOC003"));
    fallback.setIdCardBackUrl(candidateDocumentFileReference(taskId, candidate.getId(), "DOC004"));

    CertificationTaskScopeEntity scope = candidate.getTaskScopeId() == null
        ? firstScope(taskId)
        : scopeMapper.selectById(candidate.getTaskScopeId());
    if (scope != null && taskId.equals(scope.getTaskId())) {
      if (!StringUtils.hasText(fallback.getApplyWorkTypeName())) {
        fallback.setApplyWorkTypeId(scope.getWorkTypeId());
        fallback.setApplyWorkTypeName(scope.getWorkTypeName());
        fallback.setCertWorkTypeId(scope.getWorkTypeId());
        fallback.setCertWorkTypeName(scope.getWorkTypeName());
      }
      if (!StringUtils.hasText(fallback.getApplyLevelName())) {
        fallback.setApplyLevelCode(scope.getLevelCode());
        fallback.setApplyLevelName(scope.getLevelName());
        fallback.setCertLevelName(scope.getLevelName());
      }
    }
    return fallback;
  }

  private String candidateDocumentFileReference(Long taskId, Long candidateId, String docCode) {
    CertificationCandidateDocumentEntity document = documentMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
            .eq(CertificationCandidateDocumentEntity::getDocCode, docCode));
    return document != null && "approved".equals(document.getStatus()) ? fileReference(document) : null;
  }

  private String fileReference(CertificationCandidateDocumentEntity document) {
    return document == null || document.getFileId() == null ? null : "file-id:" + document.getFileId();
  }

  @Transactional
  public PreparationCandidateDocumentVO saveCandidateDocument(Long taskId, PreparationCandidateDocumentSaveReq req) {
    findTask(taskId);
    requireCandidate(taskId, req.candidateId());
    if (!CANDIDATE_DOC_CODES.contains(req.docCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的学员资料类型");
    }
    if ("DOC001".equals(req.docCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "报名信息表请使用一键生成，不支持手工上传");
    }
    validateCandidateDocumentFile(req.docCode(), req.fileName(), req.fileType());
    CertificationCandidateDocumentEntity entity = documentMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .eq(CertificationCandidateDocumentEntity::getCandidateId, req.candidateId())
            .eq(CertificationCandidateDocumentEntity::getDocCode, req.docCode()));
    if (entity == null) {
      entity = new CertificationCandidateDocumentEntity();
      entity.setTaskId(taskId);
      entity.setCandidateId(req.candidateId());
      entity.setDocCode(req.docCode());
      entity.setDocName(req.docName());
      entity.setRequired(req.required() == null || req.required());
      entity.setVersionNo(1);
      entity.setDeleted(0);
    } else {
      entity.setVersionNo((entity.getVersionNo() == null ? 0 : entity.getVersionNo()) + 1);
    }
    entity.setFileId(req.fileId());
    entity.setFileName(req.fileName());
    entity.setFileType(req.fileType());
    if (req.required() != null) {
      entity.setRequired(req.required());
    }
    boolean autoApprovedImage = AUTO_APPROVED_IMAGE_DOC_CODES.contains(req.docCode()) && req.fileId() != null;
    entity.setStatus(req.fileId() == null ? "missing" : (autoApprovedImage ? INFO_APPROVED : "pending_review"));
    entity.setUploadedAt(req.fileId() == null ? null : LocalDateTime.now());
    entity.setReviewedAt(autoApprovedImage ? LocalDateTime.now() : null);
    entity.setReviewComment(autoApprovedImage ? "图片上传后自动通过，无需审核" : null);
    entity.setRemark(req.remark());
    if (entity.getId() == null) {
      documentMapper.insert(entity);
    } else {
      documentMapper.updateById(entity);
    }
    stagingApplicationService.updateCandidateDocumentReference(taskId, req.candidateId(), req.docCode(), req.fileId());
    refreshCandidateDocumentStats(taskId, req.candidateId());
    return PreparationCandidateDocumentVO.from(entity);
  }

  @Transactional
  public void deleteCandidateDocument(Long taskId, Long documentId) {
    findTask(taskId);
    CertificationCandidateDocumentEntity entity = documentMapper.selectById(documentId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生资料不存在");
    }
    if (!"rejected".equals(entity.getStatus()) && entity.getFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前资料无需删除");
    }
    entity.setFileId(null);
    entity.setFileName(null);
    entity.setFileType(null);
    entity.setStatus("missing");
    entity.setUploadedAt(null);
    entity.setReviewedAt(null);
    entity.setReviewComment(null);
    entity.setRemark(null);
    documentMapper.updateById(entity);
    refreshCandidateDocumentStats(taskId, entity.getCandidateId());
    touchTask(taskId);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> downloadCandidateDocument(Long taskId, Long documentId) {
    findTask(taskId);
    CertificationCandidateDocumentEntity entity = documentMapper.selectById(documentId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生资料不存在");
    }
    if (entity.getFileId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件尚未上传");
    }
    return fileApplicationService.buildDownloadResponse(entity.getFileId(), true);
  }

  @Transactional
  public PreparationCandidateDocumentVO reviewCandidateDocument(Long taskId, Long documentId, PreparationDocumentReviewReq req) {
    findTask(taskId);
    CertificationCandidateDocumentEntity entity = documentMapper.selectById(documentId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生资料不存在");
    }
    if (AUTO_APPROVED_IMAGE_DOC_CODES.contains(entity.getDocCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证及证件照上传后无需审核，可直接修改");
    }
    entity.setStatus(req.status());
    entity.setReviewComment(req.reviewComment());
    entity.setReviewedAt(LocalDateTime.now());
    documentMapper.updateById(entity);
    refreshCandidateDocumentStats(taskId, entity.getCandidateId());
    return PreparationCandidateDocumentVO.from(entity);
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationMaterialBatchVO> pageMaterials(Long taskId, PreparationMaterialQueryReq req) {
    findTask(taskId);
    LambdaQueryWrapper<CertificationApplicationMaterialBatchEntity> wrapper =
        new LambdaQueryWrapper<CertificationApplicationMaterialBatchEntity>()
            .eq(CertificationApplicationMaterialBatchEntity::getTaskId, taskId)
            .orderByDesc(CertificationApplicationMaterialBatchEntity::getCreatedAt);
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationApplicationMaterialBatchEntity::getStatus, req.getStatus());
    }
    Page<CertificationApplicationMaterialBatchEntity> page =
        materialBatchMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return page(page, page.getRecords().stream().map(PreparationMaterialBatchVO::from).toList());
  }

  @Transactional
  public PreparationMaterialItemVO createApplicationMaterial(Long taskId, PreparationMaterialItemSaveReq req) {
    findTask(taskId);
    CertificationApplicationMaterialItemEntity entity = new CertificationApplicationMaterialItemEntity();
    entity.setTaskId(taskId);
    entity.setCandidateId(req.candidateId());
    entity.setItemCode(req.itemCode());
    entity.setItemName(req.itemName());
    entity.setRequired(req.required() == null || req.required());
    entity.setFileId(req.fileId());
    entity.setFileName(req.fileName());
    entity.setVersionNo(1);
    entity.setStatus(req.fileId() == null ? "missing" : "uploaded");
    entity.setRemark(req.remark());
    entity.setDeleted(0);
    materialItemMapper.insert(entity);
    return PreparationMaterialItemVO.from(entity);
  }

  @Transactional
  public PreparationMaterialBatchVO generateApplicationMaterials(Long taskId, PreparationMaterialGenerateReq req) {
    findTask(taskId);
    syncCandidateDocuments(taskId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getStatus, "active"));
    if (CollectionUtils.isEmpty(candidates)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前任务下不存在考生，不能生成申报材料");
    }
    Set<Long> requestedCandidateIds = CollectionUtils.isEmpty(req.candidateIds())
        ? candidates.stream().map(CertificationCandidateEntity::getId).collect(Collectors.toSet())
        : Set.copyOf(req.candidateIds());
    List<Long> activeCandidateIds = candidates.stream().map(CertificationCandidateEntity::getId).toList();
    List<Long> selectedCandidateIds = activeCandidateIds.stream().filter(requestedCandidateIds::contains).toList();
    if (selectedCandidateIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未选择可生成申报材料的考生");
    }
    generateMissingDirectRecognitionForms(taskId, candidates, selectedCandidateIds);
    long incompleteDocs = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .in(CertificationCandidateDocumentEntity::getCandidateId, selectedCandidateIds)
        .eq(CertificationCandidateDocumentEntity::getRequired, true)
        .ne(CertificationCandidateDocumentEntity::getStatus, "approved")));
    if (incompleteDocs > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "存在未审核通过的必传资料，不能生成申报材料");
    }
    List<CertificationCandidateEntity> readyCandidates = candidates.stream()
        .filter(candidate -> requestedCandidateIds.contains(candidate.getId()))
        .toList();
    if (readyCandidates.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "未选择可生成申报材料的考生");
    int requested = readyCandidates.size();
    CertificationApplicationMaterialBatchEntity batch = new CertificationApplicationMaterialBatchEntity();
    batch.setTaskId(taskId);
    batch.setBatchNo("AMB-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
    batch.setBatchType("generate");
    batch.setGenerateScope(StringUtils.hasText(req.generateScope()) ? req.generateScope() : "all");
    batch.setCandidateCount(requested);
    batch.setSuccessCount(requested);
    batch.setFailedCount(0);
    batch.setStatus("generated");
    batch.setPackageFileName(batch.getBatchNo() + ".zip");
    batch.setRemark(req.remark());
    batch.setDeleted(0);
    materialBatchMapper.insert(batch);
    copyApprovedCandidateDocumentsToMaterialItems(taskId, batch.getId(), readyCandidates);
    return PreparationMaterialBatchVO.from(batch);
  }

  /**
   * Direct-recognition imports carry their application data in a frozen Excel snapshot. Once the
   * required images are approved, the registration form is deterministic and should be generated
   * as part of the one-click material action instead of forcing a separate manual operation.
   */
  private void generateMissingDirectRecognitionForms(
      Long taskId,
      List<CertificationCandidateEntity> candidates,
      List<Long> selectedCandidateIds
  ) {
    if (CollectionUtils.isEmpty(candidates) || CollectionUtils.isEmpty(selectedCandidateIds)) {
      return;
    }
    Set<Long> selected = Set.copyOf(selectedCandidateIds);
    for (CertificationCandidateEntity candidate : candidates) {
      if (candidate.getId() == null || !selected.contains(candidate.getId())
          || !"other_xlsx".equals(candidate.getImportSource())) {
        continue;
      }
      long generatedForm = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
          .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
          .eq(CertificationCandidateDocumentEntity::getCandidateId, candidate.getId())
          .eq(CertificationCandidateDocumentEntity::getDocCode, "DOC001")
          .eq(CertificationCandidateDocumentEntity::getStatus, "approved")));
      if (generatedForm == 0) {
        generateCandidateDocument(taskId, candidate.getId(), "DOC001");
      }
    }
  }

  @Transactional
  public PreparationMaterialBatchVO generateApplicationMaterialsForImportBatch(Long taskId, Long importBatchId, String remark) {
    CertificationCandidateImportBatchEntity importBatch = importBatchMapper.selectById(importBatchId);
    if (importBatch == null || !taskId.equals(importBatch.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生导入批次不存在");
    }
    List<Long> candidateIds = candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getImportBatchId, importBatchId)
            .eq(CertificationCandidateEntity::getStatus, "active"))
        .stream().map(CertificationCandidateEntity::getId).filter(Objects::nonNull).toList();
    if (candidateIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该导入批次没有可生成申报材料的考生");
    }
    return generateApplicationMaterials(taskId,
        new PreparationMaterialGenerateReq("import_batch", candidateIds, remark));
  }

  private void copyApprovedCandidateDocumentsToMaterialItems(
      Long taskId,
      Long batchId,
      List<CertificationCandidateEntity> readyCandidates
  ) {
    if (batchId == null || CollectionUtils.isEmpty(readyCandidates)) {
      return;
    }
    Set<Long> readyCandidateIds = readyCandidates.stream()
        .map(CertificationCandidateEntity::getId)
        .collect(Collectors.toSet());
    List<CertificationCandidateDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, readyCandidateIds)
            .in(CertificationCandidateDocumentEntity::getDocCode, CANDIDATE_DOC_CODES)
            .eq(CertificationCandidateDocumentEntity::getStatus, "approved"));
    if (CollectionUtils.isEmpty(documents)) {
      return;
    }
    Long tenantId = readyCandidates.get(0).getTenantId();
    List<CertificationApplicationMaterialItemEntity> items = new ArrayList<>(documents.size());
    for (CertificationCandidateDocumentEntity document : documents) {
      CertificationApplicationMaterialItemEntity item = new CertificationApplicationMaterialItemEntity();
      item.setTenantId(tenantId);
      item.setTaskId(taskId);
      item.setBatchId(batchId);
      item.setCandidateId(document.getCandidateId());
      item.setTemplateId(document.getTemplateId());
      item.setItemCode(document.getDocCode());
      item.setItemName(document.getDocName());
      item.setRequired(document.getRequired());
      item.setFileId(document.getFileId());
      item.setFileName(document.getFileName());
      item.setVersionNo(1);
      item.setStatus("generated");
      item.setDeleted(0);
      items.add(item);
    }
    materialItemMapper.batchInsertGenerated(items);
  }

  @Transactional(readOnly = true)
  public PreparationMaterialBatchVO getMaterialBatch(Long taskId, Long batchId) {
    findTask(taskId);
    CertificationApplicationMaterialBatchEntity entity = materialBatchMapper.selectById(batchId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "申报材料批次不存在");
    }
    List<PreparationMaterialItemVO> items = materialItemMapper.selectList(
            new LambdaQueryWrapper<CertificationApplicationMaterialItemEntity>()
                .eq(CertificationApplicationMaterialItemEntity::getTaskId, taskId)
                .eq(CertificationApplicationMaterialItemEntity::getBatchId, batchId)
                .orderByAsc(CertificationApplicationMaterialItemEntity::getCandidateId)
                .orderByAsc(CertificationApplicationMaterialItemEntity::getItemCode))
        .stream()
        .map(PreparationMaterialItemVO::from)
        .toList();
    return PreparationMaterialBatchVO.from(entity, items);
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationExamSiteVO> pageExamSites(PreparationExamSiteQueryReq req) {
    LambdaQueryWrapper<CertificationExamSiteEntity> wrapper = new LambdaQueryWrapper<CertificationExamSiteEntity>()
        .orderByDesc(CertificationExamSiteEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationExamSiteEntity::getSiteName, req.getKeyword())
          .or()
          .like(CertificationExamSiteEntity::getAddress, req.getKeyword())
          .or()
          .like(CertificationExamSiteEntity::getFilingNo, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getSiteType())) {
      wrapper.eq(CertificationExamSiteEntity::getSiteType, req.getSiteType());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationExamSiteEntity::getStatus, req.getStatus());
    }
    Page<CertificationExamSiteEntity> page = examSiteMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return page(page, page.getRecords().stream().map(PreparationExamSiteVO::from).toList());
  }

  @Transactional
  public PreparationExamSiteVO createExamSite(PreparationExamSiteSaveReq req) {
    CertificationExamSiteEntity entity = new CertificationExamSiteEntity();
    applyExamSite(entity, req);
    entity.setDeleted(0);
    examSiteMapper.insert(entity);
    return PreparationExamSiteVO.from(entity);
  }

  @Transactional
  public PreparationExamSiteVO updateExamSite(Long siteId, PreparationExamSiteSaveReq req) {
    CertificationExamSiteEntity entity = requireExamSite(siteId);
    applyExamSite(entity, req);
    examSiteMapper.updateById(entity);
    return PreparationExamSiteVO.from(entity);
  }

  @Transactional
  public PreparationTaskExamSiteVO assignExamSite(Long taskId, PreparationTaskExamSiteSaveReq req) {
    findTask(taskId);
    CertificationExamSiteEntity site = requireExamSite(req.siteId());
    if (!List.of("active", "idle").contains(site.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "停用或使用中的考场不得绑定到新任务");
    }
    int arrangedCount = req.arrangedCount() == null ? 0 : req.arrangedCount();
    if (arrangedCount > nullToZero(site.getSeatCount())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "安排人数不得超过考场容量");
    }
    long duplicate = safeCount(taskExamSiteMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
        .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)
        .eq(CertificationTaskExamSiteEntity::getSiteId, site.getId())));
    if (duplicate > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下同一考场不得重复配置");
    }
    CertificationTaskExamSiteEntity entity = new CertificationTaskExamSiteEntity();
    entity.setTaskId(taskId);
    entity.setSiteId(site.getId());
    entity.setSiteName(site.getSiteName());
    entity.setSiteType(site.getSiteType());
    entity.setAddress(site.getAddress());
    entity.setSeatCount(site.getSeatCount());
    entity.setFilingNo(site.getFilingNo());
    entity.setExamDate(parseDate(req.examDate()));
    entity.setStartTime(req.startTime());
    entity.setEndTime(req.endTime());
    entity.setExamType(resolveTaskExamType(req.examType()));
    entity.setArrangedCount(arrangedCount);
    entity.setContactName(StringUtils.hasText(req.contactName()) ? req.contactName() : site.getContactName());
    entity.setContactPhone(StringUtils.hasText(req.contactPhone()) ? req.contactPhone() : site.getContactPhone());
    entity.setStatus("configured");
    entity.setSortOrder(100);
    entity.setRemark(req.remark());
    entity.setDeleted(0);
    taskExamSiteMapper.insert(entity);
    site.setStatus("in_use");
    examSiteMapper.updateById(site);
    touchTask(taskId);
    return PreparationTaskExamSiteVO.from(entity);
  }

  @Transactional(readOnly = true)
  public List<PreparationTaskExamSiteVO> listTaskExamSites(Long taskId) {
    findTask(taskId);
    return taskExamSiteMapper.selectList(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
            .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskExamSiteEntity::getExamDate)
            .orderByAsc(CertificationTaskExamSiteEntity::getStartTime)
            .orderByDesc(CertificationTaskExamSiteEntity::getCreatedAt))
        .stream()
        .map(PreparationTaskExamSiteVO::from)
        .toList();
  }

  @Transactional
  public void removeTaskExamSite(Long taskId, Long taskSiteId) {
    findTask(taskId);
    CertificationTaskExamSiteEntity binding = taskExamSiteMapper.selectById(taskSiteId);
    if (binding == null || !taskId.equals(binding.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务考场配置不存在");
    }
    taskExamSiteMapper.deleteById(taskSiteId);
    CertificationExamSiteEntity site = examSiteMapper.selectById(binding.getSiteId());
    if (site != null && !"disabled".equalsIgnoreCase(site.getStatus())
        && !"maintenance".equalsIgnoreCase(site.getStatus())) {
      site.setStatus("idle");
      examSiteMapper.updateById(site);
    }
    touchTask(taskId);
  }

  @Transactional
  public PreparationTaskExamStaffVO assignExamStaff(Long taskId, PreparationTaskExamStaffSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationInfoExamStaffEntity staff = requireInfoExamStaff(req.staffId());
    ExamStaffRole role = resolveExamStaffRole(req.taskRole());
    CertificationTaskExamStaffEntity existing = taskExamStaffMapper.selectByTaskAndStaffIncludingDeleted(
        task.getTenantId(), taskId, staff.getId());
    if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下同一考务人员不得重复配置");
    }
    clearInternalSupervisorIfNeeded(taskId, role);
    CertificationTaskExamStaffEntity entity;
    if (existing != null) {
      entity = existing;
      entity.setStaffName(staff.getStaffName());
      entity.setPhone(staff.getPhone());
      entity.setMaskedPhone(StringUtils.hasText(staff.getMaskedPhone()) ? staff.getMaskedPhone() : SensitiveMaskUtils.maskPhone(staff.getPhone()));
      entity.setRemark(req.remark());
      entity.setStatus("configured");
      applyExamStaffRole(entity, role);
      entity.setSortOrder(100);
      entity.setDeleted(0);
      taskExamStaffMapper.restoreSoftDeleted(entity);
      staff.setReferencedCount(nullToZero(staff.getReferencedCount()) + 1);
      staff.setWorkStatus("assigned");
      infoExamStaffMapper.updateById(staff);
    } else {
      entity = new CertificationTaskExamStaffEntity();
      entity.setTaskId(taskId);
      entity.setStaffId(staff.getId());
      entity.setStaffName(staff.getStaffName());
      entity.setPhone(staff.getPhone());
      entity.setMaskedPhone(StringUtils.hasText(staff.getMaskedPhone()) ? staff.getMaskedPhone() : SensitiveMaskUtils.maskPhone(staff.getPhone()));
      entity.setRemark(req.remark());
      entity.setStatus("configured");
      applyExamStaffRole(entity, role);
      entity.setSortOrder(100);
      entity.setDeleted(0);
      taskExamStaffMapper.insert(entity);
      staff.setReferencedCount(nullToZero(staff.getReferencedCount()) + 1);
      staff.setWorkStatus("assigned");
      infoExamStaffMapper.updateById(staff);
    }
    touchTask(taskId);
    return PreparationTaskExamStaffVO.from(entity);
  }

  @Transactional
  public PreparationTaskExamStaffVO appointExamStaffLeader(Long taskId, Long taskStaffId) {
    findTask(taskId);
    CertificationTaskExamStaffEntity entity = taskExamStaffMapper.selectById(taskStaffId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务考务配置不存在");
    }
    ExamStaffRole role = resolveExamStaffRole(EXAM_STAFF_ROLE_INTERNAL_SUPERVISOR);
    clearInternalSupervisorIfNeeded(taskId, role);
    applyExamStaffRole(entity, role);
    taskExamStaffMapper.updateById(entity);
    touchTask(taskId);
    return PreparationTaskExamStaffVO.from(entity);
  }

  @Transactional
  public PreparationTaskExamStaffVO updateExamStaffRole(Long taskId, Long taskStaffId, PreparationTaskExamStaffRoleReq req) {
    findTask(taskId);
    CertificationTaskExamStaffEntity entity = taskExamStaffMapper.selectById(taskStaffId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务考务配置不存在");
    }
    ExamStaffRole role = resolveExamStaffRole(req.taskRole());
    clearInternalSupervisorIfNeeded(taskId, role);
    applyExamStaffRole(entity, role);
    taskExamStaffMapper.updateById(entity);
    touchTask(taskId);
    return PreparationTaskExamStaffVO.from(entity);
  }

  @Transactional(readOnly = true)
  public List<PreparationTaskExamStaffVO> listTaskExamStaff(Long taskId) {
    findTask(taskId);
    return taskExamStaffMapper.selectList(new LambdaQueryWrapper<CertificationTaskExamStaffEntity>()
            .eq(CertificationTaskExamStaffEntity::getTaskId, taskId)
            .orderByDesc(CertificationTaskExamStaffEntity::getCreatedAt))
        .stream()
        .map(PreparationTaskExamStaffVO::from)
        .toList();
  }

  @Transactional
  public PreparationChecklistVO getChecklist(Long taskId) {
    CertificationTaskEntity task = findTask(taskId);
    ensureChecklistItems(task);
    return buildChecklist(task);
  }

  @Transactional
  public PreparationChecklistVO updateChecklist(Long taskId, PreparationChecklistUpdateReq req) {
    CertificationTaskEntity task = findTask(taskId);
    ensureChecklistItems(task);
    List<CertificationTaskChecklistItemEntity> items = listChecklistEntities(taskId);
    Set<Long> checkedItemIds = Set.copyOf(req.checkedItemIds() == null ? List.of() : req.checkedItemIds());
    if (checkedItemIds.size() != items.size() || items.stream().anyMatch(item -> !checkedItemIds.contains(item.getId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "清单材料必须全部勾选后才可以确认");
    }
    if (req.confirmerTaskStaffId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择确认人");
    }
    CertificationTaskExamStaffEntity confirmer = taskExamStaffMapper.selectById(req.confirmerTaskStaffId());
    if (confirmer == null || !taskId.equals(confirmer.getTaskId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "确认人必须来自当前任务已配置的考务人员");
    }
    LocalDateTime confirmedAt = LocalDateTime.now();
    CertificationTaskChecklistItemEntity patch = new CertificationTaskChecklistItemEntity();
    patch.setChecked(1);
    patch.setConfirmerTaskStaffId(confirmer.getId());
    patch.setConfirmerName(confirmer.getStaffName());
    patch.setConfirmedAt(confirmedAt);
    patch.setRemark(req.remark());
    List<Long> itemIds = items.stream()
        .map(CertificationTaskChecklistItemEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (!itemIds.isEmpty()) {
      taskChecklistItemMapper.update(patch, new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
          .in(CertificationTaskChecklistItemEntity::getId, itemIds));
    }
    touchTask(taskId);
    return buildChecklist(task);
  }

  @Transactional
  public void removeTaskExamStaff(Long taskId, Long taskStaffId) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationTaskExamStaffEntity entity = taskExamStaffMapper.selectById(taskStaffId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务考务配置不存在");
    }
    if (entity.getStaffId() != null) {
      taskExamStaffMapper.purgeSoftDeletedByTaskAndStaff(task.getTenantId(), taskId, entity.getStaffId());
      CertificationInfoExamStaffEntity staff = infoExamStaffMapper.selectById(entity.getStaffId());
      if (staff != null) {
        int next = Math.max(0, nullToZero(staff.getReferencedCount()) - 1);
        staff.setReferencedCount(next);
        infoExamStaffMapper.updateById(staff);
      }
    }
    taskExamStaffMapper.deleteById(taskStaffId);
    releaseInfoExamStaffIfIdle(entity.getStaffId());
    touchTask(taskId);
  }

  @Transactional(readOnly = true)
  public List<PreparationTaskEquipmentVO> listTaskEquipment(Long taskId) {
    findTask(taskId);
    List<CertificationTaskEquipmentEntity> bindings = taskEquipmentMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
            .eq(CertificationTaskEquipmentEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskEquipmentEntity::getSortOrder)
            .orderByDesc(CertificationTaskEquipmentEntity::getCreatedAt));
    if (bindings.isEmpty()) {
      return List.of();
    }
    Set<Long> equipmentIds = bindings.stream()
        .map(CertificationTaskEquipmentEntity::getEquipmentId)
        .collect(Collectors.toSet());
    Map<Long, CertificationInfoExamEquipmentEntity> equipmentMap = infoEquipmentMapper.selectBatchIds(equipmentIds)
        .stream()
        .collect(Collectors.toMap(CertificationInfoExamEquipmentEntity::getId, equipment -> equipment));
    return bindings.stream()
        .map(binding -> PreparationTaskEquipmentVO.from(binding, equipmentMap.get(binding.getEquipmentId())))
        .toList();
  }

  @Transactional
  public PreparationTaskEquipmentVO assignTaskEquipment(Long taskId, PreparationTaskEquipmentSaveReq req) {
    return bindTaskEquipment(taskId, req);
  }

  @Transactional
  public PreparationTaskEquipmentVO bindTaskEquipment(Long taskId, PreparationTaskEquipmentSaveReq req) {
    findTask(taskId);
    CertificationInfoExamEquipmentEntity equipment = requireInfoEquipment(req.equipmentId());
    long duplicate = safeCount(taskEquipmentMapper.selectCount(new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
        .eq(CertificationTaskEquipmentEntity::getTaskId, taskId)
        .eq(CertificationTaskEquipmentEntity::getEquipmentId, equipment.getId())));
    if (duplicate > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下同一设备不得重复配置");
    }
    CertificationTaskEquipmentEntity entity = new CertificationTaskEquipmentEntity();
    entity.setTaskId(taskId);
    entity.setEquipmentId(equipment.getId());
    entity.setEquipmentName(equipment.getEquipmentType());
    entity.setEquipmentNo(equipment.getAssetNo());
    entity.setEquipmentQuantity(nullToOne(equipment.getEquipmentQuantity()));
    entity.setStatus("configured");
    entity.setRemark(req.remark());
    entity.setSortOrder(100);
    entity.setDeleted(0);
    taskEquipmentMapper.insert(entity);
    refreshInfoEquipmentUsage(equipment.getId());
    touchTask(taskId);
    return PreparationTaskEquipmentVO.from(entity, equipment);
  }

  @Transactional
  public void removeTaskEquipment(Long taskId, Long taskEquipmentId) {
    findTask(taskId);
    CertificationTaskEquipmentEntity entity = taskEquipmentMapper.selectById(taskEquipmentId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务设备配置不存在");
    }
    Long equipmentId = entity.getEquipmentId();
    taskEquipmentMapper.deleteById(taskEquipmentId);
    refreshInfoEquipmentUsage(equipmentId);
    touchTask(taskId);
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationAssessorVO> pageAssessors(PreparationAssessorQueryReq req) {
    LambdaQueryWrapper<CertificationAssessorEntity> wrapper = new LambdaQueryWrapper<CertificationAssessorEntity>()
        .orderByDesc(CertificationAssessorEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w.like(CertificationAssessorEntity::getAssessorName, req.getKeyword())
          .or()
          .like(CertificationAssessorEntity::getCertificateNo, req.getKeyword())
          .or()
          .like(CertificationAssessorEntity::getWorkTypeScope, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationAssessorEntity::getStatus, req.getStatus());
    }
    Page<CertificationAssessorEntity> page = assessorMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return page(page, page.getRecords().stream().map(PreparationAssessorVO::from).toList());
  }

  @Transactional
  public PreparationAssessorVO createAssessor(PreparationAssessorSaveReq req) {
    CertificationAssessorEntity entity = new CertificationAssessorEntity();
    applyAssessor(entity, req);
    entity.setDeleted(0);
    assessorMapper.insert(entity);
    return PreparationAssessorVO.from(entity);
  }

  @Transactional
  public PreparationAssessorVO updateAssessor(Long assessorId, PreparationAssessorSaveReq req) {
    CertificationAssessorEntity entity = requireAssessor(assessorId);
    applyAssessor(entity, req);
    assessorMapper.updateById(entity);
    return PreparationAssessorVO.from(entity);
  }

  @Transactional
  public PreparationAssessorGroupVO createGroup(Long taskId, PreparationAssessorGroupSaveReq req) {
    findTask(taskId);
    ensureGroupNameAvailable(taskId, req.groupName(), null);
    List<Long> assessorIds = req.assessorIds() == null
        ? List.of()
        : req.assessorIds().stream().filter(Objects::nonNull).distinct().toList();
    if (!assessorIds.isEmpty()) {
      if (req.leaderAssessorId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组必须设置组长");
      }
      if (!assessorIds.contains(req.leaderAssessorId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组长必须从已选考评员中选择");
      }
    }
    CertificationTaskAssessorGroupEntity entity = new CertificationTaskAssessorGroupEntity();
    applyGroup(entity, taskId, req);
    entity.setDeleted(0);
    try {
      groupMapper.insert(entity);
    } catch (DuplicateKeyException ex) {
      throw duplicateGroupNameException();
    }
    replaceGroupWorktypes(entity.getId(), taskId, req.workTypeScopeIds());
    for (Long assessorId : assessorIds) {
      boolean isLeader = assessorId.equals(req.leaderAssessorId());
      addGroupMember(taskId, entity.getId(), new PreparationTaskAssessorSaveReq(
          assessorId,
          isLeader ? "leader" : "member",
          isLeader ? "组长" : "成员",
          req.remark()
      ));
    }
    touchTask(taskId);
    return toGroupView(entity);
  }

  @Transactional
  public PreparationAssessorGroupVO updateGroup(Long taskId, Long groupId, PreparationAssessorGroupSaveReq req) {
    findTask(taskId);
    CertificationTaskAssessorGroupEntity entity = requireGroup(taskId, groupId);
    ensureGroupNameAvailable(taskId, req.groupName(), groupId);
    if (req.workTypeScopeIds() != null) {
      List<Long> nextRequiredWorkTypeIds = resolveScopeWorkTypeIds(taskId, req.workTypeScopeIds());
      List<CertificationTaskAssessorEntity> members = taskAssessorMapper.selectList(
          new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
              .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
              .eq(CertificationTaskAssessorEntity::getGroupId, groupId));
      if (members != null) {
        for (CertificationTaskAssessorEntity member : members) {
          CertificationInfoAssessorEntity assessor = requireInfoAssessor(member.getAssessorId());
          workTypeCoverageService.requireAssessorCovers(
              assessor.getWorkTypeJson(), assessor.getWorkTypeScope(), assessor.getAssessorName(), nextRequiredWorkTypeIds);
        }
      }
    }
    applyGroup(entity, taskId, req);
    if (entity.getLeaderAssessorId() != null) {
      List<CertificationTaskAssessorEntity> members = taskAssessorMapper.selectList(
          new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
              .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
              .eq(CertificationTaskAssessorEntity::getGroupId, groupId));
      if (members != null) {
        if (!members.isEmpty() && members.stream().noneMatch(member -> entity.getLeaderAssessorId().equals(member.getAssessorId()))) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "组长必须是本考评组已绑定成员");
        }
        for (CertificationTaskAssessorEntity member : members) {
          requireAssessorCompatibleWithLeader(member.getAssessorId(), entity.getLeaderAssessorId(), taskId);
        }
      }
    }
    try {
      groupMapper.updateById(entity);
    } catch (DuplicateKeyException ex) {
      throw duplicateGroupNameException();
    }
    if (req.workTypeScopeIds() != null) {
      replaceGroupWorktypes(groupId, taskId, req.workTypeScopeIds());
    }
    touchTask(taskId);
    return toGroupView(entity);
  }

  @Transactional
  public void deleteGroup(Long taskId, Long groupId) {
    findTask(taskId);
    requireGroup(taskId, groupId);

    long memberCount = safeCount(taskAssessorMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
        .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
        .eq(CertificationTaskAssessorEntity::getGroupId, groupId)));
    if (memberCount > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组存在成员，不能删除");
    }

    long dispatched = safeCount(assessorTaskMapper.selectCount(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
        .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
        .eq(CertificationAssessorTaskEntity::getGroupId, groupId)));
    if (dispatched > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组已发放任务，不能删除");
    }

    // 联动删除关联表行
    groupWorktypeMapper.delete(new LambdaQueryWrapper<CertificationAssessorGroupWorktypeEntity>()
        .eq(CertificationAssessorGroupWorktypeEntity::getGroupId, groupId));

    groupMapper.deleteById(groupId);
    touchTask(taskId);
  }

  @Transactional(readOnly = true)
  public List<PreparationAssessorGroupVO> listGroups(Long taskId) {
    findTask(taskId);
    List<CertificationTaskAssessorGroupEntity> groups = groupMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
            .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
            .orderByDesc(CertificationTaskAssessorGroupEntity::getCreatedAt));
    if (groups.isEmpty()) {
      return List.of();
    }
    Map<Long, List<AssessorGroupWorktypeItem>> worktypeByGroup = loadWorktypesByGroupIds(
        groups.stream().map(CertificationTaskAssessorGroupEntity::getId).toList());
    Map<Long, String> leaderNames = loadAssessorNamesByIds(groups.stream()
        .map(CertificationTaskAssessorGroupEntity::getLeaderAssessorId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet()));
    Map<Long, Integer> memberCounts = loadMemberCountsByGroup(
        taskId,
        groups.stream().map(CertificationTaskAssessorGroupEntity::getId).toList());
    return groups.stream()
        .map(group -> PreparationAssessorGroupVO.from(
            group,
            resolveAssessorName(leaderNames, group.getLeaderAssessorId()),
            worktypeByGroup.getOrDefault(group.getId(), List.of()),
            memberCounts.getOrDefault(group.getId(), 0)))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AssessorGroupWorktypeItem> listTaskWorktypes(Long taskId, Long groupId) {
    findTask(taskId);
    if (groupId != null) {
      requireGroup(taskId, groupId);
    }
    return listScopes(taskId).stream()
        .map(scope -> new AssessorGroupWorktypeItem(
            scope.getId(),
            scope.getWorkTypeName(),
            scope.getWorkTypeId(),
            scope.getLevelCode(),
            scope.getLevelName(),
            scope.getSortOrder()))
        .toList();
  }

  private String leaderName(Long leaderAssessorId) {
    return resolveAssessorName(loadAssessorNamesByIds(
        leaderAssessorId == null ? Set.of() : Set.of(leaderAssessorId)), leaderAssessorId);
  }

  private String resolveAssessorName(Map<Long, String> namesById, Long assessorId) {
    if (assessorId == null) {
      return null;
    }
    return namesById.get(assessorId);
  }

  private Map<Long, String> loadAssessorNamesByIds(Set<Long> assessorIds) {
    if (assessorIds == null || assessorIds.isEmpty()) {
      return Map.of();
    }
    List<CertificationInfoAssessorEntity> rows = infoAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .in(CertificationInfoAssessorEntity::getId, assessorIds));
    if (rows == null || rows.isEmpty()) {
      return Map.of();
    }
    return rows.stream()
        .filter(row -> row.getId() != null)
        .collect(Collectors.toMap(
            CertificationInfoAssessorEntity::getId,
            CertificationInfoAssessorEntity::getAssessorName,
            (left, right) -> left));
  }

  @Transactional
  public PreparationTaskAssessorVO addGroupMember(Long taskId, Long groupId, PreparationTaskAssessorSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, groupId);
    CertificationInfoAssessorEntity assessor = requireInfoAssessor(req.assessorId());
    requireInfoAssessorAvailable(assessor);
    if (assessor.getValidUntil() != null && task.getExamDate() != null && assessor.getValidUntil().isBefore(task.getExamDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "证书已过期的考评员不得分配任务");
    }
    requireAssessorCoversGroupWorktypes(assessor, taskId, groupId);
    if (group.getLeaderAssessorId() != null) {
      requireAssessorCompatibleWithLeader(assessor.getId(), group.getLeaderAssessorId(), taskId);
    }
    long duplicate = safeCount(taskAssessorMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
        .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
        .eq(CertificationTaskAssessorEntity::getAssessorId, assessor.getId())));
    if (duplicate > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务同一考评员不得重复分配");
    }
    CertificationTaskAssessorEntity entity = new CertificationTaskAssessorEntity();
    entity.setTaskId(taskId);
    entity.setGroupId(groupId);
    entity.setAssessorId(assessor.getId());
    entity.setAssessorName(assessor.getAssessorName());
    entity.setCertificateNo(assessor.getCertificateNo());
    entity.setTaskRole(StringUtils.hasText(req.taskRole()) ? req.taskRole() : "member");
    entity.setRoleName(req.roleName());
    entity.setWorkTypeScope(assessor.getWorkTypeScope());
    entity.setValidUntil(assessor.getValidUntil());
    entity.setWorkStatus("preparing");
    entity.setRemark(req.remark());
    entity.setSortOrder(100);
    entity.setDeleted(0);
    taskAssessorMapper.insert(entity);

    markInfoAssessorWorkStatus(assessor.getId(), "preparing");
    touchTask(taskId);
    return PreparationTaskAssessorVO.from(entity);
  }

  @Transactional(readOnly = true)
  public List<PreparationTaskAssessorVO> listGroupMembers(Long taskId, Long groupId) {
    findTask(taskId);
    requireGroup(taskId, groupId);
    return taskAssessorMapper.selectList(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId)
            .orderByAsc(CertificationTaskAssessorEntity::getTaskRole)
            .orderByDesc(CertificationTaskAssessorEntity::getCreatedAt))
        .stream()
        .map(PreparationTaskAssessorVO::from)
        .toList();
  }

  @Transactional
  public List<PreparationAssessorTaskVO> dispatchGroup(Long taskId, Long groupId, PreparationAssessorDispatchReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, groupId);
    List<CertificationTaskAssessorEntity> members = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId)
            .orderByAsc(CertificationTaskAssessorEntity::getTaskRole)
            .orderByDesc(CertificationTaskAssessorEntity::getCreatedAt));
    if (CollectionUtils.isEmpty(members)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组尚未配置成员，不能发放考评任务");
    }

    List<PreparationAssessorTaskVO> dispatched = new ArrayList<>();

    List<Long> assessorIds = members.stream()
        .map(CertificationTaskAssessorEntity::getAssessorId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Long, CertificationInfoAssessorEntity> assessorById = new HashMap<>();
    if (!assessorIds.isEmpty()) {
      assessorById = infoAssessorMapper.selectList(
              new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
                  .in(CertificationInfoAssessorEntity::getId, assessorIds))
          .stream()
          .collect(Collectors.toMap(CertificationInfoAssessorEntity::getId, Function.identity()));
    }
    String displayName = formatTaskName(task);
    String leaderDisplayName = leaderName(group.getLeaderAssessorId());

    for (CertificationTaskAssessorEntity member : members) {
      CertificationInfoAssessorEntity assessor = assessorById.get(member.getAssessorId());
      if (assessor == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "考评员不存在");
      }
      CertificationAssessorTaskEntity entity = new CertificationAssessorTaskEntity();
      entity.setTenantId(task.getTenantId());
      entity.setTaskId(taskId);
      entity.setGroupId(groupId);
      entity.setTaskAssessorId(member.getId());
      entity.setAssessorId(member.getAssessorId());
      entity.setLeaderAssessorId(group.getLeaderAssessorId());
      entity.setQuestionId("group-" + groupId + "-assessor-" + member.getAssessorId());
      entity.setSubject(req.subject());
      entity.setTitle(req.title());
      entity.setAssessmentFileId(req.assessmentFileId());
      entity.setAssessmentFileName(req.assessmentFileName());
      entity.setDurationMin(req.durationMin());
      entity.setDueDate(req.dueDate());
      entity.setStatus("assigned");
      entity.setProgressNote(req.remark());
      entity.setRemark(req.remark());
      entity.setDeleted(0);
      assessorTaskMapper.insert(entity);
      dispatched.add(PreparationAssessorTaskVO.from(
          entity,
          displayName,
          group.getGroupName(),
          assessor.getAssessorName(),
          leaderDisplayName,
          List.of()));
    }
    List<Long> memberIds = members.stream()
        .map(CertificationTaskAssessorEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (!memberIds.isEmpty()) {
      CertificationTaskAssessorEntity memberPatch = new CertificationTaskAssessorEntity();
      memberPatch.setWorkStatus("assigned");
      taskAssessorMapper.update(memberPatch, new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
          .in(CertificationTaskAssessorEntity::getId, memberIds));
    }
    markInfoAssessorsWorkStatus(assessorIds, "assigned");
    touchTask(taskId);
    return dispatched;
  }

  private String formatTaskName(CertificationTaskEntity task) {
    if (task == null) {
      return null;
    }
    if (StringUtils.hasText(task.getTaskNo()) && StringUtils.hasText(task.getTaskName())) {
      return task.getTaskNo() + " " + task.getTaskName();
    }
    return task.getTaskName();
  }

  @Transactional
  public void removeGroupMember(Long taskId, Long groupId, Long memberId) {
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, groupId);
    CertificationTaskAssessorEntity member = taskAssessorMapper.selectById(memberId);
    if (member == null) {
      return;
    }
    if (!taskId.equals(member.getTaskId()) || !groupId.equals(member.getGroupId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评组成员不存在");
    }
    if (member.getAssessorId() != null && member.getAssessorId().equals(group.getLeaderAssessorId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先更换考评组组长后再解绑该成员");
    }
    Long assessorId = member.getAssessorId();
    taskAssessorMapper.deleteById(memberId);
    releaseInfoAssessorIfIdle(assessorId);
    touchTask(taskId);
  }

  private String resolveTaskName(Long taskId) {
    if (taskId == null) {
      return null;
    }
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      return null;
    }
    if (StringUtils.hasText(task.getTaskNo()) && StringUtils.hasText(task.getTaskName())) {
      return task.getTaskNo() + " " + task.getTaskName();
    }
    return StringUtils.hasText(task.getTaskName()) ? task.getTaskName() : task.getTaskNo();
  }

  private String resolveGroupName(Long groupId) {
    if (groupId == null) {
      return null;
    }
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(groupId);
    return group == null ? null : group.getGroupName();
  }

  private String resolveAssessorName(Long assessorId) {
    if (assessorId == null) {
      return null;
    }
    CertificationInfoAssessorEntity info = infoAssessorMapper.selectById(assessorId);
    return info == null ? null : info.getAssessorName();
  }

  @Transactional
  public PreparationCompleteCheckVO completeCheck(Long taskId) {
    CertificationTaskEntity task = findTask(taskId);
    syncCandidateDocuments(taskId);
    List<CertificationCandidateEntity> activeCandidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getStatus, "active"));
    List<PreparationCheckItemVO> items = new ArrayList<>();
    boolean taskPrepared = CertificationTaskPhaseSupport.PREPARATION.equals(task.getCurrentPhase());
    items.add(new PreparationCheckItemVO("task_project_prepared", "认定准备", taskPrepared,
        taskPrepared ? "认定准备已完成" : "只有认定准备已完成的任务允许完成筹备"));
    long candidateCount = activeCandidates.size();
    items.add(new PreparationCheckItemVO("candidate_required", "考生名单", candidateCount > 0,
        candidateCount > 0 ? "已导入 " + candidateCount + " 名考生" : "当前任务下至少需要 1 名考生"));
    List<Long> activeCandidateIds = activeCandidates.stream().map(CertificationCandidateEntity::getId).toList();
    long incompleteDocs = activeCandidateIds.isEmpty() ? 0 : safeCount(documentMapper.selectCount(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, activeCandidateIds)
            .eq(CertificationCandidateDocumentEntity::getRequired, true)
            .ne(CertificationCandidateDocumentEntity::getStatus, "approved")));
    boolean documentsApproved = candidateCount > 0 && incompleteDocs == 0;
    items.add(new PreparationCheckItemVO("document_approved", "考生资料", documentsApproved,
        candidateCount == 0 ? "尚无考生，无法校验必传资料"
            : incompleteDocs == 0 ? "必传资料均已审核通过" : "存在未审核通过的必传资料"));
    long materialGenerated = safeCount(materialBatchMapper.selectCount(new LambdaQueryWrapper<CertificationApplicationMaterialBatchEntity>()
        .eq(CertificationApplicationMaterialBatchEntity::getTaskId, taskId)
        .eq(CertificationApplicationMaterialBatchEntity::getBatchType, "generate")
        .eq(CertificationApplicationMaterialBatchEntity::getStatus, "generated")));
    items.add(new PreparationCheckItemVO("material_generated", "申报材料", materialGenerated > 0,
        materialGenerated > 0 ? "已生成申报材料" : "尚未生成申报材料"));
    long siteCount = safeCount(taskExamSiteMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
        .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("exam_site_required", "考场配置", siteCount > 0,
        siteCount > 0 ? "已配置 " + siteCount + " 个考场" : "至少配置 1 个有效考场"));
    long staffCount = safeCount(taskExamStaffMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamStaffEntity>()
        .eq(CertificationTaskExamStaffEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("exam_staff_required", "考务配置", staffCount > 0,
        staffCount > 0 ? "已配置 " + staffCount + " 名考务人员" : "至少配置 1 名考务人员"));
    long equipmentCount = safeCount(taskEquipmentMapper.selectCount(new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
        .eq(CertificationTaskEquipmentEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("equipment_required", "配置设备", equipmentCount > 0,
        equipmentCount > 0 ? "已配置 " + equipmentCount + " 台设备" : "至少配置 1 台设备"));
    long groupCount = safeCount(groupMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
        .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("assessor_group_required", "考评组", groupCount > 0,
        groupCount > 0 ? "已配置 " + groupCount + " 个考评组" : "至少配置 1 个考评组"));
    long leaderMissing = safeCount(groupMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
        .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
        .isNull(CertificationTaskAssessorGroupEntity::getLeaderAssessorId)));
    boolean leaderConfigured = groupCount > 0 && leaderMissing == 0;
    items.add(new PreparationCheckItemVO("assessor_group_leader", "考评组组长", leaderConfigured,
        groupCount == 0 ? "尚未配置考评组，无法校验组长"
            : leaderMissing == 0 ? "考评组组长配置完整" : "存在未配置组长的考评组"));
    long dispatched = safeCount(assessorTaskMapper.selectCount(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
        .eq(CertificationAssessorTaskEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("assessor_task_dispatched", "考评任务发放", dispatched > 0,
        dispatched > 0 ? "已发放考评任务" : "尚未发放考评任务"));
    return new PreparationCompleteCheckVO(items.stream().allMatch(PreparationCheckItemVO::passed), items);
  }

  @Transactional
  public PreparationCompleteCheckVO complete(Long taskId) {
    PreparationCompleteCheckVO check = completeCheck(taskId);
    if (!isChecklistConfirmed(taskId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "清单核查未完成，请勾选全部材料并选择确认人");
    }
    if (!check.passed()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "筹备完成检查未通过");
    }
    CertificationTaskPhaseSupport.advancePhase(
        taskMapper,
        phaseMapper,
        taskId,
        CertificationTaskPhaseSupport.PREPARATION,
        CertificationTaskPhaseSupport.ARCHIVE,
        CertificationTaskStatuses.IN_PROGRESS
    );
    return check;
  }

  private void ensureChecklistItems(CertificationTaskEntity task) {
    long existingCount = safeCount(taskChecklistItemMapper.selectCount(new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
        .eq(CertificationTaskChecklistItemEntity::getTaskId, task.getId())));
    if (existingCount >= CHECKLIST_TEMPLATE.size()) {
      return;
    }
    Set<Integer> existingNos = listChecklistEntities(task.getId()).stream()
        .map(CertificationTaskChecklistItemEntity::getItemNo)
        .collect(Collectors.toSet());
    for (ChecklistTemplateItem templateItem : CHECKLIST_TEMPLATE) {
      if (existingNos.contains(templateItem.itemNo())) {
        continue;
      }
      CertificationTaskChecklistItemEntity entity = new CertificationTaskChecklistItemEntity();
      entity.setTaskId(task.getId());
      entity.setTenantId(task.getTenantId());
      entity.setItemNo(templateItem.itemNo());
      entity.setItemName(templateItem.itemName());
      entity.setQuantityText(templateItem.quantityText());
      entity.setChecked(0);
      entity.setSortOrder(templateItem.itemNo());
      entity.setDeleted(0);
      taskChecklistItemMapper.insert(entity);
    }
  }

  private PreparationChecklistVO buildChecklist(CertificationTaskEntity task) {
    List<CertificationTaskChecklistItemEntity> entities = listChecklistEntities(task.getId());
    List<PreparationChecklistItemVO> items = entities.stream()
        .map(PreparationChecklistItemVO::from)
        .toList();
    int checkedCount = (int) items.stream().filter(PreparationChecklistItemVO::checked).count();
    CertificationTaskChecklistItemEntity confirmed = entities.stream()
        .filter(item -> item.getChecked() != null && item.getChecked() == 1 && item.getConfirmerTaskStaffId() != null)
        .findFirst()
        .orElse(null);
    return new PreparationChecklistVO(
        task.getId(),
        task.getTaskName(),
        items.size(),
        checkedCount,
        !items.isEmpty() && checkedCount == items.size(),
        confirmed == null ? null : confirmed.getConfirmerTaskStaffId(),
        confirmed == null ? null : confirmed.getConfirmerName(),
        confirmed == null ? null : confirmed.getConfirmedAt(),
        items
    );
  }

  private List<CertificationTaskChecklistItemEntity> listChecklistEntities(Long taskId) {
    return taskChecklistItemMapper.selectList(new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
        .eq(CertificationTaskChecklistItemEntity::getTaskId, taskId)
        .orderByAsc(CertificationTaskChecklistItemEntity::getSortOrder)
        .orderByAsc(CertificationTaskChecklistItemEntity::getItemNo));
  }

  private boolean isChecklistConfirmed(Long taskId) {
    long total = safeCount(taskChecklistItemMapper.selectCount(new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
        .eq(CertificationTaskChecklistItemEntity::getTaskId, taskId)));
    long checked = safeCount(taskChecklistItemMapper.selectCount(new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
        .eq(CertificationTaskChecklistItemEntity::getTaskId, taskId)
        .eq(CertificationTaskChecklistItemEntity::getChecked, 1)
        .isNotNull(CertificationTaskChecklistItemEntity::getConfirmerTaskStaffId)));
    return total > 0 && total == checked;
  }

  private PreparationTaskOverviewVO overviewFromTask(CertificationTaskEntity task) {
    long candidateCount = candidateCount(task.getId());
    long approvedDocs = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, task.getId())
        .eq(CertificationCandidateDocumentEntity::getRequired, true)
        .eq(CertificationCandidateDocumentEntity::getStatus, "approved")));
    long requiredDocs = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, task.getId())
        .eq(CertificationCandidateDocumentEntity::getRequired, true)));
    int completionRate = requiredDocs == 0 ? 0 : (int) Math.round((approvedDocs * 100.0) / requiredDocs);
    long materialGenerated = safeCount(materialBatchMapper.selectCount(new LambdaQueryWrapper<CertificationApplicationMaterialBatchEntity>()
        .eq(CertificationApplicationMaterialBatchEntity::getTaskId, task.getId())
        .eq(CertificationApplicationMaterialBatchEntity::getBatchType, "generate")
        .eq(CertificationApplicationMaterialBatchEntity::getStatus, "generated")));
    long siteCount = safeCount(taskExamSiteMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
        .eq(CertificationTaskExamSiteEntity::getTaskId, task.getId())));
    long staffCount = safeCount(taskExamStaffMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamStaffEntity>()
        .eq(CertificationTaskExamStaffEntity::getTaskId, task.getId())));
    long assessorCount = safeCount(taskAssessorMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
        .eq(CertificationTaskAssessorEntity::getTaskId, task.getId())));
    return PreparationTaskOverviewVO.from(task, listScopes(task.getId()), candidateCount, completionRate,
        materialGenerated > 0 ? "generated" : "pending", siteCount, staffCount, assessorCount);
  }

  private List<PreparationTaskOverviewVO> buildTaskOverviews(List<CertificationTaskEntity> tasks) {
    if (CollectionUtils.isEmpty(tasks)) {
      return List.of();
    }
    List<Long> taskIds = tasks.stream()
        .map(CertificationTaskEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (taskIds.isEmpty()) {
      return tasks.stream()
          .map(task -> PreparationTaskOverviewVO.from(task, List.of(), 0, 0, "pending", 0, 0, 0))
          .toList();
    }

    Map<Long, List<CertificationTaskScopeEntity>> scopesByTask = scopeMapper.selectList(
            new LambdaQueryWrapper<CertificationTaskScopeEntity>()
                .in(CertificationTaskScopeEntity::getTaskId, taskIds)
                .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
                .orderByAsc(CertificationTaskScopeEntity::getId))
        .stream()
        .collect(Collectors.groupingBy(CertificationTaskScopeEntity::getTaskId));

    Map<Long, Map<String, Object>> statsByTask = taskMapper.selectPreparationTaskOverviewStats(taskIds).stream()
        .filter(item -> item.get("taskId") != null)
        .collect(Collectors.toMap(item -> stat(item, "taskId"), item -> item, (left, right) -> left));

    return tasks.stream()
        .map(task -> {
          Long taskId = task.getId();
          Map<String, Object> stats = statsByTask.getOrDefault(taskId, Map.of());
          long requiredDocs = stat(stats, "requiredDocCount");
          long approvedDocs = stat(stats, "approvedDocCount");
          int completionRate = requiredDocs == 0 ? 0 : (int) Math.round((approvedDocs * 100.0) / requiredDocs);
          return PreparationTaskOverviewVO.from(
              task,
              scopesByTask.getOrDefault(taskId, List.of()),
              stat(stats, "candidateCount"),
              completionRate,
              stat(stats, "materialGeneratedCount") > 0 ? "generated" : "pending",
              stat(stats, "siteCount"),
              stat(stats, "staffCount"),
              stat(stats, "assessorCount")
          );
        })
        .toList();
  }

  private long stat(Map<String, Object> stats, String key) {
    Object value = stats.get(key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null) {
      return 0;
    }
    return Long.parseLong(String.valueOf(value));
  }

  private void validateCandidate(CertificationTaskEntity task, PreparationCandidateSaveReq req, Long currentCandidateId) {
    boolean xlsxImport = "other_xlsx".equals(req.importSource());
    boolean candidateEntry = !StringUtils.hasText(req.importSource())
        || Set.of("manual", "other", "other_xlsx").contains(req.importSource());
    if (candidateEntry && !StringUtils.hasText(req.admissionTicketNo())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "准考证号不能为空");
    }
    if (!xlsxImport && !StringUtils.hasText(req.idCardNo())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号不能为空");
    }
    if (StringUtils.hasText(req.idCardNo()) && !ID_CARD_PATTERN.matcher(req.idCardNo()).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号格式必须合法");
    }
    if (StringUtils.hasText(req.phone()) && !MOBILE_PATTERN.matcher(req.phone()).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "联系电话格式必须合法");
    }
    List<CertificationTaskScopeEntity> taskScopes = listScopes(task.getId());
    if (req.taskScopeId() != null && taskScopes.stream().noneMatch(scope -> req.taskScopeId().equals(scope.getId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "所选认定班级不属于当前任务");
    }
    Set<String> supportedLevelCodes = taskScopes.stream()
        .map(CertificationTaskScopeEntity::getLevelCode)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
    if (!supportedLevelCodes.isEmpty() && StringUtils.hasText(req.levelCode())
        && !supportedLevelCodes.contains(req.levelCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "报考等级必须匹配当前认定任务支持的等级");
    }
    if (StringUtils.hasText(req.idCardNo())) {
      LambdaQueryWrapper<CertificationCandidateEntity> duplicateWrapper = new LambdaQueryWrapper<CertificationCandidateEntity>()
          .eq(CertificationCandidateEntity::getTaskId, task.getId())
          .eq(CertificationCandidateEntity::getIdCardNo, req.idCardNo());
      if (currentCandidateId != null) {
        duplicateWrapper.ne(CertificationCandidateEntity::getId, currentCandidateId);
      }
      if (safeCount(candidateMapper.selectCount(duplicateWrapper)) > 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下身份证号不得重复");
      }
    }
    if (StringUtils.hasText(req.admissionTicketNo())) {
      LambdaQueryWrapper<CertificationCandidateEntity> duplicateWrapper = new LambdaQueryWrapper<CertificationCandidateEntity>()
          .eq(CertificationCandidateEntity::getTaskId, task.getId())
          .eq(CertificationCandidateEntity::getAdmissionTicketNo, req.admissionTicketNo());
      if (currentCandidateId != null) {
        duplicateWrapper.ne(CertificationCandidateEntity::getId, currentCandidateId);
      }
      if (safeCount(candidateMapper.selectCount(duplicateWrapper)) > 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一任务下准考证号不得重复");
      }
    }
  }

  private void applyCandidate(CertificationCandidateEntity entity, CertificationTaskEntity task, PreparationCandidateSaveReq req) {
    entity.setCandidateName(req.candidateName());
    entity.setAdmissionTicketNo(req.admissionTicketNo());
    entity.setIdCardNo(req.idCardNo());
    entity.setMaskedIdCardNo(SensitiveMaskUtils.maskIdCard(req.idCardNo()));
    entity.setPhone(req.phone());
    entity.setMaskedPhone(SensitiveMaskUtils.maskPhone(req.phone()));
    CertificationTaskScopeEntity defaultScope = req.taskScopeId() == null ? firstScope(task.getId())
        : scopeMapper.selectById(req.taskScopeId());
    entity.setTaskScopeId(defaultScope == null ? null : defaultScope.getId());
    entity.setLevelCode(StringUtils.hasText(req.levelCode()) ? req.levelCode() : defaultScope == null ? null : defaultScope.getLevelCode());
    entity.setLevelName(StringUtils.hasText(req.levelName()) ? req.levelName() : defaultScope == null ? null : defaultScope.getLevelName());
    entity.setWorkTypeId(defaultScope == null ? null : defaultScope.getWorkTypeId());
    entity.setWorkTypeName(defaultScope == null ? null : defaultScope.getWorkTypeName());
    entity.setImportSource(StringUtils.hasText(req.importSource()) ? req.importSource() : "manual");
    entity.setOrgName(req.orgName());
    entity.setRemark(req.remark());
  }

  private List<CertificationMaterialTemplateEntity> candidateDocumentTemplates() {
    List<CertificationMaterialTemplateEntity> templates = templateMapper.selectList(
        new LambdaQueryWrapper<CertificationMaterialTemplateEntity>()
            .eq(CertificationMaterialTemplateEntity::getTemplateScope, CANDIDATE_DOC_SCOPE)
            .in(CertificationMaterialTemplateEntity::getItemCode, CANDIDATE_DOC_CODES)
            .eq(CertificationMaterialTemplateEntity::getEnabled, true)
            .orderByAsc(CertificationMaterialTemplateEntity::getSortOrder));
    Map<String, CertificationMaterialTemplateEntity> byCode = new HashMap<>();
    if (!CollectionUtils.isEmpty(templates)) {
      templates.forEach(template -> byCode.put(template.getItemCode(), template));
    }
    for (CertificationMaterialTemplateEntity fallback : defaultCandidateDocumentTemplates()) {
      byCode.putIfAbsent(fallback.getItemCode(), fallback);
    }
    return CANDIDATE_DOC_CODES.stream().map(byCode::get).filter(Objects::nonNull).toList();
  }

  private List<CertificationMaterialTemplateEntity> defaultCandidateDocumentTemplates() {
    return List.of(
        candidateDocTemplate("DOC001", "报名信息表", 1, true),
        candidateDocTemplate("DOC002", "两寸证件照", 2, true),
        candidateDocTemplate("DOC003", "身份证正面", 3, true),
        candidateDocTemplate("DOC004", "身份证反面", 4, true),
        candidateDocTemplate("DOC005", "学历证明", 5, false),
        candidateDocTemplate("DOC006", "原技能证书", 6, false)
    );
  }

  private CertificationMaterialTemplateEntity candidateDocTemplate(
      String code,
      String name,
      int sortOrder,
      boolean required
  ) {
    CertificationMaterialTemplateEntity template = new CertificationMaterialTemplateEntity();
    template.setTemplateScope(CANDIDATE_DOC_SCOPE);
    template.setItemCode(code);
    template.setItemName(name);
    template.setRequired(required);
    template.setEnabled(true);
    template.setSortOrder(sortOrder);
    template.setDeleted(0);
    return template;
  }

  private void applyDocumentKeywordFilter(
      Long taskId,
      String keyword,
      LambdaQueryWrapper<CertificationCandidateDocumentEntity> wrapper
  ) {
    List<Long> matchedCandidateIds = candidateMapper.selectList(
            new LambdaQueryWrapper<CertificationCandidateEntity>()
                .eq(CertificationCandidateEntity::getTaskId, taskId)
                .like(CertificationCandidateEntity::getCandidateName, keyword))
        .stream()
        .map(CertificationCandidateEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    wrapper.and(w -> {
      w.like(CertificationCandidateDocumentEntity::getDocName, keyword)
          .or()
          .like(CertificationCandidateDocumentEntity::getDocCode, keyword);
      if (!matchedCandidateIds.isEmpty()) {
        w.or().in(CertificationCandidateDocumentEntity::getCandidateId, matchedCandidateIds);
      }
    });
  }

  private void initializeCandidateDocuments(Long taskId, Long candidateId) {
    if (candidateId == null) {
      return;
    }
    ensureCandidateDocuments(taskId, List.of(candidateId));
    refreshCandidateDocumentStats(taskId, candidateId);
  }

  private void syncCandidateDocuments(Long taskId) {
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId));
    if (CollectionUtils.isEmpty(candidates)) {
      return;
    }
    List<Long> candidateIds = candidates.stream()
        .map(CertificationCandidateEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    ensureCandidateDocuments(taskId, candidateIds);
    normalizeAutoApprovedImageDocuments(taskId, candidateIds);
    batchRefreshCandidateDocumentStats(taskId, candidateIds);
  }

  private void normalizeAutoApprovedImageDocuments(Long taskId, List<Long> candidateIds) {
    List<CertificationCandidateDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, candidateIds)
            .in(CertificationCandidateDocumentEntity::getDocCode, AUTO_APPROVED_IMAGE_DOC_CODES)
            .isNotNull(CertificationCandidateDocumentEntity::getFileId)
            .ne(CertificationCandidateDocumentEntity::getStatus, INFO_APPROVED));
    if (CollectionUtils.isEmpty(documents)) {
      return;
    }
    for (CertificationCandidateDocumentEntity document : documents) {
      document.setStatus(INFO_APPROVED);
      document.setReviewedAt(document.getUploadedAt() == null ? LocalDateTime.now() : document.getUploadedAt());
      document.setReviewComment("图片上传后自动通过，无需审核");
      documentMapper.updateById(document);
    }
  }

  private void ensureCandidateDocuments(Long taskId, List<Long> candidateIds) {
    if (CollectionUtils.isEmpty(candidateIds)) {
      return;
    }
    pruneObsoleteCandidateDocuments(taskId, candidateIds);
    List<CertificationMaterialTemplateEntity> templates = candidateDocumentTemplates();
    if (CollectionUtils.isEmpty(templates)) {
      return;
    }
    Map<Long, CertificationTaskEntity> taskTenant = resolveTaskTenant(taskId);
    Long tenantId = taskTenant.get(taskId) == null ? null : taskTenant.get(taskId).getTenantId();
    Set<String> existingKeys = documentMapper
        .selectExistingCandidateDocCodes(taskId, candidateIds)
        .stream()
        .filter(row -> row.getCandidateId() != null && row.getDocCode() != null)
        .map(row -> row.getCandidateId() + "\u0000" + row.getDocCode())
        .collect(Collectors.toSet());
    List<CandidateDocRow> missingRows = new ArrayList<>();
    for (Long candidateId : candidateIds) {
      for (CertificationMaterialTemplateEntity template : templates) {
        String key = candidateId + "\u0000" + template.getItemCode();
        if (existingKeys.contains(key)) {
          continue;
        }
        CandidateDocRow row = new CandidateDocRow();
        row.setId(IdWorker.getId());
        row.setCandidateId(candidateId);
        row.setTenantId(tenantId);
        row.setTaskId(taskId);
        row.setTemplateId(template.getId());
        row.setDocCode(template.getItemCode());
        row.setDocName(template.getItemName());
        row.setRequired(template.getRequired() == null || template.getRequired());
        missingRows.add(row);
      }
    }
    if (!missingRows.isEmpty()) {
      documentMapper.batchInsertMissing(missingRows);
    }
  }

  private void pruneObsoleteCandidateDocuments(Long taskId, List<Long> candidateIds) {
    if (CollectionUtils.isEmpty(candidateIds)) {
      return;
    }
    List<CertificationCandidateDocumentEntity> obsoleteDocuments = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, candidateIds)
            .notIn(CertificationCandidateDocumentEntity::getDocCode, CANDIDATE_DOC_CODES));
    if (CollectionUtils.isEmpty(obsoleteDocuments)) {
      return;
    }
    documentMapper.deleteBatchIds(obsoleteDocuments.stream()
        .map(CertificationCandidateDocumentEntity::getId)
        .filter(Objects::nonNull)
        .toList());
  }

  private Map<Long, CertificationTaskEntity> resolveTaskTenant(Long taskId) {
    if (taskId == null) {
      return Map.of();
    }
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      return Map.of();
    }
    return Map.of(taskId, task);
  }

  private Map<Long, CertificationCandidateEntity> loadCandidateMap(
      Long taskId,
      List<CertificationCandidateDocumentEntity> documents
  ) {
    if (CollectionUtils.isEmpty(documents)) {
      return Map.of();
    }
    List<Long> candidateIds = documents.stream()
        .map(CertificationCandidateDocumentEntity::getCandidateId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (candidateIds.isEmpty()) {
      return Map.of();
    }
    return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .in(CertificationCandidateEntity::getId, candidateIds))
        .stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, candidate -> candidate, (left, right) -> left));
  }

  private void enrichCandidateDocumentSummary(Long taskId, List<PreparationCandidateVO> candidates) {
    if (CollectionUtils.isEmpty(candidates)) {
      return;
    }
    List<Long> candidateIds = candidates.stream().map(PreparationCandidateVO::id).filter(Objects::nonNull).toList();
    if (candidateIds.isEmpty()) {
      return;
    }
    List<CertificationCandidateDocumentEntity> documents = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .in(CertificationCandidateDocumentEntity::getCandidateId, candidateIds)
            .in(CertificationCandidateDocumentEntity::getDocCode, CANDIDATE_DOC_CODES));
    Map<Long, List<CertificationCandidateDocumentEntity>> grouped = documents.stream()
        .collect(Collectors.groupingBy(CertificationCandidateDocumentEntity::getCandidateId));
    for (int i = 0; i < candidates.size(); i++) {
      PreparationCandidateVO candidate = candidates.get(i);
      List<CertificationCandidateDocumentEntity> candidateDocuments =
          grouped.getOrDefault(candidate.id(), List.of());
      int total = candidateDocuments.size();
      int missing = (int) candidateDocuments.stream().filter(doc -> isMissingDocument(doc)).count();
      int uploaded = total - missing;
      candidates.set(i, candidate.withDocumentSummary(uploaded, missing, total));
    }
  }

  private boolean isMissingDocument(CertificationCandidateDocumentEntity document) {
    if (document == null) {
      return true;
    }
    if (document.getFileId() != null) {
      return false;
    }
    return !StringUtils.hasText(document.getStatus()) || "missing".equalsIgnoreCase(document.getStatus());
  }

  private void validateCandidateDocumentFile(String docCode, String fileName, String fileType) {
    if (!StringUtils.hasText(fileName) && !StringUtils.hasText(fileType)) {
      return;
    }
    String lowerName = StringUtils.hasText(fileName) ? fileName.toLowerCase(Locale.ROOT) : "";
    String lowerType = StringUtils.hasText(fileType) ? fileType.toLowerCase(Locale.ROOT) : "";
    if ("DOC001".equals(docCode)) {
      boolean accepted = lowerName.endsWith(".xlsx") || lowerName.endsWith(".pdf") || lowerName.endsWith(".docx")
          || "application/pdf".equals(lowerType)
          || lowerType.contains("spreadsheetml.sheet")
          || lowerType.contains("wordprocessingml.document");
      if (!accepted) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "报名信息表仅支持 xlsx、pdf、docx 格式");
      }
    }
    if (List.of("DOC002", "DOC003", "DOC004", "DOC005", "DOC006").contains(docCode)) {
      boolean accepted = lowerType.startsWith("image/")
          || lowerName.endsWith(".jpg")
          || lowerName.endsWith(".jpeg")
          || lowerName.endsWith(".png")
          || lowerName.endsWith(".gif")
          || lowerName.endsWith(".bmp")
          || lowerName.endsWith(".webp");
      if (!accepted) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "图片资料仅支持图片格式");
      }
    }
  }

  private void refreshCandidateDocumentStats(Long taskId, Long candidateId) {
    if (candidateId == null) {
      return;
    }
    Map<Long, Map<String, Object>> stats =
        candidateMapper.selectDocumentStatsByCandidate(taskId, List.of(candidateId));
    Map<String, Object> row = stats == null ? null : stats.get(candidateId);
    long required = row == null ? 0 : toLong(row.get("requiredCount"));
    long approved = row == null ? 0 : toLong(row.get("approvedCount"));
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null) {
      return;
    }
    candidate.setDocumentRequiredCount((int) required);
    candidate.setDocumentCompletedCount((int) approved);
    candidate.setDocumentComplete(required > 0 && required == approved);
    candidateMapper.updateById(candidate);
  }

  private void batchRefreshCandidateDocumentStats(Long taskId, List<Long> candidateIds) {
    if (taskId == null || CollectionUtils.isEmpty(candidateIds)) {
      return;
    }
    List<Long> ids = candidateIds.stream().filter(Objects::nonNull).toList();
    if (ids.isEmpty()) {
      return;
    }
    candidateMapper.batchRefreshDocumentStats(taskId, ids);
  }

  private long toLong(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void applyExamSite(CertificationExamSiteEntity entity, PreparationExamSiteSaveReq req) {
    entity.setSiteName(req.siteName());
    entity.setSiteType(StringUtils.hasText(req.siteType()) ? req.siteType() : "comprehensive");
    entity.setAddress(req.address());
    entity.setSeatCount(req.seatCount() == null ? 0 : req.seatCount());
    entity.setFilingNo(req.filingNo());
    entity.setContactName(req.contactName());
    entity.setContactPhone(req.contactPhone());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "active");
    entity.setEquipmentCondition(req.equipmentCondition());
    entity.setRemark(req.remark());
  }

  private void applyAssessor(CertificationAssessorEntity entity, PreparationAssessorSaveReq req) {
    entity.setUserId(req.userId());
    entity.setAssessorName(req.assessorName());
    entity.setCertificateNo(req.certificateNo());
    entity.setWorkTypeScope(req.workTypeScope());
    entity.setLevelScope(req.levelScope());
    entity.setValidUntil(req.validUntil());
    entity.setPhone(req.phone());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "idle");
    entity.setRemark(req.remark());
  }

  private void applyGroup(CertificationTaskAssessorGroupEntity entity, Long taskId, PreparationAssessorGroupSaveReq req) {
    entity.setTaskId(taskId);
    entity.setGroupName(req.groupName());
    entity.setLeaderAssessorId(req.leaderAssessorId());
    entity.setWorkTypeName(req.workTypeName());
    if (StringUtils.hasText(req.status())) {
      entity.setStatus(req.status());
    } else if (!StringUtils.hasText(entity.getStatus())) {
      entity.setStatus("draft");
    }
    entity.setRemark(req.remark());
  }

  private void ensureGroupNameAvailable(Long taskId, String groupName, Long excludeGroupId) {
    LambdaQueryWrapper<CertificationTaskAssessorGroupEntity> query = new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
        .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
        .eq(CertificationTaskAssessorGroupEntity::getGroupName, groupName);
    if (excludeGroupId != null) {
      query.ne(CertificationTaskAssessorGroupEntity::getId, excludeGroupId);
    }
    if (safeCount(groupMapper.selectCount(query)) > 0) {
      throw duplicateGroupNameException();
    }
  }

  private BusinessException duplicateGroupNameException() {
    return new BusinessException(ErrorCode.CONFLICT, "考评组名称已存在");
  }

  private PreparationAssessorGroupVO toGroupView(CertificationTaskAssessorGroupEntity entity) {
    Map<Long, Integer> memberCounts = loadMemberCountsByGroup(entity.getTaskId(), List.of(entity.getId()));
    return PreparationAssessorGroupVO.from(
        entity,
        leaderName(entity.getLeaderAssessorId()),
        loadGroupWorktypeItems(entity.getId()),
        memberCounts.getOrDefault(entity.getId(), 0));
  }

  private Map<Long, Integer> loadMemberCountsByGroup(Long taskId, List<Long> groupIds) {
    Map<Long, Integer> memberCounts = new HashMap<>();
    if (taskId == null || groupIds == null || groupIds.isEmpty()) {
      return memberCounts;
    }
    List<CertificationTaskAssessorEntity> allMembers = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .in(CertificationTaskAssessorEntity::getGroupId, groupIds));
    Map<Long, List<CertificationTaskAssessorEntity>> grouped = allMembers == null ? Map.of()
        : allMembers.stream().collect(Collectors.groupingBy(CertificationTaskAssessorEntity::getGroupId));
    for (Long groupId : groupIds) {
      memberCounts.put(groupId, grouped.getOrDefault(groupId, List.of()).size());
    }
    return memberCounts;
  }

  private void replaceGroupWorktypes(Long groupId, Long taskId, List<Long> scopeIds) {
    List<Long> normalized = scopeIds == null
        ? List.of()
        : scopeIds.stream().filter(Objects::nonNull).distinct().toList();
    groupWorktypeMapper.delete(new LambdaQueryWrapper<CertificationAssessorGroupWorktypeEntity>()
        .eq(CertificationAssessorGroupWorktypeEntity::getGroupId, groupId));
    Map<Long, CertificationTaskScopeEntity> scopeMap = listScopes(taskId).stream()
        .filter(s -> normalized.contains(s.getId()))
        .collect(Collectors.toMap(CertificationTaskScopeEntity::getId, s -> s, (a, b) -> a));
    int order = 100;
    List<CertificationAssessorGroupWorktypeEntity> rows = new ArrayList<>(normalized.size());
    for (Long scopeId : normalized) {
      CertificationTaskScopeEntity scope = scopeMap.get(scopeId);
      if (scope == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工种范围不属于当前认定任务");
      }
      CertificationAssessorGroupWorktypeEntity row = new CertificationAssessorGroupWorktypeEntity();
      row.setGroupId(groupId);
      row.setScopeId(scopeId);
      row.setWorkTypeName(scope.getWorkTypeName());
      row.setLevelCode(scope.getLevelCode());
      row.setLevelName(scope.getLevelName());
      row.setSortOrder(scope.getSortOrder() == null ? order : scope.getSortOrder());
      row.setDeleted(0);
      rows.add(row);
      order += 10;
    }
    for (CertificationAssessorGroupWorktypeEntity row : rows) {
      groupWorktypeMapper.insert(row);
    }
    // 同步回填单值 work_type_name 字段（拼接展示，向后兼容）
    String joined = scopeMap.values().stream()
        .map(CertificationTaskScopeEntity::getWorkTypeName)
        .filter(StringUtils::hasText)
        .distinct()
        .collect(Collectors.joining("、"));
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(groupId);
    if (group != null) {
      group.setWorkTypeName(joined);
      groupMapper.updateById(group);
    }
  }

  private List<AssessorGroupWorktypeItem> loadGroupWorktypeItems(Long groupId) {
    return groupWorktypeMapper.selectList(new LambdaQueryWrapper<CertificationAssessorGroupWorktypeEntity>()
            .eq(CertificationAssessorGroupWorktypeEntity::getGroupId, groupId)
            .orderByAsc(CertificationAssessorGroupWorktypeEntity::getSortOrder)
            .orderByAsc(CertificationAssessorGroupWorktypeEntity::getId))
        .stream()
        .map(row -> new AssessorGroupWorktypeItem(
            row.getScopeId(),
            displayGroupWorkType(row),
            workTypeIdForScope(row.getScopeId()),
            row.getLevelCode(),
            row.getLevelName(),
            row.getSortOrder()))
        .toList();
  }

  private Map<Long, List<AssessorGroupWorktypeItem>> loadWorktypesByGroupIds(List<Long> groupIds) {
    if (groupIds == null || groupIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<AssessorGroupWorktypeItem>> result = new java.util.LinkedHashMap<>();
    for (Long gid : groupIds) {
      result.put(gid, new ArrayList<>());
    }
    List<CertificationAssessorGroupWorktypeEntity> rows = groupWorktypeMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorGroupWorktypeEntity>()
            .in(CertificationAssessorGroupWorktypeEntity::getGroupId, groupIds)
            .orderByAsc(CertificationAssessorGroupWorktypeEntity::getSortOrder)
            .orderByAsc(CertificationAssessorGroupWorktypeEntity::getId));
    for (CertificationAssessorGroupWorktypeEntity row : rows) {
      result.computeIfAbsent(row.getGroupId(), k -> new ArrayList<>()).add(
          new AssessorGroupWorktypeItem(
              row.getScopeId(),
              displayGroupWorkType(row),
              workTypeIdForScope(row.getScopeId()),
              row.getLevelCode(),
              row.getLevelName(),
              row.getSortOrder()));
    }
    return result;
  }

  private Long workTypeIdForScope(Long scopeId) {
    if (scopeId == null) {
      return null;
    }
    CertificationTaskScopeEntity scope = scopeMapper.selectById(scopeId);
    return scope == null ? null : scope.getWorkTypeId();
  }

  private String displayGroupWorkType(CertificationAssessorGroupWorktypeEntity row) {
    Long workTypeId = workTypeIdForScope(row.getScopeId());
    if (workTypeId == null) {
      return row.getWorkTypeName();
    }
    return workTypeCoverageService.pathOf(workTypeId);
  }

  private void requireAssessorCoversGroupWorktypes(CertificationInfoAssessorEntity assessor, Long taskId, Long groupId) {
    List<CertificationAssessorGroupWorktypeEntity> rows = groupWorktypeMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorGroupWorktypeEntity>()
            .eq(CertificationAssessorGroupWorktypeEntity::getGroupId, groupId));
    if (rows != null && rows.stream().anyMatch(row -> row == null || row.getScopeId() == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "考评组工种范围关联已失效，请重新维护考评组负责工种");
    }
    List<Long> scopeIds = rows == null ? List.of() : rows.stream()
        .map(CertificationAssessorGroupWorktypeEntity::getScopeId)
        .distinct()
        .toList();
    List<Long> requiredWorkTypeIds = scopeIds.isEmpty()
        ? resolveTaskScopeWorkTypeIds(taskId)
        : resolveScopeWorkTypeIds(taskId, scopeIds);
    workTypeCoverageService.requireAssessorCovers(
        assessor.getWorkTypeJson(), assessor.getWorkTypeScope(), assessor.getAssessorName(), requiredWorkTypeIds);
  }

  private List<Long> resolveTaskScopeWorkTypeIds(Long taskId) {
    List<CertificationTaskScopeEntity> scopes = listScopes(taskId);
    if (scopes == null || scopes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "当前认定任务未配置工种范围，不能绕过考评员工种授权校验；请先维护认定任务工种范围");
    }
    if (scopes.stream().anyMatch(scope -> scope == null || scope.getId() == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "当前认定任务工种范围数据不完整，请先维护任务工种范围");
    }
    return resolveScopeWorkTypeIds(taskId, scopes.stream().map(CertificationTaskScopeEntity::getId).toList());
  }

  private List<Long> resolveScopeWorkTypeIds(Long taskId, List<Long> scopeIds) {
    List<Long> normalized = scopeIds == null ? List.of()
        : scopeIds.stream().filter(Objects::nonNull).distinct().toList();
    if (normalized.isEmpty()) {
      return resolveTaskScopeWorkTypeIds(taskId);
    }
    Map<Long, CertificationTaskScopeEntity> scopes = listScopes(taskId).stream()
        .filter(scope -> scope.getId() != null)
        .collect(Collectors.toMap(CertificationTaskScopeEntity::getId, scope -> scope, (left, right) -> left));
    List<Long> requiredIds = new ArrayList<>(normalized.size());
    for (Long scopeId : normalized) {
      CertificationTaskScopeEntity scope = scopes.get(scopeId);
      if (scope == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "工种范围不属于当前认定任务或已失效，请重新维护考评组负责工种");
      }
      if (!StringUtils.hasText(scope.getWorkTypeName()) && scope.getWorkTypeId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "认定任务工种范围缺少工种，请先维护任务工种范围");
      }
      requiredIds.add(workTypeCoverageService.resolveWorkTypeId(scope.getWorkTypeId(), scope.getWorkTypeName()));
    }
    return requiredIds.stream().distinct().toList();
  }

  private CertificationTaskEntity findTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private List<CertificationTaskScopeEntity> listScopes(Long taskId) {
    if (taskId == null) {
      return List.of();
    }
    List<CertificationTaskScopeEntity> scopes = scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTaskId, taskId)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
        .orderByAsc(CertificationTaskScopeEntity::getId));
    return scopes == null ? List.of() : scopes;
  }

  private CertificationTaskScopeEntity firstScope(Long taskId) {
    List<CertificationTaskScopeEntity> scopes = listScopes(taskId);
    return scopes.isEmpty() ? null : scopes.get(0);
  }

  private CertificationCandidateEntity requireCandidate(Long taskId, Long candidateId) {
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    return candidate;
  }

  private CertificationExamSiteEntity requireExamSite(Long siteId) {
    CertificationExamSiteEntity site = examSiteMapper.selectById(siteId);
    if (site == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考场不存在");
    }
    return site;
  }

  private CertificationAssessorEntity requireAssessor(Long assessorId) {
    CertificationAssessorEntity assessor = assessorMapper.selectById(assessorId);
    if (assessor == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员不存在");
    }
    return assessor;
  }

  private CertificationInfoAssessorEntity requireInfoAssessor(Long assessorId) {
    CertificationInfoAssessorEntity assessor = infoAssessorMapper.selectById(assessorId);
    if (assessor == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员不存在");
    }
    return assessor;
  }

  private void requireInfoAssessorAvailable(CertificationInfoAssessorEntity assessor) {
    if (assessor == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员不存在");
    }
    if (!"approved".equalsIgnoreCase(assessor.getAuditStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评员未入库，不能分配");
    }
    LocalDate today = LocalDate.now();
    if (assessor.getValidUntil() != null && assessor.getValidUntil().isBefore(today)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "证书已过期的考评员不得分配任务");
    }
  }

  /**
   * A tablet member follows one leader-owned task context. The same assessor may be bound to many
   * tasks, but all unfinished bindings must use that same leader.
   */
  private void requireAssessorCompatibleWithLeader(Long assessorId, Long leaderAssessorId, Long targetTaskId) {
    if (assessorId == null || leaderAssessorId == null) {
      return;
    }
    List<CertificationTaskAssessorEntity> bindings = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getAssessorId, assessorId)
            .ne(CertificationTaskAssessorEntity::getTaskId, targetTaskId)
            .isNotNull(CertificationTaskAssessorEntity::getGroupId));
    if (bindings == null || bindings.isEmpty()) {
      return;
    }
    bindings = bindings.stream().filter(CertificationTaskAssessorEntity::isOccupying).toList();
    if (bindings.isEmpty()) {
      return;
    }
    List<Long> groupIds = bindings.stream().map(CertificationTaskAssessorEntity::getGroupId)
        .filter(Objects::nonNull).distinct().toList();
    List<Long> taskIds = bindings.stream().map(CertificationTaskAssessorEntity::getTaskId)
        .filter(Objects::nonNull).distinct().toList();
    if (groupIds.isEmpty() || taskIds.isEmpty()) {
      return;
    }
    Map<Long, CertificationTaskAssessorGroupEntity> groups = groupMapper.selectBatchIds(groupIds).stream()
        .collect(Collectors.toMap(CertificationTaskAssessorGroupEntity::getId, Function.identity(), (left, right) -> left));
    Map<Long, CertificationTaskEntity> tasks = taskMapper.selectBatchIds(taskIds).stream()
        .collect(Collectors.toMap(CertificationTaskEntity::getId, Function.identity(), (left, right) -> left));
    boolean conflict = bindings.stream().anyMatch(binding -> {
      CertificationTaskEntity task = tasks.get(binding.getTaskId());
      CertificationTaskAssessorGroupEntity group = groups.get(binding.getGroupId());
      return task != null && !"completed".equals(task.getStatus()) && group != null
          && group.getLeaderAssessorId() != null && !leaderAssessorId.equals(group.getLeaderAssessorId());
    });
    if (conflict) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评员存在由其他组长负责的未完成任务，不能加入当前考评组");
    }
  }

  private CertificationInfoExamStaffEntity requireInfoExamStaff(Long staffId) {
    CertificationInfoExamStaffEntity staff = infoExamStaffMapper.selectById(staffId);
    if (staff == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考务人员不存在");
    }
    if (!INFO_APPROVED.equalsIgnoreCase(staff.getAuditStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考务人员未审核通过，不能绑定到任务");
    }
    if (!"idle".equalsIgnoreCase(staff.getWorkStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考务人员非空闲，不能绑定到任务");
    }
    return staff;
  }

  private CertificationInfoExamEquipmentEntity requireInfoEquipment(Long equipmentId) {
    CertificationInfoExamEquipmentEntity equipment = infoEquipmentMapper.selectById(equipmentId);
    if (equipment == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "设备不存在");
    }
    if (!INFO_APPROVED.equalsIgnoreCase(equipment.getAuditStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "设备未审核通过，不能配置到任务");
    }
    if (!"idle".equalsIgnoreCase(equipment.getEquipmentStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "设备非空闲中，不能配置到任务");
    }
    return equipment;
  }

  private void refreshInfoEquipmentUsage(Long equipmentId) {
    if (equipmentId == null) {
      return;
    }
    CertificationInfoExamEquipmentEntity equipment = infoEquipmentMapper.selectById(equipmentId);
    if (equipment == null || "disabled".equalsIgnoreCase(equipment.getEquipmentStatus())) {
      return;
    }
    long used = safeCount(taskEquipmentMapper.selectCount(new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
        .eq(CertificationTaskEquipmentEntity::getEquipmentId, equipmentId)));
    equipment.setReferencedCount((int) used);
    equipment.setEquipmentStatus(used > 0 ? "in_use" : "idle");
    infoEquipmentMapper.updateById(equipment);
  }

  private void markInfoAssessorWorkStatus(Long assessorId, String status) {
    if (assessorId == null) {
      return;
    }
    markInfoAssessorsWorkStatus(List.of(assessorId), status);
  }

  private void markInfoAssessorsWorkStatus(List<Long> assessorIds, String status) {
    if (!StringUtils.hasText(status)) {
      return;
    }
    List<Long> ids = assessorIds == null ? List.of()
        : assessorIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return;
    }
    CertificationInfoAssessorEntity patch = new CertificationInfoAssessorEntity();
    patch.setWorkStatus(status);
    infoAssessorMapper.update(patch, new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
        .in(CertificationInfoAssessorEntity::getId, ids));
  }

  private void markInfoExamStaffWorkStatus(Long staffId, String status) {
    if (staffId == null || !StringUtils.hasText(status)) {
      return;
    }
    CertificationInfoExamStaffEntity info = infoExamStaffMapper.selectById(staffId);
    if (info == null) {
      return;
    }
    info.setWorkStatus(status);
    infoExamStaffMapper.updateById(info);
  }

  private void releaseInfoExamStaffIfIdle(Long staffId) {
    if (staffId == null) {
      return;
    }
    long used = safeCount(taskExamStaffMapper.selectCount(new LambdaQueryWrapper<CertificationTaskExamStaffEntity>()
        .eq(CertificationTaskExamStaffEntity::getStaffId, staffId)));
    if (used > 0) {
      return;
    }
    markInfoExamStaffWorkStatus(staffId, "idle");
  }

  private void releaseInfoAssessorIfIdle(Long assessorId) {
    if (assessorId == null) {
      return;
    }
    long used = safeCount(taskAssessorMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
        .eq(CertificationTaskAssessorEntity::getAssessorId, assessorId)
        .in(CertificationTaskAssessorEntity::getWorkStatus, List.of("assigned", "preparing"))));
    if (used > 0) {
      return;
    }
    markInfoAssessorWorkStatus(assessorId, "idle");
  }

  private CertificationTaskAssessorGroupEntity requireGroup(Long taskId, Long groupId) {
    CertificationTaskAssessorGroupEntity group = groupMapper.selectById(groupId);
    if (group == null || !taskId.equals(group.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评组不存在");
    }
    return group;
  }

  private long candidateCount(Long taskId) {
    return safeCount(candidateMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getStatus, "active")));
  }

  private void touchTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task != null) {
      task.setLastBusinessUpdatedAt(LocalDateTime.now());
      taskMapper.updateById(task);
    }
  }

  private <T> PageResponse<T> page(Page<?> page, List<T> records) {
    return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal());
  }

  private long safeCount(Long count) {
    return count == null ? 0 : count;
  }

  private int nullToZero(Integer value) {
    return value == null ? 0 : value;
  }

  private int nullToOne(Integer value) {
    return value == null || value < 1 ? 1 : value;
  }

  private String resolveTaskExamType(String examType) {
    if (!StringUtils.hasText(examType)) {
      return "theory";
    }
    String value = examType.trim();
    if ("theory".equals(value) || "practical".equals(value)) {
      return value;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的考试类型");
  }

  private void clearInternalSupervisorIfNeeded(Long taskId, ExamStaffRole role) {
    if (role.internalSupervisor()) {
      taskExamStaffMapper.clearInternalSupervisorsByTask(taskId);
    }
  }

  private void applyExamStaffRole(CertificationTaskExamStaffEntity entity, ExamStaffRole role) {
    entity.setTaskRole(role.code());
    entity.setRoleName(role.name());
    entity.setIsLeader(role.internalSupervisor() ? 1 : 0);
  }

  private ExamStaffRole resolveExamStaffRole(String taskRole) {
    String code = StringUtils.hasText(taskRole) ? taskRole.trim() : EXAM_STAFF_ROLE_EXAM_STAFF;
    return switch (code) {
      case EXAM_STAFF_ROLE_INTERNAL_SUPERVISOR -> new ExamStaffRole(code, "内督", true);
      case EXAM_STAFF_ROLE_EXAM_STAFF -> new ExamStaffRole(code, "考务员", false);
      case EXAM_STAFF_ROLE_INVIGILATOR -> new ExamStaffRole(code, "监考员", false);
      case EXAM_STAFF_ROLE_WORKER -> new ExamStaffRole(code, "工作人员", false);
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的考务人员角色");
    };
  }

  private record ExamStaffRole(String code, String name, boolean internalSupervisor) {
  }

  private record ChecklistTemplateItem(Integer itemNo, String itemName, String quantityText) {
  }

  private LocalDate parseDate(String value) {
    return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
  }

}
