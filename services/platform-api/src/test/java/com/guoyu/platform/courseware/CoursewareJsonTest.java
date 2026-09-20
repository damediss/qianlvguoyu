package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareJson;
import org.junit.jupiter.api.Test;

/** JSON 宽容提取(输出零信任) */
class CoursewareJsonTest {

  private final ObjectMapper om = new ObjectMapper();

  @Test
  void extractsPlainObject() {
    assertEquals(1, CoursewareJson.extractObject("{\"a\":1}", om).path("a").asInt());
  }

  @Test
  void stripsMarkdownFence() {
    assertEquals(2, CoursewareJson.extractObject("```json\n{\"a\":2}\n```", om).path("a").asInt());
  }

  @Test
  void skipsSurroundingProse() {
    assertEquals(3, CoursewareJson.extractObject("好的,结果如下:{\"a\":3} 以上是结果", om)
        .path("a").asInt());
  }

  @Test
  void truncatedThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> CoursewareJson.extractObject("{\"a\":{\"b\":1}", om));
  }

  @Test
  void noObjectThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> CoursewareJson.extractObject("纯文本没有 JSON", om));
  }

  @Test
  void stringWithBracesNotCounted() {
    assertEquals("a{b}c",
        CoursewareJson.extractObject("{\"x\":\"a{b}c\"}", om).path("x").asText());
  }
}
