#!/usr/bin/env bash
# 从公开 patra 仓库 @ patraRef 构建 linqibin-commons 并 publishToMavenLocal。
# CI 与开源外部构建者共用。需要 JDK 25 + git。
# 本地开发若已 publishToMavenLocal 过 commons,可跳过本脚本。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PATRA_REF="$(grep '^patraRef=' "$SCRIPT_DIR/gradle.properties" | cut -d= -f2)"
WORK="${PATRA_WORKDIR:-/tmp/patra-src}"

if [ ! -d "$WORK/.git" ]; then
  git clone https://github.com/linqibin0826/patra.git "$WORK"
fi
git -C "$WORK" fetch origin "$PATRA_REF"
git -C "$WORK" checkout -q "$PATRA_REF"

( cd "$WORK" && ./gradlew --no-configuration-cache \
    :linqibin-commons:linqibin-commons-core:publishToMavenLocal \
    :linqibin-commons:linqibin-spring-boot-starter-core:publishToMavenLocal \
    :linqibin-commons:linqibin-spring-boot-starter-web:publishToMavenLocal \
    :linqibin-commons:linqibin-spring-boot-starter-jpa:publishToMavenLocal \
    :linqibin-commons:linqibin-spring-boot-starter-test:publishToMavenLocal )

echo "commons 已从 patra@$PATRA_REF publishToMavenLocal"
