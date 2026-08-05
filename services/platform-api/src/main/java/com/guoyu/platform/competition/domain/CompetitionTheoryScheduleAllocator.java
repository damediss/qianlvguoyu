package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class CompetitionTheoryScheduleAllocator {

  public List<Allocation> allocate(
      List<Candidate> candidates, List<SessionSlot> sessions, long seed) {
    List<Candidate> shuffled = new ArrayList<>(safe(candidates));
    List<SessionSlot> orderedSessions = new ArrayList<>(safe(sessions));
    orderedSessions.sort(Comparator.comparing(SessionSlot::sessionCode));
    Collections.shuffle(shuffled, new Random(seed));

    int totalCapacity = orderedSessions.stream().mapToInt(SessionSlot::capacity).sum();
    if (shuffled.size() > totalCapacity) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "A/B场次总容量不足，缺少" + (shuffled.size() - totalCapacity) + "个座位");
    }

    List<Allocation> result = new ArrayList<>(shuffled.size());
    int candidateIndex = 0;
    for (SessionSlot session : orderedSessions) {
      List<RoomSlot> rooms = new ArrayList<>(safe(session.rooms()));
      rooms.sort(Comparator.comparingInt(RoomSlot::sortOrder));
      int declaredCapacity = rooms.stream().mapToInt(RoomSlot::capacity).sum();
      if (declaredCapacity != session.capacity()) {
        throw new BusinessException(ErrorCode.CONFLICT, "场次容量与考场容量快照不一致");
      }
      for (RoomSlot room : rooms) {
        for (int seatIndex = 1;
             seatIndex <= room.capacity() && candidateIndex < shuffled.size();
             seatIndex++) {
          Candidate candidate = shuffled.get(candidateIndex++);
          result.add(new Allocation(
              candidate, session.sessionId(), room.sessionRoomId(), room.roomId(),
              String.format("%03d", seatIndex)));
        }
      }
    }
    if (candidateIndex != shuffled.size()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "随机分配结果不完整");
    }
    return result;
  }

  private <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }

  public record Candidate(
      Long contestantProfileId,
      Long contestantMemberId,
      Long registrationId,
      Long playerId
  ) {
  }

  public record RoomSlot(
      Long sessionRoomId,
      Long roomId,
      int capacity,
      int sortOrder
  ) {
  }

  public record SessionSlot(
      Long sessionId,
      String sessionCode,
      int capacity,
      List<RoomSlot> rooms
  ) {
  }

  public record Allocation(
      Candidate candidate,
      Long sessionId,
      Long sessionRoomId,
      Long roomId,
      String seatNo
  ) {
  }
}
