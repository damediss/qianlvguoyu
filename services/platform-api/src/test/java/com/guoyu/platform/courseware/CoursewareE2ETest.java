package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * EA-01~EA-11 API 级 E2E(方案 §15.4):真实启动 + 测试库(gjjddb2026) + JDK HttpServer LLM 桩。
 * 桩按 system prompt 关键词分流 digest/outline/slide 三阶段;并发桩必须 setExecutor(既有坑)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("courseware-e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursewareE2ETest {

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  ObjectMapper om;

  @Autowired
  com.guoyu.platform.courseware.mapper.CwProjectMapper cwProjectMapper;

  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("coursewareExecutor")
  org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor coursewareExecutor;

  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("coursewareSlideExecutor")
  org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor slideExecutor;

  // ──────────────────────── LLM 桩 ────────────────────────

  static HttpServer llmStub;
  static int llmPort;

  /** 桩行为开关(每测试复位) */
  static volatile boolean digestBad = false;
  static volatile int slowDigestMs = 0;
  static volatile List<String> failTitles = List.of();       // 命中标题 → HTTP 500
  static volatile List<String> structureBadTitles = List.of(); // 命中 → 缺 body 的坏 JSON(残留 error)
  static final AtomicBoolean nextSlideBad = new AtomicBoolean(false);
  static final AtomicInteger slideCalls = new AtomicInteger(0);
  static final Map<String, AtomicInteger> stageCalls = new ConcurrentHashMap<>();

  static final String DIGEST_JSON = """
      {"topics":[
        {"name":"热处理基础","points":["淬火加热保温使组织奥氏体化"],"confusions":["淬火与回火目的不同"],
         "key_data":["淬火温度 850℃"],"source_refs":["S1:1"]},
        {"name":"工艺流程","points":["备料装炉加热保温出炉"],"confusions":[],
         "key_data":[],"source_refs":["S1:2"]},
        {"name":"质量检验","points":["硬度抽检判定合格"],"confusions":[],
         "key_data":["抽检比例 10%"],"source_refs":["S1:3"]}
      ]}""";

  static final String OUTLINE_JSON = """
      {"chapters":[
        {"chapter_title":"热处理基础","core_conclusion":"温度决定组织",
         "pages":[
           {"title":"热处理基础","objective":"概览","key_points":[],"page_role":"section",
            "layout_hint":"content_bullets","source_refs":["S1:1"]},
           {"title":"淬火温度决定硬度上限","objective":"讲清温度",
            "key_points":["加热到 850℃ 奥氏体化","冷却速度决定转变","回火调整韧性"],
            "page_role":"content","layout_hint":"content_bullets","source_refs":["S1:1"]}]},
        {"chapter_title":"工艺流程","core_conclusion":"五步走完",
         "pages":[
           {"title":"工艺流程","objective":"概览","key_points":[],"page_role":"section",
            "layout_hint":"content_bullets","source_refs":["S1:2"]},
           {"title":"装炉方式影响受热均匀性","objective":"讲装炉",
            "key_points":["备料要除锈","装炉留间隙","加热要匀速"],
            "page_role":"content","layout_hint":"content_bullets","source_refs":["S1:2"]}]},
        {"chapter_title":"质量检验","core_conclusion":"抽检兜底",
         "pages":[
           {"title":"质量检验","objective":"概览","key_points":[],"page_role":"section",
            "layout_hint":"content_bullets","source_refs":["S1:3"]},
           {"title":"硬度抽检比例不低于一成","objective":"讲抽检",
            "key_points":["抽检比例 10%","不合格整批复检","记录要留档"],
            "page_role":"content","layout_hint":"content_bullets","source_refs":["S1:3"]}]}
      ],"review_questions":["淬火与回火的区别是什么?","为什么冷却速度影响硬度?","抽检比例是多少?"]}
      """;

  static String slideJsonForLayout(String layoutId) {
    return switch (layoutId) {
      case "cover" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"热处理工艺培训课件"},
            {"slot_id":"subtitle","type":"text","text":"学员自学版"}],
           "speaker_notes":"开场介绍课程目标"}
          """;
      case "toc" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"目录"},
            {"slot_id":"body","type":"bullets","items":["热处理基础","工艺流程","质量检验"]}],
           "speaker_notes":"说明三章结构"}
          """;
      case "section" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"章节标题"},
            {"slot_id":"text","type":"text","text":"本章讲清一个核心结论"}],
           "speaker_notes":"章节引入"}
          """;
      case "summary" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"复习思考题"},
            {"slot_id":"body","type":"bullets","items":[
              "淬火与回火的区别是什么?","为什么冷却速度影响硬度?","抽检比例是多少?"]}],
           "speaker_notes":"引导自测"}
          """;
      case "content_table" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"本页核心结论标题"},
            {"slot_id":"body","type":"table","header":["参数","数值"],
             "rows":[["温度","850℃"],["时间","30min"]]}],
           "speaker_notes":"本页重点提示"}
          """;
      case "content_kpi" -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"本页核心结论标题"},
            {"slot_id":"kpi_1","type":"kpi","value":"850℃","label":"淬火温度","note":"碳钢上限"}],
           "speaker_notes":"本页重点提示"}
          """;
      default -> """
          {"blocks":[
            {"slot_id":"title","type":"text","text":"本页核心结论标题"},
            {"slot_id":"body","type":"bullets","items":[
              "要点一内容充实具体可考核","要点二内容充实具体可考核","要点三内容充实具体可考核"]}],
           "speaker_notes":"本页重点提示"}
          """;
    };
  }

  /** 坏结构:未知槽位 ghost(任意版式都触发 structure error) */
  static final String BAD_STRUCTURE_JSON = """
      {"blocks":[{"slot_id":"title","type":"text","text":"只有标题的坏页"},
        {"slot_id":"ghost","type":"text","text":"不存在槽位"}],"speaker_notes":""}
      """;

  private static final java.util.regex.Pattern LAYOUT_RE =
      java.util.regex.Pattern.compile("本页布局为 ([a-z_]+)");
  private static final java.util.regex.Pattern TITLE_RE =
      java.util.regex.Pattern.compile("\"title\":\"((?:[^\"\\\\]|\\\\.)*)\"");

  @BeforeAll
  void startStubAndConfig() throws IOException {
    llmStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    // 并发桩必须显式 setExecutor(否则串行处理,并发测试假死——既有坑)
    llmStub.setExecutor(Executors.newCachedThreadPool());
    llmStub.createContext("/chat/completions", exchange -> {
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      String system = "";
      String user = "";
      try {
        JsonNode req = om.readTree(body);
        for (JsonNode m : req.path("messages")) {
          if ("system".equals(m.path("role").asText())) {
            system = m.path("content").asText("");
          } else if ("user".equals(m.path("role").asText())) {
            user = m.path("content").asText("");
          }
        }
      } catch (Exception ignored) {
        // 解析失败按 slide 阶段处理
      }
      final String systemF = system;
      String stage = systemF.contains("职业培训教研员") ? "digest"
          : systemF.contains("职业培训课件策划") ? "outline" : "slide";
      stageCalls.computeIfAbsent(stage, k -> new AtomicInteger()).incrementAndGet();

      String content;
      int status = 200;
      if ("digest".equals(stage)) {
        if (slowDigestMs > 0) {
          try {
            Thread.sleep(slowDigestMs);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
        }
        content = digestBad ? "这不是 JSON" : DIGEST_JSON;
      } else if ("outline".equals(stage)) {
        content = OUTLINE_JSON;
      } else {
        slideCalls.incrementAndGet();
        // 精确取本页标题(user JSON 里 page.title;deck_title 前缀是 _ 不匹配)
        java.util.regex.Matcher tm = TITLE_RE.matcher(user);
        String pageTitle = tm.find() ? tm.group(1) : "";
        java.util.regex.Matcher lm = LAYOUT_RE.matcher(systemF);
        String layoutId = lm.find() ? lm.group(1) : "content_bullets";
        if (failTitles.contains(pageTitle)) {
          status = 500;
          content = "{\"error\":\"stub 500\"}";
        } else if (structureBadTitles.contains(pageTitle)) {
          content = BAD_STRUCTURE_JSON;
        } else if (nextSlideBad.compareAndSet(true, false)) {
          content = BAD_STRUCTURE_JSON;
        } else {
          content = slideJsonForLayout(layoutId);
        }
      }
      String resp;
      if (status == 200) {
        String escaped = om.writeValueAsString(content);
        resp = "{\"id\":\"stub\",\"object\":\"chat.completion\",\"created\":1,"
            + "\"model\":\"GLM-5.3-Flash\",\"choices\":[{\"index\":0,\"message\":"
            + "{\"role\":\"assistant\",\"content\":" + escaped + "},\"finish_reason\":\"stop\"}],"
            + "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50,\"total_tokens\":150}}";
      } else {
        resp = "{\"error\":{\"message\":\"stub error\"}}";
      }
      byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    });
    llmStub.start();
    llmPort = llmStub.getAddress().getPort();

    // 模型配置指向桩(种子行由 Flyway 迁移在启动时写入)
    jdbc.update("UPDATE exam_faq_model_config SET base_url=?, api_key='stub-key' "
        + "WHERE model_code='courseware-gen-glm' AND tenant_id=10159",
        "http://127.0.0.1:" + llmPort);
  }

  @AfterAll
  void restoreConfig() {
    if (llmStub != null) {
      llmStub.stop(0);
    }
    jdbc.update("UPDATE exam_faq_model_config "
            + "SET base_url='https://open.bigmodel.cn/api/paas/v4', api_key='PENDING_USER_INPUT' "
            + "WHERE model_code='courseware-gen-glm' AND tenant_id=10159");
    cleanup();
  }

  @BeforeEach
  void resetStub() throws InterruptedException {
    digestBad = false;
    slowDigestMs = 0;
    failTitles = List.of();
    structureBadTitles = List.of();
    nextSlideBad.set(false);
    slideCalls.set(0);
    stageCalls.clear();
    // 排空上一用例的编排/页池,避免池容量被残留任务占用导致跨用例 503
    awaitIdle(coursewareExecutor);
    awaitIdle(slideExecutor);
  }

  /** 等待线程池空闲(活动线程 0 且队列空),上限 120s */
  private static void awaitIdle(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor ex)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 120_000;
    while (System.currentTimeMillis() < deadline) {
      java.util.concurrent.ThreadPoolExecutor tpe = ex.getThreadPoolExecutor();
      if (tpe.getActiveCount() == 0 && tpe.getQueue().isEmpty()) {
        return;
      }
      Thread.sleep(500);
    }
    throw new IllegalStateException("线程池排空超时: " + ex.getThreadNamePrefix());
  }

  void cleanup() {
    // 只清测试自建项目(按标题前缀收敛):误删过冒烟项目,禁止全表 DELETE(2026-09-21 事故)
    jdbc.execute("""
        DELETE s, o, sec, d, lg, pr FROM cw_project pr
        LEFT JOIN cw_slide s ON s.project_id = pr.id
        LEFT JOIN cw_outline_page o ON o.project_id = pr.id
        LEFT JOIN cw_source_section sec ON sec.project_id = pr.id
        LEFT JOIN cw_digest d ON d.project_id = pr.id
        LEFT JOIN cw_llm_log lg ON lg.project_id = pr.id
        WHERE pr.title LIKE '热处理工艺培训%' OR pr.title LIKE '并发%' OR pr.title LIKE '课件A%'
        """);
  }

  // ──────────────────────── HTTP 辅助 ────────────────────────

  String adminToken;

  HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
    headers.set("X-Tenant-Id", "10159");
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  JsonNode login() {
    JsonNode resp = post("/auth/login",
        Map.of("tenantCode", "demo", "username", "admin", "password", "Admin@123456"),
        new HttpHeaders());
    assertEquals("0", resp.path("code").asText(), "登录失败: " + resp);
    adminToken = resp.path("data").path("accessToken").asText();
    // 自检:token 立刻打 /auth/me,验证过滤器接受
    System.out.println("[E2E-DEBUG] /auth/me => " + get("/auth/me"));
    return resp;
  }

  JsonNode post(String path, Object body, HttpHeaders headers) {
    HttpEntity<Object> entity = new HttpEntity<>(body,
        headers != null ? headers : authHeaders());
    ResponseEntity<JsonNode> resp = rest.postForEntity(path, entity, JsonNode.class);
    return resp.getBody();
  }

  JsonNode get(String path) {
    ResponseEntity<JsonNode> resp = rest.exchange(path, HttpMethod.GET,
        new HttpEntity<>(authHeaders()), JsonNode.class);
    return resp.getBody();
  }

  Long uploadDocx(String filename, byte[] bytes) {
    HttpHeaders headers = authHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return filename;
      }
    });
    form.add("accessPolicy", "private");
    ResponseEntity<JsonNode> resp = rest.postForEntity("/files",
        new HttpEntity<>(form, headers), JsonNode.class);
    JsonNode body = resp.getBody();
    assertNotNull(body);
    assertEquals("0", body.path("code").asText(), "上传失败: " + body);
    return body.path("data").path("id").asLong();
  }

  /** 轮询项目状态直到目标或超时 */
  JsonNode pollProject(Long projectId, String until, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      JsonNode resp = get("/courseware/projects/" + projectId);
      String status = resp.path("data").path("status").asText();
      if (until.equals(status)) {
        return resp.path("data");
      }
      if ("failed".equals(status) && !until.equals("failed")) {
        fail("项目进入 failed: " + resp.path("data").path("error").asText());
      }
      Thread.sleep(500);
    }
    fail("等待状态 " + until + " 超时");
    return null;
  }

  JsonNode pollProgress(Long projectId, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      JsonNode data = get("/courseware/projects/" + projectId + "/progress").path("data");
      String status = data.path("status").asText();
      if (List.of("ready", "partial", "cancelled", "failed").contains(status)) {
        return data;
      }
      Thread.sleep(500);
    }
    fail("等待生成终态超时");
    return null;
  }

  /** 建测试 docx:3 个一级标题小节 */
  static byte[] buildTestDocx() throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      doc.createParagraph().createRun().setText("第一章 热处理基础");
      doc.createParagraph().createRun().setText("淬火温度 850℃,保温 30 分钟,组织奥氏体化。");
      doc.createParagraph().createRun().setText("第二章 工艺流程");
      doc.createParagraph().createRun().setText("备料、装炉、加热、保温、出炉五步走。");
      doc.createParagraph().createRun().setText("第三章 质量检验");
      doc.createParagraph().createRun().setText("硬度抽检比例 10%,不合格整批复检。");
      doc.write(out);
      return out.toByteArray();
    }
  }

  /** 走通 上传→大纲就绪 的公共前置 */
  Long createProjectToOutlineReady() throws Exception {
    Long fileId = uploadDocx("热处理标准.docx", buildTestDocx());
    JsonNode created = post("/courseware/projects",
        Map.of("sourceFileId", fileId, "title", "热处理工艺培训", "contentDensity", "medium"), null);
    assertEquals("0", created.path("code").asText(), "创建失败: " + created);
    Long projectId = created.path("data").path("id").asLong();
    pollProject(projectId, "outline_ready", 60_000);
    return projectId;
  }

  void confirm(Long projectId) {
    JsonNode resp = post("/courseware/projects/" + projectId + "/confirm", Map.of(), null);
    assertEquals("0", resp.path("code").asText(), "确认失败: " + resp);
  }

  // ──────────────────────── 测试 ────────────────────────

  @BeforeAll
  void loginOnce() {
    login();
  }

  @Test
  @Order(1)
  @DisplayName("EA-01 全链路 happy path:上传→大纲→确认→生成→导出→下载回读")
  void ea01_happyPath() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();

    // 断言库内三阶段产物
    Integer sections = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_source_section WHERE project_id=" + projectId, Integer.class);
    assertEquals(3, sections, "docx 解析 3 个小节");
    Integer digest = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_digest WHERE project_id=" + projectId, Integer.class);
    assertEquals(1, digest, "消化素材已落库");
    Integer outlinePages = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_outline_page WHERE project_id=" + projectId, Integer.class);
    assertEquals(9, outlinePages, "3 章×2 页 + cover/toc/review = 9 页");
    // 末页是复习思考题
    String lastRole = jdbc.queryForObject(
        "SELECT page_role FROM cw_outline_page WHERE project_id=" + projectId
            + " ORDER BY position DESC LIMIT 1", String.class);
    assertEquals("review", lastRole);

    confirm(projectId);
    JsonNode progress = pollProgress(projectId, 90_000);
    assertEquals("ready", progress.path("status").asText());
    assertEquals(9, progress.path("readyPages").asInt());
    assertEquals(0, progress.path("failedPages").asInt());
    // 页内容与学习提示非空
    Integer readyWithNotes = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_slide WHERE project_id=" + projectId
            + " AND status='ready' AND speaker_notes IS NOT NULL AND speaker_notes<>''",
        Integer.class);
    assertEquals(9, readyWithNotes);

    // 导出 → fileId → 下载回读
    JsonNode exportResp = post("/courseware/projects/" + projectId + "/export", Map.of(), null);
    assertEquals("0", exportResp.path("code").asText());
    JsonNode exportData = exportResp.path("data");
    assertFalse(exportData.path("blocked").asBoolean(), "正常项目导出不被阻断");
    Long exportFileId = exportData.path("fileId").asLong();
    assertTrue(exportFileId > 0);

    ResponseEntity<byte[]> download = rest.exchange("/files/" + exportFileId + "/download",
        HttpMethod.GET, new HttpEntity<>(authHeaders()), byte[].class);
    assertEquals(200, download.getStatusCode().value());
    assertNotNull(download.getBody());
    com.guoyu.platform.courseware.service.PptxRenderer.verify(download.getBody(), 9);

    // EA-10b:重复导出幂等(内容以当前 blocks 为准,两次都成功)
    JsonNode exportAgain = post("/courseware/projects/" + projectId + "/export", Map.of(), null);
    assertEquals("0", exportAgain.path("code").asText());
    assertFalse(exportAgain.path("data").path("blocked").asBoolean());

    // EA-11:审计落库(digest 1 + outline 1 + slide ≥9)
    Integer logs = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_llm_log WHERE project_id=" + projectId, Integer.class);
    assertTrue(logs >= 11, "审计覆盖三阶段: " + logs);
    Integer badLogs = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_llm_log WHERE project_id=" + projectId
            + " AND (model_code<>'courseware-gen-glm' OR duration_ms IS NULL OR success IS NULL)",
        Integer.class);
    assertEquals(0, badLogs);
  }

  @Test
  @Order(2)
  @DisplayName("EA-02 digest 失败降级:大纲仍生成,cw_digest 无记录")
  void ea02_digestDegrade() throws Exception {
    cleanup();
    digestBad = true;
    Long projectId = createProjectToOutlineReady();
    Integer digest = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_digest WHERE project_id=" + projectId, Integer.class);
    assertEquals(0, digest, "消化失败降级:不落库");
    Integer outlinePages = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_outline_page WHERE project_id=" + projectId, Integer.class);
    assertEquals(9, outlinePages, "降级后大纲以原始小节生成");
  }

  @Test
  @Order(3)
  @DisplayName("EA-03 大纲编辑后确认:改标题/增页/删页/调序,slide 初始化一致")
  void ea03_outlineEditThenConfirm() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();

    JsonNode outline = get("/courseware/projects/" + projectId + "/outline").path("data");
    assertEquals(9, outline.size());
    // 改第 3 页标题
    JsonNode page3 = outline.get(2);
    JsonNode updated = put("/courseware/outline/pages/" + page3.path("id").asText(),
        Map.of("title", "热处理基础(改)"));
    assertEquals("0", updated.path("code").asText());
    // 增一页到第 1 章
    JsonNode added = post("/courseware/projects/" + projectId + "/outline/pages",
        Map.of("chapterNo", 1, "title", "新增内容页", "keyPoints", List.of("要点一内容充实")), null);
    assertEquals("0", added.path("code").asText());
    // 删掉刚新增的页
    JsonNode del = delete("/courseware/outline/pages/" + added.path("data").path("id").asText());
    assertEquals("0", del.path("code").asText());
    // 调序:第 3、4 页互换
    JsonNode outline2 = get("/courseware/projects/" + projectId + "/outline").path("data");
    List<String> ids = new ArrayList<>();
    outline2.forEach(n -> ids.add(n.path("id").asText()));
    String tmp = ids.get(2);
    ids.set(2, ids.get(3));
    ids.set(3, tmp);
    JsonNode reordered = post("/courseware/projects/" + projectId + "/outline/reorder",
        Map.of("orderedIds", ids), null);
    assertEquals("0", reordered.path("code").asText());

    confirm(projectId);
    JsonNode progress = pollProgress(projectId, 90_000);
    assertEquals(9, progress.path("totalPages").asInt(), "增删抵消后仍为 9 页");
    // 编辑结果按页 id 落库(reorder 只改 position 不改 id)
    String editedTitle = jdbc.queryForObject(
        "SELECT title FROM cw_outline_page WHERE id=" + page3.path("id").asLong(), String.class);
    assertEquals("热处理基础(改)", editedTitle);
  }

  @Test
  @Order(4)
  @DisplayName("EA-04 未确认不生成:outline_ready 时无 slide、无单页 LLM 调用")
  void ea04_noGenerationBeforeConfirm() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    Integer slides = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_slide WHERE project_id=" + projectId, Integer.class);
    assertEquals(0, slides, "确认前不初始化 slide");
    assertEquals(0, slideCalls.get(), "确认前不消耗单页 token");
  }

  @Test
  @Order(5)
  @DisplayName("EA-05 自纠环 E2E:首页坏结构→该页修复重调,全部页 ready,slide 调用=页数+1")
  void ea05_repairLoop() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    nextSlideBad.set(true); // 第一页 slide 调用返回坏结构(缺 body)
    confirm(projectId);
    JsonNode progress = pollProgress(projectId, 90_000);
    assertEquals("ready", progress.path("status").asText());
    assertEquals(9, progress.path("readyPages").asInt());
    assertEquals(10, slideCalls.get(), "9 页 + 1 次修复 = 10 次单页调用");
    // 修复页最终 blocks 完整(好 JSON 有 title+body)
    Integer badPages = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cw_slide s JOIN cw_outline_page p ON s.outline_page_id=p.id"
            + " WHERE s.project_id=" + projectId + " AND s.status='ready'"
            + " AND p.page_role='content' AND s.blocks NOT LIKE '%bullets%'", Integer.class);
    assertEquals(0, badPages, "修复后内容页均含 bullets");
  }

  @Test
  @Order(6)
  @DisplayName("EA-06 单页失败隔离与重试:500 页 failed、其余 ready、retry 后 ready 且项目收口")
  void ea06_failureIsolationAndRetry() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    failTitles = List.of("硬度抽检比例不低于一成"); // 该页两次调用都 500 → failed
    confirm(projectId);
    JsonNode progress = pollProgress(projectId, 90_000);
    assertEquals("partial", progress.path("status").asText());
    assertEquals(8, progress.path("readyPages").asInt());
    assertEquals(1, progress.path("failedPages").asInt());
    Long failedSlide = jdbc.queryForObject(
        "SELECT id FROM cw_slide WHERE project_id=" + projectId + " AND status='failed'",
        Long.class);

    failTitles = List.of();
    JsonNode retry = post("/courseware/slides/" + failedSlide + "/retry", Map.of(), null);
    assertEquals("0", retry.path("code").asText());
    // 等该页 ready + 项目收口 ready
    long deadline = System.currentTimeMillis() + 30_000;
    while (System.currentTimeMillis() < deadline) {
      String status = jdbc.queryForObject(
          "SELECT status FROM cw_slide WHERE id=" + failedSlide, String.class);
      if ("ready".equals(status)) {
        break;
      }
      Thread.sleep(400);
    }
    assertEquals("ready", jdbc.queryForObject(
        "SELECT status FROM cw_slide WHERE id=" + failedSlide, String.class));
    long deadline2 = System.currentTimeMillis() + 10_000;
    String projStatus;
    do {
      projStatus = jdbc.queryForObject(
          "SELECT status FROM cw_project WHERE id=" + projectId, String.class);
      Thread.sleep(300);
    } while (!"ready".equals(projStatus) && System.currentTimeMillis() < deadline2);
    assertEquals("ready", projStatus, "重试成功后项目收口 ready");
  }

  @Test
  @Order(7)
  @DisplayName("EA-07/I-03 租户隔离:他租户读不到本项目")
  void ea07_tenantIsolation() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    HttpHeaders foreign = authHeaders();
    foreign.set("X-Tenant-Id", "999999");
    ResponseEntity<JsonNode> resp = rest.exchange("/courseware/projects/" + projectId,
        HttpMethod.GET, new HttpEntity<>(foreign), JsonNode.class);
    JsonNode body = resp.getBody();
    assertNotNull(body);
    assertTrue(List.of("401", "404").contains(body.path("code").asText()),
        "跨租户访问不应拿到数据: " + body);
    assertTrue(body.path("data").isNull() || body.path("data").isEmpty(),
        "跨租户响应不得携带项目数据");

    // I-03:MyBatis-Plus 多租户插件自动拼 tenant_id(Mapper 级)
    com.guoyu.platform.common.tenant.TenantContextHolder.setTenantId(999999L);
    try {
      assertNull(cwProjectMapper.selectById(projectId),
          "外租户 TenantContextHolder 下 selectById 应查不到本项目");
    } finally {
      com.guoyu.platform.common.tenant.TenantContextHolder.clear();
    }
  }

  @Test
  @Order(8)
  @DisplayName("EA-08 AbortPolicy:编排池占满后超额提交返回 503")
  void ea08_abortPolicy() throws Exception {
    cleanup();
    slowDigestMs = 30_000; // 占住编排线程(远超 14 次提交窗口,确保容量确定性饱和)
    int http503 = 0;
    int accepted = 0;
    List<Long> ids = new ArrayList<>();
    byte[] docx = buildTestDocx();
    for (int i = 0; i < 14; i++) {
      Long fileId = uploadDocx("热处理标准" + i + ".docx", docx);
      ResponseEntity<JsonNode> resp = rest.postForEntity("/courseware/projects",
          new HttpEntity<>(Map.of("sourceFileId", fileId, "title", "并发" + i), authHeaders()),
          JsonNode.class);
      JsonNode body = resp.getBody();
      if (resp.getStatusCode().value() == 503
          || (body != null && "503".equals(body.path("code").asText()))) {
        http503++;
      } else if (body != null && "0".equals(body.path("code").asText())) {
        accepted++;
        ids.add(body.path("data").path("id").asLong());
      } else {
        System.out.println("[E2E-DEBUG] ea08 create#" + i + " http="
            + resp.getStatusCode() + " body=" + body);
      }
    }
    slowDigestMs = 0; // 后续 digest 即刻返回,但已进入 sleep 的占位调用会睡满 30s
    assertTrue(accepted >= 10, "至少 10 个进入执行(池 2+队 10): " + accepted);
    assertTrue(http503 >= 1, "超额提交应返回 503");
    assertTrue(accepted + http503 == 14, "14 次提交全部有明确结果");
    // 等派生的项目跑完,避免污染后续用例
    for (Long id : ids) {
      try {
        pollProgressOrOutline(id, 60_000);
      } catch (Exception ignored) {
        // 收尾等待,不关心结果
      }
    }
  }

  private void pollProgressOrOutline(Long projectId, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      JsonNode data = get("/courseware/projects/" + projectId).path("data");
      String status = data.path("status").asText();
      if (List.of("outline_ready", "ready", "partial", "failed").contains(status)) {
        return;
      }
      Thread.sleep(500);
    }
  }

  @Test
  @Order(9)
  @DisplayName("EA-09 导出门禁:残留 error 页阻断导出;修复后可导出且 warning 透出")
  void ea09_exportGate() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    structureBadTitles = List.of("装炉方式影响受热均匀性"); // 两轮都缺 body → ready 但残留 error
    confirm(projectId);
    JsonNode progress = pollProgress(projectId, 90_000);
    assertEquals("ready", progress.path("status").asText());

    JsonNode exportResp = post("/courseware/projects/" + projectId + "/export", Map.of(), null);
    assertEquals("0", exportResp.path("code").asText());
    JsonNode data = exportResp.path("data");
    assertTrue(data.path("blocked").asBoolean(), "structure error 应阻断导出");
    assertTrue(data.path("issues").size() > 0);
    assertTrue(data.path("fileId").isNull(), "阻断时无 fileId");

    // 修复该页后导出成功
    structureBadTitles = List.of();
    Long badSlide = jdbc.queryForObject(
        "SELECT id FROM cw_slide WHERE project_id=" + projectId
            + " AND issues LIKE '%必填槽位%'", Long.class);
    assertEquals("0", post("/courseware/slides/" + badSlide + "/retry", Map.of(), null)
        .path("code").asText());
    long deadline = System.currentTimeMillis() + 30_000;
    while (System.currentTimeMillis() < deadline) {
      String issues = jdbc.queryForObject(
          "SELECT issues FROM cw_slide WHERE id=" + badSlide, String.class);
      if (issues != null && !issues.contains("必填槽位")) {
        break;
      }
      Thread.sleep(400);
    }
    JsonNode export2 = post("/courseware/projects/" + projectId + "/export", Map.of(), null);
    assertFalse(export2.path("data").path("blocked").asBoolean());
    assertTrue(export2.path("data").path("fileId").asLong() > 0);
  }

  @Test
  @Order(10)
  @DisplayName("EA-10a 重复确认幂等:第二次确认返回 409")
  void ea10_confirmIdempotent() throws Exception {
    cleanup();
    Long projectId = createProjectToOutlineReady();
    confirm(projectId);
    ResponseEntity<JsonNode> second = rest.postForEntity(
        "/courseware/projects/" + projectId + "/confirm",
        new HttpEntity<>(Map.of(), authHeaders()), JsonNode.class);
    assertNotNull(second.getBody());
    assertEquals("409", second.getBody().path("code").asText(), "重复确认应 409");
    pollProgress(projectId, 90_000);
  }

  @Test
  @Order(11)
  @DisplayName("U-14 权限:无权限码 403;未登录 401")
  void u14_permissionDenied() throws Exception {
    cleanup();
    // 造一个无任何角色权限的用户并直接发 Sa-Token
    jdbc.update("DELETE FROM sys_user WHERE id=2026092099001");
    jdbc.update("""
        INSERT INTO sys_user (id, tenant_id, username, password_hash, display_name, user_type,
          status, created_at, updated_at, created_by, updated_by, deleted)
        SELECT 2026092099001, 10159, 'e2e_noperm', password_hash, 'E2E无权限', 'platform',
          'enabled', NOW(), NOW(), 1, 1, 0 FROM sys_user WHERE id=1000 LIMIT 1
        """);
    JsonNode nopermLogin = post("/auth/login",
        Map.of("tenantCode", "demo", "username", "e2e_noperm", "password", "Admin@123456"),
        new HttpHeaders());
    assertEquals("0", nopermLogin.path("code").asText(), "无权限用户登录失败: " + nopermLogin);
    String token = nopermLogin.path("data").path("accessToken").asText();
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    headers.set("X-Tenant-Id", "10159");
    ResponseEntity<JsonNode> resp = rest.exchange("/courseware/projects/page",
        HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    assertNotNull(resp.getBody());
    assertEquals("403", resp.getBody().path("code").asText(), "无权限码应 403: " + resp.getBody());
    jdbc.update("DELETE FROM sys_user WHERE id=2026092099001");

    // 未带 token → 400(租户头缺失)/401/403 均可(任一前置拦截即证明未放行)
    ResponseEntity<JsonNode> noAuth = rest.getForEntity("/courseware/projects/page",
        JsonNode.class);
    assertNotNull(noAuth.getBody());
    assertTrue(List.of("400", "401", "403").contains(noAuth.getBody().path("code").asText()),
        "未认证请求应被拦截: " + noAuth.getBody());
  }

  // ── 小辅助:PUT/DELETE 包装 ──

  private JsonNode put(String path, Object body) {
    return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, authHeaders()),
        JsonNode.class).getBody();
  }

  private JsonNode delete(String path) {
    return rest.exchange(path, HttpMethod.DELETE, new HttpEntity<>(authHeaders()),
        JsonNode.class).getBody();
  }
}
