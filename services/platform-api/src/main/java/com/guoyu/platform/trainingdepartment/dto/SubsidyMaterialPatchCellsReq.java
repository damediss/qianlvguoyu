package com.guoyu.platform.trainingdepartment.dto;

import java.util.Map;

/** 在线编辑：按单元格地址更新文本，后端 POI 写回以保留边框/合并等格式 */
public class SubsidyMaterialPatchCellsReq {
  /** 如 {"J2":"申报日期：2026年09月08日","C10":"高级2班"} */
  private Map<String, String> cells;

  public Map<String, String> getCells() {
    return cells;
  }

  public void setCells(Map<String, String> cells) {
    this.cells = cells;
  }
}
