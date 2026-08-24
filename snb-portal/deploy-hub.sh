#!/usr/bin/env bash
# hub 内容中心零发版部署：build + rsync 静态产物（不碰任何生产容器运行态）
# ⚠️ 目标 vultr（2026-08-08 起控制台/子站全在 vultr；本脚本此前写的 bwg 是迁移前旧值，
#    2026-08-24 核实 bwg 该目录已不存在才发现并修正）；--delete 只作用于 web/hub/——
#    电子书在 web/hub-books/ 由发布管线维护，绝不混放
set -euo pipefail
cd "$(dirname "$0")"
pnpm build:hub
rsync -avz --delete dist-hub/ vultr:/root/sub2api/deploy/caddy_config/web/hub/
echo "✅ hub 前端已同步 vultr。Caddy hub.super-nb.me 站点块未上线前公网不可见（设计稿 §11）"
