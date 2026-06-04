package com.guoyu.platform.certification.vo;

import java.util.List;

public record CertificationWorkbenchVO(
    CertificationTaskVO task,
    List<CertificationPhaseVO> phases,
    List<CertificationDocumentVO> documents,
    List<CertificationMemberVO> members,
    List<CertificationWbsItemVO> wbsItems,
    List<CertificationScheduleItemVO> scheduleItems,
    List<CertificationRiskVO> risks
) {
}
