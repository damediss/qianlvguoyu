/* 环境配置 — 独立页面 */
(function () {
  const { mockData } = window.ArchiveStore;
  let radarChart = null;
  let modalState = { entity: null, mode: 'add', id: null };

  function badge(text) {
    const m = { '可用': 'success', '占用': 'warning', '维修': 'danger' };
    return `<span class="badge badge-${m[text] || 'info'}">${text}</span>`;
  }
  function actionCell(entity, id) {
    return `<div class="action-btns">
      <button type="button" class="action-btn" data-action="edit" data-entity="${entity}" data-id="${id}"><i class="fas fa-edit"></i></button>
      <button type="button" class="action-btn del" data-action="delete" data-entity="${entity}" data-id="${id}"><i class="fas fa-trash-alt"></i></button>
    </div>`;
  }

  function renderBases() {
    document.getElementById('tableBases').innerHTML = `<thead><tr>
      <th>基地</th><th>地址</th><th>面积(㎡)</th><th>容量</th><th>利用率%</th><th>操作</th>
    </tr></thead><tbody>${mockData.bases.map(b => `<tr>
      <td>${b.name}</td><td>${b.address}</td><td>${b.area}</td><td>${b.capacity}</td>
      <td><input type="number" min="0" max="100" value="${b.utilization}" data-util="${b.id}" style="width:64px;padding:6px;border:1px solid var(--border);border-radius:8px"> %</td>
      <td>${actionCell('bases', b.id)}</td>
    </tr>`).join('')}</tbody>`;
  }
  function renderRooms() {
    document.getElementById('tableRooms').innerHTML = `<thead><tr>
      <th>场地</th><th>基地</th><th>状态</th><th>操作</th>
    </tr></thead><tbody>${mockData.rooms.map(r => `<tr>
      <td>${r.name}</td><td>${r.base}</td><td>${badge(r.status)}</td><td>${actionCell('rooms', r.id)}</td>
    </tr>`).join('')}</tbody>`;
  }
  function initRadar() {
    const el = document.getElementById('envRadar');
    if (!el || typeof echarts === 'undefined') return;
    if (!radarChart) radarChart = echarts.init(el);
    radarChart.setOption({
      tooltip: {},
      radar: { indicator: [{ name: '卫生', max: 100 }, { name: '设备', max: 100 }, { name: '网络', max: 100 }, { name: '安全', max: 100 }, { name: '舒适度', max: 100 }] },
      series: [{ type: 'radar', data: [
        { value: [88, 82, 75, 92, 85], name: '总部培训中心' },
        { value: [80, 78, 70, 90, 80], name: '华东实训基地' }
      ]}]
    });
  }

  const FORM = {
    bases: [
      { key: 'id', label: 'ID' }, { key: 'name', label: '基地名称' }, { key: 'address', label: '地址' },
      { key: 'area', label: '面积', type: 'number' }, { key: 'capacity', label: '容量', type: 'number' },
      { key: 'utilization', label: '利用率%', type: 'number' }
    ],
    rooms: [
      { key: 'id', label: 'ID' }, { key: 'name', label: '场地' }, { key: 'base', label: '基地' },
      { key: 'status', label: '状态', type: 'select', options: ['可用', '占用', '维修'] }
    ]
  };

  function openModal(entity, mode, id) {
    modalState = { entity, mode, id };
    const row = mode === 'edit' ? mockData[entity].find(x => x.id === id) || {} :
      entity === 'bases' ? { id: 'B' + Date.now(), name: '', address: '', area: 0, capacity: 0, utilization: 0 } :
      { id: 'RM' + Date.now(), name: '', base: '总部培训中心', status: '可用' };
    document.getElementById('modalTitle').textContent = (mode === 'add' ? '新增' : '编辑') + (entity === 'bases' ? '基地' : '场地');
    document.getElementById('modalBody').innerHTML = FORM[entity].map(f => {
      const val = row[f.key] ?? '';
      if (f.type === 'select') {
        return `<div class="form-group"><label>${f.label}</label><select data-field="${f.key}">${f.options.map(o => `<option ${val === o ? 'selected' : ''}>${o}</option>`).join('')}</select></div>`;
      }
      return `<div class="form-group"><label>${f.label}</label><input type="${f.type || 'text'}" data-field="${f.key}" value="${val}"></div>`;
    }).join('');
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
      const i = mockData[entity].findIndex(x => x.id === id);
      if (i !== -1) mockData[entity][i] = { ...mockData[entity][i], ...fields };
    }
    document.getElementById('modal').classList.remove('show');
    entity === 'bases' ? renderBases() : renderRooms();
  }

  document.body.innerHTML = `
    <header class="topbar">
      <div class="topbar-left">
        <a href="培训管理系统 主页面.html?panel=archives" class="back-link"><i class="fas fa-arrow-left"></i> 返回项目档案</a>
        <div><h1>环境配置</h1><span class="sub">实训基地、场地与环境满意度</span></div>
      </div>
      <span class="topbar-tag"><i class="fas fa-building"></i> 项目档案 · 环</span>
    </header>
    <main class="page">
      <div class="grid-2">
        <div class="card">
          <div class="card-head"><div class="title-wrap"><i class="fas fa-warehouse"></i> 实训基地</div>
            <button type="button" class="btn btn-primary btn-sm" data-add="bases"><i class="fas fa-plus"></i> 新增</button></div>
          <div class="table-wrap"><table class="data-table" id="tableBases"></table></div>
        </div>
        <div class="card">
          <div class="card-head"><div class="title-wrap"><i class="fas fa-door-open"></i> 教室/场地</div>
            <button type="button" class="btn btn-primary btn-sm" data-add="rooms"><i class="fas fa-plus"></i> 新增</button></div>
          <div class="table-wrap"><table class="data-table" id="tableRooms"></table></div>
        </div>
      </div>
      <div class="card">
        <div class="card-head"><div class="title-wrap"><i class="fas fa-chart-area"></i> 环境满意度</div></div>
        <div id="envRadar"></div>
      </div>
    </main>
    <div class="modal-overlay" id="modal">
      <div class="modal-box">
        <div class="modal-head"><h3 id="modalTitle">编辑</h3><button type="button" class="modal-close" id="modalClose">&times;</button></div>
        <div class="modal-body" id="modalBody"></div>
        <div class="modal-foot"><button type="button" class="btn btn-secondary" id="modalCancel">取消</button><button type="button" class="btn btn-primary" id="modalSave">保存</button></div>
      </div>
    </div>`;

  document.querySelectorAll('[data-add]').forEach(b => b.addEventListener('click', () => openModal(b.dataset.add, 'add')));
  document.getElementById('modalClose').onclick = document.getElementById('modalCancel').onclick = () => document.getElementById('modal').classList.remove('show');
  document.getElementById('modalSave').onclick = saveModal;
  document.body.addEventListener('input', e => {
    if (e.target.dataset.util) {
      const b = mockData.bases.find(x => x.id === e.target.dataset.util);
      if (b) b.utilization = Math.min(100, Math.max(0, +e.target.value || 0));
    }
  });
  document.body.addEventListener('click', e => {
    const ed = e.target.closest('[data-action="edit"]');
    if (ed) openModal(ed.dataset.entity, 'edit', ed.dataset.id);
    const del = e.target.closest('[data-action="delete"]');
    if (del && confirm('确定删除？')) {
      const list = mockData[del.dataset.entity];
      const i = list.findIndex(x => x.id === del.dataset.id);
      if (i !== -1) list.splice(i, 1);
      del.dataset.entity === 'bases' ? renderBases() : renderRooms();
    }
  });
  renderBases();
  renderRooms();
  initRadar();
  window.addEventListener('resize', () => radarChart?.resize());
})();
