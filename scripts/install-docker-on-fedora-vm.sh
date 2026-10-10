#!/usr/bin/env bash
# 在 VMware「Ubuntu」虚拟机（实际为 Fedora Server）内以 root 执行：
#   bash install-docker-on-fedora-vm.sh
# 或直接整段粘贴到 root shell。
set -euo pipefail

if [[ "$(id -u)" -ne 0 ]]; then
  echo "请用 root 执行（当前用户: $(whoami)）"
  exit 1
fi

echo "==> 系统: $(. /etc/os-release; echo "$PRETTY_NAME")"
echo "==> 内核: $(uname -r)"

# 若已有可用 docker，直接校验退出
if command -v docker >/dev/null 2>&1; then
  if systemctl is-active --quiet docker 2>/dev/null || docker info >/dev/null 2>&1; then
    echo "==> Docker 已可用:"
    docker version --format 'Client={{.Client.Version}} Server={{.Server.Version}}' 2>/dev/null || docker version
    exit 0
  fi
fi

dnf -y update --refresh || true
dnf -y install dnf-plugins-core curl ca-certificates

install_from_docker_ce() {
  local ver major
  ver="$(. /etc/os-release; echo "$VERSION_ID")"
  major="${ver%%.*}"
  echo "==> 尝试 Docker CE 官方源 (fedora/${major})"
  rm -f /etc/yum.repos.d/docker-ce.repo
  # Fedora 过新时官方可能尚无对应目录，依次回退几个大版本
  local try
  for try in "$major" $((major - 1)) $((major - 2)) 42 41 40; do
    [[ "$try" -lt 40 ]] && continue
    echo "    尝试 repo: fedora/${try}"
    if curl -fsSL "https://download.docker.com/linux/fedora/docker-ce.repo" -o /etc/yum.repos.d/docker-ce.repo; then
      # 把 $releasever 固定到 try，避免 44 无包
      sed -i "s/\$releasever/${try}/g" /etc/yum.repos.d/docker-ce.repo
      if dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin; then
        return 0
      fi
      echo "    fedora/${try} 安装失败，继续回退…"
      dnf -y remove docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin 2>/dev/null || true
    fi
  done
  return 1
}

install_from_fedora_moby() {
  echo "==> 回退：安装 Fedora 仓库 moby/docker"
  # 不同发行版包名略有差异，按序尝试
  if dnf -y install moby-engine docker-compose; then
    return 0
  fi
  if dnf -y install docker docker-compose; then
    return 0
  fi
  return 1
}

if ! install_from_docker_ce; then
  echo "==> Docker CE 不可用，改用发行版包"
  install_from_fedora_moby
fi

systemctl enable --now docker
# 部分 moby 包服务名仍是 docker
sleep 1
systemctl --no-pager --full status docker | head -n 20 || true

echo "==> 验证"
docker version
docker compose version 2>/dev/null || docker-compose version 2>/dev/null || echo "(compose 插件可选，仅跑 OnlyOffice 单容器可不强求)"

echo
echo "Docker 安装完成。"
echo "本机 IP 可执行: ip -4 addr show | grep -E 'inet .* (ens|eth|enp)'"
echo "下一步再装 OnlyOffice（宿主机浏览器访问 http://<虚拟机IP>:8088）。"
