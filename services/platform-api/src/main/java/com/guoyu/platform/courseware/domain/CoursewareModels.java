package com.guoyu.platform.courseware.domain;

import java.util.List;

/** courseware 领域模型集合(records) */
public final class CoursewareModels {

  private CoursewareModels() {
  }

  /** 来源小节(docx 解析产物) */
  public record SourceSection(String ref, String heading, int level, String text, String locator) {
  }

  /** 消化 topic(素材消化层产物) */
  public record DigestTopic(String name, List<String> points, List<String> confusions,
                            List<String> keyData, List<String> sourceRefs) {
  }

  /** 消化产物整体 */
  public record DigestDraft(List<DigestTopic> topics) {
  }

  /** 大纲页草稿(LLM 输出) */
  public record OutlinePageDraft(String title, String objective, List<String> keyPoints,
                                 String pageRole, String layoutHint, List<String> sourceRefs) {
  }

  /** 大纲章草稿(LLM 输出) */
  public record OutlineChapterDraft(String chapterTitle, String coreConclusion,
                                    List<OutlinePageDraft> pages) {
  }

  /** 大纲草稿整体 */
  public record OutlineDraft(List<OutlineChapterDraft> chapters, List<String> reviewQuestions) {
  }

  /** 内容块(单一事实源;一期 4 种类型 text/bullets/table/kpi) */
  public record SlideBlock(String slotId, String type, String text, List<String> items,
                           List<String> header, List<List<String>> rows,
                           String value, String label, String note) {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_BULLETS = "bullets";
    public static final String TYPE_TABLE = "table";
    public static final String TYPE_KPI = "kpi";

    public static SlideBlock text(String slotId, String text) {
      return new SlideBlock(slotId, TYPE_TEXT, text, null, null, null, null, null, null);
    }

    public static SlideBlock bullets(String slotId, List<String> items) {
      return new SlideBlock(slotId, TYPE_BULLETS, null, items, null, null, null, null, null);
    }

    public static SlideBlock table(String slotId, List<String> header, List<List<String>> rows) {
      return new SlideBlock(slotId, TYPE_TABLE, null, null, header, rows, null, null, null);
    }

    public static SlideBlock kpi(String slotId, String value, String label, String note) {
      return new SlideBlock(slotId, TYPE_KPI, null, null, null, null, value, label, note);
    }

    /** 块内全部纯文本(空话检测/字数统计用) */
    public String plainText() {
      StringBuilder sb = new StringBuilder();
      if (text != null) {
        sb.append(text).append('\n');
      }
      if (items != null) {
        items.forEach(i -> sb.append(i).append('\n'));
      }
      if (header != null) {
        header.forEach(h -> sb.append(h).append(' '));
        sb.append('\n');
      }
      if (rows != null) {
        rows.forEach(r -> r.forEach(c -> sb.append(c).append(' ')));
      }
      if (value != null) {
        sb.append(value).append(' ');
      }
      if (label != null) {
        sb.append(label).append(' ');
      }
      if (note != null) {
        sb.append(note);
      }
      return sb.toString();
    }
  }

  /** 单页生成草稿(LLM 输出) */
  public record SlideDraft(List<SlideBlock> blocks, String speakerNotes) {
  }

  /** 装配结果:合法块 + 结构问题(非法块被剔除并记录) */
  public record AssembledSlide(List<SlideBlock> blocks, List<StructureIssue> issues) {
  }

  /** 单页生成输入(页级独立上下文,方案 §8.2) */
  public record SlideGenInput(
      String deckTitle,
      String audience,
      int position,
      int totalPages,
      String chapterTitle,
      String pageTitle,
      String objective,
      List<String> keyPoints,
      String pageRole,
      String layoutId,
      /** 本页引用小节(已裁剪) */
      List<SourceSection> sections,
      /** digest topic 片段(可为 null) */
      String digestSlice,
      List<String> neighborTitles,
      String contentDensity,
      /** 修复轮问题清单 */
      List<String> issues) {

    public SlideGenInput withIssues(List<String> newIssues) {
      return new SlideGenInput(deckTitle, audience, position, totalPages, chapterTitle, pageTitle,
          objective, keyPoints, pageRole, layoutId, sections, digestSlice, neighborTitles,
          contentDensity, newIssues);
    }
  }

  /** 单页工作流结果 */
  public record SlideWorkflowResult(List<SlideBlock> blocks, String speakerNotes,
                                    List<StructureIssue> issues, int repairRounds) {
  }
}
