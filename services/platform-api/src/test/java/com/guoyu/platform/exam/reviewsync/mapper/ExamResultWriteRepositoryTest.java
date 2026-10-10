package com.guoyu.platform.exam.reviewsync.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * exam_result 同步直写（Q3a/Q9）：SQL 与双库时代一字不改（exam_type=5、tenant_id=10159 硬编码、
 * NOW()、旧式 VALUES 写法避 Druid wall 拦截），仅数据源换成主库 JdbcTemplate。
 */
@ExtendWith(MockitoExtension.class)
class ExamResultWriteRepositoryTest {

  private static final String EXPECTED_SQL =
      "INSERT INTO exam_result (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id,"
          + " platform_user_id, user_answer, tenant_id, create_time, deleted)"
          + " VALUES (?, ?, 0, 0, ?, 5, ?, ?, ?, 10159, NOW(), 0)";

  @Mock
  private JdbcTemplate jdbcTemplate;

  private ExamResultWriteRepository repository;

  @BeforeEach
  void setUp() {
    repository = new ExamResultWriteRepository(jdbcTemplate);
  }

  @Test
  void insertExamResultUsesExactSqlAndParamOrder() {
    repository.insertExamResult("single", 440L, 900L, 42L, "对", new BigDecimal("2"));

    // 参数顺序对齐 PHP trainsave 口径：type, exam_plan_id, get_score, exam_id, platform_user_id, user_answer
    ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
    verify(jdbcTemplate).update(eq(EXPECTED_SQL), args.capture());
    assertThat(args.getValue()).containsExactly("single", 440L, new BigDecimal("2"), 900L, 42L, "对");
  }

  @Test
  void insertFailurePropagates() {
    // INSERT 失败必须抛出——由调用方（answer/mockSubmit）所在事务回滚，不容许静默吞掉（方案 §4-C）
    doThrow(new DuplicateKeyException("dup"))
        .when(jdbcTemplate).update(anyString(), any(Object[].class));

    assertThatThrownBy(() ->
        repository.insertExamResult("single", 440L, 900L, 42L, "对", new BigDecimal("2")))
        .isInstanceOf(DuplicateKeyException.class);
  }
}
