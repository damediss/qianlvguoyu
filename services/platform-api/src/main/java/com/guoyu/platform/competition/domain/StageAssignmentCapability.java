package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStageAssignmentEntity;
import java.util.List;

/** Provider capability for content-specific assignment validation. */
public interface StageAssignmentCapability {
  List<String> validateAssignment(CompetitionStageAssignmentEntity assignment);
}
