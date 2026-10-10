package com.guoyu.platform.app.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 文章分类表（对齐 PHP cms_article_category） */
@TableName("cms_article_category")
public class CmsArticleCategoryEntity {

  @TableId
  private Integer categoryId;
  private Integer parentId;
  private String title;

  public Integer getCategoryId() { return categoryId; }
  public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
  public Integer getParentId() { return parentId; }
  public void setParentId(Integer parentId) { this.parentId = parentId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
}
