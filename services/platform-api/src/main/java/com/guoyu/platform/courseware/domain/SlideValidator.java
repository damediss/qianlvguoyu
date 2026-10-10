package com.guoyu.platform.courseware.domain;

import com.guoyu.platform.courseware.domain.CoursewareLayout.Layout;
import com.guoyu.platform.courseware.domain.CoursewareLayout.SlotSpec;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单页确定性校验器(方案 §8.4):结构/容量/溢出/空话/过瘦五类规则,纯 Java 无 LLM。
 * error=structure 阻断导出;empty_phrase/thin_content 触发修复;capacity/overflow 只透出。
 */
public final class SlideValidator {

  private SlideValidator() {
  }

  /** 空话/占位词表(内置可配;只放长度≥3 的确定短语,避免误伤"战略/策略"等) */
  private static final List<String> EMPTY_PHRASES = List.of(
      "本页介绍", "本页主要介绍", "敬请期待", "TODO", "todo", "占位符", "此处省略", "此处略去",
      "内容待补充", "待完善", "相关内容略");

  /** 密度档:要点下限/单条最短字数(方案 §8.4 medium 档 ≥3 条、≥12 字) */
  private static final Map<String, int[]> DENSITY_PROFILE = Map.of(
      "concise", new int[] {2, 8},
      "medium", new int[] {3, 12},
      "detailed", new int[] {4, 15});

  public static List<StructureIssue> validate(List<SlideBlock> blocks, Layout layout,
                                              String pageRole, String contentDensity) {
    List<StructureIssue> issues = new ArrayList<>();
    validateStructure(blocks, layout, issues);
    validateCapacity(blocks, layout, issues);
    validateOverflow(blocks, layout, issues);
    validateEmptyPhrases(blocks, issues);
    validateThinContent(blocks, layout, pageRole, contentDensity, issues);
    return issues;
  }

  // ──────────────── structure(error) ────────────────

  static void validateStructure(List<SlideBlock> blocks, Layout layout, List<StructureIssue> issues) {
    Set<String> seen = new HashSet<>();
    for (SlideBlock block : blocks) {
      SlotSpec slot = layout.slot(block.slotId());
      if (slot == null) {
        issues.add(StructureIssue.error(block.slotId(), "版式 " + layout.id() + " 不存在槽位 " + block.slotId()));
        continue;
      }
      if (!seen.add(block.slotId())) {
        issues.add(StructureIssue.error(block.slotId(), "槽位 " + block.slotId() + " 被重复填充"));
      }
      if (!slot.accepts().contains(block.type())) {
        issues.add(StructureIssue.error(block.slotId(),
            "槽位 " + block.slotId() + " 只接受 " + String.join("、", slot.accepts())
                + ",实际为 " + block.type()));
      }
      // v1.4:表格空白数据行(模型输出对象行/空内容未被兜住时)→ structure error 触发修复
      if (SlideBlock.TYPE_TABLE.equals(block.type())) {
        long blankRows = (block.rows() == null ? List.<List<String>>of() : block.rows()).stream()
            .filter(row -> row.isEmpty() || row.stream().allMatch(c -> c == null || c.isBlank()))
            .count();
        if (blankRows > 0) {
          issues.add(StructureIssue.error(block.slotId(),
              "表格存在 " + blankRows + " 行空白数据行,请补齐每一行的具体内容"));
        }
      }
    }
    for (SlotSpec slot : layout.slots()) {
      if (slot.required() && !seen.contains(slot.id())) {
        issues.add(StructureIssue.error(slot.id(), "必填槽位缺少内容: " + slot.id()));
      }
    }
  }

  // ──────────────── capacity(warning) ────────────────

  static void validateCapacity(List<SlideBlock> blocks, Layout layout, List<StructureIssue> issues) {
    for (SlideBlock block : blocks) {
      SlotSpec slot = layout.slot(block.slotId());
      if (slot == null) {
        continue;
      }
      switch (block.type()) {
        case SlideBlock.TYPE_TEXT -> {
          if (slot.maxChars() != null && block.text() != null
              && block.text().length() > slot.maxChars()) {
            issues.add(capacity(block.slotId(),
                "文字 " + block.text().length() + " 字,超出建议上限 " + slot.maxChars() + " 字"));
          }
        }
        case SlideBlock.TYPE_BULLETS -> {
          List<String> items = block.items() == null ? List.of() : block.items();
          if (slot.maxItems() != null && items.size() > slot.maxItems()) {
            issues.add(capacity(block.slotId(),
                "要点 " + items.size() + " 条,超出建议上限 " + slot.maxItems() + " 条"));
          }
          if (slot.maxCharsPerItem() != null) {
            for (int i = 0; i < items.size(); i++) {
              if (items.get(i) != null && items.get(i).length() > slot.maxCharsPerItem()) {
                issues.add(capacity(block.slotId(),
                    "第 " + (i + 1) + " 条要点 " + items.get(i).length() + " 字,超出单条上限 "
                        + slot.maxCharsPerItem() + " 字"));
              }
            }
          }
        }
        case SlideBlock.TYPE_TABLE -> {
          int cols = block.header() == null ? 0 : block.header().size();
          int rows = block.rows() == null ? 0 : block.rows().size();
          if (slot.maxCols() != null && cols > slot.maxCols()) {
            issues.add(capacity(block.slotId(), "表格 " + cols + " 列,超出上限 " + slot.maxCols() + " 列"));
          }
          if (slot.maxRows() != null && rows > slot.maxRows()) {
            issues.add(capacity(block.slotId(), "表格 " + rows + " 行,超出上限 " + slot.maxRows() + " 行"));
          }
          if (slot.maxCharsPerItem() != null) {
            long over = (block.rows() == null ? List.<List<String>>of() : block.rows()).stream()
                .flatMap(List::stream)
                .filter(c -> c != null && c.length() > slot.maxCharsPerItem())
                .count();
            if (over > 0) {
              issues.add(capacity(block.slotId(),
                  over + " 个单元格超出单格上限 " + slot.maxCharsPerItem() + " 字"));
            }
          }
        }
        case SlideBlock.TYPE_KPI -> {
          if (block.value() != null && block.value().length() > CoursewareLayout.KPI_VALUE_MAX) {
            issues.add(capacity(block.slotId(), "数值 " + block.value().length() + " 字,超出上限 "
                + CoursewareLayout.KPI_VALUE_MAX + " 字"));
          }
          if (block.label() != null && block.label().length() > CoursewareLayout.KPI_LABEL_MAX) {
            issues.add(capacity(block.slotId(), "标签 " + block.label().length() + " 字,超出上限 "
                + CoursewareLayout.KPI_LABEL_MAX + " 字"));
          }
          if (block.note() != null && block.note().length() > CoursewareLayout.KPI_NOTE_MAX) {
            issues.add(capacity(block.slotId(), "备注 " + block.note().length() + " 字,超出上限 "
                + CoursewareLayout.KPI_NOTE_MAX + " 字"));
          }
        }
        default -> {
          // 未知类型已被 structure 规则记录
        }
      }
    }
  }

  private static StructureIssue capacity(String slotId, String message) {
    return StructureIssue.warning(StructureIssue.CODE_CAPACITY, slotId, message);
  }

  // ──────────────── overflow(warning,估算路径) ────────────────

  /**
   * 字宽估算:CJK 1.0em、拉丁 0.55em、空格 0.33em(同 ai-ppt-generator 降级口径);
   * 行高 = 字号 × 1.5;bullets 另计缩进与项间距。高度超槽位矩形 → warning。
   */
  static void validateOverflow(List<SlideBlock> blocks, Layout layout, List<StructureIssue> issues) {
    for (SlideBlock block : blocks) {
      SlotSpec slot = layout.slot(block.slotId());
      if (slot == null) {
        continue;
      }
      double used = switch (block.type()) {
        case SlideBlock.TYPE_TEXT -> textHeight(block.text(), slot.rect().w(), slot.style().fontSizePt());
        case SlideBlock.TYPE_BULLETS -> {
          List<String> items = block.items() == null ? List.of() : block.items();
          double h = 0;
          for (String item : items) {
            h += textHeight(item, slot.rect().w() - CoursewareTheme.BULLET_INDENT_PT,
                slot.style().fontSizePt());
          }
          h += CoursewareTheme.BULLET_GAP_PT * Math.max(0, items.size() - 1);
          yield h;
        }
        case SlideBlock.TYPE_TABLE -> {
          int rows = (block.rows() == null ? 0 : block.rows().size()) + 1;
          yield rows * (slot.style().fontSizePt() * CoursewareTheme.LINE_HEIGHT_FACTOR + 8);
        }
        case SlideBlock.TYPE_KPI -> {
          // 数值 28pt + 标签 15pt + 备注 11pt 的行高合计(与渲染侧对齐)
          yield 28 * 1.3 + 15 * CoursewareTheme.LINE_HEIGHT_FACTOR
              + (block.note() == null || block.note().isBlank() ? 0 : 11 * 2.0);
        }
        default -> 0;
      };
      if (used > slot.rect().h() * 1.02) {
        issues.add(StructureIssue.warning(StructureIssue.CODE_OVERFLOW, block.slotId(),
            "估算占用高度约 " + (int) used + "pt,超出槽位可用高度 " + (int) slot.rect().h()
                + "pt,导出后建议检查版面"));
      }
    }
  }

  private static double textHeight(String text, double widthPt, int fontSizePt) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    double lineWidth = 0;
    int lines = 1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      double w;
      if (c == ' ' || c == '　') {
        w = fontSizePt * 0.33;
      } else if (c < 128) {
        w = fontSizePt * 0.55;
      } else {
        w = fontSizePt * 1.0;
      }
      if (lineWidth + w > widthPt) {
        lines++;
        lineWidth = w;
      } else {
        lineWidth += w;
      }
    }
    return lines * fontSizePt * CoursewareTheme.LINE_HEIGHT_FACTOR;
  }

  // ──────────────── empty_phrase(warning,触发修复) ────────────────

  static void validateEmptyPhrases(List<SlideBlock> blocks, List<StructureIssue> issues) {
    Set<String> reported = new HashSet<>();
    for (SlideBlock block : blocks) {
      String text = block.plainText();
      for (String phrase : EMPTY_PHRASES) {
        if (text.contains(phrase) && reported.add(block.slotId() + phrase)) {
          issues.add(StructureIssue.warning(StructureIssue.CODE_EMPTY_PHRASE, block.slotId(),
              "内容含空话或占位表述(「" + phrase + "」),请改成具体结论"));
        }
      }
    }
  }

  // ──────────────── thin_content(warning,触发修复) ────────────────

  static void validateThinContent(List<SlideBlock> blocks, Layout layout, String pageRole,
                                  String contentDensity, List<StructureIssue> issues) {
    if (!"content".equals(pageRole)) {
      return;
    }
    int[] profile = DENSITY_PROFILE.getOrDefault(
        contentDensity == null ? "medium" : contentDensity, DENSITY_PROFILE.get("medium"));
    int minItems = profile[0];
    int minChars = profile[1];

    List<SlideBlock> bulletBlocks = blocks.stream()
        .filter(b -> SlideBlock.TYPE_BULLETS.equals(b.type()))
        .toList();
    // medium/detailed 档内容页应包含要点列表(要点页版式)
    if (bulletBlocks.isEmpty() && CoursewareLayout.CONTENT_BULLETS.equals(layout.id())) {
      issues.add(StructureIssue.warning(StructureIssue.CODE_THIN_CONTENT, null,
          "内容页缺少要点列表,版面过瘦"));
      return;
    }
    for (SlideBlock block : bulletBlocks) {
      List<String> items = block.items() == null ? List.of() : block.items();
      if (items.size() < minItems) {
        issues.add(StructureIssue.warning(StructureIssue.CODE_THIN_CONTENT, block.slotId(),
            "要点偏少: " + items.size() + " 条,建议至少 " + minItems + " 条"));
      }
      long shortCount = items.stream()
          .filter(i -> i == null || i.strip().length() < minChars)
          .count();
      if (!items.isEmpty() && shortCount >= Math.max(1, items.size() / 2)) {
        issues.add(StructureIssue.warning(StructureIssue.CODE_THIN_CONTENT, block.slotId(),
            "要点过短: 多条不足 " + minChars + " 字,请补充具体结论或事实"));
      }
    }
  }

  /** 导出阻断判定:存在任一 error */
  public static boolean hasBlockingIssue(List<StructureIssue> issues) {
    return issues.stream().anyMatch(StructureIssue::isError);
  }
}
