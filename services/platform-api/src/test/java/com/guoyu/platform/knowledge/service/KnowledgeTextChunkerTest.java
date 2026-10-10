package com.guoyu.platform.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeTextChunkerTest {

  private final KnowledgeTextChunker chunker = new KnowledgeTextChunker();

  @Test
  void chunkKeepsHeadingAndSplitsLongText() {
    String text = """
        # 电工安全

        第一段说明电工安全操作要求和停送电流程。

        第二段说明考试前检查、工具确认和异常上报。

        第三段说明实操培训记录和复盘要求。
        """;

    List<KnowledgeTextChunk> chunks = chunker.chunk(text, 40, 8);

    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks.getFirst().heading()).isEqualTo("电工安全");
    assertThat(chunks).allMatch(chunk -> chunk.content().contains("电工") || chunk.content().length() > 0);
  }
}
