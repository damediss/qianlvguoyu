package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 操作技能试题 Validator 规则单测（方案 §7.3 V1~V8 逐规则 + 反例） */
class SkillQuestionValidatorTest {

  private final SkillQuestionValidator validator = new SkillQuestionValidator();

  /** 正例：合法一题 */
  static SkillQuestionItem validQuestion(String name) {
    SkillQuestionItem q = new SkillQuestionItem();
    q.setName(name);
    q.setDurationMinutes(20);
    q.setExamForm("实操");
    SkillQuestionItem.SitePrepare site = new SkillQuestionItem.SitePrepare();
    site.setNote("考核前确认断电");
    SkillQuestionItem.Equipment e = new SkillQuestionItem.Equipment();
    e.setName("万用表");
    e.setSpec("数字式");
    e.setUnit("台");
    e.setQty("1");
    e.setRemark("");
    site.setEquipment(new ArrayList<>(List.of(e)));
    site.setFaultSetup("设置一处通信异常");
    q.setSitePrepare(site);
    q.setExamineePrepare("本人身份证×1、准考证×1、黑色签字笔×1");
    q.setRequirements("完成联机启动并确认通信正常");
    q.setNegatives("若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违反安全规定");
    // V10 口径：配分=最大扣分=5 的倍数、步长=5，评分标准按步长列「…扣5分」项（项数=配分÷5）
    q.setScoring(new ArrayList<>(List.of(
        scoring("开机检查", "20",
            "未确认断电扣5分；未检查接地扣5分；未佩戴防护用品扣5分；未确认警示标识扣5分", "20", "5"),
        scoring("联机操作", "50",
            "漏一步扣5分；顺序错误扣5分；参数错误扣5分；未观察状态扣5分；链路未建立扣5分；"
                + "未自检扣5分；未按规程停机扣5分；记录缺失扣5分；超时扣5分；违规操作扣5分", "50", "5"),
        scoring("通信确认", "30",
            "未逐项确认扣5分；报警未验证扣5分；数据缺失扣5分；异常未上报扣5分；交接未确认扣5分；"
                + "记录不全扣5分", "30", "5"))));
    return q;
  }

  static SkillQuestionItem.ScoringItem scoring(String item, String score, String desc,
      String maxDeduct, String step) {
    SkillQuestionItem.ScoringItem s = new SkillQuestionItem.ScoringItem();
    s.setItem(item);
    s.setScore(score);
    s.setScoreDesc(desc);
    s.setMaxDeduct(maxDeduct);
    s.setStep(step);
    return s;
  }

  @Test
  void v1_题数必须精确等于5() {
    List<SkillQuestionItem> list = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      list.add(validQuestion("试题" + i));
    }
    List<String> errors = validator.validateAll(list, 5);
    assertTrue(errors.stream().anyMatch(e -> e.contains("题数必须为 5")));
  }

  @Test
  void v2_五要素缺失各一反例() {
    SkillQuestionItem base = validQuestion("试题A");
    base.setExamineePrepare(" ");
    assertTrue(validator.validate(base).stream().anyMatch(e -> e.contains("考生准备为空")));

    SkillQuestionItem noEquipment = validQuestion("试题B");
    noEquipment.getSitePrepare().setEquipment(new ArrayList<>());
    assertTrue(validator.validate(noEquipment).stream()
        .anyMatch(e -> e.contains("考场准备缺少设备设施清单")));

    SkillQuestionItem noReq = validQuestion("试题C");
    noReq.setRequirements(null);
    assertTrue(validator.validate(noReq).stream().anyMatch(e -> e.contains("具体考核要求为空")));

    SkillQuestionItem noNeg = validQuestion("试题D");
    noNeg.setNegatives("安全违规记零分");
    assertTrue(validator.validate(noNeg).stream()
        .anyMatch(e -> e.contains("否定项说明未按标准表述")));
  }

  /** 考生准备固定保底（用户 2026-09-11 确认）：身份证/准考证/黑色签字笔缺一不可 */
  @Test
  void v2b_考生准备缺少固定保底项() {
    SkillQuestionItem noId = validQuestion("试题L");
    noId.setExamineePrepare("准考证×1、黑色签字笔×1");
    assertTrue(validator.validate(noId).stream()
        .anyMatch(e -> e.contains("考生准备缺少固定保底项") && e.contains("身份证")));

    SkillQuestionItem noPen = validQuestion("试题M");
    noPen.setExamineePrepare("本人身份证×1、准考证×1");
    assertTrue(validator.validate(noPen).stream()
        .anyMatch(e -> e.contains("黑色签字笔")));

    SkillQuestionItem allSet = validQuestion("试题N");
    allSet.setExamineePrepare("本人身份证×1、准考证×1、黑色签字笔×1、安全帽×1顶");
    assertTrue(validator.validate(allSet).stream().noneMatch(e -> e.contains("考生准备")));
  }

  @Test
  void v4_评分行数越界() {
    SkillQuestionItem two = validQuestion("试题E");
    two.setScoring(new ArrayList<>(List.of(
        scoring("a", "50", "缺一项扣5分", "50", "5"),
        scoring("b", "50", "错一处扣5分", "50", "5"))));
    assertTrue(validator.validate(two).stream().anyMatch(e -> e.contains("3~10 行")));
  }

  @Test
  void v5_配分合计必须等于100() {
    SkillQuestionItem bad = validQuestion("试题F");
    bad.getScoring().get(0).setScore("10");
    bad.getScoring().get(0).setMaxDeduct("10");
    assertTrue(validator.validate(bad).stream().anyMatch(e -> e.contains("配分合计")));
    assertEquals(List.of(), validator.validate(validQuestion("试题G")), "正例应无错误");
  }

  @Test
  void v6_扣分步长与配分关系() {
    SkillQuestionItem over = validQuestion("试题H");
    over.getScoring().get(1).setScore("30");
    over.getScoring().get(2).setScore("50");
    assertTrue(validator.validate(over).stream()
        .anyMatch(e -> e.contains("最大扣分") && e.contains("超过配分")));

    SkillQuestionItem stepOver = validQuestion("试题I");
    stepOver.getScoring().get(0).setStep("30");
    assertTrue(validator.validate(stepOver).stream()
        .anyMatch(e -> e.contains("步长") && e.contains("超过最大扣分")));
  }

  @Test
  void v7_考核时间范围() {
    SkillQuestionItem fast = validQuestion("试题J");
    fast.setDurationMinutes(3);
    assertTrue(validator.validate(fast).stream().anyMatch(e -> e.contains("5~240")));
    SkillQuestionItem slow = validQuestion("试题K");
    slow.setDurationMinutes(300);
    assertTrue(validator.validate(slow).stream().anyMatch(e -> e.contains("5~240")));
  }

  /** V10（用户 2026-09-12 确认）：配分=最大扣分=5 的倍数、步长=5、评分标准按步长列「…扣5分」项 */
  @Test
  void v10_步长统一5分且按步长列扣分项() {
    SkillQuestionItem badStep = validQuestion("试题O");
    badStep.getScoring().get(0).setStep("3");
    assertTrue(validator.validate(badStep).stream().anyMatch(e -> e.contains("步长必须为 5")));

    SkillQuestionItem badMultiple = validQuestion("试题P");
    badMultiple.getScoring().get(0).setScore("12");
    badMultiple.getScoring().get(0).setMaxDeduct("12");
    assertTrue(validator.validate(badMultiple).stream()
        .anyMatch(e -> e.contains("配分必须为 5 的倍数")));

    SkillQuestionItem notEqual = validQuestion("试题Q");
    notEqual.getScoring().get(0).setMaxDeduct("15");
    assertTrue(validator.validate(notEqual).stream()
        .anyMatch(e -> e.contains("配分与最大扣分必须相等")));

    // 扣分项数 ≠ 最大扣分÷5（desc 只有 2 项「扣5分」，应为 4 项）
    SkillQuestionItem badCount = validQuestion("试题S");
    badCount.getScoring().get(0).setScoreDesc("未确认断电扣5分；未检查接地扣5分");
    assertTrue(validator.validate(badCount).stream()
        .anyMatch(e -> e.contains("评分标准须按步长列出 4 项")));

    // 「扣 5 分」带空格的合法写法可通过项数校验
    SkillQuestionItem spaced = validQuestion("试题T");
    spaced.getScoring().get(0).setScoreDesc(
        "未确认断电扣 5 分；未检查接地扣5分；未佩戴防护用品扣5分；未确认警示标识扣5分");
    assertTrue(validator.validate(spaced).stream()
        .noneMatch(e -> e.contains("评分标准须按步长列出")));

    assertTrue(validator.validate(validQuestion("试题U")).stream()
        .noneMatch(e -> e.contains("步长必须为") || e.contains("5 的倍数")
            || e.contains("必须相等") || e.contains("评分标准须按步长列出")),
        "正例应通过全部 V10 校验");
  }

  @Test
  void v8_同点内名称重复与组合重复() {
    List<SkillQuestionItem> list = new ArrayList<>();
    list.add(validQuestion("同名试题"));
    for (int i = 1; i < 5; i++) {
      list.add(validQuestion("试题" + i));
    }
    SkillQuestionItem dup = validQuestion("同名试题");
    dup.setRequirements("另一套要求");
    List<SkillQuestionItem> six = new ArrayList<>(list);
    six.add(dup);
    List<String> errors = validator.validateAll(six, 6);
    assertTrue(errors.stream().anyMatch(e -> e.contains("同点内试题名称重复")));
  }

  @Test
  void v9_合法整块零错误() {
    List<SkillQuestionItem> list = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      list.add(validQuestion("联机启动集控室监控设备（" + (i + 1) + "）"));
    }
    assertEquals(List.of(), validator.validateAll(list, 5));
  }
}
