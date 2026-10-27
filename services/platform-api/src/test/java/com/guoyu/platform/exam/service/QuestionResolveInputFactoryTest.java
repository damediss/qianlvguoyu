package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 输入组装与 sourceHash（《试题解析Markdown表格化_一期实施方案》§5.5）。 */
class QuestionResolveInputFactoryTest {

  private final ExamKnowledgeStructureMapper knowledgeMapper =
      mock(ExamKnowledgeStructureMapper.class);
  private final QuestionResolveInputFactory factory = new QuestionResolveInputFactory(
      new FollowTaskBankQuestionConverter(new ObjectMapper()), knowledgeMapper, new ObjectMapper());

  @Test
  void singleChoiceBuildsInputWithStrippedHtml() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(1);
    detail.setType("single");
    detail.setContent("<p>下列哪项是安全电压？</p>\nA. 36V\nB. 220V");
    detail.setAnswer("A");
    detail.setKnowledgeId(9);
    ExamKnowledgeStructureEntity knowledge = new ExamKnowledgeStructureEntity();
    knowledge.setTitle("电气安全");
    when(knowledgeMapper.selectById(9)).thenReturn(knowledge);

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("single", input.normalizedType());
    assertEquals("单选题", input.typeLabel());
    assertEquals("下列哪项是安全电压？", input.stem());
    assertEquals("A. 36V\nB. 220V", input.optionsText());
    assertEquals("A", input.correctAnswerText());
    assertEquals("电气安全", input.knowledgeTitle());
    assertNotNull(input.sourceHash());
    assertEquals(64, input.sourceHash().length());
  }

  @Test
  void singleChoiceFromJsonOptions() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(2);
    detail.setType("单选题");
    detail.setContent("选择正确项\nA. 甲\nB. 乙");
    detail.setAnswer("[{\"content\":\"甲\",\"isCorrect\":true},{\"content\":\"乙\",\"isCorrect\":false}]");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("single", input.normalizedType());
    assertEquals("A", input.correctAnswerText());
  }

  @Test
  void multiChoiceAnswerSortedAscending() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(3);
    detail.setType("multi");
    detail.setContent("多选\nA. 甲\nB. 乙\nC. 丙");
    detail.setAnswer("C,A");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("A、C", input.correctAnswerText());
  }

  @Test
  void judgeAnswerNormalizesToChinese() {
    ExamExamDetailEntity trueDetail = judgeDetail("true");
    assertEquals("对", factory.build(trueDetail).correctAnswerText());
    ExamExamDetailEntity falseDetail = judgeDetail("错误");
    assertEquals("错", factory.build(falseDetail).correctAnswerText());
  }

  @Test
  void subjectiveAnswerFromJsonArray() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(5);
    detail.setType("essay");
    detail.setContent("<p>论述操作规程</p>");
    detail.setAnswer("[{\"content\":\"要点一\"},{\"text\":\"要点二\"},"
        + "{\"answer\":\"要点三\"},{\"value\":\"要点四\"},\"要点五\"]");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("论述题", input.typeLabel());
    assertEquals("要点一；要点二；要点三；要点四；要点五", input.correctAnswerText());
    assertEquals("", input.optionsText());
  }

  @Test
  void subjectiveAnswerFallsBackToPlainText() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(6);
    detail.setType("short");
    detail.setContent("简答要点");
    detail.setAnswer("直接文本答案");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("直接文本答案", input.correctAnswerText());
  }

  @Test
  void invalidInputWhenMissingOptionsOrAnswerOrStem() {
    ExamExamDetailEntity noOptions = new ExamExamDetailEntity();
    noOptions.setId(7);
    noOptions.setType("single");
    noOptions.setContent("只有题干没有选项");
    noOptions.setAnswer("A");
    assertNull(factory.build(noOptions));

    ExamExamDetailEntity answerNotInOptions = new ExamExamDetailEntity();
    answerNotInOptions.setId(8);
    answerNotInOptions.setType("single");
    answerNotInOptions.setContent("题干\nA. 甲\nB. 乙");
    answerNotInOptions.setAnswer("D");
    assertNull(factory.build(answerNotInOptions));

    ExamExamDetailEntity noStem = new ExamExamDetailEntity();
    noStem.setId(9);
    noStem.setType("single");
    noStem.setContent("<p></p>");
    noStem.setAnswer("A");
    assertNull(factory.build(noStem));

    ExamExamDetailEntity blankAnswer = new ExamExamDetailEntity();
    blankAnswer.setId(10);
    blankAnswer.setType("essay");
    blankAnswer.setContent("题干");
    blankAnswer.setAnswer("  ");
    assertNull(factory.build(blankAnswer));
  }

  @Test
  void sourceHashStableAndSensitiveToChanges() {
    ExamExamDetailEntity detail = baseSingle();
    String hash1 = factory.build(detail).sourceHash();
    String hash2 = factory.build(baseSingle()).sourceHash();
    assertEquals(hash1, hash2);

    ExamExamDetailEntity changedStem = baseSingle();
    changedStem.setContent("改变了题干\nA. 36V\nB. 220V");
    assertNotEquals(hash1, factory.build(changedStem).sourceHash());

    ExamExamDetailEntity changedAnswer = baseSingle();
    changedAnswer.setAnswer("B");
    assertNotEquals(hash1, factory.build(changedAnswer).sourceHash());

    ExamKnowledgeStructureEntity knowledge = new ExamKnowledgeStructureEntity();
    knowledge.setTitle("新知识点");
    when(knowledgeMapper.selectById(any())).thenReturn(knowledge);
    assertNotEquals(hash1, factory.build(baseSingle()).sourceHash());
  }

  @Test
  void optionTextStrippedOfHtml() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(20);
    detail.setType("single");
    detail.setContent("选择正确项\nA. 甲\nB. 乙");
    detail.setAnswer("[{\"content\":\"<b>甲</b>\",\"isCorrect\":true},{\"content\":\"乙\",\"isCorrect\":false}]");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("A. 甲\nB. 乙", input.optionsText());
  }

  @Test
  void subjectiveJsonAnswerStrippedOfHtml() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(21);
    detail.setType("essay");
    detail.setContent("论述停电作业流程");
    detail.setAnswer("[{\"content\":\"<p>先验电</p>\"},{\"content\":\"<p>再挂牌</p>\"}]");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("先验电；再挂牌", input.correctAnswerText());
  }

  @Test
  void blankOptionsTextIsInvalidEvenWhenOptionsPresent() {
    FollowTaskBankQuestionConverter mockConverter =
        mock(FollowTaskBankQuestionConverter.class);
    QuestionResolveInputFactory mockFactory = new QuestionResolveInputFactory(
        mockConverter, knowledgeMapper, new ObjectMapper());
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(22);
    detail.setType("single");
    detail.setContent("题干\nA. 甲");
    detail.setAnswer("A");
    FollowTaskQuestionReq snap = new FollowTaskQuestionReq();
    snap.setStem("题干");
    snap.setAnswer("A");
    // 选项列表非空，但条目无 key → optionsText 为空 → invalid_input
    snap.setOptions(List.of(Map.of("text", "无键选项")));
    when(mockConverter.convert(any(), eq(0))).thenReturn(snap);

    assertNull(mockFactory.build(detail));
  }

  @Test
  void unknownTypeParsesLegacyOptionsAndAnswer() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(23);
    detail.setType("case-study");
    detail.setContent("案例题干\nA. 方案一\nB. 方案二");
    detail.setAnswer("A");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("case-study", input.normalizedType());
    assertEquals("未标注题型", input.typeLabel());
    assertEquals("案例题干", input.stem());
    assertEquals("A. 方案一\nB. 方案二", input.optionsText());
    assertEquals("A", input.correctAnswerText());
  }

  @Test
  void unknownTypeWithMultipleAnswersJoinsAscending() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(24);
    detail.setType("case-study");
    detail.setContent("案例题干\nA. 方案一\nB. 方案二\nC. 方案三");
    detail.setAnswer("B,A");

    QuestionPromptInput input = factory.build(detail);

    assertNotNull(input);
    assertEquals("A、B", input.correctAnswerText());
  }

  @Test
  void unknownTypeWithoutParseableOptionsIsInvalid() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(25);
    detail.setType("case-study");
    detail.setContent("没有任何选项的案例题");
    detail.setAnswer("A");
    assertNull(factory.build(detail));
  }

  @Test
  void stripHtmlDecodesEntitiesAndLineBreaks() {
    assertEquals("第一行\n第二行 <关键> & \"引号\"",
        QuestionResolveInputFactory.stripHtml(
            "<p>第一行</p><p>第二行&nbsp;&lt;关键&gt; &amp; &quot;引号&quot;</p>"));
    assertNull(QuestionResolveInputFactory.stripHtml("<p> </p>"));
    assertNull(QuestionResolveInputFactory.stripHtml(null));
  }

  private ExamExamDetailEntity baseSingle() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(100);
    detail.setType("single");
    detail.setContent("安全电压是多少\nA. 36V\nB. 220V");
    detail.setAnswer("A");
    detail.setKnowledgeId(7);
    return detail;
  }

  private ExamExamDetailEntity judgeDetail(String answer) {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(4);
    detail.setType("judge");
    detail.setContent("判断题题干");
    detail.setAnswer(answer);
    return detail;
  }
}
