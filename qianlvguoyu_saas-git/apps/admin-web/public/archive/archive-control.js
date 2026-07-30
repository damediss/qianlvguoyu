/* 项目管控 · 十大管理表 — 表单增删改查 */
window.ArchiveControl = (function () {
  const { mockData, CONTROL_TABLES, CONTROL_FORM_SCHEMAS, PAGE_META } = window.ArchiveStore;
  let entity = '';
  let filterQ = '';
  let modalState = { mode: 'add', id: null };

  const BADGE_MAP = {
    '已完成': 'success', '已解决': 'success', '已批准': 'success', '已入库': 'success', '已关闭': 'gray',
    '进行中': 'info', '审批中': 'warning', '未解决': 'danger', '未完成': 'warning', '延期': 'danger',
    '高': 'danger', '中': 'warning', '低': 'gray', '监控中': 'info', '已缓解': 'success', '开放': 'warning'
  };

  function badge(text) {
    const t = BADGE_MAP[text] || 'info';
    return `<span class="badge badge-${t}">${text ?? '—'}</span>`;
  }

  function filterList(list) {
    if (!filterQ) return list;
    const q = filterQ.toLowerCase();
    const keys = Object.keys(list[0] || {});
    return list.filter(row => keys.some(k => String(row[k] ?? '').toLowerCase().includes(q)));
  }

  function getSchema() {
    return CONTROL_FORM_SCHEMAS[entity] || [];
  }

  function getDefaultRow() {
    return CONTROL_TABLES[entity]?.defaultRow() || { id: Date.now() };
  }

  function renderField(f, val, readonly) {
    const ro = readonly ? 'disabled' : '';
    const req = f.required && !readonly ? '<span class="req">*</span>' : '';
    if (f.type === 'textarea') {
      const cls = f.full ? ' form-group full' : ' form-group';
      return `<div class="${cls.trim()}"><label>${f.label}${req}</label><textarea data-field="${f.key}" placeholder="${f.placeholder || ''}" ${ro}>${val}</textarea></div>`;
    }
    if (f.type === 'select') {
      const opts = (f.options || []).map(o =>
        `<option value="${o}" ${val === o ? 'selected' : ''}>${o}</option>`
      ).join('');
      return `<div class="form-group"><label>${f.label}${req}</label><select data-field="${f.key}" ${ro}>${opts}</select></div>`;
    }
    const type = f.type || 'text';
    const min = f.min != null ? ` min="${f.min}"` : '';
    const max = f.max != null ? ` max="${f.max}"` : '';
    return `<div class="form-group"><label>${f.label}${req}</label><input type="${type}" data-field="${f.key}" value="${val}" placeholder="${f.placeholder || ''}"${min}${max} ${ro}></div>`;
  }

  function renderFormBody(row, readonly) {
    return `<div class="form-grid">${getSchema().map(f => renderField(f, row[f.key] ?? '', readonly)).join('')}</div>`;
  }

  function validateForm(fields) {
    const missing = getSchema().filter(f => f.required && !String(fields[f.key] ?? '').trim());
    if (missing.length) {
      alert('请填写必填项：' + missing.map(f => f.label).join('、'));
      return false;
    }
    return true;
  }

  function collectFormFields() {
    const fields = {};
    document.querySelectorAll('#modalBody [data-field]').forEach(el => {
      const k = el.dataset.field;
      if (el.type === 'number') fields[k] = parseFloat(el.value) || 0;
      else fields[k] = el.value;
    });
    return fields;
  }

  function openFormModal(mode, id) {
    modalState = { mode, id };
    let row = mode === 'edit' || mode === 'view'
      ? (mockData[entity].find(x => String(x.id) === String(id)) || {})
      : getDefaultRow();
    const meta = PAGE_META[entity];
    const titles = { add: '新增记录', edit: '编辑记录', view: '查看详情' };
    document.getElementById('modalTitle').textContent = titles[mode] + ' — ' + meta.title;
    document.getElementById('modalBody').innerHTML = renderFormBody(row, mode === 'view');
    document.getElementById('modalSave').style.display = mode === 'view' ? 'none' : '';
    document.getElementById('modal').classList.add('show');
  }

  function saveForm() {
    const fields = collectFormFields();
    if (!validateForm(fields)) return;
    const list = mockData[entity];
    if (modalState.mode === 'add') {
      const row = { ...getDefaultRow(), ...fields };
      if (entity === 'risk' && !String(row.id).startsWith('RK')) row.id = 'RK' + Date.now();
      if (entity === 'issue' && !String(row.id).startsWith('ISS')) row.id = 'ISS-' + Date.now();
      if (entity === 'change' && !String(row.id).startsWith('CHG')) row.id = 'CHG-' + Date.now();
      if (fields.id) row.id = fields.id;
      list.push(row);
    } else {
      const idx = list.findIndex(x => String(x.id) === String(modalState.id));
      if (idx !== -1) list[idx] = { ...list[idx], ...fields };
    }
    document.getElementById('modal').classList.remove('show');
    renderTable();
    updateCount();
  }

  function deleteRow(id) {
    if (!confirm('确定删除该条记录？删除后不可恢复（演示）。')) return;
    const list = mockData[entity];
    const idx = list.findIndex(x => String(x.id) === String(id));
    if (idx !== -1) list.splice(idx, 1);
    renderTable();
    updateCount();
  }

  function actionCell(id) {
    return `<div class="action-btns">
      <button type="button" class="action-btn" data-action="view" data-id="${id}" title="查看"><i class="fas fa-eye"></i></button>
      <button type="button" class="action-btn" data-action="edit" data-id="${id}" title="编辑"><i class="fas fa-edit"></i></button>
      <button type="button" class="action-btn del" data-action="delete" data-id="${id}" title="删除"><i class="fas fa-trash-alt"></i></button>
    </div>`;
  }

  function renderTable() {
    const cfg = CONTROL_TABLES[entity];
    const list = filterList(mockData[entity] || []);
    const ths = cfg.columns.map(c => `<th>${c.label}</th>`).join('') + '<th style="width:120px">操作</th>';
    const rows = list.length ? list.map(row => {
      const tds = cfg.columns.map(c => {
        const v = row[c.key];
        if (c.render) return `<td>${c.render(v, row)}</td>`;
        if (c.badge) return `<td>${badge(v)}</td>`;
        return `<td>${v ?? '—'}</td>`;
      }).join('');
      return `<tr>${tds}<td>${actionCell(row.id)}</td></tr>`;
    }).join('') : `<tr><td colspan="${cfg.columns.length + 1}" class="empty-cell">暂无数据，点击「新增记录」填写表单</td></tr>`;
    document.getElementById('tableMain').innerHTML = `<thead><tr>${ths}</tr></thead><tbody>${rows}</tbody>`;
  }

  function updateCount() {
    const el = document.getElementById('recordCount');
    if (el) el.textContent = `共 ${mockData[entity].length} 条`;
  }

  function bindEvents() {
    document.getElementById('searchMain')?.addEventListener('input', e => {
      filterQ = e.target.value;
      renderTable();
    });
    document.getElementById('btnAdd')?.addEventListener('click', () => openFormModal('add'));
    document.getElementById('modalClose')?.addEventListener('click', closeModal);
    document.getElementById('modalCancel')?.addEventListener('click', closeModal);
    document.getElementById('modalSave')?.addEventListener('click', saveForm);
    document.getElementById('modal')?.addEventListener('click', e => {
      if (e.target.id === 'modal') closeModal();
    });
    document.body.addEventListener('click', e => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      const id = btn.dataset.id;
      if (btn.dataset.action === 'view') openFormModal('view', id);
      if (btn.dataset.action === 'edit') openFormModal('edit', id);
      if (btn.dataset.action === 'delete') deleteRow(id);
    });
  }

  function closeModal() {
    document.getElementById('modal').classList.remove('show');
    document.getElementById('modalSave').style.display = '';
  }

  function init(ent) {
    entity = ent;
    const meta = PAGE_META[entity];
    if (!meta || !CONTROL_TABLES[entity]) return;
    document.title = meta.title + ' - 智训企业版';

    document.body.innerHTML = `
      <header class="topbar">
        <div class="topbar-left">
          <a href="培训管理系统 主页面.html?panel=archives" class="back-link"><i class="fas fa-arrow-left"></i> 返回项目档案</a>
          <div>
            <h1>${meta.title}</h1>
            <span class="sub">${meta.sub}</span>
          </div>
        </div>
        <span class="topbar-tag"><i class="fas ${meta.icon}"></i> ${meta.tag}</span>
      </header>
      <main class="page">
        <div class="hint-card">
          <strong>表单台账</strong>：已按需求规格说明书预填演示数据。支持 <strong>查</strong>（搜索/查看）、<strong>增</strong>、<strong>改</strong>、<strong>删</strong>。立项表→Charter；总体计划/节点→里程碑；风险/问题/变更→扩展管理表对应字段。
        </div>
        <div class="card">
          <div class="card-head">
            <div class="title-wrap"><i class="fas ${meta.icon}"></i> ${meta.title}</div>
            <span class="record-count" id="recordCount"></span>
          </div>
          <div class="toolbar">
            <input type="text" class="search-input" id="searchMain" placeholder="搜索本表任意字段…">
            <div class="toolbar-actions">
              <button type="button" class="btn btn-primary" id="btnAdd"><i class="fas fa-plus"></i> 新增记录</button>
            </div>
          </div>
          <div class="table-wrap"><table class="data-table" id="tableMain"></table></div>
        </div>
      </main>
      <div class="modal-overlay" id="modal">
        <div class="modal-box modal-box-form">
          <div class="modal-head">
            <h3 id="modalTitle">表单</h3>
            <button type="button" class="modal-close" id="modalClose">&times;</button>
          </div>
          <div class="modal-body" id="modalBody"></div>
          <div class="modal-foot">
            <button type="button" class="btn btn-secondary" id="modalCancel">取消</button>
            <button type="button" class="btn btn-primary" id="modalSave"><i class="fas fa-save"></i> 保存</button>
          </div>
        </div>
      </div>`;

    bindEvents();
    renderTable();
    updateCount();
  }

  return { init };
})();
