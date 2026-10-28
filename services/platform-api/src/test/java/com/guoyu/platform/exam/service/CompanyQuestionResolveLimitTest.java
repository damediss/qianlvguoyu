package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.dto.CompanyQuestionCreateReq;
import com.guoyu.platform.exam.dto.CompanyQuestionUpdateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailCreateReq;
import com.guoyu.platform.exam.dto.ExamExamDetailUpdateReq;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.UserMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端解析 2000 字闸门（§5.8）：CompanyQuestion 与 ExamExamDetail 两服务的
 * 手工保存与 Excel 导入，2000 通过、2001 拒绝、导入报错定位行号。
 */
class CompanyQuestionResolveLimitTest {

  private static final String R2000 = "字".repeat(2000);
  private static final String R2001 = "字".repeat(2001);

  private CompanyQuestionApplicationService companyService;
  private ExamExamDetailApplicationService detailService;

  @BeforeEach
  void setUp() {
    companyService = new CompanyQuestionApplicationService(
        mock(ExamExamDetailMapper.class), mock(UserMapper.class));
    detailService = new ExamExamDetailApplicationService(
        mock(ExamExamDetailMapper.class), mock(ExamKnowledgeStructureMapper.class),
        mock(ExamCourseMapper.class), mock(ExamWorkTypeMapper.class), mock(UserMapper.class),
        mock(ExamKnowledgeStructureApplicationService.class),
        new com.fasterxml.jackson.databind.ObjectMapper());
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        7L, 10159L, "admin", "管理员", "admin", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  // ── CompanyQuestion：手工保存 ──

  @Test
  void companyCreateAndUpdateRespectLimit() {
    CompanyQuestionCreateReq ok = new CompanyQuestionCreateReq(
        "single", "题干", "A", 1L, "机构", 2, null, R2000, null, null, null);
    assertDoesNotThrow(() -> companyService.create(ok));

    CompanyQuestionCreateReq bad = new CompanyQuestionCreateReq(
        "single", "题干", "A", 1L, "机构", 2, null, R2001, null, null, null);
    BusinessException ex = assertThrows(BusinessException.class, () -> companyService.create(bad));
    assertEquals("解析不能超过 2000 字", ex.getMessage());

    ExamExamDetailEntity existing = new ExamExamDetailEntity();
    existing.setId(5);
    ExamExamDetailMapper mapper = mock(ExamExamDetailMapper.class);
    when(mapper.selectById(5)).thenReturn(existing);
    CompanyQuestionApplicationService svc =
        new CompanyQuestionApplicationService(mapper, mock(UserMapper.class));
    CompanyQuestionUpdateReq badUpdate = new CompanyQuestionUpdateReq(
        null, null, null, null, null, null, null, R2001, null, null, null);
    BusinessException ex2 = assertThrows(BusinessException.class, () -> svc.update(5, badUpdate));
    assertEquals("解析不能超过 2000 字", ex2.getMessage());
    CompanyQuestionUpdateReq okUpdate = new CompanyQuestionUpdateReq(
        null, null, null, null, null, null, null, R2000, null, null, null);
    assertDoesNotThrow(() -> svc.update(5, okUpdate));
  }

  // ── CompanyQuestion：Excel 导入 ──

  @Test
  void companyImportRejectsOverlongResolveWithRowNumber() throws Exception {
    byte[] okBytes = buildCompanyWorkbook(R2000);
    MultipartFile okFile = file(okBytes);
    assertDoesNotThrow(() -> companyService.importQuestions(okFile, 1L, "机构", 0));

    byte[] badBytes = buildCompanyWorkbook(R2001);
    MultipartFile badFile = file(badBytes);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> companyService.importQuestions(badFile, 1L, "机构", 0));
    // Excel 第 2 行（i=1，0 基表头）→ 报错带 1 基行号
    assertTrue(ex.getMessage().contains("解析不能超过 2000 字"), ex.getMessage());
    assertTrue(ex.getMessage().contains("第 2 行"), ex.getMessage());
  }

  // ── ExamExamDetail：手工保存 ──

  @Test
  void detailCreateAndUpdateRespectLimit() {
    ExamExamDetailCreateReq ok = new ExamExamDetailCreateReq(
        "single", "题干", "A", null, 2, null, R2000, null, null, null, null);
    assertDoesNotThrow(() -> detailService.create(ok));

    ExamExamDetailCreateReq bad = new ExamExamDetailCreateReq(
        "single", "题干", "A", null, 2, null, R2001, null, null, null, null);
    BusinessException ex = assertThrows(BusinessException.class, () -> detailService.create(bad));
    assertEquals("解析不能超过 2000 字", ex.getMessage());

    ExamExamDetailMapper mapper = mock(ExamExamDetailMapper.class);
    ExamExamDetailEntity existing = new ExamExamDetailEntity();
    existing.setId(6);
    when(mapper.selectById(6)).thenReturn(existing);
    ExamExamDetailApplicationService svc = new ExamExamDetailApplicationService(
        mapper, mock(ExamKnowledgeStructureMapper.class), mock(ExamCourseMapper.class),
        mock(ExamWorkTypeMapper.class), mock(UserMapper.class),
        mock(ExamKnowledgeStructureApplicationService.class),
        new com.fasterxml.jackson.databind.ObjectMapper());
    ExamExamDetailUpdateReq badUpdate = new ExamExamDetailUpdateReq(
        null, null, null, null, null, null, R2001, null, null, null, null, null, null);
    BusinessException ex2 = assertThrows(BusinessException.class, () -> svc.update(6, badUpdate));
    assertEquals("解析不能超过 2000 字", ex2.getMessage());
    ExamExamDetailUpdateReq okUpdate = new ExamExamDetailUpdateReq(
        null, null, null, null, null, null, R2000, null, null, null, null, null, null);
    assertDoesNotThrow(() -> svc.update(6, okUpdate));
  }

  // ── 正常短解析不回归 ──

  @Test
  void shortResolveUnaffected() {
    CompanyQuestionCreateReq req = new CompanyQuestionCreateReq(
        "single", "题干", "A", 1L, "机构", 2, null, "短解析", null, null, null);
    assertDoesNotThrow(() -> companyService.create(req));
    assertDoesNotThrow(() -> ResolvePlainText.assertEditableLength("【正确】答案：A。规程规定。"));
  }

  private static byte[] buildCompanyWorkbook(String resolve) throws Exception {
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("题库");
      Row header = sheet.createRow(0);
      for (int c = 0; c < 10; c++) {
        header.createCell(c).setCellValue("h" + c);
      }
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("单选题");
      row.createCell(1).setCellValue("题干");
      row.createCell(2).setCellValue("A");
      row.createCell(3).setCellValue("B");
      row.createCell(6).setCellValue("A");
      row.createCell(7).setCellValue(resolve);
      row.createCell(8).setCellValue("2");
      wb.write(out);
      return out.toByteArray();
    }
  }

  private static MultipartFile file(byte[] bytes) throws java.io.IOException {
    MultipartFile mock = mock(MultipartFile.class);
    org.mockito.Mockito.doAnswer(inv -> new ByteArrayInputStream(bytes))
        .when(mock).getInputStream();
    when(mock.isEmpty()).thenReturn(false);
    when(mock.getBytes()).thenReturn(bytes);
    return mock;
  }
}
