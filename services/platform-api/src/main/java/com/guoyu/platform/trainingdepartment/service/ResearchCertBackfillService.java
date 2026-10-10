package com.guoyu.platform.trainingdepartment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.service.CertQueryOutcome;
import com.guoyu.platform.chazheng.service.CollectedCert;
import com.guoyu.platform.chazheng.support.CertMappingSupport;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.CertVerifyDetail;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ResearchCertBackfillService {

  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final ObjectMapper objectMapper;

  public ResearchCertBackfillService(
      TrainSignUpPeopleSelfMapper peopleMapper,
      ObjectMapper objectMapper) {
    this.peopleMapper = peopleMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CertVerifyDetail backfill(TrainSignUpPeopleSelfEntity person, CertQueryOutcome outcome) {
    if (!outcome.anyNormal()) {
      person.setCertVerified(3);
      person.setUpdateTime(LocalDateTime.now());
      peopleMapper.updateById(person);
      return new CertVerifyDetail(
          person.getId(),
          person.getName(),
          person.getIdCardNumber(),
          false,
          null,
          null,
          null,
          "查证请求异常，请重试");
    }

    if (!outcome.anyCertFound() || outcome.certs().isEmpty()) {
      person.setCertVerified(2);
      person.setUpdateTime(LocalDateTime.now());
      peopleMapper.updateById(person);
      return new CertVerifyDetail(
          person.getId(),
          person.getName(),
          person.getIdCardNumber(),
          false,
          null,
          null,
          null,
          "未查到证书信息");
    }

    List<CollectedCert> certs = outcome.certs();
    CollectedCert primary = CertMappingSupport.pickPrimaryCert(certs);
    String levelText = CertMappingSupport.extractLevelText(primary.certName());

    person.setPaperNo(primary.certNo());
    person.setCertIssueDates(primary.certDate());
    person.setLevel(StringUtils.hasText(levelText) ? levelText.trim() : null);
    person.setCertificatesJson(buildCertificatesJson(certs));
    person.setCertVerified(1);
    person.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(person);

    return new CertVerifyDetail(
        person.getId(),
        person.getName(),
        person.getIdCardNumber(),
        true,
        CertMappingSupport.displayCertLevel(primary),
        primary.certNo(),
        primary.certDate(),
        null);
  }

  @Transactional
  public CertVerifyDetail markFailed(TrainSignUpPeopleSelfEntity person, String errorMessage) {
    person.setCertVerified(3);
    person.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(person);
    return new CertVerifyDetail(
        person.getId(),
        person.getName(),
        person.getIdCardNumber(),
        false,
        null,
        null,
        null,
        errorMessage);
  }

  @Transactional
  public CertVerifyDetail markInvalid(TrainSignUpPeopleSelfEntity person, String errorMessage) {
    person.setCertVerified(3);
    person.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(person);
    return new CertVerifyDetail(
        person.getId(),
        person.getName(),
        person.getIdCardNumber(),
        false,
        null,
        null,
        null,
        errorMessage);
  }

  private String buildCertificatesJson(List<CollectedCert> certs) {
    try {
      List<Map<String, String>> payload = new ArrayList<>();
      for (CollectedCert cert : certs) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("level", CertMappingSupport.extractLevelText(cert.certName()));
        item.put("certNo", defaultString(cert.certNo()));
        item.put("issueDate", defaultString(cert.certDate()));
        payload.add(item);
      }
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("组装 certificates_json 失败", e);
      return null;
    }
  }

  private static String defaultString(String value) {
    return value != null ? value : "";
  }
}
