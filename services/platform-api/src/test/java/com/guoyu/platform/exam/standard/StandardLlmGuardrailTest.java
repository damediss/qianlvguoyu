package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.service.StandardLlmPoint;
import com.guoyu.platform.exam.standard.service.StandardLlmSummarizeService;
import org.junit.jupiter.api.Test;

/**
 * B5/B6：LLM 输出确定性护栏（方案 §6.2/§6.3、§13.1）。
 */
class StandardLlmGuardrailTest {

  /** B5a：非法 JSON 抛异常（调用方重试，最终失败走原文兜底） */
  @Test
  void b5_invalidJsonThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("这不是JSON", 15, false));
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("", 15, false));
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("{\"wrong\":[]}", 15, false));
  }

  /** B5b：markdown 围栏兼容 */
  @Test
  void b5_markdownFenceStripped() {
    String content = "```json\n{\"points\":[{\"title\":\"工匠精神的基本内涵\",\"important\":\"X\"}]}\n```";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 15, false);
    assertEquals(1, result.getPoints().size());
    assertEquals("工匠精神的基本内涵", result.getPoints().get(0).title());
  }

  /** B5c：非法 important 默认 X + 警告 */
  @Test
  void b5_invalidImportantDefaultsX() {
    String content = "{\"points\":[{\"title\":\"知识点甲\",\"important\":\"核心\"},"
        + "{\"title\":\"知识点乙\",\"important\":\"y\"}]}";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 15, false);
    assertEquals(2, result.getPoints().size());
    assertEquals("X", result.getPoints().get(0).important(), "非法值默认 X");
    assertEquals("Y", result.getPoints().get(1).important(), "小写归一为大写");
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("非法 important")));
  }

  /** B5d：理论通道 >15 截断 + 警告 */
  @Test
  void b5_theoryTruncateAt15() {
    StringBuilder sb = new StringBuilder("{\"points\":[");
    for (int i = 0; i < 20; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append("{\"title\":\"知识点").append(i).append("\",\"important\":\"X\"}");
    }
    sb.append("]}");
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(sb.toString(), 15, false);
    assertEquals(15, result.getPoints().size());
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("截断")));
  }

  /** B5e：技能通道上限 = 原文条数 × 2 */
  @Test
  void b5_skillTruncateByFactor() {
    StringBuilder sb = new StringBuilder("{\"points\":[");
    for (int i = 0; i < 9; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append("{\"title\":\"操作").append(i).append("\",\"important\":\"X\"}");
    }
    sb.append("]}");
    // 原文 3 条 → 上限 6
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(sb.toString(), 6, true);
    assertEquals(6, result.getPoints().size());
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("截断")));
  }

  /** B6：技能考评点标题以"能"开头 → 警告（模型应去"能"字句式） */
  @Test
  void b6_skillTitleStartsWithNengWarns() {
    String content = "{\"points\":[{\"title\":\"能查出风门损坏\",\"important\":\"X\"},"
        + "{\"title\":\"检查风筒的损坏状况\",\"important\":\"Y\"}]}";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 10, true);
    assertEquals(2, result.getPoints().size(), "不丢条目");
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("能")),
        "应以警告提示'能'字开头");
    // 理论通道不报该警告
    StandardLlmPoint.ParseResult theory =
        StandardLlmSummarizeService.parsePointsResponse(content, 10, false);
    assertTrue(theory.getWarnings().isEmpty());
  }
}
