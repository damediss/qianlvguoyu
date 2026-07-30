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
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpSelfFilesResolver;
import com.guoyu.platform.trainingdepartment.support.TrainingWxMiniQrcodeClient;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfListVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
  private final OrgMapper orgMapper;
  private final TrainingOperatorScopeService operatorScopeService;
  private final TrainingWxMiniQrcodeClient wxMiniQrcodeClient;
  private final QrCodeGenerationService qrCodeGenerationService;
  private final SignUpSelfFilesResolver filesResolver;
  private final ClassCollectionApplicationService classCollectionApplicationService;
  private final String cdnPath;
  private final String cdnUrl;
  private final String appBaseUrl;

  public TrainSignUpCollectionApplicationService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      OrgMapper orgMapper,
      TrainingOperatorScopeService operatorScopeService,
      TrainingWxMiniQrcodeClient wxMiniQrcodeClient,
      QrCodeGenerationService qrCodeGenerationService,
      SignUpSelfFilesResolver filesResolver,
      ClassCollectionApplicationService classCollectionApplicationService,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.app-base-url:http://localhost:8080}") String appBaseUrl) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.orgMapper = orgMapper;
    this.operatorScopeService = operatorScopeService;
    this.wxMiniQrcodeClient = wxMiniQrcodeClient;
    this.qrCodeGenerationService = qrCodeGenerationService;
    this.filesResolver = filesResolver;
    this.classCollectionApplicationService = classCollectionApplicationService;
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
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(TrainSignUpSelfEntity::getOrgId, companyFilter);
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
    LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaUpdateWrapper<TrainSignUpPeopleSelfEntity>()
        .in(TrainSignUpPeopleSelfEntity::getId, ids)
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId);
    if ("status".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getStatus, value != null ? value : 0);
    } else if ("has_pay".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getHasPay, value != null ? value : 0);
    } else if ("company_status".equals(field)) {
      wrapper.set(TrainSignUpPeopleSelfEntity::getCompanyStatus, value != null ? value : 0);
    } else {
      wrapper.set(TrainSignUpPeopleSelfEntity::getDeleted, 1);
    }
    wrapper.set(TrainSignUpPeopleSelfEntity::getUpdateTime, LocalDateTime.now());
    int updated = peopleMapper.update(null, wrapper);
    if ("status".equals(field) && value != null && value == ClassCollectionApplicationService.STATUS_APPROVED) {
      classCollectionApplicationService.syncApprovedPeopleToRoster(ids);
    }
    return updated;
  }

  @Transactional(readOnly = true)
  public byte[] exportPeopleExcel(Long signUpId) {
    getSignUp(signUpId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<TrainSignUpPeopleSelfEntity> people = peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .orderByDesc(TrainSignUpPeopleSelfEntity::getId));
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("报名人员");
      Row header = sheet.createRow(0);
      String[] titles = {"ID", "姓名", "手机号", "身份证号", "性别", "学历", "状态", "创建时间"};
      for (int i = 0; i < titles.length; i++) {
        header.createCell(i).setCellValue(titles[i]);
      }
      int rowIdx = 1;
      for (TrainSignUpPeopleSelfEntity p : people) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(p.getId() != null ? p.getId() : 0);
        row.createCell(1).setCellValue(p.getName() != null ? p.getName() : "");
        row.createCell(2).setCellValue(p.getPhone() != null ? p.getPhone() : "");
        row.createCell(3).setCellValue(p.getIdCardNumber() != null ? p.getIdCardNumber() : "");
        row.createCell(4).setCellValue(p.getGender() != null ? p.getGender() : "");
        row.createCell(5).setCellValue(p.getEducation() != null ? p.getEducation() : "");
        row.createCell(6).setCellValue(statusLabel(p.getStatus()));
        row.createCell(7).setCellValue(p.getCreateTime() != null ? p.getCreateTime().toString() : "");
      }
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败");
    }
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
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
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

  private static String statusLabel(Integer status) {
    if (status == null || status == 0) {
      return "待审核";
    }
    return switch (status) {
      case 1 -> "已通过";
      case 2 -> "拒绝";
      default -> "待审核";
    };
  }
}
