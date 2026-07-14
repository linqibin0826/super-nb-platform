package me.supernb.activity.domain.model.achievement;

import java.util.Map;
import java.util.Set;

/// status 单向状态机(深化稿不可逆决策清单第 5 条):draft→active→retired,retired 无复活路径,
/// 不允许跳级(draft 不能直接到 retired)。纯函数,当前无调用方——为未来运维后台的状态
/// 转换端点预留,不是超前设计(本计划任务总览已承诺交付)。
public final class AchievementStatusTransition {

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "draft", Set.of("draft", "active"),
            "active", Set.of("active", "retired"),
            "retired", Set.of("retired"));

    private AchievementStatusTransition() {
    }

    /// from→to 是否为合法的单向前进(含同状态原地不动,视为幂等合法)。
    public static boolean isValidForward(String from, String to) {
        Set<String> allowedTargets = ALLOWED.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }
}
