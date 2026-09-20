package com.guoyu.platform.courseware.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestTopic;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideGenInput;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import java.util.List;

/** 课件生成全部 prompt 构建(方案 §6.5.2/§7/§8.3):契约与容量写死在这里,与校验器同源。 */
public final class CoursewarePrompts {

  private CoursewarePrompts() {
  }

  // ──────────────── 消化层 ────────────────

  public static String digestSystem() {
    return "你是职业培训教研员。必须只输出一个 JSON 对象,不要 Markdown,不要额外说明。\n"
        + "JSON 结构:{\"topics\":[{\"name\":\"...\",\"points\":[\"...\"],\"confusions\":[\"...\"],"
        + "\"key_data\":[\"...\"],\"source_refs\":[\"S1:2\"]}]}\n"
        + "硬性约束:\n"
        + "1. 按知识领域归纳 3~8 个 topic。\n"
        + "2. points=学员必须掌握的核心结论,每条一句话、具体可考核。\n"
        + "3. confusions=易混淆/易错点(相近概念辨析、常见误解),没有就留空数组。\n"
        + "4. key_data=关键数字(温度/时间/比例/参数等),必须原文出现,禁止编造。\n"
        + "5. source_refs 只能引用给定的小节编号。\n"
        + "6. 全部使用中文。";
  }

  public static String digestUser(String title, String audience, List<SourceSection> sections,
                                  ObjectMapper om) {
    ObjectNode body = om.createObjectNode();
    body.put("doc_title", title);
    body.put("audience", audience);
    body.set("sections", sectionsJson(sections, om));
    return "请把以下职业标准文档消化为教学素材,输出 JSON。\n" + body.toString();
  }

  // ──────────────── 大纲 ────────────────

  public static String outlineSystem() {
    return "你是职业培训课件策划。必须只输出一个 JSON 对象,不要 Markdown,不要额外说明。\n"
        + "JSON 结构:{\"chapters\":[{\"chapter_title\":\"...\",\"core_conclusion\":\"...\","
        + "\"pages\":[{\"title\":\"...\",\"objective\":\"...\",\"key_points\":[\"...\"],"
        + "\"page_role\":\"section|content\","
        + "\"layout_hint\":\"content_bullets|content_table|content_kpi\","
        + "\"source_refs\":[\"S1:2\"]}]}],\"review_questions\":[\"...\"]}\n"
        + "硬性约束:\n"
        + "1. 3~6 章,每章首页 page_role=section(title 用章标题),其余页 page_role=content。\n"
        + "2. 内容页 title 必须是本页核心结论句(如「淬火温度决定硬度上限」),禁止名词短语"
        + "(如「淬火工艺简介」)。\n"
        + "3. key_points 3~5 条,每条具体可考核;内容页总数按内容量定,不超过 47 页。\n"
        + "4. layout_hint:对比/分类/参数列表内容用 content_table;关键数据展示用 content_kpi;"
        + "其余用 content_bullets。\n"
        + "5. source_refs 只能引用给定的小节编号;数字必须来自来源文本,禁止编造。\n"
        + "6. review_questions:3~5 道复习思考题,覆盖各章核心结论与易混淆点。\n"
        + "7. 全部使用中文,面向自学学员,语言口语化。";
  }

  public static String outlineUser(String title, String audience, String density,
                                   List<SourceSection> sections, DigestDraft digest,
                                   ObjectMapper om) {
    ObjectNode body = om.createObjectNode();
    body.put("doc_title", title);
    body.put("audience", audience);
    body.put("content_density", density);
    if (digest != null && digest.topics() != null && !digest.topics().isEmpty()) {
      ArrayNode topics = om.createArrayNode();
      for (DigestTopic topic : digest.topics()) {
        ObjectNode t = om.createObjectNode();
        t.put("name", topic.name());
        t.putPOJO("points", topic.points());
        t.putPOJO("confusions", topic.confusions());
        t.putPOJO("key_data", topic.keyData());
        t.putPOJO("source_refs", topic.sourceRefs());
        topics.add(t);
      }
      body.set("digested_topics", topics);
      body.set("sections", sectionsJson(sections, om));
      return "请基于消化素材与来源小节,为课件设计章节大纲,输出 JSON。\n" + body.toString();
    }
    body.set("sections", sectionsJson(sections, om));
    return "请基于以下来源小节,为课件设计章节大纲,输出 JSON。\n" + body.toString();
  }

  // ──────────────── 单页 ────────────────

  public static String slideSystem(CoursewareLayout.Layout layout, ObjectMapper om) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是职业培训课件撰写助手。必须只输出一个 JSON 对象,不要 Markdown,不要额外说明。\n")
        .append("JSON 结构:{\"blocks\":[...],\"speaker_notes\":\"...\"}\n")
        .append("blocks 每个元素必须带 slot_id 与 type,按类型提供字段:\n")
        .append("- text: {\"slot_id\":\"title\",\"type\":\"text\",\"text\":\"...\"}\n")
        .append("- bullets: {\"slot_id\":\"body\",\"type\":\"bullets\",\"items\":[\"...\",\"...\"]}\n")
        .append("- table: {\"slot_id\":\"body\",\"type\":\"table\",\"header\":[\"...\"],\"rows\":[[\"...\"]]}\n")
        .append("- kpi: {\"slot_id\":\"kpi_1\",\"type\":\"kpi\",\"value\":\"37%\",\"label\":\"...\",\"note\":\"...\"}\n")
        .append("硬性约束:\n")
        .append("1. 只能使用下面列出的 slot_id,每个槽位最多出现一次,必填槽位不得缺失。\n")
        .append("2. 每个槽位只能使用它声明接受的 type。\n")
        .append("3. 严格遵守每个槽位的字数与条目上限;在上限内尽量写满支撑细节,贴近容量中上沿。\n")
        .append("4. 内容页标题必须是本页核心结论句(如「淬火温度决定硬度上限」),禁止名词短语。\n")
        .append("5. 正文面向自学学员,语言口语化,写具体结论与事实,不写「本页介绍……」这类空话。\n")
        .append("6. 涉及对比、分类、参数列表的内容优先用 table 块,不要大段文字。\n")
        .append("7. 操作步骤/流程用 bullets 按顺序表达,每条一个动作。\n")
        .append("8. 数字必须来自给定来源,缺少数据时不要编造,改用文字表述。\n")
        .append("9. speaker_notes 用 2~3 句话给出学习提示(本页重点、易错提醒)。\n")
        .append("本页布局为 ").append(layout.id()).append("(").append(layout.name()).append("),槽位规格:\n");
    ArrayNode slots = om.createArrayNode();
    for (CoursewareLayout.SlotSpec slot : layout.slots()) {
      ObjectNode s = om.createObjectNode();
      s.put("slot_id", slot.id());
      s.putPOJO("accepts", slot.accepts());
      s.put("required", slot.required());
      if (slot.maxChars() != null) {
        s.put("max_chars", slot.maxChars());
      }
      if (slot.maxItems() != null) {
        s.put("max_items", slot.maxItems());
      }
      if (slot.maxCharsPerItem() != null) {
        s.put("max_chars_per_item", slot.maxCharsPerItem());
      }
      if (slot.maxRows() != null) {
        s.put("max_rows", slot.maxRows());
      }
      if (slot.maxCols() != null) {
        s.put("max_cols", slot.maxCols());
      }
      slots.add(s);
    }
    sb.append(slots.toString());
    return sb.toString();
  }

  public static String slideUser(SlideGenInput input, ObjectMapper om) {
    ObjectNode body = om.createObjectNode();
    body.put("deck_title", input.deckTitle());
    body.put("audience", input.audience());
    ObjectNode page = om.createObjectNode();
    page.put("position", input.position());
    page.put("total_pages", input.totalPages());
    page.put("chapter", input.chapterTitle());
    page.put("title", input.pageTitle());
    page.put("objective", input.objective());
    page.putPOJO("key_points", input.keyPoints());
    page.put("page_role", input.pageRole());
    body.set("page", page);
    body.putPOJO("neighbor_titles", input.neighborTitles());
    if (input.digestSlice() != null && !input.digestSlice().isBlank()) {
      body.put("digested_material", input.digestSlice());
    }
    body.set("sections", sectionsJson(input.sections(), om));

    StringBuilder sb = new StringBuilder();
    sb.append(densityBlock(input.contentDensity(), input.pageRole()));
    sb.append("请为本页生成正文 JSON。\n").append(body.toString());
    if (input.issues() != null && !input.issues().isEmpty()) {
      sb.append("\n上一次生成存在以下问题,请只修正这些问题并保持其余内容稳定:")
          .append("若问题是内容过瘦或空话,请充实到密度档要求,勿超槽位上限;")
          .append("不要为消除溢出而删光支撑细节。\n");
      for (String issue : input.issues()) {
        sb.append("- ").append(issue).append('\n');
      }
    }
    return sb.toString();
  }

  /** 密度档提示(方案 §8.3;与校验器 thin_content 阈值同源) */
  static String densityBlock(String density, String pageRole) {
    if (!"content".equals(pageRole)) {
      return "";
    }
    return switch (density == null ? "medium" : density) {
      case "concise" -> "文字量档位-精简:要点 2~4 条,单条 8~25 字,只保留核心结论。\n";
      case "detailed" -> "文字量档位-详细:要点 4~5 条,单条 15~45 字,结论+支撑细节+易错提醒。\n";
      default -> "文字量档位-适中:要点 3~5 条,单条 12~45 字,结论+关键支撑。\n";
    };
  }

  private static ArrayNode sectionsJson(List<SourceSection> sections, ObjectMapper om) {
    ArrayNode arr = om.createArrayNode();
    for (SourceSection section : sections) {
      ObjectNode s = om.createObjectNode();
      s.put("ref", section.ref());
      s.put("heading", section.heading());
      s.put("text", section.text());
      arr.add(s);
    }
    return arr;
  }
}
