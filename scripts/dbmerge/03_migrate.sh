#!/usr/bin/env bash
# =============================================================================
# 03_migrate.sh — 数据迁移总控（gjjddb + gjjddb2026 → gjjddbpro + gxwebsoft_core）
#
# 前置：02_ddl.sql 已在目标库执行（目标表为空）。
# 用法：
#   export SAAS_PASS=... PHP_DB_ROOT_PASS=... TARGET_PASS=... PHP_SSH_PASS=... TARGET_SSH_PASS=...
#   bash 03_migrate.sh                                  # 预演：目标 rehearsal 库
#   TARGET_DB=gjjddbpro GX_TARGET_DB=gxwebsoft_core bash 03_migrate.sh   # 正式窗口
#
# 顺序（对应方案阶段 1.4）：①flyway ②SaaS独有+A_sup+D ⑤a B的SaaS侧 ③PHP独有+A+C+B的PHP侧
#   ④C追加SaaS独有行 ⑥gxwebsoft_core ⑦B/C auto_increment 抬升
# MANUAL 表（cms_article*/train_dev_organization）不在本脚本，待裁决后单独补。
# 凭据一律走环境变量，不写入本脚本（凭据安全约定 2026-08-24）。
# =============================================================================
set -euo pipefail

SAAS_HOST="${SAAS_HOST:-43.138.163.7}"
SAAS_PORT="${SAAS_PORT:-3308}"
SAAS_USER="${SAAS_USER:-gjjd}"
SAAS_PASS="${SAAS_PASS:?需环境变量 SAAS_PASS}"
SAAS_DB="${SAAS_DB:-gjjddb2026}"

PHP_HOST="${PHP_HOST:-114.132.67.185}"
PHP_DB="${PHP_DB:-gjjddb}"
PHP_GX_DB="${PHP_GX_DB:-gxwebsoft_core}"
PHP_SSH_USER="${PHP_SSH_USER:-root}"
PHP_SSH_PASS="${PHP_SSH_PASS:?需环境变量 PHP_SSH_PASS（114 SSH root）}"
PHP_DB_ROOT_PASS="${PHP_DB_ROOT_PASS:?需环境变量 PHP_DB_ROOT_PASS（114 本机 5.7 root）}"

TARGET_HOST="${TARGET_HOST:-43.138.163.7}"
TARGET_PORT="${TARGET_PORT:-3308}"
TARGET_USER="${TARGET_USER:-root}"
TARGET_PASS="${TARGET_PASS:?需环境变量 TARGET_PASS（43 root）}"
TARGET_DB="${TARGET_DB:-gjjddbpro_rehearsal}"
GX_TARGET_DB="${GX_TARGET_DB:-gxwebsoft_core_rehearsal}"
TARGET_SSH_USER="${TARGET_SSH_USER:-root}"
TARGET_SSH_PASS="${TARGET_SSH_PASS:?需环境变量 TARGET_SSH_PASS（43 SSH root）}"

MYSQL="C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe"
DIR="$(cd "$(dirname "$0")" && pwd)"
LISTDIR="$DIR"
LOGDIR="$DIR/logs"
mkdir -p "$LOGDIR"
TS=$(date +%Y%m%d_%H%M%S)
MAINLOG="$LOGDIR/migrate_$TS.log"

# 114 SSH 免密（运行期临时 askpass，退出即删）
ASKPASS114="$(mktemp)"
printf '#!/bin/sh\necho "%s"\n' "$PHP_SSH_PASS" > "$ASKPASS114"
chmod +x "$ASKPASS114"
# 43 SSH 免密
ASKPASS43="$(mktemp)"
printf '#!/bin/sh\necho "%s"\n' "$TARGET_SSH_PASS" > "$ASKPASS43"
chmod +x "$ASKPASS43"
trap 'rm -f "$ASKPASS114" "$ASKPASS43"' EXIT

# 114→43 直传用的内网地址与 114 侧私钥（部署期已配好免密）
TGT_INTERNAL="${TGT_INTERNAL:-172.16.16.2}"
SSH_KEY_114="${SSH_KEY_114:-/root/.ssh/id_rsa_dbmerge}"

say(){ echo "[$(date +%H:%M:%S)] $*" | tee -a "$MAINLOG"; }
# 114 SSH（密码经临时 askpass）
SSH114(){ SSH_ASKPASS="$ASKPASS114" SSH_ASKPASS_REQUIRE=force DISPLAY=:0 ssh -o StrictHostKeyChecking=accept-new "$PHP_SSH_USER@$PHP_HOST" "$@"; }
# 43 SSH（密码经临时 askpass）
SSH43(){ SSH_ASKPASS="$ASKPASS43" SSH_ASKPASS_REQUIRE=force DISPLAY=:0 ssh -o StrictHostKeyChecking=accept-new "$TARGET_SSH_USER@$TARGET_HOST" "$@"; }

# 预计算 SaaS 源库每张表的非生成列清单（INSERT SELECT 不能含生成列，否则 ERROR 3105）
say "预计算非生成列清单"
COLS_FILE="$LOGDIR/cols_$TS.tsv"
"$MYSQL" -h"$SAAS_HOST" -P"$SAAS_PORT" -u"$SAAS_USER" -p"$SAAS_PASS" -N -e \
  "SET SESSION group_concat_max_len=1000000; \
   SELECT TABLE_NAME, GROUP_CONCAT(CONCAT('\`',COLUMN_NAME,'\`') ORDER BY ORDINAL_POSITION) \
   FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$SAAS_DB' AND EXTRA NOT LIKE '%GENERATED%' \
   GROUP BY TABLE_NAME" 2>/dev/null > "$COLS_FILE"
col_list(){ awk -F'\t' -v t="$1" '$1==t{print $2}' "$COLS_FILE"; }

# 目标写入统一会话参数：关外键检查（规避父子表加载顺序）+ 放宽 sql_mode（兼容 5.7）
TGT_INIT="SET SESSION foreign_key_checks=0, sql_mode=''"

# 在目标实例(43)执行 SQL
run_target(){ "$MYSQL" -h"$TARGET_HOST" -P"$TARGET_PORT" -u"$TARGET_USER" -p"$TARGET_PASS" --default-character-set=utf8mb4 --init-command="$TGT_INIT" -e "$1" >>"$MAINLOG" 2>&1; }

# SaaS(gjjddb2026，同实例) → 目标 INSERT SELECT（显式列，排除生成列）
load_saas(){
  local t="$1" cols
  cols="$(col_list "$t")"
  if [ -z "$cols" ]; then say "  !! $t 无列清单，跳过"; return 0; fi
  run_target "INSERT INTO \`$TARGET_DB\`.\`$t\` ($cols) SELECT $cols FROM \`$SAAS_DB\`.\`$t\`"
  say "  SaaS→目标 $t 完成"
}

# PHP 表数据导出（114 docker mysqldump，仅数据不建表）并经本机 relay 导入目标
load_php(){
  local db="$1" t="$2" targetdb="$3"
  say "  PHP→目标 $db.$t"
  SSH_ASKPASS="$ASKPASS114" SSH_ASKPASS_REQUIRE=force DISPLAY=:0 ssh "$PHP_SSH_USER@$PHP_HOST" \
    "timeout 1800 docker run --rm -i --network host mysql:8.4 mysqldump -h127.0.0.1 -P3308 -uroot \
       -p'$PHP_DB_ROOT_PASS' --single-transaction --quick --set-gtid-purged=OFF \
       --default-character-set=utf8mb4 --skip-triggers --no-create-info --complete-insert \
       $db \`$t\` 2>/dev/null" \
  | "$MYSQL" -h"$TARGET_HOST" -P"$TARGET_PORT" -u"$TARGET_USER" -p"$TARGET_PASS" \
      --default-character-set=utf8mb4 --init-command="$TGT_INIT" "$targetdb" >>"$MAINLOG" 2>&1
}

say "===== 迁移开始 目标=$TARGET_DB / $GX_TARGET_DB ====="

# ① flyway_schema_history（同实例，最先）
say "① flyway_schema_history"
load_saas flyway_schema_history

# ② SaaS 独有 + A_sup + D（同实例 INSERT SELECT）
say "② SaaS 独有 / A_sup / D（同实例 INSERT SELECT）"
for grp in SAAS_ONLY A_sup D; do
  while read -r t; do
    [ -z "$t" ] && continue
    [ "$t" = "flyway_schema_history" ] && continue
    load_saas "$t"
  done < "$LISTDIR/tables_$grp.txt"
done
say "② 完成"

# ⑤a B 类 SaaS 侧先灌
say "⑤a B 类 SaaS 侧"
while read -r t; do [ -z "$t" ] && continue; load_saas "$t"; done < "$LISTDIR/tables_B.txt"
say "⑤a 完成"

# ③ PHP 独有 + A + C(PHP基) + B(PHP侧追加)：114 整批导出 → scp 内网直传 43 → 43 一次性导入
say "③ PHP 独有 / A / C基 / B-PHP侧（114 整批导出 → scp 直传 43 → 43 导入）"
PHP_TABLES=$(cat "$LISTDIR"/tables_PHP_ONLY.txt "$LISTDIR"/tables_A.txt "$LISTDIR"/tables_C.txt "$LISTDIR"/tables_B.txt | tr '\n' ' ' | xargs)
say "  PHP 侧表数：$(echo "$PHP_TABLES" | wc -w)"
say "  [③a] 114 整批导出中（555MB 级，耗时数分钟）"
SSH114 "mkdir -p /root/dbmerge_dump && timeout 2400 docker run --rm -i --network host -v /root/dbmerge_dump:/dump mysql:8.4 mysqldump \
  -h127.0.0.1 -P3308 -uroot -p'$PHP_DB_ROOT_PASS' --single-transaction --quick --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 --skip-triggers --no-create-info --complete-insert \
  --result-file=/dump/gjjddb_data.sql $PHP_DB $PHP_TABLES 2>/dev/null && \
  ls -la /root/dbmerge_dump/gjjddb_data.sql"
say "  [③b] scp 内网直传 43 中"
SSH114 "scp -i $SSH_KEY_114 -o StrictHostKeyChecking=accept-new /root/dbmerge_dump/gjjddb_data.sql root@$TGT_INTERNAL:/root/gjjddb_data.sql && ls -la /root/gjjddb_data.sql 2>/dev/null || true"
say "  [③c] 43 导入目标库中"
SSH43 "mysql -h127.0.0.1 -P$TARGET_PORT -u$TARGET_USER -p'$TARGET_PASS' --default-character-set=utf8mb4 --init-command=\"SET SESSION foreign_key_checks=0, sql_mode=''\" $TARGET_DB < /root/gjjddb_data.sql && rm -f /root/gjjddb_data.sql"
SSH114 "rm -f /root/dbmerge_dump/gjjddb_data.sql"
say "③ 完成"

# ④ C 类追加 SaaS 独有行（显式列，排除生成列）
say "④ C 类追加 SaaS 独有行"
while read -r t; do
  [ -z "$t" ] && continue
  cols="$(col_list "$t")"
  [ -z "$cols" ] && { say "  !! $t 无列清单，跳过"; continue; }
  # 给每列加 s. 前缀：先 prepend 再在逗号后补（cols 形如 `a`,`b`）
  scols="s.$(printf '%s' "$cols" | sed 's/,/,s./g')"
  run_target "INSERT INTO \`$TARGET_DB\`.\`$t\` ($cols) SELECT $scols FROM \`$SAAS_DB\`.\`$t\` s LEFT JOIN \`$TARGET_DB\`.\`$t\` d ON d.id=s.id WHERE d.id IS NULL"
  say "  C 追加 $t 完成"
done < "$LISTDIR/tables_C.txt"
say "④ 完成"

# ⑥ gxwebsoft_core 整库（同样直传链路）
say "⑥ gxwebsoft_core 整库"
SSH114 "mkdir -p /root/dbmerge_dump && timeout 1800 docker run --rm -i --network host -v /root/dbmerge_dump:/dump mysql:8.4 mysqldump -h127.0.0.1 -P3308 -uroot \
  -p'$PHP_DB_ROOT_PASS' --single-transaction --quick --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 --skip-triggers --no-create-info --complete-insert \
  --result-file=/dump/gx_data.sql $PHP_GX_DB 2>/dev/null && ls -la /root/dbmerge_dump/gx_data.sql"
SSH114 "scp -i $SSH_KEY_114 -o StrictHostKeyChecking=accept-new /root/dbmerge_dump/gx_data.sql root@$TGT_INTERNAL:/root/gx_data.sql"
SSH43 "mysql -h127.0.0.1 -P$TARGET_PORT -u$TARGET_USER -p'$TARGET_PASS' --default-character-set=utf8mb4 --init-command=\"SET SESSION foreign_key_checks=0, sql_mode=''\" $GX_TARGET_DB < /root/gx_data.sql && rm -f /root/gx_data.sql"
SSH114 "rm -f /root/dbmerge_dump/gx_data.sql"
say "⑥ 完成"

# ⑦ B/C 类 auto_increment 抬升
say "⑦ B/C auto_increment 抬升"
for grp in B C; do
  while read -r t; do
    [ -z "$t" ] && continue
    mx=$("$MYSQL" -h"$TARGET_HOST" -P"$TARGET_PORT" -u"$TARGET_USER" -p"$TARGET_PASS" -N -e \
      "SELECT COALESCE(MAX(id),0)+1 FROM \`$TARGET_DB\`.\`$t\`" 2>/dev/null)
    [ -z "$mx" ] && mx=1
    run_target "ALTER TABLE \`$TARGET_DB\`.\`$t\` AUTO_INCREMENT=$mx"
  done < "$LISTDIR/tables_$grp.txt"
done
say "⑦ 完成"

# ⑧ tenant_id 回填：并集补列后 PHP 侧行 tenant_id 为 NULL，统一回填 10159 让 SaaS 拦截器可见
say "⑧ tenant_id 回填（NULL→10159，仅目标库含 tenant_id 的表）"
run_target "SET SESSION sql_mode=''"
TENANT_TABLES=$("$MYSQL" -h"$TARGET_HOST" -P"$TARGET_PORT" -u"$TARGET_USER" -p"$TARGET_PASS" -N -e \
  "SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$TARGET_DB' AND COLUMN_NAME='tenant_id'" 2>/dev/null | tr -d '\r')
for t in $TENANT_TABLES; do
  t=$(printf '%s' "$t" | tr -d '\r')   # 去掉 Windows 输出的 \r
  [ -z "$t" ] && continue
  run_target "UPDATE \`$TARGET_DB\`.\`$t\` SET tenant_id=10159 WHERE tenant_id IS NULL"
done
say "⑧ 完成"

say "===== 迁移完成 ====="
