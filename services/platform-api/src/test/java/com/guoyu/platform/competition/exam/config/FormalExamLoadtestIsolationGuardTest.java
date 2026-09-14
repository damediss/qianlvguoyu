package com.guoyu.platform.competition.exam.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FormalExamLoadtestIsolationGuardTest {

  @Test
  void acceptsIsolatedLoadSchema() {
    FormalExamLoadtestIsolationGuard.assertJdbcUrl(
        "jdbc:mysql://127.0.0.1:13306/gjjddb_load?useUnicode=true");
    assertThat(FormalExamLoadtestIsolationGuard.schemaName(
        "jdbc:mysql://fel-mysql:3306/gjjddb_load")).isEqualTo("gjjddb_load");
    assertThat(FormalExamLoadtestIsolationGuard.hostName(
        "jdbc:mysql://127.0.0.1:13306/gjjddb_load")).isEqualTo("127.0.0.1");
  }

  @Test
  void rejectsSharedHostEvenWithLoadSchemaName() {
    assertThatThrownBy(() -> FormalExamLoadtestIsolationGuard.assertJdbcUrl(
        "jdbc:mysql://43.138.163.7:3308/gjjddb_load"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("43.138.163.7");
  }

  @Test
  void rejectsSharedAndProdSchemaNamesOnLocalhost() {
    assertThatThrownBy(() -> FormalExamLoadtestIsolationGuard.assertJdbcUrl(
        "jdbc:mysql://127.0.0.1:3306/gjjddb2026"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("gjjddb2026");
    assertThatThrownBy(() -> FormalExamLoadtestIsolationGuard.assertJdbcUrl(
        "jdbc:mysql://127.0.0.1:3306/gjjddbpro"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("gjjddbpro");
  }

  @Test
  void rejectsBlankUrl() {
    assertThatThrownBy(() -> FormalExamLoadtestIsolationGuard.assertJdbcUrl(" "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MYSQL_URL");
  }

  @Test
  void sanitizesPasswordQueryParameter() {
    assertThat(FormalExamLoadtestIsolationGuard.sanitizeJdbcUrl(
        "jdbc:mysql://127.0.0.1:3306/gjjddb_load?password=secret&useSSL=false"))
        .contains("password=***")
        .doesNotContain("secret");
  }
}
