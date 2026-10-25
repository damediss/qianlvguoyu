package com.guoyu.platform.textbook.domain;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 教材 Markdown 统一支持（方案 §8.1）：保存校验、纯文本/字数、覆盖检查、docx 导出
 * 共用同一解析实现，禁止三套各自理解 Markdown。
 *
 * <p>允许：标题、段落、粗斜体、列表、引用、表格、代码块、http/https/mailto 链接、
 * 内部图片（textbook-asset://{assetId}）。
 * 禁止：原始 HTML block/inline、data:/javascript:/blob: 等危险 scheme、外链图片、
 * 不属于本节的资产引用。后端校验失败不保存，不依赖前端过滤。
 */
public final class TextbookMarkdownSupport {

  public static final String ASSET_URI_PREFIX = "textbook-asset://";
  public static final String PLACEHOLDER = "[待专家补充]";
  /** 单节 Markdown 上限（方案 §7.2-3） */
  public static final int MAX_CONTENT_LENGTH = 1_000_000;

  private static final Pattern ASSET_URI_PATTERN =
      Pattern.compile("^" + ASSET_URI_PREFIX + "(\\d{1,19})$");
  private static final Set<String> ALLOWED_LINK_SCHEMES = Set.of("http", "https", "mailto");

  private static final Parser PARSER = Parser.builder()
      .extensions(List.of(TablesExtension.create()))
      .build();

  private TextbookMarkdownSupport() {
  }

  /** 共享解析器入口（导出器遍历 AST 用同一套理解）。 */
  public static Node parse(String markdown) {
    return PARSER.parse(markdown == null ? "" : markdown);
  }

  /**
   * 安全校验（方案 §8.1）。返回违例清单（空 = 通过）；allowedAssetIds 为该节当前合法资产
   * id 集合，引用集合之外的资产（伪造/跨节/跨章/跨租户）一律拒绝。
   */
  public static List<String> validate(String markdown, Set<Long> allowedAssetIds) {
    List<String> violations = new ArrayList<>();
    if (markdown == null || markdown.isEmpty()) {
      return violations;
    }
    Node root = parse(markdown);
    walkValidate(root, allowedAssetIds, violations);
    return violations;
  }

  private static void walkValidate(Node node, Set<Long> allowedAssetIds,
      List<String> violations) {
    for (Node child : node.getChildren()) {
      if (child instanceof HtmlBlock) {
        violations.add("不允许原始 HTML 块（含 script/iframe/style/事件属性）");
        continue;
      }
      if (child instanceof HtmlInline) {
        violations.add("不允许内联 HTML 标签");
        continue;
      }
      if (child instanceof Link link) {
        String scheme = schemeOf(urlText(link.getUrl()));
        if (!ALLOWED_LINK_SCHEMES.contains(scheme)) {
          violations.add("链接仅支持 http/https/mailto，拒绝：" + schemeOf(urlText(link.getUrl())));
        }
      }
      if (child instanceof Image image) {
        String url = urlText(image.getUrl());
        Matcher matcher = ASSET_URI_PATTERN.matcher(url);
        if (!matcher.matches()) {
          violations.add("图片仅支持本节内部资产 textbook-asset://{assetId}，拒绝外链/危险协议图片");
        } else {
          long assetId = Long.parseLong(matcher.group(1));
          if (allowedAssetIds == null || !allowedAssetIds.contains(assetId)) {
            violations.add("图片资产 " + assetId + " 不属于本节，禁止跨节/跨章引用");
          }
        }
      }
      walkValidate(child, allowedAssetIds, violations);
    }
  }

  /** 渲染后纯文本：保留行内代码与表格文字，换行折叠为空格（不做 HTML 渲染，零 HTML 面积）。 */
  public static String toPlainText(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(markdown.length());
    appendPlainText(parse(markdown), sb);
    return sb.toString().trim();
  }

  private static void appendPlainText(Node node, StringBuilder sb) {
    for (Node child : node.getChildren()) {
      if (child instanceof Text text) {
        sb.append(text.getChars()).append(' ');
      } else if (child instanceof Code code) {
        sb.append(code.getText()).append(' ');
      } else if (child instanceof SoftLineBreak || child instanceof HardLineBreak) {
        sb.append(' ');
      } else {
        appendPlainText(child, sb);
      }
    }
  }

  /**
   * 统一字数（方案 §8.3）：纯文本去除所有空白字符后的字符数；
   * Markdown 标记与图片 URI 不计，标点计入。
   */
  public static int wordCount(String markdown) {
    String plain = toPlainText(markdown);
    if (plain.isEmpty()) {
      return 0;
    }
    return plain.replaceAll("\\s+", "").length();
  }

  /** 全部图片资产 URI（校验/删除引用检查/A2 预授权用）。 */
  public static List<String> imageAssetUris(String markdown) {
    List<String> uris = new ArrayList<>();
    if (markdown == null || markdown.isEmpty()) {
      return uris;
    }
    collectImageUris(parse(markdown), uris);
    return uris;
  }

  /** Markdown 中出现的资产 id 集合（数值化；非法形态 URI 不会进入该集合，由 validate 拦）。 */
  public static Set<Long> referencedAssetIds(String markdown) {
    java.util.Set<Long> ids = new java.util.HashSet<>();
    for (String uri : imageAssetUris(markdown)) {
      Matcher matcher = ASSET_URI_PATTERN.matcher(uri);
      if (matcher.matches()) {
        ids.add(Long.parseLong(matcher.group(1)));
      }
    }
    return ids;
  }

  private static void collectImageUris(Node node, List<String> uris) {
    for (Node child : node.getChildren()) {
      if (child instanceof Image image) {
        uris.add(urlText(image.getUrl()));
      }
      collectImageUris(child, uris);
    }
  }

  /** [待专家补充] 计数（方案 §8.3）。 */
  public static int placeholderCount(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while ((idx = markdown.indexOf(PLACEHOLDER, idx)) >= 0) {
      count++;
      idx += PLACEHOLDER.length();
    }
    return count;
  }

  private static String urlText(BasedSequence url) {
    return url == null ? "" : url.toString().trim();
  }

  /** 取 URL scheme（小写）；无 scheme（相对地址）返回空串。 */
  private static String schemeOf(String url) {
    if (url == null || url.isEmpty()) {
      return "";
    }
    String lower = url.toLowerCase();
    int colon = lower.indexOf(':');
    if (colon <= 0) {
      return "";
    }
    String scheme = lower.substring(0, colon);
    for (int i = 0; i < scheme.length(); i++) {
      char c = scheme.charAt(i);
      boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
          || c == '+' || c == '-' || c == '.';
      if (!ok) {
        return "";
      }
    }
    return scheme;
  }
}
