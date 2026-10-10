package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionStageDraftSaveReq;
import com.guoyu.platform.competition.dto.CompetitionStageDraftStageReq;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionStageDraftSaveServiceTest {

  private final CompetitionStageMapper mapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionStageService service = new CompetitionStageService(mapper);

  @Test
  void replacesWithMixedUpdateCreateAndDelete() {
    CompetitionStageEntity preliminary = stage(5001L, "preliminary", 20);
    CompetitionStageEntity finalStage = stage(5002L, "final", 50);
    CompetitionStageEntity savedSemifinal = stage(5003L, "semifinal", 40);
    when(mapper.selectList(any())).thenReturn(List.of(preliminary, finalStage),
        List.of(preliminary, savedSemifinal));
    when(mapper.updateById(any(CompetitionStageEntity.class))).thenReturn(1);
    when(mapper.deleteById(5002L)).thenReturn(1);
    when(mapper.insert(any(CompetitionStageEntity.class))).thenAnswer(invocation -> {
      CompetitionStageEntity inserted = invocation.getArgument(0);
      inserted.setId(5003L);
      return 1;
    });

    List<CompetitionStageEntity> result = service.replaceDraft(1L, 1001L, 3000L,
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(5001L, "preliminary",
                LocalDateTime.of(2099, 1, 1, 9, 0), LocalDateTime.of(2099, 1, 1, 12, 0), "初赛"),
            new CompetitionStageDraftStageReq(null, "semifinal",
                LocalDateTime.of(2099, 1, 2, 9, 0), LocalDateTime.of(2099, 1, 2, 12, 0), "半决赛"))));

    assertThat(result).extracting(CompetitionStageEntity::getStageType)
        .containsExactly("preliminary", "semifinal");
    verify(mapper).updateById(preliminary);
    verify(mapper).insert(any(CompetitionStageEntity.class));
    verify(mapper).deleteById(5002L);
  }

  @Test
  void rejectsDuplicateStageTypeBeforeMutation() {
    when(mapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.replaceDraft(1L, 1001L, 3000L,
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(null, "preliminary", null, null, null),
            new CompetitionStageDraftStageReq(null, "preliminary", null, null, null)))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("同一赛事不能重复配置同一预设赛段");

    verify(mapper, never()).insert(any(CompetitionStageEntity.class));
    verify(mapper, never()).updateById(any(CompetitionStageEntity.class));
    verify(mapper, never()).deleteById(Mockito.<java.io.Serializable>any());
  }

  @Test
  void rejectsCrossEventStageId() {
    CompetitionStageEntity foreign = stage(5001L, "preliminary", 20);
    foreign.setEventId(2002L);
    when(mapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.replaceDraft(1L, 1001L, 3000L,
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(5001L, "preliminary", null, null, null)))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("赛段不属于当前租户或赛事");
  }

  @Test
  void rejectsOverlappingWindowsBeforeMutation() {
    when(mapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.replaceDraft(1L, 1001L, 3000L,
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(null, "preliminary",
                LocalDateTime.of(2099, 1, 1, 9, 0), LocalDateTime.of(2099, 1, 1, 12, 0), null),
            new CompetitionStageDraftStageReq(null, "final",
                LocalDateTime.of(2099, 1, 1, 11, 0), LocalDateTime.of(2099, 1, 1, 13, 0), null)))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("赛段时间窗口重叠或未按预设顺序递增");

    verify(mapper, never()).insert(any(CompetitionStageEntity.class));
  }

  @Test
  void createsDraftRowsInsideTheSelectedWorkTypeAndLevelScope() {
    when(mapper.selectList(any())).thenReturn(List.of(), List.of(stage(5004L, "preliminary", 20)));
    when(mapper.insert(any(CompetitionStageEntity.class))).thenAnswer(invocation -> {
      CompetitionStageEntity inserted = invocation.getArgument(0);
      inserted.setId(5004L);
      return 1;
    });

    List<CompetitionStageEntity> result = service.replaceDraftForScope(1L, 1001L, 3000L,
        2001L, "4", new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(null, "preliminary", null, null, "中级工赛段"))));

    assertThat(result).hasSize(1);
    var inserted = org.mockito.ArgumentCaptor.forClass(CompetitionStageEntity.class);
    verify(mapper).insert(inserted.capture());
    assertThat(inserted.getValue().getEventWorkTypeId()).isEqualTo(2001L);
    assertThat(inserted.getValue().getEventWorkTypeLevelCode()).isEqualTo("4");
  }

  @Test
  void keepsDraftStagesIndependentAcrossLevels() {
    when(mapper.selectList(any())).thenReturn(List.of(), List.of(), List.of(), List.of());
    when(mapper.insert(any(CompetitionStageEntity.class))).thenReturn(1);

    service.replaceDraftForScope(1L, 1001L, 3000L, 2001L, "4",
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(null, "preliminary", null, null, "中级工赛段"))));
    service.replaceDraftForScope(1L, 1001L, 3000L, 2001L, "5",
        new CompetitionStageDraftSaveReq(List.of(
            new CompetitionStageDraftStageReq(null, "preliminary", null, null, "初级工赛段"))));

    var inserted = org.mockito.ArgumentCaptor.forClass(CompetitionStageEntity.class);
    verify(mapper, Mockito.times(2)).insert(inserted.capture());
    assertThat(inserted.getAllValues())
        .extracting(CompetitionStageEntity::getEventWorkTypeLevelCode)
        .containsExactly("4", "5");
  }

  private CompetitionStageEntity stage(Long id, String type, int sortOrder) {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(id);
    stage.setTenantId(1L);
    stage.setEventId(1001L);
    stage.setName(type);
    stage.setStageType(type);
    stage.setSortOrder(sortOrder);
    stage.setStatus("draft");
    return stage;
  }
}
