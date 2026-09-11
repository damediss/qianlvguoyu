package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * 导出结构回归（2026-09-11 需求方调整）：**单 Sheet《评分记录表》**——
 * 已删除《试题表》《批次报告》；列共 9 个且不含"试题名称"；考评点列＝名称+代码。
 */
class SkillGenExportServiceTest {

  private static final Long TENANT = 1L;
  private static final Long BATCH_ID = 2097882600178417666L;
  private static final Long BP_ID = 100L;
  private static final Long POINT_ID = 11L;

  private SkillGenDraftService draftService() {
    // 真服务（渲染方法走真实实现），仅 listAll 打桩
    SkillGenDraftService real = new SkillGenDraftService(null, null, null, null, null, null, null,
        new ObjectMapper());
    SkillGenDraftService spy = spy(real);
    doReturn(List.of(draft())).when(spy).listAll(TENANT, BATCH_ID);
    return spy;
  }

  private SkillGenDraftEntity draft() {
    SkillGenDraftEntity d = new SkillGenDraftEntity();
    d.setId(1L);
    d.setTenantId(TENANT);
    d.setBatchId(BATCH_ID);
    d.setBlueprintId(BP_ID);
    d.setNodeId(POINT_ID);
    d.setQuestionName("冷态条件下集控室监控系统联机启动");
    d.setScore(new BigDecimal(100));
    d.setDurationMinutes(40);
    d.setExamForm("实操");
    d.setSitePreparation("{\"note\":\"考核前确认断电\",\"equipment\":[{\"name\":\"监控主机\","
        + "\"spec\":\"工控机\",\"unit\":\"套\",\"qty\":\"1\",\"remark\":\"\"}],\"faultSetup\":\"无故障设置\"}");
    d.setExamineePrepare("黑色签字笔、绝缘手套");
    d.setRequirements("1.按操作规程完成开机前环境与电源检查；2.完成联机启动");
    d.setNegatives("若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违规");
    d.setScoringItems("[{\"item\":\"开机前环境与电源检查\",\"score\":\"20\",\"scoreDesc\":\"漏一项扣5分\","
        + "\"maxDeduct\":\"20\",\"step\":\"5\"},{\"item\":\"联机启动\",\"score\":\"80\","
        + "\"scoreDesc\":\"顺序错误扣20分\",\"maxDeduct\":\"80\",\"step\":\"10\"}]");
    d.setDifficulty(3);
    return d;
  }

  private SkillGenExportService service(SkillGenDraftService draftService,
      SkillGenBatchMapper batchMapper, SkillBlueprintApplicationService blueprintService,
      ExamWorkTypeMapper workTypeMapper) {
    return new SkillGenExportService(draftService, batchMapper, blueprintService, workTypeMapper,
        new ObjectMapper());
  }

  @Test
  void singleSheetNineColumnsWithoutQuestionName() throws Exception {
    SkillGenBatchMapper batchMapper = mock(SkillGenBatchMapper.class);
    SkillBlueprintApplicationService blueprintService =
        mock(SkillBlueprintApplicationService.class);
    ExamWorkTypeMapper workTypeMapper = mock(ExamWorkTypeMapper.class);

    SkillGenBatchEntity batch = new SkillGenBatchEntity();
    batch.setId(BATCH_ID);
    batch.setTenantId(TENANT);
    batch.setBlueprintId(BP_ID);
    when(batchMapper.selectById(BATCH_ID)).thenReturn(batch);

    SkillBlueprintEntity bp = new SkillBlueprintEntity();
    bp.setId(BP_ID);
    bp.setTenantId(TENANT);
    bp.setWorkTypeId(144);
    bp.setLevel(2);
    when(blueprintService.findBlueprint(TENANT, BP_ID)).thenReturn(bp);

    SkillBlueprintNodeEntity point = new SkillBlueprintNodeEntity();
    point.setId(POINT_ID);
    point.setParentId(0L);
    point.setNodeLevel(3);
    point.setName("联机启动集控室监控设备");
    point.setFullCode("A-A-001");
    when(blueprintService.findBlueprintNodes(TENANT, BP_ID)).thenReturn(List.of(point));

    ExamWorkTypeEntity wt = new ExamWorkTypeEntity();
    wt.setId(144);
    wt.setTitle("矿山生产集控员");
    when(workTypeMapper.selectById(any())).thenReturn(wt);

    var file = service(draftService(), batchMapper, blueprintService, workTypeMapper)
        .exportBatch(TENANT, BATCH_ID);

    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
      // ① 仅一个 Sheet：评分记录表
      assertEquals(1, wb.getNumberOfSheets());
      assertEquals("评分记录表", wb.getSheetName(0));
      Sheet sheet = wb.getSheetAt(0);
      Row head = sheet.getRow(0);
      // ② 9 列，且不含"试题名称"
      assertEquals(9, head.getLastCellNum());
      assertArrayEquals(new String[] {"考评点", "实操题目", "考场准备", "考生准备",
          "评分要素", "配分", "评分标准", "最大扣分", "步长"},
          rowTexts(head));
      for (int i = 0; i < head.getLastCellNum(); i++) {
        assertFalse("试题名称".equals(head.getCell(i).getStringCellValue()), "不应含试题名称列");
      }
      // ③ 数据行：考评点＝名称+代码；一行一个评分要素（2 条评分 → 2 行）
      assertEquals(3, sheet.getLastRowNum() + 1);
      Row first = sheet.getRow(1);
      assertEquals("联机启动集控室监控设备A-A-001", first.getCell(0).getStringCellValue());
      assertTrue(first.getCell(2).getStringCellValue().contains("【考核有关说明】"));
      assertTrue(first.getCell(2).getStringCellValue().contains("| 监控主机 |"));
      assertEquals("黑色签字笔、绝缘手套", first.getCell(3).getStringCellValue());
      assertEquals("1. 开机前环境与电源检查", first.getCell(4).getStringCellValue());
      assertEquals("20", first.getCell(5).getStringCellValue());
      assertEquals("2. 联机启动", sheet.getRow(2).getCell(4).getStringCellValue());
      // ④ 列宽已按内容设置（2026-09-11 反馈"太窄"）且长文本列开启换行
      assertEquals(SkillGenExportService.SHEET2_WIDTHS[2], sheet.getColumnWidth(2),
          "考场准备列宽应显式设置");
      assertTrue(sheet.getColumnWidth(2) > sheet.getColumnWidth(5), "长文本列应明显宽于配分列");
      assertEquals(62 * 256, sheet.getColumnWidth(2));
      assertTrue(sheet.getRow(1).getCell(2).getCellStyle().getWrapText(), "考场准备应自动换行");
      assertTrue(sheet.getRow(1).getCell(1).getCellStyle().getWrapText(), "实操题目应自动换行");
      assertFalse(sheet.getRow(1).getCell(5).getCellStyle().getWrapText(), "配分列无需换行");
      int fontIdx = sheet.getRow(0).getCell(0).getCellStyle().getFontIndexAsInt();
      assertTrue(((XSSFWorkbook) wb).getFontAt(fontIdx).getBold(), "表头应加粗");
    }
    // ⑤ 文件名规则不变
    assertEquals("矿山生产集控员(二级-技师)_操作技能试题_批次78417666.xlsx", file.filename());
  }

  private String[] rowTexts(Row row) {
    String[] out = new String[row.getLastCellNum()];
    for (int i = 0; i < out.length; i++) {
      out[i] = row.getCell(i) == null ? null : row.getCell(i).getStringCellValue();
    }
    return out;
  }
}
