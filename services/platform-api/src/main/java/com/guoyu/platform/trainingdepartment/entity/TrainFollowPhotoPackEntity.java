package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班培训照片 Word 生成记录 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_photo_pack")
public class TrainFollowPhotoPackEntity extends BaseEntity {

  private Long classId;
  private Long fileId;
  private String filePath;
  private String title;
  private String snapshotJson;
  private LocalDateTime generatedAt;
  private Long generatedBy;
}
