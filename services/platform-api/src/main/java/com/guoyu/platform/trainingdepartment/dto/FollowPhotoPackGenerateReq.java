package com.guoyu.platform.trainingdepartment.dto;

/**
 * 生成培训照片 Word 时的封面信息（可在弹窗中编辑）。
 */
public class FollowPhotoPackGenerateReq {

  /** 文档标题，如「华友锂业浸出工高级培训照片」 */
  private String title;
  /** 培训工种 */
  private String workType;
  /** 培训级别，如「高级工」 */
  private String level;
  /** 培训时间，如「2024-06-01至2024-06-30」 */
  private String trainTime;
  /** 培训单位 */
  private String trainUnit;
  /** 培训地址 */
  private String trainAddress;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getWorkType() {
    return workType;
  }

  public void setWorkType(String workType) {
    this.workType = workType;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getTrainTime() {
    return trainTime;
  }

  public void setTrainTime(String trainTime) {
    this.trainTime = trainTime;
  }

  public String getTrainUnit() {
    return trainUnit;
  }

  public void setTrainUnit(String trainUnit) {
    this.trainUnit = trainUnit;
  }

  public String getTrainAddress() {
    return trainAddress;
  }

  public void setTrainAddress(String trainAddress) {
    this.trainAddress = trainAddress;
  }
}
