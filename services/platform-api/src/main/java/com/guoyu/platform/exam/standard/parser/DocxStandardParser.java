package com.guoyu.platform.exam.standard.parser;

import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

/**
 * 国家职业技能标准 docx → 结构化骨架（纯规则，黄金样例可单测）。
 *
 * <p>解析步骤见方案 §5：编号段落标题识别、基本要求（2.x）段落解析、工作要求 4 列表格解析
 * （vMerge/gridSpan 逻辑网格）、末尾双权重表解析（含焊工方向矩阵与跨页续表合并）。
 */
public class DocxStandardParser {

  private static final Pattern SERIES_NO_VALUE = Pattern.compile("职业编码[:：]\\s*([\\d\\s\\-—]+)");
  private static final Pattern VERSION_YEAR = Pattern.compile("[（(]\\s*(\\d{4})\\s*年版\\s*[)）]");
  private static final Pattern LEVEL_WORD = Pattern.compile("[五四三二一]");
  private static final Pattern DIRECTION_LEVEL_LINE =
      Pattern.compile("^(?<direction>.+?)工种分别为[:：]\\s*(?<levels>.+)$");
  private static final Pattern NUMERIC_CELL = Pattern.compile("^\\d{1,3}(\\.\\d+)?$");
  private static final String[] LEVEL_NAMES = {"五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"};

  public ParsedStandard parse(InputStream in) throws IOException {
    ParsedStandard result = new ParsedStandard();
    try (XWPFDocument doc = new XWPFDocument(in)) {
      ParserContext ctx = new ParserContext(result);
      for (IBodyElement element : doc.getBodyElements()) {
        if (element instanceof XWPFParagraph paragraph) {
          handleParagraph(paragraph, ctx);
        } else if (element instanceof XWPFTable table) {
          handleTable(table, ctx);
        }
      }
      finalizeParse(ctx);
    }
    return result;
  }

  // ============================================================
  // 段落处理
  // ============================================================

  private void handleParagraph(XWPFParagraph paragraph, ParserContext ctx) {
    String raw = paragraph.getText();
    if (raw == null) {
      return;
    }
    // 元信息（在清洗前的原文上取）
    captureMeta(raw, ctx);
    String text = DocxTextUtil.cleanParagraph(raw);
    if (DocxTextUtil.isPageJunk(text)) {
      return;
    }
    DocxTextUtil.Heading heading = DocxTextUtil.detectHeading(text);
    if (heading != null) {
      handleHeading(heading, ctx);
      return;
    }
    // 职业概况节：1.1 职业名称 → 紧随其后的普通段落即名称
    if (ctx.mainSection == 1 && ctx.expectSectionOneValue) {
      ctx.expectSectionOneValue = false;
      if (ctx.sectionOneSub == 1
          && (ctx.result.getWorkTypeTitle() == null || ctx.result.getWorkTypeTitle().isBlank())) {
        // 剥离页脚注引用标记（如 "焊工①"）
        ctx.result.setWorkTypeTitle(text.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩]", "").trim());
      }
      if (ctx.sectionOneSub == 2 && ctx.result.getSeriesNo() == null) {
        String no = text.replaceAll("[\\s　—]+", "").replace("－", "-");
        if (no.matches("\\d+(-\\d+)+")) {
          ctx.result.setSeriesNo(no);
        }
      }
      // 1.4 职业技能等级：连续多段（每工种一行），保持 expect 继续收集
      if (ctx.sectionOneSub == 4 && !text.isBlank()) {
        ctx.expectSectionOneValue = true;
        ctx.sectionOneFourParagraphs.add(text);
      }
      return;
    }
    // 普通段落：基本要求节内的（n）列举项
    if (ctx.mainSection == 2 && ctx.currentBasicL2 != null) {
      List<String> items = DocxTextUtil.splitItems(text);
      if (!items.isEmpty() && startsWithParenNumber(text)) {
        ctx.currentBasicL2.getTheoryMaterials().addAll(items);
      }
    }
  }

  private boolean startsWithParenNumber(String text) {
    return text.startsWith("（") || text.startsWith("(");
  }

  private void captureMeta(String raw, ParserContext ctx) {
    if (ctx.result.getSeriesNo() == null) {
      Matcher m = SERIES_NO_VALUE.matcher(raw);
      if (m.find()) {
        String no = m.group(1).replaceAll("[\\s　—]+", "").replace("－", "-");
        no = no.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        if (no.matches("\\d+(-\\d+)+")) {
          ctx.result.setSeriesNo(no);
        }
      }
    }
    if (ctx.result.getVersion() == null) {
      Matcher m = VERSION_YEAR.matcher(raw);
      if (m.find()) {
        ctx.result.setVersion(m.group(1));
      }
    }
  }

  private void handleHeading(DocxTextUtil.Heading h, ParserContext ctx) {
    if (h.depth == 1) {
      ctx.mainSection = h.n1;
      ctx.currentBasicL1 = null;
      ctx.currentBasicL2 = null;
      ctx.currentLevel = null;
      ctx.weightSection = null;
      return;
    }
    if (ctx.mainSection == 1) {
      // 1.1 职业名称 / 1.2 职业编码 → 由 captureSectionOneValue 处理
      ctx.sectionOneSub = h.n2;
      ctx.expectSectionOneValue = true;
      return;
    }
    if (ctx.mainSection == 2 && h.depth == 2) {
      StandardNodeDoc l1 = new StandardNodeDoc();
      l1.setLevel(1);
      l1.setNumber(h.number());
      l1.setTitle(h.title);
      ctx.result.getBasicSections().add(l1);
      ctx.currentBasicL1 = l1;
      ctx.currentBasicL2 = null;
      return;
    }
    if (ctx.mainSection == 2 && h.depth == 3) {
      StandardNodeDoc l2 = new StandardNodeDoc();
      l2.setLevel(2);
      l2.setNumber(h.number());
      l2.setTitle(h.title);
      if (ctx.currentBasicL1 != null) {
        ctx.currentBasicL1.getChildren().add(l2);
      } else {
        ctx.result.getParseWarnings().add("WARN: 基本要求三级标题无上级 L1：" + h.number() + " " + h.title);
      }
      ctx.currentBasicL2 = l2;
      return;
    }
    if (ctx.mainSection == 3 && h.depth == 2) {
      StandardLevelDoc level = new StandardLevelDoc();
      int levelCode = resolveLevelCode(h.title, h.n2, ctx.result.getParseWarnings());
      level.setLevelCode(levelCode);
      level.setLevelName(LEVEL_NAMES[levelCode]);
      ctx.result.getLevels().add(level);
      ctx.currentLevel = level;
      return;
    }
    if (ctx.mainSection == 4 && h.depth == 2) {
      if (h.title.contains("理论")) {
        ctx.weightSection = StandardWeightTable.Kind.THEORY;
      } else if (h.title.contains("技能")) {
        ctx.weightSection = StandardWeightTable.Kind.SKILL;
      }
      return;
    }
  }

  private int resolveLevelCode(String title, int h2, List<String> warnings) {
    Matcher m = LEVEL_WORD.matcher(title);
    if (m.find()) {
      return switch (m.group()) {
        case "五" -> 0;
        case "四" -> 1;
        case "三" -> 2;
        case "二" -> 3;
        case "一" -> 4;
        default -> 0;
      };
    }
    warnings.add("WARN: 等级标题未识别，按章节序号推断：" + title);
    return Math.min(Math.max(h2 - 1, 0), 4);
  }

  // ============================================================
  // 表格处理
  // ============================================================

  private void handleTable(XWPFTable table, ParserContext ctx) {
    List<List<String>> grid = readLogicalGrid(table, ctx.result.getParseWarnings());
    if (grid.isEmpty()) {
      return;
    }
    ctx.result.setTotalTableCount(ctx.result.getTotalTableCount() + 1);
    String firstCell = grid.get(0).isEmpty() ? "" : grid.get(0).get(0);
    if (firstCell.contains("职业功能")) {
      ctx.result.setWorkTableCount(ctx.result.getWorkTableCount() + 1);
      parseWorkTable(grid, ctx);
    } else if (containsHeader(grid.get(0), "技能等级项目")) {
      ctx.result.setWeightTableCount(ctx.result.getWeightTableCount() + 1);
      parseWeightTable(grid, ctx);
    }
    // 其他表格（鉴定要求等）忽略
  }

  private boolean containsHeader(List<String> row, String keyword) {
    for (String cell : row) {
      if (cell != null && cell.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 逻辑网格读取：gridSpan 横向展开、vMerge 纵向继承（restart 记录 / continue 继承）。
   * 悬空的 continue（上方无 restart）记解析警告。
   */
  private List<List<String>> readLogicalGrid(XWPFTable table, List<String> warnings) {
    List<XWPFTableRow> rows = table.getRows();
    List<List<String>> grid = new ArrayList<>();
    // 列数：各行物理单元格累计 span 的最大值（tblGrid 声明在部分文档中不可靠）
    int cols = 0;
    for (XWPFTableRow row : rows) {
      int cellCols = 0;
      for (XWPFTableCell cell : row.getTableCells()) {
        cellCols += gridSpanOf(cell);
      }
      cols = Math.max(cols, cellCols);
    }
    for (int r = 0; r < rows.size(); r++) {
      List<String> gridRow = new ArrayList<>();
      int c = 0;
      for (XWPFTableCell cell : rows.get(r).getTableCells()) {
        int span = gridSpanOf(cell);
        String text = DocxTextUtil.cleanCell(cellText(cell));
        String vmerge = vmergeOf(cell);
        if ("continue".equals(vmerge)) {
          if (r > 0 && c < grid.get(r - 1).size()) {
            // 继承上一行同列值
            for (int k = 0; k < span; k++) {
              int upCol = c + k;
              gridRow.add(upCol < grid.get(r - 1).size() ? grid.get(r - 1).get(upCol) : "");
            }
          } else {
            warnings.add("WARN: 悬空 vMerge continue（表第 " + (r + 1) + " 行第 " + (c + 1) + " 列）");
            for (int k = 0; k < span; k++) {
              gridRow.add("");
            }
          }
        } else {
          for (int k = 0; k < span; k++) {
            gridRow.add(text);
          }
        }
        c += span;
      }
      while (gridRow.size() < cols) {
        gridRow.add("");
      }
      grid.add(gridRow);
    }
    return grid;
  }

  private String cellText(XWPFTableCell cell) {
    // 单元格内多段落是窄列软换行（如"技能等级\n项目"），直接拼接不加分隔符；
    // 编号条目（1.1.1）由 splitItems 按编号正则切分，不依赖换行
    StringBuilder sb = new StringBuilder();
    for (XWPFParagraph p : cell.getParagraphs()) {
      String t = p.getText();
      if (t != null && !t.isBlank()) {
        sb.append(t);
      }
    }
    return sb.toString();
  }

  private int gridSpanOf(XWPFTableCell cell) {
    CTTcPr tcPr = cell.getCTTc().getTcPr();
    if (tcPr != null && tcPr.getGridSpan() != null) {
      return tcPr.getGridSpan().getVal().intValue();
    }
    return 1;
  }

  /** @return "restart" / "continue" / null */
  private String vmergeOf(XWPFTableCell cell) {
    CTTcPr tcPr = cell.getCTTc().getTcPr();
    if (tcPr == null) {
      return null;
    }
    CTVMerge vm = tcPr.getVMerge();
    if (vm == null) {
      return null;
    }
    if (vm.getVal() != null && vm.getVal() == STMerge.RESTART) {
      return "restart";
    }
    if (vm.getVal() != null && "restart".equals(vm.getVal().toString())) {
      return "restart";
    }
    return "continue";
  }

  // ============================================================
  // 工作要求表（4 列：职业功能/工作内容/技能要求/相关知识要求）
  // ============================================================

  private void parseWorkTable(List<List<String>> grid, ParserContext ctx) {
    if (ctx.currentLevel == null) {
      ctx.result.getParseWarnings().add("WARN: 工作要求表格出现在等级节之外，已忽略");
      return;
    }
    StandardNodeDoc currentFunc = null;
    for (int r = 1; r < grid.size(); r++) {
      List<String> row = grid.get(r);
      String func = cellAt(row, 0);
      String content = cellAt(row, 1);
      String skill = cellAt(row, 2);
      String knowledge = cellAt(row, 3);
      if (func.contains("职业功能") || content.contains("工作内容")) {
        continue; // 跨页重复表头
      }
      if (func.isBlank() && content.isBlank() && skill.isBlank() && knowledge.isBlank()) {
        continue;
      }
      if (!func.isBlank()) {
        String funcTitle = DocxTextUtil.stripLeadingNumber(func);
        StandardNodeDoc existing = findFunction(ctx.currentLevel, funcTitle);
        if (existing != null) {
          currentFunc = existing; // 跨页续表：同一职业功能重复出现
        } else {
          StandardNodeDoc l1 = new StandardNodeDoc();
          l1.setLevel(1);
          l1.setNumber(DocxTextUtil.normalizeDottedNumber(func));
          l1.setTitle(funcTitle);
          ctx.currentLevel.getWorkFunctions().add(l1);
          currentFunc = l1;
        }
      }
      if (currentFunc == null) {
        ctx.result.getParseWarnings().add("WARN: 工作内容行无职业功能归属：" + content);
        continue;
      }
      if (content.isBlank()) {
        continue;
      }
      StandardNodeDoc l2 = new StandardNodeDoc();
      l2.setLevel(2);
      l2.setNumber(DocxTextUtil.normalizeDottedNumber(content));
      l2.setTitle(DocxTextUtil.stripLeadingNumber(content));
      l2.setSkillRaw(DocxTextUtil.normalizeNumbering(skill));
      l2.setRelatedKnowledgeRaw(DocxTextUtil.normalizeNumbering(knowledge));
      l2.getSkillMaterials().addAll(DocxTextUtil.splitItems(skill));
      l2.getTheoryMaterials().addAll(DocxTextUtil.splitItems(knowledge));
      if (l2.getTheoryMaterials().isEmpty()) {
        l2.setNeedLlm(true);
      }
      currentFunc.getChildren().add(l2);
    }
  }

  private StandardNodeDoc findFunction(StandardLevelDoc level, String funcTitle) {
    String target = DocxTextUtil.normalizeItemName(funcTitle);
    for (StandardNodeDoc f : level.getWorkFunctions()) {
      if (DocxTextUtil.normalizeItemName(f.getTitle()).equals(target)) {
        return f;
      }
    }
    return null;
  }

  private String cellAt(List<String> row, int idx) {
    return idx < row.size() && row.get(idx) != null ? row.get(idx).trim() : "";
  }

  // ============================================================
  // 权重表（末尾 2 张，跨页续表按项目名合并）
  // ============================================================

  private void parseWeightTable(List<List<String>> grid, ParserContext ctx) {
    if (ctx.weightSection == null) {
      ctx.result.getParseWarnings().add("WARN: 权重表格出现在权重节之外，已忽略");
      return;
    }
    StandardWeightTable table = ctx.weightSection == StandardWeightTable.Kind.THEORY
        ? ctx.theoryBuilder : ctx.skillBuilder;

    int cols = grid.get(0).size();
    // 1) 方向行：前 4 行内，第 2 列起全部为不含数字/百分号/“级”的中文标签
    int directionRowIdx = -1;
    for (int r = 0; r < Math.min(4, grid.size()); r++) {
      List<String> row = grid.get(r);
      boolean directionRow = true;
      int labels = 0;
      for (int c = 2; c < cols; c++) {
        String cell = cellAt(row, c);
        if (cell.isEmpty()) {
          continue;
        }
        if (cell.matches(".*[\\d%（）()/级].*")) {
          directionRow = false;
          break;
        }
        labels++;
      }
      if (directionRow && labels >= 2) {
        directionRowIdx = r;
        break;
      }
    }

    // 2) 每逻辑列的等级与方向
    Map<Integer, Integer> colLevel = new LinkedHashMap<>();   // col -> levelCode
    Map<Integer, String> colDirection = new LinkedHashMap<>(); // col -> direction
    for (int c = 2; c < cols; c++) {
      StringBuilder headerText = new StringBuilder();
      int headerEnd = directionRowIdx >= 0 ? directionRowIdx : Math.min(4, grid.size());
      for (int r = 0; r < headerEnd; r++) {
        headerText.append(cellAt(grid.get(r), c));
      }
      Matcher m = LEVEL_WORD.matcher(headerText.toString());
      if (!m.find()) {
        continue;
      }
      int levelCode = switch (m.group()) {
        case "五" -> 0;
        case "四" -> 1;
        case "三" -> 2;
        case "二" -> 3;
        default -> 4;
      };
      colLevel.put(c, levelCode);
      String direction = directionRowIdx >= 0 ? cellAt(grid.get(directionRowIdx), c) : ParsedStandard.DEFAULT_DIRECTION;
      colDirection.put(c, direction.isBlank() ? ParsedStandard.DEFAULT_DIRECTION : direction);
      ctx.registerDirection(direction.isBlank() ? ParsedStandard.DEFAULT_DIRECTION : direction);
    }

    // 3) 数据行：项目名非空且至少一个数值/— 单元格
    int dataStart = directionRowIdx >= 0 ? directionRowIdx + 1 : 1;
    for (int r = dataStart; r < grid.size(); r++) {
      List<String> row = grid.get(r);
      String category = cellAt(row, 0);
      String item = cellAt(row, 1);
      if (category.contains("合计") || item.contains("合计")) {
        continue;
      }
      if (item.isBlank()) {
        continue;
      }
      boolean anyValue = false;
      for (int c = 2; c < cols; c++) {
        String v = cellAt(row, c);
        if (NUMERIC_CELL.matcher(v).matches() || v.contains("—") || v.contains("－")) {
          anyValue = true;
          break;
        }
      }
      if (!anyValue) {
        continue; // 表头碎片行
      }
      String normalizedItem = DocxTextUtil.normalizeItemName(item);
      StandardWeightRow weightRow = findOrCreateRow(table, normalizedItem, item,
          category.isBlank() ? null : category);
      for (Map.Entry<Integer, Integer> e : colLevel.entrySet()) {
        int c = e.getKey();
        BigDecimal value = parsePercent(cellAt(row, c));
        String key = StandardWeightRow.keyOf(e.getValue(), colDirection.get(c));
        // 续表合并：后到的非空值覆盖空值
        if (value != null || !weightRow.getValues().containsKey(key)) {
          weightRow.getValues().put(key, value);
        }
      }
    }
  }

  private StandardWeightRow findOrCreateRow(StandardWeightTable table, String normalizedItem,
      String rawItem, String category) {
    StandardWeightRow existing = table.findRow(normalizedItem);
    if (existing != null) {
      if ((existing.getCategory() == null || existing.getCategory().isBlank()) && category != null) {
        existing.setCategory(category);
      }
      return existing;
    }
    StandardWeightRow row = new StandardWeightRow();
    row.setCategory(category);
    row.setItem(rawItem);
    table.getRows().add(row);
    return row;
  }

  /** "5" → 5；"—"/"" → null */
  private BigDecimal parsePercent(String text) {
    if (text == null) {
      return null;
    }
    String v = text.trim();
    if (v.isEmpty() || v.contains("—") || v.contains("－") || v.equals("-") || v.equals("/")) {
      return null;
    }
    v = v.replace("%", "").replace("％", "").trim();
    if (!NUMERIC_CELL.matcher(v).matches()) {
      return null;
    }
    return new BigDecimal(v);
  }

  // ============================================================
  // 收尾：基本要求 L2 空小节标记、职业名称兜底、方向列表
  // ============================================================

  private void finalizeParse(ParserContext ctx) {
    ParsedStandard result = ctx.result;
    // 基本要求 L2 无列举项 → needLlm
    for (StandardNodeDoc l1 : result.getBasicSections()) {
      for (StandardNodeDoc l2 : l1.getChildren()) {
        if (l2.getTheoryMaterials().isEmpty()) {
          l2.setNeedLlm(true);
          result.getParseWarnings().add("WARN: 空小节（无列举项，需 LLM 概括）：基本要求 "
              + l1.getTitle() + " / " + l2.getTitle());
        }
        if (l2.getRelatedKnowledgeRaw() == null || l2.getRelatedKnowledgeRaw().isBlank()) {
          l2.setRelatedKnowledgeRaw(joinMaterials(l2.getTheoryMaterials()));
        }
      }
    }
    // 工作要求 L2 相关知识原文兜底
    for (StandardLevelDoc level : result.getLevels()) {
      for (StandardNodeDoc func : level.getWorkFunctions()) {
        for (StandardNodeDoc l2 : func.getChildren()) {
          if ((l2.getRelatedKnowledgeRaw() == null || l2.getRelatedKnowledgeRaw().isBlank())
              && !l2.getTheoryMaterials().isEmpty()) {
            l2.setRelatedKnowledgeRaw(joinMaterials(l2.getTheoryMaterials()));
          }
          if ((l2.getSkillRaw() == null || l2.getSkillRaw().isBlank())
              && !l2.getSkillMaterials().isEmpty()) {
            l2.setSkillRaw(joinMaterials(l2.getSkillMaterials()));
          }
        }
      }
    }
    if (result.getDirections().isEmpty()) {
      result.getDirections().add(ParsedStandard.DEFAULT_DIRECTION);
    }
    // 1.4 职业技能等级 → 每个工种允许等级范围（权威约束）
    parseDirectionLevels(ctx);
    if (result.getTheoryWeights() != null && result.getTheoryWeights().getRows().isEmpty()) {
      result.setTheoryWeights(null);
    }
    if (result.getSkillWeights() != null && result.getSkillWeights().getRows().isEmpty()) {
      result.setSkillWeights(null);
    }
    if (result.getTheoryWeights() == null) {
      result.getParseWarnings().add("WARN: 未解析到理论知识权重表，权重列需人工填写");
    }
    if (result.getSkillWeights() == null) {
      result.getParseWarnings().add("WARN: 未解析到技能要求权重表，技能细目表比重列需人工填写");
    }
    if (result.getLevels().isEmpty()) {
      result.getParseWarnings().add("ERROR: 未识别到任何工作要求等级节（3.x），疑似非国家标准格式");
    }
    if (result.getWorkTypeTitle() == null || result.getWorkTypeTitle().isBlank()) {
      result.getParseWarnings().add("WARN: 未解析到职业名称");
    }
  }

  /**
   * 解析 1.4「职业技能等级」收集到的段落 → 方向 → 允许等级代码列表。
   * 匹配形如「气焊工工种分别为：五级/初级工、四级/中级工、三级/高级工。」
   * 的句子/段落；「本职业共设五个等级…」等无方向句子自动忽略。
   */
  private void parseDirectionLevels(ParserContext ctx) {
    Map<String, List<Integer>> map = ctx.result.getDirectionLevels();
    for (String paragraph : ctx.sectionOneFourParagraphs) {
      // 单段落可能含多句（软换行/分号分隔），逐句匹配
      for (String sentence : paragraph.split("[。；;\\r\\n]+")) {
        Matcher m = DIRECTION_LEVEL_LINE.matcher(sentence.trim());
        if (!m.matches()) {
          continue;
        }
        String direction = m.group("direction").trim();
        List<Integer> codes = new ArrayList<>();
        Matcher lm = LEVEL_WORD.matcher(m.group("levels"));
        while (lm.find()) {
          codes.add(levelWordCode(lm.group()));
        }
        if (codes.isEmpty()) {
          ctx.result.getParseWarnings().add(
              "WARN: 1.4 工种等级句未识别到等级字：" + sentence);
          continue;
        }
        map.put(direction, codes);
      }
    }
    if (map.isEmpty()) {
      ctx.result.getParseWarnings()
          .add("WARN: 未解析到 1.4 职业技能等级的工种等级范围，按权重表兜底过滤");
    } else {
      ctx.result.getParseWarnings().add("INFO: 1.4 职业技能等级范围："
          + map.entrySet().stream()
              .map(e -> e.getKey() + "[" + e.getValue().stream()
                  .map(n -> LEVEL_NAMES[n]).toList() + "]")
              .toList());
    }
  }

  /** "五/四/三/二/一" → 0~4，未知返回 -1 */
  private int levelWordCode(String word) {
    return switch (word) {
      case "五" -> 0;
      case "四" -> 1;
      case "三" -> 2;
      case "二" -> 3;
      case "一" -> 4;
      default -> -1;
    };
  }

  private String joinMaterials(List<String> materials) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < materials.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(i + 1).append('.').append(materials.get(i));
    }
    return sb.toString();
  }

  // ============================================================
  // 解析上下文
  // ============================================================

  private final class ParserContext {
    private final ParsedStandard result;
    private int mainSection = 0;
    private int sectionOneSub = 0;
    private boolean expectSectionOneValue = false;
    /** 1.4 职业技能等级节的段落收集（每个工种允许等级范围） */
    private final List<String> sectionOneFourParagraphs = new ArrayList<>();
    private StandardNodeDoc currentBasicL1;
    private StandardNodeDoc currentBasicL2;
    private StandardLevelDoc currentLevel;
    private StandardWeightTable.Kind weightSection;
    private final StandardWeightTable theoryBuilder = new StandardWeightTable();
    private final StandardWeightTable skillBuilder = new StandardWeightTable();

    private ParserContext(ParsedStandard result) {
      this.result = result;
      theoryBuilder.setKind(StandardWeightTable.Kind.THEORY);
      skillBuilder.setKind(StandardWeightTable.Kind.SKILL);
      result.setTheoryWeights(theoryBuilder);
      result.setSkillWeights(skillBuilder);
    }

    private void registerDirection(String direction) {
      if (!ParsedStandard.DEFAULT_DIRECTION.equals(direction) && !result.getDirections().contains(direction)) {
        result.getDirections().add(direction);
      }
    }
  }
}
