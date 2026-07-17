package com.guoyu.platform.competition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.competition.converter.CompetitionEventConverter;
import com.guoyu.platform.competition.dto.CompetitionEventDraftReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionNoticeStatus;
import com.guoyu.platform.competition.enums.CompetitionRegistrationSource;
import com.guoyu.platform.competition.enums.CompetitionRegistrationStatus;
import com.guoyu.platform.competition.enums.CompetitionRosterImportStatus;
import com.guoyu.platform.competition.enums.CompetitionRosterValidationStatus;
import com.guoyu.platform.competition.enums.CompetitionTaskTemplateType;
import com.guoyu.platform.common.domain.BaseEntity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionFoundationContractTest {

  @Test
  void entitiesMapToTheEightFoundationTablesAndShareBaseEntity() {
    List<Class<?>> entities = List.of(
        CompetitionEventEntity.class, CompetitionEventOrgEntity.class, CompetitionStageEntity.class,
        CompetitionTaskTemplateEntity.class, CompetitionRosterImportEntity.class,
        CompetitionRosterItemEntity.class, CompetitionRegistrationEntity.class, CompetitionNoticeEntity.class);
    List<String> tables = List.of(
        "competition_event", "competition_event_org", "competition_stage", "competition_task_template",
        "competition_roster_import", "competition_roster_item", "competition_registration", "competition_notice");
    for (int i = 0; i < entities.size(); i++) {
      assertEquals(tables.get(i), entities.get(i).getAnnotation(TableName.class).value());
      assertEquals(BaseEntity.class, entities.get(i).getSuperclass());
    }
  }

  @Test
  void enumsUseContractStorageValues() {
    assertEquals(List.of("platform", "enterprise_internal"), codes(CompetitionEventScope.values()));
    assertEquals(List.of("draft", "published", "closed", "archived"), codes(CompetitionEventStatus.values()));
    assertEquals(List.of("invited", "active", "suspended", "closed"), codes(CompetitionEventOrgStatus.values()));
    assertEquals(List.of("validating", "validation_failed", "pending_confirm", "confirmed"), codes(CompetitionRosterImportStatus.values()));
    assertEquals(List.of("valid", "invalid"), codes(CompetitionRosterValidationStatus.values()));
    assertEquals(List.of("imported_pending_confirm", "pending_audit", "supplement_required", "rejected", "eligible_unactivated", "withdrawn", "expired"), codes(CompetitionRegistrationStatus.values()));
    assertEquals(List.of("enterprise_import"), codes(CompetitionRegistrationSource.values()));
    assertEquals(List.of("notice_ack", "check_in", "file_submission"), codes(CompetitionTaskTemplateType.values()));
    assertEquals(List.of("draft", "published"), codes(CompetitionNoticeStatus.values()));
  }

  @Test
  void converterDoesNotExposeEntityAsResponseAndPreservesDraftFields() {
    CompetitionEventDraftReq request = new CompetitionEventDraftReq();
    request.setName("基础赛事");
    request.setCode("FOUNDATION-001");
    request.setScope(CompetitionEventScope.ENTERPRISE_INTERNAL.getCode());
    request.setOwnerOrgId(22L);
    request.setRegistrationAuditRequired(1);

    CompetitionEventEntity entity = CompetitionEventConverter.toEntity(request);
    assertNotNull(entity);
    assertEquals(request.getName(), entity.getName());
    assertEquals(request.getCode(), entity.getCode());
    assertEquals(request.getScope(), entity.getScope());
    assertEquals(request.getOwnerOrgId(), entity.getOwnerOrgId());
    assertEquals(request.getRegistrationAuditRequired(), entity.getRegistrationAuditRequired());
    assertNotNull(CompetitionEventConverter.toVO(entity));
  }

  @Test
  void migrationContainsExactlyTheEightFoundationTablesAndNoSensitiveColumns() throws IOException {
    String migration = readResource("db/migration/V20260715_005__create_competition_foundation_tables.sql");
    List<String> tables = List.of(
        "competition_event", "competition_event_org", "competition_stage", "competition_task_template",
        "competition_roster_import", "competition_roster_item", "competition_registration", "competition_notice");
    tables.forEach(table -> assertTrue(migration.contains("CREATE TABLE " + table), table));
  }

  @Test
  void menuMigrationBindsImplementedCompetitionPageToEventViewPermission() throws IOException {
    String migration = readResource("db/migration/V20260716_006__add_competition_management_menu.sql");
    assertTrue(migration.contains("competition_foundation"));
    assertTrue(migration.contains("competition_event"));
    assertTrue(migration.contains("/competition/events"));
    assertTrue(migration.contains("competition/CompetitionEventManagementPage"));
    assertTrue(migration.contains("competition:event:view"));
    assertTrue(migration.contains("sys_role_menu"));
  }

  private static List<String> codes(Enum<?>[] values) {
    return java.util.Arrays.stream(values)
        .map(value -> {
          try {
            return (String) value.getClass().getMethod("getCode").invoke(value);
          } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
          }
        })
        .toList();
  }

  private static String readResource(String path) throws IOException {
    try (InputStream input = CompetitionFoundationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertNotNull(input, path);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
