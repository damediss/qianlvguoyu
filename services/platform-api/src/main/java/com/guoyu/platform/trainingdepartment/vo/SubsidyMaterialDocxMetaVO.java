package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/** Word 可编辑段落摘要（标题 / 元信息 / 图注） */
public record SubsidyMaterialDocxMetaVO(
    Long instanceId,
    Long versionId,
    String fileName,
    List<Line> lines) {

  public record Line(int index, String kind, String label, String value, String text) {}
}
