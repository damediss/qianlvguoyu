package com.guoyu.platform.competition.exam.formal;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamSessionStatus;
import com.guoyu.platform.competition.vo.CompetitionFormalExamFinishBlockerVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class FormalExamSessionFinishRules {

  public static final String READY_CANDIDATE_REMAINS = "READY_CANDIDATE_REMAINS";
  public static final String UNRESOLVED_WAITING_CANDIDATE = "UNRESOLVED_WAITING_CANDIDATE";
  public static final String RUNNING_CANDIDATE_REMAINS = "RUNNING_CANDIDATE_REMAINS";
  public static final String CANDIDATE_TOTAL_MISMATCH = "CANDIDATE_TOTAL_MISMATCH";
  public static final String UNRESOLVED_CANDIDATE_STATUS = "UNRESOLVED_CANDIDATE_STATUS";

  private FormalExamSessionFinishRules() {
  }

  public static LocalDateTime deadlineTime(CompetitionFormalExamSessionEntity session) {
    if (session == null) {
      return null;
    }
    if (session.getDeadlineAt() != null) {
      return session.getDeadlineAt();
    }
    if (session.getActualStartTime() == null
        || session.getDurationSeconds() == null || session.getDurationSeconds() <= 0) {
      return null;
    }
    return session.getActualStartTime().plusSeconds(session.getDurationSeconds());
  }

  public static boolean deadlineReached(CompetitionFormalExamSessionEntity session, LocalDateTime now) {
    LocalDateTime deadline = deadlineTime(session);
    return deadline != null && now != null && !now.isBefore(deadline);
  }

  public static CandidateCounts counts(List<CompetitionFormalCandidateExamEntity> candidates) {
    int waiting = 0;
    int ready = 0;
    int running = 0;
    int submitted = 0;
    int autoSubmitted = 0;
    int forcedSubmitted = 0;
    int absent = 0;
    int total = 0;
    if (candidates != null) {
      for (CompetitionFormalCandidateExamEntity candidate : candidates) {
        if (candidate == null) {
          continue;
        }
        total++;
        CompetitionFormalCandidateExamStatus status =
            CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
        if (status == CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
          waiting++;
        } else if (status == CompetitionFormalCandidateExamStatus.READY) {
          ready++;
        } else if (status == CompetitionFormalCandidateExamStatus.RUNNING) {
          running++;
        } else if (status == CompetitionFormalCandidateExamStatus.SUBMITTED) {
          submitted++;
        } else if (status == CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED) {
          autoSubmitted++;
        } else if (status == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
          forcedSubmitted++;
        } else if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
          absent++;
        }
      }
    }
    return new CandidateCounts(
        waiting, ready, running, submitted, autoSubmitted, forcedSubmitted, absent, total);
  }

  public static List<CompetitionFormalExamFinishBlockerVO> blockers(
      CompetitionFormalExamSessionEntity session,
      CandidateCounts counts,
      LocalDateTime now) {
    if (session == null || counts == null
        || !CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())
        || !deadlineReached(session, now)) {
      return List.of();
    }
    List<CompetitionFormalExamFinishBlockerVO> blockers = new ArrayList<>();
    if (counts.ready() > 0) {
      blockers.add(new CompetitionFormalExamFinishBlockerVO(
          READY_CANDIDATE_REMAINS,
          counts.ready(),
          "存在 " + counts.ready() + " 名选手处于异常 READY 状态，需要人工处理后才能结束本场比赛"));
    }
    if (counts.waiting() > 0) {
      blockers.add(new CompetitionFormalExamFinishBlockerVO(
          UNRESOLVED_WAITING_CANDIDATE,
          counts.waiting(),
          "存在 " + counts.waiting() + " 名选手仍为待进入，缺考归档尚未完成"));
    }
    if (counts.running() > 0) {
      blockers.add(new CompetitionFormalExamFinishBlockerVO(
          RUNNING_CANDIDATE_REMAINS,
          counts.running(),
          "存在 " + counts.running() + " 名选手仍在考试中"));
    }
    Integer expected = session.getCandidateTotal();
    if (expected == null || expected != counts.total()) {
      blockers.add(new CompetitionFormalExamFinishBlockerVO(
          CANDIDATE_TOTAL_MISMATCH,
          counts.total(),
          "选手考试实例数量与场次记录不一致：记录 "
              + (expected == null ? 0 : expected) + " 人，实际 " + counts.total() + " 人"));
    }
    int classified = counts.waiting() + counts.ready() + counts.running()
        + counts.submitted() + counts.autoSubmitted() + counts.forcedSubmitted() + counts.absent();
    int unknown = counts.total() - classified;
    if (unknown > 0) {
      blockers.add(new CompetitionFormalExamFinishBlockerVO(
          UNRESOLVED_CANDIDATE_STATUS,
          unknown,
          "存在 " + unknown + " 名选手状态无法归入正式运行终态"));
    }
    return List.copyOf(blockers);
  }

  public static boolean finishEligible(
      CompetitionFormalExamSessionEntity session,
      CandidateCounts counts,
      LocalDateTime now) {
    if (session == null || counts == null
        || !CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())
        || !deadlineReached(session, now)
        || !blockers(session, counts, now).isEmpty()) {
      return false;
    }
    Integer expected = session.getCandidateTotal();
    return counts.waiting() == 0
        && counts.ready() == 0
        && counts.running() == 0
        && expected != null
        && expected == counts.total()
        && counts.closed() == counts.total()
        && counts.closed() == expected;
  }

  public record CandidateCounts(
      int waiting,
      int ready,
      int running,
      int submitted,
      int autoSubmitted,
      int forcedSubmitted,
      int absent,
      int total) {

    public int closed() {
      return submitted + autoSubmitted + forcedSubmitted + absent;
    }
  }
}
