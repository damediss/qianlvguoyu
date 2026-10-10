package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExpertVO implements Serializable {

  private Integer id;
  private String nickname;

  public ExpertVO() {}

  public ExpertVO(Integer id, String nickname) {
    this.id = id;
    this.nickname = nickname;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
}
