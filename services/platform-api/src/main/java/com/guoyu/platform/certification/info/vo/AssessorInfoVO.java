package com.guoyu.platform.certification.info.vo;

import java.time.LocalDate;
import java.util.List;

public record AssessorInfoVO(
    Long id,
    Long userId,
    String accountUsername,
    String accountPassword,
    String assessorName,
    String identityNo,
    String certificateNo,
    Long certificatePhotoFileId,
    String certificatePhotoFileName,
    String workTypeScope,
    List<String> workTypeScopes,
    String levelScope,
    LocalDate validUntil,
    String maskedPhone,
    String auditStatus,
    String workStatus,
    String materialsDescription,
    String remark,
    List<String> workTypeIds,
    String workTypeMappingWarning
) {
}
