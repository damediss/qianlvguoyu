package com.guoyu.platform.knowledge.service;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.file.entity.FileEntity;

public interface KnowledgeDocumentParser {

  String extractText(FileEntity file, String rawText);
}
