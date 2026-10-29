package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleSummaryExcelSupport;
import com.guoyu.platform.trainingdepartment.support.SignUpSelfFilesResolver;
import com.guoyu.platform.trainingdepartment.support.TrainingWxMiniQrcodeClient;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfListVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 信息收集（对齐 yun_shang information.vue） */
@Service
@Slf4j
public class TrainSignUpCollectionApplicationService {

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final TrainInfoClassMapper classMapper;
  private final OrgMapper orgMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final TrainingWxMiniQrcodeClient wxMiniQrcodeClient;
  private final QrCodeGenerationService qrCodeGenerationService;
  private final SignUpSelfFilesResolver filesResolver;
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private final SignUpPeopleSummaryExcelSupport peopleSummaryExcelSupport;
  private final String cdnPath;
  private final String cdnUrl;
  private final String appBaseUrl;

  public TrainSignUpCollectionApplicationService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      TrainInfoClassMapper classMapper,
      OrgMapper orgMapper,
      TrainingOperatorScopeService operatorScopeService,
      TrainingWxMiniQrcodeClient wxMiniQrcodeClient,
      QrCodeGenerationService qrCodeGenerationService,
      SignUpSelfFilesResolver filesResolver,
      ClassCollectionApplicationService classCollectionApplicationService,
      SignUpPeopleSummaryExcelSupport peopleSummaryExcelSupport,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.app-base-url:http://localhost:8080}") String appBaseUrl) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.classMapper = classMapper;
    this.orgMapper = orgMapper;
    this.operatorScopeService = operatorScopeService;
    this.wxMiniQrcodeClient = wxMiniQrcodeClient;
    this.qrCodeGenerationService = qrCodeGenerationService;
    this.filesResolver = filesResolver;
    this.classCollectionApplicationService = classCollectionApplicationService;
    this.peopleSummaryExcelSupport = peopleSummaryExcelSupport;
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpSelfListVO> pageSignUps(TrainSignUpSelfQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .orderByAsc(TrainSignUpSelfEntity::getSortNumber)
        .orderByDesc(TrainSignUpSelfEntity::getCreateTime);
    java.util.Set<Long> companyScope =
        operatorScopeService.resolveCompanyIdScope(req.getOrgId());
    if (companyScope != null) {
      if (companyScope.isEmpty()) {
        wrapper.eq(TrainSignUpSelfEntity::getOrgId, -1L);
      } else {
        wrapper.in(TrainSignUpSelfEntity::getOrgId, companyScope);
      }
    }
    if (req.getClassId() != null) {
      wrapper.eq(TrainSignUpSelfEntity::getTrainClassId, req.getClassId())
          .eq(TrainSignUpSelfEntity::getCollectionSource, ClassCollectionApplicationService.SOURCE_CLASS_PREP);
    } else if (StringUtils.hasText(req.getCollectionSource())) {
      wrapper.eq(TrainSignUpSelfEntity::getCollectionSource, req.getCollectionSource().trim());
    } else {
      // 旧「信息收集」列表：不含调研与按班收集
      wrapper.and(w -> w
          .eq(TrainSignUpSelfEntity::getCollectionSource, "train_info")
          .or()
          .isNull(TrainSignUpSelfEntity::getCollectionSource));
    }
    if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainSignUpSelfEntity::getTrainInfoId, req.getTrainInfoId());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpSelfEntity::getTitle, req.getKeyword());
    }
    Page<TrainSignUpSelfEntity> page = signUpMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<Long> signUpIds = page.getRecords().stream().map(TrainSignUpSelfEntity::getId).toList();
    Map<Long, Long> peopleCounts = countPeopleBySignUp(signUpIds);
    Map<Long, String> orgNames = resolveOrgNames(page.getRecords());
    List<TrainSignUpSelfListVO> items = page.getRecords().stream()
        .map(e -> toListVO(e, peopleCounts.getOrDefault(e.getId(), 0L), orgNames.get(e.getOrgId())))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> resolveQrcode(Long signUpId) {
    TrainSignUpSelfEntity signUp = getSignUp(signUpId);
    Map<String, Object> result = buildQrcodeResult(signUpId, signUp.getTitle());
    
    // 生成 APP 扫码二维码（服务端生成，保存到 CDN）
    // 内容需含 pages/research/Saasdetail?id=，供 App scanRouter 识别并跳原生页
    try {
      String appSignUpUrl = appBaseUrl + "/#/pages/research/Saasdetail?id=" + signUpId;
      String appQrCodeFileName = "app_signup_" + signUpId;
      // 每次重新生成以确保二维码内容与当前 URL 一致
      if (qrCodeGenerationService.qrCodeExists(appQrCodeFileName)) {
        qrCodeGenerationService.deleteQrCode(appQrCodeFileName);
      }
      String appQrCodeUrl = qrCodeGenerationService.generateAndSaveQrCode(appSignUpUrl, appQrCodeFileName);
      result.put("appQrcode", appQrCodeUrl);
      result.put("appQrcodeDataUrl", qrCodeGenerationService.generateQrCodeDataUrl(appSignUpUrl));
    } catch (Exception e) {
      log.warn("生成 APP 二维码失败: {}", e.getMessage());
      result.put("appQrcode", "");
      result.put("appQrcodeDataUrl", "");
    }
    
    return result;
  }

  @Transactional
  public Map<String, Object> generateQrcode(Long signUpId) {
    TrainSignUpSelfEntity signUp = getSignUp(signUpId);
    Path file = qrcodeFilePath(signUpId);
    if (Files.exists(file)) {
      return buildQrcodeResult(signUpId, signUp.getTitle());
    }
    byte[] png = wxMiniQrcodeClient.createSignUpSelfQrcode(signUpId);
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, png);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存二维码失败");
    }
    if (!Files.exists(file)) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成失败");
    }
    return buildQrcodeResult(signUpId, signUp.getTitle());
  }

  @Transactional
  public int batchUpdatePeople(List<Long> ids, String field, Integer value) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择人员");
    }
    if (!"status".equals(field) && !"deleted".equals(field) && !"has_pay".equals(field) && !"company_status".equals(field)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的批量字段");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    // 逻辑删除必须走 delete：UpdateWrapper.set(deleted) 在 @TableLogic 下常被忽略，表现为「删除成功但仍可见」
    if ("deleted".equals(field)) {
      int deleted = peopleMapper.delete(new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
          .in(TrainSignUpPeopleSelfEntity::getId, ids)
          .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId));
      if (deleted <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "未删除任何人员，请刷新后重试");
      }
      return deleted;
    }
    LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity>()
        .in(TrainSignUpPeopleSelfEntity::getId, ids)
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId);
    if ("status".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getStatus, value != null ? value : 0);
    } else if ("has_pay".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getHasPay, value != null ? value : 0);
    } else if ("company_status".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getCompanyStatus, value != null ? value : 0);
    }
    wrapper.set(TrainSignUpPeopleSelfEntity::getUpdateTime, LocalDateTime.now());
    int updated = peopleMapper.update(null, wrapper);
    if ("status".equals(field) && value != null && value == ClassCollectionApplicationService.STATUS_APPROVED) {
      classCollectionApplicationService.syncApprovedPeopleToRoster(ids);
    }
    return updated;
  }

  @Transactional(readOnly = true)
  public PeopleRosterExcel exportPeopleExcel(Long signUpId, String ids) {
    TrainSignUpSelfEntity signUp = getSignUp(signUpId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<Long> idList = parseCsvIds(ids);
    List<TrainSignUpPeopleSelfEntity> people = peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .in(!idList.isEmpty(), TrainSignUpPeopleSelfEntity::getId, idList)
            .orderByDesc(TrainSignUpPeopleSelfEntity::getId));
    String companyName = "";
    if (signUp.getOrgId() != null) {
      OrgEntity org = orgMapper.selectById(signUp.getOrgId());
      if (org != null && StringUtils.hasText(org.getName())) {
        companyName = org.getName();
      }
    }
    byte[] body = peopleSummaryExcelSupport.write(companyName, people.stream().map(this::toRosterPerson).toList());
    String fileName = SignUpPeopleSummaryExcelSupport.peopleRosterFileName(
        resolveClassTitle(signUp), signUpId);
    return new PeopleRosterExcel(body, fileName);
  }

  private String resolveClassTitle(TrainSignUpSelfEntity signUp) {
    if (signUp.getTrainClassId() != null) {
      TrainInfoClassEntity clazz = classMapper.selectById(signUp.getTrainClassId());
      if (clazz != null && StringUtils.hasText(clazz.getTitle())) {
        return clazz.getTitle().trim();
      }
    }
    String title = signUp.getTitle();
    if (!StringUtils.hasText(title)) {
      return "班级";
    }
    String t = title.trim();
    if (t.endsWith("-资料收集") && t.length() > 5) {
      return t.substring(0, t.length() - "-资料收集".length());
    }
    return t;
  }

  private SignUpPeopleSummaryExcelSupport.RosterPerson toRosterPerson(TrainSignUpPeopleSelfEntity p) {
    SignUpPeopleSummaryExcelSupport.RosterPerson row = new SignUpPeopleSummaryExcelSupport.RosterPerson();
    row.name = p.getName();
    row.idCardNumber = p.getIdCardNumber();
    row.phone = p.getPhone();
    row.age = p.getAge();
    row.companyName = p.getCompanyName();
    row.industrialPark = p.getIndustrialPark();
    row.workTypeName = p.getWorkTypeName();
    row.level = p.getLevel();
    row.workTypeId = p.getWorkTypeId();
    row.paperNo = p.getPaperNo();
    row.workTypePaperHas = p.getWorkTypePaperHas();
    row.workTypePaper = p.getWorkTypePaper();
    row.education = p.getEducation();
    row.titleType = p.getTitleType();
    row.titleLevel = p.getTitleLevel();
    row.school = p.getSchool();
    row.major = p.getMajor();
    row.schoolNo = p.getSchoolNo();
    row.inSchoolDate = p.getInSchoolDate();
    row.graduateDate = p.getGraduateDate();
    row.workTime = p.getWorkTime();
    row.createTime = p.getCreateTime();
    row.declarationCondition = p.getDeclarationCondition();
    row.workExp = p.getWorkExp();
    row.address = p.getAddress();
    row.workDateStart = p.getWorkDateStart();
    row.workDateEnd = p.getWorkDateEnd();
    row.job = p.getJob();
    row.jog = p.getJog();
    row.idCardFront = p.getIdCardFront();
    row.idCardBack = p.getIdCardBack();
    row.idCardHeader = p.getIdCardHeader();
    row.schoolPaper = p.getSchoolPaper();
    row.contactFirstPage = p.getContactFirstPage();
    row.contactExpirePage = p.getContactExpirePage();
    row.contactSignPage = p.getContactSignPage();
    return row;
  }

  private static List<Long> parseCsvIds(String ids) {
    List<Long> result = new ArrayList<>();
    if (!StringUtils.hasText(ids)) {
      return result;
    }
    for (String part : ids.split(",")) {
      String t = part.trim();
      if (!StringUtils.hasText(t)) {
        continue;
      }
      try {
        result.add(Long.parseLong(t));
      } catch (NumberFormatException ignored) {
        // skip invalid token
      }
    }
    return result;
  }

  private TrainSignUpSelfEntity getSignUp(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = signUpMapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名计划不存在");
    }
    if (entity.getOrgId() != null) {
      operatorScopeService.assertCompanyAccessible(entity.getOrgId());
    }
    return entity;
  }

  private Map<Long, Long> countPeopleBySignUp(List<Long> signUpIds) {
    if (signUpIds.isEmpty()) {
      return Map.of();
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainSignUpPeopleSelfEntity> rows = peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .in(TrainSignUpPeopleSelfEntity::getSignUpId, signUpIds)
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    return rows.stream().collect(Collectors.groupingBy(TrainSignUpPeopleSelfEntity::getSignUpId, Collectors.counting()));
  }

  private Map<Long, String> resolveOrgNames(List<TrainSignUpSelfEntity> signUps) {
    List<Long> orgIds = signUps.stream()
        .map(TrainSignUpSelfEntity::getOrgId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (orgIds.isEmpty()) {
      return Map.of();
    }
    List<OrgEntity> orgs = orgMapper.selectBatchIds(orgIds);
    Map<Long, String> map = new LinkedHashMap<>();
    for (OrgEntity org : orgs) {
      map.put(org.getId(), org.getName());
    }
    return map;
  }

  private Map<String, Object> buildQrcodeResult(Long signUpId, String title) {
    Path file = qrcodeFilePath(signUpId);
    boolean exists = Files.exists(file);
    String qrcode = exists ? qrcodePublicUrl(signUpId) : "";
    String qrcodeDataUrl = exists ? readQrcodeDataUrl(file) : "";
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("signUpId", signUpId);
    result.put("title", title);
    result.put("qrcode", qrcode);
    result.put("qrcodeDataUrl", qrcodeDataUrl);
    result.put("registrationPath", "pages/registration/detail?id=" + signUpId);
    result.put("generated", StringUtils.hasText(qrcodeDataUrl) || StringUtils.hasText(qrcode));
    return result;
  }

  private String readQrcodeDataUrl(Path file) {
    try {
      byte[] bytes = Files.readAllBytes(file);
      if (bytes.length == 0) {
        return "";
      }
      String mime = detectImageMime(bytes);
      return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      log.warn("读取二维码文件失败: {}", file, e);
      return "";
    }
  }

  private static String detectImageMime(byte[] data) {
    if (data.length >= 8
        && data[0] == (byte) 0x89
        && data[1] == 0x50
        && data[2] == 0x4E
        && data[3] == 0x47) {
      return "image/png";
    }
    if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
      return "image/jpeg";
    }
    return "image/png";
  }

  private Path qrcodeFilePath(Long signUpId) {
    return Paths.get(cdnPath).resolve("wxqrcode/signup_self_" + signUpId + ".png");
  }

  private String qrcodePublicUrl(Long signUpId) {
    return cdnUrl + "/wxqrcode/signup_self_" + signUpId + ".png";
  }

  private TrainSignUpSelfListVO toListVO(TrainSignUpSelfEntity e, long peopleCount, String orgName) {
    Path qr = qrcodeFilePath(e.getId());
    String wxMiniQrcode = Files.exists(qr) ? qrcodePublicUrl(e.getId()) : "";
    return new TrainSignUpSelfListVO(
        e.getId(),
        e.getTitle(),
        e.getCover(),
        e.getTrainTime(),
        e.getExamTime(),
        e.getOrgId(),
        orgName,
        e.getTrainInfoId(),
        e.getSortNumber(),
        peopleCount,
        wxMiniQrcode,
        filesResolver.resolveFiles(e.getFiles()),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
