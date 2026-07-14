package me.supernb.activity.domain.port.achievement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;

/// 成就目录只读端口(运维/发版才新增/退役行,应用代码只读)。
public interface AchievementCatalogPort {

    /// 全部 active 状态的目录条目(判定引擎/成就墙查询共用,量级 42~56 条一次性全量取)。
    List<AchievementDefinition> activeDefinitions();

    /// 全部状态(active/draft/retired)的目录条目——成就墙展示用:draft 置灰"待启用",
    /// retired 既得者永久保留展示,墙本身就是"全站地图",不只展示已启用的。
    List<AchievementDefinition> allDefinitions();

    /// 按 code 精确取一条,不限状态(供已解锁历史条目回显——即便已 retired 也要能查到展示信息)。
    Optional<AchievementDefinition> byCode(String code);

    /// 系列 code→人话标题(achievement_series_label,内容表,详见 V10 迁移注释)。
    Map<String, String> allSeriesLabels();
}
