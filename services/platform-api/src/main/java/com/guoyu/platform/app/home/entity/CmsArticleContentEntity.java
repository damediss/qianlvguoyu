package com.guoyu.platform.app.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 文章正文表（对齐 PHP cms_article_content） */
@TableName("cms_article_content")
public class CmsArticleContentEntity {

  @TableId
  private Integer articleId;
  private String content;
  private LocalDateTime createTime;

  public Integer getArticleId() { return articleId; }
  public void setArticleId(Integer articleId) { this.articleId = articleId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
