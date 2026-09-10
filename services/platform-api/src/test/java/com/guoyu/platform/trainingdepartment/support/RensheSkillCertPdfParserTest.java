package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

class RensheSkillCertPdfParserTest {

  @Test
  void parseLuyifengSamplePdf_faceFieldsExact() throws Exception {
    byte[] bytes;
    try (InputStream in = getClass().getResourceAsStream("/certificates/luyifeng_skill_cert.pdf")) {
      assertThat(in).as("test resource certificates/luyifeng_skill_cert.pdf").isNotNull();
      bytes = in.readAllBytes();
    }
    RensheSkillCertPdfParser.Parsed p =
        RensheSkillCertPdfParser.parse(
            bytes, "S001845001003253000367_陆义逢_1000000001554965.pdf");

    assertThat(p.name).isEqualTo("陆义逢");
    assertThat(p.idDocType).isEqualTo("居民身份证");
    assertThat(p.idCard).isEqualTo("452629199002120631");
    assertThat(p.certificateNo).isEqualTo("S001845001003253000367");
    assertThat(p.issuingAuthority).isEqualTo("中国有色金属工业协会");
    assertThat(p.occupation).isEqualTo("铝电解工");
    assertThat(p.workType).isEqualTo("炭极生产工");
    assertThat(p.level).isEqualTo("三级/高级工");
    assertThat(p.evaluationOrg).isEqualTo("广西千铝工投教育科技发展有限公司");
    assertThat(p.issueDate).isEqualTo("2025-09-15");
    assertThat(p.levelNormalized).isEqualTo("三级/高级工");
    assertThat(p.queryUrls).isNotEmpty();
    assertThat(p.queryUrls.get(0)).contains("osta.org");
  }

  @Test
  void parseText_doesNotInventLevelFromPartialKeyword() {
    String text =
        """
        张三
        居民身份证
        110101199001011234
        S001845001003253000999
        本证书由      某协会      颁发，
        __________________________
        某职业
        某工种
        三级
        某评价机构
        2024年01月02日
        """;
    RensheSkillCertPdfParser.Parsed p =
        RensheSkillCertPdfParser.parseText(text, "S001845001003253000999_张三_1.pdf");
    assertThat(p.level).isEqualTo("三级");
    assertThat(p.level).isNotEqualTo("三级/高级工");
    assertThat(p.levelNormalized).isEqualTo("三级/高级工");
    assertThat(p.issueDate).isEqualTo("2024-01-02");
    assertThat(p.occupation).isEqualTo("某职业");
    assertThat(p.workType).isEqualTo("某工种");
  }

  @Test
  void parseText_noDefaultIdDocType() {
    String text =
        """
        李四
        11010119900101123X
        S001845001003253000888
        本证书由 测试机构 颁发，
        __________________________
        职业A
        工种B
        二级/技师
        评价机构C
        2023年12月01日
        """;
    RensheSkillCertPdfParser.Parsed p =
        RensheSkillCertPdfParser.parseText(text, "S001845001003253000888_李四_1.pdf");
    assertThat(p.idDocType).isNull();
    assertThat(p.level).isEqualTo("二级/技师");
    assertThat(p.issuingAuthority).isEqualTo("测试机构");
  }
}
