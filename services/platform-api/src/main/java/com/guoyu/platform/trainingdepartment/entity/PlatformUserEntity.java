package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 平台用户
 */
@TableName("platform_user")
public class PlatformUserEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 真实姓名 */
  private String realname;
  /** 身份证号 */
  private String idcard;
  /** 手机号 */
  private String phone;
  /** 密码 */
  private String password;
  /** 状态 */
  private String stat;
  /** 添加时间(Unix时间戳) */
  private Long addtime;
  /** 最后登录时间(Unix时间戳) */
  private Long logintime;
  /** 机构ID */
  private Long orgId;
  /** 企业ID */
  private Long companyId;
  /** 微信OpenID */
  private String wxopenid;
  /** 专家ID */
  private Long expertId;
  /** 任务标识 */
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
