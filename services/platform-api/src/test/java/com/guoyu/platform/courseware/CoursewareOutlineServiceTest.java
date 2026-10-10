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
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineChapterDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlineDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.OutlinePageDraft;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSlideMapper;
import com.guoyu.platform.courseware.service.CoursewareAiService;
import com.guoyu.platform.courseware.service.CoursewareOutlineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** U-07/U-08/U-15:大纲钳制与三页组装、确认快照固化 */
class CoursewareOutlineServiceTest {

  private final CoursewareAiService aiService = mock(CoursewareAiService.class);
  private final CwOutlinePageMapper outlineMapper = mock(CwOutlinePageMapper.class);
  private final CwProjectMapper projectMapper = mock(CwProjectMapper.class);
  private final CwSlideMapper slideMapper = mock(CwSlideMapper.class);
  private final ObjectMapper om = new ObjectMapper();
  private final CoursewareOutlineService service =
      new CoursewareOutlineService(aiService, outlineMapper, projectMapper, slideMapper, om, 50);

  private static OutlineChapterDraft chapter(String title, int contentPages) {
    List<OutlinePageDraft> pages = new ArrayList<>();
    for (int i = 0; i < contentPages; i++) {
      pages.add(new OutlinePageDraft(title + "·结论" + i, "目标", List.of("要点一", "要点二", "要点三"),
          "content", null, List.of("S1:1")));
    }
    return new OutlineChapterDraft(title, title + "核心结论", pages);
  }

  @Test
  void u08_fixedThreePagesAssembled() {
    OutlineDraft draft = new OutlineDraft(
        List.of(chapter("第一章", 2), chapter("第二章", 2), chapter("第三章", 1)),
        List.of("问题一", "问题二", "问题三"));
    List<CwOutlinePageEntity> pages =
        CoursewareOutlineService.assemblePages(draft, "课件A", 1L, 10159L, 7L, 50);

    assertEquals(11, pages.size(), "cover+toc + 3章×(1 section+内容页) 共 8 章页 + review");
    assertEquals(CwOutlinePageEntity.ROLE_COVER, pages.get(0).getPageRole());
    assertEquals("课件A", pages.get(0).getTitle());
    assertEquals(CwOutlinePageEntity.ROLE_TOC, pages.get(1).getPageRole());
    assertEquals(CwOutlinePageEntity.ROLE_REVIEW, pages.get(pages.size() - 1).getPageRole());
    assertEquals(CoursewareLayout.SUMMARY, pages.get(pages.size() - 1).getLayoutId());
    // 每章首页强制 section 且 title=章标题
    assertEquals(CwOutlinePageEntity.ROLE_SECTION, pages.get(2).getPageRole());
    assertEquals("第一章", pages.get(2).getTitle());
    assertEquals(Integer.valueOf(1), pages.get(2).getChapterNo());
    // position 连续
    for (int i = 0; i < pages.size(); i++) {
      assertEquals(i + 1, pages.get(i).getPosition());
    }
  }

  @Test
  void u08_reviewQuestionsFallback() {
    OutlineDraft draft = new OutlineDraft(
        List.of(chapter("第一章", 1), chapter("第二章", 1), chapter("第三章", 1)), List.of());
    List<CwOutlinePageEntity> pages =
        CoursewareOutlineService.assemblePages(draft, "课件A", 1L, 10159L, 7L, 50);
    CwOutlinePageEntity review = pages.get(pages.size() - 1);
    assertTrue(review.getKeyPoints().contains("回顾并简述"), "缺 review_questions 时用章核心结论兜底");
  }

  @Test
  void u07_chaptersOverSixMerged() {
    List<OutlineChapterDraft> chapters = new ArrayList<>();
    for (int i = 1; i <= 8; i++) {
      chapters.add(chapter("第" + i + "章", 1));
    }
    List<CwOutlinePageEntity> pages = CoursewareOutlineService.assemblePages(
        new OutlineDraft(chapters, List.of("q1", "q2", "q3")), "课件A", 1L, 10159L, 7L, 50);
    long maxChapterNo = pages.stream()
        .map(CwOutlinePageEntity::getChapterNo).filter(java.util.Objects::nonNull)
        .mapToInt(Integer::intValue).max().orElse(0);
    assertEquals(6, maxChapterNo, "章数钳制到 6(第 7/8 章并入第 6 章)");
  }

  @Test
  void u07_pagesOverFiftyTruncated() {
    List<OutlineChapterDraft> chapters = List.of(
        chapter("一", 20), chapter("二", 20), chapter("三", 20));
    List<CwOutlinePageEntity> pages = CoursewareOutlineService.assemblePages(
        new OutlineDraft(chapters, List.of("q1", "q2", "q3")), "课件A", 1L, 10159L, 7L, 50);
    assertEquals(50, pages.size(), "总页数钳制 50(含 3 固定页)");
    assertEquals(CwOutlinePageEntity.ROLE_REVIEW, pages.get(49).getPageRole());
  }

  @Test
  void u07_invalidRefsFilteredInParse() {
    String json = """
        {"chapters":[
          {"chapter_title":"一","core_conclusion":"c","pages":[
            {"title":"t1","objective":"o","key_points":["a"],"page_role":"content",
             "layout_hint":"content_table","source_refs":["S1:1","S1:99"]}]}
        ],"review_questions":["q1","q2","q3"]}
        """;
    OutlineDraft draft = CoursewareOutlineService.parseOutline(json, Set.of("S1:1"), om);
    assertEquals(List.of("S1:1"), draft.chapters().get(0).pages().get(0).sourceRefs());
    assertEquals("content_table", draft.chapters().get(0).pages().get(0).layoutHint());
  }

  @Test
  void u15_confirmSnapshotsSlides() {
    CwProjectEntity project = new CwProjectEntity();
    project.setId(100L);
    project.setTenantId(10159L);
    project.setStatus(CwProjectEntity.STATUS_OUTLINE_READY);
    when(projectMapper.selectById(100L)).thenReturn(project);
    when(projectMapper.updateById(any(CwProjectEntity.class))).thenReturn(1);

    List<CwOutlinePageEntity> outline = List.of(
        outlinePage(1L, 1, "封面", "cover", "cover"),
        outlinePage(2L, 2, "目录", "toc", "toc"),
        outlinePage(3L, 3, "第一章", "section", "section"),
        outlinePage(4L, 4, "结论页", "content", "content_bullets"));
    when(outlineMapper.selectList(any())).thenReturn(outline);

    boolean ok = service.confirm(10159L, 7L, 100L);
    assertTrue(ok);
    ArgumentCaptor<CwSlideEntity> captor = ArgumentCaptor.forClass(CwSlideEntity.class);
    verify(slideMapper, times(4)).insert(captor.capture());
    // 快照固化:slide.layoutId 来自确认时刻大纲值(U-15)
    assertEquals("content_bullets", captor.getAllValues().get(3).getLayoutId());
    assertEquals(CwSlideEntity.STATUS_PENDING, captor.getAllValues().get(0).getStatus());
    // 确认后编辑大纲(updateById outline)不影响 slide 行 —— slide 由独立 insert 生成,
    // 大纲后续变更只写 cw_outline_page,不回写 cw_slide
  }

  @Test
  void u15_confirmTwiceRejected() {
    CwProjectEntity project = new CwProjectEntity();
    project.setId(100L);
    project.setTenantId(10159L);
    project.setStatus(CwProjectEntity.STATUS_GENERATING);
    when(projectMapper.selectById(100L)).thenReturn(project);
    assertThrows(BusinessException.class, () -> service.confirm(10159L, 7L, 100L));
  }

  @Test
  void u15_deleteFixedPageRejected() {
    CwOutlinePageEntity cover = outlinePage(1L, 1, "封面", "cover", "cover");
    cover.setChapterNo(0);
    when(outlineMapper.selectById(1L)).thenReturn(cover);
    CwProjectEntity project = new CwProjectEntity();
    project.setId(100L);
    project.setTenantId(10159L);
    project.setStatus(CwProjectEntity.STATUS_OUTLINE_READY);
    when(projectMapper.selectById(100L)).thenReturn(project);
    assertThrows(BusinessException.class, () -> service.deletePage(10159L, 1L));
  }

  private static CwOutlinePageEntity outlinePage(Long id, int position, String title,
                                                 String role, String layoutId) {
    CwOutlinePageEntity page = new CwOutlinePageEntity();
    page.setId(id);
    page.setTenantId(10159L);
    page.setProjectId(100L);
    page.setPosition(position);
    page.setTitle(title);
    page.setPageRole(role);
    page.setLayoutId(layoutId);
    page.setChapterNo(1);
    page.setKeyPoints("[]");
    page.setSourceRefs("[]");
    return page;
  }
}
