package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class CompetitionScoringSystemBCalculator {

  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal TWO = new BigDecimal("2");

  public SystemBCalculationResult calculate(
      ScoreInput theory,
      ScoreInput simulation,
      ScoreInput practical,
      BigDecimal theoryWeight,
      BigDecimal simulationPracticalWeight) {
    validateInput("理论", theory);
    validateInput("仿真", simulation);
    validateInput("实操", practical);
    validateWeights(theoryWeight, simulationPracticalWeight);

    BigDecimal simulationPracticalScore =
        simulation.score().add(practical.score()).divide(TWO, MathContext.DECIMAL128);
    if (!theory.confirmed() || !simulation.confirmed() || !practical.confirmed()) {
      return new SystemBCalculationResult(
          CalculationStatus.PENDING_UNCONFIRMED_SOURCE,
          simulationPracticalScore,
          null);
    }

    BigDecimal theoryPart = theory.score()
        .multiply(theoryWeight)
        .divide(HUNDRED, MathContext.DECIMAL128);
    BigDecimal simulationPracticalPart = simulationPracticalScore
        .multiply(simulationPracticalWeight)
        .divide(HUNDRED, MathContext.DECIMAL128);
    return new SystemBCalculationResult(
        CalculationStatus.CONFIRMED,
        simulationPracticalScore,
        theoryPart.add(simulationPracticalPart));
  }

  /** New level-scoped formula. The final score is the published two-decimal value. */
  public SystemBCalculationResult calculateLevelScoped(
      ScoreInput theory,
      ScoreInput simulation,
      ScoreInput practical,
      BigDecimal theoryWeight,
      BigDecimal simulationWeight,
      BigDecimal practicalWeight) {
    validateInput("理论", theory);
    validateInput("仿真", simulation);
    validateInput("实操", practical);
    validateIndependentWeights(theoryWeight, simulationWeight, practicalWeight);
    BigDecimal simulationPracticalScore = simulation.score()
        .add(practical.score()).divide(TWO, 2, RoundingMode.HALF_UP);
    if (!theory.confirmed() || !simulation.confirmed() || !practical.confirmed()) {
      return new SystemBCalculationResult(
          CalculationStatus.PENDING_UNCONFIRMED_SOURCE, simulationPracticalScore, null);
    }
    BigDecimal total = theory.score().multiply(theoryWeight)
        .add(simulation.score().multiply(simulationWeight))
        .add(practical.score().multiply(practicalWeight))
        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    return new SystemBCalculationResult(
        CalculationStatus.CONFIRMED, simulationPracticalScore, total);
  }

  private void validateInput(String name, ScoreInput input) {
    if (input == null || input.score() == null) {
      throw new IllegalArgumentException(name + "成绩不能为空");
    }
    if (input.score().compareTo(BigDecimal.ZERO) < 0
        || input.score().compareTo(HUNDRED) > 0) {
      throw new IllegalArgumentException(name + "成绩必须在0到100之间");
    }
  }

  private void validateWeights(
      BigDecimal theoryWeight, BigDecimal simulationPracticalWeight) {
    if (theoryWeight == null || simulationPracticalWeight == null) {
      throw new IllegalArgumentException("赛制B权重不能为空");
    }
    if (theoryWeight.compareTo(BigDecimal.ZERO) < 0
        || theoryWeight.compareTo(HUNDRED) > 0
        || simulationPracticalWeight.compareTo(BigDecimal.ZERO) < 0
        || simulationPracticalWeight.compareTo(HUNDRED) > 0
        || theoryWeight.add(simulationPracticalWeight).compareTo(HUNDRED) != 0) {
      throw new IllegalArgumentException("赛制B两项权重必须在0到100之间且合计为100");
    }
  }

  private void validateIndependentWeights(
      BigDecimal theoryWeight,
      BigDecimal simulationWeight,
      BigDecimal practicalWeight) {
    if (theoryWeight == null || simulationWeight == null || practicalWeight == null
        || theoryWeight.compareTo(BigDecimal.ZERO) < 0
        || simulationWeight.compareTo(BigDecimal.ZERO) < 0
        || practicalWeight.compareTo(BigDecimal.ZERO) < 0
        || theoryWeight.compareTo(HUNDRED) > 0
        || simulationWeight.compareTo(HUNDRED) > 0
        || practicalWeight.compareTo(HUNDRED) > 0
        || theoryWeight.add(simulationWeight).add(practicalWeight).compareTo(HUNDRED) != 0) {
      throw new IllegalArgumentException("理论、仿真、实操权重必须在0到100之间且合计为100");
    }
  }

  public record ScoreInput(BigDecimal score, boolean confirmed) {
  }

  public record SystemBCalculationResult(
      CalculationStatus status,
      BigDecimal simulationPracticalScore,
      BigDecimal totalScore) {

    public boolean confirmed() {
      return status == CalculationStatus.CONFIRMED;
    }
  }

  public enum CalculationStatus {
    CONFIRMED,
    PENDING_UNCONFIRMED_SOURCE
  }
}
