package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.Size;

/** WeKnora 配置保存请求（整条 upsert） */
public record FaqWeKnoraConfigSaveReq(
    @Size(max = 256) String webhookSecret,
    String enabledEvents,
    @Size(max = 128) String knowledgeBaseId,
    @Size(max = 128) String faqSetId,
    Integer webhookEnabled,
    Integer autoExport
) {
}
