package com.guoyu.platform.app.registration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppRegistrationFormSupportTest {

  private AppRegistrationFormSupport support;

  @BeforeEach
  void setUp() {
    support = new AppRegistrationFormSupport("./data/cdn", new ObjectMapper());
  }

  @Test
  void validateRequiredFieldsReturnsEmptyWhenComplete() {
    Map<String, Object> formData = completeFormData();

    List<String> tips = support.validateRequiredFields(formData);

    assertThat(tips).isEmpty();
  }

  @Test
  void validateRequiredFieldsDetectsMissingIdCardFront() {
    Map<String, Object> formData = completeFormData();
    formData.put("id_card_front", "");

    List<String> tips = support.validateRequiredFields(formData);

    assertThat(tips).contains("身份证正面");
  }

  @Test
  void calcAgeFromIdCardUsesEighteenDigitId() {
    Integer age = support.calcAgeFromIdCard("110101199001011234");

    assertThat(age).isNotNull();
    assertThat(age).isGreaterThan(20);
  }

  private static Map<String, Object> completeFormData() {
    Map<String, Object> formData = new LinkedHashMap<>();
    formData.put("id_card_front", "front.jpg");
    formData.put("id_card_back", "back.jpg");
    formData.put("id_card_header", "header.jpg");
    formData.put("school", "测试大学");
    formData.put("education", "本科");
    formData.put("major", "计算机");
    formData.put("school_paper_has", 0);
    formData.put("in_school_date", "2018-09-01");
    formData.put("graduate_date", "2022-06-01");
    formData.put("work_time", "2022-07-01");
    formData.put("work_date_start", "2023-01-01");
    formData.put("work_type_paper_has", 0);
    formData.put("title_type", "无");
    formData.put("work_exp", List.of(Map.of("company", "测试公司")));
    formData.put("contact_first_page", "c1.jpg");
    formData.put("contact_expire_page", "c2.jpg");
    formData.put("contact_sign_page", "c3.jpg");
    return formData;
  }
}
