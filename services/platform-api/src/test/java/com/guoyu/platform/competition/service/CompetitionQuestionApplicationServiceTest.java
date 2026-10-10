package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionQuestionCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionQuestionApplicationServiceTest {

  private final CompetitionQuestionMapper questionMapper = Mockito.mock(CompetitionQuestionMapper.class);
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper =
      Mockito.mock(CompetitionPracticalQuestionDetailMapper.class);
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper =
      Mockito.mock(CompetitionPracticalQuestionScoreItemMapper.class);
  private final CompetitionQuestionImportBatchMapper batchMapper =
      Mockito.mock(CompetitionQuestionImportBatchMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionQuestionImportPersistenceService persistenceService =
      Mockito.mock(CompetitionQuestionImportPersistenceService.class);
  private final ThreadPoolTaskExecutor importExecutor = Mockito.mock(ThreadPoolTaskExecutor.class);
  private final CompetitionQuestionApplicationService service =
      new CompetitionQuestionApplicationService(
          questionMapper, practicalDetailMapper, scoreItemMapper, batchMapper, eventMapper,
          workTypeMapper, levelMapper, persistenceService, importExecutor);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:question:manage"), "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(eventMapper.selectOne(any())).thenReturn(event);
    CompetitionEventWorkTypeEntity workType = new CompetitionEventWorkTypeEntity();
    workType.setId(3001L);
    workType.setEventId(1001L);
    workType.setTenantId(1L);
    when(workTypeMapper.selectOne(any())).thenReturn(workType);
    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(3101L);
    level.setEventId(1001L);
    level.setEventWorkTypeId(3001L);
    level.setTenantId(1L);
    when(levelMapper.selectOne(any())).thenReturn(level);
    when(questionMapper.insert(any(CompetitionQuestionEntity.class))).thenReturn(1);
    when(practicalDetailMapper.selectByQuestion(anyLong(), anyLong())).thenReturn(null);
    when(scoreItemMapper.selectByQuestion(anyLong(), anyLong())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void assignsOneSequenceAcrossQuestionTypesInCreationOrder() {
    AtomicInteger queryCount = new AtomicInteger();
    when(questionMapper.selectMaxSortOrder(1L, 1001L, 3001L, 3101L, "theory", "single"))
        .thenAnswer(invocation -> nextMax(queryCount));
    when(questionMapper.selectMaxSortOrder(1L, 1001L, 3001L, 3101L, "theory", "multi"))
        .thenAnswer(invocation -> nextMax(queryCount));
    when(questionMapper.selectMaxSortOrder(1L, 1001L, 3001L, 3101L, "theory", "judge"))
        .thenAnswer(invocation -> nextMax(queryCount));
    when(questionMapper.selectMaxSortOrder(1L, 1001L, 3001L, 3101L, "theory", "fill"))
        .thenAnswer(invocation -> nextMax(queryCount));

    service.create(1001L, request("single", "A", "{\"A\":\"甲\"}"));
    service.create(1001L, request("multi", "AB", "{\"A\":\"甲\",\"B\":\"乙\"}"));
    service.create(1001L, request("judge", "T", null));
    service.create(1001L, request("fill", null, null));

    ArgumentCaptor<CompetitionQuestionEntity> captor =
        ArgumentCaptor.forClass(CompetitionQuestionEntity.class);
    Mockito.verify(questionMapper, Mockito.times(4)).insert(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(CompetitionQuestionEntity::getSortOrder)
        .containsExactly(1, 2, 3, 4);
  }

  private Integer nextMax(AtomicInteger queryCount) {
    int invocation = queryCount.getAndIncrement();
    return invocation == 0 ? null : invocation;
  }

  private CompetitionQuestionCreateReq request(String type, String answer, String options) {
    return new CompetitionQuestionCreateReq(
        3001L, 3101L, "theory", type, "题干", answer,
        "fill".equals(type) ? "填空答案" : null, options, null, "enabled", null, List.of());
  }
}
