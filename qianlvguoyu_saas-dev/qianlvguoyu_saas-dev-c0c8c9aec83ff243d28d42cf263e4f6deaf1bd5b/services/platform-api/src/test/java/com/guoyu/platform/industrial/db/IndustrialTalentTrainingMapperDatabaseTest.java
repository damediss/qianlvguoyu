package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.industrial.entity.IndustrialTalentTrainingEntity;
import com.guoyu.platform.industrial.mapper.IndustrialTalentTrainingMapper;
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
class IndustrialTalentTrainingMapperDatabaseTest {

  @Autowired
  private IndustrialTalentTrainingMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(1L);
    jdbcTemplate.execute("DROP TABLE IF EXISTS industrial_talent_training");
    jdbcTemplate.execute("""
        CREATE TABLE industrial_talent_training (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          name VARCHAR(200) NOT NULL,
          level VARCHAR(80) NOT NULL,
          work_type VARCHAR(120) NOT NULL,
          zhiwei VARCHAR(120) NOT NULL,
          education VARCHAR(120) NOT NULL,
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
  void mapperPersistsQueriesUpdatesAndLogicallyDeletesTrainingRows() {
    IndustrialTalentTrainingEntity entity = new IndustrialTalentTrainingEntity();
    entity.setId(810001L);
    entity.setTenantId(1L);
    entity.setName("千铝培训");
    entity.setLevel("高级工");
    entity.setWorkType("电解槽操作工");
    entity.setZhiwei("班组长");
    entity.setEducation("大专及以上");
    entity.setSortOrder(1);
    entity.setCreatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setDeleted(0);

    assertThat(mapper.insert(entity)).isEqualTo(1);

    IndustrialTalentTrainingEntity selected = mapper.selectById(810001L);
    assertThat(selected).isNotNull();
    assertThat(selected.getLevel()).isEqualTo("高级工");
    assertThat(selected.getWorkType()).isEqualTo("电解槽操作工");

    selected.setEducation("本科及以上");
    assertThat(mapper.updateById(selected)).isEqualTo(1);
    assertThat(mapper.selectById(810001L).getEducation()).isEqualTo("本科及以上");

    assertThat(mapper.deleteById(810001L)).isEqualTo(1);
    assertThat(mapper.selectById(810001L)).isNull();
    Integer deleted = jdbcTemplate.queryForObject(
        "SELECT deleted FROM industrial_talent_training WHERE id = 810001",
        Integer.class
    );
    assertThat(deleted).isEqualTo(1);
  }
}
