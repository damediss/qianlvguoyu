package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.SignUpMineVO;
import com.guoyu.platform.trainingdepartment.dto.SignUpRegisterReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpLevelSupport;
import com.guoyu.platform.trainingdepartment.vo.SignUpDetailVO;
import com.guoyu.platform.trainingdepartment.vo.SignUpRegisterResultVO;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 扫码信息收集 - 测试用服务（完整版）。
 *
 * <p>无登录态、跨租户允许调用，专为「扫码 → 填信息 → Java 表入库」全字段验证。
 * 生产环境请在 controller 加 @PreAuthorize 与租户校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpRegisterTestService {

  private static final Set<String> ALLOWED_EXTS = Set.of("jpg", "jpeg", "png", "pdf");
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final ObjectMapper objectMapper;
  private final ClassCollectionApplicationService classCollectionApplicationService;

  @Value("${platform.cdn.path:./data/cdn}")
  private String cdnPath;

  @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}")
  private String cdnUrl;

  /**
   * 写入库 / 返回给 App 的公网可访问 CDN 前缀。
   * 生产务必走 Nginx（不要带 :8080），例如 http://43.138.163.7/api/exam/files
   */
  @Value("${platform.cdn.public-url:}")
  private String cdnPublicUrl;

  // ==================== 详情 ====================

  @Transactional(readOnly = true)
  public SignUpDetailVO getDetail(Long signUpId) {
    return getDetail(signUpId, null);
  }

  @Transactional(readOnly = true)
  public SignUpDetailVO getDetail(Long signUpId, String idCardNumber) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    Long count = peopleMapper.selectCount(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    boolean classPrep = classCollectionApplicationService.isClassPrep(plan);
    Boolean inRoster = null;
    if (StringUtils.hasText(idCardNumber) && classPrep) {
      Map<String, Object> check = classCollectionApplicationService.checkRosterAccess(plan, idCardNumber);
      Object v = check.get("inRoster");
      inRoster = v instanceof Boolean ? (Boolean) v : Boolean.FALSE;
    }
    return SignUpDetailVO.builder()
        .signUpId(plan.getId())
        .title(plan.getTitle())
        .cover(plan.getCover())
        .trainTime(plan.getTrainTime())
        .examTime(plan.getExamTime())
        .orgId(plan.getOrgId())
        .trainInfoId(plan.getTrainInfoId())
        .trainClassId(plan.getTrainClassId())
        .collectionSource(plan.getCollectionSource())
        .tenantId(plan.getTenantId())
        .requireRoster(classPrep)
        .inRoster(inRoster)
        .peopleCount(count == null ? 0 : count.intValue())
        .files(parseFiles(plan.getFiles()))
        .build();
  }

  /** 扫码验证：花名册门禁检查（App 点「验证」时先调） */
  @Transactional(readOnly = true)
  public Map<String, Object> checkRoster(Long signUpId, String idCardNumber) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    return classCollectionApplicationService.checkRosterAccess(plan, idCardNumber);
  }

  // ==================== 文件上传 ====================

  public String uploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件为空");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能超过 10MB");
    }
    String name = file.getOriginalFilename();
    String ext = "";
    if (name != null) {
      int dot = name.lastIndexOf('.');
      if (dot >= 0) ext = name.substring(dot + 1).toLowerCase();
    }
    if (!ALLOWED_EXTS.contains(ext)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpg/png/pdf");
    }

    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    String filename = "qrcode_" + UUID.randomUUID().toString().replace("-", "")
        + (ext.isEmpty() ? "" : "." + ext);
    // 写盘路径：直接用 platform.cdn.path（与 ExamTaskXimuController.serveFile 读盘同根，
    // 配合 /api/exam/files/** 静态映射即可在前端 img 直接展示）。
    Path target = Paths.get(cdnPath, "sign-up/qrcode", today, filename).toAbsolutePath();
    try {
      Files.createDirectories(target.getParent());
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      log.error("保存上传文件失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
    }

    // 公网 URL：优先 public-url（Nginx），避免写入 :8080 导致浏览器公网无法预览
    String base = resolvePublicCdnBase();
    String url = base + "/sign-up/qrcode/" + today + "/" + filename;
    log.info("扫码上传文件成功 path={} url={}", target, url);
    return url;
  }

  /** 返回给客户端的 CDN 根地址（去掉非本机 :8080/:8081） */
  private String resolvePublicCdnBase() {
    String raw = StringUtils.hasText(cdnPublicUrl) ? cdnPublicUrl.trim() : cdnUrl;
    if (!StringUtils.hasText(raw)) {
      raw = "http://localhost:8080/api/exam/files";
    }
    String base = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    try {
      java.net.URI uri = java.net.URI.create(base);
      String host = uri.getHost();
      int port = uri.getPort();
      boolean local = host == null
          || "localhost".equalsIgnoreCase(host)
          || "127.0.0.1".equals(host);
      if (!local && (port == 8080 || port == 8081)) {
        String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
        String path = uri.getPath() != null ? uri.getPath() : "";
        if (path.endsWith("/")) {
          path = path.substring(0, path.length() - 1);
        }
        return scheme + "://" + host + path;
      }
    } catch (Exception ignored) {
      // keep base
    }
    return base;
  }

  // ==================== 提交报名（完整字段） ====================

  @Transactional
  public SignUpRegisterResultVO register(Long signUpId, SignUpRegisterReq req) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    // 开班准备：仅班级花名册内学员可提交
    classCollectionApplicationService.assertIdCardInClassRoster(plan, req.getIdCardNumber());

    // 重复报名：同 signUpId 下身份证不可重复
    Long dup = peopleMapper.selectCount(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, req.getIdCardNumber())
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    if (dup != null && dup > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "您已报名，请勿重复提交");
    }

    TrainSignUpPeopleSelfEntity e = new TrainSignUpPeopleSelfEntity();
    e.setSignUpId(signUpId);
    e.setTenantId(plan.getTenantId() != null ? plan.getTenantId() : 1L);

    // === 基本信息 ===
    e.setName(req.getName());
    e.setPhone(req.getPhone());
    e.setIdCardNumber(req.getIdCardNumber());
    e.setAge(req.getAge());
    e.setGender(req.getGender());
    e.setNation(req.getNation());
    e.setPolitical(req.getPolitical());
    e.setEducation(req.getEducation());
    e.setHouseType(req.getHouseType());
    e.setAddress(req.getAddress());

    // === 学历 ===
    e.setSchool(req.getSchool());
    e.setMajor(req.getMajor());
    e.setInSchoolDate(req.getInSchoolDate());
    e.setGraduateDate(req.getGraduateDate());
    e.setSchoolNo(req.getSchoolNo());
    e.setSchoolPaperHas(req.getSchoolPaperHas());
    e.setSchoolPaper(req.getSchoolPaper());

    // === 工作 ===
    e.setCompanyId(req.getCompanyId());
    e.setCompanyName(req.getCompanyName());
    e.setCompanyCode(req.getCompanyCode());
    e.setZipCode(req.getZipCode());
    e.setCompanyAddress(req.getCompanyAddress());
    e.setIndustrialPark(req.getIndustrialPark());
    e.setComments(req.getComments());
    e.setWorkDateStart(req.getWorkDateStart());
    e.setWorkDateEnd(req.getWorkDateEnd());
    e.setWorkTime(req.getWorkTime());

    // === 职称 ===
    e.setTitleType(req.getTitleType());
    e.setTitleLevel(req.getTitleLevel());
    e.setTitlePaper(req.getTitlePaper());

    // === 现工种 ===
    e.setWorkTypeId(req.getWorkTypeId());
    e.setWorkTypeName(req.getWorkTypeName());
    e.setLevel(SignUpLevelSupport.fromIndex(req.getLevel()));
    e.setPaperNo(req.getPaperNo());

    // === 申报工种 ===
    e.setReqWorkTypeId(req.getReqWorkTypeId());
    e.setReqWorkTypeName(req.getReqWorkTypeName());
    e.setReqLevel(req.getReqLevel());
    e.setDeclarationCondition(req.getDeclarationCondition());
    e.setWorkExp(req.getWorkExp());
    e.setJob(req.getJob());
    e.setJog(req.getJog());

    // === 上一等级证书 ===
    e.setWorkTypePaperHas(req.getWorkTypePaperHas());
    e.setWorkTypePaper(req.getWorkTypePaper());
    e.setWorkTypePaperName(req.getWorkTypePaperName());
    e.setWorkTypePaperLevel(req.getWorkTypePaperLevel());
    e.setWorkTypePaperNo(req.getWorkTypePaperNo());

    // === 合同 / 社保 ===
    e.setContactFirstPage(req.getContactFirstPage());
    e.setContactExpirePage(req.getContactExpirePage());
    e.setContactSignPage(req.getContactSignPage());
    e.setSocialSecurity(req.getSocialSecurity());

    // === 身份证 / 照片 ===
    e.setIdCardFront(req.getIdCardFront());
    e.setIdCardBack(req.getIdCardBack());
    e.setIdCardHeader(req.getIdCardHeader());
    e.setIdCardHeader2(req.getIdCardHeader2());
    e.setPhoto(req.getPhoto());
    e.setContract(req.getContract());
    e.setCertificate(req.getCertificate());

    // === 状态 ===
    e.setStatus(0);
    e.setCompanyStatus(0);
    e.setHasPay(0);
    e.setCreateTime(LocalDateTime.now());
    e.setUpdateTime(LocalDateTime.now());

    peopleMapper.insert(e);
    // 开班准备扫码：新建即同步花名册（审核通过也会再 upsert）
    if (ClassCollectionApplicationService.SOURCE_CLASS_PREP.equals(plan.getCollectionSource())) {
      classCollectionApplicationService.syncPersonToRoster(e);
    }
    log.info("扫码报名成功 signUpId={} idCard={} id={} ip={}",
        signUpId, req.getIdCardNumber(), e.getId(), currentIp());

    return new SignUpRegisterResultVO(
        e.getId(), signUpId, e.getName(), e.getPhone(),
        e.getCreateTime() != null ? e.getCreateTime().toString() : "");
  }

  // ==================== 扫码端·"我的报名"查看 / 修改 ====================

  /**
   * 查"我"在该计划下的报名记录。
   * class_prep：只按身份证（花名册门禁）；手机号可不对、可不传。
   * 其它计划：手机号+身份证双因子。
   */
  @Transactional(readOnly = true)
  public SignUpMineVO findMine(Long signUpId, String phone, String idCardNumber) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    String normalizedId = normalizeIdCard(idCardNumber);
    // 开班准备：先校验花名册，再查报名记录
    classCollectionApplicationService.assertIdCardInClassRoster(plan, normalizedId);

    boolean classPrep = classCollectionApplicationService.isClassPrep(plan);
    if (!classPrep) {
      requireValidPhone(phone);
    }

    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> q =
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .apply("UPPER(TRIM(id_card_number)) = {0}", normalizedId);
    // 非开班准备仍要求手机号+身份证双因子，降低公开链路上的探测面
    if (!classPrep) {
      q.eq(TrainSignUpPeopleSelfEntity::getPhone, phone.trim());
    }
    TrainSignUpPeopleSelfEntity e = peopleMapper.selectOne(q.last("LIMIT 1"));
    if (e == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND,
          classPrep ? "未找到您的报名记录，请确认身份证号是否在本班名单中"
              : "未找到您的报名记录，请确认手机号与身份证号");
    }
    boolean editable = isEditable(e);
    return SignUpMineVO.from(e, plan.getTitle(), editable);
  }

  /**
   * 扫码端"修改我的报名"。
   * class_prep：仅身份证定位；可把手机号改成学员自己的号。
   * 其它计划：手机号+身份证。
   * 限制：仅 status=0（待审核）可改。
   */
  @Transactional
  public SignUpRegisterResultVO updateMine(Long signUpId, String phone, String idCardNumber,
                                           SignUpRegisterReq req) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    String normalizedId = normalizeIdCard(idCardNumber);
    classCollectionApplicationService.assertIdCardInClassRoster(plan, normalizedId);
    if (StringUtils.hasText(req.getIdCardNumber())) {
      classCollectionApplicationService.assertIdCardInClassRoster(plan, req.getIdCardNumber());
    }

    boolean classPrep = classCollectionApplicationService.isClassPrep(plan);
    if (!classPrep) {
      requireValidPhone(phone);
    }

    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> q =
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .apply("UPPER(TRIM(id_card_number)) = {0}", normalizedId);
    if (!classPrep) {
      q.eq(TrainSignUpPeopleSelfEntity::getPhone, phone.trim());
    }
    TrainSignUpPeopleSelfEntity e = peopleMapper.selectOne(q.last("LIMIT 1"));
    if (e == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND,
          classPrep ? "未找到您的报名记录，请确认身份证号是否在本班名单中"
              : "未找到您的报名记录，请确认手机号与身份证号");
    }
    if (!isEditable(e)) {
      throw new BusinessException(ErrorCode.FORBIDDEN,
          "您的报名已通过审核或已锁定，无法自行修改，请联系管理后台");
    }

    // === 基本信息 ===
    e.setName(req.getName());
    e.setPhone(req.getPhone());
    e.setIdCardNumber(req.getIdCardNumber());
    e.setAge(req.getAge());
    e.setGender(req.getGender());
    e.setNation(req.getNation());
    e.setPolitical(req.getPolitical());
    e.setEducation(req.getEducation());
    e.setHouseType(req.getHouseType());
    e.setAddress(req.getAddress());

    // === 学历 ===
    e.setSchool(req.getSchool());
    e.setMajor(req.getMajor());
    e.setInSchoolDate(req.getInSchoolDate());
    e.setGraduateDate(req.getGraduateDate());
    e.setSchoolNo(req.getSchoolNo());
    e.setSchoolPaperHas(req.getSchoolPaperHas());
    e.setSchoolPaper(req.getSchoolPaper());

    // === 工作 ===
    e.setCompanyId(req.getCompanyId());
    e.setCompanyName(req.getCompanyName());
    e.setCompanyCode(req.getCompanyCode());
    e.setZipCode(req.getZipCode());
    e.setCompanyAddress(req.getCompanyAddress());
    e.setIndustrialPark(req.getIndustrialPark());
    e.setComments(req.getComments());
    e.setWorkDateStart(req.getWorkDateStart());
    e.setWorkDateEnd(req.getWorkDateEnd());
    e.setWorkTime(req.getWorkTime());

    // === 职称 ===
    e.setTitleType(req.getTitleType());
    e.setTitleLevel(req.getTitleLevel());
    e.setTitlePaper(req.getTitlePaper());

    // === 现工种 ===
    e.setWorkTypeId(req.getWorkTypeId());
    e.setWorkTypeName(req.getWorkTypeName());
    e.setLevel(SignUpLevelSupport.fromIndex(req.getLevel()));
    e.setPaperNo(req.getPaperNo());

    // === 申报工种 ===
    e.setReqWorkTypeId(req.getReqWorkTypeId());
    e.setReqWorkTypeName(req.getReqWorkTypeName());
    e.setReqLevel(req.getReqLevel());
    e.setDeclarationCondition(req.getDeclarationCondition());
    e.setWorkExp(req.getWorkExp());
    e.setJob(req.getJob());
    e.setJog(req.getJog());

    // === 上一等级证书 ===
    e.setWorkTypePaperHas(req.getWorkTypePaperHas());
    e.setWorkTypePaper(req.getWorkTypePaper());
    e.setWorkTypePaperName(req.getWorkTypePaperName());
    e.setWorkTypePaperLevel(req.getWorkTypePaperLevel());
    e.setWorkTypePaperNo(req.getWorkTypePaperNo());

    // === 合同 / 社保 ===
    e.setContactFirstPage(req.getContactFirstPage());
    e.setContactExpirePage(req.getContactExpirePage());
    e.setContactSignPage(req.getContactSignPage());
    e.setSocialSecurity(req.getSocialSecurity());

    // === 身份证 / 照片 ===
    e.setIdCardFront(req.getIdCardFront());
    e.setIdCardBack(req.getIdCardBack());
    e.setIdCardHeader(req.getIdCardHeader());
    e.setIdCardHeader2(req.getIdCardHeader2());
    e.setPhoto(req.getPhoto());
    e.setContract(req.getContract());
    e.setCertificate(req.getCertificate());

    // 不允许用户改审核状态/支付状态 - 保持原值
    // e.setStatus(...), e.setCompanyStatus(...), e.setHasPay(...) 故意不改

    e.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(e);

    log.info("扫码修改报名成功 signUpId={} idCard={} id={} ip={}",
        signUpId, req.getIdCardNumber(), e.getId(), currentIp());

    return new SignUpRegisterResultVO(
        e.getId(), signUpId, e.getName(), e.getPhone(),
        e.getUpdateTime() != null ? e.getUpdateTime().toString() : "");
  }

  /**
   * 是否允许扫码端修改。
   * 规则：status=0（待审核）允许；status=1（已通过）/ status=2（拒绝）不允许。
   */
  private boolean isEditable(TrainSignUpPeopleSelfEntity e) {
    Integer s = e.getStatus();
    return s == null || s == 0;
  }

  private static String normalizeIdCard(String idCardNumber) {
    if (!StringUtils.hasText(idCardNumber)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入身份证号");
    }
    return idCardNumber.trim().toUpperCase();
  }

  private static void requireValidPhone(String phone) {
    if (!StringUtils.hasText(phone) || !phone.trim().matches("^1[3-9]\\d{9}$")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入正确的手机号");
    }
  }

  // ==================== 工具方法 ====================

  private TrainSignUpSelfEntity mustGetSignUp(Long id) {
    if (id == null || id <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "signUpId 不合法");
    }
    TrainSignUpSelfEntity plan = signUpMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getId, id)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "报名计划不存在");
    }
    return plan;
  }

  private List<String> parseFiles(String json) {
    if (!StringUtils.hasText(json)) return List.of();
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String currentIp() {
    try {
      ServletRequestAttributes attr = (ServletRequestAttributes)
          RequestContextHolder.getRequestAttributes();
      if (attr == null) return "";
      HttpServletRequest req = attr.getRequest();
      String ip = req.getHeader("X-Forwarded-For");
      if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
        return ip.split(",")[0].trim();
      }
      return req.getRemoteAddr();
    } catch (Exception e) {
      return "";
    }
  }
}
