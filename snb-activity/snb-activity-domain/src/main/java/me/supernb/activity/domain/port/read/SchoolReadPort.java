package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// 开学季只读端口:首充判定 + 带人计数 + 拉人榜。infra 薄委托 sub2api 防腐层
/// 既有 Recharge/Referral 两读模型(复用既有只读池,零新增装配条件)。
public interface SchoolReadPort {

    /// 该用户人生第一笔 COMPLETED 付款单(balance/subscription 均算);从未付过返回 empty。
    Optional<FirstCharge> firstCharge(long userId);

    /// 合格被邀人数:被邀人窗口内注册未软删,人生首笔付款单 ≥minAmountCny 且落窗口内(不封顶)。
    int qualifiedInviteeCount(long inviterId, Instant start, Instant end, BigDecimal minAmountCny);

    /// 拉人榜 Top limit:人数降序 → 先达到者优先 → 邀请人 id;name 已脱敏。
    List<InviterRank> topInviters(Instant start, Instant end, BigDecimal minAmountCny, int limit);

    /// 人生首充事实。
    record FirstCharge(BigDecimal amountCny, Instant completedAt) {
    }

    /// 拉人榜条目(name 已脱敏)。count=首充合格数(排名与榜奖只认它);
    /// invited=窗口内注册总数(含未充值,展示用)。
    record InviterRank(String name, int count, int invited) {
    }
}
