package com.guoyu.platform.trainingdepartment.support;

/** 师资证照附件类型（与方案 file_type 一致） */
public final class TeacherBankFileTypes {

  public static final String ID_FRONT = "ID_FRONT";
  public static final String ID_BACK = "ID_BACK";
  public static final String GRADUATE = "GRADUATE";
  public static final String EDUCATION = "EDUCATION";
  public static final String TEACHER_QUAL = "TEACHER_QUAL";
  public static final String TITLE = "TITLE";
  public static final String SKILL = "SKILL";
  public static final String OTHER = "OTHER";

  private TeacherBankFileTypes() {}

  public static boolean isKnown(String type) {
    if (type == null) {
      return false;
    }
    return switch (type.trim()) {
      case ID_FRONT, ID_BACK, GRADUATE, EDUCATION, TEACHER_QUAL, TITLE, SKILL, OTHER -> true;
      default -> false;
    };
  }
}
