package com.guoyu.platform.exam.standard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import com.guoyu.platform.exam.standard.vo.StandardNodeVO;
import com.guoyu.platform.exam.standard.vo.StandardTaskVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 细目表管理：任务上传解析、树查询、节点编辑、审核确认（方案 §3/§11）。
 * LLM 概括见 {@link StandardLlmSummarizeService}，导出见 export 包。
 */
@Service
public class StandardParseApplicationService {

  private static final Logger log = LoggerFactory.getLogger(StandardParseApplicationService.class);

  public static final String STATUS_UPLOADED = "uploaded";
  public static final String STATUS_PARSED = "parsed";
  public static final String STATUS_LLM_RUNNING = "llm_running";
  public static final String STATUS_REVIEWING = "reviewing";
  public static final String STATUS_CONFIRMED = "confirmed";
  public static final String STATUS_EXPORTED = "exported";

  private final ExamStandardTaskMapper taskMapper;
  private final ExamStandardNodeMapper nodeMapper;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;

  public StandardParseApplicationService(ExamStandardTaskMapper taskMapper,
      ExamStandardNodeMapper nodeMapper,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.nodeMapper = nodeMapper;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  // ============================================================
  // 上传解析（同步，骨架秒回）
  // ============================================================

  @Transactional
  public StandardTaskVO uploadAndParse(byte[] content, String originalName, Long tenantId,
      Long userId) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }
    String fileHash = sha256(content);
    ExamStandardTaskEntity dup = taskMapper.selectOne(new LambdaQueryWrapper<ExamStandardTaskEntity>()
        .eq(ExamStandardTaskEntity::getTenantId, tenantId)
        .eq(ExamStandardTaskEntity::getFileHash, fileHash)
        .last("LIMIT 1"));
    if (dup != null) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "同一文件已存在解析任务（任务ID=" + dup.getId() + "，职业=" + dup.getWorkTypeTitle() + "），"
              + "请直接查看或先删除原任务");
    }

    ParsedStandard parsed;
    try {
      parsed = new DocxStandardParser().parse(new ByteArrayInputStream(content));
    } catch (IOException e) {
      log.warn("[细目表] docx 解析失败: {}", e.getMessage());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "docx 解析失败：" + e.getMessage());
    }
    boolean hasError = parsed.getParseWarnings().stream().anyMatch(w -> w.startsWith("ERROR"));
    if (hasError || parsed.getLevels().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "文档不符合国家职业技能标准格式：" + String.join("；", parsed.getParseWarnings()));
    }

    FileVO file = fileApplicationService.storeFromBytes(content, originalName,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "private");

    ExamStandardTaskEntity task = new ExamStandardTaskEntity();
    task.setTenantId(tenantId);
    task.setWorkTypeTitle(parsed.getWorkTypeTitle() != null ? parsed.getWorkTypeTitle() : "");
    task.setSeriesNo(parsed.getSeriesNo());
    task.setVersion(parsed.getVersion());
    task.setFileId(file.id());
    task.setFileHash(fileHash);
    task.setStatus(STATUS_PARSED);
    task.setDirectionsJson(writeJson(parsed.getDirections()));
    task.setLevelsJson(writeJson(parsed.getLevels().stream()
        .map(l -> Map.of("levelCode", l.getLevelCode(), "levelName", l.getLevelName()))
        .toList()));
    task.setWeightsJson(writeJson(weightsToJson(parsed)));
    task.setParseWarnings(String.join("\n", parsed.getParseWarnings()));
    taskMapper.insert(task);

    persistTree(task, parsed);
    log.info("[细目表] 上传解析完成：taskId={} 职业={} 等级数={} 方向={}",
        task.getId(), task.getWorkTypeTitle(), parsed.getLevels().size(), parsed.getDirections());
    return toTaskVO(task);
  }

  /** ParsedStandard 骨架 → staging 节点（每 等级×方向 一棵树，id 预分配） */
  private void persistTree(ExamStandardTaskEntity task, ParsedStandard parsed) {
    List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(parsed);
    Map<String, Long> idByKey = new HashMap<>();
    for (StandardTreeDraft draft : drafts) {
      String mapKey = treeMapKey(draft.levelCode(), draft.direction(), draft.nodeKey());
      idByKey.put(mapKey, IdWorker.getId());
    }
    for (StandardTreeDraft draft : drafts) {
      ExamStandardNodeEntity node = new ExamStandardNodeEntity();
      node.setId(idByKey.get(treeMapKey(draft.levelCode(), draft.direction(), draft.nodeKey())));
      node.setTenantId(task.getTenantId());
      node.setTaskId(task.getId());
      node.setParentId(draft.parentNodeKey() == null ? 0L
          : idByKey.get(treeMapKey(draft.levelCode(), draft.direction(), draft.parentNodeKey())));
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
      node.setReviewStatus("approved");
      node.setDirection(draft.direction());
      node.setLevelCode(draft.levelCode());
      node.setNeedLlm(draft.needLlm() ? 1 : 0);
      nodeMapper.insert(node);
    }
  }

  public static String treeMapKey(int levelCode, String direction, String nodeKey) {
    return levelCode + "|" + direction + "|" + nodeKey;
  }

  // ============================================================
  // 任务列表 / 详情
  // ============================================================

  @Transactional(readOnly = true)
  public PageResponse<StandardTaskVO> page(long page, long pageSize, String keyword,
      Long tenantId) {
    LambdaQueryWrapper<ExamStandardTaskEntity> wrapper = new LambdaQueryWrapper<ExamStandardTaskEntity>()
        .eq(ExamStandardTaskEntity::getTenantId, tenantId)
        .like(keyword != null && !keyword.isBlank(), ExamStandardTaskEntity::getWorkTypeTitle,
            keyword)
        .orderByDesc(ExamStandardTaskEntity::getId);
    Page<ExamStandardTaskEntity> result = taskMapper.selectPage(new Page<>(page, pageSize), wrapper);
    List<StandardTaskVO> items = new ArrayList<>();
    for (ExamStandardTaskEntity task : result.getRecords()) {
      items.add(toTaskVO(task));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public StandardTaskVO getTask(Long taskId, Long tenantId) {
    return toTaskVO(findTask(taskId, tenantId));
  }

  public ExamStandardTaskEntity findTask(Long taskId, Long tenantId) {
    ExamStandardTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<ExamStandardTaskEntity>()
        .eq(ExamStandardTaskEntity::getTenantId, tenantId)
        .eq(ExamStandardTaskEntity::getId, taskId));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "解析任务不存在");
    }
    return task;
  }

  // ============================================================
  // 树查询（扁平列表，前端组装树）
  // ============================================================

  @Transactional(readOnly = true)
  public List<StandardNodeVO> tree(Long taskId, Integer levelCode, String direction,
      String dimension, Long tenantId) {
    findTask(taskId, tenantId);
    LambdaQueryWrapper<ExamStandardNodeEntity> wrapper = new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, taskId)
        .eq(levelCode != null, ExamStandardNodeEntity::getLevelCode, levelCode)
        .eq(direction != null && !direction.isBlank(), ExamStandardNodeEntity::getDirection,
            direction)
        .orderByAsc(ExamStandardNodeEntity::getLevelCode)
        .orderByAsc(ExamStandardNodeEntity::getDirection)
        .orderByAsc(ExamStandardNodeEntity::getNodeKey)
        .orderByAsc(ExamStandardNodeEntity::getSortNumber);
    List<ExamStandardNodeEntity> nodes = nodeMapper.selectList(wrapper);
    List<StandardNodeVO> result = new ArrayList<>();
    for (ExamStandardNodeEntity node : nodes) {
      if (dimension != null && !dimension.isBlank() && node.getNodeLevel() == 3
          && !dimension.equals(node.getDimension())) {
        continue; // dimension 过滤仅作用于叶节点
      }
      result.add(StandardNodeVO.from(node));
    }
    return result;
  }

  // ============================================================
  // 节点编辑 / 增删 / 排序
  // ============================================================

  @Transactional
  public StandardNodeVO updateNode(Long nodeId, String title, String important,
      java.math.BigDecimal weight, Integer sortNumber, Long tenantId) {
    ExamStandardNodeEntity node = findNode(nodeId, tenantId);
    if (title != null && !title.isBlank()) {
      node.setTitle(title.trim());
    }
    if (important != null) {
      if (node.getNodeLevel() != 3) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "重要程度仅叶节点（L3）可设置");
      }
      String v = important.trim().toUpperCase();
      if (!v.isEmpty() && !List.of("X", "Y", "Z").contains(v)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "重要程度仅支持 X/Y/Z");
      }
      node.setImportant(v.isEmpty() ? null : v);
    }
    if (weight != null) {
      if (node.getNodeLevel() == 3) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "叶节点无权重，请调整其上级小节权重");
      }
      node.setWeight(weight);
    }
    if (sortNumber != null) {
      node.setSortNumber(sortNumber);
    }
    if ("approved".equals(node.getReviewStatus())) {
      node.setReviewStatus("modified");
    }
    nodeMapper.updateById(node);
    return StandardNodeVO.from(node);
  }

  @Transactional
  public StandardNodeVO createNode(Long taskId, Long parentId, String title, String important,
      String dimension, Long tenantId) {
    findTask(taskId, tenantId);
    if (title == null || title.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "标题不能为空");
    }
    ExamStandardNodeEntity parent = findNode(parentId, tenantId);
    if (parent.getNodeLevel() != 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能在二级小节（L2）下新增考评点");
    }
    String dim = "skill".equals(dimension) ? "skill" : "theory";
    if (important != null && !important.isBlank()
        && !List.of("X", "Y", "Z").contains(important.trim().toUpperCase())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "重要程度仅支持 X/Y/Z");
    }
    Integer count = nodeMapper.selectCount(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getParentId, parentId)
        .eq(ExamStandardNodeEntity::getDimension, dim)).intValue();
    ExamStandardNodeEntity node = new ExamStandardNodeEntity();
    node.setTenantId(tenantId);
    node.setTaskId(taskId);
    node.setParentId(parentId);
    node.setNodeKey(NodeKeyGenerator.leafKey(parent.getNodeKey(), count));
    node.setNodeLevel(3);
    node.setSortNumber(count + 1);
    node.setDimension(dim);
    node.setTitle(title.trim());
    node.setImportant(important == null || important.isBlank() ? "X" : important.trim().toUpperCase());
    node.setSource("manual");
    node.setReviewStatus("approved");
    node.setDirection(parent.getDirection());
    node.setLevelCode(parent.getLevelCode());
    node.setNeedLlm(0);
    nodeMapper.insert(node);
    return StandardNodeVO.from(node);
  }

  @Transactional
  public void deleteNode(Long nodeId, Long tenantId) {
    ExamStandardNodeEntity node = findNode(nodeId, tenantId);
    List<Long> ids = new ArrayList<>();
    collectDescendants(nodeId, ids);
    ids.add(nodeId);
    nodeMapper.deleteBatchIds(ids);
    log.info("[细目表] 删除节点（含子树 {} 个节点）：nodeId={}", ids.size(), nodeId);
  }

  private void collectDescendants(Long parentId, List<Long> ids) {
    List<ExamStandardNodeEntity> children = nodeMapper.selectList(
        new LambdaQueryWrapper<ExamStandardNodeEntity>()
            .eq(ExamStandardNodeEntity::getParentId, parentId));
    for (ExamStandardNodeEntity child : children) {
      ids.add(child.getId());
      collectDescendants(child.getId(), ids);
    }
  }

  @Transactional
  public void moveNode(Long nodeId, boolean up, Long tenantId) {
    ExamStandardNodeEntity node = findNode(nodeId, tenantId);
    LambdaQueryWrapper<ExamStandardNodeEntity> siblingQuery = new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getParentId, node.getParentId())
        .eq(ExamStandardNodeEntity::getTaskId, node.getTaskId())
        .eq(ExamStandardNodeEntity::getDimension, node.getDimension())
        .orderByAsc(ExamStandardNodeEntity::getSortNumber)
        .orderByAsc(ExamStandardNodeEntity::getId);
    List<ExamStandardNodeEntity> siblings = nodeMapper.selectList(siblingQuery);
    int idx = -1;
    for (int i = 0; i < siblings.size(); i++) {
      if (siblings.get(i).getId().equals(nodeId)) {
        idx = i;
        break;
      }
    }
    int target = up ? idx - 1 : idx + 1;
    if (idx < 0 || target < 0 || target >= siblings.size()) {
      return; // 已在顶端/底端
    }
    ExamStandardNodeEntity other = siblings.get(target);
    Integer a = node.getSortNumber();
    node.setSortNumber(other.getSortNumber());
    other.setSortNumber(a);
    nodeMapper.updateById(node);
    nodeMapper.updateById(other);
  }

  // ============================================================
  // 审核确认 / 删除任务
  // ============================================================

  @Transactional
  public StandardTaskVO reviewDone(Long taskId, Long tenantId) {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    if (STATUS_LLM_RUNNING.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "LLM 概括进行中，请完成后再确认");
    }
    Long pending = nodeMapper.selectCount(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, taskId)
        .eq(ExamStandardNodeEntity::getNodeLevel, 3)
        .eq(ExamStandardNodeEntity::getReviewStatus, "pending"));
    if (pending == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有待审核的叶节点（或尚未执行 LLM 概括）");
    }
    ExamStandardNodeEntity update = new ExamStandardNodeEntity();
    update.setReviewStatus("approved");
    nodeMapper.update(update, new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, taskId)
        .eq(ExamStandardNodeEntity::getReviewStatus, "pending"));
    task.setStatus(STATUS_CONFIRMED);
    taskMapper.updateById(task);
    return toTaskVO(task);
  }

  @Transactional
  public void deleteTask(Long taskId, Long tenantId) {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    if (STATUS_LLM_RUNNING.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "LLM 概括进行中，禁止删除");
    }
    nodeMapper.delete(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, taskId));
    taskMapper.deleteById(task.getId());
  }

  // ============================================================
  // VO 组装与权重 JSON
  // ============================================================

  private StandardTaskVO toTaskVO(ExamStandardTaskEntity task) {
    Long total = nodeMapper.selectCount(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, task.getId())
        .eq(ExamStandardNodeEntity::getNodeLevel, 3));
    Long pending = nodeMapper.selectCount(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTaskId, task.getId())
        .eq(ExamStandardNodeEntity::getNodeLevel, 3)
        .eq(ExamStandardNodeEntity::getReviewStatus, "pending"));
    return new StandardTaskVO(
        task.getId() == null ? null : String.valueOf(task.getId()),
        task.getWorkTypeTitle(), task.getSeriesNo(), task.getVersion(),
        task.getFileId() == null ? null : String.valueOf(task.getFileId()),
        task.getStatus(),
        readStringList(task.getDirectionsJson()),
        readLevels(task.getLevelsJson()),
        total == null ? 0 : total.intValue(),
        pending == null ? 0 : pending.intValue(),
        task.getParseWarnings(),
        task.getLlmModelId(),
        task.getLlmFinishedAt(),
        task.getCreatedAt());
  }

  private Map<String, Object> weightsToJson(ParsedStandard parsed) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("theory", weightTableToJson(parsed.getTheoryWeights()));
    map.put("skill", weightTableToJson(parsed.getSkillWeights()));
    return map;
  }

  private List<Map<String, Object>> weightTableToJson(StandardWeightTable table) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (table == null) {
      return rows;
    }
    for (StandardWeightRow row : table.getRows()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("category", row.getCategory());
      m.put("item", row.getItem());
      m.put("values", row.getValues());
      rows.add(m);
    }
    return rows;
  }

  private String writeJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JSON 序列化失败", e);
    }
  }

  private List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json,
          objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (Exception e) {
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> readLevels(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, List.class);
    } catch (Exception e) {
      return List.of();
    }
  }

  private ExamStandardNodeEntity findNode(Long nodeId, Long tenantId) {
    ExamStandardNodeEntity node = nodeMapper.selectOne(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTenantId, tenantId)
        .eq(ExamStandardNodeEntity::getId, nodeId));
    if (node == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "节点不存在");
    }
    return node;
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
