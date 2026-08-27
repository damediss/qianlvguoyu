package com.guoyu.platform.exam.reviewsync.mapper;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * exam_result 同步直写（单库化改造后替代 PhpResultWritebackService 的异步回写，Q3a/Q9）。
 *
 * <p>口径（对齐 PHP trainsave）：type=题型、exam_plan_id=train_exam_list.id、paper_id=0、detail_id=0、
 * exam_id=题 id、platform_user_id、user_answer、get_score（实得分/对=题分/错=0）、
 * exam_type=5（AI 同步练习）、tenant_id=10159 硬编码、create_time=NOW()、deleted=0。仅 INSERT。
 *
 * <p>SQL 与双库时代一字不改，仅数据源换为主库 JdbcTemplate：tenant_id 不依赖请求 header、
 * 不经租户拦截器；id 走合并表 int AUTO_INCREMENT；旧式 VALUES 写法对 Druid wall 过滤器无拦截（坑 #21）。
 * 由作答/模考事务内同步调用——INSERT 失败使整个作答事务回滚（行为变化已在方案 §4-C 明示）。
 */
@Repository
public class ExamResultWriteRepository {

  private final JdbcTemplate jdbcTemplate;

  public ExamResultWriteRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void insertExamResult(String questionType, long examPlanId, long questionId, long platformUserId,
      String userAnswer, BigDecimal getScore) {
    jdbcTemplate.update(
        "INSERT INTO exam_result (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id,"
            + " platform_user_id, user_answer, tenant_id, create_time, deleted)"
            + " VALUES (?, ?, 0, 0, ?, 5, ?, ?, ?, 10159, NOW(), 0)",
        questionType, examPlanId, getScore, questionId, platformUserId, userAnswer);
  }
}
