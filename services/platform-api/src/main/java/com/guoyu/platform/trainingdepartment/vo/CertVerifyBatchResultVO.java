package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record CertVerifyBatchResultVO(
    int total,
    int verified,
    int notFound,
    int failed,
    String message,
    List<CertVerifyDetail> details
) {}
