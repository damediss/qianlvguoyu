package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Resolves and evaluates assessor work type scopes against the current tenant's course work type tree. */
@Service
public class ExamWorkTypeCoverageService {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String PATH_SEPARATOR = " / ";

  private final ExamWorkTypeMapper mapper;

  public ExamWorkTypeCoverageService(ExamWorkTypeMapper mapper) {
    this.mapper = mapper;
  }

  /** Resolves an id or a legacy exact name to one active node in the current tenant. */
  public Long resolveWorkTypeId(Long workTypeId, String legacyName) {
    Snapshot snapshot = loadSnapshot();
    if (workTypeId != null) {
      requireActivePath(workTypeId, snapshot, "工种");
      return workTypeId;
    }
    List<String> names = parseNames(Collections.singletonList(legacyName));
    if (names.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择当前租户的有效工种");
    }
    return resolveUniqueNames(names, snapshot, "工种").getFirst();
  }

  /** Validates and returns selected active work type ids, supporting old name-only callers. */
  public List<Long> resolveSelectedWorkTypeIds(Collection<Long> workTypeIds, Collection<String> legacyNames) {
    Snapshot snapshot = loadSnapshot();
    List<Long> ids = distinctIds(workTypeIds);
    if (!ids.isEmpty()) {
      for (Long id : ids) {
        requireActivePath(id, snapshot, "工种");
      }
      return ids;
    }
    List<String> names = parseNames(legacyNames);
    if (names.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请从当前租户的工种树中选择至少一个有效工种");
    }
    return resolveUniqueNames(names, snapshot, "工种");
  }

  /** Resolves an assessor's persisted ids or maps legacy names only when each exact title is unique. */
  public WorkTypeSelection readAssessorSelection(String workTypeJson, String legacyScope, String assessorName) {
    try {
      Snapshot snapshot = loadSnapshot();
      StoredSelection stored = parseStoredSelection(workTypeJson, legacyScope);
      List<Long> ids = stored.ids().isEmpty()
          ? resolveUniqueNames(stored.names(), snapshot, "考评员历史工种")
          : stored.ids();
      List<String> paths = new ArrayList<>(ids.size());
      for (Long id : ids) {
        paths.add(pathOf(id, snapshot, "考评员工种授权"));
      }
      return new WorkTypeSelection(ids, paths, null);
    } catch (BusinessException exception) {
      List<String> names = parseStoredNamesSafely(workTypeJson, legacyScope);
      return new WorkTypeSelection(List.of(), names,
          assessorLabel(assessorName) + exception.getMessage()
              + "；请在考评员信息页编辑档案，并从当前工种树重新选择授权工种。");
    }
  }

  /** Returns true only if the assessor's every required work type is covered by an authorized ancestor. */
  public boolean assessorCovers(
      String workTypeJson,
      String legacyScope,
      String assessorName,
      Collection<Long> requiredWorkTypeIds
  ) {
    List<Long> required = distinctIds(requiredWorkTypeIds);
    if (required.isEmpty()) {
      return false;
    }
    Snapshot snapshot = loadSnapshot();
    List<Long> authorized = resolveAssessorIds(workTypeJson, legacyScope, assessorName, snapshot);
    if (authorized.isEmpty()) {
      return false;
    }
    for (Long id : authorized) {
      requireActivePath(id, snapshot, assessorLabel(assessorName) + "授权工种");
    }
    for (Long requiredId : required) {
      Set<Integer> requiredAncestors = requireActivePath(requiredId, snapshot, "考评范围工种");
      boolean covered = authorized.stream().anyMatch(id -> requiredAncestors.contains(id.intValue()));
      if (!covered) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks one assessor while building a candidate list. Invalid historical assessor grants are
   * returned as a warning so one bad profile cannot fail the whole list request. Invalid required
   * task scopes still fail fast because they affect every candidate.
   */
  public AssessorCoverageResult checkAssessorCoverageForListing(
      String workTypeJson,
      String legacyScope,
      String assessorName,
      Collection<Long> requiredWorkTypeIds
  ) {
    Snapshot snapshot = loadSnapshot();
    List<Long> required = distinctIds(requiredWorkTypeIds);
    if (required.isEmpty()) {
      return new AssessorCoverageResult(false, null);
    }

    List<Set<Integer>> requiredAncestors = required.stream()
        .map(id -> requireActivePath(id, snapshot, "考评范围工种"))
        .toList();
    List<Long> authorized;
    try {
      authorized = resolveAssessorIds(workTypeJson, legacyScope, assessorName, snapshot);
      for (Long id : authorized) {
        requireActivePath(id, snapshot, assessorLabel(assessorName) + "授权工种");
      }
    } catch (BusinessException exception) {
      return new AssessorCoverageResult(false, exception.getMessage());
    }
    if (authorized.isEmpty()) {
      return new AssessorCoverageResult(false, null);
    }

    boolean covered = requiredAncestors.stream().allMatch(ancestors -> authorized.stream()
        .anyMatch(id -> ancestors.contains(id.intValue())));
    return new AssessorCoverageResult(covered, null);
  }

  /** Fails with an actionable message when a member's authorization does not cover every required node. */
  public void requireAssessorCovers(
      String workTypeJson,
      String legacyScope,
      String assessorName,
      Collection<Long> requiredWorkTypeIds
  ) {
    List<Long> required = distinctIds(requiredWorkTypeIds);
    if (required.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前考评组和认定任务均未配置有效工种范围，请先维护认定任务工种范围");
    }
    if (!assessorCovers(workTypeJson, legacyScope, assessorName, required)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          assessorLabel(assessorName) + "工种授权未覆盖当前范围的全部工种，请维护考评员授权或调整考评组工种范围");
    }
  }

  /** Maps required legacy titles by exact, unique title match; it never uses substring matching. */
  public List<Long> resolveRequiredWorkTypeIds(Collection<Long> ids, Collection<String> names) {
    return resolveSelectedWorkTypeIds(ids, names);
  }

  public String pathOf(Long workTypeId) {
    return pathOf(workTypeId, loadSnapshot(), "工种");
  }

  public List<String> pathsOf(Collection<Long> workTypeIds) {
    List<Long> ids = distinctIds(workTypeIds);
    Snapshot snapshot = loadSnapshot();
    return ids.stream().map(id -> pathOf(id, snapshot, "工种")).toList();
  }

  public String titleOf(Long workTypeId) {
    Snapshot snapshot = loadSnapshot();
    return requireActiveNode(workTypeId, snapshot, "工种").getTitle();
  }

  private List<Long> resolveAssessorIds(
      String workTypeJson,
      String legacyScope,
      String assessorName,
      Snapshot snapshot
  ) {
    StoredSelection stored = parseStoredSelection(workTypeJson, legacyScope);
    if (!stored.ids().isEmpty()) {
      return stored.ids();
    }
    if (stored.names().isEmpty()) {
      return List.of();
    }
    try {
      return resolveUniqueNames(stored.names(), snapshot, "考评员历史工种");
    } catch (BusinessException exception) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          assessorLabel(assessorName) + exception.getMessage()
              + "；请在考评员信息页编辑档案，并从当前工种树重新选择授权工种。");
    }
  }

  private StoredSelection parseStoredSelection(String workTypeJson, String legacyScope) {
    if (StringUtils.hasText(workTypeJson)) {
      try {
        JsonNode root = JSON.readTree(workTypeJson);
        if (root != null && root.isArray()) {
          List<Long> ids = new ArrayList<>();
          List<String> names = new ArrayList<>();
          for (JsonNode item : root) {
            if (item.isIntegralNumber() && item.canConvertToLong()) {
              ids.add(item.longValue());
            } else if (item.isTextual() && StringUtils.hasText(item.textValue())) {
              names.add(item.textValue().trim());
            } else if (!item.isNull()) {
              throw new BusinessException(ErrorCode.BAD_REQUEST, "工种授权数据格式异常");
            }
          }
          if (!ids.isEmpty() && !names.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工种授权数据格式混用，请编辑档案并重新选择工种");
          }
          if (!ids.isEmpty()) {
            return new StoredSelection(distinctIds(ids), List.of());
          }
          if (!names.isEmpty()) {
            return new StoredSelection(List.of(), parseNames(names));
          }
        }
      } catch (BusinessException exception) {
        throw exception;
      } catch (Exception ignored) {
        // Older deployments may have only the legacy display column populated.
      }
    }
    return new StoredSelection(List.of(), parseNames(Collections.singletonList(legacyScope)));
  }

  private List<String> parseStoredNamesSafely(String workTypeJson, String legacyScope) {
    try {
      StoredSelection stored = parseStoredSelection(workTypeJson, legacyScope);
      return stored.names();
    } catch (RuntimeException ignored) {
      return parseNames(Collections.singletonList(legacyScope));
    }
  }

  private List<Long> resolveUniqueNames(Collection<String> names, Snapshot snapshot, String context) {
    List<String> normalized = parseNames(names);
    if (normalized.isEmpty()) {
      return List.of();
    }
    List<Long> result = new ArrayList<>(normalized.size());
    for (String name : normalized) {
      List<ExamWorkTypeEntity> matches = snapshot.nodes().values().stream()
          .filter(ExamWorkTypeCoverageService::isActive)
          .filter(node -> StringUtils.hasText(node.getTitle()) && node.getTitle().trim().equals(name))
          .toList();
      ExamWorkTypeEntity match;
      if (matches.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            context + "名称“" + name + "”没有匹配到"
                + "；请维护当前租户的工种树或重新选择有效工种。");
      }
      if (matches.size() == 1) {
        match = matches.getFirst();
      } else {
        List<ExamWorkTypeEntity> topLevelMatches = matches.stream()
            .filter(ExamWorkTypeCoverageService::isTopLevel)
            .toList();
        if (topLevelMatches.size() != 1) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              context + "名称“" + name + "”匹配到 " + matches.size()
                  + " 个同名节点，且没有唯一的顶级职业节点可默认选择；请从当前工种树明确选择有效工种。");
        }
        // Legacy records store display names without IDs. When a title is duplicated between a
        // top-level occupation and its descendants, consistently resolve the name to the unique
        // top-level occupation instead of rejecting the whole business flow.
        match = topLevelMatches.getFirst();
      }
      requireActivePath(match.getId().longValue(), snapshot, context);
      result.add(match.getId().longValue());
    }
    return result.stream().distinct().toList();
  }

  private static boolean isTopLevel(ExamWorkTypeEntity node) {
    return node.getParentId() == null || node.getParentId() <= 0;
  }

  private Snapshot loadSnapshot() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (tenantId == null || tenantId > Integer.MAX_VALUE || tenantId < Integer.MIN_VALUE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法确定当前租户，不能校验工种授权");
    }
    List<ExamWorkTypeEntity> rows = mapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .orderByAsc(ExamWorkTypeEntity::getId));
    if (rows == null) {
      rows = Collections.emptyList();
    }
    Map<Integer, ExamWorkTypeEntity> nodes = new HashMap<>();
    for (ExamWorkTypeEntity row : rows) {
      if (row != null
          && row.getId() != null
          && Objects.equals(row.getTenantId(), tenantId.intValue())
          && !Objects.equals(row.getDeleted(), 1)) {
        nodes.put(row.getId(), row);
      }
    }
    return new Snapshot(nodes);
  }

  /** Returns the node ids from the selected node to the root, rejecting cycles and broken/inactive parents. */
  private Set<Integer> requireActivePath(Long id, Snapshot snapshot, String label) {
    ExamWorkTypeEntity node = requireActiveNode(id, snapshot, label);
    Set<Integer> pathIds = new LinkedHashSet<>();
    ExamWorkTypeEntity current = node;
    while (current != null) {
      if (!pathIds.add(current.getId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "工种“" + label + "”的 parent_id 层级存在循环引用，请先修复课程管理工种树");
      }
      Integer parentId = current.getParentId();
      if (parentId == null || parentId <= 0) {
        return pathIds;
      }
      current = snapshot.nodes().get(parentId);
      if (current == null || !isActive(current)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "工种“" + label + "”的上级工种不存在、已删除、已停用或不属于当前租户，请先修复课程管理工种树");
      }
    }
    return pathIds;
  }

  private ExamWorkTypeEntity requireActiveNode(Long id, Snapshot snapshot, String label) {
    ExamWorkTypeEntity node = id == null || id > Integer.MAX_VALUE || id < Integer.MIN_VALUE
        ? null
        : snapshot.nodes().get(id.intValue());
    if (node == null || !isActive(node)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          label + "ID“" + id + "”不存在、已删除、已停用或不属于当前租户，请重新选择当前租户的有效工种");
    }
    return node;
  }

  private String pathOf(Long id, Snapshot snapshot, String label) {
    Set<Integer> path = requireActivePath(id, snapshot, label);
    return path.stream()
        .map(snapshot.nodes()::get)
        .map(ExamWorkTypeEntity::getTitle)
        .filter(StringUtils::hasText)
        .collect(Collectors.collectingAndThen(Collectors.toList(), titles -> {
          Collections.reverse(titles);
          return String.join(PATH_SEPARATOR, titles);
        }));
  }

  private static List<Long> distinctIds(Collection<Long> ids) {
    if (ids == null) {
      return List.of();
    }
    return ids.stream().filter(Objects::nonNull).distinct().toList();
  }

  private static List<String> parseNames(Collection<String> names) {
    if (names == null) {
      return List.of();
    }
    return names.stream()
        .filter(StringUtils::hasText)
        .flatMap(value -> List.of(value.split("[、,，]", -1)).stream())
        .map(String::trim)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
  }

  private static boolean isActive(ExamWorkTypeEntity node) {
    return node != null && Objects.equals(node.getDeleted(), 0) && Objects.equals(node.getStatus(), 1);
  }

  private static String assessorLabel(String assessorName) {
    return StringUtils.hasText(assessorName) ? "考评员“" + assessorName.trim() + "”" : "考评员";
  }

  private record Snapshot(Map<Integer, ExamWorkTypeEntity> nodes) { }

  private record StoredSelection(List<Long> ids, List<String> names) { }

  public record AssessorCoverageResult(boolean covered, String warning) { }

  public record WorkTypeSelection(List<Long> ids, List<String> paths, String warning) { }
}
