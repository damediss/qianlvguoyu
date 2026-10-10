package com.guoyu.platform.app.home.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.app.home.entity.CmsArticleCategoryEntity;
import com.guoyu.platform.app.home.entity.CmsArticleContentEntity;
import com.guoyu.platform.app.home.entity.CmsArticleEntity;
import com.guoyu.platform.app.home.entity.SysIndexSwiperEntity;
import com.guoyu.platform.app.home.mapper.CmsArticleCategoryMapper;
import com.guoyu.platform.app.home.mapper.CmsArticleContentMapper;
import com.guoyu.platform.app.home.mapper.CmsArticleMapper;
import com.guoyu.platform.app.home.mapper.SysIndexSwiperMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * App 首页接口（对齐 PHP ApphomeController）。
 *
 * <p>复用旧库表：cms_article / cms_article_content / cms_article_category / sys_index_swiper。
 */
@Service
@Slf4j
public class AppHomeApplicationService {

  /** 资讯分类 parent_id（PHP 端硬编码 275，指"新闻"大类下的子分类） */
  private static final int NEWS_PARENT_CATEGORY_ID = 275;
  /** 首页置顶资讯分类 id（PHP 端硬编码 258） */
  private static final int NEWS_ZX_CATEGORY_ID = 258;

  private final CmsArticleMapper articleMapper;
  private final CmsArticleContentMapper contentMapper;
  private final CmsArticleCategoryMapper categoryMapper;
  private final SysIndexSwiperMapper swiperMapper;
  private final ObjectMapper objectMapper;
  private final long defaultTenantId;
  private final String cdnUrl;

  public AppHomeApplicationService(
      CmsArticleMapper articleMapper,
      CmsArticleContentMapper contentMapper,
      CmsArticleCategoryMapper categoryMapper,
      SysIndexSwiperMapper swiperMapper,
      ObjectMapper objectMapper,
      @Value("${platform.app.default-tenant-id:1}") long defaultTenantId,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.articleMapper = articleMapper;
    this.contentMapper = contentMapper;
    this.categoryMapper = categoryMapper;
    this.swiperMapper = swiperMapper;
    this.objectMapper = objectMapper;
    this.defaultTenantId = defaultTenantId;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  // ============================================================
  // 1. 首页聚合：轮播 + 置顶资讯 + 文章列表
  // ============================================================
  public Map<String, Object> index() {
    Map<String, Object> data = new LinkedHashMap<>();

    data.put("lunbo", loadIndexLunbo());

    // 置顶资讯（最新一条）
    CmsArticleEntity newsZx = articleMapper.selectOne(
        new LambdaQueryWrapper<CmsArticleEntity>()
            .eq(CmsArticleEntity::getDeleted, 0)
            .eq(CmsArticleEntity::getCategoryId, NEWS_ZX_CATEGORY_ID)
            .orderByDesc(CmsArticleEntity::getCreateTime)
            .last("LIMIT 1"));
    Map<String, Object> newsZxMap = null;
    if (newsZx != null) {
      newsZxMap = new LinkedHashMap<>();
      newsZxMap.put("article_id", newsZx.getArticleId());
      newsZxMap.put("title", newsZx.getTitle());
    }
    data.put("newsZX", newsZxMap);

    // 文章列表（新闻大类下的所有子分类）
    List<CmsArticleCategoryEntity> subCats = categoryMapper.selectList(
        new LambdaQueryWrapper<CmsArticleCategoryEntity>()
            .eq(CmsArticleCategoryEntity::getParentId, NEWS_PARENT_CATEGORY_ID));
    Map<Integer, String> catTitle = subCats.stream()
        .collect(Collectors.toMap(CmsArticleCategoryEntity::getCategoryId,
            CmsArticleCategoryEntity::getTitle, (a, b) -> a));
    List<Map<String, Object>> articleList = List.of();
    if (!catTitle.isEmpty()) {
      List<CmsArticleEntity> articles = articleMapper.selectList(
          new LambdaQueryWrapper<CmsArticleEntity>()
              .eq(CmsArticleEntity::getDeleted, 0)
              .in(CmsArticleEntity::getCategoryId, catTitle.keySet())
              .orderByDesc(CmsArticleEntity::getRecommend)
              .orderByAsc(CmsArticleEntity::getSortNumber)
              .orderByDesc(CmsArticleEntity::getCreateTime));
      articleList = articles.stream().map(a -> toArticleListMap(a, catTitle)).toList();
    }
    data.put("articleList", articleList);
    return data;
  }

  // ============================================================
  // 2. 首页 tabs（静态标签，对齐 PHP）
  // ============================================================
  public Map<String, Object> tabs() {
    List<Map<String, Object>> tabs = new ArrayList<>();
    tabs.add(tab("发现", "tuijian"));
    tabs.add(tab("政策", "zhengce"));
    tabs.add(tab("新闻", "news"));
    tabs.add(tab("平台", "platform"));
    tabs.add(tab("分享", "share"));
    tabs.add(tab("亮点", "liangdian"));
    return Map.of("tabs", tabs);
  }

  // ============================================================
  // 3. 首页内容（对齐 PHP homeAction：一次返回全部 tab 数据）
  // ============================================================
  public Map<String, Object> home(String tag) {
    Map<String, Object> article = new LinkedHashMap<>();

    Map<String, Object> tuijian = new LinkedHashMap<>();
    tuijian.put("ren", homeSection("人物风采",
        mockHomeArticle(1, "全国技术能手 · 张三",
            "从事铝加工行业15年，精通高端铝合金材料加工工艺，主导多项技术革新项目，获国家专利3项。",
            "/static/images/balance-bg.png"),
        mockHomeArticle(2, "广西技术能手 · 李四",
            "90后青年技师，专注于绿色冶金技术研究，在节能减排领域取得显著成果，获省级表彰2次。",
            "/static/images/balance-bg.png")));
    tuijian.put("exammatch", homeSection("最新开班/考试/竞赛",
        mockHomeArticle(3, "2026冶炼工技能培训班 · 3月15日开班",
            "本次培训班由行业资深专家授课，涵盖冶炼工艺、设备维护、安全操作等核心内容，培训周期15天，考核通过颁发结业证书。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"),
        mockHomeArticle(4, "有色金属行业技能大赛 · 报名中",
            "大赛设铝加工、冶炼、检测等6个赛项，一等奖奖金5万元，获奖选手可直接认定为高级技师，报名截止日期4月30日",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png")));
    article.put("tuijian", tuijian);

    article.put("zhengce", homeSection("政策简介与动态",
        mockHomeArticle(5, "广西技能人才振兴行动计划（2026-2030）",
            "计划提出到2030年，全区培养高技能人才10万人以上，建立技能人才薪酬激励机制，企业培养技能人才可享受税收优惠。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"),
        mockHomeArticle(6, "有色金属行业技能等级认定新政策",
            "简化技能等级认定流程，新增数字化运维、绿色冶金等新兴工种，企业自主评价结果与国家职业资格证书同等效力。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png")));

    article.put("news", homeSection("平台技能提升动态",
        mockHomeArticle(7, "千铝工投技能提升平台正式上线",
            "平台整合线上课程、线下实训、技能认定、就业对接等功能，首批上线课程86门，覆盖有色金属全产业链技能培训。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"),
        mockHomeArticle(8, "2026铝加工技能培训圆满结束",
            "本次培训历时20天，参训学员120人，考核通过率92%，其中35人达到高级工水平，将纳入企业核心技术人才库。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png")));

    article.put("share", homeSection("成长故事与分享",
        mockHomeArticle(12, "从初级工到高级技师成长路",
            "分享80后技师王某某的成长经历，从一线操作工起步，通过平台系统学习和实战锻炼，5年时间实现从初级工到高级技师的跨越。",
            "/static/images/balance-bg.png"),
        mockHomeArticle(13, "技能考试通关经验分享",
            "汇总多位高分通过技能等级考试学员的备考经验，包括学习方法、重点难点、实操技巧等，帮助学员高效备考。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png")));

    article.put("platform", homeSection("平台风采",
        mockHomeArticle(14, "行业数据驾驶舱",
            "实时展示全区技能人才数量、结构、培训情况、就业分布等数据，为政府决策和企业人才培养提供数据支撑。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png"),
        mockHomeArticle(15, "年度重点培训项目",
            "2026年重点开展高端铝合金材料加工、绿色冶金技术、智能运维等6大培训项目，计划培养技能人才5000人。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png")));

    article.put("liangdian", homeSection("亮点",
        mockHomeArticle(16, "技能大赛与激励机制",
            "建立\"以赛促学、以赛促练\"的激励机制，年度技能大赛设百万奖金池，获奖选手可享受职称晋升、薪酬上浮等多项优惠。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png"),
        mockHomeArticle(17, "人才成果与企校合作",
            "平台累计培养技能人才2.8万人，其中高级工以上占比35%，与10所高校建立人才联合培养基地，就业率达98%。",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
            "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png")));

    Map<String, Object> home = new LinkedHashMap<>();
    home.put("lunbo", loadHomeLunbo());
    home.put("article", article);
    return Map.of("home", home);
  }

  // ============================================================
  // 4. 搜索文章
  // ============================================================
  public Map<String, Object> search(String keyword, int page) {
    int current = page > 0 ? page : 1;
    int size = 10;
    int offset = (current - 1) * size;

    LambdaQueryWrapper<CmsArticleEntity> q = new LambdaQueryWrapper<CmsArticleEntity>()
        .eq(CmsArticleEntity::getDeleted, 0)
        .orderByDesc(CmsArticleEntity::getCreateTime)
        .last("LIMIT " + size + " OFFSET " + offset);
    if (StringUtils.hasText(keyword)) {
      q.like(CmsArticleEntity::getTitle, keyword.trim());
    }
    List<CmsArticleEntity> articles = articleMapper.selectList(q);

    // 分类标题
    List<Integer> cids = articles.stream()
        .map(CmsArticleEntity::getCategoryId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    Map<Integer, String> catTitle = loadCategoryTitles(cids);

    List<Map<String, Object>> lists = articles.stream()
        .map(a -> toArticleListMap(a, catTitle)).toList();
    return Map.of("lists", lists);
  }

  // ============================================================
  // 5. 文章详情
  // ============================================================
  public Map<String, Object> articleInfo(Integer articleId) {
    if (articleId == null || articleId <= 0) {
      throw new AppSmsException(1, "参数不合法");
    }
    CmsArticleEntity article = null;
    CmsArticleContentEntity content = null;
    try {
      article = articleMapper.selectById(articleId);
      if (article != null && (article.getDeleted() == null || article.getDeleted() == 0)) {
        content = contentMapper.selectById(articleId);
      } else {
        article = null;
      }
    } catch (Exception e) {
      log.warn("查询文章详情失败 articleId={}: {}", articleId, e.getMessage());
    }
    if (article == null) {
      Map<String, Object> mock = buildMockArticleDetail(articleId);
      if (mock != null) {
        return Map.of("article", mock);
      }
      throw new AppSmsException(1, "当前新闻不存在");
    }
    CmsArticleCategoryEntity cat = article.getCategoryId() != null
        ? categoryMapper.selectById(article.getCategoryId()) : null;

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("article_id", article.getArticleId());
    result.put("title", article.getTitle());
    result.put("category", cat != null && StringUtils.hasText(cat.getTitle()) ? cat.getTitle() : "资讯");
    result.put("content", content != null ? content.getContent() : "");
    result.put("create_time", article.getCreateTime());
    result.put("virtual_views", article.getVirtualViews() != null ? article.getVirtualViews() : 0);
    result.put("album", parseAlbum(article.getAlbum()));
    return Map.of("article", result);
  }

  // ============================================================
  // helpers
  // ============================================================

  private Map<Integer, String> loadCategoryTitles(List<Integer> cids) {
    if (cids.isEmpty()) return Map.of();
    List<CmsArticleCategoryEntity> cats = categoryMapper.selectList(
        new LambdaQueryWrapper<CmsArticleCategoryEntity>()
            .in(CmsArticleCategoryEntity::getCategoryId, cids));
    return cats.stream().collect(Collectors.toMap(
        CmsArticleCategoryEntity::getCategoryId,
        CmsArticleCategoryEntity::getTitle, (a, b) -> a));
  }

  private Map<String, Object> toArticleListMap(CmsArticleEntity a, Map<Integer, String> catTitle) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", a.getArticleId());
    m.put("article_id", a.getArticleId());
    m.put("title", a.getTitle());
    m.put("category_id", a.getCategoryId());
    m.put("category", catTitle.getOrDefault(a.getCategoryId(), "-"));
    m.put("album", parseAlbum(a.getAlbum()));
    m.put("image", toAbsoluteUrl(a.getImage()));
    m.put("create_time", a.getCreateTime());
    m.put("virtual_views", a.getVirtualViews());
    m.put("recommend", a.getRecommend());
    m.put("sort_number", a.getSortNumber());
    return m;
  }

  /** 前端 index.vue 期望 album 为 [{url: "..."}] 结构 */
  private List<Map<String, Object>> parseAlbum(String albumJson) {
    if (!StringUtils.hasText(albumJson)) return List.of();
    try {
      JsonNode node = objectMapper.readTree(albumJson);
      if (!node.isArray()) return List.of();
      List<Map<String, Object>> list = new ArrayList<>();
      node.forEach(n -> {
        String url = null;
        if (n.isObject() && n.has("url")) {
          url = n.get("url").asText();
        } else if (n.isTextual()) {
          url = n.asText();
        }
        if (StringUtils.hasText(url)) {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("url", toAbsoluteUrl(url));
          list.add(item);
        }
      });
      return list;
    } catch (Exception e) {
      log.warn("解析 album JSON 失败: {}", albumJson, e);
      return List.of();
    }
  }

  private List<Map<String, Object>> loadIndexLunbo() {
    try {
      List<SysIndexSwiperEntity> swipers = swiperMapper.selectList(
          new LambdaQueryWrapper<SysIndexSwiperEntity>()
              .eq(SysIndexSwiperEntity::getPageType, "index")
              .eq(SysIndexSwiperEntity::getTenantId, defaultTenantId)
              .orderByAsc(SysIndexSwiperEntity::getSortOrder));
      return swipers.stream()
          .map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("img", toAbsoluteUrl(s.getImg()));
            m.put("url", s.getUrl());
            return m;
          })
          .toList();
    } catch (Exception e) {
      log.warn("加载首页轮播失败，使用空列表: {}", e.getMessage());
      return List.of();
    }
  }

  /** home.vue 轮播：优先 DB，无数据时对齐 PHP 默认图 */
  private List<Map<String, Object>> loadHomeLunbo() {
    List<Map<String, Object>> fromDb = loadIndexLunbo().stream()
        .map(s -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("img", s.get("img"));
          return m;
        })
        .toList();
    if (!fromDb.isEmpty()) {
      return fromDb;
    }
    List<Map<String, Object>> fallback = new ArrayList<>();
    fallback.add(Map.of("img", "./static/images/logo.jpg"));
    fallback.add(Map.of("img", "./static/images/brushing.jpg"));
    return fallback;
  }

  private static Map<String, Object> homeSection(String ctitle, Map<String, Object>... items) {
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("ctitle", ctitle);
    section.put("list", List.of(items));
    return section;
  }

  private static Map<String, Object> mockHomeArticle(
      int id, String title, String desc, String... imgs) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", id);
    item.put("title", title);
    item.put("desc", desc);
    item.put("img", List.of(imgs));
    return item;
  }

  /** 首页 mock 文章详情（表不存在或未迁移时的兜底） */
  private Map<String, Object> buildMockArticleDetail(int articleId) {
    String[] row = mockArticleMeta().get(articleId);
    if (row == null) {
      return null;
    }
    List<Map<String, Object>> album = new ArrayList<>();
    for (int i = 3; i < row.length; i++) {
      Map<String, Object> img = new LinkedHashMap<>();
      img.put("url", row[i]);
      album.add(img);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("article_id", articleId);
    result.put("title", row[0]);
    result.put("category", row[2]);
    result.put("content", "<p>" + row[1] + "</p>");
    result.put("create_time", java.time.LocalDateTime.now());
    result.put("virtual_views", 0);
    result.put("album", album);
    return result;
  }

  private static Map<Integer, String[]> mockArticleMeta() {
    Map<Integer, String[]> map = new LinkedHashMap<>();
    map.put(1, new String[]{"全国技术能手 · 张三",
        "从事铝加工行业15年，精通高端铝合金材料加工工艺，主导多项技术革新项目，获国家专利3项。",
        "人物风采", "/static/images/balance-bg.png"});
    map.put(2, new String[]{"广西技术能手 · 李四",
        "90后青年技师，专注于绿色冶金技术研究，在节能减排领域取得显著成果，获省级表彰2次。",
        "人物风采", "/static/images/balance-bg.png"});
    map.put(3, new String[]{"2026冶炼工技能培训班 · 3月15日开班",
        "本次培训班由行业资深专家授课，涵盖冶炼工艺、设备维护、安全操作等核心内容，培训周期15天，考核通过颁发结业证书。",
        "最新开班/考试/竞赛", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(4, new String[]{"有色金属行业技能大赛 · 报名中",
        "大赛设铝加工、冶炼、检测等6个赛项，一等奖奖金5万元，获奖选手可直接认定为高级技师，报名截止日期4月30日",
        "最新开班/考试/竞赛", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(5, new String[]{"广西技能人才振兴行动计划（2026-2030）",
        "计划提出到2030年，全区培养高技能人才10万人以上，建立技能人才薪酬激励机制，企业培养技能人才可享受税收优惠。",
        "政策简介与动态", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(6, new String[]{"有色金属行业技能等级认定新政策",
        "简化技能等级认定流程，新增数字化运维、绿色冶金等新兴工种，企业自主评价结果与国家职业资格证书同等效力。",
        "政策简介与动态", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(7, new String[]{"千铝工投技能提升平台正式上线",
        "平台整合线上课程、线下实训、技能认定、就业对接等功能，首批上线课程86门，覆盖有色金属全产业链技能培训。",
        "平台技能提升动态", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(8, new String[]{"2026铝加工技能培训圆满结束",
        "本次培训历时20天，参训学员120人，考核通过率92%，其中35人达到高级工水平，将纳入企业核心技术人才库。",
        "平台技能提升动态", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(12, new String[]{"从初级工到高级技师成长路",
        "分享80后技师王某某的成长经历，从一线操作工起步，通过平台系统学习和实战锻炼，5年时间实现从初级工到高级技师的跨越。",
        "成长故事与分享", "/static/images/balance-bg.png"});
    map.put(13, new String[]{"技能考试通关经验分享",
        "汇总多位高分通过技能等级考试学员的备考经验，包括学习方法、重点难点、实操技巧等，帮助学员高效备考。",
        "成长故事与分享", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(14, new String[]{"行业数据驾驶舱",
        "实时展示全区技能人才数量、结构、培训情况、就业分布等数据，为政府决策和企业人才培养提供数据支撑。",
        "平台风采", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(15, new String[]{"年度重点培训项目",
        "2026年重点开展高端铝合金材料加工、绿色冶金技术、智能运维等6大培训项目，计划培养技能人才5000人。",
        "平台风采", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(16, new String[]{"技能大赛与激励机制",
        "建立\"以赛促学、以赛促练\"的激励机制，年度技能大赛设百万奖金池，获奖选手可享受职称晋升、薪酬上浮等多项优惠。",
        "亮点", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    map.put(17, new String[]{"人才成果与企校合作",
        "平台累计培养技能人才2.8万人，其中高级工以上占比35%，与10所高校建立人才联合培养基地，就业率达98%。",
        "亮点", "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png",
        "/static/images/balance-bg.png", "/static/images/balance-bg.png", "/static/images/balance-bg.png"});
    return map;
  }

  private String toAbsoluteUrl(String path) {
    if (!StringUtils.hasText(path)) return path;
    if (path.startsWith("http://") || path.startsWith("https://")) return path;
    // PHP 端相对路径 ./static/... 直接返回，CDN 上的 /api/exam/files 前缀
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return cdnUrl + "/" + normalized;
  }

  private static Map<String, Object> tab(String title, String tag) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("title", title);
    m.put("tag", tag);
    return m;
  }
}
