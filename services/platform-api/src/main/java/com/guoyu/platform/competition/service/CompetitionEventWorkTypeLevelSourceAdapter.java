package com.guoyu.platform.competition.service;

import java.util.List;

/** Read-only adapter boundary from exam master data into competition snapshots. */
public interface CompetitionEventWorkTypeLevelSourceAdapter {

  List<SourceLevel> listAvailableLevels(Long tenantId, Long workTypeId);

  record SourceLevel(String levelCode, String levelName, String sourceLevelCode, String sourceLevelText) {
  }
}
