#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/docker/docker-compose.platform-api-dev.yml"
LOG_DIR="$ROOT_DIR/outputs/logs"
BACKEND_DIR="$ROOT_DIR/services/platform-api"
FRONTEND_DIR="$ROOT_DIR/apps/admin-web"

BACKEND_PORT_WAS_SET="${SERVER_PORT+x}"
FRONTEND_PORT_WAS_SET="${FRONTEND_PORT+x}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_PORT="${REDIS_PORT:-6379}"
BACKEND_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

mkdir -p "$LOG_DIR"

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

log() {
  printf '[%s] %s\n' "$(timestamp)" "$1"
}

has_command() {
  command -v "$1" >/dev/null 2>&1
}

port_open() {
  nc -z 127.0.0.1 "$1" >/dev/null 2>&1
}

platform_api_healthy() {
  curl -fsS "http://127.0.0.1:$BACKEND_PORT/api/actuator/health" 2>/dev/null | grep -q '"status":"UP"'
}

admin_web_serving() {
  curl -fsS "http://127.0.0.1:$FRONTEND_PORT" 2>/dev/null | grep -q '千铝国昱云平台'
}

find_available_port() {
  local candidate="$1"
  while port_open "$candidate"; do
    candidate=$((candidate + 1))
  done
  printf '%s' "$candidate"
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local timeout_seconds="${3:-90}"
  local elapsed=0

  while ! port_open "$port"; do
    if (( elapsed >= timeout_seconds )); then
      log "$name 未在 ${timeout_seconds}s 内就绪，请查看日志或 Docker 状态。"
      return 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  log "$name 已就绪，端口：$port"
}

wait_for_platform_api() {
  local timeout_seconds="${1:-180}"
  local elapsed=0

  while ! platform_api_healthy; do
    if (( elapsed >= timeout_seconds )); then
      log "platform-api 未在 ${timeout_seconds}s 内通过健康检查，请查看日志：$LOG_DIR/platform-api.log"
      return 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  log "platform-api 健康检查通过。"
}

require_command() {
  local cmd="$1"
  local tip="$2"
  if ! has_command "$cmd"; then
    log "缺少命令：$cmd。$tip"
    exit 1
  fi
}

start_docker_services_if_needed() {
  local services=()

  if port_open "$MYSQL_PORT"; then
    log "检测到 MySQL 端口 $MYSQL_PORT 已有服务，占用即视为已启动，跳过本项目 MySQL 容器。"
  else
    services+=("mysql")
  fi

  if port_open "$REDIS_PORT"; then
    log "检测到 Redis 端口 $REDIS_PORT 已有服务，占用即视为已启动，跳过本项目 Redis 容器。"
  else
    services+=("redis")
  fi

  if (( ${#services[@]} == 0 )); then
    return 0
  fi

  require_command docker "请先安装并启动 Docker Desktop，或手动启动 MySQL/Redis。"

  if ! docker info >/dev/null 2>&1; then
    log "Docker 当前不可用。请先打开 Docker Desktop，或手动启动 MySQL/Redis 后再运行本脚本。"
    exit 1
  fi

  log "启动开发依赖容器：${services[*]}"
  docker compose -f "$COMPOSE_FILE" up -d "${services[@]}"

  if [[ " ${services[*]} " == *" mysql "* ]]; then
    wait_for_port "$MYSQL_PORT" "MySQL"
  fi
  if [[ " ${services[*]} " == *" redis "* ]]; then
    wait_for_port "$REDIS_PORT" "Redis"
  fi
}

start_backend() {
  if port_open "$BACKEND_PORT"; then
    if platform_api_healthy; then
      log "检测到 platform-api 已在端口 $BACKEND_PORT 运行，跳过后端启动。"
      return 0
    fi

    if [[ -n "$BACKEND_PORT_WAS_SET" ]]; then
      log "后端端口 $BACKEND_PORT 已被其他服务占用，且不是本项目 platform-api。"
      log "请先停止占用 $BACKEND_PORT 的服务，或换一个 SERVER_PORT。"
      exit 1
    fi

    local fallback_port
    fallback_port="$(find_available_port 18080)"
    log "后端默认端口 $BACKEND_PORT 被其他服务占用，自动改用 $fallback_port。"
    BACKEND_PORT="$fallback_port"
  fi

  require_command java "platform-api 需要 Java 21。"

  local backend_log="$LOG_DIR/platform-api.log"
  log "启动后端 platform-api，日志：$backend_log"

  (
    cd "$BACKEND_DIR"
    SERVER_PORT="$BACKEND_PORT" ./mvnw spring-boot:run
  ) >"$backend_log" 2>&1 &
  BACKEND_PID=$!

  wait_for_platform_api 180
}

start_frontend() {
  if port_open "$FRONTEND_PORT"; then
    if admin_web_serving && [[ "$BACKEND_PORT" == "8080" ]]; then
      log "检测到 admin-web 已在端口 $FRONTEND_PORT 运行，跳过前端启动。"
      return 0
    fi

    if [[ -n "$FRONTEND_PORT_WAS_SET" ]]; then
      log "前端端口 $FRONTEND_PORT 已被其他服务占用。请先停止该服务，或换一个 FRONTEND_PORT。"
      exit 1
    fi

    local fallback_port
    fallback_port="$(find_available_port 15173)"
    log "前端默认端口 $FRONTEND_PORT 被其他服务占用，自动改用 $fallback_port。"
    FRONTEND_PORT="$fallback_port"
  fi

  require_command npm "admin-web 需要 Node.js 和 npm。"

  local frontend_log="$LOG_DIR/admin-web.log"
  log "启动前端 admin-web，日志：$frontend_log"

  (
    cd "$FRONTEND_DIR"
    if [[ ! -d node_modules ]]; then
      npm install
    fi
    VITE_API_PROXY_TARGET="http://localhost:$BACKEND_PORT" npm run dev -- --port "$FRONTEND_PORT" --strictPort
  ) >"$frontend_log" 2>&1 &
  FRONTEND_PID=$!

  wait_for_port "$FRONTEND_PORT" "admin-web" 90
}

cleanup() {
  log "收到退出信号，停止本脚本启动的前后端进程。数据库容器会保留运行。"
  if [[ -n "${BACKEND_PID:-}" ]]; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "${FRONTEND_PID:-}" ]]; then
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup INT TERM

log "千铝国昱云平台开发环境启动"
log "项目目录：$ROOT_DIR"

start_docker_services_if_needed
start_backend
start_frontend

log "启动完成。管理端地址：http://localhost:$FRONTEND_PORT"
log "后端健康检查：http://localhost:$BACKEND_PORT/api/actuator/health"
log "演示账号：tenantCode=demo username=admin password=Admin@123456"

if has_command open; then
  open "http://localhost:$FRONTEND_PORT"
fi

log "保持此窗口打开即可保持本脚本启动的前后端运行；按 Ctrl+C 停止前后端。"

while true; do
  sleep 3600
done
