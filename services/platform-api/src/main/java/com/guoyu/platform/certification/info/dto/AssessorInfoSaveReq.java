package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AssessorInfoSaveReq(
    @NotBlank String assessorName,
    @Size(max = 32) String identityNo,
    @Size(max = 64) String accountUsername,
    @Size(min = 8, max = 128) String accountPassword,
    @Size(max = 120) String certificateNo,
    Long certificatePhotoFileId,
    @Size(max = 255) String certificatePhotoFileName,
    @Deprecated String workTypeScope,
    @NotEmpty List<@NotBlank String> workTypeScopes,
    String levelScope,
    @NotNull LocalDate validUntil,
    String phone,
    String materialsDescription,
    String remark
) {
}
