package com.guoyu.platform.exam.standard.export;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识树入库 SQL 导出（方案 §9.1）：exam_work_type / exam_course / exam_knowledge_structure
 * 的 INSERT 语句，目标 PHP 库（tenant_id=10159，important varchar X/Y/Z），由运维执行。
 *
 * <ul>
 *   <li>仅含理论叶（dimension=theory）；技能考评点不入库（拍板⑤）；</li>
 *   <li>PHP 表 int auto_increment → 每节点 INSERT 后 SET @k_编码 = LAST_INSERT_ID()，子节点引用父变量；</li>
 *   <li>文件头注释：来源 docx、生成时间、操作人、行数统计。</li>
 * </ul>
 */
public class KnowledgeSqlExporter {

  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** 导出模式：CREATE=新建 work_type/course（默认）；REUSE=复用目标库已有 work_type/course */
  public enum SqlExportMode {
    CREATE, REUSE
  }

  /** 导出参数（默认值对齐矿山救护工样例口径） */
  public record SqlExportOptions(
      Long workTypeParentId,   // 默认 53
      Long operatorUserId,     // 默认 26743
      Long userInCharge,       // 默认 26843
      Long tenantId,           // 默认 10159（PHP 口径）
      String operatorName,     // 文件头注释用
      SqlExportMode mode       // 默认 CREATE
  ) {
    public static SqlExportOptions defaults(String operatorName) {
      return new SqlExportOptions(53L, 26743L, 26843L, 10159L, operatorName, SqlExportMode.CREATE);
    }

    public static SqlExportOptions defaults(String operatorName, SqlExportMode mode) {
      return new SqlExportOptions(53L, 26743L, 26843L, 10159L, operatorName,
          mode != null ? mode : SqlExportMode.CREATE);
    }
  }

  private static final String[] LEVEL_SUFFIX = {"初级", "中级", "高级", "技师", "高级技师"};

  /**
   * @param workTypeTitle 职业名称
   * @param seriesNo      职业编码（如 6-16-01-12）
   * @param version       版本年
   * @param trees         key = levelCode + "|" + direction，value = 该 等级×方向 导出树
   * @param sourceFile    来源 docx 文件名
   */
  public String export(String workTypeTitle, String seriesNo, String version,
      Map<String, ExportTree> trees, String sourceFile, SqlExportOptions options) {
    return options.mode() == SqlExportMode.REUSE
        ? exportReuse(workTypeTitle, version, trees, sourceFile, options)
        : exportCreate(workTypeTitle, seriesNo, version, trees, sourceFile, options);
  }

  /**
   * 新建模式：INSERT exam_work_type + exam_course + exam_knowledge_structure（v1.3 行为）。
   */
  private String exportCreate(String workTypeTitle, String seriesNo, String version,
      Map<String, ExportTree> trees, String sourceFile, SqlExportOptions options) {
    StringBuilder sql = new StringBuilder();
    sql.append("-- ============================================================\n");
    sql.append("-- 职业技能标准知识树入库 SQL（目标：PHP 库 exam_* 表）\n");
    sql.append("-- 职业：").append(workTypeTitle).append("（编码 ").append(seriesNo).append("，")
        .append(version != null ? version + " 年版" : "版本未知").append("）\n");
    sql.append("-- 来源文件：").append(sourceFile).append('\n');
    sql.append("-- 生成时间：").append(LocalDateTime.now().format(TS))
        .append("　操作人：").append(options.operatorName()).append('\n');
    sql.append("-- 行数统计：work_type 1 条，course ").append(trees.size()).append(" 条，")
        .append("knowledge_structure 见各分段计数\n");
    sql.append("-- 【人工核对】若 exam_work_type 已存在 title='").append(esc(workTypeTitle))
        .append("' AND series_no='").append(esc(seriesNo))
        .append("' 的记录，请勿重复执行第 1 段，改为 SET @wt_0 = 已存在ID;\n");
    sql.append("-- 【人工核对】同名 exam_course 已存在时同理跳过对应段\n");
    sql.append("-- ============================================================\n\n");

    // 1) exam_work_type
    sql.append("-- 【1】exam_work_type\n");
    sql.append("INSERT INTO exam_work_type (title, series_no, parent_id, user_id, deleted, status, ")
        .append("sort_number, tenant_id, create_time) VALUES ('")
        .append(esc(workTypeTitle)).append("', '").append(esc(seriesNo)).append("', ")
        .append(options.workTypeParentId()).append(", ").append(options.operatorUserId())
        .append(", 0, 1, 1, ").append(options.tenantId()).append(", NOW());\n");
    sql.append("SET @wt_0 = LAST_INSERT_ID();\n\n");

    // 2) exam_course + 3) exam_knowledge_structure（按 等级×方向 分段）
    List<String> keys = trees.keySet().stream().sorted().toList();
    int nodeTotal = 0;
    for (String key : keys) {
      int levelCode = Integer.parseInt(key.split("\\|", 2)[0]);
      String direction = key.split("\\|", 2)[1];
      ExportTree tree = trees.get(key);
      String courseVar = courseVar(levelCode, direction);
      String courseTitle = courseTitle(workTypeTitle, levelCode, direction);

      sql.append("-- 【2】exam_course：").append(courseTitle).append('\n');
      sql.append("INSERT INTO exam_course (work_type_id, title, level, series_no_group, series_no, ")
          .append("user_in_charge, last_level_get_year, work_year_limit, min_level, min_level_year, ")
          .append("start_age, is_general, exam_condition, version, comments, user_id, status, ")
          .append("sort_number, tenant_id, create_time, deleted) VALUES (@wt_0, '")
          .append(esc(courseTitle)).append("', ").append(levelCode).append(", 1, '")
          .append(esc(seriesNo)).append("-").append(levelCode + 1).append("', ")
          .append(options.userInCharge())
          .append(", 0, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, ")
          .append(options.operatorUserId()).append(", 1, 100, ").append(options.tenantId())
          .append(", NOW(), 0);\n");
      sql.append("SET ").append(courseVar).append(" = LAST_INSERT_ID();\n\n");

      sql.append("-- 【3】exam_knowledge_structure：").append(courseTitle).append('\n');
      int before = sql.length();
      int[] nodeCount = {0};
      // 注意不能用 List.of：A/B 根可能缺失（如焊工二级/气焊工只有基本要求无工作要求），List.of 遇 null 直接 NPE
      for (ExamStandardNodeEntity root : Arrays.asList(tree.rootA, tree.rootB)) {
        if (root != null) {
          writeNode(sql, tree, root, courseVar, options, nodeCount);
        }
      }
      nodeTotal += nodeCount[0];
      sql.append("-- 本段 knowledge_structure 行数：").append(nodeCount[0]).append("\n\n");
      if (nodeCount[0] == 0) {
        sql.delete(before, sql.length());
        sql.setLength(sql.length() - 2);
      }
    }
    sql.append("-- knowledge_structure 总行数：").append(nodeTotal).append('\n');
    return sql.toString();
  }

  /**
   * 复用模式：不新建 work_type/course，按 工种(title)×等级(level) 匹配目标库已有课程，
   * 每个 course 先备份旧树到独立备份表，再 UPDATE 软删旧知识树后写入（可重复执行）。
   */
  private String exportReuse(String workTypeTitle, String version,
      Map<String, ExportTree> trees, String sourceFile, SqlExportOptions options) {
    StringBuilder sql = new StringBuilder();
    sql.append("-- ============================================================\n");
    sql.append("-- 职业技能标准知识树入库 SQL【复用已有课程模式】（目标：PHP 库 exam_* 表）\n");
    sql.append("-- 职业：").append(workTypeTitle).append("（")
        .append(version != null ? version + " 年版" : "版本未知").append("）\n");
    sql.append("-- 来源文件：").append(sourceFile).append('\n');
    sql.append("-- 生成时间：").append(LocalDateTime.now().format(TS))
        .append("　操作人：").append(options.operatorName()).append('\n');
    sql.append("-- 每个 course 段的执行顺序：\n");
    sql.append("--   ① 备份：现有知识树整份拷入 exam_knowledge_structure_bak_年月日_<courseId>\n");
    sql.append("--      （IF NOT EXISTS：当天重复执行不会覆盖首份备份）\n");
    sql.append("--   ② 软删：UPDATE ... SET deleted=1 标记该课程全部旧节点（不做物理 DELETE）\n");
    sql.append("--   ③ 写入：INSERT 新树（deleted=0）。幂等，可重复执行。\n");
    sql.append("-- 回退：软删行可 UPDATE deleted=0 复活；整课程还原用备份表\n");
    sql.append("--      （DELETE 该 course 现有行后 INSERT SELECT * FROM 备份表）。\n");
    sql.append("-- 若 exam_work_type / exam_course 匹配不到，@c_xxx 为 NULL，\n");
    sql.append("-- 后续 UPDATE/INSERT 不生效或失败——请先核对课程名与等级。\n");
    sql.append("-- ============================================================\n\n");

    // 按 key（levelCode|direction）升序；work_type 变量按方向去重
    List<String> keys = trees.keySet().stream().sorted().toList();
    Map<String, String> wtVarByDirection = new LinkedHashMap<>();
    for (String key : keys) {
      String direction = key.split("\\|", 2)[1];
      wtVarByDirection.putIfAbsent(direction, workTypeVar(direction));
    }
    // 1) 每个方向匹配已存在 exam_work_type（title = 工种名）
    sql.append("-- 【1】exam_work_type 定位（title=工种名，不新建）\n");
    for (Map.Entry<String, String> e : wtVarByDirection.entrySet()) {
      sql.append("SELECT id INTO ").append(e.getValue())
          .append(" FROM exam_work_type WHERE title='").append(esc(e.getKey()))
          .append("' AND tenant_id=").append(options.tenantId())
          .append(" AND deleted=0 LIMIT 1;\n");
    }
    sql.append('\n');

    // 2) 每个 等级×方向：定位 course → 删旧树 → 写新树
    int nodeTotal = 0;
    for (String key : keys) {
      int levelCode = Integer.parseInt(key.split("\\|", 2)[0]);
      String direction = key.split("\\|", 2)[1];
      ExportTree tree = trees.get(key);
      String wtVar = wtVarByDirection.get(direction);
      String courseVar = courseVar(levelCode, direction);
      String courseTitle = courseTitle(workTypeTitle, levelCode, direction);

      sql.append("-- 【2】exam_course 定位：").append(courseTitle).append('\n');
      sql.append("SELECT id INTO ").append(courseVar)
          .append(" FROM exam_course WHERE work_type_id=").append(wtVar)
          .append(" AND level=").append(levelCode)
          .append(" AND tenant_id=").append(options.tenantId())
          .append(" AND deleted=0 LIMIT 1;\n");
      String bakVar = "@bak_" + courseVar.substring(1);
      String bakSqlVar = "@baksql_" + courseVar.substring(1);
      sql.append("-- 【2.1】删前备份：该课程现有知识树整份拷入独立备份表（回退用，当天重复执行不覆盖首份备份）\n");
      sql.append("SET ").append(bakVar).append(" = CONCAT('exam_knowledge_structure_bak_', ")
          .append("DATE_FORMAT(NOW(), '%Y%m%d'), '_', ").append(courseVar).append(");\n");
      sql.append("SET ").append(bakSqlVar).append(" = CONCAT('CREATE TABLE IF NOT EXISTS ', ")
          .append(bakVar).append(", ' AS SELECT * FROM exam_knowledge_structure WHERE course_id = ', ")
          .append(courseVar).append(", ' AND tenant_id=").append(options.tenantId()).append("');\n");
      sql.append("PREPARE stmt FROM ").append(bakSqlVar).append(";\n");
      sql.append("EXECUTE stmt;\n");
      sql.append("DEALLOCATE PREPARE stmt;\n");
      sql.append("-- 【2.2】软删该 course 下旧知识树（物理数据仍在，可回退）\n");
      sql.append("UPDATE exam_knowledge_structure SET deleted=1 WHERE course_id = ")
          .append(courseVar).append(" AND tenant_id=").append(options.tenantId())
          .append(" AND deleted=0;\n\n");

      sql.append("-- 【3】exam_knowledge_structure：").append(courseTitle).append('\n');
      int before = sql.length();
      int[] nodeCount = {0};
      for (ExamStandardNodeEntity root : Arrays.asList(tree.rootA, tree.rootB)) {
        if (root != null) {
          writeNode(sql, tree, root, courseVar, options, nodeCount);
        }
      }
      nodeTotal += nodeCount[0];
      sql.append("-- 本段 knowledge_structure 行数：").append(nodeCount[0]).append("\n\n");
      if (nodeCount[0] == 0) {
        sql.delete(before, sql.length());
        sql.setLength(sql.length() - 2);
      }
    }
    sql.append("-- knowledge_structure 总行数：").append(nodeTotal).append('\n');
    return sql.toString();
  }

  /** 递归写节点 INSERT（仅理论叶；按层级序） */
  private void writeNode(StringBuilder sql, ExportTree tree, ExamStandardNodeEntity node,
      String courseVar, SqlExportOptions options, int[] nodeCount) {
    if (node.getNodeLevel() == 3 && !"theory".equals(node.getDimension())) {
      return; // 技能叶不入库
    }
    String var = "@k_" + node.getNodeKey().replace('-', '_');
    String parentExpr = node.getParentId() == null || node.getParentId() == 0
        ? "0" : "@k_" + parentKeyOf(tree, node).replace('-', '_');
    // 平台口径（对齐库内既有工种数据）：title=纯名称，series_no=节点编码（如 A-A-A-001）。
    // 骨架节点标题解析期带编码尾巴（titleWithKey），此处剥掉；L3 标题本就纯净不动
    String key = node.getNodeKey();
    String title = node.getTitle();
    if (title != null && key != null && title.endsWith(key)) {
      title = title.substring(0, title.length() - key.length());
    }
    sql.append("INSERT INTO exam_knowledge_structure (use_type, type, cover, intro, course_id, ")
        .append("parent_id, title, series_no, level, important, user_id, status, sort_number, ")
        .append("tenant_id, create_time, deleted, org_id, org_name, amount) VALUES (0, 0, '', '', ")
        .append(courseVar).append(", ").append(parentExpr).append(", '").append(esc(title))
        .append("', '").append(esc(key)).append("', ").append(node.getNodeLevel()).append(", ")
        .append(node.getImportant() == null ? "NULL" : "'" + esc(node.getImportant()) + "'")
        .append(", ").append(options.operatorUserId())
        .append(", 1, 100, ").append(options.tenantId())
        .append(", NOW(), 0, NULL, NULL, 10.00);\n");
    sql.append("SET ").append(var).append(" = LAST_INSERT_ID();\n");
    nodeCount[0]++;
    for (ExamStandardNodeEntity child : tree.children(node.getId())) {
      writeNode(sql, tree, child, courseVar, options, nodeCount);
    }
  }

  /** 找父节点 nodeKey（ExportTree 反查） */
  private String parentKeyOf(ExportTree tree, ExamStandardNodeEntity node) {
    // 导出树按 (levelCode|direction) 隔离，父节点必在同一棵树
    for (Map.Entry<Long, List<ExamStandardNodeEntity>> e : tree.childrenOf.entrySet()) {
      for (ExamStandardNodeEntity child : e.getValue()) {
        if (child.getId().equals(node.getId())) {
          return findKeyById(tree, e.getKey());
        }
      }
    }
    throw new IllegalStateException("父节点不在导出树中：" + node.getNodeKey());
  }

  private String findKeyById(ExportTree tree, Long id) {
    if (tree.rootA != null && tree.rootA.getId().equals(id)) {
      return tree.rootA.getNodeKey();
    }
    if (tree.rootB != null && tree.rootB.getId().equals(id)) {
      return tree.rootB.getNodeKey();
    }
    for (List<ExamStandardNodeEntity> children : tree.childrenOf.values()) {
      for (ExamStandardNodeEntity n : children) {
        if (n.getId().equals(id)) {
          return n.getNodeKey();
        }
      }
    }
    throw new IllegalStateException("节点不在导出树中：" + id);
  }

  private String courseVar(int levelCode, String direction) {
    return "@c_" + levelCode + "_" + Math.abs(direction.hashCode() % 1000);
  }

  /** 工种（方向）变量：@wt_<方向hash>，复用模式按方向定位 exam_work_type */
  private String workTypeVar(String direction) {
    return "@wt_" + Math.abs(direction.hashCode() % 1000);
  }

  /** 课程名：单方向「矿山救护工初级」，多方向「焊工初级-电焊工」 */
  private String courseTitle(String workTypeTitle, int levelCode, String direction) {
    String base = workTypeTitle + LEVEL_SUFFIX[Math.min(levelCode, 4)];
    return "默认".equals(direction) ? base : base + "-" + direction;
  }

  private String esc(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
  }
}
