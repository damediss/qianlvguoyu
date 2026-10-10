package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_issue")
public class TrainFollowIssueEntity extends BaseEntity {
  private Long classId;
  private Long supervisionId;
  private Long itemId;
  private String title;
  private String description;
  /** info / warn / critical */
  private String severity;
  /** open / doing / done / closed */
  private String status;
  private String assigneeName;
  private String dueDate;
  private String resolveNote;
  private Long openedBy;
  private LocalDateTime openedAt;
  private LocalDateTime closedAt;
}
