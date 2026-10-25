package com.guoyu.platform.textbook.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.textbook.vo.TextbookCheckReportVO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Markdown 统一支持（方案 §12.1-TextbookMarkdownSupportTest）：
 * 危险构造拒绝、合法构造通过、字数口径、覆盖报告与占位计数。
 */
class TextbookMarkdownSupportTest {

  private static final Set<Long> OWN_ASSETS = Set.of(9901L);

  @Test
  @DisplayName("原始 HTML/script/javascript/data/外链图片拒绝")
  void dangerousConstructsRejected() {
    assertTrue(!TextbookMarkdownSupport.validate(
        "<script>alert(1)</script>\n\n正文", OWN_ASSETS).isEmpty(), "script 块拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "段落中 <img src=x onerror=alert(1)> 内联", OWN_ASSETS).isEmpty(), "内联 HTML 拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "[x](javascript:alert(1))", OWN_ASSETS).isEmpty(), "javascript: 链接拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "[x](data:text/html;base64,xxx)", OWN_ASSETS).isEmpty(), "data: 链接拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "![i](blob:xyz)", OWN_ASSETS).isEmpty(), "blob: 图片拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "![i](https://evil.example.com/a.png)", OWN_ASSETS).isEmpty(), "外链图片拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "![i](textbook-asset://8888)", OWN_ASSETS).isEmpty(), "非本节资产引用拒绝");
    assertTrue(!TextbookMarkdownSupport.validate(
        "![i](textbook-asset://abc)", OWN_ASSETS).isEmpty(), "伪造 assetId 拒绝");
  }

  @Test
  @DisplayName("合法表格/链接/内部图片通过")
  void legalConstructsAccepted() {
    String md = """
        ## 更换阳极步骤

        **准备**：确认槽号，*天气*不影响。

        | 工具 | 数量 |
        | --- | --- |
        | 扳手 | 2 |

        - [ ] 检查残极
        - 完成记录

        > 参考 [操作规程](https://example.com/doc) 或 [联系](mailto:a@b.c)。

        ![换极示意](textbook-asset://9901)

        ```
        code block
        ```
        """;
    assertTrue(TextbookMarkdownSupport.validate(md, OWN_ASSETS).isEmpty(),
        "合法 Markdown 不应有违例");
  }

  @Test
  @DisplayName("字数不计 Markdown 标记与图片 URI（alt 文本计入）")
  void wordCountIgnoresMarkup() {
    String md = "## 标题\n\n正文五个字\n\n![图](textbook-asset://9901)\n\n**加粗**也是字";
    // 纯文本 = "标题/正文五个字/图(alt)/加粗也是字" → 去空白后 13 字（标记与 URI 不计）
    assertEquals(13, TextbookMarkdownSupport.wordCount(md));
    assertEquals(0, TextbookMarkdownSupport.wordCount("![](textbook-asset://9901)"),
        "只有图片（无 alt 文本）不算正文字数");
    assertEquals(0, TextbookMarkdownSupport.wordCount(""));
  }

  @Test
  @DisplayName("覆盖报告与占位计数（§8.3）")
  void coverageReportAndPlaceholder() {
    String md = "操作前能确认换极槽号，检查残极状态。[待专家补充] 详见后文。";
    List<TextbookContentChecker.StandardPoint> points = List.of(
        new TextbookContentChecker.StandardPoint(9201L, "换极表知识", "theory"),
        new TextbookContentChecker.StandardPoint(9202L, "能确认换极槽号", "skill"));
    TextbookCheckReportVO report = TextbookContentChecker.check(md, 5000, points);
    assertEquals(1, report.placeholderCount());
    assertEquals(1, report.uncoveredPoints().size(), "完整标题包含匹配：仅「换极表知识」未命中");
    assertEquals("9201", report.uncoveredPoints().get(0).pointId());
    assertEquals(false, report.withinTargetRange());
    assertEquals(4000, report.minSuggestedWords());
    assertEquals(6000, report.maxSuggestedWords());
    assertTrue(report.checkedAt() != null);
  }
}
