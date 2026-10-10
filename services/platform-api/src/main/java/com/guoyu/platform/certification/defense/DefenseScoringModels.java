package com.guoyu.platform.certification.defense;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
      String admissionTicketNo,
      String workTypeName,
      String levelName,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName,
      String thesisStatus,
      BigDecimal thesisFinalScore,
      Boolean thesisRejected,
      String performanceStatus,
      BigDecimal performanceFinalScore,
      Boolean performanceRejected
  ) {
  }

  public record AssessorReportDetail(
      @JsonSerialize(using = ToStringSerializer.class) Long reportId,
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      String taskName,
      @JsonSerialize(using = ToStringSerializer.class) Long candidateId,
      String candidateName,
      String maskedIdCardNo,
      String admissionTicketNo,
      String workTypeName,
      String levelName,
      String orgName,
      @JsonSerialize(using = ToStringSerializer.class) Long fileId,
      String fileName,
      String thesisStatus,
      BigDecimal thesisFinalScore,
      Boolean thesisRejected,
      String performanceStatus,
      BigDecimal performanceFinalScore,
      Boolean performanceRejected
  ) {
  }

  public record ThesisPayload(
      String paperTitle,
      List<Boolean> rejectFlags,
      String rejectNote,
      List<Integer> contentScores,
      List<String> questions,
      List<Integer> defenseScores
  ) {
  }

  public record PerformanceItemScore(
      String key,
      Integer score
  ) {
  }

  public record PerformancePayload(
      List<PerformanceItemScore> items,
      Boolean disqualify,
      String remark
  ) {
  }

  public record ScoreComputation(
      BigDecimal contentSubtotal,
      BigDecimal defenseSubtotal,
      BigDecimal rawTotal,
      BigDecimal finalScore,
      boolean rejected
  ) {
  }

  public record SaveScoreSheetReq(
      ThesisPayload thesis,
      PerformancePayload performance,
      @JsonSerialize(using = ToStringSerializer.class) Long signatureFileId,
      @Size(max = 128) String signatureHash
  ) {
  }

  public record SubmitScoreSheetReq(
      ThesisPayload thesis,
      PerformancePayload performance,
      @NotNull Long signatureFileId,
      @NotBlank @Size(min = 64, max = 64) String signatureHash
  ) {
  }

  public record ScoreSheetView(
      String sheetType,
      String status,
      ThesisPayload thesis,
      PerformancePayload performance,
      BigDecimal contentSubtotal,
      BigDecimal defenseSubtotal,
      BigDecimal rawTotal,
      BigDecimal finalScore,
      Boolean rejected,
      @JsonSerialize(using = ToStringSerializer.class) Long signatureFileId,
      String signatureHash,
      String assessorName,
      LocalDateTime submittedAt
  ) {
  }

  public record ScoreSheetsView(
      AssessorReportDetail candidate,
      ScoreSheetView thesis,
      ScoreSheetView performance
  ) {
  }

  public record ExpertView(
      @JsonSerialize(using = ToStringSerializer.class) Long assessorId,
      String assessorName,
      boolean leader
  ) {
  }

  public record AssessorScoreView(
      @JsonSerialize(using = ToStringSerializer.class) Long assessorId,
      String assessorName,
      boolean leader,
      String thesisStatus,
      BigDecimal thesisScore,
      Boolean thesisRejected,
      String performanceStatus,
      BigDecimal performanceScore,
      Boolean performanceRejected
  ) {
  }

  public record CandidateProgressView(
      @JsonSerialize(using = ToStringSerializer.class) Long candidateId,
      String candidateNo,
      String admissionTicketNo,
      String candidateName,
      String maskedIdCardNo,
      String idCardNo,
      String workTypeName,
      String levelName,
      boolean reportUploaded,
      String status,
      List<BigDecimal> thesisScores,
      BigDecimal thesisAverage,
      List<BigDecimal> performanceScores,
      BigDecimal performanceAverage,
      BigDecimal finalScore,
      Boolean rejected,
      List<AssessorScoreView> assessors
  ) {
  }

  public record ProgressView(
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      String taskNo,
      String taskName,
      String workTypeName,
      String levelName,
      String examDate,
      String examSite,
      String overallStatus,
      String configStatus,
      String groupName,
      int submittedCandidateCount,
      int totalCandidateCount,
      List<ExpertView> experts,
      List<CandidateProgressView> candidates
  ) {
  }

  public record CompleteView(
      @JsonSerialize(using = ToStringSerializer.class) Long taskId,
      String status,
      int candidateCount
  ) {
  }
}
