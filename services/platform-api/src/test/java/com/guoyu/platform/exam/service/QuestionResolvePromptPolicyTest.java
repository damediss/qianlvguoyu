package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import org.junit.jupiter.api.Test;

/** Prompt v2（《试题解析Markdown表格化_一期实施方案》§5.6/§9）。 */
class QuestionResolvePromptPolicyTest {

  @Test
  void budgetsByType() {
    assertEquals(240, QuestionResolvePromptPolicy.budgetFor("single"));
    assertEquals(300, QuestionResolvePromptPolicy.budgetFor("multi"));
    assertEquals(240, QuestionResolvePromptPolicy.budgetFor("judge"));
    assertEquals(400, QuestionResolvePromptPolicy.budgetFor("essay"));
    assertEquals(400, QuestionResolvePromptPolicy.budgetFor("short"));
    assertEquals(240, QuestionResolvePromptPolicy.budgetFor("unknown-x"));
    assertEquals(240, QuestionResolvePromptPolicy.budgetFor(null));
  }

  @Test
  void maxTokensByThinking() {
    assertEquals(800, QuestionResolvePromptPolicy.maxTokensFor("disabled"));
    assertEquals(800, QuestionResolvePromptPolicy.maxTokensFor(" Disabled "));
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor("enabled"));
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor(null));
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor(""));
  }

  @Test
  void maxTokensConfiguredTakesPrecedenceWithClamp() {
    assertEquals(8192, QuestionResolvePromptPolicy.maxTokensFor("disabled", 8192));
    assertEquals(8192, QuestionResolvePromptPolicy.maxTokensFor("enabled", 8192));
    // 未配置保持原行为
    assertEquals(800, QuestionResolvePromptPolicy.maxTokensFor("disabled", null));
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor("enabled", null));
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor(null, null));
    // clamp 到 [800, 8192]
    assertEquals(800, QuestionResolvePromptPolicy.maxTokensFor("disabled", 100));
    assertEquals(8192, QuestionResolvePromptPolicy.maxTokensFor("disabled", 20000));
    // 非正数视为未配置
    assertEquals(2400, QuestionResolvePromptPolicy.maxTokensFor("enabled", 0));
    assertEquals(800, QuestionResolvePromptPolicy.maxTokensFor("disabled", -5));
  }

  @Test
  void systemPromptCarriesBudgetAndCoreRules() {
    String prompt = QuestionResolvePromptPolicy.systemPrompt(240);
    assertTrue(prompt.contains("正文不超过 240 字"));
    assertTrue(prompt.contains("【考点】"));
    assertTrue(prompt.contains("【正确】"));
    assertTrue(prompt.contains("【干扰】"));
    assertTrue(prompt.contains("【记法】"));
    assertTrue(prompt.contains("|---|---|"));
    assertTrue(prompt.contains("列数不超过 3，数据行不超过 4，每格不超过 12 字"));
    assertTrue(prompt.contains("禁止推算新数据"));
    assertTrue(prompt.contains("保过/必过/稳过"));
    // v2.1：无依据扩展 / 低价值复述硬约束
    assertTrue(prompt.contains("禁止只写“符合题意”"));
    assertTrue(prompt.contains("禁止引入题干未提供的"));
    assertTrue(prompt.contains("不得推断它属于其他等级、其他标准或其他参数"));
  }

  @Test
  void singleChoiceUserPromptStructure() {
    QuestionPromptInput input = new QuestionPromptInput(
        "single", "单选题", "安全电压是多少", "A. 36V\nB. 220V", "A", "电气安全", "h");
    String user = QuestionResolvePromptPolicy.userPrompt(input);

    assertTrue(user.contains("题型：单选题"));
    assertTrue(user.contains("知识点：电气安全"));
    assertTrue(user.contains("题干：\n安全电压是多少"));
    assertTrue(user.contains("选项：\nA. 36V\nB. 220V"));
    assertTrue(user.contains("正确答案：A"));
    assertTrue(user.contains("【正确】必须以“答案：A。”开头，"
        + "解释该答案与题干条件的对应关系，禁止只写“符合题意”。"));
    // 不再有"（A 替换为真实答案）"类括号说明，避免答案非 A 时误导模型
    assertFalse(user.contains("替换为真实答案"));
    assertFalse(user.contains("替换为真实组合"));
    assertTrue(user.endsWith("请按四行结构写出解析："));
    // 不泄露学员作答/内部口径
    assertFalse(user.contains("你选了"));
    assertFalse(user.contains("questionId"));
  }

  @Test
  void multiChoiceBranchUsesJoinedAnswer() {
    QuestionPromptInput input = new QuestionPromptInput(
        "multi", "多选题", "题干", "A. 甲\nB. 乙\nC. 丙", "A、C", "考点", "h");
    String branch = QuestionResolvePromptPolicy.branchLine(input);
    assertTrue(branch.contains("答案：A、C。”开头"));
    assertTrue(branch.contains("最常漏选/多选项"));
  }

  @Test
  void judgeBranchRequiresDuicuo() {
    QuestionPromptInput input = new QuestionPromptInput(
        "judge", "判断题", "题干", "", "对", "", "h");
    String branch = QuestionResolvePromptPolicy.branchLine(input);
    assertTrue(branch.contains("“答案：对。”或“答案：错。”开头"));
  }

  @Test
  void subjectiveBranchOmitsGanraoAndSkipsOptionsBlock() {
    QuestionPromptInput input = new QuestionPromptInput(
        "essay", "论述题", "论述题题干", "", "要点一；要点二", "知识点", "h");
    String branch = QuestionResolvePromptPolicy.branchLine(input);
    assertTrue(branch.contains("答案要点：”开头"));
    assertTrue(branch.contains("省略【干扰】行"));

    String user = QuestionResolvePromptPolicy.userPrompt(input);
    assertFalse(user.contains("选项："));
    assertTrue(user.contains("正确答案：要点一；要点二"));
  }

  @Test
  void unknownTypeBranchEchoesOriginalAnswer() {
    QuestionPromptInput input = new QuestionPromptInput(
        "case-study", "未标注题型", "题干", "", "原文答案", "", "h");
    String branch = QuestionResolvePromptPolicy.branchLine(input);
    assertTrue(branch.contains("答案：原文答案。”开头"));
    assertTrue(branch.contains("通用四行"));
  }

  @Test
  void blankKnowledgeTitleFallsBackToWeibiaozhu() {
    QuestionPromptInput blank = new QuestionPromptInput(
        "single", "单选题", "题干", "A. 甲", "A", "  ", "h");
    QuestionPromptInput nil = new QuestionPromptInput(
        "single", "单选题", "题干", "A. 甲", "A", null, "h");
    assertTrue(QuestionResolvePromptPolicy.userPrompt(blank).contains("知识点：未标注"));
    assertTrue(QuestionResolvePromptPolicy.userPrompt(nil).contains("知识点：未标注"));
  }
}
