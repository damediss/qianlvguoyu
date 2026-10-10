package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.KnowledgePointPayload;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineChapterDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlinePageDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideGenInput;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideWorkflowResult;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.entity.CwKnowledgePointEntity;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.service.CoursewareAiService;
import com.guoyu.platform.courseware.service.CoursewareOutlineService;
import com.guoyu.platform.courseware.service.CoursewarePrompts;
import com.guoyu.platform.courseware.service.PptxRenderer;
import com.guoyu.platform.courseware.service.PptxRenderer.RenderSlide;
import com.guoyu.platform.courseware.service.SlideWorkflowService;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** U-17~U-20:知识点章替换 / 知识点 prompt / note 槽位渲染 / 生成分流 */
class KnowledgePointFlowTest {

  private static CwKnowledgePointEntity kp(String category, String level, String code,
                                           String title, String ref, int sort) {
    CwKnowledgePointEntity p = new CwKnowledgePointEntity();
    p.setId(1000L + sort);
    p.setCategory(category);
    p.setLevelName(level);
    p.setJobFunction("职业功能A");
    p.setJobContent("工作内容A");
    p.setCode(code);
    p.setTitle(title);
    p.setSkillText("能" + title);
    p.setKnowledgeText(title + "的详细要求");
    p.setSourceRef(ref);
    p.setSortNo(sort);
    return p;
  }

  private static CwOutlinePageEntity page(String title, int chapterNo, String chapterTitle,
                                          String role, String refs, int position) {
    CwOutlinePageEntity p = new CwOutlinePageEntity();
    p.setChapterNo(chapterNo);
    p.setChapterTitle(chapterTitle);
    p.setPageRole(role);
    p.setTitle(title);
    p.setSourceRefs(refs);
    p.setKeyPoints("[]");
    p.setLayoutId("content".equals(role) ? CoursewareLayout.CONTENT_BULLETS : role);
    p.setPosition(position);
    return p;
  }

  @Test
  void u17_replaceRequirementChapters() {
    // 大纲:cover/toc + 第1章(S1:1) + 第2章(S1:2,S1:3=基本要求范围) + 第3章(S1:4,S1:5=工作要求范围) + review
    List<CwOutlinePageEntity> pages = new ArrayList<>(List.of(
        page("封面", 0, null, "cover", "[]", 1),
        page("目录", 0, null, "toc", "[]", 2),
        page("第一章 概况", 1, "概况", "section", "[\"S1:1\"]", 3),
        page("概况页", 1, "概况", "content", "[\"S1:1\"]", 4),
        page("第四章 职业道德", 2, "职业道德", "section", "[\"S1:2\"]", 5),
        page("基础知识模块", 2, "职业道德", "content", "[\"S1:2\",\"S1:3\"]", 6),
        page("第五章 工作要求", 3, "工作要求", "section", "[\"S1:4\"]", 7),
        page("技能递进", 3, "工作要求", "content", "[\"S1:4\",\"S1:5\"]", 8),
        page("复习思考题", 0, null, "review", "[]", 9)));

    List<CwKnowledgePointEntity> points = List.of(
        kp("basic", null, "2.1.1", "职业道德基本知识", "S1:2", 0),
        kp("basic", null, "2.1.2", "职业守则", "S1:3", 1),
        kp("work", "五级/初级工", "1.1.1", "低压电器图形符号", "S1:4", 2),
        kp("work", "五级/初级工", "1.1.2", "低压电器结构原理", "S1:4", 3),
        kp("work", "四级/中级工", "1.1.1", "PLC 编程基础", "S1:5", 4));

    List<CwOutlinePageEntity> out = CoursewareOutlineService.replaceRequirementChapters(
        pages, points, 1L, 10159L, 7L, 50);

    // 第一章保留;第 2/3 章被替换为 基本要求 + 工作要求·五级 + 工作要求·四级;review 最后
    assertEquals("cover", out.get(0).getPageRole());
    assertEquals("toc", out.get(1).getPageRole());
    assertEquals("概况", out.get(2).getChapterTitle());
    assertEquals("review", out.get(out.size() - 1).getPageRole());
    // 知识点章顺序:概况 → 基本要求(1 section+2 点) → 五级(1+2) → 四级(1+1)
    List<String> chapterTitles = out.stream().map(CwOutlinePageEntity::getChapterTitle)
        .filter(java.util.Objects::nonNull).distinct().toList();
    assertEquals(List.of("概况", "基本要求", "工作要求·五级/初级工", "工作要求·四级/中级工"),
        chapterTitles);
    // 知识点页挂接与版式
    CwOutlinePageEntity kpPage = out.stream()
        .filter(p -> p.getKnowledgePointId() != null && "1.1.2".equals(
            findCode(points, p.getKnowledgePointId()))).findFirst().orElseThrow();
    assertEquals(CoursewareLayout.CONTENT_TABLE, kpPage.getLayoutId());
    assertTrue(kpPage.getTitle().startsWith("1.1.2 "));
    assertEquals("content", kpPage.getPageRole());
    // 章号按 chapterTitle 首见顺序:概况=1,基本要求=2,五级=3,四级=4
    assertEquals(2, out.stream().filter(p -> "基本要求".equals(p.getChapterTitle()))
        .findFirst().orElseThrow().getChapterNo());
    assertEquals(3, out.stream().filter(p -> "工作要求·五级/初级工".equals(p.getChapterTitle()))
        .findFirst().orElseThrow().getChapterNo());
    // position 连续
    for (int i = 0; i < out.size(); i++) {
      assertEquals(i + 1, out.get(i).getPosition());
    }
  }

  @Test
  void u17_fallbackInsertBeforeReview() {
    // 大纲没有任何落在要求范围的章(refs 不在 basic/work refs 内)→ 兜底插到 review 前
    List<CwOutlinePageEntity> pages = new ArrayList<>(List.of(
        page("封面", 0, null, "cover", "[]", 1),
        page("第一章 概况", 1, "概况", "section", "[\"S1:1\"]", 2),
        page("复习思考题", 0, null, "review", "[]", 3)));
    List<CwKnowledgePointEntity> points = List.of(
        kp("basic", null, "2.1.1", "职业道德基本知识", "S1:2", 0));
    List<CwOutlinePageEntity> out = CoursewareOutlineService.replaceRequirementChapters(
        pages, points, 1L, 10159L, 7L, 50);
    assertEquals("review", out.get(out.size() - 1).getPageRole());
    assertEquals("基本要求", out.get(2).getChapterTitle(), "兜底插在 review 之前");
  }

  @Test
  void u17_overlapSixtyPercentReplaces() {
    // 章 3 页中 2 页在范围内(≥60%)→ 也被替换(全含规则的放宽,冒烟实测缺口)
    List<CwOutlinePageEntity> pages = new ArrayList<>(List.of(
        page("封面", 0, null, "cover", "[]", 1),
        page("目录", 0, null, "toc", "[]", 2),
        page("第五章 工作要求", 1, "工作要求", "section", "[\"S1:9\"]", 3), // 范围外 ref
        page("技能一", 1, "工作要求", "content", "[\"S1:4\"]", 4),
        page("技能二", 1, "工作要求", "content", "[\"S1:5\"]", 5),
        page("复习思考题", 0, null, "review", "[]", 6)));
    List<CwKnowledgePointEntity> points = List.of(
        kp("work", "五级/初级工", "1.1.1", "低压电器图形符号", "S1:4", 0),
        kp("work", "五级/初级工", "1.1.2", "低压电器结构原理", "S1:5", 1));
    List<CwOutlinePageEntity> out = CoursewareOutlineService.replaceRequirementChapters(
        pages, points, 1L, 10159L, 7L, 50);
    assertEquals("工作要求·五级/初级工", out.get(2).getChapterTitle(),
        "重叠率 ≥60% 的章也应被替换");
    assertEquals("review", out.get(out.size() - 1).getPageRole());
  }

  private static String findCode(List<CwKnowledgePointEntity> points, Long id) {
    return points.stream().filter(p -> p.getId().equals(id)).findFirst()
        .map(CwKnowledgePointEntity::getCode).orElse("");
  }

  @Test
  void u18_knowledgePromptContract() {
    SlideGenInput input = new SlideGenInput("电工课件", "学员自学", 5, 200, "工作要求·五级/初级工",
        "1.1.2 常用低压电器的结构、工作原理及使用方法", "", List.of(), "content",
        CoursewareLayout.CONTENT_TABLE, List.of(
            new SourceSection("S1:4", "工作要求", 1, "相关知识要求原文", "第 4 段")),
        null, List.of("1.1.1 图形符号标准"), "medium",
        new KnowledgePointPayload("work", "五级/初级工", "电器安装和线路敷设", "低压电器选用",
            "1.1.2", "常用低压电器的结构、工作原理及使用方法", "能识别常用低压电器",
            "常用低压电器的结构、工作原理及使用方法"),
        List.of());
    String system = CoursewarePrompts.knowledgeSlideSystem();
    assertTrue(system.contains("≤8 行 × ≤5 列"), "骨架表容量写进 system prompt");
    assertTrue(system.contains("note"), "口诀槽位写进 system prompt");
    ObjectMapper om = new ObjectMapper();
    String user = CoursewarePrompts.knowledgeSlideUser(input, om);
    assertTrue(user.contains("1.1.2"), user);
    assertTrue(user.contains("what_you_can_do"), "技能要求挂接进 user prompt");
    assertTrue(user.contains("常用低压电器的结构"), user);
  }

  @Test
  void u19_noteSlotRenders() throws Exception {
    var slide = new RenderSlide(CoursewareLayout.CONTENT_TABLE, List.of(
        com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock.text("title",
            "常用低压电器一览"),
        com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock.table("body",
            List.of("类别", "电器", "结构", "工作原理", "使用方法"),
            List.of(
                List.of("主令", "按钮", "按钮帽、弹簧、触点", "按下常闭断常开合", "红停绿启"),
                List.of("保护", "熔断器", "熔体、熔管", "电流过大熔体断", "按额定值选"))),
        com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock.text("note",
            "主令发指令,开关通断路;保护防过载,控制自动做")),
        "备注", null, "content");
    byte[] pptx = PptxRenderer.render(List.of(slide));
    PptxRenderer.verify(pptx, 1);
    try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
      boolean noteFound = ppt.getSlides().get(0).getShapes().stream()
          .filter(XSLFTextShape.class::isInstance)
          .map(s -> ((XSLFTextShape) s).getText())
          .anyMatch(t -> t != null && t.contains("主令发指令"));
      assertTrue(noteFound, "note 口诀应渲染在页面上");
    }
  }

  @Test
  void u18_objectRowsTolerated() {
    // 模型把 rows 输出为对象数组(真实事故形态)→ 宽容解析为单元格,不再产生空白行
    String json = """
        {"blocks":[
          {"slot_id":"title","type":"text","text":"t"},
          {"slot_id":"body","type":"table","header":["要点","含义"],
           "rows":[{"要点":"熔断器","含义":"电流过大熔体断"},
                   {"要点":"断路器","含义":"过流自动跳闸"},
                   ["热继电器","过载保护"]]},
          {"slot_id":"note","type":"text","text":"口诀"}],
         "speaker_notes":""}
        """;
    var draft = com.guoyu.platform.courseware.domain.SlideAssembler.parseDraft(
        json, new ObjectMapper());
    var table = draft.rawBlocks().get(1);
    assertEquals(3, table.rows().size());
    assertEquals(List.of("熔断器", "电流过大熔体断"), table.rows().get(0));
    assertEquals(List.of("热继电器", "过载保护"), table.rows().get(2), "数组行不受影响");
  }

  @Test
  void u20_workflowBranchesKnowledgePrompt() {
    CoursewareAiService aiService = mock(CoursewareAiService.class);
    SlideWorkflowService service = new SlideWorkflowService(aiService, new ObjectMapper());
    when(aiService.callSlide(any(), any(), any(), any(), anyString(), anyString()))
        .thenReturn("""
            {"blocks":[
              {"slot_id":"title","type":"text","text":"常用低压电器一览"},
              {"slot_id":"body","type":"table","header":["类别","电器","结构"],
               "rows":[["主令","按钮","触点"]]},
              {"slot_id":"note","type":"text","text":"红停绿启"}],
             "speaker_notes":"重点记忆"}
            """);
    SlideGenInput input = new SlideGenInput("电工课件", "学员自学", 5, 200, "工作要求·五级",
        "1.1.2 低压电器", "", List.of(), "content", CoursewareLayout.CONTENT_TABLE, List.of(),
        null, List.of(), "medium",
        new KnowledgePointPayload("work", "五级", "职业功能", "工作内容", "1.1.2",
            "低压电器", "能识别", "结构原理使用方法"),
        List.of());
    SlideWorkflowResult result = service.execute(input, 10159L, 100L, 7L, 55L);
    assertEquals(3, result.blocks().size());
    ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
    verify(aiService).callSlide(any(), any(), any(), any(), systemCaptor.capture(), anyString());
    assertTrue(systemCaptor.getValue().contains("教研员"), "知识点页应用专用 system prompt");
    // 常规页仍用原 prompt(不带 knowledgePoint)
    SlideGenInput normal = new SlideGenInput("课件A", "学员自学", 3, 10, "第一章", "标题",
        "", List.of(), "content", "content_bullets", List.of(), null, List.of(), "medium",
        null, List.of());
    assertNull(normal.knowledgePoint());
    assertNotNull(input.knowledgePoint());
  }
}
