package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.support.CertMappingSupport;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionAddPersonReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionAutoPackReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionEnsureTrainInfoReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionManualPackReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionOrgSplitReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionPackPatchReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDivisionRosterParseReq;
import com.guoyu.platform.trainingdepartment.entity.TrainDivisionPackTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainThreeYearPlanItemEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainDivisionPackTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPlanRosterMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainThreeYearPlanMapper;
import com.guoyu.platform.trainingdepartment.service.shiftassign.ShiftAssignDefaults;
import com.guoyu.platform.trainingdepartment.service.shiftassign.config.EffectiveSchedulingConfig;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ClassAssignResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ScheduleParseResult;
import com.guoyu.platform.trainingdepartment.service.shiftassign.domain.ShiftTimeDef;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ExcelScheduleParseService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ScheduleAlgorithmService;
import com.guoyu.platform.trainingdepartment.service.shiftassign.engine.ShiftTimeParser;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionPlanVO;
import com.guoyu.platform.trainingdepartment.vo.TrainDivisionReadyVO;
import com.guoyu.platform.trainingdepartment.vo.TrainPlanRosterVO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
  private final TrainDivisionPackTaskMapper packTaskMapper;
  private final OrgMapper orgMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final FileApplicationService fileApplicationService;
  private final TrainPlanRosterParseService rosterParseService;
  private final TrainingOperatorScopeService scopeService;
  private final TrainShiftProfileService shiftProfileService;
  private final ObjectMapper objectMapper;
  private final ExcelScheduleParseService excelScheduleParseService;
  private final ScheduleAlgorithmService scheduleAlgorithmService;
  private final PlatformUserEnsureService platformUserEnsureService;
  private final UserMapper userMapper;

  private static final Set<String> EDITABLE_PACK_STATUS = Set.of("draft", "rejected");
  private static final Set<String> ACTIVE_PACK_STATUS = Set.of("draft", "pending", "rejected");

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
    String trainCompanyName = resolveTrainCompanyName(req.getTrainCompanyName());

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
        // 主流程历史数据常缺培训机构名，复用时补齐供 ${pxdw}
        if (!StringUtils.hasText(existing.getTrainCompanyName()) && StringUtils.hasText(trainCompanyName)) {
          existing.setTrainCompanyName(trainCompanyName);
          trainInfoListMapper.updateById(existing);
        }
        return Map.of(
            "trainInfoId", existing.getId(),
            "created", false,
            "title", existing.getTitle(),
            "trainCompanyName", existing.getTrainCompanyName() != null ? existing.getTrainCompanyName() : "");
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
    entity.setTrainCompanyName(trainCompanyName);
    entity.setTitle(title);
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setStatus(0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setCompanyHasConfirm(0);
    trainInfoListMapper.insert(entity);
    return Map.of(
        "trainInfoId", entity.getId(),
        "created", true,
        "title", entity.getTitle(),
        "trainCompanyName", entity.getTrainCompanyName() != null ? entity.getTrainCompanyName() : "");
  }

  /**
   * 培训机构名称（${pxdw}）：请求值 → 当前登录机构组织名 → 默认「千铝工投」。
   * 与「需求转开班」对齐，避免三年规划→分班路径 train_company_name 为空。
   */
  private String resolveTrainCompanyName(String requested) {
    if (StringUtils.hasText(requested)) {
      return requested.trim();
    }
    TrainingOperatorScopeService.Scope scope = scopeService.currentScope();
    if (scope.isOrgOperator() && scope.orgId() != null) {
      OrgEntity operatorOrg = orgMapper.selectById(scope.orgId());
      if (operatorOrg != null && StringUtils.hasText(operatorOrg.getName())) {
        return operatorOrg.getName().trim();
      }
    }
    return "千铝工投";
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
      Long planId,
      Long trainInfoId,
      Integer assignStatus,
      String keyword,
      String jobName,
      int page,
      int pageSize) {
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
    if (StringUtils.hasText(keyword)) {
      String kw = keyword.trim();
      q.and(w -> w.like(TrainPlanRosterEntity::getPersonName, kw)
          .or().like(TrainPlanRosterEntity::getIdCard, kw));
    }
    if (StringUtils.hasText(jobName)) {
      q.like(TrainPlanRosterEntity::getJobName, jobName.trim());
    }
    Page<TrainPlanRosterEntity> result = rosterMapper.selectPage(new Page<>(page, pageSize), q);
    List<TrainPlanRosterVO> list = result.getRecords().stream().map(this::toRosterVO).toList();
    return new PageResponse<>(list, page, pageSize, result.getTotal());
  }

  /**
   * 名单 + 排班自动分班：仅生成草稿方案（不建班），供机构编辑后提交企业审核。
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
    String strategy = StringUtils.hasText(req.getStrategy()) ? req.getStrategy().trim() : "by_job_level";
    boolean excludeNoId = Boolean.TRUE.equals(req.getExcludeNoIdCard());
    boolean useShiftFilter = req.getUseShiftFilter() == null || Boolean.TRUE.equals(req.getUseShiftFilter());
    boolean requireJobAndLevel = req.getRequireJobAndLevel() == null
        || Boolean.TRUE.equals(req.getRequireJobAndLevel());
    boolean autoParse = req.getAutoParseRoster() == null || Boolean.TRUE.equals(req.getAutoParseRoster());

    LocalDate nowDate = LocalDate.now();
    int year = req.getYear() != null ? req.getYear() : nowDate.getYear();
    int month = req.getMonth() != null ? req.getMonth() : nowDate.getMonthValue();

    // 始终按规划上「当前」企业名单重灌待分池，避免沿用旧名单
    if (autoParse) {
      if (plan.getEnterpriseRosterFileId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未上传筛选名单，无法自动分班");
      }
      TrainDivisionRosterParseReq parseReq = new TrainDivisionRosterParseReq();
      parseReq.setPlanId(plan.getId());
      parseReq.setTrainInfoId(trainInfo.getId());
      parseReq.setMode("enterprise_roster");
      // 禁止静默回退到规划明细，否则会与企业新上传名单不一致
      parseReq.setFallbackToPlanItem(false);
      parseRoster(parseReq);
    }

    List<TrainPlanRosterEntity> pool = rosterMapper.selectList(
        new LambdaQueryWrapper<TrainPlanRosterEntity>()
            .eq(TrainPlanRosterEntity::getTenantId, tenantId)
            .eq(TrainPlanRosterEntity::getPlanId, plan.getId())
            .eq(TrainPlanRosterEntity::getAssignStatus, 0)
            .and(w -> w.eq(TrainPlanRosterEntity::getTrainInfoId, trainInfo.getId())
                .or().isNull(TrainPlanRosterEntity::getTrainInfoId))
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

    boolean poolHasPlanYear = pool.stream().anyMatch(r -> r.getPlanYear() != null);
    if (poolHasPlanYear) {
      boolean yearMatched = pool.stream().anyMatch(r -> year == (r.getPlanYear() == null ? -1 : r.getPlanYear()));
      if (!yearMatched) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "所选年份 " + year + " 与名单无交集，请核对培训/排班年或重新上传名单");
      }
    } else {
      warnings.add(Map.of(
          "level", "INFO",
          "code", "ROSTER_NO_PLAN_YEAR",
          "message", "名单未带培训年份，将使用全部待分人员；排班年仅用于解析当月排班"));
    }

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
          String activeTitle = releaseOrphanPeopleAndFindActiveClassTitle(
              tenantId, trainInfo.getId(), r.getIdCard().trim());
          if (activeTitle != null) {
            unassigned.add(unassignedRow(r, "已在班级「" + activeTitle + "」中"));
            continue;
          }
        }

      if (poolHasPlanYear) {
        if (r.getPlanYear() == null) {
          unassigned.add(unassignedRow(r, "未标注培训年份"));
          continue;
        }
        if (!r.getPlanYear().equals(year)) {
          unassigned.add(unassignedRow(r, "名单年份 " + r.getPlanYear() + " 与所选年 " + year + " 不一致"));
          continue;
        }
      }

      TrainShiftProfileService.ShiftPersonProfile profile = matchProfile(r, byName, byEmp);
      if (profile != null) {
        if (!StringUtils.hasText(r.getShiftTeam()) && StringUtils.hasText(profile.shiftClass)) {
          r.setShiftTeam(profile.shiftClass);
          rosterMapper.updateById(r);
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

    WorkTypeLookup workTypeLookup = buildWorkTypeLookup();

    List<Map<String, Object>> classes = new ArrayList<>();
    int assignedCount = 0;
    if ("by_job_level_slot".equalsIgnoreCase(strategy)
        || "by_attendable_slot".equalsIgnoreCase(strategy)) {
      SlotPackResult slotPack = buildJobLevelSlotClasses(
          plan, assignable, prefix, classMax, year, month, warnings, workTypeLookup);
      if (!slotPack.classes.isEmpty()) {
        classes = slotPack.classes;
        assignedCount = slotPack.assignedCount;
        unassigned.addAll(slotPack.extraUnassigned);
        strategy = "by_job_level_slot";
      } else {
        // 时段策略对不上人：自动降级为推荐策略，避免卡死
        warnings.add(Map.of(
            "level", "WARNING",
            "code", "SLOT_FALLBACK_JOB_LEVEL",
            "message", StringUtils.hasText(slotPack.failReason)
                ? ("按时段分班失败（" + slotPack.failReason + "），已自动改用「按工种/等级切班」")
                : "按时段分班未匹配到人，已自动改用「按工种/等级切班」"));
        strategy = "by_job_level";
      }
    }
    if (classes.isEmpty()) {
      Map<String, List<TrainPlanRosterEntity>> groups = new LinkedHashMap<>();
      if ("by_roster_order".equalsIgnoreCase(strategy)) {
        groups.put("混合", new ArrayList<>(assignable));
      } else if ("by_shift_team".equalsIgnoreCase(strategy)) {
        for (TrainPlanRosterEntity r : assignable) {
          TrainShiftProfileService.ShiftPersonProfile profile = matchProfile(r, byName, byEmp);
          String group = resolveGroupKey(r, profile);
          groups.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
        }
      } else {
        // 默认：by_job_level —— 按工种×等级稳定切班；排班仅过滤
        strategy = "by_job_level";
        List<TrainPlanRosterEntity> jobLevelPool = new ArrayList<>();
        for (TrainPlanRosterEntity r : assignable) {
          boolean missJob = !StringUtils.hasText(r.getJobName());
          boolean missLevel = !StringUtils.hasText(r.getLevelName());
          if (requireJobAndLevel && (missJob || missLevel)) {
            String reason = missJob && missLevel ? "缺工种与等级"
                : (missJob ? "缺工种" : "缺等级");
            unassigned.add(unassignedRow(r, reason));
            continue;
          }
          jobLevelPool.add(r);
        }
        if (jobLevelPool.isEmpty()) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "没有可分班人员（工种/等级缺失或均被过滤）");
        }
        for (TrainPlanRosterEntity r : jobLevelPool) {
          String group = resolveJobLevelGroupKey(r, requireJobAndLevel);
          groups.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
        }
      }
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
          List<Map<String, Object>> memberRows = new ArrayList<>();
          for (TrainPlanRosterEntity roster : chunk) {
            memberRows.add(memberDraft(roster));
          }
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("tempId", "c-" + UUID.randomUUID().toString().substring(0, 8));
          row.put("title", title);
          row.put("group", groupName);
          row.put("nums", memberRows.size());
          row.put("members", memberRows);
          String jobName = chunk.isEmpty() ? null : chunk.get(0).getJobName();
          String levelName = chunk.isEmpty() ? null : chunk.get(0).getLevelName();
          fillClassWorkMeta(row, jobName, levelName, workTypeLookup);
          classes.add(row);
          assignedCount += memberRows.size();
          seq++;
        }
      }
    }

    if (classes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "未生成任何班级。可分班人数=" + assignable.size()
              + "，未分入=" + unassigned.size()
              + "。请检查名单工种/等级、所选年月份与排班过滤条件");
    }

    supersedeActivePackTasks(tenantId, trainInfo.getId());

    LocalDateTime now = LocalDateTime.now();
    TrainDivisionPackTaskEntity task = new TrainDivisionPackTaskEntity();
    task.setTenantId(tenantId);
    task.setPlanId(plan.getId());
    task.setTrainInfoId(trainInfo.getId());
    task.setCompanyId(trainInfo.getCompanyId() != null ? trainInfo.getCompanyId() : plan.getTargetOrgId());
    task.setStrategy(strategy);
    task.setClassMax(classMax);
    task.setYear(year);
    task.setMonth(month);
    task.setClassNamePrefix(prefix);
    task.setStatus("draft");
    task.setClassCount(classes.size());
    task.setAssignedCount(assignedCount);
    task.setUnassignedCount(unassigned.size());
    task.setClassesJson(toJson(classes));
    task.setUnassignedJson(toJson(unassigned));
    task.setWarningJson(toJson(warnings));
    task.setCreateTime(now);
    task.setUpdateTime(now);
    task.setDeleted(0);
    packTaskMapper.insert(task);

    return toPackTaskVo(task);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getPackTask(Long taskId) {
    return toPackTaskVo(requirePackTask(taskId, false));
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> pagePackTasks(int page, int pageSize, String status, Long planId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    LambdaQueryWrapper<TrainDivisionPackTaskEntity> q = new LambdaQueryWrapper<TrainDivisionPackTaskEntity>()
        .eq(TrainDivisionPackTaskEntity::getTenantId, tenantId)
        .orderByDesc(TrainDivisionPackTaskEntity::getUpdateTime);
    if (companyFilter != null) {
      q.eq(TrainDivisionPackTaskEntity::getCompanyId, companyFilter);
    }
    if (StringUtils.hasText(status)) {
      q.eq(TrainDivisionPackTaskEntity::getStatus, status.trim());
    }
    if (planId != null) {
      q.eq(TrainDivisionPackTaskEntity::getPlanId, planId);
    }
    Page<TrainDivisionPackTaskEntity> mp = packTaskMapper.selectPage(new Page<>(page, pageSize), q);
    List<Map<String, Object>> items = mp.getRecords().stream().map(this::toPackTaskSummary).toList();
    return new PageResponse<>(items, page, pageSize, mp.getTotal());
  }

  @Transactional
  public Map<String, Object> patchPackTask(Long taskId, TrainDivisionPackPatchReq req) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, true);
    if (!EDITABLE_PACK_STATUS.contains(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿或已驳回方案可编辑");
    }
    applyPackDraft(task, req.getClasses(), req.getUnassigned());
    if ("rejected".equals(task.getStatus())) {
      task.setStatus("draft");
      task.setRejectReason(null);
    }
    task.setUpdateTime(LocalDateTime.now());
    packTaskMapper.updateById(task);
    return toPackTaskVo(task);
  }

  @Transactional
  public Map<String, Object> submitPackTask(Long taskId) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, true);
    if (!EDITABLE_PACK_STATUS.contains(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿或已驳回方案可提交企业审核");
    }
    List<Map<String, Object>> classes = readJsonList(task.getClassesJson());
    if (classes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个班级后再提交");
    }
    int classMax = task.getClassMax() == null ? 50 : task.getClassMax();
    for (Map<String, Object> c : classes) {
      List<Map<String, Object>> members = asMemberList(c.get("members"));
      if (members.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "班级「" + c.get("title") + "」无人，请调整或删除");
      }
      if (members.size() > classMax) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "班级「" + c.get("title") + "」人数 " + members.size() + " 超过上限 " + classMax);
      }
      if (!StringUtils.hasText(String.valueOf(c.get("title")))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "班级名称不能为空");
      }
    }
    var user = SecurityContext.currentUser();
    task.setStatus("pending");
    task.setSubmitUserId(user.getUserId());
    task.setSubmitUsername(user.getUsername());
    task.setRejectReason(null);
    task.setUpdateTime(LocalDateTime.now());
    packTaskMapper.updateById(task);
    return toPackTaskVo(task);
  }

  @Transactional
  public Map<String, Object> companyConfirmPackTask(Long taskId) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, false);
    assertEnterpriseCanReviewPack(task);
    if (!"pending".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待企业审核的方案可通过");
    }
    Map<String, Object> materialized = materializePackTask(task);
    var user = SecurityContext.currentUser();
    task.setStatus("materialized");
    task.setConfirmUserId(user.getUserId());
    task.setConfirmUsername(user.getUsername());
    task.setConfirmTime(LocalDateTime.now());
    task.setRejectReason(null);
    task.setUpdateTime(LocalDateTime.now());
    packTaskMapper.updateById(task);

    Map<String, Object> result = toPackTaskVo(task);
    result.put("materialized", materialized);
    Object sync = materialized.get("platformUserSync");
    if (sync != null) {
      result.put("platformUserSync", sync);
    }
    return result;
  }

  /**
   * 历史补跑：对已物化方案中 platform_user_id 为空的花名册学员开 App 账号。
   */
  @Transactional
  public Map<String, Object> syncPlatformUsersForPackTask(Long taskId) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, false);
    if (!"materialized".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已建班的方案可补开 App 账号");
    }
    scopeService.requireOrgOperator();
    Long tenantId = task.getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoLinked(task.getTrainInfoId(), task.getPlanId(), tenantId);
    Long orgId = resolvePlatformOrgIdForPack(task);
    Long companyId = trainInfo.getCompanyId() != null ? trainInfo.getCompanyId() : task.getCompanyId();

    List<TrainPeopleListEntity> people = peopleListMapper.selectList(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getTrainId, trainInfo.getId())
        .eq(TrainPeopleListEntity::getType, "student")
        .isNull(TrainPeopleListEntity::getPlatformUserId));

    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();
    int linked = 0;
    for (TrainPeopleListEntity row : people) {
      PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
          row.getRealname(),
          row.getIdCard(),
          row.getPhone(),
          companyId,
          orgId);
      syncStats.accept(ensure, row.getRealname(), PlatformUserEnsureService.normalizeIdCard(row.getIdCard()));
      if (ensure.platformUserId() != null) {
        Long oldId = row.getPlatformUserId();
        if (oldId == null || !oldId.equals(ensure.platformUserId())) {
          row.setPlatformUserId(ensure.platformUserId());
          peopleListMapper.updateById(row);
          linked++;
        }
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("taskId", task.getId());
    result.put("trainInfoId", trainInfo.getId());
    result.put("candidates", people.size());
    result.put("linked", linked);
    result.put("platformUserSync", syncStats.toMap());
    return result;
  }

  @Transactional
  public Map<String, Object> companyRejectPackTask(Long taskId, String reason) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, false);
    assertEnterpriseCanReviewPack(task);
    if (!"pending".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待企业审核的方案可驳回");
    }
    if (!StringUtils.hasText(reason)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写驳回理由");
    }
    var user = SecurityContext.currentUser();
    task.setStatus("rejected");
    task.setRejectReason(reason.trim());
    task.setConfirmUserId(user.getUserId());
    task.setConfirmUsername(user.getUsername());
    task.setConfirmTime(LocalDateTime.now());
    task.setUpdateTime(LocalDateTime.now());
    packTaskMapper.updateById(task);
    return toPackTaskVo(task);
  }

  /**
   * 手动创建分班草稿：名单+排班齐套，strategy=manual，不立即建班。
   */
  @Transactional
  public Map<String, Object> manualPack(TrainDivisionManualPackReq req) {
    TrainThreeYearPlanEntity plan = requirePlan(req.getPlanId());
    assertFullReturnReady(plan);
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoLinked(req.getTrainInfoId(), plan.getId(), tenantId);

    int classMax = req.getClassMax() == null ? 50 : req.getClassMax();
    if (classMax < 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班额上限无效");
    }
    String prefix = StringUtils.hasText(req.getClassNamePrefix())
        ? req.getClassNamePrefix().trim()
        : (StringUtils.hasText(trainInfo.getCompanyName()) ? trainInfo.getCompanyName() + "-" : "培训-");

    LocalDate nowDate = LocalDate.now();
    int year = nowDate.getYear();
    int month = nowDate.getMonthValue();

    List<Map<String, Object>> classes = new ArrayList<>();
    int assignedCount = 0;
    List<Map<String, Object>> warnings = new ArrayList<>();

    if (req.getClasses() != null) {
      int seq = 1;
      for (TrainDivisionManualPackReq.ClassSeed seed : req.getClasses()) {
        String title = StringUtils.hasText(seed.getTitle())
            ? seed.getTitle().trim()
            : prefix + seq + "班";
        List<Map<String, Object>> members = new ArrayList<>();
        Set<Long> inThisClass = new HashSet<>();
        if (seed.getRosterIds() != null) {
          for (Long rosterId : seed.getRosterIds()) {
            if (rosterId == null || !inThisClass.add(rosterId)) {
              continue;
            }
            TrainPlanRosterEntity roster = rosterMapper.selectById(rosterId);
            if (roster == null || !Objects.equals(roster.getTenantId(), tenantId)
                || !Objects.equals(roster.getPlanId(), plan.getId())) {
              throw new BusinessException(ErrorCode.BAD_REQUEST, "名单人员无效: " + rosterId);
            }
            members.add(memberDraft(roster));
          }
        }
        if (members.size() > classMax) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              "班级「" + title + "」人数超过上限 " + classMax);
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tempId", "c-" + UUID.randomUUID().toString().substring(0, 8));
        row.put("title", title);
        row.put("group", "手动");
        row.put("nums", members.size());
        row.put("members", members);
        String jobName = members.isEmpty() ? null : textOf(members.get(0).get("jobName"));
        String levelName = members.isEmpty() ? null : textOf(members.get(0).get("levelName"));
        fillClassWorkMeta(row, jobName, levelName, buildWorkTypeLookup());
        classes.add(row);
        assignedCount += members.size();
        seq++;
      }
    }

    supersedeActivePackTasks(tenantId, trainInfo.getId());

    LocalDateTime now = LocalDateTime.now();
    TrainDivisionPackTaskEntity task = new TrainDivisionPackTaskEntity();
    task.setTenantId(tenantId);
    task.setPlanId(plan.getId());
    task.setTrainInfoId(trainInfo.getId());
    task.setCompanyId(trainInfo.getCompanyId() != null ? trainInfo.getCompanyId() : plan.getTargetOrgId());
    task.setStrategy("manual");
    task.setClassMax(classMax);
    task.setYear(year);
    task.setMonth(month);
    task.setClassNamePrefix(prefix);
    task.setStatus("draft");
    task.setClassCount(classes.size());
    task.setAssignedCount(assignedCount);
    task.setUnassignedCount(0);
    task.setClassesJson(toJson(classes));
    task.setUnassignedJson(toJson(List.of()));
    task.setWarningJson(toJson(warnings));
    task.setCreateTime(now);
    task.setUpdateTime(now);
    task.setDeleted(0);
    packTaskMapper.insert(task);
    return toPackTaskVo(task);
  }

  @Transactional
  public Map<String, Object> addPersonToPackClass(
      Long taskId, String tempId, TrainDivisionAddPersonReq req) {
    TrainDivisionPackTaskEntity task = requireEditableManualTask(taskId);
    if (!StringUtils.hasText(req.getPersonName())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "姓名不能为空");
    }
    List<Map<String, Object>> classes = readJsonList(task.getClassesJson());
    int classIdx = findClassIndex(classes, tempId);
    Map<String, Object> clazz = classes.get(classIdx);
    List<Map<String, Object>> members = new ArrayList<>(asMemberList(clazz.get("members")));
    int classMax = task.getClassMax() == null ? 50 : task.getClassMax();
    if (members.size() >= classMax) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级已达人数上限 " + classMax);
    }

    Set<Long> occupied = collectOccupiedRosterIdsInClass(classes, classIdx);
    PersonResolveResult resolved = resolveOrCreateRoster(
        task,
        req.getPersonName().trim(),
        trimToNull(req.getIdCard()),
        trimToNull(req.getPhone()),
        trimToNull(req.getJobName()),
        trimToNull(req.getLevelName()),
        trimToNull(req.getWorkshop()),
        trimToNull(req.getShiftTeam()),
        "manual_add",
        occupied);

    members.add(memberDraft(resolved.roster));
    clazz.put("members", members);
    clazz.put("nums", members.size());
    refreshClassWorkMetaFromMembers(clazz);
    classes.set(classIdx, clazz);
    persistPackClasses(task, classes, readJsonList(task.getUnassignedJson()), List.of());

    Map<String, Object> result = toPackTaskVo(task);
    result.put("rosterId", resolved.roster.getId());
    result.put("createdRoster", resolved.created);
    return result;
  }

  @Transactional
  public Map<String, Object> importMembersToPackClass(Long taskId, String tempId, MultipartFile file) {
    TrainDivisionPackTaskEntity task = requireEditableManualTask(taskId);
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传名单文件");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
    }
    List<TrainPlanRosterEntity> parsed = rosterParseService.parseEnterpriseRoster(bytes);
    if (parsed.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未解析到有效人员");
    }

    List<Map<String, Object>> classes = readJsonList(task.getClassesJson());
    int classIdx = findClassIndex(classes, tempId);
    Map<String, Object> clazz = classes.get(classIdx);
    List<Map<String, Object>> members = new ArrayList<>(asMemberList(clazz.get("members")));
    int classMax = task.getClassMax() == null ? 50 : task.getClassMax();
    if (members.size() + parsed.size() > classMax) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "导入后人数将超过班额上限 " + classMax
              + "（当前 " + members.size() + " + 导入 " + parsed.size() + "），请拆分后重试");
    }

    Set<Long> occupied = collectOccupiedRosterIdsInClass(classes, classIdx);
    List<Map<String, Object>> skipped = new ArrayList<>();
    List<Map<String, Object>> unmatched = new ArrayList<>();
    List<Map<String, Object>> warnings = new ArrayList<>(readJsonList(task.getWarningJson()));
    int added = 0;

    for (TrainPlanRosterEntity row : parsed) {
      if (!StringUtils.hasText(row.getPersonName())) {
        unmatched.add(Map.of(
            "personName", "",
            "idCard", row.getIdCard() == null ? "" : row.getIdCard(),
            "reason", "缺姓名"));
        continue;
      }
      try {
        PersonResolveResult resolved = resolveOrCreateRoster(
            task,
            row.getPersonName().trim(),
            trimToNull(row.getIdCard()),
            trimToNull(row.getPhone()),
            trimToNull(row.getJobName()),
            trimToNull(row.getLevelName()),
            trimToNull(row.getWorkshop()),
            trimToNull(row.getShiftTeam()),
            "class_import",
            occupied);
        occupied.add(resolved.roster.getId());
        members.add(memberDraft(resolved.roster));
        added++;
        if (resolved.created) {
          warnings.add(Map.of(
              "level", "INFO",
              "code", "ROSTER_AUTO_CREATED",
              "message", resolved.roster.getPersonName() + " 不在待分池，已自动入库并入班"));
        }
      } catch (BusinessException ex) {
        Map<String, Object> skip = new LinkedHashMap<>();
        skip.put("personName", row.getPersonName());
        skip.put("idCard", row.getIdCard());
        skip.put("reason", ex.getMessage());
        skipped.add(skip);
      }
    }

    clazz.put("members", members);
    clazz.put("nums", members.size());
    refreshClassWorkMetaFromMembers(clazz);
    classes.set(classIdx, clazz);
    persistPackClasses(task, classes, readJsonList(task.getUnassignedJson()), warnings);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("added", added);
    result.put("skipped", skipped);
    result.put("unmatched", unmatched);
    result.put("task", toPackTaskVo(task));
    return result;
  }

  @Transactional(readOnly = true)
  public byte[] buildMemberImportTemplate() {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("班级人员");
      Row header = sheet.createRow(0);
      String[] cols = {"姓名", "身份证", "手机", "工种", "等级", "车间/单位", "班组"};
      for (int i = 0; i < cols.length; i++) {
        header.createCell(i).setCellValue(cols[i]);
        sheet.setColumnWidth(i, 16 * 256);
      }
      Row sample = sheet.createRow(1);
      sample.createCell(0).setCellValue("张三");
      sample.createCell(1).setCellValue("110101199001011234");
      sample.createCell(2).setCellValue("13800000000");
      sample.createCell(3).setCellValue("电工");
      sample.createCell(4).setCellValue("高级");
      wb.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成导入模板失败");
    }
  }

  private TrainDivisionPackTaskEntity requireEditableManualTask(Long taskId) {
    TrainDivisionPackTaskEntity task = requirePackTask(taskId, true);
    if (!EDITABLE_PACK_STATUS.contains(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿或已驳回方案可加人");
    }
    if (!"manual".equalsIgnoreCase(task.getStrategy())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅手动组班方案支持此操作");
    }
    return task;
  }

  private static int findClassIndex(List<Map<String, Object>> classes, String tempId) {
    if (!StringUtils.hasText(tempId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级 tempId 不能为空");
    }
    for (int i = 0; i < classes.size(); i++) {
      Object tid = classes.get(i).get("tempId");
      if (tempId.equals(String.valueOf(tid))) {
        return i;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到班级：" + tempId);
  }

  private static boolean allowsMultiClassMembership(String strategy) {
    if (!StringUtils.hasText(strategy)) {
      return false;
    }
    String s = strategy.trim();
    return "manual".equalsIgnoreCase(s)
        || "by_job_level_slot".equalsIgnoreCase(s)
        || "by_attendable_slot".equalsIgnoreCase(s);
  }

  /** 跨班占用（一人一班策略）；excludeIdx 班级也会纳入，避免本班重复 */
  private static Set<Long> collectOccupiedRosterIds(List<Map<String, Object>> classes, int excludeIdx) {
    Set<Long> occupied = new HashSet<>();
    for (int i = 0; i < classes.size(); i++) {
      if (i == excludeIdx) {
        continue;
      }
      for (Map<String, Object> m : asMemberList(classes.get(i).get("members"))) {
        Long id = toLong(m.get("rosterId"));
        if (id != null) {
          occupied.add(id);
        }
      }
    }
    for (Map<String, Object> m : asMemberList(classes.get(excludeIdx).get("members"))) {
      Long id = toLong(m.get("rosterId"));
      if (id != null) {
        occupied.add(id);
      }
    }
    return occupied;
  }

  /** 仅本班占用：允许同一人出现在方案的其他班 */
  private static Set<Long> collectOccupiedRosterIdsInClass(List<Map<String, Object>> classes, int classIdx) {
    Set<Long> occupied = new HashSet<>();
    for (Map<String, Object> m : asMemberList(classes.get(classIdx).get("members"))) {
      Long id = toLong(m.get("rosterId"));
      if (id != null) {
        occupied.add(id);
      }
    }
    return occupied;
  }

  private void persistPackClasses(
      TrainDivisionPackTaskEntity task,
      List<Map<String, Object>> classes,
      List<Map<String, Object>> unassigned,
      List<Map<String, Object>> warnings) {
    int assigned = 0;
    for (Map<String, Object> c : classes) {
      assigned += asMemberList(c.get("members")).size();
    }
    if ("rejected".equals(task.getStatus())) {
      task.setStatus("draft");
      task.setRejectReason(null);
    }
    task.setClassesJson(toJson(classes));
    task.setUnassignedJson(toJson(unassigned == null ? List.of() : unassigned));
    task.setWarningJson(toJson(warnings == null ? List.of() : warnings));
    task.setClassCount(classes.size());
    task.setAssignedCount(assigned);
    task.setUnassignedCount(unassigned == null ? 0 : unassigned.size());
    task.setUpdateTime(LocalDateTime.now());
    packTaskMapper.updateById(task);
  }

  private void assertNotAlreadyInTrainInfo(Long tenantId, Long trainInfoId, TrainPlanRosterEntity roster) {
    if (!StringUtils.hasText(roster.getIdCard())) {
      return;
    }
    String activeTitle = releaseOrphanPeopleAndFindActiveClassTitle(
        tenantId, trainInfoId, roster.getIdCard().trim());
    if (activeTitle != null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          roster.getPersonName() + " 已在班级「" + activeTitle + "」中");
    }
  }

  /**
   * 判断身份证是否仍在「未删除」的正式班级中。
   * 若人员挂在已软删班级上（历史脏数据），自动清理人员记录，允许重新入草稿/建班。
   *
   * @return 所在有效班级名称；无人在有效班则返回 null
   */
  private String releaseOrphanPeopleAndFindActiveClassTitle(Long tenantId, Long trainInfoId, String idCard) {
    if (!StringUtils.hasText(idCard) || trainInfoId == null) {
      return null;
    }
    List<TrainPeopleListEntity> people = peopleListMapper.selectList(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getTrainId, trainInfoId)
            .eq(TrainPeopleListEntity::getIdCard, idCard.trim())
            .ne(TrainPeopleListEntity::getType, "teacher"));
    if (people.isEmpty()) {
      return null;
    }
    String activeTitle = null;
    for (TrainPeopleListEntity person : people) {
      Long classId = person.getTrainClassId();
      if (classId == null) {
        peopleListMapper.deleteById(person.getId());
        continue;
      }
      // train_info_class 带 @TableLogic：已删班级 selectById 为 null
      TrainInfoClassEntity clazz = trainInfoClassMapper.selectById(classId);
      if (clazz == null) {
        peopleListMapper.deleteById(person.getId());
        continue;
      }
      if (activeTitle == null) {
        activeTitle = StringUtils.hasText(clazz.getTitle()) ? clazz.getTitle() : ("#" + clazz.getId());
      }
    }
    return activeTitle;
  }

  /**
   * 匹配待分池或自动入库，再供入班使用。
   * 手动/时段多班策略：可匹配已分班名单，且不因「已在其他正式班」拦截。
   */
  private PersonResolveResult resolveOrCreateRoster(
      TrainDivisionPackTaskEntity task,
      String personName,
      String idCard,
      String phone,
      String jobName,
      String levelName,
      String workshop,
      String shiftTeam,
      String source,
      Set<Long> occupiedRosterIds) {
    Long tenantId = task.getTenantId();
    Long planId = task.getPlanId();
    Long trainInfoId = task.getTrainInfoId();
    boolean multiClass = allowsMultiClassMembership(task.getStrategy());

    TrainPlanRosterEntity matched = findRosterForPack(tenantId, planId, personName, idCard, multiClass);

    if (matched != null) {
      if (occupiedRosterIds.contains(matched.getId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            matched.getPersonName() + " 已在本班中");
      }
      if (!multiClass) {
        assertNotAlreadyInTrainInfo(tenantId, trainInfoId, matched);
      }
      // 补全空字段
      boolean dirty = false;
      if (!StringUtils.hasText(matched.getPhone()) && StringUtils.hasText(phone)) {
        matched.setPhone(phone);
        dirty = true;
      }
      if (!StringUtils.hasText(matched.getJobName()) && StringUtils.hasText(jobName)) {
        matched.setJobName(jobName);
        dirty = true;
      }
      if (!StringUtils.hasText(matched.getLevelName()) && StringUtils.hasText(levelName)) {
        matched.setLevelName(levelName);
        dirty = true;
      }
      if (!StringUtils.hasText(matched.getWorkshop()) && StringUtils.hasText(workshop)) {
        matched.setWorkshop(workshop);
        dirty = true;
      }
      if (!StringUtils.hasText(matched.getShiftTeam()) && StringUtils.hasText(shiftTeam)) {
        matched.setShiftTeam(shiftTeam);
        dirty = true;
      }
      if (dirty) {
        matched.setUpdateTime(LocalDateTime.now());
        rosterMapper.updateById(matched);
      }
      return new PersonResolveResult(matched, false);
    }

    // 池外新人：自动入库
    TrainPlanRosterEntity created = new TrainPlanRosterEntity();
    created.setTenantId(tenantId);
    created.setPlanId(planId);
    created.setTrainInfoId(trainInfoId);
    created.setSource(source);
    created.setPersonName(personName);
    created.setIdCard(idCard);
    created.setPhone(phone);
    created.setJobName(jobName);
    created.setLevelName(levelName);
    created.setWorkshop(workshop);
    created.setShiftTeam(shiftTeam);
    created.setAssignStatus(0);
    created.setDeleted(0);
    LocalDateTime now = LocalDateTime.now();
    created.setCreateTime(now);
    created.setUpdateTime(now);
    if (!multiClass) {
      assertNotAlreadyInTrainInfo(tenantId, trainInfoId, created);
    }
    rosterMapper.insert(created);
    return new PersonResolveResult(created, true);
  }

  private TrainPlanRosterEntity findRosterForPack(
      Long tenantId, Long planId, String personName, String idCard, boolean multiClass) {
    if (StringUtils.hasText(idCard)) {
      String idKey = idCard.trim();
      List<TrainPlanRosterEntity> hits = selectRosterByIdCard(tenantId, planId, idKey, !multiClass);
      if (hits.isEmpty()) {
        LambdaQueryWrapper<TrainPlanRosterEntity> q = new LambdaQueryWrapper<TrainPlanRosterEntity>()
            .eq(TrainPlanRosterEntity::getTenantId, tenantId)
            .eq(TrainPlanRosterEntity::getPlanId, planId)
            .eq(TrainPlanRosterEntity::getDeleted, 0)
            .apply("UPPER(TRIM(id_card)) = {0}", idKey.toUpperCase(Locale.ROOT))
            .orderByAsc(TrainPlanRosterEntity::getAssignStatus)
            .orderByAsc(TrainPlanRosterEntity::getId)
            .last("LIMIT 5");
        if (!multiClass) {
          q.eq(TrainPlanRosterEntity::getAssignStatus, 0);
        }
        hits = rosterMapper.selectList(q);
      }
      return hits.isEmpty() ? null : hits.get(0);
    }

    LambdaQueryWrapper<TrainPlanRosterEntity> q = new LambdaQueryWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, planId)
        .eq(TrainPlanRosterEntity::getDeleted, 0)
        .eq(TrainPlanRosterEntity::getPersonName, personName)
        .orderByAsc(TrainPlanRosterEntity::getAssignStatus)
        .orderByAsc(TrainPlanRosterEntity::getId)
        .last("LIMIT 5");
    if (!multiClass) {
      q.eq(TrainPlanRosterEntity::getAssignStatus, 0);
    }
    List<TrainPlanRosterEntity> hits = rosterMapper.selectList(q);
    if (hits.size() > 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "姓名「" + personName + "」在名单中有多人，请补充身份证");
    }
    return hits.isEmpty() ? null : hits.get(0);
  }

  private List<TrainPlanRosterEntity> selectRosterByIdCard(
      Long tenantId, Long planId, String idKey, boolean pendingOnly) {
    LambdaQueryWrapper<TrainPlanRosterEntity> q = new LambdaQueryWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, planId)
        .eq(TrainPlanRosterEntity::getDeleted, 0)
        .eq(TrainPlanRosterEntity::getIdCard, idKey)
        .orderByAsc(TrainPlanRosterEntity::getAssignStatus)
        .orderByAsc(TrainPlanRosterEntity::getId)
        .last("LIMIT 5");
    if (pendingOnly) {
      q.eq(TrainPlanRosterEntity::getAssignStatus, 0);
    }
    return rosterMapper.selectList(q);
  }

  private static String trimToNull(String v) {
    if (!StringUtils.hasText(v)) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static final class PersonResolveResult {
    final TrainPlanRosterEntity roster;
    final boolean created;

    PersonResolveResult(TrainPlanRosterEntity roster, boolean created) {
      this.roster = roster;
      this.created = created;
    }
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

  private static String resolveJobLevelGroupKey(TrainPlanRosterEntity r, boolean requireJobAndLevel) {
    String job = StringUtils.hasText(r.getJobName()) ? r.getJobName().trim() : "未填工种";
    String level = StringUtils.hasText(r.getLevelName()) ? r.getLevelName().trim() : "未填等级";
    if (requireJobAndLevel) {
      // 调用方已过滤空值；此处兜底
      job = r.getJobName().trim();
      level = r.getLevelName().trim();
    }
    return job + "-" + level;
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
    m.put("workshop", r.getWorkshop());
    m.put("jobName", r.getJobName());
    m.put("levelName", r.getLevelName());
    m.put("shiftTeam", r.getShiftTeam());
    m.put("reason", reason);
    return m;
  }

  private static Map<String, Object> memberDraft(TrainPlanRosterEntity roster) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("rosterId", roster.getId());
    m.put("personName", roster.getPersonName());
    m.put("idCard", roster.getIdCard());
    m.put("workshop", roster.getWorkshop());
    m.put("jobName", roster.getJobName());
    m.put("levelName", roster.getLevelName());
    m.put("shiftTeam", roster.getShiftTeam());
    m.put("phone", roster.getPhone());
    return m;
  }

  /**
   * 工种+等级 × 每天上午/下午可上课时段班（复用 L2 ScheduleAlgorithmService）。
   * 产出：{工种}-{等级}-{上午N班/下午N班}，同人可出现在多个时段班草稿中。
   */
  private SlotPackResult buildJobLevelSlotClasses(
      TrainThreeYearPlanEntity plan,
      List<TrainPlanRosterEntity> assignable,
      String prefix,
      int classMax,
      int year,
      int month,
      List<Map<String, Object>> warnings,
      WorkTypeLookup workTypeLookup) {
    if (plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "企业尚未上传排班表，无法按时段分班");
    }
    byte[] bytes = fileApplicationService.readFileBytesById(plan.getEnterpriseShiftFileId());
    Map<String, ShiftTimeDef> shiftMap = ShiftAssignDefaults.defaultShiftTimeMap();
    ensureShiftAlias(shiftMap, "早", true, true, false);
    ensureShiftAlias(shiftMap, "早班", true, true, false);

    ScheduleParseResult parse;
    try {
      parse = excelScheduleParseService.parseBytes(
          bytes,
          plan.getEnterpriseShiftFileName(),
          String.valueOf(plan.getTargetOrgId()),
          year,
          month,
          shiftMap);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析排班表失败：" + ex.getMessage());
    }
    if (parse.getPersonDaySlots() == null || parse.getPersonDaySlots().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "排班表未解析到有效人日数据");
    }

    EffectiveSchedulingConfig cfg = ShiftAssignDefaults.config();
    cfg.setClassMax(classMax);
    if (cfg.getClassMin() > classMax) {
      cfg.setClassMin(Math.max(1, classMax / 2));
    }
    ClassAssignResult assign = scheduleAlgorithmService.runClassAssign(parse, cfg);

    Map<String, TrainPlanRosterEntity> byId = new HashMap<>();
    Map<String, TrainPlanRosterEntity> byName = new HashMap<>();
    Map<String, TrainPlanRosterEntity> byEmp = new HashMap<>();
    for (TrainPlanRosterEntity r : assignable) {
      if (StringUtils.hasText(r.getIdCard())) {
        String id = r.getIdCard().trim().toUpperCase(Locale.ROOT);
        byId.putIfAbsent(id, r);
        // 排班表偶发把证件号当工号列
        byEmp.putIfAbsent(id, r);
      }
      if (StringUtils.hasText(r.getPersonName())) {
        String raw = r.getPersonName().trim();
        byName.putIfAbsent(raw, r);
        String norm = normalizePersonName(raw);
        if (StringUtils.hasText(norm)) {
          byName.putIfAbsent(norm, r);
        }
      }
    }

    // algoClassName -> job|level -> LinkedHashMap rosterId -> roster
    Map<String, Map<String, Map<Long, TrainPlanRosterEntity>>> buckets = new LinkedHashMap<>();
    Set<Long> coveredRosterIds = new HashSet<>();
    int scheduleAssigned = assign.getPersonDayClass() == null ? 0 : assign.getPersonDayClass().size();
    int matchMiss = 0;
    List<String> sampleMiss = new ArrayList<>();

    if (assign.getPersonDayClass() != null) {
      for (Map.Entry<String, Map<LocalDate, Map<String, String>>> pe : assign.getPersonDayClass().entrySet()) {
        String personKey = pe.getKey();
        TrainPlanRosterEntity roster = matchRosterForSlot(
            personKey,
            assign.getPersonKeyToName() != null ? assign.getPersonKeyToName().get(personKey) : null,
            assign.getPersonKeyToEmployeeNo() != null ? assign.getPersonKeyToEmployeeNo().get(personKey) : null,
            byId, byName, byEmp);
        if (roster == null) {
          matchMiss++;
          if (sampleMiss.size() < 5) {
            String nm = assign.getPersonKeyToName() != null
                ? assign.getPersonKeyToName().get(personKey) : null;
            sampleMiss.add(StringUtils.hasText(nm) ? nm + "(" + personKey + ")" : personKey);
          }
          continue;
        }
        coveredRosterIds.add(roster.getId());
        String jobLevel = jobLevelKey(roster);
        Set<String> algoNames = new LinkedHashSet<>();
        for (Map<String, String> slots : pe.getValue().values()) {
          if (slots == null) continue;
          for (String algo : slots.values()) {
            if (StringUtils.hasText(algo)) algoNames.add(algo.trim());
          }
        }
        for (String algoName : algoNames) {
          buckets
              .computeIfAbsent(algoName, k -> new LinkedHashMap<>())
              .computeIfAbsent(jobLevel, k -> new LinkedHashMap<>())
              .putIfAbsent(roster.getId(), roster);
        }
      }
    }

    List<Map<String, Object>> classes = new ArrayList<>();
    int assignedSeats = 0;
    List<String> algoOrder = new ArrayList<>(buckets.keySet());
    algoOrder.sort(Comparator.naturalOrder());
    for (String algoName : algoOrder) {
      Map<String, Map<Long, TrainPlanRosterEntity>> byJob = buckets.get(algoName);
      List<String> jobKeys = new ArrayList<>(byJob.keySet());
      jobKeys.sort(Comparator.naturalOrder());
      for (String jobLevel : jobKeys) {
        List<TrainPlanRosterEntity> members = new ArrayList<>(byJob.get(jobLevel).values());
        members.sort(Comparator.comparing(
            x -> x.getPersonName() == null ? "" : x.getPersonName(),
            String.CASE_INSENSITIVE_ORDER));
        // 再按班额切开（算法班内同工种等级仍可能超过 classMax）
        int seq = 1;
        for (int i = 0; i < members.size(); i += classMax) {
          List<TrainPlanRosterEntity> chunk = members.subList(i, Math.min(i + classMax, members.size()));
          String group = jobLevel + "/" + algoName;
          String title = prefix + sanitizeTitle(jobLevel) + "-" + sanitizeTitle(algoName)
              + (members.size() > classMax ? ("-" + seq) : "");
          List<Map<String, Object>> memberRows = new ArrayList<>();
          for (TrainPlanRosterEntity roster : chunk) {
            memberRows.add(memberDraft(roster));
          }
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("tempId", "c-" + UUID.randomUUID().toString().substring(0, 8));
          row.put("title", title);
          row.put("group", group);
          row.put("algoClassName", algoName);
          row.put("jobLevel", jobLevel);
          row.put("nums", memberRows.size());
          row.put("members", memberRows);
          String jobName = chunk.isEmpty() ? null : chunk.get(0).getJobName();
          String levelName = chunk.isEmpty() ? null : chunk.get(0).getLevelName();
          fillClassWorkMeta(row, jobName, levelName, workTypeLookup);
          classes.add(row);
          assignedSeats += memberRows.size();
          seq++;
        }
      }
    }

    if (classes.isEmpty()) {
      int schedulePersons = assign.getPersonKeyToName() != null ? assign.getPersonKeyToName().size() : 0;
      int algoUnassigned = assign.getUnassignedPersons() != null ? assign.getUnassignedPersons().size() : 0;
      String failReason;
      if (scheduleAssigned <= 0) {
        failReason = "排班算法未排出任何上午/下午班（解析到 "
            + schedulePersons + " 人，其中未排入 " + algoUnassigned
            + " 人；请核对所选年月 " + year + "-" + String.format("%02d", month)
            + " 与排班表月份、班次是否可上课）";
      } else {
        failReason = "排班已排入 " + scheduleAssigned + " 人，与名单匹配成功 0 人（未匹配 "
            + matchMiss + "）"
            + (sampleMiss.isEmpty() ? "" : "，样例：" + String.join("、", sampleMiss))
            + "；请核对名单姓名与排班表是否同一批人";
      }
      return new SlotPackResult(List.of(), 0, List.of(), failReason);
    }

    List<Map<String, Object>> extraUnassigned = new ArrayList<>();
    for (TrainPlanRosterEntity r : assignable) {
      if (!coveredRosterIds.contains(r.getId())) {
        String reason = "排班算法未排入任何上午/下午班（全休/夜班/未识别班次等）";
        if (assign.getUnassignedReasons() != null) {
          // try match by name
          String rk = null;
          if (StringUtils.hasText(r.getIdCard())
              && assign.getUnassignedReasons().containsKey(r.getIdCard().trim())) {
            rk = r.getIdCard().trim();
          } else if (StringUtils.hasText(r.getPersonName())
              && assign.getUnassignedReasons().containsKey(r.getPersonName().trim())) {
            rk = r.getPersonName().trim();
          }
          if (rk != null && StringUtils.hasText(assign.getUnassignedReasons().get(rk))) {
            reason = assign.getUnassignedReasons().get(rk);
          }
        }
        extraUnassigned.add(unassignedRow(r, reason));
      }
    }

    if (assign.getUnassignedPersons() != null && !assign.getUnassignedPersons().isEmpty()) {
      warnings.add(Map.of(
          "level", "INFO",
          "code", "SLOT_UNASSIGNED",
          "message", "排班算法未排入人员 " + assign.getUnassignedPersons().size() + " 人（详见未分入列表）"));
    }
    warnings.add(Map.of(
        "level", "INFO",
        "code", "SLOT_STRATEGY",
        "message", "已按工种/等级 + 上午/下午时段生成 " + classes.size() + " 个班级草稿（同人可属多个时段班）"));

    return new SlotPackResult(classes, assignedSeats, extraUnassigned, null);
  }

  private static String jobLevelKey(TrainPlanRosterEntity r) {
    String job = StringUtils.hasText(r.getJobName()) ? r.getJobName().trim() : "未填工种";
    String level = StringUtils.hasText(r.getLevelName()) ? r.getLevelName().trim() : "未填等级";
    return job + "-" + level;
  }

  /** 去掉空白/全角空格等，提高名单与排班表姓名匹配率 */
  private static String normalizePersonName(String name) {
    if (name == null) return "";
    String s = name.trim()
        .replace('\u00A0', ' ')
        .replace('\u3000', ' ')
        .replace("・", "")
        .replace("·", "")
        .replace(".", "")
        .replace("．", "");
    s = s.replaceAll("\\s+", "");
    return s;
  }

  private static TrainPlanRosterEntity matchRosterForSlot(
      String personKey,
      String name,
      String employeeNo,
      Map<String, TrainPlanRosterEntity> byId,
      Map<String, TrainPlanRosterEntity> byName,
      Map<String, TrainPlanRosterEntity> byEmp) {
    if (StringUtils.hasText(personKey)) {
      String pk = personKey.trim();
      TrainPlanRosterEntity hit = byId.get(pk.toUpperCase(Locale.ROOT));
      if (hit != null) return hit;
      hit = byEmp.get(pk.toUpperCase(Locale.ROOT));
      if (hit != null) return hit;
      hit = byName.get(pk);
      if (hit != null) return hit;
      hit = byName.get(normalizePersonName(pk));
      if (hit != null) return hit;
    }
    if (StringUtils.hasText(employeeNo)) {
      String emp = employeeNo.trim().toUpperCase(Locale.ROOT);
      TrainPlanRosterEntity hit = byEmp.get(emp);
      if (hit != null) return hit;
      hit = byId.get(emp);
      if (hit != null) return hit;
    }
    if (StringUtils.hasText(name)) {
      String raw = name.trim();
      TrainPlanRosterEntity hit = byName.get(raw);
      if (hit != null) return hit;
      return byName.get(normalizePersonName(raw));
    }
    return null;
  }

  private static void ensureShiftAlias(
      Map<String, ShiftTimeDef> map, String name, boolean am, boolean pm, boolean night) {
    String key = ShiftTimeParser.normalizeShiftNameForLookup(name);
    if (map.containsKey(key) || map.containsKey(name)) return;
    ShiftTimeDef def = new ShiftTimeDef();
    def.setShiftName(name);
    def.setAllowMorning(am);
    def.setAllowAfternoon(pm);
    def.setNextDayBlock(night ? 3 : 0);
    def.setCrossDay(night);
    map.put(key, def);
    map.put(name, def);
  }

  private static final class SlotPackResult {
    final List<Map<String, Object>> classes;
    final int assignedCount;
    final List<Map<String, Object>> extraUnassigned;
    final String failReason;

    SlotPackResult(
        List<Map<String, Object>> classes,
        int assignedCount,
        List<Map<String, Object>> extraUnassigned,
        String failReason) {
      this.classes = classes;
      this.assignedCount = assignedCount;
      this.extraUnassigned = extraUnassigned;
      this.failReason = failReason;
    }
  }

  private void supersedeActivePackTasks(Long tenantId, Long trainInfoId) {
    List<TrainDivisionPackTaskEntity> actives = packTaskMapper.selectList(
        new LambdaQueryWrapper<TrainDivisionPackTaskEntity>()
            .eq(TrainDivisionPackTaskEntity::getTenantId, tenantId)
            .eq(TrainDivisionPackTaskEntity::getTrainInfoId, trainInfoId)
            .in(TrainDivisionPackTaskEntity::getStatus, ACTIVE_PACK_STATUS));
    LocalDateTime now = LocalDateTime.now();
    for (TrainDivisionPackTaskEntity old : actives) {
      old.setStatus("superseded");
      old.setUpdateTime(now);
      packTaskMapper.updateById(old);
    }
  }

  private TrainDivisionPackTaskEntity requirePackTask(Long taskId, boolean orgManage) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainDivisionPackTaskEntity task = packTaskMapper.selectById(taskId);
    if (task == null || !Objects.equals(task.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "分班方案不存在");
    }
    Long companyFilter = scopeService.resolveCompanyFilter(null);
    if (companyFilter != null && !Objects.equals(companyFilter, task.getCompanyId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该分班方案");
    }
    if (orgManage) {
      scopeService.requireOrgOperator();
    }
    return task;
  }

  private void assertEnterpriseCanReviewPack(TrainDivisionPackTaskEntity task) {
    TrainingOperatorScopeService.Scope scope = scopeService.currentScope();
    if (!scope.isEnterpriseViewer()) {
      scopeService.requireOrgOperator();
      return;
    }
    if (scope.orgId() == null || task.getCompanyId() == null || !scope.orgId().equals(task.getCompanyId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅目标企业可审核该分班方案");
    }
  }

  private void applyPackDraft(
      TrainDivisionPackTaskEntity task,
      List<TrainDivisionPackPatchReq.ClassDraft> classDrafts,
      List<TrainDivisionPackPatchReq.UnassignedDraft> unassignedDrafts) {
    Long tenantId = task.getTenantId();
    boolean allowMultiClass = allowsMultiClassMembership(task.getStrategy());
    Set<Long> seen = new HashSet<>();
    List<Map<String, Object>> classes = new ArrayList<>();
    int assigned = 0;
    int classMax = task.getClassMax() == null ? 50 : task.getClassMax();

    for (TrainDivisionPackPatchReq.ClassDraft draft : classDrafts) {
      if (!StringUtils.hasText(draft.getTitle())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "班级名称不能为空");
      }
      List<Map<String, Object>> members = new ArrayList<>();
      Set<Long> inThisClass = new HashSet<>();
      if (draft.getMembers() != null) {
        for (TrainDivisionPackPatchReq.MemberDraft m : draft.getMembers()) {
          if (m.getRosterId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "学员 rosterId 不能为空");
          }
          if (!inThisClass.add(m.getRosterId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "同一班级内学员不可重复");
          }
          if (!allowMultiClass && !seen.add(m.getRosterId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "学员不可重复分入多个班");
          }
          seen.add(m.getRosterId());
          TrainPlanRosterEntity roster = rosterMapper.selectById(m.getRosterId());
          if (roster == null || !Objects.equals(roster.getTenantId(), tenantId)
              || !Objects.equals(roster.getPlanId(), task.getPlanId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "名单人员无效: " + m.getRosterId());
          }
          if (roster.getAssignStatus() != null && roster.getAssignStatus() != 0 && !allowMultiClass) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                roster.getPersonName() + " 已分班，无法放入草稿");
          }
          Map<String, Object> row = memberDraft(roster);
          members.add(row);
        }
      }
      if (members.size() > classMax) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "班级「" + draft.getTitle().trim() + "」人数超过上限 " + classMax);
      }
      Map<String, Object> c = new LinkedHashMap<>();
      c.put("tempId", StringUtils.hasText(draft.getTempId())
          ? draft.getTempId()
          : "c-" + UUID.randomUUID().toString().substring(0, 8));
      c.put("title", draft.getTitle().trim());
      c.put("group", draft.getGroup());
      c.put("nums", members.size());
      c.put("members", members);
      String jobName = StringUtils.hasText(draft.getJobName())
          ? draft.getJobName().trim()
          : (members.isEmpty() ? null : textOf(members.get(0).get("jobName")));
      String levelName = StringUtils.hasText(draft.getLevelName())
          ? draft.getLevelName().trim()
          : (members.isEmpty() ? null : textOf(members.get(0).get("levelName")));
      Long explicitWorkTypeId = draft.getWorkTypeId() != null && draft.getWorkTypeId() > 0
          ? draft.getWorkTypeId()
          : null;
      if (explicitWorkTypeId != null) {
        ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(explicitWorkTypeId.intValue());
        if (wt != null && StringUtils.hasText(wt.getTitle())) {
          jobName = wt.getTitle().trim();
        }
      }
      fillClassWorkMeta(c, explicitWorkTypeId, jobName, levelName, buildWorkTypeLookup());
      classes.add(c);
      assigned += members.size();
    }

    List<Map<String, Object>> unassigned = new ArrayList<>();
    if (unassignedDrafts != null) {
      for (TrainDivisionPackPatchReq.UnassignedDraft u : unassignedDrafts) {
        if (u.getRosterId() != null && seen.contains(u.getRosterId())) {
          continue;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rosterId", u.getRosterId());
        row.put("personName", u.getPersonName());
        row.put("idCard", u.getIdCard());
        row.put("workshop", u.getWorkshop());
        row.put("jobName", u.getJobName());
        row.put("levelName", u.getLevelName());
        row.put("shiftTeam", u.getShiftTeam());
        row.put("reason", StringUtils.hasText(u.getReason()) ? u.getReason() : "未分入班级");
        unassigned.add(row);
      }
    }

    task.setClassesJson(toJson(classes));
    task.setUnassignedJson(toJson(unassigned));
    task.setClassCount(classes.size());
    task.setAssignedCount(assigned);
    task.setUnassignedCount(unassigned.size());
  }

  private Map<String, Object> materializePackTask(TrainDivisionPackTaskEntity task) {
    Long tenantId = task.getTenantId();
    TrainInfoListEntity trainInfo = requireTrainInfoLinked(task.getTrainInfoId(), task.getPlanId(), tenantId);
    List<Map<String, Object>> classes = readJsonList(task.getClassesJson());
    if (classes.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "方案中无班级，无法创建");
    }
    boolean multiClass = allowsMultiClassMembership(task.getStrategy());
    Long orgId = resolvePlatformOrgIdForPack(task);
    Long companyId = trainInfo.getCompanyId() != null ? trainInfo.getCompanyId() : task.getCompanyId();
    PlatformUserEnsureService.SyncStats syncStats = new PlatformUserEnsureService.SyncStats();

    LocalDateTime now = LocalDateTime.now();
    var user = SecurityContext.currentUser();
    List<Map<String, Object>> created = new ArrayList<>();
    int people = 0;
    WorkTypeLookup workTypeLookup = buildWorkTypeLookup();

    for (Map<String, Object> c : classes) {
      List<Map<String, Object>> members = asMemberList(c.get("members"));
      if (members.isEmpty()) continue;
      String title = String.valueOf(c.get("title"));
      TrainInfoClassEntity clazz = new TrainInfoClassEntity();
      clazz.setTrainInfoId(trainInfo.getId());
      clazz.setTitle(title);
      clazz.setCompanyId(companyId);
      applyWorkTypeToClass(clazz, c, trainInfo, workTypeLookup);
      clazz.setNums(members.size());
      clazz.setAddtime((int) (System.currentTimeMillis() / 1000));
      clazz.setDeleted(0);
      clazz.setCompanyConfirmStatus(1);
      clazz.setCompanyConfirmTime(now);
      clazz.setCompanyConfirmUserId(user.getUserId());
      clazz.setCompanyConfirmUsername(user.getUsername());
      trainInfoClassMapper.insert(clazz);

      int added = 0;
      for (Map<String, Object> m : members) {
        Long rosterId = toLong(m.get("rosterId"));
        if (rosterId == null) continue;
        TrainPlanRosterEntity roster = rosterMapper.selectById(rosterId);
        if (roster == null || !Objects.equals(roster.getTenantId(), tenantId)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "名单人员不存在: " + rosterId);
        }
        if (!multiClass && roster.getAssignStatus() != null && roster.getAssignStatus() != 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST,
              roster.getPersonName() + " 已分入其他班级，请重新调整方案");
        }
        if (StringUtils.hasText(roster.getIdCard())) {
          if (multiClass) {
            Long exists = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainPeopleListEntity>()
                .eq(TrainPeopleListEntity::getTenantId, tenantId)
                .eq(TrainPeopleListEntity::getTrainId, trainInfo.getId())
                .eq(TrainPeopleListEntity::getIdCard, roster.getIdCard().trim())
                .eq(TrainPeopleListEntity::getTrainClassId, clazz.getId())
                .ne(TrainPeopleListEntity::getType, "teacher"));
            if (exists != null && exists > 0) {
              continue;
            }
          } else {
            String activeTitle = releaseOrphanPeopleAndFindActiveClassTitle(
                tenantId, trainInfo.getId(), roster.getIdCard().trim());
            if (activeTitle != null) {
              throw new BusinessException(ErrorCode.BAD_REQUEST,
                  roster.getPersonName() + " 已在班级「" + activeTitle + "」中");
            }
          }
        }

        PlatformUserEnsureService.EnsureResult ensure = platformUserEnsureService.ensureFromDivisionPerson(
            roster.getPersonName(),
            roster.getIdCard(),
            roster.getPhone(),
            companyId,
            orgId);
        syncStats.accept(
            ensure,
            roster.getPersonName(),
            PlatformUserEnsureService.normalizeIdCard(roster.getIdCard()));

        TrainPeopleListEntity peopleRow = new TrainPeopleListEntity();
        peopleRow.setTenantId(tenantId);
        peopleRow.setTrainId(trainInfo.getId());
        peopleRow.setTrainClassId(clazz.getId());
        peopleRow.setType("student");
        peopleRow.setRealname(roster.getPersonName());
        peopleRow.setPhone(roster.getPhone());
        peopleRow.setIdCard(roster.getIdCard());
        peopleRow.setPlatformUserId(ensure.platformUserId());
        peopleRow.setDeleted(0);
        peopleRow.setCreateTime(now);
        peopleListMapper.insert(peopleRow);

        if (roster.getAssignStatus() == null || roster.getAssignStatus() == 0) {
          roster.setAssignStatus(1);
          roster.setTrainClassId(clazz.getId());
          roster.setTrainInfoId(trainInfo.getId());
          roster.setUpdateTime(now);
          rosterMapper.updateById(roster);
        }
        added++;
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("classId", clazz.getId());
      row.put("title", clazz.getTitle());
      row.put("nums", added);
      created.add(row);
      people += added;
    }

    if (created.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可创建的班级");
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classCount", created.size());
    out.put("peopleCount", people);
    out.put("classes", created);
    out.put("platformUserSync", syncStats.toMap());
    return out;
  }

  /**
   * 学员信息列表按培训机构 orgId 过滤；企业确认时不能用企业账号 orgId。
   * 优先取提交方案的机构账号 orgId，其次当前机构账号，最后回退 tenantId。
   */
  private Long resolvePlatformOrgIdForPack(TrainDivisionPackTaskEntity task) {
    if (task.getSubmitUserId() != null) {
      UserEntity submitter = userMapper.selectById(task.getSubmitUserId());
      if (submitter != null && submitter.getOrgId() != null && submitter.getOrgId() > 0) {
        return submitter.getOrgId();
      }
    }
    TrainingOperatorScopeService.Scope scope = scopeService.currentScope();
    if (scope.isOrgOperator() && scope.orgId() != null && scope.orgId() > 0) {
      return scope.orgId();
    }
    return task.getTenantId() != null ? task.getTenantId() : 0L;
  }

  private Map<String, Object> toPackTaskVo(TrainDivisionPackTaskEntity task) {
    Map<String, Object> result = toPackTaskSummary(task);
    result.put("classes", readJsonList(task.getClassesJson()));
    result.put("unassigned", readJsonList(task.getUnassignedJson()));
    result.put("warnings", readJsonList(task.getWarningJson()));
    return result;
  }

  private Map<String, Object> toPackTaskSummary(TrainDivisionPackTaskEntity task) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("taskId", task.getId());
    result.put("planId", task.getPlanId());
    result.put("trainInfoId", task.getTrainInfoId());
    result.put("companyId", task.getCompanyId());
    result.put("strategy", task.getStrategy());
    result.put("classMax", task.getClassMax());
    result.put("year", task.getYear());
    result.put("month", task.getMonth());
    result.put("classNamePrefix", task.getClassNamePrefix());
    result.put("status", task.getStatus());
    result.put("statusLabel", packStatusLabel(task.getStatus()));
    result.put("classCount", task.getClassCount());
    result.put("assignedCount", task.getAssignedCount());
    result.put("unassignedCount", task.getUnassignedCount());
    result.put("rejectReason", task.getRejectReason());
    result.put("submitUsername", task.getSubmitUsername());
    result.put("confirmUsername", task.getConfirmUsername());
    result.put("confirmTime", task.getConfirmTime());
    result.put("createTime", task.getCreateTime());
    result.put("updateTime", task.getUpdateTime());
    return result;
  }

  private static String packStatusLabel(String status) {
    if (status == null) return "";
    return switch (status) {
      case "draft" -> "草稿（待提交）";
      case "pending" -> "待企业审核";
      case "rejected" -> "企业已驳回";
      case "materialized" -> "已通过并建班";
      case "superseded" -> "已作废";
      default -> status;
    };
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化分班方案失败");
    }
  }

  private List<Map<String, Object>> readJsonList(String json) {
    if (!StringUtils.hasText(json)) return new ArrayList<>();
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析分班方案失败");
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> asMemberList(Object raw) {
    if (raw instanceof List<?> list) {
      List<Map<String, Object>> out = new ArrayList<>();
      for (Object o : list) {
        if (o instanceof Map<?, ?> m) {
          out.add((Map<String, Object>) m);
        }
      }
      return out;
    }
    return List.of();
  }

  private static Long toLong(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.longValue();
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (Exception e) {
      return null;
    }
  }

  private static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(v).trim());
    } catch (Exception e) {
      return null;
    }
  }

  private static String textOf(Object v) {
    if (v == null) return null;
    String s = String.valueOf(v).trim();
    return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
  }

  /** 草稿班级写入工种文本，并尽量解析 exam_work_type.id / 等级数字。 */
  private static void fillClassWorkMeta(
      Map<String, Object> row, String jobName, String levelName, WorkTypeLookup lookup) {
    fillClassWorkMeta(row, null, jobName, levelName, lookup);
  }

  /** preferredWorkTypeId 优先于按名称解析（与开班准备绑定 exam_work_type.id 一致）。 */
  private static void fillClassWorkMeta(
      Map<String, Object> row,
      Long preferredWorkTypeId,
      String jobName,
      String levelName,
      WorkTypeLookup lookup) {
    if (StringUtils.hasText(jobName)) {
      row.put("jobName", jobName.trim());
    }
    if (StringUtils.hasText(levelName)) {
      row.put("levelName", levelName.trim());
    }
    Long workTypeId = preferredWorkTypeId != null && preferredWorkTypeId > 0
        ? preferredWorkTypeId
        : (lookup != null ? lookup.resolveId(jobName) : null);
    if (workTypeId != null) {
      row.put("workTypeId", workTypeId);
    }
    Integer level = resolveWorkTypeLevel(levelName);
    if (level != null) {
      row.put("workTypeLevel", level);
    }
  }

  private void refreshClassWorkMetaFromMembers(Map<String, Object> clazz) {
    List<Map<String, Object>> members = asMemberList(clazz.get("members"));
    if (members.isEmpty()) {
      return;
    }
    String jobName = textOf(clazz.get("jobName"));
    String levelName = textOf(clazz.get("levelName"));
    if (!StringUtils.hasText(jobName)) {
      jobName = textOf(members.get(0).get("jobName"));
    }
    if (!StringUtils.hasText(levelName)) {
      levelName = textOf(members.get(0).get("levelName"));
    }
    Long preferredId = toLong(clazz.get("workTypeId"));
    fillClassWorkMeta(clazz, preferredId, jobName, levelName, buildWorkTypeLookup());
  }

  private void applyWorkTypeToClass(
      TrainInfoClassEntity clazz,
      Map<String, Object> classDraft,
      TrainInfoListEntity trainInfo,
      WorkTypeLookup lookup) {
    String jobName = textOf(classDraft.get("jobName"));
    String levelName = textOf(classDraft.get("levelName"));
    List<Map<String, Object>> members = asMemberList(classDraft.get("members"));
    if (!members.isEmpty()) {
      if (!StringUtils.hasText(jobName)) {
        jobName = textOf(members.get(0).get("jobName"));
      }
      if (!StringUtils.hasText(levelName)) {
        levelName = textOf(members.get(0).get("levelName"));
      }
    }

    Long workTypeId = toLong(classDraft.get("workTypeId"));
    if (workTypeId == null) {
      workTypeId = lookup.resolveId(jobName);
    }
    if (workTypeId == null) {
      workTypeId = trainInfo.getWorkTypeId();
    }

    Integer workTypeLevel = toInt(classDraft.get("workTypeLevel"));
    if (workTypeLevel == null) {
      workTypeLevel = resolveWorkTypeLevel(levelName);
    }
    if (workTypeLevel == null) {
      workTypeLevel = trainInfo.getWorkTypeLevel();
    }

    clazz.setWorkTypeId(workTypeId);
    clazz.setWorkTypeLevel(workTypeLevel);
  }

  /**
   * 名单等级文本 → 1~5（与采集/人员认定一致：初级工=1 … 高级技师=5）。
   */
  static Integer resolveWorkTypeLevel(String levelName) {
    if (!StringUtils.hasText(levelName)) {
      return null;
    }
    String t = levelName.trim();
    Integer mapped = CertMappingSupport.mapLevelText(t);
    if (mapped != null) {
      return mapped;
    }
    // 复合写法：五级/初级工、初级工/五级 等
    String[] labels = {"高级技师", "技师", "高级工", "中级工", "初级工"};
    for (String label : labels) {
      if (t.contains(label)) {
        return CertMappingSupport.mapLevelText(label);
      }
    }
    if (t.matches("[1-5]")) {
      return Integer.parseInt(t);
    }
    return null;
  }

  private WorkTypeLookup buildWorkTypeLookup() {
    List<ExamWorkTypeEntity> all = examWorkTypeMapper.selectList(
        new LambdaQueryWrapper<ExamWorkTypeEntity>().eq(ExamWorkTypeEntity::getDeleted, 0));
    Map<String, List<ExamWorkTypeEntity>> byTitle = new HashMap<>();
    for (ExamWorkTypeEntity e : all) {
      if (!StringUtils.hasText(e.getTitle())) {
        continue;
      }
      byTitle.computeIfAbsent(normalizeWorkTypeTitle(e.getTitle()), k -> new ArrayList<>()).add(e);
    }
    return new WorkTypeLookup(byTitle);
  }

  private static String normalizeWorkTypeTitle(String title) {
    return title.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private static final class WorkTypeLookup {
    private final Map<String, List<ExamWorkTypeEntity>> byTitle;

    private WorkTypeLookup(Map<String, List<ExamWorkTypeEntity>> byTitle) {
      this.byTitle = byTitle;
    }

    private Long resolveId(String jobName) {
      if (!StringUtils.hasText(jobName)) {
        return null;
      }
      String key = normalizeWorkTypeTitle(jobName);
      List<ExamWorkTypeEntity> hits = byTitle.get(key);
      if (hits == null || hits.isEmpty()) {
        List<ExamWorkTypeEntity> fuzzy = new ArrayList<>();
        for (Map.Entry<String, List<ExamWorkTypeEntity>> e : byTitle.entrySet()) {
          if (e.getKey().contains(key) || key.contains(e.getKey())) {
            fuzzy.addAll(e.getValue());
          }
        }
        hits = fuzzy;
      }
      if (hits == null || hits.isEmpty()) {
        return null;
      }
      ExamWorkTypeEntity best = pickBestWorkType(hits);
      return best.getId() == null ? null : best.getId().longValue();
    }

    private static ExamWorkTypeEntity pickBestWorkType(List<ExamWorkTypeEntity> hits) {
      return hits.stream()
          .sorted(Comparator
              .comparing((ExamWorkTypeEntity e) -> e.getParentId() == null || e.getParentId() == 0)
              .thenComparing(e -> e.getId() == null ? Integer.MAX_VALUE : e.getId()))
          .findFirst()
          .orElse(hits.get(0));
    }
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
      String activeTitle = releaseOrphanPeopleAndFindActiveClassTitle(
          tenantId, trainInfo.getId(), r.getIdCard().trim());
      if (activeTitle != null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "学员已在班级「" + activeTitle + "」中：" + r.getPersonName() + "/" + r.getIdCard());
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
    // 企业名单是规划级：清空该规划下全部「待分」行，避免新旧名单混用
    LambdaUpdateWrapper<TrainPlanRosterEntity> u = new LambdaUpdateWrapper<TrainPlanRosterEntity>()
        .eq(TrainPlanRosterEntity::getTenantId, tenantId)
        .eq(TrainPlanRosterEntity::getPlanId, planId)
        .eq(TrainPlanRosterEntity::getAssignStatus, 0)
        .set(TrainPlanRosterEntity::getDeleted, 1)
        .set(TrainPlanRosterEntity::getUpdateTime, LocalDateTime.now());
    rosterMapper.update(null, u);
  }

  /**
   * 企业重新上传筛选名单后：作废待分池与未物化分班草稿，强制下次分班重解析。
   */
  @Transactional
  public void invalidateAfterEnterpriseRosterUpload(Long planId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainThreeYearPlanEntity plan = planMapper.selectById(planId);
    if (plan == null || !Objects.equals(plan.getTenantId(), tenantId)) {
      return;
    }
    softDeleteExistingRoster(tenantId, planId, null);
    List<TrainDivisionPackTaskEntity> actives = packTaskMapper.selectList(
        new LambdaQueryWrapper<TrainDivisionPackTaskEntity>()
            .eq(TrainDivisionPackTaskEntity::getTenantId, tenantId)
            .eq(TrainDivisionPackTaskEntity::getPlanId, planId)
            .in(TrainDivisionPackTaskEntity::getStatus, ACTIVE_PACK_STATUS));
    LocalDateTime now = LocalDateTime.now();
    for (TrainDivisionPackTaskEntity old : actives) {
      old.setStatus("superseded");
      old.setUpdateTime(now);
      packTaskMapper.updateById(old);
    }
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

  /** 手动/自动分班：名单与排班均需齐套 */
  private void assertFullReturnReady(TrainThreeYearPlanEntity plan) {
    if (plan.getEnterpriseRosterFileId() == null || plan.getEnterpriseShiftFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先上传筛选名单与排班表后再分班");
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
