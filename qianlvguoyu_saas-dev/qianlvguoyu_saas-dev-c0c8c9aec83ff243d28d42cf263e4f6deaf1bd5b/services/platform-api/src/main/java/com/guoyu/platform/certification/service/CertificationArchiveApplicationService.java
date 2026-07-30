package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.archive.storage.ArchiveFileObject;
import com.guoyu.platform.certification.archive.storage.FileStorageService;
import com.guoyu.platform.certification.dto.CertificationArchiveCandidateQueryReq;
import com.guoyu.platform.certification.dto.CertificationArchiveConfirmReq;
import com.guoyu.platform.certification.dto.CertificationArchiveImportBatchQueryReq;
import com.guoyu.platform.certification.dto.CertificationArchiveRecordQueryReq;
import com.guoyu.platform.certification.dto.CertificationArchiveTaskQueryReq;
import com.guoyu.platform.certification.entity.CertificationArchiveBasicSnapshotEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveCandidateMaterialEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveImportErrorEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveMaterialEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveRecordEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationArchiveBasicSnapshotMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveCandidateMaterialMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveMaterialMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveRecordMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskEquipmentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.CertificationArchiveBasicSnapshotVO;
import com.guoyu.platform.certification.vo.CertificationArchiveCandidateMaterialVO;
import com.guoyu.platform.certification.vo.CertificationArchiveCandidateVO;
import com.guoyu.platform.certification.vo.CertificationArchiveCheckItemVO;
import com.guoyu.platform.certification.vo.CertificationArchiveCompleteCheckVO;
import com.guoyu.platform.certification.vo.CertificationArchiveDetailVO;
import com.guoyu.platform.certification.vo.CertificationArchiveImportBatchVO;
import com.guoyu.platform.certification.vo.CertificationArchiveImportErrorVO;
import com.guoyu.platform.certification.vo.CertificationArchiveMaterialVO;
import com.guoyu.platform.certification.vo.CertificationArchiveRecordVO;
import com.guoyu.platform.certification.vo.CertificationArchiveTaskVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CertificationArchiveApplicationService {
  private static final DataFormatter CELL_FORMATTER = new DataFormatter();
  private static final List<MaterialDef> TASK_MATERIALS = List.of(
      new MaterialDef("RESULT_CONFIRM_FORM", "职业技能等级认定评价结果确认表"),
      new MaterialDef("BUSINESS_APPLICATION", "广西壮族自治区职业技能鉴定业务申请表"),
      new MaterialDef("STAFF_ARRANGEMENT", "四类人员安排表"),
      new MaterialDef("SCORE_PUBLICITY", "职业技能等级认定成绩公示文件"),
      new MaterialDef("QUALITY_SUPERVISION", "广西技能人才评价质量督导表"),
      new MaterialDef("SCORE_ROSTER", "职业技能等级认定成绩花名册"),
      new MaterialDef("CERTIFIED_ROSTER", "职业技能等级获证人员花名册"),
      new MaterialDef("THEORY_EXAM_MATERIAL", "职业技能等级认定理论考务材料"),
      new MaterialDef("SKILL_EXAM_MATERIAL", "职业技能等级认定技能考务材料")
  );
  private static final Map<String, MaterialDef> CANDIDATE_MATERIALS = Map.ofEntries(
      Map.entry("评分表", new MaterialDef("SCORE_FORM", "评分表")),
      Map.entry("score", new MaterialDef("SCORE_FORM", "评分表")),
      Map.entry("试卷", new MaterialDef("EXAM_PAPER", "试卷")),
      Map.entry("paper", new MaterialDef("EXAM_PAPER", "试卷")),
      Map.entry("鉴定表", new MaterialDef("IDENTIFICATION", "鉴定表")),
      Map.entry("身份证", new MaterialDef("ID_CARD", "身份证")),
      Map.entry("idcard", new MaterialDef("ID_CARD", "身份证")),
      Map.entry("学历证明", new MaterialDef("EDUCATION", "学历证明")),
      Map.entry("education", new MaterialDef("EDUCATION", "学历证明")),
      Map.entry("工作年限证明", new MaterialDef("WORK_PROOF", "工作证明")),
      Map.entry("工作证明", new MaterialDef("WORK_PROOF", "工作证明")),
      Map.entry("培训合格证", new MaterialDef("TRAIN_CERT", "培训合格证")),
      Map.entry("train", new MaterialDef("TRAIN_CERT", "培训合格证"))
  );
  private static final Set<String> REQUIRED_CANDIDATE_TYPES = Set.of(
      "SCORE_FORM", "EXAM_PAPER", "IDENTIFICATION", "ID_CARD", "EDUCATION", "WORK_PROOF", "TRAIN_CERT"
  );

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskPhaseMapper phaseMapper;
  private final CertificationCandidateMapper sourceCandidateMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationTaskExamStaffMapper examStaffMapper;
  private final CertificationTaskEquipmentMapper equipmentMapper;
  private final CertificationTaskExamSiteMapper examSiteMapper;
  private final CertificationTaskAssessorMapper assessorMapper;
  private final CertificationArchiveRecordMapper recordMapper;
  private final CertificationArchiveBasicSnapshotMapper snapshotMapper;
  private final CertificationArchiveMaterialMapper materialMapper;
  private final CertificationArchiveCandidateMapper candidateMapper;
  private final CertificationArchiveCandidateMaterialMapper candidateMaterialMapper;
  private final CertificationArchiveImportBatchMapper importBatchMapper;
  private final CertificationArchiveImportErrorMapper importErrorMapper;
  private final FileStorageService fileStorageService;
  private final ObjectMapper objectMapper;

  public CertificationArchiveApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      CertificationCandidateMapper sourceCandidateMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationTaskExamStaffMapper examStaffMapper,
      CertificationTaskEquipmentMapper equipmentMapper,
      CertificationTaskExamSiteMapper examSiteMapper,
      CertificationTaskAssessorMapper assessorMapper,
      CertificationArchiveRecordMapper recordMapper,
      CertificationArchiveBasicSnapshotMapper snapshotMapper,
      CertificationArchiveMaterialMapper materialMapper,
      CertificationArchiveCandidateMapper candidateMapper,
      CertificationArchiveCandidateMaterialMapper candidateMaterialMapper,
      CertificationArchiveImportBatchMapper importBatchMapper,
      CertificationArchiveImportErrorMapper importErrorMapper,
      FileStorageService fileStorageService,
      ObjectMapper objectMapper
  ) {
    this.taskMapper = taskMapper;
    this.phaseMapper = phaseMapper;
    this.sourceCandidateMapper = sourceCandidateMapper;
    this.scopeMapper = scopeMapper;
    this.examStaffMapper = examStaffMapper;
    this.equipmentMapper = equipmentMapper;
    this.examSiteMapper = examSiteMapper;
    this.assessorMapper = assessorMapper;
    this.recordMapper = recordMapper;
    this.snapshotMapper = snapshotMapper;
    this.materialMapper = materialMapper;
    this.candidateMapper = candidateMapper;
    this.candidateMaterialMapper = candidateMaterialMapper;
    this.importBatchMapper = importBatchMapper;
    this.importErrorMapper = importErrorMapper;
    this.fileStorageService = fileStorageService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationArchiveTaskVO> pageAvailableTasks(CertificationArchiveTaskQueryReq req) {
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .and(StringUtils.hasText(req.getKeyword()), w -> w
            .like(CertificationTaskEntity::getTaskNo, req.getKeyword())
            .or()
            .like(CertificationTaskEntity::getTaskName, req.getKeyword()))
        .orderByDesc(CertificationTaskEntity::getLastBusinessUpdatedAt)
        .orderByDesc(CertificationTaskEntity::getUpdatedAt);
    // 认定归档模块只展示 current_phase = archive 的任务
    String currentPhase = StringUtils.hasText(req.getCurrentPhase())
        ? req.getCurrentPhase()
        : CertificationTaskPhases.ARCHIVE;
    wrapper.eq(CertificationTaskEntity::getCurrentPhase, currentPhase);
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CertificationTaskEntity::getStatus, req.getStatus());
    }
    Page<CertificationTaskEntity> page = taskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<CertificationArchiveTaskVO> items = page.getRecords().stream().map(this::toTaskVO).toList();
    return new PageResponse<>(items, req.getPage(), req.getPageSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationArchiveRecordVO> pageRecords(CertificationArchiveRecordQueryReq req) {
    Page<CertificationArchiveRecordEntity> page = recordMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CertificationArchiveRecordEntity>()
            .eq(StringUtils.hasText(req.getArchiveStatus()), CertificationArchiveRecordEntity::getArchiveStatus, req.getArchiveStatus())
            .and(StringUtils.hasText(req.getKeyword()), w -> w
                .like(CertificationArchiveRecordEntity::getTaskNo, req.getKeyword())
                .or()
                .like(CertificationArchiveRecordEntity::getTaskName, req.getKeyword()))
            .orderByDesc(CertificationArchiveRecordEntity::getUpdatedAt));
    return new PageResponse<>(page.getRecords().stream().map(this::toRecordVO).toList(), req.getPage(), req.getPageSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CertificationArchiveDetailVO getDetail(Long archiveId) {
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    return new CertificationArchiveDetailVO(
        toRecordVO(record),
        getBasicSnapshotVO(archiveId),
        listMaterials(archiveId),
        listImportBatchesByArchive(archiveId)
    );
  }

  @Transactional
  public CertificationArchiveBasicSnapshotVO archiveBasic(Long taskId) {
    CertificationTaskEntity task = getCompletedTask(taskId);
    CertificationArchiveRecordEntity record = getOrCreateRecord(task);
    CertificationArchiveBasicSnapshotEntity existing = snapshotMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveBasicSnapshotEntity>()
        .eq(CertificationArchiveBasicSnapshotEntity::getArchiveId, record.getId()));
    if (existing != null) {
      return toBasicVO(existing);
    }
    CertificationArchiveBasicSnapshotEntity snapshot = new CertificationArchiveBasicSnapshotEntity();
    snapshot.setArchiveId(record.getId());
    snapshot.setTenantId(current().getTenantId());
    snapshot.setCompletedAt(record.getCompletedAt());
    snapshot.setCandidateCount(record.getCandidateCount());
    snapshot.setTaskJson(json(task));
    snapshot.setScopeJson(buildScopeSnapshotJson(taskId));
    snapshot.setExamStaffJson(buildExamStaffSnapshotJson(taskId));
    snapshot.setDeviceJson(buildDeviceSnapshotJson(taskId));
    snapshot.setRoomJson(buildRoomSnapshotJson(taskId));
    snapshot.setEvaluatorJson(buildEvaluatorSnapshotJson(taskId));
    snapshotMapper.insert(snapshot);
    record.setBasicArchived(true);
    record.setArchiveStatus("BASIC_ARCHIVED");
    touch(record);
    recordMapper.updateById(record);
    return toBasicVO(snapshot);
  }

  @Transactional(readOnly = true)
  public CertificationArchiveBasicSnapshotVO getBasicSnapshot(Long archiveId) {
    return getBasicSnapshotVO(archiveId);
  }

  @Transactional(readOnly = true)
  public List<CertificationArchiveMaterialVO> listMaterials(Long archiveId) {
    getRecord(archiveId);
    return materialMapper.selectList(new LambdaQueryWrapper<CertificationArchiveMaterialEntity>()
            .eq(CertificationArchiveMaterialEntity::getArchiveId, archiveId)
            .orderByAsc(CertificationArchiveMaterialEntity::getMaterialType))
        .stream().map(this::toMaterialVO).toList();
  }

  @Transactional
  public CertificationArchiveMaterialVO uploadMaterial(Long archiveId, String materialType, String materialName, MultipartFile file) {
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    ensureNotArchived(record);
    MaterialDef def = taskMaterialDef(materialType, materialName);
    String safeName = fileStorageService.sanitizeFileName(file.getOriginalFilename());
    String fileKey = "certification/archive/%s/%d/task-materials/%s/%s".formatted(record.getTaskNo(), archiveId, def.code(), safeName);
    ArchiveFileObject stored = fileStorageService.store(file, fileKey);
    CertificationArchiveMaterialEntity entity = materialMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveMaterialEntity>()
        .eq(CertificationArchiveMaterialEntity::getArchiveId, archiveId)
        .eq(CertificationArchiveMaterialEntity::getMaterialType, def.code()));
    if (entity == null) {
      entity = new CertificationArchiveMaterialEntity();
      entity.setTenantId(current().getTenantId());
      entity.setArchiveId(archiveId);
      entity.setMaterialType(def.code());
      entity.setMaterialGroup("TASK_ARCHIVE");
    }
    fillMaterial(entity, def.name(), stored);
    if (entity.getId() == null) {
      materialMapper.insert(entity);
    } else {
      materialMapper.updateById(entity);
    }
    refreshMaterialArchived(record);
    return toMaterialVO(entity);
  }

  @Transactional
  public void deleteMaterial(Long archiveId, Long materialId) {
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    ensureNotArchived(record);
    CertificationArchiveMaterialEntity entity = getMaterial(archiveId, materialId);
    materialMapper.deleteById(entity.getId());
    refreshMaterialArchived(record);
  }

  @Transactional
  public CertificationArchiveImportBatchVO importCandidateMaterials(Long archiveId, MultipartFile zipFile) {
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    ensureNotArchived(record);
    String batchNo = "ARCHIVE-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    CertificationArchiveImportBatchEntity batch = new CertificationArchiveImportBatchEntity();
    batch.setTenantId(current().getTenantId());
    batch.setArchiveId(archiveId);
    batch.setBatchNo(batchNo);
    batch.setImportType("CANDIDATE_MATERIAL");
    batch.setZipFileName(fileStorageService.sanitizeFileName(zipFile.getOriginalFilename()));
    batch.setStatus("UPLOADED");
    batch.setTotalCandidates(0);
    batch.setSuccessCount(0);
    batch.setErrorCount(0);
    importBatchMapper.insert(batch);
    String zipKey = "certification/archive/%s/%d/import-batches/%d/%s".formatted(record.getTaskNo(), archiveId, batch.getId(), batch.getZipFileName());
    ArchiveFileObject storedZip = fileStorageService.store(zipFile, zipKey);
    batch.setZipFileKey(storedZip.fileKey());
    batch.setStatus("PARSING");
    importBatchMapper.updateById(batch);
    try {
      parseZip(record, batch, fileStorageService.load(storedZip.fileKey()));
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档 ZIP 读取失败");
    }
    return toBatchVO(batch);
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationArchiveImportBatchVO> pageImportBatches(CertificationArchiveImportBatchQueryReq req) {
    Page<CertificationArchiveImportBatchEntity> page = importBatchMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CertificationArchiveImportBatchEntity>()
            .eq(req.getArchiveId() != null, CertificationArchiveImportBatchEntity::getArchiveId, req.getArchiveId())
            .eq(StringUtils.hasText(req.getStatus()), CertificationArchiveImportBatchEntity::getStatus, req.getStatus())
            .like(StringUtils.hasText(req.getKeyword()), CertificationArchiveImportBatchEntity::getBatchNo, req.getKeyword())
            .orderByDesc(CertificationArchiveImportBatchEntity::getCreatedAt));
    return new PageResponse<>(page.getRecords().stream().map(this::toBatchVO).toList(), req.getPage(), req.getPageSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CertificationArchiveImportBatchVO getImportBatch(Long batchId) {
    return toBatchVO(getBatch(batchId));
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationArchiveImportErrorVO> pageImportErrors(Long batchId, CertificationArchiveTaskQueryReq req) {
    getBatch(batchId);
    Page<CertificationArchiveImportErrorEntity> page = importErrorMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CertificationArchiveImportErrorEntity>()
            .eq(CertificationArchiveImportErrorEntity::getBatchId, batchId)
            .orderByAsc(CertificationArchiveImportErrorEntity::getCreatedAt));
    return new PageResponse<>(page.getRecords().stream().map(this::toErrorVO).toList(), req.getPage(), req.getPageSize(), page.getTotal());
  }

  @Transactional
  public CertificationArchiveImportBatchVO confirmImport(Long batchId) {
    CertificationArchiveImportBatchEntity batch = getBatch(batchId);
    if (!"PARSE_SUCCESS".equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有解析成功的批次允许确认入库");
    }
    batch.setStatus("CONFIRMED");
    importBatchMapper.updateById(batch);
    CertificationArchiveRecordEntity record = getRecord(batch.getArchiveId());
    refreshMaterialArchived(record);
    return toBatchVO(batch);
  }

  @Transactional
  public CertificationArchiveImportBatchVO cancelImport(Long batchId) {
    CertificationArchiveImportBatchEntity batch = getBatch(batchId);
    if ("CONFIRMED".equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已确认批次不能取消");
    }
    batch.setStatus("CANCELLED");
    importBatchMapper.updateById(batch);
    return toBatchVO(batch);
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationArchiveCandidateVO> pageCandidates(Long archiveId, CertificationArchiveCandidateQueryReq req) {
    getRecord(archiveId);
    Page<CertificationArchiveCandidateEntity> page = candidateMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CertificationArchiveCandidateEntity>()
            .eq(CertificationArchiveCandidateEntity::getArchiveId, archiveId)
            .and(StringUtils.hasText(req.getKeyword()), w -> w
                .like(CertificationArchiveCandidateEntity::getName, req.getKeyword())
                .or()
                .like(CertificationArchiveCandidateEntity::getCandidateNo, req.getKeyword())
                .or()
                .like(CertificationArchiveCandidateEntity::getIdCard, req.getKeyword()))
            .orderByAsc(CertificationArchiveCandidateEntity::getCandidateNo));
    return new PageResponse<>(page.getRecords().stream().map(c -> toCandidateVO(c, true)).toList(),
        req.getPage(), req.getPageSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<CertificationArchiveCandidateMaterialVO> listCandidateMaterials(Long archiveId, Long candidateId) {
    CertificationArchiveCandidateEntity candidate = getCandidate(archiveId, candidateId);
    return candidateMaterialMapper.selectList(new LambdaQueryWrapper<CertificationArchiveCandidateMaterialEntity>()
            .eq(CertificationArchiveCandidateMaterialEntity::getArchiveCandidateId, candidate.getId()))
        .stream().map(this::toCandidateMaterialVO).toList();
  }

  @Transactional(readOnly = true)
  public CertificationArchiveCompleteCheckVO completeCheck(Long archiveId, boolean allowErrors) {
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    CertificationTaskEntity task = taskMapper.selectById(record.getTaskId());
    List<CertificationArchiveCheckItemVO> items = new ArrayList<>();
    boolean taskCompleted = isArchivableTask(task) || "ARCHIVED".equals(record.getArchiveStatus());
    items.add(new CertificationArchiveCheckItemVO("task_completed", "认定任务状态", taskCompleted,
        taskCompleted ? "认定筹备已完成，当前任务可归档" : "只有认定筹备已完成的任务允许归档"));
    items.add(new CertificationArchiveCheckItemVO("basic_archive_required", "基础信息归档", Boolean.TRUE.equals(record.getBasicArchived()),
        Boolean.TRUE.equals(record.getBasicArchived()) ? "基础信息已归档" : "请先完成基础信息归档"));
    boolean taskMaterialsReady = countUploadedTaskMaterials(archiveId) >= TASK_MATERIALS.size();
    items.add(new CertificationArchiveCheckItemVO("task_material_required", "认定材料", taskMaterialsReady,
        taskMaterialsReady ? "必要认定材料已上传" : "必要认定材料尚未上传完整"));
    boolean candidateMaterialsReady = countArchiveCandidateMaterials(archiveId) > 0;
    items.add(new CertificationArchiveCheckItemVO("candidate_material_confirmed", "考生材料", candidateMaterialsReady,
        candidateMaterialsReady ? "考生材料已导入" : "请导入考生材料"));
    long errors = importErrorMapper.selectCount(new LambdaQueryWrapper<CertificationArchiveImportErrorEntity>()
        .inSql(CertificationArchiveImportErrorEntity::getBatchId,
            "select id from certification_archive_import_batch where archive_id = " + archiveId + " and deleted = 0"));
    items.add(new CertificationArchiveCheckItemVO("import_error_handled", "导入异常", allowErrors || errors == 0,
        errors == 0 ? "无导入异常" : "存在 " + errors + " 条导入异常"));
    boolean passed = items.stream().allMatch(CertificationArchiveCheckItemVO::passed);
    return new CertificationArchiveCompleteCheckVO(passed, items);
  }

  @Transactional
  public CertificationArchiveCompleteCheckVO confirmArchive(Long archiveId, CertificationArchiveConfirmReq req) {
    boolean allowErrors = req != null && Boolean.TRUE.equals(req.allowErrors());
    CertificationArchiveCompleteCheckVO check = completeCheck(archiveId, allowErrors);
    if (!check.passed()) {
      return check;
    }
    CertificationArchiveRecordEntity record = getRecord(archiveId);
    record.setArchiveStatus("ARCHIVED");
    record.setArchivedBy(current().getUserId());
    record.setArchivedAt(LocalDateTime.now());
    touch(record);
    recordMapper.updateById(record);
    CertificationTaskEntity task = taskMapper.selectById(record.getTaskId());
    if (task != null) {
      // 认定归档完成 -> 推进到“认定完成”阶段，状态置为“已完成”
      CertificationTaskPhaseSupport.advancePhase(
          taskMapper,
          phaseMapper,
          task.getId(),
          CertificationTaskPhases.ARCHIVE,
          CertificationTaskPhases.CERTIFICATION_COMPLETED,
          CertificationTaskStatuses.COMPLETED
      );
      CertificationTaskPhaseSupport.syncPhaseProgress(taskMapper, phaseMapper, task.getId());
    }
    return check;
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> downloadFile(Long fileId) {
    FileRef ref = findFile(fileId);
    Resource resource = fileStorageService.load(ref.fileKey());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ref.fileName().replace("\"", "") + "\"")
        .body(resource);
  }

  private void parseZip(CertificationArchiveRecordEntity record, CertificationArchiveImportBatchEntity batch, Resource zipResource)
      throws IOException {
    Map<String, ZipPart> parts = new HashMap<>();
    byte[] excelBytes = null;
    String excelPath = null;
    try {
      ParsedZipContents zipContents = readZipContents(zipResource);
      parts = zipContents.parts();
      excelBytes = zipContents.excelBytes();
      excelPath = zipContents.excelPath();
    } catch (IOException ex) {
      addError(batch, null, null, null, "FILE_READ_FAILED", "ZIP 文件读取失败");
      failBatch(batch);
      return;
    }
    parts = normalizeZipPartPaths(parts, excelPath);
    if (excelBytes == null) {
      addError(batch, null, null, null, "MISSING_EXCEL", "缺少考生清单.xlsx");
      failBatch(batch);
      return;
    }
    List<CandidateRow> rows = readCandidateRows(excelBytes, batch);
    Set<String> seenIdCards = new HashSet<>();
    int success = 0;
    Map<String, Integer> stats = new LinkedHashMap<>();
    stats.put("考生数量", rows.size());
    for (CandidateRow row : rows) {
      if (!seenIdCards.add(row.idCard())) {
        addError(batch, row, "CANDIDATE_DUPLICATED", "同一批次身份证号重复");
        continue;
      }
      List<ZipPart> folderFiles = findFolderFiles(parts, row.folderName());
      if (folderFiles.isEmpty()) {
        addError(batch, row, "FOLDER_NOT_FOUND", "清单中的文件夹不存在：" + row.folderName());
        continue;
      }
      CertificationArchiveCandidateEntity candidate = saveCandidate(record, batch, row);
      Set<String> materialTypes = new HashSet<>();
      boolean rowPassed = true;
      for (ZipPart part : folderFiles) {
        MaterialDef def = candidateMaterialDef(part.name());
        if (def == null) {
          addError(batch, row, "UNSUPPORTED_FILE_TYPE", "无法识别材料类型：" + part.name());
          rowPassed = false;
          continue;
        }
        if (!materialTypes.add(def.code())) {
          addError(batch, row, "MATERIAL_DUPLICATED", "材料重复：" + def.name());
          rowPassed = false;
          continue;
        }
        if (!isAllowedFile(part.name())) {
          addError(batch, row, "UNSUPPORTED_FILE_TYPE", "不支持的文件类型：" + part.name());
          rowPassed = false;
          continue;
        }
        saveCandidateMaterial(record, batch, candidate, def, part);
        stats.put(def.name(), stats.getOrDefault(def.name(), 0) + 1);
      }
      for (String required : REQUIRED_CANDIDATE_TYPES) {
        if (!materialTypes.contains(required)) {
          addError(batch, row, "MATERIAL_MISSING", "缺少材料：" + candidateMaterialName(required));
          rowPassed = false;
        }
      }
      if (rowPassed) {
        success++;
      }
    }
    long errorCount = importErrorMapper.selectCount(new LambdaQueryWrapper<CertificationArchiveImportErrorEntity>()
        .eq(CertificationArchiveImportErrorEntity::getBatchId, batch.getId()));
    batch.setTotalCandidates(rows.size());
    batch.setSuccessCount(success);
    batch.setErrorCount((int) errorCount);
    batch.setStatisticsJson(json(stats));
    batch.setStatus(errorCount > 0 ? "PARSE_FAILED" : "PARSE_SUCCESS");
    importBatchMapper.updateById(batch);
    refreshMaterialArchived(record);
  }

  private List<CandidateRow> readCandidateRows(byte[] excelBytes, CertificationArchiveImportBatchEntity batch) {
    List<CandidateRow> rows = new ArrayList<>();
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
      var sheet = workbook.getSheetAt(0);
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        CandidateRow item = new CandidateRow(text(row.getCell(0)), text(row.getCell(1)), text(row.getCell(2)), text(row.getCell(3)));
        if (!StringUtils.hasText(item.candidateNo()) || !StringUtils.hasText(item.name())
            || !StringUtils.hasText(item.idCard()) || !StringUtils.hasText(item.folderName())) {
          addError(batch, item, "INVALID_EXCEL_FORMAT", "考生编号、姓名、身份证号、文件夹名称均必填");
          continue;
        }
        rows.add(item);
      }
    } catch (Exception ex) {
      addError(batch, null, null, null, "INVALID_EXCEL_FORMAT", "清单 Excel 格式错误");
    }
    return rows;
  }

  private CertificationArchiveCandidateEntity saveCandidate(CertificationArchiveRecordEntity record, CertificationArchiveImportBatchEntity batch, CandidateRow row) {
    CertificationArchiveCandidateEntity existing = candidateMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveCandidateEntity>()
        .eq(CertificationArchiveCandidateEntity::getArchiveId, record.getId())
        .eq(CertificationArchiveCandidateEntity::getIdCard, row.idCard()));
    CertificationArchiveCandidateEntity entity = existing == null ? new CertificationArchiveCandidateEntity() : existing;
    entity.setTenantId(current().getTenantId());
    entity.setArchiveId(record.getId());
    entity.setSourceBatchId(batch.getId());
    entity.setCandidateNo(row.candidateNo());
    entity.setName(row.name());
    entity.setIdCard(row.idCard());
    entity.setMaskedIdCard(maskIdCard(row.idCard()));
    entity.setFolderName(row.folderName());
    if (entity.getId() == null) {
      candidateMapper.insert(entity);
    } else {
      candidateMapper.updateById(entity);
    }
    return entity;
  }

  private void saveCandidateMaterial(CertificationArchiveRecordEntity record, CertificationArchiveImportBatchEntity batch,
      CertificationArchiveCandidateEntity candidate, MaterialDef def, ZipPart part) {
    String originalName = part.name().substring(part.name().lastIndexOf('/') + 1);
    String safeName = fileStorageService.sanitizeFileName(originalName);
    String fileKey = "certification/archive/%s/%d/candidates/%s/%s/%s"
        .formatted(record.getTaskNo(), record.getId(), candidate.getIdCard(), def.code(), safeName);
    ArchiveFileObject stored = fileStorageService.store(part.bytes(), safeName, contentType(safeName), fileKey);
    CertificationArchiveCandidateMaterialEntity entity = candidateMaterialMapper.selectOne(
        new LambdaQueryWrapper<CertificationArchiveCandidateMaterialEntity>()
            .eq(CertificationArchiveCandidateMaterialEntity::getArchiveCandidateId, candidate.getId())
            .eq(CertificationArchiveCandidateMaterialEntity::getMaterialType, def.code()));
    if (entity == null) {
      entity = new CertificationArchiveCandidateMaterialEntity();
      entity.setTenantId(current().getTenantId());
      entity.setArchiveCandidateId(candidate.getId());
      entity.setMaterialType(def.code());
      entity.setMaterialGroup(def.group());
    }
    entity.setSourceBatchId(batch.getId());
    entity.setFileName(stored.fileName());
    entity.setFileKey(stored.fileKey());
    entity.setFileSize(stored.fileSize());
    entity.setFileHash(stored.fileHash());
    entity.setContentType(stored.contentType());
    if (entity.getId() == null) {
      candidateMaterialMapper.insert(entity);
    } else {
      candidateMaterialMapper.updateById(entity);
    }
  }

  private CertificationArchiveTaskVO toTaskVO(CertificationTaskEntity task) {
    CertificationArchiveRecordEntity record = recordMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveRecordEntity>()
        .eq(CertificationArchiveRecordEntity::getTaskId, task.getId()));
    return new CertificationArchiveTaskVO(
        task.getId(), task.getTaskNo(), task.getTaskName(), task.getSourceName(), summarizeWorkTypes(task.getId()), summarizeLevelNames(task.getId()),
        task.getLastBusinessUpdatedAt(), countCandidates(task.getId()), archiveTaskStatus(task),
        record == null ? null : record.getId(),
        record != null && Boolean.TRUE.equals(record.getBasicArchived()),
        record != null && isMaterialArchiveComplete(record.getId()),
        record != null && hasCandidateMaterialsArchived(record.getId()),
        record != null && hasTaskMaterialsArchived(record.getId()),
        record == null ? "NOT_ARCHIVED" : record.getArchiveStatus(),
        record == null ? null : record.getArchivedAt(),
        record == null ? null : record.getLastOperatorName());
  }

  private CertificationArchiveRecordVO toRecordVO(CertificationArchiveRecordEntity e) {
    return new CertificationArchiveRecordVO(e.getId(), e.getTaskId(), e.getTaskNo(), e.getTaskName(), e.getBatchName(),
        e.getWorkTypeName(), e.getLevelName(), e.getCompletedAt(), e.getCandidateCount(), e.getArchiveStatus(),
        e.getBasicArchived(), e.getMaterialArchived(), e.getArchivedBy(), e.getArchivedAt(), e.getLastOperatorName(),
        e.getCreatedAt(), e.getUpdatedAt());
  }

  private CertificationArchiveBasicSnapshotVO toBasicVO(CertificationArchiveBasicSnapshotEntity e) {
    return new CertificationArchiveBasicSnapshotVO(e.getId(), e.getArchiveId(), e.getCompletedAt(), e.getCandidateCount(),
        e.getTaskJson(), e.getScopeJson(), e.getExamStaffJson(), e.getDeviceJson(), e.getRoomJson(), e.getEvaluatorJson(), e.getCreatedAt());
  }

  private CertificationArchiveMaterialVO toMaterialVO(CertificationArchiveMaterialEntity e) {
    return new CertificationArchiveMaterialVO(e.getId(), e.getArchiveId(), e.getMaterialType(), e.getMaterialGroup(),
        e.getMaterialName(), e.getFileName(), e.getFileSize(), e.getFileHash(), e.getContentType(), e.getFileStatus(),
        e.getUploadedBy(), e.getUploadedAt());
  }

  private CertificationArchiveImportBatchVO toBatchVO(CertificationArchiveImportBatchEntity e) {
    return new CertificationArchiveImportBatchVO(e.getId(), e.getArchiveId(), e.getBatchNo(), e.getImportType(),
        e.getZipFileName(), e.getStatus(), e.getTotalCandidates(), e.getSuccessCount(), e.getErrorCount(),
        e.getStatisticsJson(), e.getCreatedAt(), e.getUpdatedAt());
  }

  private CertificationArchiveImportErrorVO toErrorVO(CertificationArchiveImportErrorEntity e) {
    return new CertificationArchiveImportErrorVO(e.getId(), e.getBatchId(), e.getCandidateNo(), e.getName(), e.getIdCard(),
        maskIdCard(e.getIdCard()), e.getErrorType(), e.getErrorMessage(), e.getCreatedAt());
  }

  private CertificationArchiveCandidateVO toCandidateVO(CertificationArchiveCandidateEntity e, boolean withMaterials) {
    List<CertificationArchiveCandidateMaterialVO> materials = withMaterials
        ? candidateMaterialMapper.selectList(new LambdaQueryWrapper<CertificationArchiveCandidateMaterialEntity>()
            .eq(CertificationArchiveCandidateMaterialEntity::getArchiveCandidateId, e.getId()))
          .stream().map(this::toCandidateMaterialVO).toList()
        : List.of();
    return new CertificationArchiveCandidateVO(e.getId(), e.getArchiveId(), e.getCandidateNo(), e.getName(), e.getIdCard(),
        e.getMaskedIdCard(), e.getFolderName(), materials);
  }

  private CertificationArchiveCandidateMaterialVO toCandidateMaterialVO(CertificationArchiveCandidateMaterialEntity e) {
    return new CertificationArchiveCandidateMaterialVO(e.getId(), e.getArchiveCandidateId(), e.getMaterialType(), e.getMaterialGroup(),
        e.getFileName(), e.getFileSize(), e.getFileHash(), e.getContentType());
  }

  private CertificationArchiveRecordEntity getOrCreateRecord(CertificationTaskEntity task) {
    CertificationArchiveRecordEntity existing = recordMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveRecordEntity>()
        .eq(CertificationArchiveRecordEntity::getTaskId, task.getId()));
    if (existing != null) {
      return existing;
    }
    CertificationArchiveRecordEntity record = new CertificationArchiveRecordEntity();
    record.setTenantId(current().getTenantId());
    record.setTaskId(task.getId());
    record.setTaskNo(task.getTaskNo());
    record.setTaskName(task.getTaskName());
    record.setBatchName(task.getSourceName());
    record.setWorkTypeName(summarizeWorkTypes(task.getId()));
    record.setLevelName(summarizeLevelNames(task.getId()));
    record.setCompletedAt(task.getLastBusinessUpdatedAt() == null ? LocalDateTime.of(LocalDate.now(), LocalTime.MIN) : task.getLastBusinessUpdatedAt());
    record.setCandidateCount(countCandidates(task.getId()));
    record.setArchiveStatus("NOT_ARCHIVED");
    record.setBasicArchived(false);
    record.setMaterialArchived(false);
    touch(record);
    recordMapper.insert(record);
    return record;
  }

  private CertificationTaskEntity getCompletedTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    if (!isArchivableTask(task)) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有认定筹备已完成的任务允许归档");
    }
    return task;
  }

  private boolean isArchivableTask(CertificationTaskEntity task) {
    if (task == null) {
      return false;
    }
    // 归档动作只允许在“认定归档阶段”执行
    return CertificationTaskPhases.ARCHIVE.equals(task.getCurrentPhase());
  }

  private String archiveTaskStatus(CertificationTaskEntity task) {
    if (task == null) {
      return null;
    }
    return task.getStatus();
  }

  private CertificationArchiveRecordEntity getRecord(Long archiveId) {
    CertificationArchiveRecordEntity record = recordMapper.selectById(archiveId);
    if (record == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "归档记录不存在");
    }
    return record;
  }

  private CertificationArchiveImportBatchEntity getBatch(Long batchId) {
    CertificationArchiveImportBatchEntity batch = importBatchMapper.selectById(batchId);
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "导入批次不存在");
    }
    return batch;
  }

  private CertificationArchiveMaterialEntity getMaterial(Long archiveId, Long materialId) {
    CertificationArchiveMaterialEntity entity = materialMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveMaterialEntity>()
        .eq(CertificationArchiveMaterialEntity::getArchiveId, archiveId)
        .eq(CertificationArchiveMaterialEntity::getId, materialId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "归档材料不存在");
    }
    return entity;
  }

  private CertificationArchiveCandidateEntity getCandidate(Long archiveId, Long candidateId) {
    CertificationArchiveCandidateEntity entity = candidateMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveCandidateEntity>()
        .eq(CertificationArchiveCandidateEntity::getArchiveId, archiveId)
        .eq(CertificationArchiveCandidateEntity::getId, candidateId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "归档考生不存在");
    }
    return entity;
  }

  private CertificationArchiveBasicSnapshotVO getBasicSnapshotVO(Long archiveId) {
    CertificationArchiveBasicSnapshotEntity snapshot = snapshotMapper.selectOne(new LambdaQueryWrapper<CertificationArchiveBasicSnapshotEntity>()
        .eq(CertificationArchiveBasicSnapshotEntity::getArchiveId, archiveId));
    return snapshot == null ? null : toBasicVO(snapshot);
  }

  private List<CertificationArchiveImportBatchVO> listImportBatchesByArchive(Long archiveId) {
    return importBatchMapper.selectList(new LambdaQueryWrapper<CertificationArchiveImportBatchEntity>()
            .eq(CertificationArchiveImportBatchEntity::getArchiveId, archiveId)
            .orderByDesc(CertificationArchiveImportBatchEntity::getCreatedAt))
        .stream().map(this::toBatchVO).toList();
  }

  private void fillMaterial(CertificationArchiveMaterialEntity entity, String materialName, ArchiveFileObject stored) {
    entity.setMaterialName(materialName);
    entity.setFileName(stored.fileName());
    entity.setFileKey(stored.fileKey());
    entity.setFileSize(stored.fileSize());
    entity.setFileHash(stored.fileHash());
    entity.setContentType(stored.contentType());
    entity.setFileStatus("uploaded");
    entity.setUploadedBy(current().getUserId());
    entity.setUploadedAt(LocalDateTime.now());
  }

  private void refreshMaterialArchived(CertificationArchiveRecordEntity record) {
    boolean materialComplete = isMaterialArchiveComplete(record.getId());
    record.setMaterialArchived(materialComplete);
    if (materialComplete && !"ARCHIVED".equals(record.getArchiveStatus())) {
      record.setArchiveStatus("MATERIAL_ARCHIVED");
    } else if (Boolean.TRUE.equals(record.getBasicArchived())
        && hasCandidateMaterialsArchived(record.getId())
        && !"ARCHIVED".equals(record.getArchiveStatus())
        && !"MATERIAL_ARCHIVED".equals(record.getArchiveStatus())) {
      record.setArchiveStatus("BASIC_ARCHIVED");
    }
    touch(record);
    recordMapper.updateById(record);
  }

  private boolean isMaterialArchiveComplete(Long archiveId) {
    return hasTaskMaterialsArchived(archiveId) && hasCandidateMaterialsArchived(archiveId);
  }

  private boolean hasTaskMaterialsArchived(Long archiveId) {
    return countUploadedTaskMaterials(archiveId) >= TASK_MATERIALS.size();
  }

  private boolean hasCandidateMaterialsArchived(Long archiveId) {
    return countArchiveCandidateMaterials(archiveId) > 0;
  }

  private long countUploadedTaskMaterials(Long archiveId) {
    return materialMapper.selectCount(new LambdaQueryWrapper<CertificationArchiveMaterialEntity>()
        .eq(CertificationArchiveMaterialEntity::getArchiveId, archiveId)
        .eq(CertificationArchiveMaterialEntity::getFileStatus, "uploaded"));
  }

  private long countArchiveCandidateMaterials(Long archiveId) {
    List<Long> candidateIds = candidateMapper.selectList(new LambdaQueryWrapper<CertificationArchiveCandidateEntity>()
            .eq(CertificationArchiveCandidateEntity::getArchiveId, archiveId)
            .select(CertificationArchiveCandidateEntity::getId))
        .stream()
        .map(CertificationArchiveCandidateEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    if (candidateIds.isEmpty()) {
      return 0;
    }
    return candidateMaterialMapper.selectCount(new LambdaQueryWrapper<CertificationArchiveCandidateMaterialEntity>()
        .in(CertificationArchiveCandidateMaterialEntity::getArchiveCandidateId, candidateIds));
  }

  private void ensureNotArchived(CertificationArchiveRecordEntity record) {
    if ("ARCHIVED".equals(record.getArchiveStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成归档的记录不允许修改");
    }
  }

  private void failBatch(CertificationArchiveImportBatchEntity batch) {
    long errors = importErrorMapper.selectCount(new LambdaQueryWrapper<CertificationArchiveImportErrorEntity>()
        .eq(CertificationArchiveImportErrorEntity::getBatchId, batch.getId()));
    batch.setStatus("PARSE_FAILED");
    batch.setErrorCount((int) errors);
    importBatchMapper.updateById(batch);
  }

  private void addError(CertificationArchiveImportBatchEntity batch, CandidateRow row, String errorType, String message) {
    addError(batch, row == null ? null : row.candidateNo(), row == null ? null : row.name(), row == null ? null : row.idCard(), errorType, message);
  }

  private void addError(CertificationArchiveImportBatchEntity batch, String candidateNo, String name, String idCard, String errorType, String message) {
    CertificationArchiveImportErrorEntity error = new CertificationArchiveImportErrorEntity();
    error.setTenantId(current().getTenantId());
    error.setBatchId(batch.getId());
    error.setCandidateNo(candidateNo);
    error.setName(name);
    error.setIdCard(idCard);
    error.setErrorType(errorType);
    error.setErrorMessage(message);
    importErrorMapper.insert(error);
  }

  private MaterialDef taskMaterialDef(String materialType, String materialName) {
    return TASK_MATERIALS.stream()
        .filter(item -> item.code().equals(materialType))
        .findFirst()
        .orElseGet(() -> new MaterialDef(StringUtils.hasText(materialType) ? materialType : "CUSTOM", materialName));
  }

  private Map<String, ZipPart> normalizeZipPartPaths(Map<String, ZipPart> parts, String excelPath) {
    if (parts.isEmpty() || !StringUtils.hasText(excelPath) || "考生清单.xlsx".equals(excelPath)) {
      return parts;
    }
    String normalizedExcelPath = excelPath.replace("\\", "/");
    String[] segments = normalizedExcelPath.split("/");
    if (segments.length != 2 || !"考生清单.xlsx".equals(segments[1])) {
      return parts;
    }
    String rootPrefix = segments[0] + "/";
    boolean allUnderRoot = parts.keySet().stream().allMatch(name -> name.startsWith(rootPrefix));
    if (!allUnderRoot) {
      return parts;
    }
    Map<String, ZipPart> normalized = new HashMap<>();
    for (Map.Entry<String, ZipPart> entry : parts.entrySet()) {
      String normalizedKey = entry.getKey().substring(rootPrefix.length());
      normalized.put(normalizedKey, new ZipPart(normalizedKey, entry.getValue().bytes()));
    }
    return normalized;
  }

  private MaterialDef candidateMaterialDef(String path) {
    String fileName = path.substring(path.lastIndexOf('/') + 1);
    String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
    return CANDIDATE_MATERIALS.get(base);
  }

  private FileRef findFile(Long fileId) {
    CertificationArchiveMaterialEntity material = materialMapper.selectById(fileId);
    if (material != null) {
      getRecord(material.getArchiveId());
      return new FileRef(material.getFileName(), material.getFileKey());
    }
    CertificationArchiveCandidateMaterialEntity candidateMaterial = candidateMaterialMapper.selectById(fileId);
    if (candidateMaterial != null) {
      return new FileRef(candidateMaterial.getFileName(), candidateMaterial.getFileKey());
    }
    throw new BusinessException(ErrorCode.NOT_FOUND, "归档文件不存在");
  }

  private int countCandidates(Long taskId) {
    Long count = sourceCandidateMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, taskId));
    return count == null ? 0 : count.intValue();
  }

  private String summarizeWorkTypes(Long taskId) {
    return scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId))
        .stream()
        .map(CertificationTaskScopeEntity::getWorkTypeName)
        .filter(StringUtils::hasText)
        .distinct()
        .reduce((left, right) -> left + "、" + right)
        .orElse("");
  }

  private String summarizeLevelNames(Long taskId) {
    return scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId))
        .stream()
        .map(CertificationTaskScopeEntity::getLevelName)
        .filter(StringUtils::hasText)
        .distinct()
        .reduce((left, right) -> left + "、" + right)
        .orElse("");
  }

  private PlatformUserPrincipal current() {
    return SecurityContext.currentUser();
  }

  private void touch(CertificationArchiveRecordEntity record) {
    PlatformUserPrincipal user = current();
    record.setLastOperatorId(user.getUserId());
    record.setLastOperatorName(StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername());
  }

  private String buildScopeSnapshotJson(Long taskId) {
    List<Map<String, String>> items = scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder))
        .stream()
        .filter(item -> StringUtils.hasText(item.getLevelName()))
        .map(item -> Map.of("levelName", item.getLevelName()))
        .toList();
    return json(items);
  }

  private String buildExamStaffSnapshotJson(Long taskId) {
    List<Map<String, String>> items = examStaffMapper.selectList(new LambdaQueryWrapper<CertificationTaskExamStaffEntity>()
            .eq(CertificationTaskExamStaffEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskExamStaffEntity::getSortOrder))
        .stream()
        .filter(item -> StringUtils.hasText(item.getStaffName()))
        .map(item -> {
          Map<String, String> map = new LinkedHashMap<>();
          map.put("staffName", item.getStaffName());
          String role = StringUtils.hasText(item.getRoleName()) ? item.getRoleName() : item.getTaskRole();
          if (StringUtils.hasText(role)) {
            map.put("roleName", role);
          }
          return map;
        })
        .toList();
    return json(items);
  }

  private String buildDeviceSnapshotJson(Long taskId) {
    List<Map<String, String>> items = equipmentMapper.selectList(new LambdaQueryWrapper<CertificationTaskEquipmentEntity>()
            .eq(CertificationTaskEquipmentEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskEquipmentEntity::getSortOrder))
        .stream()
        .filter(item -> StringUtils.hasText(item.getEquipmentName()))
        .map(item -> Map.of("equipmentName", item.getEquipmentName()))
        .toList();
    return json(items);
  }

  private String buildRoomSnapshotJson(Long taskId) {
    List<Map<String, String>> items = examSiteMapper.selectList(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
            .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskExamSiteEntity::getSortOrder))
        .stream()
        .filter(item -> StringUtils.hasText(item.getSiteName()))
        .map(item -> Map.of("siteName", item.getSiteName()))
        .toList();
    return json(items);
  }

  private String buildEvaluatorSnapshotJson(Long taskId) {
    List<Map<String, String>> items = assessorMapper.selectList(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .orderByAsc(CertificationTaskAssessorEntity::getSortOrder))
        .stream()
        .filter(item -> StringUtils.hasText(item.getAssessorName()))
        .map(item -> Map.of("assessorName", item.getAssessorName()))
        .toList();
    return json(items);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "归档快照序列化失败");
    }
  }

  private String text(Cell cell) {
    if (cell == null) {
      return "";
    }
    return CELL_FORMATTER.formatCellValue(cell).trim();
  }

  private String maskIdCard(String value) {
    if (!StringUtils.hasText(value) || value.length() < 10) {
      return value;
    }
    return value.substring(0, 6) + "********" + value.substring(value.length() - 4);
  }

  private boolean isAllowedFile(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
        || lower.endsWith(".png") || lower.endsWith(".doc") || lower.endsWith(".docx")
        || lower.endsWith(".xls") || lower.endsWith(".xlsx");
  }

  private String contentType(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".pdf")) {
      return "application/pdf";
    }
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    return "application/octet-stream";
  }

  private byte[] readAll(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    input.transferTo(output);
    return output.toByteArray();
  }

  private ParsedZipContents readZipContents(Resource zipResource) throws IOException {
    Path tempFile = null;
    try {
      File zipFile = zipResource.isFile() ? zipResource.getFile() : null;
      if (zipFile == null) {
        tempFile = Files.createTempFile("cert-archive-import-", ".zip");
        try (InputStream input = zipResource.getInputStream()) {
          Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        zipFile = tempFile.toFile();
      }
      IOException lastFailure = null;
      ParsedZipContents bestContents = null;
      int bestScore = -1;
      for (Charset charset : List.of(Charset.forName("GBK"), StandardCharsets.UTF_8, Charset.forName("GB18030"))) {
        try (ZipFile zip = new ZipFile(zipFile, charset)) {
          ParsedZipContents contents = extractZipContents(zip);
          int score = scoreZipContents(contents);
          if (score > bestScore) {
            bestScore = score;
            bestContents = contents;
          }
        } catch (IllegalArgumentException | IOException ex) {
          lastFailure = ex instanceof IOException ? (IOException) ex : new IOException(ex);
        }
      }
      if (bestContents == null || bestContents.excelBytes() == null || bestContents.excelBytes().length == 0) {
        throw lastFailure != null ? lastFailure : new IOException("ZIP 文件读取失败");
      }
      return bestContents;
    } finally {
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  private int scoreZipContents(ParsedZipContents contents) {
    int score = contents.parts().size();
    if (contents.excelBytes() != null && contents.excelBytes().length > 0) {
      score += 100;
      if (contents.excelPath() != null && contents.excelPath().endsWith("考生清单.xlsx")) {
        score += 50;
      }
    }
    long materialFiles = contents.parts().keySet().stream()
        .filter(name -> name.contains("/") && isAllowedFile(name))
        .count();
    score += (int) materialFiles;
    return score;
  }

  private List<ZipPart> findFolderFiles(Map<String, ZipPart> parts, String folderName) {
    String normalized = folderName == null ? "" : folderName.replace("\\", "/").trim();
    if (!StringUtils.hasText(normalized)) {
      return List.of();
    }
    String prefix = normalized.endsWith("/") ? normalized : normalized + "/";
    List<ZipPart> exact = parts.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(prefix))
        .map(Map.Entry::getValue)
        .toList();
    if (!exact.isEmpty()) {
      return exact;
    }
    String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
    return parts.entrySet().stream()
        .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
        .map(Map.Entry::getValue)
        .toList();
  }

  private String candidateMaterialName(String code) {
    return CANDIDATE_MATERIALS.values().stream()
        .filter(def -> def.code().equals(code))
        .map(MaterialDef::name)
        .findFirst()
        .orElse(code);
  }

  private ParsedZipContents extractZipContents(ZipFile zip) throws IOException {
    Map<String, ZipPart> parts = new HashMap<>();
    byte[] excelBytes = null;
    String excelPath = null;
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (entry.isDirectory()) {
        continue;
      }
      try (InputStream input = zip.getInputStream(entry)) {
        byte[] bytes = readAll(input);
        String name = entry.getName().replace("\\", "/");
        if (name.endsWith("考生清单.xlsx")) {
          excelBytes = bytes;
          excelPath = name;
        } else {
          parts.put(name, new ZipPart(name, bytes));
        }
      }
    }
    return new ParsedZipContents(parts, excelBytes, excelPath);
  }

  private record MaterialDef(String code, String name, String group) {
    MaterialDef(String code, String name) {
      this(code, name, "REGISTRATION_ARCHIVE");
    }
  }

  private record CandidateRow(String candidateNo, String name, String idCard, String folderName) {
  }

  private record ZipPart(String name, byte[] bytes) {
  }

  private record ParsedZipContents(Map<String, ZipPart> parts, byte[] excelBytes, String excelPath) {
  }

  private record FileRef(String fileName, String fileKey) {
  }
}
