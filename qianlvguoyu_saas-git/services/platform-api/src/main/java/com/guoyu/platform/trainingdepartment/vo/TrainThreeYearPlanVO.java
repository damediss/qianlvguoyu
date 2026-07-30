package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TrainThreeYearPlanVO(
    Long id,
    String planNo,
    String title,
    Long targetOrgId,
    String targetOrgName,
    Long researchOrgId,
    String researchOrgTitle,
    Long rencaiTaskId,
    Long rencaiInputFileId,
    String planYears,
    Integer status,
    String statusLabel,
    String analysisStatus,
    String rejectReason,
    List<RejectHistoryItem> rejectHistory,
    List<SubmitHistoryItem> submitHistory,
    LocalDateTime companyConfirmTime,
    String companyConfirmUsername,
    Long createdBy,
    LocalDateTime createTime,
    LocalDateTime updateTime,
    Integer totalRows,
    Integer recommendedRows,
    Double progressPct,
    String rencaiTaskStatus,
    Long submitPersonnelFileId,
    Long submitSchemeFileId,
    String submitPersonnelFileName,
    String submitSchemeFileName,
    Long enterpriseRosterFileId,
    Long enterpriseShiftFileId,
    String enterpriseRosterFileName,
    String enterpriseShiftFileName,
    LocalDateTime enterpriseReturnTime,
    String enterpriseReturnUsername,
    Boolean enterpriseReturnReady,
    List<EnterpriseReturnHistoryItem> enterpriseReturnHistory
) {

  /** 驳回历史条目 */
  public record RejectHistoryItem(
      String reason,
      String rejectedBy,
      String rejectedByUsername,
      LocalDateTime rejectedAt
  ) {}

  /** 提交历史条目 */
  public record SubmitHistoryItem(
      String submittedBy,
      String submittedByUsername,
      LocalDateTime submittedAt,
      String personnelFileName,
      String schemeFileName
  ) {}

  /** 企业回传历史条目 */
  public record EnterpriseReturnHistoryItem(
      Long rosterFileId,
      String rosterFileName,
      Long shiftFileId,
      String shiftFileName,
      String uploadedBy,
      String uploadedByUsername,
      LocalDateTime uploadedAt,
      String remark
  ) {}
}
