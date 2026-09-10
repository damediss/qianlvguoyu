package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 导出格式回归（用户 2026-09-10 确认的三条修正）：
 * ① 文件名 = 工种(等级)_操作技能试题_批次{短号}.xlsx（等级形如"二级-技师"）；
 * ② 导出"考场准备"仅保留 考核有关说明/设备设施准备/故障·工况设置 三项；
 * ③ 导出"考生准备"仅输出正文（无四项头部、无"其他准备"标签）；
 * 并验证**入库渲染（kaochang/kaosheng）保持指引样例格式不变**（仅改导出）。
 */
class SkillGenExportFormatTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private SkillGenDraftEntity draft() {
    SkillGenDraftEntity d = new SkillGenDraftEntity();
    d.setId(1L);
    d.setQuestionName("冷态条件下集控室监控系统联机启动与基础参数配置");
    d.setScore(new BigDecimal(100));
    d.setWeight(new BigDecimal("10.00"));
    d.setDurationMinutes(40);
    d.setExamForm("实操");
    d.setSitePreparation("{\"note\":\"考核前由考评员确认监控主机、显示器、传输接口均已断电\","
        + "\"equipment\":[{\"name\":\"监控主机\",\"spec\":\"工控机\",\"unit\":\"套\",\"qty\":\"1\",\"remark\":\"\"}],"
        + "\"faultSetup\":\"无故障设置，按冷态状态组织考核\"}");
    d.setExamineePrepare("黑色签字笔、绝缘手套、计算器；着装符合安全要求。");
    d.setRequirements("请按操作规程完成集控室监控设备联机启动。");
    d.setNegatives("若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违规");
    d.setScoringItems("[{\"item\":\"a\",\"score\":\"50\",\"scoreDesc\":\"x\",\"maxDeduct\":\"50\",\"step\":\"10\"}]");
    d.setDifficulty(3);
    d.setReviewStatus(SkillGenDraftEntity.REVIEW_APPROVED);
    return d;
  }

  private SkillGenDraftService service() {
    return new SkillGenDraftService(null, null, null, null, null, null, null, objectMapper);
  }

  /** 修正 ①：文件名规则 */
  @Test
  void filename_workTypeLevelBatchShortId() {
    assertEquals("矿山生产集控员(二级-技师)_操作技能试题_批次78417666.xlsx",
        SkillGenExportService.buildFilename("矿山生产集控员", 2, 2097882600178417666L));
    assertEquals("矿山救护工(五级-初级工)_操作技能试题_批次12345678.xlsx",
        SkillGenExportService.buildFilename("矿山救护工", 5, 12345678L));
    assertEquals("某工种(一级-高级技师)_操作技能试题_批次9.xlsx",
        SkillGenExportService.buildFilename("某工种", 1, 9L));
    assertEquals("二级-技师", SkillGenExportService.levelText(2));
    assertEquals("五级-初级工", SkillGenExportService.levelText(5));
  }

  /** 修正 ②：导出"考场准备"只含三项 */
  @Test
  void exportKaochang_threeSectionsOnly() {
    String text = service().renderKaochangForExport(draft());
    assertTrue(text.contains("【考核有关说明】"));
    assertTrue(text.contains("【设备设施准备】"));
    assertTrue(text.contains("序号 | 名称 | 规格 | 单位 | 数量 | 备注"));
    assertTrue(text.contains("| 监控主机 | 工控机 | 套 | 1 |"));
    assertTrue(text.contains("【故障/工况设置】"));
    assertFalse(text.contains("【试题名称】"), "导出不应含四项头部");
    assertFalse(text.contains("【本题分值】"));
    assertFalse(text.contains("【考核时间】"));
    assertFalse(text.contains("【考核形式】"));
  }

  /** 修正 ③：导出"考生准备"仅正文 */
  @Test
  void exportKaosheng_plainTextOnly() {
    String text = service().renderKaoshengForExport(draft());
    assertEquals("黑色签字笔、绝缘手套、计算器；着装符合安全要求。", text);
    assertFalse(text.contains("其他准备"));
    assertFalse(text.contains("【"));
  }

  /** 入库渲染保持指引样例格式（仅改导出，用户确认项） */
  @Test
  void publishRenderTemplateUnchanged() {
    SkillGenDraftService s = service();
    SkillGenDraftEntity d = draft();
    assertTrue(s.renderKaochang(d).contains("【试题名称】"), "入库 kaochang 仍是四项头部格式");
    assertTrue(s.renderKaochang(d).contains("【考核时间】"));
    assertTrue(s.renderKaosheng(d).contains("【其他准备】"), "入库 kaosheng 仍带【其他准备】标签");
    assertTrue(s.renderKaosheng(d).contains("【考核时间】40分钟"));
    assertTrue(s.renderContent(d).contains("【否定项】"));
  }
}
