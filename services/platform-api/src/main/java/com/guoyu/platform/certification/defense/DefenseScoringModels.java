package com.guoyu.platform.certification.defense;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class DefenseScoringModels {

  private DefenseScoringModels() {
  }

  public record SaveConfigReq(@NotNull Long groupId) {
  }

  public record ReportFileReq(@NotNull Long fileId, String fileName) {
  }

  public record MatchReportsReq(@NotEmpty List<@Valid ReportFileReq> files) {
  }

  public record GroupOption(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      String groupName,
      @JsonSerialize(using = ToStringSerializer.class) Long leaderAssessorId,
      String leaderAssessorName,
      int memberCount
  ) {
  }

  public record ConfigView(
      @JsonSerialize(using = ToStringSerializer.class) Long id,
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      @JsonSerialize(using = ToStringSerializer.class) Long groupId,
      String groupName,
      String status,
      int memberCount
  ) {
  }

  public record UnmatchedFileView(
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName,
      String reason
  ) {
  }

  public record MatchResultView(
      int matched,
      int unmatched,
      int replaced,
      List<UnmatchedFileView> unmatchedFiles
  ) {
  }

  public record CandidateRowView(
      @JsonSerialize(using = ToStringSerializer.class) Long candidateId,
      String candidateNo,
      String candidateName,
      String maskedIdCardNo,
      String idCardNo,
      String workTypeName,
      String levelName,
      String orgName,
      @JsonSerialize(using = ToStringSerializer.class) Long reportId,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName,
      LocalDateTime uploadedAt
  ) {
  }

  public record SetupView(
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      ConfigView config,
      List<GroupOption> groups,
      List<CandidateRowView> candidates
  ) {
  }

  public record AssessorReportListItem(
      @JsonSerialize(using = ToStringSerializer.class) Long reportId,
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      String taskName,
      @JsonSerialize(using = ToStringSerializer.class) Long candidateId,
      String candidateName,
      String maskedIdCardNo,
      String workTypeName,
      String levelName,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName
  ) {
  }

  public record AssessorReportDetail(
      @JsonSerialize(using = ToStringSerializer.class) Long reportId,
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      String taskName,
      @JsonSerialize(using = ToStringSerializer.class) Long candidateId,
      String candidateName,
      String maskedIdCardNo,
      String workTypeName,
      String levelName,
      String orgName,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName
  ) {
  }
}
