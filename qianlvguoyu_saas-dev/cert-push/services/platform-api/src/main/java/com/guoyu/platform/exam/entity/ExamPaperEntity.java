package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("exam_paper")
public class ExamPaperEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer makeId;
  private Integer source;
  private Integer type;
  private String code;
  private String title;
  private Integer time;
  private Integer totalPoint;
  private Integer passPoint;
  private Integer isComputer;
  private Integer isPrint;
  private String comments;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getMakeId() { return makeId; }
  public void setMakeId(Integer makeId) { this.makeId = makeId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getTime() { return time; }
  public void setTime(Integer time) { this.time = time; }
  public Integer getTotalPoint() { return totalPoint; }
  public void setTotalPoint(Integer totalPoint) { this.totalPoint = totalPoint; }
  public Integer getPassPoint() { return passPoint; }
  public void setPassPoint(Integer passPoint) { this.passPoint = passPoint; }
  public Integer getIsComputer() { return isComputer; }
  public void setIsComputer(Integer isComputer) { this.isComputer = isComputer; }
  public Integer getIsPrint() { return isPrint; }
  public void setIsPrint(Integer isPrint) { this.isPrint = isPrint; }
  public String getComments() { return comments; }
  public void setComments(String comments) { this.comments = comments; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
