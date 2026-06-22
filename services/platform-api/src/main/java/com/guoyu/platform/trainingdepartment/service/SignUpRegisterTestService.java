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

  @Value("${platform.cdn.path:./data/cdn}")
  private String cdnPath;

  @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}")
  private String cdnUrl;

  // ==================== 详情 ====================

  @Transactional(readOnly = true)
  public SignUpDetailVO getDetail(Long signUpId) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);
    Long count = peopleMapper.selectCount(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    return SignUpDetailVO.builder()
        .signUpId(plan.getId())
        .title(plan.getTitle())
        .cover(plan.getCover())
        .trainTime(plan.getTrainTime())
        .examTime(plan.getExamTime())
        .orgId(plan.getOrgId())
        .trainInfoId(plan.getTrainInfoId())
        .peopleCount(count == null ? 0 : count.intValue())
        .files(parseFiles(plan.getFiles()))
        .build();
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

    // 公网 URL：用 platform.cdn.url 拼（dev 改成 http://localhost:8080/api/exam/files）
    String base = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    String url = base + "/sign-up/qrcode/" + today + "/" + filename;
    log.info("扫码上传文件成功 path={} url={}", target, url);
    return url;
  }

  // ==================== 提交报名（完整字段） ====================

  @Transactional
  public SignUpRegisterResultVO register(Long signUpId, SignUpRegisterReq req) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);

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
    e.setLevel(req.getLevel());
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
    log.info("扫码报名成功 signUpId={} idCard={} id={} ip={}",
        signUpId, req.getIdCardNumber(), e.getId(), currentIp());

    return new SignUpRegisterResultVO(
        e.getId(), signUpId, e.getName(), e.getPhone(),
        e.getCreateTime() != null ? e.getCreateTime().toString() : "");
  }

  // ==================== 扫码端·"我的报名"查看 / 修改 ====================

  /**
   * 根据手机号 + 身份证号查"我"在该计划下的报名记录。
   * 身份校验：两条同时匹配才返回（防身份证号撞库）。
   */
  @Transactional(readOnly = true)
  public SignUpMineVO findMine(Long signUpId, String phone, String idCardNumber) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);

    TrainSignUpPeopleSelfEntity e = peopleMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getPhone, phone)
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCardNumber)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (e == null) {
      // 故意不区分"手机号存在但身份证不匹配" vs "完全没记录"，防探测
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到您的报名记录，请确认手机号与身份证号");
    }
    boolean editable = isEditable(e);
    return SignUpMineVO.from(e, plan.getTitle(), editable);
  }

  /**
   * 扫码端"修改我的报名" - 用手机号+身份证号做身份校验。
   * 限制：仅在 status=0（待审核）时允许改；status=1（已通过）拒绝修改，
   * 引导用户联系管理后台。
   */
  @Transactional
  public SignUpRegisterResultVO updateMine(Long signUpId, String phone, String idCardNumber,
                                           SignUpRegisterReq req) {
    TrainSignUpSelfEntity plan = mustGetSignUp(signUpId);

    TrainSignUpPeopleSelfEntity e = peopleMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getPhone, phone)
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCardNumber)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (e == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到您的报名记录，请确认手机号与身份证号");
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
    e.setLevel(req.getLevel());
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
