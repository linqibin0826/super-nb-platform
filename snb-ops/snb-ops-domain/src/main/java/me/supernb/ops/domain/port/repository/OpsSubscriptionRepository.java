package me.supernb.ops.domain.port.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.model.SubTier;

/// 服务开通/订阅持久化端口。每「邮箱×服务」一行(库有 UNIQUE 兜底)。
public interface OpsSubscriptionRepository {

    /// 订阅全量数据(注册→付费→封号→退款一行装下;可空字段一律允许 null)。
    record SubscriptionData(long accountId, SubService service, SubTier tier, String region,
                            String cardPlatform, String cardLast4, String registerIp, String currentIp,
                            Instant registeredAt, LocalDate startedAt, LocalDate nextBillingAt,
                            BigDecimal priceUsd, SubStatus status,
                            Long sub2apiAccountId, String sub2apiAccountName,
                            Instant bannedAt, Boolean bannedWhilePaid,
                            RefundStatus refundStatus, BigDecimal refundAmountUsd, LocalDate appealedAt,
                            LocalDate refundResolvedAt, LocalDate refundFollowUpAt, String refundNotes,
                            String notes) {
    }

    /// 库中订阅行 = id + 数据 + 时间戳。
    record SubscriptionRow(long id, SubscriptionData data, Instant createdAt, Instant updatedAt) {
    }

    long create(SubscriptionData data);

    boolean update(long id, SubscriptionData data);

    boolean delete(long id);

    Optional<SubscriptionRow> find(long id);

    List<SubscriptionRow> listByAccount(long accountId);

    List<SubscriptionRow> listAll();

    int countByAccount(long accountId);
}
