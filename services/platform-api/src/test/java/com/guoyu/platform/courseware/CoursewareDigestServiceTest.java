package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.mapper.CwDigestMapper;
import com.guoyu.platform.courseware.service.CoursewareAiService;
import com.guoyu.platform.courseware.service.CoursewareDigestService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** U-04~U-06:消化层结构校验/数字回查/失败降级 */
class CoursewareDigestServiceTest {

  private final CoursewareAiService aiService = mock(CoursewareAiService.class);
  private final CwDigestMapper digestMapper = mock(CwDigestMapper.class);
  private final ObjectMapper om = new ObjectMapper();
  private final CoursewareDigestService service =
      new CoursewareDigestService(aiService, digestMapper, om);

  private static final String VALID_DIGEST = """
      {"topics":[
        {"name":"热处理基础","points":["淬火提高硬度"],"confusions":["淬火≠回火"],
         "key_data":["淬火温度 850℃"],"source_refs":["S1:1"]},
        {"name":"工艺流程","points":["备料到出炉五步"],"confusions":[],
         "key_data":[],"source_refs":["S1:2","S1:9"]},
        {"name":"质量检验","points":["硬度抽检"],"confusions":[],
         "key_data":["抽检比例 10%"],"source_refs":["S1:3"]}
      ]}""";

  private static List<SourceSection> sections() {
    return List.of(
        new SourceSection("S1:1", "热处理", 1, "淬火温度 850℃,保温 30 分钟", "第 1 段"),
        new SourceSection("S1:2", "流程", 1, "备料、装炉、加热、保温、出炉", "第 2 段"),
        new SourceSection("S1:3", "检验", 1, "硬度抽检比例 10%", "第 3 段"));
  }

  @Test
  void u04_parseValidDigest() {
    DigestDraft draft = CoursewareDigestService.parseAndValidate(VALID_DIGEST,
        Set.of("S1:1", "S1:2", "S1:3"), "淬火温度 850℃ 抽检比例 10%", om);
    assertEquals(3, draft.topics().size());
    assertEquals("热处理基础", draft.topics().get(0).name());
    // S1:9 不在合法集合,被剔除
    assertEquals(List.of("S1:2"), draft.topics().get(1).sourceRefs());
  }

  @Test
  void u04_topicsTooFewRejected() {
    String bad = "{\"topics\":[{\"name\":\"仅一个\",\"points\":[],\"confusions\":[],"
        + "\"key_data\":[],\"source_refs\":[]}]}";
    assertThrows(IllegalArgumentException.class,
        () -> CoursewareDigestService.parseAndValidate(bad, Set.of("S1:1"), "", om));
  }

  @Test
  void u05_keyDataNumberCheck() {
    // 850℃ 在来源里,10% 不在 → 含 10% 的条目整条剔除
    DigestDraft draft = CoursewareDigestService.parseAndValidate(VALID_DIGEST,
        Set.of("S1:1", "S1:2", "S1:3"), "淬火温度 850℃", om);
    assertEquals(List.of("淬火温度 850℃"), draft.topics().get(0).keyData());
    assertEquals(List.of(), draft.topics().get(2).keyData(), "编造数字的条目应被剔除");
  }

  @Test
  void u06_llmFailureDegradesToNull() {
    when(aiService.callDigest(any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("LLM 不可用"));
    DigestDraft draft = service.tryDigest(10159L, 1L, 1L, "标题", "学员自学", sections());
    assertNull(draft, "LLM 异常应降级返回 null");
    verify(digestMapper, never()).insert(any(com.guoyu.platform.courseware.entity.CwDigestEntity.class));
  }

  @Test
  void u06_twoBadJsonRoundsDegradeToNull() {
    when(aiService.callDigest(any(), any(), any(), any(), any())).thenReturn("这不是 JSON");
    DigestDraft draft = service.tryDigest(10159L, 1L, 1L, "标题", "学员自学", sections());
    assertNull(draft);
    verify(aiService, org.mockito.Mockito.times(2)).callDigest(any(), any(), any(), any(), any());
    verify(digestMapper, never()).insert(any(com.guoyu.platform.courseware.entity.CwDigestEntity.class));
  }

  @Test
  void u06_successPersists() {
    when(aiService.callDigest(any(), any(), any(), any(), any())).thenReturn(VALID_DIGEST);
    DigestDraft draft = service.tryDigest(10159L, 1L, 1L, "标题", "学员自学", sections());
    assertEquals(3, draft.topics().size());
    verify(digestMapper).insert(any(com.guoyu.platform.courseware.entity.CwDigestEntity.class));
  }

  @Test
  void trimSectionsRespectsBudget() {
    List<SourceSection> longSections = List.of(
        new SourceSection("S1:1", "a", 1, "x".repeat(3000), "第 1 段"),
        new SourceSection("S1:2", "b", 1, "y".repeat(12000), "第 2 段"),
        new SourceSection("S1:3", "c", 1, "z", "第 3 段"));
    List<SourceSection> trimmed =
        CoursewareDigestService.trimSections(longSections, 2000, 12000);
    assertEquals(2000, trimmed.get(0).text().length(), "单节上限 2000 先截");
    assertEquals(2000, trimmed.get(1).text().length());
    assertEquals(3, trimmed.size(), "预算内小节全部保留");
    assertTrue(trimmed.get(0).ref().equals("S1:1"), "裁剪不丢 ref");
  }
}
