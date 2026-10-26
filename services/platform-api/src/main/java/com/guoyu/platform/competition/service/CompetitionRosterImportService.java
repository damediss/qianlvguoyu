package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionRosterImportQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterImportListQueryReq;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.enums.CompetitionRosterImportStatus;
import com.guoyu.platform.competition.enums.CompetitionRosterValidationStatus;
import com.guoyu.platform.competition.mapper.CompetitionRosterImportMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionRosterImportService {

  private static final Set<String> ALLOWED_FIELDS = Set.of(
      "name", "idCardNo", "mobile", "workType", "level");
  private static final Set<String> REQUIRED_FIELDS = Set.of(
      "name", "idCardNo", "mobile");
  private static final Set<String> DIRECT_ALLOWED_FIELDS = Set.of(
      "orgName", "name", "idCardNo", "mobile", "workType", "level");
  private static final Set<String> DIRECT_REQUIRED_FIELDS = Set.of(
      "orgName", "name", "idCardNo", "mobile", "workType", "level");

  /** Short level descriptions used in roster templates, mapped to standard level codes. */
  private static final Map<String, String> STANDARD_LEVEL_ALIASES = Map.of(
      "初级", "5",
      "中级", "4",
      "高级", "3",
      "技师", "2",
      "高级技师", "1");

  private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
      Map.entry("orgName", "orgName"),
      Map.entry("企业名称", "orgName"),
      Map.entry("公司名称", "orgName"),
      Map.entry("参赛企业", "orgName"),
      Map.entry("参赛单位", "orgName"),
      Map.entry("单位名称", "orgName"),
      Map.entry("name", "name"),
      Map.entry("姓名", "name"),
      Map.entry("idCardNo", "idCardNo"),
      Map.entry("身份证号", "idCardNo"),
      Map.entry("身份证号码", "idCardNo"),
      Map.entry("mobile", "mobile"),
      Map.entry("手机号", "mobile"),
      Map.entry("手机号码", "mobile"),
      Map.entry("workType", "workType"),
      Map.entry("工种", "workType"),
      Map.entry("职业工种", "workType"),
      Map.entry("报名工种", "workType"),
      Map.entry("赛事工种", "workType"),
      Map.entry("level", "level"),
      Map.entry("等级", "level"),
      Map.entry("等级名称", "level"),
      Map.entry("报名等级", "level"),
      Map.entry("赛事等级", "level"));

  private final CompetitionRosterImportMapper importMapper;
  private final CompetitionRosterItemMapper itemMapper;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;

  /** Reference data used by the new row-oriented import flow. */
  public record DirectImportReferenceData(
      List<OrganizationReference> organizations,
      List<WorkTypeReference> workTypes) {
  }

  public record OrganizationReference(
      Long id,
      String name,
      boolean importAllowed) {
  }

  public record WorkTypeReference(
      Long id,
      String displayName,
      String sourceName,
      List<LevelReference> levels) {
  }

  public record LevelReference(
      Long id,
      String code,
      String name,
      String sourceCode,
      String sourceText) {

    public LevelReference(Long id, String code, String name, String sourceText) {
      this(id, code, name, null, sourceText);
    }
  }

  /** Internal representation for a manually-created player row. */
  public record SingleWorkTypeSelection(
      Long eventWorkTypeId,
      String workTypeName,
      Long eventWorkTypeLevelId,
      String levelCode,
      String levelName) {
  }

  public CompetitionRosterImportService(
      CompetitionRosterImportMapper importMapper,
      CompetitionRosterItemMapper itemMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService) {
    this.importMapper = importMapper;
    this.itemMapper = itemMapper;
    this.playerIdentityService = playerIdentityService;
  }

  @Transactional
  public CompetitionRosterImportEntity precheck(
      Long tenantId, Long eventId, Long orgId, Long fileId, Long eventWorkTypeId,
      String selectedWorkTypeName, byte[] fileBytes) {
    List<ParsedRow> rows = parse(fileBytes);
    return validateAndPersist(tenantId, eventId, orgId, fileId, eventWorkTypeId,
        selectedWorkTypeName, rows, "ROSTER-");
  }

  public CompetitionRosterImportEntity precheck(
      Long tenantId, Long eventId, Long orgId, Long fileId, byte[] fileBytes) {
    return precheck(tenantId, eventId, orgId, fileId, null, null, fileBytes);
  }

  /**
   * Prechecks a roster whose organization, work type and level are supplied by
   * each row.  No row is silently assigned to a UI-selected organization or
   * work type.
   */
  @Transactional
  public CompetitionRosterImportEntity precheckDirect(
      Long tenantId,
      Long eventId,
      Long fileId,
      Long permittedOrgId,
      byte[] fileBytes,
      DirectImportReferenceData references) {
    List<DirectParsedRow> rows = parseDirect(fileBytes);
    List<DirectParsedRow> resolvedRows = new ArrayList<>();
    for (DirectParsedRow row : rows) {
      DirectParsedRow validated = validateDirectRow(row);
      resolvedRows.add(resolveDirectReferences(validated, references, permittedOrgId));
    }
    resolvedRows = resolvedRows.stream().map(row -> resolveDirectPlayer(row, tenantId)).toList();
    resolvedRows = markDirectPlayerDuplicates(resolvedRows);
    return persistDirectRows(tenantId, eventId, fileId, resolvedRows);
  }

  @Transactional
  public CompetitionRosterImportEntity precheckSingle(
      Long tenantId,
      Long eventId,
      Long orgId,
      Long eventWorkTypeId,
      String selectedWorkTypeName,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level) {
    return validateAndPersist(tenantId, eventId, orgId, null, eventWorkTypeId, selectedWorkTypeName,
        List.of(new ParsedRow(name, idCardNo, mobile, workType, level, null, List.of())),
        "SINGLE-");
  }

  public CompetitionRosterImportEntity precheckSingle(
      Long tenantId,
      Long eventId,
      Long orgId,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level) {
    return precheckSingle(tenantId, eventId, orgId, null, null, name, idCardNo, mobile,
        workType, level);
  }

  /** Creates one import batch containing one roster row per selected work type. */
  @Transactional
  public CompetitionRosterImportEntity precheckSingleMulti(
      Long tenantId,
      Long eventId,
      Long orgId,
      String name,
      String idCardNo,
      String mobile,
      List<SingleWorkTypeSelection> selections) {
    if (selections == null || selections.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个赛事工种");
    }
    ParsedRow base = validateRow(
        new ParsedRow(name, idCardNo, mobile, null, null, null, List.of()), null);
    Long playerId = null;
    List<String> baseErrors = new ArrayList<>(base.errors());
    if (baseErrors.isEmpty()) {
      try {
        var player = playerIdentityService.resolveOrCreate(tenantId, idCardNo, mobile);
        if (player == null || player.getId() == null) {
          baseErrors.add("选手身份创建失败");
        } else {
          playerId = player.getId();
        }
      } catch (BusinessException ex) {
        if (!ErrorCode.CONFLICT.equals(ex.getCode())) {
          throw ex;
        }
        baseErrors.add(ex.getMessage());
      }
    }

    List<SingleWorkTypeSelection> safeSelections = selections.stream()
        .filter(Objects::nonNull)
        .toList();
    if (safeSelections.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个赛事工种");
    }
    List<List<String>> selectionErrors = new ArrayList<>();
    int invalidCount = 0;
    for (SingleWorkTypeSelection selection : safeSelections) {
      List<String> errors = new ArrayList<>(baseErrors);
      if (selection.eventWorkTypeId() == null) errors.add("赛事工种不能为空");
      if (isBlank(selection.workTypeName())) errors.add("赛事工种名称不能为空");
      if (isBlank(selection.levelName())) errors.add("赛事工种等级不能为空");
      selectionErrors.add(errors);
      if (!errors.isEmpty()) invalidCount++;
    }

    CompetitionRosterImportEntity batch = new CompetitionRosterImportEntity();
    batch.setTenantId(tenantId);
    batch.setEventId(eventId);
    batch.setOrgId(orgId);
    batch.setEventWorkTypeId(singleValue(safeSelections.stream()
        .map(SingleWorkTypeSelection::eventWorkTypeId).filter(Objects::nonNull).toList()));
    batch.setBatchNo("SINGLE-" + UUID.randomUUID().toString().replace("-", ""));
    batch.setStatus(invalidCount == 0
        ? CompetitionRosterImportStatus.PENDING_CONFIRM.getCode()
        : CompetitionRosterImportStatus.VALIDATION_FAILED.getCode());
    batch.setTotalCount(safeSelections.size());
    batch.setValidCount(safeSelections.size() - invalidCount);
    batch.setInvalidCount(invalidCount);
    batch.setSummary(invalidCount == 0 ? "预检通过，等待确认" : "预检完成，存在校验失败行");
    batch.setDeleted(0);
    importMapper.insert(batch);

    for (int index = 0; index < safeSelections.size(); index++) {
      SingleWorkTypeSelection selection = safeSelections.get(index);
      List<String> errors = selectionErrors.get(index);
      CompetitionRosterItemEntity item = new CompetitionRosterItemEntity();
      item.setTenantId(tenantId);
      item.setBatchId(batch.getId());
      item.setPlayerId(playerId);
      item.setOrgId(orgId);
      item.setEventWorkTypeId(selection.eventWorkTypeId());
      item.setEventWorkTypeLevelId(selection.eventWorkTypeLevelId());
      item.setName(name);
      item.setWorkType(selection.workTypeName());
      item.setLevel(selection.levelName());
      item.setValidationStatus(errors.isEmpty()
          ? CompetitionRosterValidationStatus.VALID.getCode()
          : CompetitionRosterValidationStatus.INVALID.getCode());
      item.setErrorMessage(errors.isEmpty() ? null : String.join("; ", errors));
      item.setDeleted(0);
      itemMapper.insert(item);
    }
    return batch;
  }

  public byte[] createTemplate() {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("名单导入");
      Row header = sheet.createRow(0);
      String[] fields = {"企业名称", "姓名", "身份证号", "手机号", "报名工种", "报名等级"};
      for (int i = 0; i < fields.length; i++) {
        header.createCell(i).setCellValue(fields[i]);
        sheet.setColumnWidth(i, 20 * 256);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      log.error("Failed to create competition roster import template", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "名单导入模板生成失败");
    }
  }

  private CompetitionRosterImportEntity validateAndPersist(
      Long tenantId,
      Long eventId,
      Long orgId,
      Long fileId,
      Long eventWorkTypeId,
      String selectedWorkTypeName,
      List<ParsedRow> rows,
      String batchPrefix) {
    if (rows == null || rows.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单不能为空");
    }
    if (selectedWorkTypeName != null && selectedWorkTypeName.trim().length() > 64) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种名称长度不能超过64");
    }
    List<ParsedRow> validatedRows = rows.stream()
        .map(row -> validateRow(row, selectedWorkTypeName))
        .map(row -> resolvePlayer(row, tenantId))
        .toList();
    int invalidCount = (int) validatedRows.stream().filter(row -> !row.errors().isEmpty()).count();

    CompetitionRosterImportEntity batch = new CompetitionRosterImportEntity();
    batch.setTenantId(tenantId);
    batch.setEventId(eventId);
    batch.setOrgId(orgId);
    batch.setEventWorkTypeId(eventWorkTypeId);
    batch.setBatchNo(batchPrefix + UUID.randomUUID().toString().replace("-", ""));
    batch.setSourceFileId(fileId);
    batch.setStatus(invalidCount == 0
        ? CompetitionRosterImportStatus.PENDING_CONFIRM.getCode()
        : CompetitionRosterImportStatus.VALIDATION_FAILED.getCode());
    batch.setTotalCount(validatedRows.size());
    batch.setValidCount(validatedRows.size() - invalidCount);
    batch.setInvalidCount(invalidCount);
    batch.setSummary(invalidCount == 0 ? "预检通过，等待确认" : "预检完成，存在校验失败行");
    batch.setDeleted(0);
    importMapper.insert(batch);

    for (ParsedRow row : validatedRows) {
      CompetitionRosterItemEntity item = new CompetitionRosterItemEntity();
      item.setTenantId(tenantId);
      item.setBatchId(batch.getId());
      item.setPlayerId(row.playerId());
      item.setName(row.name());
      item.setWorkType(selectedWorkTypeName == null || selectedWorkTypeName.isBlank()
          ? row.workType() : selectedWorkTypeName);
      item.setLevel(row.level());
      item.setValidationStatus(row.errors().isEmpty()
          ? CompetitionRosterValidationStatus.VALID.getCode()
          : CompetitionRosterValidationStatus.INVALID.getCode());
      item.setErrorMessage(row.errors().isEmpty() ? null : String.join("; ", row.errors()));
      item.setDeleted(0);
      itemMapper.insert(item);
    }
    return batch;
  }

  @Transactional(readOnly = true)
  public CompetitionRosterImportEntity getById(Long tenantId, Long batchId) {
    CompetitionRosterImportEntity batch = importMapper.selectOne(new LambdaQueryWrapper<CompetitionRosterImportEntity>()
        .eq(CompetitionRosterImportEntity::getTenantId, tenantId)
        .eq(CompetitionRosterImportEntity::getId, batchId)
        .eq(CompetitionRosterImportEntity::getDeleted, 0));
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "名单导入批次不存在");
    }
    return batch;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionRosterImportEntity> pageBatches(
      Long tenantId, Long eventId, CompetitionRosterImportListQueryReq req) {
    LambdaQueryWrapper<CompetitionRosterImportEntity> wrapper = new LambdaQueryWrapper<CompetitionRosterImportEntity>()
        .eq(CompetitionRosterImportEntity::getTenantId, tenantId)
        .eq(CompetitionRosterImportEntity::getEventId, eventId)
        .eq(CompetitionRosterImportEntity::getDeleted, 0)
        .orderByDesc(CompetitionRosterImportEntity::getCreatedAt)
        .orderByDesc(CompetitionRosterImportEntity::getId);
    if (req.getOrgId() != null) {
      wrapper.eq(CompetitionRosterImportEntity::getOrgId, req.getOrgId());
    }
    if (req.getStatus() != null && !req.getStatus().isBlank()) {
      wrapper.eq(CompetitionRosterImportEntity::getStatus, validateImportStatus(req.getStatus()));
    }
    if (req.getBatchNo() != null && !req.getBatchNo().isBlank()) {
      wrapper.like(CompetitionRosterImportEntity::getBatchNo, req.getBatchNo().trim());
    }
    Page<CompetitionRosterImportEntity> page = importMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (page == null) {
      log.warn("Competition roster import mapper returned null page, tenantId={}, eventId={}", tenantId, eventId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional(readOnly = true)
  public Page<CompetitionRosterItemEntity> pageItems(
      Long tenantId, Long batchId, CompetitionRosterImportQueryReq req) {
    Page<CompetitionRosterItemEntity> page = itemMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
         new LambdaQueryWrapper<CompetitionRosterItemEntity>()
             .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
             .eq(CompetitionRosterItemEntity::getBatchId, batchId)
             .eq(CompetitionRosterItemEntity::getDeleted, 0)
             .orderByAsc(CompetitionRosterItemEntity::getId));
    if (page == null) {
      log.warn("Competition roster item mapper returned null page, tenantId={}, batchId={}", tenantId, batchId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
  }

  @Transactional(readOnly = true)
  public List<CompetitionRosterItemEntity> listItems(Long tenantId, Long batchId) {
    List<CompetitionRosterItemEntity> items = itemMapper.selectList(
        new LambdaQueryWrapper<CompetitionRosterItemEntity>()
            .eq(CompetitionRosterItemEntity::getTenantId, tenantId)
            .eq(CompetitionRosterItemEntity::getBatchId, batchId)
            .eq(CompetitionRosterItemEntity::getDeleted, 0)
            .orderByAsc(CompetitionRosterItemEntity::getId));
    return items == null ? new ArrayList<>() : new ArrayList<>(items);
  }

  private List<DirectParsedRow> parseDirect(byte[] fileBytes) {
    if (fileBytes == null || fileBytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件不能为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件没有工作表");
      }
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(0);
      if (header == null || header.getLastCellNum() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单表头不能为空");
      }
      Map<String, Integer> headerIndexes = parseDirectHeader(header);
      List<DirectParsedRow> rows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        String orgName = cellValue(row, headerIndexes.get("orgName"));
        String name = cellValue(row, headerIndexes.get("name"));
        String idCardNo = cellValue(row, headerIndexes.get("idCardNo"));
        String mobile = cellValue(row, headerIndexes.get("mobile"));
        String workType = cellValue(row, headerIndexes.get("workType"));
        String level = cellValue(row, headerIndexes.get("level"));
        if (isBlank(orgName) && isBlank(name)
            && isBlank(idCardNo) && isBlank(mobile) && isBlank(workType) && isBlank(level)) {
          continue;
        }
        rows.add(new DirectParsedRow(rowIndex + 1, orgName, name, idCardNo,
            mobile, workType, level, null, null, null, null, null, null, null, null,
            List.of()));
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单不能为空");
      }
      return rows;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("Failed to parse direct competition roster file", ex);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件解析失败");
    }
  }

  private Map<String, Integer> parseDirectHeader(Row header) {
    DataFormatter formatter = new DataFormatter();
    Map<String, Integer> indexes = new LinkedHashMap<>();
    for (int i = 0; i < header.getLastCellNum(); i++) {
      String headerText = formatter.formatCellValue(header.getCell(i)).trim();
      if (headerText.isBlank()) {
        continue;
      }
      String field = HEADER_ALIASES.get(headerText);
      if (field == null || !DIRECT_ALLOWED_FIELDS.contains(field)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单包含不允许的字段: " + headerText);
      }
      if (indexes.put(field, i) != null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单字段重复: " + headerText);
      }
    }
    if (!indexes.keySet().containsAll(DIRECT_REQUIRED_FIELDS)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "名单必须包含：企业名称、姓名、身份证号、手机号、工种、等级");
    }
    return indexes;
  }

  private DirectParsedRow validateDirectRow(DirectParsedRow row) {
    List<String> errors = new ArrayList<>();
    if (isBlank(row.orgName())) {
      errors.add("企业名称不能为空");
    }
    if (isBlank(row.name())) {
      errors.add("name不能为空");
    } else if (row.name().length() > 128) {
      errors.add("name长度不能超过128");
    }
    if (isBlank(row.idCardNo())) {
      errors.add("idCardNo不能为空");
    } else {
      try {
        playerIdentityService.normalizeIdCard(row.idCardNo());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      }
    }
    if (isBlank(row.mobile())) {
      errors.add("mobile不能为空");
    } else {
      try {
        playerIdentityService.normalizeMobile(row.mobile());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      }
    }
    if (isBlank(row.workType())) {
      errors.add("workType不能为空");
    } else if (row.workType().length() > 64) {
      errors.add("workType长度不能超过64");
    }
    if (isBlank(row.level())) {
      errors.add("level不能为空");
    } else if (row.level().length() > 32) {
      errors.add("level长度不能超过32");
    }
    return withDirectErrors(row, errors);
  }

  private DirectParsedRow resolveDirectReferences(
      DirectParsedRow row,
      DirectImportReferenceData references,
      Long permittedOrgId) {
    List<String> errors = new ArrayList<>(row.errors());
    OrganizationReference organization = null;
    if (!isBlank(row.orgName())) {
      List<OrganizationReference> candidates = safeList(references == null ? null : references.organizations())
          .stream()
          .filter(Objects::nonNull)
          .filter(candidate -> sameText(candidate.name(), row.orgName()))
          .toList();
      if (candidates.size() > 1) {
        errors.add("企业匹配结果不唯一");
      } else if (candidates.isEmpty()) {
        errors.add("企业未匹配到当前赛事的参赛企业");
      } else {
        organization = candidates.get(0);
        if (!organization.importAllowed()) {
          errors.add("企业未获得当前赛事的名单导入权限");
        }
        if (permittedOrgId != null && !Objects.equals(permittedOrgId, organization.id())) {
          errors.add("导入文件包含当前账号无权维护的企业");
        }
      }
    }

    WorkTypeReference workType = null;
    if (!isBlank(row.workType())) {
      List<WorkTypeReference> candidates = safeList(references == null ? null : references.workTypes())
          .stream()
          .filter(Objects::nonNull)
          .filter(candidate -> sameText(candidate.displayName(), row.workType())
              || sameText(candidate.sourceName(), row.workType()))
          .toList();
      if (candidates.size() > 1) {
        errors.add("报名工种匹配结果不唯一");
      } else if (candidates.isEmpty()) {
        errors.add("报名工种未匹配到当前赛事工种");
      } else {
        workType = candidates.get(0);
      }
    }

    LevelReference level = null;
    if (workType != null && !isBlank(row.level())) {
      List<LevelReference> candidates = matchingLevelCandidates(workType.levels(), row.level());
      if (candidates.size() > 1) {
        errors.add("报名等级匹配结果不唯一");
      } else if (candidates.isEmpty()) {
        errors.add("报名等级不属于所报名工种");
      } else {
        level = candidates.get(0);
      }
    }
    if (workType != null && safeList(workType.levels()).isEmpty()) {
      errors.add("所报名工种尚未配置可报名等级");
    }

    return new DirectParsedRow(
        row.rowNumber(), row.orgName(), row.name(), row.idCardNo(),
        row.mobile(), row.workType(), row.level(), row.playerId(),
        organization == null ? null : organization.id(),
        organization == null ? null : organization.name(),
        workType == null ? null : workType.id(),
        workType == null ? null : workType.displayName(),
        level == null ? null : level.id(),
        level == null ? null : level.code(),
        level == null ? null : level.name(), errors);
  }

  private List<LevelReference> matchingLevelCandidates(
      List<LevelReference> levels, String requestedLevel) {
    List<LevelReference> safeLevels = safeList(levels).stream()
        .filter(Objects::nonNull)
        .toList();
    List<LevelReference> exactCandidates = safeLevels.stream()
        .filter(candidate -> sameText(candidate.code(), requestedLevel)
            || sameText(candidate.sourceCode(), requestedLevel)
            || sameText(candidate.name(), requestedLevel)
            || sameText(candidate.sourceText(), requestedLevel))
        .toList();
    if (!exactCandidates.isEmpty()) {
      return exactCandidates;
    }

    String aliasCode = STANDARD_LEVEL_ALIASES.get(requestedLevel.trim());
    if (aliasCode == null) {
      return List.of();
    }
    return safeLevels.stream()
        .filter(candidate -> sameText(candidate.code(), aliasCode)
            || sameText(candidate.sourceCode(), aliasCode))
        .toList();
  }

  private List<DirectParsedRow> markDirectPlayerDuplicates(List<DirectParsedRow> rows) {
    Map<String, Integer> counts = new HashMap<>();
    for (DirectParsedRow row : rows) {
      if (row.playerId() != null && row.orgId() != null && row.eventWorkTypeId() != null
          && row.errors().isEmpty()) {
        String key = row.orgId() + "::" + row.eventWorkTypeId() + "::" + row.playerId();
        counts.merge(key, 1, Integer::sum);
      }
    }
    return rows.stream().map(row -> {
      if (row.playerId() == null || row.orgId() == null || row.eventWorkTypeId() == null
          || !row.errors().isEmpty()) {
        return row;
      }
      String key = row.orgId() + "::" + row.eventWorkTypeId() + "::" + row.playerId();
      if (counts.getOrDefault(key, 0) <= 1) {
        return row;
      }
      List<String> errors = new ArrayList<>(row.errors());
      errors.add("同一企业同一工种的选手在当前批次中重复报名");
      return withDirectErrors(row, errors);
    }).toList();
  }

  private DirectParsedRow resolveDirectPlayer(DirectParsedRow row, Long tenantId) {
    if (!row.errors().isEmpty()) {
      return row;
    }
    try {
      var player = playerIdentityService.resolveOrCreate(tenantId, row.idCardNo(), row.mobile());
      if (player == null || player.getId() == null) {
        List<String> errors = new ArrayList<>(row.errors());
        errors.add("选手身份创建失败");
        return withDirectErrors(row, errors);
      }
      return new DirectParsedRow(
          row.rowNumber(), row.orgName(), row.name(), row.idCardNo(),
          row.mobile(), row.workType(), row.level(), player.getId(), row.orgId(),
          row.resolvedOrgName(), row.eventWorkTypeId(), row.resolvedWorkTypeName(),
          row.eventWorkTypeLevelId(), row.eventWorkTypeLevelCode(), row.eventWorkTypeLevelName(),
          row.errors());
    } catch (BusinessException ex) {
      if (!ErrorCode.CONFLICT.equals(ex.getCode())) {
        throw ex;
      }
      List<String> errors = new ArrayList<>(row.errors());
      errors.add(ex.getMessage());
      return withDirectErrors(row, errors);
    }
  }

  private CompetitionRosterImportEntity persistDirectRows(
      Long tenantId, Long eventId, Long fileId, List<DirectParsedRow> rows) {
    int invalidCount = (int) rows.stream().filter(row -> !row.errors().isEmpty()).count();
    CompetitionRosterImportEntity batch = new CompetitionRosterImportEntity();
    batch.setTenantId(tenantId);
    batch.setEventId(eventId);
    batch.setOrgId(singleValue(rows.stream().filter(row -> row.errors().isEmpty())
        .map(DirectParsedRow::orgId).filter(Objects::nonNull).distinct().toList()));
    batch.setEventWorkTypeId(singleValue(rows.stream().filter(row -> row.errors().isEmpty())
        .map(DirectParsedRow::eventWorkTypeId).filter(Objects::nonNull).distinct().toList()));
    batch.setBatchNo("ROSTER-" + UUID.randomUUID().toString().replace("-", ""));
    batch.setSourceFileId(fileId);
    batch.setStatus(invalidCount == 0
        ? CompetitionRosterImportStatus.PENDING_CONFIRM.getCode()
        : CompetitionRosterImportStatus.VALIDATION_FAILED.getCode());
    batch.setTotalCount(rows.size());
    batch.setValidCount(rows.size() - invalidCount);
    batch.setInvalidCount(invalidCount);
    batch.setSummary(invalidCount == 0
        ? "预检通过，已按企业、工种和等级匹配，等待确认"
        : "预检完成，存在企业、工种、等级或选手身份校验失败行");
    batch.setDeleted(0);
    importMapper.insert(batch);

    for (DirectParsedRow row : rows) {
      CompetitionRosterItemEntity item = new CompetitionRosterItemEntity();
      item.setTenantId(tenantId);
      item.setBatchId(batch.getId());
      item.setPlayerId(row.playerId());
      item.setOrgId(row.orgId());
      item.setOrgName(row.resolvedOrgName() == null ? row.orgName() : row.resolvedOrgName());
      item.setEventWorkTypeId(row.eventWorkTypeId());
      item.setEventWorkTypeLevelId(row.eventWorkTypeLevelId());
      item.setSourceRowNo(row.rowNumber());
      item.setName(row.name());
      item.setWorkType(row.resolvedWorkTypeName() == null ? row.workType() : row.resolvedWorkTypeName());
      item.setLevel(row.eventWorkTypeLevelName() == null ? row.level() : row.eventWorkTypeLevelName());
      item.setValidationStatus(row.errors().isEmpty()
          ? CompetitionRosterValidationStatus.VALID.getCode()
          : CompetitionRosterValidationStatus.INVALID.getCode());
      item.setErrorMessage(row.errors().isEmpty() ? null : String.join("; ", row.errors()));
      item.setDeleted(0);
      itemMapper.insert(item);
    }
    return batch;
  }

  private DirectParsedRow withDirectErrors(DirectParsedRow row, List<String> errors) {
    return new DirectParsedRow(
        row.rowNumber(), row.orgName(), row.name(), row.idCardNo(),
        row.mobile(), row.workType(), row.level(), row.playerId(), row.orgId(),
        row.resolvedOrgName(), row.eventWorkTypeId(), row.resolvedWorkTypeName(),
        row.eventWorkTypeLevelId(), row.eventWorkTypeLevelCode(), row.eventWorkTypeLevelName(), errors);
  }

  private boolean sameText(String left, String right) {
    return !isBlank(left) && !isBlank(right) && left.trim().equals(right.trim());
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private <T> T singleValue(List<T> values) {
    return values != null && values.size() == 1 ? values.get(0) : null;
  }

  private List<ParsedRow> parse(byte[] fileBytes) {
    if (fileBytes == null || fileBytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件不能为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件没有工作表");
      }
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(0);
      if (header == null || header.getLastCellNum() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单表头不能为空");
      }
      Map<String, Integer> headerIndexes = parseHeader(header);
      List<ParsedRow> rows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        String name = cellValue(row, headerIndexes.get("name"));
        String idCardNo = cellValue(row, headerIndexes.get("idCardNo"));
        String mobile = cellValue(row, headerIndexes.get("mobile"));
        String workType = cellValue(row, headerIndexes.get("workType"));
        String level = cellValue(row, headerIndexes.get("level"));
        if (isBlank(name) && isBlank(idCardNo) && isBlank(mobile)
            && isBlank(workType) && isBlank(level)) {
          continue;
        }
        rows.add(new ParsedRow(name, idCardNo, mobile, workType, level, null, List.of()));
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单不能为空");
      }
      return rows;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("Failed to parse competition roster file", ex);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件解析失败");
    }
  }

  private Map<String, Integer> parseHeader(Row header) {
    DataFormatter formatter = new DataFormatter();
    Map<String, Integer> indexes = new LinkedHashMap<>();
    for (int i = 0; i < header.getLastCellNum(); i++) {
      String headerText = formatter.formatCellValue(header.getCell(i)).trim();
      if (headerText.isBlank()) {
        continue;
      }
      String field = HEADER_ALIASES.get(headerText);
      if (field == null || !ALLOWED_FIELDS.contains(field)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单包含不允许的字段: " + headerText);
      }
      if (indexes.put(field, i) != null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "名单字段重复: " + headerText);
      }
    }
    if (!indexes.keySet().containsAll(REQUIRED_FIELDS)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "名单必须包含：姓名、身份证号、手机号");
    }
    return indexes;
  }

  private ParsedRow validateRow(
      ParsedRow row,
      String selectedWorkTypeName) {
    List<String> errors = new ArrayList<>();
    if (isBlank(row.name())) {
      errors.add("name不能为空");
    } else if (row.name().length() > 128) {
      errors.add("name长度不能超过128");
    }
    if (isBlank(row.idCardNo())) {
      errors.add("idCardNo不能为空");
    } else {
      try {
        playerIdentityService.normalizeIdCard(row.idCardNo());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      }
    }
    if (isBlank(row.mobile())) {
      errors.add("mobile不能为空");
    } else {
      try {
        playerIdentityService.normalizeMobile(row.mobile());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      }
    }
    if (row.workType() != null && row.workType().length() > 64) {
      errors.add("workType长度不能超过64");
    }
    if (!isBlank(selectedWorkTypeName) && !isBlank(row.workType())
        && !selectedWorkTypeName.trim().equals(row.workType().trim())) {
      errors.add("模板工种与所选赛事工种不一致：模板为“" + row.workType().trim()
          + "”，所选为“" + selectedWorkTypeName.trim() + "”");
    }
    if (row.level() != null && row.level().length() > 32) {
      errors.add("level长度不能超过32");
    }
    return new ParsedRow(row.name(), row.idCardNo(), row.mobile(), row.workType(), row.level(),
        null, errors);
  }

  private ParsedRow resolvePlayer(ParsedRow row, Long tenantId) {
    if (!row.errors().isEmpty()) {
      return row;
    }
    try {
      Long playerId = playerIdentityService.resolveOrCreate(tenantId, row.idCardNo(), row.mobile()).getId();
      return new ParsedRow(row.name(), row.idCardNo(), row.mobile(), row.workType(), row.level(),
          playerId, row.errors());
    } catch (BusinessException ex) {
      if (!ErrorCode.CONFLICT.equals(ex.getCode())) {
        throw ex;
      }
      List<String> errors = new ArrayList<>(row.errors());
      errors.add(ex.getMessage());
      return new ParsedRow(row.name(), row.idCardNo(), row.mobile(), row.workType(), row.level(),
          null, errors);
    }
  }

  private String cellValue(Row row, Integer index) {
    if (row == null || index == null) {
      return null;
    }
    String value = new DataFormatter().formatCellValue(row.getCell(index));
    return value == null ? null : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String validateImportStatus(String status) {
    for (CompetitionRosterImportStatus value : CompetitionRosterImportStatus.values()) {
      if (value.getCode().equals(status)) return status;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "名单批次状态不合法");
  }

  private record ParsedRow(
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level,
      Long playerId,
      List<String> errors) {
  }

  private record DirectParsedRow(
      int rowNumber,
      String orgName,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level,
      Long playerId,
      Long orgId,
      String resolvedOrgName,
      Long eventWorkTypeId,
      String resolvedWorkTypeName,
      Long eventWorkTypeLevelId,
      String eventWorkTypeLevelCode,
      String eventWorkTypeLevelName,
      List<String> errors) {
  }
}
