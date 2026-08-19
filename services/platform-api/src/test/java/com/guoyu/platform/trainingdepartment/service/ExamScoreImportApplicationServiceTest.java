package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.trainingdepartment.dto.ExamScoreImportCommitReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamScoreImportBatchEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamScoreImportBatchMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamScoreImportItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportCommitResultVO;
import com.guoyu.platform.trainingdepartment.vo.ExamScoreImportPreviewVO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamScoreImportApplicationServiceTest {

  @Mock TrainPeopleListMapper peopleMapper;
  @Mock TrainInfoClassMapper classMapper;
  @Mock TrainInfoListMapper trainInfoListMapper;
  @Mock TrainExamScoreImportBatchMapper batchMapper;
  @Mock TrainExamScoreImportItemMapper itemMapper;
  @Mock OrgMapper orgMapper;
  @Mock ExamWorkTypeMapper examWorkTypeMapper;
  @Mock TrainingOperatorScopeService operatorScopeService;

  ExamScoreImportApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ExamScoreImportApplicationService(
        new RensheScoreRosterParseService(),
        peopleMapper,
        classMapper,
        trainInfoListMapper,
        batchMapper,
        itemMapper,
        orgMapper,
        examWorkTypeMapper,
        operatorScopeService,
        new ObjectMapper());
  }

  @Test
  void resolveExamScore_prefersComprehensiveThenTheory() {
    var withBoth = new RensheScoreRosterParseService.ParsedRow(
        1, "A", "110101199001011234", null, null, null, null,
        new BigDecimal("70"), new BigDecimal("80"), new BigDecimal("75"), null);
    assertThat(ExamScoreImportApplicationService.resolveExamScore(withBoth))
        .isEqualByComparingTo("75");
    assertThat(ExamScoreImportApplicationService.theoryFallback(withBoth)).isFalse();

    var theoryOnly = new RensheScoreRosterParseService.ParsedRow(
        1, "A", "110101199001011234", null, null, null, null,
        new BigDecimal("70"), new BigDecimal("80"), null, null);
    assertThat(ExamScoreImportApplicationService.resolveExamScore(theoryOnly))
        .isEqualByComparingTo("70");
    assertThat(ExamScoreImportApplicationService.theoryFallback(theoryOnly)).isTrue();
  }

  @Test
  void previewAndCommit_doesNotWriteUnselectedClass() throws Exception {
    TrainInfoClassEntity clazz = classOf(101L, "技师班");
    TrainPeopleListEntity inClass = student(9001L, 101L, "陈朝永", "452631197706194818", null, null);
    TrainPeopleListEntity otherClass = student(9002L, 102L, "陈朝永", "452631197706194818",
        new BigDecimal("10"), new BigDecimal("11"));

    when(classMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(clazz));
    when(peopleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(inClass));
    when(orgMapper.selectBatchIds(any())).thenReturn(List.of());
    when(examWorkTypeMapper.selectList(any())).thenReturn(List.of());
    when(trainInfoListMapper.selectBatchIds(any())).thenReturn(List.of());
    when(peopleMapper.selectById(9001L)).thenReturn(inClass);
    when(batchMapper.insert(any(TrainExamScoreImportBatchEntity.class))).thenAnswer(inv -> {
      inv.getArgument(0, TrainExamScoreImportBatchEntity.class).setId(55L);
      return 1;
    });

    MockMultipartFile file = new MockMultipartFile(
        "file", "score.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        flatSheet("452631197706194818", "陈朝永", "79.5", "76", "79.4"));

    try (MockedStatic<SecurityContext> ignored = mockCurrentUser()) {
      ExamScoreImportPreviewVO preview = service.preview(List.of(101L), file);
      assertThat(preview.matched()).isEqualTo(1);
      assertThat(preview.items().get(0).status()).isEqualTo("MATCHED");
      assertThat(preview.items().get(0).targets()).hasSize(1);
      assertThat(preview.items().get(0).targets().get(0).peopleId()).isEqualTo(9001L);

      ExamScoreImportCommitReq req = new ExamScoreImportCommitReq();
      req.setPreviewId(preview.previewId());
      req.setPeopleIds(List.of(9001L));
      ExamScoreImportCommitResultVO result = service.commit(req);
      assertThat(result.written()).isEqualTo(1);
    }

    verify(peopleMapper).updateById(inClass);
    verify(peopleMapper, never()).selectById(9002L);
    assertThat(inClass.getExamScore()).isEqualByComparingTo("79.50");
    assertThat(inClass.getRealScore()).isEqualByComparingTo("79.40");
    assertThat(otherClass.getExamScore()).isEqualByComparingTo("10");
  }

  @Test
  void preview_nameMismatchStillWritable() throws Exception {
    TrainInfoClassEntity clazz = classOf(101L, "技师班");
    TrainPeopleListEntity inClass = student(9001L, 101L, "花名册名", "452631197706194818", null, null);
    when(classMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(clazz));
    when(peopleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(inClass));
    when(orgMapper.selectBatchIds(any())).thenReturn(List.of());
    when(examWorkTypeMapper.selectList(any())).thenReturn(List.of());
    when(trainInfoListMapper.selectBatchIds(any())).thenReturn(List.of());

    MockMultipartFile file = new MockMultipartFile(
        "file", "score.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        flatSheet("452631197706194818", "陈朝永", "80", "70", "75"));

    try (MockedStatic<SecurityContext> ignored = mockCurrentUser()) {
      ExamScoreImportPreviewVO preview = service.preview(List.of(101L), file);
      assertThat(preview.items().get(0).status()).isEqualTo("MATCHED_NAME_WARN");
      assertThat(preview.nameWarn()).isEqualTo(1);
    }
  }

  private static TrainInfoClassEntity classOf(Long id, String title) {
    TrainInfoClassEntity c = new TrainInfoClassEntity();
    c.setId(id);
    c.setTitle(title);
    c.setTrainInfoId(1L);
    c.setCompanyId(8L);
    c.setDeleted(0);
    return c;
  }

  private static TrainPeopleListEntity student(
      Long id, Long classId, String name, String idCard, BigDecimal exam, BigDecimal real) {
    TrainPeopleListEntity p = new TrainPeopleListEntity();
    p.setId(id);
    p.setTrainClassId(classId);
    p.setType("student");
    p.setRealname(name);
    p.setIdCard(idCard);
    p.setExamScore(exam);
    p.setRealScore(real);
    p.setDeleted(0);
    return p;
  }

  private static byte[] flatSheet(String idCard, String name, String exam, String theory, String skill)
      throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var sheet = wb.createSheet();
      var h = sheet.createRow(0);
      h.createCell(0).setCellValue("身份证号");
      h.createCell(1).setCellValue("姓名");
      h.createCell(2).setCellValue("综合");
      h.createCell(3).setCellValue("理论");
      h.createCell(4).setCellValue("技能");
      var r = sheet.createRow(1);
      r.createCell(0).setCellValue(idCard);
      r.createCell(1).setCellValue(name);
      r.createCell(2).setCellValue(Double.parseDouble(exam));
      r.createCell(3).setCellValue(Double.parseDouble(theory));
      r.createCell(4).setCellValue(Double.parseDouble(skill));
      wb.write(out);
      return out.toByteArray();
    }
  }

  private MockedStatic<SecurityContext> mockCurrentUser() {
    PlatformUserPrincipal principal = mock(PlatformUserPrincipal.class);
    lenient().when(principal.getUserId()).thenReturn(1L);
    lenient().when(principal.getTenantId()).thenReturn(1L);
    lenient().when(principal.getUsername()).thenReturn("admin");
    MockedStatic<SecurityContext> mocked = mockStatic(SecurityContext.class);
    mocked.when(SecurityContext::currentUser).thenReturn(principal);
    return mocked;
  }
}
