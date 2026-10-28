package com.guoyu.platform.exam.answercheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * XLSX 导入防错 IT（方案 §14.4 / §16.2）：真实 xlsx 数值单元格、公式/错误值、预检与哈希。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
class QuestionImportNumericCellIT {

  private static final long TENANT_ID = 10159L;
  private static final int COURSE_ID = 990_028_951;
  private static final int WORK_TYPE_ID = 990_028_901;
  private static final int KNOWLEDGE_ID = 990_028_963;

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private JdbcTemplate jdbc;
  @MockBean
  private ObjectiveAnswerCheckAiService aiService;

  @BeforeEach
  void setUp() {
    clean();
    jdbc.update("DELETE FROM exam_work_type WHERE id = ?", WORK_TYPE_ID);
    jdbc.update("DELETE FROM exam_course WHERE id = ?", COURSE_ID);
    jdbc.update("INSERT INTO exam_work_type (id, tenant_id, title, parent_id, deleted, status) "
        + "VALUES (?, ?, 'IT导入工种', 0, 0, 1)", WORK_TYPE_ID, TENANT_ID);
    jdbc.update("INSERT INTO exam_course (id, tenant_id, work_type_id, title, level, deleted) "
        + "VALUES (?, ?, ?, 'IT导入课程', 1, 0)", COURSE_ID, TENANT_ID, WORK_TYPE_ID);
    jdbc.update("INSERT INTO exam_knowledge_structure (id, tenant_id, course_id, parent_id, title, "
        + "type, level, status, deleted) VALUES (?, ?, ?, 0, '导入知识点', 1, 1, 1, 0)",
        KNOWLEDGE_ID, TENANT_ID, COURSE_ID);
  }

  @AfterEach
  void tearDown() {
    clean();
  }

  private void clean() {
    jdbc.update("DELETE FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-%'");
    jdbc.update("DELETE FROM exam_knowledge_structure WHERE id = ?", KNOWLEDGE_ID);
    jdbc.update("DELETE FROM exam_course WHERE id = ?", COURSE_ID);
    jdbc.update("DELETE FROM exam_work_type WHERE id = ?", WORK_TYPE_ID);
  }

  private UserDetails admin() {
    return new PlatformUserPrincipal(1L, TENANT_ID, "import-it", "导入测试", "admin",
        List.of("PLATFORM_ADMIN"), List.of("exam:question:create"), null);
  }

  private byte[] workbook(boolean hardError) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Sheet1");
      Row header = sheet.createRow(0);
      String[] headers = {"题型", "题干", "选项A", "选项B", "选项C", "选项D", "参考答案",
          "答案解析", "难度"};
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }
      Row first = sheet.createRow(1);
      first.createCell(0).setCellValue("单选题");
      first.createCell(1).setCellValue("IT-IMPORT-数值题");
      first.createCell(2).setCellValue(0.5);
      first.createCell(3).setCellValue(1.5);
      first.createCell(4).setCellValue("文本选项");
      first.createCell(5).setCellValue("另一个选项");
      first.createCell(6).setCellValue("A");
      first.createCell(7).setCellValue("解析");
      first.createCell(8).setCellValue("简单");

      Row second = sheet.createRow(2);
      second.createCell(0).setCellValue("单选题");
      second.createCell(1).setCellValue("IT-IMPORT-科学计数题");
      second.createCell(2).setCellValue("1.2E+3");
      second.createCell(3).setCellValue("正常选项");
      second.createCell(4).setCellValue("选项C");
      second.createCell(5).setCellValue("选项D");
      second.createCell(6).setCellValue("A");
      second.createCell(7).setCellValue(hardError ? "" : "解析");
      second.createCell(8).setCellValue("简单");

      if (hardError) {
        Row third = sheet.createRow(3);
        third.createCell(0).setCellValue("单选题");
        third.createCell(1).setCellValue("IT-IMPORT-硬错误题");
        third.createCell(2).setCellValue("#REF!");
        third.createCell(3).setCellValue("B");
        third.createCell(4).setCellValue("C");
        third.createCell(5).setCellValue("D");
        third.createCell(6).setCellValue("A");
        third.createCell(8).setCellValue("简单");
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private MockMultipartFile file(byte[] bytes) {
    return new MockMultipartFile("file", "import.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
  }

  @Test
  @DisplayName("预检只读：数值不截断、科学计数法为软警告、hash 可回传")
  void validateIsReadOnlyAndReportsWarnings() throws Exception {
    String response = mockMvc.perform(multipart("/exam/exam-details/import/validate")
            .file(file(workbook(false)))
            .param("courseId", String.valueOf(COURSE_ID))
            .header("X-Tenant-Id", TENANT_ID)
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalRows").value(2))
        .andExpect(jsonPath("$.data.hardErrorCount").value(0))
        .andExpect(jsonPath("$.data.warningCount").value(1))
        .andExpect(jsonPath("$.data.truncated").value(false))
        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    String sha = com.jayway.jsonpath.JsonPath.read(response, "$.data.fileSha256");
    assertThat(sha).hasSize(64);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-%'", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("正式导入：0.5/1.5 保真、warning 回传、非硬错误可入库")
  void importPreservesDecimalValues() throws Exception {
    byte[] bytes = workbook(false);
    String validate = mockMvc.perform(multipart("/exam/exam-details/import/validate")
            .file(file(bytes)).param("courseId", String.valueOf(COURSE_ID))
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    String sha = com.jayway.jsonpath.JsonPath.read(validate, "$.data.fileSha256");

    mockMvc.perform(multipart("/exam/exam-details/import")
            .file(file(bytes)).param("courseId", String.valueOf(COURSE_ID))
            .param("validationFileSha256", sha)
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.importedCount").value(2))
        .andExpect(jsonPath("$.data.warningCount").value(1))
        .andExpect(jsonPath("$.data.warnings[0].ruleCode")
            .value("EXCEL_SCIENTIFIC_NOTATION_SUSPECT"));

    List<String> contents = jdbc.queryForList(
        "SELECT content FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-数值题%'",
        String.class);
    assertThat(contents).hasSize(1);
    assertThat(contents.get(0)).contains("0.5").contains("1.5");
    assertThat(contents.get(0)).doesNotContain("A. 0" + (char) 10);
    assertThat(contents.get(0)).doesNotContain("B. 1" + (char) 10);
  }

  @Test
  @DisplayName("硬错误：预检报告行列并整批 400，0 行入库")
  void hardErrorRejectsWholeFile() throws Exception {
    byte[] bytes = workbook(true);
    mockMvc.perform(multipart("/exam/exam-details/import/validate")
            .file(file(bytes)).param("courseId", String.valueOf(COURSE_ID))
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hardErrorCount").value(1))
        .andExpect(jsonPath("$.data.errors[0].ruleCode").value("EXCEL_ERROR_LITERAL"))
        .andExpect(jsonPath("$.data.errors[0].row").value(4));

    mockMvc.perform(multipart("/exam/exam-details/import")
            .file(file(bytes)).param("courseId", String.valueOf(COURSE_ID))
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("格式错误")));
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-%'", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("哈希不一致：预检 A 却导入 B → 400")
  void mismatchedHashRejected() throws Exception {
    mockMvc.perform(multipart("/exam/exam-details/import")
            .file(file(workbook(false))).param("courseId", String.valueOf(COURSE_ID))
            .param("validationFileSha256", "0".repeat(64))
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            org.hamcrest.Matchers.containsString("预检的文件与实际导入的文件不一致")));
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-%'", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("企业题导入同样不再 int 截断，并返回 warning 契约")
  void companyImportPreservesDecimalValues() throws Exception {
    byte[] bytes;
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Sheet1");
      String[] headers = {"题型", "题干", "选项A", "选项B", "选项C", "选项D", "答案", "解析",
          "难度", "关键词"};
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("单选题");
      row.createCell(1).setCellValue("IT-IMPORT-企业数值题");
      row.createCell(2).setCellValue(0.5);
      row.createCell(3).setCellValue(1.5);
      row.createCell(4).setCellValue("C");
      row.createCell(5).setCellValue("D");
      row.createCell(6).setCellValue("A");
      row.createCell(7).setCellValue("解析");
      row.createCell(8).setCellValue("简单");
      row.createCell(9).setCellValue("关键词");
      workbook.write(out);
      bytes = out.toByteArray();
    }

    mockMvc.perform(multipart("/exam/company-questions/import")
            .file(new MockMultipartFile("file", "company.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes))
            .param("orgId", "1").param("orgName", "IT企业")
            .header("X-Tenant-Id", TENANT_ID).with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.importedCount").value(1))
        .andExpect(jsonPath("$.data.warningCount").value(0));

    String content = jdbc.queryForObject(
        "SELECT content FROM exam_exam_detail WHERE content LIKE '%IT-IMPORT-企业数值题%'",
        String.class);
    assertThat(content).contains("0.5").contains("1.5");
  }
}
