package com.guoyu.platform.trainingdepartment.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.StringUtils;

/**
 * 人社职业技能等级证书 PDF 文本层解析（证面原文，宁缺毋错，禁止臆造补全）。
 */
@Slf4j
public final class RensheSkillCertPdfParser {

  private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{17}[\\dXx])(?!\\d)");
  private static final Pattern CERT_NO = Pattern.compile("(S\\d{18,})");
  private static final Pattern ISSUE_DATE =
      Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");
  private static final Pattern ISSUING =
      Pattern.compile("本证书由\\s*(.+?)\\s*颁发");
  private static final Pattern SEPARATOR = Pattern.compile("^[_—\\-\\s]{6,}$");
  private static final Pattern URL_LINE =
      Pattern.compile("(?i)(https?://\\S+|\\S*osta\\.org\\S*|\\S*gxrcosta\\S*)");

  private static final String[] LEVEL_STANDARDS = {
      "五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"
  };

  private RensheSkillCertPdfParser() {}

  public static Parsed parse(byte[] pdfBytes, String fileName) {
    String text = "";
    if (pdfBytes != null && pdfBytes.length > 0) {
      try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
        text = extractFirstPageText(doc);
      } catch (Exception ex) {
        log.debug("PDF 文本抽取失败 {}", fileName, ex);
      }
    }
    return parseText(text, fileName);
  }

  /** 仅抽取首页文本（证书关键字段在首页，可显著提速）。 */
  public static String extractFirstPageText(PDDocument doc) throws Exception {
    if (doc == null || doc.getNumberOfPages() < 1) {
      return "";
    }
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setStartPage(1);
    stripper.setEndPage(1);
    return stripper.getText(doc);
  }

  /** 供单测：直接喂入已抽取文本。 */
  public static Parsed parseText(String text, String fileName) {
    Parsed r = new Parsed();
    List<String> warnings = new ArrayList<>();

    FileNameHint fn = parseFileName(fileName);
    if (StringUtils.hasText(fn.name)) {
      r.name = fn.name;
    }

    String raw = text == null ? "" : text;
    List<String> lines = splitLines(raw);

    Matcher idm = ID_CARD.matcher(raw);
    if (idm.find()) {
      r.idCard = idm.group(1).toUpperCase(Locale.ROOT);
    }
    if (!StringUtils.hasText(r.idCard) && StringUtils.hasText(fn.idCard)) {
      r.idCard = fn.idCard;
    }

    Matcher cm = CERT_NO.matcher(raw);
    if (cm.find()) {
      r.certificateNo = cm.group(1);
    }
    if (!StringUtils.hasText(r.certificateNo) && StringUtils.hasText(fn.certNo)) {
      r.certificateNo = fn.certNo;
    }
    if (StringUtils.hasText(r.certificateNo)
        && StringUtils.hasText(fn.certNo)
        && !r.certificateNo.equals(fn.certNo)) {
      warnings.add("正文证号与文件名不一致，已采用正文证号");
    }

    for (String line : lines) {
      if (line.contains("身份证") && line.length() <= 16) {
        r.idDocType = line;
        break;
      }
    }

    Matcher im = ISSUING.matcher(raw.replace('\n', ' '));
    if (im.find()) {
      r.issuingAuthority = collapseSpaces(im.group(1));
    }

    Matcher dm = ISSUE_DATE.matcher(raw);
    if (dm.find()) {
      int y = Integer.parseInt(dm.group(1));
      int mo = Integer.parseInt(dm.group(2));
      int d = Integer.parseInt(dm.group(3));
      r.issueDate = String.format(Locale.ROOT, "%04d-%02d-%02d", y, mo, d);
      int dateLineIdx = findDateLineIndex(lines);
      if (dateLineIdx > 0) {
        for (int i = dateLineIdx - 1; i >= 0; i--) {
          String prev = lines.get(i);
          if (!StringUtils.hasText(prev)) {
            continue;
          }
          if (looksLikeUrl(prev) || looksLikeLevel(prev) || looksLikeIdCard(prev) || looksLikeCertNo(prev)) {
            continue;
          }
          if (prev.contains("查询") || prev.contains("本证书由") || prev.contains("表明持证人")) {
            continue;
          }
          r.evaluationOrg = prev;
          break;
        }
      }
    }

    extractOccupationWorkTypeLevel(lines, r, warnings);
    r.queryUrls = extractQueryUrls(lines);
    r.levelNormalized = normalizeLevel(r.level);

    if (!StringUtils.hasText(r.name)) {
      for (String line : lines) {
        if (looksLikePersonName(line)) {
          r.name = line;
          break;
        }
      }
    } else {
      for (String line : lines) {
        if (looksLikePersonName(line) && !line.equals(r.name)) {
          // 文首姓名与文件名不一致时以文首为准
          if (lines.indexOf(line) <= 2) {
            warnings.add("文件名姓名与正文不一致，已采用正文姓名");
            r.name = line;
          }
          break;
        }
      }
      // prefer first line if it looks like name
      if (!lines.isEmpty() && looksLikePersonName(lines.get(0))) {
        if (!lines.get(0).equals(r.name)) {
          warnings.add("文件名姓名与正文不一致，已采用正文姓名");
        }
        r.name = lines.get(0);
      }
    }

    if (StringUtils.hasText(r.level) && !r.level.contains("/") && (r.level.contains("级") || r.level.contains("技师"))) {
      warnings.add("等级非完整「级数/称谓」格式，已按证面原文入库");
    }
    if (!StringUtils.hasText(r.occupation)) {
      warnings.add("未识别职业名称");
    }
    if (!StringUtils.hasText(r.workType)) {
      warnings.add("未识别工种名称");
    }
    if (!StringUtils.hasText(r.level)) {
      warnings.add("未识别职业技能等级");
    }
    if (!StringUtils.hasText(r.issuingAuthority)) {
      warnings.add("未识别发证机构");
    }
    if (!StringUtils.hasText(r.evaluationOrg)) {
      warnings.add("未识别评价机构");
    }
    if (!StringUtils.hasText(r.issueDate)) {
      warnings.add("未识别核发日期");
    }
    if (r.queryUrls == null || r.queryUrls.isEmpty()) {
      warnings.add("未识别查询地址");
    }

    r.warning = joinWarnings(warnings);
    return r;
  }

  /** 硬字段缺失时建议走 OCR。 */
  public static boolean needsOcr(Parsed p) {
    return p == null
        || !StringUtils.hasText(p.idCard)
        || !StringUtils.hasText(p.certificateNo);
  }

  /**
   * 报表用五档归一：仅映射已知证面写法；未知则 null（不臆造进 level）。
   */
  public static String normalizeLevel(String faceLevel) {
    if (!StringUtils.hasText(faceLevel)) {
      return null;
    }
    String v = faceLevel.trim();
    for (String s : LEVEL_STANDARDS) {
      if (s.equals(v)) {
        return s;
      }
    }
    if (v.contains("高级技师") || (v.contains("一级") && v.contains("技师"))) {
      return "一级/高级技师";
    }
    if (v.contains("二级") || (v.equals("技师"))) {
      return "二级/技师";
    }
    if (v.contains("三级") || v.contains("高级工")) {
      return "三级/高级工";
    }
    if (v.contains("四级") || v.contains("中级工")) {
      return "四级/中级工";
    }
    if (v.contains("五级") || v.contains("初级工")) {
      return "五级/初级工";
    }
    return null;
  }

  private static java.util.List<String> extractQueryUrls(List<String> lines) {
    List<String> urls = new ArrayList<>();
    for (String line : lines) {
      if (!StringUtils.hasText(line)) {
        continue;
      }
      if (URL_LINE.matcher(line).find()) {
        // 证面原文整行（含 (国家)/(广西) 标注）
        if (!urls.contains(line)) {
          urls.add(line);
        }
      }
    }
    return urls;
  }

  private static void extractOccupationWorkTypeLevel(
      List<String> lines, Parsed r, List<String> warnings) {
    int sep = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (SEPARATOR.matcher(lines.get(i)).matches() || lines.get(i).contains("____")) {
        sep = i;
        break;
      }
    }
    List<String> block = new ArrayList<>();
    if (sep >= 0) {
      for (int i = sep + 1; i < lines.size(); i++) {
        String line = lines.get(i);
        if (!StringUtils.hasText(line)) {
          continue;
        }
        if (ISSUE_DATE.matcher(line).find() || looksLikeUrl(line)) {
          break;
        }
        if (line.contains("查询")) {
          break;
        }
        block.add(line);
        if (block.size() >= 4) {
          break;
        }
      }
    }

    // block: occupation, workType, level[, evaluationOrg if date not yet found]
    if (block.size() >= 3) {
      // find level line inside block
      int levelIdx = -1;
      for (int i = 0; i < block.size(); i++) {
        if (looksLikeLevel(block.get(i))) {
          levelIdx = i;
          break;
        }
      }
      if (levelIdx >= 2) {
        r.occupation = block.get(levelIdx - 2);
        r.workType = block.get(levelIdx - 1);
        r.level = block.get(levelIdx);
      } else if (levelIdx == 1 && block.size() >= 2) {
        r.occupation = block.get(0);
        r.workType = block.get(0);
        r.level = block.get(1);
        warnings.add("职业/工种行不足，已按证面可见行入库");
      } else if (block.size() >= 3) {
        r.occupation = block.get(0);
        r.workType = block.get(1);
        r.level = block.get(2);
      }
    } else {
      // fallback: scan any level line and take two previous content lines
      for (int i = 0; i < lines.size(); i++) {
        if (!looksLikeLevel(lines.get(i))) {
          continue;
        }
        r.level = lines.get(i);
        List<String> prev = new ArrayList<>();
        for (int j = i - 1; j >= 0 && prev.size() < 2; j--) {
          String p = lines.get(j);
          if (!StringUtils.hasText(p) || SEPARATOR.matcher(p).matches() || p.contains("____")) {
            break;
          }
          if (looksLikeUrl(p) || looksLikeIdCard(p) || looksLikeCertNo(p) || p.contains("本证书由")) {
            break;
          }
          prev.add(0, p);
        }
        if (prev.size() == 2) {
          r.occupation = prev.get(0);
          r.workType = prev.get(1);
        }
        break;
      }
    }
  }

  private static int findDateLineIndex(List<String> lines) {
    for (int i = 0; i < lines.size(); i++) {
      if (ISSUE_DATE.matcher(lines.get(i)).find()) {
        return i;
      }
    }
    return -1;
  }

  private static List<String> splitLines(String text) {
    List<String> out = new ArrayList<>();
    for (String line : text.split("\\R")) {
      String t = collapseSpaces(line);
      if (StringUtils.hasText(t)) {
        out.add(t);
      }
    }
    return out;
  }

  private static String collapseSpaces(String s) {
    if (s == null) {
      return "";
    }
    return s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
  }

  private static boolean looksLikeLevel(String line) {
    if (!StringUtils.hasText(line) || line.length() > 24) {
      return false;
    }
    if (line.contains("/")) {
      return line.contains("级") || line.contains("技师") || line.contains("初级") || line.contains("中级")
          || line.contains("高级");
    }
    return line.matches(".*[一二三四五]级.*") || "技师".equals(line) || "高级技师".equals(line)
        || "初级工".equals(line) || "中级工".equals(line) || "高级工".equals(line);
  }

  private static boolean looksLikeUrl(String line) {
    return line.contains("http://") || line.contains("https://") || line.contains("osta.org")
        || line.contains("gxrcosta");
  }

  private static boolean looksLikeIdCard(String line) {
    return ID_CARD.matcher(line).find();
  }

  private static boolean looksLikeCertNo(String line) {
    return CERT_NO.matcher(line).find();
  }

  private static boolean looksLikePersonName(String line) {
    if (!StringUtils.hasText(line) || line.length() > 8 || line.length() < 2) {
      return false;
    }
    if (line.contains("身份证") || line.contains("证书") || line.contains("查询") || line.contains("http")) {
      return false;
    }
    if (looksLikeIdCard(line) || looksLikeCertNo(line) || looksLikeLevel(line)) {
      return false;
    }
    return line.matches("[\\u4e00-\\u9fa5·]{2,8}");
  }

  private static FileNameHint parseFileName(String fileName) {
    FileNameHint h = new FileNameHint();
    if (!StringUtils.hasText(fileName)) {
      return h;
    }
    String base = fileName;
    int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
    if (slash >= 0) {
      base = fileName.substring(slash + 1);
    }
    if (base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      base = base.substring(0, base.length() - 4);
    }
    String[] parts = base.split("_");
    if (parts.length >= 1 && parts[0].startsWith("S")) {
      h.certNo = parts[0].trim();
    }
    if (parts.length >= 2) {
      h.name = parts[1].trim();
    }
    Matcher idInName = ID_CARD.matcher(base);
    if (idInName.find()) {
      h.idCard = idInName.group(1).toUpperCase(Locale.ROOT);
    }
    return h;
  }

  private static String joinWarnings(List<String> warnings) {
    if (warnings == null || warnings.isEmpty()) {
      return null;
    }
    // soft-field "未识别" warnings are noisy when hard fields ok — keep all for preview
    return String.join("；", warnings);
  }

  private static final class FileNameHint {
    String certNo;
    String name;
    String idCard;
  }

  public static final class Parsed {
    public String idCard;
    public String certificateNo;
    public String name;
    public String idDocType;
    public String issuingAuthority;
    public String occupation;
    public String workType;
    public String level;
    /** 报表用五档；与 level 证面原文分离 */
    public String levelNormalized;
    public String evaluationOrg;
    public String issueDate;
    /** 证面查询地址原文行 */
    public java.util.List<String> queryUrls;
    public boolean ocrUsed;
    public String warning;
  }
}
