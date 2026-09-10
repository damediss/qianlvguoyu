package com.guoyu.platform.exam.skillgen.service;

import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;

/** 操作技能命题 prompt 构造器（方案 v1.1 §7） */
public final class SkillGenPromptBuilder {

  private SkillGenPromptBuilder() {
  }

  /** 输出格式示例（稳定前缀的一部分：缓存命中后边际成本极低；内容明确禁止照抄） */
  private static final String EXAMPLE = """
      [{
        "name": "联机启动集控室监控设备",
        "durationMinutes": 20,
        "examForm": "实操",
        "sitePrepare": {
          "note": "考核前由考评员确认设备处于断电状态",
          "equipment": [
            {"name": "集控室监控主机", "spec": "由考核点配置", "unit": "套", "qty": "1", "remark": "含配套软件"},
            {"name": "万用表", "spec": "数字式", "unit": "台", "qty": "1", "remark": ""}
          ],
          "faultSetup": "按考核点条件设置一处通信异常（三选一）"
        },
        "examineePrepare": "黑色签字笔、绝缘手套",
        "requirements": "请按操作规程完成集控室监控设备联机启动，并确认各监控点位通信正常",
        "negatives": "若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违反设备操作、工具使用等安全规定的严重错误",
        "scoring": [
          {"item": "开机前安全检查", "score": "20", "scoreDesc": "未确认断电状态扣10分，未检查接地扣10分", "maxDeduct": "20", "step": "10"},
          {"item": "联机启动操作", "score": "50", "scoreDesc": "操作步骤漏一项扣10分，顺序错误扣20分", "maxDeduct": "50", "step": "10"},
          {"item": "通信状态确认", "score": "30", "scoreDesc": "未逐项确认监控点位通信状态每项扣10分", "maxDeduct": "30", "step": "10"}
        ]
      }]""";

  public static String buildSystemPrompt(int level) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是广西技能人才评价题库的资深操作技能命题专家，命制操作技能考核试题。")
        .append("以下命题规则为《广西技能人才评价题库开发工作指引（试行）》的硬性要求，必须严格遵守；")
        .append("规则未覆盖之处按通用命题规范处理，不得臆造指引条款。")
        .append("本次针对的技能等级为").append(levelName(level)).append("。\n")
        .append("命题硬性要求：\n")
        .append("1. 每道试题对应一个考评点，考核内容为典型工作任务，不得超出该考评点的技能范围，也不得缩小范围；考核项目应具有代表性。\n")
        .append("2. 每道试题必须完整包含三部分：①准备要求（考场准备：含考核有关说明与设备设施准备清单[名称/规格/单位/数量/备注]、故障或工况设置说明；考生准备：工量具及其他准备）；")
        .append("②考核试题（具体考核要求，即考评点技能要求的细化，并给出否定项说明）；③评分标准（评分记录表）。\n")
        .append("3. 评分记录表规则：每行 = 一个评分要素，含 要素(item)/配分(score)/评分标准(scoreDesc)/最大扣分(maxDeduct)/步长(step，一次扣分的最小分值)；")
        .append("配分合计必须等于 100；最大扣分不得超过该行配分；步长不得超过该行最大扣分；评分要素应细化、量化、可操作，能区分考生水平。\n")
        .append("4. 否定项：针对关键步骤或安全环节，统一采用表述「若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：……」。\n")
        .append("5. 考核时间按任务复杂度合理设置（分钟），考核形式默认「实操」。\n")
        .append("6. 同一考评点下的多道试题必须覆盖不同工况、不同侧面或不同参数组合，不得同义改述、不得雷同。\n")
        .append("7. 试题内容正确无争议，符合社会主义核心价值观，不得出现违反法律法规、民族歧视、地域歧视、性别歧视等内容；术语规范，标点准确。\n")
        .append("考评点重要程度档位：X=核心要素（重点考查、优先覆盖），Y=重要要素，Z=一般要素。\n")
        .append("\n输出要求：只输出 JSON 数组，不要输出任何其他文字、解释或 markdown 围栏。数组元素结构：\n")
        .append("{\"name\":\"试题名称\",\"durationMinutes\":整数分钟,\"examForm\":\"实操\",")
        .append("\"sitePrepare\":{\"note\":\"考核有关说明(可空)\",")
        .append("\"equipment\":[{\"name\":\"…\",\"spec\":\"…\",\"unit\":\"…\",\"qty\":\"…\",\"remark\":\"…\"}],")
        .append("\"faultSetup\":\"故障/工况设置说明(可空)\"},")
        .append("\"examineePrepare\":\"考生准备\",\"requirements\":\"具体考核要求\",\"negatives\":\"否定项说明\",")
        .append("\"scoring\":[{\"item\":\"评分要素\",\"score\":\"配分\",\"scoreDesc\":\"评分标准\",\"maxDeduct\":\"最大扣分\",\"step\":\"步长\"}]}\n")
        .append("scoring 中的配分/最大扣分/步长用数字字符串；配分合计必须为 100。\n")
        .append("\n输出示例（仅示范格式与颗粒度，内容不得照抄）：\n").append(EXAMPLE);
    return sb.toString();
  }

  /** 生成数量说明（一期固定 5 题/点） */
  public static int fixedQuota() {
    return 5;
  }

  public static String buildUserPrompt(SkillBlueprintNodeEntity point,
      SkillBlueprintNodeEntity l2Node, SkillBlueprintNodeEntity l1Node, int questionCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("考评点代码：").append(point.getFullCode()).append("\n")
        .append("考评点名称：").append(point.getName()).append("\n")
        .append("重要程度：").append(point.getImportance() == null ? "X" : point.getImportance()).append("\n");
    if (l1Node != null) {
      sb.append("所属考评范围（一级）：").append(nvl(l1Node.getName()))
          .append("，权重 ").append(weightText(l1Node)).append("%")
          .append("，选考方式 ").append(nvl(l1Node.getSelectMode())).append("\n");
    }
    if (l2Node != null) {
      sb.append("所属考评范围（二级）：").append(nvl(l2Node.getName()))
          .append("，权重 ").append(weightText(l2Node)).append("%")
          .append("，选考方式 ").append(nvl(l2Node.getSelectMode())).append("\n");
      if (l2Node.getSkillRequirement() != null && !l2Node.getSkillRequirement().isBlank()) {
        sb.append("技能要求原文：\n").append(l2Node.getSkillRequirement()).append("\n");
      }
    }
    sb.append("请为该考评点生成 ").append(questionCount).append(" 道完整试题")
        .append("（每道均含准备要求、考核试题、评分标准三部分；本题分值 100 分；")
        .append("各题覆盖不同工况/侧面/参数组合）。只输出 JSON 数组。");
    return sb.toString();
  }

  private static String weightText(SkillBlueprintNodeEntity node) {
    return node.getWeight() == null ? "-" : node.getWeight().stripTrailingZeros().toPlainString();
  }

  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  public static String levelName(int level) {
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
