package com.guoyu.platform.certification.defense;

import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformanceItemScore;
import com.guoyu.platform.certification.defense.DefenseScoringModels.PerformancePayload;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ScoreComputation;
import com.guoyu.platform.certification.defense.DefenseScoringModels.ThesisPayload;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DefenseScoreCatalog {

  public static final String SHEET_THESIS = "THESIS";
  public static final String SHEET_PERFORMANCE = "PERFORMANCE";
  public static final String STATUS_DRAFT = "draft";
  public static final String STATUS_SUBMITTED = "submitted";
  public static final String STATUS_EMPTY = "empty";

  public static final BigDecimal THESIS_CONTENT_WEIGHT = new BigDecimal("0.40");
  public static final BigDecimal THESIS_DEFENSE_WEIGHT = new BigDecimal("0.60");
  public static final BigDecimal COMPREHENSIVE_THESIS_WEIGHT = new BigDecimal("0.60");
  public static final BigDecimal COMPREHENSIVE_PERFORMANCE_WEIGHT = new BigDecimal("0.40");
  public static final int EXPORT_EXPERT_COUNT = 3;
  public static final String STATUS_CONFIGURED = "configured";
  public static final String STATUS_COMPLETED = "completed";

  public static final List<String> THESIS_REJECT_ITEMS = List.of(
      "有严重抄袭行为或内容违背社会主义价值观。",
      "内容非本职业领域内容或不能体现本职业本级别技能特点。",
      "观点和方法严重错误。",
      "未按要求的题目或方向进行撰写"
  );

  public static final List<ScoreItem> THESIS_CONTENT_ITEMS = List.of(
      new ScoreItem("content_1", "论文内容的意义和难度", 15),
      new ScoreItem("content_2", "论文内容的正确性", 15),
      new ScoreItem("content_3", "论文结构的逻辑性", 15),
      new ScoreItem("content_4", "论文的独创性及应用价值", 15),
      new ScoreItem("content_5", "掌握基础理论知识的程度", 15),
      new ScoreItem("content_6", "综合分析和解决问题的能力", 15),
      new ScoreItem("content_7", "文字质量和书面表达能力", 10)
  );

  public static final List<ScoreItem> THESIS_DEFENSE_ITEMS = List.of(
      new ScoreItem("defense_1", "考生简述论文情况", 25),
      new ScoreItem("defense_2", "回答问题正确性", 25),
      new ScoreItem("defense_3", "对论文选题的理解程度", 25),
      new ScoreItem("defense_4", "逻辑思维及口头表达能力", 25)
  );

  public static final List<PerformanceCategory> PERFORMANCE_CATEGORIES = List.of(
      new PerformanceCategory("ability", "业务能力", 30, List.of(
          new PerformanceItem(
              "duty_ability",
              "履行岗位职责能力",
              10,
              "1.承担重要技术工作，工作责任重大得9-10分；2.承担比较复杂的技术工作，解决较复杂技术问题得7-8分；3.承担一般性的技术工作，解决一般情况技术问题得5-6分。"
          ),
          new PerformanceItem(
              "work_performance",
              "工作业绩",
              20,
              "1.解决过国家级重大技术难题，或参与国家级科技项目酌情得16-20分。2.解决过省部级关键技术难题，或参与省部级科技项目酌情得14-18分。3.解决过地市级复杂技术难题，或参与地市级科技项目酌情得12-16分。4.解决过公司级一般技术难题，或参与公司级科技项目10-14分。"
          )
      )),
      new PerformanceCategory("guidance", "指导能力", 30, List.of(
          new PerformanceItem(
              "papers_textbooks",
              "论文发表或编写教材",
              20,
              "在专业期刊发表论文的得10分，每增加一篇得5分；核心期刊以上的得15分，每增加一篇得5分。发表实用新型专利的得5分，每增加一篇得5分；发表发明专利的得10分，每增加一篇得10分。参与开发本职业技能国家职业标准、自治区题库编写、国家部委或自治区组织编写培训教材的酌情得1-5分。"
          ),
          new PerformanceItem(
              "learning",
              "学习能力",
              5,
              "1.3年内参加2次单位以外组织的业务培训得5分；2.3年内每次参加过单位以外的业务培训得4分（每次3分，最高5分）；3.3年内每次参加过单位组织的业务培训得3分（每次2分，最高5分）。"
          ),
          new PerformanceItem(
              "training_guidance",
              "培训指导",
              5,
              "在各种学习活动中承担授课、指导的酌情给分；或根据培养、传授徒弟的人数，徒弟技艺水平、比赛情况酌情给分。"
          )
      )),
      new PerformanceCategory("honor", "荣誉与奖励", 20, List.of(
          new PerformanceItem(
              "awards",
              "奖励",
              10,
              "创造发明、技术革新、技术改造项目获得省、部级奖励得10分；市、厅级奖励得9分；获得县级奖励得8分。或参加省级一类竞赛前10名、二类竞赛前5名得10分；市级一类竞赛前10名、二类竞赛前5名得9分；县级一类竞赛前10名、二类或企业内部竞赛前5名得8分。获得荣誉证书省部级得10分，市厅级9分，县级8分，本单位获得荣誉按单位规格得分。"
          ),
          new PerformanceItem(
              "annual_review",
              "年度考核",
              10,
              "近3年的年度考核全部称职（合格）得6分，一次优秀加1分。"
          )
      )),
      new PerformanceCategory("qualification", "个人资历", 20, List.of(
          new PerformanceItem(
              "education",
              "学历",
              5,
              "本科及以上学历5分，大专学历4分，中职及以下学历3分。"
          ),
          new PerformanceItem(
              "tenure",
              "任职年限",
              15,
              "任职资历满足申报条件得9分，每超过一年加1分。本项为否定项，经核实不符合申报条件的，综合科目成绩判定不合格，并按虚假申报取消所有科目成绩。"
          )
      ))
  );

  private static final Map<String, Integer> PERFORMANCE_MAX = new LinkedHashMap<>();

  static {
    for (PerformanceCategory category : PERFORMANCE_CATEGORIES) {
      for (PerformanceItem item : category.items()) {
        PERFORMANCE_MAX.put(item.key(), item.maxScore());
      }
    }
  }

  private DefenseScoreCatalog() {
  }

  public static void requireSheetType(String sheetType) {
    if (!SHEET_THESIS.equals(sheetType) && !SHEET_PERFORMANCE.equals(sheetType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的评分表类型");
    }
  }

  public static ThesisPayload emptyThesis() {
    return new ThesisPayload(
        "",
        zeros(THESIS_REJECT_ITEMS.size()).stream().map(value -> Boolean.FALSE).toList(),
        "",
        nullableList(THESIS_CONTENT_ITEMS.size()),
        List.of("", "", ""),
        nullableList(THESIS_DEFENSE_ITEMS.size())
    );
  }

  public static PerformancePayload emptyPerformance() {
    List<PerformanceItemScore> items = new ArrayList<>();
    for (String key : PERFORMANCE_MAX.keySet()) {
      items.add(new PerformanceItemScore(key, null));
    }
    return new PerformancePayload(items, false, "");
  }

  public static ScoreComputation computeThesis(ThesisPayload payload, boolean submit) {
    ThesisPayload normalized = normalizeThesis(payload);
    if (submit) {
      requireFilled(normalized.contentScores(), THESIS_CONTENT_ITEMS, "论文内容部分");
      requireFilled(normalized.defenseScores(), THESIS_DEFENSE_ITEMS, "论文答辩部分");
      if (hasReject(normalized) && !StringUtils.hasText(normalized.rejectNote())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "勾选否定项后必须填写否定说明");
      }
    }
    int content = sumScores(normalized.contentScores(), THESIS_CONTENT_ITEMS, "论文内容部分");
    int defense = sumScores(normalized.defenseScores(), THESIS_DEFENSE_ITEMS, "论文答辩部分");
    BigDecimal finalScore = BigDecimal.valueOf(content).multiply(THESIS_CONTENT_WEIGHT)
        .add(BigDecimal.valueOf(defense).multiply(THESIS_DEFENSE_WEIGHT))
        .setScale(2, RoundingMode.HALF_UP);
    boolean rejected = hasReject(normalized);
    return new ScoreComputation(
        BigDecimal.valueOf(content).setScale(2, RoundingMode.HALF_UP),
        BigDecimal.valueOf(defense).setScale(2, RoundingMode.HALF_UP),
        null,
        finalScore,
        rejected
    );
  }

  public static ScoreComputation computePerformance(PerformancePayload payload, boolean submit) {
    PerformancePayload normalized = normalizePerformance(payload);
    Map<String, Integer> scores = toScoreMap(normalized);
    if (submit) {
      for (Map.Entry<String, Integer> entry : PERFORMANCE_MAX.entrySet()) {
        if (scores.get(entry.getKey()) == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "业绩评估报告评分表尚有未打分项目");
        }
      }
    }
    int total = 0;
    for (Map.Entry<String, Integer> max : PERFORMANCE_MAX.entrySet()) {
      Integer score = scores.get(max.getKey());
      if (score == null) {
        continue;
      }
      if (score < 0 || score > max.getValue()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实际得分不能超过该项满分");
      }
      total += score;
    }
    boolean rejected = Boolean.TRUE.equals(normalized.disqualify());
    return new ScoreComputation(
        null,
        null,
        BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP),
        BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP),
        rejected
    );
  }

  public static ThesisPayload normalizeThesis(ThesisPayload payload) {
    ThesisPayload source = payload == null ? emptyThesis() : payload;
    return new ThesisPayload(
        trimToEmpty(source.paperTitle()),
        padBooleans(source.rejectFlags(), THESIS_REJECT_ITEMS.size()),
        trimToEmpty(source.rejectNote()),
        padIntegers(source.contentScores(), THESIS_CONTENT_ITEMS.size()),
        padStrings(source.questions(), 3),
        padIntegers(source.defenseScores(), THESIS_DEFENSE_ITEMS.size())
    );
  }

  public static BigDecimal average(List<BigDecimal> scores) {
    if (scores == null || scores.isEmpty()) {
      return null;
    }
    BigDecimal sum = BigDecimal.ZERO;
    int count = 0;
    for (BigDecimal score : scores) {
      if (score == null) {
        continue;
      }
      sum = sum.add(score);
      count += 1;
    }
    if (count == 0) {
      return null;
    }
    return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
  }

  public static BigDecimal comprehensive(BigDecimal thesisAverage, BigDecimal performanceAverage) {
    if (thesisAverage == null || performanceAverage == null) {
      return null;
    }
    return thesisAverage.multiply(COMPREHENSIVE_THESIS_WEIGHT)
        .add(performanceAverage.multiply(COMPREHENSIVE_PERFORMANCE_WEIGHT))
        .setScale(2, RoundingMode.HALF_UP);
  }

  public static PerformancePayload normalizePerformance(PerformancePayload payload) {
    PerformancePayload source = payload == null ? emptyPerformance() : payload;
    Map<String, Integer> scores = toScoreMap(source);
    List<PerformanceItemScore> items = new ArrayList<>();
    for (String key : PERFORMANCE_MAX.keySet()) {
      items.add(new PerformanceItemScore(key, scores.get(key)));
    }
    return new PerformancePayload(items, Boolean.TRUE.equals(source.disqualify()), trimToEmpty(source.remark()));
  }

  private static Map<String, Integer> toScoreMap(PerformancePayload payload) {
    Map<String, Integer> scores = new LinkedHashMap<>();
    if (payload == null || payload.items() == null) {
      return scores;
    }
    for (PerformanceItemScore item : payload.items()) {
      if (item == null || !StringUtils.hasText(item.key())) {
        continue;
      }
      if (!PERFORMANCE_MAX.containsKey(item.key())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "存在未知的业绩评估评分项");
      }
      scores.put(item.key(), item.score());
    }
    return scores;
  }

  private static void requireFilled(List<Integer> scores, List<ScoreItem> items, String section) {
    if (scores == null || scores.size() != items.size() || scores.stream().anyMatch(score -> score == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, section + "尚有未打分项目");
    }
  }

  private static int sumScores(List<Integer> scores, List<ScoreItem> items, String section) {
    int total = 0;
    for (int i = 0; i < items.size(); i++) {
      Integer score = scores.get(i);
      if (score == null) {
        continue;
      }
      int max = items.get(i).maxScore();
      if (score < 0 || score > max) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, section + "实际成绩不能超过满分");
      }
      total += score;
    }
    return total;
  }

  private static boolean hasReject(ThesisPayload payload) {
    return payload.rejectFlags() != null && payload.rejectFlags().stream().anyMatch(Boolean.TRUE::equals);
  }

  private static List<Integer> nullableList(int size) {
    List<Integer> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(null);
    }
    return values;
  }

  private static List<Integer> zeros(int size) {
    List<Integer> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(0);
    }
    return values;
  }

  private static List<Boolean> padBooleans(List<Boolean> source, int size) {
    List<Boolean> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(source != null && i < source.size() && Boolean.TRUE.equals(source.get(i)));
    }
    return values;
  }

  private static List<Integer> padIntegers(List<Integer> source, int size) {
    List<Integer> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(source != null && i < source.size() ? source.get(i) : null);
    }
    return values;
  }

  private static List<String> padStrings(List<String> source, int size) {
    List<String> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(source != null && i < source.size() ? trimToEmpty(source.get(i)) : "");
    }
    return values;
  }

  private static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  public record ScoreItem(String key, String label, int maxScore) {
  }

  public record PerformanceItem(String key, String label, int maxScore, String criteria) {
  }

  public record PerformanceCategory(String key, String label, int maxScore, List<PerformanceItem> items) {
  }
}
