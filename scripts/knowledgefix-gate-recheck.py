#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""知识归属修正·关键词剪枝闸门复核脚本（方案 v1.3 附四配套，全程只读）

复现 docs/05_decisions/知识归属修正_JEV分层改造方案.md §7.2 附四的闸门读数：
L2 证据聚合 top5 召回 / keep 保位 top5 / 兄弟内 top5 / 全局 L3 top1 / L2 标题直配 top2，
按课程（228/229/291/292/181）与全体输出。

评分器口径（与 v1.1/附四一致，零手工规则）：
  节点标题切短语词（分隔符切分、2~14 字）+ 字符 bigram，课程内 IDF 加权；
  短语命中以 bigram 预过滤再确认；bigram 权重 0.25、短语权重 idf*min(len/4,2)。

数据两种模式：
  1) --cache <path>  读本地 JSON 缓存 {"nodes":[...], "applied":[...]}
     nodes 行: id, course_id, parent_id, level, title   (exam_knowledge_structure, deleted=0)
     applied 行: course_id, old_knowledge_id, new_knowledge_id, content
                (exam_detail_knowledge_fix status='applied' join exam_exam_detail)
  2) --pull          直连生产库只读拉取并写缓存（v1.1 口径：deleted=0 且存在 fix 行的课程）
     连接参数走环境变量：KFIX_MYSQL_HOST / KFIX_MYSQL_PORT / KFIX_MYSQL_USER /
                         KFIX_MYSQL_PASSWORD / KFIX_MYSQL_DB（默认 gjjddbpro）
     依赖: pip install pymysql

用法:
  python scripts/knowledgefix-gate-recheck.py --cache data.json
  python scripts/knowledgefix-gate-recheck.py --pull --cache-out data.json

口径提示：取数范围（哪些课程、过滤条件）是读数的一部分——v1.1 口径（62 课 16,277 节点）
与 v1.3 复核口径（23,073 节点）在 228 课读数一致（71.8%），其余课程有 1.2~8.0 点漂移。
闸门判定（228 ≥ 60%）只依赖 228 课。更换口径须在文档中声明。
"""
import io
import json
import math
import re
import sys
from collections import defaultdict, Counter

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

FOCUS_COURSES = [228, 229, 291, 292, 181]


def pull(cache_out):
    """只读拉取（v1.1 口径）。凭据全部来自环境变量，不落代码。"""
    import os
    import pymysql
    conn = pymysql.connect(
        host=os.environ.get('KFIX_MYSQL_HOST', '127.0.0.1'),
        port=int(os.environ.get('KFIX_MYSQL_PORT', '3306')),
        user=os.environ.get('KFIX_MYSQL_USER', ''),
        password=os.environ.get('KFIX_MYSQL_PASSWORD', ''),
        database=os.environ.get('KFIX_MYSQL_DB', 'gjjddbpro'),
        charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
    cur = conn.cursor()
    cur.execute("SELECT DISTINCT course_id FROM exam_detail_knowledge_fix WHERE deleted=0")
    course_ids = [r['course_id'] for r in cur.fetchall()]
    fmt = ','.join(['%s'] * len(course_ids))
    cur.execute(f"SELECT id, course_id, parent_id, level, title, series_no, status "
                f"FROM exam_knowledge_structure WHERE deleted=0 AND course_id IN ({fmt})", course_ids)
    nodes = cur.fetchall()
    cur.execute("SELECT f.id, f.detail_id, f.course_id, f.old_knowledge_id, f.new_knowledge_id, "
                f"f.basis, d.content, d.type "
                f"FROM exam_detail_knowledge_fix f JOIN exam_exam_detail d ON d.id = f.detail_id "
                f"WHERE f.deleted=0 AND f.status='applied' AND d.deleted=0")
    applied = cur.fetchall()
    conn.close()
    json.dump({'nodes': nodes, 'applied': applied},
              open(cache_out, 'w', encoding='utf-8'), ensure_ascii=False)
    print(f'pulled: nodes={len(nodes)} applied={len(applied)} -> {cache_out}')


def main():
    args = sys.argv[1:]
    cache = 'data.json'
    if '--pull' in args:
        cache_out = args[args.index('--cache-out') + 1] if '--cache-out' in args else cache
        pull(cache_out)
    if '--cache' in args:
        cache = args[args.index('--cache') + 1]
    data = json.load(open(cache, encoding='utf-8'))
    nodes, applied = data['nodes'], data['applied']
    print(f'cache: nodes={len(nodes)} applied={len(applied)}')

    course_l2, course_l3 = defaultdict(list), defaultdict(list)
    l3_parent, node_title = {}, {}
    for n in nodes:
        node_title[n['id']] = (n.get('title') or '').strip()
        if n['level'] == 2:
            course_l2[n['course_id']].append(n['id'])
        elif n['level'] == 3:
            course_l3[n['course_id']].append(n['id'])
            l3_parent[n['id']] = n['parent_id']
    l2_children = defaultdict(list)
    for nid, pid in l3_parent.items():
        l2_children[pid].append(nid)

    split_re = re.compile(r'[、，,；;：:（）()\[\]【】/／\s★.。·\-—]+')

    def extract_terms(title):
        segs = [s for s in split_re.split(title) if 2 <= len(s) <= 14]
        phrases, bigrams = [], set()
        for s in segs:
            phrases.append(s)
            for i in range(len(s) - 1):
                bigrams.add(s[i:i + 2])
        for i in range(len(title) - 1):
            bigrams.add(title[i:i + 2])
        return phrases, bigrams

    class NodeIndex:
        def __init__(self, node_ids):
            self.node_phrases, self.node_bigrams = {}, {}
            df_p, df_b = Counter(), Counter()
            for n in node_ids:
                p, b = extract_terms(node_title.get(n, ''))
                self.node_phrases[n], self.node_bigrams[n] = p, b
                for x in set(p):
                    df_p[x] += 1
                for x in b:
                    df_b[x] += 1
            total = max(len(node_ids), 1)
            self.p_idf = {x: math.log(1 + total / (1 + df)) + 0.5 for x, df in df_p.items()}
            self.b_idf = {x: math.log(1 + total / (1 + df)) for x, df in df_b.items()}
            self.p2n = defaultdict(list)
            self.p_bigrams = {}
            for n, ps in self.node_phrases.items():
                for p in ps:
                    self.p2n[p].append(n)
                    self.p_bigrams[p] = {p[i:i + 2] for i in range(len(p) - 1)}
            self.b2n = defaultdict(list)
            for n, bs in self.node_bigrams.items():
                for b in bs:
                    self.b2n[b].append(n)
            self.phrase_list = list(self.p_idf)

        def score(self, text):
            tbg = {text[i:i + 2] for i in range(len(text) - 1)}
            scores = defaultdict(float)
            for p, pidf in self.p_idf.items():
                if self.p_bigrams[p] <= tbg and p in text:
                    w = pidf * min(len(p) / 4.0, 2.0)
                    for n in self.p2n[p]:
                        scores[n] += w
            covered = set()
            for p in self.phrase_list:
                if self.p_bigrams[p] <= tbg and p in text:
                    covered |= self.p_bigrams[p]
            for b, bidf in self.b_idf.items():
                if b in covered or b not in tbg:
                    continue
                for n in self.b2n[b]:
                    scores[n] += bidf * 0.25
            return scores

    l3_index = {cid: NodeIndex(ids) for cid, ids in course_l3.items()}
    l2_index = {cid: NodeIndex(ids) for cid, ids in course_l2.items() if len(ids) >= 2}

    def stem_of(content):
        return (content or '').split('\n')[0]

    agg = defaultdict(Counter)
    for r in applied:
        cid = r['course_id']
        target = r['new_knowledge_id']
        li3 = l3_index.get(cid)
        if li3 is None or target not in li3.node_phrases:
            continue
        s3 = li3.score(stem_of(r['content']))
        tl2 = l3_parent.get(target)
        old_l2 = l3_parent.get(r['old_knowledge_id'])
        l2_scores = {}
        for l2 in course_l2[cid]:
            ch = [c for c in l2_children.get(l2, []) if c in course_l3[cid]]
            if ch:
                l2_scores[l2] = max(s3.get(c, 0.0) for c in ch)
        rk = sorted(l2_scores.items(), key=lambda kv: (-kv[1], kv[0]))
        ids5 = [l2 for l2, _ in rk[:5]]
        l3rk = sorted(s3.items(), key=lambda kv: (-kv[1], kv[0]))
        g_rank = next((i for i, (n, _) in enumerate(l3rk) if n == target), None)
        sibs = [n for n in course_l3[cid] if l3_parent.get(n) == tl2]
        sib_ranked = sorted(sibs, key=lambda n: -s3.get(n, 0.0))
        s_rank = next((i for i, n in enumerate(sib_ranked) if n == target), None)
        t2hit = False
        li2 = l2_index.get(cid)
        if li2 is not None and tl2 in li2.node_phrases:
            t2rk = sorted(li2.score(stem_of(r['content'])).items(), key=lambda kv: (-kv[1], kv[0]))
            t2hit = tl2 in [l2 for l2, _ in t2rk[:2]]

        for b in ([cid] if cid in FOCUS_COURSES else []) + ['ALL']:
            c = agg[b]
            c['n'] += 1
            c['agg_top5'] += tl2 in ids5
            ids5p = list(ids5)
            if old_l2 and old_l2 not in ids5p and old_l2 in l2_scores:
                ids5p[-1] = old_l2
            c['reserve_top5'] += tl2 in ids5p
            c['glob_top1'] += g_rank == 0
            c['sib_top5'] += s_rank is not None and s_rank < 5
            c['title_top2'] += t2hit

    hdr = f"{'课程':>6} {'有效行':>6} {'agg_top5':>9} {'保位top5':>9} {'兄弟top5':>9} {'全局top1':>9} {'标题top2':>9}"
    print(hdr)
    for b in FOCUS_COURSES + ['ALL']:
        c = agg[b]
        def pct(a):
            return f'{a / max(c["n"], 1):.1%}'
        print(f"{str(b):>6} {c['n']:>6} {pct(c['agg_top5']):>9} {pct(c['reserve_top5']):>9} "
              f"{pct(c['sib_top5']):>9} {pct(c['glob_top1']):>9} {pct(c['title_top2']):>9}")
    print('\n闸门判定口径：228 课 agg_top5 ≥ 60%（60% × 兄弟 top5 91.6% ≈ 55% > §7.1 的 50% 放弃线）')


if __name__ == '__main__':
    main()
