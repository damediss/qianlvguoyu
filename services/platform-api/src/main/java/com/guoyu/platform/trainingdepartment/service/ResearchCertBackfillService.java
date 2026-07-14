package com.guoyu.platform.trainingdepartment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.service.CertQueryOutcome;
import com.guoyu.platform.chazheng.service.CollectedCert;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.CertVerifyDetail;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ResearchCertBackfillService {

  private static final String[] LEVEL_LABELS = {"初级工", "中级工", "高级工", "技师", "高级技师"};
  private static final Pattern LEVEL_IN_PARENS = Pattern.compile("[（(]([^）)]+)[）)]");

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
    CollectedCert primary = pickPrimaryCert(certs);
    String levelText = extractLevelText(primary.certName());

    person.setPaperNo(primary.certNo());
    person.setCertIssueDates(primary.certDate());
    person.setLevel(mapLevelText(levelText));
    person.setCertificatesJson(buildCertificatesJson(certs));
    person.setCertVerified(1);
    person.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(person);

    return new CertVerifyDetail(
        person.getId(),
        person.getName(),
        person.getIdCardNumber(),
        true,
        StringUtils.hasText(levelText) ? levelText : primary.certName(),
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

  private CollectedCert pickPrimaryCert(List<CollectedCert> certs) {
    return certs.stream()
        .max(Comparator.comparingInt(c -> {
          Integer level = mapLevelText(extractLevelText(c.certName()));
          return level != null ? level : 0;
        }))
        .orElse(certs.get(0));
  }

  private String buildCertificatesJson(List<CollectedCert> certs) {
    try {
      List<Map<String, String>> payload = new ArrayList<>();
      for (CollectedCert cert : certs) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("level", extractLevelText(cert.certName()));
        item.put("certNo", defaultString(cert.certNo()));
        item.put("issueDate", defaultString(cert.certDate()));
        payload.add(item);
      }
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("组装 certificates_json 失败 peopleId={}", certs.isEmpty() ? null : "batch", e);
      return null;
    }
  }

  static String extractLevelText(String certName) {
    if (!StringUtils.hasText(certName)) {
      return "";
    }
    Matcher matcher = LEVEL_IN_PARENS.matcher(certName);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return certName.trim();
  }

  static Integer mapLevelText(String text) {
    if (!StringUtils.hasText(text)) {
      return null;
    }
    String t = text.trim();
    return switch (t) {
      case "一级", "初级", "初级工" -> 1;
      case "二级", "中级", "中级工" -> 2;
      case "三级", "高级", "高级工" -> 3;
      case "四级", "技师" -> 4;
      case "五级", "高级技师" -> 5;
      default -> {
        for (int i = 0; i < LEVEL_LABELS.length; i++) {
          if (LEVEL_LABELS[i].equals(t)) {
            yield i + 1;
          }
        }
        yield null;
      }
    };
  }

  private static String defaultString(String value) {
    return value != null ? value : "";
  }
}
