#!/usr/bin/env bash
# ops-admin 资产运维台零发版部署:build + rsync 静态产物(不碰任何生产容器运行态)
# 🚨 生产红线:本脚本属发布动作,只能在站长明确点头后手动执行
# ⚠️ 目标 vultr(2026-08-08 起控制台/子站全在 vultr;deploy-raffle-admin.sh 里的 bwg 是迁移前旧值,
#    执行前先 `ssh vultr ls /root/sub2api/deploy/caddy_config/web/` 核实 raffle-admin 是否真在此路径,
#    以线上实际为准再改本脚本目标)
set -euo pipefail
cd "$(dirname "$0")"
pnpm build:ops-admin
rsync -avz --delete dist-ops-admin/ vultr:/root/sub2api/deploy/caddy_config/web/ops-admin/
echo "✅ ops-admin 已同步 vultr → https://api.super-nb.me/ops-admin/"
