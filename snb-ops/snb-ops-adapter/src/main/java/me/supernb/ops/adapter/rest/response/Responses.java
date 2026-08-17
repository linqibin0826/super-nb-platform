package me.supernb.ops.adapter.rest.response;

import java.time.Instant;
import java.util.List;
import me.supernb.ops.app.usecase.query.view.AccountView;
import me.supernb.ops.app.usecase.query.view.DashboardView;
import me.supernb.ops.app.usecase.query.view.SecretView;
import me.supernb.ops.app.usecase.query.view.SubscriptionView;

/// /ops/v1/admin 响应形状(全部 record;实体 id 一律字符串;日期/金额到前端统一字符串)。
public final class Responses {

    private Responses() {
    }

    public record IdResponse(String id) {
    }

    /// 账号行(密码只报有无,密文/明文都不出现)。
    public record AccountResponse(String id, String email, String provider, boolean hasPassword,
                                  String recoveryEmail, boolean hasRecoveryPassword, String regYear,
                                  String country, String owner, String status, String source, String notes,
                                  Instant createdAt) {

        public static AccountResponse of(AccountView v) {
            return new AccountResponse(String.valueOf(v.id()), v.email(), v.provider(), v.hasPassword(),
                    v.recoveryEmail(), v.hasRecoveryPassword(), v.regYear(), v.country(), v.owner(),
                    v.status().name(), v.source(), v.notes(), v.createdAt());
        }
    }

    /// 解密结果(仅「显示密码」端点)。
    public record SecretResponse(String password, String recoveryPassword) {

        public static SecretResponse of(SecretView v) {
            return new SecretResponse(v.password(), v.recoveryPassword());
        }
    }

    /// 订阅行(data 摊平;枚举 name、日期 toString、金额 toPlainString,null 原样)。
    public record SubscriptionResponse(String id, String accountId, String email, String service, String tier,
                                       String region, String cardPlatform, String cardLast4, String registerIp,
                                       String currentIp, String registeredAt, String startedAt,
                                       String nextBillingAt, String priceUsd, String status,
                                       String sub2apiAccountId, String sub2apiAccountName, String bannedAt,
                                       Boolean bannedWhilePaid, String refundStatus, String refundAmountUsd,
                                       String appealedAt, String refundResolvedAt, String refundFollowUpAt,
                                       String refundNotes, String notes) {

        public static SubscriptionResponse of(SubscriptionView v) {
            var d = v.data();
            return new SubscriptionResponse(String.valueOf(v.id()), String.valueOf(v.accountId()), v.email(),
                    d.service().name(), d.tier() == null ? null : d.tier().name(), d.region(),
                    d.cardPlatform(), d.cardLast4(), d.registerIp(), d.currentIp(),
                    d.registeredAt() == null ? null : d.registeredAt().toString(),
                    d.startedAt() == null ? null : d.startedAt().toString(),
                    d.nextBillingAt() == null ? null : d.nextBillingAt().toString(),
                    d.priceUsd() == null ? null : d.priceUsd().toPlainString(), d.status().name(),
                    d.sub2apiAccountId() == null ? null : String.valueOf(d.sub2apiAccountId()),
                    d.sub2apiAccountName(), d.bannedAt() == null ? null : d.bannedAt().toString(),
                    d.bannedWhilePaid(), d.refundStatus().name(),
                    d.refundAmountUsd() == null ? null : d.refundAmountUsd().toPlainString(),
                    d.appealedAt() == null ? null : d.appealedAt().toString(),
                    d.refundResolvedAt() == null ? null : d.refundResolvedAt().toString(),
                    d.refundFollowUpAt() == null ? null : d.refundFollowUpAt().toString(),
                    d.refundNotes(), d.notes());
        }
    }

    /// 看板待办。
    public record DashboardResponse(List<SubscriptionResponse> upcomingBilling,
                                    List<SubscriptionResponse> refundFollowUps, long bannedOpenCount) {

        public static DashboardResponse of(DashboardView v) {
            return new DashboardResponse(v.upcomingBilling().stream().map(SubscriptionResponse::of).toList(),
                    v.refundFollowUps().stream().map(SubscriptionResponse::of).toList(), v.bannedOpenCount());
        }
    }
}
