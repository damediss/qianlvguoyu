# -*- coding: utf-8 -*-
"""重建焊工系17门+电工5门知识树并配置type=3权重 — SQL 生成器。

输出：
  V20260819_002__rebuild_trees_welder_electrician.sql  （软删垃圾树 + 建新树）
  V20260819_003__type3_welder_electrician.sql          （type=3 权重）

树结构规范（对齐健康树 course 183）：
  L0: 基本要求(A,type=0) / 相关知识(B,type=0) / 技能要求(A,type=1)
  L1: 基本要求下=职业道德(A-A)/基础知识(A-B)；相关知识下=职业功能项(B-A...)；技能镜像
  L2: 职业守则/知识分类/工作内容
  L3: 鉴定点（A-A-B-001 编号，per-parent 重启）
幂等：每课以 "非deleted L0 相关知识根存在" 作为已重建标记；权重沿用 NOT EXISTS 模式。
"""

import json
import re

BASE = 'D:/workspace/qianlvguoyu_saas/docs/osta-standards/'
TREE_SQL = BASE + 'V20260819_002__rebuild_trees_welder_electrician.sql'
RATE_SQL = BASE + 'V20260819_003__type3_welder_electrician.sql'

USER_ID = 3742
TENANT = 10159
LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'


def letter(i):
    return LETTERS[i]


# ---------------- §2 基本要求解析（从 markdown） ----------------

def parse_basic(md_path):
    """返回 {'职业道德': [(sec_title, [items])], '基础知识': [(sec_title, [items])]}"""
    with open(md_path, encoding='utf-8') as f:
        text = f.read()
    m = re.search(r'# 2\.\s*基本要求(.*?)# 3\.\s*工作要求', text, re.S)
    body = m.group(1)
    # 按 ### 2. x. y 切
    sections = re.split(r'###\s*2\.\s*(\d+)\s*\.\s*(\d+)\s*(.+)', body)
    # sections: [prefix, '1','1','职业道德基本知识', rest, '1','2','职业守则', rest, ...]
    out = {'1': [], '2': []}  # 1=职业道德 2=基础知识
    i = 1
    while i < len(sections) - 3:
        top, sub, title, rest = sections[i], sections[i + 1], sections[i + 2], sections[i + 3]
        title = re.sub(r'\s+', '', title)
        items = re.findall(r'（\d+）\s*([^（\n]+?。)', rest)
        items = [re.sub(r'\s+', '', it) for it in items]
        out[top].append((title, items))
        i += 4
    return {'职业道德': out['1'], '基础知识': out['2']}


# ---------------- 课程配置 ----------------

# funcs: (occupation, level, [item numbers], {pos_in_B: rate}, comments)
# rate 映射按 B 树下 L1 的位置顺序给（含职业道德/基础知识单独给）
COURSES = [
    # --- 电焊工 ---
    dict(cid=268, occ='焊工', level='五级', items=list(range(1, 10)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 25, '低碳钢或低合金钢板角接或T形接头平焊焊条电弧焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(2, 10), [6, 6, 6, 6, 6, 6, 7, 7])]),
    dict(cid=269, occ='焊工', level='四级', items=list(range(1, 9)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 25, '管板插入式或骑座式全焊透角接头焊条电弧焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(2, 9), [8, 7, 7, 7, 7, 7, 7])]),
    dict(cid=270, occ='焊工', level='三级', items=list(range(1, 10)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 25, '低碳钢或低合金钢板对接仰焊焊条电弧焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(2, 10), [6, 6, 6, 6, 6, 6, 7, 7])]),
    dict(cid=271, occ='焊工', level='二级', items=[1, 2, 3, 10, 11],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 9, '焊条电弧焊（任选1项之一）'), (2, 8, '熔化极气体保护焊（任选1项之一）'),
                (3, 8, '非熔化极气体保护焊（任选1项之一）'),
                (10, 25, '焊接技术管理（必选）'), (11, 25, '培训与指导（必选）')]),
    dict(cid=272, occ='焊工', level='一级', items=[1, 2, 3, 4, 7, 8],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 7, '焊条电弧焊（任选1项之一）'), (2, 6, '熔化极气体保护焊（任选1项之一）'),
                (3, 6, '非熔化极气体保护焊（任选1项之一）'), (4, 6, '可达性差的结构焊接（任选1项之一）'),
                (7, 25, '焊接技术管理（必选）'), (8, 25, '培训与指导（必选）')]),
    # --- 气焊工 ---
    dict(cid=273, occ='焊工', level='五级', items=[10, 11, 12],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, 25, '全考项') for i in (10, 11, 12)]),
    dict(cid=274, occ='焊工', level='四级', items=[9, 10, 11],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, 25, '全考项') for i in (9, 10, 11)]),
    dict(cid=275, occ='焊工', level='三级', items=[10, 11, 12],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, 25, '全考项') for i in (10, 11, 12)]),
    # --- 钎焊工 ---
    dict(cid=278, occ='焊工', level='五级', items=list(range(13, 22)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (13, 25, '低碳钢板对接/搭接火焰钎焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(14, 22), [6, 6, 6, 6, 6, 6, 7, 7])]),
    dict(cid=279, occ='焊工', level='四级', items=list(range(12, 21)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (12, 25, '铝及铝合金管火焰钎焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(13, 21), [6, 6, 6, 6, 6, 6, 7, 7])]),
    dict(cid=280, occ='焊工', level='三级', items=list(range(13, 18)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (13, 25, '不等壁厚工件火焰钎焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(14, 18), [13, 13, 12, 12])]),
    dict(cid=281, occ='焊工', level='二级', items=[4, 10, 11],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (4, 25, '钎焊（必选）'), (10, 25, '焊接技术管理（必选）'), (11, 25, '培训与指导（必选）')]),
    # --- 焊接设备操作工 ---
    dict(cid=283, occ='焊工', level='五级', items=list(range(1, 10)) + [22, 23, 24],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, r, '职业功能1-9任选2项之一') for i, r in zip(range(1, 10), [6, 6, 6, 6, 6, 5, 5, 5, 5])] +
               [(i, r, '职业功能22-24任选1项之一') for i, r in zip([22, 23, 24], [9, 8, 8])]),
    dict(cid=284, occ='焊工', level='四级', items=list(range(21, 30)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (21, 25, '自动熔化极气体保护焊（必选）')] +
               [(i, r, '任选2项之一') for i, r in zip(range(22, 30), [6, 6, 6, 6, 6, 6, 7, 7])]),
    dict(cid=285, occ='焊工', level='三级', items=list(range(18, 24)),
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, r, '自动焊或机器人焊接考核3项') for i, r in zip(range(18, 24), [13, 13, 13, 12, 12, 12])]),
    dict(cid=286, occ='焊工', level='二级', items=[5, 6, 7, 8, 9, 10, 11],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识')] +
               [(i, 5, '任选1项之一') for i in range(5, 10)] +
               [(10, 25, '焊接技术管理（必选）'), (11, 25, '培训与指导（必选）')]),
    dict(cid=287, occ='焊工', level='一级', items=[5, 6, 7, 8],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (5, 13, '机器人焊接工艺优化（任选1项之一）'), (6, 12, '机器人焊接（任选1项之一）'),
                (7, 25, '焊接技术管理（必选）'), (8, 25, '培训与指导（必选）')]),
    # --- 电工（固定权重） ---
    dict(cid=288, occ='电工', level='五级', items=[1, 2, 3],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 20, '基础知识'),
                (1, 25, '电器安装和线路敷设'), (2, 30, '继电控制电路装调维修'), (3, 20, '基本电子电路装调维修')]),
    dict(cid=289, occ='电工', level='四级', items=[1, 2, 3, 4],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 15, '基础知识'),
                (1, 25, '继电控制电路装调维修'), (2, 20, '电气设备（装置）装调维修'),
                (3, 25, '自动控制电路装调维修'), (4, 10, '基本电子电路装调维修')]),
    dict(cid=290, occ='电工', level='三级', items=[1, 2, 3, 4, 5],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 10, '基础知识'),
                (1, 10, '继电控制电路装调维修'), (2, 25, '电气设备（装置）装调维修'),
                (3, 10, '自动控制电路装调维修'), (4, 15, '应用电子电路调试维修'),
                (5, 25, '交直流传动系统装调维修')]),
    dict(cid=291, occ='电工', level='二级', items=[1, 2, 3, 4, 5],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 5, '基础知识'),
                (1, 25, '电气设备（装置）装调维修'), (2, 10, '自动控制电路装调维修'),
                (3, 15, '应用电子电路调试维修'), (4, 30, '交直流传动及伺服系统调试维修'),
                (5, 10, '培训与技术管理')]),
    dict(cid=292, occ='电工', level='一级', items=[1, 2, 3],
         rates=[('职业道德', 5, '职业道德'), ('基础知识', 5, '基础知识'),
                (1, 35, '电气设备（装置）装调维修'), (2, 45, '电气自动控制系统调试维修'),
                (3, 10, '培训与技术管理')]),
]

LEVEL_CN = {'五级': '五级/初级工', '四级': '四级/中级工', '三级': '三级/高级工', '二级': '二级/技师', '一级': '一级/高级技师'}


def esc(s):
    return s.replace('\\', '\\\\').replace("'", "\\'")


def normalize_title(s):
    s = re.sub(r'\s+(?=[）》)】])', '', s)
    s = re.sub(r'(?<=[（(《【])\s+', '', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s


def build_course_tree(cid, occ, level, items, funcs, basic):
    """生成节点列表 [(series, type, level, parent_series|None, title)]"""
    nodes = []
    # L0
    nodes.append(('A', 0, 0, None, '基本要求'))
    nodes.append(('B', 0, 0, None, '相关知识'))
    nodes.append(('A', 1, 0, None, '技能要求'))

    # --- 基本要求 子树 ---
    nodes.append(('A-A', 0, 1, 'A', '职业道德'))
    sec = basic['职业道德']
    # sec: [职业道德基本知识, 职业守则]
    for j, (sec_title, sec_items) in enumerate(sec):
        p = f'A-A-{letter(j)}'
        nodes.append((p, 0, 2, 'A-A', sec_title))
        for k, it in enumerate(sec_items, 1):
            nodes.append((f'{p}-{k:03d}', 0, 3, p, it))
    nodes.append(('A-B', 0, 1, 'A', '基础知识'))
    for j, (sec_title, sec_items) in enumerate(basic['基础知识']):
        p = f'A-B-{letter(j)}'
        nodes.append((p, 0, 2, 'A-B', sec_title))
        for k, it in enumerate(sec_items, 1):
            nodes.append((f'{p}-{k:03d}', 0, 3, p, it))

    # --- 相关知识 / 技能要求 子树 ---
    for pos, item_no in enumerate(items):
        f = funcs.get(item_no)
        if f is None:
            raise RuntimeError(f'course {cid}: item {item_no} missing in parsed data')
        bseries = f'B-{letter(pos)}'
        sseries = f'A-{letter(pos)}'
        title = normalize_title(f['title'])
        nodes.append((bseries, 0, 1, 'B', title))          # 理论 L1
        nodes.append((sseries, 1, 1, 'A@skill', title))    # 技能 L1
        works = list(f['works'].values())
        for j, w in enumerate(works):
            wtitle = normalize_title(w['title'])
            bp = f'{bseries}-{letter(j)}'
            sp = f'{sseries}-{letter(j)}'
            nodes.append((bp, 0, 2, bseries, wtitle))
            nodes.append((sp, 1, 2, sseries, wtitle))
            for k, (_, pt) in enumerate(w['knows'], 1):
                nodes.append((f'{bp}-{k:03d}', 0, 3, bp, normalize_title(pt)))
            for k, (_, pt) in enumerate(w['skills'], 1):
                nodes.append((f'{sp}-{k:03d}', 1, 3, sp, normalize_title(pt)))
    return nodes


def main():
    with open(BASE + 'parsed_standards.json', encoding='utf-8') as f:
        data = json.load(f)
    # parsed json 的 funcs key 是字符串
    parsed = {}
    for occ, levels in data.items():
        for lv, funcs in levels.items():
            parsed[(occ, lv)] = {int(k): v for k, v in funcs.items()}

    basic = {
        '焊工': parse_basic(BASE + '焊工标准.md'),
        '电工': parse_basic(BASE + '电工.md'),
    }
    for occ, b in basic.items():
        print(occ, '职业道德 sections:', [(t, len(i)) for t, i in b['职业道德']])
        print(occ, '基础知识 sections:', [(t, len(i)) for t, i in b['基础知识']])

    tree_lines = [
        '-- ============================================================',
        '-- 焊工系17门 + 电工5门 知识树重建（国标 2026-08-19）',
        '-- 执行库：PHP 库 gjjddb @ 114.132.67.185:3308',
        '-- 幂等：以非删除态 L0"相关知识"根为已重建标记，可重复执行',
        '-- 注意：283/285/291/292 原有垃圾树将被软删（deleted=1），其上的',
        '--       存量题目（283:1709 / 285:607 / 291:504 / 292:348）仍指向旧节点，',
        '--       题目重新挂接由后续专项处理。',
        '-- ============================================================',
        '',
    ]
    rate_lines = [
        '-- ============================================================',
        '-- 焊工系17门 + 电工5门 type=3 国标理论知识权重（2026-08-19）',
        '-- 焊工标准为"每项25分+抽考"结构：必选=25，任选按期望分值取整，合计=100',
        '-- 电工为固定权重结构，直接取自标准4.1表',
        '-- 幂等：NOT EXISTS 模式，可重复执行',
        '-- ============================================================',
        '',
    ]

    for cfg in COURSES:
        cid, occ, level, items = cfg['cid'], cfg['occ'], cfg['level'], cfg['items']
        funcs = parsed[(occ, level)]
        nodes = build_course_tree(cid, occ, level, items, funcs, basic[occ])

        tree_lines.append(f'-- ======== course {cid}（{occ} {level}）========')
        tree_lines.append(
            f"SET @done_{cid} := (SELECT COUNT(*) FROM exam_knowledge_structure "
            f"WHERE course_id={cid} AND level=0 AND title='相关知识' AND deleted=0);"
        )
        tree_lines.append(
            f'UPDATE exam_knowledge_structure SET deleted=1 WHERE course_id={cid} AND deleted=0 AND @done_{cid}=0;'
        )
        var_of = {}
        for series, typ, lvl, parent_series, title in nodes:
            # 父 var：skill 树的 L0 与 basic 树的 L0 series 都是 A，用 type 区分 var 名
            if lvl == 0:
                key = f'{series}_t{typ}'
                parent_expr = 'NULL'
            else:
                # parent_series 可能是 'A@skill'
                if parent_series == 'A@skill':
                    pkey = 'A_t1'
                else:
                    ptype = typ  # 父子同树（type 相同）
                    pkey = f'{parent_series}_t{ptype}'
                parent_expr = f'@n{cid}_{pkey}'.replace('-', '_')
            vname = f'n{cid}_{series.replace("-", "_")}_t{typ}'
            var_of[(series, typ)] = vname
            title_sql = esc(title)
            tree_lines.append(
                'INSERT INTO exam_knowledge_structure '
                '(use_type, type, cover, intro, course_id, parent_id, title, series_no, level, important, user_id, status, sort_number, tenant_id, amount, deleted)\n'
                f"SELECT 0, {typ}, NULL, NULL, {cid}, {parent_expr}, '{title_sql}', '{series}', {lvl}, NULL, {USER_ID}, 1, 1, {TENANT}, 10.00, 0 FROM DUAL\n"
                f'WHERE @done_{cid}=0;'
            )
            tree_lines.append(f'SET @{vname} := (SELECT LAST_INSERT_ID());')
        tree_lines.append('')

        # ---- 权重 ----
        total = sum(r for _, r, _ in cfg['rates'])
        assert total == 100, f'course {cid} rate sum={total}'
        rate_lines.append(f'-- {cid}（{occ} {LEVEL_CN[level]}）')
        occ_std = '焊工' if occ == '焊工' else '电工'
        rate_lines.append(
            'INSERT INTO exam_knowledge_rate (course_id, type, title, is_standard, sort_number, comments, status, user_id, deleted, tenant_id)\n'
            f"SELECT {cid}, 3, '国标理论知识权重', 1, 1, '{occ_std}国家职业技能标准 4.1 理论知识权重表（{LEVEL_CN[level]}）', 1, NULL, 0, {TENANT} FROM DUAL\n"
            f'WHERE NOT EXISTS (SELECT 1 FROM exam_knowledge_rate WHERE course_id={cid} AND type=3 AND deleted=0);'
        )
        rate_lines.append(
            f'SET @rid_{cid} := (SELECT id FROM exam_knowledge_rate WHERE course_id={cid} AND type=3 AND deleted=0 ORDER BY id DESC LIMIT 1);'
        )
        selects = []
        # 计算每个 rate 项对应的 series：职业道德 A-A，基础知识 A-B，职业功能按位置 B-X
        pos_of_item = {item_no: idx for idx, item_no in enumerate(items)}
        for i, (ref, rate, cmt) in enumerate(cfg['rates'], 1):
            if ref == '职业道德':
                series = 'A-A'
            elif ref == '基础知识':
                series = 'A-B'
            else:
                series = f'B-{letter(pos_of_item[ref])}'
                ftitle = normalize_title(funcs[ref]['title'])
                cmt = f'{ftitle}（{cmt}）' if '（' not in cmt else cmt
            selects.append(
                f"SELECT @rid_{cid}, (SELECT id FROM exam_knowledge_structure WHERE course_id={cid} AND type=0 AND level=1 AND series_no='{series}' AND deleted=0), "
                f"{rate}, {i}, '{esc(cmt)}', 1, 0, {TENANT} FROM DUAL\n"
                f'WHERE NOT EXISTS (SELECT 1 FROM exam_knowledge_rate_detail d JOIN exam_knowledge_structure k ON k.id=d.knowledge_id '
                f"WHERE d.knowledge_rate_id=@rid_{cid} AND k.course_id={cid} AND k.series_no='{series}' AND d.deleted=0)"
            )
        rate_lines.append(
            'INSERT INTO exam_knowledge_rate_detail (knowledge_rate_id, knowledge_id, rate, sort_number, comments, status, deleted, tenant_id)\n'
            + '\nUNION ALL\n'.join(selects) + ';'
        )
        rate_lines.append('')

    with open(TREE_SQL, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(tree_lines))
    with open(RATE_SQL, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(rate_lines))
    print(f'tree sql: {TREE_SQL} lines={len(tree_lines)}')
    print(f'rate sql: {RATE_SQL} lines={len(rate_lines)}')


if __name__ == '__main__':
    main()
