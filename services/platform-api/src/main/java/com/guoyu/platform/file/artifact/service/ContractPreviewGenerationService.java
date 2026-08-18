package com.guoyu.platform.file.artifact.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.contract.entity.ContractSourceManifestEntity;
import com.guoyu.platform.contract.entity.ContractSourceSnapshotEntity;
import com.guoyu.platform.contract.mapper.ContractSourceManifestMapper;
import com.guoyu.platform.contract.mapper.ContractSourceSnapshotMapper;
import com.guoyu.platform.contract.service.ContractDocxDerivationService;
import com.guoyu.platform.contract.service.ContractDocxDerivationService.ContractSourceManifest;
import com.guoyu.platform.file.artifact.dto.ContractPreviewGenerateReq;
import com.guoyu.platform.file.artifact.dto.ContractPreviewGenerateReq.FieldValue;
import com.guoyu.platform.file.artifact.vo.GeneratedArtifactEnvelopeVO;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.integration.bot.BotActorContext;
import com.guoyu.platform.integration.bot.BotActorContextHolder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractPreviewGenerationService {

  private static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String REQUEST_SCHEMA = "guoyu.contract-derivation-request/v1";
  private static final String CONTRACT_TYPE = "software-technology-service-framework";
  private static final Set<String> POC_FIELDS =
      Set.of("contractNumber", "partyA", "partyB", "amount", "projectName", "serviceScope");
  private static final Set<String> REQUIRED_FIELDS =
      Set.of("contractNumber", "partyA", "partyB", "amount");
  private static final Pattern CURRENCY_AMOUNT_PATTERN =
      Pattern.compile("[¥￥]\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)");
  private static final Pattern PAYMENT_FIELD_PATTERN =
      Pattern.compile("^paymentAmount\\.\\d+\\.\\d+\\.(\\d+(?:\\.\\d+)?)$");

  private final FileApplicationService fileApplicationService;
  private final ContractSourceSnapshotMapper snapshotMapper;
  private final ContractSourceManifestMapper manifestMapper;
  private final ContractDocxDerivationService docxDerivationService;

  public ContractPreviewGenerationService(
      FileApplicationService fileApplicationService,
      ContractSourceSnapshotMapper snapshotMapper,
      ContractSourceManifestMapper manifestMapper,
      ContractDocxDerivationService docxDerivationService) {
    this.fileApplicationService = fileApplicationService;
    this.snapshotMapper = snapshotMapper;
    this.manifestMapper = manifestMapper;
    this.docxDerivationService = docxDerivationService;
  }

  @Transactional
  public GeneratedArtifactEnvelopeVO generate(ContractPreviewGenerateReq request) {
    BotActorContext actor = requirePrivateActor();
    validateRequestContract(request);

    Long snapshotId = parseId(request.source().sourceSnapshotId(), "sourceSnapshotId");
    Long manifestId = parseId(request.source().manifestId(), "manifestId");
    ContractSourceSnapshotEntity snapshot = snapshotMapper.selectOne(
        new LambdaQueryWrapper<ContractSourceSnapshotEntity>()
            .eq(ContractSourceSnapshotEntity::getTenantId, actor.tenantId())
            .eq(ContractSourceSnapshotEntity::getId, snapshotId)
            .eq(ContractSourceSnapshotEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (snapshot == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Contract source snapshot not found");
    }
    validateSource(request, snapshot);

    ContractSourceManifestEntity manifestEntity = manifestMapper.selectOne(
        new LambdaQueryWrapper<ContractSourceManifestEntity>()
            .eq(ContractSourceManifestEntity::getTenantId, actor.tenantId())
            .eq(ContractSourceManifestEntity::getId, manifestId)
            .eq(ContractSourceManifestEntity::getSourceSnapshotId, snapshotId)
            .eq(ContractSourceManifestEntity::getVersion, request.source().manifestVersion())
            .eq(ContractSourceManifestEntity::getStatus, "active")
            .eq(ContractSourceManifestEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (manifestEntity == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "Contract source manifest version is unavailable");
    }
    ContractSourceManifest manifest = docxDerivationService.readManifest(manifestEntity.getManifestJson());
    Map<String, String> values = validateAndExtractValues(request.fields(), manifest);
    populateCalculatedPaymentAmounts(values, manifest);

    byte[] sourceBytes = fileApplicationService.readFileBytesById(snapshot.getSourceFileId());
    byte[] content = docxDerivationService.derive(sourceBytes, manifest, values);
    String filename = sanitizeFilename(stripDocx(snapshot.getOriginalFilename())) + "-派生草稿.docx";
    FileVO stored = fileApplicationService.storeFromBytesForTenant(
        actor.tenantId(), content, filename, DOCX_MIME, "private");
    FileEntity file = fileApplicationService.getExistingFile(actor.tenantId(), stored.id());
    String id = String.valueOf(file.getId());
    var artifact = new GeneratedArtifactEnvelopeVO.GeneratedArtifactRefVO(
        "platform-file", id, id, file.getOriginalName(), file.getContentType(),
        file.getSizeBytes(), file.getSha256(), 1, true);
    return new GeneratedArtifactEnvelopeVO(
        "guoyu.generated-artifacts/v1", "contract", id, id, List.of(artifact));
  }

  private void validateRequestContract(ContractPreviewGenerateReq request) {
    if (!REQUEST_SCHEMA.equals(request.schema())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported contract derivation request schema");
    }
    if (!CONTRACT_TYPE.equals(request.contractType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported contract type for the POC");
    }
    if (!"weknora-knowledge".equals(request.source().provider())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported contract source provider");
    }
    if (request.source().manifestVersion() == null || request.source().manifestVersion() < 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "manifestVersion must be positive");
    }
    if (request.sectionEdits() != null && !request.sectionEdits().isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_IMPLEMENTED,
          "Section-level edits are not enabled in the source derivation POC");
    }
    if (!request.options().draftWatermark()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "POC preview must keep the draft watermark policy");
    }
    if (!request.options().outputFormats().equals(List.of("docx"))) {
      throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "POC preview currently outputs DOCX only");
    }
  }

  private void validateSource(
      ContractPreviewGenerateReq request, ContractSourceSnapshotEntity snapshot) {
    if (!"derivable".equals(snapshot.getDerivationStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Contract source is not derivable");
    }
    if (!request.source().knowledgeBaseId().equals(snapshot.getKnowledgeBaseId())
        || !request.source().knowledgeId().equals(snapshot.getKnowledgeId())
        || !request.source().sourceFileHash().equals(snapshot.getSourceFileHash())
        || !request.source().originalFilename().equals(snapshot.getOriginalFilename())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Contract source snapshot metadata mismatch");
    }
  }

  private Map<String, String> validateAndExtractValues(
      Map<String, FieldValue> requestedFields, ContractSourceManifest manifest) {
    Map<String, String> values = new LinkedHashMap<>();
    for (Map.Entry<String, FieldValue> entry : requestedFields.entrySet()) {
      if (!POC_FIELDS.contains(entry.getKey())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "Field is not supported by the derivation POC: " + entry.getKey());
      }
      FieldValue fieldValue = entry.getValue();
      if (fieldValue == null || !StringUtils.hasText(fieldValue.value())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Contract field is empty: " + entry.getKey());
      }
      if (!manifest.detectedFields().containsKey(entry.getKey())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "Source document has no approved anchor for field: " + entry.getKey());
      }
      if (REQUIRED_FIELDS.contains(entry.getKey()) && !fieldValue.confirmed()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "Required contract field is not confirmed: " + entry.getKey());
      }
      values.put(entry.getKey(), fieldValue.value().trim());
    }
    for (String required : REQUIRED_FIELDS) {
      if (!values.containsKey(required)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "Required contract field is missing: " + required);
      }
    }
    return values;
  }

  private void populateCalculatedPaymentAmounts(
      Map<String, String> values, ContractSourceManifest manifest) {
    String amount = values.get("amount");
    if (!StringUtils.hasText(amount)) {
      return;
    }
    Matcher amountMatcher = CURRENCY_AMOUNT_PATTERN.matcher(amount);
    if (!amountMatcher.find()) {
      return;
    }
    BigDecimal total;
    try {
      total = new BigDecimal(amountMatcher.group(1).replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "Contract amount does not contain a valid currency number");
    }
    DecimalFormat currency = new DecimalFormat("#,##0.00",
        DecimalFormatSymbols.getInstance(Locale.US));
    String symbol = amount.contains("￥") ? "￥" : "¥";
    for (String field : manifest.detectedFields().keySet()) {
      Matcher paymentMatcher = PAYMENT_FIELD_PATTERN.matcher(field);
      if (!paymentMatcher.matches()) {
        continue;
      }
      BigDecimal percentage = new BigDecimal(paymentMatcher.group(1));
      BigDecimal payment = total.multiply(percentage)
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      values.put(field, symbol + currency.format(payment));
    }
  }

  private Long parseId(String value, String field) {
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, field + " is invalid");
    }
  }

  private BotActorContext requirePrivateActor() {
    BotActorContext actor = BotActorContextHolder.get();
    if (actor == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Verified bot actor context is required");
    }
    if (!actor.isPrivateChat()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Contract files can only be generated for a private chat");
    }
    return actor;
  }

  private String stripDocx(String filename) {
    return filename.replaceFirst("(?i)\\.docx$", "");
  }

  private String sanitizeFilename(String value) {
    String safe = value.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    return safe.isEmpty() ? "合同预览" : safe;
  }
}
