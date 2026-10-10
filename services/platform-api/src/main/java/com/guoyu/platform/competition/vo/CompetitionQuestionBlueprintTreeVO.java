package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;

public record CompetitionQuestionBlueprintTreeVO(
    CompetitionQuestionBlueprintVO blueprint,
    List<NodeVO> tree
) {

  /**
   * Compares blueprint codes by hierarchy, putting alphabetic tokens before
   * numeric tokens and comparing numeric tokens by value (for example, 2
   * comes before 10).  The imported row order is not a reliable display order.
   */
  public static int compareCodes(String left, String right) {
    if (left == null || left.isBlank()) return right == null || right.isBlank() ? 0 : 1;
    if (right == null || right.isBlank()) return -1;

    String[] leftParts = left.trim().split("[-_.]+");
    String[] rightParts = right.trim().split("[-_.]+");
    int commonSize = Math.min(leftParts.length, rightParts.length);
    for (int i = 0; i < commonSize; i++) {
      int byPart = compareCodePart(leftParts[i], rightParts[i]);
      if (byPart != 0) return byPart;
    }
    return Integer.compare(leftParts.length, rightParts.length);
  }

  public record NodeVO(
      String id, String parentId, Integer nodeLevel, String code, String fullCode, String name,
      BigDecimal weight, String selectMode, String skillRequirement, String relatedKnowledge,
      String importance, String xyzStat, Integer questionQuota, Integer sortOrder,
      List<NodeVO> children
  ) {}

  public static CompetitionQuestionBlueprintTreeVO from(
      CompetitionQuestionBlueprintEntity blueprint,
      List<CompetitionQuestionBlueprintNodeEntity> nodes) {
    Map<Long, List<NodeVO>> byParent = nodes.stream()
        .map(n -> new NodeVO(String.valueOf(n.getId()), String.valueOf(n.getParentId()), n.getNodeLevel(),
            n.getCode(), n.getFullCode(), n.getName(), n.getWeight(), n.getSelectMode(),
            n.getSkillRequirement(), n.getRelatedKnowledge(), n.getImportance(), n.getXyzStat(),
            n.getQuestionQuota(), n.getSortOrder(), new ArrayList<>()))
        .collect(Collectors.groupingBy(n -> Long.valueOf(n.parentId())));
    List<NodeVO> roots = new ArrayList<>(byParent.getOrDefault(0L, List.of()));
    roots.sort(CompetitionQuestionBlueprintTreeVO::compareNodes);
    for (NodeVO node : roots) fill(node, byParent);
    return new CompetitionQuestionBlueprintTreeVO(toVO(blueprint), roots);
  }

  private static void fill(NodeVO parent, Map<Long, List<NodeVO>> byParent) {
    List<NodeVO> children = new ArrayList<>(byParent.getOrDefault(Long.valueOf(parent.id()), List.of()));
    children.sort(CompetitionQuestionBlueprintTreeVO::compareNodes);
    for (NodeVO child : children) fill(child, byParent);
    parent.children().addAll(children);
  }

  private static int compareNodes(NodeVO left, NodeVO right) {
    int byCode = compareCodes(sortCode(left), sortCode(right));
    if (byCode != 0) return byCode;
    int bySortOrder = Comparator.nullsLast(Integer::compareTo).compare(left.sortOrder(), right.sortOrder());
    if (bySortOrder != 0) return bySortOrder;
    return Comparator.nullsLast(String::compareTo).compare(left.id(), right.id());
  }

  private static String sortCode(NodeVO node) {
    return node.fullCode() == null || node.fullCode().isBlank() ? node.code() : node.fullCode();
  }

  private static int compareCodePart(String left, String right) {
    int leftIndex = 0;
    int rightIndex = 0;
    while (leftIndex < left.length() && rightIndex < right.length()) {
      char leftChar = left.charAt(leftIndex);
      char rightChar = right.charAt(rightIndex);
      boolean leftDigit = Character.isDigit(leftChar);
      boolean rightDigit = Character.isDigit(rightChar);
      if (leftDigit && rightDigit) {
        int leftEnd = leftIndex;
        int rightEnd = rightIndex;
        while (leftEnd < left.length() && Character.isDigit(left.charAt(leftEnd))) leftEnd++;
        while (rightEnd < right.length() && Character.isDigit(right.charAt(rightEnd))) rightEnd++;
        int byNumber = new BigInteger(left.substring(leftIndex, leftEnd))
            .compareTo(new BigInteger(right.substring(rightIndex, rightEnd)));
        if (byNumber != 0) return byNumber;
        int byWidth = Integer.compare(leftEnd - leftIndex, rightEnd - rightIndex);
        if (byWidth != 0) return byWidth;
        leftIndex = leftEnd;
        rightIndex = rightEnd;
        continue;
      }
      if (leftDigit != rightDigit) return leftDigit ? 1 : -1;

      int byLetter = Character.compare(Character.toUpperCase(leftChar), Character.toUpperCase(rightChar));
      if (byLetter != 0) return byLetter;
      int byCase = Character.compare(leftChar, rightChar);
      if (byCase != 0) return byCase;
      leftIndex++;
      rightIndex++;
    }
    return Integer.compare(left.length(), right.length());
  }

  public static CompetitionQuestionBlueprintVO toVO(CompetitionQuestionBlueprintEntity e) {
    return new CompetitionQuestionBlueprintVO(String.valueOf(e.getId()), String.valueOf(e.getEventId()),
        String.valueOf(e.getEventWorkTypeId()), String.valueOf(e.getEventWorkTypeLevelId()),
        e.getContentType(), e.getVersionNo(), e.getTitle(), e.getSourceFileName(),
        e.getSourceFileId() == null ? null : String.valueOf(e.getSourceFileId()), e.getPointCount(),
        e.getTopicCount(), e.getXyzRatio(), e.getWeightCheck(), e.getCheckReport(), e.getStatus(), e.getCreatedAt());
  }
}
