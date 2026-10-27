package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.service.OfficialResolveResolver.OfficialResolve;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import org.junit.jupiter.api.Test;

/** 官方解析统一读取器（《试题解析Markdown表格化_一期实施方案》§5.1/§9）。 */
class OfficialResolveResolverTest {

  private final AiGenerationDraftMapper draftMapper = mock(AiGenerationDraftMapper.class);
  private final ExamKnowledgeStructureMapper knowledgeMapper =
      mock(ExamKnowledgeStructureMapper.class);
  private final QuestionResolveInputFactory inputFactory = new QuestionResolveInputFactory(
      new FollowTaskBankQuestionConverter(new ObjectMapper()), knowledgeMapper, new ObjectMapper());
  private final OfficialResolveResolver resolver =
      new OfficialResolveResolver(draftMapper, inputFactory, new ObjectMapper());

  @Test
  void nullAndDeletedDetailReturnEmpty() {
    assertEquals("empty", resolver.resolve(null).source());
    ExamExamDetailEntity deleted = singleDetail();
    deleted.setDeleted(1);
    assertEquals("empty", resolver.resolve(deleted).source());
  }

  @Test
  void columnTextWinsAsDbSource() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setResolve("  人工编写的解析  ");
    when(draftMapper.selectOne(any())).thenReturn(null);

    OfficialResolve out = resolver.resolve(detail);

    assertEquals("db", out.source());
    assertEquals("人工编写的解析", out.text());
  }

  @Test
  void answerJsonFiveKeysInOrder() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setResolve(null);
    when(draftMapper.selectOne(any())).thenReturn(null);

    detail.setAnswer("{\"jiexi\":\"甲\",\"resolve\":\"官方解析文本\"}");
    assertEquals("官方解析文本", resolver.resolve(detail).text());
    detail.setAnswer("{\"explain\":\"解释文本\"}");
    OfficialResolve out = resolver.resolve(detail);
    assertEquals("official", out.source());
    assertEquals("解释文本", out.text());
    detail.setAnswer("{\"解析\":\"中文键\"}");
    assertEquals("中文键", resolver.resolve(detail).text());
    // 非对象根节点（选项数组形态）不当作官方解析
    detail.setAnswer("[{\"content\":\"甲\"}]");
    assertEquals("empty", resolver.resolve(detail).source());
    detail.setAnswer("not-json{");
    assertEquals("empty", resolver.resolve(detail).source());
  }

  @Test
  void nothingAvailableReturnsEmpty() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setResolve(null);
    detail.setAnswer("A");
    when(draftMapper.selectOne(any())).thenReturn(null);
    OfficialResolve out = resolver.resolve(detail);
    assertEquals("empty", out.source());
    assertNull(out.text());
    assertTrue(out.isEmpty());
  }

  @Test
  void staleAiColumnYieldsToAnswerJson() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setAnswer("A");
    detail.setResolve(null);
    // 先按当前题面算出 hash，构造一条同 hash 的 generated draft
    QuestionPromptInput input = inputFactory.build(detail);
    AiGenerationDraftEntity draft = generatedDraft(detail.getId(), "AI 生成的解析", input.sourceHash());
    when(draftMapper.selectOne(any())).thenReturn(draft);

    // 列文本 = draft 文本，但题目答案已改 → sourceHash 变化 → 判旧 → 让位 answer JSON
    detail.setAnswer("B");
    detail.setResolve("AI 生成的解析");
    detail.setAnswer(
        "{\"resolve\":\"题目已变更后的官方解析\",\"answer\":\"B\"}");
    OfficialResolve out = resolver.resolve(detail);
    assertEquals("official", out.source());
    assertEquals("题目已变更后的官方解析", out.text());
  }

  @Test
  void humanRewrittenColumnIsNeverStale() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setResolve("人工改写过的解析");
    QuestionPromptInput input = inputFactory.build(detail);
    when(draftMapper.selectOne(any()))
        .thenReturn(generatedDraft(detail.getId(), "AI 原始文本", input.sourceHash()));

    OfficialResolve out = resolver.resolve(detail);

    assertEquals("db", out.source());
    assertEquals("人工改写过的解析", out.text());
  }

  @Test
  void unchangedHashReturnsDraftSource() {
    ExamExamDetailEntity detail = singleDetail();
    QuestionPromptInput input = inputFactory.build(detail);
    detail.setResolve("AI 生成的解析");
    when(draftMapper.selectOne(any()))
        .thenReturn(generatedDraft(detail.getId(), "AI 生成的解析", input.sourceHash()));

    OfficialResolve out = resolver.resolve(detail);

    assertEquals("draft", out.source());
    assertEquals("AI 生成的解析", out.text());
  }

  @Test
  void staleColumnWithoutOfficialFallsToEmpty() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setAnswer("A");
    QuestionPromptInput input = inputFactory.build(detail);
    detail.setResolve("AI 生成的解析");
    when(draftMapper.selectOne(any()))
        .thenReturn(generatedDraft(detail.getId(), "AI 生成的解析", input.sourceHash()));

    detail.setContent("题干已经彻底改变\nA. 甲\nB. 乙");
    detail.setResolve("AI 生成的解析");

    OfficialResolve out = resolver.resolve(detail);
    assertEquals("empty", out.source());
  }

  private ExamExamDetailEntity singleDetail() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(11);
    detail.setDeleted(0);
    detail.setType("single");
    detail.setContent("安全电压是多少\nA. 36V\nB. 220V");
    detail.setAnswer("A");
    return detail;
  }

  private AiGenerationDraftEntity generatedDraft(Integer questionId, String resultText, String hash) {
    AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
    draft.setId(99L);
    draft.setSourceId(questionId.longValue());
    draft.setResultText(resultText);
    draft.setPrompt("question_resolve_v2#" + hash);
    draft.setStatus("generated");
    draft.setDeleted(0);
    return draft;
  }
}
