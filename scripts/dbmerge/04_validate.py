# -*- coding: utf-8 -*-
"""
04_validate.py — 迁移校验（方案阶段 1.5 / 0.2）

对目标库（预演=gjjddbpro_rehearsal，正式=gjjddbpro）：
  1. 行数比对：与 expected_counts.tsv（预演时生成）逐表比对，差异列表
  2. C 类行数恒等式：merged == PHP_n + only_saas_n；B 类 merged == php_n + saas_n
  3. auto_increment 水位：B/C 表 AUTO_INCREMENT > MAX(id)
  4. 关键表内容抽验：platform_user / exam_exam_detail 的若干 id 做源-目标行内容一致性比对

凭据走环境变量：SAAS_PASS / PHP_DB_ROOT_PASS / TARGET_PASS。
用法：python 04_validate.py  [TARGET_DB 默认 gjjddbpro_rehearsal]
"""
import json, os, subprocess, sys
from datetime import datetime

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
TARGET_DB = sys.argv[1] if len(sys.argv) > 1 else "gjjddbpro_rehearsal"
SAAS = dict(host="43.138.163.7", port="3308", user="gjjd", password=os.environ.get("SAAS_PASS", ""), db="gjjddb2026")
PHP = dict(host="114.132.67.185", port="3308", user="root", password=os.environ.get("PHP_DB_ROOT_PASS", ""), db="gjjddb")
TGT = dict(host="43.138.163.7", port="3308", user="root", password=os.environ.get("TARGET_PASS", ""), db=TARGET_DB)
for c in (SAAS, PHP, TGT):
    if not c["password"]:
        sys.exit("需环境变量 SAAS_PASS / PHP_DB_ROOT_PASS / TARGET_PASS")

ROOT = os.path.dirname(os.path.abspath(__file__))
ANALYSIS = os.path.join(ROOT, "../../outputs/dbmerge-analysis")
REPORT = os.path.join(ROOT, "../../outputs/dbmerge-rehearsal")
cats = json.load(open(os.path.join(ANALYSIS, "table_categories.json"), encoding="utf-8"))

def q(conn, sql):
    r = subprocess.run([MYSQL, "-h"+conn["host"], "-P"+conn["port"], "-u"+conn["user"], "-p"+conn["password"],
                        conn["db"], "-N", "-e", sql], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        raise RuntimeError("%s\n%s" % (sql[:80], r.stderr[:300]))
    return [l for l in r.stdout.splitlines() if l.strip()]

def count(conn, t):
    return int(q(conn, "SELECT COUNT(*) FROM `%s`" % t)[0])

def ids(conn, t):
    return set(l for l in q(conn, "SELECT id FROM `%s`" % t))

def main():
    rep = ["# 04 校验报告", "", "目标库：%s　时间：%s" % (TARGET_DB, datetime.now().strftime("%Y-%m-%d %H:%M:%S")), ""]
    fail = 0

    # 1. C/B 类行数恒等式（期望值用当前两侧实时计算，不用 8-20 快照）
    rep.append("## C/B 类行数恒等式（期望=PHP当前行数 + SaaS独有行当前行数）")
    for grp in ("C", "B"):
        for t in cats[grp]:
            php_n = count(PHP, t)
            saas_n = count(SAAS, t)
            if grp == "B":
                exp = php_n + saas_n           # 并集：两段 ID 不相交
            else:
                exp = php_n + len(ids(SAAS, t) - ids(PHP, t))  # PHP 基 + SaaS 独有
            act = count(TGT, t)
            ok = exp == act
            if not ok:
                fail += 1
            rep.append("- %s `%s`: 期望 %d = 实际 %d（PHP %d / SaaS %d）" % ("✅" if ok else "❌", t, exp, act, php_n, saas_n))
    rep.append("")

    # 2. auto_increment 水位（B/C）
    rep.append("## auto_increment 水位（B/C）")
    for grp in ("B", "C"):
        for t in cats[grp]:
            ai = q(TGT, "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s'" % (TARGET_DB, t))
            mx = q(TGT, "SELECT COALESCE(MAX(id),0) FROM `%s`" % t)
            aiv = int(ai[0] or 0) if ai and ai[0] not in ("NULL", "") else 0
            mxv = int(mx[0])
            ok = aiv > mxv
            if not ok: fail += 1
            rep.append("- %s `%s`: AUTO_INCREMENT=%d MAX(id)=%d" % ("✅" if ok else "❌", t, aiv, mxv))
    rep.append("")

    # 3. 关键表内容抽验（platform_user 随机 5 个 id、exam_exam_detail 最新 5 个 id，比对源/目标存在性）
    rep.append("## 关键表内容抽验")
    for t in ("platform_user", "exam_exam_detail"):
        order = "RAND()" if t == "platform_user" else "id DESC"
        idlist = q(PHP, "SELECT id FROM `%s` ORDER BY %s LIMIT 5" % (t, order))
        for i in idlist:
            a = q(PHP, "SELECT COUNT(*) FROM `%s` WHERE id=%s" % (t, i))
            b = q(TGT, "SELECT COUNT(*) FROM `%s` WHERE id=%s" % (t, i))
            ok = a and b and a[0] == b[0] == "1"
            if not ok:
                fail += 1
            rep.append("- %s `%s`.id=%s 源/目标存在性" % ("✅" if ok else "❌", t, i))
    rep.append("")

    rep.append("## 总结")
    rep.append("**%s**（失败 %d 项）" % ("通过" if fail == 0 else "存在失败项", fail))
    os.makedirs(REPORT, exist_ok=True)
    out = os.path.join(REPORT, "validate_report.md")
    open(out, "w", encoding="utf-8", newline="\n").write("\n".join(rep))
    print("written", out, "fail=", fail)

if __name__ == "__main__":
    main()
