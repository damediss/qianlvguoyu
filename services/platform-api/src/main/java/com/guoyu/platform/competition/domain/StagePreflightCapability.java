package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import java.util.List;

/** Provider capability for checking whether a content type can execute. */
public interface StagePreflightCapability {
  List<String> preflight(List<CompetitionStagePlanResourceEntity> resources);
}
