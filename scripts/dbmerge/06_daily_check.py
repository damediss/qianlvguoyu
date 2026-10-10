# -*- coding: utf-8 -*-
"""
06_daily_check.py — 阶段 3 观察期每日只读对账（方案：AI 每日一次）

只读检查（不改任何数据）：
  1. gjjddbpro 关键表行数与上一日快照的增量（gjjddbpro_daily_counts.tsv）
  2. SaaS guoyu-platform-api 日志中的 DB 报错关键词
  3. sync 通道拆除核验（单库化后：env 无 PLATFORM_SYNC_ENABLED/PHP_MYSQL_* 残留 + 日志无 sync 运行记录）
  4. PHP 站点错误日志（Fatal/SQLSTATE/Access denied）

凭据只读解析自仓库根目录「服务器信息.txt」（未入 git）。产物落 outputs/dbmerge-observe/。
运行：python 06_daily_check.py
"""
import os, re, subprocess, sys
from datetime import datetime

ROOT = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(ROOT, "../../")
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
OBSERVE = os.path.join(ROOT, "../../outputs/dbmerge-observe")
SNAP = os.path.join(OBSERVE, "gjjddbpro_daily_counts.tsv")

KEY_TABLES = ["platform_user", "platform_user_token", "exam_exam_detail", "exam_result",
              "exam_knowledge_structure", "train_people_list", "train_sign_up_self",
              "train_info_list", "ai_review_answer", "sys_audit_log", "exam_course"]

def parse_creds():
    txt = open(os.path.join(REPO, "服务器信息.txt"), encoding="utf-8").read()
    def grab(label_ip):
        m = re.search(re.escape(label_ip) + r"[\s\S]*?用户名：(\S+)\s*\n密码：(\S+)", txt)
        return (m.group(1), m.group(2)) if m else (None, None)
    return {
        "ssh114": grab("114.132.67.185"),
        "ssh43": grab("43.138.163.7"),
        "mysql43": grab("mysql8.4.9"),
    }

def mysql43(sql, db="gjjddbpro", mysql43=None):
    u, p = mysql43
    r = subprocess.run([MYSQL, "-h43.138.163.7", "-P3308", "-u"+u, "-p"+p, db, "-N", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout if r.returncode == 0 else "ERROR: " + r.stderr[:200]

def ssh(ip, sshcred, cmd):
    u, p = sshcred
    ap = os.path.join(os.environ.get("TEMP", "/tmp"), "ap_obs.sh")
    open(ap, "w").write('#!/bin/sh\necho \'%s\'\n' % p)
    os.chmod(ap, 0o700)
    env = dict(os.environ, SSH_ASKPASS=ap, SSH_ASKPASS_REQUIRE="force", DISPLAY=":0")
    r = subprocess.run(["ssh", "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=10",
                        u+"@"+ip, cmd], capture_output=True, text=True, encoding="utf-8",
                       errors="replace", env=env, timeout=60)
    os.remove(ap)
    return r.stdout + r.stderr

def clean_ssh_noise(text):
    drop = ("post-quantum", "store now, decrypt later", "openssh.com", "Warning: Permanently",
            "server may need to be upgraded")
    return "\n".join([l for l in text.splitlines()
                      if l.strip() and not any(d in l for d in drop)])

def main():
    creds = parse_creds()
    os.makedirs(OBSERVE, exist_ok=True)
    now = datetime.now()
    day = now.strftime("%Y-%m-%d")
    rep = ["# 观察期对账简报 %s" % now.strftime("%Y-%m-%d %H:%M"), ""]
    issues = 0

    # 1. 关键表行数 + 日增量
    rep.append("## 1. gjjddbpro 关键表行数（含日增量）")
    prev = {}
    if os.path.exists(SNAP):
        for l in open(SNAP, encoding="utf-8"):
            p = l.rstrip("\n").split("\t")
            if len(p) == 3:
                prev[p[1]] = int(p[2])
    cur = {}
    for t in KEY_TABLES:
        n = mysql43("SELECT COUNT(*) FROM `%s`" % t, mysql43=creds["mysql43"]).strip()
        n = int(n) if n.isdigit() else -1
        cur[t] = n
        delta = n - prev.get(t, n)
        rep.append("- `%s` = %d（较昨日 %+d）" % (t, n, delta))
    with open(SNAP, "a", encoding="utf-8", newline="\n") as f:
        for t, n in cur.items():
            f.write("%s\t%s\t%d\n" % (day, t, n))
    rep.append("")

    # 2. SaaS DB 报错
    rep.append("## 2. SaaS guoyu-platform-api 近 24h DB 报错")
    saas_err = ssh("43.138.163.7", creds["ssh43"],
                   "journalctl -u guoyu-platform-api --no-pager --since '24 hours ago' 2>/dev/null | grep -iE 'Unknown column|doesn.t exist|Bad SQL|Communications link|Access denied' | tail -10")
    saas_err = clean_ssh_noise(saas_err)
    if saas_err.strip():
        issues += 1
        rep.append("```\n" + saas_err.strip() + "\n```")
    else:
        rep.append("（无）")
    rep.append("")

    # 3. sync 通道拆除核验（单库化改造后口径：env 不得再有 PLATFORM_SYNC_* / PHP_MYSQL_* 残留，
    #    日志不得出现 PlatformSync/镜像同步记录；改造部署前旧口径会持续报 ⚠️，属预期过渡）
    rep.append("## 3. sync 通道（单库化拆除核验）")
    sync_env = ssh("43.138.163.7", creds["ssh43"],
                   "grep -E 'PLATFORM_SYNC_|PHP_MYSQL_|PLATFORM_USER_PHP_WRITE_ENABLED' /opt/guoyu-platform/config/platform-api.env")
    sync_log = ssh("43.138.163.7", creds["ssh43"],
                   "journalctl -u guoyu-platform-api --no-pager --since '24 hours ago' 2>/dev/null | grep -iE 'PlatformSync|sync.*mirror|php_synced' | tail -3")
    sync_env_c = clean_ssh_noise(sync_env)
    env_leftover = bool([l for l in sync_env_c.splitlines() if l.strip() and "=" in l])
    sync_log_c = clean_ssh_noise(sync_log)
    sync_ran = bool([l for l in sync_log_c.splitlines() if l.strip()])
    if env_leftover or sync_ran:
        issues += 1
    rep.append("- env 残留：%s；近 24h 运行记录：%s" % (env_leftover and "⚠️ 有" or "无", sync_ran and "⚠️ 有" or "无"))
    rep.append("")

    # 4. PHP 站点错误
    rep.append("## 4. PHP 站点近 24h 致命/ SQL 错误")
    php_err = ssh("114.132.67.185", creds["ssh114"],
                  "tail -500 /www/wwwlogs/gjjdp.gjjd168.com.error.log 2>/dev/null | grep -iE 'Fatal|SQLSTATE|Access denied|Unknown column' | tail -10")
    php_err = clean_ssh_noise(php_err)
    if php_err.strip():
        issues += 1
        rep.append("```\n" + php_err.strip() + "\n```")
    else:
        rep.append("（无）")
    rep.append("")

    rep.append("## 结论")
    rep.append("**%s**" % ("⚠️ 有异常项，见上" if issues else "✅ 双端运行正常，无 sync 运行记录，无 DB 报错"))
    out = os.path.join(OBSERVE, "observe_%s.md" % day.replace("-", ""))
    open(out, "w", encoding="utf-8", newline="\n").write("\n".join(rep))
    print("written", out, "| issues=", issues)

if __name__ == "__main__":
    main()
