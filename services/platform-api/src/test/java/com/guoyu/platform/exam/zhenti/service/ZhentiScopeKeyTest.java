package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiScopeKeyTest {

  @Test
  @DisplayName("共享组键与私有键格式")
  void keyFormats() {
    assertThat(ZhentiScopeKey.of(new ZhentiScope(35L, 1))).isEqualTo("work:35:1");
    assertThat(ZhentiScopeKey.ofExam(441L)).isEqualTo("exam:441");
    assertThat(ZhentiScopeKey.ofScopeOrExam(new ZhentiScope(76L, 0), 441L)).isEqualTo("work:76:0");
    assertThat(ZhentiScopeKey.ofScopeOrExam(null, 441L)).isEqualTo("exam:441");
  }

  @Test
  @DisplayName("共享组展示标签")
  void label() {
    assertThat(ZhentiScopeKey.label(new ZhentiScope(35L, 1), "铝及铝合金熔铸工"))
        .isEqualTo("铝及铝合金熔铸工 · 四级/中级工");
  }
}
