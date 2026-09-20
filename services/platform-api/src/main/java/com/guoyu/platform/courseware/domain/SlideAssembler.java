package com.guoyu.platform.courseware.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * 单页草稿装配(方案 §8.1 assemble):解析模型 JSON → 槽位契约校验 → 合法块集合。
 * 非法块(未知槽位/重复槽位/类型不接受)剔除并记 structure issue,进入修复回路。
 */
public final class SlideAssembler {

  private SlideAssembler() {
  }

  public record ParsedDraft(List<CoursewareModels.SlideBlock> rawBlocks, String speakerNotes) {
  }

  /** 解析 JSON 为原始块(不做槽位校验);JSON 非法抛 IllegalArgumentException */
  public static ParsedDraft parseDraft(String content, ObjectMapper om) {
    JsonNode root = CoursewareJson.extractObject(content, om);
    JsonNode blocksNode = root.path("blocks");
    if (!blocksNode.isArray()) {
      throw new IllegalArgumentException("输出缺少 blocks 数组");
    }
    List<CoursewareModels.SlideBlock> blocks = new ArrayList<>();
    for (JsonNode node : blocksNode) {
      String slotId = textOrNull(node, "slot_id");
      String type = textOrNull(node, "type");
      if (slotId == null || slotId.isBlank() || type == null || type.isBlank()) {
        throw new IllegalArgumentException("blocks 元素缺少 slot_id 或 type");
      }
      blocks.add(switch (type) {
        case CoursewareModels.SlideBlock.TYPE_TEXT ->
            CoursewareModels.SlideBlock.text(slotId, textOrNull(node, "text"));
        case CoursewareModels.SlideBlock.TYPE_BULLETS ->
            CoursewareModels.SlideBlock.bullets(slotId, stringList(node.path("items")));
        case CoursewareModels.SlideBlock.TYPE_TABLE ->
            CoursewareModels.SlideBlock.table(slotId, stringList(node.path("header")),
                rowsList(node.path("rows")));
        case CoursewareModels.SlideBlock.TYPE_KPI ->
            CoursewareModels.SlideBlock.kpi(slotId, textOrNull(node, "value"),
                textOrNull(node, "label"), textOrNull(node, "note"));
        default -> throw new IllegalArgumentException("未知块类型: " + type);
      });
    }
    String notes = textOrNull(root, "speaker_notes");
    return new ParsedDraft(blocks, notes == null ? "" : notes);
  }

  /** 槽位契约校验:合法块保留,非法块剔除并记录 structure issue;必填缺失补记 */
  public static CoursewareModels.AssembledSlide assemble(List<CoursewareModels.SlideBlock> rawBlocks,
                                                         CoursewareLayout.Layout layout) {
    List<CoursewareModels.SlideBlock> kept = new ArrayList<>();
    List<StructureIssue> issues = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (CoursewareModels.SlideBlock block : rawBlocks) {
      CoursewareLayout.SlotSpec slot = layout.slot(block.slotId());
      if (slot == null) {
        issues.add(StructureIssue.error(block.slotId(),
            "版式 " + layout.id() + " 不存在槽位 " + block.slotId()));
        continue;
      }
      if (!seen.add(block.slotId())) {
        issues.add(StructureIssue.error(block.slotId(), "槽位 " + block.slotId() + " 被重复填充"));
        continue;
      }
      if (!slot.accepts().contains(block.type())) {
        issues.add(StructureIssue.error(block.slotId(),
            "槽位 " + block.slotId() + " 只接受 " + String.join("、", slot.accepts())
                + ",实际为 " + block.type()));
        continue;
      }
      kept.add(block);
    }
    for (CoursewareLayout.SlotSpec slot : layout.slots()) {
      if (slot.required() && !seen.contains(slot.id())) {
        issues.add(StructureIssue.error(slot.id(), "必填槽位缺少内容: " + slot.id()));
      }
    }
    return new CoursewareModels.AssembledSlide(kept, issues);
  }

  private static String textOrNull(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isTextual() ? v.asText() : null;
  }

  private static List<String> stringList(JsonNode node) {
    if (!node.isArray()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (JsonNode item : node) {
      out.add(item.isTextual() ? item.asText() : item.toString());
    }
    return out;
  }

  private static List<List<String>> rowsList(JsonNode node) {
    if (!node.isArray()) {
      return List.of();
    }
    List<List<String>> out = new ArrayList<>();
    for (JsonNode row : node) {
      out.add(stringList(row));
    }
    return out;
  }
}
