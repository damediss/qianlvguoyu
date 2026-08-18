package com.guoyu.platform.contract.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.junit.jupiter.api.Test;

class ContractDocxDerivationServiceTest {

  private static final String OLD_PARTY_A = "历史甲方科技有限公司";
  private static final String OLD_PARTY_B = "历史乙方服务有限公司";
  private static final String OLD_AMOUNT = "人民币100万元";
  private static final String OLD_CONTRACT_NUMBER = "HIST-SVC-2026-001";

  private final ContractDocxDerivationService service =
      new ContractDocxDerivationService(new ObjectMapper());

  @Test
  void derivesBodyTableHeaderAndFooterWhileKeepingRunStyle() throws Exception {
    byte[] source = createDerivableDocument();

    ContractDocxDerivationService.AnalysisResult analysis = service.analyze(source);

    assertEquals("derivable", analysis.derivationStatus());
    assertEquals(OLD_CONTRACT_NUMBER,
        analysis.manifest().detectedFields().get("contractNumber"));
    assertEquals(OLD_PARTY_A, analysis.manifest().detectedFields().get("partyA"));
    assertEquals(OLD_PARTY_B, analysis.manifest().detectedFields().get("partyB"));
    assertEquals(OLD_AMOUNT, analysis.manifest().detectedFields().get("amount"));
    assertTrue(analysis.manifest().anchors().stream()
        .anyMatch(anchor -> anchor.path().startsWith("header[")));
    assertTrue(analysis.manifest().anchors().stream()
        .anyMatch(anchor -> anchor.path().startsWith("footer[")));

    byte[] rendered = service.derive(source, analysis.manifest(), Map.of(
        "contractNumber", "NEW-SVC-2026-002",
        "partyA", "新甲方集团有限公司",
        "partyB", "新乙方数字科技有限公司",
        "amount", "人民币贰佰陆拾捌万元整（¥2,680,000.00）",
        "projectName", "企业知识服务平台",
        "serviceScope", "平台建设、部署及运维服务",
        "paymentAmount.1.1.30", "¥804,000.00",
        "paymentAmount.1.2.40", "¥1,072,000.00",
        "paymentAmount.1.3.25", "¥670,000.00",
        "paymentAmount.1.4.5", "¥134,000.00"
    ));

    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(rendered))) {
      String allText = allText(document);
      assertTrue(allText.contains("NEW-SVC-2026-002"));
      assertTrue(allText.contains("新甲方集团有限公司"));
      assertTrue(allText.contains("新乙方数字科技有限公司"));
      assertTrue(allText.contains("人民币贰佰陆拾捌万元整（¥2,680,000.00）"));
      assertTrue(allText.contains("¥804,000.00"));
      assertTrue(allText.contains("¥1,072,000.00"));
      assertTrue(allText.contains("¥670,000.00"));
      assertTrue(allText.contains("¥134,000.00"));
      assertTrue(allText.contains("企业知识服务平台"));
      assertTrue(allText.contains("平台建设、部署及运维服务"));
      assertFalse(allText.contains(OLD_PARTY_A));
      assertFalse(allText.contains(OLD_PARTY_B));
      assertFalse(allText.contains(OLD_AMOUNT));
      assertFalse(allText.contains(OLD_CONTRACT_NUMBER));

      XWPFParagraph partyAParagraph = document.getParagraphs().get(0);
      assertTrue(partyAParagraph.getRuns().get(0).isBold());
      XWPFRun replacementRun = partyAParagraph.getRuns().get(1);
      assertTrue(replacementRun.isItalic());
      assertEquals(12.0, replacementRun.getFontSizeAsDouble());
      assertEquals("宋体", replacementRun.getFontFamily());
    }
  }

  @Test
  void requiresReviewWhenRequiredAnchorIsMissing() throws Exception {
    byte[] source;
    try (XWPFDocument document = new XWPFDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.createParagraph().createRun().setText("甲方：某甲方公司");
      document.createParagraph().createRun().setText("乙方：某乙方公司");
      document.write(output);
      source = output.toByteArray();
    }

    ContractDocxDerivationService.AnalysisResult analysis = service.analyze(source);

    assertEquals("requires_review", analysis.derivationStatus());
    assertTrue(analysis.manifest().warnings().contains("missing_required_anchor:amount"));
  }

  @Test
  void producesThreeVisualRegressionPairsWithoutChangingDocumentTopology() throws Exception {
    List<byte[]> sources = List.of(
        createDerivableDocument(),
        createTableCentricDocument(),
        createMultiPageDocument());
    Path outputDir = Path.of("target", "contract-docx-regression");
    Files.createDirectories(outputDir);

    for (int index = 0; index < sources.size(); index++) {
      byte[] source = sources.get(index);
      ContractDocxDerivationService.AnalysisResult analysis = service.analyze(source);
      assertEquals("derivable", analysis.derivationStatus());
      byte[] rendered = service.derive(source, analysis.manifest(), Map.of(
          "contractNumber", "NEW-SVC-2026-00" + (index + 1),
          "partyA", "广西新甲方产业集团有限公司",
          "partyB", "广西新乙方数字科技有限公司",
          "amount", "人民币268万元",
          "projectName", "企业知识服务平台",
          "serviceScope", "平台建设、部署、培训及运维服务"));

      assertEquals(topologyOf(source), topologyOf(rendered));
      Files.write(outputDir.resolve("fixture-" + (index + 1) + "-source.docx"), source);
      Files.write(outputDir.resolve("fixture-" + (index + 1) + "-derived.docx"), rendered);
    }
  }

  private byte[] createDerivableDocument() throws Exception {
    try (XWPFDocument document = new XWPFDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      configureA4(document);
      XWPFParagraph partyA = document.createParagraph();
      XWPFRun label = partyA.createRun();
      label.setBold(true);
      label.setText("甲方：");
      XWPFRun valueStart = partyA.createRun();
      valueStart.setItalic(true);
      valueStart.setFontFamily("宋体");
      valueStart.setFontSize(12);
      valueStart.setText("历史甲方科技");
      XWPFRun valueEnd = partyA.createRun();
      valueEnd.setItalic(true);
      valueEnd.setFontFamily("宋体");
      valueEnd.setFontSize(12);
      valueEnd.setText("有限公司");

      XWPFTable partyBTable = document.createTable(1, 2);
      partyBTable.getRow(0).getCell(0).setText("乙方：");
      partyBTable.getRow(0).getCell(1).setText(OLD_PARTY_B);
      document.createParagraph().createRun().setText("合同金额：" + OLD_AMOUNT);
      document.createParagraph().createRun().setText("合同编号：" + OLD_CONTRACT_NUMBER);
      document.createParagraph().createRun().setText("项目名称：旧项目名称");
      document.createParagraph().createRun().setText("服务范围：旧服务内容");

      XWPFTable paymentTable = document.createTable(5, 4);
      String[][] payments = {
          {"阶段", "付款比例", "付款条件", "金额"},
          {"首付款", "30%", "合同生效", "¥300,000.00"},
          {"里程碑款", "40%", "进入试运行", "¥400,000.00"},
          {"验收款", "25%", "终验通过", "¥250,000.00"},
          {"质保尾款", "5%", "质保条件满足", "¥50,000.00"}
      };
      for (int row = 0; row < payments.length; row++) {
        for (int column = 0; column < payments[row].length; column++) {
          paymentTable.getRow(row).getCell(column).setText(payments[row][column]);
        }
      }

      XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
      header.createParagraph().createRun().setText("内部资料 - " + OLD_PARTY_A);
      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      footer.createParagraph().createRun().setText("签署方：" + OLD_PARTY_B);

      document.write(output);
      return output.toByteArray();
    }
  }

  private byte[] createTableCentricDocument() throws Exception {
    try (XWPFDocument document = new XWPFDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      configureA4(document);
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
      XWPFRun titleRun = title.createRun();
      titleRun.setBold(true);
      titleRun.setFontFamily("黑体");
      titleRun.setFontSize(18);
      titleRun.setText("软件技术服务框架协议");

      XWPFTable table = document.createTable(6, 2);
      String[][] values = {
          {"合同编号：", OLD_CONTRACT_NUMBER},
          {"甲方：", OLD_PARTY_A},
          {"乙方：", OLD_PARTY_B},
          {"合同金额：", OLD_AMOUNT},
          {"项目名称：", "旧知识服务平台"},
          {"服务范围：", "旧平台建设与运维服务"}
      };
      for (int row = 0; row < values.length; row++) {
        table.getRow(row).getCell(0).setText(values[row][0]);
        table.getRow(row).getCell(1).setText(values[row][1]);
        table.getRow(row).getCell(0).setWidth("2400");
        table.getRow(row).getCell(1).setWidth("6500");
      }
      document.createParagraph().createRun().setText("第一条 合作原则");
      document.createParagraph().createRun().setText(
          "双方依据本协议约定开展软件平台建设、实施和持续服务。");
      XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
      header.createParagraph().createRun().setText("技术服务协议 - " + OLD_PARTY_A);
      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      footer.createParagraph().createRun().setText("服务提供方：" + OLD_PARTY_B);
      document.write(output);
      return output.toByteArray();
    }
  }

  private byte[] createMultiPageDocument() throws Exception {
    try (XWPFDocument document = new XWPFDocument();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      configureA4(document);
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
      XWPFRun titleRun = title.createRun();
      titleRun.setBold(true);
      titleRun.setFontFamily("宋体");
      titleRun.setFontSize(20);
      titleRun.setText("信息化项目技术服务合同");
      document.createParagraph().createRun().setText("合同编号：" + OLD_CONTRACT_NUMBER);
      document.createParagraph().createRun().setText("甲方：" + OLD_PARTY_A);
      document.createParagraph().createRun().setText("乙方：" + OLD_PARTY_B);
      document.createParagraph().createRun().setText("合同金额：" + OLD_AMOUNT);
      document.createParagraph().createRun().setText("项目名称：旧数字化转型项目");
      document.createParagraph().createRun().setText("服务内容：旧系统实施及技术支持");

      for (int clause = 1; clause <= 14; clause++) {
        XWPFParagraph heading = document.createParagraph();
        XWPFRun headingRun = heading.createRun();
        headingRun.setBold(true);
        headingRun.setText("第" + clause + "条 服务条款");
        document.createParagraph().createRun().setText(
            "本条约定项目实施、交付、验收、保密及持续运维责任。双方应按照确认的计划协同执行，"
                + "并保留必要的交付与沟通记录。");
        if (clause == 7) {
          document.createParagraph().createRun().addBreak(BreakType.PAGE);
        }
      }
      XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
      header.createParagraph().createRun().setText("内部合同文件");
      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      footer.createParagraph().createRun().setText(OLD_PARTY_A + " / " + OLD_PARTY_B);
      document.write(output);
      return output.toByteArray();
    }
  }

  private DocumentTopology topologyOf(byte[] bytes) throws Exception {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      int headerParagraphs = document.getHeaderList().stream()
          .mapToInt(header -> header.getParagraphs().size()).sum();
      int footerParagraphs = document.getFooterList().stream()
          .mapToInt(footer -> footer.getParagraphs().size()).sum();
      return new DocumentTopology(
          document.getParagraphs().size(),
          document.getTables().size(),
          document.getHeaderList().size(),
          document.getFooterList().size(),
          headerParagraphs,
          footerParagraphs);
    }
  }

  private void configureA4(XWPFDocument document) {
    CTBody body = document.getDocument().getBody();
    CTSectPr section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    CTPageSz pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
    pageSize.setW(BigInteger.valueOf(11906));
    pageSize.setH(BigInteger.valueOf(16838));
    CTPageMar margins = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
    margins.setTop(BigInteger.valueOf(1440));
    margins.setRight(BigInteger.valueOf(1440));
    margins.setBottom(BigInteger.valueOf(1440));
    margins.setLeft(BigInteger.valueOf(1440));
    margins.setHeader(BigInteger.valueOf(720));
    margins.setFooter(BigInteger.valueOf(720));
  }

  private record DocumentTopology(
      int bodyParagraphs,
      int bodyTables,
      int headers,
      int footers,
      int headerParagraphs,
      int footerParagraphs) {}

  private String allText(XWPFDocument document) {
    List<String> values = new ArrayList<>();
    document.getParagraphs().forEach(paragraph -> values.add(paragraph.getText()));
    document.getTables().forEach(table -> values.add(table.getText()));
    document.getHeaderList().forEach(header -> {
      header.getParagraphs().forEach(paragraph -> values.add(paragraph.getText()));
      header.getTables().forEach(table -> values.add(table.getText()));
    });
    document.getFooterList().forEach(footer -> {
      footer.getParagraphs().forEach(paragraph -> values.add(paragraph.getText()));
      footer.getTables().forEach(table -> values.add(table.getText()));
    });
    return String.join("\n", values);
  }
}
