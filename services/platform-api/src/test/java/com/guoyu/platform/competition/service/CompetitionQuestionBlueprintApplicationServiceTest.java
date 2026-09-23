package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionQuestionBlueprintApplicationServiceTest {

  private final CompetitionQuestionBlueprintMapper blueprintMapper = Mockito.mock(CompetitionQuestionBlueprintMapper.class);
  private final CompetitionQuestionBlueprintNodeMapper nodeMapper = Mockito.mock(CompetitionQuestionBlueprintNodeMapper.class);
  private final CompetitionQuestionBlueprintBindingMapper bindingMapper =
      Mockito.mock(CompetitionQuestionBlueprintBindingMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper = Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionBlueprintWorkbookParser parser = Mockito.mock(CompetitionBlueprintWorkbookParser.class);
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
  private final CompetitionQuestionBlueprintApplicationService service =
      new CompetitionQuestionBlueprintApplicationService(
          blueprintMapper, nodeMapper, bindingMapper, eventMapper, workTypeMapper, levelMapper,
          parser, fileApplicationService, new ObjectMapper());

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:question-blueprint:import", "competition:question-blueprint:activate"),
        "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(eventMapper.selectOne(any())).thenReturn(event());
    when(workTypeMapper.selectOne(any())).thenReturn(workType());
    when(levelMapper.selectOne(any())).thenReturn(level());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void importRejectsSecondLiveBlueprintForSameWorkTypeAndLevel() throws java.io.IOException {
    CompetitionQuestionBlueprintEntity live = blueprint(8L, "active", 2);
    when(blueprintMapper.selectList(any())).thenReturn(List.of(live));
    MockMultipartFile file = new MockMultipartFile(
        "file", "竞赛理论细目表.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> service.importBlueprint(1001L, 3001L, 3101L, "theory", file, false))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("同一赛事工种等级只能有一份理论细目表")
        .hasMessageContaining("v2")
        .hasMessageContaining("启用");
    verify(parser, never()).parse(any(), any());
  }

  @Test
  void importContinuesWhenOnlyArchivedBlueprintExists() throws java.io.IOException {
    when(blueprintMapper.selectList(any())).thenReturn(List.of());
    when(parser.parse(any(), eq("theory"))).thenThrow(new IllegalArgumentException("stop-after-uniqueness"));
    MockMultipartFile file = new MockMultipartFile(
        "file", "竞赛理论细目表.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> service.importBlueprint(1001L, 3001L, 3101L, "theory", file, false))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("stop-after-uniqueness");
    verify(parser).parse(any(), eq("theory"));
  }

  @Test
  void archiveAllowsActiveBlueprintSoScopeCanBeReplaced() {
    CompetitionQuestionBlueprintEntity active = blueprint(8L, "active", 2);
    when(blueprintMapper.selectById(8L)).thenReturn(active);
    when(blueprintMapper.updateById(any(CompetitionQuestionBlueprintEntity.class))).thenReturn(1);

    service.archive(8L);

    ArgumentCaptor<CompetitionQuestionBlueprintEntity> captor =
        ArgumentCaptor.forClass(CompetitionQuestionBlueprintEntity.class);
    verify(blueprintMapper).updateById(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("archived");
  }

  @Test
  void listEffectiveBindingsRemapsArchivedFullCodeOntoLiveLeaves() {
    CompetitionQuestionBlueprintEntity live = blueprint(20L, "active", 2);
    CompetitionQuestionBlueprintEntity archived = blueprint(10L, "archived", 1);
    when(blueprintMapper.selectById(20L)).thenReturn(live);
    when(nodeMapper.selectByBlueprint(1L, 20L)).thenReturn(List.of(
        node(201L, 20L, 0L, "A-1-1-1"),
        node(202L, 20L, 201L, "A-1-1-1-x")));
    when(bindingMapper.selectByBlueprint(1L, 20L)).thenReturn(List.of());
    when(blueprintMapper.selectList(any())).thenReturn(List.of(archived));
    when(nodeMapper.selectByBlueprint(1L, 10L)).thenReturn(List.of(
        node(101L, 10L, 0L, "A-1-1-1"),
        node(102L, 10L, 101L, "A-1-1-1-x")));
    when(bindingMapper.selectByBlueprint(1L, 10L)).thenReturn(List.of(binding(501L, 10L, 102L)));

    List<CompetitionQuestionBlueprintBindingEntity> effective = service.listEffectiveBindings(1L, 20L);

    assertThat(effective).hasSize(1);
    assertThat(effective.get(0).getQuestionId()).isEqualTo(501L);
    assertThat(effective.get(0).getBlueprintId()).isEqualTo(20L);
    assertThat(effective.get(0).getBlueprintNodeId()).isEqualTo(202L);
  }

  @Test
  void persistInheritedBindingsCopiesRemappedRowsOntoLiveBlueprint() {
    CompetitionQuestionBlueprintEntity live = blueprint(20L, "active", 2);
    CompetitionQuestionBlueprintEntity archived = blueprint(10L, "archived", 1);
    when(blueprintMapper.selectById(20L)).thenReturn(live);
    when(nodeMapper.selectByBlueprint(1L, 20L)).thenReturn(List.of(node(202L, 20L, 0L, "A-1")));
    when(bindingMapper.selectByBlueprint(1L, 20L)).thenReturn(List.of());
    when(blueprintMapper.selectList(any())).thenReturn(List.of(archived));
    when(nodeMapper.selectByBlueprint(1L, 10L)).thenReturn(List.of(node(102L, 10L, 0L, "A-1")));
    when(bindingMapper.selectByBlueprint(1L, 10L)).thenReturn(List.of(binding(501L, 10L, 102L)));
    when(bindingMapper.insert(any(CompetitionQuestionBlueprintBindingEntity.class))).thenReturn(1);

    int saved = service.persistInheritedBindings(1L, 20L, 9L);

    assertThat(saved).isEqualTo(1);
    ArgumentCaptor<CompetitionQuestionBlueprintBindingEntity> captor =
        ArgumentCaptor.forClass(CompetitionQuestionBlueprintBindingEntity.class);
    verify(bindingMapper).insert(captor.capture());
    assertThat(captor.getValue().getQuestionId()).isEqualTo(501L);
    assertThat(captor.getValue().getBlueprintId()).isEqualTo(20L);
    assertThat(captor.getValue().getBlueprintNodeId()).isEqualTo(202L);
    assertThat(captor.getValue().getMatchMode()).isEqualTo("full_code");
  }

  @Test
  void liveConflictMessageNamesWorkTypeLevelLimit() {
    assertThat(CompetitionQuestionBlueprintApplicationService.liveBlueprintConflictMessage("theory", 2, "active"))
        .contains("同一赛事工种等级只能有一份理论细目表")
        .contains("v2（启用）");
  }

  private CompetitionQuestionBlueprintEntity blueprint(Long id, String status, int version) {
    CompetitionQuestionBlueprintEntity entity = new CompetitionQuestionBlueprintEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setEventWorkTypeId(3001L);
    entity.setEventWorkTypeLevelId(3101L);
    entity.setContentType("theory");
    entity.setStatus(status);
    entity.setVersionNo(version);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionQuestionBlueprintNodeEntity node(Long id, Long blueprintId, Long parentId, String code) {
    CompetitionQuestionBlueprintNodeEntity node = new CompetitionQuestionBlueprintNodeEntity();
    node.setId(id);
    node.setBlueprintId(blueprintId);
    node.setParentId(parentId);
    node.setFullCode(code);
    node.setName(code);
    node.setSortOrder(1);
    return node;
  }

  private CompetitionQuestionBlueprintBindingEntity binding(Long questionId, Long blueprintId, Long nodeId) {
    CompetitionQuestionBlueprintBindingEntity binding = new CompetitionQuestionBlueprintBindingEntity();
    binding.setId(questionId);
    binding.setQuestionId(questionId);
    binding.setBlueprintId(blueprintId);
    binding.setBlueprintNodeId(nodeId);
    binding.setMatchMode("full_code");
    return binding;
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventWorkTypeEntity workType() {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(3001L);
    entity.setEventId(1001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventWorkTypeLevelEntity level() {
    CompetitionEventWorkTypeLevelEntity entity = new CompetitionEventWorkTypeLevelEntity();
    entity.setId(3101L);
    entity.setEventId(1001L);
    entity.setEventWorkTypeId(3001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }
}
