# -*- coding: utf-8 -*-
p = r'd:\新版原型图\HTML\培训管理系统 主页面.html'
with open(p, encoding='utf-8') as f:
    c = f.read()
entry = (
    '        <a href="培训业务驾驶舱.html" class="create-task-entry" style="margin-bottom:24px">\n'
    '          <div class="entry-text">\n'
    '            <h3><i class="fas fa-tachometer-alt"></i> 培训业务数据驾驶舱</h3>\n'
    '            <p>人才、资源、平台、项目四维全景看板，ECharts 交互图表，大屏与 PC 自适应展示。</p>\n'
    '          </div>\n'
    '          <span class="entry-btn"><i class="fas fa-arrow-right"></i> 进入驾驶舱</span>\n'
    '        </a>\n\n'
)
marker = '        <!-- 核心指标 -->\n'
if '培训业务驾驶舱.html' in c and '培训业务数据驾驶舱' in c:
    print('exists')
elif marker in c:
    c = c.replace(marker, entry + marker, 1)
    with open(p, 'w', encoding='utf-8', newline='') as f:
        f.write(c)
    print('inserted')
else:
    print('no marker')
