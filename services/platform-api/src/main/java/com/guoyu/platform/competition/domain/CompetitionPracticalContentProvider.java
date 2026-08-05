package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageResourceBookingEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Practical scheduling is resolved by the practical execution APIs and is safe to preflight here. */
@Component
public class CompetitionPracticalContentProvider implements CompetitionStageContentProvider,
    StageAllocationCapability, StageAssignmentCapability, StageBookingProjectionCapability,
    StageSettlementFactCapability {
  @Override
  public String contentType() { return "practical"; }

  @Override
  public List<String> preflight(List<CompetitionStagePlanResourceEntity> resources) {
    return resources == null || resources.isEmpty() ? List.of("PRACTICAL_RESOURCE_MISSING") : List.of();
  }

  @Override
  public List<CompetitionStagePlanResourceEntity> allocate(List<CompetitionStagePlanResourceEntity> resources) {
    return resources == null ? List.of() : new ArrayList<>(resources);
  }

  @Override
  public List<String> confirmAllocation(List<CompetitionStagePlanResourceEntity> resources) {
    return preflight(resources);
  }

  @Override
  public List<String> validateAssignment(CompetitionStageAssignmentEntity assignment) {
    if (assignment == null || assignment.getPlanId() == null || assignment.getSessionId() == null
        || assignment.getContestantMemberId() == null) {
      return List.of("PRACTICAL_ASSIGNMENT_SCOPE_MISSING");
    }
    return List.of();
  }

  @Override
  public List<CompetitionStageResourceBookingEntity> projectBookings(
      List<CompetitionStagePlanResourceEntity> resources) {
    if (resources == null) return List.of();
    return resources.stream().filter(resource -> resource.getResourceId() != null
        && resource.getStartAt() != null && resource.getEndAt() != null)
        .map(resource -> {
          CompetitionStageResourceBookingEntity booking = new CompetitionStageResourceBookingEntity();
          booking.setPlanId(resource.getPlanId());
          booking.setEventId(resource.getEventId());
          booking.setStageId(resource.getStageId());
          booking.setResourceType(resource.getResourceType());
          booking.setResourceId(resource.getResourceId());
          booking.setStartAt(resource.getStartAt());
          booking.setEndAt(resource.getEndAt());
          booking.setStatus("reserved");
          return booking;
        }).toList();
  }

  @Override
  public List<CompetitionScoreSourceEntity> collectSettlementFacts(List<CompetitionScoreSourceEntity> sources) {
    if (sources == null) return List.of();
    return sources.stream().filter(source -> "practical".equals(source.getSourceType())
        && Objects.equals(source.getCurrentFlag(), 1)
        && "confirmed".equals(source.getConfirmationStatus())).toList();
  }
}
