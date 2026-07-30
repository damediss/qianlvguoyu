package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class TrainShiftAssignPatchReq {

  @NotNull
  private Integer version;
  @NotEmpty
  @Valid
  private List<Change> changes;

  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public List<Change> getChanges() { return changes; }
  public void setChanges(List<Change> changes) { this.changes = changes; }

  public static class Change {
    @NotNull
    private String personName;
    private String employeeNo;
    @NotNull
    private String assignDate;
    private String slot;
    /** null 表示取消该时段排课 */
    private String algoClassName;

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getAssignDate() { return assignDate; }
    public void setAssignDate(String assignDate) { this.assignDate = assignDate; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getAlgoClassName() { return algoClassName; }
    public void setAlgoClassName(String algoClassName) { this.algoClassName = algoClassName; }
  }
}
