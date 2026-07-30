package com.guoyu.platform.system.vo;

import java.util.ArrayList;
import java.util.List;

public class OrgTreeVO {

  private Long id;
  private Long parentId;
  private String orgType;
  private String code;
  private String name;
  private Integer sortOrder;
  private String status;
  private List<OrgTreeVO> children = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public String getOrgType() {
    return orgType;
  }

  public void setOrgType(String orgType) {
    this.orgType = orgType;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<OrgTreeVO> getChildren() {
    return children;
  }

  public void setChildren(List<OrgTreeVO> children) {
    this.children = children;
  }
}
