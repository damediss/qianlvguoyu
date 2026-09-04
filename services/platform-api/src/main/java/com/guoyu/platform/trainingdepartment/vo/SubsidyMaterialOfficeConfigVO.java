package com.guoyu.platform.trainingdepartment.vo;

import java.util.Map;

public record SubsidyMaterialOfficeConfigVO(
    boolean enabled,
    String message,
    String documentServerUrl,
    String apiScriptUrl,
    Map<String, Object> config
) {
  public static SubsidyMaterialOfficeConfigVO disabled(String message) {
    return new SubsidyMaterialOfficeConfigVO(false, message, null, null, null);
  }
}
