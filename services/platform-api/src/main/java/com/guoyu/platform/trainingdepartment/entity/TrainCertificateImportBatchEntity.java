package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 毕业典礼证书文件夹导入批次审计 */
@Getter
@Setter
@TableName("train_certificate_import_batch")
public class TrainCertificateImportBatchEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long operatorId;
  private String classIdsJson;
  private String previewId;
  private Integer totalFiles;
  private Integer matched;
  private Integer written;
  private Integer updated;
  private Integer skipped;
  private String errorsJson;
  private String remark;
  private LocalDateTime createTime;
}
