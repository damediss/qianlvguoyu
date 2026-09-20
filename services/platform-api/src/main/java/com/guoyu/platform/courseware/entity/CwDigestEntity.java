package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件消化素材 cw_digest(每项目一份;失败无记录走降级) */
@Getter
@Setter
@TableName("cw_digest")
public class CwDigestEntity extends BaseEntity {

  public static final String STATUS_READY = "ready";
  public static final String STATUS_FAILED = "failed";

  private Long projectId;
  /** JSON: [{name, points[], confusions[], key_data[], source_refs[]}] */
  private String topics;
  private String status;
  private String error;
}
