package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchCollectionCreatePlanReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCertificateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonUpdateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainThreeYearPlanCreateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainResearchOrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainResearchOrgMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMapper;
import com.guoyu.platform.trainingdepartment.support.ResearchCollectionTemplateExportService;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchCollectionVO;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchPersonCertificateVO;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchPersonVO;
import com.guoyu.platform.trainingdepartment.vo.TrainThreeYearPlanVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class TrainResearchCollectionApplicationService {

  private static final String COLLECTION_SOURCE_RESEARCH = "research";
  private static final String[] LEVEL_LABELS = {"初级工", "中级工", "高级工", "技师", "高级技师"};

  private final TrainResearchOrgMapper researchOrgMapper;
  private final TrainSignUpSelfMapper signUpMapper;
  private final TrainSignUpPeopleSelfMapper peopleMapper;
  private final OrgMapper orgMapper;
  private final TrainThreeYearPlanApplicationService threeYearPlanService;
  private final TrainThreeYearPlanMapper planMapper;
  private final ResearchCollectionTemplateExportService exportService;
  private final ObjectMapper objectMapper;

  public TrainResearchCollectionApplicationService(
      TrainResearchOrgMapper researchOrgMapper,
      TrainSignUpSelfMapper signUpMapper,
      TrainSignUpPeopleSelfMapper peopleMapper,
      OrgMapper orgMapper,
      TrainThreeYearPlanApplicationService threeYearPlanService,
      TrainThreeYearPlanMapper planMapper,
      ResearchCollectionTemplateExportService exportService,
      ObjectMapper objectMapper) {
    this.researchOrgMapper = researchOrgMapper;
    this.signUpMapper = signUpMapper;
    this.peopleMapper = peopleMapper;
    this.orgMapper = orgMapper;
    this.threeYearPlanService = threeYearPlanService;
    this.planMapper = planMapper;
    this.exportService = exportService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public TrainResearchCollectionVO getCollection(Long researchOrgId) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    OrgEntity targetOrg = resolveTargetOrg(research);
    TrainSignUpSelfEntity signUp = resolveActiveSignUp(research, false);
    return toCollectionVO(research, targetOrg, signUp);
  }

  @Transactional
  public TrainResearchCollectionVO startCollection(Long researchOrgId) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    if (research.getTargetOrgId() == null || research.getTargetOrgId() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先在调研企业中关联系统企业");
    }
    OrgEntity targetOrg = resolveTargetOrg(research);

    TrainSignUpSelfEntity existing = resolveActiveSignUp(research, false);
    if (existing != null) {
      return toCollectionVO(research, targetOrg, existing);
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity signUp = new TrainSignUpSelfEntity();
    signUp.setTitle(research.getTitle() + "人员采集");
    signUp.setOrgId(research.getTargetOrgId());
    signUp.setResearchOrgId(researchOrgId);
    signUp.setCollectionSource(COLLECTION_SOURCE_RESEARCH);
    signUp.setQrcodeToken(UUID.randomUUID().toString().replace("-", ""));
    signUp.setSortNumber(100);
    signUp.setTenantId(tenantId);
    signUp.setCreateTime(LocalDateTime.now());
    signUp.setUpdateTime(LocalDateTime.now());
    signUpMapper.insert(signUp);

    research.setActiveSignUpId(signUp.getId());
    if (research.getPersonnelCount() == null) {
      research.setPersonnelCount(0);
    }
    researchOrgMapper.updateById(research);

    return toCollectionVO(research, targetOrg, signUp);
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainResearchPersonVO> pagePeople(Long researchOrgId, TrainResearchPersonQueryReq req) {
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
        .orderByDesc(TrainSignUpPeopleSelfEntity::getId);
    if (StringUtils.hasText(req.keyword())) {
      wrapper.and(w -> w.like(TrainSignUpPeopleSelfEntity::getName, req.keyword())
          .or().like(TrainSignUpPeopleSelfEntity::getIdCardNumber, req.keyword())
          .or().like(TrainSignUpPeopleSelfEntity::getPhone, req.keyword()));
    }
    Page<TrainSignUpPeopleSelfEntity> page = peopleMapper.selectPage(
        new Page<>(req.page(), req.pageSize()), wrapper);
    List<TrainResearchPersonVO> list = page.getRecords().stream().map(this::toPersonVO).toList();
    return new PageResponse<>(list, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public TrainResearchPersonVO createPerson(Long researchOrgId, TrainResearchPersonCreateReq req) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    if (!StringUtils.hasText(req.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "姓名不能为空");
    }
    if (StringUtils.hasText(req.idCardNumber())) {
      assertIdCardUnique(signUp.getId(), req.idCardNumber(), null);
    }

    TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
    entity.setSignUpId(signUp.getId());
    entity.setTenantId(SecurityContext.currentUser().getTenantId());
    entity.setDataSource("admin");
    applyPersonFields(entity, req, research.getTitle());
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    peopleMapper.insert(entity);
    refreshPersonnelCount(researchOrgId);
    return toPersonVO(entity);
  }

  @Transactional
  public TrainResearchPersonVO updatePerson(Long researchOrgId, Long peopleId, TrainResearchPersonUpdateReq req) {
    requireResearchOrg(researchOrgId);
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    TrainSignUpPeopleSelfEntity entity = requirePerson(signUp.getId(), peopleId);
    if (StringUtils.hasText(req.idCardNumber())) {
      assertIdCardUnique(signUp.getId(), req.idCardNumber(), peopleId);
    }
    applyPersonFields(entity, req, entity.getCompanyName());
    entity.setUpdateTime(LocalDateTime.now());
    peopleMapper.updateById(entity);
    return toPersonVO(entity);
  }

  @Transactional
  public void deletePerson(Long researchOrgId, Long peopleId) {
    requireResearchOrg(researchOrgId);
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    TrainSignUpPeopleSelfEntity entity = requirePerson(signUp.getId(), peopleId);
    peopleMapper.deleteById(entity.getId());
    refreshPersonnelCount(researchOrgId);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportCollectionTemplate(Long researchOrgId) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    List<TrainSignUpPeopleSelfEntity> people = listAllPeople(signUp.getId());
    if (people.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无人员数据可导出");
    }
    byte[] bytes = exportService.export(people, research.getTitle());
    String filename = research.getTitle() + "-认定采集精简版.xlsx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @Transactional
  public TrainThreeYearPlanVO createThreeYearPlan(Long researchOrgId, TrainResearchCollectionCreatePlanReq req) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    if (research.getTargetOrgId() == null || research.getTargetOrgId() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先在调研企业中关联系统企业");
    }
    TrainSignUpSelfEntity signUp = requireActiveSignUp(researchOrgId);
    List<TrainSignUpPeopleSelfEntity> people = listAllPeople(signUp.getId());
    if (people.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先录入人员信息");
    }

    byte[] bytes = exportService.export(people, research.getTitle());
    String planTitle = StringUtils.hasText(req != null ? req.title() : null)
        ? req.title().trim()
        : research.getTitle() + "三年规划";
    MultipartFile file = new InMemoryMultipartFile(
        "collection.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        bytes);
    TrainThreeYearPlanCreateReq createReq = new TrainThreeYearPlanCreateReq(
        planTitle, research.getTargetOrgId(), researchOrgId);
    TrainThreeYearPlanVO plan = threeYearPlanService.createWithFile(createReq, file);

    if (plan.id() != null) {
      TrainThreeYearPlanEntity entity = planMapper.selectById(plan.id());
      if (entity != null) {
        entity.setSourceSignUpId(signUp.getId());
        planMapper.updateById(entity);
      }
    }
    return plan;
  }

  private TrainResearchOrgEntity requireResearchOrg(Long researchOrgId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainResearchOrgEntity entity = researchOrgMapper.selectOne(
        new LambdaQueryWrapper<TrainResearchOrgEntity>()
            .eq(TrainResearchOrgEntity::getId, researchOrgId)
            .eq(TrainResearchOrgEntity::getTenantId, tenantId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "调研企业不存在");
    }
    return entity;
  }

  private OrgEntity resolveTargetOrg(TrainResearchOrgEntity research) {
    if (research.getTargetOrgId() == null) {
      return null;
    }
    return orgMapper.selectById(research.getTargetOrgId());
  }

  private TrainSignUpSelfEntity resolveActiveSignUp(TrainResearchOrgEntity research, boolean required) {
    if (research.getActiveSignUpId() == null) {
      if (required) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请先开启人员采集");
      }
      return null;
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity signUp = signUpMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getId, research.getActiveSignUpId())
            .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (signUp == null && required) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "采集批次不存在，请重新开启采集");
    }
    return signUp;
  }

  private TrainSignUpSelfEntity requireActiveSignUp(Long researchOrgId) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    TrainSignUpSelfEntity signUp = resolveActiveSignUp(research, true);
    if (!COLLECTION_SOURCE_RESEARCH.equals(signUp.getCollectionSource())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前批次不是企业调研采集");
    }
    return signUp;
  }

  private TrainSignUpPeopleSelfEntity requirePerson(Long signUpId, Long peopleId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpPeopleSelfEntity entity = peopleMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getId, peopleId)
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "人员不存在");
    }
    return entity;
  }

  private void assertIdCardUnique(Long signUpId, String idCardNumber, Long excludeId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCardNumber.trim())
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0);
    if (excludeId != null) {
      wrapper.ne(TrainSignUpPeopleSelfEntity::getId, excludeId);
    }
    if (peopleMapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该身份证号已存在");
    }
  }

  private List<TrainSignUpPeopleSelfEntity> listAllPeople(Long signUpId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return peopleMapper.selectList(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
            .orderByAsc(TrainSignUpPeopleSelfEntity::getId));
  }

  private void refreshPersonnelCount(Long researchOrgId) {
    TrainResearchOrgEntity research = requireResearchOrg(researchOrgId);
    if (research.getActiveSignUpId() == null) {
      research.setPersonnelCount(0);
      researchOrgMapper.updateById(research);
      return;
    }
    long count = peopleMapper.selectCount(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, research.getActiveSignUpId())
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    research.setPersonnelCount((int) count);
    researchOrgMapper.updateById(research);
  }

  private void applyPersonFields(TrainSignUpPeopleSelfEntity entity, TrainResearchPersonCreateReq req, String defaultCompany) {
    entity.setName(trim(req.name()));
    entity.setIdCardNumber(trim(req.idCardNumber()));
    entity.setPhone(trim(req.phone()));
    entity.setCompanyName(firstNonBlank(trim(req.companyName()), defaultCompany));
    entity.setMajor(trim(req.major()));
    entity.setEducation(trim(req.education()));
    entity.setWorkTime(trim(req.workTime()));
    entity.setWorkTypeName(trim(req.workTypeName()));
    entity.setDepartment(trim(req.department()));
    entity.setJob(trim(req.job()));
    entity.setWorkDateStart(trim(req.workDateStart()));
    entity.setTitleType(trim(req.titleType()));
    entity.setTitleMajor(trim(req.titleMajor()));
    entity.setTitleCertDate(trim(req.titleCertDate()));
    applyCertificates(entity, req.certificates(), req.level(), req.paperNo(), req.certIssueDates());
  }

  private void applyPersonFields(TrainSignUpPeopleSelfEntity entity, TrainResearchPersonUpdateReq req, String defaultCompany) {
    if (req.name() != null) entity.setName(trim(req.name()));
    if (req.idCardNumber() != null) entity.setIdCardNumber(trim(req.idCardNumber()));
    if (req.phone() != null) entity.setPhone(trim(req.phone()));
    if (req.companyName() != null) entity.setCompanyName(trim(req.companyName()));
    else if (!StringUtils.hasText(entity.getCompanyName())) entity.setCompanyName(defaultCompany);
    if (req.major() != null) entity.setMajor(trim(req.major()));
    if (req.education() != null) entity.setEducation(trim(req.education()));
    if (req.workTime() != null) entity.setWorkTime(trim(req.workTime()));
    if (req.workTypeName() != null) entity.setWorkTypeName(trim(req.workTypeName()));
    if (req.department() != null) entity.setDepartment(trim(req.department()));
    if (req.job() != null) entity.setJob(trim(req.job()));
    if (req.workDateStart() != null) entity.setWorkDateStart(trim(req.workDateStart()));
    if (req.titleType() != null) entity.setTitleType(trim(req.titleType()));
    if (req.titleMajor() != null) entity.setTitleMajor(trim(req.titleMajor()));
    if (req.titleCertDate() != null) entity.setTitleCertDate(trim(req.titleCertDate()));
    if (req.certificates() != null || req.level() != null || req.paperNo() != null || req.certIssueDates() != null) {
      applyCertificates(entity, req.certificates(), req.level(), req.paperNo(), req.certIssueDates());
    }
  }

  private void applyCertificates(
      TrainSignUpPeopleSelfEntity entity,
      List<TrainResearchPersonCertificateReq> certificates,
      Integer level,
      String paperNo,
      String certIssueDates) {
    if (certificates != null && !certificates.isEmpty()) {
      List<Map<String, String>> payload = new ArrayList<>();
      List<String> levels = new ArrayList<>();
      List<String> nos = new ArrayList<>();
      List<String> dates = new ArrayList<>();
      for (TrainResearchPersonCertificateReq cert : certificates) {
        if (!StringUtils.hasText(cert.level()) && !StringUtils.hasText(cert.certNo())
            && !StringUtils.hasText(cert.issueDate())) {
          continue;
        }
        Map<String, String> item = new LinkedHashMap<>();
        item.put("level", defaultString(cert.level()));
        item.put("certNo", defaultString(cert.certNo()));
        item.put("issueDate", defaultString(cert.issueDate()));
        payload.add(item);
        levels.add(defaultString(cert.level()));
        nos.add(defaultString(cert.certNo()));
        dates.add(defaultString(cert.issueDate()));
      }
      try {
        entity.setCertificatesJson(objectMapper.writeValueAsString(payload));
      } catch (Exception e) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "证书信息保存失败");
      }
      entity.setCertIssueDates(String.join("\n", dates));
      entity.setPaperNo(nos.isEmpty() ? null : nos.get(0));
      entity.setLevel(textToLevel(levels.isEmpty() ? null : levels.get(0)));
      return;
    }
    if (level != null) entity.setLevel(level);
    if (paperNo != null) entity.setPaperNo(trim(paperNo));
    if (certIssueDates != null) entity.setCertIssueDates(trim(certIssueDates));
  }

  private TrainResearchCollectionVO toCollectionVO(
      TrainResearchOrgEntity research, OrgEntity targetOrg, TrainSignUpSelfEntity signUp) {
    String status = signUp == null ? "not_started" : "active";
    int count = research.getPersonnelCount() != null ? research.getPersonnelCount() : 0;
    if (signUp != null && count == 0) {
      Long peopleCount = peopleMapper.selectCount(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
      count = peopleCount != null ? peopleCount.intValue() : 0;
    }
    return new TrainResearchCollectionVO(
        research.getId(),
        research.getTitle(),
        research.getTargetOrgId(),
        targetOrg != null ? targetOrg.getName() : null,
        signUp != null ? signUp.getId() : null,
        status,
        count,
        signUp != null ? signUp.getQrcodeToken() : null,
        false,
        signUp != null ? signUp.getCreateTime() : null);
  }

  private TrainResearchPersonVO toPersonVO(TrainSignUpPeopleSelfEntity entity) {
    return new TrainResearchPersonVO(
        entity.getId(),
        entity.getSignUpId(),
        entity.getName(),
        entity.getIdCardNumber(),
        entity.getPhone(),
        entity.getCompanyName(),
        entity.getMajor(),
        entity.getEducation(),
        entity.getWorkTime(),
        entity.getWorkTypeName(),
        entity.getDepartment(),
        firstNonBlank(entity.getJob(), entity.getJog()),
        entity.getWorkDateStart(),
        entity.getTitleType(),
        entity.getTitleMajor(),
        entity.getTitleCertDate(),
        entity.getLevel(),
        entity.getPaperNo(),
        entity.getCertIssueDates(),
        parseCertificateVOs(entity.getCertificatesJson()),
        entity.getDataSource(),
        entity.getCreateTime(),
        entity.getUpdateTime());
  }

  private List<TrainResearchPersonCertificateVO> parseCertificateVOs(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
      List<TrainResearchPersonCertificateVO> result = new ArrayList<>();
      for (Map<String, Object> item : raw) {
        result.add(new TrainResearchPersonCertificateVO(
            asString(item.get("level")),
            asString(firstNonNull(item.get("certNo"), item.get("cert_no"))),
            asString(firstNonNull(item.get("issueDate"), item.get("issue_date")))));
      }
      return result;
    } catch (Exception e) {
      return List.of();
    }
  }

  private static Integer textToLevel(String levelText) {
    if (!StringUtils.hasText(levelText)) {
      return null;
    }
    for (int i = 0; i < LEVEL_LABELS.length; i++) {
      if (LEVEL_LABELS[i].equals(levelText.trim())) {
        return i + 1;
      }
    }
    return null;
  }

  private static String trim(String value) {
    return value != null ? value.trim() : null;
  }

  private static String defaultString(String value) {
    return value != null ? value : "";
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private static Object firstNonNull(Object a, Object b) {
    return a != null ? a : b;
  }

  private static String asString(Object value) {
    return value != null ? String.valueOf(value) : "";
  }

  private static final class InMemoryMultipartFile implements MultipartFile {
    private final String filename;
    private final String contentType;
    private final byte[] content;

    private InMemoryMultipartFile(String filename, String contentType, byte[] content) {
      this.filename = filename;
      this.contentType = contentType;
      this.content = content;
    }

    @Override
    public String getName() { return "file"; }

    @Override
    public String getOriginalFilename() { return filename; }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public boolean isEmpty() { return content.length == 0; }

    @Override
    public long getSize() { return content.length; }

    @Override
    public byte[] getBytes() { return content; }

    @Override
    public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }

    @Override
    public void transferTo(java.io.File dest) {
      throw new UnsupportedOperationException();
    }
  }
}
