package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开班信息
 */
@TableName("train_info_list")
public class TrainInfoListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训标题 */
  private String title;
  /** 封面 */
  private String cover;
  /** 简介 */
  private String intro;
  /** 分组类型 */
  private Integer groupType;
  /** 师傅类型 */
  private Integer masterType;
  /** 类型 */
  private Integer type;
  /** 分类 */
  private String cate;
  /** 企业ID */
  private Long companyId;
  /** 企业名称 */
  private String companyName;
  /** 企业地址 */
  private String companyAddress;
  /** 工种ID */
  private Long workTypeId;
  /** 工种等级 */
  private Integer workTypeLevel;
  /** 课程ID */
  private Long courseId;
  /** 人数 */
  private Integer num;
  /** 岗位 */
  private String position;
  /** 课时数 */
  private Integer courseNum;
  /** 负责人确认 0-未确认 1-已确认 */
  private Integer headConfirm;
  /** 用户ID */
  private Long userId;
  /** 企业是否确认 0-未确认 1-已确认 */
  private Integer companyHasConfirm;
  /** 企业确认时间 */
  private LocalDateTime companyConfirmTime;
  /** 企业确认人ID */
  private Long companyConfirmUserId;
  /** 企业确认人姓名 */
  private String companyConfirmUsername;
  /** 审核状态 */
  private Integer passStatus;
  /** 审核人ID */
  private Long checkUserId;
  /** 审核人姓名 */
  private String checkUsername;
  /** 是否顺序学习 0-否 1-是 */
  private Integer isSortedLearn;
  /** 平时成绩满分 */
  private BigDecimal maxNormalScore;
  /** 课程文件 */
  private String courseFile;
  /** 开始日期 */
  private String startDate;
  /** 结束日期 */
  private String endDate;
  /** 培训机构名称 */
  private String trainCompanyName;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 积分 */
  private Integer point;
  /** 相册 */
  private String album;
  /** 总结 */
  private String summary;
  /** 省份ID */
  private Long provinceId;
  /** 省份名称 */
  private String provinceName;
  /** 城市ID */
  private Long cityId;
  /** 城市名称 */
  private String cityName;
  /** 能力成绩 */
  private BigDecimal abilityScore;
  /** 区域ID */
  private Long regionId;
  /** 区域名称 */
  private String regionName;
  /** 负责人ID */
  private Long inChargeUserId;
  /** 负责人姓名 */
  private String inChargeUsername;
  /** 附件 */
  private String files;
  /** 培训需求ID */
  private Long traindemandId;
  /** 来源三年规划ID */
  private Long threeYearPlanId;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public String getIntro() { return intro; }
  public void setIntro(String intro) { this.intro = intro; }
  public Integer getGroupType() { return groupType; }
  public void setGroupType(Integer groupType) { this.groupType = groupType; }
  public Integer getMasterType() { return masterType; }
  public void setMasterType(Integer masterType) { this.masterType = masterType; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getCate() { return cate; }
  public void setCate(String cate) { this.cate = cate; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public String getCompanyAddress() { return companyAddress; }
  public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getWorkTypeLevel() { return workTypeLevel; }
  public void setWorkTypeLevel(Integer workTypeLevel) { this.workTypeLevel = workTypeLevel; }
  public Long getCourseId() { return courseId; }
  public void setCourseId(Long courseId) { this.courseId = courseId; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }
  public Integer getCourseNum() { return courseNum; }
  public void setCourseNum(Integer courseNum) { this.courseNum = courseNum; }
  public Integer getHeadConfirm() { return headConfirm; }
  public void setHeadConfirm(Integer headConfirm) { this.headConfirm = headConfirm; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getCompanyHasConfirm() { return companyHasConfirm; }
  public void setCompanyHasConfirm(Integer companyHasConfirm) { this.companyHasConfirm = companyHasConfirm; }
  public LocalDateTime getCompanyConfirmTime() { return companyConfirmTime; }
  public void setCompanyConfirmTime(LocalDateTime companyConfirmTime) { this.companyConfirmTime = companyConfirmTime; }
  public Long getCompanyConfirmUserId() { return companyConfirmUserId; }
  public void setCompanyConfirmUserId(Long companyConfirmUserId) { this.companyConfirmUserId = companyConfirmUserId; }
  public String getCompanyConfirmUsername() { return companyConfirmUsername; }
  public void setCompanyConfirmUsername(String companyConfirmUsername) { this.companyConfirmUsername = companyConfirmUsername; }
  public Integer getPassStatus() { return passStatus; }
  public void setPassStatus(Integer passStatus) { this.passStatus = passStatus; }
  public Long getCheckUserId() { return checkUserId; }
  public void setCheckUserId(Long checkUserId) { this.checkUserId = checkUserId; }
  public String getCheckUsername() { return checkUsername; }
  public void setCheckUsername(String checkUsername) { this.checkUsername = checkUsername; }
  public Integer getIsSortedLearn() { return isSortedLearn; }
  public void setIsSortedLearn(Integer isSortedLearn) { this.isSortedLearn = isSortedLearn; }
  public BigDecimal getMaxNormalScore() { return maxNormalScore; }
  public void setMaxNormalScore(BigDecimal maxNormalScore) { this.maxNormalScore = maxNormalScore; }
  public String getCourseFile() { return courseFile; }
  public void setCourseFile(String courseFile) { this.courseFile = courseFile; }
  public String getStartDate() { return startDate; }
  public void setStartDate(String startDate) { this.startDate = startDate; }
  public String getEndDate() { return endDate; }
  public void setEndDate(String endDate) { this.endDate = endDate; }
  public String getTrainCompanyName() { return trainCompanyName; }
  public void setTrainCompanyName(String trainCompanyName) { this.trainCompanyName = trainCompanyName; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getPoint() { return point; }
  public void setPoint(Integer point) { this.point = point; }
  public String getAlbum() { return album; }
  public void setAlbum(String album) { this.album = album; }
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  public Long getProvinceId() { return provinceId; }
  public void setProvinceId(Long provinceId) { this.provinceId = provinceId; }
  public String getProvinceName() { return provinceName; }
  public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
  public Long getCityId() { return cityId; }
  public void setCityId(Long cityId) { this.cityId = cityId; }
  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }
  public BigDecimal getAbilityScore() { return abilityScore; }
  public void setAbilityScore(BigDecimal abilityScore) { this.abilityScore = abilityScore; }
  public Long getRegionId() { return regionId; }
  public void setRegionId(Long regionId) { this.regionId = regionId; }
  public String getRegionName() { return regionName; }
  public void setRegionName(String regionName) { this.regionName = regionName; }
  public Long getInChargeUserId() { return inChargeUserId; }
  public void setInChargeUserId(Long inChargeUserId) { this.inChargeUserId = inChargeUserId; }
  public String getInChargeUsername() { return inChargeUsername; }
  public void setInChargeUsername(String inChargeUsername) { this.inChargeUsername = inChargeUsername; }
  public String getFiles() { return files; }
  public void setFiles(String files) { this.files = files; }
  public Long getTraindemandId() { return traindemandId; }
  public void setTraindemandId(Long traindemandId) { this.traindemandId = traindemandId; }
  public Long getThreeYearPlanId() { return threeYearPlanId; }
  public void setThreeYearPlanId(Long threeYearPlanId) { this.threeYearPlanId = threeYearPlanId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
