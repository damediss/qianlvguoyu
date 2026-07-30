package com.guoyu.platform.industrial.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.industrial.entity.IndustrialPlatformItemEntity;
import com.guoyu.platform.industrial.mapper.IndustrialPlatformItemMapper;
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
class IndustrialPlatformItemMapperDatabaseTest {

  @Autowired
  private IndustrialPlatformItemMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(1L);
    jdbcTemplate.execute("DROP TABLE IF EXISTS industrial_platform_item");
    jdbcTemplate.execute("""
        CREATE TABLE industrial_platform_item (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          category VARCHAR(40) NOT NULL,
          company VARCHAR(200) NOT NULL,
          name VARCHAR(200) NOT NULL,
          nums INT NULL,
          item_date DATE NULL,
          state VARCHAR(80) NULL,
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
  void mapperPersistsQueriesUpdatesAndLogicallyDeletesRows() {
    IndustrialPlatformItemEntity entity = new IndustrialPlatformItemEntity();
    entity.setId(9001L);
    entity.setTenantId(1L);
    entity.setCategory("master");
    entity.setCompany("Guoyu Aluminium");
    entity.setName("Master Studio");
    entity.setNums(12);
    entity.setItemDate(LocalDate.of(2026, 5, 27));
    entity.setSortOrder(10);
    entity.setCreatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 27, 9, 0));
    entity.setDeleted(0);

    assertThat(mapper.insert(entity)).isEqualTo(1);

    IndustrialPlatformItemEntity selected = mapper.selectById(9001L);
    assertThat(selected).isNotNull();
    assertThat(selected.getCompany()).isEqualTo("Guoyu Aluminium");
    assertThat(selected.getName()).isEqualTo("Master Studio");

    selected.setName("Updated Master Studio");
    assertThat(mapper.updateById(selected)).isEqualTo(1);
    assertThat(mapper.selectById(9001L).getName()).isEqualTo("Updated Master Studio");

    assertThat(mapper.deleteById(9001L)).isEqualTo(1);
    assertThat(mapper.selectById(9001L)).isNull();
    Integer deleted = jdbcTemplate.queryForObject(
        "SELECT deleted FROM industrial_platform_item WHERE id = 9001",
        Integer.class
    );
    assertThat(deleted).isEqualTo(1);
  }
}
