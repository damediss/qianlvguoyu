package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.industrial.entity.IndustrialTalentServiceEntity;
import com.guoyu.platform.industrial.mapper.IndustrialTalentServiceMapper;
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
class IndustrialTalentServiceMapperDatabaseTest {

  @Autowired
  private IndustrialTalentServiceMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(1L);
    jdbcTemplate.execute("DROP TABLE IF EXISTS industrial_talent_service");
    jdbcTemplate.execute("""
        CREATE TABLE industrial_talent_service (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          title VARCHAR(300) NOT NULL,
          source VARCHAR(120) NOT NULL,
          service_date VARCHAR(40) NOT NULL,
          sort_order INT NOT NULL DEFAULT 100,
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
  void mapperPersistsQueriesUpdatesAndLogicallyDeletesServiceRows() {
    IndustrialTalentServiceEntity entity = new IndustrialTalentServiceEntity();
    entity.setId(810101L);
    entity.setTenantId(1L);
    entity.setTitle("2025.8.5技师认定交流会");
    entity.setSource("千铝工投");
    entity.setServiceDate("2025.9.10");
    entity.setSortOrder(1);
    entity.setCreatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setDeleted(0);

    assertThat(mapper.insert(entity)).isEqualTo(1);

    IndustrialTalentServiceEntity selected = mapper.selectById(810101L);
    assertThat(selected).isNotNull();
    assertThat(selected.getTitle()).contains("技师认定");
    assertThat(selected.getServiceDate()).isEqualTo("2025.9.10");

    selected.setSource("千铝培训");
    assertThat(mapper.updateById(selected)).isEqualTo(1);
    assertThat(mapper.selectById(810101L).getSource()).isEqualTo("千铝培训");

    assertThat(mapper.deleteById(810101L)).isEqualTo(1);
    assertThat(mapper.selectById(810101L)).isNull();
    Integer deleted = jdbcTemplate.queryForObject(
        "SELECT deleted FROM industrial_talent_service WHERE id = 810101",
        Integer.class
    );
    assertThat(deleted).isEqualTo(1);
  }
}
