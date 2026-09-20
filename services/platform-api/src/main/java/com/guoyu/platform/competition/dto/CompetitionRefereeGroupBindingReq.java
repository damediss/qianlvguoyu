package com.guoyu.platform.competition.dto;

/** Null group id intentionally clears a previous player-to-group binding. */
public record CompetitionRefereeGroupBindingReq(Long refereeGroupId) {
}
