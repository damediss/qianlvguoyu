package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock;
import com.guoyu.platform.courseware.domain.CoursewareTheme;
import com.guoyu.platform.courseware.service.CoursewareExportService;
import com.guoyu.platform.courseware.service.PptxRenderer;
import com.guoyu.platform.courseware.service.PptxRenderer.RenderSlide;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

/** U-12/U-13:POI XSLF 渲染回读(页数/文本/ea 字体/页脚来源/备注页) */
class PptxRendererTest {

  private static List<RenderSlide> sampleDeck() {
    return List.of(
        new RenderSlide(CoursewareLayout.COVER, List.of(
            SlideBlock.text("title", "热处理工艺培训课件"),
            SlideBlock.text("subtitle", "学员自学版")),
            "封面备注", null, "cover"),
        new RenderSlide(CoursewareLayout.TOC, List.of(
            SlideBlock.text("title", "目录"),
            SlideBlock.bullets("body", List.of("第一章 基础", "第二章 工艺", "第三章 检验"))),
            null, null, "toc"),
        new RenderSlide(CoursewareLayout.CONTENT_BULLETS, List.of(
            SlideBlock.text("title", "淬火温度决定硬度上限"),
            SlideBlock.bullets("body", List.of(
                "淬火加热到 850℃ 以上保温使组织奥氏体化",
                "冷却速度决定马氏体转变是否充分",
                "回火消除淬火应力并调整韧性"))),
            "重点讲温度与冷却速度的关系", "来源:《热处理标准》淬火工艺", "content"),
        new RenderSlide(CoursewareLayout.CONTENT_TABLE, List.of(
            SlideBlock.text("title", "常用淬火介质对比"),
            SlideBlock.table("body", List.of("介质", "冷却速度", "适用"),
                List.of(List.of("水", "快", "碳钢"), List.of("油", "中", "合金钢")))),
            null, "来源:《热处理标准》淬火介质", "content"),
        new RenderSlide(CoursewareLayout.CONTENT_KPI, List.of(
            SlideBlock.text("title", "关键工艺参数"),
            SlideBlock.kpi("kpi_1", "850℃", "淬火温度", "碳钢常用上限"),
            SlideBlock.kpi("kpi_2", "30min", "保温时间", null)),
            null, "来源:《热处理标准》工艺参数", "content"),
        new RenderSlide(CoursewareLayout.SUMMARY, List.of(
            SlideBlock.text("title", "复习思考题"),
            SlideBlock.bullets("body", List.of(
                "淬火与回火的区别是什么?",
                "为什么冷却速度影响硬度?",
                "简述常见淬火介质的适用场景"))),
            null, "来源:《热处理标准》全章", "review"));
  }

  @Test
  void u12_renderAndVerify() throws Exception {
    byte[] pptx = PptxRenderer.render(sampleDeck());
    assertTrue(pptx.length > 1000, "pptx 应有实质内容");
    // 回读验证(页数/非空文本/ea 字体)不抛异常即通过
    PptxRenderer.verify(pptx, 6);

    try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
      assertEquals(6, ppt.getSlides().size());
      // 第 3 页标题文本存在
      XSLFSlide slide3 = ppt.getSlides().get(2);
      boolean titleFound = slide3.getShapes().stream()
          .filter(XSLFTextShape.class::isInstance)
          .map(s -> ((XSLFTextShape) s).getText())
          .anyMatch(t -> t != null && t.contains("淬火温度决定硬度上限"));
      assertTrue(titleFound, "第 3 页应含标题文本");
      // 备注页含学习提示
      XSLFNotes notes = slide3.getNotes();
      String notesText = notes.getPlaceholders() != null
          ? java.util.Arrays.stream(notes.getPlaceholders())
              .map(XSLFTextShape::getText).reduce("", String::concat)
          : "";
      assertTrue(notesText.contains("重点讲温度"), "备注页应含学习提示: " + notesText);
    }
  }

  @Test
  void u12_footerOnContentNotOnCover() throws Exception {
    byte[] pptx = PptxRenderer.render(sampleDeck());
    try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
      // cover 页不渲染页脚
      XSLFSlide cover = ppt.getSlides().get(0);
      boolean coverFooter = cover.getShapes().stream()
          .filter(XSLFTextShape.class::isInstance)
          .map(s -> ((XSLFTextShape) s).getText())
          .anyMatch(t -> t != null && t.startsWith("来源:"));
      assertFalse(coverFooter, "封面不渲染来源页脚");
      // content 页渲染页脚
      XSLFSlide content = ppt.getSlides().get(2);
      boolean contentFooter = content.getShapes().stream()
          .filter(XSLFTextShape.class::isInstance)
          .map(s -> ((XSLFTextShape) s).getText())
          .anyMatch(t -> t != null && t.contains("来源:《热处理标准》淬火工艺"));
      assertTrue(contentFooter, "内容页应含来源页脚");
    }
  }

  @Test
  void u12_verifyPageCountMismatchFails() {
    byte[] pptx = PptxRenderer.render(sampleDeck());
    assertThrows(IllegalStateException.class, () -> PptxRenderer.verify(pptx, 5));
  }

  @Test
  void u13_buildFooterFallbacks() {
    var sections = new java.util.LinkedHashMap<String, com.guoyu.platform.courseware.entity.CwSourceSectionEntity>();
    var s1 = new com.guoyu.platform.courseware.entity.CwSourceSectionEntity();
    s1.setRef("S1:1");
    s1.setHeading("淬火工艺");
    sections.put("S1:1", s1);
    var s2 = new com.guoyu.platform.courseware.entity.CwSourceSectionEntity();
    s2.setRef("S1:2");
    s2.setLocator("第 7 段"); // 无 heading 用 locator
    sections.put("S1:2", s2);

    var page = new com.guoyu.platform.courseware.entity.CwOutlinePageEntity();
    page.setSourceRefs("[\"S1:1\",\"S1:2\",\"S1:9\"]");
    String footer = CoursewareExportService.buildFooter("热处理标准", page, sections);
    assertEquals("来源:《热处理标准》淬火工艺、第 7 段", footer);

    // 映射全空 → 不渲染
    var page2 = new com.guoyu.platform.courseware.entity.CwOutlinePageEntity();
    page2.setSourceRefs("[\"S1:9\"]");
    assertEquals(null, CoursewareExportService.buildFooter("热处理标准", page2, sections));
  }

  @Test
  void themeConstantsConsistent() {
    assertEquals("微软雅黑", CoursewareTheme.FONT_FAMILY);
    assertEquals("1F5C99", CoursewareTheme.PRIMARY);
  }
}
