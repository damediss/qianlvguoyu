package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Simulation is intentionally a placeholder and always blocks publication/settlement. */
@Component
public class CompetitionSimulationContentProvider implements CompetitionStageContentProvider {
  @Override
  public String contentType() { return "simulation"; }

  @Override
  public List<String> preflight(List<CompetitionStagePlanResourceEntity> resources) {
    return List.of("SIMULATION_PROVIDER_UNAVAILABLE");
  }
}
