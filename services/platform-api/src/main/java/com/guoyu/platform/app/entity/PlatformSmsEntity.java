package com.guoyu.platform.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 对齐 PHP platform_sms */
@TableName("platform_sms")
public class PlatformSmsEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String phone;
  private String sms;
  private Long addtime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getSms() { return sms; }
  public void setSms(String sms) { this.sms = sms; }
  public Long getAddtime() { return addtime; }
  public void setAddtime(Long addtime) { this.addtime = addtime; }
}
