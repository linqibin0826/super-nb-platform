package me.supernb.activity.app.usecase.thursday.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 疯四桶配置(spec §3):场次日期→分组 id 的映射、单笔门槛、桶上限、卡有效期。
/// 值来自 `activity.thursday.*`(application.yml / env)。app 模块只依赖 spring-context,
/// 照 GateProperties 用 @Value 构造注入。
///
/// **sessions 为空 = 功能休眠**(handler 短路,连资格都不查),照闸机 win-rate ≤ 0 的惯例。
///
/// 🚨 **一场一个分组**,不是三场共用一个。sub2api 的 bulk-assign 幂等键是
/// `(user_id, group_id)`——同组已有订阅时,validity_days 与 notes 都一致就返回 `reused`
/// **不新建**、任一不同就 409。三场若共用一个分组,第二场的回头客要么被静默判成"已领过"
/// (拿不到第二张卡且看不出来)、要么撞 409。分组分开则 notes 可以照 runbook 固定成
/// `opening-fk` 不动,幂等语义正好落在"这一场领过没"上。
@Component
public class ThursdayProperties {

    private final Map<LocalDate, Long> sessions;
    private final BigDecimal minAmountCny;
    private final int bucketLimit;
    private final int validityDays;
    private final String notes;
    private final int hiddenCount;
    private final String hiddenSalt;
    private final LocalTime revealAt;

    public ThursdayProperties(
            @Value("${activity.thursday.sessions:}") String sessions,
            @Value("${activity.thursday.min-amount-cny:50}") BigDecimal minAmountCny,
            @Value("${activity.thursday.bucket-limit:50}") int bucketLimit,
            @Value("${activity.thursday.validity-days:1}") int validityDays,
            @Value("${activity.thursday.notes:opening-fk}") String notes,
            @Value("${activity.thursday.hidden-count:3}") int hiddenCount,
            @Value("${activity.thursday.hidden-salt:}") String hiddenSalt,
            @Value("${activity.thursday.reveal-at:22:00}") String revealAt) {
        this.sessions = parseSessions(sessions);
        this.minAmountCny = minAmountCny;
        this.bucketLimit = bucketLimit;
        this.validityDays = validityDays;
        this.notes = notes;
        this.hiddenCount = hiddenCount;
        this.hiddenSalt = hiddenSalt;
        this.revealAt = LocalTime.parse(revealAt);
    }

    /// 解析 `2026-07-30=123,2026-08-06=124` 形式;空串/空白项跳过,格式错直接抛——
    /// 宁可启动失败也不要带着半份场次表上线(错一个日期就是一整场发不出卡)。
    private static Map<LocalDate, Long> parseSessions(String raw) {
        Map<LocalDate, Long> parsed = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return Map.copyOf(parsed);
        }
        for (String item : raw.split(",")) {
            String entry = item.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int eq = entry.indexOf('=');
            if (eq <= 0 || eq == entry.length() - 1) {
                throw new IllegalArgumentException("activity.thursday.sessions 格式应为 yyyy-MM-dd=groupId,实得:" + entry);
            }
            parsed.put(LocalDate.parse(entry.substring(0, eq).trim()),
                    Long.valueOf(entry.substring(eq + 1).trim()));
        }
        return Map.copyOf(parsed);
    }

    /// 该日期的场次分组 id;当天不是场次则返回 null。
    public Long groupIdFor(LocalDate date) {
        return sessions.get(date);
    }

    public BigDecimal minAmountCny() {
        return minAmountCny;
    }

    public int bucketLimit() {
        return bucketLimit;
    }

    public int validityDays() {
        return validityDays;
    }

    public String notes() {
        return notes;
    }

    /// 每场隐藏款(瑞幸)个数。
    public int hiddenCount() {
        return hiddenCount;
    }

    /// 摇号用的服务端盐。不配也能跑(退化成公开可复算),配了则外人无法提前枚举。
    public String hiddenSalt() {
        return hiddenSalt;
    }

    /// 开奖时刻(当地时区);此刻之前一律不揭晓,此刻之后名单冻结。
    public LocalTime revealAt() {
        return revealAt;
    }
}
