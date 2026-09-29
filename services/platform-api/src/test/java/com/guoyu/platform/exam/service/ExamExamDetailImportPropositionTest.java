package com.guoyu.platform.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.service.ExamExamDetailApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.UserMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 试题导入（命题模板格式）解析回归。
 * 命题模板 = 系统导出同款：数字题型码 + 选项"内容、分值"后缀 + 下一行"是"标记正确项。
 * 与平台标准对齐的断言：题型英文码、answer 存 JSON 选项数组（判分消费端口径）。
 */
@ExtendWith(MockitoExtension.class)
class ExamExamDetailImportPropositionTest {

  @Mock
  private ExamExamDetailMapper mapper;
  @Mock
  private ExamKnowledgeStructureMapper knowledgeMapper;
  @Mock
  private ExamCourseMapper courseMapper;
  @Mock
  private ExamWorkTypeMapper workTypeMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private ExamKnowledgeStructureApplicationService knowledgeService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private ExamExamDetailApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ExamExamDetailApplicationService(mapper, knowledgeMapper, courseMapper,
        workTypeMapper, userMapper, knowledgeService, objectMapper);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new PlatformUserPrincipal(1L, 10159L, "tester", "测试用户", "admin", List.of(), List.of(), null),
        "token", List.of()));
    lenient().doReturn(List.of(901, 902)).when(knowledgeService).getLevel3IdsByCourse(205);
    // 默认：知识点编码在科目下查不到 → 回退随机 L3
    lenient().doReturn(List.of()).when(knowledgeMapper).selectList(any());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** 列：知识点编码 | 级别 | 题型 | 内容 | 难度 | 分值 | 答案(G起)。数值列写数值类型，贴近真实文件 */
  private MockMultipartFile propositionFile(List<String[]> rows) throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Sheet1");
      Row header = sheet.createRow(0);
      String[] heads = {"知识点编码", "级别（5：初级、4：中级、3：高级、2：技师、1：高级技师",
          "题型", "内容", "难度", "分值", "答案"};
      for (int c = 0; c < heads.length; c++) {
        header.createCell(c).setCellValue(heads[c]);
      }
      int r = 1;
      for (String[] row : rows) {
        Row xr = sheet.createRow(r++);
        for (int c = 0; c < row.length; c++) {
          String v = row[c];
          if (v == null) continue;
          Cell cell = xr.createCell(c);
          if (c == 1 || c == 2 || c == 5) {
            cell.setCellValue(Double.parseDouble(v));
          } else {
            cell.setCellValue(v);
          }
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return new MockMultipartFile("file", "proposition.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
  }

  private MockMultipartFile flatFile(String[]... rows) throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Sheet1");
      Row header = sheet.createRow(0);
      String[] heads = {"题型", "题干", "A", "B", "参考答案", "难度"};
      for (int c = 0; c < heads.length; c++) {
        header.createCell(c).setCellValue(heads[c]);
      }
      int r = 1;
      for (String[] row : rows) {
        Row xr = sheet.createRow(r++);
        for (int c = 0; c < row.length; c++) {
          if (row[c] != null) xr.createCell(c).setCellValue(row[c]);
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return new MockMultipartFile("file", "flat.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
  }

  @Test
  void propositionMixedTypesImportPlatformStandardRows() throws Exception {
    MockMultipartFile file = propositionFile(List.of(
        // 判断题：G/H 选项带分值，下一行 G 标"是"
        new String[] {"A-B-A-001", "3", "1", "题干甲。（　　）", "易", "0.5", "正确、1", "错误、0"},
        new String[] {null, null, null, null, null, null, "是"},
        // 单选题：正确项在第 3 个选项（I 列）
        new String[] {"A-B-A-002", "3", "2", "题干乙（　　）。", "中", "1", "A1、0", "A2、0", "A3、1"},
        new String[] {null, null, null, null, null, null, null, null, "是"},
        // 多选题：G/H/J 三项正确
        new String[] {"A-B-A-003", "3", "7", "题干丙（　　）。", "难", "2",
            "M1、1", "M2、1", "M3、0", "M4、1"},
        new String[] {null, null, null, null, null, null, "是", "是", null, "是"},
        // 填空题：答案文本直接在 G 列
        new String[] {"A-B-A-004", "3", "3", "题干丁____。", "易", "1", "填空答案"}));

    doReturn(1).when(mapper).insert(any(ExamExamDetailEntity.class));
    int count = service.importExamDetails(file, 205, 0);
    assertEquals(4, count);

    ArgumentCaptor<ExamExamDetailEntity> captor = ArgumentCaptor.forClass(ExamExamDetailEntity.class);
    org.mockito.Mockito.verify(mapper, times(4)).insert(captor.capture());
    List<ExamExamDetailEntity> saved = captor.getAllValues();

    ExamExamDetailEntity judge = saved.get(0);
    assertEquals("judge", judge.getType());
    assertEquals("题干甲。（　　）", judge.getContent());
    assertEquals(1, judge.getDifficulty());
    assertEquals(0, judge.getScore().compareTo(new BigDecimal("0.5")));
    assertEquals(0, judge.getFinalCheckStatus());
    JsonNode judgeOpts = objectMapper.readTree(judge.getAnswer());
    assertEquals(2, judgeOpts.size());
    assertEquals("正确", judgeOpts.get(0).get("content").asText());
    assertTrue(judgeOpts.get(0).get("isCorrect").asBoolean());
    assertEquals(1, judgeOpts.get(0).get("score").asInt());
    assertEquals("错误", judgeOpts.get(1).get("content").asText());
    assertTrue(!judgeOpts.get(1).get("isCorrect").asBoolean());

    ExamExamDetailEntity single = saved.get(1);
    assertEquals("single", single.getType());
    JsonNode singleOpts = objectMapper.readTree(single.getAnswer());
    assertTrue(!singleOpts.get(0).get("isCorrect").asBoolean());
    assertEquals("A3", singleOpts.get(2).get("content").asText());
    assertTrue(singleOpts.get(2).get("isCorrect").asBoolean());

    ExamExamDetailEntity multi = saved.get(2);
    assertEquals("multi", multi.getType());
    JsonNode multiOpts = objectMapper.readTree(multi.getAnswer());
    assertTrue(multiOpts.get(0).get("isCorrect").asBoolean());
    assertTrue(multiOpts.get(1).get("isCorrect").asBoolean());
    assertTrue(!multiOpts.get(2).get("isCorrect").asBoolean());
    assertTrue(multiOpts.get(3).get("isCorrect").asBoolean());

    ExamExamDetailEntity fill = saved.get(3);
    assertEquals("fill", fill.getType());
    assertEquals("填空答案", fill.getAnswer());

    // 知识点编码在科目下无匹配 → 回退随机 L3
    for (ExamExamDetailEntity entity : saved) {
      assertTrue(entity.getKnowledgeId() == null
          || List.of(901, 902).contains(entity.getKnowledgeId()));
    }
  }

  @Test
  void knowledgeSeriesNoOverridesRandomAssignment() throws Exception {
    MockMultipartFile file = propositionFile(List.of(
        new String[] {"A-B-A-009", "3", "1", "题干戊。（　　）", "易", "0.5", "正确、1", "错误、0"},
        new String[] {null, null, null, null, null, null, "是"}));

    ExamKnowledgeStructureEntity node = new ExamKnowledgeStructureEntity();
    node.setId(777);
    doReturn(List.of(node)).when(knowledgeMapper).selectList(any());
    doReturn(1).when(mapper).insert(any(ExamExamDetailEntity.class));

    service.importExamDetails(file, 205, 0);

    ArgumentCaptor<ExamExamDetailEntity> captor = ArgumentCaptor.forClass(ExamExamDetailEntity.class);
    org.mockito.Mockito.verify(mapper).insert(captor.capture());
    assertEquals(777, captor.getValue().getKnowledgeId());
  }

  @Test
  void choiceQuestionWithoutMarkerRowLeavesAnswerNull() throws Exception {
    // 判断题是最后一行，没有标记行 → answer 留空待审核补录，不产生全错判分数据
    MockMultipartFile file = propositionFile(List.<String[]>of(
        new String[] {"A-B-A-001", "3", "1", "题干己。（　　）", "易", "0.5", "正确、1", "错误、0"}));

    doReturn(1).when(mapper).insert(any(ExamExamDetailEntity.class));
    int count = service.importExamDetails(file, 205, 0);
    assertEquals(1, count);

    ArgumentCaptor<ExamExamDetailEntity> captor = ArgumentCaptor.forClass(ExamExamDetailEntity.class);
    org.mockito.Mockito.verify(mapper).insert(captor.capture());
    assertNull(captor.getValue().getAnswer());
  }

  @Test
  void legacyFlatFormatStillImports() throws Exception {
    MockMultipartFile file = flatFile(new String[][] {
        new String[] {"单选题", "题干庚（　　）。", "选项1", "选项2", "A", "简单"}});

    doReturn(1).when(mapper).insert(any(ExamExamDetailEntity.class));
    int count = service.importExamDetails(file, 205, 0);
    assertEquals(1, count);

    ArgumentCaptor<ExamExamDetailEntity> captor = ArgumentCaptor.forClass(ExamExamDetailEntity.class);
    org.mockito.Mockito.verify(mapper).insert(captor.capture());
    ExamExamDetailEntity entity = captor.getValue();
    assertEquals("danxuan", entity.getType());
    assertTrue(entity.getContent().contains("题干庚（　　）。"));
    assertTrue(entity.getContent().contains("A. 选项1"));
    assertTrue(entity.getContent().contains("B. 选项2"));
    assertEquals("A", entity.getAnswer());
    assertEquals(1, entity.getDifficulty());
    assertNull(entity.getScore());
  }

  @Test
  void unknownNumericTypeSkippedButValidRowsImported() throws Exception {
    MockMultipartFile file = propositionFile(List.of(
        new String[] {"A-B-A-001", "3", "11", "题型码越界。（　　）", "易", "0.5", "正确、1"},
        new String[] {"A-B-A-001", "3", "1", "题干辛。（　　）", "易", "0.5", "正确、1", "错误、0"},
        new String[] {null, null, null, null, null, null, "是"}));

    doReturn(1).when(mapper).insert(any(ExamExamDetailEntity.class));
    int count = service.importExamDetails(file, 205, 0);
    assertEquals(1, count);

    ArgumentCaptor<ExamExamDetailEntity> captor = ArgumentCaptor.forClass(ExamExamDetailEntity.class);
    org.mockito.Mockito.verify(mapper).insert(captor.capture());
    assertEquals("judge", captor.getValue().getType());
  }
}
