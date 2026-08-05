package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;

/** The tenant/event/contestant scope captured while the profile row is locked. */
public record CompetitionContestantScope(
    CompetitionEventMemberEntity member,
    CompetitionContestantProfileEntity profile) {
}
