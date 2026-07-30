package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_material")
public class CertificationArchiveMaterialEntity extends BaseEntity {
  private Long archiveId;
  private String materialType;
  private String materialGroup;
  private String materialName;
  private String fileName;
  private String fileKey;
  private Long fileSize;
  private String fileHash;
  private String contentType;
  private String fileStatus;
  private Long uploadedBy;
  private LocalDateTime uploadedAt;
}
