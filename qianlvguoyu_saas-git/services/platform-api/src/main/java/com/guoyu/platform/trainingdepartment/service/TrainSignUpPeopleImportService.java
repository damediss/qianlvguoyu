package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.SignUpPeopleImportResultVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 报名人员 Excel 批量导入（对齐 PHP peopleselfimportAction） */
@Service
@Slf4j
public class TrainSignUpPeopleImportService {

  private static final String IMPORT_TEMPLATE_REL = "template/报名批量导入模板.xlsx";
  private static final String IMPORT_TEMPLATE_CLASSPATH = "templates/sign-up-people-import-template.xlsx";

  private static final String[] PREV_LEVEL_LABELS =
      {"五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"};

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final PlatformUserMapper platformUserMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final ObjectMapper objectMapper;
  private final String cdnPath;

  public TrainSignUpPeopleImportService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      PlatformUserMapper platformUserMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      OrgMapper orgMapper,
      UserMapper userMapper,
      TrainingOperatorScopeService operatorScopeService,
      ObjectMapper objectMapper,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.platformUserMapper = platformUserMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.operatorScopeService = operatorScopeService;
    this.objectMapper = objectMapper;
    this.cdnPath = cdnPath;
  }

  /** 下载 Excel 批量导入模板（对齐 PHP cdn template/报名批量导入模板.xlsx） */
  public ResponseEntity<InputStreamResource> downloadImportTemplate() throws IOException {
    InputStreamResource resource = new InputStreamResource(openImportTemplateStream());
    String filename = "报名批量导入模板.xlsx";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build()
            .toString())
        .body(resource);
  }

  private InputStream openImportTemplateStream() throws IOException {
    Path cdnTemplate = Paths.get(cdnPath, IMPORT_TEMPLATE_REL).normalize();
    Path cdnRoot = Paths.get(cdnPath).toAbsolutePath().normalize();
    if (cdnTemplate.startsWith(cdnRoot) && Files.exists(cdnTemplate)) {
      return Files.newInputStream(cdnTemplate);
    }
    ClassPathResource bundled = new ClassPathResource(IMPORT_TEMPLATE_CLASSPATH);
    if (bundled.exists()) {
      return bundled.getInputStream();
    }
    throw new BusinessException(ErrorCode.NOT_FOUND, "导入模板不存在");
  }

  @Transactional
  public SignUpPeopleImportResultVO importExcel(Long signUpId, MultipartFile file) {
    if (signUpId == null || signUpId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参数不完整");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不合法");
    }
    String originalName = file.getOriginalFilename();
    if (!StringUtils.hasText(originalName)
        || (!originalName.toLowerCase().endsWith(".xlsx") && !originalName.toLowerCase().endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能上传 xlsx/xls 文件");
    }

    TrainSignUpSelfEntity signUp = getSignUp(signUpId);
    OrgEntity org = signUp.getOrgId() != null && signUp.getOrgId() > 0
        ? orgMapper.selectById(signUp.getOrgId()) : null;
    String orgTitle = org != null && StringUtils.hasText(org.getName()) ? org.getName() : "";

    String storedPath = saveUpload(file);
    Path fullPath = Paths.get(cdnPath, storedPath);
    List<List<String>> rows = parseSheet(fullPath);

    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long platformOrgId = resolvePlatformOrgId();
    int inserted = 0;
    int skipped = 0;
    int invalid = 0;
    List<String> errors = new ArrayList<>();

    int rowIdx = 0;
    for (List<String> row : rows) {
      rowIdx++;
      if (rowIdx == 1) {
        continue;
      }
      String idCard = normalize(cell(row, 2));
      if (!StringUtils.hasText(idCard)) {
        break;
      }
      invalid++;
      String phone = normalize(cell(row, 5));
      String realname = normalize(cell(row, 1));

      if (idCard.length() != 18) {
        errors.add("第" + rowIdx + "行：身份证号不是18位");
        skipped++;
        continue;
      }
      if (phone.length() != 11) {
        errors.add("第" + rowIdx + "行：手机号不是11位");
        skipped++;
        continue;
      }

      TrainSignUpPeopleSelfEntity exists = peopleMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard)
              .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
      if (exists != null) {
        skipped++;
        continue;
      }

      TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
      entity.setSignUpId(signUpId);
      entity.setTenantId(tenantId);
      entity.setIdCardNumber(idCard);
      entity.setName(realname);
      entity.setAddress(cell(row, 3));
      entity.setPhone(phone);
      entity.setNation(cell(row, 12));
      entity.setPolitical(cell(row, 13));
      entity.setSchool(cell(row, 9));
      entity.setEducation(cell(row, 6));
      entity.setMajor(cell(row, 7));
      entity.setSchoolNo(cell(row, 8));
      entity.setGraduateDate(cell(row, 10));
      entity.setJog(cell(row, 15));
      entity.setWorkTime(cell(row, 11));
      entity.setWorkDateStart(cell(row, 17));
      entity.setWorkDateEnd(cell(row, 18));
      entity.setStatus(0);
      entity.setCompanyStatus(0);
      entity.setHasPay(0);
      entity.setDeleted(0);
      entity.setCreateTime(LocalDateTime.now());
      entity.setUpdateTime(LocalDateTime.now());

      String workDateStart = cell(row, 17);
      String workDateEnd = cell(row, 18);
      if (StringUtils.hasText(workDateStart) && StringUtils.hasText(workDateEnd)) {
        entity.setWorkExp(buildWorkExpJson(workDateStart, workDateEnd, orgTitle, cell(row, 15),
            cell(row, 19), cell(row, 20)));
      }

      String prevWorkType = cell(row, 21);
      if (StringUtils.hasText(prevWorkType)) {
        entity.setWorkTypePaperName(prevWorkType);
      }
      Integer prevLevel = parsePrevLevel(cell(row, 22));
      if (prevLevel != null) {
        entity.setWorkTypePaperLevel(prevLevel);
      }
      String prevPaperNo = cell(row, 23);
      if (StringUtils.hasText(prevPaperNo)) {
        entity.setWorkTypePaperNo(prevPaperNo);
        entity.setWorkTypePaperHas(1);
      }

      PlatformUserEntity platformUser = platformUserMapper.selectOne(
          new LambdaQueryWrapper<PlatformUserEntity>()
              .eq(PlatformUserEntity::getIdcard, idCard)
              .eq(PlatformUserEntity::getDeleted, 0));
      if (platformUser != null) {
        platformUser.setOrgId(platformOrgId);
        platformUserMapper.updateById(platformUser);
        entity.setPlatformUserId(platformUser.getId());
        entity.setName(platformUser.getRealname());
        entity.setPhone(platformUser.getPhone());
        mergeFromUserDetail(entity, idCard);
      } else {
        Long userId = createPlatformUser(realname, idCard, phone, platformOrgId, signUp.getOrgId(), row);
        if (userId != null) {
          entity.setPlatformUserId(userId);
        }
        entity.setAge(calcAgeFromIdCard(idCard));
      }

      peopleMapper.insert(entity);
      inserted++;
    }

    appendSignUpFile(signUp, storedPath);
    return new SignUpPeopleImportResultVO(inserted, skipped, invalid, storedPath, errors);
  }

  @Transactional
  public SignUpPeopleImportResultVO importFromStudents(Long signUpId, List<Long> ids) {
    if (signUpId == null || signUpId <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参数错误");
    }
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择导入的学员");
    }

    getSignUp(signUpId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long platformOrgId = resolvePlatformOrgId();

    List<PlatformUserEntity> users = platformUserMapper.selectList(
        new LambdaQueryWrapper<PlatformUserEntity>()
            .in(PlatformUserEntity::getId, ids)
            .eq(PlatformUserEntity::getOrgId, platformOrgId)
            .eq(PlatformUserEntity::getDeleted, 0));
    if (users.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到可导入的学员");
    }

    List<String> idCards = users.stream()
        .map(PlatformUserEntity::getIdcard)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    Map<String, PlatformUserDetailEntity> detailMap = idCards.isEmpty()
        ? Map.of()
        : platformUserDetailMapper.selectList(
            new LambdaQueryWrapper<PlatformUserDetailEntity>()
                .in(PlatformUserDetailEntity::getIdcard, idCards)
                .eq(PlatformUserDetailEntity::getDeleted, 0))
            .stream()
            .collect(Collectors.toMap(
                PlatformUserDetailEntity::getIdcard,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new));

    int inserted = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (PlatformUserEntity user : users) {
      if (!StringUtils.hasText(user.getIdcard())) {
        errors.add("学员" + safeName(user.getRealname()) + "没有身份证号");
        skipped++;
        continue;
      }

      TrainSignUpPeopleSelfEntity exists = peopleMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, user.getIdcard())
              .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
      if (exists != null) {
        skipped++;
        continue;
      }

      TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
      entity.setSignUpId(signUpId);
      entity.setTenantId(tenantId);
      entity.setPlatformUserId(user.getId());
      entity.setIdCardNumber(user.getIdcard());
      entity.setName(user.getRealname());
      entity.setPhone(user.getPhone());
      entity.setStatus(0);
      entity.setCompanyStatus(0);
      entity.setHasPay(0);
      entity.setDeleted(0);
      entity.setCreateTime(now);
      entity.setUpdateTime(now);

      PlatformUserDetailEntity detail = detailMap.get(user.getIdcard());
      if (detail != null) {
        applyDetailFields(entity, detail);
      } else {
        entity.setAge(calcAgeFromIdCard(user.getIdcard()));
      }

      peopleMapper.insert(entity);
      inserted++;
    }

    return new SignUpPeopleImportResultVO(inserted, skipped, users.size(), null, errors);
  }

  private void applyDetailFields(TrainSignUpPeopleSelfEntity entity, PlatformUserDetailEntity detail) {
    entity.setAge(calcAgeFromIdCard(entity.getIdCardNumber()));
    if (StringUtils.hasText(detail.getAddress())) {
      entity.setAddress(detail.getAddress());
    }
    if (StringUtils.hasText(detail.getNation())) {
      entity.setNation(detail.getNation());
    }
    if (StringUtils.hasText(detail.getPolitical())) {
      entity.setPolitical(detail.getPolitical());
    }
    if (StringUtils.hasText(detail.getEducation())) {
      entity.setEducation(detail.getEducation());
    }
    if (StringUtils.hasText(detail.getIdcardHeader())) {
      entity.setIdCardHeader(detail.getIdcardHeader());
      entity.setIdCardHeader2(detail.getIdcardHeader());
    }
  }

  private static String safeName(String name) {
    return StringUtils.hasText(name) ? name : "";
  }

  private TrainSignUpSelfEntity getSignUp(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = signUpMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名计划不存在了");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
    }
    return entity;
  }

  private Long resolvePlatformOrgId() {
    UserEntity user = userMapper.selectById(SecurityContext.currentUser().getUserId());
    if (user != null && user.getOrgId() != null) {
      return user.getOrgId();
    }
    return 0L;
  }

  private String saveUpload(MultipartFile file) {
    try {
      Path dir = Paths.get(cdnPath).toAbsolutePath().normalize();
      Files.createDirectories(dir);
      String orig = file.getOriginalFilename();
      String ext = "";
      if (orig != null && orig.contains(".")) {
        ext = orig.substring(orig.lastIndexOf('.'));
      }
      String name = "sign_up_import/" + UUID.randomUUID() + ext;
      Path target = dir.resolve(name);
      Files.createDirectories(target.getParent());
      file.transferTo(target);
      return name.replace("\\", "/");
    } catch (IOException e) {
      log.error("保存导入文件失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
    }
  }

  private List<List<String>> parseSheet(Path fullPath) {
    try (InputStream is = Files.newInputStream(fullPath)) {
      Workbook workbook = WorkbookFactory.create(is);
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter fmt = new DataFormatter();
      List<List<String>> rows = new ArrayList<>();
      int lastRow = sheet.getLastRowNum();
      for (int i = 0; i <= lastRow; i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          rows.add(List.of());
          continue;
        }
        List<String> cells = new ArrayList<>();
        int lastCol = Math.max(row.getLastCellNum(), 24);
        for (int j = 0; j < lastCol; j++) {
          var cell = row.getCell(j);
          cells.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
        }
        rows.add(cells);
      }
      workbook.close();
      return rows;
    } catch (Exception e) {
      log.error("Excel 解析失败: {}", fullPath, e);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel解析失败");
    }
  }

  private void mergeFromUserDetail(TrainSignUpPeopleSelfEntity entity, String idCard) {
    PlatformUserDetailEntity detail = platformUserDetailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idCard)
            .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (detail == null) {
      entity.setAge(calcAgeFromIdCard(idCard));
      return;
    }
    applyDetailFields(entity, detail);
  }

  private Long createPlatformUser(
      String realname,
      String idCard,
      String phone,
      Long orgId,
      Long companyId,
      List<String> row) {
    PlatformUserEntity entity = new PlatformUserEntity();
    entity.setRealname(realname);
    entity.setIdcard(idCard);
    entity.setPhone(phone);
    entity.setOrgId(orgId);
    entity.setCompanyId(companyId);
    entity.setAddtime(System.currentTimeMillis() / 1000);
    entity.setDeleted(0);
    String stat = randomStat();
    entity.setStat(stat);
    entity.setPassword(md5("123123", stat));
    platformUserMapper.insert(entity);

    PlatformUserDetailEntity exists = platformUserDetailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idCard)
            .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (exists == null) {
      PlatformUserDetailEntity detail = new PlatformUserDetailEntity();
      detail.setUserId(entity.getId());
      detail.setRealname(realname);
      detail.setPhone(phone);
      detail.setIdcard(idCard);
      detail.setBirthday(idCard.substring(6, 14));
      detail.setGender(genderFromIdCard(idCard));
      detail.setNation(cell(row, 12));
      detail.setAddress(cell(row, 3));
      detail.setPolitical(cell(row, 13));
      detail.setEducation(cell(row, 6));
      detail.setDeleted(0);
      platformUserDetailMapper.insert(detail);
    }
    return entity.getId();
  }

  private void appendSignUpFile(TrainSignUpSelfEntity signUp, String storedPath) {
    List<String> files = new ArrayList<>();
    if (StringUtils.hasText(signUp.getFiles())) {
      try {
        files.addAll(objectMapper.readValue(signUp.getFiles(), new TypeReference<List<String>>() {}));
      } catch (Exception ignored) {
        files = new ArrayList<>();
      }
    }
    files.add(storedPath);
    try {
      signUp.setFiles(objectMapper.writeValueAsString(files));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新方案附件失败");
    }
    signUp.setUpdateTime(LocalDateTime.now());
    signUpMapper.updateById(signUp);
  }

  private String buildWorkExpJson(
      String startDate,
      String endDate,
      String company,
      String position,
      String proveName,
      String provePhone) {
    try {
      Map<String, String> item = new LinkedHashMap<>();
      item.put("startDate", startDate);
      item.put("endDate", endDate);
      item.put("company", company);
      item.put("position", position);
      item.put("proveName", proveName);
      item.put("provePhone", provePhone);
      return objectMapper.writeValueAsString(List.of(item));
    } catch (Exception e) {
      return "[]";
    }
  }

  private Integer parsePrevLevel(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      int idx = Integer.parseInt(raw.trim());
      if (idx >= 0 && idx < PREV_LEVEL_LABELS.length) {
        return idx;
      }
    } catch (NumberFormatException ignored) {
      for (int i = 0; i < PREV_LEVEL_LABELS.length; i++) {
        if (PREV_LEVEL_LABELS[i].equals(raw.trim())) {
          return i;
        }
      }
    }
    return null;
  }

  private static String cell(List<String> row, int index) {
    if (index < 0 || index >= row.size()) {
      return "";
    }
    String value = row.get(index);
    return value == null ? "" : value.trim();
  }

  private static String normalize(String value) {
    return value == null ? "" : value.replace(" ", "");
  }

  private static Integer calcAgeFromIdCard(String idCard) {
    try {
      String birthRaw;
      if (idCard.length() == 18) {
        birthRaw = idCard.substring(6, 14);
      } else if (idCard.length() == 15) {
        birthRaw = "19" + idCard.substring(6, 12);
      } else {
        return null;
      }
      LocalDate birth = LocalDate.parse(birthRaw, DateTimeFormatter.BASIC_ISO_DATE);
      return Period.between(birth, LocalDate.now()).getYears();
    } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
      return null;
    }
  }

  private static Integer genderFromIdCard(String idCard) {
    if (idCard.length() < 2) {
      return 0;
    }
    int digit = Character.getNumericValue(idCard.charAt(idCard.length() - 2));
    return digit % 2 > 0 ? 0 : 1;
  }

  private static String randomStat() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 4);
  }

  private static String md5(String password, String stat) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest((password + stat).getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "密码加密失败");
    }
  }
}
