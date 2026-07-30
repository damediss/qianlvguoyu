package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/** 对齐 PHP platform_user_detail */
@TableName("platform_user_detail")
public class PlatformUserDetailEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String realname;
  private String phone;
  private String idcard;
  private Integer gender;
  private String nation;
  private String education;
  private String political;
  private String address;
  private String idcardHeader;
  private String idcardFront;
  private String idcardBack;
  private String idcardStartDate;
  private String idcardEndDate;
  private String birthday;
  /** 户口性质 */
  private String houseType;
  private String school;
  private String major;
  private String schoolNo;
  private String inSchoolDate;
  private String graduateDate;
  private String workTime;
  private String titleType;
  /** 毕业证照片路径 */
  private String schoolPaper;

  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getRealname() { return realname; }
  public void setRealname(String realname) { this.realname = realname; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getIdcard() { return idcard; }
  public void setIdcard(String idcard) { this.idcard = idcard; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public String getNation() { return nation; }
  public void setNation(String nation) { this.nation = nation; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getPolitical() { return political; }
  public void setPolitical(String political) { this.political = political; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getIdcardHeader() { return idcardHeader; }
  public void setIdcardHeader(String idcardHeader) { this.idcardHeader = idcardHeader; }
  public String getIdcardFront() { return idcardFront; }
  public void setIdcardFront(String idcardFront) { this.idcardFront = idcardFront; }
  public String getIdcardBack() { return idcardBack; }
  public void setIdcardBack(String idcardBack) { this.idcardBack = idcardBack; }
  public String getIdcardStartDate() { return idcardStartDate; }
  public void setIdcardStartDate(String idcardStartDate) { this.idcardStartDate = idcardStartDate; }
  public String getIdcardEndDate() { return idcardEndDate; }
  public void setIdcardEndDate(String idcardEndDate) { this.idcardEndDate = idcardEndDate; }
  public String getBirthday() { return birthday; }
  public void setBirthday(String birthday) { this.birthday = birthday; }
  public String getHouseType() { return houseType; }
  public void setHouseType(String houseType) { this.houseType = houseType; }
  public String getSchool() { return school; }
  public void setSchool(String school) { this.school = school; }
  public String getMajor() { return major; }
  public void setMajor(String major) { this.major = major; }
  public String getSchoolNo() { return schoolNo; }
  public void setSchoolNo(String schoolNo) { this.schoolNo = schoolNo; }
  public String getInSchoolDate() { return inSchoolDate; }
  public void setInSchoolDate(String inSchoolDate) { this.inSchoolDate = inSchoolDate; }
  public String getGraduateDate() { return graduateDate; }
  public void setGraduateDate(String graduateDate) { this.graduateDate = graduateDate; }
  public String getWorkTime() { return workTime; }
  public void setWorkTime(String workTime) { this.workTime = workTime; }
  public String getTitleType() { return titleType; }
  public void setTitleType(String titleType) { this.titleType = titleType; }
  public String getSchoolPaper() { return schoolPaper; }
  public void setSchoolPaper(String schoolPaper) { this.schoolPaper = schoolPaper; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
