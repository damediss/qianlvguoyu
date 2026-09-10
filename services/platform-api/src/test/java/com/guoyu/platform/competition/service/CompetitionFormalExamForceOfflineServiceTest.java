package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionFormalExamForceOfflineServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T01:10:00Z"), ZoneId.of("Asia/Shanghai"));

  private final CompetitionStageApplicationService stages =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionEventService events = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeMapper workTypes =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levels =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final StpLogic examStpLogic = Mockito.mock(StpLogic.class);
  private final CompetitionFormalExamAccessSessionService accessSessions =
      Mockito.mock(CompetitionFormalExamAccessSessionService.class);

  @BeforeEach
  void authenticate() {
    var principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "Admin", "platform", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    var stage = new CompetitionStageVO();
    stage.setId(10L);
    stage.setEventId(20L);
    stage.setEventWorkTypeId(30L);
    stage.setEventWorkTypeLevelCode("3");
    stage.setName("初赛");
    when(stages.assertCanManage(10L)).thenReturn(stage);
    var event = new CompetitionEventEntity();
    event.setId(20L);
    event.setName("技能大赛");
    when(events.getById(1L, 20L)).thenReturn(event);
    var workType = new CompetitionEventWorkTypeEntity();
    workType.setId(30L);
    workType.setDisplayName("电工");
    when(workTypes.selectOne(any())).thenReturn(workType);
    var level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(40L);
    level.setLevelCode("3");
    level.setLevelName("高级工");
    when(levels.selectOne(any())).thenReturn(level);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void kickoutDoesNotChangeCandidateExamStatus() {
    stubSession();
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    when(candidateExams.selectById(9101L)).thenReturn(submitted);

    service().forceOffline(9001L, 9101L);

    verify(accessSessions).forceOffline(9001L, 9101L, 77L, 1000L, "工作人员清退");
    verify(examStpLogic, never()).kickout(any());
    verify(examStpLogic, never()).kickout(any(), anyString());
  }

  @Test
  void waitingEnterCanBeKickedAfterLogin() {
    stubSession();
    when(candidateExams.selectById(9101L)).thenReturn(candidate("waiting_enter"));

    service().forceOffline(9001L, 9101L);

    verify(accessSessions).forceOffline(9001L, 9101L, 77L, 1000L, "工作人员清退");
    verify(examStpLogic, never()).kickout(any());
  }

  @Test
  void rejectsCandidateWithoutPlayerAccount() {
    stubSession();
    CompetitionFormalCandidateExamEntity unbound = candidate("ready");
    unbound.setPlayerId(null);
    when(candidateExams.selectById(9101L)).thenReturn(unbound);

    assertThatThrownBy(() -> service().forceOffline(9001L, 9101L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该选手尚未绑定考试账号，无法清退");
    verify(accessSessions, never()).forceOffline(any(), any(), any(), any(), any());
    verify(examStpLogic, never()).kickout(any());
  }

  @Test
  void rejectsCandidateFromAnotherSession() {
    stubSession();
    CompetitionFormalCandidateExamEntity other = candidate("running");
    other.setExamSessionId(9999L);
    when(candidateExams.selectById(9101L)).thenReturn(other);

    assertThatThrownBy(() -> service().forceOffline(9001L, 9101L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手考试实例不存在");
    verify(accessSessions, never()).forceOffline(any(), any(), any(), any(), any());
    verify(examStpLogic, never()).kickout(any());
  }

  private CompetitionFormalExamSessionApplicationService service() {
    return new CompetitionFormalExamSessionApplicationService(
        stages, events, workTypes, levels,
        Mockito.mock(CompetitionStageContentMapper.class),
        Mockito.mock(CompetitionStagePaperAssignmentMapper.class),
        Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class),
        Mockito.mock(CompetitionTheoryPaperQuestionMapper.class),
        sessions,
        Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class),
        Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class),
        candidateExams,
        Mockito.mock(CompetitionFormalCandidateResultMapper.class),
        Mockito.mock(CompetitionFormalCandidateQuestionScoreMapper.class),
        CLOCK,
        new CompetitionFormalExamCandidateFinalizationService(
            candidateExams, Mockito.mock(CompetitionFormalCandidateAnswerMapper.class)),
        examStpLogic,
        accessSessions);
  }

  private void stubSession() {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setTenantId(1L);
    session.setEventId(20L);
    session.setStageId(10L);
    session.setStatus("running");
    when(sessions.selectById(9001L)).thenReturn(session);
  }

  private static CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setPlayerId(77L);
    entity.setCandidateNo("001");
    entity.setCandidateName("张三");
    entity.setStatus(status);
    return entity;
  }
}
