package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import java.util.List;

/** Provider capability for allocation and allocation confirmation. */
public interface StageAllocationCapability {
  List<CompetitionStagePlanResourceEntity> allocate(List<CompetitionStagePlanResourceEntity> resources);

  List<String> confirmAllocation(List<CompetitionStagePlanResourceEntity> resources);
}
