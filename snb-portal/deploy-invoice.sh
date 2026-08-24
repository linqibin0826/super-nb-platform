#!/usr/bin/env bash
# invoice 发票中心零发版部署:build + rsync 静态产物(不碰任何生产容器运行态)
# 🚨 生产红线:本脚本属发布动作,只能在站长明确点头后手动执行
# ⚠️ 目标 vultr(2026-08-08 起控制台/子站全在 vultr;本脚本此前写的 bwg 是迁移前旧值,
#    2026-08-24 核实 bwg 该目录已不存在才发现并修正)
set -euo pipefail
cd "$(dirname "$0")"
pnpm build:invoice
rsync -avz --delete dist-invoice/ vultr:/root/sub2api/deploy/caddy_config/web/invoice/
echo "✅ invoice 前端已同步 vultr。Caddy invoice.super-nb.me 站点块未上线前公网不可见(runbook 32)"
