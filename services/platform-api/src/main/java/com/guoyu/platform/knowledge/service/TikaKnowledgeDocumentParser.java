package com.guoyu.platform.knowledge.service;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

@Component
@Slf4j
public class TikaKnowledgeDocumentParser implements KnowledgeDocumentParser {

  private static final int MAX_EXTRACT_CHARS = 2_000_000;

  private final FileApplicationService fileApplicationService;

  public TikaKnowledgeDocumentParser(FileApplicationService fileApplicationService) {
    this.fileApplicationService = fileApplicationService;
  }

  @Override
  public String extractText(FileEntity file, String rawText) {
    if (StringUtils.hasText(rawText)) {
      return rawText;
    }
    Path path = fileApplicationService.localPath(file)
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Only local file parsing is available in current MVP"));
    if (!Files.exists(path)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "File content not found");
    }

    AutoDetectParser parser = new AutoDetectParser();
    BodyContentHandler handler = new BodyContentHandler(MAX_EXTRACT_CHARS);
    Metadata metadata = new Metadata();
    ParseContext context = new ParseContext();
    try (InputStream input = Files.newInputStream(path)) {
      parser.parse(input, handler, metadata, context);
      return handler.toString();
    } catch (IOException | SAXException | TikaException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Document parsing failed");
    }
  }
}
