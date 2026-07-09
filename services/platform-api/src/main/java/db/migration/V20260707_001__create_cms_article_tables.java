package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * App 首页 CMS 表（对齐 PHP cms_article / cms_article_category / cms_article_content / sys_index_swiper）。
 *
 * <p>gjjddb2026 若未从 PHP 全量同步，需本迁移创建表并写入首页 mock 文章（id 1-17）。
 */
public class V20260707_001__create_cms_article_tables extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    try (Statement stmt = conn.createStatement()) {
      if (!tableExists(conn, "cms_article_category")) {
        stmt.execute("""
            CREATE TABLE cms_article_category (
              category_id INT NOT NULL PRIMARY KEY,
              parent_id INT NOT NULL DEFAULT 0,
              title VARCHAR(200) NOT NULL DEFAULT '',
              deleted TINYINT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类'
            """);
      }
      if (!tableExists(conn, "cms_article")) {
        stmt.execute("""
            CREATE TABLE cms_article (
              article_id INT NOT NULL PRIMARY KEY,
              title VARCHAR(500) NOT NULL DEFAULT '',
              type TINYINT NOT NULL DEFAULT 0 COMMENT '0列表 1单页 2外链',
              category_id INT NULL,
              image VARCHAR(500) NULL,
              album TEXT NULL,
              actual_views INT NOT NULL DEFAULT 0,
              virtual_views INT NOT NULL DEFAULT 0,
              likes INT NOT NULL DEFAULT 0,
              comment_numbers INT NOT NULL DEFAULT 0,
              recommend TINYINT NOT NULL DEFAULT 0,
              sort_number INT NOT NULL DEFAULT 0,
              status TINYINT NOT NULL DEFAULT 1,
              publish_time DATETIME NULL,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              deleted TINYINT NOT NULL DEFAULT 0,
              INDEX idx_cms_article_category (category_id),
              INDEX idx_cms_article_deleted (deleted)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章'
            """);
      }
      if (!tableExists(conn, "cms_article_content")) {
        stmt.execute("""
            CREATE TABLE cms_article_content (
              article_id INT NOT NULL PRIMARY KEY,
              content MEDIUMTEXT NULL,
              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章正文'
            """);
      }
      if (!tableExists(conn, "sys_index_swiper")) {
        stmt.execute("""
            CREATE TABLE sys_index_swiper (
              id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              img VARCHAR(500) NULL,
              sort_order INT NOT NULL DEFAULT 0,
              is_show TINYINT NOT NULL DEFAULT 1,
              url VARCHAR(500) NULL,
              page_type VARCHAR(32) NOT NULL DEFAULT 'index',
              param VARCHAR(500) NULL,
              tenant_id BIGINT NOT NULL DEFAULT 10159,
              INDEX idx_swiper_page_tenant (page_type, tenant_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播'
            """);
      }

      seedCategories(stmt);
      seedArticles(stmt);
      seedSwiper(stmt);
    }
  }

  private static void seedCategories(Statement stmt) throws Exception {
    stmt.execute("""
        INSERT IGNORE INTO cms_article_category (category_id, parent_id, title, deleted) VALUES
        (258, 0, '置顶资讯', 0),
        (275, 0, '新闻', 0),
        (276, 275, '行业动态', 0),
        (277, 275, '培训动态', 0),
        (100, 0, '人物风采', 0),
        (101, 0, '政策', 0),
        (102, 0, '新闻动态', 0),
        (103, 0, '分享', 0),
        (104, 0, '平台', 0),
        (105, 0, '亮点', 0)
        """);
  }

  private static void seedArticles(Statement stmt) throws Exception {
    String[][] rows = {
        {"1", "全国技术能手 · 张三", "100", "从事铝加工行业15年，精通高端铝合金材料加工工艺，主导多项技术革新项目，获国家专利3项。", "[{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"2", "广西技术能手 · 李四", "100", "90后青年技师，专注于绿色冶金技术研究，在节能减排领域取得显著成果，获省级表彰2次。", "[{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"3", "2026冶炼工技能培训班 · 3月15日开班", "100", "本次培训班由行业资深专家授课，涵盖冶炼工艺、设备维护、安全操作等核心内容，培训周期15天，考核通过颁发结业证书。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"4", "有色金属行业技能大赛 · 报名中", "100", "大赛设铝加工、冶炼、检测等6个赛项，一等奖奖金5万元，获奖选手可直接认定为高级技师，报名截止日期4月30日", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"5", "广西技能人才振兴行动计划（2026-2030）", "101", "计划提出到2030年，全区培养高技能人才10万人以上，建立技能人才薪酬激励机制，企业培养技能人才可享受税收优惠。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"6", "有色金属行业技能等级认定新政策", "101", "简化技能等级认定流程，新增数字化运维、绿色冶金等新兴工种，企业自主评价结果与国家职业资格证书同等效力。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"7", "千铝工投技能提升平台正式上线", "102", "平台整合线上课程、线下实训、技能认定、就业对接等功能，首批上线课程86门，覆盖有色金属全产业链技能培训。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"8", "2026铝加工技能培训圆满结束", "102", "本次培训历时20天，参训学员120人，考核通过率92%，其中35人达到高级工水平，将纳入企业核心技术人才库。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"12", "从初级工到高级技师成长路", "103", "分享80后技师王某某的成长经历，从一线操作工起步，通过平台系统学习和实战锻炼，5年时间实现从初级工到高级技师的跨越。", "[{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"13", "技能考试通关经验分享", "103", "汇总多位高分通过技能等级考试学员的备考经验，包括学习方法、重点难点、实操技巧等，帮助学员高效备考。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"14", "行业数据驾驶舱", "104", "实时展示全区技能人才数量、结构、培训情况、就业分布等数据，为政府决策和企业人才培养提供数据支撑。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"15", "年度重点培训项目", "104", "2026年重点开展高端铝合金材料加工、绿色冶金技术、智能运维等6大培训项目，计划培养技能人才5000人。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"16", "技能大赛与激励机制", "105", "建立\"以赛促学、以赛促练\"的激励机制，年度技能大赛设百万奖金池，获奖选手可享受职称晋升、薪酬上浮等多项优惠。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"17", "人才成果与企校合作", "105", "平台累计培养技能人才2.8万人，其中高级工以上占比35%，与10所高校建立人才联合培养基地，就业率达98%。", "[{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"},{\"url\":\"/static/images/balance-bg.png\"}]"},
        {"1001", "首页置顶资讯示例", "258", "欢迎使用工匠基地平台，关注最新技能提升与培训动态。", "[{\"url\":\"/static/images/balance-bg.png\"}]"}
    };
    for (String[] row : rows) {
      String id = row[0];
      String title = escape(row[1]);
      String categoryId = row[2];
      String desc = escape(row[3]);
      String album = row[4];
      stmt.execute("""
          INSERT INTO cms_article
            (article_id, title, type, category_id, album, virtual_views, recommend, sort_number, status, create_time, deleted)
          SELECT %s, '%s', 0, %s, '%s', 0, 1, 0, 1, NOW(), 0
          FROM DUAL
          WHERE NOT EXISTS (SELECT 1 FROM cms_article WHERE article_id = %s)
          """.formatted(id, title, categoryId, album, id));
      stmt.execute("""
          INSERT INTO cms_article_content (article_id, content, create_time)
          SELECT %s, '<p>%s</p>', NOW()
          FROM DUAL
          WHERE NOT EXISTS (SELECT 1 FROM cms_article_content WHERE article_id = %s)
          """.formatted(id, desc, id));
    }
  }

  private static void seedSwiper(Statement stmt) throws Exception {
    stmt.execute("""
        INSERT INTO sys_index_swiper (img, sort_order, is_show, page_type, tenant_id)
        SELECT './static/images/logo.jpg', 1, 1, 'index', 10159 FROM DUAL
        WHERE NOT EXISTS (
          SELECT 1 FROM sys_index_swiper WHERE page_type = 'index' AND tenant_id = 10159
        )
        """);
    stmt.execute("""
        INSERT INTO sys_index_swiper (img, sort_order, is_show, page_type, tenant_id)
        SELECT './static/images/brushing.jpg', 2, 1, 'index', 10159 FROM DUAL
        WHERE (SELECT COUNT(*) FROM sys_index_swiper WHERE page_type = 'index' AND tenant_id = 10159) < 2
        """);
  }

  private static String escape(String value) {
    return value.replace("'", "''");
  }

  private static boolean tableExists(Connection conn, String table) throws Exception {
    DatabaseMetaData meta = conn.getMetaData();
    String catalog = conn.getCatalog();
    try (ResultSet rs = meta.getTables(catalog, null, table, new String[]{"TABLE"})) {
      if (rs.next()) return true;
    }
    try (ResultSet rs = meta.getTables(catalog, null, table.toUpperCase(), new String[]{"TABLE"})) {
      return rs.next();
    }
  }
}
