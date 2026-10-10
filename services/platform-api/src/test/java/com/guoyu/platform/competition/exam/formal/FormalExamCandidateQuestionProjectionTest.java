package com.guoyu.platform.competition.exam.formal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.ProjectedOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormalExamCandidateQuestionProjectionTest {

  private final FormalExamCandidateQuestionProjection projection =
      new FormalExamCandidateQuestionProjection(new ObjectMapper());

  @Test
  void optionIdsAreStableAndNamespacedByQuestionSnapshot() {
    List<ProjectedOption> first = projection.projectOptions(9102L, """
        [{"key":"A","content":"甲"},{"key":"B","content":"乙"}]
        """);
    List<ProjectedOption> second = projection.projectOptions(9102L, """
        [{"key":"B","content":"乙"},{"key":"A","content":"甲"}]
        """);
    assertThat(first).extracting(ProjectedOption::optionId).containsExactly("9102:A", "9102:B");
    assertThat(first).extracting(ProjectedOption::label).containsExactly("A", "B");
    assertThat(second).extracting(ProjectedOption::optionId).containsExactly("9102:B", "9102:A");
    assertThat(projection.projectOptions(9103L, """
        [{"key":"A","content":"甲"}]
        """)).extracting(ProjectedOption::optionId).containsExactly("9103:A");
  }

  @Test
  void objectOptionsUseLettersAsSnapshotKeysNotDisplayPosition() {
    List<ProjectedOption> options = projection.projectOptions(1L, "{\"C\":\"丙\",\"A\":\"甲\"}");
    assertThat(options).extracting(ProjectedOption::optionId).containsExactly("1:C", "1:A");
  }

  @Test
  void rejectsOptionThatDoesNotBelongToQuestion() throws Exception {
    List<ProjectedOption> options = projection.projectOptions(9102L, "[{\"key\":\"A\",\"content\":\"甲\"}]");
    ObjectNode answer = new ObjectMapper().createObjectNode();
    answer.putArray("selectedOptionIds").add("9103:A");
    assertThatThrownBy(() -> projection.normalizeAnswer("single", answer, options))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选项不属于当前题目");
  }
}
