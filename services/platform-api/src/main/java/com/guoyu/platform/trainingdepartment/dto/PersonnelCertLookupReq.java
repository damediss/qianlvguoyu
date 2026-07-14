package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PersonnelCertLookupReq(
    @NotEmpty(message = "请至少提交一名人员")
    @Size(max = 50, message = "单次最多提交 50 人")
    @Valid
    List<PersonnelCertLookupPerson> people
) {}
