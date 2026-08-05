package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/** 机构编辑分班草稿 */
public class TrainDivisionPackPatchReq {

  @NotEmpty
  @Valid
  private List<ClassDraft> classes = new ArrayList<>();

  @Valid
  private List<UnassignedDraft> unassigned = new ArrayList<>();

  public List<ClassDraft> getClasses() { return classes; }
  public void setClasses(List<ClassDraft> classes) { this.classes = classes; }
  public List<UnassignedDraft> getUnassigned() { return unassigned; }
  public void setUnassigned(List<UnassignedDraft> unassigned) { this.unassigned = unassigned; }

  public static class ClassDraft {
    private String tempId;
    @NotNull
    private String title;
    private String group;
    /** 班级工种 ID（exam_work_type.id，与开班准备一致） */
    private Long workTypeId;
    /** 班级工种名称（展示/兼容；以 workTypeId 为准） */
    private String jobName;
    /** 班级等级文本（可多班不同等级） */
    private String levelName;
    @Valid
    private List<MemberDraft> members = new ArrayList<>();

    public String getTempId() { return tempId; }
    public void setTempId(String tempId) { this.tempId = tempId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public Long getWorkTypeId() { return workTypeId; }
    public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public List<MemberDraft> getMembers() { return members; }
    public void setMembers(List<MemberDraft> members) { this.members = members; }
  }

  public static class MemberDraft {
    @NotNull
    private Long rosterId;
    private String personName;
    private String idCard;
    private String workshop;
    private String jobName;
    private String levelName;
    private String shiftTeam;
    private String phone;

    public Long getRosterId() { return rosterId; }
    public void setRosterId(Long rosterId) { this.rosterId = rosterId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getWorkshop() { return workshop; }
    public void setWorkshop(String workshop) { this.workshop = workshop; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public String getShiftTeam() { return shiftTeam; }
    public void setShiftTeam(String shiftTeam) { this.shiftTeam = shiftTeam; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
  }

  public static class UnassignedDraft {
    private Long rosterId;
    private String personName;
    private String idCard;
    private String workshop;
    private String jobName;
    private String levelName;
    private String shiftTeam;
    private String reason;

    public Long getRosterId() { return rosterId; }
    public void setRosterId(Long rosterId) { this.rosterId = rosterId; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getWorkshop() { return workshop; }
    public void setWorkshop(String workshop) { this.workshop = workshop; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public String getShiftTeam() { return shiftTeam; }
    public void setShiftTeam(String shiftTeam) { this.shiftTeam = shiftTeam; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
  }
}
