package com.guoyu.platform.exam.standard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.export.KnowledgeSqlExporter;
import com.guoyu.platform.exam.standard.export.KnowledgeSqlExporter.SqlExportMode;
import com.guoyu.platform.exam.standard.export.MubiaoExcelExporter;
import com.guoyu.platform.exam.standard.export.SkillMubiaoExcelExporter;
import com.guoyu.platform.exam.standard.export.StandardExportSupport;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 细目表导出编排（方案 §9/§11 #9~#11）：理论细目表 Excel / 操作技能细目表 Excel / 入库 SQL。
 * 导出不再强制要求叶节点已审核；review-done 仍要求无 pending。
 */
@Service
public class StandardExportApplicationService {

  private static final Logger log = LoggerFactory.getLogger(StandardExportApplicationService.class);
  private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final String[] LEVEL_SHORT = {"五", "四", "三", "二", "一"};

  private final ExamStandardTaskMapper taskMapper;
  private final ExamStandardNodeMapper nodeMapper;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public StandardExportApplicationService(ExamStandardTaskMapper taskMapper,
      ExamStandardNodeMapper nodeMapper,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.nodeMapper = nodeMapper;
    this.objectMapper = objectMapper;
  }

  /** 导出文件载体 */
  public record ExportFile(String fileName, String contentType, byte[] content) {
  }

  // ============================================================
  // 理论细目表 Excel
  // ============================================================

  @Transactional
  public ExportFile exportTheoryExcel(Long taskId, Integer levelCode, String direction,
      Long tenantId) throws IOException {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    List<TreeScope> scopes = resolveScopes(taskId, levelCode, direction);
    markExported(task);
    if (scopes.size() == 1) {
      TreeScope scope = scopes.get(0);
      byte[] content = new MubiaoExcelExporter().export(scope.tree(),
          "Sheet1", theoryTitle(task, scope));
      return new ExportFile(excelFileName(task, scope, "理论知识考评要素细目表"),
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }
    return zipScopes(task, scopes, "理论知识考评要素细目表", scope -> {
      try {
        return new MubiaoExcelExporter().export(scope.tree(), "Sheet1", theoryTitle(task, scope));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  // ============================================================
  // 操作技能细目表 Excel
  // ============================================================

  @Transactional
  public ExportFile exportSkillExcel(Long taskId, Integer levelCode, String direction,
      Long tenantId) throws IOException {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    Map<String, StandardWeightTable> weights =
        StandardExportSupport.restoreWeights(task.getWeightsJson());
    StandardWeightTable skillTable = weights.get("skill");
    List<TreeScope> scopes = resolveScopes(taskId, levelCode, direction);
    markExported(task);
    if (scopes.size() == 1) {
      TreeScope scope = scopes.get(0);
      byte[] content = new SkillMubiaoExcelExporter().export(scope.tree(),
          skillTitle(task, scope), careerLine(task, scope),
          l1 -> skillWeightOf(skillTable, l1, scope));
      return new ExportFile(excelFileName(task, scope, "操作技能考评要素细目表"),
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }
    return zipScopes(task, scopes, "操作技能考评要素细目表", scope -> {
      try {
        return new SkillMubiaoExcelExporter().export(scope.tree(),
            skillTitle(task, scope), careerLine(task, scope),
            l1 -> skillWeightOf(skillTable, l1, scope));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  /** 技能权重表取值：L1 标题剥离编码后缀后按项目名匹配（百分比 → 小数） */
  private BigDecimal skillWeightOf(StandardWeightTable skillTable, ExamStandardNodeEntity l1,
      TreeScope scope) {
    if (skillTable == null) {
      return null; // 无技能权重表 → 留空（§12）
    }
    String funcTitle = l1.getTitle().replaceAll("[A-Z](-[A-Z]){0,2}$", "");
    var row = skillTable.findRow(
        com.guoyu.platform.exam.standard.parser.model.DocxTextUtil.normalizeItemName(funcTitle));
    if (row == null) {
      return null;
    }
    BigDecimal percent = row.valueAt(scope.levelCode(), scope.direction());
    return percent == null ? null
        : percent.movePointLeft(2).setScale(4, java.math.RoundingMode.HALF_UP);
  }

  // ============================================================
  // 入库 SQL
  // ============================================================

  @Transactional
  public ExportFile exportSql(Long taskId, Long tenantId, String operatorName, String mode) {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    List<TreeScope> scopes = resolveScopes(taskId, null, null);
    markExported(task);
    Map<String, ExportTree> trees = new LinkedHashMap<>();
    for (TreeScope scope : scopes) {
      trees.put(scope.levelCode() + "|" + scope.direction(), scope.tree());
    }
    SqlExportMode sqlMode = "reuse".equalsIgnoreCase(mode) ? SqlExportMode.REUSE
        : SqlExportMode.CREATE;
    String sql = new KnowledgeSqlExporter().export(task.getWorkTypeTitle(),
        task.getSeriesNo() != null ? task.getSeriesNo() : "", task.getVersion(), trees,
        "task-" + taskId + ".docx", KnowledgeSqlExporter.SqlExportOptions.defaults(operatorName,
            sqlMode));
    return new ExportFile(
        task.getWorkTypeTitle() + "_knowledge_structure_" + FILE_TS.format(LocalDateTime.now())
            + ".sql",
        "application/sql; charset=UTF-8", sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  // ============================================================
  // 共用
  // ============================================================

  /** 解析导出范围：参数缺省时覆盖任务内全部 等级×方向 */
  private List<TreeScope> resolveScopes(Long taskId, Integer levelCode, String direction) {
    LambdaQueryWrapper<ExamStandardNodeEntity> wrapper = new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, taskId)
        .eq(levelCode != null, ExamStandardNodeEntity::getLevelCode, levelCode)
        .eq(direction != null && !direction.isBlank(), ExamStandardNodeEntity::getDirection,
            direction)
        .orderByAsc(ExamStandardNodeEntity::getLevelCode)
        .orderByAsc(ExamStandardNodeEntity::getDirection)
        .orderByAsc(ExamStandardNodeEntity::getNodeKey);
    List<ExamStandardNodeEntity> nodes = nodeMapper.selectList(wrapper);
    Map<String, List<ExamStandardNodeEntity>> grouped = new LinkedHashMap<>();
    for (ExamStandardNodeEntity node : nodes) {
      grouped.computeIfAbsent(node.getLevelCode() + "|" + node.getDirection(),
          k -> new ArrayList<>()).add(node);
    }
    List<TreeScope> scopes = new ArrayList<>();
    for (Map.Entry<String, List<ExamStandardNodeEntity>> e : grouped.entrySet()) {
      String[] parts = e.getKey().split("\\|", 2);
      scopes.add(new TreeScope(Integer.parseInt(parts[0]), parts[1],
          StandardExportSupport.assemble(e.getValue())));
    }
    if (scopes.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "任务下无可导出的树节点");
    }
    return scopes;
  }

  private ExportFile zipScopes(ExamStandardTaskEntity task, List<TreeScope> scopes, String kind,
      ScopeWriter writer) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(buf, java.nio.charset.StandardCharsets.UTF_8)) {
      for (TreeScope scope : scopes) {
        zip.putNextEntry(new ZipEntry(excelFileName(task, scope, kind)));
        zip.write(writer.write(scope));
        zip.closeEntry();
      }
    }
    return new ExportFile(task.getWorkTypeTitle() + "_" + kind + "_"
        + FILE_TS.format(LocalDateTime.now()) + ".zip", "application/zip", buf.toByteArray());
  }

  private interface ScopeWriter {
    byte[] write(TreeScope scope);
  }

  private String theoryTitle(ExamStandardTaskEntity task, TreeScope scope) {
    return task.getWorkTypeTitle() + "(" + levelName(task, scope.levelCode()) + ")"
        + ("默认".equals(scope.direction()) ? "" : scope.direction())
        + "理论知识考评要素细目表";
  }

  private String skillTitle(ExamStandardTaskEntity task, TreeScope scope) {
    return task.getWorkTypeTitle() + "（" + levelName(task, scope.levelCode()) + "）"
        + "操作技能考评要素细目表";
  }

  private String careerLine(ExamStandardTaskEntity task, TreeScope scope) {
    return "职业： " + task.getWorkTypeTitle() + "                级别： "
        + levelName(task, scope.levelCode());
  }

  private String excelFileName(ExamStandardTaskEntity task, TreeScope scope, String kind) {
    String directionPart = "默认".equals(scope.direction()) ? "" : scope.direction();
    // 文件名不能含 "/"，完整等级名（二级/技师）中的斜杠替换为短横
    String levelPart = levelName(task, scope.levelCode()).replace('/', '-');
    return task.getWorkTypeTitle() + "(" + levelPart + ")"
        + directionPart + "_" + kind + "_" + FILE_TS.format(LocalDateTime.now()) + ".xlsx";
  }

  /** 完整等级名（"二级/技师"）：取自任务 levelsJson，缺失时回退简写（"二级"） */
  private String levelName(ExamStandardTaskEntity task, int levelCode) {
    String json = task.getLevelsJson();
    if (json != null && !json.isBlank()) {
      try {
        List<Map<String, Object>> levels = objectMapper.readValue(json, List.class);
        for (Map<String, Object> level : levels) {
          Object code = level.get("levelCode");
          Object name = level.get("levelName");
          if (code instanceof Number && ((Number) code).intValue() == levelCode
              && name != null && !name.toString().isBlank()) {
            return name.toString();
          }
        }
      } catch (Exception e) {
        log.warn("[细目表导出] levelsJson 解析失败，回退简写等级名: {}", e.getMessage());
      }
    }
    return LEVEL_SHORT[Math.min(levelCode, 4)] + "级";
  }

  /** 导出成功后状态流转（reviewing/confirmed → exported） */
  private void markExported(ExamStandardTaskEntity task) {
    if (StandardParseApplicationService.STATUS_REVIEWING.equals(task.getStatus())
        || StandardParseApplicationService.STATUS_CONFIRMED.equals(task.getStatus())) {
      ExamStandardTaskEntity update = new ExamStandardTaskEntity();
      update.setId(task.getId());
      update.setStatus(StandardParseApplicationService.STATUS_EXPORTED);
      taskMapper.updateById(update);
    }
  }

  private ExamStandardTaskEntity findTask(Long taskId, Long tenantId) {
    ExamStandardTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<ExamStandardTaskEntity>()
        .eq(ExamStandardTaskEntity::getTenantId, tenantId)
        .eq(ExamStandardTaskEntity::getId, taskId));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "解析任务不存在");
    }
    return task;
  }

  /** 导出范围（等级×方向 + 装配树） */
  private record TreeScope(int levelCode, String direction, ExportTree tree) {
  }
}
