package com.guoyu.platform.trainingdepartment.security;

/**
 * 培训模块跨菜单接口的 SpEL 权限表达式。
 * <p>
 * 前端四步流程（需求/方案/培训信息/班级人员/开班申报）常只分配当前步骤菜单权限，
 * 但会调用公共下拉、班级子功能等接口，需在此做 OR 兼容。
 */
public final class TrainingAuthorityExpressions {

  private TrainingAuthorityExpressions() {}

  /** 公共下拉：企业/工种/培训信息/课件等 */
  public static final String PUBLIC_DATA_VIEW =
      "hasAnyAuthority('train:demand-train:view', 'train:need:view', "
          + "'train:info-list:view', 'train:info-class:view', 'train:people-list:view', "
          + "'train:solution:view', 'train:declare:view', 'train:follow-exam:view')";

  /** 需求管理待办/回收站（菜单 train_needs） */
  public static final String DEMAND_PIPELINE_VIEW =
      "hasAnyAuthority('train:demand-train:view', 'train:need:view')";

  /** 需求转培训开班 */
  public static final String DEMAND_CONVERT =
      "hasAnyAuthority('train:demand-train:create', 'train:need:create', 'train:info-list:create')";

  /** 开班信息列表（需求管理已办 + 培训信息页） */
  public static final String INFO_LIST_PAGE_VIEW =
      "hasAnyAuthority('train:info-list:view', 'train:need:view')";

  /** 班级列表（班级人员 + 开班申报 + 企业审核） */
  public static final String INFO_CLASS_PAGE_VIEW =
      "hasAnyAuthority('train:info-class:view', 'train:people-list:view', 'train:declare:view', "
          + "'pxqiyeshenhe:view')";

  /** 班级新增/编辑（班级人员页 save） */
  public static final String INFO_CLASS_SAVE =
      "hasAnyAuthority('train:info-class:create', 'train:info-class:update', "
          + "'train:people-list:create', 'train:people-list:update')";

  /** 班级删除 */
  public static final String INFO_CLASS_BATCH_DELETE =
      "hasAnyAuthority('train:info-class:delete', 'train:people-list:delete')";

  /** 班级人员子功能（学员/教务/负责人/报名导入 + 企业审核查看学员） */
  public static final String CLASS_PEOPLE_VIEW =
      "hasAnyAuthority('train:info-class:view', 'train:people-list:view', 'pxqiyeshenhe:view')";
  public static final String CLASS_PEOPLE_CREATE =
      "hasAnyAuthority('train:info-class:create', 'train:people-list:create')";
  public static final String CLASS_PEOPLE_UPDATE =
      "hasAnyAuthority('train:info-class:update', 'train:people-list:update')";
  public static final String CLASS_PEOPLE_DELETE =
      "hasAnyAuthority('train:info-class:delete', 'train:people-list:delete')";

  /** 班级培训任务（班级人员页） */
  public static final String CLASS_TASK_VIEW =
      "hasAnyAuthority('train:info-class:view', 'train:task-log:view')";
  public static final String CLASS_TASK_SAVE =
      "hasAnyAuthority('train:info-class:update', 'train:task-log:create', 'train:task-log:update')";

  /** 开班申报：班级材料挂载/生成 */
  public static final String DECLARE_CLASS_MUTATE =
      "hasAnyAuthority('train:info-class:update', 'train:declare:update')";

  /** 材料模板（开班申报页 legacy 接口） */
  public static final String FILE_TEMP_LEGACY_VIEW =
      "hasAnyAuthority('train:file-temp:view', 'train:declare:view', 'train:info-class:view')";
  public static final String FILE_TEMP_LEGACY_SAVE =
      "hasAnyAuthority('train:file-temp:create', 'train:file-temp:update', "
          + "'train:declare:update', 'train:info-class:update')";
  public static final String FILE_TEMP_LEGACY_DELETE =
      "hasAnyAuthority('train:file-temp:delete', 'train:declare:delete', 'train:info-class:update')";

  /** 企业端查看方案 */
  public static final String SOLUTION_BY_DEMAND_VIEW =
      "hasAnyAuthority('train:solution:view', 'train:demand-train:view')";

  /** 企业确认/驳回培训方案 */
  public static final String INFO_LIST_ENTERPRISE_CONFIRM =
      "hasAnyAuthority('train:info-list:update', 'train:demand-train:update')";

  /** 企业确认/驳回培训分班 */
  public static final String INFO_CLASS_ENTERPRISE_CONFIRM =
      "hasAnyAuthority('pxqiyeshenhe:confirm', 'pxanfangguihua:enterprise_confirm', "
          + "'train:info-list:update', 'train:demand-train:update')";

  /** 排课冲突检查 */
  public static final String SCHEDULE_CONFLICTS_VIEW =
      "hasAnyAuthority('train:class:view', 'train:info-class:view', 'train:class-list:view')";

  /** 三年规划企业确认/驳回 */
  public static final String THREE_YEAR_PLAN_ENTERPRISE_CONFIRM =
      "hasAnyAuthority('pxanfangguihua:enterprise_confirm', 'pxqiyeshenhe:confirm', "
          + "'pxanfangguihua:manage', 'train:info-list:update', 'train:demand-train:update')";

  /** 三年规划查看（机构 + 企业审核页） */
  public static final String THREE_YEAR_PLAN_VIEW =
      "hasAnyAuthority('pxanfangguihua:view', 'pxqiyeshenhe:view', "
          + "'pxanfangguihua:enterprise_confirm', 'pxqiyeshenhe:confirm')";

  /** 三年规划新建/同步/提交（机构操作员 + 平台管理员） */
  public static final String THREE_YEAR_PLAN_MANAGE =
      "hasAnyAuthority('pxanfangguihua:manage', 'pxanfangguihua:view')";

  /** 分班（组织分班）查看 */
  public static final String DIVISION_VIEW =
      "hasAnyAuthority('train:division:view', 'train:class-list:view', 'train:info-class:view', "
          + "'train:people-list:view', 'pxanfangguihua:view', 'train:info-list:view')";

  /** 分班（组织分班）写操作 */
  public static final String DIVISION_MANAGE =
      "hasAnyAuthority('train:division:view', 'train:class-list:create', 'train:class-list:update', "
          + "'train:info-class:create', 'train:info-class:update', "
          + "'train:people-list:create', 'train:people-list:update', 'pxanfangguihua:manage')";

  /** 人社成绩单导入：读 / 写分离（写不得仅凭 view） */
  public static final String EXAM_SCORE_IMPORT_VIEW =
      "hasAnyAuthority('train:follow-exam:view', 'train:exam-list:view', 'train:people-list:view')";
  public static final String EXAM_SCORE_IMPORT_UPDATE =
      "hasAnyAuthority('train:follow-exam:update', 'train:exam-list:update', 'train:people-list:update')";

  /** 企业调研：查看兼容菜单码 pxyanjiu:view；写操作仅 train:research_org:*（由种子授予） */
  public static final String RESEARCH_ORG_VIEW =
      "hasAnyAuthority('train:research_org:view', 'pxyanjiu:view')";
  public static final String RESEARCH_ORG_CREATE =
      "hasAuthority('train:research_org:create')";
  public static final String RESEARCH_ORG_UPDATE =
      "hasAuthority('train:research_org:update')";
  public static final String RESEARCH_ORG_DELETE =
      "hasAuthority('train:research_org:delete')";
  public static final String RESEARCH_ORG_MUTATE =
      "hasAnyAuthority('train:research_org:create', 'train:research_org:update')";
}
