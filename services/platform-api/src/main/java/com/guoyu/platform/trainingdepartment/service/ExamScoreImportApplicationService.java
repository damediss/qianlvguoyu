package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.ExamScoreImportCommitReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamScoreImportBatchEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamScoreImportItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamScoreImportBatchMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamScoreImportItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.service.RensheScoreRosterParseService.ParsedRoster;
import com.guoyu.platform.trainingdepartment.service.RensheScoreRosterParseService.ParsedRow;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportBatchItemVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportBatchVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportClassVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportCommitResultVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportItemVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportPreviewVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportRosterGapVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportTargetVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExamScoreImportApplicationService {

  static final String MATCHED = "MATCHED";
  static final String MATCHED_NAME_WARN = "MATCHED_NAME_WARN";
  static final String MATCHED_JOB_WARN = "MATCHED_JOB_WARN";
  static final String MULTI_CLASS = "MULTI_CLASS";
  static final String UNMATCHED_NOT_IN_CLASS = "UNMATCHED_NOT_IN_CLASS";
  static final String NO_ID_CARD = "NO_ID_CARD";
  static final String EMPTY_SCORE = "EMPTY_SCORE";
  static final String SHEET_DUP_CONFLICT = "SHEET_DUP_CONFLICT";
  static final String SKIPPED = "SKIPPED";
  static final String WRITTEN = "WRITTEN";
  static final String OVERWRITTEN = "OVERWRITTEN";

  private static final Set<String> WRITABLE = Set.of(
      MATCHED, MATCHED_NAME_WARN, MATCHED_JOB_WARN, MULTI_CLASS);
  private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
  private static final long MAX_FILE_BYTES = 8L * 1024 * 1024;

  private final RensheScoreRosterParseService parseService;
  private final TrainPeopleListMapper peopleMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainExamScoreImportBatchMapper batchMapper;
  private final TrainExamScoreImportItemMapper itemMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final ObjectMapper objectMapper;
  private final ConcurrentHashMap<String, CachedPreview> previewCache = new ConcurrentHashMap<>();

  public ExamScoreImportApplicationService(
      RensheScoreRosterParseService parseService,
      TrainPeopleListMapper peopleMapper,
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper trainInfoListMapper,
      TrainExamScoreImportBatchMapper batchMapper,
      TrainExamScoreImportItemMapper itemMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainingOperatorScopeService operatorScopeService,
      ObjectMapper objectMapper) {
    this.parseService = parseService;
    this.peopleMapper = peopleMapper;
    this.classMapper = classMapper;
    this.trainInfoListMapper = trainInfoListMapper;
    this.batchMapper = batchMapper;
    this.itemMapper = itemMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.operatorScopeService = operatorScopeService;
    this.objectMapper = objectMapper;
  }

  public List<ExamScoreImportClassVO> listClasses(Long companyId, Long trainInfoId, String keyword) {
    Long companyFilter = operatorScopeService.resolveCompanyFilter(companyId);
    LambdaQueryWrapper<TrainInfoClassEntity> wrapper = new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getDeleted, 0)
        .orderByDesc(TrainInfoClassEntity::getAddtime)
        .last("LIMIT 300");
    if (companyFilter != null) {
      wrapper.eq(TrainInfoClassEntity::getCompanyId, companyFilter);
    }
    if (trainInfoId != null && trainInfoId != 0) {
      wrapper.eq(TrainInfoClassEntity::getTrainInfoId, trainInfoId);
    }
    if (StringUtils.hasText(keyword)) {
      wrapper.like(TrainInfoClassEntity::getTitle, keyword.trim());
    }
    List<TrainInfoClassEntity> classes = classMapper.selectList(wrapper);
    if (classes.isEmpty()) {
      return List.of();
    }
    Map<Long, ClassContext> ctx = loadClassContext(classes);
    return classes.stream().map(c -> {
      ClassContext cc = ctx.get(c.getId());
      return new ExamScoreImportClassVO(
          c.getId(),
          c.getTitle(),
          c.getCompanyId(),
          cc == null ? null : cc.companyName,
          c.getTrainInfoId(),
          cc == null ? null : cc.trainTitle,
          c.getWorkTypeId(),
          cc == null ? null : cc.workTypeTitle,
          c.getWorkTypeLevel());
    }).toList();
  }

  public ResponseEntity<byte[]> downloadTemplate() {
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("成绩导入");
      Row header = sheet.createRow(0);
      String[] cols = {"身份证号", "姓名", "综合", "理论", "技能"};
      for (int i = 0; i < cols.length; i++) {
        header.createCell(i).setCellValue(cols[i]);
        sheet.setColumnWidth(i, 18 * 256);
      }
      wb.write(out);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
              .filename("成绩导入标准模板.xlsx", StandardCharsets.UTF_8).build().toString())
          .body(out.toByteArray());
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成模板失败");
    }
  }

  public ExamScoreImportPreviewVO preview(List<Long> classIds, MultipartFile file) {
    List<Long> selected = normalizeClassIds(classIds);
    if (selected.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择班级后再上传成绩单");
    }
    byte[] bytes = readFile(file);
    ParsedRoster parsed = parseService.parse(bytes);
    List<TrainInfoClassEntity> classes = loadAccessibleClasses(selected);
    Map<Long, ClassContext> classCtx = loadClassContext(classes);
    List<TrainPeopleListEntity> roster = loadRoster(selected);
    Map<String, List<TrainPeopleListEntity>> byIdCard = indexRosterByIdCard(roster);

    Map<String, List<ParsedRow>> bySheetId = new LinkedHashMap<>();
    List<ParsedRow> noId = new ArrayList<>();
    for (ParsedRow row : parsed.rows()) {
      if (!StringUtils.hasText(row.idCard())) {
        noId.add(row);
        continue;
      }
      bySheetId.computeIfAbsent(row.idCard(), k -> new ArrayList<>()).add(row);
    }

    Set<String> sheetDupIds = new HashSet<>();
    List<ExamScoreImportItemVO> items = new ArrayList<>();
    Map<Long, CachedTarget> byPeopleId = new LinkedHashMap<>();

    for (ParsedRow row : noId) {
      items.add(toItem(row, NO_ID_CARD, resolveExamScore(row), false, List.of()));
    }

    for (Map.Entry<String, List<ParsedRow>> e : bySheetId.entrySet()) {
      List<ParsedRow> group = e.getValue();
      if (group.size() > 1 && scoresConflict(group)) {
        sheetDupIds.add(e.getKey());
        for (ParsedRow row : group) {
          items.add(toItem(row, SHEET_DUP_CONFLICT, resolveExamScore(row), theoryFallback(row), List.of()));
        }
        continue;
      }
      ParsedRow row = group.get(0);
      BigDecimal examScore = resolveExamScore(row);
      boolean fallback = theoryFallback(row);
      if (examScore == null && row.skill() == null) {
        items.add(toItem(row, EMPTY_SCORE, null, false, List.of()));
        continue;
      }
      List<TrainPeopleListEntity> hits = byIdCard.getOrDefault(row.idCard(), List.of());
      if (hits.isEmpty()) {
        items.add(toItem(row, UNMATCHED_NOT_IN_CLASS, examScore, fallback, List.of()));
        continue;
      }
      List<ExamScoreImportTargetVO> targets = new ArrayList<>();
      boolean anyNameMismatch = false;
      boolean anyJobMismatch = false;
      for (TrainPeopleListEntity person : hits) {
        ClassContext ctx = classCtx.get(person.getTrainClassId());
        boolean nameMismatch = nameMismatch(row.name(), person.getRealname());
        boolean jobMismatch = jobMismatch(ctx, row);
        anyNameMismatch |= nameMismatch;
        anyJobMismatch |= jobMismatch;
        targets.add(new ExamScoreImportTargetVO(
            person.getId(),
            person.getTrainClassId(),
            ctx == null ? null : ctx.title,
            person.getRealname(),
            person.getExamScore(),
            person.getRealScore(),
            nameMismatch,
            jobMismatch));
        byPeopleId.put(person.getId(), new CachedTarget(
            row.idCard(),
            person.getTrainClassId(),
            examScore,
            row.skill(),
            row.rowNo(),
            row.name(),
            row.ticketNo(),
            row.theory(),
            row.skill(),
            row.comprehensive(),
            fallback));
      }
      String status;
      if (hits.size() > 1) {
        status = MULTI_CLASS;
      } else if (anyNameMismatch) {
        status = MATCHED_NAME_WARN;
      } else if (anyJobMismatch) {
        status = MATCHED_JOB_WARN;
      } else {
        status = MATCHED;
      }
      items.add(toItem(row, status, examScore, fallback, targets));
    }

    Set<String> sheetIds = bySheetId.keySet();
    List<ExamScoreImportRosterGapVO> missingOnSheet = new ArrayList<>();
    List<ExamScoreImportRosterGapVO> missingIdCard = new ArrayList<>();
    for (TrainPeopleListEntity person : roster) {
      ClassContext ctx = classCtx.get(person.getTrainClassId());
      String cid = RensheScoreRosterParseService.normalizeIdCard(person.getIdCard());
      if (!StringUtils.hasText(cid)) {
        missingIdCard.add(new ExamScoreImportRosterGapVO(
            person.getId(),
            person.getTrainClassId(),
            ctx == null ? null : ctx.title,
            person.getRealname(),
            person.getIdCard()));
        continue;
      }
      if (!sheetIds.contains(cid) || sheetDupIds.contains(cid)) {
        if (!sheetIds.contains(cid)) {
          missingOnSheet.add(new ExamScoreImportRosterGapVO(
              person.getId(),
              person.getTrainClassId(),
              ctx == null ? null : ctx.title,
              person.getRealname(),
              cid));
        }
      }
    }

    int matched = 0;
    int nameWarn = 0;
    int jobWarn = 0;
    int unmatched = 0;
    int emptyScore = 0;
    int sheetDup = 0;
    for (ExamScoreImportItemVO item : items) {
      switch (item.status()) {
        case MATCHED -> matched++;
        case MATCHED_NAME_WARN -> {
          matched++;
          nameWarn++;
        }
        case MATCHED_JOB_WARN -> {
          matched++;
          jobWarn++;
        }
        case MULTI_CLASS -> matched++;
        case UNMATCHED_NOT_IN_CLASS, NO_ID_CARD -> unmatched++;
        case EMPTY_SCORE -> emptyScore++;
        case SHEET_DUP_CONFLICT -> sheetDup++;
        default -> {
        }
      }
    }

    String previewId = UUID.randomUUID().toString();
    ExamScoreImportPreviewVO vo = new ExamScoreImportPreviewVO(
        previewId,
        parsed.planName(),
        parsed.planCode(),
        parsed.examTimeText(),
        parsed.layout(),
        selected,
        parsed.rows().size(),
        matched,
        nameWarn,
        jobWarn,
        unmatched,
        emptyScore,
        sheetDup,
        missingOnSheet.size(),
        missingIdCard.size(),
        items,
        missingOnSheet,
        missingIdCard);

    evictExpired();
    previewCache.put(previewId, new CachedPreview(
        tenantId(),
        SecurityContext.currentUser().getUserId(),
        Instant.now().plus(PREVIEW_TTL),
        file.getOriginalFilename(),
        selected,
        parsed,
        vo,
        byPeopleId,
        items));
    return vo;
  }

  @Transactional
  public ExamScoreImportCommitResultVO commit(ExamScoreImportCommitReq req) {
    if (req == null || !StringUtils.hasText(req.getPreviewId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "预览已失效，请重新上传");
    }
    CachedPreview cached = previewCache.get(req.getPreviewId().trim());
    if (cached == null || cached.expiresAt.isBefore(Instant.now())) {
      previewCache.remove(req.getPreviewId());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "预览已失效，请重新上传");
    }
    if (!tenantId().equals(cached.tenantId) || !SecurityContext.currentUser().getUserId().equals(cached.userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权提交该预览");
    }
    Set<Long> selectedPeople = req.getPeopleIds() == null
        ? Set.of()
        : req.getPeopleIds().stream().filter(id -> id != null && id > 0).collect(Collectors.toSet());
    if (selectedPeople.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请勾选要写入的学员");
    }

    List<TrainInfoClassEntity> classes = loadAccessibleClasses(cached.classIds);
    Map<Long, TrainInfoClassEntity> classById = classes.stream()
        .collect(Collectors.toMap(TrainInfoClassEntity::getId, c -> c, (a, b) -> a));

    int written = 0;
    int overwritten = 0;
    int skipped = 0;
    List<TrainExamScoreImportItemEntity> auditItems = new ArrayList<>();

    for (Long peopleId : selectedPeople) {
      CachedTarget target = cached.byPeopleId.get(peopleId);
      if (target == null) {
        skipped++;
        continue;
      }
      TrainPeopleListEntity person = peopleMapper.selectById(peopleId);
      if (person == null || (person.getDeleted() != null && person.getDeleted() != 0)
          || !"student".equals(person.getType())
          || !classById.containsKey(person.getTrainClassId())
          || !person.getTrainClassId().equals(target.classId)) {
        skipped++;
        continue;
      }
      String dbId = RensheScoreRosterParseService.normalizeIdCard(person.getIdCard());
      if (!target.idCard.equals(dbId)
          || (person.getTenantId() != null && !tenantId().equals(person.getTenantId()))) {
        skipped++;
        continue;
      }
      BigDecimal oldExam = person.getExamScore();
      BigDecimal oldReal = person.getRealScore();
      BigDecimal newExam = target.examScore;
      BigDecimal newReal = target.realScore;
      boolean hadOld = oldExam != null || oldReal != null;
      boolean changed = (newExam != null && (oldExam == null || oldExam.compareTo(newExam) != 0))
          || (newReal != null && (oldReal == null || oldReal.compareTo(newReal) != 0));
      if (!changed) {
        skipped++;
        auditItems.add(buildItem(null, target, peopleId, person.getTrainClassId(), SKIPPED,
            oldExam, newExam, oldReal, newReal));
        continue;
      }
      if (newExam != null) {
        person.setExamScore(newExam);
      }
      if (newReal != null) {
        person.setRealScore(newReal);
      }
      peopleMapper.updateById(person);
      written++;
      if (hadOld) {
        overwritten++;
      }
      auditItems.add(buildItem(null, target, peopleId, person.getTrainClassId(),
          hadOld ? OVERWRITTEN : WRITTEN, oldExam, newExam, oldReal, newReal));
    }

    Set<Long> writtenOrSkipped = auditItems.stream()
        .map(TrainExamScoreImportItemEntity::getPeopleId)
        .filter(id -> id != null)
        .collect(Collectors.toSet());
    for (ExamScoreImportItemVO item : cached.items) {
      if (WRITABLE.contains(item.status())) {
        for (ExamScoreImportTargetVO t : item.targets()) {
          if (t.peopleId() != null && !selectedPeople.contains(t.peopleId())
              && !writtenOrSkipped.contains(t.peopleId())) {
            skipped++;
            CachedTarget ct = cached.byPeopleId.get(t.peopleId());
            if (ct != null) {
              auditItems.add(buildItem(null, ct, t.peopleId(), t.classId(), SKIPPED,
                  t.oldExamScore(), ct.examScore, t.oldRealScore(), ct.realScore));
            }
          }
        }
      } else {
        CachedTarget dummy = new CachedTarget(
            item.idCard(),
            null,
            item.examScore(),
            item.skillScore(),
            item.rowNo(),
            item.name(),
            item.ticketNo(),
            item.theoryScore(),
            item.skillScore(),
            item.examScore(),
            item.theoryFallback());
        auditItems.add(buildItem(null, dummy, null, null, item.status(),
            null, item.examScore(), null, item.skillScore()));
      }
    }

    int unmatched = 0;
    for (ExamScoreImportItemVO item : cached.items) {
      if (UNMATCHED_NOT_IN_CLASS.equals(item.status()) || NO_ID_CARD.equals(item.status())) {
        unmatched++;
      }
    }

    TrainExamScoreImportBatchEntity batch = new TrainExamScoreImportBatchEntity();
    batch.setTenantId(tenantId());
    batch.setOperatorUserId(SecurityContext.currentUser().getUserId());
    batch.setOperatorUsername(SecurityContext.currentUser().getUsername());
    batch.setFileName(cached.fileName);
    batch.setPlanName(cached.parsed.planName());
    batch.setPlanCode(cached.parsed.planCode());
    batch.setExamTimeText(cached.parsed.examTimeText());
    batch.setLayout(cached.parsed.layout());
    batch.setClassIdsJson(writeJson(cached.classIds));
    batch.setTrainInfoId(classes.isEmpty() ? null : classes.get(0).getTrainInfoId());
    batch.setSyncExamPlan(0);
    batch.setTotalRows(cached.vo.totalRows());
    batch.setMatchedCount(cached.vo.matched());
    batch.setWrittenCount(written);
    batch.setOverwrittenCount(overwritten);
    batch.setSkippedCount(skipped);
    batch.setUnmatchedCount(unmatched);
    batchMapper.insert(batch);

    for (TrainExamScoreImportItemEntity item : auditItems) {
      item.setBatchId(batch.getId());
      item.setTenantId(tenantId());
      itemMapper.insert(item);
    }

    previewCache.remove(req.getPreviewId());
    return new ExamScoreImportCommitResultVO(batch.getId(), written, overwritten, skipped, unmatched);
  }

  public PageResponse<ExamScoreImportBatchVO> pageBatches(long page, long pageSize) {
    Page<TrainExamScoreImportBatchEntity> mp = batchMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<TrainExamScoreImportBatchEntity>()
            .eq(TrainExamScoreImportBatchEntity::getTenantId, tenantId())
            .eq(TrainExamScoreImportBatchEntity::getDeleted, 0)
            .orderByDesc(TrainExamScoreImportBatchEntity::getCreatedAt));
    List<ExamScoreImportBatchVO> items = mp.getRecords().stream().map(this::toBatchVo).toList();
    return new PageResponse<>(items, page, pageSize, mp.getTotal());
  }

  public PageResponse<ExamScoreImportBatchItemVO> pageItems(Long batchId, long page, long pageSize) {
    requireBatch(batchId);
    Page<TrainExamScoreImportItemEntity> mp = itemMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<TrainExamScoreImportItemEntity>()
            .eq(TrainExamScoreImportItemEntity::getBatchId, batchId)
            .eq(TrainExamScoreImportItemEntity::getTenantId, tenantId())
            .eq(TrainExamScoreImportItemEntity::getDeleted, 0)
            .orderByAsc(TrainExamScoreImportItemEntity::getRowNo));
    List<ExamScoreImportBatchItemVO> items = mp.getRecords().stream().map(this::toItemVo).toList();
    return new PageResponse<>(items, page, pageSize, mp.getTotal());
  }

  public ResponseEntity<byte[]> exportFailures(Long batchId) {
    requireBatch(batchId);
    List<TrainExamScoreImportItemEntity> rows = itemMapper.selectList(
        new LambdaQueryWrapper<TrainExamScoreImportItemEntity>()
            .eq(TrainExamScoreImportItemEntity::getBatchId, batchId)
            .eq(TrainExamScoreImportItemEntity::getTenantId, tenantId())
            .eq(TrainExamScoreImportItemEntity::getDeleted, 0)
            .notIn(TrainExamScoreImportItemEntity::getResultCode, WRITTEN, OVERWRITTEN)
            .orderByAsc(TrainExamScoreImportItemEntity::getRowNo));
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("失败清单");
      Row header = sheet.createRow(0);
      String[] cols = {"行号", "证件号码", "姓名", "结果码", "综合", "理论", "技能"};
      for (int i = 0; i < cols.length; i++) {
        header.createCell(i).setCellValue(cols[i]);
      }
      int r = 1;
      for (TrainExamScoreImportItemEntity item : rows) {
        Row row = sheet.createRow(r++);
        if (item.getRowNo() != null) {
          row.createCell(0).setCellValue(item.getRowNo());
        }
        row.createCell(1).setCellValue(nullToEmpty(item.getIdCard()));
        row.createCell(2).setCellValue(nullToEmpty(item.getName()));
        row.createCell(3).setCellValue(nullToEmpty(item.getResultCode()));
        if (item.getComprehensiveScore() != null) {
          row.createCell(4).setCellValue(item.getComprehensiveScore().doubleValue());
        }
        if (item.getTheoryScore() != null) {
          row.createCell(5).setCellValue(item.getTheoryScore().doubleValue());
        }
        if (item.getSkillScore() != null) {
          row.createCell(6).setCellValue(item.getSkillScore().doubleValue());
        }
      }
      wb.write(out);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
              .filename("成绩导入失败清单.xlsx", StandardCharsets.UTF_8).build().toString())
          .body(out.toByteArray());
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败清单失败");
    }
  }

  private TrainExamScoreImportBatchEntity requireBatch(Long batchId) {
    TrainExamScoreImportBatchEntity batch = batchMapper.selectOne(
        new LambdaQueryWrapper<TrainExamScoreImportBatchEntity>()
            .eq(TrainExamScoreImportBatchEntity::getId, batchId)
            .eq(TrainExamScoreImportBatchEntity::getTenantId, tenantId())
            .eq(TrainExamScoreImportBatchEntity::getDeleted, 0));
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "导入批次不存在");
    }
    return batch;
  }

  private ExamScoreImportBatchVO toBatchVo(TrainExamScoreImportBatchEntity e) {
    return new ExamScoreImportBatchVO(
        e.getId(),
        e.getFileName(),
        e.getPlanName(),
        e.getPlanCode(),
        e.getExamTimeText(),
        e.getLayout(),
        readClassIds(e.getClassIdsJson()),
        n(e.getTotalRows()),
        n(e.getMatchedCount()),
        n(e.getWrittenCount()),
        n(e.getOverwrittenCount()),
        n(e.getSkippedCount()),
        n(e.getUnmatchedCount()),
        e.getOperatorUsername(),
        e.getCreatedAt());
  }

  private ExamScoreImportBatchItemVO toItemVo(TrainExamScoreImportItemEntity e) {
    return new ExamScoreImportBatchItemVO(
        e.getId(),
        e.getRowNo(),
        e.getIdCard(),
        e.getName(),
        e.getTicketNo(),
        e.getTheoryScore(),
        e.getSkillScore(),
        e.getComprehensiveScore(),
        e.getPeopleId(),
        e.getClassId(),
        e.getResultCode(),
        e.getOldExamScore(),
        e.getNewExamScore(),
        e.getOldRealScore(),
        e.getNewRealScore(),
        e.getTheoryFallback() != null && e.getTheoryFallback() == 1);
  }

  private TrainExamScoreImportItemEntity buildItem(
      Long batchId,
      CachedTarget target,
      Long peopleId,
      Long classId,
      String resultCode,
      BigDecimal oldExam,
      BigDecimal newExam,
      BigDecimal oldReal,
      BigDecimal newReal) {
    TrainExamScoreImportItemEntity item = new TrainExamScoreImportItemEntity();
    item.setBatchId(batchId);
    item.setRowNo(target.rowNo);
    item.setIdCard(target.idCard);
    item.setName(target.name);
    item.setTicketNo(target.ticketNo);
    item.setTheoryScore(target.theory);
    item.setSkillScore(target.skill);
    item.setComprehensiveScore(target.comprehensive);
    item.setPeopleId(peopleId);
    item.setClassId(classId);
    item.setResultCode(resultCode);
    item.setOldExamScore(oldExam);
    item.setNewExamScore(newExam);
    item.setOldRealScore(oldReal);
    item.setNewRealScore(newReal);
    item.setTheoryFallback(target.theoryFallback ? 1 : 0);
    return item;
  }

  private ExamScoreImportItemVO toItem(
      ParsedRow row,
      String status,
      BigDecimal examScore,
      boolean theoryFallback,
      List<ExamScoreImportTargetVO> targets) {
    return new ExamScoreImportItemVO(
        row.rowNo(),
        row.idCard() != null ? row.idCard() : row.idCardRaw(),
        row.name(),
        row.ticketNo(),
        row.jobName(),
        row.levelName(),
        row.theory(),
        row.skill(),
        examScore,
        theoryFallback,
        status,
        targets);
  }

  private List<TrainInfoClassEntity> loadAccessibleClasses(List<Long> classIds) {
    List<TrainInfoClassEntity> classes = classMapper.selectList(
        new LambdaQueryWrapper<TrainInfoClassEntity>()
            .in(TrainInfoClassEntity::getId, classIds)
            .eq(TrainInfoClassEntity::getDeleted, 0));
    if (classes.size() != classIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "部分班级不存在或已删除");
    }
    for (TrainInfoClassEntity clazz : classes) {
      operatorScopeService.assertClassAccessible(clazz);
    }
    return classes;
  }

  private Map<Long, ClassContext> loadClassContext(List<TrainInfoClassEntity> classes) {
    Map<Long, ClassContext> map = new HashMap<>();
    if (classes.isEmpty()) {
      return map;
    }
    List<Long> companyIds = classes.stream().map(TrainInfoClassEntity::getCompanyId)
        .filter(id -> id != null).distinct().toList();
    List<Long> workTypeIds = classes.stream().map(TrainInfoClassEntity::getWorkTypeId)
        .filter(id -> id != null).distinct().toList();
    List<Long> trainInfoIds = classes.stream().map(TrainInfoClassEntity::getTrainInfoId)
        .filter(id -> id != null).distinct().toList();
    Map<Long, String> companyNames = companyIds.isEmpty() ? Map.of()
        : orgMapper.selectBatchIds(companyIds).stream()
            .collect(Collectors.toMap(o -> o.getId(), OrgEntity::getName, (a, b) -> a));
    Map<Long, String> workTypeNames = workTypeIds.isEmpty() ? Map.of()
        : examWorkTypeMapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
                .in(ExamWorkTypeEntity::getId, workTypeIds)
                .eq(ExamWorkTypeEntity::getDeleted, 0))
            .stream()
            .collect(Collectors.toMap(e -> e.getId().longValue(), ExamWorkTypeEntity::getTitle, (a, b) -> a));
    Map<Long, TrainInfoListEntity> trainInfos = trainInfoIds.isEmpty() ? Map.of()
        : trainInfoListMapper.selectBatchIds(trainInfoIds).stream()
            .collect(Collectors.toMap(TrainInfoListEntity::getId, e -> e, (a, b) -> a));
    for (TrainInfoClassEntity c : classes) {
      TrainInfoListEntity ti = c.getTrainInfoId() == null ? null : trainInfos.get(c.getTrainInfoId());
      String companyName = c.getCompanyId() == null ? null : companyNames.get(c.getCompanyId());
      if (!StringUtils.hasText(companyName) && ti != null) {
        companyName = ti.getCompanyName();
      }
      map.put(c.getId(), new ClassContext(
          c.getTitle(),
          companyName,
          ti == null ? null : ti.getTitle(),
          c.getWorkTypeId() == null ? null : workTypeNames.get(c.getWorkTypeId()),
          c.getWorkTypeLevel()));
    }
    return map;
  }

  private List<TrainPeopleListEntity> loadRoster(List<Long> classIds) {
    return peopleMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .in(TrainPeopleListEntity::getTrainClassId, classIds)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
  }

  private Map<String, List<TrainPeopleListEntity>> indexRosterByIdCard(List<TrainPeopleListEntity> roster) {
    Map<String, List<TrainPeopleListEntity>> map = new LinkedHashMap<>();
    for (TrainPeopleListEntity person : roster) {
      String id = RensheScoreRosterParseService.normalizeIdCard(person.getIdCard());
      if (!StringUtils.hasText(id)) {
        continue;
      }
      map.computeIfAbsent(id, k -> new ArrayList<>()).add(person);
    }
    return map;
  }

  private byte[] readFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传成绩单文件");
    }
    String name = file.getOriginalFilename();
    if (!StringUtils.hasText(name)
        || (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")
            && !name.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能上传 xlsx/xls 文件");
    }
    if (file.getSize() > MAX_FILE_BYTES) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件过大，请压缩后重试");
    }
    try {
      return file.getBytes();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取成绩单失败");
    }
  }

  static List<Long> normalizeClassIds(List<Long> classIds) {
    if (classIds == null) {
      return List.of();
    }
    return classIds.stream().filter(id -> id != null && id > 0).distinct().toList();
  }

  static BigDecimal resolveExamScore(ParsedRow row) {
    if (row.comprehensive() != null) {
      return row.comprehensive();
    }
    return row.theory();
  }

  static boolean theoryFallback(ParsedRow row) {
    return row.comprehensive() == null && row.theory() != null;
  }

  static boolean scoresConflict(List<ParsedRow> group) {
    ParsedRow first = group.get(0);
    for (int i = 1; i < group.size(); i++) {
      ParsedRow other = group.get(i);
      if (!scoreEq(first.theory(), other.theory())
          || !scoreEq(first.skill(), other.skill())
          || !scoreEq(first.comprehensive(), other.comprehensive())) {
        return true;
      }
    }
    return false;
  }

  static boolean scoreEq(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return a.compareTo(b) == 0;
  }

  static boolean nameMismatch(String sheetName, String rosterName) {
    String a = normalizeName(sheetName);
    String b = normalizeName(rosterName);
    if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
      return false;
    }
    return !a.equals(b);
  }

  static boolean jobMismatch(ClassContext ctx, ParsedRow row) {
    if (ctx == null) {
      return false;
    }
    boolean job = false;
    if (StringUtils.hasText(ctx.workTypeTitle) && StringUtils.hasText(row.jobName())) {
      String a = normalizeName(ctx.workTypeTitle);
      String b = normalizeName(row.jobName());
      job = !a.contains(b) && !b.contains(a);
    }
    boolean level = false;
    Integer parsed = parseLevel(row.levelName());
    if (ctx.workTypeLevel != null && parsed != null && parsed > 0) {
      level = !ctx.workTypeLevel.equals(parsed);
    }
    return job || level;
  }

  static Integer parseLevel(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String s = raw.replace(" ", "");
    if (s.contains("高级技师") || s.contains("一级")) {
      return 5;
    }
    if (s.contains("技师") || s.contains("二级")) {
      return 4;
    }
    if (s.contains("高级工") || s.contains("三级")) {
      return 3;
    }
    if (s.contains("中级") || s.contains("四级")) {
      return 2;
    }
    if (s.contains("初级") || s.contains("五级")) {
      return 1;
    }
    return null;
  }

  static String normalizeName(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw.replace(" ", "").replace("\u3000", "").trim();
  }

  private void evictExpired() {
    Instant now = Instant.now();
    previewCache.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
  }

  private Long tenantId() {
    Long t = SecurityContext.currentUser().getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private String writeJson(List<Long> ids) {
    try {
      return objectMapper.writeValueAsString(ids);
    } catch (Exception ex) {
      return "[]";
    }
  }

  private List<Long> readClassIds(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private static int n(Integer v) {
    return v == null ? 0 : v;
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v;
  }

  record ClassContext(
      String title,
      String companyName,
      String trainTitle,
      String workTypeTitle,
      Integer workTypeLevel) {}

  private static final class CachedPreview {
    final Long tenantId;
    final Long userId;
    final Instant expiresAt;
    final String fileName;
    final List<Long> classIds;
    final ParsedRoster parsed;
    final ExamScoreImportPreviewVO vo;
    final Map<Long, CachedTarget> byPeopleId;
    final List<ExamScoreImportItemVO> items;

    CachedPreview(
        Long tenantId,
        Long userId,
        Instant expiresAt,
        String fileName,
        List<Long> classIds,
        ParsedRoster parsed,
        ExamScoreImportPreviewVO vo,
        Map<Long, CachedTarget> byPeopleId,
        List<ExamScoreImportItemVO> items) {
      this.tenantId = tenantId;
      this.userId = userId;
      this.expiresAt = expiresAt;
      this.fileName = fileName;
      this.classIds = classIds;
      this.parsed = parsed;
      this.vo = vo;
      this.byPeopleId = byPeopleId;
      this.items = items;
    }
  }

  private record CachedTarget(
      String idCard,
      Long classId,
      BigDecimal examScore,
      BigDecimal realScore,
      int rowNo,
      String name,
      String ticketNo,
      BigDecimal theory,
      BigDecimal skill,
      BigDecimal comprehensive,
      boolean theoryFallback) {}
}
