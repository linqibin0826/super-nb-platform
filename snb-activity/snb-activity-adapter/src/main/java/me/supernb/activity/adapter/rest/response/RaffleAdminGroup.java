package me.supernb.activity.adapter.rest.response;

import me.supernb.activity.domain.model.read.raffle.SubscriptionGroupView;

/// 管理端分组下拉项。id 转 String(实体 id 在 JSON 中一律字符串的仓库契约)。
public record RaffleAdminGroup(String id, String name) {

    /// 从读视图构造。
    public static RaffleAdminGroup of(SubscriptionGroupView v) {
        return new RaffleAdminGroup(String.valueOf(v.id()), v.name());
    }
}
