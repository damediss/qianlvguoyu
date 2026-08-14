package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_supervision_item")
public class TrainFollowSupervisionItemEntity extends BaseEntity {
  private Long supervisionId;
  private String itemCode;
  private String itemTitle;
  private Integer sortNo;
  /** pass / fail / na */
  private String result;
  private String remark;
}
