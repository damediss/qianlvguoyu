package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideGenInput;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideWorkflowResult;
import com.guoyu.platform.courseware.domain.StructureIssue;
import com.guoyu.platform.courseware.service.CoursewareAiService;
import com.guoyu.platform.courseware.service.SlideWorkflowService;
import java.util.List;
import org.junit.jupiter.api.Test;

/** U-11:自纠环(修复路径/1 轮上限/capacity 不重写/JSON 契约失败) */
class SlideWorkflowServiceTest {

  private final CoursewareAiService aiService = mock(CoursewareAiService.class);
  private final SlideWorkflowService service = new SlideWorkflowService(aiService, new ObjectMapper());

  private static final String GOOD_JSON = """
      {"blocks":[
        {"slot_id":"title","type":"text","text":"淬火温度决定硬度上限"},
        {"slot_id":"body","type":"bullets","items":[
          "淬火加热到 850℃ 以上保温使组织奥氏体化",
          "冷却速度决定马氏体转变是否充分",
          "回火消除淬火应力并调整韧性"]}],
       "speaker_notes":"重点讲温度与冷却速度的关系"}
      """;

  /** 缺必填槽位 body 的坏 JSON */
  private static final String BAD_JSON = """
      {"blocks":[{"slot_id":"title","type":"text","text":"只有标题"}],"speaker_notes":""}
      """;

  /** 仅超容量(6 条要点超上限 5,内容充实不触发 thin)但不缺槽位 */
  private static final String CAPACITY_ONLY_JSON = """
      {"blocks":[
        {"slot_id":"title","type":"text","text":"标题"},
        {"slot_id":"body","type":"bullets","items":[
          "要点一的具体内容充实可靠","要点二的具体内容充实可靠","要点三的具体内容充实可靠",
          "要点四的具体内容充实可靠","要点五的具体内容充实可靠","要点六的具体内容充实可靠"]}],
       "speaker_notes":""}
      """;

  private static SlideGenInput input() {
    return new SlideGenInput("课件A", "学员自学", 3, 10, "第一章", "淬火温度决定硬度上限",
        "讲清温度", List.of("要点一", "要点二", "要点三"), "content", "content_bullets",
        List.of(), null, List.of(), "medium", null, List.of());
  }

  @Test
  void u11_badThenGoodRepairsOnce() {
    when(aiService.callSlide(any(), any(), any(), any(), any(), any()))
        .thenReturn(BAD_JSON, GOOD_JSON);
    SlideWorkflowResult result = service.execute(input(), 10159L, 100L, 7L, 55L);
    assertEquals(1, result.repairRounds(), "坏→好应走 1 轮修复");
    assertEquals(2, result.blocks().size(), "修复后块齐全(title+body)");
    verify(aiService, times(2)).callSlide(any(), any(), any(), any(), any(), any());
  }

  @Test
  void u11_twoBadRoundsStopAtOneRepair() {
    when(aiService.callSlide(any(), any(), any(), any(), any(), any())).thenReturn(BAD_JSON);
    SlideWorkflowResult result = service.execute(input(), 10159L, 100L, 7L, 55L);
    assertEquals(1, result.repairRounds(), "最多修复 1 轮");
    verify(aiService, times(2)).callSlide(any(), any(), any(), any(), any(), any());
    assertTrue(result.issues().stream().anyMatch(StructureIssue::isError),
        "残留 structure error 落库透出");
  }

  @Test
  void u11_capacityOnlyNoRepair() {
    when(aiService.callSlide(any(), any(), any(), any(), any(), any()))
        .thenReturn(CAPACITY_ONLY_JSON);
    SlideWorkflowResult result = service.execute(input(), 10159L, 100L, 7L, 55L);
    assertEquals(0, result.repairRounds(), "仅 capacity 告警不触发重写");
    verify(aiService, times(1)).callSlide(any(), any(), any(), any(), any(), any());
  }

  @Test
  void u11_invalidJsonTwiceFails() {
    when(aiService.callSlide(any(), any(), any(), any(), any(), any())).thenReturn("不是 JSON");
    assertThrows(IllegalStateException.class,
        () -> service.execute(input(), 10159L, 100L, 7L, 55L));
    verify(aiService, times(2)).callSlide(any(), any(), any(), any(), any(), any());
  }
}
