package com.guoyu.platform.competition.vo;

import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimeEnvelope;
import java.util.List;

public record FormalExamRealtimeCatchUpVO(
    List<FormalExamRealtimeEnvelope> events,
    String nextSeq,
    String highWatermark,
    boolean hasMore,
    boolean snapshotRequired) {
}
