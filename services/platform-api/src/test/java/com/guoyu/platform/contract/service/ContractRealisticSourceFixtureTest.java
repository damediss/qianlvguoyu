package com.guoyu.platform.contract.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class ContractRealisticSourceFixtureTest {

  private static final Path FIXTURE_DIR = Path.of(
      "..", "..", "docs", "04_requirements", "contract-test-fixtures").normalize();
  private static final List<String> FIXTURES = List.of(
      "01_软件技术服务框架协议_源文档.docx",
      "02_软件系统开发实施合同_源文档.docx",
      "03_信息平台运维服务合同_源文档.docx");
  private static final List<String> REQUIRED_FIELDS =
      List.of("contractNumber", "partyA", "partyB", "amount", "projectName", "serviceScope",
          "signingDate", "partyAContact", "partyAPhone", "partyBContact", "partyBPhone");

  private final ContractDocxDerivationService service =
      new ContractDocxDerivationService(new ObjectMapper());

  @Test
  void realisticSourceDocumentsAreDerivableAndKeepTopology() throws Exception {
    Path outputDir = Path.of("target", "contract-realistic-fixtures-derived");
    Files.createDirectories(outputDir);

    for (String filename : FIXTURES) {
      Path sourcePath = FIXTURE_DIR.resolve(filename);
      assertTrue(Files.isRegularFile(sourcePath), "Missing fixture: " + sourcePath);
      byte[] source = Files.readAllBytes(sourcePath);
      ContractDocxDerivationService.AnalysisResult analysis = service.analyze(source);

      assertEquals("derivable", analysis.derivationStatus(),
          () -> filename + " warnings=" + analysis.manifest().warnings());
      for (String field : REQUIRED_FIELDS) {
        assertTrue(analysis.manifest().detectedFields().containsKey(field),
            () -> filename + " missing field=" + field);
      }
      assertFalse(analysis.manifest().anchors().isEmpty());

      Map<String, String> replacements = new LinkedHashMap<>();
      replacements.put("contractNumber", "REAL-2026-0817");
      replacements.put("partyA", "广西新甲方产业集团有限公司");
      replacements.put("partyB", "广西新乙方数字科技有限公司");
      replacements.put("amount", "人民币贰佰陆拾捌万元（¥2,680,000.00）");
      replacements.put("projectName", "企业智能知识与合同协同平台项目");
      replacements.put("serviceScope", "平台建设、系统集成、业务培训、数据治理及持续运维服务");
      replacements.put("signingDate", "2026年8月17日");
      replacements.put("partyAContact", "张明");
      replacements.put("partyAPhone", "13800000001");
      replacements.put("partyBContact", "李华");
      replacements.put("partyBPhone", "13800000002");
      byte[] derived = service.derive(source, analysis.manifest(), replacements);

      assertEquals(topologyOf(source), topologyOf(derived), filename);
      Path derivedPath = outputDir.resolve(filename.replace("_源文档", "_派生验证"));
      Files.write(derivedPath, derived);
    }
  }

  private DocumentTopology topologyOf(byte[] content) throws Exception {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
      int sectionCount = document.getDocument().selectPath(
          "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
              + ".//w:sectPr").length;
      int tableRows = document.getTables().stream().mapToInt(table -> table.getRows().size()).sum();
      int tableCells = document.getTables().stream()
          .flatMap(table -> table.getRows().stream())
          .mapToInt(row -> row.getTableCells().size()).sum();
      int headerParagraphs = document.getHeaderList().stream()
          .mapToInt(header -> header.getParagraphs().size()).sum();
      int footerParagraphs = document.getFooterList().stream()
          .mapToInt(footer -> footer.getParagraphs().size()).sum();
      return new DocumentTopology(
          document.getParagraphs().size(),
          document.getTables().size(),
          tableRows,
          tableCells,
          sectionCount,
          document.getHeaderList().size(),
          document.getFooterList().size(),
          headerParagraphs,
          footerParagraphs);
    }
  }

  private record DocumentTopology(
      int bodyParagraphs,
      int bodyTables,
      int tableRows,
      int tableCells,
      int sections,
      int headers,
      int footers,
      int headerParagraphs,
      int footerParagraphs) {}
}
