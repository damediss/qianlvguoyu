/**
 * 项目管理表 — 结构化文档数据（对齐 Excel 模板字段）
 */
(function (global) {
  const base = (name, no, mgr) => ({
    projectName: name,
    projectNo: no || 'PRJ-2026-' + String(Math.floor(Math.random() * 900) + 100),
    preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华',
    projectManager: mgr || '王强',
    prepareDate: '2026-05-15'
  });

  const PM_DOCUMENTS = {
    charter: [
      {
        id: 'chr-001',
        ...base('电工技能等级认证培训项目', 'PRJ-2026-001', '王强'),
        background:
          '为满足企业电工岗位持证上岗要求，解决一线员工技能等级偏低、实操考核通过率不足的问题，特立项开展电工技能等级认证培训。',
        goals:
          '质量目标：实操考核通过率≥85%；进度目标：2026年6月底前完成全部学员认证；成本目标：人均培训成本控制在预算内；交付物：培训方案、实操记录、考核成绩单、发证台账。',
        milestonePlan:
          'M1（5.20）完成开班与摸底测评；M2（6.10）完成理论+实操集训；M3（6.25）组织等级认定考试；M4（6.30）归档发证与项目总结。',
        evaluationCriteria:
          '学员全部完成规定学时且考核合格；教学设备点检合格；资料档案完整可追溯；项目总结经审核人签字确认后验收。',
        assumptions:
          '假定实训场地按期开放、师资档期无冲突；约束：须符合安全生产规范，高温季节调整户外实操时段。',
        stakeholders: [
          { name: '李华', category: '高层管理', dept: '培训部', position: '部长' },
          { name: '赵敏', category: '客户代表', dept: '生产一部', position: 'HR对接人' },
          { name: '周工', category: '供应商', dept: '鉴定中心', position: '考务联络人' }
        ],
        teamMembers: [
          {
            name: '王强',
            role: '项目经理',
            dept: '培训部',
            duty: '统筹计划、资源协调、验收',
            dateRange: '2026-05-01 ~ 2026-06-30',
            workload: '全程 80%',
            phone: '138****1001',
            supervisor: '李华'
          },
          {
            name: '陈老师',
            role: '主讲讲师',
            dept: '培训中心',
            duty: '理论授课、实操指导',
            dateRange: '2026-05-15 ~ 2026-06-20',
            workload: '每周 3 天',
            phone: '139****2002',
            supervisor: '王强'
          }
        ]
      }
    ],
    milestone: [
      {
        id: 'ms-001',
        ...base('焊工晋升集训项目', 'PRJ-2026-002', '刘洋'),
        scheduleNote: '按周跟踪关键任务，责任人落实到人。',
        tasks: [
          { phase: '开始', code: '', owner: '', milestone: '项目启动' },
          { phase: '开工会', code: '', owner: '刘洋', milestone: '' },
          { phase: '阶段I 摸底测评', code: '111', owner: '教务组', milestone: '' },
          { phase: '阶段II 理论集训', code: '211', owner: '陈老师', milestone: '完成80学时' },
          { phase: '阶段III 实操考核', code: '311', owner: '考评组', milestone: '晋升认定' }
        ]
      }
    ],
    risk: [
      {
        id: 'risk-001',
        ...base('起重机械操作工复训项目', 'PRJ-2026-003', '赵磊'),
        risks: [
          {
            seq: 1,
            desc: '实训设备故障导致实操课延期',
            probability: '30%',
            impact: '高',
            level: '中风险',
            responsePlan: '提前一周完成设备点检，准备备用工位与机动讲师',
            owner: '设备管理员',
            status: '开放'
          },
          {
            seq: 2,
            desc: '学员出勤率不足影响考试批次',
            probability: '20%',
            impact: '中',
            level: '低风险',
            responsePlan: '与用人部门确认脱产安排，缺勤补训',
            owner: '班主任',
            status: '开放'
          }
        ]
      }
    ],
    change: [
      {
        id: 'chg-001',
        ...base('安全员继续教育培训', 'PRJ-2026-004', '孙婷'),
        history: [
          {
            seq: 1,
            changeTime: '2026-04-10',
            tasks: '开班时间',
            points: '推迟 3 天',
            reason: '鉴定中心考位调整',
            applicant: '孙婷',
            approver: '李华'
          }
        ],
        requestDesc: '建议将第三期实操考核由 6/18 调整至 6/22，以便与鉴定中心统一排考。',
        impactBaseline: ['进度计划', '资源计划'],
        needAnalysis: '是',
        costImpact: '无额外费用，场地预约顺延',
        scheduleImpact: '整体进度延后 4 天，仍在合同期内',
        resourceImpact: '讲师档期需微调 1 次',
        changeLevel: '低',
        ifNotChangeImpact: '可能导致学员与考位冲突，影响发证时效',
        applicantSign: '孙婷',
        applyDate: '2026-05-12',
        approvalOpinion: '同意调整，请更新里程碑并通知学员',
        approverSign: '李华',
        approvalDate: '2026-05-13'
      }
    ],
    meeting: [
      {
        id: 'mtg-001',
        meetingName: '电工认证项目周例会',
        convener: '王强',
        meetingDate: '2026-05-16',
        startTime: '14:00',
        location: '培训部会议室 A',
        duration: '1.5 小时',
        recorder: '小张',
        reviewer: '李华',
        objectives: '通报本周教学进度、设备点检结果及下周考试安排',
        participants: '王强（PM）、陈老师（讲师）、设备管理员、教务 2 人',
        materials: '进度周报、设备点检表、学员出勤表',
        speechRecord:
          '设备组反馈 2 号实训台接触器需更换；讲师提出周末加练需求；PM 要求周五前完成补课名单确认。',
        resolution: '1）周五前完成设备维修；2）周六安排加练 4 小时；3）缺勤学员名单报生产部备案',
        reportTo: '培训部',
        mainTo: '项目组全员',
        copyTo: '生产一部 HR',
        projectName: '电工技能等级认证培训项目',
        projectNo: 'PRJ-2026-001'
      }
    ],
    status: [
      {
        id: 'st-001',
        ...base('电工技能等级认证培训项目', 'PRJ-2026-001', '王强'),
        currentStatus: '按计划进行',
        aheadFlag: '',
        reportPeriod: '2026-05-09 ~ 2026-05-16',
        taskStatus: [
          { keyTask: '理论模块授课', indicator: '绿', desc: '已完成 32/40 学时' },
          { keyTask: '实操模块', indicator: '黄', desc: '设备维修中，预计周五恢复' }
        ],
        periodActivities: '完成安全培训、摸底测评；提交设备点检报告；确认考试批次 48 人。',
        nextPlan: '完成剩余理论课；组织模拟考；启动发证资料收集。',
        finance: '已用预算 62%，无超支风险。',
        legacyIssues: '上期 3 名缺勤学员已安排补课。',
        issuesHelp: '需协调鉴定中心考位确认函。'
      }
    ],
    communication: [
      {
        id: 'com-001',
        ...base('焊工晋升集训项目', 'PRJ-2026-002', '刘洋'),
        plans: [
          {
            stakeholder: '生产一部主管',
            info: '学员出勤与考核结果',
            frequency: '每周',
            method: '邮件+周报',
            owner: '刘洋'
          },
          {
            stakeholder: '学员',
            info: '课表、考场安排',
            frequency: '按需',
            method: '企业微信',
            owner: '班主任'
          }
        ]
      }
    ],
    wbs: [
      {
        id: 'wbs-001',
        ...base('T 客户考察公司接待项目（示例）', 'PRJ-DEMO-001', '王强'),
        wbsRows: [
          {
            code: '1',
            taskName: '邀请客户',
            activity: '提交邀请函给客户',
            hours: '4',
            hr: '市场部 2 人',
            otherRes: '—',
            cost: '500',
            duration: '3 天'
          },
          {
            code: '1.1',
            taskName: '',
            activity: '安排行程',
            hours: '8',
            hr: '行政',
            otherRes: '—',
            cost: '1200',
            duration: '5 天'
          },
          {
            code: '2',
            taskName: '落实资源',
            activity: '安排高层接待与座谈人员',
            hours: '16',
            hr: '多部门',
            otherRes: '会议室',
            cost: '3000',
            duration: '7 天'
          }
        ]
      }
    ],
    strategic: [
      {
        id: 'stg-001',
        ...base('2026 培训部战略解码', 'STR-2026', '李华'),
        rows: [
          { goal: '持证率提升', kpi: '关键岗位持证率≥95%', owner: '培训部', deadline: '2026-12' }
        ]
      }
    ],
    orgPerf: [
      {
        id: 'org-001',
        ...base('培训部 Q2 组织绩效', 'ORG-2026-Q2', '李华'),
        rows: [
          { dept: '教务组', target: '开班准时率 100%', actual: '98%', gap: '2 个项目延期' }
        ]
      }
    ],
    pbc: [
      {
        id: 'pbc-001',
        ...base('个人绩效承诺 — 王强', 'PBC-2026-001', '王强'),
        rows: [
          { item: '完成 3 个认证项目交付', weight: '40%', status: '进行中' }
        ]
      }
    ],
    issue: [
      {
        id: 'iss-001',
        ...base('电工技能等级认证培训项目', 'PRJ-2026-001', '王强'),
        rows: [
          {
            title: '2 号实训台故障',
            severity: '中',
            owner: '设备组',
            status: '处理中',
            due: '2026-05-17'
          }
        ]
      }
    ],
    knowledge: [
      {
        id: 'kn-001',
        ...base('电工认证项目知识收割', 'KM-2026-001', '王强'),
        findings: '摸底测评题库可复用；实操评分表需增加安全项检查点。',
        suggestions: '沉淀为标准作业指导书附件，纳入方法规则库。'
      }
    ]
  };

  /** 各页类型与存储键、标题 */
  const PM_PAGE_TYPES = {
    charter: { key: 'charter', title: '项目立项 Charter', listLabel: 'Charter 文档' },
    milestone: { key: 'milestone', title: '里程碑 / 项目进度', listLabel: '进度表' },
    risk: { key: 'risk', title: '风险管理表', listLabel: '风险登记册' },
    change: { key: 'change', title: '变更管理表', listLabel: '变更申请' },
    meeting: { key: 'meeting', title: '会议行动项 / 会议纪要', listLabel: '会议纪要' },
    status: { key: 'status', title: '项目状态报告', listLabel: '状态报告' },
    communication: { key: 'communication', title: '项目沟通计划', listLabel: '沟通计划' },
    wbs: { key: 'wbs', title: '工作分解结构 WBS', listLabel: 'WBS' },
    strategic: { key: 'strategic', title: '战略解码表', listLabel: '战略解码' },
    orgPerf: { key: 'orgPerf', title: '组织绩效表', listLabel: '组织绩效' },
    pbc: { key: 'pbc', title: '个人绩效承诺', listLabel: '个人绩效承诺' },
    issue: { key: 'issue', title: '问题管理表', listLabel: '问题清单' },
    knowledge: { key: 'knowledge', title: '知识收割表', listLabel: '知识收割' }
  };

  global.PM_DOCUMENTS = PM_DOCUMENTS;
  global.PM_PAGE_TYPES = PM_PAGE_TYPES;
  global.pmBaseFields = () => [
    { key: 'projectName', label: '项目名称' },
    { key: 'projectNo', label: '项目编号' },
    { key: 'preparedBy', label: '制作人' },
    { key: 'reviewedBy', label: '审核人' },
    { key: 'projectManager', label: '项目经理' },
    { key: 'prepareDate', label: '制作日期', type: 'date' }
  ];
})(typeof window !== 'undefined' ? window : global);
