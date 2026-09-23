package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintQueryReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintTreeVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionBlueprintCoverageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 竞赛独立细目表的导入、发布和树形查询。 */
@Service
@Slf4j
public class CompetitionQuestionBlueprintApplicationService {

  private static final String[] LEVEL_NAMES = {"-", "高级技师", "技师", "高级工", "中级工", "初级工"};
  private final CompetitionQuestionBlueprintMapper blueprintMapper;
  private final CompetitionQuestionBlueprintNodeMapper nodeMapper;
  private final CompetitionQuestionBlueprintBindingMapper bindingMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionBlueprintWorkbookParser parser;
  private final FileApplicationService fileApplicationService;
  private final ObjectMapper objectMapper;

  public CompetitionQuestionBlueprintApplicationService(
      CompetitionQuestionBlueprintMapper blueprintMapper,
      CompetitionQuestionBlueprintNodeMapper nodeMapper,
      CompetitionQuestionBlueprintBindingMapper bindingMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionBlueprintWorkbookParser parser,
      FileApplicationService fileApplicationService,
      ObjectMapper objectMapper) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.bindingMapper = bindingMapper;
    this.eventMapper = eventMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.parser = parser;
    this.fileApplicationService = fileApplicationService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CompetitionQuestionBlueprintVO importBlueprint(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType,
      MultipartFile file, boolean confirmWarnings) {
    PlatformUserPrincipal principal = currentUser();
    String type = normalizeContentType(contentType);
    requireScope(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId);
    if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传细目表文件");
    String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "竞赛细目表.xlsx";
    if (!(fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 xlsx/xls 文件");
    }
    requireNoLiveBlueprint(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId, type);
    Path temp = null;
    try {
      temp = Files.createTempFile("competition-blueprint-", fileName.toLowerCase().endsWith(".xls") ? ".xls" : ".xlsx");
      file.transferTo(temp);
      CompetitionBlueprintWorkbookParser.ParsedBlueprint parsed = parser.parse(temp, type);
      Validation validation = validate(parsed, type, principal.getTenantId(), eventId,
          eventWorkTypeId, eventWorkTypeLevelId);
      if (!validation.errors().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, String.join("；", validation.errors()));
      }
      if (!confirmWarnings && !validation.warnings().isEmpty()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "细目表存在待确认警告，请确认后重新提交：" + String.join("；", validation.warnings()));
      }
      byte[] bytes = Files.readAllBytes(temp);
      FileVO source = fileApplicationService.storeFromBytesForTenant(principal.getTenantId(), bytes, fileName,
          file.getContentType(), "private");
      int version = nextVersion(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId, type);
      CompetitionQuestionBlueprintEntity blueprint = new CompetitionQuestionBlueprintEntity();
      blueprint.setId(IdWorker.getId());
      blueprint.setTenantId(principal.getTenantId()); blueprint.setEventId(eventId);
      blueprint.setEventWorkTypeId(eventWorkTypeId); blueprint.setEventWorkTypeLevelId(eventWorkTypeLevelId);
      blueprint.setContentType(type); blueprint.setVersionNo(version); blueprint.setTitle(parsed.title());
      blueprint.setSourceFileName(fileName); blueprint.setSourceFileId(source.id());
      blueprint.setPointCount(countLeafNodes(parsed.rows()));
      blueprint.setTopicCount(countTopicNodes(parsed.rows()));
      blueprint.setXyzRatio(xyzRatio(parsed.rows())); blueprint.setWeightCheck(validation.weightOk() ? 1 : 0);
      Map<String, Object> report = new LinkedHashMap<>(); report.put("errors", validation.errors()); report.put("warnings", validation.warnings());
      report.put("pointCount", blueprint.getPointCount()); report.put("topicCount", blueprint.getTopicCount());
      report.put("xyzRatio", blueprint.getXyzRatio());
      try { blueprint.setCheckReport(objectMapper.writeValueAsString(report)); }
      catch (Exception ex) { blueprint.setCheckReport("{\"warnings\":[\"报告序列化失败\"]}"); }
      blueprint.setStatus("draft"); blueprint.setCreatedBy(principal.getUserId()); blueprint.setUpdatedBy(principal.getUserId()); blueprint.setDeleted(0);
      try {
        if (blueprintMapper.insert(blueprint) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "竞赛细目表版本保存失败");
        }
      } catch (DuplicateKeyException ex) {
        throw new BusinessException(ErrorCode.CONFLICT, liveBlueprintConflictMessage(type, null, null));
      }
      persistNodes(principal, blueprint, parsed.rows());
      return CompetitionQuestionBlueprintTreeVO.toVO(blueprint);
    } catch (IllegalArgumentException ex) {
      // Parser failures are user-correctable workbook errors. Return them as
      // a validation response instead of allowing a malformed spreadsheet to
      // surface as an HTTP 500.
      throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage() == null ? "细目表格式无效" : ex.getMessage());
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "细目表文件读取失败: " + ex.getMessage());
    } finally {
      if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionQuestionBlueprintVO> page(Long eventId, CompetitionQuestionBlueprintQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    requireScopeOptional(principal.getTenantId(), eventId, req.getEventWorkTypeId(), req.getEventWorkTypeLevelId());
    Page<CompetitionQuestionBlueprintEntity> page = blueprintMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
            .eq(CompetitionQuestionBlueprintEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionQuestionBlueprintEntity::getEventId, eventId)
            .eq(req.getEventWorkTypeId() != null, CompetitionQuestionBlueprintEntity::getEventWorkTypeId, req.getEventWorkTypeId())
            .eq(req.getEventWorkTypeLevelId() != null, CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, req.getEventWorkTypeLevelId())
            .eq(StringUtils.hasText(req.getContentType()), CompetitionQuestionBlueprintEntity::getContentType, req.getContentType())
            .eq(StringUtils.hasText(req.getStatus()), CompetitionQuestionBlueprintEntity::getStatus, req.getStatus())
            .orderByDesc(CompetitionQuestionBlueprintEntity::getVersionNo));
    if (page == null) {
      log.warn("Competition blueprint mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      page = new Page<>(req.getPage(), req.getPageSize());
    }
    List<CompetitionQuestionBlueprintVO> records = new ArrayList<>();
    if (page.getRecords() != null) for (CompetitionQuestionBlueprintEntity e : page.getRecords()) records.add(CompetitionQuestionBlueprintTreeVO.toVO(e));
    return new PageResponse<>(records, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionBlueprintTreeVO tree(Long blueprintId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionBlueprintEntity bp = find(principal.getTenantId(), blueprintId);
    List<CompetitionQuestionBlueprintNodeEntity> nodes = nodeMapper.selectByBlueprint(principal.getTenantId(), blueprintId);
    return CompetitionQuestionBlueprintTreeVO.from(bp, nodes == null ? List.of() : nodes);
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionBlueprintEntity find(Long tenantId, Long blueprintId) {
    CompetitionQuestionBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !tenantId.equals(bp.getTenantId()) || !Integer.valueOf(0).equals(bp.getDeleted())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛细目表不存在");
    }
    return bp;
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionBlueprintEntity findForEvent(Long tenantId, Long eventId, Long blueprintId) {
    CompetitionQuestionBlueprintEntity blueprint = find(tenantId, blueprintId);
    if (eventId == null || !eventId.equals(blueprint.getEventId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛细目表不存在");
    }
    return blueprint;
  }

  @Transactional(readOnly = true)
  public List<CompetitionQuestionBlueprintNodeEntity> nodes(Long tenantId, Long blueprintId) {
    find(tenantId, blueprintId);
    List<CompetitionQuestionBlueprintNodeEntity> nodes = nodeMapper.selectByBlueprint(tenantId, blueprintId);
    return nodes == null ? new ArrayList<>() : new ArrayList<>(nodes);
  }

  @Transactional
  public CompetitionQuestionBlueprintCoverageVO coverage(Long blueprintId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionBlueprintEntity blueprint = find(principal.getTenantId(), blueprintId);
    List<CompetitionQuestionBlueprintNodeEntity> nodes = nodeMapper.selectByBlueprint(principal.getTenantId(), blueprintId);
    nodes = nodes == null ? List.of() : nodes;
    Set<Long> parentIds = nodes.stream().map(CompetitionQuestionBlueprintNodeEntity::getParentId)
        .filter(parent -> parent != null && parent != 0L).collect(Collectors.toSet());
    List<CompetitionQuestionBlueprintNodeEntity> leaves = nodes.stream()
        .filter(node -> node.getId() != null && !parentIds.contains(node.getId()))
        .sorted((left, right) -> {
          int byCode = CompetitionQuestionBlueprintTreeVO.compareCodes(left.getFullCode(), right.getFullCode());
          if (byCode != 0) return byCode;
          int bySortOrder = java.util.Comparator.nullsLast(Integer::compareTo)
              .compare(left.getSortOrder(), right.getSortOrder());
          if (bySortOrder != 0) return bySortOrder;
          return java.util.Comparator.nullsLast(Long::compareTo).compare(left.getId(), right.getId());
        })
        .toList();
    persistInheritedBindings(principal.getTenantId(), blueprintId, principal.getUserId());
    List<CompetitionQuestionBlueprintBindingEntity> bindings =
        listEffectiveBindings(principal.getTenantId(), blueprintId);
    Map<Long, Integer> counts = new HashMap<>();
    for (var binding : bindings) if (binding.getBlueprintNodeId() != null) {
      counts.merge(binding.getBlueprintNodeId(), 1, Integer::sum);
    }
    Map<Long, String> paths = buildNodePathsForCoverage(nodes);
    List<CompetitionQuestionBlueprintCoverageVO.PointCoverage> items = new ArrayList<>();
    int covered = 0; int totalQuestions = 0; int boundQuestions = 0;
    for (var leaf : leaves) {
      int count = counts.getOrDefault(leaf.getId(), 0);
      if (count > 0) covered++;
      totalQuestions += leaf.getQuestionQuota() == null ? 0 : leaf.getQuestionQuota();
      boundQuestions += count;
      int shortage = leaf.getQuestionQuota() == null ? 0 : Math.max(0, leaf.getQuestionQuota() - count);
      items.add(new CompetitionQuestionBlueprintCoverageVO.PointCoverage(String.valueOf(leaf.getId()),
          leaf.getFullCode(), paths.getOrDefault(leaf.getId(), leaf.getName()), leaf.getName(), leaf.getQuestionQuota(), count, shortage));
    }
    return new CompetitionQuestionBlueprintCoverageVO(String.valueOf(blueprint.getId()), leaves.size(), covered,
        totalQuestions, boundQuestions, items);
  }

  private Map<Long, String> buildNodePathsForCoverage(List<CompetitionQuestionBlueprintNodeEntity> nodes) {
    Map<Long, CompetitionQuestionBlueprintNodeEntity> byId = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) byId.put(node.getId(), node);
    Map<Long, String> result = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
      List<String> names = new ArrayList<>();
      Set<Long> guard = new HashSet<>();
      CompetitionQuestionBlueprintNodeEntity current = node;
      while (current != null && guard.add(current.getId())) {
        names.add(current.getName());
        Long parent = current.getParentId();
        current = parent == null || parent == 0L ? null : byId.get(parent);
      }
      java.util.Collections.reverse(names);
      result.put(node.getId(), String.join("/", names));
    }
    return result;
  }

  /**
   * Resolves the one currently published blueprint for an event scope.  Importers
   * and the make-paper service use this method from worker/request threads, so it
   * deliberately accepts tenant and scope identifiers instead of consulting the
   * request security context.
   */
  /**
   * A missing active blueprint is an expected state for the historical question
   * list: callers deliberately catch this business exception and still show
   * unbound questions.  Do not mark a surrounding read transaction rollback-only
   * in that case, otherwise the caller reaches commit with an
   * UnexpectedRollbackException after it has handled the absence.
   */
  @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
  public CompetitionQuestionBlueprintEntity findActive(Long tenantId, Long eventId,
      Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType) {
    String type = normalizeContentType(contentType);
    CompetitionQuestionBlueprintEntity bp = blueprintMapper.selectOne(
        new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
            .eq(CompetitionQuestionBlueprintEntity::getTenantId, tenantId)
            .eq(CompetitionQuestionBlueprintEntity::getEventId, eventId)
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .eq(CompetitionQuestionBlueprintEntity::getContentType, type)
            .eq(CompetitionQuestionBlueprintEntity::getStatus, "active")
            .eq(CompetitionQuestionBlueprintEntity::getDeleted, 0)
            .orderByDesc(CompetitionQuestionBlueprintEntity::getVersionNo)
            .last("LIMIT 1"));
    if (bp == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛事工种等级没有启用的" + ("theory".equals(type) ? "理论" : "实操") + "细目表，请先导入并启用");
    }
    return bp;
  }

  /** Validates a selected blueprint belongs to the requested event scope and content type. */
  @Transactional(readOnly = true)
  public CompetitionQuestionBlueprintEntity requireActive(Long tenantId, Long blueprintId,
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType) {
    CompetitionQuestionBlueprintEntity bp = find(tenantId, blueprintId);
    String type = normalizeContentType(contentType);
    if (!"active".equals(bp.getStatus()) || !eventId.equals(bp.getEventId())
        || !eventWorkTypeId.equals(bp.getEventWorkTypeId())
        || !eventWorkTypeLevelId.equals(bp.getEventWorkTypeLevelId())
        || !type.equals(bp.getContentType())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选细目表未启用或与当前赛事工种等级/内容类型不匹配");
    }
    return bp;
  }

  @Transactional(readOnly = true)
  public byte[] readSourceFile(Long fileId) {
    if (fileId == null) throw new BusinessException(ErrorCode.NOT_FOUND, "细目表源文件不存在");
    return fileApplicationService.readFileBytesById(fileId);
  }

  @Transactional
  public CompetitionQuestionBlueprintVO activate(Long blueprintId, boolean confirmWarnings) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionBlueprintEntity bp = find(principal.getTenantId(), blueprintId);
    if (!confirmWarnings && hasWarnings(bp.getCheckReport())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "细目表存在待确认警告，请确认后启用");
    }
    List<CompetitionQuestionBlueprintEntity> active = blueprintMapper.selectList(new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
        .eq(CompetitionQuestionBlueprintEntity::getTenantId, principal.getTenantId()).eq(CompetitionQuestionBlueprintEntity::getEventId, bp.getEventId())
        .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeId, bp.getEventWorkTypeId()).eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, bp.getEventWorkTypeLevelId())
        .eq(CompetitionQuestionBlueprintEntity::getContentType, bp.getContentType()).eq(CompetitionQuestionBlueprintEntity::getStatus, "active").eq(CompetitionQuestionBlueprintEntity::getDeleted, 0));
    if (active != null) for (CompetitionQuestionBlueprintEntity old : active) {
      old.setStatus("archived"); old.setUpdatedBy(principal.getUserId());
      if (blueprintMapper.updateById(old) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "旧版竞赛细目表归档失败");
      }
    }
    bp.setStatus("active"); bp.setUpdatedBy(principal.getUserId());
    if (blueprintMapper.updateById(bp) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛细目表启用失败");
    }
    persistInheritedBindings(principal.getTenantId(), bp.getId(), principal.getUserId());
    return CompetitionQuestionBlueprintTreeVO.toVO(bp);
  }

  @Transactional
  public void archive(Long blueprintId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionBlueprintEntity bp = find(principal.getTenantId(), blueprintId);
    if ("archived".equals(bp.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "细目表已经归档");
    }
    bp.setStatus("archived"); bp.setUpdatedBy(principal.getUserId());
    if (blueprintMapper.updateById(bp) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛细目表归档失败");
    }
  }

  /**
   * Bindings belong to a specific blueprint version.  When an older version is
   * archived and a newer one is enabled, question-bank rows stay on the archived
   * version unless the same full_code leaf exists on the live tree.  Grouping
   * and coverage therefore merge those equivalent archived bindings into the
   * current version so a duplicate import does not look like an empty bank.
   */
  @Transactional(readOnly = true)
  public List<CompetitionQuestionBlueprintBindingEntity> listEffectiveBindings(Long tenantId, Long blueprintId) {
    if (tenantId == null || blueprintId == null || bindingMapper == null) return List.of();
    CompetitionQuestionBlueprintEntity target = find(tenantId, blueprintId);
    List<CompetitionQuestionBlueprintNodeEntity> targetNodes = nodeMapper.selectByBlueprint(tenantId, blueprintId);
    targetNodes = targetNodes == null ? List.of() : targetNodes;
    Set<Long> parentIds = targetNodes.stream()
        .map(CompetitionQuestionBlueprintNodeEntity::getParentId)
        .filter(parent -> parent != null && parent != 0L)
        .collect(Collectors.toSet());
    Map<String, Long> leafIdByCode = new LinkedHashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : targetNodes) {
      if (node.getId() == null || parentIds.contains(node.getId())) continue;
      String code = normalizeNodeCode(node.getFullCode());
      if (!code.isBlank()) leafIdByCode.putIfAbsent(code, node.getId());
    }
    List<CompetitionQuestionBlueprintBindingEntity> own = bindingMapper.selectByBlueprint(tenantId, blueprintId);
    List<CompetitionQuestionBlueprintBindingEntity> result = new ArrayList<>();
    Set<Long> boundQuestions = new HashSet<>();
    if (own != null) {
      for (CompetitionQuestionBlueprintBindingEntity row : own) {
        if (row == null || row.getQuestionId() == null) continue;
        result.add(row);
        boundQuestions.add(row.getQuestionId());
      }
    }
    List<CompetitionQuestionBlueprintEntity> archived = blueprintMapper.selectList(
        new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
            .eq(CompetitionQuestionBlueprintEntity::getTenantId, tenantId)
            .eq(CompetitionQuestionBlueprintEntity::getEventId, target.getEventId())
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeId, target.getEventWorkTypeId())
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, target.getEventWorkTypeLevelId())
            .eq(CompetitionQuestionBlueprintEntity::getContentType, target.getContentType())
            .eq(CompetitionQuestionBlueprintEntity::getStatus, "archived")
            .eq(CompetitionQuestionBlueprintEntity::getDeleted, 0)
            .ne(CompetitionQuestionBlueprintEntity::getId, blueprintId)
            .orderByDesc(CompetitionQuestionBlueprintEntity::getVersionNo));
    if (archived == null || archived.isEmpty()) return result;
    for (CompetitionQuestionBlueprintEntity sibling : archived) {
      if (sibling == null || sibling.getId() == null) continue;
      List<CompetitionQuestionBlueprintNodeEntity> siblingNodes = nodeMapper.selectByBlueprint(tenantId, sibling.getId());
      Map<Long, String> siblingCodeById = new HashMap<>();
      if (siblingNodes != null) {
        for (CompetitionQuestionBlueprintNodeEntity node : siblingNodes) {
          if (node == null || node.getId() == null) continue;
          siblingCodeById.put(node.getId(), normalizeNodeCode(node.getFullCode()));
        }
      }
      List<CompetitionQuestionBlueprintBindingEntity> siblingBindings =
          bindingMapper.selectByBlueprint(tenantId, sibling.getId());
      if (siblingBindings == null) continue;
      for (CompetitionQuestionBlueprintBindingEntity binding : siblingBindings) {
        if (binding == null || binding.getQuestionId() == null || boundQuestions.contains(binding.getQuestionId())) {
          continue;
        }
        String code = siblingCodeById.get(binding.getBlueprintNodeId());
        Long newNodeId = code == null ? null : leafIdByCode.get(code);
        if (newNodeId == null) continue;
        CompetitionQuestionBlueprintBindingEntity remapped = new CompetitionQuestionBlueprintBindingEntity();
        remapped.setId(binding.getId());
        remapped.setTenantId(tenantId);
        remapped.setQuestionId(binding.getQuestionId());
        remapped.setBlueprintId(blueprintId);
        remapped.setBlueprintNodeId(newNodeId);
        remapped.setMatchMode(StringUtils.hasText(binding.getMatchMode()) ? binding.getMatchMode() : "full_code");
        remapped.setSourceBatchId(binding.getSourceBatchId());
        result.add(remapped);
        boundQuestions.add(binding.getQuestionId());
      }
    }
    return result;
  }

  @Transactional
  public int persistInheritedBindings(Long tenantId, Long blueprintId, Long userId) {
    if (tenantId == null || blueprintId == null || bindingMapper == null) return 0;
    List<CompetitionQuestionBlueprintBindingEntity> effective = listEffectiveBindings(tenantId, blueprintId);
    List<CompetitionQuestionBlueprintBindingEntity> own = bindingMapper.selectByBlueprint(tenantId, blueprintId);
    Set<Long> ownQuestions = new HashSet<>();
    if (own != null) {
      for (CompetitionQuestionBlueprintBindingEntity row : own) {
        if (row != null && row.getQuestionId() != null) ownQuestions.add(row.getQuestionId());
      }
    }
    int saved = 0;
    for (CompetitionQuestionBlueprintBindingEntity row : effective) {
      if (row == null || row.getQuestionId() == null || ownQuestions.contains(row.getQuestionId())) continue;
      CompetitionQuestionBlueprintBindingEntity copy = new CompetitionQuestionBlueprintBindingEntity();
      copy.setId(IdWorker.getId());
      copy.setTenantId(tenantId);
      copy.setQuestionId(row.getQuestionId());
      copy.setBlueprintId(blueprintId);
      copy.setBlueprintNodeId(row.getBlueprintNodeId());
      copy.setMatchMode(StringUtils.hasText(row.getMatchMode()) ? row.getMatchMode() : "full_code");
      copy.setSourceBatchId(row.getSourceBatchId());
      copy.setCreatedBy(userId);
      copy.setUpdatedBy(userId);
      copy.setDeleted(0);
      try {
        if (bindingMapper.insert(copy) == 1) {
          ownQuestions.add(row.getQuestionId());
          saved++;
        }
      } catch (DuplicateKeyException ex) {
        ownQuestions.add(row.getQuestionId());
      }
    }
    if (saved > 0) {
      log.info("Inherited {} question bindings onto blueprint {}", saved, blueprintId);
    }
    return saved;
  }

  static String liveBlueprintConflictMessage(String contentType, Integer versionNo, String status) {
    String typeName = "practical".equalsIgnoreCase(contentType) ? "实操" : "理论";
    String statusName = "active".equals(status) ? "启用" : "draft".equals(status) ? "草稿" : "";
    String existing = versionNo == null ? ""
        : "，当前已存在 v" + versionNo + (statusName.isBlank() ? "" : "（" + statusName + "）");
    return "同一赛事工种等级只能有一份" + typeName + "细目表" + existing
        + "。如需更换，请先归档现有细目表后再导入。";
  }

  private void persistNodes(PlatformUserPrincipal principal, CompetitionQuestionBlueprintEntity bp,
      List<CompetitionBlueprintWorkbookParser.ParsedNode> rows) {
    Map<Integer, Long> ids = new HashMap<>(); Map<Integer, String> fullCodes = new HashMap<>();
    for (int i = 0; i < rows.size(); i++) {
      CompetitionBlueprintWorkbookParser.ParsedNode n = rows.get(i);
      CompetitionQuestionBlueprintNodeEntity e = new CompetitionQuestionBlueprintNodeEntity(); e.setId(IdWorker.getId());
      e.setTenantId(principal.getTenantId()); e.setBlueprintId(bp.getId());
      Long parent = n.parentIndex() >= 0 ? ids.get(n.parentIndex()) : null; e.setParentId(parent == null ? 0L : parent);
      e.setNodeLevel(n.nodeLevel()); e.setCode(n.code()); e.setName(n.name()); e.setWeight(n.weight()); e.setSelectMode(n.selectMode());
      e.setSkillRequirement(n.skillRequirement()); e.setRelatedKnowledge(n.relatedKnowledge()); e.setImportance(n.importance()); e.setXyzStat(n.xyzStat()); e.setQuestionQuota(n.questionQuota()); e.setSortOrder(n.sortOrder());
      String parentFull = n.parentIndex() >= 0 ? fullCodes.getOrDefault(n.parentIndex(), "") : "";
      String full = combineNodeCode(parentFull, e.getCode()); e.setFullCode(full);
      e.setCreatedBy(principal.getUserId()); e.setUpdatedBy(principal.getUserId()); e.setDeleted(0);
      if (nodeMapper.insert(e) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "竞赛细目表节点保存失败");
      }
      ids.put(i, e.getId()); fullCodes.put(i, full);
    }
  }

  private Validation validate(CompetitionBlueprintWorkbookParser.ParsedBlueprint parsed, String type,
      Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    List<String> errors = new ArrayList<>(); List<String> warnings = new ArrayList<>(parsed.warnings());
    Map<String, Integer> seen = new HashMap<>(); Set<Integer> levels = new HashSet<>();
    Map<Integer, String> fullPaths = new HashMap<>();
    for (int rowIndex = 0; rowIndex < parsed.rows().size(); rowIndex++) {
      CompetitionBlueprintWorkbookParser.ParsedNode n = parsed.rows().get(rowIndex);
      if (n.name().isBlank()) errors.add("第" + (n.sourceRow() + 1) + "行节点名称不能为空");
      if (n.code().isBlank()) errors.add("第" + (n.sourceRow() + 1) + "行节点代码不能为空");
      if (n.nodeLevel() == 1) {
        if (n.parentIndex() >= 0) {
          errors.add("一级节点不能存在父节点: " + n.name());
        }
      } else if (n.parentIndex() < 0 || n.parentIndex() >= rowIndex) {
        errors.add("第" + (n.sourceRow() + 1) + "行缺少有效的上级节点: " + n.name());
      } else {
        int parentLevel = parsed.rows().get(n.parentIndex()).nodeLevel();
        if (parentLevel != n.nodeLevel() - 1) {
          errors.add("节点层级与父节点不连续: " + n.name());
        }
      }
      if (n.weight() != null && (n.weight().signum() < 0 || n.weight().compareTo(BigDecimal.ONE) > 0)) {
        errors.add("节点权重必须在 0% 到 100% 之间: " + n.name());
      }
      levels.add(n.nodeLevel()); String key = n.parentIndex() + "|" + n.code();
      if (seen.put(key, 1) != null) errors.add("节点代码重复: " + n.code());
      String parentPath = n.parentIndex() < 0 ? "" : fullPaths.getOrDefault(n.parentIndex(), "");
      String fullPath = combineNodeCode(parentPath, n.code());
      if (fullPaths.containsValue(fullPath)) errors.add("节点完整路径编码重复: " + fullPath);
      fullPaths.put(rowIndex, fullPath);
      if (n.nodeLevel() >= ("theory".equals(type) ? 4 : 3) && n.importance() != null
          && !Set.of("X", "Y", "Z").contains(n.importance().toUpperCase())) errors.add("重要程度必须是 X/Y/Z: " + n.name());
      if (n.nodeLevel() >= ("theory".equals(type) ? 4 : 3) && n.questionQuota() != null && n.questionQuota() <= 0) errors.add("试题量必须为正数: " + n.name());
    }
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (int index = 0; index < parsed.rows().size(); index++) {
      Integer parent = parsed.rows().get(index).parentIndex();
      children.computeIfAbsent(parent == null ? -1 : parent, ignored -> new ArrayList<>()).add(index);
    }
    for (int index = 0; index < parsed.rows().size(); index++) {
      CompetitionBlueprintWorkbookParser.ParsedNode node = parsed.rows().get(index);
      List<Integer> childIndexes = children.getOrDefault(index, List.of());
      boolean leaf = childIndexes.isEmpty();
      if (leaf && (node.importance() == null || !Set.of("X", "Y", "Z").contains(node.importance().toUpperCase()))) {
        errors.add("末级考评点重要程度必须是 X/Y/Z: " + node.name());
      }
      // 实操“试题量”只表示题库储备要求，不是组卷题量。业务细目表允许
      // 该列留空，实际能否组卷由末级绑定题库的预检结果决定；仅对已经
      // 填写但不为正数的值沿用上面的通用校验。
      boolean topic = !childIndexes.isEmpty() && childIndexes.stream().allMatch(child -> children.getOrDefault(child, List.of()).isEmpty());
      if ("practical".equals(type) && topic) {
        if (node.weight() == null || node.weight().signum() < 0) errors.add("实操主题权重不能为空且不能为负数: " + node.name());
        String selectMode = node.selectMode() == null ? "" : node.selectMode().trim();
        if (!selectMode.isBlank() && !Set.of("必考", "全部必考", "必选", "全部", "选考", "任选一项", "任选1项",
            "任一", "任意一项", "任选", "任意", "required", "mandatory", "optional", "chooseone", "choose_one", "choose-one")
            .contains(selectMode.toLowerCase())) {
          errors.add("实操主题选考方式必须是必考或选考: " + node.name());
        }
      }
    }
    if (!levels.contains(1)) errors.add("未解析到一级考评范围");
    validateSiblingWeights(parsed.rows(), errors);
    List<CompetitionBlueprintWorkbookParser.ParsedNode> topNodes = parsed.rows().stream()
        .filter(n -> n.nodeLevel() == 1).toList();
    boolean topWeightsComplete = topNodes.stream().allMatch(n -> n.weight() != null);
    BigDecimal top = topNodes.stream().map(CompetitionBlueprintWorkbookParser.ParsedNode::weight)
        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean weightOk = topWeightsComplete
        && top.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.005")) <= 0;
    if (!weightOk) errors.add("一级权重合计 " + top + " 不等于 100%");
    warnings.addAll(validateXyzDeclarations(parsed.rows()));
    warnings.addAll(validateMetadataTitle(tenantId, eventId, workTypeId, levelId, parsed.title()));
    return new Validation(errors, warnings, weightOk);
  }

  /**
   * Metadata conflicts are warnings, never silent corrections.  We compare the
   * title against sibling work types/levels in the selected event so a workbook
   * labelled “粗液脱硅工” cannot silently be published for “焙烧工”.
   */
  private List<String> validateMetadataTitle(Long tenantId, Long eventId, Long workTypeId,
      Long levelId, String title) {
    List<String> warnings = new ArrayList<>();
    String normalizedTitle = normalizeTitle(title);
    if (normalizedTitle.isBlank()) return warnings;
    String selectedName = "";
    List<CompetitionEventWorkTypeEntity> workTypes = workTypeMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workTypes != null) {
      CompetitionEventWorkTypeEntity selected = workTypes.stream()
          .filter(item -> workTypeId.equals(item.getId())).findFirst().orElse(null);
      selectedName = selected == null ? "" : normalizeTitle(selected.getDisplayName());
      for (CompetitionEventWorkTypeEntity candidate : workTypes) {
        if (candidate == null || workTypeId.equals(candidate.getId())) continue;
        String candidateName = normalizeTitle(candidate.getDisplayName());
        if (!candidateName.isBlank() && normalizedTitle.contains(candidateName)
            && !candidateName.equals(selectedName)) {
          warnings.add("文件标题包含其他赛事工种“" + candidate.getDisplayName() + "”，当前选择工种可能不一致");
          break;
        }
      }
      // A generic title remains valid; only an explicit sibling conflict warns.
    }
    // These are common legacy workbook titles whose work-type text is known to
    // be confused in the supplied practical samples.  Keep the warning
    // explicit even when an older event has no sibling display-name metadata.
    boolean knownProfessionMismatch = normalizedTitle.contains("粗液脱硅")
        && !selectedName.contains("粗液脱硅")
        || normalizedTitle.contains("焙烧") && !selectedName.contains("焙烧");
    if (knownProfessionMismatch && warnings.stream().noneMatch(item -> item.contains("工种"))) {
      warnings.add("文件标题中的职业名称与当前选择工种可能不一致，请确认后继续");
    }
    List<CompetitionEventWorkTypeLevelEntity> levels = levelMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (levels != null) {
      for (CompetitionEventWorkTypeLevelEntity candidate : levels) {
        if (candidate == null || levelId.equals(candidate.getId())) continue;
        String candidateLevel = normalizeTitle(candidate.getLevelName());
        if ((!candidateLevel.isBlank() && normalizedTitle.contains(candidateLevel))
            || (StringUtils.hasText(candidate.getLevelCode())
                && normalizedTitle.contains(normalizeTitle(candidate.getLevelCode())))) {
          warnings.add("文件标题包含其他等级“" + candidate.getLevelName() + "”，当前选择等级可能不一致");
          break;
        }
      }
    }
    return warnings;
  }

  private String normalizeTitle(String value) {
    if (value == null) return "";
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
  }

  private List<String> validateXyzDeclarations(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows) {
    List<String> warnings = new ArrayList<>();
    Map<Integer, List<Integer>> children = new HashMap<>();
    for (int index = 0; index < rows.size(); index++) {
      Integer parent = rows.get(index).parentIndex();
      children.computeIfAbsent(parent == null ? -1 : parent, ignored -> new ArrayList<>()).add(index);
    }
    for (int index = 0; index < rows.size(); index++) {
      String declared = rows.get(index).xyzStat();
      if (!StringUtils.hasText(declared)) continue;
      String[] values = declared.replace('：', ':').split(":");
      if (values.length != 3) {
        warnings.add("节点 " + rows.get(index).name() + " 的 XYZ 比例格式无法识别: " + declared);
        continue;
      }
      try {
        int[] expected = {Integer.parseInt(values[0].trim()), Integer.parseInt(values[1].trim()), Integer.parseInt(values[2].trim())};
        int[] actual = xyzCounts(rows, children, index);
        if (expected[0] != actual[0] || expected[1] != actual[1] || expected[2] != actual[2]) {
          warnings.add("节点 " + rows.get(index).name() + " 声明 XYZ 比例 " + declared
              + " 与实际统计 " + actual[0] + ":" + actual[1] + ":" + actual[2] + " 不一致");
        }
      } catch (NumberFormatException ex) {
        warnings.add("节点 " + rows.get(index).name() + " 的 XYZ 比例格式无法识别: " + declared);
      }
    }
    return warnings;
  }

  private int[] xyzCounts(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows,
      Map<Integer, List<Integer>> children, int rootIndex) {
    int[] counts = new int[3];
    List<Integer> descendants = new ArrayList<>();
    collectDescendants(children, rootIndex, descendants);
    for (Integer index : descendants) {
      CompetitionBlueprintWorkbookParser.ParsedNode node = rows.get(index);
      if (children.containsKey(index)) continue;
      String importance = node.importance() == null ? "" : node.importance().trim().toUpperCase();
      if ("X".equals(importance)) counts[0]++;
      else if ("Y".equals(importance)) counts[1]++;
      else if ("Z".equals(importance)) counts[2]++;
    }
    return counts;
  }

  private void collectDescendants(Map<Integer, List<Integer>> children, int parent, List<Integer> output) {
    for (Integer child : children.getOrDefault(parent, List.of())) {
      output.add(child);
      collectDescendants(children, child, output);
    }
  }

  private int countLeafNodes(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows) {
    Set<Integer> parents = rows.stream().map(CompetitionBlueprintWorkbookParser.ParsedNode::parentIndex)
        .filter(index -> index != null && index >= 0).collect(Collectors.toSet());
    int leaves = 0;
    for (int index = 0; index < rows.size(); index++) {
      if (!parents.contains(index)) leaves++;
    }
    return leaves;
  }

  private void validateSiblingWeights(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows, List<String> errors) {
    Map<Integer, List<CompetitionBlueprintWorkbookParser.ParsedNode>> children = new HashMap<>();
    Map<Integer, List<Integer>> childIndexes = new HashMap<>();
    for (int index = 0; index < rows.size(); index++) {
      CompetitionBlueprintWorkbookParser.ParsedNode node = rows.get(index);
      children.computeIfAbsent(node.parentIndex(), ignored -> new ArrayList<>()).add(node);
      childIndexes.computeIfAbsent(node.parentIndex(), ignored -> new ArrayList<>()).add(index);
    }
    for (Map.Entry<Integer, List<CompetitionBlueprintWorkbookParser.ParsedNode>> entry : children.entrySet()) {
      List<CompetitionBlueprintWorkbookParser.ParsedNode> childNodes = entry.getValue();
      boolean anyWeight = childNodes.stream().anyMatch(node -> node.weight() != null);
      if (!anyWeight) {
        // Leaf assessment-point rows are intentionally unweighted.  A higher
        // hierarchy group, however, must not silently omit all child weights.
        boolean allLeaves = childIndexes.getOrDefault(entry.getKey(), List.of()).stream()
            .noneMatch(index -> childIndexes.containsKey(index));
        if (!allLeaves) {
          errors.add("非末级节点的下级权重不能为空（父节点行 " + entry.getKey() + "）");
        }
        continue;
      }
      boolean allWeights = childNodes.stream().allMatch(node -> node.weight() != null);
      if (!allWeights) {
        errors.add("同一父节点下级权重必须全部填写或全部留空（父节点行 " + entry.getKey() + "）");
        continue;
      }
      int parentIndex = entry.getKey();
      CompetitionBlueprintWorkbookParser.ParsedNode parent = parentIndex >= 0
          && parentIndex < rows.size() ? rows.get(parentIndex) : null;
      boolean requiredParentWithChooseOneChildren = parent != null
          && isRequiredSelection(parent.selectMode())
          && childNodes.stream().allMatch(node -> isChooseOneSelection(node.selectMode()));
      if (requiredParentWithChooseOneChildren) {
        BigDecimal expected = parent.weight();
        if (expected == null) {
          errors.add("必考父节点权重不能为空（父节点行 " + parentIndex + "）");
        } else if (childNodes.stream().anyMatch(node -> node.weight().subtract(expected).abs()
            .compareTo(new BigDecimal("0.005")) > 0)) {
          errors.add("必考父节点下的任选候选必须继承父节点权重 " + expected
              + "（父节点行 " + parentIndex + "）");
        }
        continue;
      }
      List<BigDecimal> childWeights = childNodes.stream()
          .map(CompetitionBlueprintWorkbookParser.ParsedNode::weight).toList();
      // A parent with a single weighted child is still a hierarchy integrity
      // check: its child must carry the complete parent weight. Skipping
      // single-child groups allowed malformed workbooks (for example a 60%
      // child under a 100% parent) to pass import silently.
      if (childWeights.isEmpty()) continue;
      BigDecimal total = childWeights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal expected = BigDecimal.ONE;
      if (parentIndex >= 0 && parentIndex < rows.size() && rows.get(parentIndex).weight() != null) {
        expected = rows.get(parentIndex).weight();
      }
      if (total.subtract(expected).abs().compareTo(new BigDecimal("0.005")) > 0) {
        errors.add("同一父节点下级权重合计 " + total + " 不等于父节点权重 " + expected
            + "（父节点行 " + entry.getKey() + "）");
      }
    }
  }

  private boolean isRequiredSelection(String value) {
    if (!StringUtils.hasText(value)) return false;
    return Set.of("必考", "全部必考", "必选", "全部", "required", "mandatory")
        .contains(value.replace(" ", "").replace("　", "").toLowerCase());
  }

  private boolean isChooseOneSelection(String value) {
    if (!StringUtils.hasText(value)) return false;
    return Set.of("选考", "任选一项", "任选1项", "任一", "任意一项", "任选", "任意",
        "optional", "chooseone", "choose_one", "choose-one")
        .contains(value.replace(" ", "").replace("　", "").toLowerCase());
  }

  private int countTopicNodes(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows) {
    Set<Integer> parents = rows.stream().map(CompetitionBlueprintWorkbookParser.ParsedNode::parentIndex)
        .filter(index -> index != null && index >= 0).collect(Collectors.toSet());
    Set<Integer> topicIndexes = new HashSet<>();
    for (int index = 0; index < rows.size(); index++) {
      CompetitionBlueprintWorkbookParser.ParsedNode node = rows.get(index);
      if (!parents.contains(index) && node.parentIndex() >= 0) topicIndexes.add(node.parentIndex());
    }
    return topicIndexes.size();
  }

  private String xyzRatio(List<CompetitionBlueprintWorkbookParser.ParsedNode> rows) {
    Map<String, Integer> c = new LinkedHashMap<>(); c.put("X", 0); c.put("Y", 0); c.put("Z", 0);
    for (var n : rows) if (n.importance() != null) { String x = n.importance().toUpperCase(); if (c.containsKey(x)) c.put(x, c.get(x) + 1); }
    return c.get("X") + ":" + c.get("Y") + ":" + c.get("Z");
  }

  /**
   * Workbook authors use both local node codes (for example {@code 1}) and
   * already-expanded codes (for example {@code A-1-1}). The latter must not be
   * prefixed a second time when the persisted full path is built.
   */
  private String combineNodeCode(String parentCode, String code) {
    String parent = parentCode == null ? "" : parentCode.trim();
    String child = code == null ? "" : code.trim();
    if (parent.isBlank()) return child;
    if (child.isBlank()) return parent;
    if (child.startsWith(parent + "-")
        || child.startsWith(parent + ".")
        || child.startsWith(parent + "_")) {
      return child;
    }
    return parent + "-" + child;
  }

  private int nextVersion(Long tenantId, Long eventId, Long workTypeId, Long levelId, String type) {
    CompetitionQuestionBlueprintEntity last = blueprintMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
        .eq(CompetitionQuestionBlueprintEntity::getTenantId, tenantId).eq(CompetitionQuestionBlueprintEntity::getEventId, eventId)
        .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeId, workTypeId).eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, levelId)
        .eq(CompetitionQuestionBlueprintEntity::getContentType, type)
        .eq(CompetitionQuestionBlueprintEntity::getDeleted, 0)
        .orderByDesc(CompetitionQuestionBlueprintEntity::getVersionNo).last("LIMIT 1"));
    return last == null || last.getVersionNo() == null ? 1 : last.getVersionNo() + 1;
  }

  private void requireNoLiveBlueprint(Long tenantId, Long eventId, Long workTypeId, Long levelId, String type) {
    CompetitionQuestionBlueprintEntity live = findLiveBlueprint(tenantId, eventId, workTypeId, levelId, type);
    if (live != null) {
      throw new BusinessException(ErrorCode.CONFLICT,
          liveBlueprintConflictMessage(type, live.getVersionNo(), live.getStatus()));
    }
  }

  private CompetitionQuestionBlueprintEntity findLiveBlueprint(
      Long tenantId, Long eventId, Long workTypeId, Long levelId, String type) {
    List<CompetitionQuestionBlueprintEntity> live = blueprintMapper.selectList(
        new LambdaQueryWrapper<CompetitionQuestionBlueprintEntity>()
            .eq(CompetitionQuestionBlueprintEntity::getTenantId, tenantId)
            .eq(CompetitionQuestionBlueprintEntity::getEventId, eventId)
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeId, workTypeId)
            .eq(CompetitionQuestionBlueprintEntity::getEventWorkTypeLevelId, levelId)
            .eq(CompetitionQuestionBlueprintEntity::getContentType, type)
            .in(CompetitionQuestionBlueprintEntity::getStatus, List.of("draft", "active"))
            .eq(CompetitionQuestionBlueprintEntity::getDeleted, 0)
            .orderByDesc(CompetitionQuestionBlueprintEntity::getVersionNo));
    return live == null || live.isEmpty() ? null : live.get(0);
  }

  private String normalizeNodeCode(String value) {
    if (value == null) return "";
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
  }

  private void requireScope(Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    if (eventId == null || workTypeId == null || levelId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事、工种和等级不能为空");
    CompetitionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId).eq(CompetitionEventEntity::getId, eventId)
        .eq(CompetitionEventEntity::getDeleted, 0));
    CompetitionEventWorkTypeEntity wt = workTypeMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
        .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeEntity::getId, workTypeId)
        .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
        .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeLevelEntity::getId, levelId)
        .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (event == null || !tenantId.equals(event.getTenantId()) || wt == null || !tenantId.equals(wt.getTenantId())
        || !eventId.equals(wt.getEventId()) || level == null || !tenantId.equals(level.getTenantId())
        || !eventId.equals(level.getEventId()) || !workTypeId.equals(level.getEventWorkTypeId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种等级范围不存在");
    }
  }
  private void requireScopeOptional(Long tenantId, Long eventId, Long workTypeId, Long levelId) { if (workTypeId != null && levelId != null) requireScope(tenantId, eventId, workTypeId, levelId); }
  private String normalizeContentType(String type) { if ("theory".equalsIgnoreCase(type) || "practical".equalsIgnoreCase(type)) return type.toLowerCase(); throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical"); }
  private boolean hasWarnings(String report) {
    if (!StringUtils.hasText(report)) return false;
    try {
      com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(report).path("warnings");
      return node.isArray() && node.size() > 0;
    } catch (Exception ex) {
      log.warn("Failed to parse blueprint warning report; requiring explicit confirmation", ex);
      return true;
    }
  }
  private PlatformUserPrincipal currentUser() { return SecurityContext.currentUser(); }
  private record Validation(List<String> errors, List<String> warnings, boolean weightOk) {}
}
