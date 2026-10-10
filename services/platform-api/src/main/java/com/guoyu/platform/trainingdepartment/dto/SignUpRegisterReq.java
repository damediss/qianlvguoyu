package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 扫码信息收集 - 完整版请求体（JSON）。
 *
 * <p>字段按业务域分组：基本信息 / 户籍与地址 / 学历信息 / 工作信息 / 现工种 /
 * 申报工种 / 附件URL（已通过 /upload 提前上传）。
 */
@Data
public class SignUpRegisterReq {

  // ==================== 1. 基本信息 ====================
  @NotBlank(message = "姓名必填")
  @Size(max = 50)
  private String name;

  @NotBlank(message = "手机号必填")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
  private String phone;

  @NotBlank(message = "身份证号必填")
  @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式错误")
  private String idCardNumber;

  private Integer age;

  @NotBlank(message = "性别必填")
  @Pattern(regexp = "^(男|女)$", message = "性别只能是男/女")
  private String gender;

  private String nation;
  private String political;
  private String education;
  private String houseType;
  private String address;

  // ==================== 2. 学历信息 ====================
  private String school;
  private String major;
  private String inSchoolDate;
  private String graduateDate;
  private String schoolNo;
  private Integer schoolPaperHas;

  private String schoolPaper;

  // ==================== 3. 工作信息 ====================
  private Long companyId;
  private String companyName;
  private String companyCode;
  private String zipCode;
  private String companyAddress;
  private String industrialPark;
  private String comments;
  private String workDateStart;
  private String workDateEnd;
  private String workTime;

  // ==================== 4. 职称 ====================
  private String titleType;
  private String titleLevel;
  private String titlePaper;

  // ==================== 5. 现工种 ====================
  private Long workTypeId;
  private String workTypeName;
  private Integer level;
  private String paperNo;

  // ==================== 6. 申报工种（拟报） ====================
  private Long reqWorkTypeId;
  private String reqWorkTypeName;
  private Integer reqLevel;

  private String declarationCondition;
  private String workExp;
  private String job;
  private String jog;

  // ==================== 7. 上一等级证书 ====================
  private Integer workTypePaperHas;
  private String workTypePaper;
  private String workTypePaperName;
  private Integer workTypePaperLevel;
  private String workTypePaperNo;

  // ==================== 8. 合同 / 社保（图片URL） ====================
  private String contactFirstPage;
  private String contactExpirePage;
  private String contactSignPage;
  private String socialSecurity;

  // ==================== 9. 身份证 / 照片URL ====================
  private String idCardFront;
  private String idCardBack;
  private String idCardHeader;
  private String idCardHeader2;
  private String photo;
  private String contract;
  private String certificate;

  /** 其它附加 URL（前端自由使用） */
  private List<String> extraFileUrls;
}
