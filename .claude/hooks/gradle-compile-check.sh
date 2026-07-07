#!/bin/bash

# gradle-compile-check.sh
# 目的：会话停止时对全仓做快速 Gradle 编译检查（增量，主源 + 测试源）
# 触发器：Stop 事件（仅当本会话修改过 Java 文件时才真正编译）

set -eo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
cd "$PROJECT_ROOT" || exit 1

# 本会话没改过 Java 文件就跳过
MARKER_FILE="$PROJECT_ROOT/.claude/hooks/.java-files-modified"
if [ ! -f "$MARKER_FILE" ]; then
    exit 0
fi
rm -f "$MARKER_FILE"

if [ ! -f "build.gradle.kts" ] && [ ! -f "settings.gradle.kts" ]; then
    echo -e "${RED}❌ 项目根目录未找到 Gradle 构建文件${NC}"
    exit 1
fi

echo "🔨 运行 Gradle 编译检查: ./gradlew classes testClasses -q"

TEMP_OUTPUT=$(mktemp)
trap 'rm -f "$TEMP_OUTPUT"' EXIT

if ./gradlew classes testClasses --parallel -q 2>&1 | tee "$TEMP_OUTPUT"; then
    echo -e "${GREEN}✅ Gradle 编译成功${NC}"
    exit 0
else
    echo -e "${RED}❌ Gradle 编译失败${NC}"
    echo ""
    echo "=== 编译错误摘要 ==="
    grep -E "error:|cannot find symbol|package .* does not exist|FAILED" "$TEMP_OUTPUT" | head -20
    echo ""
    echo "=== 失败的任务 ==="
    grep -E ":.*FAILED" "$TEMP_OUTPUT" | sed 's/.*\(:[^ ]*\).*/  - \1/' | sort -u
    echo ""
    echo -e "${YELLOW}💡 运行 './gradlew classes testClasses --info' 获取详细错误${NC}"
    exit 1
fi
