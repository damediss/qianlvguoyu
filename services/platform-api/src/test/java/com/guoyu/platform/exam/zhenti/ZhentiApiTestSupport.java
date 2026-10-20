package com.guoyu.platform.exam.zhenti;

import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiImportLogMapper;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import com.guoyu.platform.exam.zhenti.service.ZhentiIngestService;
import com.guoyu.platform.exam.zhenti.service.ZhentiSignatureService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 历年真题接口测试基类：跑真实 MySQL 测试库（courseware-e2e）+ 真实 Flyway 迁移。
 *
 * <p>所有用例统一挂到固定 examId，前置/后置清理同一批数据，避免污染测试库。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
abstract class ZhentiApiTestSupport {

  protected static final long TENANT_ID = 10159L;
  protected static final long EXAM_ID = 990_018_001L;

  // 共享组 IT 专用 fixture（方案 v2.1 §14.2）：考试计划 / 工种 / 课程均用 990_018_9xx 合成 id
  protected static final long SCOPE_EXAM_A = 990_018_101L;
  protected static final long SCOPE_EXAM_B = 990_018_102L;
  protected static final long SCOPE_EXAM_C = 990_018_103L;
  protected static final long SCOPE_EXAM_D = 990_018_104L;
  protected static final long SCOPE_EXAM_E = 990_018_105L;
  protected static final long SCOPE_WORK_TYPE = 990_018_901L;
  protected static final long SCOPE_WORK_TYPE_2 = 990_018_902L;
  protected static final long SCOPE_COURSE = 990_018_951L;
  protected static final long SCOPE_MISSING_WORK_TYPE = 990_018_999L;
  protected static final long SCOPE_EXAM_F = 990_018_106L;
  protected static final List<Long> SCOPE_EXAM_IDS = List.of(
      SCOPE_EXAM_A, SCOPE_EXAM_B, SCOPE_EXAM_C, SCOPE_EXAM_D, SCOPE_EXAM_E, SCOPE_EXAM_F);

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ExamZhentiMapper zhentiMapper;

  @Autowired
  protected ExamZhentiImportLogMapper importLogMapper;

  @Autowired
  protected ZhentiIngestService ingestService;

  @Autowired
  protected ZhentiSignatureService signatureService;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanBefore() {
    cleanUp();
  }

  @AfterEach
  void cleanAfter() {
    cleanUp();
  }

  protected void cleanUp() {
    java.util.List<Long> allExamIds = new java.util.ArrayList<>(
        java.util.List.of(EXAM_ID, EXAM_ID + 1));
    allExamIds.addAll(SCOPE_EXAM_IDS);
    String examPlaceholders = String.join(
        ",", java.util.Collections.nCopies(allExamIds.size(), "?"));
    java.util.List<Long> ids = jdbcTemplate.queryForList(
        "SELECT id FROM exam_zhenti WHERE exam_id IN (" + examPlaceholders + ")",
        Long.class, allExamIds.toArray());
    if (!ids.isEmpty()) {
      String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
      jdbcTemplate.update(
          "DELETE FROM sys_audit_log WHERE target_type = 'exam_zhenti' AND target_id IN ("
              + placeholders + ")", ids.toArray());
    }
    jdbcTemplate.update(
        "DELETE FROM exam_zhenti WHERE exam_id IN (" + examPlaceholders + ")",
        allExamIds.toArray());
    jdbcTemplate.update(
        "DELETE FROM exam_zhenti_import_log WHERE exam_id IN (" + examPlaceholders + ")",
        allExamIds.toArray());
    // 共享组 fixture：绑定 → 计划 → 合成工种/课程（id 均为 990_018_9xx，不会触碰真实数据）
    jdbcTemplate.update("DELETE FROM train_exam_work_type_list WHERE exam_id IN ("
        + examPlaceholders + ")", allExamIds.toArray());
    jdbcTemplate.update("DELETE FROM train_exam_list WHERE id IN (" + examPlaceholders
        + ") AND title LIKE '真题IT-%'", allExamIds.toArray());
    jdbcTemplate.update("DELETE FROM exam_work_type WHERE id IN (?, ?, ?)",
        SCOPE_WORK_TYPE, SCOPE_WORK_TYPE_2, SCOPE_MISSING_WORK_TYPE);
    jdbcTemplate.update("DELETE FROM exam_course WHERE id IN (?)", SCOPE_COURSE);
  }

  /** 共享组 fixture：合成工种（叶子节点）。 */
  protected void insertScopeWorkType(long id, String title) {
    jdbcTemplate.update("INSERT INTO exam_work_type (id, title) VALUES (?, ?)", id, title);
  }

  /** 共享组 fixture：合成课程（挂在指定工种+等级下）。 */
  protected void insertScopeCourse(long id, long workTypeId, int level, String title) {
    jdbcTemplate.update(
        "INSERT INTO exam_course (id, work_type_id, level, title) VALUES (?, ?, ?, ?)",
        id, workTypeId, level, title);
  }

  /** 共享组 fixture：考试计划（标题带 真题IT- 前缀，cleanUp 只删这类）。 */
  protected void insertScopeExam(long examId, String title) {
    jdbcTemplate.update(
        "INSERT INTO train_exam_list (id, tenant_id, title, level, org_id, deleted, create_time)"
            + " VALUES (?, ?, ?, '初/中/高/技师/高级技师', 0, 0, NOW())",
        examId, TENANT_ID, "真题IT-" + title);
  }

  /** 共享组 fixture：计划绑定（workTypeId 传 null 可造空工种行）。 */
  protected void insertScopeBinding(long examId, Long workTypeId, Integer level, Long courseId) {
    jdbcTemplate.update(
        "INSERT INTO train_exam_work_type_list (exam_id, work_type_id, level, course_id,"
            + " tenant_id, deleted, sort_number, create_time) VALUES (?, ?, ?, ?, ?, 0, 1, NOW())",
        examId, workTypeId, level, courseId, TENANT_ID);
  }

  protected static UserDetails principalWith(String... permissions) {
    return new PlatformUserPrincipal(
        1L, TENANT_ID, "zhenti-it", "真题集成测试", "admin", List.of(),
        List.of(permissions), null);
  }

  protected ExamZhentiEntity insertRow(int status, String year, String md5Seed) {
    ExamZhentiEntity entity = new ExamZhentiEntity();
    entity.setTenantId(TENANT_ID);
    entity.setExamId(EXAM_ID);
    entity.setTitle("测试真题-" + md5Seed);
    entity.setYear(year);
    entity.setFileKey("zhenti/" + EXAM_ID + "/" + md5Seed + ".pdf");
    entity.setFileSize(11L);
    entity.setFileMd5(md5Seed);
    entity.setSort(0);
    entity.setStatus(status);
    entity.setMetaPending(0);
    zhentiMapper.insert(entity);
    return entity;
  }

  protected static String uniqueMd5() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  protected static byte[] pdfBytes(String body) {
    return ("%PDF-1.4\n" + body + "\n%%EOF").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  }

  /** 构造 zip（每个 PDF 内容不同，避免同包内 MD5 去重互相影响）。 */
  protected static byte[] zipOfPdfs(int count, String prefix) {
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(
        buffer, java.nio.charset.StandardCharsets.UTF_8)) {
      for (int i = 1; i <= count; i++) {
        zip.putNextEntry(new java.util.zip.ZipEntry(prefix + i + "-2024.pdf"));
        zip.write(pdfBytes("batch-" + prefix + i + "-" + System.nanoTime()));
        zip.closeEntry();
      }
    } catch (java.io.IOException ex) {
      throw new IllegalStateException(ex);
    }
    return buffer.toByteArray();
  }
}
