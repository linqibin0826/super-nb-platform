package me.supernb.activity.app.usecase.checkin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.app.usecase.checkin.config.CheckinEntryGateProperties;
import me.supernb.activity.domain.model.checkin.CheckinEntryGate;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.springframework.stereotype.Component;

/// 准入闸判定的唯一入口(spec §12):状态查询与打卡命令共用,窗口边界只在这里换算一次
/// ——两条路径各自换算迟早算出两个不同的窗。
///
/// 窗口 = CST 自然日闭区间 [today-(windowDays-1), today],取数窗到明天零点(排他),
/// 今天到账的每一笔都算数,不受调用时刻早晚影响。
@Component
public class CheckinEntryGateChecker {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CheckinRechargeReadPort recharge;
    private final CheckinEntryGateProperties props;

    /// 构造:注入充值只读端口与准入闸配置。
    public CheckinEntryGateChecker(CheckinRechargeReadPort recharge, CheckinEntryGateProperties props) {
        this.recharge = recharge;
        this.props = props;
    }

    /// 闸门是否启用;false 时调用方保持旧行为(人人可签),不要再调 [#check]。
    public boolean enabled() {
        return props.enabled();
    }

    /// 门槛金额透传(视图组装用)。
    public BigDecimal minCny() {
        return props.minCny();
    }

    /// 窗口天数透传(视图组装用)。
    public int windowDays() {
        return props.windowDays();
    }

    /// 评估某用户今天的准入闸(调用前先看 [#enabled],闸关时不该发这条 SQL)。
    public CheckinEntryGate.Result check(long userId, LocalDate today) {
        Instant from = today.minusDays(props.windowDays() - 1L).atStartOfDay(ZONE).toInstant();
        Instant toExclusive = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        List<CheckinEntryGate.Event> events = recharge.rechargeEvents(userId, from, toExclusive).stream()
                .map(e -> new CheckinEntryGate.Event(LocalDate.ofInstant(e.at(), ZONE), e.amount()))
                .toList();
        return CheckinEntryGate.evaluate(events, today, props.windowDays(), props.minCny());
    }

    /// 锁态提示文案(403 响应体与状态接口共用同一句,口径 env 可调不写死)。
    public String lockedMessage() {
        return "近 " + props.windowDays() + " 天充值满 ¥" + plain(props.minCny()) + " 才能上机签到";
    }

    /// 金额展示:去尾零(30.00→30、29.99 原样)。查询侧组装锁态文案也用它,保持同一口径。
    public static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
