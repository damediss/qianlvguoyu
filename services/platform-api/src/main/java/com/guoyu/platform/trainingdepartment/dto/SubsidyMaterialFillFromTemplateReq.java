package com.guoyu.platform.trainingdepartment.dto;

/** 从模板创建材料实例并自动填数 */
public class SubsidyMaterialFillFromTemplateReq {

  private Long templateId;
  /** 可选：绑定到现有槽位实例；空则新建自定义材料实例 */
  private Long instanceId;
  private String slotCode;
  private String title;
  /** 花名册学员范围：all（默认）/ certified（仅已获证） */
  private String rosterScope;
  /**
   * 为 true 时：以材料实例当前版本文件为模板填数（用于「替换上传」后的填数）。
   * 此时可不传 templateId。
   */
  private Boolean useCurrentFile;

  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public Long getInstanceId() { return instanceId; }
  public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
  public String getSlotCode() { return slotCode; }
  public void setSlotCode(String slotCode) { this.slotCode = slotCode; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getRosterScope() { return rosterScope; }
  public void setRosterScope(String rosterScope) { this.rosterScope = rosterScope; }
  public Boolean getUseCurrentFile() { return useCurrentFile; }
  public void setUseCurrentFile(Boolean useCurrentFile) { this.useCurrentFile = useCurrentFile; }
}
