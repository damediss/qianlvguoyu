package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.util.List;

public class PublicDataVO implements Serializable {

  private List<ExamWorkTypeVO> workTypeList;
  private List<ExpertVO> expertList;

  public List<ExamWorkTypeVO> getWorkTypeList() { return workTypeList; }
  public void setWorkTypeList(List<ExamWorkTypeVO> workTypeList) { this.workTypeList = workTypeList; }
  public List<ExpertVO> getExpertList() { return expertList; }
  public void setExpertList(List<ExpertVO> expertList) { this.expertList = expertList; }
}
