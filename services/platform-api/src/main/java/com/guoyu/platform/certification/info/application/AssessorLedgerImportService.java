package com.guoyu.platform.certification.info.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.info.entity.AssessorLedgerImportEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAuditRecordEntity;
import com.guoyu.platform.certification.info.mapper.AssessorLedgerImportMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAuditRecordMapper;
import com.guoyu.platform.certification.info.vo.AssessorLedgerImportVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.service.UserApplicationService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Imports the business-maintained assessor ledger without changing existing assessor records. */
@Service
@Slf4j
public class AssessorLedgerImportService {

  private static final String QUEUED = "queued";
  private static final String PROCESSING = "processing";
  private static final String COMPLETED = "completed";
  private static final String FAILED = "failed";
  private static final String INITIAL_PASSWORD = "12345678";
  private static final LocalDate MISSING_VALID_UNTIL = LocalDate.of(1949, 10, 1);
  private static final Pattern VALID_UNTIL_PATTERN = Pattern.compile("(20\\d{2})年(\\d{1,2})月");
  private static final Pattern DISP_IMAGE_PATTERN = Pattern.compile("DISPIMG\\(\\\"([^\\\"]+)\\\"");
  private static final String DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing";
  private static final String DRAWING_MAIN_NS = "http://schemas.openxmlformats.org/drawingml/2006/main";
  private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
  private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
  private static final String[] HEADERS = {
      "序号", "姓名", "身份证号", "所在单位", "考评级别", "考评职业/工种1", "考评职业/工种2",
      "考评职业/工种", "证号", "有效期", "考评员证", "电话号码", "备注"
  };

  private final AssessorLedgerImportMapper importMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationInfoAuditRecordMapper auditRecordMapper;
  private final RoleMapper roleMapper;
  private final UserMapper userMapper;
  private final UserApplicationService userApplicationService;
  private final FileApplicationService fileApplicationService;
  private final ThreadPoolTaskExecutor executor;
  private final TransactionTemplate transactionTemplate;

  public AssessorLedgerImportService(
      AssessorLedgerImportMapper importMapper,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationInfoAuditRecordMapper auditRecordMapper,
      RoleMapper roleMapper,
      UserMapper userMapper,
      UserApplicationService userApplicationService,
      FileApplicationService fileApplicationService,
      @Qualifier("assessorLedgerImportExecutor") ThreadPoolTaskExecutor executor,
      TransactionTemplate transactionTemplate
  ) {
    this.importMapper = importMapper;
    this.assessorMapper = assessorMapper;
    this.auditRecordMapper = auditRecordMapper;
    this.roleMapper = roleMapper;
    this.userMapper = userMapper;
    this.userApplicationService = userApplicationService;
    this.fileApplicationService = fileApplicationService;
    this.executor = executor;
    this.transactionTemplate = transactionTemplate;
  }

  public AssessorLedgerImportVO start(Long fileId) {
    PlatformUserPrincipal currentUser = SecurityContext.currentUser();
    Long tenantId = currentUser.getTenantId();
    var source = fileApplicationService.getExistingFile(tenantId, fileId);
    if (!source.getOriginalName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 .xlsx 格式的考评员台账");
    }

    AssessorLedgerImportEntity batch = new AssessorLedgerImportEntity();
    batch.setTenantId(tenantId);
    batch.setSourceFileId(fileId);
    batch.setStatus(QUEUED);
    batch.setTotalCount(0);
    batch.setImportedCount(0);
    batch.setSkippedCount(0);
    batch.setFailedCount(0);
    batch.setOperatorId(currentUser.getUserId());
    importMapper.insert(batch);
    try {
      executor.execute(() -> process(batch.getId(), tenantId, currentUser.getUserId()));
    } catch (TaskRejectedException ex) {
      importMapper.deleteById(batch.getId());
      log.warn("考评员台账导入任务被线程池拒绝: tenantId={}, batchId={}", tenantId, batch.getId());
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "导入任务繁忙，请稍后重试");
    }
    return toVO(batch);
  }

  public AssessorLedgerImportVO get(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    AssessorLedgerImportEntity entity = importMapper.selectOne(new LambdaQueryWrapper<AssessorLedgerImportEntity>()
        .eq(AssessorLedgerImportEntity::getTenantId, tenantId)
        .eq(AssessorLedgerImportEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员台账导入任务不存在");
    }
    return toVO(entity);
  }

  private void process(Long batchId, Long tenantId, Long operatorId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      transactionTemplate.executeWithoutResult(status -> importLedger(batchId, tenantId, operatorId));
    } catch (RuntimeException ex) {
      log.error("考评员台账导入失败: tenantId={}, batchId={}", tenantId, batchId, ex);
      markFailed(batchId, "导入失败：" + safeMessage(ex));
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void importLedger(Long batchId, Long tenantId, Long operatorId) {
    AssessorLedgerImportEntity batch = requireBatch(batchId, tenantId);
    batch.setStatus(PROCESSING);
    batch.setStartedAt(LocalDateTime.now());
    importMapper.updateById(batch);

    LedgerWorkbook ledger = parse(batch.getSourceFileId());
    RoleEntity assessorRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getCode, "CERTIFICATION_ASSESSOR")
        .eq(RoleEntity::getStatus, "enabled")
        .last("LIMIT 1"));
    if (assessorRole == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操考评员角色未配置，请先执行最新数据库迁移");
    }

    int imported = 0;
    int skipped = 0;
    int failed = 0;
    List<String> failures = new ArrayList<>();
    try {
      Set<String> existingNames = existingValues(ledger.rows(), true);
      Set<String> existingCertificateNos = existingValues(ledger.rows(), false);
      Set<String> seenNames = new HashSet<>();
      Set<String> seenCertificateNos = new HashSet<>();

      for (LedgerRow row : ledger.rows()) {
      boolean sourceCertificateDuplicate = StringUtils.hasText(row.certificateNo())
          && !seenCertificateNos.add(row.certificateNo());
      if (!seenNames.add(row.name()) || sourceCertificateDuplicate
          || existingNames.contains(row.name()) || (StringUtils.hasText(row.certificateNo())
              && existingCertificateNos.contains(row.certificateNo()))) {
        skipped++;
        continue;
      }
      String validationError = validate(row);
      if (validationError != null) {
        failed++;
        addFailure(failures, row.rowNo(), validationError);
        continue;
      }

      String username = nextAvailableUsername(row.name(), row.identityNo());
      var account = userApplicationService.createForTenant(tenantId, new UserCreateReq(
          null, null, username, INITIAL_PASSWORD, row.name(), "assessor", row.phone(), null, "enabled",
          List.of(assessorRole.getId())
      ));
      CertificationInfoAssessorEntity assessor = new CertificationInfoAssessorEntity();
      assessor.setTenantId(tenantId);
      assessor.setCreatedBy(operatorId);
      assessor.setUpdatedBy(operatorId);
      assessor.setUserId(account.id());
      assessor.setAccountUsername(username);
      assessor.setAccountPassword(INITIAL_PASSWORD);
      assessor.setAssessorName(row.name());
      assessor.setIdentityNo(row.identityNo());
      assessor.setCertificateNo(StringUtils.hasText(row.certificateNo()) ? row.certificateNo() : null);
      assessor.setOrgName(row.orgName());
      assessor.setWorkTypeJson(toJson(row.workTypes()));
      assessor.setWorkTypeScope(String.join("、", row.workTypes()));
      assessor.setLevelScope(levelScope(row.level()));
      assessor.setValidUntil(StringUtils.hasText(row.validUntil())
          ? parseValidUntil(row.validUntil()) : MISSING_VALID_UNTIL);
      assessor.setPhone(row.phone());
      assessor.setMaskedPhone(maskPhone(row.phone()));
      assessor.setMaterialsDescription("业务部门考评员台账批量导入");
      assessor.setRemark(row.remark());
      assessor.setAuditStatus("approved");
      assessor.setWorkStatus("idle");
      assessor.setReferencedCount(0);

        LedgerImage photo = ledger.images().get(row.certificateImageId());
        if (photo != null) {
          String name = (StringUtils.hasText(row.certificateNo()) ? row.certificateNo() : row.name())
              + "-考评员证" + photo.extension();
          FileVO stored = fileApplicationService.storeFromBytesForTenant(
              tenantId, Files.readAllBytes(photo.path()), name, photo.contentType(), "private");
          assessor.setCertificatePhotoFileId(stored.id());
          assessor.setCertificatePhotoFileName(stored.originalName());
        }
        assessorMapper.insert(assessor);
        recordImportAudit(tenantId, operatorId, assessor.getId());
        imported++;
      }
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取台账内的考评员证图片失败");
    } finally {
      ledger.close();
    }

    batch.setStatus(COMPLETED);
    batch.setTotalCount(ledger.rows().size());
    batch.setImportedCount(imported);
    batch.setSkippedCount(skipped);
    batch.setFailedCount(failed);
    batch.setResultMessage(summary(imported, skipped, failed, failures));
    batch.setCompletedAt(LocalDateTime.now());
    importMapper.updateById(batch);
  }

  private AssessorLedgerImportEntity requireBatch(Long id, Long tenantId) {
    AssessorLedgerImportEntity batch = importMapper.selectOne(new LambdaQueryWrapper<AssessorLedgerImportEntity>()
        .eq(AssessorLedgerImportEntity::getTenantId, tenantId)
        .eq(AssessorLedgerImportEntity::getId, id));
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考评员台账导入任务不存在");
    }
    return batch;
  }

  private Set<String> existingValues(List<LedgerRow> rows, boolean names) {
    Collection<String> values = rows.stream().map(row -> names ? row.name() : row.certificateNo())
        .filter(StringUtils::hasText).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (values.isEmpty()) {
      return Set.of();
    }
    LambdaQueryWrapper<CertificationInfoAssessorEntity> query = new LambdaQueryWrapper<>();
    if (names) {
      query.in(CertificationInfoAssessorEntity::getAssessorName, values);
    } else {
      query.in(CertificationInfoAssessorEntity::getCertificateNo, values);
    }
    return assessorMapper.selectList(query).stream().map(item -> names ? item.getAssessorName() : item.getCertificateNo())
        .filter(StringUtils::hasText).collect(java.util.stream.Collectors.toSet());
  }

  private String nextAvailableUsername(String name, String identityNo) {
    String base = usernamePrefix(name) + identityNo.substring(identityNo.length() - 5).toLowerCase(Locale.ROOT);
    String candidate = base;
    int sequence = 2;
    while (userMapper.exists(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, candidate))) {
      candidate = base + sequence++;
    }
    return candidate;
  }

  private String usernamePrefix(String name) {
    StringBuilder result = new StringBuilder();
    for (char ch : name.toCharArray()) {
      String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch);
      if (pinyin != null && pinyin.length > 0 && !pinyin[0].isBlank()) {
        result.append(Character.toLowerCase(pinyin[0].charAt(0)));
      } else if (Character.isLetterOrDigit(ch)) {
        result.append(Character.toLowerCase(ch));
      }
    }
    return result.isEmpty() ? "kp" : result.toString();
  }

  private String validate(LedgerRow row) {
    if (!StringUtils.hasText(row.name())) return "姓名为空";
    if (!StringUtils.hasText(row.identityNo()) || row.identityNo().length() < 5) return "身份证号不足 5 位";
    if (row.workTypes().isEmpty()) return "考评职业/工种1、2 均为空";
    if (!StringUtils.hasText(levelScope(row.level()))) return "考评级别仅支持“高级考评员”或“考评员”";
    if (StringUtils.hasText(row.validUntil())) {
      try {
        parseValidUntil(row.validUntil());
      } catch (IllegalArgumentException ex) {
        return ex.getMessage();
      }
    }
    return null;
  }

  private String levelScope(String level) {
    if ("高级考评员".equals(trim(level))) return "初级-高级技师";
    if ("考评员".equals(trim(level))) return "初级-高级工";
    return null;
  }

  private LocalDate parseValidUntil(String value) {
    Matcher matcher = VALID_UNTIL_PATTERN.matcher(trim(value));
    YearMonth last = null;
    while (matcher.find()) {
      last = YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }
    if (last == null) {
      throw new IllegalArgumentException("有效期格式应为“2023年7月-2026年7月”");
    }
    return last.atEndOfMonth();
  }

  private void recordImportAudit(Long tenantId, Long operatorId, Long assessorId) {
    CertificationInfoAuditRecordEntity audit = new CertificationInfoAuditRecordEntity();
    audit.setTenantId(tenantId);
    audit.setOwnerType("assessor");
    audit.setOwnerId(assessorId);
    audit.setAction("ledger-import");
    audit.setAfterStatus("approved");
    audit.setOperatorId(operatorId);
    audit.setComment("业务部门考评员台账批量导入");
    auditRecordMapper.insert(audit);
  }

  private LedgerWorkbook parse(Long sourceFileId) {
    List<LedgerRow> rows = new ArrayList<>();
    try (InputStream input = fileApplicationService.openFileInputStreamById(sourceFileId);
        Workbook workbook = WorkbookFactory.create(input)) {
      Sheet sheet = workbook.getSheetAt(0);
      validateHeaders(sheet.getRow(0));
      DataFormatter formatter = new DataFormatter(Locale.CHINA);
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        String name = cellText(row, 1, formatter);
        String certificateNo = cellText(row, 8, formatter);
        if (!StringUtils.hasText(name) && !StringUtils.hasText(certificateNo)) continue;
        List<String> workTypes = new ArrayList<>();
        // 优先使用“考评职业/工种1、2”；两列都未维护时，兼容业务台账的合并列。
        addWorkTypes(workTypes, cellText(row, 5, formatter));
        addWorkTypes(workTypes, cellText(row, 6, formatter));
        if (workTypes.isEmpty()) {
          addWorkTypes(workTypes, cellText(row, 7, formatter));
        }
        rows.add(new LedgerRow(i + 1, name, cellText(row, 2, formatter), cellText(row, 3, formatter),
            cellText(row, 4, formatter), workTypes, certificateNo, cellText(row, 9, formatter),
            imageId(row.getCell(10)), cellText(row, 11, formatter), cellText(row, 12, formatter)));
      }
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取考评员台账，请确认文件未损坏");
    }
    ParsedImages images = parseImages(sourceFileId);
    return new LedgerWorkbook(rows, images.images(), images.tempDirectory());
  }

  private void validateHeaders(Row header) {
    if (header == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "台账缺少表头");
    DataFormatter formatter = new DataFormatter(Locale.CHINA);
    for (int i = 0; i < HEADERS.length; i++) {
      if (!HEADERS[i].equals(cellText(header, i, formatter))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "台账表头不匹配，缺少“" + HEADERS[i] + "”列");
      }
    }
  }

  private ParsedImages parseImages(Long sourceFileId) {
    Map<String, byte[]> parts = new HashMap<>();
    Path tempDirectory;
    try {
      tempDirectory = Files.createTempDirectory("assessor-ledger-images-");
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法创建台账图片临时目录");
    }
    try (InputStream input = fileApplicationService.openFileInputStreamById(sourceFileId);
        ZipInputStream zip = new ZipInputStream(input)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.getName().equals("xl/cellimages.xml") || entry.getName().equals("xl/_rels/cellimages.xml.rels")) {
          parts.put(entry.getName(), readAll(zip));
        } else if (!entry.isDirectory() && entry.getName().startsWith("xl/media/")) {
          Path destination = tempDirectory.resolve(Path.of(entry.getName()).getFileName()).normalize();
          if (!destination.startsWith(tempDirectory)) {
            throw new IOException("非法图片路径");
          }
          Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (IOException ex) {
      deleteDirectory(tempDirectory);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取台账内的考评员证图片");
    }
    if (!parts.containsKey("xl/cellimages.xml") || !parts.containsKey("xl/_rels/cellimages.xml.rels")) {
      return new ParsedImages(Map.of(), tempDirectory);
    }
    try {
      Map<String, String> relationshipTargets = parseRelationships(parts.get("xl/_rels/cellimages.xml.rels"));
      Document document = xml(parts.get("xl/cellimages.xml"));
      NodeList nodes = document.getElementsByTagNameNS("*", "cellImage");
      Map<String, LedgerImage> images = new HashMap<>();
      for (int i = 0; i < nodes.getLength(); i++) {
        Element image = (Element) nodes.item(i);
        NodeList names = image.getElementsByTagNameNS(DRAWING_NS, "cNvPr");
        NodeList blips = image.getElementsByTagNameNS(DRAWING_MAIN_NS, "blip");
        if (names.getLength() == 0 || blips.getLength() == 0) continue;
        String imageId = ((Element) names.item(0)).getAttribute("name");
        String relationshipId = ((Element) blips.item(0)).getAttributeNS(REL_NS, "embed");
        String target = relationshipTargets.get(relationshipId);
        if (!StringUtils.hasText(imageId) || !StringUtils.hasText(target)) continue;
        String partName = "xl/" + target.replace("../", "");
        Path content = tempDirectory.resolve(Path.of(partName).getFileName()).normalize();
        if (content.startsWith(tempDirectory) && Files.isRegularFile(content)) {
          images.put(imageId, LedgerImage.of(partName, content));
        }
      }
      return new ParsedImages(images, tempDirectory);
    } catch (Exception ex) {
      log.warn("台账证件图片映射读取失败，将继续导入文字信息", ex);
      return new ParsedImages(Map.of(), tempDirectory);
    }
  }

  private Map<String, String> parseRelationships(byte[] xml) throws Exception {
    Document document = xml(xml);
    NodeList nodes = document.getElementsByTagNameNS(PACKAGE_REL_NS, "Relationship");
    Map<String, String> targets = new HashMap<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Element relationship = (Element) nodes.item(i);
      targets.put(relationship.getAttribute("Id"), relationship.getAttribute("Target"));
    }
    return targets;
  }

  private Document xml(byte[] value) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(value));
  }

  private static byte[] readAll(InputStream input) throws IOException {
    return input.readAllBytes();
  }

  private static String cellText(Row row, int index, DataFormatter formatter) {
    Cell cell = row.getCell(index);
    return cell == null ? "" : trim(formatter.formatCellValue(cell));
  }

  private static String imageId(Cell cell) {
    if (cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.FORMULA) return "";
    Matcher matcher = DISP_IMAGE_PATTERN.matcher(cell.getCellFormula());
    return matcher.find() ? matcher.group(1) : "";
  }

  private static void addWorkTypes(List<String> workTypes, String value) {
    if (!StringUtils.hasText(value)) return;
    for (String item : value.split("[/、，,；;]+")) {
      String workType = trim(item);
      if (StringUtils.hasText(workType) && !workTypes.contains(workType)) workTypes.add(workType);
    }
  }

  private static String toJson(List<String> workTypes) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(workTypes);
    } catch (Exception ex) {
      return "[]";
    }
  }

  private static String maskPhone(String phone) {
    if (!StringUtils.hasText(phone) || phone.length() < 7) return phone;
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  private static void addFailure(List<String> failures, int rowNo, String message) {
    if (failures.size() < 20) failures.add("第" + rowNo + "行：" + message);
  }

  private static String summary(int imported, int skipped, int failed, List<String> failures) {
    String result = "导入 " + imported + " 条，跳过 " + skipped + " 条，失败 " + failed + " 条";
    return failures.isEmpty() ? result : result + "；" + String.join("；", failures);
  }

  private void markFailed(Long batchId, String message) {
    transactionTemplate.executeWithoutResult(status -> {
      AssessorLedgerImportEntity batch = importMapper.selectById(batchId);
      if (batch == null) return;
      batch.setStatus(FAILED);
      batch.setResultMessage(message);
      batch.setCompletedAt(LocalDateTime.now());
      importMapper.updateById(batch);
    });
  }

  private static String safeMessage(RuntimeException ex) {
    return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "系统处理异常";
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private static AssessorLedgerImportVO toVO(AssessorLedgerImportEntity entity) {
    return new AssessorLedgerImportVO(entity.getId(), entity.getStatus(), entity.getTotalCount(),
        entity.getImportedCount(), entity.getSkippedCount(), entity.getFailedCount(), entity.getResultMessage(),
        entity.getStartedAt(), entity.getCompletedAt());
  }

  private static void deleteDirectory(Path directory) {
    if (directory == null) return;
    try (var paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException ex) {
          log.warn("删除考评员台账图片临时文件失败: {}", path, ex);
        }
      });
    } catch (IOException ex) {
      log.warn("删除考评员台账图片临时目录失败: {}", directory, ex);
    }
  }

  private record LedgerWorkbook(List<LedgerRow> rows, Map<String, LedgerImage> images, Path tempDirectory)
      implements AutoCloseable {
    @Override
    public void close() {
      deleteDirectory(tempDirectory);
    }
  }

  private record LedgerRow(
      int rowNo, String name, String identityNo, String orgName, String level, List<String> workTypes,
      String certificateNo, String validUntil, String certificateImageId, String phone, String remark
  ) {
  }

  private record ParsedImages(Map<String, LedgerImage> images, Path tempDirectory) {
  }

  private record LedgerImage(Path path, String extension, String contentType) {
    private static LedgerImage of(String partName, Path path) {
      boolean png = partName.toLowerCase(Locale.ROOT).endsWith(".png");
      return new LedgerImage(path, png ? ".png" : ".jpg", png ? "image/png" : "image/jpeg");
    }
  }
}
