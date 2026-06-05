package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_file_temp
 */
@TableName("train_file_temp")
public class TrainFileTempEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 模板类型 */
  private String type;

  /** 模板名称 */
  private String title;

  /** 城市/地区 */
  private String city;

  /** 源文件路径 */
  private String pathSource;

  /** 扩展名 */
  private String ext;

  /** 变量替换后的 Word 路径 */
  private String pathUsed;

  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getPathSource() { return pathSource; }
  public void setPathSource(String pathSource) { this.pathSource = pathSource; }
  public String getExt() { return ext; }
  public void setExt(String ext) { this.ext = ext; }
  public String getPathUsed() { return pathUsed; }
  public void setPathUsed(String pathUsed) { this.pathUsed = pathUsed; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
