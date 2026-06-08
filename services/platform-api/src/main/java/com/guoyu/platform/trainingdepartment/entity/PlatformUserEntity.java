package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/** 对齐 PHP platform_user（App/学员账号） */
@TableName("platform_user")
public class PlatformUserEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String realname;
  private String idcard;
  private String phone;
  private String password;
  private String stat;
  private Long addtime;
  private Long logintime;
  private Long orgId;
  private Long companyId;
  private String wxopenid;
  private Long expertId;
  private Integer task;

  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getIdcard() { return idcard; }
  public void setIdcard(String idcard) { this.idcard = idcard; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getStat() { return stat; }
  public void setStat(String stat) { this.stat = stat; }
  public Long getAddtime() { return addtime; }
  public void setAddtime(Long addtime) { this.addtime = addtime; }
  public Long getLogintime() { return logintime; }
  public void setLogintime(Long logintime) { this.logintime = logintime; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getWxopenid() { return wxopenid; }
  public void setWxopenid(String wxopenid) { this.wxopenid = wxopenid; }
  public Long getExpertId() { return expertId; }
  public void setExpertId(Long expertId) { this.expertId = expertId; }
  public Integer getTask() { return task; }
  public void setTask(Integer task) { this.task = task; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
