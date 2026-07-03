package com.guoyu.platform.auth.vo;

import java.util.ArrayList;
import java.util.List;

public class MenuVO {

  private Long id;
  private Long parentId;
  private String menuType;
  private String code;
  private String name;
  private String path;
  private String component;
  private String permissionCode;
  private String icon;
  private Integer sortOrder;
  private Integer expanded;
  private List<MenuVO> children = new ArrayList<>();

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

  public String getMenuType() {
    return menuType;
  }

  public void setMenuType(String menuType) {
    this.menuType = menuType;
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

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public String getPermissionCode() {
    return permissionCode;
  }

  public void setPermissionCode(String permissionCode) {
    this.permissionCode = permissionCode;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Integer getExpanded() {
    return expanded;
  }

  public void setExpanded(Integer expanded) {
    this.expanded = expanded;
  }

  public List<MenuVO> getChildren() {
    return children;
  }

  public void setChildren(List<MenuVO> children) {
    this.children = children;
  }
}
