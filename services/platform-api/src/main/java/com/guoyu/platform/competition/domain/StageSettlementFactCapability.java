package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import java.util.List;

/** Provider capability for selecting immutable, confirmed settlement facts. */
public interface StageSettlementFactCapability {
  List<CompetitionScoreSourceEntity> collectSettlementFacts(List<CompetitionScoreSourceEntity> sources);
}
