package com.guoyu.platform.exam.zhenti.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 历年真题（exam_zhenti）。
 *
 * <p>file_key 是存储对象键，属于内部字段，任何接口出参都不得包含（方案 §2.3）。
 */
@TableName("exam_zhenti")
public class ExamZhentiEntity extends BaseEntity {

  private Long examId;
  private Long workTypeId;
  private Integer levelCode;
  private String scopeKey;
  private String title;

  /** 允许管理员清空年份（updateById 默认忽略 null，需显式 ALWAYS 才能写回 NULL）。 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String year;
  private String fileKey;
  private Long fileSize;
  private String fileMd5;
  private Integer sort;
  private Integer status;
  private Integer metaPending;

  public Long getExamId() {
    return examId;
  }

  public void setExamId(Long examId) {
    this.examId = examId;
  }

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public Integer getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(Integer levelCode) {
    this.levelCode = levelCode;
  }

  public String getScopeKey() {
    return scopeKey;
  }

  public void setScopeKey(String scopeKey) {
    this.scopeKey = scopeKey;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getFileKey() {
    return fileKey;
  }

  public void setFileKey(String fileKey) {
    this.fileKey = fileKey;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public String getFileMd5() {
    return fileMd5;
  }

  public void setFileMd5(String fileMd5) {
    this.fileMd5 = fileMd5;
  }

  public Integer getSort() {
    return sort;
  }

  public void setSort(Integer sort) {
    this.sort = sort;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getMetaPending() {
    return metaPending;
  }

  public void setMetaPending(Integer metaPending) {
    this.metaPending = metaPending;
  }
}
