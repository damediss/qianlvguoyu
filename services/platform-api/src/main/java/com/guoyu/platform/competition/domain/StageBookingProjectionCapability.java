package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageResourceBookingEntity;
import java.util.List;

/** Provider capability for turning frozen resources into booking facts. */
public interface StageBookingProjectionCapability {
  List<CompetitionStageResourceBookingEntity> projectBookings(
      List<CompetitionStagePlanResourceEntity> resources);
}
