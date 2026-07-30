package com.guoyu.platform.exam.service;

import java.util.Map;
import org.springframework.stereotype.Component;

/** LLM Prompt 组装器 — 含「国家职业标准」专家角色上下文 */
@Component
public class FaqLlmPromptBuilder {

  /** System Prompt 共用前缀 */
  private static final String SYSTEM_PROMPT = """
      你是一位「国家职业标准」专家。你熟知中华人民共和国人力资源和社会保障部颁布的各职业国家职业标准（Occupational Standard of the People's Republic of China），包括但不限于：

      - 职业定义、职业等级（五级/初级工、四级/中级工、三级/高级工、二级/技师、一级/高级技师）
      - 职业技能要求的知识体系和技能权重
      - 鉴定要求（申报条件、鉴定方式、考核时长）
      - 各行业标准术语表和行业规范

      你正在协助将职业技能鉴定试题转换为 FAQ 知识库条目。生成 FAQ 时请严格遵守以下原则：

      1. **国家标准优先**：使用国家职业标准中的规范术语和表述，不使用口语化或地方性用语
      2. **鉴定点导向**：生成的相似问/反例应覆盖同一鉴定点的不同表述方式
      3. **干扰项准确性**：negative_questions 应体现真实考试中常见的错误理解和国家标准中的区分边界
      4. **分级意识**：初级工侧重"是什么"，中高级工侧重"怎么做"和"为什么"，编制时要符合对应鉴定层级的认知要求

      每次收到题目，你都将当前题目涵盖的国家职业标准上下文（工种名称、职业等级、鉴定范围），再按要求生成输出。
      """;

  /**
   * 生成 similar_questions 的完整 prompt（system + user）。
   */
  public String buildSimilarQuestionsPrompt(Map<String, String> ctx) {
    return SYSTEM_PROMPT + "\n\n" + buildSimilarUserPrompt(ctx);
  }

  /**
   * 生成 negative_questions 的完整 prompt（system + user）。
   */
  public String buildNegativeQuestionsPrompt(Map<String, String> ctx) {
    return SYSTEM_PROMPT + "\n\n" + buildNegativeUserPrompt(ctx);
  }

  /**
   * 合并生成 similar_questions + negative_questions 的完整 prompt（system + user）。
   * 一次调用生成 3 条相似问 + 3 条反例，共享题目上下文。
   * 使用 buildLlmPromptContext 已有的键（knowledgePath/typeLabel/diffLabel/contentStripped/correctOptions/wrongOptions/keyConcepts）。
   */
  public String buildMergedPrompt(Map<String, String> ctx) {
    return SYSTEM_PROMPT + "\n\n" + buildMergedUserPrompt(ctx);
  }

  private String buildMergedUserPrompt(Map<String, String> ctx) {
    return """
        ## 题目上下文
        - 鉴定范围：%s
        - 题型：%s
        - 难度：%s

        ## 题目信息
        - 题型：%s
        - 题干：%s
        - 正确选项：%s
        - 干扰项：%s
        - 题目考查的关键概念：%s

        ## 生成要求
        一次性生成 3 条「相似问」和 3 条「反例」。

        ### 相似问要求
        1. 与原题答案一致，语义等价
        2. 使用国家职业标准中的规范术语，避免口语化
        3. 保留题干核心概念（名词、限定词）不变
        4. 通过替换疑问词、同义改写、调整句式生成多样性问法
        5. 不生成需要额外推理或引入新知识才能回答的问法

        ### 反例要求
        1. 与原题「看起来相似但答案不同」
        2. 每条使用不同策略：属性替换 / 实体替换 / 等级混淆 / 工段场景替换 / 答非所问
        3. 每条反例后用括号标注「应匹配的正确答案」，格式：（答案应为 XXX）
        4. 体现国家职业标准中容易混淆的邻近概念

        ## 输出格式
        {
          "similar_questions": ["问法1", "问法2", "问法3"],
          "negative_questions": ["反例1（答案应为XXX）", "反例2（答案应为XXX）", "反例3（答案应为XXX）"]
        }
        """.formatted(
        ctx.getOrDefault("knowledgePath", "未分类"),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("diffLabel", ""),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("contentStripped", ""),
        ctx.getOrDefault("correctOptions", ""),
        ctx.getOrDefault("wrongOptions", ""),
        ctx.getOrDefault("keyConcepts", "无")
    );
  }

  private String buildSimilarUserPrompt(Map<String, String> ctx) {
    return """
        #### 题目上下文
        - 鉴定范围：%s
        - 题型：%s
        - 难度：%s

        #### 题目信息
        - 题型：%s
        - 题干：%s
        - 正确选项：%s
        - 干扰项：%s

        #### 生成要求
        1. 相似问必须与原题答案一致，语义等价
        2. 使用国家职业标准中的规范术语，避免口语化表述
        3. 保留题干中的核心概念（名词、限定词）不变
        4. 生成的问法应覆盖学员在实际作答或质检中可能出现的真实提问方式
        5. 通过替换疑问词、同义改写、调整句式来生成多样性问法
        6. 不要生成需要额外推理或引入新知识才能回答的问法
        7. 输出为 JSON 数组，每条为纯文本字符串

        #### 输出格式
        {"similar_questions": ["问法1", "问法2", "问法3"]}
        """.formatted(
        ctx.getOrDefault("knowledgePath", "未分类"),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("diffLabel", ""),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("contentStripped", ""),
        ctx.getOrDefault("correctOptions", ""),
        ctx.getOrDefault("wrongOptions", "")
    );
  }

  private String buildNegativeUserPrompt(Map<String, String> ctx) {
    return """
        #### 题目上下文
        - 鉴定范围：%s
        - 题型：%s
        - 难度：%s

        #### 题目信息
        - 题型：%s
        - 题干：%s
        - 正确选项：%s
        - 干扰项：%s
        - 题目考查的关键概念：%s

        #### 生成要求
        1. 反例必须与原题「看起来相似但答案不同」，用于培训模型区分能力
        2. 使用以下策略生成反例（每条使用不同策略）：
           - 属性替换：保留主题词，把属性词替换为同职业标准中该主题的其他属性
           - 实体替换：保留句式结构，把核心实体换成同工种的其他标准实体
           - 等级替换：将职业等级要求混淆（如把"初级工要求"换成"高级工要求"）
           - 工段/场景替换：把题干中的工段/场景换成工种内相近但不同的工段
           - 答非所问：保留相关词，但答案需要完全不同的标准知识点
        3. 每条反例后用括号标注「应匹配的正确答案」
        4. 输出为 JSON 数组，每条为纯文本字符串

        #### 输出格式
        {"negative_questions": ["反例1（答案应为XXX）", "反例2（答案应为XXX）", "反例3（答案应为XXX）"]}
        """.formatted(
        ctx.getOrDefault("knowledgePath", "未分类"),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("diffLabel", ""),
        ctx.getOrDefault("typeLabel", ""),
        ctx.getOrDefault("contentStripped", ""),
        ctx.getOrDefault("correctOptions", ""),
        ctx.getOrDefault("wrongOptions", ""),
        ctx.getOrDefault("keyConcepts", "无")
    );
  }
}
