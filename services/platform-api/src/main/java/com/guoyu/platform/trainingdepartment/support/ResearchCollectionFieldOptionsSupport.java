package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCertificateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchPersonUpdateReq;
import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldCatalog.FieldDef;
import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldCatalog.GroupDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResearchCollectionFieldOptionsSupport {

  public static final int VERSION = ResearchCollectionFieldCatalog.SCHEMA_VERSION;

  private final ObjectMapper objectMapper;

  public ResearchCollectionFieldOptionsSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ResearchCollectionFieldOptions parse(String json) {
    if (!StringUtils.hasText(json)) {
      return defaultOptions();
    }
    try {
      ResearchCollectionFieldOptions options = objectMapper.readValue(json, ResearchCollectionFieldOptions.class);
      return options != null ? options.normalize() : defaultOptions();
    } catch (JsonProcessingException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "采集字段配置格式无效");
    }
  }

  public String serialize(ResearchCollectionFieldOptions options) {
    if (options == null) {
      return null;
    }
    ResearchCollectionFieldOptions normalized = options.normalize();
    try {
      return objectMapper.writeValueAsString(normalized);
    } catch (JsonProcessingException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "采集字段配置无法保存");
    }
  }

  public ResearchCollectionFieldOptions mergeIncoming(ResearchCollectionFieldOptions incoming) {
    if (incoming == null) {
      return defaultOptions();
    }
    return incoming.normalize();
  }

  public ResearchCollectionFieldOptions defaultOptions() {
    return ResearchCollectionFieldOptions.empty().normalize();
  }

  public void validateCreatePerson(ResearchCollectionFieldOptions options, TrainResearchPersonCreateReq req) {
    ResearchCollectionFieldOptions effective = options != null ? options.normalize() : defaultOptions();
    validateRequired(effective, req);
    validateSelectValues(effective, req);
  }

  public void validateUpdatePerson(ResearchCollectionFieldOptions options, TrainResearchPersonUpdateReq req) {
    ResearchCollectionFieldOptions effective = options != null ? options.normalize() : defaultOptions();
    if (req.name() != null && effective.isRequired("name") && !StringUtils.hasText(req.name())) {
      throw requiredError(effective, "name");
    }
    if (req.idCardNumber() != null && effective.isRequired("idCardNumber") && !StringUtils.hasText(req.idCardNumber())) {
      throw requiredError(effective, "idCardNumber");
    }
    validateSelectValuesOnUpdate(effective, req);
  }

  private void validateRequired(ResearchCollectionFieldOptions options, TrainResearchPersonCreateReq req) {
    for (ResolvedField field : options.resolvedVisibleFields()) {
      if (!field.config().isRequired()) {
        continue;
      }
      if (!StringUtils.hasText(extractCreateValue(req, field.def().key()))) {
        throw requiredError(options, field.def().key());
      }
    }
    if (!StringUtils.hasText(req.name())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "姓名不能为空");
    }
    if (!StringUtils.hasText(req.idCardNumber())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号码不能为空");
    }
  }

  private void validateSelectValues(ResearchCollectionFieldOptions options, TrainResearchPersonCreateReq req) {
    for (ResolvedField field : options.resolvedVisibleFields()) {
      if (!field.config().isSelect()) {
        continue;
      }
      String value = extractCreateValue(req, field.def().key());
      validateSelectValue(options, field.def().key(), value);
    }
  }

  private void validateSelectValuesOnUpdate(ResearchCollectionFieldOptions options, TrainResearchPersonUpdateReq req) {
    validateSelectIfPresent(options, "job", req.job());
    validateSelectIfPresent(options, "department", req.department());
    validateSelectIfPresent(options, "workTypeName", req.workTypeName());
    validateSelectIfPresent(options, "education", req.education());
    validateSelectIfPresent(options, "titleType", req.titleType());
    validateSelectIfPresent(options, "major", req.major());
  }

  private void validateSelectIfPresent(ResearchCollectionFieldOptions options, String key, String value) {
    if (value != null) {
      validateSelectValue(options, key, value);
    }
  }

  private void validateSelectValue(ResearchCollectionFieldOptions options, String key, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    ResearchCollectionFieldConfig config = options.field(key);
    if (config == null || !config.isSelect() || !config.hasOptions()) {
      return;
    }
    if (config.isAllowCustom()) {
      return;
    }
    if (!config.options().contains(value.trim())) {
      String label = StringUtils.hasText(config.label()) ? config.label() : key;
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "必须从预设选项中选择");
    }
  }

  private BusinessException requiredError(ResearchCollectionFieldOptions options, String key) {
    ResearchCollectionFieldConfig config = options.field(key);
    FieldDef def = ResearchCollectionFieldCatalog.find(key);
    String label = config != null && StringUtils.hasText(config.label())
        ? config.label()
        : (def != null ? def.defaultLabel() : key);
    return new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
  }

  private String extractCreateValue(TrainResearchPersonCreateReq req, String key) {
    return switch (key) {
      case "name" -> req.name();
      case "idCardNumber" -> req.idCardNumber();
      case "phone" -> req.phone();
      case "companyName" -> req.companyName();
      case "major" -> req.major();
      case "education" -> req.education();
      case "workTime" -> req.workTime();
      case "workTypeName" -> req.workTypeName();
      case "department" -> req.department();
      case "job" -> req.job();
      case "workDateStart" -> req.workDateStart();
      case "titleType" -> req.titleType();
      case "titleMajor" -> req.titleMajor();
      case "titleCertDate" -> req.titleCertDate();
      case "certLevel" -> firstCertificateValue(req.certificates(), "level");
      case "certNo" -> firstCertificateValue(req.certificates(), "certNo");
      case "certIssueDate" -> firstCertificateValue(req.certificates(), "issueDate");
      default -> null;
    };
  }

  private String firstCertificateValue(List<TrainResearchPersonCertificateReq> certificates, String part) {
    if (certificates == null || certificates.isEmpty()) {
      return null;
    }
    TrainResearchPersonCertificateReq first = certificates.get(0);
    return switch (part) {
      case "level" -> first.level();
      case "certNo" -> first.certNo();
      case "issueDate" -> first.issueDate();
      default -> null;
    };
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResearchCollectionGroupConfig(String key, String label, Integer order) {
    public ResearchCollectionGroupConfig normalize(GroupDef def) {
      return new ResearchCollectionGroupConfig(
          def.key(),
          StringUtils.hasText(label) ? label.trim() : def.defaultLabel(),
          order != null && order > 0 ? order : def.defaultOrder());
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResearchCollectionFieldOptions(
      Integer version,
      List<ResearchCollectionGroupConfig> groups,
      Map<String, ResearchCollectionFieldConfig> fields
  ) {
    public static ResearchCollectionFieldOptions empty() {
      return new ResearchCollectionFieldOptions(VERSION, List.of(), Map.of());
    }

    public ResearchCollectionFieldConfig field(String key) {
      return fields != null ? fields.get(key) : null;
    }

    public boolean isRequired(String key) {
      ResearchCollectionFieldConfig config = field(key);
      FieldDef def = ResearchCollectionFieldCatalog.find(key);
      if (def != null && def.locked() && ("name".equals(key) || "idCardNumber".equals(key))) {
        return true;
      }
      return config != null && Boolean.TRUE.equals(config.required());
    }

    public ResearchCollectionFieldOptions normalize() {
      Map<String, ResearchCollectionFieldConfig> normalizedFields = new LinkedHashMap<>();
      for (FieldDef def : ResearchCollectionFieldCatalog.ALL_FIELDS) {
        ResearchCollectionFieldConfig incoming = fields != null ? fields.get(def.key()) : null;
        normalizedFields.put(def.key(), ResearchCollectionFieldConfig.merge(def, incoming));
      }
      List<ResearchCollectionGroupConfig> normalizedGroups = new ArrayList<>();
      Map<String, ResearchCollectionGroupConfig> incomingGroups = new LinkedHashMap<>();
      if (groups != null) {
        for (ResearchCollectionGroupConfig group : groups) {
          if (group != null && StringUtils.hasText(group.key())) {
            incomingGroups.put(group.key(), group);
          }
        }
      }
      for (GroupDef def : ResearchCollectionFieldCatalog.DEFAULT_GROUPS) {
        normalizedGroups.add(incomingGroups.getOrDefault(def.key(), new ResearchCollectionGroupConfig(def.key(), null, null))
            .normalize(def));
      }
      normalizedGroups.sort(Comparator.comparingInt(g -> g.order() != null ? g.order() : 0));
      return new ResearchCollectionFieldOptions(
          version != null && version > 0 ? version : VERSION,
          normalizedGroups,
          normalizedFields);
    }

    public List<ResolvedField> resolvedVisibleFields() {
      List<ResolvedField> result = new ArrayList<>();
      for (FieldDef def : ResearchCollectionFieldCatalog.ALL_FIELDS) {
        ResearchCollectionFieldConfig config = fields != null ? fields.get(def.key()) : null;
        if (config == null) {
          config = ResearchCollectionFieldConfig.merge(def, null);
        }
        if (def.locked() || config.isVisible()) {
          result.add(new ResolvedField(def, config));
        }
      }
      result.sort(Comparator
          .comparingInt((ResolvedField f) -> groupOrder(f.def().defaultGroup()))
          .thenComparingInt(f -> f.config().order() != null ? f.config().order() : f.def().defaultOrder()));
      return result;
    }

    private int groupOrder(String groupKey) {
      if (groups == null) {
        GroupDef def = ResearchCollectionFieldCatalog.DEFAULT_GROUPS.stream()
            .filter(g -> g.key().equals(groupKey)).findFirst().orElse(null);
        return def != null ? def.defaultOrder() : 99;
      }
      for (ResearchCollectionGroupConfig group : groups) {
        if (groupKey.equals(group.key())) {
          return group.order() != null ? group.order() : 99;
        }
      }
      return 99;
    }
  }

  public record ResolvedField(FieldDef def, ResearchCollectionFieldConfig config) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResearchCollectionFieldConfig(
      String inputType,
      String label,
      Boolean required,
      Boolean visible,
      Boolean allowCustom,
      Integer order,
      String group,
      List<String> options
  ) {
    public boolean isSelect() {
      return "select".equalsIgnoreCase(inputType);
    }

    public boolean isDate() {
      return "date".equalsIgnoreCase(inputType);
    }

    public boolean isAllowCustom() {
      return Boolean.TRUE.equals(allowCustom);
    }

    public boolean isRequired() {
      return Boolean.TRUE.equals(required);
    }

    public boolean isVisible() {
      return visible == null || Boolean.TRUE.equals(visible);
    }

    public boolean hasOptions() {
      return options != null && !options.isEmpty();
    }

    public static ResearchCollectionFieldConfig merge(FieldDef def, ResearchCollectionFieldConfig incoming) {
      String inputType = incoming != null && StringUtils.hasText(incoming.inputType())
          ? incoming.inputType().trim().toLowerCase()
          : def.defaultInputType();
      List<String> savedOptions = cleanOptionList(incoming != null ? incoming.options() : null);
      // 部门/岗位固定 select；有 options 时不因历史 inputType=text 丢弃选项
      if (("department".equals(def.key()) || "job".equals(def.key())) && "select".equalsIgnoreCase(def.defaultInputType())) {
        inputType = "select";
      }
      if (!savedOptions.isEmpty() && "select".equalsIgnoreCase(def.defaultInputType())) {
        inputType = "select";
      }
      String label = incoming != null && StringUtils.hasText(incoming.label())
          ? incoming.label().trim()
          : def.defaultLabel();
      boolean required = def.locked() && ("name".equals(def.key()) || "idCardNumber".equals(def.key()))
          || (incoming != null && Boolean.TRUE.equals(incoming.required()));
      boolean visible = def.locked() || "workDateStart".equals(def.key())
          || (incoming == null || incoming.isVisible());
      int order;
      String group;
      if ("workDateStart".equals(def.key())) {
        order = def.defaultOrder();
        group = def.defaultGroup();
      } else {
        order = incoming != null && incoming.order() != null && incoming.order() > 0
            ? incoming.order()
            : def.defaultOrder();
        group = incoming != null && StringUtils.hasText(incoming.group())
            ? incoming.group().trim()
            : def.defaultGroup();
      }
      List<String> options = normalizeOptions(incoming != null ? incoming.options() : null, def.key(), inputType);
      boolean allowCustom = incoming != null && incoming.allowCustom() != null
          ? Boolean.TRUE.equals(incoming.allowCustom())
          : defaultAllowCustomFor(def.key());
      if (("department".equals(def.key()) || "job".equals(def.key()))
          && "select".equalsIgnoreCase(inputType) && !options.isEmpty()) {
        allowCustom = false;
      }
      return new ResearchCollectionFieldConfig(inputType, label, required, visible, allowCustom, order, group, options);
    }

    private static List<String> cleanOptionList(List<String> raw) {
      List<String> cleaned = new ArrayList<>();
      if (raw == null) {
        return cleaned;
      }
      for (String option : raw) {
        if (StringUtils.hasText(option)) {
          String trimmed = option.trim();
          if (!cleaned.contains(trimmed)) {
            cleaned.add(trimmed);
          }
        }
      }
      return cleaned;
    }

    private static List<String> normalizeOptions(List<String> raw, String key, String inputType) {
      List<String> defaults = defaultOptionsFor(key);
      if (!"select".equalsIgnoreCase(inputType)) {
        return defaults.isEmpty() ? List.of() : defaults;
      }
      List<String> cleaned = new ArrayList<>();
      if (raw != null) {
        for (String option : raw) {
          if (StringUtils.hasText(option)) {
            String trimmed = option.trim();
            if (!cleaned.contains(trimmed)) {
              cleaned.add(trimmed);
            }
          }
        }
      }
      if (cleaned.isEmpty() && !defaults.isEmpty()) {
        return defaults;
      }
      return cleaned;
    }

    private static List<String> defaultOptionsFor(String key) {
      return switch (key) {
        case "education" -> ResearchCollectionFieldCatalog.defaultEducationOptions();
        case "certLevel" -> ResearchCollectionFieldCatalog.defaultLevelOptions();
        default -> List.of();
      };
    }

    private static boolean defaultAllowCustomFor(String key) {
      return switch (key) {
        case "major", "titleType" -> true;
        default -> false;
      };
    }

    public ResearchCollectionFieldConfig normalize() {
      return this;
    }
  }
}
