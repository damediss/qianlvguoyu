package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 开班人员列表
 */
@Getter
@Setter
@TableName("train_people_list")
public class TrainPeopleListEntity {

  /** 主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 班级ID */
  private Long trainClassId;
  /** 人员类型 */
  private String type;
  /** 用户ID */
  private Long userId;
  /** 平台用户ID */
  private Long platformUserId;
  /** 来源报名人员ID */
  private Long signUpPeopleId;
  /** 真实姓名 */
  private String realname;
  /** 手机号 */
  private String phone;
  /** 是否组长 0-否 1-是 */
  private Integer isLeader;
  /** 是否班委 0-否 1-是 */
  private Integer isCommittee;
  /** 考试成绩 */
  private BigDecimal examScore;
  /** 实操成绩 */
  private BigDecimal realScore;
  /** 证书 */
  private String paper;
  /** 平时成绩 */
  private BigDecimal normalScore;
  /** 国家证书 */
  private String nationalPaper;
  /** 当前等级 */
  private Integer currentLevel;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 身份证号 */
  private String idCard;

  /** 年龄 */
  private Integer age;
  /** 申报工种ID */
  private Long reqWorkTypeId;
  /** 申报工种 */
  private String reqWorkTypeName;
  /** 申报工种等级 */
  private Integer reqLevel;
  /** 职业 */
  private String job;
  /** 工种ID */
  private Long workTypeId;
  /** 工种 */
  private String workTypeName;
  /** 工种等级 */
  @TableField("`level`")
  private String level;
  /** 企业ID */
  private Long companyId;
  /** 企业名称 */
  private String companyName;
  /** 企业代码 */
  private String companyCode;
  /** 邮编 */
  private String zipCode;
  /** 企业地址 */
  private String companyAddress;
  /** 工业园 */
  private String industrialPark;
  /** 备注 */
  private String comments;
  /** 企业审核状态 */
  private Integer companyStatus;
  /** 身份证正面 */
  private String idCardFront;
  /** 身份证反面 */
  private String idCardBack;
  /** 一寸照 */
  private String idCardHeader;
  /** 两寸照 */
  private String idCardHeader2;
  /** 是否有上一等级证书 */
  private Integer workTypePaperHas;
  /** 上一等级证书图片 */
  private String workTypePaper;
  /** 上一等级证书编号 */
  private String workTypePaperNo;
  /** 上一等级证书等级 */
  private Integer workTypePaperLevel;
  /** 上一等级证书工种 */
  private String workTypePaperName;
  /** 本公司入职日期 */
  private String workDateStart;
  /** 本公司在职结束日期 */
  private String workDateEnd;
  /** 从业日期 */
  private String workTime;
  /** 职称 */
  private String titleType;
  /** 职称等级 */
  private String titleLevel;
  /** 职称证 */
  private String titlePaper;
  /** 是否缴费 */
  private Integer hasPay;
  /** 学校 */
  private String school;
  /** 专业 */
  private String major;
  /** 入学日期 */
  private String inSchoolDate;
  /** 毕业日期 */
  private String graduateDate;
  /** 是否有毕业证 */
  private Integer schoolPaperHas;
  /** 毕业证图片 */
  private String schoolPaper;
  /** 民族 */
  private String nation;
  /** 地址 */
  private String address;
  /** 学历 */
  private String education;
  /** 申报条件 */
  @TableField("`condition`")
  private String declarationCondition;
  /** 毕业证编号 */
  private String schoolNo;
  /** 证书编号 */
  private String paperNo;
  /** 工作经历 */
  private String workExp;
  /** 劳动合同第一页 */
  private String contactFirstPage;
  /** 劳动合同期限页 */
  private String contactExpirePage;
  /** 劳动合同盖章页 */
  private String contactSignPage;
  /** 社保 */
  private String socialSecurity;
  /** 户口性质 */
  private String houseType;
  /** 政治面貌 */
  private String political;
  /** 材料导出任务标记 */
  private Integer task;

  /** 审核状态: 0待审核, 1通过, 2拒绝 */
  private Integer status;

  /** 逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
}
