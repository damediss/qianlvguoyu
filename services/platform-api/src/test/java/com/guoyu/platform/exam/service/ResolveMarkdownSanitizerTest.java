package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import com.guoyu.platform.exam.service.ResolveMarkdownSanitizer.SanitizeResult;
import org.junit.jupiter.api.Test;

/** 解析生成质量门 S1~S14（《试题解析Markdown表格化_一期实施方案》§5.7/§9）。 */
class ResolveMarkdownSanitizerTest {

  private static QuestionPromptInput singleInput() {
    return new QuestionPromptInput(
        "single", "单选题", "安全电压是多少伏，规程规定为 36 伏", "A. 36\nB. 220", "A", "电气安全", "h");
  }

  private static QuestionPromptInput multiInput() {
    return new QuestionPromptInput(
        "multi", "多选题", "哪些属于防护措施", "A. 验电\nB. 挂牌\nC. 徒手操作", "A、B", "电气安全", "h");
  }

  private static QuestionPromptInput judgeInput() {
    return new QuestionPromptInput("judge", "判断题", "潮湿环境可用三十六伏", "", "对", "电气安全", "h");
  }

  private static QuestionPromptInput subjectiveInput() {
    return new QuestionPromptInput(
        "essay", "论述题", "论述停电作业流程", "", "验电；挂牌；作业；恢复", "作业安全", "h");
  }

  private static String validSingle() {
    return "【考点】电气安全\n"
        + "【正确】答案：A。规程规定安全电压为 36 伏，现场作业必须验电并挂警示牌，两人互保。"
        + "潮湿环境要更低，操作前必须确认停电范围并设专人监护。\n"
        + "【干扰】选 B 错在把市电当安全电压，220 伏远超安全限值。\n"
        + "【记法】三十六伏记心间。";
  }

  // ──────────────────────── S1 / S14 ────────────────────────

  @Test
  void s1StripsCodeFences() {
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(
        "```markdown\n" + validSingle() + "\n```", singleInput());
    assertNull(out.rejectedReason());
    assertFalse(out.text().contains("```"));
    assertTrue(out.text().contains("【考点】"));
  }

  @Test
  void s14RejectsThinkTagJsonShellAndPromptEcho() {
    assertEquals("think_tag",
        ResolveMarkdownSanitizer.sanitize("<think>推理</think>\n" + validSingle(), singleInput())
            .rejectedReason());
    assertEquals("json_shell",
        ResolveMarkdownSanitizer.sanitize("{\"resolve\":\"解析内容\"}", singleInput())
            .rejectedReason());
    assertEquals("prompt_echo",
        ResolveMarkdownSanitizer.sanitize("你是职业技能等级认定考试的辅导老师\n" + validSingle(),
            singleInput()).rejectedReason());
  }

  // ──────────────────────── S11 ────────────────────────

  @Test
  void s11RejectsForbiddenContent() {
    assertEquals("forbidden_promise",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\n保过班承诺", singleInput()).rejectedReason());
    assertEquals("forbidden_student_answer",
        ResolveMarkdownSanitizer.sanitize("你选了 A 所以错了\n" + validSingle(), singleInput())
            .rejectedReason());
    assertEquals("forbidden_html",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\n<p>额外</p>", singleInput()).rejectedReason());
    assertEquals("forbidden_link",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\n详见 https://example.com", singleInput())
            .rejectedReason());
  }

  @Test
  void s11RejectsInternalModelAndTableTerms() {
    assertEquals("forbidden_internal_term",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\n由 deepseek 模型生成", singleInput())
            .rejectedReason());
    assertEquals("forbidden_internal_term",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\nmax_tokens 参数已调整", singleInput())
            .rejectedReason());
    assertEquals("forbidden_internal_term",
        ResolveMarkdownSanitizer.sanitize(validSingle() + "\n数据来自 sys_dict 配置", singleInput())
            .rejectedReason());
  }

  @Test
  void s11InternalTermsDoNotHarmNormalBusinessText() {
    // 正常业务文本（含"安全问题"等相近字眼但不命中内部术语）不得误伤
    String text = "【考点】电气安全\n"
        + "【正确】答案：A。规程规定安全电压为 36 伏，现场作业必须验电挂牌，两人互保完成作业。\n"
        + "【干扰】选 B 错在把市电当安全电压，220 伏远超安全限值。\n"
        + "【记法】三十六伏记心间，安全问题不放松。";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
  }

  // ──────────────────────── S2~S5 / S10 ────────────────────────

  @Test
  void tableKeptWhenLegal() {
    String text = validSingle() + "\n| 电压 | 场景 |\n|---|---|\n| 36 | 干燥 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertFalse(out.tableDropped());
    assertTrue(out.text().contains("| 36 | 干燥 |"));
  }

  @Test
  void s5OrphanPipesKept() {
    String text = validSingle() + "\n这不是表格 | 只是管道符";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.text().contains("这不是表格 | 只是管道符"));
  }

  @Test
  void s3TooManyColumnsDropsWholeTable() {
    String text = validSingle() + "\n| a | b | c | d |\n|---|---|---|---|\n| 1 | 2 | 3 | 4 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.tableDropped());
    assertFalse(out.text().contains("| a | b | c | d |"));
  }

  @Test
  void s3TooManyDataRowsDropsTable() {
    StringBuilder text = new StringBuilder(validSingle()).append("\n| 电压 | 场景 |\n|---|---|");
    for (int i = 0; i < 5; i++) {
      text.append("\n| 36 | 环境").append(i).append(" |");
    }
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text.toString(), singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.tableDropped());
    assertFalse(out.text().contains("| 电压 | 场景 |"));
  }

  @Test
  void s3OverlongCellDropsTable() {
    String text = validSingle() + "\n| 电压 | 场景 |\n|---|---|\n| 36 | 这是一个超过十二个字的长单元格内容 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.tableDropped());
  }

  @Test
  void s4RaggedRowsDropsTable() {
    String text = validSingle() + "\n| 电压 | 场景 |\n|---|---|\n| 36 |\n| 36 | 干燥 | 多余 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.tableDropped());
  }

  @Test
  void s10FabricatedNumberDropsTableButKeepsText() {
    String text = validSingle() + "\n| 电压 | 场景 |\n|---|---|\n| 99 | 干燥 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.tableDropped());
    assertTrue(out.text().contains("【记法】"));
  }

  @Test
  void s10SingleDigitNotChecked() {
    String text = validSingle() + "\n| 序号 | 场景 |\n|---|---|\n| 5 | 干燥 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertFalse(out.tableDropped());
  }

  @Test
  void s10DecimalAndPercentWhitelistedFromSource() {
    QuestionPromptInput input = new QuestionPromptInput(
        "single", "单选题", "浓度 3.5% 为限值", "A. 3.5%\nB. 10%", "A", "化工", "h");
    String text = "【考点】化工\n【正确】答案：A。浓度限值 3.5% 必须控制，超标要停机处理并报告班长。\n"
        + "【干扰】选 B 错在把 10% 当限值。\n【记法】三点五记牢。\n"
        + "| 浓度 | 状态 |\n|---|---|\n| 3.5% | 合格 |";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, input);
    assertNull(out.rejectedReason());
    assertFalse(out.tableDropped());
  }

  // ──────────────────────── S6 ────────────────────────

  @Test
  void s6StripsListMarkersButKeepsNegativeNumbers() {
    String text = "# 补充说明\n- 断电后验电\n【考点】电气安全\n"
        + "【正确】答案：A。温度下限 -5 度仍要作业，必须两人互保并完成验电挂牌的全部动作。\n"
        + "【干扰】选 B 错。\n【记法】负五也干活。";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.text().contains("补充说明"));
    assertFalse(out.text().contains("# 补充说明"));
    assertTrue(out.text().contains("断电后验电"));
    assertTrue(out.text().contains("-5"));
  }

  // ──────────────────────── S7 ────────────────────────

  @Test
  void s7TruncatesByWholeLinesAndKeepsSections() {
    StringBuilder body = new StringBuilder(
        "【考点】电气安全\n【正确】答案：A。规程规定安全电压三十六伏，现场作业必须验电挂牌。\n"
            + "【干扰】选 B 错在市电。\n【记法】三十六伏。");
    // 超长尾段（完整行）：截断只从尾部按行吸收，不动小节标题行
    while (body.length() < 1200) {
      body.append("\n现场作业必须两人互保，停电验电挂牌后再操作。");
    }
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(body.toString(), singleInput());
    assertNull(out.rejectedReason());
    assertTrue(out.text().length() < body.length());
    assertTrue(out.text().contains("【记法】"));
    assertTrue(out.text().contains("【干扰】"));
    assertTrue(out.text().contains("【正确】"));
  }

  @Test
  void s7OversizeSingleLineRejects() {
    StringBuilder oneLine = new StringBuilder("答案：A。");
    while (oneLine.length() < 900) {
      oneLine.append("很长");
    }
    String text = "【考点】电气安全\n【正确】" + oneLine + "\n【干扰】选 B 错在市电。\n【记法】口诀。";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, singleInput());
    assertEquals("exceeds_length", out.rejectedReason());
  }

  // ──────────────────────── S8 / S9 ────────────────────────

  @Test
  void s8MissingSectionRejected() {
    String text = "【考点】电气安全\n【正确】答案：A。规程规定安全电压三十六伏，现场必须验电挂牌两人互保作业。\n【记法】口诀。";
    assertEquals("missing_section_干扰",
        ResolveMarkdownSanitizer.sanitize(text, singleInput()).rejectedReason());
  }

  @Test
  void s8WrongOrderRejected() {
    String text = "【正确】答案：A。规程规定安全电压三十六伏，现场必须验电挂牌两人互保作业。\n"
        + "【考点】电气安全\n【干扰】选 B 错。\n【记法】口诀。";
    assertNotNull(ResolveMarkdownSanitizer.sanitize(text, singleInput()).rejectedReason());
  }

  @Test
  void subjectivePassesWithoutGanrao() {
    String text = "【考点】作业安全\n【正确】答案要点：先验电，再挂牌，确认停电范围后作业，完工清点工具恢复送电。"
        + "每一步都要两人互保并记录，严禁单人操作。\n【记法】验电挂牌再动手。";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, subjectiveInput());
    assertNull(out.rejectedReason());
  }

  @Test
  void s9SingleAnswerMismatchRejected() {
    String text = validSingle().replace("答案：A。", "答案：B。");
    assertEquals("answer_prefix_mismatch",
        ResolveMarkdownSanitizer.sanitize(text, singleInput()).rejectedReason());
  }

  @Test
  void s9MultiAnswerMismatchRejected() {
    String text = "【考点】电气安全\n【正确】答案：A、C。A 和 C 都是防护措施，要逐项落实不能省略。\n"
        + "【干扰】B 是错误做法。\n【记法】验电挂牌。";
    assertEquals("answer_prefix_mismatch",
        ResolveMarkdownSanitizer.sanitize(text, multiInput()).rejectedReason());
  }

  @Test
  void s9MultiAnswerInJoinedFormPasses() {
    String text = "【考点】电气安全\n【正确】答案：A、B。A 验电和 B 挂牌都是防护措施，逐项落实才能开工。"
        + "缺一项都要补做，严禁口头代替。\n【干扰】C 是错误做法，严禁徒手。\n【记法】验电挂牌。";
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(text, multiInput());
    assertNull(out.rejectedReason());
  }

  @Test
  void s9JudgeMismatchRejected() {
    String text = "【考点】电气安全\n【正确】答案：错。潮湿环境可以用三十六伏，这个说法本身是对的。\n"
        + "【干扰】说错了，三十六伏是允许的。\n【记法】看环境。";
    assertEquals("answer_prefix_mismatch",
        ResolveMarkdownSanitizer.sanitize(text, judgeInput()).rejectedReason());
  }

  @Test
  void s9SubjectiveRequiresAnswerYaos() {
    String text = "【考点】作业安全\n【正确】先验电再挂牌，确认范围后作业，完工恢复送电，全程两人互保并记录。\n"
        + "【记法】验电挂牌。";
    assertEquals("answer_prefix_mismatch",
        ResolveMarkdownSanitizer.sanitize(text, subjectiveInput()).rejectedReason());
  }

  // ──────────────────────── S12 ────────────────────────

  @Test
  void s12ContentTooShortRejected() {
    String text = "【考点】电气\n【正确】答案：A。因为 A 对。\n【干扰】B 错。\n【记法】背。";
    assertEquals("content_too_short",
        ResolveMarkdownSanitizer.sanitize(text, singleInput()).rejectedReason());
  }

  @Test
  void s12SameAsStemRejected() {
    // 拼接文本（去小节括号）与题干完全一致：考点安全+正确答案：A。…+干扰…+记法…
    String combined = "考点安全正确答案：A。安全电压规程要求验电挂牌两人互保现场操作规范执行。干扰选B错记法口诀记牢";
    QuestionPromptInput input =
        new QuestionPromptInput("single", "单选题", combined, "A. 甲", "A", "", "h");
    String text = "【考点】安全\n【正确】答案：A。安全电压规程要求验电挂牌两人互保现场操作规范执行。\n"
        + "【干扰】选B错\n【记法】口诀记牢";
    assertEquals("same_as_stem", ResolveMarkdownSanitizer.sanitize(text, input).rejectedReason());
  }

  // ──────────────────────── S13 ────────────────────────

  @Test
  void s13OfficialOnlyKnowledgeMatching() {
    assertTrue(ResolveMarkdownSanitizer.isOfficialOnlyKnowledge("电工安全规程"));
    assertTrue(ResolveMarkdownSanitizer.isOfficialOnlyKnowledge("安全生产法律法规"));
    assertFalse(ResolveMarkdownSanitizer.isOfficialOnlyKnowledge("电气安全常识"));
    assertFalse(ResolveMarkdownSanitizer.isOfficialOnlyKnowledge(null));
    assertFalse(ResolveMarkdownSanitizer.isOfficialOnlyKnowledge(""));
  }

  // ──────────────────────── 完整通过路径 ────────────────────────

  @Test
  void validSinglePassesWithAllSections() {
    SanitizeResult out = ResolveMarkdownSanitizer.sanitize(validSingle(), singleInput());
    assertNull(out.rejectedReason());
    assertFalse(out.tableDropped());
    assertEquals(validSingle(), out.text());
  }

  @Test
  void blankOutputRejected() {
    assertEquals("empty_output",
        ResolveMarkdownSanitizer.sanitize("   ", singleInput()).rejectedReason());
    assertEquals("empty_output",
        ResolveMarkdownSanitizer.sanitize(null, singleInput()).rejectedReason());
  }
}
