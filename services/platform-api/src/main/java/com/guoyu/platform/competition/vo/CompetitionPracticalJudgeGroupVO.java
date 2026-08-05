package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionPracticalJudgeGroupVO(
    Long id, Long sessionId, Long planId, Integer groupNo, String groupName,
    Integer officialCount, String status, List<CompetitionPracticalJudgeMemberVO> members) { }
