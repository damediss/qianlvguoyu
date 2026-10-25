package com.guoyu.platform.textbook;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 职业标准驱动教材生成 二期编写协同（§12.2）后端集成测试。
 *
 * <p>真实 MySQL 测试库 gjjddb2026（courseware-e2e profile）+ 真实 Flyway 迁移
 * （V20261024_001 五表 / V20261024_002 种子由 Flyway 自动应用）。
 * 覆盖：全链路、三层授权/越权/跨租户、空正文契约、两标签版本冲突、改派竞态（真实栅栏并发）、
 * 快照/恢复、图片与受保护文件区绕过、结构冻结、完成条件、统计卡全量、导出、种子三步链。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("courseware-e2e")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TextbookCollaborationApiIntegrationTest {

  private static final long TENANT = 10159L;
  private static final long OTHER_TENANT = 999L;
  private static final long ADMIN = 990_900L;
  private static final long WRITER1 = 990_911L;
  private static final long WRITER2 = 990_912L;
  private static final long REVIEWER = 990_921L;
  private static final long REVIEWER2 = 990_922L;
  private static final long VIEWER_ONLY = 990_931L;
  private static final long NOBODY = 990_932L;

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
    insertTestUser(WRITER1, "tb-writer1", "主笔一", "TEXTBOOK_WRITER", "TEXTBOOK_REVIEWER");
    insertTestUser(WRITER2, "tb-writer2", "主笔二", "TEXTBOOK_WRITER");
    insertTestUser(REVIEWER, "tb-reviewer1", "审定一", "TEXTBOOK_REVIEWER");
    insertTestUser(REVIEWER2, "tb-reviewer2", "审定二", "TEXTBOOK_REVIEWER");
    insertTestUser(VIEWER_ONLY, "tb-viewer", "只读者");
    insertTestUser(NOBODY, "tb-nobody", "无权限者");
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  private void insertTestUser(long userId, String username, String displayName,
      String... roleCodes) {
    jdbcTemplate.update("""
        INSERT INTO sys_user (id, tenant_id, username, password_hash, display_name, user_type,
            status, created_at, updated_at, deleted)
        VALUES (?, ?, ?, '$2a$10$invalidhashinvalidhashinvalidhash', ?, 'staff', 'enabled',
            NOW(), NOW(), 0)
        """, userId, TENANT, username, displayName);
    for (String roleCode : roleCodes) {
      Long roleId = jdbcTemplate.queryForObject(
          "SELECT id FROM sys_role WHERE tenant_id = ? AND code = ? AND deleted = 0 LIMIT 1",
          Long.class, TENANT, roleCode);
      assertNotNull(roleId, "种子角色缺失：" + roleCode);
      jdbcTemplate.update("""
          INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, created_at, updated_at, deleted)
          VALUES (?, ?, ?, ?, NOW(), NOW(), 0)
          """, userId * 1000 + roleId % 1000, TENANT, userId, roleId);
    }
  }

  private void cleanUp() {
    int attempts = 0;
    while (true) {
      try {
        jdbcTemplate.update(
            "DELETE FROM sys_audit_log WHERE target_type LIKE 'textbook_%'");
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN "
            + "(990911,990912,990921,990922,990931,990932)");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id IN "
            + "(990911,990912,990921,990922,990931,990932)");
        jdbcTemplate.update("DELETE FROM textbook_section_asset");
        jdbcTemplate.update("DELETE FROM textbook_section_review_record");
        jdbcTemplate.update("DELETE FROM textbook_section_content_version");
        jdbcTemplate.update("DELETE FROM textbook_section_content");
        jdbcTemplate.update("DELETE FROM textbook_chapter_assignment");
        jdbcTemplate.update("DELETE FROM textbook_outline");
        jdbcTemplate.update("DELETE FROM textbook_project");
        jdbcTemplate.update("DELETE FROM sys_file WHERE object_key LIKE 'protected/%'");
        jdbcTemplate.update("DELETE FROM sys_file WHERE created_by = ?", ADMIN);
        return;
      } catch (org.springframework.dao.DataAccessException e) {
        if (++attempts >= 6) {
          throw e;
        }
        sleep(3000);
      }
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // ============================================================
  // 主体方法
  // ============================================================

  /** 创建工程（outlined）并把大纲评审为 reviewed，返回 projectId。 */
  private long reviewedProject() throws Exception {
    return reviewedProject(5, "1,2,12");
  }

  /** 同一用例内需要第二个工程时用不同等级/功能映射，避开「同分册已存在」查重。 */
  private long reviewedProject(int levelCode, String functionNos) throws Exception {
    MvcResult created = mockMvc.perform(multipart("/textbook/projects")
            .file(docxFile())
            .param("direction", "铝电解槽操作工")
            .param("levelCode", String.valueOf(levelCode))
            .param("functionNos", functionNos)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("outlined"))
        .andReturn();
    long projectId = data(created).get("id").asLong();
    mockMvc.perform(put("/textbook/projects/{id}", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"reviewed\"}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("reviewed"));
    return projectId;
  }

  private MockMultipartFile docxFile() {
    return new MockMultipartFile("file", "铝电解工.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", lvDocx);
  }

  private List<JsonNode> chapters(long projectId) throws Exception {
    List<JsonNode> chapters = new ArrayList<>();
    for (JsonNode node : outline(projectId)) {
      if (node.get("nodeLevel").asInt() == 2) {
        chapters.add(node);
      }
    }
    return chapters;
  }

  private List<JsonNode> sections(long projectId, long chapterId) throws Exception {
    List<JsonNode> sections = new ArrayList<>();
    for (JsonNode node : outline(projectId)) {
      if (node.get("nodeLevel").asInt() == 3 && node.get("parentId").asLong() == chapterId) {
        sections.add(node);
      }
    }
    sections.sort((a, b) -> Integer.compare(a.get("sortNo").asInt(), b.get("sortNo").asInt()));
    return sections;
  }

  private List<JsonNode> outline(long projectId) throws Exception {
    MvcResult result = mockMvc.perform(get("/textbook/projects/{id}/outline", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andReturn();
    List<JsonNode> nodes = new ArrayList<>();
    data(result).forEach(nodes::add);
    return nodes;
  }

  private JsonNode chapterByTitle(long projectId, String title) throws Exception {
    for (JsonNode chapter : chapters(projectId)) {
      if (title.equals(chapter.get("title").asText())) {
        return chapter;
      }
    }
    throw new IllegalStateException("章不存在：" + title);
  }

  private String assignBody(long chapterId, long writerId, long reviewerId,
      Integer assignmentVersion) {
    ObjectNode item = objectMapper.createObjectNode();
    item.put("chapterId", String.valueOf(chapterId));
    item.put("writerId", String.valueOf(writerId));
    item.put("reviewerId", String.valueOf(reviewerId));
    item.put("instruction", "结合现场操作写清步骤");
    if (assignmentVersion != null) {
      item.put("assignmentVersion", assignmentVersion);
    }
    ObjectNode body = objectMapper.createObjectNode();
    body.set("items", objectMapper.valueToTree(List.of(item)));
    return body.toString();
  }

  private long assign(long projectId, long chapterId, long writerId, long reviewerId,
      Integer assignmentVersion) throws Exception {
    mockMvc.perform(put("/textbook/projects/{id}/assignments", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(assignBody(chapterId, writerId, reviewerId, assignmentVersion))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    return chapterId;
  }

  private JsonNode saveContent(long sectionId, String contentMd, int baseVersion,
      UserDetails principal) throws Exception {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("contentMd", contentMd);
    body.put("baseVersion", baseVersion);
    MvcResult result = mockMvc.perform(put("/textbook/sections/{id}/content", sectionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(principal)))
        .andReturn();
    if (result.getResponse().getStatus() != 200) {
      throw new IllegalStateException("保存失败：" + result.getResponse().getContentAsString());
    }
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
  }

  private JsonNode submitSection(long sectionId, int baseVersion, UserDetails principal)
      throws Exception {
    MvcResult result = mockMvc.perform(post("/textbook/sections/{id}/submit", sectionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"baseVersion\":" + baseVersion + "}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(principal)))
        .andReturn();
    assertEquals(200, result.getResponse().getStatus(),
        "提交失败：" + result.getResponse().getContentAsString());
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
  }

  private void reviewSection(long sectionId, String action, String comment, int baseVersion)
      throws Exception {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("action", action);
    if (comment != null) {
      body.put("comment", comment);
    }
    body.put("baseVersion", baseVersion);
    MvcResult result = mockMvc.perform(post("/textbook/sections/{id}/review", sectionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andReturn();
    assertEquals(200, result.getResponse().getStatus(),
        "审定失败：" + result.getResponse().getContentAsString());
  }

  // ============================================================
  // ① 完整链路（reviewed → 分配 2 章 → 编写/提交 → 审定 → 完成 → final docx）
  // ============================================================

  @Test
  @DisplayName("01-完整链路：分配2章→各自编写提交→通过→完成→final docx 可由 POI 重新打开")
  void fullCollaborationChain() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapterA = chapterByTitle(projectId, "铝电解槽操作");
    JsonNode chapterB = chapterByTitle(projectId, "工作准备");
    assign(projectId, chapterA.get("id").asLong(), WRITER1, REVIEWER, null);
    assign(projectId, chapterB.get("id").asLong(), WRITER2, REVIEWER, null);

    // C2：progress 与 chapters
    mockMvc.perform(get("/textbook/projects/{id}/collaboration", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.project.status").value("writing"))
        .andExpect(jsonPath("$.data.project.direction").value("铝电解槽操作工"))
        .andExpect(jsonPath("$.data.project.levelCode").value(5))
        .andExpect(jsonPath("$.data.progress.assignedChapterCount").value(2))
        .andExpect(jsonPath("$.data.completion.canComplete").value(false));

    // 工程列表二期批量字段：编写进度/结构锁一次返回（前端 §10.2）
    mockMvc.perform(get("/textbook/projects")
            .param("status", "writing")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].assignedChapterCount").value(2))
        .andExpect(jsonPath("$.data.items[0].structureLocked").value(true));

    // 章A：两节走完整 状态机
    List<JsonNode> sections = sections(projectId, chapterA.get("id").asLong());
    assertTrue(sections.size() >= 2, "章A 至少两节");
    long s1 = sections.get(0).get("id").asLong();
    long s2 = sections.get(1).get("id").asLong();

    // 空正文契约：W2 200 contentExists=false
    mockMvc.perform(get("/textbook/sections/{id}/content", s1)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.contentExists").value(false))
        .andExpect(jsonPath("$.data.version").value(0))
        .andExpect(jsonPath("$.data.contentStatus").doesNotExist());

    JsonNode saved = saveContent(s1, "## 更换阳极\n\n确认槽号后作业。", 0, writer1());
    assertTrue(saved.get("contentExists").asBoolean());
    assertEquals("draft", saved.get("contentStatus").asText());
    assertEquals(1, saved.get("version").asInt());

    JsonNode submitted = submitSection(s1, 1, writer1());
    assertEquals("submitted", submitted.get("contentStatus").asText());
    assertEquals(1, submitted.get("submitRevision").asInt());
    assertTrue(submitted.get("canWrite").asBoolean() == false, "提交后主笔只读");
    assertTrue(submitted.get("readOnly").asBoolean());

    // 打回必须带意见 → 修改 → 重交 → 通过
    ObjectNode emptyComment = objectMapper.createObjectNode();
    emptyComment.put("action", "return");
    emptyComment.put("baseVersion", 2);
    mockMvc.perform(post("/textbook/sections/{id}/review", s1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyComment.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andExpect(status().isBadRequest());

    reviewSection(s1, "return", "步骤不完整，补充扭矩要求", 2);
    JsonNode returned = getContent(s1, writer1());
    assertEquals("returned", returned.get("contentStatus").asText());
    JsonNode revised = saveContent(s1, "## 更换阳极\n\n确认槽号后作业，按 200N·m 紧固。", 3, writer1());
    assertTrue(revised.get("wordCount").asInt() > saved.get("wordCount").asInt(),
        "保存后 word_count/check_report 必须随正文一同落库，而不是停留在旧值");
    JsonNode resubmitted = submitSection(s1, 4, writer1());
    assertEquals(2, resubmitted.get("submitRevision").asInt(), "第二次提交 revision=2");
    reviewSection(s1, "approve", "通过", 5);

    // 章A其余节（含 s2）直接 approve；章B 全部节 approve
    approveAll(projectId, chapterA.get("id").asLong(), WRITER1, s1, -1);
    approveAll(projectId, chapterB.get("id").asLong(), WRITER2, -1, -1);

    // 其余未分配章补齐分配并 approve（凑齐完成条件：每章有分配且全部节通过）
    for (JsonNode chapter : chapters(projectId)) {
      long chapterId = chapter.get("id").asLong();
      if (chapterId == chapterA.get("id").asLong()
          || chapterId == chapterB.get("id").asLong()) {
        continue;
      }
      assign(projectId, chapterId, WRITER1, REVIEWER, null);
      approveAll(projectId, chapterId, WRITER1, -1, -1);
    }

    mockMvc.perform(post("/textbook/projects/{id}/complete", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());

    mockMvc.perform(get("/textbook/projects/{id}", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(jsonPath("$.data.status").value("done"));

    // final docx（必须仍在 done 时导出；C6 重开在后面验证）
    MvcResult finalDocx = mockMvc.perform(get("/textbook/projects/{id}/export-book", projectId)
            .param("mode", "final")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(exporter())))
        .andExpect(status().isOk())
        .andReturn();
    byte[] docx = finalDocx.getResponse().getContentAsByteArray();
    assertTrue(docx.length > 1000);
    try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(
        new ByteArrayInputStream(docx))) {
      assertTrue(doc.getParagraphs().size() > 5, "final docx 应有正文段落");
    }

    // done：写操作一律 409（含审定撤销，需先 C6）
    mockMvc.perform(post("/textbook/sections/{id}/review", s1)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\":\"reopen\",\"comment\":\"重开测试\",\"baseVersion\":6}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andExpect(status().isConflict());

    mockMvc.perform(post("/textbook/projects/{id}/reopen", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("writing"));
  }

  private JsonNode getContent(long sectionId, UserDetails principal) throws Exception {
    MvcResult result = mockMvc.perform(get("/textbook/sections/{id}/content", sectionId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(principal)))
        .andExpect(status().isOk())
        .andReturn();
    return data(result);
  }

  /** 把章下全部节 approve（跳过 skipSectionId）。 */
  private void approveAll(long projectId, long chapterId, long writerId, long skip1, long skip2)
      throws Exception {
    for (JsonNode section : sections(projectId, chapterId)) {
      long sectionId = section.get("id").asLong();
      if (sectionId == skip1 || sectionId == skip2) {
        continue;
      }
      UserDetails writer = writerId == WRITER1 ? writer1() : writer2();
      JsonNode current = getContent(sectionId, writer);
      int version = current.get("version").asInt();
      if (!current.get("contentExists").asBoolean()) {
        current = saveContent(sectionId, "正文内容：讲解要点，覆盖标准要求。", 0, writer);
        version = current.get("version").asInt();
      } else if (!"draft".equals(current.get("contentStatus").asText())
          && !"returned".equals(current.get("contentStatus").asText())) {
        continue;
      }
      JsonNode submitted = submitSection(sectionId, version, writer);
      reviewSection(sectionId, "approve", null, submitted.get("version").asInt());
    }
  }

  // ============================================================
  // ② ④ ⑤ 权限矩阵：越权/无token/无权限/跨租户/只读
  // ============================================================

  @Test
  @DisplayName("02-越权与权限：A不能读写B章；无token 401；无权限 403；对象越权 404；只读可读不可写；跨租户不可见")
  void accessControlMatrix() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapterA = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapterA.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapterA.get("id").asLong()).get(0).get("id").asLong();

    // 主笔B 改 URL sectionId 访问主笔A 的节 → 404（对象级无权访问统一 404）
    mockMvc.perform(get("/textbook/sections/{id}/content", section)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer2())))
        .andExpect(status().isNotFound());
    ObjectNode body = objectMapper.createObjectNode();
    body.put("contentMd", "x");
    body.put("baseVersion", 0);
    mockMvc.perform(put("/textbook/sections/{id}/content", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer2())))
        .andExpect(status().isNotFound());

    // 无 token 401
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .header("X-Tenant-Id", String.valueOf(TENANT)))
        .andExpect(status().isUnauthorized());

    // 无功能权限（NOBODY 无 task:view）→ 403
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(nobody())))
        .andExpect(status().isForbidden());

    // 审定人不能改正文（有 review:view 可读，但 content:write 无 → 403）
    mockMvc.perform(put("/textbook/sections/{id}/content", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andExpect(status().isForbidden());

    // 主笔不能审定（无 content:review → 403）
    mockMvc.perform(post("/textbook/sections/{id}/review", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\":\"approve\",\"baseVersion\":1}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isForbidden());

    // 只有 project:view 的用户可读不可写
    mockMvc.perform(get("/textbook/sections/{id}/content", section)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(viewer())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.readOnly").value(true));
    mockMvc.perform(put("/textbook/sections/{id}/content", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(viewer())))
        .andExpect(status().isForbidden());

    // 跨租户：工程/章/节/正文/快照/图片均不可见
    UserDetails otherTenant = principal(OTHER_TENANT, ADMIN, "textbook:project:view",
        "textbook:project:manage", "textbook:project:export");
    mockMvc.perform(get("/textbook/projects/{id}", projectId)
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT))
            .with(user(otherTenant)))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/chapters/{id}/workspace", chapterA.get("id").asLong())
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT))
            .with(user(otherTenant)))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .header("X-Tenant-Id", String.valueOf(OTHER_TENANT))
            .with(user(principal(OTHER_TENANT, WRITER1, "textbook:task:view"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
  }

  // ============================================================
  // ③ 同一人提交后不能自审（改派后变相自审拦截）
  // ============================================================

  @Test
  @DisplayName("03-自审拦截：改派为新主笔后，新主笔提交、原审定人仍不能自审变体；writer/reviewer 同人配置被拒")
  void selfReviewGuards() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    // 同人配置拒绝
    mockMvc.perform(put("/textbook/projects/{id}/assignments", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(assignBody(chapter.get("id").asLong(), WRITER1, WRITER1, null))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("同一人")));

    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();
    saveContent(section, "正文内容", 0, writer1());
    JsonNode submitted = submitSection(section, 1, writer1());

    // submittedBy == 当前审定人 → 409（用同 userId 的 principal 伪造变相自审）
    UserDetails sameIdReviewer = principal(TENANT, WRITER1, "textbook:review:view",
        "textbook:content:review", "textbook:task:view", "textbook:content:write");
    // 注意：此时章分配 reviewer=REVIEWER，WRITER1 不是当前审定人 → 403/404 归属校验先行；
    // 直接构造 submittedBy==reviewer 的自审场景：改派主笔为 REVIEWER 不被允许（同人），
    // 因此自审只可能出现在改派后 submittedBy 遗留 —— 由下列改派流程覆盖：
    // 改派主笔 WRITER1 → WRITER2，submittedBy 仍是 WRITER1，审定人 REVIEWER ≠ WRITER1，合法；
    // 把审定人改派为原主笔 WRITER1（submittedBy=WRITER1）→ 其审定操作必须 409。
    mockMvc.perform(put("/textbook/projects/{id}/assignments", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(assignBody(chapter.get("id").asLong(), WRITER2, WRITER1, 1))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].assignmentVersion").value(2));

    mockMvc.perform(post("/textbook/sections/{id}/review", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\":\"approve\",\"baseVersion\":" + submitted.get("version").asInt() + "}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(sameIdReviewer)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("自己提交")));
  }

  // ============================================================
  // ⑥ C1 候选人过滤
  // ============================================================

  @Test
  @DisplayName("04-C1候选人：仅启用且有对应权限的用户；不暴露手机号")
  void collaboratorFiltering() throws Exception {
    MvcResult writers = mockMvc.perform(get("/textbook/collaborators")
            .param("role", "writer")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode items = data(writers).get("items");
    List<String> ids = new ArrayList<>();
    items.forEach(i -> {
      ids.add(i.get("userId").asText());
      assertTrue(i.get("phone") == null, "不得返回手机号");
    });
    assertTrue(ids.contains(String.valueOf(WRITER1)), "writer1 绑定了 TEXTBOOK_WRITER 应在候选中");
    assertTrue(ids.contains(String.valueOf(WRITER2)));
    assertFalse(ids.contains(String.valueOf(REVIEWER)), "审定专家不应出现在 writer 候选");
    assertFalse(ids.contains(String.valueOf(VIEWER_ONLY)));

    mockMvc.perform(get("/textbook/collaborators")
            .param("role", "reviewer")
            .param("keyword", "主笔二")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
  }

  // ============================================================
  // ⑦ 两标签版本冲突
  // ============================================================

  @Test
  @DisplayName("05-两标签冲突：后提交者409 VERSION_CONFLICT，服务端仍为先保存内容")
  void twoTabVersionConflict() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();

    saveContent(section, "标签一先保存的内容", 0, writer1());
    ObjectNode stale = objectMapper.createObjectNode();
    stale.put("contentMd", "标签二旧页面内容");
    stale.put("baseVersion", 0);
    MvcResult conflict = mockMvc.perform(put("/textbook/sections/{id}/content", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(stale.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isConflict())
        .andReturn();
    JsonNode error = objectMapper.readTree(conflict.getResponse().getContentAsString());
    assertEquals("VERSION_CONFLICT", error.get("data").get("reason").asText());
    assertEquals(1, error.get("data").get("currentVersion").asInt());
    assertEquals("标签一先保存的内容", error.get("data").get("currentContentMd").asText());

    assertEquals("标签一先保存的内容", getContent(section, writer1()).get("contentMd").asText());
  }

  // ============================================================
  // ⑧ 改派：原主笔立即失效 + 真实栅栏并发（改派/保存、改派/审定）
  // ============================================================

  @Test
  @DisplayName("06-改派竞态：CountDownLatch 栅栏下改派/保存真正并行，不产生越权写入")
  void reassignmentRaceWithLatch() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();
    JsonNode saved = saveContent(section, "初始草稿", 0, writer1());
    int version = saved.get("version").asInt();

    CountDownLatch start = new CountDownLatch(1);
    AtomicReference<Integer> saveStatus = new AtomicReference<>();
    AtomicReference<Integer> assignStatus = new AtomicReference<>();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> saver = pool.submit(() -> {
        try {
          start.await();
          ObjectNode body = objectMapper.createObjectNode();
          body.put("contentMd", "旧主笔在改派瞬间的写入");
          body.put("baseVersion", version);
          saveStatus.set(mockMvc.perform(put("/textbook/sections/{id}/content", section)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body.toString())
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(writer1())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          saveStatus.set(-1);
        }
      });
      Future<?> assigner = pool.submit(() -> {
        try {
          start.await();
          assignStatus.set(mockMvc.perform(
                  put("/textbook/projects/{id}/assignments", projectId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(assignBody(chapter.get("id").asLong(), WRITER2, REVIEWER, 1))
                      .header("X-Tenant-Id", String.valueOf(TENANT))
                      .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          assignStatus.set(-1);
        }
      });
      start.countDown();
      saver.get(60, TimeUnit.SECONDS);
      assigner.get(60, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    // 分配必然成功（manage 权限无竞争条件）
    assertEquals(200, assignStatus.get(), "改派请求应成功");
    // 保存要么 200（先赢工程锁）要么 409/404（改派先提交）；绝不允许越权写入新主笔内容
    assertTrue(saveStatus.get() == 200 || saveStatus.get() == 409 || saveStatus.get() == 404,
        "保存状态应为 200/409/404 之一，实际 " + saveStatus.get());
    JsonNode finalContent = getContent(section, writer2());
    if (saveStatus.get() == 200) {
      assertEquals("旧主笔在改派瞬间的写入", finalContent.get("contentMd").asText(),
          "保存成功时内容必须是旧主笔的（锁序保证先于改派）");
    } else {
      assertEquals("初始草稿", finalContent.get("contentMd").asText(),
          "保存被拒时服务端内容不被覆盖");
    }
    // 原主笔立即失效
    ObjectNode body = objectMapper.createObjectNode();
    body.put("contentMd", "改派后原主笔再写");
    body.put("baseVersion", finalContent.get("version").asInt());
    mockMvc.perform(put("/textbook/sections/{id}/content", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().is(404));

    // 改派/审定同样用真实栅栏并发：旧审定人要么先赢（approved），要么改派后 404/409，
    // 不允许改派后的非当前审定人产生审定结果
    JsonNode writer2Draft = saveContent(section, "供审定竞态的内容", finalContent.get("version").asInt(),
        writer2());
    JsonNode submittedForRace = submitSection(section, writer2Draft.get("version").asInt(), writer2());
    int submittedVersion = submittedForRace.get("version").asInt();
    int assignmentVersionBeforeReviewerRace = jdbcTemplate.queryForObject(
        "SELECT assignment_version FROM textbook_chapter_assignment WHERE chapter_id = ?",
        Integer.class, chapter.get("id").asLong());
    CountDownLatch reviewerRaceStart = new CountDownLatch(1);
    AtomicReference<Integer> reviewStatus = new AtomicReference<>();
    AtomicReference<Integer> reassignStatus = new AtomicReference<>();
    ExecutorService reviewerPool = Executors.newFixedThreadPool(2);
    try {
      Future<?> oldReviewer = reviewerPool.submit(() -> {
        try {
          reviewerRaceStart.await();
          ObjectNode reviewBody = objectMapper.createObjectNode();
          reviewBody.put("action", "approve");
          reviewBody.put("baseVersion", submittedVersion);
          reviewStatus.set(mockMvc.perform(post("/textbook/sections/{id}/review", section)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(reviewBody.toString())
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(reviewer())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          reviewStatus.set(-1);
        }
      });
      Future<?> reviewerReassigner = reviewerPool.submit(() -> {
        try {
          reviewerRaceStart.await();
          reassignStatus.set(mockMvc.perform(
                  put("/textbook/projects/{id}/assignments", projectId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(assignBody(chapter.get("id").asLong(), WRITER2, REVIEWER2,
                          assignmentVersionBeforeReviewerRace))
                      .header("X-Tenant-Id", String.valueOf(TENANT))
                      .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          reassignStatus.set(-1);
        }
      });
      reviewerRaceStart.countDown();
      oldReviewer.get(60, TimeUnit.SECONDS);
      reviewerReassigner.get(60, TimeUnit.SECONDS);
    } finally {
      reviewerPool.shutdownNow();
    }
    assertEquals(200, reassignStatus.get(), "改派审定人请求应成功");
    assertTrue(reviewStatus.get() == 200 || reviewStatus.get() == 409 || reviewStatus.get() == 404,
        "旧审定人竞态结果只能是 200/409/404，实际 " + reviewStatus.get());
    JsonNode afterReviewerRace = getContent(section, writer2());
    if (reviewStatus.get() == 200) {
      assertEquals("approved", afterReviewerRace.get("contentStatus").asText());
    } else {
      assertEquals("submitted", afterReviewerRace.get("contentStatus").asText(),
          "旧审定人未获准时不得改变正文状态");
    }
  }

  // ============================================================
  // ⑨ 快照与恢复
  // ============================================================

  @Test
  @DisplayName("07-快照链：两次提交 revision 1/2；恢复生成 before_restore；快照不被后续保存修改；W7 分页稳定")
  void snapshotAndRestoreChain() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();

    saveContent(section, "第一版内容", 0, writer1());
    JsonNode v1 = submitSection(section, 1, writer1());
    assertEquals(1, v1.get("submitRevision").asInt());

    reviewSection(section, "return", "再补充", v1.get("version").asInt());
    saveContent(section, "第二版内容（修订）", v1.get("version").asInt() + 1, writer1());
    JsonNode v2 = submitSection(section, v1.get("version").asInt() + 2, writer1());
    assertEquals(2, v2.get("submitRevision").asInt());

    // W7：快照列表 snapshotNo 倒序，两个 submit 快照
    MvcResult versions = mockMvc.perform(get("/textbook/sections/{id}/versions", section)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode items = data(versions).get("items");
    assertEquals(2, items.size());
    assertEquals(2, items.get(0).get("snapshotNo").asInt());
    assertEquals("submit", items.get(0).get("snapshotType").asText());
    assertTrue(items.get(0).get("contentMd") == null, "列表不含大正文");

    // 快照详情内容不被后续保存修改
    long snapshotId = items.get(1).get("id").asLong();
    MvcResult detail = mockMvc.perform(get("/textbook/section-versions/{id}", snapshotId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    assertEquals("第一版内容", data(detail).get("contentMd").asText());

    // 打回后恢复：先存 before_restore，状态保持 returned
    reviewSection(section, "return", "二次打回", v2.get("version").asInt());
    JsonNode current = getContent(section, writer1());
    assertEquals("returned", current.get("contentStatus").asText());
    ObjectNode restore = objectMapper.createObjectNode();
    restore.put("snapshotId", String.valueOf(snapshotId));
    restore.put("baseVersion", current.get("version").asInt());
    MvcResult restored = mockMvc.perform(post("/textbook/sections/{id}/restore", section)
            .contentType(MediaType.APPLICATION_JSON)
            .content(restore.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode afterRestore = data(restored);
    assertEquals("returned", afterRestore.get("contentStatus").asText(), "恢复不得清除打回标识");
    assertEquals("第一版内容", afterRestore.get("contentMd").asText());
    assertTrue(afterRestore.get("submitRevision").isNull(), "恢复后当前快照引用为 null");
    assertTrue(afterRestore.get("currentSnapshotId").isNull());

    // before_restore 已生成
    mockMvc.perform(get("/textbook/sections/{id}/versions", section)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(3));
  }

  // ============================================================
  // ⑩ 图片 + ⑪ 通用文件绕过
  // ============================================================

  @Test
  @DisplayName("08-图片链路：空正文首传成功；伪造PNG/超限/跨节引用/越权下载失败；快照引用禁删；通用文件接口全拒")
  void imageAssetsAndFileBypass() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    List<JsonNode> sectionList = sections(projectId, chapter.get("id").asLong());
    long sectionA = sectionList.get(0).get("id").asLong();
    long sectionB = sectionList.get(1).get("id").asLong();

    // 空正文首传成功（A1 不创建 content）
    byte[] png = tinyPng();
    MvcResult uploaded = mockMvc.perform(multipart("/textbook/sections/{id}/assets", sectionA)
            .file(new MockMultipartFile("file", "photo.png", "image/png", png))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode asset = data(uploaded);
    String assetId = asset.get("assetId").asText();
    assertEquals("textbook-asset://" + assetId, asset.get("sourceUri").asText());
    mockMvc.perform(get("/textbook/sections/{id}/content", sectionA)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(jsonPath("$.data.contentExists").value(false))
        .andExpect(jsonPath("$.data.version").value(0))
        .andExpect(jsonPath("$.data.assets[0].assetId").value(assetId));

    // A2：归属主笔可 inline 下载 + Cache-Control: private, no-store
    MvcResult download = mockMvc.perform(
            get("/textbook/sections/{s}/assets/{a}/content", sectionA, assetId)
                .header("X-Tenant-Id", String.valueOf(TENANT))
                .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    assertNotNull(download.getResponse().getHeader("Cache-Control"));
    assertTrue(download.getResponse().getHeader("Cache-Control").contains("no-store"));
    assertTrue(download.getResponse().getContentAsByteArray().length > 0);

    // 越权下载：同租户非归属用户（writer2）→ 404；审定人可读
    mockMvc.perform(get("/textbook/sections/{s}/assets/{a}/content", sectionA, assetId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer2())))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/sections/{s}/assets/{a}/content", sectionA, assetId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andExpect(status().isOk());

    // 伪造 PNG（内容非图片）→ 415；超 10MB → 413
    mockMvc.perform(multipart("/textbook/sections/{id}/assets", sectionA)
            .file(new MockMultipartFile("file", "fake.png", "image/png",
                "not-a-png".getBytes(StandardCharsets.UTF_8)))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isUnsupportedMediaType());
    mockMvc.perform(multipart("/textbook/sections/{id}/assets", sectionA)
            .file(new MockMultipartFile("file", "big.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isPayloadTooLarge());

    // 跨节引用：sectionB（同章可写）引用 sectionA 的资产 → 400（资产归属校验）
    ObjectNode foreign = objectMapper.createObjectNode();
    foreign.put("contentMd", "![steal](textbook-asset://" + assetId + ")");
    foreign.put("baseVersion", 0);
    mockMvc.perform(put("/textbook/sections/{id}/content", sectionB)
            .contentType(MediaType.APPLICATION_JSON)
            .content(foreign.toString())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isBadRequest());

    // 通用文件接口绕过：全部拒绝
    Long fileId = jdbcTemplate.queryForObject(
        "SELECT file_id FROM textbook_section_asset WHERE id = ?", Long.class, Long.parseLong(assetId));
    assertNotNull(fileId, "资产元数据应存在");
    mockMvc.perform(get("/files/{id}", fileId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/files/{id}/download-url", fileId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/files/{id}/download", fileId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isNotFound());
    mockMvc.perform(delete("/files/{id}", fileId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isNotFound());

    // 匿名 objectKey 直链（含编码/路径变形）全拒（404=闸门拒绝；400=容器层拒绝畸形 URI，
    // 同样不可达文件内容，两者均通过）。用字面量 URI（不经 {p} 模板展开二次编码），
    // 直接以原始形态打闸门。
    for (String path : List.of(
        "protected/textbook/" + TENANT,
        "protected%2Ftextbook%2F" + TENANT,
        "..%2Fprotected%2Ftextbook",
        "protected%252Ftextbook")) {
      MvcResult examFile = mockMvc.perform(get("/exam/files/" + path)).andReturn();
      int statusCode = examFile.getResponse().getStatus();
      String body = examFile.getResponse().getContentAsString();
      // 拒绝形态二选一：本模块闸门/租户头校验的 404/400；Sa-Token 防火墙对
      // %2F 等危险字符的拦截（HTTP 200 + text/plain「非法请求:」文本，同样不可达文件内容）
      boolean rejectedByGuard = statusCode == 404 || statusCode == 400;
      boolean rejectedBySaTokenFirewall = statusCode == 200
          && body != null && body.contains("非法请求");
      assertTrue(rejectedByGuard || rejectedBySaTokenFirewall,
          "受保护直链必须被拒绝，实际 " + statusCode + "：" + path + "，body=" + head(body, 120));
    }

    // 快照引用禁删：提交带图快照后 A3 → 409
    saveContent(sectionA, "带图正文 ![img](textbook-asset://" + assetId + ")", 0, writer1());
    JsonNode submitted = submitSection(sectionA, 1, writer1());
    mockMvc.perform(delete("/textbook/sections/{s}/assets/{a}", sectionA, assetId)
            .param("baseVersion", String.valueOf(submitted.get("version").asInt()))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isConflict());

    // submitted 状态下 A1/A3 均 409；锁内授权失败的 A1 上传必须立即清理物理文件，
    // 不能因事务回滚导致 deleteAfterCommit 不触发而残留无主受保护对象
    int protectedFilesBefore = countProtectedStorageFiles();
    mockMvc.perform(multipart("/textbook/sections/{id}/assets", sectionA)
            .file(new MockMultipartFile("file", "x.png", "image/png", png))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isConflict());

    // submitted 状态下 A1 上传也一律 409
    mockMvc.perform(multipart("/textbook/sections/{id}/assets", sectionA)
            .file(new MockMultipartFile("file", "y.png", "image/png", png))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isConflict());
    assertEquals(protectedFilesBefore, countProtectedStorageFiles(),
        "授权失败的受保护上传不得在存储层残留物理文件");
  }

  /** courseware-e2e local 存储根下 protected/** 的物理文件数（失败上传清理断言用）。 */
  private static int countProtectedStorageFiles() throws IOException {
    java.nio.file.Path root = java.nio.file.Path.of("target", "courseware-e2e-files", "protected");
    if (!java.nio.file.Files.exists(root)) {
      return 0;
    }
    try (var stream = java.nio.file.Files.walk(root)) {
      return (int) stream.filter(java.nio.file.Files::isRegularFile).count();
    }
  }

  private static byte[] tinyPng() throws IOException {
    java.awt.image.BufferedImage image =
        new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  // ============================================================
  // ⑫ 结构冻结
  // ============================================================

  @Test
  @DisplayName("09-结构冻结：分配后 rebuild/删节 409；标题字数仍可改；reviewed 改结构须先撤回；旧 PUT 不能越级")
  void structureFreeze() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long chapterId = chapter.get("id").asLong();
    long sectionId = sections(projectId, chapterId).get(0).get("id").asLong();

    // writing 下 rebuild/删节 409
    mockMvc.perform(post("/textbook/projects/{id}/rebuild", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isConflict());
    mockMvc.perform(delete("/textbook/outline/nodes/{id}", sectionId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isConflict());

    // 标题/目标字数仍可改（writing 允许安全编辑）
    mockMvc.perform(put("/textbook/outline/nodes/{id}", sectionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"更换阳极（修订名）\",\"targetWords\":5200}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());

    // 旧 PUT 不能把 writing 改成 outlined/reviewed
    for (String target : List.of("outlined", "reviewed", "parsed")) {
      mockMvc.perform(put("/textbook/projects/{id}", projectId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"status\":\"" + target + "\"}")
              .header("X-Tenant-Id", String.valueOf(TENANT))
              .with(user(admin())))
          .andExpect(status().isConflict());
    }

    // reviewed 工程改结构（未撤回）→ 409；撤回后结构字段+status 同请求 → 400
    long projectId2 = reviewedProject(3, "1,11,12");
    mockMvc.perform(put("/textbook/projects/{id}", projectId2)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"wordsPerHour\":2600}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("撤回")));
    mockMvc.perform(put("/textbook/projects/{id}", projectId2)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"wordsPerHour\":2600,\"status\":\"reviewed\"}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isBadRequest());

    // rebuild/首个分配真实并发：reviewed 下 rebuild 必被拒绝，首个分配可成功；
    // 不变量——大纲节点不得被并发重建删掉，工程进入 writing
    JsonNode raceChapter = chapters(projectId2).get(0);
    int nodeCountBeforeRace = outline(projectId2).size();
    CountDownLatch structureRaceStart = new CountDownLatch(1);
    AtomicReference<Integer> rebuildStatus = new AtomicReference<>();
    AtomicReference<Integer> firstAssignStatus = new AtomicReference<>();
    ExecutorService structurePool = Executors.newFixedThreadPool(2);
    try {
      Future<?> rebuilder = structurePool.submit(() -> {
        try {
          structureRaceStart.await();
          rebuildStatus.set(mockMvc.perform(post("/textbook/projects/{id}/rebuild", projectId2)
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          rebuildStatus.set(-1);
        }
      });
      Future<?> firstAssigner = structurePool.submit(() -> {
        try {
          structureRaceStart.await();
          firstAssignStatus.set(mockMvc.perform(
                  put("/textbook/projects/{id}/assignments", projectId2)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(assignBody(raceChapter.get("id").asLong(), WRITER1, REVIEWER, null))
                      .header("X-Tenant-Id", String.valueOf(TENANT))
                      .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          firstAssignStatus.set(-1);
        }
      });
      structureRaceStart.countDown();
      rebuilder.get(60, TimeUnit.SECONDS);
      firstAssigner.get(60, TimeUnit.SECONDS);
    } finally {
      structurePool.shutdownNow();
    }
    assertEquals(409, rebuildStatus.get(), "reviewed 工程 rebuild 必须被状态闸门拒绝");
    assertEquals(200, firstAssignStatus.get(), "首个分配应成功");
    assertEquals("writing", jdbcTemplate.queryForObject(
        "SELECT status FROM textbook_project WHERE id = ?", String.class, projectId2));
    assertEquals(nodeCountBeforeRace, outline(projectId2).size(),
        "并发 rebuild 被拒时不得删除任何大纲节点");
  }

  // ============================================================
  // ⑬ 完成条件 + complete/reopen/cancel 并发
  // ============================================================

  @Test
  @DisplayName("10-完成条件：未分配/未全通过不能 done（409+blockers）；并发 complete/cancel 不产生非法状态")
  void completionConditionsAndRace() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long section = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();
    saveContent(section, "草稿", 0, writer1());

    // 有章未分配 + 有节未通过 → 409 + blockers
    MvcResult blocked = mockMvc.perform(post("/textbook/projects/{id}/complete", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isConflict())
        .andReturn();
    JsonNode error = objectMapper.readTree(blocked.getResponse().getContentAsString());
    ArrayNode blockers = (ArrayNode) error.get("data").get("blockers");
    assertTrue(blockers.size() >= 2, "应同时给出未分配与未通过阻塞项");
    List<String> codes = new ArrayList<>();
    blockers.forEach(b -> codes.add(b.get("code").asText()));
    assertTrue(codes.contains("CHAPTER_UNASSIGNED"));
    assertTrue(codes.contains("SECTION_NOT_APPROVED"));

    // 全部凑齐后 complete；随后 complete/cancel 真实并发
    approveAll(projectId, chapter.get("id").asLong(), WRITER1, -1, -1);
    for (JsonNode other : chapters(projectId)) {
      if (other.get("id").asLong() == chapter.get("id").asLong()) {
        continue;
      }
      assign(projectId, other.get("id").asLong(), WRITER2, REVIEWER, null);
      approveAll(projectId, other.get("id").asLong(), WRITER2, -1, -1);
    }

    CountDownLatch start = new CountDownLatch(1);
    AtomicReference<Integer> completeStatus = new AtomicReference<>();
    AtomicReference<Integer> cancelStatus = new AtomicReference<>();
    Integer assignmentVersion = jdbcTemplate.queryForObject(
        "SELECT assignment_version FROM textbook_chapter_assignment WHERE chapter_id = ?",
        Integer.class, chapter.get("id").asLong());
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> completer = pool.submit(() -> {
        try {
          start.await();
          completeStatus.set(mockMvc.perform(post("/textbook/projects/{id}/complete", projectId)
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          completeStatus.set(-1);
        }
      });
      Future<?> canceller = pool.submit(() -> {
        try {
          start.await();
          cancelStatus.set(mockMvc.perform(
                  post("/textbook/chapters/{id}/assignment/cancel", chapter.get("id").asLong())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"assignmentVersion\":" + assignmentVersion + "}")
                      .header("X-Tenant-Id", String.valueOf(TENANT))
                      .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          cancelStatus.set(-1);
        }
      });
      start.countDown();
      completer.get(60, TimeUnit.SECONDS);
      canceller.get(60, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    // 不变量：状态只有 writing/done；若 done 则 complete 赢；若 cancel 赢则 complete 应 409
    String status = jdbcTemplate.queryForObject(
        "SELECT status FROM textbook_project WHERE id = ?", String.class, projectId);
    assertTrue("writing".equals(status) || "done".equals(status), "非法状态：" + status);
    assertTrue(completeStatus.get() == 200 || completeStatus.get() == 409,
        "complete 应 200 或 409，实际 " + completeStatus.get());
    assertTrue(cancelStatus.get() == 200 || cancelStatus.get() == 409,
        "cancel 应 200 或 409，实际 " + cancelStatus.get());
    if ("done".equals(status)) {
      assertEquals(200, completeStatus.get());
      // done 下 cancel 409
      mockMvc.perform(post("/textbook/chapters/{id}/assignment/cancel", chapter.get("id").asLong())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"assignmentVersion\":" + (assignmentVersion + 1) + "}")
              .header("X-Tenant-Id", String.valueOf(TENANT))
              .with(user(admin())))
          .andExpect(status().isConflict());
    }

    // complete/reopen 也做真实并发：先补齐可能被 cancel 打断的前提，再同时发起 C5/C6。
    // 不变量：最终状态只能是 writing/done；若 done 则 reopen 未赢；若 writing 则 reopen 先落地。
    if ("writing".equals(status)) {
      Integer currentVersion = jdbcTemplate.queryForObject(
          "SELECT assignment_version FROM textbook_chapter_assignment WHERE chapter_id = ?",
          Integer.class, chapter.get("id").asLong());
      assertNotNull(currentVersion);
      mockMvc.perform(put("/textbook/projects/{id}/assignments", projectId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(assignBody(chapter.get("id").asLong(), WRITER1, REVIEWER, currentVersion))
              .header("X-Tenant-Id", String.valueOf(TENANT))
              .with(user(admin())))
          .andExpect(status().isOk());
      mockMvc.perform(post("/textbook/projects/{id}/complete", projectId)
              .header("X-Tenant-Id", String.valueOf(TENANT))
              .with(user(admin())))
          .andExpect(status().isOk());
    }
    CountDownLatch reopenRaceStart = new CountDownLatch(1);
    AtomicReference<Integer> reopenStatus = new AtomicReference<>();
    AtomicReference<Integer> completeAgainStatus = new AtomicReference<>();
    ExecutorService reopenPool = Executors.newFixedThreadPool(2);
    try {
      Future<?> reopener = reopenPool.submit(() -> {
        try {
          reopenRaceStart.await();
          reopenStatus.set(mockMvc.perform(post("/textbook/projects/{id}/reopen", projectId)
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          reopenStatus.set(-1);
        }
      });
      Future<?> completerAgain = reopenPool.submit(() -> {
        try {
          reopenRaceStart.await();
          completeAgainStatus.set(mockMvc.perform(post("/textbook/projects/{id}/complete", projectId)
                  .header("X-Tenant-Id", String.valueOf(TENANT))
                  .with(user(admin())))
              .andReturn().getResponse().getStatus());
        } catch (Exception e) {
          completeAgainStatus.set(-1);
        }
      });
      reopenRaceStart.countDown();
      reopener.get(60, TimeUnit.SECONDS);
      completerAgain.get(60, TimeUnit.SECONDS);
    } finally {
      reopenPool.shutdownNow();
    }
    String finalRaceStatus = jdbcTemplate.queryForObject(
        "SELECT status FROM textbook_project WHERE id = ?", String.class, projectId);
    assertTrue("writing".equals(finalRaceStatus) || "done".equals(finalRaceStatus),
        "complete/reopen 并发后非法状态：" + finalRaceStatus);
    assertTrue(reopenStatus.get() == 200 || reopenStatus.get() == 409,
        "reopen 应 200/409，实际 " + reopenStatus.get());
    assertTrue(completeAgainStatus.get() == 200 || completeAgainStatus.get() == 409,
        "complete 应 200/409，实际 " + completeAgainStatus.get());
    if ("writing".equals(finalRaceStatus)) {
      assertEquals(200, reopenStatus.get(), "最终 writing 说明 reopen 必须成功");
    }
    // 最终 done 时 reopen 可能 409（complete 先赢），也可能 200 后 complete 又抢回 done；
    // 两者都合法，上面已断言状态与各请求码的白名单。
  }

  // ============================================================
  // ⑭ 统计卡全量（任务数 > pageSize）
  // ============================================================

  @Test
  @DisplayName("11-统计卡：任务数大于 pageSize 时仍按当前用户全量统计，筛选/分页不改变 T3/T4 数字")
  void summaryCardsAreGlobal() throws Exception {
    long projectId = reviewedProject();
    int assigned = 0;
    for (JsonNode chapter : chapters(projectId)) {
      assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
      assigned++;
    }
    assertTrue(assigned >= 3, "章数应超过分页 pageSize=1");

    mockMvc.perform(get("/textbook/my-writing-tasks")
            .param("pageSize", "1")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(assigned))
        .andExpect(jsonPath("$.data.items.length()").value(1));

    // 关键字 EXISTS 子查询参数化：命中教材/章名；注入式关键字不得扩大结果集
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .param("keyword", "铝电解")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(assigned));
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .param("keyword", "' OR '1'='1")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));

    // T3：不受分页影响（assigned 全量）；带筛选仍全量
    mockMvc.perform(get("/textbook/my-writing-tasks/summary")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.assignedCount").value(assigned));
    mockMvc.perform(get("/textbook/my-writing-tasks")
            .param("status", "approved")
            .param("pageSize", "1")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));

    // T4：reviewer 统计卡全量
    mockMvc.perform(get("/textbook/my-review-tasks/summary")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(reviewer())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pendingCount").value(0));
  }

  // ============================================================
  // ⑮ 导出（review/单节/单章）+ ⑯ 种子三步链
  // ============================================================

  @Test
  @DisplayName("12-导出与种子：review/单节/单章导出权限与文件名；docx 可重开；角色/菜单/权限三步链齐全且幂等")
  void exportsAndSeedChain() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    assign(projectId, chapter.get("id").asLong(), WRITER1, REVIEWER, null);
    long sectionId = sections(projectId, chapter.get("id").asLong()).get(0).get("id").asLong();
    saveContent(sectionId, "## 小节\n\n正文内容，含 [待专家补充] 一处。", 0, writer1());

    // E1 单节：主笔自己可导出；文件名口径
    MvcResult sectionDocx = mockMvc.perform(get("/textbook/sections/{id}/export-docx", sectionId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer1())))
        .andExpect(status().isOk())
        .andReturn();
    assertTrue(sectionDocx.getResponse().getHeader("Content-Disposition")
        .contains("filename*=UTF-8''"));
    try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(
        sectionDocx.getResponse().getContentAsByteArray()))) {
      assertTrue(doc.getParagraphs().size() > 2);
    }

    // E2 单章：无关联的 writer2 → 404；admin(view) → 200
    mockMvc.perform(get("/textbook/chapters/{id}/export-docx", chapter.get("id").asLong())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(writer2())))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/textbook/chapters/{id}/export-docx", chapter.get("id").asLong())
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());

    // E3 review 整册：需要 export 权限（view-only → 403）
    mockMvc.perform(get("/textbook/projects/{id}/export-book", projectId)
            .param("mode", "review")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(viewer())))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/textbook/projects/{id}/export-book", projectId)
            .param("mode", "review")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(exporter())))
        .andExpect(status().isOk())
        .andExpect(result -> result.getResponse().getHeader("Content-Disposition")
            .contains("review"));

    // E4 final：非 done → 409
    mockMvc.perform(get("/textbook/projects/{id}/export-book", projectId)
            .param("mode", "final")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(exporter())))
        .andExpect(status().isConflict());

    // 种子三步链：菜单→权限→角色授权 齐全且幂等（唯一一条）
    assertEquals(2, count("SELECT COUNT(*) FROM sys_menu WHERE tenant_id=? AND code IN "
            + "('textbook_my_writing','textbook_my_review') AND deleted=0", TENANT));
    assertEquals(4, count("SELECT COUNT(*) FROM sys_permission WHERE tenant_id=? "
            + "AND code IN ('textbook:task:view','textbook:content:write',"
            + "'textbook:review:view','textbook:content:review') AND deleted=0", TENANT));
    assertEquals(2, count("SELECT COUNT(*) FROM sys_role WHERE tenant_id=? "
            + "AND code IN ('TEXTBOOK_WRITER','TEXTBOOK_REVIEWER') AND deleted=0", TENANT));
    // 角色权限链：writer 2 权限 / reviewer 2 权限 / 管理员 2000 全部 4 权限
    assertEquals(2, count("""
        SELECT COUNT(*) FROM sys_role_permission rp
        JOIN sys_role r ON r.id = rp.role_id AND r.code = 'TEXTBOOK_WRITER' AND r.deleted = 0
        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
          AND p.code LIKE 'textbook:%'
        WHERE rp.tenant_id = ? AND rp.deleted = 0""", TENANT), "TEXTBOOK_WRITER 授权链");
    assertEquals(2, count("""
        SELECT COUNT(*) FROM sys_role_permission rp
        JOIN sys_role r ON r.id = rp.role_id AND r.code = 'TEXTBOOK_REVIEWER' AND r.deleted = 0
        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
          AND p.code LIKE 'textbook:%'
        WHERE rp.tenant_id = ? AND rp.deleted = 0""", TENANT), "TEXTBOOK_REVIEWER 授权链");
    assertEquals(7, count("""
        SELECT COUNT(DISTINCT p.code) FROM sys_role_permission rp
        JOIN sys_role r ON r.id = rp.role_id AND r.id = 2000 AND r.deleted = 0
        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
          AND p.code LIKE 'textbook:%'
        WHERE rp.tenant_id = ? AND rp.deleted = 0""", TENANT), "平台管理员应持有教材全部 7 权限");
    // 菜单挂载链
    assertEquals(2, count("""
        SELECT COUNT(*) FROM sys_role_menu rm
        JOIN sys_menu m ON m.id = rm.menu_id AND m.code IN
          ('textbook_my_writing','textbook_my_review') AND m.deleted = 0
        WHERE rm.tenant_id = ? AND rm.role_id = 2000 AND rm.deleted = 0""", TENANT));
  }

  private long count(String sql, Object... args) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  // ============================================================
  // ⑯ 删除级联 + 重建保留分配行（两轮重建唯一键）
  // ============================================================

  @Test
  @DisplayName("13-重建与删除：取消→重分配复用原行；有正文 writing 禁撤回、全取消无内容可受控撤回重建；两轮重建保留人工章；删除级联")
  void rebuildTwiceAndDeleteCascade() throws Exception {
    long projectId = reviewedProject();
    JsonNode chapter = chapterByTitle(projectId, "铝电解槽操作");
    long chapterId = chapter.get("id").asLong();
    assign(projectId, chapterId, WRITER1, REVIEWER, null);
    long section = sections(projectId, chapterId).get(0).get("id").asLong();
    saveContent(section, "正文", 0, writer1());

    // writing 下：撤回评审为 outlined → 409（§3.5 白名单：writing 不允许回退）
    mockMvc.perform(put("/textbook/projects/{id}", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"outlined\"}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isConflict());

    // 取消分配 → 重新分配：复用原行（active=0 → 1，version+1），不撞唯一键
    Integer version = jdbcTemplate.queryForObject(
        "SELECT assignment_version FROM textbook_chapter_assignment WHERE chapter_id = ?",
        Integer.class, chapterId);
    mockMvc.perform(post("/textbook/chapters/{id}/assignment/cancel", chapterId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"assignmentVersion\":" + version + "}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.active").value(false));
    mockMvc.perform(put("/textbook/projects/{id}/assignments", projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(assignBody(chapterId, WRITER2, REVIEWER, version + 1))
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    Long rows = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM textbook_chapter_assignment WHERE chapter_id = ?",
        Long.class, chapterId);
    assertEquals(1L, rows, "「取消→重新分配」后每章仍只有一行当前分配（复用原行）");

    // 两轮重建（从未分配的 outlined 工程）：人工章保留原 ID、合计守恒
    long projectId2 = reviewedProject(3, "1,11,12");
    JsonNode chapter2 = chapterByTitle(projectId2, "铝电解槽操作");
    long chapter2Id = chapter2.get("id").asLong();
    mockMvc.perform(put("/textbook/projects/{id}", projectId2)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"outlined\"}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    mockMvc.perform(put("/textbook/outline/nodes/{id}", chapter2Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetWords\":8000}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    for (int round = 1; round <= 2; round++) {
      mockMvc.perform(post("/textbook/projects/{id}/rebuild", projectId2)
              .header("X-Tenant-Id", String.valueOf(TENANT))
              .with(user(admin())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.balanced").value(true));
      JsonNode kept = chapterByTitle(projectId2, "铝电解槽操作");
      assertEquals(chapter2Id, kept.get("id").asLong(), "第 " + round + " 轮重建保留人工章原 ID");
      assertEquals(8000, kept.get("targetWords").asInt());
    }

    // writing 的受控撤回：无任何正文且全部分配取消后，才允许 writing→outlined 并重建
    long projectId3 = reviewedProject(4, "1,2,12");
    JsonNode chapter3 = chapterByTitle(projectId3, "铝电解槽操作");
    long chapter3Id = chapter3.get("id").asLong();
    assign(projectId3, chapter3Id, WRITER1, REVIEWER, null);
    Integer version3 = jdbcTemplate.queryForObject(
        "SELECT assignment_version FROM textbook_chapter_assignment WHERE chapter_id = ?",
        Integer.class, chapter3Id);
    mockMvc.perform(post("/textbook/chapters/{id}/assignment/cancel", chapter3Id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"assignmentVersion\":" + version3 + "}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    mockMvc.perform(put("/textbook/projects/{id}", projectId3)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"outlined\"}")
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("outlined"));
    mockMvc.perform(post("/textbook/projects/{id}/rebuild", projectId3)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());

    // 工程删除：级联软删新表（含正文/快照/审定/资产元数据）。
    // 章已改派给 WRITER2，须由新主笔继续保存（原主笔立即失效是设计行为）；
    // 版本以服务端当前值为准（改派/取消不改变正文 version）
    JsonNode beforeDelete = getContent(section, writer2());
    saveContent(section, "更多正文", beforeDelete.get("version").asInt(), writer2());
    mockMvc.perform(delete("/textbook/projects/{id}", projectId)
            .header("X-Tenant-Id", String.valueOf(TENANT))
            .with(user(admin())))
        .andExpect(status().isOk());
    assertEquals(0L, count(
        "SELECT COUNT(*) FROM textbook_section_content WHERE project_id = ? AND deleted = 0",
        projectId));
    assertEquals(0L, count(
        "SELECT COUNT(*) FROM textbook_chapter_assignment WHERE project_id = ? AND deleted = 0",
        projectId));
    assertEquals(0L, count(
        "SELECT COUNT(*) FROM textbook_section_content_version v JOIN "
            + "textbook_section_content c ON c.id = v.content_id WHERE c.project_id = ? "
            + "AND v.deleted = 0", projectId));
  }

  // ============================================================
  // 工具
  // ============================================================

  private static UserDetails principal(long tenantId, long userId, String... permissions) {
    return new PlatformUserPrincipal(userId, tenantId, "u" + userId, "用户" + userId, "admin",
        List.of(), List.of(permissions), null);
  }

  private static UserDetails admin() {
    return principal(TENANT, ADMIN, "textbook:project:view", "textbook:project:manage",
        "textbook:project:export", "file:delete");
  }

  private static UserDetails exporter() {
    return principal(TENANT, ADMIN, "textbook:project:view", "textbook:project:export");
  }

  private static UserDetails writer1() {
    return principal(TENANT, WRITER1, "textbook:task:view", "textbook:content:write");
  }

  private static UserDetails writer2() {
    return principal(TENANT, WRITER2, "textbook:task:view", "textbook:content:write");
  }

  private static UserDetails reviewer() {
    return principal(TENANT, REVIEWER, "textbook:review:view", "textbook:content:review");
  }

  private static UserDetails reviewer2() {
    return principal(TENANT, REVIEWER2, "textbook:review:view", "textbook:content:review");
  }

  private static UserDetails viewer() {
    return principal(TENANT, VIEWER_ONLY, "textbook:project:view");
  }

  private static UserDetails nobody() {
    return principal(TENANT, NOBODY);
  }

  private JsonNode data(MvcResult result) throws IOException {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
  }

  private static String head(String text, int max) {
    if (text == null) {
      return "";
    }
    return text.length() <= max ? text : text.substring(0, max);
  }
}
