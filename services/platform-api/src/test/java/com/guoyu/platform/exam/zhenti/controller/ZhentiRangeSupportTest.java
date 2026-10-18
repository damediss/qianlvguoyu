package com.guoyu.platform.exam.zhenti.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiRangeSupportTest {

  private static final long SIZE = 1000;

  @Test
  @DisplayName("bytes=0-99 → 206 区间 0..99，长度 100")
  void firstHundredBytes() {
    var range = ZhentiFileController.parseRange("bytes=0-99", SIZE);
    assertThat(range).isNotNull();
    assertThat(range.start()).isZero();
    assertThat(range.end()).isEqualTo(99);
    assertThat(range.length()).isEqualTo(100);
  }

  @Test
  @DisplayName("无 Range → null（200 全量）")
  void noRange() {
    assertThat(ZhentiFileController.parseRange(null, SIZE)).isNull();
    assertThat(ZhentiFileController.parseRange("", SIZE)).isNull();
    assertThat(ZhentiFileController.parseRange("items=0-99", SIZE)).isNull();
  }

  @Test
  @DisplayName("越界 range → 416")
  void outOfRange() {
    assertThatThrownBy(() -> ZhentiFileController.parseRange("bytes=1000-1200", SIZE))
        .isInstanceOf(ZhentiFileController.UnsatisfiableRangeException.class);
    assertThatThrownBy(() -> ZhentiFileController.parseRange("bytes=500-400", SIZE))
        .isInstanceOf(ZhentiFileController.UnsatisfiableRangeException.class);
  }

  @Test
  @DisplayName("开放区间 bytes=500- → 到文件末尾")
  void openEnded() {
    var range = ZhentiFileController.parseRange("bytes=500-", SIZE);
    assertThat(range.start()).isEqualTo(500);
    assertThat(range.end()).isEqualTo(999);
    assertThat(range.length()).isEqualTo(500);
  }

  @Test
  @DisplayName("后缀区间 bytes=-100 → 最后 100 字节")
  void suffixRange() {
    var range = ZhentiFileController.parseRange("bytes=-100", SIZE);
    assertThat(range.start()).isEqualTo(900);
    assertThat(range.end()).isEqualTo(999);
    assertThat(range.length()).isEqualTo(100);
  }

  @Test
  @DisplayName("多段 Range 不支持 → 退回 200 全量")
  void multiRangeFallsBack() {
    assertThat(ZhentiFileController.parseRange("bytes=0-99,200-299", SIZE)).isNull();
  }

  @Test
  @DisplayName("非法语法按无 Range 处理")
  void malformedIgnored() {
    assertThat(ZhentiFileController.parseRange("bytes=abc-def", SIZE)).isNull();
  }
}
