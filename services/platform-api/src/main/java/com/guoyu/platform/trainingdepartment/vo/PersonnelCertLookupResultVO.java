package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record PersonnelCertLookupResultVO(
    int total,
    int verified,
    int notFound,
    int invalid,
    int failed,
    List<PersonnelCertLookupDetail> details
) {}
