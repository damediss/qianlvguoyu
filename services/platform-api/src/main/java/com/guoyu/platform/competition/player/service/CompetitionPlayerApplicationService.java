package com.guoyu.platform.competition.player.service;

import com.guoyu.platform.common.util.SensitiveMaskUtils;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.enums.CompetitionRegistrationStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionNoticeInboxCompensationService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationSendReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationVerifyReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerLoginReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerChangeMobileVerifyReq;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeResult;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeService;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEntryVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerLoginResultVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerMeVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerSessionVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerSmsChallengeVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerApplicationService {

  private final CompetitionEventMapper eventMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;
  private final CompetitionPlayerDataHash dataHash;
  private final CompetitionPlayerSmsChallengeService smsChallengeService;
  private final StpLogic competitionPlayerStpLogic;
  private final PasswordEncoder passwordEncoder;
  private final CompetitionNoticeInboxCompensationService noticeInboxCompensationService;

  @Autowired
  public CompetitionPlayerApplicationService(
      CompetitionEventMapper eventMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionPlayerDataHash dataHash,
      CompetitionPlayerSmsChallengeService smsChallengeService,
      @Qualifier("competitionPlayerStpLogic") StpLogic competitionPlayerStpLogic,
      PasswordEncoder passwordEncoder,
      CompetitionNoticeInboxCompensationService noticeInboxCompensationService) {
    this.eventMapper = eventMapper;
    this.registrationMapper = registrationMapper;
    this.playerMapper = playerMapper;
    this.playerIdentityService = playerIdentityService;
    this.dataHash = dataHash;
    this.smsChallengeService = smsChallengeService;
    this.competitionPlayerStpLogic = competitionPlayerStpLogic;
    this.passwordEncoder = passwordEncoder;
    this.noticeInboxCompensationService = noticeInboxCompensationService;
  }

  CompetitionPlayerApplicationService(
      CompetitionEventMapper eventMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionPlayerDataHash dataHash,
      CompetitionPlayerSmsChallengeService smsChallengeService,
      StpLogic competitionPlayerStpLogic,
      PasswordEncoder passwordEncoder) {
    this(eventMapper, registrationMapper, playerMapper, playerIdentityService, dataHash,
        smsChallengeService, competitionPlayerStpLogic, passwordEncoder, null);
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerEntryVO resolveEntry(String entryCode) {
    if (entryCode == null || entryCode.isBlank()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手入口不存在");
    }
    CompetitionEventEntity event = eventMapper.selectByPlayerEntryCode(entryCode.trim());
    if (event == null || !CompetitionEventStatus.PUBLISHED.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手入口不存在");
    }
    return new CompetitionPlayerEntryVO(event.getTenantId(), event.getId(), event.getName(), event.getStatus());
  }

  @Transactional
  public CompetitionPlayerSmsChallengeVO sendActivationCode(
      CompetitionPlayerActivationSendReq request, HttpServletRequest servletRequest) {
    CompetitionRegistrationEntity registration = findEligibleActivationRegistration(
        request.entryCode(), request.mobile());
    CompetitionPlayerIdentityEntity player = findPlayer(registration.getPlayerId());
    if (CompetitionPlayerStatus.DISABLED.getCode().equals(player.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "选手身份已停用");
    }
    CompetitionPlayerSmsChallengeResult challenge = smsChallengeService.requestChallenge(
        currentTenantId(), player.getId(), player.getMobile(),
        CompetitionPlayerSmsPurpose.ACTIVATION, requestIpHash(servletRequest), requestDeviceHash(servletRequest));
    return new CompetitionPlayerSmsChallengeVO(challenge.challengeId(), challenge.expiresAt());
  }

  @Transactional
  public CompetitionPlayerSessionVO verifyActivation(
      CompetitionPlayerActivationVerifyReq request) {
    CompetitionRegistrationEntity registration = findEligibleActivationRegistration(
        request.entryCode(), request.mobile());
    CompetitionPlayerIdentityEntity player = findPlayer(registration.getPlayerId());
    smsChallengeService.verifyChallenge(player.getId(), request.challengeId(),
        CompetitionPlayerSmsPurpose.ACTIVATION, request.code());
    LocalDateTime now = LocalDateTime.now();
    if (!CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())) {
      player.setStatus(CompetitionPlayerStatus.ACTIVE.getCode());
      player.setActivatedAt(now);
    }
    if (player.getPasswordHash() == null || player.getPasswordHash().isBlank()) {
      String idCardNo = player.getIdCardNo();
      player.setPasswordHash(passwordEncoder.encode(initialPassword(idCardNo)));
      player.setPasswordInitializedAt(now);
    }
    if (playerMapper.updateById(player) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手激活失败");
    }
    registrationMapper.activateEligibleRegistrations(currentTenantId(), player.getId(), null, now);
    compensatePublishedNotices(currentTenantId(), registration.getEventId(), player.getId());
    return issueSession(player);
  }

  @Transactional
  public CompetitionPlayerLoginResultVO login(CompetitionPlayerLoginReq request) {
    String mobile = playerIdentityService.normalizeMobile(request.account());
    String mobileLookupHash = dataHash.lookupHash(mobile);
    List<CompetitionPlayerIdentityEntity> players =
        playerMapper.selectForUpdateByMobileLookupHashGlobally(mobileLookupHash);
    if (players != null && players.size() > 1) {
      log.error("Competition player login found duplicate global mobile identity, mobileLookupHash={}",
          mobileLookupHash);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛账号数据异常");
    }
    CompetitionPlayerIdentityEntity player = players == null || players.isEmpty() ? null : players.getFirst();
    if (player == null) {
      log.warn("Competition player login account does not exist, mobileLookupHash={}", mobileLookupHash);
      throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
    }
    if (CompetitionPlayerStatus.DISABLED.getCode().equals(player.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "选手身份已停用");
    }
    if (player.getPasswordHash() == null || player.getPasswordHash().isBlank()) {
      String idCardNo = player.getIdCardNo();
      if (!initialPassword(idCardNo).equals(request.password())) {
        log.warn("Competition player password login failed, tenantId={}, mobileLookupHash={}",
            player.getTenantId(), mobileLookupHash);
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
      }
      LocalDateTime initializedAt = LocalDateTime.now();
      String passwordHash = passwordEncoder.encode(request.password());
      int updated = playerMapper.initializePasswordIfAbsentGlobally(
          player.getId(), player.getTenantId(), passwordHash, initializedAt);
      if (updated != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛账号初始密码设置失败");
      }
      player.setPasswordHash(passwordHash);
      player.setPasswordInitializedAt(initializedAt);
    }
    if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
      log.warn("Competition player password login failed, tenantId={}, mobileLookupHash={}",
          player.getTenantId(), mobileLookupHash);
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    if (CompetitionPlayerStatus.PENDING_ACTIVATION.getCode().equals(player.getStatus())) {
      return CompetitionPlayerLoginResultVO.pendingActivation();
    }
    return CompetitionPlayerLoginResultVO.active(issueSession(player));
  }

  @Transactional
  public CompetitionPlayerMeVO me() {
    CompetitionPlayerPrincipal principal = currentPlayer();
    CompetitionPlayerIdentityEntity player = findPlayerWithoutLock(principal.getPlayerId());
    if (!CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "选手身份已停用");
    }
    List<CompetitionRegistrationEntity> registrations = registrationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getPlayerId, player.getId())
            .eq(CompetitionRegistrationEntity::getStatus, CompetitionRegistrationStatus.ELIGIBLE_ACTIVATED.getCode())
            .eq(CompetitionRegistrationEntity::getDeleted, 0));
    String maskedMobile = SensitiveMaskUtils.maskPhone(player.getMobile());
    if (registrations == null || registrations.isEmpty()) {
      return new CompetitionPlayerMeVO(player.getId(), player.getStatus(), maskedMobile, List.of());
    }
    List<Long> eventIds = registrations.stream().map(CompetitionRegistrationEntity::getEventId).distinct().toList();
    for (Long eventId : eventIds) {
      compensatePublishedNotices(principal.getTenantId(), eventId, player.getId());
    }
    List<CompetitionEventEntity> events = eventMapper.selectBatchIds(eventIds);
    Map<Long, CompetitionEventEntity> eventsById = new HashMap<>();
    if (events != null) {
      events.forEach(event -> eventsById.put(event.getId(), event));
    }
    List<CompetitionPlayerMeVO.EventSummary> summaries = registrations.stream()
        .map(registration -> eventsById.get(registration.getEventId()))
        .filter(event -> event != null)
        .map(event -> new CompetitionPlayerMeVO.EventSummary(
            event.getId(), event.getName(), CompetitionRegistrationStatus.ELIGIBLE_ACTIVATED.getCode()))
        .toList();
    return new CompetitionPlayerMeVO(player.getId(), player.getStatus(), maskedMobile, summaries);
  }

  public void logout() {
    currentPlayer();
    competitionPlayerStpLogic.logout();
  }

  @Transactional
  public CompetitionPlayerSessionVO verifyMobileChange(CompetitionPlayerChangeMobileVerifyReq request) {
    CompetitionPlayerPrincipal principal = currentPlayer();
    CompetitionPlayerIdentityEntity player = findPlayer(principal.getPlayerId());
    assertCurrentTenant(player.getTenantId());
    assertPlayerActive(player);
    CompetitionPlayerIdentityEntity changed = playerIdentityService.changeMobile(
        player.getTenantId(), player.getId(), request.idCardNo(), request.newMobile());
    competitionPlayerStpLogic.logout(changed.getId());
    return issueSession(changed);
  }

  private CompetitionRegistrationEntity findEligibleActivationRegistration(String entryCode, String mobile) {
    CompetitionEventEntity event = eventMapper.selectByPlayerEntryCode(entryCode == null ? null : entryCode.trim());
    if (event == null || !CompetitionEventStatus.PUBLISHED.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "激活信息无效");
    }
    assertCurrentTenant(event.getTenantId());
    String normalizedMobile = playerIdentityService.normalizeMobile(mobile);
    CompetitionRegistrationEntity registration = registrationMapper.selectEligibleUnactivatedForPlayerActivation(
        event.getTenantId(), event.getId(), dataHash.lookupHash(normalizedMobile));
    if (registration == null || registration.getPlayerId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "激活信息无效");
    }
    return registration;
  }

  private CompetitionPlayerIdentityEntity findPlayer(Long playerId) {
    CompetitionPlayerIdentityEntity player = playerMapper.selectForUpdateById(playerId);
    if (player == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手身份不存在");
    }
    return player;
  }

  private CompetitionPlayerIdentityEntity findPlayerWithoutLock(Long playerId) {
    CompetitionPlayerIdentityEntity player = playerMapper.selectById(playerId);
    if (player == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手身份不存在");
    }
    return player;
  }

  private CompetitionPlayerSessionVO issueSession(CompetitionPlayerIdentityEntity player) {
    competitionPlayerStpLogic.login(player.getId());
    String token = competitionPlayerStpLogic.getTokenValue();
    if (token == null || token.isBlank()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手会话创建失败");
    }
    return new CompetitionPlayerSessionVO(player.getId(), player.getTenantId(), token,
        "Bearer", competitionPlayerStpLogic.getTokenTimeout());
  }

  private void assertPlayerActive(CompetitionPlayerIdentityEntity player) {
    if (!CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "选手身份已停用");
    }
  }

  private void compensatePublishedNotices(Long tenantId, Long eventId, Long playerId) {
    if (noticeInboxCompensationService != null) {
      noticeInboxCompensationService.compensatePublishedNotices(tenantId, eventId, playerId);
    }
  }

  private String initialPassword(String idCardNo) {
    if (idCardNo == null || idCardNo.length() < 6) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手初始密码生成失败");
    }
    return idCardNo.substring(idCardNo.length() - 6);
  }


  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player request has no authenticated player token");
      throw ex;
    }
  }

  private Long currentTenantId() {
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少租户信息");
    }
    return tenantId;
  }

  private void assertCurrentTenant(Long tenantId) {
    if (!tenantId.equals(currentTenantId())) {
      log.warn("Competition player request tenant mismatch, requestedTenantId={}, currentTenantId={}",
          tenantId, TenantContextHolder.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "租户信息无效");
    }
  }

  private String requestIpHash(HttpServletRequest request) {
    return request == null ? null : dataHash.lookupHash("ip:" + request.getRemoteAddr());
  }

  private String requestDeviceHash(HttpServletRequest request) {
    String userAgent = request == null ? null : request.getHeader("User-Agent");
    return userAgent == null || userAgent.isBlank() ? null : dataHash.lookupHash("device:" + userAgent);
  }
}
