package com.guoyu.platform.trainingdepartment.dto;

import java.util.Map;

/** Word 轻量编辑：按段落索引写回整段文本 */
public class SubsidyMaterialPatchDocxReq {
  /** 段落索引 → 整段新文本，如 {"0":"标题","2":"培训时间：2026-08-18至2026-08-19"} */
  private Map<Integer, String> paragraphs;

  public Map<Integer, String> getParagraphs() {
    return paragraphs;
  }

  public void setParagraphs(Map<Integer, String> paragraphs) {
    this.paragraphs = paragraphs;
  }
}
