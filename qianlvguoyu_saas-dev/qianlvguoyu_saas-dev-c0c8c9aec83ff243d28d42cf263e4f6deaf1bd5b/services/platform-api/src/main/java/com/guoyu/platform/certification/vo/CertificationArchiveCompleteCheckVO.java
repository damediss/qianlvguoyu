package com.guoyu.platform.certification.vo;

import java.util.List;

public record CertificationArchiveCompleteCheckVO(
    boolean passed,
    List<CertificationArchiveCheckItemVO> items
) {
}
