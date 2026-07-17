package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("train_shift_assign_record")
public class TrainShiftAssignRecordEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long taskId;
  private String personName;
  private String idCard;
  private String employeeNo;
  private String dept;
  private String shiftClass;
  private LocalDate assignDate;
  private String slot;
  private String algoClassName;
  private Long trainClassId;
  private Integer rowIndex;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public String getPersonName() { return personName; }
  public void setPersonName(String personName) { this.personName = personName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
  public String getDept() { return dept; }
  public void setDept(String dept) { this.dept = dept; }
  public String getShiftClass() { return shiftClass; }
  public void setShiftClass(String shiftClass) { this.shiftClass = shiftClass; }
  public LocalDate getAssignDate() { return assignDate; }
  public void setAssignDate(LocalDate assignDate) { this.assignDate = assignDate; }
  public String getSlot() { return slot; }
  public void setSlot(String slot) { this.slot = slot; }
  public String getAlgoClassName() { return algoClassName; }
  public void setAlgoClassName(String algoClassName) { this.algoClassName = algoClassName; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public Integer getRowIndex() { return rowIndex; }
  public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
