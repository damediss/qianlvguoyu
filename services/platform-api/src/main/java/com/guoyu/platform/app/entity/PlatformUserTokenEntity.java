package com.guoyu.platform.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 对齐 PHP platform_user_token */
@TableName("platform_user_token")
public class PlatformUserTokenEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String token;
  private Long addtime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public Long getAddtime() { return addtime; }
  public void setAddtime(Long addtime) { this.addtime = addtime; }
}
