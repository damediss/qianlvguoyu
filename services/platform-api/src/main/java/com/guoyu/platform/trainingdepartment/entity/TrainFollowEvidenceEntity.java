package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班课堂影像证据 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_evidence")
public class TrainFollowEvidenceEntity extends BaseEntity {

  private Long classId;
  private Long scheduleId;
  private Long sessionId;
  private String bizDate;
  private String teachFormOverride;
  private String classHoursOverride;
  /** photo / video */
  private String mediaType;
  /** 视频时长（秒） */
  private Integer durationSeconds;
  private Long fileId;
  private String filePath;
  private String thumbPath;
  private String caption;
  private Long uploaderUserId;
  private String uploaderRole;
  /** pending / approved / rejected */
  private String reviewStatus;
  private Long reviewedBy;
  private LocalDateTime reviewedAt;
  private String rejectReason;
  private Integer sortNo;
}
