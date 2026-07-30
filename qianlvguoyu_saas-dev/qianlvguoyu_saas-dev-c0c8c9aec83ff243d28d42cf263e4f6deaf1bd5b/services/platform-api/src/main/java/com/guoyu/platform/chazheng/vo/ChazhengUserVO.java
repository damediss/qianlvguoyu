package com.guoyu.platform.chazheng.vo;

import com.guoyu.platform.chazheng.entity.ChazhengUserEntity;
import java.time.LocalDateTime;
import java.util.List;

public class ChazhengUserVO {

  private Integer id;
  private Integer enterpriseId;
  private String userName;
  private String idCard;
  private Integer gender;
  private String datas;
  private Integer isCert;
  private Integer isChecked;
  private String ymd;
  private Integer status;
  private Integer sortNumber;
  private LocalDateTime createTime;
  private List<ChazhengCertVO> certs;

  public static ChazhengUserVO from(ChazhengUserEntity entity) {
    ChazhengUserVO vo = new ChazhengUserVO();
    vo.setId(entity.getId());
    vo.setEnterpriseId(entity.getEnterpriseId());
    vo.setUserName(entity.getUserName());
    vo.setIdCard(entity.getIdCard());
    vo.setGender(entity.getGender());
    vo.setDatas(entity.getDatas());
    vo.setIsCert(entity.getIsCert());
    vo.setIsChecked(entity.getIsChecked());
    vo.setYmd(entity.getYmd());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setCreateTime(entity.getCreateTime());
    return vo;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getEnterpriseId() { return enterpriseId; }
  public void setEnterpriseId(Integer enterpriseId) { this.enterpriseId = enterpriseId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public String getDatas() { return datas; }
  public void setDatas(String datas) { this.datas = datas; }
  public Integer getIsCert() { return isCert; }
  public void setIsCert(Integer isCert) { this.isCert = isCert; }
  public Integer getIsChecked() { return isChecked; }
  public void setIsChecked(Integer isChecked) { this.isChecked = isChecked; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public List<ChazhengCertVO> getCerts() { return certs; }
  public void setCerts(List<ChazhengCertVO> certs) { this.certs = certs; }
}