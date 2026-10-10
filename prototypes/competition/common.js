(function () {
  function getCurrentFile() {
    const p = window.location.pathname.replace(/\\/g, '/');
    const last = p.split('/').pop();
    return last || '';
  }

  function safeOnCapture(el, event, handler) {
    if (!el) return;
    el.addEventListener(event, handler, true);
  }

  function toggleExpanded(menuEl, listEl) {
    if (!menuEl || !listEl) return;
    menuEl.classList.toggle('expanded');
    listEl.classList.toggle('expanded');
  }

  /** 认定流程子菜单内二级折叠（认定筹备 / 认定中 / 认定后） */
  window.__toggleNest = function (ev, nestId) {
    if (!ev) return;
    ev.preventDefault();
    ev.stopPropagation();
    if (typeof ev.stopImmediatePropagation === 'function') ev.stopImmediatePropagation();
    const head = ev.currentTarget;
    const nest = document.getElementById(nestId);
    if (!nest || !head) return;
    head.classList.toggle('expanded');
    nest.classList.toggle('expanded');
    const arrow = head.querySelector('.sidebar-arrow');
    if (arrow) {
      arrow.style.transform = head.classList.contains('expanded') ? 'rotate(90deg)' : 'rotate(0deg)';
    }
  };

  function processSubmenuHtml() {
    return (
      '<div class="sidebar-subitem" onclick="window.location.href=\'任务选择.html\'">' +
      '<span>📋</span> 任务选择</div>' +
      '<div class="sidebar-subitem" id="prepNestHead" onclick="window.__toggleNest(event, \'prepNest\')">' +
      '<span>🔧</span> 认定筹备 <span class="sidebar-arrow" style="margin-left:auto">▶</span></div>' +
      '<div class="sidebar-sublist" id="prepNest">' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'认定筹备.html\'" style="padding-left:56px">' +
      '<span>👥</span> 学员资料</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'考评员配置.html\'" style="padding-left:56px">' +
      '<span>👷</span> 考评员配置</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'认定筹备-考场资料.html\'" style="padding-left:56px">' +
      '<span>🏫</span> 考场配置</div></div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'认定前.html\'">' +
      '<span>📅</span> 认定前</div>' +
      '<div class="sidebar-subitem" id="midNestHead" onclick="window.__toggleNest(event, \'midNest\')">' +
      '<span>⚙️</span> 认定中 <span class="sidebar-arrow" style="margin-left:auto">▶</span></div>' +
      '<div class="sidebar-sublist" id="midNest">' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'理论笔试.html\'" style="padding-left:56px">' +
      '<span>📝</span> 理论笔试</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'实操考试.html\'" style="padding-left:56px">' +
      '<span>🔨</span> 实操考试</div></div>' +
      '<div class="sidebar-subitem" id="afterNestHead" onclick="window.__toggleNest(event, \'afterNest\')">' +
      '<span>📮</span> 认定后 <span class="sidebar-arrow" style="margin-left:auto">▶</span></div>' +
      '<div class="sidebar-sublist" id="afterNest">' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'认定归档.html\'" style="padding-left:56px">' +
      '<span>📦</span> 认定归档</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'申领补贴.html\'" style="padding-left:56px">' +
      '<span>💰</span> 申领补贴</div></div>'
    );
  }

  function ensureMaterialLibraryMenu(sidebarEl) {
    if (!sidebarEl || document.getElementById('materialMenu')) return;
    const processSubmenu = document.getElementById('processSubmenu');
    if (!processSubmenu) return;

    const materialMenu = document.createElement('div');
    materialMenu.className = 'sidebar-item';
    materialMenu.id = 'materialMenu';
    materialMenu.innerHTML =
      '<div class="sidebar-item-content">' +
      '<span class="sidebar-icon">🧱</span>' +
      '<span>物料库管理</span></div>' +
      '<span class="sidebar-arrow">▶</span>';

    const materialSubmenu = document.createElement('div');
    materialSubmenu.className = 'sidebar-sublist';
    materialSubmenu.id = 'materialSubmenu';
    materialSubmenu.innerHTML =
      '<div class="sidebar-subitem" onclick="window.location.href=\'考评员信息管理.html\'">' +
      '<span>👷</span> 考评员信息</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'考场信息管理.html\'">' +
      '<span>🏛️</span> 考场信息</div>' +
      '<div class="sidebar-subitem" onclick="window.location.href=\'考试设备管理.html\'">' +
      '<span>🖥️</span> 考试设备</div>';

    processSubmenu.insertAdjacentElement('afterend', materialMenu);
    materialMenu.insertAdjacentElement('afterend', materialSubmenu);
  }

  /**
   * 统一侧边栏：任务管理命名、任务档案入口、认定流程 IA、物料库、移除考前强化注入块。
   */
  function normalizeSidebar(sidebarEl) {
    if (!sidebarEl || sidebarEl.dataset.cmNormalized === '1') return;
    sidebarEl.dataset.cmNormalized = '1';

    document.getElementById('boostMenu')?.remove();
    document.getElementById('boostSubmenu')?.remove();

    const projectMenu = document.getElementById('projectMenu');
    if (projectMenu) {
      const spans = projectMenu.querySelectorAll('.sidebar-item-content span');
      spans.forEach((s) => {
        if (s.textContent.trim() === '项目管理') s.textContent = '任务管理';
      });
    }

    const projectSubmenu = document.getElementById('projectSubmenu');
    if (projectSubmenu) {
      projectSubmenu.querySelectorAll('.sidebar-subitem').forEach((el) => {
        const t = el.textContent || '';
        if (t.includes('项目准备')) {
          el.innerHTML = el.innerHTML.replace(/项目准备/g, '任务准备');
        }
      });
    }

    const archiveMenu = document.getElementById('archiveMenu');
    const archiveSubmenu = document.getElementById('archiveSubmenu');
    if (archiveMenu && archiveSubmenu && archiveMenu.parentElement) {
      const parent = archiveMenu.parentElement;
      const link = document.createElement('div');
      link.className = 'sidebar-subitem';
      link.id = 'taskArchiveEntry';
      link.setAttribute('onclick', "window.location.href='任务档案.html'");
      link.innerHTML = '<span>📦</span> 任务档案';
      parent.insertBefore(link, archiveMenu);
      archiveMenu.remove();
      archiveSubmenu.remove();
    }

    const processSubmenu = document.getElementById('processSubmenu');
    if (processSubmenu && processSubmenu.dataset.cmProcessReplaced !== '1') {
      processSubmenu.dataset.cmProcessReplaced = '1';
      processSubmenu.innerHTML = processSubmenuHtml();
    }

    ensureMaterialLibraryMenu(sidebarEl);
  }

  function initSidebarToggles() {
    const projectMenu = document.getElementById('projectMenu');
    const projectSubmenu = document.getElementById('projectSubmenu');
    const processMenu = document.getElementById('processMenu');
    const processSubmenu = document.getElementById('processSubmenu');
    const archiveMenu = document.getElementById('archiveMenu');
    const archiveSubmenu = document.getElementById('archiveSubmenu');
    const materialMenu = document.getElementById('materialMenu');
    const materialSubmenu = document.getElementById('materialSubmenu');

    safeOnCapture(projectMenu, 'click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      toggleExpanded(projectMenu, projectSubmenu);
    });
    safeOnCapture(processMenu, 'click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      toggleExpanded(processMenu, processSubmenu);
    });

    safeOnCapture(materialMenu, 'click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      toggleExpanded(materialMenu, materialSubmenu);
    });

    safeOnCapture(archiveMenu, 'click', (e) => {
      if (typeof window.toggleArchiveSubmenu === 'function') return;
      e.preventDefault();
      e.stopPropagation();
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
      toggleExpanded(archiveMenu, archiveSubmenu);
      const arrow = archiveMenu.querySelector('.sidebar-arrow');
      if (arrow) {
        arrow.style.transform = archiveMenu.classList.contains('expanded') ? 'rotate(90deg)' : 'rotate(0deg)';
      }
    });
  }

  function initSidebarNavigation(sidebarEl) {
    if (!sidebarEl) return;

    // Use CAPTURE to win against per-page inline onclick handlers.
    sidebarEl.addEventListener('click', (e) => {
      const target = e.target;
      if (!(target instanceof Element)) return;

      const clickable = target.closest('.sidebar-item, .sidebar-subitem');
      if (!clickable) return;

      const id = clickable.getAttribute('id') || '';
      // ----- Stable toggle handling -----
      if (id === 'projectMenu' || id === 'processMenu' || id === 'materialMenu' || id === 'archiveMenu' ||
          id === 'prepNestHead' || id === 'midNestHead' || id === 'afterNestHead') {
        e.preventDefault();
        e.stopPropagation();
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();

        if (id === 'projectMenu') {
          toggleExpanded(document.getElementById('projectMenu'), document.getElementById('projectSubmenu'));
          return;
        }
        if (id === 'processMenu') {
          toggleExpanded(document.getElementById('processMenu'), document.getElementById('processSubmenu'));
          return;
        }
        if (id === 'materialMenu') {
          toggleExpanded(document.getElementById('materialMenu'), document.getElementById('materialSubmenu'));
          return;
        }
        if (id === 'archiveMenu') {
          const am = document.getElementById('archiveMenu');
          const as = document.getElementById('archiveSubmenu');
          toggleExpanded(am, as);
          const arrow = am && am.querySelector('.sidebar-arrow');
          if (arrow) arrow.style.transform = am.classList.contains('expanded') ? 'rotate(90deg)' : 'rotate(0deg)';
          return;
        }
        if (id === 'prepNestHead') {
          window.__toggleNest(e, 'prepNest');
          return;
        }
        if (id === 'midNestHead') {
          window.__toggleNest(e, 'midNest');
          return;
        }
        if (id === 'afterNestHead') {
          window.__toggleNest(e, 'afterNest');
          return;
        }
      }

      const onclick = clickable.getAttribute('onclick') || '';
      if (onclick.includes('__toggleNest')) {
        // Fallback: if some page keeps inline nest toggles, still block navigation.
        e.preventDefault();
        e.stopPropagation();
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();
        return;
      }

      const dataHref = clickable.getAttribute('data-href');
      if (dataHref) {
        window.location.href = dataHref;
        return;
      }

      const hrefMatch =
        onclick.match(/window\.location\.href\s*=\s*'([^']+)'/) ||
        onclick.match(/window\.location\.href\s*=\s*"([^"]+)"/) ||
        onclick.match(/location\.href\s*=\s*'([^']+)'/) ||
        onclick.match(/location\.href\s*=\s*"([^"]+)"/);
      if (hrefMatch && hrefMatch[1]) {
        window.location.href = hrefMatch[1];
        return;
      }
    }, true);
  }

  function setActiveByUrl() {
    const cur = getCurrentFile();
    if (!cur) return;

    const subitems = Array.from(document.querySelectorAll('.sidebar-subitem'));
    subitems.forEach((el) => {
      const onclick = el.getAttribute('onclick') || '';
      if (onclick.includes('__toggleNest')) return;
      const hrefMatch = onclick.match(/window\.location\.href\s*=\s*'([^']+)'/);
      if (hrefMatch && hrefMatch[1] === cur) {
        el.classList.add('active');
        const parentList = el.closest('.sidebar-sublist');
        if (parentList && !parentList.classList.contains('expanded')) parentList.classList.add('expanded');
        const prev = parentList ? parentList.previousElementSibling : null;
        if (prev && prev.classList.contains('sidebar-item')) prev.classList.add('expanded');
        if (parentList && parentList.id === 'prepNest') {
          const h = document.getElementById('prepNestHead');
          if (h) {
            h.classList.add('expanded');
            const ar = h.querySelector('.sidebar-arrow');
            if (ar) ar.style.transform = 'rotate(90deg)';
          }
        }
        if (parentList && parentList.id === 'midNest') {
          const h = document.getElementById('midNestHead');
          if (h) {
            h.classList.add('expanded');
            const ar = h.querySelector('.sidebar-arrow');
            if (ar) ar.style.transform = 'rotate(90deg)';
          }
        }
        if (parentList && parentList.id === 'afterNest') {
          const h = document.getElementById('afterNestHead');
          if (h) {
            h.classList.add('expanded');
            const ar = h.querySelector('.sidebar-arrow');
            if (ar) ar.style.transform = 'rotate(90deg)';
          }
        }
      }
    });

    const items = Array.from(document.querySelectorAll('.sidebar-item'));
    items.forEach((el) => {
      const onclick = el.getAttribute('onclick') || '';
      const hrefMatch = onclick.match(/window\.location\.href\s*=\s*'([^']+)'/);
      if (hrefMatch && hrefMatch[1] === cur) {
        el.classList.add('active');
      }
    });
  }

  function ensureCommonLink() {
    const has = Array.from(document.querySelectorAll('link[rel="stylesheet"]')).some((l) =>
      (l.getAttribute('href') || '').endsWith('common.css')
    );
    if (has) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'common.css';
    document.head.appendChild(link);
  }

  document.addEventListener('DOMContentLoaded', () => {
    ensureCommonLink();
    const sidebar = document.querySelector('.sidebar');
    normalizeSidebar(sidebar);
    initSidebarToggles();
    initSidebarNavigation(sidebar);
    setActiveByUrl();
  });
})();
