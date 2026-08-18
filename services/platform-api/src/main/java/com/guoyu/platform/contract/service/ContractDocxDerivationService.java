package com.guoyu.platform.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Builds stable text anchors and applies field replacements to an immutable DOCX snapshot. */
@Service
public class ContractDocxDerivationService {

  public static final String MANIFEST_SCHEMA = "guoyu.contract-source-manifest/v1";
  public static final String RENDERER_VERSION = "contract-docx-poc-2";
  private static final Set<String> REQUIRED_FIELDS =
      Set.of("contractNumber", "partyA", "partyB", "amount");
  private static final Pattern PAYMENT_PERCENT_PATTERN =
      Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)%\\s*$");

  private static final List<FieldPattern> FIELD_PATTERNS = List.of(
      new FieldPattern("contractNumber", Pattern.compile(
          "^\\s*(?:合同编号|协议编号)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:合同编号|协议编号)\\s*[:：]?\\s*$")),
      new FieldPattern("partyA", Pattern.compile(
          "^\\s*(?:甲方|甲方（[^）]{1,30}）|委托方)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:甲方|甲方（[^）]{1,30}）|委托方)\\s*[:：]?\\s*$")),
      new FieldPattern("partyB", Pattern.compile(
          "^\\s*(?:乙方|乙方（[^）]{1,30}）|受托方|服务方)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:乙方|乙方（[^）]{1,30}）|受托方|服务方)\\s*[:：]?\\s*$")),
      new FieldPattern("amount", Pattern.compile(
          "^\\s*(?:合同金额|合同总额|项目金额|总金额|金额)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:合同金额|合同总额|项目金额|总金额|金额)\\s*[:：]?\\s*$")),
      new FieldPattern("projectName", Pattern.compile(
          "^\\s*(?:项目名称|项目名)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:项目名称|项目名)\\s*[:：]?\\s*$")),
      new FieldPattern("serviceScope", Pattern.compile(
          "^\\s*(?:服务内容|服务范围|项目内容)\\s*[:：]\\s*(.+?)\\s*$"),
          Pattern.compile("^\\s*(?:服务内容|服务范围|项目内容)\\s*[:：]?\\s*$"))
  );

  private final ObjectMapper objectMapper;

  public ContractDocxDerivationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AnalysisResult analyze(byte[] sourceBytes) {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(sourceBytes))) {
      ScanContext scan = collectParagraphs(document);
      Map<String, LinkedHashSet<String>> candidates = new LinkedHashMap<>();
      for (FieldPattern fieldPattern : FIELD_PATTERNS) {
        candidates.put(fieldPattern.field(), new LinkedHashSet<>());
      }

      for (ParagraphRef ref : scan.paragraphs()) {
        String text = textOf(ref.paragraph());
        if (!StringUtils.hasText(text)) {
          continue;
        }
        for (FieldPattern fieldPattern : FIELD_PATTERNS) {
          Matcher matcher = fieldPattern.inlinePattern().matcher(text);
          if (matcher.matches() && StringUtils.hasText(matcher.group(1))) {
            candidates.get(fieldPattern.field()).add(matcher.group(1).trim());
          }
        }
      }
      for (AdjacentCandidate adjacent : scan.adjacentCandidates()) {
        if (StringUtils.hasText(adjacent.value())) {
          candidates.get(adjacent.field()).add(adjacent.value().trim());
        }
      }

      Map<String, String> detectedFields = new LinkedHashMap<>();
      List<String> warnings = new ArrayList<>();
      for (Map.Entry<String, LinkedHashSet<String>> entry : candidates.entrySet()) {
        if (entry.getValue().isEmpty()) {
          continue;
        }
        detectedFields.put(entry.getKey(), entry.getValue().iterator().next());
        if (entry.getValue().size() > 1) {
          warnings.add("conflicting_values:" + entry.getKey());
        }
      }
      detectPaymentAmounts(document, detectedFields, warnings);

      List<Anchor> anchors = buildOccurrenceAnchors(scan.paragraphs(), detectedFields);
      for (String required : REQUIRED_FIELDS) {
        if (!detectedFields.containsKey(required)) {
          warnings.add("missing_required_anchor:" + required);
        }
      }

      boolean derivable = detectedFields.keySet().containsAll(REQUIRED_FIELDS)
          && warnings.stream().noneMatch(value -> value.startsWith("conflicting_values:"));
      ContractSourceManifest manifest = new ContractSourceManifest(
          MANIFEST_SCHEMA,
          RENDERER_VERSION,
          detectedFields,
          anchors,
          warnings
      );
      return new AnalysisResult(manifest, derivable ? "derivable" : "requires_review");
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "Source DOCX structure analysis failed: " + ex.getMessage());
    }
  }

  public String writeManifest(ContractSourceManifest manifest) {
    try {
      return objectMapper.writeValueAsString(manifest);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Contract source manifest serialization failed");
    }
  }

  public ContractSourceManifest readManifest(String manifestJson) {
    try {
      ContractSourceManifest manifest = objectMapper.readValue(manifestJson, ContractSourceManifest.class);
      if (!MANIFEST_SCHEMA.equals(manifest.schema())) {
        throw new BusinessException(ErrorCode.CONFLICT, "Unsupported contract source manifest schema");
      }
      return manifest;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Contract source manifest is invalid");
    }
  }

  public byte[] derive(byte[] sourceBytes, ContractSourceManifest manifest, Map<String, String> values) {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(sourceBytes));
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ScanContext scan = collectParagraphs(document);
      Map<String, XWPFParagraph> paragraphByPath = new HashMap<>();
      for (ParagraphRef ref : scan.paragraphs()) {
        paragraphByPath.put(ref.path(), ref.paragraph());
      }

      Map<String, List<Anchor>> anchorsByPath = new LinkedHashMap<>();
      for (Anchor anchor : manifest.anchors()) {
        if (values.containsKey(anchor.field())) {
          anchorsByPath.computeIfAbsent(anchor.path(), ignored -> new ArrayList<>()).add(anchor);
        }
      }
      for (Map.Entry<String, List<Anchor>> entry : anchorsByPath.entrySet()) {
        XWPFParagraph paragraph = paragraphByPath.get(entry.getKey());
        if (paragraph == null) {
          throw new BusinessException(ErrorCode.CONFLICT,
              "Contract source anchor is missing: " + entry.getKey());
        }
        entry.getValue().sort(Comparator.comparingInt(Anchor::start).reversed());
        for (Anchor anchor : entry.getValue()) {
          String replacement = values.get(anchor.field());
          replaceRangePreservingRuns(paragraph, anchor.start(), anchor.end(),
              anchor.originalValue(), replacement);
        }
      }

      List<ParagraphRef> renderedParagraphs = collectParagraphs(document).paragraphs();
      for (Map.Entry<String, String> detected : manifest.detectedFields().entrySet()) {
        String replacement = values.get(detected.getKey());
        if (!StringUtils.hasText(replacement) || detected.getValue().equals(replacement)) {
          continue;
        }
        for (ParagraphRef ref : renderedParagraphs) {
          if (textOf(ref.paragraph()).contains(detected.getValue())) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
                "Source value remains after derivation: " + detected.getKey());
          }
        }
      }

      document.write(output);
      return output.toByteArray();
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "Contract DOCX derivation failed: " + ex.getMessage());
    }
  }

  private List<Anchor> buildOccurrenceAnchors(
      List<ParagraphRef> paragraphs, Map<String, String> detectedFields) {
    List<Anchor> anchors = new ArrayList<>();
    Map<String, Integer> counters = new HashMap<>();
    for (ParagraphRef ref : paragraphs) {
      String text = textOf(ref.paragraph());
      if (!StringUtils.hasText(text)) {
        continue;
      }
      for (Map.Entry<String, String> field : detectedFields.entrySet()) {
        String originalValue = field.getValue();
        int from = 0;
        while (StringUtils.hasText(originalValue) && from <= text.length() - originalValue.length()) {
          int start = text.indexOf(originalValue, from);
          if (start < 0) {
            break;
          }
          int index = counters.merge(field.getKey(), 1, Integer::sum);
          anchors.add(new Anchor(
              "field." + field.getKey() + "." + index,
              field.getKey(),
              ref.path(),
              start,
              start + originalValue.length(),
              originalValue,
              List.of("replace_variable")
          ));
          from = start + originalValue.length();
        }
      }
    }
    return anchors;
  }

  private ScanContext collectParagraphs(XWPFDocument document) {
    List<ParagraphRef> paragraphs = new ArrayList<>();
    List<AdjacentCandidate> adjacentCandidates = new ArrayList<>();

    for (int i = 0; i < document.getParagraphs().size(); i++) {
      paragraphs.add(new ParagraphRef("body.p[" + i + "]", document.getParagraphs().get(i)));
    }
    for (int i = 0; i < document.getTables().size(); i++) {
      collectTable(document.getTables().get(i), "body.t[" + i + "]", paragraphs, adjacentCandidates);
    }
    List<XWPFHeader> headers = document.getHeaderList();
    for (int h = 0; h < headers.size(); h++) {
      XWPFHeader header = headers.get(h);
      for (int i = 0; i < header.getParagraphs().size(); i++) {
        paragraphs.add(new ParagraphRef("header[" + h + "].p[" + i + "]", header.getParagraphs().get(i)));
      }
      for (int i = 0; i < header.getTables().size(); i++) {
        collectTable(header.getTables().get(i), "header[" + h + "].t[" + i + "]",
            paragraphs, adjacentCandidates);
      }
    }
    List<XWPFFooter> footers = document.getFooterList();
    for (int f = 0; f < footers.size(); f++) {
      XWPFFooter footer = footers.get(f);
      for (int i = 0; i < footer.getParagraphs().size(); i++) {
        paragraphs.add(new ParagraphRef("footer[" + f + "].p[" + i + "]", footer.getParagraphs().get(i)));
      }
      for (int i = 0; i < footer.getTables().size(); i++) {
        collectTable(footer.getTables().get(i), "footer[" + f + "].t[" + i + "]",
            paragraphs, adjacentCandidates);
      }
    }
    return new ScanContext(paragraphs, adjacentCandidates);
  }

  private void collectTable(
      XWPFTable table,
      String tablePath,
      List<ParagraphRef> paragraphs,
      List<AdjacentCandidate> adjacentCandidates) {
    List<XWPFTableRow> rows = table.getRows();
    for (int r = 0; r < rows.size(); r++) {
      List<XWPFTableCell> cells = rows.get(r).getTableCells();
      for (int c = 0; c < cells.size(); c++) {
        XWPFTableCell cell = cells.get(c);
        String cellPath = tablePath + ".r[" + r + "].c[" + c + "]";
        for (int p = 0; p < cell.getParagraphs().size(); p++) {
          paragraphs.add(new ParagraphRef(cellPath + ".p[" + p + "]", cell.getParagraphs().get(p)));
        }
        for (int t = 0; t < cell.getTables().size(); t++) {
          collectTable(cell.getTables().get(t), cellPath + ".t[" + t + "]",
              paragraphs, adjacentCandidates);
        }
        if (c + 1 < cells.size()) {
          String field = fieldForStandaloneLabel(cell.getText());
          if (field != null) {
            XWPFTableCell valueCell = cells.get(c + 1);
            for (int p = 0; p < valueCell.getParagraphs().size(); p++) {
              String value = textOf(valueCell.getParagraphs().get(p)).trim();
              if (StringUtils.hasText(value)) {
                String valuePath = tablePath + ".r[" + r + "].c[" + (c + 1) + "].p[" + p + "]";
                adjacentCandidates.add(new AdjacentCandidate(field, valuePath, value));
                break;
              }
            }
          }
        }
      }
    }
  }

  private String fieldForStandaloneLabel(String text) {
    if (!StringUtils.hasText(text)) {
      return null;
    }
    for (FieldPattern fieldPattern : FIELD_PATTERNS) {
      if (fieldPattern.standalonePattern().matcher(text.trim()).matches()) {
        return fieldPattern.field();
      }
    }
    return null;
  }

  private void detectPaymentAmounts(
      XWPFDocument document, Map<String, String> detectedFields, List<String> warnings) {
    int scheduleIndex = 0;
    for (XWPFTable table : document.getTables()) {
      List<XWPFTableRow> rows = table.getRows();
      for (int headerRow = 0; headerRow < rows.size(); headerRow++) {
        List<XWPFTableCell> headerCells = rows.get(headerRow).getTableCells();
        int percentColumn = findColumn(headerCells, "付款比例");
        int amountColumn = findColumn(headerCells, "金额");
        if (percentColumn < 0 || amountColumn < 0) {
          continue;
        }
        scheduleIndex++;
        int itemIndex = 0;
        for (int rowIndex = headerRow + 1; rowIndex < rows.size(); rowIndex++) {
          List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();
          if (percentColumn >= cells.size() || amountColumn >= cells.size()) {
            continue;
          }
          String percentText = cells.get(percentColumn).getText().trim();
          String amountText = cells.get(amountColumn).getText().trim();
          Matcher percentMatcher = PAYMENT_PERCENT_PATTERN.matcher(percentText);
          if (!percentMatcher.matches() || !StringUtils.hasText(amountText)) {
            continue;
          }
          itemIndex++;
          String field = "paymentAmount." + scheduleIndex + "." + itemIndex + "."
              + percentMatcher.group(1);
          if (detectedFields.putIfAbsent(field, amountText) != null) {
            warnings.add("conflicting_values:" + field);
          }
        }
        break;
      }
    }
  }

  private int findColumn(List<XWPFTableCell> cells, String label) {
    for (int index = 0; index < cells.size(); index++) {
      if (label.equals(cells.get(index).getText().trim())) {
        return index;
      }
    }
    return -1;
  }

  private String textOf(XWPFParagraph paragraph) {
    if (paragraph.getRuns().isEmpty()) {
      return paragraph.getText() == null ? "" : paragraph.getText();
    }
    StringBuilder value = new StringBuilder();
    for (XWPFRun run : paragraph.getRuns()) {
      String text = run.text();
      if (text != null) {
        value.append(text);
      }
    }
    return value.toString();
  }

  private void replaceRangePreservingRuns(
      XWPFParagraph paragraph,
      int start,
      int end,
      String expected,
      String replacement) {
    List<XWPFRun> runs = paragraph.getRuns();
    String fullText = textOf(paragraph);
    if (start < 0 || end > fullText.length() || start >= end
        || !fullText.substring(start, end).equals(expected)) {
      throw new BusinessException(ErrorCode.CONFLICT, "Contract source anchor text changed");
    }
    if (runs.isEmpty()) {
      XWPFRun run = paragraph.createRun();
      run.setText(fullText.substring(0, start) + replacement + fullText.substring(end));
      return;
    }

    int cursor = 0;
    int startRun = -1;
    int endRun = -1;
    int startOffset = -1;
    int endOffset = -1;
    for (int i = 0; i < runs.size(); i++) {
      String runText = runs.get(i).text();
      int length = runText == null ? 0 : runText.length();
      int next = cursor + length;
      if (startRun < 0 && start >= cursor && start < next) {
        startRun = i;
        startOffset = start - cursor;
      }
      if (endRun < 0 && end > cursor && end <= next) {
        endRun = i;
        endOffset = end - cursor;
        break;
      }
      cursor = next;
    }
    if (startRun < 0 || endRun < 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "Contract source anchor run mapping changed");
    }

    String firstText = safeRunText(runs.get(startRun));
    if (startRun == endRun) {
      setRunText(runs.get(startRun),
          firstText.substring(0, startOffset) + replacement + firstText.substring(endOffset));
      return;
    }
    setRunText(runs.get(startRun), firstText.substring(0, startOffset) + replacement);
    for (int i = startRun + 1; i < endRun; i++) {
      setRunText(runs.get(i), "");
    }
    String lastText = safeRunText(runs.get(endRun));
    setRunText(runs.get(endRun), lastText.substring(endOffset));
  }

  private String safeRunText(XWPFRun run) {
    return run.text() == null ? "" : run.text();
  }

  private void setRunText(XWPFRun run, String value) {
    if (run.getCTR().sizeOfTArray() == 0) {
      run.setText(value);
    } else {
      run.setText(value, 0);
      while (run.getCTR().sizeOfTArray() > 1) {
        run.getCTR().removeT(1);
      }
    }
  }

  private record FieldPattern(String field, Pattern inlinePattern, Pattern standalonePattern) {}
  private record ParagraphRef(String path, XWPFParagraph paragraph) {}
  private record AdjacentCandidate(String field, String path, String value) {}
  private record ScanContext(List<ParagraphRef> paragraphs, List<AdjacentCandidate> adjacentCandidates) {}

  public record Anchor(
      String anchorId,
      String field,
      String path,
      int start,
      int end,
      String originalValue,
      List<String> allowedOperations
  ) {}

  public record ContractSourceManifest(
      String schema,
      String rendererVersion,
      Map<String, String> detectedFields,
      List<Anchor> anchors,
      List<String> warnings
  ) {}

  public record AnalysisResult(ContractSourceManifest manifest, String derivationStatus) {}
}
