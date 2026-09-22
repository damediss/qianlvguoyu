package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.courseware.entity.CwKnowledgePointEntity;
import com.guoyu.platform.courseware.service.DocxSectionParser;
import com.guoyu.platform.courseware.service.KnowledgePointEnumerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** U-16:知识点枚举(基本要求三级编号 + 工作要求表格 x.y.z 条目) */
class KnowledgePointEnumeratorTest {

  private final KnowledgePointEnumerator enumerator =
      new KnowledgePointEnumerator(new DocxSectionParser());

  @Test
  void u16_realElectricianDocx() throws Exception {
    Path path = Path.of("../../docs/osta-standards/电工.docx");
    if (!Files.exists(path)) {
      path = Path.of("docs/osta-standards/电工.docx");
    }
    byte[] bytes = Files.readAllBytes(path);
    List<CwKnowledgePointEntity> points = enumerator.enumerate(bytes);
    long basic = points.stream()
        .filter(p -> CwKnowledgePointEntity.CATEGORY_BASIC.equals(p.getCategory())).count();
    long work = points.stream()
        .filter(p -> CwKnowledgePointEntity.CATEGORY_WORK.equals(p.getCategory())).count();
    assertEquals(10, basic, "基本要求 10 个知识点(2.1.1~2.2.8)");
    assertEquals(184, work, "工作要求 184 个相关知识要求知识点(逻辑列展开后双路径复核)");

    // 首个工作要求点:1.1.1 常用低压电器图形符号、文字符号的国家标准
    CwKnowledgePointEntity first = points.stream()
        .filter(p -> CwKnowledgePointEntity.CATEGORY_WORK.equals(p.getCategory()))
        .findFirst().orElseThrow();
    assertEquals("1.1.1", first.getCode());
    assertTrue(first.getTitle().contains("图形符号"), first.getTitle());
    assertEquals("五级/初级工", first.getLevelName());
    assertTrue(first.getJobFunction().contains("电器安装"), first.getJobFunction());
    assertNotNull(first.getSourceRef());

    // 1.1.2 应与技能要求配对(技能列 1.1.2 存在则挂接)
    CwKnowledgePointEntity p112 = points.stream()
        .filter(p -> "1.1.2".equals(p.getCode())
            && CwKnowledgePointEntity.CATEGORY_WORK.equals(p.getCategory()))
        .findFirst().orElseThrow();
    assertTrue(p112.getTitle().contains("结构"), p112.getTitle());
    assertNotNull(p112.getSkillText(), "1.1.2 应挂接技能要求原文");

    // 等级分章可用:五个等级均出现
    long levels = points.stream()
        .filter(p -> CwKnowledgePointEntity.CATEGORY_WORK.equals(p.getCategory()))
        .map(CwKnowledgePointEntity::getLevelName).distinct().count();
    assertEquals(5, levels);

    // 基本要求首点:2.1.1 职业道德基本知识,所属节=职业道德
    CwKnowledgePointEntity b1 = points.stream()
        .filter(p -> CwKnowledgePointEntity.CATEGORY_BASIC.equals(p.getCategory()))
        .findFirst().orElseThrow();
    assertEquals("2.1.1", b1.getCode());
    assertTrue(b1.getTitle().contains("职业道德"), b1.getTitle());
    assertEquals("职业道德", b1.getJobFunction());
  }

  @Test
  void u16_splitXyzItemsWithSpaces() {
    Map<String, String> items = KnowledgePointEnumerator.splitXyzItems(
        "1. 1. 1 常用低压电器图形符号、文字符号的国家标准\n"
            + "1. 1. 2 常用低压电器的结构、工作原理及使用方法");
    assertEquals(2, items.size());
    assertEquals("常用低压电器图形符号、文字符号的国家标准", items.get("1.1.1"));
    assertEquals("常用低压电器的结构、工作原理及使用方法", items.get("1.1.2"));
  }

  @Test
  void u16_cjkSpacesRemovedFromTitles() {
    // 用户口径 2026-09-22:标题中 CJK 逐字空格自动去除
    assertEquals("三相交流笼型异步电动机",
        KnowledgePointEnumerator.normalizeCjkSpaces("三 相 交 流 笼 型 异 步 电 动 机"));
    // 拉丁字母间空格不处理(保留「PLC 编程」等合法间距),仅 CJK-CJK 去空格
    assertEquals("P L C 编程基础", KnowledgePointEnumerator.normalizeCjkSpaces("P L C 编 程 基 础"));
    // 真实文档:三级编号标题去除编号后归一化
    assertEquals("常用低压电器的结构、工作原理及使用方法", KnowledgePointEnumerator.normalizeCjkSpaces(
        KnowledgePointEnumerator.stripNumber("1. 1. 2 常 用 低 压 电 器 的结构 、工作原理及使用方法")));
  }

  @Test
  void u16_numberedDepthAndStrip() {
    assertEquals(3, KnowledgePointEnumerator.numberedDepth("2. 1. 1 职业道德基本知识"));
    assertEquals(2, KnowledgePointEnumerator.numberedDepth("2. 1   职业道德"));
    assertEquals(1, KnowledgePointEnumerator.numberedDepth("2.  基本要求"));
    assertEquals("职业道德基本知识",
        KnowledgePointEnumerator.stripNumber("2. 1. 1 职业道德基本知识"));
    assertEquals("五级/初级工", KnowledgePointEnumerator.stripNumber("3. 1   五级/初级工"));
  }
}
