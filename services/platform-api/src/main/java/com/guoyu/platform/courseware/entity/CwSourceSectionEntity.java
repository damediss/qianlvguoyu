package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件来源小节 cw_source_section */
@Getter
@Setter
@TableName("cw_source_section")
public class CwSourceSectionEntity extends BaseEntity {

  private Long projectId;
  private String ref;
  private String heading;
  private Integer level;
  private String text;
  private String locator;
}
