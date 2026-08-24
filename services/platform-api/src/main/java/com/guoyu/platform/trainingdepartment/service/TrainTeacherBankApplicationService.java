package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankFileMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.support.TeacherBankFileTypes;
import com.guoyu.platform.trainingdepartment.support.TeacherCertTextParser;
import com.guoyu.platform.trainingdepartment.support.TeacherCertTextParser.ParsedCerts;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankFileVO;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankImportResultVO;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankVO;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainTeacherBankApplicationService {

  private final TrainTeacherBankMapper mapper;
  private final TrainTeacherBankFileMapper fileMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public TrainTeacherBankApplicationService(
      TrainTeacherBankMapper mapper,
      TrainTeacherBankFileMapper fileMapper,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper) {
    this.mapper = mapper;
    this.fileMapper = fileMapper;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTeacherBankVO> page(TrainTeacherBankQueryReq req) {
    Long tenantId = tenantId();
    LambdaQueryWrapper<TrainTeacherBankEntity> wrapper =
        new LambdaQueryWrapper<TrainTeacherBankEntity>()
        .eq(TrainTeacherBankEntity::getTenantId, tenantId)
        .eq(TrainTeacherBankEntity::getDeleted, 0)
        .orderByDesc(TrainTeacherBankEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      String kw = req.getKeyword().trim();
      wrapper.and(
          w ->
              w.like(TrainTeacherBankEntity::getName, kw)
                  .or()
                  .like(TrainTeacherBankEntity::getPhone, kw)
                  .or()
                  .like(TrainTeacherBankEntity::getIdCardNumber, kw)
                  .or()
                  .like(TrainTeacherBankEntity::getCompany, kw)
          .or()
                  .like(TrainTeacherBankEntity::getEducation, kw)
          .or()
                  .like(TrainTeacherBankEntity::getMajor, kw));
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainTeacherBankEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getEducation())) {
      wrapper.eq(TrainTeacherBankEntity::getEducation, req.getEducation());
    }
    Page<TrainTeacherBankEntity> page =
        mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(e -> toVo(e, false)).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTeacherBankVO getById(Long id) {
    return toVo(getExisting(id), true);
  }

  @Transactional
  public TrainTeacherBankVO create(TrainTeacherBankCreateReq req) {
    Long tenantId = tenantId();
    TrainTeacherBankEntity entity = new TrainTeacherBankEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(SecurityContext.currentUser().getUserId());
    entity.setSource("manual");
    applyCreate(entity, req);
    entity.setDeleted(0);
    if (entity.getStatus() == null) {
      entity.setStatus(1);
    }
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    mapper.insert(entity);
    return toVo(entity, true);
  }

  @Transactional
  public TrainTeacherBankVO update(Long id, TrainTeacherBankUpdateReq req) {
    TrainTeacherBankEntity entity = getExisting(id);
    applyUpdate(entity, req);
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    return toVo(entity, true);
  }

  @Transactional
  public void delete(Long id) {
    TrainTeacherBankEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> downloadImportTemplate() {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("教师信息");
      Row title = sheet.createRow(0);
      title.createCell(0).setCellValue("教师信息表");
      Row header = sheet.createRow(1);
      String[] heads = {
        "序号", "姓名", "性别", "身份证号码", "学历", "专业", "持有何种职业资格证书", "职业资格等级", "职业技能名称", "专业技术职务", "联系电话"
      };
      for (int i = 0; i < heads.length; i++) {
        header.createCell(i).setCellValue(heads[i]);
      }
      wb.write(bos);
      String encoded =
          URLEncoder.encode("教师信息导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
          .contentType(
              MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(bos.toByteArray());
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成模板失败：" + e.getMessage());
    }
  }

  @Transactional
  public TrainTeacherBankImportResultVO importExcel(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 Excel 文件");
    }
    Long tenantId = tenantId();
    List<TrainTeacherBankImportResultVO.Item> failed = new ArrayList<>();
    List<TrainTeacherBankImportResultVO.Item> warnings = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int provisional = 0;
    int success = 0;
    int total = 0;
    DataFormatter fmt = new DataFormatter();
    try (InputStream in = file.getInputStream();
        Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet = wb.getSheetAt(0);
      int headerRowIdx = findHeaderRow(sheet, fmt);
      if (headerRowIdx < 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到表头（需含「姓名」「身份证」）");
      }
      Map<String, Integer> col = mapHeader(sheet.getRow(headerRowIdx), fmt);
      for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        String name = cell(row, col.get("姓名"), fmt);
        String idCardNumber = cell(row, col.get("身份证号码"), fmt);
        if (!StringUtils.hasText(idCardNumber)) {
          idCardNumber = cell(row, col.get("身份证"), fmt);
        }
        String phone = cell(row, col.get("联系电话"), fmt);
        String genderCn = cell(row, col.get("性别"), fmt);
        String education = cell(row, col.get("学历"), fmt);
        String major = cell(row, col.get("专业"), fmt);
        String certText = cell(row, col.get("持有何种职业资格证书"), fmt);
        String qualificationLevel = cell(row, col.get("职业资格等级"), fmt);
        String skillName = cell(row, col.get("职业技能名称"), fmt);
        String professionalTitle = cell(row, col.get("专业技术职务"), fmt);
        if (!StringUtils.hasText(name)
            && !StringUtils.hasText(idCardNumber)
            && !StringUtils.hasText(phone)) {
          continue;
        }
        total++;
        int excelRow = r + 1;
        if (!StringUtils.hasText(name)) {
          failed.add(new TrainTeacherBankImportResultVO.Item(excelRow, name, "姓名为空"));
          continue;
        }
        name = name.trim();
        idCardNumber = normalizeIdCardNumber(idCardNumber);
        phone = blankToNull(phone);
        Integer gender = parseGenderCn(genderCn);
        ParsedCerts parsed = TeacherCertTextParser.parse(certText);
        try {
          boolean isProvisional = !StringUtils.hasText(idCardNumber);
          TrainTeacherBankEntity existing = null;
          if (!isProvisional) {
            existing = findByIdCard(tenantId, idCardNumber);
          } else if (StringUtils.hasText(phone)) {
            existing = findProvisionalByNamePhone(tenantId, name, phone);
          }
          if (existing == null) {
            TrainTeacherBankEntity entity = new TrainTeacherBankEntity();
            entity.setTenantId(tenantId);
            entity.setDeleted(0);
            entity.setStatus(1);
            entity.setSource("excel");
            entity.setName(name);
            entity.setPhone(phone);
            entity.setIdCardNumber(idCardNumber);
            entity.setGender(gender);
            entity.setEducation(blankToNull(education));
            entity.setMajor(blankToNull(major));
            entity.setCertText(blankToNull(certText));
            entity.setQualificationLevel(blankToNull(qualificationLevel));
            entity.setSkillName(blankToNull(skillName));
            entity.setProfessionalTitle(blankToNull(professionalTitle));
            entity.setTeacherQualNo(parsed.teacherQualNo());
            entity.setTitleCertNo(parsed.titleCertNo());
            entity.setSkillCertNo(parsed.skillCertNo());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            mapper.insert(entity);
            created++;
            if (isProvisional) {
              provisional++;
              warnings.add(
                  new TrainTeacherBankImportResultVO.Item(excelRow, name, "临时建档（无身份证）"));
            }
          } else {
            existing.setName(name);
            if (phone != null) {
              existing.setPhone(phone);
            }
            if (idCardNumber != null) {
              existing.setIdCardNumber(idCardNumber);
            }
            if (gender != null) {
              existing.setGender(gender);
            }
            existing.setEducation(blankToNull(education));
            existing.setMajor(blankToNull(major));
            existing.setCertText(blankToNull(certText));
            existing.setQualificationLevel(blankToNull(qualificationLevel));
            existing.setSkillName(blankToNull(skillName));
            existing.setProfessionalTitle(blankToNull(professionalTitle));
            if (parsed.teacherQualNo() != null) {
              existing.setTeacherQualNo(parsed.teacherQualNo());
            }
            if (parsed.titleCertNo() != null) {
              existing.setTitleCertNo(parsed.titleCertNo());
            }
            if (parsed.skillCertNo() != null) {
              existing.setSkillCertNo(parsed.skillCertNo());
            }
            if (!StringUtils.hasText(existing.getSource())) {
              existing.setSource("excel");
            }
            existing.setUpdateTime(LocalDateTime.now());
            mapper.updateById(existing);
            updated++;
            if (!StringUtils.hasText(existing.getIdCardNumber())) {
              provisional++;
            }
          }
          success++;
        } catch (Exception ex) {
          failed.add(
              new TrainTeacherBankImportResultVO.Item(
                  excelRow, name, ex.getMessage() == null ? "导入失败" : ex.getMessage()));
        }
      }
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析 Excel 失败：" + e.getMessage());
    }
    return new TrainTeacherBankImportResultVO(
        total, success, created, updated, provisional, failed, warnings);
  }

  @Transactional
  public TrainTeacherBankVO bindFile(Long teacherId, String fileType, Long fileId) {
    if (!TeacherBankFileTypes.isKnown(fileType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知证照类型");
    }
    if (fileId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "fileId 必填");
    }
    TrainTeacherBankEntity entity = getExisting(teacherId);
    Long tenantId = tenantId();
    Long operatorId = SecurityContext.currentUser().getUserId();
    TrainTeacherBankFileEntity file = new TrainTeacherBankFileEntity();
    file.setTenantId(tenantId);
    file.setTeacherId(teacherId);
    file.setFileType(fileType.trim());
    file.setFileId(fileId);
    file.setSortNo(0);
    file.setDeleted(0);
    file.setCreatedBy(operatorId);
    fileMapper.insert(file);
    setPrimaryFileId(entity, fileType.trim(), fileId, true);
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    return toVo(entity, true);
  }

  @Transactional
  public TrainTeacherBankVO clearFile(Long teacherId, String fileType) {
    if (!TeacherBankFileTypes.isKnown(fileType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知证照类型");
    }
    TrainTeacherBankEntity entity = getExisting(teacherId);
    Long tenantId = tenantId();
    List<TrainTeacherBankFileEntity> list =
        fileMapper.selectList(
            new LambdaQueryWrapper<TrainTeacherBankFileEntity>()
                .eq(TrainTeacherBankFileEntity::getTenantId, tenantId)
                .eq(TrainTeacherBankFileEntity::getTeacherId, teacherId)
                .eq(TrainTeacherBankFileEntity::getFileType, fileType.trim())
                .eq(TrainTeacherBankFileEntity::getDeleted, 0));
    for (TrainTeacherBankFileEntity f : list) {
      fileMapper.deleteById(f.getId());
    }
    setPrimaryFileId(entity, fileType.trim(), null, true);
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    return toVo(entity, true);
  }

  @Transactional(readOnly = true)
  public List<TrainTeacherBankFileVO> listFiles(Long teacherId) {
    getExisting(teacherId);
    return loadFiles(teacherId);
  }

  /**
   * 异库迁移：上传 PHP 导出的 JSON 数组（元素字段名与 train_teacher_bank 列一致）。
   * gender 会从 PHP 口径翻转为 SaaS（0女1男）。
   * 不加整包事务：带图迁移耗时长，按人提交避免超时全量回滚。
   */
  public TrainTeacherBankImportResultVO migrateFromLegacyJson(
      MultipartFile file, boolean migrateImages) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 legacy JSON");
    }
    Long tenantId = tenantId();
    List<TrainTeacherBankImportResultVO.Item> failed = new ArrayList<>();
    List<TrainTeacherBankImportResultVO.Item> warnings = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int provisional = 0;
    int success = 0;
    try {
      JsonNode root = objectMapper.readTree(file.getInputStream());
      if (!root.isArray()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON 根节点必须是数组");
      }
      int row = 0;
      for (JsonNode node : root) {
        row++;
        String name = text(node, "name");
        if (!StringUtils.hasText(name)) {
          failed.add(new TrainTeacherBankImportResultVO.Item(row, name, "姓名为空"));
          continue;
        }
        Long legacyId = longVal(node, "id");
        String idCardNumber = normalizeIdCardNumber(text(node, "id_card_number"));
        try {
          TrainTeacherBankEntity existing = null;
          if (legacyId != null) {
            existing = findByLegacyId(tenantId, legacyId);
          }
          if (existing == null && idCardNumber != null) {
            existing = findByIdCard(tenantId, idCardNumber);
          }
          boolean isNew = existing == null;
          TrainTeacherBankEntity entity = isNew ? new TrainTeacherBankEntity() : existing;
          if (isNew) {
            entity.setTenantId(tenantId);
            entity.setDeleted(0);
            entity.setStatus(1);
            entity.setCreateTime(LocalDateTime.now());
          }
          entity.setSource("legacy");
          entity.setLegacyId(legacyId);
          entity.setName(name.trim());
          entity.setPhone(blankToNull(text(node, "phone")));
          entity.setIdCardNumber(idCardNumber);
          entity.setGender(flipPhpGender(intVal(node, "gender")));
          entity.setBirthday(blankToNull(text(node, "birthday")));
          entity.setNation(blankToNull(text(node, "nation")));
          entity.setEducation(blankToNull(text(node, "education")));
          entity.setMajor(blankToNull(text(node, "major")));
          entity.setStartDate(blankToNull(text(node, "start_date")));
          entity.setEndDate(blankToNull(text(node, "end_date")));
          entity.setCompany(blankToNull(text(node, "company")));
          entity.setAddress(blankToNull(text(node, "address")));
          entity.setUpdateTime(LocalDateTime.now());
          if (isNew) {
            mapper.insert(entity);
            created++;
          } else {
            mapper.updateById(entity);
            updated++;
          }
          if (!StringUtils.hasText(entity.getIdCardNumber())) {
            provisional++;
            warnings.add(
                new TrainTeacherBankImportResultVO.Item(row, name, "临时建档（无身份证）"));
          }
          if (migrateImages) {
            migrateImagesFor(entity, node, warnings, row, name);
          }
          success++;
        } catch (Exception ex) {
          log.warn("migrate teacher row {} failed: {}", row, ex.toString());
          failed.add(
              new TrainTeacherBankImportResultVO.Item(
                  row, name, ex.getMessage() == null ? "迁移失败" : ex.getMessage()));
        }
      }
      return new TrainTeacherBankImportResultVO(
          row, success, created, updated, provisional, failed, warnings);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析 legacy JSON 失败：" + e.getMessage());
    }
  }

  private void migrateImagesFor(
      TrainTeacherBankEntity entity,
      JsonNode node,
      List<TrainTeacherBankImportResultVO.Item> warnings,
      int row,
      String name) {
    Map<String, List<String>> groups = new LinkedHashMap<>();
    groups.put(TeacherBankFileTypes.ID_FRONT, parseUrlList(node.get("id_card")));
    // 第2张起作为反面候选
    List<String> idCards = groups.get(TeacherBankFileTypes.ID_FRONT);
    if (idCards.size() >= 2) {
      groups.put(TeacherBankFileTypes.ID_BACK, List.of(idCards.get(1)));
      groups.put(TeacherBankFileTypes.ID_FRONT, List.of(idCards.get(0)));
    }
    groups.put(TeacherBankFileTypes.GRADUATE, parseUrlList(node.get("graduate_paper")));
    groups.put(TeacherBankFileTypes.EDUCATION, parseUrlList(node.get("edu_license")));
    groups.put(TeacherBankFileTypes.TEACHER_QUAL, parseUrlList(node.get("teacher_license")));
    groups.put(TeacherBankFileTypes.TITLE, parseUrlList(node.get("engineer_license")));
    groups.put(TeacherBankFileTypes.SKILL, parseUrlList(node.get("level_license")));

    int sort = 0;
    for (Map.Entry<String, List<String>> e : groups.entrySet()) {
      for (String url : e.getValue()) {
        if (!StringUtils.hasText(url) || !url.toLowerCase(Locale.ROOT).startsWith("http")) {
          continue;
        }
        if (existsLegacyUrl(entity.getTenantId(), url)) {
          continue;
        }
        try {
          byte[] bytes = download(url);
          if (bytes == null || bytes.length == 0) {
            warnings.add(
                new TrainTeacherBankImportResultVO.Item(row, name, "图片下载为空：" + e.getKey()));
            continue;
          }
          String fname = guessFileName(url, e.getKey());
          FileVO stored =
              fileApplicationService.storeFromBytesForTenant(
                  entity.getTenantId(), bytes, fname, guessContentType(fname), "private");
          TrainTeacherBankFileEntity file = new TrainTeacherBankFileEntity();
          file.setTenantId(entity.getTenantId());
          file.setTeacherId(entity.getId());
          file.setFileType(e.getKey());
          file.setFileId(stored.id());
          file.setLegacyUrl(url);
          file.setSortNo(sort++);
          file.setDeleted(0);
          fileMapper.insert(file);
          setPrimaryFileId(entity, e.getKey(), stored.id(), false);
        } catch (Exception ex) {
          warnings.add(
              new TrainTeacherBankImportResultVO.Item(
                  row, name, "图片转存失败(" + e.getKey() + "): " + ex.getMessage()));
        }
      }
    }
    mapper.updateById(entity);
  }

  private boolean existsLegacyUrl(Long tenantId, String url) {
    Long c =
        fileMapper.selectCount(
            new LambdaQueryWrapper<TrainTeacherBankFileEntity>()
                .eq(TrainTeacherBankFileEntity::getTenantId, tenantId)
                .eq(TrainTeacherBankFileEntity::getLegacyUrl, url)
                .eq(TrainTeacherBankFileEntity::getDeleted, 0));
    return c != null && c > 0;
  }

  private byte[] download(String url) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
    HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
      return resp.body();
    }
    throw new IllegalStateException("HTTP " + resp.statusCode());
  }

  private static String guessFileName(String url, String type) {
    String path = url;
    int q = path.indexOf('?');
    if (q > 0) {
      path = path.substring(0, q);
    }
    int slash = path.lastIndexOf('/');
    String name = slash >= 0 ? path.substring(slash + 1) : path;
    if (!StringUtils.hasText(name) || !name.contains(".")) {
      return type.toLowerCase(Locale.ROOT) + ".jpg";
    }
    return name;
  }

  private static String guessContentType(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".webp")) {
      return "image/webp";
    }
    if (lower.endsWith(".gif")) {
      return "image/gif";
    }
    return "image/jpeg";
  }

  private List<String> parseUrlList(JsonNode node) {
    List<String> out = new ArrayList<>();
    if (node == null || node.isNull()) {
      return out;
    }
    try {
      if (node.isArray()) {
        for (JsonNode n : node) {
          if (n.isTextual() && StringUtils.hasText(n.asText())) {
            out.add(n.asText().trim());
          }
        }
        return out;
      }
      if (node.isTextual()) {
        String raw = node.asText();
        if (!StringUtils.hasText(raw)) {
          return out;
        }
        String t = raw.trim();
        if (t.startsWith("[")) {
          List<String> list = objectMapper.readValue(t, new TypeReference<List<String>>() {});
          for (String s : list) {
            if (StringUtils.hasText(s)) {
              out.add(s.trim());
            }
          }
        } else if (t.startsWith("http")) {
          out.add(t);
        }
      }
    } catch (Exception e) {
      log.debug("parse url list failed: {}", e.toString());
    }
    return out;
  }

  private void setPrimaryFileId(
      TrainTeacherBankEntity entity, String fileType, Long fileId, boolean force) {
    switch (fileType) {
      case TeacherBankFileTypes.ID_FRONT -> {
        if (force || entity.getIdCardFrontFileId() == null) {
          entity.setIdCardFrontFileId(fileId);
        }
      }
      case TeacherBankFileTypes.ID_BACK -> {
        if (force || entity.getIdCardBackFileId() == null) {
          entity.setIdCardBackFileId(fileId);
        }
      }
      case TeacherBankFileTypes.GRADUATE -> {
        if (force || entity.getGraduateFileId() == null) {
          entity.setGraduateFileId(fileId);
        }
      }
      case TeacherBankFileTypes.EDUCATION -> {
        if (force || entity.getEducationFileId() == null) {
          entity.setEducationFileId(fileId);
        }
      }
      case TeacherBankFileTypes.TEACHER_QUAL -> {
        if (force || entity.getTeacherQualFileId() == null) {
          entity.setTeacherQualFileId(fileId);
        }
      }
      case TeacherBankFileTypes.TITLE -> {
        if (force || entity.getTitleCertFileId() == null) {
          entity.setTitleCertFileId(fileId);
        }
      }
      case TeacherBankFileTypes.SKILL -> {
        if (force || entity.getSkillCertFileId() == null) {
          entity.setSkillCertFileId(fileId);
        }
      }
      default -> {}
    }
  }

  private void applyCreate(TrainTeacherBankEntity entity, TrainTeacherBankCreateReq req) {
    entity.setName(req.getName());
    entity.setPhone(req.getPhone());
    entity.setIdCard(req.getIdCard());
    entity.setTeacherLicense(req.getTeacherLicense());
    entity.setEngineerLicense(req.getEngineerLicense());
    entity.setLevelLicense(req.getLevelLicense());
    entity.setEduLicense(req.getEduLicense());
    entity.setGraduatePaper(req.getGraduatePaper());
    entity.setCompany(req.getCompany());
    entity.setAddress(req.getAddress());
    entity.setIdCardNumber(normalizeIdCardNumber(req.getIdCardNumber()));
    entity.setStartDate(req.getStartDate());
    entity.setEndDate(req.getEndDate());
    entity.setBirthday(req.getBirthday());
    entity.setGender(req.getGender());
    entity.setNation(req.getNation());
    entity.setEducation(req.getEducation());
    entity.setMajor(req.getMajor());
    entity.setSkillName(req.getSkillName());
    entity.setQualificationLevel(req.getQualificationLevel());
    entity.setProfessionalTitle(req.getProfessionalTitle());
    entity.setCertText(req.getCertText());
    applyParsedCerts(entity, req.getCertText(), req.getTeacherQualNo(), req.getTitleCertNo(), req.getSkillCertNo());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
  }

  private void applyUpdate(TrainTeacherBankEntity entity, TrainTeacherBankUpdateReq req) {
    if (req.getName() != null) {
      entity.setName(req.getName());
    }
    if (req.getPhone() != null) {
      entity.setPhone(req.getPhone());
    }
    if (req.getIdCard() != null) {
      entity.setIdCard(req.getIdCard());
    }
    if (req.getTeacherLicense() != null) {
      entity.setTeacherLicense(req.getTeacherLicense());
    }
    if (req.getEngineerLicense() != null) {
      entity.setEngineerLicense(req.getEngineerLicense());
    }
    if (req.getLevelLicense() != null) {
      entity.setLevelLicense(req.getLevelLicense());
    }
    if (req.getEduLicense() != null) {
      entity.setEduLicense(req.getEduLicense());
    }
    if (req.getGraduatePaper() != null) {
      entity.setGraduatePaper(req.getGraduatePaper());
    }
    if (req.getCompany() != null) {
      entity.setCompany(req.getCompany());
    }
    if (req.getAddress() != null) {
      entity.setAddress(req.getAddress());
    }
    if (req.getIdCardNumber() != null) {
      entity.setIdCardNumber(normalizeIdCardNumber(req.getIdCardNumber()));
    }
    if (req.getStartDate() != null) {
      entity.setStartDate(req.getStartDate());
    }
    if (req.getEndDate() != null) {
      entity.setEndDate(req.getEndDate());
    }
    if (req.getBirthday() != null) {
      entity.setBirthday(req.getBirthday());
    }
    if (req.getGender() != null) {
      entity.setGender(req.getGender());
    }
    if (req.getNation() != null) {
      entity.setNation(req.getNation());
    }
    if (req.getEducation() != null) {
      entity.setEducation(req.getEducation());
    }
    if (req.getMajor() != null) {
      entity.setMajor(req.getMajor());
    }
    if (req.getSkillName() != null) {
      entity.setSkillName(req.getSkillName());
    }
    if (req.getQualificationLevel() != null) {
      entity.setQualificationLevel(req.getQualificationLevel());
    }
    if (req.getProfessionalTitle() != null) {
      entity.setProfessionalTitle(req.getProfessionalTitle());
    }
    if (req.getCertText() != null) {
      entity.setCertText(req.getCertText());
      applyParsedCerts(
          entity, req.getCertText(), req.getTeacherQualNo(), req.getTitleCertNo(), req.getSkillCertNo());
    } else {
      if (req.getTeacherQualNo() != null) {
        entity.setTeacherQualNo(req.getTeacherQualNo());
      }
      if (req.getTitleCertNo() != null) {
        entity.setTitleCertNo(req.getTitleCertNo());
      }
      if (req.getSkillCertNo() != null) {
        entity.setSkillCertNo(req.getSkillCertNo());
      }
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getUserId() != null) {
      entity.setUserId(req.getUserId());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
  }

  private void applyParsedCerts(
      TrainTeacherBankEntity entity,
      String certText,
      String teacherQualNo,
      String titleCertNo,
      String skillCertNo) {
    ParsedCerts parsed = TeacherCertTextParser.parse(certText);
    entity.setTeacherQualNo(
        StringUtils.hasText(teacherQualNo) ? teacherQualNo : parsed.teacherQualNo());
    entity.setTitleCertNo(StringUtils.hasText(titleCertNo) ? titleCertNo : parsed.titleCertNo());
    entity.setSkillCertNo(StringUtils.hasText(skillCertNo) ? skillCertNo : parsed.skillCertNo());
  }

  private TrainTeacherBankVO toVo(TrainTeacherBankEntity e, boolean withFiles) {
    String idCardNumber = normalizeIdCardNumber(e.getIdCardNumber());
    boolean provisional = !StringUtils.hasText(idCardNumber);
    int ready = 0;
    if (e.getIdCardFrontFileId() != null) ready++;
    if (e.getIdCardBackFileId() != null) ready++;
    if (e.getGraduateFileId() != null) ready++;
    if (e.getEducationFileId() != null) ready++;
    if (e.getTeacherQualFileId() != null) ready++;
    if (e.getTitleCertFileId() != null) ready++;
    if (e.getSkillCertFileId() != null) ready++;
    List<TrainTeacherBankFileVO> files = withFiles ? loadFiles(e.getId()) : List.of();
    return new TrainTeacherBankVO(
        e.getId(),
        e.getName(),
        e.getPhone(),
        e.getIdCard(),
        e.getTeacherLicense(),
        e.getEngineerLicense(),
        e.getLevelLicense(),
        e.getEduLicense(),
        e.getGraduatePaper(),
        e.getCompany(),
        e.getAddress(),
        idCardNumber,
        e.getStartDate(),
        e.getEndDate(),
        e.getBirthday(),
        e.getGender(),
        e.getNation(),
        e.getEducation(),
        e.getMajor(),
        e.getSkillName(),
        e.getQualificationLevel(),
        e.getProfessionalTitle(),
        e.getCertText(),
        e.getTeacherQualNo(),
        e.getTitleCertNo(),
        e.getSkillCertNo(),
        e.getIdCardFrontFileId(),
        e.getIdCardBackFileId(),
        e.getGraduateFileId(),
        e.getEducationFileId(),
        e.getTeacherQualFileId(),
        e.getTitleCertFileId(),
        e.getSkillCertFileId(),
        e.getLegacyId(),
        e.getSource(),
        e.getStatus(),
        e.getTenantId(),
        e.getUserId(),
        e.getSortNumber(),
        e.getCreateTime(),
        e.getUpdateTime(),
        provisional,
        ready,
        files);
  }

  private List<TrainTeacherBankFileVO> loadFiles(Long teacherId) {
    return fileMapper
        .selectList(
            new LambdaQueryWrapper<TrainTeacherBankFileEntity>()
                .eq(TrainTeacherBankFileEntity::getTeacherId, teacherId)
                .eq(TrainTeacherBankFileEntity::getDeleted, 0)
                .orderByAsc(TrainTeacherBankFileEntity::getSortNo)
                .orderByAsc(TrainTeacherBankFileEntity::getId))
        .stream()
        .map(
            f ->
                new TrainTeacherBankFileVO(
                    f.getId(),
                    f.getFileType(),
                    f.getFileId(),
                    f.getLegacyUrl(),
                    f.getCertNo(),
                    f.getSortNo(),
                    f.getRemark()))
        .toList();
  }

  private TrainTeacherBankEntity findByIdCard(Long tenantId, String idCardNumber) {
    return mapper.selectOne(
        new LambdaQueryWrapper<TrainTeacherBankEntity>()
            .eq(TrainTeacherBankEntity::getTenantId, tenantId)
            .eq(TrainTeacherBankEntity::getIdCardNumber, idCardNumber)
            .eq(TrainTeacherBankEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private TrainTeacherBankEntity findByLegacyId(Long tenantId, Long legacyId) {
    return mapper.selectOne(
        new LambdaQueryWrapper<TrainTeacherBankEntity>()
            .eq(TrainTeacherBankEntity::getTenantId, tenantId)
            .eq(TrainTeacherBankEntity::getLegacyId, legacyId)
            .eq(TrainTeacherBankEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private TrainTeacherBankEntity findProvisionalByNamePhone(
      Long tenantId, String name, String phone) {
    return mapper.selectOne(
        new LambdaQueryWrapper<TrainTeacherBankEntity>()
            .eq(TrainTeacherBankEntity::getTenantId, tenantId)
            .eq(TrainTeacherBankEntity::getName, name)
            .eq(TrainTeacherBankEntity::getPhone, phone)
            .and(w -> w.isNull(TrainTeacherBankEntity::getIdCardNumber).or().eq(TrainTeacherBankEntity::getIdCardNumber, ""))
            .eq(TrainTeacherBankEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private TrainTeacherBankEntity getExisting(Long id) {
    Long tenantId = tenantId();
    TrainTeacherBankEntity entity =
        mapper.selectOne(
            new LambdaQueryWrapper<TrainTeacherBankEntity>()
        .eq(TrainTeacherBankEntity::getTenantId, tenantId)
        .eq(TrainTeacherBankEntity::getId, id)
        .eq(TrainTeacherBankEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "师资不存在");
    }
    return entity;
  }

  private Long tenantId() {
    Long t = SecurityContext.currentUser().getTenantId();
    if (t == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户");
    }
    return t;
  }

  private static int findHeaderRow(Sheet sheet, DataFormatter fmt) {
    int max = Math.min(sheet.getLastRowNum(), 10);
    for (int i = 0; i <= max; i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue;
      }
      String joined = "";
      for (Cell c : row) {
        joined += fmt.formatCellValue(c);
      }
      if (joined.contains("姓名") && joined.contains("身份证")) {
        return i;
      }
    }
    return -1;
  }

  private static Map<String, Integer> mapHeader(Row row, DataFormatter fmt) {
    Map<String, Integer> map = new HashMap<>();
    if (row == null) {
      return map;
    }
    for (Cell c : row) {
      String v = fmt.formatCellValue(c);
      if (StringUtils.hasText(v)) {
        map.put(v.trim(), c.getColumnIndex());
      }
    }
    // aliases
    if (!map.containsKey("身份证号码") && map.containsKey("身份证号")) {
      map.put("身份证号码", map.get("身份证号"));
    }
    return map;
  }

  private static String cell(Row row, Integer idx, DataFormatter fmt) {
    if (row == null || idx == null) {
      return null;
    }
    Cell c = row.getCell(idx);
    if (c == null) {
      return null;
    }
    String v = fmt.formatCellValue(c);
    return StringUtils.hasText(v) ? v.trim() : null;
  }

  private static Integer parseGenderCn(String cn) {
    if (!StringUtils.hasText(cn)) {
      return null;
    }
    if (cn.contains("男")) {
      return 1;
    }
    if (cn.contains("女")) {
      return 0;
    }
    return null;
  }

  /** PHP: 0男1女 → SaaS: 1男0女 */
  private static Integer flipPhpGender(Integer phpGender) {
    if (phpGender == null) {
      return null;
    }
    if (phpGender == 0) {
      return 1;
    }
    if (phpGender == 1) {
      return 0;
    }
    return phpGender;
  }

  private static String text(JsonNode node, String field) {
    JsonNode n = node.get(field);
    if (n == null || n.isNull()) {
      return null;
    }
    return n.asText();
  }

  private static Long longVal(JsonNode node, String field) {
    JsonNode n = node.get(field);
    if (n == null || n.isNull() || !n.isNumber()) {
      return null;
    }
    return n.asLong();
  }

  private static Integer intVal(JsonNode node, String field) {
    JsonNode n = node.get(field);
    if (n == null || n.isNull() || !n.isNumber()) {
      return null;
    }
    return n.asInt();
  }

  private static String blankToNull(String s) {
    return StringUtils.hasText(s) ? s.trim() : null;
  }

  /** 合法 15/18 位证号才入库；拒绝「无身份证图片」等占位文案 */
  private static String normalizeIdCardNumber(String raw) {
    String s = blankToNull(raw);
    if (s == null) {
      return null;
    }
    if (s.matches("(?i)^[0-9Xx]{15}$") || s.matches("(?i)^[0-9Xx]{18}$")) {
      return s.toUpperCase(Locale.ROOT);
    }
    return null;
  }
}
