package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportErrorEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateSourceStagingMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.PreparationImportBatchVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

/** Imports the direct-recognition Excel workbook and creates a frozen certification-side source snapshot. */
@Service
@Slf4j
public class CertificationCandidateCsvImportService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final List<String> HEADERS = List.of(
      "序号", "姓名", "身份证号", "身份证地址", "性别", "手机号码", "文化程度（最低为初中）",
      "所学专业（初中、高中学历的填无）", "毕业证编号（如有请填写，如没有请填写入学时间）", "毕业学校",
      "毕业时间", "参加工作时间(第一次工作时间）", "民族", "政治面貌", "所在单位", "所在岗位", "入职时间",
      "劳动合同签订开始时间", "劳动合同签订结束时间", "负责人", "负责人电话", "原工种", "原技能等级", "原证书编号");

  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationCandidateImportBatchMapper batchMapper;
  private final CertificationCandidateImportErrorMapper errorMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationCandidateSourceStagingMapper stagingMapper;
  private final CertificationPreparationApplicationService preparationService;
  private final FileApplicationService fileApplicationService;
  private final TransactionTemplate transactionTemplate;

  public CertificationCandidateCsvImportService(
      CertificationTaskScopeMapper scopeMapper,
      CertificationCandidateImportBatchMapper batchMapper,
      CertificationCandidateImportErrorMapper errorMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationCandidateSourceStagingMapper stagingMapper,
      CertificationPreparationApplicationService preparationService,
      FileApplicationService fileApplicationService,
      PlatformTransactionManager transactionManager
  ) {
    this.scopeMapper = scopeMapper;
    this.batchMapper = batchMapper;
    this.errorMapper = errorMapper;
    this.candidateMapper = candidateMapper;
    this.stagingMapper = stagingMapper;
    this.preparationService = preparationService;
    this.fileApplicationService = fileApplicationService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public byte[] templateBytes() {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("考生信息");
      Row header = sheet.createRow(0);
      for (int column = 0; column < HEADERS.size(); column++) {
        header.createCell(column).setCellValue(HEADERS.get(column));
        sheet.setColumnWidth(column, Math.min(HEADERS.get(column).length() * 2 + 4, 50) * 256);
      }
      for (int index = 0; index < SAMPLE_ROWS.size(); index++) {
        Row row = sheet.createRow(index + 1);
        List<String> values = SAMPLE_ROWS.get(index);
        for (int column = 0; column < values.size(); column++) {
          row.createCell(column).setCellValue(values.get(column));
        }
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考生信息导入模板生成失败");
    }
  }

  public PreparationImportBatchVO importExcel(Long taskId, Long taskScopeId, MultipartFile file, String remark) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择 XLSX 文件");
    }
    if (!StringUtils.hasText(file.getOriginalFilename()) || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 XLSX 格式的考生导入文件");
    }
    CertificationTaskScopeEntity scope = requireScope(taskId, taskScopeId);
    List<SpreadsheetRow> rows = readRows(file);
    FileVO sourceFile = fileApplicationService.upload(file, "private");

    CertificationCandidateImportBatchEntity batch = new CertificationCandidateImportBatchEntity();
    batch.setTaskId(taskId);
    batch.setTaskScopeId(taskScopeId);
    batch.setBatchNo("CIB-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
    batch.setSourceType("other_xlsx");
    batch.setSourceFileId(sourceFile.id());
    batch.setStartedAt(LocalDateTime.now());
    batch.setStatus("processing");
    batch.setRemark(remark);
    batch.setTotalCount(rows.size());
    batch.setSuccessCount(0);
    batch.setFailedCount(0);
    batch.setDuplicateCount(0);
    batch.setDeleted(0);
    batchMapper.insert(batch);

    int success = 0;
    int failed = 0;
    for (SpreadsheetRow row : rows) {
      try {
        transactionTemplate.executeWithoutResult(status -> importRow(taskId, scope, batch.getId(), row, remark));
        success++;
      } catch (BusinessException ex) {
        failed++;
        saveError(taskId, batch.getId(), row.lineNo(), value(row, "姓名"), value(row, "身份证号"),
            value(row, "手机号码"), "candidate_invalid", ex.getMessage());
      } catch (RuntimeException ex) {
        failed++;
        log.error("Certification XLSX import failed. taskId={}, batchId={}, row={}",
            taskId, batch.getId(), row.lineNo(), ex);
        saveError(taskId, batch.getId(), row.lineNo(), value(row, "姓名"), value(row, "身份证号"),
            value(row, "手机号码"), "candidate_import_failed", "导入失败，请联系管理员检查认定考生导入配置");
      }
    }
    batch.setSuccessCount(success);
    batch.setFailedCount(failed);
    batch.setDuplicateCount(failed);
    batch.setStatus(failed == 0 ? "completed" : success == 0 ? "failed" : "completed_with_errors");
    batch.setCompletedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    return PreparationImportBatchVO.from(batch);
  }

  private void importRow(
      Long taskId,
      CertificationTaskScopeEntity scope,
      Long batchId,
      SpreadsheetRow row,
      String remark
  ) {
    PreparationCandidateSaveReq candidateReq = new PreparationCandidateSaveReq(
        required(row, "姓名"), required(row, "身份证号"), value(row, "手机号码"),
        scope.getLevelCode(), scope.getLevelName(), "other_xlsx", value(row, "所在单位"), remark, scope.getId());
    CertificationCandidateEntity candidate = createOrResumeCandidate(taskId, batchId, candidateReq);
    saveSnapshot(taskId, scope, batchId, candidate, row, remark);
  }

  /**
   * A former import could create the candidate before its source snapshot failed. Re-importing
   * the same direct-recognition workbook completes that missing snapshot, but never takes over
   * a candidate from another source or a candidate that already has a direct source snapshot.
   */
  private CertificationCandidateEntity createOrResumeCandidate(
      Long taskId,
      Long batchId,
      PreparationCandidateSaveReq req
  ) {
    CertificationCandidateEntity existing = candidateMapper.selectByTaskAndIdCardIncludingDeleted(
        SecurityContext.currentUser().getTenantId(), taskId, req.idCardNo());
    if (existing != null && !Integer.valueOf(1).equals(existing.getDeleted())
        && "other_xlsx".equals(existing.getImportSource())) {
      long snapshotCount = stagingMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateSourceStagingEntity>()
          .eq(CertificationCandidateSourceStagingEntity::getTaskId, taskId)
          .eq(CertificationCandidateSourceStagingEntity::getCandidateId, existing.getId())
          .eq(CertificationCandidateSourceStagingEntity::getSourceType, "other_xlsx"));
      if (snapshotCount == 0) {
        existing.setImportBatchId(batchId);
        candidateMapper.updateById(existing);
        return existing;
      }
    }
    return preparationService.createCandidateForImport(taskId, req, batchId);
  }

  private CertificationTaskScopeEntity requireScope(Long taskId, Long scopeId) {
    if (scopeId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择认定班级（工种和等级）");
    }
    CertificationTaskScopeEntity scope = scopeMapper.selectById(scopeId);
    if (scope == null || !taskId.equals(scope.getTaskId()) || Integer.valueOf(1).equals(scope.getDeleted())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "所选认定班级不属于当前任务");
    }
    return scope;
  }

  private List<SpreadsheetRow> readRows(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
      if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "XLSX 文件为空");
      }
      DataFormatter formatter = new DataFormatter();
      Row headerRow = sheet.getRow(sheet.getFirstRowNum());
      if (headerRow == null || headerRow.getLastCellNum() != HEADERS.size()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "XLSX 表头必须与考生信息导入模板完全一致");
      }
      List<String> header = new ArrayList<>();
      for (int column = 0; column < HEADERS.size(); column++) {
        header.add(cellValue(headerRow, column, formatter));
      }
      if (!HEADERS.equals(header)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "XLSX 表头必须与考生信息导入模板完全一致");
      }
      List<SpreadsheetRow> rows = new ArrayList<>();
      for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new LinkedHashMap<>();
        boolean hasValue = false;
        for (int column = 0; column < HEADERS.size(); column++) {
          String value = cellValue(row, column, formatter);
          values.put(HEADERS.get(column), value);
          hasValue |= StringUtils.hasText(value);
        }
        if (!hasValue) {
          continue;
        }
        if (row.getLastCellNum() > HEADERS.size()) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "第 " + (rowIndex + 1) + " 行列数与模板不一致");
        }
        rows.add(new SpreadsheetRow(rowIndex + 1, values));
      }
      return rows;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "XLSX 文件读取失败");
    }
  }

  private void saveSnapshot(
      Long taskId,
      CertificationTaskScopeEntity scope,
      Long batchId,
      CertificationCandidateEntity candidate,
      SpreadsheetRow row,
      String remark
  ) {
    CertificationCandidateSourceStagingEntity entity = new CertificationCandidateSourceStagingEntity();
    entity.setTenantId(SecurityContext.currentUser().getTenantId());
    entity.setTaskId(taskId);
    entity.setTaskScopeId(scope.getId());
    entity.setCandidateId(candidate.getId());
    entity.setImportBatchId(batchId);
    entity.setSourceType("other_xlsx");
    entity.setPromoteStatus("promoted");
    entity.setPromotedAt(LocalDateTime.now());
    entity.setCandidateName(value(row, "姓名"));
    entity.setIdCardNo(value(row, "身份证号"));
    entity.setHomeAddress(value(row, "身份证地址"));
    entity.setGender(value(row, "性别"));
    entity.setPhone(value(row, "手机号码"));
    entity.setEducation(value(row, "文化程度（最低为初中）"));
    entity.setMajor(value(row, "所学专业（初中、高中学历的填无）"));
    entity.setGraduationCertificateNo(value(row, "毕业证编号（如有请填写，如没有请填写入学时间）"));
    entity.setSchool(value(row, "毕业学校"));
    entity.setGraduateDate(date(row, "毕业时间"));
    entity.setWorkStartDate(date(row, "参加工作时间(第一次工作时间）"));
    entity.setNation(value(row, "民族"));
    entity.setPoliticalStatus(value(row, "政治面貌"));
    entity.setCompanyName(value(row, "所在单位"));
    entity.setPositionName(value(row, "所在岗位"));
    entity.setEntryDate(date(row, "入职时间"));
    entity.setLaborContractStartDate(date(row, "劳动合同签订开始时间"));
    entity.setLaborContractEndDate(date(row, "劳动合同签订结束时间"));
    entity.setResponsibleName(value(row, "负责人"));
    entity.setResponsiblePhone(value(row, "负责人电话"));
    entity.setOriginalWorkTypeName(value(row, "原工种"));
    entity.setOriginalLevelName(value(row, "原技能等级"));
    entity.setOriginalCertNo(value(row, "原证书编号"));
    entity.setApplyWorkTypeId(scope.getWorkTypeId());
    entity.setApplyWorkTypeName(scope.getWorkTypeName());
    entity.setCertWorkTypeId(scope.getWorkTypeId());
    entity.setCertWorkTypeName(scope.getWorkTypeName());
    entity.setApplyLevelCode(scope.getLevelCode());
    entity.setApplyLevelName(scope.getLevelName());
    entity.setCertLevelName(scope.getLevelName());
    entity.setRawPayload(toJson(row.values()));
    entity.setRemark(remark);
    entity.setDeleted(0);
    stagingMapper.insert(entity);
  }

  private LocalDate date(SpreadsheetRow row, String field) {
    String value = value(row, field);
    if (!StringUtils.hasText(value)) {
      return null;
    }
    for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"), DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy年M月d日"))) {
      try {
        return LocalDate.parse(value, formatter);
      } catch (DateTimeParseException ignored) {
        // Try the next supported format.
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, field + "日期格式不正确");
  }

  private String required(SpreadsheetRow row, String field) {
    String value = value(row, field);
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, field + "不能为空");
    }
    return value;
  }

  private String value(SpreadsheetRow row, String field) {
    return row.values().getOrDefault(field, "").trim();
  }

  private String toJson(Map<String, String> values) {
    try {
      return OBJECT_MAPPER.writeValueAsString(values);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考生导入来源数据序列化失败");
    }
  }

  private void saveError(Long taskId, Long batchId, int rowNo, String name, String idCard, String phone,
                         String code, String message) {
    CertificationCandidateImportErrorEntity error = new CertificationCandidateImportErrorEntity();
    error.setTaskId(taskId);
    error.setBatchId(batchId);
    error.setRowNo(rowNo);
    error.setCandidateName(name);
    error.setIdCardNo(idCard);
    error.setPhone(phone);
    error.setErrorCode(code);
    error.setErrorMessage(message);
    error.setDeleted(0);
    errorMapper.insert(error);
  }

  private static String cellValue(Row row, int column, DataFormatter formatter) {
    if (row == null) {
      return "";
    }
    Cell cell = row.getCell(column);
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }

  private record SpreadsheetRow(int lineNo, Map<String, String> values) {}

  private static final List<List<String>> SAMPLE_ROWS = List.of(
      List.of("1", "测试考生一", "110101199001010015", "北京市东城区测试路1号", "男", "13800138000", "大专", "机械制造", "2010-001", "测试职业技术学院", "2010-07-01", "2011-07-01", "汉族", "群众", "测试企业一", "操作工", "2020-01-01", "2020-01-01", "2025-12-31", "张主管", "13900139000", "无", "无", "无"),
      List.of("2", "测试考生二", "110101199202020025", "北京市西城区测试路2号", "女", "13800138001", "本科", "电气工程", "2014-002", "测试理工大学", "2014-07-01", "2014-08-01", "汉族", "群众", "测试企业二", "检修工", "2021-02-01", "2021-02-01", "2026-01-31", "李主管", "13900139001", "无", "无", "无"),
      List.of("3", "测试考生三", "110101198503030038", "北京市朝阳区测试路3号", "男", "13800138002", "高中", "无", "2003-003", "测试高级中学", "2003-07-01", "2004-07-01", "汉族", "群众", "测试企业三", "设备工", "2019-03-01", "2019-03-01", "2024-12-31", "王主管", "13900139002", "无", "无", "无"),
      List.of("4", "测试考生四", "110101198804040045", "北京市海淀区测试路4号", "女", "13800138003", "中专", "机电一体化", "2006-004", "测试中等专业学校", "2006-07-01", "2007-07-01", "汉族", "群众", "测试企业四", "质检员", "2022-04-01", "2022-04-01", "2027-03-31", "赵主管", "13900139003", "无", "无", "无"),
      List.of("5", "测试考生五", "110101199505050053", "北京市丰台区测试路5号", "男", "13800138004", "大专", "安全工程", "2016-005", "测试职业学院", "2016-07-01", "2016-08-01", "汉族", "群众", "测试企业五", "安全员", "2023-05-01", "2023-05-01", "2028-04-30", "陈主管", "13900139004", "无", "无", "无"));
}
