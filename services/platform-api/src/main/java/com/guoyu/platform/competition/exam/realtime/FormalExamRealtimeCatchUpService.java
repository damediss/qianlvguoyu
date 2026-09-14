package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import com.guoyu.platform.competition.vo.FormalExamRealtimeCatchUpVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamRealtimeCatchUpService {

  public enum Viewer {
    PLAYER,
    CONSOLE
  }

  private final CompetitionFormalExamOutboxMapper outboxMapper;
  private final FormalExamRealtimeProperties properties;
  private final ObjectMapper objectMapper;

  public FormalExamRealtimeCatchUpService(
      CompetitionFormalExamOutboxMapper outboxMapper,
      FormalExamRealtimeProperties properties,
      ObjectMapper objectMapper) {
    this.outboxMapper = outboxMapper;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public FormalExamRealtimeCatchUpVO catchUp(
      Long tenantId,
      Long examSessionId,
      String afterSeqValue,
      Integer limitValue,
      Viewer viewer,
      Long accessSessionId,
      Integer generation) {
    if (tenantId == null || examSessionId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试场次无效");
    }
    long afterSeq = parseAfterSeq(afterSeqValue);
    int limit = resolveLimit(limitValue);
    Long highWatermarkValue = outboxMapper.selectHighWatermark(tenantId, examSessionId);
    long highWatermark = highWatermarkValue == null ? 0L : highWatermarkValue;
    if (afterSeq >= highWatermark) {
      return new FormalExamRealtimeCatchUpVO(
          List.of(), String.valueOf(afterSeq), String.valueOf(highWatermark), false, false);
    }
    Long minSeqValue = outboxMapper.selectMinSeq(tenantId, examSessionId);
    long minSeq = minSeqValue == null ? 0L : minSeqValue;
    boolean snapshotRequired = afterSeq > 0 && minSeq > 0 && minSeq > afterSeq + 1;
    List<CompetitionFormalExamOutboxEntity> scanned = outboxMapper.selectCatchUp(
        tenantId, examSessionId, afterSeq, highWatermark, limit);
    if (scanned == null) {
      scanned = List.of();
    }
    long nextSeq = afterSeq;
    List<FormalExamRealtimeEnvelope> visible = new ArrayList<>();
    Predicate<FormalExamRealtimeEnvelope> allowed = visibility(viewer, accessSessionId, generation);
    for (CompetitionFormalExamOutboxEntity row : scanned) {
      if (row == null || row.getSeq() == null) {
        continue;
      }
      nextSeq = Math.max(nextSeq, row.getSeq());
      FormalExamRealtimeEnvelope envelope = parse(row);
      if (envelope == null || !allowed.test(envelope)) {
        continue;
      }
      visible.add(envelope.withPayload(FormalExamRealtimePayloadSanitizer.sanitize(envelope.payload())));
    }
    boolean hasMore = !scanned.isEmpty()
        && scanned.size() >= limit
        && nextSeq < highWatermark;
    if (snapshotRequired) {
      log.warn("Formal exam realtime catch-up requires snapshot, examSessionId={}, afterSeq={}, minSeq={}",
          examSessionId, afterSeq, minSeq);
    }
    return new FormalExamRealtimeCatchUpVO(
        List.copyOf(visible),
        String.valueOf(nextSeq),
        String.valueOf(highWatermark),
        hasMore,
        snapshotRequired);
  }

  private FormalExamRealtimeEnvelope parse(CompetitionFormalExamOutboxEntity row) {
    try {
      FormalExamRealtimeEnvelope envelope =
          objectMapper.readValue(row.getPayloadJson(), FormalExamRealtimeEnvelope.class);
      if (envelope == null) {
        return null;
      }
      return envelope.withSeq(row.getSeq());
    } catch (Exception ex) {
      log.warn("Formal exam realtime catch-up skipped malformed payload, outboxId={}", row.getId());
      return null;
    }
  }

  private static Predicate<FormalExamRealtimeEnvelope> visibility(
      Viewer viewer, Long accessSessionId, Integer generation) {
    if (viewer == Viewer.CONSOLE) {
      return envelope -> true;
    }
    return envelope -> visibleToPlayer(envelope, accessSessionId, generation);
  }

  static boolean visibleToPlayer(
      FormalExamRealtimeEnvelope envelope, Long accessSessionId, Integer generation) {
    if (envelope == null) {
      return false;
    }
    String target = envelope.target();
    if (!StringUtils.hasText(target) || FormalExamRealtimeTypes.TARGET_SESSION.equals(target)) {
      return true;
    }
    if (FormalExamRealtimeTypes.TARGET_ACCESS.equals(target)) {
      if (accessSessionId == null || !Objects.equals(String.valueOf(accessSessionId), envelope.accessSessionId())) {
        return false;
      }
      if (generation != null && envelope.generation() != null && !generation.equals(envelope.generation())) {
        return false;
      }
      return true;
    }
    return false;
  }

  private static long parseAfterSeq(String afterSeqValue) {
    if (!StringUtils.hasText(afterSeqValue)) {
      return 0L;
    }
    try {
      long value = Long.parseLong(afterSeqValue.trim());
      if (value < 0L) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "afterSeq 无效");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "afterSeq 无效");
    }
  }

  private int resolveLimit(Integer limitValue) {
    int max = Math.max(1, properties.getCatchUpMaxLimit());
    int fallback = Math.min(max, Math.max(1, properties.getCatchUpDefaultLimit()));
    if (limitValue == null) {
      return fallback;
    }
    if (limitValue < 1) {
      return 1;
    }
    return Math.min(limitValue, max);
  }
}
