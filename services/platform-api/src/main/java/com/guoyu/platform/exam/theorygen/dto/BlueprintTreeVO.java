package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 细目表批次 + 节点树 VO */
public record BlueprintTreeVO(BlueprintVO blueprint, List<NodeVO> tree) {

  public record NodeVO(Long id, Long parentId, Integer nodeLevel, String code, String fullCode,
      String name, java.math.BigDecimal weight, String importance, String relatedKnowledge,
      String xyzStat, Long knowledgeId, Integer sortOrder, List<NodeVO> children) {
  }

  public static BlueprintTreeVO from(TheoryBlueprintEntity bp,
      List<TheoryBlueprintNodeEntity> nodes) {
    Map<Long, List<NodeVO>> byParent = nodes.stream()
        .map(n -> new NodeVO(n.getId(), n.getParentId(), n.getNodeLevel(), n.getCode(),
            n.getFullCode(), n.getName(), n.getWeight(), n.getImportance(),
            n.getRelatedKnowledge(), n.getXyzStat(), n.getKnowledgeId(), n.getSortOrder(),
            new ArrayList<>()))
        .collect(Collectors.groupingBy(NodeVO::parentId));
    List<NodeVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
    roots.sort(Comparator.comparing(NodeVO::sortOrder));
    for (NodeVO root : roots) {
      fillChildren(root, byParent);
    }
    return new BlueprintTreeVO(BlueprintVO.from(bp, null), roots);
  }

  private static void fillChildren(NodeVO parent, Map<Long, List<NodeVO>> byParent) {
    List<NodeVO> children = byParent.getOrDefault(parent.id(), new ArrayList<>());
    children.sort(Comparator.comparing(NodeVO::sortOrder));
    for (NodeVO child : children) {
      fillChildren(child, byParent);
    }
    parent.children().addAll(children);
  }
}
