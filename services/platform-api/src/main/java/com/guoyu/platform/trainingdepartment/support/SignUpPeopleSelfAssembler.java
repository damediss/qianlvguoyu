package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.file.support.CdnFileServeHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpPeopleSelfVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 报名人员 Entity/VO/DTO 转换与列表 enrich（对齐 PHP peopleselflist） */
@Slf4j
@Component
public class SignUpPeopleSelfAssembler {

  private static final String[] STATUS_CN = {"待审核", "已通过", "拒绝"};
  private static final String[] COMPANY_STATUS_CN = {"待审核", "已通过", "未通过"};

  private final String cdnPath;
  private final String cdnUrl;
  private final ObjectMapper objectMapper;
  private final CdnFileServeHelper cdnFileServeHelper;

  public SignUpPeopleSelfAssembler(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      ObjectMapper objectMapper,
      CdnFileServeHelper cdnFileServeHelper) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.objectMapper = objectMapper;
    this.cdnFileServeHelper = cdnFileServeHelper;
  }

  public TrainSignUpPeopleSelfVO toVo(TrainSignUpPeopleSelfEntity entity) {
    if (entity == null) {
      return null;
    }
    TrainSignUpPeopleSelfVO vo = new TrainSignUpPeopleSelfVO();
    vo.setId(entity.getId());
    vo.setSignUpId(entity.getSignUpId());
    vo.setPlatformUserId(entity.getPlatformUserId());
    vo.setUserId(entity.getUserId());
    vo.setName(entity.getName());
    vo.setPhone(entity.getPhone());
    vo.setIdCardNumber(entity.getIdCardNumber());
    vo.setAge(entity.getAge());
    vo.setGender(entity.getGender());
    vo.setNation(entity.getNation());
    vo.setPolitical(entity.getPolitical());
    vo.setHouseType(entity.getHouseType());
    vo.setAddress(entity.getAddress());
    vo.setEducation(entity.getEducation());
    vo.setIdCardFront(entity.getIdCardFront());
    vo.setIdCardBack(entity.getIdCardBack());
    vo.setIdCardHeader(entity.getIdCardHeader());
    vo.setIdCardHeader2(entity.getIdCardHeader2());
    vo.setPhoto(entity.getPhoto());
    vo.setSchool(entity.getSchool());
    vo.setMajor(entity.getMajor());
    vo.setSchoolPaperHas(entity.getSchoolPaperHas());
    vo.setSchoolNo(entity.getSchoolNo());
    vo.setInSchoolDate(entity.getInSchoolDate());
    vo.setGraduateDate(entity.getGraduateDate());
    vo.setSchoolPaper(entity.getSchoolPaper());
    vo.setWorkTime(entity.getWorkTime());
    vo.setTitleType(entity.getTitleType());
    vo.setTitlePaper(entity.getTitlePaper());
    vo.setCompanyId(entity.getCompanyId());
    vo.setWorkDateStart(entity.getWorkDateStart());
    vo.setWorkDateEnd(entity.getWorkDateEnd());
    vo.setWorkTypePaperHas(entity.getWorkTypePaperHas());
    vo.setWorkTypePaper(entity.getWorkTypePaper());
    vo.setWorkTypeId(entity.getWorkTypeId());
    vo.setWorkTypeName(entity.getWorkTypeName());
    vo.setLevel(entity.getLevel());
    vo.setPaperNo(entity.getPaperNo());
    vo.setReqWorkTypeId(entity.getReqWorkTypeId());
    vo.setReqWorkTypeName(entity.getReqWorkTypeName());
    vo.setReqLevel(entity.getReqLevel());
    vo.setDeclarationCondition(entity.getDeclarationCondition());
    vo.setWorkExp(entity.getWorkExp());
    vo.setJog(entity.getJog());
    vo.setContactFirstPage(entity.getContactFirstPage());
    vo.setContactExpirePage(entity.getContactExpirePage());
    vo.setContactSignPage(entity.getContactSignPage());
    vo.setContract(entity.getContract());
    vo.setCertificate(entity.getCertificate());
    vo.setWorkTypePaperName(entity.getWorkTypePaperName());
    vo.setWorkTypePaperLevel(entity.getWorkTypePaperLevel());
    vo.setWorkTypePaperNo(entity.getWorkTypePaperNo());
    vo.setStatus(entity.getStatus());
    vo.setCompanyStatus(entity.getCompanyStatus());
    vo.setHasPay(entity.getHasPay());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    vo.setUpdateTime(entity.getUpdateTime());
    return vo;
  }

  public void enrich(TrainSignUpPeopleSelfVO vo) {
    if (vo == null) {
      return;
    }
    Long signUpId = vo.getSignUpId();
    String idCard = vo.getIdCardNumber();

    if (vo.getAge() == null && StringUtils.hasText(idCard)) {
      try {
        vo.setAge(calcAgeFromIdCard(idCard));
      } catch (Exception ignored) {
        // ignore
      }
    }

    int status = vo.getStatus() != null ? vo.getStatus() : 0;
    vo.setStatusCn(status >= 0 && status < STATUS_CN.length ? STATUS_CN[status] : STATUS_CN[0]);

    int companyStatus = vo.getCompanyStatus() != null ? vo.getCompanyStatus() : 0;
    vo.setCompanyStatusCn(companyStatus > 0 ? "已通过" : "待审核");

    int hasPay = vo.getHasPay() != null ? vo.getHasPay() : 0;
    vo.setPayCn(hasPay > 0 ? "已支付" : "待支付");

    try {
      vo.setIdcardFront(safeResolve(() -> resolveIdCardImage(idCard, signUpId, vo.getIdCardFront(), "front")));
      vo.setIdcardBack(safeResolve(() -> resolveIdCardImage(idCard, signUpId, vo.getIdCardBack(), "back")));
      vo.setIdcardHeader(safeResolve(() -> resolveHeaderImage(idCard, vo.getIdCardHeader())));
      vo.setPhoto(safeResolve(() -> resolveMediaUrl(vo.getPhoto())));
      vo.setIdCardHeader2(safeResolve(() -> resolveMediaUrl(vo.getIdCardHeader2())));
      vo.setSchoolPaper(safeResolve(() -> resolveMediaUrl(vo.getSchoolPaper())));
      vo.setTitlePaper(safeResolve(() -> resolveMediaUrl(vo.getTitlePaper())));
      vo.setWorkTypePaper(safeResolve(() -> resolveMediaUrl(vo.getWorkTypePaper())));

      vo.setContractFirst(safeResolve(() -> resolveStoredOrCdnUrl(vo.getContactFirstPage(),
          idCard, signUpId, "first_self.png", "_contact_first_page.jpg")));
      vo.setContractExpire(safeResolve(() -> resolveStoredOrCdnUrl(vo.getContactExpirePage(),
          idCard, signUpId, "expire_self.png", "_contact_expire_page.jpg")));
      vo.setContractSign(safeResolve(() -> resolveStoredOrCdnUrl(vo.getContactSignPage(),
          idCard, signUpId, "sign_self.png", "_contact_sign_page.jpg")));
    } catch (Exception ex) {
      // enrich 只做 URL 解析，单条失败也不能让 VO 半残
      log.warn("enrich resolve media failed, ignore", ex);
    }

    if (StringUtils.hasText(vo.getWorkExp())) {
      try {
        vo.setWorkExpList(objectMapper.readValue(vo.getWorkExp(), new TypeReference<List<Map<String, Object>>>() {}));
      } catch (Exception ignored) {
        vo.setWorkExpList(Collections.emptyList());
      }
    }
  }

  private String safeResolve(java.util.function.Supplier<String> supplier) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      return null;
    }
  }

  public void applyUpdate(TrainSignUpPeopleSelfEntity entity, TrainSignUpPeopleSelfUpdateReq req) {
    if (req.getSignUpId() != null) {
      entity.setSignUpId(req.getSignUpId());
    }
    if (req.getPlatformUserId() != null) {
      entity.setPlatformUserId(req.getPlatformUserId());
    }
    if (req.getUserId() != null) {
      entity.setUserId(req.getUserId());
    }
    if (req.getName() != null) {
      entity.setName(req.getName());
    }
    if (req.getPhone() != null) {
      entity.setPhone(req.getPhone());
    }
    if (req.getIdCardNumber() != null) {
      entity.setIdCardNumber(req.getIdCardNumber());
    }
    if (req.getAge() != null) {
      entity.setAge(req.getAge());
    }
    if (req.getGender() != null) {
      entity.setGender(req.getGender());
    }
    if (req.getNation() != null) {
      entity.setNation(req.getNation());
    }
    if (req.getPolitical() != null) {
      entity.setPolitical(req.getPolitical());
    }
    if (req.getHouseType() != null) {
      entity.setHouseType(req.getHouseType());
    }
    if (req.getAddress() != null) {
      entity.setAddress(req.getAddress());
    }
    if (req.getEducation() != null) {
      entity.setEducation(req.getEducation());
    }
    if (req.getPhoto() != null) {
      entity.setPhoto(req.getPhoto());
    }
    if (req.getSchool() != null) {
      entity.setSchool(req.getSchool());
    }
    if (req.getMajor() != null) {
      entity.setMajor(req.getMajor());
    }
    if (req.getSchoolPaperHas() != null) {
      entity.setSchoolPaperHas(req.getSchoolPaperHas());
    }
    if (req.getSchoolNo() != null) {
      entity.setSchoolNo(req.getSchoolNo());
    }
    if (req.getInSchoolDate() != null) {
      entity.setInSchoolDate(req.getInSchoolDate());
    }
    if (req.getGraduateDate() != null) {
      entity.setGraduateDate(req.getGraduateDate());
    }
    if (req.getWorkTime() != null) {
      entity.setWorkTime(req.getWorkTime());
    }
    if (req.getTitleType() != null) {
      entity.setTitleType(req.getTitleType());
    }
    if (req.getCompanyId() != null) {
      entity.setCompanyId(req.getCompanyId());
    }
    if (req.getWorkDateStart() != null) {
      entity.setWorkDateStart(req.getWorkDateStart());
    }
    if (req.getWorkDateEnd() != null) {
      entity.setWorkDateEnd(req.getWorkDateEnd());
    }
    if (req.getWorkTypePaperHas() != null) {
      entity.setWorkTypePaperHas(req.getWorkTypePaperHas());
    }
    if (req.getWorkTypeId() != null) {
      entity.setWorkTypeId(req.getWorkTypeId());
    }
    if (req.getWorkTypeName() != null) {
      entity.setWorkTypeName(req.getWorkTypeName());
    }
    if (req.getLevel() != null) {
      entity.setLevel(req.getLevel());
    }
    if (req.getPaperNo() != null) {
      entity.setPaperNo(req.getPaperNo());
    }
    if (req.getReqWorkTypeId() != null) {
      entity.setReqWorkTypeId(req.getReqWorkTypeId());
    }
    if (req.getReqWorkTypeName() != null) {
      entity.setReqWorkTypeName(req.getReqWorkTypeName());
    }
    if (req.getReqLevel() != null) {
      entity.setReqLevel(req.getReqLevel());
    }
    if (req.getDeclarationCondition() != null) {
      entity.setDeclarationCondition(req.getDeclarationCondition());
    }
    if (req.getWorkExp() != null) {
      entity.setWorkExp(req.getWorkExp());
    }
    if (req.getJog() != null) {
      entity.setJog(req.getJog());
    }
    if (req.getContactFirstPage() != null) {
      entity.setContactFirstPage(req.getContactFirstPage());
    }
    if (req.getContactExpirePage() != null) {
      entity.setContactExpirePage(req.getContactExpirePage());
    }
    if (req.getContactSignPage() != null) {
      entity.setContactSignPage(req.getContactSignPage());
    }
    if (req.getContract() != null) {
      entity.setContract(req.getContract());
    }
    if (req.getCertificate() != null) {
      entity.setCertificate(req.getCertificate());
    }
    if (req.getWorkTypePaperName() != null) {
      entity.setWorkTypePaperName(req.getWorkTypePaperName());
    }
    if (req.getWorkTypePaperLevel() != null) {
      entity.setWorkTypePaperLevel(req.getWorkTypePaperLevel());
    }
    if (req.getWorkTypePaperNo() != null) {
      entity.setWorkTypePaperNo(req.getWorkTypePaperNo());
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getCompanyStatus() != null) {
      entity.setCompanyStatus(req.getCompanyStatus());
    }
    if (req.getHasPay() != null) {
      entity.setHasPay(req.getHasPay());
    }
  }

  private Integer calcAgeFromIdCard(String idCard) {
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

  private String resolveMediaUrl(String stored) {
    if (!StringUtils.hasText(stored) || isInvalidImageStored(stored)) {
      return null;
    }
    return cdnFileServeHelper.publicUrl(stored);
  }

  private boolean isInvalidImageStored(String path) {
    String t = path.trim();
    return t.startsWith("process=image")
        || t.startsWith("x-oss-process=")
        || t.startsWith("?x-oss-process=")
        || t.matches("^(image/)?(resize|quality|crop).*");
  }

  private String resolveIdCardImage(String idCard, Long signUpId, String stored, String side) {
    if (StringUtils.hasText(stored) && isInvalidImageStored(stored)) {
      stored = null;
    }
    if (StringUtils.hasText(stored) && stored.startsWith("http")) {
      return stored;
    }
    if (!StringUtils.hasText(idCard)) {
      return stored;
    }
    String suffix = "front".equals(side) ? "_front" : "_back";
    Path oldPng = Paths.get(cdnPath, "idCard/old/headerOrigin", idCard + suffix + ".png");
    if (Files.exists(oldPng)) {
      return cdnUrl + "/idCard/old/headerOrigin/" + idCard + suffix + ".png";
    }
    Path jpg = Paths.get(cdnPath, "idCard/headerOrigin", idCard + suffix + ".jpg");
    if (Files.exists(jpg)) {
      return cdnUrl + "/idCard/headerOrigin/" + idCard + suffix + ".jpg";
    }
    return toAbsoluteUrl(stored);
  }

  private String resolveHeaderImage(String idCard, String stored) {
    if (StringUtils.hasText(stored) && isInvalidImageStored(stored)) {
      stored = null;
    }
    if (StringUtils.hasText(stored) && stored.startsWith("http")) {
      return stored;
    }
    if (!StringUtils.hasText(idCard)) {
      return toAbsoluteUrl(stored);
    }
    Path oldFull = Paths.get(cdnPath, "idCard/old/headerOrigin", idCard + "_full.png");
    if (Files.exists(oldFull)) {
      return cdnUrl + "/idCard/old/headerOrigin/" + idCard + "_full.png";
    }
    Path header = Paths.get(cdnPath, "idCard/headerOrigin", idCard + "_header.jpg");
    if (Files.exists(header)) {
      return cdnUrl + "/idCard/headerOrigin/" + idCard + "_header.jpg";
    }
    return toAbsoluteUrl(stored);
  }

  private String resolveStoredOrCdnUrl(
      String stored, String idCard, Long signUpId, String contractFile, String legacySuffix) {
    if (StringUtils.hasText(stored) && stored.startsWith("http")) {
      return stored;
    }
    if (StringUtils.hasText(stored) && !stored.startsWith("http")) {
      Path rel = Paths.get(cdnPath, stored.startsWith("/") ? stored.substring(1) : stored);
      if (Files.exists(rel)) {
        return cdnUrl + "/" + (stored.startsWith("/") ? stored.substring(1) : stored);
      }
    }
    if (!StringUtils.hasText(idCard) || signUpId == null) {
      return toAbsoluteUrl(stored);
    }
    Path contract = Paths.get(cdnPath, "contract", idCard + "_" + signUpId + "_" + contractFile);
    if (Files.exists(contract)) {
      return cdnUrl + "/contract/" + idCard + "_" + signUpId + "_" + contractFile;
    }
    Path legacy = Paths.get(cdnPath, "idCard/headerOrigin", idCard + "_" + signUpId + legacySuffix);
    if (Files.exists(legacy)) {
      return cdnUrl + "/contract/" + idCard + "_" + signUpId + "_" + contractFile;
    }
    return toAbsoluteUrl(stored);
  }

  private String toAbsoluteUrl(String path) {
    if (!StringUtils.hasText(path) || isInvalidImageStored(path)) {
      return null;
    }
    return cdnFileServeHelper.publicUrl(path);
  }
}
