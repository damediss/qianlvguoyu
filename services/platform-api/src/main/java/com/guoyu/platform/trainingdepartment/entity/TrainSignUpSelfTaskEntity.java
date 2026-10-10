package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** train_sign_up_selftask — 默认模板导出任务 */
@TableName("train_sign_up_selftask")
public class TrainSignUpSelfTaskEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long signUpId;
  private String idsIn;
  private String idsOut;
  private Long addtime;
  private Long endtime;
  private String errorMsg;
  /** 成功时的 zip 相对路径，如 down/标题_t123.zip */
  @TableField("result_file")
  private String resultFile;
  private Long tenantId;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public String getIdsIn() { return idsIn; }
  public void setIdsIn(String idsIn) { this.idsIn = idsIn; }
  public String getIdsOut() { return idsOut; }
  public void setIdsOut(String idsOut) { this.idsOut = idsOut; }
  public Long getAddtime() { return addtime; }
  public void setAddtime(Long addtime) { this.addtime = addtime; }
  public Long getEndtime() { return endtime; }
  public void setEndtime(Long endtime) { this.endtime = endtime; }
  public String getErrorMsg() { return errorMsg; }
  public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
  public String getResultFile() { return resultFile; }
  public void setResultFile(String resultFile) { this.resultFile = resultFile; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
