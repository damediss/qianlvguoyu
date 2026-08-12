package com.guoyu.platform.file.artifact.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.artifact.dto.ContractPreviewGenerateReq;
import com.guoyu.platform.file.artifact.vo.GeneratedArtifactEnvelopeVO;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import com.guoyu.platform.integration.bot.BotActorContext;
import com.guoyu.platform.integration.bot.BotActorContextHolder;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractPreviewGenerationService {

  private static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  private final FileApplicationService fileApplicationService;

  public ContractPreviewGenerationService(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional
  public GeneratedArtifactEnvelopeVO generate(ContractPreviewGenerateReq request) {
    BotActorContext actor = BotActorContextHolder.get();
    if (actor == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Verified bot actor context is required");
    }
    if (!actor.isPrivateChat()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Contract files can only be generated for a private chat");
    }

    String title = StringUtils.hasText(request.contractTitle())
        ? request.contractTitle().trim() : "信息化技术服务合同";
    String scope = StringUtils.hasText(request.serviceScope())
        ? request.serviceScope().trim() : "信息化技术服务及相关交付支持";
    String filename = sanitizeFilename(title) + "-本地E2E草稿.docx";
    byte[] content = renderDocx(title, request.partyA().trim(), request.partyB().trim(),
        request.amount().trim(), scope);
    FileVO stored = fileApplicationService.storeFromBytesForTenant(
        actor.tenantId(), content, filename, DOCX_MIME, "private");
    FileEntity file = fileApplicationService.getExistingFile(actor.tenantId(), stored.id());
    String id = String.valueOf(file.getId());
    var artifact = new GeneratedArtifactEnvelopeVO.GeneratedArtifactRefVO(
        "platform-file", id, id, file.getOriginalName(), file.getContentType(),
        file.getSizeBytes(), file.getSha256(), 1, true);
    return new GeneratedArtifactEnvelopeVO(
        "guoyu.generated-artifacts/v1", "contract", id, id, List.of(artifact));
  }

  private byte[] renderDocx(String title, String partyA, String partyB, String amount, String scope) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      XWPFParagraph heading = document.createParagraph();
      heading.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun headingRun = heading.createRun();
      headingRun.setBold(true);
      headingRun.setFontSize(18);
      headingRun.setText(title + "（草稿）");

      addParagraph(document, "甲方：" + partyA);
      addParagraph(document, "乙方：" + partyB);
      addParagraph(document, "合同金额：" + amount);
      addParagraph(document, "服务内容：" + scope);
      addParagraph(document, "第一条  双方应按照本合同约定履行各自义务，并就具体交付事项保持沟通。");
      addParagraph(document, "第二条  本合同为本地端到端联调生成的测试草稿，不具备法律效力，不得用于真实交易或签署。");
      addParagraph(document, "第三条  未尽事项由双方另行书面确认。");
      addParagraph(document, "生成日期：" + LocalDate.now());
      addParagraph(document, "测试标识：LOCAL-WEKNORA-MCP-E2E");
      document.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Contract preview rendering failed");
    }
  }

  private void addParagraph(XWPFDocument document, String value) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setSpacingAfter(160);
    XWPFRun run = paragraph.createRun();
    run.setFontSize(11);
    run.setText(value);
  }

  private String sanitizeFilename(String value) {
    String safe = value.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    return safe.isEmpty() ? "合同预览" : safe;
  }
}
