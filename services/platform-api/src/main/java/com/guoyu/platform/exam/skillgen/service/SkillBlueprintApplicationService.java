package com.guoyu.platform.exam.skillgen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoDetailEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoDetailMapper;
import com.guoyu.platform.exam.mapper.ExamTaskExamshicaoMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult.NodeRow;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintTreeVO;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintVO;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import com.guoyu.platform.exam.skillgen.parser.SkillBlueprintXlsxParser;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
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

/**
 * 操作技能细目表批次：导入解析 + 校验 + 查询（方案 v1.1 §T2）。
 *
 * <p>与理论链路差异：两级考评范围 + 考评点（3 层）；有选考方式/技能要求/试题量列；
 * X:Y:Z 只统计展示不拦截（样例 18:5:0 = X 78%）；考评点数量无下限（指引未规定，
 * 试题总量下限在发起预检与入库闸门按"点数×5 + 累计已入库"校验）；
 * 一期不做科目匹配/创建（C2/S1：不写 exam_knowledge_structure / exam_course）。
 */
@Service
@Slf4j
public class SkillBlueprintApplicationService {

  /** 等级 → 显示名（index=level，level 数字=国标级别数：1=一级/高级技师 … 5=五级/初级工） */
  public static final String[] LEVEL_NAMES = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};

  /** 等级 → 试题总量下限（指引：五~三级 ≥50、二~一级 ≥30），发起预检与入库闸门用 */
  public static final Map<Integer, Integer> MIN_QUESTIONS = Map.of(5, 50, 4, 50, 3, 50, 2, 30, 1, 30);

  /** 一期固定每考评点题量（指引 3~5 取上限，决策 C3） */
  public static final int PER_POINT_QUOTA = 5;

  private final SkillBlueprintMapper blueprintMapper;
  private final SkillBlueprintNodeMapper nodeMapper;
  private final SkillGenBatchMapper batchMapper;
  private final SkillGenDraftMapper draftMapper;
  private final SkillBlueprintXlsxParser parser;
  private final ExamWorkTypeMapper workTypeMapper;
  private final ExamTaskExamshicaoMapper taskMapper;
  private final ExamTaskExamshicaoDetailMapper detailMapper;
  private final ObjectMapper objectMapper;
  private final FileApplicationService fileApplicationService;

  public SkillBlueprintApplicationService(SkillBlueprintMapper blueprintMapper,
      SkillBlueprintNodeMapper nodeMapper, SkillGenBatchMapper batchMapper,
      SkillGenDraftMapper draftMapper, SkillBlueprintXlsxParser parser,
      ExamWorkTypeMapper workTypeMapper, ExamTaskExamshicaoMapper taskMapper,
      ExamTaskExamshicaoDetailMapper detailMapper, ObjectMapper objectMapper,
      FileApplicationService fileApplicationService) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.batchMapper = batchMapper;
    this.draftMapper = draftMapper;
    this.parser = parser;
    this.workTypeMapper = workTypeMapper;
    this.taskMapper = taskMapper;
    this.detailMapper = detailMapper;
    this.objectMapper = objectMapper;
    this.fileApplicationService = fileApplicationService;
  }

  /** 导入操作技能细目表 xlsx（同工种+等级已有批次时按"重做"语义重建） */
  @Transactional
  public SkillBlueprintVO importBlueprint(MultipartFile file, Integer workTypeId, Integer level) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传细目表 xlsx 文件");
    }
    if (workTypeId == null || level == null || level < 1 || level > 5) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择工种与等级（1~5）");
    }
    String fileName = file.getOriginalFilename();
    if (fileName == null || !(fileName.toLowerCase().endsWith(".xlsx")
        || fileName.toLowerCase().endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 xlsx/xls 文件");
    }
    Long tenantId = TenantContextHolder.getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamWorkTypeEntity workType = workTypeMapper.selectById(Long.valueOf(workTypeId));
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工种不存在");
    }

    SkillBlueprintParseResult parsed;
    byte[] sourceBytes;
    try (InputStream in = file.getInputStream()) {
      // 一次性读入内存：解析与源文件留存共用同一份字节，避免二次取流
      sourceBytes = in.readAllBytes();
      parsed = parser.parse(new java.io.ByteArrayInputStream(sourceBytes), fileName);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件读取失败: " + e.getMessage());
    }

    List<NodeRow> rows = parsed.getRows();
    if (rows.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未解析到有效数据行，请检查文件格式");
    }

    // —— 校验（方案 §T2；校验错误不阻塞导入，随 check_report 返回由人工判断）——
    List<String> errors = new ArrayList<>(parsed.getWarnings());
    Map<String, Integer> impCount = new LinkedHashMap<>();
    int pointCount = 0;
    for (NodeRow n : rows) {
      if (n.getNodeLevel() == 3) {
        pointCount++;
        if (n.getImportance() == null) {
          errors.add("考评点[" + n.getName() + "]缺少重要程度");
        } else {
          impCount.merge(n.getImportance(), 1, Integer::sum);
        }
        if (n.getQuestionQuota() != null && n.getQuestionQuota() != PER_POINT_QUOTA) {
          errors.add("考评点[" + n.getName() + "]试题量列为 " + n.getQuestionQuota()
              + "，本期固定按每点 " + PER_POINT_QUOTA + " 题生成，将忽略该列值");
        }
      }
      if (n.getNodeLevel() <= 2) {
        if (n.getSelectMode() == null || n.getSelectMode().isBlank()) {
          errors.add("考评范围[" + n.getName() + "]缺少选考方式");
        } else if (!n.getSelectMode().contains("必考") && !n.getSelectMode().startsWith("任选")) {
          errors.add("考评范围[" + n.getName() + "]选考方式非法：" + n.getSelectMode());
        }
      }
    }
    boolean sawL1 = rows.stream().anyMatch(n -> n.getNodeLevel() == 1);
    if (!sawL1) {
      errors.add("未解析到一级考评范围");
    }
    // 一级权重合计 ≈100%（容差 ±0.5，与理论链路同口径）
    double l1WeightSum = rows.stream().filter(n -> n.getNodeLevel() == 1 && n.getWeight() != null)
        .mapToDouble(n -> n.getWeight().doubleValue()).sum();
    boolean weightOk = Math.abs(l1WeightSum - 100.0) < 0.51;
    if (!weightOk) {
      errors.add(String.format("一级权重合计 %.2f%% ≠ 100%%", l1WeightSum));
    }
    // 同级代码唯一（一/二级），考评点名称同级不重复
    Map<String, Integer> codeSeen = new HashMap<>();
    Map<String, Integer> pointNameSeen = new HashMap<>();
    int l1IdxCursor = -1;
    int l2IdxCursor = -1;
    for (NodeRow n : rows) {
      if (n.getNodeLevel() == 1) {
        l1IdxCursor++;
        l2IdxCursor = -1;
      }
      if (n.getNodeLevel() == 2) {
        l2IdxCursor++;
      }
      if (n.getNodeLevel() <= 2) {
        String uk = (n.getNodeLevel() == 1 ? "L1" : "L2-" + l1IdxCursor) + "|" + n.getCode();
        if (codeSeen.containsKey(uk)) {
          errors.add("节点代码重复: [" + n.getName() + "] 代码 " + n.getCode());
        }
        codeSeen.put(uk, 1);
      } else {
        String uk = l1IdxCursor + "-" + l2IdxCursor + "|" + n.getName();
        if (n.getName() != null && !n.getName().isBlank()) {
          if (pointNameSeen.containsKey(uk)) {
            errors.add("考评点名称同级重复: " + n.getName());
          }
          pointNameSeen.put(uk, 1);
        }
      }
    }
    int x = impCount.getOrDefault("X", 0);
    int y = impCount.getOrDefault("Y", 0);
    int z = impCount.getOrDefault("Z", 0);
    // 点数×5 vs 等级下限：不足时报告提示（发起生成时 400 拦截，S3）
    int floor = MIN_QUESTIONS.getOrDefault(level, 50);
    int planned = pointCount * PER_POINT_QUOTA;
    if (planned < floor) {
      errors.add(String.format("按每点 %d 题共 %d 道，低于本等级题库总量下限 %d 道（发起生成时将被拦截，请补充考评点）",
          PER_POINT_QUOTA, planned, floor));
    }

    // —— 重做语义：同工种+等级已有批次时，仅当无入库记录才允许重建 ——
    SkillBlueprintEntity old = blueprintMapper.selectOne(new LambdaQueryWrapper<SkillBlueprintEntity>()
        .eq(SkillBlueprintEntity::getTenantId, tenantId)
        .eq(SkillBlueprintEntity::getWorkTypeId, workTypeId)
        .eq(SkillBlueprintEntity::getLevel, level)
        .last("LIMIT 1"));
    if (old != null) {
      if (hasPublishedDrafts(tenantId, old.getId())) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "该工种等级已有试题入库记录，不允许重建细目表；如需调整请在原细目表基础上继续生成");
      }
      // 物理清理旧批次未入库草稿腾位（复盘"重做一等语义"），旧蓝图与节点软删
      int cleaned = draftMapper.physicalDeleteUnpublished(tenantId, old.getId());
      for (SkillBlueprintNodeEntity n : listNodes(tenantId, old.getId())) {
        nodeMapper.deleteById(n.getId());
      }
      blueprintMapper.deleteById(old.getId());
      log.info("细目表重建：清理旧蓝图 {} 未入库草稿 {} 条", old.getId(), cleaned);
    }

    // —— 落库（源文件留存放在解析成功之后；同事务，失败回滚，无孤儿文件）——
    FileVO storedSource = fileApplicationService.storeFromBytes(
        sourceBytes, parsed.getFileName(), file.getContentType(), "private");
    SkillBlueprintEntity bp = new SkillBlueprintEntity();
    bp.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    bp.setTenantId(tenantId);
    bp.setWorkTypeId(workTypeId);
    bp.setLevel(level);
    bp.setTitle(workType.getTitle() + "（" + level + "级/" + LEVEL_NAMES[level] + "）操作技能考评要素细目表");
    bp.setSourceFileName(parsed.getFileName());
    bp.setSourceFileId(storedSource.id());
    bp.setPointCount(pointCount);
    bp.setXyzRatio(x + ":" + y + ":" + z);
    bp.setWeightCheck(weightOk ? 1 : 0);
    try {
      Map<String, Object> report = new LinkedHashMap<>();
      report.put("errors", errors);
      report.put("pointCount", pointCount);
      report.put("xyzRatio", x + ":" + y + ":" + z);
      report.put("l1WeightSum", l1WeightSum);
      report.put("perPointQuota", PER_POINT_QUOTA);
      report.put("plannedQuestions", planned);
      report.put("minQuestions", floor);
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

    // 节点落库：parentIndex → 父行 id；full_code 沿父链拼代码（A-A-001）
    Map<Integer, Long> idByRowIndex = new HashMap<>();
    List<String> fullCodes = new ArrayList<>(rows.size());
    for (int i = 0; i < rows.size(); i++) {
      NodeRow n = rows.get(i);
      SkillBlueprintNodeEntity node = new SkillBlueprintNodeEntity();
      node.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
      node.setTenantId(tenantId);
      node.setBlueprintId(bp.getId());
      Long parentId = n.getParentIndex() >= 0 ? idByRowIndex.get(n.getParentIndex()) : null;
      node.setParentId(parentId == null ? 0L : parentId);
      node.setNodeLevel(n.getNodeLevel());
      node.setCode(n.getCode());
      String parentFull = n.getParentIndex() >= 0 && n.getParentIndex() < fullCodes.size()
          ? fullCodes.get(n.getParentIndex()) : "";
      String full = parentFull == null || parentFull.isEmpty()
          ? n.getCode() : parentFull + "-" + n.getCode();
      fullCodes.add(full);
      node.setFullCode(full);
      node.setName(n.getName());
      node.setWeight(n.getWeight());
      node.setSelectMode(n.getSelectMode());
      node.setSkillRequirement(n.getSkillRequirement());
      node.setImportance(n.getImportance());
      node.setQuestionQuota(n.getQuestionQuota());
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

    log.info("操作技能细目表导入完成：blueprintId={}, points={}, x:y:z={}，校验提示 {} 条，预计产出 {} 道",
        bp.getId(), pointCount, bp.getXyzRatio(), errors.size(), planned);
    return SkillBlueprintVO.from(bp, workType.getTitle());
  }

  /** 旧蓝图关联批次是否存在已入库草稿（重做闸门） */
  private boolean hasPublishedDrafts(Long tenantId, Long blueprintId) {
    Long published = draftMapper.selectCount(new LambdaQueryWrapper<SkillGenDraftEntity>()
        .eq(SkillGenDraftEntity::getTenantId, tenantId)
        .eq(SkillGenDraftEntity::getBlueprintId, blueprintId)
        .eq(SkillGenDraftEntity::getPublishStatus, 1));
    return published != null && published > 0;
  }

  @Transactional(readOnly = true)
  public SkillBlueprintEntity findBlueprint(Long tenantId, Long blueprintId) {
    SkillBlueprintEntity b = blueprintMapper.selectById(blueprintId);
    return b == null || !b.getTenantId().equals(tenantId) ? null : b;
  }

  /** 全量节点一次载入（导出按父链渲染用，避免 N+1） */
  @Transactional(readOnly = true)
  public List<SkillBlueprintNodeEntity> findBlueprintNodes(Long tenantId, Long blueprintId) {
    List<SkillBlueprintNodeEntity> out = new ArrayList<>();
    for (SkillBlueprintNodeEntity n : listNodes(tenantId, blueprintId)) {
      if (n.getTenantId() != null && n.getTenantId().equals(tenantId)) {
        out.add(n);
      }
    }
    return out;
  }

  private List<SkillBlueprintNodeEntity> listNodes(Long tenantId, Long blueprintId) {
    List<SkillBlueprintNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<SkillBlueprintNodeEntity>()
            .eq(SkillBlueprintNodeEntity::getBlueprintId, blueprintId)
            .orderByAsc(SkillBlueprintNodeEntity::getId));
    return nodes == null ? new ArrayList<>() : nodes;
  }

  /** 批次分页 */
  @Transactional(readOnly = true)
  public PageResponse<SkillBlueprintVO> page(Long tenantId, Integer workTypeId, Integer level,
      long page, long pageSize) {
    LambdaQueryWrapper<SkillBlueprintEntity> w = new LambdaQueryWrapper<SkillBlueprintEntity>()
        .eq(SkillBlueprintEntity::getTenantId, tenantId)
        .eq(workTypeId != null, SkillBlueprintEntity::getWorkTypeId, workTypeId)
        .eq(level != null, SkillBlueprintEntity::getLevel, level)
        .orderByDesc(SkillBlueprintEntity::getId);
    Page<SkillBlueprintEntity> p = blueprintMapper.selectPage(new Page<>(page, pageSize), w);
    List<SkillBlueprintVO> items = p.getRecords().stream()
        .map(b -> SkillBlueprintVO.from(b, null)).toList();
    return new PageResponse<>(items, p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 单批次 + 全量节点树 */
  @Transactional(readOnly = true)
  public SkillBlueprintTreeVO tree(Long tenantId, Long blueprintId) {
    SkillBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    List<SkillBlueprintNodeEntity> nodes = nodeMapper.selectList(
        new LambdaQueryWrapper<SkillBlueprintNodeEntity>()
            .eq(SkillBlueprintNodeEntity::getTenantId, tenantId)
            .eq(SkillBlueprintNodeEntity::getBlueprintId, blueprintId)
            .orderByAsc(SkillBlueprintNodeEntity::getSortOrder));
    return SkillBlueprintTreeVO.from(bp, nodes == null ? List.of() : nodes);
  }

  /** 删除批次（软删；已有生成批次的不可删） */
  @Transactional
  public void delete(Long tenantId, Long blueprintId) {
    SkillBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    Long batchCount = batchMapper.selectCount(new LambdaQueryWrapper<SkillGenBatchEntity>()
        .eq(SkillGenBatchEntity::getTenantId, tenantId)
        .eq(SkillGenBatchEntity::getBlueprintId, blueprintId));
    if (batchCount != null && batchCount > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该细目表已有生成批次，不可删除");
    }
    blueprintMapper.deleteById(blueprintId);
    for (SkillBlueprintNodeEntity n : listNodes(tenantId, blueprintId)) {
      nodeMapper.deleteById(n.getId());
    }
  }

  /** 发起生成预检：点数×5 + 累计已入库 ≥ 等级下限（与入库闸门共用同一统计方法，S9/S10） */
  @Transactional(readOnly = true)
  public CumulativeStat cumulativeStat(Long tenantId, Integer workTypeId, Integer level) {
    List<ExamTaskExamshicaoEntity> tasks = taskMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
            .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId.intValue())
            .eq(ExamTaskExamshicaoEntity::getWorkTypeId, workTypeId)
            .eq(ExamTaskExamshicaoEntity::getLevel, level));
    if (tasks == null || tasks.isEmpty()) {
      return new CumulativeStat(0, 0);
    }
    List<Integer> taskIds = tasks.stream().map(ExamTaskExamshicaoEntity::getId).toList();
    Long total = detailMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
        .in(ExamTaskExamshicaoDetailEntity::getTaskId, taskIds));
    Long ai = detailMapper.selectCount(new LambdaQueryWrapper<ExamTaskExamshicaoDetailEntity>()
        .in(ExamTaskExamshicaoDetailEntity::getTaskId, taskIds)
        .eq(ExamTaskExamshicaoDetailEntity::getSource, 2));
    return new CumulativeStat(total == null ? 0 : total, ai == null ? 0 : ai);
  }

  /** 累计口径统计：total=全部来源有效明细数（S10）；aiTotal=其中 source=2 的 AI 生成数（展示用） */
  public record CumulativeStat(long total, long aiTotal) {
  }
}
