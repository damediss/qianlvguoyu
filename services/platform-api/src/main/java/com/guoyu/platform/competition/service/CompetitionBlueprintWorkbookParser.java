package com.guoyu.platform.competition.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

/** 竞赛理论/实操细目表解析器。支持 XLSX、XLS 及扩展名与实际格式不一致的工作簿。 */
@Component
public class CompetitionBlueprintWorkbookParser {

  private final DataFormatter formatter = new DataFormatter();

  public ParsedBlueprint parse(Path file, String contentType) throws IOException {
    if (file == null || contentType == null) throw new IllegalArgumentException("细目表文件和类型不能为空");
    try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
      if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("细目表没有工作表");
      Sheet sheet = workbook.getSheetAt(0);
      return "theory".equalsIgnoreCase(contentType) ? parseTheory(sheet) : parsePractical(sheet);
    }
  }

  private ParsedBlueprint parseTheory(Sheet sheet) {
    Map<String, String> merged = collectMergedValues(sheet);
    int header = locate(sheet, merged, "考评范围一级");
    if (header < 0) header = locateTheoryHeader(sheet, merged);
    if (header < 0) header = locateBusinessTheoryHeader(sheet, merged);
    if (header < 0) throw new IllegalArgumentException("未找到理论细目表表头（考评范围一级）");
    TheoryColumns columns = theoryColumns(sheet, header, merged);
    List<ParsedNode> rows = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    int dataStart = findTheoryDataStart(sheet, header, merged, columns);
    String l1Key = null, l2Key = null, l3Key = null;
    int l1Index = -1, l2Index = -1, l3Index = -1;
    int l1Sort = 0, l2Sort = 0, l3Sort = 0, pointSort = 0;
    String lastL1Raw = "", lastL1Name = "", lastL1Weight = "";
    String lastL2Raw = "", lastL2Name = "", lastL2Weight = "";
    String lastL3Raw = "", lastL3Name = "", lastL3Weight = "";
    for (int r = dataStart; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      String l1Raw = carry(at(row, sheet, columns.l1Raw(), merged), lastL1Raw);
      String l1Name = carry(l1Raw, lastL1Name);
      String l1Weight = carry(at(row, sheet, columns.l1Weight(), merged), lastL1Weight);
      String l2Raw = carry(at(row, sheet, columns.l2Raw(), merged), lastL2Raw);
      String l2Name = carry(l2Raw, lastL2Name);
      String l2Weight = carry(at(row, sheet, columns.l2Weight(), merged), lastL2Weight);
      String l3Raw = carry(at(row, sheet, columns.l3Raw(), merged), lastL3Raw);
      String l3Name = carry(l3Raw, lastL3Name);
      String l3Weight = carry(at(row, sheet, columns.l3Weight(), merged), lastL3Weight);
      lastL1Raw = l1Raw; lastL1Name = l1Name; lastL1Weight = l1Weight;
      lastL2Raw = l2Raw; lastL2Name = l2Name; lastL2Weight = l2Weight;
      lastL3Raw = l3Raw; lastL3Name = l3Name; lastL3Weight = l3Weight;
      String related = at(row, sheet, columns.relatedKnowledge(), merged);
      String pointCode = at(row, sheet, columns.pointCode(), merged);
      String pointName = at(row, sheet, columns.pointName(), merged);
      String importance = at(row, sheet, columns.importance(), merged);
      String quota = at(row, sheet, columns.questionQuota(), merged);
      if (isFooter(l1Raw, l2Raw, l3Raw, pointCode, pointName)) break;
      if (blank(l1Raw, l1Name, l2Raw, l2Name, l3Raw, l3Name, pointCode, pointName)) continue;

      String l1Identity = identity(l1Raw, l1Name);
      if (!l1Identity.equals(l1Key)) {
        l1Key = l1Identity; l2Key = null; l3Key = null; l2Sort = 0; l3Sort = 0; pointSort = 0;
        ParsedNode n = parentNode(1, l1Raw, l1Name, l1Weight, null, ++l1Sort, r);
        l1Index = rows.size(); rows.add(n);
      }
      String l2Identity = identity(l2Raw, l2Name);
      if (!l2Identity.isBlank() && !l2Identity.equals(l2Key)) {
        l2Key = l2Identity; l3Key = null; l3Sort = 0; pointSort = 0;
        ParsedNode n = parentNode(2, l2Raw, l2Name, l2Weight, l1Index, ++l2Sort, r);
        l2Index = rows.size(); rows.add(n);
      }
      String l3Identity = identity(l3Raw, l3Name);
      if (!l3Identity.isBlank() && !l3Identity.equals(l3Key)) {
        l3Key = l3Identity; pointSort = 0;
        ParsedNode n = parentNode(3, l3Raw, l3Name, l3Weight, l2Index, ++l3Sort, r);
        l3Index = rows.size(); rows.add(n);
      }
      if (!text(pointCode).isBlank() || !text(pointName).isBlank()) {
        if (l3Index < 0) {
          warnings.add("第" + (r + 1) + "行考评点缺少所属三级考核范围，已跳过");
          continue;
        }
        ParsedNode point = new ParsedNode(4, text(pointCode), text(pointName),
            null, null, null, null, text(importance), null, parseQuota(quota), ++pointSort, r);
        point.parentIndex = l3Index;
        point.relatedKnowledge = text(related);
        rows.add(point);
      }
    }
    if (rows.isEmpty()) throw new IllegalArgumentException("理论细目表没有有效数据");
    return new ParsedBlueprint(firstText(sheet, merged), rows, warnings);
  }

  private ParsedBlueprint parsePractical(Sheet sheet) {
    Map<String, String> merged = collectMergedValues(sheet);
    int header = locate(sheet, merged, "考评范围一级");
    if (header < 0) header = locateTheoryHeader(sheet, merged);
    if (header < 0) throw new IllegalArgumentException("未找到实操细目表表头（考评范围一级）");
    // The competition template uses a two-row header: row `header` contains
    // the grouped labels while the actual column names are on the next row.
    // Historical generic templates put the same labels on one row.  Inspect
    // both rows so the parser does not silently interpret a competition sheet
    // as the legacy layout (which would turn the 13-column point rows into
    // level-2/level-3 nodes without topics).
    String selectHeader = at(sheet.getRow(header), sheet, 8, merged)
        + " " + at(sheet.getRow(header + 1), sheet, 8, merged);
    String legacySelectHeader = at(sheet.getRow(header), sheet, 3, merged)
        + " " + at(sheet.getRow(header + 1), sheet, 3, merged);
    boolean competitionLayout = contains(selectHeader, "选考方式")
        && !contains(legacySelectHeader, "选考方式");
    boolean standardPracticalLayout = !competitionLayout
        && isStandardPracticalLayout(sheet, header, merged);
    int dataStart = findPracticalDataStart(sheet, header, merged);
    boolean standardSkillRequirementLayout = standardPracticalLayout
        && usesNumberedSkillRequirements(sheet, dataStart, merged);
    Map<String, BigDecimal> standardL2Weights = standardPracticalLayout
        && !standardSkillRequirementLayout
        ? collectStandardPracticalL2Weights(sheet, findPracticalDataStart(sheet, header, merged), merged)
        : new HashMap<>();
    List<ParsedNode> rows = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    String l1Key = null, l2Key = null, topicKey = null;
    int l1Index = -1, l2Index = -1, topicIndex = -1;
    int l1Sort = 0, l2Sort = 0, topicSort = 0, pointSort = 0;
    String lastL1Raw = "", lastL1Name = "", lastL1Weight = "", lastL1Select = "";
    String lastL2Raw = "", lastL2Name = "", lastL2Weight = "", lastL2Select = "", lastSkill = "";
    String lastTopicName = "", lastTopicWeight = "", lastTopicSelect = "";
    for (int r = dataStart; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      String l1Raw, l1Name, l1Weight, l1Select;
      String l2Raw, l2Name, l2Weight, l2Select, skill;
      String topicRaw, topicName, topicWeight, topicSelect;
      String pointCode, pointName, importance, quota;
      if (competitionLayout) {
        l1Raw = at(row, sheet, 0, merged); l1Name = at(row, sheet, 1, merged); l1Weight = at(row, sheet, 2, merged); l1Select = "";
        l2Raw = at(row, sheet, 3, merged); l2Name = at(row, sheet, 4, merged); l2Weight = at(row, sheet, 5, merged); l2Select = ""; skill = "";
        topicRaw = ""; topicName = at(row, sheet, 6, merged); topicWeight = at(row, sheet, 7, merged); topicSelect = at(row, sheet, 8, merged);
        pointCode = at(row, sheet, 9, merged); pointName = at(row, sheet, 10, merged); importance = at(row, sheet, 11, merged); quota = at(row, sheet, 12, merged);
      } else if (standardPracticalLayout) {
        // The exported 13-column competition template has composite headers:
        // A:D is level 1, E:I is level 2 plus skill/topic, and J:M is the
        // assessment point. Two business variants share this header:
        // 1) G is a short selectable sub-range name, so G/H/I becomes level 3;
        // 2) G contains numbered skill requirements (for example 1.1.1...),
        //    so E/F/H/I is the selectable level 2 and G is only its attribute.
        l1Raw = at(row, sheet, 0, merged); l1Name = at(row, sheet, 1, merged); l1Weight = at(row, sheet, 2, merged); l1Select = at(row, sheet, 3, merged);
        l2Raw = at(row, sheet, 4, merged); l2Name = at(row, sheet, 5, merged); skill = at(row, sheet, 6, merged);
        if (standardSkillRequirementLayout) {
          l2Weight = at(row, sheet, 7, merged); l2Select = at(row, sheet, 8, merged);
          topicRaw = ""; topicName = ""; topicWeight = ""; topicSelect = "";
        } else {
          l2Weight = ""; l2Select = "";
          topicRaw = ""; topicName = skill; topicWeight = at(row, sheet, 7, merged); topicSelect = at(row, sheet, 8, merged);
        }
        pointCode = at(row, sheet, 9, merged); pointName = at(row, sheet, 10, merged); importance = at(row, sheet, 11, merged); quota = at(row, sheet, 12, merged);
      } else {
        l1Raw = at(row, sheet, 0, merged); l1Name = at(row, sheet, 1, merged); l1Weight = at(row, sheet, 2, merged); l1Select = at(row, sheet, 3, merged);
        l2Raw = at(row, sheet, 4, merged); l2Name = at(row, sheet, 5, merged); skill = at(row, sheet, 6, merged); l2Weight = at(row, sheet, 7, merged); l2Select = at(row, sheet, 8, merged);
        topicRaw = ""; topicName = ""; topicWeight = ""; topicSelect = "";
        pointCode = at(row, sheet, 9, merged); pointName = at(row, sheet, 10, merged); importance = at(row, sheet, 11, merged); quota = at(row, sheet, 12, merged);
      }
      l1Raw = carry(l1Raw, lastL1Raw); l1Name = carry(l1Name, lastL1Name); l1Weight = carry(l1Weight, lastL1Weight); l1Select = carry(l1Select, lastL1Select);
      l2Raw = carry(l2Raw, lastL2Raw); l2Name = carry(l2Name, lastL2Name);
      if (!standardPracticalLayout || standardSkillRequirementLayout) {
        l2Weight = carry(l2Weight, lastL2Weight); l2Select = carry(l2Select, lastL2Select);
      }
      skill = carry(skill, lastSkill);
      topicName = carry(topicName, lastTopicName); topicWeight = carry(topicWeight, lastTopicWeight); topicSelect = carry(topicSelect, lastTopicSelect);
      lastL1Raw = l1Raw; lastL1Name = l1Name; lastL1Weight = l1Weight; lastL1Select = l1Select;
      lastL2Raw = l2Raw; lastL2Name = l2Name; lastL2Weight = l2Weight; lastL2Select = l2Select; lastSkill = skill;
      lastTopicName = topicName; lastTopicWeight = topicWeight; lastTopicSelect = topicSelect;
      if (isFooter(l1Raw, l2Raw, topicName, pointCode, pointName)) break;
      if (blank(l1Raw, l1Name, l2Raw, l2Name, topicName, pointCode, pointName)) continue;
      String newL1 = identity(l1Raw, l1Name);
      if (!newL1.isBlank() && !newL1.equals(l1Key)) {
        l1Key = newL1; l2Key = null; topicKey = null; l2Sort = 0; topicSort = 0; pointSort = 0;
        ParsedNode n = parentNode(1, l1Raw, l1Name, l1Weight, null, ++l1Sort, r); n.selectMode = l1Select;
        l1Index = rows.size(); rows.add(n);
      }
      String newL2 = identity(l2Raw, l2Name);
      if (!newL2.isBlank() && !newL2.equals(l2Key)) {
        l2Key = newL2; topicKey = null; topicSort = 0; pointSort = 0;
        String effectiveL2Weight = l2Weight;
        if (standardPracticalLayout && !standardSkillRequirementLayout) {
          BigDecimal aggregateWeight = standardL2Weights.get(newL2);
          effectiveL2Weight = aggregateWeight == null ? "" : aggregateWeight.toPlainString();
        }
        ParsedNode n = parentNode(2, l2Raw, l2Name, effectiveL2Weight, l1Index, ++l2Sort, r); n.selectMode = l2Select;
        if (!standardPracticalLayout || standardSkillRequirementLayout) n.skillRequirement = skill;
        l2Index = rows.size(); rows.add(n);
      }
      if (competitionLayout || standardPracticalLayout && !standardSkillRequirementLayout) {
        String newTopic = identity(topicRaw, topicName);
        if (!newTopic.isBlank() && !newTopic.equals(topicKey)) {
          topicKey = newTopic; pointSort = 0;
          ParsedNode n = parentNode(3, fallbackCode(3, ++topicSort), topicName, topicWeight, l2Index, topicSort, r); n.selectMode = topicSelect;
          topicIndex = rows.size(); rows.add(n);
        }
      } else {
        topicIndex = l2Index;
      }
      if (!text(pointCode).isBlank() || !text(pointName).isBlank()) {
        if (topicIndex < 0) { warnings.add("第" + (r + 1) + "行考评点缺少所属主题，已跳过"); continue; }
        ParsedNode point = new ParsedNode(competitionLayout
            || standardPracticalLayout && !standardSkillRequirementLayout ? 4 : 3, text(pointCode), text(pointName),
            null, null, null, null, text(importance), null, parseQuota(quota), ++pointSort, r);
        point.parentIndex = topicIndex; rows.add(point);
      }
    }
    if (rows.isEmpty()) throw new IllegalArgumentException("实操细目表没有有效数据");
    return new ParsedBlueprint(firstText(sheet, merged), rows, warnings);
  }

  private ParsedNode parentNode(int level, String raw, String name, String weight, Integer parent,
      int sort, int sourceRow) {
    CodeParts parts = parseCode(raw, name, level, sort);
    ParsedNode n = new ParsedNode(level, parts.code, parts.name, parseWeight(weight), parent,
        null, null, null, parts.xyz, null, sort, sourceRow);
    n.parentIndex = parent == null ? -1 : parent;
    return n;
  }

  private Integer parseQuota(String value) {
    if (value == null || value.isBlank()) return null;
    try { return new BigDecimal(value.trim()).intValueExact(); }
    catch (Exception e) { return null; }
  }

  private BigDecimal parseWeight(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      String normalized = value.trim();
      boolean percent = normalized.endsWith("%");
      if (percent) normalized = normalized.substring(0, normalized.length() - 1).trim();
      BigDecimal d = new BigDecimal(normalized);
      return percent || d.compareTo(BigDecimal.ONE) > 0 ? d.movePointLeft(2) : d;
    } catch (Exception e) { return null; }
  }

  private CodeParts parseCode(String raw, String fallbackName, int level, int sort) {
    String value = text(raw).replace('\r', '\n').trim();
    String[] lines = value.split("\\n");
    String code = "", name = text(fallbackName);
    boolean hasSeparateName = !name.isBlank() && !name.equals(value);
    // Most exported workbooks put the code on the first line and the display
    // name on the second line (for example `A-1\\n安全操作`).  Handle the
    // complete hierarchical token before the looser legacy expressions below;
    // otherwise the old `A` prefix matcher would silently truncate the code.
    if (lines.length > 1) {
      String first = lines[0].trim().replaceAll("[（(].*$", "");
      if (first.matches("[A-Za-z0-9]+(?:[-_.][A-Za-z0-9]+)*")) {
        code = first;
        StringBuilder b = new StringBuilder();
        for (int i = 1; i < lines.length; i++) if (!lines[i].isBlank()) b.append(lines[i].trim()).append(' ');
        if (!b.isEmpty() && (!hasSeparateName || name.isBlank())) name = b.toString().trim();
      }
    }
    if (lines.length > 0) {
      String last = lines[lines.length - 1].trim();
      if (code.isBlank() && last.matches("[A-Za-z0-9]+(?:[-_.][A-Za-z0-9]+)*(?:[（(][^）)]*[）)])?")) {
        code = last.replaceAll("[（(].*$", "");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines.length - 1; i++) if (!lines[i].isBlank()) b.append(lines[i].trim()).append(' ');
        if (!b.isEmpty() && (!hasSeparateName || name.isBlank())) name = b.toString().trim();
      }
    }
    if (code.isBlank()) {
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([A-Za-z0-9]+(?:[-_.][A-Za-z0-9]+)*)\\s*[：:、.]?\\s*(.*)$").matcher(value);
      if (m.matches() && !m.group(2).isBlank()) {
        code = m.group(1);
        if (!hasSeparateName || name.isBlank()) name = m.group(2).trim();
      }
    }
    if (code.isBlank()) {
      java.util.regex.Matcher trailing = java.util.regex.Pattern.compile("^(.+?)([A-Za-z]\\d*)$").matcher(value);
      if (trailing.matches() && !trailing.group(1).isBlank()) {
        code = trailing.group(2);
        if (!hasSeparateName || name.isBlank()) name = trailing.group(1).trim();
      }
    }
    if (code.isBlank()) code = fallbackCode(level, sort);
    if (name.isBlank()) name = value;
    String xyz = value.replaceAll(".*[（(]\\s*([^）)]*:[^）)]*)[）)].*", "$1");
    if (xyz.equals(value)) xyz = null;
    return new CodeParts(code.trim(), name.trim(), xyz);
  }

  private String fallbackCode(int level, int sort) { return level == 1 ? (sort == 1 ? "A" : "B") : String.valueOf((char) ('A' + sort - 1)); }

  private int locateTheoryHeader(Sheet sheet, Map<String, String> merged) {
    for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 15); rowIndex++) {
      String first = at(sheet.getRow(rowIndex), sheet, 0, merged);
      String next = at(sheet.getRow(rowIndex + 1), sheet, 0, merged);
      if (contains(first, "考评范围") && (contains(next, "一级") || contains(first, "一级"))) return rowIndex;
      if (contains(first, "一级") && contains(at(sheet.getRow(rowIndex), sheet, 2, merged), "二级")) return rowIndex;
    }
    return -1;
  }

  private int locateBusinessTheoryHeader(Sheet sheet, Map<String, String> merged) {
    for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 15); rowIndex++) {
      boolean behavior = false;
      boolean point = false;
      for (int column = 0; column <= 12; column++) {
        String value = at(sheet.getRow(rowIndex), sheet, column, merged);
        behavior = behavior || contains(value, "行为领域");
        point = point || contains(value, "鉴定点") || contains(value, "考评点");
      }
      if (behavior && point) return rowIndex;
    }
    return -1;
  }

  private TheoryColumns theoryColumns(Sheet sheet, int header, Map<String, String> merged) {
    String firstHeader = at(sheet.getRow(header), sheet, 0, merged);
    if (contains(firstHeader, "行为领域") && !contains(firstHeader, "考评范围")) {
      if (contains(at(sheet.getRow(header), sheet, 6, merged), "级别")
          && contains(at(sheet.getRow(header), sheet, 9, merged), "题干")) {
        // The supplied business question bank is a question-level source,
        // not a complete competition blueprint: it has no weights, point
        // codes, XYZ importance, or quotas. Parse its hierarchy without
        // guessing those missing values so validation can report the exact
        // fields that must be completed in the competition template.
        return new TheoryColumns(0, -1, 1, -1, 2, -1, -1, -1, 5, -1, -1);
      }
      // The business theory workbook uses the question-bank hierarchy as its
      // starting point. The competition blueprint keeps four persisted
      // levels: behavior domain, two assessment ranges, and the point.
      return new TheoryColumns(0, 2, 1, 4, 5, 6, 7, 8, 9, 10, 11);
    }
    return new TheoryColumns(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  private int findTheoryDataStart(Sheet sheet, int header, Map<String, String> merged,
      TheoryColumns columns) {
    for (int rowIndex = header + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      String pointCode = at(sheet.getRow(rowIndex), sheet, columns.pointCode(), merged);
      String pointName = at(sheet.getRow(rowIndex), sheet, columns.pointName(), merged);
      if ((!text(pointCode).isBlank() || !text(pointName).isBlank())
          && !headerLike(pointCode) && !headerLike(pointName)) return rowIndex;
    }
    return header + 1;
  }

  private boolean isStandardPracticalLayout(Sheet sheet, int header, Map<String, String> merged) {
    Row columns = sheet.getRow(header + 1);
    return contains(at(columns, sheet, 3, merged), "选考方式")
        && contains(at(columns, sheet, 4, merged), "代码")
        && contains(at(columns, sheet, 6, merged), "技能要求")
        && contains(at(columns, sheet, 8, merged), "选考方式")
        && contains(at(columns, sheet, 9, merged), "代码")
        && contains(at(columns, sheet, 10, merged), "名称")
        && contains(at(columns, sheet, 11, merged), "重要程度")
        && contains(at(columns, sheet, 12, merged), "试题量");
  }

  private boolean usesNumberedSkillRequirements(Sheet sheet, int dataStart,
      Map<String, String> merged) {
    for (int rowIndex = dataStart; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      String value = text(at(sheet.getRow(rowIndex), sheet, 6, merged));
      if (value.isBlank()) continue;
      for (String line : value.replace('\r', '\n').split("\\n")) {
        if (line.trim().matches("\\d+(?:\\.\\d+){1,}\\s*.*")) return true;
      }
      return false;
    }
    return false;
  }

  /**
   * In the standard exported template H is the weight of each G topic.  The
   * competition tree also persists the E/F level-2 node, so derive its weight
   * from its topics before building the tree.  Topic names can be merged down
   * several rows; de-duplicate them while aggregating to avoid double-counting.
   */
  private Map<String, BigDecimal> collectStandardPracticalL2Weights(Sheet sheet, int dataStart,
      Map<String, String> merged) {
    Map<String, BigDecimal> weights = new HashMap<>();
    Map<String, Boolean> seenTopics = new HashMap<>();
    String lastL2Raw = "", lastL2Name = "", lastTopicName = "";
    for (int r = dataStart; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      String rawL2 = at(row, sheet, 4, merged);
      String rawL2Name = at(row, sheet, 5, merged);
      String rawTopic = at(row, sheet, 6, merged);
      String l2Raw = carry(rawL2, lastL2Raw);
      String l2Name = carry(rawL2Name, lastL2Name);
      String topicName = carry(rawTopic, lastTopicName);
      String pointCode = at(row, sheet, 9, merged);
      String pointName = at(row, sheet, 10, merged);
      if (isFooter(l2Raw, topicName, pointCode, pointName)) break;
      if (blank(l2Raw, l2Name, topicName, pointCode, pointName)) continue;

      if (!text(rawTopic).isBlank()) {
        BigDecimal topicWeight = parseWeight(at(row, sheet, 7, merged));
        String topicKey = identity(l2Raw, l2Name) + "|" + identity("", topicName);
        if (topicWeight != null && seenTopics.putIfAbsent(topicKey, Boolean.TRUE) == null) {
          weights.merge(identity(l2Raw, l2Name), topicWeight, BigDecimal::add);
        }
      }
      lastL2Raw = l2Raw;
      lastL2Name = l2Name;
      lastTopicName = topicName;
    }
    return weights;
  }

  private int findPracticalDataStart(Sheet sheet, int header, Map<String, String> merged) {
    for (int rowIndex = header + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      String pointCode = at(row, sheet, 9, merged);
      String pointName = at(row, sheet, 10, merged);
      if ((!text(pointCode).isBlank() || !text(pointName).isBlank())
          && !headerLike(pointCode) && !headerLike(pointName)) {
        return rowIndex;
      }
      // Legacy/generic templates do not have the competition point columns;
      // their first data row is identified by the question/stem column.
      String stem = at(row, sheet, 1, merged);
      if (!text(stem).isBlank() && !headerLike(stem)) return rowIndex;
    }
    return header + 1;
  }

  private boolean headerLike(String value) {
    String normalized = text(value).replace(" ", "").replace("　", "");
    return normalized.equals("代码") || normalized.equals("名称") || normalized.equals("考评点")
        || normalized.equals("一级代码") || normalized.equals("一级名称")
        || normalized.equals("二级代码") || normalized.equals("二级名称")
        || normalized.equals("主题权重") || normalized.equals("考评点代码")
        || normalized.equals("考评点名称") || normalized.equals("重要程度")
        || normalized.equals("试题量") || normalized.contains("表头") || normalized.contains("选考方式");
  }
  private int locate(Sheet sheet, Map<String, String> merged, String keyword) {
    for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 15); r++) for (int c = 0; c <= 12; c++) if (contains(at(sheet.getRow(r), sheet, c, merged), keyword)) return r;
    return -1;
  }
  private String firstText(Sheet sheet, Map<String, String> merged) { return text(at(sheet.getRow(0), sheet, 0, merged)); }
  private Map<String, String> collectMergedValues(Sheet sheet) {
    Map<String, String> out = new HashMap<>();
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress r = sheet.getMergedRegion(i); String v = text(cell(sheet.getRow(r.getFirstRow()), r.getFirstColumn()));
      for (int row = r.getFirstRow(); row <= r.getLastRow(); row++) for (int col = r.getFirstColumn(); col <= r.getLastColumn(); col++) out.put(row + ":" + col, v);
    }
    return out;
  }
  private String at(Row row, Sheet sheet, int col, Map<String, String> merged) {
    if (row == null || col < 0) return "";
    return merged.getOrDefault(row.getRowNum() + ":" + col, text(cell(row, col)));
  }
  private String cell(Row row, int col) { Cell c = row == null ? null : row.getCell(col); return c == null ? "" : formatter.formatCellValue(c); }
  private boolean isFooter(String... values) { for (String v : values) if (contains(v, "填表人") || contains(v, "审表人") || contains(v, "制表人")) return true; return false; }
  private boolean blank(String... values) { for (String v : values) if (!text(v).isBlank()) return false; return true; }
  private String identity(String raw, String name) { return (text(raw) + "|" + text(name)).trim(); }
  private boolean contains(String value, String keyword) { return value != null && value.replace(" ", "").replace("　", "").contains(keyword); }
  private String text(String value) { return value == null ? "" : value.replace('\u200b', ' ').replace('\ufeff', ' ').trim(); }
  private String carry(String value, String previous) { return text(value).isBlank() ? text(previous) : text(value); }

  private record CodeParts(String code, String name, String xyz) {}

  private record TheoryColumns(
      int l1Raw, int l1Weight, int l2Raw, int l2Weight, int l3Raw, int l3Weight,
      int relatedKnowledge, int pointCode, int pointName, int importance, int questionQuota) {
  }

  public static final class ParsedBlueprint {
    private final String title;
    private final List<ParsedNode> rows;
    private final List<String> warnings;
    public ParsedBlueprint(String title, List<ParsedNode> rows, List<String> warnings) {
      this.title = title; this.rows = List.copyOf(rows); this.warnings = List.copyOf(warnings);
    }
    public String title() { return title; }
    public List<ParsedNode> rows() { return rows; }
    public List<String> warnings() { return warnings; }
  }

  public static final class ParsedNode {
    private final int nodeLevel; private final String code; private final String name; private final BigDecimal weight;
    private final Integer parentIndexHint; private final String importance; private final String xyzStat;
    private final Integer questionQuota; private final int sortOrder; private final int sourceRow;
    private String selectMode; private String skillRequirement; private String relatedKnowledge; private int parentIndex = -1;
    public ParsedNode(int nodeLevel, String code, String name, BigDecimal weight, Integer parentIndexHint,
        String selectMode, String skillRequirement, String importance, String xyzStat, Integer questionQuota,
        int sortOrder, int sourceRow) {
      this.nodeLevel = nodeLevel; this.code = code == null ? "" : code.trim(); this.name = name == null ? "" : name.trim();
      this.weight = weight; this.parentIndexHint = parentIndexHint; this.selectMode = selectMode;
      this.skillRequirement = skillRequirement; this.importance = importance; this.xyzStat = xyzStat;
      this.questionQuota = questionQuota; this.sortOrder = sortOrder; this.sourceRow = sourceRow;
    }
    public int nodeLevel() { return nodeLevel; } public String code() { return code; } public String name() { return name; }
    public BigDecimal weight() { return weight; } public String selectMode() { return selectMode; }
    public String skillRequirement() { return skillRequirement; } public String relatedKnowledge() { return relatedKnowledge; }
    public String importance() { return importance; } public String xyzStat() { return xyzStat; }
    public Integer questionQuota() { return questionQuota; } public int sortOrder() { return sortOrder; }
    public int sourceRow() { return sourceRow; } public int parentIndex() { return parentIndex; }
  }
}
