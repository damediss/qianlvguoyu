package com.guoyu.platform.competition.exam.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.exam.dto.CompetitionExamLoginReq;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerSaveRequestMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionExamApplicationServiceTest {

  @Test
  void rejectsSecondDeviceBeforeLookingUpAnExamAssignment() {
    CompetitionPlayerIdentityMapper playerMapper = mock(CompetitionPlayerIdentityMapper.class);
    CompetitionPlayerIdentityDomainService identityService =
        mock(CompetitionPlayerIdentityDomainService.class);
    CompetitionPlayerDataHash dataHash = mock(CompetitionPlayerDataHash.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    CompetitionTheoryCandidateAssignmentMapper assignmentMapper =
        mock(CompetitionTheoryCandidateAssignmentMapper.class);
    StpLogic examStpLogic = mock(StpLogic.class);
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(77L);
    player.setStatus(CompetitionPlayerStatus.ACTIVE.getCode());
    player.setPasswordHash("encoded");
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup"))
        .thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(examStpLogic.isLogin(77L)).thenReturn(true);
    CompetitionExamApplicationService service = new CompetitionExamApplicationService(
        playerMapper,
        identityService,
        dataHash,
        passwordEncoder,
        assignmentMapper,
        mock(CompetitionTheoryAttemptMapper.class),
        mock(CompetitionTheoryExamSessionMapper.class),
        mock(CompetitionTheoryPaperQuestionMapper.class),
        mock(CompetitionTheoryAnswerMapper.class),
        mock(CompetitionTheoryAnswerSaveRequestMapper.class),
        mock(CompetitionTheoryExamSessionTokenAuditMapper.class),
        mock(CompetitionTheorySubmissionService.class),
        examStpLogic,
        new ObjectMapper(),
        Clock.systemUTC());

    assertThatThrownBy(() -> service.login(new CompetitionExamLoginReq(
        "13800000000", "secret12")))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("409");
    verify(assignmentMapper, never()).selectExamLoginCandidatesGlobally(77L, null);
  }
}
