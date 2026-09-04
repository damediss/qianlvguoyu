package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.config.OnlyOfficeProperties;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEventEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialInstanceEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyMaterialVersionEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidySlotFileEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyEventMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialInstanceMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMaterialVersionMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidySlotFileMapper;
import com.guoyu.platform.trainingdepartment.support.OnlyOfficeJwtSupport;
import com.guoyu.platform.trainingdepartment.vo.SubsidyMaterialOfficeConfigVO;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 补贴材料 OnlyOffice 在线编辑：下发 editor config、匿名文件下载、保存回调写新版本。
 */
@Service
@Slf4j
@EnableConfigurationProperties(OnlyOfficeProperties.class)
public class TrainSubsidyMaterialOfficeApplicationService {

  private static final Set<String> WORD_EXT = Set.of("doc", "docx", "odt", "rtf", "txt");
  private static final Set<String> CELL_EXT = Set.of("xls", "xlsx", "ods", "csv");
  private static final Set<String> SLIDE_EXT = Set.of("ppt", "pptx", "odp");

  private final OnlyOfficeProperties props;
  private final TrainSubsidyMapper subsidyMapper;
  private final TrainSubsidyMaterialInstanceMapper instanceMapper;
  private final TrainSubsidyMaterialVersionMapper versionMapper;
  private final TrainSubsidySlotFileMapper slotFileMapper;
  private final TrainSubsidyEventMapper eventMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public TrainSubsidyMaterialOfficeApplicationService(
      OnlyOfficeProperties props,
      TrainSubsidyMapper subsidyMapper,
      TrainSubsidyMaterialInstanceMapper instanceMapper,
      TrainSubsidyMaterialVersionMapper versionMapper,
      TrainSubsidySlotFileMapper slotFileMapper,
      TrainSubsidyEventMapper eventMapper,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper) {
    this.props = props;
    this.subsidyMapper = subsidyMapper;
    this.instanceMapper = instanceMapper;
    this.versionMapper = versionMapper;
    this.slotFileMapper = slotFileMapper;
    this.eventMapper = eventMapper;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public SubsidyMaterialOfficeConfigVO buildEditorConfig(Long instanceId) {
    if (!props.enabled()) {
      return SubsidyMaterialOfficeConfigVO.disabled("未启用 OnlyOffice（platform.onlyoffice.enabled=false）");
    }
    if (!StringUtils.hasText(props.normalizedDocumentServerUrl())) {
      return SubsidyMaterialOfficeConfigVO.disabled("未配置 platform.onlyoffice.document-server-url");
    }
    if (!StringUtils.hasText(props.normalizedPublicBaseUrl())) {
      return SubsidyMaterialOfficeConfigVO.disabled("未配置 platform.onlyoffice.public-base-url（Document Server 需能访问本 API）");
    }

    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    assertEditable(subsidy);

    TrainSubsidyMaterialVersionEntity version = requireCurrentVersionWithFile(instance);
    String fileName = StringUtils.hasText(version.getFileName()) ? version.getFileName() : "material.docx";
    String ext = fileExtension(fileName);
    String documentType = documentTypeOf(ext);
    if (documentType == null) {
      return SubsidyMaterialOfficeConfigVO.disabled("当前文件格式不支持在线编辑：" + ext);
    }

    Long userId = SecurityContext.currentUser().getUserId();
    String userName = "user-" + userId;
    String downloadToken = signDownloadToken(version.getFileId(), instance.getTenantId(), version.getId());
    String documentUrl = props.normalizedPublicBaseUrl()
        + "/training/office/files/" + version.getFileId()
        + "?token=" + downloadToken;
    String callbackUrl = props.normalizedPublicBaseUrl()
        + "/training/office/callback?instanceId=" + instance.getId()
        + "&versionId=" + version.getId()
        + "&tenantId=" + instance.getTenantId()
        + "&userId=" + userId;

    // key 必须在内容变更后变化；用 instance+version 即可（保存回调会新建 version）
    String key = "tsmi-" + instance.getId() + "-v" + version.getId();

    Map<String, Object> document = new LinkedHashMap<>();
    document.put("fileType", ext);
    document.put("key", key);
    document.put("title", StringUtils.hasText(instance.getTitle()) ? instance.getTitle() : fileName);
    document.put("url", documentUrl);

    Map<String, Object> permissions = new LinkedHashMap<>();
    permissions.put("edit", true);
    permissions.put("download", true);
    permissions.put("print", true);
    permissions.put("review", true);
    document.put("permissions", permissions);

    Map<String, Object> user = new LinkedHashMap<>();
    user.put("id", String.valueOf(userId));
    user.put("name", userName);

    Map<String, Object> customization = new LinkedHashMap<>();
    customization.put("forcesave", true);
    customization.put("autosave", true);

    Map<String, Object> editorConfig = new LinkedHashMap<>();
    editorConfig.put("callbackUrl", callbackUrl);
    editorConfig.put("mode", "edit");
    editorConfig.put("lang", "zh-CN");
    editorConfig.put("user", user);
    editorConfig.put("customization", customization);

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("documentType", documentType);
    config.put("document", document);
    config.put("editorConfig", editorConfig);
    config.put("width", "100%");
    config.put("height", "100%");

    if (props.jwtEnabled()) {
      if (!StringUtils.hasText(props.jwtSecret())) {
        return SubsidyMaterialOfficeConfigVO.disabled("已启用 JWT 但未配置 platform.onlyoffice.jwt-secret");
      }
      config.put("token", OnlyOfficeJwtSupport.signHs256(config, props.jwtSecret()));
    }

    return new SubsidyMaterialOfficeConfigVO(
        true,
        null,
        props.normalizedDocumentServerUrl(),
        props.apiScriptUrl(),
        config);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> downloadForOffice(Long fileId, String token) {
    DownloadToken claims = verifyDownloadToken(token);
    if (!claims.fileId().equals(fileId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "下载令牌与文件不匹配");
    }
    return fileApplicationService.buildDownloadResponse(claims.tenantId(), fileId, true);
  }

  /**
   * OnlyOffice 回调。必须返回 {@code {"error":0}}。
   */
  @Transactional
  public Map<String, Object> handleCallback(
      Long instanceId,
      Long versionId,
      Long tenantId,
      Long userId,
      String authorizationHeader,
      String bodyJson) {
    try {
      JsonNode root = parseCallbackBody(authorizationHeader, bodyJson);
      int status = root.path("status").asInt(-1);
      log.info("OnlyOffice callback instanceId={} versionId={} status={}", instanceId, versionId, status);

      // 2=关闭后保存；6=强制保存（forcesave）
      if (status == 2 || status == 6) {
        String url = root.path("url").asText(null);
        if (!StringUtils.hasText(url)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "回调缺少下载 url");
        }
        saveEditedDocument(instanceId, versionId, tenantId, userId, url, root.path("filetype").asText(null));
      }
      return Map.of("error", 0);
    } catch (BusinessException ex) {
      log.warn("OnlyOffice callback business error: {}", ex.getMessage());
      return Map.of("error", 1);
    } catch (Exception ex) {
      log.error("OnlyOffice callback failed", ex);
      return Map.of("error", 1);
    }
  }

  private void saveEditedDocument(
      Long instanceId,
      Long openVersionId,
      Long tenantId,
      Long userId,
      String downloadUrl,
      String filetypeHint) throws Exception {
    TrainSubsidyMaterialInstanceEntity instance = requireInstance(instanceId);
    if (tenantId != null && !tenantId.equals(instance.getTenantId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "租户不匹配");
    }
    TrainSubsidyEntity subsidy = requireSubsidy(instance.getSubsidyId());
    if ("closed".equals(subsidy.getPhase()) || "granted".equals(subsidy.getPhase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可保存编辑");
    }

    TrainSubsidyMaterialVersionEntity openVersion = openVersionId != null
        ? versionMapper.selectById(openVersionId)
        : null;
    String baseName = openVersion != null && StringUtils.hasText(openVersion.getFileName())
        ? openVersion.getFileName()
        : (StringUtils.hasText(instance.getTitle()) ? instance.getTitle() + ".docx" : "material.docx");
    if (StringUtils.hasText(filetypeHint)) {
      String ext = filetypeHint.toLowerCase(Locale.ROOT).replace(".", "");
      int dot = baseName.lastIndexOf('.');
      String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
      baseName = stem + "." + ext;
    }

    byte[] bytes = downloadBytes(downloadUrl);
    String contentType = guessContentType(baseName);
    FileVO stored = fileApplicationService.storeFromBytesForTenant(
        instance.getTenantId(), bytes, baseName, contentType, "private");
    String path = "/api/files/" + stored.id() + "/download";

    TrainSubsidyMaterialVersionEntity version = new TrainSubsidyMaterialVersionEntity();
    version.setInstanceId(instance.getId());
    version.setVersionNo(nextVersionNo(instance.getId()));
    version.setFileId(stored.id());
    version.setFilePath(path);
    version.setFileName(StringUtils.hasText(stored.originalName()) ? stored.originalName() : baseName);
    version.setSource("manual_edit");
    version.setConfirmed(0);
    version.setCreatedBy(userId);
    version.setTenantId(instance.getTenantId());
    version.setDeleted(0);
    versionMapper.insert(version);

    instance.setCurrentVersionId(version.getId());
    if (!"confirmed".equals(instance.getStatus())) {
      instance.setStatus("filled");
    } else {
      // 已确认后又编辑 → 回到 filled，需重新确认
      instance.setStatus("filled");
    }
    instanceMapper.updateById(instance);

    if (StringUtils.hasText(instance.getSlotCode())) {
      TrainSubsidySlotFileEntity slotFile = new TrainSubsidySlotFileEntity();
      slotFile.setSubsidyId(subsidy.getId());
      slotFile.setSlotCode(instance.getSlotCode());
      slotFile.setInstanceId(instance.getId());
      slotFile.setVersionId(version.getId());
      slotFile.setFileId(stored.id());
      slotFile.setFilePath(path);
      slotFile.setFileName(version.getFileName());
      slotFile.setUploadedBy(userId);
      slotFile.setTenantId(instance.getTenantId());
      slotFile.setDeleted(0);
      slotFileMapper.insert(slotFile);
    }

    addEvent(subsidy, "office_save", "在线编辑保存 " + instance.getTitle(),
        Map.of("instanceId", instance.getId(), "versionId", version.getId(), "fileId", stored.id()));
  }

  private JsonNode parseCallbackBody(String authorizationHeader, String bodyJson) throws Exception {
    if (props.jwtEnabled()) {
      String token = extractBearer(authorizationHeader);
      if (!StringUtils.hasText(token) && StringUtils.hasText(bodyJson)) {
        JsonNode raw = objectMapper.readTree(bodyJson);
        if (raw.hasNonNull("token")) {
          token = raw.get("token").asText();
        }
      }
      if (!StringUtils.hasText(token)) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少 OnlyOffice JWT");
      }
      Map<String, Object> payload = OnlyOfficeJwtSupport.verifyHs256(token, props.jwtSecret());
      // OnlyOffice outbox JWT 常把业务字段放在 payload 根上，或放在 payload 字段内
      if (payload.containsKey("payload") && payload.get("payload") instanceof Map<?, ?> nested) {
        return objectMapper.valueToTree(nested);
      }
      return objectMapper.valueToTree(payload);
    }
    return objectMapper.readTree(bodyJson == null ? "{}" : bodyJson);
  }

  private byte[] downloadBytes(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofMinutes(2))
        .GET()
        .build();
    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "从 Document Server 下载编辑结果失败: HTTP " + response.statusCode());
    }
    try (InputStream in = response.body()) {
      return in.readAllBytes();
    }
  }

  private String signDownloadToken(Long fileId, Long tenantId, Long versionId) {
    long exp = Instant.now().getEpochSecond() + props.downloadTokenTtlSeconds();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("fid", fileId);
    claims.put("tid", tenantId);
    claims.put("vid", versionId);
    claims.put("exp", exp);
    String secret = StringUtils.hasText(props.jwtSecret()) ? props.jwtSecret() : "onlyoffice-download-dev-secret";
    return OnlyOfficeJwtSupport.signHs256(claims, secret);
  }

  private DownloadToken verifyDownloadToken(String token) {
    if (!StringUtils.hasText(token)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少下载令牌");
    }
    String secret = StringUtils.hasText(props.jwtSecret()) ? props.jwtSecret() : "onlyoffice-download-dev-secret";
    Map<String, Object> claims;
    try {
      claims = OnlyOfficeJwtSupport.verifyHs256(token, secret);
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "下载令牌无效");
    }
    Number exp = (Number) claims.get("exp");
    if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "下载令牌已过期，请重新打开编辑器");
    }
    Number fid = (Number) claims.get("fid");
    Number tid = (Number) claims.get("tid");
    if (fid == null || tid == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "下载令牌缺少必要字段");
    }
    return new DownloadToken(fid.longValue(), tid.longValue());
  }

  private record DownloadToken(Long fileId, Long tenantId) {}

  private TrainSubsidyMaterialVersionEntity requireCurrentVersionWithFile(TrainSubsidyMaterialInstanceEntity instance) {
    if (instance.getCurrentVersionId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，请先「替换」上传后再在线编辑");
    }
    TrainSubsidyMaterialVersionEntity version = versionMapper.selectById(instance.getCurrentVersionId());
    if (version == null || version.getFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前材料尚无文件，请先「替换」上传后再在线编辑");
    }
    return version;
  }

  private int nextVersionNo(Long instanceId) {
    TrainSubsidyMaterialVersionEntity last = versionMapper.selectOne(
        new LambdaQueryWrapper<TrainSubsidyMaterialVersionEntity>()
            .eq(TrainSubsidyMaterialVersionEntity::getInstanceId, instanceId)
            .eq(TrainSubsidyMaterialVersionEntity::getDeleted, 0)
            .orderByDesc(TrainSubsidyMaterialVersionEntity::getVersionNo)
            .last("LIMIT 1"));
    return last == null ? 1 : last.getVersionNo() + 1;
  }

  private void addEvent(TrainSubsidyEntity subsidy, String action, String remark, Map<String, Object> detail) {
    try {
      TrainSubsidyEventEntity event = new TrainSubsidyEventEntity();
      event.setSubsidyId(subsidy.getId());
      event.setAction(action);
      event.setFromPhase(subsidy.getPhase());
      event.setToPhase(subsidy.getPhase());
      event.setRemark(remark);
      event.setPayloadJson(objectMapper.writeValueAsString(detail));
      event.setOperatorId(null);
      event.setTenantId(subsidy.getTenantId());
      event.setDeleted(0);
      event.setCreatedAt(LocalDateTime.now());
      eventMapper.insert(event);
    } catch (Exception ex) {
      log.warn("write subsidy event failed: {}", ex.getMessage());
    }
  }

  private TrainSubsidyMaterialInstanceEntity requireInstance(Long id) {
    TrainSubsidyMaterialInstanceEntity entity = instanceMapper.selectById(id);
    if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "材料实例不存在");
    }
    return entity;
  }

  private TrainSubsidyEntity requireSubsidy(Long id) {
    TrainSubsidyEntity entity = subsidyMapper.selectById(id);
    if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "补贴单不存在");
    }
    return entity;
  }

  private void assertEditable(TrainSubsidyEntity subsidy) {
    if ("closed".equals(subsidy.getPhase()) || "granted".equals(subsidy.getPhase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段不可编辑材料");
    }
  }

  private static String fileExtension(String fileName) {
    if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
      return "";
    }
    return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
  }

  private static String documentTypeOf(String ext) {
    if (WORD_EXT.contains(ext)) {
      return "word";
    }
    if (CELL_EXT.contains(ext)) {
      return "cell";
    }
    if (SLIDE_EXT.contains(ext)) {
      return "slide";
    }
    return null;
  }

  private static String guessContentType(String fileName) {
    String ext = fileExtension(fileName);
    return switch (ext) {
      case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
      case "doc" -> "application/msword";
      case "xls" -> "application/vnd.ms-excel";
      case "pdf" -> "application/pdf";
      default -> "application/octet-stream";
    };
  }

  private static String extractBearer(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader)) {
      return null;
    }
    String v = authorizationHeader.trim();
    if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return v.substring(7).trim();
    }
    return v;
  }
}
