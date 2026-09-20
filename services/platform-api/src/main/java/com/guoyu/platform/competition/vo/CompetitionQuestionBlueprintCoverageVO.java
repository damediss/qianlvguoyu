package com.guoyu.platform.competition.vo;

import java.util.List;

/** 细目表末级考评点的题库绑定覆盖率。 */
public record CompetitionQuestionBlueprintCoverageVO(
    String blueprintId,
    Integer totalPoints,
    Integer coveredPoints,
    Integer totalQuestions,
    Integer boundQuestions,
    List<PointCoverage> points
) {
  public record PointCoverage(
      String nodeId,
      String fullCode,
      String path,
      String name,
      Integer questionQuota,
      Integer boundQuestionCount,
      Integer shortage
  ) {}
}
