package com.guoyu.platform.certification.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.certification.entity.CertificationRiskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskMemberEntity;
import com.guoyu.platform.certification.mapper.CertificationRiskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMemberMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CertificationProjectMapperDatabaseTest {

  @Autowired
  private CertificationTaskMapper taskMapper;

  @Autowired
  private CertificationTaskMemberMapper memberMapper;

  @Autowired
  private CertificationRiskMapper riskMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(1L);
    jdbcTemplate.execute("DROP TABLE IF EXISTS certification_risk");
    jdbcTemplate.execute("DROP TABLE IF EXISTS certification_task_member");
    jdbcTemplate.execute("DROP TABLE IF EXISTS certification_task");
    jdbcTemplate.execute("""
        CREATE TABLE certification_task (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          task_no VARCHAR(80) NOT NULL,
          task_name VARCHAR(200) NOT NULL,
          task_manager_name VARCHAR(80) NOT NULL,
          company_name VARCHAR(200) NULL,
          certification_type VARCHAR(80) NULL,
          planned_people_count INT NOT NULL DEFAULT 0,
          plan_start_date DATE NULL,
          plan_end_date DATE NULL,
          exam_date DATE NULL,
          source_type VARCHAR(40) NOT NULL DEFAULT 'manual',
          source_name VARCHAR(120) NULL,
          current_phase VARCHAR(40) NOT NULL DEFAULT 'task_creation',
          status VARCHAR(40) NOT NULL DEFAULT 'in_progress',
          document_completed_count INT NOT NULL DEFAULT 0,
          document_total_count INT NOT NULL DEFAULT 5,
          phase_completed_count INT NOT NULL DEFAULT 0,
          phase_total_count INT NOT NULL DEFAULT 4,
          last_business_updated_at TIMESTAMP NULL,
          remark VARCHAR(1000) NULL,
          created_at TIMESTAMP NULL,
          updated_at TIMESTAMP NULL,
          created_by BIGINT NULL,
          updated_by BIGINT NULL,
          deleted INT NOT NULL DEFAULT 0
        )
        """);
    jdbcTemplate.execute("""
        CREATE TABLE certification_task_member (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          task_id BIGINT NOT NULL,
          document_id BIGINT NULL,
          user_id BIGINT NULL,
          member_name VARCHAR(80) NOT NULL,
          role_code VARCHAR(60) NULL,
          role_name VARCHAR(80) NULL,
          department_name VARCHAR(120) NULL,
          responsibility VARCHAR(500) NULL,
          start_date DATE NULL,
          end_date DATE NULL,
          input_frequency VARCHAR(40) NULL,
          workload_percent DECIMAL(8,2) NULL,
          phone VARCHAR(30) NULL,
          masked_phone VARCHAR(30) NULL,
          core_role TINYINT NOT NULL DEFAULT 0,
          sort_order INT NOT NULL DEFAULT 100,
          remark VARCHAR(500) NULL,
          created_at TIMESTAMP NULL,
          updated_at TIMESTAMP NULL,
          created_by BIGINT NULL,
          updated_by BIGINT NULL,
          deleted INT NOT NULL DEFAULT 0
        )
        """);
    jdbcTemplate.execute("""
        CREATE TABLE certification_risk (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          task_id BIGINT NOT NULL,
          document_id BIGINT NULL,
          risk_no VARCHAR(60) NOT NULL,
          risk_description VARCHAR(1000) NOT NULL,
          probability_code VARCHAR(20) NOT NULL,
          impact_code VARCHAR(20) NOT NULL,
          risk_level_code VARCHAR(40) NOT NULL,
          risk_level_score INT NOT NULL,
          response_plan VARCHAR(1000) NULL,
          owner_member_id BIGINT NULL,
          owner_user_id BIGINT NULL,
          owner_name VARCHAR(80) NULL,
          status VARCHAR(40) NOT NULL DEFAULT 'open',
          expected_close_date DATE NULL,
          closed_at TIMESTAMP NULL,
          closed_by BIGINT NULL,
          close_reason VARCHAR(500) NULL,
          sort_order INT NOT NULL DEFAULT 100,
          remark VARCHAR(500) NULL,
          created_at TIMESTAMP NULL,
          updated_at TIMESTAMP NULL,
          created_by BIGINT NULL,
          updated_by BIGINT NULL,
          deleted INT NOT NULL DEFAULT 0
        )
        """);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void mappersPersistQueryUpdateAndLogicallyDeleteProjectRows() {
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setId(930001L);
    task.setTenantId(1L);
    task.setTaskNo("RD-DB-001");
    task.setTaskName("Database certification project");
    task.setTaskManagerName("Manager");
    task.setCompanyName("Guoyu");
    task.setPlannedPeopleCount(20);
    task.setPlanStartDate(LocalDate.of(2026, 5, 29));
    task.setPlanEndDate(LocalDate.of(2026, 6, 10));
    task.setExamDate(LocalDate.of(2026, 6, 10));
    task.setSourceType("manual");
    task.setSourceName("Manual");
    task.setCurrentPhase("preparation");
    task.setStatus("running");
    task.setDocumentCompletedCount(0);
    task.setDocumentTotalCount(5);
    task.setPhaseCompletedCount(0);
    task.setPhaseTotalCount(5);
    task.setLastBusinessUpdatedAt(LocalDateTime.of(2026, 5, 29, 9, 0));
    task.setCreatedAt(LocalDateTime.of(2026, 5, 29, 9, 0));
    task.setUpdatedAt(LocalDateTime.of(2026, 5, 29, 9, 0));
    task.setDeleted(0);

    assertThat(taskMapper.insert(task)).isEqualTo(1);
    CertificationTaskEntity selectedTask = taskMapper.selectById(930001L);
    assertThat(selectedTask).isNotNull();
    assertThat(selectedTask.getTaskNo()).isEqualTo("RD-DB-001");

    selectedTask.setStatus("pending_review");
    assertThat(taskMapper.updateById(selectedTask)).isEqualTo(1);
    assertThat(taskMapper.selectById(930001L).getStatus()).isEqualTo("pending_review");

    CertificationTaskMemberEntity member = new CertificationTaskMemberEntity();
    member.setId(930101L);
    member.setTenantId(1L);
    member.setTaskId(930001L);
    member.setMemberName("Li Min");
    member.setRoleCode("material_specialist");
    member.setRoleName("Material specialist");
    member.setPhone("13800001234");
    member.setMaskedPhone("138****1234");
    member.setCoreRole(true);
    member.setSortOrder(1);
    member.setDeleted(0);
    assertThat(memberMapper.insert(member)).isEqualTo(1);
    assertThat(memberMapper.selectById(930101L).getMaskedPhone()).isEqualTo("138****1234");

    CertificationRiskEntity risk = new CertificationRiskEntity();
    risk.setId(930201L);
    risk.setTenantId(1L);
    risk.setTaskId(930001L);
    risk.setRiskNo("R-001");
    risk.setRiskDescription("Materials may be late");
    risk.setProbabilityCode("high");
    risk.setImpactCode("medium");
    risk.setRiskLevelCode("medium_high");
    risk.setRiskLevelScore(4);
    risk.setOwnerName("Manager");
    risk.setStatus("open");
    risk.setExpectedCloseDate(LocalDate.of(2026, 6, 1));
    risk.setSortOrder(1);
    risk.setDeleted(0);
    assertThat(riskMapper.insert(risk)).isEqualTo(1);

    risk.setStatus("closed");
    risk.setClosedAt(LocalDateTime.of(2026, 5, 30, 10, 0));
    assertThat(riskMapper.updateById(risk)).isEqualTo(1);
    assertThat(riskMapper.selectById(930201L).getStatus()).isEqualTo("closed");

    assertThat(taskMapper.deleteById(930001L)).isEqualTo(1);
    assertThat(taskMapper.selectById(930001L)).isNull();
    Integer deleted = jdbcTemplate.queryForObject(
        "SELECT deleted FROM certification_task WHERE id = 930001",
        Integer.class
    );
    assertThat(deleted).isEqualTo(1);
  }
}
