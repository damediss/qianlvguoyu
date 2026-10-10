package com.guoyu.platform.file.artifact.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ContractPreviewGenerateReq(
    @NotBlank String schema,
    @NotBlank String contractType,
    @Valid @NotNull SourceDocument source,
    @NotEmpty Map<@Size(max = 64) String, @Valid FieldValue> fields,
    @Valid @Size(max = 50) List<SectionEdit> sectionEdits,
    @Valid @NotNull RenderOptions options
) {

  public record SourceDocument(
      @NotBlank String provider,
      @NotBlank @Size(max = 64) String knowledgeBaseId,
      @NotBlank @Size(max = 64) String knowledgeId,
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,31}$") String sourceSnapshotId,
      @NotBlank @Size(max = 255) @Pattern(regexp = ".*\\.[dD][oO][cC][xX]$") String originalFilename,
      @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String sourceFileHash,
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,31}$") String manifestId,
      @NotNull Integer manifestVersion,
      @Size(max = 1000) String selectionReason,
      @AssertTrue boolean userConfirmed
  ) {}

  public record FieldValue(
      @NotBlank @Size(max = 5000) String value,
      @NotBlank @Pattern(regexp = "user|source_document|rag|wiki|system|agent_inference") String source,
      @Size(max = 64) String sourceDocumentId,
      boolean confirmed
  ) {}

  public record SectionEdit(
      @NotBlank @Size(max = 128) String anchorId,
      @NotBlank String operation,
      @Size(max = 20000) String content,
      @NotBlank String source,
      @Size(max = 64) String sourceDocumentId,
      boolean confirmed,
      @AssertTrue boolean preserveStyle
  ) {}

  public record RenderOptions(
      @AssertTrue boolean preserveLayout,
      boolean draftWatermark,
      @NotEmpty List<@Pattern(regexp = "docx|pdf") String> outputFormats
  ) {}
}
