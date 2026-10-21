import { jsx as _jsx } from "react/jsx-runtime";
import './utils/legacyBrowserPolyfills';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
import ResourceCenterPage from './pages/question_bank/ResourceCenterPage';
import KnowledgeFixPage from './pages/question_bank/KnowledgeFixPage';
import XiMuBiaoGuanLiPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage';
import TheoryQuestionGenPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/TheoryQuestionGenPage';
import SkillQuestionGenPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage';
import AiCoursewarePage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage';
import { RencaishibieEmbedPage } from './pages/knowledge/RencaishibieEmbedPage';
import { WorkTypePage } from './pages/exam/WorkTypePage';
import { CoursePage } from './pages/exam/CoursePage';
import { DevTaskDetailPage } from './pages/exam/DevTaskDetailPage';
import { TaskExamAuditAssignListPage } from './pages/exam/TaskExamAuditAssignListPage';
import { TaskExamshicaoAuditAssignListPage } from './pages/exam/TaskExamshicaoAuditAssignListPage';
import { TaskExamsubAuditAssignListPage } from './pages/exam/TaskExamsubAuditAssignListPage';
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingDashboardPage } from './pages/training/TrainingDashboardPage';
import { TrainingFollowClassPage } from './pages/training/TrainingFollowClassPage';
import { TrainingFollowExamPage } from './pages/training/TrainingFollowExamPage';
import { TrainingSubsidyDeclarePage } from './pages/training/TrainingSubsidyDeclarePage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
import { TrainingClassDivisionPage } from './pages/training/TrainingClassDivisionPage';
import { TrainingClassDeclarePage } from './pages/training/TrainingClassDeclarePage';
import { UserAuthCoursePage } from './pages/exam/UserAuthCoursePage';
import { UserListPage } from './pages/chazheng/UserListPage';
import { CertQueryPage } from './pages/chazheng/CertQueryPage';
import { RequirementsManagementPage } from './pages/training/RequirementsManagementPage';
import { EnterpriseTrainingDemandPage } from './pages/training/EnterpriseTrainingDemandPage';
import { EnterpriseTalentDemandPage } from './pages/training/EnterpriseTalentDemandPage';
import { TrainingProgramPage } from './pages/training/TrainingProgramPage';
import { TrainingSchedulePage } from './pages/training/TrainingSchedulePage';
import { TrainingGraduationPage } from './pages/training/TrainingGraduationPage';
import { TrainingGraduationCeremonyPage } from './pages/training/TrainingGraduationCeremonyPage';
import { TrainingGraduationClassDetailPage } from './pages/training/TrainingGraduationClassDetailPage';
import { TrainingCapabilityPage } from './pages/training/TrainingCapabilityPage';
import { TrainingSummaryPage } from './pages/training/TrainingSummaryPage';
import { TrainingCertificateTemplatePage } from './pages/training/TrainingCertificateTemplatePage';
import { TrainingInformationPage } from './pages/training/TrainingInformationPage';
import { TrainingStudentPage } from './pages/training/TrainingStudentPage';
import { TrainingAcademicManagePage } from './pages/training/TrainingAcademicManagePage';
import { EnterpriseResearchPage } from './pages/training/EnterpriseResearchPage';
import { EnterpriseManagePage } from './pages/training/EnterpriseManagePage';
import { SolutionPlanningPage } from './pages/training/SolutionPlanningPage';
import { EnterprisePlanReviewPage } from './pages/training/EnterprisePlanReviewPage';
import { DataPanelPage } from './pages/zhixunplatform/DataPanelPage';
import { TaskManagementPage } from './pages/zhixunplatform/TaskManagementPage';
import { TaskArchivesPage } from './pages/zhixunplatform/TaskArchivesPage';
import { TrainingProcessPage } from './pages/zhixunplatform/TrainingProcessPage';
import { ProjectArchivesPage } from './pages/zhixunplatform/ProjectArchivesPage';
import { MaterialArchivesPage } from './pages/zhixunplatform/MaterialArchivesPage';
import { TalentIdentificationPage } from './pages/zhixunplatform/TalentIdentificationPage';
import { PreliminaryPlanPage } from './pages/zhixunplatform/PreliminaryPlanPage';
import { CreateTaskPage } from './pages/zhixunplatform/CreateTaskPage';
import { FinalPlanPage } from './pages/zhixunplatform/FinalPlanPage';
import { ClassDeclarationPage } from './pages/zhixunplatform/ClassDeclarationPage';
import { TrainingImplementationPage } from './pages/zhixunplatform/TrainingImplementationPage';
import { CertificationPage } from './pages/zhixunplatform/CertificationPage';
import { DeclarationServicePage } from './pages/zhixunplatform/DeclarationServicePage';
import { GraduationPage } from './pages/zhixunplatform/GraduationPage';
import { RefundPage } from './pages/zhixunplatform/RefundPage';
import { PersonnelArchivePage } from './pages/zhixunplatform/PersonnelArchivePage';
import { createArchiveTablePage } from './pages/zhixunplatform/ArchiveTablePage';
import { createPmFormPage } from './pages/zhixunplatform/PmFormPage';
import { TaskStartPage } from './pages/expert/TaskStartPage';
import { QuestionBankPage } from './pages/exam/QuestionBankPage';
import { CompanyQuestionPage } from './pages/exam/CompanyQuestionPage';
import { MakeExamPlanPage } from './pages/exam/MakeExamPlanPage';
import { KnowledgeRatePage } from './pages/exam/KnowledgeRatePage';
import { KnowledgeStructurePage } from './pages/exam/KnowledgeStructurePage';
import { FaqConversionPage } from './pages/exam/FaqConversionPage';
import { TrainExamListPage } from './pages/training/TrainExamListPage';
import { StatsOverviewPage } from './pages/exam/reviewSyncStats/StatsOverviewPage';
import { StudentDetailPage } from './pages/exam/reviewSyncStats/StudentDetailPage';
import { TabletAssessorApp } from './pages/assessor/TabletAssessorApp';
import { DefenseReviewApp } from './pages/assessor/DefenseReviewApp';
import { PracticalCollectionApp } from './pages/practicalCollection/PracticalCollectionApp';
import { CompetitionEventManagementPage } from './pages/competition/CompetitionEventManagementPage';
import { CompetitionPlayerManagementPage } from './pages/competition/CompetitionPlayerManagementPage';
import { CompetitionPlayerDrawDistributionPage, CompetitionPlayerDrawManagementPage } from './pages/competition/CompetitionPlayerDrawPage';
import { CompetitionRefereeManagementPage } from './pages/competition/CompetitionRefereeManagementPage';
import { CompetitionRefereeGroupPage } from './pages/competition/CompetitionRefereeGroupPage';
import { CompetitionEventModulePage } from './pages/competition/CompetitionEventModulePage';
import { CompetitionQuestionBankPage, CompetitionSimplePracticalQuestionPage } from './pages/competition/CompetitionQuestionBankPage';
import { CompetitionQuestionBlueprintPage } from './pages/competition/CompetitionQuestionBlueprintPage';
import { CompetitionMakeExamPlanPage } from './pages/competition/CompetitionMakeExamPlanPage';
import { CompetitionPracticalScoringSchemePage } from './pages/competition/CompetitionPracticalScoringSchemePage';
import { CompetitionPracticalJudgeApp } from './pages/competition/CompetitionPracticalJudgeApp';
import { FormalExamPlayerApp } from './pages/formalExam/FormalExamPlayerApp';
import { findCompetitionMenuCode, isCompetitionPath } from './routes/competitionRoute';
import { useAuthStore } from './stores/authStore';
import { useTabStore } from './stores/tabStore';
import { NavigationBlockerProvider, useNavigationBlocker } from './hooks/useUnsavedChangesBlocker';
// Pre-instantiate factory components for stable React references
const ArchiveEquipmentPage = createArchiveTablePage('equipment');
const ArchiveMaterialsPage = createArchiveTablePage('materials');
const ArchiveRulesPage = createArchiveTablePage('rules');
const ArchiveEnvironmentPage = createArchiveTablePage('environment');
const PmStrategicPage = createPmFormPage('strategic');
const PmOrgPerfPage = createPmFormPage('orgPerf');
const PmPbcPage = createPmFormPage('pbc');
const PmCharterPage = createPmFormPage('charter');
const PmMilestonePage = createPmFormPage('milestone');
const PmRiskPage = createPmFormPage('risk');
const PmIssuePage = createPmFormPage('issue');
const PmChangePage = createPmFormPage('change');
const PmMeetingPage = createPmFormPage('meeting');
const PmKnowledgePage = createPmFormPage('knowledge');
const PmWbsPage = createPmFormPage('wbs');
const PmCommunicationPage = createPmFormPage('communication');
const PmStatusPage = createPmFormPage('status');
import { ProductPlaceholder } from './pages/product/ProductPlaceholder';
import { TaskEndPage } from './pages/expert/TaskEndPage';
import { ProjectManagementPage } from './pages/certification/ProjectManagementPage';
import { CertificationDataPanelPage } from './pages/certification/CertificationDataPanelPage';
import { PracticalConfigurationPage } from './pages/certification/PracticalConfigurationPage';
import { PreparationPage } from './pages/certification/PreparationPage';
import { DefenseManagementPage } from './pages/certification/DefenseManagementPage';
import { CollectionTaskListPage } from './pages/certification/collection/CollectionTaskListPage';
import { CollectionManagePage } from './pages/certification/collection/CollectionManagePage';
import { ArchivePage } from './pages/certification/ArchivePage';
import { TaskArchivePage } from './pages/certification/TaskArchivePage';
import { AssessorInfoPage } from './pages/certification/info/AssessorInfoPage';
import { ExamSiteInfoPage } from './pages/certification/info/ExamSiteInfoPage';
import { ExamEquipmentInfoPage } from './pages/certification/info/ExamEquipmentInfoPage';
import { ExamStaffInfoPage } from './pages/certification/info/ExamStaffInfoPage';
import { TenantPage } from './pages/system/TenantPage';
import { UserPage } from './pages/system/UserPage';
import { OrgPage } from './pages/system/OrgPage';
import { PostPage } from './pages/system/PostPage';
import { RolePage } from './pages/system/RolePage';
import { MenuPage } from './pages/system/MenuPage';
import { DictPage } from './pages/system/DictPage';
import { AuditLogPage } from './pages/system/AuditLogPage';
import { PermissionPage } from './pages/system/PermissionPage';
import { appTheme } from './theme';
import './styles/tokens.css';
import './styles/global.css';
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: 1,
        },
    },
});
const pageMap = {
    knowledge: _jsx(KnowledgePage, {}),
    // 智能课程材料管理 > 细目表管理
    // 课程管理 > 资源管理（知识归属修正）
    question_resource: _jsx(ResourceCenterPage, {}),
    knowledge_fix: _jsx(KnowledgeFixPage, {}),
    xi_mu_biao_guan_li: _jsx(XiMuBiaoGuanLiPage, {}),
    // 智能课程材料管理 > 理论知识试题生成管理
    theory_question_gen: _jsx(TheoryQuestionGenPage, {}),
    // 智能课程材料管理 > 操作技能试题生成管理
    skill_question_gen: _jsx(SkillQuestionGenPage, {}),
    // 智能课程材料管理 > AI课件生成管理
    ai_courseware_gen: _jsx(AiCoursewarePage, {}),
    exam_work_type: _jsx(WorkTypePage, {}),
    dev_task_detail: _jsx(DevTaskDetailPage, {}),
    // 题库审核指派(管理端 3 子菜单,挂在开发任务下)
    task_exam_audit_assign: _jsx(TaskExamAuditAssignListPage, {}),
    task_examshicao_audit_assign: _jsx(TaskExamshicaoAuditAssignListPage, {}),
    task_examsub_audit_assign: _jsx(TaskExamsubAuditAssignListPage, {}),
    exam_course: _jsx(CoursePage, {}),
    'train_info-list': _jsx(TrainingInfoPage, {}),
    train_dashboard: _jsx(TrainingDashboardPage, {}),
    train_follow: _jsx(TrainingFollowClassPage, {}),
    train_follow_exam: _jsx(TrainingFollowExamPage, {}),
    train_graduation_ceremony: _jsx(TrainingGraduationCeremonyPage, {}),
    train_subsidy_declare: _jsx(TrainingSubsidyDeclarePage, {}),
    fen_ban: _jsx(TrainingClassDivisionPage, {}),
    'train_people-list': _jsx(TrainingClassPeoplePage, {}),
    train_people: _jsx(TrainingClassPeoplePage, {}),
    train_declares: _jsx(TrainingClassDeclarePage, {}),
    exam_user_auth_course: _jsx(UserAuthCoursePage, {}),
    chazheng_user_list: _jsx(UserListPage, {}),
    chazheng_cert_query: _jsx(CertQueryPage, {}),
    // 培训管理 → 战略规划
    pxyan_jiu: _jsx(EnterpriseResearchPage, {}),
    pxan_fang_gui_hua: _jsx(SolutionPlanningPage, {}),
    train_needs: _jsx(RequirementsManagementPage, {}),
    'train_demand-train': _jsx(EnterpriseTrainingDemandPage, {}),
    company_demand_train: _jsx(EnterpriseTrainingDemandPage, {}),
    company_plan_review: _jsx(EnterprisePlanReviewPage, {}),
    'train_demand-employee': _jsx(EnterpriseTalentDemandPage, {}),
    company_demand_employee: _jsx(EnterpriseTalentDemandPage, {}),
    train_solutions: _jsx(TrainingProgramPage, {}),
    train_schedule: _jsx(TrainingSchedulePage, {}),
    train_graduation: _jsx(TrainingGraduationPage, {}),
    'train_user-ability': _jsx(TrainingCapabilityPage, {}),
    train_summary: _jsx(TrainingSummaryPage, {}),
    train_certificates: _jsx(TrainingCertificateTemplatePage, {}),
    'train_sign-up': _jsx(TrainingInformationPage, {}),
    'train_sign-up-people': _jsx(TrainingInformationPage, {}),
    'train_user-info': _jsx(TrainingStudentPage, {}),
    'train_teacher-bank': _jsx(TrainingAcademicManagePage, {}),
    train_company: _jsx(EnterpriseManagePage, {}),
    knowledge_spaces: _jsx(KnowledgePage, {}),
    knowledge_search: _jsx(KnowledgePage, {}),
    knowledge_rencaishibie: _jsx(RencaishibieEmbedPage, {}),
    zhixun_data_panel: _jsx(DataPanelPage, {}),
    zhixun_task_management: _jsx(TaskManagementPage, {}),
    zhixun_task_archives: _jsx(TaskArchivesPage, {}),
    zhixun_training_process: _jsx(TrainingProcessPage, {}),
    zhixun_project_archives: _jsx(ProjectArchivesPage, {}),
    zhixun_material_archives: _jsx(MaterialArchivesPage, {}),
    zhixun_talent_identification: _jsx(TalentIdentificationPage, {}),
    zhixun_preliminary_plan: _jsx(PreliminaryPlanPage, {}),
    zhixun_create_task: _jsx(CreateTaskPage, {}),
    zhixun_final_plan: _jsx(FinalPlanPage, {}),
    zhixun_class_declaration: _jsx(ClassDeclarationPage, {}),
    zhixun_training_implementation: _jsx(TrainingImplementationPage, {}),
    zhixun_certification: _jsx(CertificationPage, {}),
    zhixun_declaration_service: _jsx(DeclarationServicePage, {}),
    zhixun_graduation: _jsx(GraduationPage, {}),
    zhixun_refund: _jsx(RefundPage, {}),
    zhixun_archive_personnel: _jsx(PersonnelArchivePage, {}),
    zhixun_archive_equipment: _jsx(ArchiveEquipmentPage, {}),
    zhixun_archive_materials: _jsx(ArchiveMaterialsPage, {}),
    zhixun_archive_rules: _jsx(ArchiveRulesPage, {}),
    zhixun_archive_environment: _jsx(ArchiveEnvironmentPage, {}),
    zhixun_table_strategic: _jsx(PmStrategicPage, {}),
    zhixun_table_org_perf: _jsx(PmOrgPerfPage, {}),
    zhixun_table_pbc: _jsx(PmPbcPage, {}),
    zhixun_table_charter: _jsx(PmCharterPage, {}),
    zhixun_table_milestone: _jsx(PmMilestonePage, {}),
    zhixun_table_risk: _jsx(PmRiskPage, {}),
    zhixun_table_issue: _jsx(PmIssuePage, {}),
    zhixun_table_change: _jsx(PmChangePage, {}),
    zhixun_table_meeting: _jsx(PmMeetingPage, {}),
    zhixun_table_knowledge: _jsx(PmKnowledgePage, {}),
    zhixun_table_wbs: _jsx(PmWbsPage, {}),
    zhixun_table_communication: _jsx(PmCommunicationPage, {}),
    zhixun_table_status: _jsx(PmStatusPage, {}),
    exam_question_bank: _jsx(QuestionBankPage, {}),
    gong_si_shi_ti_guan_li: _jsx(CompanyQuestionPage, {}),
    zu_juan_ji_hua: _jsx(MakeExamPlanPage, {}),
    'train_exam-list': _jsx(TrainExamListPage, {}),
    kao_shi_ji_hua: _jsx(TrainExamListPage, {}),
    exam_knowledge_rate: _jsx(KnowledgeRatePage, {}),
    zhi_shi_jie_gou_xi_mu_biao_guan_li: _jsx(KnowledgeStructurePage, {}),
    exam_faq_conversion: _jsx(FaqConversionPage, {}),
    expert_task_start: _jsx(TaskStartPage, {}),
    expert_task_end: _jsx(TaskEndPage, {}),
    certification_dashboard: _jsx(CertificationDataPanelPage, {}),
    certification_project: _jsx(ProjectManagementPage, {}),
    certification_preparation: _jsx(PreparationPage, {}),
    certification_practical_config: _jsx(PracticalConfigurationPage, {}),
    certification_defense_management: _jsx(DefenseManagementPage, {}),
    certification_practical_collection: _jsx(CollectionTaskListPage, {}),
    certification_archive: _jsx(ArchivePage, {}),
    certification_task_archive: _jsx(TaskArchivePage, {}),
    certification_info_assessor: _jsx(AssessorInfoPage, {}),
    certification_info_site: _jsx(ExamSiteInfoPage, {}),
    certification_info_equipment: _jsx(ExamEquipmentInfoPage, {}),
    certification_info_exam_staff: _jsx(ExamStaffInfoPage, {}),
    // System management pages
    system_tenant: _jsx(TenantPage, {}),
    system_user: _jsx(UserPage, {}),
    system_org: _jsx(OrgPage, {}),
    system_post: _jsx(PostPage, {}),
    system_role: _jsx(RolePage, {}),
    system_menu: _jsx(MenuPage, {}),
    system_dict: _jsx(DictPage, {}),
    system_audit_log: _jsx(AuditLogPage, {}),
    system_permission: _jsx(PermissionPage, {}),
    // kebab-case aliases for page-internal navigation (navStore.setSelectedKey)
    'certification-dashboard': _jsx(CertificationDataPanelPage, {}),
    'zhixun-data-panel': _jsx(DataPanelPage, {}),
    'zhixun-task-management': _jsx(TaskManagementPage, {}),
    'zhixun-task-archives': _jsx(TaskArchivesPage, {}),
    'zhixun-training-process': _jsx(TrainingProcessPage, {}),
    'zhixun-project-archives': _jsx(ProjectArchivesPage, {}),
    'zhixun-material-archives': _jsx(MaterialArchivesPage, {}),
    'zhixun-talent-identification': _jsx(TalentIdentificationPage, {}),
    'zhixun-preliminary-plan': _jsx(PreliminaryPlanPage, {}),
    'zhixun-create-task': _jsx(CreateTaskPage, {}),
    'zhixun-final-plan': _jsx(FinalPlanPage, {}),
    'zhixun-class-declaration': _jsx(ClassDeclarationPage, {}),
    'zhixun-training-implementation': _jsx(TrainingImplementationPage, {}),
    'zhixun-certification': _jsx(CertificationPage, {}),
    'zhixun-declaration-service': _jsx(DeclarationServicePage, {}),
    'zhixun-graduation': _jsx(GraduationPage, {}),
    'zhixun-refund': _jsx(RefundPage, {}),
    'zhixun-archive-personnel': _jsx(PersonnelArchivePage, {}),
    'zhixun-archive-equipment': _jsx(ArchiveEquipmentPage, {}),
    'zhixun-archive-materials': _jsx(ArchiveMaterialsPage, {}),
    'zhixun-archive-rules': _jsx(ArchiveRulesPage, {}),
    'zhixun-archive-environment': _jsx(ArchiveEnvironmentPage, {}),
    'zhixun-table-strategic': _jsx(PmStrategicPage, {}),
    'zhixun-table-org-perf': _jsx(PmOrgPerfPage, {}),
    'zhixun-table-pbc': _jsx(PmPbcPage, {}),
    'zhixun-table-charter': _jsx(PmCharterPage, {}),
    'zhixun-table-milestone': _jsx(PmMilestonePage, {}),
    'zhixun-table-risk': _jsx(PmRiskPage, {}),
    'zhixun-table-issue': _jsx(PmIssuePage, {}),
    'zhixun-table-change': _jsx(PmChangePage, {}),
    'zhixun-table-meeting': _jsx(PmMeetingPage, {}),
    'zhixun-table-knowledge': _jsx(PmKnowledgePage, {}),
    'zhixun-table-wbs': _jsx(PmWbsPage, {}),
    'zhixun-table-communication': _jsx(PmCommunicationPage, {}),
    'zhixun-table-status': _jsx(PmStatusPage, {}),
    'certification-task-archive': _jsx(TaskArchivePage, {}),
    'certification-practical-config': _jsx(PracticalConfigurationPage, {}),
    'certification-defense-management': _jsx(DefenseManagementPage, {}),
    // 战略规划 → 产品规划 模块
    strate_talent_standard: _jsx(ProductPlaceholder, { moduleCode: "strate_talent_standard" }),
    strate_talent_check: _jsx(ProductPlaceholder, { moduleCode: "strate_talent_check" }),
    product_ai_industry: _jsx(ProductPlaceholder, { moduleCode: "product_ai_industry" }),
    product_ai_college: _jsx(ProductPlaceholder, { moduleCode: "product_ai_college" }),
    product_ai_enterprise: _jsx(ProductPlaceholder, { moduleCode: "product_ai_enterprise" }),
    product_ai_individual: _jsx(ProductPlaceholder, { moduleCode: "product_ai_individual" }),
    product_course_dev: _jsx(ProductPlaceholder, { moduleCode: "product_course_dev" }),
    product_knowledge_nonferrous: _jsx(ProductPlaceholder, { moduleCode: "product_knowledge_nonferrous" }),
    product_qianlv_ecosystem: _jsx(ProductPlaceholder, { moduleCode: "product_qianlv_ecosystem" }),
    product_simulation: _jsx(ProductPlaceholder, { moduleCode: "product_simulation" }),
    product_training_base: _jsx(ProductPlaceholder, { moduleCode: "product_training_base" }),
    product_apprentice: _jsx(ProductPlaceholder, { moduleCode: "product_apprentice" }),
    product_cert_agency: _jsx(ProductPlaceholder, { moduleCode: "product_cert_agency" }),
    product_competition: _jsx(ProductPlaceholder, { moduleCode: "product_competition" }),
    product_studio_apply: _jsx(ProductPlaceholder, { moduleCode: "product_studio_apply" }),
    product_course_ops: _jsx(ProductPlaceholder, { moduleCode: "product_course_ops" }),
    product_ai_nonferrous_assess: _jsx(ProductPlaceholder, { moduleCode: "product_ai_nonferrous_assess" }),
    product_qianlv_knowledge: _jsx(ProductPlaceholder, { moduleCode: "product_qianlv_knowledge" }),
    product_smart_classroom: _jsx(ProductPlaceholder, { moduleCode: "product_smart_classroom" }),
    product_expert_system: _jsx(ProductPlaceholder, { moduleCode: "product_expert_system" }),
    product_ai_learning: _jsx(ProductPlaceholder, { moduleCode: "product_ai_learning" }),
    product_ai_assess: _jsx(ProductPlaceholder, { moduleCode: "product_ai_assess" }),
    product_ai_agency_apply: _jsx(ProductPlaceholder, { moduleCode: "product_ai_agency_apply" }),
    product_ai_individual_apply: _jsx(ProductPlaceholder, { moduleCode: "product_ai_individual_apply" }),
    product_ai_class_manage: _jsx(ProductPlaceholder, { moduleCode: "product_ai_class_manage" }),
    product_ai_skill_manage: _jsx(ProductPlaceholder, { moduleCode: "product_ai_skill_manage" }),
    // 业务规划 13 个部门
    biz_product_design: _jsx(ProductPlaceholder, { moduleCode: "biz_product_design" }),
    biz_market_expand: _jsx(ProductPlaceholder, { moduleCode: "biz_market_expand" }),
    biz_course_dev: _jsx(ProductPlaceholder, { moduleCode: "biz_course_dev" }),
    biz_agent_dev: _jsx(ProductPlaceholder, { moduleCode: "biz_agent_dev" }),
    biz_cloud_dev: _jsx(ProductPlaceholder, { moduleCode: "biz_cloud_dev" }),
    biz_simulation_dev: _jsx(ProductPlaceholder, { moduleCode: "biz_simulation_dev" }),
    biz_training_ops: _jsx(ProductPlaceholder, { moduleCode: "biz_training_ops" }),
    biz_cert_ops: _jsx(ProductPlaceholder, { moduleCode: "biz_cert_ops" }),
    biz_competition_ops: _jsx(ProductPlaceholder, { moduleCode: "biz_competition_ops" }),
    biz_hr: _jsx(ProductPlaceholder, { moduleCode: "biz_hr" }),
    biz_finance: _jsx(ProductPlaceholder, { moduleCode: "biz_finance" }),
    biz_admin: _jsx(ProductPlaceholder, { moduleCode: "biz_admin" }),
    biz_platform_coop: _jsx(ProductPlaceholder, { moduleCode: "biz_platform_coop" }),
    // 人才规划
    talent_three_year: _jsx(ProductPlaceholder, { moduleCode: "talent_three_year" }),
    talent_five_year: _jsx(ProductPlaceholder, { moduleCode: "talent_five_year" }),
    // 计划管理
    plan_work_year: _jsx(ProductPlaceholder, { moduleCode: "plan_work_year" }),
    plan_work_quarter: _jsx(ProductPlaceholder, { moduleCode: "plan_work_quarter" }),
    plan_work_month: _jsx(ProductPlaceholder, { moduleCode: "plan_work_month" }),
    plan_train_year: _jsx(ProductPlaceholder, { moduleCode: "plan_train_year" }),
    plan_train_quarter: _jsx(ProductPlaceholder, { moduleCode: "plan_train_quarter" }),
    plan_train_month: _jsx(ProductPlaceholder, { moduleCode: "plan_train_month" }),
    // 运营流程 - 线索招生
    ops_lead_collect: _jsx(ProductPlaceholder, { moduleCode: "ops_lead_collect" }),
    ops_lead_assign: _jsx(ProductPlaceholder, { moduleCode: "ops_lead_assign" }),
    ops_lead_follow: _jsx(ProductPlaceholder, { moduleCode: "ops_lead_follow" }),
    ops_lead_convert: _jsx(ProductPlaceholder, { moduleCode: "ops_lead_convert" }),
    // 运营流程 - 客户管理
    ops_customer_archive: _jsx(ProductPlaceholder, { moduleCode: "ops_customer_archive" }),
    ops_customer_tag: _jsx(ProductPlaceholder, { moduleCode: "ops_customer_tag" }),
    ops_customer_intent: _jsx(ProductPlaceholder, { moduleCode: "ops_customer_intent" }),
    ops_customer_visit: _jsx(ProductPlaceholder, { moduleCode: "ops_customer_visit" }),
    ops_customer_remind: _jsx(ProductPlaceholder, { moduleCode: "ops_customer_remind" }),
    // 运营流程 - 订单财务
    ops_order_center: _jsx(ProductPlaceholder, { moduleCode: "ops_order_center" }),
    ops_contract_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_contract_mgmt" }),
    ops_receipt_record: _jsx(ProductPlaceholder, { moduleCode: "ops_receipt_record" }),
    ops_invoice_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_invoice_mgmt" }),
    ops_settlement: _jsx(ProductPlaceholder, { moduleCode: "ops_settlement" }),
    // 运营流程 - 营销推广
    ops_enroll_brochure: _jsx(ProductPlaceholder, { moduleCode: "ops_enroll_brochure" }),
    ops_activity_mkt: _jsx(ProductPlaceholder, { moduleCode: "ops_activity_mkt" }),
    ops_coupon_card: _jsx(ProductPlaceholder, { moduleCode: "ops_coupon_card" }),
    ops_channel_qrcode: _jsx(ProductPlaceholder, { moduleCode: "ops_channel_qrcode" }),
    ops_distribution: _jsx(ProductPlaceholder, { moduleCode: "ops_distribution" }),
    // 运营流程 - 数据看板
    ops_dashboard_funnel: _jsx(ProductPlaceholder, { moduleCode: "ops_dashboard_funnel" }),
    ops_dashboard_rank: _jsx(ProductPlaceholder, { moduleCode: "ops_dashboard_rank" }),
    ops_dashboard_convert: _jsx(ProductPlaceholder, { moduleCode: "ops_dashboard_convert" }),
    ops_dashboard_target: _jsx(ProductPlaceholder, { moduleCode: "ops_dashboard_target" }),
    ops_dashboard_roi: _jsx(ProductPlaceholder, { moduleCode: "ops_dashboard_roi" }),
    // 运营流程 - 课程开发流程
    ops_std_dev_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_std_dev_flow" }),
    ops_question_dev_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_question_dev_flow" }),
    ops_video_dev_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_video_dev_flow" }),
    ops_simulation_dev_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_simulation_dev_flow" }),
    ops_tech_doc_dev_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_tech_doc_dev_flow" }),
    // 运营流程 - 培训运营流程
    ops_train_demand_survey: _jsx(ProductPlaceholder, { moduleCode: "ops_train_demand_survey" }),
    ops_train_solution_design: _jsx(ProductPlaceholder, { moduleCode: "ops_train_solution_design" }),
    ops_train_class_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_train_class_mgmt" }),
    ops_train_implementation: _jsx(ProductPlaceholder, { moduleCode: "ops_train_implementation" }),
    ops_train_cert_issue: _jsx(ProductPlaceholder, { moduleCode: "ops_train_cert_issue" }),
    ops_train_graduation: _jsx(TrainingGraduationCeremonyPage, {}),
    ops_train_project_close: _jsx(ProductPlaceholder, { moduleCode: "ops_train_project_close" }),
    ops_train_project_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_train_project_apply" }),
    // 培训运营 - 需求调研
    ops_talent_demand: _jsx(ProductPlaceholder, { moduleCode: "ops_talent_demand" }),
    ops_training_demand: _jsx(ProductPlaceholder, { moduleCode: "ops_training_demand" }),
    // 培训运营 - 方案设计
    ops_class_split_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_class_split_setting" }),
    ops_schedule_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_schedule_setting" }),
    ops_incentive_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_incentive_setting" }),
    ops_budget_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_budget_setting" }),
    ops_ai_learning_task_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_ai_learning_task_setting" }),
    ops_academic_division_setting: _jsx(ProductPlaceholder, { moduleCode: "ops_academic_division_setting" }),
    // 培训运营 - 开班管理
    ops_material_generate: _jsx(ProductPlaceholder, { moduleCode: "ops_material_generate" }),
    ops_material_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_material_apply" }),
    ops_mobilization_meeting: _jsx(ProductPlaceholder, { moduleCode: "ops_mobilization_meeting" }),
    // 培训运营 - 培训实施
    ops_attendance_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_attendance_mgmt" }),
    ops_pass_rate_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_pass_rate_mgmt" }),
    ops_homework_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_homework_mgmt" }),
    ops_evaluation_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_evaluation_mgmt" }),
    ops_record_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_record_mgmt" }),
    // 培训运营 - 项目结题
    ops_subsidy_material_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_subsidy_material_apply" }),
    ops_subsidy_material_export: _jsx(ProductPlaceholder, { moduleCode: "ops_subsidy_material_export" }),
    // 培训运营 - 项目申报
    ops_qianlv_reward_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_qianlv_reward_apply" }),
    ops_gov_reward_guide: _jsx(ProductPlaceholder, { moduleCode: "ops_gov_reward_guide" }),
    ops_enterprise_reward_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_enterprise_reward_apply" }),
    ops_college_reward_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_college_reward_apply" }),
    ops_student_service_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_student_service_flow" }),
    ops_title_apply_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_title_apply_flow" }),
    ops_subsidy_apply_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_subsidy_apply_flow" }),
    ops_points_exchange_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_points_exchange_flow" }),
    ops_expert_apply_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_expert_apply_flow" }),
    ops_honor_apply_flow: _jsx(ProductPlaceholder, { moduleCode: "ops_honor_apply_flow" }),
    // 认定运营流程
    ops_cert_notice_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_notice_mgmt" }),
    ops_cert_qualification: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_qualification" }),
    ops_cert_plan_mgmt: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_plan_mgmt" }),
    ops_cert_exam_arrange: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_exam_arrange" }),
    ops_cert_pre_exam_prep: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_pre_exam_prep" }),
    ops_cert_exam_meeting: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_exam_meeting" }),
    ops_cert_assessment: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_assessment" }),
    ops_cert_score_publish: _jsx(ProductPlaceholder, { moduleCode: "ops_cert_score_publish" }),
    // 竞赛运营流程
    ops_comp_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_apply" }),
    ops_comp_register: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_register" }),
    ops_comp_tech_seminar: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_tech_seminar" }),
    ops_comp_enterprise_sel: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_enterprise_sel" }),
    ops_comp_preliminary: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_preliminary" }),
    ops_comp_semifinal: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_semifinal" }),
    ops_comp_pre_training: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_pre_training" }),
    ops_comp_opening: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_opening" }),
    ops_comp_final: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_final" }),
    ops_comp_closing: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_closing" }),
    ops_comp_project_close: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_project_close" }),
    // 竞赛结题
    ops_comp_close_report: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_close_report" }),
    ops_comp_close_settlement: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_close_settlement" }),
    ops_comp_close_apply: _jsx(ProductPlaceholder, { moduleCode: "ops_comp_close_apply" }),
    // 支持保障 - 政策库
    sup_policy_national: _jsx(ProductPlaceholder, { moduleCode: "sup_policy_national" }),
    sup_policy_provincial: _jsx(ProductPlaceholder, { moduleCode: "sup_policy_provincial" }),
    sup_policy_city: _jsx(ProductPlaceholder, { moduleCode: "sup_policy_city" }),
    sup_policy_company: _jsx(ProductPlaceholder, { moduleCode: "sup_policy_company" }),
    // 支持保障 - 专家库
    sup_expert_question: _jsx(ProductPlaceholder, { moduleCode: "sup_expert_question" }),
    sup_trainer_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_trainer_mgmt" }),
    sup_assessor_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_assessor_mgmt" }),
    sup_referee_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_referee_mgmt" }),
    sup_academic_staff: _jsx(ProductPlaceholder, { moduleCode: "sup_academic_staff" }),
    sup_exam_staff: _jsx(ProductPlaceholder, { moduleCode: "sup_exam_staff" }),
    sup_comp_staff: _jsx(ProductPlaceholder, { moduleCode: "sup_comp_staff" }),
    // 支持保障 - 设备库
    sup_equip_practical: _jsx(ProductPlaceholder, { moduleCode: "sup_equip_practical" }),
    sup_equip_training: _jsx(ProductPlaceholder, { moduleCode: "sup_equip_training" }),
    sup_equip_cert: _jsx(ProductPlaceholder, { moduleCode: "sup_equip_cert" }),
    sup_equip_comp: _jsx(ProductPlaceholder, { moduleCode: "sup_equip_comp" }),
    // 支持保障 - 物料库
    sup_job_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_job_mgmt" }),
    sup_post_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_post_mgmt" }),
    sup_standard_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_standard_mgmt" }),
    sup_question_bank: _jsx(ProductPlaceholder, { moduleCode: "sup_question_bank" }),
    sup_textbook_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_textbook_mgmt" }),
    sup_video_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_video_mgmt" }),
    sup_material_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_material_mgmt" }),
    sup_ad_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_ad_mgmt" }),
    sup_logo_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_logo_mgmt" }),
    sup_budget_mgmt: _jsx(ProductPlaceholder, { moduleCode: "sup_budget_mgmt" }),
    // 支持保障 - 方法库
    sup_rule_standard: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_standard" }),
    sup_rule_question: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_question" }),
    sup_rule_textbook: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_textbook" }),
    sup_rule_video: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_video" }),
    sup_rule_practical: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_practical" }),
    sup_rule_forum: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_forum" }),
    sup_rule_training: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_training" }),
    sup_rule_cert: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_cert" }),
    sup_rule_comp: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_comp" }),
    sup_rule_ceremony: _jsx(ProductPlaceholder, { moduleCode: "sup_rule_ceremony" }),
    // 支持保障 - 环境库
    sup_env_forum: _jsx(ProductPlaceholder, { moduleCode: "sup_env_forum" }),
    sup_env_question_accept: _jsx(ProductPlaceholder, { moduleCode: "sup_env_question_accept" }),
    sup_env_ceremony: _jsx(ProductPlaceholder, { moduleCode: "sup_env_ceremony" }),
    sup_env_training_class: _jsx(ProductPlaceholder, { moduleCode: "sup_env_training_class" }),
    sup_env_cert_exam: _jsx(ProductPlaceholder, { moduleCode: "sup_env_cert_exam" }),
    sup_env_comp_site: _jsx(ProductPlaceholder, { moduleCode: "sup_env_comp_site" }),
    // 绩效人才
    perf_assess_structure: _jsx(ProductPlaceholder, { moduleCode: "perf_assess_structure" }),
    perf_assess_qualification: _jsx(ProductPlaceholder, { moduleCode: "perf_assess_qualification" }),
    perf_assess_resource: _jsx(ProductPlaceholder, { moduleCode: "perf_assess_resource" }),
    perf_skill_map: _jsx(ProductPlaceholder, { moduleCode: "perf_skill_map" }),
    perf_pipeline_build: _jsx(ProductPlaceholder, { moduleCode: "perf_pipeline_build" }),
    perf_talent_map: _jsx(ProductPlaceholder, { moduleCode: "perf_talent_map" }),
    // 机构风控
    risk_product_design: _jsx(ProductPlaceholder, { moduleCode: "risk_product_design" }),
    risk_market_expand: _jsx(ProductPlaceholder, { moduleCode: "risk_market_expand" }),
    risk_course_dev: _jsx(ProductPlaceholder, { moduleCode: "risk_course_dev" }),
    risk_agent_dev: _jsx(ProductPlaceholder, { moduleCode: "risk_agent_dev" }),
    risk_cloud_dev: _jsx(ProductPlaceholder, { moduleCode: "risk_cloud_dev" }),
    risk_simulation_dev: _jsx(ProductPlaceholder, { moduleCode: "risk_simulation_dev" }),
    risk_training_ops: _jsx(ProductPlaceholder, { moduleCode: "risk_training_ops" }),
    risk_cert_ops: _jsx(ProductPlaceholder, { moduleCode: "risk_cert_ops" }),
    risk_competition_ops: _jsx(ProductPlaceholder, { moduleCode: "risk_competition_ops" }),
    risk_hr: _jsx(ProductPlaceholder, { moduleCode: "risk_hr" }),
    risk_finance: _jsx(ProductPlaceholder, { moduleCode: "risk_finance" }),
    risk_admin: _jsx(ProductPlaceholder, { moduleCode: "risk_admin" }),
    risk_platform_coop: _jsx(ProductPlaceholder, { moduleCode: "risk_platform_coop" }),
    'certification-practical-collection': _jsx(CollectionTaskListPage, {}),
    competition_event: _jsx(CompetitionEventManagementPage, {}),
    competition_player: _jsx(CompetitionPlayerManagementPage, {}),
    competition_player_info: _jsx(CompetitionPlayerManagementPage, {}),
    competition_player_draw_distribution: _jsx(CompetitionPlayerDrawDistributionPage, {}),
    competition_player_draw_management: _jsx(CompetitionPlayerDrawManagementPage, {}),
    competition_referee: _jsx(CompetitionRefereeManagementPage, {}),
    competition_referee_info: _jsx(CompetitionRefereeManagementPage, {}),
    competition_referee_group: _jsx(CompetitionRefereeGroupPage, {}),
    competition_stage: _jsx(CompetitionModuleShell, { module: "stages" }),
    competition_notice: _jsx(CompetitionModuleShell, { module: "notices" }),
    competition_theory_exam: _jsx(CompetitionModuleShell, { module: "theory-exam" }),
    competition_formal_exam: _jsx(CompetitionModuleShell, { module: "formal-exam-console" }),
    competition_practical_exam: _jsx(CompetitionModuleShell, { module: "practical-exam-console" }),
    competition_score_ranking: _jsx(CompetitionModuleShell, { module: "scores" }),
    competition_theory_question: _jsx(CompetitionQuestionBankPage, { contentType: "theory" }),
    competition_practical_question: _jsx(CompetitionQuestionBankPage, { contentType: "practical" }),
    competition_simple_theory_question: _jsx(CompetitionQuestionBankPage, { contentType: "theory", simpleMode: true }),
    competition_simple_practical_question: _jsx(CompetitionSimplePracticalQuestionPage, {}),
    competition_theory_blueprint: _jsx(CompetitionQuestionBlueprintPage, { contentType: "theory" }),
    competition_practical_blueprint: _jsx(CompetitionQuestionBlueprintPage, { contentType: "practical" }),
    competition_theory_make_exam_plan: _jsx(CompetitionMakeExamPlanPage, { contentType: "theory" }),
    competition_practical_make_exam_plan: _jsx(CompetitionMakeExamPlanPage, { contentType: "practical" }),
    competition_simple_theory_make_exam_plan: _jsx(CompetitionMakeExamPlanPage, { contentType: "theory", simpleMode: true }),
    competition_simple_practical_make_exam_plan: _jsx(CompetitionPracticalScoringSchemePage, {}),
};
function useBrowserPathname() {
    const [pathname, setPathname] = React.useState(() => window.location.pathname);
    React.useEffect(() => {
        const syncPathname = () => setPathname(window.location.pathname);
        window.addEventListener('popstate', syncPathname);
        return () => window.removeEventListener('popstate', syncPathname);
    }, []);
    return pathname;
}
function CollectionManageShell({ taskId, examSessionId }) {
    const [manageActive, setManageActive] = React.useState(true);
    // §3.7 覆盖态标签：原子入口记录 overridePath（D1）——先取当前完整 URL 再入 store，
    // openOverrideTab 全程不触碰 URL，杜绝"菜单 path 改写直达 URL"连锁丢页。
    React.useEffect(() => {
        const overrideUrl = window.location.pathname + window.location.search;
        useTabStore.getState().openOverrideTab('certification_practical_collection', undefined, overrideUrl);
    }, [taskId]);
    return (_jsx(AppShell, { pageMap: pageMap, selectedMenuKeyOverride: manageActive ? 'certification_practical_collection' : undefined, contentOverride: manageActive ? (_jsx(CollectionManagePage, { taskId: taskId, examSessionId: examSessionId })) : undefined, onMenuNavigate: () => setManageActive(false) }));
}
function CompetitionModuleShell({ module }) {
    const { confirmNavigation } = useNavigationBlocker();
    const searchParams = new URLSearchParams(window.location.search);
    const navigate = async (path) => {
        if (!(await confirmNavigation()))
            return;
        window.history.pushState({}, '', path);
        window.dispatchEvent(new PopStateEvent('popstate'));
    };
    return (_jsx(CompetitionEventModulePage, { module: module, onNavigate: navigate, initialEventId: searchParams.get('eventId') || undefined, initialStageId: searchParams.get('stageId') || undefined, initialEventWorkTypeId: searchParams.get('eventWorkTypeId') || undefined }));
}
function CompetitionEventShell({ pathname }) {
    const [eventActive, setEventActive] = React.useState(true);
    const menuTree = useAuthStore((state) => state.menuTree);
    const competitionMenuKey = findCompetitionMenuCode(menuTree, pathname) || 'competition_event';
    const competitionPage = pageMap[competitionMenuKey] || pageMap.competition_event;
    React.useEffect(() => {
        setEventActive(true);
        // §3.7 覆盖态标签（D1 + 幽灵标签守卫）：menuTree 未到时不开标签（避免兜底码开幽灵标签），
        // 到达后按真实菜单码 openOverrideTab——原子记录 overridePath，全程不触碰 URL。
        if (!menuTree.length)
            return;
        const overrideUrl = window.location.pathname + window.location.search;
        useTabStore.getState().openOverrideTab(competitionMenuKey, undefined, overrideUrl);
    }, [pathname, competitionMenuKey, menuTree.length]);
    return (_jsx(AppShell, { pageMap: pageMap, selectedMenuKeyOverride: eventActive ? competitionMenuKey : undefined, contentOverride: eventActive ? _jsx(React.Fragment, { children: competitionPage }, pathname) : undefined, onMenuNavigate: (node) => {
            if (node?.path && isCompetitionPath(node.path)) {
                setEventActive(true);
                if (node.path !== window.location.pathname) {
                    window.history.pushState({}, '', node.path);
                    window.dispatchEvent(new PopStateEvent('popstate'));
                }
                return;
            }
            setEventActive(false);
        }, hideBreadcrumb: eventActive }));
}
/** AI 同步学统计隐藏路由：/training/exam-lists/:examId/review-sync-stats[/student/:planId]
 *  （《AI同步学统计功能设计方案》v1.1 §6.1：仅考试管理列表页行内按钮进入，不进菜单） */
function isReviewSyncStatsPath(pathname) {
    return /^\/training\/exam-lists\/\d+\/review-sync-stats/.test(pathname);
}
function ReviewSyncStatsShell({ pathname }) {
    const [statsActive, setStatsActive] = React.useState(true);
    const match = pathname.match(/^\/training\/exam-lists\/(\d+)\/review-sync-stats(?:\/student\/(\d+))?\/?$/);
    const examId = match ? Number(match[1]) : 0;
    const planId = match?.[2];
    React.useEffect(() => {
        setStatsActive(true);
        // §3.7 覆盖态标签（D1）：先取当前完整 URL 再入 store，原子记录 overridePath 不触碰 URL
        const overrideUrl = window.location.pathname + window.location.search;
        useTabStore.getState().openOverrideTab('kao_shi_ji_hua', undefined, overrideUrl);
    }, [pathname]);
    return (_jsx(AppShell, { pageMap: pageMap, selectedMenuKeyOverride: statsActive ? 'kao_shi_ji_hua' : undefined, contentOverride: statsActive ? (_jsx(React.Fragment, { children: planId
                ? _jsx(StudentDetailPage, { examId: examId, planId: planId })
                : _jsx(StatsOverviewPage, { examId: examId }) }, pathname)) : undefined, onMenuNavigate: () => setStatsActive(false), hideBreadcrumb: statsActive }));
}
/** 毕业典礼班级详情隐藏路由：/training/graduation/classes/:classId */
function isGraduationClassDetailPath(pathname) {
    return /^\/training\/graduation\/classes\/\d+\/?$/.test(pathname);
}
function GraduationClassDetailShell({ pathname }) {
    const [detailActive, setDetailActive] = React.useState(true);
    const match = pathname.match(/^\/training\/graduation\/classes\/(\d+)\/?$/);
    const classId = match ? Number(match[1]) : 0;
    React.useEffect(() => {
        setDetailActive(true);
        // §3.7 覆盖态标签（D1）：先取当前完整 URL 再入 store，原子记录 overridePath 不触碰 URL
        const overrideUrl = window.location.pathname + window.location.search;
        useTabStore.getState().openOverrideTab('train_graduation_ceremony', undefined, overrideUrl);
    }, [pathname]);
    return (_jsx(AppShell, { pageMap: pageMap, selectedMenuKeyOverride: detailActive ? 'train_graduation_ceremony' : undefined, contentOverride: detailActive ? (_jsx(React.Fragment, { children: _jsx(TrainingGraduationClassDetailPage, { classId: classId }) }, pathname)) : undefined, onMenuNavigate: () => setDetailActive(false), hideBreadcrumb: detailActive }));
}
function App() {
    const pathname = useBrowserPathname();
    const formalExamMode = pathname === '/formal-exam' || pathname.startsWith('/formal-exam/');
    const assessorMode = pathname.startsWith('/assessor-scoring');
    const defenseMode = pathname.startsWith('/assessor-defense');
    const practicalCollectionMode = pathname.startsWith('/practical-collection');
    const competitionJudgeMode = pathname.startsWith('/competition-practical-judge');
    const collectionManageTaskId = pathname.match(/^\/certification\/practical-collection\/([^/]+)\/manage\/?$/)?.[1];
    const collectionManageExamSessionId = new URLSearchParams(window.location.search).get('examSessionId') || undefined;
    const collectionManageMode = Boolean(collectionManageTaskId);
    const competitionEventMode = isCompetitionPath(pathname);
    const reviewSyncStatsMode = isReviewSyncStatsPath(pathname);
    const graduationClassDetailMode = isGraduationClassDetailPath(pathname);
    return (_jsx(NavigationBlockerProvider, { children: formalExamMode ? (_jsx(FormalExamPlayerApp, {})) : (_jsx(AuthGate, { mode: assessorMode ? 'assessor' : defenseMode ? 'defense' : practicalCollectionMode ? 'collection' : competitionJudgeMode ? 'competitionJudge' : 'admin', children: competitionJudgeMode ? (_jsx(CompetitionPracticalJudgeApp, {})) : assessorMode ? (_jsx(TabletAssessorApp, {})) : defenseMode ? (_jsx(DefenseReviewApp, {})) : practicalCollectionMode ? (_jsx(PracticalCollectionApp, {})) : collectionManageMode ? (_jsx(CollectionManageShell, { taskId: decodeURIComponent(collectionManageTaskId), examSessionId: collectionManageExamSessionId })) : graduationClassDetailMode ? (_jsx(GraduationClassDetailShell, { pathname: pathname })) : reviewSyncStatsMode ? (_jsx(ReviewSyncStatsShell, { pathname: pathname })) : competitionEventMode ? (_jsx(CompetitionEventShell, { pathname: pathname })) : (_jsx(AppShell, { pageMap: pageMap })) })) }));
}
ReactDOM.createRoot(document.getElementById('root')).render(_jsx(React.StrictMode, { children: _jsx(ConfigProvider, { locale: zhCN, theme: appTheme, children: _jsx(QueryClientProvider, { client: queryClient, children: _jsx(App, {}) }) }) }));
