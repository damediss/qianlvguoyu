package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.converter.CompetitionStageContentConverter;
import com.guoyu.platform.competition.dto.CompetitionStageContentBatchReq;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.vo.CompetitionStageContentVO;
import com.guoyu.platform.competition.vo.CompetitionStageReadinessCheckVO;
import com.guoyu.platform.competition.vo.CompetitionStageReadinessVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.competition.vo.CompetitionStageWorkspaceVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionStagePlanningApplicationService {

  private final CompetitionStageApplicationService stageApplicationService;
  private final CompetitionStageContentService contentService;

  public CompetitionStagePlanningApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionStageContentService contentService) {
    this.stageApplicationService = stageApplicationService;
    this.contentService = contentService;
  }

  @Transactional(readOnly = true)
  public CompetitionStageReadinessVO readiness(Long stageId) {
    CompetitionStageVO stage = stageApplicationService.get(stageId);
    List<CompetitionStageContentEntity> contents = contentService.list(
        currentTenantId(), stage.getId());
    List<CompetitionStageReadinessCheckVO> checks = new ArrayList<>();
    boolean timeValid = stage.getStartAt() != null && stage.getEndAt() != null
        && stage.getEndAt().isAfter(stage.getStartAt());
    checks.add(new CompetitionStageReadinessCheckVO(
        "STAGE_TIME_INVALID", timeValid,
        timeValid ? "赛段时间窗口有效" : "赛段必须配置结束时间晚于开始时间的时间窗口"));
    List<CompetitionStageContentEntity> enabled = contents.stream()
        .filter(item -> "enabled".equals(item.getStatus())).toList();
    boolean hasContent = !enabled.isEmpty();
    checks.add(new CompetitionStageReadinessCheckVO(
        "STAGE_CONTENT_MISSING", hasContent,
        hasContent ? "至少一项比赛内容已启用" : "赛段尚未启用比赛内容"));
    boolean simulationAvailable = enabled.stream().noneMatch(item ->
        CompetitionStageContentType.SIMULATION.getCode().equals(item.getContentType()));
    checks.add(new CompetitionStageReadinessCheckVO(
        "SIMULATION_PROVIDER_UNAVAILABLE", simulationAvailable,
        simulationAvailable ? "未启用仿真占位内容" : "仿真 Provider 尚未实现，本期不能就绪或发布执行方案"));
    boolean ready = checks.stream().allMatch(CompetitionStageReadinessCheckVO::passed);
    return new CompetitionStageReadinessVO(stageId, ready, List.copyOf(checks));
  }

  @Transactional(readOnly = true)
  public CompetitionStageWorkspaceVO workspace(Long stageId) {
    CompetitionStageVO stage = stageApplicationService.get(stageId);
    List<CompetitionStageContentVO> contents = contentService.list(
            currentTenantId(), stage.getId()).stream()
        .map(CompetitionStageContentConverter::toVO).toList();
    return new CompetitionStageWorkspaceVO(stage, contents, readiness(stageId));
  }

  @Transactional
  public List<CompetitionStageContentVO> replaceContents(
      Long stageId, CompetitionStageContentBatchReq req) {
    CompetitionStageVO stage = stageApplicationService.assertCanManageStagePlan(stageId);
    return contentService.replace(currentTenantId(), stage.getId(), req).stream()
        .map(CompetitionStageContentConverter::toVO).toList();
  }

  private Long currentTenantId() {
    return com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
  }
}
