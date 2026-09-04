package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import java.util.List;
import java.util.Map;

/** 指引口径 prompt 构造器（方案 §7） */
public final class TheoryGenPromptBuilder {

  private TheoryGenPromptBuilder() {
  }

  /** 每等级默认题量配置（指引 §2.1 下限） */
  public static Map<String, Integer> defaultQuota(int level) {
    return switch (level) {
      // 五级/四级：单选≥4、判断≥2
      case 5, 4 -> Map.of("single", 4, "judge", 2);
      // 三级：单选≥3、多选≥2、判断≥2
      case 3 -> Map.of("single", 3, "multi", 2, "judge", 2);
      // 二级/一级：单选≥3、多选≥2、判断≥2 + 主观题（简答类每 5 考评点 1 题的简化配额由提交方调整）
      default -> Map.of("single", 3, "multi", 2, "judge", 2);
    };
  }

  /** 题型代码映射（指引 §2.2） */
  public static String typeCodeOf(String questionType) {
    return switch (questionType) {
      case "single" -> "B";
      case "judge" -> "C";
      case "multi" -> "D";
      case "essay", "cal", "draw" -> "E";
      case "discuss" -> "F";
      case "case_analysis" -> "I";
      default -> "?";
    };
  }

  public static String buildSystemPrompt(int level, Map<String, Integer> quota) {
    boolean subjective = level <= 2;
    StringBuilder sb = new StringBuilder();
    sb.append("你是广西技能人才评价题库的资深命题专家，依据《广西技能人才评价题库开发工作指引（试行）》命制理论知识试题。")
        .append("本次针对的技能等级为").append(levelName(level)).append("。")
        .append("命题硬性要求：\n")
        .append("1. 严格按考评点命制，不得超出考评点知识范围，也不得缩小范围；考查考评点核心内容，避免单纯数字/文字记忆。\n")
        .append("2. 单选题：4 个备选项，仅 1 个正确答案；干扰项须有效（考生易混淆、与正确答案相近但非正确）；题干为空缺句，空缺部分用（ ）表示，句尾用句号；避免疑问句、否定句式、复杂句式。\n")
        .append("3. 多选题：5 个备选项（A~E），正确答案 2 个及以上；题干空缺句同单选。\n")
        .append("4. 判断题：陈述句题干，答案用 √ 或 ×。\n")
        .append("5. 选项标号用英文大写字母；正确答案字母随机分布，不要集中于同一选项。\n")
        .append("6. 难度按 1易 2较易 3中 4较难 5难 标定，以 3 为宜；1 和 5 的题合计不超过 20%。难度须符合")
        .append(levelName(level)).append("的认知定位，不人为提高或降低。\n")
        .append("7. 试题内容正确无争议，符合社会主义核心价值观，不得出现违反法律法规、民族歧视、地域歧视、性别歧视等内容。\n")
        .append("8. 术语规范，采用法定计量单位；标点准确；答案唯一。\n");
    if (subjective) {
      sb.append("9. 主观题（简答/计算/绘图/论述/案例分析）：须给出参考答案、评分要点、评分标准；")
          .append("评分要点逐条标注分值且分值之和等于建议分值；每条要点分值不超过 2 分（5 分制题型）或 3 分（10/15 分制题型）；")
          .append("建议分值：简答/计算/绘图 5 分，论述/案例分析 10 或 15 分。\n");
    }
    sb.append("\n输出要求：只输出 JSON 数组，不要输出任何其他文字、解释或 markdown 围栏。数组元素结构：\n")
        .append("{\"questionType\":\"single|multi|judge")
        .append(subjective ? "|essay|cal|draw|discuss|case_analysis" : "")
        .append("\",\"typeCode\":\"B|C|D")
        .append(subjective ? "|E|F|I" : "")
        .append("\",\"difficulty\":1-5,\"content\":\"题干\",\"options\":[{\"label\":\"A\",\"content\":\"...\"}]（客观题必填，主观题为 null）")
        .append(",\"answer\":\"字母或√×\",\"analysis\":\"解析\",\"referenceAnswer\":\"参考答案（主观题）\"")
        .append(",\"scorePoints\":\"评分要点含分值（主观题）\",\"scoringStandard\":\"评分标准（主观题）\",\"score\":建议分值（主观题）}\n")
        .append("本次每个考评点的题量配额：").append(quotaText(quota)).append("。按配额足量输出。");
    return sb.toString();
  }

  public static String buildUserPrompt(TheoryBlueprintNodeEntity point, Map<String, Integer> quota) {
    StringBuilder sb = new StringBuilder();
    sb.append("考评点代码：").append(point.getFullCode()).append("\n")
        .append("考评点名称：").append(point.getName()).append("\n")
        .append("重要程度：").append(point.getImportance() == null ? "X" : point.getImportance()).append("\n");
    if (point.getRelatedKnowledge() != null && !point.getRelatedKnowledge().isBlank()) {
      sb.append("相关知识要求：").append(point.getRelatedKnowledge()).append("\n");
    }
    sb.append("请按系统提示中的题量配额（").append(quotaText(quota)).append("）为该考评点生成试题，只输出 JSON 数组。");
    return sb.toString();
  }

  private static String quotaText(Map<String, Integer> quota) {
    List<String> parts = new java.util.ArrayList<>();
    quota.forEach((k, v) -> parts.add(typeName(k) + v + "道"));
    return String.join("、", parts);
  }

  private static String typeName(String t) {
    return switch (t) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "essay" -> "简答题";
      case "cal" -> "计算题";
      case "draw" -> "绘图题";
      case "discuss" -> "论述题";
      case "case_analysis" -> "案例分析题";
      default -> t;
    };
  }

  private static String levelName(int level) {
    return switch (level) {
      case 5 -> "五级/初级工";
      case 4 -> "四级/中级工";
      case 3 -> "三级/高级工";
      case 2 -> "二级/技师";
      case 1 -> "一级/高级技师";
      default -> level + "级";
    };
  }
}
