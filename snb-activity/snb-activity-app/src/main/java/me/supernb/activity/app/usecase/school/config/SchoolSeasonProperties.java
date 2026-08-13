package me.supernb.activity.app.usecase.school.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 开学季·带兄弟来包机配置(设计稿 ai-relay specs/2026-08-13-school-season-referral-design.md)。
/// 值来自 `activity.school.*`(application.yml / env)。app 模块只依赖 spring-context,
/// 照 ThursdayProperties 用 @Value 构造注入。
///
/// **start/end 任一为空或六个组 id 任一 ≤0 = 功能休眠**(端点回 closed,连资格都不查),
/// 照 thursday sessions 为空的惯例;日期格式错直接抛——宁可启动失败也不要带错窗口上线。
///
/// 🚨 六个组必须分立(首充三档 + 里程碑三档):总额卡靠周/月窗封顶,同组续期只加天数
/// 不重置额度(07-31 事故语义),两线共用组会让叠加领取的第二张卡变空壳。
@Component
public class SchoolSeasonProperties {

    /// 首充定档门槛(元,降序判档)与对应卡面。
    public static final int[] FIRST_CHARGE_TIERS_CNY = {100, 50, 30};
    public static final int[] FIRST_CHARGE_CARDS = {200, 100, 50};
    /// 里程碑人数档与对应卡面(1→50、3→100、6→200)。
    public static final int[] MILESTONE_TIERS = {1, 3, 6};
    public static final int[] MILESTONE_CARDS = {50, 100, 200};
    /// KFC 档(人工私聊发放,无 claim 端点)与里程碑计数封顶。
    public static final int KFC_TIER = 10;
    public static final int INVITE_CAP = 10;
    /// 被邀人首充门槛(元)。
    public static final BigDecimal INVITEE_MIN_CNY = new BigDecimal("30");

    private final Instant start;
    private final Instant end;
    private final Instant claimDeadline;
    private final long fcGroup50;
    private final long fcGroup100;
    private final long fcGroup200;
    private final long msGroup1;
    private final long msGroup3;
    private final long msGroup6;
    private final int validityDays;
    private final String notes;

    public SchoolSeasonProperties(
            @Value("${activity.school.start:}") String start,
            @Value("${activity.school.end:}") String end,
            @Value("${activity.school.claim-deadline:}") String claimDeadline,
            @Value("${activity.school.fc-group-50:0}") long fcGroup50,
            @Value("${activity.school.fc-group-100:0}") long fcGroup100,
            @Value("${activity.school.fc-group-200:0}") long fcGroup200,
            @Value("${activity.school.ms-group-1:0}") long msGroup1,
            @Value("${activity.school.ms-group-3:0}") long msGroup3,
            @Value("${activity.school.ms-group-6:0}") long msGroup6,
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
        this.msGroup1 = msGroup1;
        this.msGroup3 = msGroup3;
        this.msGroup6 = msGroup6;
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

    /// 窗口与六组是否配齐;false = 功能休眠。
    public boolean configured() {
        return start != null && end != null
                && fcGroup50 > 0 && fcGroup100 > 0 && fcGroup200 > 0
                && msGroup1 > 0 && msGroup3 > 0 && msGroup6 > 0;
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

    /// 里程碑人数档(1/3/6)→分组 id。KFC 档(10)人工发放,不在此表。
    public long milestoneGroup(int tierPeople) {
        return switch (tierPeople) {
            case 1 -> msGroup1;
            case 3 -> msGroup3;
            case 6 -> msGroup6;
            default -> throw new IllegalArgumentException("未知里程碑人数档:" + tierPeople);
        };
    }

    public int validityDays() {
        return validityDays;
    }

    public String notes() {
        return notes;
    }
}
