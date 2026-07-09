package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * App 端智能组卷/模拟考/错题重练相关表。
 *
 * <p>exam_user_answer_log  — 用户答题明细（每题作答对错，错题本/考点预测基础）
 * <p>exam_mock_paper       — App 端生成的模拟考卷（一次组卷=一张卷）
 */
public class V20260706_001__create_app_exam_tables extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    try (Statement stmt = conn.createStatement()) {
      if (!tableExists(conn, "exam_user_answer_log")) {
        stmt.execute("""
            CREATE TABLE exam_user_answer_log (
              id BIGINT NOT NULL PRIMARY KEY,
              user_id BIGINT NOT NULL COMMENT '答题学员',
              tenant_id BIGINT NOT NULL COMMENT '租户',
              question_id INT NOT NULL COMMENT '题目 id (exam_exam_detail.id)',
              knowledge_id INT NULL COMMENT '知识点 id (冗余，便于聚合)',
              question_type VARCHAR(16) NULL COMMENT '题型: single/multiple/judge/subjective',
              user_answer VARCHAR(2000) NULL COMMENT '学员作答',
              is_right TINYINT NOT NULL DEFAULT 0 COMMENT '1对 0错',
              source VARCHAR(32) NOT NULL DEFAULT 'mock' COMMENT '来源: mock=模考 wrong=错题重练 practice=随机练习',
              paper_id INT NULL COMMENT '关联模拟考卷 id (exam_mock_paper.id)',
              spend_ms BIGINT NOT NULL DEFAULT 0 COMMENT '本题耗时(毫秒)',
              create_time DATETIME NOT NULL,
              INDEX idx_eual_user (tenant_id, user_id, is_right),
              INDEX idx_eual_paper (paper_id),
              INDEX idx_eual_knowledge (tenant_id, knowledge_id, is_right)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App 端用户答题明细'
            """);
      }
      if (!tableExists(conn, "exam_mock_paper")) {
        stmt.execute("""
            CREATE TABLE exam_mock_paper (
              id INT NOT NULL PRIMARY KEY,
              user_id BIGINT NOT NULL COMMENT '所属学员',
              tenant_id BIGINT NOT NULL,
              title VARCHAR(128) NOT NULL COMMENT '试卷标题',
              question_ids VARCHAR(2000) NOT NULL COMMENT '题目 id 列表(英文逗号分隔)',
              total_count INT NOT NULL DEFAULT 0 COMMENT '题目数',
              total_point INT NOT NULL DEFAULT 0 COMMENT '总分',
              pass_point INT NOT NULL DEFAULT 0 COMMENT '及格分',
              time_limit INT NOT NULL DEFAULT 0 COMMENT '限时(分钟)',
              mode VARCHAR(16) NOT NULL DEFAULT 'mock' COMMENT 'mock=模拟考 wrong=错题重练 random=随机练习',
              score INT NOT NULL DEFAULT 0 COMMENT '本次得分(交卷后填)',
              right_count INT NOT NULL DEFAULT 0 COMMENT '答对题数',
              status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending=未开始 doing=答题中 done=已交卷',
              create_time DATETIME NOT NULL,
              finish_time DATETIME NULL,
              INDEX idx_emp_user (tenant_id, user_id, status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App 端模拟考卷'
            """);
      }
    }
  }

  private static boolean tableExists(Connection conn, String table) throws Exception {
    DatabaseMetaData meta = conn.getMetaData();
    String catalog = conn.getCatalog();
    try (ResultSet rs = meta.getTables(catalog, null, table, new String[]{"TABLE"})) {
      if (rs.next()) return true;
    }
    try (ResultSet rs = meta.getTables(catalog, null, table.toUpperCase(), new String[]{"TABLE"})) {
      return rs.next();
    }
  }
}
