package com.guoyu.platform.knowledge.service;

import com.guoyu.platform.file.entity.FileEntity;

public interface KnowledgeDocumentParser {

  String extractText(FileEntity file, String rawText);
}
