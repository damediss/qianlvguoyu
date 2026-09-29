package com.guoyu.platform.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ExamWorkTypeCoverageServiceTest {

  private final ExamWorkTypeMapper mapper = Mockito.mock(ExamWorkTypeMapper.class);
  private final ExamWorkTypeCoverageService service = new ExamWorkTypeCoverageService(mapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1001L, 1L, "admin", "管理员", "admin", List.of("PLATFORM_ADMIN"), List.of(), "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(mapper.selectList(any())).thenReturn(List.of(
        node(1, "电工", 0, 1, 0, 1),
        node(2, "低压电工", 1, 1, 0, 1),
        node(3, "高压电工", 2, 1, 0, 1),
        node(10, "高压焊接", 1, 1, 0, 1),
        node(4, "焊工", 0, 1, 0, 1),
        node(5, "电工维修", 0, 1, 0, 1),
        node(6, "重复工种", 0, 1, 0, 1),
        node(7, "重复工种", 6, 1, 0, 1),
        node(8, "已删除工种", 0, 1, 1, 1),
        node(9, "其他租户工种", 0, 1, 0, 2)
    ));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void parentAuthorizationCoversDirectChild() {
    assertThat(service.assessorCovers("[1]", null, "考评员甲", List.of(2L))).isTrue();
  }

  @Test
  void parentAuthorizationCoversMultipleLevelsOfDescendants() {
    assertThat(service.assessorCovers("[1]", null, "考评员甲", List.of(3L))).isTrue();
  }

  @Test
  void sameWorkTypeAuthorizationCoversItself() {
    assertThat(service.assessorCovers("[2]", null, "考评员甲", List.of(2L))).isTrue();
  }

  @Test
  void childAuthorizationDoesNotCoverParent() {
    assertThat(service.assessorCovers("[2]", null, "考评员甲", List.of(1L))).isFalse();
  }

  @Test
  void siblingWorkTypesDoNotCoverEachOther() {
    assertThat(service.assessorCovers("[2]", null, "考评员甲", List.of(10L))).isFalse();
  }

  @Test
  void everyRequiredWorkTypeMustBeCovered() {
    assertThat(service.assessorCovers("[1]", null, "考评员甲", List.of(2L, 4L))).isFalse();
  }

  @Test
  void partialNameOverlapWithoutTreeRelationDoesNotCover() {
    assertThat(service.assessorCovers("[1]", null, "考评员甲", List.of(5L))).isFalse();
  }

  @Test
  void deletedAndCrossTenantWorkTypesAreRejected() {
    assertThatThrownBy(() -> service.assessorCovers("[1]", null, "考评员甲", List.of(8L)))
        .hasMessageContaining("不存在、已删除、已停用或不属于当前租户");
    assertThatThrownBy(() -> service.assessorCovers("[1]", null, "考评员甲", List.of(9L)))
        .hasMessageContaining("不存在、已删除、已停用或不属于当前租户");
    assertThatThrownBy(() -> service.assessorCovers("[8]", null, "考评员甲", List.of(2L)))
        .hasMessageContaining("不存在、已删除、已停用或不属于当前租户");
  }

  @Test
  void duplicateLegacyNameDefaultsToUniqueTopLevelOccupation() {
    assertThat(service.assessorCovers("[\"重复工种\"]", null, "考评员甲", List.of(6L))).isTrue();
  }

  @Test
  void uniquelyMatchedLegacyNameRemainsReadableAndCanCoverDescendant() {
    assertThat(service.assessorCovers("[\"电工\"]", null, "历史考评员", List.of(3L))).isTrue();
  }

  @Test
  void parentCycleIsRejected() {
    when(mapper.selectList(any())).thenReturn(List.of(
        node(20, "循环甲", 21, 1, 0, 1),
        node(21, "循环乙", 20, 1, 0, 1)
    ));

    assertThatThrownBy(() -> service.assessorCovers("[20]", null, "考评员甲", List.of(20L)))
        .hasMessageContaining("循环引用");
  }

  private ExamWorkTypeEntity node(int id, String title, int parentId, int status, int deleted, int tenantId) {
    ExamWorkTypeEntity entity = new ExamWorkTypeEntity();
    entity.setId(id);
    entity.setTitle(title);
    entity.setParentId(parentId);
    entity.setStatus(status);
    entity.setDeleted(deleted);
    entity.setTenantId(tenantId);
    return entity;
  }
}
