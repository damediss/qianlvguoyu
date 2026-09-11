package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.export.MubiaoExcelExporter;
import com.guoyu.platform.exam.standard.export.StandardExportSupport;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.service.StandardTreeBuilder;
import com.guoyu.platform.exam.standard.service.StandardTreeDraft;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * 细目表导出名称/相关知识版式的真实数据回归（重冶备料工 docx）：
 * 二级（L1）、三级（L2）名称不带枚举序号、中文之间无空格，三级相关知识逐行且编号从 1 起。
 */
class MubiaoRealDataExportTest {

  private static final long TASK_ID = 9001L;

  @Test
  void beiliaoExportNamesAndKnowledgeAreNormalized() throws Exception {
    ParsedStandard parsed;
    try (InputStream in = getClass().getResourceAsStream("/standard/重冶备料工.docx")) {
      assertNotNull(in, "测试资源缺失：重冶备料工.docx");
      parsed = new DocxStandardParser().parse(in);
    }
    List<ExamStandardNodeEntity> nodes = toEntities(new StandardTreeBuilder().build(parsed));
    ExportTree tree = StandardExportSupport.assemble(nodes);

    byte[] bytes = new MubiaoExcelExporter().export(tree, "Sheet1",
        "重冶备料工(五级/初级工)理论知识考评要素细目表");
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheet("Sheet1");
      List<String> l1Names = new ArrayList<>();
      List<String> l2Names = new ArrayList<>();
      String peiliaoKnowledge = null;
      for (int r = 4; r <= sheet.getLastRowNum(); r++) {
        if (sheet.getRow(r) == null) {
          continue;
        }
        String l1 = firstLine(sheet.getRow(r), 2);
        String l2 = firstLine(sheet.getRow(r), 4);
        if (!l1.isEmpty()) {
          l1Names.add(l1);
        }
        if (!l2.isEmpty()) {
          l2Names.add(l2);
          if ("配料".equals(l2)) {
            peiliaoKnowledge = cellText(sheet.getRow(r), 6);
          }
        }
      }

      assertTrue(l1Names.contains("工作准备") && l1Names.contains("设备维护")
              && l1Names.contains("安全操作"),
          "二级名称应去掉枚举序号（一、/四、/五、）：" + l1Names);
      assertEquals(1, l1Names.stream().filter("物料制备（工作内容三至五为可选）"::equals).count(),
          "跨页续表不应产生重复的物料制备节点：" + l1Names);
      assertTrue(l2Names.contains("交接班") && l2Names.contains("配料") && l2Names.contains("干燥"),
          "三级名称应去掉「（一）」这类序号：" + l2Names);
      for (String name : l1Names) {
        assertFalse(name.matches("^[（(]?[一二三四五六七八九十\\d]+[)）.、．].*"), "名称残留序号：" + name);
        assertFalse(name.matches(".*[\\u4e00-\\u9fff] +[\\u4e00-\\u9fff].*"), "名称残留空格：" + name);
      }

      assertNotNull(peiliaoKnowledge, "应导出物料制备/配料小节");
      assertEquals("1.物料的名称、性质、外观特点及化学成分\n2.岗位技术操作规程", peiliaoKnowledge,
          "相关知识应逐点换行且编号从 1 起");
    }
  }

  private String firstLine(org.apache.poi.ss.usermodel.Row row, int col) {
    String text = cellText(row, col);
    int nl = text.indexOf('\n');
    return nl >= 0 ? text.substring(0, nl) : text;
  }

  private String cellText(org.apache.poi.ss.usermodel.Row row, int col) {
    var cell = row.getCell(col);
    return cell == null ? "" : cell.getStringCellValue();
  }

  /** 与 StandardParseApplicationService#persistTree 相同的草稿→实体映射（测试内自持 id） */
  private List<ExamStandardNodeEntity> toEntities(List<StandardTreeDraft> drafts) {
    Map<String, Long> idByKey = new LinkedHashMap<>();
    long id = 1000L;
    for (StandardTreeDraft draft : drafts) {
      idByKey.put(mapKey(draft), id++);
    }
    List<ExamStandardNodeEntity> nodes = new ArrayList<>();
    for (StandardTreeDraft draft : drafts) {
      if (draft.levelCode() != 0) {
        continue; // 只导出五级
      }
      ExamStandardNodeEntity node = new ExamStandardNodeEntity();
      node.setId(idByKey.get(mapKey(draft)));
      node.setTenantId(1L);
      node.setTaskId(TASK_ID);
      node.setParentId(draft.parentNodeKey() == null ? 0L
          : idByKey.get(mapKey(draft.levelCode(), draft.direction(), draft.parentNodeKey())));
      node.setNodeKey(draft.nodeKey());
      node.setNodeLevel(draft.nodeLevel());
      node.setSortNumber(draft.sortNumber());
      node.setDimension(draft.dimension());
      node.setTitle(draft.title());
      node.setTitleRaw(draft.titleRaw());
      node.setImportant(draft.important());
      node.setWeight(draft.weight());
      node.setRelatedKnowledge(draft.relatedKnowledge());
      node.setSkillRequirement(draft.skillRequirement());
      node.setSource(draft.source());
      node.setDirection(draft.direction());
      node.setLevelCode(draft.levelCode());
      node.setNeedLlm(draft.needLlm() ? 1 : 0);
      nodes.add(node);
    }
    return nodes;
  }

  private String mapKey(StandardTreeDraft draft) {
    return mapKey(draft.levelCode(), draft.direction(), draft.nodeKey());
  }

  private String mapKey(int levelCode, String direction, String nodeKey) {
    return levelCode + "|" + direction + "|" + nodeKey;
  }
}
