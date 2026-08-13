package me.supernb.activity.app.usecase.school.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 带兄弟来包机配置(设计稿 ai-relay specs/2026-08-13-school-season-referral-design.md §4-v2)。
/// 值来自 `activity.school.*`(application.yml / env)。app 模块只依赖 spring-context,
/// 照 ThursdayProperties 用 @Value 构造注入。
///
/// **start/end 任一为空或七个组 id 任一 ≤0 = 功能休眠**(端点回 closed,连资格都不查),
/// 照 thursday sessions 为空的惯例;日期格式错直接抛——宁可启动失败也不要带错窗口上线。
///
/// 老客线 v2 =「邀请卡养成 + 重置银行」(Tibo 梗,站长 2026-08-13 晚拍板取代 1/3/6 阶梯):
/// 1 人开卡 Go $50 → 5 人升 Plus $100 → 10 人升 ProLite $150 → 20 人升 Pro $200+疯四;
/// 非节点合格人头每人 +1 次重置,攒着自助按、不封顶。
///
/// 🚨 七个组必须分立(首充三档 + 邀请卡四档):总额卡靠周/月窗封顶,同组续期只加天数
/// 不重置额度(07-31 事故语义);升档换新档组=消耗从零起算(隐形满血,官方升级体感)。
@Component
public class SchoolSeasonProperties {

    /// 首充定档门槛(元,降序判档)与对应卡面。
    public static final int[] FIRST_CHARGE_TIERS_CNY = {100, 50, 30};
    public static final int[] FIRST_CHARGE_CARDS = {200, 100, 50};
    /// 邀请卡升档节点(合格人数,升序)与对应卡面/档名(下标 = 档位序 tier-1)。
    public static final int[] CARD_TIER_THRESHOLDS = {1, 5, 10, 20};
    public static final int[] CARD_AMOUNTS = {50, 100, 150, 200};
    public static final String[] CARD_NAMES = {"Go", "Plus", "ProLite", "Pro"};
    /// KFC 档(人工私聊发放,无 claim 端点;随 Pro 档同点)。
    public static final int KFC_TIER = 20;
    /// 被邀人首充门槛(元)。
    public static final BigDecimal INVITEE_MIN_CNY = new BigDecimal("30");

    private final Instant start;
    private final Instant end;
    private final Instant claimDeadline;
    private final long fcGroup50;
    private final long fcGroup100;
    private final long fcGroup200;
    private final long cardGroupGo;
    private final long cardGroupPlus;
    private final long cardGroupProlite;
    private final long cardGroupPro;
    private final int validityDays;
    private final String notes;

    public SchoolSeasonProperties(
            @Value("${activity.school.start:}") String start,
            @Value("${activity.school.end:}") String end,
            @Value("${activity.school.claim-deadline:}") String claimDeadline,
            @Value("${activity.school.fc-group-50:0}") long fcGroup50,
            @Value("${activity.school.fc-group-100:0}") long fcGroup100,
            @Value("${activity.school.fc-group-200:0}") long fcGroup200,
            @Value("${activity.school.card-group-go:0}") long cardGroupGo,
            @Value("${activity.school.card-group-plus:0}") long cardGroupPlus,
            @Value("${activity.school.card-group-prolite:0}") long cardGroupProlite,
            @Value("${activity.school.card-group-pro:0}") long cardGroupPro,
            @Value("${activity.school.validity-days:3}") int validityDays,
            @Value("${activity.school.notes:school-season}") String notes) {
        this.start = parseInstant(start, "activity.school.start");
        this.end = parseInstant(end, "activity.school.end");
        Instant explicitDeadline = parseInstant(claimDeadline, "activity.school.claim-deadline");
        this.claimDeadline = explicitDeadline != null ? explicitDeadline
                : (this.end == null ? null : this.end.plus(Duration.ofHours(48)));
        this.fcGroup50 = fcGroup50;
        this.fcGroup100 = fcGroup100;
        this.fcGroup200 = fcGroup200;
        this.cardGroupGo = cardGroupGo;
        this.cardGroupPlus = cardGroupPlus;
        this.cardGroupProlite = cardGroupProlite;
        this.cardGroupPro = cardGroupPro;
        this.validityDays = validityDays;
        this.notes = notes;
    }

    /// 空串→null(休眠);非法格式直接抛(启动期即炸)。
    private static Instant parseInstant(String raw, String key) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(key + " 须为 ISO-8601 Instant(如 2026-08-31T16:00:00Z),实得:" + raw, e);
        }
    }

    /// 应得档位(1..4;count<1 → 0):满足的最高节点序号。开卡/升档都取这一个口径,跨档直升。
    public static int deservedTier(int count) {
        int tier = 0;
        for (int i = 0; i < CARD_TIER_THRESHOLDS.length; i++) {
            if (count >= CARD_TIER_THRESHOLDS[i]) {
                tier = i + 1;
            }
        }
        return tier;
    }

    /// 重置银行获得侧(推导制,防事件丢失):非节点合格人头每人 +1,不封顶。
    /// earned(n) = n − |{节点 ≤ n}|;可用重置 = earned − used(used 落库)。
    public static int resetsEarned(int count) {
        int nodes = 0;
        for (int t : CARD_TIER_THRESHOLDS) {
            if (count >= t) {
                nodes++;
            }
        }
        return Math.max(0, count - nodes);
    }

    /// 窗口与七组是否配齐;false = 功能休眠。
    public boolean configured() {
        return start != null && end != null
                && fcGroup50 > 0 && fcGroup100 > 0 && fcGroup200 > 0
                && cardGroupGo > 0 && cardGroupPlus > 0 && cardGroupProlite > 0 && cardGroupPro > 0;
    }

    public Instant start() {
        return start;
    }

    public Instant end() {
        return end;
    }

    /// 领取截止(资格事件仍只认 [start,end),宽限只放领取)。
    public Instant claimDeadline() {
        return claimDeadline;
    }

    /// 首充礼卡面(50/100/200)→分组 id。
    public long firstChargeGroup(int tierCard) {
        return switch (tierCard) {
            case 50 -> fcGroup50;
            case 100 -> fcGroup100;
            case 200 -> fcGroup200;
            default -> throw new IllegalArgumentException("未知首充卡面:" + tierCard);
        };
    }

    /// 邀请卡档位(1..4 = Go/Plus/ProLite/Pro)→分组 id。
    public long cardGroup(int tier) {
        return switch (tier) {
            case 1 -> cardGroupGo;
            case 2 -> cardGroupPlus;
            case 3 -> cardGroupProlite;
            case 4 -> cardGroupPro;
            default -> throw new IllegalArgumentException("未知邀请卡档位:" + tier);
        };
    }

    /// 邀请卡有效期(天):发卡/升档时刻 → 活动结束,向上取整、至少 1 天
    /// (「卡陪你到月底」语义;首充卡仍走固定 validityDays=3)。
    public int cardValidityDays(Instant now) {
        if (end == null || !now.isBefore(end)) {
            return 1;
        }
        long seconds = Duration.between(now, end).getSeconds();
        return (int) Math.max(1, (seconds + 86399) / 86400);
    }

    /// 首充卡固定窗口天数。
    public int validityDays() {
        return validityDays;
    }

    public String notes() {
        return notes;
    }
}
