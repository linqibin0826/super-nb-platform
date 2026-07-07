package me.supernb.activity.app;

import java.math.BigDecimal;
import java.time.Instant;

/// 活动上下文应用层视图 DTO 汇总(对外契约的中间形态,adapter 再转 JSON)。
public final class ActivityDto {

    private ActivityDto() {
    }

    /// 抽奖资格状态。
    public record DrawStatus(boolean eligible, int remaining) {
    }

    /// 榜单条目(name 已脱敏)。
    public record LeaderEntry(String name, BigDecimal amount) {
    }

    /// 充值动态条目(name 已脱敏)。
    public record RechargeEntry(String name, BigDecimal amount, Instant at) {
    }

    /// 奖池档位余量。
    public record PoolTier(BigDecimal amount, int total, int available) {
    }

    /// 兑换码状态(app 层形态)。
    public record CodeStatus(String status, Instant expiresAt) {
    }

    /// 活动库里的一条原始抽奖记录(未 enrich)。
    public record RawDraw(BigDecimal amount, String redeemCode, boolean consolation, Instant createdAt) {
    }

    /// 一条真实中奖记录(未 enrich,仅 userId+金额)。
    public record RawWinner(long userId, BigDecimal amount) {
    }

    /// 我的中奖记录(已 enrich 兑换码状态,面向本人不脱敏)。
    public record MyDrawView(
            BigDecimal amount,
            String redeemCode,
            boolean consolation,
            Instant createdAt,
            String codeStatus,
            Instant expiresAt) {
    }

    /// 公开中奖信息流条目(name 已脱敏)。
    public record PublicDraw(String name, BigDecimal amount) {
    }
}
