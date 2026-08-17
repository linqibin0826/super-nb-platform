package me.supernb.ops.adapter.rest.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.model.SubTier;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;

/// 订阅入参:字段全 String,集中解析(数字/日期/枚举非法 → 422 带字段名);必填校验在 app 层。
public record SubscriptionInput(String accountId, String service, String tier, String region,
                                String cardPlatform, String cardLast4, String registerIp, String currentIp,
                                String registeredAt, String startedAt, String nextBillingAt, String priceUsd,
                                String status, String sub2apiAccountId, String sub2apiAccountName,
                                String bannedAt, Boolean bannedWhilePaid, String refundStatus,
                                String refundAmountUsd, String appealedAt, String refundResolvedAt,
                                String refundFollowUpAt, String refundNotes, String notes) {

    /// 转端口数据形状。
    public SubscriptionData toData() {
        return new SubscriptionData(requiredLong(accountId, "accountId"),
                parseEnum(SubService.class, service, "service"), parseEnum(SubTier.class, tier, "tier"),
                region, cardPlatform, cardLast4, registerIp, currentIp,
                parseInstant(registeredAt, "registeredAt"), parseDate(startedAt, "startedAt"),
                parseDate(nextBillingAt, "nextBillingAt"), parseDecimal(priceUsd, "priceUsd"),
                parseEnum(SubStatus.class, status, "status"), parseLong(sub2apiAccountId, "sub2apiAccountId"),
                sub2apiAccountName, parseInstant(bannedAt, "bannedAt"), bannedWhilePaid,
                parseEnum(RefundStatus.class, refundStatus, "refundStatus"),
                parseDecimal(refundAmountUsd, "refundAmountUsd"), parseDate(appealedAt, "appealedAt"),
                parseDate(refundResolvedAt, "refundResolvedAt"), parseDate(refundFollowUpAt, "refundFollowUpAt"),
                refundNotes, notes);
    }

    private static long requiredLong(String v, String field) {
        Long parsed = parseLong(v, field);
        if (parsed == null) {
            throw OpsException.invalidInput(field + " 必填");
        }
        return parsed;
    }

    private static Long parseLong(String v, String field) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw OpsException.invalidInput(field + " 不是合法数字: " + v);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String v, String field) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            throw OpsException.invalidInput(field + " 取值非法: " + v);
        }
    }

    private static Instant parseInstant(String v, String field) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException e) {
            throw OpsException.invalidInput(field + " 不是 ISO 时间: " + v);
        }
    }

    private static LocalDate parseDate(String v, String field) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw OpsException.invalidInput(field + " 不是 yyyy-MM-dd 日期: " + v);
        }
    }

    private static BigDecimal parseDecimal(String v, String field) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw OpsException.invalidInput(field + " 不是合法金额: " + v);
        }
    }
}
