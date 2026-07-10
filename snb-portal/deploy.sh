#!/usr/bin/env bash
# 创作工坊零发版部署：build + rsync 静态产物（不碰任何生产容器运行态）
# ⚠️ 目标路径写死、不接受参数；--delete 只作用于 studio/ 目录本身
set -euo pipefail
cd "$(dirname "$0")"
# 子域名部署（studio.super-nb.me 站点根），资产 base 必须是 /
STUDIO_BASE=/ pnpm build
rsync -avz --delete dist/ bwg:/root/sub2api/deploy/caddy_config/web/studio/
echo "✅ 已同步。Caddy studio.super-nb.me 站点块未上线前公网不可见（见 ai-relay deployment/18）"
