package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.dto.CompetitionFormalCandidateExamQueryReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamSessionStartReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalPaperCandidateCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalStatusCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentPlayerRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.exam.realtime.FormalExamPresenceStore;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimeProperties;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.vo.CompetitionFormalExamSessionVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionFormalExamSessionApplicationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T01:00:00Z"), ZoneId.of("Asia/Shanghai"));
  private static final LocalDateTime START_AT = LocalDateTime.of(2026, 9, 8, 9, 0, 0);

  private final CompetitionStageApplicationService stages =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionEventService events = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeMapper workTypes =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levels =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionStageContentMapper contents =
      Mockito.mock(CompetitionStageContentMapper.class);
  private final CompetitionStagePaperAssignmentMapper assignments =
      Mockito.mock(CompetitionStagePaperAssignmentMapper.class);
  private final CompetitionTheoryPaperSnapshotMapper theoryPapers =
      Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class);
  private final CompetitionTheoryPaperQuestionMapper theoryQuestions =
      Mockito.mock(CompetitionTheoryPaperQuestionMapper.class);
  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalExamPaperSnapshotMapper paperSnapshots =
      Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class);
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshots =
      Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class);
  private final CompetitionFormalCandidateResultMapper results =
      Mockito.mock(CompetitionFormalCandidateResultMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);

  private final List<CompetitionFormalExamSessionEntity> sessionStore = new ArrayList<>();
  private final List<CompetitionFormalExamPaperSnapshotEntity> paperStore = new ArrayList<>();
  private final List<CompetitionFormalExamQuestionSnapshotEntity> questionStore = new ArrayList<>();
  private final List<CompetitionFormalCandidateExamEntity> candidateStore = new ArrayList<>();

  private final CompetitionFormalExamSessionApplicationService service =
      new CompetitionFormalExamSessionApplicationService(
          stages, events, workTypes, levels, contents, assignments, theoryPapers, theoryQuestions,
          sessions, paperSnapshots, questionSnapshots, candidateExams,
          results,
          Mockito.mock(CompetitionFormalCandidateQuestionScoreMapper.class),
          CLOCK,
          new CompetitionFormalExamCandidateFinalizationService(
              candidateExams, Mockito.mock(CompetitionFormalCandidateAnswerMapper.class)));

  @BeforeEach
  void setup() {
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
    when(events.lockForUpdate(1L, 20L)).thenReturn(event);
    var workType = new CompetitionEventWorkTypeEntity();
    workType.setId(30L);
    workType.setDisplayName("电工");
    when(workTypes.selectOne(any())).thenReturn(workType);
    var level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(40L);
    level.setLevelCode("3");
    level.setLevelName("高级工");
    when(levels.selectOne(any())).thenReturn(level);
    var theory = new CompetitionStageContentEntity();
    theory.setContentType("theory");
    when(contents.selectList(any())).thenReturn(List.of(theory));
    when(assignments.selectByScopeForUpdate(1L, 20L, 10L, 30L, 40L)).thenReturn(List.of());
    when(sessions.selectActiveForUpdate(eq(1L), eq(10L), eq("theory"))).thenAnswer(
        invocation -> sessionStore.stream()
            .filter(item -> item.getOccupancyToken() == null || item.getOccupancyToken() == 0L)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    when(sessions.selectActive(eq(1L), eq(10L), eq("theory"))).thenAnswer(
        invocation -> sessionStore.stream()
            .filter(item -> item.getOccupancyToken() == null || item.getOccupancyToken() == 0L)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    when(sessions.selectLatestByStageAndExamType(eq(1L), eq(10L), eq("theory"))).thenAnswer(
        invocation -> sessionStore.stream()
            .filter(item -> "theory".equals(item.getExamType()))
            .max(java.util.Comparator.comparing(CompetitionFormalExamSessionEntity::getId))
            .orElse(null));
    when(sessions.insert(any(CompetitionFormalExamSessionEntity.class))).thenAnswer(invocation -> {
      sessionStore.add(invocation.getArgument(0));
      return 1;
    });
    when(sessions.updateById(any(CompetitionFormalExamSessionEntity.class))).thenReturn(1);
    when(paperSnapshots.insert(any(CompetitionFormalExamPaperSnapshotEntity.class)))
        .thenAnswer(invocation -> {
          paperStore.add(invocation.getArgument(0));
          return 1;
        });
    when(questionSnapshots.insert(any(CompetitionFormalExamQuestionSnapshotEntity.class)))
        .thenAnswer(invocation -> {
          questionStore.add(invocation.getArgument(0));
          return 1;
        });
    when(candidateExams.insert(any(CompetitionFormalCandidateExamEntity.class)))
        .thenAnswer(invocation -> {
          candidateStore.add(invocation.getArgument(0));
          return 1;
        });
    when(paperSnapshots.selectList(any()))
        .thenAnswer(invocation -> new ArrayList<>(paperStore));
    when(questionSnapshots.selectList(any()))
        .thenAnswer(invocation -> new ArrayList<>(questionStore));
    when(candidateExams.selectList(any()))
        .thenAnswer(invocation -> {
          synchronized (candidateStore) {
            return new ArrayList<>(candidateStore);
          }
        });
    when(sessions.selectById(any())).thenAnswer(invocation -> sessionStore.stream()
        .filter(item -> java.util.Objects.equals(item.getId(), invocation.getArgument(0)))
        .findFirst().orElse(null));
    when(sessions.selectByIdForUpdate(any())).thenAnswer(invocation -> sessionStore.stream()
        .filter(item -> java.util.Objects.equals(item.getId(), invocation.getArgument(0)))
        .findFirst().orElse(null));
    when(sessions.markRunningIfReady(any(), any(), any(), any(), any(), any()))
        .thenAnswer(invocation -> {
          Long id = invocation.getArgument(0);
          String fromStatus = invocation.getArgument(1);
          String toStatus = invocation.getArgument(2);
          LocalDateTime actualStartTime = invocation.getArgument(3);
          synchronized (sessionStore) {
            for (CompetitionFormalExamSessionEntity session : sessionStore) {
              if (java.util.Objects.equals(session.getId(), id)
                  && fromStatus.equals(session.getStatus())) {
                session.setStatus(toStatus);
                session.setActualStartTime(actualStartTime);
                session.setUpdatedAt(invocation.getArgument(4));
                session.setUpdatedBy(invocation.getArgument(5));
                return 1;
              }
            }
          }
          return 0;
        });
    when(candidateExams.markReadyToRunning(any(), any(), any(), any(), any(), any()))
        .thenAnswer(invocation -> {
          Long sessionId = invocation.getArgument(0);
          String fromStatus = invocation.getArgument(1);
          String toStatus = invocation.getArgument(2);
          LocalDateTime startTime = invocation.getArgument(3);
          int moved = 0;
          synchronized (candidateStore) {
            for (CompetitionFormalCandidateExamEntity candidate : candidateStore) {
              if (java.util.Objects.equals(candidate.getExamSessionId(), sessionId)
                  && fromStatus.equals(candidate.getStatus())) {
                candidate.setStatus(toStatus);
                candidate.setStartTime(startTime);
                candidate.setUpdatedAt(invocation.getArgument(4));
                candidate.setUpdatedBy(invocation.getArgument(5));
                moved++;
              }
            }
          }
          return moved;
        });
    when(candidateExams.selectById(any())).thenAnswer(invocation -> candidateStore.stream()
        .filter(item -> java.util.Objects.equals(item.getId(), invocation.getArgument(0)))
        .findFirst().orElse(null));
    when(candidateExams.selectPage(any(), any())).thenAnswer(invocation -> {
      Page<CompetitionFormalCandidateExamEntity> page = invocation.getArgument(0);
      page.setRecords(new ArrayList<>(candidateStore));
      page.setTotal(candidateStore.size());
      return page;
    });
    when(candidateExams.countStatusByExamSessionId(any())).thenAnswer(invocation -> {
      Long sessionId = invocation.getArgument(0);
      java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
      for (CompetitionFormalCandidateExamEntity candidate : candidateStore) {
        if (java.util.Objects.equals(candidate.getExamSessionId(), sessionId)
            && candidate.getStatus() != null) {
          counts.merge(candidate.getStatus(), 1L, Long::sum);
        }
      }
      List<CompetitionFormalStatusCountRow> rows = new ArrayList<>();
      counts.forEach((status, total) -> {
        CompetitionFormalStatusCountRow row = new CompetitionFormalStatusCountRow();
        row.setStatus(status);
        row.setTotal(total);
        rows.add(row);
      });
      return rows;
    });
    when(candidateExams.countPaperCandidatesByExamSessionId(any())).thenAnswer(invocation -> {
      Long sessionId = invocation.getArgument(0);
      java.util.Map<Long, Long> counts = new java.util.LinkedHashMap<>();
      for (CompetitionFormalCandidateExamEntity candidate : candidateStore) {
        if (java.util.Objects.equals(candidate.getExamSessionId(), sessionId)
            && candidate.getPaperSnapshotId() != null) {
          counts.merge(candidate.getPaperSnapshotId(), 1L, Long::sum);
        }
      }
      List<CompetitionFormalPaperCandidateCountRow> rows = new ArrayList<>();
      counts.forEach((paperId, total) -> {
        CompetitionFormalPaperCandidateCountRow row = new CompetitionFormalPaperCandidateCountRow();
        row.setPaperSnapshotId(paperId);
        row.setTotal(total);
        rows.add(row);
      });
      return rows;
    });
    when(results.countStatusByExamSessionId(any())).thenReturn(List.of());
    when(results.selectByExamSessionIdAndCandidateIds(any(), any())).thenReturn(List.of());
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    service.setRealtimePublisher(null);
  }

  @Test
  void prepareCreatesOneSessionAndMapsPlayersOntoRuntimePaperSnapshots() {
    players(player(100L, "张三", "001", 500L, "A"), player(101L, "李四", "002", 501L, "B"));
    papers(paper(500L, "A", "A卷", 90), paper(501L, "B", "B卷", 90), paper(502L, "C", "C卷", 90));
    questions(500L, 2);
    questions(501L, 3);

    CompetitionFormalExamSessionVO vo = service.prepareExamSession(10L, "theory");

    assertThat(sessionStore).hasSize(1);
    assertThat(sessionStore.get(0).getStatus()).isEqualTo("ready");
    assertThat(sessionStore.get(0).getOccupancyToken()).isZero();
    assertThat(sessionStore.get(0).getDurationSeconds()).isEqualTo(5400);
    assertThat(paperStore).extracting(CompetitionFormalExamPaperSnapshotEntity::getSourcePaperId)
        .containsExactly(500L, 501L);
    assertThat(questionStore).hasSize(5);
    assertThat(candidateStore).hasSize(2);
    CompetitionFormalCandidateExamEntity zhang = candidateStore.stream()
        .filter(item -> item.getContestantProfileId().equals(100L)).findFirst().orElseThrow();
    CompetitionFormalCandidateExamEntity li = candidateStore.stream()
        .filter(item -> item.getContestantProfileId().equals(101L)).findFirst().orElseThrow();
    assertThat(zhang.getStatus()).isEqualTo("waiting_enter");
    assertThat(zhang.getSourcePaperId()).isEqualTo(500L);
    assertThat(li.getSourcePaperId()).isEqualTo(501L);
    assertThat(zhang.getPaperSnapshotId()).isNotEqualTo(li.getPaperSnapshotId());
    assertThat(zhang.getPaperSnapshotId()).isEqualTo(paperStore.stream()
        .filter(item -> item.getSourcePaperId().equals(500L)).findFirst().orElseThrow().getId());
    assertThat(questionStore)
        .extracting(CompetitionFormalExamQuestionSnapshotEntity::getPaperSnapshotId)
        .contains(zhang.getPaperSnapshotId(), li.getPaperSnapshotId());
    assertThat(vo.paperCount()).isEqualTo(2);
    assertThat(vo.candidateTotal()).isEqualTo(2);
    assertThat(vo.waitingEnterCount()).isEqualTo(2);
    assertThat(vo.readyCount()).isZero();
    assertThat(vo.absentCount()).isZero();
    verify(theoryPapers, Mockito.never()).selectById(502L);
  }

  @Test
  void prepareRejectsFinishedHistoricalSession() {
    players(player(100L, "张三", "001", 500L, "A"));
    papers(paper(500L, "A", "A卷", 60));
    questions(500L, 1);
    CompetitionFormalExamSessionEntity finished = new CompetitionFormalExamSessionEntity();
    finished.setId(2099L);
    finished.setExamType("theory");
    finished.setStatus("finished");
    finished.setOccupancyToken(2099L);
    sessionStore.add(finished);

    assertThatThrownBy(() -> service.prepareExamSession(10L, "theory"))
        .hasMessage("本场正式比赛已结束，本阶段不能重新初始化正式比赛");
    verify(sessions, Mockito.never()).insert(any(CompetitionFormalExamSessionEntity.class));
  }

  @Test
  void getByStageIgnoresFinishedOccupancyReleasedSession() {
    players(player(100L, "张三", "001", 500L, "A"));
    papers(paper(500L, "A", "A卷", 60));
    questions(500L, 1);
    CompetitionFormalExamSessionVO prepared = service.prepareExamSession(10L, "theory");
    sessionStore.get(0).setStatus("finished");
    sessionStore.get(0).setOccupancyToken(Long.valueOf(prepared.id()));

    assertThat(service.getByStage(10L, "theory")).isNull();
    assertThat(service.getById(Long.valueOf(prepared.id())).status()).isEqualTo("finished");
  }

  @Test
  void runningPastDeadlineExposesReadyFinishBlocker() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(2);
    sessionStore.get(0).setStatus("running");
    sessionStore.get(0).setActualStartTime(LocalDateTime.of(2026, 9, 8, 7, 0, 0));
    sessionStore.get(0).setDurationSeconds(5400);
    candidateStore.get(0).setStatus("submitted");
    candidateStore.get(1).setStatus("ready");

    CompetitionFormalExamSessionVO vo = service.getById(Long.valueOf(prepared.id()));

    assertThat(vo.status()).isEqualTo("running");
    assertThat(vo.finishEligible()).isFalse();
    assertThat(vo.absentCount()).isZero();
    assertThat(vo.finishBlockers()).extracting(item -> item.code())
        .contains("READY_CANDIDATE_REMAINS");
  }

  @Test
  void prepareIsIdempotentForAnExistingReadySession() {
    players(player(100L, "张三", "001", 500L, "A"));
    papers(paper(500L, "A", "A卷", 60));
    questions(500L, 1);
    service.prepareExamSession(10L, "theory");
    CompetitionFormalExamSessionVO again = service.prepareExamSession(10L, "theory");

    verify(sessions, times(1)).insert(any(CompetitionFormalExamSessionEntity.class));
    verify(paperSnapshots, times(1)).insert(any(CompetitionFormalExamPaperSnapshotEntity.class));
    verify(candidateExams, times(1)).insert(any(CompetitionFormalCandidateExamEntity.class));
    assertThat(again.id()).isEqualTo(String.valueOf(sessionStore.get(0).getId()));
    assertThat(sessionStore).hasSize(1);
  }

  @Test
  void prepareRejectsUnconfirmedAssignments() {
    CompetitionStagePaperAssignmentPlayerRow player = player(100L, "张三", "001", 500L, "A");
    player.setAssignmentStatus("draft");
    players(player);
    papers(paper(500L, "A", "A卷", 60));
    questions(500L, 1);
    assertThatThrownBy(() -> service.prepareExamSession(10L, null))
        .hasMessage("选手试卷配置尚未全部确认，不能初始化正式比赛");
    verify(sessions, Mockito.never()).insert(any(CompetitionFormalExamSessionEntity.class));
  }

  @Test
  void prepareRejectsMissingTheoryContent() {
    when(contents.selectList(any())).thenReturn(List.of());
    players(player(100L, "张三", "001", 500L, "A"));
    assertThatThrownBy(() -> service.prepareExamSession(10L, "theory"))
        .hasMessage("当前赛段未配置理论考试，不能初始化正式比赛");
  }

  @Test
  void prepareRejectsPaperWithoutQuestions() {
    players(player(100L, "张三", "001", 500L, "A"));
    papers(paper(500L, "A", "A卷", 60));
    when(theoryQuestions.selectByPaper(500L)).thenReturn(List.of());
    assertThatThrownBy(() -> service.prepareExamSession(10L, "theory"))
        .hasMessageContaining("没有题目");
  }

  @Test
  void prepareRejectsUnsupportedExamType() {
    assertThatThrownBy(() -> service.prepareExamSession(10L, "practical"))
        .hasMessage("本阶段仅支持理论正式比赛初始化");
  }

  @Test
  void preflightPassesForReadySession() {
    CompetitionFormalExamSessionVO session = readySession();
    var preflight = service.getPreflight(Long.valueOf(session.id()));
    assertThat(preflight.ready()).isTrue();
    assertThat(preflight.checks()).extracting(item -> item.code())
        .containsExactly(
            "SESSION_STATUS", "CANDIDATE_COUNT", "CANDIDATE_PAPER_MAPPING",
            "PAPER_SNAPSHOT", "QUESTION_SNAPSHOT", "CANDIDATE_PROFILE");
    assertThat(preflight.checks()).allMatch(item -> item.passed());
  }

  @Test
  void preflightFailsWhenCandidateCountDoesNotMatch() {
    CompetitionFormalExamSessionVO session = readySession();
    candidateStore.remove(0);
    var preflight = service.getPreflight(Long.valueOf(session.id()));
    assertThat(preflight.ready()).isFalse();
    assertThat(failed(preflight, "CANDIDATE_COUNT")).contains("数量异常");
  }

  @Test
  void preflightFailsWhenPaperSnapshotIsMissing() {
    CompetitionFormalExamSessionVO session = readySession();
    paperStore.clear();
    var preflight = service.getPreflight(Long.valueOf(session.id()));
    assertThat(preflight.ready()).isFalse();
    assertThat(failed(preflight, "PAPER_SNAPSHOT")).contains("正式试卷快照缺失");
  }

  @Test
  void preflightFailsWhenQuestionSnapshotCountIsWrong() {
    CompetitionFormalExamSessionVO session = readySession();
    paperStore.get(0).setQuestionCount(99);
    var preflight = service.getPreflight(Long.valueOf(session.id()));
    assertThat(preflight.ready()).isFalse();
    assertThat(failed(preflight, "PAPER_SNAPSHOT")).contains("预期 99 题");
  }

  @Test
  void preflightFailsWhenCandidatePointsToUnknownPaperSnapshot() {
    CompetitionFormalExamSessionVO session = readySession();
    candidateStore.get(0).setPaperSnapshotId(999L);
    var preflight = service.getPreflight(Long.valueOf(session.id()));
    assertThat(preflight.ready()).isFalse();
    assertThat(failed(preflight, "CANDIDATE_PAPER_MAPPING")).contains("错误的试卷快照");
  }

  @Test
  void pageCandidatesAppliesKeywordPaperAndStatusFilters() {
    CompetitionFormalExamSessionVO session = readySession();
    CompetitionFormalCandidateExamQueryReq req = new CompetitionFormalCandidateExamQueryReq();
    req.setKeyword("张");
    req.setStatus("waiting_enter");
    req.setPaperSnapshotId(candidateStore.get(0).getPaperSnapshotId());
    var page = service.pageCandidates(Long.valueOf(session.id()), req);
    assertThat(page.items()).extracting(item -> item.candidateName()).containsExactly("张三");
    verify(candidateExams).selectPage(any(), any());
    verify(results).selectByExamSessionIdAndCandidateIds(eq(Long.valueOf(session.id())), any());
  }

  @Test
  void pageCandidatesAttachesConnectionStateFromPresenceNotLastActive() {
    CompetitionFormalExamSessionVO session = readySession();
    candidateStore.get(0).setStatus("running");
    candidateStore.get(0).setLastActiveTime(START_AT);
    CompetitionFormalExamAccessSessionEntity access = new CompetitionFormalExamAccessSessionEntity();
    access.setId(55L);
    access.setCandidateExamId(candidateStore.get(0).getId());
    access.setStatus("active");
    CompetitionFormalExamAccessSessionService accessService =
        Mockito.mock(CompetitionFormalExamAccessSessionService.class);
    when(accessService.latestAccessByCandidateExamIds(any())).thenReturn(
        java.util.Map.of(candidateStore.get(0).getId(), access));
    FormalExamPresenceStore presenceStore = Mockito.mock(FormalExamPresenceStore.class);
    when(presenceStore.readMany(any())).thenReturn(java.util.Map.of(
        55L,
        new FormalExamPresenceStore.Presence(
            55L,
            1L,
            Long.valueOf(session.id()),
            candidateStore.get(0).getId(),
            1,
            Instant.parse("2026-09-07T00:00:00Z"))));
    CompetitionFormalExamSessionApplicationService live =
        new CompetitionFormalExamSessionApplicationService(
            stages, events, workTypes, levels, contents, assignments, theoryPapers, theoryQuestions,
            sessions, paperSnapshots, questionSnapshots, candidateExams, results,
            Mockito.mock(CompetitionFormalCandidateQuestionScoreMapper.class),
            CLOCK,
            new CompetitionFormalExamCandidateFinalizationService(
                candidateExams, Mockito.mock(CompetitionFormalCandidateAnswerMapper.class)),
            null,
            accessService);
    live.setPresenceStore(presenceStore);
    live.setRealtimeProperties(new FormalExamRealtimeProperties());

    CompetitionFormalCandidateExamQueryReq req = new CompetitionFormalCandidateExamQueryReq();
    var page = live.pageCandidates(Long.valueOf(session.id()), req);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).connectionState()).isEqualTo("briefly_offline");
    assertThat(page.items().get(0).accessSessionStatus()).isEqualTo("active");
    assertThat(page.items().get(0).lastSeenAt()).isEqualTo("2026-09-07T00:00:00Z");
    assertThat(page.items().get(0).lastActiveTime()).isEqualTo(START_AT);
  }

  @Test
  void getCandidateReturnsRuntimePaperBindingWithoutAnswers() {
    readySession();
    var detail = service.getCandidate(candidateStore.get(0).getId());
    assertThat(detail.candidateName()).isEqualTo("张三");
    assertThat(detail.paperCode()).isEqualTo("A");
    assertThat(detail.paperSnapshotId()).isEqualTo(String.valueOf(candidateStore.get(0).getPaperSnapshotId()));
    assertThat(detail.sourcePaperId()).isEqualTo("500");
  }

  @Test
  void startMovesReadyCandidatesWithSharedActualStartTime() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(3);
    markReady(3);

    CompetitionFormalExamSessionVO started = service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false));

    assertThat(started.status()).isEqualTo("running");
    assertThat(started.actualStartTime()).isEqualTo(START_AT);
    assertThat(started.actualEndTime()).isNull();
    assertThat(started.deadlineTime()).isEqualTo(START_AT.plusSeconds(5400));
    assertThat(started.serverTime()).isEqualTo(START_AT);
    assertThat(started.readyCount()).isZero();
    assertThat(started.runningCount()).isEqualTo(3);
    assertThat(started.waitingEnterCount()).isZero();
    assertThat(sessionStore.get(0).getActualEndTime()).isNull();
    assertThat(candidateStore).allMatch(item -> "running".equals(item.getStatus()));
    assertThat(candidateStore).extracting(CompetitionFormalCandidateExamEntity::getStartTime)
        .containsOnly(START_AT);
    verify(sessions, times(1)).markRunningIfReady(
        eq(sessionStore.get(0).getId()), eq("ready"), eq("running"),
        eq(START_AT), eq(START_AT), eq(1000L));
  }

  @Test
  void startPublishesSessionStatusAndSnapshotReloadForPads() {
    FormalExamRealtimePublisher publisher = Mockito.mock(FormalExamRealtimePublisher.class);
    service.setRealtimePublisher(publisher);
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(2);
    markReady(2);

    service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false));

    Long sessionId = sessionStore.get(0).getId();
    verify(publisher).sessionStatus(eq(1L), eq(sessionId), eq("running"));
    verify(publisher).snapshotReload(eq(1L), eq(sessionId), eq("session-started"));
  }

  @Test
  void startKeepsWaitingEnterCandidatesUntilConfirmed() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(5);
    markReady(3);

    assertThatThrownBy(() -> service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false)))
        .hasMessage("当前仍有 2 名选手未进入候考，确认后才能开始比赛");
    assertThat(sessionStore.get(0).getStatus()).isEqualTo("ready");

    CompetitionFormalExamSessionVO started = service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(true));

    assertThat(started.status()).isEqualTo("running");
    assertThat(started.runningCount()).isEqualTo(3);
    assertThat(started.waitingEnterCount()).isEqualTo(2);
    assertThat(started.readyCount()).isZero();
    assertThat(candidateStore.stream().filter(item -> "running".equals(item.getStatus()))
        .map(CompetitionFormalCandidateExamEntity::getStartTime).toList())
        .containsOnly(START_AT);
    assertThat(candidateStore.stream().filter(item -> "waiting_enter".equals(item.getStatus())))
        .hasSize(2)
        .allMatch(item -> item.getStartTime() == null);
  }

  @Test
  void startRejectsWhenNoReadyCandidates() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(2);
    assertThatThrownBy(() -> service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(true)))
        .hasMessage("当前没有已进入候考的选手，无法开始比赛");
    verify(sessions, Mockito.never()).markRunningIfReady(any(), any(), any(), any(), any(), any());
  }

  @Test
  void startIsIdempotentForRunningSession() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(2);
    markReady(2);
    CompetitionFormalExamSessionVO first = service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false));
    LocalDateTime firstStart = sessionStore.get(0).getActualStartTime();
    candidateStore.get(0).setStartTime(firstStart.plusSeconds(8));

    CompetitionFormalExamSessionVO second = service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false));

    assertThat(second.status()).isEqualTo("running");
    assertThat(second.actualStartTime()).isEqualTo(first.actualStartTime());
    assertThat(sessionStore.get(0).getActualStartTime()).isEqualTo(firstStart);
    assertThat(candidateStore.get(0).getStartTime()).isEqualTo(firstStart.plusSeconds(8));
    verify(sessions, times(1)).markRunningIfReady(any(), any(), any(), any(), any(), any());
    verify(candidateExams, times(1)).markReadyToRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void concurrentStartOnlyTransitionsOnce() throws Exception {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(3);
    markReady(3);
    Long sessionId = Long.valueOf(prepared.id());
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);
    try {
      Future<CompetitionFormalExamSessionVO> first = pool.submit(() -> {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        try {
          return service.startExamSession(sessionId, new CompetitionFormalExamSessionStartReq(false));
        } finally {
          SecurityContextHolder.clearContext();
        }
      });
      Future<CompetitionFormalExamSessionVO> second = pool.submit(() -> {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        try {
          return service.startExamSession(sessionId, new CompetitionFormalExamSessionStartReq(false));
        } finally {
          SecurityContextHolder.clearContext();
        }
      });
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      CompetitionFormalExamSessionVO left = first.get(10, TimeUnit.SECONDS);
      CompetitionFormalExamSessionVO right = second.get(10, TimeUnit.SECONDS);
      assertThat(left.status()).isEqualTo("running");
      assertThat(right.status()).isEqualTo("running");
      assertThat(left.actualStartTime()).isEqualTo(right.actualStartTime()).isEqualTo(START_AT);
      assertThat(candidateStore).extracting(CompetitionFormalCandidateExamEntity::getStartTime)
          .containsOnly(START_AT);
      verify(candidateExams, times(1)).markReadyToRunning(any(), any(), any(), any(), any(), any());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void startRejectsWhenPreflightFails() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(1);
    markReady(1);
    paperStore.clear();
    assertThatThrownBy(() -> service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false)))
        .hasMessageContaining("开考前检查未通过");
    assertThat(sessionStore.get(0).getStatus()).isEqualTo("ready");
    verify(sessions, Mockito.never()).markRunningIfReady(any(), any(), any(), any(), any(), any());
  }

  @Test
  void startRejectsSubmittedCandidateOnReadySession() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(2);
    markReady(2);
    candidateStore.get(1).setStatus("submitted");
    assertThatThrownBy(() -> service.startExamSession(
        Long.valueOf(prepared.id()), new CompetitionFormalExamSessionStartReq(false)))
        .hasMessage("正式比赛运行数据异常：存在已交卷状态的选手，不能开始比赛");
    verify(sessions, Mockito.never()).markRunningIfReady(any(), any(), any(), any(), any(), any());
  }

  @Test
  void startRejectsPreparingFinishedAndCancelledSessions() {
    CompetitionFormalExamSessionVO prepared = readySessionWithPlayers(1);
    markReady(1);
    Long sessionId = Long.valueOf(prepared.id());
    sessionStore.get(0).setStatus("preparing");
    assertThatThrownBy(() -> service.startExamSession(
        sessionId, new CompetitionFormalExamSessionStartReq(true)))
        .hasMessage("正式比赛数据仍在准备中");
    sessionStore.get(0).setStatus("finished");
    assertThatThrownBy(() -> service.startExamSession(
        sessionId, new CompetitionFormalExamSessionStartReq(true)))
        .hasMessage("本场正式比赛已结束");
    sessionStore.get(0).setStatus("cancelled");
    assertThatThrownBy(() -> service.startExamSession(
        sessionId, new CompetitionFormalExamSessionStartReq(true)))
        .hasMessage("本场正式比赛已取消");
  }

  private CompetitionFormalExamSessionVO readySession() {
    players(player(100L, "张三", "001", 500L, "A"));
    papers(paper(500L, "A", "A卷", 60));
    questions(500L, 1);
    return service.prepareExamSession(10L, "theory");
  }

  private CompetitionFormalExamSessionVO readySessionWithPlayers(int count) {
    CompetitionStagePaperAssignmentPlayerRow[] rows = new CompetitionStagePaperAssignmentPlayerRow[count];
    for (int index = 0; index < count; index++) {
      rows[index] = player(100L + index, "选手" + (index + 1), String.format("%03d", index + 1), 500L, "A");
    }
    players(rows);
    papers(paper(500L, "A", "A卷", 90));
    questions(500L, 1);
    return service.prepareExamSession(10L, "theory");
  }

  private void markReady(int readyCount) {
    LocalDateTime enterTime = LocalDateTime.of(2026, 9, 8, 8, 0, 0);
    for (int index = 0; index < candidateStore.size(); index++) {
      if (index < readyCount) {
        candidateStore.get(index).setStatus("ready");
        candidateStore.get(index).setEnterTime(enterTime);
      }
    }
  }

  private String failed(
      com.guoyu.platform.competition.vo.CompetitionFormalExamPreflightVO preflight, String code) {
    return preflight.checks().stream()
        .filter(item -> code.equals(item.code()) && !item.passed())
        .map(item -> item.message())
        .findFirst()
        .orElse("");
  }

  private void players(CompetitionStagePaperAssignmentPlayerRow... rows) {
    when(assignments.selectEligiblePlayers(eq(1L), eq(20L), eq(10L), eq(30L), eq(40L),
        any(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(rows));
  }

  private void papers(CompetitionTheoryPaperSnapshotEntity... rows) {
    for (CompetitionTheoryPaperSnapshotEntity paper : rows) {
      when(theoryPapers.selectById(paper.getId())).thenReturn(paper);
    }
  }

  private void questions(long paperId, int count) {
    List<CompetitionTheoryPaperQuestionEntity> rows = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      CompetitionTheoryPaperQuestionEntity question = new CompetitionTheoryPaperQuestionEntity();
      question.setId(paperId * 10 + index);
      question.setPaperSnapshotId(paperId);
      question.setSourceQuestionId(8000L + index);
      question.setSortOrder(index + 1);
      question.setQuestionType("single");
      question.setContent("题干" + index);
      question.setOptionsJson("[{\"key\":\"A\",\"text\":\"选项\"}]");
      question.setCorrectAnswersJson("[\"A\"]");
      question.setScore(BigDecimal.ONE);
      rows.add(question);
    }
    when(theoryQuestions.selectByPaper(paperId)).thenReturn(rows);
  }

  private CompetitionStagePaperAssignmentPlayerRow player(
      Long profileId, String name, String no, Long paperId, String paperCode) {
    CompetitionStagePaperAssignmentPlayerRow row = new CompetitionStagePaperAssignmentPlayerRow();
    row.setContestantProfileId(profileId);
    row.setContestantMemberId(profileId + 1000);
    row.setRegistrationId(profileId + 2000);
    row.setContestantName(name);
    row.setContestantNo(no);
    row.setPaperSnapshotId(paperId);
    row.setPaperCode(paperCode);
    row.setAssignmentStatus("confirmed");
    return row;
  }

  private CompetitionTheoryPaperSnapshotEntity paper(
      Long id, String code, String title, int durationMinutes) {
    CompetitionTheoryPaperSnapshotEntity paper = new CompetitionTheoryPaperSnapshotEntity();
    paper.setId(id);
    paper.setPaperCode(code);
    paper.setSourceTitle(title);
    paper.setVersionNo(1);
    paper.setDurationMinutes(durationMinutes);
    paper.setQuestionCount(1);
    paper.setTotalScore(BigDecimal.TEN);
    paper.setDeleted(0);
    return paper;
  }
}
