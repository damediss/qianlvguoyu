package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageResourceBookingEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Theory scheduling is an existing provider boundary; the actual exam service owns its data. */
@Component
public class CompetitionTheoryContentProvider implements CompetitionStageContentProvider,
    StageAllocationCapability, StageAssignmentCapability, StageBookingProjectionCapability,
    StageSettlementFactCapability {
  @Override
  public String contentType() { return "theory"; }

  @Override
  public List<String> preflight(List<CompetitionStagePlanResourceEntity> resources) {
    return resources == null || resources.isEmpty() ? List.of("THEORY_RESOURCE_MISSING") : List.of();
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
    if (assignment == null || assignment.getPlanId() == null || assignment.getContestantMemberId() == null) {
      return List.of("THEORY_ASSIGNMENT_SCOPE_MISSING");
    }
    return List.of();
  }

  @Override
  public List<CompetitionStageResourceBookingEntity> projectBookings(
      List<CompetitionStagePlanResourceEntity> resources) {
    return project(resources);
  }

  @Override
  public List<CompetitionScoreSourceEntity> collectSettlementFacts(List<CompetitionScoreSourceEntity> sources) {
    if (sources == null) return List.of();
    return sources.stream().filter(source -> "theory".equals(source.getSourceType())
        && Objects.equals(source.getCurrentFlag(), 1)
        && "confirmed".equals(source.getConfirmationStatus())).toList();
  }

  private List<CompetitionStageResourceBookingEntity> project(List<CompetitionStagePlanResourceEntity> resources) {
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
}
