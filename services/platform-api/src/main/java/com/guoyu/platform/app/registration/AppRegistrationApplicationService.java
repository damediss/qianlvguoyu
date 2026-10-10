package com.guoyu.platform.app.registration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.app.auth.AppPlatformAuthService;
import com.guoyu.platform.app.auth.AppPlatformUser;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 小程序自主报名（对齐 PHP AppregistrationController） */
@Service
@Slf4j
public class AppRegistrationApplicationService {

  private static final int PAGE_SIZE = 20;

  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final OrgMapper orgMapper;
  private final AppPlatformAuthService appPlatformAuthService;
  private final AppRegistrationFormSupport formSupport;
  private final String cdnUrl;

  public AppRegistrationApplicationService(
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      OrgMapper orgMapper,
      AppPlatformAuthService appPlatformAuthService,
      AppRegistrationFormSupport formSupport,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.orgMapper = orgMapper;
    this.appPlatformAuthService = appPlatformAuthService;
    this.formSupport = formSupport;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> selfList(int page) {
    int currentPage = page > 0 ? page : 1;
    Page<TrainSignUpSelfEntity> result = signUpMapper.selectPage(
        new Page<>(currentPage, PAGE_SIZE),
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getDeleted, 0)
            .orderByAsc(TrainSignUpSelfEntity::getSortNumber)
            .orderByDesc(TrainSignUpSelfEntity::getCreateTime));
    Map<Long, String> orgNames = resolveOrgNames(result.getRecords());
    List<Map<String, Object>> selflist = result.getRecords().stream()
        .map(item -> toAppSignUpMap(item, orgNames.get(item.getOrgId())))
        .toList();
    return Map.of("selflist", selflist);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> selfInfo(Long signUpId, String token) {
    if (signUpId == null || signUpId <= 0) {
      throw new AppRegistrationException(-1, "非法请求");
    }
    TrainSignUpSelfEntity signUp = signUpMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getId, signUpId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUp == null) {
      throw new AppRegistrationException(-1, "非法请求");
    }

    Map<String, Object> data = new LinkedHashMap<>();
    String orgName = resolveOrgName(signUp.getOrgId());
    data.put("info", toAppSignUpMap(signUp, orgName));

    AppPlatformUser user = appPlatformAuthService.resolveUser(token);
    if (user != null && StringUtils.hasText(user.idcard())) {
      TrainSignUpPeopleSelfEntity existing = peopleMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, user.idcard())
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (existing != null) {
        data.put("formData", formSupport.entityToFormMap(existing));
      } else if (user.id() != null && user.id() > 0) {
        Map<String, Object> detail = platformUserDetailMapper.selectMapByIdcard(user.idcard());
        if (detail != null && !detail.isEmpty()) {
          data.put("formData", formSupport.buildPrefillFromUserDetail(
              signUpId,
              user.id(),
              user.idcard(),
              detail,
              signUp.getOrgId(),
              resolveCompanyName(signUp, orgName)));
        }
      }
    }

    Map<String, Object> workTypes = buildWorkTypeLists();
    data.put("workList", workTypes.get("workList"));
    data.put("workListID", workTypes.get("workListID"));
    return data;
  }

  @Transactional
  public Map<String, Object> submit(Long signUpId, Map<String, Object> formData, String token) {
    if (!StringUtils.hasText(token)) {
      throw new AppRegistrationException(1, "请先登录");
    }
    AppPlatformUser user = appPlatformAuthService.resolveUser(token);
    if (user == null || user.id() == null) {
      throw new AppRegistrationException(1, "请先登录");
    }
    if (signUpId == null || signUpId <= 0 || formData == null || formData.isEmpty()) {
      throw new AppRegistrationException(1, "参数不合法", Map.of("post", Map.of("id", signUpId, "formData", formData)));
    }
    String idCardNumber = stringVal(formData.get("id_card_number"));
    if (!StringUtils.hasText(idCardNumber)) {
      throw new AppRegistrationException(1, "身份证号码为空", Map.of("post", Map.of("id", signUpId, "formData", formData)));
    }

    TrainSignUpSelfEntity signUp = signUpMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getId, signUpId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUp == null) {
      throw new AppRegistrationException(1, "参数不合法");
    }

    formData.put("platform_user_id", user.id());
    List<String> tips = formSupport.validateRequiredFields(formData);
    int status = tips.isEmpty() ? 1 : 0;
    if (status == 1) {
      formData.put("status", status);
    }

    Long existingId = longVal(formData.get("id"));
    boolean updated;
    TrainSignUpPeopleSelfEntity entity;
    if (existingId != null && existingId > 0) {
      entity = peopleMapper.selectById(existingId);
      if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
        throw new AppRegistrationException(1, "失败");
      }
      formSupport.applyFormData(entity, formData);
      if (status == 1) {
        entity.setStatus(status);
      }
      entity.setUpdateTime(LocalDateTime.now());
      updated = peopleMapper.updateById(entity) > 0;
      formData.put("id", entity.getId());
    } else {
      TrainSignUpPeopleSelfEntity duplicate = peopleMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCardNumber)
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
              .last("LIMIT 1"));
      if (duplicate == null) {
        entity = new TrainSignUpPeopleSelfEntity();
        entity.setSignUpId(signUpId);
        entity.setTenantId(signUp.getTenantId() != null ? signUp.getTenantId() : 1L);
        entity.setCompanyId(signUp.getOrgId());
        entity.setCompanyName(resolveCompanyName(signUp, resolveOrgName(signUp.getOrgId())));
        entity.setStatus(status);
        entity.setCompanyStatus(0);
        entity.setHasPay(0);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        formSupport.applyFormData(entity, formData);
        if (status == 1) {
          entity.setStatus(status);
        }
        entity.setUpdateTime(LocalDateTime.now());
        updated = peopleMapper.insert(entity) > 0;
        formData.put("id", entity.getId());
      } else {
        entity = duplicate;
        formSupport.applyFormData(entity, formData);
        if (status == 1) {
          entity.setStatus(status);
        }
        entity.setUpdateTime(LocalDateTime.now());
        updated = peopleMapper.updateById(entity) > 0;
        formData.put("id", entity.getId());
      }
    }

    if (!updated) {
      throw new AppRegistrationException(1, "失败", Map.of("post", Map.of("id", signUpId, "formData", formData)));
    }

    formData.put("work_exp", formSupport.entityToFormMap(entity).get("work_exp"));
    String msg = tips.isEmpty() ? "提交成功" : "保存成功";
    return Map.of(
        "formData", formData,
        "tips", tips,
        "status", status,
        "msg", msg);
  }

  private Map<String, Object> buildWorkTypeLists() {
    List<ExamWorkTypeEntity> workTypes = examWorkTypeMapper.selectList(
        new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .eq(ExamWorkTypeEntity::getDeleted, 0)
            .orderByAsc(ExamWorkTypeEntity::getParentId)
            .orderByAsc(ExamWorkTypeEntity::getSortNumber));
    Map<String, String> workList = new LinkedHashMap<>();
    Map<String, Map<String, String>> workListId = new LinkedHashMap<>();
    for (ExamWorkTypeEntity wt : workTypes) {
      if (wt.getParentId() != null && wt.getParentId() > 0) {
        workListId
            .computeIfAbsent(String.valueOf(wt.getParentId()), key -> new LinkedHashMap<>())
            .put(String.valueOf(wt.getId()), wt.getTitle());
      } else {
        workList.put(String.valueOf(wt.getId()), wt.getTitle());
      }
    }
    return Map.of("workList", workList, "workListID", workListId);
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
    return orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>().in(OrgEntity::getId, orgIds))
        .stream()
        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getName, (a, b) -> a));
  }

  private String resolveOrgName(Long orgId) {
    if (orgId == null || orgId <= 0) {
      return "";
    }
    OrgEntity org = orgMapper.selectById(orgId);
    return org != null && StringUtils.hasText(org.getName()) ? org.getName() : "";
  }

  private String resolveCompanyName(TrainSignUpSelfEntity signUp, String orgName) {
    if (StringUtils.hasText(orgName)) {
      return orgName;
    }
    return signUp.getTitle() != null ? signUp.getTitle() : "";
  }

  private Map<String, Object> toAppSignUpMap(TrainSignUpSelfEntity signUp, String orgName) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", signUp.getId());
    map.put("title", signUp.getTitle());
    map.put("cover", toAbsoluteUrl(signUp.getCover()));
    map.put("train_time", signUp.getTrainTime());
    map.put("exam_time", signUp.getExamTime());
    map.put("org_id", signUp.getOrgId());
    map.put("train_info_id", signUp.getTrainInfoId());
    map.put("sort_number", signUp.getSortNumber());
    map.put("company", orgName);
    map.put("company_id", signUp.getOrgId());
    map.put("company_name", orgName);
    map.put("subtitle", "");
    map.put("intro", "");
    map.put("create_time", signUp.getCreateTime());
    map.put("update_time", signUp.getUpdateTime());
    return map;
  }

  private String toAbsoluteUrl(String path) {
    if (!StringUtils.hasText(path)) {
      return path;
    }
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return cdnUrl + "/" + normalized;
  }

  private static String stringVal(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value).trim();
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
