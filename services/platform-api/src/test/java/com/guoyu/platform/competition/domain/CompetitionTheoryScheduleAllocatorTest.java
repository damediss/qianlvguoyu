package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.Candidate;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.RoomSlot;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.SessionSlot;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheoryScheduleAllocatorTest {

  private final CompetitionTheoryScheduleAllocator allocator =
      new CompetitionTheoryScheduleAllocator();

  @Test
  void allocatesEveryCandidateOnceAcrossSessionsRoomsAndSeats() {
    List<Candidate> candidates = java.util.stream.LongStream.rangeClosed(1, 7)
        .mapToObj(id -> new Candidate(id, id + 100, id + 200, id + 300))
        .toList();
    List<SessionSlot> sessions = List.of(
        new SessionSlot(10L, "A", 4, List.of(
            new RoomSlot(101L, 1001L, 2, 1),
            new RoomSlot(102L, 1002L, 2, 2))),
        new SessionSlot(20L, "B", 3, List.of(
            new RoomSlot(201L, 2001L, 3, 1))));

    var result = allocator.allocate(candidates, sessions, 20260731L);

    assertThat(result).hasSize(7);
    assertThat(result).extracting(item -> item.candidate().contestantProfileId())
        .doesNotHaveDuplicates()
        .containsExactlyInAnyOrderElementsOf(
            candidates.stream().map(Candidate::contestantProfileId).toList());
    assertThat(result.stream().filter(item -> item.sessionId().equals(10L))).hasSize(4);
    assertThat(result.stream().filter(item -> item.sessionId().equals(20L))).hasSize(3);
    assertThat(result.stream()
        .filter(item -> item.sessionRoomId().equals(101L))
        .map(CompetitionTheoryScheduleAllocator.Allocation::seatNo))
        .containsExactly("001", "002");
  }

  @Test
  void sameSeedProducesSameTraceableAllocation() {
    List<Candidate> candidates = java.util.stream.LongStream.rangeClosed(1, 5)
        .mapToObj(id -> new Candidate(id, id, id, id))
        .toList();
    List<SessionSlot> sessions = List.of(new SessionSlot(
        10L, "A", 5, List.of(new RoomSlot(101L, 1001L, 5, 1))));

    assertThat(allocator.allocate(candidates, sessions, 42L))
        .containsExactlyElementsOf(allocator.allocate(candidates, sessions, 42L));
  }

  @Test
  void rejectsInsufficientCapacityAndInconsistentRoomSnapshot() {
    List<Candidate> candidates = List.of(
        new Candidate(1L, 1L, 1L, 1L),
        new Candidate(2L, 2L, 2L, 2L));

    assertThatThrownBy(() -> allocator.allocate(candidates, List.of(
        new SessionSlot(10L, "A", 1, List.of(new RoomSlot(101L, 1001L, 1, 1)))), 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("总容量不足");
    assertThatThrownBy(() -> allocator.allocate(candidates, List.of(
        new SessionSlot(10L, "A", 2, List.of(new RoomSlot(101L, 1001L, 1, 1)))), 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("容量快照不一致");
  }
}
