package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 准备要求版式回归（用户 2026-09-11 确认，对齐 2级实操题库/矿山监控岗位实操考评评分表两份样本）：
 * ① 考场准备 = 设备设施清单一句文字（名称×数量+单位、顿号分隔），不带表格/说明/故障设置；
 * ② 考生准备 = 纯清单文本，不带四项头部与"（工、量具及）其他准备"标签；
 * ③ 入库与导出同版式；考核试题（content）仍保留指引样例的六项结构。
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
        + "\"equipment\":[{\"name\":\"监控主机\",\"spec\":\"工控机\",\"unit\":\"套\",\"qty\":\"1\",\"remark\":\"\"},"
        + "{\"name\":\"数字万用表\",\"spec\":\"\",\"unit\":\"台\",\"qty\":\"1\",\"remark\":\"\"}],"
        + "\"faultSetup\":\"无故障设置，按冷态状态组织考核\"}");
    d.setExamineePrepare("本人身份证×1、准考证×1、黑色签字笔×1、绝缘手套×1副");
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

  /** 文件名规则（用户 2026-09-10 确认） */
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

  /** 考场准备 = 清单一句文字（名称×数量+单位、顿号分隔），无表格/说明/故障设置/四项头部 */
  @Test
  void kaochang_equipmentListTextOnly() {
    SkillGenDraftService s = service();
    assertEquals("监控主机×1套、数字万用表×1台", s.renderKaochang(draft()));
    assertEquals(s.renderKaochang(draft()), s.renderKaochangForExport(draft()),
        "入库与导出同版式");
    String text = s.renderKaochang(draft());
    assertFalse(text.contains("【"));
    assertFalse(text.contains("|"), "不得残留管道符表格");
    assertFalse(text.contains("考核有关说明"));
    assertFalse(text.contains("故障"));
  }

  /** 考生准备 = 纯清单文本，入库/导出一致，无标签无头部 */
  @Test
  void kaosheng_plainListBothSides() {
    SkillGenDraftService s = service();
    assertEquals("本人身份证×1、准考证×1、黑色签字笔×1、绝缘手套×1副", s.renderKaosheng(draft()));
    assertEquals(s.renderKaosheng(draft()), s.renderKaoshengForExport(draft()),
        "入库与导出同版式");
    assertFalse(s.renderKaosheng(draft()).contains("【"));
    assertFalse(s.renderKaosheng(draft()).contains("其他准备"));
  }

  /** 考场准备缺清单/条目缺数量时的退化行为：空清单为空串，缺数量只写名称 */
  @Test
  void kaochang_degradedInputs() {
    SkillGenDraftService s = service();
    SkillGenDraftEntity empty = draft();
    empty.setSitePreparation("{\"note\":\"x\",\"equipment\":[]}");
    assertEquals("", s.renderKaochang(empty));
    assertEquals("", s.renderKaochangForExport(new SkillGenDraftEntity()));

    SkillGenDraftEntity noQty = draft();
    noQty.setSitePreparation("{\"equipment\":[{\"name\":\"记录表\",\"unit\":\"份\",\"qty\":\"\"}]}");
    assertEquals("记录表", s.renderKaochang(noQty), "数量为空只写名称，不输出悬空的×");
  }

  /** 考核试题（content）仍保留指引样例六项结构 */
  @Test
  void content_keepsGuidelineTemplate() {
    SkillGenDraftEntity d = draft();
    SkillGenDraftService s = service();
    assertTrue(s.renderContent(d).contains("【试题名称】"));
    assertTrue(s.renderContent(d).contains("【本题分值】100分，权重10%"));
    assertTrue(s.renderContent(d).contains("【考核时间】40分钟"));
    assertTrue(s.renderContent(d).contains("【考核形式】实操"));
    assertTrue(s.renderContent(d).contains("【否定项】"));
  }
}
