package com.guoyu.platform.app.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 文章表（对齐 PHP cms_article） */
@TableName("cms_article")
public class CmsArticleEntity {

  @TableId
  private Integer articleId;
  private String title;
  /** 0列表 1单页 2外链 */
  private Integer type;
  private Integer categoryId;
  private String image;
  /** 图集 JSON 字符串 */
  private String album;
  private Integer actualViews;
  private Integer virtualViews;
  private Integer likes;
  private Integer commentNumbers;
  private Integer recommend;
  private Integer sortNumber;
  private Integer status;
  private LocalDateTime publishTime;
  private LocalDateTime createTime;
  private Integer deleted;

  public Integer getArticleId() { return articleId; }
  public void setArticleId(Integer articleId) { this.articleId = articleId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getCategoryId() { return categoryId; }
  public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
  public String getImage() { return image; }
  public void setImage(String image) { this.image = image; }
  public String getAlbum() { return album; }
  public void setAlbum(String album) { this.album = album; }
  public Integer getActualViews() { return actualViews; }
  public void setActualViews(Integer actualViews) { this.actualViews = actualViews; }
  public Integer getVirtualViews() { return virtualViews; }
  public void setVirtualViews(Integer virtualViews) { this.virtualViews = virtualViews; }
  public Integer getLikes() { return likes; }
  public void setLikes(Integer likes) { this.likes = likes; }
  public Integer getCommentNumbers() { return commentNumbers; }
  public void setCommentNumbers(Integer commentNumbers) { this.commentNumbers = commentNumbers; }
  public Integer getRecommend() { return recommend; }
  public void setRecommend(Integer recommend) { this.recommend = recommend; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public LocalDateTime getPublishTime() { return publishTime; }
  public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
