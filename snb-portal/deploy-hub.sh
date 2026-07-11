#!/usr/bin/env bash
# hub 内容中心零发版部署：build + rsync 静态产物（不碰任何生产容器运行态）
# ⚠️ 目标一律 bwg；--delete 只作用于 web/hub/——电子书在 web/hub-books/ 由发布管线维护，绝不混放
set -euo pipefail
cd "$(dirname "$0")"
pnpm build:hub
rsync -avz --delete dist-hub/ bwg:/root/sub2api/deploy/caddy_config/web/hub/
echo "✅ hub 前端已同步 bwg。Caddy hub.super-nb.me 站点块未上线前公网不可见（设计稿 §11）"
