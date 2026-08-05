package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionRosterImportQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterImportListQueryReq;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
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
      "employeeNo", "name", "idCardNo", "mobile", "workType", "level");
  private static final Set<String> REQUIRED_FIELDS = Set.of(
      "employeeNo", "name", "idCardNo", "mobile");

  private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
      Map.entry("employeeNo", "employeeNo"),
      Map.entry("员工编号", "employeeNo"),
      Map.entry("工号", "employeeNo"),
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
      Map.entry("level", "level"),
      Map.entry("等级", "level"));

  private final CompetitionRosterImportMapper importMapper;
  private final CompetitionRosterItemMapper itemMapper;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;

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

  @Transactional
  public CompetitionRosterImportEntity precheckSingle(
      Long tenantId,
      Long eventId,
      Long orgId,
      Long eventWorkTypeId,
      String selectedWorkTypeName,
      String employeeNo,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level) {
    return validateAndPersist(tenantId, eventId, orgId, null, eventWorkTypeId, selectedWorkTypeName,
        List.of(new ParsedRow(employeeNo, name, idCardNo, mobile, workType, level, null, List.of())),
        "SINGLE-");
  }

  public CompetitionRosterImportEntity precheckSingle(
      Long tenantId,
      Long eventId,
      Long orgId,
      String employeeNo,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level) {
    return precheckSingle(tenantId, eventId, orgId, null, null, employeeNo, name, idCardNo,
        mobile, workType, level);
  }

  public byte[] createTemplate() {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("名单导入");
      Row header = sheet.createRow(0);
      String[] fields = {"员工编号", "姓名", "身份证号", "手机号", "工种", "等级"};
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
    Map<String, Integer> employeeNoCounts = countValues(rows, ParsedRow::employeeNo);
    Map<String, Integer> idCardCounts = countValues(rows, ParsedRow::idCardNo);
    Map<String, Integer> mobileCounts = countValues(rows, ParsedRow::mobile);

    List<ParsedRow> validatedRows = rows.stream()
        .map(row -> validateRow(row, employeeNoCounts, idCardCounts, mobileCounts,
            selectedWorkTypeName))
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
      item.setEmployeeNo(row.employeeNo());
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
            .orderByAsc(CompetitionRosterItemEntity::getId));
    if (page == null) {
      log.warn("Competition roster item mapper returned null page, tenantId={}, batchId={}", tenantId, batchId);
      return new Page<>(req.getPage(), req.getPageSize());
    }
    return page;
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
        String employeeNo = cellValue(row, headerIndexes.get("employeeNo"));
        String name = cellValue(row, headerIndexes.get("name"));
        String idCardNo = cellValue(row, headerIndexes.get("idCardNo"));
        String mobile = cellValue(row, headerIndexes.get("mobile"));
        String workType = cellValue(row, headerIndexes.get("workType"));
        String level = cellValue(row, headerIndexes.get("level"));
        if (isBlank(employeeNo) && isBlank(name) && isBlank(idCardNo) && isBlank(mobile)
            && isBlank(workType) && isBlank(level)) {
          continue;
        }
        rows.add(new ParsedRow(employeeNo, name, idCardNo, mobile, workType, level, null, List.of()));
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
          "名单必须包含：员工编号、姓名、身份证号、手机号");
    }
    return indexes;
  }

  private ParsedRow validateRow(
      ParsedRow row,
      Map<String, Integer> employeeNoCounts,
      Map<String, Integer> idCardCounts,
      Map<String, Integer> mobileCounts,
      String selectedWorkTypeName) {
    List<String> errors = new ArrayList<>();
    if (isBlank(row.employeeNo())) {
      errors.add("employeeNo不能为空");
    } else if (employeeNoCounts.getOrDefault(row.employeeNo(), 0) > 1) {
      errors.add("employeeNo重复");
    } else if (row.employeeNo().length() > 64) {
      errors.add("employeeNo长度不能超过64");
    }
    if (isBlank(row.name())) {
      errors.add("name不能为空");
    } else if (row.name().length() > 128) {
      errors.add("name长度不能超过128");
    }
    if (isBlank(row.idCardNo())) {
      errors.add("idCardNo不能为空");
    } else if (idCardCounts.getOrDefault(row.idCardNo(), 0) > 1) {
      errors.add("idCardNo重复");
    } else {
      try {
        playerIdentityService.normalizeIdCard(row.idCardNo());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      }
    }
    if (isBlank(row.mobile())) {
      errors.add("mobile不能为空");
    } else if (mobileCounts.getOrDefault(row.mobile(), 0) > 1) {
      errors.add("mobile重复");
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
    return new ParsedRow(row.employeeNo(), row.name(), row.idCardNo(), row.mobile(), row.workType(),
        row.level(), null, errors);
  }

  private ParsedRow resolvePlayer(ParsedRow row, Long tenantId) {
    if (!row.errors().isEmpty()) {
      return row;
    }
    try {
      Long playerId = playerIdentityService.resolveOrCreate(tenantId, row.idCardNo(), row.mobile()).getId();
      return new ParsedRow(row.employeeNo(), row.name(), row.idCardNo(), row.mobile(), row.workType(),
          row.level(), playerId, row.errors());
    } catch (BusinessException ex) {
      if (!ErrorCode.CONFLICT.equals(ex.getCode())) {
        throw ex;
      }
      List<String> errors = new ArrayList<>(row.errors());
      errors.add(ex.getMessage());
      return new ParsedRow(row.employeeNo(), row.name(), row.idCardNo(), row.mobile(), row.workType(),
          row.level(), null, errors);
    }
  }

  private Map<String, Integer> countValues(
      List<ParsedRow> rows, java.util.function.Function<ParsedRow, String> extractor) {
    Map<String, Integer> counts = new HashMap<>();
    rows.stream().map(extractor).filter(value -> value != null && !value.isBlank())
        .forEach(value -> counts.merge(value.trim(), 1, Integer::sum));
    return counts;
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
      String employeeNo,
      String name,
      String idCardNo,
      String mobile,
      String workType,
      String level,
      Long playerId,
      List<String> errors) {
  }
}
