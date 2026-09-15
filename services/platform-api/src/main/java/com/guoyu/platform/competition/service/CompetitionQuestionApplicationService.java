package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.dto.CompetitionPracticalQuestionDetailReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreItemReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionCreateReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionQueryReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionImportBatchEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoreItemVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionImportBatchVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CompetitionQuestionApplicationService {

  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;
  private final CompetitionQuestionImportBatchMapper batchMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionQuestionWorkbookParser parser;
  private final CompetitionQuestionImportPersistenceService persistenceService;
  private final ThreadPoolTaskExecutor importExecutor;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public CompetitionQuestionApplicationService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionQuestionImportBatchMapper batchMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionQuestionImportPersistenceService persistenceService,
      @Qualifier("competitionQuestionImportExecutor") ThreadPoolTaskExecutor importExecutor) {
    this.questionMapper = questionMapper;
    this.practicalDetailMapper = practicalDetailMapper;
    this.scoreItemMapper = scoreItemMapper;
    this.batchMapper = batchMapper;
    this.eventMapper = eventMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.parser = new CompetitionQuestionWorkbookParser();
    this.persistenceService = persistenceService;
    this.importExecutor = importExecutor;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionQuestionVO> page(Long eventId, CompetitionQuestionQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    Integer status = req.getStatus();
    validateScope(principal.getTenantId(), eventId, req.getEventWorkTypeId(), req.getEventWorkTypeLevelId());
    LambdaQueryWrapper<CompetitionQuestionEntity> wrapper = new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(req.getEventWorkTypeId() != null, CompetitionQuestionEntity::getEventWorkTypeId, req.getEventWorkTypeId())
        .eq(req.getEventWorkTypeLevelId() != null, CompetitionQuestionEntity::getEventWorkTypeLevelId, req.getEventWorkTypeLevelId())
        .eq(StringUtils.hasText(req.getContentType()), CompetitionQuestionEntity::getContentType, req.getContentType())
        .eq(StringUtils.hasText(req.getQuestionType()), CompetitionQuestionEntity::getQuestionType, req.getQuestionType())
        .eq(status != null, CompetitionQuestionEntity::getStatus, Integer.valueOf(1).equals(status) ? "enabled" : "disabled")
        .like(StringUtils.hasText(req.getKeyword()), CompetitionQuestionEntity::getStem, req.getKeyword())
        .orderByAsc(CompetitionQuestionEntity::getSortOrder)
        .orderByAsc(CompetitionQuestionEntity::getId);
    Page<CompetitionQuestionEntity> result = questionMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (result == null) {
      log.warn("Competition question mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      result = new Page<>(req.getPage(), req.getPageSize());
    }
    List<CompetitionQuestionVO> items = new ArrayList<>();
    if (result.getRecords() != null) {
      for (CompetitionQuestionEntity entity : result.getRecords()) {
        items.add(toVO(principal.getTenantId(), entity, false));
      }
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionVO get(Long eventId, Long questionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    return toVO(principal.getTenantId(), entity, true);
  }

  @Transactional
  public CompetitionQuestionVO create(Long eventId, CompetitionQuestionCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    lockEvent(principal.getTenantId(), eventId);
    String contentType = normalize(req.contentType());
    String questionType = normalize(req.questionType());
    Integer sortOrder = nextSortOrder(principal.getTenantId(), eventId, req.eventWorkTypeId(),
        req.eventWorkTypeLevelId(), contentType, questionType);
    validateQuestion(principal.getTenantId(), eventId, req.eventWorkTypeId(), req.eventWorkTypeLevelId(),
        contentType, questionType, sortOrder, req.practicalDetail(),
        req.scoreItems(), req.referenceAnswer(), req.fillAnswer(), req.optionsJson());
    CompetitionQuestionEntity entity = new CompetitionQuestionEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(principal.getTenantId());
    entity.setEventId(eventId);
    entity.setEventWorkTypeId(req.eventWorkTypeId());
    entity.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    entity.setContentType(contentType);
    entity.setQuestionType(questionType);
    entity.setSortOrder(sortOrder);
    entity.setStem(req.stem());
    entity.setReferenceAnswer(req.referenceAnswer());
    entity.setFillAnswer(req.fillAnswer());
    entity.setOptionsJson(req.optionsJson());
    entity.setDifficulty(req.difficulty());
    entity.setStatus("disabled".equalsIgnoreCase(req.status()) ? "disabled" : "enabled");
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    entity.setDeleted(0);
    if (questionMapper.insert(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题保存失败");
    }
    savePracticalChildren(principal.getTenantId(), principal.getUserId(), entity.getId(), req.practicalDetail(), req.scoreItems());
    return toVO(principal.getTenantId(), entity, true);
  }

  @Transactional
  public CompetitionQuestionVO update(Long eventId, Long questionId, CompetitionQuestionUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    String oldContentType = entity.getContentType();
    Long workTypeId = req.eventWorkTypeId() != null ? req.eventWorkTypeId() : entity.getEventWorkTypeId();
    Long levelId = req.eventWorkTypeLevelId() != null ? req.eventWorkTypeLevelId() : entity.getEventWorkTypeLevelId();
    String contentType = StringUtils.hasText(req.contentType()) ? req.contentType() : entity.getContentType();
    String questionType = StringUtils.hasText(req.questionType()) ? req.questionType() : entity.getQuestionType();
    Integer sequence = entity.getSortOrder();
    String referenceAnswer = req.referenceAnswer() != null
        ? req.referenceAnswer() : entity.getReferenceAnswer();
    String fillAnswer = req.fillAnswer() != null ? req.fillAnswer() : entity.getFillAnswer();
    String optionsJson = req.optionsJson() != null ? req.optionsJson() : entity.getOptionsJson();
    validateQuestion(principal.getTenantId(), eventId, workTypeId, levelId, contentType, questionType, sequence,
        req.practicalDetail(), req.scoreItems(), entity.getContentType().equals(contentType),
        referenceAnswer, fillAnswer, optionsJson);
    ensureSequenceAvailable(principal.getTenantId(), eventId, workTypeId, levelId, contentType, questionType, sequence, questionId);
    entity.setEventWorkTypeId(workTypeId);
    entity.setEventWorkTypeLevelId(levelId);
    entity.setContentType(contentType.toLowerCase(Locale.ROOT));
    entity.setQuestionType(questionType.toLowerCase(Locale.ROOT));
    entity.setSortOrder(sequence);
    if (req.stem() != null) entity.setStem(req.stem());
    if (req.referenceAnswer() != null) entity.setReferenceAnswer(req.referenceAnswer());
    if (req.fillAnswer() != null) entity.setFillAnswer(req.fillAnswer());
    if (req.optionsJson() != null) entity.setOptionsJson(req.optionsJson());
    if (req.difficulty() != null) entity.setDifficulty(req.difficulty());
    if (req.status() != null) entity.setStatus("disabled".equalsIgnoreCase(req.status()) ? "disabled" : "enabled");
    entity.setUpdatedBy(principal.getUserId());
    if (questionMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题已发生变化，请刷新后重试");
    }
    if ("practical".equalsIgnoreCase(contentType)) {
      replacePracticalChildren(principal.getTenantId(), principal.getUserId(), entity.getId(), req.practicalDetail(), req.scoreItems());
    } else if ("practical".equalsIgnoreCase(oldContentType)) {
      deletePracticalChildren(principal.getTenantId(), entity.getId());
    }
    return toVO(principal.getTenantId(), entity, true);
  }

  @Transactional
  public void delete(Long eventId, Long questionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    if (questionMapper.deleteById(entity.getId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题删除失败");
    }
    deletePracticalChildren(principal.getTenantId(), questionId);
  }

  @Transactional
  public CompetitionQuestionImportBatchVO importFile(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType, MultipartFile file) {
    PlatformUserPrincipal principal = currentUser();
    validateScope(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId);
    if (!"theory".equalsIgnoreCase(contentType) && !"practical".equalsIgnoreCase(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的 Excel 文件");
    }
    Path tempFile;
    try {
      String suffix = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xls")
          ? ".xls" : ".xlsx";
      tempFile = Files.createTempFile("competition-question-", suffix);
      file.transferTo(tempFile);
    } catch (IOException ex) {
      log.error("Failed to stage competition question import file", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入文件暂存失败");
    }
    CompetitionQuestionImportBatchEntity batch = new CompetitionQuestionImportBatchEntity();
    batch.setId(IdWorker.getId());
    batch.setTenantId(principal.getTenantId());
    batch.setEventId(eventId);
    batch.setEventWorkTypeId(eventWorkTypeId);
    batch.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    batch.setContentType(contentType.toLowerCase(Locale.ROOT));
    batch.setOriginalFilename(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "题库导入.xlsx");
    batch.setTempFilePath(tempFile.toString());
    batch.setStatus("pending");
    batch.setTotalCount(0);
    batch.setSuccessCount(0);
    batch.setFailureCount(0);
    batch.setCreatedBy(principal.getUserId());
    batch.setUpdatedBy(principal.getUserId());
    batch.setDeleted(0);
    if (batchMapper.insert(batch) != 1) {
      deleteQuietly(tempFile);
      throw new BusinessException(ErrorCode.CONFLICT, "导入批次创建失败");
    }
    try {
      Long tenantId = principal.getTenantId();
      Long userId = principal.getUserId();
      importExecutor.execute(() -> processImport(batch.getId(), tenantId, userId));
    } catch (RejectedExecutionException ex) {
      batch.setStatus("failed");
      batch.setFailureCount(1);
      batch.setErrorDetail("[\"导入任务繁忙，请稍后重试\"]");
      batch.setFinishedAt(LocalDateTime.now());
      batch.setTempFilePath(null);
      batchMapper.updateById(batch);
      deleteQuietly(tempFile);
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "导入任务繁忙，请稍后重试");
    }
    return toBatchVO(batch);
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionImportBatchVO getImportBatch(Long eventId, Long batchId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionImportBatchEntity batch = batchMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionImportBatchEntity>()
        .eq(CompetitionQuestionImportBatchEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionQuestionImportBatchEntity::getEventId, eventId)
        .eq(CompetitionQuestionImportBatchEntity::getId, batchId)
        .eq(CompetitionQuestionImportBatchEntity::getDeleted, 0));
    if (batch == null) throw new BusinessException(ErrorCode.NOT_FOUND, "导入批次不存在或不属于当前赛事");
    return toBatchVO(batch);
  }

  public byte[] template(String contentType) {
    if (!"theory".equalsIgnoreCase(contentType) && !"practical".equalsIgnoreCase(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
    }
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("竞赛试题");
      String[] headers = "theory".equalsIgnoreCase(contentType)
          ? new String[] {"序号", "题型", "题干", "参考答案", "回答", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "选项G", "选项H", "难度"}
          : new String[] {"序号", "题型", "题干", "操作内容", "考生准备", "设备准备", "评分项序号", "评分要素", "评分标准", "最大分值", "最大扣分", "扣分步长"};
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      Row example = sheet.createRow(1);
      if ("theory".equalsIgnoreCase(contentType)) {
        example.createCell(0).setCellValue(1);
        example.createCell(1).setCellValue("单选题");
        example.createCell(2).setCellValue("示例题干");
        example.createCell(3).setCellValue("A");
        example.createCell(5).setCellValue("选项A");
        example.createCell(6).setCellValue("选项B");
      } else {
        example.createCell(0).setCellValue(1);
        example.createCell(1).setCellValue("实操题");
        example.createCell(2).setCellValue("示例实操题");
        example.createCell(6).setCellValue(1);
        example.createCell(7).setCellValue("评分要素");
        example.createCell(9).setCellValue(100);
        example.createCell(10).setCellValue(100);
        example.createCell(11).setCellValue(1);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板生成失败");
    }
  }

  private void processImport(Long batchId, Long tenantId, Long userId) {
    TenantContextHolder.setTenantId(tenantId);
    CompetitionQuestionImportBatchEntity batch = null;
    try {
      batch = batchMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionImportBatchEntity>()
          .eq(CompetitionQuestionImportBatchEntity::getTenantId, tenantId)
          .eq(CompetitionQuestionImportBatchEntity::getId, batchId)
          .eq(CompetitionQuestionImportBatchEntity::getDeleted, 0));
      if (batch == null) return;
      batch.setStatus("processing");
      batch.setStartedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      batchMapper.updateById(batch);
      CompetitionQuestionWorkbookParser.ParsedWorkbook parsed = parser.parse(Path.of(batch.getTempFilePath()), batch.getContentType());
      batch.setTotalCount(parsed.questions().size());
      validateImportConflicts(tenantId, batch, parsed.questions());
      persistenceService.persist(tenantId, userId, batch.getEventId(), batch.getEventWorkTypeId(),
          batch.getEventWorkTypeLevelId(), batch.getContentType(), batch.getId(), parsed.questions());
      batch.setStatus("succeeded");
      batch.setSuccessCount(parsed.questions().size());
      batch.setFailureCount(0);
      batch.setFinishedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      batchMapper.updateById(batch);
      log.info("Competition question import succeeded, batchId={}, count={}, tenantId={}", batchId, parsed.questions().size(), tenantId);
    } catch (Exception ex) {
      log.error("Competition question import failed, batchId={}, tenantId={}", batchId, tenantId, ex);
      if (batch == null) {
        batch = new CompetitionQuestionImportBatchEntity();
        batch.setId(batchId);
        batch.setTenantId(tenantId);
      }
      batch.setStatus("failed");
      batch.setFailureCount(batch.getTotalCount() == null || batch.getTotalCount() == 0 ? 1 : batch.getTotalCount());
      batch.setErrorDetail("[\"" + jsonEscape(ex.getMessage() == null ? "导入失败" : ex.getMessage()) + "\"]");
      batch.setFinishedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      if (batch.getId() != null) batchMapper.updateById(batch);
    } finally {
      if (batch != null && StringUtils.hasText(batch.getTempFilePath())) {
        deleteQuietly(Path.of(batch.getTempFilePath()));
        batch.setTempFilePath(null);
        batch.setUpdatedBy(userId);
        batchMapper.updateById(batch);
      }
      TenantContextHolder.clear();
    }
  }

  private void validateImportConflicts(Long tenantId, CompetitionQuestionImportBatchEntity batch,
      List<CompetitionQuestionWorkbookParser.ParsedQuestion> questions) {
    Set<String> seen = new HashSet<>();
    for (CompetitionQuestionWorkbookParser.ParsedQuestion question : questions) {
      String key = question.questionType() + ":" + question.sortOrder();
      if (!seen.add(key)) throw new BusinessException(ErrorCode.CONFLICT, "文件内存在重复范围+题型+序号: " + key);
      validateParsedQuestion(question, batch.getContentType());
    }
    List<CompetitionQuestionEntity> existing = questionMapper.selectList(new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, batch.getEventId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .eq(CompetitionQuestionEntity::getContentType, batch.getContentType())
        .eq(CompetitionQuestionEntity::getDeleted, 0));
    if (existing != null) {
      for (CompetitionQuestionEntity row : existing) {
        String key = row.getQuestionType() + ":" + row.getSortOrder();
        if (seen.contains(key)) throw new BusinessException(ErrorCode.CONFLICT, "数据库中已存在范围+题型+序号: " + key);
      }
    }
  }

  private void validateParsedQuestion(CompetitionQuestionWorkbookParser.ParsedQuestion question, String contentType) {
    if (question.sortOrder() == null || question.sortOrder() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目序号必须为正数");
    }
    if ("practical".equals(contentType)) {
      if (question.scoreItems().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题至少需要一个评分项（序号" + question.sortOrder() + "）");
      }
      Set<Integer> itemNos = new HashSet<>();
      for (CompetitionQuestionWorkbookParser.ParsedScoreItem item : question.scoreItems()) {
        if (item.itemNo() == null || item.itemNo() <= 0 || !itemNos.add(item.itemNo())
            || item.maxScore() == null || item.maxScore().signum() <= 0
            || item.maxDeduction() == null || item.maxDeduction().signum() < 0
            || item.deductionStep() == null || item.maxDeduction().compareTo(item.maxScore()) > 0
            || item.deductionStep().signum() <= 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题评分项编号、分值、扣分上限或扣分步长无效（序号" + question.sortOrder() + "）");
        }
      }
    }
  }

  private void validateQuestion(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> scoreItems,
      String referenceAnswer, String fillAnswer, String optionsJson) {
    validateQuestion(tenantId, eventId, workTypeId, levelId, contentType, questionType, sortOrder,
        detail, scoreItems, true, referenceAnswer, fillAnswer, optionsJson);
  }

  private void validateQuestion(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> scoreItems,
      boolean sameContentType, String referenceAnswer, String fillAnswer, String optionsJson) {
    validateScope(tenantId, eventId, workTypeId, levelId);
    if (!StringUtils.hasText(contentType) || !StringUtils.hasText(questionType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目类型不能为空");
    }
    String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
    String normalizedQuestionType = questionType.toLowerCase(Locale.ROOT);
    if (("theory".equals(normalizedContentType) && !Set.of("single", "multi", "judge", "fill").contains(normalizedQuestionType))
        || ("practical".equals(normalizedContentType) && !"practical".equals(normalizedQuestionType))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题型与理论/实操类型不匹配");
    }
    if (sortOrder == null || sortOrder <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "序号必须为正数");
    }
    if ("practical".equals(normalizedContentType)) {
      if (scoreItems == null || scoreItems.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题至少需要一个评分项");
      Set<Integer> numbers = new HashSet<>();
      for (CompetitionPracticalScoreItemReq item : scoreItems) {
        if (item == null || item.itemNo() == null || item.itemNo() <= 0
            || item.maxScore() == null || item.maxScore().signum() <= 0
            || item.maxDeduction() == null || item.maxDeduction().signum() < 0
            || item.deductionStep() == null || item.deductionStep().signum() <= 0
            || !numbers.add(item.itemNo()) || item.maxDeduction().compareTo(item.maxScore()) > 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "评分项编号不能重复，扣分上限不能超过项目分值");
        }
      }
    } else {
      validateTheoryAnswer(normalizedQuestionType, referenceAnswer, fillAnswer, optionsJson);
      if (detail != null || (scoreItems != null && !scoreItems.isEmpty())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题不能提交实操详情或评分项");
      }
    }
  }

  private void validateTheoryAnswer(
      String questionType, String referenceAnswer, String fillAnswer, String optionsJson) {
    if ("fill".equals(questionType)) {
      if (!StringUtils.hasText(fillAnswer) && !StringUtils.hasText(referenceAnswer)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "填空题必须填写回答或参考答案");
      }
      return;
    }
    if (!StringUtils.hasText(referenceAnswer)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选择题和判断题必须填写参考答案");
    }
    if ("judge".equals(questionType)) {
      String answer = referenceAnswer.trim().toUpperCase(Locale.ROOT);
      if (!Set.of("T", "F").contains(answer)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "判断题参考答案只能是 T 或 F");
      }
      return;
    }
    JsonNode options;
    try {
      options = objectMapper.readTree(optionsJson);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题选项必须是有效JSON对象");
    }
    if (options == null || !options.isObject() || options.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题必须配置选项A-H");
    }
    Set<String> available = new HashSet<>();
    options.fieldNames().forEachRemaining(key -> available.add(key.toUpperCase(Locale.ROOT)));
    String letters = referenceAnswer.toUpperCase(Locale.ROOT).replaceAll("[^A-H]", "");
    if (letters.isBlank() || ("single".equals(questionType) && letters.length() != 1)
        || ("multi".equals(questionType) && letters.length() < 2)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "答案必须是有效的A-H选项");
    }
    Set<String> selected = new HashSet<>();
    for (int index = 0; index < letters.length(); index++) {
      String key = String.valueOf(letters.charAt(index));
      if (!available.contains(key) || !selected.add(key)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "参考答案选项不存在或重复");
      }
    }
  }

  private void ensureSequenceAvailable(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder, Long exceptId) {
    LambdaQueryWrapper<CompetitionQuestionEntity> wrapper = new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(CompetitionQuestionEntity::getEventWorkTypeId, workTypeId)
        .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, levelId)
        .eq(CompetitionQuestionEntity::getContentType, contentType.toLowerCase(Locale.ROOT))
        .eq(CompetitionQuestionEntity::getQuestionType, questionType.toLowerCase(Locale.ROOT))
        .eq(CompetitionQuestionEntity::getSortOrder, sortOrder)
        .eq(CompetitionQuestionEntity::getDeleted, 0)
        .ne(exceptId != null, CompetitionQuestionEntity::getId, exceptId);
    if (questionMapper.selectCount(wrapper) > 0) throw new BusinessException(ErrorCode.CONFLICT, "同范围+题型+序号的竞赛试题已存在");
  }

  private Integer nextSortOrder(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType) {
    if (!StringUtils.hasText(contentType) || !StringUtils.hasText(questionType)) return 1;
    // A question number identifies its position in the complete scope, not a position within
    // each individual question type.  This keeps single, multi, judge and fill creation order
    // contiguous (1, 2, 3, ...).
    Integer currentMax = questionMapper.selectMaxSortOrder(
        tenantId, eventId, workTypeId, levelId, contentType, questionType);
    if (currentMax == null) return 1;
    if (currentMax >= Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题序号已达到最大值，无法继续新增");
    }
    return Math.max(currentMax, 0) + 1;
  }

  private void savePracticalChildren(Long tenantId, Long userId, Long questionId,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> scoreItems) {
    if (detail == null && (scoreItems == null || scoreItems.isEmpty())) return;
    CompetitionPracticalQuestionDetailEntity detailEntity = new CompetitionPracticalQuestionDetailEntity();
    detailEntity.setId(IdWorker.getId());
    detailEntity.setTenantId(tenantId);
    detailEntity.setQuestionId(questionId);
    if (detail != null) {
      detailEntity.setOperationContent(detail.operationContent());
      detailEntity.setStudentPrepare(detail.studentPrepare());
      detailEntity.setEquipmentPrepare(detail.equipmentPrepare());
    }
    detailEntity.setCreatedBy(userId);
    detailEntity.setUpdatedBy(userId);
    detailEntity.setDeleted(0);
    if (practicalDetailMapper.insert(detailEntity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题详情保存失败");
    }
    if (scoreItems != null) {
      for (CompetitionPracticalScoreItemReq item : scoreItems) {
        CompetitionPracticalQuestionScoreItemEntity entity = new CompetitionPracticalQuestionScoreItemEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(tenantId);
        entity.setQuestionId(questionId);
        entity.setItemNo(item.itemNo());
        entity.setItemName(item.itemName());
        entity.setCriterion(item.criterion());
        entity.setMaxScore(item.maxScore());
        entity.setMaxDeduction(item.maxDeduction());
        entity.setDeductionStep(item.deductionStep());
        entity.setStatus("enabled");
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setDeleted(0);
        if (scoreItemMapper.insert(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "实操评分项保存失败");
        }
      }
    }
  }

  private void replacePracticalChildren(Long tenantId, Long userId, Long questionId,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> scoreItems) {
    deletePracticalChildren(tenantId, questionId);
    savePracticalChildren(tenantId, userId, questionId, detail, scoreItems);
  }

  private void deletePracticalChildren(Long tenantId, Long questionId) {
    practicalDetailMapper.delete(new LambdaQueryWrapper<CompetitionPracticalQuestionDetailEntity>()
        .eq(CompetitionPracticalQuestionDetailEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalQuestionDetailEntity::getQuestionId, questionId));
    scoreItemMapper.delete(new LambdaQueryWrapper<CompetitionPracticalQuestionScoreItemEntity>()
        .eq(CompetitionPracticalQuestionScoreItemEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalQuestionScoreItemEntity::getQuestionId, questionId));
  }

  private CompetitionQuestionEntity findQuestion(Long tenantId, Long eventId, Long questionId) {
    CompetitionQuestionEntity entity = questionMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(CompetitionQuestionEntity::getId, questionId)
        .eq(CompetitionQuestionEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛试题不存在");
    return entity;
  }

  private void validateScope(Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    CompetitionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId).eq(CompetitionEventEntity::getId, eventId)
        .eq(CompetitionEventEntity::getDeleted, 0));
    if (event == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    if (workTypeId == null || levelId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择赛事、工种和等级范围");
    CompetitionEventWorkTypeEntity workType = workTypeMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
        .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeEntity::getId, workTypeId)
        .eq(CompetitionEventWorkTypeEntity::getEventId, eventId).eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
        .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeLevelEntity::getId, levelId)
        .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId).eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workTypeId)
        .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种等级不存在");
  }

  private void lockEvent(Long tenantId, Long eventId) {
    CompetitionEventEntity event = eventMapper.selectForUpdate(tenantId, eventId);
    if (event == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
  }

  private CompetitionQuestionVO toVO(Long tenantId, CompetitionQuestionEntity entity, boolean includeChildren) {
    CompetitionPracticalQuestionDetailEntity detail = includeChildren
        ? practicalDetailMapper.selectByQuestion(tenantId, entity.getId()) : null;
    List<CompetitionPracticalQuestionScoreItemEntity> scoreItems = includeChildren
        ? scoreItemMapper.selectByQuestion(tenantId, entity.getId()) : List.of();
    List<CompetitionPracticalScoreItemVO> itemVOs = new ArrayList<>();
    if (scoreItems != null) {
      for (CompetitionPracticalQuestionScoreItemEntity item : scoreItems) {
        itemVOs.add(new CompetitionPracticalScoreItemVO(String.valueOf(item.getId()), item.getItemNo(),
            item.getItemName(), item.getCriterion(), item.getMaxScore(), item.getMaxDeduction(), item.getDeductionStep(), item.getStatus()));
      }
    }
    return new CompetitionQuestionVO(String.valueOf(entity.getId()), String.valueOf(entity.getEventId()),
        String.valueOf(entity.getEventWorkTypeId()), String.valueOf(entity.getEventWorkTypeLevelId()), entity.getContentType(),
        entity.getQuestionType(), entity.getSortOrder(), entity.getStem(), entity.getReferenceAnswer(),
        entity.getFillAnswer(), entity.getOptionsJson(), entity.getDifficulty(), entity.getStatus(),
        entity.getSourceBatchId() == null ? null : String.valueOf(entity.getSourceBatchId()),
        detail == null ? null : detail.getOperationContent(), detail == null ? null : detail.getStudentPrepare(),
        detail == null ? null : detail.getEquipmentPrepare(), itemVOs);
  }

  private CompetitionQuestionImportBatchVO toBatchVO(CompetitionQuestionImportBatchEntity batch) {
    return new CompetitionQuestionImportBatchVO(String.valueOf(batch.getId()), String.valueOf(batch.getEventId()),
        String.valueOf(batch.getEventWorkTypeId()), String.valueOf(batch.getEventWorkTypeLevelId()), batch.getContentType(),
        batch.getOriginalFilename(), batch.getStatus(), batch.getTotalCount(), batch.getSuccessCount(), batch.getFailureCount(),
        batch.getErrorDetail(), batch.getStartedAt(), batch.getFinishedAt());
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition question request has no authenticated platform user");
      throw ex;
    }
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private void deleteQuietly(Path path) {
    try { Files.deleteIfExists(path); } catch (IOException ex) { log.warn("Failed to delete temporary import file {}", path); }
  }

  private String jsonEscape(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
  }
}
