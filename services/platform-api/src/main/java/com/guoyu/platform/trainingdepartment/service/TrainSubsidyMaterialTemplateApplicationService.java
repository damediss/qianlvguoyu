package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.SubsidyMaterialFillFromTemplateReq;
import com.guoyu.platform.trainingdepartment.dto.SubsidyMaterialTemplateUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialInstanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialTemplateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialVersionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialInstanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialTemplateMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.support.SubsidyCertifiedPeopleSupport;
import com.guoyu.platform.trainingdepartment.support.SubsidyMaterialTypes;
import com.guoyu.platform.trainingdepartment.support.SubsidyTemplatePlaceholderParser;
import com.guoyu.platform.trainingdepartment.support.SubsidyXlsxLabelFiller;
import com.guoyu.platform.trainingdepartment.support.SubsidyXlsxRosterFiller;
import com.guoyu.platform.trainingdepartment.support.TrainingDocxVariableReplacer;
import com.guoyu.platform.trainingdepartment.support.TrainingXlsxVariableReplacer;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialTemplateVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 补贴材料模板库 + 选模板自动填数生成。
 * 支持 docx/xlsx 标量占位符；xlsx 花名册支持原型行占位符或中文表头逐行填学员。
 */
@Service
@Slf4j
public class TrainSubsidyMaterialTemplateApplicationService {

  private static final String DOCX =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String XLS = "application/vnd.ms-excel";

  private final TrainSubsidyMaterialTemplateMapper templateMapper;
  private final TrainSubsidyMapper subsidyMapper;
  private final TrainSubsidyMaterialInstanceMapper instanceMapper;
  private final TrainSubsidyMaterialVersionMapper versionMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final TrainInfoClassMapper classMapper;
  private final TrainInfoListMapper infoListMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;
  private final TrainCertificateMapper certificateMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;
  private final TrainSubsidyMaterialTemplateApplicationService self;

  public TrainSubsidyMaterialTemplateApplicationService(
      TrainSubsidyMaterialTemplateMapper templateMapper,
      TrainSubsidyMapper subsidyMapper,
      TrainSubsidyMaterialInstanceMapper instanceMapper,
      TrainSubsidyMaterialVersionMapper versionMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainSubsidyEventMapper eventMapper,
      TrainInfoClassMapper classMapper,
      TrainInfoListMapper infoListMapper,
      TrainPeopleListMapper peopleListMapper,
      OrgMapper orgMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper,
      TrainCertificateMapper certificateMapper,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper,
      @Lazy TrainSubsidyMaterialTemplateApplicationService self) {
    this.templateMapper = templateMapper;
    this.subsidyMapper = subsidyMapper;
    this.instanceMapper = instanceMapper;
    this.versionMapper = versionMapper;
    this.slotFileMapper = slotFileMapper;
    this.eventMapper = eventMapper;
    this.classMapper = classMapper;
    this.infoListMapper = infoListMapper;
    this.peopleListMapper = peopleListMapper;
    this.orgMapper = orgMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
    this.certificateMapper = certificateMapper;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
    this.self = self;
  }

  @Transactional(readOnly = true)
  public List<SubsidyMaterialTemplateVO> list(
      String scope,
      String materialType,
      String regionCode,
      String trainMode,
      Integer policyYear,
      String status,
      Long classId,
      String keyword) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSubsidyMaterialTemplateEntity> q =
        new LambdaQueryWrapper<TrainSubsidyMaterialTemplateEntity>()
            .eq(TrainSubsidyMaterialTemplateEntity::getDeleted, 0)
            .orderByDesc(TrainSubsidyMaterialTemplateEntity::getId);

    String st = StringUtils.hasText(status) ? status.trim() : "active";
    if (!"all".equalsIgnoreCase(st)) {
      q.eq(TrainSubsidyMaterialTemplateEntity::getStatus, st);
    }
    if (StringUtils.hasText(materialType)) {
      q.eq(TrainSubsidyMaterialTemplateEntity::getMaterialType, materialType.trim());
    }
    if (StringUtils.hasText(regionCode)) {
      q.and(w -> w.eq(TrainSubsidyMaterialTemplateEntity::getRegionCode, regionCode.trim())
          .or()
          .isNull(TrainSubsidyMaterialTemplateEntity::getRegionCode)
          .or()
          .eq(TrainSubsidyMaterialTemplateEntity::getRegionCode, ""));
    }
    if (StringUtils.hasText(trainMode)) {
      q.and(w -> w.eq(TrainSubsidyMaterialTemplateEntity::getTrainMode, trainMode.trim())
          .or()
          .isNull(TrainSubsidyMaterialTemplateEntity::getTrainMode)
          .or()
          .eq(TrainSubsidyMaterialTemplateEntity::getTrainMode, ""));
    }
    if (policyYear != null) {
      q.and(w -> w.eq(TrainSubsidyMaterialTemplateEntity::getPolicyYear, policyYear)
          .or()
          .isNull(TrainSubsidyMaterialTemplateEntity::getPolicyYear));
    }
    if (StringUtils.hasText(keyword)) {
      q.like(TrainSubsidyMaterialTemplateEntity::getTitle, keyword.trim());
    }

    // 可见范围：本租户 + 平台；班级覆盖按 classId 过滤
    String sc = StringUtils.hasText(scope) ? scope.trim().toLowerCase(Locale.ROOT) : "";
    if ("platform".equals(sc)) {
      q.eq(TrainSubsidyMaterialTemplateEntity::getScope, "platform");
    } else if ("class".equals(sc)) {
      q.eq(TrainSubsidyMaterialTemplateEntity::getScope, "class");
      if (classId != null) {
        q.eq(TrainSubsidyMaterialTemplateEntity::getClassId, classId);
      }
    } else if ("tenant".equals(sc)) {
      q.eq(TrainSubsidyMaterialTemplateEntity::getScope, "tenant")
          .eq(TrainSubsidyMaterialTemplateEntity::getTenantId, tenantId);
    } else {
      q.and(w -> w.eq(TrainSubsidyMaterialTemplateEntity::getScope, "platform")
          .or(x -> x.eq(TrainSubsidyMaterialTemplateEntity::getScope, "tenant")
              .eq(TrainSubsidyMaterialTemplateEntity::getTenantId, tenantId))
          .or(x -> x.eq(TrainSubsidyMaterialTemplateEntity::getScope, "class")
              .eq(TrainSubsidyMaterialTemplateEntity::getTenantId, tenantId)
              .eq(classId != null, TrainSubsidyMaterialTemplateEntity::getClassId, classId)));
    }

    return templateMapper.selectList(q).stream().map(this::toVO).toList();
  }

  @Transactional(readOnly = true)
  public SubsidyMaterialTemplateVO get(Long id) {
    return toVO(requireTemplate(id));
  }

  @Transactional
  public SubsidyMaterialTemplateVO upload(
      MultipartFile file,
      String title,
      String materialType,
      String slotCode,
      String scope,
      Long classId,
      String regionCode,
      String trainMode,
      Integer policyYear,
      String remark) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传模板文件");
    }
    String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "template.docx";
    String lower = original.toLowerCase(Locale.ROOT);
    if (!lower.endsWith(".docx") && !lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 docx / xlsx 模板");
    }
    String format = lower.endsWith(".docx") ? "docx" : "xlsx";
    String sc = StringUtils.hasText(scope) ? scope.trim().toLowerCase(Locale.ROOT) : "tenant";
    if (!Set.of("platform", "tenant", "class").contains(sc)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "scope 须为 platform/tenant/class");
    }
    if ("class".equals(sc) && classId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级模板须指定 classId");
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    FileVO stored = fileApplicationService.upload(file, "private");
    String path = "/api/files/" + stored.id() + "/download";

    String schemaJson = "[]";
    int parseVersion = 2;
    try {
      byte[] bytes = file.getBytes();
      Set<String> keys = new LinkedHashSet<>(SubsidyTemplatePlaceholderParser.extract(bytes, format));
      if ("xlsx".equals(format) || "xls".equals(format)) {
        keys.addAll(SubsidyXlsxLabelFiller.detectMatchedKeys(bytes));
      }
      schemaJson = objectMapper.writeValueAsString(new ArrayList<>(keys));
    } catch (Exception ex) {
      log.warn("解析模板占位符失败 name={}", original, ex);
      schemaJson = "[]";
    }

    TrainSubsidyMaterialTemplateEntity row = new TrainSubsidyMaterialTemplateEntity();
    row.setScope(sc);
    row.setTenantId("platform".equals(sc) ? null : tenantId);
    row.setClassId("class".equals(sc) ? classId : null);
    row.setMaterialType(
        StringUtils.hasText(materialType) ? materialType.trim() : SubsidyMaterialTypes.APPLICATION_FORM);
    row.setSlotCode(blankToNull(slotCode));
    row.setRegionCode(blankToNull(regionCode));
    row.setTrainMode(blankToNull(trainMode));
    row.setPolicyYear(policyYear);
    row.setTitle(StringUtils.hasText(title) ? title.trim() : stripExt(original));
    row.setFileFormat(format);
    row.setTemplateFileId(stored.id());
    row.setTemplateFilePath(path);
    row.setFieldSchemaJson(schemaJson);
    row.setParseVersion(parseVersion);
    row.setStatus("active");
    row.setRemark(blankToNull(remark));
    row.setCreatedBy(userId);
    row.setUpdatedBy(userId);
    row.setDeleted(0);
    templateMapper.insert(row);
    return toVO(row);
  }

  @Transactional
  public SubsidyMaterialTemplateVO update(Long id, SubsidyMaterialTemplateUpdateReq req) {
    TrainSubsidyMaterialTemplateEntity row = requireOwnedTemplate(id);
    if (req == null) {
      return toVO(row);
    }
    if (StringUtils.hasText(req.getTitle())) {
      row.setTitle(req.getTitle().trim());
    }
    if (StringUtils.hasText(req.getMaterialType())) {
      row.setMaterialType(req.getMaterialType().trim());
    }
    if (req.getSlotCode() != null) {
      row.setSlotCode(blankToNull(req.getSlotCode()));
    }
    if (req.getRegionCode() != null) {
      row.setRegionCode(blankToNull(req.getRegionCode()));
    }
    if (req.getTrainMode() != null) {
      row.setTrainMode(blankToNull(req.getTrainMode()));
    }
    if (req.getPolicyYear() != null) {
      row.setPolicyYear(req.getPolicyYear());
    }
    if (StringUtils.hasText(req.getStatus())) {
      String st = req.getStatus().trim();
      if (!Set.of("draft", "active", "retired").contains(st)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "status 无效");
      }
      row.setStatus(st);
    }
    if (req.getRemark() != null) {
      row.setRemark(blankToNull(req.getRemark()));
    }
    if (StringUtils.hasText(req.getScope())) {
      String sc = req.getScope().trim().toLowerCase(Locale.ROOT);
      if (!Set.of("platform", "tenant", "class").contains(sc)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "scope 无效");
      }
      row.setScope(sc);
      if ("class".equals(sc)) {
        Long classId = req.getClassId() != null ? req.getClassId() : row.getClassId();
        if (classId == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "班级模板须指定 classId");
        }
        row.setClassId(classId);
        row.setTenantId(SecurityContext.currentUser().getTenantId());
      } else if ("tenant".equals(sc)) {
        row.setClassId(null);
        row.setTenantId(SecurityContext.currentUser().getTenantId());
      } else {
        row.setClassId(null);
        row.setTenantId(null);
      }
    }
    row.setUpdatedBy(SecurityContext.currentUser().getUserId());
    templateMapper.updateById(row);
    return toVO(row);
  }

  @Transactional
  public void delete(Long id) {
    TrainSubsidyMaterialTemplateEntity row = requireOwnedTemplate(id);
    row.setStatus("retired");
    row.setUpdatedBy(SecurityContext.currentUser().getUserId());
    templateMapper.updateById(row);
    templateMapper.deleteById(id);
  }

  @Transactional
  public SubsidyMaterialTemplateVO reparse(Long id) {
    TrainSubsidyMaterialTemplateEntity row = requireOwnedTemplate(id);
    String fmt = row.getFileFormat() == null ? "" : row.getFileFormat().toLowerCase(Locale.ROOT);
    if (!"docx".equals(fmt) && !"xlsx".equals(fmt) && !"xls".equals(fmt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅 docx/xlsx 支持占位符解析");
    }
    try {
      byte[] bytes = fileApplicationService.readFileBytesById(row.getTemplateFileId());
      Set<String> keys = new LinkedHashSet<>(SubsidyTemplatePlaceholderParser.extract(bytes, fmt));
      if ("xlsx".equals(fmt) || "xls".equals(fmt)) {
        keys.addAll(SubsidyXlsxLabelFiller.detectMatchedKeys(bytes));
      }
      row.setFieldSchemaJson(objectMapper.writeValueAsString(new ArrayList<>(keys)));
      row.setParseVersion((row.getParseVersion() == null ? 0 : row.getParseVersion()) + 1);
      row.setUpdatedBy(SecurityContext.currentUser().getUserId());
      templateMapper.updateById(row);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "重新解析失败: " + ex.getMessage());
    }
    return toVO(row);
  }

  /**
   * 选模板 → 自动填本班/补贴字段 → 写入材料实例新版本（status=filled，待人工确认）。
   */
  @Transactional
  public Map<String, Object> fillFromTemplate(Long subsidyId, SubsidyMaterialFillFromTemplateReq req) {
    if (req == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择模板");
    }
    boolean useCurrentFile = Boolean.TRUE.equals(req.getUseCurrentFile());
    if (!useCurrentFile && req.getTemplateId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择模板");
    }
    if (useCurrentFile && req.getInstanceId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "基于当前文件填数需要指定材料");
    }

    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    assertEditable(subsidy);

    TrainSubsidyMaterialTemplateEntity libraryTemplate = null;
    if (!useCurrentFile) {
      libraryTemplate = requireTemplate(req.getTemplateId());
      if (!"active".equals(libraryTemplate.getStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "模板未发布或已退役");
      }
    }

    TrainSubsidyMaterialInstanceEntity instance;
    if (req.getInstanceId() != null) {
      instance = requireInstance(req.getInstanceId());
      if (!subsidyId.equals(instance.getSubsidyId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "材料不属于本补贴单");
      }
    } else {
      instance = new TrainSubsidyMaterialInstanceEntity();
      instance.setSubsidyId(subsidy.getId());
      String slot = blankToNull(req.getSlotCode());
      if (slot == null && libraryTemplate != null) {
        slot = blankToNull(libraryTemplate.getSlotCode());
      }
      instance.setSlotCode(slot);
      if (libraryTemplate != null) {
        instance.setTemplateId(libraryTemplate.getId());
        instance.setMaterialType(
            StringUtils.hasText(libraryTemplate.getMaterialType())
                ? libraryTemplate.getMaterialType()
                : SubsidyMaterialTypes.APPLICATION_FORM);
        instance.setTitle(
            StringUtils.hasText(req.getTitle())
                ? req.getTitle().trim()
                : libraryTemplate.getTitle());
      } else {
        instance.setMaterialType(SubsidyMaterialTypes.APPLICATION_FORM);
        instance.setTitle(StringUtils.hasText(req.getTitle()) ? req.getTitle().trim() : "未命名材料");
      }
      instance.setStatus("pending");
      instance.setRequiredFlag(0);
      instance.setSortOrder(900);
      instance.setTenantId(subsidy.getTenantId());
      instance.setDeleted(0);
      instanceMapper.insert(instance);
    }

    final String fmt;
    final byte[] templateBytes;
    final String sourceTitle;
    final Long sourceTemplateId;
    final String libraryMaterialType;

    if (useCurrentFile) {
      TrainSubsidyMaterialVersionEntity base = resolveUploadTemplateVersion(instance.getId());
      if (base == null || base.getFileId() == null) {
        throw new BusinessException(
            ErrorCode.BAD_REQUEST,
            "没有可用的上传模板。当前预览多半是上次填数结果（如误用花名册），请先「替换上传」正确的申请表/模板空白文件，再填数");
      }
      fmt = detectOfficeFormat(base.getFileName(), null);
      if (!"docx".equals(fmt) && !"xlsx".equals(fmt) && !"xls".equals(fmt)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "上传模板不是 docx/xlsx，无法自动填数");
      }
      templateBytes = fileApplicationService.readFileBytesById(base.getFileId());
      // 申请表误用花名册文件时直接拦截，避免再次生成花名册
      if (SubsidyMaterialTypes.APPLICATION_FORM.equals(instance.getMaterialType())
          && looksLikeRosterWorkbook(templateBytes)) {
        throw new BusinessException(
            ErrorCode.BAD_REQUEST,
            "上传的文件内容是花名册（含姓名表头/学员行），与当前「申请表」材料不符。请重新「替换上传」申请表模板后再填数");
      }
      sourceTitle = StringUtils.hasText(base.getFileName()) ? base.getFileName() : instance.getTitle();
      sourceTemplateId = instance.getTemplateId();
      libraryMaterialType = instance.getMaterialType();
    } else {
      fmt =
          libraryTemplate.getFileFormat() == null
              ? ""
              : libraryTemplate.getFileFormat().toLowerCase(Locale.ROOT);
      if (!"docx".equals(fmt) && !"xlsx".equals(fmt) && !"xls".equals(fmt)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 docx / xlsx 模板自动填数");
      }
      templateBytes = fileApplicationService.readFileBytesById(libraryTemplate.getTemplateFileId());
      sourceTitle = libraryTemplate.getTitle();
      sourceTemplateId = libraryTemplate.getId();
      libraryMaterialType = libraryTemplate.getMaterialType();
      instance.setTemplateId(libraryTemplate.getId());
      if (!StringUtils.hasText(instance.getMaterialType())) {
        instance.setMaterialType(libraryTemplate.getMaterialType());
      }
    }

    Map<String, String> vars = buildFillVars(subsidy);
    List<String> fieldKeys = useCurrentFile
        ? extractFieldKeysFromBytes(templateBytes, fmt)
        : readFieldKeys(libraryTemplate);
    List<String> missing = new ArrayList<>();
    for (String key : fieldKeys) {
      if (SubsidyXlsxRosterFiller.PERSON_ONLY_PLACEHOLDERS.contains(key)
          || SubsidyXlsxRosterFiller.STUDENT_PLACEHOLDERS.contains(key)) {
        continue;
      }
      if (!StringUtils.hasText(vars.get(key))) {
        missing.add(key);
      }
    }

    byte[] filled;
    String fillMode = "scalar";
    int studentFilled = 0;
    try {
      if ("docx".equals(fmt)) {
        filled = TrainingDocxVariableReplacer.replaceAll(templateBytes, vars);
      } else {
        boolean rosterMode =
            SubsidyXlsxRosterFiller.shouldUseRosterMode(instance.getMaterialType(), templateBytes)
                || SubsidyXlsxRosterFiller.shouldUseRosterMode(libraryMaterialType, templateBytes);
        if (rosterMode) {
          List<Map<String, String>> studentRows =
              buildStudentRows(subsidy, vars, "certified");
          SubsidyXlsxRosterFiller.FillResult result =
              SubsidyXlsxRosterFiller.fill(templateBytes, studentRows, vars);
          filled = result.bytes();
          fillMode = result.mode();
          studentFilled = result.studentCount();
        } else {
          filled = TrainingXlsxVariableReplacer.replaceAll(templateBytes, vars);
          fillMode = "xlsx_scalar";
          // 政务空白表：按中文标签自动匹配右侧/下方空格
          SubsidyXlsxLabelFiller.FillResult labelResult =
              SubsidyXlsxLabelFiller.fill(filled, vars);
          if (labelResult.matchedCount() > 0) {
            filled = labelResult.bytes();
            fillMode = "xlsx_label";
          }
        }
      }
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "填数失败: " + ex.getMessage());
    }

    String ext = "docx".equals(fmt) ? "docx" : ("xls".equals(fmt) ? "xls" : "xlsx");
    String contentType = "docx".equals(fmt) ? DOCX : ("xls".equals(fmt) ? XLS : XLSX);
    String fileNamePrefix = useCurrentFile ? "[填数]" : "[模板]";
    String fileName = fileNamePrefix + sanitize(instance.getTitle()) + "." + ext;
    FileVO stored = fileApplicationService.storeFromBytes(filled, fileName, contentType, "private");
    String path = "/api/files/" + stored.id() + "/download";

    TrainSubsidyMaterialVersionEntity version = new TrainSubsidyMaterialVersionEntity();
    version.setInstanceId(instance.getId());
    version.setVersionNo(nextVersionNo(instance.getId()));
    version.setFileId(stored.id());
    version.setFilePath(path);
    version.setFileName(fileName);
    version.setSource(useCurrentFile ? "auto_fill_current" : "auto_fill");
    try {
      Map<String, Object> snap = new LinkedHashMap<>();
      if (sourceTemplateId != null) {
        snap.put("templateId", sourceTemplateId);
      }
      snap.put("useCurrentFile", useCurrentFile);
      snap.put("sourceTitle", sourceTitle);
      snap.put("vars", vars);
      snap.put("missing", missing);
      snap.put("fillMode", fillMode);
      snap.put("studentCount", studentFilled);
      snap.put("rosterScope", "certified");
      version.setFieldSnapshotJson(objectMapper.writeValueAsString(snap));
    } catch (Exception ignore) {
      version.setFieldSnapshotJson(null);
    }
    version.setConfirmed(0);
    version.setRemark(
        (useCurrentFile ? "基于当前文件填数：" : "模板填数：") + sourceTitle + "（" + fillMode + "）");
    version.setCreatedBy(SecurityContext.currentUser().getUserId());
    version.setTenantId(subsidy.getTenantId());
    version.setDeleted(0);
    versionMapper.insert(version);

    instance.setCurrentVersionId(version.getId());
    instance.setStatus("filled");
    instanceMapper.updateById(instance);

    if (StringUtils.hasText(instance.getSlotCode())) {
      TrainSubsidySlotFileEntity slotFile = new TrainSubsidySlotFileEntity();
      slotFile.setSubsidyId(subsidy.getId());
      slotFile.setSlotCode(instance.getSlotCode());
      slotFile.setInstanceId(instance.getId());
      slotFile.setVersionId(version.getId());
      slotFile.setFileId(stored.id());
      slotFile.setFilePath(path);
      slotFile.setFileName(fileName);
      slotFile.setUploadedBy(SecurityContext.currentUser().getUserId());
      slotFile.setTenantId(subsidy.getTenantId());
      slotFile.setDeleted(0);
      slotFileMapper.insert(slotFile);
    }

    Map<String, Object> eventPayload = new LinkedHashMap<>();
    eventPayload.put("instanceId", instance.getId());
    if (sourceTemplateId != null) {
      eventPayload.put("templateId", sourceTemplateId);
    }
    eventPayload.put("useCurrentFile", useCurrentFile);
    eventPayload.put("versionId", version.getId());
    eventPayload.put("missingCount", missing.size());
    eventPayload.put("fillMode", fillMode);
    eventPayload.put("studentCount", studentFilled);
    addEvent(
        subsidy,
        "template_fill",
        (useCurrentFile ? "当前文件填数 " : "模板填数 ") + instance.getTitle(),
        eventPayload);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);
    out.put("instanceId", instance.getId());
    out.put("templateId", sourceTemplateId);
    out.put("useCurrentFile", useCurrentFile);
    out.put("versionId", version.getId());
    out.put("filePath", path);
    out.put("fileName", fileName);
    out.put("filledKeys", vars.keySet());
    out.put("missingKeys", missing);
    out.put("fillMode", fillMode);
    out.put("studentCount", studentFilled);
    String msg;
    if (useCurrentFile) {
      if (studentFilled > 0) {
        msg = "已按当前上传文件填入花名册（" + studentFilled + " 人），请预览后确认本材料";
      } else if (missing.isEmpty()) {
        msg = "已按当前上传文件填数，请预览后确认本材料";
      } else {
        msg = "已按当前上传文件填数（缺失字段 " + missing.size() + " 个：" + String.join(", ", missing)
            + "），请核对后确认";
      }
    } else if (studentFilled > 0) {
      msg = "已按模板生成花名册（" + studentFilled + " 人，模式 " + fillMode + "），请预览后确认本材料";
      if (!missing.isEmpty()) {
        msg += "；班级字段缺失 " + missing.size() + " 个：" + String.join(", ", missing);
      }
    } else if (missing.isEmpty()) {
      msg = "已按模板生成，请预览后确认本材料";
    } else {
      msg = "已按模板生成（缺失字段 " + missing.size() + " 个：" + String.join(", ", missing) + "），请核对后确认";
    }
    if ("xlsx_label".equals(fillMode)) {
      msg = "已按表内中文标签自动匹配填数，请预览核对后确认本材料"
          + (missing.isEmpty() ? "" : "（部分 ${} 占位符仍缺失：" + String.join(", ", missing) + "）");
    }
    out.put("message", msg);
    return out;
  }

  /** 对已有材料实例选模板填数（可基于当前文件或模板库） */
  @Transactional
  public Map<String, Object> fillInstance(Long instanceId, SubsidyMaterialFillFromTemplateReq body) {
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    SubsidyMaterialFillFromTemplateReq req = body != null ? body : new SubsidyMaterialFillFromTemplateReq();
    boolean useCurrent = Boolean.TRUE.equals(req.getUseCurrentFile());
    if (!useCurrent && req.getTemplateId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择模板，或改用基于当前文件填数");
    }
    req.setInstanceId(instanceId);
    return fillFromTemplate(instance.getSubsidyId(), req);
  }

  /**
   * 按本班材料清单逐项自动填数：优先「替换上传/本班同步」底稿，否则按材料类型匹配模板库。
   * 每项独立事务，单项失败不影响其它项。
   */
  public Map<String, Object> fillAllInstances(Long subsidyId) {
    TrainSubsidyEntity subsidy = requireSubsidy(subsidyId);
    assertEditable(subsidy);
    List<TrainSubsidyMaterialInstanceEntity> instances =
        instanceMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialInstanceEntity>()
                .eq(TrainSubsidyMaterialInstanceEntity::getSubsidyId, subsidyId)
                .eq(TrainSubsidyMaterialInstanceEntity::getDeleted, 0)
                .orderByAsc(TrainSubsidyMaterialInstanceEntity::getSortOrder)
                .orderByAsc(TrainSubsidyMaterialInstanceEntity::getId));

    int filled = 0;
    int skipped = 0;
    int failed = 0;
    List<Map<String, Object>> details = new ArrayList<>();

    for (TrainSubsidyMaterialInstanceEntity instance : instances) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("instanceId", instance.getId());
      row.put("title", instance.getTitle());
      row.put("materialType", instance.getMaterialType());

      if (!isBatchFillCandidate(instance)) {
        skipped++;
        row.put("status", "skipped");
        if (isPhotoLike(instance)) {
          row.put("reason", "培训照片由本班跟班照片册自动关联；无册时请手工替换");
        } else if (SubsidyMaterialTypes.CERTIFICATE.equals(instance.getMaterialType())) {
          row.put("reason", "证书/获证名册由本班同步；无上游数据时请手工替换");
        } else {
          row.put("reason", "不支持自动填数");
        }
        details.add(row);
        continue;
      }

      SubsidyMaterialFillFromTemplateReq req = new SubsidyMaterialFillFromTemplateReq();
      req.setInstanceId(instance.getId());
      req.setRosterScope("certified");

      TrainSubsidyMaterialVersionEntity fileBase = resolveUploadTemplateVersion(instance.getId());
      if (fileBase != null && fileBase.getFileId() != null) {
        String fmt = detectOfficeFormat(fileBase.getFileName(), null);
        if ("docx".equals(fmt) || "xlsx".equals(fmt) || "xls".equals(fmt)) {
          req.setUseCurrentFile(true);
        }
      }
      if (!Boolean.TRUE.equals(req.getUseCurrentFile())) {
        Long templateId = resolveLibraryTemplateId(instance, subsidy);
        if (templateId == null) {
          skipped++;
          row.put("status", "skipped");
          row.put("reason", "无上传底稿且模板库无匹配的 docx/xlsx");
          details.add(row);
          continue;
        }
        req.setTemplateId(templateId);
      }

      try {
        Map<String, Object> result = self.fillInstanceInNewTx(instance.getId(), req);
        filled++;
        row.put("status", "filled");
        row.put("fillMode", result.get("fillMode"));
        row.put("studentCount", result.get("studentCount"));
        row.put("message", result.get("message"));
        details.add(row);
      } catch (Exception ex) {
        failed++;
        row.put("status", "failed");
        row.put("reason", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        details.add(row);
        log.warn("batch fill failed subsidyId={} instanceId={} title={}",
            subsidyId, instance.getId(), instance.getTitle(), ex);
      }
    }

    addEvent(
        subsidy,
        "template_fill_all",
        "清单一键填数 成功" + filled + "/跳过" + skipped + "/失败" + failed,
        Map.of("filled", filled, "skipped", skipped, "failed", failed));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", failed == 0);
    out.put("filled", filled);
    out.put("skipped", skipped);
    out.put("failed", failed);
    out.put("total", instances.size());
    out.put("details", details);
    out.put(
        "message",
        "一键填数完成：成功 " + filled + "，跳过 " + skipped + "，失败 " + failed
            + "（共 " + instances.size() + " 项）");
    return out;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> fillInstanceInNewTx(
      Long instanceId, SubsidyMaterialFillFromTemplateReq body) {
    return fillInstance(instanceId, body);
  }

  private boolean isBatchFillCandidate(TrainSubsidyMaterialInstanceEntity instance) {
    // 培训照片、证书 PDF / 获证名册：由「本班同步」负责，不走占位符填数
    if (isPhotoLike(instance) || SubsidyMaterialTypes.CERTIFICATE.equals(instance.getMaterialType())) {
      return false;
    }
    return true;
  }

  private static boolean isPhotoLike(TrainSubsidyMaterialInstanceEntity instance) {
    return "follow_photo".equals(instance.getRenderer())
        || "photo_album".equals(instance.getSlotCode())
        || SubsidyMaterialTypes.PHOTO.equals(instance.getMaterialType());
  }

  private Long resolveLibraryTemplateId(
      TrainSubsidyMaterialInstanceEntity instance, TrainSubsidyEntity subsidy) {
    if (instance.getTemplateId() != null) {
      try {
        TrainSubsidyMaterialTemplateEntity t = requireTemplate(instance.getTemplateId());
        if ("active".equals(t.getStatus()) && isOfficeTemplateFormat(t.getFileFormat())) {
          return t.getId();
        }
      } catch (BusinessException ignore) {
        // fall through
      }
    }
    String materialType = instance.getMaterialType();
    if (!StringUtils.hasText(materialType)) {
      return null;
    }
    Long tenantId = subsidy.getTenantId();
    List<TrainSubsidyMaterialTemplateEntity> candidates =
        templateMapper.selectList(
            new LambdaQueryWrapper<TrainSubsidyMaterialTemplateEntity>()
                .eq(TrainSubsidyMaterialTemplateEntity::getDeleted, 0)
                .eq(TrainSubsidyMaterialTemplateEntity::getStatus, "active")
                .eq(TrainSubsidyMaterialTemplateEntity::getMaterialType, materialType)
                .and(
                    w ->
                        w.eq(TrainSubsidyMaterialTemplateEntity::getScope, "platform")
                            .or()
                            .eq(TrainSubsidyMaterialTemplateEntity::getTenantId, tenantId)
                            .or()
                            .eq(TrainSubsidyMaterialTemplateEntity::getClassId, subsidy.getClassId()))
                .orderByDesc(TrainSubsidyMaterialTemplateEntity::getUpdatedAt)
                .orderByDesc(TrainSubsidyMaterialTemplateEntity::getId));
    for (TrainSubsidyMaterialTemplateEntity t : candidates) {
      if (isOfficeTemplateFormat(t.getFileFormat())) {
        return t.getId();
      }
    }
    return null;
  }

  private static boolean isOfficeTemplateFormat(String fmt) {
    String f = fmt == null ? "" : fmt.toLowerCase(Locale.ROOT);
    return "docx".equals(f) || "xlsx".equals(f) || "xls".equals(f);
  }

  /**
   * 构建花名册逐行数据。申报补贴固定仅已获证学员（rosterScope 参数保留兼容，一律按 certified）。
   */
  private List<Map<String, String>> buildStudentRows(
      TrainSubsidyEntity subsidy, Map<String, String> classVars, String rosterScope) {
    if (subsidy.getClassId() == null) {
      return List.of();
    }
    List<TrainPeopleListEntity> students =
        peopleListMapper.selectList(
            new LambdaQueryWrapper<TrainPeopleListEntity>()
                .eq(TrainPeopleListEntity::getTrainClassId, subsidy.getClassId())
                .eq(TrainPeopleListEntity::getType, "student")
                .eq(TrainPeopleListEntity::getDeleted, 0)
                .orderByAsc(TrainPeopleListEntity::getId));
    if (students.isEmpty()) {
      return List.of();
    }

    Map<Long, TrainCertificateEntity> certs = new HashMap<>();
    certificateMapper
        .selectList(
            new LambdaQueryWrapper<TrainCertificateEntity>()
                .eq(TrainCertificateEntity::getClassId, subsidy.getClassId())
                .eq(TrainCertificateEntity::getDeleted, 0))
        .forEach(
            c -> {
              if (c.getPeopleId() != null) {
                certs.putIfAbsent(c.getPeopleId(), c);
              }
            });

    students = SubsidyCertifiedPeopleSupport.filterCertified(students, certs);

    List<String> idCards =
        students.stream()
            .map(TrainPeopleListEntity::getIdCard)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    Map<String, PlatformUserDetailEntity> details = new HashMap<>();
    Map<String, TrainSignUpPeopleSelfEntity> signUps = new HashMap<>();
    if (!idCards.isEmpty()) {
      platformUserDetailMapper
          .selectList(
              new LambdaQueryWrapper<PlatformUserDetailEntity>()
                  .in(PlatformUserDetailEntity::getIdcard, idCards)
                  .eq(PlatformUserDetailEntity::getDeleted, 0))
          .forEach(
              p -> {
                if (StringUtils.hasText(p.getIdcard())) {
                  details.putIfAbsent(p.getIdcard().trim(), p);
                }
              });
      signUpPeopleSelfMapper
          .selectList(
              new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
                  .in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards)
                  .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
                  .orderByDesc(TrainSignUpPeopleSelfEntity::getId))
          .forEach(
              p -> {
                if (StringUtils.hasText(p.getIdCardNumber())) {
                  signUps.putIfAbsent(p.getIdCardNumber().trim(), p);
                }
              });
    }

    String company = classVars.getOrDefault("company", "");
    String worktype = classVars.getOrDefault("worktype", "");
    String level = classVars.getOrDefault("level", "");
    String classTitle = firstNonBlank(
        classVars.get("class_title"), classVars.get("banqiname"), classVars.get("banqi"));
    String pxtime = firstNonBlank(classVars.get("pxtime"), classVars.get("train_period"));
    String dateStart = nullToEmpty(classVars.get("date_start"));
    String dateEnd = nullToEmpty(classVars.get("date_end"));
    String trainSchool = firstNonBlank(
        classVars.get("trainSchool"),
        classVars.get("train_school"),
        classVars.get("pxdw"),
        classVars.get("school"));
    String subsidyAmount = firstNonBlank(
        classVars.get("perPersonAmount"),
        classVars.get("subsidyAmount"),
        classVars.get("subsidy"),
        classVars.get("amount"));
    String trainModeLabel = trainModeLabel(classVars.get("trainMode"));

    List<Map<String, String>> rows = new ArrayList<>();
    int seq = 1;
    for (TrainPeopleListEntity s : students) {
      String idCard = nullToEmpty(s.getIdCard());
      PlatformUserDetailEntity detail = StringUtils.hasText(idCard) ? details.get(idCard) : null;
      TrainSignUpPeopleSelfEntity signUp = StringUtils.hasText(idCard) ? signUps.get(idCard) : null;
      TrainCertificateEntity cert = certs.get(s.getId());

      String name =
          firstNonBlank(
              s.getRealname(),
              detail != null ? detail.getRealname() : null,
              signUp != null ? signUp.getName() : null);
      String phone =
          firstNonBlank(
              s.getPhone(),
              detail != null ? detail.getPhone() : null,
              signUp != null ? signUp.getPhone() : null);
      String address =
          firstNonBlank(
              detail != null ? detail.getAddress() : null,
              signUp != null ? signUp.getAddress() : null);
      String gender = resolveGender(idCard, detail, signUp);
      String nation =
          firstNonBlank(
              detail != null ? detail.getNation() : null,
              signUp != null ? signUp.getNation() : null);
      String education =
          firstNonBlank(
              detail != null ? detail.getEducation() : null,
              signUp != null ? signUp.getEducation() : null);
      String political =
          firstNonBlank(
              detail != null ? detail.getPolitical() : null,
              signUp != null ? signUp.getPolitical() : null);
      String personCompany =
          firstNonBlank(
              signUp != null ? signUp.getCompanyName() : null,
              company);
      String personJob =
          firstNonBlank(
              signUp != null ? signUp.getJob() : null,
              signUp != null ? signUp.getJog() : null,
              worktype);
      String personWorktype =
          firstNonBlank(
              cert != null ? cert.getWorkType() : null,
              signUp != null ? signUp.getWorkTypeName() : null,
              worktype);
      String personLevel =
          firstNonBlank(
              cert != null ? formatCertLevel(cert.getLevel()) : null,
              level);
      String category;
      if (StringUtils.hasText(address)
          && (address.contains("镇") || address.contains("村") || address.contains("屯"))) {
        category = "在岗农民工";
      } else {
        category = "企业职工";
      }
      String trainTarget = firstNonBlank(trainModeLabel, category);
      String employment = buildEmployment(personCompany, personJob, personWorktype, address);
      String certNo = cert != null ? nullToEmpty(cert.getCertificateNo()) : "";
      String issueDate = cert != null ? formatIssueDate(cert.getIssueDate()) : "";
      String issuingAuthority = cert != null ? nullToEmpty(cert.getIssuingAuthority()) : "";

      Map<String, String> row = new LinkedHashMap<>();
      row.put("seq", String.valueOf(seq));
      row.put("index", String.valueOf(seq));
      row.put("no", String.valueOf(seq));
      row.put("name", name);
      row.put("realname", name);
      row.put("idCard", idCard);
      row.put("idcard", idCard);
      row.put("phone", phone);
      row.put("gender", gender);
      row.put("nation", nation);
      row.put("education", education);
      row.put("political", political);
      row.put("address", address);
      row.put("ticketNo", nullToEmpty(s.getTicketNo()));
      row.put("ticket_no", nullToEmpty(s.getTicketNo()));
      row.put("company", personCompany);
      row.put("worktype", personWorktype);
      row.put("work_type", personWorktype);
      row.put("level", personLevel);
      row.put("certificateNo", certNo);
      row.put("certificate_no", certNo);
      row.put("certNo", certNo);
      row.put("issueDate", issueDate);
      row.put("issue_date", issueDate);
      row.put("issuingAuthority", issuingAuthority);
      row.put("theoryScore", scoreText(s.getTheoryScore()));
      row.put("realScore", scoreText(s.getRealScore()));
      row.put("examScore", scoreText(s.getExamScore()));
      row.put("performanceScore", scoreText(s.getPerformanceScore()));
      row.put("category", category);
      row.put("trainTarget", trainTarget);
      row.put("train_target", trainTarget);
      row.put("banqi", classTitle);
      row.put("class_term", classTitle);
      row.put("class_title", classTitle);
      row.put("banqiname", classTitle);
      row.put("pxtime", pxtime);
      row.put("train_period", pxtime);
      row.put("date_start", dateStart);
      row.put("date_end", dateEnd);
      row.put("subsidy", subsidyAmount);
      row.put("subsidyAmount", subsidyAmount);
      row.put("subsidy_amount", subsidyAmount);
      row.put("amount", subsidyAmount);
      row.put("perPersonAmount", subsidyAmount);
      row.put("trainSchool", trainSchool);
      row.put("train_school", trainSchool);
      row.put("pxdw", trainSchool);
      row.put("school", trainSchool);
      row.put("employment", employment);
      row.put("job", personJob);
      row.put("remark", "");
      rows.add(row);
      seq++;
    }
    return rows;
  }

  private static String trainModeLabel(String mode) {
    if (!StringUtils.hasText(mode)) {
      return "";
    }
    return switch (mode.trim()) {
      case "skill_upgrade" -> "岗位技能提升";
      case "apprentice" -> "新型学徒";
      default -> mode.trim();
    };
  }

  private static String buildEmployment(String company, String job, String worktype, String address) {
    String unit = firstNonBlank(company, "");
    String post = firstNonBlank(job, worktype);
    if (StringUtils.hasText(unit) && StringUtils.hasText(post)) {
      return unit + " / " + post;
    }
    if (StringUtils.hasText(unit)) {
      return unit;
    }
    if (StringUtils.hasText(post)) {
      return post;
    }
    return nullToEmpty(address);
  }

  private static String formatCertLevel(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String v = raw.trim();
    if (v.contains("/")) {
      return v;
    }
    return switch (v) {
      case "1", "一级" -> "一级/高级技师";
      case "2", "二级" -> "二级/技师";
      case "3", "三级" -> "三级/高级工";
      case "4", "四级" -> "四级/中级工";
      case "5", "五级" -> "五级/初级工";
      default -> v;
    };
  }

  private static String formatIssueDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String v = raw.trim().replace('/', '-');
    if (v.length() >= 10) {
      return v.substring(0, 10);
    }
    if (v.matches("\\d{8}")) {
      return v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8);
    }
    return v;
  }

  private static String resolveGender(
      String idCard, PlatformUserDetailEntity detail, TrainSignUpPeopleSelfEntity signUp) {
    if (detail != null && detail.getGender() != null) {
      return detail.getGender() > 0 ? "女" : "男";
    }
    if (signUp != null && StringUtils.hasText(signUp.getGender())) {
      String g = signUp.getGender().trim();
      if ("女".equals(g) || "2".equals(g)) {
        return "女";
      }
      if ("男".equals(g) || "1".equals(g)) {
        return "男";
      }
      return g;
    }
    if (StringUtils.hasText(idCard) && idCard.trim().length() >= 17) {
      char ch = idCard.trim().charAt(16);
      if (Character.isDigit(ch)) {
        return ((ch - '0') % 2 == 0) ? "女" : "男";
      }
    }
    return "";
  }

  private static String scoreText(BigDecimal score) {
    if (score == null) {
      return "";
    }
    return score.setScale(1, RoundingMode.HALF_UP).toPlainString();
  }

  private Map<String, String> buildFillVars(TrainSubsidyEntity subsidy) {
    Map<String, String> vars = new LinkedHashMap<>();
    TrainInfoClassEntity clazz =
        subsidy.getClassId() != null ? classMapper.selectById(subsidy.getClassId()) : null;
    TrainInfoListEntity train =
        subsidy.getTrainInfoId() != null
            ? infoListMapper.selectById(subsidy.getTrainInfoId())
            : (clazz != null && clazz.getTrainInfoId() != null
                ? infoListMapper.selectById(clazz.getTrainInfoId())
                : null);

    String companyName = "";
    Long companyId = clazz != null ? clazz.getCompanyId() : null;
    if (companyId != null) {
      OrgEntity org = orgMapper.selectById(companyId);
      if (org != null) {
        companyName = nullToEmpty(org.getName());
      }
    }
    if (!StringUtils.hasText(companyName) && train != null) {
      companyName = nullToEmpty(train.getCompanyName());
    }

    String workType = "";
    String level = "";
    if (clazz != null && clazz.getWorkTypeId() != null) {
      ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(clazz.getWorkTypeId());
      if (wt != null) {
        workType = nullToEmpty(wt.getTitle());
      }
      level = formatLevel(clazz.getWorkTypeLevel());
    }

    int studentCount = 0;
    if (clazz != null) {
      List<TrainPeopleListEntity> all =
          peopleListMapper.selectList(
              new LambdaQueryWrapper<TrainPeopleListEntity>()
                  .eq(TrainPeopleListEntity::getTrainClassId, clazz.getId())
                  .eq(TrainPeopleListEntity::getType, "student")
                  .eq(TrainPeopleListEntity::getDeleted, 0));
      Map<Long, TrainCertificateEntity> certs = new HashMap<>();
      certificateMapper
          .selectList(
              new LambdaQueryWrapper<TrainCertificateEntity>()
                  .eq(TrainCertificateEntity::getClassId, clazz.getId())
                  .eq(TrainCertificateEntity::getDeleted, 0))
          .forEach(
              c -> {
                if (c.getPeopleId() != null) {
                  certs.putIfAbsent(c.getPeopleId(), c);
                }
              });
      studentCount = SubsidyCertifiedPeopleSupport.countCertified(all, certs);
    }
    if (studentCount <= 0 && subsidy.getStudentCount() != null) {
      studentCount = subsidy.getStudentCount();
    }

    String classTitle = clazz != null ? nullToEmpty(clazz.getTitle()) : "";
    String trainTitle = train != null ? nullToEmpty(train.getTitle()) : "";
    String dateStart = clazz != null ? nullToEmpty(clazz.getDateStart()) : "";
    String dateEnd = clazz != null ? nullToEmpty(clazz.getDateEnd()) : "";
    String pxtime =
        StringUtils.hasText(dateStart) || StringUtils.hasText(dateEnd)
            ? (dateStart + " ~ " + dateEnd)
            : "";
    String amount =
        subsidy.getSubsidyAmount() != null
            ? subsidy.getSubsidyAmount().stripTrailingZeros().toPlainString()
            : "";
    String perPersonAmount = amount;
    if (subsidy.getSubsidyAmount() != null && studentCount > 0) {
      perPersonAmount =
          subsidy
              .getSubsidyAmount()
              .divide(BigDecimal.valueOf(studentCount), 2, RoundingMode.HALF_UP)
              .stripTrailingZeros()
              .toPlainString();
    }
    String region = firstNonBlank(subsidy.getRegionName(), subsidy.getRegionCode());
    String year = subsidy.getPolicyYear() != null ? String.valueOf(subsidy.getPolicyYear()) : "";
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    String trainSchool =
        firstNonBlank(
            train != null ? train.getTrainCompanyName() : null,
            train != null ? train.getPosition() : null,
            "千铝工投");
    String trainMode = nullToEmpty(subsidy.getTrainMode());
    String trainTarget =
        switch (trainMode) {
          case "skill_upgrade" -> "岗位技能提升";
          case "apprentice" -> "新型学徒";
          default -> StringUtils.hasText(trainMode) ? trainMode : "企业职工";
        };

    // 开班申请表常用
    put(vars, "pxdw", trainSchool);
    put(vars, "trainSchool", trainSchool);
    put(vars, "train_school", trainSchool);
    put(vars, "school", trainSchool);
    put(vars, "company", companyName);
    put(vars, "company_name", companyName);
    put(vars, "com_name", companyName);
    put(vars, "worktype", workType);
    put(vars, "wktype", workType);
    put(vars, "work_type", workType);
    put(vars, "level", level);
    put(vars, "work_type_level", level);
    put(vars, "nums", String.valueOf(studentCount));
    put(vars, "studentCount", String.valueOf(studentCount));
    put(vars, "pxtime", pxtime);
    put(vars, "train_period", pxtime);
    put(vars, "date_start", dateStart);
    put(vars, "date_end", dateEnd);
    put(vars, "banqiname", classTitle);
    put(vars, "banqi", classTitle);
    put(vars, "class_term", classTitle);
    put(vars, "class_title", classTitle);
    put(vars, "classTitle", classTitle);
    put(vars, "train_title", trainTitle);
    put(vars, "trainTitle", trainTitle);
    put(vars, "pxaddr", firstNonBlank(companyName, trainSchool));
    put(vars, "keshi", train != null && train.getCourseNum() != null ? String.valueOf(train.getCourseNum()) : "");
    put(vars, "trainTarget", trainTarget);
    put(vars, "train_target", trainTarget);
    put(vars, "fzr", "");
    put(vars, "fzrtel", "");
    put(vars, "company_phone", "");
    put(vars, "org_nature", "");
    put(vars, "cfzr", "");
    put(vars, "cfzrtel", "");
    put(vars, "teacher1", "");
    put(vars, "teatel1", "");
    put(vars, "teacher2", "");
    put(vars, "teatel2", "");
    put(vars, "pxchengnuo", "");

    // 补贴单字段
    put(vars, "region", region);
    put(vars, "regionName", region);
    put(vars, "region_code", nullToEmpty(subsidy.getRegionCode()));
    put(vars, "policyYear", year);
    put(vars, "policy_year", year);
    put(vars, "trainMode", trainMode);
    put(vars, "train_mode", trainMode);
    put(vars, "subsidyAmount", amount);
    put(vars, "subsidy_amount", amount);
    put(vars, "subsidy", perPersonAmount);
    put(vars, "amount", amount);
    put(vars, "perPersonAmount", perPersonAmount);
    put(vars, "create_time", today);
    put(vars, "today", today);
    // 申请表「各类人员」拆分：企业组织培训默认全部计入企业在职员工
    put(vars, "count_enterprise_employee", String.valueOf(studentCount));
    put(vars, "count_urban_unemployed", "0");
    put(vars, "count_rural_transfer", "0");
    put(vars, "count_graduate", "0");
    put(vars, "zero", "0");
    return vars;
  }

  private SubsidyMaterialTemplateVO toVO(TrainSubsidyMaterialTemplateEntity e) {
    return new SubsidyMaterialTemplateVO(
        e.getId(),
        e.getScope(),
        e.getTenantId(),
        e.getClassId(),
        e.getMaterialType(),
        SubsidyMaterialTypes.materialTypeLabel(e.getMaterialType()),
        e.getSlotCode(),
        e.getRegionCode(),
        e.getTrainMode(),
        e.getPolicyYear(),
        e.getTitle(),
        e.getFileFormat(),
        e.getTemplateFileId(),
        e.getTemplateFilePath(),
        readFieldKeys(e),
        e.getParseVersion(),
        e.getStatus(),
        e.getRemark(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private List<String> readFieldKeys(TrainSubsidyMaterialTemplateEntity e) {
    if (!StringUtils.hasText(e.getFieldSchemaJson())) {
      return List.of();
    }
    try {
      return objectMapper.readValue(e.getFieldSchemaJson(), new TypeReference<List<String>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private List<String> extractFieldKeysFromBytes(byte[] bytes, String fmt) {
    try {
      Set<String> keys = new LinkedHashSet<>(SubsidyTemplatePlaceholderParser.extract(bytes, fmt));
      if ("xlsx".equals(fmt) || "xls".equals(fmt)) {
        for (String k : SubsidyXlsxLabelFiller.detectMatchedKeys(bytes)) {
          if (!k.startsWith("label:")) {
            keys.add(k);
          }
        }
      }
      return new ArrayList<>(keys);
    } catch (Exception ex) {
      log.warn("extract placeholders from current file failed: {}", ex.getMessage());
      return List.of();
    }
  }

  /**
   * 「基于当前文件填数」应使用最近一次用户上传或本班同步的空白模板，而不是已填数结果。
   * 否则会把上次误填的花名册再次当作模板。
   */
  private TrainSubsidyMaterialVersionEntity resolveUploadTemplateVersion(Long instanceId) {
    TrainSubsidyMaterialVersionEntity upload = versionMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
            .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
            .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
            .eq(TrainSubsidyMaterialVersionEntity::getSource, "upload")
            .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
            .last("LIMIT 1"));
    if (upload != null) {
      return upload;
    }
    TrainSubsidyMaterialVersionEntity classSync = versionMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
            .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
            .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
            .eq(TrainSubsidyMaterialVersionEntity::getSource, "class_sync")
            .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
            .last("LIMIT 1"));
    if (classSync != null) {
      return classSync;
    }
    // 无 upload/class_sync 时：仅当当前版本本身不是填数产物才可用
    TrainSubsidyMaterialInstanceEntity instance = instanceMapper.selectById(instanceId);
    if (instance == null || instance.getCurrentVersionId() == null) {
      return null;
    }
    TrainSubsidyMaterialVersionEntity cur = versionMapper.selectById(instance.getCurrentVersionId());
    if (cur == null) {
      return null;
    }
    String src = cur.getSource() == null ? "" : cur.getSource();
    if ("auto_fill".equals(src) || "auto_fill_current".equals(src)) {
      return null;
    }
    return cur;
  }

  private static boolean looksLikeRosterWorkbook(byte[] templateBytes) {
    if (templateBytes == null || templateBytes.length == 0) {
      return false;
    }
    try {
      return SubsidyXlsxRosterFiller.shouldUseRosterMode(null, templateBytes);
    } catch (Exception ex) {
      return false;
    }
  }

  private static String detectOfficeFormat(String fileName, String contentType) {
    String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    if (name.endsWith(".docx")) {
      return "docx";
    }
    if (name.endsWith(".xlsx")) {
      return "xlsx";
    }
    if (name.endsWith(".xls")) {
      return "xls";
    }
    String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    if (ct.contains("wordprocessingml") || ct.contains("msword")) {
      return "docx";
    }
    if (ct.contains("spreadsheetml")) {
      return "xlsx";
    }
    if (ct.contains("ms-excel")) {
      return "xls";
    }
    return "";
  }

  private TrainSubsidyMaterialTemplateEntity requireTemplate(Long id) {
    TrainSubsidyMaterialTemplateEntity row = templateMapper.selectById(id);
    if (row == null || (row.getDeleted() != null && row.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
    }
    return row;
  }

  private TrainSubsidyMaterialTemplateEntity requireOwnedTemplate(Long id) {
    TrainSubsidyMaterialTemplateEntity row = requireTemplate(id);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if ("platform".equals(row.getScope())) {
      return row;
    }
    if (row.getTenantId() != null && !row.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该模板");
    }
    return row;
  }

  private TrainSubsidyEntity requireSubsidy(Long id) {
    TrainSubsidyEntity entity = subsidyMapper.selectById(id);
    if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "补贴单不存在");
    }
    return entity;
  }

  private TrainSubsidyMaterialInstanceEntity requireInstance(Long id) {
    TrainSubsidyMaterialInstanceEntity entity = instanceMapper.selectById(id);
    if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "材料实例不存在");
    }
    return entity;
  }

  private void assertEditable(TrainSubsidyEntity subsidy) {
    String phase = subsidy.getPhase();
    if (!"draft".equals(phase)
        && !"checking".equals(phase)
        && !"ready".equals(phase)
        && !"rejected".equals(phase)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可修改材料");
    }
  }

  private int nextVersionNo(Long instanceId) {
    TrainSubsidyMaterialVersionEntity last =
        versionMapper.selectOne(
            new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
                .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
                .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
                .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
                .last("LIMIT 1"));
    return last == null ? 1 : last.getVersionNo() + 1;
  }

  private void addEvent(
      TrainSubsidyEntity subsidy, String action, String remark, Map<String, Object> payload) {
    TrainSubsidyEventEntity event = new TrainSubsidyEventEntity();
    event.setSubsidyId(subsidy.getId());
    event.setAction(action);
    event.setFromPhase(subsidy.getPhase());
    event.setToPhase(subsidy.getPhase());
    event.setRemark(remark);
    try {
      event.setPayloadJson(objectMapper.writeValueAsString(payload));
    } catch (Exception ignore) {
      event.setPayloadJson(null);
    }
    event.setOperatorId(SecurityContext.currentUser().getUserId());
    event.setTenantId(subsidy.getTenantId());
    event.setDeleted(0);
    eventMapper.insert(event);
  }

  private static void put(Map<String, String> vars, String key, String value) {
    vars.put(key, value == null ? "" : value);
  }

  private static String formatLevel(Integer level) {
    if (level == null || level < 1 || level > 5) {
      return "";
    }
    // 库：1=一级 … 5=五级；LEVEL_LABELS 下标按 1..5 存「五级…一级」需反转
    // 与前端 CLASS_LEVEL_LABELS 一致：5=五级/初级工 … 1=一级/高级技师
    return switch (level) {
      case 5 -> "五级/初级工";
      case 4 -> "四级/中级工";
      case 3 -> "三级/高级工";
      case 2 -> "二级/技师";
      case 1 -> "一级/高级技师";
      default -> "";
    };
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String v : values) {
      if (StringUtils.hasText(v)) {
        return v.trim();
      }
    }
    return "";
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v.trim();
  }

  private static String blankToNull(String v) {
    if (!StringUtils.hasText(v)) {
      return null;
    }
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static String stripExt(String name) {
    int i = name.lastIndexOf('.');
    return i > 0 ? name.substring(0, i) : name;
  }

  private static String sanitize(String name) {
    if (!StringUtils.hasText(name)) {
      return "material";
    }
    return name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
