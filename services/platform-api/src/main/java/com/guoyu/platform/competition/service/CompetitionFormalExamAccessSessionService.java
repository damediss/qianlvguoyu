package com.guoyu.platform.competition.service;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessCommandEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalExamAccessCommandStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamAccessCommandType;
import com.guoyu.platform.competition.enums.CompetitionFormalExamAccessSessionStatus;
import com.guoyu.platform.competition.exam.formal.CompetitionFormalExamAccessTokens;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessCommandMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionFormalExamAccessSessionService {

  public static final String REASON_STAFF_KICK = "工作人员清退";
  public static final String REASON_SAME_DEVICE = "same_device_restore";
  public static final String REASON_LOGOUT = "player_logout";
  private static final String CONFLICT_OTHER_DEVICE = "该选手已有有效考试会话，请联系监考人员";

  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalExamAccessSessionMapper accessSessionMapper;
  private final CompetitionFormalExamAccessCommandMapper commandMapper;
  private final StpLogic examStpLogic;
  private final Clock clock;
  private FormalExamRealtimePublisher realtimePublisher;

  public CompetitionFormalExamAccessSessionService(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalExamAccessSessionMapper accessSessionMapper,
      CompetitionFormalExamAccessCommandMapper commandMapper,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      Clock clock) {
    this.sessionMapper = sessionMapper;
    this.candidateExamMapper = candidateExamMapper;
    this.accessSessionMapper = accessSessionMapper;
    this.commandMapper = commandMapper;
    this.examStpLogic = examStpLogic;
    this.clock = clock;
  }

  @Autowired(required = false)
  public void setRealtimePublisher(FormalExamRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  public Map<Long, CompetitionFormalExamAccessSessionEntity> latestAccessByCandidateExamIds(
      List<Long> candidateExamIds) {
    Map<Long, CompetitionFormalExamAccessSessionEntity> result = new LinkedHashMap<>();
    if (candidateExamIds == null || candidateExamIds.isEmpty()) {
      return result;
    }
    List<CompetitionFormalExamAccessSessionEntity> rows =
        accessSessionMapper.selectLatestByCandidateExamIds(candidateExamIds);
    if (rows == null) {
      return result;
    }
    for (CompetitionFormalExamAccessSessionEntity row : rows) {
      if (row == null || row.getCandidateExamId() == null) {
        continue;
      }
      result.putIfAbsent(row.getCandidateExamId(), row);
    }
    return result;
  }

  public record IssuedAccess(
      Long accessSessionId,
      int generation,
      String deviceId,
      Long examSessionId,
      Long candidateExamId) {
  }

  @Transactional
  public IssuedAccess issueLogin(
      CompetitionPlayerIdentityEntity player, Long sessionId, String deviceIdValue) {
    if (player == null || player.getId() == null || player.getTenantId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null || !Objects.equals(session.getTenantId(), player.getTenantId())) {
      log.warn("Formal exam access login rejected: session missing or tenant mismatch, playerId={}, sessionId={}",
          player.getId(), sessionId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    List<CompetitionFormalCandidateExamEntity> byPlayer = safe(
        candidateExamMapper.selectBySessionAndPlayerId(sessionId, player.getId()));
    if (byPlayer.size() > 1) {
      log.error("Formal exam access login matched multiple CandidateExam rows, playerId={}, sessionId={}",
          player.getId(), sessionId);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR,
          "当前账号在本场比赛存在多份参赛档案，请联系工作人员");
    }
    if (byPlayer.isEmpty()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
    }
    CompetitionFormalCandidateExamEntity locked =
        candidateExamMapper.selectByIdForUpdate(byPlayer.getFirst().getId());
    if (locked == null || !Objects.equals(locked.getExamSessionId(), sessionId)
        || !Objects.equals(locked.getPlayerId(), player.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
    }
    String deviceId = normalizeDeviceId(deviceIdValue);
    List<CompetitionFormalExamAccessSessionEntity> actives = safe(
        accessSessionMapper.selectActiveByCandidateExamIdForUpdate(locked.getId()));
    if (actives.size() > 1) {
      log.error("Formal exam multiple ACTIVE access sessions, candidateExamId={}", locked.getId());
      throw new BusinessException(ErrorCode.CONFLICT, CONFLICT_OTHER_DEVICE);
    }
    if (actives.size() == 1) {
      CompetitionFormalExamAccessSessionEntity active = actives.getFirst();
      if (!sameDevice(active.getDeviceId(), deviceId)) {
        log.warn("Formal exam second device login rejected, playerId={}, candidateExamId={}",
            player.getId(), locked.getId());
        throw new BusinessException(ErrorCode.CONFLICT, CONFLICT_OTHER_DEVICE);
      }
      endActive(active, CompetitionFormalExamAccessSessionStatus.REPLACED, REASON_SAME_DEVICE, player.getId());
      writeCommand(active, CompetitionFormalExamAccessCommandType.LOGIN_REPLACE,
          REASON_SAME_DEVICE, player.getId(), null);
      revokeToken(active.getPlayerId(), active.getId(), active.getTokenFingerprint());
    }
    int generation = nextGeneration(locked.getId());
    LocalDateTime now = LocalDateTime.now(clock);
    CompetitionFormalExamAccessSessionEntity created = new CompetitionFormalExamAccessSessionEntity();
    created.setId(IdWorker.getId());
    created.setTenantId(player.getTenantId());
    created.setExamSessionId(session.getId());
    created.setCandidateExamId(locked.getId());
    created.setPlayerId(player.getId());
    created.setDeviceId(deviceId);
    created.setGeneration(generation);
    created.setStatus(CompetitionFormalExamAccessSessionStatus.ACTIVE.getCode());
    created.setLoginAt(now);
    created.setLastSeenAt(now);
    created.setCreatedAt(now);
    created.setUpdatedAt(now);
    created.setCreatedBy(player.getId());
    created.setUpdatedBy(player.getId());
    created.setDeleted(0);
    accessSessionMapper.insert(created);
    return new IssuedAccess(
        created.getId(), generation, deviceId, session.getId(), locked.getId());
  }

  @Transactional
  public void attachTokenFingerprint(Long accessSessionId, Long playerId, String token) {
    if (accessSessionId == null || !StringUtils.hasText(token)) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话创建失败");
    }
    int updated = accessSessionMapper.updateTokenFingerprint(
        accessSessionId,
        CompetitionFormalExamAccessTokens.fingerprint(token),
        LocalDateTime.now(clock),
        playerId);
    if (updated != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话创建失败");
    }
  }

  @Transactional
  public void logout(CompetitionExamPrincipal principal) {
    if (principal == null || principal.getAccessSessionId() == null) {
      if (principal != null) {
        examStpLogic.logout(principal.getPlayerId());
      }
      return;
    }
    List<CompetitionFormalExamAccessSessionEntity> actives = safe(
        accessSessionMapper.selectActiveByCandidateExamIdForUpdate(principal.getCandidateExamId()));
    for (CompetitionFormalExamAccessSessionEntity active : actives) {
      if (!Objects.equals(active.getId(), principal.getAccessSessionId())) {
        continue;
      }
      if (!Objects.equals(active.getPlayerId(), principal.getPlayerId())
          || !Objects.equals(active.getTenantId(), principal.getTenantId())) {
        log.warn("Formal exam logout rejected: access session mismatch, playerId={}",
            principal.getPlayerId());
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
      }
      endActive(active, CompetitionFormalExamAccessSessionStatus.LOGGED_OUT, REASON_LOGOUT,
          principal.getPlayerId());
      writeCommand(active, CompetitionFormalExamAccessCommandType.LOGOUT, REASON_LOGOUT,
          principal.getPlayerId(), principal.getPlayerId());
      revokeToken(active.getPlayerId(), active.getId(), active.getTokenFingerprint());
      return;
    }
    examStpLogic.kickout(principal.getPlayerId(),
        CompetitionFormalExamAccessTokens.device(principal.getAccessSessionId()));
  }

  @Transactional
  public void forceOffline(
      Long examSessionId, Long candidateExamId, Long playerId, Long operatorId, String reason) {
    List<CompetitionFormalExamAccessSessionEntity> actives = safe(
        accessSessionMapper.selectActiveByCandidateExamIdForUpdate(candidateExamId));
    if (actives.isEmpty()) {
      log.warn(
          "Formal exam force-offline idempotent: no ACTIVE access session, sessionId={}, candidateExamId={}, playerId={}",
          examSessionId, candidateExamId, playerId);
      return;
    }
    String endedReason = StringUtils.hasText(reason) ? reason.trim() : REASON_STAFF_KICK;
    for (CompetitionFormalExamAccessSessionEntity active : actives) {
      if (!Objects.equals(active.getExamSessionId(), examSessionId)
          || !Objects.equals(active.getPlayerId(), playerId)) {
        log.warn("Formal exam force-offline skipped mismatched ACTIVE row, accessSessionId={}",
            active.getId());
        continue;
      }
      int updated = endActive(active, CompetitionFormalExamAccessSessionStatus.KICKED, endedReason,
          operatorId);
      if (updated != 1) {
        continue;
      }
      CompetitionFormalExamAccessCommandEntity command = writeCommand(
          active, CompetitionFormalExamAccessCommandType.FORCE_OFFLINE, endedReason,
          operatorId, operatorId);
      if (realtimePublisher != null && command != null) {
        realtimePublisher.kicked(
            active.getTenantId(),
            active.getExamSessionId(),
            active.getCandidateExamId(),
            active.getId(),
            active.getGeneration(),
            command.getId(),
            toInstant(command.getExpiresAt()));
      }
      revokeToken(active.getPlayerId(), active.getId(), active.getTokenFingerprint());
      log.warn(
          "Formal exam force-offline, operatorId={}, sessionId={}, candidateExamId={}, playerId={}, accessSessionId={}, generation={}",
          operatorId, examSessionId, candidateExamId, playerId, active.getId(), active.getGeneration());
    }
  }

  public CompetitionFormalExamAccessSessionEntity requireActiveByToken(
      String token, Long tenantId, Long playerId) {
    if (!StringUtils.hasText(token) || tenantId == null || playerId == null) {
      return null;
    }
    CompetitionFormalExamAccessSessionEntity access =
        accessSessionMapper.selectByTokenFingerprint(
            CompetitionFormalExamAccessTokens.fingerprint(token));
    if (access == null
        || !CompetitionFormalExamAccessSessionStatus.ACTIVE.getCode().equals(access.getStatus())
        || !Objects.equals(access.getTenantId(), tenantId)
        || !Objects.equals(access.getPlayerId(), playerId)) {
      return null;
    }
    return access;
  }

  private int endActive(
      CompetitionFormalExamAccessSessionEntity active,
      CompetitionFormalExamAccessSessionStatus toStatus,
      String reason,
      Long updatedBy) {
    Integer generation = active.getGeneration();
    if (generation == null) {
      return 0;
    }
    return accessSessionMapper.markEndedIfGeneration(
        active.getId(),
        CompetitionFormalExamAccessSessionStatus.ACTIVE.getCode(),
        toStatus.getCode(),
        generation,
        reason,
        LocalDateTime.now(clock),
        updatedBy);
  }

  private CompetitionFormalExamAccessCommandEntity writeCommand(
      CompetitionFormalExamAccessSessionEntity access,
      CompetitionFormalExamAccessCommandType type,
      String reason,
      Long operatorId,
      Long actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    CompetitionFormalExamAccessCommandEntity command = new CompetitionFormalExamAccessCommandEntity();
    command.setId(IdWorker.getId());
    command.setTenantId(access.getTenantId());
    command.setExamSessionId(access.getExamSessionId());
    command.setCandidateExamId(access.getCandidateExamId());
    command.setAccessSessionId(access.getId());
    command.setGeneration(access.getGeneration());
    command.setCommandType(type.getCode());
    command.setReason(reason);
    command.setOperatorId(operatorId);
    command.setStatus(CompetitionFormalExamAccessCommandStatus.ISSUED.getCode());
    command.setExpiresAt(now.plusHours(24));
    command.setCreatedAt(now);
    command.setUpdatedAt(now);
    command.setCreatedBy(actorId);
    command.setUpdatedBy(actorId);
    command.setDeleted(0);
    commandMapper.insert(command);
    return command;
  }

  private Instant toInstant(LocalDateTime value) {
    if (value == null) {
      return Instant.now(clock).plusSeconds(24 * 3600);
    }
    return value.atZone(clock.getZone()).toInstant();
  }

  private void revokeToken(Long playerId, Long accessSessionId, String fingerprint) {
    if (playerId == null || accessSessionId == null) {
      return;
    }
    String device = CompetitionFormalExamAccessTokens.device(accessSessionId);
    try {
      examStpLogic.kickout(playerId, device);
    } catch (RuntimeException ex) {
      log.warn("Formal exam token kickout by device failed, playerId={}, accessSessionId={}",
          playerId, accessSessionId);
    }
    if (!StringUtils.hasText(fingerprint)) {
      return;
    }
    try {
      List<String> tokens = examStpLogic.getTokenValueListByLoginId(playerId);
      if (tokens == null) {
        return;
      }
      for (String token : tokens) {
        if (token != null && fingerprint.equals(CompetitionFormalExamAccessTokens.fingerprint(token))) {
          examStpLogic.logoutByTokenValue(token);
        }
      }
    } catch (RuntimeException ex) {
      log.warn("Formal exam token fingerprint revoke failed, playerId={}, accessSessionId={}",
          playerId, accessSessionId);
    }
  }

  private int nextGeneration(Long candidateExamId) {
    Integer max = accessSessionMapper.selectMaxGeneration(candidateExamId);
    int current = max == null ? 0 : max;
    return current + 1;
  }

  static String normalizeDeviceId(String deviceId) {
    if (!StringUtils.hasText(deviceId)) {
      return UUID.randomUUID().toString();
    }
    String trimmed = deviceId.trim();
    if (trimmed.length() > 64) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "设备标识无效");
    }
    return trimmed;
  }

  private static boolean sameDevice(String left, String right) {
    return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equals(right);
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : values.stream().filter(Objects::nonNull).toList();
  }
}
