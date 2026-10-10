package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import java.math.BigDecimal;
import java.util.List;

/**
 * Interprets 赛段编排「内容权重占比」as the combined-score formula.
 * Enabled subjects must sum to 100; missing or disabled subjects contribute 0.
 */
public final class CompetitionStageContentScoringWeights {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final BigDecimal theoryWeight;
  private final BigDecimal simulationWeight;
  private final BigDecimal practicalWeight;
  private final boolean valid;

  private CompetitionStageContentScoringWeights(
      BigDecimal theoryWeight,
      BigDecimal simulationWeight,
      BigDecimal practicalWeight,
      boolean valid) {
    this.theoryWeight = theoryWeight;
    this.simulationWeight = simulationWeight;
    this.practicalWeight = practicalWeight;
    this.valid = valid;
  }

  public static CompetitionStageContentScoringWeights invalid() {
    return new CompetitionStageContentScoringWeights(ZERO, ZERO, ZERO, false);
  }

  public static CompetitionStageContentScoringWeights from(
      List<CompetitionStageContentEntity> contents) {
    if (contents == null || contents.isEmpty()) {
      return invalid();
    }
    BigDecimal theory = ZERO;
    BigDecimal simulation = ZERO;
    BigDecimal practical = ZERO;
    BigDecimal enabledTotal = ZERO;
    int enabledCount = 0;
    for (CompetitionStageContentEntity content : contents) {
      if (content == null || content.getContentType() == null) {
        return invalid();
      }
      String type = content.getContentType().trim().toLowerCase();
      if (!"theory".equals(type) && !"simulation".equals(type) && !"practical".equals(type)) {
        return invalid();
      }
      boolean enabled = content.getStatus() == null
          || content.getStatus().isBlank()
          || "enabled".equalsIgnoreCase(content.getStatus());
      BigDecimal weight = content.getWeight() == null ? ZERO : content.getWeight();
      if (enabled) {
        if (weight.compareTo(ZERO) <= 0) {
          return invalid();
        }
        enabledCount++;
        enabledTotal = enabledTotal.add(weight);
        if ("theory".equals(type)) {
          theory = weight;
        } else if ("simulation".equals(type)) {
          simulation = weight;
        } else {
          practical = weight;
        }
      } else if (weight.compareTo(ZERO) != 0) {
        return invalid();
      }
    }
    if (enabledCount == 0 || enabledTotal.compareTo(HUNDRED) != 0) {
      return invalid();
    }
    return new CompetitionStageContentScoringWeights(theory, simulation, practical, true);
  }

  public boolean valid() {
    return valid;
  }

  public boolean enabled(String sourceType) {
    return weight(sourceType).compareTo(ZERO) > 0;
  }

  public boolean theoryOnly() {
    return valid && enabled("theory") && !enabled("simulation") && !enabled("practical");
  }

  public BigDecimal theoryWeight() {
    return theoryWeight;
  }

  public BigDecimal simulationWeight() {
    return simulationWeight;
  }

  public BigDecimal practicalWeight() {
    return practicalWeight;
  }

  public BigDecimal combinedSimulationPracticalWeight() {
    return simulationWeight.add(practicalWeight);
  }

  public BigDecimal weight(String sourceType) {
    if ("theory".equalsIgnoreCase(sourceType)) {
      return theoryWeight;
    }
    if ("simulation".equalsIgnoreCase(sourceType)) {
      return simulationWeight;
    }
    if ("practical".equalsIgnoreCase(sourceType)) {
      return practicalWeight;
    }
    return ZERO;
  }
}
