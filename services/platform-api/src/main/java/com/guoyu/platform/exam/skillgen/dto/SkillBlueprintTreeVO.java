package com.guoyu.platform.exam.skillgen.dto;

import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 操作技能细目表批次 + 节点树 VO */
public record SkillBlueprintTreeVO(SkillBlueprintVO blueprint, List<NodeVO> tree) {

  public record NodeVO(Long id, Long parentId, Integer nodeLevel, String code, String fullCode,
      String name, java.math.BigDecimal weight, String selectMode, String skillRequirement,
      String importance, Integer questionQuota, String xyzStat, Integer sortOrder,
      List<NodeVO> children) {
  }

  public static SkillBlueprintTreeVO from(SkillBlueprintEntity bp,
      List<SkillBlueprintNodeEntity> nodes) {
    Map<Long, List<NodeVO>> byParent = nodes.stream()
        .map(n -> new NodeVO(n.getId(), n.getParentId(), n.getNodeLevel(), n.getCode(),
            n.getFullCode(), n.getName(), n.getWeight(), n.getSelectMode(), n.getSkillRequirement(),
            n.getImportance(), n.getQuestionQuota(), n.getXyzStat(), n.getSortOrder(),
            new ArrayList<>()))
        .collect(Collectors.groupingBy(NodeVO::parentId));
    List<NodeVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
    roots.sort(Comparator.comparing(NodeVO::sortOrder));
    for (NodeVO root : roots) {
      fillChildren(root, byParent);
    }
    return new SkillBlueprintTreeVO(SkillBlueprintVO.from(bp, null), roots);
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
