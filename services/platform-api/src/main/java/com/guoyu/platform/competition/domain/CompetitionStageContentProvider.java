package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import java.util.List;

/** Provider boundary for theory/simulation/practical resources; providers never access another domain's Mapper. */
public interface CompetitionStageContentProvider extends StagePreflightCapability {
  String contentType();
}
