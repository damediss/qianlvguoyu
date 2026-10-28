package com.guoyu.platform.exam.answercheck.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * 严格解析与确定性预检（方案 §8 / §16.1-C）。
 *
 * <p>判断题不得兜底猜答案，结构异常必须在调用模型前判定。
 */
class ObjectiveQuestionParserTest {

  private final ObjectiveQuestionParser parser = new ObjectiveQuestionParser(new ObjectMapper());

  private ObjectiveQuestionSource source(String type, String content, String answer) {
    return new ObjectiveQuestionSource(101, type, content, answer, null, null, "铝电解工/安全知识");
  }

  private ParseOutcome parse(String type, String content, String answer) {
    return parser.parse(source(type, content, answer), 12000, 2000);
  }

  private static final String SINGLE = """
      [{"content":"红色","isCorrect":false},{"content":"蓝色","isCorrect":true},
       {"content":"黑色","isCorrect":false},{"content":"白色","isCorrect":false}]
      """;

  private static final String JUDGE = """
      [{"content":"正确","isCorrect":true},{"content":"错误","isCorrect":false}]
      """;

  @Test
  void singleQuestionParsesStableOptionIdsAndLabels() {
    ParseOutcome outcome = parse("single", "<p>海洋是什么颜色的？</p>", SINGLE);
    assertTrue(outcome.valid());
    NormalizedObjectiveQuestion question = outcome.question();
    assertEquals("海洋是什么颜色的？", question.stemText());
    assertEquals(4, question.options().size());
    assertEquals("opt_0", question.options().get(0).optionId());
    assertEquals("A", question.options().get(0).label());
    assertEquals("D", question.options().get(3).label());
    assertTrue(question.options().get(1).originalCorrect());
    assertFalse(question.options().get(0).originalCorrect());
    assertEquals(0, question.options().get(0).originalIndex());
    assertEquals(1, question.options().get(1).originalIndex());
  }

  @Test
  void multiRequiresAtLeastTwoCorrectOptions() {
    String valid = """
        [{"content":"A选项","isCorrect":true},{"content":"B选项","isCorrect":"yes"},
         {"content":"C选项","isCorrect":1},{"content":"D选项","isCorrect":false}]
        """;
    assertTrue(parse("multi", "多选题干", valid).valid());

    String oneCorrect = """
        [{"content":"A选项","isCorrect":true},{"content":"B选项","isCorrect":false}]
        """;
    assertEquals("MULTI_CORRECT_COUNT_INVALID",
        parse("multi", "多选题干", oneCorrect).invalidReason());
  }

  @Test
  void singleWithMultipleCorrectOptionsIsInvalid() {
    String answer = """
        [{"content":"A","isCorrect":true},{"content":"B","isCorrect":true}]
        """;
    assertEquals("SINGLE_CORRECT_COUNT_INVALID",
        parse("single", "单选", answer).invalidReason());
  }

  @Test
  void judgeNormalizesPositiveAndNegativeVariants() {
    String[] answers = {
        "[{\"content\":\"对\",\"isCorrect\":true},{\"content\":\"错\",\"isCorrect\":false}]",
        "[{\"content\":\"正确\",\"isCorrect\":false},{\"content\":\"错误\",\"isCorrect\":true}]",
        "[{\"content\":\"T\",\"isCorrect\":true},{\"content\":\"F\",\"isCorrect\":false}]",
        "[{\"content\":\"√\",\"isCorrect\":true},{\"content\":\"×\",\"isCorrect\":false}]",
        "[{\"content\":\"TRUE\",\"isCorrect\":false},{\"content\":\"FALSE\",\"isCorrect\":true}]"};
    for (String answer : answers) {
      ParseOutcome outcome = parse("judge", "判断题干", answer);
      assertTrue(outcome.valid(), "应可归一: " + answer);
      assertEquals(2, outcome.question().options().size());
      assertTrue(outcome.question().options().stream()
          .anyMatch(o -> "T".equals(o.label()) || "F".equals(o.label())));
    }
  }

  @Test
  void judgeRemovesPlaceholderAfterBothTruthValues() {
    String answer = """
        [{"content":"对","isCorrect":true},{"content":"错","isCorrect":false},
         {"content":"C","isCorrect":false},{"content":"D","isCorrect":false}]
        """;
    ParseOutcome outcome = parse("judge", "判断题干", answer);
    assertTrue(outcome.valid());
    assertEquals(2, outcome.question().options().size());
  }

  @Test
  void judgeWithUnrecognizedTextIsNotGuessed() {
    String answer = """
        [{"content":"答案1","isCorrect":true},{"content":"答案2","isCorrect":false}]
        """;
    assertEquals("JUDGE_OPTIONS_INVALID", parse("judge", "判断题干", answer).invalidReason());
  }

  @Test
  void structuralDefectsAreRejectedBeforeModelCall() {
    assertEquals("TYPE_UNSUPPORTED", parse("fill", "题干", SINGLE).invalidReason());
    assertEquals("STEM_BLANK", parse("single", "<p> </p>", SINGLE).invalidReason());
    assertEquals("ANSWER_JSON_INVALID", parse("single", "题干", "不是 JSON").invalidReason());
    assertEquals("ANSWER_JSON_INVALID", parse("single", "题干", null).invalidReason());
    assertEquals("OPTIONS_EMPTY", parse("single", "题干", "[]").invalidReason());
    assertEquals("OPTION_COUNT_INVALID", parse("single", "题干",
        "[{\"content\":\"唯一\",\"isCorrect\":true}]").invalidReason());
    assertEquals("OPTION_TEXT_BLANK", parse("single", "题干",
        "[{\"content\":\"A\",\"isCorrect\":true},{\"content\":\" \",\"isCorrect\":false}]")
        .invalidReason());
    assertEquals("OPTION_TEXT_DUPLICATE", parse("single", "题干",
        "[{\"content\":\"A\",\"isCorrect\":true},{\"content\":\"a\",\"isCorrect\":false}]")
        .invalidReason());
    assertEquals("CORRECT_OPTION_MISSING", parse("single", "题干",
        "[{\"content\":\"A\",\"isCorrect\":false},{\"content\":\"B\",\"isCorrect\":false}]")
        .invalidReason());
  }

  @Test
  void optionCountBoundariesAreEnforced() {
    StringBuilder seventeen = new StringBuilder("[");
    for (int i = 0; i < 17; i++) {
      if (i > 0) {
        seventeen.append(',');
      }
      seventeen.append("{\"content\":\"选项").append(i).append("\",\"isCorrect\":")
          .append(i == 0).append('}');
    }
    seventeen.append(']');
    assertEquals("OPTION_COUNT_INVALID",
        parse("single", "题干", seventeen.toString()).invalidReason());
  }

  @Test
  void mediaIsRejectedNotStripped() {
    assertEquals("MEDIA_UNSUPPORTED", parse("single",
        "<p>看图<img src=\"a.png\"/></p>", SINGLE).invalidReason());
    assertEquals("MEDIA_UNSUPPORTED", parse("single",
        "<p><audio src=\"a.mp3\"></audio>题干</p>", SINGLE).invalidReason());
    assertEquals("MEDIA_UNSUPPORTED", new ObjectiveQuestionParserTestHelper()
        .withAudio(source("single", "题干", SINGLE)).invalidReason());
  }

  @Test
  void longTextIsNotSilentlyTruncated() {
    String longStem = "题".repeat(12001);
    assertEquals("TEXT_TOO_LONG", parse("single", longStem, SINGLE).invalidReason());
  }

  @Test
  void htmlIsConvertedToPlainTextAndNbspNormalized() {
    ParseOutcome outcome = parse("single",
        "<p>海洋&nbsp;是什么&nbsp;&nbsp;颜色的？<br/>请选择</p>", SINGLE);
    assertEquals("海洋 是什么 颜色的？ 请选择", outcome.question().stemText());
  }

  @Test
  void sourceHashIsSensitiveToEverySegmentAndDistinguishesNullFromEmpty() {
    String base = parser.sourceHash("single", "题干", SINGLE);
    assertNotEquals(base, parser.sourceHash("multi", "题干", SINGLE));
    assertNotEquals(base, parser.sourceHash("single", "题干2", SINGLE));
    assertNotEquals(base, parser.sourceHash("single", "题干", SINGLE + " "));
    assertNotEquals(parser.sourceHash("single", null, SINGLE),
        parser.sourceHash("single", "", SINGLE));
    assertEquals(base, parser.sourceHash("single", "题干", SINGLE));
    assertEquals(64, base.length());
  }

  @Test
  void sqlNullAnswerFallsToInvalidButHashStillComputable() {
    ParseOutcome outcome = parse("single", "题干", null);
    assertEquals("ANSWER_JSON_INVALID", outcome.invalidReason());
    assertEquals(64, parser.sourceHash("single", "题干", null).length());
  }

  /** 小工具：把 audio 非空的 source 传入解析。 */
  private static final class ObjectiveQuestionParserTestHelper {
    private final ObjectiveQuestionParser parser = new ObjectiveQuestionParser(new ObjectMapper());

    ParseOutcome withAudio(ObjectiveQuestionSource base) {
      ObjectiveQuestionSource withAudio = new ObjectiveQuestionSource(base.detailId(), base.type(),
          base.contentHtml(), base.answerJson(), "a.mp3", null, base.contextPath());
      return parser.parse(withAudio, 12000, 2000);
    }
  }
}
