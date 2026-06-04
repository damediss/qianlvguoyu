package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.PreparationAssessorDispatchReq;
import com.guoyu.platform.certification.dto.PreparationAssessorGroupSaveReq;
import com.guoyu.platform.certification.dto.PreparationAssessorQueryReq;
import com.guoyu.platform.certification.dto.PreparationAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationAssessorTaskDraftReq;
import com.guoyu.platform.certification.dto.PreparationAssessorTaskReviewReq;
import com.guoyu.platform.certification.dto.PreparationAssessorWorkbenchQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateDocumentSaveReq;
import com.guoyu.platform.certification.dto.PreparationCandidateImportReq;
import com.guoyu.platform.certification.dto.PreparationCandidateQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.dto.PreparationDocumentQueryReq;
import com.guoyu.platform.certification.dto.PreparationDocumentReviewReq;
import com.guoyu.platform.certification.dto.PreparationExamSiteQueryReq;
import com.guoyu.platform.certification.dto.PreparationExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialGenerateReq;
import com.guoyu.platform.certification.dto.PreparationMaterialItemSaveReq;
import com.guoyu.platform.certification.dto.PreparationMaterialQueryReq;
import com.guoyu.platform.certification.dto.PreparationScoreReq;
import com.guoyu.platform.certification.dto.PreparationTaskAssessorSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskEquipmentSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffRoleReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamStaffSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskQueryReq;
import com.guoyu.platform.certification.entity.CertificationApplicationMaterialBatchEntity;
import com.guoyu.platform.certification.entity.CertificationApplicationMaterialItemEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorNotificationEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskScoreEntity;
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
import com.guoyu.platform.certification.mapper.CertificationAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorNotificationMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskScoreMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
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
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.PreparationAssessorGroupVO;
import com.guoyu.platform.certification.vo.PreparationAssessorTaskVO;
import com.guoyu.platform.certification.vo.PreparationAssessorVO;
import com.guoyu.platform.certification.vo.PreparationCandidateDocumentVO;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.certification.vo.PreparationCheckItemVO;
import com.guoyu.platform.certification.vo.PreparationCompleteCheckVO;
import com.guoyu.platform.certification.vo.PreparationExamSiteVO;
import com.guoyu.platform.certification.vo.PreparationImportBatchVO;
import com.guoyu.platform.certification.vo.PreparationImportErrorVO;
import com.guoyu.platform.certification.vo.PreparationMaterialBatchVO;
import com.guoyu.platform.certification.vo.PreparationMaterialItemVO;
import com.guoyu.platform.certification.vo.PreparationScoreVO;
import com.guoyu.platform.certification.vo.PreparationTaskAssessorVO;
import com.guoyu.platform.certification.vo.PreparationTaskEquipmentVO;
import com.guoyu.platform.certification.vo.PreparationTaskExamSiteVO;
import com.guoyu.platform.certification.vo.PreparationTaskExamStaffVO;
import com.guoyu.platform.certification.vo.PreparationTaskOverviewVO;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
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
  private static final List<String> CANDIDATE_DOC_CODES = List.of("DOC001", "DOC002");
  private static final String INFO_APPROVED = "approved";

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
  private final CertificationInfoExamStaffMapper infoExamStaffMapper;
  private final CertificationTaskEquipmentMapper taskEquipmentMapper;
  private final CertificationInfoExamEquipmentMapper infoEquipmentMapper;
  private final CertificationAssessorMapper assessorMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final CertificationAssessorTaskScoreMapper scoreMapper;
  private final CertificationAssessorNotificationMapper notificationMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationInfoAssessorMapper infoAssessorMapper;
  private final FileApplicationService fileApplicationService;

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
      CertificationInfoExamStaffMapper infoExamStaffMapper,
      CertificationTaskEquipmentMapper taskEquipmentMapper,
      CertificationInfoExamEquipmentMapper infoEquipmentMapper,
      CertificationAssessorMapper assessorMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      CertificationAssessorTaskScoreMapper scoreMapper,
      CertificationAssessorNotificationMapper notificationMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationInfoAssessorMapper infoAssessorMapper,
      FileApplicationService fileApplicationService
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
    this.infoExamStaffMapper = infoExamStaffMapper;
    this.taskEquipmentMapper = taskEquipmentMapper;
    this.infoEquipmentMapper = infoEquipmentMapper;
    this.assessorMapper = assessorMapper;
    this.groupMapper = groupMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.scoreMapper = scoreMapper;
    this.notificationMapper = notificationMapper;
    this.scopeMapper = scopeMapper;
    this.infoAssessorMapper = infoAssessorMapper;
    this.fileApplicationService = fileApplicationService;
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
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationTaskEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getCurrentPhase())) {
      wrapper.eq(CertificationTaskEntity::getCurrentPhase, req.getCurrentPhase());
    }
    Page<CertificationTaskEntity> page = taskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::overviewFromTask).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public PreparationTaskOverviewVO overview(Long taskId) {
    return overviewFromTask(findTask(taskId));
  }

  @Transactional
  public PreparationImportBatchVO createImportBatch(Long taskId, PreparationCandidateImportReq req) {
    findTask(taskId);
    CertificationCandidateImportBatchEntity batch = new CertificationCandidateImportBatchEntity();
    batch.setTaskId(taskId);
    batch.setBatchNo("CIB-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
    batch.setSourceType(StringUtils.hasText(req.sourceType()) ? req.sourceType() : "manual");
    batch.setSourceFileId(req.sourceFileId());
    batch.setStartedAt(LocalDateTime.now());
    batch.setStatus("completed");
    batch.setRemark(req.remark());
    int total = req.candidates() == null ? 0 : req.candidates().size();
    int success = 0;
    int failed = 0;
    if (!CollectionUtils.isEmpty(req.candidates())) {
      for (int i = 0; i < req.candidates().size(); i++) {
        try {
          createCandidate(taskId, req.candidates().get(i));
          success++;
        } catch (BusinessException ex) {
          failed++;
          CertificationCandidateImportErrorEntity error = new CertificationCandidateImportErrorEntity();
          error.setTaskId(taskId);
          error.setBatchId(batch.getId());
          error.setRowNo(i + 1);
          error.setCandidateName(req.candidates().get(i).candidateName());
          error.setIdCardNo(req.candidates().get(i).idCardNo());
          error.setPhone(req.candidates().get(i).phone());
          error.setErrorCode("candidate_invalid");
          error.setErrorMessage(ex.getMessage());
          error.setDeleted(0);
          importErrorMapper.insert(error);
        }
      }
    }
    batch.setTotalCount(total);
    batch.setSuccessCount(success);
    batch.setFailedCount(failed);
    batch.setDuplicateCount(failed);
    batch.setCompletedAt(LocalDateTime.now());
    batch.setDeleted(0);
    importBatchMapper.insert(batch);
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
    if (StringUtils.hasText(req.getLevelCode())) {
      wrapper.eq(CertificationCandidateEntity::getLevelCode, req.getLevelCode());
    }
    Page<CertificationCandidateEntity> page = candidateMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<PreparationCandidateVO> candidates = new ArrayList<>(
        page.getRecords().stream().map(PreparationCandidateVO::from).toList());
    enrichCandidateDocumentSummary(taskId, candidates);
    return page(page, candidates);
  }

  @Transactional
  public PreparationCandidateVO createCandidate(Long taskId, PreparationCandidateSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    validateCandidate(task, req, null);
    CertificationCandidateEntity entity = new CertificationCandidateEntity();
    applyCandidate(entity, task, req);
    entity.setTaskId(taskId);
    entity.setCandidateNo("C-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
    entity.setDocumentCompletedCount(0);
    entity.setDocumentRequiredCount(CANDIDATE_DOC_CODES.size());
    entity.setDocumentComplete(false);
    entity.setStatus("active");
    entity.setDeleted(0);
    candidateMapper.insert(entity);
    Long candidateId = resolveCandidateIdAfterInsert(taskId, entity);
    if (candidateId != null) {
      initializeCandidateDocuments(taskId, candidateId);
    }
    touchTask(taskId);
    return PreparationCandidateVO.from(entity);
  }

  @Transactional
  public PreparationCandidateVO updateCandidate(Long taskId, Long candidateId, PreparationCandidateSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationCandidateEntity entity = requireCandidate(taskId, candidateId);
    validateCandidate(task, req, candidateId);
    applyCandidate(entity, task, req);
    candidateMapper.updateById(entity);
    touchTask(taskId);
    return PreparationCandidateVO.from(entity);
  }

  @Transactional
  public void deleteCandidate(Long taskId, Long candidateId) {
    findTask(taskId);
    requireCandidate(taskId, candidateId);
    long generated = safeCount(materialItemMapper.selectCount(new LambdaQueryWrapper<CertificationApplicationMaterialItemEntity>()
        .eq(CertificationApplicationMaterialItemEntity::getTaskId, taskId)
        .eq(CertificationApplicationMaterialItemEntity::getCandidateId, candidateId)
        .eq(CertificationApplicationMaterialItemEntity::getStatus, "generated")));
    if (generated > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已进入后续流程的考生不得删除");
    }
    candidateMapper.deleteById(candidateId);
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
  public PreparationCandidateDocumentVO saveCandidateDocument(Long taskId, PreparationCandidateDocumentSaveReq req) {
    findTask(taskId);
    requireCandidate(taskId, req.candidateId());
    if (!CANDIDATE_DOC_CODES.contains(req.docCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持上传报名信息表与两寸证件照");
    }
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
    entity.setStatus(req.fileId() == null ? "missing" : "pending_review");
    entity.setUploadedAt(req.fileId() == null ? null : LocalDateTime.now());
    entity.setRemark(req.remark());
    if (entity.getId() == null) {
      documentMapper.insert(entity);
    } else {
      documentMapper.updateById(entity);
    }
    refreshCandidateDocumentStats(taskId, req.candidateId());
    return PreparationCandidateDocumentVO.from(entity);
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
    long candidateCount = candidateCount(taskId);
    if (candidateCount == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前任务下不存在考生，不能生成申报材料");
    }
    long incomplete = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .eq(CertificationCandidateDocumentEntity::getRequired, true)
        .ne(CertificationCandidateDocumentEntity::getStatus, "approved")));
    if (incomplete > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "存在未审核通过的必传资料，不能生成申报材料");
    }
    int requested = CollectionUtils.isEmpty(req.candidateIds()) ? Math.toIntExact(candidateCount) : req.candidateIds().size();
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
    return PreparationMaterialBatchVO.from(batch);
  }

  @Transactional(readOnly = true)
  public PreparationMaterialBatchVO getMaterialBatch(Long taskId, Long batchId) {
    findTask(taskId);
    CertificationApplicationMaterialBatchEntity entity = materialBatchMapper.selectById(batchId);
    if (entity == null || !taskId.equals(entity.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "申报材料批次不存在");
    }
    return PreparationMaterialBatchVO.from(entity);
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
    if (!"active".equals(site.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "停用考场不得绑定到新任务");
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
    taskExamSiteMapper.deleteById(taskSiteId);
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
      entity.setMaskedPhone(StringUtils.hasText(staff.getMaskedPhone()) ? staff.getMaskedPhone() : maskPhone(staff.getPhone()));
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
      entity.setMaskedPhone(StringUtils.hasText(staff.getMaskedPhone()) ? staff.getMaskedPhone() : maskPhone(staff.getPhone()));
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
    return taskEquipmentMapper.selectList(new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
            .eq(CertificationTaskEquipmentEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskEquipmentEntity::getSortOrder)
            .orderByDesc(CertificationTaskEquipmentEntity::getCreatedAt))
        .stream()
        .map(PreparationTaskEquipmentVO::from)
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
    return PreparationTaskEquipmentVO.from(entity);
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
    CertificationTaskAssessorGroupEntity entity = new CertificationTaskAssessorGroupEntity();
    applyGroup(entity, taskId, req);
    entity.setDeleted(0);
    groupMapper.insert(entity);
    touchTask(taskId);
    return PreparationAssessorGroupVO.from(entity, leaderName(entity.getLeaderAssessorId()));
  }

  @Transactional
  public PreparationAssessorGroupVO updateGroup(Long taskId, Long groupId, PreparationAssessorGroupSaveReq req) {
    findTask(taskId);
    CertificationTaskAssessorGroupEntity entity = requireGroup(taskId, groupId);
    applyGroup(entity, taskId, req);
    groupMapper.updateById(entity);
    touchTask(taskId);
    return PreparationAssessorGroupVO.from(entity, leaderName(entity.getLeaderAssessorId()));
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

    groupMapper.deleteById(groupId);
    touchTask(taskId);
  }

  @Transactional(readOnly = true)
  public List<PreparationAssessorGroupVO> listGroups(Long taskId) {
    findTask(taskId);
    return groupMapper.selectList(new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
            .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
            .orderByDesc(CertificationTaskAssessorGroupEntity::getCreatedAt))
        .stream()
        .map(group -> PreparationAssessorGroupVO.from(group, leaderName(group.getLeaderAssessorId())))
        .toList();
  }

  private String leaderName(Long leaderAssessorId) {
    if (leaderAssessorId == null) {
      return null;
    }
    CertificationInfoAssessorEntity info = infoAssessorMapper.selectById(leaderAssessorId);
    return info == null ? null : info.getAssessorName();
  }

  @Transactional
  public PreparationTaskAssessorVO addGroupMember(Long taskId, Long groupId, PreparationTaskAssessorSaveReq req) {
    CertificationTaskEntity task = findTask(taskId);
    requireGroup(taskId, groupId);
    CertificationInfoAssessorEntity assessor = requireInfoAssessor(req.assessorId());
    requireInfoAssessorAvailable(assessor);
    if (assessor.getValidUntil() != null && task.getExamDate() != null && assessor.getValidUntil().isBefore(task.getExamDate())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "证书已过期的考评员不得分配任务");
    }
    String taskWorkTypeName = summarizeWorkTypes(taskId);
    if (StringUtils.hasText(taskWorkTypeName)
        && (!StringUtils.hasText(assessor.getWorkTypeScope())
        || !assessor.getWorkTypeScope().toLowerCase(Locale.ROOT).contains(taskWorkTypeName.toLowerCase(Locale.ROOT)))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评员工种范围必须覆盖当前认定任务工种");
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
  public void removeGroupMember(Long taskId, Long groupId, Long memberId) {
    requireGroup(taskId, groupId);
    CertificationTaskAssessorEntity member = taskAssessorMapper.selectById(memberId);
    if (member == null) {
      return;
    }
    Long assessorId = member.getAssessorId();
    taskAssessorMapper.deleteById(memberId);
    releaseInfoAssessorIfIdle(assessorId);
    touchTask(taskId);
  }

  @Transactional
  public List<PreparationAssessorTaskVO> dispatchGroup(Long taskId, Long groupId, PreparationAssessorDispatchReq req) {
    CertificationTaskEntity task = findTask(taskId);
    CertificationTaskAssessorGroupEntity group = requireGroup(taskId, groupId);
    if (group.getLeaderAssessorId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "每个考评组必须有组长");
    }
    List<CertificationTaskAssessorEntity> members = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getGroupId, groupId));
    List<PreparationAssessorTaskVO> result = new ArrayList<>();
    for (CertificationTaskAssessorEntity member : members) {
      CertificationAssessorTaskEntity assessorTask = new CertificationAssessorTaskEntity();
      assessorTask.setTaskId(taskId);
      assessorTask.setGroupId(groupId);
      assessorTask.setTaskAssessorId(member.getId());
      assessorTask.setAssessorId(member.getAssessorId());
      assessorTask.setLeaderAssessorId(group.getLeaderAssessorId());
      assessorTask.setQuestionId("PRACTICAL-" + task.getId());
      assessorTask.setSubject(StringUtils.hasText(req.subject()) ? req.subject() : summarizeWorkTypes(taskId));
      assessorTask.setAssessmentFileId(req.assessmentFileId());
      assessorTask.setAssessmentFileName(req.assessmentFileName());
      String title = StringUtils.hasText(req.assessmentFileName()) ? req.assessmentFileName() : req.title();
      assessorTask.setTitle(StringUtils.hasText(title) ? title : "实操考评任务");
      assessorTask.setDurationMin(req.durationMin());
      assessorTask.setDueDate(req.dueDate());
      assessorTask.setStatus("dispatched");
      assessorTask.setRemark(req.remark());
      assessorTask.setDeleted(0);
      assessorTaskMapper.insert(assessorTask);
      member.setWorkStatus("assigned");
      taskAssessorMapper.updateById(member);
      result.add(PreparationAssessorTaskVO.from(
          assessorTask,
          resolveTaskName(assessorTask.getTaskId()),
          resolveGroupName(assessorTask.getGroupId()),
          resolveAssessorName(assessorTask.getAssessorId()),
          resolveAssessorName(assessorTask.getLeaderAssessorId()),
          List.of()
      ));

      markInfoAssessorWorkStatus(member.getAssessorId(), "assigned");

      CertificationAssessorNotificationEntity notification = new CertificationAssessorNotificationEntity();
      notification.setTaskId(taskId);
      notification.setAssessorId(member.getAssessorId());
      notification.setMessageType("task");
      notification.setMessageContent("您有新的认定考评任务：" + assessorTask.getTitle());
      notification.setReadStatus("unread");
      notification.setDeleted(0);
      notificationMapper.insert(notification);
    }
    group.setStatus("dispatched");
    groupMapper.updateById(group);
    touchTask(taskId);
    return result;
  }

  @Transactional(readOnly = true)
  public PageResponse<PreparationAssessorTaskVO> pageWorkbenchTasks(PreparationAssessorWorkbenchQueryReq req) {
    LambdaQueryWrapper<CertificationAssessorTaskEntity> wrapper = new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
        .orderByDesc(CertificationAssessorTaskEntity::getUpdatedAt);

    String view = normalizeWorkbenchView(req.getView());
    Long scopedAssessorId = req.getAssessorId() != null ? req.getAssessorId() : currentUserAssessorId();
    if ("leader".equals(view)) {
      if (scopedAssessorId != null) {
        wrapper.eq(CertificationAssessorTaskEntity::getLeaderAssessorId, scopedAssessorId);
      }
      if (StringUtils.hasText(req.getStatus())) {
        wrapper.eq(CertificationAssessorTaskEntity::getStatus, req.getStatus());
      } else {
        // 组长审批默认只看待复核
        wrapper.eq(CertificationAssessorTaskEntity::getStatus, "submitted");
      }
    } else {
      if (scopedAssessorId != null) {
        wrapper.eq(CertificationAssessorTaskEntity::getAssessorId, scopedAssessorId);
      }
      if (StringUtils.hasText(req.getStatus())) {
        wrapper.eq(CertificationAssessorTaskEntity::getStatus, req.getStatus());
      }
    }

    Page<CertificationAssessorTaskEntity> page =
        assessorTaskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return page(page, page.getRecords().stream().map(t -> PreparationAssessorTaskVO.from(
        t,
        resolveTaskName(t.getTaskId()),
        resolveGroupName(t.getGroupId()),
        resolveAssessorName(t.getAssessorId()),
        resolveAssessorName(t.getLeaderAssessorId()),
        List.of()
    )).toList());
  }

  @Transactional(readOnly = true)
  public PreparationAssessorTaskVO getWorkbenchTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = requireAssessorTask(assessorTaskId);
    List<PreparationScoreVO> scores = scoreMapper.selectList(new LambdaQueryWrapper<CertificationAssessorTaskScoreEntity>()
            .eq(CertificationAssessorTaskScoreEntity::getAssessorTaskId, assessorTaskId)
            .orderByAsc(CertificationAssessorTaskScoreEntity::getStepNo))
        .stream()
        .map(PreparationScoreVO::from)
        .toList();
    return PreparationAssessorTaskVO.from(
        task,
        resolveTaskName(task.getTaskId()),
        resolveGroupName(task.getGroupId()),
        resolveAssessorName(task.getAssessorId()),
        resolveAssessorName(task.getLeaderAssessorId()),
        scores
    );
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
  public PreparationAssessorTaskVO saveAssessorTaskDraft(Long assessorTaskId, PreparationAssessorTaskDraftReq req) {
    CertificationAssessorTaskEntity task = requireAssessorTask(assessorTaskId);
    validateAssessorOwner(task, req.assessorId());
    if ("reviewed".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已复核任务不得再次编辑");
    }
    List<PreparationScoreVO> scores = saveScores(task, req.scores());
    task.setProgressNote(req.progressNote());
    task.setStatus("scoring");
    task.setTotalScore(scores.stream()
        .map(PreparationScoreVO::score)
        .filter(score -> score != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
    assessorTaskMapper.updateById(task);
    return PreparationAssessorTaskVO.from(
        task,
        resolveTaskName(task.getTaskId()),
        resolveGroupName(task.getGroupId()),
        resolveAssessorName(task.getAssessorId()),
        resolveAssessorName(task.getLeaderAssessorId()),
        scores
    );
  }

  @Transactional
  public PreparationAssessorTaskVO submitAssessorTask(Long assessorTaskId, PreparationAssessorTaskDraftReq req) {
    PreparationAssessorTaskVO draft = saveAssessorTaskDraft(assessorTaskId, req);
    CertificationAssessorTaskEntity task = requireAssessorTask(assessorTaskId);
    task.setStatus("submitted");
    task.setSubmittedAt(LocalDateTime.now());
    assessorTaskMapper.updateById(task);
    return PreparationAssessorTaskVO.from(
        task,
        resolveTaskName(task.getTaskId()),
        resolveGroupName(task.getGroupId()),
        resolveAssessorName(task.getAssessorId()),
        resolveAssessorName(task.getLeaderAssessorId()),
        draft.scores()
    );
  }

  @Transactional
  public PreparationAssessorTaskVO reviewAssessorTask(Long assessorTaskId, PreparationAssessorTaskReviewReq req) {
    CertificationAssessorTaskEntity task = requireAssessorTask(assessorTaskId);
    if (req.reviewerAssessorId() == null || !req.reviewerAssessorId().equals(task.getLeaderAssessorId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "非本组组长不得复核任务");
    }
    String normalized = normalizeReviewStatus(req.reviewStatus());
    if ("reviewed".equals(normalized)) {
      task.setStatus("reviewed");
    } else {
      task.setStatus("returned");
    }
    task.setReviewNote(req.reviewNote());
    task.setReviewedAt(LocalDateTime.now());
    task.setReviewedBy(req.reviewerAssessorId());
    assessorTaskMapper.updateById(task);
    return getWorkbenchTask(assessorTaskId);
  }

  private String normalizeWorkbenchView(String view) {
    if (!StringUtils.hasText(view)) {
      return "assessor";
    }
    String normalized = view.trim().toLowerCase(Locale.ROOT);
    return "leader".equals(normalized) ? "leader" : "assessor";
  }

  private String normalizeReviewStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "returned";
    }
    String normalized = status.trim().toLowerCase(Locale.ROOT);
    if (List.of("reviewed", "approved", "pass", "passed", "ok").contains(normalized)) {
      return "reviewed";
    }
    if (List.of("returned", "rework", "reject", "rejected", "fail", "failed").contains(normalized)) {
      return "returned";
    }
    // 前端可能直接传中文/其它枚举：兜底按“打回”
    return "returned";
  }

  private Long currentUserAssessorId() {
    Long userId = currentUserId();
    if (userId == null) {
      return null;
    }
    CertificationInfoAssessorEntity entity = infoAssessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, userId)
            .last("LIMIT 1"));
    return entity == null ? null : entity.getId();
  }

  private Long currentUserId() {
    try {
      PlatformUserPrincipal principal = SecurityContext.currentUser();
      return principal.getUserId();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  @Transactional(readOnly = true)
  public PreparationCompleteCheckVO completeCheck(Long taskId) {
    findTask(taskId);
    List<PreparationCheckItemVO> items = new ArrayList<>();
    long candidateCount = candidateCount(taskId);
    items.add(new PreparationCheckItemVO("candidate_required", "考生名单", candidateCount > 0,
        candidateCount > 0 ? "已导入 " + candidateCount + " 名考生" : "当前任务下至少需要 1 名考生"));
    long incompleteDocs = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .eq(CertificationCandidateDocumentEntity::getRequired, true)
        .ne(CertificationCandidateDocumentEntity::getStatus, "approved")));
    items.add(new PreparationCheckItemVO("document_approved", "考生资料", incompleteDocs == 0,
        incompleteDocs == 0 ? "必传资料均已审核通过" : "存在未审核通过的必传资料"));
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
    long groupCount = safeCount(groupMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
        .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("assessor_group_required", "考评组", groupCount > 0,
        groupCount > 0 ? "已配置 " + groupCount + " 个考评组" : "至少配置 1 个考评组"));
    long leaderMissing = safeCount(groupMapper.selectCount(new LambdaQueryWrapper<CertificationTaskAssessorGroupEntity>()
        .eq(CertificationTaskAssessorGroupEntity::getTaskId, taskId)
        .isNull(CertificationTaskAssessorGroupEntity::getLeaderAssessorId)));
    items.add(new PreparationCheckItemVO("assessor_group_leader", "考评组组长", leaderMissing == 0,
        leaderMissing == 0 ? "考评组组长配置完整" : "存在未配置组长的考评组"));
    long dispatched = safeCount(assessorTaskMapper.selectCount(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
        .eq(CertificationAssessorTaskEntity::getTaskId, taskId)));
    items.add(new PreparationCheckItemVO("assessor_task_dispatched", "考评任务发放", dispatched > 0,
        dispatched > 0 ? "已发放考评任务" : "尚未发放考评任务"));
    return new PreparationCompleteCheckVO(items.stream().allMatch(PreparationCheckItemVO::passed), items);
  }

  @Transactional
  public PreparationCompleteCheckVO complete(Long taskId) {
    PreparationCompleteCheckVO check = completeCheck(taskId);
    if (!check.passed()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "筹备完成检查未通过");
    }
    CertificationTaskEntity task = findTask(taskId);
    task.setCurrentPhase("before_exam");
    task.setStatus("running");
    task.setLastBusinessUpdatedAt(LocalDateTime.now());
    taskMapper.updateById(task);
    CertificationTaskPhaseEntity phase = phaseMapper.selectOne(new LambdaQueryWrapper<CertificationTaskPhaseEntity>()
        .eq(CertificationTaskPhaseEntity::getTaskId, taskId)
        .eq(CertificationTaskPhaseEntity::getPhaseCode, "preparation"));
    if (phase != null) {
      phase.setStatus("completed");
      phase.setCompletedAt(LocalDateTime.now());
      phaseMapper.updateById(phase);
    }
    return check;
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

  private void validateCandidate(CertificationTaskEntity task, PreparationCandidateSaveReq req, Long currentCandidateId) {
    if (!ID_CARD_PATTERN.matcher(req.idCardNo()).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号格式必须合法");
    }
    if (StringUtils.hasText(req.phone()) && !MOBILE_PATTERN.matcher(req.phone()).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "联系电话格式必须合法");
    }
    Set<String> supportedLevelCodes = listScopes(task.getId()).stream()
        .map(CertificationTaskScopeEntity::getLevelCode)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
    if (!supportedLevelCodes.isEmpty() && StringUtils.hasText(req.levelCode())
        && !supportedLevelCodes.contains(req.levelCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "报考等级必须匹配当前认定任务支持的等级");
    }
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

  private void applyCandidate(CertificationCandidateEntity entity, CertificationTaskEntity task, PreparationCandidateSaveReq req) {
    entity.setCandidateName(req.candidateName());
    entity.setIdCardNo(req.idCardNo());
    entity.setMaskedIdCardNo(maskIdCard(req.idCardNo()));
    entity.setPhone(req.phone());
    entity.setMaskedPhone(maskPhone(req.phone()));
    CertificationTaskScopeEntity defaultScope = firstScope(task.getId());
    entity.setLevelCode(StringUtils.hasText(req.levelCode()) ? req.levelCode() : defaultScope == null ? null : defaultScope.getLevelCode());
    entity.setLevelName(StringUtils.hasText(req.levelName()) ? req.levelName() : defaultScope == null ? null : defaultScope.getLevelName());
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
    if (!CollectionUtils.isEmpty(templates)) {
      return templates;
    }
    return defaultCandidateDocumentTemplates();
  }

  private List<CertificationMaterialTemplateEntity> defaultCandidateDocumentTemplates() {
    return List.of(
        candidateDocTemplate("DOC001", "报名信息表", 1, true),
        candidateDocTemplate("DOC002", "两寸证件照", 2, true)
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

  private Long resolveCandidateIdAfterInsert(Long taskId, CertificationCandidateEntity entity) {
    if (entity.getId() != null) {
      return entity.getId();
    }
    CertificationCandidateEntity saved = candidateMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getIdCardNo, entity.getIdCardNo())
            .orderByDesc(CertificationCandidateEntity::getCreatedAt)
            .last("LIMIT 1"));
    if (saved != null) {
      entity.setId(saved.getId());
      return saved.getId();
    }
    return null;
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
    pruneObsoleteCandidateDocuments(taskId, candidateId);
    List<CertificationMaterialTemplateEntity> templates = candidateDocumentTemplates();
    if (CollectionUtils.isEmpty(templates)) {
      return;
    }
    for (CertificationMaterialTemplateEntity template : templates) {
      if (safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
          .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
          .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
          .eq(CertificationCandidateDocumentEntity::getDocCode, template.getItemCode()))) > 0) {
        continue;
      }
      CertificationCandidateDocumentEntity document = new CertificationCandidateDocumentEntity();
      document.setTaskId(taskId);
      document.setCandidateId(candidateId);
      document.setTemplateId(template.getId());
      document.setDocCode(template.getItemCode());
      document.setDocName(template.getItemName());
      document.setRequired(template.getRequired() == null || template.getRequired());
      document.setVersionNo(1);
      document.setStatus("missing");
      document.setDeleted(0);
      documentMapper.insert(document);
    }
    refreshCandidateDocumentStats(taskId, candidateId);
  }

  private void syncCandidateDocuments(Long taskId) {
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, taskId));
    if (CollectionUtils.isEmpty(candidates)) {
      return;
    }
    for (CertificationCandidateEntity candidate : candidates) {
      initializeCandidateDocuments(taskId, candidate.getId());
    }
  }

  private void pruneObsoleteCandidateDocuments(Long taskId, Long candidateId) {
    List<CertificationCandidateDocumentEntity> obsoleteDocuments = documentMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
            .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
            .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
            .notIn(CertificationCandidateDocumentEntity::getDocCode, CANDIDATE_DOC_CODES));
    if (CollectionUtils.isEmpty(obsoleteDocuments)) {
      return;
    }
    for (CertificationCandidateDocumentEntity document : obsoleteDocuments) {
      documentMapper.deleteById(document.getId());
    }
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

  private void refreshCandidateDocumentStats(Long taskId, Long candidateId) {
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null) {
      return;
    }
    long required = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
        .eq(CertificationCandidateDocumentEntity::getRequired, true)));
    long approved = safeCount(documentMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateDocumentEntity>()
        .eq(CertificationCandidateDocumentEntity::getTaskId, taskId)
        .eq(CertificationCandidateDocumentEntity::getCandidateId, candidateId)
        .eq(CertificationCandidateDocumentEntity::getRequired, true)
        .eq(CertificationCandidateDocumentEntity::getStatus, "approved")));
    candidate.setDocumentRequiredCount((int) required);
    candidate.setDocumentCompletedCount((int) approved);
    candidate.setDocumentComplete(required > 0 && required == approved);
    candidateMapper.updateById(candidate);
  }

  private List<PreparationScoreVO> saveScores(CertificationAssessorTaskEntity task, List<PreparationScoreReq> reqs) {
    if (CollectionUtils.isEmpty(reqs)) {
      return List.of();
    }
    List<PreparationScoreVO> result = new ArrayList<>();
    for (PreparationScoreReq req : reqs) {
      BigDecimal max = req.maxScore() == null ? BigDecimal.ZERO : req.maxScore();
      BigDecimal score = req.score();
      if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(max) > 0)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "得分不得小于 0，且不得超过步骤分值上限");
      }
      CertificationAssessorTaskScoreEntity entity = new CertificationAssessorTaskScoreEntity();
      entity.setAssessorTaskId(task.getId());
      entity.setTaskId(task.getTaskId());
      entity.setStepNo(req.stepNo());
      entity.setStepName(req.stepName());
      entity.setMaxScore(max);
      entity.setScore(score);
      entity.setRemark(req.remark());
      entity.setDeleted(0);
      scoreMapper.insert(entity);
      result.add(PreparationScoreVO.from(entity));
    }
    return result;
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
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "draft");
    entity.setRemark(req.remark());
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
    return scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTaskId, taskId)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
        .orderByAsc(CertificationTaskScopeEntity::getId));
  }

  private CertificationTaskScopeEntity firstScope(Long taskId) {
    List<CertificationTaskScopeEntity> scopes = listScopes(taskId);
    return scopes.isEmpty() ? null : scopes.get(0);
  }

  private String summarizeWorkTypes(Long taskId) {
    return listScopes(taskId).stream()
        .map(CertificationTaskScopeEntity::getWorkTypeName)
        .filter(StringUtils::hasText)
        .distinct()
        .collect(Collectors.joining("、"));
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
    if (!"idle".equalsIgnoreCase(assessor.getWorkStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评员非空闲，不能分配");
    }
    LocalDate today = LocalDate.now();
    if (assessor.getValidUntil() != null && assessor.getValidUntil().isBefore(today)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "证书已过期的考评员不得分配任务");
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
    if (assessorId == null || !StringUtils.hasText(status)) {
      return;
    }
    CertificationInfoAssessorEntity info = infoAssessorMapper.selectById(assessorId);
    if (info == null) {
      return;
    }
    info.setWorkStatus(status);
    infoAssessorMapper.updateById(info);
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
        .eq(CertificationTaskAssessorEntity::getAssessorId, assessorId)));
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

  private CertificationAssessorTaskEntity requireAssessorTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = assessorTaskMapper.selectById(assessorTaskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评任务不存在");
    }
    return task;
  }

  private void validateAssessorOwner(CertificationAssessorTaskEntity task, Long assessorId) {
    if (assessorId == null || !assessorId.equals(task.getAssessorId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非任务所属考评员不得编辑评分");
    }
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

  private LocalDate parseDate(String value) {
    return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
  }

  private String maskPhone(String phone) {
    if (!StringUtils.hasText(phone) || phone.length() != 11) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
  }

  private String maskIdCard(String idCardNo) {
    if (!StringUtils.hasText(idCardNo) || idCardNo.length() < 10) {
      return idCardNo;
    }
    return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
  }
}
