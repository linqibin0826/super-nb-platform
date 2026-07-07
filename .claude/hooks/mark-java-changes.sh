#!/bin/bash

# mark-java-changes.sh
# 目的：标记本会话通过 Edit/Write/MultiEdit 修改过 Java 文件
# 触发器：PostToolUse（Edit|Write|MultiEdit 之后）
# 用法：落一个标志文件，gradle-compile-check.sh 在 Stop 时据此决定是否编译

# 从工具输入的 stdin 中读取文件路径
FILE_PATH=$(cat | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# 只关心 Java 文件
if [[ "$FILE_PATH" == *.java ]]; then
    PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
    mkdir -p "$PROJECT_ROOT/.claude/hooks"
    touch "$PROJECT_ROOT/.claude/hooks/.java-files-modified"
fi

# 非阻塞 hook：始终成功退出
exit 0
