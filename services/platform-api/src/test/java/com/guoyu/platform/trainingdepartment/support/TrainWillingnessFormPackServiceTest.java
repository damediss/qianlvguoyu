package com.guoyu.platform.trainingdepartment.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import org.junit.jupiter.api.Test;

class TrainWillingnessFormPackServiceTest {

  @Test
  void normalizeIdCardUppercasesTrailingX() {
    assertEquals("43112720030918001X", TrainWillingnessFormPackService.normalizeIdCard(" 43112720030918001x "));
    assertEquals("", TrainWillingnessFormPackService.normalizeIdCard("  "));
  }

  @Test
  void preferRicherSignUpKeepsRecordWithAddress() {
    TrainSignUpPeopleSelfEntity empty = new TrainSignUpPeopleSelfEntity();
    empty.setId(1L);
    empty.setName("陈潼");
    empty.setIdCardNumber("431127200309180015");

    TrainSignUpPeopleSelfEntity rich = new TrainSignUpPeopleSelfEntity();
    rich.setId(2L);
    rich.setName("陈潭");
    rich.setIdCardNumber("431127200309180015");
    rich.setAddress("湖南省蓝山县塔峰镇龙泉社区湘粤路282号1栋2单元402室");
    rich.setNation("汉");

    assertSame(rich, TrainWillingnessFormPackService.preferRicherSignUp(empty, rich));
    assertSame(rich, TrainWillingnessFormPackService.preferRicherSignUp(rich, empty));
  }
}
