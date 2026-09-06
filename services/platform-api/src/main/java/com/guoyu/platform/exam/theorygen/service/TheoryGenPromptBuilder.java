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

  /** 输出格式示例（稳定前缀的一部分：缓存命中后边际成本极低；内容明确禁止照抄） */
  private static final String EXAMPLES_OBJECTIVE =
      "[{\"questionType\":\"single\",\"typeCode\":\"B\",\"difficulty\":3,"
          + "\"content\":\"低压断路器在电路中的主要作用是（ ）。\","
          + "\"options\":[{\"label\":\"A\",\"content\":\"实现过载与短路保护\"},"
          + "{\"label\":\"B\",\"content\":\"仅用于隔离电源\"},"
          + "{\"label\":\"C\",\"content\":\"提高功率因数\"},"
          + "{\"label\":\"D\",\"content\":\"滤除电路谐波\"}],"
          + "\"answer\":\"A\",\"analysis\":\"断路器具有过载与短路保护职能，故为主要作用；"
          + "隔离电源是隔离开关的职能，功率因数补偿属无功补偿装置，谐波滤除属滤波装置，均为干扰项。\"},"
          + "{\"questionType\":\"judge\",\"typeCode\":\"C\",\"difficulty\":2,"
          + "\"content\":\"使用低压验电笔前，应先在已知带电体上验证其是否完好。\","
          + "\"options\":null,\"answer\":\"√\","
          + "\"analysis\":\"验电笔使用前须先验证功能完好，否则可能漏判带电引发触电，故表述正确。\"}]";

  /** 主观题示例（仅 level≤2 时拼入）：评分要点分值之和=建议分值、单条要点≤2 分（5 分制） */
  private static final String EXAMPLE_SUBJECTIVE =
      "{\"questionType\":\"essay\",\"typeCode\":\"E\",\"difficulty\":3,"
          + "\"content\":\"简述检修作业安全用电的基本要求。\",\"options\":null,\"answer\":\"\","
          + "\"analysis\":\"考查检修作业安全用电规范的掌握程度。\","
          + "\"referenceAnswer\":\"检修前须停电、验电并挂牌上锁；使用合格的绝缘工具并按规定佩戴防护用品；严禁单人带电作业。\","
          + "\"scorePoints\":\"停电验电挂牌上锁（2分）；绝缘工具与防护用品（2分）；严禁单人带电作业（1分）\","
          + "\"scoringStandard\":\"每点按表述完整度给分，答出要点给满分，表述不完整酌情减分\","
          + "\"score\":5}";

  public static String buildSystemPrompt(int level, Map<String, Integer> quota) {
    boolean subjective = level <= 2;
    StringBuilder sb = new StringBuilder();
    sb.append("你是广西技能人才评价题库的资深命题专家，命制理论知识试题。")
        .append("以下命题规则为《广西技能人才评价题库开发工作指引（试行）》的硬性要求，必须严格遵守；")
        .append("规则未覆盖之处按通用命题规范处理，不得臆造指引条款。")
        .append("本次针对的技能等级为").append(levelName(level)).append("。")
        .append("命题硬性要求：\n")
        .append("1. 严格按考评点命制，不得超出考评点知识范围，也不得缩小范围；考查考评点核心内容，避免单纯数字/文字记忆。\n")
        .append("2. 单选题：4 个备选项，仅 1 个正确答案；干扰项须有效（考生易混淆、与正确答案相近但非正确）；题干为空缺句，空缺部分用（ ）表示，句尾用句号；避免疑问句、否定句式、复杂句式。\n")
        .append("3. 多选题：5 个备选项（A~E），正确答案 2 个及以上；题干空缺句同单选。\n")
        .append("4. 判断题：陈述句题干，答案用 √ 或 ×。\n")
        .append("5. 选项标号用英文大写字母；正确答案按内容合理性放置即可，系统会统一随机重排选项位置，无需刻意平衡字母分布。\n")
        .append("6. 难度按 1易 2较易 3中 4较难 5难 标定；每块试题难度默认以 3（中）为主，1（易）和 5（难）合计至多 1 道；难度须符合")
        .append(levelName(level)).append("的认知定位，不人为提高或降低。\n")
        .append("7. 解析须点明正确项（或判断成立与否）的依据，并至少辨析一个主要干扰项为何错误。\n")
        .append("8. 试题内容正确无争议，符合社会主义核心价值观，不得出现违反法律法规、民族歧视、地域歧视、性别歧视等内容。\n")
        .append("9. 术语规范，采用法定计量单位；标点准确；答案唯一。\n");
    if (subjective) {
      sb.append("10. 主观题（简答/计算/绘图/论述/案例分析）：须给出参考答案、评分要点、评分标准；")
          .append("评分要点逐条标注分值且分值之和等于建议分值；每条要点分值不超过 2 分（5 分制题型）或 3 分（10/15 分制题型）；")
          .append("建议分值：简答/计算/绘图 5 分，论述/案例分析 10 或 15 分。\n");
    }
    sb.append("考评点重要程度档位：X=核心要素（应重点考查、优先覆盖），Y=重要要素，Z=一般要素。\n")
        .append("\n输出要求：只输出 JSON 数组，不要输出任何其他文字、解释或 markdown 围栏。数组元素结构：\n")
        .append("{\"questionType\":\"single|multi|judge")
        .append(subjective ? "|essay|cal|draw|discuss|case_analysis" : "")
        .append("\",\"typeCode\":\"B|C|D")
        .append(subjective ? "|E|F|I" : "")
        .append("\",\"difficulty\":1-5,\"content\":\"题干\",\"options\":[{\"label\":\"A\",\"content\":\"...\"}]（客观题必填，主观题为 null）")
        .append(",\"answer\":\"字母或√×\",\"analysis\":\"解析\",\"referenceAnswer\":\"参考答案（主观题）\"")
        .append(",\"scorePoints\":\"评分要点含分值（主观题）\",\"scoringStandard\":\"评分标准（主观题）\",\"score\":建议分值（主观题）}\n");
    sb.append("\n输出示例（仅示范格式与解析口径，内容不得照抄）：\n").append(EXAMPLES_OBJECTIVE);
    if (subjective) {
      sb.append("\n主观题示例：\n").append(EXAMPLE_SUBJECTIVE);
    }
    sb.append("\n本次每个考评点的题量配额：").append(quotaText(quota)).append("。按配额足量输出。");
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
