package com.guoyu.platform.industrial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.industrial.dto.ProjectResourceQueryReq;
import org.junit.jupiter.api.Test;

class ProjectResourceApplicationServiceTest {

  private final ProjectResourceApplicationService service = new ProjectResourceApplicationService();

  @Test
  void courseResourcesContainPreviewFieldsAndFixedBusinessValues() {
    ProjectResourceQueryReq req = new ProjectResourceQueryReq();
    req.setPageSize(10);

    var result = service.page("course", req);

    assertThat(result.total()).isEqualTo(10);
    assertThat(result.items()).hasSize(10);
    assertThat(result.items().getFirst().date()).isEqualTo("2025-06-10");
    assertThat(result.items().getFirst().name()).startsWith("铝电解槽操作工");
    assertThat(result.items().getFirst().state()).isEqualTo("通过");
    assertThat(result.items().getFirst().source()).isEqualTo("千铝工投");
    assertThat(result.items().getFirst().preview()).contains("/upload/common/20250810/");
    assertThat(result.items().getFirst().fileType()).isEqualTo("pptx");
  }

  @Test
  void examResourcesDoNotExposePreviewFields() {
    ProjectResourceQueryReq req = new ProjectResourceQueryReq();

    var result = service.page("exam", req);

    assertThat(result.items()).allSatisfy(item -> {
      assertThat(item.preview()).isNull();
      assertThat(item.fileType()).isNull();
      assertThat(item.source()).isEqualTo("千铝工投");
    });
  }

  @Test
  void trainResourcesUseCompanyAndRunningState() {
    ProjectResourceQueryReq req = new ProjectResourceQueryReq();

    var result = service.page("train", req);

    assertThat(result.items()).allSatisfy(item -> {
      assertThat(item.name()).endsWith("实训中心");
      assertThat(item.state()).isEqualTo("进行中");
      assertThat(item.company()).isEqualTo("千铝工投");
    });
  }

  @Test
  void keywordAndStateFilterResourcesBeforePaging() {
    ProjectResourceQueryReq req = new ProjectResourceQueryReq();
    req.setKeyword("高级技师");
    req.setState("通过");

    var result = service.page("teach", req);

    assertThat(result.total()).isEqualTo(2);
    assertThat(result.items().getFirst().name()).contains("高级技师");
  }

  @Test
  void unsupportedTypeIsRejected() {
    ProjectResourceQueryReq req = new ProjectResourceQueryReq();

    assertThatThrownBy(() -> service.page("unknown", req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("开发工程资源类型不正确");
  }
}
