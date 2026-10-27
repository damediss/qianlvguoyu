package com.guoyu.platform.exam.service;

import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;

/**
 * 解析生成 prompt v2（《试题解析Markdown表格化_一期实施方案》§5.6）。
 *
 * <p>受控语言四原则 + 四行骨架 + 题型分支 + 条件表格；表格是唯一允许的 markdown 语法。
 * 字数预算/分支行等 prompt 侧专属规则集中在本类，不塞进通用 {@link QuestionTypes}。
 */
public final class QuestionResolvePromptPolicy {

  private QuestionResolvePromptPolicy() {
  }

  /**
   * System prompt 固定全文（§5.6）；{预算} 按题型替换。
   */
  public static String systemPrompt(int budget) {
    return """
        你是职业技能等级认定考试的辅导老师，考生是一线在职工人。写解析必须遵守：
        1. 短句：一句只说一件事，单句不超过 25 个字。
        2. 主动语态：写“断电后才能更换熔断器”，不写“熔断器的更换应当在断电后进行”。
        3. 同一事物用同一个词：题干里叫“电解槽”，解析里就叫“电解槽”，禁止换成同义词。
        4. 只依据题干、选项、正确答案和所给知识点名称作答。可以引用岗位操作常识，
           但禁止编造标准编号、文件名称、具体数据，禁止输出学员作答或“你选了 X”。
        5. 正文不超过 %d 字。除第 8 条允许的表格外，不得使用任何 markdown、emoji、
           HTML、链接或代码块。
        6. 不出现“保过/必过/稳过”，不出现模型名、参数名、表名、内部文档名。
        7. 严格按下列结构输出，每行以【】小节名开头，不输出小节名以外的内容：
        【考点】本题考查的知识点，10 字以内。
        【正确】必须解释正确答案与题干条件的对应关系，禁止只写“符合题意”
        “题目给出的答案就是 X”这类空话。
        【干扰】只能依据题干、选项和正确答案说明该选项哪里不符，禁止引入题干未提供的
        等级、标准、分类、历史、背景或其他参数；无法确定时，只写“与题干给出的正确答案
        不一致”，不得推断它属于其他等级、其他标准或其他参数。
        【记法】一条现场记忆口诀或动作要点，不超过 30 字。
        8. 表格规则（唯一允许的 markdown 语法）：仅当题干包含 2 个及以上可对比的
           数值、配比或档位，且表格确实更易读时，才在【正确】内容之后插入一张表格；
           其余情况禁止表格。表格必须为管道语法：表头行、分隔行（|---|---|）、
           数据行各占一行；列数不超过 3，数据行不超过 4，每格不超过 12 字；
           表格数值必须原样来自题干、选项或正确答案，禁止推算新数据。表格不占字数预算。
        """.formatted(budget);
  }

  /** 题型分支行与【正确】硬结构（§5.6 表；主观题省略【干扰】）。 */
  public static String branchLine(QuestionPromptInput input) {
    String type = input.normalizedType();
    if ("single".equals(type)) {
      // 直接以实际答案开头，避免"（X 替换为真实答案）"在答案非 A 时误导模型
      return "【正确】必须以“答案：" + input.correctAnswerText() + "。”开头，"
          + "解释该答案与题干条件的对应关系，禁止只写“符合题意”。";
    }
    if ("multi".equals(type)) {
      return "【正确】必须以“答案：" + input.correctAnswerText() + "。”开头，"
          + "逐项说明入选原因；【干扰】点名最常漏选/多选项，只依据题干与选项，"
          + "不得引入题干未提供的信息。";
    }
    if ("judge".equals(type)) {
      return "【正确】必须以“答案：对。”或“答案：错。”开头；【干扰】说明错误说法错在哪、正确做法是什么，"
          + "不得引入题干未提供的等级或标准。";
    }
    if (QuestionTypes.isSubjective(type)) {
      return "【正确】以“答案要点：”开头，按给分要点逐条说明，禁止只写结论或复述题干；"
          + "省略【干扰】行，只输出【考点】【正确】【记法】。";
    }
    return "【正确】以“答案：" + input.correctAnswerText() + "。”开头，"
        + "解释该答案与题干条件的对应关系，按通用四行输出。";
  }

  /** 正文预算（§5.6 表）：单选/判断/未知 240，多选 300，主观 400。 */
  public static int budgetFor(String normalizedType) {
    if ("multi".equals(normalizedType)) {
      return 300;
    }
    if (QuestionTypes.isSubjective(normalizedType)) {
      return 400;
    }
    return 240;
  }

  /**
   * maxTokens（§5.6 + 模型隔离批）：模型配置 max_tokens 非空且 >0 时优先采用，
   * clamp 到 [800, 8192]；未配置时按 thinking 推导——disabled=800，enabled/未知=2400
   * （思维链计入输出预算）。
   */
  public static int maxTokensFor(String thinking, Integer configuredMaxTokens) {
    if (configuredMaxTokens != null && configuredMaxTokens > 0) {
      return Math.max(800, Math.min(8192, configuredMaxTokens));
    }
    return maxTokensFor(thinking);
  }

  /**
   * maxTokens（§5.6）：关闭思考 800；配置开启思考 2400（思维链计入输出预算）；
   * 无法判断（null/blank/其它值）按开启处理取 2400。
   */
  public static int maxTokensFor(String thinking) {
    if ("disabled".equalsIgnoreCase(String.valueOf(thinking).trim())) {
      return 800;
    }
    return 2400;
  }

  /** User prompt（§5.6）：题型/知识点/题干/选项/正确答案/题型分支行 + 固定收尾。 */
  public static String userPrompt(QuestionPromptInput input) {
    StringBuilder sb = new StringBuilder();
    sb.append("题型：").append(input.typeLabel()).append('\n');
    sb.append("知识点：")
        .append(input.knowledgeTitle() == null || input.knowledgeTitle().isBlank()
            ? "未标注" : input.knowledgeTitle()).append('\n');
    sb.append("题干：\n").append(input.stem()).append('\n');
    if (input.optionsText() != null && !input.optionsText().isBlank()) {
      sb.append("选项：\n").append(input.optionsText()).append('\n');
    }
    sb.append("正确答案：").append(input.correctAnswerText()).append('\n');
    sb.append(branchLine(input)).append('\n');
    sb.append("请按四行结构写出解析：");
    return sb.toString();
  }
}
