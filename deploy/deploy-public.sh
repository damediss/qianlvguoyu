#!/usr/bin/env bash
# =============================================================================
# 千铝国昱云平台 / 公网测试环境 一键部署脚本
# 目标: Ubuntu 22.04+ (root 或带 sudo 权限的用户)
# 用法:
#   本地:  bash deploy/deploy-public.sh
#   远端:  scp -r deploy/ ubuntu@<IP>:/tmp/guoyu-deploy && ssh ubuntu@<IP> 'cd /tmp/guoyu-deploy && sudo bash deploy-public.sh'
# =============================================================================

set -euo pipefail

# ---------- 颜色 ----------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[+]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
err()  { echo -e "${RED}[x]${NC} $*" >&2; }
hr()   { echo -e "${BLUE}============================================================${NC}"; }

DEPLOY_DIR=/data/guoyu
GUOYU_USER=guoyu
SERVICE_NAME=platform-api

# ---------- 前置检查 ----------
hr
log "公网测试环境部署脚本"
hr

if [ "$(id -u)" -ne 0 ]; then
  err "请用 root 或 sudo 执行: sudo bash $0"
  exit 1
fi

# ---------- 1. 安装基础依赖 ----------
hr
log "[1/7] 安装 JDK 21 / Maven / Nginx / MySQL 8 / Redis / git / unzip"
apt-get update -qq
apt-get install -y --no-install-recommends \
  openjdk-21-jdk-headless \
  maven \
  nginx \
  mysql-server-8.0 \
  redis-server \
  git \
  unzip \
  curl \
  python3-pip

# ---------- 2. 创建部署用户 ----------
hr
log "[2/7] 创建部署用户 $GUOYU_USER"
if ! id "$GUOYU_USER" &>/dev/null; then
  useradd --system --home "$DEPLOY_DIR" --shell /bin/bash "$GUOYU_USER"
fi
mkdir -p "$DEPLOY_DIR"/{storage/files,web,logs}
chown -R "$GUOYU_USER:$GUOYU_USER" "$DEPLOY_DIR"

# ---------- 3. MySQL 初始化 ----------
hr
log "[3/7] 配置 MySQL 8"
systemctl enable --now mysql
# 仅在首次运行时创建库和应用账号
if ! mysql -e "SHOW DATABASES LIKE 'gjjd_test'" | grep -q gjjd_test; then
  warn "创建数据库 gjjd_test 和应用账号 guoyu_app"
  MYSQL_APP_PWD="${MYSQL_APP_PWD:-$(openssl rand -hex 12)}"
  mysql <<SQL
CREATE DATABASE gjjd_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'guoyu_app'@'localhost' IDENTIFIED BY '${MYSQL_APP_PWD}';
GRANT ALL PRIVILEGES ON gjjd_test.* TO 'guoyu_app'@'localhost';
FLUSH PRIVILEGES;
SQL
  warn "已生成 MySQL 应用密码: ${MYSQL_APP_PWD}  (请记下,稍后填入 .env)"
  echo "${MYSQL_APP_PWD}" > "$DEPLOY_DIR/.mysql_app_pwd"
  chmod 600 "$DEPLOY_DIR/.mysql_app_pwd"
else
  warn "数据库 gjjd_test 已存在,跳过创建"
fi

# ---------- 4. Redis 初始化 ----------
hr
log "[4/7] 配置 Redis"
systemctl enable --now redis-server
# 设置 Redis 密码
REDIS_PWD="${REDIS_PWD:-$(openssl rand -hex 12)}"
if ! grep -q "^requirepass" /etc/redis/redis.conf; then
  echo "requirepass $REDIS_PWD" >> /etc/redis/redis.conf
  systemctl restart redis-server
fi
echo "$REDIS_PWD" > "$DEPLOY_DIR/.redis_pwd"
chmod 600 "$DEPLOY_DIR/.redis_pwd"
warn "Redis 密码: ${REDIS_PWD}  (记下,稍后填入 .env)"

# ---------- 5. 拉取代码 / 构建 ----------
hr
log "[5/7] 构建 platform-api jar + 前端 dist"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
cd "$REPO_DIR"

log "  - mvn package (platform-api)..."
mvn -B -DskipTests -f services/platform-api/pom.xml \
  -s scripts/custom-maven-settings.xml package
cp services/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar "$DEPLOY_DIR/"
chown "$GUOYU_USER:$GUOYU_USER" "$DEPLOY_DIR/platform-api-0.1.0-SNAPSHOT.jar"

log "  - npm run build (admin-web)..."
cd apps/admin-web
[ -d node_modules ] || npm ci
npm run build
cp -r dist/* "$DEPLOY_DIR/web/"
chown -R "$GUOYU_USER:$GUOYU_USER" "$DEPLOY_DIR/web"
cd "$REPO_DIR"

# ---------- 6. 配置 .env ----------
hr
log "[6/7] 写入 /data/guoyu/.env"
if [ ! -f "$DEPLOY_DIR/.env" ]; then
  cp deploy/.env.example "$DEPLOY_DIR/.env"
  JWT_RANDOM=$(openssl rand -hex 32)
  MYSQL_ACTUAL_PWD=$(cat "$DEPLOY_DIR/.mysql_app_pwd" 2>/dev/null || echo "CHANGE_ME")
  REDIS_ACTUAL_PWD=$(cat "$DEPLOY_DIR/.redis_pwd" 2>/dev/null || echo "CHANGE_ME")
  PUBLIC_IP=$(curl -s --max-time 5 https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')
  sed -i "s|MYSQL_PASSWORD=CHANGE_ME.*|MYSQL_PASSWORD=${MYSQL_ACTUAL_PWD}|" "$DEPLOY_DIR/.env"
  sed -i "s|REDIS_PASSWORD=CHANGE_ME.*|REDIS_PASSWORD=${REDIS_ACTUAL_PWD}|" "$DEPLOY_DIR/.env"
  sed -i "s|JWT_SECRET=CHANGE_ME.*|JWT_SECRET=${JWT_RANDOM}|" "$DEPLOY_DIR/.env"
  sed -i "s|CORS_ALLOWED_ORIGINS=.*|CORS_ALLOWED_ORIGINS=http://${PUBLIC_IP}|" "$DEPLOY_DIR/.env"
  warn "已生成 .env,公网 IP=${PUBLIC_IP}。COS 凭证已预填,部署后请用 sudo systemctl restart platform-api 让配置生效。"
fi
chown "$GUOYU_USER:$GUOYU_USER" "$DEPLOY_DIR/.env"
chmod 600 "$DEPLOY_DIR/.env"

# ---------- 7. systemd + nginx ----------
hr
log "[7/7] 部署 systemd unit + nginx 站点"
cp deploy/platform-api.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now "$SERVICE_NAME"

cp deploy/nginx.conf /etc/nginx/sites-available/guoyu
ln -sf /etc/nginx/sites-available/guoyu /etc/nginx/sites-enabled/guoyu
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

# ---------- 验收 ----------
hr
log "等待 30s 让后端启动..."
sleep 30
PUBLIC_IP=$(curl -s --max-time 5 https://api.ipify.org 2>/dev/null || hostname -I | awk '{print $1}')
log "冒烟测试:"
echo "  1) nginx health:    curl -s http://$PUBLIC_IP/healthz"
echo "  2) 后端 health:     curl -s http://$PUBLIC_IP/api/actuator/health"
echo "  3) 前端首页:        curl -sI http://$PUBLIC_IP/"

curl -s -o /dev/null -w "  nginx health    → HTTP %{http_code}\n" http://127.0.0.1/healthz
curl -s -o /dev/null -w "  backend health  → HTTP %{http_code}\n" http://127.0.0.1:8080/api/actuator/health
curl -s -o /dev/null -w "  frontend index  → HTTP %{http_code}\n" http://127.0.0.1/

hr
log "部署完成!"
log "前端访问:  http://$PUBLIC_IP"
log "默认账号:  tenant=demo, user=admin, password=Admin@123456"
log ""
log "常用命令:"
log "  - 查看后端日志:  sudo journalctl -u platform-api -f"
log "  - 查看 nginx:     sudo tail -f /var/log/nginx/guoyu.error.log"
log "  - 重启后端:      sudo systemctl restart platform-api"
log "  - 重新加载 nginx:sudo nginx -t && sudo systemctl reload nginx"
log "  - 修改 .env:     sudo nano /data/guoyu/.env && sudo systemctl restart platform-api"
hr
