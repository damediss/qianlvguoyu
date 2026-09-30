package com.guoyu.platform.exam.standard.parser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * docx 文本清洗与编号识别工具（规则来自《DOCX转Markdown转换指南》坑 2/3/4/5 与实测）。
 */
public final class DocxTextUtil {

  private DocxTextUtil() {
  }

  /** 职业编码碎片，如 "职业编码：6- 18-02-04"（页眉/正文混入） */
  private static final Pattern SERIES_NO_JUNK = Pattern.compile("职业编码[:：][\\d\\s\\-—.]+");
  /** 页码碎片：整段纯数字与空格（如 "105105"、"1 101 10"） */
  private static final Pattern PAGE_NO_LINE = Pattern.compile("^[\\d\\s　]+$");
  /** 编号标题：1. / 2.1 / 2.1.1 + 中文。编号后必须跟中文，避免误判职业编码（坑3） */
  private static final Pattern HEADING = Pattern.compile(
      "^(\\d{1,2})(?:\\s*\\.\\s*(\\d{1,2}))?(?:\\s*\\.\\s*(\\d{1,2}))?\\s*\\.?\\s*([\\u4e00-\\u9fff].*)$");
  /** 全文搜索 plausible 标题（用于页眉垃圾与标题粘连的段落） */
  private static final Pattern HEADING_SEARCH = Pattern.compile(
      "(\\d)\\s*\\.\\s*(\\d{1,2})(?:\\s*\\.\\s*(\\d{1,2}))?\\s*\\.?\\s*([\\u4e00-\\u9fff][^　]*)$");
  /**
   * 编号特征（"2. 6"/"2.6"）：粘连候选的尾部再含它即为枚举/说明段而非标题。
   * 坑：炭素成型工每等级开头的选考说明"…工作内容 2. 6 及 2. 7"曾被当标题，按章节序号兜底
   * 推断成一级(levelCode=4)，同一等级文档重复三份，persistTree 预分配 ID 撞主键 500。
   */
  private static final Pattern NUMBERING_TAIL = Pattern.compile("\\d\\s*\\.\\s*\\d");
  /** 表格单元格内的点号编号项，如 1.1.1 / 2.1（可能带空格：1. 1. 1） */
  private static final Pattern DOTTED_ITEM = Pattern.compile(
      "(\\d{1,2}(?:\\s*\\.\\s*\\d{1,2}){1,2})\\s*\\.?\\s*");
  /** （1）/ (1) 编号项 */
  private static final Pattern PAREN_ITEM = Pattern.compile("[（(]\\s*(\\d{1,2})\\s*[)）]\\s*");
  /** 1) / 1）编号项（仅右括号） */
  private static final Pattern CLOSE_PAREN_ITEM = Pattern.compile("(?<![\\d.])\\s*(\\d{1,2})[)）]\\s*");
  /** 阿拉伯数字顿号编号 1、xxx */
  private static final Pattern DUNHAO_ITEM = Pattern.compile("(?<!\\d)(\\d{1,2})、");
  /**
   * 单级编号项：1. / 1、/ 1 . 后接正文。PDF 转 docx 的标准里编号常被拆成「数字 + 空格 + 点号 + 空格」，
   * DOTTED_ITEM（要求两级以上）匹配不到，需单独兜底。前后都加数字/点号排除，避开 0.5 这类小数。
   */
  private static final Pattern SINGLE_ITEM = Pattern.compile(
      "(?<![\\d.．])(\\d{1,2})\\s*[.．、]\\s*(?=[\\u4e00-\\u9fffA-Za-z（(])");
  /** 中文/中文标点之间的空白（用先行断言，避免非重叠匹配漏掉相邻段；含全角括号，名称常写成「物料制备  （备注）」） */
  private static final Pattern CJK_SPACE = Pattern.compile(
      "([\\u4e00-\\u9fff，。、；：！？,.;:!?（）()])[ \\t]+(?=[\\u4e00-\\u9fff，。、；：！？,.;:!?（）()])");
  /**
   * 前导枚举编号：一、/（一）/1./1、/（1）/(1)/1（空格分隔）/1. 1/1业前准备（编号与标题无分隔符），
   * 用于考评点名称去序号。分隔符后禁止再跟数字，避免把「0.5毫米」这类小数当成编号剥掉；
   * 数字后必须跟分隔符、空白或直接跟汉字（与 normalizeMatchKey 的口径一致），避免剥掉「5S管理」。
   */
  private static final Pattern ENUM_PREFIX = Pattern.compile(
      "^(?:[（(]\\s*(?:[一二三四五六七八九十]{1,3}|\\d{1,2})\\s*[)）]"
          + "|[一二三四五六七八九十]{1,3}\\s*[、.．]"
          + "|\\d{1,2}(?:\\s*[.．]\\s*\\d{1,2}){0,2}\\s*(?:[.．、](?![\\d.．])|\\s+)"
          + "|\\d{1,2}(?=[\\u4e00-\\u9fff])"
          + ")\\s*");
  /** 技能条目安全标记 ★ */
  private static final Pattern STAR = Pattern.compile("[★☆]");

  /** 清洗段落文本：去职业编码碎片、压缩空白（不动结构换行由调用方控制） */
  public static String cleanParagraph(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw.replace('\u00a0', ' ').replace('\u3000', ' ')
        .replace('︵', '（').replace('︶', '）');
    s = SERIES_NO_JUNK.matcher(s).replaceAll(" ");
    // 中文/中文标点之间的空格（坑2：只清空格与Tab，不动换行）。
    // 循环收敛：replaceAll 为非重叠匹配，共享字符的相邻段（"工  作  准"）一轮只吃得掉一段，
    // 否则会残留 "工作  准备" 这类半个空格。先行断言保证只消费左侧字符。
    String prev;
    do {
      prev = s;
      s = CJK_SPACE.matcher(s).replaceAll("$1");
    } while (!s.equals(prev));
    s = s.replaceAll("([（(])[ \\t]+", "$1");
    s = s.replaceAll("[ \\t]+([）)])", "$1");
    return s.trim();
  }

  /** 是否页码等纯数字垃圾行 */
  public static boolean isPageJunk(String cleanedText) {
    return cleanedText.isEmpty() || PAGE_NO_LINE.matcher(cleanedText).matches();
  }

  /**
   * 标题检测。返回 int[]{depth, number1, number2, number3} + title 的 Heading 对象；非标题返回 null。
   * depth = 编号段数（1/2/3）。
   */
  public static Heading detectHeading(String cleanedText) {
    if (cleanedText == null || cleanedText.isEmpty()) {
      return null;
    }
    String text = cleanedText.trim();
    if (text.startsWith("（") || text.startsWith("(")) {
      return null;
    }
    if (text.equals("续表") || text.equals("∗") || text.equals("*") || text.startsWith("注:") || text.startsWith("注：")) {
      return null;
    }
    // 直接匹配
    Heading h = matchHeading(text);
    if (h != null) {
      return h;
    }
    // 页眉/页码垃圾与标题粘连（如 "105105职业编码：…4. 1 理论知识权重表"）：取最后一个 plausible 匹配
    Matcher m = HEADING_SEARCH.matcher(text);
    Heading last = null;
    int lastStart = -1;
    while (m.find()) {
      int n1 = Integer.parseInt(m.group(1));
      int n2 = Integer.parseInt(m.group(2));
      if (n1 >= 1 && n1 <= 9 && n2 <= 30) {
        int n3 = m.group(3) != null ? Integer.parseInt(m.group(3)) : -1;
        if (n3 > 30) {
          continue;
        }
        String tail = m.group(4).trim();
        // 尾部再含编号特征 = 枚举/说明段（如选考说明"…工作内容 2. 6 及 2. 7"），不是标题
        if (NUMBERING_TAIL.matcher(tail).find()) {
          continue;
        }
        last = new Heading(n3 >= 0 ? 3 : 2, n1, n2, n3, tail);
        lastStart = m.start();
      }
    }
    // 粘连匹配要求标题前存在垃圾（否则直接匹配已成功）
    return lastStart > 0 ? last : null;
  }

  private static Heading matchHeading(String text) {
    String cleaned = text.replaceAll("\\s+", "");
    Matcher m = HEADING.matcher(cleaned);
    if (!m.matches()) {
      return null;
    }
    int n1 = Integer.parseInt(m.group(1));
    if (n1 < 1 || n1 > 9) {
      return null; // "60分（含）以上者为合格" 之类
    }
    int n2 = m.group(2) != null ? Integer.parseInt(m.group(2)) : -1;
    int n3 = m.group(3) != null ? Integer.parseInt(m.group(3)) : -1;
    if (n2 > 30 || n3 > 30) {
      return null;
    }
    int depth = n3 >= 0 ? 3 : (n2 >= 0 ? 2 : 1);
    return new Heading(depth, n1, n2, n3, m.group(4).trim());
  }

  /**
   * 拆分单元格/段落文本为编号素材条目。
   * 支持 `1.1.1 xxx`、`（1）xxx`、`(1)`、`1) xxx`、`1、xxx` 等写法；无编号时整体作为单条（非空）。
   */
  public static List<String> splitItems(String rawText) {
    List<String> items = new ArrayList<>();
    if (rawText == null || rawText.isBlank()) {
      return items;
    }
    String text = rawText.replace('\r', '\n');
    // 段落换行先并成一行，再按编号切（编号项内部一般无换行；跨页断行在逻辑网格层已拼接）
    String flat = text.replaceAll("\\s*\\n\\s*", " ").trim();
    Matcher dotted = DOTTED_ITEM.matcher(flat);
    List<int[]> anchors = new ArrayList<>();
    while (dotted.find()) {
      anchors.add(new int[]{dotted.start(), dotted.end()});
    }
    if (anchors.isEmpty()) {
      Matcher paren = PAREN_ITEM.matcher(flat);
      while (paren.find()) {
        anchors.add(new int[]{paren.start(), paren.end()});
      }
    }
    if (anchors.isEmpty()) {
      Matcher closeParen = CLOSE_PAREN_ITEM.matcher(flat);
      while (closeParen.find()) {
        anchors.add(new int[]{closeParen.start(), closeParen.end()});
      }
    }
    if (anchors.isEmpty()) {
      Matcher dunhao = DUNHAO_ITEM.matcher(flat);
      while (dunhao.find()) {
        anchors.add(new int[]{dunhao.start(), dunhao.end()});
      }
    }
    if (anchors.isEmpty()) {
      Matcher single = SINGLE_ITEM.matcher(flat);
      while (single.find()) {
        anchors.add(new int[]{single.start(), single.end()});
      }
    }
    if (anchors.isEmpty()) {
      String single = STAR.matcher(flat).replaceAll("").trim();
      if (!single.isEmpty()) {
        items.add(single);
      }
      return items;
    }
    for (int i = 0; i < anchors.size(); i++) {
      int start = anchors.get(i)[1];
      int end = i + 1 < anchors.size() ? anchors.get(i + 1)[0] : flat.length();
      String item = flat.substring(start, end).trim();
      item = STAR.matcher(item).replaceAll("").trim();
      if (!item.isEmpty()) {
        items.add(item);
      }
    }
    return items;
  }

  /**
   * 表格单元格原文按编号条目重排为逐行文本，供「相关知识/技能要求」存储与导出逐行显示。
   *
   * <p>拆分锚点优先级与 {@link #splitItems} 一致（点号编号 &gt; （1） &gt; 1） &gt; 1、 &gt; 单级 1.）；
   * 分层编号（1.1.1）保留原编号原文，平铺编号（1. / （1） / 1） / 1、）统一改写为「n.正文」并从 1 重新编号；
   * 已含换行的文本原样返回；无锚点的单行文本原样返回。
   */
  public static String relineItems(String rawText) {
    if (rawText == null || rawText.isBlank()) {
      return "";
    }
    if (rawText.indexOf('\n') >= 0) {
      return rawText;
    }
    String flat = rawText.replace('\r', ' ').trim();
    List<int[]> anchors = new ArrayList<>();
    boolean hierarchical = false;
    Matcher dotted = DOTTED_ITEM.matcher(flat);
    while (dotted.find()) {
      hierarchical = true;
      anchors.add(new int[]{dotted.start(), dotted.end()});
    }
    if (anchors.isEmpty()) {
      for (Pattern pattern : new Pattern[]{PAREN_ITEM, CLOSE_PAREN_ITEM, DUNHAO_ITEM, SINGLE_ITEM}) {
        Matcher m = pattern.matcher(flat);
        while (m.find()) {
          anchors.add(new int[]{m.start(), m.end()});
        }
        if (!anchors.isEmpty()) {
          break;
        }
      }
    }
    if (anchors.isEmpty()) {
      // 单个无编号条目：按细目表版式统一补「1.」（"—" 占位符/空文本保持原样）
      return flat.matches("[—－\\-–\\s]*") ? flat : "1." + flat;
    }
    List<String> lines = new ArrayList<>();
    String lead = flat.substring(0, anchors.get(0)[0]).trim();
    if (!lead.isEmpty()) {
      lines.add(lead);
    }
    int seq = 0;
    for (int i = 0; i < anchors.size(); i++) {
      int start = anchors.get(i)[0];
      int end = i + 1 < anchors.size() ? anchors.get(i + 1)[0] : flat.length();
      if (hierarchical) {
        String line = flat.substring(start, end).trim();
        if (!line.isEmpty()) {
          lines.add(line);
        }
      } else {
        String text = flat.substring(anchors.get(i)[1], end).trim();
        if (!text.isEmpty()) {
          seq++;
          lines.add(seq + "." + text);
        }
      }
    }
    return String.join("\n", lines);
  }

  /** 点号编号归一化："1. 1. 1" → "1.1.1"；编号后须有点/顿号/空格分隔，无匹配返回 null */
  public static String normalizeDottedNumber(String text) {
    if (text == null) {
      return null;
    }
    Matcher m = Pattern.compile("^(\\d{1,2}(?:\\s*\\.\\s*\\d{1,2}){0,2})(?:\\s*[.、]|\\s+)")
        .matcher(text.trim());
    if (!m.find()) {
      return null;
    }
    return m.group(1).replaceAll("\\s*", "").replaceAll("\\.$", "");
  }

  /** 剥离标题前的点号编号："1. 1 安全检查" → "安全检查"；编号后须有点/顿号/空格分隔，否则不剥 */
  public static String stripLeadingNumber(String text) {
    if (text == null) {
      return "";
    }
    return text.trim()
        .replaceAll("^\\d{1,2}(?:\\s*\\.\\s*\\d{1,2}){0,2}(?:\\s*[.、]|\\s+)\\s*", "")
        .trim();
  }

  /**
   * 考评点名称去枚举序号（一、/（一）/1./1、/（1）），用于树展示与细目表导出名称列。
   *
   * <p>只剥前导序号，不动名称本身；剥空（整个名字就是序号）时回退原文，避免名字变空。
   */
  public static String stripEnumerationPrefix(String text) {
    if (text == null) {
      return "";
    }
    String trimmed = text.trim();
    String stripped = ENUM_PREFIX.matcher(trimmed).replaceFirst("").trim();
    return stripped.isEmpty() ? trimmed : stripped;
  }

  /** 权重表项目名归一化：去空格、去（必选）/（必选）后缀 */
  public static String normalizeItemName(String name) {
    if (name == null) {
      return "";
    }
    String s = name.replaceAll("[\\s　]+", "");
    s = s.replaceAll("[（(](必选|任选)[)）]$", "");
    return s;
  }

  /**
   * 权重表匹配的宽松归一化（仅作 findRow 兜底，不改变展示标题）：
   * 在 normalizeItemName 基础上统一 丶→、，剥离前导枚举编号与尾部括注。
   * 兼容职业功能编号无分隔符直写（"1业前准备"）与中文数字编号（"五、安全操作"）的标准，
   * 以及树标题带尾注而权重表条目不带（"物料制备︵工作内容三至五为可选︶" vs "物料制备"）的场景。
   */
  public static String normalizeMatchKey(String name) {
    String s = normalizeItemName(name).replace('丶', '、');
    // 前导枚举编号：阿拉伯数字（可带点号层级）或中文数字，后接分隔符或直接跟汉字
    s = s.replaceFirst(
        "^(?:\\d{1,2}(?:\\.\\d{1,2}){0,2}|[一二三四五六七八九十]{1,3})(?:[.、．]|(?=[\\u4e00-\\u9fff]))",
        "");
    // 尾部括注（含全角 ︵︶ 变体）
    s = s.replaceFirst("[（(︵][^（()）︵︶]*[)）︶]$", "");
    return s;
  }

  /** 表格单元格文本：多段落以 \n 拼接后整体清洗（保留段落间换行） */
  public static String cleanCell(String rawText) {
    if (rawText == null) {
      return "";
    }
    String[] parts = rawText.replace('\r', '\n').split("\\n");
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      String c = cleanParagraph(p);
      if (!c.isEmpty()) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * 表格原文的编号规范化（细目表"技能要求/相关知识"列版式对齐样例）：
   * "1. 1. 1 能…" → "1.1.1能…"（折叠编号内部空格、去编号与正文间空格）。
   */
  public static String normalizeNumbering(String text) {
    if (text == null) {
      return "";
    }
    String s = text;
    String prev;
    do {
      prev = s;
      // 必须有空格邻接点才折叠（"1. 1"/"1 .1"），避免 "1.1" 自匹配阻断链式折叠
      s = s.replaceAll("(\\d)\\s*\\.\\s+(\\d)", "$1.$2");
      s = s.replaceAll("(\\d)\\s+\\.\\s*(\\d)", "$1.$2");
    } while (!s.equals(prev));
    // 点号编号与中文正文之间的空格（"1.1.1 能" → "1.1.1能"）
    s = s.replaceAll("(\\d{1,2}(?:\\.\\d{1,2})+)[ \\t]+(?=[\\u4e00-\\u9fff])", "$1");
    return s;
  }

  /** 标题检测结果 */
  public static final class Heading {
    public final int depth;
    public final int n1;
    public final int n2;
    public final int n3;
    public final String title;

    public Heading(int depth, int n1, int n2, int n3, String title) {
      this.depth = depth;
      this.n1 = n1;
      this.n2 = n2;
      this.n3 = n3;
      this.title = title;
    }

    public String number() {
      if (depth == 1) {
        return String.valueOf(n1);
      }
      if (depth == 2) {
        return n1 + "." + n2;
      }
      return n1 + "." + n2 + "." + n3;
    }
  }
}
