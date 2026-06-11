package com.guoyu.platform.app.registration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 自主报名表单：校验、Map 转换、证件路径回填（对齐 PHP AppregistrationController） */
@Component
public class AppRegistrationFormSupport {

  private static final Set<String> PRIMARY_EDUCATION = Set.of("小学", "初中");
  private static final Set<String> NO_MAJOR_EDUCATION = Set.of("小学", "初中", "高中");

  private final String cdnPath;
  private final ObjectMapper objectMapper;

  public AppRegistrationFormSupport(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      ObjectMapper objectMapper) {
    this.cdnPath = cdnPath;
    this.objectMapper = objectMapper;
  }

  public List<String> validateRequiredFields(Map<String, Object> formData) {
    List<String> tips = new ArrayList<>();
    if (isBlank(formData.get("id_card_front"))) {
      tips.add("身份证正面");
    }
    if (isBlank(formData.get("id_card_back"))) {
      tips.add("身份证反面");
    }
    if (isBlank(formData.get("id_card_header"))) {
      tips.add("证件照");
    }
    if (isBlank(formData.get("school"))) {
      tips.add("毕业院校");
    }
    if (isBlank(formData.get("education"))) {
      tips.add("学历");
    }
    String education = stringVal(formData.get("education"));
    if (!PRIMARY_EDUCATION.contains(education)) {
      if (isBlank(formData.get("major")) && !NO_MAJOR_EDUCATION.contains(education)) {
        tips.add("专业");
      }
      if (intVal(formData.get("school_paper_has")) == 1) {
        if (isBlank(formData.get("school_paper"))) {
          tips.add("毕业证照片");
        }
        if (isBlank(formData.get("school_no"))) {
          tips.add("毕业证编号");
        }
      }
    }
    if (isBlank(formData.get("in_school_date"))) {
      tips.add("入学日期");
    }
    if (isBlank(formData.get("graduate_date"))) {
      tips.add("毕业日期");
    }
    if (isBlank(formData.get("work_time"))) {
      tips.add("从业日期");
    }
    if (isBlank(formData.get("work_date_start"))) {
      tips.add("本公司入职日期");
    }
    if (intVal(formData.get("work_type_paper_has")) > 0 && isBlank(formData.get("work_type_paper"))) {
      tips.add("上一等级证书");
    }
    if (isBlank(formData.get("title_type"))) {
      tips.add("职称");
    }
    Object workExp = formData.get("work_exp");
    if (workExp == null || (workExp instanceof List<?> list && list.isEmpty())) {
      tips.add("工作经历");
    }
    if (isBlank(formData.get("contact_first_page"))) {
      tips.add("劳动合同第一页");
    }
    if (isBlank(formData.get("contact_expire_page"))) {
      tips.add("劳动合同期限页");
    }
    if (isBlank(formData.get("contact_sign_page"))) {
      tips.add("劳动合同盖章页");
    }
    return tips;
  }

  public Map<String, Object> entityToFormMap(TrainSignUpPeopleSelfEntity entity) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (entity == null) {
      return map;
    }
    put(map, "id", entity.getId());
    put(map, "sign_up_id", entity.getSignUpId());
    put(map, "platform_user_id", entity.getPlatformUserId());
    put(map, "user_id", entity.getUserId());
    put(map, "name", entity.getName());
    put(map, "phone", entity.getPhone());
    put(map, "id_card_number", entity.getIdCardNumber());
    put(map, "age", entity.getAge());
    put(map, "gender", entity.getGender());
    put(map, "nation", entity.getNation());
    put(map, "political", entity.getPolitical());
    put(map, "house_type", entity.getHouseType());
    put(map, "address", entity.getAddress());
    put(map, "education", entity.getEducation());
    put(map, "id_card_front", entity.getIdCardFront());
    put(map, "id_card_back", entity.getIdCardBack());
    put(map, "id_card_header", entity.getIdCardHeader());
    put(map, "school", entity.getSchool());
    put(map, "major", entity.getMajor());
    put(map, "school_paper_has", entity.getSchoolPaperHas());
    put(map, "school_paper", entity.getSchoolPaper());
    put(map, "school_no", entity.getSchoolNo());
    put(map, "in_school_date", entity.getInSchoolDate());
    put(map, "graduate_date", entity.getGraduateDate());
    put(map, "work_time", entity.getWorkTime());
    put(map, "work_date_start", entity.getWorkDateStart());
    put(map, "work_date_end", entity.getWorkDateEnd());
    put(map, "req_work_type_id", entity.getReqWorkTypeId());
    put(map, "req_work_type_name", entity.getReqWorkTypeName());
    put(map, "req_level", entity.getReqLevel());
    put(map, "work_type_paper_has", entity.getWorkTypePaperHas());
    put(map, "work_type_paper", entity.getWorkTypePaper());
    put(map, "level", entity.getLevel());
    put(map, "work_type_id", entity.getWorkTypeId());
    put(map, "paper_no", entity.getPaperNo());
    put(map, "title_type", entity.getTitleType());
    put(map, "title_paper", entity.getTitlePaper());
    put(map, "contact_first_page", entity.getContactFirstPage());
    put(map, "contact_expire_page", entity.getContactExpirePage());
    put(map, "contact_sign_page", entity.getContactSignPage());
    put(map, "company_id", entity.getCompanyId());
    put(map, "company_name", entity.getCompanyName());
    put(map, "status", entity.getStatus());
    put(map, "company_status", entity.getCompanyStatus());
    put(map, "has_pay", entity.getHasPay());
    put(map, "work_exp", parseWorkExpList(entity.getWorkExp()));
    fillIdCardFallbackPaths(map);
    return map;
  }

  public Map<String, Object> buildPrefillFromUserDetail(
      Long signUpId,
      Long platformUserId,
      String idcard,
      Map<String, Object> detail,
      Long companyId,
      String companyName) {
    Map<String, Object> formData = new LinkedHashMap<>();
    formData.put("id", 0);
    formData.put("sign_up_id", signUpId);
    formData.put("has_pay", 0);
    formData.put("company_id", companyId != null ? companyId : 0);
    formData.put("company_name", companyName);
    formData.put("platform_user_id", platformUserId);
    formData.put("phone", mapVal(detail, "phone"));
    formData.put("id_card_front", mapVal(detail, "idcard_front"));
    formData.put("id_card_back", mapVal(detail, "idcard_back"));
    formData.put("name", mapVal(detail, "realname"));
    formData.put("id_card_number", idcard);
    formData.put("address", mapVal(detail, "address"));
    formData.put("age", calcAgeFromIdCard(idcard));
    formData.put("nation", mapVal(detail, "nation"));
    formData.put("id_card_header", mapVal(detail, "idcard_header"));
    formData.put("house_type", mapVal(detail, "house_type"));
    formData.put("political", mapVal(detail, "political"));
    formData.put("school", mapVal(detail, "school"));
    formData.put("education", mapVal(detail, "education"));
    formData.put("major", mapVal(detail, "major"));
    formData.put("school_paper_has", StringUtils.hasText(mapVal(detail, "school_paper")) ? 1 : 0);
    formData.put("school_paper", mapVal(detail, "school_paper"));
    formData.put("school_no", mapVal(detail, "school_no"));
    formData.put("in_school_date", mapVal(detail, "in_school_date"));
    formData.put("graduate_date", mapVal(detail, "graduate_date"));
    formData.put("work_time", mapVal(detail, "work_time"));
    formData.put("work_date_start", "");
    formData.put("work_date_end", "");
    formData.put("req_work_type_id", 0);
    formData.put("req_work_type_name", "");
    formData.put("req_level", 0);
    formData.put("work_type_paper_has", 0);
    formData.put("work_type_paper", "");
    formData.put("level", "0");
    formData.put("work_type_id", 0);
    formData.put("paper_no", "");
    formData.put("title_type", mapVal(detail, "title_type"));
    formData.put("work_exp", Collections.emptyList());
    formData.put("contact_first_page", "");
    formData.put("contact_expire_page", "");
    formData.put("contact_sign_page", "");
    fillIdCardFallbackPaths(formData);
    return formData;
  }

  public void applyFormData(TrainSignUpPeopleSelfEntity entity, Map<String, Object> formData) {
    entity.setPlatformUserId(longVal(formData.get("platform_user_id")));
    entity.setUserId(longVal(formData.get("user_id")));
    entity.setName(stringVal(formData.get("name")));
    entity.setPhone(stringVal(formData.get("phone")));
    entity.setIdCardNumber(stringVal(formData.get("id_card_number")));
    entity.setAge(intVal(formData.get("age")));
    entity.setGender(stringVal(formData.get("gender")));
    entity.setNation(stringVal(formData.get("nation")));
    entity.setPolitical(stringVal(formData.get("political")));
    entity.setHouseType(stringVal(formData.get("house_type")));
    entity.setAddress(stringVal(formData.get("address")));
    entity.setEducation(stringVal(formData.get("education")));
    entity.setIdCardFront(stringVal(formData.get("id_card_front")));
    entity.setIdCardBack(stringVal(formData.get("id_card_back")));
    entity.setIdCardHeader(stringVal(formData.get("id_card_header")));
    entity.setSchool(stringVal(formData.get("school")));
    entity.setMajor(stringVal(formData.get("major")));
    entity.setSchoolPaperHas(intVal(formData.get("school_paper_has")));
    entity.setSchoolPaper(stringVal(formData.get("school_paper")));
    entity.setSchoolNo(stringVal(formData.get("school_no")));
    entity.setInSchoolDate(stringVal(formData.get("in_school_date")));
    entity.setGraduateDate(stringVal(formData.get("graduate_date")));
    entity.setWorkTime(stringVal(formData.get("work_time")));
    entity.setWorkDateStart(stringVal(formData.get("work_date_start")));
    entity.setWorkDateEnd(stringVal(formData.get("work_date_end")));
    entity.setReqWorkTypeId(longVal(formData.get("req_work_type_id")));
    entity.setReqWorkTypeName(stringVal(formData.get("req_work_type_name")));
    entity.setReqLevel(intVal(formData.get("req_level")));
    entity.setWorkTypePaperHas(intVal(formData.get("work_type_paper_has")));
    entity.setWorkTypePaper(stringVal(formData.get("work_type_paper")));
    entity.setLevel(intVal(formData.get("level")));
    entity.setWorkTypeId(longVal(formData.get("work_type_id")));
    entity.setPaperNo(stringVal(formData.get("paper_no")));
    entity.setTitleType(stringVal(formData.get("title_type")));
    entity.setTitlePaper(stringVal(formData.get("title_paper")));
    entity.setContactFirstPage(stringVal(formData.get("contact_first_page")));
    entity.setContactExpirePage(stringVal(formData.get("contact_expire_page")));
    entity.setContactSignPage(stringVal(formData.get("contact_sign_page")));
    entity.setCompanyId(longVal(formData.get("company_id")));
    entity.setCompanyName(stringVal(formData.get("company_name")));
    entity.setWorkExp(encodeWorkExp(formData.get("work_exp")));
  }

  public void fillIdCardFallbackPaths(Map<String, Object> formData) {
    String idCard = stringVal(formData.get("id_card_number"));
    if (!StringUtils.hasText(idCard)) {
      return;
    }
    if (isBlank(formData.get("id_card_front"))) {
      String front = resolveStoredIdCardPath(idCard, "front");
      if (StringUtils.hasText(front)) {
        formData.put("id_card_front", front);
      }
    }
    if (isBlank(formData.get("id_card_back"))) {
      String back = resolveStoredIdCardPath(idCard, "back");
      if (StringUtils.hasText(back)) {
        formData.put("id_card_back", back);
      }
    }
  }

  private String resolveStoredIdCardPath(String idCard, String side) {
    String suffix = "front".equals(side) ? "_front.jpg" : "_back.jpg";
    Path jpg = Paths.get(cdnPath, "idCard/headerOrigin", idCard + suffix);
    if (Files.exists(jpg)) {
      return "idCard/headerOrigin/" + idCard + suffix;
    }
    if ("back".equals(side)) {
      Path png = Paths.get(cdnPath, "idCard/headerOrigin", idCard + "_back.png");
      if (Files.exists(png)) {
        return "idCard/headerOrigin/" + idCard + "_back.png";
      }
    }
    return null;
  }

  private List<Map<String, Object>> parseWorkExpList(String workExpJson) {
    if (!StringUtils.hasText(workExpJson)) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(workExpJson, new TypeReference<>() {});
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  private String encodeWorkExp(Object workExp) {
    if (workExp == null) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(workExp);
    } catch (Exception ex) {
      return "[]";
    }
  }

  public Integer calcAgeFromIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return null;
    }
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

  private static void put(Map<String, Object> map, String key, Object value) {
    map.put(key, value != null ? value : "");
  }

  private static String mapVal(Map<String, Object> map, String key) {
    if (map == null || !map.containsKey(key) || map.get(key) == null) {
      return "";
    }
    return String.valueOf(map.get(key));
  }

  private static boolean isBlank(Object value) {
    return !StringUtils.hasText(stringVal(value));
  }

  private static String stringVal(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value).trim();
  }

  private static int intVal(Object value) {
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return 0;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private static Long longVal(Object value) {
    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
