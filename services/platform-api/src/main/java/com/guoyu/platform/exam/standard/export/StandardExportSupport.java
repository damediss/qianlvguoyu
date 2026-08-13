package com.guoyu.platform.exam.standard.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出共享支持：staging 节点装配为树、X/Y/Z 子树统计、L2 权重按叶数比例计算、权重表 JSON 还原。
 */
public final class StandardExportSupport {

  private StandardExportSupport() {
  }

  /** 单 等级×方向 的导出树 */
  public static final class ExportTree {
    public ExamStandardNodeEntity rootA;            // 基本要求
    public ExamStandardNodeEntity rootB;            // 工作要求
    /** L1/L2 节点按 sortNumber 排序；children 通过 childrenOf 取 */
    public final Map<Long, List<ExamStandardNodeEntity>> childrenOf = new LinkedHashMap<>();

    public List<ExamStandardNodeEntity> children(Long parentId) {
      return childrenOf.getOrDefault(parentId, List.of());
    }
  }

  /** 由扁平节点列表装配导出树（nodeKey 排序保证与 sortNumber 一致的稳定序） */
  public static ExportTree assemble(List<ExamStandardNodeEntity> nodes) {
    ExportTree tree = new ExportTree();
    List<ExamStandardNodeEntity> sorted = new ArrayList<>(nodes);
    sorted.sort(Comparator.comparing(ExamStandardNodeEntity::getNodeLevel)
        .thenComparing(ExamStandardNodeEntity::getSortNumber)
        .thenComparing(ExamStandardNodeEntity::getNodeKey));
    for (ExamStandardNodeEntity node : sorted) {
      if (node.getNodeLevel() == 0) {
        if ("A".equals(node.getNodeKey())) {
          tree.rootA = node;
        } else if ("B".equals(node.getNodeKey())) {
          tree.rootB = node;
        }
        continue;
      }
      tree.childrenOf.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
    }
    return tree;
  }

  /** 子树内 X:Y:Z 叶计数（dimension 过滤；null important 按 X 计） */
  public static int[] countXyz(ExportTree tree, Long nodeId, String dimension) {
    int[] counts = new int[3];
    for (ExamStandardNodeEntity child : tree.children(nodeId)) {
      if (child.getNodeLevel() == 3) {
        if (dimension == null || dimension.equals(child.getDimension())) {
          String imp = child.getImportant();
          if (imp == null || "X".equals(imp)) {
            counts[0]++;
          } else if ("Y".equals(imp)) {
            counts[1]++;
          } else if ("Z".equals(imp)) {
            counts[2]++;
          }
        }
      } else {
        int[] sub = countXyz(tree, child.getId(), dimension);
        counts[0] += sub[0];
        counts[1] += sub[1];
        counts[2] += sub[2];
      }
    }
    return counts;
  }

  public static String xyzText(int[] counts) {
    return "（" + counts[0] + ":" + counts[1] + ":" + counts[2] + "）";
  }

  /** L1 子树理论叶数 */
  public static int leafCount(ExportTree tree, Long nodeId, String dimension) {
    int[] c = countXyz(tree, nodeId, dimension);
    return c[0] + c[1] + c[2];
  }

  /**
   * L2 考评权重：节点手工权重优先；否则 L1 权重 ×（L2 叶数 ÷ L1 总叶数），4 位小数。
   */
  public static BigDecimal l2Weight(ExportTree tree, ExamStandardNodeEntity l1,
      ExamStandardNodeEntity l2) {
    if (l2.getWeight() != null) {
      return l2.getWeight();
    }
    if (l1.getWeight() == null) {
      return null;
    }
    int l1Total = leafCount(tree, l1.getId(), "theory");
    int l2Count = leafCount(tree, l2.getId(), "theory");
    if (l1Total == 0 || l2Count == 0) {
      return null;
    }
    return l1.getWeight()
        .multiply(BigDecimal.valueOf(l2Count))
        .divide(BigDecimal.valueOf(l1Total), 4, RoundingMode.HALF_UP);
  }

  /** weights_json → 权重表（kind: theory/skill） */
  public static Map<String, StandardWeightTable> restoreWeights(String weightsJson) {
    Map<String, StandardWeightTable> result = new LinkedHashMap<>();
    if (weightsJson == null || weightsJson.isBlank()) {
      return result;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(weightsJson);
      for (String kind : List.of("theory", "skill")) {
        StandardWeightTable table = new StandardWeightTable();
        table.setKind("theory".equals(kind)
            ? StandardWeightTable.Kind.THEORY : StandardWeightTable.Kind.SKILL);
        JsonNode rows = root.path(kind);
        if (rows.isArray()) {
          for (JsonNode r : rows) {
            StandardWeightRow row = new StandardWeightRow();
            row.setCategory(r.path("category").isNull() ? null : r.path("category").asText(null));
            row.setItem(r.path("item").asText(""));
            JsonNode values = r.path("values");
            values.fields().forEachRemaining(e -> {
              if (e.getValue().isNumber()) {
                row.getValues().put(e.getKey(), e.getValue().decimalValue());
              } else if (e.getValue().isTextual() && !e.getValue().asText().isBlank()) {
                row.getValues().put(e.getKey(), new BigDecimal(e.getValue().asText()));
              } else {
                row.getValues().put(e.getKey(), null);
              }
            });
            table.getRows().add(row);
          }
        }
        result.put(kind, table);
      }
    } catch (Exception e) {
      throw new IllegalStateException("weights_json 解析失败", e);
    }
    return result;
  }

  /** 编码末段（A-A-B-001 → 001；A-A → A） */
  public static String lastSegment(String nodeKey) {
    int idx = nodeKey.lastIndexOf('-');
    return idx < 0 ? nodeKey : nodeKey.substring(idx + 1);
  }
}
