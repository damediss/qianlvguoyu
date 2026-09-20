package com.guoyu.platform.competition.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionQuestionBlueprintTreeVOTest {

  @Test
  void sortsEveryTreeLevelByAlphabeticAndNumericCodeInsteadOfImportRowOrder() {
    CompetitionQuestionBlueprintEntity blueprint = new CompetitionQuestionBlueprintEntity();
    blueprint.setId(100L);
    blueprint.setEventId(200L);
    blueprint.setEventWorkTypeId(300L);
    blueprint.setEventWorkTypeLevelId(400L);
    blueprint.setContentType("theory");
    blueprint.setVersionNo(1);
    blueprint.setTitle("理论细目表");

    var tree = CompetitionQuestionBlueprintTreeVO.from(blueprint, List.of(
        node(1L, 0L, 1, "A", "A", 1),
        node(2L, 1L, 2, "A", "A-A", 1),
        node(3L, 2L, 3, "A", "A-A-A", 2),
        node(4L, 2L, 3, "B", "A-A-B", 1),
        node(5L, 3L, 4, "002", "A-A-A-002", 1),
        node(6L, 4L, 4, "001", "A-A-B-001", 1),
        node(7L, 3L, 4, "10", "A-A-A-10", 2),
        node(8L, 3L, 4, "2", "A-A-A-2", 3)));

    var levelTwo = tree.tree().get(0).children().get(0);
    assertThat(levelTwo.children()).extracting(CompetitionQuestionBlueprintTreeVO.NodeVO::fullCode)
        .containsExactly("A-A-A", "A-A-B");
    assertThat(levelTwo.children().get(0).children())
        .extracting(CompetitionQuestionBlueprintTreeVO.NodeVO::fullCode)
        .containsExactly("A-A-A-2", "A-A-A-002", "A-A-A-10");
  }

  private CompetitionQuestionBlueprintNodeEntity node(
      Long id, Long parentId, int level, String code, String fullCode, int sortOrder) {
    CompetitionQuestionBlueprintNodeEntity node = new CompetitionQuestionBlueprintNodeEntity();
    node.setId(id);
    node.setParentId(parentId);
    node.setNodeLevel(level);
    node.setCode(code);
    node.setFullCode(fullCode);
    node.setName(fullCode);
    node.setSortOrder(sortOrder);
    return node;
  }
}
