/* 人员档案 — 独立页面 */
(function () {
  const { mockData, PROJECTS } = window.ArchiveStore;
  let activeSub = 'teachers';
  let teacherFilter = 'all';
  let studentFilter = 'all';
  let filterQ = { teachers: '', students: '' };
  let modalState = { entity: null, mode: 'add', id: null };

  function badge(text, type) {
    const map = { '已签署': 'success', '已确认': 'success', '已通过': 'success', '已齐全': 'success', '已结算': 'success',
      '在职': 'success', '在培': 'info', '待确认': 'warning', '待审核': 'warning', '补正中': 'warning', '审批中': 'warning',
      '未签署': 'warning', '未提交': 'danger', '缺勤预警': 'danger', '综合预警': 'danger', '作业预警': 'danger' };
    const t = type || map[text] || 'info';
    return `<span class="badge badge-${t}">${text || '—'}</span>`;
  }

  function daysUntil(d) {
    if (!d || d === '—') return 999;
    const x = new Date(d);
    return isNaN(x) ? 999 : Math.ceil((x - new Date()) / 86400000);
  }
  function isCertWarn(t) { const d = daysUntil(t.certExpiry); return d >= 0 && d <= 90; }
  function isAgreePending(t) { return t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署'); }
  function isStudentWarn(s) { return !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60; }

  function filterList(list, q, keys) {
    if (!q) return list;
    const lower = q.toLowerCase();
    return list.filter(r => keys.some(k => String(r[k] ?? '').toLowerCase().includes(lower)));
  }

  function applyTeacherFilter(list) {
    if (teacherFilter === 'certWarn') return list.filter(isCertWarn);
    if (teacherFilter === 'agreement') return list.filter(isAgreePending);
    if (teacherFilter === 'settle') return list.filter(t => t.settleStatus === '待结算' || t.settleStatus === '未结算');
    return list;
  }
  function applyStudentFilter(list) {
    if (studentFilter === 'confirm') return list.filter(s => s.confirmStatus === '待确认');
    if (studentFilter === 'audit') return list.filter(s => s.auditStatus === '待审核');
    if (studentFilter === 'material') return list.filter(s => ['未提交', '补正中', '审核中'].includes(s.materialStatus));
    if (studentFilter === 'warn') return list.filter(isStudentWarn);
    return list;
  }

  function countTeachers(k) {
    const all = mockData.teachers;
    if (k === 'all') return all.length;
    if (k === 'certWarn') return all.filter(isCertWarn).length;
    if (k === 'agreement') return all.filter(isAgreePending).length;
    if (k === 'settle') return all.filter(t => t.settleStatus === '待结算' || t.settleStatus === '未结算').length;
    return 0;
  }
  function countStudents(k) {
    const all = mockData.students;
    if (k === 'all') return all.length;
    if (k === 'confirm') return all.filter(s => s.confirmStatus === '待确认').length;
    if (k === 'audit') return all.filter(s => s.auditStatus === '待审核').length;
    if (k === 'material') return all.filter(s => ['未提交', '补正中', '审核中'].includes(s.materialStatus)).length;
    if (k === 'warn') return all.filter(isStudentWarn).length;
    return 0;
  }

  function renderKpi() {
    const certWarn = mockData.teachers.filter(isCertWarn).length;
    const pendingConfirm = mockData.students.filter(s => s.confirmStatus === '待确认').length;
    const pendingAudit = mockData.students.filter(s => s.auditStatus === '待审核').length;
    const warnStudents = mockData.students.filter(isStudentWarn).length;
    document.getElementById('personnelKpi').innerHTML = `
      <div class="stat-card"><div class="val">${mockData.teachers.length}</div><div class="lbl">在册讲师</div></div>
      <div class="stat-card ${certWarn ? 'warn' : ''}"><div class="val">${certWarn}</div><div class="lbl">资质90天内到期</div></div>
      <div class="stat-card"><div class="val">${mockData.students.length}</div><div class="lbl">参训学员</div></div>
      <div class="stat-card ${pendingConfirm ? 'warn' : ''}"><div class="val">${pendingConfirm}</div><div class="lbl">待企业确认</div></div>
      <div class="stat-card ${pendingAudit ? 'warn' : ''}"><div class="val">${pendingAudit}</div><div class="lbl">待资格审核</div></div>
      <div class="stat-card ${warnStudents ? 'danger' : ''}"><div class="val">${warnStudents}</div><div class="lbl">过程预警学员</div></div>`;
  }

  function renderTeacherChips() {
    const chips = [['all', '全部'], ['certWarn', '资质即将到期'], ['agreement', '协议待签署'], ['settle', '课酬待结算']];
    document.getElementById('teacherChips').innerHTML = chips.map(([k, l]) =>
      `<button type="button" class="filter-chip ${teacherFilter === k ? 'active' : ''}" data-tf="${k}">${l}<span class="count">${countTeachers(k)}</span></button>`
    ).join('');
  }
  function renderStudentChips() {
    const chips = [['all', '全部'], ['confirm', '待企业确认'], ['audit', '待资格审核'], ['material', '材料待办'], ['warn', '过程预警']];
    document.getElementById('studentChips').innerHTML = chips.map(([k, l]) =>
      `<button type="button" class="filter-chip ${studentFilter === k ? 'active' : ''}" data-sf="${k}">${l}<span class="count">${countStudents(k)}</span></button>`
    ).join('');
  }

  function actionCell(entity, id) {
    return `<div class="action-btns">
      <button type="button" class="action-btn" data-action="edit" data-entity="${entity}" data-id="${id}"><i class="fas fa-edit"></i></button>
      <button type="button" class="action-btn del" data-action="delete" data-entity="${entity}" data-id="${id}"><i class="fas fa-trash-alt"></i></button>
    </div>`;
  }

  function certCell(t) {
    const days = daysUntil(t.certExpiry);
    if (days < 0) return badge('已过期', 'danger');
    if (days <= 90) return badge(t.certExpiry + '（' + days + '天）', 'warning');
    return t.certExpiry || '—';
  }

  function renderTeachers() {
    let list = filterList(mockData.teachers, filterQ.teachers, ['id', 'name', 'skills', 'project', 'certName']);
    list = applyTeacherFilter(list);
    document.getElementById('tableTeachers').innerHTML = `<thead><tr>
      <th>编号</th><th>姓名</th><th>角色</th><th>职称/等级</th><th>工种</th><th>项目</th><th>资质</th><th>到期</th><th>协议</th><th>承诺</th><th>课酬</th><th>评价</th><th>状态</th><th>操作</th>
    </tr></thead><tbody>${list.map(t => `<tr class="${isCertWarn(t) || isAgreePending(t) ? 'row-warn' : ''}">
      <td>${t.id}</td><td><strong>${t.name}</strong><br><span style="font-size:11px;color:var(--text-secondary)">${t.phone}</span></td>
      <td>${badge(t.role)}</td><td>${t.title}<br><span style="font-size:11px;color:var(--text-secondary)">${t.teachLevel}</span></td>
      <td>${t.skills}</td><td>${t.project}</td><td>${t.certName}</td><td>${certCell(t)}</td>
      <td>${badge(t.agreement)}</td><td>${badge(t.nda)}</td><td>${badge(t.settleStatus)}</td>
      <td>${t.rating ? t.rating + ' ★' : '—'}</td><td>${badge(t.status)}</td><td>${actionCell('teachers', t.id)}</td>
    </tr>`).join('') || '<tr><td colspan="14">暂无数据</td></tr>'}</tbody>`;
    renderTeacherChips();
    renderKpi();
  }

  function renderStudents() {
    let list = filterList(mockData.students, filterQ.students, ['id', 'name', 'enterprise', 'trade', 'className']);
    list = applyStudentFilter(list);
    document.getElementById('tableStudents').innerHTML = `<thead><tr>
      <th>编号</th><th>姓名</th><th>企业</th><th>工种</th><th>项目</th><th>班级</th>
      <th>参训</th><th>确认</th><th>审核</th><th>材料</th><th>出勤</th><th>作业</th><th>模拟</th><th>预警</th><th>进度</th><th>证书</th><th>操作</th>
    </tr></thead><tbody>${list.map(s => `<tr class="${isStudentWarn(s) ? 'row-warn' : ''}">
      <td>${s.id}</td><td><strong>${s.name}</strong><br><span style="font-size:11px;color:var(--text-secondary)">${s.idCard}</span></td>
      <td>${s.enterprise}</td><td>${s.trade}</td><td>${s.project}</td><td>${s.className}</td>
      <td>${badge(s.enrollStatus)}</td><td>${badge(s.confirmStatus)}</td><td>${badge(s.auditStatus)}</td><td>${badge(s.materialStatus)}</td>
      <td>${s.attendance}%</td><td>${s.homework}%</td><td>${s.mockScore || '—'}</td>
      <td>${s.warnLevel ? badge(s.warnLevel, 'danger') : '—'}</td>
      <td><span class="progress-bar"><i style="width:${s.progress}%"></i></span>${s.progress}%</td>
      <td>${badge(s.cert)}</td><td>${actionCell('students', s.id)}</td>
    </tr>`).join('') || '<tr><td colspan="16">暂无数据</td></tr>'}</tbody>`;
    renderStudentChips();
    renderKpi();
  }

  const FORM = {
    teachers: [
      { key: 'id', label: '编号' }, { key: 'name', label: '姓名' }, { key: 'gender', label: '性别', type: 'select', options: ['男', '女'] },
      { key: 'role', label: '角色', type: 'select', options: ['主讲', '助教', '班主任', '跟班'] },
      { key: 'title', label: '职称', type: 'select', options: ['初级', '中级', '高级', '专家'] },
      { key: 'teachLevel', label: '授课等级' }, { key: 'skills', label: '工种' },
      { key: 'project', label: '项目', type: 'select', options: PROJECTS },
      { key: 'certName', label: '资质名称' }, { key: 'certExpiry', label: '到期日' },
      { key: 'agreement', label: '协议', type: 'select', options: ['已签署', '审批中', '未签署'] },
      { key: 'nda', label: '保密承诺', type: 'select', options: ['已签署', '未签署'] },
      { key: 'phone', label: '电话' }, { key: 'status', label: '状态', type: 'select', options: ['在职', '兼职'] }
    ],
    students: [
      { key: 'id', label: '编号' }, { key: 'name', label: '姓名' }, { key: 'enterprise', label: '企业' },
      { key: 'trade', label: '工种' }, { key: 'className', label: '班级' },
      { key: 'confirmStatus', label: '企业确认', type: 'select', options: ['待确认', '已确认'] },
      { key: 'auditStatus', label: '资格审核', type: 'select', options: ['待审核', '已通过', '未通过'] },
      { key: 'materialStatus', label: '材料', type: 'select', options: ['未提交', '审核中', '补正中', '已齐全'] },
      { key: 'progress', label: '进度%', type: 'number' },
      { key: 'cert', label: '证书', type: 'select', options: ['未开始', '培训中', '已结业', '已获证'] }
    ]
  };

  function openModal(entity, mode, id) {
    modalState = { entity, mode, id };
    const row = mode === 'edit' ? (mockData[entity].find(x => String(x.id) === String(id)) || {}) : (
      entity === 'teachers' ? { id: 'T' + Date.now(), name: '', gender: '男', role: '主讲', title: '中级', teachLevel: '', skills: '', project: PROJECTS[0], certName: '', certExpiry: '', agreement: '未签署', nda: '未签署', phone: '', status: '兼职', rating: 0, settleStatus: '未结算' } :
      { id: 'S' + Date.now(), name: '', enterprise: '', trade: '', className: '', confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', progress: 0, cert: '未开始', idCard: '', project: PROJECTS[0], enrollStatus: '待入班', attendance: 0, homework: 0, mockScore: 0, warnLevel: '' }
    );
    document.getElementById('modalTitle').textContent = (mode === 'add' ? '新增' : '编辑') + (entity === 'teachers' ? '讲师' : '学员');
    document.getElementById('modalBody').innerHTML = FORM[entity].map(f => {
      const val = row[f.key] ?? '';
      if (f.type === 'select') {
        return `<div class="form-group"><label>${f.label}</label><select data-field="${f.key}">${(f.options || []).map(o => `<option ${val === o ? 'selected' : ''}>${o}</option>`).join('')}</select></div>`;
      }
      return `<div class="form-group"><label>${f.label}</label><input data-field="${f.key}" value="${val}"></div>`;
    }).join('');
    document.querySelector('#modal .modal-box').classList.add('wide');
    document.getElementById('modal').classList.add('show');
  }

  function saveModal() {
    const { entity, mode, id } = modalState;
    const fields = {};
    document.querySelectorAll('#modalBody [data-field]').forEach(el => {
      fields[el.dataset.field] = el.type === 'number' ? +el.value || 0 : el.value;
    });
    if (mode === 'add') mockData[entity].push(fields);
    else {
      const i = mockData[entity].findIndex(x => String(x.id) === String(id));
      if (i !== -1) mockData[entity][i] = { ...mockData[entity][i], ...fields };
    }
    document.getElementById('modal').classList.remove('show');
    entity === 'teachers' ? renderTeachers() : renderStudents();
  }

  function switchSub(sub) {
    activeSub = sub;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.sub === sub));
    document.getElementById('wrapTeachers').style.display = sub === 'teachers' ? 'block' : 'none';
    document.getElementById('wrapStudents').style.display = sub === 'students' ? 'block' : 'none';
    sub === 'teachers' ? renderTeachers() : renderStudents();
  }

  document.body.innerHTML = `
    <header class="topbar">
      <div class="topbar-left">
        <a href="培训管理系统 主页面.html?panel=archives" class="back-link"><i class="fas fa-arrow-left"></i> 返回项目档案</a>
        <div><h1>人员档案</h1><span class="sub">5M·人：师资与教务 + 参训学员</span></div>
      </div>
      <span class="topbar-tag"><i class="fas fa-user-friends"></i> 项目档案</span>
    </header>
    <main class="page">
      <div class="stat-row" id="personnelKpi"></div>
      <div class="hint-card"><strong>人机料法环 · 人</strong>：讲师资质/协议/课酬；学员名单确认、资格审核、材料审核与过程预警。</div>
      <div class="tab-bar">
        <button type="button" class="tab-btn active" data-sub="teachers"><i class="fas fa-chalkboard-teacher"></i> 师资与教务</button>
        <button type="button" class="tab-btn" data-sub="students"><i class="fas fa-user-graduate"></i> 参训学员</button>
      </div>
      <div class="card" id="wrapTeachers">
        <div class="card-head"><div class="title-wrap"><i class="fas fa-id-card"></i> 讲师档案与资质</div></div>
        <div class="filter-chips" id="teacherChips"></div>
        <div class="toolbar">
          <input class="search-input" id="searchTeachers" placeholder="搜索姓名、工种、项目…">
          <div class="toolbar-actions">
            <button type="button" class="btn btn-secondary btn-sm" id="btnImportT"><i class="fas fa-file-import"></i> 导入讲师池</button>
            <button type="button" class="btn btn-primary btn-sm" data-add="teachers"><i class="fas fa-plus"></i> 新增讲师</button>
          </div>
        </div>
        <div class="table-wrap"><table class="data-table" id="tableTeachers"></table></div>
      </div>
      <div class="card" id="wrapStudents" style="display:none">
        <div class="card-head"><div class="title-wrap"><i class="fas fa-users"></i> 培训人员台账</div></div>
        <div class="filter-chips" id="studentChips"></div>
        <div class="toolbar">
          <input class="search-input" id="searchStudents" placeholder="搜索姓名、企业、班级…">
          <div class="toolbar-actions">
            <button type="button" class="btn btn-secondary btn-sm" id="btnImportS"><i class="fas fa-file-import"></i> 导入名单</button>
            <button type="button" class="btn btn-primary btn-sm" data-add="students"><i class="fas fa-plus"></i> 新增学员</button>
          </div>
        </div>
        <div class="table-wrap"><table class="data-table" id="tableStudents"></table></div>
      </div>
    </main>
    <div class="modal-overlay" id="modal">
      <div class="modal-box wide">
        <div class="modal-head"><h3 id="modalTitle">编辑</h3><button type="button" class="modal-close" id="modalClose">&times;</button></div>
        <div class="modal-body" id="modalBody"></div>
        <div class="modal-foot"><button type="button" class="btn btn-secondary" id="modalCancel">取消</button><button type="button" class="btn btn-primary" id="modalSave">保存</button></div>
      </div>
    </div>`;

  document.querySelectorAll('.tab-btn').forEach(b => b.addEventListener('click', () => switchSub(b.dataset.sub)));
  document.getElementById('teacherChips').addEventListener('click', e => {
    const c = e.target.closest('[data-tf]');
    if (c) { teacherFilter = c.dataset.tf; renderTeachers(); }
  });
  document.getElementById('studentChips').addEventListener('click', e => {
    const c = e.target.closest('[data-sf]');
    if (c) { studentFilter = c.dataset.sf; renderStudents(); }
  });
  document.getElementById('searchTeachers').addEventListener('input', e => { filterQ.teachers = e.target.value; renderTeachers(); });
  document.getElementById('searchStudents').addEventListener('input', e => { filterQ.students = e.target.value; renderStudents(); });
  document.getElementById('btnImportT').addEventListener('click', () => alert('演示：从讲师池导入并校验资质。'));
  document.getElementById('btnImportS').addEventListener('click', () => alert('演示：导入参训名单并进入确认流。'));
  document.querySelectorAll('[data-add]').forEach(b => b.addEventListener('click', () => openModal(b.dataset.add, 'add')));
  document.getElementById('modalClose').onclick = document.getElementById('modalCancel').onclick = () => document.getElementById('modal').classList.remove('show');
  document.getElementById('modalSave').onclick = saveModal;
  document.body.addEventListener('click', e => {
    const ed = e.target.closest('[data-action="edit"]');
    if (ed) openModal(ed.dataset.entity, 'edit', ed.dataset.id);
    const del = e.target.closest('[data-action="delete"]');
    if (del && confirm('确定删除？')) {
      const list = mockData[del.dataset.entity];
      const i = list.findIndex(x => String(x.id) === String(del.dataset.id));
      if (i !== -1) list.splice(i, 1);
      del.dataset.entity === 'teachers' ? renderTeachers() : renderStudents();
    }
  });

  renderTeachers();
})();
