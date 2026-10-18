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
    java.util.List<Long> ids = jdbcTemplate.queryForList(
        "SELECT id FROM exam_zhenti WHERE exam_id IN (?, ?)", Long.class, EXAM_ID, EXAM_ID + 1);
    if (!ids.isEmpty()) {
      String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
      jdbcTemplate.update(
          "DELETE FROM sys_audit_log WHERE target_type = 'exam_zhenti' AND target_id IN ("
              + placeholders + ")", ids.toArray());
    }
    jdbcTemplate.update(
        "DELETE FROM exam_zhenti WHERE exam_id IN (?, ?)", EXAM_ID, EXAM_ID + 1);
    jdbcTemplate.update(
        "DELETE FROM exam_zhenti_import_log WHERE exam_id IN (?, ?)", EXAM_ID, EXAM_ID + 1);
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
