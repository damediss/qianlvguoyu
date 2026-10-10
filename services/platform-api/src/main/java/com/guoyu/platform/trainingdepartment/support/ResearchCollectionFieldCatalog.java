package com.guoyu.platform.trainingdepartment.support;

import java.util.Arrays;
import java.util.List;

/** 企业调研人员采集精简版字段目录（与 Excel 模板 16 列 + 手机号对齐） */
public final class ResearchCollectionFieldCatalog {

  public static final int SCHEMA_VERSION = 2;

  public record FieldDef(
      String key,
      String defaultLabel,
      String defaultInputType,
      String defaultGroup,
      int defaultOrder,
      boolean locked
  ) {}

  public record GroupDef(String key, String defaultLabel, int defaultOrder) {}

  public static final List<GroupDef> DEFAULT_GROUPS = List.of(
      new GroupDef("basic", "基本信息", 1),
      new GroupDef("certificate", "技能等级证书", 2),
      new GroupDef("title", "职称信息", 3)
  );

  public static final List<FieldDef> ALL_FIELDS = List.of(
      new FieldDef("name", "姓名", "text", "basic", 1, true),
      new FieldDef("idCardNumber", "身份证号码", "text", "basic", 2, true),
      new FieldDef("phone", "手机号", "text", "basic", 3, false),
      new FieldDef("companyName", "所在单位", "text", "basic", 4, false),
      new FieldDef("major", "所学专业（初中、高中学历的填无）", "select", "basic", 5, false),
      new FieldDef("education", "学历层次", "select", "basic", 6, false),
      new FieldDef("workTime", "参加工作时间", "date", "basic", 7, false),
      new FieldDef("workTypeName", "原工种", "select", "basic", 8, false),
      new FieldDef("department", "部门", "select", "basic", 9, false),
      new FieldDef("job", "实际岗位名称", "select", "basic", 10, false),
      new FieldDef("workDateStart", "入职时间", "date", "basic", 11, false),
      new FieldDef("certLevel", "已有证书等级", "select", "certificate", 12, false),
      new FieldDef("certNo", "证书编号", "text", "certificate", 13, false),
      new FieldDef("certIssueDate", "发证日期", "date", "certificate", 14, false),
      new FieldDef("titleType", "职称", "select", "title", 15, false),
      new FieldDef("titleMajor", "职称专业", "text", "title", 16, false),
      new FieldDef("titleCertDate", "职称获证时间", "date", "title", 17, false)
  );

  public static FieldDef find(String key) {
    return ALL_FIELDS.stream().filter(f -> f.key().equals(key)).findFirst().orElse(null);
  }

  public static List<String> defaultLevelOptions() {
    return Arrays.asList("初级工", "中级工", "高级工", "技师", "高级技师");
  }

  public static List<String> defaultEducationOptions() {
    return Arrays.asList("初中", "高中", "中专", "大专", "本科", "研究生");
  }

  private ResearchCollectionFieldCatalog() {}
}
