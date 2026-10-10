package com.guoyu.platform.competition.sorting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.util.SensitiveMaskUtils;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingGroupsReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingPreparation;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingRolesReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskCreateReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskSelectionReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingWorkstationsReq;
import com.guoyu.platform.competition.sorting.entity.CompetitionPlayerSortingTaskEntity;
import com.guoyu.platform.competition.sorting.mapper.CompetitionPlayerSortingTaskMapper;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingMemberVO;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingRevealVO;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingTaskVO;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingWorkstationVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerSortingApplicationService {

  private static final String PRACTICAL = "practical";
  private static final String CREATED = "created";
  private static final String ORDERED = "ordered";
  private static final String DRAWN = "drawn";
  private static final String SORTED = "sorted";
  private static final String COMPANY = "company";
  private static final String BALANCED = "balanced";
  private static final String DEFAULT_ROLE = "组员";
  private static final int MAX_SELECTED_PLAYER_COUNT = 5000;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<CompetitionPlayerSortingWorkstationVO>> STATIONS =
      new TypeReference<>() {};
  private static final TypeReference<List<CompetitionPlayerSortingAssignmentVO>> ASSIGNMENTS =
      new TypeReference<>() {};
  private static final TypeReference<CompetitionPlayerSortingPreparation> PREPARATION =
      new TypeReference<>() {};

  private final CompetitionStageApplicationService stageService;
  private final CompetitionStagePaperAssignmentApplicationService stagePlayerService;
  private final CompetitionPlayerSortingTaskMapper taskMapper;
  private final CompetitionPlayerSortingWorkbookExportService workbookExportService;
  private final CompetitionPlayerIdentityMapper identityMapper;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerSortingApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStagePaperAssignmentApplicationService stagePlayerService,
      CompetitionPlayerSortingTaskMapper taskMapper,
      CompetitionPlayerSortingWorkbookExportService workbookExportService,
      CompetitionPlayerIdentityMapper identityMapper,
      ObjectMapper objectMapper) {
    this.stageService = stageService;
    this.stagePlayerService = stagePlayerService;
    this.taskMapper = taskMapper;
    this.workbookExportService = workbookExportService;
    this.identityMapper = identityMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStagePaperAssignmentPlayerVO> candidates(Long stageId) {
    stageService.assertCanManage(stageId);
    List<CompetitionStagePaperAssignmentPlayerVO> candidates =
        stagePlayerService.visualization(stageId, PRACTICAL);
    return candidates == null ? List.of() : candidates;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerSortingTaskVO> page(
      Long stageId, long page, long pageSize) {
    stageService.assertCanManage(stageId);
    Page<CompetitionPlayerSortingTaskEntity> result = taskMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<CompetitionPlayerSortingTaskEntity>()
            .eq(CompetitionPlayerSortingTaskEntity::getStageId, stageId)
            .orderByDesc(CompetitionPlayerSortingTaskEntity::getCreatedAt));
    if (result == null) {
      log.warn("Player sorting task page returned null, stageId={}", stageId);
      return PageResponse.empty(page, pageSize);
    }
    List<CompetitionPlayerSortingTaskVO> items = result.getRecords() == null
        ? List.of()
        : result.getRecords().stream().map(entity -> toVO(entity, List.of(), false)).toList();
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO create(
      Long stageId, CompetitionPlayerSortingTaskCreateReq req) {
    CompetitionStageVO stage = stageService.assertCanManage(stageId);
    if (req == null || req.contestantProfileIds() == null || req.contestantProfileIds().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择参加排序的选手");
    }
    validateSelectedIdentities(stageId, req.contestantProfileIds());
    CompetitionPlayerSortingPreparation preparation =
        preparationFor(stageId, req.contestantProfileIds());

    PlatformUserPrincipal principal = currentUser();
    LocalDateTime now = LocalDateTime.now();
    CompetitionPlayerSortingTaskEntity entity = new CompetitionPlayerSortingTaskEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(principal.getTenantId());
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setDeleted(0);
    entity.setEventId(stage.getEventId());
    entity.setStageId(stageId);
    entity.setStageName(stage.getName());
    entity.setGroupCount(0);
    entity.setWorkstationCount(0);
    entity.setPlayersPerGroup(0);
    entity.setPlayerCount(req.contestantProfileIds().size());
    entity.setStatus(CREATED);
    entity.setSelectedProfileIdsJson(writeJson(
        req.contestantProfileIds().stream().map(String::valueOf).toList()));
    entity.setPreparationJson(writeJson(preparation));
    int inserted = taskMapper.insert(entity);
    if (inserted != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务创建失败");
    }
    return toVO(entity, List.of(), true);
  }

  @Transactional
  public void delete(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerSortingTaskEntity>()
            .eq(CompetitionPlayerSortingTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
    stageService.assertCanManage(entity.getStageId());
    if (taskMapper.deleteById(taskId) != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO selectContestants(
      Long taskId, CompetitionPlayerSortingTaskSelectionReq req) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (entity.getPreparationJson() != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "按参赛公司分组的任务请删除后重新选择选手");
    }
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成随机排序的任务不能修改选手");
    }
    if (req == null || req.contestantProfileIds() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择参加排序的选手");
    }
    validateSelectedContestants(entity.getStageId(), req.contestantProfileIds(),
        entity.getGroupCount(), entity.getPlayersPerGroup());

    PlatformUserPrincipal principal = currentUser();
    entity.setSelectedProfileIdsJson(writeJson(
        req.contestantProfileIds().stream().map(String::valueOf).toList()));
    entity.setPlayerCount(req.contestantProfileIds().size());
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务选手保存失败");
    }
    return toVO(entity, List.of(), false);
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO saveGroups(
      Long taskId, CompetitionPlayerSortingGroupsReq req) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus()) || entity.getPreparationJson() == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前任务不能修改分组");
    }
    if (req == null || req.groups() == null || req.groups().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择成员并成组");
    }
    CompetitionPlayerSortingPreparation current = readPreparation(entity.getPreparationJson());
    Map<String, CompetitionPlayerSortingPreparation.Member> memberByProfileId = new HashMap<>();
    for (CompetitionPlayerSortingPreparation.Member member : current.members()) {
      memberByProfileId.put(member.contestantProfileId(), member);
    }
    Set<Long> seen = new HashSet<>();
    Set<String> groupNames = new HashSet<>();
    List<CompetitionPlayerSortingPreparation.Member> rewritten = new ArrayList<>();
    int groupNo = 1;
    int largest = 1;
    for (CompetitionPlayerSortingGroupsReq.Group group : req.groups()) {
      if (group == null || group.groupName() == null || group.contestantProfileIds() == null
          || group.contestantProfileIds().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "每个组都需要组名和至少 1 名选手");
      }
      String groupName = group.groupName().trim();
      if (groupName.isEmpty() || groupName.length() > 32) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组名不能为空，且不能超过 32 个字");
      }
      if (!groupNames.add(groupName)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组名不能重复：" + groupName);
      }
      String groupOrg = null;
      int size = 0;
      for (Long profileId : group.contestantProfileIds()) {
        if (profileId == null || !seen.add(profileId)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "选手不能重复进入多个组");
        }
        CompetitionPlayerSortingPreparation.Member existing =
            memberByProfileId.get(String.valueOf(profileId));
        if (existing == null) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "分组名单与已选选手不一致，请刷新后重试");
        }
        String orgName = CompetitionPlayerSortingPlan.normalizeOrg(existing.orgName());
        if (groupOrg == null) {
          groupOrg = orgName;
        } else if (!groupOrg.equals(orgName)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "同一组只能选择同一参赛公司的选手");
        }
        rewritten.add(new CompetitionPlayerSortingPreparation.Member(
            existing.contestantProfileId(), existing.orgName(), null, groupNo, groupName, null));
        size++;
      }
      largest = Math.max(largest, size);
      groupNo++;
    }
    if (seen.size() != memberByProfileId.size()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "还有选手未成组，请继续选择成员");
    }
    entity.setPreparationJson(writeJson(
        new CompetitionPlayerSortingPreparation(current.roleOptions(), rewritten, false)));
    entity.setGroupCount(req.groups().size());
    entity.setPlayersPerGroup(largest);
    entity.setResultSnapshotJson(null);
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分组保存失败");
    }
    return toVO(entity, List.of(), true);
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO shuffleGroupOrder(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus()) || entity.getPreparationJson() == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "已开始抽签后不能重新排列各组出场顺序");
    }
    CompetitionPlayerSortingPreparation current = readPreparation(entity.getPreparationJson());
    List<Integer> groupNos = new ArrayList<>();
    for (CompetitionPlayerSortingPreparation.Member member : current.members()) {
      if (member.groupNo() == null || member.groupName() == null || member.groupName().isBlank()) {
        throw new BusinessException(ErrorCode.CONFLICT, "请先完成成组，再排列各组出场顺序");
      }
      if (member.drawOrder() != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "已开始组内抽签后不能重新排列各组出场顺序");
      }
      groupNos.add(member.groupNo());
    }
    for (CompetitionPlayerSortingAssignmentVO assignment : readAssignments(entity.getResultSnapshotJson())) {
      if (assignment.workstation() != null && !assignment.workstation().isBlank()) {
        throw new BusinessException(ErrorCode.CONFLICT, "已抽出工位后不能重新排列各组出场顺序");
      }
    }
    Map<Integer, Integer> oldToNew = CompetitionPlayerSortingPlan.shuffleGroupAppearance(groupNos, RANDOM);
    List<CompetitionPlayerSortingPreparation.Member> rewritten = new ArrayList<>();
    for (CompetitionPlayerSortingPreparation.Member member : current.members()) {
      Integer newGroupNo = oldToNew.get(member.groupNo());
      rewritten.add(new CompetitionPlayerSortingPreparation.Member(
          member.contestantProfileId(), member.orgName(), member.role(), newGroupNo,
          member.groupName(), null));
    }
    entity.setPreparationJson(writeJson(
        new CompetitionPlayerSortingPreparation(current.roleOptions(), rewritten, true)));
    entity.setResultSnapshotJson(null);
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "组出场顺序保存失败");
    }
    return toVO(entity, List.of(), true);
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO saveRoles(
      Long taskId, CompetitionPlayerSortingRolesReq req) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus()) || entity.getPreparationJson() == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前任务不能修改组内角色");
    }
    CompetitionPlayerSortingPreparation current = readPreparation(entity.getPreparationJson());
    List<String> roleOptions = normalizeRoleOptions(req == null ? null : req.roleOptions());
    if (req == null || req.members() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请为每名选手选择组内角色");
    }
    Map<Long, String> roleByProfileId = new HashMap<>();
    for (CompetitionPlayerSortingRolesReq.Member member : req.members()) {
      if (member == null || member.contestantProfileId() == null || member.role() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请为每名选手选择组内角色");
      }
      String role = member.role().trim();
      if (!roleOptions.contains(role)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "组内角色必须从已添加的角色中选择");
      }
      if (roleByProfileId.put(member.contestantProfileId(), role) != null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "选手列表中存在重复选手");
      }
    }
    List<CompetitionPlayerSortingPreparation.Member> members = new ArrayList<>();
    for (CompetitionPlayerSortingPreparation.Member member : current.members()) {
      Long profileId = parseId(member.contestantProfileId());
      String role = profileId == null ? null : roleByProfileId.remove(profileId);
      if (role == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "角色名单与已选选手不一致，请刷新后重试");
      }
      if (member.groupNo() == null || member.groupName() == null || member.groupName().isBlank()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为全部选手成组并填写组名");
      }
      members.add(new CompetitionPlayerSortingPreparation.Member(
          member.contestantProfileId(), member.orgName(), role, member.groupNo(), member.groupName(),
          member.drawOrder()));
    }
    if (!roleByProfileId.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "角色名单与已选选手不一致，请刷新后重试");
    }
    entity.setPreparationJson(writeJson(
        new CompetitionPlayerSortingPreparation(roleOptions, members, current.groupOrderShuffled())));
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "组内角色保存失败");
    }
    return toVO(entity, List.of(), true);
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO saveWorkstations(
      Long taskId, CompetitionPlayerSortingWorkstationsReq req) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (entity.getPreparationJson() == null
        || (!CREATED.equals(entity.getStatus()) && !ORDERED.equals(entity.getStatus())
            && !DRAWN.equals(entity.getStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "请先完成成组，再设置工位");
    }
    CompetitionPlayerSortingPreparation preparation = readPreparation(entity.getPreparationJson());
    int groupCount = countPreparationGroups(preparation);
    if (groupCount < 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "请先完成成组，再设置工位");
    }
    requireGroupOrderShuffled(preparation);
    String label = normalizeLabel(req == null ? null : req.label(), "请填写工位描述");
    String subLabel = normalizeLabel(req == null ? null : req.subLabel(), "请填写子工位描述");
    if (label.equals(subLabel)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "子工位描述不能与工位描述相同");
    }
    List<CompetitionPlayerSortingWorkstationVO> stations = normalizeStations(
        req == null ? null : req.stations());
    if (stations.size() < groupCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "已绑定子工位的工位数量不能少于组数，当前共 " + groupCount + " 组");
    }
    entity.setWorkstationLabel(label);
    entity.setWorkstationCodesJson(writeJson(stations));
    entity.setSubWorkstationLabel(subLabel);
    entity.setSubWorkstationCodesJson(null);
    entity.setWorkstationCount(stations.size());
    List<CompetitionPlayerSortingAssignmentVO> assignments =
        readAssignments(entity.getResultSnapshotJson());
    if (!assignments.isEmpty()) {
      entity.setResultSnapshotJson(writeJson(clearWorkstations(assignments)));
    }
    entity.setStatus(allOrdersDrawn(preparation) ? ORDERED : CREATED);
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工位编号保存失败");
    }
    return toVO(entity, withMobile(entity.getStageId(),
        readAssignments(entity.getResultSnapshotJson())), true);
  }

  @Transactional
  public CompetitionPlayerSortingRevealVO drawNextOrder(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTaskForUpdate(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (entity.getPreparationJson() == null || !CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          ORDERED.equals(entity.getStatus()) || DRAWN.equals(entity.getStatus())
              ? "组内排序号已全部抽取" : "请先完成成组，再点击抽签");
    }
    if (!stationsConfigured(entity)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先设置工位，再进行组内抽签");
    }
    CompetitionPlayerSortingPreparation preparation = readPreparation(entity.getPreparationJson());
    requireGroupOrderShuffled(preparation);
    List<CompetitionPlayerSortingPreparation.Member> members = new ArrayList<>(preparation.members());
    Map<Integer, List<Integer>> indexesByGroup = new TreeMap<>();
    for (int index = 0; index < members.size(); index++) {
      CompetitionPlayerSortingPreparation.Member member = members.get(index);
      if (member.groupNo() == null || member.groupName() == null || member.groupName().isBlank()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为全部选手成组并填写组名");
      }
      indexesByGroup.computeIfAbsent(member.groupNo(), key -> new ArrayList<>()).add(index);
    }
    Integer targetGroup = null;
    int targetIndex = -1;
    for (Map.Entry<Integer, List<Integer>> entry : indexesByGroup.entrySet()) {
      for (int index : entry.getValue()) {
        if (members.get(index).drawOrder() == null) {
          targetGroup = entry.getKey();
          targetIndex = index;
          break;
        }
      }
      if (targetIndex >= 0) {
        break;
      }
    }
    if (targetIndex < 0 || targetGroup == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "组内排序号已全部抽取");
    }
    List<Integer> groupIndexes = indexesByGroup.get(targetGroup);
    Set<Integer> used = new HashSet<>();
    for (int index : groupIndexes) {
      Integer drawOrder = members.get(index).drawOrder();
      if (drawOrder != null) {
        used.add(drawOrder);
      }
    }
    int picked = CompetitionPlayerSortingPlan.drawUnusedOrder(groupIndexes.size(), used, RANDOM);
    CompetitionPlayerSortingPreparation.Member current = members.get(targetIndex);
    members.set(targetIndex, new CompetitionPlayerSortingPreparation.Member(
        current.contestantProfileId(), current.orgName(), current.role(),
        current.groupNo(), current.groupName(), picked));
    boolean groupCompleted = true;
    for (int index : groupIndexes) {
      if (members.get(index).drawOrder() == null) {
        groupCompleted = false;
        break;
      }
    }
    boolean allCompleted = members.stream().allMatch(member -> member.drawOrder() != null);
    entity.setPreparationJson(writeJson(
        new CompetitionPlayerSortingPreparation(
            preparation.roleOptions(), members, preparation.groupOrderShuffled())));
    if (groupCompleted) {
      syncCompletedGroups(entity, members);
    }
    List<CompetitionPlayerSortingAssignmentVO> assignments =
        readAssignments(entity.getResultSnapshotJson());
    if (allCompleted) {
      entity.setStatus(allGroupsAssigned(indexesByGroup.keySet(), assignments) ? DRAWN : ORDERED);
    }
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序号抽签保存失败");
    }
    CompetitionStagePaperAssignmentPlayerVO candidate =
        candidateMap(entity.getStageId()).get(parseId(current.contestantProfileId()));
    return new CompetitionPlayerSortingRevealVO(
        toVO(entity, withMobile(entity.getStageId(),
            readAssignments(entity.getResultSnapshotJson())), true),
        current.contestantProfileId(),
        candidate == null ? null : candidate.name(),
        current.groupName(), current.orgName(), picked, null,
        null, null, null, null, groupCompleted, allCompleted);
  }

  @Transactional
  public CompetitionPlayerSortingRevealVO drawNextWorkstation(Long taskId) {
    return drawNextWorkstation(taskId, null);
  }

  @Transactional
  public CompetitionPlayerSortingRevealVO drawNextWorkstation(Long taskId, List<Integer> requestedGroupNos) {
    CompetitionPlayerSortingTaskEntity entity = requireTaskForUpdate(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (DRAWN.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "工位已全部抽取");
    }
    if (entity.getPreparationJson() == null
        || (!CREATED.equals(entity.getStatus()) && !ORDERED.equals(entity.getStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "请先完成成组并设置工位，再抽取工位");
    }
    List<CompetitionPlayerSortingWorkstationVO> stations = readStations(entity.getWorkstationCodesJson());
    if (!stationsConfigured(entity)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先保存工位，并为其绑定子工位");
    }
    CompetitionPlayerSortingPreparation preparation = readPreparation(entity.getPreparationJson());
    requireGroupOrderShuffled(preparation);
    Map<Integer, List<CompetitionPlayerSortingPreparation.Member>> membersByGroup = new TreeMap<>();
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      if (member.groupNo() == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为全部选手成组并填写组名");
      }
      membersByGroup.computeIfAbsent(member.groupNo(), key -> new ArrayList<>()).add(member);
    }
    if (stations.size() < membersByGroup.size()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "已绑定子工位的工位数量不能少于组数，当前共 " + membersByGroup.size() + " 组");
    }
    List<CompetitionPlayerSortingAssignmentVO> current = syncCompletedGroups(
        entity, preparation.members());
    List<Integer> targets = workstationTargets(requestedGroupNos, membersByGroup, current);
    Set<String> usedCodes = new HashSet<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : current) {
      if (assignment.workstation() != null && !assignment.workstation().isBlank()) {
        usedCodes.add(assignment.workstation());
      }
    }
    List<CompetitionPlayerSortingWorkstationVO> remaining = new ArrayList<>();
    for (CompetitionPlayerSortingWorkstationVO station : stations) {
      if (station != null && station.code() != null && !usedCodes.contains(station.code())) {
        remaining.add(station);
      }
    }
    if (remaining.size() < targets.size()) {
      throw new BusinessException(ErrorCode.CONFLICT, "可抽取的工位少于所选组数");
    }
    CompetitionPlayerSortingAssignmentVO sample = null;
    String station = null;
    String subStation = null;
    for (Integer targetGroup : targets) {
      CompetitionPlayerSortingWorkstationVO picked = remaining.remove(RANDOM.nextInt(remaining.size()));
      if (picked.subCodes() == null || picked.subCodes().isEmpty()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "工位 " + picked.code() + " 未绑定子工位");
      }
      station = picked.code();
      subStation = String.join("、", picked.subCodes());
      boolean targetFound = false;
      for (int index = 0; index < current.size(); index++) {
        CompetitionPlayerSortingAssignmentVO assignment = current.get(index);
        if (assignment.groupNo() != targetGroup) {
          continue;
        }
        targetFound = true;
        sample = assignment;
        current.set(index, copyStations(assignment, station, subStation));
      }
      if (!targetFound) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "该组的组内抽签结果不完整，请刷新后重试");
      }
    }
    boolean allCompleted = allGroupsAssigned(membersByGroup.keySet(), current);
    entity.setResultSnapshotJson(writeJson(current));
    if (allCompleted) {
      entity.setStatus(DRAWN);
    } else if (allOrdersDrawn(preparation)) {
      entity.setStatus(ORDERED);
    } else {
      entity.setStatus(CREATED);
    }
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工位抽签结果保存失败");
    }
    return new CompetitionPlayerSortingRevealVO(
        toVO(entity, withMobile(entity.getStageId(), current), true),
        null, null,
        sample == null ? null : sample.groupName(),
        sample == null ? null : sample.orgName(),
        null, null,
        entity.getWorkstationLabel(), station,
        entity.getSubWorkstationLabel(), subStation,
        true, allCompleted);
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO randomize(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    CompetitionStageVO stage = stageService.assertCanManage(entity.getStageId());
    if (!stage.getEventId().equals(entity.getEventId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "排序任务与赛段不匹配");
    }
    if (entity.getPreparationJson() != null) {
      return randomizeCompanyGroups(entity);
    }
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该排序任务已完成抽签，请新建任务重新抽签");
    }

    List<Long> selectedIds = readSelectedIds(entity.getSelectedProfileIdsJson());
    int maximumPlayerCount = Math.min(
        Math.multiplyExact(entity.getGroupCount(), entity.getPlayersPerGroup()),
        MAX_SELECTED_PLAYER_COUNT);
    if (selectedIds.size() != entity.getPlayerCount()
        || selectedIds.size() < entity.getGroupCount()
        || selectedIds.size() > maximumPlayerCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "排序任务选手人数不符合配置，请重新选择选手");
    }
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById =
        candidateMap(entity.getStageId());
    List<CompetitionStagePaperAssignmentPlayerVO> selected = new ArrayList<>();
    for (Long selectedId : selectedIds) {
      CompetitionStagePaperAssignmentPlayerVO candidate = candidateById.get(selectedId);
      if (candidate == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "有选手已不属于当前赛段，请重新创建排序任务");
      }
      selected.add(candidate);
    }

    if (entity.getWorkstationCount() == null || entity.getPlayersPerGroup() == null
        || entity.getWorkstationCount() < 1
        || entity.getWorkstationCount() > entity.getPlayersPerGroup()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "工位数量必须大于零且不能大于每组人数上限，请重新创建排序任务");
    }
    int workstationCount = entity.getWorkstationCount();
    Collections.shuffle(selected, RANDOM);
    Map<Long, String> mobileByProfileId = maskedMobileByProfileId(selected, new HashSet<>(selectedIds));
    List<CompetitionPlayerSortingAssignmentVO> assignments = new ArrayList<>(selected.size());
    int baseGroupSize = selected.size() / entity.getGroupCount();
    int largerGroupCount = selected.size() % entity.getGroupCount();
    int selectedIndex = 0;
    for (int groupNo = 1; groupNo <= entity.getGroupCount(); groupNo++) {
      int groupSize = baseGroupSize + (groupNo <= largerGroupCount ? 1 : 0);
      for (int groupOrder = 1; groupOrder <= groupSize; groupOrder++) {
        CompetitionStagePaperAssignmentPlayerVO candidate = selected.get(selectedIndex++);
        assignments.add(new CompetitionPlayerSortingAssignmentVO(
            candidate.contestantProfileId(), candidate.contestantNo(), candidate.name(),
            mobileByProfileId.get(parseId(candidate.contestantProfileId())),
            groupNo, groupOrder,
            CompetitionPlayerSortingWorkstations.assign(groupOrder, workstationCount),
            null, null, null, null, null));
      }
    }

    PlatformUserPrincipal principal = currentUser();
    entity.setResultSnapshotJson(writeJson(assignments));
    entity.setStatus(SORTED);
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "随机排序结果保存失败");
    }
    return toVO(entity, assignments, false);
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerSortingTaskVO get(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    return toVO(entity, withMobile(entity.getStageId(),
        readAssignments(entity.getResultSnapshotJson())), true);
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!SORTED.equals(entity.getStatus()) && !ORDERED.equals(entity.getStatus())
        && !DRAWN.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先完成随机排序再导出名单");
    }
    List<CompetitionPlayerSortingAssignmentVO> assignments = withMobile(entity.getStageId(),
        readAssignments(entity.getResultSnapshotJson()));
    if (assignments.size() != entity.getPlayerCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "排序结果不完整，无法导出名单");
    }
    return workbookExportService.export(
        entity.getStageName(), assignments, entity.getGroupCount(),
        entity.getWorkstationLabel(), stationCodes(readStations(entity.getWorkstationCodesJson())),
        entity.getSubWorkstationLabel());
  }

  private Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateMap(Long stageId) {
    List<CompetitionStagePaperAssignmentPlayerVO> candidates = candidates(stageId);
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> result = new HashMap<>();
    for (CompetitionStagePaperAssignmentPlayerVO candidate : candidates) {
      if (candidate == null || candidate.contestantProfileId() == null) {
        continue;
      }
      try {
        result.put(Long.valueOf(candidate.contestantProfileId()), candidate);
      } catch (NumberFormatException ex) {
        log.warn("Ignoring stage candidate with an invalid profile ID, stageId={}", stageId);
      }
    }
    return result;
  }

  /** 把名单行的手机号按选手档案批量回填（脱敏），旧快照与新快照统一以 identity 表当前值为准。 */
  private List<CompetitionPlayerSortingAssignmentVO> withMobile(
      Long stageId, List<CompetitionPlayerSortingAssignmentVO> assignments) {
    if (assignments == null || assignments.isEmpty()) {
      return assignments;
    }
    Set<Long> profileIds = new HashSet<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      Long profileId = parseId(assignment.contestantProfileId());
      if (profileId != null) {
        profileIds.add(profileId);
      }
    }
    Map<Long, String> mobileByProfileId = maskedMobileByProfileId(candidates(stageId), profileIds);
    if (mobileByProfileId.isEmpty()) {
      return assignments;
    }
    List<CompetitionPlayerSortingAssignmentVO> result = new ArrayList<>(assignments.size());
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      Long profileId = parseId(assignment.contestantProfileId());
      result.add(new CompetitionPlayerSortingAssignmentVO(
          assignment.contestantProfileId(), assignment.contestantNo(), assignment.name(),
          profileId == null ? null : mobileByProfileId.get(profileId),
          assignment.groupNo(), assignment.groupOrder(), assignment.workstation(),
          assignment.groupLeader(), assignment.orgName(), assignment.groupName(),
          assignment.role(), assignment.subWorkstation()));
    }
    return result;
  }

  /** 档案 ID → 脱敏手机号。手机号来源 competition_player_identity，经候选行的 playerId 关联。 */
  private Map<Long, String> maskedMobileByProfileId(
      List<CompetitionStagePaperAssignmentPlayerVO> candidates, Set<Long> profileIds) {
    if (candidates == null || candidates.isEmpty() || profileIds == null || profileIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> playerIdByProfileId = new HashMap<>();
    for (CompetitionStagePaperAssignmentPlayerVO candidate : candidates) {
      if (candidate == null || candidate.contestantProfileId() == null || candidate.playerId() == null) {
        continue;
      }
      Long profileId = parseId(candidate.contestantProfileId());
      if (profileId == null || !profileIds.contains(profileId)) {
        continue;
      }
      Long playerId = parseId(candidate.playerId());
      if (playerId != null) {
        playerIdByProfileId.put(profileId, playerId);
      }
    }
    if (playerIdByProfileId.isEmpty()) {
      return Map.of();
    }
    List<Long> playerIds = playerIdByProfileId.values().stream().distinct().toList();
    Map<Long, String> mobileByPlayerId = new HashMap<>();
    final int chunk = 500;
    for (int i = 0; i < playerIds.size(); i += chunk) {
      List<Long> part = playerIds.subList(i, Math.min(i + chunk, playerIds.size()));
      List<CompetitionPlayerIdentityEntity> rows = identityMapper.selectBatchIds(part);
      if (rows == null) {
        continue;
      }
      for (CompetitionPlayerIdentityEntity row : rows) {
        if (row != null && row.getId() != null
            && row.getMobile() != null && !row.getMobile().isBlank()) {
          mobileByPlayerId.put(row.getId(), SensitiveMaskUtils.maskPhone(row.getMobile()));
        }
      }
    }
    Map<Long, String> result = new HashMap<>();
    for (Map.Entry<Long, Long> entry : playerIdByProfileId.entrySet()) {
      String mobile = mobileByPlayerId.get(entry.getValue());
      if (mobile != null) {
        result.put(entry.getKey(), mobile);
      }
    }
    return result;
  }

  private Long parseId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private void validateSelectedContestants(Long stageId, List<Long> contestantProfileIds,
      int groupCount, int playersPerGroup) {
    int minimumCount = groupCount;
    int maximumCount = Math.min(
        Math.multiplyExact(groupCount, playersPerGroup), MAX_SELECTED_PLAYER_COUNT);
    if (contestantProfileIds.size() < minimumCount || contestantProfileIds.size() > maximumCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "已选人数需在 " + minimumCount + " 至 " + maximumCount + " 人之间（每组至少 1 人，"
              + "每组最多 " + playersPerGroup + " 人），当前已选 " + contestantProfileIds.size() + " 人");
    }
    Set<Long> selectedIds = new HashSet<>(contestantProfileIds);
    if (selectedIds.size() != contestantProfileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手列表中存在重复选手");
    }
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(stageId);
    if (!candidateById.keySet().containsAll(selectedIds)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选选手中包含不属于当前赛段的选手，请刷新名单后重试");
    }
  }

  private List<CompetitionPlayerSortingAssignmentVO> materializeDraw(
      CompetitionPlayerSortingTaskEntity entity,
      List<CompetitionPlayerSortingPreparation.Member> members) {
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(entity.getStageId());
    Set<Long> profileIds = new HashSet<>();
    for (CompetitionPlayerSortingPreparation.Member member : members) {
      Long profileId = parseId(member.contestantProfileId());
      if (profileId != null) {
        profileIds.add(profileId);
      }
    }
    Map<Long, String> mobileByProfileId = maskedMobileByProfileId(
        new ArrayList<>(candidateById.values()), profileIds);
    List<CompetitionPlayerSortingAssignmentVO> assignments = new ArrayList<>();
    for (CompetitionPlayerSortingPreparation.Member member : members) {
      CompetitionStagePaperAssignmentPlayerVO candidate =
          candidateById.get(parseId(member.contestantProfileId()));
      if (candidate == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "有选手已不属于当前赛段，请重新创建排序任务");
      }
      int drawOrder = member.drawOrder() == null ? 0 : member.drawOrder();
      assignments.add(new CompetitionPlayerSortingAssignmentVO(
          member.contestantProfileId(), candidate.contestantNo(), candidate.name(),
          mobileByProfileId.get(parseId(member.contestantProfileId())),
          member.groupNo(), drawOrder, null, null,
          member.orgName(), member.groupName(), null, null));
    }
    assignments.sort(java.util.Comparator
        .comparingInt(CompetitionPlayerSortingAssignmentVO::groupNo)
        .thenComparingInt(CompetitionPlayerSortingAssignmentVO::groupOrder));
    return assignments;
  }

  private CompetitionPlayerSortingTaskEntity requireTaskForUpdate(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerSortingTaskEntity>()
            .eq(CompetitionPlayerSortingTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
    return entity;
  }

  private CompetitionPlayerSortingTaskEntity requireTask(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = taskMapper.selectById(taskId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
    return entity;
  }

  private List<Long> readSelectedIds(String json) {
    try {
      return objectMapper.readValue(json, STRING_LIST).stream().map(Long::valueOf).toList();
    } catch (Exception ex) {
      log.error("Failed to read player sorting task selected IDs");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务选手数据损坏");
    }
  }

  private CompetitionPlayerSortingPreparation readPreparation(String json) {
    CompetitionPlayerSortingPreparation preparation = readPreparationOrNull(json);
    if (preparation == null || preparation.members() == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务分组数据损坏");
    }
    return preparation;
  }

  private CompetitionPlayerSortingPreparation readPreparationOrNull(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, PREPARATION);
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player sorting preparation");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务分组数据损坏");
    }
  }

  private List<CompetitionPlayerSortingWorkstationVO> readStations(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      if (root == null || !root.isArray() || root.isEmpty()) {
        return List.of();
      }
      if (root.get(0).isTextual()) {
        List<String> codes = objectMapper.convertValue(root, STRING_LIST);
        if (codes == null) {
          return List.of();
        }
        List<CompetitionPlayerSortingWorkstationVO> stations = new ArrayList<>();
        for (String code : codes) {
          stations.add(new CompetitionPlayerSortingWorkstationVO(code, List.of()));
        }
        return stations;
      }
      List<CompetitionPlayerSortingWorkstationVO> stations = objectMapper.convertValue(root, STATIONS);
      return stations == null ? List.of() : stations;
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.error("Failed to read player sorting workstation stations");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工位编号数据损坏");
    }
  }

  private List<CompetitionPlayerSortingWorkstationVO> normalizeStations(
      List<CompetitionPlayerSortingWorkstationsReq.Station> stations) {
    if (stations == null || stations.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少添加一个工位，并绑定子工位");
    }
    Set<String> codes = new HashSet<>();
    List<CompetitionPlayerSortingWorkstationVO> normalized = new ArrayList<>();
    for (CompetitionPlayerSortingWorkstationsReq.Station station : stations) {
      if (station == null || station.code() == null || station.code().isBlank()
          || station.subCodes() == null || station.subCodes().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "每个工位都要填写编号，并至少绑定一个子工位");
      }
      String code = station.code().trim();
      if (code.length() > 32) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工位编号不能超过 32 个字符");
      }
      if (!codes.add(code)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工位编号不能重复：" + code);
      }
      List<String> bound = new ArrayList<>();
      for (String subCode : station.subCodes()) {
        if (subCode == null || subCode.isBlank()) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "子工位编号不能为空");
        }
        String normalizedSubCode = subCode.trim();
        if (normalizedSubCode.length() > 32) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "子工位编号不能超过 32 个字符");
        }
        if (!bound.contains(normalizedSubCode)) {
          bound.add(normalizedSubCode);
        }
      }
      normalized.add(new CompetitionPlayerSortingWorkstationVO(code, List.copyOf(bound)));
    }
    return List.copyOf(normalized);
  }

  private List<String> stationCodes(List<CompetitionPlayerSortingWorkstationVO> stations) {
    List<String> codes = new ArrayList<>();
    if (stations == null) {
      return codes;
    }
    for (CompetitionPlayerSortingWorkstationVO station : stations) {
      if (station != null && station.code() != null && !station.code().isBlank()) {
        codes.add(station.code());
      }
    }
    return codes;
  }

  private List<String> readCodes(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> codes = objectMapper.readValue(json, STRING_LIST);
      return codes == null ? List.of() : codes;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player sorting workstation codes");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工位编号数据损坏");
    }
  }

  private List<CompetitionPlayerSortingAssignmentVO> readAssignments(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<CompetitionPlayerSortingAssignmentVO> result =
          objectMapper.readValue(json, ASSIGNMENTS);
      return result == null ? List.of() : result;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player sorting task result snapshot");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序结果数据损坏");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize player sorting task data");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务数据保存失败");
    }
  }

  private CompetitionPlayerSortingTaskVO randomizeCompanyGroups(
      CompetitionPlayerSortingTaskEntity entity) {
    if (!CREATED.equals(entity.getStatus()) && !ORDERED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成工位抽签的任务不能重新排序，请删除后重建");
    }
    CompetitionPlayerSortingPreparation preparation = readPreparation(entity.getPreparationJson());
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      if (member.role() == null || member.role().isBlank()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为每名选手选择组内角色");
      }
    }
    List<Long> selectedIds = readSelectedIds(entity.getSelectedProfileIdsJson());
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById =
        candidateMap(entity.getStageId());
    Map<Long, String> mobileByProfileId = maskedMobileByProfileId(
        new ArrayList<>(candidateById.values()), new HashSet<>(selectedIds));
    List<CompetitionPlayerSortingPlan.CompanyGroup> groups = groupsOf(preparation);
    List<CompetitionPlayerSortingAssignmentVO> assignments = new ArrayList<>();
    Map<String, CompetitionPlayerSortingPreparation.Member> memberByProfileId = new HashMap<>();
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      memberByProfileId.put(member.contestantProfileId(), member);
    }
    for (CompetitionPlayerSortingPlan.CompanyGroup group : groups) {
      List<String> profileIds = new ArrayList<>(group.profileIds());
      Collections.shuffle(profileIds, RANDOM);
      int groupOrder = 1;
      for (String profileId : profileIds) {
        CompetitionStagePaperAssignmentPlayerVO candidate =
            candidateById.get(parseId(profileId));
        if (candidate == null) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
              "有选手已不属于当前赛段，请重新创建排序任务");
        }
        CompetitionPlayerSortingPreparation.Member member = memberByProfileId.get(profileId);
        assignments.add(new CompetitionPlayerSortingAssignmentVO(
            profileId, candidate.contestantNo(), candidate.name(),
            mobileByProfileId.get(parseId(profileId)),
            group.groupNo(), groupOrder, null, null,
            member == null ? group.orgName() : member.orgName(), group.groupName(),
            member == null ? null : member.role(), null));
        groupOrder++;
      }
    }
    entity.setResultSnapshotJson(writeJson(assignments));
    entity.setStatus(ORDERED);
    touch(entity);
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "随机排序结果保存失败");
    }
    return toVO(entity, assignments, true);
  }

  private CompetitionPlayerSortingPreparation preparationFor(
      Long stageId, List<Long> contestantProfileIds) {
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(stageId);
    List<CompetitionPlayerSortingPreparation.Member> members = new ArrayList<>();
    for (Long profileId : contestantProfileIds) {
      CompetitionStagePaperAssignmentPlayerVO candidate = candidateById.get(profileId);
      members.add(new CompetitionPlayerSortingPreparation.Member(
          String.valueOf(profileId),
          CompetitionPlayerSortingPlan.normalizeOrg(candidate == null ? null : candidate.orgName()),
          null, null, null, null));
    }
    return new CompetitionPlayerSortingPreparation(List.of(DEFAULT_ROLE), members);
  }

  private List<CompetitionPlayerSortingPlan.CompanyGroup> groupsOf(
      CompetitionPlayerSortingPreparation preparation) {
    Map<Integer, List<String>> profileIdsByGroup = new LinkedHashMap<>();
    Map<Integer, String> nameByGroup = new HashMap<>();
    Map<Integer, String> orgByGroup = new HashMap<>();
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      if (member.groupNo() == null || member.groupName() == null || member.groupName().isBlank()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先为全部选手成组并填写组名");
      }
      profileIdsByGroup.computeIfAbsent(member.groupNo(), key -> new ArrayList<>())
          .add(member.contestantProfileId());
      nameByGroup.putIfAbsent(member.groupNo(), member.groupName());
      orgByGroup.putIfAbsent(member.groupNo(), member.orgName());
    }
    List<CompetitionPlayerSortingPlan.CompanyGroup> groups = new ArrayList<>();
    for (Map.Entry<Integer, List<String>> entry : profileIdsByGroup.entrySet()) {
      groups.add(new CompetitionPlayerSortingPlan.CompanyGroup(
          entry.getKey(), orgByGroup.get(entry.getKey()), List.copyOf(entry.getValue()),
          nameByGroup.get(entry.getKey())));
    }
    return groups;
  }

  private void validateSelectedIdentities(Long stageId, List<Long> contestantProfileIds) {
    if (contestantProfileIds.size() > MAX_SELECTED_PLAYER_COUNT) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "单次最多选择 5000 名选手");
    }
    Set<Long> selectedIds = new HashSet<>(contestantProfileIds);
    if (selectedIds.size() != contestantProfileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手列表中存在重复选手");
    }
    if (!candidateMap(stageId).keySet().containsAll(selectedIds)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选选手中包含不属于当前赛段的选手，请刷新名单后重试");
    }
  }

  private List<String> normalizeRoleOptions(List<String> roleOptions) {
    if (roleOptions == null || roleOptions.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少添加一个组内角色");
    }
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String roleOption : roleOptions) {
      if (roleOption == null || roleOption.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组内角色不能为空");
      }
      String role = roleOption.trim();
      if (role.length() > 16) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组内角色不能超过 16 个字");
      }
      unique.add(role);
    }
    if (unique.size() > 20) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "组内角色最多 20 个");
    }
    return List.copyOf(unique);
  }

  private String normalizeLabel(String label, String emptyMessage) {
    if (label == null || label.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, emptyMessage);
    }
    String normalized = label.trim();
    if (normalized.length() > 16) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "工位描述不能超过 16 个字");
    }
    return normalized;
  }

  private List<String> normalizeCodes(List<String> codes, String label) {
    if (codes == null || codes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少创建一个" + label + "编号");
    }
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String code : codes) {
      if (code == null || code.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, label + "编号不能为空");
      }
      String normalized = code.trim();
      if (normalized.length() > 32) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, label + "编号不能超过 32 个字符");
      }
      if (!unique.add(normalized)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, label + "编号不能重复：" + normalized);
      }
    }
    if (unique.size() > 500) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "编号最多 500 个");
    }
    return List.copyOf(unique);
  }

  private int countPreparationGroups(CompetitionPlayerSortingPreparation preparation) {
    Set<Integer> groupNos = new HashSet<>();
    if (preparation == null || preparation.members() == null) {
      return 0;
    }
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      if (member.groupNo() != null && member.groupName() != null && !member.groupName().isBlank()) {
        groupNos.add(member.groupNo());
      }
    }
    return groupNos.size();
  }

  private boolean allOrdersDrawn(CompetitionPlayerSortingPreparation preparation) {
    if (preparation == null || preparation.members() == null || preparation.members().isEmpty()) {
      return false;
    }
    for (CompetitionPlayerSortingPreparation.Member member : preparation.members()) {
      if (member.groupNo() == null || member.drawOrder() == null) {
        return false;
      }
    }
    return true;
  }

  private boolean stationsConfigured(CompetitionPlayerSortingTaskEntity entity) {
    if (entity.getWorkstationLabel() == null || entity.getWorkstationLabel().isBlank()
        || entity.getSubWorkstationLabel() == null || entity.getSubWorkstationLabel().isBlank()) {
      return false;
    }
    List<CompetitionPlayerSortingWorkstationVO> stations = readStations(entity.getWorkstationCodesJson());
    if (stations.isEmpty()) {
      return false;
    }
    for (CompetitionPlayerSortingWorkstationVO station : stations) {
      if (station == null || station.code() == null || station.code().isBlank()
          || station.subCodes() == null || station.subCodes().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /** 把已经完成组内抽签的小组写入名单快照，已有工位结果保持不变。 */
  private List<CompetitionPlayerSortingAssignmentVO> syncCompletedGroups(
      CompetitionPlayerSortingTaskEntity entity,
      List<CompetitionPlayerSortingPreparation.Member> members) {
    List<CompetitionPlayerSortingAssignmentVO> existing =
        new ArrayList<>(readAssignments(entity.getResultSnapshotJson()));
    Set<Integer> present = new HashSet<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : existing) {
      present.add(assignment.groupNo());
    }
    Map<Integer, List<CompetitionPlayerSortingPreparation.Member>> byGroup = new TreeMap<>();
    for (CompetitionPlayerSortingPreparation.Member member : members) {
      if (member.groupNo() == null) {
        continue;
      }
      byGroup.computeIfAbsent(member.groupNo(), key -> new ArrayList<>()).add(member);
    }
    List<CompetitionPlayerSortingPreparation.Member> missing = new ArrayList<>();
    for (Map.Entry<Integer, List<CompetitionPlayerSortingPreparation.Member>> entry : byGroup.entrySet()) {
      boolean complete = true;
      for (CompetitionPlayerSortingPreparation.Member member : entry.getValue()) {
        if (member.drawOrder() == null) {
          complete = false;
          break;
        }
      }
      if (complete && !present.contains(entry.getKey())) {
        missing.addAll(entry.getValue());
      }
    }
    if (missing.isEmpty()) {
      return existing;
    }
    existing.addAll(materializeDraw(entity, missing));
    existing.sort(java.util.Comparator
        .comparingInt(CompetitionPlayerSortingAssignmentVO::groupNo)
        .thenComparingInt(CompetitionPlayerSortingAssignmentVO::groupOrder));
    entity.setResultSnapshotJson(writeJson(existing));
    return existing;
  }

  private boolean allGroupsAssigned(
      Set<Integer> groupNos, List<CompetitionPlayerSortingAssignmentVO> assignments) {
    if (groupNos == null || groupNos.isEmpty()) {
      return false;
    }
    Set<Integer> assigned = new HashSet<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      if (assignment.workstation() != null && !assignment.workstation().isBlank()
          && assignment.subWorkstation() != null && !assignment.subWorkstation().isBlank()) {
        assigned.add(assignment.groupNo());
      }
    }
    return assigned.containsAll(groupNos);
  }

  private List<Integer> workstationTargets(
      List<Integer> requestedGroupNos,
      Map<Integer, List<CompetitionPlayerSortingPreparation.Member>> membersByGroup,
      List<CompetitionPlayerSortingAssignmentVO> current) {
    if (requestedGroupNos == null || requestedGroupNos.isEmpty()) {
      CompetitionPlayerSortingPlan.WorkstationDrawChoice choice = chooseWorkstationGroup(
          membersByGroup, current);
      if (choice.gate() == CompetitionPlayerSortingPlan.WorkstationDrawGate.NEED_GROUP_ORDER) {
        throw new BusinessException(ErrorCode.CONFLICT, "请先完成至少一组的组内抽签，再抽取工位");
      }
      if (choice.gate() == CompetitionPlayerSortingPlan.WorkstationDrawGate.WAIT_FOR_REMAINING_GROUPS) {
        throw new BusinessException(ErrorCode.CONFLICT, "已完成组内抽签的小组都已抽取工位，请先完成其余小组的组内抽签");
      }
      if (choice.gate() != CompetitionPlayerSortingPlan.WorkstationDrawGate.READY || choice.groupNo() == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "工位已全部抽取");
      }
      return List.of(choice.groupNo());
    }
    LinkedHashSet<Integer> unique = new LinkedHashSet<>();
    for (Integer groupNo : requestedGroupNos) {
      if (groupNo == null || !unique.add(groupNo)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要抽取工位的小组，且不能重复");
      }
      List<CompetitionPlayerSortingPreparation.Member> members = membersByGroup.get(groupNo);
      if (members == null || members.isEmpty()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选小组不存在，请刷新后重试");
      }
      for (CompetitionPlayerSortingPreparation.Member member : members) {
        if (member.drawOrder() == null) {
          String groupName = member.groupName() == null || member.groupName().isBlank()
              ? "第" + groupNo + "组" : member.groupName();
          throw new BusinessException(ErrorCode.CONFLICT, groupName + "尚未完成组内抽签，不能抽取工位");
        }
      }
      for (CompetitionPlayerSortingAssignmentVO assignment : current) {
        if (assignment.groupNo() == groupNo
            && assignment.workstation() != null
            && !assignment.workstation().isBlank()) {
          String groupName = assignment.groupName() == null || assignment.groupName().isBlank()
              ? "第" + groupNo + "组" : assignment.groupName();
          throw new BusinessException(ErrorCode.CONFLICT, groupName + "已经抽取工位");
        }
      }
    }
    return List.copyOf(unique);
  }

  private CompetitionPlayerSortingPlan.WorkstationDrawChoice chooseWorkstationGroup(
      Map<Integer, List<CompetitionPlayerSortingPreparation.Member>> membersByGroup,
      List<CompetitionPlayerSortingAssignmentVO> assignments) {
    Set<Integer> orderCompleted = new HashSet<>();
    for (Map.Entry<Integer, List<CompetitionPlayerSortingPreparation.Member>> entry
        : membersByGroup.entrySet()) {
      boolean complete = true;
      for (CompetitionPlayerSortingPreparation.Member member : entry.getValue()) {
        if (member.drawOrder() == null) {
          complete = false;
          break;
        }
      }
      if (complete) {
        orderCompleted.add(entry.getKey());
      }
    }
    Set<Integer> workstationAssigned = new HashSet<>();
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      if (assignment.workstation() != null && !assignment.workstation().isBlank()
          && assignment.subWorkstation() != null && !assignment.subWorkstation().isBlank()) {
        workstationAssigned.add(assignment.groupNo());
      }
    }
    return CompetitionPlayerSortingPlan.chooseWorkstationGroup(
        new ArrayList<>(membersByGroup.keySet()), orderCompleted, workstationAssigned);
  }

  private List<CompetitionPlayerSortingAssignmentVO> clearWorkstations(
      List<CompetitionPlayerSortingAssignmentVO> assignments) {
    List<CompetitionPlayerSortingAssignmentVO> cleared = new ArrayList<>(assignments.size());
    for (CompetitionPlayerSortingAssignmentVO assignment : assignments) {
      cleared.add(copyStations(assignment, null, null));
    }
    return cleared;
  }

  private CompetitionPlayerSortingAssignmentVO copyStations(
      CompetitionPlayerSortingAssignmentVO assignment, String workstation, String subWorkstation) {
    return new CompetitionPlayerSortingAssignmentVO(
        assignment.contestantProfileId(), assignment.contestantNo(), assignment.name(),
        assignment.mobile(), assignment.groupNo(), assignment.groupOrder(), workstation,
        assignment.groupLeader(), assignment.orgName(), assignment.groupName(),
        assignment.role(), subWorkstation);
  }

  private void touch(CompetitionPlayerSortingTaskEntity entity) {
    PlatformUserPrincipal principal = currentUser();
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
  }

  private CompetitionPlayerSortingTaskVO toVO(
      CompetitionPlayerSortingTaskEntity entity,
      List<CompetitionPlayerSortingAssignmentVO> assignments,
      boolean includeMembers) {
    CompetitionPlayerSortingPreparation preparation = readPreparationOrNull(entity.getPreparationJson());
    List<CompetitionPlayerSortingMemberVO> members = List.of();
    List<String> roleOptions = List.of();
    if (preparation != null) {
      roleOptions = preparation.roleOptions() == null ? List.of() : preparation.roleOptions();
      if (includeMembers && preparation.members() != null) {
        members = preparation.members().stream()
            .map(member -> new CompetitionPlayerSortingMemberVO(
                member.contestantProfileId(), member.orgName(), member.role(),
                member.groupNo(), member.groupName(), member.drawOrder()))
            .toList();
      }
    }
    List<CompetitionPlayerSortingWorkstationVO> stations = readStations(entity.getWorkstationCodesJson());
    return new CompetitionPlayerSortingTaskVO(
        String.valueOf(entity.getId()), String.valueOf(entity.getEventId()),
        String.valueOf(entity.getStageId()), entity.getStageName(), entity.getGroupCount(),
        entity.getWorkstationCount(), entity.getPlayersPerGroup(), entity.getPlayerCount(),
        entity.getStatus(), entity.getCreatedAt(), assignments,
        preparation == null ? BALANCED : COMPANY,
        entity.getWorkstationLabel(), stationCodes(stations),
        roleOptions, members, entity.getSubWorkstationLabel(),
        List.of(), stations,
        preparation != null && Boolean.TRUE.equals(preparation.groupOrderShuffled()));
  }

  private void requireGroupOrderShuffled(CompetitionPlayerSortingPreparation preparation) {
    if (preparation == null || !Boolean.TRUE.equals(preparation.groupOrderShuffled())) {
      throw new BusinessException(ErrorCode.CONFLICT, "请先随机排列各组的出场顺序，再设置工位");
    }
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null || principal.getTenantId() == null || principal.getUserId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户身份无效");
    }
    return principal;
  }
}
