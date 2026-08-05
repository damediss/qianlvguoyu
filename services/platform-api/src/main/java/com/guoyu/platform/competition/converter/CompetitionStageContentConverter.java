package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.vo.CompetitionStageContentVO;

public final class CompetitionStageContentConverter {

  private CompetitionStageContentConverter() {
  }

  public static CompetitionStageContentVO toVO(CompetitionStageContentEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionStageContentVO target = new CompetitionStageContentVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setStageId(source.getStageId());
    target.setContentType(source.getContentType());
    target.setStatus(source.getStatus());
    target.setWeight(source.getWeight());
    target.setSortOrder(source.getSortOrder());
    target.setProviderCode(source.getProviderCode());
    return target;
  }
}
