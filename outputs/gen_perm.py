mport sys

# Permission codes that controllers require but are MISSING from seed data
# Format: (resource_group, display_name, menu_component_path)
MISSING = [
    ("info-list", "培训信息", "training/TrainingInfo"),
    ("info-class", "开班信息", "training/TrainingInfoClass"),
    ("class-list", "分班管理", "training/TrainingClassList"),
    ("course-kejian", "课件管理", "training/TrainingCourseKejian"),
    ("exam-list", "考试列表", "training/TrainingExamList"),
    ("exam-result", "考试结果", "training/TrainingExamResult"),
    ("task-list", "任务列表", "training/TrainingTaskList"),
    ("task-info", "任务信息", "training/TrainingTaskInfo"),
    ("task-info-detail", "任务详情", "training/TrainingTaskInfoDetail"),
    ("task-log", "任务日志", "training/TrainingTaskLog"),
    ("people-list", "人员列表", "training/TrainingPeopleList"),
    ("project", "培训项目", "training/TrainingProject"),
    ("project-table", "项目管理表", "training/TrainingProjectTable"),
    ("protocol", "培训协议", "training/TrainingProtocol"),
    ("settlement", "培训结算", "training/TrainingSettlement"),
    ("subsidy", "培训补贴", "training/TrainingSubsidy"),
    ("summary", "培训总结", "training/TrainingSummary"),
    ("talent-needs", "人才需求", "training/TrainingTalentNeeds"),
    ("teacher-bank", "教师库", "training/TrainingTeacherBank"),
    ("user-info", "学员信息", "training/TrainingUserInfo"),
    ("user-ability", "学员能力", "training/TrainingUserAbility"),
    ("user-course-paper", "学员课程试卷", "training/TrainingUserCoursePaper"),
    ("user-course-plan", "学员课程计划", "training/TrainingUserCoursePlan"),
    ("user-exam-log", "学员考试日志", "training/TrainingUserExamLog"),
    ("user-learn-log", "学员学习日志", "training/TrainingUserLearnLog"),
    ("user-paper", "学员试卷", "training/TrainingUserPaper"),
    ("user-patent", "学员专利", "training/TrainingUserPatent"),
    ("archive", "档案管理", "training/TrainingArchive"),
    ("attendance", "考勤管理", "training/TrainingAttendance"),
    ("dashboard", "培训仪表盘", "training/TrainingDashboard"),
    ("file-list", "文件列表", "training/TrainingFileList"),
    ("file-detail", "文件详情", "training/TrainingFileDetail"),
    ("schedule", "排课管理", "training/TrainingSchedule"),
]

# Actions that need both standard CRUD and special actions
EXTRA_ACTIONS = {
    "people-list": ["audit"],
    "class-list": ["assign"],
}

PARENT_MENU_ID = 4300
ROLE_ID = 2000
TENANT_ID = 1

menu_start = 4900
perm_start = 5400
rm_start = 604800
rp_start = 705300

menu_sql = []
perm_sql = []
rm_sql = []
rp_sql = []

mi = menu_start
pi = perm_start
rmi = rm_start
rpi = rp_start

for resource, display, component in MISSING:
    # Menu entry
    perm_code_view = f"train:{resource}:view"
    menu_sql.append(
        f"({mi}, {TENANT_ID}, {PARENT_MENU_ID}, 'page', 'train_{resource}', "
        f"'{display}', '/training/{resource}', '{component}', "
        f"'{perm_code_view}', 'AppstoreOutlined', {mi - 4890}, 1, 'enabled', NOW(), NOW(), 1, 1, 0)"
    )
    
    actions = ['view', 'create', 'update', 'delete']
    if resource in EXTRA_ACTIONS:
        actions.extend(EXTRA_ACTIONS[resource])
    
    for act in actions:
        code = f"train:{resource}:{act}"
        act_cn = {'view': '查看', 'create': '新增', 'update': '编辑', 'delete': '删除', 'audit': '审核', 'assign': '分配'}[act]
        perm_sql.append(
            f"({pi}, {TENANT_ID}, {mi}, '{code}', '{act_cn}{display}', '{act}', 'enabled', NOW(), NOW(), 1, 1, 0)"
        )
        rp_sql.append(
            f"({rpi}, {TENANT_ID}, {ROLE_ID}, {pi}, NOW(), NOW(), 1, 1, 0)"
        )
        pi += 1
        rpi += 1
    
    rm_sql.append(f"({rmi}, {TENANT_ID}, {ROLE_ID}, {mi}, NOW(), NOW(), 1, 1, 0)")
    mi += 1
    rmi += 1

# Also add handler for existing menus that have mismatched permission codes
# The existing menus (4801-4815) have simpler permission codes but controllers use hyphenated ones.
# We need to also add the hyphenated permission codes for existing resources.
EXISTING_FIX = [
    ("4801", "need", "培训需求"),       # already has train:need:*, but controllers also OK
    ("4802", "solution", "培训方案"),  
    ("4803", "request", "培训请求"),
    ("4804", "declare", "培训申报"),
    ("4805", "people", "培训人员"),     # controllers use train:people-list:* instead of train:people:*
    ("4806", "class", "班级管理"),      # controllers use train:class-list:* instead of train:class:*
    ("4807", "course", "课程管理"),
    ("4808", "resource", "资源管理"),
    ("4809", "task", "任务管理"),       # but we already added task-list, task-info, etc. above
    ("4810", "exam", "考试管理"),       # controllers use train:exam-list:* and train:exam-result:* instead of train:exam:*
    ("4811", "budget", "预算管理"),
    ("4812", "certificate", "认定管理"),
    ("4813", "talent", "人才需求"),     # controllers use train:talent-needs:* instead of train:talent:*
    ("4814", "teacher", "教师库"),      # controllers use train:teacher-bank:* instead of train:teacher:*
    ("4815", "file", "档案管理"),       # controllers use train:file-list:* and train:file-detail:* instead of train:file:*
]

# Print SQL
print("-- Add missing training department permissions")
print("-- Auto-generated to match all trainingdepartment controller @PreAuthorize annotations\n")

print("-- 1. Add missing menu items for training sub-modules")
print("INSERT IGNORE INTO sys_menu (")
print("  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code, icon, sort_order, visible,")
print("  status, created_at, updated_at, created_by, updated_by, deleted")
print(") VALUES")
print(",\n".join(f"  {m}" for m in menu_sql) + ";\n")

print("-- 2. Add missing permissions")
print("INSERT IGNORE INTO sys_permission (")
print("  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted")
print(") VALUES")
print(",\n".join(f"  {p}" for p in perm_sql) + ";\n")

print("-- 3. Grant menus to PLATFORM_ADMIN role")
print("INSERT IGNORE INTO sys_role_menu (")
print("  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted")
print(") VALUES")
print(",\n".join(f"  {r}" for r in rm_sql) + ";\n")

print("-- 4. Grant permissions to PLATFORM_ADMIN role")
print("INSERT IGNORE INTO sys_role_permission (")
print("  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted")
print(") VALUES")
print(",\n".join(f"  {r}" for r in rp_sql) + ";")

