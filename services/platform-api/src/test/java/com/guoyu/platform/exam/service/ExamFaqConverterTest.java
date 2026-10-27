package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FAQ 链路解析接入（§5.12：统一读取 + 表格扁平化，无裸管道符）。 */
class ExamFaqConverterTest {

  private ExamFaqConverter converter;

  @BeforeEach
  void setUp() {
    AiGenerationDraftMapper draftMapper = mock(AiGenerationDraftMapper.class);
    ExamKnowledgeStructureMapper knowledgeMapper = mock(ExamKnowledgeStructureMapper.class);
    QuestionResolveInputFactory inputFactory = new QuestionResolveInputFactory(
        new FollowTaskBankQuestionConverter(new ObjectMapper()), knowledgeMapper, new ObjectMapper());
    OfficialResolveResolver resolver = new OfficialResolveResolver(draftMapper, inputFactory, new ObjectMapper());
    converter = new ExamFaqConverter(new ObjectMapper(), resolver);
  }

  private ExamExamDetailEntity singleQuestion(String resolve, String answerJson) {
    ExamExamDetailEntity exam = new ExamExamDetailEntity();
    exam.setId(1);
    exam.setDeleted(0);
    exam.setType("single");
    exam.setContent("安全电压是多少\nA. 36V\nB. 220V");
    exam.setAnswer(answerJson);
    exam.setResolve(resolve);
    return exam;
  }

  @Test
  void tableResolveFlattenedWithoutPipes() {
    String resolve = "【正确】答案：A。见下表。\n| 电压 | 场景 |\n|---|---|\n| 36 | 干燥 |";
    String out = converter.buildAnswers(singleQuestion(resolve, "A"));

    assertTrue(out.contains("解析："));
    assertTrue(out.contains("电压36、场景干燥"));
    assertFalse(out.contains("|"));
    assertFalse(out.contains("---"));
  }

  @Test
  void answerJsonOfficialResolveEntersFaq() {
    // resolve 列为空、answer JSON 根字段携带官方解析 → 统一读取器命中并扁平进 FAQ
    String answerJson = "{\"resolve\":\"【正确】答案：A。规程规定安全电压为三十六伏。\",\"options\":[]}";
    ExamExamDetailEntity exam = singleQuestion(null, answerJson);
    exam.setContent("安全电压是多少伏");
    String out = converter.buildAnswers(exam);

    assertTrue(out.contains("解析："));
    assertTrue(out.contains("规程规定安全电压为三十六伏"));
  }

  @Test
  void noResolveKeepsOriginalBehavior() {
    ExamExamDetailEntity exam = singleQuestion(null, "A");
    String out = converter.buildAnswers(exam);
    assertFalse(out.contains("解析："));
    // answers 仍是数组 JSON，含答案正文
    assertTrue(out.startsWith("[") && out.endsWith("]"));
  }
}
