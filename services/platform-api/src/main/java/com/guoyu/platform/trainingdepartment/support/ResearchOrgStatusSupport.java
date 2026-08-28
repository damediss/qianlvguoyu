package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.entity.TrainResearchOrgEntity;

/** 调研企业启用/禁用（status：1 启用，0 禁用） */
public final class ResearchOrgStatusSupport {

  public static final String CODE_DISABLED = "RESEARCH_ORG_DISABLED";
  public static final String MSG_DISABLED = "该调研企业已禁用，无法采集";

  private ResearchOrgStatusSupport() {}

  public static boolean isEnabled(TrainResearchOrgEntity entity) {
    return entity != null && entity.getStatus() != null && entity.getStatus() == 1;
  }

  public static void assertEnabled(TrainResearchOrgEntity entity) {
    if (!isEnabled(entity)) {
      throw new BusinessException(CODE_DISABLED, MSG_DISABLED);
    }
  }
}
