package com.guoyu.platform.app.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 首页轮播图表（对齐 PHP sys_index_swiper） */
@TableName("sys_index_swiper")
public class SysIndexSwiperEntity {

  @TableId
  private Integer id;
  private String img;
  private Integer sortOrder;
  private Integer isShow;
  private String url;
  /** 页面类型：index=首页 */
  private String pageType;
  private String param;
  private Long tenantId;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public String getImg() { return img; }
  public void setImg(String img) { this.img = img; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public Integer getIsShow() { return isShow; }
  public void setIsShow(Integer isShow) { this.isShow = isShow; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public String getPageType() { return pageType; }
  public void setPageType(String pageType) { this.pageType = pageType; }
  public String getParam() { return param; }
  public void setParam(String param) { this.param = param; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
