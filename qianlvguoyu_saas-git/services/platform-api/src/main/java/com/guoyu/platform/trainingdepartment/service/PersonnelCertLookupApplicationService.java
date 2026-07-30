package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.chazheng.service.CertQueryOutcome;
import com.guoyu.platform.chazheng.service.ChazhengCertQueryService;
import com.guoyu.platform.chazheng.service.CollectedCert;
import com.guoyu.platform.chazheng.support.CertMappingSupport;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.PersonnelCertLookupPerson;
import com.guoyu.platform.trainingdepartment.dto.PersonnelCertLookupReq;
import com.guoyu.platform.trainingdepartment.vo.PersonnelCertLookupDetail;
import com.guoyu.platform.trainingdepartment.vo.PersonnelCertLookupResultVO;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 方案规划「人员认定」：按姓名+身份证查证，不落库，返回明细供前端写回 Excel。
 */
@Service
@Slf4j
public class PersonnelCertLookupApplicationService {

  private final ChazhengCertQueryService chazhengCertQueryService;

  public PersonnelCertLookupApplicationService(ChazhengCertQueryService chazhengCertQueryService) {
    this.chazhengCertQueryService = chazhengCertQueryService;
  }

  public PersonnelCertLookupResultVO lookup(PersonnelCertLookupReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<PersonnelCertLookupDetail> details = new ArrayList<>();
    int verified = 0;
    int notFound = 0;
    int invalid = 0;
    int failed = 0;

    for (PersonnelCertLookupPerson person : req.people()) {
      String name = person.name() != null ? person.name().trim() : "";
      String idCard = person.idCard() != null ? person.idCard().trim() : "";

      if (!StringUtils.hasText(name) || !StringUtils.hasText(idCard)) {
        details.add(detail(person, name, idCard, "invalid", null, null, null, "姓名或身份证为空"));
        invalid++;
        continue;
      }
      if (!CertMappingSupport.isValidIdCard(idCard)) {
        details.add(detail(person, name, idCard, "invalid", null, null, null, "身份证格式无效"));
        invalid++;
        continue;
      }

      try {
        CertQueryOutcome outcome = chazhengCertQueryService.queryByNameIdCard(name, idCard, tenantId);
        PersonnelCertLookupDetail d = toDetail(person, name, idCard, outcome);
        details.add(d);
        switch (d.status()) {
          case "verified" -> verified++;
          case "not_found" -> notFound++;
          default -> failed++;
        }
      } catch (BusinessException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "查证失败";
        if (msg.contains("查证接口未配置") || msg.contains("查证请求模板未配置")) {
          throw e;
        }
        log.warn("[personnel-cert-lookup] 业务异常 name={} err={}", name, msg);
        details.add(detail(person, name, idCard, "failed", null, null, null, msg));
        failed++;
      } catch (Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "查证失败";
        log.error("[personnel-cert-lookup] 查证失败 name={} err={}", name, msg, e);
        details.add(detail(person, name, idCard, "failed", null, null, null, msg));
        failed++;
      }
    }

    return new PersonnelCertLookupResultVO(
        details.size(), verified, notFound, invalid, failed, details);
  }

  private static PersonnelCertLookupDetail toDetail(
      PersonnelCertLookupPerson person,
      String name,
      String idCard,
      CertQueryOutcome outcome) {
    if (!outcome.anyNormal()) {
      return detail(person, name, idCard, "failed", null, null, null, "查证请求异常，请重试");
    }
    if (!outcome.anyCertFound() || outcome.certs().isEmpty()) {
      return detail(person, name, idCard, "not_found", null, null, null, "未查到证书信息");
    }
    CollectedCert primary = CertMappingSupport.pickPrimaryCert(outcome.certs());
    return detail(
        person,
        name,
        idCard,
        "verified",
        CertMappingSupport.displayCertLevel(primary),
        primary.certNo(),
        primary.certDate(),
        null);
  }

  private static PersonnelCertLookupDetail detail(
      PersonnelCertLookupPerson person,
      String name,
      String idCard,
      String status,
      String certLevel,
      String certNo,
      String issueDate,
      String message) {
    return new PersonnelCertLookupDetail(
        person.rowIndex(),
        name,
        idCard,
        status,
        certLevel,
        certNo,
        issueDate,
        message);
  }
}
