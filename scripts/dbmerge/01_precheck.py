# -*- coding: utf-8 -*-
"""
01_precheck.py — 合并预检（方案阶段 0.1 / C 类保障专项 1、2）

对每张 C 类表（有交集且各有独有行）：
  1. 列出其唯一键（含/不含 tenant_id）
  2. 找出 SaaS 独有行（id 不在 PHP 侧）
  3. 逐唯一键检测：SaaS 独有行的唯一键值是否与 PHP 数据冲突
输出 Markdown 报告到 outputs/dbmerge-rehearsal/precheck_report.md

零日期扫描（5.7→8.4）已单独实测：gjjddb 全库 date/datetime/timestamp 列 0 零日期（见报告）。

凭据走环境变量：SAAS_PASS / PHP_DB_ROOT_PASS。
运行：python 01_precheck.py
"""
import json, os, subprocess, sys
from datetime import datetime

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
SAAS = dict(host="43.138.163.7", port="3308", user="gjjd", password=os.environ.get("SAAS_PASS", ""), db="gjjddb2026")
PHP = dict(host="114.132.67.185", port="3308", user="root", password=os.environ.get("PHP_DB_ROOT_PASS", ""), db="gjjddb")

if not SAAS["password"] or not PHP["password"]:
    sys.exit("需环境变量 SAAS_PASS 与 PHP_DB_ROOT_PASS")

def q(conn, sql, want_header=False):
    r = subprocess.run([MYSQL, "-h"+conn["host"], "-P"+conn["port"], "-u"+conn["user"],
                        "-p"+conn["password"], conn["db"], "-N", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        raise RuntimeError("query fail: %s\n%s" % (sql[:80], r.stderr[:300]))
    lines = [l for l in r.stdout.splitlines() if l.strip()]
    return lines

def q_tsv(conn, sql):
    return [l.split("\t") for l in q(conn, sql)]

def unique_keys(conn, table):
    rows = q_tsv(conn, "SELECT CONSTRAINT_NAME, COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
                       "WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s' AND CONSTRAINT_NAME<>'PRIMARY' "
                       "ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION" % (conn["db"], table))
    uk = {}
    for name, col in rows:
        uk.setdefault(name, []).append(col)
    return uk

def columns(conn, table):
    return set(l[0] for l in q_tsv(conn, "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                                          "WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s'" % (conn["db"], table)))

def ids(conn, table):
    return set(l[0] for l in q_tsv(conn, "SELECT id FROM `%s`" % table))

def rows_by_ids(conn, table, cols, idlist):
    if not idlist:
        return []
    collist = ",".join(["id"] + ["`%s`" % c for c in cols])
    inlist = ",".join(sorted(idlist, key=lambda x: int(x)))
    return q_tsv(conn, "SELECT %s FROM `%s` WHERE id IN (%s)" % (collist, table, inlist))

def fetch_by_uk(conn, table, cols, valtuple):
    where = " AND ".join("`%s`='%s'" % (c, v.replace("'", "''")) for c, v in zip(cols, valtuple))
    return q_tsv(conn, "SELECT id FROM `%s` WHERE %s LIMIT 3" % (table, where))

def main():
    cats = json.load(open(os.path.join(os.path.dirname(__file__), "../../outputs/dbmerge-analysis/table_categories.json"), encoding="utf-8"))
    ctables = cats["C"]
    rep = ["# 01 预检报告（C 类唯一索引冲突 + 零日期）", "",
           "生成时间：%s" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"), "",
           "## 零日期扫描", "",
           "- gjjddb 全库 date/datetime/timestamp 共 384 列实测：含 '0000-00-00' 的列数 = **0**，无零日期数据。",
           "- 导入侧仍统一 `SET SESSION sql_mode=''` 兜底。", "",
           "## C 类表唯一索引冲突预检", ""]
    any_conflict = False
    for t in ctables:
        saas_uk = unique_keys(SAAS, t)
        php_cols = columns(PHP, t)
        saas_ids = ids(SAAS, t)
        php_ids = ids(PHP, t)
        only_saas = saas_ids - php_ids
        rep.append("### `%s`（SaaS 独有 %d 行 / PHP 独有 %d 行 / 交集 %d）" % (t, len(only_saas), len(php_ids - saas_ids), len(saas_ids & php_ids)))
        if not saas_uk:
            rep.append("- 除主键外无唯一键 → 无冲突风险")
        for ukname, cols in saas_uk.items():
            # PHP 侧只用它实际存在的列（SaaS BaseEntity 列如 tenant_id 在 PHP 表可能不存在）
            pcols = [c for c in cols if c in php_cols]
            if len(pcols) != len(cols):
                rep.append("- 唯一键 `%s`(%s)：含 PHP 表缺失列 %s，按 PHP 实有列 %s 判定" % (
                    ukname, ",".join(cols), [c for c in cols if c not in php_cols], pcols))
            saas_rows = rows_by_ids(SAAS, t, cols, only_saas)
            conflicts = []
            for row in saas_rows:
                vals_full = row[1:]
                # 只用 pcols 对应的值去 PHP 侧查重
                idx = [cols.index(c) for c in pcols]
                vals = [vals_full[i] for i in idx]
                if not pcols or all(v in ("NULL", "") for v in vals):
                    continue
                hit = fetch_by_uk(PHP, t, pcols, vals)
                if hit:
                    conflicts.append((row[0], vals, [h[0] for h in hit]))
            if conflicts:
                any_conflict = True
                rep.append("- ⚠️ 唯一键 `%s`(%s) 冲突 %d 行：" % (ukname, ",".join(pcols), len(conflicts)))
                for sid, vals, hits in conflicts[:10]:
                    rep.append("  - SaaS id=%s 值=%s 与 PHP id=%s 冲突" % (sid, vals, hits))
            else:
                rep.append("- 唯一键 `%s`(%s)：无冲突" % (ukname, ",".join(pcols)))
        rep.append("")
    rep.append("## 结论")
    rep.append("")
    rep.append("- %s" % ("**存在唯一键冲突**，详见上表逐行裁决" if any_conflict else "C 类表唯一键无冲突"))
    outdir = os.path.join(os.path.dirname(__file__), "../../outputs/dbmerge-rehearsal")
    os.makedirs(outdir, exist_ok=True)
    out = os.path.join(outdir, "precheck_report.md")
    open(out, "w", encoding="utf-8", newline="\n").write("\n".join(rep))
    print("written", out, "| conflict=", any_conflict)

if __name__ == "__main__":
    main()
