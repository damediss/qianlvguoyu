package com.guoyu.platform.textbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.textbook.domain.TextbookDirectionResolver;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 方向→职业功能序号解析（设计 §4.1-TextbookDirectionResolverTest）。
 */
class TextbookDirectionResolverTest {

  /** 铝电解工 3.1 五级/初级工正文首段原文（v1.0 功能方案 §2.1 对拍基准） */
  private static final String L5_INTRO =
      "本级别 2~ 11 项职业功能为可选项。铝电解槽操作工考核职业功能 1、2、12 项;"
          + "铝吸出工考核职业功能 1、3、 12 项;多功能机组操作工考核职业功能 1、4、12 项;"
          + "物料输送及烟气净化工考核职业功能 1、5、12 项;电解槽计算机监控工考核职业功能 1、6、12 项;"
          + "铝及铝合金熔铸工考核职业功能 1、7、12 项;炭极生产工考核职业功能 1、8、12 项;"
          + "物料分析检测工考核职业功能 1、9、 12 项;铝电解筑炉工考核职业功能 1、10、12 项;"
          + "铝电解综合工考核职业功能 1、11、12 项。";

  @Test
  void lv10DirectionMappingMatchesV10Spec() {
    StandardLevelDoc level = levelWithIntro(L5_INTRO, 12);
    Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(level,
        List.of("默认"));

    assertEquals(10, mapping.size(), "铝电解工应解析出 10 个方向：" + mapping.keySet());
    assertEquals(List.of(1, 2, 12), mapping.get("铝电解槽操作工"));
    assertEquals(List.of(1, 3, 12), mapping.get("铝吸出工"));
    assertEquals(List.of(1, 4, 12), mapping.get("多功能机组操作工"));
    assertEquals(List.of(1, 5, 12), mapping.get("物料输送及烟气净化工"));
    assertEquals(List.of(1, 6, 12), mapping.get("电解槽计算机监控工"));
    assertEquals(List.of(1, 7, 12), mapping.get("铝及铝合金熔铸工"));
    assertEquals(List.of(1, 8, 12), mapping.get("炭极生产工"));
    assertEquals(List.of(1, 9, 12), mapping.get("物料分析检测工"));
    assertEquals(List.of(1, 10, 12), mapping.get("铝电解筑炉工"));
    assertEquals(List.of(1, 11, 12), mapping.get("铝电解综合工"));
  }

  @Test
  void toleratesSpacingAndFullWidthVariants() {
    StandardLevelDoc noSpace = levelWithIntro("铝电解槽操作工考核职业功能1、2、12项", 12);
    assertEquals(List.of(1, 2, 12),
        TextbookDirectionResolver.resolve(noSpace, List.of("默认")).get("铝电解槽操作工"));

    StandardLevelDoc fullWidthComma = levelWithIntro("铝吸出工考核职业功能 1，3，12 项", 12);
    assertEquals(List.of(1, 3, 12),
        TextbookDirectionResolver.resolve(fullWidthComma, List.of("默认")).get("铝吸出工"));

    StandardLevelDoc fullWidthDigit = levelWithIntro("铝电解槽操作工考核职业功能 １、２、１２ 项", 12);
    assertEquals(List.of(1, 2, 12),
        TextbookDirectionResolver.resolve(fullWidthDigit, List.of("默认")).get("铝电解槽操作工"));
  }

  @Test
  void returnsEmptyWhenUnparseableOnMultiDirectionStandard() {
    StandardLevelDoc level = levelWithIntro("本级别各职业功能均为必考内容。", 12);
    Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(level,
        List.of("电焊工", "气焊工"));
    assertTrue(mapping.isEmpty(), "多方向标准解析不出应返回空（人工登记兜底）");
  }

  @Test
  void singleDirectionWorkTypeEnablesAllFunctions() {
    StandardLevelDoc level = levelWithIntro("", 5);
    Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(level,
        List.of(ParsedStandard.DEFAULT_DIRECTION));
    assertEquals(1, mapping.size());
    assertEquals(List.of(1, 2, 3, 4, 5), mapping.get(ParsedStandard.DEFAULT_DIRECTION));
  }

  /** 真实 docx 端到端对拍：解析器抓出的 3.1 首段应解析出与 §2.1 相同的 10 方向映射 */
  @Test
  void realDocxIntroParsesToSameMapping() throws Exception {
    ParsedStandard parsed;
    try (InputStream in = getClass().getResourceAsStream("/standard/铝电解工.docx")) {
      assertTrue(in != null, "测试资源缺失：standard/铝电解工.docx");
      parsed = new DocxStandardParser().parse(in);
    }
    StandardLevelDoc five = parsed.levelOf(0);
    Map<String, List<Integer>> mapping = TextbookDirectionResolver.resolve(five,
        parsed.getDirections());
    assertEquals(10, mapping.size(), "真实 docx 解析映射应含 10 个方向：" + mapping.keySet());
    assertEquals(List.of(1, 2, 12), mapping.get("铝电解槽操作工"));
    assertEquals(List.of(1, 11, 12), mapping.get("铝电解综合工"));
  }

  private static StandardLevelDoc levelWithIntro(String intro, int functionCount) {
    StandardLevelDoc level = new StandardLevelDoc();
    level.setLevelCode(0);
    level.setLevelName("五级/初级工");
    if (intro != null && !intro.isBlank()) {
      level.getIntroParagraphs().add(intro);
    }
    for (int i = 0; i < functionCount; i++) {
      StandardNodeDoc func = new StandardNodeDoc();
      func.setLevel(1);
      func.setNumber(String.valueOf(i + 1));
      func.setTitle("职业功能" + (i + 1));
      level.getWorkFunctions().add(func);
    }
    return level;
  }
}
