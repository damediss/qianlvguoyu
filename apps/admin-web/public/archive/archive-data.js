/* 项目档案子页面 — 演示数据与表结构 */
window.ArchiveStore = (function () {
  const PROJECTS = ['电工认证项目', '焊工晋升项目', '起重工安全培训', '安全员岗前培训'];

  const mockData = {
    teachers: [
      { id: 'T001', name: '张建国', gender: '男', role: '主讲', title: '高级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX202401001', certExpiry: '2027-08-15', agreement: '已签署', nda: '已签署', rating: 4.8, phone: '138****1001', status: '在职', hours: 120, settleStatus: '已结算' },
      { id: 'T002', name: '李秀英', gender: '女', role: '主讲', title: '专家', teachLevel: '高级技师', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级技师证', certNo: 'GX202308882', certExpiry: '2026-06-20', agreement: '已签署', nda: '已签署', rating: 4.9, phone: '139****2002', status: '在职', hours: 96, settleStatus: '待结算' },
      { id: 'T003', name: '王德明', gender: '男', role: '助教', title: '中级', teachLevel: '高级工', skills: '起重工', project: '起重工安全培训', certName: '起重机械作业证', certNo: 'GX202205501', certExpiry: '2026-04-01', agreement: '未签署', nda: '已签署', rating: 4.2, phone: '137****3003', status: '兼职', hours: 48, settleStatus: '未结算' },
      { id: 'T004', name: '陈丽华', gender: '女', role: '班主任', title: '高级', teachLevel: '—', skills: '教务管理', project: '电工认证项目', certName: '人力资源管理师', certNo: 'GX202011203', certExpiry: '2028-01-10', agreement: '已签署', nda: '已签署', rating: 4.7, phone: '136****4004', status: '在职', hours: 0, settleStatus: '—' },
      { id: 'T005', name: '刘志强', gender: '男', role: '主讲', title: '初级', teachLevel: '中级工', skills: '安全员', project: '安全员岗前培训', certName: '安全员培训师资证', certNo: 'GX202506001', certExpiry: '2029-03-01', agreement: '已签署', nda: '已签署', rating: 4.0, phone: '135****5005', status: '在职', hours: 64, settleStatus: '已结算' },
      { id: 'T006', name: '赵敏', gender: '女', role: '主讲', title: '中级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX201907766', certExpiry: '2026-05-28', agreement: '审批中', nda: '未签署', rating: 4.5, phone: '134****6006', status: '兼职', hours: 32, settleStatus: '未结算' },
      { id: 'T007', name: '黄伟', gender: '男', role: '跟班', title: '中级', teachLevel: '高级工', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级工证', certNo: 'GX202103445', certExpiry: '2027-11-30', agreement: '已签署', nda: '已签署', rating: 4.3, phone: '133****7007', status: '在职', hours: 80, settleStatus: '待结算' }
    ],
    students: [
      { id: 'S1001', name: '陈伟', gender: '男', idCard: '4501**********1234', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 94, homework: 88, mockScore: 76, warnLevel: '', progress: 78, cert: '培训中' },
      { id: 'S1002', name: '李娜', gender: '女', idCard: '4501**********5678', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 96, homework: 92, mockScore: 82, warnLevel: '', progress: 85, cert: '培训中' },
      { id: 'S1003', name: '周强', gender: '男', idCard: '4502**********9012', enterprise: '柳州钢铁公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '补正中', attendance: 72, homework: 65, mockScore: 58, warnLevel: '缺勤预警', progress: 62, cert: '培训中' },
      { id: 'S1004', name: '吴芳', gender: '女', idCard: '4503**********3456', enterprise: '桂林旅游股份公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '已结业', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 98, homework: 95, mockScore: 88, warnLevel: '', progress: 100, cert: '已结业' },
      { id: 'S1005', name: '郑磊', gender: '男', idCard: '4501**********7890', enterprise: '南宁轨道交通', trade: '起重工', level: '高级工', project: '起重工安全培训', className: '起重工安全班', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 85, homework: 70, mockScore: 62, warnLevel: '', progress: 45, cert: '培训中' },
      { id: 'S1006', name: '孙静', gender: '女', idCard: '4509**********2345', enterprise: '玉林能源公司', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '已获证', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 100, homework: 98, mockScore: 91, warnLevel: '', progress: 100, cert: '已获证' },
      { id: 'S1007', name: '马超', gender: '男', idCard: '4502**********6789', enterprise: '柳州钢铁公司', trade: '安全员', level: '岗前', project: '安全员岗前培训', className: '安全员岗前班', enrollStatus: '在培', confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', attendance: 0, homework: 0, mockScore: 0, warnLevel: '', progress: 0, cert: '未开始' },
      { id: 'S1008', name: '黄丽', gender: '女', idCard: '4501**********1122', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '待分班', enrollStatus: '待入班', confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', attendance: 0, homework: 0, mockScore: 0, warnLevel: '', progress: 0, cert: '未开始' },
      { id: 'S1009', name: '林峰', gender: '男', idCard: '4503**********3344', enterprise: '桂林旅游股份公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '审核中', attendance: 68, homework: 55, mockScore: 52, warnLevel: '综合预警', progress: 56, cert: '培训中' },
      { id: 'S1010', name: '何娟', gender: '女', idCard: '4501**********5566', enterprise: '梧州港务集团', trade: '电工', level: '中级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 78, homework: 72, mockScore: 60, warnLevel: '作业预警', progress: 70, cert: '培训中' }
    ],
    equipment: [
      { id: 'EQ01', name: '电工实训台', model: 'DG-200A', qty: 24, rate: 96, nextMaintain: '2026-06-15', status: '正常' },
      { id: 'EQ02', name: '焊机', model: 'NBC-350', qty: 18, rate: 88, nextMaintain: '2026-05-20', status: '正常' },
      { id: 'EQ03', name: '起重机模拟器', model: 'QZ-SIM3', qty: 2, rate: 72, nextMaintain: '2026-04-10', status: '维修' },
      { id: 'EQ04', name: 'PLC实验装置', model: 'PLC-Lab', qty: 12, rate: 100, nextMaintain: '2026-08-01', status: '正常' },
      { id: 'EQ05', name: '安全防护套装', model: 'SAFE-KIT', qty: 50, rate: 0, nextMaintain: '—', status: '报废' }
    ],
    materials: [
      { id: 'M001', name: '焊条', spec: 'J422 φ3.2', stock: 120, safety: 100, unit: 'kg', lastIn: '2026-05-01' },
      { id: 'M002', name: '电工教材', spec: '2025版高级', stock: 45, safety: 50, unit: '册', lastIn: '2026-04-18' },
      { id: 'M003', name: '安全帽', spec: 'ABS标准', stock: 28, safety: 30, unit: '顶', lastIn: '2026-03-22' },
      { id: 'M004', name: '实训手套', spec: '防割五级', stock: 200, safety: 80, unit: '双', lastIn: '2026-05-10' },
      { id: 'M005', name: '绝缘胶带', spec: 'PVC 20m', stock: 15, safety: 40, unit: '卷', lastIn: '2026-02-15' }
    ],
    rules: [
      { id: 'R01', name: '教学方法SOP', version: 'V2.1', effective: '2025-01-01', file: '教学方法SOP.pdf', summary: '规定理实一体授课流程：课前准备→理论讲授→示范演示→学员实操→点评总结。' },
      { id: 'R02', name: '考核评分规则', version: 'V3.0', effective: '2025-06-01', file: '考核评分规则.pdf', summary: '理论30%、实操50%、安全规范20%。' },
      { id: 'R03', name: '安全操作规范', version: 'V1.8', effective: '2024-12-01', file: '安全操作规范.pdf', summary: '进入实训区须穿戴劳保；设备使用前点检。' },
      { id: 'R04', name: '学员考勤管理办法', version: 'V1.2', effective: '2025-03-15', file: '考勤办法.pdf', summary: '缺课超20%不得参加结业考核。' }
    ],
    bases: [
      { id: 'B1', name: '总部培训中心', address: '南宁市青秀区实训路88号', area: 3200, capacity: 200, utilization: 76 },
      { id: 'B2', name: '华东实训基地', address: '柳州市鱼峰区技能大道12号', area: 4800, capacity: 280, utilization: 68 },
      { id: 'B3', name: '桂林教学点', address: '桂林市七星区教育园5号', area: 1200, capacity: 80, utilization: 45 }
    ],
    rooms: [
      { id: 'RM01', name: '电工实训室A', base: '总部培训中心', status: '可用' },
      { id: 'RM02', name: '焊工实训室B', base: '总部培训中心', status: '占用' },
      { id: 'RM03', name: '理论教室301', base: '总部培训中心', status: '可用' },
      { id: 'RM04', name: '起重模拟厅', base: '华东实训基地', status: '维修' },
      { id: 'RM05', name: '多媒体教室201', base: '桂林教学点', status: '可用' }
    ],
    /* 项目管控十大表演示数据（依据需求规格：立项表、总体计划/里程碑、风险、问题、变更等） */
    strategic: [
      { id: 1, goal: '提升培训质量与结业率', action: '推行理实一体标准化教案，统一各工种授课模板', metric: '结业合格率≥92%，学员满意度≥4.5', dept: '教务部', deadline: '2026-12-31', progress: 68 },
      { id: 2, goal: '扩大电工/焊工认定项目覆盖', action: '拓展南宁、柳州、桂林3家重点企业合作', metric: '全年新增认证人数500+', dept: '市场部', deadline: '2026-09-30', progress: 45 },
      { id: 3, goal: '数字化教学与过程留痕', action: '学习进度、考勤、影像证据链上线率达100%', metric: '过程数据完整率100%', dept: '信息中心', deadline: '2026-08-15', progress: 82 },
      { id: 4, goal: '降低实训安全风险', action: '设备双控点检、安全交底纳入开班必办', metric: '安全事故零发生', dept: '实训中心', deadline: '2026-12-31', progress: 55 }
    ],
    orgPerf: [
      { id: 1, dept: '教务部', indicator: '培训人次（年度）', target: 2000, actual: 1680, score: 84 },
      { id: 2, dept: '市场部', indicator: '客户满意度（NPS）', target: 90, actual: 92, score: 96 },
      { id: 3, dept: '实训中心', indicator: '结业率', target: 92, actual: 89, score: 88 },
      { id: 4, dept: '综合办', indicator: '项目按时交付率', target: 95, actual: 91, score: 90 },
      { id: 5, dept: '财务部', indicator: '预算执行偏差(%)', target: 5, actual: 3.2, score: 94 }
    ],
    pbc: [
      { id: 1, name: '张明', role: '项目经理', goal: '电工认证项目按期交付并回款', kr: 'Q2开班2期、结业率≥90%、回款率100%', weight: 40, self: 88, manager: 85 },
      { id: 2, name: '赵丽', role: '教务主管', goal: '提升排课与师资匹配效率', kr: '排课冲突率<2%、师资到位率≥98%', weight: 35, self: 90, manager: 92 },
      { id: 3, name: '李强', role: '市场经理', goal: '拓展企业客户与签约', kr: '新签企业≥3家、续约率≥80%', weight: 30, self: 75, manager: 78 },
      { id: 4, name: '王芳', role: '实训主任', goal: '保障设备完好与场地落实', kr: '设备完好率≥95%、开班场地100%落实', weight: 25, self: 86, manager: 88 }
    ],
    charter: [
      { id: 1, name: '电工认证项目', type: '技能认定', date: '2025-11-01', benefit: '年增收约180万，满足建工集团持证上岗', manager: '张明', budget: 120, status: '进行中', summary: '面向广西建工集团电工高级工认定，全年计划4期每期约42人，含理论+实操+安全专项。' },
      { id: 2, name: '焊工晋升项目', type: '企业内训', date: '2026-01-15', benefit: '提升柳钢焊接合格率，降低返工成本', manager: '赵丽', budget: 85, status: '进行中', summary: '柳钢焊工中级晋升培训，理论+实操+安全8课时，对接企业生产排班。' },
      { id: 3, name: '起重工安全培训', type: '合规培训', date: '2026-03-01', benefit: '满足轨道公司特种作业复训合规', manager: '李强', budget: 45, status: '已批准', summary: '南宁轨道交通起重机械操作安全复训，含模拟器实训与考核。' },
      { id: 4, name: '安全员岗前培训', type: '岗前培训', date: '2025-08-20', benefit: '企业新员工持证上岗', manager: '王芳', budget: 30, status: '已关闭', summary: '已完成2期岗前培训并归档，补贴材料已提交。' }
    ],
    milestone: [
      { id: 1, project: '电工认证项目', node: '立项审批通过', plan: '2025-11-15', actual: '2025-11-18', deliverable: '项目立项表、十大管理表初始化', status: '已完成' },
      { id: 2, project: '电工认证项目', node: '首期开班', plan: '2026-03-01', actual: '2026-03-05', deliverable: '开班通知书、花名册、承诺书', status: '已完成' },
      { id: 3, project: '电工认证项目', node: '中期考核', plan: '2026-05-15', actual: '', deliverable: '考核成绩单、过程留痕包', status: '进行中' },
      { id: 4, project: '电工认证项目', node: '认定申报提交', plan: '2026-07-10', actual: '', deliverable: '认定材料包、获证花名册', status: '未开始' },
      { id: 5, project: '焊工晋升项目', node: '企业需求确认', plan: '2026-02-28', actual: '2026-02-25', deliverable: '需求确认书、培养方案', status: '已完成' },
      { id: 6, project: '焊工晋升项目', node: '结业典礼', plan: '2026-07-30', actual: '', deliverable: '结业证书、总结报告', status: '未开始' },
      { id: 7, project: '起重工安全培训', node: '模拟器验收', plan: '2026-04-10', actual: '2026-04-18', deliverable: '设备验收报告', status: '延期' },
      { id: 8, project: '起重工安全培训', node: '补贴材料提交', plan: '2026-06-01', actual: '', deliverable: '补贴申报材料包', status: '未开始' }
    ],
    risk: [
      { id: 'RK01', project: '电工认证项目', desc: '师资不足导致排课冲突，兼职讲师档期难协调', prob: '中', impact: '高', measure: '提前30天锁定兼职讲师池，教务部周度排课评审', owner: '赵丽', status: '监控中' },
      { id: 'RK02', project: '焊工晋升项目', desc: '焊材价格波动影响实训成本', prob: '高', impact: '中', measure: '季度集中采购，财务部监控预算偏差', owner: '财务部', status: '已缓解' },
      { id: 'RK03', project: '起重工安全培训', desc: '起重机模拟器故障导致培训延期', prob: '中', impact: '高', measure: '启用备用场地+外协设备，更新风险登记', owner: '王芳', status: '开放' },
      { id: 'RK04', project: '电工认证项目', desc: '企业学员中途离职造成人数不足', prob: '低', impact: '中', measure: '与企业HR联动考勤，开班前二次确认名单', owner: '张明', status: '关闭' },
      { id: 'RK05', project: '焊工晋升项目', desc: '人社认定政策口径调整', prob: '中', impact: '高', measure: '认定部跟踪政策公告，材料模板版本控制', owner: '认定部联络人', status: '监控中' }
    ],
    issue: [
      { id: 'ISS-101', time: '2026-05-08', desc: '学员反馈教材版本与2026考纲不一致', source: '学员反馈', priority: '高', solution: '更换2025版官方教材，补发电子版考纲', status: '已解决' },
      { id: 'ISS-102', time: '2026-05-12', desc: '焊工实训室通风不足，夏季高温影响实操', source: '内部审计', priority: '中', solution: '加装排风系统，调整下午实操时段', status: '未解决' },
      { id: 'ISS-103', time: '2026-04-20', desc: '排课系统导出Excel格式与企业模板不匹配', source: '教务部', priority: '低', solution: '修复导出模板，增加企业定制格式', status: '关闭' },
      { id: 'ISS-104', time: '2026-05-15', desc: '安全帽库存低于安全线，影响开班物资准备', source: '物料台账', priority: '高', solution: '紧急采购50顶，更新物料台账预警阈值', status: '已解决' },
      { id: 'ISS-105', time: '2026-05-18', desc: '部分学员补贴材料缺少企业盖章', source: '材料审核', priority: '高', solution: '下发补正清单，企业3个工作日内盖章回传', status: '未解决' }
    ],
    change: [
      { id: 'CHG-001', project: '电工认证项目', type: '进度', content: '第2期开班由6月1日推迟至6月15日，因场地检修', applicant: '张明', approval: '已批准', effective: '2026-06-01' },
      { id: 'CHG-002', project: '焊工晋升项目', type: '范围', content: '增加安全专项8课时，纳入最终方案', applicant: '赵丽', approval: '审批中', effective: '' },
      { id: 'CHG-003', project: '起重工安全培训', type: '成本', content: '预算追加5万元用于模拟器维修与外协场地', applicant: '王芳', approval: '已批准', effective: '2026-05-01' },
      { id: 'CHG-004', project: '电工认证项目', type: '师资', content: '主讲由外聘改为内部高级讲师，课酬结算方式调整', applicant: '赵丽', approval: '已批准', effective: '2026-05-10' },
      { id: 'CHG-005', project: '焊工晋升项目', type: '场地', content: '实操由总部基地调整至华东实训基地', applicant: '王芳', approval: '已驳回', effective: '' }
    ],
    meeting: [
      { id: 1, meeting: '月度教务例会', action: '完成焊工班师资排课表并发布', owner: '赵丽', deadline: '2026-05-25', done: '未完成', proof: '' },
      { id: 2, meeting: '项目周会-电工认证', action: '提交中期考核方案与监考安排', owner: '张明', deadline: '2026-05-20', done: '已完成', proof: '考核方案_v1.pdf' },
      { id: 3, meeting: '安全专题会', action: '更新实训室每日点检表并培训班主任', owner: '王芳', deadline: '2026-05-18', done: '已完成', proof: '点检表_2026.xlsx' },
      { id: 4, meeting: '市场拓展会', action: '拜访玉林能源公司洽谈2026培训合作', owner: '李强', deadline: '2026-06-05', done: '未完成', proof: '' },
      { id: 5, meeting: '补贴材料专项会', action: '核对电工班补贴材料一次性通过清单', owner: '张明', deadline: '2026-05-28', done: '未完成', proof: '' }
    ],
    knowledge: [
      { id: 1, project: '电工认证项目', time: '2026-04-30', category: '成功经验', finding: '理实一体+早晚分段排课使通过率提升12%', suggest: '推广至焊工、起重项目；纳入标准教案库', status: '已入库' },
      { id: 2, project: '焊工晋升项目', time: '2026-05-10', category: '失败教训', finding: '立项前未做企业需求调研，导致课表返工2次', suggest: '立项 Charter 强制附需求确认书', status: '已入库' },
      { id: 3, project: '安全员岗前培训', time: '2025-12-15', category: '成功经验', finding: '线上预习+线下集中考核可缩短面授周期30%', suggest: '纳入岗前培训标准流程', status: '已入库' },
      { id: 4, project: '起重工安全培训', time: '2026-05-16', category: '失败教训', finding: '模拟器维修导致档期延误，未提前备备用设备清单', suggest: '环境配置表增加备用设备字段，风险表联动', status: '待入库' },
      { id: 5, project: '电工认证项目', time: '2026-05-12', category: '改进建议', finding: '补贴材料企业盖章环节平均耗时5天', suggest: '与企业签订批量盖章授权或电子签章流程', status: '待入库' }
    ]
  };

  const CONTROL_TABLES = {
    strategic: {
      columns: [
        { key: 'goal', label: '战略目标' }, { key: 'action', label: '关键行动' }, { key: 'metric', label: '衡量指标' },
        { key: 'dept', label: '责任部门' }, { key: 'deadline', label: '完成时限' },
        { key: 'progress', label: '进度%', render: v => v + '%' }
      ],
      defaultRow: () => ({ id: Date.now(), goal: '', action: '', metric: '', dept: '', deadline: '', progress: 0 })
    },
    orgPerf: {
      columns: [
        { key: 'dept', label: '考核部门' }, { key: 'indicator', label: '考核指标' },
        { key: 'target', label: '目标值' }, { key: 'actual', label: '实际值' }, { key: 'score', label: '得分' }
      ],
      defaultRow: () => ({ id: Date.now(), dept: '', indicator: '', target: 0, actual: 0, score: 0 })
    },
    pbc: {
      columns: [
        { key: 'name', label: '员工姓名' }, { key: 'role', label: '岗位' }, { key: 'goal', label: '个人目标' },
        { key: 'kr', label: '关键结果' }, { key: 'weight', label: '权重%' }, { key: 'self', label: '自评' }, { key: 'manager', label: '上级评分' }
      ],
      defaultRow: () => ({ id: Date.now(), name: '', role: '', goal: '', kr: '', weight: 0, self: 0, manager: 0 })
    },
    charter: {
      columns: [
        { key: 'name', label: '项目名称' }, { key: 'type', label: '项目类型' }, { key: 'date', label: '立项日期' },
        { key: 'benefit', label: '预期收益' }, { key: 'manager', label: '项目经理' }, { key: 'budget', label: '预算(万)' },
        { key: 'status', label: '状态' }, { key: 'summary', label: '章程摘要', render: v => `<span class="charter-summary">${v || '—'}</span>` }
      ],
      defaultRow: () => ({ id: Date.now(), name: '', type: '', date: '', benefit: '', manager: '', budget: 0, status: '进行中', summary: '' })
    },
    milestone: {
      columns: [
        { key: 'project', label: '项目名称' }, { key: 'node', label: '里程碑节点' }, { key: 'plan', label: '计划完成' },
        { key: 'actual', label: '实际完成' }, { key: 'deliverable', label: '交付物' },
        { key: 'status', label: '状态', badge: true }
      ],
      defaultRow: () => ({ id: Date.now(), project: PROJECTS[0], node: '', plan: '', actual: '', deliverable: '', status: '未开始' })
    },
    risk: {
      columns: [
        { key: 'id', label: '风险编号' }, { key: 'project', label: '所属项目' }, { key: 'desc', label: '风险描述' },
        { key: 'prob', label: '概率' }, { key: 'impact', label: '影响' }, { key: 'measure', label: '应对措施' },
        { key: 'owner', label: '责任人' }, { key: 'status', label: '状态' }
      ],
      defaultRow: () => ({ id: 'RK' + Date.now(), project: PROJECTS[0], desc: '', prob: '中', impact: '中', measure: '', owner: '', status: '开放' })
    },
    issue: {
      columns: [
        { key: 'id', label: '问题ID' }, { key: 'time', label: '发现时间' }, { key: 'desc', label: '问题描述' },
        { key: 'source', label: '来源' }, { key: 'priority', label: '优先级', badge: true },
        { key: 'solution', label: '解决方案' }, { key: 'status', label: '状态', badge: true }
      ],
      defaultRow: () => ({ id: 'ISS-' + Date.now(), time: new Date().toISOString().slice(0, 10), desc: '', source: '', priority: '中', solution: '', status: '未解决' })
    },
    change: {
      columns: [
        { key: 'id', label: '变更单号' }, { key: 'project', label: '关联项目' }, { key: 'type', label: '变更类型' },
        { key: 'content', label: '变更内容' }, { key: 'applicant', label: '申请人' },
        { key: 'approval', label: '审批状态', badge: true }, { key: 'effective', label: '生效日期' }
      ],
      defaultRow: () => ({ id: 'CHG-' + Date.now(), project: PROJECTS[0], type: '范围', content: '', applicant: '', approval: '审批中', effective: '' })
    },
    meeting: {
      columns: [
        { key: 'meeting', label: '会议名称' }, { key: 'action', label: '行动项' }, { key: 'owner', label: '负责人' },
        { key: 'deadline', label: '截止日期' }, { key: 'done', label: '完成状态', badge: true },
        { key: 'proof', label: '完成证明', render: v => v ? `<a class="link-file" href="#">${v}</a>` : '—' }
      ],
      defaultRow: () => ({ id: Date.now(), meeting: '', action: '', owner: '', deadline: '', done: '未完成', proof: '' })
    },
    knowledge: {
      columns: [
        { key: 'project', label: '项目名称' }, { key: 'time', label: '收割时间' }, { key: 'category', label: '经验类别' },
        { key: 'finding', label: '关键发现' }, { key: 'suggest', label: '改进建议' },
        { key: 'status', label: '入库状态', badge: true }
      ],
      defaultRow: () => ({ id: Date.now(), project: PROJECTS[0], time: '', category: '成功经验', finding: '', suggest: '', status: '待入库' })
    }
  };

  /** 十大管控表 — 表单字段定义（增删改查弹窗） */
  const CONTROL_FORM_SCHEMAS = {
    strategic: [
      { key: 'goal', label: '战略目标', required: true, type: 'textarea', placeholder: '描述本周期战略目标' },
      { key: 'action', label: '关键行动', required: true, type: 'textarea' },
      { key: 'metric', label: '衡量指标', placeholder: '如：结业合格率≥92%' },
      { key: 'dept', label: '责任部门', type: 'select', options: ['教务部', '市场部', '实训中心', '综合办', '财务部'] },
      { key: 'deadline', label: '完成时限', type: 'date' },
      { key: 'progress', label: '完成进度(%)', type: 'number', min: 0, max: 100 }
    ],
    orgPerf: [
      { key: 'dept', label: '考核部门', type: 'select', options: ['教务部', '市场部', '实训中心', '综合办', '财务部'], required: true },
      { key: 'indicator', label: '考核指标', required: true },
      { key: 'target', label: '目标值', type: 'number' },
      { key: 'actual', label: '实际值', type: 'number' },
      { key: 'score', label: '得分', type: 'number', min: 0, max: 100 }
    ],
    pbc: [
      { key: 'name', label: '员工姓名', required: true },
      { key: 'role', label: '岗位', type: 'select', options: ['项目经理', '教务主管', '市场经理', '实训主任', '培训专员'] },
      { key: 'goal', label: '个人目标', type: 'textarea' },
      { key: 'kr', label: '关键结果(KR)', type: 'textarea' },
      { key: 'weight', label: '权重(%)', type: 'number', min: 0, max: 100 },
      { key: 'self', label: '自评', type: 'number', min: 0, max: 100 },
      { key: 'manager', label: '上级评分', type: 'number', min: 0, max: 100 }
    ],
    charter: [
      { key: 'name', label: '项目名称', required: true },
      { key: 'type', label: '项目类型', type: 'select', options: ['技能认定', '企业内训', '合规培训', '岗前培训', '新型学徒'] },
      { key: 'date', label: '立项日期', type: 'date' },
      { key: 'benefit', label: '预期收益', placeholder: '如：年增收约180万' },
      { key: 'manager', label: '项目经理', required: true },
      { key: 'budget', label: '预算(万元)', type: 'number', min: 0 },
      { key: 'status', label: '项目状态', type: 'select', options: ['进行中', '已批准', '已关闭', '暂停'] },
      { key: 'summary', label: '章程摘要', type: 'textarea', full: true }
    ],
    milestone: [
      { key: 'project', label: '关联项目', type: 'select', options: PROJECTS, required: true },
      { key: 'node', label: '里程碑节点', required: true },
      { key: 'plan', label: '计划完成日期', type: 'date' },
      { key: 'actual', label: '实际完成日期', type: 'date' },
      { key: 'deliverable', label: '交付物', placeholder: '如：开班通知书、考核成绩单' },
      { key: 'status', label: '节点状态', type: 'select', options: ['未开始', '进行中', '已完成', '延期'] }
    ],
    risk: [
      { key: 'id', label: '风险编号', placeholder: '留空自动生成 RK…' },
      { key: 'project', label: '所属项目', type: 'select', options: PROJECTS, required: true },
      { key: 'desc', label: '风险描述', type: 'textarea', required: true },
      { key: 'prob', label: '发生概率', type: 'select', options: ['低', '中', '高'] },
      { key: 'impact', label: '影响程度', type: 'select', options: ['低', '中', '高'] },
      { key: 'measure', label: '应对措施', type: 'textarea' },
      { key: 'owner', label: '责任人', required: true },
      { key: 'status', label: '风险状态', type: 'select', options: ['开放', '监控中', '已缓解', '关闭'] }
    ],
    issue: [
      { key: 'id', label: '问题ID', placeholder: '留空自动生成 ISS-…' },
      { key: 'time', label: '发现时间', type: 'date' },
      { key: 'desc', label: '问题描述', type: 'textarea', required: true },
      { key: 'source', label: '问题来源', type: 'select', options: ['学员反馈', '内部审计', '教务部', '物料台账', '企业方', '其他'] },
      { key: 'priority', label: '优先级', type: 'select', options: ['低', '中', '高'] },
      { key: 'solution', label: '解决方案', type: 'textarea' },
      { key: 'status', label: '处理状态', type: 'select', options: ['未解决', '已解决', '关闭'] }
    ],
    change: [
      { key: 'id', label: '变更单号', placeholder: '留空自动生成 CHG-…' },
      { key: 'project', label: '关联项目', type: 'select', options: PROJECTS, required: true },
      { key: 'type', label: '变更类型', type: 'select', options: ['范围', '进度', '成本', '师资', '场地', '其他'] },
      { key: 'content', label: '变更内容', type: 'textarea', required: true },
      { key: 'applicant', label: '申请人', required: true },
      { key: 'approval', label: '审批状态', type: 'select', options: ['审批中', '已批准', '已驳回'] },
      { key: 'effective', label: '生效日期', type: 'date' }
    ],
    meeting: [
      { key: 'meeting', label: '会议名称', required: true },
      { key: 'action', label: '行动项', type: 'textarea', required: true },
      { key: 'owner', label: '负责人', required: true },
      { key: 'deadline', label: '截止日期', type: 'date' },
      { key: 'done', label: '完成状态', type: 'select', options: ['未完成', '已完成'] },
      { key: 'proof', label: '完成证明(附件名)', placeholder: '如：考核方案.pdf' }
    ],
    knowledge: [
      { key: 'project', label: '关联项目', type: 'select', options: PROJECTS, required: true },
      { key: 'time', label: '收割时间', type: 'date' },
      { key: 'category', label: '经验类别', type: 'select', options: ['成功经验', '失败教训', '最佳实践', '改进建议'] },
      { key: 'finding', label: '关键发现', type: 'textarea', required: true },
      { key: 'suggest', label: '改进建议', type: 'textarea' },
      { key: 'status', label: '入库状态', type: 'select', options: ['待入库', '已入库'] }
    ]
  };

  const FORM_SCHEMAS = {
    equipment: [
      { key: 'id', label: '设备ID' }, { key: 'name', label: '设备名称' }, { key: 'model', label: '型号' },
      { key: 'qty', label: '数量', type: 'number' }, { key: 'rate', label: '完好率%', type: 'number' },
      { key: 'nextMaintain', label: '下次保养日期' },
      { key: 'status', label: '状态', type: 'select', options: ['正常', '维修', '报废'] }
    ],
    materials: [
      { key: 'id', label: '物料编码' }, { key: 'name', label: '物料名称' }, { key: 'spec', label: '规格' },
      { key: 'stock', label: '库存量', type: 'number' }, { key: 'safety', label: '安全库存', type: 'number' },
      { key: 'unit', label: '单位' }, { key: 'lastIn', label: '上次入库日期' }
    ],
    rules: [
      { key: 'id', label: '规则ID' }, { key: 'name', label: '规则名称' }, { key: 'version', label: '版本' },
      { key: 'effective', label: '生效日期' }, { key: 'file', label: '附件文件名' },
      { key: 'summary', label: '摘要', type: 'textarea' }
    ],
    bases: [
      { key: 'id', label: 'ID' }, { key: 'name', label: '基地名称' }, { key: 'address', label: '地址' },
      { key: 'area', label: '面积', type: 'number' }, { key: 'capacity', label: '容纳人数', type: 'number' },
      { key: 'utilization', label: '利用率%', type: 'number' }
    ],
    rooms: [
      { key: 'id', label: 'ID' }, { key: 'name', label: '场地名称' }, { key: 'base', label: '所属基地' },
      { key: 'status', label: '状态', type: 'select', options: ['可用', '占用', '维修'] }
    ]
  };

  const PAGE_META = {
    equipment: { title: '教学设备', sub: '设备台账、完好率与保养计划', tag: '项目档案 · 机', icon: 'fa-tools' },
    materials: { title: '物料台账', sub: '库存管理、安全库存与出入库', tag: '项目档案 · 料', icon: 'fa-boxes' },
    rules: { title: '方法规则', sub: 'SOP、考核规范与安全制度文档', tag: '项目档案 · 法', icon: 'fa-book' },
    environment: { title: '环境配置', sub: '实训基地、场地状态与环境评价', tag: '项目档案 · 环', icon: 'fa-building' },
    strategic: { title: '战略解码表', sub: '填写：战略目标、关键行动、衡量指标、责任部门、完成时限与进度（组织级OKR分解）', tag: '项目管控', icon: 'fa-bullseye' },
    orgPerf: { title: '组织绩效表', sub: '填写：考核部门、指标、目标值、实际值、得分（部门年度/季度绩效）', tag: '项目管控', icon: 'fa-chart-bar' },
    pbc: { title: '个人绩效承诺（PBC）', sub: '填写：员工、岗位、个人目标、KR、权重、自评与上级评分', tag: '项目管控', icon: 'fa-user-check' },
    charter: { title: '项目立项 Charter', sub: '一、基本情况；二、项目描述；三、里程碑计划；四、评价标准；五、假定与约束；六、利益干系人；项目组成员', tag: '项目管控', icon: 'fa-file-contract' },
    milestone: { title: '里程碑 / 项目进度表', sub: '项目基本情况 + 任务阶段、责任人、关键里程碑（对齐进度表模板）', tag: '项目管控', icon: 'fa-flag-checkered' },
    risk: { title: '风险管理表', sub: '风险概率准则 + 风险清单（描述、概率、影响、等级、响应计划、责任人、开放/关闭）', tag: '项目管控', icon: 'fa-exclamation-triangle' },
    issue: { title: '问题管理表', sub: '项目基本情况 + 问题登记清单', tag: '项目管控', icon: 'fa-bug' },
    change: { title: '变更管理表', sub: '历史变更记录、变更描述、影响分析、审批结果（变更申请模板）', tag: '项目管控', icon: 'fa-exchange-alt' },
    meeting: { title: '会议纪要 / 行动项', sub: '会议基本信息、目标、参会人、材料、发言记录、决议、发放范围', tag: '项目管控', icon: 'fa-comments' },
    knowledge: { title: '知识收割表', sub: '项目基本情况 + 经验发现 + 改进建议', tag: '项目管控', icon: 'fa-lightbulb' },
    wbs: { title: '工作分解结构 WBS', sub: '分解代码、任务名称、活动、工时/人力/费用/工期（R/As/I/Ap）', tag: '项目管控', icon: 'fa-sitemap' },
    communication: { title: '项目沟通计划', sub: '利益干系人、所需信息、频率、方法、责任人', tag: '项目管控', icon: 'fa-bullhorn' },
    status: { title: '项目状态报告', sub: '任务状态、本周期活动、下周期计划、财务、遗留问题、本期求助', tag: '项目管控', icon: 'fa-clipboard-list' }
  };

  return { PROJECTS, mockData, CONTROL_TABLES, CONTROL_FORM_SCHEMAS, FORM_SCHEMAS, PAGE_META };
})();
