#!/usr/bin/env bash
# raffle-admin 抽奖后台零发版部署:build + rsync 静态产物(不碰任何生产容器运行态)
# 🚨 生产红线:本脚本属发布动作,只能在站长明确点头后手动执行;目标一律 bwg
#
# 🪦 2026-07-30 补建。此前本站**只有构建目标、没有发布脚本**,历史上是手工发的——
#    结果是每次「portal 三站」批量发版都会把它漏掉(它和另外三站吃同一套 --snb-* 令牌,
#    漏发就会单独停在旧令牌上)。这次改深色控件描边时才发现线上还有它。
#    以后一律四站:deploy.sh / deploy-hub.sh / deploy-invoice.sh / 本脚本。
set -euo pipefail
cd "$(dirname "$0")"
pnpm build:raffle-admin
rsync -avz --delete dist-raffle-admin/ bwg:/root/sub2api/deploy/caddy_config/web/raffle-admin/
echo "✅ raffle-admin 已同步 bwg → https://api.super-nb.me/raffle-admin/"
