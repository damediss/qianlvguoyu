package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareLayout.Layout;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock;
import com.guoyu.platform.courseware.domain.SlideValidator;
import com.guoyu.platform.courseware.domain.StructureIssue;
import java.util.List;
import org.junit.jupiter.api.Test;

/** U-09/U-10:五类校验规则正反例与修复分流 */
class SlideValidatorTest {

  private static final Layout BULLETS = CoursewareLayout.require(CoursewareLayout.CONTENT_BULLETS);

  private static SlideBlock title(String text) {
    return SlideBlock.text("title", text);
  }

  private static SlideBlock bullets(String... items) {
    return SlideBlock.bullets("body", List.of(items));
  }

  private static List<StructureIssue> validate(List<SlideBlock> blocks) {
    return SlideValidator.validate(blocks, BULLETS, "content", "medium");
  }

  // ── U-09 structure ──

  @Test
  void u09_validSlideNoIssues() {
    List<StructureIssue> issues = validate(List.of(
        title("淬火温度决定硬度上限"),
        bullets("淬火加热到 850℃ 以上保温使组织奥氏体化", "冷却速度决定马氏体转变是否充分",
            "回火消除淬火应力并调整韧性")));
    assertTrue(issues.isEmpty(), "合法页不应有问题: " + issues);
  }

  @Test
  void u09_unknownSlotIsError() {
    List<StructureIssue> issues = validate(List.of(
        title("标题"), SlideBlock.text("ghost", "x"),
        bullets("要点一具体内容", "要点二具体内容", "要点三具体内容")));
    assertTrue(issues.stream().anyMatch(i -> i.isError() && "ghost".equals(i.slotId())));
  }

  @Test
  void u09_duplicateSlotIsError() {
    List<StructureIssue> issues = validate(List.of(
        title("标题一"), SlideBlock.text("title", "标题二"),
        bullets("要点一具体内容", "要点二具体内容", "要点三具体内容")));
    assertTrue(issues.stream().anyMatch(i -> i.isError() && i.message().contains("重复")));
  }

  @Test
  void u09_typeNotAcceptedIsError() {
    List<StructureIssue> issues = validate(List.of(
        SlideBlock.bullets("title", List.of("a")), // title 槽不接受 bullets
        bullets("要点一具体内容", "要点二具体内容", "要点三具体内容")));
    assertTrue(issues.stream().anyMatch(i -> i.isError() && i.message().contains("只接受")));
  }

  @Test
  void u09_missingRequiredIsError() {
    List<StructureIssue> issues = validate(List.of(
        bullets("要点一具体内容", "要点二具体内容", "要点三具体内容")));
    assertTrue(issues.stream().anyMatch(i -> i.isError() && i.message().contains("必填")));
  }

  @Test
  void u09_structureErrorBlocksExport() {
    List<StructureIssue> issues = validate(List.of(
        bullets("要点一具体内容", "要点二具体内容", "要点三具体内容")));
    assertTrue(SlideValidator.hasBlockingIssue(issues));
  }

  // ── U-10 capacity / overflow / empty / thin ──

  @Test
  void u10_capacityWarningNotRepairWorthy() {
    List<StructureIssue> issues = validate(List.of(
        title("t".repeat(50)),
        bullets("要点一的具体内容充实可靠", "要点二的具体内容充实可靠", "要点三的具体内容充实可靠",
            "要点四的具体内容充实可靠", "要点五的具体内容充实可靠", "要点六的具体内容充实可靠")));
    assertTrue(issues.stream().anyMatch(i -> StructureIssue.CODE_CAPACITY.equals(i.code())));
    assertFalse(issues.stream().anyMatch(StructureIssue::isRepairWorthy),
        "capacity 告警不触发修复");
  }

  @Test
  void u10_overflowWarning() {
    String longItem = "这是一条非常长的要点用来触发溢出估算".repeat(30);
    List<StructureIssue> issues = validate(List.of(title("标题"),
        bullets(longItem, longItem, longItem)));
    assertTrue(issues.stream().anyMatch(i -> StructureIssue.CODE_OVERFLOW.equals(i.code())),
        "超长文本应报溢出估算告警");
    assertFalse(issues.stream().anyMatch(StructureIssue::isRepairWorthy));
  }

  @Test
  void u10_emptyPhraseRepairWorthy() {
    List<StructureIssue> issues = validate(List.of(
        title("标题"), bullets("本页介绍热处理", "要点二具体内容", "要点三具体内容")));
    assertTrue(issues.stream().anyMatch(i -> StructureIssue.CODE_EMPTY_PHRASE.equals(i.code())));
    assertTrue(issues.stream().anyMatch(StructureIssue::isRepairWorthy));
  }

  @Test
  void u10_thinContentRepairWorthy() {
    // 只有 2 条要点 → 过瘦
    List<StructureIssue> issues = validate(List.of(
        title("标题"), bullets("要点一内容充实", "要点二内容充实")));
    assertTrue(issues.stream().anyMatch(i -> StructureIssue.CODE_THIN_CONTENT.equals(i.code())));
    assertTrue(issues.stream().anyMatch(StructureIssue::isRepairWorthy));
  }

  @Test
  void u10_shortBulletsMajorityRepairWorthy() {
    List<StructureIssue> issues = validate(List.of(
        title("标题"), bullets("短", "很短", "也短")));
    assertTrue(issues.stream().anyMatch(i -> i.message().contains("要点过短")));
  }

  @Test
  void u10_nonContentRoleSkipsThin() {
    Layout section = CoursewareLayout.require(CoursewareLayout.SECTION);
    List<StructureIssue> issues = SlideValidator.validate(List.of(
        SlideBlock.text("title", "第一章"), SlideBlock.text("text", "一句引言")),
        section, "section", "medium");
    assertTrue(issues.stream().noneMatch(i -> StructureIssue.CODE_THIN_CONTENT.equals(i.code())));
  }

  @Test
  void u09_blankTableRowIsError() {
    Layout tableLayout = CoursewareLayout.require(CoursewareLayout.CONTENT_TABLE);
    List<StructureIssue> issues = SlideValidator.validate(List.of(
        SlideBlock.text("title", "参数对照表"),
        SlideBlock.table("body", List.of("参数", "值"), List.of(
            List.of("温度", "850℃"), List.of(), List.of(" ", " ")))),
        tableLayout, "content", "medium");
    assertTrue(issues.stream().anyMatch(i -> i.isError() && i.message().contains("空白数据行")),
        "空白数据行应报 structure error 触发修复");
    assertTrue(issues.stream().anyMatch(StructureIssue::isRepairWorthy));
  }

  @Test
  void u10_tableCapacityRules() {
    Layout tableLayout = CoursewareLayout.require(CoursewareLayout.CONTENT_TABLE);
    List<StructureIssue> issues = SlideValidator.validate(List.of(
        SlideBlock.text("title", "参数对照表"),
        SlideBlock.table("body", List.of("参数", "值"), List.of(
            List.of("温度", "850℃"), List.of("时间", "30min"), List.of("介质", "油"),
            List.of("a", "b"), List.of("c", "d"), List.of("e", "f"), List.of("g", "h"),
            List.of("i", "j"), List.of("k", "l")))),
        tableLayout, "content", "medium");
    assertTrue(issues.stream().anyMatch(i -> i.message().contains("行,超出上限")));
  }
}
