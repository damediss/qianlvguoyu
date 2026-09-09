package com.guoyu.platform.trainingdepartment.support;

import org.springframework.util.StringUtils;

/** 补贴材料类型与导出目录映射 */
public final class SubsidyMaterialTypes {

  public static final String APPLICATION_FORM = "application_form";
  public static final String ROSTER = "roster";
  public static final String SUMMARY = "summary";
  public static final String PHOTO = "photo";
  public static final String CERTIFICATE = "certificate";
  public static final String OTHER = "other";

  private SubsidyMaterialTypes() {}

  public static String resolveMaterialType(String slotCode, String renderer) {
    if ("follow_photo".equals(renderer)) {
      return PHOTO;
    }
    if (!StringUtils.hasText(slotCode)) {
      return OTHER;
    }
    String code = slotCode.toLowerCase();
    if (code.contains("photo")) {
      return PHOTO;
    }
    if (code.contains("cert") || code.contains("certificate")) {
      return CERTIFICATE;
    }
    if (code.contains("roster") || code.contains("claim")) {
      return ROSTER;
    }
    if (code.contains("summary")) {
      return SUMMARY;
    }
    if (code.contains("apply") || code.contains("fund") || code.contains("cover")
        || code.contains("proxy") || code.contains("agreement") || code.contains("satisfaction")
        || code.contains("train_plan") || code.contains("contract") || code.contains("syllabus")) {
      return APPLICATION_FORM;
    }
    return OTHER;
  }

  public static String exportFolder(String materialType) {
    return switch (materialType) {
      case APPLICATION_FORM -> "01_申请表/";
      case ROSTER -> "02_花名册汇总/";
      case SUMMARY -> "02_花名册汇总/";
      case PHOTO -> "03_培训照片/";
      case CERTIFICATE -> "04_证书材料/";
      default -> "99_其他/";
    };
  }

  public static String materialTypeLabel(String materialType) {
    return switch (materialType) {
      case APPLICATION_FORM -> "申请表";
      case ROSTER -> "花名册";
      case SUMMARY -> "汇总表";
      case PHOTO -> "培训照片";
      case CERTIFICATE -> "证书材料";
      default -> "其他";
    };
  }

  public static int materialTypeOrder(String materialType) {
    return switch (materialType) {
      case APPLICATION_FORM -> 10;
      case ROSTER -> 20;
      case SUMMARY -> 25;
      case PHOTO -> 30;
      case CERTIFICATE -> 40;
      default -> 90;
    };
  }

  /**
   * 工作台默认清单：仅「培训照片」「证书材料」由配方/本班自动建项；
   * 申请表/花名册/汇总表等需「从模板生成」或替换上传后出现。
   */
  public static boolean isDefaultWorkbenchType(String materialType) {
    return PHOTO.equals(materialType) || CERTIFICATE.equals(materialType);
  }
}
