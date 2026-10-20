package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.zhenti.dto.ZhentiScopeBatchSaveReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiScopeInfoReq;
import com.guoyu.platform.exam.zhenti.vo.ZhentiScopeInfoVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiScopePlansVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.service.TrainExamListApplicationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 真题共享组管理（方案 v2.1 §7）：提示条 scopeInfo、「真题共享范围」页列表与批量保存。
 *
 * <p>批量保存的编辑模型 = 课程绑定：只提交 courseIdList，由 TrainExamListApplicationService
 * 重新派生工种/等级，不直接改 work_type_id（避免派生关系漂移，Q5 已拍板保留该页长期维护）。
 */
@Service
@Slf4j
public class ZhentiScopeAdminService {

  private final ZhentiScopeResolver scopeResolver;
  private final TrainExamListMapper trainExamListMapper;
  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final ExamCourseMapper examCourseMapper;
  private final TrainExamListApplicationService trainExamListApplicationService;

  public ZhentiScopeAdminService(
      ZhentiScopeResolver scopeResolver,
      TrainExamListMapper trainExamListMapper,
      TrainExamWorkTypeListMapper workTypeListMapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      ExamCourseMapper examCourseMapper,
      TrainExamListApplicationService trainExamListApplicationService) {
    this.scopeResolver = scopeResolver;
    this.trainExamListMapper = trainExamListMapper;
    this.workTypeListMapper = workTypeListMapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.examCourseMapper = examCourseMapper;
    this.trainExamListApplicationService = trainExamListApplicationService;
  }

  /** 提示条 / 上传弹窗：计划的全部共享组（label + 组内计划数）。 */
  @Transactional(readOnly = true)
  public ZhentiScopeInfoVO scopeInfo(ZhentiScopeInfoReq req) {
    Long tenantId = currentUser().getTenantId();
    List<ZhentiScope> scopes = scopeResolver.resolve(req.examId());
    Map<ZhentiScope, Long> counts = scopeResolver.scopePlanCounts(tenantId, scopes);
    Map<Long, String> titles = workTypeTitles(scopes.stream()
        .map(ZhentiScope::workTypeId).collect(Collectors.toSet()));
    List<ZhentiScopeInfoVO.Item> items = new ArrayList<>();
    for (ZhentiScope scope : scopes) {
      items.add(new ZhentiScopeInfoVO.Item(
          scope.workTypeId(), scope.level(),
          ZhentiScopeKey.label(scope, titles.getOrDefault(scope.workTypeId(), "未知工种")),
          counts.getOrDefault(scope, 0L)));
    }
    return new ZhentiScopeInfoVO(scopeResolver.scopeEnabled(), scopes.isEmpty(), items);
  }

  /**
   * 「真题共享范围」页：计划 + 现有绑定（含失效标记）+ 课程选项。
   *
   * <p>生产规模 261 个计划：全量取回后在内存过滤/分页，避免为筛选条件写脆弱 SQL；
   * filter ∈ all（默认）/ unbound（无绑定）/ multi（多绑定）/ invalid（含失效工种）。
   */
  @Transactional(readOnly = true)
  public ZhentiScopePlansVO scopePlans(Long page, Long pageSize, String keyword,
      String filter) {
    Long tenantId = currentUser().getTenantId();
    long current = page == null || page < 1 ? 1 : page;
    long size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
    List<TrainExamListEntity> plans = trainExamListMapper.selectList(
        new LambdaQueryWrapper<TrainExamListEntity>()
            .eq(TrainExamListEntity::getTenantId, tenantId)
            .eq(TrainExamListEntity::getDeleted, 0)
            .like(StringUtils.hasText(keyword), TrainExamListEntity::getTitle, keyword)
            .orderByDesc(TrainExamListEntity::getId));
    List<Long> examIds = plans.stream().map(TrainExamListEntity::getId).toList();

    Map<Long, List<TrainExamWorkTypeListEntity>> bindingMap = loadBindings(examIds);
    Map<Long, ExamWorkTypeEntity> workTypeMap = loadWorkTypes(bindingMap);
    Map<Long, ExamCourseEntity> courseMap = loadCourses(bindingMap);

    List<ZhentiScopePlansVO.Item> all = new ArrayList<>();
    for (TrainExamListEntity plan : plans) {
      List<TrainExamWorkTypeListEntity> bindings =
          bindingMap.getOrDefault(plan.getId(), List.of());
      List<ZhentiScopePlansVO.Binding> items = new ArrayList<>();
      boolean hasInvalid = false;
      for (TrainExamWorkTypeListEntity binding : bindings) {
        ExamWorkTypeEntity workType =
            binding.getWorkTypeId() == null ? null : workTypeMap.get(binding.getWorkTypeId());
        boolean valid = workType != null;
        hasInvalid |= !valid;
        ExamCourseEntity course =
            binding.getCourseId() == null ? null : courseMap.get(binding.getCourseId());
        Integer level = ZhentiLevel.parseLevel(binding.getLevel());
        items.add(new ZhentiScopePlansVO.Binding(
            binding.getWorkTypeId(),
            valid ? workType.getTitle() : null,
            level,
            ZhentiLevel.labelOf(level),
            binding.getCourseId(),
            course != null ? course.getTitle() : null,
            valid));
      }
      all.add(new ZhentiScopePlansVO.Item(
          String.valueOf(plan.getId()),
          plan.getTitle(),
          items,
          bindings.isEmpty(),
          bindings.size() > 1,
          hasInvalid));
    }
    List<ZhentiScopePlansVO.Item> filtered = all.stream()
        .filter(item -> switch (filter == null ? "all" : filter) {
          case "unbound" -> item.noBinding();
          case "multi" -> item.multiBinding();
          case "invalid" -> item.hasInvalid();
          default -> true;
        })
        .toList();
    int from = (int) Math.min((current - 1) * size, filtered.size());
    int to = (int) Math.min(from + size, filtered.size());
    return new ZhentiScopePlansVO(
        filtered.subList(from, to), filtered.size(), current, size, courseOptions());
  }

  /** 批量保存课程绑定（每行复用计划保存路径，逐条独立事务由调用方保证整体语义）。 */
  @Transactional
  public void scopeBatchSave(ZhentiScopeBatchSaveReq req) {
    for (ZhentiScopeBatchSaveReq.Item item : req.items()) {
      TrainExamListUpdateReq updateReq = new TrainExamListUpdateReq();
      updateReq.setCourseIdList(item.courseIdList());
      trainExamListApplicationService.update(item.examId(), updateReq);
      log.info("[ZhentiScope] 绑定已更新 examId={} courses={}",
          item.examId(), item.courseIdList().size());
    }
  }

  private Map<Long, List<TrainExamWorkTypeListEntity>> loadBindings(List<Long> examIds) {
    if (examIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<TrainExamWorkTypeListEntity>> map = new HashMap<>();
    for (TrainExamWorkTypeListEntity binding : workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .in(TrainExamWorkTypeListEntity::getExamId, examIds)
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0))) {
      map.computeIfAbsent(binding.getExamId(), k -> new ArrayList<>()).add(binding);
    }
    return map;
  }

  private Map<Long, ExamWorkTypeEntity> loadWorkTypes(
      Map<Long, List<TrainExamWorkTypeListEntity>> bindingMap) {
    List<Long> ids = bindingMap.values().stream()
        .flatMap(List::stream)
        .map(TrainExamWorkTypeListEntity::getWorkTypeId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return examWorkTypeMapper.selectBatchIds(ids).stream()
        .collect(Collectors.toMap(w -> w.getId().longValue(), w -> w, (a, b) -> a));
  }

  private Map<Long, ExamCourseEntity> loadCourses(
      Map<Long, List<TrainExamWorkTypeListEntity>> bindingMap) {
    List<Long> ids = bindingMap.values().stream()
        .flatMap(List::stream)
        .map(TrainExamWorkTypeListEntity::getCourseId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return examCourseMapper.selectBatchIds(ids).stream()
        .collect(Collectors.toMap(c -> c.getId().longValue(), c -> c, (a, b) -> a));
  }

  private List<ZhentiScopePlansVO.CourseOption> courseOptions() {
    return examCourseMapper.selectList(new LambdaQueryWrapper<ExamCourseEntity>()
            .eq(ExamCourseEntity::getDeleted, 0)
            .orderByAsc(ExamCourseEntity::getId))
        .stream()
        .map(c -> new ZhentiScopePlansVO.CourseOption(
            c.getId().longValue(), c.getTitle(),
            c.getWorkTypeId() == null ? null : c.getWorkTypeId().longValue(),
            c.getLevel()))
        .toList();
  }

  private Map<Long, String> workTypeTitles(java.util.Set<Long> workTypeIds) {
    if (workTypeIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return examWorkTypeMapper.selectBatchIds(
            workTypeIds.stream().map(Long::intValue).collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(w -> w.getId().longValue(), ExamWorkTypeEntity::getTitle,
            (a, b) -> a));
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.UNAUTHORIZED, "请先登录");
    }
  }
}
