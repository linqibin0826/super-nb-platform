package me.supernb.ops.app.usecase.query.view;

import java.time.Instant;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionRow;

/// 订阅读视图 = 行 + 冗余 email 展示列(订阅本身无密文,data 原样携带)。
public record SubscriptionView(long id, long accountId, String email, SubscriptionData data, Instant createdAt) {

    /// 从仓储行装配,email 由调用方按 accountId 补齐(查不到给 "?")。
    public static SubscriptionView of(SubscriptionRow row, String email) {
        return new SubscriptionView(row.id(), row.data().accountId(), email, row.data(), row.createdAt());
    }
}
