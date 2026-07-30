/* Zhixun fenban + paike shared module */
(function (global) {
  const STORAGE_FENBAN = 'zhixun_fenban_by_project';
  const STORAGE_PAIKE = 'zhixun_paike_by_project';
  const SURNAMES = ['\u9648', '\u5218', '\u9ec4', '\u5468', '\u5434', '\u90d1', '\u5b59', '\u9a6c'];
  const GIVEN = ['\u660e', '\u6d0b', '\u4f1f', '\u6770', '\u82b3', '\u5f3a', '\u4e3d', '\u8d85'];
  const DEPTS = ['\u751f\u4ea7\u4e00\u90e8', '\u751f\u4ea7\u4e8c\u90e8', '\u8fd0\u7ef4\u4e2d\u5fc3', '\u540e\u52e4\u4fdd\u969c'];
  const REGIONS = ['\u5357\u5b81', '\u6842\u6797', '\u67f3\u5dde', '\u7389\u6797'];
  const TEACHERS = ['\u9648\u6559\u6388', '\u5218\u5e08\u5085', '\u5468\u5de5', '\u5434\u5f8b\u5e08', '\u5f20\u5efa\u56fd', '\u674e\u79c0\u82f1'];
  const VENUES = ['\u603b\u90e8\u57f9\u8bad\u4e2d\u5fc3', '\u534e\u4e1c\u5b9e\u8bad\u57fa\u5730', '\u534e\u5357\u6280\u80fd\u4e2d\u5fc3', '\u67f3\u5dde\u57f9\u8bad\u57fa\u5730'];
  const SLOTS = { morning: { start: '09:00', end: '11:30' }, afternoon: { start: '14:00', end: '17:00' } };
  const WEEKDAYS = ['\u5468\u4e00', '\u5468\u4e8c', '\u5468\u4e09', '\u5468\u56db', '\u5468\u4e94'];
  const TIME_ROWS = ['\u4e0a\u5348', '\u4e0b\u5348'];

  function getMonday(d) {
    const date = new Date(d);
    const day = date.getDay() || 7;
    if (day !== 1) date.setDate(date.getDate() - day + 1);
    return date.toISOString().slice(0, 10);
  }
  function getAll(key) {
    try { return JSON.parse(localStorage.getItem(key) || '{}'); } catch (e) { return {}; }
  }

  function generateStudents(project) {
    const count = Math.max(project.planCount || 40, 8);
    const baseOcc = project.occupation || '\u7535\u5de5';
    const baseReg = project.region || '\u5357\u5b81';
    const students = [];
    for (let i = 0; i < count; i++) {
      students.push({
        id: project.id * 100000 + i + 1,
        name: SURNAMES[i % SURNAMES.length] + GIVEN[Math.floor(i / SURNAMES.length) % GIVEN.length],
        occupation: i % 7 === 0 && baseOcc !== '\u710a\u5de5' ? '\u710a\u5de5' : baseOcc,
        region: i % 5 === 0 ? REGIONS[(REGIONS.indexOf(baseReg) + 1) % REGIONS.length] : baseReg,
        department: DEPTS[i % DEPTS.length],
        enterprise: project.enterprise || '\u672c\u4f01\u4e1a'
      });
    }
    return students;
  }

  function initFenbanBundle(project) {
    const students = generateStudents(project);
    const registry = {};
    students.forEach(s => { registry[s.id] = { ...s }; });
    return {
      projectId: project.id, classes: [], pendingStudents: students.map(s => ({ ...s })),
      studentRegistry: registry, rules: { capacity: 42, ruleBy: 'mixed', shift: '\u5168\u5929' },
      fenbanDraft: true, fenbanFinalized: false
    };
  }

  function createFenbanController(opts) {
    const prefix = opts.prefix || 'fb';
    const el = id => document.getElementById(prefix + id);
    let project = null, classes = [], pendingStudents = [], studentRegistry = {}, classIdCounter = 10;
    const onChange = opts.onChange || (() => {});

    function load(proj) {
      project = proj;
      if (!proj) return;
      const all = getAll(STORAGE_FENBAN);
      let bundle = all[String(proj.id)];
      if (!bundle) { bundle = initFenbanBundle(proj); all[String(proj.id)] = bundle; localStorage.setItem(STORAGE_FENBAN, JSON.stringify(all)); }
      classes = (bundle.classes || []).map(c => ({ ...c, studentIds: [...(c.studentIds || [])] }));
      pendingStudents = (bundle.pendingStudents || []).map(s => ({ ...s }));
      studentRegistry = { ...(bundle.studentRegistry || {}) };
      classIdCounter = classes.length ? Math.max(...classes.map(c => c.id), 0) + 1 : 10;
      if (el('RuleCapacity')) el('RuleCapacity').value = bundle.rules?.capacity ?? 42;
      if (el('RuleBy')) el('RuleBy').value = bundle.rules?.ruleBy ?? 'mixed';
      render();
    }

    function save() {
      if (!project) return;
      const all = getAll(STORAGE_FENBAN);
      const assigned = classes.reduce((s, c) => s + (c.studentIds?.length || 0), 0);
      all[String(project.id)] = {
        projectId: project.id, classes, pendingStudents, studentRegistry,
        rules: { capacity: +(el('RuleCapacity')?.value || 42), ruleBy: el('RuleBy')?.value || 'mixed', shift: '\u5168\u5929' },
        fenbanDone: assigned > 0 && !pendingStudents.length,
        fenbanDraft: opts.draft !== false,
        fenbanFinalized: !!opts.allowFinalize && assigned > 0
      };
      localStorage.setItem(STORAGE_FENBAN, JSON.stringify(all));
      onChange();
    }

    function groupKey(s, ruleBy) {
      if (ruleBy === 'region') return s.region;
      if (ruleBy === 'department') return s.department || '\u672a\u5206\u7ec4';
      if (ruleBy === 'mixed') return s.occupation + '_' + s.region;
      return s.occupation;
    }

    function findOrCreateClass(key, sample, capacity) {
      let existing = classes.find(c => c.groupKey === key && (c.studentIds?.length || 0) < (c.capacity || capacity));
      if (existing) return existing;
      const cls = {
        id: classIdCounter++,
        name: (project.name || '\u9879\u76ee').slice(0, 8) + ' \u00b7 ' + sample.occupation + sample.region + '\u73ed',
        occupation: sample.occupation, region: sample.region, teacher: '', capacity,
        studentIds: [], status: '\u62db\u751f\u4e2d', groupKey: key
      };
      classes.push(cls);
      return cls;
    }

    function smartAssign() {
      if (!project) return alert('\u8bf7\u5148\u9009\u62e9\u9879\u76ee');
      if (opts.allowFinalize === false && getAll(STORAGE_FENBAN)[String(project.id)]?.fenbanFinalized) {
        return alert('\u5206\u73ed\u5df2\u5b9a\u7a3f');
      }
      const capacity = +(el('RuleCapacity')?.value || 42);
      const ruleBy = el('RuleBy')?.value || 'mixed';
      if (!pendingStudents.length) return alert('\u5f85\u5206\u73ed\u5b66\u5458\u6c60\u4e3a\u7a7a');
      [...pendingStudents].forEach(s => {
        const key = groupKey(s, ruleBy);
        findOrCreateClass(key, s, capacity).studentIds.push(s.id);
      });
      pendingStudents = [];
      save(); render();
      alert('\u667a\u80fd\u5206\u73ed\u5b8c\u6210\uff1a' + classes.length + ' \u4e2a\u73ed\u7ea7');
    }

    function render() {
      const assigned = classes.reduce((s, c) => s + (c.studentIds?.length || 0), 0);
      const total = pendingStudents.length + assigned;
      const rate = total ? Math.round((assigned / total) * 100) : 0;
      if (el('StatPending')) el('StatPending').textContent = pendingStudents.length;
      if (el('StatClasses')) el('StatClasses').textContent = classes.length;
      if (el('StatAssigned')) el('StatAssigned').textContent = assigned;
      if (el('StatRate')) el('StatRate').textContent = rate + '%';
      const empty = '<tr><td colspan="4" style="text-align:center;padding:20px;color:#64748B">\u6682\u65e0\u6570\u636e</td></tr>';
      if (el('PendingBody')) {
        el('PendingBody').innerHTML = pendingStudents.length
          ? pendingStudents.map(s => '<tr><td>' + s.name + '</td><td>' + s.occupation + '</td><td>' + s.region + '</td><td>' + (s.department || '\u2014') + '</td></tr>').join('')
          : empty.replace('colspan="4"', 'colspan="4"').replace('\u6682\u65e0\u6570\u636e', '\u6682\u65e0\u5f85\u5206\u73ed\u5b66\u5458');
      }
      if (el('ClassBody')) {
        el('ClassBody').innerHTML = classes.length
          ? classes.map(c => {
              const cnt = c.studentIds?.length || 0, cap = c.capacity || 42;
              return '<tr><td><strong>' + c.name + '</strong></td><td>' + c.occupation + '</td><td>' + c.region +
                '</td><td>' + (c.teacher || '\u2014') + '</td><td>' + cnt + '</td><td>' + cap +
                '</td><td><span class="status-badge st-draft">' + (cnt >= cap ? '\u5df2\u6ee1\u5458' : '\u62db\u751f\u4e2d') +
                '</span></td><td><button type="button" class="btn-link btn-del-cls" data-id="' + c.id + '">\u5220\u9664</button></td></tr>';
            }).join('')
          : '<tr><td colspan="8" style="text-align:center;padding:20px;color:#64748B">\u6682\u65e0\u73ed\u7ea7</td></tr>';
      }
      if (el('PreviewBars') && classes.length) {
        const max = Math.max(...classes.map(c => c.studentIds?.length || 0), 1);
        el('PreviewBars').innerHTML = classes.map(c => {
          const cnt = c.studentIds?.length || 0, pct = Math.round((cnt / max) * 100);
          return '<div style="display:flex;gap:10px;margin-bottom:8px;font-size:13px;align-items:center"><span style="min-width:120px">' + c.name +
            '</span><div style="flex:1;height:8px;background:#F1F5F9;border-radius:4px"><div style="width:' + pct + '%;height:100%;background:#2A6DFF;border-radius:4px"></div></div><span>' + cnt + '\u4eba</span></div>';
        }).join('');
      }
    }

    function confirmFinal() {
      if (!classes.length) return alert('\u8bf7\u5148\u5b8c\u6210\u5206\u73ed');
      save();
      const all = getAll(STORAGE_FENBAN);
      all[String(project.id)].fenbanDraft = false;
      all[String(project.id)].fenbanFinalized = true;
      localStorage.setItem(STORAGE_FENBAN, JSON.stringify(all));
      alert('\u5206\u73ed\u5df2\u5b9a\u7a3f');
      onChange();
    }

    function bind() {
      el('BtnSmartAssign')?.addEventListener('click', smartAssign);
      el('BtnNewClass')?.addEventListener('click', () => {
        if (!project) return;
        classes.push({
          id: classIdCounter++, name: (project.occupation || '\u57f9\u8bad') + '\u73ed',
          occupation: project.occupation || '\u7535\u5de5', region: project.region || '\u5357\u5b81',
          teacher: '', capacity: +(el('RuleCapacity')?.value || 42), studentIds: [], status: '\u62db\u751f\u4e2d'
        });
        save(); render();
      });
      el('ClassBody')?.addEventListener('click', e => {
        const btn = e.target.closest('.btn-del-cls');
        if (!btn || !confirm('\u5220\u9664\u73ed\u7ea7\uff1f')) return;
        const c = classes.find(x => String(x.id) === btn.dataset.id);
        if (c?.studentIds) c.studentIds.forEach(sid => { if (studentRegistry[sid]) pendingStudents.push({ ...studentRegistry[sid] }); });
        classes = classes.filter(x => String(x.id) !== btn.dataset.id);
        save(); render();
      });
      el('BtnConfirmFenban')?.addEventListener('click', confirmFinal);
    }

    return { load, save, render, getClasses: () => classes, bind };
  }

  function createPaikeController(opts) {
    const prefix = opts.prefix || 'pk';
    const el = id => document.getElementById(prefix + id);
    let project = null, projectClasses = [], courses = [], published = false, conflictIds = new Set(), courseIdCounter = 1;
    const onChange = opts.onChange || (() => {});

    function loadClasses() {
      projectClasses = (getAll(STORAGE_FENBAN)[String(project.id)]?.classes || []).filter(c => c.name);
    }

    function load(proj) {
      project = proj;
      if (!proj) return;
      loadClasses();
      const all = getAll(STORAGE_PAIKE);
      let bundle = all[String(proj.id)];
      if (!bundle) {
        bundle = { courses: [], published: false, weekStart: getMonday(new Date()), draft: true };
        all[String(proj.id)] = bundle;
        localStorage.setItem(STORAGE_PAIKE, JSON.stringify(all));
      }
      courses = (bundle.courses || []).map(c => ({ ...c }));
      published = bundle.published || false;
      courseIdCounter = courses.length ? Math.max(...courses.map(c => c.id)) + 1 : 1;
      if (el('WeekStart')) el('WeekStart').value = bundle.weekStart || getMonday(new Date());
      if (el('NoClassAlert')) el('NoClassAlert').style.display = projectClasses.length ? 'none' : 'flex';
      if (el('FilterClass')) {
        el('FilterClass').innerHTML = '<option value="">\u5168\u90e8\u73ed\u7ea7</option>' +
          projectClasses.map(c => '<option value="' + c.id + '">' + c.name + '</option>').join('');
      }
      render();
    }

    function save() {
      if (!project) return;
      const all = getAll(STORAGE_PAIKE);
      all[String(project.id)] = {
        projectId: project.id, courses, published: opts.isFinal ? published : false,
        weekStart: el('WeekStart')?.value || getMonday(new Date()),
        draft: !opts.isFinal, scheduleFinalized: opts.isFinal && published
      };
      localStorage.setItem(STORAGE_PAIKE, JSON.stringify(all));
      onChange();
    }

    function timeToMin(t) { const p = t.split(':'); return +p[0] * 60 + +p[1]; }
    function filtered() {
      const cid = el('FilterClass')?.value;
      return cid ? courses.filter(c => String(c.classId) === String(cid)) : courses;
    }

    function detectConflicts() {
      conflictIds = new Set();
      const list = [];
      for (let i = 0; i < courses.length; i++) {
        for (let j = i + 1; j < courses.length; j++) {
          const a = courses[i], b = courses[j];
          if (a.date !== b.date) continue;
          if (!(timeToMin(a.startTime) < timeToMin(b.endTime) && timeToMin(b.startTime) < timeToMin(a.endTime))) continue;
          let reason = '';
          if (a.teacher === b.teacher) reason = '\u8bb2\u5e08\u51b2\u7a81';
          else if (a.venue === b.venue) reason = '\u573a\u5730\u51b2\u7a81';
          else if (a.classId === b.classId) reason = '\u73ed\u7ea7\u65f6\u95f4\u91cd\u53e0';
          else continue;
          conflictIds.add(a.id); conflictIds.add(b.id);
          list.push(a.date + ' ' + a.courseName + ' / ' + b.courseName + '\uff1a' + reason);
        }
      }
      return list;
    }

    function getWeekDates() {
      const base = new Date((el('WeekStart')?.value || getMonday(new Date())) + 'T00:00:00');
      return Array.from({ length: 5 }, (_, i) => {
        const d = new Date(base); d.setDate(base.getDate() + i); return d.toISOString().slice(0, 10);
      });
    }

    function render() {
      const list = filtered(), conflicts = detectConflicts();
      if (el('StatClasses')) el('StatClasses').textContent = projectClasses.length;
      if (el('StatCourses')) el('StatCourses').textContent = list.length;
      if (el('StatConflicts')) el('StatConflicts').textContent = conflicts.length;
      if (el('CourseBody')) {
        el('CourseBody').innerHTML = list.length
          ? list.map(c => '<tr style="' + (conflictIds.has(c.id) ? 'background:#FEF2F2' : '') + '"><td>' + c.className + '</td><td><strong>' +
            c.courseName + '</strong></td><td>' + c.courseType + '</td><td>' + c.date + '</td><td>' + c.startTime + '-' + c.endTime +
            '</td><td>' + c.teacher + '</td><td>' + c.venue + '</td><td>' + (c.status || '\u8349\u7a3f') +
            '</td><td><button type="button" class="btn-link btn-del-course" data-id="' + c.id + '">\u5220\u9664</button></td></tr>').join('')
          : '<tr><td colspan="9" style="text-align:center;padding:20px;color:#64748B">\u6682\u65e0\u8bfe\u7a0b</td></tr>';
      }
      if (el('ConflictPanel')) {
        el('ConflictPanel').innerHTML = conflicts.length
          ? '<ul style="list-style:none;font-size:13px">' + conflicts.map(x => '<li style="padding:8px;margin-bottom:6px;background:#FEF2F2;border-radius:6px;color:#991B1B">' + x + '</li>').join('') + '</ul>'
          : '<p style="color:#059669;font-size:14px"><i class="fas fa-check-circle"></i> \u672a\u68c0\u6d4b\u5230\u51b2\u7a81</p>';
      }
      const grid = el('WeekGrid');
      if (grid) {
        const dates = getWeekDates();
        let html = '<div class="wg-head"></div>';
        dates.forEach((d, i) => { html += '<div class="wg-head">' + WEEKDAYS[i] + '<br><small>' + d.slice(5) + '</small></div>'; });
        TIME_ROWS.forEach(row => {
          html += '<div class="wg-slot">' + row + '</div>';
          dates.forEach(date => {
            const slotCourses = list.filter(c => c.date === date && (row === '\u4e0a\u5348' ? timeToMin(c.startTime) < 720 : timeToMin(c.startTime) >= 720));
            html += '<div class="wg-cell">';
            slotCourses.forEach(c => {
              const cls = c.courseType === '\u5b9e\u64cd' ? 'practice' : c.courseType === '\u8003\u6838' ? 'exam' : 'theory';
              html += '<div class="week-course ' + cls + (conflictIds.has(c.id) ? ' conflict' : '') + '"><strong>' + c.courseName + '</strong><br>' + c.className + '</div>';
            });
            html += '</div>';
          });
        });
        grid.innerHTML = html;
      }
    }

    function smartSchedule() {
      if (!projectClasses.length) return alert('\u8bf7\u5148\u5b8c\u6210\u5206\u73ed');
      const dates = getWeekDates(), occ = project.occupation || '\u901a\u7528';
      const tpls = [{ name: occ + '\u5b89\u5168\u89c4\u8303', type: '\u7406\u8bba' }, { name: occ + '\u5b9e\u64cd', type: '\u5b9e\u64cd' }, { name: '\u9636\u6bb5\u8003\u6838', type: '\u8003\u6838' }];
      projectClasses.forEach((cls, ci) => {
        tpls.forEach((tpl, ti) => {
          const s = tpl.type !== '\u7406\u8bba' ? SLOTS.afternoon : SLOTS.morning;
          courses.push({
            id: courseIdCounter++, classId: cls.id, className: cls.name, courseName: tpl.name, courseType: tpl.type,
            date: dates[(ci + ti) % dates.length], startTime: s.start, endTime: s.end,
            teacher: TEACHERS[(ci + ti) % TEACHERS.length], venue: VENUES[ci % VENUES.length], status: '\u8349\u7a3f'
          });
        });
      });
      save(); render();
      alert('\u5df2\u4e3a ' + projectClasses.length + ' \u4e2a\u73ed\u7ea7\u751f\u6210\u6392\u8bfe');
    }

    function publish() {
      const conflicts = detectConflicts();
      if (conflicts.length) return alert('\u5b58\u5728 ' + conflicts.length + ' \u5904\u51b2\u7a81');
      if (!courses.length) return alert('\u6682\u65e0\u8bfe\u7a0b');
      published = true;
      courses.forEach(c => { c.status = '\u5df2\u53d1\u5e03'; });
      save(); render();
      alert(opts.isFinal ? '\u6700\u7ec8\u8bfe\u8868\u5df2\u53d1\u5e03' : '\u6392\u8bfe\u8349\u6848\u5df2\u4fdd\u5b58');
    }

    function bind() {
      el('BtnSmartSchedule')?.addEventListener('click', smartSchedule);
      el('BtnDetectConflict')?.addEventListener('click', () => { detectConflicts(); render(); });
      el('BtnPublish')?.addEventListener('click', publish);
      el('WeekStart')?.addEventListener('change', () => { save(); render(); });
      el('FilterClass')?.addEventListener('change', render);
      el('CourseBody')?.addEventListener('click', e => {
        const btn = e.target.closest('.btn-del-course');
        if (!btn) return;
        courses = courses.filter(c => String(c.id) !== btn.dataset.id);
        save(); render();
      });
    }

    return { load, save, render, publish, bind, hasClasses: () => projectClasses.length > 0 };
  }

  global.ZhixunFenbanPaike = {
    createFenbanController, createPaikeController,
    getFenbanBundle: pid => getAll(STORAGE_FENBAN)[String(pid)],
    getPaikeBundle: pid => getAll(STORAGE_PAIKE)[String(pid)]
  };
})(window);
