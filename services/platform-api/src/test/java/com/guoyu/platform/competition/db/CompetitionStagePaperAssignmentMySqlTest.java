package com.guoyu.platform.competition.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledIfSystemProperty(named = "competition.mysql.integration", matches = "true")
class CompetitionStagePaperAssignmentMySqlTest {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("assignment_test").withUsername("test").withPassword("test");

  @Test void migrationAndNullAssignmentUpdateWorkOnMySql() throws Exception {
    try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), "test", "test")) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(
          "db/migration/V20260505_001__init_phase1_tables.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(
          "db/migration/V20260505_002__seed_demo_tenant_admin.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(
          "db/migration/V20260907_004__create_competition_stage_paper_assignments.sql"));
      try (var statement = connection.createStatement(); var rows = statement.executeQuery(
          "SELECT COUNT(*) FROM sys_permission WHERE code LIKE 'competition:stage-paper-assignment:%'")) {
        rows.next(); assertThat(rows.getInt(1)).isEqualTo(3);
      }
    }
    var configuration = new MybatisConfiguration();
    configuration.setMapUnderscoreToCamelCase(true);
    configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
        new UnpooledDataSource("com.mysql.cj.jdbc.Driver", MYSQL.getJdbcUrl(), "test", "test")));
    configuration.addMapper(CompetitionStagePaperAssignmentMapper.class);
    var factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
    try (var session = factory.openSession(false)) {
      var mapper = session.getMapper(CompetitionStagePaperAssignmentMapper.class);
      var assignment = new CompetitionStagePaperAssignmentEntity();
      assignment.setId(1L); assignment.setTenantId(1L); assignment.setEventId(2L);
      assignment.setStageId(3L); assignment.setEventWorkTypeId(4L); assignment.setEventWorkTypeLevelId(5L);
      assignment.setContestantProfileId(6L); assignment.setContestantMemberId(7L); assignment.setRegistrationId(8L);
      assignment.setPaperSnapshotId(9L); assignment.setStatus("confirmed"); assignment.setAllocationSource("manual");
      assignment.setConfirmedBy(10L); assignment.setConfirmedAt(LocalDateTime.now());
      assignment.setCreatedAt(LocalDateTime.now()); assignment.setUpdatedAt(LocalDateTime.now()); assignment.setDeleted(0);
      assertThat(mapper.insert(assignment)).isEqualTo(1);
      assignment.setPaperSnapshotId(null); assignment.setConfirmedBy(null); assignment.setConfirmedAt(null);
      assignment.setStatus("draft");
      assertThat(mapper.updateById(assignment)).isEqualTo(1);
      session.clearCache();
      var saved = mapper.selectById(1L);
      assertThat(saved.getPaperSnapshotId()).isNull();
      assertThat(saved.getConfirmedBy()).isNull();
      assertThat(saved.getConfirmedAt()).isNull();
      session.rollback();
      assertThat(mapper.selectById(1L)).isNull();
    }
  }
}
