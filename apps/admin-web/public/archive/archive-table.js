/* 项目档案 — 通用列表页（与主站风格一致） */
window.ArchiveTable = (function () {
  const { mockData, CONTROL_TABLES, FORM_SCHEMAS, PAGE_META, PROJECTS } = window.ArchiveStore;
  let modalState = { entity: null, mode: 'add', id: null };
  let filterQ = '';

  const BADGE_MAP = {
    '已完成': 'success', '已解决': 'success', '已批准': 'success', '已入库': 'success', '已关闭': 'gray',
    '进行中': 'info', '审批中': 'warning', '未解决': 'danger', '未完成': 'warning', '延期': 'danger',
    '高': 'danger', '中': 'warning', '低': 'gray', '正常': 'success', '维修': 'warning', '报废': 'danger',
    '可用': 'success', '占用': 'warning', '待入库': 'info'
  };

  function badge(text, type) {
    const t = type || BADGE_MAP[text] || 'info';
    return `<span class="badge badge-${t}">${text || '—'}</span>`;
  }

  function filterList(list, q, keys) {
    if (!q) return list;
    const lower = q.toLowerCase();
    return list.filter(row => keys.some(k => String(row[k] ?? '').toLowerCase().includes(lower)));
  }

  function actionCell(entity, id) {
    return `<div class="action-btns">
      <button type="button" class="action-btn" data-action="edit" data-entity="${entity}" data-id="${id}" title="编辑"><i class="fas fa-edit"></i></button>
      <button type="button" class="action-btn del" data-action="delete" data-entity="${entity}" data-id="${id}" title="删除"><i class="fas fa-trash-alt"></i></button>
    </div>`;
  }

  function renderTopbar(meta) {
    return `
      <header class="topbar">
        <div class="topbar-left">
          <a href="培训管理系统 主页面.html?panel=archives" class="back-link"><i class="fas fa-arrow-left"></i> 返回项目档案</a>
          <div>
            <h1>${meta.title}</h1>
            <span class="sub">${meta.sub}</span>
          </div>
        </div>
        <span class="topbar-tag"><i class="fas ${meta.icon}"></i> ${meta.tag}</span>
      </header>`;
  }

  function renderModal() {
    return `
      <div class="modal-overlay" id="modal">
        <div class="modal-box">
          <div class="modal-head">
            <h3 id="modalTitle">编辑</h3>
            <button type="button" class="modal-close" id="modalClose">&times;</button>
          </div>
          <div class="modal-body" id="modalBody"></div>
          <div class="modal-foot">
            <button type="button" class="btn btn-secondary" id="modalCancel">取消</button>
            <button type="button" class="btn btn-primary" id="modalSave">保存</button>
          </div>
        </div>
      </div>`;
  }

  function getColumns(entity) {
    if (CONTROL_TABLES[entity]) return CONTROL_TABLES[entity].columns;
    return null;
  }

  function renderEquipment(entity) {
    const list = filterList(mockData.equipment, filterQ, ['name', 'model', 'id']);
    const el = document.getElementById('tableMain');
    el.innerHTML = `<thead><tr>
      <th>设备ID</th><th>设备名称</th><th>型号</th><th>数量</th><th>完好率</th><th>下次保养</th><th>状态</th><th>操作</th>
    </tr></thead><tbody>${list.map(e => `<tr>
      <td>${e.id}</td><td>${e.name}</td><td>${e.model}</td><td>${e.qty}</td><td>${e.rate}%</td><td>${e.nextMaintain}</td>
      <td>${badge(e.status)}</td><td>${actionCell(entity, e.id)}</td>
    </tr>`).join('')}</tbody>`;
  }

  function renderMaterials(entity) {
    const list = filterList(mockData.materials, filterQ, ['name', 'id', 'spec']);
    const el = document.getElementById('tableMain');
    el.innerHTML = `<thead><tr>
      <th>物料编码</th><th>名称</th><th>规格</th><th>库存</th><th>安全库存</th><th>单位</th><th>上次入库</th><th>操作</th>
    </tr></thead><tbody>${list.map(m => {
      const warn = m.stock < m.safety;
      return `<tr class="${warn ? 'row-warn' : ''}">
        <td>${m.id}</td><td>${m.name}</td><td>${m.spec}</td>
        <td><strong>${m.stock}</strong></td><td>${m.safety}</td><td>${m.unit}</td><td>${m.lastIn}</td>
        <td><div class="stock-ops">
          <button type="button" class="btn btn-secondary btn-sm" data-stock="in" data-id="${m.id}">+入库</button>
          <button type="button" class="btn btn-secondary btn-sm" data-stock="out" data-id="${m.id}">-出库</button>
        </div>${actionCell(entity, m.id)}</td>
      </tr>`;
    }).join('')}</tbody>`;
  }

  function renderRules(entity) {
    const list = filterList(mockData.rules, filterQ, ['name']);
    const el = document.getElementById('tableMain');
    el.innerHTML = `<thead><tr>
      <th>规则名称</th><th>版本</th><th>生效日期</th><th>附件</th><th>操作</th>
    </tr></thead><tbody>${list.map(r => `<tr>
      <td><a href="#" class="link-file rule-link" data-id="${r.id}">${r.name}</a></td>
      <td>${r.version}</td><td>${r.effective}</td>
      <td><a class="link-file" href="#">${r.file}</a></td>
      <td>${actionCell(entity, r.id)}</td>
    </tr>`).join('')}</tbody>`;
  }

  function renderControl(entity) {
    const cfg = CONTROL_TABLES[entity];
    const list = filterList(mockData[entity], filterQ, cfg.columns.map(c => c.key));
    const ths = cfg.columns.map(c => `<th>${c.label}</th>`).join('') + '<th>操作</th>';
    const rows = list.map(row => {
      const tds = cfg.columns.map(c => {
        const v = row[c.key];
        if (c.render) return `<td>${c.render(v, row)}</td>`;
        if (c.badge) return `<td>${badge(v)}</td>`;
        return `<td>${v ?? '—'}</td>`;
      }).join('');
      return `<tr>${tds}<td>${actionCell(entity, row.id)}</td></tr>`;
    }).join('');
    document.getElementById('tableMain').innerHTML = `<thead><tr>${ths}</tr></thead><tbody>${rows || '<tr><td colspan="20">暂无数据</td></tr>'}</tbody>`;
  }

  const RENDERERS = {
    equipment: renderEquipment,
    materials: renderMaterials,
    rules: renderRules
  };

  function renderTable(entity) {
    if (RENDERERS[entity]) RENDERERS[entity](entity);
    else if (CONTROL_TABLES[entity]) renderControl(entity);
  }

  function openModal(entity, mode, id) {
    modalState = { entity, mode, id };
    let schema = FORM_SCHEMAS[entity];
    let row = {};
    if (CONTROL_TABLES[entity]) {
      schema = CONTROL_TABLES[entity].columns.map(c => ({ key: c.key, label: c.label }));
      if (entity === 'charter') schema.push({ key: 'summary', label: '章程摘要', type: 'textarea' });
    }
    if (mode === 'edit') {
      row = mockData[entity].find(x => String(x.id) === String(id)) || {};
    } else if (CONTROL_TABLES[entity]) {
      row = CONTROL_TABLES[entity].defaultRow();
    } else if (entity === 'equipment') {
      row = { id: 'EQ' + Date.now(), name: '', model: '', qty: 1, rate: 100, nextMaintain: '', status: '正常' };
    } else if (entity === 'materials') {
      row = { id: 'M' + Date.now(), name: '', spec: '', stock: 0, safety: 0, unit: '', lastIn: '' };
    } else if (entity === 'rules') {
      row = { id: 'R' + Date.now(), name: '', version: 'V1.0', effective: '', file: '', summary: '' };
    }
    const meta = PAGE_META[entity];
    document.getElementById('modalTitle').textContent = (mode === 'add' ? '新增' : '编辑') + ' — ' + (meta?.title || entity);
    document.getElementById('modalBody').innerHTML = (schema || []).map(f => {
      const val = row[f.key] ?? '';
      if (f.type === 'select') {
        const opts = f.options || PROJECTS;
        return `<div class="form-group"><label>${f.label}</label><select data-field="${f.key}">${opts.map(o => `<option value="${o}" ${val === o ? 'selected' : ''}>${o}</option>`).join('')}</select></div>`;
      }
      if (f.type === 'textarea') {
        return `<div class="form-group"><label>${f.label}</label><textarea data-field="${f.key}">${val}</textarea></div>`;
      }
      return `<div class="form-group"><label>${f.label}</label><input type="${f.type || 'text'}" data-field="${f.key}" value="${val}"></div>`;
    }).join('');
    document.querySelector('#modal .modal-box')?.classList.toggle('wide', false);
    document.getElementById('modal').classList.add('show');
  }

  function saveModal() {
    const { entity, mode, id } = modalState;
    const fields = {};
    document.querySelectorAll('#modalBody [data-field]').forEach(el => {
      fields[el.dataset.field] = el.type === 'number' ? parseFloat(el.value) || 0 : el.value;
    });
    const list = mockData[entity];
    if (!list) return;
    if (mode === 'add') {
      if (!fields.id && CONTROL_TABLES[entity]) fields.id = Date.now();
      list.push(fields);
    } else {
      const idx = list.findIndex(x => String(x.id) === String(id));
      if (idx !== -1) list[idx] = { ...list[idx], ...fields };
    }
    document.getElementById('modal').classList.remove('show');
    renderTable(entity);
  }

  function bindEvents(entity) {
    document.getElementById('searchMain')?.addEventListener('input', e => {
      filterQ = e.target.value;
      renderTable(entity);
    });
    document.getElementById('btnAdd')?.addEventListener('click', () => openModal(entity, 'add'));
    document.getElementById('modalClose').addEventListener('click', () => document.getElementById('modal').classList.remove('show'));
    document.getElementById('modalCancel').addEventListener('click', () => document.getElementById('modal').classList.remove('show'));
    document.getElementById('modalSave').addEventListener('click', saveModal);
    document.body.addEventListener('click', e => {
      const editBtn = e.target.closest('[data-action="edit"]');
      if (editBtn) { openModal(editBtn.dataset.entity, 'edit', editBtn.dataset.id); return; }
      const delBtn = e.target.closest('[data-action="delete"]');
      if (delBtn) {
        if (!confirm('确定删除该记录？')) return;
        const list = mockData[delBtn.dataset.entity];
        const idx = list.findIndex(x => String(x.id) === String(delBtn.dataset.id));
        if (idx !== -1) list.splice(idx, 1);
        renderTable(entity);
        return;
      }
      const ruleLink = e.target.closest('.rule-link');
      if (ruleLink && entity === 'rules') {
        e.preventDefault();
        const r = mockData.rules.find(x => x.id === ruleLink.dataset.id);
        if (r) document.getElementById('rulePreviewText').textContent = r.summary;
        return;
      }
      const stockBtn = e.target.closest('[data-stock]');
      if (stockBtn && entity === 'materials') {
        const m = mockData.materials.find(x => x.id === stockBtn.dataset.id);
        if (m) {
          m.stock = Math.max(0, m.stock + (stockBtn.dataset.stock === 'in' ? 10 : -10));
          m.lastIn = new Date().toISOString().slice(0, 10);
          renderTable(entity);
        }
      }
    });
  }

  function init(entity) {
    const meta = PAGE_META[entity];
    if (!meta) return;
    document.title = meta.title + ' - 智训企业版';
    document.body.innerHTML = renderTopbar(meta) + `
      <main class="page">
        <div class="card">
          <div class="card-head">
            <div class="title-wrap"><i class="fas ${meta.icon}"></i> ${meta.title}</div>
          </div>
          <div class="toolbar">
            <input type="text" class="search-input" id="searchMain" placeholder="搜索…">
            <div class="toolbar-actions">
              <button type="button" class="btn btn-primary btn-sm" id="btnAdd"><i class="fas fa-plus"></i> 新增</button>
            </div>
          </div>
          <div class="table-wrap"><table class="data-table" id="tableMain"></table></div>
          ${entity === 'rules' ? `<div class="rule-preview"><h4>规则摘要预览</h4><p id="rulePreviewText">点击规则名称查看摘要。</p></div>` : ''}
        </div>
      </main>` + renderModal();
    bindEvents(entity);
    renderTable(entity);
  }

  return { init };
})();
