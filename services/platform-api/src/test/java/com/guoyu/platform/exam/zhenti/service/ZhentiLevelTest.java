package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiLevelTest {

  @Test
  @DisplayName("0~4 展示标签")
  void labels() {
    assertThat(ZhentiLevel.labelOf(0)).isEqualTo("五级/初级工");
    assertThat(ZhentiLevel.labelOf(1)).isEqualTo("四级/中级工");
    assertThat(ZhentiLevel.labelOf(2)).isEqualTo("三级/高级工");
    assertThat(ZhentiLevel.labelOf(3)).isEqualTo("二级/技师");
    assertThat(ZhentiLevel.labelOf(4)).isEqualTo("一级/高级技师");
  }

  @Test
  @DisplayName("空值与越界 → null")
  void labelEdgeCases() {
    assertThat(ZhentiLevel.labelOf(null)).isNull();
    assertThat(ZhentiLevel.labelOf(-1)).isNull();
    assertThat(ZhentiLevel.labelOf(5)).isNull();
  }

  @Test
  @DisplayName("parseLevel 容错：数值/数字串/空/非数字")
  void parseLevelTolerant() {
    assertThat(ZhentiLevel.parseLevel(2)).isEqualTo(2);
    assertThat(ZhentiLevel.parseLevel("3")).isEqualTo(3);
    assertThat(ZhentiLevel.parseLevel(" 1 ")).isEqualTo(1);
    assertThat(ZhentiLevel.parseLevel(null)).isNull();
    assertThat(ZhentiLevel.parseLevel("")).isNull();
    assertThat(ZhentiLevel.parseLevel("中级")).isNull();
  }
}
