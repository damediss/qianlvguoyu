package com.guoyu.platform.exam.theorygen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult.NodeRow;
import com.guoyu.platform.exam.theorygen.dto.BlueprintTreeVO;
import com.guoyu.platform.exam.theorygen.dto.BlueprintVO;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.parser.TheoryBlueprintXlsxParser;
import com.guoyu.platform.security.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 理论细目表批次：导入解析 + 校验 + 查询 */
@Service
@Slf4j
public class TheoryBlueprintApplicationService {

  /** 等级 → 考评点数下限（指引：五~三级≥200，二~一级≥120） */
  private static final Map<Integer, Integer> MIN_POINTS = Map.of(5, 200, 4, 200, 3, 200, 2, 120, 1, 120);
  /** 等级 → 显示名。与 ExamTaskExam* 的 LEVELS 完全一致（index=level，level 数字=级别数）：5=五级/初级工 … 1=一级/高级技师 */
  public static final String[] LEVEL_NAMES = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};

  private final TheoryBlueprintMapper blueprintMapper;
  private final TheoryBlueprintNodeMapper nodeMapper;
  private final TheoryBlueprintXlsxParser parser;
  private final ExamWorkTypeMapper workTypeMapper;
  private final ExamCourseMapper courseMapper;
  private final ObjectMapper objectMapper;

  public TheoryBlueprintApplicationService(TheoryBlueprintMapper blueprintMapper,
      TheoryBlueprintNodeMapper nodeMapper, TheoryBlueprintXlsxParser parser,
      ExamWorkTypeMapper workTypeMapper, ExamCourseMapper courseMapper,
      ObjectMapper objectMapper) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.parser = parser;
    this.workTypeMapper = workTypeMapper;
    this.courseMapper = courseMapper;
    this.objectMapper = objectMapper;
  }

  /** 导入细目表 xlsx */
  @Transactional
  public BlueprintVO importBlueprint(MultipartFile file, Integer workTypeId, Integer level) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传细目表 xlsx 文件");
    }
    if (workTypeId == null || level == null || level < 1 || level > 5) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择工种与等级（1~5）");
    }
    String fileName = file.getOriginalFilename();
    if (fileName == null || !(fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 xlsx/xls 文件");
    }
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamWorkTypeEntity workType = workTypeMapper.selectById(Long.valueOf(workTypeId));
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工种不存在");
    }

    BlueprintParseResult parsed;
    try (InputStream in = file.getInputStream()) {
      parsed = parser.parse(in, fileName);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件读取失败: " + e.getMessage());
    }

    List<NodeRow> rows = parsed.getRows();
    if (rows.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未解析到有效数据行，请检查文件格式");
    }

    // —— 校验（方案 §T2）——
    List<String> errors = new ArrayList<>(parsed.getWarnings());
    Map<String, Integer> impCount = new LinkedHashMap<>();
    int pointCount = 0;
    double weightSum = 0;
    boolean sawL1 = false;
    for (NodeRow n : rows) {
      if (n.getNodeLevel() == 4) {
        pointCount++;
        String imp = n.getImportance();
        if (imp == null) {
          errors.add("考评点[" + n.getName() + "]缺少重要程度");
        } else {
          impCount.merge(imp, 1, Integer::sum);
        }
      }
      if (n.getNodeLevel() == 1) {
        sawL1 = true;
        if (n.getCode().isBlank()) {
          errors.add("一级节点[" + n.getName() + "]缺少代码");
        }
      }
      if (n.getNodeLevel() < 4 && n.getWeight() != null) {
        weightSum += n.getWeight().doubleValue();
      }
    }
    if (!sawL1) {
      errors.add("未解析到一级考评范围");
    }
    // 权重合计按一级节点求和（各层级重复累计不做）
    double l1WeightSum = rows.stream().filter(n -> n.getNodeLevel() == 1 && n.getWeight() != null)
        .mapToDouble(n -> n.getWeight().doubleValue()).sum();
    boolean weightOk = Math.abs(l1WeightSum - 100.0) < 0.51;
    if (!weightOk) {
      errors.add(String.format("一级权重合计 %.2f%% ≠ 100%%", l1WeightSum));
    }
    int minPoints = MIN_POINTS.getOrDefault(level, 200);
    if (pointCount < minPoints) {
      errors.add("考评点数量 " + pointCount + " 低于该等级下限 " + minPoints);
    }
    int x = impCount.getOrDefault("X", 0);
    int y = impCount.getOrDefault("Y", 0);
    int z = impCount.getOrDefault("Z", 0);
    if (pointCount > 0) {
      double xPct = x * 100.0 / pointCount;
      double yPct = y * 100.0 / pointCount;
      double zPct = z * 100.0 / pointCount;
      if (xPct < 85) {
        errors.add(String.format("重要程度 X 占比 %.1f%% 低于 85%%", xPct));
      }
      if (yPct > 10) {
        errors.add(String.format("重要程度 Y 占比 %.1f%% 超过 10%%", yPct));
      }
      if (zPct > 5) {
        errors.add(String.format("重要程度 Z 占比 %.1f%% 超过 5%%", zPct));
      }
    }
    // 同级代码唯一性
    Map<String, Integer> codeSeen = new HashMap<>();
    for (NodeRow n : rows) {
      if (n.getNodeLevel() == 4) {
        continue;
      }
      String uk = n.getParentKey() + "|" + n.getCode();
      if (codeSeen.containsKey(uk)) {
        errors.add("节点代码重复: [" + n.getName() + "] 代码 " + n.getCode());
      }
      codeSeen.put(uk, 1);
    }

    // —— 落库（校验错误不阻塞导入，随 check_report 返回由人工判断；但空结构阻塞）——
    TheoryBlueprintEntity bp = new TheoryBlueprintEntity();
    bp.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    bp.setTenantId(tenantId);
    bp.setWorkTypeId(workTypeId);
    bp.setLevel(level);
    bp.setTitle(workType.getTitle() + "（" + level + "级/" + LEVEL_NAMES[level] + "）理论知识考评要素细目表");
    bp.setSourceFileName(parsed.getFileName());
    bp.setPointCount(pointCount);
    bp.setXyzRatio(x + ":" + y + ":" + z);
    bp.setWeightCheck(weightOk ? 1 : 0);
    try {
      Map<String, Object> report = new LinkedHashMap<>();
      report.put("errors", errors);
      report.put("pointCount", pointCount);
      report.put("xyzRatio", x + ":" + y + ":" + z);
      report.put("l1WeightSum", l1WeightSum);
      report.put("weightSumAllLevels", weightSum);
      bp.setCheckReport(objectMapper.writeValueAsString(report));
    } catch (Exception e) {
      bp.setCheckReport("{\"errors\":[\"报告序列化失败\"]}");
    }
    bp.setStatus("imported");
    bp.setCreatedAt(LocalDateTime.now());
    bp.setUpdatedAt(LocalDateTime.now());
    bp.setCreatedBy(userId);
    bp.setUpdatedBy(userId);
    bp.setDeleted(0);
    blueprintMapper.insert(bp);

    // 节点落库：parentIndex → 父行 id；full_code 沿父链拼代码
    Map<Integer, Long> idByRowIndex = new HashMap<>();
    List<String> fullCodes = new ArrayList<>(rows.size());
    for (int i = 0; i < rows.size(); i++) {
      NodeRow n = rows.get(i);
      TheoryBlueprintNodeEntity node = new TheoryBlueprintNodeEntity();
      node.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
      node.setTenantId(tenantId);
      node.setBlueprintId(bp.getId());
      Long parentId = n.getParentIndex() >= 0 ? idByRowIndex.get(n.getParentIndex()) : null;
      node.setParentId(parentId == null ? 0L : parentId);
      node.setNodeLevel(n.getNodeLevel());
      node.setCode(n.getCode());
      // full_code：沿父链逐级拼本级代码（指引口径：考评点代码=A-A-A-001，各级代码用-连接）
      String parentFull = "";
      if (n.getParentIndex() >= 0 && n.getParentIndex() < fullCodes.size()
          && fullCodes.get(n.getParentIndex()) != null) {
        parentFull = fullCodes.get(n.getParentIndex());
      }
      String full = parentFull.isEmpty() ? n.getCode() : parentFull + "-" + n.getCode();
      fullCodes.add(full);
      node.setFullCode(full);
      node.setName(n.getName());
      node.setWeight(n.getWeight());
      node.setImportance(n.getImportance());
      node.setRelatedKnowledge(n.getRelatedKnowledge());
      node.setXyzStat(n.getXyzStat());
      node.setSortOrder(n.getSortOrder());
      node.setCreatedAt(LocalDateTime.now());
      node.setUpdatedAt(LocalDateTime.now());
      node.setCreatedBy(userId);
      node.setUpdatedBy(userId);
      node.setDeleted(0);
      nodeMapper.insert(node);
      idByRowIndex.put(i, node.getId());
    }

    // —— 科目挂接（方案 v1.1 T2：导入时匹配，不阻塞）——
    Integer courseId = matchCourse(tenantId, workTypeId, level);
    if (courseId != null) {
      bp.setCourseId(courseId);
      blueprintMapper.updateById(bp);
    }

    log.info("理论细目表导入完成：blueprintId={}, points={}, x:y:z={}，校验错误 {} 条",
        bp.getId(), pointCount, bp.getXyzRatio(), errors.size());
    return BlueprintVO.from(bp, workType.getTitle());
  }

  /** 按工种+等级匹配既有科目（course.level 0~4 = 任务链路 level-1） */
  private Integer matchCourse(Long tenantId, Integer workTypeId, Integer level) {
    // exam_course.level 0~4（0=初级…4=高级技师）↔ 任务链路 level 5~1（5=五级/初级工），换算：course = 5 - level
    int courseLevel = 5 - level;
    LambdaQueryWrapper<ExamCourseEntity> w = new LambdaQueryWrapper<ExamCourseEntity>()
        .eq(ExamCourseEntity::getTenantId, tenantId)
        .eq(ExamCourseEntity::getWorkTypeId, workTypeId)
        .eq(ExamCourseEntity::getLevel, courseLevel)
        .eq(ExamCourseEntity::getDeleted, 0)
        .last("LIMIT 1");
    ExamCourseEntity c = courseMapper.selectOne(w);
    return c == null ? null : c.getId();
  }

  /** 查单节点（导出服务用） */
  @Transactional(readOnly = true)
  public TheoryBlueprintNodeEntity findNode(Long tenantId, Long nodeId) {
    TheoryBlueprintNodeEntity n = nodeMapper.selectById(nodeId);
    return n == null || !n.getTenantId().equals(tenantId) ? null : n;
  }

  /** 全量节点一次载入（导出按父链拼考评点代码等行级渲染用），避免逐行 selectById 的 N+1；
   * 租户过滤由多租户插件自动拼接，此处再按 tenantId 防御性过滤越权行 */
  @Transactional(readOnly = true)
  public List<TheoryBlueprintNodeEntity> findBlueprintNodes(Long tenantId, Long blueprintId) {
    List<TheoryBlueprintNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
            .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId)
            .orderByAsc(TheoryBlueprintNodeEntity::getId));
    List<TheoryBlueprintNodeEntity> out = new ArrayList<>();
    for (TheoryBlueprintNodeEntity n : nodes) {
      if (n.getTenantId() != null && n.getTenantId().equals(tenantId)) {
        out.add(n);
      }
    }
    return out;
  }

  /** 批次分页 */
  @Transactional(readOnly = true)
  public PageResponse<BlueprintVO> page(Long tenantId, Integer workTypeId, Integer level,
      long page, long pageSize) {
    LambdaQueryWrapper<TheoryBlueprintEntity> w = new LambdaQueryWrapper<TheoryBlueprintEntity>()
        .eq(TheoryBlueprintEntity::getTenantId, tenantId)
        .eq(workTypeId != null, TheoryBlueprintEntity::getWorkTypeId, workTypeId)
        .eq(level != null, TheoryBlueprintEntity::getLevel, level)
        .orderByDesc(TheoryBlueprintEntity::getId);
    Page<TheoryBlueprintEntity> p = blueprintMapper.selectPage(new Page<>(page, pageSize), w);
    List<BlueprintVO> items = p.getRecords().stream().map(b -> BlueprintVO.from(b, null)).toList();
    return new PageResponse<>(items, p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 单批次 + 全量节点树 */
  @Transactional(readOnly = true)
  public BlueprintTreeVO tree(Long tenantId, Long blueprintId) {
    TheoryBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    List<TheoryBlueprintNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
            .eq(TheoryBlueprintNodeEntity::getTenantId, tenantId)
            .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId)
            .orderByAsc(TheoryBlueprintNodeEntity::getSortOrder));
    return BlueprintTreeVO.from(bp, nodes);
  }

  /** 删除批次（软删；已有生成批次的不可删） */
  @Transactional
  public void delete(Long tenantId, Long blueprintId) {
    TheoryBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    blueprintMapper.deleteById(blueprintId);
    // 节点级联软删
    List<TheoryBlueprintNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
            .eq(TheoryBlueprintNodeEntity::getTenantId, tenantId)
            .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId));
    if (nodes != null) {
      for (TheoryBlueprintNodeEntity n : nodes) {
        nodeMapper.deleteById(n.getId());
      }
    }
  }
}
