package me.supernb.activity.app.usecase.referral.config;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 拉新活动双榜配置(独立于抽奖 Campaign 机制):窗口起止、新人组 id、匹配封顶、榜长度。
/// 值来自 `activity.referral.*`(application.yml / env),经 @Value 构造注入——app 模块只依赖
/// spring-context、不引 spring-boot,故不用 @ConfigurationProperties。窗口 [start, end),end 排他。
@Component
public class ReferralProperties {

    private final Instant start;
    private final Instant end;
    private final long newcomerGroupId;
    private final BigDecimal cap;
    private final int limit;

    /// 构造:@Value 注入(start/end 收 ISO 字符串再 parse;冒号后为占位符默认值,Spring 按首个冒号分隔)。
    public ReferralProperties(
            @Value("${activity.referral.start:2026-07-09T16:00:00Z}") String start,
            @Value("${activity.referral.end:2026-07-16T15:59:59Z}") String end,
            @Value("${activity.referral.newcomer-group-id:77}") long newcomerGroupId,
            @Value("${activity.referral.cap:288}") BigDecimal cap,
            @Value("${activity.referral.limit:10}") int limit) {
        this.start = Instant.parse(start);
        this.end = Instant.parse(end);
        this.newcomerGroupId = newcomerGroupId;
        this.cap = cap;
        this.limit = limit;
    }

    /// 活动窗口起(含)。
    public Instant getStart() {
        return start;
    }

    /// 活动窗口止(排他上界)。
    public Instant getEnd() {
        return end;
    }

    /// 新人组 id(人数榜「曾开通此组」的口径)。
    public long getNewcomerGroupId() {
        return newcomerGroupId;
    }

    /// 充值匹配单人封顶(元)。
    public BigDecimal getCap() {
        return cap;
    }

    /// 两榜返回条目数。
    public int getLimit() {
        return limit;
    }
}
