package com.guoyu.platform.textbook;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 职业标准驱动教材生成 一期（§4.2）后端集成测试。
 *
 * <p>跑真实 MySQL 测试库 gjjddb2026（courseware-e2e profile）+ 真实 Flyway 迁移：
 * textbook_project/textbook_outline 两表由 V20260821_001 迁移创建。每个用例前后清理
 * textbook 两表 + 本类产生的 sys_file/审计日志，互不污染。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TextbookProjectApiIntegrationTest {

  private static final long TENANT_ID = 10159L;
  private static final long OTHER_TENANT_ID = 999L;
  private static final long USER_ID = 990_871_001L;
  private static final String WORK_TYPE = "铝电解工";
  private static final String DIRECTION_CAO = "铝电解槽操作工";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private byte[] lvDocx;

  @BeforeEach
  void setUp() throws IOException {
    cleanUp();
    try (InputStream in = getClass().getResourceAsStream("/standard/铝电解工.docx")) {
      assertNotNull(in, "测试资源缺失：standard/铝电解工.docx");
      lvDocx = in.readAllBytes();
    }
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  private void cleanUp() {
    // 远程共享测试库偶发连接重置/锁等待（Communications link failure / Lock wait timeout）：
    // 清理语句幂等，重试 6 次、3s 退避，容忍病态窗口的锁释放与重连
    int attempts = 0;
    while (true) {
      try {
        jdbcTemplate.update(
            "DELETE FROM sys_audit_log WHERE target_type IN ('textbook_project','textbook_outline')");
        jdbcTemplate.update("DELETE FROM textbook_outline");
        jdbcTemplate.update("DELETE FROM textbook_project");
        jdbcTemplate.update("DELETE FROM sys_file WHERE created_by = ?", USER_ID);
        return;
      } catch (org.springframework.dao.DataAccessException e) {
        if (++attempts >= 6) {
          throw e;
        }
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw e;
        }
      }
    }
  }

  // ============================================================
  // ① 全链路：parse-preview → 创建（槽操作工 × 五级）→ 骨架与配比断言
  // ============================================================

  @Test
  @DisplayName("01-全链路：预览回填 + 创建 outlined + 章集合 + 100000 守恒 + 点级条数")
  void fullChainLvCaoOperatorLevel5() throws Exception {
    // parse-preview：同步解析不落库
    MvcResult preview = mockMvc.perform(multipart("/textbook/projects/parse-preview")
            .file(docxFile(lvDocx))
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.workTypeTitle").value(WORK_TYPE))
        .andExpect(jsonPath("$.data.seriesNo").value("6-17-06-02"))
        .andExpect(jsonPath("$.data.directions").isArray())
        .andReturn();
    JsonNode previewData = data(preview);
    assertTrue(texts(previewData.get("directions")).contains(DIRECTION_CAO));
    assertEquals(40, previewData.get("trainingHoursByLevel").get("5").asInt());
    assertEquals(List.of(1, 2, 12), ints(previewData.get("guessedFunctionNos")));
    // 职业功能真实名称透出（供向导勾选项显示，替代"职业功能 N"占位标签）
    assertEquals("工作准备", previewData.get("functionNamesByLevel").get("5").get("1").asText());
    assertEquals("铝电解槽操作", previewData.get("functionNamesByLevel").get("5").get("2").asText());
    assertEquals("设备管理", previewData.get("functionNamesByLevel").get("5").get("12").asText());

    // 创建（multipart 直传）：同步完成 解析→方向过滤→骨架→配比
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);

    // 详情：status/outlined + total_words=100000（40 学时 × 2500）
    mockMvc.perform(get("/textbook/projects/{id}", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("outlined"))
        .andExpect(jsonPath("$.data.totalWords").value(100000))
        .andExpect(jsonPath("$.data.trainingHours").value(40))
        .andExpect(jsonPath("$.data.wordsPerHour").value(2500))
        .andExpect(jsonPath("$.data.summary.chapterSum").value(100000))
        .andExpect(jsonPath("$.data.summary.balanced").value(true))
        .andExpect(jsonPath("$.data.chapterCount").value(5));

    // 大纲：章集合 = 职业道德/基础知识/工作准备/铝电解槽操作/设备管理(职业功能12)
    List<JsonNode> nodes = outlineNodes(projectId);
    Set<String> chapters = new HashSet<>();
    long chapterSum = 0;
    int theoryPoints = 0;
    int skillPoints = 0;
    for (JsonNode node : nodes) {
      int level = node.get("nodeLevel").asInt();
      if (level == 2) {
        chapters.add(node.get("title").asText());
        chapterSum += node.get("targetWords").asLong();
      } else if (level == 4) {
        if ("theory".equals(node.get("dimension").asText())) {
          theoryPoints++;
        } else {
          skillPoints++;
        }
      }
    }
    assertEquals(Set.of("职业道德", "基础知识", "工作准备", "铝电解槽操作", "设备管理"), chapters,
        "五级槽操作工章集合不符：" + chapters);
    assertEquals(100000, chapterSum, "各章之和必须严格 = 总字数");
    assertTrue(theoryPoints > 0 && skillPoints > 0, "点级 theory/skill 条数应 > 0");
  }

  // ============================================================
  // ② 权重边界：三级/高级工无"工作准备"章（权重 — 不生章）
  // ============================================================

  @Test
  @DisplayName("02-权重边界：三级槽操作工无工作准备章")
  void level3SkipsNullWeightChapter() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 3, "1,11,12", null, null);
    Set<String> chapters = chapterTitles(outlineNodes(projectId));
    assertFalse(chapters.contains("工作准备"), "三级工作准备权重为 —，不生章：" + chapters);
    assertTrue(chapters.contains("铝电解槽操作"), "三级 1 号功能重编号为铝电解槽操作：" + chapters);
  }

  // ============================================================
  // ③ 方向映射人工登记：人工 functionNos 优先于解析映射
  // ============================================================

  @Test
  @DisplayName("03-人工登记：functionNos=1,3,12 覆盖解析值 1,2,12，骨架按人工映射生成")
  void manualFunctionNosOverrideGuess() throws Exception {
    // 解析猜测为 [1,2,12]（槽操作工），人工登记吸出工映射 [1,3,12]
    long projectId = createProject(DIRECTION_CAO, 5, "1,3,12", null, null);
    Set<String> chapters = chapterTitles(outlineNodes(projectId));
    assertTrue(chapters.contains("铝吸出作业"), "应按人工映射生成铝吸出作业章：" + chapters);
    assertFalse(chapters.contains("铝电解槽操作"), "不应再含解析方向的铝电解槽操作章：" + chapters);
    assertEquals(Set.of("职业道德", "基础知识", "工作准备", "铝吸出作业", "设备管理"), chapters);
  }

  // ============================================================
  // ④ 缺权重表 / 缺学时 → 400 + warnings，不落半成品
  // ============================================================

  @Test
  @DisplayName("04a-缺理论权重表 → 400 + warnings，不落半成品")
  void missingWeightTableRejected() throws Exception {
    byte[] broken = buildDocx(false, true);
    mockMvc.perform(multipart("/textbook/projects")
            .file(docxFile(broken))
            .param("direction", "默认")
            .param("levelCode", "5")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("理论知识权重表")))
        .andExpect(jsonPath("$.data").isArray());
    assertEquals(0, countProjects("合成测试工种"), "解析失败不允许落半成品工程");
  }

  @Test
  @DisplayName("04b-缺培训学时（未手工填写）→ 400，不落半成品")
  void missingTrainingHoursRejected() throws Exception {
    byte[] noHours = buildDocx(true, false);
    mockMvc.perform(multipart("/textbook/projects")
            .file(docxFile(noHours))
            .param("direction", "默认")
            .param("levelCode", "5")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("培训参考学时")));
    assertEquals(0, countProjects("合成测试工种"), "学时缺失不允许落半成品工程");
  }

  // ============================================================
  // ⑤ 重复创建同分册 → 409
  // ============================================================

  @Test
  @DisplayName("05-同分册重复创建 → 409")
  void duplicateProjectConflict() throws Exception {
    createProject(DIRECTION_CAO, 5, "1,2,12", null, null);
    mockMvc.perform(multipart("/textbook/projects")
            .file(docxFile(lvDocx))
            .param("direction", DIRECTION_CAO)
            .param("levelCode", "5")
            .param("functionNos", "1,2,12")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("已存在")));
  }

  // ============================================================
  // ⑥ 越权：无 token → 401；无 manage 权限 → 403
  // ============================================================

  @Test
  @DisplayName("06a-匿名访问 manage 接口 → 401")
  void anonymousUnauthorized() throws Exception {
    mockMvc.perform(get("/textbook/projects")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("06b-已登录但无 manage 权限 → 403（@PreAuthorize 生效）")
  void withoutManagePermissionForbidden() throws Exception {
    mockMvc.perform(multipart("/textbook/projects")
            .file(docxFile(lvDocx))
            .param("direction", DIRECTION_CAO)
            .param("levelCode", "5")
            .param("functionNos", "1,2,12")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:view"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("06c-view 权限可列表/导出，export 权限缺失调导出 → 403")
  void exportRequiresExportPermission() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);
    mockMvc.perform(get("/textbook/projects/{id}/export", projectId)
            .param("format", "md")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:view"))))
        .andExpect(status().isForbidden());
  }

  // ============================================================
  // ⑦ 多租户：他租户查工程/大纲 → 404 / 空列表
  // ============================================================

  @Test
  @DisplayName("07-多租户：他租户查详情/大纲 404、列表空")
  void otherTenantSeesNothing() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);
    UserDetails other = principal(OTHER_TENANT_ID, "textbook:project:view", "textbook:project:manage",
        "textbook:project:export");
    mockMvc.perform(get("/textbook/projects/{id}", projectId)
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT_ID))
            .with(user(other)))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/projects/{id}/outline", projectId)
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT_ID))
            .with(user(other)))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/projects")
            .param("page", "1").param("pageSize", "20")
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT_ID))
            .with(user(other)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.items").isArray());
  }

  // ============================================================
  // ⑧ 导出 md/xlsx：200 + Content-Disposition + xlsx 可解析含"配比依据"列
  // ============================================================

  @Test
  @DisplayName("08a-导出 Markdown：200 + 文件名 + 配比依据表")
  void exportMarkdown() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);
    MvcResult result = mockMvc.perform(get("/textbook/projects/{id}/export", projectId)
            .param("format", "md")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:export"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("attachment")))
        .andExpect(header().string("Content-Disposition", containsString(".md")))
        .andReturn();
    String md = result.getResponse().getContentAsString();
    assertTrue(md.contains("# 铝电解工-铝电解槽操作工（五级/初级工）"), "Markdown 标题不符");
    assertTrue(md.contains("## 铝电解槽操作"), "应含章节");
    assertTrue(md.contains("- [理论]"), "应含理论点");
    assertTrue(md.contains("- [技能]"), "应含技能点");
    assertTrue(md.contains("配比依据"), "文末应附配比依据表");
  }

  @Test
  @DisplayName("08b-导出 Excel：200 + 文件名 + POI 读回含配比依据列")
  void exportExcel() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);
    MvcResult result = mockMvc.perform(get("/textbook/projects/{id}/export", projectId)
            .param("format", "xlsx")
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:export"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
        .andReturn();
    byte[] bytes = result.getResponse().getContentAsByteArray();
    assertTrue(bytes.length > 1000, "xlsx 内容不应为空");
    try (org.apache.poi.ss.usermodel.Workbook wb =
             org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
      org.apache.poi.ss.usermodel.Row header = sheet.getRow(0);
      List<String> headers = new ArrayList<>();
      for (org.apache.poi.ss.usermodel.Cell cell : header) {
        headers.add(cell.getStringCellValue());
      }
      assertEquals(List.of("层级", "编号", "标题", "维度", "权重%", "目标字数", "配比依据"), headers);
      boolean hasBasis = false;
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
        if (row == null || row.getCell(6) == null) {
          continue;
        }
        String basis = row.getCell(6).getStringCellValue();
        if (basis.contains("理论知识权重表") || basis.contains("条占比")) {
          hasBasis = true;
          break;
        }
      }
      assertTrue(hasBasis, "xlsx 应含配比依据列内容");
    }
  }

  // ============================================================
  // ⑨ 合计校验与重建：改字数 balanced 变化；rebuild 保留人工节点且合计守恒
  // ============================================================

  @Test
  @DisplayName("09-改字数 → balanced=false；rebuild 保留人工字数且合计守恒")
  void editWordsThenRebuildConserves() throws Exception {
    long projectId = createProject(DIRECTION_CAO, 5, "1,2,12", null, null);

    // 找一个章，把它的字数改小 1000 → 合计不平衡
    List<JsonNode> nodes = outlineNodes(projectId);
    JsonNode chapter = null;
    for (JsonNode node : nodes) {
      if (node.get("nodeLevel").asInt() == 2) {
        chapter = node;
        break;
      }
    }
    assertNotNull(chapter);
    long chapterId = chapter.get("id").asLong();
    int originalWords = chapter.get("targetWords").asInt();
    String body = "{\"targetWords\": " + (originalWords - 1000) + "}";
    MvcResult edit = mockMvc.perform(put("/textbook/outline/nodes/{id}", chapterId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.balanced").value(false))
        .andReturn();
    assertEquals(99000, data(edit).get("chapterSum").asLong());
    JsonNode edited = outlineNodes(projectId).stream()
        .filter(n -> n.get("id").asLong() == chapterId).findFirst().orElseThrow();
    assertEquals(1, edited.get("wordsManual").asInt(), "改字数应置 words_manual=1");

    // rebuild：人工调整过的章字数保留，其余章重新分盘，合计恢复守恒
    mockMvc.perform(post("/textbook/projects/{id}/rebuild", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:manage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.balanced").value(true))
        .andExpect(jsonPath("$.data.chapterSum").value(100000));

    List<JsonNode> rebuilt = outlineNodes(projectId);
    long chapterSum = 0;
    JsonNode kept = null;
    for (JsonNode node : rebuilt) {
      if (node.get("nodeLevel").asInt() == 2) {
        chapterSum += node.get("targetWords").asLong();
        if (node.get("id").asLong() == chapterId) {
          kept = node;
        }
      }
    }
    assertEquals(100000, chapterSum, "rebuild 后各章之和必须仍 = 总字数");
    assertNotNull(kept, "人工调整章应仍存在");
    assertEquals(originalWords - 1000, kept.get("targetWords").asInt(), "人工字数应保留");
    assertEquals(1, kept.get("wordsManual").asInt());
  }

  // ============================================================
  // 工具方法
  // ============================================================

  /** multipart 创建并返回工程 id（断言 200 + outlined） */
  private long createProject(String direction, int levelCode, String functionNos,
      Integer trainingHours, Integer wordsPerHour) throws Exception {
    var request = multipart("/textbook/projects")
        .file(docxFile(lvDocx))
        .param("direction", direction)
        .param("levelCode", String.valueOf(levelCode))
        .header("X-Tenant-Id", String.valueOf(TENANT_ID))
        .with(user(principal(TENANT_ID, "textbook:project:manage")));
    if (functionNos != null) {
      request = request.param("functionNos", functionNos);
    }
    if (trainingHours != null) {
      request = request.param("trainingHours", String.valueOf(trainingHours));
    }
    if (wordsPerHour != null) {
      request = request.param("wordsPerHour", String.valueOf(wordsPerHour));
    }
    MvcResult result = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("outlined"))
        .andReturn();
    return data(result).get("id").asLong();
  }

  private List<JsonNode> outlineNodes(long projectId) throws Exception {
    MvcResult result = mockMvc.perform(get("/textbook/projects/{id}/outline", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT_ID))
            .with(user(principal(TENANT_ID, "textbook:project:view"))))
        .andExpect(status().isOk())
        .andReturn();
    List<JsonNode> nodes = new ArrayList<>();
    data(result).forEach(nodes::add);
    return nodes;
  }

  private static Set<String> chapterTitles(List<JsonNode> nodes) {
    Set<String> titles = new HashSet<>();
    for (JsonNode node : nodes) {
      if (node.get("nodeLevel").asInt() == 2) {
        titles.add(node.get("title").asText());
      }
    }
    return titles;
  }

  private int countProjects(String workTypeTitle) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM textbook_project WHERE work_type_title = ? AND deleted = 0",
        Integer.class, workTypeTitle);
    return count == null ? 0 : count;
  }

  private JsonNode data(MvcResult result) throws IOException {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
  }

  private static List<String> texts(JsonNode array) {
    List<String> out = new ArrayList<>();
    array.forEach(n -> out.add(n.asText()));
    return out;
  }

  private static List<Integer> ints(JsonNode array) {
    List<Integer> out = new ArrayList<>();
    array.forEach(n -> out.add(n.asInt()));
    return out;
  }

  private static MockMultipartFile docxFile(byte[] content) {
    return new MockMultipartFile("file", "铝电解工.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", content);
  }

  private static UserDetails principal(long tenantId, String... permissions) {
    return new PlatformUserPrincipal(
        USER_ID, tenantId, "textbook-it", "教材集成测试", "admin", List.of(),
        List.of(permissions), null);
  }

  /**
   * 用 POI 合成最小职业技能标准 docx：可开关理论权重表与 1.8 培训学时段，
   * 用于 400 分支（缺权重表/缺学时）构造。含 3.1 五级一个职业功能（1.工作准备）。
   */
  private static byte[] buildDocx(boolean withWeightTable, boolean withTrainingHours)
      throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      // 一级节标题必须存在：解析器按 depth-1 标题登记 mainSection，否则 1.x 小节标题被忽略
      doc.createParagraph().createRun().setText("1.职业概况");
      doc.createParagraph().createRun().setText("1.1 职业名称");
      doc.createParagraph().createRun().setText("合成测试工种");
      if (withTrainingHours) {
        doc.createParagraph().createRun().setText("1.8 培训参考学时");
        doc.createParagraph().createRun().setText("五级/初级工 40 标准学时");
      }
      doc.createParagraph().createRun().setText("3.工作要求");
      doc.createParagraph().createRun().setText("3.1 五级/初级工");
      XWPFTable work = doc.createTable(2, 4);
      setRow(work.getRow(0), "职业功能", "工作内容", "技能要求", "相关知识要求");
      setRow(work.getRow(1), "1.工作准备", "1.1 交接班",
          "1.1.1 能确认现场安全生产状况", "1.1.1 安全操作规程");
      if (withWeightTable) {
        doc.createParagraph().createRun().setText("4.权重表");
        doc.createParagraph().createRun().setText("4.1 理论知识权重表");
        XWPFTable weight = doc.createTable(4, 4);
        setRow(weight.getRow(0), "技能等级项目", "项目", "五级/初级工(%)", "四级/中级工(%)");
        setRow(weight.getRow(1), "基本要求", "职业道德", "5", "5");
        setRow(weight.getRow(2), "基本要求", "基础知识", "30", "25");
        setRow(weight.getRow(3), "相关知识要求", "工作准备", "10", "15");
      }
      doc.write(out);
      return out.toByteArray();
    }
  }

  private static void setRow(XWPFTableRow row, String... texts) {
    for (int i = 0; i < texts.length; i++) {
      row.getCell(i).setText(texts[i]);
    }
  }
}
