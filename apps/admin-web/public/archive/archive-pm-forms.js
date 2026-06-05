/**
 * 项目管理表 — 分节表单 CRUD（对齐 Excel 模板）
 */
(function (global) {
  const esc = (s) =>
    String(s ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/"/g, '&quot;');

  function uid(prefix) {
    return prefix + '-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
  }

  function getStore(type) {
    if (!global.PM_DOCUMENTS[type]) global.PM_DOCUMENTS[type] = [];
    return global.PM_DOCUMENTS[type];
  }

  function renderBaseGrid(doc, readonly) {
    const fields = global.pmBaseFields();
    let html = '<div class="pm-sheet-wrap"><div class="pm-section-head">一、项目基本情况</div><table class="pm-sheet"><tbody>';
    for (let i = 0; i < fields.length; i += 2) {
      const a = fields[i];
      const b = fields[i + 1];
      html += '<tr>';
      [a, b].forEach((f) => {
        if (!f) return;
        const val = doc[f.key] ?? '';
        const inp =
          readonly || f.type === 'date'
            ? f.type === 'date'
              ? `<input type="date" data-k="${f.key}" value="${esc(val)}" ${readonly ? 'readonly' : ''}>`
              : `<input type="text" data-k="${f.key}" value="${esc(val)}" ${readonly ? 'readonly' : ''}>`
            : `<input type="${f.type || 'text'}" data-k="${f.key}" value="${esc(val)}">`;
        html += `<th class="pm-label">${f.label}</th><td>${inp}</td>`;
      });
      html += '</tr>';
    }
    html += '</tbody></table></div>';
    return html;
  }

  function renderTextSection(title, hint, key, val, readonly, rows) {
    const r = rows || 4;
    return `
      <div class="pm-sheet-wrap">
        <div class="pm-section-head">${title}</div>
        ${hint ? `<div class="pm-hint">${hint}</div>` : ''}
        <table class="pm-sheet"><tr><td>
          <textarea data-k="${key}" rows="${r}" ${readonly ? 'readonly' : ''}>${esc(val)}</textarea>
        </td></tr></table>
      </div>`;
  }

  function renderSubTable(title, columns, rowsKey, rows, readonly, entity) {
    const list = rows || [];
    let thead = '<tr>' + columns.map((c) => `<th>${c.label}</th>`).join('');
    if (!readonly) thead += '<th style="width:72px">操作</th>';
    thead = '<thead>' + thead + '</tr>';

    let tbody = '';
    list.forEach((row, idx) => {
      tbody += '<tr data-idx="' + idx + '">';
      columns.forEach((c) => {
        const v = row[c.key] ?? '';
        tbody += `<td><input type="text" data-col="${c.key}" value="${esc(v)}" ${readonly ? 'readonly' : ''}></td>`;
      });
      if (!readonly) {
        tbody += `<td><button type="button" class="btn btn-ghost btn-sm" data-del-row="${rowsKey}">删除</button></td>`;
      }
      tbody += '</tr>';
    });
    if (!list.length && readonly) {
      tbody = '<tr><td colspan="' + columns.length + '" style="text-align:center;color:#999">暂无数据</td></tr>';
    }

    const addBtn = readonly
      ? ''
      : `<div class="pm-sub-toolbar"><button type="button" class="btn btn-secondary btn-sm" data-add-row="${rowsKey}">+ 添加行</button></div>`;

    return `
      <div class="pm-sheet-wrap" data-subtable="${rowsKey}">
        <div class="pm-section-head">${title}</div>
        ${addBtn}
        <table class="pm-sheet pm-dynamic-table">${thead}<tbody>${tbody}</tbody></table>
      </div>`;
  }

  const SUB_COLS = {
    teamMembers: [
      { key: 'name', label: '成员姓名' },
      { key: 'role', label: '项目角色' },
      { key: 'dept', label: '所在部门' },
      { key: 'duty', label: '职责' },
      { key: 'dateRange', label: '项目起止日期' },
      { key: 'workload', label: '投入频度及工作量' },
      { key: 'phone', label: '联系电话' },
      { key: 'supervisor', label: '主管经理' }
    ],
    stakeholders: [
      { key: 'name', label: '姓名' },
      { key: 'category', label: '类别' },
      { key: 'dept', label: '部门' },
      { key: 'position', label: '职务' }
    ],
    risks: [
      { key: 'seq', label: '序号' },
      { key: 'desc', label: '风险描述' },
      { key: 'probability', label: '发生概率' },
      { key: 'impact', label: '影响程度' },
      { key: 'level', label: '风险等级' },
      { key: 'responsePlan', label: '风险响应计划' },
      { key: 'owner', label: '责任人' },
      { key: 'status', label: '开放/关闭' }
    ],
    history: [
      { key: 'seq', label: '序号' },
      { key: 'changeTime', label: '变更时间' },
      { key: 'tasks', label: '涉及项目任务' },
      { key: 'points', label: '变更要点' },
      { key: 'reason', label: '变更理由' },
      { key: 'applicant', label: '申请人' },
      { key: 'approver', label: '审批人' }
    ],
    plans: [
      { key: 'stakeholder', label: '利益干系人' },
      { key: 'info', label: '所需信息' },
      { key: 'frequency', label: '频率' },
      { key: 'method', label: '方法' },
      { key: 'owner', label: '责任人' }
    ],
    taskStatus: [
      { key: 'keyTask', label: '关键任务' },
      { key: 'indicator', label: '状态指标' },
      { key: 'desc', label: '状态描述' }
    ],
    wbsRows: [
      { key: 'code', label: '分解代码' },
      { key: 'taskName', label: '任务名称' },
      { key: 'activity', label: '包含活动' },
      { key: 'hours', label: '工时估算' },
      { key: 'hr', label: '人力资源' },
      { key: 'otherRes', label: '其他资源' },
      { key: 'cost', label: '费用估算' },
      { key: 'duration', label: '工期' }
    ],
    tasks: [
      { key: 'phase', label: '任务/阶段' },
      { key: 'code', label: '编号' },
      { key: 'owner', label: '责任人' },
      { key: 'milestone', label: '关键里程碑' }
    ],
    rows: []
  };

  const GENERIC_TABLE_COLS = {
    strategic: [
      { key: 'goal', label: '战略目标' },
      { key: 'kpi', label: '关键指标' },
      { key: 'owner', label: '责任人' },
      { key: 'deadline', label: '完成时限' }
    ],
    orgPerf: [
      { key: 'dept', label: '部门' },
      { key: 'target', label: '目标' },
      { key: 'actual', label: '实际' },
      { key: 'gap', label: '差距说明' }
    ],
    pbc: [
      { key: 'item', label: '承诺事项' },
      { key: 'weight', label: '权重' },
      { key: 'status', label: '状态' }
    ],
    issue: [
      { key: 'title', label: '问题描述' },
      { key: 'severity', label: '严重程度' },
      { key: 'owner', label: '责任人' },
      { key: 'status', label: '状态' },
      { key: 'due', label: '计划关闭日' }
    ]
  };

  function buildFormHtml(type, doc, readonly) {
    let h = renderBaseGrid(doc, readonly);

    switch (type) {
      case 'charter':
        h += renderTextSection(
          '二、项目描述 — 项目背景与目的',
          '所有项目都源于一定的业务问题，本节应简要描述这些问题。',
          'background',
          doc.background,
          readonly
        );
        h += renderTextSection(
          '项目目标',
          '包括质量目标、进度目标、成本目标及交付产品特性等说明。',
          'goals',
          doc.goals,
          readonly
        );
        h += renderTextSection(
          '三、项目里程碑计划',
          '包括里程碑时间点及成果。',
          'milestonePlan',
          doc.milestonePlan,
          readonly
        );
        h += renderTextSection('四、评价标准', '说明何种情况下项目成果将被接受。', 'evaluationCriteria', doc.evaluationCriteria, readonly);
        h += renderTextSection('五、项目假定与约束条件', '说明项目主要假定及限制条件。', 'assumptions', doc.assumptions, readonly);
        h += renderSubTable('六、项目主要利益干系人', SUB_COLS.stakeholders, 'stakeholders', doc.stakeholders, readonly);
        h += renderSubTable('项目组成员', SUB_COLS.teamMembers, 'teamMembers', doc.teamMembers, readonly);
        break;

      case 'wbs':
        h += `<div class="pm-hint" style="border:1px solid #94a3b8;margin-bottom:0">（R-负责；As-辅助；I-通知；Ap-审批）</div>`;
        h += renderSubTable('二、工作分解结构', SUB_COLS.wbsRows, 'wbsRows', doc.wbsRows, readonly);
        break;

      case 'milestone':
        h += renderTextSection('二、项目进度说明', '按周跟踪任务，可在下方维护任务清单。', 'scheduleNote', doc.scheduleNote, readonly, 2);
        h += renderSubTable('项目进度表（任务 / 责任人 / 里程碑）', SUB_COLS.tasks, 'tasks', doc.tasks, readonly);
        break;

      case 'risk':
        h += `<div class="pm-sheet-wrap"><div class="pm-section-head">二、项目风险管理</div>
          <div class="pm-criteria"><strong>风险发生概率判断准则：</strong><br>
          高风险：发生概率 &gt;60%；中风险：30%–60%；低风险：&lt;30%。</div></div>`;
        h += renderSubTable('风险清单', SUB_COLS.risks, 'risks', doc.risks, readonly);
        break;

      case 'communication':
        h += renderSubTable('二、项目沟通计划', SUB_COLS.plans, 'plans', doc.plans, readonly);
        break;

      case 'change':
        h += renderSubTable('二、历史变更记录', SUB_COLS.history, 'history', doc.history, readonly);
        h += renderTextSection('三、请求变更信息', '建议的变更描述及参考资料', 'requestDesc', doc.requestDesc, readonly, 5);
        h += `<div class="pm-sheet-wrap"><div class="pm-section-head">四、影响分析</div>
          <table class="pm-sheet"><tbody>
            <tr><th class="pm-label">受影响基准计划</th><td colspan="3">
              <div class="pm-check-group" data-check-group="impactBaseline">
                ${['进度计划', '费用计划', '资源计划']
                  .map(
                    (x) =>
                      `<label><input type="checkbox" value="${x}" ${(doc.impactBaseline || []).includes(x) ? 'checked' : ''} ${readonly ? 'disabled' : ''}> ${x}</label>`
                  )
                  .join('')}
              </div></td></tr>
            <tr><th class="pm-label">需成本/进度影响分析</th><td>
              <select data-k="needAnalysis" ${readonly ? 'disabled' : ''}><option ${doc.needAnalysis === '是' ? 'selected' : ''}>是</option><option ${doc.needAnalysis === '否' ? 'selected' : ''}>否</option></select>
            </td><th class="pm-label">变更程度</th><td>
              <select data-k="changeLevel" ${readonly ? 'disabled' : ''}><option ${doc.changeLevel === '高' ? 'selected' : ''}>高</option><option ${doc.changeLevel === '中' ? 'selected' : ''}>中</option><option ${doc.changeLevel === '低' ? 'selected' : ''}>低</option></select>
            </td></tr>
            ${['costImpact', 'scheduleImpact', 'resourceImpact', 'ifNotChangeImpact']
              .map((k) => {
                const labels = {
                  costImpact: '对成本的影响',
                  scheduleImpact: '对进度的影响',
                  resourceImpact: '对资源的影响',
                  ifNotChangeImpact: '若不进行变更为何影响'
                };
                return `<tr><th class="pm-label">${labels[k]}</th><td colspan="3"><input data-k="${k}" value="${esc(doc[k])}" ${readonly ? 'readonly' : ''}></td></tr>`;
              })
              .join('')}
            <tr><th class="pm-label">申请人签字</th><td><input data-k="applicantSign" value="${esc(doc.applicantSign)}" ${readonly ? 'readonly' : ''}></td>
                <th class="pm-label">申请日期</th><td><input type="date" data-k="applyDate" value="${esc(doc.applyDate)}" ${readonly ? 'readonly' : ''}></td></tr>
          </tbody></table></div>`;
        h += `<div class="pm-sheet-wrap"><div class="pm-section-head">五、审批结果</div>
          <table class="pm-sheet"><tbody>
            <tr><th class="pm-label">审批意见</th><td colspan="3"><textarea data-k="approvalOpinion" rows="3" ${readonly ? 'readonly' : ''}>${esc(doc.approvalOpinion)}</textarea></td></tr>
            <tr><th class="pm-label">审批人签字</th><td><input data-k="approverSign" value="${esc(doc.approverSign)}" ${readonly ? 'readonly' : ''}></td>
                <th class="pm-label">日期</th><td><input type="date" data-k="approvalDate" value="${esc(doc.approvalDate)}" ${readonly ? 'readonly' : ''}></td></tr>
          </tbody></table></div>`;
        break;

      case 'meeting':
        h = ''; // 会议纪要用独立基本信息
        h += `<div class="pm-sheet-wrap"><div class="pm-section-head">会议纪要</div>
          <table class="pm-sheet"><tbody>
            ${[
              ['meetingName', '会议名称', 'convener', '召集人'],
              ['meetingDate', '会议日期', 'startTime', '开始时间'],
              ['location', '会议地点', 'duration', '持续时间'],
              ['recorder', '记录人', 'reviewer', '审核人']
            ]
              .map(
                ([k1, l1, k2, l2]) =>
                  `<tr><th class="pm-label">${l1}</th><td><input data-k="${k1}" value="${esc(doc[k1])}" ${readonly ? 'readonly' : ''}></td>
                   <th class="pm-label">${l2}</th><td><input data-k="${k2}" value="${esc(doc[k2])}" ${readonly ? 'readonly' : ''}></td></tr>`
              )
              .join('')}
            <tr><th class="pm-label">项目名称</th><td><input data-k="projectName" value="${esc(doc.projectName)}" ${readonly ? 'readonly' : ''}></td>
                <th class="pm-label">项目编号</th><td><input data-k="projectNo" value="${esc(doc.projectNo)}" ${readonly ? 'readonly' : ''}></td></tr>
          </tbody></table></div>`;
        h += renderTextSection('会议目标', '简要说明会议目标及期望结果', 'objectives', doc.objectives, readonly, 2);
        h += renderTextSection('参加人员', '列出参会人员及项目中角色', 'participants', doc.participants, readonly, 3);
        h += renderTextSection('发放材料', '列出会议讨论的项目资料', 'materials', doc.materials, readonly, 2);
        h += renderTextSection('发言记录', '记录发言人观点、意见和建议', 'speechRecord', doc.speechRecord, readonly, 4);
        h += renderTextSection('会议决议', '说明会议结论', 'resolution', doc.resolution, readonly, 2);
        h += `<div class="pm-sheet-wrap"><div class="pm-section-head">会议纪要发放范围</div>
          <table class="pm-sheet"><tbody>
            <tr><th class="pm-label">报送</th><td><input data-k="reportTo" value="${esc(doc.reportTo)}" ${readonly ? 'readonly' : ''}></td>
                <th class="pm-label">主送</th><td><input data-k="mainTo" value="${esc(doc.mainTo)}" ${readonly ? 'readonly' : ''}></td></tr>
            <tr><th class="pm-label">抄送</th><td colspan="3"><input data-k="copyTo" value="${esc(doc.copyTo)}" ${readonly ? 'readonly' : ''}></td></tr>
          </tbody></table></div>`;
        break;

      case 'status':
        h += `<div class="pm-sheet-wrap"><table class="pm-sheet"><tbody>
            <tr><th class="pm-label">当前项目状况</th><td><input data-k="currentStatus" value="${esc(doc.currentStatus)}" ${readonly ? 'readonly' : ''}></td>
                <th class="pm-label">汇报周期</th><td><input data-k="reportPeriod" value="${esc(doc.reportPeriod)}" ${readonly ? 'readonly' : ''}></td></tr>
          </tbody></table></div>`;
        h += renderSubTable('二、当前任务状态', SUB_COLS.taskStatus, 'taskStatus', doc.taskStatus, readonly);
        h += renderTextSection('三、本周期主要活动', '对本周期主要交付物进行总结', 'periodActivities', doc.periodActivities, readonly);
        h += renderTextSection('四、下一周期活动计划', '需与项目计划及 WBS 对应', 'nextPlan', doc.nextPlan, readonly);
        h += renderTextSection('五、财务状况', '', 'finance', doc.finance, readonly, 3);
        h += renderTextSection('六、上期遗留问题处理', '', 'legacyIssues', doc.legacyIssues, readonly, 3);
        h += renderTextSection('七、本期问题与求助', '', 'issuesHelp', doc.issuesHelp, readonly, 3);
        break;

      case 'strategic':
        h += renderSubTable('二、战略解码', GENERIC_TABLE_COLS.strategic, 'rows', doc.rows, readonly);
        break;
      case 'orgPerf':
        h += renderSubTable('二、组织绩效', GENERIC_TABLE_COLS.orgPerf, 'rows', doc.rows, readonly);
        break;
      case 'pbc':
        h += renderSubTable('二、个人绩效承诺', GENERIC_TABLE_COLS.pbc, 'rows', doc.rows, readonly);
        break;
      case 'issue':
        h += renderSubTable('二、问题清单', GENERIC_TABLE_COLS.issue, 'rows', doc.rows, readonly);
        break;
      case 'knowledge':
        h += renderTextSection('二、经验与发现', '', 'findings', doc.findings, readonly);
        h += renderTextSection('三、改进建议', '', 'suggestions', doc.suggestions, readonly);
        break;
      default:
        h += '<p>未配置表单模板</p>';
    }
    return h;
  }

  function collectForm(root, type) {
    const doc = { id: root.dataset.docId || uid(type) };
    root.querySelectorAll('[data-k]').forEach((el) => {
      doc[el.dataset.k] = el.value;
    });
    root.querySelectorAll('textarea[data-k]').forEach((el) => {
      doc[el.dataset.k] = el.value;
    });
    const cg = root.querySelector('[data-check-group="impactBaseline"]');
    if (cg) {
      doc.impactBaseline = [...cg.querySelectorAll('input:checked')].map((x) => x.value);
    }
    const allSubKeys = [...new Set([...Object.keys(SUB_COLS), ...Object.keys(GENERIC_TABLE_COLS)])];
    allSubKeys.forEach((key) => {
      const wrap = root.querySelector(`[data-subtable="${key}"]`);
      if (!wrap) return;
      const cols = (SUB_COLS[key] || GENERIC_TABLE_COLS[key] || []).map((c) => c.key);
      if (!cols.length) return;
      doc[key] = [];
      wrap.querySelectorAll('tbody tr[data-idx]').forEach((tr) => {
        const row = {};
        let has = false;
        cols.forEach((col) => {
          const inp = tr.querySelector(`[data-col="${col}"]`);
          if (inp) {
            row[col] = inp.value;
            if (inp.value.trim()) has = true;
          }
        });
        if (has) doc[key].push(row);
      });
    });
    return doc;
  }

  function emptyDoc(type) {
    const d = { id: uid(type) };
    global.pmBaseFields().forEach((f) => {
      d[f.key] = '';
    });
    if (type === 'meeting') {
      Object.assign(d, {
        meetingName: '',
        convener: '',
        meetingDate: '',
        startTime: '',
        location: '',
        duration: '',
        recorder: '',
        reviewer: ''
      });
    }
    ['stakeholders', 'teamMembers', 'risks', 'history', 'plans', 'taskStatus', 'wbsRows', 'tasks', 'rows'].forEach((k) => {
      if (SUB_COLS[k] || k === 'tasks' || k === 'rows') d[k] = [];
    });
    return d;
  }

  function listTitle(doc, type) {
    if (type === 'meeting') return doc.meetingName || '未命名会议';
    return doc.projectName || doc.projectNo || '未命名文档';
  }

  function listSubtitle(doc, type) {
    if (type === 'meeting') return (doc.meetingDate || '') + ' · ' + (doc.projectName || '');
    return (doc.projectNo || '') + ' · 项目经理：' + (doc.projectManager || '—') + ' · ' + (doc.prepareDate || '');
  }

  const ArchivePmForm = {
    type: null,
    mode: 'list',
    editingId: null,

    init(pageType) {
      this.type = pageType;
      this.mode = 'list';
      const pmMeta = PM_PAGE_TYPES[pageType];
      const meta = (window.ArchiveStore && window.ArchiveStore.PAGE_META[pageType]) || pmMeta;
      if (!pmMeta) return;

      document.title = (meta.title || pmMeta.title) + ' - 智训企业版';
      const hints = {
        charter: '按立项 Charter 模板：基本情况、项目描述、里程碑、评价标准、假定约束、利益干系人与项目组成员。',
        milestone: '按项目进度表：任务阶段、责任人、关键里程碑（周次甘特可在实施阶段扩展）。',
        risk: '含风险概率准则及风险清单（描述、概率、影响、等级、响应计划、责任人、开放/关闭）。',
        change: '含历史变更记录、变更描述、影响分析与审批结果。',
        meeting: '按会议纪要模板：基本信息、目标、参会人、材料、发言记录、决议与发放范围。',
        issue: '问题登记清单，可关联项目基本情况。',
        knowledge: '项目结束后的经验发现与改进建议。',
        wbs: '工作分解：分解代码、任务、活动、工时、人力、其他资源、费用、工期。',
        communication: '沟通计划：利益干系人、所需信息、频率、方法、责任人。',
        status: '状态报告：任务状态表、本周期活动、下周期计划、财务、遗留问题、本期求助。'
      };

      document.body.innerHTML = `
        <header class="topbar">
          <div class="topbar-left">
            <a href="培训管理系统 主页面.html?panel=archives" class="back-link"><i class="fas fa-arrow-left"></i> 返回项目档案</a>
            <div>
              <h1>${meta.title || pmMeta.title}</h1>
              <span class="sub">${meta.sub || ''}</span>
            </div>
          </div>
          <span class="topbar-tag"><i class="fas ${meta.icon || 'fa-table'}"></i> ${meta.tag || '项目管控'}</span>
        </header>
        <main class="page">
          <div class="hint-card">
            <strong>分节表单台账</strong>：字段结构对齐您提供的 Excel 管理表模板。支持列表 <strong>查</strong>、表单 <strong>增/改</strong>、记录 <strong>删</strong>；子表支持动态添加行。
            ${hints[pageType] ? '<br>' + hints[pageType] : ''}
          </div>
          <div class="card" id="pm-app"></div>
        </main>`;

      this.root = document.getElementById('pm-app');
      this.render();
    },

    render() {
      const meta = PM_PAGE_TYPES[this.type];
      const store = getStore(this.type);
      if (this.mode === 'list') {
        this.root.innerHTML = `
          <div class="list-view">
            <div class="toolbar" style="margin-bottom:16px">
              <button type="button" class="btn btn-primary" id="pm-btn-new">+ 新建</button>
              <input type="search" id="pm-search" class="search-input" placeholder="搜索项目名称、编号…" style="max-width:280px;margin-left:12px">
            </div>
            <div class="list-panel" id="pm-list"></div>
          </div>
          <div class="form-view" id="pm-form-view"></div>`;
        this.renderList(store, meta);
        document.getElementById('pm-btn-new').onclick = () => this.openForm(null, false);
        document.getElementById('pm-search').oninput = (e) => this.filterList(e.target.value);
      } else {
        this.renderFormView();
      }
    },

    filterList(q) {
      const store = getStore(this.type);
      const meta = PM_PAGE_TYPES[this.type];
      const qq = (q || '').toLowerCase();
      const filtered = store.filter((d) => {
        const t = (listTitle(d, this.type) + listSubtitle(d, this.type)).toLowerCase();
        return !qq || t.includes(qq);
      });
      this.renderList(filtered, meta);
    },

    renderList(store, meta) {
      const el = document.getElementById('pm-list');
      if (!el) return;
      if (!store.length) {
        el.innerHTML = '<p class="empty-hint">暂无记录，点击「新建」填写表单。</p>';
        return;
      }
      el.innerHTML = store
        .map(
          (doc) => `
        <div class="record-card" data-id="${doc.id}">
          <div>
            <h4>${esc(listTitle(doc, this.type))}</h4>
            <p>${esc(listSubtitle(doc, this.type))}</p>
          </div>
          <div class="table-actions">
            <button type="button" class="btn btn-ghost btn-sm" data-act="view">查看</button>
            <button type="button" class="btn btn-secondary btn-sm" data-act="edit">编辑</button>
            <button type="button" class="btn btn-ghost btn-sm" data-act="del">删除</button>
          </div>
        </div>`
        )
        .join('');

      el.querySelectorAll('.record-card').forEach((card) => {
        const id = card.dataset.id;
        card.querySelector('[data-act="view"]').onclick = () => this.openForm(id, true);
        card.querySelector('[data-act="edit"]').onclick = () => this.openForm(id, false);
        card.querySelector('[data-act="del"]').onclick = () => this.deleteDoc(id);
      });
    },

    openForm(id, readonly) {
      this.mode = 'form';
      this.readonly = readonly;
      this.editingId = id;
      const store = getStore(this.type);
      this.currentDoc = id ? store.find((d) => d.id === id) : emptyDoc(this.type);
      if (!this.currentDoc) this.currentDoc = emptyDoc(this.type);
      this.renderFormView();
    },

    renderFormView() {
      const meta = PM_PAGE_TYPES[this.type];
      const readonly = this.readonly;
      const html = buildFormHtml(this.type, this.currentDoc, readonly);
      this.root.innerHTML = `
        <div class="form-view active">
          <div class="form-actions-bar">
            <button type="button" class="btn btn-ghost" id="pm-back">← 返回列表</button>
            ${readonly ? '' : '<button type="button" class="btn btn-primary" id="pm-save">保存</button>'}
          </div>
          <div id="pm-form-root" data-doc-id="${this.currentDoc.id}">${html}</div>
        </div>`;

      document.getElementById('pm-back').onclick = () => {
        this.mode = 'list';
        this.render();
      };
      if (!readonly) {
        document.getElementById('pm-save').onclick = () => this.save();
        this.bindDynamicRows();
      }
    },

    bindDynamicRows() {
      const root = document.getElementById('pm-form-root');
      root.addEventListener('click', (e) => {
        const addKey = e.target.closest('[data-add-row]')?.dataset?.addRow;
        if (addKey) {
          e.preventDefault();
          this.addSubRow(addKey);
        }
        const delKey = e.target.closest('[data-del-row]')?.dataset?.delRow;
        if (delKey) {
          e.preventDefault();
          const tr = e.target.closest('tr');
          if (tr) tr.remove();
        }
      });
    },

    addSubRow(rowsKey) {
      const wrap = document.querySelector(`[data-subtable="${rowsKey}"]`);
      if (!wrap) return;
      const table = wrap.querySelector('table');
      const cols = SUB_COLS[rowsKey] || GENERIC_TABLE_COLS[this.type] || GENERIC_TABLE_COLS[rowsKey];
      if (!cols || !cols.length) return;
      const tbody = table.querySelector('tbody');
      const idx = tbody.querySelectorAll('tr[data-idx]').length;
      let row = '<tr data-idx="' + idx + '">';
      cols.forEach((c) => {
        row += `<td><input type="text" data-col="${c.key}"></td>`;
      });
      row += `<td><button type="button" class="btn btn-ghost btn-sm" data-del-row="${rowsKey}">删除</button></td>`;
      tbody.insertAdjacentHTML('beforeend', row);
    },

    save() {
      const root = document.getElementById('pm-form-root');
      const doc = collectForm(root, this.type);
      const store = getStore(this.type);
      const idx = store.findIndex((d) => d.id === doc.id);
      if (idx >= 0) store[idx] = doc;
      else store.push(doc);
      alert('已保存');
      this.mode = 'list';
      this.render();
    },

    deleteDoc(id) {
      if (!confirm('确定删除该记录？')) return;
      const store = getStore(this.type);
      const idx = store.findIndex((d) => d.id === id);
      if (idx >= 0) store.splice(idx, 1);
      this.renderList(store, PM_PAGE_TYPES[this.type]);
    }
  };

  global.ArchivePmForm = ArchivePmForm;
})(typeof window !== 'undefined' ? window : global);
