package com.guoyu.platform.exam.skillgen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 操作技能试题导出（方案 §8，决策 C8；2026-09-11 需求方调整为**单 Sheet**）：
 * 仅《评分记录表》一个 Sheet——一行一个评分要素，列：考评点 / 实操题目 / 考场准备 / 考生准备 /
 * 评分要素 / 配分 / 评分标准 / 最大扣分 / 步长（已去掉试题名称列；不再输出试题表与批次报告）。
 */
@Service
public class SkillGenExportService {

  private static final String[] SHEET2_HEADERS = {
      "考评点", "实操题目", "考场准备", "考生准备",
      "评分要素", "配分", "评分标准", "最大扣分", "步长"
  };

  /**
   * 列宽（单位：1/256 字符；中文按 2 倍宽估算，2026-09-11 需求方反馈"太窄"）。
   * 顺序与 SHEET2_HEADERS 一致：考评点/实操题目/考场准备/考生准备/评分要素/配分/评分标准/最大扣分/步长。
   */
  static final int[] SHEET2_WIDTHS = {
      26 * 256, 42 * 256, 62 * 256, 30 * 256, 24 * 256, 8 * 256, 48 * 256, 10 * 256, 8 * 256
  };

  /** 需要自动换行的列（长文本列；考场准备/考生准备为清单文字通常单行，保留 wrap 以防长列表折行） */
  private static final boolean[] SHEET2_WRAP = {
      false, true, true, true, false, false, true, false, false
  };

  private final SkillGenDraftService draftService;
  private final SkillGenBatchMapper batchMapper;
  private final SkillBlueprintApplicationService blueprintService;
  private final com.guoyu.platform.exam.mapper.ExamWorkTypeMapper workTypeMapper;
  private final ObjectMapper objectMapper;

  public SkillGenExportService(SkillGenDraftService draftService, SkillGenBatchMapper batchMapper,
      SkillBlueprintApplicationService blueprintService,
      com.guoyu.platform.exam.mapper.ExamWorkTypeMapper workTypeMapper, ObjectMapper objectMapper) {
    this.draftService = draftService;
    this.batchMapper = batchMapper;
    this.blueprintService = blueprintService;
    this.workTypeMapper = workTypeMapper;
    this.objectMapper = objectMapper;
  }

  /** 导出文件：xlsx 字节流 + 中文文件名 */
  public record ExportFile(byte[] content, String filename) {
  }

  public ExportFile exportBatch(Long tenantId, Long batchId) throws IOException {
    SkillGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    SkillBlueprintEntity bp = blueprintService.findBlueprint(tenantId, batch.getBlueprintId());
    List<SkillGenDraftEntity> drafts = draftService.listAll(tenantId, batchId);
    Map<Long, SkillBlueprintNodeEntity> nodeMap = new LinkedHashMap<>();
    if (bp != null) {
      for (SkillBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId, bp.getId())) {
        nodeMap.put(n.getId(), n);
      }
    }

    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeSheet2(wb, drafts, nodeMap);
      wb.write(out);
      return new ExportFile(out.toByteArray(), buildFilename(workTypeTitle(bp), bp == null ? null : bp.getLevel(), batchId));
    }
  }

  /** 工种名（exam_work_type.title）；查不到时回退蓝图标题前缀 */
  private String workTypeTitle(SkillBlueprintEntity bp) {
    if (bp == null) {
      return "";
    }
    com.guoyu.platform.exam.entity.ExamWorkTypeEntity wt =
        workTypeMapper.selectById(Long.valueOf(bp.getWorkTypeId()));
    if (wt != null && wt.getTitle() != null && !wt.getTitle().isBlank()) {
      return wt.getTitle();
    }
    return "";
  }

  /** 文件名规则（用户 2026-09-10 确认）：工种(等级)_操作技能试题_批次{短号}.xlsx，等级形如"二级-技师" */
  static String buildFilename(String workTypeTitle, Integer level, Long batchId) {
    String wt = workTypeTitle == null || workTypeTitle.isBlank() ? "未指定工种" : workTypeTitle.trim();
    String levelText = levelText(level);
    String shortId = String.valueOf(batchId);
    if (shortId.length() > 8) {
      shortId = shortId.substring(shortId.length() - 8);
    }
    return String.format("%s(%s)_操作技能试题_批次%s.xlsx", wt, levelText, shortId);
  }

  /** 等级中文写法："二级-技师"（1~5 → 一/二/三/四/五级-等级名）；level 为空时退化为"未知等级" */
  static String levelText(Integer level) {
    if (level == null || level < 1 || level > 5) {
      return "未知等级";
    }
    String[] digits = {"一", "二", "三", "四", "五"};
    String name = SkillBlueprintApplicationService.LEVEL_NAMES[level];
    return digits[level - 1] + "级-" + name;
  }

  /** 评分记录表：一行一个评分要素（试题级字段按题重复填充；已去掉试题名称列） */
  private void writeSheet2(Workbook workbook, List<SkillGenDraftEntity> drafts,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    Sheet sheet = workbook.createSheet("评分记录表");
    // 列宽（2026-09-11 调整：原默认宽度太窄，长文本列几乎不可读）
    for (int i = 0; i < SHEET2_WIDTHS.length && i < SHEET2_HEADERS.length; i++) {
      sheet.setColumnWidth(i, SHEET2_WIDTHS[i]);
    }
    // 表头加粗 + 长文本列自动换行（未开启 wrap 时 Excel 只显示一行）
    CellStyle headerStyle = workbook.createCellStyle();
    Font bold = workbook.createFont();
    bold.setBold(true);
    headerStyle.setFont(bold);
    headerStyle.setWrapText(false);
    CellStyle wrapStyle = workbook.createCellStyle();
    wrapStyle.setWrapText(true);
    CellStyle plainStyle = workbook.createCellStyle();
    plainStyle.setWrapText(false);

    Row head = sheet.createRow(0);
    for (int i = 0; i < SHEET2_HEADERS.length; i++) {
      Cell cell = head.createCell(i);
      cell.setCellValue(SHEET2_HEADERS[i]);
      cell.setCellStyle(headerStyle);
    }
    int r = 1;
    for (SkillGenDraftEntity d : drafts) {
      SkillBlueprintNodeEntity point = nodeMap.get(d.getNodeId());
      List<Map<String, String>> rows = parseScoring(d.getScoringItems());
      for (Map<String, String> item : rows) {
        Row row = sheet.createRow(r++);
        int c = 0;
        // 考评点列＝名称+代码（此行是识别考评点的唯一依据）
        cell(row, c++, point == null ? ""
            : nvl(point.getName()) + nvl(point.getFullCode()), wrapStyle, plainStyle);
        // 实操题目列＝简短题名（2026-09-11 需求方确认，对齐样本）
        cell(row, c++, nvl(d.getQuestionName()), wrapStyle, plainStyle);
        // 考场准备/考生准备＝纯文字清单（2026-09-11 需求方确认，与入库同版式）
        cell(row, c++, draftService.renderKaochangForExport(d), wrapStyle, plainStyle);
        cell(row, c++, draftService.renderKaoshengForExport(d), wrapStyle, plainStyle);
        cell(row, c++, nvl(item.get("item")), wrapStyle, plainStyle);
        cell(row, c++, nvl(item.get("score")), wrapStyle, plainStyle);
        cell(row, c++, nvl(item.get("scoreDesc")), wrapStyle, plainStyle);
        cell(row, c++, nvl(item.get("maxDeduct")), wrapStyle, plainStyle);
        cell(row, c, nvl(item.get("step")), wrapStyle, plainStyle);
      }
    }
    // 前 4 列（考评点/实操题目/考场准备/考生准备）相邻同值纵向合并（2026-09-12 需求，
    // 对齐《矿山监控岗位实操考评评分表》样本版式：同题多评分要素行合并展示）
    mergeEqualColumns(sheet, 4);
  }

  /**
   * 前 mergeCols 列：相邻行值相同（非空白）则纵向合并单元格，0 行为表头不参与。
   * 空白值不合并——避免空准备清单跨题连成一块造成误导；单行 run 无需合并。
   */
  private void mergeEqualColumns(Sheet sheet, int mergeCols) {
    int lastRow = sheet.getLastRowNum();
    for (int col = 0; col < mergeCols; col++) {
      int runStart = 1;
      String runValue = valueAt(sheet, runStart, col);
      for (int r = 2; r <= lastRow + 1; r++) {
        // r = lastRow+1 作哨兵收敛最后一个 run
        String v = r <= lastRow ? valueAt(sheet, r, col) : null;
        boolean same = v != null && v.equals(runValue) && !v.isBlank();
        if (!same) {
          if (runValue != null && !runValue.isBlank() && r - 1 > runStart) {
            sheet.addMergedRegion(new CellRangeAddress(runStart, r - 1, col, col));
          }
          runStart = r;
        }
        runValue = v;
      }
    }
  }

  private String valueAt(Sheet sheet, int r, int col) {
    Row row = sheet.getRow(r);
    if (row == null || row.getCell(col) == null) {
      return "";
    }
    return row.getCell(col).getStringCellValue();
  }

  /** 写入单元格并按列应用换行样式 */
  private void cell(Row row, int col, String value, CellStyle wrapStyle, CellStyle plainStyle) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(col < SHEET2_WRAP.length && SHEET2_WRAP[col] ? wrapStyle : plainStyle);
  }

  private List<Map<String, String>> parseScoring(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() { });
    } catch (Exception e) {
      return List.of();
    }
  }

  private String nvl(Object o) {
    return o == null ? "" : String.valueOf(o);
  }
}
