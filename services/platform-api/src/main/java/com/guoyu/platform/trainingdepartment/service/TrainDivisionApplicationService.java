package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionAutoPackReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionEnsureTrainInfoReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionOrgSplitReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionRosterParseReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanItemEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionPlanVO;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionReadyVO;
import com.guoyu.platform.trainingdepartment.vo.TrainPlanRosterVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TrainDivisionApplicationService {

  @Value("${platform.training.division.require-roster:true}")
  private boolean requireRoster;

  private static final Map<Integer, String> STATUS_LABELS = Map.of(
      0, "草稿",
      1, "分析中",
      2, "分析完成请审核",
      3, "待机构审核",
      4, "企业已确认",
      5, "已驳回",
      6, "已作废"
  );

  private final TrainThreeYearPlanMapper planMapper;
  private final TrainThreeYearPlanItemMapper itemMapper;
  private final TrainInfoListMapper trainInfoListMapper;
  private final TrainInfoClassMapper trainInfoClassMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainPlanRosterMapper rosterMapper;
  private final OrgMapper orgMapper;
  private final FileApplicationService fileApplicationService;
  private final TrainPlanRosterParseService rosterParseService;
  private final TrainingOperatorScopeService scopeService;
  private final TrainShiftProfileService shiftProfileService;

  @Transactional(readOnly = true)
  public PageResponse<TrainDivisionPlanVO> pagePlans(int page, int pageSize, String keyword, Boolean readyOnly) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long companyFilter = scopeService.resolveCompanyFilter(null);

    LambdaQueryWrapper<TrainThreeYearPlanEntity> wrapper = new LambdaQueryWrapper<TrainThreeYearPlanEntity>()
        .eq(TrainThreeYearPlanEntity::getTenantId, tenantId)
        .eq(TrainThreeYearPlanEntity::getStatus, 4)
        .orderByDesc(TrainThreeYearPlanEntity::getUpdateTime);
    if (companyFilter != null) {
      wrapper.eq(TrainThreeYearPlanEntity::getTargetOrgId, companyFilter);
    }
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(TrainThreeYearPlanEntity::getTitle, keyword.trim())
          .or().like(TrainThreeYearPlanEntity::getPlanNo, keyword.trim()));
    }
    if (Boolean.TRUE.equals(readyOnly)) {
      wrapper.isNotNull(TrainThreeYearPlanEntity::getEnterpriseShiftFileId);
      if (requireRoster) {
        wrapper.isNotNull(TrainThreeYearPlanEntity::getEnterpriseRosterFileId);
      }
    }

    Page<TrainThreeYearPlanEntity> result = planMapper.selectPage(new Page<>(page, pageSize), wrapper);
    List<TrainDivisionPlanVO> list = result.getRecords().stream().map(this::toPlanVO).toList();
    return new PageResponse<>(list, page, pageSize, result.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainDivisionReadyVO ready(Long planId) {
    TrainThreeYearPlanEntity plan = requirePlan(planId);
    boolean shiftReady = plan.getEnterpriseShiftFileId() != null;
    boolean rosterReady = plan.getEnterpriseRosterFileId() != null;
    boolean fullReturn = shiftReady && rosterReady;
    List<String> missing = new ArrayList<>();
    if (!shiftReady) missing.add("排班表");
    if (requireRoster && !rosterReady) missing.add("筛选名单");
    boolean pathReady = shiftReady && (!requireRoster || rosterReady);
    return new TrainDivisionReadyVO(
        plan.getId(),
        plan.getStatus(),
        fullReturn,
        shiftReady,
        rosterReady,
        requireRoster,
        pathReady,
        plan.getEnterpriseRosterFileId(),
        plan.getEnterpriseShiftFileId(),
        plan.getEnterpriseRosterFileName(),
        plan.getEnterpriseShiftFileName(),
        missing
    );
  }

  @Transactional
  public Map<String, Object> ensureTrainInfo(Long planId, TrainDivisionEnsureTrainInfoReq req) {
    TrainThreeYearPlanEntity plan = requirePlan(planId);
    assertReturnReady(plan);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    boolean reuse = req.getReuseExisting() == null || Boolean.TRUE.equals(req.getReuseExisting());

    if (reuse) {
      LambdaQueryWrapper<TrainInfoListEntity> q = new LambdaQueryWrapper<TrainInfoListEntity>()
          .eq(TrainInfoListEntity::getTenantId, tenantId)
          .eq(TrainInfoListEntity::getThreeYearPlanId, planId)
          .orderByDesc(TrainInfoListEntity::getId)
          .last("LIMIT 1");
      if (req.getWorkTypeId() != null) {
        q.eq(TrainInfoListEntity::getWorkTypeId, req.getWorkTypeId());
      }
      if (req.getWorkTypeLevel() != null) {
        q.eq(TrainInfoListEntity::getWorkTypeLevel, req.getWorkTypeLevel());
      }
      TrainInfoListEntity existing = trainInfoListMapper.selectOne(q);
      if (existing != null) {
        return Map.of("trainInfoId", existing.getId(), "created", false, "title", existing.getTitle());
      }
    }

    OrgEntity org = orgMapper.selectById(plan.getTargetOrgId());
    String orgName = org != null ? org.getName() : "";
    String title = StringUtils.hasText(req.getTitle())
        ? req.getTitle().trim()
        : buildDefaultTitle(plan, orgName, req.getPlanYear());

    TrainInfoListEntity entity = new TrainInfoListEntity();
    entity.setTenantId(tenantId);
    entity.setThreeYearPlanId(planId);
    entity.setCompanyId(plan.getTargetOrgId());
    entity.setCompanyName(orgName);
    entity.setTitle(title);
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setStatus(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setCompanyHasConfirm(0);
    trainInfoListMapper.insert(entity);
    return Map.of("trainInfoId", entity.getId(), "created", true, "title", entity.getTitle());
  }

  @Transactional
  public Map<String, Object> parseRoster(TrainDivisionRosterParseReq req) {
    TrainThreeYearPlanEntity plan = requirePlan(req.getPlanId());
    assertReturnReady(plan);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (req.getTrainInfoId() != null) {
      requireTrainInfoLinked(req.getTrainInfoId(), plan.getId(), tenantId);
    }

    softDeleteExistingRoster(tenantId, plan.getId(), req.getTrainInfoId());

    String mode = StringUtils.hasText(req.getMode()) ? req.getMode() : "enterprise_roster";
    List<TrainPlanRosterEntity> rows = new ArrayList<>();
    String sourceUsed = mode;

    if ("plan_item".equalsIgnoreCase(mode)) {
      rows = fromPlanItems(plan, req);
      sourceUsed = "plan_item";
    } else {
      try {
        byte[] bytes = fileApplicationService.readFileBytesById(plan.getEnterpriseRosterFileId());
        rows = rosterParseService.parseEnterpriseRoster(bytes);
      } catch (BusinessException ex) {
        if (Boolean.TRUE.equals(req.getFallbackToPlanItem())) {
          rows = fromPlanItems(plan, req);
          sourceUsed = "plan_item";
        } else {
          throw ex;
        }
      }
      if (rows.isEmpty() && Boolean.TRUE.equals(req.getFallbackToPlanItem())) {
        rows = fromPlanItems(plan, req);
        sourceUsed = "plan_item";
      }
    }

    LocalDateTime now = LocalDateTime.now();
    int withIdCard = 0;
    for (TrainPlanRosterEntity row : rows) {
      row.setTenantId(tenantId);
      row.setPlanId(plan.getId());
      row.setTrainInfoId(req.getTrainInfoId());
      row.setSource(sourceUsed);
      row.setAssignStatus(0);
      row.setDeleted(0);
      row.setCreateTime(now);
      row.setUpdateTime(now);
      if (StringUtils.hasText(row.getIdCard())) withIdCard++;
      rosterMapper.insert(row);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("total", rows.size());
    result.put("withIdCard", withIdCard);
    result.put("withoutIdCard", rows.size() - withIdCard);
    result.put("source", sourceUsed);
    return result;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainPlanRosterVO> pageRoster(
      Long planId, Long trainInfoId, Integer assignStatus, int page, int pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    requirePlan(planId);
    LambdaQueryWrapper<TrainPlanRosterEntity> q = new LambdaQueryWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, planId)
        .orderByAsc(TrainPlanRosterEntity::getId);
    if (trainInfoId != null) {
      q.and(w -> w.eq(TrainPlanRosterEntity::getTrainInfoId, trainInfoId)
          .or().isNull(TrainPlanRosterEntity::getTrainInfoId));
    }
    if (assignStatus != null) {
      q.eq(TrainPlanRosterEntity::getAssignStatus, assignStatus);
    }
    Page<TrainPlanRosterEntity> result = rosterMapper.selectPage(new Page<>(page, pageSize), q);
    List<TrainPlanRosterVO> list = result.getRecords().stream().map(this::toRosterVO).toList();
    return new PageResponse<>(list, page, pageSize, result.getTotal());
  }

  /**
   * 名单 + 排班自动分班（S1 按班组切 ≤classMax；S3 by_roster_order 按名单顺序切）。
   */
  @Transactional
  public Map<String, Object> autoPack(TrainDivisionAutoPackReq req) {
    TrainThreeYearPlanEntity plan = requirePlan(req.getPlanId());
    assertFullReturnReady(plan);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoLinked(req.getTrainInfoId(), plan.getId(), tenantId);

    int classMax = req.getClassMax() == null ? 50 : req.getClassMax();
    if (classMax < 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班额上限无效");
    }
    String strategy = StringUtils.hasText(req.getStrategy()) ? req.getStrategy().trim() : "by_shift_team";
    boolean excludeNoId = Boolean.TRUE.equals(req.getExcludeNoIdCard());
    boolean useShiftFilter = req.getUseShiftFilter() == null || Boolean.TRUE.equals(req.getUseShiftFilter());
    boolean submitConfirm = req.getSubmitForCompanyConfirm() == null
        || Boolean.TRUE.equals(req.getSubmitForCompanyConfirm());
    boolean autoParse = req.getAutoParseRoster() == null || Boolean.TRUE.equals(req.getAutoParseRoster());

    LocalDate nowDate = LocalDate.now();
    int year = req.getYear() != null ? req.getYear() : nowDate.getYear();
    int month = req.getMonth() != null ? req.getMonth() : nowDate.getMonthValue();

    long pending = rosterMapper.selectCount(new LambdaQueryWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, plan.getId())
        .eq(TrainPlanRosterEntity::getAssignStatus, 0));
    if (pending == 0 && autoParse) {
      TrainDivisionRosterParseReq parseReq = new TrainDivisionRosterParseReq();
      parseReq.setPlanId(plan.getId());
      parseReq.setTrainInfoId(trainInfo.getId());
      parseReq.setMode("enterprise_roster");
      parseReq.setFallbackToPlanItem(true);
      parseRoster(parseReq);
    }

    List<TrainPlanRosterEntity> pool = rosterMapper.selectList(
        new LambdaQueryWrapper<TrainPlanRosterEntity>()
            .eq(TrainPlanRosterEntity::getTenantId, tenantId)
            .eq(TrainPlanRosterEntity::getPlanId, plan.getId())
            .eq(TrainPlanRosterEntity::getAssignStatus, 0)
            .orderByAsc(TrainPlanRosterEntity::getId));
    if (pool.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "待分班人员为空，请先上传/解析筛选名单");
    }

    Map<String, TrainShiftProfileService.ShiftPersonProfile> profiles = Map.of();
    List<Map<String, Object>> warnings = new ArrayList<>();
    try {
      profiles = shiftProfileService.loadProfiles(plan, year, month);
    } catch (BusinessException ex) {
      warnings.add(Map.of(
          "level", "WARNING",
          "code", "SHIFT_PARSE_FAIL",
          "message", "排班表解析失败，已降级：" + ex.getMessage()));
      if ("by_shift_team".equalsIgnoreCase(strategy)) {
        strategy = "by_roster_order";
      }
      useShiftFilter = false;
    }

    Map<String, TrainShiftProfileService.ShiftPersonProfile> byName = new HashMap<>();
    Map<String, TrainShiftProfileService.ShiftPersonProfile> byEmp = new HashMap<>();
    for (TrainShiftProfileService.ShiftPersonProfile p : profiles.values()) {
      if (StringUtils.hasText(p.name)) byName.putIfAbsent(p.name.trim(), p);
      if (StringUtils.hasText(p.employeeNo)) byEmp.putIfAbsent(p.employeeNo.trim().toUpperCase(), p);
    }

    List<Map<String, Object>> unassigned = new ArrayList<>();
    List<TrainPlanRosterEntity> assignable = new ArrayList<>();
    Set<String> seenIdCards = new HashSet<>();

    for (TrainPlanRosterEntity r : pool) {
      if (excludeNoId && !StringUtils.hasText(r.getIdCard())) {
        unassigned.add(unassignedRow(r, "无身份证且配置排除"));
        continue;
      }
      if (StringUtils.hasText(r.getIdCard())) {
        String idKey = r.getIdCard().trim().toUpperCase();
        if (!seenIdCards.add(idKey)) {
          unassigned.add(unassignedRow(r, "名单内身份证重复，跳过"));
          continue;
        }
        Long exists = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getTrainId, trainInfo.getId())
            .eq(TrainPeopleListEntity::getIdCard, r.getIdCard().trim())
            .ne(TrainPeopleListEntity::getType, "teacher"));
        if (exists != null && exists > 0) {
          unassigned.add(unassignedRow(r, "已在该培训信息班级中"));
          continue;
        }
      }

      TrainShiftProfileService.ShiftPersonProfile profile = matchProfile(r, byName, byEmp);
      if (profile != null) {
        if (!StringUtils.hasText(r.getShiftTeam()) && StringUtils.hasText(profile.shiftClass)) {
          r.setShiftTeam(profile.shiftClass);
        }
        if (useShiftFilter && profile.attendableSlots <= 0) {
          unassigned.add(unassignedRow(r, "排班显示当月无可上课日（全夜/全休等）"));
          continue;
        }
      } else if (useShiftFilter && !profiles.isEmpty()) {
        warnings.add(Map.of(
            "level", "INFO",
            "code", "SHIFT_NOT_FOUND",
            "message", r.getPersonName() + " 未在排班表匹配到，仍参与分班"));
      }
      assignable.add(r);
    }

    if (assignable.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可分班人员（均被过滤或已分班）");
    }

    String prefix = StringUtils.hasText(req.getClassNamePrefix())
        ? req.getClassNamePrefix().trim()
        : (StringUtils.hasText(trainInfo.getCompanyName()) ? trainInfo.getCompanyName() + "-" : "")
            + year + String.format("%02d", month) + "-";

    Map<String, List<TrainPlanRosterEntity>> groups = new LinkedHashMap<>();
    if ("by_roster_order".equalsIgnoreCase(strategy)) {
      groups.put("混合", new ArrayList<>(assignable));
    } else {
      for (TrainPlanRosterEntity r : assignable) {
        TrainShiftProfileService.ShiftPersonProfile profile = matchProfile(r, byName, byEmp);
        String group = resolveGroupKey(r, profile);
        groups.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
      }
    }

    List<Map<String, Object>> classes = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    int assignedCount = 0;

    for (Map.Entry<String, List<TrainPlanRosterEntity>> ge : groups.entrySet()) {
      String groupName = ge.getKey();
      List<TrainPlanRosterEntity> members = new ArrayList<>(ge.getValue());
      members.sort(Comparator.comparing(
          x -> x.getPersonName() == null ? "" : x.getPersonName(),
          String.CASE_INSENSITIVE_ORDER));
      int seq = 1;
      for (int i = 0; i < members.size(); i += classMax) {
        List<TrainPlanRosterEntity> chunk = members.subList(i, Math.min(i + classMax, members.size()));
        String title = prefix + sanitizeTitle(groupName) + "-" + seq + "班";
        TrainInfoClassEntity clazz = new TrainInfoClassEntity();
        clazz.setTrainInfoId(trainInfo.getId());
        clazz.setTitle(title);
        clazz.setCompanyId(trainInfo.getCompanyId());
        clazz.setWorkTypeId(trainInfo.getWorkTypeId());
        clazz.setWorkTypeLevel(trainInfo.getWorkTypeLevel());
        clazz.setNums(chunk.size());
        clazz.setAddtime((int) (System.currentTimeMillis() / 1000));
        clazz.setDeleted(0);
        clazz.setCompanyConfirmStatus(submitConfirm ? 0 : 0);
        trainInfoClassMapper.insert(clazz);

        int added = 0;
        for (TrainPlanRosterEntity roster : chunk) {
          TrainPeopleListEntity people = new TrainPeopleListEntity();
          people.setTenantId(tenantId);
          people.setTrainId(trainInfo.getId());
          people.setTrainClassId(clazz.getId());
          people.setType("student");
          people.setRealname(roster.getPersonName());
          people.setPhone(roster.getPhone());
          people.setIdCard(roster.getIdCard());
          people.setDeleted(0);
          people.setCreateTime(now);
          peopleListMapper.insert(people);

          roster.setAssignStatus(1);
          roster.setTrainClassId(clazz.getId());
          roster.setTrainInfoId(trainInfo.getId());
          roster.setUpdateTime(now);
          rosterMapper.updateById(roster);
          added++;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("classId", clazz.getId());
        row.put("title", clazz.getTitle());
        row.put("group", groupName);
        row.put("nums", added);
        classes.add(row);
        assignedCount += added;
        seq++;
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("trainInfoId", trainInfo.getId());
    result.put("strategy", strategy);
    result.put("classMax", classMax);
    result.put("classCount", classes.size());
    result.put("assignedCount", assignedCount);
    result.put("classes", classes);
    result.put("unassigned", unassigned);
    result.put("unassignedCount", unassigned.size());
    result.put("warnings", warnings);
    result.put("submitForCompanyConfirm", submitConfirm);
    return result;
  }

  private static TrainShiftProfileService.ShiftPersonProfile matchProfile(
      TrainPlanRosterEntity r,
      Map<String, TrainShiftProfileService.ShiftPersonProfile> byName,
      Map<String, TrainShiftProfileService.ShiftPersonProfile> byEmp) {
    if (StringUtils.hasText(r.getIdCard())) {
      TrainShiftProfileService.ShiftPersonProfile p = byEmp.get(r.getIdCard().trim().toUpperCase());
      if (p != null) return p;
    }
    if (StringUtils.hasText(r.getPersonName())) {
      return byName.get(r.getPersonName().trim());
    }
    return null;
  }

  private static String resolveGroupKey(
      TrainPlanRosterEntity r, TrainShiftProfileService.ShiftPersonProfile profile) {
    if (StringUtils.hasText(r.getShiftTeam())) return r.getShiftTeam().trim();
    if (profile != null && StringUtils.hasText(profile.shiftClass)) return profile.shiftClass.trim();
    if (profile != null && StringUtils.hasText(profile.dominantType)
        && !"未识别".equals(profile.dominantType) && !"休".equals(profile.dominantType)) {
      return "倒班-" + profile.dominantType;
    }
    return "未分组";
  }

  private static String sanitizeTitle(String raw) {
    if (!StringUtils.hasText(raw)) return "未分组";
    return raw.trim().replaceAll("[\\\\/:*?\"<>|]", "-");
  }

  private static Map<String, Object> unassignedRow(TrainPlanRosterEntity r, String reason) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("rosterId", r.getId());
    m.put("personName", r.getPersonName());
    m.put("idCard", r.getIdCard());
    m.put("shiftTeam", r.getShiftTeam());
    m.put("reason", reason);
    return m;
  }

  @Transactional
  public Map<String, Object> orgSplit(TrainDivisionOrgSplitReq req) {
    TrainThreeYearPlanEntity plan = requirePlan(req.getPlanId());
    assertFullReturnReady(plan);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoLinked(req.getTrainInfoId(), plan.getId(), tenantId);

    Set<Long> allRosterIds = new HashSet<>();
    for (TrainDivisionOrgSplitReq.ClassItem c : req.getClasses()) {
      allRosterIds.addAll(c.getRosterIds());
    }
    if (allRosterIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少分配一名学员");
    }

    List<TrainPlanRosterEntity> rosterRows = rosterMapper.selectList(
        new LambdaQueryWrapper<TrainPlanRosterEntity>()
            .eq(TrainPlanRosterEntity::getTenantId, tenantId)
            .eq(TrainPlanRosterEntity::getPlanId, plan.getId())
            .in(TrainPlanRosterEntity::getId, allRosterIds));
    Map<Long, TrainPlanRosterEntity> rosterMap = rosterRows.stream()
        .collect(Collectors.toMap(TrainPlanRosterEntity::getId, r -> r, (a, b) -> a));
    if (rosterMap.size() != allRosterIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "存在无效的待分班人员");
    }

    // 同 trainInfo 下身份证不可重复分班
    Set<String> idCardsInRequest = new HashSet<>();
    for (TrainPlanRosterEntity r : rosterRows) {
      if (!StringUtils.hasText(r.getIdCard())) continue;
      String key = r.getIdCard().trim().toUpperCase();
      if (!idCardsInRequest.add(key)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一身份证不能分入多个班级：" + r.getIdCard());
      }
      Long exists = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
          .eq(TrainPeopleListEntity::getTenantId, tenantId)
          .eq(TrainPeopleListEntity::getTrainId, trainInfo.getId())
          .eq(TrainPeopleListEntity::getIdCard, r.getIdCard().trim())
          .ne(TrainPeopleListEntity::getType, "teacher"));
      if (exists != null && exists > 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "学员已在该培训信息的班级中：" + r.getPersonName() + "/" + r.getIdCard());
      }
    }

    List<Map<String, Object>> createdClasses = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    boolean submitConfirm = req.getSubmitForCompanyConfirm() == null
        || Boolean.TRUE.equals(req.getSubmitForCompanyConfirm());

    for (TrainDivisionOrgSplitReq.ClassItem item : req.getClasses()) {
      TrainInfoClassEntity clazz;
      if (item.getClassId() != null && item.getClassId() > 0) {
        clazz = trainInfoClassMapper.selectById(item.getClassId());
        if (clazz == null || !Objects.equals(clazz.getTrainInfoId(), trainInfo.getId())) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "班级不存在或不属于当前培训信息");
        }
        clazz.setTitle(item.getTitle().trim());
        if (item.getWorkTypeId() != null) clazz.setWorkTypeId(item.getWorkTypeId());
        if (item.getWorkTypeLevel() != null) clazz.setWorkTypeLevel(item.getWorkTypeLevel());
        if (StringUtils.hasText(item.getDateStart())) clazz.setDateStart(item.getDateStart());
        if (StringUtils.hasText(item.getDateEnd())) clazz.setDateEnd(item.getDateEnd());
        clazz.setNums(item.getRosterIds().size());
        if (submitConfirm) clazz.setCompanyConfirmStatus(0);
        trainInfoClassMapper.updateById(clazz);
      } else {
        clazz = new TrainInfoClassEntity();
        clazz.setTrainInfoId(trainInfo.getId());
        clazz.setTitle(item.getTitle().trim());
        clazz.setCompanyId(trainInfo.getCompanyId());
        clazz.setWorkTypeId(item.getWorkTypeId() != null ? item.getWorkTypeId() : trainInfo.getWorkTypeId());
        clazz.setWorkTypeLevel(item.getWorkTypeLevel() != null ? item.getWorkTypeLevel() : trainInfo.getWorkTypeLevel());
        clazz.setDateStart(item.getDateStart());
        clazz.setDateEnd(item.getDateEnd());
        clazz.setNums(item.getRosterIds().size());
        clazz.setAddtime((int) (System.currentTimeMillis() / 1000));
        clazz.setDeleted(0);
        clazz.setCompanyConfirmStatus(submitConfirm ? 0 : 0);
        trainInfoClassMapper.insert(clazz);
      }

      int added = 0;
      for (Long rosterId : item.getRosterIds()) {
        TrainPlanRosterEntity roster = rosterMap.get(rosterId);
        if (roster == null) continue;
        if (roster.getAssignStatus() != null && roster.getAssignStatus() == 1) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "人员已分班，请勿重复提交：" + roster.getPersonName());
        }
        TrainPeopleListEntity people = new TrainPeopleListEntity();
        people.setTenantId(tenantId);
        people.setTrainId(trainInfo.getId());
        people.setTrainClassId(clazz.getId());
        people.setType("student");
        people.setRealname(roster.getPersonName());
        people.setPhone(roster.getPhone());
        people.setIdCard(roster.getIdCard());
        people.setDeleted(0);
        people.setCreateTime(now);
        peopleListMapper.insert(people);

        roster.setAssignStatus(1);
        roster.setTrainClassId(clazz.getId());
        roster.setTrainInfoId(trainInfo.getId());
        roster.setUpdateTime(now);
        rosterMapper.updateById(roster);
        added++;
      }

      Map<String, Object> classResult = new HashMap<>();
      classResult.put("classId", clazz.getId());
      classResult.put("title", clazz.getTitle());
      classResult.put("nums", added);
      createdClasses.add(classResult);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("trainInfoId", trainInfo.getId());
    result.put("classes", createdClasses);
    result.put("classCount", createdClasses.size());
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> trainInfoSummary(Long trainInfoId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity info = trainInfoListMapper.selectById(trainInfoId);
    if (info == null || !Objects.equals(info.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训信息不存在");
    }
    long classCount = trainInfoClassMapper.selectCount(new LambdaQueryWrapper<TrainInfoClassEntity>()
        .eq(TrainInfoClassEntity::getTrainInfoId, trainInfoId));
    long peopleCount = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getTrainId, trainInfoId)
        .eq(TrainPeopleListEntity::getType, "student"));
    long pending = 0;
    if (info.getThreeYearPlanId() != null) {
      pending = rosterMapper.selectCount(new LambdaQueryWrapper<TrainPlanRosterEntity>()
          .eq(TrainPlanRosterEntity::getTenantId, tenantId)
          .eq(TrainPlanRosterEntity::getPlanId, info.getThreeYearPlanId())
          .eq(TrainPlanRosterEntity::getAssignStatus, 0));
    }
    Map<String, Object> map = new HashMap<>();
    map.put("trainInfoId", info.getId());
    map.put("title", info.getTitle());
    map.put("planId", info.getThreeYearPlanId());
    map.put("companyId", info.getCompanyId());
    map.put("companyName", info.getCompanyName());
    map.put("classCount", classCount);
    map.put("peopleCount", peopleCount);
    map.put("pendingRosterCount", pending);
    return map;
  }

  private TrainDivisionPlanVO toPlanVO(TrainThreeYearPlanEntity plan) {
    OrgEntity org = plan.getTargetOrgId() != null ? orgMapper.selectById(plan.getTargetOrgId()) : null;
    boolean shiftReady = plan.getEnterpriseShiftFileId() != null;
    boolean rosterReady = plan.getEnterpriseRosterFileId() != null;
    boolean ready = shiftReady && (!requireRoster || rosterReady);
    Long tenantId = plan.getTenantId();
    long linked = trainInfoListMapper.selectCount(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getThreeYearPlanId, plan.getId()));
    long pending = rosterMapper.selectCount(new LambdaQueryWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, plan.getId())
        .eq(TrainPlanRosterEntity::getAssignStatus, 0));
    return new TrainDivisionPlanVO(
        plan.getId(),
        plan.getPlanNo(),
        plan.getTitle(),
        plan.getTargetOrgId(),
        org != null ? org.getName() : "",
        plan.getStatus(),
        STATUS_LABELS.getOrDefault(plan.getStatus(), "未知"),
        ready,
        plan.getEnterpriseRosterFileName(),
        plan.getEnterpriseShiftFileName(),
        plan.getEnterpriseRosterFileId(),
        plan.getEnterpriseShiftFileId(),
        linked,
        pending
    );
  }

  private TrainPlanRosterVO toRosterVO(TrainPlanRosterEntity e) {
    return new TrainPlanRosterVO(
        e.getId(), e.getPlanId(), e.getTrainInfoId(), e.getSource(),
        e.getPersonName(), e.getIdCard(), e.getPhone(), e.getJobName(), e.getLevelName(),
        e.getWorkshop(), e.getShiftTeam(), e.getPlanYear(), e.getTrainMode(),
        e.getAssignStatus(), e.getTrainClassId()
    );
  }

  private List<TrainPlanRosterEntity> fromPlanItems(TrainThreeYearPlanEntity plan, TrainDivisionRosterParseReq req) {
    LambdaQueryWrapper<TrainThreeYearPlanItemEntity> q = new LambdaQueryWrapper<TrainThreeYearPlanItemEntity>()
        .eq(TrainThreeYearPlanItemEntity::getPlanId, plan.getId())
        .eq(TrainThreeYearPlanItemEntity::getTenantId, plan.getTenantId())
        .isNotNull(TrainThreeYearPlanItemEntity::getPersonName);
    if (req.getPlanYear() != null) {
      q.eq(TrainThreeYearPlanItemEntity::getPlanYear, req.getPlanYear());
    }
    if (StringUtils.hasText(req.getJobName())) {
      q.eq(TrainThreeYearPlanItemEntity::getJobName, req.getJobName().trim());
    }
    List<TrainThreeYearPlanItemEntity> items = itemMapper.selectList(q);
    // 按身份证/姓名去重
    Map<String, TrainPlanRosterEntity> uniq = new HashMap<>();
    for (TrainThreeYearPlanItemEntity item : items) {
      if (!StringUtils.hasText(item.getPersonName())) continue;
      String key = StringUtils.hasText(item.getIdCard())
          ? item.getIdCard().trim().toUpperCase()
          : "N:" + item.getPersonName().trim();
      if (uniq.containsKey(key)) continue;
      TrainPlanRosterEntity row = new TrainPlanRosterEntity();
      row.setSource("plan_item");
      row.setPersonName(item.getPersonName().trim());
      row.setIdCard(StringUtils.hasText(item.getIdCard()) ? item.getIdCard().trim() : null);
      row.setJobName(item.getJobName());
      row.setLevelName(item.getLevelName());
      row.setPlanYear(item.getPlanYear());
      row.setTrainMode(item.getTrainMode());
      row.setAssignStatus(0);
      row.setDeleted(0);
      uniq.put(key, row);
    }
    if (uniq.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "规划明细中无可用人员，无法灌入待分班池");
    }
    return new ArrayList<>(uniq.values());
  }

  private void softDeleteExistingRoster(Long tenantId, Long planId, Long trainInfoId) {
    LambdaUpdateWrapper<TrainPlanRosterEntity> u = new LambdaUpdateWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, planId)
        .eq(TrainPlanRosterEntity::getAssignStatus, 0)
        .set(TrainPlanRosterEntity::getDeleted, 1)
        .set(TrainPlanRosterEntity::getUpdateTime, LocalDateTime.now());
    if (trainInfoId != null) {
      u.and(w -> w.eq(TrainPlanRosterEntity::getTrainInfoId, trainInfoId)
          .or().isNull(TrainPlanRosterEntity::getTrainInfoId));
    }
    rosterMapper.update(null, u);
  }

  private TrainThreeYearPlanEntity requirePlan(Long planId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainThreeYearPlanEntity plan = planMapper.selectById(planId);
    if (plan == null || !Objects.equals(plan.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "三年规划不存在");
    }
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    if (companyFilter != null && !Objects.equals(companyFilter, plan.getTargetOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该企业规划");
    }
    if (plan.getStatus() == null || plan.getStatus() != 4) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅「企业已确认」的规划可进入分班");
    }
    return plan;
  }

  /** 主路径门禁：排班必填；名单按配置 */
  private void assertReturnReady(TrainThreeYearPlanEntity plan) {
    if (plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未补交排班表，请先补交后再分班");
    }
    if (requireRoster && plan.getEnterpriseRosterFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未补交筛选名单，请先补交后再分班");
    }
  }

  /** 兼容模式 org-split：仍要求名单+排班齐套（依赖名单勾选） */
  private void assertFullReturnReady(TrainThreeYearPlanEntity plan) {
    if (plan.getEnterpriseRosterFileId() == null || plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "兼容模式需名单与排班均已上传");
    }
  }

  private TrainInfoListEntity requireTrainInfoLinked(Long trainInfoId, Long planId, Long tenantId) {
    TrainInfoListEntity info = trainInfoListMapper.selectById(trainInfoId);
    if (info == null || !Objects.equals(info.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训信息不存在");
    }
    if (info.getThreeYearPlanId() != null && !Objects.equals(info.getThreeYearPlanId(), planId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "培训信息与规划不匹配");
    }
    if (info.getThreeYearPlanId() == null) {
      info.setThreeYearPlanId(planId);
      trainInfoListMapper.updateById(info);
    }
    return info;
  }

  private String buildDefaultTitle(TrainThreeYearPlanEntity plan, String orgName, Integer planYear) {
    String year = planYear != null ? String.valueOf(planYear) : "";
    String base = StringUtils.hasText(orgName) ? orgName : plan.getTitle();
    if (StringUtils.hasText(year)) {
      return base + "-" + year + "-培训班";
    }
    return base + "-培训班";
  }
}
